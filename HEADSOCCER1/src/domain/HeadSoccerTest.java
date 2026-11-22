package src.domain;

import static org.junit.Assert.*;
import org.junit.Test;

import java.awt.Color;
import java.io.File;

/**
 * Automated JUnit 4 + standalone unit test suite for DOPOSoccer (Version 2).
 * Tests fast goal-height jump physics, free airspace over goal crossbars,
 * all four Annex 1 powers and 8 power rules, Annex 2 stats/damage/stun,
 * AI profiles, resolutions, sudden death, and persistence.
 */
public class HeadSoccerTest {

    private static int testsPassed = 0;
    private static int testsTotal  = 0;

    public static void main(String[] args) {
        System.out.println("============================================");
        System.out.println("  RUNNING DOPOSOCCER UNIT TEST SUITE        ");
        System.out.println("============================================\n");

        HeadSoccerTest suite = new HeadSoccerTest();
        runTest("Player Movement and Boundaries", suite::testPlayerMovementAndBounds);
        runTest("Player Fast Jump (Just Above Goal Height)", suite::testPlayerJumpAndGravity);
        runTest("Player-Player Collision", suite::testPlayerPlayerCollision);
        runTest("Ball Gravity and Ground Bounce", suite::testBallGravityAndBounce);
        runTest("Ball Ceiling, Crossbar, and Passing Over Goal", suite::testBallCeilingCrossbarAndOverGoal);
        runTest("Player-Ball Head Collision Separation", suite::testPlayerBallCollisionHead);
        runTest("Player-Ball Foot Collision Separation", suite::testPlayerBallCollisionFoot);
        runTest("Player Hit / Kick Ball Without Damaging Rival", suite::testPlayerHit);
        runTest("Left Goal Detection (Player 2 Scores)", suite::testLeftGoalDetection);
        runTest("Right Goal Detection (Player 1 Scores)", suite::testRightGoalDetection);
        runTest("Goal Freeze and Reset (No Double Counting)", suite::testGoalFreezeAndReset);
        runTest("Clock Countdown, Pause, and Match End", suite::testClockAndMatchEnd);
        runTest("Statistics and Damage Formulas (PDF Example)", suite::testStatisticsAndDamageFormulas);
        runTest("Points and Upgrade Cost Doubling", suite::testPointsAndUpgradeCostDoubling);
        runTest("Power Bar 10s Charge, Immediate Recharge, and Goal Quarter Bonus", suite::testPowerBarAndEightRules);
        runTest("Four Annex 1 Countries Powers and Effects", suite::testFourCountriesPowersAndEffects);
        runTest("Stun 3s When Health Reaches Zero and Recovery", suite::testStunWhenHealthReachesZero);
        runTest("Match By Goals and Sudden Death", suite::testMatchByGoalsAndSuddenDeath);
        runTest("Machine Player All Four Profiles", suite::testMachinePlayerAllFourProfiles);
        runTest("Three Field Resolutions", suite::testThreeFieldResolutions);
        runTest("Persistence Save and Open (PvP, PvM, Errors)", suite::testPersistenceSaveAndOpenMatch);

        System.out.println("\n============================================");
        System.out.printf("  RESULTS: %d / %d tests passed successfully%n", testsPassed, testsTotal);
        System.out.println("============================================");

        if (testsPassed != testsTotal) {
            System.exit(1);
        }
    }

    private static void runTest(String name, Runnable test) {
        testsTotal++;
        try {
            test.run();
            testsPassed++;
            System.out.println("[PASS] " + name);
        } catch (Throwable t) {
            System.err.println("[FAIL] " + name + ": " + t.getMessage());
            t.printStackTrace();
        }
    }

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

    @Test
    public void testBallCeilingCrossbarAndOverGoal() {
        HeadSoccer game = new HeadSoccer(900, 540);
        Ball ball = game.getBall();

        // 1. Ceiling bounce
        ball.setPosition(450, 25);
        ball.setVelocity(0, -10);
        ball.move(game.getField());
        ball.move(game.getField());
        assertTrue("Ball must bounce off ceiling", ball.getPositionY() >= Ball.RADIUS);
        assertTrue("Ball vertical velocity should reflect downwards", ball.getVelocityY() > 0);

        // 2. Ball falling on top of crossbar bounces up
        int leftGoalLine = game.getField().getLeftGoalLine();
        int goalTop = game.getField().getGoalTop();
        ball.setPosition(leftGoalLine / 2.0, goalTop - 30);
        ball.setVelocity(0, 8);
        for (int i = 0; i < 10; i++) {
            ball.move(game.getField());
        }
        assertTrue("Ball must bounce on top of crossbar", ball.getPositionY() <= goalTop - Ball.RADIUS + 1.0);

        // 3. Ball traveling high over left goal passes freely past leftGoalLine without scoring
        game.start();
        ball.setPosition(leftGoalLine + 15, goalTop - 80);
        ball.setVelocity(-12, 0);
        for (int i = 0; i < 15; i++) {
            game.update();
            assertFalse("Ball above crossbar must never count as a goal",
                    game.getField().getLeftGoal().containsBall(ball));
        }
        assertTrue("Ball above crossbar must pass freely past leftGoalLine", ball.getPositionX() < leftGoalLine);
        assertEquals("Home score must remain 0 when ball goes over goal", 0, game.getScoreboard().getScorePlayer1());
        assertEquals("Away score must remain 0 when ball goes over goal", 0, game.getScoreboard().getScorePlayer2());
    }

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

    @Test
    public void testStatisticsAndDamageFormulas() {
        // Exact PDF page 5 example: Player 1 = Camerún (Kick=5), Player 2 = Survival=3
        HeadSoccer game = new HeadSoccer(900, 540, HeadSoccer.MODE_PVP, null,
                Power.COUNTRY_CAMEROON, Power.COUNTRY_KOREA,
                HeadSoccer.MATCH_BY_TIME, 3, 5);
        Player p1 = game.getPlayer1();
        Player p2 = game.getPlayer2();

        p1.setProgress(1, 1, 5, 1, 0);
        p2.setProgress(1, 1, 1, 3, 0);

        assertEquals(120.0, p2.getMaxHealth(), 0.01);
        assertEquals(1.4, p1.getKickMultiplier(), 0.001);

        // Normal kick hitting rival without touching ball: 10 * 1.4 = 14 -> 106 HP
        p2.setPositionX(p1.getPositionX() + 30);
        p1.hit(null, p2);
        assertEquals(106.0, p2.getCurrentHealth(), 0.01);

        // Power shot hit: 60 * 1.4 = 84 -> 36 HP from 120 HP
        p2.resetForMatch();
        double powerDmg = p1.getPower().calculateDamage(p1.getKickMultiplier());
        assertEquals(84.0, powerDmg, 0.01);
        p2.takeDamage(powerDmg);
        assertEquals(36.0, p2.getCurrentHealth(), 0.01);
    }

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

    @Test
    public void testPowerBarAndEightRules() {
        HeadSoccer game = new HeadSoccer(900, 540);
        game.start();
        Player p1 = game.getPlayer1();
        Ball ball = game.getBall();

        // Rule 1: 10s charge (600 ticks) and +1/4 (+25%) on conceding goal
        p1.onGoalConceded();
        assertEquals(25.0, p1.getPowerBar(), 0.01);

        p1.setPowerBar(0);
        for (int i = 0; i < 600; i++) {
            p1.update(0, 900, 480);
        }
        assertEquals(100.0, p1.getPowerBar(), 0.5);

        // Rule 2 & 3: Activate clears bar and begins recharging immediately while aura stays active
        p1.activatePower();
        assertTrue(p1.getPower().isActive());
        assertEquals(0.0, p1.getPowerBar(), 0.01);

        for (int i = 0; i < 600; i++) {
            p1.update(0, 900, 480);
        }
        assertEquals("Bar must recharge even while aura is active (Rule 2)", 100.0, p1.getPowerBar(), 0.5);

        // Rule 7: Cannot activate another power while aura is active
        p1.activatePower();
        assertEquals("Bar remains full because second activation is blocked", 100.0, p1.getPowerBar(), 0.5);

        // Rule 4: Touching ball executes power shot and removes aura
        ball.setPosition(p1.getPositionX() + 30, p1.getPositionY());
        p1.hit(ball, game.getPlayer2());
        assertFalse(p1.getPower().isActive());
        assertTrue(ball.hasActivePowerShot());

        // Rule 6: Power shot entering goal directly scores without damaging rival
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

        // USA Illusion divides into 8 identical balls (1 real + 7 decoys) and disappears after 2.5s (150 ticks)
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

        // Rule 8: effects do not accumulate; new effect replaces previous
        Player rival = new Player(600, 450, Color.RED, "P2", korea, -1);
        rival.applyEffect(Power.EFFECT_ELECTROCUTE, 180, 1);
        assertEquals(Power.EFFECT_ELECTROCUTE, rival.getActiveEffect());
        rival.applyEffect(Power.EFFECT_UNCONSCIOUS, 180, 1);
        assertEquals(Power.EFFECT_UNCONSCIOUS, rival.getActiveEffect());
        assertFalse(rival.canAct());
    }

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

    @Test
    public void testThreeFieldResolutions() {
        HeadSoccer small = new HeadSoccer(Field.RES_SMALL_WIDTH, Field.RES_SMALL_HEIGHT);
        HeadSoccer med   = new HeadSoccer(Field.RES_MEDIUM_WIDTH, Field.RES_MEDIUM_HEIGHT);
        HeadSoccer large = new HeadSoccer(Field.RES_LARGE_WIDTH, Field.RES_LARGE_HEIGHT);

        assertEquals(800, small.getField().getWidth());
        assertEquals(480, small.getField().getHeight());
        assertEquals(900, med.getField().getWidth());
        assertEquals(540, med.getField().getHeight());
        assertEquals(1050, large.getField().getWidth());
        assertEquals(630, large.getField().getHeight());
    }

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
                    HeadSoccer.MODE_PVM, MachinePlayer.PROFILE_EXPERTA,
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
            assertEquals(MachinePlayer.PROFILE_EXPERTA, ((MachinePlayer) restoredPvm.getPlayer2()).getProfile());

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
}
