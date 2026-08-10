package cypher.server;

import cypher.player.app.PlayerApp;
import cypher.server.app.ServerAppPOA;
import cypher.server.config.GameConfig;
import cypher.server.controller.exceptions.*;
import cypher.server.tables.game.Game;
import cypher.server.tables.leaderboard.Leaderboards;
import cypher.server.tables.player.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("all")
public class Servant extends ServerAppPOA {
    private static final Map<Integer, PlayerApp> clients = new ConcurrentHashMap<>();

    public static void notifyAndDisconnectBannedPlayer(int playerId) {
        try {
            PlayerApp app = clients.get(playerId);
            if (app != null) {
                try {
                    app.notifyBanned();
                } catch (Exception ex) {
                    System.err.println("Failed to notify banned player " + playerId + ": " + ex.getMessage());
                }
            }
            // Check if player is in an active game and handle game cancellation
            String query = "SELECT gp.game_id, g.status FROM game_players gp JOIN games g ON gp.game_id = g.game_id WHERE gp.player_id = ? AND g.status = 'IN PROGRESS'";
            try (PreparedStatement ps = CypherDB.getConnection().prepareStatement(query)) {
                ps.setInt(1, playerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int gameId = rs.getInt("game_id");
                        // Remove player from game
                        String removePlayer = "DELETE FROM game_players WHERE game_id = ? AND player_id = ?";
                        try (PreparedStatement psRemove = CypherDB.getConnection().prepareStatement(removePlayer)) {
                            psRemove.setInt(1, gameId);
                            psRemove.setInt(2, playerId);
                            psRemove.executeUpdate();
                        }
                        // Check remaining players
                        List<Player> remainingPlayers = CypherDB.getPlayersInGame(gameId);
                        if (remainingPlayers.size() < 1) {
                            // No players left, cancel the game
                            CypherDB.cancelGame(gameId);
                        } else {
                            // Any players remain - cancel the game due to ban and notify them
                            Player bannedPlayer = CypherDB.getPlayerFromId(playerId);
                            String bannedPlayerName = (bannedPlayer != null) ? bannedPlayer.username : "Unknown";
                            
                            // Cancel the game with CANCELLED status
                            CypherDB.cancelGame(gameId);
                            
                            // Notify remaining players about the ban and game cancellation
                            for (Player p : remainingPlayers) {
                                PlayerApp remainingApp = clients.get(p.playerId);
                                if (remainingApp != null) {
                                    try {
                                        remainingApp.playerDisconnected(bannedPlayerName);
                                    } catch (Exception ex) {
                                        System.err.println("Failed to call playerDisconnected: " + ex.getMessage());
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } finally {
            clients.remove(playerId);
            CypherDB.setPlayerOffline(playerId);
        }
    }

    @Override
    public boolean register(String username, String password) {
        if (username == null || password == null) return false;

        username = username.trim();
        password = password.trim();

        if (username.isEmpty() || password.isEmpty()) return false;
        if (!username.matches("[a-zA-Z0-9]+")) return false;
        if (username.length() < 5 || username.length() > 30) return false;
        if (password.length() < 6 || password.length() > 30) return false;

        return CypherDB.registerPlayer(username, password);
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
            List<Player> players = CypherDB.getPlayersInGame(gameId);
            for (Player p : players) {
                PlayerApp app = clients.get(p.playerId);
                if (app != null) {
                    // Build opponents list
                    List<String> opps = new ArrayList<>();
                    for (Player p2 : players) {
                        if (p2.playerId != p.playerId) opps.add(encodeLobbyPlayer(p2));
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

    private static String encodeLobbyPlayer(Player player) {
        if (player == null) return "-1|unknown";
        String username = player.username != null ? player.username : "unknown";
        return player.playerId + "|" + username;
    }

    @Override
    public void logout(int playerId) {
        try {
            Player p = CypherDB.getPlayerFromId(playerId);

            CypherDB.setPlayerOffline(playerId);
            clients.remove(playerId);

            String username = (p != null && p.username != null) ? p.username : ("id=" + playerId);
            System.out.println("[SERVER] Player logged out: " + username);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startGame(int gameId) throws GameNotFoundException, GameAlreadyStartedException {
        try {
            // Fetch players and push initial letters to all clients so their in-game views open
            List<Player> players = CypherDB.getPlayersInGame(gameId);

            if (players.size() < 2) {
                return;
            }

            CypherDB.startGame(gameId);

            // Generate letters (same logic as client)
            String letters = generateLettersForRound();

            for (Player p : players) {
                PlayerApp app = clients.get(p.playerId);
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
        Random random = new Random();
        StringBuilder letters = new StringBuilder();
        String vowels = "aeiou";
        String consonants = "bcdfghjklmnpqrstvwxyz";

        for (int i = 0; i < 7; i++) letters.append(vowels.charAt(random.nextInt(vowels.length())));
        for (int i = 0; i < 13; i++) letters.append(consonants.charAt(random.nextInt(consonants.length())));

        List<Character> list = new ArrayList<>();
        for (char c : letters.toString().toCharArray()) list.add(c);
        Collections.shuffle(list);

        StringBuilder out = new StringBuilder();
        for (char c : list) out.append(c);
        return out.toString();
    }

    @Override
    public Game findNewGame(int playerId) {
        Game g = CypherDB.findNewGame(playerId);
        if (g != null && g.gameId > 0) {
            // Player successfully joined an existing game.
            // Notify THIS player of the current opponents.
            List<String> opponents = CypherDB.getOpponentsInGame(g.gameId, playerId);
            PlayerApp app = clients.get(playerId);
            if (app != null) {
                try {
                    app.gameFound(g.gameId, opponents.toArray(new String[0]));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Also notify ALL other players in this game that a new player (this one) joined.
            List<Player> allPlayers = CypherDB.getPlayersInGame(g.gameId);
            String thisUsername = "";
            for (Player p : allPlayers) {
                if (p.playerId == playerId) {
                    thisUsername = p.username;
                    break;
                }
            }

            for (Player p : allPlayers) {
                if (p.playerId != playerId) {
                    PlayerApp otherApp = clients.get(p.playerId);
                    if (otherApp != null) {
                        try {
                            // Fetch all opponents for that specific player
                            List<String> otherOpponents = CypherDB.getOpponentsInGame(g.gameId, p.playerId);
                            otherApp.gameFound(g.gameId, otherOpponents.toArray(new String[0]));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return g;
    }

    @Override
    public Game createNewGame(int playerId) {
        // A player must belong to only one WAITING lobby at a time.
        // If they were previously queued/joined elsewhere, leave that queue first
        // "Host Game" always creates their own independent lobby.
        cancelQueue(playerId);

        Game g = CypherDB.createNewGame(playerId);
        // Notify the new host of the initial state (just themselves)
        PlayerApp app = clients.get(playerId);
        if (app != null && g.gameId > 0) {
            try {
                // When first creating, there are no opponents yet.
                app.gameFound(g.gameId, new String[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return g;
    }

    @Override
    public boolean hasOpponentJoined(int playerId) {
        return CypherDB.hasOpponentJoined(playerId);
    }

    @Override
    public void cancelQueue(int playerId) {
        // Find if this player is in a WAITING game
        String query = "SELECT gp.game_id, g.host_player_id FROM game_players gp JOIN games g ON gp.game_id = g.game_id " +
                "WHERE gp.player_id = ? AND g.status = 'WAITING'";
        try (PreparedStatement ps = CypherDB.getConnection().prepareStatement(query)) {
            ps.setInt(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int gameId = rs.getInt("game_id");
                    int hostId = rs.getInt("host_player_id");

                    if (playerId == hostId) {
                        // Notify remaining players that the host left before deleting
                        List<Player> players = CypherDB.getPlayersInGame(gameId);
                        Player host = CypherDB.getPlayerFromId(hostId);
                        String hostName = (host != null && host.username != null) ? host.username : "Host";
                        for (Player p : players) {
                            if (p.playerId == hostId) {
                                CypherDB.setPlayerOnline(p.playerId);
                                continue;
                            }
                            PlayerApp app = clients.get(p.playerId);
                            if (app != null) {
                                try {
                                    app.playerDisconnected("HOST_LEFT:" + hostName);
                                } catch (Exception ignored) {
                                }
                            }
                            // Set all participants back to online
                            CypherDB.setPlayerOnline(p.playerId);
                        }

                        CypherDB.cancelGame(gameId);
                    } else {
                        // Just an opponent leaving, remove them from game_players
                        String removePlayer = "DELETE FROM game_players WHERE game_id = ? AND player_id = ?";
                        try (PreparedStatement psRemove = CypherDB.getConnection().prepareStatement(removePlayer)) {
                            psRemove.setInt(1, gameId);
                            psRemove.setInt(2, playerId);
                            psRemove.executeUpdate();
                        }

                        // Set player status back to online
                        CypherDB.setPlayerOnline(playerId);

                        // Notify remaining players (especially the host) that someone left
                        List<Player> players = CypherDB.getPlayersInGame(gameId);
                        if (players.isEmpty()) {
                            // If no one left, might as well delete the game
                            CypherDB.deleteGame(gameId);
                        } else {
                            for (Player p : players) {
                                PlayerApp app = clients.get(p.playerId);
                                if (app != null) {
                                    List<String> opps = new ArrayList<>();
                                    for (Player p2 : players) {
                                        if (p2.playerId != p.playerId) opps.add(encodeLobbyPlayer(p2));
                                    }
                                    try {
                                        app.gameFound(gameId, opps.toArray(new String[0]));
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void kickPlayer(int hostPlayerId, int targetPlayerId, int gameId) throws NotInGameException {
        try {
            String hostCheck = "SELECT g.host_player_id FROM games g WHERE g.game_id = ?";
            try (PreparedStatement ps = CypherDB.getConnection().prepareStatement(hostCheck)) {
                ps.setInt(1, gameId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || rs.getInt("host_player_id") != hostPlayerId) {
                        NotInGameException e = new NotInGameException();
                        e.message = "Only the host can kick players from this game.";
                        throw e;
                    }
                }
            }

            List<Player> players = CypherDB.getPlayersInGame(gameId);
            Player host = null;
            Player target = null;
            for (Player p : players) {
                if (p.playerId == hostPlayerId) host = p;
                if (p.playerId == targetPlayerId) target = p;
            }

            if (host == null || target == null || hostPlayerId == targetPlayerId) {
                NotInGameException e = new NotInGameException();
                e.message = "The selected player is not in the current game.";
                throw e;
            }

            String removePlayer = "DELETE FROM game_players WHERE game_id = ? AND player_id = ?";
            try (PreparedStatement psRemove = CypherDB.getConnection().prepareStatement(removePlayer)) {
                psRemove.setInt(1, gameId);
                psRemove.setInt(2, targetPlayerId);
                int removed = psRemove.executeUpdate();
                if (removed == 0) {
                    NotInGameException e = new NotInGameException();
                    e.message = "The selected player could not be removed from the game.";
                    throw e;
                }
            }

            CypherDB.setPlayerOnline(targetPlayerId);

            PlayerApp kickedApp = clients.get(targetPlayerId);
            if (kickedApp != null) {
                try {
                    String hostName = (host.username != null) ? host.username : "Host";
                    kickedApp.playerDisconnected("KICKED_BY_HOST:" + hostName);
                } catch (Exception ex) {
                    System.err.println("Failed to notify kicked player " + targetPlayerId + ": " + ex.getMessage());
                }
            }

            List<Player> remainingPlayers = CypherDB.getPlayersInGame(gameId);
            for (Player p : remainingPlayers) {
                PlayerApp app = clients.get(p.playerId);
                if (app != null) {
                    List<String> opps = new ArrayList<>();
                    for (Player p2 : remainingPlayers) {
                        if (p2.playerId != p.playerId) opps.add(encodeLobbyPlayer(p2));
                    }
                    try {
                        app.gameFound(gameId, opps.toArray(new String[0]));
                    } catch (Exception ex) {
                        System.err.println("Failed to refresh lobby after kick for player " + p.playerId + ": " + ex.getMessage());
                    }
                }
            }
        } catch (NotInGameException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            NotInGameException ex = new NotInGameException();
            ex.message = "Unable to kick the selected player.";
            throw ex;
        }
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

            CypherDB.updateLeaderboardLongestWord(playerId, word);

            // notify the submitting player that word was accepted
            PlayerApp app = clients.get(playerId);
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
            // Get the leaving player's name for notification
            Player leavingPlayer = CypherDB.getPlayerFromId(playerId);
            String leavingPlayerName = (leavingPlayer != null) ? leavingPlayer.username : "Unknown";

            // Remove player from game
            String removePlayer = "DELETE FROM game_players WHERE game_id = ? AND player_id = ?";
            try (PreparedStatement psRemove = CypherDB.getConnection().prepareStatement(removePlayer)) {
                psRemove.setInt(1, gameId);
                psRemove.setInt(2, playerId);
                psRemove.executeUpdate();
            }

            // Check remaining players
            List<Player> remainingPlayers = CypherDB.getPlayersInGame(gameId);

            if (remainingPlayers.size() < 1) {
                // No players left, cancel the game
                CypherDB.cancelGame(gameId);
                return;
            }

            // Notify remaining players about the disconnection
            for (Player p : remainingPlayers) {
                PlayerApp app = clients.get(p.playerId);
                if (app != null) {
                    try {
                        app.playerDisconnected(leavingPlayerName);
                    } catch (Exception ex) {
                        System.err.println("Failed to call playerDisconnected on player " + p.playerId + ": " + ex.getMessage());
                    }
                }
            }

            // If only 1 player remains, end the game and declare them winner
            if (remainingPlayers.size() == 1) {
                String winner = remainingPlayers.get(0).username;
                CypherDB.finalizeGameAndGetWinner(gameId);

                List<Map<String, Object>> stats = CypherDB.getGamePlayersStats(gameId);
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

                // Notify the remaining player
                PlayerApp app = clients.get(remainingPlayers.get(0).playerId);
                if (app != null) {
                    try {
                        app.sendGameResult(usernames, roundWins, totalScores, winner);
                    } catch (Exception ex) {
                        System.err.println("Failed to call sendGameResult on player " + remainingPlayers.get(0).playerId + ": " + ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Leaderboards[] getLeaderboard() {
        List<Leaderboards> entries = CypherDB.getLeaderboard();
        return entries.toArray(new Leaderboards[0]);
    }

    @Override
    public GameConfig getGameConfig() {
        Map<String, Integer> s = CypherDB.getGameSettings();
        if (s.isEmpty()) return new GameConfig(30, 180, 2, 3);
        return new GameConfig(
                s.getOrDefault("waiting_time_sec", 30),
                s.getOrDefault("game_duration_sec", 180),
                s.getOrDefault("max_players", 2),
                s.getOrDefault("rounds_to_win", 3)
        );
    }

    @Override
    public void setWaitingTime(int seconds) {
        Map<String, Integer> s = CypherDB.getGameSettings();
        CypherDB.updateGameSettings(
                seconds,
                s.getOrDefault("game_duration_sec", 180),
                s.getOrDefault("max_players", 2),
                s.getOrDefault("rounds_to_win", 3)
        );
    }

    @Override
    public void setRoundDuration(int seconds) {
        Map<String, Integer> s = CypherDB.getGameSettings();
        CypherDB.updateGameSettings(
                s.getOrDefault("waiting_time_sec", 30),
                seconds,
                s.getOrDefault("max_players", 2),
                s.getOrDefault("rounds_to_win", 3)
        );
    }

    @Override
    public void setMaxPlayers(int maxPlayers) {
        Map<String, Integer> s = CypherDB.getGameSettings();
        CypherDB.updateGameSettings(
                s.getOrDefault("waiting_time_sec", 30),
                s.getOrDefault("game_duration_sec", 180),
                maxPlayers,
                s.getOrDefault("rounds_to_win", 3)
        );
    }
}
