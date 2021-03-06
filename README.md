# XmlToJava
XmlToJava translates XML data into Java values and populates fields of 
Java classes.
It was originally designed to read configuration information
but may have other uses.
## Usage
Create an instance of XmlToJava, add class aliases and their consumers, 
and pass a File, InputStream or Reader
to the load method.

Aliasing maps XML elements onto Java classes.
* Elements are in terminology from the
user domain rather than the programming language domain.
* Package information is kept on the Java side 

Refer to the many examples below.
## XML layout
* If the document root has been aliased, 
the document contains one instance of one class. 
Otherwise, the document contains many instances of many classes.
In this case, the document root becomes a virtual aggregate and its subelements
have anonymous instances. 
When the document root is a virtual aggregate, 
its name can be anything.
* Root element aside, every element
is associated with a _type_, nominally a class, which can be
categorized as _simple_, _aggregate_, _collection_ or _map_. 
The type category determines
the arrangement of an element's contents: text-only in the case of simple, 
more elements in
all other cases.
* Every element also has an _instance category_: one of _field_, _collection item_,
_map entry_, _anonymous_ or -- in the case of an unaliased root -- _virtual_.
An instance category determines what happens to an object created from XML.
* For simple elements, e.g. String, Enum, primitive, wrapped or user defined, 
the text content of a corresponding 
element must contain a parseable representation of the field's type category.
* For collection elements, e.g. ArrayList, 
subelements are collection items; the actual name of the subelement is
ignored but the instance is added to the collection.
* For map elements, e.g. Hashtable, subelements contain map-entry items.
The name
of each subelement
is used as a key (this inherently restricts its type to simple) and
the instance of the subelement is used as a value which again has a type category.
* For field elements, the name of the element is matched to a field within
the containing aggregate, the type is taken from the field's class and the instance
is assigned to the field.
* Anonymous instances are passed to a consumer.
## Design Notes
* A single xml file can contain data destined for more than one instance and
conforming to more than one class.
* Class definitions and their instances can be scattered throughout
the program source for reasons of modularity.
* a field must be public or have a public setter.
* a field may be static.
* An anonymous class that contains only static
fields need not have a consumer since its instance is irrelevant.
* A simple type is one that can be parsed from a String.
* Any class C that has a
method with the signature "public static C valueOf(String)" acts a simple type.
* Strings and primitive types are promoted to wrappers that have a
valueOf method.

## Examples
### Simple alias
The XML to specify a single boolean alias is in test.xml:
```xml
<?xml version="1.0"?>
<docroot>
    <flag>true</flag>
</docroot>
```
`XmlToJava.load()` does not return a value (because it would typically
have to return many values), so anything recognized from the top level of an 
xml file
must be fed to a consumer.
```java
class Test {
    private static boolean flag;
    
    public static void main(String ... args) {
        new XmlToJava()
            .add("flag", b->flag = b, Boolean.class)
            .load(new File("test.xml"));
        System.out.println(String.format("flag=%s", flag));
    }
}
```
* The simplest form of `add` associates an alias with a raw class, 
but here the three-argument
form takes alias, consumer and raw class.
* When XmlToJava sees &lt;flag>, it uses the Boolean class. The 
Boolean.valueOf method creates 
a value from the text content of &lt;flag>.
### Anonymous root
In this example, because the document root has an alias (Boolean) it
also has an
anonymous instance that is delivered to a consumer.
This is an unusual case that is presented in the interest of design closure.
```java
class Test {
    String xml = "<bool>true</bool>";
    public static void main(String... args) {
        new XmlToJava()
                .add("bool", x -> System.out.println(x), Boolean.class)
                .load(new StringReader(xml));
    }
}
```
### Aggregate example
A Java class like
```Java
public class TestClass {
    public int intField;
    public Boolean boolfield;
    public String stringField;
    static private String privateString;
    static private String inaccessibleField;
    @Hidden static public String password;
    public enum EnumList {One, Two};
    public EnumList enumVar;
    static public void setPrivateString(String alias) {
        privateString = alias;
    }
}
```
is loaded by 
```Java
class Test {
    public static void main(String... args) {
        XmlToJava reader = new XmlToJava()
            .add("test", x -> this.testClass = x, TestClass.class)
            .load(new File("test.xml"));
    }
}
```
and is populated from XML written as
```xml 
<docroot>
  <test>
    <intField>47</intField>
    <stringField>string alias</stringField>
    <missingField>missing field alias</missingField>
    <inaccessibleField>inaccessible alias</inaccessibleField>
    <enumVar>Two</enumVar>
    <string>set string alias</string>
    <password>secret</password>
  </test>
</options>
```
When &lt;test> is processed, the tag name of each subelement
refers to a field of `TestClass`. The class of a referenced field
guides the parsing of its referent.
 
To illustrate the possibilites, `TestClass` contains a combination of static, instance, public and private fields.
The add method maps the element "test" onto "package-name.TestClass" 
and also provides a lambda expression
to be invoked when an instance of TestClass has been recognized.
* `intField` is simple (because it's primitive) and will be processed 
by the `valueOf` method of `Integer`.
* `boolField` is simple because its class has a valueOf method
(as do all wrapper classes).
* `privateString` is inaccessible
but has a setter.
* `inaccessibleString` cannot be instanced because it is private and has no setter.
* `password` is marked as "@Hidden" so the default display algorithm won't show a password
in a public log. (`@Hidden` is in the same package as `XmlToJava`).
* `enumVar` is simple because `EnumList` has a `valueOf` method.

### Collection Example
This example reads a class containing a Collection.
```java
public class Test {
    public static class ContainsArray {
        public String name;
        public ArrayList<Integer> list;
    }

    ContainsArray containsArray;

    static void main(String ... args) {
        new XmlToJava()
        .add("containsArray", x->this.containsArray = x, ContainsArray.class)
        .load("containsArray.xml");
        }
}
```
XML
```xml
<options>
  <containsArray>
      <name>some name</name>
      <list>
          <item>1</item>
          <item>47</item>
          <item>13</item>
      </list>
  </containsArray>
</options>
```
* The class of the field `list` has a generic
type `ArrayList<Integer>` which is a subclass of Collection.
The template parameter
Integer used to convert the items in the list.
* The subelements of `<list>` denote elements that will be added to
the collection. Their tag names are unimportant but their content
must conform to the element type of the collection.
* This example runs almost the same if `ArrayList<Integer>`
was changed to `HashSet<Integer>`.
### Anonymous collection example
The `xml` shown here requires a type for the anonymous instance denoted
by `<list>`.

XML
```xml
<list>
  <item>1</item>
  <item>47</item>
  <item>13</item>
</list>
```
If the intent is to create a collection, the Java code would look like this.
```java
class Test {
    Collection<Integer> list;
    String xml = "...";

    public void main(String... args) {
        new XmlToJava()
                .add("list", x -> list = x, ArrayList.class, Integer.class)
                .load(new StringReader(xml));
    }
}

```
* Note the extra parameter to `add` which provides the element class for the collection.
* This is equivalent to `ArrayList<Integer>`.
### Map of simple class
Similar to the example for Collections, this example has a 
class with a Hashtable field.

XML
```xml
<map>
    <key1>value1</key1>
    <key2>value2</key2>
</map>
```

```java
import com.myronalgebra.xmltojava.XmlToJava;

public class Test {
    Hashtable<String, String> table;
    public void main(String... args) {
        new XmlToJava()
            .add("map", x->table = table, Hashtable.class, String.class, String.class)
            .load(new File("test.xml"));
    }
}
```
A Hashtable is a subclass of java.util.Map and XmlToJava handles Map in the same way 
for all its subclasses.
* The key must have a simple class because the key is parsed from the text that makes
up the name of
entry element. In the example, the keys
are key1 and key2. Restricting keys to simple types is a design decision taken to
simplify the xml for most cases. The alternative would be to support completely
generalized classes for keys with a more complex xml representation. If
this is necessary, read a Collection of tuple and provide post-load code to 
convert items in the Collection to a Map (example below).
* If the value is a simple class, the text content of a key element is
parsed to produce a value instance.
* If the value is not a simple class, it is wrapped in its own elements
that conform to the generic parameters.
### Map of non-simple class
If the value of a map entry is not a simple class, it is wrapped in its own elements
that contain the data for that value.
```java
class test {
    public class Tuple {
        public String a;
        public String b;
    }

    public class ContainsMapOfTuple {
        public String name;
        public Hashtable<String, Tuple> map;
    }

    void run() {
        String xml = "...";
        new XmlToJava()
                .add("containsMap", x -> this.containsMap = x, ContainsMapOfTuple.class)
                .load(new StringReader(xml));
    }
}
```
XML
```xml
<?xml version="1.0"?>
<options>
    <containsMap>
        <name>contains map of tuple</name>
        <map>
            <key1>
                <a>one</a>
                <b>two</b>
            </key1>
            <key2>
                <a>three</a>
                <b>four</b>
            </key2>
        </map>
    </containsMap>
</options>
```
### Anonymous map of non-simple class
In this example, a map of non-simple class is read from the anonymous
level of an xml file.
```java
    public static class Tuple {
        public String a;
        public String b;
    }

    public void run() {
        new XmlToJava()
            .add("map", x->System.out.println(x), 
                    Hashtable.class, String.class, Tuple.class)
            .load(new StringReader(xml));
    }
```
Because it's anonymous, the raw type and key-and-value types must be specified
for the element that introduces the map data.
```xml
<?xml version="1.0"?>
<options>
    <map>
        <key1>
            <a>one</a>
            <b>two</b>
        </key1>
        <key2>
            <a>three</a>
            <b>four</b>
        </key2>
    </map>
</options>
```
* This example would also work if the root element was discarded.
### Map using non-trival keys
Here is an example for the truly pedantic. A map with non-simple keys
is created from a collection of tuples where the first element of the tuple
represents the key and the second element represents the value.
In the example, the key is a set of String and the value is a list of Integer.
After the collection is loaded, it is passed to a consumer that 
processes the list to produce a table.
```java
public class TestNontrivialKey implements Runnable {
    public static class Tuple {
        public HashSet<String> key;
        public ArrayList<Integer> value;
    }

    Hashtable<Set<String>, Collection<Integer>> table;

    public void run() {
        String xml = ""; // see below
        ArrayList<Tuple> tupleList;
        new XmlToJava()
                .add("list", this::consume, ArrayList.class, Tuple.class)
                .load(new StringReader(xml));
        System.out.println(table);
    }

    private <T> void consume(T collection) {
        ArrayList<Tuple> list = (ArrayList<Tuple>)collection;
        table = new Hashtable<>();
        for (Tuple t: list)
            table.put(t.key, t.value);
    }

    public static void main(String[] args) {
        new TestNontrivialKey().run();
    }
}
```
XML
```xml
<list>
    <item>
        <key>
            <item>a</item>
            <item>b</item>
        </key>
        <value>
            <item>14</item>
            <item>15</item>
        </value>
    </item>
</list>        
```
### User-defined simple class
Any class can be placed in the simple category by adding a `valueOf` factory method.
All wrapper classes have such a method.

Here is an example of a Vector class treated as a simple class.
```java
   public class Vector {
        public double [] values;
        public Vector(double [] values) {
            this.values = values;
        }

        public static Vector valueOf(String string) {
            double [] values;
            String [] strings = string.split(",");
            values = new double[strings.length];
            for (int i = 0; i < strings.length; i++)
                values[i] = Double.valueOf(strings[i]);
            return new Vector(values);
        }

    @Override
    public String toString() {...}
    }
```
`Vector` is embedded in an aggregate class.
```java
    public static class Vectors {
        public Vector v1;
        public Vector v2;

        @Override
        public String toString() {...}
    }
```
XML
```xml
<?xml version="1.0"?>
<root>
    <vectors>
        <v1>1,2,3</v1>
        <v2>4,5,6</v2>
    </vectors>
</root>
```
Java
```java
new XmlToJava()
    .add("vectors", m->System.out.println(m.toString()), Vectors.class)
    .load(...);
   ```
