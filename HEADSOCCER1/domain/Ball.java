package domain;

/**
 * Represents the ball used in the Head Soccer game.
 * Handles velocity, gravity, friction, and collisions with boundaries and crossbars.
 */
public class Ball {

    /** Radius of the ball in pixels. */
    public static final int RADIUS = 20;

    private static final double GRAVITY = 0.5;
    private static final double BOUNCE  = 0.7;
    private static final double FRICTION = 0.98;
    private static final double MAX_SPEED = 16.0;

    private double positionX;
    private double positionY;
    private double velocityX;
    private double velocityY;

    private final double initialX;
    private final double initialY;

    /**
     * Creates a Ball at the specified initial position with zero velocity.
     *
     * @param x initial X position (centre of ball)
     * @param y initial Y position (centre of ball)
     */
    public Ball(double x, double y) {
        this.positionX = x;
        this.positionY = y;
        this.initialX  = x;
        this.initialY  = y;
        this.velocityX = 0;
        this.velocityY = 0;
    }

    /**
     * Applies an impulse to the ball's current velocity.
     *
     * @param impulseX horizontal impulse
     * @param impulseY vertical impulse
     */
    public void applyImpulse(double impulseX, double impulseY) {
        this.velocityX += impulseX;
        this.velocityY += impulseY;
        capSpeed();
    }

    /**
     * Sets the position of the ball directly.
     * Used for collision separation to prevent penetration into players or objects.
     *
     * @param x new X position
     * @param y new Y position
     */
    public void setPosition(double x, double y) {
        this.positionX = x;
        this.positionY = y;
    }

    /**
     * Sets the velocity of the ball directly.
     *
     * @param vx horizontal velocity
     * @param vy vertical velocity
     */
    public void setVelocity(double vx, double vy) {
        this.velocityX = vx;
        this.velocityY = vy;
        capSpeed();
    }

    /**
     * Advances the ball physics by one tick and resolves basic boundary collisions.
     *
     * @param field the playing field
     */
    public void move(Field field) {
        velocityY += GRAVITY;
        velocityX *= FRICTION;

        positionX += velocityX;
        positionY += velocityY;

        int width = field.getWidth();
        int groundY = field.getGroundY();
        int goalTop = field.getGoalTop();
        int leftGoalLine = field.getLeftGoalLine();
        int rightGoalLine = field.getRightGoalLine();

        if (positionY - RADIUS < 0) {
            positionY = RADIUS;
            velocityY = Math.abs(velocityY) * BOUNCE;
        }

        if (positionY + RADIUS > groundY) {
            positionY = groundY - RADIUS;
            velocityY = -Math.abs(velocityY) * BOUNCE;
            if (Math.abs(velocityY) < 1.0) {
                velocityY = 0;
            }
        }

        if (positionX - RADIUS < 0) {
            positionX = RADIUS;
            velocityX = Math.abs(velocityX) * BOUNCE;
        }

        if (positionX <= leftGoalLine) {
            if (positionY + RADIUS >= goalTop && positionY - RADIUS < goalTop) {
                if (velocityY > 0) {
                    positionY = goalTop - RADIUS;
                    velocityY = -Math.abs(velocityY) * BOUNCE;
                }
            }
        } else {
            if (positionY < goalTop && positionX - RADIUS < leftGoalLine) {
                positionX = leftGoalLine + RADIUS;
                velocityX = Math.abs(velocityX) * BOUNCE;
            }
        }

        if (positionX + RADIUS > width) {
            positionX = width - RADIUS;
            velocityX = -Math.abs(velocityX) * BOUNCE;
        }

        if (positionX >= rightGoalLine) {
            if (positionY + RADIUS >= goalTop && positionY - RADIUS < goalTop) {
                if (velocityY > 0) {
                    positionY = goalTop - RADIUS;
                    velocityY = -Math.abs(velocityY) * BOUNCE;
                }
            }
        } else {
            if (positionY < goalTop && positionX + RADIUS > rightGoalLine) {
                positionX = rightGoalLine - RADIUS;
                velocityX = -Math.abs(velocityX) * BOUNCE;
            }
        }

        capSpeed();
    }

    /**
     * Advances the ball without boundary checks (backward-compatible).
     */
    public void move() {
        velocityY += GRAVITY;
        velocityX *= FRICTION;
        positionX += velocityX;
        positionY += velocityY;
        capSpeed();
    }

    /**
     * Clamps velocity to MAX_SPEED to avoid runaway movement.
     */
    private void capSpeed() {
        if (velocityX >  MAX_SPEED) velocityX =  MAX_SPEED;
        if (velocityX < -MAX_SPEED) velocityX = -MAX_SPEED;
        if (velocityY >  MAX_SPEED) velocityY =  MAX_SPEED;
        if (velocityY < -MAX_SPEED) velocityY = -MAX_SPEED;
    }

    /**
     * Resets the ball to its initial position with zero velocity.
     */
    public void reset() {
        positionX = initialX;
        positionY = initialY;
        velocityX = 0;
        velocityY = 0;
    }

    /** @return current X position of the ball's centre */
    public double getPositionX() { return positionX; }

    /** @return current Y position of the ball's centre */
    public double getPositionY() { return positionY; }

    /** @return current horizontal velocity */
    public double getVelocityX() { return velocityX; }

    /** @return current vertical velocity */
    public double getVelocityY() { return velocityY; }
}
