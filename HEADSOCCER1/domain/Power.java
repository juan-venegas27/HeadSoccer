package domain;

/**
 * Represents a special power that a player can activate during the game.
 * The specific effect of the power is intended to be implemented in future versions.
 */
public class Power {

    private final String name;
    private boolean active;

    /**
     * Creates a Power with the given name.
     *
     * @param name the name of the power
     */
    public Power(String name) {
        this.name = name;
        this.active = false;
    }

    /**
     * Activates the power. The concrete effect should be implemented in subclasses
     * or by the game logic when this power is triggered.
     */
    public void activate() {
        this.active = true;
    }

    /**
     * Deactivates the power.
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Returns whether this power is currently active.
     *
     * @return true if the power is active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Returns the name of this power.
     *
     * @return the power name
     */
    public String getName() {
        return name;
    }
}
