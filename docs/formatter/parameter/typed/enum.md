# Enum

This formatter is included in the `DefaultFormatterService`.

When a parameter value is a Java enum constant, the library automatically selects the `EnumFormatter`. This
formatter is registered for the `Enum` type and therefore handles all enum types without any additional
configuration.

By default, the formatter outputs the enum constant's name as returned by the `name()` method.

```java
messageSupport
    .message("%{direction}")
    .with("direction", Direction.NORTH)
    .format();
// "NORTH"
```

```java
messageSupport
    .message("Status: %{status}")
    .with("status", OrderStatus.PENDING)
    .format();
// "Status: PENDING"
```


## The `enum` Configuration Key

The formatter uses the `enum` configuration key to select what aspect of the enum constant to output. Two modes
are available: `name` and `ordinal`. If the configuration key contains a value that does not match any of these
options, formatting is delegated to the next formatter in the chain.

### Name Mode (Default)

When the `enum` configuration key is absent or set to `name`, the formatter outputs the constant's name. This is
the default behavior.

```java
messageSupport
    .message("%{color,enum:name}")
    .with("color", Color.RED)
    .format();
// "RED"
```

Since `name` is the default, writing `enum:name` explicitly is equivalent to omitting the configuration entirely.

### Ordinal Mode

When the `enum` configuration key is set to `ordinal` (or its short form `ord`), the formatter outputs the
constant's ordinal value as a number.

```java
messageSupport
    .message("%{priority,enum:ordinal}")
    .with("priority", Priority.HIGH)
    .format();
// "2"  (assuming HIGH is the third constant, ordinal 2)
```

```java
messageSupport
    .message("%{day,enum:ord}")
    .with("day", DayOfWeek.WEDNESDAY)
    .format();
// "2"  (assuming WEDNESDAY has ordinal 2)
```

The ordinal mode can also be set as a system-wide default using `setDefaultConfig`:

```java
messageSupport.setDefaultConfig("enum", "ordinal");

messageSupport
    .message("%{item}")
    .with("item", MyEnum.BB)
    .format();
// "1"  (assuming BB has ordinal 1)
```


## Map Keys

### String Keys (Name Mode)

In name mode, string map keys are compared against the constant's name. This allows you to map specific enum
constant names to custom text.

```java
messageSupport
    .message("%{status,'PENDING':'Awaiting review','APPROVED':'Ready','REJECTED':'Denied'}")
    .with("status", OrderStatus.APPROVED)
    .format();
// "Ready"
```

Comparison operators work as well. String keys are compared lexicographically against the constant's name.

```java
messageSupport
    .message("%{e,>'C':upper,<'C':lower}")
    .with("e", MyEnum.CC)
    .format();
// "upper"
```

A default map key catches any constant that does not match a specific key.

```java
messageSupport
    .message("%{e,'AA':first,'BB':second,:'other'}")
    .with("e", MyEnum.CC)
    .format();
// "other"
```

### Number Keys (Ordinal Mode)

In ordinal mode, number map keys are compared against the constant's ordinal value. This allows you to map
specific ordinal positions to custom text.

```java
messageSupport
    .message("%{level,enum:ordinal,0:'lowest',1:'low',2:'medium',3:'high'}")
    .with("level", Level.MEDIUM)
    .format();
// "medium"  (assuming MEDIUM has ordinal 2)
```

### Null Handling

When the parameter value is `null`, the formatter outputs an empty string by default. You can provide a `null`
map key to produce specific text.

```java
messageSupport
    .message("%{role,null:'unassigned'}")
    .with("role", (Role) null)
    .format();
// "unassigned"
```


## Unmatched Values

When a string or number map key is present but the enum constant's name or ordinal does not match any of them,
the formatter outputs an empty string. If a default map key (`:`) is provided, it is used instead.

```java
messageSupport
    .message("%{color,'RED':'warm','BLUE':'cool'}")
    .with("color", Color.GREEN)
    .format();
// ""

messageSupport
    .message("%{color,'RED':'warm','BLUE':'cool',:'neutral'}")
    .with("color", Color.GREEN)
    .format();
// "neutral"
```
