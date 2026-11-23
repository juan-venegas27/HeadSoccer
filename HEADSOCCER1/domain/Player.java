package domain;

import java.awt.Color;

/**
 * Represents a player in the Head Soccer game.
 * Manages movement, jumping, gravity, kicking, and field boundaries.
 */
public class Player {

    /** Radius of the player's head circle in pixels. */
    public static final int HEAD_RADIUS = 28;

    /** Radius of the single foot circle in pixels. */
    public static final int FOOT_RADIUS = 11;

    private static final double GRAVITY    = 0.55;
    private static final double JUMP_FORCE = -13.5;
    private static final double MOVE_SPEED = 5.0;

    private double positionX;
    private double positionY;
    private double velocityY;

    private final double initialX;
    private final double initialY;

    private final Color  color;
    private final String name;
    private final Power  power;

    private boolean onGround;

    private boolean movingLeft;
    private boolean movingRight;
    private boolean wantsToJump;

    /**
     * Creates a Player with the specified position, color, name, and power.
     *
     * @param x     initial centre X
     * @param y     initial centre Y (centre of head)
     * @param color player color
     * @param name  player name
     * @param power the special power assigned to this player
     */
    public Player(double x, double y, Color color, String name, Power power) {
        this.positionX = x;
        this.positionY = y;
        this.initialX  = x;
        this.initialY  = y;
        this.color     = color;
        this.name      = name;
        this.power     = power;
        this.velocityY = 0;
        this.onGround  = true;
    }

    /**
     * Updates the player's physics and enforces lateral and ground limits.
     *
     * @param minX    minimum allowed X coordinate for player centre
     * @param maxX    maximum allowed X coordinate for player centre
     * @param groundY Y coordinate of the ground surface
     */
    public void update(int minX, int maxX, int groundY) {
        if (movingLeft && !movingRight)  positionX -= MOVE_SPEED;
        if (movingRight && !movingLeft) positionX += MOVE_SPEED;

        if (wantsToJump && onGround) {
            velocityY = JUMP_FORCE;
            onGround  = false;
        }
        wantsToJump = false;

        velocityY += GRAVITY;
        positionY += velocityY;

        double floorY = groundY - HEAD_RADIUS;
        if (positionY >= floorY) {
            positionY = floorY;
            velocityY = 0;
            onGround  = true;
        } else {
            onGround = false;
        }

        if (positionX < minX) positionX = minX;
        if (positionX > maxX) positionX = maxX;
    }

    /**
     * Backward-compatible update method.
     *
     * @param fieldWidth width of the playing field
     * @param groundY    Y coordinate of the ground surface
     */
    public void update(int fieldWidth, int groundY) {
        update(HEAD_RADIUS, fieldWidth - HEAD_RADIUS, groundY);
    }

    /**
     * Schedules a jump action on the next update.
     */
    public void jump() {
        wantsToJump = true;
    }

    /**
     * Sets whether the left-movement key is active.
     *
     * @param moving true to move left
     */
    public void setMovingLeft(boolean moving) {
        this.movingLeft = moving;
    }

    /**
     * Sets whether the right-movement key is active.
     *
     * @param moving true to move right
     */
    public void setMovingRight(boolean moving) {
        this.movingRight = moving;
    }

    /**
     * Performs a hit/kick on the ball if within reach.
     * Gives the ball a forward and upward trajectory.
     *
     * @param ball the ball to hit
     */
    public void hit(Ball ball) {
        double dx = ball.getPositionX() - positionX;
        double dy = ball.getPositionY() - positionY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        double range = HEAD_RADIUS + Ball.RADIUS + 25;

        if (dist < range && dist > 0) {
            double nx = dx / dist;
            double ny = dy / dist;
            double kickForceX = nx * 14.0;
            double kickForceY = ny * 14.0 - 5.0;
            ball.setVelocity(kickForceX, kickForceY);
        }
    }

    /**
     * Activates this player's special power.
     */
    public void activatePower() {
        power.activate();
    }

    /**
     * Resets the player to initial position and clears velocity and movement flags.
     */
    public void reset() {
        positionX   = initialX;
        positionY   = initialY;
        velocityY   = 0;
        onGround    = true;
        movingLeft  = false;
        movingRight = false;
        wantsToJump = false;
    }

    /**
     * Directly adjusts the player's X position (used for player-player separation).
     *
     * @param x new X position
     */
    public void setPositionX(double x) {
        this.positionX = x;
    }

    /** @return current centre X */
    public double getPositionX() { return positionX; }

    /** @return current centre Y (of the head) */
    public double getPositionY() { return positionY; }

    /** @return centre X of the single foot (centered with head) */
    public double getFootX() { return positionX; }

    /** @return centre Y of the single foot */
    public double getFootY() { return positionY + HEAD_RADIUS; }

    /** @return current effective horizontal velocity */
    public double getVelocityX() {
        if (movingLeft && !movingRight) return -MOVE_SPEED;
        if (movingRight && !movingLeft) return MOVE_SPEED;
        return 0;
    }

    /** @return current vertical velocity */
    public double getVelocityY() { return velocityY; }

    /** @return true if currently standing on the ground */
    public boolean isOnGround() { return onGround; }

    /** @return player's display color */
    public Color getColor() { return color; }

    /** @return player's name */
    public String getName() { return name; }

    /** @return player's assigned power */
    public Power getPower() { return power; }
}
