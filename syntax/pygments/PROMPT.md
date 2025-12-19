# Pygments Lexer for MessageFormat - Final Plan

## Overview

A Pygments lexer is created that performs syntax highlighting for the MessageFormat language according to **BOTH MessageParser.g4 AND MessageLexer.g4**. The lexer implements fully recursive state-stack management with Latin-Extended Unicode support and follows the ANTLR grammar as precisely as possible.

## Key Requirements from MessageParser.g4

The parser grammar defines structural patterns that influence syntax highlighting:

1. **Parameter Structure** (line 70-79):
   - Format: `%{parameterName, parameterFormat, configElement*, :defaultMessage}`
   - First name after `%{` is the parameter name
   - Second name (after first comma) is the format name
   - Remaining elements are config keys/values
   - Optional default message after `,:`

2. **Template Structure** (line 95-100):
   - Format: `%[templateName, namedConfig|parameterDelegate, ...]`
   - First name after `%[` is the template name
   - Following elements can be named configs or parameter delegates (`param=delegated`)

3. **Config Elements** (line 108-127):
   - **Named configs**: `name:value` (where value can be BOOL, NUMBER, string, or quoted message)
   - **Map configs**: `key:value` or `(key1,key2,...):value`
   - Keys can have optional operators before them

4. **Context-Aware Tokens**:
   - `nameOrKeyword` (line 157): NAME, BOOL, NULL, EMPTY can all act as names in certain contexts
   - This means keywords can be used as parameter/template names

5. **Quoted Contexts**:
   - `quotedMessage`: Full messages within quotes (can contain parameters/templates)
   - `quotedString`: Simple text within quotes
   - Both single and double quotes supported

## Implementation Steps

### 1. Create `syntax/pygments/message_format.py`

Implement `RegexLexer` with the following properties, incorporating insights from **both MessageLexer.g4 and MessageParser.g4**:

**Lexer Metadata:**
- `name = "Message Format"`
- `aliases = ["msgfmt", "message-format"]`
- `filenames = ["*.mfp"]`

**State Management:**
- Fully recursive state stack with 8 states: `root`, `parameter`, `parameter-format`, `parameter-config`, `template`, `template-config`, `single-quote`, `double-quote`
- Uses `#push` and `#pop` for nested structures
- Supports unlimited nesting depth (parameters in strings, strings in parameters, etc.)
- Parser-aware substates for context-sensitive token types

**Parser-Aware Enhancements:**
- Track position within parameter/template to provide context-aware highlighting
- Distinguish between parameter names, format names, and config keys
- Recognize named config patterns (`name:value`)
- Support parameter delegation syntax (`param=delegated`) in templates
- Handle comma-colon pattern (`,:`before default messages)

**Name Pattern (according to MessageLexer.g4 lines 206-217):**
- NameStartChar: `[a-zA-Z\u00C0-\u024F\u1E00-\u1EFF]`
- NameChar: `[a-zA-Z0-9\u00C0-\u024F\u1E00-\u1EFF_]` (with underscore according to line 211)
- Complete name with hyphens: `[a-zA-Z\u00C0-\u024F\u1E00-\u1EFF][a-zA-Z0-9\u00C0-\u024F\u1E00-\u1EFF_]*(-[a-zA-Z0-9\u00C0-\u024F\u1E00-\u1EFF_]+)*`

**Character Fragment (according to MessageLexer.g4 lines 218-223):**

Approximation of Unicode categories with Latin-Extended character sets:
- Whitespace: `[ \t\u00A0]+` (Unicode Zs approximated)
- Letters: `[a-zA-Z\u00C0-\u024F\u1E00-\u1EFF]` (Unicode L approximated)
- Numbers: `[0-9]` (Unicode N approximated)
- Punctuation: `[!\"#$%&'()*+,\-./:;<=>?@\[\\\]^_\`{|}~¡-¿]` (Unicode P approximated)
- Symbols: `[×÷±°§¶†‡•‰€£¥]` (Unicode S approximated)

**Control Characters (line 203):**
- Pattern: `[\u0000-\u001f]+`
- Token: `Token.Whitespace`

**Escape Sequences (lines 225-227):**
- Unicode escape: `\\u[0-9a-fA-F]{4}`
- Character escape: `\\["'%{\\[]`
- Token: `Token.String.Escape`

**Whitespace in Parameter/Template Modes:**
- According to MessageLexer.g4 lines 127 and 185, whitespaces are skipped
- In Pygments lexer: Do not match (ignore)

**Number Pattern (line 220):**
- Pattern: `-?[0-9]+`
- No length restriction (arbitrarily long integers allowed)
- Token: `Number.Integer`

**Token Mapping (with Parser Context):**

Basic tokens:
- Keywords (`true`, `false`, `null`, `empty`) → `Keyword.Constant`
- Numbers (negative/positive integers of arbitrary length) → `Number.Integer`
- Operators (`<>`, `!`, `<`, `<=`, `>`, `>=`, `=`) → `Operator`
- Delimiters (`,`, `:`, `(`, `)`) → `Punctuation`
- Parameter start/end (`%{`, `}`) and template start/end (`%[`, `]`) → `Punctuation.Special`
- Quote characters (`'`, `"`) → `String.Delimiter`
- String content → `String`
- Regular text (CH) → `Text`

Context-aware tokens (based on MessageParser.g4):
- **Parameter name** (first name after `%{`) → `Name.Variable` 
- **Format name** (second name in parameter, after first comma) → `Name.Function`
- **Template name** (first name after `%[`) → `Name.Class`
- **Config key names** (in `name:value` patterns) → `Name.Attribute`
- **Generic names** (other contexts) → `Name`

Note: While Pygments lexers cannot fully implement parser-level context awareness, the lexer should attempt to distinguish these contexts where possible using state tracking.

### 2. Create `syntax/pygments/test/test_message_format.py`

Pytest tests with bundled test cases:

**Test 1: `test_message_properties`**
- Parametrized test with all 9 MSG examples from `message.properties`
- MSG-001: Empty message
- MSG-002: Simple text
- MSG-003: Special characters and escape sequences (`\\uD83D`, `\\uDE00`)
- MSG-004: Enum keys with various operators (`empty`, `!empty`, `=null`, `!null`, `true`, `false`)
- MSG-005: Named keys with nested parameters (`'msg %{m}'`), negative numbers (`-678764782`)
- MSG-006: Number keys with relational operators (`<10`, `<=20`, `=30`, `>1999`, `>=200`, `!25`)
- MSG-007: String keys with relational operators (`<'A'`, `>'Z'`)
- MSG-008: Templates (`%[result,n=count,p:true]`, `%[ex-colon]`)
- MSG-009: Extreme integer values (`-9223372036854775808`, `9223372036854775807`)

**Test 2: `test_template_properties`**
- Both examples from `template.properties`:
  - `ex-colon=%{ex,!empty:': %{ex}'}`
  - `result=%{n,1:'1 result',:'%{n} results'}`

**Test 3: `test_edge_cases`**
- Nested structures (multiple levels)
- All escape sequences (`\\u`, `\\"`, `\\'`, `\\%`, `\\{`, `\\[`, `\\\\`)
- Hyphenated names (`ex-colon`, `my-name`, `test-123`)
- All operators individually
- Empty strings (`''`, `""`)
- Control characters
- Names with underscores (`my_name`, `test_123`)

**Test 4: `test_parser_structures`** (NEW - based on MessageParser.g4)
- Parameter structure: `%{name,format,key:value,:default}`
- Template parameter delegation: `%[template,param=delegated]`
- Named config elements: `name:true`, `name:123`, `name:'text'`
- Map config elements: `(key1,key2):value`
- Comma-colon default pattern: `,:default message`
- Keywords as names (`%{null}`, `%{empty}`, `%{true}`)

**Assertions:**
- Use `list(lex(code, MessageFormatLexer()))` for token sequences
- Check token types and optionally token values
- Example: `assert tokens[0] == (Token.Punctuation.Special, '%{')`

### 3. Create `syntax/pygments/test/Dockerfile`

```dockerfile
FROM python:3.11-slim

# Install dependencies
RUN pip install --no-cache-dir pygments pytest

# Set working directory
WORKDIR /app

# Copy lexer and tests
COPY ../message_format.py /app/syntax/pygments/message_format.py
COPY test_message_format.py /app/syntax/pygments/test/test_message_format.py

# Set PYTHONPATH so imports work
ENV PYTHONPATH=/app

# Run tests by default
CMD ["pytest", "-v", "/app/syntax/pygments/test/test_message_format.py"]
```

### 4. Create `syntax/pygments/test/run_tests.sh`

Executable shell script (`#!/usr/bin/env bash`, `chmod +x`).

**Note:** Uses bash-compatible syntax (not zsh-specific) for cross-platform compatibility (Linux/macOS).

```bash
#!/usr/bin/env bash

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

### 5. Create `syntax/pygments/README.md`

Documentation with the following sections:

**Requirements:**
- Python 3.7+
- Pygments 2.10+

**Implementation Decisions:**

1. **Fully Recursive State-Stack Management**
   - Implemented via `#push`/`#pop` for unlimited nesting
   - Supports parameters in strings, strings in parameters, templates in strings, etc.
   - Mimics ANTLR's `pushMode`/`popMode` behavior

2. **Parser-Aware Context Tracking (NEW)**
   - Based on MessageParser.g4 structural patterns
   - Distinguishes parameter names, format names, and template names
   - Recognizes named config patterns (`name:value`)
   - Handles parameter delegation (`param=delegated`)
   - Detects comma-colon default patterns (`,:`before default messages)
   - Note: Limited context awareness due to lexer constraints

3. **Unicode Approximation**
   - ANTLR uses `\p{L}`, `\p{N}`, `\p{P}`, `\p{S}`, `\p{Zs}` (Unicode categories)
   - Python regex doesn't natively support these
   - Approximation with Latin-Extended character sets:
     - `\u00C0-\u024F`: Latin Extended-A and -B
     - `\u1E00-\u1EFF`: Latin Extended Additional
   - No underscores in NameStartChar (only in NameChar)

4. **NameChar Includes Underscore**
   - According to MessageLexer.g4 line 211
   - NameStartChar may NOT contain underscore
   - NameChar may contain underscore, digits, and letters

5. **Hyphenated Names**
   - According to MessageLexer.g4 line 206
   - Pattern: `NameStartChar NameChar* (-NameChar+)*`
   - Examples: `ex-colon`, `my-name-123`

6. **No Integer Length Limitation**
   - Pattern: `-?[0-9]+`
   - Arbitrarily long integers like `9223372036854775807` are supported
   - No additional validation

7. **Whitespace in Parameter/Template Modes**
   - ANTLR skips whitespaces (lines 127, 185)
   - Pygments implementation: Whitespace not explicitly matched (ignored)

8. **Control Characters as Whitespace Token**
   - ANTLR skips `[\u0000-\u001f]`
   - Pygments matches these as `Token.Whitespace` for better debugging visibility

9. **Escape Sequences Separately Highlighted**
   - Unicode escape: `\\uXXXX` with 4 hex digits
   - Character escape: `\"`, `\'`, `\%`, `\{`, `\[`, `\\`
   - Token: `Token.String.Escape`

10. **Error Handling**
    - Best-effort matching without explicit error tokens
    - Permissive behavior typical for Pygments lexers

**Complete Token Mapping Table:**

| ANTLR Token | Pygments Token | Example |
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
| `CH` (in quotes) | `String` | Text in strings |
| `CH` (root) | `Text` | Regular text |
| `EscapeSequence` | `String.Escape` | `\\uD83D`, `\\'` |
| `CTRL_CHAR` | `Whitespace` | `\u0000-\u001f` |

**Context-Aware Token Extensions (from MessageParser.g4):**

| Parser Context | Token Enhancement | Example |
|----------------|-------------------|---------|
| `parameterName` (first after `%{`) | `Name.Variable` | `%{userName}` |
| `parameterFormat` (second in param) | `Name.Function` | `%{value,number}` |
| `templateName` (first after `%[`) | `Name.Class` | `%[errorTemplate]` |
| `configNamedElement` key | `Name.Attribute` | `level:high` |
| `templateParameterDelegate` | `Name` + `Operator` + `Name` | `param=delegated` |

**Usage:**

1. **Programmatically:**

```python
from pygments import highlight
from pygments.formatters import HtmlFormatter
from syntax.pygments.message_format import MessageFormatLexer

code = "Hello %{name}!"
result = highlight(code, MessageFormatLexer(), HtmlFormatter())
print(result)
```

2. **CLI with pygmentize:**

```bash
pygmentize -l msgfmt file.mfp
pygmentize -l message-format file.mfp
```

3. **Installation as Custom Lexer:**

In `setup.py`:

```python
from setuptools import setup

setup(
    name='messageformat-pygments',
    packages=['syntax.pygments'],
    entry_points={
        'pygments.lexers': [
            'msgfmt = syntax.pygments.message_format:MessageFormatLexer',
        ],
    },
)
```

After installation with `pip install .`, the lexer is available system-wide.

4. **Sphinx Integration:**

In `conf.py`:

```python
from syntax.pygments.message_format import MessageFormatLexer
from sphinx.highlighting import lexers

lexers['msgfmt'] = MessageFormatLexer()
```

Usage in RST/Markdown:

```rst
.. code-block:: msgfmt

   Hello %{name}!
```

**Testing:**

1. **Locally with pytest:**

```bash
cd syntax/pygments
pytest test/test_message_format.py -v
```

2. **With Docker:**

```bash
./syntax/pygments/test/run_tests.sh
```

3. **Interactive (for debugging):**

```bash
./syntax/pygments/test/run_tests.sh --interactive
```

This opens a shell in the Docker container where you can manually run tests or try the lexer.

**Examples:**

Example from `message.properties`:

```
MSG-005=Name key type %{p,richtig:true,falsch:false,size:-678764782,text:'text',message:'msg %{m}'}.
```

Expected token sequence:
- `Name key type ` → `Text`
- `%{` → `Punctuation.Special`
- `p` → `Name`
- `,` → `Punctuation`
- `richtig` → `Name`
- `:` → `Punctuation`
- `true` → `Keyword.Constant`
- `,` → `Punctuation`
- ... (more tokens)
- `'` → `String.Delimiter`
- `msg ` → `String`
- `%{` → `Punctuation.Special`
- `m` → `Name`
- `}` → `Punctuation.Special`
- `'` → `String.Delimiter`
- `}` → `Punctuation.Special`
- `.` → `Text`

## Implementation Decisions - Summary

1. **Latin-Extended Unicode instead of full Unicode support** - Simplification for better compatibility
2. **State-stack fully recursive** - Exact replication of ANTLR behavior
3. **NameChar with underscore, NameStartChar without** - According to grammar specification
4. **Hyphenated names supported** - As defined in the grammar
5. **No integer limitation** - Arbitrarily long numbers allowed
6. **Whitespace in parameter/template ignored** - As skipped in ANTLR
7. **Control chars as whitespace token** - For debugging visibility
8. **Escape sequences separately highlighted** - Better visual distinction
9. **Best-effort error handling** - Permissive lexer without explicit error tokens
10. **Bundled test cases** - Efficient test organization with pytest parametrize
11. **Parser-aware context tracking (NEW)** - Attempts to distinguish parameter names, format names, template names, and config keys based on position

## Note on Lexer Limitations

Pygments lexers have inherent limitations compared to full parsers:

- **Cannot fully track parser-level context** - A lexer processes tokens sequentially without the full parse tree that a parser maintains
- **Should attempt to distinguish contexts where possible using state tracking** - The implementation uses state substates to approximate parser context
- **Best-effort approach with limited lookahead** - State transitions are based on the next token type, not full lookahead of the entire structure
- **May not always correctly identify context in complex nested structures** - Deeply nested or unusual patterns may not always be highlighted with perfect context awareness

This is a reasonable compromise between full parser implementation and lexer simplicity. The implementation provides meaningful context-aware highlighting for the vast majority of real-world MessageFormat usage patterns while maintaining the performance and simplicity expected of a Pygments lexer.

## References

- **MessageLexer.g4**: `/Users/jeroen/projects/message-format/message-format/src/main/antlr/MessageLexer.g4`
- **MessageParser.g4**: `/Users/jeroen/projects/message-format/message-format/src/main/antlr/MessageParser.g4`
- **Test Data**: `/Users/jeroen/projects/message-format/message-format/src/test/resources/message.properties`
- **Template Data**: `/Users/jeroen/projects/message-format/message-format/src/test/resources/template.properties`
- **Pygments Documentation**: https://pygments.org/docs/lexerdevelopment/
