import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const root = fs.mkdtempSync(path.join(os.tmpdir(), 'app-run-validation-'));
const orchestrator = path.join(root, 'orchestrator');
const tasks = path.join(orchestrator, 'tasks');
const stem = 'TASK_1_runtime_validation';
const fingerprint = 'sha256:' + '2'.repeat(64);
const taskText = [
  '# TASK_1 — Runtime validation fixture',
  '',
  '```md',
  '## Acceptance',
  '### Manual',
  '- A fenced decoy must not become a checklist item',
  '```',
  '',
  '<!--',
  '## Acceptance',
  '### Manual',
  '- An HTML-hidden decoy must not become a checklist item',
  '-->',
  '',
  '## Acceptance ##',
  '',
  '### Manual ###',
  '',
  '- Launches on the selected virtual device',
  '  - Confirm the initial screen is visible',
  '- A captured failure can be attached',
  '',
].join('\n');

for (const directory of [
  path.join(tasks, 'backlog'),
  path.join(tasks, 'pending'),
  path.join(tasks, 'todo'),
  path.join(tasks, 'done'),
]) fs.mkdirSync(directory, { recursive: true });
fs.writeFileSync(path.join(tasks, 'todo', stem + '.md'), taskText);
fs.writeFileSync(path.join(tasks, 'INDEX.json'), JSON.stringify({
  version: 2,
  generatedAt: '2026-07-26T12:00:00.000Z',
  backlog: [],
  pending: [],
  todo: [{
    stem,
    title: 'Runtime validation fixture',
    state: 'todo',
    createdAt: '2026-07-26T11:00:00.000Z',
    doneAt: null,
    sourceRevision: fingerprint,
    origin: { kind: 'manual', type: 'manual', ref: 'fixture:runtime-validation', fingerprint },
    dependsOn: [],
    splitFrom: null,
    outcomeStatus: null,
    questionsCount: null,
    round: null,
  }],
  done: [],
}, null, 2) + '\n');

process.env.ORCHESTRATOR_PROJECT_ROOT = root;
process.env.ORCHESTRATOR_TASKS_DIR = tasks;
process.env.ORCHESTRATOR_CACHE_DIR = path.join(orchestrator, '.cache');
process.env.ORCHESTRATOR_APP_RUN_DIR = path.join(orchestrator, '.cache', 'runtime', 'app-run');
process.env.ORCHESTRATOR_APP_RUN_AUTHORITY_ROOT = root;

const storage = require('../server/app-run-storage.js');
const validation = require('../server/app-run-validation.js');
storage.init();

const checklist = validation.checklist(stem);
assert.equal(checklist.ok, true);
assert.equal(checklist.items.length, 2);
assert.deepEqual(checklist.items[0].notes, ['Confirm the initial screen is visible']);

const sessionId = 'session-' + 'a'.repeat(36);
const session = {
  sessionId,
  sessionRevision: 3,
  taskStem: stem,
  jobId: 'job-' + 'b'.repeat(36),
  platform: 'android',
  deviceSummary: 'Pixel 8 · Android 15',
  artifactId: 'artifact-' + 'c'.repeat(36),
  appProjectSourceRevision: 'sha256:' + 'd'.repeat(64),
};
const items = checklist.items.map((item) => ({
  itemId: item.itemId,
  result: 'pass',
  note: null,
  screenshotIds: [],
}));
const body = {
  taskStem: stem,
  expectedTaskSourceRevision: checklist.taskSourceRevision,
  sessionId,
  expectedSessionRevision: session.sessionRevision,
  validationRevision: checklist.validationRevision,
  items,
  acknowledgeStaleTask: false,
  idempotencyKey: 'validation-fixture-current',
};
const context = {
  resolveSession() { return { ok: true, session }; },
  screenshotOwned() { return true; },
  currentSourceRevision() { return session.appProjectSourceRevision; },
};

const mismatched = await validation.save(body, {
  ...context,
  resolveSession() { return { ok: true, session: { ...session, taskStem: 'TASK_2_other' } }; },
});
assert.equal(mismatched.error, 'context-mismatch');
const foreignScreenshot = await validation.save({
  ...body,
  idempotencyKey: 'validation-fixture-foreign-screenshot',
  items: body.items.map((item, index) => ({
    ...item,
    screenshotIds: index === 0 ? ['shot-' + 'e'.repeat(36)] : [],
  })),
}, {
  ...context,
  screenshotOwned() { return false; },
});
assert.deepEqual(foreignScreenshot, {
  ok: false,
  status: 400,
  error: 'screenshot-not-owned',
}, 'a screenshot owned by another session or task must be rejected');
const corruptScreenshot = await validation.save({
  ...body,
  idempotencyKey: 'validation-fixture-corrupt-screenshot',
  items: body.items.map((item, index) => ({
    ...item,
    screenshotIds: index === 0 ? ['shot-' + 'f'.repeat(36)] : [],
  })),
}, {
  ...context,
  screenshotOwned() {
    return { ok: false, status: 409, error: 'runtime-recovery-required' };
  },
});
assert.deepEqual(corruptScreenshot, {
  ok: false,
  status: 409,
  error: 'runtime-recovery-required',
}, 'corrupt screenshot evidence must not be mislabeled as foreign ownership');

const saved = await validation.save(body, context);
assert.equal(saved.status, 201);
assert.equal(saved.receipt.overall, 'passed');
assert.equal(saved.receipt.staleSource, false);
assert.equal(saved.receipt.staleTask, false);
assert.equal(saved.receipt.sessionId, sessionId,
  'a receipt must durably pin the exact validated session');
assert.equal(saved.receipt.runJobId, session.jobId);
assert.equal(saved.receipt.artifactId, session.artifactId);
assert.equal(saved.receipt.appProjectSourceRevision, session.appProjectSourceRevision);
assert.equal(validation.validateReceipt(saved.receipt), null);
assert.equal(saved.journalRecorded, true,
  'a healthy journal append must be reported to the caller');
const journalFile = path.join(orchestrator, '.cache', 'tasks', 'journal', stem + '.jsonl');
const journalEvents = fs.readFileSync(journalFile, 'utf8').trim().split('\n').map((line) => JSON.parse(line));
assert.equal(journalEvents.length, 1);
assert.equal(journalEvents[0].phase, 'runtime-verify');
assert.equal(journalEvents[0].status, 'ok');
assert.equal(journalEvents[0].meta.reportId, saved.receipt.receiptId,
  'the journal event must reference the already-persisted receipt');

const staleBody = {
  ...body,
  idempotencyKey: 'validation-fixture-stale-preview',
};
fs.appendFileSync(path.join(tasks, 'todo', stem + '.md'), '- A newly added manual check\n');
const preview = await validation.save(staleBody, context);
assert.equal(preview.status, 409);
assert.equal(preview.confirmationRequired, true);
assert.equal(preview.current.items.length, 3);
const nowStale = validation.currentReceipt(
  saved.receipt,
  preview.current.taskSourceRevision,
  session.appProjectSourceRevision,
);
assert.equal(nowStale.staleTask, true,
  'a previously current receipt must project as stale after the task changes');
assert.equal(validation.currentReceipt(
  saved.receipt,
  saved.receipt.taskSourceRevision,
  'sha256:' + 'e'.repeat(64),
).staleSource, true,
  'a previously current receipt must project as stale after app sources change');
const staleSaved = await validation.save({
  ...staleBody,
  acknowledgeStaleTask: true,
  idempotencyKey: 'validation-fixture-stale-confirmed',
}, context);
assert.equal(staleSaved.status, 201);
assert.equal(staleSaved.receipt.staleTask, true);
assert.equal(staleSaved.receipt.taskSourceRevision, checklist.taskSourceRevision);
assert.equal(staleSaved.receipt.checklist.length, 2);
assert.equal(validation.history(stem, 10).length, 2);

// A journal outage must be reported separately and must never roll back or
// hide the already-persisted receipt.
const journalDirectory = path.join(orchestrator, '.cache', 'tasks', 'journal');
fs.rmSync(journalDirectory, { recursive: true, force: true });
fs.writeFileSync(journalDirectory, 'not a directory\n');
const freshChecklist = validation.checklist(stem);
assert.equal(freshChecklist.ok, true);
const journalBlocked = await validation.save({
  taskStem: stem,
  expectedTaskSourceRevision: freshChecklist.taskSourceRevision,
  sessionId,
  expectedSessionRevision: session.sessionRevision,
  validationRevision: freshChecklist.validationRevision,
  items: freshChecklist.items.map((item) => ({
    itemId: item.itemId, result: 'pass', note: null, screenshotIds: [],
  })),
  acknowledgeStaleTask: false,
  idempotencyKey: 'validation-fixture-journal-outage',
}, context);
assert.equal(journalBlocked.status, 201,
  'a journal failure must not fail the validation save');
assert.equal(journalBlocked.journalRecorded, false,
  'a journal failure must be visible to the caller');
assert.equal(validation.history(stem, 10).length, 3,
  'the receipt must remain persisted evidence even when the journal is down');
fs.rmSync(journalDirectory, { force: true });

// Empty Manual sections of two different tasks must not share one snapshot
// key: each save must bind to its own task even with identical (empty) items.
const emptyStemA = 'TASK_3_empty_manual_a';
const emptyStemB = 'TASK_4_empty_manual_b';
for (const [emptyStem, title] of [[emptyStemA, 'Empty A'], [emptyStemB, 'Empty B']]) {
  fs.writeFileSync(path.join(tasks, 'todo', emptyStem + '.md'),
    '# ' + title + '\n\n## Acceptance\n\n- automated only\n');
}
const indexValue = JSON.parse(fs.readFileSync(path.join(tasks, 'INDEX.json'), 'utf8'));
indexValue.todo.push(
  { ...indexValue.todo[0], stem: emptyStemA, title: 'Empty A' },
  { ...indexValue.todo[0], stem: emptyStemB, title: 'Empty B' },
);
fs.writeFileSync(path.join(tasks, 'INDEX.json'), JSON.stringify(indexValue, null, 2) + '\n');
const emptyA = validation.checklist(emptyStemA);
const emptyB = validation.checklist(emptyStemB);
assert.equal(emptyA.items.length, 0);
assert.equal(emptyB.items.length, 0);
assert.notEqual(emptyA.validationRevision, emptyB.validationRevision,
  'empty checklists of different tasks must not collide on one snapshot key');
const emptySaved = await validation.save({
  taskStem: emptyStemA,
  expectedTaskSourceRevision: emptyA.taskSourceRevision,
  sessionId,
  expectedSessionRevision: session.sessionRevision,
  validationRevision: emptyA.validationRevision,
  items: [],
  acknowledgeStaleTask: false,
  idempotencyKey: 'validation-fixture-empty-checklist',
}, {
  ...context,
  resolveSession() { return { ok: true, session: { ...session, taskStem: emptyStemA } }; },
});
assert.equal(emptySaved.status, 201);
assert.equal(emptySaved.receipt.overall, 'partial',
  'an empty checklist must never publish as a passed validation');

fs.rmSync(root, { recursive: true, force: true });
