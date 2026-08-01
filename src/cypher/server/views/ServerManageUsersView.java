package cypher.server.views;

import cypher.server.CypherDB;
import cypher.server.Server;
import cypher.server.Servant;
import cypher.server.tables.player.Player;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class ServerManageUsersView extends JFrame {
    private static ServerManageUsersView instance;

    // ==================== Fields ====================
    private Timer serverWatchTimer;
    private Timer userRefreshTimer;
    private volatile boolean loadingUsers = false;

    private JTable userTable;
    private int selectedRow = -1;
    private DefaultTableModel userModel;
    private JButton banPlayerButton;
    private JTextField searchField;
    private String activeSearchQuery = "";

    // ==================== Constructor & Lifecycle ====================
    public ServerManageUsersView() {
        initComponents();
        startUserRefreshTimer();
        startServerWatcher();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (serverWatchTimer != null) serverWatchTimer.stop();
                if (userRefreshTimer != null) userRefreshTimer.stop();
                instance = null;
            }
        });
    }

    public static ServerManageUsersView getInstance() {
        if (instance == null) {
            instance = new ServerManageUsersView();
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
        setTitle("Server | User Management");
        setIconImage(icon.getImage());
        setSize(900, 520);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = buildTopPanel();
        JPanel usersPanel = buildUsersTablePanel();

        add(topPanel, BorderLayout.NORTH);
        add(usersPanel, BorderLayout.CENTER);
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

        JToolBar toolBar = buildUserToolbar();
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

    private JToolBar buildUserToolbar() {
        JToolBar toolBar = new JToolBar();
        searchField = new JTextField(12);
        toolBar.setFloatable(false);

        JButton searchButton = new JButton("Search");
        JButton addPlayerButton = new JButton("Add");
        JButton editPlayerButton = new JButton("Edit");
        JButton deletePlayerButton = new JButton("Delete");
        banPlayerButton = new JButton("Ban");

        toolBar.add(searchButton);
        toolBar.add(searchField);
        toolBar.addSeparator();
        toolBar.add(addPlayerButton);
        toolBar.add(editPlayerButton);
        toolBar.add(deletePlayerButton);
        toolBar.addSeparator();
        toolBar.add(banPlayerButton);

        toolBar.setBorderPainted(false);
        toolBar.setOpaque(false);

        // Style buttons
        for (JButton b : new JButton[]{searchButton}) {
            b.setFocusPainted(false);
            b.setBorderPainted(false);
            b.setContentAreaFilled(true);
            b.setBackground(Color.decode("#D9D9D9"));
        }

        for (JButton b : new JButton[]{addPlayerButton, editPlayerButton, deletePlayerButton, banPlayerButton}) {
            b.setFocusPainted(false);
            b.setBorderPainted(true);
            b.setContentAreaFilled(true);
            b.setBackground(Color.decode("#D9D9D9"));
        }

        // Attach listeners
        searchButton.addActionListener(e -> handleSearch());
        searchField.addActionListener(e -> handleSearch());
        addPlayerButton.addActionListener(e -> handleAddPlayer());
        editPlayerButton.addActionListener(e -> handleEditPlayer());
        deletePlayerButton.addActionListener(e -> handleDeletePlayer());
        banPlayerButton.addActionListener(e -> handleBanToggle());
        banPlayerButton.setEnabled(false);


        return toolBar;
    }

    private JPanel buildUsersTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        userModel = new DefaultTableModel(new Object[]{
                "Player ID",
                "Username",
                "Password",
                "Status",
                "Ban Status",
                "Date Created"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        userTable = new JTable(userModel);
        loadUsersFromDatabase();
        configureTable();

        JScrollPane tableScroll = new JScrollPane(userTable);
        panel.add(tableScroll, BorderLayout.CENTER);

        return panel;
    }

    private void configureTable() {
        userTable.getColumnModel().getColumn(0).setMinWidth(55);
        userTable.getColumnModel().getColumn(0).setMaxWidth(70);
        userTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        userTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        userTable.getColumnModel().getColumn(4).setPreferredWidth(100);

        userTable.getColumn("Status").setCellRenderer(new StatusRenderer());
        userTable.getColumn("Ban Status").setCellRenderer(new BanStatusRenderer());

        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedRow = userTable.getSelectedRow();
                updateBanButtonState();
            }
        });
    }

    // ==================== Event Handlers ====================
    private void handleSearch() {
        activeSearchQuery = (searchField == null) ? "" : searchField.getText().trim();
        refreshUsersSafely();
    }

    private void handleAddPlayer() {
        JTextField usernameField = new JTextField(15);
        JPasswordField passwordField = new JPasswordField(15);
        JPanel panel = new JPanel(new GridLayout(0, 2, 4, 4));
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add New Player", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (hasInvalidPlayerInput(username, password)) return;

        if (CypherDB.registerPlayer(username, password)) {
            JOptionPane.showMessageDialog(this, "Player added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshUsersSafely();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to add player (username may already exist).", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleEditPlayer() {
        if (selectedRow < 0 || selectedRow >= userModel.getRowCount()) {
            JOptionPane.showMessageDialog(this, "Please select a player first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int playerId = (int) userModel.getValueAt(selectedRow, 0);
        String currentUsername = String.valueOf(userModel.getValueAt(selectedRow, 1));
        String currentPassword = String.valueOf(userModel.getValueAt(selectedRow, 2));
        String currentStatus = String.valueOf(userModel.getValueAt(selectedRow, 3));

        JTextField usernameField = new JTextField(currentUsername, 15);
        JPasswordField passwordField = new JPasswordField(currentPassword, 15);
        JComboBox<String> statusField = new JComboBox<>(new String[]{"online", "offline", "in-game"});
        statusField.setSelectedItem(currentStatus == null ? "offline" : currentStatus.trim().toLowerCase());
        JPanel panel = new JPanel(new GridLayout(0, 2, 4, 4));
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(new JLabel("Status:"));
        panel.add(statusField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Edit Player", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        String newUsername = usernameField.getText().trim();
        String newPassword = new String(passwordField.getPassword()).trim();
        String newStatus = String.valueOf(statusField.getSelectedItem()).trim().toLowerCase();

        if (hasInvalidPlayerInput(newUsername, newPassword)) return;

        Player playerToUpdate = CypherDB.getPlayerFromId(playerId);
        if (playerToUpdate != null) {
            playerToUpdate.username = newUsername;
            playerToUpdate.password = newPassword;
            playerToUpdate.status = newStatus;
            CypherDB.updatePlayer(playerToUpdate);
            JOptionPane.showMessageDialog(this, "Player updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshUsersSafely();
        }
    }

    private void handleDeletePlayer() {
        if (selectedRow < 0 || selectedRow >= userModel.getRowCount()) {
            JOptionPane.showMessageDialog(this, "Please select a player first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int playerId = (int) userModel.getValueAt(selectedRow, 0);
        String username = String.valueOf(userModel.getValueAt(selectedRow, 1));

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete player '" + username + "' (ID: " + playerId + ")?\nThis action cannot be undone.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        CypherDB.deletePlayer(playerId);
        CypherDB.resetAutoIncrement();
        JOptionPane.showMessageDialog(this, "Player deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        refreshUsersSafely();
        selectedRow = -1;
        userTable.clearSelection();
        updateBanButtonState();
    }

    private void handleBanToggle() {
        if (selectedRow < 0 || selectedRow >= userModel.getRowCount()) {
            JOptionPane.showMessageDialog(this, "Please select a player first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int playerId = (int) userModel.getValueAt(selectedRow, 0);
        String username = String.valueOf(userModel.getValueAt(selectedRow, 1));
        String banText = String.valueOf(userModel.getValueAt(selectedRow, 4));
        boolean isBanned = "banned".equalsIgnoreCase(banText);

        if (!isBanned) {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Ban player '" + username + "' (ID: " + playerId + ")?",
                    "Confirm Ban",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (confirm != JOptionPane.YES_OPTION) return;

            CypherDB.banPlayer(playerId);
            CypherDB.setPlayerOffline(playerId);
            Servant.notifyAndDisconnectBannedPlayer(playerId);
        } else {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Unban player '" + username + "' (ID: " + playerId + ")?",
                    "Confirm Unban",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (confirm != JOptionPane.YES_OPTION) return;

            CypherDB.unbanPlayer(playerId);
        }

        refreshUsersSafely();
        selectedRow = -1;
        userTable.clearSelection();
        updateBanButtonState();
    }

    // ==================== Data & UI Utility Methods ====================
    private void loadUsersFromDatabase() {
        userModel.setRowCount(0);

        try {
            List<Player> playerList = CypherDB.getPlayers();
            for (Player p : playerList) {
                if (!matchesSearch(p, activeSearchQuery)) continue;
                userModel.addRow(new Object[]{
                        p.playerId,
                        p.username,
                        p.password,
                        capitalize(p.status),
                        p.isBanned ? "Banned" : "Not Banned",
                        p.createdAt
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Failed to load users from database:\n" + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void refreshUsersSafely() {
        if (loadingUsers) return;
        loadingUsers = true;
        try {
            Integer selectedPlayerId = getSelectedPlayerId();
            loadUsersFromDatabase();
            restoreSelectionByPlayerId(selectedPlayerId);
            updateBanButtonState();
        } finally {
            loadingUsers = false;
        }
    }

    private void updateBanButtonState() {
        if (banPlayerButton == null) return;
        if (selectedRow < 0 || selectedRow >= userModel.getRowCount()) {
            banPlayerButton.setText("Ban");
            banPlayerButton.setEnabled(false);
            return;
        }

        String banText = String.valueOf(userModel.getValueAt(selectedRow, 4));
        boolean isBanned = "banned".equalsIgnoreCase(banText);
        banPlayerButton.setText(isBanned ? "Unban" : "Ban");
        banPlayerButton.setEnabled(true);
    }

    private Integer getSelectedPlayerId() {
        int row = userTable != null ? userTable.getSelectedRow() : -1;
        if (row < 0 || row >= userModel.getRowCount()) return null;
        Object value = userModel.getValueAt(row, 0);
        if (value instanceof Integer) return (Integer) value;
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private void restoreSelectionByPlayerId(Integer playerId) {
        if (playerId == null || userTable == null) {
            selectedRow = -1;
            if (userTable != null) userTable.clearSelection();
            return;
        }

        for (int i = 0; i < userModel.getRowCount(); i++) {
            Object rowValue = userModel.getValueAt(i, 0);
            if (rowValue != null && String.valueOf(rowValue).equals(String.valueOf(playerId))) {
                userTable.setRowSelectionInterval(i, i);
                selectedRow = i;
                return;
            }
        }

        selectedRow = -1;
        userTable.clearSelection();
    }

    private boolean hasInvalidPlayerInput(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username and password cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return true;
        }

        if (!username.matches("[a-zA-Z0-9]+")) {
            JOptionPane.showMessageDialog(this, "Username must contain only alphanumeric characters.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return true;
        }

        if (username.length() < 5 || username.length() > 30) {
            JOptionPane.showMessageDialog(this, "Username must be 5-30 characters.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return true;
        }

        if (password.length() < 6 || password.length() > 30) {
            JOptionPane.showMessageDialog(this, "Password must be 6-30 characters.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return true;
        }

        return false;
    }

    private boolean matchesSearch(Player player, String query) {
        if (query == null || query.trim().isEmpty()) return true;
        String q = query.trim().toLowerCase();
        return String.valueOf(player.playerId).contains(q)
                || (player.username != null && player.username.toLowerCase().contains(q))
                || (player.status != null && player.status.toLowerCase().contains(q))
                || (player.isBanned ? "banned" : "not banned").contains(q);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    // ==================== Timer Methods ====================
    private void startUserRefreshTimer() {
        userRefreshTimer = new Timer(1000, e -> refreshUsersSafely());
        userRefreshTimer.start();
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
    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (isSelected) return c;

            String status = value == null ? "" : value.toString().trim().toLowerCase();
            switch (status) {
                case "online":
                    c.setBackground(Color.decode("#C8F7C5"));
                    break;
                case "offline":
                    c.setBackground(Color.decode("#D3D3D3"));
                    break;
                case "in-game":
                    c.setBackground(Color.decode("#FFFACD"));
                    break;
                default:
                    c.setBackground(Color.WHITE);
                    break;
            }
            return c;
        }
    }

    private static class BanStatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (isSelected) return c;

            String ban = value == null ? "" : value.toString().trim().toLowerCase();
            if ("banned".equals(ban)) {
                c.setBackground(Color.decode("#FFC0C0"));
            } else if ("not banned".equals(ban)) {
                c.setBackground(Color.decode("#CFFFD2"));
            } else {
                c.setBackground(Color.WHITE);
            }
            return c;
        }
    }
}