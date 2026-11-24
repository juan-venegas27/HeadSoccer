package controller;

import domain.Field;
import domain.HeadSoccer;
import domain.HeadSoccerException;
import domain.Log;
import domain.MachinePlayer;
import domain.Player;
import domain.Power;

import java.awt.Color;
import java.io.File;

/** Coordinates configuration, progression persistence, and match lifecycle between view and model. */
public class GameController {

    private static final File PROGRESS_FILE = new File("progress.dat");

    private HeadSoccer model;
    private int fieldWidth = Field.RES_MEDIUM_WIDTH;
    private int fieldHeight = Field.RES_MEDIUM_HEIGHT;
    private int modality = HeadSoccer.MODE_PVP;
    private String machineProfile = MachinePlayer.PROFILE_TORPE;
    private String p1Country = Power.COUNTRY_KOREA;
    private String p2Country = Power.COUNTRY_CAMEROON;
    private int matchType = HeadSoccer.MATCH_BY_TIME;
    private int matchMinutes = 3;
    private int targetGoals = 5;

    private int[] p1Progress = {1, 1, 1, 1, 0};
    private int[] p2Progress = {1, 1, 1, 1, 0};

    /**
     * Initializes controller defaults and loads saved player progression.
     */
    public GameController() {
        loadProgress();
    }

    /**
     * Loads saved player progression from disk if available.
     */
    public void loadProgress() {
        if (!PROGRESS_FILE.exists()) {
            return;
        }
        try {
            int[][] loaded = HeadSoccer.loadProgress(PROGRESS_FILE);
            if (loaded != null && loaded.length == 2) {
                p1Progress = loaded[0];
                p2Progress = loaded[1];
            }
        } catch (HeadSoccerException e) {
            Log.record(e);
        }
    }

    /**
     * Saves current player statistics and points to disk.
     */
    public void saveProgress() {
        try {
            HeadSoccer.saveProgress(PROGRESS_FILE, p1Progress, p2Progress);
        } catch (HeadSoccerException e) {
            Log.record(e);
        }
    }

    /**
     * Upgrades one player statistic, updates progression, and saves changes.
     * @param isPlayer1 true for player 1, false for player 2
     * @param statKey statistic key (speed, jump, kick, or survival)
     * @throws HeadSoccerException if the configuration, input, or file operation fails
     */
    public void upgradePlayerStat(boolean isPlayer1, String statKey) throws HeadSoccerException {
        int[] progress = isPlayer1 ? p1Progress : p2Progress;
        Player temp = new Player(0, 0, Color.BLUE, "Temp",
                Power.createForCountry(Power.COUNTRY_KOREA), 1);
        temp.setProgress(progress[0], progress[1], progress[2], progress[3], progress[4]);
        temp.upgradeStat(statKey);
        progress[0] = temp.getSpeedStat();
        progress[1] = temp.getJumpStat();
        progress[2] = temp.getKickStat();
        progress[3] = temp.getSurvivalStat();
        progress[4] = temp.getPoints();
        saveProgress();
    }

    /**
     * Validates settings, builds a new match with current progress, and stores it.
     * @return newly created match model
     * @throws HeadSoccerException if the configuration, input, or file operation fails
     */
    public HeadSoccer startNewMatch() throws HeadSoccerException {
        HeadSoccer.validateConfiguration(fieldWidth, fieldHeight, matchMinutes, targetGoals);
        HeadSoccer match = new HeadSoccer(
                fieldWidth, fieldHeight,
                modality, machineProfile,
                p1Country, p2Country,
                matchType, matchMinutes, targetGoals
        );
        match.getPlayer1().setProgress(p1Progress[0], p1Progress[1], p1Progress[2], p1Progress[3], p1Progress[4]);
        match.getPlayer2().setProgress(p2Progress[0], p2Progress[1], p2Progress[2], p2Progress[3], p2Progress[4]);
        this.model = match;
        return match;
    }

    /**
     * Loads a saved match from disk and sets it as active.
     * @param file target file on disk
     * @return loaded match model
     * @throws HeadSoccerException if the configuration, input, or file operation fails
     */
    public HeadSoccer loadMatch(File file) throws HeadSoccerException {
        HeadSoccer loaded = HeadSoccer.open(file);
        this.model = loaded;
        this.fieldWidth = loaded.getField().getWidth();
        this.fieldHeight = loaded.getField().getHeight();
        return loaded;
    }

    /**
     * Saves the specified or active match to disk.
     * @param match match model instance
     * @param file target file on disk
     * @throws HeadSoccerException if the configuration, input, or file operation fails
     */
    public void saveMatch(HeadSoccer match, File file) throws HeadSoccerException {
        HeadSoccer target = (match != null) ? match : this.model;
        if (target != null) {
            target.save(file);
        }
    }

    /**
     * Finalizes the active match and synchronizes earned player progression.
     */
    public void finishMatch() {
        if (model == null) {
            return;
        }
        if (!model.isMatchFinished()) {
            model.finish();
        }
        syncProgressFromModel(model);
    }

    /**
     * Copies stats and points from the finished match into stored progression.
     * @param match match model instance
     */
    private void syncProgressFromModel(HeadSoccer match) {
        if (match == null) {
            return;
        }
        p1Progress = new int[]{
            match.getPlayer1().getSpeedStat(),
            match.getPlayer1().getJumpStat(),
            match.getPlayer1().getKickStat(),
            match.getPlayer1().getSurvivalStat(),
            match.getPlayer1().getPoints()
        };
        p2Progress = new int[]{
            match.getPlayer2().getSpeedStat(),
            match.getPlayer2().getJumpStat(),
            match.getPlayer2().getKickStat(),
            match.getPlayer2().getSurvivalStat(),
            match.getPlayer2().getPoints()
        };
        saveProgress();
    }

    /** Starts the active match model. */
    public void startMatch() {
        if (model != null) {
            model.start();
        }
    }

    /**
     * Pauses or resumes the active match and clears pending player movement.
     * @param paused true to pause, false to resume
     */
    public void setPaused(boolean paused) {
        if (model != null) {
            model.setPaused(paused);
            clearPlayerMovement();
        }
    }

    /** Advances simulation physics by one frame tick. */
    public void updateMatch() {
        if (model != null) {
            model.update();
        }
    }

    /** Decrements match time by one second. */
    public void tickSecond() {
        if (model != null) {
            model.tickSecond();
        }
    }

    /** Returns true when the active match is currently running. */
    public boolean isMatchRunning() {
        return model != null && model.isRunning();
    }

    /** Returns true when the active match has ended. */
    public boolean isMatchFinished() {
        return model != null && model.isMatchFinished();
    }

    /** Returns true when player 2 is controlled by the machine. */
    public boolean isPlayer2Machine() {
        return model != null && model.getPlayer2() instanceof MachinePlayer;
    }

    /**
     * Resets directional movement flags for both players.
     */
    public void clearPlayerMovement() {
        if (model == null) {
            return;
        }
        model.getPlayer1().setMovingLeft(false);
        model.getPlayer1().setMovingRight(false);
        model.getPlayer2().setMovingLeft(false);
        model.getPlayer2().setMovingRight(false);
    }

    /**
     * Updates player 1 leftward movement state.
     * @param moving true while directional movement key is held
     */
    public void setPlayer1MovingLeft(boolean moving) {
        if (model != null) {
            model.getPlayer1().setMovingLeft(moving);
        }
    }

    /**
     * Updates player 1 rightward movement state.
     * @param moving true while directional movement key is held
     */
    public void setPlayer1MovingRight(boolean moving) {
        if (model != null) {
            model.getPlayer1().setMovingRight(moving);
        }
    }

    /** Triggers player 1 jump action. */
    public void player1Jump() {
        if (model != null) {
            model.getPlayer1().jump();
        }
    }

    /** Triggers player 1 kick or opponent strike action. */
    public void player1Hit() {
        if (model != null) {
            model.getPlayer1().hit(model.getBall(), model.getPlayer2());
        }
    }

    /** Activates player 1 special power when charged. */
    public void player1ActivatePower() {
        if (model != null) {
            model.getPlayer1().activatePower();
        }
    }

    /**
     * Updates player 2 leftward movement state.
     * @param moving true while directional movement key is held
     */
    public void setPlayer2MovingLeft(boolean moving) {
        if (model != null) {
            model.getPlayer2().setMovingLeft(moving);
        }
    }

    /**
     * Updates player 2 rightward movement state.
     * @param moving true while directional movement key is held
     */
    public void setPlayer2MovingRight(boolean moving) {
        if (model != null) {
            model.getPlayer2().setMovingRight(moving);
        }
    }

    /** Triggers player 2 jump action. */
    public void player2Jump() {
        if (model != null) {
            model.getPlayer2().jump();
        }
    }

    /** Triggers player 2 kick or opponent strike action. */
    public void player2Hit() {
        if (model != null) {
            model.getPlayer2().hit(model.getBall(), model.getPlayer1());
        }
    }

    /** Activates player 2 special power when charged. */
    public void player2ActivatePower() {
        if (model != null) {
            model.getPlayer2().activatePower();
        }
    }

    /** Returns the active match model. */
    public HeadSoccer getModel() {
        return model;
    }

    /**
     * Sets the configured field resolution dimensions.
     * @param width width in pixels
     * @param height height in pixels
     */
    public void setResolution(int width, int height) {
        this.fieldWidth = width;
        this.fieldHeight = height;
    }

    /** Returns configured field width in pixels. */
    public int getFieldWidth() {
        return fieldWidth;
    }

    /** Returns configured field height in pixels. */
    public int getFieldHeight() {
        return fieldHeight;
    }

    /** Returns selected match modality. */
    public int getModality() {
        return modality;
    }

    /**
     * Sets match modality constant.
     * @param modality match modality constant (PvP or PvM)
     */
    public void setModality(int modality) {
        this.modality = modality;
    }

    /** Returns selected machine difficulty profile. */
    public String getMachineProfile() {
        return machineProfile;
    }

    /**
     * Sets machine difficulty profile name.
     * @param machineProfile machine player profile name
     */
    public void setMachineProfile(String machineProfile) {
        this.machineProfile = machineProfile;
    }

    /** Returns selected country for player 1. */
    public String getP1Country() {
        return p1Country;
    }

    /**
     * Sets country name for player 1.
     * @param p1Country country name for player 1
     */
    public void setP1Country(String p1Country) {
        this.p1Country = p1Country;
    }

    /** Returns selected country for player 2. */
    public String getP2Country() {
        return p2Country;
    }

    /**
     * Sets country name for player 2.
     * @param p2Country country name for player 2
     */
    public void setP2Country(String p2Country) {
        this.p2Country = p2Country;
    }

    /** Returns match victory type constant. */
    public int getMatchType() {
        return matchType;
    }

    /**
     * Sets match victory type constant.
     * @param matchType match mode constant (by time or by goals)
     */
    public void setMatchType(int matchType) {
        this.matchType = matchType;
    }

    /** Returns configured match duration in minutes. */
    public int getMatchMinutes() {
        return matchMinutes;
    }

    /**
     * Sets match duration in minutes.
     * @param matchMinutes match duration in minutes
     */
    public void setMatchMinutes(int matchMinutes) {
        this.matchMinutes = matchMinutes;
    }

    /** Returns configured goal target. */
    public int getTargetGoals() {
        return targetGoals;
    }

    /**
     * Sets goal target count.
     * @param targetGoals goal count required to win
     */
    public void setTargetGoals(int targetGoals) {
        this.targetGoals = targetGoals;
    }

    /** Returns player 1 progression array. */
    public int[] getP1Progress() {
        return p1Progress;
    }

    /** Returns player 2 progression array. */
    public int[] getP2Progress() {
        return p2Progress;
    }

    /** Returns maximum statistic level allowed for upgrades. */
    public int getMaxStatLevel() {
        return Player.MAX_STAT_LEVEL;
    }

    /**
     * Returns point cost to upgrade from the given statistic level.
     * @param currentLevel current statistic level
     * @return point cost for the next upgrade level
     */
    public int getStatUpgradeCost(int currentLevel) {
        return Player.getUpgradeCost(currentLevel);
    }
}
