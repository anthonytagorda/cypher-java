package cypher.player.views;

import cypher.player.views.components.fonts.FontLoader;

import javax.swing.*;
import java.awt.*;

public class PlayerWinnerView extends JFrame {
    private final String winnerName;
    private final int winnerScore;
    private Timer timer;

    public PlayerWinnerView(String winnerName, int winnerScore) {
        this.winnerName = winnerName;
        this.winnerScore = winnerScore;
        initComponents();
        setVisible(true);
        startAutoCloseTimer();
    }

    private void initComponents() {
        // Frame Configuration
        setTitle("Winner!");
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Window icon (this is the small logo in the title bar)
        ImageIcon trophy = new ImageIcon("src/cypher/assets/cypher_win.png");
        setIconImage(trophy.getImage());

        // ─── Background Panel (draws the GIF) ──────────────────────
        JPanel backgroundPanel = new JPanel(null) {
            private final Image gifImage;

            {
                // Load the GIF once
                ImageIcon gifIcon = new ImageIcon("src/cypher/assets/cypher_player-win.gif");
                gifImage = gifIcon.getImage();
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (gifImage != null) {
                    // Draw the GIF scaled to fill the panel
                    g.drawImage(gifImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    // Fallback if GIF is missing
                    g.setColor(Color.BLACK);
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.setColor(Color.WHITE);
                    g.drawString("Background not found", 50, 50);
                }
            }
        };
        backgroundPanel.setLayout(new BorderLayout());
        setContentPane(backgroundPanel);

        // ─── Transparent content panel (overlay) ───────────────────
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);

        // Winner Label
        JLabel winnerLabel = new JLabel("Winner: " + winnerName.toUpperCase());
        winnerLabel.setHorizontalAlignment(JLabel.CENTER);
        winnerLabel.setFont(FontLoader.loadFont(20f));
        winnerLabel.setForeground(new Color(255, 215, 0));

        // Score Label
        JLabel scoreLabel = new JLabel("Score: " + winnerScore);
        scoreLabel.setHorizontalAlignment(JLabel.CENTER);
        scoreLabel.setFont(FontLoader.loadFont(16f));
        scoreLabel.setForeground(new Color(128, 0, 128));

        // Return Button
        JButton returnButton = new JButton("Return to Main Menu");
        returnButton.setForeground(Color.WHITE);
        returnButton.setBackground(new Color(0, 128, 0));
        returnButton.setFocusPainted(false);
        returnButton.setFont(FontLoader.loadFont(12f));
        returnButton.setPreferredSize(new Dimension(300, 45));
        returnButton.addActionListener(e -> {
            dispose();
            new PlayerMainMenuView(winnerName);
        });

        // Layout
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);

        contentPanel.add(winnerLabel, gbc);
        contentPanel.add(scoreLabel, gbc);

        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.SOUTH;
        gbc.insets = new Insets(240, 0, 30, 0);
        contentPanel.add(returnButton, gbc);

        // Add overlay to background
        backgroundPanel.add(contentPanel, BorderLayout.CENTER);
    }

    private void startAutoCloseTimer() {
        timer = new Timer(30000, e -> {
            dispose();
            new PlayerMainMenuView(winnerName);
        });
        timer.setRepeats(false);
        timer.start();
    }

    @Override
    public void dispose() {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }
        super.dispose();
    }
}