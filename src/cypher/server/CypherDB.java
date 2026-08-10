package cypher.server;

import cypher.server.tables.game.Game;
import cypher.server.tables.leaderboard.Leaderboards;
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
        // First check if this player is already hosting a game
        String checkHostQuery = "SELECT game_id FROM games WHERE host_player_id = ? AND status = 'WAITING'";
        try {
            Connection conn = getConnection();
            try (PreparedStatement psCheck = conn.prepareStatement(checkHostQuery)) {
                psCheck.setInt(1, playerId);
                try (ResultSet rsCheck = psCheck.executeQuery()) {
                    if (rsCheck.next()) {
                        // Player is already hosting a game, don't join another
                        Game g = new Game();
                        g.gameId = 0;
                        g.startTime = "";
                        g.endTime = "";
                        g.gameStatus = "";
                        g.playerWinner = "";
                        return g;
                    }
                }
            }

            String query = "SELECT g.game_id, g.created_at, g.status FROM games g " +
                    "JOIN players p ON g.host_player_id = p.player_id " +
                    "WHERE g.status = ? AND p.status = 'online' AND g.host_player_id != ? " +
                    "ORDER BY g.game_id";
            Game g = new Game();
            g.gameId = 0;
            g.startTime = "";
            g.endTime = "";
            g.gameStatus = "";
            g.playerWinner = "";

            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, "WAITING");
                ps.setInt(2, playerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        g.gameId = rs.getInt("game_id");
                        g.startTime = String.valueOf(rs.getTimestamp("created_at"));
                        g.gameStatus = "WAITING";
                        addPlayerToGame(g.gameId, playerId, false);
                    }
                    return g;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Game g = new Game();
        g.gameId = 0;
        g.startTime = "";
        g.endTime = "";
        g.gameStatus = "";
        g.playerWinner = "";
        return g;
    }

    private static void addPlayerToGame(int gameId, int playerId, boolean isHost) {
        String statusCheck = "SELECT status FROM players WHERE player_id = ?";
        try {
            Connection conn = getConnection();
            try (PreparedStatement psStatusCheck = conn.prepareStatement(statusCheck)) {
                psStatusCheck.setInt(1, playerId);
                try (ResultSet rs = psStatusCheck.executeQuery()) {
                    if (rs.next()) {
                        String status = rs.getString("status");
                        if (!"online".equalsIgnoreCase(status) && !isHost) {
                            return;
                        }
                    }
                }
            }

            String query = "INSERT IGNORE INTO game_players(game_id, player_id, is_host) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setInt(1, gameId);
                ps.setInt(2, playerId);
                ps.setInt(3, isHost ? 1 : 0);
                ps.execute();
                Player p = getPlayerFromId(playerId);
                String playerName = (p != null && p.username != null) ? p.username : "unknown";

                if (isHost) {
                    System.out.println("Player " + playerName + " hosted Game " + gameId + ".");
                } else {
                    // Get host username for joiner players
                    String hostQuery = "SELECT host_player_id FROM games WHERE game_id = ?";
                    try (PreparedStatement psHost = conn.prepareStatement(hostQuery)) {
                        psHost.setInt(1, gameId);
                        try (ResultSet rsHost = psHost.executeQuery()) {
                            if (rsHost.next()) {
                                int hostId = rsHost.getInt("host_player_id");
                                Player host = getPlayerFromId(hostId);
                                String hostName = (host != null && host.username != null) ? host.username : "unknown";
                                System.out.println("Player " + playerName + " joined Game " + gameId + " (Host: " + hostName + ").");
                            }
                        }
                    }
                }
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
        List<Player> players = new ArrayList<>();
        String query = "SELECT p.player_id, p.username, p.password, p.status, p.is_banned, p.total_wins, p.created_at FROM players p JOIN game_players gp ON p.player_id = gp.player_id WHERE gp.game_id = ? ORDER BY gp.joined_at";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, gameId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    players.add(new Player(
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
        String update = "UPDATE games SET started_at = now(), status = 'IN PROGRESS', rounds_played = rounds_played + 1 WHERE game_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(update)) {
            ps.setInt(1, gameId);
            ps.executeUpdate();

            // Change status of players as "in-game" only if they are currently online
            String playersUpdate = "UPDATE players p JOIN game_players gp ON p.player_id = gp.player_id SET p.status = 'in-game' WHERE gp.game_id = ? AND p.status = 'online'";
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
        return createNewGame(playerId, "multiplayer");
    }

    public static Game createNewGame(int playerId, String gameType) {
        boolean hasGameType = hasGameTypeColumn();
        String query = hasGameType
                ? "INSERT INTO games(status, host_player_id, game_type, created_at) VALUES ('WAITING', ?, ?, now())"
                : "INSERT INTO games(status, host_player_id, created_at) VALUES ('WAITING', ?, now())";
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
                if (hasGameType) {
                    ps.setString(2, (gameType != null ? gameType : "multiplayer"));
                }
                ps.execute();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        g.gameId = keys.getInt(1);
                        g.startTime = String.valueOf(new Timestamp(System.currentTimeMillis()));
                        g.gameStatus = "WAITING";
                        addPlayerToGame(g.gameId, playerId, true);
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

    public static List<String> getOpponentsInGame(int gameId, int playerId) {
        List<String> opponents = new ArrayList<>();
        String query = "SELECT concat(p.player_id, '|', p.username) AS lobby_entry " +
                "FROM game_players gp " +
                "JOIN players p ON gp.player_id = p.player_id " +
                "JOIN games g ON gp.game_id = g.game_id " +
                "WHERE gp.game_id = ? AND gp.player_id != ? " +
                "ORDER BY CASE WHEN p.player_id = g.host_player_id THEN 0 ELSE 1 END, gp.joined_at, p.username";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, gameId);
            ps.setInt(2, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    opponents.add(rs.getString("lobby_entry"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return opponents;
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
        int best = -1;
        boolean tie = false;
        List<Map<String, Object>> stats = getGamePlayersStats(gameId);
        for (Map<String, Object> m : stats) {
            Object scoreObj = m.get("total_score");
            int score = (scoreObj instanceof Number) ? ((Number) scoreObj).intValue() : 0;
            if (score > best) {
                best = score;
                winner = (String) m.get("username");
                tie = false;
            } else
                if (score == best && best != -1) {
                    tie = true;
                }
        }

        if (tie) {
            winner = "TIE";
        }

        updateLeaderboardAfterGame(gameId, winner);

        String update = "UPDATE games SET ended_at = now(), status = 'ENDED', player_winner = ? WHERE game_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(update)) {
            ps.setString(1, "TIE".equals(winner) ? null : winner);
            ps.setInt(2, gameId);
            ps.executeUpdate();

            // Delete game if no winner and it's not a tie (or even if tie if that's what user wants)
            if (winner == null || winner.isEmpty() || "TIE".equals(winner)) {
                deleteGame(gameId);
            }

            // Change the status of players as "online" again (if they were in-game)
            String playersUpdate = "UPDATE players p JOIN game_players gp ON p.player_id = gp.player_id SET p.status = 'online' WHERE gp.game_id = ? AND p.status = 'in-game'";
            try (PreparedStatement ps2 = getConnection().prepareStatement(playersUpdate)) {
                ps2.setInt(1, gameId);
                ps2.executeUpdate();
            } catch (SQLException e2) {
                e2.printStackTrace();
            }

            // if we have a winner, increment their total_wins
            if (winner != null && !winner.isEmpty() && !"TIE".equals(winner)) {
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

    // =========================
    // Leaderboard Queries
    // =========================
    public static List<Leaderboards> getLeaderboard() {
        List<Leaderboards> entries = new ArrayList<>();
        boolean hasLongestWord = hasLongestWordColumn();
        String longestWordSelect = hasLongestWord ? "COALESCE(lb.longest_word, '')" : "''";

        String query = "SELECT p.username, lb.games_played, lb.games_won, lb.total_score, "
                + longestWordSelect + " AS longest_word "
                + "FROM leaderboard lb "
                + "JOIN players p ON lb.player_id = p.player_id "
                + "WHERE lb.games_played > 0 OR lb.games_won > 0 OR lb.total_score > 0 "
                + "ORDER BY lb.games_won DESC, lb.total_score DESC, p.username ASC "
                + "LIMIT 50";

        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            int rank = 1;
            while (rs.next()) {
                Leaderboards entry = new Leaderboards();
                entry.rank = rank++;
                entry.username = rs.getString("username");
                entry.gamesPlayed = rs.getInt("games_played");
                entry.gamesWon = rs.getInt("games_won");
                entry.highestScore = rs.getInt("total_score");
                String word = rs.getString("longest_word");
                entry.longestWord = (word == null || word.isEmpty()) ? "-" : word;
                entries.add(entry);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entries;
    }

    private static void ensureLeaderboardEntry(int playerId) {
        String query = "INSERT IGNORE INTO leaderboard (player_id) VALUES (?)";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, playerId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateLeaderboardAfterGame(int gameId, String winnerUsername) {
        List<Map<String, Object>> stats = getGamePlayersStats(gameId);
        for (Map<String, Object> m : stats) {
            int playerId = ((Number) m.get("player_id")).intValue();
            int gameScore = ((Number) m.get("total_score")).intValue();
            String username = (String) m.get("username");
            boolean won = winnerUsername != null
                    && !winnerUsername.isEmpty()
                    && !"TIE".equalsIgnoreCase(winnerUsername)
                    && username.equalsIgnoreCase(winnerUsername);

            ensureLeaderboardEntry(playerId);
            String update = "UPDATE leaderboard SET games_played = games_played + 1, "
                    + "games_won = games_won + ?, "
                    + "total_score = GREATEST(total_score, ?) "
                    + "WHERE player_id = ?";
            try (PreparedStatement ps = getConnection().prepareStatement(update)) {
                ps.setInt(1, won ? 1 : 0);
                ps.setInt(2, gameScore);
                ps.setInt(3, playerId);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void updateLeaderboardLongestWord(int playerId, String word) {
        if (word == null || word.isEmpty() || !hasLongestWordColumn()) {
            return;
        }

        word = word.trim().toLowerCase();
        ensureLeaderboardEntry(playerId);

        String update = "UPDATE leaderboard SET longest_word = ? "
                + "WHERE player_id = ? AND (longest_word IS NULL OR longest_word = '' OR CHAR_LENGTH(longest_word) < ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(update)) {
            ps.setString(1, word);
            ps.setInt(2, playerId);
            ps.setInt(3, word.length());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static boolean hasLongestWordColumn() {
        String query = "SELECT COUNT(*) AS cnt FROM information_schema.columns "
                + "WHERE table_schema = database() AND table_name = 'leaderboard' AND column_name = 'longest_word'";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            return rs.next() && rs.getInt("cnt") > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    // =========================
    // Game Admin Queries
    // =========================
    public static List<Map<String, Object>> getGames() {
        List<Map<String, Object>> games = new ArrayList<>();
        boolean hasGameType = hasGameTypeColumn();
        String query = hasGameType
                ? "SELECT g.game_id, COALESCE(p.username, '?') AS host_username, g.game_type, " +
                "g.status, g.rounds_played, COALESCE(g.player_winner, '-') AS player_winner, " +
                "g.created_at, g.started_at, g.ended_at " +
                "FROM games g LEFT JOIN players p ON g.host_player_id = p.player_id " +
                "ORDER BY g.game_id ASC"
                : "SELECT g.game_id, COALESCE(p.username, '?') AS host_username, 'multiplayer' AS game_type, " +
                "g.status, g.rounds_played, COALESCE(g.player_winner, '-') AS player_winner, " +
                "g.created_at, g.started_at, g.ended_at " +
                "FROM games g LEFT JOIN players p ON g.host_player_id = p.player_id " +
                "ORDER BY g.game_id ASC";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(query)
        ) {
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("game_id", rs.getInt("game_id"));
                m.put("host", rs.getString("host_username"));
                m.put("game_type", rs.getString("game_type"));
                m.put("status", rs.getString("status"));
                m.put("rounds_played", rs.getInt("rounds_played"));
                m.put("winner", rs.getString("player_winner"));
                m.put("created_at", rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toString() : "-");
                m.put("started_at", rs.getTimestamp("started_at") != null ? rs.getTimestamp("started_at").toString() : "-");
                m.put("ended_at", rs.getTimestamp("ended_at") != null ? rs.getTimestamp("ended_at").toString() : "-");
                games.add(m);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return games;
    }

    public static void deleteGame(int gameId) {
        String query = "DELETE FROM games WHERE game_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, gameId);
            ps.executeUpdate();
            resetGameAutoIncrement();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void cancelGame(int gameId) {
        // Update game status to CANCELLED instead of deleting
        String updateGame = "UPDATE games SET status = 'CANCELLED', ended_at = now() WHERE game_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(updateGame)) {
            ps.setInt(1, gameId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Delete related game_players
        String deletePlayers = "DELETE FROM game_players WHERE game_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(deletePlayers)) {
            ps.setInt(1, gameId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Delete the game record
        String deleteGame = "DELETE FROM games WHERE game_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(deleteGame)) {
            ps.setInt(1, gameId);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                resetGameAutoIncrement();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void resetGameAutoIncrement() {
        try {
            String maxQuery = "SELECT MAX(game_id) AS max_id FROM games";
            String resetQuery = "ALTER TABLE games AUTO_INCREMENT = ?";

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
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateGameType(int gameId, String gameType) {
        if (!hasGameTypeColumn()) {
            return;
        }
        String query = "UPDATE games SET game_type = ? WHERE game_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, gameType);
            ps.setInt(2, gameId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static boolean hasGameTypeColumn() {
        String query = "SELECT COUNT(*) AS cnt FROM information_schema.columns " +
                "WHERE table_schema = database() AND table_name = 'games' AND column_name = 'game_type'";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(query)
        ) {
            return rs.next() && rs.getInt("cnt") > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    // =========================
    // Settings Queries
    // =========================
    public static Map<String, Integer> getGameSettings() {
        Map<String, Integer> settings = new HashMap<>();
        String query = "SELECT waiting_time_sec, game_duration_sec, max_players, rounds_to_win FROM settings WHERE id = 1";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(query)
        ) {
            if (rs.next()) {
                settings.put("waiting_time_sec", rs.getInt("waiting_time_sec"));
                settings.put("game_duration_sec", rs.getInt("game_duration_sec"));
                settings.put("max_players", rs.getInt("max_players"));
                settings.put("rounds_to_win", rs.getInt("rounds_to_win"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return settings;
    }

    public static void updateGameSettings(int waitingTimeSecs, int gameDurationSecs, int maxPlayers, int roundsToWin) {
        String query = "UPDATE settings SET waiting_time_sec = ?, game_duration_sec = ?, max_players = ?, rounds_to_win = ? WHERE id = 1";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, waitingTimeSecs);
            ps.setInt(2, gameDurationSecs);
            ps.setInt(3, maxPlayers);
            ps.setInt(4, roundsToWin);
            ps.executeUpdate();
            System.out.println("Game settings updated.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}