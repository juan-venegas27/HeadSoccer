package domain;

/** Aggressive attack strategy that pursues the ball toward the rival goal. */
public class OfensivaStrategy implements MachineStrategy {

    private static final long serialVersionUID = 1L;

    /** Returns the Ofensiva profile identifier. */
    @Override
    public String getProfileName() {
        return MachinePlayer.PROFILE_OFENSIVA;
    }

    /**
     * Pursues the ball toward the opponent goal, jumps for aerial balls, kicks, and activates powers.
     * @param self controlled machine player instance
     * @param ball active match ball
     * @param opponent opposing player instance
     * @param field playing field boundaries
     * @param scoreboard match scoreboard
     */
    @Override
    public void decideAction(MachinePlayer self, Ball ball, Player opponent, Field field, Scoreboard scoreboard) {
        double[] perceived = self.getPerceivedBallPosition(ball);
        double ballX = perceived[0];
        double ballY = perceived[1];
        self.moveTowards(ballX + 20.0, 10.0);
        if (ballY < self.getPositionY() - 25 && Math.abs(ballX - self.getPositionX()) < 75) {
            self.jump();
        }
        if (self.getPowerBar() >= 100.0 && !self.getPower().isActive()) {
            self.activatePower();
        }
        if (self.isBallNear(ball) || self.isOpponentNear(opponent)) {
            self.hit(ball, opponent);
        }
    }
}
