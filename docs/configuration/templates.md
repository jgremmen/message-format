---
toc_depth: 2
---

# Templates

Templates are reusable message fragments that other messages can reference through the `%[template-name]` syntax. A 
template is parsed from a format string just like a message, but unlike messages it cannot contain nested template 
references. Templates are registered on a `ConfigurableMessageSupport` under a name and are resolved at format time 
when a message references them.

Template names must follow the kebab-case naming convention: lowercase letters and digits separated by single hyphens
(e.g. `opt-error`, `item-count`). The name must start with a letter.

A template is formatted in place of its `%[...]` reference. Leading and trailing spaces produced by the template are
not significant. They are replaced by the spaces surrounding the reference in the enclosing message, so a space
before or after the template output has to be written in the message, not in the template. A reference to a template
that has not been registered produces no text. `TemplateAccessor.findMissingTemplates(...)`, described on the
[MessageSupport](message-support.md#templateaccessor) page, finds such references in advance.

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

A template that contains a template reference is rejected with a `MessageParserException`.

### Parsing Localized Templates

Localized templates work the same way as localized messages. Pass a `Map<Locale,String>` to `parseTemplate(Map)`:

```java
Template template = factory.parseTemplate(Map.of(
    Locale.ENGLISH,
    "%{count} %{count,format:choice,1:'item',:'items'}",
    Locale.GERMAN,
    "%{count} %{count,format:choice,1:'Eintrag',:'Einträge'}"));
```

The locale is selected with the same rules as for [localized messages](messages.md#messagelocaleaware). When the map
contains more than one entry, the factory generates a template code with the prefix `TPL[...]` internally. An empty
map produces a template that formats to an empty string.


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
    .buildAsTemplate();

messageSupport.addTemplate("opt-error", template);
```

`buildAsTemplate()` throws an `IllegalStateException` if the built message contains a template reference.

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
    .parameter("value")
    .parameter("unit")
        .mapEmpty().ne().message(inner ->
            inner.parameter("unit").spaceBefore())
    .buildAsTemplate();

messageSupport.addTemplate("measure", template);

messageSupport
    .message("Distance: %[measure].")
    .with("value", 42)
    .with("unit", "km")
    .format();
// "Distance: 42 km."
```

The optional unit and its separating space are both part of the template. Writing the space inside the template is
essential here. A template that only produced the unit, referenced as `%{value}%[opt-unit]`, would lose its leading
space, because the spacing of a template reference is determined by the enclosing message.

### Custom Templates

Beyond message-based templates, the library supports custom `Template` implementations through the
`AbstractNamedTemplate` base class. A custom template receives the message accessor and parameters at format time and 
returns formatted text directly from Java code, without a parsed message format string.

The `Template` interface is sealed and permits two implementation paths. Templates backed by a parsed message format
string are created by `MessageFactory` and `MessageBuilder` and use an internal implementation class. Templates
implemented entirely in Java implement `NamedTemplate`, which is sealed as well and only permits
`AbstractNamedTemplate`. Custom templates therefore always extend `AbstractNamedTemplate`, which allows formatted
output to be produced through arbitrary logic rather than through the message format parser.

A custom template must implement the `getName()` method (returning the kebab-case template name) and the `formatAsText`
method, which receives a `MessageAccessor` and a `Parameters` object and returns a `MessagePart.Text`. The 
`AbstractNamedTemplate` base class provides a default `isSame` implementation that considers two templates the same 
when the other template is a `NamedTemplate` with the same name.

The following example implements a template that renders the current date:

```java title="CurrentDateTemplate.java"
package com.example;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.TextPartFactory;
import de.sayayi.lib.message.template.AbstractNamedTemplate;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import org.jetbrains.annotations.NotNull;

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

Register the custom template on a `ConfigurableMessageSupport` like any other template. The `addTemplate(NamedTemplate)`
overload registers it under the name returned by `getName()`:

```java
messageSupport.addTemplate(new CurrentDateTemplate());

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
implement the `getName()` method to return the kebab-case name under which the template will be registered. The
`CurrentDateTemplate` class shown above already meets these requirements. As with every service provider, the class
must be public and have a public no-argument constructor.

Declare the provider in the `module-info.java` or in a `META-INF/services/de.sayayi.lib.message.template.NamedTemplate`
file:

=== "module-info.java"

    ```java
    module com.example.reports {
      requires de.sayayi.lib.message;

      provides de.sayayi.lib.message.template.NamedTemplate
          with com.example.CurrentDateTemplate;
    }
    ```

=== "META-INF/services"

    ```text title="META-INF/services/de.sayayi.lib.message.template.NamedTemplate"
    com.example.CurrentDateTemplate
    ```

To trigger discovery, call `registerTemplatesFromService` on a `ConfigurableMessageSupport`:

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());

messageSupport.registerTemplatesFromService(
    CurrentDateTemplate.class.getClassLoader());
```

Each discovered template is passed through the installed `TemplateFilter` and registered under the name returned by
`getName()`. A provider whose name does not follow the kebab-case convention causes an `IllegalArgumentException`.

The shared `MessageSupport` singleton returned by `MessageSupportFactory.shared()` performs this discovery 
automatically with the class loader of the library, so any `NamedTemplate` providers visible to that class loader are
registered without explicit code.

/// note | Custom Templates and Pack Files
Custom templates have no format string representation and are never written to a [pack file](pack-files.md), even
if an exported message references them. They must be registered again on the message support that imports the pack.
///


### Duplicate Handling

Attempting to add a template whose name already exists throws a `DuplicateTemplateException` if the content differs. If
the new template is identical to the existing one, the duplicate is silently ignored. Two parsed templates are identical
when their message parts are equal. Two custom templates are identical when they have the same name. This behavior can
be customized by installing a `TemplateFilter` as described on the [MessageSupport](message-support.md#filters) page.
