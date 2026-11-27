package domain;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

/** Central registry mapping playable countries to definitions, powers, and visual assets. */
public final class CharacterRegistry {

    private static final Map<String, CharacterDefinition> DEFINITIONS = new LinkedHashMap<>();

    static {
        register(new CharacterDefinition(
                Power.COUNTRY_KOREA, "korea.png", new Color(40, 170, 255), AuraAzulPower::new));
        register(new CharacterDefinition(
                Power.COUNTRY_CAMEROON, "cameroon.png", new Color(255, 230, 40), RayoPower::new));
        register(new CharacterDefinition(
                Power.COUNTRY_NIGERIA, "nigeria.png", new Color(60, 235, 120), TornadoPower::new));
        register(new CharacterDefinition(
                Power.COUNTRY_USA, "usa.png", new Color(255, 90, 210), IlusionPower::new));
    }

    /**
     * Prevents instantiation of utility class.
     */
    private CharacterRegistry() {
    }

    /**
     * Registers or replaces a playable character definition.
     * @param definition character descriptor to register
     */
    public static synchronized void register(CharacterDefinition definition) {
        if (definition != null && definition.getCountry() != null) {
            DEFINITIONS.put(definition.getCountry(), definition);
        }
    }

    /**
     * Unregisters a character definition by country name.
     * @param country selected country name
     */
    public static synchronized void unregister(String country) {
        if (country != null) {
            DEFINITIONS.remove(country);
        }
    }

    /** Returns all registered country names in registration order. */
    public static synchronized String[] getAvailableCountries() {
        return DEFINITIONS.keySet().toArray(new String[0]);
    }

    /**
     * Finds the registered character definition matching a country name or alias.
     * @param country selected country name
     * @return matching character definition, or Korea as visual fallback
     */
    public static synchronized CharacterDefinition get(String country) {
        CharacterDefinition found = findRegistered(country);
        if (found != null) {
            return found;
        }
        return DEFINITIONS.get(Power.COUNTRY_KOREA);
    }

    /**
     * Creates a fresh Power instance for the requested country name or custom country fallback.
     * @param country selected country name
     * @return configured Power instance for the country
     */
    public static synchronized Power createPowerForCountry(String country) {
        if (country == null) {
            return DEFINITIONS.get(Power.COUNTRY_KOREA).createPower();
        }
        CharacterDefinition found = findRegistered(country);
        if (found != null) {
            return found.createPower();
        }
        return new AuraAzulPower(country);
    }

    /**
     * Searches registered characters by exact key, case-insensitive name, or known country alias.
     * @param country requested country label
     * @return matching character definition, or null if not registered
     */
    private static CharacterDefinition findRegistered(String country) {
        if (country == null) {
            return null;
        }
        CharacterDefinition direct = DEFINITIONS.get(country);
        if (direct != null) {
            return direct;
        }
        String normalized = country.trim().toLowerCase();
        for (Map.Entry<String, CharacterDefinition> entry : DEFINITIONS.entrySet()) {
            if (entry.getKey().toLowerCase().equals(normalized)) {
                return entry.getValue();
            }
        }
        if (normalized.contains("corea") || normalized.contains("korea")) {
            return DEFINITIONS.get(Power.COUNTRY_KOREA);
        }
        if (normalized.contains("camer")) {
            return DEFINITIONS.get(Power.COUNTRY_CAMEROON);
        }
        if (normalized.contains("nigeria")) {
            return DEFINITIONS.get(Power.COUNTRY_NIGERIA);
        }
        if (normalized.contains("estados") || normalized.contains("usa")) {
            return DEFINITIONS.get(Power.COUNTRY_USA);
        }
        return null;
    }
}
