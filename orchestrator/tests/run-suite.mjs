import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

import {
  createSuiteCatalog,
  verifyFigmaTestAliasContract,
  verifyWorkspaceContract
} from './suite-discovery.mjs';

const orchestratorDirectory = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
);
const projectRoot = path.resolve(orchestratorDirectory, '..');
const suiteEnvironment = Object.freeze({
  ...process.env,
  PYTHONDONTWRITEBYTECODE: '1'
});
const catalog = createSuiteCatalog(projectRoot, process.execPath);

const requested = process.argv[2];
if (requested === '--check') {
  verifyWorkspaceContract(projectRoot);
  verifyFigmaTestAliasContract(projectRoot, catalog.figmaTests);
  console.log(
    `test-ownership: ${catalog.discoveredProjectTests.length} files, exactly one owner each`
  );
  console.log(JSON.stringify(catalog.ownershipSummary));
  process.exit(0);
}

const selected = Object.hasOwn(catalog.entriesBySuite, requested)
  ? catalog.entriesBySuite[requested]
  : null;
if (!selected) {
  console.error(`unknown suite: ${String(requested || '')}`);
  console.error(`available: ${catalog.availableSuiteNames.join(', ')}`);
  process.exit(2);
}

const startedAt = Date.now();
for (let index = 0; index < selected.length; index += 1) {
  const item = selected[index];
  console.log(`[${requested}] ${index + 1}/${selected.length} ${item.label}`);
  const result = spawnSync(item.command, item.args, {
    cwd: path.resolve(projectRoot, item.cwd),
    env: suiteEnvironment,
    stdio: 'inherit'
  });
  if (result.error) throw result.error;
  if (result.status !== 0) {
    process.exit(Number.isInteger(result.status) ? result.status : 1);
  }
}

console.log(
  `${requested}: ${selected.length} checks passed in ${Math.ceil((Date.now() - startedAt) / 1000)}s`
);
