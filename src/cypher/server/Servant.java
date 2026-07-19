package cypher.server;

import cypher.player.app.PlayerApp;
import cypher.server.app.ServerAppPOA;
import cypher.server.config.GameConfig;
import cypher.server.controller.exceptions.*;
import cypher.server.tables.CypherDB;
import cypher.server.tables.leaderboard.LeaderboardEntry;
import cypher.server.tables.player.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@SuppressWarnings("all")
public class Servant extends ServerAppPOA {

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

    }


    @Override
    public int findOrCreateGame(int playerId) throws NoPlayersAvailableException, GameAlreadyStartedException, PlayerAlreadyInQueueException {
        return playerId;
    }

    @Override
    public void cancelQueue(int playerId) throws PlayerNotInQueueException {

    }

    @Override
    public void submitWord(int playerId, int gameId, String word) throws WordTooShortException, InvalidWordException, InvalidLettersException, RoundNotActiveException, NotInGameException {

    }

    @Override
    public void leaveGame(int playerId, int gameId) throws NotInGameException {

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
