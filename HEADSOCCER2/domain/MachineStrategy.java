package domain;

import java.io.Serializable;

/** Strategy interface defining AI decision logic for a machine-controlled player. */
public interface MachineStrategy extends Serializable {

    /** Returns the display profile name handled by this strategy. */
    String getProfileName();

    /**
     * Selects movement, jump, kick, and power actions for the current tick.
     * @param self controlled machine player instance
     * @param ball active match ball
     * @param opponent opposing player instance
     * @param field playing field boundaries
     * @param scoreboard match scoreboard
     */
    void decideAction(MachinePlayer self, Ball ball, Player opponent, Field field, Scoreboard scoreboard);
}
