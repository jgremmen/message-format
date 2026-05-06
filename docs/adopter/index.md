---
icon: material/invoice-import-outline
---

# Message Adopters

Adopters are the bridge between external message sources and the message format library. They
read messages and templates from formats you may already have in your project, such as Java
resource bundles or properties files, parse the values as message format strings, and publish the
results to a `MessageSupport` instance. This lets you reuse existing localization infrastructure
without manually re-registering every message in code.

The core library ships with two concrete adopters:
[`ResourceBundleAdopter`](resource-bundle.md) for Java `ResourceBundle` instances and
[`PropertiesAdopter`](properties.md) for `Properties` objects. Additional adopters for
annotation-based message definitions are provided by the
[Annotations](annotation/index.md), [ASM](annotation/asm.md) and
[Spring](annotation/spring.md) modules.


## AbstractMessageAdopter

All adopters extend `AbstractMessageAdopter`, which holds the two collaborators every adopter
needs: a `MessageFactory` for parsing message format strings into `Message` objects, and a
`MessagePublisher` for storing the parsed messages and templates.

The most common way to construct an adopter is by passing a `ConfigurableMessageSupport`. Because
`ConfigurableMessageSupport` implements `MessagePublisher` and provides access to a
`MessageFactory` through its message accessor, a single argument is sufficient:

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());

var adopter = new ResourceBundleAdopter(messageSupport);
```

When you need to decouple factory and publisher, for example to collect messages before
publishing them to multiple targets, every adopter also offers a two-argument constructor:

```java
var adopter = new ResourceBundleAdopter(messageFactory, publisher);
```
