// Snapshot tests. The messages come from the Java test resources (read in place, so both
// implementations work on the same corpus) and from test/fixtures/*.msg. Run with
// `UPDATE_SNAPSHOTS=1 node --test test/` (or `npm run test:update`) to regenerate the snapshots.

import assert from 'node:assert/strict';
import { existsSync, mkdirSync, readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { describe, it } from 'node:test';
import { fileURLToPath } from 'node:url';

import { balanced, highlight, textOf } from './helpers.js';


const testDir = dirname(fileURLToPath(import.meta.url));
const resourceDir = resolve(testDir, '../../../message-format/src/test/resources');
const fixtureDir = join(testDir, 'fixtures');
const snapshotDir = join(testDir, 'snapshots');
const update = !!process.env.UPDATE_SNAPSHOTS;


/** Parses a Java properties file (ISO-8859-1, single-line entries). */
function properties(file)
{
  const result = new Map();

  for(const line of readFileSync(file, 'latin1').split(/\r?\n/))
  {
    if (/^\s*([#!]|$)/.test(line))
      continue;

    const [ , key, value ] = /^\s*([^=:\s]+)\s*[=:]?\s*(.*)$/.exec(line);
    result.set(key, value.replace(/\\(u[0-9a-fA-F]{4}|.)/g, (_, e) =>
        e.length === 5 ? String.fromCharCode(parseInt(e.slice(1), 16))
                       : ({ t: '\t', n: '\n', r: '\r', f: '\f' })[e] ?? e));
  }

  return result;
}


function corpus()
{
  const sets = [];

  for(const file of [ 'message.properties', 'template.properties' ])
    sets.push({ name: file, messages: properties(join(resourceDir, file)) });

  const fixtures = new Map(readdirSync(fixtureDir)
      .filter(f => f.endsWith('.msg'))
      .sort()
      .map(f => [ f.slice(0, -4), readFileSync(join(fixtureDir, f), 'utf8').replace(/\n$/, '') ]));
  sets.push({ name: 'fixtures', messages: fixtures });

  return sets;
}


for(const { name, messages } of corpus())
  describe(`snapshot ${name}`, () => {
    const file = join(snapshotDir, `${name}.json`);
    const snapshot = existsSync(file) ? JSON.parse(readFileSync(file, 'utf8')) : {};
    const actual = {};

    for(const [ key, message ] of messages)
      it(key, () => {
        const html = actual[key] = highlight(message);

        assert.equal(textOf(html), message);
        assert.ok(balanced(html), html);

        if (!update)
          assert.equal(html, snapshot[key], `snapshot ${name}/${key} differs (UPDATE_SNAPSHOTS=1 to update)`);
      });

    if (update)
      it('write snapshot', () => {
        mkdirSync(snapshotDir, { recursive: true });
        writeFileSync(file, JSON.stringify(actual, null, 2) + '\n');
      });
  });
