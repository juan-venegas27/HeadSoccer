package presentation;

import domain.CharacterRegistry;
import domain.Log;
import domain.Power;

import javax.imageio.ImageIO;
import java.awt.*;
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

    /**
     * Prevents instantiation of utility class.
     */
    private AssetManager() {
    }

    /**
     * Returns the loaded stadium background image.
     * @return the loaded stadium background image
     */
    public static BufferedImage getStadiumImage() {
        if (stadiumImage == null) {
            stadiumImage = loadImage(STADIUM_FILE);
            if (stadiumImage == null) {
                stadiumImage = createFallbackStadium(1050, 630);
            }
        }
        return stadiumImage;
    }

    /**
     * Returns the loaded soccer ball sprite.
     * @return the loaded soccer ball sprite
     */
    public static BufferedImage getBallImage() {
        if (ballImage == null) {
            ballImage = loadImage(BALL_FILE);
            if (ballImage == null) {
                ballImage = createFallbackBall(128);
            }
        }
        return ballImage;
    }

    /**
     * Returns the loaded character portrait image for the given country.
     * @param country selected country name
     * @return the loaded character portrait image for the given country
     */
    public static BufferedImage getCharacterHead(String country) {
        String key = (country != null) ? country : Power.COUNTRY_KOREA;
        if (!characterCache.containsKey(key)) {
            String fileName = CharacterRegistry.get(key).getImageFileName();
            BufferedImage head = loadImage(fileName);
            if (head == null) {
                head = createFallbackHead(128);
            }
            characterCache.put(key, head);
        }
        return characterCache.get(key);
    }

    /**
     * Loads a PNG asset from the classpath or local images folder.
     * @param fileName image file name inside the images directory
     * @return loaded image, or null if not found
     */
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

    /**
     * Generates a minimal fallback stadium backdrop when the image asset is unavailable.
     * @param w width in pixels
     * @param h height in pixels
     * @return simple drawn fallback stadium image
     */
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

    /**
     * Generates a minimal fallback soccer ball sprite when the image asset is unavailable.
     * @param size square image side length in pixels
     * @return simple drawn fallback ball image
     */
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

    /**
     * Generates a minimal fallback character head sprite when the image asset is unavailable.
     * @param size square image side length in pixels
     * @return simple drawn fallback head image
     */
    private static BufferedImage createFallbackHead(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(235, 200, 170));
        g.fillOval(18, 18, size - 36, size - 36);
        g.setColor(new Color(40, 30, 25));
        g.setStroke(new BasicStroke(3.5f));
        g.drawOval(18, 18, size - 36, size - 36);
        g.dispose();
        return img;
    }

    /**
     * Draws the static soccer ball image centered at (x, y).
     * @param g 2D graphics context used for drawing
     * @param x horizontal coordinate
     * @param y vertical coordinate
     * @param radius ball radius in pixels
     */
    public static void drawBall(Graphics2D g, double x, double y, int radius) {
        BufferedImage img = getBallImage();
        int drawX = (int) Math.round(x) - radius;
        int drawY = (int) Math.round(y) - radius;
        g.drawImage(img, drawX, drawY, radius * 2, radius * 2, null);
    }
}
