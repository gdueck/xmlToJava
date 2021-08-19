package com.myronalgebra.xmltojava;

import com.myronalgebra.common.*;
import com.myronalgebra.logger.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Reads options from an xml file into fields of classes referenced from the file.
 * The file has structure &lt;options>&lt;classname>&lt;fieldname>value&lt;/fieldname>...&lt;/classname>...&lt;/options>
 * where ... means the previous element can be repeated. Classnames must match the simple names
 * of classes supplied in the constructor.
 * <p>Logs error if no class with given classname can be loaded or if file references non-existent field.</p>
 * <p>If the class has a consumer, an instance is created, its fields are populated from the options file
 * and the instance is passed to the consumer. If the class has no consumer, only static fields can be populated.</p>
 * <p> Private fields, whether static or instance, are accessible only via a public setter.
 * <p> Public fields may have a setter in which case the setter is always used instead of direct assignment.</p></p>
 */
public class XmlToJava {
    private boolean echoOptions = true;
    private boolean error;

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    private static class StringConverter {
        public static String valueOf(String string) {
            return string;
        }
    }

    private class OptionType implements ParameterizedType {

        private final Class<?> rawType;

        public OptionType(Class<?> rawType, OptionType...paramTypes) {
            this.rawType = rawType;
        }
        @Override
        public Type[] getActualTypeArguments() {
            return new Type[0];
        }

        @Override
        public Type getRawType() {
            return null;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }

    }

    private static class Alias<T> {
        Consumer<T> consumer;
        Class<?> rawClass;
        Type type;

        public Alias(Consumer<T> consumer, Class<?> rawClass, Class<?>... parameters) {
            this.consumer = consumer;
            this.rawClass = rawClass;
            Type [] types = new Type[parameters.length];
            for (int i = 0; i < parameters.length; i++)
                types[i] = parameters[i];
            try {
                this.type = ParameterizedTypeImpl.make(rawClass, types, null);
            } catch (MalformedParameterizedTypeException e) {
                Logger.error("invalid generic parameters for raw class %s", rawClass.getSimpleName());
            }
        }

        public Alias(Consumer<T> consumer, Class<?> rawClass, Type... parameters) {
            this.consumer = consumer;
            this.rawClass = rawClass;
            type = ParameterizedTypeImpl.make(rawClass, parameters, null);
        }
    }

    private Hashtable<String, Alias<?>> aliasTable = new Hashtable<>();

    /**
     * Constructs an XmlToJava.
     */
    public XmlToJava() {
    }

    public XmlToJava(boolean echoOptions) {
        this.echoOptions = echoOptions;
    }

    /**
     * Adds options classes to the reader
     *
     * @param alias the element in the xml file that maps onto this class
     * @param cls  the class whose static fields will be populated
     */
    public XmlToJava add(String alias, Class<?> cls) {
        aliasTable.put(alias, new Alias(null, cls, cls.getTypeParameters()));
        return this;
    }

    public <T> XmlToJava add(String alias, Class<T> cls, Class<?> ... parameters) {
        add(alias, null, cls, parameters);
        return this;
    }

    /**
     * Adds options classes to the reader with a consumer
     *
     * @param <T>      a generic parameter inferred from rawClass
     * @param alias     the element in the xml file that maps onto this class
     * @param consumer a lambda that consumes the instance
     * @param rawClass      the class of the instance to be created
     * @return
     */
    public <T> XmlToJava add(String alias, Consumer<T> consumer, Class<T> rawClass, Class<?>... parameters) {
        aliasTable.put(alias, new Alias(consumer, rawClass, parameters));
        return this;
    }

    public <T> XmlToJava add(String alias, Consumer<T> consumer, Class<T> rawClass, Type... parameters) {
        aliasTable.put(alias, new Alias(consumer, rawClass, parameters));
        return this;
    }

    private void error(String format, String... params) {
        Logger.error(format, (Object[]) params);
        error = true;
    }

    private void error(Exception ex, String format, String... params) {
        Logger.error(ex, format, (Object[]) params);
        error = true;
    }

    public void load(File optionFile) {
        Document doc = XmlUtil.readDocument(optionFile);
        if (doc == null) {
            error("Cannot read file '%s'", optionFile.getAbsolutePath());
            return;
        }
        Element root = doc.getDocumentElement();
//        getOptions(root);
        readDocRoot(root);
    }

    public void load(InputStream inputStream) {
        Document doc = XmlUtil.readDocument(inputStream);
        if (doc == null) {
            error("Cannot read from input stream");
            return;
        }
        Element root = doc.getDocumentElement();
        readDocRoot(root);
    }

    public void load(Reader reader) {
        Document doc = XmlUtil.readDocument(reader);
        if (doc == null) {
            error("Cannot read from input stream");
            return;
        }
        Element root = doc.getDocumentElement();
//        getOptions(root);
        readDocRoot(root);
    }

    /**
     * Obtains the Alias for a class-by-tag.
     *
     * @param className
     * @return
     * @throws ClassNotFoundException
     */
    private Alias loadAlias(String className) throws ClassNotFoundException {
        Alias v = aliasTable.get(className);
        if (v != null)
            return v;
        throw new ClassNotFoundException(className);
    }

    /**
     * Elements contained directly within the root must match classnames provided in the classes list.
     *
     * @param root
     */
    private void readDocRoot(Element root) {
        try {
            Alias alias = loadAlias(root.getTagName());
            readAnonymous(root, alias);
            return;
        } catch (ClassNotFoundException e) {
            // continue
        } catch (Exception e) {
            error(e, "Unable to load <%s>", root.getTagName());
            return;
        }
        for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element) {
                Element e = (Element) child;
                try {
                    Alias alias = loadAlias(e.getTagName());
                    readAnonymous(e, alias);
                } catch (Exception ex) {
                    error(ex, "Unable to load <%s>", e.getTagName());
                }
            }
        }
    }

    private void readAnonymous(Element e, Alias alias) throws Exception {
        Object object;
        if (Collection.class.isAssignableFrom(alias.rawClass)) {
            object = readCollection(e, alias.rawClass, alias.type, alias.consumer);
        } else if (Map.class.isAssignableFrom(alias.rawClass)) {
            object = readMap(e, alias.rawClass, alias.type, alias.consumer);
        } else if (isSimple(alias.rawClass)) {
            object = readValue(e, alias.rawClass);
            if (alias.consumer != null)
                alias.consumer.accept(object);
        } else {
            object = readAggregate(e, alias.rawClass, alias.consumer);
        }
        if (object != null && echoOptions)
            logOptions(Logger.LogLevel.Info, object, object.getClass(), 0);
    }

    private Object readAggregate(Element e, Class aClass, Consumer consumer) throws Exception {
        Constructor<?> init = aClass.getConstructor();
        Object a = init.newInstance();
        readFields(e, aClass, a);
        if (consumer != null)
            consumer.accept(a);
        return a;
    }

    private Object readCollection(Element e, Class aClass, Type genericType, Consumer consumer) throws Exception {
        Constructor<?> init;
        try {
            init = aClass.getConstructor();
        } catch (NoSuchMethodException ex) {
            throw new Exception(String.format("cannot create instance of %s", aClass.getSimpleName()));
        }
        Collection c = (Collection) init.newInstance();
        Type itemType = null;
        if (genericType == null) {
            error("%s: unable to determing item type", e.getTagName());
            return c;
        } else if (genericType instanceof ParameterizedType) {
            itemType = ((ParameterizedType)genericType).getActualTypeArguments()[0];
        } else if (genericType instanceof Class) {
            itemType = genericType;
        }

        if (itemType != null) {
            readCollectionItems(e, itemType, ((Object x) -> c.add(x)));
        } else {
            error("%s: unable to determing item type", e.getTagName());
        }

        if (consumer != null)
            consumer.accept(c);
        return c;
    }

    private Map readMap(Element e, Class aClass, Type genericType, Consumer consumer) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Constructor<?> init = aClass.getConstructor();
        Map c = (Map) init.newInstance();
        Type keyClass = null;
        Type valueClass = null;

        if (genericType == null) {
            error("%s: unable to determine key or value types", e.getTagName());
            return c;
        } else if (genericType instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) genericType;
            Type [] pTypes = pType.getActualTypeArguments();
            keyClass = pTypes[0];
            valueClass = pTypes[1];
        }
        if (keyClass != null && valueClass != null && keyClass instanceof Class) {
            readMapItems(e, (Class)keyClass, valueClass, (Object k, Object v) -> c.put(k, v));
        } else {
            error("%s: unable to determine key or value types", e.getTagName());
        }
        if (consumer != null)
            consumer.accept(c);
        return c;
    }

    /**
     * If the instance is null, only static fields can be assigned. Otherwise
     * fields can be static or instance.
     * @param parentElement
     * @param parentClass
     * @param instance
     */
    private void readFields(Element parentElement, Class<?> parentClass, Object instance) {
        Field[] fields = parentClass.getFields();
        for (Node child = parentElement.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element) {
                Element e = (Element) child;
                String fieldName = e.getTagName();
                Field field = null;
                try {
                    field = parentClass.getField(fieldName);
                    Object value = readValue(e, field); //fieldName, fieldValue, field.getType());
                    if (value != null) {
                        assignField(parentClass, instance, field, value);
                    }
                } catch (NoSuchFieldException ex) {
                    Method setter = getAnySetterMethod(parentClass, fieldName);
                    if (setter != null) {
                        Object value = readValue(e, setter.getParameters()[0].getType());
                        try {
                            setter.invoke(instance, value);
                        } catch (IllegalAccessException exc) {
                            error("%s: %s", setter.toString(), exc.getMessage());
                        } catch (InvocationTargetException exc) {
                            error("%s: %s", setter.toString(), exc.getMessage());
                        }
                    } else {
                        error("field named \"%s\" in class %s is absent, inaccessible, or missing setter", fieldName, parentElement.getTagName());
                    }
                }
            }
        }
    }

    private void assignField(Class<?> parentClass, Object instance, Field field, Object fieldValue) {
        String fieldName = field.getName();
        Class<?> fieldType = field.getType();
        Method setter = getSetterMethod(parentClass, fieldName, fieldType);
        if (setter != null) {
            try {
                setter.invoke(instance, fieldValue);
            } catch (IllegalAccessException e) {
                error("setter for field %s in class %s is not accessible", fieldName, parentClass.getSimpleName());
            } catch (InvocationTargetException e) {
                error("setter for field %s in class %s is not invocable", fieldName, parentClass.getSimpleName());
            }
        } else {
            try {
                field.set(instance, fieldValue);
            } catch (IllegalAccessException e) {
                error("field %s in class %s is not accessible", fieldName, parentClass.getSimpleName());
            } catch (Exception e) {
                error("field %s in class %s is not setable", fieldName, parentClass.getSimpleName());
            }
        }
    }

    private String getParentTag(Element e) {
        return ((Element)e.getParentNode()).getTagName();
    }

    /**
     * Parses an element to produce a value matching the type of the field parameter.
     * If the type is String, return the text content as a string.
     * If the type has a valueOf method, use it to parse the text content.
     * If the type is Collection, read the collection.
     * If the type is Map, read the map.
     * Finally, Assume the type is aggregate and read an instance.
     * @param e
     * @param field
     * @return
     */
    private Object readValue(Element e, Field field) { //String fieldName, String fieldValue, Class<?> valueClass) {
        Class<?> valueClass = field.getType();
        valueClass = typeWrapperFor(valueClass);
        String fieldName = field.getName();
        try {
            Method valueOf = getValueOfMethod(valueClass); //valueClass.getMethod("valueOf", String.class);
            if (valueOf != null) {
                return valueOf.invoke(null, e.getTextContent().trim());
            }
        } catch (InvocationTargetException ex) {
            error("%s.%s: %s", e.getParentNode().getNodeName(), fieldName, ex.getTargetException().getMessage());
            return null;
        } catch (Exception ex) {
            error("%s.%s: %s", getParentTag(e), fieldName, ex.getMessage());
            return null;
        }
        try {
            if (Collection.class.isAssignableFrom(valueClass)) {
                Object c = readCollection(e, valueClass, field.getGenericType(), null);
                return c;
            } else if (Map.class.isAssignableFrom(valueClass)) {
                Map m = readMap(e, valueClass, field.getGenericType(), null);
                return m;
            } else {
                Object c = readAggregate(e, valueClass, null);
                return c;
            }
        } catch (Exception ex) {
            error("error <%s>.%s: %s", getParentTag(e), fieldName, ex.getMessage());
        }

        return null;

    }

    private Object readValue(Element e, Class<?> valueClass) { //String fieldName, String fieldValue, Class<?> valueClass) {
        valueClass = typeWrapperFor(valueClass);
        try {
            Method valueOf = getValueOfMethod(valueClass); //valueClass.getMethod("valueOf", String.class);
            if (valueOf != null) {
                return valueOf.invoke(null, e.getTextContent().trim());
            }
        } catch (InvocationTargetException ex) {
            error("%s.%s: %s", e.getParentNode().getNodeName(), e.getTagName(), ex.getTargetException().getMessage());
            return null;
        } catch (Exception ex) {
            error("%s.%s: %s", getParentTag(e), e.getTagName(), ex.getMessage());
            return null;
        }
        try {
            if (Collection.class.isAssignableFrom(valueClass)) {
                Object c = readCollection(e, valueClass, (Type)valueClass, null);
                return c;
            } else if (Map.class.isAssignableFrom(valueClass)) {
                Map m = readMap(e, valueClass, (Type)valueClass, null);
                return m;
            } else {
                Object c = readAggregate(e, valueClass, null);
                return c;
            }
        } catch (Exception ex) {
            error("error %s.%s: %s", getParentTag(e), e.getTagName(), ex.getMessage());
        }
        return null;

    }

    private Hashtable<Class<?>, Class<?>> typeTable = new Hashtable () {{
        put(String.class, StringConverter.class);
        put(int.class, Integer.class);
        put(byte.class, Byte.class);
        put(short.class, Short.class);
        put(long.class, Long.class);
        put(float.class, Float.class);
        put(double.class, Double.class);
        put(char.class, Character.class);
        put(boolean.class, Boolean.class);
        put(void.class, Void.class);
    }};

    private Class<?> typeWrapperFor(Class<?> type) {
        Class<?> result = typeTable.get(type);
        if (result == null)
            return type;
        return result;
    }

    private void readMapItems(Element root, Class<?> keyClass, Type valueClass, BiConsumer consumer) {
        Method keyConverter = null;
        Method valueConverter = null;
        keyClass = typeWrapperFor(keyClass);
        keyConverter = getValueOfMethod(keyClass);
        if (valueClass instanceof Class) {
            valueClass = typeWrapperFor((Class)valueClass);
            valueConverter = getValueOfMethod((Class)valueClass);
        }

        for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element) {
                Element e = (Element) child;
                String tagName = e.getTagName();
                Object keyValue = null;
                try {

                    if (keyConverter != null) {
                        try {
                            keyValue = keyConverter.invoke(null, tagName);
                        } catch (Exception ex) {
                            throw new Exception(String.format("key %s cannot be converted to %s", tagName, keyClass.getSimpleName()));
                        }
                    }
                    if (keyValue == null) {
                        throw new Exception(String.format("key %s cannot be converted to %s", tagName, keyClass.getSimpleName()));
                    }
                    Object valueValue = null;
                    if (valueConverter != null) {
                        valueValue = valueConverter.invoke(null, e.getTextContent().trim());
                    } else if (valueClass instanceof Class) {
                        valueValue = readValue(e, (Class<?>) valueClass);
                    } else if (valueClass instanceof ParameterizedType) {
                        ParameterizedType ptype = (ParameterizedType) valueClass;
                        Type rawType = ptype.getRawType();
                        if (!(rawType instanceof Class)) {
                            throw new Exception(String.format("map item %s not supported", valueClass.toString()));
                        }
                        Type [] generics = ptype.getActualTypeArguments();
                        if (Collection.class.isAssignableFrom((Class)rawType)) {
                            valueValue = readCollection(e, (Class)rawType, generics[0], null);
                        } else if (Map.class.isAssignableFrom((Class)rawType)) {
                            valueValue = readMap(e, (Class)generics[0], generics[1], null);
                        } else {
                            valueValue = readAggregate(e, (Class)rawType, null);
                        }
                    } else {
                        valueValue = readValue(e, (Class<?>)valueClass);
                    }
                    if (valueValue != null) {
                        consumer.accept(keyValue, valueValue);
                    } else {
                        throw new Exception(String.format("value %s cannot be converted to %s", e.getTextContent().trim(), ((Class)valueClass).getSimpleName()));
                    }
                } catch (InvocationTargetException ex) {
                    error("Unable to map %s because %s cannot be converted to %s", tagName, e.getTextContent(), ((Class)valueClass).getSimpleName());
                } catch (Exception ex) {
                    error("Unable to map %s onto %s: %s", tagName, e.getTextContent(), ex.getMessage());
                }
            }
        }
    }

    private void readCollectionItems(Element root, Type itemType, Consumer consumer) {
        for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element) {
                Element e = (Element) child;
                try {
                    if (itemType instanceof ParameterizedType) {
                        ParameterizedType pType = (ParameterizedType)itemType;
                        if (Collection.class.isAssignableFrom((Class)pType.getRawType())) {
                            Object value = readCollection(e, (Class) pType.getRawType(), pType.getActualTypeArguments()[0], null);
                            consumer.accept(value);
                        } else if (Map.class.isAssignableFrom((Class)pType.getRawType())) {
                            Object value = readMap(e, (Class)pType.getRawType(), pType, null);
                            consumer.accept(value);
                        }
                    } else if (itemType instanceof Class) {
                        consumer.accept(readValue(e, (Class)itemType));
                    } else {
                        error("Collection of %s not supported", itemType.toString());
                    }
                } catch (Exception ex) {
                    error(ex, "Unable to load class for <%s>", e.getTagName());
                }
            }
        }
    }

    /**
     * Setting field directly failed. Try to find setter.
     */
    private void setOption(Class<?> optionsClass, String className, Object instance, String fieldName, String text) {
        Method m;
        try {
            m = getSetterMethod(optionsClass, fieldName);
            if (m != null) {
                m.invoke(instance, text);
            } else {
                error("<%s> no accessible field or setter for '%s'", className, fieldName);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void setOption(Class<?> parentClass, Object instance, String fieldName, Object fieldValue) {
        Method m;
        try {
            m = getSetterMethod(parentClass, fieldName);
            if (m != null) {
                m.invoke(instance, fieldValue);
            } else {
                error("<%s> no accessible field or setter for '%s'", parentClass.getSimpleName(), fieldName);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private Method getSetterMethod(Class<?> optionsClass, String fieldName) {
        return getSetterMethod(optionsClass, fieldName, String.class);
    }

    private Method getValueOfMethod(Class<?> parentClass) {
        try {
            if (parentClass == String.class)
                parentClass = StringConverter.class;
            Method m = parentClass.getMethod("valueOf", String.class);
            if (Modifier.isStatic(m.getModifiers()) && Modifier.isPublic(m.getModifiers())) {
                return m;
            } else {
                error("%s.valueOf method must be public and static", parentClass.getSimpleName());
            }
        } catch (NoSuchMethodException e) {
        }
        return null;
    }

    private Method getSetterMethod(Class<?> parentClass, String fieldName, Class<?> fieldType) {
        try {
            return parentClass.getMethod(getSetterName(fieldName), fieldType);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private Method getAnySetterMethod(Class <?> parentClass, String fieldName) {
        String setterName = getSetterName(fieldName);
        for (Method m: parentClass.getMethods()) {
            if (m.getName().equals(setterName) && m.getParameters().length == 1) {
                return m;
            }
        }
        return null;
    }

    private String getSetterName(String fieldName) {
        return "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }

    private boolean isSimple(Class<?> optionsClass) {
        try {
            return optionsClass == String.class || optionsClass.isPrimitive() || optionsClass.getMethod("valueOf", String.class) != null;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
    public void logOptions(Logger.LogLevel logLevel, Object instance, Class<?> optionsClass, int indent) {
        logOptions(logLevel, "", instance, optionsClass, indent);
    }

    public void logOptions(Logger.LogLevel logLevel, String name, Object instance, Class <?> optionsClass, int indent) {
        if (!Logger.isLogging(logLevel))
            return;
        try {
            if (instance == null) {
                Logger.log(logLevel, "%s%s=%s", indent(indent), name, "null");
            } else if (isSimple(optionsClass)) {
                Logger.log(logLevel, "%s%s=%s", indent(indent), name, instance);
                return;
            } else if (Collection.class.isAssignableFrom(optionsClass)) {
                Logger.log(logLevel, "%s%s:%s", indent(indent), name, "collection"); //optionsClass.getSimpleName());
                int index = 0;
                for (Object item: (Collection)instance) {
                    logOptions(logLevel, ""+index++, item, item.getClass(), indent+1);
                }
            } else if (Map.class.isAssignableFrom(optionsClass)) {
                Logger.log(logLevel, "%s%s:%s", indent(indent), name, "map"); //optionsClass.getSimpleName());
                Map m = (Map)instance;
                for (Object k: m.keySet()) {
                    logOptions(logLevel, k.toString(), m.get(k), m.get(k).getClass(), indent+1);
                }
            } else {
                Logger.log(logLevel, "%s%s:%s", indent(indent), name, optionsClass.getSimpleName());
                for (Field f : optionsClass.getFields()) {
                    try {
                        Object value = f.get(instance);
                        if (value == instance) {
                            Logger.log(logLevel, "%s%s=self", indent(indent+1), f.getName());
                        } else
                            logOptions(logLevel, f.getName(), value, f.getType(), indent+1);
                    } catch (IllegalAccessException ex2) {


                    }
                }
            }
        } catch (Exception e) {
            error(e, "@logOptions");
        }
    }

    private String indent(int indent) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < indent * 3; i++)
            sb.append(" ");
        return sb.toString();
    }
}
