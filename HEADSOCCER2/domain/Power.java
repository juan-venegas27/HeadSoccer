package domain;

import java.io.Serializable;

/** Represents a country special power and its shot effect. */
public class Power implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String COUNTRY_KOREA = "Corea del Sur";
    public static final String COUNTRY_CAMEROON = "Camerún";
    public static final String COUNTRY_NIGERIA = "Nigeria";
    public static final String COUNTRY_USA = "Estados Unidos";

    public static final String POWER_AURA_AZUL = "Aura Azul";
    public static final String POWER_RAYO = "Rayo";
    public static final String POWER_TORNADO = "Tornado";
    public static final String POWER_ILUSION = "Ilusión";

    public static final String EFFECT_PUSH = "Empuje";
    public static final String EFFECT_ELECTROCUTE = "Electrocutado";
    public static final String EFFECT_UNCONSCIOUS = "Inconsciente";
    public static final String EFFECT_CONFUSION = "Confusión";

    public static final String[] AVAILABLE_COUNTRIES = {
        COUNTRY_KOREA, COUNTRY_CAMEROON, COUNTRY_NIGERIA, COUNTRY_USA
    };

    private final String name;
    private final String country;
    private final String effectName;
    private final double effectDurationSeconds;
    private final int baseDamage;
    private final double shotSpeedX;
    private final double shotSpeedY;
    private boolean active;

    /**
     * Creates a power with the given name.
     * @param name identifying name
     */
    public Power(String name) {
        this(name, COUNTRY_KOREA, EFFECT_PUSH, 1.5, 40, 22.0, 0.0);
    }

    /**
     * Creates a power with metadata and base damage.
     * @param name identifying name
     * @param country selected country name
     * @param effectName status effect identifier applied to rival
     * @param effectDurationSeconds effect duration in seconds
     * @param baseDamage base damage inflicted on opponent hit
     */
    public Power(String name, String country, String effectName, double effectDurationSeconds, int baseDamage) {
        this(name, country, effectName, effectDurationSeconds, baseDamage, 20.0, 0.0);
    }

    /**
     * Creates a power with full metadata and shot speeds.
     * @param name identifying name
     * @param country selected country name
     * @param effectName status effect identifier applied to rival
     * @param effectDurationSeconds effect duration in seconds
     * @param baseDamage base damage inflicted on opponent hit
     * @param shotSpeedX horizontal speed of the power shot
     * @param shotSpeedY vertical speed of the power shot
     */
    public Power(String name, String country, String effectName,
                 double effectDurationSeconds, int baseDamage,
                 double shotSpeedX, double shotSpeedY) {
        this.name = name;
        this.country = country;
        this.effectName = effectName;
        this.effectDurationSeconds = effectDurationSeconds;
        this.baseDamage = baseDamage;
        this.shotSpeedX = shotSpeedX;
        this.shotSpeedY = shotSpeedY;
        this.active = false;
    }

    /**
     * Creates the power assigned to the given country.
     * @param country selected country name
     * @return configured Power instance for the country
     */
    public static Power createForCountry(String country) {
        return CharacterRegistry.createPowerForCountry(country);
    }

    /**
     * Launches this power shot on the given ball in the specified horizontal direction.
     * @param ball active match ball
     * @param direction horizontal shot direction (+1 right, -1 left)
     */
    public void onLaunch(Ball ball, int direction) {
        if (ball != null) {
            ball.setVelocity(shotSpeedX * direction, shotSpeedY);
        }
    }

    /**
     * Applies active flight physics to the ball while this power shot is in flight.
     * @param ball active match ball
     * @param gravity default ball gravity constant
     * @param airFriction default ball air friction factor
     */
    public void applyFlightPhysics(Ball ball, double gravity, double airFriction) {
        if (ball != null) {
            ball.stepVelocity(0.05, 0.997);
        }
    }

    /**
     * Handles custom ground bounce physics when this power shot strikes the turf.
     * @param ball active match ball
     * @return true if this power handled the ground bounce, false for standard bounce
     */
    public boolean onGroundBounce(Ball ball) {
        return false;
    }

    /**
     * Creates the status effect applied when this power shot hits an opponent.
     * @param pushDir push direction (+1 right, -1 left)
     * @return matching status effect instance
     */
    public StatusEffect createStatusEffect(int pushDir) {
        return AbstractStatusEffect.createByName(effectName, getEffectDurationTicks(), pushDir);
    }

    /** Activates the power aura on the owning player. */
    public void activate() {
        this.active = true;
    }

    /** Deactivates the power aura (e.g. after executing the power shot). */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Returns total damage scaled by the kick multiplier.
     * @param kickMultiplier damage multiplier from player kick stat
     * @return total damage scaled by kick multiplier
     */
    public double calculateDamage(double kickMultiplier) {
        return baseDamage * kickMultiplier;
    }

    /** Returns true if the power aura is currently active. */
    public boolean isActive() { return active; }

    /** Returns the power name. */
    public String getName() { return name; }

    /** Returns the associated country name. */
    public String getCountry() { return country; }

    /** Returns the effect name inflicted on the opponent. */
    public String getEffectName() { return effectName; }

    /** Returns the effect duration in seconds. */
    public double getEffectDurationSeconds() { return effectDurationSeconds; }

    /** Returns the effect duration in 60-FPS ticks. */
    public int getEffectDurationTicks() {
        return (int) Math.round(effectDurationSeconds * 60.0);
    }

    /** Returns base damage value. */
    public int getBaseDamage() { return baseDamage; }

    /** Returns horizontal launch speed of this power shot. */
    public double getShotSpeedX() { return shotSpeedX; }

    /** Returns vertical launch speed of this power shot. */
    public double getShotSpeedY() { return shotSpeedY; }
}
