// Lists the highlight.js themes that need the delimiter fallback CSS
// (dist/message-format-delimiters.css), i.e. themes
// where .hljs-template-tag is not styled or has the same colour as .hljs-string. Prints a
// Markdown table for the README.
//
// Usage: node scripts/check-themes.mjs [--all]   (--all also lists the themes that are fine)

import { readFileSync, readdirSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const stylesDir = join(root, 'node_modules/highlight.js/styles');
const version = JSON.parse(readFileSync(join(root, 'node_modules/highlight.js/package.json'), 'utf8')).version;
const all = process.argv.includes('--all');


function* themes(dir)
{
  for(const entry of readdirSync(dir, { withFileTypes: true }).sort((a, b) => a.name.localeCompare(b.name)))
    if (entry.isDirectory())
      yield* themes(join(dir, entry.name));
    else if (entry.name.endsWith('.css') && !entry.name.endsWith('.min.css'))
      yield join(dir, entry.name);
}


/** Returns the colour the theme assigns to a top-level `.hljs-<scope>` selector, if any. */
function colors(css)
{
  const result = new Map();

  css = css.replace(/\/\*[\s\S]*?\*\//g, '');
  for(const [ , selectors, declarations ] of css.matchAll(/([^{}]+)\{([^}]*)\}/g))
  {
    const color = /(?:^|;)\s*color\s*:\s*([^;]+)/.exec(declarations)?.[1].trim().toLowerCase();
    if (!color)
      continue;

    for(const selector of selectors.split(','))
    {
      const m = /^\.hljs-([\w-]+)$/.exec(selector.trim());
      if (m)
        result.set(m[1], color);
    }
  }

  return result;
}


const rows = [];

for(const file of themes(stylesDir))
{
  const c = colors(readFileSync(file, 'utf8'));
  const tag = c.get('template-tag'), string = c.get('string');
  const name = relative(stylesDir, file).replace(/\.css$/, '');

  if (!tag)
    rows.push([ name, 'not styled', string ?? '–' ]);
  else if (tag === string)
    rows.push([ name, `same as string (${tag})`, string ]);
  else if (all)
    rows.push([ name, `ok (${tag})`, string ?? '–' ]);
}

console.log(`Themes that need the delimiter fallback CSS (highlight.js ${version}):\n`);
console.log('| Theme | `template-tag` | `string` |');
console.log('|---|---|---|');
for(const row of rows)
  console.log(`| \`${row[0]}\` | ${row.slice(1).join(' | ')} |`);
