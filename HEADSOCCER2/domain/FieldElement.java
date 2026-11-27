package domain;

import java.awt.Color;
import java.io.Serializable;

/** Represents an extensible dynamic element or obstacle inside the playing field. */
public interface FieldElement extends Serializable {

    /** Returns element identifier shown in the field. */
    String getName();

    /** Returns horizontal center position in pixels. */
    double getPositionX();

    /** Returns vertical center position in pixels. */
    double getPositionY();

    /** Returns collision radius in pixels. */
    int getRadius();

    /** Returns primary display color used to render this element. */
    Color getDisplayColor();

    /** Returns true while this element remains active in the arena. */
    boolean isActive();

    /**
     * Updates internal element state on each simulation tick.
     * @param match active match model
     */
    void update(HeadSoccer match);

    /**
     * Handles collision between this element and the match ball.
     * @param ball active match ball
     * @param match active match model
     */
    void onBallTouch(Ball ball, HeadSoccer match);

    /**
     * Handles collision between this element and a player.
     * @param player touching player instance
     * @param match active match model
     */
    void onPlayerTouch(Player player, HeadSoccer match);
}
