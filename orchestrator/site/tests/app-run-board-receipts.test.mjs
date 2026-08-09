import assert from 'node:assert/strict';
import crypto from 'node:crypto';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { createRequire } from 'node:module';

// Regression net for the receipt -> board bridge: receipts persisted by the
// app runner must clear (or refuse to clear) manual validation on the task
// summary through the production default reader and the canonical task BODY
// revision domain — never through row.sourceRevision, another task's receipt,
// or a stale/corrupt receipt.

const require = createRequire(import.meta.url);
const root = fs.mkdtempSync(path.join(os.tmpdir(), 'app-run-board-'));
const orchestrator = path.join(root, 'orchestrator');
const tasks = path.join(orchestrator, 'tasks');
const doneStem = 'TASK_1_done_manual';
const otherStem = 'TASK_2_other_done';
const pendingStem = 'TASK_3_pending_body';

for (const directory of [
  path.join(tasks, 'backlog'),
  path.join(tasks, 'pending'),
  path.join(tasks, 'todo'),
  path.join(tasks, 'done'),
]) fs.mkdirSync(directory, { recursive: true });

const doneText = (suffix = '') => [
  '# TASK 1 — Done manual fixture',
  '',
  '## Acceptance ##',
  '',
  '### Manual ###',
  '',
  '- Launches on the selected virtual device' + suffix,
  '',
].join('\n');
fs.writeFileSync(path.join(tasks, 'done', doneStem + '.md'), doneText());
fs.writeFileSync(path.join(tasks, 'done', otherStem + '.md'), [
  '# TASK 2 — Other done fixture',
  '',
  '## Acceptance ##',
  '',
  '### Manual ###',
  '',
  '- A completely different manual check',
  '',
].join('\n'));
fs.writeFileSync(path.join(tasks, 'backlog', pendingStem + '.md'), [
  '# TASK 3 — Pending body fixture',
  '',
  '## Acceptance ##',
  '',
  '### Manual ###',
  '',
  '- The pending checklist must come from this backlog body',
  '',
].join('\n'));
fs.writeFileSync(path.join(tasks, 'pending', pendingStem + '.md'), [
  '---',
  'round: 1',
  '---',
  '',
  '## Q1 — Which flow should the emulator open first?',
  '',
].join('\n'));

const fingerprint = 'sha256:' + '2'.repeat(64);
const indexRow = (stem, state, title) => ({
  stem,
  title,
  state,
  createdAt: '2026-07-26T11:00:00.000Z',
  doneAt: state === 'done' ? '2026-07-26T11:30:00.000Z' : null,
  sourceRevision: fingerprint,
  origin: { kind: 'manual', type: 'manual', ref: 'fixture:' + stem, fingerprint },
  dependsOn: [],
  splitFrom: null,
  outcomeStatus: state === 'done' ? 'completed' : null,
  questionsCount: state === 'pending' ? 1 : null,
  round: state === 'pending' ? 1 : null,
});
fs.writeFileSync(path.join(tasks, 'INDEX.json'), JSON.stringify({
  version: 2,
  generatedAt: '2026-07-26T12:00:00.000Z',
  backlog: [],
  pending: [indexRow(pendingStem, 'pending', 'Pending body fixture')],
  todo: [],
  done: [
    indexRow(doneStem, 'done', 'Done manual fixture'),
    indexRow(otherStem, 'done', 'Other done fixture'),
  ],
}, null, 2) + '\n');

process.env.ORCHESTRATOR_PROJECT_ROOT = root;
process.env.ORCHESTRATOR_TASKS_DIR = tasks;
process.env.ORCHESTRATOR_CACHE_DIR = path.join(orchestrator, '.cache');
process.env.ORCHESTRATOR_APP_RUN_DIR = path.join(orchestrator, '.cache', 'runtime', 'app-run');
process.env.ORCHESTRATOR_APP_RUN_AUTHORITY_ROOT = root;

const appRunPaths = require('../server/paths.js');
const storage = require('../server/app-run-storage.js');
const validation = require('../server/app-run-validation.js');
const taskSummary = require('../server/task-summary.js');
storage.init();

const sha = (file) => 'sha256:' + crypto.createHash('sha256')
  .update(fs.readFileSync(file)).digest('hex');

// The pending checklist must be parsed from the backlog BODY, never from the
// pending questions sidecar, and its revision must pin those exact bytes.
const pendingChecklist = validation.checklist(pendingStem);
assert.equal(pendingChecklist.ok, true);
assert.deepEqual(pendingChecklist.items.map((item) => item.text),
  ['The pending checklist must come from this backlog body']);
assert.equal(pendingChecklist.taskSourceRevision,
  sha(path.join(tasks, 'backlog', pendingStem + '.md')),
  'a pending task checklist must pin the backlog body bytes');

function sessionFor(stem, marker) {
  return {
    sessionId: 'session-' + marker.repeat(36),
    sessionRevision: 2,
    taskStem: stem,
    jobId: 'job-' + marker.repeat(36),
    platform: 'android',
    deviceSummary: 'Pixel 8 · Android 15',
    artifactId: 'artifact-' + marker.repeat(36),
    appProjectSourceRevision: 'sha256:' + marker.repeat(64),
  };
}

async function saveReceipt(stem, session, currentSourceRevision) {
  const checklist = validation.checklist(stem);
  assert.equal(checklist.ok, true);
  const saved = await validation.save({
    taskStem: stem,
    expectedTaskSourceRevision: checklist.taskSourceRevision,
    sessionId: session.sessionId,
    expectedSessionRevision: session.sessionRevision,
    validationRevision: checklist.validationRevision,
    items: checklist.items.map((item) => ({
      itemId: item.itemId, result: 'pass', note: null, screenshotIds: [],
    })),
    acknowledgeStaleTask: false,
    idempotencyKey: 'board-receipt-' + stem + '-' + crypto.randomBytes(6).toString('hex'),
  }, {
    resolveSession() { return { ok: true, session }; },
    screenshotOwned() { return true; },
    currentSourceRevision() {
      return currentSourceRevision === undefined
        ? session.appProjectSourceRevision : currentSourceRevision;
    },
  });
  assert.equal(saved.status, 201);
  return saved.receipt;
}

function summaryDependencies() {
  const index = JSON.parse(fs.readFileSync(path.join(tasks, 'INDEX.json'), 'utf8'));
  const canonicalRows = Object.fromEntries(['backlog', 'pending', 'todo', 'done']
    .flatMap((column) => index[column]).map((row) => [row.stem, row]));
  const artifacts = new Map([
    [doneStem, { done: { contentHash: sha(path.join(tasks, 'done', doneStem + '.md')) } }],
    [otherStem, { done: { contentHash: sha(path.join(tasks, 'done', otherStem + '.md')) } }],
  ]);
  const metadata = new Map([
    [doneStem, { outcome: { acceptance: [{ verdict: 'manual' }] } }],
    [otherStem, { outcome: { acceptance: [{ verdict: 'manual' }] } }],
  ]);
  return {
    indexRead: { value: index, revision: 'sha256:' + 'b'.repeat(64) },
    canonicalRows,
    validation: { indexStatus: 'fresh', findings: [], _model: { metadata, artifacts } },
    snapshot: {
      runnerActive: true,
      progress: { setupDone: true, inProgress: [], finalizations: [], requests: [], shallowIntake: {} },
      sessions: {}, screensCache: {}, reviewerConfig: { state: 'valid' }, status: { worker: {} },
    },
    sourceAvailability: { design: {}, coverage: {}, drift: {} },
    latestReader: () => ({ event: null, truncated: false }),
    // No validationReceipts injection: builds below exercise the production
    // default reader against the real app-run history directory.
  };
}

function summaryRow(stem) {
  const summary = taskSummary.build({ limit: 100 }, summaryDependencies());
  const row = ['backlog', 'pending', 'todo', 'done']
    .flatMap((column) => summary.columns[column])
    .find((candidate) => candidate.stem === stem);
  assert.ok(row, 'summary must project ' + stem);
  return { summary, row };
}

// 1) Before any receipt: manual validation is required and pending.
let projected = summaryRow(doneStem);
assert.deepEqual(projected.row.appValidation, { required: true, current: false, overall: null });
assert.equal(projected.row.primaryAction.kind, 'validate-in-app');

// 2) A current receipt saved by the runner clears manual validation through
// the production reader, and pins the exact session/job/artifact identity.
const doneSession = sessionFor(doneStem, 'a');
const receipt = await saveReceipt(doneStem, doneSession);
assert.equal(receipt.sessionId, doneSession.sessionId);
assert.equal(receipt.runJobId, doneSession.jobId);
assert.equal(receipt.artifactId, doneSession.artifactId);
projected = summaryRow(doneStem);
assert.deepEqual(projected.row.appValidation, { required: true, current: true, overall: 'passed' });
assert.equal(projected.row.primaryAction.kind, 'review-result');
assert.equal(projected.row.blockers.some((blocker) => blocker.kind === 'validation-required'), false);
assert.equal(projected.summary.limitations.includes('app-validation-receipts-invalid'), false);

// 3) Another task's receipt must never clear this task, and this task's
// receipt must never clear the other done task.
projected = summaryRow(otherStem);
assert.deepEqual(projected.row.appValidation, { required: true, current: false, overall: null },
  'a receipt of a different task must not close manual validation');

// 4) Editing the done task makes the stored receipt stale on the board (the
// receipt file itself is untouched and survives as historical evidence).
fs.writeFileSync(path.join(tasks, 'done', doneStem + '.md'), doneText(' (edited)'));
projected = summaryRow(doneStem);
assert.equal(projected.row.appValidation.current, false,
  'a receipt of an older task revision must not stay current after an edit');
assert.equal(projected.row.primaryAction.kind, 'validate-in-app');
assert.equal(validation.history(doneStem, 10).length, 1,
  'the stale receipt must remain persisted history, not disappear');
const projectedStale = validation.currentReceipt(
  validation.history(doneStem, 1)[0],
  sha(path.join(tasks, 'done', doneStem + '.md')),
  doneSession.appProjectSourceRevision,
);
assert.equal(projectedStale.staleTask, true,
  'the panel projection must label the surviving receipt as stale');

// 5) A receipt that was already stale against the project source at save time
// must not clear manual validation either.
fs.writeFileSync(path.join(tasks, 'done', doneStem + '.md'), doneText(' (edited twice)'));
const staleSourceReceipt = await saveReceipt(doneStem, sessionFor(doneStem, 'b'),
  'sha256:' + 'f'.repeat(64));
assert.equal(staleSourceReceipt.staleSource, true);
projected = summaryRow(doneStem);
assert.equal(projected.row.appValidation.current, false,
  'a stale-source receipt must never satisfy the board');

// 6) A receipt matching the current body revision written for the OTHER task
// clears only that task.
await saveReceipt(otherStem, sessionFor(otherStem, 'c'));
projected = summaryRow(otherStem);
assert.equal(projected.row.appValidation.current, true);
projected = summaryRow(doneStem);
assert.equal(projected.row.appValidation.current, false);

// 7) Corruption fails closed and is explicitly visible; removing the corrupt
// record restores the healthy projection via the stat-keyed reader.
const corruptId = storage.randomId('receipt');
storage.writeJson(appRunPaths.APP_RUN_HISTORY_DIR, corruptId, {
  schemaVersion: 1, receiptId: corruptId, unexpected: true,
}, 128 * 1024);
projected = summaryRow(otherStem);
assert.equal(projected.summary.partial, true);
assert.ok(projected.summary.limitations.includes('app-validation-receipts-invalid'),
  'receipt corruption must surface as a named limitation');
assert.equal(projected.row.appValidation.current, false,
  'corruption must fail closed instead of trusting unverifiable receipts');
storage.remove(appRunPaths.APP_RUN_HISTORY_DIR, corruptId, '.json', 128 * 1024);
projected = summaryRow(otherStem);
assert.equal(projected.summary.limitations.includes('app-validation-receipts-invalid'), false);
assert.equal(projected.row.appValidation.current, true,
  'removing the corrupt record must restore the receipt-backed projection');

fs.rmSync(root, { recursive: true, force: true });
