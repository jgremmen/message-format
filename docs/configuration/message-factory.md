# MessageFactory

The `MessageFactory` is responsible for parsing message format strings into `Message` objects. Every `MessageSupport`
instance uses a `MessageFactory` internally and the chosen factory determines how parsed messages are normalized and
whether they are cached. This page covers how to create and configure a `MessageFactory`. For the full API of parsing 
methods and the `MessageBuilder`, see [Messages](messages.md) and [Templates](templates.md).


## The Shared Instance

For most applications the shared `MessageFactory.getSharedInstance()` singleton is sufficient. It uses the 
`PASS_THROUGH` normalizer, which means message parts are not deduplicated and it caches up to 128 parsed messages. When
`MessageSupportFactory.create(FormatterService)` is called without specifying a factory, this is the instance that is 
used. `MessageSupportFactory.createDefault()` and `createGeneric()` use it as well:

```java
var formatterService = DefaultFormatterService.getSharedInstance();

// these two calls are equivalent
var ms1 = MessageSupportFactory.create(formatterService);
var ms2 = MessageSupportFactory.create(formatterService,
    MessageFactory.getSharedInstance());
```


## Creating a Custom MessageFactory

A `MessageFactory` is constructed with a `MessagePartNormalizer` and an optional cache size. The simplest form takes 
only a normalizer and disables caching:

```java
var factory = new MessageFactory(MessagePartNormalizer.PASS_THROUGH);
```

To enable caching, pass a positive cache size as the second argument. A size of `0` or less disables the cache:

```java
var factory = new MessageFactory(MessagePartNormalizer.PASS_THROUGH, 256);
```

The resulting factory is then passed to `MessageSupportFactory.create`:

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance(), factory);
```


## Message Caching

When an application formats the same inline message string repeatedly (via `messageSupport.message("...")`), the format
string is parsed on every call unless the factory has a message cache. The shared factory has one. A caching
`MessageFactory` keeps up to the configured number of parsed messages in an LRU cache. Once the cache is full, the least
recently used entry is evicted to make room for new ones.

```java
var factory = new MessageFactory(MessagePartNormalizer.PASS_THROUGH, 512);
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance(), factory);

// first call: parses the format string
messageSupport
    .message("Hello %{name}!")
    .with("name", "Alice")
    .format();
// "Hello Alice!"

// second call with the same format string: returns the cached Message
messageSupport
    .message("Hello %{name}!")
    .with("name", "Bob")
    .format();
// "Hello Bob!"
```

The cache is keyed by the format string and is used by every method that parses a message: `parseMessage(String)`,
which is what gets called when formatting inline message strings, as well as `parseMessage(String, String)` and the
map-based variants. Messages registered by code through `addMessage(String, String)` therefore also pass through the
cache, although they are parsed only once and then stored in the message support. Templates parsed with
`parseTemplate(...)` are never cached.

/// tip | Cache Contention
Lookups, and parsing on a cache miss, happen while the factory holds its cache lock. Threads that parse many
different format strings at the same time therefore wait for each other. Registering frequently used messages by code,
or importing them from a [pack file](pack-files.md), avoids repeated parsing altogether.
///


## MessagePartNormalizer

When a message format string is parsed, the resulting `Message` object is composed of individual `MessagePart` 
instances representing text fragments, parameter references, template references and post-formatter invocations. In 
applications that register many messages, structurally identical parts appear frequently. A `MessagePartNormalizer` can
deduplicate these parts so that equal instances share the same object in memory, reducing the overall memory footprint.

The normalizer's single method, `normalize`, receives a parsed message part and returns either the same instance or a
previously cached equal instance. It is called during parsing, so it affects every message created through the factory.

### PASS_THROUGH

The built-in `PASS_THROUGH` normalizer performs no deduplication. Every parsed part is a fresh object. This is the
default and is appropriate when the number of messages is small or memory is not a concern.

```java
var factory = new MessageFactory(MessagePartNormalizer.PASS_THROUGH);
```

### LRUMessagePartNormalizer

For applications with a large number of messages, `LRUMessagePartNormalizer` maintains a bounded cache of previously 
seen parts. When a part equal to a cached one is normalized, the cached instance is returned instead of keeping the new
one. The cache evicts the least recently used entry when it reaches the configured maximum size. The maximum size
passed to `LRUMessagePartNormalizer.create(int)` must be positive, otherwise an `IllegalArgumentException` is thrown.

```java
MessagePartNormalizer normalizer = LRUMessagePartNormalizer.create(512);
var factory = new MessageFactory(normalizer, 256);
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance(), factory);
```

In this example, the `LRUMessagePartNormalizer` deduplicates up to 512 message parts while the `MessageFactory` caches
up to 256 parsed messages. These are independent caches that serve different purposes: the part normalizer reduces
memory consumption across all parsed messages, while the message cache avoids repeated parsing of the same format 
string.

### Custom Normalizers

A custom implementation of `MessagePartNormalizer` can be provided as well. Because `normalize` is a generic method,
the interface cannot be implemented with a lambda expression, even though it is annotated as a functional interface. A
custom normalizer receives a `MessagePart` and must return an equal instance (which may or may not be the same
object). The factory calls the normalizer from every thread that parses messages, so the implementation must be
thread-safe:

```java title="UnboundedPartNormalizer.java"
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.part.normalizer.MessagePartNormalizer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

public final class UnboundedPartNormalizer
    implements MessagePartNormalizer
{
  private final Map<MessagePart,MessagePart> cache =
      new ConcurrentHashMap<>();

  @Override
  public <T extends MessagePart> @NotNull T normalize(@NotNull T part)
  {
    @SuppressWarnings("unchecked")
    T cached = (T)cache.putIfAbsent(part, part);

    // returns the first instance seen for each distinct part
    return cached != null ? cached : part;
  }
}
```

/// warning | Unbounded Normalizer
The normalizer above is unsafe for long-running applications that parse format strings from unbounded sources. It
grows without bound and will eventually consume more memory than it saves. Always use a bounded cache or a strategy
that limits the number of retained entries.
///


## Thread Safety

`MessageFactory` is thread-safe. All its public methods can be called concurrently from multiple threads. The
normalizers returned by `LRUMessagePartNormalizer.create(int)` are thread-safe as well. A `MessageBuilder` obtained
from `messageBuilder()` is not thread-safe and must be used by a single thread only.
