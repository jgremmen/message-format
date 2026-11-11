import assert from 'node:assert/strict';
import { describe, it, mock } from 'node:test';

import hljs from 'highlight.js/lib/core';

import { createMessageFormat } from '../src/index.js';
import { balanced, highlight, textOf } from './helpers.js';


const INVALID = [
  '%{',
  '%{p,',
  '%{p',
  '%{p,a:',
  '%[t,=]',
  '%[',
  '%(',
  '%(fmt)',
  '%(fmt,x)',
  '%{p,@}',
  '%{p,a:\'unterminated',
  '\'top-level quote',
  '%{p,:\'a %{q,:\'b}',
  '}]) stray closing delimiters',
  '%{p,\u0000\u0001\u001f}',
  '%{p,a:b:c,,}',
  '%{,}',
  '%[t,a->]',
  '%{p,format:}',
  '%{p,(1):x}',
  '%{p,(1,}'
];


describe('invalid input', () => {
  for(const message of INVALID)
    it(JSON.stringify(message), () => {
      const log = mock.method(console, 'log');
      const error = mock.method(console, 'error');
      const warn = mock.method(console, 'warn');

      try {
        const html = highlight(message);

        assert.equal(textOf(html), message);
        assert.ok(balanced(html), html);
        assert.equal(log.mock.callCount() + error.mock.callCount() + warn.mock.callCount(), 0);
      } finally {
        mock.restoreAll();
      }
    });

  it('unexpected characters keep their position', () => {
    const html = highlight('%{p,@}');

    assert.match(html, /<span class="hljs-punctuation">,<\/span>@<span class="hljs-template-tag">}<\/span>/);
  });

  it('valid parts after an error are still classified', () =>
    assert.ok(highlight('%{p,@} %[tpl]').includes('<span class="hljs-title class_">tpl</span>')));
});


describe('markErrors option', () => {
  const marked = hljs.newInstance();
  marked.registerLanguage('message-format', createMessageFormat({ markErrors: true }));

  const highlightMarked = code => marked.highlight(code, { language: 'message-format' }).value;

  it('marks characters rejected by the lexer', () =>
    assert.ok(highlightMarked('%{p,@}').includes('<span class="hljs-deletion">@</span>')));

  it('marks unexpected tokens', () =>
    assert.match(highlightMarked('%{p,a:b:c}'), /<span class="hljs-deletion">.*:.*<\/span>/));

  it('marks nothing in valid messages', () =>
    assert.ok(!highlightMarked('%{p,a:\'x %{y}\'} %[t,a->b]').includes('hljs-deletion')));

  it('keeps the round trip invariant', () => {
    for(const message of INVALID)
    {
      const html = highlightMarked(message);

      assert.equal(textOf(html), message);
      assert.ok(balanced(html), html);
    }
  });
});


describe('robustness', () => {
  it('deeply nested messages do not throw', () => {
    let message = 'x';
    for(let n = 0; n < 2000; n++)
      message = `%{p,:'${message}'}`;

    const html = highlight(message);
    assert.equal(textOf(html), message);
  });

  it('long messages', () => {
    const message = '%{p}.'.repeat(5000);

    assert.equal(textOf(highlight(message)), message);
  });
});
