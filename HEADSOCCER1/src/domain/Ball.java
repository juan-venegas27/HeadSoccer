package src.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the ball used in the DOPOSoccer game.
 *
 * <p>Handles 2D arcade physics:
 * <ul>
 *   <li>Gravity, air/ground friction, and bounce restitution.</li>
 *   <li>Free airspace above the goal crossbars (no invisible wall above goals).</li>
 *   <li>Bounces on top/bottom surfaces and front corners of the goal crossbars.</li>
 *   <li>Power shot trajectories (Aura Azul, Rayo, Tornado, and 8-ball Ilusión).</li>
 * </ul>
 */
public class Ball implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Radius of the ball in pixels. */
    public static final int RADIUS = 20;

    private static final double GRAVITY         = 0.48;
    private static final double BOUNCE          = 0.74;
    private static final double AIR_FRICTION    = 0.992;
    private static final double GROUND_FRICTION = 0.965;
    private static final double MAX_SPEED       = 22.0;

    private double positionX;
    private double positionY;
    private double velocityX;
    private double velocityY;
    private double rotationAngle;

    private final double initialX;
    private final double initialY;

    // Power shot state
    private Power  activePowerShot;
    private Player powerShotOwner;
    private int    powerShotFlightTicks;
    private int    illusionTicks;
    private final List<double[]> decoyBalls;

    /**
     * Creates a Ball at the specified initial position with zero velocity.
     *
     * @param x initial X position (centre of ball)
     * @param y initial Y position (centre of ball)
     */
    public Ball(double x, double y) {
        this.positionX            = x;
        this.positionY            = y;
        this.initialX             = x;
        this.initialY             = y;
        this.velocityX            = 0;
        this.velocityY            = 0;
        this.rotationAngle        = 0;
        this.activePowerShot      = null;
        this.powerShotOwner       = null;
        this.powerShotFlightTicks = 0;
        this.illusionTicks        = 0;
        this.decoyBalls           = new ArrayList<>();
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
     * Launches the ball as an active power shot owned by a player.
     *
     * @param power     the power being executed
     * @param owner     the player launching the shot
     * @param direction +1 to shoot right, -1 to shoot left
     */
    public void launchPowerShot(Power power, Player owner, int direction) {
        if (power == null) return;
        this.activePowerShot      = power;
        this.powerShotOwner       = owner;
        this.powerShotFlightTicks = power.getEffectDurationTicks();

        double vx = power.getShotSpeedX() * direction;
        double vy = power.getShotSpeedY();
        setVelocity(vx, vy);

        this.decoyBalls.clear();
        this.illusionTicks = 0;
        if (Power.POWER_ILUSION.equals(power.getName())) {
            this.illusionTicks = power.getEffectDurationTicks();
            double[] vyOffsets = {-4.5, -2.5, -1.0, 1.0, 2.5, 4.0, -5.5};
            for (int i = 0; i < 7; i++) {
                double dvx = vx * (0.86 + 0.05 * (i % 3));
                double dvy = vy + vyOffsets[i];
                decoyBalls.add(new double[]{positionX, positionY, dvx, dvy});
            }
        }
    }

    /**
     * Clears the active power shot state after hitting the opponent or bouncing off a wall.
     * Decoy balls for Ilusión remain active until their 2.5s effect timer expires.
     */
    public void clearPowerShotImpact() {
        this.activePowerShot      = null;
        this.powerShotOwner       = null;
        this.powerShotFlightTicks = 0;
    }

    /**
     * Clears all power shot state including Ilusión decoy balls immediately.
     */
    public void clearAllPowerShotEffects() {
        clearPowerShotImpact();
        this.illusionTicks = 0;
        this.decoyBalls.clear();
    }

    /**
     * Advances the ball physics by one tick and resolves boundary and goal crossbar collisions.
     * The space above the goals is completely open so the ball can travel over the crossbar.
     *
     * @param field the playing field
     */
    public void move(Field field) {
        boolean isStraightPower = activePowerShot != null && powerShotFlightTicks > 0 &&
                (Power.POWER_AURA_AZUL.equals(activePowerShot.getName()) ||
                 Power.POWER_RAYO.equals(activePowerShot.getName()));
        boolean isTornadoPower = activePowerShot != null && powerShotFlightTicks > 0 &&
                Power.POWER_TORNADO.equals(activePowerShot.getName());

        if (isStraightPower) {
            velocityY += 0.05;
            velocityX *= 0.997;
        } else if (isTornadoPower) {
            velocityY += GRAVITY * 1.4;
            velocityX *= 0.995;
        } else {
            velocityY += GRAVITY;
            velocityX *= AIR_FRICTION;
        }

        double prevY = positionY;
        positionX += velocityX;
        positionY += velocityY;
        rotationAngle += velocityX / RADIUS;

        if (powerShotFlightTicks > 0) {
            powerShotFlightTicks--;
            if (powerShotFlightTicks == 0) {
                clearPowerShotImpact();
            }
        }

        if (illusionTicks > 0) {
            illusionTicks--;
            if (illusionTicks == 0) {
                decoyBalls.clear();
            }
        }

        int width         = field.getWidth();
        int groundY       = field.getGroundY();
        int goalTop       = field.getGoalTop();
        int leftGoalLine  = field.getLeftGoalLine();
        int rightGoalLine = field.getRightGoalLine();

        // Ceiling bounce
        if (positionY - RADIUS < 0) {
            positionY = RADIUS;
            velocityY = Math.abs(velocityY) * BOUNCE;
        }

        // Ground bounce
        if (positionY + RADIUS > groundY) {
            positionY = groundY - RADIUS;
            if (isTornadoPower) {
                velocityY = -7.5;
            } else {
                velocityY = -Math.abs(velocityY) * BOUNCE;
                if (Math.abs(velocityY) < 1.1) {
                    velocityY = 0;
                }
            }
            velocityX *= GROUND_FRICTION;
        }

        // Left screen boundary (above goal or back of net)
        if (positionX - RADIUS < 0) {
            positionX = RADIUS;
            velocityX = Math.abs(velocityX) * BOUNCE;
            if (positionY <= goalTop) {
                clearPowerShotImpact();
            }
        }

        // Right screen boundary (above goal or back of net)
        if (positionX + RADIUS > width) {
            positionX = width - RADIUS;
            velocityX = -Math.abs(velocityX) * BOUNCE;
            if (positionY <= goalTop) {
                clearPowerShotImpact();
            }
        }

        // Left goal crossbar top & bottom surfaces (airspace above goalTop is open!)
        if (positionX < leftGoalLine) {
            if (prevY <= goalTop && positionY + RADIUS >= goalTop && velocityY >= 0) {
                positionY = goalTop - RADIUS;
                velocityY = -Math.abs(velocityY) * BOUNCE;
                if (Math.abs(velocityY) < 1.0) {
                    velocityY = 0;
                }
            } else if (prevY > goalTop && positionY - RADIUS <= goalTop && velocityY < 0) {
                positionY = goalTop + RADIUS;
                velocityY = Math.abs(velocityY) * BOUNCE;
            }
        }

        // Right goal crossbar top & bottom surfaces (airspace above goalTop is open!)
        if (positionX > rightGoalLine) {
            if (prevY <= goalTop && positionY + RADIUS >= goalTop && velocityY >= 0) {
                positionY = goalTop - RADIUS;
                velocityY = -Math.abs(velocityY) * BOUNCE;
                if (Math.abs(velocityY) < 1.0) {
                    velocityY = 0;
                }
            } else if (prevY > goalTop && positionY - RADIUS <= goalTop && velocityY < 0) {
                positionY = goalTop + RADIUS;
                velocityY = Math.abs(velocityY) * BOUNCE;
            }
        }

        // Crossbar tip posts only (at exact corner of crossbar, no wall above)
        resolveCrossbarPost(leftGoalLine, goalTop);
        resolveCrossbarPost(rightGoalLine, goalTop);

        updateDecoyBalls(field);
        capSpeed();
    }

    private void resolveCrossbarPost(double postX, double postY) {
        double dx = positionX - postX;
        double dy = positionY - postY;
        double distSq = dx * dx + dy * dy;
        double postRadius = RADIUS + 2.0;
        if (distSq < postRadius * postRadius && distSq > 0.0001) {
            double dist = Math.sqrt(distSq);
            double nx = dx / dist;
            double ny = dy / dist;
            positionX = postX + nx * postRadius;
            positionY = postY + ny * postRadius;

            double vn = velocityX * nx + velocityY * ny;
            if (vn < 0) {
                velocityX -= (1.0 + BOUNCE) * vn * nx;
                velocityY -= (1.0 + BOUNCE) * vn * ny;
            }
        }
    }

    private void updateDecoyBalls(Field field) {
        if (decoyBalls.isEmpty()) return;

        int width = field.getWidth();
        int groundY = field.getGroundY();
        int goalTop = field.getGoalTop();
        int leftGoalLine = field.getLeftGoalLine();
        int rightGoalLine = field.getRightGoalLine();

        for (double[] d : decoyBalls) {
            d[3] += GRAVITY;
            d[2] *= AIR_FRICTION;
            d[0] += d[2];
            d[1] += d[3];

            if (d[1] - RADIUS < 0) {
                d[1] = RADIUS;
                d[3] = Math.abs(d[3]) * BOUNCE;
            }
            if (d[1] + RADIUS > groundY) {
                d[1] = groundY - RADIUS;
                d[3] = -Math.abs(d[3]) * BOUNCE;
            }
            if (d[0] - RADIUS < 0) {
                d[0] = RADIUS;
                d[2] = Math.abs(d[2]) * BOUNCE;
            }
            if (d[0] + RADIUS > width) {
                d[0] = width - RADIUS;
                d[2] = -Math.abs(d[2]) * BOUNCE;
            }
            if ((d[0] < leftGoalLine || d[0] > rightGoalLine)
                    && d[1] + RADIUS >= goalTop && d[1] < goalTop && d[3] > 0) {
                d[1] = goalTop - RADIUS;
                d[3] = -Math.abs(d[3]) * BOUNCE;
            }
        }
    }

    private void capSpeed() {
        if (velocityX >  MAX_SPEED) velocityX =  MAX_SPEED;
        if (velocityX < -MAX_SPEED) velocityX = -MAX_SPEED;
        if (velocityY >  MAX_SPEED) velocityY =  MAX_SPEED;
        if (velocityY < -MAX_SPEED) velocityY = -MAX_SPEED;
    }

    /**
     * Resets the ball to its initial position with zero velocity and clears power shots.
     */
    public void reset() {
        positionX     = initialX;
        positionY     = initialY;
        velocityX     = 0;
        velocityY     = 0;
        rotationAngle = 0;
        clearAllPowerShotEffects();
    }

    /** @return current X position of the ball's centre */
    public double getPositionX() { return positionX; }

    /** @return current Y position of the ball's centre */
    public double getPositionY() { return positionY; }

    /** @return current horizontal velocity */
    public double getVelocityX() { return velocityX; }

    /** @return current vertical velocity */
    public double getVelocityY() { return velocityY; }

    /** @return current rotation angle in radians */
    public double getRotationAngle() { return rotationAngle; }

    /** @return active power shot currently traveling on this ball, or null */
    public Power getActivePowerShot() { return activePowerShot; }

    /** @return player who launched the active power shot, or null */
    public Player getPowerShotOwner() { return powerShotOwner; }

    /** @return true if the ball is currently carrying an active power shot */
    public boolean hasActivePowerShot() { return activePowerShot != null; }

    /** @return unmodifiable list of decoy ball states [x, y, vx, vy] for Ilusión */
    public List<double[]> getDecoyBalls() {
        return Collections.unmodifiableList(decoyBalls);
    }
}
