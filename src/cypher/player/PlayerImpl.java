package cypher.player;

import cypher.player.app.PlayerAppPOA;
import cypher.player.views.*;

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
                PlayerLobbyView lobby = PlayerLobbyView.getInstance();
                if (lobby != null) {
                    try {
                        lobby.dispose();
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception ignored) {
            }

            if (inGameView == null || !inGameView.isDisplayable()) {
                inGameView = new PlayerInGameView();
            }
            inGameView.setVisible(true);
            Player.setCurrentGameStatus("IN PROGRESS");
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
                new PlayerWinnerView(gameWinner, 0);
            }
            // Clear local game state after presenting final server result
            Player.clearLocalGameState();
        });
    }

    @Override
    public void waitingForPlayers(int timeout) {
        System.out.println("Waiting for players: " + timeout + "s");
    }

    @Override
    public void gameFound(int gameId, String[] opponents) {
        SwingUtilities.invokeLater(() -> {
            try {
                PlayerLobbyView lobby = PlayerLobbyView.getInstance();
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
        SwingUtilities.invokeLater(() -> PlayerMainMenuView.open(playerUsername));
    }

    @Override
    public void notifyBanned() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                    null,
                    "You have been banned. Logging you out...",
                    "Banned",
                    JOptionPane.ERROR_MESSAGE
            );
            Player.forceLogoutAfterBan();
        });
    }

    @Override
    public void notifyGameStopped() {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Game stopped by server.", "Stopped", JOptionPane.INFORMATION_MESSAGE));
        new PlayerLoaderView();
    }

    @Override
    public void playerDisconnected(String playerName) {
        SwingUtilities.invokeLater(() -> {
            String message;
            boolean returnToMenu = false;

            if (playerName == null || playerName.trim().isEmpty()) {
                message = "A player has disconnected from the game.";
            } else if (playerName.startsWith("HOST_LEFT:")) {
                String hostName = playerName.substring("HOST_LEFT:".length()).trim();
                message = hostName.isEmpty() ? "The host left the game." : hostName + " left the game.";
                returnToMenu = true;
            } else if (playerName.startsWith("KICKED_BY_HOST:")) {
                String hostName = playerName.substring("KICKED_BY_HOST:".length()).trim();
                message = hostName.isEmpty() ? "You were kicked from the game." : "You were kicked by " + hostName + ".";
                returnToMenu = true;
            } else {
                message = playerName + " has disconnected from the game.";
            }

            JOptionPane.showMessageDialog(null,
                                          message,
                                          "Player Disconnected",
                                          JOptionPane.WARNING_MESSAGE);

            if (returnToMenu) {
                Player.clearLocalGameState();
                PlayerLobbyView lobby = PlayerLobbyView.getInstance();
                if (lobby != null) {
                    try {
                        lobby.dispose();
                    } catch (Exception ignored) {
                    }
                }
                if (inGameView != null) {
                    try {
                        inGameView.dispose();
                    } catch (Exception ignored) {
                    }
                    inGameView = null;
                }
                PlayerMainMenuView.open(playerUsername);
            }
        });
    }
}