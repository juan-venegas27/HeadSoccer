package domain;

import java.awt.Color;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** AI-controlled player delegating decisions to an extensible strategy registry. */
public class MachinePlayer extends Player implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Factory interface producing MachineStrategy instances for registered profiles. */
    public interface StrategyFactory extends Serializable {

        /** Creates a new strategy instance for a machine player. */
        MachineStrategy create();
    }

    public static final String PROFILE_TORPE = "Torpe";
    public static final String PROFILE_OFENSIVA = "Ofensiva";
    public static final String PROFILE_DEFENSIVA = "Defensiva";
    public static final String PROFILE_EXPERTA = "Experta";

    public static final String[] PROFILES = {
        PROFILE_TORPE, PROFILE_OFENSIVA, PROFILE_DEFENSIVA, PROFILE_EXPERTA
    };

    private static final Map<String, StrategyFactory> STRATEGIES = new LinkedHashMap<>();

    static {
        registerStrategy(PROFILE_TORPE, TorpeStrategy::new);
        registerStrategy(PROFILE_OFENSIVA, OfensivaStrategy::new);
        registerStrategy(PROFILE_DEFENSIVA, DefensivaStrategy::new);
        registerStrategy(PROFILE_EXPERTA, ExpertaStrategy::new);
    }

    private final String profile;
    private final MachineStrategy strategy;
    private int decisionTicks;

    /**
     * Creates a machine player with the requested AI profile.
     * @param x horizontal coordinate
     * @param y vertical coordinate
     * @param color player kit color
     * @param name identifying name
     * @param power assigned special power
     * @param facingDirection initial facing direction (+1 right, -1 left)
     * @param profile machine player profile name
     */
    public MachinePlayer(double x, double y, Color color, String name,
                         Power power, int facingDirection, String profile) {
        super(x, y, color, name, power, facingDirection);
        this.profile = normalizeProfileName(profile);
        this.strategy = createStrategyForProfile(this.profile);
        this.decisionTicks = 0;
    }

    /**
     * Registers or replaces a machine AI strategy under the given profile name.
     * @param profileName machine profile display name
     * @param factory factory producing the strategy instance
     */
    public static synchronized void registerStrategy(String profileName, StrategyFactory factory) {
        if (profileName != null && factory != null) {
            STRATEGIES.put(profileName, factory);
        }
    }

    /**
     * Unregisters a machine AI strategy by profile name.
     * @param profileName machine profile display name
     */
    public static synchronized void unregisterStrategy(String profileName) {
        if (profileName != null) {
            STRATEGIES.remove(profileName);
        }
    }

    /** Returns all registered machine profile names in registration order. */
    public static synchronized String[] getAvailableProfiles() {
        return STRATEGIES.keySet().toArray(new String[0]);
    }

    /**
     * Normalizes a profile label to a registered profile constant or Torpe fallback.
     * @param profile requested machine profile name
     * @return normalized profile name
     */
    public static synchronized String normalizeProfileName(String profile) {
        if (profile == null) {
            return PROFILE_TORPE;
        }
        String lower = profile.trim().toLowerCase();
        for (String key : STRATEGIES.keySet()) {
            if (key.toLowerCase().equals(lower)) {
                return key;
            }
        }
        if (lower.contains("torp")) return PROFILE_TORPE;
        if (lower.contains("ofen")) return PROFILE_OFENSIVA;
        if (lower.contains("def")) return PROFILE_DEFENSIVA;
        if (lower.contains("exp")) return PROFILE_EXPERTA;
        return PROFILE_TORPE;
    }

    /**
     * Instantiates the MachineStrategy mapped to the normalized profile name.
     * @param profileName normalized machine profile name
     * @return strategy instance for the profile
     */
    private static synchronized MachineStrategy createStrategyForProfile(String profileName) {
        StrategyFactory factory = STRATEGIES.get(profileName);
        if (factory != null) {
            return factory.create();
        }
        return new TorpeStrategy();
    }

    /**
     * Delegates AI decision making for the current simulation tick.
     * @param ball active match ball
     * @param opponent opposing player
     * @param field playing field boundaries
     * @param scoreboard match scoreboard
     */
    public void decideAction(Ball ball, Player opponent, Field field, Scoreboard scoreboard) {
        if (!canAct() || ball == null || field == null) {
            setMovingLeft(false);
            setMovingRight(false);
            return;
        }
        decisionTicks++;
        strategy.decideAction(this, ball, opponent, field, scoreboard);
    }

    /**
     * Returns perceived ball coordinates, tracking decoys while confused.
     * @param ball active match ball
     * @return two-element array [ballX, ballY]
     */
    public double[] getPerceivedBallPosition(Ball ball) {
        double ballX = ball.getPositionX();
        double ballY = ball.getPositionY();
        StatusEffect effect = getActiveStatusEffect();
        if (effect != null && effect.confusesVision()) {
            List<double[]> decoys = ball.getDecoyBalls();
            if (!decoys.isEmpty()) {
                double[] d = decoys.get((decisionTicks / 20) % decoys.size());
                ballX = d[0];
                ballY = d[1];
            }
        }
        return new double[]{ballX, ballY};
    }

    /**
     * Sets horizontal movement flags toward a target X coordinate within tolerance.
     * @param targetX horizontal target coordinate
     * @param tolerance dead-zone distance around targetX
     */
    public void moveTowards(double targetX, double tolerance) {
        double diff = targetX - getPositionX();
        if (diff < -tolerance) {
            setMovingLeft(true);
            setMovingRight(false);
        } else if (diff > tolerance) {
            setMovingRight(true);
            setMovingLeft(false);
        } else {
            setMovingLeft(false);
            setMovingRight(false);
        }
    }

    /**
     * Returns true if the ball is within kicking range of the machine player.
     * @param ball active match ball
     * @return true if the ball is within kicking range
     */
    public boolean isBallNear(Ball ball) {
        double dx = ball.getPositionX() - getPositionX();
        double dy = ball.getPositionY() - getPositionY();
        return Math.sqrt(dx * dx + dy * dy) < (HEAD_RADIUS + Ball.RADIUS + 24);
    }

    /**
     * Returns true if the opposing player is within close kicking proximity.
     * @param opponent opposing player
     * @return true if the opponent is within kicking reach
     */
    public boolean isOpponentNear(Player opponent) {
        if (opponent == null) return false;
        return Math.abs(opponent.getPositionX() - getPositionX()) < 55
                && Math.abs(opponent.getPositionY() - getPositionY()) < 45;
    }

    /** Returns the elapsed AI decision tick counter. */
    public int getDecisionTicks() {
        return decisionTicks;
    }

    /** Returns the current machine profile name. */
    public String getProfile() {
        return profile;
    }
}
