package src.presentation;

import src.domain.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * The main game rendering and input panel for DOPOSoccer (Version 2).
 * Handles the 60 FPS animation loop, 1 Hz clock timer, keyboard controls
 * (including power activation keys), stadium/character/ball rendering,
 * power auras, status effect indicators, 8-ball Illusion visuals, and full HUD.
 */
public class GamePanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private HeadSoccerGUI gui;
    private HeadSoccer    model;

    private Timer gameTimer;
    private Timer secondTimer;

    private PauseMenu pauseMenu;
    private boolean paused;

    /**
     * Constructs the game panel with the specified model and navigation references.
     *
     * @param gui   the main application window
     * @param model the game domain model
     */
    public GamePanel(HeadSoccerGUI gui, HeadSoccer model) {
        this.gui    = gui;
        this.model  = model;
        this.paused = false;

        setPreferredSize(new Dimension(model.getField().getWidth(),
                                       model.getField().getHeight()));
        setFocusable(true);

        gameTimer   = new Timer(1000 / 60, this);
        secondTimer = new Timer(1000, e -> {
            if (!paused && model.isRunning()) {
                model.tickSecond();
                if (!model.isRunning()) {
                    handleMatchEnd();
                }
            }
        });

        pauseMenu = new PauseMenu(gui, this);
        pauseMenu.setVisible(false);
        setLayout(null);
        add(pauseMenu);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                pauseMenu.setBounds(0, 0, getWidth(), getHeight());
            }
        });

        setupKeyBindings();
    }

    private void setupKeyBindings() {
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        // Player 1 controls
        bindKey(im, am, KeyEvent.VK_A, false, "p1_left_on",
                e -> model.getPlayer1().setMovingLeft(true));
        bindKey(im, am, KeyEvent.VK_A, true, "p1_left_off",
                e -> model.getPlayer1().setMovingLeft(false));
        bindKey(im, am, KeyEvent.VK_D, false, "p1_right_on",
                e -> model.getPlayer1().setMovingRight(true));
        bindKey(im, am, KeyEvent.VK_D, true, "p1_right_off",
                e -> model.getPlayer1().setMovingRight(false));
        bindKey(im, am, KeyEvent.VK_W, false, "p1_jump",
                e -> model.getPlayer1().jump());
        bindKey(im, am, KeyEvent.VK_S, false, "p1_hit",
                e -> model.getPlayer1().hit(model.getBall(), model.getPlayer2()));
        bindKey(im, am, KeyEvent.VK_E, false, "p1_power_e",
                e -> model.getPlayer1().activatePower());
        bindKey(im, am, KeyEvent.VK_SPACE, false, "p1_power_space",
                e -> model.getPlayer1().activatePower());

        // Player 2 controls (only active in PvP; machine handles PvM)
        if (!(model.getPlayer2() instanceof MachinePlayer)) {
            bindKey(im, am, KeyEvent.VK_LEFT, false, "p2_left_on",
                    e -> model.getPlayer2().setMovingLeft(true));
            bindKey(im, am, KeyEvent.VK_LEFT, true, "p2_left_off",
                    e -> model.getPlayer2().setMovingLeft(false));
            bindKey(im, am, KeyEvent.VK_RIGHT, false, "p2_right_on",
                    e -> model.getPlayer2().setMovingRight(true));
            bindKey(im, am, KeyEvent.VK_RIGHT, true, "p2_right_off",
                    e -> model.getPlayer2().setMovingRight(false));
            bindKey(im, am, KeyEvent.VK_UP, false, "p2_jump",
                    e -> model.getPlayer2().jump());
            bindKey(im, am, KeyEvent.VK_DOWN, false, "p2_hit",
                    e -> model.getPlayer2().hit(model.getBall(), model.getPlayer1()));
            bindKey(im, am, KeyEvent.VK_ENTER, false, "p2_power_enter",
                    e -> model.getPlayer2().activatePower());
            bindKey(im, am, KeyEvent.VK_L, false, "p2_power_l",
                    e -> model.getPlayer2().activatePower());
        }

        bindKey(im, am, KeyEvent.VK_ESCAPE, false, "pause_toggle",
                e -> togglePause());
    }

    private void bindKey(InputMap im, ActionMap am, int keyCode,
                         boolean onRelease, String name, ActionListener action) {
        KeyStroke ks = onRelease
                ? KeyStroke.getKeyStroke(keyCode, 0, true)
                : KeyStroke.getKeyStroke(keyCode, 0, false);
        im.put(ks, name);
        am.put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (paused && keyCode != KeyEvent.VK_ESCAPE) {
                    return;
                }
                action.actionPerformed(e);
            }
        });
    }

    private void clearPlayerMovement() {
        model.getPlayer1().setMovingLeft(false);
        model.getPlayer1().setMovingRight(false);
        model.getPlayer2().setMovingLeft(false);
        model.getPlayer2().setMovingRight(false);
    }

    /** Starts the game loop timers and the domain model. */
    public void startGame() {
        model.start();
        paused = false;
        pauseMenu.setVisible(false);
        gameTimer.start();
        secondTimer.start();
    }

    /** Stops both timers to prevent background resource leaks. */
    public void stopGame() {
        gameTimer.stop();
        secondTimer.stop();
    }

    private void handleMatchEnd() {
        stopGame();
        gui.openEndGame();
    }

    /** Toggles the paused state of the match. */
    public void togglePause() {
        paused = !paused;
        model.setPaused(paused);
        clearPlayerMovement();
        if (paused) {
            secondTimer.stop();
        } else {
            secondTimer.start();
        }
        pauseMenu.setBounds(0, 0, getWidth(), getHeight());
        pauseMenu.setVisible(paused);
        if (!paused) requestFocusInWindow();
        repaint();
    }

    /** Resumes the match from the pause menu. */
    public void resumeGame() {
        paused = false;
        model.setPaused(false);
        clearPlayerMovement();
        secondTimer.start();
        pauseMenu.setVisible(false);
        requestFocusInWindow();
        repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        model.update();
        if (!model.isRunning() && model.isMatchFinished()) {
            handleMatchEnd();
            return;
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Field field = model.getField();
        drawField(g2, field);
        drawGoals(g2, field);
        drawPlayer(g2, model.getPlayer1());
        drawPlayer(g2, model.getPlayer2());
        drawBall(g2, model.getBall());
        drawHUD(g2, field);

        if (paused) {
            pauseMenu.setBounds(0, 0, getWidth(), getHeight());
        }
    }

    /**
     * Draws the stadium background and grass ground without center dashed lines or center circle.
     */
    private void drawField(Graphics2D g, Field field) {
        int width   = field.getWidth();
        int height  = field.getHeight();
        int groundY = field.getGroundY();

        BufferedImage stadium = AssetManager.getStadiumImage();
        g.drawImage(stadium, 0, 0, width, groundY, null);

        int stripeWidth = 60;
        for (int x = 0; x < width; x += stripeWidth) {
            boolean even = ((x / stripeWidth) % 2) == 0;
            g.setColor(even ? new Color(46, 160, 52) : new Color(36, 138, 42));
            g.fillRect(x, groundY, stripeWidth, height - groundY);
        }

        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3));
        g.drawLine(0, groundY, width, groundY);
        g.setStroke(new BasicStroke(1));
    }

    private void drawGoals(Graphics2D g, Field field) {
        int groundY = field.getGroundY();
        drawSingleGoal(g, field.getLeftGoal(), groundY, true);
        drawSingleGoal(g, field.getRightGoal(), groundY, false);
    }

    private void drawSingleGoal(Graphics2D g, Goal goal, int groundY, boolean isLeft) {
        int x1 = goal.getPositionX();
        int x2 = goal.getPositionX() + goal.getWidth();
        int y1 = goal.getPositionY();
        int y2 = groundY;

        g.setColor(new Color(255, 255, 255, 45));
        g.fillRect(x1, y1, goal.getWidth(), y2 - y1);

        g.setColor(new Color(255, 255, 255, 140));
        g.setStroke(new BasicStroke(1.2f));
        for (int x = x1 + 14; x < x2; x += 14) {
            g.drawLine(x, y1, x, y2);
        }
        for (int y = y1 + 18; y < y2; y += 18) {
            g.drawLine(x1, y, x2, y);
        }

        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(x1, y1, x2, y1);
        if (isLeft) {
            g.drawLine(x1, y1, x1, y2);
            g.fillOval(x2 - 4, y1 - 4, 8, 8);
        } else {
            g.drawLine(x2, y1, x2, y2);
            g.fillOval(x1 - 4, y1 - 4, 8, 8);
        }
        g.setStroke(new BasicStroke(1));
    }

    private void drawPlayer(Graphics2D g, Player player) {
        int cx = (int) player.getPositionX();
        int cy = (int) player.getPositionY();
        int hr = Player.HEAD_RADIUS;
        int fr = Player.FOOT_RADIUS;

        // Power active aura
        if (player.getPower().isActive()) {
            Color auraColor = getAuraColor(player.getCountry());
            for (int i = 3; i >= 1; i--) {
                g.setColor(new Color(auraColor.getRed(), auraColor.getGreen(), auraColor.getBlue(), 55 * (4 - i)));
                int pad = i * 6;
                g.fillOval(cx - hr - pad, cy - hr - pad, (hr + pad) * 2, (hr + pad) * 2);
            }
        }

        BufferedImage headImg = AssetManager.getCharacterHead(player.getCountry());
        AffineTransform old = g.getTransform();
        g.translate(cx, cy);
        if (player.getFacingDirection() < 0) {
            g.scale(-1, 1);
        }
        g.drawImage(headImg, -hr - 4, -hr - 6, (hr + 4) * 2, (hr + 4) * 2, null);
        g.setTransform(old);

        int fx = (int) player.getFootX();
        int fy = (int) player.getFootY();
        AffineTransform footOld = g.getTransform();
        g.translate(fx, fy);
        g.rotate(player.getKickAngle());
        g.setColor(player.getColor());
        g.fillRoundRect(-fr - 3, -fr + 2, fr * 2 + 8, fr * 2 - 3, 10, 10);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(-fr - 3, -fr + 2, fr * 2 + 8, fr * 2 - 3, 10, 10);
        g.setTransform(footOld);

        if (player.isStunned()) {
            drawFloatingStatus(g, cx, cy - hr - 16, "★ ATURDIDO ★", Color.YELLOW);
        } else if (player.getActiveEffect() != null) {
            Color effColor = Color.CYAN;
            if (Power.EFFECT_ELECTROCUTE.equals(player.getActiveEffect())) {
                effColor = new Color(255, 240, 50);
                g.setColor(new Color(255, 255, 80, 180));
                g.setStroke(new BasicStroke(2.5f));
                g.drawLine(cx - hr, cy - 10, cx - hr + 8, cy);
                g.drawLine(cx + hr, cy - 8, cx + hr - 8, cy + 5);
            } else if (Power.EFFECT_UNCONSCIOUS.equals(player.getActiveEffect())) {
                effColor = new Color(180, 240, 255);
            } else if (Power.EFFECT_CONFUSION.equals(player.getActiveEffect())) {
                effColor = new Color(255, 160, 255);
            }
            drawFloatingStatus(g, cx, cy - hr - 16, player.getActiveEffect().toUpperCase(), effColor);
        }
    }

    private void drawFloatingStatus(Graphics2D g, int cx, int y, String text, Color color) {
        g.setFont(new Font("Arial", Font.BOLD, 11));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(text);
        g.setColor(new Color(0, 0, 0, 175));
        g.fillRoundRect(cx - w / 2 - 5, y - 12, w + 10, 16, 8, 8);
        g.setColor(color);
        g.drawString(text, cx - w / 2, y);
    }

    private Color getAuraColor(String country) {
        if (country == null) return Color.CYAN;
        String lower = country.toLowerCase();
        if (lower.contains("camer")) return new Color(255, 230, 40);
        if (lower.contains("nigeria")) return new Color(60, 235, 120);
        if (lower.contains("estados") || lower.contains("usa")) return new Color(255, 90, 210);
        return new Color(40, 170, 255);
    }

    private void drawBall(Graphics2D g, Ball ball) {
        int br = Ball.RADIUS;
        double bx = ball.getPositionX();
        double by = ball.getPositionY();
        List<double[]> decoys = ball.getDecoyBalls();

        // Annex 1: When Illusion is active, all 8 balls look identical for both players
        if (ball.hasActivePowerShot() && decoys.isEmpty()) {
            Color trail = getAuraColor(ball.getActivePowerShot().getCountry());
            g.setColor(new Color(trail.getRed(), trail.getGreen(), trail.getBlue(), 110));
            g.fillOval((int) bx - br - 7, (int) by - br - 7, (br + 7) * 2, (br + 7) * 2);
        }

        AssetManager.drawRotatedBall(g, bx, by, br, ball.getRotationAngle());

        for (double[] d : decoys) {
            AssetManager.drawRotatedBall(g, d[0], d[1], br, ball.getRotationAngle());
        }
    }

    private void drawHUD(Graphics2D g, Field field) {
        int width = field.getWidth();
        Player p1 = model.getPlayer1();
        Player p2 = model.getPlayer2();
        Scoreboard sb = model.getScoreboard();

        g.setColor(new Color(8, 12, 30, 210));
        g.fillRect(0, 0, width, 62);
        g.setColor(new Color(255, 215, 40, 180));
        g.drawLine(0, 62, width, 62);

        int barWidth = Math.min(200, Math.max(150, width / 5));

        g.setColor(new Color(120, 175, 255));
        g.setFont(new Font("Arial", Font.BOLD, 13));
        g.drawString(p1.getName() + " [" + p1.getCountry() + "]", 14, 18);
        drawPlayerBars(g, 14, 24, barWidth, p1);

        int rightBarX = width - barWidth - 14;
        g.setColor(new Color(255, 130, 120));
        g.setFont(new Font("Arial", Font.BOLD, 13));
        String p2Title = p2.getName() + " [" + p2.getCountry() + "]";
        FontMetrics fmTitle = g.getFontMetrics();
        g.drawString(p2Title, width - fmTitle.stringWidth(p2Title) - 14, 18);
        drawPlayerBars(g, rightBarX, 24, barWidth, p2);

        int centerX = width / 2;
        String scoreText = sb.getScorePlayer1() + "  -  " + sb.getScorePlayer2();
        g.setFont(new Font("Arial", Font.BOLD, 28));
        FontMetrics sfm = g.getFontMetrics();
        g.setColor(Color.WHITE);
        g.drawString(scoreText, centerX - sfm.stringWidth(scoreText) / 2, 32);

        String clockText;
        Color clockColor = new Color(255, 215, 40);
        if (model.isSuddenDeath()) {
            clockText = "¡MUERTE SÚBITA!";
            clockColor = new Color(255, 90, 90);
        } else if (model.getMatchType() == HeadSoccer.MATCH_BY_GOALS) {
            clockText = "META: " + model.getTargetGoals() + " GOLES";
        } else {
            int rem = model.getRemainingTime();
            clockText = String.format("%02d:%02d", rem / 60, rem % 60);
        }
        g.setFont(new Font("Arial", Font.BOLD, 14));
        FontMetrics cfm = g.getFontMetrics();
        g.setColor(clockColor);
        g.drawString(clockText, centerX - cfm.stringWidth(clockText) / 2, 54);

        if (model.isGoalFreeze()) {
            String goalText = "¡GOL!";
            if (model.getLastGoalScorer() != null) {
                goalText = "¡GOL DE " + model.getLastGoalScorer().toUpperCase() + "!";
            }
            g.setFont(new Font("Arial", Font.BOLD, 30));
            FontMetrics gfm = g.getFontMetrics();
            int gtw = gfm.stringWidth(goalText);
            int gx = (width - gtw) / 2;

            g.setColor(new Color(0, 0, 0, 195));
            g.fillRoundRect(gx - 24, 88, gtw + 48, 52, 16, 16);
            g.setColor(Color.YELLOW);
            g.drawString(goalText, gx, 125);
        }
    }

    private void drawPlayerBars(Graphics2D g, int x, int y, int w, Player p) {
        double hpRatio = Math.max(0, Math.min(1.0, p.getCurrentHealth() / p.getMaxHealth()));
        g.setColor(new Color(60, 20, 20));
        g.fillRoundRect(x, y, w, 13, 6, 6);
        g.setColor(hpRatio > 0.35 ? new Color(60, 210, 80) : new Color(235, 65, 55));
        g.fillRoundRect(x, y, (int) (w * hpRatio), 13, 6, 6);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 10));
        g.drawString("HP " + (int) Math.ceil(p.getCurrentHealth()) + "/" + (int) p.getMaxHealth(), x + 6, y + 10);

        int py = y + 17;
        double pwrRatio = Math.max(0, Math.min(1.0, p.getPowerBar() / 100.0));
        g.setColor(new Color(20, 30, 55));
        g.fillRoundRect(x, py, w, 12, 6, 6);
        if (p.getPower().isActive()) {
            g.setColor(new Color(255, 190, 30));
            g.fillRoundRect(x, py, w, 12, 6, 6);
        } else {
            g.setColor(pwrRatio >= 1.0 ? new Color(0, 235, 255) : new Color(55, 145, 240));
            g.fillRoundRect(x, py, (int) (w * pwrRatio), 12, 6, 6);
        }
        g.setColor(Color.WHITE);
        String pwrText = p.getPower().isActive() ? "PODER LISTO (TOCA BALON)"
                : (pwrRatio >= 1.0 ? "PODER 100% ¡ACTIVAR!" : "PODER " + (int) p.getPowerBar() + "%");
        g.drawString(pwrText, x + 6, py + 10);
    }

    /** @return the domain model associated with this panel */
    public HeadSoccer getModel() {
        return model;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(model.getField().getWidth(),
                             model.getField().getHeight());
    }
}
