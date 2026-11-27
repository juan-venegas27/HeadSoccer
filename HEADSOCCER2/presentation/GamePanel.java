package presentation;

import controller.GameController;
import domain.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;

/** Panel that renders the match and delegates player input to the controller. */
public class GamePanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private HeadSoccerGUI gui;
    private GameController controller;
    private HeadSoccer model;

    private Timer gameTimer;
    private Timer secondTimer;

    private PauseMenu pauseMenu;
    private boolean paused;

    /**
     * Creates the game panel for the given window and match controller.
     * @param gui main application frame
     * @param controller controller coordinating the match
     */
    public GamePanel(HeadSoccerGUI gui, GameController controller) {
        this.gui = gui;
        this.controller = controller;
        this.model = controller.getModel();
        this.paused = false;

        setPreferredSize(new Dimension(model.getField().getWidth(),
                                       model.getField().getHeight()));
        setFocusable(true);

        gameTimer = new Timer(1000 / 60, this);
        secondTimer = new Timer(1000, e -> {
            if (!paused && controller.isMatchRunning()) {
                controller.tickSecond();
                if (!controller.isMatchRunning()) {
                    handleMatchEnd();
                }
            }
        });

        pauseMenu = new PauseMenu(gui, this);
        pauseMenu.setVisible(false);
        setLayout(null);
        add(pauseMenu);

        addComponentListener(new ComponentAdapter() {
            /**
             * Keeps the pause overlay sized to the panel.
             * @param e component resize event
             */
            @Override
            public void componentResized(ComponentEvent e) {
                pauseMenu.setBounds(0, 0, getWidth(), getHeight());
            }
        });

        setupKeyBindings();
    }

    /**
     * Configures keyboard input bindings for Player 1, Player 2, and pause controls.
     */
    private void setupKeyBindings() {
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        bindKey(im, am, KeyEvent.VK_A, false, "p1_left_on",
                e -> controller.setPlayer1MovingLeft(true));
        bindKey(im, am, KeyEvent.VK_A, true, "p1_left_off",
                e -> controller.setPlayer1MovingLeft(false));
        bindKey(im, am, KeyEvent.VK_D, false, "p1_right_on",
                e -> controller.setPlayer1MovingRight(true));
        bindKey(im, am, KeyEvent.VK_D, true, "p1_right_off",
                e -> controller.setPlayer1MovingRight(false));
        bindKey(im, am, KeyEvent.VK_W, false, "p1_jump",
                e -> controller.player1Jump());
        bindKey(im, am, KeyEvent.VK_S, false, "p1_hit",
                e -> controller.player1Hit());
        bindKey(im, am, KeyEvent.VK_E, false, "p1_power_e",
                e -> controller.player1ActivatePower());
        bindKey(im, am, KeyEvent.VK_SPACE, false, "p1_power_space",
                e -> controller.player1ActivatePower());

        if (!controller.isPlayer2Machine()) {
            bindKey(im, am, KeyEvent.VK_LEFT, false, "p2_left_on",
                    e -> controller.setPlayer2MovingLeft(true));
            bindKey(im, am, KeyEvent.VK_LEFT, true, "p2_left_off",
                    e -> controller.setPlayer2MovingLeft(false));
            bindKey(im, am, KeyEvent.VK_RIGHT, false, "p2_right_on",
                    e -> controller.setPlayer2MovingRight(true));
            bindKey(im, am, KeyEvent.VK_RIGHT, true, "p2_right_off",
                    e -> controller.setPlayer2MovingRight(false));
            bindKey(im, am, KeyEvent.VK_UP, false, "p2_jump",
                    e -> controller.player2Jump());
            bindKey(im, am, KeyEvent.VK_DOWN, false, "p2_hit",
                    e -> controller.player2Hit());
            bindKey(im, am, KeyEvent.VK_ENTER, false, "p2_power_enter",
                    e -> controller.player2ActivatePower());
            bindKey(im, am, KeyEvent.VK_L, false, "p2_power_l",
                    e -> controller.player2ActivatePower());
        }

        bindKey(im, am, KeyEvent.VK_ESCAPE, false, "pause_toggle",
                e -> togglePause());
    }

    /**
     * Registers a key press or release binding that ignores gameplay input while paused.
     * @param im panel input map
     * @param am panel action map
     * @param keyCode KeyEvent virtual key code
     * @param onRelease true to trigger on key release, false on key press
     * @param name identifying name
     * @param action action invoked when the key binding fires
     */
    private void bindKey(InputMap im, ActionMap am, int keyCode,
                         boolean onRelease, String name, ActionListener action) {
        KeyStroke ks = onRelease
                ? KeyStroke.getKeyStroke(keyCode, 0, true)
                : KeyStroke.getKeyStroke(keyCode, 0, false);
        im.put(ks, name);
        am.put(name, new AbstractAction() {
            /**
             * Invokes the mapped action unless gameplay is paused.
             * @param e action event from timer or control
             */
            @Override
            public void actionPerformed(ActionEvent e) {
                if (paused && keyCode != KeyEvent.VK_ESCAPE) {
                    return;
                }
                action.actionPerformed(e);
            }
        });
    }

    /**
     * Starts the game loop timers and the match model.
     */
    public void startGame() {
        controller.startMatch();
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

    /** Stops timers and navigates to the end-game panel. */
    private void handleMatchEnd() {
        stopGame();
        gui.openEndGame();
    }

    /**
     * Toggles the paused state of the match.
     */
    public void togglePause() {
        paused = !paused;
        controller.setPaused(paused);
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

    /**
     * Resumes the match from the pause menu.
     */
    public void resumeGame() {
        paused = false;
        controller.setPaused(false);
        secondTimer.start();
        pauseMenu.setVisible(false);
        requestFocusInWindow();
        repaint();
    }

    /**
     * Advances simulation physics on each frame tick and repaints the field.
     * @param e action event from timer or control
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        controller.updateMatch();
        if (!controller.isMatchRunning() && controller.isMatchFinished()) {
            handleMatchEnd();
            return;
        }
        repaint();
    }

    /**
     * Renders the stadium, goals, players, ball, and HUD overlay.
     * @param g graphics context used for painting
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        Field field = model.getField();
        drawField(g2, field);
        drawGoals(g2, field);
        drawFieldElements(g2);
        drawPlayer(g2, model.getPlayer1());
        drawPlayer(g2, model.getPlayer2());
        drawBall(g2, model.getBall());
        drawHUD(g2, field);

        if (paused) {
            pauseMenu.setBounds(0, 0, getWidth(), getHeight());
        }
    }

    /**
     * Draws the stadium and grass field background image.
     * @param g 2D graphics context used for drawing
     * @param field playing field boundaries
     */
    private void drawField(Graphics2D g, Field field) {
        int width = field.getWidth();
        int height = field.getHeight();
        int groundY = field.getGroundY();

        BufferedImage stadium = AssetManager.getStadiumImage();
        int imgW = stadium.getWidth();
        int imgH = stadium.getHeight();
        int splitY = (int) Math.round(imgH * AssetManager.STADIUM_GRASS_RATIO);

        g.drawImage(stadium, 0, 0, width, groundY, 0, 0, imgW, splitY, null);
        g.drawImage(stadium, 0, groundY, width, height, 0, splitY, imgW, imgH, null);
    }

    /**
     * Draws both goal frames and nets.
     * @param g 2D graphics context used for drawing
     * @param field playing field boundaries
     */
    private void drawGoals(Graphics2D g, Field field) {
        int groundY = field.getGroundY();
        drawSingleGoal(g, field.getLeftGoal(), groundY, true);
        drawSingleGoal(g, field.getRightGoal(), groundY, false);
    }

    /**
     * Draws a single goal post structure and mesh.
     * @param g 2D graphics context used for drawing
     * @param goal goal instance to draw
     * @param groundY vertical coordinate of the ground surface
     * @param isLeft true for the left goal, false for the right goal
     */
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

    /**
     * Draws all active custom elements registered on the match arena.
     * @param g 2D graphics context used for drawing
     */
    private void drawFieldElements(Graphics2D g) {
        for (FieldElement element : model.getFieldElements()) {
            if (!element.isActive()) {
                continue;
            }
            int cx = (int) Math.round(element.getPositionX());
            int cy = (int) Math.round(element.getPositionY());
            int r = element.getRadius();
            Color fill = (element.getDisplayColor() != null) ? element.getDisplayColor() : Color.ORANGE;
            g.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 190));
            g.fillOval(cx - r, cy - r, r * 2, r * 2);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2f));
            g.drawOval(cx - r, cy - r, r * 2, r * 2);
            String label = element.getName();
            if (label != null && !label.trim().isEmpty()) {
                g.setFont(new Font("Arial", Font.BOLD, 10));
                FontMetrics fm = g.getFontMetrics();
                int tw = fm.stringWidth(label);
                g.drawString(label, cx - tw / 2, cy + 4);
            }
        }
        g.setStroke(new BasicStroke(1));
    }

    /**
     * Draws a player's head sprite, kicking foot, aura, and active status indicator.
     * @param g 2D graphics context used for drawing
     * @param player player instance to draw or update
     */
    private void drawPlayer(Graphics2D g, Player player) {
        int cx = (int) player.getPositionX();
        int cy = (int) player.getPositionY();
        int hr = Player.HEAD_RADIUS;
        int fr = Player.FOOT_RADIUS;

        if (player.getPower().isActive()) {
            Color auraColor = getAuraColor(player.getCountry());
            for (int i = 3; i >= 1; i--) {
                g.setColor(new Color(auraColor.getRed(), auraColor.getGreen(), auraColor.getBlue(), 55 * (4 - i)));
                int pad = i * 6;
                g.fillOval(cx - hr - pad, cy - hr - pad, (hr + pad) * 2, (hr + pad) * 2);
            }
        }

        BufferedImage headImg = AssetManager.getCharacterHead(player.getCountry());
        int headSize = (hr + 4) * 2;
        if (player == model.getPlayer2()) {
            g.drawImage(headImg, cx + hr + 4, cy - hr - 6, -headSize, headSize, null);
        } else {
            g.drawImage(headImg, cx - hr - 4, cy - hr - 6, headSize, headSize, null);
        }

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

        drawPlayerStatusEffect(g, player, cx, cy, hr);
    }

    /**
     * Draws stun or power effect indicators above the given player.
     * @param g 2D graphics context used for drawing
     * @param player player instance to draw or update
     * @param cx center horizontal coordinate
     * @param cy center vertical coordinate
     * @param hr player head radius in pixels
     */
    private void drawPlayerStatusEffect(Graphics2D g, Player player, int cx, int cy, int hr) {
        StatusEffect effect = player.getActiveStatusEffect();
        if (player.isStunned()) {
            drawFloatingStatus(g, cx, cy - hr - 16, "★ ATURDIDO ★", Color.YELLOW);
        } else if (effect != null) {
            if (effect.showsSparks()) {
                g.setColor(new Color(255, 255, 80, 180));
                g.setStroke(new BasicStroke(2.5f));
                g.drawLine(cx - hr, cy - 10, cx - hr + 8, cy);
                g.drawLine(cx + hr, cy - 8, cx + hr - 8, cy + 5);
            }
            Color badgeColor = (effect.getBadgeColor() != null) ? effect.getBadgeColor() : Color.CYAN;
            drawFloatingStatus(g, cx, cy - hr - 16, effect.getName().toUpperCase(), badgeColor);
        }
    }

    /**
     * Draws a pill-shaped status badge above a player's head.
     * @param g 2D graphics context used for drawing
     * @param cx center horizontal coordinate
     * @param y vertical coordinate
     * @param text text to display
     * @param color player kit color
     */
    private void drawFloatingStatus(Graphics2D g, int cx, int y, String text, Color color) {
        g.setFont(new Font("Arial", Font.BOLD, 11));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(text);
        g.setColor(new Color(0, 0, 0, 175));
        g.fillRoundRect(cx - w / 2 - 5, y - 12, w + 10, 16, 8, 8);
        g.setColor(color);
        g.drawString(text, cx - w / 2, y);
    }

    /**
     * Returns the visual aura color associated with the given country.
     * @param country selected country name
     * @return visual aura color for the country
     */
    private Color getAuraColor(String country) {
        return CharacterRegistry.get(country).getAuraColor();
    }

    /**
     * Draws the soccer ball and any identical Illusion decoy balls.
     * @param g 2D graphics context used for drawing
     * @param ball active match ball
     */
    private void drawBall(Graphics2D g, Ball ball) {
        int br = Ball.RADIUS;
        double bx = ball.getPositionX();
        double by = ball.getPositionY();
        List<double[]> decoys = ball.getDecoyBalls();

        if (ball.hasActivePowerShot() && decoys.isEmpty()) {
            Color trail = getAuraColor(ball.getActivePowerShot().getCountry());
            g.setColor(new Color(trail.getRed(), trail.getGreen(), trail.getBlue(), 110));
            g.fillOval((int) bx - br - 7, (int) by - br - 7, (br + 7) * 2, (br + 7) * 2);
        }

        AssetManager.drawBall(g, bx, by, br);

        for (double[] d : decoys) {
            AssetManager.drawBall(g, d[0], d[1], br);
        }
    }

    /**
     * Draws the top scoreboard bar, health/power bars, timer, and goal banner.
     * @param g 2D graphics context used for drawing
     * @param field playing field boundaries
     */
    private void drawHUD(Graphics2D g, Field field) {
        int width = field.getWidth();
        Player p1 = model.getPlayer1();
        Player p2 = model.getPlayer2();

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

        drawScoreAndClock(g, width / 2, model.getScoreboard());
        if (model.isGoalFreeze()) {
            drawGoalBanner(g, width);
        }
    }

    /**
     * Draws the center scoreboard digits and clock or victory target label.
     * @param g 2D graphics context used for drawing
     * @param centerX horizontal center coordinate of the scoreboard
     * @param sb match scoreboard
     */
    private void drawScoreAndClock(Graphics2D g, int centerX, Scoreboard sb) {
        String scoreText = sb.getScorePlayer1() + "  -  " + sb.getScorePlayer2();
        g.setFont(new Font("Arial", Font.BOLD, 28));
        FontMetrics sfm = g.getFontMetrics();
        g.setColor(Color.WHITE);
        g.drawString(scoreText, centerX - sfm.stringWidth(scoreText) / 2, 32);

        String clockText = model.getMatchRule().getHudStatusText(model);
        Color clockColor = model.isSuddenDeath()
                ? new Color(255, 90, 90)
                : new Color(255, 215, 40);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        FontMetrics cfm = g.getFontMetrics();
        g.setColor(clockColor);
        g.drawString(clockText, centerX - cfm.stringWidth(clockText) / 2, 54);
    }

    /**
     * Draws the center goal celebration banner during post-goal freeze.
     * @param g 2D graphics context used for drawing
     * @param width width in pixels
     */
    private void drawGoalBanner(Graphics2D g, int width) {
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

    /**
     * Draws health and power charge bars for one player.
     * @param g 2D graphics context used for drawing
     * @param x horizontal coordinate
     * @param y vertical coordinate
     * @param w width in pixels
     * @param p player instance
     */
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

    /** Returns the domain model associated with this panel. */
    public HeadSoccer getModel() {
        return model;
    }

    /** Returns preferred panel dimensions matching the configured field size. */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(model.getField().getWidth(),
                             model.getField().getHeight());
    }
}
