package domain;

import java.awt.Color;
import java.io.Serializable;

/** Represents a player with movement, kicks, statistics, health, and special power. */
public class Player implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int HEAD_RADIUS = 28;
    public static final int FOOT_RADIUS = 11;

    public static final int MAX_STAT_LEVEL = 10;
    public static final int BASE_UPGRADE_COST = 500;
    public static final int STUN_DURATION_TICKS = 180;
    public static final double POWER_CHARGE_PER_TICK = 100.0 / 600.0;

    private static final double GRAVITY = 0.85;
    private static final double BASE_JUMP_FORCE = -13.2;
    private static final double BASE_MOVE_SPEED = 5.0;
    private static final double BASE_KICK_DAMAGE = 10.0;

    private double positionX;
    private double positionY;
    private double velocityX;
    private double velocityY;

    private final double initialX;
    private final double initialY;
    private final int initialFacingDirection;

    private final Color color;
    private final String name;
    private final String country;
    private final Power power;
    private int facingDirection;

    private boolean onGround;
    private boolean movingLeft;
    private boolean movingRight;
    private boolean wantsToJump;

    private int kickAnimTicks;

    private int speedStat;
    private int jumpStat;
    private int kickStat;
    private int survivalStat;
    private int points;

    private double currentHealth;
    private double powerBar;
    private int stunTicks;
    private String activeEffect;
    private int effectTicks;
    private double effectVelocityX;

    /**
     * Creates a player with the given initial state and power.
     * @param x horizontal coordinate
     * @param y vertical coordinate
     * @param color player kit color
     * @param name identifying name
     * @param power assigned special power
     * @param facingDirection initial facing direction (+1 right, -1 left)
     */
    public Player(double x, double y, Color color, String name, Power power, int facingDirection) {
        this.positionX = x;
        this.positionY = y;
        this.initialX = x;
        this.initialY = y;
        this.color = color;
        this.name = name;
        this.power = (power != null) ? power : Power.createForCountry(Power.COUNTRY_KOREA);
        this.country = this.power.getCountry();
        this.initialFacingDirection = (facingDirection >= 0) ? 1 : -1;
        this.facingDirection = this.initialFacingDirection;

        this.velocityX = 0;
        this.velocityY = 0;
        this.onGround = true;
        this.kickAnimTicks = 0;

        this.speedStat = 1;
        this.jumpStat = 1;
        this.kickStat = 1;
        this.survivalStat = 1;
        this.points = 0;

        this.currentHealth = getMaxHealth();
        this.powerBar = 0.0;
        this.stunTicks = 0;
        this.activeEffect = null;
        this.effectTicks = 0;
        this.effectVelocityX = 0;
    }

    /**
     * Updates player movement, timers, gravity, and bounds.
     * @param minX minimum allowed horizontal coordinate
     * @param maxX maximum allowed horizontal coordinate
     * @param groundY vertical coordinate of the ground surface
     */
    public void update(int minX, int maxX, int groundY) {
        tickTimersAndEffects();
        updateControlledMovement();
        applyGravityAndBounds(minX, maxX, groundY);
    }

    /**
     * Advances power bar recharge, kick animation, and active effect timers.
     */
    private void tickTimersAndEffects() {
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
                activeEffect = null;
                effectVelocityX = 0;
            }
        }
    }

    /**
     * Updates horizontal velocity and jumping from player input, stun, or disabling effects.
     */
    private void updateControlledMovement() {
        if (stunTicks > 0) {
            stunTicks--;
            velocityX = 0;
            wantsToJump = false;
            if (stunTicks == 0) {
                currentHealth = getMaxHealth();
            }
            return;
        }

        if (!Power.EFFECT_UNCONSCIOUS.equals(activeEffect)
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
                onGround = false;
            }
        } else if (Power.EFFECT_UNCONSCIOUS.equals(activeEffect)) {
            velocityX *= 0.9;
        }
        wantsToJump = false;
    }

    /**
     * Integrates velocity, gravity, floor contact, and horizontal bounds.
     * @param minX minimum allowed horizontal coordinate
     * @param maxX maximum allowed horizontal coordinate
     * @param groundY vertical coordinate of the ground surface
     */
    private void applyGravityAndBounds(int minX, int maxX, int groundY) {
        positionX += velocityX;
        velocityY += GRAVITY;
        positionY += velocityY;

        double floorY = groundY - HEAD_RADIUS;
        if (positionY >= floorY) {
            positionY = floorY;
            velocityY = 0;
            onGround = true;
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

    /**
     * 
     * @param moving true while directional movement key is held
     */
    public void setMovingLeft(boolean moving) {
        this.movingLeft = moving;
    }

    /**
     * 
     * @param moving true while directional movement key is held
     */
    public void setMovingRight(boolean moving) {
        this.movingRight = moving;
    }

    /**
     * Performs a kick against the ball or nearby opponent.
     * @param ball active match ball
     * @param opponent opposing player
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
                    double kickForceX = nx * 11.5 + velocityX * 0.35;
                    double kickForceY = ny * 10.0 - 4.5;
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
     * Launches the player's active power shot on the ball.
     * @param ball active match ball
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
     * Activates the player's special power aura when charged.
     */
    public void activatePower() {
        if (!canAct()) return;
        if (powerBar >= 100.0 && !power.isActive()) {
            powerBar = 0.0;
            power.activate();
        }
    }

    /** Increases the power bar when conceding a goal. */
    public void onGoalConceded() {
        powerBar = Math.min(100.0, powerBar + 25.0);
    }

    /**
     * Applies damage and stuns the player when health reaches zero.
     * @param damage damage amount applied to health
     */
    public void takeDamage(double damage) {
        if (damage <= 0 || isStunned()) return;
        currentHealth = Math.max(0.0, currentHealth - damage);
        if (currentHealth <= 0.0) {
            stunTicks = STUN_DURATION_TICKS;
            movingLeft = false;
            movingRight = false;
            wantsToJump = false;
            velocityX = 0;
        }
    }

    /**
     * Applies a rival power effect to this player.
     * @param effectName status effect identifier applied to rival
     * @param durationTicks effect duration in game ticks
     * @param pushDir push direction (+1 right, -1 left)
     */
    public void applyEffect(String effectName, int durationTicks, int pushDir) {
        this.activeEffect = effectName;
        this.effectTicks = Math.max(1, durationTicks);
        this.effectVelocityX = 0;

        if (Power.EFFECT_PUSH.equals(effectName)) {
            this.effectVelocityX = pushDir * 11.0;
        } else if (Power.EFFECT_UNCONSCIOUS.equals(effectName)) {
            this.velocityY = -12.5;
            this.onGround = false;
            this.movingLeft = false;
            this.movingRight = false;
            this.wantsToJump = false;
        }
    }

    /**
     * Upgrades a player statistic using earned points.
     * @param statName statistic name (speed, jump, kick, or survival)
     * @throws HeadSoccerException if the configuration, input, or file operation fails
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
     * Returns upgrade cost for the given stat level.
     * @param currentLevel current statistic level
     * @return point cost for the next upgrade level
     */
    public static int getUpgradeCost(int currentLevel) {
        if (currentLevel >= MAX_STAT_LEVEL) return 0;
        int cost = BASE_UPGRADE_COST;
        for (int i = 1; i < currentLevel; i++) {
            cost = cost * 2;
        }
        return cost;
    }

    /**
     * Returns level of stat by name, or -1 if unknown.
     * @param statName statistic name (speed, jump, kick, or survival)
     * @return level of stat by name, or -1 if unknown
     */
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
     * Sets the four player statistics and points.
     * @param speed speed statistic level
     * @param jump jump statistic level
     * @param kick kick statistic level
     * @param survival survival statistic level
     * @param points available upgrade points
     */
    public void setProgress(int speed, int jump, int kick, int survival, int points) {
        this.speedStat = clampStat(speed);
        this.jumpStat = clampStat(jump);
        this.kickStat = clampStat(kick);
        this.survivalStat = clampStat(survival);
        this.points = Math.max(0, points);
        this.currentHealth = getMaxHealth();
    }

    /**
     * Clamps a stat level within [1, MAX_STAT_LEVEL].
     * @param level statistic level to clamp
     * @return level clamped between 1 and 10
     */
    private int clampStat(int level) {
        return Math.max(1, Math.min(MAX_STAT_LEVEL, level));
    }

    /**
     * Adds earned match points.
     * @param earned points earned to add
     */
    public void addPoints(int earned) {
        if (earned > 0) {
            this.points += earned;
        }
    }

    /**
     * Resets position and movement flags after a goal.
     */
    public void reset() {
        positionX = initialX;
        positionY = initialY;
        velocityX = 0;
        velocityY = 0;
        onGround = true;
        movingLeft = false;
        movingRight = false;
        wantsToJump = false;
        kickAnimTicks = 0;
        facingDirection = initialFacingDirection;
    }

    /**
     * Resets full match state for testing or new round.
     */
    public void resetForMatch() {
        reset();
        currentHealth = getMaxHealth();
        powerBar = 0.0;
        stunTicks = 0;
        activeEffect = null;
        effectTicks = 0;
        effectVelocityX = 0;
        if (power != null) {
            power.deactivate();
        }
    }

    /** Returns true if player is not stunned and not unconscious. */
    public boolean canAct() {
        return stunTicks <= 0 && !Power.EFFECT_UNCONSCIOUS.equals(activeEffect);
    }

    /** Returns true if player is currently stunned. */
    public boolean isStunned() { return stunTicks > 0; }

    /** Returns kick damage multiplier: 1 + 0.10 * (Kick - 1). */
    public double getKickMultiplier() {
        return 1.0 + 0.10 * (kickStat - 1);
    }

    /** Returns max health: 100 * (1 + 0.10 * (Survival - 1)). */
    public double getMaxHealth() {
        return Math.round(100.0 * (1.0 + 0.10 * (survivalStat - 1)));
    }

    /**
     * Returns effective horizontal speed considering Speed stat and Electrocuted effect.
     * @return effective horizontal speed considering Speed stat and Electrocuted effect
     */
    public double getEffectiveMoveSpeed() {
        double speed = BASE_MOVE_SPEED * (1.0 + 0.10 * (speedStat - 1));
        if (Power.EFFECT_ELECTROCUTE.equals(activeEffect)) {
            speed *= 0.5;
        }
        return speed;
    }

    /** Returns effective jump velocity calibrated to clear goal crossbar cleanly. */
    public double getEffectiveJumpForce() {
        return BASE_JUMP_FORCE * (1.0 + 0.02 * (jumpStat - 1));
    }

    /**
     * 
     * @param x horizontal coordinate
     */
    public void setPositionX(double x) { this.positionX = x; }

    /** Returns current centre X position. */
    public double getPositionX() { return positionX; }

    /** Returns current centre Y position. */
    public double getPositionY() { return positionY; }

    /**
     * Returns current X coordinate of the kicking foot.
     * @return current X coordinate of the kicking foot
     */
    public double getFootX() {
        if (kickAnimTicks > 0) {
            double progress = Math.sin((kickAnimTicks / 12.0) * Math.PI);
            return positionX + facingDirection * progress * 16.0;
        }
        return positionX;
    }

    /**
     * Returns current Y coordinate of the kicking foot.
     * @return current Y coordinate of the kicking foot
     */
    public double getFootY() {
        if (kickAnimTicks > 0) {
            double progress = Math.sin((kickAnimTicks / 12.0) * Math.PI);
            return positionY + HEAD_RADIUS - progress * 6.0;
        }
        return positionY + HEAD_RADIUS;
    }

    /** Returns current kick animation rotation angle in radians. */
    public double getKickAngle() {
        if (kickAnimTicks <= 0) return 0.0;
        double progress = Math.sin((kickAnimTicks / 12.0) * Math.PI);
        return -facingDirection * progress * 0.9;
    }

    /**
     * Returns current effective horizontal velocity.
     * @return current effective horizontal velocity
     */
    public double getVelocityX() {
        if (stunTicks > 0 || Power.EFFECT_UNCONSCIOUS.equals(activeEffect)) {
            return velocityX;
        }
        if (movingLeft && !movingRight) return -getEffectiveMoveSpeed();
        if (movingRight && !movingLeft) return getEffectiveMoveSpeed();
        return velocityX;
    }

    /** Returns current vertical velocity. */
    public double getVelocityY() { return velocityY; }

    /** Returns true if the player is standing on the ground. */
    public boolean isOnGround() { return onGround; }

    /** Returns display color. */
    public Color getColor() { return color; }

    /** Returns player display name. */
    public String getName() { return name; }

    /** Returns selected country character name. */
    public String getCountry() { return country; }

    /** Returns assigned country Power instance. */
    public Power getPower() { return power; }

    /** Returns facing direction (+1 right, -1 left). */
    public int getFacingDirection() { return facingDirection; }

    /** Returns current health points. */
    public double getCurrentHealth() { return currentHealth; }

    /** Returns current power bar charge percentage (0.0 to 100.0). */
    public double getPowerBar() { return powerBar; }

    /**
     * 
     * @param value power bar value between 0 and 100
     */
    public void setPowerBar(double value) { this.powerBar = Math.max(0.0, Math.min(100.0, value)); }

    /** Returns currently active rival status effect name, or null. */
    public String getActiveEffect() { return activeEffect; }

    /** Returns current Speed statistic level. */
    public int getSpeedStat() { return speedStat; }

    /** Returns current Jump statistic level. */
    public int getJumpStat() { return jumpStat; }

    /** Returns current Kick statistic level. */
    public int getKickStat() { return kickStat; }

    /** Returns current Survival statistic level. */
    public int getSurvivalStat() { return survivalStat; }

    /** Returns accumulated upgrade points. */
    public int getPoints() { return points; }
}
