// skills:wiring — offline proof that the current skill wiring is complete and
// consistent. It does not prove runtime behaviour; that requires a live build.

import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { fileURLToPath } from 'node:url';
import { execFileSync, spawnSync } from 'node:child_process';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(HERE, '..', '..', '..');
const p = (...x) => path.join(ROOT, ...x);

let fail = 0;
const bad = (message) => { console.error('    FAIL: ' + message); fail = 1; };

const capabilities = JSON.parse(fs.readFileSync(p('orchestrator/skills/_index/capabilities.json'), 'utf8'));
const manifest = JSON.parse(fs.readFileSync(p('orchestrator/skills/_index/install-manifest.json'), 'utf8'));
const manifestByFolder = new Map(manifest.skills.map((skill) => [skill.folderName, skill]));

for (const operation of capabilities.operations) {
  if (!operation.skill) {
    bad(`operation ${operation.operation} has no skill`);
    continue;
  }
  const skill = manifestByFolder.get(operation.skill);
  if (!skill) {
    bad(`operation ${operation.operation} -> skill '${operation.skill}' not in install-manifest`);
    continue;
  }
  if (!skill.externalSourceException && !fs.existsSync(p('orchestrator/skills', operation.skill))) {
    bad(`skill '${operation.skill}' (operation ${operation.operation}) has no on-disk directory`);
  }
}

const board = fs.readFileSync(p('orchestrator/site/scripts/panels/board.js'), 'utf8');
const taskPrompts = fs.readFileSync(p('orchestrator/site/server/task-action-prompts.js'), 'utf8');
if (/entrypoints\.js/.test(board)) bad('board.js still imports the retired browser prompt entrypoint map');
if (!/executeTaskAction/.test(board)) bad('board.js does not dispatch typed task actions');
for (const reference of ['task-prep skill', 'task-orchestrator skill', 'task-orchestrator task-drop contract']) {
  if (!taskPrompts.includes(reference)) {
    bad(`server task prompt builders do not route through '${reference}'`);
  }
}

const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'skills-install-'));
function installedDigest(root) {
  const rows = [];
  function walk(directory, prefix) {
    for (const entry of fs.readdirSync(directory, { withFileTypes: true }).sort((a, b) => a.name.localeCompare(b.name))) {
      const absolute = path.join(directory, entry.name);
      const relative = prefix ? `${prefix}/${entry.name}` : entry.name;
      if (entry.isDirectory()) walk(absolute, relative);
      else if (entry.isFile()) rows.push(`${relative}\0${fs.readFileSync(absolute).toString('base64')}`);
      else bad(`install produced a non-regular entry: ${relative}`);
    }
  }
  walk(path.join(root, '.claude'), '.claude');
  return rows.join('\n');
}
try {
  const unsafeManifest = path.join(tempRoot, 'unsafe-install-manifest.json');
  const mutated = structuredClone(manifest);
  mutated.files[0].installPath = '../../outside-target';
  fs.writeFileSync(unsafeManifest, JSON.stringify(mutated));
  const rejected = spawnSync('python3', [
    p('orchestrator/skills/_index/validate-install-manifest.py'), ROOT, unsafeManifest,
  ], { encoding: 'utf8' });
  if (rejected.status === 0 || !/INSTALL_MANIFEST_INVALID/.test(rejected.stderr || '')) {
    bad('install manifest validator accepted an escaping destination');
  }
  execFileSync('bash', [p('orchestrator/skills/install-skills.sh'), tempRoot, '--no-hooks'], { stdio: 'pipe' });
  for (const skill of manifest.skills) {
    if (skill.externalSourceException) continue;
    const destination = path.join(tempRoot, '.claude/skills', skill.folderName);
    if (!fs.existsSync(path.join(destination, 'SKILL.md'))) {
      bad(`install: ${skill.folderName}/SKILL.md missing`);
      continue;
    }
    const references = path.join(destination, 'references');
    const files = fs.existsSync(references)
      ? fs.readdirSync(references).filter((file) => file.endsWith('.md'))
      : [];
    if (files.length === 0) bad(`install: ${skill.folderName}/references is empty`);
  }
  for (const file of manifest.files || []) {
    const destination = path.join(tempRoot, file.installPath);
    if (!fs.existsSync(destination) || !fs.statSync(destination).isFile()) {
      bad(`install: ${file.installPath} missing`);
      continue;
    }
    if (!fs.readFileSync(destination).equals(fs.readFileSync(p(file.sourcePath)))) {
      bad(`install: ${file.installPath} differs from ${file.sourcePath}`);
    }
  }
  for (const file of manifest.files || []) {
    fs.rmSync(path.join(tempRoot, file.installPath), { force: true });
  }
  const partial = spawnSync('bash', [
    p('orchestrator/skills/checks/install-sync.sh'), tempRoot,
  ], { encoding: 'utf8' });
  if (partial.status === 0 || !/partial installed-file set/.test(
    `${partial.stdout || ''}${partial.stderr || ''}`)) {
    bad('install-sync accepted skills without the complete managed-file set');
  }
  execFileSync('bash', [p('orchestrator/skills/install-skills.sh'), tempRoot, '--no-hooks'], { stdio: 'pipe' });
  const firstDigest = installedDigest(tempRoot);
  execFileSync('bash', [p('orchestrator/skills/install-skills.sh'), tempRoot, '--no-hooks'], { stdio: 'pipe' });
  if (installedDigest(tempRoot) !== firstDigest) bad('install is not byte-idempotent across two copy-mode runs');
} catch (error) {
  bad('install-skills.sh failed: ' + (error.message || error));
} finally {
  fs.rmSync(tempRoot, { recursive: true, force: true });
}

if (!fail) {
  console.log(`    ok: ${capabilities.operations.length} operations resolve to installed skills; ` +
    `server-owned task prompts, typed board dispatch, and ${manifest.files.length} installed files are complete`);
}
process.exit(fail);
