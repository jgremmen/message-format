# Utilities

The `de.sayayi.lib.message.util` package contains helper classes used throughout the library
and available for application code.


## MessageUtil

`MessageUtil` is a collection of static utility methods. The most commonly used ones deal
with pack file handling and name validation.

### Pack file detection

`isMessageFormatPack(Path)` checks whether a file is a valid message format pack file by
probing its content. This is useful in environments where the standard
`Files.probeContentType()` SPI mechanism is not available, such as Gradle plugins or IDE
integrations:

```java
Path packFile = Path.of("messages.mfp");

if (MessageUtil.isMessageFormatPack(packFile))
    System.out.println("Valid pack file");
else
    System.out.println("Not a pack file");
```

### Importing messages from a pack stream

`importMessages` reads a pack file from an `InputStream` and invokes callbacks for each
message and template found. This gives you full control over how imported entries are
processed:

```java
try (var in = new FileInputStream("messages.mfp")) {
    MessageUtil.importMessages(in,
        message -> System.out.println("Message: " + message.getCode()),
        (name, template) -> System.out.println("Template: " + name));
}
```

Either consumer can be `null` if you are only interested in one type of entry. For example,
to count messages without processing templates:

```java
var count = new int[1];

try (var in = new FileInputStream("messages.mfp")) {
    MessageUtil.importMessages(in, message -> count[0]++, null);
}

System.out.println("Pack file contains " + count[0] + " messages");
```

The stream is always closed when the method returns, regardless of success or failure.

If you simply want to load all messages and templates into a message support instance,
`ConfigurableMessageSupport.importMessages(InputStream)` is a more convenient alternative
that calls this method internally.

### Name validation

Several methods validate naming conventions used by the library:

- `isKebabCaseName(String)` – checks for valid kebab-case (e.g. `clip-suffix`).
- `isLowerCamelCaseName(String)` – checks for valid lower camel-case (e.g. `clipSuffix`).
- `isKebabOrLowerCamelCaseName(String)` – accepts either convention.
- `isName(String)` – checks whether a string is a valid name as defined by the message format
  grammar. A valid name starts with a Unicode letter, followed by letters or numbers,
  optionally separated by underscores or hyphens.

```java
MessageUtil.isKebabCaseName("clip-suffix");       // true
MessageUtil.isKebabCaseName("clipSuffix");         // false

MessageUtil.isLowerCamelCaseName("clipSuffix");    // true
MessageUtil.isLowerCamelCaseName("clip-suffix");   // false

MessageUtil.isName("my-param");                    // true
MessageUtil.isName("myParam");                     // true
MessageUtil.isName("123invalid");                  // false
```

### Space handling

`MessageUtil` provides space-related utilities that follow the library's definition of space
characters (Unicode space separators, paragraph separators, tab and carriage return, but not
newline):

- `isSpaceChar(char)` – tests whether a character is a space character.
- `trimSpaces(String)` – trims leading and trailing spaces, preserving newlines.
- `trimAndNormalizeSpaces(String)` – trims, collapses consecutive spaces, and normalizes
  Unicode spaces to ASCII.
- `isTrimmedEmpty(String)` – returns `true` if the string is empty after trimming.

```java
MessageUtil.trimSpaces("  hello  ");                // "hello"
MessageUtil.trimSpaces("\thello\n");                 // "hello\n"

MessageUtil.trimAndNormalizeSpaces("  a   b  ");     // "a b"
MessageUtil.trimAndNormalizeSpaces("a\u00a0\u00a0b");// "a b"

MessageUtil.isTrimmedEmpty("   ");                   // true
MessageUtil.isTrimmedEmpty("  a  ");                 // false
```


## SupplierDelegate

`SupplierDelegate` is a `Supplier` implementation that delegates to another supplier and
caches the result. The wrapped supplier is invoked at most once; subsequent calls to `get()`
return the cached value. After the first invocation, the reference to the delegate supplier is
released so it can be garbage collected.

Create an instance using the `of` factory method:

```java
Supplier<String> lazy = SupplierDelegate.of(() -> {
    System.out.println("Computing...");
    return "result";
});

lazy.get(); // prints "Computing..." and returns "result"
lazy.get(); // returns "result" without printing
```

The library uses `SupplierDelegate` internally for deferred message parsing and lazy exception
formatting. It can also be useful in application code whenever you need a simple
compute-once supplier without pulling in a heavier caching mechanism.
