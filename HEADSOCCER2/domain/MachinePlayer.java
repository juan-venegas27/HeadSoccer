package domain;

import java.awt.Color;
import java.io.Serializable;
import java.util.Random;

/** AI-controlled player with random Torpe behavior. */
public class MachinePlayer extends Player implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String PROFILE_TORPE = "Torpe";
    public static final String[] PROFILES = {PROFILE_TORPE};

    private final String profile;
    private final Random random;
    private int decisionTicks;

    /** Creates a machine player with the Torpe AI profile. */
    public MachinePlayer(double x, double y, Color color, String name,
                         Power power, int facingDirection, String profile) {
        super(x, y, color, name, power, facingDirection);
        this.profile = PROFILE_TORPE;
        this.random = new Random();
        this.decisionTicks = 0;
    }

    /** Chooses random movements, jumps, kicks, and power usage for the current tick. */
    public void decideAction(Ball ball, Player opponent, Field field, Scoreboard scoreboard) {
        if (!canAct() || ball == null || field == null) {
            setMovingLeft(false);
            setMovingRight(false);
            return;
        }

        decisionTicks++;
        if (decisionTicks % 20 == 0) {
            int choice = random.nextInt(3);
            setMovingLeft(choice == 0);
            setMovingRight(choice == 1);
            if (random.nextDouble() < 0.25) jump();
            if (random.nextDouble() < 0.30) hit(ball, opponent);
            if (getPowerBar() >= 100.0 && random.nextDouble() < 0.4) activatePower();
        }
    }

    /** Returns the current machine profile. */
    public String getProfile() {
        return profile;
    }
}
