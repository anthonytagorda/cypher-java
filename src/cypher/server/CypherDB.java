package cypher.server;

import cypher.server.tables.game.Game;
import cypher.server.tables.player.Player;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("all")
public class CypherDB {
    private static Connection connection;

    private static final String URL = "jdbc:mysql://localhost:3306/cypher";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    // =========================
    // Connection
    // =========================
    public static synchronized boolean setConnection() {
        try {
            if (connection != null && !connection.isClosed()) return true;
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Successful connection to database.");
            return true;
        } catch (SQLException e) {
            System.err.println("Cypher DB connection failed.");
            e.printStackTrace();
            return false;
        }
    }

    public static synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                if (!setConnection()) {
                    throw new RuntimeException("Database connection is not available.");
                }
            }
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to validate DB connection.", e);
        }
    }

    public static synchronized void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    System.out.println("Connection to database closed.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                connection = null;
            }
        }
    }

    public static synchronized boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    // =========================
    // Player Queries
    // =========================
    public static List<Player> getPlayers() throws Exception {
        List<Player> players = new ArrayList<>();
        String query = "SELECT player_id, username, password, status, is_banned, total_wins, created_at FROM players";

        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(query)
        ) {

            while (rs.next()) {
                Player p = new Player(
                        rs.getInt("player_id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("status"),
                        rs.getBoolean("is_banned"),
                        rs.getInt("total_wins"),
                        rs.getTimestamp("created_at").toString()
                );
                players.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        return players;
    }

    public static Player getPlayerFromId(int id) {
        String query = "SELECT player_id, username, password, status, is_banned, total_wins, created_at FROM players WHERE player_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Player(
                            rs.getInt("player_id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("status"),
                            rs.getBoolean("is_banned"),
                            rs.getInt("total_wins"),
                            rs.getTimestamp("created_at").toString()
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static Player getPlayerFromUsername(String username) {
        String query = "SELECT player_id, username, password, status, is_banned, total_wins, created_at FROM players WHERE username = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Player(
                            rs.getInt("player_id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("status"),
                            rs.getBoolean("is_banned"),
                            rs.getInt("total_wins"),
                            rs.getTimestamp("created_at").toString()
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void updatePlayer(Player player) {
        String query = "UPDATE players SET username = ?, password = ?, status = ?, is_banned = ?, total_wins = ? WHERE player_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, player.username);
            ps.setString(2, player.password);
            ps.setString(3, player.status);
            ps.setBoolean(4, player.isBanned);
            ps.setInt(5, player.totalWins);
            ps.setInt(6, player.playerId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deletePlayer(int playerId) {
        String query = "DELETE FROM players WHERE player_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, playerId);
            ps.executeUpdate();
            System.out.println("Deleted player " + playerId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void resetAutoIncrement() {
        try {
            String maxQuery = "SELECT MAX(player_id) AS max_id FROM players";
            String resetQuery = "ALTER TABLE players AUTO_INCREMENT = ?";

            try (Statement stmt = getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery(maxQuery)
            ) {
                int maxId = 0;
                if (rs.next()) {
                    maxId = rs.getInt("max_id");
                }

                try (PreparedStatement ps = getConnection().prepareStatement(resetQuery)) {
                    ps.setInt(1, maxId + 1);
                    ps.executeUpdate();
                    System.out.println("Reset AUTO_INCREMENT to " + (maxId + 1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void banPlayer(int playerId) {
        String query = "UPDATE players SET is_banned = 1 WHERE player_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, playerId);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                Player p = getPlayerFromId(playerId);
                String username = (p != null && p.username != null) ? p.username : "unknown";
                System.out.println("Banned player: " + username + " (ID: " + playerId + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void unbanPlayer(int playerId) {
        String query = "UPDATE players SET is_banned = 0 WHERE player_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, playerId);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                Player p = getPlayerFromId(playerId);
                String username = (p != null && p.username != null) ? p.username : "unknown";
                System.out.println("Unbanned player: " + username + " (ID: " + playerId + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean registerPlayer(String username, String password) {
        String query = "INSERT INTO players(username, password, status, is_banned, total_wins) VALUES (?, ?, 'offline', 0, 0)";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, username);
            ps.setString(2, password);
            int created = ps.executeUpdate();
            if (created > 0) {
                System.out.println("Added player: " + username);
                return true;
            }
        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState())) {
                System.out.println("Cannot add player. Username already exists: " + username);
            } else {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static String setPlayerOnline(int playerId) {
        String query = "UPDATE players SET status = 'online' WHERE player_id = ?";
        String getUserQuery = "SELECT username FROM players WHERE player_id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, playerId);
            int updated = ps.executeUpdate();

            if (updated > 0) {
                try (PreparedStatement ps2 = getConnection().prepareStatement(getUserQuery)) {
                    ps2.setInt(1, playerId);
                    try (ResultSet rs = ps2.executeQuery()) {
                        if (rs.next()) {
                            String username = rs.getString("username");
                            System.out.println(username + " logged in.");
                            return username;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String setPlayerOffline(int playerId) {
        String update = "UPDATE players SET status = 'offline' WHERE player_id = ?";
        String getUser = "SELECT username FROM players WHERE player_id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(update)) {
            ps.setInt(1, playerId);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                try (PreparedStatement ps2 = getConnection().prepareStatement(getUser)) {
                    ps2.setInt(1, playerId);
                    try (ResultSet rs = ps2.executeQuery()) {
                        if (rs.next()) return rs.getString("username");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void logoutAllPlayers() {
        String query = "UPDATE players SET status = 'offline'";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            int count = ps.executeUpdate();
            System.out.println("Logged out all players (" + count + ").");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Return codes:
     * 0 = success
     * 1 = already online
     * 2 = invalid credentials / player not found
     * 3 = banned
     * -1 = SQL error
     */
    public static int loginPlayer(String username, String password) {
        String query = "SELECT player_id, status, is_banned FROM players WHERE username = ? AND password = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("status");
                    boolean isBanned = rs.getBoolean("is_banned");
                    int playerId = rs.getInt("player_id");

                    if ("online".equalsIgnoreCase(status)) return 1;
                    if (isBanned) return 3;

                    setPlayerOnline(playerId);
                    return 0;
                } else {
                    return 2;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static Game findNewGame(int playerId) {
        String query = "SELECT game_id, created_at, status FROM games WHERE status = ? ORDER BY game_id";
        Game g = new Game();
        g.gameId = 0;
        g.startTime = "";
        g.endTime = "";
        g.gameStatus = "";
        g.playerWinner = "";

        try {
            Connection conn = getConnection();
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, "WAITING");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        g.gameId = rs.getInt("game_id");
                        g.startTime = String.valueOf(rs.getTimestamp("created_at"));
                        g.gameStatus = rs.getString("status");
                        addPlayerToGame(g.gameId, playerId);
                    }
                    return g;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return g;
    }

    private static void addPlayerToGame(int gameId, int playerId) {
        String query = "INSERT IGNORE INTO game_players(game_id, player_id, is_host) VALUES (?, ?, 0)";
        try {
            Connection conn = getConnection();
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setInt(1, gameId);
                ps.setInt(2, playerId);
                ps.execute();
                System.out.println("Player " + playerId + " joined Game " + gameId + ".");
            }

            // Entering any game (host or join) should mark the player as in-game immediately.
            String setInGame = "UPDATE players SET status = 'in-game' WHERE player_id = ?";
            try (PreparedStatement psStatus = conn.prepareStatement(setInGame)) {
                psStatus.setInt(1, playerId);
                psStatus.executeUpdate();
            }

            // After adding a player, check how many players are in the game. If two or more, mark game as started.
            String countQuery = "SELECT COUNT(*) AS cnt FROM game_players WHERE game_id = ?";
            try (PreparedStatement ps2 = conn.prepareStatement(countQuery)) {
                ps2.setInt(1, gameId);
                try (ResultSet rs = ps2.executeQuery()) {
                    if (rs.next()) {
                        int cnt = rs.getInt("cnt");
                        if (cnt >= 2) {
                            // Notify server layer that an opponent joined so server can notify clients (do NOT auto-start)
                            try {
                                Servant.notifyGameStarted(gameId);
                            } catch (Throwable t) {
                                System.err.println("Failed to notify Servant about player-join: " + t.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<Player> getPlayersInGame(int gameId) {
        List<Player> players = new java.util.ArrayList<>();
        String query = "SELECT p.player_id, p.username, p.password, p.status, p.is_banned, p.total_wins, p.created_at FROM players p JOIN game_players gp ON p.player_id = gp.player_id WHERE gp.game_id = ? ORDER BY gp.joined_at";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, gameId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    players.add(new cypher.server.tables.player.Player(
                            rs.getInt("player_id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("status"),
                            rs.getBoolean("is_banned"),
                            rs.getInt("total_wins"),
                            rs.getTimestamp("created_at").toString()
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return players;
    }

    public static void startGame(int gameId) {
        String update = "UPDATE games SET started_at = now(), status = 'IN_PROGRESS', rounds_played = rounds_played + 1 WHERE game_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(update)) {
            ps.setInt(1, gameId);
            ps.executeUpdate();

            // Change status of players as "in-game"
            String playersUpdate = "UPDATE players p JOIN game_players gp ON p.player_id = gp.player_id SET p.status = 'in-game' WHERE gp.game_id = ?";
            try (PreparedStatement ps2 = getConnection().prepareStatement(playersUpdate)) {
                ps2.setInt(1, gameId);
                ps2.executeUpdate();
            } catch (SQLException e2) {
                e2.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Game createNewGame(int playerId) {
        String query = "INSERT INTO games(status, host_player_id, created_at) VALUES ('WAITING', ?, now())";
        Game g = new Game();
        g.gameId = 0;
        g.startTime = "";
        g.endTime = "";
        g.gameStatus = "";
        g.playerWinner = "";

        try {
            Connection conn = getConnection();
            try (PreparedStatement ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, playerId);
                ps.execute();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        g.gameId = keys.getInt(1);
                        g.startTime = String.valueOf(new Timestamp(System.currentTimeMillis()));
                        g.gameStatus = "WAITING";
                        addPlayerToGame(g.gameId, playerId);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return g;
    }

    public static boolean hasOpponentJoined(int playerId) {
        String query =
                "SELECT COUNT(*) AS cnt FROM game_players WHERE game_id = " +
                        "(SELECT game_id FROM game_players WHERE player_id = ? ORDER BY joined_at DESC LIMIT 1)";

        try {
            Connection conn = getConnection();
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setInt(1, playerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("cnt") >= 2;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static List<Map<String, Object>> getGamePlayersStats(int gameId) {
        List<Map<String, Object>> out = new ArrayList<>();
        String query = "SELECT gp.player_id, p.username, gp.total_score, gp.round_wins FROM game_players gp JOIN players p ON gp.player_id = p.player_id WHERE gp.game_id = ? ORDER BY gp.joined_at";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, gameId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("player_id", rs.getInt("player_id"));
                    m.put("username", rs.getString("username"));
                    m.put("total_score", rs.getInt("total_score"));
                    m.put("round_wins", rs.getInt("round_wins"));
                    out.add(m);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    public static String finalizeGameAndGetWinner(int gameId) {
        // compute winner by highest total_score
        String winner = null;
        int best = Integer.MIN_VALUE;
        List<Map<String, Object>> stats = getGamePlayersStats(gameId);
        for (Map<String, Object> m : stats) {
            int score = (int) m.get("total_score");
            if (score > best) {
                best = score;
                winner = (String) m.get("username");
            }
        }

        String update = "UPDATE games SET ended_at = now(), status = 'ENDED', player_winner = ? WHERE game_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(update)) {
            ps.setString(1, winner);
            ps.setInt(2, gameId);
            ps.executeUpdate();

            // Change the status of players as "online" again
            String playersUpdate = "UPDATE players p JOIN game_players gp ON p.player_id = gp.player_id SET p.status = 'online' WHERE gp.game_id = ?";
            try (PreparedStatement ps2 = getConnection().prepareStatement(playersUpdate)) {
                ps2.setInt(1, gameId);
                ps2.executeUpdate();
            } catch (SQLException e2) {
                e2.printStackTrace();
            }

            // if we have a winner, increment their total_wins
            if (winner != null && !winner.isEmpty()) {
                String incWins = "UPDATE players SET total_wins = total_wins + 1 WHERE username = ?";
                try (PreparedStatement ps3 = getConnection().prepareStatement(incWins)) {
                    ps3.setString(1, winner);
                    ps3.executeUpdate();
                } catch (SQLException e3) {
                    e3.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return winner;
    }
}