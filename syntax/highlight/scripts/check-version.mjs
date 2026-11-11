// Fails the build if the package version differs from the library version (`message-format` in
// gradle/libs.versions.toml). The Gradle build syncs it automatically (task highlightSyncVersion);
// this check catches npm builds run directly after a version change.

import {readFileSync} from 'node:fs';
import {dirname, join, resolve} from 'node:path';
import {fileURLToPath} from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const catalog = join(root, '../../gradle/libs.versions.toml');

const packageVersion = JSON.parse(readFileSync(join(root, 'package.json'), 'utf8')).version;
const match = /^message-format\s*=\s*(['"])(.+?)\1/m.exec(readFileSync(catalog, 'utf8'));

if (!match)
  throw new Error(`${catalog}: no message-format version found`);

const libraryVersion = match[2];

if (packageVersion !== libraryVersion)
{
  console.error(`package version ${packageVersion} differs from library version ${libraryVersion}.\n` +
      `Run \`./gradlew highlightSyncVersion\` in the repository root or ` +
      `\`npm version ${libraryVersion} --no-git-tag-version\` here.`);
  process.exit(1);
}
