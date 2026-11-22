package src.domain;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Programmer error logger for DOPOSoccer.
 * Records exceptions and diagnostic messages into a persistent log file.
 */
public final class Log {

    private static final String LOG_NAME = "DOPOSoccer";
    private static final String LOG_FILE = "headsoccer.log";
    private static Logger logger;

    private Log() {
        // Utility class
    }

    /**
     * Records an exception in the log file without interrupting execution.
     *
     * @param e the exception to log
     */
    public static void record(Exception e) {
        record(e != null ? e.getMessage() : "Unknown error", e);
    }

    /**
     * Records a custom message and exception in the log file.
     *
     * @param message contextual message
     * @param e       the exception to log (may be null)
     */
    public static synchronized void record(String message, Exception e) {
        try {
            Logger log = getLogger();
            if (e != null) {
                log.log(Level.SEVERE, message, e);
            } else {
                log.log(Level.WARNING, message);
            }
        } catch (Exception loggingFailure) {
            System.err.println("Failed to write to HeadSoccer log: " + loggingFailure.getMessage());
        }
    }

    private static Logger getLogger() throws IOException {
        if (logger == null) {
            logger = Logger.getLogger(LOG_NAME);
            logger.setUseParentHandlers(false);
            FileHandler fileHandler = new FileHandler(LOG_FILE, true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        }
        return logger;
    }
}
