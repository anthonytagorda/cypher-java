package cypher.player.views.components;

import javax.swing.*;
import java.awt.*;

public class RoundButton extends JButton {
	 protected Color buttonColor;

	 public RoundButton(String text, Color buttonColor) {
		  super(text);
		  setContentAreaFilled(false);
		  setOpaque(false);
		  setForeground(Color.WHITE);
		  this.buttonColor = buttonColor;
	 }

	 @Override
	 protected void paintComponent(Graphics g) {
		  if (getModel().isArmed()) {
				g.setColor(Color.lightGray);
		  } else {
				g.setColor(buttonColor);
		  }
		  g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 50, 50);
		  super.paintComponent(g);
	 }

	 @Override
	 protected void paintBorder(Graphics g) {
		  g.setColor(getForeground());
		  g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 50, 50);
	 }
}
