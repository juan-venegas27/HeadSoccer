package domain;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Central game model for DOPOSoccer (Version 2: Domain + Persistence).
 * Coordinates players, AI machine player, ball, field, scoreboard, clock,
 * power shot collisions, match modes (time / sudden death / goals),
 * progression points, and match serialization.
 */
public class HeadSoccer implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int MODE_PVP = 0;
    public static final int MODE_PVM = 1;

    public static final int MATCH_BY_TIME  = 0;
    public static final int MATCH_BY_GOALS = 1;

    public static final int POINTS_WINNER = 100;
    public static final int POINTS_LOSER  = 25;

    /** Number of physics ticks to freeze action after a goal (~1.2 seconds at 60 FPS). */
    private static final int GOAL_FREEZE_TICKS = 75;

    private final Player     player1;
    private final Player     player2;
    private final Ball       ball;
    private final Field      field;
    private final Scoreboard scoreboard;

    private final int modality;
    private final int matchType;
    private final int targetGoals;

    private int     remainingTime;
    private boolean suddenDeath;
    private boolean running;
    private boolean paused;
    private boolean matchFinished;
    private boolean pointsAwarded;
    private int     goalFreezeTicks;
    private String  lastGoalScorer;
    private int     lastPointsP1;
    private int     lastPointsP2;

    /**
     * Convenience constructor creating a standard 3-minute PvP match.
     */
    public HeadSoccer(int fieldWidth, int fieldHeight) {
        this(fieldWidth, fieldHeight, MODE_PVP, null,
                Power.COUNTRY_KOREA, Power.COUNTRY_CAMEROON,
                MATCH_BY_TIME, 3, 5);
    }

    /**
     * Full constructor configuring field resolution, modality, machine profile,
     * character countries, and match rules (by time or by goals).
     */
    public HeadSoccer(int fieldWidth, int fieldHeight, int modality, String machineProfile,
                      String p1Country, String p2Country, int matchType, int minutes, int targetGoals) {
        this.field       = new Field(fieldWidth, fieldHeight);
        this.modality    = (modality == MODE_PVM) ? MODE_PVM : MODE_PVP;
        this.matchType   = (matchType == MATCH_BY_GOALS) ? MATCH_BY_GOALS : MATCH_BY_TIME;
        this.targetGoals = Math.max(1, targetGoals);

        int groundY = field.getGroundY();
        int leftGoalLine = field.getLeftGoalLine();
        int rightGoalLine = field.getRightGoalLine();

        Power power1 = Power.createForCountry(p1Country);
        Power power2 = Power.createForCountry(p2Country);

        double p1x = leftGoalLine + 120.0;
        double p2x = rightGoalLine - 120.0;
        double py  = groundY - Player.HEAD_RADIUS;

        this.player1 = new Player(p1x, py, new Color(40, 100, 220), "Jugador 1", power1, 1);
        if (this.modality == MODE_PVM) {
            String prof = (machineProfile != null) ? machineProfile : MachinePlayer.PROFILE_OFENSIVA;
            this.player2 = new MachinePlayer(p2x, py, new Color(210, 50, 50),
                    "Máquina (" + prof + ")", power2, -1, prof);
        } else {
            this.player2 = new Player(p2x, py, new Color(210, 50, 50), "Jugador 2", power2, -1);
        }

        this.ball = new Ball(fieldWidth / 2.0, groundY - Ball.RADIUS * 4);

        this.scoreboard      = new Scoreboard();
        this.remainingTime   = Math.max(10, minutes * 60);
        this.suddenDeath     = false;
        this.running         = false;
        this.paused          = false;
        this.matchFinished   = false;
        this.pointsAwarded   = false;
        this.goalFreezeTicks = 0;
        this.lastGoalScorer  = null;
        this.lastPointsP1    = 0;
        this.lastPointsP2    = 0;
    }

    /**
     * Validates configuration parameters and throws HeadSoccerException if invalid.
     */
    public static void validateConfiguration(int fieldWidth, int fieldHeight, int minutes, int targetGoals)
            throws HeadSoccerException {
        if (fieldWidth < 600 || fieldHeight < 400 || minutes <= 0 || targetGoals <= 0) {
            throw new HeadSoccerException(HeadSoccerException.INVALID_CONFIG);
        }
    }

    /** Starts the game loop. */
    public void start() {
        running       = true;
        paused        = false;
        matchFinished = false;
    }

    /** Pauses or unpauses the game. */
    public void setPaused(boolean pause) {
        this.paused = pause;
    }

    /** Advances game physics by one tick (~60 FPS). */
    public void update() {
        if (!running || paused) return;

        if (goalFreezeTicks > 0) {
            goalFreezeTicks--;
            if (goalFreezeTicks == 0) {
                if (checkPostGoalVictory()) {
                    return;
                }
                resetAfterGoal();
            }
            return;
        }

        if (player2 instanceof MachinePlayer) {
            ((MachinePlayer) player2).decideAction(ball, player1, field, scoreboard);
        }

        int leftGoalLine  = field.getLeftGoalLine();
        int rightGoalLine = field.getRightGoalLine();

        player1.update(leftGoalLine + Player.HEAD_RADIUS, rightGoalLine - Player.HEAD_RADIUS, field.getGroundY());
        player2.update(leftGoalLine + Player.HEAD_RADIUS, rightGoalLine - Player.HEAD_RADIUS, field.getGroundY());

        resolvePlayerPlayerCollision();

        ball.move(field);

        resolvePlayerBallCollision(player1, player2);
        resolvePlayerBallCollision(player2, player1);

        checkGoals();
    }

    /**
     * Decrements remaining match time by one second (in timed matches).
     * Triggers Sudden Death if tied at 00:00, or finishes the match if someone leads.
     */
    public void tickSecond() {
        if (!running || paused) return;
        if (matchType != MATCH_BY_TIME || suddenDeath) return;

        if (remainingTime > 0) {
            remainingTime--;
        }
        if (remainingTime == 0) {
            if (scoreboard.getScorePlayer1() == scoreboard.getScorePlayer2()) {
                suddenDeath = true;
            } else {
                finish();
            }
        }
    }

    private void resolvePlayerPlayerCollision() {
        double minDist = Player.HEAD_RADIUS * 2;
        double dx = player2.getPositionX() - player1.getPositionX();
        double dy = Math.abs(player2.getPositionY() - player1.getPositionY());
        if (Math.abs(dx) < minDist && dy < Player.HEAD_RADIUS * 1.8) {
            double sign = (dx >= 0) ? 1.0 : -1.0;
            double overlap = minDist - Math.abs(dx);
            player1.setPositionX(player1.getPositionX() - sign * overlap / 2.0);
            player2.setPositionX(player2.getPositionX() + sign * overlap / 2.0);
        }
    }

    private void resolvePlayerBallCollision(Player player, Player opponent) {
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

            // Rule 5: Power shot hits rival -> apply damage and effect
            if (ball.hasActivePowerShot() && ball.getPowerShotOwner() == opponent) {
                Power shot = ball.getActivePowerShot();
                double damage = shot.calculateDamage(opponent.getKickMultiplier());
                player.takeDamage(damage);
                int pushTowardOwnGoal = (player == player1) ? -1 : 1;
                player.applyEffect(shot.getEffectName(), shot.getEffectDurationTicks(), pushTowardOwnGoal);
                ball.clearPowerShotImpact();
            }

            // Rule 4: Player with active aura touches ball -> execute power shot and remove aura
            if (player.getPower().isActive()) {
                player.executePowerShot(ball);
                return;
            }

            double pvx = player.getVelocityX();
            double pvy = player.getVelocityY();

            double relVx = ball.getVelocityX() - pvx;
            double relVy = ball.getVelocityY() - pvy;

            double velAlongNormal = relVx * nx + relVy * ny;

            if (velAlongNormal < 0) {
                double restitution = 0.80;
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

    private void checkGoals() {
        if (goalFreezeTicks > 0) return;

        if (field.getLeftGoal().containsBall(ball)) {
            scoreboard.scoreGoalPlayer2();
            player1.onGoalConceded(); // Rule 1: +1/4 power bar when conceding goal
            goalFreezeTicks = GOAL_FREEZE_TICKS;
            lastGoalScorer  = player2.getName();
            ball.clearAllPowerShotEffects(); // Rule 6: goal counts, no damage/effect if untouched
        } else if (field.getRightGoal().containsBall(ball)) {
            scoreboard.scoreGoalPlayer1();
            player2.onGoalConceded();
            goalFreezeTicks = GOAL_FREEZE_TICKS;
            lastGoalScorer  = player1.getName();
            ball.clearAllPowerShotEffects();
        }
    }

    private boolean checkPostGoalVictory() {
        if (suddenDeath) {
            finish();
            return true;
        }
        if (matchType == MATCH_BY_GOALS) {
            if (scoreboard.getScorePlayer1() >= targetGoals ||
                scoreboard.getScorePlayer2() >= targetGoals) {
                finish();
                return true;
            }
        }
        return false;
    }

    private void resetAfterGoal() {
        player1.reset();
        player2.reset();
        ball.reset();
        lastGoalScorer = null;
    }

    /**
     * Finishes the game and awards match points (+100 winner, +25 loser).
     */
    public void finish() {
        running       = false;
        matchFinished = true;
        awardMatchPoints();
    }

    private void awardMatchPoints() {
        if (pointsAwarded) return;
        pointsAwarded = true;

        int s1 = scoreboard.getScorePlayer1();
        int s2 = scoreboard.getScorePlayer2();

        if (s1 > s2) {
            lastPointsP1 = POINTS_WINNER;
            lastPointsP2 = POINTS_LOSER;
        } else if (s2 > s1) {
            lastPointsP1 = POINTS_LOSER;
            lastPointsP2 = POINTS_WINNER;
        } else {
            lastPointsP1 = POINTS_LOSER;
            lastPointsP2 = POINTS_LOSER;
        }

        player1.addPoints(lastPointsP1);
        player2.addPoints(lastPointsP2);
    }

    // ==================== PERSISTENCE ====================

    /** Saves the complete state of this match to the given file. */
    public void save(File file) throws HeadSoccerException {
        if (file == null) {
            throw new HeadSoccerException(HeadSoccerException.NULL_FILE);
        }
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(this);
        } catch (IOException e) {
            Log.record(HeadSoccerException.SAVE_ERROR, e);
            throw new HeadSoccerException(HeadSoccerException.SAVE_ERROR + " (" + e.getMessage() + ")", e);
        }
    }

    /** Opens and restores a previously saved match from the given file. */
    public static HeadSoccer open(File file) throws HeadSoccerException {
        if (file == null) {
            throw new HeadSoccerException(HeadSoccerException.NULL_FILE);
        }
        if (!file.exists()) {
            throw new HeadSoccerException(HeadSoccerException.FILE_NOT_FOUND);
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = ois.readObject();
            if (!(obj instanceof HeadSoccer)) {
                throw new HeadSoccerException(HeadSoccerException.OPEN_ERROR);
            }
            return (HeadSoccer) obj;
        } catch (HeadSoccerException e) {
            throw e;
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            Log.record(HeadSoccerException.OPEN_ERROR, e);
            throw new HeadSoccerException(HeadSoccerException.OPEN_ERROR + " (" + e.getMessage() + ")", e);
        }
    }

    /** Saves both players' persistent progress (stats and points). */
    public static void saveProgress(File file, int[] p1Progress, int[] p2Progress) throws HeadSoccerException {
        if (file == null) {
            throw new HeadSoccerException(HeadSoccerException.NULL_FILE);
        }
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(new int[][]{p1Progress, p2Progress});
        } catch (IOException e) {
            Log.record("Error al guardar el progreso de los jugadores", e);
            throw new HeadSoccerException(HeadSoccerException.SAVE_ERROR, e);
        }
    }

    /** Loads both players' persistent progress (stats and points). */
    public static int[][] loadProgress(File file) throws HeadSoccerException {
        if (file == null || !file.exists()) {
            throw new HeadSoccerException(HeadSoccerException.FILE_NOT_FOUND);
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = ois.readObject();
            if (!(obj instanceof int[][])) {
                throw new HeadSoccerException(HeadSoccerException.OPEN_ERROR);
            }
            return (int[][]) obj;
        } catch (HeadSoccerException e) {
            throw e;
        } catch (IOException | ClassNotFoundException e) {
            Log.record("Error al cargar el progreso de los jugadores", e);
            throw new HeadSoccerException(HeadSoccerException.OPEN_ERROR, e);
        }
    }

    public boolean isRunning() { return running; }
    public boolean isMatchFinished() { return matchFinished; }
    public boolean isSuddenDeath() { return suddenDeath; }
    public boolean isGoalFreeze() { return goalFreezeTicks > 0; }
    public String getLastGoalScorer() { return lastGoalScorer; }
    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
    public Ball getBall() { return ball; }
    public Field getField() { return field; }
    public Scoreboard getScoreboard() { return scoreboard; }
    public int getRemainingTime() { return remainingTime; }
    public int getModality() { return modality; }
    public int getMatchType() { return matchType; }
    public int getTargetGoals() { return targetGoals; }
    public int getLastPointsPlayer1() { return lastPointsP1; }
    public int getLastPointsPlayer2() { return lastPointsP2; }
}
