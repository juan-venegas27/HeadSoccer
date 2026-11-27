package domain;

import java.awt.Color;

/** Base timed implementation for status effects applied to players. */
public abstract class AbstractStatusEffect implements StatusEffect {

    private static final long serialVersionUID = 1L;

    private final String name;
    private int remainingTicks;

    /**
     * Creates a status effect with the specified name and duration in ticks.
     * @param name status effect identifier
     * @param durationTicks effect duration in ticks
     */
    protected AbstractStatusEffect(String name, int durationTicks) {
        this.name = name;
        this.remainingTicks = Math.max(1, durationTicks);
    }

    /** Returns the status effect identifier. */
    @Override
    public String getName() {
        return name;
    }

    /** Returns true when no ticks remain for this effect. */
    @Override
    public boolean isExpired() {
        return remainingTicks <= 0;
    }

    /**
     * Applies optional initial effect behavior to the target player.
     * @param target affected player instance
     */
    @Override
    public void onApply(Player target) {
    }

    /**
     * Decrements remaining ticks by one on each simulation step.
     * @param target affected player instance
     */
    @Override
    public void onTick(Player target) {
        if (remainingTicks > 0) {
            remainingTicks--;
        }
    }

    /** Returns the default horizontal speed multiplier of 1.0. */
    @Override
    public double getSpeedMultiplier() {
        return 1.0;
    }

    /** Returns false by default so jumping remains enabled. */
    @Override
    public boolean blocksJump() {
        return false;
    }

    /** Returns false by default so normal player actions remain enabled. */
    @Override
    public boolean blocksAction() {
        return false;
    }

    /** Returns false by default so normal player controls apply. */
    @Override
    public boolean overridesMovement() {
        return false;
    }

    /** Returns cyan as default status badge color. */
    @Override
    public Color getBadgeColor() {
        return Color.CYAN;
    }

    /** Returns false by default so electric sparks are not drawn. */
    @Override
    public boolean showsSparks() {
        return false;
    }

    /** Returns false by default so opponent ball perception is unaffected. */
    @Override
    public boolean confusesVision() {
        return false;
    }

    /**
     * Creates a status effect instance matching the given effect name.
     * @param effectName effect identifier
     * @param durationTicks duration in simulation ticks
     * @param pushDir push direction (+1 right, -1 left)
     * @return matching status effect instance
     */
    public static StatusEffect createByName(String effectName, int durationTicks, int pushDir) {
        if (Power.EFFECT_PUSH.equals(effectName)) {
            return new PushEffect(durationTicks, pushDir);
        }
        if (Power.EFFECT_ELECTROCUTE.equals(effectName)) {
            return new ElectrocuteEffect(durationTicks);
        }
        if (Power.EFFECT_UNCONSCIOUS.equals(effectName)) {
            return new UnconsciousEffect(durationTicks);
        }
        if (Power.EFFECT_CONFUSION.equals(effectName)) {
            return new ConfusionEffect(durationTicks);
        }
        return new ConfusionEffect(effectName, durationTicks);
    }
}
