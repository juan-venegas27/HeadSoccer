package domain;

/** Adaptive strategy that switches between defensive coverage and offensive attack by match state. */
public class ExpertaStrategy implements MachineStrategy {

    private static final long serialVersionUID = 1L;

    private final MachineStrategy defensiva = new DefensivaStrategy();
    private final MachineStrategy ofensiva = new OfensivaStrategy();

    /** Returns the Experta profile identifier. */
    @Override
    public String getProfileName() {
        return MachinePlayer.PROFILE_EXPERTA;
    }

    /**
     * Switches between defensive and offensive behavior based on score, ball direction, and rival stun state.
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
        int myScore = (scoreboard != null) ? scoreboard.getScorePlayer2() : 0;
        int oppScore = (scoreboard != null) ? scoreboard.getScorePlayer1() : 0;
        boolean ballComing = ball.getVelocityX() > 2.0 && ballX > field.getWidth() / 2.0;
        boolean rivalStunned = (opponent != null && opponent.isStunned());

        if (ballComing || (myScore > oppScore && !rivalStunned)) {
            defensiva.decideAction(self, ball, opponent, field, scoreboard);
        } else {
            ofensiva.decideAction(self, ball, opponent, field, scoreboard);
        }
    }
}
