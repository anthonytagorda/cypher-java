package cypher.server;

import cypher.player.app.PlayerApp;
import cypher.server.app.ServerAppPOA;
import cypher.server.config.GameConfig;
import cypher.server.controller.exceptions.*;
import cypher.server.tables.game.Game;
import cypher.server.tables.leaderboard.LeaderboardEntry;
import cypher.server.tables.player.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("all")
public class Servant extends ServerAppPOA {
    private static final Map<Integer, cypher.player.app.PlayerApp> clients = new ConcurrentHashMap<>();

    @Override
    public boolean register(String username, String password) {
        if (username == null || password == null) return false;

        username = username.trim();
        password = password.trim();

        if (username.isEmpty() || password.isEmpty()) return false;
        if (!username.matches("[a-zA-Z0-9]+")) return false;
        if (username.length() < 5 || username.length() > 30) return false;
        if (password.length() < 6 || password.length() > 30) return false;

        final String checkSql = "SELECT 1 FROM players WHERE username = ? LIMIT 1";
        final String insertSql =
                "INSERT INTO players(username, password, status, is_banned, total_wins) " +
                        "VALUES (?, ?, 'offline', 0, 0)";

        try {
            Connection conn = CypherDB.getConnection();

            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, username);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) return false;
                }
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, password);
                return insertStmt.executeUpdate() > 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public int login(String username, String password, PlayerApp callback)
            throws InvalidCredentialsException, AlreadyLoggedInException, PlayerBannedException {

        if (username == null || password == null) {
            InvalidCredentialsException e = new InvalidCredentialsException();
            e.message = "Username/password cannot be null.";
            throw e;
        }

        username = username.trim();
        password = password.trim();

        if (username.isEmpty() || password.isEmpty()) {
            InvalidCredentialsException e = new InvalidCredentialsException();
            e.message = "Username/password cannot be empty.";
            throw e;
        }

        try {
            int code = CypherDB.loginPlayer(username, password);

            switch (code) {
                case 0: {
                    // success -> fetch player id
                    Player p = CypherDB.getPlayerFromUsername(username);
                    if (p == null) {
                        InvalidCredentialsException e = new InvalidCredentialsException();
                        e.message = "Player record not found after login.";
                        throw e;
                    }
                    // register callback for this player so server can call back
                    try {
                        clients.put(p.playerId, callback);
                    } catch (Throwable t) {
                        System.err.println("Failed to register callback for player " + p.playerId + ": " + t.getMessage());
                    }
                    return p.playerId;
                }
                case 1: {
                    AlreadyLoggedInException e = new AlreadyLoggedInException();
                    e.message = "Player is already logged in.";
                    throw e;
                }
                case 2: {
                    InvalidCredentialsException e = new InvalidCredentialsException();
                    e.message = "Invalid username or password.";
                    throw e;
                }
                case 3: {
                    PlayerBannedException e = new PlayerBannedException();
                    e.message = "This account is banned.";
                    throw e;
                }
                default: {
                    InvalidCredentialsException e = new InvalidCredentialsException();
                    e.message = "Login failed due to server/database error.";
                    throw e;
                }
            }
        } catch (InvalidCredentialsException | AlreadyLoggedInException | PlayerBannedException ex) {
            throw ex;
        } catch (Exception ex) {
            ex.printStackTrace();
            InvalidCredentialsException e = new InvalidCredentialsException();
            e.message = "Unexpected login error: " + ex.getMessage();
            throw e;
        }
    }

    // Called by CypherDB when a waiting game becomes started (second player joined)
    public static void notifyGameStarted(int gameId) {
        try {
            List<cypher.server.tables.player.Player> players = CypherDB.getPlayersInGame(gameId);
            for (cypher.server.tables.player.Player p : players) {
                cypher.player.app.PlayerApp app = clients.get(p.playerId);
                if (app != null) {
                    // Build opponents list
                    java.util.List<String> opps = new java.util.ArrayList<>();
                    for (cypher.server.tables.player.Player p2 : players) {
                        if (p2.playerId != p.playerId) opps.add(p2.username);
                    }
                    String[] oppArray = opps.toArray(new String[0]);
                    try {
                        app.gameFound(gameId, oppArray);
                    } catch (Exception ex) {
                        System.err.println("Failed to call gameFound on player " + p.playerId + ": " + ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void logout(int playerId) {
        try {
            cypher.server.tables.player.Player p = CypherDB.getPlayerFromId(playerId);

            CypherDB.setPlayerOffline(playerId);

            String username = (p != null && p.username != null) ? p.username : ("id=" + playerId);
            System.out.println("[SERVER] Player logged out: " + username);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startGame(int gameId) throws GameNotFoundException, GameAlreadyStartedException {
        try {
            CypherDB.startGame(gameId);

            // Fetch players and push initial letters to all clients so their in-game views open
            java.util.List<cypher.server.tables.player.Player> players = CypherDB.getPlayersInGame(gameId);

            // Generate letters (same logic as client)
            String letters = generateLettersForRound();

            for (cypher.server.tables.player.Player p : players) {
                cypher.player.app.PlayerApp app = clients.get(p.playerId);
                if (app != null) {
                    try {
                        app.sendLetters(letters);
                    } catch (Exception ex) {
                        System.err.println("Failed to send letters to player " + p.playerId + ": " + ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String generateLettersForRound() {
        java.util.Random random = new java.util.Random();
        StringBuilder letters = new StringBuilder();
        String vowels = "aeiou";
        String consonants = "bcdfghjklmnpqrstvwxyz";

        for (int i = 0; i < 7; i++) letters.append(vowels.charAt(random.nextInt(vowels.length())));
        for (int i = 0; i < 13; i++) letters.append(consonants.charAt(random.nextInt(consonants.length())));

        java.util.List<Character> list = new java.util.ArrayList<>();
        for (char c : letters.toString().toCharArray()) list.add(c);
        java.util.Collections.shuffle(list);

        StringBuilder out = new StringBuilder();
        for (char c : list) out.append(c);
        return out.toString();
    }

    @Override
    public Game findNewGame(int playerId) {
        return CypherDB.findNewGame(playerId);
    }

    @Override
    public Game createNewGame(int playerId) {
        return CypherDB.createNewGame(playerId);
    }

    @Override
    public boolean hasOpponentJoined(int playerId) {
        return CypherDB.hasOpponentJoined(playerId);
    }

    @Override
    public void cancelQueue(int playerId) throws PlayerNotInQueueException {

    }

    @Override
    public void submitWord(int playerId, int gameId, String word) throws WordTooShortException, InvalidWordException, InvalidLettersException, RoundNotActiveException, NotInGameException {
        if (word == null) {
            WordTooShortException e = new WordTooShortException();
            e.message = "Word cannot be null.";
            throw e;
        }
        word = word.trim().toLowerCase();
        if (word.length() < 4) {
            WordTooShortException e = new WordTooShortException();
            e.message = "Word too short (min 4).";
            throw e;
        }

        // Minimal server-side handling: award points equal to word length to player's total_score
        String update = "UPDATE game_players SET total_score = total_score + ? WHERE game_id = ? AND player_id = ?";
        try (PreparedStatement ps = CypherDB.getConnection().prepareStatement(update)) {
            ps.setInt(1, word.length());
            ps.setInt(2, gameId);
            ps.setInt(3, playerId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                NotInGameException e = new NotInGameException();
                e.message = "Player not in specified game.";
                throw e;
            }

            // notify the submitting player that word was accepted
            cypher.player.app.PlayerApp app = clients.get(playerId);
            if (app != null) {
                try {
                    app.wordAccepted(word);
                } catch (Exception ex) {
                    System.err.println("Failed to call wordAccepted on player " + playerId + ": " + ex.getMessage());
                }
            }
        } catch (NotInGameException e) {
            throw e;
        } catch (SQLException e) {
            e.printStackTrace();
            InvalidLettersException ile = new InvalidLettersException();
            ile.message = "Database error when submitting word.";
            throw ile;
        }
    }

    @Override
    public void leaveGame(int playerId, int gameId) throws NotInGameException {
        try {
            String winner = CypherDB.finalizeGameAndGetWinner(gameId);

            List<java.util.Map<String, Object>> stats = CypherDB.getGamePlayersStats(gameId);

            int n = stats.size();
            String[] usernames = new String[n];
            int[] roundWins = new int[n];
            int[] totalScores = new int[n];

            for (int i = 0; i < n; i++) {
                Map<String, Object> m = stats.get(i);
                usernames[i] = (String) m.get("username");
                roundWins[i] = (int) m.get("round_wins");
                totalScores[i] = (int) m.get("total_score");
            }

            // notify all players in the game
            List<Player> players = CypherDB.getPlayersInGame(gameId);
            for (Player p : players) {
                cypher.player.app.PlayerApp app = clients.get(p.playerId);
                if (app != null) {
                    try {
                        app.sendGameResult(usernames, roundWins, totalScores, winner);
                    } catch (Exception ex) {
                        System.err.println("Failed to call sendGameResult on player " + p.playerId + ": " + ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public LeaderboardEntry[] getLeaderboard() {
        return new LeaderboardEntry[0];
    }

    @Override
    public GameConfig getGameConfig() {
        return null;
    }

    @Override
    public void setWaitingTime(int seconds) {

    }

    @Override
    public void setRoundDuration(int seconds) {

    }

    @Override
    public void setMaxPlayers(int maxPlayers) {

    }
}
