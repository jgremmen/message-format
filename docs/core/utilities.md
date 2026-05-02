# Utilities

The `de.sayayi.lib.message.util` package provides helper classes that are used internally by the
library but are also available for application code. The two public classes in this package are
`MessageUtil`, a collection of static methods for pack file handling, name validation and space
processing, and `SupplierDelegate`, a compute-once `Supplier` wrapper.


## MessageUtil

`MessageUtil` groups static utility methods into three areas: pack file operations, name
validation and space handling. All methods are stateless and thread-safe.


### Pack File Detection

In a standard Java environment the library registers a `FileTypeDetector` through the service
provider mechanism, which allows `Files.probeContentType()` to recognize message format pack
files. However, some environments do not support this SPI. Gradle plugins and IDE integrations,
for example, use isolated classloaders that prevent the detector from being discovered. In those
situations, `isMessageFormatPack` provides a direct alternative. It reads the file header and
returns `true` if the content matches the pack file format.

```java
Path candidate = Path.of("build/messages.mfp");

if (MessageUtil.isMessageFormatPack(candidate)) {
    // proceed with importing
}
```

The method never throws. If the file does not exist, cannot be read, or has invalid content, it
returns `false`.


### Importing Messages from a Pack Stream

The `importMessages` method reads a pack file from an `InputStream` and invokes callbacks for
each message and template it encounters. This gives you full control over how the imported entries
are processed instead of adding them to a `MessageSupport` directly.

Two functional parameters control the processing. The first is a `Consumer<Message.WithCode>`
that is called for every message. The second is a `BiConsumer<String, Message.WithSpaces>` that
is called for every template, receiving the template name and the parsed template. Either
parameter can be `null` if you are only interested in one type of entry.

```java
try(var in = new FileInputStream("messages.mfp")) {
  MessageUtil.importMessages(in,
      message -> System.out.println("Message: " + message.getCode()),
      (name, template) -> System.out.println("Template: " + name));
}
```

The stream is always closed when the method returns, regardless of whether the import succeeded
or failed. If the stream does not contain valid pack data, an `IOException` is thrown.

A common use case is counting the entries in a pack file without fully processing them:

```java
var messageCount = new int[1];
var templateCount = new int[1];

try(var in = new FileInputStream("messages.mfp")) {
  MessageUtil.importMessages(in,
      message -> messageCount[0]++,
      (name, template) -> templateCount[0]++);
}

System.out.println(messageCount[0] + " messages, " + templateCount[0] + " templates");
```

If all you need is to load a pack file into a `ConfigurableMessageSupport`, the convenience
method `ConfigurableMessageSupport.importMessages(InputStream)` is a simpler alternative. It
delegates to `MessageUtil.importMessages` internally and adds every entry to the message support.


### Name Validation

The library uses several naming conventions for identifiers in message format strings.
Configuration keys, formatter names, template names and post formatter names must be kebab-case.
Parameter names may be either kebab-case or lower camelCase. The `Name` token in the message
format grammar accepts a broader set of characters: any string that starts with a Unicode letter,
continues with letters or digits, and optionally contains groups separated by a single underscore
or hyphen.

`MessageUtil` exposes four methods that check these conventions.

`isKebabCaseName` accepts names composed of lowercase letters and digits separated by single
hyphens. The name must start with a lowercase letter and must not end with a hyphen.

```java
MessageUtil.isKebabCaseName("clip-suffix");    // true
MessageUtil.isKebabCaseName("list-sep-last");  // true
MessageUtil.isKebabCaseName("clipSuffix");     // false (contains uppercase)
MessageUtil.isKebabCaseName("-invalid");       // false (starts with hyphen)
MessageUtil.isKebabCaseName("no--double");     // false (consecutive hyphens)
```

`isLowerCamelCaseName` accepts names that start with a lowercase letter and contain only letters
and digits. No hyphens or underscores are allowed.

```java
MessageUtil.isLowerCamelCaseName("firstName");   // true
MessageUtil.isLowerCamelCaseName("count");       // true
MessageUtil.isLowerCamelCaseName("first-name");  // false (contains hyphen)
MessageUtil.isLowerCamelCaseName("FirstName");   // false (starts with uppercase)
```

`isKebabOrLowerCamelCaseName` accepts either convention but not a mix. A name that contains both
hyphens and uppercase letters is rejected because it does not belong to either convention.

```java
MessageUtil.isKebabOrLowerCamelCaseName("clip-suffix");  // true
MessageUtil.isKebabOrLowerCamelCaseName("clipSuffix");   // true
MessageUtil.isKebabOrLowerCamelCaseName("clip-Suffix");  // false (mixed conventions)
```

`isName` checks whether a string matches the general `Name` token from the message format
grammar. This is the broadest check: any Unicode letter can start the name, followed by Unicode
letters or numbers, with optional underscore or hyphen separators.

```java
MessageUtil.isName("my-param");    // true
MessageUtil.isName("myParam");     // true
MessageUtil.isName("Über_wert");   // true (Unicode letters allowed)
MessageUtil.isName("123invalid");  // false (starts with digit)
MessageUtil.isName("no--good");    // false (consecutive hyphens)
```

There is also `validateName`, which checks that a string is not null and not blank. It returns
the string unchanged on success and throws a `NullPointerException` or
`IllegalArgumentException` otherwise. This is used internally to guard method parameters.


### Space Handling

The library defines its own notion of what constitutes a space character. Unicode space
separators (`SPACE_SEPARATOR`), paragraph separators (`PARAGRAPH_SEPARATOR`), tabs and carriage
returns are all considered spaces. Newlines are explicitly excluded. This distinction matters
during message parsing, where whitespace is collapsed but newlines have no special meaning and
are silently dropped as control characters.

`isSpaceChar` tests a single character against this definition:

```java
MessageUtil.isSpaceChar(' ');       // true  (regular space)
MessageUtil.isSpaceChar('\t');      // true  (tab)
MessageUtil.isSpaceChar('\u00A0');  // true  (non-breaking space)
MessageUtil.isSpaceChar('\n');      // false (newline)
```

`trimSpaces` removes leading and trailing space characters from a string. Unlike
`String.trim()`, it preserves newlines because they are not part of the space definition. It
returns `null` when given `null`.

```java
MessageUtil.trimSpaces("  hello  ");  // "hello"
MessageUtil.trimSpaces("\thello\n");  // "hello\n"
MessageUtil.trimSpaces(null);         // null
```

`trimAndNormalizeSpaces` goes a step further. After trimming, it collapses consecutive internal
space characters into a single ASCII space and replaces any remaining Unicode space character
(such as non-breaking space or em space) with a regular space.

```java
MessageUtil.trimAndNormalizeSpaces("  a   b  ");       // "a b"
MessageUtil.trimAndNormalizeSpaces("a\u00A0\u00A0b");  // "a b"
MessageUtil.trimAndNormalizeSpaces("x\n  y");          // "x\n y"
```

`isTrimmedEmpty` returns `true` if the string is empty or consists entirely of space characters:

```java
MessageUtil.isTrimmedEmpty("   ");    // true
MessageUtil.isTrimmedEmpty("");       // true
MessageUtil.isTrimmedEmpty("  a  "); // false
```

`isEmpty` is a simple null-safe check for `null` or zero-length strings. It does not consider
whitespace.

```java
MessageUtil.isEmpty(null);  // true
MessageUtil.isEmpty("");    // true
MessageUtil.isEmpty(" ");   // false
```


## SupplierDelegate

`SupplierDelegate` is a `Supplier` implementation that computes its value exactly once. On the
first call to `get()`, it invokes the wrapped supplier, caches the result, and releases the
reference to the wrapped supplier so it can be garbage collected. All subsequent calls return the
cached value without invoking the supplier again.

Instances are created through the `of` factory method:

```java
Supplier<String> lazy = SupplierDelegate.of(() -> {
    System.out.println("Computing...");
    return "result";
});

lazy.get();  // prints "Computing..." and returns "result"
lazy.get();  // returns "result" (no print, supplier not invoked)
```

The library uses `SupplierDelegate` internally for deferred message parsing and lazy exception
formatting. In application code it can serve as a lightweight compute-once wrapper when you need
lazy initialization without the overhead of a full caching framework.

Note that `SupplierDelegate` is not thread-safe. If multiple threads may call `get()`
concurrently, external synchronization is required.
