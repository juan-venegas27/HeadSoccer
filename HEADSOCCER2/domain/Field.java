package domain;

import java.io.Serializable;

/** Represents the playing field, its dimensions, and both goals. */
public class Field implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int RES_SMALL_WIDTH = 800;
    public static final int RES_SMALL_HEIGHT = 480;

    public static final int RES_MEDIUM_WIDTH = 900;
    public static final int RES_MEDIUM_HEIGHT = 540;

    public static final int RES_LARGE_WIDTH = 1050;
    public static final int RES_LARGE_HEIGHT = 630;

    public static final int GOAL_WIDTH = 70;
    public static final int GOAL_HEIGHT = 130;
    public static final int GROUND_HEIGHT = 60;

    private final int width;
    private final int height;
    private final int groundY;
    private final Goal leftGoal;
    private final Goal rightGoal;

    /** Creates a playing field with the given dimensions. */
    public Field(int width, int height) {
        this.width = width;
        this.height = height;
        this.groundY = height - GROUND_HEIGHT;

        int goalTop = groundY - GOAL_HEIGHT;
        this.leftGoal = new Goal(0, goalTop, GOAL_WIDTH, GOAL_HEIGHT, true);
        this.rightGoal = new Goal(width - GOAL_WIDTH, goalTop, GOAL_WIDTH, GOAL_HEIGHT, false);
    }

    /** @return total width of the field in pixels */
    public int getWidth() {
        return width;
    }

    /** @return total height of the field in pixels */
    public int getHeight() {
        return height;
    }

    /** @return Y coordinate of the ground surface (top of the green strip) */
    public int getGroundY() {
        return groundY;
    }

    /** @return Y coordinate of the goal crossbar */
    public int getGoalTop() {
        return groundY - GOAL_HEIGHT;
    }

    /** @return X coordinate of the left goal line (boundary of left goal mouth) */
    public int getLeftGoalLine() {
        return leftGoal.getPositionX() + leftGoal.getWidth();
    }

    /** @return X coordinate of the right goal line */
    public int getRightGoalLine() {
        return rightGoal.getPositionX();
    }

    /** @return the left goal */
    public Goal getLeftGoal() {
        return leftGoal;
    }

    /** @return the right goal */
    public Goal getRightGoal() {
        return rightGoal;
    }
}
