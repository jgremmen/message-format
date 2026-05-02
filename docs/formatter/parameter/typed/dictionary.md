# Dictionary

This formatter is included in the `DefaultFormatterService`.

The `DictionaryFormatter` is a type-based formatter registered for `java.util.Dictionary`. It is automatically
selected whenever a parameter value is a `Dictionary` subclass, which includes `Hashtable` and `Properties`.

The formatter's primary feature is looking up a value inside the dictionary using the `key` configuration entry.
The looked-up value is then formatted using the appropriate type-based formatter for its type. When no `key`
configuration is present, the formatter delegates to the next available formatter, which for `Hashtable` is
typically the [Map formatter](map.md) (since `Hashtable` implements `Map`).


## Key Lookup

The `key` configuration entry specifies which entry to retrieve from the dictionary. The key can be a string,
a number or a boolean value.

### String Keys

A string key performs a direct lookup in the dictionary. If the string is a single character and no entry is
found, a second lookup with the `char` value is attempted.

```java
Hashtable<String, String> config = new Hashtable<>();
config.put("host", "localhost");
config.put("port", "8080");

messageSupport
    .message("%{cfg,key:'host'}")
    .with("cfg", config)
    .format();
// "localhost"

messageSupport
    .message("%{cfg,key:'port'}")
    .with("cfg", config)
    .format();
// "8080"
```

### Number Keys

A number key attempts the lookup using several numeric types in sequence: `long`, `BigInteger`, `BigDecimal`,
`int`, `short` and `byte`. This covers dictionaries keyed by any of these types.

```java
Hashtable<Integer, String> codes = new Hashtable<>();
codes.put(200, "OK");
codes.put(404, "Not Found");

messageSupport
    .message("%{codes,key:200}")
    .with("codes", codes)
    .format();
// "OK"
```

### Boolean Keys

A boolean key performs a direct lookup using a `Boolean` value.

```java
Hashtable<Boolean, String> flags = new Hashtable<>();
flags.put(true, "enabled");
flags.put(false, "disabled");

messageSupport
    .message("%{flags,key:true}")
    .with("flags", flags)
    .format();
// "enabled"
```


## Missing Keys

When the key is not found in the dictionary, the result is treated as `null`. If a `null` or `empty` map key is
present in the parameter configuration, its message is used. Otherwise, the output is an empty string.

```java
Hashtable<String, String> config = new Hashtable<>();
config.put("host", "localhost");

messageSupport
    .message("%{cfg,key:'missing'}")
    .with("cfg", config)
    .format();
// ""

messageSupport
    .message("%{cfg,key:'missing',null:'not configured'}")
    .with("cfg", config)
    .format();
// "not configured"
```


## Without Key Configuration

When no `key` configuration entry is present, the formatter delegates to the next available formatter. For
`Hashtable` (which implements `Map`), this is the [Map formatter](map.md), so the entire dictionary is formatted
as a list of key-value pairs. For `Dictionary` subclasses that do not implement `Map`, the default string
formatter is used, which calls `toString()` on the dictionary.

```java
Hashtable<String, String> ht = new Hashtable<>();
ht.put("color", "red");
ht.put("shape", "circle");

messageSupport
    .message("%{ht}")
    .with("ht", ht)
    .format();
// "color=red, shape=circle" (or "shape=circle, color=red")
```


## Properties

`Properties` extends `Hashtable<Object, Object>` and is therefore handled by this formatter. Key lookup
works the same way.

```java
Properties props = new Properties();
props.setProperty("host", "localhost");
props.setProperty("port", "8080");

messageSupport
    .message("Server: %{p,key:'host'}:%{p,key:'port'}")
    .with("p", props)
    .format();
// "Server: localhost:8080"
```


## Null Handling

A `null` parameter value produces an empty string by default. You can provide a `null` map key to produce
specific text.

```java
messageSupport
    .message("%{cfg,key:'host',null:'no config'}")
    .with("cfg", null)
    .format();
// "no config"
```
