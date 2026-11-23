package presentation;

import domain.Log;
import domain.Power;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/** Loads and caches stadium, ball, and character visual assets. */
public final class AssetManager {

    public static final double STADIUM_GRASS_RATIO = 621.0 / 768.0;
    private static final String STADIUM_FILE = "stadium_bg.png";
    private static final String BALL_FILE = "ball.png";

    private static BufferedImage stadiumImage;
    private static BufferedImage ballImage;
    private static final Map<String, BufferedImage> characterCache = new HashMap<>();

    /** Prevents instantiation of utility class. */
    private AssetManager() {
    }

    /** Returns the loaded stadium background image. */
    public static BufferedImage getStadiumImage() {
        if (stadiumImage == null) {
            stadiumImage = loadImage(STADIUM_FILE);
            if (stadiumImage == null) {
                stadiumImage = createFallbackStadium(1050, 630);
            }
        }
        return stadiumImage;
    }

    /** Returns the loaded soccer ball sprite. */
    public static BufferedImage getBallImage() {
        if (ballImage == null) {
            ballImage = loadImage(BALL_FILE);
            if (ballImage == null) {
                ballImage = createFallbackBall(128);
            }
        }
        return ballImage;
    }

    /** Returns a rendered big-head character portrait for the given country. */
    public static BufferedImage getCharacterHead(String country) {
        String key = (country != null) ? country : Power.COUNTRY_KOREA;
        if (!characterCache.containsKey(key)) {
            characterCache.put(key, createCharacterHeadSprite(key));
        }
        return characterCache.get(key);
    }

    /** Loads a PNG asset from the classpath or local images folder. */
    private static BufferedImage loadImage(String fileName) {
        try {
            URL url = AssetManager.class.getResource("/images/" + fileName);
            if (url != null) {
                return ImageIO.read(url);
            }
            File local = new File("images", fileName);
            if (local.isFile()) {
                return ImageIO.read(local);
            }
            File nested = new File("HEADSOCCER2/images", fileName);
            if (nested.isFile()) {
                return ImageIO.read(nested);
            }
        } catch (IOException e) {
            Log.record(e);
        }
        return null;
    }

    /** Generates a minimal fallback stadium backdrop when the image asset is unavailable. */
    private static BufferedImage createFallbackStadium(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(18, 32, 74));
        g.fillRect(0, 0, w, h);
        int grassY = (int) Math.round(h * STADIUM_GRASS_RATIO);
        g.setColor(new Color(42, 150, 48));
        g.fillRect(0, grassY, w, h - grassY);
        g.dispose();
        return img;
    }

    /** Generates a minimal fallback soccer ball sprite when the image asset is unavailable. */
    private static BufferedImage createFallbackBall(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillOval(4, 4, size - 8, size - 8);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(3f));
        g.drawOval(4, 4, size - 8, size - 8);
        g.dispose();
        return img;
    }

    /** Generates a character head sprite themed by country. */
    private static BufferedImage createCharacterHeadSprite(String country) {
        int size = 128;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int cx = size / 2;
        int cy = size / 2 + 4;
        int r = 46;
        String lower = country.toLowerCase();
        Color[] palette = getCountryPalette(lower);

        drawCharacterHair(g, cx, cy, r, lower, palette[1]);
        g.setColor(palette[0]);
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        g.setColor(new Color(40, 30, 25));
        g.setStroke(new BasicStroke(3.5f));
        g.drawOval(cx - r, cy - r, r * 2, r * 2);
        drawCharacterFace(g, cx, cy, r, lower, palette[2], palette[3]);

        g.dispose();
        return img;
    }

    /** Returns the [skin, hair, bandBackground, bandAccent] colors for a country. */
    private static Color[] getCountryPalette(String lower) {
        if (lower.contains("camer")) {
            return new Color[]{
                new Color(115, 72, 46), new Color(20, 20, 22),
                new Color(0, 145, 70), new Color(245, 205, 30)
            };
        }
        if (lower.contains("nigeria")) {
            return new Color[]{
                new Color(102, 62, 38), new Color(20, 20, 22),
                new Color(0, 135, 81), Color.WHITE
            };
        }
        if (lower.contains("estados") || lower.contains("usa")) {
            return new Color[]{
                new Color(244, 198, 164), new Color(195, 148, 52),
                new Color(25, 65, 165), new Color(215, 40, 45)
            };
        }
        return new Color[]{
            new Color(250, 214, 180), new Color(22, 24, 32),
            new Color(220, 40, 50), new Color(30, 80, 200)
        };
    }

    /** Draws stylized hair shapes for the character sprite. */
    private static void drawCharacterHair(Graphics2D g, int cx, int cy, int r, String lower, Color hair) {
        g.setColor(hair);
        if (lower.contains("camer")) {
            for (int i = -3; i <= 3; i++) {
                g.fillRoundRect(cx + i * 11 - 5, cy - r - 12, 10, 26, 8, 8);
            }
        } else {
            for (int i = -3; i <= 3; i++) {
                int sx = cx + i * 12;
                g.fillPolygon(new int[]{sx - 9, sx, sx + 9},
                              new int[]{cy - r + 14, cy - r - 14 + Math.abs(i) * 3, cy - r + 14}, 3);
            }
        }
    }

    /** Draws headband, eyes, eyebrows, smile, and glasses details on a character head. */
    private static void drawCharacterFace(Graphics2D g, int cx, int cy, int r,
                                          String lower, Color bandBg, Color bandAccent) {
        g.setColor(bandBg);
        g.fillRoundRect(cx - r + 2, cy - r + 14, r * 2 - 4, 16, 8, 8);
        g.setColor(bandAccent);
        g.fillRect(cx - 10, cy - r + 16, 20, 12);

        g.setColor(Color.WHITE);
        g.fillOval(cx - 22, cy - 8, 16, 18);
        g.fillOval(cx + 6, cy - 8, 16, 18);
        g.setColor(Color.BLACK);
        g.fillOval(cx - 16, cy - 3, 8, 10);
        g.fillOval(cx + 12, cy - 3, 8, 10);

        g.setStroke(new BasicStroke(3.2f));
        g.drawLine(cx - 24, cy - 13, cx - 6, cy - 9);
        g.drawLine(cx + 24, cy - 13, cx + 6, cy - 9);

        g.setColor(Color.WHITE);
        g.fillArc(cx - 14, cy + 12, 28, 16, 180, 180);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2.2f));
        g.drawArc(cx - 14, cy + 12, 28, 16, 180, 180);

        if (lower.contains("nigeria")) {
            g.setColor(new Color(20, 200, 100, 175));
            g.fillRoundRect(cx - 26, cy - 10, 52, 20, 8, 8);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(cx - 26, cy - 10, 52, 20, 8, 8);
        }
    }

    /** Draws a rotated soccer ball sprite centered at (x, y). */
    public static void drawRotatedBall(Graphics2D g, double x, double y, int radius, double angle) {
        BufferedImage img = getBallImage();
        AffineTransform old = g.getTransform();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.translate(x, y);
        g.rotate(angle);
        g.drawImage(img, -radius, -radius, radius * 2, radius * 2, null);
        g.setTransform(old);
    }
}
