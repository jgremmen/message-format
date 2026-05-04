---
title: 0.21.0 -> 0.22.0
toc_depth: 2
---

# Version [0.22.0](https://github.com/jgremmen/message-format/tree/0.22.0) (2026-05-04)

## Breaking Changes

### Sealed `Message` interface hierarchy

The `Message` interface and its sub-interfaces `Message.WithSpaces`, `Message.WithCode` and `Message.LocaleAware` are
now sealed. This means that custom implementations of these interfaces are no longer permitted.

`Message.WithSpaces` permits `CompoundMessage`, `EmptyMessage` and `TextMessage`.
`Message.WithCode` permits `AbstractMessageWithCode` (which in turn permits `EmptyMessageWithCode`,
`LocalizedMessageBundleWithCode` and `MessageDelegateWithCode`).
`Message.LocaleAware` permits `LocalizedMessageBundleWithCode`.

If you have custom classes implementing any of these interfaces, you must refactor your code to use one of the
permitted implementations or compose behavior by delegating to them.

### Sealed `FormatterService` interface hierarchy

The `FormatterService` interface is now sealed. It permits `FormatterService.WithRegistry` and
`GenericFormatterService.SealedFormatterService`. The `FormatterService.WithRegistry` sub-interface is also sealed and
permits only `GenericFormatterService`.

`GenericFormatterService` is declared as `non-sealed`, so existing subclasses of `GenericFormatterService` (such as
`DefaultFormatterService`) continue to work without changes.

If you have custom classes that directly implement `FormatterService` or `FormatterService.WithRegistry`, you must
change them to extend `GenericFormatterService` instead.

### `DefaultFormatterService.getSharedInstance()` now returns a sealed instance

The shared singleton returned by `DefaultFormatterService.getSharedInstance()` is now a sealed (immutable) formatter
service. The return type remains `FormatterService`, but the instance no longer implements
`FormatterService.WithRegistry`. Any code that cast the shared instance to `FormatterService.WithRegistry` or
`GenericFormatterService` to register additional formatters will fail at runtime.

To register custom formatters, create your own `DefaultFormatterService` instance:

```java
var formatterService = new DefaultFormatterService();
formatterService.addParameterFormatter(new MyCustomFormatter());
```

### `CharsetFormatter` removed

The `CharsetFormatter` for `java.nio.charset.Charset` values has been removed. `Charset.toString()` already produces
the charset name, so the dedicated formatter did not add value beyond what the default `String` formatting provides.

If you relied on the `charset:display` or `charset:display-name` configuration to obtain the locale-specific display
name, format the display name explicitly before passing it as a parameter:

```java
messageSupport
    .message("my-msg")
    .with("charset", myCharset.displayName(locale))
    .format();
```

### `BitsFormatter` removed

The deprecated named formatter `bits` has been removed. Use the new `bitmask` named formatter
(`BitmaskFormatter`) together with the `BitSetFormatter` as a replacement. See the "New Features" section for details.

### `AbstractMessageWithCode` visibility changed

The class `AbstractMessageWithCode` has been changed from package-private to `public sealed abstract`. While this
broadens its visibility, the `sealed` modifier restricts subclassing to the permitted classes
(`EmptyMessageWithCode`, `LocalizedMessageBundleWithCode`, `MessageDelegateWithCode`). External subclasses are not
allowed.


## New Features

### `BitSetFormatter` for `java.util.BitSet` values

A new typed parameter formatter handles `BitSet` values. The `bitset` configuration key controls the output mode:

- `lsb-set` (default) -- renders set bits using their mapped messages, least significant bit first
- `msb-set` -- renders set bits using their mapped messages, most significant bit first
- `lsb-bits` -- formats all bits as a binary string, least significant bit first
- `msb-bits` -- formats all bits as a binary string, most significant bit first

In set-bit mode (`lsb-set`/`msb-set`), each set bit is mapped to a message via a number-keyed map entry:

```msgfmt
%{flags, bitset:'lsb-set', 0:'Read', 1:'Write', 2:'Execute'}
```

For a `BitSet` with bits 0 and 2 set, this produces `Read, Execute`.

In binary string mode (`lsb-bits`/`msb-bits`), the `bit0` and `bit1` configuration keys control the character
used for unset and set bits (defaulting to `0` and `1`):

```msgfmt
%{flags, bitset:'msb-bits', bit0:'.', bit1:'#'}
```

The formatter inherits all list formatting configuration keys from `AbstractListFormatter` (such as `list-sep`,
`list-sep-last`, `list-max-size`, `list-value-more` and `list-unique`).

### `BitmaskFormatter` named formatter

A new named parameter formatter `bitmask` converts integral numeric values (`byte`, `short`, `int`, `long`,
`char`, `BigInteger`) into a `BitSet` and delegates formatting to the registered `BitSetFormatter`.

```msgfmt
%{permissions, format:bitmask, 0:'Read', 1:'Write', 2:'Execute'}
```

This replaces the removed `BitsFormatter`. Where `bits` produced a raw binary string, `bitmask` combined with
`BitSetFormatter` supports both binary string output and mapped set-bit rendering.

### Duplicate suppression in list formatting

The `AbstractListFormatter` (used by `ArrayFormatter`, `IterableFormatter`, `MapFormatter` and `BitSetFormatter`)
now supports a `list-unique` configuration key. When set to `true`, duplicate formatted element texts are
suppressed:

```msgfmt
%{items, list-unique:true, list-sep:', '}
```

### `FormatterService.WithRegistry.seal()` method

A new `seal()` method on `FormatterService.WithRegistry` creates an immutable snapshot of the formatter service. The
returned instance delegates all queries to the original service but does not expose any registration methods:

```java
FormatterService.WithRegistry registry = new GenericFormatterService();
registry.addParameterFormatter(new MyFormatter());

FormatterService sealed = registry.seal();
```

### `MessageSupportFactory.create(FormatterService)` convenience method

A new overload of `MessageSupportFactory.create` accepts a single `FormatterService` parameter and uses a non-caching
`MessageFactory`. This simplifies the common case where no custom message factory is needed:

```java
var messageSupport = MessageSupportFactory.create(formatterService);
```

### `MessageUtil.isMessageFormatPack(Path)` utility method

A new static method `MessageUtil.isMessageFormatPack(Path)` probes whether a file is a message format pack file. This
is useful in environments where the `PackFileTypeDetector` service provider is not available due to classloader
isolation (e.g. Gradle plugins or IntelliJ IDEA plugins):

```java
if (MessageUtil.isMessageFormatPack(path))
  messageSupport.importMessages(Files.newInputStream(path));
```


## Bug Fixes

- Fixed `PackFileTypeDetector` not being discovered in Gradle's isolated classloader environment, causing
  `Files.probeContentType()` to return `null` for message format pack files.
- Fixed compiler warning for ANTLR4-generated source files by adding the `-implicit:class` compiler argument.
