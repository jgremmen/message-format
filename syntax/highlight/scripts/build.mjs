// Builds the distributables in dist/ (see DESIGN.md §6):
//   message-format.mjs      ESM, antlr4 runtime bundled
//   message-format.min.js   IIFE for <script> tags, registers itself with a global hljs
//   message-format.lib.mjs  ESM, antlr4 runtime external
//   message-format.css      base stylesheet (escape sequences), for every theme
//   message-format-delimiters.css  optional delimiter fallback stylesheet

import {copyFileSync, mkdirSync, rmSync} from 'node:fs';
import {dirname, join, resolve} from 'node:path';
import {fileURLToPath} from 'node:url';

import * as esbuild from 'esbuild';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const dist = join(root, 'dist');

const common = {
  bundle: true,
  platform: 'browser',
  target: 'es2020',
  legalComments: 'none',
  logLevel: 'warning'
};

rmSync(dist, { recursive: true, force: true });
mkdirSync(dist, { recursive: true });

await Promise.all([
  esbuild.build({
    ...common,
    entryPoints: [ join(root, 'src/index.js') ],
    outfile: join(dist, 'message-format.mjs'),
    format: 'esm',
    minify: true,
    sourcemap: true
  }),
  esbuild.build({
    ...common,
    entryPoints: [ join(root, 'src/browser.js') ],
    outfile: join(dist, 'message-format.min.js'),
    format: 'iife',
    minify: true,
    sourcemap: true
  }),
  esbuild.build({
    ...common,
    entryPoints: [ join(root, 'src/index.js') ],
    outfile: join(dist, 'message-format.lib.mjs'),
    format: 'esm',
    external: [ 'antlr4' ],
    minify: true,
    sourcemap: true
  })
]);

for(const css of [ 'message-format.css', 'message-format-delimiters.css' ])
  copyFileSync(join(root, 'src', css), join(dist, css));

console.log(`built distributables in ${dist}`);
