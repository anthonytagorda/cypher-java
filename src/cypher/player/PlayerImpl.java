package cypher.player;

import cypher.player.app.PlayerAppPOA;
import cypher.player.views.PlayerInGameView;
import cypher.player.views.PlayerLoaderView;
import cypher.player.views.PlayerMainMenuView;

import javax.swing.*;
import java.util.Arrays;

public class PlayerImpl extends PlayerAppPOA {

    private PlayerInGameView inGameView;
    private String playerUsername;

    public void setPlayerUsername(String playerUsername) {
        this.playerUsername = playerUsername;
    }

    @Override
    public void sendLetters(String letters) {
        SwingUtilities.invokeLater(() -> {
            try {
                // dispose lobby if open
                cypher.player.views.PlayerLobbyView lobby = cypher.player.views.PlayerLobbyView.getInstance();
                if (lobby != null) {
                    try {
                        lobby.dispose();
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}

            if (inGameView == null || !inGameView.isDisplayable()) {
                inGameView = new PlayerInGameView();
            }
            inGameView.setVisible(true);
            inGameView.generateGameBoard(letters);
        });
    }

    @Override
    public void invalidWord(String word, String reason) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                                                                       "Invalid word: " + word + "\nReason: " + reason,
                                                                       "Invalid Word",
                                                                       JOptionPane.WARNING_MESSAGE));
    }

    @Override
    public void wordAccepted(String word) {

    }

    @Override
    public void sendRoundResult(String[] usernames, int[] roundScores, String[] longestWord, String roundWinner, int roundNumber) {
        System.out.println("Round " + roundNumber + " winner: " + roundWinner);
        System.out.println(Arrays.toString(usernames));
        System.out.println(Arrays.toString(roundScores));
        System.out.println(Arrays.toString(longestWord));

        SwingUtilities.invokeLater(() -> {
            if (inGameView == null) {
                inGameView = new PlayerInGameView();
                inGameView.setVisible(true);
            }
            inGameView.showRoundResult(roundWinner, false); // notify the view about round result; not a full game end
        });
    }

    @Override
    public void sendGameResult(String[] usernames, int[] roundWins, int[] totalScores, String gameWinner) {
        SwingUtilities.invokeLater(() -> {
            if (inGameView != null) {
                inGameView.showServerFinalGameResult(gameWinner);
                inGameView = null;
            } else {
                JOptionPane.showMessageDialog(null,
                        gameWinner.toUpperCase() + " won the game!",
                        "Game Over",
                        JOptionPane.INFORMATION_MESSAGE);
                new cypher.player.views.PlayerWinnerView(gameWinner, 0);
            }
            // Clear local game state after presenting final server result
            cypher.player.Player.clearLocalGameState();
        });
    }

    @Override
    public void waitingForPlayers(int timeout) {
        System.out.println("Waiting for players: " + timeout + "s");
    }

    @Override
    public void gameFound(int gameId, String[] opponents) {
        System.out.println("Game found: " + gameId + " opponents=" + Arrays.toString(opponents));

        SwingUtilities.invokeLater(() -> {
            try {
                cypher.player.views.PlayerLobbyView lobby = cypher.player.views.PlayerLobbyView.getInstance();
                if (lobby != null) {
                    lobby.onOpponentsJoined(opponents);
                } else {
                    JOptionPane.showMessageDialog(null, "Opponent(s) joined: " + Arrays.toString(opponents), "Players Joined", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void noPlayersJoined() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null,
                                          "No other players joined your game. Please start another game",
                                          "Game Cancelled",
                                          JOptionPane.WARNING_MESSAGE);
            new PlayerMainMenuView(playerUsername);
        });
    }

    @Override
    public void notifyBanned() {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "You are banned.", "Banned", JOptionPane.ERROR_MESSAGE));
    }

    @Override
    public void notifyGameStopped() {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Game stopped by server.", "Stopped", JOptionPane.INFORMATION_MESSAGE));
        new PlayerLoaderView();
    }
}