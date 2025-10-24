package oarlib.util;

/**
 * A simple logging utility that replaces Log4j for TeaVM compatibility.
 * Uses System.out and System.err for output. Provides debug, info, warn, and error levels.
 */
public class SimpleLogger {
    private String name;

    private SimpleLogger(String name) {
        this.name = name;
    }

    /**
     * Get a logger instance for the given class
     *
     * @param clazz the class
     * @return a SimpleLogger instance
     */
    public static SimpleLogger getLogger(Class<?> clazz) {
        return new SimpleLogger(clazz.getSimpleName());
    }

    /**
     * Get a logger instance for the given name
     *
     * @param name the logger name
     * @return a SimpleLogger instance
     */
    public static SimpleLogger getLogger(String name) {
        return new SimpleLogger(name);
    }

    /**
     * Log a debug message
     *
     * @param message the message to log
     */
    public void debug(String message) {
        System.out.println("[DEBUG] " + name + " - " + message);
    }

    /**
     * Log an info message
     *
     * @param message the message to log
     */
    public void info(String message) {
        System.out.println("[INFO] " + name + " - " + message);
    }

    /**
     * Log a warn message
     *
     * @param message the message to log
     */
    public void warn(String message) {
        System.out.println("[WARN] " + name + " - " + message);
    }

    /**
     * Log an error message
     *
     * @param message the message to log
     */
    public void error(String message) {
        System.err.println("[ERROR] " + name + " - " + message);
    }

    /**
     * Log an error message with an exception
     *
     * @param message the message to log
     * @param throwable the exception
     */
    public void error(String message, Throwable throwable) {
        System.err.println("[ERROR] " + name + " - " + message);
        if (throwable != null) {
            throwable.printStackTrace(System.err);
        }
    }
}
