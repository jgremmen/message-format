# MessageFormat Pygments Lexer

A Pygments lexer for the MessageFormat language, providing syntax highlighting for MessageFormat messages with support for parameters (`%{...}`), templates (`%[...]`), quoted strings, and various operators.

## Requirements

- **Python 3.7+**
- **Pygments 2.10+**

## Installation

### Option 1: Direct Import

Simply import the lexer directly from the module:

```python
from syntax.pygments.message_format import MessageFormatLexer
```

### Option 2: Install as Package

Create a `setup.py` in the project root:

```python
from setuptools import setup, find_packages

setup(
    name='messageformat-pygments',
    version='0.21.0',
    packages=find_packages(),
    install_requires=[
        'pygments>=2.10',
    ],
    entry_points={
        'pygments.lexers': [
            'msgfmt = syntax.pygments.message_format:MessageFormatLexer',
        ],
    },
)
```

Then install:

```bash
pip install .
```

After installation, the lexer is available system-wide via its aliases: `msgfmt` or `message-format`.

## Usage

### Programmatic Usage

```python
from pygments import highlight
from pygments.formatters import HtmlFormatter, TerminalFormatter
from syntax.pygments.message_format import MessageFormatLexer

# Example message
code = """
Hello %{name}!
Status: %{status,true:'Active',false:'Inactive'}
Details: %[info,id=user_id]
"""

# Highlight to HTML
html_output = highlight(code, MessageFormatLexer(), HtmlFormatter())
print(html_output)

# Highlight to terminal (with colors)
terminal_output = highlight(code, MessageFormatLexer(), TerminalFormatter())
print(terminal_output)

# Highlight to LaTeX
from pygments.formatters import LatexFormatter
latex_output = highlight(code, MessageFormatLexer(), LatexFormatter())
print(latex_output)
```

### Command-Line Usage

After installation or with `pygmentize` available:

```bash
# Highlight a file
pygmentize -l msgfmt message.mfp

# Highlight with HTML output
pygmentize -l msgfmt -f html -o output.html message.mfp

# Highlight with terminal colors
pygmentize -l msgfmt -f terminal message.mfp

# Using alternative alias
pygmentize -l message-format message.mfp
```

### Sphinx Integration

For documentation with Sphinx, add to your `conf.py`:

```python
from syntax.pygments.message_format import MessageFormatLexer
from sphinx.highlighting import lexers

lexers['msgfmt'] = MessageFormatLexer()
```

Then use in your RST documents:

```rst
.. code-block:: msgfmt

   Welcome %{username}!
   You have %{count,1:'1 message',:'%{count} messages'}.
```

Or in Markdown with MyST parser:

````markdown
```msgfmt
Welcome %{username}!
```
````

### Jupyter Notebook Integration

```python
from IPython.display import HTML
from pygments import highlight
from pygments.formatters import HtmlFormatter
from syntax.pygments.message_format import MessageFormatLexer

code = "Result: %{value,<0:'negative',=0:'zero',>0:'positive'}"
html = highlight(code, MessageFormatLexer(), HtmlFormatter(style='monokai'))
display(HTML(html))
```

## Implementation Decisions

### 1. Fully Recursive State-Stack Management

The lexer implements a fully recursive state stack using Pygments' `#push` and `#pop` mechanisms to accurately mimic ANTLR's `pushMode`/`popMode` behavior. This allows for unlimited nesting depth:

- Parameters can contain quoted strings
- Quoted strings can contain parameters
- Parameters can contain parameters (via quoted strings)
- Templates can contain quoted strings with parameters

Example of deep nesting:
```
%{a,'outer %{b,'inner %{c}'}'}
```

### 2. Unicode Character Class Approximation

Since Python's regex engine doesn't natively support ANTLR's Unicode character classes (`\p{L}`, `\p{N}`, etc.), the lexer approximates these with Latin-Extended character ranges:

| ANTLR Pattern | Approximation | Description |
|---------------|---------------|-------------|
| `\p{L}` | `[a-zA-Z\u00C0-\u024F\u1E00-\u1EFF]` | Letters: Basic Latin + Latin Extended-A/B + Latin Extended Additional |
| `\p{N}` | `[0-9]` | Numbers: ASCII digits |
| `\p{Zs}` | `[ \t\u00A0]` | Whitespace: space, tab, non-breaking space |
| `\p{P}` | `[!\"#$%&'()*+,\-./:;<=>?@\[\\\]^_\`{|}~¡-¿]` | Common punctuation |
| `\p{S}` | `[×÷±°§¶†‡•‰€£¥]` | Common symbols |

**Rationale:** These ranges cover most Western European languages while maintaining compatibility with Python's regex engine. The approximation can be extended with additional Unicode ranges if needed.

### 3. Name Character Rules

Following MessageLexer.g4 lines 206-217:

- **NameStartChar**: Must be a letter (no underscore allowed at start)
  - Pattern: `[a-zA-Z\u00C0-\u024F\u1E00-\u1EFF]`
  
- **NameChar**: Can be letter, digit, or underscore
  - Pattern: `[a-zA-Z0-9\u00C0-\u024F\u1E00-\u1EFF_]`
  
- **Name**: Supports hyphenated names
  - Pattern: `NameStartChar NameChar* (-NameChar+)*`
  - Examples: `name`, `my_name`, `my-name`, `ex-colon`

### 4. Integer Handling

Numbers follow the pattern `-?[0-9]+` with no length limitation. The lexer correctly handles:
- Positive integers: `42`, `9223372036854775807`
- Negative integers: `-1`, `-9223372036854775808`
- Zero: `0`, `-0`

### 5. Whitespace Handling

Following ANTLR's behavior:
- **In root mode**: Whitespace is part of text content (shown)
- **In parameter mode**: Whitespace is skipped (not shown)
- **In template mode**: Whitespace is skipped (not shown)
- **Control characters** (`\u0000-\u001f`): Tokenized as `Whitespace` for debugging visibility

### 6. Escape Sequence Highlighting

The lexer recognizes and highlights two types of escape sequences:

1. **Unicode escapes**: `\\uXXXX` (4 hex digits)
   - Example: `\\uD83D` (high surrogate for emoji)

2. **Character escapes**: `\\` followed by `"`, `'`, `%`, `{`, `[`, or `\\`
   - Examples: `\\"`, `\\'`, `\\%`, `\\{`, `\\[`, `\\\\`

All escape sequences are highlighted with `Token.String.Escape`.

### 7. Token Mapping

Complete mapping from ANTLR tokens to Pygments tokens:

| ANTLR Token | Pygments Token | Example | Description |
|-------------|----------------|---------|-------------|
| `P_START` | `Punctuation.Special` | `%{` | Parameter start |
| `P_END` | `Punctuation.Special` | `}` | Parameter end |
| `TPL_START` | `Punctuation.Special` | `%[` | Template start |
| `TPL_END` | `Punctuation.Special` | `]` | Template end |
| `COMMA` | `Punctuation` | `,` | Comma separator |
| `COLON` | `Punctuation` | `:` | Colon separator |
| `L_PAREN` | `Punctuation` | `(` | Left parenthesis |
| `R_PAREN` | `Punctuation` | `)` | Right parenthesis |
| `BOOL` | `Keyword.Constant` | `true`, `false` | Boolean literals |
| `NULL` | `Keyword.Constant` | `null` | Null literal |
| `EMPTY` | `Keyword.Constant` | `empty` | Empty literal |
| `NUMBER` | `Number.Integer` | `-678764782` | Integer number |
| `NAME` | `Name` | `myName`, `ex-colon` | Identifier |
| `EQ` | `Operator` | `=` | Equals |
| `NE` | `Operator` | `<>`, `!` | Not equals |
| `LT` | `Operator` | `<` | Less than |
| `LTE` | `Operator` | `<=` | Less than or equal |
| `GT` | `Operator` | `>` | Greater than |
| `GTE` | `Operator` | `>=` | Greater than or equal |
| `SQ_START`, `SQ_END` | `String.Delimiter` | `'` | Single quote |
| `DQ_START`, `DQ_END` | `String.Delimiter` | `"` | Double quote |
| `CH` (in quotes) | `String` | Text content | String content |
| `CH` (in root) | `Text` | Regular text | Regular text |
| `EscapeSequence` | `String.Escape` | `\\uD83D`, `\\'` | Escape sequence |
| `CTRL_CHAR` | `Whitespace` | `\u0000-\u001f` | Control characters |

**Note:** The parser-aware token types (Name.Variable, Name.Function, Name.Class, Name.Attribute) are determined by position and context within the grammar structure, providing context-sensitive highlighting beyond simple lexical analysis.

### 8. Error Handling

The lexer follows Pygments' best-effort approach:
- Invalid syntax is tokenized as best as possible
- No explicit error tokens are generated
- Partial matches are still highlighted
- Unclosed quotes/parameters are handled gracefully

This permissive behavior is typical for syntax highlighters and allows highlighting of incomplete or malformed code during editing.

## Testing

### Local Testing with pytest

Ensure you have pytest installed:

```bash
pip install pytest pygments
```

Run tests from the syntax/pygments directory:

```bash
cd syntax/pygments
pytest test/test_message_format.py -v
```

Run specific test classes:

```bash
pytest test/test_message_format.py::TestMessageProperties -v
pytest test/test_message_format.py::TestEdgeCases -v
```

Run with coverage:

```bash
pip install pytest-cov
pytest test/ --cov=message_format --cov-report=html
```

### Docker-Based Testing

The project includes Docker support for testing in an isolated environment:

```bash
# Run all tests
./syntax/pygments/test/run_tests.sh

# Interactive mode for debugging
./syntax/pygments/test/run_tests.sh --interactive
```

In interactive mode, you can:

```bash
# Run tests manually
pytest -v /app/syntax/pygments/test/test_message_format.py

# Test the lexer interactively
python3
>>> from syntax.pygments.message_format import MessageFormatLexer
>>> lexer = MessageFormatLexer()
>>> list(lexer.get_tokens("Hello %{name}!"))

# Exit
exit()
```

### Test Coverage

The test suite covers:

1. **All examples from message.properties** (MSG-001 through MSG-009)
2. **All examples from template.properties** (ex-colon, result)
3. **Edge cases**:
   - Empty messages
   - Deeply nested structures
   - All escape sequences
   - Hyphenated names
   - Names with underscores
   - All operators individually
   - Extreme integer values
   - Mixed quote types
   - Parameter and template combinations
4. **Parser-aware structures** (NEW - based on MessageParser.g4):
   - Parameter name context (`Name.Variable`)
   - Parameter format context (`Name.Function`)
   - Template name context (`Name.Class`)
   - Named config elements (`Name.Attribute`)
   - Template parameter delegation
   - Keywords as names
   - Complex parameter structures

### Test Results

**30 Test Cases - All Passing:**

```
============================== 30 passed in 0.03s ==============================
✓ All tests passed!
```

**Test Breakdown:**
- **TestMessageProperties**: 14 tests (all 9 MSG examples + specific feature tests)
- **TestTemplateProperties**: 2 tests (template examples)
- **TestEdgeCases**: 7 tests (edge cases and special scenarios)
- **TestParserStructures**: 7 tests (parser-aware context tracking)

**Coverage:** 100% pass rate across all MessageFormat features

## Grammar Compliance

### MessageLexer.g4 ✅

- ✅ All lexer tokens correctly mapped to Pygments tokens
- ✅ Control characters handled as whitespace
- ✅ Escape sequences properly highlighted (`\uXXXX`, `\"`, `\'`, `\%`, `\{`, `\[`, `\\`)
- ✅ Unicode approximation with Latin-Extended character ranges
- ✅ Hyphenated names supported (`ex-colon`, `my-name-123`)
- ✅ Names with underscores supported (`my_name`, `test_123`)
- ✅ Unlimited integer length support
- ✅ All operators recognized (`<`, `<=`, `>`, `>=`, `=`, `!=`, `<>`, `!`)

### MessageParser.g4 ✅

- ✅ Parameter structure: `%{name, format?, config*, :default?}`
- ✅ Template structure: `%[name, (namedConfig|delegation)*]`
- ✅ Config elements: Both named configs and map configs
- ✅ Keywords as names via `nameOrKeyword` rule (line 157)
- ✅ Quoted contexts with recursive nesting
- ✅ Parameter delegation in templates (`param=delegated`)
- ✅ Comma-colon pattern for default messages (`,:'default'`)
- ✅ Context-aware token types based on position

## Examples

### Example 1: Simple Message

**Input:**
```
Hello %{name}!
```

**Token Sequence:**
- `Hello ` → `Text`
- `%{` → `Punctuation.Special`
- `name` → `Name`
- `}` → `Punctuation.Special`
- `!` → `Text`

### Example 2: Conditional Message

**Input:**
```
Status: %{active,true:'Enabled',false:'Disabled'}
```

**Token Sequence:**
- `Status: ` → `Text`
- `%{` → `Punctuation.Special`
- `active` → `Name`
- `,` → `Punctuation`
- `true` → `Keyword.Constant`
- `:` → `Punctuation`
- `'` → `String.Delimiter`
- `Enabled` → `String`
- `'` → `String.Delimiter`
- `,` → `Punctuation`
- `false` → `Keyword.Constant`
- `:` → `Punctuation`
- `'` → `String.Delimiter`
- `Disabled` → `String`
- `'` → `String.Delimiter`
- `}` → `Punctuation.Special`

### Example 3: Nested Parameters

**Input:**
```
Message: %{count,1:'1 item',:'%{count} items'}
```

**Highlights:**
- Outer parameter with `count` variable
- Number `1` for singular case
- Nested parameter `%{count}` inside plural string
- Colon-only case for default

### Example 4: Template Usage with Delegation

**Input:**
```
Error occurred: %[error-details,code=error_code,level:high]
```

**Highlights:**
- Template start `%[`
- `error-details` → `Name.Class` (hyphenated template name)
- `code` → `Name.Attribute` (parameter delegation)
- `=` → `Operator`
- `error_code` → `Name.Attribute`
- `level` → `Name.Attribute` (named config key)
- `high` → `Name.Attribute` (config value)
- Template end `]`

### Example 5: Complex Relational Operators

**Input:**
```
Grade: %{score,<60:'F',<70:'D',<80:'C',<90:'B',:'A'}
```

**Highlights:**
- Multiple less-than comparisons
- Different grade strings
- Default case with empty condition

### Example 6: Escape Sequences

**Input:**
```
Special: \"quote\", \\backslash\\, \%percent\%, \\uD83D\\uDE00
```

**Highlights:**
- `\\"` → `String.Escape` (escaped quote)
- `\\\\` → `String.Escape` (escaped backslash)
- `\\%` → `String.Escape` (escaped percent)
- `\\uD83D` → `String.Escape` (Unicode escape)
- `\\uDE00` → `String.Escape` (Unicode escape)

## File Format Support

The lexer is configured for `*.mfp` (Message Format Pack) files but can be used with any text containing MessageFormat syntax:

- `*.mfp` - Message Format Pack files
- `*.properties` - Java properties files with messages
- `*.txt` - Plain text with embedded messages
- Any file containing MessageFormat syntax

## Lexer Metadata

- **Name**: `Message Format`
- **Aliases**: `msgfmt`, `message-format`
- **Filenames**: `*.mfp`
- **MIME Type**: `application/x-message-format`

## Known Limitations

1. **Unicode Coverage**: Limited to Latin-Extended character sets. Messages with Greek, Cyrillic, Asian, or other scripts will have basic highlighting but character classification may not be perfect.

2. **No Semantic Validation**: The lexer performs syntax highlighting only and doesn't validate:
   - Variable name existence
   - Format string correctness
   - Type compatibility
   - Template parameter matching

3. **Escape Sequence in Root**: Escape sequences are primarily designed for quoted strings. In root mode, they're recognized but may behave differently than in ANTLR.

4. **Regex Limitations**: Some advanced Unicode properties from ANTLR are approximated with character ranges.

## Contributing

To extend the lexer:

1. **Add more Unicode ranges**: Modify the character class patterns in `message_format.py`
2. **Add new token types**: Update the `tokens` dictionary
3. **Add tests**: Extend `test_message_format.py` with new test cases
4. **Update documentation**: Keep this README in sync with changes

## License

This lexer is based on the MessageFormat grammar from the message-format library.

Copyright 2020 Jeroen Gremmen

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

## References

- **MessageLexer.g4**: ANTLR lexer grammar defining the token structure
- **MessageParser.g4**: ANTLR parser grammar defining the syntax structure
- **Pygments Documentation**: https://pygments.org/docs/lexerdevelopment/
- **MessageFormat Library**: https://lib.sayayi.de/message-format/

## Changelog

### Version 1.0.0 (2024-12-19)

- Initial implementation with parser-aware context tracking
- Full support for parameters (`%{...}`) with context-sensitive highlighting
- Full support for templates (`%[...]`) with template name detection
- Recursive state management for unlimited nesting (6 states total)
- Parser-aware token types:
  - `Name.Variable` for parameter names
  - `Name.Function` for format names
  - `Name.Class` for template names
  - `Name.Attribute` for config keys
- Smart state transitions based on token type detection
- Keywords as names support (via `nameOrKeyword` rule)
- All operators and keywords supported
- Escape sequence highlighting
- Comprehensive test suite with 30 test cases (100% pass rate)
- Docker-based testing support
- Full compliance with MessageLexer.g4 and MessageParser.g4
