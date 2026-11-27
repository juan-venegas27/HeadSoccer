package domain;

import java.io.Serializable;

/** Represents extensible victory, clock countdown, and HUD status rules for a match. */
public interface MatchRule extends Serializable {

    /** Returns numeric match type constant associated with this rule. */
    int getMatchTypeCode();

    /** Returns true if this rule uses the countdown timer. */
    boolean usesCountdownClock();

    /**
     * Processes one elapsed second and returns true if the match should finish.
     * @param match active match model
     * @return true if the match ended after this second tick
     */
    boolean onSecondElapsed(HeadSoccer match);

    /**
     * Checks whether the match should finish once a post-goal freeze completes.
     * @param match active match model
     * @return true if post-goal victory conditions are met
     */
    boolean shouldFinishAfterGoal(HeadSoccer match);

    /**
     * Returns HUD status label displayed under the scoreboard score.
     * @param match active match model
     * @return formatted clock or rule status string
     */
    String getHudStatusText(HeadSoccer match);
}
