#!/usr/bin/env node

import assert from 'node:assert/strict';
import { createRequire } from 'node:module';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { runIndexOperation } from '../task-index.mjs';

const HERE = dirname(fileURLToPath(import.meta.url));
const TASKS_DIR = join(HERE, '..');
const support = createRequire(import.meta.url)('../platform-support.cjs');

assert.equal(support.assertCanonicalTaskPlatform('linux'), 'linux');
assert.equal(support.assertCanonicalTaskPlatform('darwin'), 'darwin');
for (const platform of ['win32', 'freebsd', '', null]) {
  assert.throws(() => support.assertCanonicalTaskPlatform(platform), (error) =>
    error && error.code === 'PLATFORM_UNSUPPORTED' && error.exitCode === 3 &&
    !/python3|dir_fd|stack/i.test(error.message));
}

const fixtureRoot = mkdtempSync(join(tmpdir(), 'task-platform-api-'));
const fixtureTasks = join(fixtureRoot, 'orchestrator', 'tasks');
for (const column of ['backlog', 'pending', 'todo', 'done']) mkdirSync(join(fixtureTasks, column), { recursive: true });
const fixtureIndex = join(fixtureTasks, 'INDEX.json');
const initialIndex = JSON.stringify({
  version: 2, generatedAt: '1970-01-01T00:00:00Z', backlog: [], pending: [], todo: [], done: []
}, null, 2) + '\n';
writeFileSync(fixtureIndex, initialIndex);
const fixtureOutcome = join(fixtureRoot, 'outcome-shape.json');
writeFileSync(fixtureOutcome, readFileSync(join(TASKS_DIR, '..', 'contracts', 'outcome-shape.json')));

const descriptor = Object.getOwnPropertyDescriptor(process, 'platform');
const savedFixtureRoot = process.env.TASK_FS_TEST_ROOT;
try {
  process.env.TASK_FS_TEST_ROOT = fixtureRoot;
  Object.defineProperty(process, 'platform', { ...descriptor, value: 'win32' });
  const core = createRequire(import.meta.url)('../task-state-core.cjs');
  assert.throws(() => core.validateTaskState({}), (error) =>
    error && error.code === 'PLATFORM_UNSUPPORTED' && error.exitCode === 3);
  const api = await runIndexOperation({
    repoRoot: fixtureRoot, tasksDir: fixtureTasks, outcomeShapePath: fixtureOutcome,
  });
  assert.equal(api.exitCode, 3);
  assert.equal(api.diagnostic.code, 'PLATFORM_UNSUPPORTED');
  assert.equal(api.index, null);
  assert.equal(readFileSync(fixtureIndex, 'utf8'), initialIndex,
    'unsupported exported API call must not publish INDEX bytes');
  assert.equal(existsSync(join(fixtureRoot, 'orchestrator', '.cache')), false,
    'unsupported exported API call must fail before opening the publication boundary');
} finally {
  Object.defineProperty(process, 'platform', descriptor);
  if (savedFixtureRoot === undefined) delete process.env.TASK_FS_TEST_ROOT;
  else process.env.TASK_FS_TEST_ROOT = savedFixtureRoot;
  rmSync(fixtureRoot, { recursive: true, force: true });
}

for (const file of ['task-state-core.cjs', 'validate-task-state.mjs', 'task-index.mjs', 'task-lock.mjs',
  'transition-task-state.mjs', 'finalize-task.mjs']) {
  assert.match(readFileSync(join(TASKS_DIR, file), 'utf8'), /assertCanonicalTaskPlatform\(\)/,
    `${file} must fail before entering the native lifecycle boundary`);
}
for (const file of ['create-backlog.py', 'edit-backlog.py']) {
  assert.match(readFileSync(join(TASKS_DIR, file), 'utf8'), /require_supported_task_platform\(\)/,
    `${file} must fail before entering the native lifecycle boundary`);
}
assert.match(readFileSync(join(TASKS_DIR, '..', 'figma', 'scripts', 'descope-task.mjs'), 'utf8'),
  /assertCanonicalTaskPlatform\(\)/,
  'the sanctioned Figma task editor must use the same supported-host boundary');

console.log('platform support: 2 supported hosts + fail-closed unsupported-host contract passed across every public task writer');
