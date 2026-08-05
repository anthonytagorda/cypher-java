package cypher.server.views;

import cypher.server.CypherDB;
import cypher.server.Server;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Map;

public class ServerManageGameView extends JFrame {
    private static ServerManageGameView instance;

    // ==================== Fields ====================
    private Timer serverWatchTimer;
    private Timer gameRefreshTimer;
    private volatile boolean loadingGames = false;

    private JTable gameTable;
    private int selectedRow = -1;
    private DefaultTableModel gameModel;
    private JButton deleteGameButton;
    private JTextField searchField;
    private String activeSearchQuery = "";

    // ==================== Constructor & Lifecycle ====================
    public ServerManageGameView() {
        initComponents();
        startGameRefreshTimer();
        startServerWatcher();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (serverWatchTimer != null) serverWatchTimer.stop();
                if (gameRefreshTimer != null) gameRefreshTimer.stop();
                instance = null;
            }
        });
    }

    public static ServerManageGameView getInstance() {
        if (instance == null) {
            instance = new ServerManageGameView();
        } else {
            JOptionPane.showMessageDialog(
                    null,
                    "A window is already open.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
        return instance;
    }

    // ==================== UI Initialization ====================
    private void initComponents() {
        ImageIcon icon = new ImageIcon("src/cypher/assets/cypher_logo.png");
        setTitle("Server | Game Management");
        setIconImage(icon.getImage());
        setSize(1000, 520);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = buildTopPanel();
        JPanel gamesPanel = buildGamesTablePanel();

        add(topPanel, BorderLayout.NORTH);
        add(gamesPanel, BorderLayout.CENTER);
    }

    private JPanel buildTopPanel() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        topPanel.setBackground(Color.decode("#212121"));

        JButton returnButton = new JButton("Return to Dashboard");
        returnButton.setPreferredSize(new Dimension(180, 32));
        returnButton.setMaximumSize(new Dimension(180, 32));
        returnButton.addActionListener(e -> dispose());

        JSeparator verticalSeparator = new JSeparator(SwingConstants.VERTICAL);
        verticalSeparator.setPreferredSize(new Dimension(6, 28));
        verticalSeparator.setMaximumSize(new Dimension(6, 28));

        JToolBar toolBar = buildGameToolbar();
        toolBar.setFloatable(false);

        topPanel.add(Box.createHorizontalStrut(8));
        topPanel.add(returnButton);
        topPanel.add(Box.createHorizontalStrut(8));
        topPanel.add(verticalSeparator);
        topPanel.add(Box.createHorizontalStrut(8));
        topPanel.add(toolBar);
        topPanel.add(Box.createHorizontalGlue());
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        return topPanel;
    }

    private JToolBar buildGameToolbar() {
        JToolBar toolBar = new JToolBar();
        searchField = new JTextField(12);
        toolBar.setFloatable(false);

        JButton searchButton = new JButton("Search");
        deleteGameButton = new JButton("Delete");
        JButton settingsButton = new JButton("Settings");

        toolBar.add(searchButton);
        toolBar.add(searchField);
        toolBar.addSeparator();
        toolBar.add(deleteGameButton);
        toolBar.addSeparator();
        toolBar.add(settingsButton);

        toolBar.setBorderPainted(false);
        toolBar.setOpaque(false);

        // Style buttons
        for (JButton b : new JButton[]{searchButton}) {
            b.setFocusPainted(false);
            b.setBorderPainted(false);
            b.setContentAreaFilled(true);
            b.setBackground(Color.decode("#D9D9D9"));
        }

        for (JButton b : new JButton[]{deleteGameButton, settingsButton}) {
            b.setFocusPainted(false);
            b.setBorderPainted(true);
            b.setContentAreaFilled(true);
            b.setBackground(Color.decode("#D9D9D9"));
        }

        // Attach listeners
        searchButton.addActionListener(e -> handleSearch());
        searchField.addActionListener(e -> handleSearch());
        deleteGameButton.addActionListener(e -> handleDeleteGame());
        settingsButton.addActionListener(e -> handleEditSettings());

        deleteGameButton.setEnabled(false);

        return toolBar;
    }

    private JPanel buildGamesTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        gameModel = new DefaultTableModel(new Object[]{
                "Game ID",
                "Host",
                "Game Type",
                "Status",
                "Rounds",
                "Winner",
                "Created",
                "Started",
                "Ended"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        gameTable = new JTable(gameModel);
        loadGamesFromDatabase();
        configureTable();

        JScrollPane tableScroll = new JScrollPane(gameTable);
        panel.add(tableScroll, BorderLayout.CENTER);

        return panel;
    }

    private void configureTable() {
        gameTable.getColumnModel().getColumn(0).setMinWidth(60);
        gameTable.getColumnModel().getColumn(0).setMaxWidth(80);
        gameTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        gameTable.getColumnModel().getColumn(2).setPreferredWidth(95);
        gameTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        gameTable.getColumnModel().getColumn(5).setPreferredWidth(100);

        gameTable.getColumn("Game Type").setCellRenderer(new GameTypeRenderer());
        gameTable.getColumn("Status").setCellRenderer(new StatusRenderer());

        gameTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        gameTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedRow = gameTable.getSelectedRow();
                updateButtonStates();
            }
        });
    }

    // ==================== Event Handlers ====================
    private void handleSearch() {
        activeSearchQuery = (searchField == null) ? "" : searchField.getText().trim();
        refreshGamesSafely();
    }

    private void handleDeleteGame() {
        if (selectedRow < 0 || selectedRow >= gameModel.getRowCount()) {
            JOptionPane.showMessageDialog(this, "Please select a game first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int gameId = (int) gameModel.getValueAt(selectedRow, 0);
        String status = String.valueOf(gameModel.getValueAt(selectedRow, 3));

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete game " + gameId + " (Status: " + status + ")?\nThis action cannot be undone.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        CypherDB.deleteGame(gameId);
        JOptionPane.showMessageDialog(this, "Game deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        refreshGamesSafely();
        selectedRow = -1;
        gameTable.clearSelection();
        updateButtonStates();
    }

    private void handleEditSettings() {
        Map<String, Integer> settings = CypherDB.getGameSettings();

        int currentWaitTime = settings.getOrDefault("waiting_time_sec", 30);
        int currentDuration = settings.getOrDefault("game_duration_sec", 180);
        int currentMaxPlayers = settings.getOrDefault("max_players", 2);
        int currentRoundsToWin = settings.getOrDefault("rounds_to_win", 3);

        JSpinner waitingTimeSpinner = new JSpinner(new SpinnerNumberModel(currentWaitTime, 1, 300, 5));
        JSpinner roundDurationSpinner = new JSpinner(new SpinnerNumberModel(currentDuration, 30, 600, 30));
        JSpinner maxPlayersSpinner = new JSpinner(new SpinnerNumberModel(currentMaxPlayers, 1, 10, 1));
        JSpinner roundsToWinSpinner = new JSpinner(new SpinnerNumberModel(currentRoundsToWin, 1, 10, 1));

        JPanel panel = new JPanel(new GridLayout(4, 2, 8, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel waitingLabel = new JLabel("Waiting Time (seconds):");
        waitingLabel.setFont(waitingLabel.getFont().deriveFont(Font.BOLD));
        panel.add(waitingLabel);
        panel.add(waitingTimeSpinner);

        JLabel durationLabel = new JLabel("Round Duration (seconds):");
        durationLabel.setFont(durationLabel.getFont().deriveFont(Font.BOLD));
        panel.add(durationLabel);
        panel.add(roundDurationSpinner);

        JLabel maxPlayersLabel = new JLabel("Max Players per Game:");
        maxPlayersLabel.setFont(maxPlayersLabel.getFont().deriveFont(Font.BOLD));
        panel.add(maxPlayersLabel);
        panel.add(maxPlayersSpinner);

        JLabel roundsToWinLabel = new JLabel("Rounds Required to Win:");
        roundsToWinLabel.setFont(roundsToWinLabel.getFont().deriveFont(Font.BOLD));
        panel.add(roundsToWinLabel);
        panel.add(roundsToWinSpinner);

        int result = JOptionPane.showConfirmDialog(this, panel, "Edit Game Settings", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        int waitTime = (Integer) waitingTimeSpinner.getValue();
        int duration = (Integer) roundDurationSpinner.getValue();
        int maxPlayers = (Integer) maxPlayersSpinner.getValue();
        int roundsToWin = (Integer) roundsToWinSpinner.getValue();

        CypherDB.updateGameSettings(waitTime, duration, maxPlayers, roundsToWin);

        JOptionPane.showMessageDialog(this, "Game settings updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    // ==================== Data & UI Utility Methods ====================
    private void loadGamesFromDatabase() {
        gameModel.setRowCount(0);

        try {
            List<Map<String, Object>> gameList = CypherDB.getGames();
            for (Map<String, Object> m : gameList) {
                if (!matchesSearch(m, activeSearchQuery)) continue;
                gameModel.addRow(new Object[]{
                        m.get("game_id"),
                        m.get("host"),
                        capitalize((String) m.get("game_type")),
                        capitalize((String) m.get("status")),
                        m.get("rounds_played"),
                        m.get("winner"),
                        m.get("created_at"),
                        m.get("started_at"),
                        m.get("ended_at")
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Failed to load games from database:\n" + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void refreshGamesSafely() {
        if (loadingGames) return;
        loadingGames = true;
        try {
            Integer selectedGameId = getSelectedGameId();
            loadGamesFromDatabase();
            restoreSelectionByGameId(selectedGameId);
            updateButtonStates();
        } finally {
            loadingGames = false;
        }
    }

    private void updateButtonStates() {
        boolean hasSelection = selectedRow >= 0 && selectedRow < gameModel.getRowCount();
        deleteGameButton.setEnabled(hasSelection);
    }

    private Integer getSelectedGameId() {
        int row = gameTable != null ? gameTable.getSelectedRow() : -1;
        if (row < 0 || row >= gameModel.getRowCount()) return null;
        Object value = gameModel.getValueAt(row, 0);
        if (value instanceof Integer) return (Integer) value;
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private void restoreSelectionByGameId(Integer gameId) {
        if (gameId == null || gameTable == null) {
            selectedRow = -1;
            if (gameTable != null) gameTable.clearSelection();
            return;
        }

        for (int i = 0; i < gameModel.getRowCount(); i++) {
            Object rowValue = gameModel.getValueAt(i, 0);
            if (rowValue != null && String.valueOf(rowValue).equals(String.valueOf(gameId))) {
                gameTable.setRowSelectionInterval(i, i);
                selectedRow = i;
                return;
            }
        }

        selectedRow = -1;
        gameTable.clearSelection();
    }

    private boolean matchesSearch(Map<String, Object> game, String query) {
        if (query == null || query.trim().isEmpty()) return true;
        String q = query.trim().toLowerCase();
        return String.valueOf(game.get("game_id")).contains(q)
                || String.valueOf(game.get("host")).toLowerCase().contains(q)
                || String.valueOf(game.get("game_type")).toLowerCase().contains(q)
                || String.valueOf(game.get("status")).toLowerCase().contains(q)
                || String.valueOf(game.get("winner")).toLowerCase().contains(q);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    // ==================== Timer Methods ====================
    private void startGameRefreshTimer() {
        gameRefreshTimer = new Timer(1000, e -> refreshGamesSafely());
        gameRefreshTimer.start();
    }

    private void startServerWatcher() {
        serverWatchTimer = new Timer(500, e -> {
            if (!Server.isRunning()) {
                ((Timer) e.getSource()).stop();
                dispose();
            }
        });
        serverWatchTimer.start();
    }

    // ==================== Table Renderers ====================
    private static class GameTypeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (isSelected) return c;

            String type = value == null ? "" : value.toString().trim().toLowerCase();
            if ("multiplayer".equals(type)) {
                c.setBackground(Color.decode("#B3E5FC"));
            } else
                if ("singleplayer".equals(type)) {
                    c.setBackground(Color.decode("#F8BBD0"));
                } else {
                    c.setBackground(Color.WHITE);
                }
            return c;
        }
    }

    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (isSelected) return c;

            String status = value == null ? "" : value.toString().trim().toLowerCase();
            switch (status) {
                case "waiting":
                    c.setBackground(Color.decode("#FFFACD"));
                    break;
                case "in_progress":
                    c.setBackground(Color.decode("#FFE0B2"));
                    break;
                case "ended":
                    c.setBackground(Color.decode("#C8E6C9"));
                    break;
                case "cancelled":
                    c.setBackground(Color.decode("#FFCCBC"));
                    break;
                default:
                    c.setBackground(Color.WHITE);
                    break;
            }
            return c;
        }
    }
}
