package de.bonn.eis.utils;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.logging.*;

/**
 * Created by Ainuddin Faizan on 1/27/17.
 */
public class QGenLogger {
    private static final Logger LOGGER = Logger.getLogger(QGenUtils.class.getName());
    private static Handler fileHandler, fineFileHandler;

    static {
        try {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            fileHandler = new FileHandler("./query_messages_" + timestamp.toString() + ".log");
            fineFileHandler = new FileHandler("./fine_messages_" + timestamp.toString() + ".log");
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOGGER.addHandler(fileHandler);
        LOGGER.addHandler(fineFileHandler);
        SimpleFormatter simpleFormatter = new SimpleFormatter();
        fileHandler.setFormatter(simpleFormatter);
        fineFileHandler.setFormatter(simpleFormatter);
        fileHandler.setLevel(Level.INFO);
        fineFileHandler.setLevel(Level.FINE);
        LOGGER.setUseParentHandlers(false);
        LOGGER.setLevel(Level.ALL);
    }

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void severe(String message) {
        LOGGER.severe(message);
    }

    public static void severe(String... messages) {
        for (String message : messages) {
            LOGGER.severe(message);
        }
    }

    public static void severe(Class<?> caller, String... messages) {
        LOGGER.severe(caller.getName());
        for (String message : messages) {
            LOGGER.severe(message);
        }
    }

    public static void fine(String message) {
        LOGGER.fine(message);
    }
}
