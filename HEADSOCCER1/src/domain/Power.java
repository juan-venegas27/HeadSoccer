package src.domain;

import java.io.Serializable;

/**
 * Represents a special power that a player can activate during the game.
 * Strictly implements the four characters/countries from DOPOSoccer Annex 1:
 * Corea del Sur, Camerún, Nigeria, and Estados Unidos.
 */
public class Power implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String COUNTRY_KOREA    = "Corea del Sur";
    public static final String COUNTRY_CAMEROON = "Camerún";
    public static final String COUNTRY_NIGERIA  = "Nigeria";
    public static final String COUNTRY_USA      = "Estados Unidos";

    public static final String POWER_AURA_AZUL = "Aura Azul";
    public static final String POWER_RAYO      = "Rayo";
    public static final String POWER_TORNADO   = "Tornado";
    public static final String POWER_ILUSION   = "Ilusión";

    public static final String EFFECT_PUSH        = "Empuje";
    public static final String EFFECT_ELECTROCUTE = "Electrocutado";
    public static final String EFFECT_UNCONSCIOUS = "Inconsciente";
    public static final String EFFECT_CONFUSION   = "Confusión";

    public static final String[] AVAILABLE_COUNTRIES = {
        COUNTRY_KOREA, COUNTRY_CAMEROON, COUNTRY_NIGERIA, COUNTRY_USA
    };

    private final String name;
    private final String country;
    private final String effectName;
    private final double effectDurationSeconds;
    private final int    baseDamage;
    private final double shotSpeedX;
    private final double shotSpeedY;
    private boolean      active;

    /**
     * Backward-compatible constructor creating a Power with only a name.
     *
     * @param name the name of the power
     */
    public Power(String name) {
        this(name, COUNTRY_KOREA, EFFECT_PUSH, 1.5, 40, 22.0, 0.0);
    }

    /**
     * Creates a Power with full country, effect, duration, and base damage metadata.
     *
     * @param name                  name of the power shot
     * @param country               country/character associated with this power
     * @param effectName            effect applied when hitting the opponent
     * @param effectDurationSeconds duration of the effect in seconds
     * @param baseDamage            base damage (DañoTiro * 5)
     */
    public Power(String name, String country, String effectName, double effectDurationSeconds, int baseDamage) {
        this(name, country, effectName, effectDurationSeconds, baseDamage, 20.0, 0.0);
    }

    /**
     * Creates a Power with full configuration including shot trajectory speeds.
     *
     * @param name                  name of the power shot
     * @param country               country/character associated with this power
     * @param effectName            effect applied when hitting the opponent
     * @param effectDurationSeconds duration of the effect in seconds
     * @param baseDamage            base damage (DañoTiro * 5)
     * @param shotSpeedX            horizontal shot speed
     * @param shotSpeedY            vertical shot speed
     */
    public Power(String name, String country, String effectName,
                 double effectDurationSeconds, int baseDamage,
                 double shotSpeedX, double shotSpeedY) {
        this.name                  = name;
        this.country               = country;
        this.effectName            = effectName;
        this.effectDurationSeconds = effectDurationSeconds;
        this.baseDamage            = baseDamage;
        this.shotSpeedX            = shotSpeedX;
        this.shotSpeedY            = shotSpeedY;
        this.active                = false;
    }

    /**
     * Factory method creating the official power for one of the supported countries.
     *
     * @param country country name ("Corea del Sur", "Camerún", "Nigeria", "Estados Unidos")
     * @return configured Power instance
     */
    public static Power createForCountry(String country) {
        if (country == null) {
            return createForCountry(COUNTRY_KOREA);
        }
        String normalized = country.trim().toLowerCase();
        if (normalized.contains("corea") || normalized.contains("korea")) {
            return new Power(POWER_AURA_AZUL, COUNTRY_KOREA, EFFECT_PUSH, 1.5, 40, 22.0, 0.0);
        } else if (normalized.contains("camer")) {
            return new Power(POWER_RAYO, COUNTRY_CAMEROON, EFFECT_ELECTROCUTE, 3.0, 60, 19.0, 0.0);
        } else if (normalized.contains("nigeria")) {
            return new Power(POWER_TORNADO, COUNTRY_NIGERIA, EFFECT_UNCONSCIOUS, 3.0, 40, 16.0, 7.0);
        } else if (normalized.contains("estados") || normalized.contains("usa")) {
            return new Power(POWER_ILUSION, COUNTRY_USA, EFFECT_CONFUSION, 2.5, 30, 15.5, -3.5);
        }
        return new Power(POWER_AURA_AZUL, country, EFFECT_PUSH, 1.5, 40, 22.0, 0.0);
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
     * Computes total power shot damage using the attacker's kick multiplier.
     *
     * @param kickMultiplier attacker's kick multiplier
     * @return computed damage
     */
    public double calculateDamage(double kickMultiplier) {
        return baseDamage * kickMultiplier;
    }

    /** @return true if the power aura is currently active */
    public boolean isActive() { return active; }

    /** @return the power name */
    public String getName() { return name; }

    /** @return the associated country name */
    public String getCountry() { return country; }

    /** @return the effect name inflicted on the opponent */
    public String getEffectName() { return effectName; }

    /** @return the effect duration in seconds */
    public double getEffectDurationSeconds() { return effectDurationSeconds; }

    /** @return the effect duration in 60-FPS ticks */
    public int getEffectDurationTicks() {
        return (int) Math.round(effectDurationSeconds * 60.0);
    }

    /** @return base damage (DañoTiro * 5) */
    public int getBaseDamage() { return baseDamage; }

    /** @return horizontal launch speed of this power shot */
    public double getShotSpeedX() { return shotSpeedX; }

    /** @return vertical launch speed of this power shot */
    public double getShotSpeedY() { return shotSpeedY; }
}
