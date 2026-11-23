---
icon: simple/intellijidea
---

# IntelliJ Plugin

Message format strings are written as Java string literals, so the IDE treats them as plain text by default. A typo
in a parameter map, a missing closing brace or a map entry that can never be selected is only noticed when the
message is formatted at runtime.

The IntelliJ plugin *Message Format Language* adds the language `MessageFormat` to IntelliJ IDEA. The IDE then
understands message format strings wherever they occur in a project: it highlights their structure, reports syntax
errors while typing, offers code completion and analyzes the messages with inspections. The plugin also includes a
viewer for compiled [pack files](../configuration/pack-files.md).

The plugin is not published on the JetBrains Marketplace. It is distributed through a custom plugin repository,
which is added to the IDE once. After that, the plugin is installed and updated like any other plugin.


## Requirements

The plugin requires IntelliJ IDEA 2026.1 or later, with the bundled Java plugin enabled. Highlighting of message
format code fences in Markdown documents additionally requires the bundled Markdown plugin. Without it, all other
features are available.

The plugin needs no dependency in the project. It recognizes the annotations of the
[annotation adopter](../adopter/annotation/index.md) by name, so it works for projects with and without the
`message-format-annotations` artifact.


## Installation

The custom plugin repository is located at:

```text
https://plugins.sayayi.de/intellij/message-format
```

To install the plugin from this repository:

1. Open the settings with Ctrl+Alt+S (⌘, on macOS) and select **Plugins**.
2. Click the gear icon at the top of the dialog and select **Manage Plugin Repositories...**.
3. Click **+**, enter the repository URL shown above and confirm the dialog with **OK**.
4. Switch to the **Marketplace** tab, search for *Message Format Language* and click **Install**.
5. Restart the IDE if asked to do so.

Once the repository has been added, the IDE checks it for updates together with the JetBrains Marketplace and
offers new versions of the plugin automatically.

/// note | Installation without a plugin repository
The repository URL serves an `updatePlugins.xml` file, which names the download URL of the plugin ZIP file of the
current version. The ZIP file can be downloaded from there and installed with **Install Plugin from Disk...** in the
gear menu of the **Plugins** settings. A plugin installed this way is not updated automatically.
///


## Messages in Java Code

Most messages are defined in Java code. The plugin injects the `MessageFormat` language into string literals, text
blocks and string concatenations in the following places:

- the message texts of the `@MessageDef`, `@TemplateDef` and `@Text` annotations
- the initializer of a variable or field annotated with `@Language("MessageFormat")`
- an argument for a method or constructor parameter annotated with `@Language("MessageFormat")`
- the value of an annotation attribute whose method is annotated with `@Language("MessageFormat")`

The methods of the library that take a message format string, such as `MessageSupport.message(String)` and
`MessageFactory.parseMessage(String)`, declare their parameter with `@Language("MessageFormat")`. Messages passed to
these methods are therefore recognized without further changes to the code:

```java
@MessageDef(code = "cart.summary",
    text = "%{items,format:choice,0:'empty',:'%{items} items'}")
public void checkout()
{
  ...
}

String text = messageSupport
    .message("Hello %{name}!")
    .with("name", name)
    .format();
```

Your own methods and fields can opt in the same way with the `@Language` annotation of the JetBrains annotations
library:

```java
@Language("MessageFormat")
private static final String GREETING = "Hello %{name}!";
```

The injected message is treated as a single piece of message format code. Java escape sequences are decoded before
the message is analyzed, and the delimiters and indentation of text blocks are left out. A concatenation of several
literals is analyzed as one message.

Messages can also be kept in standalone files with the extension `.messageformat`, and written in Markdown documents
as fenced code blocks with the language `message-format`.


## Editing Messages

### Syntax highlighting

Parameters, formatter names, map keys, configuration entries, templates, post formatters and quoted messages are
colored separately, so the structure of a message is visible at a glance. The colors can be adjusted for light
and dark color schemes in **Settings | Editor | Color Scheme | MessageFormat**.

### Code completion

Completion is offered only where the grammar allows it, e.g. formatter names after `format:` and configuration
keys within a parameter. The formatter names are taken from the message format library.

### Quick documentation

The quick documentation (Ctrl+Q, F1 on macOS) of a parameter, template or post formatter shows the
highlighted part with its attributes, such as its format, configuration entries, map entries and template
parameters.

### Brace matching and folding

Matching braces are highlighted, and long or nested parameters can be folded to keep complex messages readable.

### Live templates

The live templates `mfparam`, `mfmap`, `mfformat`, `mftemplate`, `mfpost` and `mfconfig` insert a parameter, a
parameter with a map entry, a parameter with a format, a template reference, a post formatter and a configuration
entry.


## Structure of a Message

The structure view shows a message in two tabs. The **Outline** tab lists all parts of the message in source order,
at any nesting depth, with previews of nested messages. The **Inputs** tab lists every parameter, template, post
formatter and format the message depends on, together with all their uses.

For messages in Java code, the outline is shown below the field, method or class that defines the message in the
structure view of the Java file. **Navigate | Message Structure** opens a popup with both tabs for the message at the
caret. Within a message in Java code, the File Structure shortcut (Ctrl+F12, ⌘F12 on macOS) opens this
popup instead of the structure of the Java file.


## Finding Mistakes

Syntax errors are reported while typing, with a precise description of the problem. Constructs that the library
rejects, such as a template reference within a template definition, are reported as errors as well.

Names that violate the naming conventions of the library can be fixed with an intention, which converts a name to
kebab case or lower camel case.

Inspections analyze the meaning of a message. They are found in the **MessageFormat** group of
**Settings | Editor | Inspections** and are enabled by default. Both inspections provide a quick fix that removes the
reported entry, also in the results of **Code | Inspect Code...**.

### Unreachable parameter map entry

Reports map entries that can never be selected, because earlier entries already cover all their keys. Number keys
are compared as number ranges. String keys are compared in the locale of the message: the locale of its `@Text`
annotation or, for other messages, the locale configured in **Settings | Languages & Frameworks | MessageFormat**,
which defaults to the locale of the system.

```text
%{n,0:'none',<5:'few',<3:'some',:'many'}
```

In this message, the entry `<3:'some'` is reported, as all its keys are already covered by `0` and `<5`.

### Unreachable template entry

Reports parameter delegations and default parameter values of a template reference that have no effect: a
delegation of a parameter to itself, a second delegation of the same parameter and a second default value for the
same parameter.

```text
%[tpl,a->a,x->y,x->z,n=42,n=99]
```

Here `a->a`, `x->z` and `n=99` are reported.


## Pack Files

Message format packs, created with `exportMessages` or the [Gradle plugin](../gradle-plugin/index.md), are opened
in a read-only viewer. Packs are recognized by the extension `.mfp`, or by their header if a pack has a different
extension.

The viewer lists the messages and the templates of a pack on separate tabs, in tables that can be sorted and
filtered. Each message is shown with syntax highlighting, and the templates tab shows the number of messages
referencing each template. Selecting a message or a template opens a structure pane with its outline and inputs. A
template referenced in the structure pane leads to its entry on the templates tab.
