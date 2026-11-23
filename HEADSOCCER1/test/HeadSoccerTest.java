package test;

import domain.*; 

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests the Head Soccer domain logic, player movement, ball physics,
 * collisions, goals, and match timer.
 */
public class HeadSoccerTest {

    @Test
    public void testPlayerMovementAndBounds() {
        HeadSoccer game = new HeadSoccer(900, 540);
        Player p1 = game.getPlayer1();
        int leftGoalLine = game.getField().getLeftGoalLine();

        p1.setMovingLeft(true);

        for (int i = 0; i < 200; i++) {
            p1.update(leftGoalLine + Player.HEAD_RADIUS, 900,
                    game.getField().getGroundY());
        }

        p1.setMovingLeft(false);

        assertTrue("Player 1 must not cross the left goal line",
                p1.getPositionX() >= leftGoalLine + Player.HEAD_RADIUS);
    }

    @Test
    public void testPlayerJumpAndGravity() {
        HeadSoccer game = new HeadSoccer(900, 540);
        Player p1 = game.getPlayer1();
        int groundY = game.getField().getGroundY();
        double initialY = groundY - Player.HEAD_RADIUS;

        assertTrue("Player should start on ground", p1.isOnGround());

        p1.jump();
        p1.update(0, 900, groundY);

        assertFalse("Player should be airborne after jump", p1.isOnGround());
        assertTrue("Player Y should decrease during jump ascent",
                p1.getPositionY() < initialY);

        for (int i = 0; i < 80; i++) {
            p1.update(0, 900, groundY);
        }

        assertTrue("Player should land back on ground", p1.isOnGround());
        assertEquals("Player should be on the floor after landing",
                initialY, p1.getPositionY(), 0.1);
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

        assertTrue("Player 1 must remain to the left of Player 2",
                p1.getPositionX() < p2.getPositionX());

        double distance = p2.getPositionX() - p1.getPositionX();

        assertTrue("Players must not overlap horizontally",
                distance >= Player.HEAD_RADIUS * 2 - 0.5);
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

        assertTrue("Ball should fall due to gravity",
                ball.getPositionY() > 200);

        for (int i = 0; i < 60; i++) {
            ball.move(game.getField());
        }

        assertTrue("Ball must not penetrate the ground",
                ball.getPositionY() <= groundY - Ball.RADIUS + 0.1);
    }

    @Test
    public void testBallCeilingAndCrossbar() {
        HeadSoccer game = new HeadSoccer(900, 540);
        Ball ball = game.getBall();

        ball.setPosition(450, 25);
        ball.setVelocity(0, -10);

        ball.move(game.getField());
        ball.move(game.getField());

        assertTrue("Ball must not exceed the ceiling",
                ball.getPositionY() >= Ball.RADIUS);

        assertTrue("Ball should bounce downwards after ceiling hit",
                ball.getVelocityY() > 0);

        int leftGoalLine = game.getField().getLeftGoalLine();
        int goalTop = game.getField().getGoalTop();

        ball.setPosition(leftGoalLine / 2.0, goalTop - 30);
        ball.setVelocity(0, 8);

        for (int i = 0; i < 10; i++) {
            ball.move(game.getField());
        }

        assertTrue("Ball must bounce on the crossbar",
                ball.getPositionY() <= goalTop - Ball.RADIUS + 1.0);
    }

    @Test
    public void testPlayerBallCollisionHead() {
        HeadSoccer game = new HeadSoccer(900, 540);
        game.start();

        Player p1 = game.getPlayer1();
        Ball ball = game.getBall();

        ball.setPosition(p1.getPositionX(),
                p1.getPositionY() - 45);
        ball.setVelocity(0, 6);

        game.update();

        double dx = ball.getPositionX() - p1.getPositionX();
        double dy = ball.getPositionY() - p1.getPositionY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        double minDistance = Player.HEAD_RADIUS + Ball.RADIUS;

        assertTrue("Ball must be pushed outside player's head",
                distance >= minDistance - 0.5);

        assertTrue("Ball must receive an upward bounce",
                ball.getVelocityY() < 0);
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
        double distance = Math.sqrt(dx * dx + dy * dy);
        double minDistance = Player.FOOT_RADIUS + Ball.RADIUS;

        assertTrue("Ball must be pushed outside player's foot",
                distance >= minDistance - 0.5);
    }

    @Test
    public void testPlayerHit() {
        HeadSoccer game = new HeadSoccer(900, 540);
        Player p1 = game.getPlayer1();
        Ball ball = game.getBall();

        ball.setPosition(p1.getPositionX() + 35,
                p1.getPositionY());
        ball.setVelocity(0, 0);

        p1.hit(ball);

        assertTrue("Hit should give positive forward velocity",
                ball.getVelocityX() > 5.0);

        assertTrue("Hit should give upward velocity",
                ball.getVelocityY() < 0);
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

        assertEquals("Player 1 should have 0 goals",
                0, game.getScoreboard().getScorePlayer1());

        assertEquals("Player 2 should have scored 1 goal",
                1, game.getScoreboard().getScorePlayer2());

        assertTrue("Game should enter goal freeze",
                game.isGoalFreeze());
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

        assertEquals("Player 1 should have scored 1 goal",
                1, game.getScoreboard().getScorePlayer1());

        assertEquals("Player 2 should have 0 goals",
                0, game.getScoreboard().getScorePlayer2());

        assertTrue("Game should enter goal freeze",
                game.isGoalFreeze());
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

        assertEquals("Score should be 1",
                1, game.getScoreboard().getScorePlayer2());

        assertTrue("Game should enter goal freeze",
                game.isGoalFreeze());

        for (int i = 0; i < 85; i++) {
            game.update();
        }

        assertEquals("Goal must not be counted twice",
                1, game.getScoreboard().getScorePlayer2());

        assertFalse("Goal freeze should have ended",
                game.isGoalFreeze());

        assertEquals("Ball should return to center pitch",
                450.0, ball.getPositionX(), 1.0);
    }

    @Test
    public void testClockAndMatchEnd() {
        HeadSoccer game = new HeadSoccer(900, 540);
        game.start();

        assertEquals("Clock should start at 180 seconds",
                180, game.getRemainingTime());

        game.tickSecond();

        assertEquals("Clock should decrement by one second",
                179, game.getRemainingTime());

        game.setPaused(true);
        game.tickSecond();

        assertEquals("Clock must not decrease while paused",
                179, game.getRemainingTime());

        game.setPaused(false);

        while (game.getRemainingTime() > 0) {
            game.tickSecond();
        }

        assertEquals("Clock should reach zero",
                0, game.getRemainingTime());

        assertFalse("Game should finish when clock reaches zero",
                game.isRunning());
    }
    
    @Test
    public void testBallApplyImpulse() {
        Ball ball = new Ball(100, 100);

        ball.applyImpulse(5, -3);

        assertEquals(5.0, ball.getVelocityX(), 0.001);
        assertEquals(-3.0, ball.getVelocityY(), 0.001);
    }

    @Test
    public void testBallVelocityLimit() {
        Ball ball = new Ball(100, 100);

        ball.setVelocity(50, -50);

        assertEquals(16.0, ball.getVelocityX(), 0.001);
        assertEquals(-16.0, ball.getVelocityY(), 0.001);
    }

    @Test
    public void testBallReset() {
        Ball ball = new Ball(300, 200);

        ball.setVelocity(8, -5);
        ball.setPosition(500, 400);
        ball.reset();

        assertEquals(300.0, ball.getPositionX(), 0.001);
        assertEquals(200.0, ball.getPositionY(), 0.001);
        assertEquals(0.0, ball.getVelocityX(), 0.001);
        assertEquals(0.0, ball.getVelocityY(), 0.001);
    }

    @Test
    public void testBallMoveWithoutField() {
        Ball ball = new Ball(100, 100);

        ball.setVelocity(4, 0);
        ball.move();

        assertTrue("Ball should move horizontally",
                ball.getPositionX() > 100);
        assertTrue("Gravity should move the ball downward",
                ball.getPositionY() > 100);
    }

    @Test
    public void testFieldDimensions() {
        Field field = new Field(900, 540);

        assertEquals(900, field.getWidth());
        assertEquals(540, field.getHeight());
    }

    @Test
    public void testFieldGroundPosition() {
        Field field = new Field(900, 540);

        assertEquals(480, field.getGroundY());
    }

    @Test
    public void testFieldGoalLines() {
        Field field = new Field(900, 540);

        assertEquals(Field.GOAL_WIDTH, field.getLeftGoalLine());
        assertEquals(900 - Field.GOAL_WIDTH,
                field.getRightGoalLine());
    }

    @Test
    public void testBallInsideLeftGoal() {
        Field field = new Field(900, 540);
        Goal goal = field.getLeftGoal();
        Ball ball = new Ball(30, goal.getPositionY() + 50);

        assertTrue("Ball should be detected inside the left goal",
                goal.containsBall(ball));
    }

    @Test
    public void testBallOutsideGoalHeight() {
        Field field = new Field(900, 540);
        Goal goal = field.getLeftGoal();
        Ball ball = new Ball(30, goal.getPositionY() - 50);

        assertFalse("Ball should not be detected outside goal height",
                goal.containsBall(ball));
    }

    @Test
    public void testPlayerMovesRight() {
        Player player = new HeadSoccer(900, 540).getPlayer1();
        double initialX = player.getPositionX();

        player.setMovingRight(true);
        player.update(0, 900, 480);
        player.setMovingRight(false);

        assertTrue("Player should move to the right",
                player.getPositionX() > initialX);
    }

    @Test
    public void testPlayerMovesLeft() {
        Player player = new HeadSoccer(900, 540).getPlayer1();
        double initialX = player.getPositionX();

        player.setMovingLeft(true);
        player.update(0, 900, 480);
        player.setMovingLeft(false);

        assertTrue("Player should move to the left",
                player.getPositionX() < initialX);
    }

    @Test
    public void testPlayerFootPosition() {
        Player player = new HeadSoccer(900, 540).getPlayer1();

        assertEquals("Foot should be centered horizontally",
                player.getPositionX(), player.getFootX(), 0.001);

        assertEquals("Foot should be located below the head",
                player.getPositionY() + Player.HEAD_RADIUS,
                player.getFootY(), 0.001);
    }

    @Test
    public void testPlayerReset() {
        Player player = new HeadSoccer(900, 540).getPlayer1();
        double initialX = player.getPositionX();
        double initialY = player.getPositionY();

        player.setMovingRight(true);
        player.jump();
        player.update(0, 900, 480);
        player.reset();

        assertEquals(initialX, player.getPositionX(), 0.001);
        assertEquals(initialY, player.getPositionY(), 0.001);
        assertTrue(player.isOnGround());
        assertEquals(0.0, player.getVelocityY(), 0.001);
    }

    @Test
    public void testPlayerHasAssignedPower() {
        Player player = new HeadSoccer(900, 540).getPlayer1();

        assertNotNull("Player should have an assigned power",
                player.getPower());

        assertEquals("Boost", player.getPower().getName());
    }

    @Test
    public void testPowerActivation() {
        Power power = new Power("Test Power");

        assertFalse(power.isActive());

        power.activate();

        assertTrue(power.isActive());
    }

    @Test
    public void testPowerDeactivation() {
        Power power = new Power("Test Power");

        power.activate();
        power.deactivate();

        assertFalse(power.isActive());
    }

    @Test
    public void testScoreboardInitialScore() {
        Scoreboard scoreboard = new Scoreboard();

        assertEquals(0, scoreboard.getScorePlayer1());
        assertEquals(0, scoreboard.getScorePlayer2());
        assertEquals("0 - 0", scoreboard.getScoreText());
    }

    @Test
    public void testScoreboardPlayer1Scoring() {
        Scoreboard scoreboard = new Scoreboard();

        scoreboard.scoreGoalPlayer1();
        scoreboard.scoreGoalPlayer1();

        assertEquals(2, scoreboard.getScorePlayer1());
        assertEquals(0, scoreboard.getScorePlayer2());
        assertEquals("2 - 0", scoreboard.getScoreText());
    }

    @Test
    public void testScoreboardPlayer2Scoring() {
        Scoreboard scoreboard = new Scoreboard();

        scoreboard.scoreGoalPlayer2();

        assertEquals(0, scoreboard.getScorePlayer1());
        assertEquals(1, scoreboard.getScorePlayer2());
        assertEquals("0 - 1", scoreboard.getScoreText());
    }

    @Test
    public void testScoreboardReset() {
        Scoreboard scoreboard = new Scoreboard();

        scoreboard.scoreGoalPlayer1();
        scoreboard.scoreGoalPlayer2();
        scoreboard.scoreGoalPlayer2();
        scoreboard.reset();

        assertEquals(0, scoreboard.getScorePlayer1());
        assertEquals(0, scoreboard.getScorePlayer2());
        assertEquals("0 - 0", scoreboard.getScoreText());
    }

    @Test
    public void testGameInitialState() {
        HeadSoccer game = new HeadSoccer(900, 540);

        assertFalse(game.isRunning());
        assertFalse(game.isPaused());
        assertEquals(180, game.getRemainingTime());
        assertNotNull(game.getPlayer1());
        assertNotNull(game.getPlayer2());
        assertNotNull(game.getBall());
        assertNotNull(game.getField());
        assertNotNull(game.getScoreboard());
    }

    @Test
    public void testGamePauseState() {
        HeadSoccer game = new HeadSoccer(900, 540);

        game.start();
        game.setPaused(true);

        assertTrue(game.isRunning());
        assertTrue(game.isPaused());

        game.setPaused(false);

        assertFalse(game.isPaused());
    }

    @Test
    public void testGameFinish() {
        HeadSoccer game = new HeadSoccer(900, 540);

        game.start();
        game.finish();

        assertFalse("Game should no longer be running",
                game.isRunning());
    }
}

