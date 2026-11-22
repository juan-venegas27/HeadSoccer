package src.domain;

import java.io.Serializable;

/**
 * Represents a goal on the playing field.
 * Handles the geometry of the goal area and detects when a goal is scored.
 */
public class Goal implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int positionX;
    private final int positionY;
    private final int width;
    private final int height;
    private final boolean leftGoal;

    /**
     * Creates a Goal at the specified position and dimensions.
     *
     * @param positionX left edge X coordinate of the goal
     * @param positionY top edge Y coordinate of the goal (crossbar)
     * @param width     width (depth) of the goal opening
     * @param height    height of the goal opening
     * @param leftGoal  true if this is the left goal, false if right goal
     */
    public Goal(int positionX, int positionY, int width, int height, boolean leftGoal) {
        this.positionX = positionX;
        this.positionY = positionY;
        this.width     = width;
        this.height    = height;
        this.leftGoal  = leftGoal;
    }

    /**
     * Determines whether the given ball has completely crossed the goal line
     * and is inside the goal opening.
     *
     * @param ball the ball to check
     * @return true if the entire ball has crossed the goal line
     */
    public boolean containsBall(Ball ball) {
        double bx = ball.getPositionX();
        double by = ball.getPositionY();
        int br = Ball.RADIUS;

        if (by - br < positionY - 2 || by + br > positionY + height + 5) {
            return false;
        }

        if (leftGoal) {
            int goalLine = positionX + width;
            return (bx + br <= goalLine) && (bx - br >= positionX - 10);
        } else {
            int goalLine = positionX;
            return (bx - br >= goalLine) && (bx + br <= positionX + width + 10);
        }
    }

    /** @return left X coordinate of the goal */
    public int getPositionX() {
        return positionX;
    }

    /** @return top Y coordinate of the goal (crossbar) */
    public int getPositionY() {
        return positionY;
    }

    /** @return width (depth) of the goal opening */
    public int getWidth() {
        return width;
    }

    /** @return height of the goal opening */
    public int getHeight() {
        return height;
    }
}
