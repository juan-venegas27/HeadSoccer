package domain;

/** Defensive goalkeeper strategy that protects its own half and clears incoming shots. */
public class DefensivaStrategy implements MachineStrategy {

    private static final long serialVersionUID = 1L;

    /** Returns the Defensiva profile identifier. */
    @Override
    public String getProfileName() {
        return MachinePlayer.PROFILE_DEFENSIVA;
    }

    /**
     * Defends near its goal line and intercepts or kicks balls entering its own half.
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
        double midX = field.getWidth() / 2.0;
        if (ballX > midX) {
            self.moveTowards(ballX + 16.0, 8.0);
            if (ballY < self.getPositionY() - 25 && Math.abs(ballX - self.getPositionX()) < 75) {
                self.jump();
            }
            if (self.getPowerBar() >= 100.0 && !self.getPower().isActive()) {
                self.activatePower();
            }
            if (self.isBallNear(ball)) {
                self.hit(ball, opponent);
            }
        } else {
            self.moveTowards(field.getRightGoalLine() - 70.0, 10.0);
        }
    }
}
