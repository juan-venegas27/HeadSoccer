package domain;

/** Push status effect that knocks the rival toward their own goal. */
public class PushEffect extends AbstractStatusEffect {

    private static final long serialVersionUID = 1L;

    private double pushVelocityX;

    /**
     * Creates a push effect with the given duration and horizontal direction.
     * @param durationTicks effect duration in simulation ticks
     * @param pushDir push direction (+1 right, -1 left)
     */
    public PushEffect(int durationTicks, int pushDir) {
        super(Power.EFFECT_PUSH, durationTicks);
        this.pushVelocityX = pushDir * 11.0;
    }

    /** Returns true because push movement overrides player controls. */
    @Override
    public boolean overridesMovement() {
        return true;
    }

    /** Returns true to prevent jumping while pushed. */
    @Override
    public boolean blocksJump() {
        return true;
    }

    /**
     * Applies decaying horizontal knockback velocity on each simulation tick.
     * @param target affected player instance
     */
    @Override
    public void onTick(Player target) {
        super.onTick(target);
        if (target == null) {
            return;
        }
        if (isExpired()) {
            target.setRawVelocityX(0);
        } else {
            target.setRawVelocityX(pushVelocityX);
            pushVelocityX = pushVelocityX * 0.92;
        }
    }
}
