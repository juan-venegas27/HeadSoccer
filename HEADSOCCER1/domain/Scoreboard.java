package domain;

/**
 * Tracks the score of both players during a Head Soccer game.
 */
public class Scoreboard {

    private int scorePlayer1;
    private int scorePlayer2;

    /**
     * Creates a Scoreboard with both scores set to zero.
     */
    public Scoreboard() {
        this.scorePlayer1 = 0;
        this.scorePlayer2 = 0;
    }

    /**
     * Registers a goal scored by player 1.
     */
    public void scoreGoalPlayer1() {
        scorePlayer1++;
    }

    /**
     * Registers a goal scored by player 2.
     */
    public void scoreGoalPlayer2() {
        scorePlayer2++;
    }

    /**
     * Resets both scores to zero.
     */
    public void reset() {
        scorePlayer1 = 0;
        scorePlayer2 = 0;
    }

    /** @return current score for player 1 */
    public int getScorePlayer1() { return scorePlayer1; }

    /** @return current score for player 2 */
    public int getScorePlayer2() { return scorePlayer2; }

    /**
     * Returns a formatted score string.
     *
     * @return e.g. "2 - 1"
     */
    public String getScoreText() {
        return scorePlayer1 + " - " + scorePlayer2;
    }
}
