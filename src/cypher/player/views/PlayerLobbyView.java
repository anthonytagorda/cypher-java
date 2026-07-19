package cypher.player.views;

import cypher.player.Player;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class PlayerLobbyView extends JFrame {
    private JLabel countdownLabel;

    private int waitingTime;
    private Timer timer;
    private boolean navigating = false;

    public PlayerLobbyView() {
        initComponents();
        setVisible(true);
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
        JLabel usernameLabel = new JLabel(username.toUpperCase());
        usernameLabel.setFont(new Font("Arial", Font.BOLD, 18));
        usernameLabel.setForeground(Color.WHITE);

        JLabel titleLabel = new JLabel("Waiting for Other Players");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);

        JLabel timerLabel = new JLabel("Time Remaining:");
        timerLabel.setForeground(Color.WHITE);

        countdownLabel = new JLabel("--");
        countdownLabel.setFont(new Font("Arial", Font.BOLD, 18));
        countdownLabel.setForeground(Color.WHITE);

        JButton returnButton = new JButton("Return to Main Menu");
        returnButton.addActionListener(e -> goToMainMenuOnce());

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
        panel.add(timerLabel, gbc);

        gbc.gridx = 1;
        gbc.insets = new Insets(10, 10, 10, 20);
        panel.add(countdownLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 20, 20, 20);
        panel.add(returnButton, gbc);

        setContentPane(panel);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Player.gracefulExit();
            }

            @Override
            public void windowClosed(WindowEvent e) {
            }
        });
    }

    private void goToMainMenuOnce() {
        if (navigating) return;
        navigating = true;
        dispose();
        PlayerMainMenuView.open(Player.getPlayerUsername());
    }
}