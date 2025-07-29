/** @type LanguageFn */
export default function(hljs) {
    const regex = hljs.regex;
    const ESCAPE_RE = regex.either('\\\\u[0-9a-fA-F]{4}', '\\\\["\'%{\\[]');
    const NAME_START_CHAR_RE = '\\p{L}';
    const NAME_CHAR_RE = regex.either(NAME_START_CHAR_RE, '\\p{N}', '_');
    const NAME_RE = regex.concat(
        NAME_START_CHAR_RE,
        regex.anyNumberOfTimes(NAME_CHAR_RE),
        regex.anyNumberOfTimes(regex.concat('-', NAME_CHAR_RE, '+')));
    const PARAMETER_START_RE = '%{';
    const PARAMETER_END_RE = '}';
    const TEMPLATE_START_RE = '%[';
    const TEMPLATE_END_RE = ']';

    const TEXT = {
        className: "text",
        begin: /^[^ ].+?(?=%{|%\[|(\\u[a-fA-F0-9]{4})|(\\["'%{\\\[])|$)/,
        relevance: 0
    }

    const WHITESPACE = {
        className: "whitespace",
        begin: '\\s+',
        relevance: 0
    }

    const ESCAPE = {
        className: "string-escape",
        begin: ESCAPE_RE
    }

    const NUMBERS = {
        className: "number",
        begin: '-?\\d+'
    }

    const CONFIG_KEY = {
        className: "name",
        begin: regex.concat(NAME_RE, regex.lookahead(':'))
    }

    const STRING = {

    }

    const PARAMETER = {
        scope: "parameter",
        className: "parameter",
        begin: /%{/,
        end: /}/,
        returnBegin: true,
        returnEnd: true,
        contains: [
            NUMBERS,
            {
                beginKeywords: "true false null empty"
            },
            CONFIG_KEY,
            {
                className: "punctuation",
                begin: /[,:=]/
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

    return {
        name: 'message-format',
        case_insensitive: false,
        contains: [
            TEXT,
            WHITESPACE,
            ESCAPE,
            PARAMETER,
        ]
    };
}