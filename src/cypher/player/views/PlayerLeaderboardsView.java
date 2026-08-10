package cypher.player.views;

import cypher.player.Player;
import cypher.player.views.components.fonts.FontLoader;
import cypher.server.tables.leaderboard.Leaderboards;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;

public class PlayerLeaderboardsView extends JFrame {
    private static PlayerLeaderboardsView currentInstance = null;
    private DefaultTableModel tableModel;

    public static void showLeaderboard() {
        if (currentInstance != null && currentInstance.isVisible()) {
            currentInstance.toFront();
            currentInstance.requestFocus();
            return;
        }
        currentInstance = new PlayerLeaderboardsView();
        currentInstance.setVisible(true);
    }

    private PlayerLeaderboardsView() {
        initComponents();
        loadLeaderboards();
    }

    @Override
    public void dispose() {
        currentInstance = null;
        super.dispose();
    }

    private void initComponents() {
        setTitle("Cypher | Leaderboards");
        setSize(960, 540);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        ImageIcon icon = new ImageIcon("src/cypher/assets/cypher_logo.png");
        setIconImage(icon.getImage());

        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Image bg = new ImageIcon(
                        "src/cypher/assets/cypher_leaderboards.gif")
                        .getImage();

                g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
            }
        };

        backgroundPanel.setLayout(null);

        //----------------------------------------
        // Title
        //----------------------------------------
        JLabel titleLabel = new JLabel("LEADERBOARDS");
        titleLabel.setBounds(370, 30, 350, 40);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(FontLoader.loadFont(24f));

        //----------------------------------------
        // Player Label
        //----------------------------------------
        JLabel playerLabel = new JLabel(
                "Player: " + Player.getPlayerUsername().toUpperCase());

        playerLabel.setBounds(60, 30, 300, 25);
        playerLabel.setForeground(Color.WHITE);
        playerLabel.setFont(FontLoader.loadFont(14f));

        //----------------------------------------
        // Back Button
        //----------------------------------------
        JButton backButton = new JButton("BACK");
        backButton.setBounds(60, 90, 120, 38);

        backButton.setFocusable(false);
        backButton.setBackground(new Color(236, 6, 212));
        backButton.setForeground(Color.WHITE);
        backButton.setFont(FontLoader.loadFont(14f));

        backButton.addActionListener(e -> dispose());

        //----------------------------------------
        // Table
        //----------------------------------------
        tableModel = new DefaultTableModel(
                new Object[]{
                        "Rank",
                        "Username",
                        "Games Played",
                        "Wins",
                        "Highest Score",
                        "Longest Word"
                }, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

        };

        JTable leaderboardTable = new JTable(tableModel);

        leaderboardTable.setRowHeight(32);
        leaderboardTable.setFont(new Font("Arial", Font.PLAIN, 14));
        leaderboardTable.getTableHeader().setFont(new Font("Roboto", Font.BOLD, 14));

        leaderboardTable.getTableHeader().setBackground(new Color(32, 1, 75));
        leaderboardTable.getTableHeader().setForeground(Color.WHITE);

        leaderboardTable.setBackground(new Color(4, 0, 7));
        leaderboardTable.setForeground(Color.WHITE);

        leaderboardTable.setGridColor(new Color(60, 60, 60));

        leaderboardTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        leaderboardTable.setRowSelectionAllowed(false);
        leaderboardTable.setFocusable(false);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        centerRenderer.setForeground(Color.WHITE);
        centerRenderer.setOpaque(false);

        for (int i = 0; i < leaderboardTable.getColumnCount(); i++) {
            leaderboardTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane tableScrollPane = new JScrollPane(leaderboardTable);
        tableScrollPane.setOpaque(false);
        tableScrollPane.getViewport().setOpaque(false);
        tableScrollPane.getViewport().setBackground(new Color(4, 0, 7));
        tableScrollPane.setBounds(210, 90, 680, 340);

        //----------------------------------------
        // Add Components
        //----------------------------------------

        backgroundPanel.add(titleLabel);
        backgroundPanel.add(playerLabel);
        backgroundPanel.add(backButton);
        backgroundPanel.add(tableScrollPane);

        add(backgroundPanel);
    }

    private void loadLeaderboards() {

        ArrayList<Leaderboards> leaderboards = Player.getLeaderboards();

        tableModel.setRowCount(0);

        for (Leaderboards leaderboard : leaderboards) {

            tableModel.addRow(new Object[]{
                    leaderboard.rank,
                    leaderboard.username,
                    leaderboard.gamesPlayed,
                    leaderboard.gamesWon,
                    leaderboard.highestScore,
                    leaderboard.longestWord
            });

        }

        if (leaderboards.isEmpty()) {
            tableModel.addRow(new Object[]{
                    "-",
                    "No entries yet",
                    "-",
                    "-",
                    "-",
                    "-"
            });
        }
    }
}