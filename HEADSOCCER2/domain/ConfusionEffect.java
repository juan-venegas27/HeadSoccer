package domain;

import java.awt.Color;

/** Confusion status effect associated with Illusion decoy balls. */
public class ConfusionEffect extends AbstractStatusEffect {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a confusion status effect with the standard Confusion name.
     * @param durationTicks effect duration in simulation ticks
     */
    public ConfusionEffect(int durationTicks) {
        super(Power.EFFECT_CONFUSION, durationTicks);
    }

    /**
     * Creates a passive timed status effect with a custom effect name.
     * @param customName custom status effect identifier
     * @param durationTicks effect duration in simulation ticks
     */
    public ConfusionEffect(String customName, int durationTicks) {
        super(customName, durationTicks);
    }

    /** Returns pink badge color for the confusion indicator. */
    @Override
    public Color getBadgeColor() {
        return new Color(255, 160, 255);
    }

    /** Returns true so opponent AI can chase Illusion decoys while confused. */
    @Override
    public boolean confusesVision() {
        return true;
    }
}
