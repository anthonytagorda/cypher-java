package cypher.player.views.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A circular button with a transparent fill, a colored border, and centered text.
 */
public class CircleButton extends JComponent {

    private String text;
    private Color borderColor;
    private int borderThickness;
    private Color textColor;
    private Font font;
    private boolean pressed = false;
    private boolean hovered = false;

    public CircleButton(String text, Color borderColor) {
        this(text, borderColor, 3, Color.WHITE, new Font("Arial", Font.BOLD, 14));
    }

    public CircleButton(String text, Color borderColor, int borderThickness, Color textColor, Font font) {
        this.text = text;
        this.borderColor = borderColor;
        this.borderThickness = borderThickness;
        this.textColor = textColor;
        this.font = font;
        setOpaque(false);
        setFocusable(false);

        // Mouse listeners for visual feedback
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                pressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pressed = false;
                repaint();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                pressed = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int diameter = Math.min(getWidth(), getHeight());
        int x = (getWidth() - diameter) / 2;
        int y = (getHeight() - diameter) / 2;

        // Background (transparent – we don't fill anything)
        // Optionally, you could add a semi‑transparent overlay when pressed/hovered
        if (pressed) {
            g2.setColor(new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), 40));
            g2.fillOval(x, y, diameter, diameter);
        } else
            if (hovered) {
                g2.setColor(new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), 20));
                g2.fillOval(x, y, diameter, diameter);
            }

        // Border
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(borderThickness));
        g2.drawOval(x + borderThickness / 2, y + borderThickness / 2,
                    diameter - borderThickness, diameter - borderThickness);

        // Text
        g2.setColor(textColor);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent() - fm.getDescent();
        int textX = x + (diameter - textWidth) / 2;
        int textY = y + (diameter + textHeight) / 2 - 2;
        g2.drawString(text, textX, textY);

        g2.dispose();
    }

    // ─── Getters & Setters ──────────────────────────────────────

    public void setText(String text) {
        this.text = text;
        repaint();
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
        repaint();
    }

    public void setBorderThickness(int thickness) {
        this.borderThickness = thickness;
        repaint();
    }

    public void setTextColor(Color textColor) {
        this.textColor = textColor;
        repaint();
    }

    public void setFont(Font font) {
        this.font = font;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(120, 120); // default, but you can override with setBounds
    }
}