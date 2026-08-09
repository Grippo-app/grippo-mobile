// skills:wiring — offline proof that the current skill wiring is complete and
// consistent. It does not prove runtime behaviour; that requires a live build.

import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';

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
try {
  execFileSync('bash', [p('orchestrator/skills/install-skills.sh'), tempRoot], { stdio: 'pipe' });
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
} catch (error) {
  bad('install-skills.sh failed: ' + (error.message || error));
} finally {
  fs.rmSync(tempRoot, { recursive: true, force: true });
}

if (!fail) {
  console.log(`    ok: ${capabilities.operations.length} operations resolve to installed skills; ` +
    'server-owned task prompts, typed board dispatch, and installation are complete');
}
process.exit(fail);
