package src.domain;

/**
 * Custom exception class for DOPOSoccer domain and persistence errors.
 * Provides clear and detailed error messages for users and programmers.
 */
public class HeadSoccerException extends Exception {

    private static final long serialVersionUID = 1L;

    public static final String INVALID_CONFIG      = "La configuracion del partido es invalida (tiempo, goles o resolucion fuera de rango).";
    public static final String INSUFFICIENT_POINTS = "Puntos insuficientes para mejorar esta estadistica.";
    public static final String MAX_STAT_REACHED    = "La estadistica ya alcanzo el nivel maximo permitido (10).";
    public static final String INVALID_STAT_NAME   = "El nombre de la estadistica especificada no es valido.";
    public static final String FILE_NOT_FOUND      = "No se encontro el archivo de partida seleccionado.";
    public static final String SAVE_ERROR          = "Ocurrio un error al intentar guardar la partida o el progreso.";
    public static final String OPEN_ERROR          = "Ocurrio un error al abrir el archivo de partida (formato incompatible o danado).";
    public static final String NULL_FILE           = "El archivo especificado es nulo.";

    /**
     * Constructs a HeadSoccerException with a detailed message.
     *
     * @param message descriptive error message
     */
    public HeadSoccerException(String message) {
        super(message);
    }

    /**
     * Constructs a HeadSoccerException with a detailed message and cause.
     *
     * @param message descriptive error message
     * @param cause   underlying exception
     */
    public HeadSoccerException(String message, Throwable cause) {
        super(message, cause);
    }
}
