# Enum

This formatter is included in the `DefaultFormatterService`.

When a parameter value is a Java enum constant, the library automatically selects the `EnumFormatter`. This formatter
is registered for the `Enum` type and therefore handles all enum types without any additional configuration.

By default, the formatter outputs the enum constant's name as returned by the `name()` method.

The examples on this page use the following message support.

```java title="Example setup"
import de.sayayi.lib.message.MessageSupportFactory;

import java.time.DayOfWeek;

enum OrderStatus { PENDING, APPROVED, REJECTED, SHIPPED }
enum Priority { LOW, MEDIUM, HIGH, CRITICAL }
enum Grade { A, B, C, D, F }

var messageSupport = MessageSupportFactory.createDefault();
// all built-in formatters are available
```

```java
messageSupport
    .message("%{priority}")
    .with("priority", Priority.HIGH)
    .format();
// "HIGH"
```

```java
messageSupport
    .message("Status: %{status}")
    .with("status", OrderStatus.PENDING)
    .format();
// "Status: PENDING"
```


## The `enum` Configuration Key

The formatter uses the `enum` configuration key to select what aspect of the enum constant to output. Two modes are
available: `name` and `ordinal`. If the configuration key contains a value that does not match any of these options,
formatting is delegated to the next formatter in the chain.

### Name Mode (Default)

When the `enum` configuration key is absent or set to `name`, the formatter outputs the constant's name. This is the 
default behavior.

```java
messageSupport
    .message("%{status,enum:name}")
    .with("status", OrderStatus.SHIPPED)
    .format();
// "SHIPPED"
```

Since `name` is the default, writing `enum:name` explicitly is equivalent to omitting the configuration entirely.

### Ordinal Mode

When the `enum` configuration key is set to `ordinal` (or its short form `ord`), the formatter outputs the constant's
ordinal value as a number.

```java
messageSupport
    .message("%{priority,enum:ordinal}")
    .with("priority", Priority.HIGH)
    .format();
// "2"
```

```java
messageSupport
    .message("%{day,enum:ord}")
    .with("day", DayOfWeek.WEDNESDAY)
    .format();
// "2"
```

The ordinal mode can also be set as a default for all messages of a message support using `setDefaultConfig`. The
following example uses a separate instance, so the other examples on this page keep the name mode.

```java
var ordinalSupport = MessageSupportFactory.createDefault();
ordinalSupport.setDefaultConfig("enum", "ordinal");

ordinalSupport
    .message("%{priority}")
    .with("priority", Priority.MEDIUM)
    .format();
// "1"
```


## Map Keys

### String Keys (Name Mode)

In name mode, string map keys are compared against the constant's name. This allows mapping specific enum constant 
names to custom text.

```java
messageSupport
    .message("""
        %{status,\
            'PENDING':'Awaiting review',\
            'APPROVED':'Ready',\
            'REJECTED':'Denied'}\
        """)
    .with("status", OrderStatus.APPROVED)
    .format();
// "Ready"
```

Comparison operators work as well. String keys are compared lexicographically against the constant's name.

```java
messageSupport
    .message("%{grade,<='B':good,>'B':'needs improvement'}")
    .with("grade", Grade.D)
    .format();
// "needs improvement"
```

A default map key catches any constant that does not match a specific key.

```java
messageSupport
    .message("%{status,'PENDING':'Awaiting review',:'Processed'}")
    .with("status", OrderStatus.SHIPPED)
    .format();
// "Processed"
```

### Number Keys (Ordinal Mode)

In ordinal mode, number map keys are compared against the constant's ordinal value. Because the ordinal reflects the
declaration order, relational number keys can select ranges of constants. All priorities from `HIGH` onward are
treated as urgent in the following example.

```java
messageSupport
    .message("%{priority,enum:ordinal,>=2:'urgent',:'routine'}")
    .with("priority", Priority.HIGH)
    .format();
// "urgent"
```

### Null Handling

When the parameter value is `null`, the formatter outputs an empty string by default. A `null` map key can be provided
to produce specific text.

```java
messageSupport
    .message("%{status,null:'unknown'}")
    .with("status", (OrderStatus) null)
    .format();
// "unknown"
```


## Unmatched Values

When a string or number map key is present but the enum constant's name or ordinal does not match any of them, the
formatter outputs an empty string. If a default map key (`:`) is provided, it is used instead.

```java
messageSupport
    .message("%{priority,'LOW':'relaxed','CRITICAL':'immediate'}")
    .with("priority", Priority.MEDIUM)
    .format();
// ""

messageSupport
    .message("%{priority,'LOW':'relaxed','CRITICAL':'immediate',:'normal'}")
    .with("priority", Priority.MEDIUM)
    .format();
// "normal"
```
