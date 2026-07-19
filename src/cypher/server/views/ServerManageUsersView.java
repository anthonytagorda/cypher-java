package cypher.server.views;

import cypher.server.Server;
import cypher.server.tables.player.Player;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class ServerManageUsersView extends JFrame {
    private static ServerManageUsersView instance;

    private Timer serverWatchTimer;
    private Timer userRefreshTimer; // <-- field (fix)
    private volatile boolean loadingUsers = false;

    private JTable userTable;
    private int selectedRow = -1;
    private DefaultTableModel userModel;

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

    private void initComponents() {
        ImageIcon icon = new ImageIcon("src/cypher/assets/cypher_logo.png");
        setTitle("Server | User Management");
        setIconImage(icon.getImage());
        setSize(900, 520);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        Color primaryColor = Color.decode("#7b44e4");
        Border primaryBorder = BorderFactory.createLineBorder(primaryColor);

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

        JPanel usersPanel = buildUsersTablePanel();

        add(topPanel, BorderLayout.NORTH);
        add(usersPanel, BorderLayout.CENTER);
    }

    private JToolBar buildUserToolbar() {
        JToolBar toolBar = new JToolBar();
        JTextField searchField = new JTextField(12);
        toolBar.setFloatable(false);

        JButton searchButton = new JButton("Search");
        JButton addPlayerButton = new JButton("Add");
        JButton editPlayerButton = new JButton("Edit");
        JButton deletePlayerButton = new JButton("Delete");
        JButton banPlayerButton = new JButton("Ban");

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
            }
        });

        JScrollPane tableScroll = new JScrollPane(userTable);
        panel.add(tableScroll, BorderLayout.CENTER);

        return panel;
    }

    private void loadUsersFromDatabase() {
        userModel.setRowCount(0);

        try {
            List<Player> playerList = cypher.server.tables.CypherDB.getPlayers();

            for (Player p : playerList) {
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

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (isSelected) return c;

            String status = value == null ? "" : value.toString().trim().toLowerCase();
            if ("online".equals(status)) {
                c.setBackground(Color.decode("#C8F7C5"));
            } else
                if ("offline".equals(status)) {
                    c.setBackground(Color.decode("#D3D3D3"));
                } else {
                    c.setBackground(Color.WHITE);
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
                c.setBackground(Color.decode("#FFCCCC"));
            } else
                if ("not banned".equals(ban)) {
                    c.setBackground(Color.decode("#DFFFE0"));
                } else {
                    c.setBackground(Color.WHITE);
                }
            return c;
        }
    }

    private void refreshUsersSafely() {
        if (loadingUsers) return;
        loadingUsers = true;
        try {
            loadUsersFromDatabase();
        } finally {
            loadingUsers = false;
        }
    }

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
}