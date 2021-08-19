package com.myronalgebra.logger;

import javax.swing.*;
import java.awt.*;
import java.util.stream.Stream;

import static com.myronalgebra.swingutil.Register.*;

/**
 * A UI that captures and presents lines written to a LogStream. Also
 * provides a combo box that the user can select to change the log level.
 */
public class LogFrame extends JFrame {
    private JPanel panel1;
    private JTextArea logText;
    private JButton clearButton;
    private JComboBox comboBox1;
    public LogStream logStream;
    public static LogFrame instance;

    public LogFrame() {
        initComponents();
        logStream = new LogStream(logText);
    }

    private void initComponents() {
        register("logframebounds", this, 600, 100, 600, 400);
        logText = new JTextArea();
        logText.setLineWrap(true);
        logText.setRows(1024);
        logText.setPreferredSize(new Dimension(500, 400));

        JScrollPane sp = new JScrollPane(logText);

        clearButton = new JButton("Clear");
        comboBox1 = new JComboBox();
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
//        model.addAll(Stream.of(Logger.Flag.values()).map(t->t.toString()).collect(Collectors.toList()));
        Stream.of(Logger.LogLevel.values()).forEach(t->model.addElement(t.toString()));
        comboBox1.setModel(model);

        clearButton.addActionListener(e -> logStream.clear());
        comboBox1.addActionListener(e -> LoggerOptions.logLevel = Logger.LogLevel.values()[comboBox1.getSelectedIndex()]);
        comboBox1.setSelectedItem(LoggerOptions.logLevel.toString());

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        controlPanel.add(comboBox1);
        controlPanel.add(clearButton);

        panel1 = new JPanel();
        panel1.setLayout(new BorderLayout());
        panel1.add(sp, BorderLayout.CENTER);
        panel1.add(controlPanel, BorderLayout.SOUTH);
    }

    public void clear() {
        logStream.clear();
    }

    public static LogFrame getInstance() {
        if (instance == null)
            createInstance();
        return instance;
    }
    
    public static LogFrame createInstance() {
        instance = new LogFrame();

        instance.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
//        PrefUtil prefs = new PrefUtil(LogFrame_old.class);
//        Rectangle bounds = prefs.getRectangle("logframebounds",
//                new Rectangle(600, 100, 600, 400));
//        instance.setBounds(bounds);
//        instance.setPreferredSize(new Dimension(bounds.width, bounds.height));
        instance.setContentPane(instance.panel1);
        instance.pack();
//        instance.setVisible(true);
//        instance.addWindowListener(new java.awt.event.WindowAdapter() {
//            @Override
//            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
//                SwingUtil.close("logframebounds", instance);
//            }
//        });
        return instance;
    }

    public static void main(String [] args) {
        createInstance().setVisible(true);
    }

    private void createUIComponents() {
        logText = new JTextArea();
        logText.setPreferredSize(new Dimension(500, 400));
        logStream = new LogStream(logText);
        logStream.setListener(x->instance.toFront());
    }

    public void setFlag(Logger.LogLevel logLevel) {
        comboBox1.setSelectedIndex(logLevel.ordinal());
    }
}
