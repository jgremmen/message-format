// Runs the ANTLR tool (JavaScript target) on the prepared grammars in build/grammar/ and writes the generated
// lexer/parser to src/generated/.
//
// The tool is taken from $ANTLR_CLASSPATH (the tool and its dependencies, as provided by the Gradle build), from
// $ANTLR_JAR (a complete jar), or downloaded once into build/. Its version must match the `antlr4` runtime dependency
// in package.json exactly. A downloaded jar is only used if its SHA-256 checksum matches the one pinned in
// `antlrJarSha256` for that version.

import {execFileSync} from 'node:child_process';
import {createHash} from 'node:crypto';
import {existsSync, mkdirSync, readFileSync, renameSync, rmSync, writeFileSync} from 'node:fs';
import {dirname, join, resolve} from 'node:path';
import {fileURLToPath} from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const version = JSON.parse(readFileSync(join(root, 'package.json'), 'utf8')).dependencies.antlr4;
const grammarDir = join(root, 'build/grammar');
const outDir = join(root, 'src/generated');

// SHA-256 checksums of antlr-<version>-complete.jar from antlr.org (the SHA-1 matches
// org.antlr:antlr4:<version>:complete on Maven Central); add one when the antlr4 version changes
const antlrJarSha256 = {
  '4.13.2': 'eae2dfa119a64327444672aff63e9ec35a20180dc5b8090b7a6ab85125df4d76'
};


/** Throws if the SHA-256 checksum of `content` differs from the pinned checksum. */
function verifyAntlrJar(content, source)
{
  const actual = createHash('sha256').update(content).digest('hex');

  if (actual !== antlrJarSha256[version])
    throw new Error(`${source}: SHA-256 checksum ${actual} differs from ${antlrJarSha256[version]}`);
}


/** Returns the java arguments that start the ANTLR tool. */
async function antlrTool()
{
  if (process.env.ANTLR_CLASSPATH)
    return [ '-cp', process.env.ANTLR_CLASSPATH, 'org.antlr.v4.Tool' ];
  if (process.env.ANTLR_JAR)
    return [ '-jar', process.env.ANTLR_JAR ];

  if (!antlrJarSha256[version]) {
    throw new Error(
        `no checksum for ANTLR ${version} known; set ANTLR_CLASSPATH or ANTLR_JAR, or add the checksum of the ` +
        'complete jar to antlrJarSha256 in scripts/generate.mjs');
  }

  const jar = join(root, `build/antlr-${version}-complete.jar`);
  if (existsSync(jar))
    verifyAntlrJar(readFileSync(jar), jar);
  else
  {
    const url = `https://www.antlr.org/download/antlr-${version}-complete.jar`;
    console.log(`downloading ${url}`);

    const response = await fetch(url);
    if (!response.ok)
      throw new Error(`download of ${url} failed: ${response.status} ${response.statusText}`);

    const content = Buffer.from(await response.arrayBuffer());
    verifyAntlrJar(content, url);

    // write to a temporary file first, so an interrupted write never leaves a partial jar behind
    mkdirSync(dirname(jar), { recursive: true });
    writeFileSync(`${jar}.tmp`, content);
    renameSync(`${jar}.tmp`, jar);
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
