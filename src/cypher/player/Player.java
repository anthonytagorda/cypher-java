package cypher.player;

import cypher.player.app.PlayerApp;
import cypher.player.app.PlayerAppHelper;
import cypher.player.views.PlayerLoginView;
import cypher.server.app.ServerApp;
import cypher.server.app.ServerAppHelper;
import cypher.server.tables.leaderboard.LeaderboardEntry;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import javax.swing.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

public class Player {
    private static final String CYPHER = "Cypher";

    private static ORB orb;
    private static ServerApp server;
    private static int playerId;
    private static String playerUsername;

    public static void main(String[] args) {
        try {
            connect();
            SwingUtilities.invokeLater(PlayerLoginView::new);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "Failed to start player:\n" + e.getMessage(),
                    "Player Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private static void connect() throws Exception {
        Properties cfg = loadConfig();
        String host = cfg.getProperty("server.ip").trim();
        String port = cfg.getProperty("server.port").trim();

        String[] orbArgs = {"-ORBInitialHost", host, "-ORBInitialPort", port};
        orb = ORB.init(orbArgs, null);

        org.omg.CORBA.Object nsObj = orb.resolve_initial_references("NameService");
        NamingContextExt nc = NamingContextExtHelper.narrow(nsObj);
        server = ServerAppHelper.narrow(nc.resolve_str(CYPHER));
    }

    private static Properties loadConfig() throws Exception {
        Properties p = new Properties();

        try (InputStream in = Player.class.getClassLoader().getResourceAsStream("src/config.properties")) {
            if (in != null) {
                p.load(in);
                return p;
            }
        }

        java.nio.file.Path path = java.nio.file.Paths.get("src/config.properties");
        if (java.nio.file.Files.exists(path)) {
            try (InputStream in = java.nio.file.Files.newInputStream(path)) {
                p.load(in);
                System.out.println("Loaded config from " + path.toAbsolutePath());
                return p;
            }
        }

        throw new IllegalStateException("config.properties not found (expected classpath or src/config.properties).");
    }

    public static int login(String username, String password) {
        try {
            PlayerApp callback = createCallback(username);
            playerId = server.login(username, password, callback);
            playerUsername = username;
            return 0;
        } catch (cypher.server.controller.exceptions.InvalidCredentialsException e) {
            return 2;
        } catch (cypher.server.controller.exceptions.AlreadyLoggedInException e) {
            return 1;
        } catch (cypher.server.controller.exceptions.PlayerBannedException e) {
            return 3;
        } catch (org.omg.CORBA.COMM_FAILURE e) {
            serverOfflineExit();
            return -1;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean register(String username, String password) {
        try {
            return server.register(username, password);
        } catch (org.omg.CORBA.COMM_FAILURE e) {
            System.out.println("[CLIENT] COMM_FAILURE during register");
            serverOfflineExit();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void logout() {
        try {
            server.logout(playerId);
            SwingUtilities.invokeLater(PlayerLoginView::new);
        } catch (Exception e) {
            serverOfflineExit();
        }
    }

    public static String getPlayerUsername() {
        return playerUsername;
    }

    public static ArrayList<LeaderboardEntry> getLeaderboards() {
        return new ArrayList<>(Arrays.asList(server.getLeaderboard()));
    }

    private static PlayerApp createCallback(String username) {
        try {
            POA rootPoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootPoa.the_POAManager().activate();

            PlayerImpl callbackServant = new PlayerImpl();
            callbackServant.setPlayerUsername(username);

            org.omg.CORBA.Object ref = rootPoa.servant_to_reference(callbackServant);
            return PlayerAppHelper.narrow(ref);
        } catch (Exception e) {
            throw new RuntimeException("Failed creating callback", e);
        }
    }

    public static void gracefulExit() {
        try {
            if (playerId > 0) {
                server.logout(playerId);
            }
        } catch (Exception ignored) {
        } finally {
            System.exit(0);
        }
    }

    private static void serverOfflineExit() {
        JOptionPane.showMessageDialog(
                null,
                "The server is offline. Please try again later.",
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
        System.exit(0);
    }
}