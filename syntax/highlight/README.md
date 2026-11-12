# highlight.js language for message-format

A [highlight.js](https://highlightjs.org/) language definition for message-format messages. Tokenization
is done by the real grammar (`MessageLexer.g4` / `MessageParser.g4` in `message-format/src/main/antlr/`)
compiled with the ANTLR JavaScript target and fed into highlight.js through its (beta) `__emitTokens`
hook. That way the highlighting follows the Java parser, including context-dependent cases such as
keywords used as names.

Requires **highlight.js 11.9 or later** (the `__emitTokens` hook exists since 11.8, but 11.8 isn't tested). Auto-detection isn't supported, so always specify the language
(`language-message-format`, or one of the aliases `msgfmt` and `message-format`).

How it works internally (grammar preparation, scope classification, nesting, error handling) is
described in [DESIGN.md](DESIGN.md).

## Distributables

`npm run build` writes these files to `dist/`. highlight.js itself is never included.

| File                     | Format | ANTLR runtime  | Use                                                    |
|--------------------------|--------|----------------|--------------------------------------------------------|
| `message-format.mjs`     | ESM    | bundled        | `import` in a bundler or `<script type="module">`      |
| `message-format.min.js`  | IIFE   | bundled        | plain `<script>` tag, registers itself with `hljs`     |
| `message-format.lib.mjs` | ESM    | external       | bundlers/import maps that provide `antlr4@4.13.2`      |
| `message-format.css`     | CSS    | –              | base stylesheet (escape sequences), see [Themes](#themes) |
| `message-format-delimiters.css` | CSS | –          | optional delimiter fallback, see [Themes](#themes)     |

All three variants are minified and come with source maps. The bundled variants are about 190 KB
(about 50 KB gzipped), mostly the ANTLR runtime and the serialized grammar; `message-format.lib.mjs`
is about 76 KB (about 19 KB gzipped) plus the separately loaded runtime.

The package isn't published to npm. A web project can use the files from `dist/` directly, or install
the package locally with `npm install path/to/syntax/highlight` (after `npm run build`), or from a
tarball made with `npm pack`. The package exports:

| Import path                            | File                     |
|----------------------------------------|--------------------------|
| `highlightjs-message-format`           | `message-format.mjs`     |
| `highlightjs-message-format/lib`       | `message-format.lib.mjs` |
| `highlightjs-message-format/script`    | `message-format.min.js`  |
| `highlightjs-message-format/css`       | `message-format.css`     |
| `highlightjs-message-format/css/delimiters` | `message-format-delimiters.css` |

## Usage

### `<script>` tag

```html
<script src="https://cdn.jsdelivr.net/npm/@highlightjs/cdn-assets@11.12.0/highlight.min.js"></script>
<link rel="stylesheet" href="message-format.css">
<script src="message-format.min.js"></script>  <!-- registers 'message-format' with the global hljs -->
<script>hljs.highlightAll();</script>

<pre><code class="language-message-format">Hello %{name}!</code></pre>
```

The script also sets `window.hljsMessageFormat` (the language) and `window.hljsMessageFormat.create`
(see [Options](#options)). If it's loaded before highlight.js, register it yourself with
`hljs.registerLanguage('message-format', hljsMessageFormat)`.

### ES module

```js
import hljs from 'highlight.js/lib/core';
import messageFormat from 'highlightjs-message-format';   // or './message-format.mjs'

hljs.registerLanguage('message-format', messageFormat);
```

### ES module with an external ANTLR runtime

`message-format.lib.mjs` imports `antlr4` instead of bundling it. The version must be exactly
**4.13.2**, the version the parser was generated with. A bundler resolves it from `node_modules`;
without a bundler, use an import map:

```html
<script type="importmap">
  { "imports": { "antlr4": "https://cdn.jsdelivr.net/npm/antlr4@4.13.2/dist/antlr4.web.mjs" } }
</script>
<script type="module">
  import hljs from 'https://cdn.jsdelivr.net/npm/@highlightjs/cdn-assets@11.12.0/es/core.min.js';
  import messageFormat from './message-format.lib.mjs';

  hljs.registerLanguage('message-format', messageFormat);
  hljs.highlightAll();
</script>
```

### Highlighting strings

`hljs.highlightElement()` adds the `language-message-format` class to the element itself. When you use
`hljs.highlight()` to get an HTML string, wrap the result yourself, so the message-format stylesheets apply:

```js
const html = hljs.highlight(message, { language: 'message-format' }).value;
element.innerHTML = `<code class="hljs language-message-format">${html}</code>`;
```

### Options

`createMessageFormat(options)` (ESM export; `hljsMessageFormat.create` in the script variant) returns
a language definition with options:

| Option       | Default | Description                                                        |
|--------------|---------|--------------------------------------------------------------------|
| `markErrors` | `false` | Wrap syntax errors in an additional `deletion` scope (usually red) |

```js
import { createMessageFormat } from 'highlightjs-message-format';

hljs.registerLanguage('message-format', createMessageFormat({ markErrors: true }));
```

Invalid or incomplete messages never throw. Valid parts are still classified by the parser, and
everything else is highlighted by token type.

## Scopes

| Element                                                     | Scope            | CSS class                |
|-------------------------------------------------------------|------------------|--------------------------|
| message text (top level and quoted), unquoted values      | `string`         | `hljs-string`            |
| quotes `'` `"` around quoted messages and strings           | `punctuation.quote` | `hljs-punctuation quote_` |
| escape sequences (`\u0041`, `\x41`, `\'`, `\"`, `\%`, `\{`, `\\`, `\[`) | `char.escape` | `hljs-char escape_` |
| a whole `%{…}`, `%[…]` or `%(…)` part                       | `subst`          | `hljs-subst`             |
| delimiters `%{` `}` `%[` `]` `%(` `)`                       | `template-tag`   | `hljs-template-tag`      |
| `,` `:` `(` `)`                                             | `punctuation`    | `hljs-punctuation`       |
| parameter name, template parameter names                    | `variable`       | `hljs-variable`          |
| `format` keyword                                            | `keyword`        | `hljs-keyword`           |
| format name, post-format name                               | `title.function` | `hljs-title function_`   |
| template name                                               | `title.class`    | `hljs-title class_`      |
| config key (`size` in `size:3`)                             | `attr`           | `hljs-attr`              |
| `true`, `false`, `null`, `empty` as values or map keys      | `literal`        | `hljs-literal`           |
| numbers                                                     | `number`         | `hljs-number`            |
| `=` `!` `<>` `<` `<=` `>` `>=` `->`                         | `operator`       | `hljs-operator`          |
| syntax errors (only with `markErrors`)                      | `deletion`       | `hljs-deletion`          |

Parts nest: a `subst` part contains the scopes of its contents, including quoted messages whose
text is again split into `string` runs and nested `subst` parts. The quotes themselves aren't part
of the string. Most themes colour `punctuation`, so quotes look like `,` and `:`; in the other themes
they inherit the colour of the enclosing part (`subst`). Style `.hljs-punctuation.quote_` to give them
a colour of their own.

## Themes

Two small stylesheets complement the highlight.js theme. Both only affect message-format blocks
(`.language-message-format`) and win over the theme regardless of stylesheet order.

### `message-format.css`: escape sequences (include with every theme)

Escape sequences use the `char.escape` scope, but only 12 of the 258 highlight.js themes style it, so
escapes would look exactly like the surrounding text. `message-format.css` draws them in a slightly
lighter shade of the string colour (70% opacity). That keeps the hue, so it works the same with every
light or dark theme. Requires `color-mix()` support (all current browsers). Customise it with:

```css
.hljs {
  --hljs-mf-escape-color: #b45309;
}
```

### `message-format-delimiters.css`: delimiter fallback (only for some themes)

The delimiters use the `template-tag` scope, which highlight.js defines for the tags of template
languages (`{%` in Django, `{{` in Handlebars). Since message text is a `string`, delimiters must look
different from strings. Some themes don't style `template-tag`, or give it the same colour as `string`.
For these themes, also include the delimiter fallback stylesheet:

```html
<link rel="stylesheet" href="message-format-delimiters.css">
```

By default it renders delimiters in bold, in the colour inherited from the enclosing part (`subst`),
which most themes colour; otherwise in the block's text colour. In `monokai`, `subst` has the string
colour, so set `--hljs-mf-template-tag-color` there. Customise it with CSS properties on
`.hljs` or any ancestor:

```css
.hljs {
  --hljs-mf-template-tag-color: #4078f2;  /* e.g. for atom-one-light */
  --hljs-mf-template-tag-weight: normal;
}
```

Themes that need the delimiter fallback CSS (highlight.js 11.12.0, generated with `npm run check-themes`; the
other 206 themes give `template-tag` a colour of its own):

| Theme | `template-tag` | `string` |
|---|---|---|
| `1c-light` | same as string (#000000) | #000000 |
| `a11y-dark` | not styled | #abe338 |
| `a11y-light` | not styled | #008000 |
| `an-old-hope` | not styled | #4fb4d7 |
| `androidstudio` | not styled | #6a8759 |
| `arduino-light` | same as string (#005c5f) | #005c5f |
| `ascetic` | not styled | #888 |
| `atom-one-dark-reasonable` | not styled | #98c379 |
| `atom-one-dark` | not styled | #98c379 |
| `atom-one-light` | not styled | #50a14f |
| `base16/green-screen` | same as string (#00bb00) | #00bb00 |
| `brown-paper` | same as string (#2c009f) | #2c009f |
| `codepen-embed` | not styled | #8f9c6c |
| `color-brewer` | same as string (#756bb1) | #756bb1 |
| `dark` | same as string (#d88) | #d88 |
| `default` | same as string (#880000) | #880000 |
| `docco` | not styled | #219161 |
| `equinox` | not styled | color-mix(in srgb, currentcolor, orange) |
| `far` | same as string (#ff0) | #ff0 |
| `felipec` | not styled | #93e4a4 |
| `foundation` | not styled | #d14 |
| `googlecode` | not styled | #080 |
| `grayscale` | not styled | #333 |
| `idea` | not styled | #008000 |
| `ir-black` | not styled | #a8ff60 |
| `kimbie-dark` | not styled | #889b4a |
| `kimbie-light` | not styled | #889b4a |
| `lightfair` | same as string (#4286f4) | #4286f4 |
| `lioshi` | not styled | #b5bd68 |
| `magula` | same as string (#050) | #050 |
| `mono-blue` | not styled | #0048ab |
| `monokai-sublime` | not styled | #e6db74 |
| `monokai` | same as string (#a6e22e) | #a6e22e |
| `nnfx-dark` | not styled | #bce |
| `nnfx-light` | not styled | #00f |
| `paraiso-dark` | not styled | #48b685 |
| `paraiso-light` | not styled | #48b685 |
| `pojoaque` | not styled | #468966 |
| `purebasic` | not styled | #0080ff |
| `qtcreator-dark` | same as string (#ff55ff) | #ff55ff |
| `qtcreator-light` | same as string (#008000) | #008000 |
| `rainbow` | not styled | #8abeb7 |
| `routeros` | same as string (#880000) | #880000 |
| `school-book` | same as string (#2c009f) | #2c009f |
| `shades-of-purple` | same as string (#4cd213) | #4cd213 |
| `sunburst` | not styled | #65b042 |
| `tomorrow-night-blue` | not styled | #d1f1a9 |
| `tomorrow-night-bright` | not styled | #b9ca4a |
| `vs-dark` | same as string (#ce9178) | #ce9178 |
| `vs` | same as string (#a31515) | #a31515 |
| `xcode` | not styled | #c41a16 |
| `xt256` | not styled | #00ff00 |

## Building and testing

Requirements: Node.js 20+, and Java 11+ to run the ANTLR tool.

```sh
npm install
npm run build        # check version, prepare grammar, generate parser, bundle into dist/
npm test             # build + run all tests
npm run test:update  # build + regenerate test snapshots
npm run check-themes # list themes that need the delimiter fallback CSS
npm run clean        # remove build/, dist/ and src/generated/
npm run check-version # fail if the package version differs from libs.versions.toml
```

How the build works:

1. `scripts/prepare-grammar.mjs` reads the original grammars from `message-format/src/main/antlr/`
   and writes cleaned copies to `build/grammar/`, removing the Java-specific `@header`, `returns […]`
   and `language = Java`. **The original `*.g4` files are never modified.** The script fails if the
   grammar gets other target-specific code.
2. `scripts/generate.mjs` runs the ANTLR 4.13.2 tool with the JavaScript target into
   `src/generated/`. The tool comes from `$ANTLR_CLASSPATH` (set by the Gradle build), from
   `$ANTLR_JAR`, or is downloaded once into `build/`.
3. `scripts/build.mjs` bundles `src/` with esbuild into `dist/`.

### Gradle

The highlighter is the Gradle subproject `:highlight` (build file `highlight.gradle`), so it is part of the
normal build. Node.js and npm must be on the `PATH`.

| Task                  | Does                                                      | Hooked into |
|-----------------------|-----------------------------------------------------------|-------------|
| `highlightSyncVersion`| sets the package version to the library version           | –           |
| `highlightNpmInstall` | `npm ci`                                                  | –           |
| `highlightBuild`      | `npm run build`, with the ANTLR tool resolved by Gradle    | `assemble`  |
| `highlightTest`       | `node --test test/`                                       | `check`     |
| `highlightClean`      | deletes `build/`, `dist/` and `src/generated/`            | `clean`     |

The package version follows the library version (`message-format` in `gradle/libs.versions.toml`).
`highlightSyncVersion` runs before `highlightNpmInstall` and updates `package.json` and
`package-lock.json` with `npm version` when the versions differ; commit the result together with the
version change. A plain npm build doesn't sync the version, but `npm run build` (and therefore
`npm test`) fails with a hint when the versions differ (`npm run check-version`).

The tasks declare their inputs and outputs, so they're skipped when nothing changed (including
changes to the grammars and the Java test resources used by the snapshot tests).

The tests (`node --test`) check:

- that the highlighted text always reads back exactly as the input,
- the scopes of each construct,
- invalid and incomplete input,
- snapshots of the messages in the Java test resources and `test/fixtures/*.msg`,
- the files in `dist/`.

`demo/index.html` (script variant) and `demo/esm.html` (ESM variant) show sample messages with a theme
switcher and toggles for both stylesheets. Serve the directory over HTTP, e.g. `python3 -m http.server` in
`syntax/highlight`, and open `http://localhost:8000/demo/`.
