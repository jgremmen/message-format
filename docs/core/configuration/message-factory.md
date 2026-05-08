# MessageFactory

The `MessageFactory` is responsible for parsing message format strings into `Message` objects.
Every `MessageSupport` instance uses a `MessageFactory` internally, and the factory you choose
determines how parsed messages are normalized and whether they are cached. This page covers how
to create and configure a `MessageFactory`. For the full API of parsing methods and the
`MessageBuilder`, see [Messages and Templates](../messages-and-templates.md).


## The Shared No-Cache Instance

For most applications the shared `MessageFactory.NO_CACHE_INSTANCE` is sufficient. It uses the
`PASS_THROUGH` normalizer, which means message parts are not deduplicated, and it does not cache
parsed messages. When you call `MessageSupportFactory.create(FormatterService)` without
specifying a factory, this is the instance that is used:

```java
// these two calls are equivalent
var ms1 = MessageSupportFactory.create(formatterService);
var ms2 = MessageSupportFactory.create(formatterService, MessageFactory.NO_CACHE_INSTANCE);
```


## Creating a Custom MessageFactory

A `MessageFactory` is constructed with a `MessagePartNormalizer` and an optional cache size.
The simplest form takes only a normalizer and disables caching:

```java
var factory = new MessageFactory(MessagePartNormalizer.PASS_THROUGH);
```

To enable caching, pass a positive cache size as the second argument:

```java
var factory = new MessageFactory(MessagePartNormalizer.PASS_THROUGH, 256);
```

The resulting factory is then passed to `MessageSupportFactory.create`:

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance(), factory);
```


## Message Caching

When your application formats the same inline message string repeatedly (via
`messageSupport.message("...")`), the format string is parsed on every call unless caching is
enabled. A caching `MessageFactory` keeps up to the configured number of parsed messages in an
LRU cache. Once the cache is full, the least recently used entry is evicted to make room for
new ones.

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

The cache only applies to the `parseMessage(String)` method, which is what gets called when
you format inline message strings. Messages registered by code through
`addMessage(String, String)` are parsed once and stored directly in the message support, so
they do not go through the cache.


## MessagePartNormalizer

When a message format string is parsed, the resulting `Message` object is composed of individual
`MessagePart` instances representing text fragments, parameter references, template references
and post-formatter invocations. In applications that register many messages, structurally
identical parts appear frequently. A `MessagePartNormalizer` can deduplicate these parts so that
equal instances share the same object in memory, reducing the overall memory footprint.

The normalizer's single method, `normalize`, receives a parsed message part and returns either
the same instance or a previously cached equal instance. It is called during parsing, so it
affects every message created through the factory.

### PASS_THROUGH

The built-in `PASS_THROUGH` normalizer performs no deduplication. Every parsed part is a fresh
object. This is the default and is appropriate when the number of messages is small or memory
is not a concern.

```java
var factory = new MessageFactory(MessagePartNormalizer.PASS_THROUGH);
```

### LRUMessagePartNormalizer

For applications with a large number of messages, `LRUMessagePartNormalizer` maintains a bounded
cache of previously seen parts. When a part equal to a cached one is normalized, the cached
instance is returned instead of keeping the new one. The cache evicts the least recently used
entry when it reaches the configured maximum size.

```java
MessagePartNormalizer normalizer = LRUMessagePartNormalizer.create(512);
var factory = new MessageFactory(normalizer, 256);
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance(), factory);
```

In this example, the `LRUMessagePartNormalizer` deduplicates up to 512 message parts while the
`MessageFactory` caches up to 256 parsed messages. These are independent caches that serve
different purposes: the part normalizer reduces memory consumption across all parsed messages,
while the message cache avoids repeated parsing of the same format string.

### Custom Normalizers

Since `MessagePartNormalizer` is a functional interface, you can provide your own implementation.
A custom normalizer receives a `MessagePart` and must return an equal instance (which may or may
not be the same object):

```java
MessagePartNormalizer myNormalizer = new MessagePartNormalizer() {
  private final Map<MessagePart, MessagePart> cache =
      new ConcurrentHashMap<>();

  @Override
  public <T extends MessagePart> @NotNull T normalize(@NotNull T part) 
  {
    @SuppressWarnings("unchecked")
    T cached = (T)cache.putIfAbsent(part, part);

    return cached != null ? cached : part;
  }
};
```

!!! warning
    A normalizer that grows without bound will eventually consume more memory than it saves.
    Always use a bounded cache or a strategy that limits the number of retained entries.


## Thread Safety

`MessageFactory` is thread-safe. All its public methods can be called concurrently from multiple
threads.
