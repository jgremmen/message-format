# Boolean

This formatter is included in the `DefaultFormatterService`.

The library offers three formatters that deal with boolean values. It is important to understand that these
formatters fall into two fundamentally different categories: type-based formatters that are selected automatically
when a parameter holds a boolean-related Java type, and a named formatter that you select explicitly to force
boolean interpretation on values that are not inherently boolean.


## Map Key Types

All boolean formatting, regardless of whether the formatter was selected by type or by name, supports the same
four map key types: `bool`, `string`, `null` and `empty`.

The **bool** keys `true` and `false` map the boolean value to custom text. This is the most common and
recommended way to customize boolean output.

```java
messageSupport
    .message("%{active,true:'enabled',false:'disabled'}")
    .with("active", true)
    .format();
// "enabled"
```

**String** keys match against the string representation of the boolean value, although this is not recommended.
Use `true` and `false` bool keys instead.

```java
messageSupport
    .message("%{flag,'true':ja,'false':nein}")
    .with("flag", true)
    .format();
// "ja"
```

The **null** key matches when the parameter value is `null`. The **empty** key matches when the value cannot be
converted to a boolean (this is only relevant for the named `bool` formatter, since type-based formatters always
receive a valid boolean). The negated forms `!null` and `!empty` are also available.

```java
messageSupport
    .message("%{flag,null:'unspecified',true:'yes',false:'no'}")
    .with("flag", (Boolean) null)
    .format();
// "unspecified"
```

When no map keys are provided, the formatter outputs the text `true` or `false`. For `null` values it outputs
an empty string.


## Type-Based Formatters (Automatic Selection)

The formatters described in this section are selected automatically by the library based on the Java type of the
parameter value. They handle values that are already boolean in nature and simply need to be rendered as text or
matched against map keys.

### Boolean and primitive boolean

When you pass a `Boolean` object or a primitive `boolean` as a parameter value, the library automatically selects
the appropriate formatter. No format configuration is required. The formatter renders `true` or `false` as plain
text.

```java
messageSupport
    .message("%{active}")
    .with("active", true)
    .format();
// "true"
```

```java
messageSupport
    .message("%{active}")
    .with("active", false)
    .format();
// "false"
```

Because the value is already a boolean, you can use `true` and `false` map keys directly to produce custom text
for each state.

```java
messageSupport
    .message("%{active,true:'enabled',false:'disabled'}")
    .with("active", true)
    .format();
// "enabled"
```

```java
messageSupport
    .message("Status: %{ready,true:'ready to go',false:'not yet'}")
    .with("ready", false)
    .format();
// "Status: not yet"
```

When the parameter value is `null`, the formatter outputs an empty string by default. You can provide a `null`
map key to produce specific text for that case.

```java
messageSupport
    .message("%{flag}")
    .with("flag", (Boolean) null)
    .format();
// ""

messageSupport
    .message("%{flag,null:'unspecified',true:'yes',false:'no'}")
    .with("flag", (Boolean) null)
    .format();
// "unspecified"
```

### BooleanSupplier

When a parameter value implements `java.util.function.BooleanSupplier`, the library automatically selects the
`BooleanSupplierFormatter`. This formatter calls `getAsBoolean()` on the supplier to obtain the actual boolean
value and then delegates all rendering to the boolean formatter. The supplier is evaluated once at format time.

From the perspective of map key matching and text output, the result is identical to passing a plain `boolean`.
All map key types that work with `Boolean` values work with `BooleanSupplier` values as well.

```java
BooleanSupplier checker = () -> Files.exists(Path.of("/tmp/lockfile"));

messageSupport
    .message("Locked: %{locked,true:'yes',false:'no'}")
    .with("locked", checker)
    .format();
// "Locked: yes" (if /tmp/lockfile exists)
// "Locked: no"  (if /tmp/lockfile does not exist)
```

```java
BooleanSupplier isReady = () -> true;

messageSupport
    .message("System %{status,true:'operational',false:'offline'}")
    .with("status", isReady)
    .format();
// "System operational"
```

A `null` parameter value is not passed to the supplier. The formatter outputs an empty string unless a `null`
map key is present.

```java
messageSupport
    .message("%{check,null:'no checker provided',true:'ok',false:'failed'}")
    .with("check", (BooleanSupplier) null)
    .format();
// "no checker provided"
```

### AtomicBoolean

When the parameter value is a `java.util.concurrent.atomic.AtomicBoolean`, the library automatically selects the
`AtomicBooleanFormatter`. This formatter reads the current boolean value by calling `get()` and then delegates to
the boolean formatter. The value is read once at format time; if another thread modifies the `AtomicBoolean`
between message construction and formatting, the formatter uses whatever value `get()` returns at the moment of
formatting.

All map keys and output behavior are identical to those described for `Boolean` parameters.

```java
AtomicBoolean toggle = new AtomicBoolean(true);

messageSupport
    .message("Feature: %{feature,true:'enabled',false:'disabled'}")
    .with("feature", toggle)
    .format();
// "Feature: enabled"
```

```java
AtomicBoolean maintenance = new AtomicBoolean(false);

messageSupport
    .message("Maintenance mode: %{mode}")
    .with("mode", maintenance)
    .format();
// "Maintenance mode: false"
```

As with the other type-based formatters, a `null` parameter value produces an empty string by default.

```java
messageSupport
    .message("%{flag,null:'unset',true:'on',false:'off'}")
    .with("flag", (AtomicBoolean) null)
    .format();
// "unset"
```


## Named Formatter `bool` (Explicit Selection)

The named formatter `bool` is documented on its own page: [Bool](../named/bool.md). It serves an entirely
different purpose from the type-based formatters described above. You select it explicitly by writing `format:bool`
to coerce values that are not inherently boolean (such as numbers, strings or optionals) into a boolean result.
