package domain;

import java.awt.Color;

/** Base implementation for extensible dynamic elements placed inside the playing field. */
public abstract class AbstractFieldElement implements FieldElement {

    private static final long serialVersionUID = 1L;

    private final String name;
    private double positionX;
    private double positionY;
    private final int radius;
    private final Color displayColor;
    private boolean active;

    /**
     * Creates a dynamic arena element at the given center coordinates and collision radius.
     * @param name element identifier shown in the arena
     * @param positionX horizontal center position in pixels
     * @param positionY vertical center position in pixels
     * @param radius collision radius in pixels
     * @param displayColor primary color used to draw this element
     */
    protected AbstractFieldElement(String name, double positionX, double positionY, int radius, Color displayColor) {
        this.name = (name != null) ? name : "";
        this.positionX = positionX;
        this.positionY = positionY;
        this.radius = Math.max(4, radius);
        this.displayColor = (displayColor != null) ? displayColor : Color.ORANGE;
        this.active = true;
    }

    /** Returns element identifier shown in the field. */
    @Override
    public String getName() {
        return name;
    }

    /** Returns horizontal center position in pixels. */
    @Override
    public double getPositionX() {
        return positionX;
    }

    /** Returns vertical center position in pixels. */
    @Override
    public double getPositionY() {
        return positionY;
    }

    /** Returns collision radius in pixels. */
    @Override
    public int getRadius() {
        return radius;
    }

    /** Returns primary display color used to render this element. */
    @Override
    public Color getDisplayColor() {
        return displayColor;
    }

    /** Returns true while this element remains active in the arena. */
    @Override
    public boolean isActive() {
        return active;
    }

    /** Deactivates this element so it is removed from the match arena. */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Moves this element to new center coordinates.
     * @param x horizontal coordinate
     * @param y vertical coordinate
     */
    public void setPosition(double x, double y) {
        this.positionX = x;
        this.positionY = y;
    }

    /**
     * Performs per-tick element behavior when overridden by subclasses.
     * @param match active match model
     */
    @Override
    public void update(HeadSoccer match) {
    }

    /**
     * Handles ball collision when overridden by subclasses.
     * @param ball active match ball
     * @param match active match model
     */
    @Override
    public void onBallTouch(Ball ball, HeadSoccer match) {
    }

    /**
     * Handles player collision when overridden by subclasses.
     * @param player touching player instance
     * @param match active match model
     */
    @Override
    public void onPlayerTouch(Player player, HeadSoccer match) {
    }
}
