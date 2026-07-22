package cypher.player.views.components.fonts;

import java.awt.*;
import java.io.InputStream;

public class FontLoader {

    private static Font baseFont;

    static {
        try {
            InputStream is = FontLoader.class.getResourceAsStream("/cypher/assets/fonts/PressStart2P.ttf");

            if (is == null) {
                throw new RuntimeException("Font not found: /cypher/assets/fonts/PressStart2P.ttf");
            }

            baseFont = Font.createFont(Font.TRUETYPE_FONT, is);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(baseFont);

        } catch (Exception e) {
            e.printStackTrace();
            baseFont = new Font("SansSerif", Font.PLAIN, 12);
        }
    }

    public static Font loadFont(float size) {
        return baseFont.deriveFont(size);
    }
}