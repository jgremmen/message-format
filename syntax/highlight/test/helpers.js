import assert from 'node:assert/strict';

import hljs from 'highlight.js/lib/core';

import {createMessageFormat} from '../src/index.js';


/** Returns a function which highlights code with the language created with `options` and returns the HTML. */
export function highlighter(options)
{
  const instance = hljs.newInstance();

  instance.registerLanguage('message-format', createMessageFormat(options));

  return code => instance.highlight(code, { language: 'message-format' }).value;
}


/** Highlights `code` with the default options and returns the HTML. */
export const highlight = highlighter();


/** Returns the text content of highlighted HTML. */
export function textOf(html)
{
  return html
      .replace(/<[^>]*>/g, '')
      .replace(/&lt;/g, '<')
      .replace(/&gt;/g, '>')
      .replace(/&quot;/g, '"')
      .replace(/&#x27;/g, '\'')
      .replace(/&amp;/g, '&');
}


/** Returns `true` if all spans in `html` are balanced. */
export function balanced(html)
{
  let depth = 0;

  for(const [ tag ] of html.matchAll(/<\/?span[^>]*>/g))
    if ((depth += tag.startsWith('</') ? -1 : 1) < 0)
      return false;

  return depth === 0;
}


/** Asserts that `html` reproduces `message` as text and has balanced spans. */
export function assertRoundTrip(html, message)
{
  assert.equal(textOf(html), message);
  assert.ok(balanced(html), html);
}
