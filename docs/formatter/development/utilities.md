# Formatter Utilities

The core module includes several helper classes that solve problems you will encounter repeatedly
when building formatters. Instead of reimplementing space handling, text assembly, lazy
initialization or lightweight parameter containers, you can rely on these ready-made utilities.
They are the same building blocks that the library's own formatters use internally.


## `MessageUtil` Space and String Methods

The library uses its own definition of what constitutes a "space character" because the standard
`Character.isSpaceChar(char)` and `String.trim()` treat newlines as whitespace characters. In the
message format engine, newlines are significant and must be preserved. The `MessageUtil` class
provides a set of static methods that operate under this custom definition.

### `isSpaceChar(char)`

This method returns `true` if the given character is a tab (`\t`), a carriage return (`\r`), a
Unicode `SPACE_SEPARATOR` (such as the regular space U+0020 or the non-breaking space U+00A0),
or a Unicode `PARAGRAPH_SEPARATOR`. Notably, the newline character (`\n`) is **not** considered
a space character. This distinction is critical because message templates can contain intentional
line breaks that must survive trimming and normalization.

```java
// regular space and tab are recognized as space characters
MessageUtil.isSpaceChar(' ');     // true
MessageUtil.isSpaceChar('\t');    // true

// non-breaking space (U+00A0) is a SPACE_SEPARATOR
MessageUtil.isSpaceChar('\u00A0'); // true

// newline is NOT a space character
MessageUtil.isSpaceChar('\n');    // false
```

### `trimSpaces(String)`

This method strips leading and trailing space characters from a string, where "space character"
follows the definition of `isSpaceChar`. Unlike `String.trim()`, it does not remove newlines.
If the input is `null`, the method returns `null`. If the string contains only space characters,
the result is an empty string. When the input already has no leading or trailing spaces, the
original `String` instance is returned, avoiding a needless allocation.

```java
MessageUtil.trimSpaces("  hello  ");          // "hello"
MessageUtil.trimSpaces("\t  hello\t ");        // "hello"
MessageUtil.trimSpaces(null);                  // null

// newlines at the edges are preserved
MessageUtil.trimSpaces("\nhello\n");           // "\nhello\n"

// mixed: tabs are trimmed, newlines stay
MessageUtil.trimSpaces("\t\nhello\n\t");       // "\nhello\n"
```

### `trimAndNormalizeSpaces(String)`

This method goes a step further than `trimSpaces`. It trims leading and trailing spaces, collapses
consecutive internal space characters into a single ASCII space (U+0020), and replaces any remaining
Unicode space character with a regular ASCII space. Newlines are left untouched throughout this
process. The method returns `null` when the input is `null` and an empty string when the input
contains only space characters.

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

This method is especially useful when a formatter receives user-supplied text that may contain a
mix of Unicode whitespace characters and you want to produce clean, consistently spaced output
without destroying intentional line breaks.

### `isTrimmedEmpty(String)`

This method returns `true` if the given string has zero length or consists entirely of space
characters as defined by `isSpaceChar`. It does not accept `null`. Use this when you need to
tell apart a string that is visually empty from one that contains meaningful content.

```java
MessageUtil.isTrimmedEmpty("");          // true
MessageUtil.isTrimmedEmpty("   ");       // true
MessageUtil.isTrimmedEmpty("\t \t");     // true
MessageUtil.isTrimmedEmpty("\u00A0");    // true

// a string containing only a newline is NOT trimmed empty
MessageUtil.isTrimmedEmpty("\n");        // false

MessageUtil.isTrimmedEmpty("hello");    // false
```

### `isEmpty(String)`

This is a straightforward null-safe check. It returns `true` if the string is `null` or has zero
length. Unlike `isTrimmedEmpty`, it does not examine the string's content, so a string consisting
of nothing but spaces returns `false`.

```java
MessageUtil.isEmpty(null);   // true
MessageUtil.isEmpty("");     // true
MessageUtil.isEmpty("   ");  // false
MessageUtil.isEmpty("a");    // false
```


## `TextPartFactory`

When a formatter produces its output, it returns a `Text` object. The `Text` interface represents a
piece of text together with optional leading and trailing space flags. These flags control how the
text is joined with its neighbors when the message is assembled. Building `Text` instances by hand
requires you to worry about null handling, empty strings and space tracking. `TextPartFactory`
provides a set of static factory methods that take care of these details.

### `nullText()` and `emptyText()`

These methods return singleton instances for the two most common edge cases. `nullText()` produces
a `Text` whose `getText()` returns `null`, which signals to the formatting engine that the
formatter produced no output. `emptyText()` produces a `Text` whose `getText()` returns an empty
string, signaling that the formatter explicitly produced an empty result.

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

This method creates a `Text` from a string with no leading or trailing space. The input string is
trimmed using `MessageUtil.trimSpaces`. If the input is `null`, the result is equivalent to
`nullText()`. If the trimmed input is empty (as determined by `MessageUtil.isTrimmedEmpty`), the
result is equivalent to `emptyText()`. This is the method you will use most often when returning
formatted output that should be placed exactly where the parameter reference appears, without
adding any surrounding whitespace.

```java
Text t = noSpaceText("hello");
t.getText();         // "hello"
t.isSpaceBefore();   // false
t.isSpaceAfter();    // false

// leading and trailing spaces are trimmed
Text t2 = noSpaceText("  hello  ");
t2.getText();        // "hello"

// null input yields the null-text singleton
Text t3 = noSpaceText(null);
t3.getText();        // null
```

### `spacedText(String)`

This method creates a `Text` from a string while preserving any leading and trailing spaces in the
input. The text content and its space flags are derived directly from the raw string. If the input
is `null`, the result is `nullText()`. If the input is an empty string, the result is `emptyText()`.
Use this when the space context of the input string is meaningful and must be carried through to the
assembled message.

```java
Text t = spacedText(" hello ");
t.getText();         // "hello"
t.isSpaceBefore();   // true
t.isSpaceAfter();    // true

// no surrounding spaces means no space flags
Text t2 = spacedText("hello");
t2.isSpaceBefore();  // false
t2.isSpaceAfter();   // false
```

### `addSpaces(Text, boolean, boolean)`

This method adds space flags to an existing `Text`. The first boolean controls whether a leading
space should be added, and the second controls the trailing space. If the `Text` already has the
requested space flag set, no change is made for that side. When neither flag would change, the
original `Text` instance is returned as-is. This is useful when a formatter needs to inject spacing
around an intermediate result that was produced by another method or formatter.

```java
Text original = noSpaceText("hello");
Text spaced = addSpaces(original, true, false);

spaced.getText();         // "hello"
spaced.isSpaceBefore();   // true
spaced.isSpaceAfter();    // false

// adding a trailing space to text that already has one is a no-op
Text alreadySpaced = spacedText(" hello ");
Text same = addSpaces(alreadySpaced, false, true);
// 'same' has the same space flags as 'alreadySpaced'
```


## `TextJoiner`

Assembling multiple text fragments into a single `Text` is a common task inside formatters. When
formatting a collection of values, for example, the formatter iterates the elements, formats each
one and joins the results with a separator. Handling the spaces between fragments manually is
error-prone: you need to collapse adjacent spaces, track trailing space state across iterations,
and avoid emitting a leading or trailing separator space.

`TextJoiner` solves all of this. It accumulates `Text` parts, strings and individual characters
into a single buffer. Whenever it encounters a space character, it does not append it immediately.
Instead, it records the space as pending. The space is emitted only when actual non-space content
follows, and adjacent pending spaces are collapsed into one separator space.

### Creating and Adding Content

A new `TextJoiner` starts with an empty buffer and no pending space. You add content through
several methods, each accepting a different input type. All `add` methods return the joiner itself,
so calls can be chained.

`add(Text)` appends a `Text` instance. The leading and trailing space flags of the `Text` are
honored: a leading space flag causes a separator space before the content, and a trailing space
flag is recorded as pending for the next addition.

`add(char)` appends a single character. If the character is a space character (as defined by
`MessageUtil.isSpaceChar`), it is recorded as pending rather than appended immediately. A non-space
character is appended to the buffer, preceded by a separator space if one was pending.

`addWithSpace(String)` appends a string, preserving any leading and trailing spaces in the string.
Internally, it delegates to `spacedText` from `TextPartFactory`.

`addNoSpace(String)` and `addNoSpace(Text)` append content with leading and trailing spaces
stripped.

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

`asSpacedText()` returns a `Text` that preserves both a leading and a trailing space. If the first
content added was preceded by a leading space flag, the resulting `Text` will have its
`isSpaceBefore()` flag set. Likewise, if the last content added was followed by a space (either
through a `Text` with a trailing space flag or through a space-only addition), the resulting `Text`
will have its `isSpaceAfter()` flag set.

`asNoSpaceText()` returns a `Text` with no leading or trailing space, discarding any pending
trailing space.

```java
TextJoiner joiner = new TextJoiner();

joiner.addWithSpace(" red ")
      .addWithSpace(null)       // null is ignored
      .addWithSpace(" ")        // only records a pending space
      .addWithSpace("green ");

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

The following example shows how a formatter might use `TextJoiner` to render the elements of a
collection as a comma-separated list. Each element is formatted individually, and the joiner
takes care of the spacing between elements.

```java
TextJoiner joiner = new TextJoiner();

for(String element : List.of("apple", "banana", "cherry"))
{
  if (!joiner.asNoSpaceText().isEmpty())
    joiner.add(spacedText(", "));

  joiner.add(noSpaceText(element));
}

Text result = joiner.asNoSpaceText();
// result.getText() → "apple, banana, cherry"
```


## `SingletonParameters`

The `Parameters` interface provides the locale and parameter values that the formatting engine
passes to formatters. When you need to format a sub-message inside a formatter, you need a
`Parameters` instance to pass along. Creating a full implementation each time is verbose.
`SingletonParameters` is a lightweight implementation that holds exactly one named parameter. It
is perfect for formatters that iterate over a collection and format each element using a
sub-message that references a single parameter.

The constructor takes a `Locale` and the parameter name. The initial value is `null`. You set
the value through `setValue(Object)`, which returns the `SingletonParameters` instance itself,
allowing it to be used inline in a `format` call.

```java
SingletonParameters params = new SingletonParameters(Locale.US, "item");

// format individual elements by updating the value in place
for(Object element : collection) 
{
  Text formatted = message.format(accessor, params.setValue(element));
  joiner.add(formatted);
}
```

Requesting the value of any parameter name other than the one provided at construction time
returns `null`. The `getParameterNames()` method returns an immutable singleton set containing
only the configured name.

```java
SingletonParameters params = new SingletonParameters(Locale.GERMAN, "name");
params.setValue("Berlin");

params.getParameterValue("name");   // "Berlin"
params.getParameterValue("other");  // null
params.getParameterNames();         // Set["name"]
params.getLocale();                 // Locale.GERMAN
```

Because `SingletonParameters` is mutable through `setValue`, you can reuse a single instance
across many formatting calls without allocating a new `Parameters` object for each iteration.
This keeps memory overhead low when formatting large collections.


## `SupplierDelegate`

Some formatters need to compute an expensive intermediate value that may or may not be used
depending on the map entries in the message. Computing the value eagerly wastes resources when it
turns out to be unnecessary. The standard `Supplier` interface from `java.util.function` solves
the lazy-evaluation part, but it does not prevent the computation from running multiple times if
the supplier is called more than once.

`SupplierDelegate` wraps another `Supplier` and caches the result of the first invocation. Every
subsequent call to `get()` returns the cached value without invoking the delegate again. After the
first call, the reference to the original supplier is released, allowing it to be garbage-collected.

You create a `SupplierDelegate` through its static factory method `of(Supplier)`:

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

`SupplierDelegate` is thread-safe. After the first invocation, the delegate supplier is released
for garbage collection.

A typical use case inside a formatter is to defer a type conversion until a map comparison method
actually needs it:

```java
@Override
public @NotNull Text format(
    @NotNull ParameterFormatter.Context context,
    @NotNull Object value)
{
  Supplier<String> normalizedName = SupplierDelegate.of(
      () -> normalizePersonName((Person) value));

  // The supplier is only invoked if the map contains a string key
  // that triggers a comparison against the normalized name.
  context.delegateToNextFormatter();
}
```
