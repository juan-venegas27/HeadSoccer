package domain;

import java.io.Serializable;

/** Represents a goal area and detects scored goals. */
public class Goal implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int positionX;
    private final int positionY;
    private final int width;
    private final int height;
    private final boolean leftGoal;

    /** Creates a goal at the specified position and size. */
    public Goal(int positionX, int positionY, int width, int height, boolean leftGoal) {
        this.positionX = positionX;
        this.positionY = positionY;
        this.width = width;
        this.height = height;
        this.leftGoal = leftGoal;
    }

    /** @return true if the ball is completely inside the goal */
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
