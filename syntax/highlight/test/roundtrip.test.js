import assert from 'node:assert/strict';
import { describe, it } from 'node:test';

import { balanced, highlight, textOf } from './helpers.js';


const MESSAGES = [
  '',
  'Simple text',
  'HTML-special characters <b>&amp;</b> & "x" < > in text and %{p,:\'<i>\'}',
  'Map keys %{count, 0:none, <5:\'a few\', >=5:many, !null:\'set\', (7,8):lucky}',
  'Templates %[greeting, user->name, polite=true]%[footer] and %[t , a -> b]',
  'Post format %(upper,\'%{name}\',locale:de)',
  'Control\tcharacters\u0001 in %{p,\n  a:\t\'x\'\r\n} and %[t,\u0007x=1]',
  'Emoji 😀👍🏽 in text %{p,\'😀\':\'smile 😀\'}',
  'Supplementary letters in names %{𝔸𝔹,𝔠:\'𝕏\'} %[𝔱𝔭𝔩]',
  'Quotes "double %{x}" and \'single\' with escapes \\\' \\" \\% \\{ \\\\ \\[ \\x41 \\u0041',
  'Deep %{level1,:\'one %(upper,\'two %{level3,:\'three %[deep,x->y]\'}\')\'}'
];


describe('round trip', () => {
  for(const message of MESSAGES)
    it(JSON.stringify(message), () => {
      const html = highlight(message);

      assert.equal(textOf(html), message);
      assert.ok(balanced(html), html);
    });
});
