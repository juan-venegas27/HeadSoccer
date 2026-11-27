package domain;

/** Special Ilusion power assigned to USA that splits the ball into decoy trajectories. */
public class IlusionPower extends Power {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the USA Ilusion power instance.
     */
    public IlusionPower() {
        super(POWER_ILUSION, COUNTRY_USA, EFFECT_CONFUSION, 2.5, 30, 13.5, -3.0);
    }

    /**
     * Launches the ball and spawns seven identical decoy balls across varied arcs.
     * @param ball active match ball
     * @param direction horizontal shot direction (+1 right, -1 left)
     */
    @Override
    public void onLaunch(Ball ball, int direction) {
        super.onLaunch(ball, direction);
        ball.spawnIllusionDecoys(getEffectDurationTicks());
    }

    /**
     * Applies standard gravity and air friction during the Illusion shot.
     * @param ball active match ball
     * @param gravity default ball gravity constant
     * @param airFriction default ball air friction factor
     */
    @Override
    public void applyFlightPhysics(Ball ball, double gravity, double airFriction) {
        ball.stepVelocity(gravity, airFriction);
    }

    /**
     * Creates a confusion effect masking the real ball from the opponent.
     * @param pushDir push direction (+1 right, -1 left)
     * @return configured confusion status effect
     */
    @Override
    public StatusEffect createStatusEffect(int pushDir) {
        return new ConfusionEffect(getEffectDurationTicks());
    }
}
