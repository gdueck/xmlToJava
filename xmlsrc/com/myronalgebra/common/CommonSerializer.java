package com.myronalgebra.common;

abstract public class CommonSerializer {
    public abstract Object get(String name);

    public abstract void putString(String name, String value);

    public abstract void putInt(String name, int value);

    public abstract int getInt(String name, int defaultValue);

    public abstract void putFloat(String name, float value);

    public abstract float getFloat(String name, float defaultValue);

    public abstract String getString(String name, String defaultValue);

    public abstract void putBoolean(String name, boolean value);

    public abstract boolean getBoolean(String name, boolean defaultValue);

    public abstract void commit();

    public abstract Throwable getError();

//    public void putByteArray(String key, byte[] value) {
//        String base64 = PlatformIndependent.getInstance().base64Encode(value);
//        putString(key, base64);
//
//    }
//
//    public byte[] getByteArray(String key) {
//        String base64 = getString(key, "");
//        byte[] value = PlatformIndependent.getInstance().base64Decode(base64);
//        return value;
//    }
//
//    public void putMap(String masterKey, Map<String, Object> bundle) {
//        String keys = "";
//        String sep = "";
//        for (Entry<String, Object> entry : bundle.entrySet()) {
//            String key = entry.getKey();
//            Object value = entry.getValue();
//            String mapKey = masterKey + "_" + key;
//            String typeKey = mapKey + "_type";
//            keys += sep + key;
//            sep = ",";
//            if (value instanceof String) {
//                putString(mapKey, (String) value);
//                putString(typeKey, "string");
//            } else if (value instanceof Integer) {
//                putInt(mapKey, (Integer) value);
//                putString(typeKey, "integer");
//            } else if (value instanceof Float) {
//                putFloat(mapKey, (Float) value);
//                putString(typeKey, "float");
//            } else if (value instanceof byte[]) {
//                putByteArray(mapKey, (byte[]) value);
//                putString(typeKey, "byte[]");
//            }
//        }
//        putString(masterKey, keys);
//    }
//
//    public Map<String, Object> getMap(String masterKey) {
//        String keys = getString(masterKey, null);
//        if (keys == null)
//            return null;
//        Map<String, Object> map = new HashMap<String, Object>();
//        for (String key : keys.split(",")) {
//            String mapKey = masterKey + "_" + key;
//            String typeKey = mapKey + "_type";
//            String type = getString(typeKey, null);
//            if (type == null)
//                continue;
//            if (type.equals("string")) {
//                map.put(key, getString(mapKey, null));
//            } else if (type.equals("integer")) {
//                map.put(key, getInt(key, 0));
//            } else if (type.equals("float")) {
//                map.put(key, getFloat(key, 0));
//            } else if (type.equals("byte[]")) {
//                map.put(key, getByteArray(key));
//            }
//        }
//        return map;
//    }
//

}
