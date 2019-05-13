package com.katalon.kit.report.uploader.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogHelper {

    private static final String BEGIN = "BEGIN: ";

    private static final String END = "END: ";

    public static Logger getLogger() {
        final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    /*
     * stackTrace[0] is for Thread.currentThread().getStackTrace() stackTrace[1] is for this method log()
     */
        String className = stackTrace[2].getClassName();
        return LoggerFactory.getLogger(className);
    }

    private static void info(Logger log, String prefix, String message, Object... arguments) {
        log.info(prefix + message, arguments);
    }

    public static void infoBegin(Logger log, String message, Object... arguments) {
        info(log, BEGIN, message, arguments);
    }

    public static void infoEnd(Logger log, String message, Object... arguments) {
        info(log, END, message, arguments);
    }
}