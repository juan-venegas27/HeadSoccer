package test;

import domain.*;

import static org.junit.Assert.*;
import org.junit.Test;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Unit test suite for DOPOSoccer. */
public class HeadSoccerTest {

    /** Verifies that Player 1 cannot move past the left goal line boundary. */
    @Test
    public void testPlayerMovementAndBounds() {
        HeadSoccer game = new HeadSoccer(900, 540);
        Player p1 = game.getPlayer1();
        int leftGoalLine = game.getField().getLeftGoalLine();

        p1.setMovingLeft(true);
        for (int i = 0; i < 200; i++) {
            p1.update(leftGoalLine + Player.HEAD_RADIUS, 900, game.getField().getGroundY());
        }
        p1.setMovingLeft(false);

        assertTrue("Player 1 must not cross past the left goal line",
                p1.getPositionX() >= leftGoalLine + Player.HEAD_RADIUS);
    }

    /** Verifies fast jump arc timing, goal-height clearance, and floor landing. */
    @Test
    public void testPlayerJumpAndGravity() {
        HeadSoccer game = new HeadSoccer(900, 540);
        Player p1 = game.getPlayer1();
        int groundY = game.getField().getGroundY();
        int goalTop = game.getField().getGoalTop();
        double initialY = groundY - Player.HEAD_RADIUS;

        assertTrue("Player should start on ground", p1.isOnGround());

        p1.jump();
        p1.update(0, 900, groundY);
        assertFalse("Player should be airborne after jump", p1.isOnGround());

        double minHeadY = initialY;
        int ticksToLand = 1;
        for (int i = 0; i < 60; i++) {
            p1.update(0, 900, groundY);
            ticksToLand++;
            minHeadY = Math.min(minHeadY, p1.getPositionY());
            if (p1.isOnGround()) break;
        }

        assertTrue("Fast jump must complete in under 40 ticks (~0.65s)", ticksToLand < 40);
        assertTrue("Player head top must clear goal height slightly", (minHeadY - Player.HEAD_RADIUS) < goalTop);
        assertTrue("Jump must not soar overly high above goal crossbar", minHeadY > goalTop - 30);
        assertEquals("Player Y should be on the floor after landing", initialY, p1.getPositionY(), 0.1);
    }

    /** Verifies that both players separate without overlapping on collision. */
    @Test
    public void testPlayerPlayerCollision() {
        HeadSoccer game = new HeadSoccer(900, 540);
        game.start();
        Player p1 = game.getPlayer1();
        Player p2 = game.getPlayer2();

        p1.setMovingRight(true);
        p2.setMovingLeft(true);

        for (int i = 0; i < 200; i++) {
            game.update();
        }

        assertTrue("Player 1 must remain on left of Player 2", p1.getPositionX() < p2.getPositionX());
        double dist = p2.getPositionX() - p1.getPositionX();
        assertTrue("Players must not overlap horizontally", dist >= Player.HEAD_RADIUS * 2 - 0.5);
    }

    /** Verifies ball gravity acceleration and ground bounce containment. */
    @Test
    public void testBallGravityAndBounce() {
        HeadSoccer game = new HeadSoccer(900, 540);
        Ball ball = game.getBall();
        int groundY = game.getField().getGroundY();

        ball.setPosition(450, 200);
        ball.setVelocity(0, 0);

        game.start();
        for (int i = 0; i < 30; i++) {
            ball.move(game.getField());
        }

        assertTrue("Ball should fall due to gravity", ball.getPositionY() > 200);

        for (int i = 0; i < 60; i++) {
            ball.move(game.getField());
        }

        assertTrue("Ball must not penetrate the ground", ball.getPositionY() <= groundY - Ball.RADIUS + 0.1);
    }

    /** Verifies ceiling rebound, crossbar roof bounce, and non-scoring flight above left goal. */
    @Test
    public void testBallCeilingCrossbarAndOverGoal() {
        HeadSoccer game = new HeadSoccer(900, 540);
        Ball ball = game.getBall();

        ball.setPosition(450, 25);
        ball.setVelocity(0, -10);
        ball.move(game.getField());
        ball.move(game.getField());
        assertTrue("Ball must bounce off ceiling", ball.getPositionY() >= Ball.RADIUS);
        assertTrue("Ball vertical velocity should reflect downwards", ball.getVelocityY() > 0);

        int leftGoalLine = game.getField().getLeftGoalLine();
        int goalTop = game.getField().getGoalTop();
        ball.setPosition(leftGoalLine / 2.0, goalTop - 30);
        ball.setVelocity(0, 8);
        for (int i = 0; i < 10; i++) {
            ball.move(game.getField());
        }
        assertTrue("Ball must bounce on top of crossbar", ball.getPositionY() <= goalTop - Ball.RADIUS + 1.0);

        game.start();
        ball.setPosition(leftGoalLine + 15, goalTop - 80);
        ball.setVelocity(-12, 0);
        boolean passedLeftGoalLine = false;
        for (int i = 0; i < 15; i++) {
            game.update();
            if (ball.getPositionX() < leftGoalLine) {
                passedLeftGoalLine = true;
            }
            assertFalse("Ball above crossbar must never count as a goal",
                    game.getField().getLeftGoal().containsBall(ball));
        }
        assertTrue("Ball above crossbar must pass freely past leftGoalLine", passedLeftGoalLine);
        assertEquals("Home score must remain 0 when ball goes over goal", 0, game.getScoreboard().getScorePlayer1());
        assertEquals("Away score must remain 0 when ball goes over goal", 0, game.getScoreboard().getScorePlayer2());
    }

    /** Verifies head collision separation and upward bounce impulse on the ball. */
    @Test
    public void testPlayerBallCollisionHead() {
        HeadSoccer game = new HeadSoccer(900, 540);
        game.start();
        Player p1 = game.getPlayer1();
        Ball ball = game.getBall();

        ball.setPosition(p1.getPositionX(), p1.getPositionY() - 45);
        ball.setVelocity(0, 6);

        game.update();

        double dx = ball.getPositionX() - p1.getPositionX();
        double dy = ball.getPositionY() - p1.getPositionY();
        double dist = Math.sqrt(dx * dx + dy * dy);
        double minDist = Player.HEAD_RADIUS + Ball.RADIUS;

        assertTrue("Ball must be pushed outside player head", dist >= minDist - 0.5);
        assertTrue("Ball must receive an upward bounce impulse", ball.getVelocityY() < 0);
    }

    /** Verifies foot collision pushes the ball outside the player's foot radius. */
    @Test
    public void testPlayerBallCollisionFoot() {
        HeadSoccer game = new HeadSoccer(900, 540);
        game.start();
        Player p1 = game.getPlayer1();
        Ball ball = game.getBall();

        double footX = p1.getFootX();
        double footY = p1.getFootY();

        ball.setPosition(footX - 25, footY);
        ball.setVelocity(5, 0);

        game.update();

        double dx = ball.getPositionX() - footX;
        double dy = ball.getPositionY() - footY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        double minDist = Player.FOOT_RADIUS + Ball.RADIUS;

        assertTrue("Ball must be pushed outside player foot", dist >= minDist - 0.5);
    }

    /** Verifies kicking the ball launches it without damaging a nearby rival. */
    @Test
    public void testPlayerHit() {
        HeadSoccer game = new HeadSoccer(900, 540);
        Player p1 = game.getPlayer1();
        Player p2 = game.getPlayer2();
        Ball ball = game.getBall();

        p2.setPositionX(p1.getPositionX() + 35);
        ball.setPosition(p1.getPositionX() + 35, p1.getPositionY());
        ball.setVelocity(0, 0);

        p1.hit(ball, p2);

        assertTrue("Hit should give positive forward velocity", ball.getVelocityX() > 5.0);
        assertTrue("Hit should give upward velocity", ball.getVelocityY() < 0);
        assertEquals("Kick touching ball must not damage opponent", p2.getMaxHealth(), p2.getCurrentHealth(), 0.01);
    }

    /** Verifies scoring into the left goal increments Player 2's score and triggers freeze. */
    @Test
    public void testLeftGoalDetection() {
        HeadSoccer game = new HeadSoccer(900, 540);
        game.start();
        Ball ball = game.getBall();
        int leftGoalLine = game.getField().getLeftGoalLine();
        int goalTop = game.getField().getGoalTop();

        ball.setPosition(leftGoalLine - 2, goalTop + 40);
        ball.setVelocity(-10, 0);

        for (int i = 0; i < 5; i++) {
            game.update();
        }

        assertEquals(0, game.getScoreboard().getScorePlayer1());
        assertEquals(1, game.getScoreboard().getScorePlayer2());
        assertTrue(game.isGoalFreeze());
    }

    /** Verifies scoring into the right goal increments Player 1's score and triggers freeze. */
    @Test
    public void testRightGoalDetection() {
        HeadSoccer game = new HeadSoccer(900, 540);
        game.start();
        Ball ball = game.getBall();
        int rightGoalLine = game.getField().getRightGoalLine();
        int goalTop = game.getField().getGoalTop();

        ball.setPosition(rightGoalLine + 2, goalTop + 40);
        ball.setVelocity(10, 0);

        for (int i = 0; i < 5; i++) {
            game.update();
        }

        assertEquals(1, game.getScoreboard().getScorePlayer1());
        assertEquals(0, game.getScoreboard().getScorePlayer2());
        assertTrue(game.isGoalFreeze());
    }

    /** Verifies post-goal freeze prevents duplicate goal counts and resets the ball to center. */
    @Test
    public void testGoalFreezeAndReset() {
        HeadSoccer game = new HeadSoccer(900, 540);
        game.start();
        Ball ball = game.getBall();
        int leftGoalLine = game.getField().getLeftGoalLine();
        int goalTop = game.getField().getGoalTop();

        ball.setPosition(leftGoalLine - 2, goalTop + 40);
        ball.setVelocity(-10, 0);

        for (int i = 0; i < 5; i++) {
            game.update();
        }
        assertEquals(1, game.getScoreboard().getScorePlayer2());
        assertTrue(game.isGoalFreeze());

        for (int i = 0; i < 85; i++) {
            game.update();
        }

        assertEquals(1, game.getScoreboard().getScorePlayer2());
        assertFalse(game.isGoalFreeze());
        assertEquals(450.0, ball.getPositionX(), 1.0);
    }

    /** Verifies clock countdown, pause freeze, and timed match completion. */
    @Test
    public void testClockAndMatchEnd() {
        HeadSoccer game = new HeadSoccer(900, 540);
        game.start();

        assertEquals(180, game.getRemainingTime());

        game.tickSecond();
        assertEquals(179, game.getRemainingTime());

        game.setPaused(true);
        game.tickSecond();
        assertEquals(179, game.getRemainingTime());

        game.setPaused(false);
        game.getScoreboard().scoreGoalPlayer1();

        while (game.getRemainingTime() > 0) {
            game.tickSecond();
        }

        assertEquals(0, game.getRemainingTime());
        assertFalse(game.isRunning());
    }

    /** Verifies health, kick multiplier, normal kick, and power shot damage formulas. */
    @Test
    public void testStatisticsAndDamageFormulas() {
        HeadSoccer game = new HeadSoccer(900, 540, HeadSoccer.MODE_PVP, null,
                Power.COUNTRY_CAMEROON, Power.COUNTRY_KOREA,
                HeadSoccer.MATCH_BY_TIME, 3, 5);
        Player p1 = game.getPlayer1();
        Player p2 = game.getPlayer2();

        p1.setProgress(1, 1, 5, 1, 0);
        p2.setProgress(1, 1, 1, 3, 0);

        assertEquals(120.0, p2.getMaxHealth(), 0.01);
        assertEquals(1.4, p1.getKickMultiplier(), 0.001);

        p2.setPositionX(p1.getPositionX() + 30);
        p1.hit(null, p2);
        assertEquals(106.0, p2.getCurrentHealth(), 0.01);

        p2.resetForMatch();
        double powerDmg = p1.getPower().calculateDamage(p1.getKickMultiplier());
        assertEquals(84.0, powerDmg, 0.01);
        p2.takeDamage(powerDmg);
        assertEquals(36.0, p2.getCurrentHealth(), 0.01);
    }

    /** Verifies upgrade cost doubling and insufficient points exception handling. */
    @Test
    public void testPointsAndUpgradeCostDoubling() {
        Player p = new Player(100, 400, Color.BLUE, "P1", Power.createForCountry(Power.COUNTRY_KOREA), 1);
        assertEquals(500, Player.getUpgradeCost(1));
        assertEquals(1000, Player.getUpgradeCost(2));
        assertEquals(2000, Player.getUpgradeCost(3));

        p.addPoints(1500);
        try {
            p.upgradeStat("kick");
            assertEquals(2, p.getKickStat());
            assertEquals(1000, p.getPoints());
            p.upgradeStat("kick");
            assertEquals(3, p.getKickStat());
            assertEquals(0, p.getPoints());
        } catch (HeadSoccerException e) {
            fail("Should not fail when enough points exist: " + e.getMessage());
        }

        boolean threw = false;
        try {
            p.upgradeStat("kick");
        } catch (HeadSoccerException e) {
            threw = true;
        }
        assertTrue("Upgrading without enough points must throw HeadSoccerException", threw);
    }

    /** Verifies the power bar charging rules, activation constraints, and goal bonus. */
    @Test
    public void testPowerBarAndEightRules() {
        HeadSoccer game = new HeadSoccer(900, 540);
        game.start();
        Player p1 = game.getPlayer1();
        Ball ball = game.getBall();

        p1.onGoalConceded();
        assertEquals(25.0, p1.getPowerBar(), 0.01);

        p1.setPowerBar(0);
        for (int i = 0; i < 600; i++) {
            p1.update(0, 900, 480);
        }
        assertEquals(100.0, p1.getPowerBar(), 0.5);

        p1.activatePower();
        assertTrue(p1.getPower().isActive());
        assertEquals(0.0, p1.getPowerBar(), 0.01);

        for (int i = 0; i < 600; i++) {
            p1.update(0, 900, 480);
        }
        assertEquals("Bar must recharge even while aura is active (Rule 2)", 100.0, p1.getPowerBar(), 0.5);

        p1.activatePower();
        assertEquals("Bar remains full because second activation is blocked", 100.0, p1.getPowerBar(), 0.5);

        ball.setPosition(p1.getPositionX() + 30, p1.getPositionY());
        p1.hit(ball, game.getPlayer2());
        assertFalse(p1.getPower().isActive());
        assertTrue(ball.hasActivePowerShot());

        double hpBefore = game.getPlayer2().getCurrentHealth();
        ball.setPosition(game.getField().getRightGoalLine() + 5, game.getField().getGoalTop() + 40);
        ball.setVelocity(12, 0);
        for (int i = 0; i < 5; i++) {
            game.update();
        }
        assertEquals(1, game.getScoreboard().getScorePlayer1());
        assertEquals(hpBefore, game.getPlayer2().getCurrentHealth(), 0.01);
        assertEquals("Conceding player must receive +25% power bar", 25.0, game.getPlayer2().getPowerBar(), 1.0);
    }

    /** Verifies country power specifications, Illusion decoy expiry, and non-stacking effects. */
    @Test
    public void testFourCountriesPowersAndEffects() {
        Power korea = Power.createForCountry(Power.COUNTRY_KOREA);
        assertEquals(Power.POWER_AURA_AZUL, korea.getName());
        assertEquals(Power.EFFECT_PUSH, korea.getEffectName());
        assertEquals(40, korea.getBaseDamage());
        assertEquals(90, korea.getEffectDurationTicks());

        Power cameroon = Power.createForCountry(Power.COUNTRY_CAMEROON);
        assertEquals(Power.POWER_RAYO, cameroon.getName());
        assertEquals(Power.EFFECT_ELECTROCUTE, cameroon.getEffectName());
        assertEquals(60, cameroon.getBaseDamage());
        assertEquals(180, cameroon.getEffectDurationTicks());

        Power nigeria = Power.createForCountry(Power.COUNTRY_NIGERIA);
        assertEquals(Power.POWER_TORNADO, nigeria.getName());
        assertEquals(Power.EFFECT_UNCONSCIOUS, nigeria.getEffectName());
        assertEquals(40, nigeria.getBaseDamage());
        assertEquals(180, nigeria.getEffectDurationTicks());

        Power usa = Power.createForCountry(Power.COUNTRY_USA);
        assertEquals(Power.POWER_ILUSION, usa.getName());
        assertEquals(Power.EFFECT_CONFUSION, usa.getEffectName());
        assertEquals(30, usa.getBaseDamage());
        assertEquals(150, usa.getEffectDurationTicks());

        Field field = new Field(900, 540);
        Ball ball = new Ball(450, 300);
        Player p1 = new Player(400, 450, Color.BLUE, "P1", usa, 1);
        p1.setPowerBar(100);
        p1.activatePower();
        p1.executePowerShot(ball);
        assertEquals(7, ball.getDecoyBalls().size());

        for (int i = 0; i < 150; i++) {
            ball.move(field);
        }
        assertEquals("Decoy balls must disappear when Illusion effect ends", 0, ball.getDecoyBalls().size());

        Player rival = new Player(600, 450, Color.RED, "P2", korea, -1);
        rival.applyEffect(Power.EFFECT_ELECTROCUTE, 180, 1);
        assertEquals(Power.EFFECT_ELECTROCUTE, rival.getActiveEffect());
        rival.applyEffect(Power.EFFECT_UNCONSCIOUS, 180, 1);
        assertEquals(Power.EFFECT_UNCONSCIOUS, rival.getActiveEffect());
        assertFalse(rival.canAct());
    }

    /** Verifies 3-second stun when health reaches zero and full health recovery afterward. */
    @Test
    public void testStunWhenHealthReachesZero() {
        Player p = new Player(300, 452, Color.BLUE, "P1", Power.createForCountry(Power.COUNTRY_KOREA), 1);
        p.takeDamage(150);
        assertEquals("Health must never drop below 0", 0.0, p.getCurrentHealth(), 0.01);
        assertTrue(p.isStunned());
        assertFalse(p.canAct());

        for (int i = 0; i < 180; i++) {
            p.update(0, 900, 480);
        }
        assertFalse(p.isStunned());
        assertTrue(p.canAct());
        assertEquals(p.getMaxHealth(), p.getCurrentHealth(), 0.01);
    }

    /** Verifies sudden death activation on timed ties and victory in goal-target matches. */
    @Test
    public void testMatchByGoalsAndSuddenDeath() {
        HeadSoccer timedGame = new HeadSoccer(900, 540, HeadSoccer.MODE_PVP, null,
                Power.COUNTRY_KOREA, Power.COUNTRY_CAMEROON, HeadSoccer.MATCH_BY_TIME, 1, 5);
        timedGame.start();
        while (timedGame.getRemainingTime() > 0) {
            timedGame.tickSecond();
        }
        assertTrue(timedGame.isSuddenDeath());
        assertTrue(timedGame.isRunning());

        HeadSoccer goalGame = new HeadSoccer(900, 540, HeadSoccer.MODE_PVP, null,
                Power.COUNTRY_KOREA, Power.COUNTRY_CAMEROON, HeadSoccer.MATCH_BY_GOALS, 3, 2);
        goalGame.start();
        goalGame.getScoreboard().scoreGoalPlayer1();
        goalGame.getBall().setPosition(goalGame.getField().getRightGoalLine() + 5,
                goalGame.getField().getGoalTop() + 40);
        goalGame.getBall().setVelocity(10, 0);
        for (int i = 0; i < 85; i++) {
            goalGame.update();
        }
        assertEquals(2, goalGame.getScoreboard().getScorePlayer1());
        assertFalse(goalGame.isRunning());
        assertEquals(HeadSoccer.POINTS_WINNER, goalGame.getLastPointsPlayer1());
        assertEquals(HeadSoccer.POINTS_LOSER, goalGame.getLastPointsPlayer2());
    }

    /** Verifies that the Torpe machine AI profile runs without errors. */
    @Test
    public void testMachinePlayerAllFourProfiles() {
        for (String profile : MachinePlayer.PROFILES) {
            HeadSoccer game = new HeadSoccer(900, 540, HeadSoccer.MODE_PVM, profile,
                    Power.COUNTRY_KOREA, Power.COUNTRY_NIGERIA, HeadSoccer.MATCH_BY_TIME, 2, 5);
            assertTrue(game.getPlayer2() instanceof MachinePlayer);
            MachinePlayer ai = (MachinePlayer) game.getPlayer2();
            assertEquals(profile, ai.getProfile());
            game.start();
            for (int i = 0; i < 30; i++) {
                game.update();
            }
            assertTrue(game.isRunning());
        }
    }

    /** Verifies all three supported field resolutions construct with exact dimensions. */
    @Test
    public void testThreeFieldResolutions() {
        HeadSoccer small = new HeadSoccer(Field.RES_SMALL_WIDTH, Field.RES_SMALL_HEIGHT);
        HeadSoccer med = new HeadSoccer(Field.RES_MEDIUM_WIDTH, Field.RES_MEDIUM_HEIGHT);
        HeadSoccer large = new HeadSoccer(Field.RES_LARGE_WIDTH, Field.RES_LARGE_HEIGHT);

        assertEquals(800, small.getField().getWidth());
        assertEquals(480, small.getField().getHeight());
        assertEquals(900, med.getField().getWidth());
        assertEquals(540, med.getField().getHeight());
        assertEquals(1050, large.getField().getWidth());
        assertEquals(630, large.getField().getHeight());
    }

    /** Verifies saving and opening PvP and PvM matches plus null/missing file exceptions. */
    @Test
    public void testPersistenceSaveAndOpenMatch() {
        File pvpFile = new File("test_pvp_save.tmp");
        File pvmFile = new File("test_pvm_save.tmp");
        try {
            HeadSoccer pvp = new HeadSoccer(Field.RES_MEDIUM_WIDTH, Field.RES_MEDIUM_HEIGHT);
            pvp.start();
            pvp.tickSecond();
            pvp.getScoreboard().scoreGoalPlayer1();
            pvp.getPlayer1().setPositionX(320.0);
            pvp.save(pvpFile);

            HeadSoccer restoredPvp = HeadSoccer.open(pvpFile);
            assertEquals(1, restoredPvp.getScoreboard().getScorePlayer1());
            assertEquals(179, restoredPvp.getRemainingTime());
            assertEquals(320.0, restoredPvp.getPlayer1().getPositionX(), 0.01);

            HeadSoccer pvm = new HeadSoccer(Field.RES_LARGE_WIDTH, Field.RES_LARGE_HEIGHT,
                    HeadSoccer.MODE_PVM, MachinePlayer.PROFILE_TORPE,
                    Power.COUNTRY_USA, Power.COUNTRY_NIGERIA,
                    HeadSoccer.MATCH_BY_GOALS, 3, 7);
            pvm.start();
            pvm.getScoreboard().scoreGoalPlayer2();
            pvm.save(pvmFile);

            HeadSoccer restoredPvm = HeadSoccer.open(pvmFile);
            assertEquals(1050, restoredPvm.getField().getWidth());
            assertEquals(1, restoredPvm.getScoreboard().getScorePlayer2());
            assertEquals(Power.COUNTRY_USA, restoredPvm.getPlayer1().getCountry());
            assertTrue(restoredPvm.getPlayer2() instanceof MachinePlayer);
            assertEquals(MachinePlayer.PROFILE_TORPE, ((MachinePlayer) restoredPvm.getPlayer2()).getProfile());

            try {
                HeadSoccer.open(null);
                fail("Opening null file must throw HeadSoccerException");
            } catch (HeadSoccerException ex) {
                assertEquals(HeadSoccerException.NULL_FILE, ex.getMessage());
            }

            try {
                HeadSoccer.open(new File("non_existent_save_12345.soccer"));
                fail("Opening missing file must throw HeadSoccerException");
            } catch (HeadSoccerException ex) {
                assertEquals(HeadSoccerException.FILE_NOT_FOUND, ex.getMessage());
            }
        } catch (HeadSoccerException e) {
            fail("Persistence save/open failed: " + e.getMessage());
        } finally {
            if (pvpFile.exists()) pvpFile.delete();
            if (pvmFile.exists()) pvmFile.delete();
        }
    }

    /** Verifies ball flight over the right goal crosses rightGoalLine without scoring. */
    @Test
    public void testBallRightBoundaryAndOverRightGoalNoScore() {
        HeadSoccer game = new HeadSoccer(900, 540);
        game.start();
        Ball ball = game.getBall();
        Field field = game.getField();
        int rightGoalLine = field.getRightGoalLine();
        int goalTop = field.getGoalTop();

        ball.setPosition(rightGoalLine - 15, goalTop - 80);
        ball.setVelocity(12, 0);
        boolean passedRightGoalLine = false;
        for (int i = 0; i < 15; i++) {
            game.update();
            if (ball.getPositionX() > rightGoalLine) {
                passedRightGoalLine = true;
            }
            assertFalse("Ball above right crossbar must never count as goal",
                    field.getRightGoal().containsBall(ball));
            assertTrue("Ball must remain inside right screen border",
                    ball.getPositionX() <= field.getWidth() - Ball.RADIUS + 0.1);
        }
        assertTrue("Ball above right goal must cross past rightGoalLine", passedRightGoalLine);
        assertEquals(0, game.getScoreboard().getScorePlayer1());
        assertEquals(0, game.getScoreboard().getScorePlayer2());
    }

    /** Verifies fast vertical drops onto either goal roof bounce without counting goals. */
    @Test
    public void testBallBouncesOnTopOfCrossbarWithoutScoring() {
        HeadSoccer game = new HeadSoccer(900, 540);
        game.start();
        Ball ball = game.getBall();
        Field field = game.getField();
        int leftGoalLine = field.getLeftGoalLine();
        int rightGoalLine = field.getRightGoalLine();
        int goalTop = field.getGoalTop();

        ball.setPosition(leftGoalLine / 2.0, goalTop - Ball.RADIUS - 2);
        ball.setVelocity(0, 14);
        for (int i = 0; i < 20; i++) {
            game.update();
            assertTrue("Ball falling on left crossbar must stay above roof",
                    ball.getPositionY() <= goalTop - Ball.RADIUS + 0.5);
            assertFalse("Ball on top of left crossbar must not trigger goal",
                    field.getLeftGoal().containsBall(ball));
        }

        ball.setPosition(rightGoalLine + (field.getWidth() - rightGoalLine) / 2.0, goalTop - Ball.RADIUS - 2);
        ball.setVelocity(0, 14);
        for (int i = 0; i < 20; i++) {
            game.update();
            assertTrue("Ball falling on right crossbar must stay above roof",
                    ball.getPositionY() <= goalTop - Ball.RADIUS + 0.5);
            assertFalse("Ball on top of right crossbar must not trigger goal",
                    field.getRightGoal().containsBall(ball));
        }

        assertEquals(0, game.getScoreboard().getScorePlayer1());
        assertEquals(0, game.getScoreboard().getScorePlayer2());
    }

    /** Verifies upward shots inside the goal rebound off the underside of the crossbar. */
    @Test
    public void testBallBouncesUnderCrossbarInsideGoal() {
        Field field = new Field(900, 540);
        Ball ball = new Ball(field.getLeftGoalLine() / 2.0, field.getGoalTop() + Ball.RADIUS + 5);
        ball.setVelocity(0, -12);

        ball.move(field);

        assertTrue("Ball hitting underside of crossbar must stay below crossbar",
                ball.getPositionY() >= field.getGoalTop() + Ball.RADIUS - 0.1);
        assertTrue("Ball velocity must reflect downward after hitting underside of crossbar",
                ball.getVelocityY() > 0);
    }

    /** Verifies circular rebound when striking the front corner tip of a crossbar. */
    @Test
    public void testBallCrossbarCornerPostBounce() {
        Field field = new Field(900, 540);
        int leftGoalLine = field.getLeftGoalLine();
        int goalTop = field.getGoalTop();
        Ball ball = new Ball(leftGoalLine + 18, goalTop);
        ball.setVelocity(-8, 0);

        ball.move(field);

        assertTrue("Ball hitting front tip of crossbar post should rebound rightwards",
                ball.getVelocityX() > 0);
        double dx = ball.getPositionX() - leftGoalLine;
        double dy = ball.getPositionY() - goalTop;
        assertTrue("Ball must be pushed clear of crossbar tip post",
                Math.sqrt(dx * dx + dy * dy) >= Ball.RADIUS + 1.5);
    }

    /** Verifies ball speed clamping, impulse accumulation, and full state reset. */
    @Test
    public void testBallSpeedCapAndApplyImpulse() {
        Ball ball = new Ball(450, 200);
        ball.setVelocity(100, -100);
        assertEquals(22.0, ball.getVelocityX(), 0.01);
        assertEquals(-22.0, ball.getVelocityY(), 0.01);

        ball.applyImpulse(-100, 100);
        assertEquals(-22.0, ball.getVelocityX(), 0.01);
        assertEquals(22.0, ball.getVelocityY(), 0.01);

        ball.setPosition(100, 100);
        ball.reset();
        assertEquals(450.0, ball.getPositionX(), 0.01);
        assertEquals(200.0, ball.getPositionY(), 0.01);
        assertEquals(0.0, ball.getVelocityX(), 0.01);
        assertEquals(0.0, ball.getVelocityY(), 0.01);
        assertEquals(0.0, ball.getRotationAngle(), 0.01);
        assertFalse(ball.hasActivePowerShot());
    }

    /** Verifies right boundary containment, initial facing direction, and opposing input cancellation. */
    @Test
    public void testPlayerRightBoundaryAndFacingDirection() {
        HeadSoccer game = new HeadSoccer(900, 540);
        Player p1 = game.getPlayer1();
        Player p2 = game.getPlayer2();
        int maxX = game.getField().getRightGoalLine() - Player.HEAD_RADIUS;
        int groundY = game.getField().getGroundY();

        assertEquals(1, p1.getFacingDirection());
        assertEquals(-1, p2.getFacingDirection());

        p2.setMovingRight(true);
        for (int i = 0; i < 100; i++) {
            p2.update(0, maxX, groundY);
        }
        assertEquals(1, p2.getFacingDirection());
        assertEquals(maxX, p2.getPositionX(), 0.01);

        p2.setMovingLeft(true);
        p2.update(0, maxX, groundY);
        assertEquals(0.0, p2.getVelocityX(), 0.01);
    }

    /** Verifies field goal coordinates and scoreboard reset behavior. */
    @Test
    public void testFieldGoalGeometryAndScoreboardReset() {
        Field field = new Field(900, 540);
        assertEquals(480, field.getGroundY());
        assertEquals(350, field.getGoalTop());
        assertEquals(70, field.getLeftGoalLine());
        assertEquals(830, field.getRightGoalLine());
        assertEquals(0, field.getLeftGoal().getPositionX());
        assertEquals(350, field.getLeftGoal().getPositionY());
        assertEquals(Field.GOAL_WIDTH, field.getLeftGoal().getWidth());
        assertEquals(Field.GOAL_HEIGHT, field.getLeftGoal().getHeight());
        assertEquals(830, field.getRightGoal().getPositionX());

        Scoreboard sb = new Scoreboard();
        sb.scoreGoalPlayer1();
        sb.scoreGoalPlayer1();
        sb.scoreGoalPlayer2();
        assertEquals(2, sb.getScorePlayer1());
        assertEquals(1, sb.getScorePlayer2());

        sb.reset();
        assertEquals(0, sb.getScorePlayer1());
        assertEquals(0, sb.getScorePlayer2());
    }

    /** Verifies kicking deals no damage when the opponent is out of reach or stunned. */
    @Test
    public void testPlayerKickOutOfRangeDoesNoDamage() {
        Player p1 = new Player(200, 452, Color.BLUE, "P1", Power.createForCountry(Power.COUNTRY_KOREA), 1);
        Player p2 = new Player(500, 452, Color.RED, "P2", Power.createForCountry(Power.COUNTRY_CAMEROON), -1);

        p1.hit(null, p2);
        assertEquals("Rival out of reach must not take damage", p2.getMaxHealth(), p2.getCurrentHealth(), 0.01);

        p2.setPositionX(230);
        p2.takeDamage(100);
        assertTrue(p2.isStunned());
        p1.hit(null, p2);
        assertEquals(0.0, p2.getCurrentHealth(), 0.01);
    }

    /** Verifies upgrading all four stats and enforcing MAX_STAT_LEVEL with HeadSoccerException. */
    @Test
    public void testPlayerUpgradeAllFourStatsAndMaxLevel() {
        Player p = new Player(200, 452, Color.BLUE, "P1", Power.createForCountry(Power.COUNTRY_KOREA), 1);
        p.addPoints(600000);

        try {
            p.upgradeStat("speed");
            p.upgradeStat("jump");
            p.upgradeStat("kick");
            p.upgradeStat("survival");
        } catch (HeadSoccerException e) {
            fail("Upgrading all four stats should succeed: " + e.getMessage());
        }

        assertEquals(2, p.getSpeedStat());
        assertEquals(2, p.getJumpStat());
        assertEquals(2, p.getKickStat());
        assertEquals(2, p.getSurvivalStat());
        assertEquals(110.0, p.getCurrentHealth(), 0.01);
        assertEquals(2, p.getStatLevel("SPEED"));
        assertEquals(2, p.getStatLevel("jump"));

        try {
            while (p.getSpeedStat() < Player.MAX_STAT_LEVEL) {
                p.upgradeStat("speed");
            }
        } catch (HeadSoccerException e) {
            fail("Should reach max stat level cleanly: " + e.getMessage());
        }
        assertEquals(Player.MAX_STAT_LEVEL, p.getSpeedStat());
        assertEquals(0, Player.getUpgradeCost(Player.MAX_STAT_LEVEL));

        try {
            p.upgradeStat("speed");
            fail("Upgrading past MAX_STAT_LEVEL must throw HeadSoccerException");
        } catch (HeadSoccerException ex) {
            assertEquals(HeadSoccerException.MAX_STAT_REACHED, ex.getMessage());
        }
    }

    /** Verifies invalid and null stat names return -1 or throw HeadSoccerException. */
    @Test
    public void testPlayerUpgradeInvalidStatNameAndNullThrows() {
        Player p = new Player(200, 452, Color.BLUE, "P1", Power.createForCountry(Power.COUNTRY_KOREA), 1);
        p.addPoints(5000);

        assertEquals(-1, p.getStatLevel(null));
        assertEquals(-1, p.getStatLevel("defense"));

        try {
            p.upgradeStat(null);
            fail("Null stat name must throw HeadSoccerException");
        } catch (HeadSoccerException ex) {
            assertEquals(HeadSoccerException.INVALID_STAT_NAME, ex.getMessage());
        }

        try {
            p.upgradeStat("invalid_stat");
            fail("Unknown stat name must throw HeadSoccerException");
        } catch (HeadSoccerException ex) {
            assertEquals(HeadSoccerException.INVALID_STAT_NAME, ex.getMessage());
        }
    }

    /** Verifies setProgress clamping to [1..10] and resulting stat multipliers. */
    @Test
    public void testPlayerStatProgressClampingAndMultipliers() {
        Player p = new Player(200, 452, Color.BLUE, "P1", Power.createForCountry(Power.COUNTRY_KOREA), 1);
        double baseSpeed = p.getEffectiveMoveSpeed();
        double baseJump = p.getEffectiveJumpForce();

        p.setProgress(-5, 99, 5, 10, -100);
        assertEquals(1, p.getSpeedStat());
        assertEquals(Player.MAX_STAT_LEVEL, p.getJumpStat());
        assertEquals(5, p.getKickStat());
        assertEquals(Player.MAX_STAT_LEVEL, p.getSurvivalStat());
        assertEquals(0, p.getPoints());
        assertEquals(190.0, p.getMaxHealth(), 0.01);
        assertEquals(190.0, p.getCurrentHealth(), 0.01);
        assertEquals(1.4, p.getKickMultiplier(), 0.001);
        assertTrue("Higher jump stat must yield stronger upward force", p.getEffectiveJumpForce() < baseJump);

        p.setProgress(6, 1, 1, 1, 100);
        assertTrue("Higher speed stat must increase movement speed", p.getEffectiveMoveSpeed() > baseSpeed);
    }

    /** Verifies Electrocute halves horizontal speed, blocks jumping, and expires after 3s. */
    @Test
    public void testElectrocuteEffectHalvesSpeedAndBlocksJump() {
        Player p = new Player(450, 452, Color.BLUE, "P1", Power.createForCountry(Power.COUNTRY_KOREA), 1);
        double normalSpeed = p.getEffectiveMoveSpeed();

        p.applyEffect(Power.EFFECT_ELECTROCUTE, 180, 1);
        assertEquals(normalSpeed * 0.5, p.getEffectiveMoveSpeed(), 0.001);

        p.jump();
        p.update(0, 900, 480);
        assertTrue("Electrocuted player must not be able to jump", p.isOnGround());

        for (int i = 0; i < 180; i++) {
            p.update(0, 900, 480);
        }
        assertNull("Electrocute effect must expire after 180 ticks", p.getActiveEffect());
        assertEquals(normalSpeed, p.getEffectiveMoveSpeed(), 0.001);
    }

    /** Verifies Push forces the affected player toward its own goal and expires after 1.5s. */
    @Test
    public void testPushEffectMovesPlayerTowardOwnGoal() {
        Player p = new Player(450, 452, Color.BLUE, "P1", Power.createForCountry(Power.COUNTRY_KOREA), 1);
        p.applyEffect(Power.EFFECT_PUSH, 90, -1);
        p.setMovingRight(true);

        for (int i = 0; i < 10; i++) {
            p.update(0, 900, 480);
        }
        assertTrue("Push effect must push player leftward toward own goal", p.getPositionX() < 400.0);

        for (int i = 0; i < 85; i++) {
            p.update(0, 900, 480);
        }
        assertNull("Push effect must expire after 90 ticks", p.getActiveEffect());
    }

    /** Verifies Unconscious launches the player upward, blocks actions, and recovers after 3s. */
    @Test
    public void testUnconsciousEffectLaunchesPlayerAndBlocksActions() {
        Player p = new Player(450, 452, Color.RED, "P2", Power.createForCountry(Power.COUNTRY_NIGERIA), -1);
        p.setPowerBar(100);
        p.applyEffect(Power.EFFECT_UNCONSCIOUS, 180, 1);

        assertFalse("Unconscious effect must launch player off ground", p.isOnGround());
        assertTrue("Unconscious effect must apply upward velocity", p.getVelocityY() < 0);
        assertFalse("Unconscious player cannot act", p.canAct());

        p.activatePower();
        assertFalse("Unconscious player cannot activate power", p.getPower().isActive());

        for (int i = 0; i < 180; i++) {
            p.update(0, 900, 480);
        }
        assertNull(p.getActiveEffect());
        assertTrue("Player regains ability to act after unconscious expires", p.canAct());
    }

    /** Verifies a Rayo power shot deals damage, applies Electrocute, and clears on rival impact. */
    @Test
    public void testPowerShotCollisionAppliesDamageAndEffectToOpponent() {
        HeadSoccer game = new HeadSoccer(900, 540, HeadSoccer.MODE_PVP, null,
                Power.COUNTRY_CAMEROON, Power.COUNTRY_KOREA, HeadSoccer.MATCH_BY_TIME, 3, 5);
        game.start();
        Player p1 = game.getPlayer1();
        Player p2 = game.getPlayer2();
        Ball ball = game.getBall();

        p2.setPositionX(p1.getPositionX() + 130);
        p1.setPowerBar(100);
        p1.activatePower();
        ball.setPosition(p1.getPositionX() + 30, p1.getPositionY());
        p1.hit(ball, p2);
        assertTrue("Ball must carry active Rayo power shot", ball.hasActivePowerShot());

        for (int i = 0; i < 15 && ball.hasActivePowerShot(); i++) {
            game.update();
        }

        assertFalse("Power shot impact must clear after hitting rival", ball.hasActivePowerShot());
        assertEquals("Rayo base damage (60) must reduce rival HP from 100 to 40", 40.0, p2.getCurrentHealth(), 0.01);
        assertEquals(Power.EFFECT_ELECTROCUTE, p2.getActiveEffect());
    }

    /** Verifies Power factory defaults, aliases, unknown fallback, and convenience constructors. */
    @Test
    public void testPowerFactoryAndBackwardCompatibleConstructors() {
        Power defaultNull = Power.createForCountry(null);
        assertEquals(Power.COUNTRY_KOREA, defaultNull.getCountry());
        assertEquals(Power.POWER_AURA_AZUL, defaultNull.getName());

        Power usaAlias = Power.createForCountry("  usa ");
        assertEquals(Power.POWER_ILUSION, usaAlias.getName());
        assertEquals(2.5, usaAlias.getEffectDurationSeconds(), 0.001);

        Power custom = Power.createForCountry("Brasil");
        assertEquals("Brasil", custom.getCountry());
        assertEquals(Power.POWER_AURA_AZUL, custom.getName());

        Power singleArg = new Power("CustomShot");
        assertEquals("CustomShot", singleArg.getName());
        assertEquals(40, singleArg.getBaseDamage());
        assertEquals(22.0, singleArg.getShotSpeedX(), 0.01);

        Power fiveArg = new Power("TestPower", "TestLand", "Push", 2.0, 50);
        assertEquals("TestPower", fiveArg.getName());
        assertEquals(120, fiveArg.getEffectDurationTicks());
        assertEquals(20.0, fiveArg.getShotSpeedX(), 0.01);
        assertEquals(0.0, fiveArg.getShotSpeedY(), 0.01);
    }

    /** Verifies Tornado ground bounce trajectory, clearing power shots, and Aura Azul speed. */
    @Test
    public void testPowerShotTornadoAndAuraAzulTrajectories() {
        Field field = new Field(900, 540);
        Player pNigeria = new Player(300, 452, Color.BLUE, "P1", Power.createForCountry(Power.COUNTRY_NIGERIA), 1);
        Ball ball = new Ball(330, field.getGroundY() - Ball.RADIUS - 2);

        pNigeria.setPowerBar(100);
        pNigeria.activatePower();
        pNigeria.executePowerShot(ball);

        assertTrue(ball.hasActivePowerShot());
        assertSame(pNigeria, ball.getPowerShotOwner());
        ball.move(field);
        assertEquals("Tornado power shot must bounce upward with -7.5 velocity on ground hit",
                -7.5, ball.getVelocityY(), 0.01);

        ball.clearAllPowerShotEffects();
        assertFalse(ball.hasActivePowerShot());
        assertNull(ball.getPowerShotOwner());

        Player pKorea = new Player(300, 452, Color.BLUE, "P1", Power.createForCountry(Power.COUNTRY_KOREA), 1);
        ball.setPosition(330, 300);
        pKorea.setPowerBar(100);
        pKorea.activatePower();
        pKorea.executePowerShot(ball);
        assertTrue("Aura Azul must fire fast horizontal shot", ball.getVelocityX() >= 20.0);
    }

    /** Verifies power activation fails when bar is below 100% or while player is stunned. */
    @Test
    public void testPowerBarCannotActivateBelowFullOrWhileStunned() {
        Player p = new Player(300, 452, Color.BLUE, "P1", Power.createForCountry(Power.COUNTRY_KOREA), 1);
        p.setPowerBar(99.0);
        p.activatePower();
        assertFalse("Power cannot activate below 100%", p.getPower().isActive());
        assertEquals(99.0, p.getPowerBar(), 0.01);

        p.setPowerBar(100.0);
        p.takeDamage(100.0);
        assertTrue(p.isStunned());
        p.activatePower();
        assertFalse("Stunned player cannot activate power", p.getPower().isActive());
        assertEquals(100.0, p.getPowerBar(), 0.01);
    }

    /** Verifies sudden death finishes the match immediately after a golden goal freeze. */
    @Test
    public void testSuddenDeathFinishesImmediatelyAfterGoalFreeze() {
        HeadSoccer game = new HeadSoccer(900, 540, HeadSoccer.MODE_PVP, null,
                Power.COUNTRY_KOREA, Power.COUNTRY_CAMEROON, HeadSoccer.MATCH_BY_TIME, 1, 5);
        game.start();
        while (game.getRemainingTime() > 0) {
            game.tickSecond();
        }
        assertTrue(game.isSuddenDeath());
        assertTrue(game.isRunning());

        game.getBall().setPosition(game.getField().getRightGoalLine() + 5, game.getField().getGoalTop() + 40);
        game.getBall().setVelocity(12, 0);
        for (int i = 0; i < 85; i++) {
            game.update();
        }

        assertFalse("Match must end after golden goal in sudden death", game.isRunning());
        assertTrue(game.isMatchFinished());
        assertEquals(1, game.getScoreboard().getScorePlayer1());
        assertEquals(HeadSoccer.POINTS_WINNER, game.getLastPointsPlayer1());
        assertEquals(HeadSoccer.POINTS_LOSER, game.getLastPointsPlayer2());
    }

    /** Verifies finishing a tied match awards loser points once and is idempotent. */
    @Test
    public void testMatchFinishTieAwardsLoserPointsToBothAndIdempotent() {
        HeadSoccer game = new HeadSoccer(900, 540);
        game.start();
        game.finish();

        assertTrue(game.isMatchFinished());
        assertFalse(game.isRunning());
        assertEquals(HeadSoccer.POINTS_LOSER, game.getLastPointsPlayer1());
        assertEquals(HeadSoccer.POINTS_LOSER, game.getLastPointsPlayer2());
        assertEquals(HeadSoccer.POINTS_LOSER, game.getPlayer1().getPoints());
        assertEquals(HeadSoccer.POINTS_LOSER, game.getPlayer2().getPoints());

        game.finish();
        assertEquals(HeadSoccer.POINTS_LOSER, game.getPlayer1().getPoints());
        assertEquals(HeadSoccer.POINTS_LOSER, game.getPlayer2().getPoints());
    }

    /** Verifies validateConfiguration accepts valid bounds and rejects invalid configurations. */
    @Test
    public void testValidateConfigurationValidAndInvalidCases() {
        try {
            HeadSoccer.validateConfiguration(800, 480, 1, 1);
            HeadSoccer.validateConfiguration(1050, 630, 5, 10);
        } catch (HeadSoccerException e) {
            fail("Valid configurations should not throw: " + e.getMessage());
        }

        int[][] invalidCases = {
            {500, 480, 3, 5},
            {900, 300, 3, 5},
            {900, 540, 0, 5},
            {900, 540, 3, 0}
        };
        for (int[] c : invalidCases) {
            try {
                HeadSoccer.validateConfiguration(c[0], c[1], c[2], c[3]);
                fail("Invalid config must throw HeadSoccerException");
            } catch (HeadSoccerException ex) {
                assertEquals(HeadSoccerException.INVALID_CONFIG, ex.getMessage());
            }
        }
    }

    /** Verifies Torpe machine actions, field bounds, null-ball reset, and profile normalization. */
    @Test
    public void testMachinePlayerDefensiveAndOffensiveBehavior() {
        Field field = new Field(900, 540);
        Scoreboard sb = new Scoreboard();
        Player human = new Player(200, 452, Color.BLUE, "P1", Power.createForCountry(Power.COUNTRY_USA), 1);

        MachinePlayer torpeAi = new MachinePlayer(500, 452, Color.RED, "CPU",
                Power.createForCountry(Power.COUNTRY_NIGERIA), -1, "torpe");
        assertEquals(MachinePlayer.PROFILE_TORPE, torpeAi.getProfile());

        Ball ball = new Ball(200, 450);
        torpeAi.setPowerBar(100);
        for (int i = 0; i < 60; i++) {
            torpeAi.decideAction(ball, human, field, sb);
            torpeAi.update(field.getLeftGoalLine() + Player.HEAD_RADIUS,
                    field.getRightGoalLine() - Player.HEAD_RADIUS, field.getGroundY());
        }
        assertTrue("Torpe AI must stay within field bounds",
                torpeAi.getPositionX() >= field.getLeftGoalLine() + Player.HEAD_RADIUS
                && torpeAi.getPositionX() <= field.getRightGoalLine() - Player.HEAD_RADIUS);

        torpeAi.setMovingRight(true);
        torpeAi.decideAction(null, human, field, sb);
        torpeAi.update(field.getLeftGoalLine() + Player.HEAD_RADIUS,
                field.getRightGoalLine() - Player.HEAD_RADIUS, field.getGroundY());
        assertEquals(0.0, torpeAi.getVelocityX(), 0.01);

        MachinePlayer defaultAi = new MachinePlayer(700, 452, Color.RED, "CPU",
                Power.createForCountry(Power.COUNTRY_KOREA), -1, null);
        assertEquals(MachinePlayer.PROFILE_TORPE, defaultAi.getProfile());
    }

    /** Verifies saveProgress and loadProgress round-trip accuracy and error cases. */
    @Test
    public void testPersistenceProgressSaveLoadAndErrors() {
        File progressFile = new File("test_progress_save.tmp");
        try {
            int[] p1Prog = {3, 4, 5, 2, 1250};
            int[] p2Prog = {2, 1, 6, 3, 750};
            HeadSoccer.saveProgress(progressFile, p1Prog, p2Prog);

            int[][] loaded = HeadSoccer.loadProgress(progressFile);
            assertNotNull(loaded);
            assertEquals(2, loaded.length);
            assertArrayEquals(p1Prog, loaded[0]);
            assertArrayEquals(p2Prog, loaded[1]);

            try {
                HeadSoccer.saveProgress(null, p1Prog, p2Prog);
                fail("Saving progress to null file must throw");
            } catch (HeadSoccerException ex) {
                assertEquals(HeadSoccerException.NULL_FILE, ex.getMessage());
            }

            try {
                HeadSoccer.loadProgress(null);
                fail("Loading progress from null file must throw");
            } catch (HeadSoccerException ex) {
                assertEquals(HeadSoccerException.FILE_NOT_FOUND, ex.getMessage());
            }

            try {
                HeadSoccer.loadProgress(new File("missing_progress_98765.dat"));
                fail("Loading missing progress file must throw");
            } catch (HeadSoccerException ex) {
                assertEquals(HeadSoccerException.FILE_NOT_FOUND, ex.getMessage());
            }
        } catch (HeadSoccerException e) {
            fail("Progress persistence failed unexpectedly: " + e.getMessage());
        } finally {
            if (progressFile.exists()) progressFile.delete();
        }
    }

    /** Verifies saving to null throws NULL_FILE and corrupted files throw OPEN_ERROR. */
    @Test
    public void testPersistenceCorruptedFileAndNullSaveThrows() {
        HeadSoccer game = new HeadSoccer(900, 540);
        try {
            game.save(null);
            fail("Saving match to null file must throw HeadSoccerException");
        } catch (HeadSoccerException ex) {
            assertEquals(HeadSoccerException.NULL_FILE, ex.getMessage());
        }

        File corrupted = new File("test_corrupted_match.tmp");
        try (FileOutputStream fos = new FileOutputStream(corrupted)) {
            fos.write("not a valid java serialized object".getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            fail("Failed to create temp corrupted file: " + e.getMessage());
        }

        try {
            try {
                HeadSoccer.open(corrupted);
                fail("Opening corrupted match file must throw HeadSoccerException");
            } catch (HeadSoccerException ex) {
                assertTrue("Error message must start with OPEN_ERROR",
                        ex.getMessage().startsWith(HeadSoccerException.OPEN_ERROR));
            }

            try {
                HeadSoccer.loadProgress(corrupted);
                fail("Loading corrupted progress file must throw HeadSoccerException");
            } catch (HeadSoccerException ex) {
                assertEquals(HeadSoccerException.OPEN_ERROR, ex.getMessage());
            }
        } finally {
            if (corrupted.exists()) corrupted.delete();
        }
    }
}
