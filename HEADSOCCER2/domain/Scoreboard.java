package domain;

import java.io.Serializable;

/** Tracks the score for both players during a match. */
public class Scoreboard implements Serializable {

    private static final long serialVersionUID = 1L;

    private int scorePlayer1;
    private int scorePlayer2;

    /** Creates a Scoreboard with both scores set to zero. */
    public Scoreboard() {
        this.scorePlayer1 = 0;
        this.scorePlayer2 = 0;
    }

    /** Registers a goal scored by player 1. */
    public void scoreGoalPlayer1() {
        scorePlayer1++;
    }

    /** Registers a goal scored by player 2. */
    public void scoreGoalPlayer2() {
        scorePlayer2++;
    }

    /** Resets both scores to zero. */
    public void reset() {
        scorePlayer1 = 0;
        scorePlayer2 = 0;
    }

    /** Returns current score for player 1. */
    public int getScorePlayer1() { return scorePlayer1; }

    /** Returns current score for player 2. */
    public int getScorePlayer2() { return scorePlayer2; }
}
