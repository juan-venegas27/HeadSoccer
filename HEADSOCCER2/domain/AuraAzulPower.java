package domain;

/** Special Aura Azul power assigned to Korea that fires a fast horizontal shot with push effect. */
public class AuraAzulPower extends Power {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the default Korea Aura Azul power instance.
     */
    public AuraAzulPower() {
        this(COUNTRY_KOREA);
    }

    /**
     * Creates an Aura Azul power bound to the specified country name.
     * @param country selected country name
     */
    public AuraAzulPower(String country) {
        super(POWER_AURA_AZUL, country, EFFECT_PUSH, 1.5, 40, 20.0, 0.0);
    }

    /**
     * Applies low-drop horizontal flight physics while the Aura Azul shot is active.
     * @param ball active match ball
     * @param gravity default ball gravity constant
     * @param airFriction default ball air friction factor
     */
    @Override
    public void applyFlightPhysics(Ball ball, double gravity, double airFriction) {
        ball.stepVelocity(0.05, 0.997);
    }

    /**
     * Creates a push effect driving the opponent toward their own goal.
     * @param pushDir push direction (+1 right, -1 left)
     * @return configured push status effect
     */
    @Override
    public StatusEffect createStatusEffect(int pushDir) {
        return new PushEffect(getEffectDurationTicks(), pushDir);
    }
}
