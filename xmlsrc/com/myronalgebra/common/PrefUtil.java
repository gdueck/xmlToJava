package com.myronalgebra.common;

import java.awt.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Stores window locations.
 * @author Gerald Dueck
 *
 */
public class PrefUtil  {

    private Preferences prefs;

    public PrefUtil(Class <?> cls) {
        prefs = Preferences.userNodeForPackage(cls);
    }

    public PrefUtil(Preferences prefs) {
        this.prefs = prefs;
    }

    public Rectangle getRectangle(String key, Rectangle def) {
        String sRect = prefs.get(key, "");
        Rectangle result = rectFromString(sRect);
        if (result == null)
            result = def;
        if (result == null)
            result = new Rectangle();
        return result;
    }


    public void putRectangle(String key, Rectangle rect) {
        String sBounds = String.format("%d,%d,%d,%d", rect.x, rect.y, rect.width, rect.height);
        prefs.put(key, sBounds);
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
        }
    }

    public void putInt(String key, int value) {
        prefs.putInt(key, value);
    }

    public int getInt(String key, int defValue) {
        return prefs.getInt(key, defValue);
    }

    public void putString(String key, String value) {
        prefs.put(key,  value);
    }

    public String getString(String key, String defValue) {
        return prefs.get(key, defValue);
    }
    protected Rectangle rectFromString(String sRect) {
        String [] items = sRect.split(",");
        try {
            return new Rectangle(
                    Integer.parseInt(items[0]),
                    Integer.parseInt(items[1]),
                    Integer.parseInt(items[2]),
                    Integer.parseInt(items[3])
            );
        } catch (NumberFormatException e) {
            return null;
        }
    }


    public void flush() {
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
        }
    }

    
    public Object get(String name) {
        return prefs.get(name, null);
    }

    
    public void putFloat(String name, float value) {
        prefs.putFloat(name, value);

    }

    
    public float getFloat(String name, float defaultValue) {
        return prefs.getFloat(name, defaultValue);
    }

    
    public void putBoolean(String name, boolean value) {
        prefs.putBoolean(name, value);
    }

    
    public boolean getBoolean(String name, boolean defaultValue) {
        return prefs.getBoolean(name, defaultValue);
    }

    
    public void commit() {
    }

    
    public Throwable getError() {
        return null;
    }
}
