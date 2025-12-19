# Pygments Lexer für MessageFormat - Finaler Plan

## Übersicht

Ein Pygments-Lexer wird erstellt, der Syntax-Highlighting für die MessageFormat-Sprache gemäß MessageParser.g4 und MessageLexer.g4 durchführt. Der Lexer implementiert vollständig rekursives State-Stack-Management mit Latin-Extended-Unicode-Support und folgt der ANTLR-Grammatik so präzise wie möglich.

## Implementierungsschritte

### 1. Erstelle `syntax/pygments/messageformat_lexer.py`

Implementiere `RegexLexer` mit folgenden Eigenschaften:

**Lexer-Metadaten:**
- `name = "Message Format"`
- `aliases = ["msgfmt", "message-format"]`
- `filenames = ["*.mfp"]`

**State-Management:**
- Vollständig rekursiver State-Stack mit States: `root`, `parameter`, `template`, `singlequote`, `doublequote`
- Verwendet `#push` und `#pop` für verschachtelte Strukturen
- Unterstützt beliebige Verschachtelungstiefe (Parameter in Strings, Strings in Parametern, etc.)

**Name-Pattern (gemäß MessageLexer.g4 Zeilen 206-217):**
- NameStartChar: `[a-zA-Z\u00C0-\u024F\u1E00-\u1EFF]`
- NameChar: `[a-zA-Z0-9\u00C0-\u024F\u1E00-\u1EFF_]` (mit Underscore gemäß Zeile 211)
- Vollständiger Name mit Hyphens: `[a-zA-Z\u00C0-\u024F\u1E00-\u1EFF][a-zA-Z0-9\u00C0-\u024F\u1E00-\u1EFF_]*(-[a-zA-Z0-9\u00C0-\u024F\u1E00-\u1EFF_]+)*`

**Character-Fragment (gemäß MessageLexer.g4 Zeilen 218-223):**

Approximation der Unicode-Kategorien mit Latin-Extended-Zeichensätzen:
- Whitespace: `[ \t\u00A0]+` (Unicode Zs approximiert)
- Letters: `[a-zA-Z\u00C0-\u024F\u1E00-\u1EFF]` (Unicode L approximiert)
- Numbers: `[0-9]` (Unicode N approximiert)
- Punctuation: `[!\"#$%&'()*+,\-./:;<=>?@\[\\\]^_\`{|}~¡-¿]` (Unicode P approximiert)
- Symbols: `[×÷±°§¶†‡•‰€£¥]` (Unicode S approximiert)

**Control-Chars (Zeile 203):**
- Pattern: `[\u0000-\u001f]+`
- Token: `Token.Whitespace`

**Escape-Sequenzen (Zeilen 225-227):**
- Unicode-Escape: `\\u[0-9a-fA-F]{4}`
- Character-Escape: `\\["'%{\\[]`
- Token: `Token.String.Escape`

**Whitespace in Parameter/Template-Modes:**
- Gemäß MessageLexer.g4 Zeilen 127 und 185 werden Whitespaces geskippt
- Im Pygments-Lexer: Nicht matchen (ignorieren)

**Number-Pattern (Zeile 220):**
- Pattern: `-?[0-9]+`
- Keine Längenbeschränkung (beliebig lange Integers erlaubt)
- Token: `Number.Integer`

**Token-Mapping:**
- Keywords (`true`, `false`, `null`, `empty`) → `Keyword.Constant`
- Numbers (negative/positive Integers beliebiger Länge) → `Number.Integer`
- Operatoren (`<>`, `!`, `<`, `<=`, `>`, `>=`, `=`) → `Operator`
- Delimiters (`,`, `:`, `(`, `)`) → `Punctuation`
- Parameter-Start/End (`%{`, `}`) und Template-Start/End (`%[`, `]`) → `Punctuation.Special`
- Quote-Zeichen (`'`, `"`) → `String.Delimiter`
- String-Content → `String`
- Names → `Name`
- Regular text (CH) → `Text`

### 2. Erstelle `syntax/pygments/test/test_messageformat_lexer.py`

Pytest-Tests mit gebündelten Test-Cases:

**Test 1: `test_message_properties`**
- Parametrisierter Test mit allen 9 MSG-Beispielen aus `message.properties`
- MSG-001: Leere Message
- MSG-002: Einfacher Text
- MSG-003: Sonderzeichen und Escape-Sequenzen (`\\uD83D`, `\\uDE00`)
- MSG-004: Enum-Keys mit verschiedenen Operatoren (`empty`, `!empty`, `=null`, `!null`, `true`, `false`)
- MSG-005: Named-Keys mit verschachtelten Parametern (`'msg %{m}'`), negative Zahlen (`-678764782`)
- MSG-006: Number-Keys mit relationalen Operatoren (`<10`, `<=20`, `=30`, `>1999`, `>=200`, `!25`)
- MSG-007: String-Keys mit relationalen Operatoren (`<'A'`, `>'Z'`)
- MSG-008: Templates (`%[result,n=count,p:true]`, `%[ex-colon]`)
- MSG-009: Extreme Integer-Werte (`-9223372036854775808`, `9223372036854775807`)

**Test 2: `test_template_properties`**
- Beide Beispiele aus `template.properties`:
  - `ex-colon=%{ex,!empty:': %{ex}'}`
  - `result=%{n,1:'1 result',:'%{n} results'}`

**Test 3: `test_edge_cases`**
- Verschachtelte Strukturen (mehrere Ebenen)
- Alle Escape-Sequenzen (`\\u`, `\\"`, `\\'`, `\\%`, `\\{`, `\\[`, `\\\\`)
- Hyphenated Names (`ex-colon`, `my-name`, `test-123`)
- Alle Operatoren einzeln
- Leere Strings (`''`, `""`)
- Control-Chars
- Namen mit Underscores (`my_name`, `test_123`)

**Assertions:**
- Verwende `list(lex(code, MessageFormatLexer()))` für Token-Sequenzen
- Prüfe Token-Typen und optional Token-Werte
- Beispiel: `assert tokens[0] == (Token.Punctuation.Special, '%{')`

### 3. Erstelle `syntax/pygments/test/Dockerfile`

```dockerfile
FROM python:3.11-slim

# Install dependencies
RUN pip install --no-cache-dir pygments pytest

# Set working directory
WORKDIR /app

# Copy lexer and tests
COPY ../messageformat_lexer.py /app/syntax/pygments/messageformat_lexer.py
COPY test_messageformat_lexer.py /app/syntax/pygments/test/test_messageformat_lexer.py

# Set PYTHONPATH so imports work
ENV PYTHONPATH=/app

# Run tests by default
CMD ["pytest", "-v", "/app/syntax/pygments/test/test_messageformat_lexer.py"]
```

### 4. Erstelle `syntax/pygments/test/run_tests.sh`

Executable Shell-Script (`#!/bin/zsh`, `chmod +x`):

```bash
#!/bin/zsh

# Navigate to project root
cd "$(dirname "$0")/../../.."

# Build Docker image
echo "Building Docker image..."
docker build -t messageformat-lexer-test -f syntax/pygments/test/Dockerfile . || {
    echo "Failed to build Docker image!"
    exit 1
}

# Run tests
echo "Running tests..."
if [ "$1" = "--interactive" ]; then
    docker run --rm -it messageformat-lexer-test /bin/bash
else
    docker run --rm messageformat-lexer-test
    EXIT_CODE=$?
    
    if [ $EXIT_CODE -eq 0 ]; then
        echo "All tests passed!"
    else
        echo "Tests failed!"
    fi
    
    exit $EXIT_CODE
fi
```

### 5. Erstelle `syntax/pygments/README.md`

Dokumentation mit folgenden Sections:

**Requirements:**
- Python 3.7+
- Pygments 2.10+

**Implementation Decisions:**

1. **Vollständig rekursives State-Stack-Management**
   - Implementiert via `#push`/`#pop` für unbegrenzte Verschachtelung
   - Unterstützt Parameter in Strings, Strings in Parametern, Templates in Strings, etc.
   - Mimic des ANTLR `pushMode`/`popMode`-Verhaltens

2. **Unicode-Approximation**
   - ANTLR verwendet `\p{L}`, `\p{N}`, `\p{P}`, `\p{S}`, `\p{Zs}` (Unicode-Kategorien)
   - Python regex unterstützt diese nicht nativ
   - Approximation mit Latin-Extended-Zeichensätzen:
     - `\u00C0-\u024F`: Latin Extended-A und -B
     - `\u1E00-\u1EFF`: Latin Extended Additional
   - Keine Unterstriche in NameStartChar (nur in NameChar)

3. **NameChar inkludiert Underscore**
   - Gemäß MessageLexer.g4 Zeile 211
   - NameStartChar darf KEINEN Underscore enthalten
   - NameChar darf Underscore, Zahlen und Letters enthalten

4. **Hyphenated-Names**
   - Gemäß MessageLexer.g4 Zeile 206
   - Pattern: `NameStartChar NameChar* (-NameChar+)*`
   - Beispiele: `ex-colon`, `my-name-123`

5. **Keine Integer-Längen-Limitierung**
   - Pattern: `-?[0-9]+`
   - Beliebig lange Integers wie `9223372036854775807` werden unterstützt
   - Keine zusätzliche Validierung

6. **Whitespace in Parameter/Template-Modes**
   - ANTLR skippt Whitespaces (Zeilen 127, 185)
   - Pygments-Implementierung: Whitespace wird nicht explizit gematcht (ignoriert)

7. **Control-Chars als Whitespace-Token**
   - ANTLR skippt `[\u0000-\u001f]`
   - Pygments matcht diese als `Token.Whitespace` für bessere Debugging-Sichtbarkeit

8. **Escape-Sequenzen separat hervorgehoben**
   - Unicode-Escape: `\\uXXXX` mit 4 Hex-Digits
   - Character-Escape: `\"`, `\'`, `\%`, `\{`, `\[`, `\\`
   - Token: `Token.String.Escape`

9. **Error-Handling**
   - Best-effort-Matching ohne explizite Error-Tokens
   - Permissives Verhalten wie typisch bei Pygments-Lexern

**Vollständige Token-Mapping-Tabelle:**

| ANTLR Token | Pygments Token | Beispiel |
|-------------|----------------|----------|
| `P_START`, `TPL_START` | `Punctuation.Special` | `%{`, `%[` |
| `P_END`, `TPL_END` | `Punctuation.Special` | `}`, `]` |
| `COMMA` | `Punctuation` | `,` |
| `COLON` | `Punctuation` | `:` |
| `L_PAREN`, `R_PAREN` | `Punctuation` | `(`, `)` |
| `BOOL` | `Keyword.Constant` | `true`, `false` |
| `NULL` | `Keyword.Constant` | `null` |
| `EMPTY` | `Keyword.Constant` | `empty` |
| `NUMBER` | `Number.Integer` | `-678764782` |
| `NAME` | `Name` | `myName`, `ex-colon` |
| `EQ` | `Operator` | `=` |
| `NE` | `Operator` | `<>`, `!` |
| `LT` | `Operator` | `<` |
| `LTE` | `Operator` | `<=` |
| `GT` | `Operator` | `>` |
| `GTE` | `Operator` | `>=` |
| `SQ_START`, `SQ_END` | `String.Delimiter` | `'` |
| `DQ_START`, `DQ_END` | `String.Delimiter` | `"` |
| `CH` (in quotes) | `String` | Text in Strings |
| `CH` (root) | `Text` | Regulärer Text |
| `EscapeSequence` | `String.Escape` | `\\uD83D`, `\\'` |
| `CTRL_CHAR` | `Whitespace` | `\u0000-\u001f` |

**Usage:**

1. **Programmatisch:**

```python
from pygments import highlight
from pygments.formatters import HtmlFormatter
from syntax.pygments.messageformat_lexer import MessageFormatLexer

code = "Hello %{name}!"
result = highlight(code, MessageFormatLexer(), HtmlFormatter())
print(result)
```

2. **CLI mit pygmentize:**

```bash
pygmentize -l msgfmt file.mfp
pygmentize -l message-format file.mfp
```

3. **Installation als Custom-Lexer:**

In `setup.py`:

```python
from setuptools import setup

setup(
    name='messageformat-pygments',
    packages=['syntax.pygments'],
    entry_points={
        'pygments.lexers': [
            'msgfmt = syntax.pygments.messageformat_lexer:MessageFormatLexer',
        ],
    },
)
```

Nach Installation mit `pip install .` ist der Lexer systemweit verfügbar.

4. **Sphinx-Integration:**

In `conf.py`:

```python
from syntax.pygments.messageformat_lexer import MessageFormatLexer
from sphinx.highlighting import lexers

lexers['msgfmt'] = MessageFormatLexer()
```

Verwendung in RST/Markdown:

```rst
.. code-block:: msgfmt

   Hello %{name}!
```

**Testing:**

1. **Lokal mit pytest:**

```bash
cd syntax/pygments
pytest test/test_messageformat_lexer.py -v
```

2. **Mit Docker:**

```bash
./syntax/pygments/test/run_tests.sh
```

3. **Interaktiv (für Debugging):**

```bash
./syntax/pygments/test/run_tests.sh --interactive
```

Dies öffnet eine Shell im Docker-Container, wo man manuell Tests ausführen oder den Lexer testen kann.

**Examples:**

Beispiel aus `message.properties`:

```
MSG-005=Name key type %{p,richtig:true,falsch:false,size:-678764782,text:'text',message:'msg %{m}'}.
```

Erwartete Token-Sequenz:
- `Name key type ` → `Text`
- `%{` → `Punctuation.Special`
- `p` → `Name`
- `,` → `Punctuation`
- `richtig` → `Name`
- `:` → `Punctuation`
- `true` → `Keyword.Constant`
- `,` → `Punctuation`
- ... (weitere Tokens)
- `'` → `String.Delimiter`
- `msg ` → `String`
- `%{` → `Punctuation.Special`
- `m` → `Name`
- `}` → `Punctuation.Special`
- `'` → `String.Delimiter`
- `}` → `Punctuation.Special`
- `.` → `Text`

## Implementierungsentscheidungen - Zusammenfassung

1. **Latin-Extended Unicode statt vollständiger Unicode-Support** - Vereinfachung für bessere Kompatibilität
2. **State-Stack vollständig rekursiv** - Exakte Nachbildung des ANTLR-Verhaltens
3. **NameChar mit Underscore, NameStartChar ohne** - Gemäß Grammatik-Spezifikation
4. **Hyphenated Names unterstützt** - Wie in der Grammatik definiert
5. **Keine Integer-Limitierung** - Beliebig lange Zahlen erlaubt
6. **Whitespace in Parameter/Template ignoriert** - Wie in ANTLR geskippt
7. **Control-Chars als Whitespace-Token** - Für Debugging-Sichtbarkeit
8. **Escape-Sequenzen separat highlighted** - Bessere visuelle Unterscheidung
9. **Best-effort Error-Handling** - Permissiver Lexer ohne explizite Error-Tokens
10. **Gebündelte Test-Cases** - Effiziente Test-Organisation mit pytest parametrize

## Referenzen

- **MessageLexer.g4**: `/Users/jeroen/projects/message-format/message-format/src/main/antlr/MessageLexer.g4`
- **MessageParser.g4**: `/Users/jeroen/projects/message-format/message-format/src/main/antlr/MessageParser.g4`
- **Test-Daten**: `/Users/jeroen/projects/message-format/message-format/src/test/resources/message.properties`
- **Template-Daten**: `/Users/jeroen/projects/message-format/message-format/src/test/resources/template.properties`
- **Pygments Dokumentation**: https://pygments.org/docs/lexerdevelopment/

