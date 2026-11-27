package domain;

import java.util.Random;

/** Random decision strategy for the Torpe machine profile. */
public class TorpeStrategy implements MachineStrategy {

    private static final long serialVersionUID = 1L;

    private final Random random;

    /**
     * Creates a random Torpe machine strategy instance.
     */
    public TorpeStrategy() {
        this.random = new Random();
    }

    /** Returns the Torpe profile identifier. */
    @Override
    public String getProfileName() {
        return MachinePlayer.PROFILE_TORPE;
    }

    /**
     * Picks random movement, jump, kick, and power actions every 20 ticks.
     * @param self controlled machine player instance
     * @param ball active match ball
     * @param opponent opposing player instance
     * @param field playing field boundaries
     * @param scoreboard match scoreboard
     */
    @Override
    public void decideAction(MachinePlayer self, Ball ball, Player opponent, Field field, Scoreboard scoreboard) {
        if (self.getDecisionTicks() % 20 == 0) {
            int choice = random.nextInt(3);
            self.setMovingLeft(choice == 0);
            self.setMovingRight(choice == 1);
            if (random.nextDouble() < 0.25) self.jump();
            if (random.nextDouble() < 0.30) self.hit(ball, opponent);
            if (self.getPowerBar() >= 100.0 && random.nextDouble() < 0.4) self.activatePower();
        }
    }
}
