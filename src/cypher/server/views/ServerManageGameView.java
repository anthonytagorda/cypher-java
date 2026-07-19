package cypher.server.views;

import cypher.server.Server;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ServerManageGameView extends JFrame {
    protected static ServerManageGameView instance;
    private Timer serverWatchTimer;

    protected ImageIcon icon = new ImageIcon("src/cypher/assets/cypher_logo.png");


    public ServerManageGameView() {
        setTitle("Server | Game Management");
        setSize(785, 460);
        setIconImage(icon.getImage());
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        startServerWatcher();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (serverWatchTimer != null) serverWatchTimer.stop();
                instance = null; // allow reopening later
            }
        });
    }

    public static ServerManageGameView getInstance() {
        if (instance == null) {
            instance = new ServerManageGameView();
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
