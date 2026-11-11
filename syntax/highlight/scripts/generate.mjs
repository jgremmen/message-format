// Runs the ANTLR tool (JavaScript target) on the prepared grammars in build/grammar/ and writes
// the generated lexer/parser to src/generated/.
//
// The tool is taken from $ANTLR_CLASSPATH (the tool and its dependencies, as provided by the
// Gradle build), from $ANTLR_JAR (a complete jar), or downloaded once into build/. Its version
// must match the `antlr4` runtime dependency in package.json exactly.

import { execFileSync } from 'node:child_process';
import { existsSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const version = JSON.parse(readFileSync(join(root, 'package.json'), 'utf8')).dependencies.antlr4;
const grammarDir = join(root, 'build/grammar');
const outDir = join(root, 'src/generated');


/** Returns the java arguments that start the ANTLR tool. */
async function antlrTool()
{
  if (process.env.ANTLR_CLASSPATH)
    return [ '-cp', process.env.ANTLR_CLASSPATH, 'org.antlr.v4.Tool' ];
  if (process.env.ANTLR_JAR)
    return [ '-jar', process.env.ANTLR_JAR ];

  const jar = join(root, `build/antlr-${version}-complete.jar`);
  if (!existsSync(jar))
  {
    const url = `https://www.antlr.org/download/antlr-${version}-complete.jar`;
    console.log(`downloading ${url}`);

    const response = await fetch(url);
    if (!response.ok)
      throw new Error(`download of ${url} failed: ${response.status} ${response.statusText}`);

    mkdirSync(dirname(jar), { recursive: true });
    writeFileSync(jar, Buffer.from(await response.arrayBuffer()));
  }

  return [ '-jar', jar ];
}


const tool = await antlrTool();

rmSync(outDir, { recursive: true, force: true });
mkdirSync(outDir, { recursive: true });

execFileSync('java', [
  ...tool,
  '-Dlanguage=JavaScript',
  '-Xexact-output-dir',
  '-no-listener', '-no-visitor',
  '-o', outDir,
  join(grammarDir, 'MessageLexer.g4'),
  join(grammarDir, 'MessageParser.g4')
], { stdio: 'inherit', cwd: grammarDir });

console.log(`generated lexer/parser in ${outDir}`);
