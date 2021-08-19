package com.myronalgebra.logger;

import javax.swing.*;
import java.io.PrintStream;
import java.util.function.Consumer;

/**
 * Provides a PrintStream that writes to a JTextArea.
 */
public class LogStream extends PrintStream {
    Consumer<String> listener;
    private JTextArea textArea;

    public LogStream(JTextArea textArea) {
        super(System.out);
        this.textArea = textArea;
    }

    public void setListener(Consumer<String> listener) {
        this.listener = listener;
    }

     @Override
    public void print(String s) {
        textArea.append(s);
//        if (listener != null)
//            listener.accept(s);
    }

    @Override
    public void println(String s) {
        textArea.append(s);
        textArea.append("\n");
        if (listener != null)
            listener.accept(s);
    }

    @Override
    public void print(Object obj) {
        textArea.append(String.valueOf(obj));
    }

    @Override
    public void flush() {
        textArea.setSelectionStart(Integer.MAX_VALUE);
        textArea.setSelectionEnd(Integer.MAX_VALUE);

    }

    public void clear() {
        textArea.setText("");
    }
}
