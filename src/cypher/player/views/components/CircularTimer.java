package cypher.player.views.components;

import javax.swing.*;
import java.awt.*;

/**
 * A circular countdown timer that draws a filled arc (like a "cake")
 * and displays the remaining seconds in the center.
 */
public class CircularTimer extends JPanel {
    private final int maxTime;
    private int timeLeft;
    private final Color startColor = Color.GREEN;
    private final Color endColor = Color.RED;

    public CircularTimer(int maxTime) {
        this.maxTime = maxTime;
        this.timeLeft = maxTime;
        setOpaque(false);
        setPreferredSize(new Dimension(120, 120));
    }

    /**
     * Updates the remaining time and repaints the component.
     */
    public void setTimeLeft(int timeLeft) {
        this.timeLeft = Math.max(0, Math.min(maxTime, timeLeft));
        repaint();
    }

    /**
     * Resets the timer to the initial maximum time.
     */
    public void reset() {
        this.timeLeft = maxTime;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int diameter = Math.min(width, height) - 16;
        int x = (width - diameter) / 2;
        int y = (height - diameter) / 2;

        // Background circle (empty)
        g2.setColor(new Color(60, 60, 60));
        g2.fillOval(x, y, diameter, diameter);

        // Progress: 0.0 (empty) to 1.0 (full)
        float progress = (float) timeLeft / maxTime;
        int arcAngle = (int) (progress * 360);

        // Color interpolation between start and end
        Color color = blendColors(startColor, endColor, 1 - progress);
        g2.setColor(color);

        // Draw filled arc starting at 12 o'clock (90 degrees, negative to go clockwise)
        g2.fillArc(x, y, diameter, diameter, 90, -arcAngle);

        // Border
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawOval(x, y, diameter, diameter);

        // Center text: remaining seconds
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 28));
        String text = String.valueOf(timeLeft);
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent();
        g2.drawString(text, (width - textWidth) / 2, (height + textHeight) / 2);

        g2.dispose();
    }

    /**
     * Blend two colors based on a ratio.
     */
    private Color blendColors(Color c1, Color c2, float ratio) {
        int r = (int) (c1.getRed() * (1 - ratio) + c2.getRed() * ratio);
        int g = (int) (c1.getGreen() * (1 - ratio) + c2.getGreen() * ratio);
        int b = (int) (c1.getBlue() * (1 - ratio) + c2.getBlue() * ratio);
        return new Color(r, g, b);
    }
}