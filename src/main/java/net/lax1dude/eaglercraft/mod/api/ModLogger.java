package net.lax1dude.eaglercraft.mod.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModLogger {

    private final Logger logger;

    public ModLogger(String modName) {
        this.logger = LogManager.getLogger("Mod/" + modName);
    }

    public void info(String message) {
        logger.info(message);
    }

    public void info(String format, Object... args) {
        logger.info(String.format(format, args));
    }

    public void warn(String message) {
        logger.warn(message);
    }

    public void warn(String format, Object... args) {
        logger.warn(String.format(format, args));
    }

    public void error(String message) {
        logger.error(message);
    }

    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    public void debug(String message) {
        logger.debug(message);
    }
}
