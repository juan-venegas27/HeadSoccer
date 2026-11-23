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

    /** Creates a power with the given name. */
    public Power(String name) {
        this(name, COUNTRY_KOREA, EFFECT_PUSH, 1.5, 40, 22.0, 0.0);
    }

    /** Creates a power with metadata and base damage. */
    public Power(String name, String country, String effectName, double effectDurationSeconds, int baseDamage) {
        this(name, country, effectName, effectDurationSeconds, baseDamage, 20.0, 0.0);
    }

    /** Creates a power with full metadata and shot speeds. */
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

    /** Creates the power assigned to the given country. */
    public static Power createForCountry(String country) {
        if (country == null) {
            return createForCountry(COUNTRY_KOREA);
        }
        String normalized = country.trim().toLowerCase();
        if (normalized.contains("corea") || normalized.contains("korea")) {
            return new Power(POWER_AURA_AZUL, COUNTRY_KOREA, EFFECT_PUSH, 1.5, 40, 20.0, 0.0);
        } else if (normalized.contains("camer")) {
            return new Power(POWER_RAYO, COUNTRY_CAMEROON, EFFECT_ELECTROCUTE, 3.0, 60, 16.5, 0.0);
        } else if (normalized.contains("nigeria")) {
            return new Power(POWER_TORNADO, COUNTRY_NIGERIA, EFFECT_UNCONSCIOUS, 3.0, 40, 14.0, 6.0);
        } else if (normalized.contains("estados") || normalized.contains("usa")) {
            return new Power(POWER_ILUSION, COUNTRY_USA, EFFECT_CONFUSION, 2.5, 30, 13.5, -3.0);
        }
        return new Power(POWER_AURA_AZUL, country, EFFECT_PUSH, 1.5, 40, 20.0, 0.0);
    }

    /** Activates the power aura on the owning player. */
    public void activate() {
        this.active = true;
    }

    /** Deactivates the power aura (e.g. after executing the power shot). */
    public void deactivate() {
        this.active = false;
    }

    /** @return total damage scaled by the kick multiplier */
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

    /** @return base damage value */
    public int getBaseDamage() { return baseDamage; }

    /** @return horizontal launch speed of this power shot */
    public double getShotSpeedX() { return shotSpeedX; }

    /** @return vertical launch speed of this power shot */
    public double getShotSpeedY() { return shotSpeedY; }
}
