package src.presentation;

import src.domain.Power;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates and caches procedural 2D arcade visuals in memory:
 * stadium background, soccer ball sprite, and country character heads.
 */
public final class AssetManager {

    private static BufferedImage stadiumImage;
    private static BufferedImage ballImage;
    private static final Map<String, BufferedImage> characterCache = new HashMap<>();

    private AssetManager() {
    }

    /** Returns the procedural stadium background image. */
    public static BufferedImage getStadiumImage() {
        if (stadiumImage == null) {
            stadiumImage = createStadiumImage(1050, 630);
        }
        return stadiumImage;
    }

    /** Returns the procedural classic soccer ball sprite. */
    public static BufferedImage getBallImage() {
        if (ballImage == null) {
            ballImage = createBallImage(128);
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

    private static BufferedImage createStadiumImage(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        GradientPaint sky = new GradientPaint(0, 0, new Color(12, 22, 58), 0, h * 0.65f, new Color(40, 82, 150));
        g.setPaint(sky);
        g.fillRect(0, 0, w, h);

        g.setColor(new Color(255, 255, 210, 35));
        g.fillPolygon(new int[]{80, 320, 420, 40}, new int[]{35, h - 60, h - 60, 35}, 4);
        g.fillPolygon(new int[]{w - 80, w - 40, w - 420, w - 320}, new int[]{35, h - 60, h - 60, 35}, 4);

        drawFloodlight(g, 95, 55);
        drawFloodlight(g, w - 95, 55);

        int standsTop = (int) (h * 0.42);
        int standsBottom = h - 60;
        g.setColor(new Color(30, 38, 65));
        g.fillRect(0, standsTop, w, standsBottom - standsTop);

        Color[] crowdColors = {
            new Color(220, 60, 60), new Color(60, 140, 230),
            new Color(240, 200, 40), new Color(240, 240, 240), new Color(70, 200, 100)
        };
        for (int row = standsTop + 18; row < standsBottom - 26; row += 22) {
            for (int col = 10; col < w; col += 18) {
                g.setColor(crowdColors[(row + col) % crowdColors.length]);
                g.fillOval(col, row, 10, 10);
                g.fillRoundRect(col - 1, row + 8, 12, 12, 4, 4);
            }
        }

        g.setColor(new Color(22, 30, 50));
        g.fillRect(0, standsBottom - 26, w, 26);
        g.setColor(new Color(255, 204, 0));
        g.fillRect(0, standsBottom - 26, w, 3);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.setColor(Color.WHITE);
        for (int x = 30; x < w; x += 210) {
            g.drawString("DOPOSOCCER 2026", x, standsBottom - 8);
        }

        g.dispose();
        return img;
    }

    private static void drawFloodlight(Graphics2D g, int cx, int cy) {
        g.setColor(new Color(70, 80, 100));
        g.fillRect(cx - 5, cy, 10, 140);
        g.setColor(new Color(210, 220, 235));
        g.fillRoundRect(cx - 32, cy - 18, 64, 28, 6, 6);
        g.setColor(new Color(255, 250, 190));
        for (int lx = cx - 24; lx <= cx + 16; lx += 14) {
            g.fillOval(lx, cy - 12, 10, 10);
            g.fillOval(lx, cy - 1, 10, 10);
        }
    }

    private static BufferedImage createBallImage(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int pad = 4;
        int d = size - pad * 2;
        int r = d / 2;
        int cx = size / 2;
        int cy = size / 2;

        RadialGradientPaint paint = new RadialGradientPaint(
                cx - r * 0.3f, cy - r * 0.3f, r * 1.1f,
                new float[]{0f, 0.7f, 1f},
                new Color[]{Color.WHITE, new Color(235, 235, 240), new Color(185, 190, 200)}
        );
        g.setPaint(paint);
        g.fillOval(pad, pad, d, d);

        Shape oldClip = g.getClip();
        g.setClip(new java.awt.geom.Ellipse2D.Double(pad, pad, d, d));

        g.setColor(new Color(30, 30, 35));
        drawPolygon(g, cx, cy, (int) (r * 0.34), 5);
        for (int i = 0; i < 5; i++) {
            double ang = i * (2 * Math.PI / 5) - Math.PI / 2;
            int px = (int) (cx + Math.cos(ang) * r * 0.85);
            int py = (int) (cy + Math.sin(ang) * r * 0.85);
            drawPolygon(g, px, py, (int) (r * 0.28), 5);
            g.setStroke(new BasicStroke(3f));
            g.drawLine((int) (cx + Math.cos(ang) * r * 0.32),
                       (int) (cy + Math.sin(ang) * r * 0.32), px, py);
        }

        g.setClip(oldClip);
        g.setColor(new Color(20, 20, 20));
        g.setStroke(new BasicStroke(3f));
        g.drawOval(pad, pad, d, d);
        g.dispose();
        return img;
    }

    private static void drawPolygon(Graphics2D g, int cx, int cy, int rad, int sides) {
        int[] xs = new int[sides];
        int[] ys = new int[sides];
        for (int i = 0; i < sides; i++) {
            double a = i * 2 * Math.PI / sides - Math.PI / 2;
            xs[i] = (int) (cx + Math.cos(a) * rad);
            ys[i] = (int) (cy + Math.sin(a) * rad);
        }
        g.fillPolygon(xs, ys, sides);
    }

    private static BufferedImage createCharacterHeadSprite(String country) {
        int size = 128;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int cx = size / 2;
        int cy = size / 2 + 4;
        int r = 46;

        Color skin;
        Color hair;
        Color bandBg;
        Color bandAccent;

        String lower = country.toLowerCase();
        if (lower.contains("camer")) {
            skin = new Color(115, 72, 46);
            hair = new Color(20, 20, 22);
            bandBg = new Color(0, 145, 70);
            bandAccent = new Color(245, 205, 30);
        } else if (lower.contains("nigeria")) {
            skin = new Color(102, 62, 38);
            hair = new Color(20, 20, 22);
            bandBg = new Color(0, 135, 81);
            bandAccent = Color.WHITE;
        } else if (lower.contains("estados") || lower.contains("usa")) {
            skin = new Color(244, 198, 164);
            hair = new Color(195, 148, 52);
            bandBg = new Color(25, 65, 165);
            bandAccent = new Color(215, 40, 45);
        } else {
            skin = new Color(250, 214, 180);
            hair = new Color(22, 24, 32);
            bandBg = new Color(220, 40, 50);
            bandAccent = new Color(30, 80, 200);
        }

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

        g.setColor(skin);
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        g.setColor(new Color(40, 30, 25));
        g.setStroke(new BasicStroke(3.5f));
        g.drawOval(cx - r, cy - r, r * 2, r * 2);

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

        g.dispose();
        return img;
    }

    /** Draws a rotated soccer ball sprite centered at (x, y). */
    public static void drawRotatedBall(Graphics2D g, double x, double y, int radius, double angle) {
        BufferedImage img = getBallImage();
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(angle);
        g.drawImage(img, -radius, -radius, radius * 2, radius * 2, null);
        g.setTransform(old);
    }
}
