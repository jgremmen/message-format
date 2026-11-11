# Styling

highlight.js separates the analysis of code from its presentation. A language assigns scopes to pieces of the code,
such as `string`, `variable` or `number`, and highlight.js turns each scope into a `<span>` with a matching CSS
class. A theme is a stylesheet that colors these classes. The same theme therefore works for all languages, as long
as the languages use the common scope names.

This page lists the scopes that the message format language assigns and shows how they nest. It then explains the
two stylesheets of the add-on, which fill gaps that many highlight.js themes leave for message format strings.


## Scopes

Each syntax element of a message receives the scope that highlight.js defines for a comparable element in other
languages. The text of a message is a `string`, because it is literal output, and a parameter name is a `variable`.
The terms in the first column follow the [syntax description](../../message/syntax.md).

| Syntax element                                                    | Scope               | CSS classes                |
|-------------------------------------------------------------------|---------------------|----------------------------|
| Message text, at the top level and in quoted messages             | `string`            | `hljs-string`              |
| Unquoted string values, such as `upper` in `case:upper`           | `string`            | `hljs-string`              |
| Quotes `'` and `"` around quoted messages and strings             | `punctuation.quote` | `hljs-punctuation quote_`  |
| Escape sequences, such as `\%` or `\u00e9`                   | `char.escape`       | `hljs-char escape_`        |
| A complete parameter, template or post formatter part             | `subst`             | `hljs-subst`               |
| Part delimiters `%{` `}` `%[` `]` `%(` `)`                        | `template-tag`      | `hljs-template-tag`        |
| Separators `,` and `:`, parentheses of grouped map keys           | `punctuation`       | `hljs-punctuation`         |
| Parameter names, template parameter names                         | `variable`          | `hljs-variable`            |
| The keyword `format`                                              | `keyword`           | `hljs-keyword`             |
| Formatter names after `format:`, post formatter names             | `title.function`    | `hljs-title function_`     |
| Template names                                                    | `title.class`       | `hljs-title class_`        |
| Configuration keys, such as `clip` in `clip:20`                   | `attr`              | `hljs-attr`                |
| `true`, `false`, `null` and `empty` as values or map keys         | `literal`           | `hljs-literal`             |
| Numbers                                                           | `number`            | `hljs-number`              |
| Operators `=` `!` `<>` `<` `<=` `>` `>=` in map keys, `=` and `->` in templates | `operator` | `hljs-operator` |
| Syntax errors, only with the option `markErrors`                  | `deletion`          | `hljs-deletion`            |

The scope depends on the role of a word in the syntax, not on the word itself. The keywords `true`, `false`, `null`,
`empty` and `format` may also be used as names. In `%{true,format:empty}`, the language highlights `true` as a
parameter name and `empty` as a formatter name.

```html title="Generated HTML for %{true,format:empty} (line breaks added)"
<span class="hljs-subst">
  <span class="hljs-template-tag">%{</span>
  <span class="hljs-variable">true</span>
  <span class="hljs-punctuation">,</span>
  <span class="hljs-keyword">format</span>
  <span class="hljs-punctuation">:</span>
  <span class="hljs-title function_">empty</span>
  <span class="hljs-template-tag">}</span>
</span>
<!-- true is a variable, empty is a function title -->
```


## Nesting

Scopes are nested in the same way as the parts of a message. Every parameter, template or post formatter part is a
`subst` scope, from its opening delimiter to its closing delimiter. All syntax elements of the part are inside it.
highlight.js uses the scope `subst` for the same purpose in other languages, for example for the embedded
expressions of JavaScript template literals.

Message text forms a `string` scope that is interrupted by every part. Text before a part, text after it and the part
itself are three separate spans. The same applies to the text of a quoted message, which may contain parts of its
own. The quotes are not part of the string. They delimit it in the same way as `%{` and `}` delimit a parameter, and
receive their own scope `punctuation.quote`. Escape sequences are nested inside the `string` scope of their text.

The following message is a map with two string keys. The second value is a quoted message that contains a parameter
and two escaped double quotes.

```html title="Message in a code block"
<pre><code class="language-message-format"
  >%{status,'open':'Open',"closed":"Closed \"%{date}\""}</code></pre>
<!-- the inner %{date} is a part inside a quoted message -->
```

```html title="Generated HTML for the value of the second entry"
<span class="hljs-punctuation quote_">&quot;</span>
<span class="hljs-string">Closed
  <span class="hljs-char escape_">\&quot;</span>
</span>
<span class="hljs-subst">
  <span class="hljs-template-tag">%{</span>
  <span class="hljs-variable">date</span>
  <span class="hljs-template-tag">}</span>
</span>
<span class="hljs-string">
  <span class="hljs-char escape_">\&quot;</span>
</span>
<span class="hljs-punctuation quote_">&quot;</span>
<!-- quotes, string runs and the nested part are siblings -->
```

The nesting allows a stylesheet to treat parts inside quoted messages differently from parts at the top level, for
example with a background color for `.hljs-subst .hljs-subst`.


## The Two Stylesheets

highlight.js themes are written for programming languages. Message format strings have two properties that many
themes do not anticipate. The text is mostly literal output, so most of a message is a `string`. In addition, escape
sequences and part delimiters appear inside or next to that text and must stand out from it. Many themes give
these elements no color of their own or the same color as strings.

The add-on ships two small stylesheets that fill these gaps. Both only apply inside elements with the class
`language-message-format`, so other languages on the same page keep the styling of their theme. Their selectors
combine two or three classes and therefore take precedence over the single-class selectors of the themes, regardless
of the order in which the stylesheets are loaded. Both stylesheets are customized with CSS custom properties, which
may be set on the code element or on any of its ancestors.

| Stylesheet                      | Purpose                                     | Include with      |
|---------------------------------|---------------------------------------------|-------------------|
| `message-format.css`            | Distinguishes escape sequences from text    | Every theme       |
| `message-format-delimiters.css` | Distinguishes part delimiters from text     | Only some themes  |


### Escape Sequences

An [escape sequence](../../message/syntax.md#escape-sequences) represents a character that would otherwise have a
special meaning, such as `\%` for a percent sign in front of `{`. A reader must be able to tell the escape sequence
from the plain characters around it. Escape sequences have the scope `char.escape`, but only 12 of the 258 themes
of highlight.js 11.12.0 give that scope a color that differs from strings. In all other themes, the escape sequence
in `Type \%{name} to insert the name` would look exactly like the text around it.

`message-format.css` colors escape sequences with the color of the surrounding string at 70% opacity. The hue stays
the same, so the result fits every light and dark theme. The stylesheet is meant to be included with every theme.

```html title="Including the base stylesheet"
<link rel="stylesheet" href="/css/default.min.css">
<link rel="stylesheet" href="/css/message-format.css">
<!-- escape sequences appear in a lighter shade of the text color -->
```

The custom property `--hljs-mf-escape-color` replaces the computed shade with a fixed color.

```css title="site.css"
.hljs {
  --hljs-mf-escape-color: #b45309;
}
/* escape sequences appear in amber in all message blocks */
```

The stylesheet relies on the CSS function `color-mix()`. A browser without support for it treats the declaration as
invalid, and escape sequences then take the color of the surrounding string.


### Part Delimiters

The part delimiters `%{`, `%[` and `%(` and their closing counterparts have the scope `template-tag`. highlight.js
defines this scope for the tags of template languages, such as `{%` in Django or `{{` in Handlebars. Part delimiters
serve the same purpose. Because the text around them is a `string`, they must look different from strings. Some
themes do not color `template-tag` at all, and others give it exactly the color of strings. With the `default`
theme, the delimiters of `Hello %{user}!` have the same color as `Hello` and `!`.

For these themes, `message-format-delimiters.css` provides a fallback. It renders delimiters in bold and in the color
they inherit from the surrounding part. Most themes color the scope `subst` of the part, and the delimiters then take
that color. Otherwise, they take the text color of the code block.

```html title="Including the delimiter stylesheet"
<link rel="stylesheet" href="/css/default.min.css">
<link rel="stylesheet" href="/css/message-format.css">
<link rel="stylesheet" href="/css/message-format-delimiters.css">
<!-- delimiters are bold and no longer share the string color -->
```

Two custom properties adjust the fallback. `--hljs-mf-template-tag-color` sets the color, and
`--hljs-mf-template-tag-weight` sets the font weight, which defaults to `bold`.

```css title="site.css"
.hljs {
  --hljs-mf-template-tag-color: #4078f2;
  --hljs-mf-template-tag-weight: normal;
}
/* delimiters appear in blue with a regular font weight */
```

/// warning | Themes That Already Color Delimiters
The delimiter stylesheet replaces the delimiter color of every theme, including themes that already give
`template-tag` a distinct color. It should only be included for the themes listed below. A site that lets readers
switch themes should only load it while one of these themes is active.
///

/// tip | Choose a Delimiter Color for monokai
In `monokai`, the scope `subst` has the same color as strings. With the delimiter stylesheet alone, delimiters only
differ from text by their bold font. Setting `--hljs-mf-template-tag-color` to a color of the theme, such as its
keyword color `#f92672`, gives them a color of their own.
///


### Themes That Need the Delimiter Stylesheet

The following 52 themes of highlight.js 11.12.0 either leave `template-tag` unstyled or give it the same color as
`string`. The other 206 themes give `template-tag` a color of its own and need no fallback.

| Theme                      | `template-tag`             | `string`  |
|----------------------------|----------------------------|-----------|
| `1c-light`                 | same as string             | `#000000` |
| `a11y-dark`                | not styled                 | `#abe338` |
| `a11y-light`               | not styled                 | `#008000` |
| `an-old-hope`              | not styled                 | `#4fb4d7` |
| `androidstudio`            | not styled                 | `#6a8759` |
| `arduino-light`            | same as string             | `#005c5f` |
| `ascetic`                  | not styled                 | `#888`    |
| `atom-one-dark-reasonable` | not styled                 | `#98c379` |
| `atom-one-dark`            | not styled                 | `#98c379` |
| `atom-one-light`           | not styled                 | `#50a14f` |
| `base16/green-screen`      | same as string             | `#00bb00` |
| `brown-paper`              | same as string             | `#2c009f` |
| `codepen-embed`            | not styled                 | `#8f9c6c` |
| `color-brewer`             | same as string             | `#756bb1` |
| `dark`                     | same as string             | `#d88`    |
| `default`                  | same as string             | `#880000` |
| `docco`                    | not styled                 | `#219161` |
| `equinox`                  | not styled                 | `color-mix(in srgb, currentcolor, orange)` |
| `far`                      | same as string             | `#ff0`    |
| `felipec`                  | not styled                 | `#93e4a4` |
| `foundation`               | not styled                 | `#d14`    |
| `googlecode`               | not styled                 | `#080`    |
| `grayscale`                | not styled                 | `#333`    |
| `idea`                     | not styled                 | `#008000` |
| `ir-black`                 | not styled                 | `#a8ff60` |
| `kimbie-dark`              | not styled                 | `#889b4a` |
| `kimbie-light`             | not styled                 | `#889b4a` |
| `lightfair`                | same as string             | `#4286f4` |
| `lioshi`                   | not styled                 | `#b5bd68` |
| `magula`                   | same as string             | `#050`    |
| `mono-blue`                | not styled                 | `#0048ab` |
| `monokai-sublime`          | not styled                 | `#e6db74` |
| `monokai`                  | same as string             | `#a6e22e` |
| `nnfx-dark`                | not styled                 | `#bce`    |
| `nnfx-light`               | not styled                 | `#00f`    |
| `paraiso-dark`             | not styled                 | `#48b685` |
| `paraiso-light`            | not styled                 | `#48b685` |
| `pojoaque`                 | not styled                 | `#468966` |
| `purebasic`                | not styled                 | `#0080ff` |
| `qtcreator-dark`           | same as string             | `#ff55ff` |
| `qtcreator-light`          | same as string             | `#008000` |
| `rainbow`                  | not styled                 | `#8abeb7` |
| `routeros`                 | same as string             | `#880000` |
| `school-book`              | same as string             | `#2c009f` |
| `shades-of-purple`         | same as string             | `#4cd213` |
| `sunburst`                 | not styled                 | `#65b042` |
| `tomorrow-night-blue`      | not styled                 | `#d1f1a9` |
| `tomorrow-night-bright`    | not styled                 | `#b9ca4a` |
| `vs`                       | same as string             | `#a31515` |
| `vs-dark`                  | same as string             | `#ce9178` |
| `xcode`                    | not styled                 | `#c41a16` |
| `xt256`                    | not styled                 | `#00ff00` |


## Custom Styles

The CSS classes of the language can be styled directly as well. A selector that starts with
`.language-message-format` limits a rule to message blocks and takes precedence over the theme.

Quotes have the scope `punctuation.quote`. Most themes color `punctuation`, and the quotes then share the color of
`,` and `:`. In themes that do not color `punctuation`, quotes take the color of the surrounding part. In a few
themes, such as `monokai`, that color equals the string color. A rule for the class combination
`hljs-punctuation quote_` gives quotes a color of their own.

```css title="site.css"
.language-message-format .hljs-punctuation.quote_ {
  color: #6a737d;
}
/* quotes appear in gray, independent of other punctuation */
```

Syntax errors marked with the option [`markErrors`](integration.md#options) have the scope `deletion`. Themes
usually style it for removed lines in diffs, often with a background color. For an editor preview, an underline may
be more suitable, because it does not hide the color of the marked token.

```css title="site.css"
.language-message-format .hljs-deletion {
  background: none;
  text-decoration: underline wavy #d73a49;
}
/* syntax errors are underlined with a red wavy line */
```

Parts inside quoted messages are `subst` scopes nested in other `subst` scopes, as shown in [Nesting](#nesting).
A descendant selector reaches them without affecting top-level parts.

```css title="site.css"
.language-message-format .hljs-subst .hljs-subst {
  background: rgba(127, 127, 127, 0.12);
  border-radius: 3px;
}
/* nested parts get a light background, top-level parts do not */
```
