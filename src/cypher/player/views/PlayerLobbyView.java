package cypher.player.views;

import cypher.player.Player;
import cypher.player.views.components.fonts.FontLoader;
import cypher.server.tables.game.Game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class PlayerLobbyView extends JFrame {
    private static PlayerLobbyView instance;
    private JLabel lobbyStatusValueLabel;
    private JLabel gameIdValueLabel;
    private JLabel playersJoinedValueLabel;

    private boolean navigating = false;
    private DefaultListModel<String> lobbyListModel;
    private boolean hosting = false;
    private JButton beginButton;
    private Timer joinCountdownTimer;
    private JScrollPane listScroll;

    public PlayerLobbyView() {
        initComponents();
        instance = this;
        setVisible(true);
    }

    public static PlayerLobbyView getInstance() {
        return instance;
    }

    public void onOpponentsJoined(String[] opponents) {
        // DB refresh is authoritative; callback payload is used as immediate UI hint.
        int callbackOpponentCount = (opponents == null) ? 0 : opponents.length;
        if (callbackOpponentCount > 0) {
            setPlayersJoinedCount(callbackOpponentCount + 1);
        }
        // Callback can arrive at both host and joiners; keep countdown active for both.
        refreshLobbyPlayersFromOpponents(opponents);
        setLobbyStatus("Waiting");
        startLobbyCountdown(null);
        if (hosting && beginButton != null) {
            beginButton.setEnabled(true);
            beginButton.setVisible(true);
        }
    }

    private void initComponents() {
        setTitle("Cypher | Lobby");
        setSize(540, 540);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);
        setIconImage(new ImageIcon("src/cypher/assets/cypher_logo.png").getImage());

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Image img = new ImageIcon("src/cypher/assets/cypher_lobby.png").getImage();
                g.drawImage(img, 0, 0, getWidth(), getHeight(), this);
            }
        };
        panel.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();

        String username = Player.getPlayerUsername() == null ? "Player" : Player.getPlayerUsername();
        // set the window title to username | Lobby
        setTitle(username + " | Lobby");
        JLabel usernameLabel = new JLabel(username.toUpperCase());
        usernameLabel.setFont(FontLoader.loadFont(24f));
        usernameLabel.setForeground(Color.WHITE);

        JLabel titleLabel = new JLabel("Lobby");
        titleLabel.setFont(FontLoader.loadFont(10f));
        titleLabel.setForeground(Color.WHITE);

        JLabel statusLabel = new JLabel("Status:");
        statusLabel.setFont(FontLoader.loadFont(10f));
        statusLabel.setForeground(Color.WHITE);

        lobbyStatusValueLabel = new JLabel("Waiting");
        lobbyStatusValueLabel.setFont(FontLoader.loadFont(10f));
        lobbyStatusValueLabel.setForeground(Color.WHITE);

        // Lobby players list (Multiplayer mode)
        lobbyListModel = new DefaultListModel<>();
        JList<String> playersList = new JList<>(lobbyListModel);
        playersList.setVisibleRowCount(6);
        playersList.setFixedCellWidth(240);
        playersList.setFont(FontLoader.loadFont(12f));
        playersList.setOpaque(false);
        playersList.setForeground(Color.WHITE);
        playersList.setBackground(new Color(0, 0, 0, 0));
        playersList.setSelectionBackground(new Color(255, 16, 240, 120));
        playersList.setSelectionForeground(Color.WHITE);

        listScroll = new JScrollPane(playersList);
        listScroll.setOpaque(false);
        listScroll.getViewport().setOpaque(false);
        listScroll.setPreferredSize(new Dimension(260, 220));
        listScroll.setMinimumSize(new Dimension(260, 220));
        listScroll.setMaximumSize(new Dimension(260, 220));
        listScroll.setVisible(false);   // False for Singleplayer mode

        JButton returnButton = new JButton("Return to Main Menu");
        returnButton.setForeground(Color.WHITE);
        returnButton.setBackground(new Color(0, 128, 0));
        returnButton.setFocusPainted(false);
        returnButton.setPreferredSize(new Dimension(300, 45));
        returnButton.setMinimumSize(new Dimension(300, 45));
        returnButton.setMaximumSize(new Dimension(300, 45));
        returnButton.setFont(FontLoader.loadFont(12f));
        returnButton.addActionListener(e -> goToMainMenuOnce());

        JLabel gameIdLabel = new JLabel("Game ID:");
        gameIdLabel.setFont(FontLoader.loadFont(10f));
        gameIdLabel.setForeground(Color.WHITE);

        gameIdValueLabel = new JLabel("-");
        gameIdValueLabel.setFont(FontLoader.loadFont(10f));
        gameIdValueLabel.setForeground(Color.WHITE);

        JLabel playersJoinedLabel = new JLabel("Players Joined:");
        playersJoinedLabel.setFont(FontLoader.loadFont(10f));
        playersJoinedLabel.setForeground(Color.WHITE);

        playersJoinedValueLabel = new JLabel("0");
        playersJoinedValueLabel.setFont(FontLoader.loadFont(10f));
        playersJoinedValueLabel.setForeground(Color.WHITE);

        // Mode selection (single / multi)
        JRadioButton singleBtn = new JRadioButton("Single Player");
        JRadioButton multiBtn = new JRadioButton("Multiplayer");
        singleBtn.setOpaque(false);
        multiBtn.setOpaque(false);
        singleBtn.setForeground(Color.WHITE);
        multiBtn.setForeground(Color.WHITE);
        singleBtn.setSelected(true);
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(singleBtn);
        modeGroup.add(multiBtn);

        JButton startButton = getJButton(singleBtn, multiBtn);

        JButton joinButton = new JButton("Join Game");
        joinButton.setPreferredSize(new Dimension(120, 36));
        joinButton.setFocusPainted(false);
        joinButton.setVisible(false); // hidden initially (singleplayer default)
        joinButton.addActionListener(e -> {
            Player.findNewGame();
            if (Player.currentGame != null && Player.currentGame.gameId > 0) {
                Player.setSinglePlayerMode(false);
                hosting = false;
                showCurrentPlayerInLobby();
                updateGameIdLabel();
                setLobbyStatus("Waiting");
                startLobbyCountdown(() -> setLobbyStatus("Cancelled"));
                JOptionPane.showMessageDialog(this, "Joined game " + Player.currentGame.gameId + ". Waiting for host to begin.", "Joined", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "No available games to join.", "Join Failed", JOptionPane.WARNING_MESSAGE);
            }
        });

        singleBtn.addActionListener(e -> {
            joinButton.setVisible(false);
            startButton.setText("Start");
            setLobbyStatus("Waiting");
            listScroll.setVisible(false);
            lobbyListModel.clear();
            setPlayersJoinedCount(0);
            gameIdValueLabel.setText("-");
            stopLobbyCountdown();
            beginButton.setVisible(false);
            beginButton.setEnabled(false);
            panel.revalidate();
            panel.repaint();
        });
        multiBtn.addActionListener(e -> {
            joinButton.setVisible(true);
            startButton.setText("Host Game");
            setLobbyStatus("Waiting");
            listScroll.setVisible(true);
            showCurrentPlayerInLobby();
            updateGameIdLabel();
            panel.revalidate();
            panel.repaint();
        });

        // Layout: left = players list, right = controls
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 20, 0, 20);
        panel.add(usernameLabel, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(10, 20, 20, 20);
        panel.add(titleLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(10, 20, 10, 10);
        panel.add(listScroll, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(10, 10, 10, 20);
        panel.add(statusLabel, gbc);

        gbc.gridy = 3;
        gbc.gridx = 1;
        gbc.insets = new Insets(0, 10, 10, 20);
        panel.add(lobbyStatusValueLabel, gbc);

        gbc.gridy = 4;
        gbc.gridx = 1;
        gbc.insets = new Insets(0, 10, 10, 20);
        panel.add(gameIdLabel, gbc);

        gbc.gridy = 5;
        gbc.gridx = 1;
        gbc.insets = new Insets(0, 10, 10, 20);
        panel.add(gameIdValueLabel, gbc);

        gbc.gridy = 6;
        gbc.gridx = 1;
        gbc.insets = new Insets(0, 10, 10, 20);
        panel.add(playersJoinedLabel, gbc);

        gbc.gridy = 7;
        gbc.gridx = 1;
        gbc.insets = new Insets(0, 10, 10, 20);
        panel.add(playersJoinedValueLabel, gbc);

        gbc.gridy = 9;
        gbc.gridx = 1;
        gbc.insets = new Insets(10, 10, 6, 20);
        panel.add(singleBtn, gbc);

        gbc.gridy = 10;
        gbc.gridx = 1;
        gbc.insets = new Insets(6, 10, 6, 20);
        panel.add(multiBtn, gbc);

        gbc.gridy = 11;
        gbc.gridx = 1;
        gbc.insets = new Insets(10, 10, 6, 20);
        panel.add(startButton, gbc);

        gbc.gridy = 12;
        gbc.gridx = 1;
        gbc.insets = new Insets(6, 10, 6, 20);
        panel.add(joinButton, gbc);

        // Begin button (visible to host once opponent joins)
        beginButton = new JButton("Begin Game");
        beginButton.setPreferredSize(new Dimension(160, 36));
        beginButton.setFocusPainted(false);
        beginButton.setVisible(false);
        beginButton.setEnabled(false);
        beginButton.addActionListener(ev -> {
            if (Player.currentGame != null && Player.currentGame.gameId > 0) {
                Player.startGame(Player.currentGame.gameId);
            }
        });

        gbc.gridy = 13;
        gbc.gridx = 1;
        gbc.insets = new Insets(6, 10, 6, 20);
        panel.add(beginButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 14;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 20, 20, 20);
        panel.add(returnButton, gbc);

        setContentPane(panel);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopLobbyCountdown();
                Player.gracefulExit();
            }
        });
    }

    private JButton getJButton(JRadioButton singleBtn, JRadioButton multiBtn) {
        JButton startButton = new JButton("Start Game");
        startButton.setPreferredSize(new Dimension(160, 36));
        startButton.setFocusPainted(false);
        startButton.addActionListener(ev -> {
            if (singleBtn.isSelected()) {
                Player.setSinglePlayerMode(true);
                // Singleplayer should remain local-only and should not create a DB-backed game ID.
                Game g = new Game();
                g.gameId = 0; // local-only
                Player.currentGame = g;
                stopLobbyCountdown();
                // dispose lobby and open local in-game UI
                dispose();
                new PlayerInGameView();
                return;
            }

            Player.setSinglePlayerMode(false);
            startButton.setEnabled(false);
            multiBtn.setEnabled(false);
            singleBtn.setEnabled(false);
            setLobbyStatus("Waiting");

            SwingUtilities.invokeLater(() -> {
                Player.createNewGame();
                if (Player.currentGame == null || Player.currentGame.gameId == 0) {
                    JOptionPane.showMessageDialog(null, "Failed to create game on server.", "Error", JOptionPane.ERROR_MESSAGE);
                    startButton.setEnabled(true);
                    multiBtn.setEnabled(true);
                    singleBtn.setEnabled(true);
                    setLobbyStatus("Cancelled");
                    return;
                }

                hosting = true;
                showCurrentPlayerInLobby();
                updateGameIdLabel();
                setLobbyStatus("Waiting");

                if (instance != null) {
                    instance.beginButton.setVisible(true);
                    instance.beginButton.setEnabled(false);
                    instance.startLobbyCountdown(() -> {
                        JOptionPane.showMessageDialog(instance, "No opponent joined in time.", "Timeout", JOptionPane.INFORMATION_MESSAGE);
                        instance.beginButton.setVisible(false);
                        instance.beginButton.setEnabled(false);
                        startButton.setEnabled(true);
                        multiBtn.setEnabled(true);
                        singleBtn.setEnabled(true);
                        instance.setLobbyStatus("Cancelled");
                    });
                }
            });
        });
        return startButton;
    }

    private void goToMainMenuOnce() {
        if (navigating) return;
        navigating = true;
        stopLobbyCountdown();
        dispose();
        PlayerMainMenuView.open(Player.getPlayerUsername());
    }

    private void showCurrentPlayerInLobby() {
        if (lobbyListModel == null) return;
        lobbyListModel.clear();
        String currentUser = Player.getPlayerUsername() == null ? "Player" : Player.getPlayerUsername();
        lobbyListModel.addElement(currentUser + (Player.isHost() ? " (Host)" : " (You)"));
        setPlayersJoinedCount(lobbyListModel.getSize());
    }

    private void refreshLobbyPlayersFromOpponents(String[] opponents) {
        if (lobbyListModel == null) return;

        showCurrentPlayerInLobby();
        if (opponents != null) {
            for (String opponent : opponents) {
                if (opponent == null || opponent.trim().isEmpty()) continue;
                lobbyListModel.addElement(opponent.trim());
            }
        }

        setPlayersJoinedCount(lobbyListModel.getSize());
        listScroll.setVisible(true);
        listScroll.revalidate();
        listScroll.repaint();

        if (lobbyListModel.getSize() > 1 && beginButton != null && hosting) {
            beginButton.setEnabled(true);
        }
    }

    private void startLobbyCountdown(Runnable onTimeout) {
        stopLobbyCountdown();
        joinCountdownTimer = new Timer(1000, e -> {
            int remaining = Player.getWaitingTime();
            if (remaining < 0) {
                return;
            }

            if (remaining == 0) {
                stopLobbyCountdown();
                if (onTimeout != null) onTimeout.run();
            }
        });
        joinCountdownTimer.setInitialDelay(0);
        joinCountdownTimer.start();
    }

    private void stopLobbyCountdown() {
        if (joinCountdownTimer != null && joinCountdownTimer.isRunning()) {
            joinCountdownTimer.stop();
        }
    }

    private void setLobbyStatus(String status) {
        if (lobbyStatusValueLabel != null) {
            lobbyStatusValueLabel.setText(status == null ? "" : status);
        }
    }

    private void updateGameIdLabel() {
        if (gameIdValueLabel == null) return;
        if (Player.currentGame != null && Player.currentGame.gameId > 0) {
            gameIdValueLabel.setText(String.valueOf(Player.currentGame.gameId));
        } else {
            gameIdValueLabel.setText("-");
        }
    }

    private void setPlayersJoinedCount(int count) {
        if (playersJoinedValueLabel != null) {
            playersJoinedValueLabel.setText(String.valueOf(Math.max(count, 0)));
        }
    }
}
