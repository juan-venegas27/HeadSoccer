package test;

import controller.GameController;
import domain.*;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** End-to-end acceptance test suite covering full gameplay, AI, persistence, and error handling workflows. */
public class HeadSoccerAcceptanceTest {

    /** Verifies a timed match ending in a draw enters sudden death and ends on the next goal. */
    @Test
    public void testTimedMatchTieEntersSuddenDeathAndFinishesOnGoldenGoal() {
        GameController controller = new GameController();
        controller.setModality(HeadSoccer.MODE_PVP);
        controller.setMatchType(HeadSoccer.MATCH_BY_TIME);
        controller.setMatchMinutes(1);
        controller.setP1Country(Power.COUNTRY_KOREA);
        controller.setP2Country(Power.COUNTRY_CAMEROON);

        HeadSoccer match = assertStartsMatch(controller);
        controller.startMatch();

        match.getScoreboard().scoreGoalPlayer1();
        match.getScoreboard().scoreGoalPlayer2();
        for (int i = 0; i < 60; i++) {
            controller.tickSecond();
        }
        assertTrue("Tied match must enter sudden death", match.isSuddenDeath());
        assertTrue("Match must continue during sudden death", controller.isMatchRunning());

        match.getBall().setPosition(match.getField().getRightGoalLine() + 4.0,
                match.getField().getGoalTop() + 45.0);
        match.getBall().setVelocity(10.0, 0.0);
        for (int i = 0; i < 90; i++) {
            controller.updateMatch();
        }
        assertFalse("Sudden death must finish after golden goal freeze", controller.isMatchRunning());
        assertTrue(controller.isMatchFinished());
        assertEquals(HeadSoccer.POINTS_WINNER, match.getLastPointsPlayer1());
        assertEquals(HeadSoccer.POINTS_LOSER, match.getLastPointsPlayer2());
    }

    /** Verifies a goal-target match includes power shot stun, recovery, and victory upon reaching target goals. */
    @Test
    public void testGoalTargetMatchWithPowerShotStunAndVictory() {
        HeadSoccer match = new HeadSoccer(
                Field.RES_MEDIUM_WIDTH, Field.RES_MEDIUM_HEIGHT,
                HeadSoccer.MODE_PVP, null,
                Power.COUNTRY_CAMEROON, Power.COUNTRY_USA,
                HeadSoccer.MATCH_BY_GOALS, 3, 2
        );
        match.start();

        Player p1 = match.getPlayer1();
        Player p2 = match.getPlayer2();
        Ball ball = match.getBall();

        p1.setPowerBar(100.0);
        p1.activatePower();
        ball.setPosition(p1.getPositionX() + 18.0, p1.getPositionY());
        p2.setPositionX(p1.getPositionX() + 70.0);
        for (int i = 0; i < 20; i++) {
            match.update();
        }
        assertTrue("Cameroon Rayo shot must damage or electrocute opponent",
                p2.getCurrentHealth() < p2.getMaxHealth() || p2.getActiveEffect() != null);

        p2.takeDamage(500.0);
        assertTrue("Zero health must stun the opponent", p2.isStunned());
        for (int i = 0; i < Player.STUN_DURATION_TICKS; i++) {
            p2.update(100, 800, match.getField().getGroundY());
        }
        assertFalse("Opponent must recover after stun duration", p2.isStunned());
        assertEquals(p2.getMaxHealth(), p2.getCurrentHealth(), 0.01);

        match.getScoreboard().scoreGoalPlayer1();
        ball.setPosition(match.getField().getRightGoalLine() + 4.0, match.getField().getGoalTop() + 45.0);
        ball.setVelocity(10.0, 0.0);
        for (int i = 0; i < 90; i++) {
            match.update();
        }
        assertTrue("Reaching target goals must finish match", match.isMatchFinished());
        assertEquals(2, match.getScoreboard().getScorePlayer1());
    }

    /** Verifies Player vs Machine matches run cleanly across all four machine AI profiles. */
    @Test
    public void testPlayerVsMachineAcrossAllFourProfiles() {
        String[] profiles = {
            MachinePlayer.PROFILE_TORPE,
            MachinePlayer.PROFILE_OFENSIVA,
            MachinePlayer.PROFILE_DEFENSIVA,
            MachinePlayer.PROFILE_EXPERTA
        };
        for (String profile : profiles) {
            GameController controller = new GameController();
            controller.setModality(HeadSoccer.MODE_PVM);
            controller.setMachineProfile(profile);
            HeadSoccer match = assertStartsMatch(controller);
            assertTrue(controller.isPlayer2Machine());
            assertEquals(profile, ((MachinePlayer) match.getPlayer2()).getProfile());

            controller.startMatch();
            for (int i = 0; i < 60; i++) {
                controller.updateMatch();
            }
            assertTrue("PvM match must stay running for profile " + profile, controller.isMatchRunning());
        }
    }

    /** Verifies pausing mid-match, saving to disk, loading back, and resuming without losing state. */
    @Test
    public void testSaveAndResumeMatchPreservesFullState() {
        File saveFile = new File("test_acc_match.tmp");
        try {
            GameController saver = new GameController();
            saver.setResolution(Field.RES_LARGE_WIDTH, Field.RES_LARGE_HEIGHT);
            saver.setModality(HeadSoccer.MODE_PVM);
            saver.setMachineProfile(MachinePlayer.PROFILE_DEFENSIVA);
            HeadSoccer match = assertStartsMatch(saver);

            saver.startMatch();
            match.getScoreboard().scoreGoalPlayer2();
            saver.tickSecond();
            saver.setPaused(true);
            saver.saveMatch(match, saveFile);

            GameController loader = new GameController();
            HeadSoccer restored = loader.loadMatch(saveFile);
            assertEquals(Field.RES_LARGE_WIDTH, loader.getFieldWidth());
            assertEquals(1, restored.getScoreboard().getScorePlayer2());
            assertEquals(179, restored.getRemainingTime());
            assertTrue(loader.isPlayer2Machine());
            assertEquals(MachinePlayer.PROFILE_DEFENSIVA, ((MachinePlayer) restored.getPlayer2()).getProfile());

            loader.setPaused(false);
            loader.updateMatch();
            assertTrue(loader.isMatchRunning());
        } catch (HeadSoccerException ex) {
            fail("Unexpected save/load acceptance error: " + ex.getMessage());
        } finally {
            saveFile.delete();
        }
    }

    /** Verifies finishing matches earns points, upgrades stats cleanly, and reloads persisted progress. */
    @Test
    public void testMatchRewardsStatUpgradesAndProgressPersistence() {
        File progressFile = new File("progress.dat");
        byte[] backup = readIfExists(progressFile);
        try {
            progressFile.delete();
            GameController controller = new GameController();
            for (int m = 0; m < 5; m++) {
                HeadSoccer match = assertStartsMatch(controller);
                controller.startMatch();
                match.getScoreboard().scoreGoalPlayer1();
                controller.finishMatch();
            }
            assertEquals(500, controller.getP1Progress()[4]);
            controller.upgradePlayerStat(true, "speed");
            assertEquals(2, controller.getP1Progress()[0]);
            assertEquals(0, controller.getP1Progress()[4]);

            GameController reloaded = new GameController();
            assertEquals(2, reloaded.getP1Progress()[0]);
            assertEquals(0, reloaded.getP1Progress()[4]);
        } catch (HeadSoccerException ex) {
            fail("Stat upgrade workflow failed unexpectedly: " + ex.getMessage());
        } finally {
            restoreOrDelete(progressFile, backup);
        }
    }

    /** Verifies invalid configuration and corrupted save files raise descriptive HeadSoccerException errors. */
    @Test
    public void testInvalidConfigAndCorruptedFileErrorHandling() {
        File badFile = new File("test_acc_corrupt.tmp");
        try {
            HeadSoccer.validateConfiguration(400, 300, 3, 5);
            fail("Expected invalid configuration to throw HeadSoccerException");
        } catch (HeadSoccerException ex) {
            assertEquals(HeadSoccerException.INVALID_CONFIG, ex.getMessage());
        }

        try {
            try (FileOutputStream fos = new FileOutputStream(badFile)) {
                fos.write("corrupted".getBytes(StandardCharsets.UTF_8));
            }
            GameController controller = new GameController();
            controller.loadMatch(badFile);
            fail("Expected corrupted file to throw HeadSoccerException");
        } catch (HeadSoccerException ex) {
            assertTrue(ex.getMessage().startsWith(HeadSoccerException.OPEN_ERROR));
        } catch (Exception ioEx) {
            fail("Unexpected IO error creating test file: " + ioEx.getMessage());
        } finally {
            badFile.delete();
        }
    }

    /**
     * Starts a new match through the controller and fails the test if creation throws an exception.
     * @param controller active controller coordinating match configuration
     * @return created HeadSoccer match instance
     */
    private HeadSoccer assertStartsMatch(GameController controller) {
        try {
            return controller.startNewMatch();
        } catch (HeadSoccerException ex) {
            fail("Match creation failed: " + ex.getMessage());
            return null;
        }
    }

    /**
     * Reads raw bytes from an existing file before temporarily modifying it.
     * @param file target file on disk
     * @return file bytes if present, or null
     */
    private byte[] readIfExists(File file) {
        try {
            return file.exists() ? Files.readAllBytes(file.toPath()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Restores a backed-up file or deletes the test artifact when no prior file existed.
     * @param file target file on disk
     * @param backup saved original file bytes, or null
     */
    private void restoreOrDelete(File file, byte[] backup) {
        try {
            if (backup != null) {
                Files.write(file.toPath(), backup);
            } else {
                file.delete();
            }
        } catch (Exception ignored) {
            file.delete();
        }
    }
}
