package domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Represents the soccer ball with its physics, collisions, and power shots. */
public class Ball implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int RADIUS = 20;

    private static final double GRAVITY = 0.48;
    private static final double BOUNCE = 0.70;
    private static final double AIR_FRICTION = 0.986;
    private static final double GROUND_FRICTION = 0.950;
    private static final double MAX_SPEED = 22.0;

    private double positionX;
    private double positionY;
    private double velocityX;
    private double velocityY;
    private double rotationAngle;

    private final double initialX;
    private final double initialY;

    private Power activePowerShot;
    private Player powerShotOwner;
    private int powerShotFlightTicks;
    private int illusionTicks;
    private final List<double[]> decoyBalls;

    /**
     * Creates a ball at the specified initial position.
     * @param x horizontal coordinate
     * @param y vertical coordinate
     */
    public Ball(double x, double y) {
        this.positionX = x;
        this.positionY = y;
        this.initialX = x;
        this.initialY = y;
        this.velocityX = 0;
        this.velocityY = 0;
        this.rotationAngle = 0;
        this.activePowerShot = null;
        this.powerShotOwner = null;
        this.powerShotFlightTicks = 0;
        this.illusionTicks = 0;
        this.decoyBalls = new ArrayList<>();
    }

    /**
     * Applies an impulse to the ball velocity.
     * @param impulseX horizontal velocity impulse
     * @param impulseY vertical velocity impulse
     */
    public void applyImpulse(double impulseX, double impulseY) {
        this.velocityX += impulseX;
        this.velocityY += impulseY;
        capSpeed();
    }

    /**
     * Sets the ball position directly.
     * @param x horizontal coordinate
     * @param y vertical coordinate
     */
    public void setPosition(double x, double y) {
        this.positionX = x;
        this.positionY = y;
    }

    /**
     * Sets the ball velocity directly.
     * @param vx horizontal velocity component
     * @param vy vertical velocity component
     */
    public void setVelocity(double vx, double vy) {
        this.velocityX = vx;
        this.velocityY = vy;
        capSpeed();
    }

    /**
     * Launches the ball as an active power shot.
     * @param power assigned special power
     * @param owner player who launched the power shot
     * @param direction shot horizontal direction (+1 right, -1 left)
     */
    public void launchPowerShot(Power power, Player owner, int direction) {
        if (power == null) return;
        this.activePowerShot = power;
        this.powerShotOwner = owner;
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

    /** Clears the active power shot state after impact or rebound. */
    public void clearPowerShotImpact() {
        this.activePowerShot = null;
        this.powerShotOwner = null;
        this.powerShotFlightTicks = 0;
    }

    /** Clears all power shot effects and decoy balls. */
    public void clearAllPowerShotEffects() {
        clearPowerShotImpact();
        this.illusionTicks = 0;
        this.decoyBalls.clear();
    }

    /**
     * Advances ball physics and resolves collisions for one tick.
     * @param field playing field boundaries
     */
    public void move(Field field) {
        applyGravityAndAirFriction();

        double prevY = positionY;
        positionX += velocityX;
        positionY += velocityY;
        rotationAngle += velocityX / RADIUS;

        tickPowerShotTimers();
        resolveWallAndFloorBounces(field);
        resolveCrossbarSurfaces(field, prevY);
        resolveCrossbarPost(field.getLeftGoalLine(), field.getGoalTop());
        resolveCrossbarPost(field.getRightGoalLine(), field.getGoalTop());

        updateDecoyBalls(field);
        capSpeed();
    }

    /** Returns true when a Tornado power shot is currently in flight. */
    private boolean isActiveTornadoShot() {
        return activePowerShot != null && powerShotFlightTicks > 0
                && Power.POWER_TORNADO.equals(activePowerShot.getName());
    }

    /**
     * Applies gravity and aerodynamic drag according to the active shot type.
     */
    private void applyGravityAndAirFriction() {
        boolean isStraightPower = activePowerShot != null && powerShotFlightTicks > 0
                && (Power.POWER_AURA_AZUL.equals(activePowerShot.getName())
                || Power.POWER_RAYO.equals(activePowerShot.getName()));

        if (isStraightPower) {
            velocityY += 0.05;
            velocityX *= 0.997;
        } else if (isActiveTornadoShot()) {
            velocityY += GRAVITY * 1.4;
            velocityX *= 0.995;
        } else {
            velocityY += GRAVITY;
            velocityX *= AIR_FRICTION;
        }
    }

    /**
     * Decrements active power shot flight and Illusion decoy duration timers.
     */
    private void tickPowerShotTimers() {
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
    }

    /**
     * Resolves ceiling, ground, and outer screen wall bounces.
     * @param field playing field boundaries
     */
    private void resolveWallAndFloorBounces(Field field) {
        int width = field.getWidth();
        int groundY = field.getGroundY();
        int goalTop = field.getGoalTop();

        if (positionY - RADIUS < 0) {
            positionY = RADIUS;
            velocityY = Math.abs(velocityY) * BOUNCE;
        }

        if (positionY + RADIUS > groundY) {
            positionY = groundY - RADIUS;
            if (isActiveTornadoShot()) {
                velocityY = -7.5;
            } else {
                velocityY = -Math.abs(velocityY) * BOUNCE;
                if (Math.abs(velocityY) < 1.1) {
                    velocityY = 0;
                }
            }
            velocityX *= GROUND_FRICTION;
        }

        if (positionX - RADIUS < 0) {
            positionX = RADIUS;
            velocityX = Math.abs(velocityX) * BOUNCE;
            if (positionY <= goalTop) {
                clearPowerShotImpact();
            }
        }

        if (positionX + RADIUS > width) {
            positionX = width - RADIUS;
            velocityX = -Math.abs(velocityX) * BOUNCE;
            if (positionY <= goalTop) {
                clearPowerShotImpact();
            }
        }
    }

    /**
     * Resolves rebounds on top and underside surfaces of either goal crossbar.
     * @param field playing field boundaries
     * @param prevY ball vertical coordinate from the previous tick
     */
    private void resolveCrossbarSurfaces(Field field, double prevY) {
        int goalTop = field.getGoalTop();
        if (positionX >= field.getLeftGoalLine() && positionX <= field.getRightGoalLine()) {
            return;
        }

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

    /**
     * Resolves circular collision against a crossbar tip post.
     * @param postX crossbar tip horizontal coordinate
     * @param postY crossbar tip vertical coordinate
     */
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

    /**
     * Advances physics for active Illusion decoy balls.
     * @param field playing field boundaries
     */
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

    /** Clamps velocity components within the maximum allowed speed. */
    private void capSpeed() {
        if (velocityX > MAX_SPEED) velocityX = MAX_SPEED;
        if (velocityX < -MAX_SPEED) velocityX = -MAX_SPEED;
        if (velocityY > MAX_SPEED) velocityY = MAX_SPEED;
        if (velocityY < -MAX_SPEED) velocityY = -MAX_SPEED;
    }

    /**
     * Resets the ball to its initial state.
     */
    public void reset() {
        positionX = initialX;
        positionY = initialY;
        velocityX = 0;
        velocityY = 0;
        rotationAngle = 0;
        clearAllPowerShotEffects();
    }

    /** Returns current X position of the ball's centre. */
    public double getPositionX() { return positionX; }

    /** Returns current Y position of the ball's centre. */
    public double getPositionY() { return positionY; }

    /** Returns current horizontal velocity. */
    public double getVelocityX() { return velocityX; }

    /** Returns current vertical velocity. */
    public double getVelocityY() { return velocityY; }

    /** Returns current rotation angle in radians. */
    public double getRotationAngle() { return rotationAngle; }

    /** Returns active power shot currently traveling on this ball, or null. */
    public Power getActivePowerShot() { return activePowerShot; }

    /** Returns player who launched the active power shot, or null. */
    public Player getPowerShotOwner() { return powerShotOwner; }

    /** Returns true if the ball is currently carrying an active power shot. */
    public boolean hasActivePowerShot() { return activePowerShot != null; }

    /** Returns unmodifiable list of decoy ball states [x, y, vx, vy] for Ilusión. */
    public List<double[]> getDecoyBalls() {
        return Collections.unmodifiableList(decoyBalls);
    }
}
