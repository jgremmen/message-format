# -*- coding: utf-8 -*-
"""
    pygments.lexers.messageformat
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    Lexer for MessageFormat language.

    Based on MessageLexer.g4 and MessageParser.g4 from the message-format library.

    Implements parser-aware context tracking to distinguish between:
    - Parameter names (first position in %{...}) -> Name.Variable
    - Format names (after FORMAT keyword) -> Name.Function
    - Template names (first position in %[...]) -> Name.Label
    - Post-format names (first position in %(...)) -> Name.Function
    - Config keys (in name:value patterns) -> Name.Attribute

    :copyright: Copyright 2020 Jeroen Gremmen
    :license: Apache License 2.0
"""

from pygments.lexer import RegexLexer
from pygments.token import (
    Operator, Keyword, Name, String, Number, Punctuation, Whitespace
)

__all__ = ['MessageFormatLexer']


class MessageFormatLexer(RegexLexer):
    """
    Lexer for MessageFormat language.

    MessageFormat is a powerful message formatting library that supports
    parameters (%{...}), templates (%[...]), post-formats (%(...)), and localized messages.

    This lexer implements context-aware highlighting based on both
    MessageLexer.g4 and MessageParser.g4, distinguishing between:
    - Parameter names (first position in %{...}) -> Name.Variable
    - Format names (after FORMAT keyword) -> Name.Function
    - Template names (first position in %[...]) -> Name.Label
    - Post-format names (first position in %(...)) -> Name.Function
    - Config keys (in name:value patterns) -> Name.Attribute

    .. versionadded:: 2.15
    """

    name = 'Message Format'
    aliases = ['msgfmt', 'message-format']
    filenames = ['*.mfp']
    mimetypes = ['application/x-message-format']

    # Unicode character classes approximation
    # NameStartChar: Letters (Unicode L)
    _name_start = r'[a-zA-Z\u00C0-\u024F\u1E00-\u1EFF]'
    # NameChar: Letters, Digits (Unicode L + N)
    _name_char = r'[a-zA-Z0-9\u00C0-\u024F\u1E00-\u1EFF]'
    # Name: NameStartChar NameChar* ([_-] NameChar+)*
    _name = _name_start + _name_char + r'*(?:[_-]' + _name_char + r'+)*'

    tokens = {
        'root': [
            # Control characters (skipped in ANTLR)
            (r'[\u0000-\u001f]+', Whitespace),

            # Parameter start: %{ -> push to parameter-name mode
            (r'%\{', Punctuation.Special, 'parameter-name'),

            # Template start: %[ -> push to template-name mode
            (r'%\[', Punctuation.Special, 'template-name'),

            # Post-format start: %( -> push to postformat-name mode
            (r'%\(', Punctuation.Special, 'postformat-name'),

            # Escape sequences
            (r'\\u[0-9a-fA-F]{4}', String.Escape),
            (r'\\["%\'{\\[]', String.Escape),

            # Regular text (Character fragment - any character that's not special)
            (r'[^%\\\u0000-\u001f]+', String),
            (r'%', String),  # Standalone % not followed by {, [, or (
            (r'\\', String),  # Standalone \ not part of escape
        ],

        # Parameter mode - first position is parameter name (Name.Variable)
        'parameter-name': [
            # Parameter end: } -> pop back
            (r'\}', Punctuation.Special, '#pop'),

            # First comma transitions to parameter body
            (r',', Punctuation, ('#pop', 'parameter-body')),

            # Parameter name (first position) - can be NAME, BOOL, NULL, EMPTY, FORMAT
            # All highlighted as Name.Variable per tokens.md line 19
            (r'\b(?:true|false)\b', Name.Variable),
            (r'\bnull\b', Name.Variable),
            (r'\bempty\b', Name.Variable),
            (r'\bformat\b', Name.Variable),
            (_name, Name.Variable),
        ],

        # After first comma in parameter: format, mapEntry, or configDefinition
        'parameter-body': [
            (r'\}', Punctuation.Special, '#pop'),
            (r',', Punctuation),

            # Check for FORMAT keyword (parameterFormat rule)
            # This is a 3-token sequence: FORMAT, COLON, nameOrKeyword
            (r'\bformat\b', Keyword.Reserved, 'parameter-format-colon'),

            # Operators for mapKey (before other patterns)
            (r'<=', Operator),
            (r'>=', Operator),
            (r'<>', Operator),
            (r'!', Operator),
            (r'<', Operator),
            (r'>', Operator),
            (r'=', Operator),

            # Colon for configDefinition or mapEntry
            (r':', Punctuation),

            # Keywords (for mapKey BOOL, NULL, EMPTY)
            (r'\b(?:true|false)\b', Keyword.Constant),
            (r'\bnull\b', Keyword.Constant),
            (r'\bempty\b', Keyword.Constant),

            # Numbers
            (r'-?[0-9]+', Number.Integer),

            # Parentheses for grouped map keys
            (r'\(', Punctuation),
            (r'\)', Punctuation),

            # Strings (quotedString for mapKey or quotedMessage for values)
            (r"'", String.Single, 'single-quote'),
            (r'"', String.Double, 'double-quote'),

            # Config key names (NAME in configDefinition) - Name.Attribute
            # This won't match BOOL/NULL/EMPTY/FORMAT since they're keywords above
            (_name, Name.Attribute),
        ],

        # After FORMAT keyword: expecting COLON
        'parameter-format-colon': [
            (r':', Punctuation, ('#pop', 'parameter-format-name')),
        ],

        # After FORMAT COLON: expecting nameOrKeyword -> Name.Function
        'parameter-format-name': [
            (r'\b(?:true|false)\b', Name.Function, '#pop'),
            (r'\bnull\b', Name.Function, '#pop'),
            (r'\bempty\b', Name.Function, '#pop'),
            (_name, Name.Function, '#pop'),
        ],

        # Template mode - first position is template name (Name.Label)
        'template-name': [
            # Template end: ] -> pop back
            (r'\]', Punctuation.Special, '#pop'),

            # First comma transitions to template body
            (r',', Punctuation, ('#pop', 'template-body')),

            # Template name (first position) - can be NAME, BOOL, NULL, EMPTY
            # All highlighted as Name.Label per tokens.md line 23
            (r'\b(?:true|false)\b', Name.Label),
            (r'\bnull\b', Name.Label),
            (r'\bempty\b', Name.Label),
            (_name, Name.Label),
        ],

        # Template body: configDefinition or templateParameterDelegate
        'template-body': [
            (r'\]', Punctuation.Special, '#pop'),
            (r',', Punctuation),
            (r':', Punctuation),

            # Equals for templateParameterDelegate (simpleString = simpleString)
            # Per tokens.md line 24: Name.Variable, Operator, Name.Variable
            (r'=', Operator),

            # Keywords - used in configDefinition values or as simpleString
            (r'\b(?:true|false)\b', Keyword.Constant),
            (r'\bnull\b', Keyword.Constant),
            (r'\bempty\b', Keyword.Constant),

            # Numbers
            (r'-?[0-9]+', Number.Integer),

            # Strings
            (r"'", String.Single, 'single-quote'),
            (r'"', String.Double, 'double-quote'),

            # Names - for configDefinition (Name.Attribute) or templateParameterDelegate
            # In templateParameterDelegate context (before/after =), should be Name.Variable
            # In configDefinition context (before :), should be Name.Attribute
            # We use Name.Attribute as the default since we can't distinguish perfectly
            (_name, Name.Attribute),
        ],

        # Post-format mode - first position is post-format name (Name.Function)
        'postformat-name': [
            # Post-format end: ) -> pop back
            (r'\)', Punctuation.Special, '#pop'),

            # First comma transitions to postformat-message (mandatory quotedMessage)
            (r',', Punctuation, ('#pop', 'postformat-message')),

            # Post-format name (first position) - can be NAME, BOOL, NULL, EMPTY, FORMAT
            # All highlighted as Name.Function per tokens.md line 25
            (r'\b(?:true|false)\b', Name.Function),
            (r'\bnull\b', Name.Function),
            (r'\bempty\b', Name.Function),
            (r'\bformat\b', Name.Function),
            (_name, Name.Function),
        ],

        # After post-format name: expecting mandatory quotedMessage
        'postformat-message': [
            (r'\)', Punctuation.Special, '#pop'),

            # After quotedMessage, comma leads to configDefinition
            (r',', Punctuation, ('#pop', 'postformat-config')),

            # Strings (quotedMessage) - must be quoted
            (r"'", String.Single, 'single-quote'),
            (r'"', String.Double, 'double-quote'),
        ],

        # Post-format config: configDefinition only (no mapKeys)
        'postformat-config': [
            (r'\)', Punctuation.Special, '#pop'),
            (r',', Punctuation),
            (r':', Punctuation),

            # Keywords
            (r'\b(?:true|false)\b', Keyword.Constant),
            (r'\bnull\b', Keyword.Constant),
            (r'\bempty\b', Keyword.Constant),

            # Numbers
            (r'-?[0-9]+', Number.Integer),

            # Strings
            (r"'", String.Single, 'single-quote'),
            (r'"', String.Double, 'double-quote'),

            # Config names (NAME only, not keywords)
            (_name, Name.Attribute),
        ],

        'single-quote': [
            # Single quote end -> pop back (SQ_END)
            (r"'", String.Single, '#pop'),

            # Parameter start in string: %{ -> push to parameter-name mode
            (r'%\{', Punctuation.Special, 'parameter-name'),

            # Template start in string: %[ -> push to template-name mode
            (r'%\[', Punctuation.Special, 'template-name'),

            # Post-format start in string: %( -> push to postformat-name mode
            (r'%\(', Punctuation.Special, 'postformat-name'),

            # Escape sequences
            (r'\\u[0-9a-fA-F]{4}', String.Escape),
            (r'\\["%\'{\\[]', String.Escape),

            # Control characters
            (r'[\u0000-\u001f]+', Whitespace),

            # String content (text inside quotes)
            (r"[^'%\\\u0000-\u001f]+", String),

            # Single characters that might be start of something
            (r'%', String),
            (r'\\', String),
        ],

        'double-quote': [
            # Double quote end -> pop back (DQ_END)
            (r'"', String.Double, '#pop'),

            # Parameter start in string: %{ -> push to parameter-name mode
            (r'%\{', Punctuation.Special, 'parameter-name'),

            # Template start in string: %[ -> push to template-name mode
            (r'%\[', Punctuation.Special, 'template-name'),

            # Post-format start in string: %( -> push to postformat-name mode
            (r'%\(', Punctuation.Special, 'postformat-name'),

            # Escape sequences
            (r'\\u[0-9a-fA-F]{4}', String.Escape),
            (r'\\["%\'{\\[]', String.Escape),

            # Control characters
            (r'[\u0000-\u001f]+', Whitespace),

            # String content (text inside quotes)
            (r'[^"%\\\u0000-\u001f]+', String),

            # Single characters that might be start of something
            (r'%', String),
            (r'\\', String),
        ],
    }
