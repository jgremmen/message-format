# highlight.js

Message format strings appear in many places outside of Java code. They show up in documentation, in administration
pages that list the messages of an application, and in tools that translators use to edit message bundles. A message
such as `%{n,format:choice,0:'no items',:'%{n} items'}` is hard to read as plain text. Parameter names, map keys,
quoted messages and nested parts all blend into one string.

The highlight.js add-on is a language definition for [highlight.js](https://highlightjs.org/), the JavaScript syntax
highlighter used by many websites. It colors message format strings in a browser or in any other JavaScript
environment that runs highlight.js. Unlike a highlighter built from regular expressions, it uses the same ANTLR
grammar as the Java parser of the library. The grammar is compiled to JavaScript, and every message is lexed and
parsed exactly as the Java side does it. The highlighting therefore follows the real syntax, including cases that
only the parser can resolve. In `%{true,format:empty}`, for example, `true` is a parameter name and `empty` is a
formatter name, although both words are keywords elsewhere in the syntax.

The add-on is written in JavaScript and is not a Maven artifact. It consists of a few JavaScript and CSS files that a
web project serves along with highlight.js. Each version of the add-on is generated from the grammar of the library
with the same version number. Using matching versions ensures that the highlighting accepts exactly the syntax that
the Java parser accepts.


## Requirements

The language requires highlight.js 11.9 or later, the minimum version declared by the add-on. It drives the
highlighting through `__emitTokens`, a hook that highlight.js marks as beta. With this hook, a language does its own
parsing and only uses highlight.js to render the HTML. Because the hook is not yet a stable API, a future
highlight.js release may change it. The add-on is tested with highlight.js 11.12.0.

highlight.js cannot detect message format strings automatically. Auto-detection depends on the regular expression
engine, which the language bypasses. A code block must therefore always name the language, either by its name
`message-format` or by the alias `msgfmt`.

The JavaScript files are compiled for ECMAScript 2020 and run in all current browsers. The base stylesheet uses the
CSS function `color-mix()`, which is also supported by all current browsers.


## Files

The add-on provides three variants of the language and two stylesheets. All three variants contain the same language
definition. They differ in their module format and in whether they include the ANTLR runtime. highlight.js itself is
never included and must be provided by the web project.

| File                            | Format     | ANTLR runtime  | Intended use                                       |
|---------------------------------|------------|----------------|----------------------------------------------------|
| `message-format.min.js`         | script     | included       | A plain `<script>` tag, registers itself           |
| `message-format.mjs`            | ES module  | included       | `import` in a bundler or `<script type="module">`  |
| `message-format.lib.mjs`        | ES module  | imported       | Bundlers or import maps that provide `antlr4`      |
| `message-format.css`            | CSS        |                | Colors escape sequences, for every theme           |
| `message-format-delimiters.css` | CSS        |                | Colors part delimiters, only for some themes       |

All three variants are minified and come with source maps. The two variants that include the ANTLR runtime have a
size of about 190 KB, or about 50 KB when compressed with gzip. Most of it is the ANTLR runtime and the serialized
grammar. The variant `message-format.lib.mjs` imports the runtime from the package `antlr4` instead, which allows a
web project to share one copy of the runtime with other ANTLR-based code. Without the runtime, it has a size of
about 76 KB, or about 19 KB with gzip. [Integration](integration.md) describes each variant in detail, and
[Styling](styling.md) explains the two stylesheets.

Projects with an npm build that install the add-on as the package `highlightjs-message-format` import the files by
the following paths.

| Import path                                   | File                            |
|-----------------------------------------------|---------------------------------|
| `highlightjs-message-format`                  | `message-format.mjs`            |
| `highlightjs-message-format/lib`              | `message-format.lib.mjs`        |
| `highlightjs-message-format/script`           | `message-format.min.js`         |
| `highlightjs-message-format/css`              | `message-format.css`            |
| `highlightjs-message-format/css/delimiters`   | `message-format-delimiters.css` |

The package declares highlight.js as a peer dependency, so the project must depend on highlight.js itself. The
package `antlr4` is a regular dependency. Only `message-format.lib.mjs` imports it, because the other two variants
include the runtime.


## Minimal Example

The following page highlights a single message. It assumes that highlight.js and the files of the add-on are
served from the `/js` and `/css` directories of the website. The first tab loads the language with a plain
`<script>` tag, and the second tab imports it as an ES module.

=== "Script Tag"

    ```html title="index.html"
    <!doctype html>
    <html>
    <head>
      <link rel="stylesheet" href="/css/default.min.css">
      <link rel="stylesheet" href="/css/message-format.css">
      <link rel="stylesheet"
            href="/css/message-format-delimiters.css">
    </head>
    <body>
      <pre><code class="language-message-format"
        >Hello %{user,empty:'Guest'}!</code></pre>

      <script src="/js/highlight.min.js"></script>
      <script src="/js/message-format.min.js"></script>
      <script>hljs.highlightAll();</script>
      <!-- the message is shown with colored parts -->
    </body>
    </html>
    ```

=== "ES Module"

    ```html title="index.html"
    <!doctype html>
    <html>
    <head>
      <link rel="stylesheet" href="/css/default.min.css">
      <link rel="stylesheet" href="/css/message-format.css">
      <link rel="stylesheet"
            href="/css/message-format-delimiters.css">
    </head>
    <body>
      <pre><code class="language-message-format"
        >Hello %{user,empty:'Guest'}!</code></pre>

      <script type="module">
        import hljs from '/js/es/core.min.js';
        import messageFormat from '/js/message-format.mjs';

        hljs.registerLanguage('message-format', messageFormat);
        hljs.highlightAll();
        // the message is shown with colored parts
      </script>
    </body>
    </html>
    ```

The page uses the `default` theme of highlight.js. This theme gives the part delimiters `%{` and `}` the same color
as message text, so the page also includes the delimiter stylesheet. The page
[Styling](styling.md#themes-that-need-the-delimiter-stylesheet) lists all themes that need it.

After highlighting, the `<code>` element contains nested `<span>` elements, one for each syntax element of the
message. The text `Hello ` and `!` is marked as a string, the parameter `%{user,empty:'Guest'}` as a substitution,
and the parameter name `user` as a variable.

```html title="Generated HTML (line breaks added)"
<span class="hljs-string">Hello </span>
<span class="hljs-subst">
  <span class="hljs-template-tag">%{</span>
  <span class="hljs-variable">user</span>
  <span class="hljs-punctuation">,</span>
  <span class="hljs-literal">empty</span>
  <span class="hljs-punctuation">:</span>
  <span class="hljs-punctuation quote_">&#x27;</span>
  <span class="hljs-string">Guest</span>
  <span class="hljs-punctuation quote_">&#x27;</span>
  <span class="hljs-template-tag">}</span>
</span>
<span class="hljs-string">!</span>
<!-- the theme colors each span by its class -->
```


## Contents

The add-on documentation is divided into two further pages.

[Integration](integration.md) shows how to register the language with each variant, how to highlight strings instead
of page elements, and which options exist. It also covers error handling, performance and common pitfalls.

[Styling](styling.md) lists the highlight.js scopes the language produces and explains how the two stylesheets
complement the highlight.js themes.
