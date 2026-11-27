package domain;

/** Countdown clock match rule with automatic sudden death overtime on ties. */
public class TimeMatchRule implements MatchRule {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a timed match rule with countdown clock and sudden death tiebreaker.
     */
    public TimeMatchRule() {
    }

    /** Returns the timed match type constant. */
    @Override
    public int getMatchTypeCode() {
        return HeadSoccer.MATCH_BY_TIME;
    }

    /** Returns true because this rule counts down match seconds. */
    @Override
    public boolean usesCountdownClock() {
        return true;
    }

    /**
     * Decrements remaining time and triggers sudden death or match completion at zero.
     * @param match active match model
     * @return true when regulation time expires with a non-tied score
     */
    @Override
    public boolean onSecondElapsed(HeadSoccer match) {
        if (match == null || match.isSuddenDeath()) {
            return false;
        }
        match.decrementRemainingTime();
        if (match.getRemainingTime() == 0) {
            if (match.getScoreboard().getScorePlayer1() == match.getScoreboard().getScorePlayer2()) {
                match.activateSuddenDeath();
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Ends the match immediately after a golden goal in sudden death overtime.
     * @param match active match model
     * @return true if sudden death is active
     */
    @Override
    public boolean shouldFinishAfterGoal(HeadSoccer match) {
        return match != null && match.isSuddenDeath();
    }

    /**
     * Formats remaining regulation time or sudden death notice for the HUD.
     * @param match active match model
     * @return formatted MM:SS countdown or sudden death banner
     */
    @Override
    public String getHudStatusText(HeadSoccer match) {
        if (match == null) {
            return "00:00";
        }
        if (match.isSuddenDeath()) {
            return "¡MUERTE SÚBITA!";
        }
        int rem = match.getRemainingTime();
        return String.format("%02d:%02d", rem / 60, rem % 60);
    }
}
