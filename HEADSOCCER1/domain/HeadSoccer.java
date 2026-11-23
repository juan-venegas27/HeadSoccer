package domain;

import java.awt.Color;

/**
 * Central game model for Head Soccer.
 * Coordinates players, ball, field, scoreboard, clock, and game rules.
 */
public class HeadSoccer {

    /** Total match duration in seconds (3 minutes = 180 seconds). */
    public static final int MATCH_DURATION_SECONDS = 180;

    /** Number of physics ticks to freeze action after a goal (~1.2 seconds at 60 FPS). */
    private static final int GOAL_FREEZE_TICKS = 75;

    private final Player     player1;
    private final Player     player2;
    private final Ball       ball;
    private final Field      field;
    private final Scoreboard scoreboard;

    private int     remainingTime;
    private boolean running;
    private boolean paused;
    private int     goalFreezeTicks;
    private String  lastGoalScorer;

    /**
     * Creates and initialises a new HeadSoccer game.
     *
     * @param fieldWidth  width of the playing area in pixels
     * @param fieldHeight height of the playing area in pixels
     */
    public HeadSoccer(int fieldWidth, int fieldHeight) {
        this.field = new Field(fieldWidth, fieldHeight);

        int groundY = field.getGroundY();
        int leftGoalLine = field.getLeftGoalLine();
        int rightGoalLine = field.getRightGoalLine();

        Power power1 = new Power("Boost");
        Power power2 = new Power("Shield");

        double p1x = leftGoalLine + 140.0;
        double p2x = rightGoalLine - 140.0;
        double p1y = groundY - Player.HEAD_RADIUS;
        double p2y = groundY - Player.HEAD_RADIUS;

        this.player1 = new Player(p1x, p1y, Color.BLUE, "Player 1", power1);
        this.player2 = new Player(p2x, p2y, Color.RED,  "Player 2", power2);

        this.ball = new Ball(fieldWidth / 2.0, groundY - Ball.RADIUS * 4);

        this.scoreboard      = new Scoreboard();
        this.remainingTime   = MATCH_DURATION_SECONDS;
        this.running         = false;
        this.paused          = false;
        this.goalFreezeTicks = 0;
        this.lastGoalScorer  = null;
    }

    /**
     * Starts the game loop.
     */
    public void start() {
        running = true;
        paused  = false;
    }

    /**
     * Pauses or unpauses the game.
     *
     * @param pause true to pause, false to resume
     */
    public void setPaused(boolean pause) {
        this.paused = pause;
    }

    /**
     * Advances game physics by one tick.
     * Updates players, ball, collisions, and checks for goals.
     */
    public void update() {
        if (!running || paused) return;

        if (goalFreezeTicks > 0) {
            goalFreezeTicks--;
            if (goalFreezeTicks == 0) {
                resetAfterGoal();
            }
            return;
        }

        int leftGoalLine  = field.getLeftGoalLine();
        int rightGoalLine = field.getRightGoalLine();

        player1.update(leftGoalLine + Player.HEAD_RADIUS, rightGoalLine - Player.HEAD_RADIUS, field.getGroundY());
        player2.update(leftGoalLine + Player.HEAD_RADIUS, rightGoalLine - Player.HEAD_RADIUS, field.getGroundY());

        resolvePlayerPlayerCollision();

        ball.move(field);

        resolvePlayerBallCollision(player1);
        resolvePlayerBallCollision(player2);

        checkGoals();
    }

    /**
     * Decrements the remaining match time by one second.
     * Automatically finishes the game when time expires.
     */
    public void tickSecond() {
        if (!running || paused) return;
        if (remainingTime > 0) {
            remainingTime--;
        }
        if (remainingTime == 0) {
            finish();
        }
    }

    /**
     * Prevents the two players from walking through each other horizontally.
     */
    private void resolvePlayerPlayerCollision() {
        double minDist = Player.HEAD_RADIUS * 2;
        double dx = player2.getPositionX() - player1.getPositionX();
        if (dx < minDist && dx > 0) {
            double overlap = minDist - dx;
            player1.setPositionX(player1.getPositionX() - overlap / 2.0);
            player2.setPositionX(player2.getPositionX() + overlap / 2.0);
        }
    }

    /**
     * Resolves physical collision between the ball and a player (head and single foot).
     * Applies immediate positional separation and velocity response.
     *
     * @param player the player to test against the ball
     */
    private void resolvePlayerBallCollision(Player player) {
        double bx = ball.getPositionX();
        double by = ball.getPositionY();
        double px = player.getPositionX();
        double py = player.getPositionY();

        double headDistSq = (bx - px) * (bx - px) + (by - py) * (by - py);

        double fx = player.getFootX();
        double fy = player.getFootY();
        double footDistSq = (bx - fx) * (bx - fx) + (by - fy) * (by - fy);

        double cx = px;
        double cy = py;
        double cr = Player.HEAD_RADIUS;
        double closestDistSq = headDistSq;

        if (footDistSq < closestDistSq) {
            cx = fx;
            cy = fy;
            cr = Player.FOOT_RADIUS;
            closestDistSq = footDistSq;
        }

        double minDist = cr + Ball.RADIUS;
        if (closestDistSq < minDist * minDist && closestDistSq > 0.0001) {
            double dist = Math.sqrt(closestDistSq);
            double nx = (bx - cx) / dist;
            double ny = (by - cy) / dist;
            double overlap = minDist - dist;

            ball.setPosition(bx + nx * overlap, by + ny * overlap);

            double pvx = player.getVelocityX();
            double pvy = player.getVelocityY();

            double relVx = ball.getVelocityX() - pvx;
            double relVy = ball.getVelocityY() - pvy;

            double velAlongNormal = relVx * nx + relVy * ny;

            if (velAlongNormal < 0) {
                double restitution = 0.8;
                double impulseMag = -(1.0 + restitution) * velAlongNormal;
                double newVx = ball.getVelocityX() + impulseMag * nx;
                double newVy = ball.getVelocityY() + impulseMag * ny;

                if (ny < -0.3 && newVy > -4.0) {
                    newVy = -4.5;
                }

                ball.setVelocity(newVx, newVy);
            } else if (ny < -0.5 && ball.getVelocityY() > -2.0) {
                ball.setVelocity(ball.getVelocityX(), -3.0);
            }
        }
    }

    /**
     * Checks whether the ball has entered either goal and updates the scoreboard.
     */
    private void checkGoals() {
        if (goalFreezeTicks > 0) return;

        if (field.getLeftGoal().containsBall(ball)) {
            scoreboard.scoreGoalPlayer2();
            goalFreezeTicks = GOAL_FREEZE_TICKS;
            lastGoalScorer  = player2.getName();
        } else if (field.getRightGoal().containsBall(ball)) {
            scoreboard.scoreGoalPlayer1();
            goalFreezeTicks = GOAL_FREEZE_TICKS;
            lastGoalScorer  = player1.getName();
        }
    }

    /**
     * Resets players and ball to starting positions after a goal celebration.
     */
    private void resetAfterGoal() {
        player1.reset();
        player2.reset();
        ball.reset();
        lastGoalScorer = null;
    }

    /**
     * Finishes the game.
     */
    public void finish() {
        running = false;
    }

    /** @return true if the game is currently running */
    public boolean isRunning() { return running; }

    /** @return true if the game is currently paused */
    public boolean isPaused() { return paused; }

    /** @return true if currently in the post-goal freeze celebration */
    public boolean isGoalFreeze() { return goalFreezeTicks > 0; }

    /** @return name of player who scored the latest goal, or null */
    public String getLastGoalScorer() { return lastGoalScorer; }

    /** @return player 1 */
    public Player getPlayer1() { return player1; }

    /** @return player 2 */
    public Player getPlayer2() { return player2; }

    /** @return the ball */
    public Ball getBall() { return ball; }

    /** @return the field */
    public Field getField() { return field; }

    /** @return the scoreboard */
    public Scoreboard getScoreboard() { return scoreboard; }

    /** @return remaining time in seconds */
    public int getRemainingTime() { return remainingTime; }
}
