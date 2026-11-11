// Creates JavaScript-target-safe copies of the message-format grammars.
//
// The original *.g4 files are only read, never modified. The cleaned copies are written to
// build/grammar/. Removed from the parser grammar:
//   - the @header { ... } block (Java imports)
//   - all `returns [ ... ]` clauses (Java types)
//   - `language = Java;` from the options block
// Any other action ({ ... } or @name { ... }) is reported as an error, as it would be new
// target-specific code that needs a decision.

import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const grammarDir = resolve(root, '../../message-format/src/main/antlr');
const outDir = join(root, 'build/grammar');


/**
 * Splits grammar source into code and non-code segments (comments, string literals and
 * character sets), so the scanner never mistakes a bracket inside a literal for structure.
 */
function segments(src)
{
  const result = [];
  let code = '';
  let i = 0;

  const flush = () => { if (code) { result.push({ code: true, text: code }); code = ''; } };
  const literal = end => {
    let j = i + 1;
    while (j < src.length && src[j] !== end)
      j += src[j] === '\\' ? 2 : 1;
    flush();
    result.push({ code: false, text: src.slice(i, j + 1) });
    i = j + 1;
  };

  while (i < src.length)
  {
    const c = src[i];

    if (c === '/' && src[i + 1] === '/')
    {
      const j = src.indexOf('\n', i);
      const end = j < 0 ? src.length : j;
      flush();
      result.push({ code: false, text: src.slice(i, end) });
      i = end;
    }
    else if (c === '/' && src[i + 1] === '*')
    {
      const j = src.indexOf('*/', i + 2);
      const end = j < 0 ? src.length : j + 2;
      flush();
      result.push({ code: false, text: src.slice(i, end) });
      i = end;
    }
    else if (c === '\'')
      literal('\'');
    else if (c === '[' && !/returns\s*$/.test(code))
      literal(']');
    else
    {
      code += c;
      i++;
    }
  }

  flush();
  return result;
}


/** Returns the index after the bracket matching the one at `start` (code only). */
function matchBracket(text, start, open, close)
{
  let depth = 0;

  for(let i = start; i < text.length; i++)
    if (text[i] === open)
      depth++;
    else if (text[i] === close && --depth === 0)
      return i + 1;

  throw new Error(`unbalanced '${open}' at offset ${start}`);
}


function cleanParser(src, file)
{
  // joining code segments with placeholders keeps literals out of the structural scan
  const parts = segments(src);
  const literals = [];
  let code = parts
      .map(p => p.code ? p.text : `\u0000${literals.push(p.text) - 1}\u0000`)
      .join('');

  // @header { ... }
  for(let m; (m = /@header\s*\{/.exec(code)); )
  {
    const open = m.index + m[0].length - 1;
    code = code.slice(0, m.index) + code.slice(matchBracket(code, open, '{', '}'));
  }

  // returns [ ... ]  (the scanner kept these brackets as code)
  for(let m; (m = /\s*returns\s*\[/.exec(code)); )
  {
    const open = m.index + m[0].length - 1;
    code = code.slice(0, m.index) + code.slice(matchBracket(code, open, '[', ']'));
  }

  // options { language = Java; }
  code = code.replace(/\blanguage\s*=\s*Java\s*;[ \t]*\r?\n?/g, '');

  // the only braces left must belong to `options { ... }`; anything else is an action
  for(let i = code.indexOf('{'); i >= 0; i = code.indexOf('{', i))
  {
    const end = matchBracket(code, i, '{', '}');

    if (!/\boptions\s*$/.test(code.slice(0, i)))
      throw new Error(`${file}: unexpected action found: ${code.slice(Math.max(0, i - 20), end)}`);

    i = end;
  }

  return code.replace(/\u0000(\d+)\u0000/g, (_, n) => literals[n]);
}


mkdirSync(outDir, { recursive: true });

const lexer = readFileSync(join(grammarDir, 'MessageLexer.g4'), 'utf8');
writeFileSync(join(outDir, 'MessageLexer.g4'), lexer);

const parser = readFileSync(join(grammarDir, 'MessageParser.g4'), 'utf8');
writeFileSync(join(outDir, 'MessageParser.g4'), cleanParser(parser, 'MessageParser.g4'));

console.log(`prepared grammars in ${outDir}`);
