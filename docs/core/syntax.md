---
toc_depth: 2
---

# Message Format Syntax

A message format string is the textual representation of a message. When parsed, it produces
an immutable `Message` object that can be formatted with parameter values and a locale. The
format string is composed of four types of parts that are concatenated during formatting:
literal text, parameter references, template references and post formatter invocations.

This page walks through the syntax from simple to complex, with examples and Java code
showing how each feature works in practice.


## Text

The simplest possible message is plain text. It contains no special syntax, just characters
that appear in the output exactly as written.

```java
messageSupport.message("Hello World!").format();
// "Hello World!"
```

Text may contain Unicode letters, numbers, punctuation and symbols. Multiple consecutive
whitespace characters are collapsed into a single space. Control characters (U+0000 through
U+001F) are silently ignored by the parser.

### Escape Sequences

Some characters have special meaning in the message format syntax. The percent sign followed
by `{`, `[` or `(` starts a parameter, template or post formatter part respectively. To
include these characters literally in your text you need to escape the percent sign.

The following escape sequences are recognized:

| Escape | Character |
|---|---|
| `\\` | backslash |
| `\'` | single quote |
| `\"` | double quote |
| `\%` | percent sign |
| `\{` | left curly brace |
| `\[` | left square bracket |
| `\uXXXX` | Unicode character (4 hex digits) |

In the following example the literal text `%{` must be escaped because the parser would
otherwise interpret it as the start of a parameter reference. Note that in Java string
literals the backslash itself must be doubled.

```java
messageSupport
    .message("Use \\%{name} to insert a parameter.")
    .format();
// "Use %{name} to insert a parameter."
```

The Unicode escape is useful for inserting characters that may not be available in your source
file encoding.

```java
messageSupport
    .message("Price: \\u20ac 49.99")
    .format();
// "Price: € 49.99"
```


## Parameters

A parameter inserts a formatted value into the message. The simplest form references a named
value using `%{parameterName}`. When the message is formatted, the parameter is replaced with
the string representation of the value that was provided for that name.

Parameter names follow kebab-case or camelCase conventions. A name must start with a letter
and may contain letters, digits, and `_` or `-` separators. Each separator must be followed
by at least one letter or digit.

/// note
The keywords `null`, `empty`, `true`, `false` and `format` are also valid parameter names.
///

### Simple Parameter

The most basic use of a parameter is direct value substitution. You provide the value using
the `.with(name, value)` method on the message configurer.

```java
messageSupport
    .message("Hello %{name}!")
    .with("name", "Alice")
    .format();
// "Hello Alice!"
```

Multiple parameters can appear in the same message. Each one is substituted independently.

```java
messageSupport
    .message("%{firstName} %{lastName} is %{age} years old.")
    .with("firstName", "Bob")
    .with("lastName", "Smith")
    .with("age", 42)
    .format();
// "Bob Smith is 42 years old."
```

### Named Format

By default, the library selects a formatter based on the type of the value. You can override
this by specifying a named formatter explicitly with the `format` keyword. The syntax is
`format:<name>` where the name identifies a registered `NamedParameterFormatter`.

Built-in named formatters include `string`, `bool`, `choice`, `size` and `classifier`.

Explicitly selecting a named formatter is particularly useful when the automatic type-based
selection would pick a different formatter than the one you need. In the following example the
parameter value is the integer `4`. Without `format:bool` the library would select the number
formatter. By forcing the `bool` formatter, the numeric value is interpreted as a boolean
instead: any number that is not equal to zero is considered `true`, so the value `4` matches
the `true` map entry.

```java
messageSupport
    .message("%{active,format:bool,true:'yes',false:'no'}")
    .with("active", 4)
    .format();
// "yes"
```

### Configuration Keys

In addition to `format`, a parameter can carry any number of configuration key-value pairs.
These provide additional settings to the formatter that handles the parameter. The syntax is
`key:value` where the value can be a boolean (`true`/`false`), a number, a plain string or a
quoted message.

Different formatters recognize different configuration keys. For example, the temporal
formatter recognizes the `date` key, and the iterable/array formatter recognizes `list-sep`
and `list-sep-last`.

```java
messageSupport
    .message("%{ts,date:short}")
    .with("ts", LocalDate.of(2026, 5, 1))
    .locale(Locale.US)
    .format();
// "5/1/26"
```

The next example configures the separator and the last-item separator for formatting a list
of values.

```java
messageSupport
    .message("%{items,list-sep:', ',list-sep-last:' and '}")
    .with("items", List.of("apples", "bananas", "cherries"))
    .format();
// "apples, bananas and cherries"
```


## Map Keys

Map keys are one of the most powerful features of the message format syntax. They allow a
parameter to select different output based on the parameter value. Each map entry consists of
a key that specifies a condition and a value that provides the text to use when the condition
matches.

The general structure looks like this:

```message-format
%{param, key1:'message1', key2:'message2', :'default'}
```

The value side of a map entry can be either a quoted message (using single or double quotes)
or a simple string. Quoted messages are full sub-messages that can contain nested parameter
references, template references and post formatter invocations. Simple strings are plain text
without any nesting.

There are five types of map keys: `null`, `empty`, `bool`, `number` and `string`. Each type
is described below with examples.

### Null Key

The `null` key matches when the parameter value is `null`. The negated form `!null` matches
any non-null value. This is useful for providing a fallback when a value might not be present.

```java
messageSupport
    .message("%{user,null:'anonymous',!null:'%{user}'}")
    .with("user", null)
    .format();
// "anonymous"

messageSupport
    .message("%{user,null:'anonymous',!null:'%{user}'}")
    .with("user", "Alice")
    .format();
// "Alice"
```

### Empty Key

The `empty` key matches when the parameter value is considered empty. What counts as "empty"
depends on the type: null values, empty strings, empty collections and empty arrays are all
considered empty. The negated form `!empty` matches non-empty values.

```java
messageSupport
    .message("%{query,empty:'no search term',!empty:'searching for: %{query}'}")
    .with("query", "")
    .format();
// "no search term"

messageSupport
    .message("%{query,empty:'no search term',!empty:'searching for: %{query}'}")
    .with("query", "hello")
    .format();
// "searching for: hello"
```

### Bool Key

The `true` and `false` keys match when the parameter value is (or can be interpreted as) a
boolean. This is a straightforward way to produce different text for on/off or yes/no
scenarios.

```java
messageSupport
    .message("%{enabled,true:'on',false:'off'}")
    .with("enabled", true)
    .format();
// "on"
```

### Number Key

Number keys match integer values. They can be combined with relational operators to express
ranges. The supported operators are `=` (equal, also the default when no operator is given),
`!` or `<>` (not equal), `<` (less than), `<=` (less than or equal), `>` (greater than) and
`>=` (greater than or equal). Numbers must be integers and may be negative.

A common use case is pluralization, where you provide specific text for zero, one and many
items.

```java
messageSupport
    .message("%{count,0:'no items',1:'one item',:'%{count} items'}")
    .with("count", 0)
    .format();
// "no items"

messageSupport
    .message("%{count,0:'no items',1:'one item',:'%{count} items'}")
    .with("count", 1)
    .format();
// "one item"

messageSupport
    .message("%{count,0:'no items',1:'one item',:'%{count} items'}")
    .with("count", 42)
    .format();
// "42 items"
```

Relational operators allow you to match ranges of values. In the following example, any
negative number matches `<0`, zero matches `0`, and any positive number matches `>0`.

```java
messageSupport
    .message("%{temp,<0:'freezing',0:'zero',>0:'warm'}")
    .with("temp", -10)
    .format();
// "freezing"
```

### String Key

String keys match against string values. They use the same relational operators as number
keys, but the comparison is locale-aware. The string itself must be quoted with single or
double quotes.

```java
messageSupport
    .message("%{status,'active':'running','stopped':'halted',:'unknown state'}")
    .with("status", "active")
    .format();
// "running"
```

### Grouped Keys

Sometimes multiple keys should map to the same value. Rather than repeating the value for
each key, you can group the keys in parentheses. The grouped keys are separated by commas and
the group is followed by a colon and the shared value.

```java
messageSupport
    .message("%{day,(1,7):'weekend',(2,3,4,5,6):'weekday'}")
    .with("day", 7)
    .format();
// "weekend"
```

### Default Entry

A trailing entry with no key serves as a fallback when no other key matches. It consists of
just a colon followed by the value. The default entry must always be the last entry in the
parameter.

```java
messageSupport
    .message("%{level,1:'low',2:'medium',3:'high',:'unknown'}")
    .with("level", 99)
    .format();
// "unknown"
```

### The Choice Formatter

The `choice` formatter is a named formatter specifically designed for value-based selection.
It supports all map key types and picks the best matching entry. Unlike the default type-based
formatters, the choice formatter does not convert or format the value itself. Instead it
purely acts as a selector that picks one of several mapped messages based on the value.

A typical use case is pluralization where the same parameter appears both as a number and as
part of the choice logic.

```java
messageSupport
    .message("%{n} %{n,format:choice,1:'colour',:'colours'}.")
    .with("n", 1)
    .format();
// "1 colour."

messageSupport
    .message("%{n} %{n,format:choice,1:'colour',:'colours'}.")
    .with("n", 4)
    .format();
// "4 colours."
```

### The Size Formatter

The `size` formatter determines the size of a value and makes that size available for mapping.
It can determine the length of a string, the size of a collection, the length of an array and
similar measurements. The size calculation is delegated to `SizeQueryable` formatters
registered for the value's type.

The resulting size can then be mapped to custom text using number map keys. If no mapping is
provided, the numeric size is formatted as text.

```java
messageSupport
    .message("%{list,format:size,0:'empty',1:'single',:'%{list,format:size} elements'}")
    .with("list", List.of("a", "b", "c"))
    .format();
// "3 elements"
```


## Quoted Messages

Inside parameter, template and post formatter parts, single-quoted (`'...'`) and
double-quoted (`"..."`) strings define sub-messages. These quoted messages are not just plain
text. They are full messages that can themselves contain nested `%{...}`, `%[...]` and
`%(...)` references.

This is what makes map entries so powerful. The value side of a map entry can be a quoted
message that references other parameters, templates or post formatters, allowing for
arbitrarily complex formatting logic.

```java
messageSupport
    .message("%{amount,>1000:'high: %{amount}',:'normal'}")
    .with("amount", 5000)
    .format();
// "high: 5000"
```

An empty quoted string (`''` or `""`) represents an empty message and produces no output.


## Templates

A template is a reusable message fragment identified by a name. Templates allow you to define
a piece of formatting logic once and reference it from multiple messages. This is particularly
useful for complex formatting patterns that would otherwise be duplicated.

A template reference uses the syntax `%[template-name]`. The template itself is a regular
message that is registered separately on the `ConfigurableMessageSupport`.

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());

messageSupport.addTemplate("greeting",
    MessageFactory.NO_CACHE_INSTANCE.parseTemplate("Hello %{who}!"));

messageSupport
    .message("Result: %[greeting]")
    .with("who", "World")
    .format();
// "Result: Hello World!"
```

By default, a template accesses the same parameters as the enclosing message. The template
in the example above reads the `who` parameter directly from the values provided to the
message configurer.

### Parameter Delegation

Parameter delegation allows you to rename a parameter so the template sees it under a
different name. The syntax is `templateParam->messageParam`. This is useful when the template
uses a generic parameter name but the enclosing message has a more specific one.

In the following example, the template expects a parameter called `who`, but the message
provides the value under the name `userName`. The delegation `who->userName` bridges this gap.

```java
messageSupport.addTemplate("greeting",
    MessageFactory.NO_CACHE_INSTANCE.parseTemplate("Hello %{who}!"));

messageSupport
    .message("%[greeting,who->userName]")
    .with("userName", "Alice")
    .format();
// "Hello Alice!"
```

### Parameter Defaults

A template reference can provide default values for parameters that may not be available in
the enclosing context. Default values can be strings (quoted), numbers or booleans. If the
enclosing message does not provide a value for the parameter, the default is used instead.

```java
messageSupport.addTemplate("greeting",
    MessageFactory.NO_CACHE_INSTANCE.parseTemplate("Hello %{who}!"));

messageSupport
    .message("%[greeting,who='stranger']")
    .format();
// "Hello stranger!"
```

The syntax for numeric and boolean defaults is straightforward: `count=0` or `verbose=false`.

### Combining Delegation and Defaults

Delegation and defaults can be mixed freely in a single template reference. This gives you
full control over how the template's parameters are wired to the enclosing message.

In and of itself this is a simple concept, but it becomes very powerful for templates that
are used across multiple messages with different parameter naming conventions.

```java
var factory = MessageFactory.NO_CACHE_INSTANCE;

messageSupport.addTemplate("order-line",
    factory.parseTemplate("%{qty}x %{product}"));

messageSupport
    .message("Order: %[order-line,product->item,qty=1]")
    .with("item", "Widget")
    .format();
// "Order: 1x Widget"

messageSupport.message("Order: %[order-line,product->item]")
    .with("item", "Gadget")
    .with("qty", 3)
    .format();
// "Order: 3x Gadget"
```


## Post Formatters

A post formatter applies a text transformation to the result of a sub-message. Unlike
parameter formatters which operate on individual values, post formatters operate on already
formatted text. The syntax is:

```message-format
%(formatter-name, 'sub-message', config:value)
```

The first argument is the name of the post formatter. The second argument is a quoted message
whose formatted result will be transformed. Any additional arguments are configuration
key-value pairs that control the transformation.

### Case Conversion

The `case` post formatter converts text to uppercase or lowercase. The target case is
specified by the `case` configuration key, which accepts `upper` (or `uppercase`) and `lower`
(or `lowercase`). The conversion is locale-aware, meaning it respects locale-specific casing
rules.

```java
messageSupport
    .message("%(case,'%{name}',case:upper)")
    .with("name", "alice")
    .locale(Locale.US)
    .format();
// "ALICE"
```

```java
messageSupport
    .message("%(case,'%{label}',case:lowercase)")
    .with("label", "WARNING")
    .locale(Locale.US)
    .format();
// "warning"
```

### Clipping

The `clip` post formatter truncates text to a maximum length. The maximum length is specified
by the `clip` configuration key as a numeric value. When the text exceeds this length, it is
truncated and by default an ellipsis character is appended to indicate that the text has been
clipped.

```java
messageSupport
    .message("%(clip,'%{description}',clip:20)")
    .with("description", "This is a very long description that should be truncated")
    .format();
// "This is a very long…"
```

The suffix behavior can be customized. Setting `clip-suffix` to `false` disables the suffix
entirely and performs a hard truncation at the exact maximum length. Alternatively, you can
provide a custom suffix string using `clip-suffix-text`.

```java
messageSupport
    .message("%(clip,'%{text}',clip:10,clip-suffix:false)")
    .with("text", "Hello World, this is too long")
    .format();
// "Hello Worl"
```

```java
messageSupport
    .message("%(clip,'%{text}',clip:15,clip-suffix-text:'...')")
    .with("text", "A sentence that is way too long")
    .format();
// "A sentence t..."
```


## Putting It All Together

The following example combines all four part types in a single message. It uses a post
formatter to uppercase the user name, a parameter with map keys to handle the empty-user
case, a template reference with parameter delegation for the item count, and plain text to
tie everything together.

```java
var factory = MessageFactory.NO_CACHE_INSTANCE;
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());

// Define a reusable template for item counts
messageSupport.addTemplate("item-count",
    factory.parseTemplate(
        "%{n,format:choice,0:'no items',1:'1 item',:'%{n} items'}"));

// A complex message combining all features
String msg = 
    "%(case,'%{user,!empty:'%{user}',:'Guest'}',case:upper) " +
    "has %[item-count,n->cartSize] in the cart.";

messageSupport
    .message(msg)
    .with("user", "alice")
    .with("cartSize", 3)
    .locale(Locale.US)
    .format();
// "ALICE has 3 items in the cart."

messageSupport
    .message(msg)
    .with("user", null)
    .with("cartSize", 0)
    .locale(Locale.US)
    .format();
// "GUEST has no items in the cart."
```
