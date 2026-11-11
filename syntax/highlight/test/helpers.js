import hljs from 'highlight.js/lib/core';

import messageFormat from '../src/index.js';


hljs.registerLanguage('message-format', messageFormat);


/** Highlights `code` and returns the HTML. */
export function highlight(code)
{
  return hljs.highlight(code, { language: 'message-format' }).value;
}


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
