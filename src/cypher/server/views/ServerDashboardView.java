package cypher.server.views;

import cypher.server.Server;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.OutputStream;
import java.io.PrintStream;

public class ServerDashboardView extends JFrame {
    // Logo
    private final ImageIcon logo = new ImageIcon("src/cypher/assets/cypher_logo.png");

    // Color
    private final Color primaryColor = Color.decode("#7b44e4");
    private final Color secondaryColor = Color.decode("#212121");

    // Log
    private JTextArea logText;

    // Prevent double-close execution
    private boolean closing = false;

    public ServerDashboardView() {
        initComponents();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @SuppressWarnings("finally")
            @Override
            public void windowClosing(WindowEvent e) {
                if (closing) return;
                closing = true;
                try {
                    Server.shutdownServer();
                } finally {
                    dispose();
                    System.exit(0);
                }
            }
        });
        setVisible(true);
    }

    private void initComponents() {
        // Border
        Border primaryBorder = BorderFactory.createLineBorder(primaryColor);
        Border secondaryBorder = BorderFactory.createLineBorder(secondaryColor);

        // Buttons
        JButton startServerButton = new JButton("Start Server");
        JButton stopServerButton = new JButton("Stop Server");
        JButton manageUsersButton = new JButton("Manage Users");
        JButton manageGameButton = new JButton("Manage Game");
        JButton clearButton = new JButton("Clear");

        // Panels
        JPanel sidePanel = new JPanel();
        JPanel mainPanel = new JPanel();

        /* Button Events */
        // Start Server Button Function
        startServerButton.addActionListener(startServer -> {
            try {
                Server.startServer();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // Stop Server Button Function
        stopServerButton.addActionListener(stopServer -> {
            clearLogText();
            Server.shutdownServer();
        });

        // Manage Users Button Function
        manageUsersButton.addActionListener(e -> {
            if (!Server.isRunning()) {
                JOptionPane.showMessageDialog(this, "Server is not running. Please start the server first.", "Server Not Running", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ServerManageUsersView view = new ServerManageUsersView();
            view.setVisible(true);
        });

        // Manage Game Button Function
        manageGameButton.addActionListener(e -> {
            if (!Server.isRunning()) {
                JOptionPane.showMessageDialog(this, "Server is not running. Please start the server first.", "Server Not Running", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ServerManageGameView view = new ServerManageGameView();
            view.setVisible(true);
        });

        // Clear Button Function
        clearButton.addActionListener(e -> clearLogText());

        // Dashboard Logo
        Image logo_original = logo.getImage();
        Image logo_resized = logo_original.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
        ImageIcon resizedLogo = new ImageIcon(logo_resized);
        JLabel logoLabel = new JLabel(resizedLogo);

        /* GRAPHICAL INTERFACE CONFIGURATIONS */
        // Frame Configuration
        setTitle("Server | Dashboard");
        setSize(750, 500);
        setResizable(false);
        setLocationRelativeTo(null);
        setIconImage(logo.getImage());

        // Side Panel Configuration
        add(sidePanel);
        sidePanel.setLayout(null);
        sidePanel.setBorder(primaryBorder);
        sidePanel.setBackground(Color.decode("#212121"));
        sidePanel.setBounds(0, 0, 200, 461);

        // Logo
        sidePanel.add(logoLabel);
        logoLabel.setBounds(20, -40, 170, 200);

        // Buttons
        sidePanel.add(startServerButton);
        startServerButton.setBounds(30, 340, 150, 25);
        startServerButton.setBackground(Color.decode("#30DE21"));

        sidePanel.add(stopServerButton);
        stopServerButton.setBounds(30, 370, 150, 25);
        stopServerButton.setBackground(Color.decode("#FF2147"));

        sidePanel.add(manageUsersButton);
        manageUsersButton.setBounds(30, 130, 150, 25);
        manageUsersButton.setBackground(Color.decode("#D9D9D9"));

        // Game Config Button
        sidePanel.add(manageGameButton);
        manageGameButton.setBounds(30, 170, 150, 25);
        manageGameButton.setBackground(Color.decode("#D9D9D9"));

        // MainPanel Configuration
        add(mainPanel);
        mainPanel.setLayout(null);
        mainPanel.setBorder(primaryBorder);
        mainPanel.setBackground(Color.decode("#212121"));
        mainPanel.setBounds(200, 0, 550, 500);

        // Logs
        logText = new JTextArea(20, 20);
        JScrollPane scrollPane = new JScrollPane(logText);
        JLabel logLabel = new JLabel("Server Audit Log");
        logLabel.setForeground(Color.WHITE);
        logLabel.setBounds(210, 10, 100, 20);
        redirectSystemStreams(); // Redirect output streams to server audit log textarea

        logText.setBackground(Color.decode("#2b2d30"));
        logText.setEditable(false);
        logText.setLineWrap(true);
        logText.setWrapStyleWord(true);
        logText.setForeground(Color.WHITE);
        logText.setFont(new Font("Roboto", Font.PLAIN, 12));
        logText.setMargin(new Insets(8, 10, 8, 10));

        scrollPane.setBounds(210, 42, 515, 288);
        scrollPane.setBorder(secondaryBorder);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        // Clear Button
        mainPanel.add(clearButton);
        clearButton.setBounds(625, 340, 100, 25);
        clearButton.setBackground(Color.decode("#D9D9D9"));

        mainPanel.add(scrollPane);
        mainPanel.add(logLabel);
    }

    private void redirectSystemStreams() {
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) {
                updateTextArea(String.valueOf((char) b));
            }

            @Override
            public void write(byte[] b, int off, int len) {
                updateTextArea(new String(b, off, len));
            }

            @Override
            public void write(byte[] b) {
                write(b, 0, b.length);
            }
        };

        // Redirect standard output to the custom server audit logs
        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }

    // Update the server audit logs
    private void updateTextArea(final String text) {
        SwingUtilities.invokeLater(() -> {
            logText.append(text);
            logText.setCaretPosition(logText.getDocument().getLength());
        });
    }

    // Clear server audit logs
    public void clearLogText() {
        logText.setText("");
    }
}