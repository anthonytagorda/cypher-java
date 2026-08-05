package cypher.server;

import cypher.server.app.ServerApp;
import cypher.server.app.ServerAppHelper;
import cypher.server.views.ServerDashboardView;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CORBA.Object;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import javax.swing.*;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class Server {
    // CORBA core runtime objects used while server is active.
    private static ORB orb;
    private static POA rootpoa;
    private static NamingContextExt namingContext;

    private static final String CYPHER = "Cypher"; // Constant bind name used in CORBA NameService
    private static volatile boolean isServerRunning = false;   // Single source of truth for server lifecycle state

    private static Process nameServiceProc;    // Process handle for local NameService (tnameserv).

    private static String[] startupArgs = new String[0];   // Captured startup args from main run config (needed by ORB.init)

    public static void main(String[] args) {
        startupArgs = (args == null) ? new String[0] : args;  // Preserve args so Start button flow can reuse JVM run-config arguments.
        new ServerDashboardView(); // Launch Swing dashboard (actual server starts only on button click)
    }

    public static synchronized void startServer() {
        // 1) Read config early (fail-fast if missing/invalid)
        Properties cfg = loadConfig();
        String port = cfg.getProperty("server.port").trim();

        // 2) Ensure CORBA NameService is available before bind/rebind
        ensureNameServiceRunning(port);

        // 3) Guard against duplicate starts
        if (isServerRunning) {
            JOptionPane.showMessageDialog(null, "Server is already running.", "Server Warning!", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 4) Connect DB before booting CORBA thread
        boolean dbConnected;
        try {
            dbConnected = CypherDB.setConnection();
        } catch (Exception e) {
            e.printStackTrace();
            dbConnected = false;
        }

        if (!dbConnected) {
            JOptionPane.showMessageDialog(null, "Database connection failed.", "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 5) Start server runtime in dedicated thread (keeps Swing UI responsive)
        new Thread(() -> {
            try {
                // Initiate ORB from preserved startup args
                orb = ORB.init(startupArgs, null);

                // Resolve and activate Root POA (required before servant registration)
                rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
                rootpoa.the_POAManager().activate();

                // Register servant and narrow to generated ServerApp type
                Servant servant = new Servant();
                Object poaObject = rootpoa.servant_to_reference(servant);
                ServerApp serverApp = ServerAppHelper.narrow(poaObject);

                // Resolve NameService and bind service under constant name
                Object orbObject = orb.resolve_initial_references("NameService");
                namingContext = NamingContextExtHelper.narrow(orbObject);

                NameComponent[] path = namingContext.to_name(CYPHER);
                namingContext.rebind(path, serverApp);

                isServerRunning = true; // Mark server as running only after successful bind

                // Diagnostic startup logs (seen in audit log UI).
                InetAddress localHost = InetAddress.getLocalHost();
                System.out.println("Host Name: " + localHost.getHostName());
                System.out.println("IP Address: " + localHost.getHostAddress());
                System.out.println("Port: " + port);
                System.out.println("Server bind name: " + CYPHER);
                System.out.println("Server is running.");
                System.out.println();

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        System.out.println("[SERVER] Shutdown detected. Logging out all players...");
                        CypherDB.logoutAllPlayers();
                        CypherDB.closeConnection();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }));

                orb.run();  // Blocking loop - ORB handles remote requests until shutdown
            } catch (InvalidName | AdapterInactive | WrongPolicy | ServantNotActive |
                     org.omg.CosNaming.NamingContextPackage.InvalidName | CannotProceed | NotFound e) {
                e.printStackTrace();
                safeCleanupAfterFailedStart();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Server failed to start:\n" + e.getMessage(), "Startup Error", JOptionPane.ERROR_MESSAGE));
            } catch (Exception e) {
                e.printStackTrace();
                safeCleanupAfterFailedStart();
            }
        }, "cypher-server-thread").start();
    }

    public static synchronized void shutdownServer() {
        if (!isServerRunning) return;

        // Force all online users to offline state before DB close
        try {
            CypherDB.logoutAllPlayers();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            // Unbind name from NameService
            if (namingContext != null) {
                try {
                    NameComponent[] path = namingContext.to_name(CYPHER);
                    namingContext.unbind(path);
                } catch (Exception ignored) {

                }
            }

            // Stop ORB loop and destroy ORB resources
            if (orb != null) {
                try {
                    orb.shutdown(true);
                } catch (Exception ignored) {
                }
                try {
                    orb.destroy();
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Always close DB + reset in-memory state
        try {
            CypherDB.closeConnection();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            stopNameService();
            resetState();
            JOptionPane.showMessageDialog(null, "Server stopped.", "Shutting Server Down", JOptionPane.ERROR_MESSAGE);
        }

        System.out.println("Server shut down cleanly.");
    }

    // Centralized state reset helper used on both normal and failed shutdown/start paths
    private static void resetState() {
        isServerRunning = false;
        orb = null;
        rootpoa = null;
        namingContext = null;
    }

    private static synchronized void ensureNameServiceRunning(String port) {
        try {
            // If process exists and alive, reuse it
            if (nameServiceProc != null && nameServiceProc.isAlive())
                return;

            // Windows launch: run tnameserv in background.
            // Note: this assumes tnameserv is on PATH.
            nameServiceProc = new ProcessBuilder("cmd", "/c", "tnameserv", "-ORBInitialPort", port).redirectErrorStream(true).start();

            Thread.sleep(1000); // Small warm-up delay for NameService startup
        } catch (Exception e) {
            throw new RuntimeException("Failed to start tnameserv", e);
        }
    }

    private static synchronized void safeCleanupAfterFailedStart() {
        try {
            if (orb != null) {
                try {
                    orb.shutdown(false);
                } catch (Exception ignored) {
                }
                try {
                    orb.destroy();
                } catch (Exception ignored) {
                }
            }
        } finally {
            try {
                CypherDB.closeConnection();
            } catch (Exception ignored) {
            }
            stopNameService();
            resetState();
        }
    }

    private static Properties loadConfig() {
        try {
            Properties p = new Properties();

            // Reads from project-relative src/config.properties.
            // If packaging to JAR later, consider classpath resource loading
            try (InputStream in = Files.newInputStream(Paths.get("src/config.properties"))) {
                p.load(in);
                return p;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    private static synchronized void stopNameService() {
        try {
            if (nameServiceProc != null && nameServiceProc.isAlive()) {
                nameServiceProc.destroy();
                try {
                    nameServiceProc.waitFor();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                if (nameServiceProc.isAlive()) {
                    nameServiceProc.destroyForcibly();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            nameServiceProc = null;
        }
    }

    public static boolean isRunning() {
        return isServerRunning;
    }
}
