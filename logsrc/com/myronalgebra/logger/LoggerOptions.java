package com.myronalgebra.logger;

public class LoggerOptions {
    public static Logger.LogLevel logLevel = Logger.LogLevel.Error;
    private static String logFilename;
    public static void setLogFile(String logFile) {
        logFilename = logFile;
        Logger.setLogFile(logFile);
    }
}
