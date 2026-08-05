package cypher.player;

import cypher.player.app.PlayerApp;
import cypher.player.app.PlayerAppHelper;
import cypher.player.views.PlayerLoginView;
import cypher.server.app.ServerApp;
import cypher.server.app.ServerAppHelper;
import cypher.server.config.GameConfig;
import cypher.server.tables.game.Game;
import cypher.server.tables.leaderboard.Leaderboards;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

public class Player {
    private static final String CYPHER = "Cypher";

    private static ORB orb;
    public static Game currentGame;
    private static ServerApp server;
    private static int playerId;
    private static String playerUsername;
    private static boolean isHost = false;
    private static boolean singlePlayerMode = false;
    private static Timer serverWatchTimer;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                connect();
                new PlayerLoginView();
            } catch (Exception e) {
                System.err.println("Initial connection failed: " + e.getMessage());
                JOptionPane.showMessageDialog(
                        null,
                        "Could not connect to the server. The application will now close.",
                        "Connection Error",
                        JOptionPane.ERROR_MESSAGE
                );
                System.exit(1);
            }
        });
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

        // Start server watcher after successful connection
        startServerWatcher();
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
            if (server == null) {
                connect();
            }
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
            if (server == null) {
                connect();
            }
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
            if (server != null) {
                server.logout(playerId);
            }
        } catch (Exception ignored) {
        } finally {
            disposeAndExit();
        }
    }

    public static String getPlayerUsername() {
        return playerUsername;
    }

    public static ArrayList<Leaderboards> getLeaderboards() {
        try {
            if (server == null) {
                connect();
            }
            return new ArrayList<>(Arrays.asList(server.getLeaderboard()));
        } catch (org.omg.CORBA.COMM_FAILURE e) {
            serverOfflineExit();
            return new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
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

    @SuppressWarnings("finally")
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
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                    null,
                    "The server is offline. The application will now close.",
                    "Server Offline",
                    JOptionPane.ERROR_MESSAGE
            );
            disposeAndExit();
        });
    }

    public static void disposeAndExit() {
        if (serverWatchTimer != null) {
            serverWatchTimer.stop();
        }

        clearLocalGameState();
        playerId = -1;
        playerUsername = null;
        server = null;

        // Close all open windows
        for (Window window : Window.getWindows()) {
            window.dispose();
        }

        System.exit(0);
    }

    public static void findNewGame() {
        try {
            currentGame = server.findNewGame(playerId);
            // joined game as non-host
            isHost = false;
            singlePlayerMode = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void createNewGame() {
        try {
            currentGame = server.createNewGame(playerId);
            // if creation returned a valid game id, this client is the host
            isHost = (currentGame != null && currentGame.gameId > 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static GameConfig getGameConfig() {
        try {
            if (server == null) {
                connect();
            }
            return server.getGameConfig();
        } catch (org.omg.CORBA.COMM_FAILURE e) {
            serverOfflineExit();
            return new GameConfig(30, 180, 2, 3);
        } catch (Exception e) {
            return new GameConfig(30, 180, 2, 3);
        }
    }

    public static int getWaitingTime() {
        if (currentGame == null || currentGame.startTime == null) return -999;

        GameConfig cfg = getGameConfig();

        long diffMs = System.currentTimeMillis() - java.sql.Timestamp.valueOf(currentGame.startTime).getTime();
        return cfg.waitingTimeSecs - (int) (diffMs / 1000);
    }

    public static int getRoundDurationSeconds() {
        return getGameConfig().roundDurationSecs;
    }

    public static int getRoundsToWin() {
        return getGameConfig().roundsToWin;
    }

    public static void submitWord(String word) {
        if (word == null || word.trim().isEmpty()) return;

        if (currentGame == null) {
            return;
        }

        try {
            // Only call server when the game exists on the server (gameId>0).
            //noinspection StatementWithEmptyBody
            if (currentGame.gameId > 0) {
                server.submitWord(playerId, currentGame.gameId, word);
            } else {
                // local singleplayer: nothing to send, accept locally
            }
        } catch (org.omg.CORBA.COMM_FAILURE e) {
            serverOfflineExit();
        } catch (Exception e) {
            // show error to the user but don't crash the client
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    null,
                    "Failed to submit word: " + e.getMessage(),
                    "Submit Error",
                    JOptionPane.ERROR_MESSAGE
            ));
        }
    }

    public static void leaveGame() {
        try {
            if (currentGame != null && currentGame.gameId > 0) {
                server.leaveGame(playerId, currentGame.gameId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void startGame(int gameId) {
        try {
            server.startGame(gameId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isHost() {
        return isHost;
    }

    public static void clearLocalGameState() {
        currentGame = null;
        isHost = false;
        singlePlayerMode = false;
    }

    public static void setSinglePlayerMode(boolean single) {
        singlePlayerMode = single;
    }

    public static boolean isSinglePlayerMode() {
        return singlePlayerMode;
    }

    @SuppressWarnings("finally")
    public static void forceLogoutAfterBan() {
        try {
            if (playerId > 0) {
                try {
                    server.logout(playerId);
                } catch (Exception ignored) {
                    // Server may have already disconnected this player on the admin side.
                }
            }
        } finally {
            clearLocalGameState();
            System.exit(0);
        }
    }

    private static void startServerWatcher() {
        if (serverWatchTimer != null && serverWatchTimer.isRunning()) {
            return; // Already running
        }

        serverWatchTimer = new Timer(500, e -> {
            try {
                // Ping server by calling a lightweight method
                // Only ping if not in singleplayer mode
                if (!singlePlayerMode && server != null) {
                    server.getGameConfig();
                }
            } catch (org.omg.CORBA.COMM_FAILURE ex) {
                ((Timer) e.getSource()).stop();
                serverOfflineExit();
            } catch (Exception ex) {
                // ignore other errors during ping
            }
        });
        serverWatchTimer.start();
    }
}