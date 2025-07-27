package tecnico.logger;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class CustomLogger {

    private final Logger logger;

    /**
     * Creates a new CustomLogger.
     *
     * @param name The name of the logger (usually the class name).
     */
    public CustomLogger(String name) {
        this.logger = Logger.getLogger(name);
        this.logger.setLevel(Level.ALL);
        this.logger.setUseParentHandlers(false);

        // Configura o ConsoleHandler com o CustomLog
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new CustomLog());
        this.logger.addHandler(handler);
    }

    /**
     * Logs a message with the process ID included.
     *
     * @param level   The log level (e.g., INFO, WARNING, ERROR).
     * @param message The message to log.
     */
    public void log(Level level, String message) {
        logger.log(level, message);
    }

    /**
     * Logs an informational message.
     *
     * @param message The message to log.
     */
    public void info(String message) {
        log(Level.INFO, message);
    }

    /**
     * Logs a warning message.
     *
     * @param message The message to log.
     */
    public void warn(String message) {
        log(Level.WARNING, message);
    }

    /**
     * Logs an error message.
     *
     * @param message The message to log.
     */
    public void error(String message) {
        log(Level.SEVERE, message);
    }

    /**
     * Logs a debug message.
     *
     * @param message The message to log.
     */
    public void debug(String message) {
        log(Level.FINE, message);
    }
}

/**
 * Custom formatter for log messages.
 */
class CustomLog extends Formatter {

    @Override
    public String format(LogRecord record) {
        // Retorna apenas a mensagem de log, sem data, hora ou nome da classe
        return record.getMessage() + "\n";
    }
}