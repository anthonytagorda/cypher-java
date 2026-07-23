package cypher.player.views;

import cypher.player.Player;
import cypher.player.views.components.fonts.FontLoader;

import javax.swing.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.TimeUnit;

public class PlayerLoaderView extends JFrame {
    private JProgressBar loadingBar;
    private SwingWorker<Void, Void> worker;
    private boolean isCancelled = false;

    public PlayerLoaderView() {
        initComponents();
        setVisible(true);
        startProgress();
    }

    private void initComponents() {
        setTitle("Cypher | Loading");
        setSize(960, 540);
        setResizable(false);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        ImageIcon logo = new ImageIcon("src/cypher/assets/cypher_logo.png");
        setIconImage(logo.getImage());

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Image img = new ImageIcon("src/cypher/assets/cypher_loader.gif").getImage();
                g.drawImage(img, 0, 0, getWidth(), getHeight(), this);
            }
        };

        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 400));

        loadingBar = new JProgressBar(0, 100);
        loadingBar.setPreferredSize(new Dimension(840, 50));
        loadingBar.setStringPainted(true);
        loadingBar.setFont(FontLoader.loadFont(8f));
        loadingBar.setForeground(Color.decode("#644BA0"));
        loadingBar.setUI(new BasicProgressBarUI() {
            @Override
            protected Color getSelectionForeground() {
                return Color.BLACK;
            }
        });
        loadingBar.setUI(new BasicProgressBarUI() {
            @Override
            protected Color getSelectionForeground() {
                return Color.WHITE;
            }

            @Override
            protected Color getSelectionBackground() {
                return Color.BLACK;
            }
        });

        panel.add(loadingBar);
        add(panel);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                isCancelled = true;
                if (worker != null && !worker.isDone()) {
                    worker.cancel(true);
                }
                Player.logout();
                dispose();
            }
        });
    }

    private void startProgress() {
        worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                for (int i = 0; i <= 100; i++) {
                    if (isCancelled || Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    loadingBar.setValue(i);
                    TimeUnit.MILLISECONDS.sleep(18);
                }
                return null;
            }

            @Override
            protected void done() {
                if (!isCancelled && !Thread.currentThread().isInterrupted()) {
                    dispose();
                    new PlayerMainMenuView(Player.getPlayerUsername());
                }
            }
        };
        worker.execute();
    }
}