package cypher.player.views;

import cypher.player.Player;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PlayerLoginView extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;

    public PlayerLoginView() {
        initComponents();
        setVisible(true);
    }

    private void initComponents() {
        JLabel usernameLabel = new JLabel("Username");
        JLabel passwordLabel = new JLabel("Password");
        JLabel signUpLabel = new JLabel("<html>Don't have an account? <u>Register!</u></html>");

        usernameField = new JTextField();
        passwordField = new JPasswordField();

        JButton loginButton = new JButton("Login");

        ImageIcon appIcon = new ImageIcon("src/cypher/assets/cypher_logo.png");

        ImageIcon headerRaw = new ImageIcon("src/cypher/assets/cypher_login-header.png");
        Image headerScaled = headerRaw.getImage().getScaledInstance(350, 100, Image.SCALE_SMOOTH);
        JLabel imageLabel = new JLabel(new ImageIcon(headerScaled));

        JPanel contentPane = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                ImageIcon bgImage = new ImageIcon("src/cypher/assets/cypher_space-bg.gif");
                super.paintComponent(g);
                g.drawImage(bgImage.getImage(), 0, 0, getWidth(), getHeight(), this);
            }
        };
        contentPane.setLayout(null);
        setContentPane(contentPane);

        // Frame
        setTitle("Cypher | Player Login");
        setSize(420, 440);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setIconImage(appIcon.getImage());
        setResizable(false);
        setLocationRelativeTo(null);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                Player.gracefulExit();
            }
        });

        // Layout
        usernameLabel.setBounds(70, 210, 120, 25);
        usernameLabel.setForeground(Color.WHITE);

        passwordLabel.setBounds(70, 250, 120, 25);
        passwordLabel.setForeground(Color.WHITE);

        signUpLabel.setBounds(105, 335, 220, 30);
        signUpLabel.setForeground(Color.decode("#00ffff"));
        signUpLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        usernameField.setBounds(190, 210, 120, 25);
        passwordField.setBounds(190, 250, 120, 25);

        loginButton.setBounds(140, 300, 120, 30);

        imageLabel.setBounds(-250, -40, 900, 300);

        contentPane.add(usernameLabel);
        contentPane.add(passwordLabel);
        contentPane.add(signUpLabel);
        contentPane.add(usernameField);
        contentPane.add(passwordField);
        contentPane.add(loginButton);
        contentPane.add(imageLabel);

        // Events
        loginButton.addActionListener(this::login);
        signUpLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                register();
            }
        });
    }

    private void login(ActionEvent actionEvent) {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (username == null || username.trim().isEmpty()) {
            showError("Username cannot be empty.");
            return;
        } else
            if (!username.matches("[a-zA-Z0-9]+")) {
                showError("Username must contain only alphanumeric characters.");
                return;
            } else
                if (username.trim().length() < 5) {
                    showError("Username must be at least 5 characters.");
                    return;
                }

        if (password.trim().isEmpty()) {
            showError("Password cannot be empty.");
            return;
        } else
            if (password.length() < 6) {
                showError("Password too short.");
                return;
            }

        int result = Player.login(username, password);

        if (result == 0) {
            dispose();
            new PlayerLoaderView();
        } else
            if (result == 1) {
                showError("Already logged in.");
            } else
                if (result == 2) {
                    showError("Invalid username or password.");
                } else
                    if (result == 3) {
                        showError("Your account is banned.");
                    } else {
                        showError("An error has occurred. Please try again later.");
                    }
    }

    private void register() {
        dispose();
        new PlayerRegisterView();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Login Failed", JOptionPane.ERROR_MESSAGE);
    }
}