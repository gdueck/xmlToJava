package com.myronalgebra.logger;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Supplier;

/**
 * Provides methods to write a timestamped message to a PrintStream filtered by
 * LogLevel.
 */
public class Logger {

    private static PrintStream out = System.out;

    public static PrintStream getPrintStream() {
        return out;
    }

    public static void setPrintStream(PrintStream stream) {
        out = stream;
    }

    public static void log(LogLevel logLevel, String format, Object ... items) {
        if (isLogging(logLevel)) {
            timestamp();
            out.print("[");
            out.print(logLevel);
            out.print("] ");
            try {
                out.println(String.format(format, items));
            } catch (Throwable e) {
                out.println(String.format("error logging message %s", format));
            }
            out.flush();
        }
    }

    public static void log(LogLevel logLevel, Supplier<String> supplier) {
        if (isLogging(logLevel)) {
            log(logLevel, supplier.get());
        }
    }

    public static boolean isLogging(LogLevel logLevel) {
        return logLevel.ordinal() >= LoggerOptions.logLevel.ordinal();
    }

    public static void log(LogLevel logLevel, Exception e, String format, Object ... items) {
        timestamp();
        out.print("[");
        out.print(logLevel);
        out.print("] ");
        out.println(e.getClass().getSimpleName() + " " + e.getMessage() + " " +String.format(format, items));
    }

    public static void error(String format, Object ... items) {
        log(LogLevel.Error, format, items);
    }

    public static void success(String format, Object ... items) {
        log(LogLevel.Success, format, items);
    }

    public static void error(Exception e, String format, Object ... items) {
        log(LogLevel.Error, e, format, items);
    }

    public static void info(String format, Object ... items) {
        log(LogLevel.Info, format, items);
    }

    public static void warn(String format, Object ... items) {
        log(LogLevel.Warning, format, items);
    }

    public static void trace(String format, Object ... items) {
        log(LogLevel.Trace, format, items);
    }

    /**
     * Logs trace data using lazy evaluation.
     *
     * @param supplier will be evaluated only if logging at Trace level
     */
    public static void trace(Supplier<String> supplier) {
        log(LogLevel.Trace, supplier);
    }

    private static SimpleDateFormat dformat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");

    private static void timestamp() {
        Date date = new Date(System.currentTimeMillis());
        out.print(dformat.format(date));
        out.print(" ");
    }

    public static void setLogFile(String filename) {
        try {
            if (filename.equals("-"))
                out = System.out;
            else
                out = new PrintStream(new FileOutputStream(filename));
        } catch (FileNotFoundException e) {
            error(e, "Logger.setLogFile %s", filename);
        }
    }


    public enum LogLevel {Trace, Info, Warning, Error, Success}
}
