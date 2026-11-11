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
import { CharStreams, CommonTokenStream, Token } from 'antlr4';

import MessageLexer from './generated/MessageLexer.js';
import MessageParser from './generated/MessageParser.js';
import { classify, tokenTypeScope } from './scopes.js';


/**
 * Buffers text for the hljs emitter, so that adjacent pieces of text with the same scope end up
 * in a single span.
 */
class Output
{
  constructor(emitter)
  {
    this.emitter = emitter;
    this.scope = null;
    this.buffer = '';
    this.depth = 0;
  }

  text(text, scope)
  {
    if (scope !== this.scope)
    {
      this.flush();
      this.scope = scope;
    }

    this.buffer += text;
  }

  open(scope)
  {
    this.flush();
    this.emitter.startScope(scope);
    this.depth++;
  }

  close()
  {
    if (this.depth > 0)
    {
      this.flush();
      this.emitter.endScope();
      this.depth--;
    }
  }

  closeAll()
  {
    while(this.depth > 0)
      this.close();

    this.flush();
  }

  flush()
  {
    if (this.buffer)
    {
      if (this.scope)
      {
        this.emitter.startScope(this.scope);
        this.emitter.addText(this.buffer);
        this.emitter.endScope();
      }
      else
        this.emitter.addText(this.buffer);

      this.buffer = '';
    }
  }
}


/**
 * Lexes the message. Token positions reported by ANTLR are code point indices; `offsets` maps
 * each code point index to its UTF-16 offset in `code`.
 */
function lex(code)
{
  const lexer = new MessageLexer(CharStreams.fromString(code));
  lexer.removeErrorListeners();

  const tokens = new CommonTokenStream(lexer);
  tokens.fill();

  const offsets = [ 0 ];
  let offset = 0;
  for(const cp of code)
    offsets.push(offset += cp.length);

  return { lexer, tokens, offsets };
}


/**
 * Parses the lexed message and classifies all tokens. Returns `null` if parsing fails, in which
 * case tokens are classified by their token type only.
 */
function parse(tokens)
{
  try {
    const parser = new MessageParser(tokens);
    parser.removeErrorListeners();

    const errors = new Set();
    parser.addErrorListener({
      syntaxError: (recognizer, offendingSymbol) => {
        if (offendingSymbol?.tokenIndex >= 0)
          errors.add(offendingSymbol.tokenIndex);
      },
      reportAmbiguity() {},
      reportAttemptingFullContext() {},
      reportContextSensitivity() {}
    });

    const plan = classify(parser.message());
    for(const index of errors)
      plan.errors.add(index);

    return plan;
  } catch(e) {
    return null;
  }
}


/** Returns `true` if a gap between tokens consists of characters rejected by the lexer. */
const isRejected = gap => !/^[\p{Cc}\p{Zs}]*$/u.test(gap);


/**
 * Emits the message `code` to the hljs `emitter`.
 *
 * @param {string} code                   message to highlight
 * @param {object} emitter                hljs token tree emitter
 * @param {object} [options]
 * @param {boolean} [options.markErrors]  wrap syntax errors in an additional `deletion` scope
 */
export function emitMessageFormat(code, emitter, { markErrors = false } = {})
{
  const out = new Output(emitter);
  let cursor = 0;  // UTF-16 offset of the first character not yet emitted

  try {
    const { tokens, offsets } = lex(code);
    const plan = parse(tokens);

    for(const token of tokens.tokens)
    {
      if (token.type === Token.EOF)
        break;

      const start = offsets[token.start];
      const end = offsets[token.stop + 1];

      // characters skipped or rejected by the lexer
      if (start > cursor)
      {
        const gap = code.slice(cursor, start);
        out.text(gap, markErrors && isRejected(gap) ? 'deletion' : null);
      }

      for(const scope of plan?.opens.get(token.tokenIndex) ?? [])
        out.open(scope);

      const text = code.slice(start, end);
      const scope = plan?.scopes.has(token.tokenIndex)
          ? plan.scopes.get(token.tokenIndex)
          : tokenTypeScope(token);  // not reached by the parse tree

      if (markErrors && plan?.errors.has(token.tokenIndex))
      {
        out.open('deletion');
        out.text(text, scope);
        out.close();
      }
      else
        out.text(text, scope);

      cursor = end;

      for(let n = plan?.closes.get(token.tokenIndex) ?? 0; n > 0; n--)
        out.close();
    }

    if (cursor < code.length)
    {
      const gap = code.slice(cursor);
      out.text(gap, markErrors && isRejected(gap) ? 'deletion' : null);
      cursor = code.length;
    }
  } catch(e) {
    // never lose text: whatever has not been emitted yet is emitted as plain text below
  }

  out.closeAll();

  if (cursor < code.length)
    emitter.addText(code.slice(cursor));
}
