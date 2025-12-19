# -*- coding: utf-8 -*-
"""Test suite for MessageFormat Pygments lexer"""

import pytest
from pygments.token import (
    Text, Keyword, Name, String, Number, Operator, Punctuation, Whitespace
)

import sys
import os
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '../..')))
from syntax.pygments.messageformat_lexer import MessageFormatLexer


def tokenize(code):
    """Helper function to tokenize code and return list of (token_type, value) tuples."""
    lexer = MessageFormatLexer()
    return list(lexer.get_tokens(code))


def has_token_type(tokens, token_type):
    """Check if a specific token type exists in the token list."""
    return any(t[0] == token_type for t in tokens)


def get_tokens_by_type(tokens, token_type):
    """Get all tokens of a specific type."""
    return [t for t in tokens if t[0] == token_type]


class TestMessageProperties:
    """Test cases based on message.properties examples."""

    @pytest.mark.parametrize("msg_id,message", [
        ("MSG-001", ""),
        ("MSG-002", "Simple text"),
        ("MSG-003", r"Strange characters: ^~`@\\uD83D\\uDE00"),
        ("MSG-004", r"Enum key type %{p,empty:empty,!empty:'not empty'}"),
        ("MSG-005", r"Name key type %{p,richtig:true,size:-678764782,message:'msg %{m}'}."),
        ("MSG-006", r'Number key type %{p, <10:A, <=20:B, =30:C, >1999:D, >=200:E, !25:F}'),
        ("MSG-007", r"String key type %{p,mychar, <'A':A, >'Z':Z, :P}"),
        ("MSG-008", r"Something went wrong while parsing %[result,n=count,p:true]%[ex-colon]"),
        ("MSG-009", r"Number value type %{p,hh:-9223372036854775808,ii:9223372036854775807}"),
    ])
    def test_message_tokenization(self, msg_id, message):
        """Test that messages from message.properties can be tokenized without errors."""
        tokens = tokenize(message)

        if message:
            assert len(tokens) > 0, f"{msg_id}: Should produce tokens"
            non_newline_tokens = [t for t in tokens if t[1] != '\n']
            assert len(non_newline_tokens) > 0, f"{msg_id}: Should have non-newline tokens"

    def test_msg004_enum_keys(self):
        """MSG-004: Enum key types with operators."""
        message = r"%{p,empty:empty,!empty:'not empty',true:true,false:false}"
        tokens = tokenize(message)

        assert has_token_type(tokens, Punctuation.Special)
        assert has_token_type(tokens, Keyword.Constant)
        assert has_token_type(tokens, Operator)
        assert has_token_type(tokens, String.Delimiter)

    def test_msg005_nested_parameters(self):
        """MSG-005: Nested parameters in strings."""
        message = r"%{p,message:'msg %{m}'}"
        tokens = tokenize(message)

        param_markers = get_tokens_by_type(tokens, Punctuation.Special)
        assert len(param_markers) >= 4, "Should have opening and closing markers for both parameters"
        assert has_token_type(tokens, String.Delimiter)
        assert has_token_type(tokens, Name)

    def test_msg005_negative_number(self):
        """MSG-005: Negative numbers."""
        message = r"%{p,size:-678764782}"
        tokens = tokenize(message)

        number_tokens = get_tokens_by_type(tokens, Number.Integer)
        assert any('-678764782' in t[1] for t in number_tokens), "Should recognize negative number"

    def test_msg006_relational_operators(self):
        """MSG-006: All relational operators."""
        message = r"%{p, <10:A, <=20:B, =30:C, >1999:D, >=200:E, !25:F}"
        tokens = tokenize(message)

        operator_tokens = get_tokens_by_type(tokens, Operator)
        operator_values = [t[1] for t in operator_tokens]

        assert '<' in operator_values or any('<' in v for v in operator_values)
        assert '>' in operator_values or any('>' in v for v in operator_values)
        assert '=' in operator_values or any('=' in v for v in operator_values)

    def test_msg008_templates(self):
        """MSG-008: Template syntax."""
        message = r"%[result,n=count,p:true]%[ex-colon]"
        tokens = tokenize(message)

        special_tokens = get_tokens_by_type(tokens, Punctuation.Special)
        special_values = [t[1] for t in special_tokens]

        assert '%[' in special_values, "Should recognize template start"
        assert ']' in special_values, "Should recognize template end"
        assert has_token_type(tokens, Keyword.Constant)


class TestTemplateProperties:
    """Test cases based on template.properties examples."""

    def test_ex_colon_template(self):
        """Test ex-colon template with nested parameter."""
        message = r"%{ex,!empty:': %{ex}'}"
        tokens = tokenize(message)

        assert has_token_type(tokens, Punctuation.Special)
        keyword_tokens = get_tokens_by_type(tokens, Keyword.Constant)
        keyword_values = [t[1] for t in keyword_tokens]
        assert 'empty' in keyword_values
        assert has_token_type(tokens, Operator)

    def test_result_template(self):
        """Test result template with multiple conditions."""
        message = r"%{n,1:'1 result',:'%{n} results'}"
        tokens = tokenize(message)

        assert has_token_type(tokens, Punctuation.Special)
        number_tokens = get_tokens_by_type(tokens, Number.Integer)
        assert any('1' in t[1] for t in number_tokens)
        assert has_token_type(tokens, String)


class TestEdgeCases:
    """Test edge cases and special scenarios."""

    def test_empty_strings(self):
        """Test empty single and double quoted strings in parameters."""
        # In root mode, quotes are just text, not string delimiters
        # Test empty strings in parameter mode where they are actual strings
        tokens_sq = tokenize("%{p,x:''}")
        tokens_dq = tokenize('%{p,x:""}')

        assert has_token_type(tokens_sq, String.Delimiter)
        assert has_token_type(tokens_dq, String.Delimiter)

    def test_deeply_nested_structures(self):
        """Test deeply nested parameters in strings."""
        message = r"%{a,'outer %{b,'inner %{c}'}'}"
        tokens = tokenize(message)

        param_markers = get_tokens_by_type(tokens, Punctuation.Special)
        assert len(param_markers) >= 6, "Should have markers for all nested parameters"

    def test_all_escape_sequences(self):
        """Test all supported escape sequences."""
        test_cases = [
            (r"\\u0041", String.Escape),
            (r"\\\"", String.Escape),
            (r"\\'", String.Escape),
            (r"\\%", String.Escape),
            (r"\\{", String.Escape),
            (r"\\[", String.Escape),
            (r"\\\\", String.Escape),
        ]

        for escape_seq, expected_token in test_cases:
            tokens = tokenize(escape_seq)
            assert has_token_type(tokens, expected_token), f"Should recognize {escape_seq}"

    def test_hyphenated_names(self):
        """Test names with hyphens."""
        tokens = tokenize("%[ex-colon]")

        name_tokens = get_tokens_by_type(tokens, Name)
        name_values = [t[1] for t in name_tokens]
        assert 'ex-colon' in name_values, "Should recognize hyphenated name"

    def test_names_with_underscores(self):
        """Test names with underscores."""
        tokens = tokenize("%{my_name}")

        name_tokens = get_tokens_by_type(tokens, Name)
        name_values = [t[1] for t in name_tokens]
        assert 'my_name' in name_values, "Should recognize name with underscore"

    def test_lexer_registration(self):
        """Test that lexer can be retrieved by alias."""
        lexer = MessageFormatLexer()
        assert lexer.name == 'Message Format'
        assert 'msgfmt' in lexer.aliases
        assert 'message-format' in lexer.aliases
        assert '*.mfp' in lexer.filenames


if __name__ == '__main__':
    pytest.main([__file__, '-v'])

