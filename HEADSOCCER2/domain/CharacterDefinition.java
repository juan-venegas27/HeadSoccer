package domain;

import java.awt.Color;
import java.io.Serializable;

/** Descriptor grouping a country name, visual metadata, and power factory. */
public class CharacterDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Factory interface producing a fresh Power instance for a character. */
    public interface PowerFactory extends Serializable {

        /** Creates a new special power instance. */
        Power create();
    }

    private final String country;
    private final String imageFileName;
    private final Color auraColor;
    private final PowerFactory powerFactory;

    /**
     * Creates a character definition with country identity, sprite filename, aura color, and power factory.
     * @param country selected country name
     * @param imageFileName image file name inside the images directory
     * @param auraColor visual aura color displayed in the arena
     * @param powerFactory factory producing the country special power
     */
    public CharacterDefinition(String country, String imageFileName, Color auraColor, PowerFactory powerFactory) {
        this.country = country;
        this.imageFileName = imageFileName;
        this.auraColor = auraColor;
        this.powerFactory = powerFactory;
    }

    /** Returns the country name for this character. */
    public String getCountry() {
        return country;
    }

    /** Returns the image file name for this character head. */
    public String getImageFileName() {
        return imageFileName;
    }

    /** Returns the visual aura color associated with this character. */
    public Color getAuraColor() {
        return auraColor;
    }

    /** Creates a new Power instance using the registered factory. */
    public Power createPower() {
        return (powerFactory != null) ? powerFactory.create() : new AuraAzulPower(country);
    }
}
