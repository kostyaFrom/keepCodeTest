package com.konstantinbulygin.onlinestore.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpringLoggingHelper {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public void helpMethod() {
        logger.debug("This is a debug message");
        logger.warn("This is a warn message");
        logger.error("This is an error message");
    }
}