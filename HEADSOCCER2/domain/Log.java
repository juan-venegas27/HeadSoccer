package domain;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/** Error logger that writes exceptions to the log file. */
public final class Log {

    private static final String LOG_NAME = "DOPOSoccer";
    private static final String LOG_FILE = "headsoccer.log";
    private static Logger logger;

    /**
     * Prevents instantiation of utility class.
     */
    private Log() {
    }

    /**
     * Records an exception in the log file.
     * @param e exception to record
     */
    public static void record(Exception e) {
        record(e != null ? e.getMessage() : "Unknown error", e);
    }

    /**
     * Records a message and exception in the log file.
     * @param message descriptive message
     * @param e exception to record
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

    /**
     * Initializes and returns the shared file logger.
     * @return shared logger configured to write to disk
     * @throws IOException if the log file cannot be opened
     */
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
