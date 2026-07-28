# Formatter Utilities

The core module includes several helper classes that solve problems encountered repeatedly when building formatters.
Instead of reimplementing space handling, text assembly, lazy initialization or lightweight parameter containers, these
ready-made utilities can be relied upon. They are the same building blocks that the library's own formatters use
internally.


## `MessageUtil` Space and String Methods

The library uses its own definition of what constitutes a "space character" because the standard
`Character.isSpaceChar(char)` and `String.trim()` treat newlines as whitespace characters. In the message format engine,
newlines are significant and must be preserved. The `MessageUtil` class provides a set of static methods that operate 
under this custom definition.

### `isSpaceChar(char)`

This method returns `true` if the given character is a tab (`\t`), a carriage return (`\r`), a Unicode `SPACE_SEPARATOR` 
(such as the regular space U+0020 or the non-breaking space U+00A0), or a Unicode `PARAGRAPH_SEPARATOR`. Notably, the 
newline character (`\n`) is **not** considered a space character. This distinction is critical because message templates
can contain intentional line breaks that must survive trimming and normalization.

```java
// regular space and tab are recognized as space characters
MessageUtil.isSpaceChar(' ');       // true
MessageUtil.isSpaceChar('\t');      // true

// non-breaking space (U+00A0) is a SPACE_SEPARATOR
MessageUtil.isSpaceChar('\u00A0');  // true

// newline is NOT a space character
MessageUtil.isSpaceChar('\n');      // false
```

### `trimSpaces(String)`

This method strips leading and trailing space characters from a string, where "space character" follows the definition 
of `isSpaceChar`. Unlike `String.trim()`, it does not remove newlines. If the input is `null`, the method returns
`null`. If the string contains only space characters, the result is an empty string. When the input already has no
leading or trailing spaces, the original `String` instance is returned, avoiding a needless allocation.

```java
MessageUtil.trimSpaces("  hello  ");      // "hello"
MessageUtil.trimSpaces("\t  hello\t ");   // "hello"
MessageUtil.trimSpaces(null);             // null

// newlines at the edges are preserved
MessageUtil.trimSpaces("\nhello\n");      // "\nhello\n"

// mixed: tabs are trimmed, newlines stay
MessageUtil.trimSpaces("\t\nhello\n\t");  // "\nhello\n"
```

### `trimAndNormalizeSpaces(String)`

This method goes a step further than `trimSpaces`. It trims leading and trailing spaces, collapses consecutive internal
space characters into a single ASCII space (U+0020) and replaces any remaining Unicode space character with a regular 
ASCII space. Newlines are left untouched throughout this process. The method returns `null` when the input is `null` 
and an empty string when the input contains only space characters.

```java
MessageUtil.trimAndNormalizeSpaces("  hello   world  ");
// "hello world"

// non-breaking spaces (U+00A0) are normalized to regular spaces
MessageUtil.trimAndNormalizeSpaces("hello\u00A0\u00A0world");
// "hello world"

// newlines are preserved even when surrounded by spaces
MessageUtil.trimAndNormalizeSpaces("  hello \n world  ");
// "hello \n world"

// tabs and regular spaces interleaved
MessageUtil.trimAndNormalizeSpaces("\thello\t \tworld\t");
// "hello world"
```

This method is especially useful when a formatter receives user-supplied text that may contain a mix of Unicode
whitespace characters and clean, consistently spaced output is desired without destroying intentional line breaks.

### `isTrimmedEmpty(String)`

This method returns `true` if the given string has zero length or consists entirely of space characters as defined by
`isSpaceChar`. It does not accept `null`. Use this to tell apart a string that is visually empty from one that contains 
meaningful content.

```java
MessageUtil.isTrimmedEmpty("");        // true
MessageUtil.isTrimmedEmpty("   ");     // true
MessageUtil.isTrimmedEmpty("\t \t");   // true
MessageUtil.isTrimmedEmpty("\u00A0");  // true

// a string containing only a newline is NOT trimmed empty
MessageUtil.isTrimmedEmpty("\n");      // false

MessageUtil.isTrimmedEmpty("hello");   // false
```

### `isEmpty(String)`

This is a straightforward null-safe check. It returns `true` if the string is `null` or has zero length. Unlike 
`isTrimmedEmpty`, it does not examine the string's content, so a string consisting of nothing but spaces returns 
`false`.

```java
MessageUtil.isEmpty(null);   // true
MessageUtil.isEmpty("");     // true
MessageUtil.isEmpty("   ");  // false
MessageUtil.isEmpty("a");    // false
```


## `MessageUtil` Name Validation Methods

The library enforces naming conventions for various identifiers such as formatter names, parameter names and template
names. `MessageUtil` provides a set of static validation methods that check whether a string conforms to a particular
naming style. These methods operate on Unicode code points, so they correctly handle characters outside the Basic
Multilingual Plane.

### `validateName(String, String)`

This is a guard method that throws an exception if the given name is `null` or blank. The second argument is a
descriptive label used in the exception message to indicate which name failed validation. When the check passes, the
original name is returned, making it convenient for inline validation in constructors or setters.

```java
// valid name passes through
String name = MessageUtil
        .validateName("myFormatter", "formatter name");
// name → "myFormatter"

// null throws NullPointerException with message 
// "formatter name must not be null"
MessageUtil.validateName(null, "formatter name");

// blank throws IllegalArgumentException with message 
// "template name must not be empty"
MessageUtil.validateName("   ", "template name");
```

### `isKebabCaseName(String)`

This method checks whether a string follows kebab-case conventions. A valid kebab-case name starts with a lowercase
letter, contains only lowercase letters, digits and hyphens, does not end with a hyphen and does not contain
consecutive hyphens.

```java
MessageUtil.isKebabCaseName("date-format");     // true
MessageUtil.isKebabCaseName("my-formatter-2");  // true
MessageUtil.isKebabCaseName("x");               // true

// must start with a lowercase letter
MessageUtil.isKebabCaseName("2things");         // false
MessageUtil.isKebabCaseName("MyName");          // false

// no trailing hyphen
MessageUtil.isKebabCaseName("trailing-");       // false

// no consecutive hyphens
MessageUtil.isKebabCaseName("double--dash");    // false

// uppercase letters are not allowed
MessageUtil.isKebabCaseName("camelCase");       // false
```

### `isLowerCamelCaseName(String)`

This method checks whether a string follows lower camelCase conventions. A valid lower camelCase name starts with a
lowercase letter and contains only letters and digits. Unlike kebab-case, hyphens and underscores are not permitted.
Uppercase letters are allowed after the first character to form the camelCase humps.

```java
MessageUtil.isLowerCamelCaseName("dateFormat");    // true
MessageUtil.isLowerCamelCaseName("myFormatter2");  // true
MessageUtil.isLowerCamelCaseName("x");             // true

// must start with a lowercase letter
MessageUtil.isLowerCamelCaseName("DateFormat");    // false
MessageUtil.isLowerCamelCaseName("3items");        // false

// hyphens and underscores are not allowed
MessageUtil.isLowerCamelCaseName("date-format");   // false
MessageUtil.isLowerCamelCaseName("date_format");   // false
```

### `isKebabOrLowerCamelCaseName(String)`

This method combines the two checks above into a single-pass validation. It accepts a name that is either valid
kebab-case or valid lower camelCase, but it rejects names that mix the two styles. A name containing both uppercase
letters and hyphens fails the check because such a name belongs to neither convention.

```java
MessageUtil.isKebabOrLowerCamelCaseName("date-format");   // true (kebab)
MessageUtil.isKebabOrLowerCamelCaseName("dateFormat");    // true (camelCase)

// mixing styles is rejected
MessageUtil.isKebabOrLowerCamelCaseName("date-Format");   // false
MessageUtil.isKebabOrLowerCamelCaseName("dateFormat-x");  // false
```

This method is useful when an API accepts identifiers in either convention and needs to validate the input without
caring which style was chosen, as long as the styles are not mixed.

### `isName(String)`

This method validates a name against the rules defined by the message format lexer grammar. A valid name starts with
a Unicode letter (`\p{L}`), followed by zero or more Unicode letters or numbers (`\p{L}` or `\p{N}`). After this
initial segment, zero or more groups may follow, where each group consists of a single underscore or hyphen followed
by one or more Unicode letters or numbers. The name must not end with a separator character and consecutive separators
are not allowed.

```java
MessageUtil.isName("hello");        // true
MessageUtil.isName("myParam");      // true
MessageUtil.isName("date-format");  // true
MessageUtil.isName("item_count");   // true
MessageUtil.isName("größe");        // true (Unicode letters allowed)
MessageUtil.isName("abc123");       // true
MessageUtil.isName("a-b_c");        // true

// must start with a letter
MessageUtil.isName("123abc");       // false
MessageUtil.isName("_hidden");      // false
MessageUtil.isName("-start");       // false

// must not end with a separator
MessageUtil.isName("trailing-");    // false
MessageUtil.isName("trailing_");    // false

// consecutive separators are not allowed
MessageUtil.isName("double--sep");  // false
MessageUtil.isName("double__sep");  // false

// empty string is not a valid name
MessageUtil.isName("");             // false
```


## `MessageUtil` Serialization Methods

The message format engine can convert its internal message representation back into a format string. This process is
called serialization and is driven by a `FormatStringSerializer.Context`. The context carries a `CharsetEncoder` to
determine which characters can be represented directly, a `TextJoiner` that accumulates the serialized output and an
optional quote character that indicates whether serialization is currently inside a quoted string.

`MessageUtil` provides three static methods that handle the most common serialization tasks: writing a raw string with
proper escaping, wrapping a string in quotes and serializing a full `Message` object.

### `serializeString(Context, String)`

This method appends a string character by character to the context's text joiner, applying the following escaping
rules. If the context has an active quote character (because serialization is happening inside a quoted string), every
occurrence of that quote character is backslash-escaped. A `%` character followed by `{`, `[` or `(` is
backslash-escaped to prevent the parser from interpreting it as a parameter reference. ISO control characters and
characters that cannot be encoded in the context's charset are written as Unicode escape sequences in the form
`\u0000`.

```java
var context = 
    new FormatStringSerializer.Context(StandardCharsets.UTF_8);

// serializing a plain string without a quote context
MessageUtil.serializeString(context, "hello world");
// text joiner contains: hello world

// with an active single-quote context, single quotes are escaped
var quoted = context.withStringQuote('\'');
MessageUtil.serializeString(quoted, "it's a test");
// text joiner contains: it\'s a test

// percent followed by { is escaped to avoid parameter interpretation
MessageUtil.serializeString(context, "100%{done}");
// text joiner contains: 100\%{done}

// control characters are written as unicode escapes
MessageUtil.serializeString(context, "line\u0000end");
// text joiner contains: line\u0000end
```

### `serializeQuotedString(Context, String)`

This method serializes a string wrapped in quotes. It automatically selects the quote character that minimizes the
number of escape sequences in the output. If the string contains more single quotes than double quotes, a double
quote is used as the wrapper; otherwise a single quote is chosen. The opening quote, the escaped content and the
closing quote are all appended to the context's text joiner.

```java
var context = 
    new FormatStringSerializer.Context(StandardCharsets.UTF_8);

// string with no quotes uses single quotes by default
MessageUtil.serializeQuotedString(context, "hello");
// text joiner contains: 'hello'

// string with more single quotes switches to double quotes
MessageUtil.serializeQuotedString(context, "it's five o'clock");
// text joiner contains: "it's five o'clock"

// string with more double quotes uses single quotes
MessageUtil.serializeQuotedString(context, "say \"hi\" now");
// text joiner contains: 'say "hi" now'
```

### `serializeMessage(Context, Message, boolean)`

This method serializes an entire `Message` object. The `forceQuoted` parameter controls whether the message is always
wrapped in quotes. When `forceQuoted` is `false` and the message is a simple `TextMessage` whose text is a valid name
(as defined by `isName`), the text is serialized directly without quotes. In all other cases, the message is wrapped
in quotes. The quote character is chosen by examining all text parts in the message for single quotes; if any text
part contains a single quote, double quotes are used as the wrapper.

```java
var context = 
    new FormatStringSerializer.Context(StandardCharsets.UTF_8);

// a simple text message that is a valid name can be serialized unquoted
Message simpleMsg = ...;  // TextMessage containing "hello"
MessageUtil.serializeMessage(context, simpleMsg, false);
// text joiner contains: hello

// forcing quotes on a simple name
MessageUtil.serializeMessage(context, simpleMsg, true);
// text joiner contains: 'hello'

// a message containing spaces is always quoted regardless of forceQuoted
Message spacedMsg = ...;  // TextMessage containing "hello world"
MessageUtil.serializeMessage(context, spacedMsg, false);
// text joiner contains: 'hello world'
```


## `MessageUtil` Pack File Methods

The library supports a binary pack format for storing pre-compiled messages and templates. The pack format allows
applications to ship pre-parsed message bundles that can be loaded without re-parsing the format strings at runtime.
`MessageUtil` offers two static methods to detect and import pack files.

### `isMessageFormatPack(Path)`

This method probes a file to determine whether it is a message format pack file. It bypasses the standard
`Files.probeContentType()` SPI mechanism and directly inspects the file content. This is particularly useful in
environments where the `PackFileTypeDetector` service provider is not active due to classloader isolation, such as
IntelliJ IDEA plugins or Gradle build scripts.

```java
Path packFile = Path.of("messages.pack");
Path textFile = Path.of("messages.properties");

MessageUtil.isMessageFormatPack(packFile);  // true (if it is a valid pack file)
MessageUtil.isMessageFormatPack(textFile);  // false
```

The method returns `false` for any file that does not exist, cannot be read or does not contain a valid pack file
header. It never throws an exception.

### `importMessages(InputStream, Consumer, BiConsumer)`

This method reads a pack input stream and delivers each message and template it contains to the provided consumers.
The first consumer receives `Message.WithCode` instances (messages with their associated code). The second consumer
receives templates as name-template pairs. Either consumer may be `null` if only one type of entry is needed.

The input stream is always closed when this method returns, regardless of success or failure.

```java
try(var stream = Files.newInputStream(Path.of("messages.pack"))) {
  MessageUtil.importMessages(
      stream,
      message -> messageSupport.addMessage(message),
      (name, template) -> messageSupport.addTemplate(name, template));
}

// importing only messages, ignoring templates
try(var stream = getClass().getResourceAsStream("/bundle.pack")) {
  MessageUtil.importMessages(stream, 
      message -> registry.put(message.getCode(), message), null);
}
```

An `IOException` is thrown if the stream is unreadable or the content does not conform to the pack format. An
`IllegalArgumentException` is thrown if the pack stream lacks version information.


## `MessageUtil` Enum Utility

### `findEnumValue(String, Class)`

This method performs a case-insensitive lookup of an enum constant by name. It also supports the common convention of
using hyphens in configuration values where Java enum constants use underscores. A value like `"my-value"` matches an
enum constant named `MY_VALUE` because the method replaces underscores in the enum constant name with hyphens before
comparing.

The method returns an `Optional` containing the matching constant, or an empty `Optional` if no match is found.

```java
enum Alignment { LEFT, CENTER, RIGHT, JUSTIFY_ALL }

MessageUtil.findEnumValue("left", Alignment.class);
// Optional[LEFT]

// case-insensitive matching
MessageUtil.findEnumValue("Center", Alignment.class);
// Optional[CENTER]

MessageUtil.findEnumValue("RIGHT", Alignment.class);
// Optional[RIGHT]

// hyphenated value matches underscored enum constant
MessageUtil.findEnumValue("justify-all", Alignment.class);
// Optional[JUSTIFY_ALL]

// no match returns empty
MessageUtil.findEnumValue("unknown", Alignment.class);
// Optional.empty()
```

This method is especially useful in formatters that accept configuration parameters as strings and need to resolve
them to internal enum values without forcing callers to know the exact casing or separator convention.


## `TextPartFactory`

When a formatter produces its output, it returns a `Text` object. The `Text` interface represents a piece of text 
together with optional leading and trailing space flags. These flags control how the text is joined with its neighbors 
when the message is assembled. Building `Text` instances by hand requires careful handling of null values, empty strings 
and space tracking. `TextPartFactory` provides a set of static factory methods that take care of these details.

### `nullText()` and `emptyText()`

These methods return singleton instances for the two most common edge cases. `nullText()` produces a `Text` whose
`getText()` returns `null`, which signals to the formatting engine that the formatter produced no output. `emptyText()` 
produces a `Text` whose `getText()` returns an empty string, signaling that the formatter explicitly produced an empty 
result.

```java
import static de.sayayi.lib.message.part.TextPartFactory.*;

// when the formatter has nothing to produce
Text result = nullText();
result.getText();  // null

// when the formatter explicitly returns empty output
Text result = emptyText();
result.getText();  // ""
```

### `noSpaceText(String)`

This method creates a `Text` from a string with no leading or trailing space. The input string is trimmed using 
`MessageUtil.trimSpaces`. If the input is `null`, the result is equivalent to `nullText()`. If the trimmed input is 
empty (as determined by `MessageUtil.isTrimmedEmpty`), the result is equivalent to `emptyText()`. This is the method 
used most often when returning formatted output that should be placed exactly where the parameter reference appears, 
without adding any surrounding whitespace.

```java
Text t = noSpaceText("hello");
t.getText();        // "hello"
t.isSpaceBefore();  // false
t.isSpaceAfter();   // false

// leading and trailing spaces are trimmed
Text t2 = noSpaceText("  hello  ");
t2.getText();       // "hello"

// null input yields the null-text singleton
Text t3 = noSpaceText(null);
t3.getText();       // null
```

### `spacedText(String)`

This method creates a `Text` from a string while preserving any leading and trailing spaces in the input. The text
content and its space flags are derived directly from the raw string. If the input is `null`, the result is
`nullText()`. If the input is an empty string, the result is `emptyText()`. Use this when the space context of the 
input string is meaningful and must be carried through to the assembled message.

```java
Text t = spacedText(" hello ");
t.getText();        // "hello"
t.isSpaceBefore();  // true
t.isSpaceAfter();   // true

// no surrounding spaces means no space flags
Text t2 = spacedText("hello");
t2.isSpaceBefore();  // false
t2.isSpaceAfter();   // false
```

### `addSpaces(Text, boolean, boolean)`

This method adds space flags to an existing `Text`. The first boolean controls whether a leading space should be added
and the second controls the trailing space. If the `Text` already has the requested space flag set, no change is made 
for that side. When neither flag would change, the original `Text` instance is returned as-is. This is useful when a 
formatter needs to inject spacing around an intermediate result that was produced by another method or formatter.

```java
Text original = noSpaceText("hello");
Text spaced = addSpaces(original, true, false);

spaced.getText();        // "hello"
spaced.isSpaceBefore();  // true
spaced.isSpaceAfter();   // false

// adding a trailing space to text that already has one is a no-op
Text alreadySpaced = spacedText(" hello ");
Text same = addSpaces(alreadySpaced, false, true);
// 'same' has the same space flags as 'alreadySpaced'
```


## `TextJoiner`

Assembling multiple text fragments into a single `Text` is a common task inside formatters. When formatting a 
collection of values, for example, the formatter iterates the elements, formats each one and joins the results with a 
separator. Handling the spaces between fragments manually is error-prone: adjacent spaces must be collapsed, trailing 
space state must be tracked across iterations, and a leading or trailing separator space must be avoided.

`TextJoiner` solves all of this. It accumulates `Text` parts, strings and individual characters into a single buffer. 
Whenever it encounters a space character, it does not append it immediately. Instead, it records the space as pending. 
The space is emitted only when actual non-space content follows and adjacent pending spaces are collapsed into one 
separator space.

### Creating and Adding Content

A new `TextJoiner` starts with an empty buffer and no pending space. Content is added through several methods, each 
accepting a different input type. All `add` methods return the joiner itself, so calls can be chained.

`add(Text)` appends a `Text` instance. The leading and trailing space flags of the `Text` are honored: a leading space 
flag causes a separator space before the content and a trailing space flag is recorded as pending for the next addition.

`add(char)` appends a single character. If the character is a space character (as defined by `MessageUtil.isSpaceChar`),
it is recorded as pending rather than appended immediately. A non-space character is appended to the buffer, preceded 
by a separator space if one was pending.

`add(char[])` processes each character in the array sequentially, applying the same space-handling logic as `add(char)`.
Runs of space characters are collapsed into a single pending separator space.

`add(String)` appends a string, preserving any leading and trailing spaces in the string. Internally, it converts the 
string to a character array and processes it character by character, meaning consecutive spaces within the string are 
also collapsed. A `null` or empty string is ignored.

`addNoSpace(String)` and `addNoSpace(Text)` append content with leading and trailing spaces stripped.

```java
TextJoiner joiner = new TextJoiner();

joiner.add(noSpaceText("Hello"))
      .add(spacedText(", "))
      .add(noSpaceText("world"));

Text result = joiner.asNoSpaceText();
// result.getText() → "Hello, world"
```

### Retrieving the Result

The joiner offers two ways to retrieve the assembled text.

`asSpacedText()` returns a `Text` that preserves both a leading and a trailing space. If the first content added was 
preceded by a leading space flag, the resulting `Text` will have its `isSpaceBefore()` flag set. Likewise, if the last 
content added was followed by a space (either through a `Text` with a trailing space flag or through a space-only 
addition), the resulting `Text` will have its `isSpaceAfter()` flag set.

`asNoSpaceText()` returns a `Text` with no leading or trailing space, discarding any pending trailing space.

```java
TextJoiner joiner = new TextJoiner();

joiner.add(" red ")
      .add((String)null)  // null is ignored
      .add(" ")           // only records a pending space
      .add("green ");

// asNoSpaceText() strips the surrounding spaces
Text noSpace = joiner.asNoSpaceText();
// noSpace.getText() → "red green"
// noSpace.isSpaceBefore() → false
// noSpace.isSpaceAfter() → false

// asSpacedText() preserves the leading and trailing space state
Text spaced = joiner.asSpacedText();
// spaced.getText() → "red green"
// spaced.isSpaceBefore() → true
// spaced.isSpaceAfter() → true
```

### Practical Example: Joining Collection Elements

The following example shows how a formatter might use `TextJoiner` to render the elements of a collection as a 
comma-separated list. Each element is formatted individually and the joiner takes care of the spacing between elements.

```java
TextJoiner joiner = new TextJoiner();

for(String element: List.of("apple", "banana", "cherry"))
{
  if (!joiner.asNoSpaceText().isEmpty())
    joiner.add(spacedText(", "));

  joiner.add(noSpaceText(element));
}

Text result = joiner.asNoSpaceText();
// result.getText() → "apple, banana, cherry"
```


## `SingletonParameters`

The `Parameters` interface provides the locale and parameter values that the formatting engine passes to formatters.
Formatting a sub-message inside a formatter requires a `Parameters` instance to pass along. Creating a full 
implementation each time is verbose. `SingletonParameters` is a lightweight implementation that holds exactly one named 
parameter. It is perfect for formatters that iterate over a collection and format each element using a sub-message that 
references a single parameter.

The constructor takes a `Locale` and the parameter name. The initial value is `null`. The value is set through 
`setValue(Object)`, which returns the `SingletonParameters` instance itself, allowing it to be used inline in a 
`format` call.

```java
SingletonParameters params = 
    new SingletonParameters(Locale.US, "item");

// format individual elements by updating the value in place
for(Object element: collection) 
{
  Text formatted = message.format(accessor, 
      params.setValue(element));
  joiner.add(formatted);
}
```

Requesting the value of any parameter name other than the one provided at construction time returns `null`. The
`getParameterNames()` method returns an immutable singleton set containing only the configured name.

```java
SingletonParameters params = 
    new SingletonParameters(Locale.GERMAN, "name");
params.setValue("Berlin");

params.getParameterValue("name");   // "Berlin"
params.getParameterValue("other");  // null
params.getParameterNames();         // Set["name"]
params.getLocale();                 // Locale.GERMAN
```

Because `SingletonParameters` is mutable through `setValue`, a single instance can be reused across many formatting 
calls without allocating a new `Parameters` object for each iteration. This keeps memory overhead low when formatting 
large collections.


## `SupplierDelegate`

Some formatters need to compute an expensive intermediate value that may or may not be used depending on the map 
entries in the message. Computing the value eagerly wastes resources when it turns out to be unnecessary. The standard 
`Supplier` interface from `java.util.function` solves the lazy-evaluation part, but it does not prevent the computation 
from running multiple times if the supplier is called more than once.

`SupplierDelegate` wraps another `Supplier` and caches the result of the first invocation. Every subsequent call to 
`get()` returns the cached value without invoking the delegate again. After the first call, the reference to the 
original supplier is released, allowing it to be garbage-collected.

A `SupplierDelegate` is created through its static factory method `of(Supplier)`:

```java
Supplier<BigDecimal> expensiveValue = SupplierDelegate.of(() -> {
  // costly computation that should run at most once
  return computeNormalizedAmount(rawValue);
});

// first call invokes the delegate and caches the result
BigDecimal val1 = expensiveValue.get();

// second call returns the cached value; the delegate is not invoked again
BigDecimal val2 = expensiveValue.get();
// val1 == val2
```

`SupplierDelegate` is thread-safe. After the first invocation, the delegate supplier is released for garbage collection.

A typical use case inside a formatter is to defer an expensive computation until it is actually needed. The 
`IterableFormatter` in this library illustrates this pattern well. When formatting a collection, a self-reference 
placeholder (e.g. `"(this collection)"`) is only required if one of the elements in the collection turns out to be the 
collection itself. Rather than computing that text eagerly for every formatting call, the formatter wraps it in a 
`SupplierDelegate`:

```java
@Override
public @NotNull Text format(
    @NotNull ParameterFormatterContext context, Object value)
{
  List<?> list = (List<?>)value;

  // deferred: only computed if the list contains a self-reference
  Supplier<Text> thisText = SupplierDelegate.of(() -> noSpaceText(
      context.getConfigValueString("this").orElse("(this list)")));

  SingletonParameters params = 
      new SingletonParameters(context.getLocale(), "item");
  TextJoiner joiner = new TextJoiner();

  for(Object element: list)
  {
    Text formatted = (element == list)
        ? thisText.get()  // triggers computation only on first self-reference
        : itemMessage.formatAsText(context.getMessageAccessor(), 
              params.setValue(element));

    joiner.add(formatted);
  }

  return joiner.asNoSpaceText();
  // For input ["hello", <self-ref>, "world"]:
  // result → "hello, (this list), world"
}
```

In this example, if no element in the list is a self-reference, the config value lookup and `Text` construction never 
execute. When a self-reference does occur, the first call to `thisText.get()` computes and caches the result. Any 
subsequent self-references in the same list reuse the cached value without repeating the lookup.
