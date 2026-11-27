package domain;

import java.awt.Color;
import java.io.Serializable;

/** Represents a temporary rival status effect applied by a power shot. */
public interface StatusEffect extends Serializable {

    /** Returns the status effect name shown in the interface. */
    String getName();

    /** Returns true once the effect duration has expired. */
    boolean isExpired();

    /**
     * Executes one-time behavior when the effect is first applied to a player.
     * @param target affected player instance
     */
    void onApply(Player target);

    /**
     * Advances the effect timer and per-tick behavior on the affected player.
     * @param target affected player instance
     */
    void onTick(Player target);

    /** Returns the horizontal movement speed multiplier while active. */
    double getSpeedMultiplier();

    /** Returns true if jumping is prevented while this effect is active. */
    boolean blocksJump();

    /** Returns true if all player actions are blocked while this effect is active. */
    boolean blocksAction();

    /** Returns true if this effect overrides normal horizontal player movement. */
    boolean overridesMovement();

    /** Returns badge text color displayed above an affected player. */
    Color getBadgeColor();

    /** Returns true if electric spark lines should be drawn around the player. */
    boolean showsSparks();

    /** Returns true if opponent decoys should confuse AI ball perception. */
    boolean confusesVision();
}
