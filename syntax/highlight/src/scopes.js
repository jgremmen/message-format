/*
 * Copyright 2026 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {Token} from 'antlr4';

import MessageLexer from './generated/MessageLexer.js';
import MessageParser from './generated/MessageParser.js';


const L = MessageLexer;

const DELIMITERS = new Set([
  L.P_START, L.P_END, L.TPL_START, L.TPL_END, L.PF_START, L.PF_END
]);

const PUNCTUATION = new Set([ L.COMMA, L.COLON, L.L_PAREN, L.R_PAREN ]);
const OPERATORS = new Set([ L.EQ, L.NE, L.LT, L.LTE, L.GT, L.GTE, L.ARROW ]);
const LITERALS = new Set([ L.BOOL, L.NULL, L.EMPTY ]);
const QUOTES = new Set([ L.SQ_START, L.SQ_END, L.DQ_START, L.DQ_END ]);


/**
 * Returns `true` if the given `CH` token represents an escape sequence.
 */
export function isEscape(token) {
  return token.type === L.CH && token.text.startsWith('\\');
}


/**
 * Classifies a token by its lexer token type only. This is used for tokens which are not reached
 * through a valid parser rule (error recovery) and as a fallback if parsing fails altogether.
 *
 * @returns {string|null}  hljs scope or `null` for plain text
 */
export function tokenTypeScope(token)
{
  const type = token.type;

  if (DELIMITERS.has(type))
    return 'template-tag';
  if (PUNCTUATION.has(type))
    return 'punctuation';
  if (OPERATORS.has(type))
    return 'operator';
  if (LITERALS.has(type))
    return 'literal';
  if (type === L.NUMBER)
    return 'number';
  if (type === L.FORMAT)
    return 'keyword';
  if (type === L.NAME)
    return 'variable';
  if (QUOTES.has(type))
    return 'punctuation.quote';
  if (type === L.CH)
    return isEscape(token) ? 'char.escape' : 'string';

  return null;
}


const P = MessageParser;

const PARTS = new Set([ P.RULE_parameterPart, P.RULE_templatePart, P.RULE_postFormatPart ]);

// scopes for names, by the rule enclosing the `nameOrKeyword` or the terminal itself
const NAME_SCOPES = new Map([
  [ P.RULE_parameterName, 'variable' ],
  [ P.RULE_parameterFormat, 'title.function' ],
  [ P.RULE_templateName, 'title.class' ],
  [ P.RULE_postFormatName, 'title.function' ],
  [ P.RULE_templateParameterDelegate, 'variable' ],
  [ P.RULE_templateParameterDefault, 'variable' ],
  [ P.RULE_configDefinition, 'attr' ],
  [ P.RULE_simpleString, 'string' ]
]);


/**
 * Classifies a terminal inside a valid rule context. Plain text characters get no scope of their
 * own, as they are emitted inside the `string` scope of their `text` container.
 */
function terminalScope(token, parent)
{
  switch(token.type)
  {
    case L.CH:
      return isEscape(token) ? 'char.escape' : null;

    case L.FORMAT:
      if (parent.ruleIndex === P.RULE_parameterFormat)
        return 'keyword';
      // fall through

    case L.NAME:
    case L.BOOL:
    case L.NULL:
    case L.EMPTY:
      if (parent.ruleIndex === P.RULE_nameOrKeyword)
        return NAME_SCOPES.get(parent.parentCtx?.ruleIndex) ?? tokenTypeScope(token);
      if (parent.ruleIndex === P.RULE_configDefinition && token.type === L.NAME)
        return 'attr';

      return tokenTypeScope(token);

    default:
      return tokenTypeScope(token);
  }
}


/**
 * Walks the parse tree and classifies every token reached.
 *
 * @returns {{ scopes: Map<number,string|null>, opens: Map<number,string[]>,
 *             closes: Map<number,number>, errors: Set<number> }}
 *   `scopes` maps token indices to their scope; `opens`/`closes` hold the container scopes
 *   to open before and the number of scopes to close after a token; `errors` contains the
 *   indices of tokens in error nodes
 */
export function classify(tree)
{
  const scopes = new Map();
  const opens = new Map();
  const closes = new Map();
  const errors = new Set();

  const container = (ctx, scope) => {
    const start = ctx.start, stop = ctx.stop;

    if (start && start.type !== Token.EOF && start.tokenIndex >= 0)
    {
      if (!opens.has(start.tokenIndex))
        opens.set(start.tokenIndex, []);
      opens.get(start.tokenIndex).push(scope);

      // a context without a valid stop token is closed at the end of the input
      if (stop && stop.tokenIndex >= start.tokenIndex && stop.type !== Token.EOF)
        closes.set(stop.tokenIndex, (closes.get(stop.tokenIndex) ?? 0) + 1);
    }
  };

  const walk = node => {
    if (node.symbol)  // terminal or error node
    {
      const token = node.symbol;

      // tokens conjured by error recovery have no index and no text in the input
      if (token.tokenIndex >= 0 && token.type !== Token.EOF)
      {
        if (node.isErrorNode?.())  // only defined on error nodes
        {
          scopes.set(token.tokenIndex, tokenTypeScope(token));
          errors.add(token.tokenIndex);
        }
        else
          scopes.set(token.tokenIndex, terminalScope(token, node.parentCtx));
      }

      return;
    }

    if (PARTS.has(node.ruleIndex))
      container(node, 'subst');
    else if (node.ruleIndex === P.RULE_text)  // the quotes around quoted text are not part of it
      container(node, 'string');

    for(const child of node.children ?? [])
      walk(child);
  };

  walk(tree);

  return { scopes, opens, closes, errors };
}
