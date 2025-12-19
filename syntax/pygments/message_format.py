# -*- coding: utf-8 -*-
"""
    pygments.lexers.messageformat
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    Lexer for MessageFormat language.

    Based on MessageLexer.g4 and MessageParser.g4 from the message-format library.

    Implements parser-aware context tracking to distinguish between:
    - Parameter names (first position in %{...}) -> Name.Variable
    - Format names (second position in parameters) -> Name.Function
    - Template names (first position in %[...]) -> Name.Class
    - Config keys (in name:value patterns) -> Name.Attribute

    :copyright: Copyright 2020 Jeroen Gremmen
    :license: Apache License 2.0
"""

from pygments.lexer import RegexLexer, bygroups, include
from pygments.token import (
    Text, Comment, Operator, Keyword, Name, String, Number, Punctuation, Whitespace
)

__all__ = ['MessageFormatLexer']


class MessageFormatLexer(RegexLexer):
    """
    Lexer for MessageFormat language.

    MessageFormat is a powerful message formatting library that supports
    parameters (%{...}), templates (%[...]), and localized messages.

    This lexer implements context-aware highlighting based on both
    MessageLexer.g4 and MessageParser.g4, distinguishing between:
    - Parameter names (first position in %{...})
    - Format names (second position in parameters)
    - Template names (first position in %[...])
    - Config keys (in name:value patterns)

    .. versionadded:: 2.15
    """

    name = 'Message Format'
    aliases = ['msgfmt', 'message-format']
    filenames = ['*.mfp']
    mimetypes = ['application/x-message-format']

    # Unicode character classes approximation
    # NameStartChar: Letters (Latin Extended)
    _name_start = r'[a-zA-Z\u00C0-\u024F\u1E00-\u1EFF]'
    # NameChar: Letters, Digits, Underscore
    _name_char = r'[a-zA-Z0-9\u00C0-\u024F\u1E00-\u1EFF_]'
    # Name: NameStartChar NameChar* ('-' NameChar+)*
    _name = _name_start + _name_char + r'*(?:-' + _name_char + r'+)*'

    tokens = {
        'root': [
            # Control characters (skipped in ANTLR, shown as whitespace here)
            (r'[\u0000-\u001f]+', Whitespace),

            # Parameter start: %{ -> push to parameter mode
            (r'%\{', Punctuation.Special, 'parameter'),

            # Template start: %[ -> push to template mode
            (r'%\[', Punctuation.Special, 'template'),

            # Escape sequences
            (r'\\u[0-9a-fA-F]{4}', String.Escape),
            (r'\\["%\'{\\[]', String.Escape),

            # Regular text (Character fragment approximation)
            # Whitespace (Unicode Zs approximation)
            (r'[ \t\u00A0]+', Text),
            # Letters, numbers, punctuation, symbols
            (r'[a-zA-Z\u00C0-\u024F\u1E00-\u1EFF]+', Text),
            (r'[0-9]+', Text),
            (r'[!\"#$%&()*+,\-./:;<=>?@\\\^_`{|}~¡-¿×÷±°§¶†‡•‰€£¥]', Text),

            # Any other character
            (r'.', Text),
        ],

        # Parameter mode - first position is parameter name (Name.Variable)
        'parameter': [
            # Parameter end: } -> pop back
            (r'\}', Punctuation.Special, '#pop'),

            # First comma transitions to format state
            (r',', Punctuation, 'parameter-format'),

            # Colon before comma means no format (go to config state)
            (r':', Punctuation, 'parameter-config'),

            # Single quoted string -> push to single-quote mode
            (r"'", String.Delimiter, 'single-quote'),

            # Double quoted string -> push to double-quote mode
            (r'"', String.Delimiter, 'double-quote'),

            # Parameter name (first position) - highlighted as Name.Variable
            # Keywords can be parameter names per nameOrKeyword rule
            (r'\b(?:true|false)\b', Name.Variable),
            (r'\bnull\b', Name.Variable),
            (r'\bempty\b', Name.Variable),
            (_name, Name.Variable),

            # Whitespace (ignored/skipped)
            (r'[ \t\u0000-\u001f]+', Text),
        ],

        # After first comma: format name (Name.Function) OR config elements
        'parameter-format': [
            (r'\}', Punctuation.Special, ('#pop', '#pop')),
            (r',', Punctuation, ('#pop', 'parameter-config')),
            (r':', Punctuation, ('#pop', 'parameter-config')),

            # If we see an operator, this isn't a format, transition to config
            (r'<=', Operator, ('#pop', 'parameter-config')),
            (r'>=', Operator, ('#pop', 'parameter-config')),
            (r'<>', Operator, ('#pop', 'parameter-config')),
            (r'<', Operator, ('#pop', 'parameter-config')),
            (r'>', Operator, ('#pop', 'parameter-config')),
            (r'=', Operator, ('#pop', 'parameter-config')),
            (r'!', Operator, ('#pop', 'parameter-config')),

            # If we see a number, this is config (not a format)
            (r'-?[0-9]+', Number.Integer, ('#pop', 'parameter-config')),

            # If we see a parenthesis, this is config (grouped keys)
            (r'\(', Punctuation, ('#pop', 'parameter-config')),

            (r"'", String.Delimiter, 'single-quote'),
            (r'"', String.Delimiter, 'double-quote'),

            # Format name - highlighted as Name.Function
            (r'\b(?:true|false)\b', Name.Function),
            (r'\bnull\b', Name.Function),
            (r'\bempty\b', Name.Function),
            (_name, Name.Function),

            (r'[ \t\u0000-\u001f]+', Text),
        ],

        # Config elements: key:value or operators with values
        'parameter-config': [
            (r'\}', Punctuation.Special, ('#pop', '#pop')),
            (r',', Punctuation),

            # Parentheses for grouped map keys
            (r'\(', Punctuation),
            (r'\)', Punctuation),

            # Colon (for name:value patterns)
            (r':', Punctuation),

            # Keywords
            (r'\b(?:true|false)\b', Keyword.Constant),
            (r'\bnull\b', Keyword.Constant),
            (r'\bempty\b', Keyword.Constant),

            # Operators (order matters: longest first)
            (r'<=', Operator),
            (r'>=', Operator),
            (r'<>', Operator),
            (r'<', Operator),
            (r'>', Operator),
            (r'=', Operator),
            (r'!', Operator),

            # Numbers
            (r'-?[0-9]+', Number.Integer),

            # Strings
            (r"'", String.Delimiter, 'single-quote'),
            (r'"', String.Delimiter, 'double-quote'),

            # Config key names - highlighted as Name.Attribute
            (_name, Name.Attribute),

            (r'[ \t\u0000-\u001f]+', Text),
        ],

        # Template mode - first position is template name (Name.Class)
        'template': [
            # Template end: ] -> pop back
            (r'\]', Punctuation.Special, '#pop'),

            # First comma transitions to template config state
            (r',', Punctuation, 'template-config'),

            # Strings
            (r"'", String.Delimiter, 'single-quote'),
            (r'"', String.Delimiter, 'double-quote'),

            # Template name (first position) - highlighted as Name.Class
            (r'\b(?:true|false)\b', Name.Class),
            (r'\bnull\b', Name.Class),
            (r'\bempty\b', Name.Class),
            (_name, Name.Class),

            (r'[ \t\u0000-\u001f]+', Text),
        ],

        # Template config: named configs or parameter delegation (param=delegated)
        'template-config': [
            (r'\]', Punctuation.Special, ('#pop', '#pop')),
            (r',', Punctuation),
            (r':', Punctuation),

            # Equals for parameter delegation (param=delegated)
            (r'=', Operator),

            # Keywords
            (r'\b(?:true|false)\b', Keyword.Constant),
            (r'\bnull\b', Keyword.Constant),
            (r'\bempty\b', Keyword.Constant),

            # Numbers
            (r'-?[0-9]+', Number.Integer),

            # Strings
            (r"'", String.Delimiter, 'single-quote'),
            (r'"', String.Delimiter, 'double-quote'),

            # Config names - highlighted as Name.Attribute
            (_name, Name.Attribute),

            (r'[ \t\u0000-\u001f]+', Text),
        ],

        'single-quote': [
            # Single quote end -> pop back
            (r"'", String.Delimiter, '#pop'),

            # Parameter start in string: %{ -> push to parameter mode
            (r'%\{', Punctuation.Special, 'parameter'),

            # Template start in string: %[ -> push to template mode
            (r'%\[', Punctuation.Special, 'template'),

            # Escape sequences
            (r'\\u[0-9a-fA-F]{4}', String.Escape),
            (r'\\["%\'{\\[]', String.Escape),

            # Control characters
            (r'[\u0000-\u001f]+', Whitespace),

            # String content (any character except quote, %, backslash)
            (r'[^\'%\\]+', String),

            # Single characters that might be start of something
            (r'%', String),
            (r'\\', String),
        ],

        'double-quote': [
            # Double quote end -> pop back
            (r'"', String.Delimiter, '#pop'),

            # Parameter start in string: %{ -> push to parameter mode
            (r'%\{', Punctuation.Special, 'parameter'),

            # Template start in string: %[ -> push to template mode
            (r'%\[', Punctuation.Special, 'template'),

            # Escape sequences
            (r'\\u[0-9a-fA-F]{4}', String.Escape),
            (r'\\["%\'{\\[]', String.Escape),

            # Control characters
            (r'[\u0000-\u001f]+', Whitespace),

            # String content (any character except quote, %, backslash)
            (r'[^"%\\]+', String),

            # Single characters that might be start of something
            (r'%', String),
            (r'\\', String),
        ],
    }
