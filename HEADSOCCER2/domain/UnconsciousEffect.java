package domain;

import java.awt.Color;

/** Unconscious status effect that launches the rival upward and blocks control. */
public class UnconsciousEffect extends AbstractStatusEffect {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an unconscious status effect with the given tick duration.
     * @param durationTicks effect duration in simulation ticks
     */
    public UnconsciousEffect(int durationTicks) {
        super(Power.EFFECT_UNCONSCIOUS, durationTicks);
    }

    /** Returns true to block all player actions while unconscious. */
    @Override
    public boolean blocksAction() {
        return true;
    }

    /** Returns true to block jumping while unconscious. */
    @Override
    public boolean blocksJump() {
        return true;
    }

    /** Returns true because unconsciousness overrides directional player controls. */
    @Override
    public boolean overridesMovement() {
        return true;
    }

    /** Returns ice blue badge color for the unconscious indicator. */
    @Override
    public Color getBadgeColor() {
        return new Color(180, 240, 255);
    }

    /**
     * Launches the affected player into the air when unconsciousness begins.
     * @param target affected player instance
     */
    @Override
    public void onApply(Player target) {
        if (target != null) {
            target.launchUpward(-12.5);
        }
    }

    /**
     * Dampens residual horizontal speed on each tick while unconscious.
     * @param target affected player instance
     */
    @Override
    public void onTick(Player target) {
        super.onTick(target);
        if (target != null) {
            target.setRawVelocityX(target.getRawVelocityX() * 0.9);
        }
    }
}
