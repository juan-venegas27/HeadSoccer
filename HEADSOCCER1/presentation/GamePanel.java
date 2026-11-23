package presentation;

import domain.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * The main game rendering and input panel.
 * Handles the animation loop, match clock, keyboard controls,
 * drawing of the pitch, goals, players, ball, HUD, and pause overlay.
 */
public class GamePanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private final HeadSoccerGUI gui;
    private final HeadSoccer    model;

    private final Timer gameTimer;
    private final Timer secondTimer;

    private final PauseMenu pauseMenu;
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

    /**
     * Overloaded constructor for backwards compatibility.
     *
     * @param gui          the main application window
     * @param model        the game domain model
     * @param endGamePanel unused reference (maintained for compatibility)
     */
    public GamePanel(HeadSoccerGUI gui, HeadSoccer model, EndGamePanel endGamePanel) {
        this(gui, model);
    }

    /**
     * Configures keyboard inputs using Swing Key Bindings.
     */
    private void setupKeyBindings() {
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

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
                e -> model.getPlayer1().hit(model.getBall()));

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
                e -> model.getPlayer2().hit(model.getBall()));

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
                action.actionPerformed(e);
            }
        });
    }

    /**
     * Starts the game loop timers and the domain model.
     */
    public void startGame() {
        model.start();
        paused = false;
        pauseMenu.setVisible(false);
        gameTimer.start();
        secondTimer.start();
    }

    /**
     * Stops both timers to prevent background resource leaks.
     */
    public void stopGame() {
        gameTimer.stop();
        secondTimer.stop();
    }

    /**
     * Finishes the match when time expires and navigates to the results screen.
     */
    private void handleMatchEnd() {
        stopGame();
        gui.openEndGame();
    }

    /**
     * Toggles the paused state of the match.
     */
    public void togglePause() {
        paused = !paused;
        model.setPaused(paused);
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
        model.setPaused(false);
        secondTimer.start();
        pauseMenu.setVisible(false);
        requestFocusInWindow();
        repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        model.update();
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
     * Draws the sky, grass ground, and ground boundary line.
     */
    private void drawField(Graphics2D g, Field field) {
        int width   = field.getWidth();
        int height  = field.getHeight();
        int groundY = field.getGroundY();

        g.setColor(new Color(135, 206, 235));
        g.fillRect(0, 0, width, groundY);

        g.setColor(new Color(34, 139, 34));
        g.fillRect(0, groundY, width, height - groundY);

        g.setColor(new Color(0, 100, 0));
        g.setStroke(new BasicStroke(3));
        g.drawLine(0, groundY, width, groundY);
        g.setStroke(new BasicStroke(1));
    }

    /**
     * Draws both soccer goals with crossbars and posts.
     */
    private void drawGoals(Graphics2D g, Field field) {
        Goal left  = field.getLeftGoal();
        Goal right = field.getRightGoal();
        int groundY = field.getGroundY();

        int lx1 = left.getPositionX();
        int lx2 = left.getPositionX() + left.getWidth();
        int ly1 = left.getPositionY();
        int ly2 = groundY;

        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(4));
        g.drawLine(lx1, ly1, lx2, ly1);
        g.drawLine(lx2, ly1, lx2, ly2);
        g.drawLine(lx1, ly1, lx1, ly2);

        int rx1 = right.getPositionX();
        int rx2 = right.getPositionX() + right.getWidth();
        int ry1 = right.getPositionY();
        int ry2 = groundY;

        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(4));
        g.drawLine(rx1, ry1, rx2, ry1);
        g.drawLine(rx1, ry1, rx1, ry2);
        g.drawLine(rx2, ry1, rx2, ry2);

        g.setStroke(new BasicStroke(1));
    }

    /**
     * Draws a player: circular head and a single centered circular foot.
     */
    private void drawPlayer(Graphics2D g, Player player) {
        int cx = (int) player.getPositionX();
        int cy = (int) player.getPositionY();
        int hr = Player.HEAD_RADIUS;
        int fr = Player.FOOT_RADIUS;

        g.setColor(player.getColor());
        g.fillOval(cx - hr, cy - hr, hr * 2, hr * 2);
        g.setColor(player.getColor().darker());
        g.drawOval(cx - hr, cy - hr, hr * 2, hr * 2);

        g.setColor(player.getColor());
        g.fillOval(cx - fr, cy + hr - fr, fr * 2, fr * 2);
        g.setColor(player.getColor().darker());
        g.drawOval(cx - fr, cy + hr - fr, fr * 2, fr * 2);
    }

    /**
     * Draws the ball as a white circle with black border and center mark.
     */
    private void drawBall(Graphics2D g, Ball ball) {
        int bx = (int) ball.getPositionX();
        int by = (int) ball.getPositionY();
        int br = Ball.RADIUS;

        g.setColor(Color.WHITE);
        g.fillOval(bx - br, by - br, br * 2, br * 2);
        g.setColor(Color.BLACK);
        g.drawOval(bx - br, by - br, br * 2, br * 2);

        g.setColor(Color.BLACK);
        g.fillOval(bx - 5, by - 5, 10, 10);
    }

    /**
     * Draws the HUD divided into three independent non-overlapping zones:
     * Left (Player 1), Center (Clock and celebration), and Right (Player 2).
     */
    private void drawHUD(Graphics2D g, Field field) {
        int width = field.getWidth();

        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, 0, width, 55);

        Scoreboard sb = model.getScoreboard();

        g.setColor(new Color(65, 105, 225));
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString(model.getPlayer1().getName(), 25, 36);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 30));
        g.drawString(String.valueOf(sb.getScorePlayer1()), 230, 39);

        int remaining = model.getRemainingTime();
        int min = remaining / 60;
        int sec = remaining % 60;
        String timeStr = String.format("%02d:%02d", min, sec);

        g.setFont(new Font("Arial", Font.BOLD, 28));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(timeStr);
        int tx = (width - tw) / 2;

        g.setColor(Color.WHITE);
        g.drawString(timeStr, tx, 38);

        if (model.isGoalFreeze()) {
            String goalText = "¡GOL!";
            if (model.getLastGoalScorer() != null) {
                goalText = "¡GOL DE " + model.getLastGoalScorer().toUpperCase() + "!";
            }
            g.setFont(new Font("Arial", Font.BOLD, 32));
            FontMetrics gfm = g.getFontMetrics();
            int gtw = gfm.stringWidth(goalText);
            int gx = (width - gtw) / 2;

            g.setColor(new Color(0, 0, 0, 180));
            g.fillRoundRect(gx - 20, 90, gtw + 40, 50, 15, 15);
            g.setColor(Color.YELLOW);
            g.drawString(goalText, gx, 127);
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 30));
        g.drawString(String.valueOf(sb.getScorePlayer2()), 640, 39);

        g.setColor(new Color(255, 69, 0));
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString(model.getPlayer2().getName(), 730, 36);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(model.getField().getWidth(),
                             model.getField().getHeight());
    }
}
