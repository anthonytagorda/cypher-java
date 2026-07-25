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
    private JLabel countdownLabel;

    private int waitingTime;
    private Timer timer;
    private boolean navigating = false;
    private DefaultListModel<String> lobbyListModel;
    private Timer joinPollTimer;
    private boolean hosting = false;
    private JButton beginButton;
    private Timer joinCountdownTimer;
    private int joinCountdownSecs = 30; // seconds to wait for opponent
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
        if (opponents == null) return;
        // stop join countdown if running
        try {
            if (joinCountdownTimer != null && joinCountdownTimer.isRunning()) joinCountdownTimer.stop();
        } catch (Exception ignored) {
        }
        lobbyListModel.clear();
        String username = Player.getPlayerUsername() == null ? "Player" : Player.getPlayerUsername();
        lobbyListModel.addElement(username + (hosting ? " (Host)" : " (You)"));
        for (String o : opponents) {
            lobbyListModel.addElement(o);
        }
        countdownLabel.setText("Opponent(s) joined");
        if (hosting && beginButton != null) {
            beginButton.setEnabled(true);
            beginButton.setVisible(true);
        }
    }

    private void startTimer() {
        countdownLabel.setText(String.valueOf(waitingTime));

        timer = new Timer(1000, e -> {
            waitingTime--;
            countdownLabel.setText(String.valueOf(Math.max(waitingTime, 0)));

            if (waitingTime <= 0) {
                stopTimer();
                dispose();
                new PlayerInGameView();
            }
        });

        timer.start();
    }

    private void stopTimer() {
        if (timer != null && timer.isRunning()) {
            timer.stop();
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

        JLabel timerLabel = new JLabel("Status:");
        timerLabel.setFont(FontLoader.loadFont(10f));
        timerLabel.setForeground(Color.WHITE);

        countdownLabel = new JLabel("--");
        countdownLabel.setFont(FontLoader.loadFont(10f));
        countdownLabel.setForeground(Color.WHITE);

        // Lobby players list (Multiplayer mode)
        lobbyListModel = new DefaultListModel<>();
        JList<String> playersList = new JList<>(lobbyListModel);
        playersList.setVisibleRowCount(6);
        playersList.setFixedCellWidth(200);
        playersList.setFont(FontLoader.loadFont(12f));
        playersList.setOpaque(false);

        listScroll = new JScrollPane(playersList);
        listScroll.setOpaque(false);
        listScroll.getViewport().setOpaque(false);
        listScroll.setPreferredSize(new Dimension(220, 160));
        listScroll.setVisible(false);   // False for Singleplayer mode

        JButton returnButton = new JButton("Return to Main Menu");
        returnButton.setForeground(Color.WHITE);
        returnButton.setBackground(new Color(0, 128, 0));
        returnButton.setFocusPainted(false);
        returnButton.setPreferredSize(new Dimension(300, 45));
        returnButton.setFont(FontLoader.loadFont(12f));
        returnButton.addActionListener(e -> goToMainMenuOnce());

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

        JButton startButton = getJButton(singleBtn, multiBtn, username);

        JButton joinButton = new JButton("Join Game");
        joinButton.setPreferredSize(new Dimension(120, 36));
        joinButton.setFocusPainted(false);
        joinButton.setVisible(false); // hidden initially (singleplayer default)
        joinButton.addActionListener(e -> {
            // Attempt to join an existing waiting game
            Player.findNewGame();
            if (Player.currentGame != null && Player.currentGame.gameId > 0) {
                Player.setSinglePlayerMode(false);
                // joined successfully: update lobby list and wait for host to begin
                lobbyListModel.clear();
                // fetch players in this game from server if available (server will notify via callback too)
                lobbyListModel.addElement(Player.getPlayerUsername() + " (Joined)");
                JOptionPane.showMessageDialog(this, "Joined game " + Player.currentGame.gameId + ". Waiting for host to begin.", "Joined", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "No available games to join.", "Join Failed", JOptionPane.WARNING_MESSAGE);
            }
        });

        // Toggle visibility and button labels when switching modes
        singleBtn.addActionListener(e -> {
            joinButton.setVisible(false);
            startButton.setText("Start");
            countdownLabel.setText("Singleplayer");
            // hide lobby list in singleplayer
            listScroll.setVisible(false);
            lobbyListModel.clear();
        });
        multiBtn.addActionListener(e -> {
            joinButton.setVisible(true);
            startButton.setText("Host Game");
            countdownLabel.setText("--");
            // show lobby list in multiplayer
            listScroll.setVisible(true);
            lobbyListModel.clear();
            lobbyListModel.addElement(username + " (You)");
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
        gbc.insets = new Insets(10, 20, 10, 10);
        panel.add(listScroll, gbc);

        gbc.gridx = 1;
        gbc.insets = new Insets(10, 10, 10, 20);
        panel.add(timerLabel, gbc);

        gbc.gridy = 3;
        gbc.gridx = 1;
        gbc.insets = new Insets(0, 10, 10, 20);
        panel.add(countdownLabel, gbc);

        gbc.gridy = 4;
        gbc.gridx = 1;
        gbc.insets = new Insets(10, 10, 6, 20);
        panel.add(singleBtn, gbc);

        gbc.gridy = 5;
        gbc.gridx = 1;
        gbc.insets = new Insets(6, 10, 6, 20);
        panel.add(multiBtn, gbc);

        gbc.gridy = 6;
        gbc.gridx = 1;
        gbc.insets = new Insets(10, 10, 6, 20);
        panel.add(startButton, gbc);

        gbc.gridy = 7;
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
            // host starts the game explicitly
            if (Player.currentGame != null && Player.currentGame.gameId > 0) {
                Player.startGame(Player.currentGame.gameId);
            }
        });

        gbc.gridy = 8;
        gbc.gridx = 1;
        gbc.insets = new Insets(6, 10, 6, 20);
        panel.add(beginButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 20, 20, 20);
        panel.add(returnButton, gbc);

        setContentPane(panel);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopTimer();
                Player.gracefulExit();
            }
        });
    }

    private JButton getJButton(JRadioButton singleBtn, JRadioButton multiBtn, String username) {
        JButton startButton = new JButton("Start Game");
        startButton.setPreferredSize(new Dimension(160, 36));
        startButton.setFocusPainted(false);
        startButton.addActionListener(ev -> {
            if (singleBtn.isSelected()) {
                Player.setSinglePlayerMode(true);
                // Singleplayer: attempt to create a server-backed game so it is persisted in DB.
                // If server creation/start fails, fall back to local-only mode.
                try {
                    Player.createNewGame();
                    if (Player.currentGame != null && Player.currentGame.gameId > 0) {
                        // request server to start the game so started_at/status are set
                        Player.startGame(Player.currentGame.gameId);
                        // rely on server callback (sendLetters) to open the in-game view for all players
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Fallback: If server is unavailable or creation failed, run local-only game
                Game g = new Game();
                g.gameId = 0; // local-only
                Player.currentGame = g;
                // dispose lobby and open local in-game UI
                dispose();
                new PlayerInGameView();
                return;
            }

            // Multiplayer flow: create game on server and wait for opponent. Host must explicitly begin.
            Player.setSinglePlayerMode(false);
            startButton.setEnabled(false);
            multiBtn.setEnabled(false);
            singleBtn.setEnabled(false);
            countdownLabel.setText("Creating game...");

            // Create game on server (adds DB entry)
            SwingUtilities.invokeLater(() -> {
                Player.createNewGame();
                if (Player.currentGame == null || Player.currentGame.gameId == 0) {
                    JOptionPane.showMessageDialog(null, "Failed to create game on server.", "Error", JOptionPane.ERROR_MESSAGE);
                    startButton.setEnabled(true);
                    multiBtn.setEnabled(true);
                    singleBtn.setEnabled(true);
                    countdownLabel.setText("--");
                    return;
                }

                hosting = true;
                lobbyListModel.clear();
                lobbyListModel.addElement(username + " (Host)");
                countdownLabel.setText("Waiting for opponent...");

                // Show the Begin button so host can start when ready (it will be enabled when opponent joins via callback)
                if (instance != null) {
                    instance.beginButton.setVisible(true);
                    instance.beginButton.setEnabled(false);
                    // start join countdown
                    if (instance.joinCountdownTimer != null && instance.joinCountdownTimer.isRunning())
                        instance.joinCountdownTimer.stop();
                    instance.joinCountdownSecs = 30;
                    instance.joinCountdownTimer = new Timer(1000, ev2 -> {
                        instance.joinCountdownSecs--;
                        instance.countdownLabel.setText("Waiting: " + instance.joinCountdownSecs + "s");
                        if (instance.joinCountdownSecs <= 0) {
                            instance.joinCountdownTimer.stop();
                            JOptionPane.showMessageDialog(instance, "No opponent joined in time.", "Timeout", JOptionPane.INFORMATION_MESSAGE);
                            // reset UI
                            instance.beginButton.setVisible(false);
                            instance.beginButton.setEnabled(false);
                            startButton.setEnabled(true);
                            multiBtn.setEnabled(true);
                            singleBtn.setEnabled(true);
                            instance.countdownLabel.setText("--");
                        }
                    });
                    instance.joinCountdownTimer.setInitialDelay(0);
                    instance.joinCountdownTimer.start();
                }
            });
        });
        return startButton;
    }

    private void goToMainMenuOnce() {
        if (navigating) return;
        navigating = true;
        stopTimer();
        dispose();
        PlayerMainMenuView.open(Player.getPlayerUsername());
    }
}