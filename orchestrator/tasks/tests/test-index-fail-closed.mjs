#!/usr/bin/env node

import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';
import { runIndexOperation } from '../task-index.mjs';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const TASKS_DIR = path.join(HERE, '..');
const SHAPE = path.join(TASKS_DIR, '..', 'contracts', 'outcome-shape.json');
const INDEX_ENTRYPOINT = path.join(TASKS_DIR, 'regen-index.py');
const INDEX_CLI = path.join(TASKS_DIR, 'task-index.mjs');
const EMPTY_INDEX = {
  version: 2,
  generatedAt: '1970-01-01T00:00:00Z',
  backlog: [],
  pending: [],
  todo: [],
  done: []
};

let passed = 0;
let failed = 0;

async function test(name, callback) {
  try {
    await callback();
    passed++;
    process.stdout.write('  ok: ' + name + '\n');
  } catch (error) {
    failed++;
    process.stderr.write('  FAIL: ' + name + ': ' + (error && error.stack || error) + '\n');
  }
}

function fixture() {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'task-index-fail-closed-'));
  const tasks = path.join(root, 'orchestrator', 'tasks');
  for (const column of ['backlog', 'pending', 'todo', 'done']) {
    fs.mkdirSync(path.join(tasks, column), { recursive: true });
  }
  fs.writeFileSync(path.join(tasks, 'INDEX.json'), JSON.stringify(EMPTY_INDEX, null, 2) + '\n');
  return {
    root,
    tasks,
    index: path.join(tasks, 'INDEX.json'),
    diagnostic: path.join(root, 'orchestrator', '.cache', 'tasks', 'integrity', 'index.json'),
    cleanup() { fs.rmSync(root, { recursive: true, force: true }); }
  };
}

function options(fx, extra = {}) {
  return {
    repoRoot: fx.root,
    tasksDir: fx.tasks,
    outcomeShapePath: SHAPE,
    outcomeShapeAuthorityRoot: path.dirname(SHAPE),
    ...extra
  };
}

function backlogBody(number = 1, title = 'Canonical backlog') {
  return '# TASK ' + number + ' — ' + title + '\n\n' + sourceBlock('task-' + number) + '\n\n## Goal\nDescribe the work.\n';
}

function sourceBlock(ref) {
  return [
    '## Source', '',
    '- Kind: manual',
    '- Type: manual',
    '- Ref: ' + ref,
    '- Fingerprint: ' + hash(Buffer.from('source\0' + ref, 'utf8'))
  ].join('\n');
}

function assertNoTemps(fx) {
  const leaked = fs.readdirSync(fx.tasks).filter((name) =>
    /^\.INDEX\.json\..+\.tmp$/.test(name) || /^\.task-index-(?:cas|committed)-/.test(name));
  assert.deepEqual(leaked, []);
}

function hash(bytes) {
  return 'sha256:' + crypto.createHash('sha256').update(bytes).digest('hex');
}

function treeSnapshot(root) {
  const rows = [];
  function visit(absolute, relative) {
    const stat = fs.lstatSync(absolute, { bigint: true });
    const row = {
      path: relative || '.',
      mode: stat.mode.toString(),
      size: stat.size.toString(),
      mtimeNs: stat.mtimeNs.toString(),
      ctimeNs: stat.ctimeNs.toString(),
      kind: stat.isDirectory() ? 'directory' : stat.isSymbolicLink() ? 'symlink' : stat.isFile() ? 'file' : 'other'
    };
    if (stat.isFile()) row.hash = hash(fs.readFileSync(absolute));
    if (stat.isSymbolicLink()) row.target = fs.readlinkSync(absolute);
    rows.push(row);
    if (stat.isDirectory()) {
      for (const name of fs.readdirSync(absolute).sort()) visit(path.join(absolute, name), relative ? relative + '/' + name : name);
    }
  }
  visit(root, '');
  return rows;
}

function cliEnv(fx, extra = {}) {
  return {
    ...process.env,
    ORCHESTRATOR_PROJECT_ROOT: fx.root,
    ORCHESTRATOR_TASKS_DIR: fx.tasks,
    ORCHESTRATOR_OUTCOME_SHAPE_PATH: SHAPE,
    ORCHESTRATOR_OUTCOME_SHAPE_AUTHORITY_ROOT: path.dirname(SHAPE),
    TASK_FS_TEST_ROOT: fx.root,
    ...extra
  };
}

function runCli(fx, args = [], extraEnv = {}) {
  return spawnSync(process.execPath, [INDEX_CLI, ...args], {
    cwd: fx.root,
    env: cliEnv(fx, extraEnv),
    encoding: 'utf8',
    timeout: 30000
  });
}

const OBSERVATION_KEYS = [
  'action', 'architectureStatus', 'caller', 'durationMs', 'event', 'expectedState',
  'findings', 'findingsTruncated', 'observedState', 'ok', 'overallOk', 'phase',
  'result', 'scanMode', 'scope', 'slow', 'slowThresholdMs', 'snapshotHash',
  'taskBodyReads', 'transition', 'version'
];

function taskStateObservations(stderr) {
  return String(stderr || '').split(/\r?\n/)
    .filter((line) => line.startsWith('[task-state] '))
    .map((line) => JSON.parse(line.slice('[task-state] '.length)));
}

function assertIndexObservations(events, action, count) {
  assert.equal(events.length, count);
  for (const event of events) {
    assert.deepEqual(Object.keys(event).sort(), OBSERVATION_KEYS);
    assert.equal(event.version, 1);
    assert.equal(event.event, 'task-state-validation');
    assert.equal(event.caller, 'server');
    assert.equal(event.scope, 'all');
    assert.equal(event.action, action);
    assert.ok(['pre', 'post'].includes(event.phase));
    assert.ok(['valid', 'invalid'].includes(event.result));
    assert.equal(event.findings.every((item) =>
      Object.keys(item).sort().join(',') === 'code,severity'), true);
  }
}

function canonicalManifest(value) {
  const sortValue = (input) => {
    if (Array.isArray(input)) return input.map(sortValue);
    if (!input || typeof input !== 'object') return input;
    return Object.fromEntries(Object.keys(input).sort().map((key) => [key, sortValue(input[key])]));
  };
  return JSON.stringify(sortValue(value)) + '\n';
}

process.stdout.write('Fail-closed canonical INDEX publisher:\n');

await test('empty publication is deterministic and records a bounded diagnostic', async () => {
  const fx = fixture();
  try {
    const previousUmask = process.platform === 'win32' ? null : process.umask(0o077);
    let result;
    try { result = await runIndexOperation(options(fx)); }
    finally { if (previousUmask !== null) process.umask(previousUmask); }
    assert.equal(result.exitCode, 0);
    assert.equal(result.diagnostic.code, 'INDEX_PUBLISHED');
    assert.equal(result.diagnostic.integrityOk, true);
    assert.deepEqual(JSON.parse(fs.readFileSync(fx.index, 'utf8')), EMPTY_INDEX);
    const diagnostic = JSON.parse(fs.readFileSync(fx.diagnostic, 'utf8'));
    assert.equal(diagnostic.kind, 'task-index-diagnostic');
    assert.equal(diagnostic.ok, true);
    assert.ok(fs.statSync(fx.diagnostic).size < 256 * 1024);
    if (process.platform !== 'win32') {
      assert.equal(fs.statSync(fx.diagnostic).mode & 0o777, 0o600);
      assert.equal(fs.statSync(fx.index).mode & 0o777, 0o644);
    }
    assertNoTemps(fx);
  } finally { fx.cleanup(); }
});

await test('canonical derivation publishes backlog rows from the second snapshot', async () => {
  const fx = fixture();
  try {
    fs.writeFileSync(path.join(fx.tasks, 'backlog', 'TASK_1_alpha.md'), backlogBody(1, 'Alpha'));
    const result = await runIndexOperation(options(fx));
    assert.equal(result.exitCode, 0);
    const index = JSON.parse(fs.readFileSync(fx.index, 'utf8'));
    assert.equal(index.backlog.length, 1);
    assert.equal(index.backlog[0].stem, 'TASK_1_alpha');
    assert.equal(index.backlog[0].title, 'Alpha');
    assertNoTemps(fx);
  } finally { fx.cleanup(); }
});

await test('pending is derived only from its canonical two-file logical state', async () => {
  const fx = fixture();
  try {
    const stem = 'TASK_9_pending_pair';
    fs.writeFileSync(path.join(fx.tasks, 'backlog', stem + '.md'), backlogBody(9, 'Pending pair'));
    fs.writeFileSync(path.join(fx.tasks, 'pending', stem + '.questions.md'), [
      '---',
      'forTask: ' + stem,
      'createdAt: 2026-01-01T00:00:00Z',
      'updatedAt: 2026-01-01T00:00:00Z',
      'round: 2',
      'gapCount: 1',
      'prevGapCount: 2',
      '---',
      '',
      '## Q1 — Which path?',
      '**Type**: choice',
      '**Options**: A, B',
      '',
      '### Answer',
      ''
    ].join('\n'));
    const result = await runIndexOperation(options(fx));
    assert.equal(result.exitCode, 0);
    const index = JSON.parse(fs.readFileSync(fx.index, 'utf8'));
    assert.equal(index.backlog.length, 0);
    assert.deepEqual(index.pending.map(({ stem: rowStem, title, questionsCount, round }) => ({ stem: rowStem, title, questionsCount, round })), [{
      stem,
      title: 'Pending pair',
      questionsCount: 1,
      round: 2
    }]);
  } finally { fx.cleanup(); }
});

await test('done outcome status is derived by the canonical core parser', async () => {
  const fx = fixture();
  try {
    const body = [
      '# TASK 10 — Canonical done',
      '',
      sourceBlock('task-10'),
      '',
      '## Goal',
      'Finish safely.',
      '',
      '---',
      '',
      '## Outcome',
      '',
      '**Status**: completed',
      '**Completed at**: 2026-01-01T00:00:00Z',
      '**Reviewer**: codex',
      '**Review iterations**: 1',
      '',
      '### Build gates',
      '- `node test` — pass',
      '',
      '### Runtime verify',
      '- Gate: skipped (no runtime-observable change)',
      '- Result: n/a — no runtime-observable change',
      '',
      '### Acceptance trace',
      '- `publisher` — verified — fail closed',
      '',
      '### Caveats',
      '- none',
      '',
      '### Follow-ups',
      '- none',
      '',
      '### Files touched',
      '- `orchestrator/tasks/task-index.mjs` — modified',
      ''
    ].join('\n');
    fs.writeFileSync(path.join(fx.tasks, 'done', 'TASK_10_canonical_done.md'), body);
    const result = await runIndexOperation(options(fx));
    assert.equal(result.exitCode, 0);
    assert.equal(result.diagnostic.integrityOk, true);
    const index = JSON.parse(fs.readFileSync(fx.index, 'utf8'));
    assert.equal(index.done[0].outcomeStatus, 'completed');
  } finally { fx.cleanup(); }
});

await test('local content errors preserve the last valid index byte-for-byte', async () => {
  const fx = fixture();
  try {
    const before = fs.readFileSync(fx.index);
    fs.writeFileSync(path.join(fx.tasks, 'todo', 'TASK_2_local_error.md'), '# TASK 2 — Local error\n\n## Goal\nOnly one section.\n');
    const result = await runIndexOperation(options(fx));
    assert.equal(result.exitCode, 1);
    assert.equal(result.diagnostic.code, 'INDEX_PUBLICATION_BLOCKED');
    assert.equal(result.diagnostic.integrityOk, false);
    assert.ok(result.diagnostic.findings.some((item) => item.code === 'TODO_REQUIRED_SECTION_MISSING'));
    assert.deepEqual(fs.readFileSync(fx.index), before);
  } finally { fx.cleanup(); }
});

await test('same-stem column collision preserves the last valid index byte-for-byte', async () => {
  const fx = fixture();
  try {
    const stem = 'TASK_3_collision';
    fs.writeFileSync(path.join(fx.tasks, 'backlog', stem + '.md'), backlogBody(3, 'Collision'));
    assert.equal((await runIndexOperation(options(fx))).exitCode, 0);
    const before = fs.readFileSync(fx.index);
    fs.writeFileSync(path.join(fx.tasks, 'todo', stem + '.md'), '# TASK 3 — Collision\n');
    const blocked = await runIndexOperation(options(fx));
    assert.equal(blocked.exitCode, 1);
    assert.equal(blocked.diagnostic.code, 'INDEX_PUBLICATION_BLOCKED');
    assert.ok(blocked.diagnostic.findings.some((item) => item.code === 'TASK_PRESENT_IN_MULTIPLE_STATES'));
    assert.deepEqual(fs.readFileSync(fx.index), before);
    assertNoTemps(fx);
  } finally { fx.cleanup(); }
});

await test('numeric identity ambiguity blocks publication and reports only relative paths', async () => {
  const fx = fixture();
  try {
    const before = fs.readFileSync(fx.index);
    fs.writeFileSync(path.join(fx.tasks, 'backlog', 'TASK_4_alpha.md'), backlogBody(4, 'Alpha'));
    fs.writeFileSync(path.join(fx.tasks, 'backlog', 'TASK_4_beta.md'), backlogBody(4, 'Beta'));
    const blocked = await runIndexOperation(options(fx));
    assert.equal(blocked.exitCode, 1);
    const conflict = blocked.diagnostic.findings.find((item) => item.code === 'TASK_NUMBER_CONFLICT');
    assert.ok(conflict);
    assert.ok(conflict.paths.length >= 2);
    assert.ok(conflict.paths.every((entry) => !path.isAbsolute(entry) && !entry.includes(fx.root)));
    assert.deepEqual(fs.readFileSync(fx.index), before);
  } finally { fx.cleanup(); }
});

await test('orphan pending sidecar blocks publication instead of inventing a pending card', async () => {
  const fx = fixture();
  try {
    const before = fs.readFileSync(fx.index);
    fs.writeFileSync(path.join(fx.tasks, 'pending', 'TASK_5_orphan.questions.md'), [
      '---',
      'forTask: TASK_5_orphan',
      'createdAt: 2026-01-01T00:00:00Z',
      'updatedAt: 2026-01-01T00:00:00Z',
      'round: 1',
      'gapCount: 1',
      '---',
      '',
      '## Q1 — Missing source?',
      '**Type**: text',
      '',
      '### Answer',
      ''
    ].join('\n'));
    const blocked = await runIndexOperation(options(fx));
    assert.equal(blocked.exitCode, 1);
    assert.ok(blocked.diagnostic.findings.some((item) => item.code === 'PENDING_SOURCE_MISSING'));
    assert.deepEqual(fs.readFileSync(fx.index), before);
  } finally { fx.cleanup(); }
});

await test('double-scan CAS detects a source edit and publishes nothing', async () => {
  const fx = fixture();
  try {
    const file = path.join(fx.tasks, 'backlog', 'TASK_6_race.md');
    fs.writeFileSync(file, backlogBody(6, 'Race'));
    assert.equal((await runIndexOperation(options(fx))).exitCode, 0);
    const before = fs.readFileSync(fx.index);
    const raced = await runIndexOperation(options(fx, {
      beforeRescan() { fs.appendFileSync(file, '\nExtra source bytes.\n'); }
    }));
    assert.equal(raced.exitCode, 4);
    assert.equal(raced.diagnostic.code, 'TASK_SNAPSHOT_CHANGED');
    assert.deepEqual(fs.readFileSync(fx.index), before);
    assertNoTemps(fx);
  } finally { fx.cleanup(); }
});

await test('final pre-publication scan catches a source edit and preserves the incumbent index', async () => {
  const fx = fixture();
  try {
    const file = path.join(fx.tasks, 'backlog', 'TASK_11_late_race.md');
    fs.writeFileSync(file, backlogBody(11, 'Late race'));
    assert.equal((await runIndexOperation(options(fx))).exitCode, 0);
    const before = fs.readFileSync(fx.index);
    const raced = await runIndexOperation(options(fx, {
      beforePublish() { fs.appendFileSync(file, '\nChanged after the second scan.\n'); }
    }));
    assert.equal(raced.exitCode, 4);
    assert.equal(raced.diagnostic.code, 'TASK_SNAPSHOT_CHANGED');
    assert.deepEqual(fs.readFileSync(fx.index), before);
    assertNoTemps(fx);
  } finally { fx.cleanup(); }
});

await test('post-publication source drift can never return a false success', async () => {
  const fx = fixture();
  try {
    const file = path.join(fx.tasks, 'backlog', 'TASK_12_post_race.md');
    fs.writeFileSync(file, backlogBody(12, 'Post race'));
    const raced = await runIndexOperation(options(fx, {
      afterPublish() { fs.appendFileSync(file, '\nChanged after publication.\n'); }
    }));
    assert.equal(raced.exitCode, 4);
    assert.equal(raced.diagnostic.code, 'TASK_SNAPSHOT_CHANGED_AFTER_PUBLICATION');
    const check = await runIndexOperation(options(fx, { check: true }));
    assert.equal(check.exitCode, 1, 'the uncommitted candidate must roll back to the last validated index');
    assert.equal(check.diagnostic.code, 'INDEX_NOT_FRESH');
    assert.equal((await runIndexOperation(options(fx))).exitCode, 0, 'a fresh retry must reconcile the stable source');
    assertNoTemps(fx);
  } finally { fx.cleanup(); }
});

await test('--check distinguishes a fresh index from stale structure without rewriting it', async () => {
  const fx = fixture();
  try {
    fs.writeFileSync(path.join(fx.tasks, 'backlog', 'TASK_7_check.md'), backlogBody(7, 'Check'));
    assert.equal((await runIndexOperation(options(fx))).exitCode, 0);
    const fresh = await runIndexOperation(options(fx, { check: true }));
    assert.equal(fresh.exitCode, 0);
    assert.equal(fresh.diagnostic.code, 'INDEX_FRESH');
    fs.writeFileSync(fx.index, JSON.stringify(EMPTY_INDEX, null, 2) + '\n');
    const staleBytes = fs.readFileSync(fx.index);
    const stale = await runIndexOperation(options(fx, { check: true }));
    assert.equal(stale.exitCode, 1);
    assert.equal(stale.diagnostic.code, 'INDEX_NOT_FRESH');
    assert.ok(stale.diagnostic.findings.some((item) => item.code === 'INDEX_STALE'));
    assert.deepEqual(fs.readFileSync(fx.index), staleBytes);
  } finally { fx.cleanup(); }
});

await test('unsafe INDEX symlink is never followed or replaced', async () => {
  if (process.platform === 'win32') return;
  const fx = fixture();
  try {
    const outside = path.join(fx.root, 'outside.json');
    const sentinel = Buffer.from('{"outside":true}\n');
    fs.writeFileSync(outside, sentinel);
    fs.unlinkSync(fx.index);
    fs.symlinkSync(outside, fx.index);
    const blocked = await runIndexOperation(options(fx));
    assert.equal(blocked.exitCode, 3);
    assert.equal(blocked.diagnostic.code, 'DESTINATION_UNSAFE');
    assert.equal(fs.lstatSync(fx.index).isSymbolicLink(), true);
    assert.deepEqual(fs.readFileSync(outside), sentinel);
  } finally { fx.cleanup(); }
});

await test('unreadable contract exits 3 and preserves the current index', async () => {
  const fx = fixture();
  try {
    const before = fs.readFileSync(fx.index);
    const failedResult = await runIndexOperation(options(fx, { outcomeShapePath: path.join(fx.root, 'missing-shape.json') }));
    assert.equal(failedResult.exitCode, 3);
    assert.equal(failedResult.diagnostic.code, 'CONTRACT_UNREADABLE');
    assert.deepEqual(fs.readFileSync(fx.index), before);
  } finally { fx.cleanup(); }
});

await test('Outcome contract symlink is rejected without following it', async () => {
  if (process.platform === 'win32') return;
  const fx = fixture();
  try {
    const before = fs.readFileSync(fx.index);
    const link = path.join(fx.root, 'shape-link.json');
    fs.symlinkSync(SHAPE, link);
    const blocked = await runIndexOperation(options(fx, { outcomeShapePath: link }));
    assert.equal(blocked.exitCode, 3);
    assert.equal(blocked.diagnostic.code, 'CONTRACT_UNSAFE');
    assert.equal(fs.lstatSync(link).isSymbolicLink(), true);
    assert.deepEqual(fs.readFileSync(fx.index), before);
  } finally { fx.cleanup(); }
});

await test('Python entrypoint delegates publication and keeps fail-closed semantics', async () => {
  const fx = fixture();
  try {
    fs.writeFileSync(path.join(fx.tasks, 'backlog', 'TASK_8_entrypoint.md'), backlogBody(8, 'Entrypoint'));
    const first = spawnSync('python3', [INDEX_ENTRYPOINT], { cwd: fx.root, encoding: 'utf8' });
    assert.equal(first.status, 0, first.stderr);
    const published = fs.readFileSync(fx.index);
    fs.writeFileSync(path.join(fx.tasks, 'done', 'TASK_8_entrypoint.md'), '# TASK 8 — Entrypoint\n');
    const blocked = spawnSync('python3', [INDEX_ENTRYPOINT], { cwd: fx.root, encoding: 'utf8' });
    assert.equal(blocked.status, 1, blocked.stdout + blocked.stderr);
    assert.deepEqual(fs.readFileSync(fx.index), published);
    const check = spawnSync('python3', [INDEX_ENTRYPOINT, '--check'], { cwd: fx.root, encoding: 'utf8' });
    assert.equal(check.status, 1);
  } finally { fx.cleanup(); }
});

await test('Python entrypoint reprojects observations and replaces malformed child telemetry without leaking stderr', async () => {
  const fx = fixture();
  const secret = 'REGEN-STDERR-SECRET-a8817ee9';
  try {
    fs.writeFileSync(path.join(fx.tasks, 'backlog', 'TASK_122_entrypoint_observed.md'),
      backlogBody(122, secret));
    const published = spawnSync('python3', [INDEX_ENTRYPOINT], { cwd: fx.root, encoding: 'utf8' });
    assert.equal(published.status, 0, published.stdout + published.stderr);
    const events = taskStateObservations(published.stderr);
    assertIndexObservations(events, 'index-publish', 6);
    assert.doesNotMatch(published.stderr, new RegExp(secret + '|' + fx.root));

    const malformedPayload = secret +
      '\n[task-state] ' + '['.repeat(1200) + '0' + ']'.repeat(1200) +
      '\n[task-state] {"version":1,"event":"task-state-validation","secret":"' + secret + '"}\n';
    const program = `
import importlib.util
import sys
spec = importlib.util.spec_from_file_location("regen_observation_test", sys.argv[1])
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
module.relay_task_state_events(${JSON.stringify(malformedPayload)}, "index-check", 2)
`;
    const malformed = spawnSync('python3', ['-c', program, INDEX_ENTRYPOINT], {
      cwd: fx.root, encoding: 'utf8', env: { ...process.env, PYTHONDONTWRITEBYTECODE: '1' }
    });
    assert.equal(malformed.status, 0, malformed.stdout + malformed.stderr);
    const synthetic = taskStateObservations(malformed.stderr);
    assertIndexObservations(synthetic, 'index-check', 2);
    assert.equal(synthetic.every((event) => event.findings.some((finding) =>
      finding.code === 'INDEX_OBSERVATION_INVALID' && finding.severity === 'blocker')), true);
    assert.doesNotMatch(malformed.stdout + malformed.stderr, new RegExp(secret));
  } finally { fx.cleanup(); }
});

await test('read-only Python check never creates a lock through an unsafe cache ancestor', async () => {
  if (process.platform === 'win32') return;
  const fx = fixture();
  const outside = fs.mkdtempSync(path.join(os.tmpdir(), 'task-index-lock-outside-'));
  try {
    fs.symlinkSync(outside, path.join(fx.root, 'orchestrator', '.cache'), 'dir');
    const blocked = spawnSync('python3', [INDEX_ENTRYPOINT, '--check'], { cwd: fx.root, encoding: 'utf8' });
    assert.notEqual(blocked.status, 0, blocked.stdout + blocked.stderr);
    assert.equal(fs.existsSync(path.join(outside, 'tasks')), false,
      'read-only check must not create a lock or diagnostic through the symlink ancestor');
  } finally {
    fx.cleanup();
    fs.rmSync(outside, { recursive: true, force: true });
  }
});

await test('direct Node CLI owns serialization and ignores the old spoofable lock token', async () => {
  const fx = fixture();
  try {
    fs.writeFileSync(path.join(fx.tasks, 'backlog', 'TASK_13_direct_cli.md'), backlogBody(13, 'Direct CLI'));
    const before = fs.readFileSync(fx.index);
    const env = {
      ...process.env,
      ORCHESTRATOR_PROJECT_ROOT: fx.root,
      ORCHESTRATOR_TASKS_DIR: fx.tasks,
      ORCHESTRATOR_OUTCOME_SHAPE_PATH: SHAPE,
      ORCHESTRATOR_OUTCOME_SHAPE_AUTHORITY_ROOT: path.dirname(SHAPE),
      TASK_INDEX_LOCK_HELD: '1'
    };
    const direct = spawnSync(process.execPath, [INDEX_CLI], { cwd: fx.root, env, encoding: 'utf8' });
    assert.equal(direct.status, 0, direct.stdout + direct.stderr);
    assert.notDeepEqual(fs.readFileSync(fx.index), before);
    assert.equal(JSON.parse(fs.readFileSync(fx.index, 'utf8')).backlog[0].stem, 'TASK_13_direct_cli');
  } finally { fx.cleanup(); }
});

await test('CLI emits one bounded observation per canonical scan and imported API stays silent by default', async () => {
  const fx = fixture();
  const secret = 'INDEX-BODY-SECRET-7f41f7c4';
  try {
    fs.writeFileSync(path.join(fx.tasks, 'backlog', 'TASK_121_observed.md'),
      backlogBody(121, secret));
    const published = runCli(fx);
    assert.equal(published.status, 0, published.stdout + published.stderr);
    const publishEvents = taskStateObservations(published.stderr);
    assertIndexObservations(publishEvents, 'index-publish', 6);
    assert.deepEqual(publishEvents.map((event) => event.phase),
      ['pre', 'pre', 'pre', 'post', 'post', 'post']);
    assert.doesNotMatch(JSON.stringify(publishEvents), new RegExp(secret + '|' + fx.root));

    const checked = runCli(fx, ['--check', '--json']);
    assert.equal(checked.status, 0, checked.stdout + checked.stderr);
    const checkEvents = taskStateObservations(checked.stderr);
    assertIndexObservations(checkEvents, 'index-check', 2);
    assert.deepEqual(checkEvents.map((event) => event.phase), ['pre', 'post']);
    assert.equal(JSON.parse(checked.stdout).code, 'INDEX_FRESH');

    let captured = '';
    const originalWrite = process.stderr.write;
    process.stderr.write = function (chunk, ...args) { captured += String(chunk); return true; };
    try {
      const imported = await runIndexOperation(options(fx, { check: true }));
      assert.equal(imported.exitCode, 0);
    } finally {
      process.stderr.write = originalWrite;
    }
    assert.equal(captured, '');

    fs.writeFileSync(path.join(fx.tasks, 'backlog', 'TASK_123_quiet_publish.md'),
      backlogBody(123, 'Quiet publish'));
    const quietPublish = runCli(fx, ['--quiet']);
    assert.equal(quietPublish.status, 0, quietPublish.stdout + quietPublish.stderr);
    assert.equal(quietPublish.stdout, '');
    assert.equal(quietPublish.stderr, '');
  } finally { fx.cleanup(); }
});

await test('a throwing canonical scan emits one synthetic privacy-bounded observation', async () => {
  const fx = fixture();
  try {
    const missing = path.join(fx.root, 'private-secret-contract-name.json');
    const failed = runCli(fx, ['--check'], {
      ORCHESTRATOR_OUTCOME_SHAPE_PATH: missing,
      ORCHESTRATOR_OUTCOME_SHAPE_AUTHORITY_ROOT: fx.root
    });
    assert.equal(failed.status, 3, failed.stdout + failed.stderr);
    const events = taskStateObservations(failed.stderr);
    assertIndexObservations(events, 'index-check', 1);
    assert.equal(events[0].result, 'invalid');
    assert.equal(events[0].findings[0].severity, 'blocker');
    assert.doesNotMatch(JSON.stringify(events), /private-secret-contract-name|task-index-fail-closed-/);
  } finally { fx.cleanup(); }
});

await test('CLI JSON/quiet contracts stay stable and read-only checks remain zero-mutation', async () => {
  const fx = fixture();
  try {
    const before = treeSnapshot(fx.root);
    const json = runCli(fx, ['--check', '--json']);
    assert.equal(json.status, 0, json.stdout + json.stderr);
    assertIndexObservations(taskStateObservations(json.stderr), 'index-check', 2);
    const payload = JSON.parse(json.stdout);
    assert.equal(payload.code, 'INDEX_FRESH');
    assert.equal(payload.operation, 'check');
    assert.deepEqual(treeSnapshot(fx.root), before);

    const quiet = runCli(fx, ['--check', '--quiet']);
    assert.equal(quiet.status, 0);
    assert.equal(quiet.stdout, '');
    assert.equal(quiet.stderr, '');
    assert.deepEqual(treeSnapshot(fx.root), before);

    const invalid = runCli(fx, ['--json', '--quiet']);
    assert.equal(invalid.status, 2);
    assert.match(invalid.stderr, /mutually exclusive/);
    assert.deepEqual(treeSnapshot(fx.root), before);
  } finally { fx.cleanup(); }
});

await test('--check is byte/mtime zero-mutation for fresh, stale, missing, contract-error, and Python-entrypoint paths', async () => {
  const fx = fixture();
  try {
    fs.writeFileSync(path.join(fx.tasks, 'backlog', 'TASK_14_zero_mutation.md'), backlogBody(14, 'Zero mutation'));
    assert.equal((await runIndexOperation(options(fx))).exitCode, 0);

    let before = treeSnapshot(fx.root);
    let checked = await runIndexOperation(options(fx, { check: true }));
    assert.equal(checked.exitCode, 0);
    assert.deepEqual(treeSnapshot(fx.root), before, 'fresh check mutated the fixture tree');

    fs.writeFileSync(fx.index, JSON.stringify(EMPTY_INDEX, null, 2) + '\n');
    before = treeSnapshot(fx.root);
    checked = await runIndexOperation(options(fx, { check: true }));
    assert.equal(checked.exitCode, 1);
    assert.deepEqual(treeSnapshot(fx.root), before, 'stale check mutated the fixture tree');

    fs.unlinkSync(fx.index);
    before = treeSnapshot(fx.root);
    checked = await runIndexOperation(options(fx, { check: true }));
    assert.equal(checked.exitCode, 1);
    assert.deepEqual(treeSnapshot(fx.root), before, 'missing-index check mutated the fixture tree');

    before = treeSnapshot(fx.root);
    checked = await runIndexOperation(options(fx, {
      check: true,
      outcomeShapePath: path.join(fx.root, 'missing-contract.json')
    }));
    assert.equal(checked.exitCode, 3);
    assert.deepEqual(treeSnapshot(fx.root), before, 'contract-error check mutated the fixture tree');

    assert.equal((await runIndexOperation(options(fx))).exitCode, 0);
    before = treeSnapshot(fx.root);
    const entrypoint = spawnSync('python3', [INDEX_ENTRYPOINT, '--check'], {
      cwd: fx.root, encoding: 'utf8', env: { ...process.env, PYTHONDONTWRITEBYTECODE: '1' }
    });
    assert.equal(entrypoint.status, 0, entrypoint.stdout + entrypoint.stderr);
    assert.deepEqual(treeSnapshot(fx.root), before, 'Python entrypoint check mutated the fixture tree');
  } finally { fx.cleanup(); }
});

await test('importing regen-index.py exposes only an inert current CLI entrypoint', async () => {
  const fx = fixture();
  try {
    const before = treeSnapshot(fx.root);
    const program = [
      'import importlib.util, sys',
      'script = sys.argv[1]',
      'sys.argv = ["embedded", "--not-a-real-cli-option"]',
      'spec = importlib.util.spec_from_file_location("regen_index_entrypoint", script)',
      'module = importlib.util.module_from_spec(spec)',
      'spec.loader.exec_module(module)',
      'assert callable(module.main)',
      'assert not hasattr(module, "outcome_status_of")'
    ].join('; ');
    const imported = spawnSync('python3', ['-c', program, INDEX_ENTRYPOINT], {
      cwd: fx.root,
      encoding: 'utf8',
      env: { ...process.env, PYTHONDONTWRITEBYTECODE: '1' }
    });
    assert.equal(imported.status, 0, imported.stdout + imported.stderr);
    assert.deepEqual(treeSnapshot(fx.root), before);
    assert.equal(fs.existsSync(path.join(fx.root, 'orchestrator', '.cache')), false);
  } finally { fx.cleanup(); }
});

await test('kernel lock serializes direct APIs, ignores spoofed env authority, while --check remains lock-free', async () => {
  const fx = fixture();
  let release;
  let first;
  try {
    let entered;
    const reached = new Promise((resolve) => { entered = resolve; });
    const gate = new Promise((resolve) => { release = resolve; });
    first = runIndexOperation(options(fx, {
      beforeRescan: async () => { entered(); await gate; }
    }));
    await reached;

    const readOnly = await runIndexOperation(options(fx, { check: true }));
    assert.equal(readOnly.exitCode, 0, 'read-only check must not wait for or acquire the publication lock');

    const previous = process.env.TASK_INDEX_LOCK_HELD;
    process.env.TASK_INDEX_LOCK_HELD = '1';
    const contended = await runIndexOperation(options(fx, { lockTimeoutMs: 150 }));
    if (previous === undefined) delete process.env.TASK_INDEX_LOCK_HELD;
    else process.env.TASK_INDEX_LOCK_HELD = previous;
    assert.equal(contended.exitCode, 4);
    assert.equal(contended.diagnostic.code, 'LOCK_TIMEOUT');
    release();
    assert.equal((await first).exitCode, 0);
  } finally {
    if (release) release();
    if (first) await first.catch(() => {});
    fx.cleanup();
  }
});

await test('lock-free check reports an in-flight staged WAL instead of a false fresh verdict', async () => {
  const fx = fixture();
  let release;
  let publisher;
  try {
    fs.writeFileSync(path.join(fx.tasks, 'backlog', 'TASK_140_staged.md'), backlogBody(140, 'Staged'));
    let entered;
    const reached = new Promise((resolve) => { entered = resolve; });
    const gate = new Promise((resolve) => { release = resolve; });
    publisher = runIndexOperation(options(fx, {
      afterPublish: async () => { entered(); await gate; }
    }));
    await reached;
    const before = treeSnapshot(fx.root);
    const check = await runIndexOperation(options(fx, { check: true }));
    assert.equal(check.exitCode, 4);
    assert.equal(check.diagnostic.code, 'INDEX_RECOVERY_REQUIRED');
    assert.deepEqual(treeSnapshot(fx.root), before, 'read-only in-flight inspection mutated WAL state');
    release();
    assert.equal((await publisher).exitCode, 0);
  } finally {
    if (release) release();
    if (publisher) await publisher.catch(() => {});
    fx.cleanup();
  }
});

await test('lock path and every parent are anchored; symlinks and swap-away/back fail closed', async () => {
  if (process.platform === 'win32') return;
  const symlinkFx = fixture();
  const outside = fs.mkdtempSync(path.join(os.tmpdir(), 'index-lock-outside-'));
  try {
    fs.symlinkSync(outside, path.join(symlinkFx.root, 'orchestrator', '.cache'), 'dir');
    const before = fs.readFileSync(symlinkFx.index);
    const blocked = await runIndexOperation(options(symlinkFx));
    assert.equal(blocked.exitCode, 3);
    assert.equal(blocked.diagnostic.code, 'DIRECTORY_UNSAFE');
    assert.deepEqual(fs.readFileSync(symlinkFx.index), before);
    assert.deepEqual(fs.readdirSync(outside), []);
  } finally {
    symlinkFx.cleanup();
    fs.rmSync(outside, { recursive: true, force: true });
  }

  const swapFx = fixture();
  try {
    const before = fs.readFileSync(swapFx.index);
    const raced = await runIndexOperation(options(swapFx, {
      beforeRescan() {
        const lock = path.join(swapFx.root, 'orchestrator', '.cache', 'tasks', 'index.lock');
        const original = lock + '.original';
        fs.renameSync(lock, original);
        fs.writeFileSync(lock, 'foreign-lock\n');
        fs.unlinkSync(lock);
        fs.renameSync(original, lock);
      }
    }));
    assert.equal(raced.exitCode, 4);
    assert.ok(['DIRECTORY_CHANGED', 'LOCK_CHANGED'].includes(raced.diagnostic.code), raced.diagnostic.code);
    assert.deepEqual(fs.readFileSync(swapFx.index), before);
  } finally { swapFx.cleanup(); }
});

await test('Outcome contract is part of the exact source snapshot and external contracts need explicit authority', async () => {
  const fx = fixture();
  try {
    const contractDir = path.join(fx.root, 'orchestrator', 'contracts');
    fs.mkdirSync(contractDir, { recursive: true });
    const contract = path.join(contractDir, 'outcome-shape.json');
    fs.copyFileSync(SHAPE, contract);
    const before = fs.readFileSync(fx.index);
    const raced = await runIndexOperation({
      repoRoot: fx.root,
      tasksDir: fx.tasks,
      outcomeShapePath: contract,
      beforeRescan() { fs.appendFileSync(contract, '\n'); }
    });
    assert.equal(raced.exitCode, 4);
    assert.equal(raced.diagnostic.code, 'TASK_SNAPSHOT_CHANGED');
    assert.deepEqual(fs.readFileSync(fx.index), before);

    const unauthorized = await runIndexOperation({
      repoRoot: fx.root,
      tasksDir: fx.tasks,
      outcomeShapePath: SHAPE,
      check: true
    });
    assert.equal(unauthorized.exitCode, 3);
    assert.equal(unauthorized.diagnostic.code, 'CONTRACT_AUTHORITY_REQUIRED');
  } finally { fx.cleanup(); }
});

await test('ancestor swap and swap-away/back hooks cannot redirect a publication', async () => {
  if (process.platform === 'win32') return;
  const fx = fixture();
  const replacement = path.join(fx.root, 'replacement-tasks');
  try {
    fs.mkdirSync(replacement);
    for (const column of ['backlog', 'pending', 'todo', 'done']) fs.mkdirSync(path.join(replacement, column));
    fs.writeFileSync(path.join(replacement, 'INDEX.json'), '{"replacement":true}\n');
    const before = fs.readFileSync(fx.index);
    const previous = Object.fromEntries([
      'TASK_FS_TEST_ROOT', 'TASK_FS_TEST_SWAP_PATH', 'TASK_FS_TEST_SWAP_WITH',
      'TASK_FS_TEST_SWAP_RESTORE_BEFORE_VERIFY'
    ].map((key) => [key, process.env[key]]));
    Object.assign(process.env, {
      TASK_FS_TEST_ROOT: fx.root,
      TASK_FS_TEST_SWAP_PATH: fx.tasks,
      TASK_FS_TEST_SWAP_WITH: replacement,
      TASK_FS_TEST_SWAP_RESTORE_BEFORE_VERIFY: '1'
    });
    const raced = await runIndexOperation(options(fx));
    for (const [key, value] of Object.entries(previous)) {
      if (value === undefined) delete process.env[key]; else process.env[key] = value;
    }
    assert.equal(raced.exitCode, 4);
    assert.ok(['DIRECTORY_CHANGED', 'TASK_SNAPSHOT_RACE', 'INDEX_OPERATION_IO'].includes(raced.diagnostic.code), raced.diagnostic.code);
    assert.deepEqual(fs.readFileSync(fx.index), before);
    assert.equal(fs.readFileSync(path.join(replacement, 'INDEX.json'), 'utf8'), '{"replacement":true}\n');
  } finally { fx.cleanup(); }
});

await test('last-window destination appearance is retained byte-for-byte with incumbent evidence and no false success', async () => {
  const fx = fixture();
  try {
    fs.writeFileSync(path.join(fx.tasks, 'backlog', 'TASK_15_foreign.md'), backlogBody(15, 'Foreign race'));
    const incumbent = fs.readFileSync(fx.index);
    const foreignPath = path.join(fx.tasks, 'foreign-index.json');
    const foreign = Buffer.from('{"foreign":"must-survive"}\n');
    fs.writeFileSync(foreignPath, foreign);
    const previous = {
      root: process.env.TASK_FS_TEST_ROOT,
      appear: process.env.TASK_INDEX_TEST_APPEAR_TARGET_WITH
    };
    process.env.TASK_FS_TEST_ROOT = fx.root;
    process.env.TASK_INDEX_TEST_APPEAR_TARGET_WITH = foreignPath;
    const raced = await runIndexOperation(options(fx));
    if (previous.root === undefined) delete process.env.TASK_FS_TEST_ROOT; else process.env.TASK_FS_TEST_ROOT = previous.root;
    if (previous.appear === undefined) delete process.env.TASK_INDEX_TEST_APPEAR_TARGET_WITH;
    else process.env.TASK_INDEX_TEST_APPEAR_TARGET_WITH = previous.appear;
    assert.equal(raced.exitCode, 4);
    assert.equal(raced.diagnostic.code, 'DESTINATION_CHANGED');
    assert.deepEqual(fs.readFileSync(fx.index), foreign);
    const evidence = fs.readdirSync(fx.tasks).filter((name) => /^\.task-index-evidence-[a-f0-9]{48}$/.test(name));
    assert.equal(evidence.length, 1);
    const evidenceDir = path.join(fx.tasks, evidence[0]);
    assert.ok(fs.readdirSync(evidenceDir).includes('source'));
    assert.deepEqual(fs.readFileSync(path.join(evidenceDir, 'source')), incumbent);
  } finally { fx.cleanup(); }
});

await test('exact target replacement after candidate link preserves foreign target plus both WAL generations', async () => {
  const fx = fixture();
  try {
    fs.writeFileSync(path.join(fx.tasks, 'backlog', 'TASK_151_replacement.md'), backlogBody(151, 'Replacement'));
    const incumbent = fs.readFileSync(fx.index);
    const foreignPath = path.join(fx.tasks, 'replacement-index.json');
    const foreign = Buffer.from('{"foreign":"replacement"}\n');
    fs.writeFileSync(foreignPath, foreign);
    const previous = {
      root: process.env.TASK_FS_TEST_ROOT,
      replacement: process.env.TASK_INDEX_TEST_REPLACE_PUBLISHED_WITH
    };
    process.env.TASK_FS_TEST_ROOT = fx.root;
    process.env.TASK_INDEX_TEST_REPLACE_PUBLISHED_WITH = foreignPath;
    const raced = await runIndexOperation(options(fx));
    if (previous.root === undefined) delete process.env.TASK_FS_TEST_ROOT; else process.env.TASK_FS_TEST_ROOT = previous.root;
    if (previous.replacement === undefined) delete process.env.TASK_INDEX_TEST_REPLACE_PUBLISHED_WITH;
    else process.env.TASK_INDEX_TEST_REPLACE_PUBLISHED_WITH = previous.replacement;
    assert.equal(raced.exitCode, 4);
    assert.deepEqual(fs.readFileSync(fx.index), foreign);
    const evidenceName = fs.readdirSync(fx.tasks).find((name) => /^\.task-index-evidence-[a-f0-9]{48}$/.test(name));
    assert.ok(evidenceName);
    const evidence = path.join(fx.tasks, evidenceName);
    assert.deepEqual(fs.readFileSync(path.join(evidence, 'source')), incumbent);
    assert.ok(fs.statSync(path.join(evidence, 'candidate')).size > 0);
    assert.ok(fs.statSync(path.join(evidence, 'published')).size > 0);
  } finally { fx.cleanup(); }
});

await test('mutation test hooks are inert without isolated-fixture authority', async () => {
  const fx = fixture();
  try {
    const replacement = path.join(fx.tasks, 'unauthorized-replacement.json');
    fs.writeFileSync(replacement, '{"unauthorized":true}\n');
    const before = fs.readFileSync(fx.index);
    const beforeTree = treeSnapshot(fx.root);
    const previousRoot = process.env.TASK_FS_TEST_ROOT;
    const previousHook = process.env.TASK_INDEX_TEST_REPLACE_TARGET_WITH;
    delete process.env.TASK_FS_TEST_ROOT;
    process.env.TASK_INDEX_TEST_REPLACE_TARGET_WITH = replacement;
    const blocked = await runIndexOperation(options(fx));
    if (previousRoot === undefined) delete process.env.TASK_FS_TEST_ROOT; else process.env.TASK_FS_TEST_ROOT = previousRoot;
    if (previousHook === undefined) delete process.env.TASK_INDEX_TEST_REPLACE_TARGET_WITH;
    else process.env.TASK_INDEX_TEST_REPLACE_TARGET_WITH = previousHook;
    assert.equal(blocked.exitCode, 4);
    assert.equal(blocked.diagnostic.code, 'TEST_HOOK_INVALID');
    assert.deepEqual(fs.readFileSync(fx.index), before);
    assert.equal(fs.readFileSync(replacement, 'utf8'), '{"unauthorized":true}\n');
    assert.deepEqual(treeSnapshot(fx.root), beforeTree);
    assertNoTemps(fx);
  } finally { fx.cleanup(); }
});

await test('post-link non-cooperating replacement is detected and its bytes remain the target', async () => {
  const fx = fixture();
  try {
    fs.writeFileSync(path.join(fx.tasks, 'backlog', 'TASK_16_post_link.md'), backlogBody(16, 'Post link'));
    const foreign = Buffer.from('{"foreign":"post-link"}\n');
    const raced = await runIndexOperation(options(fx, {
      afterPublish() { fs.writeFileSync(fx.index, foreign); }
    }));
    assert.equal(raced.exitCode, 4);
    assert.ok(['DESTINATION_CHANGED', 'TASK_SNAPSHOT_CHANGED_AFTER_PUBLICATION'].includes(raced.diagnostic.code));
    assert.deepEqual(fs.readFileSync(fx.index), foreign);
    assert.ok(fs.readdirSync(fx.tasks).some((name) => /^\.task-index-evidence-[a-f0-9]{48}$/.test(name)));
  } finally { fx.cleanup(); }
});

await test('destination swap-away/back is detected even when identical candidate inode returns to the name', async () => {
  const fx = fixture();
  try {
    fs.writeFileSync(path.join(fx.tasks, 'backlog', 'TASK_161_swap_back.md'), backlogBody(161, 'Swap back'));
    const incumbent = fs.readFileSync(fx.index);
    const raced = await runIndexOperation(options(fx, {
      afterPublish() {
        const held = fx.index + '.held';
        fs.renameSync(fx.index, held);
        fs.writeFileSync(fx.index, '{"foreign":"transient"}\n');
        fs.unlinkSync(fx.index);
        fs.renameSync(held, fx.index);
      }
    }));
    assert.equal(raced.exitCode, 4);
    assert.ok(['DIRECTORY_CHANGED', 'DESTINATION_CHANGED'].includes(raced.diagnostic.code), raced.diagnostic.code);
    assert.deepEqual(fs.readFileSync(fx.index), incumbent);
    assertNoTemps(fx);
  } finally { fx.cleanup(); }
});

await test('diagnostic failure is honest, foreign-safe, and cannot damage a successful INDEX publication', async () => {
  if (process.platform === 'win32') return;
  const fx = fixture();
  try {
    fs.writeFileSync(path.join(fx.tasks, 'backlog', 'TASK_17_diagnostic.md'), backlogBody(17, 'Diagnostic'));
    const diagnosticDir = path.dirname(fx.diagnostic);
    fs.mkdirSync(diagnosticDir, { recursive: true });
    const outside = path.join(fx.root, 'outside-diagnostic.json');
    const sentinel = Buffer.from('{"outside":true}\n');
    fs.writeFileSync(outside, sentinel);
    fs.symlinkSync(outside, fx.diagnostic);
    const result = await runIndexOperation(options(fx));
    assert.equal(result.exitCode, 0);
    assert.equal(result.diagnostic.code, 'INDEX_PUBLISHED');
    assert.match(result.diagnostic.diagnosticWriteError, /regular file|unsafe/i);
    assert.deepEqual(fs.readFileSync(outside), sentinel);
    assert.equal(fs.lstatSync(fx.diagnostic).isSymbolicLink(), true);
    assert.equal(JSON.parse(fs.readFileSync(fx.index, 'utf8')).backlog[0].stem, 'TASK_17_diagnostic');
  } finally { fx.cleanup(); }
});

await test('diagnostics and JSON output never expose local absolute roots or the external contract path', async () => {
  const fx = fixture();
  try {
    fs.writeFileSync(path.join(fx.tasks, 'todo', 'TASK_18_private.md'), '# TASK 18 — Private\n');
    const result = await runIndexOperation(options(fx));
    assert.equal(result.exitCode, 1);
    const serialized = JSON.stringify(result.diagnostic);
    assert.equal(serialized.includes(fx.root), false);
    assert.equal(serialized.includes(SHAPE), false);
    const persisted = fs.readFileSync(fx.diagnostic, 'utf8');
    assert.equal(persisted.includes(fx.root), false);
    assert.equal(persisted.includes(SHAPE), false);
  } finally { fx.cleanup(); }
});

await test('read-only recovery inspection accepts >2^53 decimal proofs without Number coercion', async () => {
  const fx = fixture();
  try {
    const token = '1'.repeat(48);
    const operation = path.join(fx.tasks, '.task-index-cas-' + token);
    fs.mkdirSync(operation);
    const candidate = Buffer.from('{}\n');
    const manifest = {
      version: 1,
      kind: 'task-index',
      operationId: token,
      targetName: 'INDEX.json',
      candidateHash: hash(candidate),
      candidateSize: candidate.length,
      expected: {
        hash: hash(fs.readFileSync(fx.index)),
        proof: {
          dev: '9007199254740993',
          ino: '18446744073709551615',
          mode: 33188,
          size: fs.statSync(fx.index).size,
          mtimeNs: '9223372036854775807',
          ctimeNs: '9223372036854775806'
        }
      }
    };
    fs.writeFileSync(path.join(operation, 'manifest.json'), canonicalManifest(manifest));
    fs.writeFileSync(path.join(operation, 'candidate'), candidate);
    const result = await runIndexOperation(options(fx, { check: true }));
    assert.equal(result.exitCode, 4);
    assert.equal(result.diagnostic.code, 'INDEX_RECOVERY_REQUIRED');
  } finally { fx.cleanup(); }
});

await test('malformed, symlinked, oversized, and unbounded recovery artifacts fail closed without mutation', async () => {
  const cases = [
    (fx) => fs.mkdirSync(path.join(fx.tasks, '.task-index-cas-not-canonical')),
    (fx) => {
      const dir = path.join(fx.tasks, '.task-index-evidence-' + '2'.repeat(48));
      fs.mkdirSync(dir); fs.writeFileSync(path.join(dir, 'unknown'), 'x');
    },
    (fx) => {
      const dir = path.join(fx.tasks, '.task-index-cas-' + '3'.repeat(48));
      fs.mkdirSync(dir); fs.symlinkSync(fx.index, path.join(dir, 'candidate'));
    },
    (fx) => fs.writeFileSync(fx.index, Buffer.alloc(8 * 1024 * 1024 + 1, 0x20)),
    (fx) => { fs.unlinkSync(fx.index); fs.mkdirSync(fx.index); },
    (fx) => {
      for (let index = 0; index < 33; index++) {
        fs.mkdirSync(path.join(fx.tasks, '.task-index-evidence-' + index.toString(16).padStart(48, '0')));
      }
    }
  ];
  for (const setup of cases) {
    const fx = fixture();
    try {
      setup(fx);
      const before = treeSnapshot(fx.root);
      const result = await runIndexOperation(options(fx, { check: true }));
      assert.equal(result.exitCode, 3, result.diagnostic.code);
      assert.deepEqual(treeSnapshot(fx.root), before);
    } finally { fx.cleanup(); }
  }
});

await test('non-canonical decimal fields in durable manifests fail closed', async () => {
  const fx = fixture();
  try {
    const token = '4'.repeat(48);
    const operation = path.join(fx.tasks, '.task-index-cas-' + token);
    fs.mkdirSync(operation);
    const candidate = Buffer.from('{}\n');
    const manifest = {
      version: 1, kind: 'task-index', operationId: token, targetName: 'INDEX.json',
      candidateHash: hash(candidate), candidateSize: candidate.length,
      expected: {
        hash: hash(fs.readFileSync(fx.index)),
        proof: { dev: '09007199254740993', ino: '1', mode: 33188,
          size: fs.statSync(fx.index).size, mtimeNs: '1', ctimeNs: '1' }
      }
    };
    fs.writeFileSync(path.join(operation, 'manifest.json'), canonicalManifest(manifest));
    fs.writeFileSync(path.join(operation, 'candidate'), candidate);
    const result = await runIndexOperation(options(fx, { check: true }));
    assert.equal(result.exitCode, 3);
    assert.equal(result.diagnostic.code, 'MANIFEST_INVALID');
  } finally { fx.cleanup(); }
});

await test('every durable INDEX boundary is crash-recoverable on the next ordinary publish', async () => {
  const labels = [
    'after-mkdir', 'after-manifest', 'after-candidate', 'after-detach', 'after-publish',
    'after-commit-marker', 'after-commit-rename', 'cleanup-after-source',
    'cleanup-after-candidate', 'cleanup-after-commit', 'cleanup-after-manifest'
  ];
  for (const [index, label] of labels.entries()) {
    const fx = fixture();
    try {
      fs.writeFileSync(path.join(fx.tasks, 'backlog', `TASK_${100 + index}_crash.md`),
        backlogBody(100 + index, 'Crash ' + label));
      const crashed = runCli(fx, ['--quiet'], { TASK_INDEX_TEST_CRASH_AT: label });
      assert.notEqual(crashed.status, 0, label + ': crash hook did not interrupt publication');
      const recovered = runCli(fx, ['--quiet']);
      assert.equal(recovered.status, 0, label + ': ' + recovered.stdout + recovered.stderr);
      const checked = runCli(fx, ['--check', '--quiet']);
      assert.equal(checked.status, 0, label + ': post-recovery check failed');
      assertNoTemps(fx);
      const rowCount = JSON.parse(fs.readFileSync(fx.index, 'utf8')).backlog.length;
      assert.equal(rowCount, 1, label + ': recovered generation is not canonical');
    } finally { fx.cleanup(); }
  }
});

await test('committed recovery distinguishes a same-bytes foreign inode and retains rollback evidence', async () => {
  const fx = fixture();
  try {
    const incumbent = fs.readFileSync(fx.index);
    fs.writeFileSync(path.join(fx.tasks, 'backlog', 'TASK_119_committed_lineage.md'),
      backlogBody(119, 'Committed lineage'));
    const crashed = runCli(fx, ['--quiet'], { TASK_INDEX_TEST_CRASH_AT: 'after-commit-rename' });
    assert.notEqual(crashed.status, 0);
    const committedBytes = fs.readFileSync(fx.index);
    const originalTarget = fx.index + '.original-candidate';
    fs.renameSync(fx.index, originalTarget);
    fs.writeFileSync(fx.index, committedBytes);
    fs.unlinkSync(originalTarget);

    const recovered = runCli(fx, ['--quiet']);
    assert.equal(recovered.status, 0, recovered.stdout + recovered.stderr);
    const evidenceNames = fs.readdirSync(fx.tasks)
      .filter((name) => /^\.task-index-evidence-[a-f0-9]{48}$/.test(name));
    assert.equal(evidenceNames.length, 1);
    const evidence = path.join(fx.tasks, evidenceNames[0]);
    assert.deepEqual(fs.readFileSync(path.join(evidence, 'candidate')), committedBytes);
    assert.deepEqual(fs.readFileSync(path.join(evidence, 'source')), incumbent);
    assert.equal(runCli(fx, ['--check', '--quiet']).status, 0);
  } finally { fx.cleanup(); }
});

await test('a crash after publishing into an initially absent target rolls back absence before retry', async () => {
  const fx = fixture();
  try {
    fs.unlinkSync(fx.index);
    fs.writeFileSync(path.join(fx.tasks, 'backlog', 'TASK_120_absent.md'), backlogBody(120, 'Absent'));
    const crashed = runCli(fx, ['--quiet'], { TASK_INDEX_TEST_CRASH_AT: 'after-publish' });
    assert.notEqual(crashed.status, 0);
    const recovered = runCli(fx, ['--quiet']);
    assert.equal(recovered.status, 0, recovered.stdout + recovered.stderr);
    assert.equal(runCli(fx, ['--check', '--quiet']).status, 0);
    assertNoTemps(fx);
  } finally { fx.cleanup(); }
});

process.stdout.write('test-index-fail-closed.mjs: ' + passed + ' passed, ' + failed + ' failed\n');
if (failed) process.exitCode = 1;
