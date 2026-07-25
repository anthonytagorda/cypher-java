package cypher.player.views;

import cypher.player.Player;
import cypher.player.views.components.fonts.FontLoader;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class PlayerInGameView extends JFrame {
    private static final int ROUND_DURATION_SECS = 60;
    private static final int NUM_LETTERS = 20;
    private static final Color ACCENT = Color.decode("#FF10F0");
    private static final Color BG = Color.decode("#0f091e");
    private static final Color USED_COLOR = Color.LIGHT_GRAY;
    private static final Color ACTIVE_COLOR = Color.WHITE;
    private static final Color DISABLED_COLOR = Color.decode("#999999");

    private JLabel roundLabel;
    private JLabel timerLabel;
    private JLabel roundScoreLabel;
    private JLabel totalScoreLabel;
    private JLabel feedbackLabel;
    private JLabel timesUpLabel;
    private JTextField wordInputField;
    private JButton submitButton;
    private JPanel gridPanel;
    private JLabel submittedWordsLabel;

    private JButton[] letterButtons = new JButton[NUM_LETTERS];
    private boolean[] letterUsed = new boolean[NUM_LETTERS];
    private final HashSet<String> validWords;
    private final HashSet<String> validPrefixes;
    private final HashSet<String> submittedWords;
    private final List<Integer> selectedIndices = new ArrayList<>();
    private String letters = "";
    private int roundNumber = 1;
    private int timeLeft = ROUND_DURATION_SECS;
    private int roundScore = 0;
    private int totalScore = 0;
    private Timer timer;
    private final String currentPlayerUsername;

    public PlayerInGameView() {
        this.currentPlayerUsername = Player.getPlayerUsername();
        validWords = readValidWords();
        validPrefixes = buildPrefixSet(validWords);
        submittedWords = new HashSet<>();

        initComponents();
        setVisible(true);

        generateGameBoard(generateLetters()); // generate letters locally until server-side round dispatch is wired back in.
    }

    private void initComponents() {
        setTitle("Cypher | " + currentPlayerUsername.toUpperCase());
        setSize(960, 810);
        setResizable(false);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(null);
        setLocationRelativeTo(null);
        setIconImage(new ImageIcon("src/cypher/assets/cypher_logo.png").getImage());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Player.gracefulExit();
            }
        });

        JPanel background = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                ImageIcon icon = new ImageIcon("src/cypher/assets/cypher_ingame.gif");
                Image img = icon.getImage();

                // ─── BACKGROUND ZOOM FACTOR ──────────────────────────────────────────

                double zoomFactor = 0.8;  // 0.5 = half size, 1.0 = original, 1.5 = 150%

                int originalWidth = img.getWidth(this);
                int originalHeight = img.getHeight(this);

                int newWidth = (int) (originalWidth * zoomFactor);
                int newHeight = (int) (originalHeight * zoomFactor);

                // Center the image
                int x = (getWidth() - newWidth) / 2;
                int y = (getHeight() - newHeight) / 2;

                g.drawImage(img, x, y, newWidth, newHeight, this);
            }
        };
        background.setBounds(0, 0, 960, 810);
        setContentPane(background);

        // ── Header labels ─────────────────────────────────────────
        JLabel usernameLabel = new JLabel("Player: " + currentPlayerUsername.toUpperCase());
        usernameLabel.setBounds(390, 720, 400, 30);
        usernameLabel.setForeground(ACCENT);
        usernameLabel.setFont(FontLoader.loadFont(12f));
        background.add(usernameLabel);

        // Round / Score / Timer block — grouped together, top-right
        timerLabel = new JLabel("Time Left: " + timeLeft);
        timerLabel.setBounds(100, 670, 200, 25);
        timerLabel.setForeground(ACCENT);
        timerLabel.setFont(FontLoader.loadFont(14f));
        background.add(timerLabel);

        roundLabel = new JLabel("Round " + roundNumber);
        roundLabel.setBounds(40, 60, 200, 25);
        roundLabel.setForeground(ACCENT);
        roundLabel.setFont(FontLoader.loadFont(20f));
        background.add(roundLabel);

        roundScoreLabel = new JLabel("Round Score: 0");
        roundScoreLabel.setBounds(710, 110, 250, 25);
        roundScoreLabel.setForeground(ACCENT);
        roundScoreLabel.setFont(FontLoader.loadFont(12f));
        background.add(roundScoreLabel);

        totalScoreLabel = new JLabel("Total Score: 0");
        totalScoreLabel.setBounds(710, 130, 250, 25);
        totalScoreLabel.setForeground(ACCENT);
        totalScoreLabel.setFont(FontLoader.loadFont(12f));
        background.add(totalScoreLabel);

        // "Valid word!" / error feedback — centered above the grid
        feedbackLabel = new JLabel("<html><center></center></html>", SwingConstants.CENTER);
        feedbackLabel.setBounds(220, 640, 510, 30);
        feedbackLabel.setFont(FontLoader.loadFont(14f));
        feedbackLabel.setForeground(Color.WHITE);
        background.add(feedbackLabel);

        // "Time's Up!" banner — separate label, hidden until round ends
        timesUpLabel = new JLabel("Time's Up!", SwingConstants.CENTER);
        timesUpLabel.setBounds(220, 600, 510, 30);
        timesUpLabel.setFont(FontLoader.loadFont(16f));
        timesUpLabel.setForeground(Color.RED);
        timesUpLabel.setVisible(false);
        background.add(timesUpLabel);

        // CLEAR button — top-right corner of the letter grid
        JButton clearButton = new JButton("CLEAR");
        clearButton.setBounds(680, 190, 100, 30);
        clearButton.setFont(FontLoader.loadFont(14f));
        clearButton.setForeground(Color.BLACK);
        clearButton.setBackground(Color.WHITE);
        clearButton.setOpaque(true);
        clearButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK, 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        clearButton.setFocusPainted(false);
        clearButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                clearButton.setBackground(Color.LIGHT_GRAY);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                clearButton.setBackground(Color.WHITE);
            }
        });
        clearButton.addActionListener(e -> resetSelection());
        background.add(clearButton);

        // ── Letter grid ────────────────────────────────────────────
        gridPanel = new JPanel(new GridLayout(4, 5, 6, 6));
        gridPanel.setBackground(BG);
        gridPanel.setBounds(260, 175, 410, 350);
        background.add(gridPanel);

        // ── Submitted words panel ───────────────────────────────────
        JPanel submittedWordsPanel = new JPanel(null);
        submittedWordsPanel.setOpaque(false);

        JLabel submittedWordsTitle = new JLabel("Submitted Words");
        submittedWordsTitle.setFont(FontLoader.loadFont(12f));
        submittedWordsTitle.setForeground(ACCENT);
        submittedWordsTitle.setBounds(10, 10, 200, 20);
        submittedWordsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        submittedWordsLabel = new JLabel("<html></html>");
        submittedWordsLabel.setFont(FontLoader.loadFont(14f));
        submittedWordsLabel.setBounds(15, 20, 160, 300);
        submittedWordsLabel.setForeground(Color.GREEN);
        submittedWordsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        submittedWordsPanel.add(submittedWordsTitle);
        submittedWordsPanel.add(submittedWordsLabel);

        JScrollPane scrollPane = new JScrollPane(submittedWordsPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setBounds(10, 150, 200, 400);
        background.add(scrollPane);

        // ── Word input ───────────────────────────────────────────────
        wordInputField = new JTextField();
        wordInputField.setBounds(250, 50, 400, 90);
        wordInputField.setFont(FontLoader.loadFont(20f));

        wordInputField.setForeground(Color.GREEN);
        wordInputField.setOpaque(false);
        wordInputField.setEditable(false);
        wordInputField.addActionListener(e -> submitCurrentWord());
        background.add(wordInputField);

        submitButton = new JButton("SUBMIT");
        submitButton.setBounds(567, 595, 200, 40);
        submitButton.setFont(FontLoader.loadFont(14f));
        submitButton.setForeground(Color.GREEN);
        submitButton.setContentAreaFilled(false);
        submitButton.setBorderPainted(false);
        submitButton.setFocusPainted(false);
        submitButton.setOpaque(false);
        submitButton.addActionListener(e -> submitCurrentWord());
        background.add(submitButton);

        InputMap inputMap = background.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = background.getActionMap();

        // Enter to Submit
        inputMap.put(KeyStroke.getKeyStroke("ENTER"), "submitWord");

        actionMap.put("submitWord", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (submitButton.isEnabled()
                        && !wordInputField.getText().trim().isEmpty()) {
                    submitCurrentWord();
                }
            }
        });

        // Backspace to Erase
        inputMap.put(KeyStroke.getKeyStroke("BACK_SPACE"), "undoLetter");

        actionMap.put("undoLetter", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                undoLastLetter();
            }
        });

        timer = new Timer(1000, e -> {
            timeLeft--;

            timerLabel.setText("Time Left: " + timeLeft);

            if (timeLeft > 15) {
                timerLabel.setForeground(ACCENT);
            } else {
                timerLabel.setForeground(Color.RED);
            }

            if (timeLeft <= 0) {
                timer.stop();
                endRound();
            }
        });
    }

    private String generateLetters() {
        Random random = new Random();
        StringBuilder letters = new StringBuilder();
        String vowels = "aeiou";
        String consonants = "bcdfghjklmnpqrstvwxyz";

        for (int i = 0; i < 7; i++) {
            letters.append(vowels.charAt(random.nextInt(vowels.length())));
        }
        for (int i = 0; i < 13; i++) {
            letters.append(consonants.charAt(random.nextInt(consonants.length())));
        }

        List<Character> letterList = letters.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList());
        Collections.shuffle(letterList);

        StringBuilder shuffled = new StringBuilder();
        for (char c : letterList) {
            shuffled.append(c);
        }
        return shuffled.toString();
    }

    // ── Called by PlayerImpl.sendLetters(letters) once server dispatch is restored ──
    public void generateGameBoard(String newLetters) {
        this.letters = newLetters;
        selectedIndices.clear();
        wordInputField.setText("");
        gridPanel.removeAll();
        letterButtons = new JButton[newLetters.length()];
        letterUsed = new boolean[newLetters.length()];

        for (int i = 0; i < newLetters.length(); i++) {
            final int index = i;
            char c = newLetters.charAt(i);

            JButton letterButton = new JButton(String.valueOf(c).toUpperCase());
            letterButton.setPreferredSize(new Dimension(90, 90));
            letterButton.setFont(FontLoader.loadFont(20f));
            letterButton.setBackground(ACTIVE_COLOR);
            letterButton.setFocusPainted(false);
            letterButton.setBorderPainted(false);

            letterButton.addActionListener(e -> onLetterClicked(index));

            letterButtons[i] = letterButton;
            gridPanel.add(letterButton);
        }
        gridPanel.revalidate();
        gridPanel.repaint();

        refreshButtonStates();
        startRound();
    }

    private void onLetterClicked(int index) {
        JButton btn = letterButtons[index];

        // If this is the last selected letter, remove it (Backspace)
        if (!selectedIndices.isEmpty() && selectedIndices.get(selectedIndices.size() - 1) == index) {

            selectedIndices.remove(selectedIndices.size() - 1);
            letterUsed[index] = false;

            String current = wordInputField.getText();
            if (!current.isEmpty()) {
                wordInputField.setText(current.substring(0, current.length() - 1));
            }

            refreshButtonStates();
            return;
        }

        // Ignore already-used letters that aren't the last one
        if (letterUsed[index] || !btn.isEnabled()) {
            return;
        }

        // Select new letter
        letterUsed[index] = true;
        selectedIndices.add(index);

        wordInputField.setText(
                wordInputField.getText() + letters.charAt(index)
        );

        refreshButtonStates();
    }

    private void undoLastLetter() {
        if (selectedIndices.isEmpty()) {
            return;
        }

        int index = selectedIndices.remove(selectedIndices.size() - 1);
        letterUsed[index] = false;

        String current = wordInputField.getText();
        wordInputField.setText(current.substring(0, current.length() - 1));

        refreshButtonStates();
    }

    private void refreshButtonStates() {
        String currentPrefix = wordInputField.getText().toLowerCase();

        for (int i = 0; i < letterButtons.length; i++) {
            JButton btn = letterButtons[i];
            if (letterUsed[i]) {
                btn.setEnabled(true);
                btn.setBackground(USED_COLOR);
                continue;
            }

            String candidatePrefix = currentPrefix + Character.toLowerCase(letters.charAt(i));
            boolean canContinue = validPrefixes.contains(candidatePrefix) || validWords.contains(candidatePrefix);

            btn.setEnabled(canContinue);
            btn.setBackground(canContinue ? ACTIVE_COLOR : DISABLED_COLOR);
        }
    }

    private void resetSelection() {
        wordInputField.setText("");

        Arrays.fill(letterUsed, false);
        selectedIndices.clear();

        refreshButtonStates();
    }

    private void startRound() {
        timeLeft = ROUND_DURATION_SECS;

        timerLabel.setText("Time Left: " + timeLeft);
        timerLabel.setForeground(Color.GREEN);

        submittedWords.clear();
        submittedWordsLabel.setText("<html></html>");
        roundScore = 0;
        totalScore = 0;
        roundScoreLabel.setText("Round Score: " + roundScore);
        totalScoreLabel.setText("Total Score: " + totalScore);

        // Reset UI for a new round
        feedbackLabel.setText("");
        timesUpLabel.setVisible(false);
        submitButton.setEnabled(true);

        resetSelection();

        // Start the round timer
        if (timer != null) timer.start();
    }

    private void endRound() {
        timesUpLabel.setVisible(true);
        submitButton.setEnabled(false);
        for (JButton btn : letterButtons) {
            if (btn != null) btn.setEnabled(false);
        }

        // In a networked game the server should call the callbacks that trigger showRoundResult/showGameResult.
        if (cypher.player.Player.currentGame == null || cypher.player.Player.currentGame.gameId == 0) {
            // Local singleplayer: show the post-round dialog and allow play again/end game
            SwingUtilities.invokeLater(() -> showGameResult(currentPlayerUsername));
        } else
            if (cypher.player.Player.isSinglePlayerMode()) {
                // Server-backed singleplayer: show play-again/end-game dialog.
                SwingUtilities.invokeLater(() -> showGameResult(currentPlayerUsername));
            } else {
                // Server-backed game: only the host should request finalization. Non-hosts should wait
                // for the server to push results via callbacks.
                SwingUtilities.invokeLater(() -> {
                    try {
                        if (cypher.player.Player.isHost()) {
                            JOptionPane.showMessageDialog(this, "Time's up!", "Round Over", JOptionPane.INFORMATION_MESSAGE);
                            try {
                                cypher.player.Player.leaveGame();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            JOptionPane.showMessageDialog(this, "Time's up! Waiting for host to finalize results...", "Round Over", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } catch (Exception ignored) {
                    }
                });
            }
    }

    private void submitCurrentWord() {
        String word = wordInputField.getText().trim().toLowerCase();
        if (!word.isEmpty()) {
            validateWord(word);
        }
        resetSelection();
    }

    private void validateWord(String word) {
        if (submittedWords.contains(word)) {
            showFeedback("Already submitted!", Color.RED);
            return;
        }
        if (word.length() < 4) {
            showFeedback("Too short! (min 4 letters)", Color.RED);
            return;
        }
        if (!validWords.contains(word)) {
            showFeedback("Invalid word!", Color.RED);
            return;
        }

        try {
            Player.submitWord(word);
            showFeedback("Valid word!", Color.GREEN);
            submittedWords.add(word);
            updateSubmittedWordsDisplay(word);
        } catch (Exception ex) {
            showFeedback(ex.getMessage() != null ? ex.getMessage() : "Word rejected by server.", Color.RED);
        }
    }

    private void showFeedback(String message, Color color) {
        feedbackLabel.setText("<html><center>" + message + "</center></html>");
        feedbackLabel.setForeground(color);
    }

    private void updateSubmittedWordsDisplay(String word) {
        roundScore += word.length();
        totalScore += word.length();
        String currentText = submittedWordsLabel.getText().replace("</html>", "");
        currentText += "<br>" + word + " (+" + word.length() + ")</html>";
        submittedWordsLabel.setText(currentText);
        roundScoreLabel.setText("Round Score: " + roundScore);
    }

    private HashSet<String> readValidWords() {
        HashSet<String> words = new HashSet<>();
        File file = new File("src/words.txt");
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                words.add(line.trim().toLowerCase());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return words;
    }

    private HashSet<String> buildPrefixSet(HashSet<String> words) {
        HashSet<String> prefixes = new HashSet<>();
        for (String w : words) {
            for (int len = 1; len <= w.length(); len++) {
                prefixes.add(w.substring(0, len));
            }
        }
        return prefixes;
    }

    public void showRoundResult(String roundWinner, boolean isGameEnd) {
        SwingUtilities.invokeLater(() -> {
            String winnerText = (roundWinner == null || roundWinner.isEmpty())
                    ? "No one won this round."
                    : roundWinner + " won this round.";

            JOptionPane optionPane = new JOptionPane(
                    winnerText + "\nRound score: " + roundScore + "\nTotal score: " + totalScore + "\n\n" +
                            (isGameEnd ? "Showing final results shortly..." : "Next round starting shortly..."),
                    JOptionPane.PLAIN_MESSAGE);

            JDialog dialog = optionPane.createDialog("Round Over!");
            dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            dialog.setIconImage(new ImageIcon("src/cypher/assets/cypher_logo.png").getImage());

            Timer closeTimer = new Timer(5000, e -> {
                dialog.dispose();
                if (isGameEnd) {
                    dispose();
                    new PlayerWinnerView(currentPlayerUsername, totalScore);
                    return;
                }
                roundNumber++;
                roundLabel.setText("Round " + roundNumber);
                generateGameBoard(generateLetters());
            });
            closeTimer.setRepeats(false);
            closeTimer.start();
            dialog.setVisible(true);
        });
    }

    public void showGameResult(String gameWinner) {
        SwingUtilities.invokeLater(() -> {
            // Multiplayer server-backed game: server already finalized and sent results.
            // Singleplayer server-backed games should still show the play-again/end-game option.
            if (cypher.player.Player.currentGame != null
                    && cypher.player.Player.currentGame.gameId > 0
                    && !cypher.player.Player.isSinglePlayerMode()) {
                JOptionPane.showMessageDialog(this,
                                              gameWinner.toUpperCase() + " won the game!",
                                              "Game Over",
                                              JOptionPane.INFORMATION_MESSAGE);
                dispose();
                new PlayerWinnerView(gameWinner, totalScore);
                return;
            }

            // Local singleplayer: offer the player a choice to play one more round or end the game
            Object[] options = {"Play another round", "End game"};
            ImageIcon icon = new ImageIcon("src/cypher/assets/cypher_logo.png");
            String message = gameWinner.toUpperCase() + " won this round!\nRound score: " + roundScore + "\nTotal score: " + totalScore + "\n\nWould you like to play another round?";
            int choice = JOptionPane.showOptionDialog(this,
                                                      message,
                                                      "Round Over",
                                                      JOptionPane.YES_NO_OPTION,
                                                      JOptionPane.QUESTION_MESSAGE,
                                                      icon,
                                                      options,
                                                      options[0]);

            if (choice == 0) {
                roundNumber++;
                roundLabel.setText("Round " + roundNumber);

                // Start another round. If this session is backed by the server, request the server
                // to start the next round so the DB `rounds_played` increments. Otherwise, run locally.
                if (cypher.player.Player.currentGame != null && cypher.player.Player.currentGame.gameId > 0) {
                    try {
                        // ask server to start the next round; server will push letters via callback
                        cypher.player.Player.startGame(cypher.player.Player.currentGame.gameId);
                        // show an info dialog while waiting for server to send letters
                        JOptionPane.showMessageDialog(this, "Starting next round...", "Info", JOptionPane.INFORMATION_MESSAGE);
                        // Do not locally generate board; wait for server.sendLetters callback
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                        // fallback to local round if server call fails
                    }
                }

                // Local-only: start another local round
                generateGameBoard(generateLetters());
            } else {
                // End the game and show final results. If this was a server-backed game, notify server to finalize
                if (cypher.player.Player.currentGame != null && cypher.player.Player.currentGame.gameId > 0) {
                    try {
                        cypher.player.Player.leaveGame();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    JOptionPane.showMessageDialog(this, "Ending game... \nwaiting for final result from server.", "Info", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                dispose();
                new PlayerWinnerView(gameWinner, totalScore);
            }
        });
    }

    public void showServerFinalGameResult(String gameWinner) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                                          gameWinner.toUpperCase() + " won the game!",
                                          "Game Over",
                                          JOptionPane.INFORMATION_MESSAGE);
            dispose();
            new PlayerWinnerView(gameWinner, totalScore);
        });
    }
}