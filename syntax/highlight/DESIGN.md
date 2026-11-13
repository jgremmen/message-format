# Design: highlight.js language for message-format (ANTLR-driven)

The highlight.js language `message-format` tokenizes with the real grammar (`MessageLexer.g4` /
`MessageParser.g4`), compiled with the ANTLR JavaScript target and fed into highlight.js through the
beta `__emitTokens` hook. This document explains how it works and why. Usage is in the
[README](README.md).

## 1. How `__emitTokens` works (constraints that drive the design)

In `src/highlight.js` (`_highlight`), if the language object has `__emitTokens`, the regex mode
engine is skipped completely:

```js
if (!language.__emitTokens) { /* regex engine */ }
else { language.__emitTokens(codeToHighlight, emitter); }
emitter.finalize();
result = emitter.toHTML();
```

Consequences:

- The language drives the `TokenTreeEmitter` itself through `emitter.startScope(scope)`,
  `emitter.addText(text)` and `emitter.endScope()`. Scopes may be nested. `addText` HTML-escapes, so
  raw text is passed.
- **Every character of the input must be handed to `addText`, in order.** Anything the lexer skips
  (control chars and whitespace, characters it rejects) has to be re-inserted as plain text, or it
  disappears from the output.
- `relevance` is always 0 on this path, so auto-detection can't work. The language sets
  `disableAutodetect: true`.
- If `__emitTokens` throws, highlight.js (in safe mode) falls back to escaped plain text. Errors are
  still caught by the language itself so that partial output isn't lost (see §5).
- `__emitTokens` was added in highlight.js 11.8.0. The declared range is **^11.9.0**, recorded as
  a peer dependency; 11.8 isn't tested, and a new major version may change the hook. The dev dependency (11.12.0) is pinned.

## 2. Directory layout (`syntax/highlight/`)

```
syntax/highlight/
  DESIGN.md                   (this file)
  README.md                   usage, scopes, build instructions
  package.json                private (unpublished) package, build scripts
  highlight.gradle            build file of the Gradle subproject :highlight (§3.2)
  scripts/
    prepare-grammar.mjs       creates JS-target-safe copies of the .g4 files
    generate.mjs              runs the ANTLR tool (JavaScript target)
    build.mjs                 esbuild: the three dist/ variants + CSS files (§6)
    check-themes.mjs          lists hljs themes that need the delimiter fallback CSS (§4.4)
  src/
    index.js                  hljs language factory: (hljs) => ({ name, __emitTokens, ... })
    browser.js                entry point of the IIFE build (registers with a global hljs)
    emit.js                   token stream + parse tree -> emitter calls
    scopes.js                 token/context -> hljs scope mapping
    message-format.css        base stylesheet: escape sequences (§4.4)
    message-format-delimiters.css  optional delimiter fallback stylesheet (§4.4)
    generated/                ANTLR output (git-ignored, regenerated on build)
  test/
    *.test.js                 node:test based tests
    fixtures/*.msg            additional sample messages
    snapshots/*.json          expected HTML per corpus
  demo/                       manual test pages (IIFE + ESM)
  dist/                       bundled outputs (git-ignored)
```

The grammars stay where they are (`message-format/src/main/antlr/`). They are read from there and
**not** copied or forked by hand, so the highlighter always follows the Java implementation.

## 3. Generating the JavaScript lexer/parser

### 3.1 Grammar preparation

`MessageLexer.g4` is target-neutral and is used as-is.

`MessageParser.g4` contains Java-specific parts that break the JS target:

- `options { language = Java; ... }`: could be overridden with `-Dlanguage=JavaScript`, but stripping
  it is cleaner.
- `@header { import de.sayayi...; }`: copied verbatim into the generated JS, which is a syntax
  error. **Must be removed.**
- `returns [Message.WithSpaces messageWithSpaces]`, `returns [Map<String,TypedValue<?>> config, ...]`:
  Java types in the return declarations. These aren't needed for highlighting and are removed to
  avoid target-specific type parsing problems with generics.

`scripts/prepare-grammar.mjs` reads both grammars and writes cleaned copies to `build/grammar/`:

- removes the `@header { ... }` block (brace-balanced scan, not a regex),
- removes `returns [ ... ]` clauses (bracket-balanced scan),
- removes `language = Java;` from `options`, keeping `tokenVocab = MessageLexer;`,
- leaves rules, alternatives and `#labels` untouched, so rule/context names stay identical to the Java
  side.

The script fails loudly if it finds any other `{...}` action or `@members` block, because that would
mean new Java code was added to the grammar and needs a decision.

**The original `*.g4` files must never be modified.** Preprocessing is the only supported way to get
JS-target grammars. The script only reads the originals and writes its output under `build/grammar/`,
which is git-ignored.

### 3.2 Running the tool

- The ANTLR tool version must match the npm runtime exactly. The Gradle catalog uses
  `antlr4 = '[4.13.0,4.14)'`, so **4.13.2** is used for both the tool and the `antlr4` npm package.
- `scripts/generate.mjs` takes the tool from `$ANTLR_CLASSPATH` or `$ANTLR_JAR` if set, and otherwise
  downloads (and caches in `build/`) `antlr-4.13.2-complete.jar`. It then runs the equivalent of:

  ```
  java -jar antlr-4.13.2-complete.jar -Dlanguage=JavaScript -Xexact-output-dir \
       -no-listener -no-visitor -o src/generated build/grammar/MessageLexer.g4 build/grammar/MessageParser.g4
  ```

- npm scripts: `prepare-grammar`, `generate` (runs both), `build`, `test`, `clean`. `build` and
  `test` depend on `generate`.
- Gradle: this directory is the subproject `:highlight` (build file `highlight.gradle`), which registers
  `highlightSyncVersion` (sets the package version to the library version from
  `gradle/libs.versions.toml`), `highlightNpmInstall`, `highlightBuild` (→ `assemble`), `highlightTest` (→ `check`) and
  `highlightClean` (→ `clean`). Gradle resolves `org.antlr:antlr4` in the exact version of the npm
  runtime from Maven Central and passes it to `generate.mjs` as `$ANTLR_CLASSPATH`, so no separate
  download is needed. As it publishes nothing, the root build leaves it out of
  `projectLocalRepositories`.

## 4. Turning tokens into highlight.js scopes

### 4.1 Why the parser is needed, not just the lexer

The lexer alone can't tell what a `NAME` means. The same token type is a parameter name, a template
name, a post-format name, a config key, a format name, a template parameter, or an unquoted string
value (`simpleString`). The keywords `true`, `false`, `null`, `empty` and `format` can also be used as
names (`nameOrKeyword`). The parse tree resolves all of these, so the highlighter does
**lex → parse → classify terminals by rule context → emit**.

### 4.2 Classification

`scopes.js` walks the parse tree once and builds `Map<tokenIndex, scope>`. A terminal is classified
by its token type and its parent rule context (looking through `nameOrKeyword` and `simpleString` to
the enclosing rule):

| Grammar context                                                   | hljs scope        |
|-------------------------------------------------------------------|-------------------|
| `P_START`, `P_END`, `TPL_START`, `TPL_END`, `PF_START`, `PF_END`  | `template-tag`    |
| `COMMA`, `COLON`, `L_PAREN`, `R_PAREN`                            | `punctuation`     |
| `parameterName` → `nameOrKeyword`                                 | `variable`        |
| `parameterFormat`: `FORMAT`                                       | `keyword`         |
| `parameterFormat`: `nameOrKeyword`                                | `title.function`  |
| `templateName` → `nameOrKeyword`                                  | `title.class`     |
| `postFormatName` → `nameOrKeyword`                                | `title.function`  |
| `templateParameterDelegate`: both `nameOrKeyword`, `ARROW`        | `variable`, `operator` |
| `templateParameterDefault*`: `nameOrKeyword`, `EQ`                | `variable`, `operator` |
| `configDefinition*`: `NAME`                                       | `attr`            |
| `simpleString` → `nameOrKeyword` (unquoted value)                 | `string`          |
| `BOOL`, `NULL`, `EMPTY` as values or map keys                     | `literal`         |
| `NUMBER`                                                          | `number`          |
| `EQ`, `NE`, `LT`, `LTE`, `GT`, `GTE` in `mapKey`                  | `operator`        |
| `SQ_START`, `SQ_END`, `DQ_START`, `DQ_END`                        | `punctuation.quote` |
| `text` inside quotes                                              | `string`          |
| `CH` whose text starts with `\` (`EscapeSequence`)                | `char.escape`     |
| `CH` at top level (`message` → `message0` → `text`)               | `string`          |

Context always takes priority over token type. For example, `true` in `%{p,format:true}` is
`title.function`, not `literal`.

Terminals that aren't reached through a valid rule (error nodes, tokens consumed by recovery) fall
back to a classification **by lexer token type only** (`BOOL`→`literal`, `NUMBER`→`number`,
`NAME`→`variable`, delimiters→`template-tag`, …). That way invalid input still gets sensible
highlighting.

The tree walk uses duck typing (`node.symbol`, `node.isErrorNode?.()`) because the `antlr4` package
doesn't export `ErrorNode`.

### 4.3 Nesting

Nested scopes are useful for themes and for click-through / hover in a docs site:

- Top-level message text is a string: each run of top-level `CH` tokens (`textPart` under the root
  `message0`) is emitted inside one `string` scope, with `char.escape` nested for escape sequences.
  The scope closes before a `%{`/`%[`/`%(` part and reopens after it.
- Quoted text works the same way: each `text` run inside a quoted message/string is one `string`
  scope. The quotes are not part of the string. They delimit it, like `%{` and `}` delimit a part,
  so they get their own scope `punctuation.quote` (classes `hljs-punctuation quote_`). Themes colour
  it like `punctuation`, which 190 of the 258 themes style. In the other themes, quotes inherit the
  colour of the enclosing part (`subst`) or the block's text colour. Either way they differ from the
  string in almost all themes; `monokai` is an exception where `subst` has the string colour. A page
  can style `.hljs-punctuation.quote_` to give quotes a colour of their own.
- A parameter/template/post-format part opens a `subst` scope from its start delimiter to its end
  delimiter. Inside a quoted message, this makes nested `%{...}` stand out from the surrounding
  string text, the same way string interpolation does in other hljs languages.

Implementation: besides the per-token scope map, `open`/`close` events are recorded at the start/stop
token index of every `text`, `parameterPart`, `templatePart` and `postFormatPart` context. The emitter goes through tokens in order and applies the events around them.
Unbalanced contexts from error recovery (a missing stop token) are closed at the end of the input.

### 4.4 Delimiter scope `template-tag` and the CSS fallback

The part delimiters `%{` `}` `%[` `]` `%(` `)` use the hljs scope `template-tag` (CSS class
`hljs-template-tag`). hljs defines it as the tag of a template language (Django/Jinja `{%`,
Handlebars `{{`), which matches what these delimiters are.

Problem: themes style `template-tag` inconsistently. Because top-level text is a `string`, a
delimiter must look different from string text. Of the 258 highlight.js 11.12.0 themes, 52 leave
`template-tag` unstyled or give it the same colour as `string`, for example:

| Theme                            | `template-tag` vs. `string`                 |
|----------------------------------|---------------------------------------------|
| `default`, `monokai`             | same colour as `string`: delimiters vanish  |
| `atom-one-dark`, `atom-one-light`| not styled (inherits the `subst` colour)    |
| `github`, `github-dark`, `nord`, `stackoverflow-light`, `tokyo-night-dark`, `vs2015` | distinct colour, fine |

Solution: an optional stylesheet, `dist/message-format-delimiters.css` (source
`src/message-format-delimiters.css`, copied by `scripts/build.mjs`). Web projects include it only
when their theme needs it:

```css
/* Fallback for themes where .hljs-template-tag is missing or equals .hljs-string */
.language-message-format .hljs-template-tag {
  color: var(--hljs-mf-template-tag-color, inherit);
  font-weight: var(--hljs-mf-template-tag-weight, bold);
}
```

- The selector has two classes, so it beats the theme's single-class `.hljs-template-tag` no matter
  which stylesheet loads first. It only applies inside message-format blocks, so other languages on
  the page keep the theme's styling.
- `hljs.highlightElement()` adds `language-message-format` to the `<code>` element automatically.
  For `hljs.highlight()` (HTML string output), the README shows wrapping the result in
  `<code class="hljs language-message-format">`.
- By default, delimiters inherit the colour of the enclosing part plus bold. The part is a `subst`
  scope (§4.3), which 233 of the 258 themes colour; in the others, the delimiters get the block's
  text colour. That works with both light and dark themes, because it never hard-codes a colour.
  In a few themes, such as `monokai`, `subst` has the string colour, so only the bold weight sets
  the delimiters apart; there a theme colour should be set through the custom property. Projects can set
  `--hljs-mf-template-tag-color` / `--hljs-mf-template-tag-weight` on `.hljs` (or any ancestor) to
  match their theme, e.g. `--hljs-mf-template-tag-color: #4078f2` for Atom One Light.
- The fallback also covers `template-tag` spans from error recovery (§4.2), so all delimiters look
  the same.

`scripts/check-themes.mjs` (run manually, not part of `build`) goes through every theme in
`node_modules/highlight.js/styles/` and lists those where `.hljs-template-tag` is unstyled or has the
same colour as `.hljs-string`. Its output is the "Themes that need the fallback CSS" table in the
README, so the list can be kept accurate when highlight.js is upgraded.

#### Escape sequences: `message-format.css`

Escape sequences use the `char.escape` scope (classes `hljs-char escape_`). Only 12 of the 258
highlight.js 11.12.0 themes style it differently from `string`, so escapes would look like the
surrounding text. No other hljs scope works as a stand-in: scopes such as `symbol` or `meta` get
unrelated colours, which sometimes clash with the delimiters. A theme-independent base stylesheet
`dist/message-format.css` (source `src/message-format.css`), meant to be included with every theme,
draws escapes in a slightly lighter shade of the string colour:

```css
.language-message-format .hljs-char.escape_ {
  color: var(--hljs-mf-escape-color, color-mix(in srgb, currentColor 70%, transparent));
}
```

Escapes are always emitted inside a `string` scope (§4.3), so `currentColor` is the theme's string
colour. This keeps the hue in light and dark themes. `--hljs-mf-escape-color` overrides it.

The delimiter fallback is in a separate file because it's needed for only some themes. Including it
with every theme would replace the delimiter colours of themes that already style `template-tag`.

### 4.5 Emitting and gap filling

- Lexing uses `CharStreams.fromString(code)`. This stream works in **code points**, so
  `token.start`/`token.stop` are code point indices, not UTF-16 offsets. They're converted through a
  code-point → UTF-16 offset table built once per input. Without this, names or text with
  supplementary characters (emoji, `\p{L}` outside the BMP) would shift every following token.
- All tokens are taken from the `CommonTokenStream` after parsing (`stream.fill()`, `stream.tokens`),
  skipping `EOF`.
- A cursor tracks the emitted position. For each token, any input between the cursor and
  `token.start` (skipped `CtrlChar`/`P_WS`/`T_WS`/`PF_WS`, and characters rejected by the lexer) is
  emitted with `addText` and no scope. Then the token is emitted with its scope.
- Adjacent `CH` tokens with the same scope are merged into one `addText` call. Each `CH` token is a
  single character, so this keeps the HTML output small.
- After the last token, the rest of the input is emitted as plain text.
- Invariant (asserted in tests): concatenating everything passed to `addText` gives back exactly
  `code`.

## 5. Error handling

- The default `ConsoleErrorListener` is removed from both lexer and parser
  (`removeErrorListeners()`). Nothing is logged to the console while highlighting.
- The default recovering error strategy (not `BailErrorStrategy`) is kept, so a partial tree is still
  built for invalid or unfinished messages, such as `Hello %{name` while typing.
- An error listener collects syntax errors. With the `markErrors` option, the offending tokens get an
  extra `deletion` scope (a hljs scope that themes usually colour red), which is useful in docs. It's
  off by default.
- The whole function is wrapped in `try/catch`. On an unexpected exception, the not-yet-emitted rest
  of the input is emitted as plain text instead of throwing, so the output is never truncated.
- Extremely deep nesting (somewhere between 500 and 1000 levels) exhausts the parser's stack. That
  exception is caught and highlighting falls back to token-type scopes (§4.2).

## 6. The language module

```js
// src/index.js
export function createMessageFormat(options = {})
{
  return function messageFormat(hljs) {
    return {
      name: 'Message Format',
      aliases: [ 'msgfmt', 'message-format' ],
      disableAutodetect: true,
      contains: [],  // not used, required by the hljs mode compiler
      __emitTokens: (code, emitter) => emitMessageFormat(code, emitter, options)
    };
  };
}

const messageFormat = createMessageFormat();

export default messageFormat;
```

### Packaging

The package isn't published to npm (`"private": true` in `package.json`). `npm run build` writes three
distributables to `dist/` from one esbuild script (`scripts/build.mjs`, three `esbuild.build()`
calls). In all three, highlight.js itself is never bundled; it's provided by the web project.

| File                         | Format | `antlr4` runtime | Intended use                                                                                     |
|------------------------------|--------|------------------|--------------------------------------------------------------------------------------------------|
| `dist/message-format.mjs`    | ESM    | bundled          | `import mf from './message-format.mjs'; hljs.registerLanguage('message-format', mf)`, either through a bundler or directly in `<script type="module">` |
| `dist/message-format.min.js` | IIFE   | bundled          | A plain `<script>` tag after highlight.js. It calls `hljs.registerLanguage('message-format', …)` itself if a global `hljs` exists, and also exposes `window.hljsMessageFormat` |
| `dist/message-format.lib.mjs`| ESM    | external (`import … from 'antlr4'`) | Projects with a bundler (Vite/webpack/Rollup) or an import map that resolve `antlr4` themselves, e.g. to share it with other ANTLR code |

Details:

- All three variants are minified and come with source maps. The `.lib.mjs` variant is minified
  too, because it's also loaded directly through an import map, without a bundler. A bundler
  minifying it again costs nothing.
- For `.lib.mjs`, the README documents the exact runtime version required (`antlr4@4.13.2`) and an
  import map example.
- `package.json` `exports` maps `"."` → `dist/message-format.mjs`, `"./lib"` →
  `dist/message-format.lib.mjs` and `"./script"` → the IIFE. A web project can then use it locally
  without npm publishing, via `npm install ../path/to/syntax/highlight` (a `file:` dependency) or a
  tarball from `npm pack`.
- `dist/` also contains the two stylesheets from §4.4: `message-format.css` (escape sequences, for
  every theme) and `message-format-delimiters.css` (optional delimiter fallback). They work with all
  three variants and are exported as `"./css"` and `"./css/delimiters"`.
- The `.lib.mjs` variant is about 76 KB (about 19 KB gzipped) without the runtime.
- The bundled variants are about 190 KB minified (about 50 KB gzipped), mostly the ANTLR runtime and
  the serialized grammar. That's acceptable for a docs site.
- `demo/index.html` loads highlight.js from a CDN plus `dist/message-format.min.js`, and
  `demo/esm.html` uses the ESM file. A theme switcher and toggles for both stylesheets let you check
  escape and delimiter visibility by hand.

## 7. Tests (`node --test`)

The tests use `highlight.js/lib/core` plus the language, with no DOM required.

1. **Round-trip invariant** (`roundtrip.test.js`): the text content of the HTML output (tags
   stripped, entities decoded) equals the input. Covers control characters, tabs/newlines inside
   parts, emoji in text, and supplementary-plane letters in names.
2. **Scope assertions** (`scopes.test.js`) per construct, checking for specific
   `<span class="hljs-…">x</span>` fragments: top-level text and escape sequences, parameters and
   formats (including keywords as format names), map keys and operators, config values, templates,
   post formats, and balanced `subst`/`string` spans in nested messages.
3. **Invalid/incomplete input** (`errors.test.js`): each input keeps the round-trip invariant, doesn't
   throw and doesn't log to the console. Also covers `markErrors`.
4. **Snapshot tests** (`snapshot.test.js`): the messages in the Java test resources
   (`message.properties`, `template.properties`, read in place so both implementations use the same
   corpus) plus `test/fixtures/*.msg`. The expected HTML is in `test/snapshots/<corpus>.json`.
   Regenerate it with `npm run test:update`.
5. **Distributables** (`dist.test.js`): loads each of the three files (IIFE through `vm` with a fake
   global `hljs`, the ESM files via `import()`), highlights one sample message and checks both
   stylesheets.

## 8. Open items

- Documentation site: the language isn't used by the documentation site yet.
- Optional cross-check: a small Java test that tokenizes the fixtures with the Java lexer and writes
  token type + text, compared with the JS token stream to show both targets lex identically.
