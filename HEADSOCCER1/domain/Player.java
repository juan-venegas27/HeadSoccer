package domain;

import java.awt.Color;
import java.io.Serializable;

/**
 * Represents a player in the DOPOSoccer game.
 * Manages fast, goal-height-calibrated jumping, movement, kicking,
 * statistics (speed, jump, kick, survival), upgrade points,
 * health, 3-second stun, power bar, and power shot effects.
 */
public class Player implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Radius of the player's head circle in pixels. */
    public static final int HEAD_RADIUS = 28;

    /** Radius of the single foot circle in pixels. */
    public static final int FOOT_RADIUS = 11;

    public static final int    MAX_STAT_LEVEL        = 10;
    public static final int    BASE_UPGRADE_COST     = 500;
    public static final int    STUN_DURATION_TICKS   = 180; // 3 seconds at 60 FPS
    public static final double POWER_CHARGE_PER_TICK = 100.0 / 600.0; // 10 seconds to full

    // Calibrated jump: quick arc (~0.52s) reaching just slightly above goal height (130 px)
    private static final double GRAVITY          = 0.85;
    private static final double BASE_JUMP_FORCE  = -13.2;
    private static final double BASE_MOVE_SPEED  = 5.0;
    private static final double BASE_KICK_DAMAGE = 10.0;

    private double positionX;
    private double positionY;
    private double velocityX;
    private double velocityY;

    private final double initialX;
    private final double initialY;
    private final int    initialFacingDirection;

    private final Color  color;
    private final String name;
    private final String country;
    private final Power  power;
    private int          facingDirection;

    private boolean onGround;
    private boolean movingLeft;
    private boolean movingRight;
    private boolean wantsToJump;

    private int kickAnimTicks;

    // Statistics (1 to 10)
    private int speedStat;
    private int jumpStat;
    private int kickStat;
    private int survivalStat;
    private int points;

    // Health, power bar, stun, and active rival effects
    private double currentHealth;
    private double powerBar; // 0.0 to 100.0
    private int    stunTicks;
    private String activeEffect;
    private int    effectTicks;
    private double effectVelocityX;

    /**
     * Creates a Player with the specified position, color, name, power, and facing direction.
     *
     * @param x               initial centre X
     * @param y               initial centre Y (centre of head)
     * @param color           player color
     * @param name            player name
     * @param power           special power assigned to this player
     * @param facingDirection +1 facing right, -1 facing left
     */
    public Player(double x, double y, Color color, String name, Power power, int facingDirection) {
        this.positionX              = x;
        this.positionY              = y;
        this.initialX               = x;
        this.initialY               = y;
        this.color                  = color;
        this.name                   = name;
        this.power                  = (power != null) ? power : Power.createForCountry(Power.COUNTRY_KOREA);
        this.country                = this.power.getCountry();
        this.initialFacingDirection = (facingDirection >= 0) ? 1 : -1;
        this.facingDirection        = this.initialFacingDirection;

        this.velocityX     = 0;
        this.velocityY     = 0;
        this.onGround      = true;
        this.kickAnimTicks = 0;

        this.speedStat    = 1;
        this.jumpStat     = 1;
        this.kickStat     = 1;
        this.survivalStat = 1;
        this.points       = 0;

        this.currentHealth   = getMaxHealth();
        this.powerBar        = 0.0;
        this.stunTicks       = 0;
        this.activeEffect    = null;
        this.effectTicks     = 0;
        this.effectVelocityX = 0;
    }

    /**
     * Updates the player's physics, power bar charge, stun, effects, and boundaries.
     *
     * @param minX    minimum allowed X coordinate for player centre
     * @param maxX    maximum allowed X coordinate for player centre
     * @param groundY Y coordinate of the ground surface
     */
    public void update(int minX, int maxX, int groundY) {
        // Rule 1 & 2: Bar fills in 10s, and starts recharging immediately when activated
        if (powerBar < 100.0) {
            powerBar = Math.min(100.0, powerBar + POWER_CHARGE_PER_TICK);
        }

        if (kickAnimTicks > 0) {
            kickAnimTicks--;
        }

        if (effectTicks > 0) {
            effectTicks--;
            if (Power.EFFECT_PUSH.equals(activeEffect)) {
                velocityX = effectVelocityX;
                effectVelocityX *= 0.92;
            }
            if (effectTicks == 0) {
                activeEffect    = null;
                effectVelocityX = 0;
            }
        }

        if (stunTicks > 0) {
            stunTicks--;
            velocityX   = 0;
            wantsToJump = false;
            if (stunTicks == 0) {
                currentHealth = getMaxHealth();
            }
        } else if (!Power.EFFECT_UNCONSCIOUS.equals(activeEffect)
                && !Power.EFFECT_PUSH.equals(activeEffect)) {
            double moveSpeed = getEffectiveMoveSpeed();
            if (movingLeft && !movingRight) {
                velocityX = -moveSpeed;
                facingDirection = -1;
            } else if (movingRight && !movingLeft) {
                velocityX = moveSpeed;
                facingDirection = 1;
            } else {
                velocityX = 0;
            }

            if (wantsToJump && onGround && !Power.EFFECT_ELECTROCUTE.equals(activeEffect)) {
                velocityY = getEffectiveJumpForce();
                onGround  = false;
            }
        } else {
            wantsToJump = false;
            if (Power.EFFECT_UNCONSCIOUS.equals(activeEffect)) {
                velocityX *= 0.9;
            }
        }
        wantsToJump = false;

        positionX += velocityX;
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

        if (positionX < minX) {
            positionX = minX;
            velocityX = 0;
        }
        if (positionX > maxX) {
            positionX = maxX;
            velocityX = 0;
        }
    }

    /** Schedules a jump action on the next update. */
    public void jump() {
        if (!canAct()) return;
        if (Power.EFFECT_ELECTROCUTE.equals(activeEffect)) return;
        wantsToJump = true;
    }

    /** @param moving true to move left */
    public void setMovingLeft(boolean moving) {
        this.movingLeft = moving;
    }

    /** @param moving true to move right */
    public void setMovingRight(boolean moving) {
        this.movingRight = moving;
    }

    /**
     * Performs a kick targeting the ball and optionally damaging an opponent in foot reach.
     * If the kick touches the ball, it only propels the ball (or launches power shot) and deals no damage.
     *
     * @param ball     the ball
     * @param opponent the rival player (may be null)
     */
    public void hit(Ball ball, Player opponent) {
        if (!canAct()) return;

        kickAnimTicks = 12;

        boolean touchedBall = false;
        if (ball != null) {
            double dx = ball.getPositionX() - positionX;
            double dy = ball.getPositionY() - positionY;
            double dist = Math.sqrt(dx * dx + dy * dy);
            double range = HEAD_RADIUS + Ball.RADIUS + 26;

            if (dist < range && dist > 0) {
                touchedBall = true;
                if (power.isActive()) {
                    executePowerShot(ball);
                } else {
                    double nx = dx / dist;
                    double ny = dy / dist;
                    double kickForceX = nx * 14.5 + velocityX * 0.4;
                    double kickForceY = ny * 12.5 - 5.5;
                    ball.setVelocity(kickForceX, kickForceY);
                }
            }
        }

        if (!touchedBall && opponent != null && !opponent.isStunned()) {
            double footCenterX = positionX + facingDirection * 18.0;
            double footCenterY = getFootY();
            double odx = opponent.getPositionX() - footCenterX;
            double ody = opponent.getPositionY() - footCenterY;
            double oDist = Math.sqrt(odx * odx + ody * ody);
            double kickReach = HEAD_RADIUS + FOOT_RADIUS + 20.0;

            if (oDist <= kickReach) {
                double damage = BASE_KICK_DAMAGE * getKickMultiplier();
                opponent.takeDamage(damage);
            }
        }
    }

    /**
     * Executes this player's active power shot on the ball and consumes the aura (Rule 4).
     *
     * @param ball the ball being struck
     */
    public void executePowerShot(Ball ball) {
        if (ball == null || !power.isActive()) return;
        int shotDir = initialFacingDirection;
        if (ball.getPositionX() < positionX - 8) {
            shotDir = -1;
        } else if (ball.getPositionX() > positionX + 8) {
            shotDir = 1;
        } else {
            shotDir = facingDirection;
        }
        power.deactivate();
        ball.launchPowerShot(power, this, shotDir);
    }

    /**
     * Activates the special power aura when bar is 100% and no aura is currently active (Rules 2 & 7).
     */
    public void activatePower() {
        if (!canAct()) return;
        if (powerBar >= 100.0 && !power.isActive()) {
            powerBar = 0.0;
            power.activate();
        }
    }

    /**
     * Increases power bar immediately by 1/4 when this player concedes a goal (Rule 1).
     */
    public void onGoalConceded() {
        powerBar = Math.min(100.0, powerBar + 25.0);
    }

    /**
     * Applies damage to this player (never below 0). Enters 3s stun if health reaches 0.
     *
     * @param damage damage amount
     */
    public void takeDamage(double damage) {
        if (damage <= 0 || isStunned()) return;
        currentHealth = Math.max(0.0, currentHealth - damage);
        if (currentHealth <= 0.0) {
            stunTicks   = STUN_DURATION_TICKS;
            movingLeft  = false;
            movingRight = false;
            wantsToJump = false;
            velocityX   = 0;
        }
    }

    /**
     * Applies a power shot effect to this player (Rule 8: replaces any previous effect).
     *
     * @param effectName    effect name
     * @param durationTicks duration in ticks
     * @param pushDir       direction toward own goal (+1 or -1)
     */
    public void applyEffect(String effectName, int durationTicks, int pushDir) {
        this.activeEffect    = effectName;
        this.effectTicks     = Math.max(1, durationTicks);
        this.effectVelocityX = 0;

        if (Power.EFFECT_PUSH.equals(effectName)) {
            this.effectVelocityX = pushDir * 11.0;
        } else if (Power.EFFECT_UNCONSCIOUS.equals(effectName)) {
            this.velocityY   = -12.5;
            this.onGround    = false;
            this.movingLeft  = false;
            this.movingRight = false;
            this.wantsToJump = false;
        }
    }

    /**
     * Upgrades one of the player's four statistics using accumulated points.
     *
     * @param statName "speed", "jump", "kick", or "survival"
     * @throws HeadSoccerException if stat is invalid, maxed, or points are insufficient
     */
    public void upgradeStat(String statName) throws HeadSoccerException {
        if (statName == null) {
            throw new HeadSoccerException(HeadSoccerException.INVALID_STAT_NAME);
        }
        String key = statName.trim().toLowerCase();
        int currentLevel = getStatLevel(key);
        if (currentLevel < 1) {
            throw new HeadSoccerException(HeadSoccerException.INVALID_STAT_NAME);
        }
        if (currentLevel >= MAX_STAT_LEVEL) {
            throw new HeadSoccerException(HeadSoccerException.MAX_STAT_REACHED);
        }
        int cost = getUpgradeCost(currentLevel);
        if (points < cost) {
            throw new HeadSoccerException(HeadSoccerException.INSUFFICIENT_POINTS
                    + " (Necesitas " + cost + ", tienes " + points + ")");
        }

        points -= cost;
        switch (key) {
            case "speed":
                speedStat++;
                break;
            case "jump":
                jumpStat++;
                break;
            case "kick":
                kickStat++;
                break;
            case "survival":
                survivalStat++;
                currentHealth = getMaxHealth();
                break;
            default:
                throw new HeadSoccerException(HeadSoccerException.INVALID_STAT_NAME);
        }
    }

    /**
     * Calculates upgrade cost starting at 500 and doubling each level.
     *
     * @param currentLevel stat level (1..9)
     * @return upgrade cost
     */
    public static int getUpgradeCost(int currentLevel) {
        if (currentLevel >= MAX_STAT_LEVEL) return 0;
        return BASE_UPGRADE_COST * (1 << Math.max(0, currentLevel - 1));
    }

    /** @return level of stat by name, or -1 if unknown */
    public int getStatLevel(String statName) {
        if (statName == null) return -1;
        switch (statName.trim().toLowerCase()) {
            case "speed": return speedStat;
            case "jump": return jumpStat;
            case "kick": return kickStat;
            case "survival": return survivalStat;
            default: return -1;
        }
    }

    /**
     * Sets all 4 statistics and accumulated points.
     */
    public void setProgress(int speed, int jump, int kick, int survival, int points) {
        this.speedStat     = clampStat(speed);
        this.jumpStat      = clampStat(jump);
        this.kickStat      = clampStat(kick);
        this.survivalStat  = clampStat(survival);
        this.points        = Math.max(0, points);
        this.currentHealth = getMaxHealth();
    }

    private int clampStat(int level) {
        return Math.max(1, Math.min(MAX_STAT_LEVEL, level));
    }

    /** Adds earned match points. */
    public void addPoints(int earned) {
        if (earned > 0) {
            this.points += earned;
        }
    }

    /** Resets position and movement flags after a goal. */
    public void reset() {
        positionX       = initialX;
        positionY       = initialY;
        velocityX       = 0;
        velocityY       = 0;
        onGround        = true;
        movingLeft      = false;
        movingRight     = false;
        wantsToJump     = false;
        kickAnimTicks   = 0;
        facingDirection = initialFacingDirection;
    }

    /** Resets full match state for testing or new round. */
    public void resetForMatch() {
        reset();
        currentHealth   = getMaxHealth();
        powerBar        = 0.0;
        stunTicks       = 0;
        activeEffect    = null;
        effectTicks     = 0;
        effectVelocityX = 0;
        if (power != null) {
            power.deactivate();
        }
    }

    /** @return true if player is not stunned and not unconscious */
    public boolean canAct() {
        return stunTicks <= 0 && !Power.EFFECT_UNCONSCIOUS.equals(activeEffect);
    }

    /** @return true if player is currently stunned */
    public boolean isStunned() { return stunTicks > 0; }

    /** @return kick damage multiplier: 1 + 0.10 * (Kick - 1) */
    public double getKickMultiplier() {
        return 1.0 + 0.10 * (kickStat - 1);
    }

    /** @return max health: 100 * (1 + 0.10 * (Survival - 1)) */
    public double getMaxHealth() {
        return Math.round(100.0 * (1.0 + 0.10 * (survivalStat - 1)));
    }

    /** @return effective horizontal speed considering Speed stat and Electrocuted effect */
    public double getEffectiveMoveSpeed() {
        double speed = BASE_MOVE_SPEED * (1.0 + 0.10 * (speedStat - 1));
        if (Power.EFFECT_ELECTROCUTE.equals(activeEffect)) {
            speed *= 0.5;
        }
        return speed;
    }

    /** @return effective jump velocity calibrated to clear goal crossbar cleanly */
    public double getEffectiveJumpForce() {
        return BASE_JUMP_FORCE * (1.0 + 0.02 * (jumpStat - 1));
    }

    public void setPositionX(double x) { this.positionX = x; }

    public double getPositionX() { return positionX; }
    public double getPositionY() { return positionY; }

    public double getFootX() {
        if (kickAnimTicks > 0) {
            double progress = Math.sin((kickAnimTicks / 12.0) * Math.PI);
            return positionX + facingDirection * progress * 16.0;
        }
        return positionX;
    }

    public double getFootY() {
        if (kickAnimTicks > 0) {
            double progress = Math.sin((kickAnimTicks / 12.0) * Math.PI);
            return positionY + HEAD_RADIUS - progress * 6.0;
        }
        return positionY + HEAD_RADIUS;
    }

    public double getKickAngle() {
        if (kickAnimTicks <= 0) return 0.0;
        double progress = Math.sin((kickAnimTicks / 12.0) * Math.PI);
        return -facingDirection * progress * 0.9;
    }

    public double getVelocityX() {
        if (stunTicks > 0 || Power.EFFECT_UNCONSCIOUS.equals(activeEffect)) {
            return velocityX;
        }
        if (movingLeft && !movingRight) return -getEffectiveMoveSpeed();
        if (movingRight && !movingLeft) return getEffectiveMoveSpeed();
        return velocityX;
    }

    public double getVelocityY() { return velocityY; }
    public boolean isOnGround() { return onGround; }
    public Color getColor() { return color; }
    public String getName() { return name; }
    public String getCountry() { return country; }
    public Power getPower() { return power; }
    public int getFacingDirection() { return facingDirection; }
    public double getCurrentHealth() { return currentHealth; }
    public double getPowerBar() { return powerBar; }
    public void setPowerBar(double value) { this.powerBar = Math.max(0.0, Math.min(100.0, value)); }
    public String getActiveEffect() { return activeEffect; }
    public int getSpeedStat() { return speedStat; }
    public int getJumpStat() { return jumpStat; }
    public int getKickStat() { return kickStat; }
    public int getSurvivalStat() { return survivalStat; }
    public int getPoints() { return points; }
}
