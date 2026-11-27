package domain;

import java.awt.Color;

/** Electrocute status effect that halves speed and disables jumping. */
public class ElectrocuteEffect extends AbstractStatusEffect {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an electrocute status effect with the given tick duration.
     * @param durationTicks effect duration in simulation ticks
     */
    public ElectrocuteEffect(int durationTicks) {
        super(Power.EFFECT_ELECTROCUTE, durationTicks);
    }

    /** Returns 0.5 to halve horizontal movement speed while electrocuted. */
    @Override
    public double getSpeedMultiplier() {
        return 0.5;
    }

    /** Returns true because jumping is blocked while electrocuted. */
    @Override
    public boolean blocksJump() {
        return true;
    }

    /** Returns yellow badge color for the electrocute indicator. */
    @Override
    public Color getBadgeColor() {
        return new Color(255, 240, 50);
    }

    /** Returns true so electric sparks appear around the electrocuted player. */
    @Override
    public boolean showsSparks() {
        return true;
    }
}
