package domain;

/** Special Tornado power assigned to Nigeria that bounces rapidly along the ground. */
public class TornadoPower extends Power {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the Nigeria Tornado power instance.
     */
    public TornadoPower() {
        super(POWER_TORNADO, COUNTRY_NIGERIA, EFFECT_UNCONSCIOUS, 3.0, 40, 14.0, 6.0);
    }

    /**
     * Applies amplified downward acceleration so the ball skips rapidly across the turf.
     * @param ball active match ball
     * @param gravity default ball gravity constant
     * @param airFriction default ball air friction factor
     */
    @Override
    public void applyFlightPhysics(Ball ball, double gravity, double airFriction) {
        ball.stepVelocity(gravity * 1.4, 0.995);
    }

    /**
     * Forces an upward ground bounce velocity of -7.5 while Tornado is active.
     * @param ball active match ball
     * @return true indicating Tornado overrode standard ground bounce
     */
    @Override
    public boolean onGroundBounce(Ball ball) {
        ball.setVelocity(ball.getVelocityX(), -7.5);
        return true;
    }

    /**
     * Creates an unconscious effect launching the rival upward and blocking actions.
     * @param pushDir push direction (+1 right, -1 left)
     * @return configured unconscious status effect
     */
    @Override
    public StatusEffect createStatusEffect(int pushDir) {
        return new UnconsciousEffect(getEffectDurationTicks());
    }
}
