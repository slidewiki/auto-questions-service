package de.bonn.eis.controller;

import de.bonn.eis.utils.QGenUtils;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Ainuddin Faizan on 1/27/17.
 */
public class QGenLogger {
    private static final Logger LOGGER = Logger.getLogger(QGenUtils.class.getName());
    private static Handler fileHandler;

    static {
        try {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            fileHandler = new FileHandler("./query_messages_" + timestamp.toString() + ".log");
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOGGER.addHandler(fileHandler);
        fileHandler.setLevel(Level.ALL);
        LOGGER.setLevel(Level.ALL);
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
}
