package cypher.player.views;

import cypher.player.Player;
import cypher.player.views.components.RoundButton;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class PlayerMainMenuView extends JFrame {
    private static PlayerMainMenuView instance;
    private boolean isClosing = false;
    private JPanel panel;
    private final String username;

    public PlayerMainMenuView(String username) {
        this.username = (username == null || username.isEmpty()) ? "Player" : username;

        initComponents();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Player.gracefulExit();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                instance = null;
            }
        });

        setVisible(true);
    }

    public static synchronized PlayerMainMenuView open(String username) {
        if (instance == null) {
            instance = new PlayerMainMenuView(username);
        } else {
            instance.toFront();
            instance.requestFocus();
        }
        return instance;
    }

    private void initComponents() {
        // Icon
        ImageIcon icon = new ImageIcon("src/cypher/assets/cypher_logo.png");
        setIconImage(icon.getImage());

        // Panel
        panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Image img = new ImageIcon("src/cypher/assets/cypher_main.gif").getImage();
                g.drawImage(img, 0, 0, getWidth(), getHeight(), this);
            }
        };
        panel.setLayout(null);

        // Logo
        ImageIcon logo = new ImageIcon("src/cypher/assets/cypher.png");
        JLabel logoLabel = new JLabel(logo);
        logoLabel.setBounds(80, 40, logo.getIconWidth(), logo.getIconHeight());
        panel.add(logoLabel);

        // Username Label
        JLabel usernameLabel = new JLabel(username.toUpperCase());
        usernameLabel.setFont(new Font("Roboto", Font.BOLD, 18));
        usernameLabel.setForeground(Color.WHITE);
        usernameLabel.setBounds(20, 20, 200, 30);
        panel.add(usernameLabel);

        // Play Button
        Color playColor = Color.decode("#008000");
        RoundButton playButton = new RoundButton("PLAY", playColor);
        playButton.setPreferredSize(new Dimension(240, 50));
        playButton.setForeground(Color.WHITE);
        playButton.setFont(new Font("Arial", Font.BOLD, 18));
        playButton.setBounds(350, 240, 240, 50);
        playButton.addActionListener(e -> {
            dispose();
            PlayerLobbyView lobbyView = new PlayerLobbyView();
            lobbyView.setVisible(true);
            SwingUtilities.getWindowAncestor(panel).dispose();
        });
        panel.add(playButton);

        // Leaderboards Button
        RoundButton leaderboardsButton = createLeaderboardsButton();
        panel.add(leaderboardsButton);

        // Learn How to Play Button
        Color howToPlayColor = Color.decode("#6A5ACD");
        RoundButton howToPlayButton = new RoundButton("LEARN HOW TO PLAY", howToPlayColor);
        howToPlayButton.setPreferredSize(new Dimension(240, 50));
        howToPlayButton.setForeground(Color.WHITE);
        howToPlayButton.setFont(new Font("Arial", Font.BOLD, 18));
        howToPlayButton.setBounds(350, 360, 240, 50);
        howToPlayButton.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                "How to Play:\n\n" +
                        "1. Join or create a game.\n" +
                        "2. Form words from the given letters.\n" +
                        "3. Submit before time runs out.\n" +
                        "4. Valid words earn points.\n" +
                        "5. Highest score wins.",
                "Learn How to Play",
                JOptionPane.INFORMATION_MESSAGE
        ));
        panel.add(howToPlayButton);

        // Logout Button
        Color logoutColor = Color.RED;
        RoundButton logoutButton = new RoundButton("LOGOUT", logoutColor);
        logoutButton.setPreferredSize(new Dimension(200, 50));
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setFont(new Font("Arial", Font.BOLD, 18));
        logoutButton.setBounds(50, 420, 130, 50);
        logoutButton.addActionListener(e -> {
            Player.logout();
            dispose();
        });
        panel.add(logoutButton);

        // Settings Button
        ImageIcon settingsIcon = new ImageIcon("src/cypher/assets/settings_icon.png");
        JButton settingsButton = new JButton(settingsIcon);
        Image scaled = settingsIcon.getImage().getScaledInstance(90, 90, Image.SCALE_SMOOTH);
        settingsButton.setIcon(new ImageIcon(scaled));
        settingsButton.setBounds(840, 400, 90, 90);
        settingsButton.setBorderPainted(false);
        settingsButton.setContentAreaFilled(false);
        settingsButton.setFocusPainted(false);
        settingsButton.setOpaque(false);
        settingsButton.setToolTipText("Settings");
        settingsButton.addActionListener(e -> JOptionPane.showMessageDialog(this, "Settings clicked"));
        panel.add(settingsButton);

        // Window Configuration
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!isClosing) {
                    isClosing = true;
                    Player.logout();
                    dispose();
                }
            }
        });

        setTitle("Cypher | Main Menu | " + username.toUpperCase());
        setSize(960, 540);
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                Player.gracefulExit();
            }
        });
        setLocationRelativeTo(null);
        setContentPane(panel);
    }

    private RoundButton createLeaderboardsButton() {
        Color leaderboardsColor = Color.BLUE;
        RoundButton leaderboardsButton = new RoundButton("LEADERBOARDS", leaderboardsColor);
        leaderboardsButton.setPreferredSize(new Dimension(240, 50));
        leaderboardsButton.setForeground(Color.WHITE);
        leaderboardsButton.setFont(new Font("Arial", Font.BOLD, 18));
        leaderboardsButton.setBounds(350, 300, 240, 50);
        leaderboardsButton.addActionListener(e -> {
            PlayerLeaderboardsView leaderboardsView = new PlayerLeaderboardsView();
            leaderboardsView.setVisible(true);
        });
        return leaderboardsButton;
    }
}