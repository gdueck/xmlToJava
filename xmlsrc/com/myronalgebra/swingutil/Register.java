package com.myronalgebra.swingutil;

import com.myronalgebra.common.PrefUtil;

import javax.swing.*;
import java.awt.*;

public class Register {
        public static void register(String label, JFrame frame, int x, int y, int w, int h) {
//        frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
            PrefUtil prefs = new PrefUtil(frame.getClass());
            Rectangle bounds = prefs.getRectangle(label,
                    new Rectangle(x, y, w, h));
            frame.setBounds(bounds);
            frame.setPreferredSize(new Dimension(bounds.width, bounds.height));
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    PrefUtil prefs = new PrefUtil(frame.getClass());
                    prefs.putRectangle(label, frame.getBounds());
                    prefs.flush();
                }
            });

        }

}
