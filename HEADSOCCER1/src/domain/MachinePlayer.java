package src.domain;

import java.awt.Color;
import java.io.Serializable;
import java.util.List;
import java.util.Random;

/**
 * Simple AI-controlled player for Player vs Machine in DOPOSoccer.
 * Supports the four required profiles:
 * Torpe, Ofensiva, Defensiva, and Experta.
 */
public class MachinePlayer extends Player implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String PROFILE_TORPE     = "Torpe";
    public static final String PROFILE_OFENSIVA  = "Ofensiva";
    public static final String PROFILE_DEFENSIVA = "Defensiva";
    public static final String PROFILE_EXPERTA   = "Experta";

    public static final String[] PROFILES = {
        PROFILE_TORPE, PROFILE_OFENSIVA, PROFILE_DEFENSIVA, PROFILE_EXPERTA
    };

    private final String profile;
    private final Random random;
    private int          decisionTicks;

    /**
     * Creates a MachinePlayer with the specified profile.
     *
     * @param x               initial centre X
     * @param y               initial centre Y
     * @param color           display color
     * @param name            display name
     * @param power           country power
     * @param facingDirection +1 right, -1 left
     * @param profile         AI profile name
     */
    public MachinePlayer(double x, double y, Color color, String name,
                         Power power, int facingDirection, String profile) {
        super(x, y, color, name, power, facingDirection);
        this.profile       = normalizeProfile(profile);
        this.random        = new Random();
        this.decisionTicks = 0;
    }

    private String normalizeProfile(String p) {
        if (p == null) return PROFILE_OFENSIVA;
        String lower = p.trim().toLowerCase();
        if (lower.contains("torp")) return PROFILE_TORPE;
        if (lower.contains("def"))  return PROFILE_DEFENSIVA;
        if (lower.contains("exp"))  return PROFILE_EXPERTA;
        return PROFILE_OFENSIVA;
    }

    /**
     * Chooses simple movement, jump, kick, and power actions for this tick.
     */
    public void decideAction(Ball ball, Player opponent, Field field, Scoreboard scoreboard) {
        if (!canAct() || ball == null || field == null) {
            setMovingLeft(false);
            setMovingRight(false);
            return;
        }

        decisionTicks++;

        double ballX = ball.getPositionX();
        double ballY = ball.getPositionY();
        if (Power.EFFECT_CONFUSION.equals(getActiveEffect())) {
            List<double[]> decoys = ball.getDecoyBalls();
            if (!decoys.isEmpty()) {
                double[] d = decoys.get((decisionTicks / 20) % decoys.size());
                ballX = d[0];
                ballY = d[1];
            }
        }

        switch (profile) {
            case PROFILE_TORPE:
                actTorpe(ball, opponent);
                break;
            case PROFILE_DEFENSIVA:
                actDefensiva(ballX, ballY, ball, opponent, field);
                break;
            case PROFILE_EXPERTA:
                actExperta(ballX, ballY, ball, opponent, field, scoreboard);
                break;
            case PROFILE_OFENSIVA:
            default:
                actOfensiva(ballX, ballY, ball, opponent);
                break;
        }
    }

    private void actTorpe(Ball ball, Player opponent) {
        if (decisionTicks % 20 == 0) {
            int choice = random.nextInt(3);
            setMovingLeft(choice == 0);
            setMovingRight(choice == 1);
            if (random.nextDouble() < 0.25) jump();
            if (random.nextDouble() < 0.30) hit(ball, opponent);
            if (getPowerBar() >= 100.0 && random.nextDouble() < 0.4) activatePower();
        }
    }

    private void actOfensiva(double ballX, double ballY, Ball ball, Player opponent) {
        moveTowards(ballX + 20.0, 10.0);
        if (ballY < getPositionY() - 25 && Math.abs(ballX - getPositionX()) < 75) {
            jump();
        }
        if (getPowerBar() >= 100.0 && !getPower().isActive()) {
            activatePower();
        }
        if (isBallNear(ball) || isOpponentNear(opponent)) {
            hit(ball, opponent);
        }
    }

    private void actDefensiva(double ballX, double ballY, Ball ball, Player opponent, Field field) {
        double midX = field.getWidth() / 2.0;
        if (ballX > midX) {
            moveTowards(ballX + 16.0, 8.0);
            if (ballY < getPositionY() - 25 && Math.abs(ballX - getPositionX()) < 75) {
                jump();
            }
            if (getPowerBar() >= 100.0 && !getPower().isActive()) {
                activatePower();
            }
            if (isBallNear(ball)) {
                hit(ball, opponent);
            }
        } else {
            moveTowards(field.getRightGoalLine() - 70.0, 10.0);
        }
    }

    private void actExperta(double ballX, double ballY, Ball ball, Player opponent,
                            Field field, Scoreboard scoreboard) {
        int myScore  = (scoreboard != null) ? scoreboard.getScorePlayer2() : 0;
        int oppScore = (scoreboard != null) ? scoreboard.getScorePlayer1() : 0;
        boolean ballComing = ball.getVelocityX() > 2.0 && ballX > field.getWidth() / 2.0;
        boolean rivalStunned = (opponent != null && opponent.isStunned());

        if (ballComing || (myScore > oppScore && !rivalStunned)) {
            actDefensiva(ballX, ballY, ball, opponent, field);
        } else {
            actOfensiva(ballX, ballY, ball, opponent);
        }
    }

    private void moveTowards(double targetX, double tolerance) {
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

    private boolean isBallNear(Ball ball) {
        double dx = ball.getPositionX() - getPositionX();
        double dy = ball.getPositionY() - getPositionY();
        return Math.sqrt(dx * dx + dy * dy) < (HEAD_RADIUS + Ball.RADIUS + 24);
    }

    private boolean isOpponentNear(Player opponent) {
        if (opponent == null) return false;
        return Math.abs(opponent.getPositionX() - getPositionX()) < 55
                && Math.abs(opponent.getPositionY() - getPositionY()) < 45;
    }

    /** @return current AI profile */
    public String getProfile() {
        return profile;
    }
}
