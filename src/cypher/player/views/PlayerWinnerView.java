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
        setTitle("Cypher | Winner!");
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        ImageIcon icon = new ImageIcon("src/cypher/assets/cypher_logo.png");
        setIconImage(icon.getImage());

        // GIF Background
        ImageIcon gifBackground = new ImageIcon("src/cypher/assets/cypher_player-win.gif");
        JLabel background = new JLabel(gifBackground) {
            @Override
            protected void paintComponent(Graphics g) {
                g.drawImage(gifBackground.getImage(), 0, 0, 500,640,this);
            }
        };
        background.setLayout(new BorderLayout());

        // Content Panel
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

        // Layout Constraints for Winner and Score
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);

        contentPanel.add(winnerLabel, gbc);
        contentPanel.add(scoreLabel, gbc);

        // Layout Constraints for Return Button (positioned at bottom)
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.SOUTH;
        gbc.insets = new Insets(240, 0, 30, 0);

        contentPanel.add(returnButton, gbc);
        background.add(contentPanel, BorderLayout.CENTER);
        setContentPane(background);
    }

    private void startAutoCloseTimer() {
        // Auto-close after 30 seconds (30000 ms)
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
