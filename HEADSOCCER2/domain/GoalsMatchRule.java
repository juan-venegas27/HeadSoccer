package domain;

/** Target-goal match rule that finishes as soon as a player reaches the goal limit. */
public class GoalsMatchRule implements MatchRule {

    private static final long serialVersionUID = 1L;

    private final int targetGoals;

    /**
     * Creates a target-goal match rule requiring at least one goal to win.
     * @param targetGoals goal count required to win
     */
    public GoalsMatchRule(int targetGoals) {
        this.targetGoals = Math.max(1, targetGoals);
    }

    /** Returns the goal count required to win under this rule. */
    public int getTargetGoals() {
        return targetGoals;
    }

    /** Returns the target-goals match type constant. */
    @Override
    public int getMatchTypeCode() {
        return HeadSoccer.MATCH_BY_GOALS;
    }

    /** Returns false because target-goal matches do not expire by timer. */
    @Override
    public boolean usesCountdownClock() {
        return false;
    }

    /**
     * Ignores elapsed seconds because victory depends solely on goals scored.
     * @param match active match model
     * @return false so timer ticks never end the match
     */
    @Override
    public boolean onSecondElapsed(HeadSoccer match) {
        return false;
    }

    /**
     * Returns true once either player reaches targetGoals or scores in sudden death.
     * @param match active match model
     * @return true if the goal target has been reached
     */
    @Override
    public boolean shouldFinishAfterGoal(HeadSoccer match) {
        return match != null && (match.isSuddenDeath()
                || match.getScoreboard().getScorePlayer1() >= targetGoals
                || match.getScoreboard().getScorePlayer2() >= targetGoals);
    }

    /**
     * Formats the goal target label or sudden death notice for the HUD.
     * @param match active match model
     * @return target-goals HUD label
     */
    @Override
    public String getHudStatusText(HeadSoccer match) {
        if (match != null && match.isSuddenDeath()) {
            return "¡MUERTE SÚBITA!";
        }
        return "META: " + targetGoals + " GOLES";
    }
}
