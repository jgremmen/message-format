import assert from 'node:assert/strict';
import {describe, it} from 'node:test';

import {balanced, highlight} from './helpers.js';


/** Asserts that `html` contains a span with the given hljs class(es) and text. */
function hasSpan(html, cls, text)
{
  const expected = `<span class="${cls}">${text}</span>`;
  assert.ok(html.includes(expected), `expected ${expected} in\n${html}`);
}

const span = (cls, text) => `<span class="${cls}">${text}</span>`;
const q = '&#x27;';
const quote = text => span('hljs-punctuation quote_', q) + span('hljs-string', text) + span('hljs-punctuation quote_', q);


describe('text', () => {
  it('top-level text is a string', () =>
    assert.equal(highlight('Hello world'), span('hljs-string', 'Hello world')));

  it('string scope is interrupted by parts', () =>
    assert.match(highlight('a %{b} c'),
        /^<span class="hljs-string">a <\/span><span class="hljs-subst">.*<\/span><span class="hljs-string"> c<\/span>$/));

  for(const escape of [ '\\u0041', '\\x41', '\\\'', '\\"', '\\%', '\\{', '\\\\', '\\[' ])
    it(`escape sequence ${escape}`, () => {
      const html = highlight(`a${escape}b`);
      const text = escape.replace('\'', q).replace('"', '&quot;');

      assert.equal(html, span('hljs-string', `a${span('hljs-char escape_', text)}b`));
    });

  for(const text of [ 'a\\nb', 'a\\' ])
    it(`lone backslash in ${text} is a regular character`, () =>
      assert.equal(highlight(text), span('hljs-string', text)));
});


describe('parameter', () => {
  it('%{name}', () => {
    const html = highlight('%{name}');

    assert.equal(html, span('hljs-subst',
        span('hljs-template-tag', '%{') + span('hljs-variable', 'name') + span('hljs-template-tag', '}')));
  });

  it('format', () => {
    const html = highlight('%{name,format:fmt}');

    hasSpan(html, 'hljs-keyword', 'format');
    hasSpan(html, 'hljs-title function_', 'fmt');
  });

  it('keyword as format name', () =>
    hasSpan(highlight('%{p,format:true}'), 'hljs-title function_', 'true'));

  for(const keyword of [ 'true', 'false', 'null', 'empty', 'format' ])
    it(`keyword ${keyword} as parameter name`, () =>
      hasSpan(highlight(`%{${keyword}}`), 'hljs-variable', keyword));

  it('map keys', () => {
    const html = highlight('%{p,null:a,!empty:b,true:c,<10:d,<=20:e,=30:f,>1999:g,>=200:h,!25:i,<>\'x\':j}');

    for(const literal of [ 'null', 'empty', 'true' ])
      hasSpan(html, 'hljs-literal', literal);
    for(const op of [ '!', '&lt;', '&lt;=', '=', '&gt;', '&gt;=', '&lt;&gt;' ])
      hasSpan(html, 'hljs-operator', op);
    for(const n of [ '10', '20', '30', '1999', '200', '25' ])
      hasSpan(html, 'hljs-number', n);
    assert.ok(html.includes(quote('x')), html);
    for(const value of [ 'a', 'b', 'c', 'j' ])
      hasSpan(html, 'hljs-string', value);
  });

  it('grouped map keys and default', () => {
    const html = highlight('%{p,(1,2,3):\'x\',:\'y\'}');

    hasSpan(html, 'hljs-punctuation', ',(');
    hasSpan(html, 'hljs-number', '2');
    hasSpan(html, 'hljs-punctuation', '):');
    assert.ok(html.includes(quote('y')), html);
  });

  it('config definitions', () => {
    const html = highlight('%{p,b:true,n:-42,s:value,m:\'msg %{x}\'}');

    for(const name of [ 'b', 'n', 's', 'm' ])
      hasSpan(html, 'hljs-attr', name);
    hasSpan(html, 'hljs-literal', 'true');
    hasSpan(html, 'hljs-number', '-42');
    hasSpan(html, 'hljs-string', 'value');
    assert.ok(html.includes(`${span('hljs-punctuation quote_', q)}${span('hljs-string', 'msg ')}<span class="hljs-subst">`), html);
  });
});


describe('template', () => {
  it('%[tpl]', () =>
    assert.equal(highlight('%[tpl]'), span('hljs-subst',
        span('hljs-template-tag', '%[') + span('hljs-title class_', 'tpl') + span('hljs-template-tag', ']'))));

  it('parameter defaults', () => {
    const html = highlight('%[tpl,n=1,b=true,s=\'x\']');

    for(const name of [ 'n', 'b', 's' ])
      hasSpan(html, 'hljs-variable', name);
    hasSpan(html, 'hljs-operator', '=');
    hasSpan(html, 'hljs-number', '1');
    hasSpan(html, 'hljs-literal', 'true');
    assert.ok(html.includes(quote('x')), html);
  });

  it('parameter delegate', () => {
    const html = highlight('%[tpl,a->b]');

    hasSpan(html, 'hljs-variable', 'a');
    hasSpan(html, 'hljs-operator', '-&gt;');
    hasSpan(html, 'hljs-variable', 'b');
  });

  it('keywords as names', () => {
    const html = highlight('%[empty,null->format,true=false]');

    hasSpan(html, 'hljs-title class_', 'empty');
    hasSpan(html, 'hljs-variable', 'null');
    hasSpan(html, 'hljs-variable', 'format');
    hasSpan(html, 'hljs-variable', 'true');
    hasSpan(html, 'hljs-literal', 'false');
  });
});


describe('post format', () => {
  it('name and config key are classified differently', () => {
    const html = highlight('%(clip,\'%{s,empty:x}\',clip:34)');

    hasSpan(html, 'hljs-title function_', 'clip');
    hasSpan(html, 'hljs-attr', 'clip');
    hasSpan(html, 'hljs-literal', 'empty');
    hasSpan(html, 'hljs-number', '34');
    hasSpan(html, 'hljs-template-tag', '%(');
    hasSpan(html, 'hljs-template-tag', ')');
  });

  it('deep nesting is balanced', () => {
    const html = highlight('%{level1,:\'one %(upper,\'two %{level3,:\'three %[deep,x->y]\'}\')\'}');

    assert.ok(balanced(html), html);
    assert.equal(html.match(/hljs-subst/g).length, 4);
    assert.equal(html.match(/hljs-string/g).length, 3);
    hasSpan(html, 'hljs-variable', 'level1');
    hasSpan(html, 'hljs-title function_', 'upper');
    hasSpan(html, 'hljs-variable', 'level3');
    hasSpan(html, 'hljs-title class_', 'deep');
  });
});
