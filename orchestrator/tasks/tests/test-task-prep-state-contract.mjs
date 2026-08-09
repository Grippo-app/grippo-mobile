#!/usr/bin/env node

import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const ROOT = join(HERE, '..', '..', '..');
const SKILL = join(ROOT, 'orchestrator', 'skills', 'task-prep', 'SKILL.md');
const FLOW = join(ROOT, 'orchestrator', 'skills', 'task-prep', 'references', 'prep-flow.md');
const FIGMA = join(ROOT, 'orchestrator', 'skills', 'task-prep', 'references', 'figma-split.md');
const TRANSITION = join(ROOT, 'orchestrator', 'tasks', 'transition-task-state.mjs');
const DROP = join(ROOT, 'orchestrator', 'skills', 'task-orchestrator', 'references', 'task-drop.md');
const RUN_LOOP = join(ROOT, 'orchestrator', 'skills', 'task-orchestrator', 'references', 'run-loop.md');
const DESCOPE = join(ROOT, 'orchestrator', 'figma', 'scripts', 'descope-task.mjs');
const FIXTURE = join(ROOT, 'orchestrator', 'figma', 'scripts', 'seed-evidence-fixture.mjs');

const docs = new Map([
  ['task-prep/SKILL.md', readFileSync(SKILL, 'utf8')],
  ['task-prep/references/prep-flow.md', readFileSync(FLOW, 'utf8')],
  ['task-prep/references/figma-split.md', readFileSync(FIGMA, 'utf8')],
  ['tasks/transition-task-state.mjs', readFileSync(TRANSITION, 'utf8')],
  ['task-orchestrator/references/task-drop.md', readFileSync(DROP, 'utf8')],
  ['task-orchestrator/references/run-loop.md', readFileSync(RUN_LOOP, 'utf8')],
  ['figma/scripts/descope-task.mjs', readFileSync(DESCOPE, 'utf8')],
  ['figma/scripts/seed-evidence-fixture.mjs', readFileSync(FIXTURE, 'utf8')],
]);

let checks = 0;
function check(label, fn) {
  fn();
  checks++;
  process.stdout.write(`PASS ${label}\n`);
}

check('task-prep lock lifecycle uses the canonical helper and exact receipt tuple', () => {
  const flow = docs.get('task-prep/references/prep-flow.md');
  for (const operation of ['acquire', 'verify', 'release']) {
    assert.match(flow, new RegExp(`task-lock\\.mjs ${operation}\\b`), `missing canonical ${operation}`);
  }
  for (const field of ['--stem', '--stage', '--run-id', '--session-id', '--expected-hash', '--expected-state', '--source-revision']) {
    assert.match(flow, new RegExp(field.replaceAll('-', '\\-')), `missing exact lock identity flag ${field}`);
  }
  for (const receiptField of ['stem', 'stage', 'runId', 'sessionId', 'lockHash', 'startedAt', 'owner']) {
    assert.match(flow, new RegExp(`\\b${receiptField}\\b`), `missing retained receipt field ${receiptField}`);
  }
});

check('site, standby, and direct prep authority never self-conflict or steal lease release', () => {
  const flow = docs.get('task-prep/references/prep-flow.md');
  assert.match(flow, /writer-lease\.mjs verify --guard-finalization/);
  for (const field of ['--lease-id', '--token', '--session-id', '--stem']) {
    assert.match(flow, new RegExp(field.replaceAll('-', '\\-')));
  }
  assert.match(flow, /Do not acquire a second lease/i);
  assert.match(flow, /may[\s\S]{0,80}renew that exact generation/i);
  assert.match(flow, /writer-lease\.mjs renew --lease-id <id> --token\s+<token> --ttl-ms 3600000/i);
  assert.match(flow, /must immediately repeat the guarded exact\s+verification/i);
  assert.match(flow, /must never release it/i);
  assert.match(flow, /--owner-kind "<site\|direct>"/);
  assert.match(flow, /uses `--owner-kind\s+standby`/i);
  assert.match(flow, /--writer-lease-id "<standby receipt\.leaseId>"/);
  assert.match(flow, /--writer-lease-token "<standby receipt\.token>"/);
  assert.match(flow, /before and after lock publication/i);
  assert.match(flow, /caller-supplied standby lease[\s\S]{0,240}--lease-id\s+<id> --lease-token <token>/i);
});

check('task-prep docs contain no raw lock writer, overwrite, delete, or age-reaper command', () => {
  const combined = `${docs.get('task-prep/SKILL.md')}\n${docs.get('task-prep/references/prep-flow.md')}`;
  const forbidden = [
    /mkdir\s+-p\s+orchestrator\/\.cache\/tasks\/locks/,
    /mktemp\s+orchestrator\/\.cache\/tasks\/locks/,
    /printf[^\n]*["']stage["'][^\n]*task-prep/,
    /^\s*mv\s+[^\n]*orchestrator\/\.cache\/tasks\/locks/m,
    /^\s*rm\s+-f\s+orchestrator\/\.cache\/tasks\/locks/m,
    /^\s*find\s+orchestrator\/\.cache\/tasks\/locks/m,
  ];
  for (const pattern of forbidden) assert.doesNotMatch(combined, pattern);
});

check('durable ask/promote and INDEX publication belong only to the transition helper', () => {
  const skill = docs.get('task-prep/SKILL.md');
  const flow = docs.get('task-prep/references/prep-flow.md');
  for (const operation of ['ask', 'promote']) {
    assert.match(flow, new RegExp(`transition-task-state\\.mjs ${operation}\\b`));
  }
  assert.match(skill, /never write the durable sidecar directly/i);
  assert.match(flow, /helper atomically publishes todo, detaches backlog\/pending sources/i);
  assert.match(flow, /Running `regen-index\.py`, `rm`, `mv`, or a direct Write[\s\S]{0,200}contract violation/i);
  assert.doesNotMatch(flow, /^\s*(?:python3\s+)?orchestrator\/tasks\/regen-index\.py\b/m);
});

check('task-prep preserves composite API package provenance', () => {
  const skill = docs.get('task-prep/SKILL.md');
  const flow = docs.get('task-prep/references/prep-flow.md');
  for (const document of [skill, flow]) {
    assert.match(document, /Source Type [`]?api-work-package[`]?/);
    assert.match(
      document,
      /complete canonical\s+`## API Work Package` section byte-for-byte/,
    );
  }
});

check('Board Prepare is action-scoped, non-interactive, and mechanically cannot publish questions', () => {
  const skill = docs.get('task-prep/SKILL.md');
  const flow = docs.get('task-prep/references/prep-flow.md');
  const figma = docs.get('task-prep/references/figma-split.md');
  const transition = docs.get('tasks/transition-task-state.mjs');
  for (const document of [skill, flow, figma]) {
    assert.match(document, /BOARD PREP POLICY: NO QUESTIONS\./);
    assert.match(document, /project\/global setting|does not\s+change direct task-prep/i);
  }
  assert.match(skill, /Never emit `needs_action`, invoke the `ask` transition, or publish/);
  assert.match(flow, /Reversible ambiguity[\s\S]{0,120}safest conservative default/i);
  assert.match(flow, /typed actionable[\s\S]{0,80}never a request for user input/i);
  assert.match(figma, /typed blockers, never pending questions/i);
  assert.match(transition, /ORCHESTRATOR_TASK_PREP_NO_QUESTIONS/);
  assert.match(transition, /authority\.key === 'standby:prep'/);
  assert.match(transition, /TASK_PREP_QUESTIONS_DISABLED/);
});

check('Figma child split preserves absent-to-backlog-to-todo canonical ownership', () => {
  const figma = docs.get('task-prep/references/figma-split.md');
  assert.match(figma, /create-backlog\.py/);
  assert.match(figma, /CREATE_BACKLOG_PARENT_WRITER_SESSION_ID/);
  assert.match(figma, /global `task:create-backlog` publication guard/);
  assert.match(figma, /transition-task-state\.mjs promote/);
  assert.match(figma, /task-lock\.mjs acquire --stem <child> --stage task-prep/);
  assert.match(figma, /--authority-stem <parent-stem>/);
  assert.match(figma, /## Origin` is exactly `- split from <parent-stem>`/);
  assert.match(figma, /task-lock\.mjs release --stem <child>/);
  assert.match(figma, /--expected-state <promotion\.state> --source-revision <promotion\.sourceRevision>/);
  assert.match(figma, /recover-release/);
  assert.match(figma, /absent→backlog→todo/);
  assert.match(figma, /there is no `absent→todo` exception/i);
});

check('Figma de-scope and evidence fixtures cannot bypass canonical task writers', () => {
  const descope = docs.get('figma/scripts/descope-task.mjs');
  const fixture = docs.get('figma/scripts/seed-evidence-fixture.mjs');
  assert.match(descope, /transition-task-state\.mjs/);
  assert.match(descope, /'edit', '--stem', stem/);
  assert.match(descope, /'--input', '-'/);
  assert.match(descope, /input: newBody/);
  assert.doesNotMatch(descope, /writeFileSync\(taskFile|renameSync\([^\n]*taskFile|unlinkSync\(taskFile/);
  assert.doesNotMatch(descope, /residual citation[^\n]*by hand/i);
  assert.match(fixture, /unknown flag/);
  assert.doesNotMatch(fixture, /tasksRoot|regen-index\.py|tasks\/backlog|tasks\/todo/);
});

check('drop and reopen docs route durable effects through the transition helper', () => {
  const drop = docs.get('task-orchestrator/references/task-drop.md');
  const runLoop = docs.get('task-orchestrator/references/run-loop.md');
  assert.match(drop, /transition-task-state\.mjs inspect-drop/);
  assert.match(drop, /transition-task-state\.mjs drop/);
  assert.match(runLoop, /transition-task-state\.mjs reopen/);
  assert.doesNotMatch(drop, /^\s*(?:git\s+)?rm\b/m);
  assert.doesNotMatch(runLoop, /^\s*mv\s+[^\n]*orchestrator\/tasks\/(?:done|todo)/m);
});

check('orchestrator run-loop reuses exact caller-owned standby authority', () => {
  const runLoop = docs.get('task-orchestrator/references/run-loop.md');
  assert.match(runLoop, /writer-lease\.mjs verify --guard-finalization/);
  for (const field of ['--lease-id', '--token', '--session-id', '--stem']) {
    assert.match(runLoop, new RegExp(field.replaceAll('-', '\\-')));
  }
  assert.match(runLoop, /--owner-kind standby/);
  assert.match(runLoop, /--writer-lease-id "<caller receipt leaseId>"/);
  assert.match(runLoop, /--writer-lease-token "<caller receipt token>"/);
  assert.match(runLoop, /before and\s+after publication/i);
  assert.match(runLoop, /Do not acquire a second lease/i);
  assert.match(runLoop, /Never release a caller-owned standby lease/i);
});

process.stdout.write(`task-prep state contract: ${checks} checks passed\n`);
