package domain;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Main game model coordinating the match, players, ball, field, and persistence. */
public class HeadSoccer implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int MODE_PVP = 0;
    public static final int MODE_PVM = 1;

    public static final int MATCH_BY_TIME = 0;
    public static final int MATCH_BY_GOALS = 1;

    public static final int POINTS_WINNER = 100;
    public static final int POINTS_LOSER = 25;

    private static final int GOAL_FREEZE_TICKS = 75;

    private final Player player1;
    private final Player player2;
    private final Ball ball;
    private final Field field;
    private final Scoreboard scoreboard;
    private final List<FieldElement> fieldElements;

    private final int modality;
    private final int matchType;
    private final int targetGoals;
    private MatchRule matchRule;

    private int remainingTime;
    private boolean suddenDeath;
    private boolean running;
    private boolean paused;
    private boolean matchFinished;
    private boolean pointsAwarded;
    private int goalFreezeTicks;
    private String lastGoalScorer;
    private int lastPointsP1;
    private int lastPointsP2;

    /**
     * Creates a standard PvP match with default settings.
     * @param fieldWidth field width in pixels
     * @param fieldHeight field height in pixels
     */
    public HeadSoccer(int fieldWidth, int fieldHeight) {
        this(fieldWidth, fieldHeight, MODE_PVP, null,
                Power.COUNTRY_KOREA, Power.COUNTRY_CAMEROON,
                MATCH_BY_TIME, 3, 5);
    }

    /**
     * Creates a match with full game and player configuration.
     * @param fieldWidth field width in pixels
     * @param fieldHeight field height in pixels
     * @param modality match modality constant (PvP or PvM)
     * @param machineProfile machine player profile name
     * @param p1Country country name for player 1
     * @param p2Country country name for player 2
     * @param matchType match mode constant (by time or by goals)
     * @param minutes match duration in minutes
     * @param targetGoals goal count required to win
     */
    public HeadSoccer(int fieldWidth, int fieldHeight, int modality, String machineProfile,
                      String p1Country, String p2Country, int matchType, int minutes, int targetGoals) {
        this.field = new Field(fieldWidth, fieldHeight);
        this.modality = (modality == MODE_PVM) ? MODE_PVM : MODE_PVP;
        this.matchType = (matchType == MATCH_BY_GOALS) ? MATCH_BY_GOALS : MATCH_BY_TIME;
        this.targetGoals = Math.max(1, targetGoals);

        int groundY = field.getGroundY();
        int leftGoalLine = field.getLeftGoalLine();
        int rightGoalLine = field.getRightGoalLine();

        Power power1 = Power.createForCountry(p1Country);
        Power power2 = Power.createForCountry(p2Country);

        double p1x = leftGoalLine + 120.0;
        double p2x = rightGoalLine - 120.0;
        double py = groundY - Player.HEAD_RADIUS;

        this.player1 = new Player(p1x, py, new Color(40, 100, 220), "Jugador 1", power1, 1);
        if (this.modality == MODE_PVM) {
            String prof = MachinePlayer.normalizeProfileName(machineProfile);
            this.player2 = new MachinePlayer(p2x, py, new Color(210, 50, 50),
                    "Máquina (" + prof + ")", power2, -1, prof);
        } else {
            this.player2 = new Player(p2x, py, new Color(210, 50, 50), "Jugador 2", power2, -1);
        }

        this.ball = new Ball(fieldWidth / 2.0, groundY - Ball.RADIUS * 4);

        this.scoreboard = new Scoreboard();
        this.fieldElements = new ArrayList<>();
        this.matchRule = (this.matchType == MATCH_BY_GOALS)
                ? new GoalsMatchRule(this.targetGoals)
                : new TimeMatchRule();
        this.remainingTime = Math.max(10, minutes * 60);
        this.suddenDeath = false;
        this.running = false;
        this.paused = false;
        this.matchFinished = false;
        this.pointsAwarded = false;
        this.goalFreezeTicks = 0;
        this.lastGoalScorer = null;
        this.lastPointsP1 = 0;
        this.lastPointsP2 = 0;
    }

    /**
     * Validates match configuration parameters.
     * @param fieldWidth field width in pixels
     * @param fieldHeight field height in pixels
     * @param minutes match duration in minutes
     * @param targetGoals goal count required to win
     * @throws HeadSoccerException if the configuration, input, or file operation fails
     */
    public static void validateConfiguration(int fieldWidth, int fieldHeight, int minutes, int targetGoals)
            throws HeadSoccerException {
        if (fieldWidth < 600 || fieldHeight < 400 || minutes <= 0 || targetGoals <= 0) {
            throw new HeadSoccerException(HeadSoccerException.INVALID_CONFIG);
        }
    }

    /** Starts the game loop. */
    public void start() {
        running = true;
        paused = false;
        matchFinished = false;
    }

    /**
     * Pauses or unpauses the game.
     * @param pause true to pause, false to resume
     */
    public void setPaused(boolean pause) {
        this.paused = pause;
    }

    /**
     * Advances game physics by one tick (~60 FPS).
     */
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

        int leftGoalLine = field.getLeftGoalLine();
        int rightGoalLine = field.getRightGoalLine();

        player1.update(leftGoalLine + Player.HEAD_RADIUS, rightGoalLine - Player.HEAD_RADIUS, field.getGroundY());
        player2.update(leftGoalLine + Player.HEAD_RADIUS, rightGoalLine - Player.HEAD_RADIUS, field.getGroundY());

        resolvePlayerPlayerCollision();

        ball.move(field);

        resolvePlayerBallCollision(player1, player2);
        resolvePlayerBallCollision(player2, player1);

        updateFieldElements();

        checkGoals();
    }

    /**
     * Decrements the match clock and checks end-of-time conditions.
     */
    public void tickSecond() {
        if (!running || paused) {
            return;
        }
        if (matchRule.onSecondElapsed(this)) {
            finish();
        }
    }

    /**
     * Prevents horizontal overlap between both players.
     */
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

    /**
     * Resolves head/foot ball collision and active power shot effects.
     * @param player player instance to draw or update
     * @param opponent opposing player
     */
    private void resolvePlayerBallCollision(Player player, Player opponent) {
        double bx = ball.getPositionX();
        double by = ball.getPositionY();
        double px = player.getPositionX();
        double py = player.getPositionY();

        double cx = px;
        double cy = py;
        double cr = Player.HEAD_RADIUS;
        double closestDistSq = (bx - px) * (bx - px) + (by - py) * (by - py);

        double fx = player.getFootX();
        double fy = player.getFootY();
        double footDistSq = (bx - fx) * (bx - fx) + (by - fy) * (by - fy);
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

            ball.setPosition(bx + nx * (minDist - dist), by + ny * (minDist - dist));
            applyPowerShotHit(player, opponent);
            if (player.getPower().isActive()) {
                player.executePowerShot(ball);
                return;
            }
            applyBallBounceImpulse(player, nx, ny);
        }
    }

    /**
     * Applies active power shot damage and status effect when hitting the opponent.
     * @param player player instance to draw or update
     * @param opponent opposing player
     */
    private void applyPowerShotHit(Player player, Player opponent) {
        if (ball.hasActivePowerShot() && ball.getPowerShotOwner() == opponent) {
            Power shot = ball.getActivePowerShot();
            player.takeDamage(shot.calculateDamage(opponent.getKickMultiplier()));
            int pushTowardOwnGoal = (player == player1) ? -1 : 1;
            player.applyEffect(shot.createStatusEffect(pushTowardOwnGoal));
            ball.clearPowerShotImpact();
        }
    }

    /**
     * Computes and applies elastic collision impulse between player and ball.
     * @param player player instance to draw or update
     * @param nx collision normal X component
     * @param ny collision normal Y component
     */
    private void applyBallBounceImpulse(Player player, double nx, double ny) {
        double relVx = ball.getVelocityX() - player.getVelocityX();
        double relVy = ball.getVelocityY() - player.getVelocityY();
        double velAlongNormal = relVx * nx + relVy * ny;

        if (velAlongNormal < 0) {
            double impulseMag = -1.55 * velAlongNormal;
            double newVx = ball.getVelocityX() + impulseMag * nx;
            double newVy = ball.getVelocityY() + impulseMag * ny;
            if (ny < -0.3 && newVy > -3.5) {
                newVy = -3.8;
            }
            ball.setVelocity(newVx, newVy);
        } else if (ny < -0.5 && ball.getVelocityY() > -2.0) {
            ball.setVelocity(ball.getVelocityX(), -3.0);
        }
    }

    /**
     * Checks whether a circle overlaps the given field element.
     * @param x center horizontal coordinate
     * @param y center vertical coordinate
     * @param radius circle radius in pixels
     * @param element target field element
     * @return true when the circle intersects the element bounds
     */
    private boolean isOverlappingElement(double x, double y, double radius, FieldElement element) {
        double dx = x - element.getPositionX();
        double dy = y - element.getPositionY();
        double minDist = radius + element.getRadius();
        return dx * dx + dy * dy <= minDist * minDist;
    }

    /**
     * Updates active field elements and triggers ball or player touch callbacks.
     */
    private void updateFieldElements() {
        fieldElements.removeIf(e -> e == null || !e.isActive());
        for (FieldElement element : new ArrayList<>(fieldElements)) {
            if (!element.isActive()) {
                continue;
            }
            element.update(this);
            if (element.isActive() && isOverlappingElement(
                    ball.getPositionX(), ball.getPositionY(), Ball.RADIUS, element)) {
                element.onBallTouch(ball, this);
            }
            if (element.isActive() && isOverlappingElement(
                    player1.getPositionX(), player1.getPositionY(), Player.HEAD_RADIUS, element)) {
                element.onPlayerTouch(player1, this);
            }
            if (element.isActive() && isOverlappingElement(
                    player2.getPositionX(), player2.getPositionY(), Player.HEAD_RADIUS, element)) {
                element.onPlayerTouch(player2, this);
            }
        }
        fieldElements.removeIf(e -> e == null || !e.isActive());
    }

    /**
     * Checks if the ball entered either goal and updates scores and freeze state.
     */
    private void checkGoals() {
        if (goalFreezeTicks > 0) return;

        if (field.getLeftGoal().containsBall(ball)) {
            scoreboard.scoreGoalPlayer2();
            player1.onGoalConceded();
            goalFreezeTicks = GOAL_FREEZE_TICKS;
            lastGoalScorer = player2.getName();
            ball.clearAllPowerShotEffects();
        } else if (field.getRightGoal().containsBall(ball)) {
            scoreboard.scoreGoalPlayer1();
            player2.onGoalConceded();
            goalFreezeTicks = GOAL_FREEZE_TICKS;
            lastGoalScorer = player1.getName();
            ball.clearAllPowerShotEffects();
        }
    }

    /**
     * Checks post-goal victory conditions using the active match rule.
     * @return true if the match ended after the goal
     */
    private boolean checkPostGoalVictory() {
        if (matchRule.shouldFinishAfterGoal(this)) {
            finish();
            return true;
        }
        return false;
    }

    /** Resets players and ball to starting positions after a goal freeze. */
    private void resetAfterGoal() {
        player1.reset();
        player2.reset();
        ball.reset();
        lastGoalScorer = null;
    }

    /** Finishes the match and awards player points. */
    public void finish() {
        running = false;
        matchFinished = true;
        awardMatchPoints();
    }

    /**
     * Awards match points once when the game concludes.
     */
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

    /**
     * Saves the complete state of this match to the given file.
     * @param file target file on disk
     * @throws HeadSoccerException if the configuration, input, or file operation fails
     */
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

    /**
     * Opens and restores a previously saved match from the given file.
     * @param file target file on disk
     * @return restored match model
     * @throws HeadSoccerException if the configuration, input, or file operation fails
     */
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
        } catch (Exception e) {
            Log.record(HeadSoccerException.OPEN_ERROR, e);
            throw new HeadSoccerException(HeadSoccerException.OPEN_ERROR + " (" + e.getMessage() + ")", e);
        }
    }

    /**
     * Saves both players' persistent progress (stats and points).
     * @param file target file on disk
     * @param p1Progress progression array for player 1
     * @param p2Progress progression array for player 2
     * @throws HeadSoccerException if the configuration, input, or file operation fails
     */
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

    /**
     * Loads both players' persistent progress (stats and points).
     * @param file target file on disk
     * @return two-row array with player 1 and player 2 progression
     * @throws HeadSoccerException if the configuration, input, or file operation fails
     */
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
        } catch (Exception e) {
            Log.record("Error al cargar el progreso de los jugadores", e);
            throw new HeadSoccerException(HeadSoccerException.OPEN_ERROR, e);
        }
    }

    /**
     * Registers an active dynamic element into the arena.
     * @param element field element to add
     */
    public void addFieldElement(FieldElement element) {
        if (element != null && !fieldElements.contains(element)) {
            fieldElements.add(element);
        }
    }

    /**
     * Removes the given dynamic element from the arena.
     * @param element field element to remove
     */
    public void removeFieldElement(FieldElement element) {
        fieldElements.remove(element);
    }

    /** Removes all dynamic elements from the arena. */
    public void clearFieldElements() { fieldElements.clear(); }

    /** Returns an unmodifiable view of active field elements. */
    public List<FieldElement> getFieldElements() { return Collections.unmodifiableList(fieldElements); }

    /** Decrements remaining match time by one second when positive. */
    public void decrementRemainingTime() {
        if (remainingTime > 0) {
            remainingTime--;
        }
    }

    /** Enables sudden death golden-goal overtime mode. */
    public void activateSuddenDeath() { this.suddenDeath = true; }

    /** Returns the active match victory and clock rule. */
    public MatchRule getMatchRule() { return matchRule; }

    /**
     * Replaces the active match victory and clock rule.
     * @param rule custom or built-in match rule
     */
    public void setMatchRule(MatchRule rule) {
        if (rule != null) {
            this.matchRule = rule;
        }
    }

    /** Returns true if the match is currently running. */
    public boolean isRunning() { return running; }

    /** Returns true if the match has finished. */
    public boolean isMatchFinished() { return matchFinished; }

    /** Returns true if the match is in sudden death overtime. */
    public boolean isSuddenDeath() { return suddenDeath; }

    /** Returns true while goal freeze countdown is active. */
    public boolean isGoalFreeze() { return goalFreezeTicks > 0; }

    /** Returns name of the player who scored the latest goal, or null. */
    public String getLastGoalScorer() { return lastGoalScorer; }

    /** Returns player 1 instance. */
    public Player getPlayer1() { return player1; }

    /** Returns player 2 (human or machine) instance. */
    public Player getPlayer2() { return player2; }

    /** Returns the soccer ball. */
    public Ball getBall() { return ball; }

    /** Returns the playing field. */
    public Field getField() { return field; }

    /** Returns the match scoreboard. */
    public Scoreboard getScoreboard() { return scoreboard; }

    /** Returns remaining match time in seconds. */
    public int getRemainingTime() { return remainingTime; }

    /** Returns match modality (PvP or PvM). */
    public int getModality() { return modality; }

    /** Returns match type code reported by the active rule. */
    public int getMatchType() { return (matchRule != null) ? matchRule.getMatchTypeCode() : matchType; }

    /** Returns target goal limit for goal-based matches. */
    public int getTargetGoals() { return targetGoals; }

    /** Returns points awarded to player 1 at match end. */
    public int getLastPointsPlayer1() { return lastPointsP1; }

    /** Returns points awarded to player 2 at match end. */
    public int getLastPointsPlayer2() { return lastPointsP2; }
}
