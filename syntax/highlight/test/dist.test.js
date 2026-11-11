// Smoke tests for the distributables in dist/ (run `npm run build` first).

import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { describe, it } from 'node:test';
import { fileURLToPath, pathToFileURL } from 'node:url';
import vm from 'node:vm';

import hljs from 'highlight.js/lib/core';


const dist = join(dirname(fileURLToPath(import.meta.url)), '../dist');
const MESSAGE = 'Hello %{name,format:fmt} %[tpl,a->b] %(clip,\'x\',size:3)';
const skip = existsSync(dist) ? false : 'dist/ missing, run `npm run build`';


function check(instance)
{
  const html = instance.highlight(MESSAGE, { language: 'message-format' }).value;

  assert.ok(html.includes('<span class="hljs-title function_">fmt</span>'), html);
  assert.ok(html.includes('<span class="hljs-title class_">tpl</span>'), html);
  assert.ok(html.includes('<span class="hljs-attr">size</span>'), html);
}


describe('dist', { skip }, () => {
  for(const file of [ 'message-format.mjs', 'message-format.lib.mjs' ])
    it(file, async () => {
      const { default: messageFormat } = await import(pathToFileURL(join(dist, file)));
      const instance = hljs.newInstance();

      instance.registerLanguage('message-format', messageFormat);
      check(instance);
    });

  it('message-format.min.js registers itself with a global hljs', () => {
    const instance = hljs.newInstance();
    const context = vm.createContext({ hljs: instance });

    vm.runInContext(readFileSync(join(dist, 'message-format.min.js'), 'utf8'), context);

    assert.equal(typeof context.hljsMessageFormat, 'function');
    assert.equal(typeof context.hljsMessageFormat.create, 'function');
    check(instance);
  });

  it('stylesheets', () => {
    assert.match(readFileSync(join(dist, 'message-format.css'), 'utf8'),
        /\.language-message-format \.hljs-char\.escape_ \{/);
    assert.match(readFileSync(join(dist, 'message-format-delimiters.css'), 'utf8'),
        /\.language-message-format \.hljs-template-tag \{/);
  });

  it('message-format.min.js without a global hljs', () => {
    const context = vm.createContext({});

    vm.runInContext(readFileSync(join(dist, 'message-format.min.js'), 'utf8'), context);

    const instance = hljs.newInstance();
    instance.registerLanguage('message-format', context.hljsMessageFormat);
    check(instance);
  });
});
