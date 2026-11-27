package domain;

/** Special Rayo power assigned to Cameroon that fires an electric straight shot. */
public class RayoPower extends Power {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the Cameroon Rayo power instance.
     */
    public RayoPower() {
        super(POWER_RAYO, COUNTRY_CAMEROON, EFFECT_ELECTROCUTE, 3.0, 60, 16.5, 0.0);
    }

    /**
     * Applies low-drop horizontal flight physics while the Rayo shot is active.
     * @param ball active match ball
     * @param gravity default ball gravity constant
     * @param airFriction default ball air friction factor
     */
    @Override
    public void applyFlightPhysics(Ball ball, double gravity, double airFriction) {
        ball.stepVelocity(0.05, 0.997);
    }

    /**
     * Creates an electrocute effect reducing speed and disabling jumps.
     * @param pushDir push direction (+1 right, -1 left)
     * @return configured electrocute status effect
     */
    @Override
    public StatusEffect createStatusEffect(int pushDir) {
        return new ElectrocuteEffect(getEffectDurationTicks());
    }
}
