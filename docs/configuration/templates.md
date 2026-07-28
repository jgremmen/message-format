---
toc_depth: 2
---

# Templates

Templates are reusable message fragments that other messages can reference through the `%[template-name]` syntax. A 
template is parsed from a format string just like a message, but unlike messages it cannot contain nested template 
references. Templates are registered on a `ConfigurableMessageSupport` under a name and are resolved at format time 
when a message references them.

Template names must follow the kebab-case naming convention: lowercase letters and digits separated by single hyphens
(e.g. `opt-error`, `item-count`).

This page explains how to create templates through `MessageFactory` parsing methods and the programmatic
`MessageBuilder`, how to register them on a `ConfigurableMessageSupport` and how to implement custom templates in Java.
For the format string syntax itself, see [Syntax](../message/syntax.md). For how to configure and obtain a
`MessageFactory`, see [MessageFactory](message-factory.md).


## Creating Templates with MessageFactory

`MessageFactory` provides parsing methods for templates in two flavors: a single format string and a locale-keyed map
of format strings.

### Parsing a Single Template

Templates are parsed with `parseTemplate(String)`, which returns a `Template`. The returned `Template` wraps the parsed
message internally and can be registered on a `ConfigurableMessageSupport` under a name.

```java
MessageFactory factory = MessageFactory.getSharedInstance();

Template template = factory.parseTemplate("%{error,!empty:': %{error}'}");
```

### Parsing Localized Templates

Localized templates work the same way as localized messages. Pass a `Map<Locale,String>` to `parseTemplate(Map)`:

```java
Template template = factory.parseTemplate(Map.of(
    Locale.ENGLISH, "%{count} %{count,1:'item',:'items'}",
    Locale.GERMAN,  "%{count} %{count,1:'Eintrag',:'Einträge'}"));
```

When the map contains more than one entry, the factory generates a template code with the prefix `TPL[...]` internally.


## Building Templates Programmatically

The `MessageBuilder` fluent API can produce a `Template` directly by calling `buildAsTemplate()` as the terminal 
operation instead of `build()`. This is useful when the template structure is determined at runtime or to avoid 
embedding format strings in Java source.

```java
Template template = MessageBuilder
    .create()
    .parameter("error")
        .mapEmpty().ne().message(inner ->
            inner.text(":").parameter("error").spaceBefore())
        .mapDefault().message(Message.empty())
    .buildAsTemplate();

messageSupport.addTemplate("opt-error", template);
```

For the full `MessageBuilder` API including all part types, space control and configuration  options, see
[Messages](messages.md#building-messages-programmatically).


## Adding Templates to ConfigurableMessageSupport

Once templates have been created or parsed, they need to be registered on a `ConfigurableMessageSupport` before messages
can reference them. The [MessageSupport](message-support.md) page covers the `ConfigurableMessageSupport` API in detail;
this section focuses on the different ways to add templates.

### Adding Parsed Templates

Templates are registered with a name and a `Template`. The template is typically obtained from `parseTemplate(String)`
or `parseTemplate(Map)`:

```java
MessageFactory factory = messageSupport
    .getMessageAccessor()
    .getMessageFactory();

messageSupport.addTemplate("opt-error",
    factory.parseTemplate("%{error,!empty:': %{error}'}"));

messageSupport
    .message("Operation failed%[opt-error]")
    .with("error", "disk full")
    .format();
// "Operation failed: disk full"

messageSupport
    .message("Operation failed%[opt-error]")
    .with("error", "")
    .format();
// "Operation failed"
```

Templates can also be built programmatically and then registered. When using a `MessageBuilder`, call
`buildAsTemplate()` instead of `build()` to obtain a `Template` directly:

```java
Template template = MessageBuilder
    .create()
    .parameter("unit")
        .mapEmpty().ne().message(inner ->
            inner.parameter("unit").spaceBefore())
        .mapDefault().message(Message.empty())
    .buildAsTemplate();

messageSupport.addTemplate("opt-unit", template);

messageSupport
    .message("Distance: %{value}%[opt-unit]")
    .with("value", 42)
    .with("unit", "km")
    .format();
// "Distance: 42 km"
```

### Custom Templates

Beyond message-based templates, the library supports custom `Template` implementations through the
`AbstractNamedTemplate` base class. A custom template receives the message accessor and parameters at format time and 
returns formatted text directly from Java code, without a parsed message format string.

The `Template` interface is sealed and permits two implementation paths: `MessageTemplate` for templates backed by a
parsed message format string and `NamedTemplate` for templates implemented entirely in Java. `AbstractNamedTemplate`
is the non-sealed base class that implements `NamedTemplate`, so extending it allows formatted output to be produced 
through arbitrary logic rather than through the message format parser.

A custom template must implement the `getName()` method (returning the kebab-case template name) and the `formatAsText`
method, which receives a `MessageAccessor` and a `Parameters` object and returns a `MessagePart.Text`. The 
`AbstractNamedTemplate` base class provides a default `isSame` implementation that considers two templates the same 
when the other template is a `NamedTemplate` with the same name.

The following example implements a template that renders the current date:

```java
public class CurrentDateTemplate extends AbstractNamedTemplate 
{
  @Override
  public @NotNull String getName() {
    return "current-date";
  }

  @Override
  public @NotNull Text formatAsText(
      @NotNull MessageAccessor messageAccessor,
      @NotNull Parameters parameters)
  {
    var locale = parameters.getLocale();
    var formatted = LocalDate.now()
        .format(DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(locale));

    return TextPartFactory.noSpaceText(formatted);
  }
}
```

Register the custom template on a `ConfigurableMessageSupport` like any other template:

```java
messageSupport.addTemplate("current-date", new CurrentDateTemplate());

messageSupport
    .message("Report generated on %[current-date].")
    .locale(Locale.US)
    .format();
// "Report generated on Jun 9, 2026."
```


### Template Registration via ServiceLoader

Custom templates can be registered automatically through the Java `ServiceLoader` mechanism  instead of adding them 
manually in code. This is especially useful for library authors who want to ship templates that are available out of 
the box when the library is on the classpath.

To participate in service-based discovery, extend `AbstractNamedTemplate` (which implements `NamedTemplate`) and 
implement the `getName()` method to return the kebab-case name under which the template will be registered:

```java
public class CurrentDateTemplate extends AbstractNamedTemplate 
{
  @Override
  public @NotNull String getName() {
    return "current-date";
  }

  @Override
  public @NotNull Text formatAsText(
      @NotNull MessageAccessor messageAccessor,
      @NotNull Parameters parameters)
  {
    var locale = parameters.getLocale();
    var formatted = LocalDate.now()
        .format(DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(locale));

    return TextPartFactory.noSpaceText(formatted);
  }
}
```

Declare the provider in the `module-info.java` or in a `META-INF/services/de.sayayi.lib.message.template.NamedTemplate`
file:

```java
// module-info.java
provides de.sayayi.lib.message.template.NamedTemplate
    with com.example.CurrentDateTemplate;
```

To trigger discovery, call `registerTemplatesFromService` on a `ConfigurableMessageSupport`:

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());

messageSupport.registerTemplatesFromService(
    CurrentDateTemplate.class.getClassLoader());
```

The shared `MessageSupport` singleton returned by `MessageSupportFactory.shared()` performs this discovery 
automatically, so any `NamedTemplate` providers on the classpath are registered without explicit code.


### Duplicate Handling

Attempting to add a template whose name already exists throws a `DuplicateTemplateException` if the content differs. 
If the new template is identical to the existing one, the duplicate is silently ignored. This behavior can be 
customized by installing a `TemplateFilter` as described on the [MessageSupport](message-support.md#filters) page.
