# Map

This formatter is included in the `DefaultFormatterService`.

The library provides two formatters for map-related types: `MapFormatter` for `java.util.Map` values and
`MapEntryFormatter` for individual `Map.Entry` values. Both are automatically selected based on the parameter's
type.


## MapFormatter

The `MapFormatter` handles any `java.util.Map` value, including `HashMap`, `LinkedHashMap`, `TreeMap` and all
other map implementations. It formats each entry as a key-value pair and joins the results into a single text
string. Separator, truncation and overflow behavior are controlled by the same list configuration keys used by
the array and iterable formatters, plus two map-specific configuration keys.

```java
messageSupport
    .message("%{props}")
    .with("props", Map.of("host", "localhost"))
    .format();
// "host=localhost"
```

By default each entry is rendered as `key=(null)` when the value is `null`, `(null)=value` when the key is
`null`, and `key=value` otherwise. Multiple entries are separated by `", "`.

```java
Map<String,Integer> scores = new LinkedHashMap<>();
scores.put("Alice", 95);
scores.put("Bob", 87);
scores.put("Charlie", 72);

messageSupport
    .message("%{scores}")
    .with("scores", scores)
    .format();
// "Alice=95, Bob=87, Charlie=72"
```

### `map-kv`

The `map-kv` configuration key specifies a message format used to render each entry. Inside this message the
parameters `key` and `value` are available and refer to the entry's key and value respectively. This is how you
customize the appearance of each entry.

The default format is `%{key,null:'(null)'}=%{value,null:'(null)'}`.

```java
messageSupport
    .message("%{map,map-kv:'%{key} -> %{value}'}")
    .with("map", Map.of("name", "Alice"))
    .format();
// "name -> Alice"
```

```java
messageSupport
    .message("%{map,map-kv:'%{key}: %{value}'}")
    .with("map", Map.of("host", "localhost", "port", "8080"))
    .format();
// "host: localhost, port: 8080"
```

Because the `map-kv` message is a full message format, you can apply any formatting or map keys to the key and
value parameters individually.

```java
Map<Integer,Integer> data = new LinkedHashMap<>();
data.put(10, 1);
data.put(0, -1234);
data.put(-4, 0);

messageSupport
    .message("%{map,map-kv:'%{key,format:bool}:%{value}',list-sep:' / '}")
    .with("map", data)
    .format();
// "true:1 / false:-1234 / true:0"
```

```java
Map<String,Integer> items = new LinkedHashMap<>();
items.put("map1", 1);
items.put("map2", -1234);
items.put("map3", 8);

messageSupport
    .message("%{map,map-kv:'%{key} -> %{value,number:\"0000\"}',list-sep:', ',list-sep-last:' and '}")
    .with("map", items)
    .format();
// "map1 -> 0001, map2 -> -1234 and map3 -> 0008"
```

You can also customize how `null` keys and values appear inside the entry format:

```java
messageSupport
    .message("%{map,map-kv:'%{key,null:key}=%{value,null:value}'}")
    .with("map", Collections.singletonMap(null, null))
    .format();
// "key=value"
```

### `map-this`

The text to output when the map references itself as a key or value within one of its own entries. Defaults to
`(this map)`.

### List Configuration Keys

The `MapFormatter` inherits all list configuration keys from the array and iterable formatters. These control
how entries are separated and truncated:

- `list-sep` is the separator between entries (default: `", "`)
- `list-sep-last` is the separator before the last entry
- `list-max-size` is the maximum number of entries to include
- `list-value-more` is the overflow text appended when truncated
- `list-unique` suppresses duplicate entry texts

```java
Map<String, String> env = new LinkedHashMap<>();
env.put("HOME", "/home/user");
env.put("PATH", "/usr/bin");
env.put("LANG", "en_US");

messageSupport
    .message("%{env,list-max-size:2,list-value-more:'...'}")
    .with("env", env)
    .format();
// "HOME=/home/user, PATH=/usr/bin, ..."
```

### Map Keys

The `empty` key matches when the map contains no entries. Its negated form `!empty` matches when the map has at
least one entry.

```java
messageSupport
    .message("%{map,empty:'no entries'}")
    .with("map", Map.of())
    .format();
// "no entries"
```

The `null` key matches when the parameter value is `null`.

```java
messageSupport
    .message("%{map,null:'no map',empty:'empty map'}")
    .with("map", null)
    .format();
// "no map"
```

### Size Queries

The `MapFormatter` reports the number of entries in the map as its size.


## MapEntryFormatter

The `MapEntryFormatter` handles `java.util.Map.Entry` values. It is automatically selected whenever a parameter
value is a `Map.Entry` instance. This formatter uses the `entry` configuration key to select which part of the
entry to format.

### The `entry` Configuration Key

The `entry` configuration key determines what aspect of the entry is rendered:

- `key` formats the entry's key
- `value` formats the entry's value

If the `entry` configuration key is absent or set to an unrecognized value, formatting is delegated to the next
available formatter in the chain.

```java
Map.Entry<String,Integer> entry = Map.entry("score", 42);

messageSupport
    .message("Key: %{e,entry:key}")
    .with("e", entry)
    .format();
// "Key: score"
```

```java
Map.Entry<String,Integer> entry = Map.entry("score", 42);

messageSupport
    .message("Value: %{e,entry:value}")
    .with("e", entry)
    .format();
// "Value: 42"
```

Because the key or value is delegated to the formatter appropriate for its type, all map keys and configuration
that apply to that type work transparently.

```java
Map.Entry<String,Boolean> entry = Map.entry("active", true);

messageSupport
    .message("%{e,entry:value,true:'yes',false:'no'}")
    .with("e", entry)
    .format();
// "yes"
```

### Null Handling

A `null` parameter value produces an empty string by default. You can provide a `null` map key to handle this
case explicitly.

```java
messageSupport
    .message("%{e,null:'no entry'}")
    .with("e", (Map.Entry<?,?>) null)
    .format();
// "no entry"
```
