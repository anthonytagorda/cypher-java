package cypher.player.views;

import cypher.player.Player;
import cypher.player.views.components.fonts.FontLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PlayerRegisterView extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private ImageIcon successIcon;

    public PlayerRegisterView() {
        initComponents();
        setVisible(true);
    }

    private void initComponents() {
        JLabel usernameLabel = new JLabel("Username");
        JLabel passwordLabel = new JLabel("Password");
        JLabel confirmPasswordLabel = new JLabel("Confirm Pass");
        JLabel loginLabel = new JLabel("<html>Already have an account? <u>Log In!</u></html>");

        usernameField = new JTextField();
        passwordField = new JPasswordField();
        confirmPasswordField = new JPasswordField();

        JButton registerButton = new JButton("Register");

        ImageIcon appIcon = new ImageIcon("src/cypher/assets/cypher_logo.png");
        ImageIcon headerRaw = new ImageIcon("src/cypher/assets/cypher_register-header.png");
        Image headerScaled = headerRaw.getImage().getScaledInstance(370, 110, Image.SCALE_SMOOTH);
        JLabel imageLabel = new JLabel(new ImageIcon(headerScaled));
        imageLabel.setBounds(-250, -40, 900, 300);

        ImageIcon successRaw = new ImageIcon("src/cypher/assets/success.png");
        Image successScaled = successRaw.getImage().getScaledInstance(35, 35, Image.SCALE_SMOOTH);
        successIcon = new ImageIcon(successScaled);

        // Panel
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

        // Layout
        usernameLabel.setBounds(60, 180, 120, 25);
        passwordLabel.setBounds(60, 220, 120, 25);
        confirmPasswordLabel.setBounds(60, 260, 150, 25);

        usernameLabel.setFont(FontLoader.loadFont(9f));
        passwordLabel.setFont(FontLoader.loadFont(9f));
        confirmPasswordLabel.setFont(FontLoader.loadFont(9f));
        usernameField.setFont(FontLoader.loadFont(10f));

        usernameLabel.setForeground(Color.WHITE);
        passwordLabel.setForeground(Color.WHITE);
        confirmPasswordLabel.setForeground(Color.WHITE);

        usernameField.setBounds(190, 180, 150, 25);
        passwordField.setBounds(190, 220, 150, 25);
        confirmPasswordField.setBounds(190, 260, 150, 25);

        registerButton.setBounds(135, 310, 150, 40);
        registerButton.setFont(FontLoader.loadFont(12f));

        loginLabel.setBounds(60, 360, 300, 30);
        loginLabel.setForeground(Color.decode("#00ffff"));
        loginLabel.setFont(FontLoader.loadFont(9f));
        loginLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        contentPane.add(usernameLabel);
        contentPane.add(passwordLabel);
        contentPane.add(confirmPasswordLabel);
        contentPane.add(usernameField);
        contentPane.add(passwordField);
        contentPane.add(confirmPasswordField);
        contentPane.add(registerButton);
        contentPane.add(loginLabel);
        contentPane.add(imageLabel);

        // Frame config
        setTitle("Cypher | Player Registration");
        setSize(420, 440);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImage(appIcon.getImage());
        setResizable(false);
        setLocationRelativeTo(null);

        // Events
        registerButton.addActionListener(e -> register());
        loginLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new PlayerLoginView();
                dispose();
            }
        });
    }

    private void register() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        if (username == null || username.trim().isEmpty()) {
            showError("Username cannot be empty.");
            return;
        }
        if (!username.matches("[a-zA-Z0-9]+")) {
            showError("Username must contain only alphanumeric characters.");
            return;
        }
        if (username.trim().length() < 5) {
            showError("Username must be at least 5 characters.");
            return;
        }
        if (username.length() > 30) {
            showError("Username is too long.");
            return;
        }

        if (password.trim().isEmpty()) {
            showError("Password cannot be empty.");
            return;
        }
        if (password.length() < 6) {
            showError("Password must have at least 6 characters.");
            return;
        }
        if (password.length() > 30) {
            showError("Password is too long.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match.");
            return;
        }

        if (Player.register(username, password)) {
            JOptionPane.showMessageDialog(this, "Successfully created an account.", "Registration Successful",
                                          JOptionPane.INFORMATION_MESSAGE, successIcon);
            usernameField.setText("");
            passwordField.setText("");
            confirmPasswordField.setText("");
            dispose();
            new PlayerLoginView();
        } else {
            showError("Username already taken.");
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Registration Failed", JOptionPane.ERROR_MESSAGE);
    }
}