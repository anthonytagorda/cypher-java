package cypher.player.views;

import cypher.player.Player;
import cypher.player.views.components.fonts.FontLoader;
import cypher.server.tables.game.Game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class PlayerLobbyView extends JFrame {

    // ── Instance ──────────────────────────────────────────────────────────────
    private static PlayerLobbyView instance;

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean navigating = false;
    private boolean hosting = false;

    // ── Labels ────────────────────────────────────────────────────────────────
    private JLabel lobbyStatusLabel;
    private JLabel lobbyStatusValueLabel;
    private JLabel gameIdLabel;
    private JLabel gameIdValueLabel;
    private JLabel playersJoinedLabel;
    private JLabel playersJoinedValueLabel;

    // ── Buttons ───────────────────────────────────────────────────────────────
    private JButton actionButton;   // "Start" / "Host Game"
    private JButton joinButton;
    private JButton beginButton;
    private JButton kickButton;

    // ── Player list ───────────────────────────────────────────────────────────
    private JScrollPane listScroll;
    private JList<String> playersList;
    private DefaultListModel<String> lobbyListModel;
    private final List<Integer> lobbyPlayerIds = new ArrayList<>();

    // ── Layout ────────────────────────────────────────────────────────────────
    private JPanel lobbyPanel;

    // ── Timer ─────────────────────────────────────────────────────────────────
    private Timer joinCountdownTimer;

    // ─────────────────────────────────────────────────────────────────────────
    public PlayerLobbyView() {
        initComponents();
        instance = this;
        setVisible(true);
    }

    public static PlayerLobbyView getInstance() {
        return instance;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public callbacks (called by PlayerCallbackImpl)
    // ─────────────────────────────────────────────────────────────────────────
    public void onOpponentsJoined(String[] opponents) {
        refreshLobbyPlayersFromOpponents(opponents);
        setLobbyStatus("Waiting");
        startLobbyCountdown(null);

        if (hosting) {
            beginButton.setVisible(true);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setSize(540, 540);
        setResizable(false);
        setLocationRelativeTo(null);
        setIconImage(new ImageIcon("src/cypher/assets/cypher_logo.png").getImage());

        String username = Player.getPlayerUsername() == null
                ? "Player"
                : Player.getPlayerUsername();

        setTitle(username + " | Lobby");

        // ── Background panel ─────────────────────────────────────────────────
        lobbyPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                g.drawImage(
                        new ImageIcon("src/cypher/assets/cypher_lobby.png").getImage(),
                        0,
                        0,
                        getWidth(),
                        getHeight(),
                        this
                );
            }
        };

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;

        // ── Header ───────────────────────────────────────────────────────────
        JLabel usernameLabel = makeLabel(
                username.toUpperCase(),
                24f,
                true
        );

        JLabel titleLabel = makeLabel(
                "Lobby",
                10f,
                false
        );

        // ── Info labels ──────────────────────────────────────────────────────
        lobbyStatusLabel = makeLabel(
                "Status",
                10f,
                true
        );

        lobbyStatusValueLabel = makeLabel(
                "Waiting",
                10f,
                false
        );

        gameIdLabel = makeLabel(
                "Game ID",
                10f,
                true
        );

        gameIdValueLabel = makeLabel(
                "-",
                10f,
                false
        );

        playersJoinedLabel = makeLabel(
                "Players joined",
                10f,
                true
        );

        playersJoinedValueLabel = makeLabel(
                "0",
                10f,
                false
        );

        hideInfoLabels();

        // ── Mode radio buttons ───────────────────────────────────────────────
        JRadioButton singleBtn = makeRadio("Single player");
        JRadioButton multiBtn = makeRadio("Multiplayer");

        singleBtn.setSelected(true);

        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(singleBtn);
        modeGroup.add(multiBtn);

        // ── Action buttons ──────────────────────────────────────────────────
        actionButton = makeButton(
                "Start",
                160,
                36
        );

        joinButton = makeButton(
                "Join game",
                160,
                36
        );

        beginButton = makeButton(
                "Begin game",
                260,
                36
        );

        kickButton = makeButton(
                "Kick player",
                100,
                26
        );

        joinButton.setVisible(false);

        beginButton.setVisible(false);
        beginButton.setEnabled(false);

        kickButton.setVisible(false);
        kickButton.setEnabled(false);

        // ── Return button ────────────────────────────────────────────────────
        JButton returnButton = makeButton(
                "Return to main menu",
                300,
                45
        );

        returnButton.setForeground(Color.WHITE);
        returnButton.setBackground(new Color(0, 128, 0));

        // ── Player list ──────────────────────────────────────────────────────
        lobbyListModel = new DefaultListModel<>();

        playersList = new JList<>(lobbyListModel);

        playersList.setVisibleRowCount(6);
        playersList.setFixedCellWidth(240);
        playersList.setFont(FontLoader.loadFont(12f));

        playersList.setOpaque(false);
        playersList.setForeground(Color.WHITE);
        playersList.setBackground(new Color(0, 0, 0, 0));

        playersList.setSelectionBackground(
                new Color(255, 16, 240, 120)
        );

        playersList.setSelectionForeground(Color.WHITE);

        playersList.setBorder(
                BorderFactory.createEmptyBorder(
                        10,
                        10,
                        10,
                        10
                )
        );

        playersList.setEnabled(false);

        playersList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateLobbyActionButtons();
            }
        });

        listScroll = new JScrollPane(playersList);

        listScroll.setOpaque(false);
        listScroll.getViewport().setOpaque(false);

        listScroll.setPreferredSize(
                new Dimension(260, 220)
        );

        listScroll.setMinimumSize(
                new Dimension(260, 220)
        );

        listScroll.setMaximumSize(
                new Dimension(260, 220)
        );

        listScroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        listScroll.setVisible(false);

        listScroll.setBorder(
                BorderFactory.createLineBorder(
                        new Color(255, 255, 255, 40),
                        1
                )
        );

        // ── Action listeners ─────────────────────────────────────────────────
        singleBtn.addActionListener(
                e -> onSinglePlayerSelected()
        );

        multiBtn.addActionListener(
                e -> onMultiplayerSelected()
        );

        actionButton.addActionListener(
                e -> onActionButtonClicked(
                        singleBtn,
                        multiBtn
                )
        );

        joinButton.addActionListener(
                e -> onJoinButtonClicked(
                        multiBtn,
                        singleBtn
                )
        );

        beginButton.addActionListener(
                e -> onBeginButtonClicked()
        );

        kickButton.addActionListener(
                e -> handleKickPlayer()
        );

        returnButton.addActionListener(
                e -> goToMainMenuOnce()
        );

        // ── Layout ───────────────────────────────────────────────────────────

        // Row 0-1: Header
        addGbc(
                lobbyPanel,
                usernameLabel,
                gbc,
                0,
                0,
                2,
                new Insets(20, 20, 0, 20)
        );

        addGbc(
                lobbyPanel,
                titleLabel,
                gbc,
                0,
                1,
                2,
                new Insets(4, 20, 16, 20)
        );

        // ── Left column: Lobby / Player list ─────────────────────────────────
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.gridheight = 8;

        // Give the left column its own horizontal space.
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        gbc.insets = new Insets(10, 30, 10, 10);

        lobbyPanel.add(
                listScroll,
                gbc
        );

        // Reset constraints for the rest of the layout.
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;

        // Kick player
        gbc.anchor = GridBagConstraints.NORTHWEST;
        addGbc(
                lobbyPanel,
                kickButton,
                gbc,
                0,
                10,
                1,
                new Insets(4, 30, 4, 10)
        );

        gbc.anchor = GridBagConstraints.CENTER;

        // Begin Game
        addGbc(
                lobbyPanel,
                beginButton,
                gbc,
                0,
                11,
                1,
                new Insets(6, 20, 4, 10)
        );

        // Return to Main Menu
        addGbc(
                lobbyPanel,
                returnButton,
                gbc,
                0,
                13,
                2,
                new Insets(20, 20, 20, 20)
        );

        // ── Right column: Info ───────────────────────────────────────────────

        addGbc(
                lobbyPanel,
                lobbyStatusLabel,
                gbc,
                1,
                2,
                1,
                new Insets(10, 30, 2, 20)
        );

        addGbc(
                lobbyPanel,
                lobbyStatusValueLabel,
                gbc,
                1,
                3,
                1,
                new Insets(0, 30, 10, 20)
        );

        addGbc(
                lobbyPanel,
                gameIdLabel,
                gbc,
                1,
                4,
                1,
                new Insets(0, 30, 2, 20)
        );

        addGbc(
                lobbyPanel,
                gameIdValueLabel,
                gbc,
                1,
                5,
                1,
                new Insets(0, 30, 10, 20)
        );

        addGbc(
                lobbyPanel,
                playersJoinedLabel,
                gbc,
                1,
                6,
                1,
                new Insets(0, 30, 2, 20)
        );

        addGbc(
                lobbyPanel,
                playersJoinedValueLabel,
                gbc,
                1,
                7,
                1,
                new Insets(0, 30, 10, 20)
        );

        // ── Initial mode controls ────────────────────────────────────────────
        // These occupy rows 8 and 9.
        addGbc(
                lobbyPanel,
                singleBtn,
                gbc,
                1,
                8,
                1,
                new Insets(10, 5, 4, 30)
        );

        addGbc(
                lobbyPanel,
                multiBtn,
                gbc,
                1,
                9,
                1,
                new Insets(4, 5, 4, 30)
        );

        // Host / Start and Join buttons start below the radio buttons.
        addGbc(
                lobbyPanel,
                actionButton,
                gbc,
                1,
                10,
                1,
                new Insets(10, 5, 4, 30)
        );

        addGbc(
                lobbyPanel,
                joinButton,
                gbc,
                1,
                11,
                1,
                new Insets(4, 5, 4, 30)
        );

        // ── Return button ────────────────────────────────────────────────────
        addGbc(
                lobbyPanel,
                beginButton,
                gbc,
                0,
                11,
                2,
                new Insets(10, 0, 10, 0)
        );

        setContentPane(lobbyPanel);

        addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        goToMainMenuOnce();
                    }
                }
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Radio button handlers
    // ─────────────────────────────────────────────────────────────────────────

    private void onSinglePlayerSelected() {
        actionButton.setText("Start");

        joinButton.setVisible(false);
        joinButton.setEnabled(true);

        listScroll.setVisible(false);

        lobbyListModel.clear();
        lobbyPlayerIds.clear();

        playersList.setEnabled(false);

        beginButton.setVisible(false);
        beginButton.setEnabled(false);

        kickButton.setVisible(false);
        kickButton.setEnabled(false);

        hideInfoLabels();

        setPlayersJoinedCount(0);

        stopLobbyCountdown();

        revalidatePanel();
    }

    private void onMultiplayerSelected() {
        actionButton.setText("Host game");

        joinButton.setVisible(true);
        joinButton.setEnabled(true);

        listScroll.setVisible(false);

        playersList.setEnabled(false);

        hideInfoLabels();

        updateLobbyActionButtons();

        revalidatePanel();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Action button: Start / Host Game
    // ─────────────────────────────────────────────────────────────────────────

    private void onActionButtonClicked(
            JRadioButton singleBtn,
            JRadioButton multiBtn) {

        if (singleBtn.isSelected()) {

            // ── Single player ────────────────────────────────────────────────
            Player.setSinglePlayerMode(true);

            Game g = new Game();
            g.gameId = 0;

            Player.currentGame = g;

            stopLobbyCountdown();

            dispose();

            new PlayerInGameView();

            return;
        }

        // ── Host game (multiplayer) ──────────────────────────────────────────
        Player.setSinglePlayerMode(false);

        // Disable the mode controls first.
        lockModeControls(
                singleBtn,
                multiBtn
        );

        // Remove the radio buttons.
        singleBtn.setVisible(false);
        multiBtn.setVisible(false);

        // Move Host Game / Join Game into their positions.
        moveActionButtonsToRadioPosition();

        setLobbyStatus("Waiting");

        SwingUtilities.invokeLater(() -> {

            Player.createNewGame();

            if (Player.currentGame == null
                    || Player.currentGame.gameId == 0) {

                JOptionPane.showMessageDialog(
                        this,
                        "Failed to create game on server.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );

                unlockModeControls(
                        singleBtn,
                        multiBtn
                );

                // Restore radio buttons if hosting failed.
                singleBtn.setVisible(true);
                multiBtn.setVisible(true);

                restoreActionButtonPositions();

                setLobbyStatus("Cancelled");

                return;
            }

            hosting = true;

            showInfoLabels();

            playersList.setEnabled(true);
            listScroll.setVisible(true);

            updateGameIdLabel();

            setLobbyStatus("Waiting");

            updateLobbyActionButtons();

            beginButton.setVisible(true);
            beginButton.setEnabled(true);

            kickButton.setVisible(true);

            startLobbyCountdown(() -> {

                JOptionPane.showMessageDialog(
                        instance,
                        "No opponent joined in time.",
                        "Timeout",
                        JOptionPane.INFORMATION_MESSAGE
                );

                beginButton.setVisible(false);
                beginButton.setEnabled(false);

                kickButton.setVisible(false);
                kickButton.setEnabled(false);

                unlockModeControls(
                        singleBtn,
                        multiBtn
                );

                // Restore the radio buttons.
                singleBtn.setVisible(true);
                multiBtn.setVisible(true);

                // Put Host / Join back below them.
                restoreActionButtonPositions();

                setLobbyStatus("Cancelled");

                revalidatePanel();
            });

            revalidatePanel();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Join button
    // ─────────────────────────────────────────────────────────────────────────

    private void onJoinButtonClicked(
            JRadioButton multiBtn,
            JRadioButton singleBtn) {

        Player.findNewGame();

        if (Player.currentGame == null
                || Player.currentGame.gameId <= 0) {

            JOptionPane.showMessageDialog(
                    this,
                    "No available games to join or you are already in a game.",
                    "Join failed",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        Player.setSinglePlayerMode(false);

        hosting = false;

        showInfoLabels();

        playersList.setEnabled(true);
        listScroll.setVisible(true);

        lockModeControls(
                singleBtn,
                multiBtn
        );

        // Hide the radio buttons.
        singleBtn.setVisible(false);
        multiBtn.setVisible(false);

        // Move Host / Join buttons into their positions.
        moveActionButtonsToRadioPosition();

        updateGameIdLabel();

        setLobbyStatus("Waiting");

        startLobbyCountdown(() -> {

            setLobbyStatus("Cancelled");

            unlockModeControls(
                    singleBtn,
                    multiBtn
            );

            // Restore the radio buttons.
            singleBtn.setVisible(true);
            multiBtn.setVisible(true);

            // Restore Host / Join buttons to their original rows.
            restoreActionButtonPositions();

            revalidatePanel();
        });

        JOptionPane.showMessageDialog(
                this,
                "Joined game "
                        + Player.currentGame.gameId
                        + ". Waiting for the host to begin.",
                "Joined",
                JOptionPane.INFORMATION_MESSAGE
        );

        revalidatePanel();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Move Host / Join buttons to radio button positions
    // ─────────────────────────────────────────────────────────────────────────

    private void moveActionButtonsToRadioPosition() {

        if (lobbyPanel == null) {
            return;
        }

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;

        // Host Game replaces "Single player"
        addGbc(
                lobbyPanel,
                actionButton,
                gbc,
                1,
                8,
                1,
                new Insets(10, 10, 4, 20)
        );

        // Join Game replaces "Multiplayer"
        addGbc(
                lobbyPanel,
                joinButton,
                gbc,
                1,
                9,
                1,
                new Insets(4, 10, 4, 20)
        );

        actionButton.setVisible(true);
        joinButton.setVisible(true);

        revalidatePanel();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Restore original button positions
    // ─────────────────────────────────────────────────────────────────────────

    private void restoreActionButtonPositions() {

        if (lobbyPanel == null) {
            return;
        }

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;

        // Start / Host Game returns to row 10.
        addGbc(
                lobbyPanel,
                actionButton,
                gbc,
                1,
                10,
                1,
                new Insets(10, 10, 4, 20)
        );

        // Join Game returns to row 11.
        addGbc(
                lobbyPanel,
                joinButton,
                gbc,
                1,
                11,
                1,
                new Insets(4, 10, 4, 20)
        );

        revalidatePanel();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Begin button
    // ─────────────────────────────────────────────────────────────────────────

    private void onBeginButtonClicked() {

        if (Player.currentGame == null
                || Player.currentGame.gameId <= 0) {

            return;
        }

        if (lobbyListModel.getSize() < 2) {

            JOptionPane.showMessageDialog(
                    this,
                    "At least 2 players are required to start a multiplayer game.",
                    "Cannot start game",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        Player.startGame(
                Player.currentGame.gameId
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Button state helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void lockModeControls(
            JRadioButton singleBtn,
            JRadioButton multiBtn) {

        actionButton.setEnabled(false);
        joinButton.setEnabled(false);

        multiBtn.setEnabled(false);
        singleBtn.setEnabled(false);
    }

    private void unlockModeControls(
            JRadioButton singleBtn,
            JRadioButton multiBtn) {

        actionButton.setEnabled(true);
        joinButton.setEnabled(true);

        multiBtn.setEnabled(true);
        singleBtn.setEnabled(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Info labels
    // ─────────────────────────────────────────────────────────────────────────

    private void showInfoLabels() {

        lobbyStatusLabel.setVisible(true);
        lobbyStatusValueLabel.setVisible(true);

        gameIdLabel.setVisible(true);
        gameIdValueLabel.setVisible(true);

        playersJoinedLabel.setVisible(true);
        playersJoinedValueLabel.setVisible(true);
    }

    private void hideInfoLabels() {

        lobbyStatusLabel.setVisible(false);
        lobbyStatusValueLabel.setVisible(false);

        gameIdLabel.setVisible(false);
        gameIdValueLabel.setVisible(false);

        playersJoinedLabel.setVisible(false);
        playersJoinedValueLabel.setVisible(false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Revalidate
    // ─────────────────────────────────────────────────────────────────────────

    private void revalidatePanel() {

        Container contentPane =
                getContentPane();

        if (contentPane instanceof JPanel) {

            JPanel p =
                    (JPanel) contentPane;

            p.revalidate();
            p.repaint();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Factory helpers
    // ─────────────────────────────────────────────────────────────────────────

    private JLabel makeLabel(
            String text,
            float size,
            boolean bold) {

        JLabel label =
                new JLabel(text);

        label.setFont(
                bold
                        ? FontLoader.loadFont(size)
                        .deriveFont(Font.BOLD)
                        : FontLoader.loadFont(size)
        );

        label.setForeground(Color.WHITE);

        return label;
    }

    private JButton makeButton(
            String text,
            int w,
            int h) {

        JButton btn =
                new JButton(text);

        btn.setPreferredSize(
                new Dimension(w, h)
        );

        btn.setFocusPainted(false);

        return btn;
    }

    private JRadioButton makeRadio(
            String text) {

        JRadioButton rb =
                new JRadioButton(text);

        rb.setOpaque(false);
        rb.setForeground(Color.WHITE);

        return rb;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GridBag helper
    // ─────────────────────────────────────────────────────────────────────────

    private void addGbc(
            JPanel panel,
            Component comp,
            GridBagConstraints gbc,
            int gridx,
            int gridy,
            int gridwidth,
            Insets insets) {

        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.gridwidth = gridwidth;
        gbc.gridheight = 1;
        gbc.insets = insets;

        panel.add(
                comp,
                gbc
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lobby player list
    // ─────────────────────────────────────────────────────────────────────────

    private void showCurrentPlayerInLobby(
            String[] opponents) {

        if (lobbyListModel == null) {
            return;
        }

        lobbyListModel.clear();
        lobbyPlayerIds.clear();

        String me =
                Player.getPlayerUsername() == null
                        ? "Player"
                        : Player.getPlayerUsername();

        if (Player.isHost()) {

            lobbyListModel.addElement(
                    me + " (Host)"
            );

            lobbyPlayerIds.add(-1);

            if (opponents != null) {

                for (String opp : opponents) {

                    LobbyEntry e =
                            decodeLobbyEntry(opp);

                    if (e == null
                            || e.username == null
                            || e.username.isEmpty()) {

                        continue;
                    }

                    lobbyListModel.addElement(
                            e.username.trim()
                    );

                    lobbyPlayerIds.add(
                            e.playerId
                    );
                }
            }

        } else {

            if (opponents != null) {

                boolean hostAdded = false;

                for (String opp : opponents) {

                    LobbyEntry e =
                            decodeLobbyEntry(opp);

                    if (e == null
                            || e.username == null
                            || e.username.isEmpty()) {

                        continue;
                    }

                    if (!hostAdded) {

                        lobbyListModel.addElement(
                                e.username.trim()
                                        + " (Host)"
                        );

                        lobbyPlayerIds.add(
                                e.playerId
                        );

                        hostAdded = true;

                    } else {

                        lobbyListModel.addElement(
                                e.username.trim()
                        );

                        lobbyPlayerIds.add(
                                e.playerId
                        );
                    }
                }
            }

            lobbyListModel.addElement(
                    me + " (You)"
            );

            lobbyPlayerIds.add(-1);
        }

        setPlayersJoinedCount(
                lobbyListModel.getSize()
        );

        updateLobbyActionButtons();
    }

    private void refreshLobbyPlayersFromOpponents(
            String[] opponents) {

        if (lobbyListModel == null) {
            return;
        }

        showCurrentPlayerInLobby(
                opponents
        );

        setPlayersJoinedCount(
                lobbyListModel.getSize()
        );

        listScroll.setVisible(true);

        kickButton.setVisible(hosting);

        if (lobbyListModel.getSize() > 1
                && hosting) {

            beginButton.setEnabled(true);
        }

        updateLobbyActionButtons();

        revalidate();
        repaint();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Countdown
    // ─────────────────────────────────────────────────────────────────────────

    private void startLobbyCountdown(
            Runnable onTimeout) {

        stopLobbyCountdown();

        joinCountdownTimer =
                new Timer(
                        1000,
                        e -> {

                            int remaining =
                                    Player.getWaitingTime();

                            if (remaining < 0) {
                                return;
                            }

                            if (remaining == 0) {

                                stopLobbyCountdown();

                                if (onTimeout != null) {
                                    onTimeout.run();
                                }
                            }
                        }
                );

        joinCountdownTimer.setInitialDelay(0);
        joinCountdownTimer.start();
    }

    private void stopLobbyCountdown() {

        if (joinCountdownTimer != null
                && joinCountdownTimer.isRunning()) {

            joinCountdownTimer.stop();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Label updaters
    // ─────────────────────────────────────────────────────────────────────────

    private void setLobbyStatus(
            String status) {

        if (lobbyStatusValueLabel != null) {

            lobbyStatusValueLabel.setText(
                    status == null
                            ? ""
                            : status
            );
        }
    }

    private void updateGameIdLabel() {

        if (gameIdValueLabel == null) {
            return;
        }

        gameIdValueLabel.setText(
                (Player.currentGame != null
                        && Player.currentGame.gameId > 0)
                        ? String.valueOf(
                        Player.currentGame.gameId
                )
                        : "-"
        );
    }

    private void setPlayersJoinedCount(
            int count) {

        if (playersJoinedValueLabel != null) {

            playersJoinedValueLabel.setText(
                    String.valueOf(
                            Math.max(count, 0)
                    )
            );
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Kick
    // ─────────────────────────────────────────────────────────────────────────

    private void handleKickPlayer() {

        if (!hosting
                || Player.currentGame == null
                || Player.currentGame.gameId <= 0) {

            return;
        }

        Integer targetId =
                getSelectedLobbyPlayerId();

        if (targetId == null) {

            JOptionPane.showMessageDialog(
                    this,
                    "Select a player to kick.",
                    "No selection",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        try {

            Player.kickPlayer(targetId);

        } catch (Exception ex) {

            ex.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Failed to kick player: "
                            + ex.getMessage(),
                    "Kick failed",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private Integer getSelectedLobbyPlayerId() {

        if (playersList == null
                || lobbyPlayerIds.isEmpty()) {

            return null;
        }

        int idx =
                playersList.getSelectedIndex();

        if (idx < 0
                || idx >= lobbyPlayerIds.size()) {

            return null;
        }

        int id =
                lobbyPlayerIds.get(idx);

        return id > 0
                ? id
                : null;
    }

    private void updateLobbyActionButtons() {

        if (kickButton == null) {
            return;
        }

        boolean show =
                hosting
                        && !Player.isSinglePlayerMode()
                        && listScroll != null
                        && listScroll.isVisible();

        kickButton.setVisible(show);

        kickButton.setEnabled(show && getSelectedLobbyPlayerId() != null
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Navigation
    // ─────────────────────────────────────────────────────────────────────────

    private void goToMainMenuOnce() {

        if (navigating) {
            return;
        }

        navigating = true;

        stopLobbyCountdown();

        Player.leaveGame();

        dispose();

        PlayerMainMenuView.open(
                Player.getPlayerUsername()
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LobbyEntry decoder
    // ─────────────────────────────────────────────────────────────────────────

    private static final class LobbyEntry {

        final int playerId;
        final String username;

        LobbyEntry(
                int playerId,
                String username) {

            this.playerId = playerId;
            this.username = username;
        }
    }

    private LobbyEntry decodeLobbyEntry(
            String value) {

        if (value == null) {
            return null;
        }

        String s =
                value.trim();

        if (s.isEmpty()) {
            return null;
        }

        int sep =
                s.indexOf('|');

        if (sep < 0) {
            return new LobbyEntry(
                    -1,
                    s
            );
        }

        try {

            return new LobbyEntry(
                    Integer.parseInt(
                            s.substring(0, sep).trim()
                    ),
                    s.substring(sep + 1).trim()
            );

        } catch (NumberFormatException e) {

            return new LobbyEntry(
                    -1,
                    s.substring(sep + 1).trim()
            );
        }
    }
}