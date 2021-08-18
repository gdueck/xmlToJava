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
* Element tags are in terminology from the
user domain rather than the programming domain.
* aliasing keeps package information on the Java side 
so package information is not necessary on the XML side.

## xml layout
* The element at the document root is typically "options" or "config" 
but may be anything. Its role is to contain data for one or more class instances.
* Every element with the exception of the root element 
is associated with a type, nominally a class, which can be
  categorized as simple, aggregate, collection or map.
* Elements directly under the document root represent anonymous instances that
are passed on to Consumers for disposition. All other elements represent fields,
array items or map items.
* For fields with simple types, the text content of a corresponding 
element must contain a parseable representation of that type.
* For fields whose type is a subclass of Collection, e.g. ArrayList, 
subelements contain items
of the collection and the name of the subelement is ignored..
* For fields whose type is a subclass of Map, e.g. Hashtable, the name 
of each subelement
is used as a key and
the content of the subelement is used as value.
* Remaining fields are aggregate and sub elements represent fields within
contained classes.
## Design Notes
* An single xml file can contain data destined for more than one instance and
conforming to more than one class.
* Class definitions and their instances can be scattered throughout
the program source for reasons of modularity.
* an xml tag _may_ be interpreted as a class, 
but only where an anonymous instance is expected
* Every element is associated with a type, which can be
  categorized as class, simple, aggregate, collection, collection item, map or map entry.
  * the type of the field, taken by reflection from the class being populated,
   determines the interpretation of an element's text content.
* a field must be public or have a public setter.
* a field may be static.
* unconsumed anonymous instances are still useful if they contain static fields.
* simple types are String, primitive or primitive wrapper, or enumeration. Essentially,
a simple type is one that can be parsed from a String and this means any type that has
public static method with the signature "resultclass valueOf(String)" is permitted.
A program-defined aggregate class, like a three-part vector, a two-part dimension, or a one-part size
can be treated as a simple type if it has a valueOf method.

## Examples
### Simple alias
The XML to specify a single boolean alias is this:
```xml
<?xml version="1.0"?>
<options>
    <flag>true</flag>
</options>
```
`XmlToJava.load()` does not return a value (because it would typically
have to return many values), so anything recognized from the top level of an xml file
must be fed to a consumer.
```java
class Test {}
    private static boolean flag;
    
    public static void main(String ... args) {
        System.out.println("---test10");
        new XmlToJava()
        .add("flag", b->flag = b, Boolean.class)
        .load(new File("test10.xml"));
        System.out.println(String.format("flag=%s", flag));
    }
}
```
* The simplest form of `add` associates an alias with a raw class, 
but here the three-argument
form takes alias, consumer and raw class.
* When XmlToJava sees &lt;flag>, it uses the Boolean class. The 
* Boolean.valueOf creates 
a value from the text content of &lt;flag>.
### Aggregate alias
A Java class like
```Java
public class TestClass {
    public int intField;
    public Boolean boolfield;
    public String stringField;
    static private String privateString;
    static private String inaccessibleField;
    @Hidden
    static public String password;
    public enum EnumList {One, Two};
    public EnumList enumVar;
    static public void setPrivateString(String alias) {
        privateString = alias;
    }
}
```
is loaded by 
```Java
XmlToJava reader = new XmlToJava()
  .add("test", x->this.testClass = x, TestClass.class)
  .load(new File("test.xml"));
```
and is populated from XML written as
```xml 
<options>
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
refers to a field o  `TestClass`. The class of a referenced field
guides the parsing of its referent. Processing proceeds as for simple fields.

 
`TestClass` contains a combination of static, instance, public and private fields.
The add method maps the tag "test" onto "package-name.TestClass" 
and also provides a lambda expression
to be invoked when an instance of TestClass has been recognized.
* `intField` is simple (because it's primitive) and will be processed by the `valueOf` method of Integer.
* `boolField` is simple because its class has a valueOf method (as do all wrapper classes).
* `privateString` is inaccessible
but has a setter.
* `inaccessibleString` cannot be given a alias because it is private and has no setter.
* `password` is marked as "@Hidden" so the default display algorithm won't show a password
in a public log. (`@Hidden` is in the same package as `XmlToJava`).
* `enumVar` is simple because `EnumList` has a `valueOf` method.

###Collection Example
A class that contains a collection
```java
public class ContainsArray {
    public String name;
    public ArrayList<Integer> list;
}
```
Java code
```java
static ContainsArray containsArray;

static void main(String ... args) {
    new XmlToJava()
    .add("containsArray", x->this.containsArray = x, ContainsArray.class)
    .load("containsArray.xml");
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
<options>
  <list>
      <item>1</item>
      <item>47</item>
      <item>13</item>
  </list>
</options>
```
If the intent is to create a collection, the Java code would look like this.
```java
        String xml = "...";
        new XmlToJava()
            .add("list", x->someField=x, ArrayList.class, Integer.class)
            .load(new StringReader(xml));

```
* Note the extra parameter to `add` which provides the element class for the collection.
* This is equivalent to `ArrayList<Integer>`.
###Map of simple class
Similar to the example for Collections, this example has a 
class with a Hashtable field.

XML
```xml
<options>
    <containsMap>
        <name>name</name>
        <map>
            <key1>value1</key1>
            <key2>value2</key2>
        </map>
    </containsMap>
</options>
```
A Hashtable is a subclass of Map and XmlToJava handles Map in the same way 
for all its subclasses.
* The key must have a simple class because the key is parsed from the text that makes
up the names of
elements directly under the map element. In the example, the keys
are key1 and key2. This is a design decision taken to
simplify the xml for maps. The alternative would be to support completely
generalized classes for keys with a more complex xml representation.
* If the value is a simple class, the text content of a key element is
used parsed to produce a value instance.
* If the value is not a simple class, it is wrapped in its own elements
that contain the
### Map of non-simple class
If the value of a map is not a simple class, it is wrapped in its own elements
  that contain the data for that value.
```java
    public class Tuple {
        public String a;
        public String b;
    }
    
    public class ContainsMapOfTuple {
        public String name;
        public Hashtable<String,Tuple> map;
    }

    void run() {
        String xml = "...";
        new XmlToJava()
            .add("containsMap", x -> this.containsMap = x, ContainsMapOfTuple.class)
            .load(new StringReader(xml));
    }
```
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
###Anonymous map of non-simple class
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
Because its anonymous, the raw type and key-and-value types must be specified
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