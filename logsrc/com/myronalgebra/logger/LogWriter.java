package com.myronalgebra.logger;

import java.io.*;

/**
 * Captures PrintWriter output and redirects it to Logger.
 * Most print methods are handled by the super class which is
 * provided at construction with a string-backed writer.
 * println() is overridden to write the buffer to the com.myronalgebra.logger
 * and clear the buffer.
 */
public class LogWriter extends PrintWriter {

    Logger.LogLevel logLevel;

    public LogWriter(Logger.LogLevel logLevel) {
        super(new StringWriter());
        this.logLevel = logLevel;
    }

    public void println() {
        Logger.log(logLevel, out.toString());
        ((StringWriter)out).clear();
    }
}

/**
 * Identical to java.io.StringWriter with an added clear() method.
 * This wouldn't be necessary if the original programmer had made
 * buf protected instead of private. Asshole.
 */
class StringWriter extends Writer {
    private StringBuffer buf;

    public StringWriter() {
        this.buf = new StringBuffer();
        this.lock = this.buf;
    }

    public StringWriter(int initialSize) {
        if (initialSize < 0) {
            throw new IllegalArgumentException("Negative buffer size");
        } else {
            this.buf = new StringBuffer(initialSize);
            this.lock = this.buf;
        }
    }

    public void write(int c) {
        this.buf.append((char)c);
    }

    public void write(char[] cbuf, int off, int len) {
        if (off >= 0 && off <= cbuf.length && len >= 0 && off + len <= cbuf.length && off + len >= 0) {
            if (len != 0) {
                this.buf.append(cbuf, off, len);
            }
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    public void write(String str) {
        this.buf.append(str);
    }

    public void write(String str, int off, int len) {
        this.buf.append(str, off, off + len);
    }

    public StringWriter append(CharSequence csq) {
        this.write(String.valueOf(csq));
        return this;
    }

    public StringWriter append(CharSequence csq, int start, int end) {
        if (csq == null) {
            csq = "null";
        }

        return this.append(((CharSequence)csq).subSequence(start, end));
    }

    public StringWriter append(char c) {
        this.write(c);
        return this;
    }

    public String toString() {
        return this.buf.toString();
    }

    public StringBuffer getBuffer() {
        return this.buf;
    }

    public void flush() {
    }

    public void close() throws IOException {
    }

    public void clear() {
        buf.setLength(0);
    }
}
