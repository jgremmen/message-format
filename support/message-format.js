/** @type LanguageFn */
export default function(hljs) {
    const regex = hljs.regex;
    const ESCAPE_RE = regex.concat('\\\\', regex.either('u[0-9a-fA-F]{4}', '["\'%{\\[]'));
    const NAME_START_CHAR_RE = '[a-zA-Z\\u00C0-\\u024F\\u0400-\\u04FF\\u0370-\\u03FF]';
    const NAME_CHAR_RE = regex.either(NAME_START_CHAR_RE, '[0-9\\u0660-\\u0669\\u06F0-\\u06F9\\u0966-\\u096F]', '_');
    const NAME_RE = regex.concat(
        NAME_START_CHAR_RE,
        regex.anyNumberOfTimes(NAME_CHAR_RE),
        regex.anyNumberOfTimes(regex.concat('-', NAME_CHAR_RE + '+')));
    const NUMBER_RE = '-?\\d+';
    const PARAMETER_START_RE = '%{';
    const PARAMETER_END_RE = '}';
    const TEMPLATE_START_RE = '%[';
    const TEMPLATE_END_RE = ']';
    const EQUAL_OPERATOR_RE = regex.either('=', '!', '<>');
    const RELATIONAL_OPERATOR_RE = regex.either(EQUAL_OPERATOR_RE, '<', '<=', '>', '>=');
    const NULL_KEY_RE = regex.concat('null',  regex.lookahead('\\s*:'));
    const EMPTY_KEY_RE = regex.concat('empty',  regex.lookahead('\\s*:'));
    const BOOL_KEY_RE = regex.concat(regex.either('true', 'false'),  regex.lookahead('\\s*:'));
    const NUMBER_KEY_RE = regex.concat(NUMBER_RE,  regex.lookahead('\\s*:'));

    const TEXT = {
        className: "text",
        begin: /^[^ ].+?(?=%{|%\[|(\\u[a-fA-F0-9]{4})|(\\["'%{\\\[])|$)/,
        relevance: 0
    }

    const WHITESPACE = {
        className: "whitespace",
        begin: '[ \\u0000-\\u001f]+',
        relevance: 0
    }

    const ESCAPE = {
        className: "escape",
        begin: ESCAPE_RE
    }

    const NUMBERS = {
        className: "number",
        begin: NUMBER_RE
    }

    const CONFIG_KEY = {
        className: "name",
        begin: regex.concat(NAME_RE, regex.lookahead('\\s*:'))
    }

    const OPERATOR = {
        className: "operator",
        variants: [
            { begin: regex.concat(EQUAL_OPERATOR_RE, regex.lookahead(NULL_KEY_RE)) },
            { begin: regex.concat(EQUAL_OPERATOR_RE, regex.lookahead(EMPTY_KEY_RE)) },
            { begin: regex.concat(RELATIONAL_OPERATOR_RE, regex.lookahead(NUMBER_KEY_RE)) }
        ]
    }

    const QUOTED_STRING = {
        className: "string",
        variants: [
            {
                begin: '"',
                end: '"',
                contains: [
                    TEXT,
                    WHITESPACE,
                    ESCAPE
                ]
            },
            {
                begin: '\'',
                end: '\'',
                contains: [
                    TEXT,
                    WHITESPACE,
                    ESCAPE
                ]
            }
        ]
    };

    const PARAMETER_PART = {
        scope: "parameter",
        className: "parameter",
        begin: PARAMETER_START_RE,
        end: PARAMETER_END_RE,
        returnBegin: true,
        returnEnd: true,
        contains: [
            OPERATOR,
            NUMBERS,
            {
                beginKeywords: "true false null empty"
            },
            CONFIG_KEY,
            QUOTED_STRING,
            {
                className: "punctuation",
                begin: /[,:=()]/
            },
            {
                className: "parameter-start",
                begin: PARAMETER_START_RE
            },
            {
                className: "parameter-end",
                begin: PARAMETER_END_RE
            }
        ],
        relevance: 5
    };

    QUOTED_STRING.variants[0].contains.push(PARAMETER_PART);
    QUOTED_STRING.variants[1].contains.push(PARAMETER_PART);

    const MESSAGE = {
        scope: "message",
        className: "message",
        contains: [
            TEXT,
            WHITESPACE,
            ESCAPE,
            PARAMETER_PART
        ]
    };

    return {
        name: 'message-format',
        case_insensitive: false,
        contains: [
            MESSAGE
        ]
    };
}