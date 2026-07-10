---
icon: material/invoice-import-outline
---

# Message Adopters

Adopters are the bridge between external message sources and the message format library. They read messages and 
templates from formats already present in a project, such as Java resource bundles or properties files, parse
the values as message format strings and publish the results to a `MessageSupport` instance. This allows existing
localization infrastructure to be reused without manually re-registering every message in code.

The core library ships with two concrete adopters: [`ResourceBundleAdopter`](resource-bundle/index.md) for Java 
`ResourceBundle` instances and [`PropertiesAdopter`](properties/index.md) for `Properties` objects. The
[annotation adopter](annotation/index.md) provides additional support for discovering messages and templates declared
through `@MessageDef` and `@TemplateDef` annotations in compiled class files.


## AbstractMessageAdopter

All adopters extend `AbstractMessageAdopter`, which holds the two collaborators every adopter needs: a 
`MessageFactory` for parsing message format strings into `Message` objects and a `MessagePublisher` for storing the
parsed messages and templates.

The most common way to construct an adopter is by passing a `ConfigurableMessageSupport`. Because
`ConfigurableMessageSupport` implements `MessagePublisher` and provides access to a `MessageFactory` through its
message accessor, a single argument is sufficient:

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());

var adopter = new ResourceBundleAdopter(messageSupport);
```

To decouple factory and publisher, for example to collect messages before publishing them to multiple targets, every 
adopter also offers a two-argument constructor:

```java
var adopter = new ResourceBundleAdopter(messageFactory, publisher);
```
