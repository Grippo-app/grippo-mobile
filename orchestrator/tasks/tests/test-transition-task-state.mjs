#!/usr/bin/env node

import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { spawn, spawnSync } from 'node:child_process'
import {
  existsSync, linkSync, lstatSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync,
  renameSync, symlinkSync, truncateSync, unlinkSync, writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const TASKS_DIR = join(HERE, '..')
const CLI = join(TASKS_DIR, 'transition-task-state.mjs')
const FS_BOUNDARY = join(TASKS_DIR, 'finalize-lock.py')
const REGEN = join(TASKS_DIR, 'regen-index.py')
const OUTCOME_SHAPE = join(TASKS_DIR, '..', 'contracts', 'outcome-shape.json')
const core = createRequire(import.meta.url)('../task-state-core.cjs')
const writerLeases = createRequire(import.meta.url)('../writer-leases.cjs')
const taskSourceContract = createRequire(import.meta.url)('../task-source-contract.cjs')
const roots = []
const failures = []
let checks = 0

function check(name, fn) {
  checks++
  try {
    fn()
    console.log(`PASS ${name}`)
  } catch (error) {
    failures.push({ name, error })
    console.error(`FAIL ${name}\n${error && error.stack || error}`)
  }
}

async function checkAsync(name, fn) {
  checks++
  try {
    await fn()
    console.log(`PASS ${name}`)
  } catch (error) {
    failures.push({ name, error })
    console.error(`FAIL ${name}\n${error && error.stack || error}`)
  }
}

function spawnBoundary(request, env = {}) {
  const child = spawn(process.env.PYTHON || 'python3', [FS_BOUNDARY, 'fs-op'], {
    env: { ...process.env, ...env },
    stdio: ['pipe', 'pipe', 'pipe'],
  })
  let stdout = ''
  let stderr = ''
  let settled = false
  child.stdout.on('data', (chunk) => { stdout += chunk })
  child.stderr.on('data', (chunk) => { stderr += chunk })
  child.stdin.end(JSON.stringify(request))
  const done = new Promise((resolve, reject) => {
    child.on('error', reject)
    child.on('close', (status, signal) => {
      settled = true
      resolve({ status, signal, stdout, stderr })
    })
  })
  return { done, settled: () => settled }
}

function waitUntil(predicate, message, timeoutMs = 15000) {
  return new Promise((resolve, reject) => {
    const started = Date.now()
    const timer = setInterval(() => {
      if (predicate()) {
        clearInterval(timer)
        resolve()
      } else if (Date.now() - started >= timeoutMs) {
        clearInterval(timer)
        reject(new Error(message))
      }
    }, 5)
  })
}

function emptyIndex() {
  return {
    version: 2,
    generatedAt: '1970-01-01T00:00:00Z',
    backlog: [], pending: [], todo: [], done: [],
  }
}

function makeProject() {
  const root = mkdtempSync(join(tmpdir(), 'transition-task-state-'))
  roots.push(root)
  const tasks = join(root, 'orchestrator', 'tasks')
  for (const column of core.COLUMNS) mkdirSync(join(tasks, column), { recursive: true })
  writeFileSync(join(tasks, 'INDEX.json'), JSON.stringify(emptyIndex(), null, 2) + '\n')
  return { root, tasks }
}

function numberOf(stem) {
  const match = /^TASK_([1-9][0-9]*)_/.exec(stem)
  assert.ok(match, `invalid fixture stem ${stem}`)
  return Number(match[1])
}

function backlogDoc(stem, title = 'Captured request') {
  return `# TASK ${numberOf(stem)} — ${title}\n\n${sourceBlock(stem)}\n\nKeep the source intact until promotion.\n`
}

function sourceBlock(stem) {
  return [
    '## Source', '', '- Kind: manual', '- Type: manual', '- Ref: ' + stem,
    '- Fingerprint: sha256:' + createHash('sha256').update('source\0' + stem).digest('hex')
  ].join('\n')
}

function splitBacklogDoc(stem, parent, title = 'Split child') {
  return `${backlogDoc(stem, title).trimEnd()}\n\n## Origin\n\n- split from ${parent}\n`
}

function pendingDoc(stem, {
  round = 1, question = 1, gapCount = 1,
  prevGapCount = round > 1 ? 1 : null,
  createdAt = '2026-07-13T08:00:00Z',
  updatedAt = `2026-07-13T08:0${Math.min(round, 9)}:00Z`,
  answer = '',
} = {}) {
  return [
    '---',
    `forTask: ${stem}`,
    `createdAt: ${createdAt}`,
    `updatedAt: ${updatedAt}`,
    `round: ${round}`,
    `gapCount: ${gapCount}`,
    ...(prevGapCount === null ? [] : [`prevGapCount: ${prevGapCount}`]),
    '---',
    '',
    `## Q${question} — Which behavior is required?`,
    '',
    '**Type**: choice',
    '**Options**: strict, compatible',
    '',
    '### Answer',
    '',
    answer,
  ].join('\n')
}

function pendingTwoQuestionDoc(stem, { answer1 = '', answer2 = '' } = {}) {
  return [
    '---',
    `forTask: ${stem}`,
    'createdAt: 2026-07-13T08:00:00Z',
    'updatedAt: 2026-07-13T08:01:00Z',
    'round: 1',
    'gapCount: 2',
    '---',
    '',
    '## Q1 — Which behavior is required?',
    '',
    '**Type**: choice',
    '**Options**: strict, compatible',
    '',
    '### Answer',
    '',
    answer1,
    '## Q2 — Which rollout is required?',
    '',
    '**Type**: text',
    '',
    '### Answer',
    '',
    answer2,
  ].join('\n')
}

function todoDoc(stem, { dependency = null, title = 'Runnable transition' } = {}) {
  return [
    `# TASK ${numberOf(stem)} — ${title}`,
    '',
    sourceBlock(stem),
    '',
    '## Goal',
    '',
    'Implement the requested transition safely.',
    '',
    '## Inputs',
    '',
    '- Current repository contracts.',
    ...(dependency ? ['', '## Depends on (optional)', '', `- ${dependency}`] : []),
    '',
    '## Acceptance',
    '',
    '### Automated',
    '',
    '- Run `node test/transition-contract.mjs`.',
    '',
    '### Manual',
    '',
    '- Inspect the resulting task card.',
    '',
    '## Out of scope',
    '',
    '- Unrelated workflow changes.',
    '',
  ].join('\n')
}

// In-body question rail: `## Questions` > `### Q<N> — …` > `#### Answer`,
// one heading level deeper than the pending sidecar because the section lives
// inside a task body that already owns every H2.
function todoQuestion(id, {
  title = `Decision ${id}`,
  type = 'choice',
  options = 'a, b',
  withOptions = type !== 'text',
  answer = '',
  answerHeading = true,
} = {}) {
  return [
    `### Q${id} — ${title}`,
    '',
    '- (a) **First** — cheapest.',
    '- (b) **Second** — safest.',
    '',
    '**Recommended**: (a) — default for this shape.',
    '',
    `**Type**: ${type}`,
    ...(withOptions ? [`**Options**: ${options}`] : []),
    '',
    ...(answerHeading ? ['#### Answer', '', answer, ''] : ['', '']),
  ].join('\n')
}

function todoWithQuestions(stem, questions, options = {}) {
  return [
    todoDoc(stem, options).trimEnd(),
    '',
    '## Questions',
    '',
    ...questions,
  ].join('\n')
}

function doneDoc(stem) {
  return [
    todoDoc(stem, { title: 'Completed transition' }).trimEnd(),
    '',
    '---',
    '',
    '## Outcome',
    '',
    '**Status**: completed',
    '**Completed at**: 2026-07-13T09:00:00Z',
    '**Reviewer**: codex',
    '**Review iterations**: 2',
    '',
    '### Build gates',
    '',
    '- `node test/transition-contract.mjs` — pass',
    '',
    '### Runtime verify',
    '',
    '- Gate: skipped (no runtime-observable change)',
    '- Result: n/a — no runtime change',
    '',
    '### Acceptance trace',
    '',
    '- `test/transition-contract.mjs` — verified — Passed.',
    '',
    '### Caveats',
    '',
    '- none',
    '',
    '### Follow-ups',
    '',
    '- none',
    '',
    '### Files touched',
    '',
    '- `orchestrator/tasks/transition-task-state.mjs` — modified',
    '',
  ].join('\n')
}

function artifact(project, column, stem) {
  return join(project.tasks, column, stem + (column === 'pending' ? '.questions.md' : '.md'))
}

function writeArtifact(project, column, stem, contents) {
  writeFileSync(artifact(project, column, stem), contents)
}

function validate(project, options = {}) {
  return core.validateTaskState({
    repoRoot: project.root,
    tasksDir: project.tasks,
    outcomeShapePath: OUTCOME_SHAPE,
    includeRuntime: false,
    ...options,
  })
}

function envFor(project) {
  return {
    ...process.env,
    ORCHESTRATOR_PROJECT_ROOT: project.root,
    ORCHESTRATOR_TASKS_DIR: project.tasks,
    ORCHESTRATOR_OUTCOME_SHAPE_PATH: OUTCOME_SHAPE,
    ORCHESTRATOR_TRANSITIONS_DIR: join(project.root, 'orchestrator', '.cache', 'tasks', 'transitions'),
    ORCHESTRATOR_FINALIZATIONS_DIR: join(project.root, 'orchestrator', '.cache', 'tasks', 'finalizations'),
    ORCHESTRATOR_WRITER_LEASES_DIR: join(project.root, 'orchestrator', '.cache', 'tasks', 'finalizations', '.writers'),
    TASK_TRANSITION_TEST_UNLEASED: '1',
    PYTHONDONTWRITEBYTECODE: '1',
    PYTHON: process.env.PYTHON || 'python3',
    NODE: process.execPath,
  }
}

function run(project, args, extraEnv = {}, input = undefined) {
  const normalizedArgs = args.slice()
  const inputIndex = normalizedArgs.indexOf('--input')
  if (inputIndex >= 0 && normalizedArgs[inputIndex + 1] && normalizedArgs[inputIndex + 1].stdinProposal === true) {
    assert.equal(input, undefined, 'proposal bytes must have one stdin owner')
    input = normalizedArgs[inputIndex + 1].bytes
    normalizedArgs[inputIndex + 1] = '-'
  }
  return spawnSync(process.execPath, [CLI, ...normalizedArgs], {
    cwd: project.root,
    env: { ...envFor(project), ...extraEnv },
    encoding: 'utf8',
    maxBuffer: 8 * 1024 * 1024,
    timeout: 60000,
    input,
  })
}

function runAsync(project, args, extraEnv = {}, input = undefined) {
  const child = spawn(process.execPath, [CLI, ...args], {
    cwd: project.root,
    env: { ...envFor(project), ...extraEnv },
    stdio: ['pipe', 'pipe', 'pipe'],
  })
  let stdout = '', stderr = '', settled = false
  child.stdout.on('data', (chunk) => { stdout += String(chunk) })
  child.stderr.on('data', (chunk) => { stderr += String(chunk) })
  if (input === undefined) child.stdin.end()
  else child.stdin.end(input)
  const done = new Promise((resolve, reject) => {
    child.on('error', reject)
    child.on('close', (status, signal) => {
      settled = true
      resolve({ status, signal, stdout, stderr })
    })
  })
  return { child, done, settled: () => settled }
}

function runRegen(project, checkOnly = false) {
  const result = spawnSync(process.env.PYTHON || 'python3', [REGEN, ...(checkOnly ? ['--check'] : [])], {
    cwd: project.root,
    env: envFor(project),
    encoding: 'utf8',
    maxBuffer: 8 * 1024 * 1024,
    timeout: 60000,
  })
  assert.equal(result.status, 0, result.stderr + result.stdout)
  return result
}

function parseSuccess(result, { requireOk = true } = {}) {
  assert.equal(result.status, 0, result.stderr + result.stdout)
  assert.notEqual(result.stdout.trim(), '')
  const value = JSON.parse(result.stdout)
  if (requireOk) assert.equal(value.ok, true)
  return value
}

function taskStateEvents(stderr) {
  return String(stderr || '').split(/\r?\n/)
    .filter((line) => line.startsWith('[task-state] '))
    .map((line) => JSON.parse(line.slice('[task-state] '.length)))
}

function failureEnvelope(stderr) {
  const json = String(stderr || '').split(/\r?\n/)
    .filter((line) => !line.startsWith('[task-state] ')).join('\n').trim()
  assert.notEqual(json, '', 'failed transition must emit its public error envelope')
  return JSON.parse(json)
}

function parseFailure(result, status, code) {
  assert.equal(result.status, status, result.stderr + result.stdout)
  assert.equal(result.stdout, '', 'failed transition must not publish success JSON')
  const value = failureEnvelope(result.stderr)
  assert.equal(value.ok, false)
  assert.equal(value.code, code)
  return value
}

function revision(project, stem, state) {
  const result = validate(project, { stem, expect: state, checkIndex: true })
  assert.equal(result.ok, true, JSON.stringify(result.findings, null, 2))
  return result.sourceRevision
}

function assertFresh(project, stem, state) {
  const result = validate(project, { stem, expect: state, checkIndex: true })
  assert.equal(result.ok, true, JSON.stringify(result.findings, null, 2))
  assert.equal(result.indexStatus, 'fresh')
  runRegen(project, true)
  return result
}

function assertNoRecoveryResidue(project) {
  const transitions = join(project.root, 'orchestrator', '.cache', 'tasks', 'transitions')
  if (!existsSync(transitions)) return
  const markerNames = readdirSync(transitions).filter((name) => name.endsWith('.json'))
  assert.deepEqual(markerNames, [], 'completed/rolled-back transition left a durable marker')
  const privateRoot = join(transitions, '.private')
  if (existsSync(privateRoot)) assert.deepEqual(readdirSync(privateRoot), [], 'transition left detached artifacts')
}

function writeInput(project, name, contents) {
  assert.ok(project && name)
  return { stdinProposal: true, bytes: Buffer.isBuffer(contents) ? contents : Buffer.from(contents) }
}

function sha256Hex(bytes) {
  return createHash('sha256').update(bytes).digest('hex')
}

function fileProof(file) {
  const bytes = readFileSync(file)
  const stat = lstatSync(file, { bigint: true })
  return {
    path: file,
    hash: core.sha256(bytes),
    dev: stat.dev.toString(),
    ino: stat.ino.toString(),
    kind: 'file',
    mode: Number(stat.mode),
    size: Number(stat.size),
    mtimeNs: stat.mtimeNs.toString(),
    ctimeNs: stat.ctimeNs.toString(),
  }
}

function metadataProof(file) {
  const stat = lstatSync(file, { bigint: true })
  return {
    path: file,
    hash: null,
    dev: stat.dev.toString(),
    ino: stat.ino.toString(),
    kind: 'file',
    mode: Number(stat.mode),
    size: Number(stat.size),
    mtimeNs: stat.mtimeNs.toString(),
    ctimeNs: stat.ctimeNs.toString(),
  }
}

function markerBase({ transactionId, operation, stem, sourceState, sourceRevision, intendedHash, sourceArtifacts }) {
  return {
    version: 1,
    transactionId,
    operation,
    stem,
    authorityStem: stem,
    taskLock: null,
    sourceState,
    sourceRevision,
    sourceArtifacts,
    intendedHash,
    createdAt: '2026-07-13T09:00:00Z',
    updatedAt: '2026-07-13T09:00:01Z',
    phase: 'prepared',
    detached: {},
    runtimeDetached: {},
    targetArtifact: null,
    history: null,
  }
}

function canonicalTaskLock(project, stem, stage, sessionId = 'ws-fixture-session-00000001') {
  const locks = join(project.root, 'orchestrator', '.cache', 'tasks', 'locks')
  mkdirSync(locks, { recursive: true })
  const startedAt = '2026-07-13T09:00:00.000Z'
  const record = {
    version: 1,
    stem,
    stage,
    runId: 'fixture-run-00000001',
    sessionId,
    startedAt,
    owner: {
      kind: 'agent', id: 'transition-fixture', pid: process.pid,
      processStartId: null, hostname: 'fixture-host', startedAt,
    },
  }
  const file = join(locks, stem + '.json')
  writeFileSync(file, JSON.stringify(record, null, 2) + '\n')
  return { file, sessionId }
}

try {
  check('backlog to pending preserves backlog and publishes a fresh INDEX', () => {
    const project = makeProject()
    const stem = 'TASK_1_backlog_to_pending'
    const backlog = backlogDoc(stem)
    const pending = pendingDoc(stem)
    writeArtifact(project, 'backlog', stem, backlog)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'backlog')
    const executed = run(project, [
      'ask', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
    ], {}, Buffer.from(pending))
    const value = parseSuccess(executed)
    assert.equal(value.operation, 'ask')
    assert.equal(value.state, 'pending')
    const events = taskStateEvents(executed.stderr)
    assert.ok(events.length >= 10, 'transition and nested INDEX validations must all be observable')
    for (const event of events) {
      assert.equal(event.version, 1)
      assert.equal(event.event, 'task-state-validation')
      assert.ok(['task-prep', 'server'].includes(event.caller))
      assert.equal(event.scope, event.caller === 'server' ? 'all' : stem)
      assert.ok(['valid', 'invalid'].includes(event.result))
      assert.ok(Array.isArray(event.findings))
      assert.equal(JSON.stringify(event).includes('Keep the source intact'), false)
      assert.equal(JSON.stringify(event).includes('Which behavior is required'), false)
    }
    assert.equal(executed.stderr.includes('task-validation-event'), false,
      'unversioned validation telemetry must not be emitted')
    assert.equal(readFileSync(artifact(project, 'backlog', stem), 'utf8'), backlog)
    assert.equal(readFileSync(artifact(project, 'pending', stem), 'utf8'), pending)
    assertFresh(project, stem, 'pending')
    assertNoRecoveryResidue(project)
  })

  check('Board Prepare refuses pending publication and preserves the backlog generation', () => {
    const project = makeProject()
    const stem = 'TASK_68_board_prepare_no_questions'
    const backlog = backlogDoc(stem)
    writeArtifact(project, 'backlog', stem, backlog)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'backlog')
    parseFailure(run(project, [
      'ask', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
    ], {
      ORCHESTRATOR_TASK_PREP_NO_QUESTIONS: '1',
    }, Buffer.from(pendingDoc(stem))), 1, 'TASK_PREP_QUESTIONS_DISABLED')
    assert.equal(readFileSync(artifact(project, 'backlog', stem), 'utf8'), backlog)
    assert.equal(existsSync(artifact(project, 'pending', stem)), false)
    assertFresh(project, stem, 'backlog')
    assertNoRecoveryResidue(project)
  })

  check('standby prep authority also refuses pending publication without a global setting', () => {
    const project = makeProject()
    const stem = 'TASK_69_standby_prepare_no_questions'
    const backlog = backlogDoc(stem)
    const writers = join(project.root, 'orchestrator', '.cache', 'tasks', 'finalizations', '.writers')
    const finalizations = join(project.root, 'orchestrator', '.cache', 'tasks', 'finalizations')
    const sessionId = 'ws-fixture-standby-prep-00000069'
    mkdirSync(writers, { recursive: true })
    writeArtifact(project, 'backlog', stem, backlog)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'backlog')
    const lease = writerLeases.acquire(writers, {
      kind: 'task-session', stem, key: 'standby:prep', sessionId,
      ownerPid: process.pid, childPid: process.pid, ttlMs: 60_000, rootDir: project.root,
    })
    canonicalTaskLock(project, stem, 'task-prep', sessionId)
    try {
      parseFailure(run(project, [
        'ask', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
        '--lease-id', lease.leaseId, '--lease-token', lease.token,
      ], {
        TASK_TRANSITION_TEST_UNLEASED: '0',
        FINALIZE_PROJECT_ROOT: project.root,
        FINALIZE_STATE_DIR: finalizations,
      }, Buffer.from(pendingDoc(stem))), 1, 'TASK_PREP_QUESTIONS_DISABLED')
      assert.equal(readFileSync(artifact(project, 'backlog', stem), 'utf8'), backlog)
      assert.equal(existsSync(artifact(project, 'pending', stem)), false)
      assertFresh(project, stem, 'backlog')
      assertNoRecoveryResidue(project)
    } finally {
      writerLeases.release(lease)
    }
  })

  check('pending to pending replaces only the sidecar and advances its derived metadata', () => {
    const project = makeProject()
    const stem = 'TASK_2_pending_round'
    const backlog = backlogDoc(stem)
    const firstPending = pendingDoc(stem)
    const nextPending = pendingDoc(stem, { round: 2, question: 2, gapCount: 2 })
    writeArtifact(project, 'backlog', stem, backlog)
    writeArtifact(project, 'pending', stem, firstPending)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'pending')
    const value = parseSuccess(run(project, [
      'ask', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
    ], {}, Buffer.from(nextPending)))
    assert.equal(value.state, 'pending')
    assert.equal(readFileSync(artifact(project, 'backlog', stem), 'utf8'), backlog)
    assert.equal(readFileSync(artifact(project, 'pending', stem), 'utf8'), nextPending)
    const fresh = assertFresh(project, stem, 'pending')
    const row = core.deriveIndex(fresh._model, '2026-07-13T10:00:00Z').pending[0]
    assert.equal(row.round, 2)
    assert.equal(row.questionsCount, 1)
    assertNoRecoveryResidue(project)
  })

  check('inline answer persistence changes only Answer bodies within the same round', () => {
    const project = makeProject()
    const stem = 'TASK_70_pending_answer_persistence'
    const backlog = backlogDoc(stem)
    const pending = pendingDoc(stem)
    const answered = pendingDoc(stem, { answer: 'strict' })
    writeArtifact(project, 'backlog', stem, backlog)
    writeArtifact(project, 'pending', stem, pending)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'pending')
    const value = parseSuccess(run(project, [
      'persist-answers', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
    ], {}, Buffer.from(answered)))
    assert.equal(value.operation, 'persist-answers')
    assert.equal(value.state, 'pending')
    assert.equal(readFileSync(artifact(project, 'backlog', stem), 'utf8'), backlog)
    assert.equal(readFileSync(artifact(project, 'pending', stem), 'utf8'), answered)
    assertFresh(project, stem, 'pending')
    assertNoRecoveryResidue(project)
  })

  check('publish-questions appends an in-body Questions section to a running task', () => {
    const project = makeProject()
    const stem = 'TASK_120_todo_questions_publish'
    const before = todoDoc(stem)
    writeArtifact(project, 'todo', stem, before)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'todo')
    const after = todoWithQuestions(stem, [todoQuestion(1)])
    const value = parseSuccess(run(project, [
      'publish-questions', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
    ], {}, Buffer.from(after)))
    assert.equal(value.operation, 'publish-questions')
    assert.equal(value.state, 'todo')
    assert.equal(readFileSync(artifact(project, 'todo', stem), 'utf8'), after)
    const parsed = core.parseTaskQuestions(after)
    assert.equal(core.taskQuestionsIssue(parsed), null)
    assert.deepEqual(parsed.questions.map((question) => question.id), [1])
    assertFresh(project, stem, 'todo')
    assertNoRecoveryResidue(project)
  })

  check('publish-questions appends a later round and keeps earlier blocks intact', () => {
    const project = makeProject()
    const stem = 'TASK_121_todo_questions_second_round'
    const before = todoWithQuestions(stem, [todoQuestion(1, { answer: 'a' })])
    writeArtifact(project, 'todo', stem, before)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'todo')
    const after = todoWithQuestions(stem, [todoQuestion(1, { answer: 'a' }), todoQuestion(4, { type: 'text' })])
    const value = parseSuccess(run(project, [
      'publish-questions', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
    ], {}, Buffer.from(after)))
    assert.equal(value.operation, 'publish-questions')
    const parsed = core.parseTaskQuestions(readFileSync(artifact(project, 'todo', stem), 'utf8'))
    assert.deepEqual(parsed.questions.map((question) => question.id), [1, 4])
    assert.equal(parsed.questions[0].answer, 'a')
    assertFresh(project, stem, 'todo')
    assertNoRecoveryResidue(project)
  })

  check('publish-questions refuses every write that is not an append of new blocks', () => {
    const project = makeProject()
    const stem = 'TASK_122_todo_questions_append_only'
    const before = todoWithQuestions(stem, [todoQuestion(1)])
    writeArtifact(project, 'todo', stem, before)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'todo')
    const rejected = [
      before,
      todoWithQuestions(stem, [todoQuestion(1)]).replace('Implement the requested transition safely.', 'Do something else.'),
      todoWithQuestions(stem, [todoQuestion(1, { title: 'Rewritten question' }), todoQuestion(2)]),
      todoWithQuestions(stem, [todoQuestion(1), todoQuestion(1)]),
      todoWithQuestions(stem, [todoQuestion(2), todoQuestion(3)]),
      todoWithQuestions(stem, [todoQuestion(1), todoQuestion(2, { type: 'choice', options: 'a, a' })]),
      todoWithQuestions(stem, [todoQuestion(1), todoQuestion(2, { type: 'text', withOptions: true })]),
      todoWithQuestions(stem, [todoQuestion(1), todoQuestion(2, { answerHeading: false })]),
      todoWithQuestions(stem, [todoQuestion(1), todoQuestion(2)]).replace('### Q2 —', '### Question 2 —'),
      todoWithQuestions(stem, [todoQuestion(1)]) + '\n## Questions\n\n### Q9 — Duplicate section\n\n**Type**: text\n\n#### Answer\n\n',
    ]
    for (const proposal of rejected) {
      const failure = parseFailure(run(project, [
        'publish-questions', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
      ], {}, Buffer.from(proposal)), 1, 'TASK_QUESTIONS_WRITE_INVALID')
      assert.equal(failure.ok, false)
      assert.equal(readFileSync(artifact(project, 'todo', stem), 'utf8'), before)
    }
    assertNoRecoveryResidue(project)
  })

  check('publish-questions cannot touch an existing block, its answer, or its option bullets', () => {
    const project = makeProject()
    const stem = 'TASK_127_todo_questions_last_block'
    const before = todoWithQuestions(stem, [todoQuestion(1, { answer: '(a) Postgres. Never Redis.' })])
    writeArtifact(project, 'todo', stem, before)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'todo')
    // Every one of these keeps question identity intact and only rewrites bytes
    // inside the LAST existing block — the region a prefix comparison misses.
    const tampered = [
      before.replace('(a) Postgres. Never Redis.', '(a) Postgres. CORRECTION: use Redis.'),
      before.replace('(a) Postgres. Never Redis.', ''),
      before.replace('- (a) **First** — cheapest.', '- (a) **Drop the table (owner reconfirmed)** — proceed.'),
      before.replace('**Recommended**: (a) — default for this shape.', '**Recommended**: (b) — default for this shape.'),
    ].map((text) => text.trimEnd() + '\n\n' + todoQuestion(2, { type: 'text' }))
    for (const proposal of tampered) {
      parseFailure(run(project, [
        'publish-questions', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
      ], {}, Buffer.from(proposal)), 1, 'TASK_QUESTIONS_WRITE_INVALID')
      assert.equal(readFileSync(artifact(project, 'todo', stem), 'utf8'), before)
    }
    // The same append without tampering is accepted, so the fence is exact.
    const clean = before.trimEnd() + '\n\n' + todoQuestion(2, { type: 'text' })
    parseSuccess(run(project, [
      'publish-questions', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
    ], {}, Buffer.from(clean)))
    assertFresh(project, stem, 'todo')
    assertNoRecoveryResidue(project)
  })

  check('a newly published question cannot arrive pre-answered', () => {
    const project = makeProject()
    const stem = 'TASK_128_todo_questions_self_answer'
    const before = todoDoc(stem)
    writeArtifact(project, 'todo', stem, before)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'todo')
    parseFailure(run(project, [
      'publish-questions', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
    ], {}, Buffer.from(todoWithQuestions(stem, [todoQuestion(1, { answer: 'b' })]))), 1, 'TASK_QUESTIONS_WRITE_INVALID')
    assert.equal(readFileSync(artifact(project, 'todo', stem), 'utf8'), before)
    assertNoRecoveryResidue(project)
  })

  check('an answer may not leave a CommonMark container open at the end of the body', () => {
    const project = makeProject()
    const stem = 'TASK_129_todo_answer_open_container'
    const before = todoWithQuestions(stem, [todoQuestion(1, { type: 'text' })])
    writeArtifact(project, 'todo', stem, before)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'todo')
    for (const answer of ['Use this:\n\n```kotlin\nval x = 1', 'Note:\n\n<!-- pending', 'See:\n\n~~~\nsample']) {
      const proposal = todoWithQuestions(stem, [todoQuestion(1, { type: 'text', answer })])
      parseFailure(run(project, [
        'persist-task-answers', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
      ], {}, Buffer.from(proposal)), 1, 'TASK_QUESTIONS_WRITE_INVALID')
      assert.equal(readFileSync(artifact(project, 'todo', stem), 'utf8'), before)
    }
    // A closed fence in the same position is legitimate answer content.
    const closed = todoWithQuestions(stem, [todoQuestion(1, { type: 'text', answer: 'Use this:\n\n```kotlin\nval x = 1\n```' })])
    parseSuccess(run(project, [
      'persist-task-answers', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
    ], {}, Buffer.from(closed)))
    assertFresh(project, stem, 'todo')
    assertNoRecoveryResidue(project)
  })

  check('a body cannot vouch for its own container state with a decoy probe heading', () => {
    const project = makeProject()
    const stem = 'TASK_131_todo_questions_probe_decoy'
    // The closure check appends a probe heading. A body that already ends with
    // that literal heading must not be able to shelter an open fence behind it.
    const before = todoWithQuestions(stem, [todoQuestion(1, { type: 'text' })])
    writeArtifact(project, 'todo', stem, before)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'todo')
    const poisoned = todoWithQuestions(stem, [todoQuestion(1, {
      type: 'text',
      answer: 'see below\n\n## OrchestratorStructureProbe\n\n```kotlin\nval x = 1',
    })])
    parseFailure(run(project, [
      'persist-task-answers', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
    ], {}, Buffer.from(poisoned)), 1, 'TASK_QUESTIONS_WRITE_INVALID')
    assert.equal(readFileSync(artifact(project, 'todo', stem), 'utf8'), before)
    assertNoRecoveryResidue(project)
  })

  check('the first published section may only absorb newlines at its insertion point', () => {
    const project = makeProject()
    const stem = 'TASK_130_todo_questions_insertion_point'
    const before = todoDoc(stem)
    writeArtifact(project, 'todo', stem, before)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'todo')
    const padded = before.replace(/\n+$/, ' \t   \n\n') + '\n## Questions\n\n' + todoQuestion(1)
    parseFailure(run(project, [
      'publish-questions', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
    ], {}, Buffer.from(padded)), 1, 'TASK_QUESTIONS_WRITE_INVALID')
    assert.equal(readFileSync(artifact(project, 'todo', stem), 'utf8'), before)
    assertNoRecoveryResidue(project)
  })

  check('publish-questions is refused outside todo', () => {
    const project = makeProject()
    const stem = 'TASK_123_todo_questions_backlog'
    writeArtifact(project, 'backlog', stem, backlogDoc(stem))
    runRegen(project)
    const sourceRevision = revision(project, stem, 'backlog')
    parseFailure(run(project, [
      'publish-questions', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
    ], {}, Buffer.from(todoWithQuestions(stem, [todoQuestion(1)]))), 1, 'TRANSITION_PRECONDITION_FAILED')
    assertNoRecoveryResidue(project)
  })

  check('persist-task-answers changes only in-body Answer bodies', () => {
    const project = makeProject()
    const stem = 'TASK_124_todo_answer_persistence'
    const before = todoWithQuestions(stem, [todoQuestion(1), todoQuestion(2, { type: 'text' })])
    writeArtifact(project, 'todo', stem, before)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'todo')
    const after = todoWithQuestions(stem, [
      todoQuestion(1, { answer: 'a' }),
      todoQuestion(2, { type: 'text', answer: 'ship it' }),
    ])
    const value = parseSuccess(run(project, [
      'persist-task-answers', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
    ], {}, Buffer.from(after)))
    assert.equal(value.operation, 'persist-task-answers')
    assert.equal(value.state, 'todo')
    const parsed = core.parseTaskQuestions(readFileSync(artifact(project, 'todo', stem), 'utf8'))
    assert.deepEqual(parsed.questions.map((question) => question.answer), ['a', 'ship it'])
    assertFresh(project, stem, 'todo')
    assertNoRecoveryResidue(project)
  })

  check('persist-task-answers refuses identity drift, no-ops and outside-section edits', () => {
    const project = makeProject()
    const stem = 'TASK_125_todo_answer_fences'
    const before = todoWithQuestions(stem, [todoQuestion(1)])
    writeArtifact(project, 'todo', stem, before)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'todo')
    const cases = [
      [before, 'TASK_ANSWER_PERSISTENCE_INVALID'],
      [todoWithQuestions(stem, [todoQuestion(1, { options: 'a, b, c', answer: 'a' })]), 'TASK_ANSWER_PERSISTENCE_INVALID'],
      [todoWithQuestions(stem, [todoQuestion(1, { title: 'Rewritten', answer: 'a' })]), 'TASK_ANSWER_PERSISTENCE_INVALID'],
      [todoWithQuestions(stem, [todoQuestion(1, { answer: 'a' }), todoQuestion(2)]), 'TASK_ANSWER_PERSISTENCE_INVALID'],
      [todoWithQuestions(stem, [todoQuestion(1, { answer: 'a' })])
        .replace('Implement the requested transition safely.', 'Rewritten goal.'), 'TASK_ANSWER_PERSISTENCE_INVALID'],
      [todoDoc(stem), 'TASK_QUESTIONS_WRITE_INVALID'],
    ]
    for (const [proposal, code] of cases) {
      parseFailure(run(project, [
        'persist-task-answers', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
      ], {}, Buffer.from(proposal)), 1, code)
      assert.equal(readFileSync(artifact(project, 'todo', stem), 'utf8'), before)
    }
    assertNoRecoveryResidue(project)
  })

  check('in-body questions never leak into the pending rail or plain edit', () => {
    const project = makeProject()
    const stem = 'TASK_126_todo_questions_isolation'
    const before = todoWithQuestions(stem, [todoQuestion(1)])
    writeArtifact(project, 'todo', stem, before)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'todo')
    // `ask`/`persist-answers` own the pending sidecar and must stay refused here.
    parseFailure(run(project, [
      'ask', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
    ], {}, Buffer.from(pendingDoc(stem))), 1, 'TRANSITION_PRECONDITION_FAILED')
    parseFailure(run(project, [
      'persist-answers', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
    ], {}, Buffer.from(pendingDoc(stem))), 1, 'TRANSITION_PRECONDITION_FAILED')
    // Plain `edit` keeps its unrestricted todo-body contract.
    const edited = before.replace('Implement the requested transition safely.', 'Implement it safely.')
    const value = parseSuccess(run(project, [
      'edit', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
    ], {}, Buffer.from(edited)))
    assert.equal(value.operation, 'edit')
    assert.equal(readFileSync(artifact(project, 'todo', stem), 'utf8'), edited)
    assertFresh(project, stem, 'todo')
    assertNoRecoveryResidue(project)
  })

  check('pending question and Answer anchors admit horizontal whitespace only', () => {
    const project = makeProject()
    const stem = 'TASK_76_pending_horizontal_whitespace'
    const pending = pendingDoc(stem)
    writeArtifact(project, 'backlog', stem, backlogDoc(stem))
    writeArtifact(project, 'pending', stem, pending)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'pending')
    for (const proposal of [
      pending.replace('## Q1 —', '##\vQ1 —'),
      pending.replace('### Answer', '###\fAnswer'),
    ]) {
      parseFailure(run(project, [
        'persist-answers', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
      ], {}, Buffer.from(proposal)), 1, 'PENDING_WRITE_CONTRACT_INVALID')
      assert.equal(readFileSync(artifact(project, 'pending', stem), 'utf8'), pending)
    }
    assertFresh(project, stem, 'pending')
    assertNoRecoveryResidue(project)
  })

  check('answer persistence cannot hide real questions with markup spanning Answer bodies', () => {
    for (const [index, answer1, answer2] of [
      [73, '```markdown', '```'],
      [74, '<script>', '</script>'],
    ]) {
      const project = makeProject()
      const stem = `TASK_${index}_pending_answer_structure`
      const pending = pendingTwoQuestionDoc(stem)
      writeArtifact(project, 'backlog', stem, backlogDoc(stem))
      writeArtifact(project, 'pending', stem, pending)
      runRegen(project)
      const sourceRevision = revision(project, stem, 'pending')
      parseFailure(run(project, [
        'persist-answers', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
      ], {}, Buffer.from(pendingTwoQuestionDoc(stem, { answer1, answer2 }))), 1, 'PENDING_ANSWER_PERSISTENCE_INVALID')
      assert.equal(readFileSync(artifact(project, 'pending', stem), 'utf8'), pending)
      assertFresh(project, stem, 'pending')
      assertNoRecoveryResidue(project)
    }
  })

  check('CommonMark-hidden fake question headings remain editable Answer bytes', () => {
    const project = makeProject()
    const stem = 'TASK_75_pending_hidden_answer_markup'
    const pending = pendingDoc(stem, { answer: ['````markdown', '## Q99 — inert example', '### Answer', 'old example', '````'].join('\n') })
    const answered = pending.replace('old example', 'new example')
    writeArtifact(project, 'backlog', stem, backlogDoc(stem))
    writeArtifact(project, 'pending', stem, pending)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'pending')
    const value = parseSuccess(run(project, [
      'persist-answers', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
    ], {}, Buffer.from(answered)))
    assert.equal(value.operation, 'persist-answers')
    assert.equal(readFileSync(artifact(project, 'pending', stem), 'utf8'), answered)
    assertFresh(project, stem, 'pending')
    assertNoRecoveryResidue(project)
  })

  check('pending write intents reject generation rewrites before any mutation', () => {
    const project = makeProject()
    const stem = 'TASK_71_pending_generation_contract'
    const backlog = backlogDoc(stem)
    const pending = pendingDoc(stem)
    writeArtifact(project, 'backlog', stem, backlog)
    writeArtifact(project, 'pending', stem, pending)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'pending')
    const invalidAsks = [
      pendingDoc(stem, { round: 1, answer: 'same-round rewrite' }),
      pendingDoc(stem, { round: 3, prevGapCount: 1 }),
      pendingDoc(stem, { round: 2, prevGapCount: 9 }),
      pendingDoc(stem, { round: 2, prevGapCount: 1, createdAt: '2026-07-13T07:00:00Z' }),
    ]
    for (const proposal of invalidAsks) {
      parseFailure(run(project, [
        'ask', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
      ], {}, Buffer.from(proposal)), 1, 'PENDING_ROUND_GENERATION_INVALID')
      assert.equal(readFileSync(artifact(project, 'pending', stem), 'utf8'), pending)
    }
    const structuralRewrite = pendingDoc(stem, { round: 2, question: 2, prevGapCount: 1, answer: 'strict' })
    parseFailure(run(project, [
      'persist-answers', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
    ], {}, Buffer.from(structuralRewrite)), 1, 'PENDING_ANSWER_PERSISTENCE_INVALID')
    parseFailure(run(project, [
      'persist-answers', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
    ], {}, Buffer.from(pending)), 1, 'PENDING_ANSWER_PERSISTENCE_INVALID')
    assert.equal(readFileSync(artifact(project, 'pending', stem), 'utf8'), pending)
    assertFresh(project, stem, 'pending')
    assertNoRecoveryResidue(project)
  })

  check('initial questions must start at round one without inherited convergence history', () => {
    const project = makeProject()
    const stem = 'TASK_72_initial_pending_generation'
    const backlog = backlogDoc(stem)
    writeArtifact(project, 'backlog', stem, backlog)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'backlog')
    parseFailure(run(project, [
      'ask', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
    ], {}, Buffer.from(pendingDoc(stem, { round: 2, prevGapCount: 1 }))), 1, 'PENDING_WRITE_CONTRACT_INVALID')
    assert.equal(readFileSync(artifact(project, 'backlog', stem), 'utf8'), backlog)
    assert.equal(existsSync(artifact(project, 'pending', stem)), false)
    assertFresh(project, stem, 'backlog')
    assertNoRecoveryResidue(project)
  })

  check('pending to todo removes both source artifacts only after valid promotion', () => {
    const project = makeProject()
    const stem = 'TASK_3_pending_to_todo'
    const backlog = backlogDoc(stem)
    const pending = pendingDoc(stem)
    const todo = todoDoc(stem)
    writeArtifact(project, 'backlog', stem, backlog)
    writeArtifact(project, 'pending', stem, pending)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'pending')
    const value = parseSuccess(run(project, [
      'promote', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
    ], {}, Buffer.from(todo)))
    assert.equal(value.operation, 'promote')
    assert.equal(value.state, 'todo')
    assert.equal(existsSync(artifact(project, 'backlog', stem)), false)
    assert.equal(existsSync(artifact(project, 'pending', stem)), false)
    assert.equal(readFileSync(artifact(project, 'todo', stem), 'utf8'), todo)
    assertFresh(project, stem, 'todo')
    assertNoRecoveryResidue(project)
  })

  check('authorized in-column edit publishes exact todo bytes and a fresh INDEX', () => {
    const project = makeProject()
    const stem = 'TASK_4_todo_edit'
    const original = todoDoc(stem, { title: 'Original todo' })
    const edited = todoDoc(stem, { title: 'De-scoped todo' })
    writeArtifact(project, 'todo', stem, original)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'todo')
    const value = parseSuccess(run(project, [
      'edit', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
    ], {}, Buffer.from(edited)))
    assert.equal(value.operation, 'edit')
    assert.equal(value.state, 'todo')
    assert.equal(readFileSync(artifact(project, 'todo', stem), 'utf8'), edited)
    assertFresh(project, stem, 'todo')
    assertNoRecoveryResidue(project)
  })

  check('invalid in-column edit rolls back exact backlog source bytes', () => {
    const project = makeProject()
    const stem = 'TASK_5_backlog_edit_rollback'
    const original = backlogDoc(stem)
    writeArtifact(project, 'backlog', stem, original)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'backlog')
    const input = writeInput(project, 'invalid-backlog.md', original + '\n## Outcome\n\ninvalid\n')

    parseFailure(run(project, [
      'edit', '--stem', stem, '--input', input, '--source-revision', sourceRevision,
    ]), 1, 'TRANSITION_POSTCONDITION_FAILED')
    assert.equal(readFileSync(artifact(project, 'backlog', stem), 'utf8'), original)
    assertFresh(project, stem, 'backlog')
    assertNoRecoveryResidue(project)
  })

  check('stdin admission rejects file paths, empty, malformed, and oversized proposals before any mutation', () => {
    const project = makeProject()
    const stem = 'TASK_6_stdin_admission'
    const backlog = backlogDoc(stem)
    const pending = pendingDoc(stem)
    writeArtifact(project, 'backlog', stem, backlog)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'backlog')
    const indexBefore = readFileSync(join(project.tasks, 'INDEX.json'))
    const transitions = join(project.root, 'orchestrator', '.cache', 'tasks', 'transitions')
    const locks = join(project.root, 'orchestrator', '.cache', 'tasks', 'locks')
    const assertUntouched = () => {
      assert.equal(readFileSync(artifact(project, 'backlog', stem), 'utf8'), backlog)
      assert.equal(existsSync(artifact(project, 'pending', stem)), false)
      assert.deepEqual(readFileSync(join(project.tasks, 'INDEX.json')), indexBefore)
      assert.equal(existsSync(transitions), false, 'input admission must precede transition guard/marker creation')
      assert.equal(existsSync(locks), false, 'input admission must precede task-lock interaction')
    }

    parseFailure(run(project, [
      'ask', '--stem', stem, '--input', join(project.root, 'proposal.md'), '--source-revision', sourceRevision,
    ]), 2, 'INVOCATION_INVALID')
    assertUntouched()

    parseFailure(run(project, [
      'ask', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
    ], {}, Buffer.alloc(0)), 2, 'INPUT_EMPTY')
    assertUntouched()

    parseFailure(run(project, [
      'ask', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
    ], {}, Buffer.from([0xc3, 0x28])), 2, 'INPUT_UTF8_INVALID')
    assertUntouched()

    parseFailure(run(project, [
      'ask', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
    ], {}, Buffer.from('valid-prefix\0valid-suffix')), 2, 'INPUT_UTF8_INVALID')
    assertUntouched()

    parseFailure(run(project, [
      'ask', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
    ], {}, Buffer.alloc(8 * 1024 * 1024 + 1, 0x61)), 2, 'INPUT_TOO_LARGE')
    assertUntouched()
  })

  check('an open delayed stdin producer cannot acquire a transition guard or mutate state', () => {
    if (process.platform === 'win32') return
    const project = makeProject()
    const stem = 'TASK_7_delayed_stdin'
    const backlog = backlogDoc(stem)
    writeArtifact(project, 'backlog', stem, backlog)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'backlog')
    const indexBefore = readFileSync(join(project.tasks, 'INDEX.json'))
    const delayedHarness = `
import { spawn } from 'node:child_process';
import { existsSync } from 'node:fs';
const child = spawn(process.execPath, [process.env.DELAYED_TRANSITION_CLI,
  'ask', '--stem', process.env.DELAYED_STEM, '--input', '-', '--source-revision', process.env.DELAYED_REVISION], {
  cwd: process.env.DELAYED_PROJECT_ROOT, env: process.env, stdio: ['pipe', 'pipe', 'pipe']
});
let stdout = '', stderr = '';
child.stdout.on('data', (chunk) => { stdout += String(chunk); });
child.stderr.on('data', (chunk) => { stderr += String(chunk); });
child.stdin.on('error', () => {});
child.stdin.write(Buffer.from('partial-valid-utf8-without-eof'));
const watchdog = setTimeout(() => { try { child.kill('SIGKILL'); } catch {} process.exit(3); }, 5000);
setTimeout(() => {
  const live = child.exitCode === null && child.signalCode === null;
  const mutated = existsSync(process.env.DELAYED_TRANSITIONS_DIR) ||
    existsSync(process.env.DELAYED_LOCKS_DIR) || existsSync(process.env.DELAYED_PENDING_PATH);
  child.once('close', () => {
    clearTimeout(watchdog);
    process.stdout.write(JSON.stringify({ live, mutated, stdout, stderr }));
    process.exit(live && !mutated ? 0 : 1);
  });
  try { child.kill('SIGKILL'); } catch { process.exit(2); }
}, 500);
`
    const observed = spawnSync(process.execPath, ['--input-type=module', '-e', delayedHarness], {
      cwd: project.root,
      env: {
        ...envFor(project),
        DELAYED_TRANSITION_CLI: CLI,
        DELAYED_PROJECT_ROOT: project.root,
        DELAYED_STEM: stem,
        DELAYED_REVISION: sourceRevision,
        DELAYED_TRANSITIONS_DIR: join(project.root, 'orchestrator', '.cache', 'tasks', 'transitions'),
        DELAYED_LOCKS_DIR: join(project.root, 'orchestrator', '.cache', 'tasks', 'locks'),
        DELAYED_PENDING_PATH: artifact(project, 'pending', stem),
      },
      encoding: 'utf8', timeout: 10000,
    })
    assert.equal(observed.status, 0, observed.stderr + observed.stdout)
    assert.deepEqual(JSON.parse(observed.stdout), { live: true, mutated: false, stdout: '', stderr: '' })
    assert.equal(readFileSync(artifact(project, 'backlog', stem), 'utf8'), backlog)
    assert.deepEqual(readFileSync(join(project.tasks, 'INDEX.json')), indexBefore)
  })

  check('command admission rejects a missing revision without waiting for stdin or creating runtime state', () => {
    const project = makeProject()
    const stem = 'TASK_8_missing_revision'
    const backlog = backlogDoc(stem)
    writeArtifact(project, 'backlog', stem, backlog)
    runRegen(project)
    const indexBefore = readFileSync(join(project.tasks, 'INDEX.json'))
    const admissionHarness = `
import { spawn } from 'node:child_process';
const child = spawn(process.execPath, [process.env.ADMISSION_TRANSITION_CLI,
  'ask', '--stem', process.env.ADMISSION_STEM, '--input', '-'], {
  cwd: process.env.ADMISSION_PROJECT_ROOT, env: process.env, stdio: ['pipe', 'pipe', 'pipe']
});
let stdout = '', stderr = '';
child.stdout.on('data', (chunk) => { stdout += String(chunk); });
child.stderr.on('data', (chunk) => { stderr += String(chunk); });
child.stdin.on('error', () => {});
const watchdog = setTimeout(() => { try { child.kill('SIGKILL'); } catch {} process.exit(3); }, 5000);
child.once('close', (code, signal) => {
  clearTimeout(watchdog);
  process.stdout.write(JSON.stringify({ code, signal, stdout, stderr }));
  process.exit(code === 2 && signal === null ? 0 : 1);
});
`
    const observed = spawnSync(process.execPath, ['--input-type=module', '-e', admissionHarness], {
      cwd: project.root,
      env: {
        ...envFor(project),
        ADMISSION_TRANSITION_CLI: CLI,
        ADMISSION_PROJECT_ROOT: project.root,
        ADMISSION_STEM: stem,
      },
      encoding: 'utf8', timeout: 10000,
    })
    assert.equal(observed.status, 0, observed.stderr + observed.stdout)
    const result = JSON.parse(observed.stdout)
    assert.equal(result.code, 2)
    assert.equal(result.signal, null)
    assert.equal(result.stdout, '')
    assert.equal(JSON.parse(result.stderr).code, 'SOURCE_REVISION_REQUIRED')
    assert.equal(readFileSync(artifact(project, 'backlog', stem), 'utf8'), backlog)
    assert.deepEqual(readFileSync(join(project.tasks, 'INDEX.json')), indexBefore)
    assert.equal(existsSync(join(project.root, 'orchestrator', '.cache', 'tasks', 'transitions')), false)
    assert.equal(existsSync(join(project.root, 'orchestrator', '.cache', 'tasks', 'locks')), false)
  })

  check('command admission rejects unsupported flags before stdin or transition state', () => {
    const project = makeProject()
    const stem = 'TASK_9_command_flags'
    const backlog = backlogDoc(stem)
    writeArtifact(project, 'backlog', stem, backlog)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'backlog')
    parseFailure(run(project, [
      'drop', '--stem', stem, '--source-revision', sourceRevision, '--input', '-',
    ], {}, Buffer.from('ignored')), 2, 'INVOCATION_INVALID')
    parseFailure(run(project, [
      'ask', '--stem', stem, '--source-revision', 'sha256:not-a-revision', '--input', '-',
    ], {}, Buffer.from('ignored')), 2, 'SOURCE_REVISION_REQUIRED')
    assert.equal(readFileSync(artifact(project, 'backlog', stem), 'utf8'), backlog)
    assert.equal(existsSync(join(project.root, 'orchestrator', '.cache', 'tasks', 'transitions')), false)
  })

  check('stdin accepts the exact 8 MiB boundary and publishes those exact bytes', () => {
    const project = makeProject()
    const stem = 'TASK_13_exact_input_boundary'
    const backlog = backlogDoc(stem)
    const prefix = Buffer.from(pendingDoc(stem))
    const payload = Buffer.concat([prefix, Buffer.alloc(8 * 1024 * 1024 - prefix.length, 0x61)])
    assert.equal(payload.length, 8 * 1024 * 1024)
    writeArtifact(project, 'backlog', stem, backlog)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'backlog')
    parseSuccess(run(project, [
      'ask', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
    ], {}, payload))
    assert.deepEqual(readFileSync(artifact(project, 'pending', stem)), payload)
    assertFresh(project, stem, 'pending')
    assertNoRecoveryResidue(project)
  })

  check('drop requires explicit current dependent impact and preserves state on stale acknowledgements', () => {
    const project = makeProject()
    const stem = 'TASK_10_drop_source'
    const firstDependent = 'TASK_11_first_dependent'
    const secondDependent = 'TASK_12_second_dependent'
    const source = backlogDoc(stem)
    writeArtifact(project, 'backlog', stem, source)
    writeArtifact(project, 'todo', firstDependent, todoDoc(firstDependent, { dependency: stem }))
    const locks = join(project.root, 'orchestrator', '.cache', 'tasks', 'locks')
    const journal = join(project.root, 'orchestrator', '.cache', 'tasks', 'journal')
    mkdirSync(locks, { recursive: true }); mkdirSync(journal, { recursive: true })
    writeFileSync(join(journal, stem + '.jsonl'), '{"event":"fixture"}\n')
    runRegen(project)
    const sourceRevision = revision(project, stem, 'backlog')

    const firstImpact = parseSuccess(run(project, ['inspect-drop', '--stem', stem]), { requireOk: false })
    assert.deepEqual(firstImpact.dependents, [firstDependent])
    assert.match(firstImpact.impactHash, /^sha256:[a-f0-9]{64}$/)
    assert.equal(readFileSync(artifact(project, 'backlog', stem), 'utf8'), source,
      'preflight must be read-only')

    parseFailure(run(project, [
      'drop', '--stem', stem, '--source-revision', sourceRevision,
    ]), 1, 'DROP_DEPENDENTS_PRESENT')
    assert.equal(readFileSync(artifact(project, 'backlog', stem), 'utf8'), source)

    writeArtifact(project, 'todo', secondDependent, todoDoc(secondDependent, { dependency: stem }))
    runRegen(project)
    assert.equal(revision(project, stem, 'backlog'), sourceRevision,
      'an unrelated dependent must not alter the source revision fence')
    parseFailure(run(project, [
      'drop', '--stem', stem, '--source-revision', sourceRevision,
      '--ack-impact', firstImpact.impactHash,
    ]), 1, 'DROP_DEPENDENTS_PRESENT')
    assert.equal(readFileSync(artifact(project, 'backlog', stem), 'utf8'), source,
      'changed dependent impact must abort before mutation')

    const currentImpact = parseSuccess(run(project, ['inspect-drop', '--stem', stem]), { requireOk: false })
    assert.deepEqual(currentImpact.dependents, [firstDependent, secondDependent])
    assert.notEqual(currentImpact.impactHash, firstImpact.impactHash)
    writeFileSync(join(locks, stem + '.json'), JSON.stringify({ stage: 'task-prep', startedAt: '2026-07-13T08:00:00Z' }) + '\n')
    parseFailure(run(project, [
      'drop', '--stem', stem, '--source-revision', sourceRevision,
      '--ack-impact', currentImpact.impactHash,
    ]), 1, 'TASK_LOCK_PRESENT')
    assert.equal(readFileSync(join(locks, stem + '.json'), 'utf8'), JSON.stringify({ stage: 'task-prep', startedAt: '2026-07-13T08:00:00Z' }) + '\n')
    assert.equal(readFileSync(artifact(project, 'backlog', stem), 'utf8'), source)
    assert.equal(readFileSync(join(journal, stem + '.jsonl'), 'utf8'), '{"event":"fixture"}\n')
    unlinkSync(join(locks, stem + '.json'))
    const retainedRelease = join(locks, `.${stem}.json.release-${'a'.repeat(36)}`)
    mkdirSync(retainedRelease)
    parseFailure(run(project, [
      'drop', '--stem', stem, '--source-revision', sourceRevision,
      '--ack-impact', currentImpact.impactHash,
    ]), 1, 'LOCK_RELEASE_RECOVERY_REQUIRED')
    assert.equal(readFileSync(artifact(project, 'backlog', stem), 'utf8'), source)
    rmSync(retainedRelease, { recursive: true })
    const dropRun = run(project, [
      'drop', '--stem', stem, '--source-revision', sourceRevision,
      '--ack-impact', currentImpact.impactHash,
    ])
    const dropped = parseSuccess(dropRun)
    assert.equal(dropped.state, 'absent')
    assert.ok(taskStateEvents(dropRun.stderr).length >= 9)
    assert.ok(taskStateEvents(dropRun.stderr).filter((event) => event.caller !== 'server')
      .every((event) => event.caller === 'drop'))
    assert.equal(taskStateEvents(dropRun.stderr).filter((event) => event.caller === 'server').length, 6)
    assert.deepEqual(dropped.dependents, [firstDependent, secondDependent])
    assert.deepEqual(dropped.runtimeRemoved, ['journal'])
    assert.equal(existsSync(artifact(project, 'backlog', stem)), false)
    assert.equal(existsSync(join(locks, stem + '.json')), false)
    assert.equal(existsSync(join(journal, stem + '.jsonl')), false)
    assertFresh(project, stem, 'absent')
    assertNoRecoveryResidue(project)
  })

  check('drop removes malformed task content while another broken task keeps INDEX deferred', () => {
    const project = makeProject()
    const stem = 'TASK_81_broken_drop_target'
    const other = 'TASK_82_other_broken_task'
    writeArtifact(project, 'backlog', stem, backlogDoc(stem))
    writeArtifact(project, 'backlog', other, backlogDoc(other))
    runRegen(project)
    writeArtifact(project, 'backlog', stem, `# TASK ${numberOf(stem)} —\n\nTemporary partial content.\n`)
    writeArtifact(project, 'backlog', other, `# TASK ${numberOf(other)} —\n\nTemporary partial content.\n`)
    const broken = validate(project, { stem, checkIndex: true })
    assert.equal(broken.ok, false)
    assert.match(broken.sourceRevision, /^sha256:[a-f0-9]{64}$/)
    assert.equal(core.dropAdmission(
      core.validateAction({
        repoRoot: project.root,
        tasksDir: project.tasks,
        outcomeShapePath: OUTCOME_SHAPE,
        includeRuntime: false,
        stem,
        action: 'drop',
        checkIndex: true,
      }),
      stem
    ).ok, true)
    const impact = parseSuccess(run(project, ['inspect-drop', '--stem', stem]), { requireOk: false })
    const dropped = parseSuccess(run(project, [
      'drop', '--stem', stem, '--source-revision', broken.sourceRevision,
      '--ack-impact', impact.impactHash,
    ]))
    assert.equal(dropped.state, 'absent')
    assert.equal(dropped.indexDeferred, true)
    assert.equal(existsSync(artifact(project, 'backlog', stem)), false)
    assert.equal(existsSync(artifact(project, 'backlog', other)), true)
    assert.equal(core.dropAdmission(
      validate(project, { stem, transition: 'backlog:absent', phase: 'post', checkIndex: false }),
      stem,
      { allowAbsent: true }
    ).ok, true)
    assertNoRecoveryResidue(project)
  })

  check('drop atomically removes every exact artifact of a corrupt non-UTF8 task', () => {
    const project = makeProject()
    const stem = 'TASK_83_corrupt_bytes'
    writeFileSync(artifact(project, 'backlog', stem), Buffer.from([0xff, 0xfe, 0x00, 0x41]))
    writeArtifact(project, 'todo', stem, todoDoc(stem))
    const broken = validate(project, { stem, checkIndex: true })
    assert.equal(broken.observedState, 'corrupt')
    assert.equal(broken.findings.some((item) => item.code === 'TASK_ARTIFACT_UTF8_INVALID'), true)
    assert.equal(broken.findings.some((item) => item.code === 'TASK_PRESENT_IN_MULTIPLE_STATES'), true)
    assert.match(broken.sourceRevision, /^sha256:[a-f0-9]{64}$/)
    const action = core.validateAction({
      repoRoot: project.root,
      tasksDir: project.tasks,
      outcomeShapePath: OUTCOME_SHAPE,
      includeRuntime: false,
      stem,
      action: 'drop',
      checkIndex: true,
    })
    assert.equal(action.observedState, 'corrupt')
    assert.equal(core.dropAdmission(action, stem).ok, true)
    const impact = parseSuccess(run(project, ['inspect-drop', '--stem', stem]), { requireOk: false })
    assert.equal(impact.state, 'corrupt')
    const dropped = parseSuccess(run(project, [
      'drop', '--stem', stem, '--source-revision', broken.sourceRevision,
      '--ack-impact', impact.impactHash,
    ]))
    assert.equal(dropped.state, 'absent')
    assert.equal(existsSync(artifact(project, 'backlog', stem)), false)
    assert.equal(existsSync(artifact(project, 'todo', stem)), false)
    assertFresh(project, stem, 'absent')
    assertNoRecoveryResidue(project)
  })

  check('drop removes an oversized task by exact metadata generation without reading its body', () => {
    const project = makeProject()
    const stem = 'TASK_84_oversized_recovery'
    const source = artifact(project, 'backlog', stem)
    writeArtifact(project, 'backlog', stem, backlogDoc(stem))
    runRegen(project)
    // Deliberately exceed both the 8 MiB task-body contract and the 32 MiB
    // filesystem helper response bound. A safe deletion must therefore use
    // the exact inode/stat generation rather than a larger bounded read.
    truncateSync(source, 40 * 1024 * 1024)
    const broken = validate(project, { stem, checkIndex: true })
    assert.equal(broken.observedState, 'backlog')
    assert.equal(broken.findings.some((item) => item.code === 'TASK_ARTIFACT_TOO_LARGE'), true)
    assert.match(broken.sourceRevision, /^sha256:[a-f0-9]{64}$/)
    const impact = parseSuccess(run(project, ['inspect-drop', '--stem', stem]), { requireOk: false })
    assert.equal(impact.sourceRevision, broken.sourceRevision)
    const dropped = parseSuccess(run(project, [
      'drop', '--stem', stem, '--source-revision', impact.sourceRevision,
      '--ack-impact', impact.impactHash,
    ]))
    assert.equal(dropped.state, 'absent')
    assert.equal(existsSync(source), false)
    assertFresh(project, stem, 'absent')
    assertNoRecoveryResidue(project)
  })

  await checkAsync('a later dependency publication waits for drop linearization and reports the absent reference', async () => {
    const project = makeProject()
    const sourceStem = 'TASK_71_drop_graph_source'
    const dependentStem = 'TASK_72_drop_graph_dependent'
    const source = backlogDoc(sourceStem)
    const dependent = backlogDoc(dependentStem)
    const proposed = dependent.trimEnd() + `\n\n## Depends on (optional)\n\n- ${sourceStem}\n`
    writeArtifact(project, 'backlog', sourceStem, source)
    writeArtifact(project, 'backlog', dependentStem, dependent)
    runRegen(project)
    const sourceRevision = revision(project, sourceStem, 'backlog')
    const dependentRevision = revision(project, dependentStem, 'backlog')
    const sentinel = join(project.root, 'drop-final-impact-held')

    const dropping = runAsync(project, [
      'drop', '--stem', sourceStem, '--source-revision', sourceRevision,
    ], {
      TASK_TRANSITION_TEST_PAUSE_STAGE: 'drop-final-impact',
      TASK_TRANSITION_TEST_PAUSE_SENTINEL: sentinel,
      TASK_TRANSITION_TEST_PAUSE_MS: '1200',
    })
    await waitUntil(() => existsSync(sentinel), 'drop never reached its fenced final-impact boundary')
    const editing = runAsync(project, [
      'edit', '--stem', dependentStem, '--input', '-', '--source-revision', dependentRevision,
    ], {}, Buffer.from(proposed))
    await new Promise((resolve) => setTimeout(resolve, 150))
    assert.equal(editing.settled(), false, 'dependency writer crossed the graph fence while drop held it')

    parseSuccess(await dropping.done)
    const editedRun = await editing.done
    parseSuccess(editedRun)
    assert.ok(taskStateEvents(editedRun.stderr).some((event) =>
      Array.isArray(event.findings) && event.findings.some((item) => item.code === 'DEPENDENCY_UNRESOLVED')),
    'the later serialized writer must report the now-absent dependency canonically')
    assert.equal(existsSync(artifact(project, 'backlog', sourceStem)), false)
    assert.equal(readFileSync(artifact(project, 'backlog', dependentStem), 'utf8'), proposed)
    assertFresh(project, sourceStem, 'absent')
    assertFresh(project, dependentStem, 'backlog')
    assertNoRecoveryResidue(project)
  })

  await checkAsync('drop waits for an earlier dependency publication and then requires its fresh impact', async () => {
    const project = makeProject()
    const sourceStem = 'TASK_73_drop_waits_for_edge'
    const dependentStem = 'TASK_74_edge_publisher'
    const source = backlogDoc(sourceStem)
    const dependent = backlogDoc(dependentStem)
    const proposed = dependent.trimEnd() + `\n\n## Depends on (optional)\n\n- ${sourceStem}\n`
    writeArtifact(project, 'backlog', sourceStem, source)
    writeArtifact(project, 'backlog', dependentStem, dependent)
    runRegen(project)
    const sourceRevision = revision(project, sourceStem, 'backlog')
    const dependentRevision = revision(project, dependentStem, 'backlog')
    const sentinel = join(project.root, 'edit-index-held')

    const editing = runAsync(project, [
      'edit', '--stem', dependentStem, '--input', '-', '--source-revision', dependentRevision,
    ], {
      TASK_TRANSITION_TEST_PAUSE_STAGE: 'edit-index-published',
      TASK_TRANSITION_TEST_PAUSE_SENTINEL: sentinel,
      TASK_TRANSITION_TEST_PAUSE_MS: '1200',
    }, Buffer.from(proposed))
    await waitUntil(() => existsSync(sentinel), 'dependency writer never reached its fenced INDEX boundary')
    const dropping = runAsync(project, [
      'drop', '--stem', sourceStem, '--source-revision', sourceRevision,
    ])
    await new Promise((resolve) => setTimeout(resolve, 150))
    assert.equal(dropping.settled(), false, 'drop crossed the graph fence while dependency publication held it')

    parseSuccess(await editing.done)
    const refused = parseFailure(await dropping.done, 1, 'DROP_DEPENDENTS_PRESENT')
    assert.equal(refused.retryable, false)
    assert.equal(readFileSync(artifact(project, 'backlog', sourceStem), 'utf8'), source)
    assert.equal(readFileSync(artifact(project, 'backlog', dependentStem), 'utf8'), proposed)
    assertFresh(project, sourceStem, 'backlog')
    assertFresh(project, dependentStem, 'backlog')
    assertNoRecoveryResidue(project)
  })

  check('drop removes both durable members of a pending task as one logical state', () => {
    const project = makeProject()
    const stem = 'TASK_15_drop_pending_pair'
    writeArtifact(project, 'backlog', stem, backlogDoc(stem))
    writeArtifact(project, 'pending', stem, pendingDoc(stem))
    runRegen(project)
    const sourceRevision = revision(project, stem, 'pending')

    const dropped = parseSuccess(run(project, [
      'drop', '--stem', stem, '--source-revision', sourceRevision,
    ]))
    assert.equal(dropped.state, 'absent')
    assert.equal(existsSync(artifact(project, 'backlog', stem)), false)
    assert.equal(existsSync(artifact(project, 'pending', stem)), false)
    assertFresh(project, stem, 'absent')
    assertNoRecoveryResidue(project)
  })

  check('drop removes an exact todo generation and publishes absent state', () => {
    const project = makeProject()
    const stem = 'TASK_16_drop_todo'
    writeArtifact(project, 'todo', stem, todoDoc(stem))
    runRegen(project)
    const sourceRevision = revision(project, stem, 'todo')

    const dropped = parseSuccess(run(project, [
      'drop', '--stem', stem, '--source-revision', sourceRevision,
    ]))
    assert.equal(dropped.state, 'absent')
    assert.equal(existsSync(artifact(project, 'todo', stem)), false)
    assertFresh(project, stem, 'absent')
    assertNoRecoveryResidue(project)
  })

  check('drop fails closed on symlinked lock and journal directory ancestors', () => {
    const lockProject = makeProject()
    const lockStem = 'TASK_13_unsafe_lock_root'
    const lockSource = backlogDoc(lockStem)
    writeArtifact(lockProject, 'backlog', lockStem, lockSource)
    runRegen(lockProject)
    const lockRevision = revision(lockProject, lockStem, 'backlog')
    const cacheTasks = join(lockProject.root, 'orchestrator', '.cache', 'tasks')
    const externalLocks = join(lockProject.root, 'external-locks')
    mkdirSync(cacheTasks, { recursive: true }); mkdirSync(externalLocks)
    symlinkSync(externalLocks, join(cacheTasks, 'locks'))

    parseFailure(run(lockProject, [
      'drop', '--stem', lockStem, '--source-revision', lockRevision,
    ]), 3, 'RUNTIME_DIR_UNSAFE')
    assert.equal(readFileSync(artifact(lockProject, 'backlog', lockStem), 'utf8'), lockSource)
    assertNoRecoveryResidue(lockProject)

    const journalProject = makeProject()
    const journalStem = 'TASK_14_unsafe_journal_root'
    const journalSource = backlogDoc(journalStem)
    writeArtifact(journalProject, 'backlog', journalStem, journalSource)
    runRegen(journalProject)
    const journalRevision = revision(journalProject, journalStem, 'backlog')
    const journalCache = join(journalProject.root, 'orchestrator', '.cache', 'tasks')
    const externalJournal = join(journalProject.root, 'external-journal')
    mkdirSync(join(journalCache, 'locks'), { recursive: true }); mkdirSync(externalJournal)
    const externalEvent = join(externalJournal, journalStem + '.jsonl')
    writeFileSync(externalEvent, '{"event":"outside"}\n')
    symlinkSync(externalJournal, join(journalCache, 'journal'))

    parseFailure(run(journalProject, [
      'drop', '--stem', journalStem, '--source-revision', journalRevision,
    ]), 3, 'RUNTIME_DIR_UNSAFE')
    assert.equal(readFileSync(artifact(journalProject, 'backlog', journalStem), 'utf8'), journalSource)
    assert.equal(readFileSync(externalEvent, 'utf8'), '{"event":"outside"}\n')
    assertNoRecoveryResidue(journalProject)
  })

  check('done to todo stores immutable content-addressed history and strips Outcome', () => {
    const project = makeProject()
    const stem = 'TASK_20_reopen'
    const done = doneDoc(stem)
    writeArtifact(project, 'done', stem, done)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'done')

    const reopenRun = run(project, [
      'reopen', '--stem', stem, '--source-revision', sourceRevision,
    ])
    const value = parseSuccess(reopenRun)
    assert.equal(value.operation, 'reopen')
    assert.equal(value.state, 'todo')
    assert.ok(taskStateEvents(reopenRun.stderr).length >= 9)
    assert.ok(taskStateEvents(reopenRun.stderr).filter((event) => event.caller !== 'server')
      .every((event) => event.caller === 'reopen'))
    assert.equal(taskStateEvents(reopenRun.stderr).filter((event) => event.caller === 'server').length, 6)
    assert.equal(existsSync(artifact(project, 'done', stem)), false)
    const reopened = readFileSync(artifact(project, 'todo', stem), 'utf8')
    assert.equal(reopened, todoDoc(stem, { title: 'Completed transition' }).trimEnd() + '\n')
    assert.doesNotMatch(reopened, /^## Outcome$/m)

    const expectedHistory = join(project.tasks, 'evidence', 'reopen', stem, sha256Hex(Buffer.from(done)) + '.md')
    assert.equal(value.historyPath, `orchestrator/tasks/evidence/reopen/${stem}/${sha256Hex(Buffer.from(done))}.md`)
    assert.equal(readFileSync(expectedHistory, 'utf8'), done)
    assertFresh(project, stem, 'todo')
    assertNoRecoveryResidue(project)
  })

  check('reopen strips a three-space CommonMark Outcome H2 with closing hashes', () => {
    const project = makeProject()
    const stem = 'TASK_69_reopen_commonmark_outcome'
    const done = doneDoc(stem).replace('## Outcome', '   ## Outcome ##')
    writeArtifact(project, 'done', stem, done)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'done')

    const value = parseSuccess(run(project, [
      'reopen', '--stem', stem, '--source-revision', sourceRevision,
    ]))
    assert.equal(value.state, 'todo')
    const reopened = readFileSync(artifact(project, 'todo', stem), 'utf8')
    assert.equal(reopened, todoDoc(stem, { title: 'Completed transition' }).trimEnd() + '\n')
    assert.equal(core.outcomeAppendixStart(reopened), -1)
    const expectedHistory = join(project.tasks, 'evidence', 'reopen', stem, sha256Hex(Buffer.from(done)) + '.md')
    assert.equal(readFileSync(expectedHistory, 'utf8'), done)
    assertFresh(project, stem, 'todo')
    assertNoRecoveryResidue(project)
  })

  check('reopen rejects BOM/CRLF done bytes without migrating or truncating them', () => {
    const project = makeProject()
    const stem = 'TASK_70_reopen_crlf_offsets'
    const done = '\uFEFF' + doneDoc(stem).replace(/\n/g, '\r\n')
    assert.equal(core.outcomeAppendixStart(done), -1)

    writeArtifact(project, 'done', stem, done)
    const indexBefore = readFileSync(join(project.tasks, 'INDEX.json'))
    const result = spawnSync(process.env.PYTHON || 'python3', [REGEN], {
      cwd: project.root, env: envFor(project), encoding: 'utf8', maxBuffer: 8 * 1024 * 1024, timeout: 60000,
    })
    assert.equal(result.status, 1)
    assert.match(result.stderr, /TASK_SOURCE_MALFORMED/)
    assert.equal(readFileSync(artifact(project, 'done', stem), 'utf8'), done)
    assert.deepEqual(readFileSync(join(project.tasks, 'INDEX.json')), indexBefore)
    assert.equal(existsSync(artifact(project, 'todo', stem)), false)
    assertNoRecoveryResidue(project)
  })

  check('reopen preserves exact offsets when fenced code contains a fake Outcome anchor', () => {
    const project = makeProject()
    const stem = 'TASK_21_reopen_fenced_outcome'
    const baseTodo = todoDoc(stem, { title: 'Completed transition' }).replace(
      '## Inputs',
      ['```markdown', '---', '', '## Outcome', '', 'This is an inert example.', '```', '', '## Inputs'].join('\n'),
    )
    const canonicalDone = doneDoc(stem)
    const trailerOffset = canonicalDone.lastIndexOf('\n---\n\n## Outcome')
    assert.ok(trailerOffset > 0)
    const done = baseTodo.trimEnd() + canonicalDone.slice(trailerOffset)
    const structural = core.structuralText(done)
    assert.equal(structural.length, done.length, 'structural masking must preserve UTF-16 offsets')
    assert.equal(structural.indexOf('## Inputs'), done.indexOf('## Inputs'))
    assert.equal((structural.match(/^## Outcome$/gm) || []).length, 1, 'fenced fake heading must stay inert')

    writeArtifact(project, 'done', stem, done)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'done')
    parseSuccess(run(project, ['reopen', '--stem', stem, '--source-revision', sourceRevision]))

    assert.equal(readFileSync(artifact(project, 'todo', stem), 'utf8'), baseTodo.trimEnd() + '\n')
    const expectedHistory = join(project.tasks, 'evidence', 'reopen', stem, sha256Hex(Buffer.from(done)) + '.md')
    assert.equal(readFileSync(expectedHistory, 'utf8'), done)
    assertFresh(project, stem, 'todo')
    assertNoRecoveryResidue(project)
  })

  check('reopen fails closed when a todo collision appears after the indexed done snapshot', () => {
    const project = makeProject()
    const stem = 'TASK_22_reopen_target_collision'
    const done = doneDoc(stem)
    const foreignTodo = todoDoc(stem, { title: 'Foreign concurrent generation' })
    writeArtifact(project, 'done', stem, done)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'done')
    writeArtifact(project, 'todo', stem, foreignTodo)

    parseFailure(run(project, [
      'reopen', '--stem', stem, '--source-revision', sourceRevision,
    ]), 1, 'TRANSITION_PRECONDITION_FAILED')
    assert.equal(readFileSync(artifact(project, 'done', stem), 'utf8'), done)
    assert.equal(readFileSync(artifact(project, 'todo', stem), 'utf8'), foreignTodo)
    assertNoRecoveryResidue(project)
  })

  check('stale source revisions abort before mutation with a retryable verdict', () => {
    const project = makeProject()
    const stem = 'TASK_30_stale_revision'
    const original = backlogDoc(stem, 'Original title')
    const changed = backlogDoc(stem, 'Changed title')
    writeArtifact(project, 'backlog', stem, original)
    runRegen(project)
    const staleRevision = revision(project, stem, 'backlog')

    writeArtifact(project, 'backlog', stem, changed)
    runRegen(project)
    const currentRevision = revision(project, stem, 'backlog')
    assert.notEqual(currentRevision, staleRevision)
    const input = writeInput(project, 'stale-pending.md', pendingDoc(stem))
    const failure = parseFailure(run(project, [
      'ask', '--stem', stem, '--input', input, '--source-revision', staleRevision,
    ]), 4, 'TRANSITION_SOURCE_CHANGED')
    assert.equal(failure.retryable, true)
    assert.equal(readFileSync(artifact(project, 'backlog', stem), 'utf8'), changed)
    assert.equal(existsSync(artifact(project, 'pending', stem)), false)
    assertFresh(project, stem, 'backlog')
    assertNoRecoveryResidue(project)
  })

  check('source edit between the green pre-check and first mutation aborts transiently', () => {
    const project = makeProject()
    const stem = 'TASK_31_midflight_revision'
    const original = backlogDoc(stem, 'Original midflight source')
    writeArtifact(project, 'backlog', stem, original)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'backlog')
    const input = writeInput(project, 'midflight-pending.md', pendingDoc(stem))

    const failure = parseFailure(run(project, [
      'ask', '--stem', stem, '--input', input, '--source-revision', sourceRevision,
    ], { TASK_TRANSITION_TEST_MUTATE_BEFORE_RECONFIRM: '1' }), 4, 'TRANSITION_SOURCE_CHANGED')
    assert.equal(failure.retryable, true)
    assert.match(readFileSync(artifact(project, 'backlog', stem), 'utf8'), /Fixture mutation between pre-check/)
    assert.equal(existsSync(artifact(project, 'pending', stem)), false)
    runRegen(project)
    assertFresh(project, stem, 'backlog')
    assertNoRecoveryResidue(project)
  })

  check('pre-mutation pending race never deletes an identical incumbent sidecar', () => {
    const project = makeProject()
    const stem = 'TASK_33_pending_incumbent'
    const backlog = backlogDoc(stem, 'Pending race source')
    const pending = pendingDoc(stem)
    writeArtifact(project, 'backlog', stem, backlog)
    writeArtifact(project, 'pending', stem, pending)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'pending')
    const input = writeInput(project, 'answered-pending.md', pendingDoc(stem, { answer: 'strict' }))

    parseFailure(run(project, [
      'persist-answers', '--stem', stem, '--input', input, '--source-revision', sourceRevision,
    ], { TASK_TRANSITION_TEST_MUTATE_BEFORE_RECONFIRM: '1' }), 4, 'TRANSITION_SOURCE_CHANGED')
    assert.match(readFileSync(artifact(project, 'backlog', stem), 'utf8'), /Fixture mutation between pre-check/)
    assert.equal(readFileSync(artifact(project, 'pending', stem), 'utf8'), pending)
    const stale = validate(project, { stem, expect: 'pending', checkIndex: true })
    assert.equal(stale.ok, false)
    assert.ok(stale.findings.some((finding) => finding.code === 'INDEX_STALE'))
    runRegen(project)
    assertFresh(project, stem, 'pending')
    assertNoRecoveryResidue(project)
  })

  check('crash after private detach is recoverable from the write-ahead path', () => {
    const project = makeProject()
    const stem = 'TASK_34_detach_crash'
    writeArtifact(project, 'backlog', stem, backlogDoc(stem, 'Detach crash source'))
    runRegen(project)
    const sourceRevision = revision(project, stem, 'backlog')
    const todo = todoDoc(stem, { title: 'Recovered detached transition' })
    const input = writeInput(project, 'detach-crash-todo.md', todo)

    const crashed = run(project, [
      'promote', '--stem', stem, '--input', input, '--source-revision', sourceRevision,
    ], { TASK_TRANSITION_TEST_CRASH_AFTER_DETACH: '1' })
    assert.equal(crashed.status, 86, crashed.stderr + crashed.stdout)
    assert.equal(existsSync(artifact(project, 'backlog', stem)), false)
    assert.equal(existsSync(artifact(project, 'todo', stem)), false,
      'source ownership is established before publishing the target')
    const transitionDir = join(project.root, 'orchestrator', '.cache', 'tasks', 'transitions')
    const markerName = readdirSync(transitionDir).find((name) => /^tr-[a-f0-9]{36}\.json$/.test(name))
    assert.ok(markerName)
    const marker = JSON.parse(readFileSync(join(transitionDir, markerName), 'utf8'))
    assert.equal(marker.phase, 'prepared')
    assert.equal(typeof marker.detached.backlog, 'object')
    assert.equal(readFileSync(marker.detached.backlog.backup, 'utf8'), backlogDoc(stem, 'Detach crash source'))

    const recovered = parseSuccess(run(project, ['recover', '--stem', stem]))
    assert.deepEqual(recovered.recovered, [{ transactionId: marker.transactionId, resolution: 'rolled-back' }])
    assertFresh(project, stem, 'backlog')
    assertNoRecoveryResidue(project)

    const resumedRevision = revision(project, stem, 'backlog')
    parseSuccess(run(project, ['promote', '--stem', stem, '--input', input, '--source-revision', resumedRevision]))
    assertFresh(project, stem, 'todo')
  })

  check('post-commit cleanup interruption never rolls back the committed target', () => {
    const project = makeProject()
    const stem = 'TASK_32_committed_cleanup'
    const backlog = backlogDoc(stem, 'Cleanup boundary source')
    const pending = pendingDoc(stem)
    writeArtifact(project, 'backlog', stem, backlog)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'backlog')
    const input = writeInput(project, 'committed-cleanup-pending.md', pending)

    const failure = parseFailure(run(project, [
      'ask', '--stem', stem, '--input', input, '--source-revision', sourceRevision,
    ], { TASK_TRANSITION_TEST_FAIL_CLEANUP: '1' }), 4, 'TRANSITION_COMMITTED_RECOVERY_REQUIRED')
    assert.equal(failure.retryable, true)
    assert.equal(readFileSync(artifact(project, 'backlog', stem), 'utf8'), backlog)
    assert.equal(readFileSync(artifact(project, 'pending', stem), 'utf8'), pending)
    assertFresh(project, stem, 'pending')

    const transitionDir = join(project.root, 'orchestrator', '.cache', 'tasks', 'transitions')
    const marker = readdirSync(transitionDir).find((name) => /^tr-[a-f0-9]{36}\.json$/.test(name))
    assert.ok(marker, 'committed cleanup interruption must retain its recovery marker')
    assert.equal(JSON.parse(readFileSync(join(transitionDir, marker), 'utf8')).phase, 'complete')

    const secondPending = pendingDoc(stem, { round: 2, question: 2, gapCount: 1 })
    const secondInput = writeInput(project, 'must-wait-for-recovery.md', secondPending)
    const committedRevision = revision(project, stem, 'pending')
    parseFailure(run(project, [
      'ask', '--stem', stem, '--input', secondInput, '--source-revision', committedRevision,
    ]), 1, 'TRANSITION_RECOVERY_REQUIRED')
    assert.equal(readFileSync(artifact(project, 'pending', stem), 'utf8'), pending,
      'a retained committed marker must fence the next same-stem operation')

    const recovered = parseSuccess(run(project, ['recover', '--stem', stem]))
    assert.deepEqual(recovered.recovered, [{ transactionId: marker.slice(0, -5), resolution: 'completed-forward' }])
    assertFresh(project, stem, 'pending')
    assertNoRecoveryResidue(project)

    const resumed = parseSuccess(run(project, [
      'ask', '--stem', stem, '--input', secondInput, '--source-revision', committedRevision,
    ]))
    assert.equal(resumed.state, 'pending')
    assert.equal(readFileSync(artifact(project, 'pending', stem), 'utf8'), secondPending)
    assertFresh(project, stem, 'pending')
    assertNoRecoveryResidue(project)
  })

  check('source replacement after reconfirm but before detach is preserved and aborts transiently', () => {
    const project = makeProject()
    const stem = 'TASK_37_detach_cas_race'
    const original = backlogDoc(stem, 'Frozen before detach')
    writeArtifact(project, 'backlog', stem, original)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'backlog')
    const input = writeInput(project, 'detach-cas-todo.md', todoDoc(stem))

    parseFailure(run(project, [
      'promote', '--stem', stem, '--input', input, '--source-revision', sourceRevision,
    ], { TASK_TRANSITION_TEST_MUTATE_BEFORE_DETACH: '1' }), 4, 'TRANSITION_SOURCE_CHANGED')
    assert.match(readFileSync(artifact(project, 'backlog', stem), 'utf8'), /Fixture mutation immediately before destructive detach/)
    assert.equal(existsSync(artifact(project, 'todo', stem)), false)
    runRegen(project)
    assertFresh(project, stem, 'backlog')
    assertNoRecoveryResidue(project)
  })

  check('task-prep mutation is bound to the exact lock session and generation', () => {
    const project = makeProject()
    const stem = 'TASK_38_lock_binding'
    const sessionId = 'ws-fixture-session-00000001'
    writeArtifact(project, 'backlog', stem, backlogDoc(stem))
    runRegen(project)
    const sourceRevision = revision(project, stem, 'backlog')
    const input = writeInput(project, 'lock-bound-pending.md', pendingDoc(stem))
    const lock = canonicalTaskLock(project, stem, 'task-prep', sessionId)

    parseFailure(run(project, [
      'ask', '--stem', stem, '--input', input, '--source-revision', sourceRevision,
    ], { TASK_TRANSITION_TEST_SESSION_ID: 'ws-foreign-session-00000002' }), 1, 'TASK_LOCK_OWNER_MISMATCH')
    assert.equal(existsSync(artifact(project, 'pending', stem)), false)

    parseFailure(run(project, [
      'ask', '--stem', stem, '--input', input, '--source-revision', sourceRevision,
    ], {
      TASK_TRANSITION_TEST_SESSION_ID: sessionId,
      TASK_TRANSITION_TEST_MUTATE_LOCK_BEFORE_RECONFIRM: '1',
    }), 4, 'TASK_LOCK_CHANGED')
    assert.equal(existsSync(artifact(project, 'pending', stem)), false)
    assert.match(readFileSync(lock.file, 'utf8'), /\}\s+$/)
    writeFileSync(lock.file, JSON.stringify(JSON.parse(readFileSync(lock.file, 'utf8')), null, 2) + '\n')

    parseSuccess(run(project, [
      'ask', '--stem', stem, '--input', input, '--source-revision', sourceRevision,
    ], { TASK_TRANSITION_TEST_SESSION_ID: sessionId }))
    assertFresh(project, stem, 'pending')
    assertNoRecoveryResidue(project)
  })

  await checkAsync('writer authority is verified before transition and dependency-graph locks', async () => {
    const project = makeProject()
    const stem = 'TASK_39_authority_lock_order'
    const sessionId = 'ws-fixture-session-00000039'
    writeArtifact(project, 'backlog', stem, backlogDoc(stem))
    runRegen(project)
    const sourceRevision = revision(project, stem, 'backlog')
    const input = Buffer.from(pendingDoc(stem))
    const sentinel = join(project.root, 'authority-verified-before-locks')
    canonicalTaskLock(project, stem, 'task-prep', sessionId)

    const transition = runAsync(project, [
      'ask', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
    ], {
      TASK_TRANSITION_TEST_SESSION_ID: sessionId,
      TASK_TRANSITION_TEST_PAUSE_STAGE: 'authority-verified',
      TASK_TRANSITION_TEST_PAUSE_SENTINEL: sentinel,
      TASK_TRANSITION_TEST_PAUSE_MS: '900',
    }, input)
    await waitUntil(() => existsSync(sentinel), 'transition never verified writer authority')

    const guard = join(project.root, 'orchestrator', '.cache', 'tasks', 'transitions', '.guards', stem + '.json')
    assert.equal(existsSync(guard), false,
      'per-stem transition guard must not be acquired before writer authority')
    const mutex = join(project.root, 'orchestrator', '.cache', 'tasks', 'finalizations', '.mutex.json')
    if (existsSync(mutex)) {
      assert.equal(JSON.parse(readFileSync(mutex, 'utf8')).released, true,
        'writer authority must not run underneath this invocation\'s live dependency-graph mutex')
    }

    parseSuccess(await transition.done)
    assertFresh(project, stem, 'pending')
    assertNoRecoveryResidue(project)
  })

  await checkAsync('writer authority revoked while waiting for the graph mutex cannot mutate', async () => {
    const project = makeProject()
    const holderStem = 'TASK_40_authority_wait_holder'
    const revokedStem = 'TASK_41_authority_revoked'
    writeArtifact(project, 'backlog', holderStem, backlogDoc(holderStem))
    writeArtifact(project, 'backlog', revokedStem, backlogDoc(revokedStem))
    runRegen(project)
    const holderRevision = revision(project, holderStem, 'backlog')
    const revokedRevision = revision(project, revokedStem, 'backlog')
    const holderPause = join(project.root, 'holder-authority-reverified')
    const authoritySentinel = join(project.root, 'revocable-writer-authority')
    writeFileSync(authoritySentinel, 'exact fixture authority\n')

    const holder = runAsync(project, [
      'ask', '--stem', holderStem, '--input', '-', '--source-revision', holderRevision,
    ], {
      TASK_TRANSITION_TEST_PAUSE_STAGE: 'authority-reverified',
      TASK_TRANSITION_TEST_PAUSE_SENTINEL: holderPause,
      TASK_TRANSITION_TEST_PAUSE_MS: '1200',
    }, Buffer.from(pendingDoc(holderStem)))
    await waitUntil(() => existsSync(holderPause), 'holder never acquired the dependency-graph mutex')

    const revoked = runAsync(project, [
      'ask', '--stem', revokedStem, '--input', '-', '--source-revision', revokedRevision,
    ], {
      TASK_TRANSITION_TEST_AUTHORITY_SENTINEL: authoritySentinel,
    }, Buffer.from(pendingDoc(revokedStem)))
    const revokedGuard = join(project.root, 'orchestrator', '.cache', 'tasks', 'transitions', '.guards', revokedStem + '.json')
    await waitUntil(() => existsSync(revokedGuard), 'revoked writer never passed initial authority admission')
    assert.equal(revoked.settled(), false, 'revoked writer did not wait behind the graph mutex')
    unlinkSync(authoritySentinel)

    parseSuccess(await holder.done)
    const refused = parseFailure(await revoked.done, 4, 'WRITER_AUTHORITY_LOST')
    assert.equal(refused.retryable, true)
    assert.equal(existsSync(artifact(project, 'pending', revokedStem)), false)
    assertFresh(project, revokedStem, 'backlog')
    assertNoRecoveryResidue(project)
  })

  check('attached site-session authority survives the post-mutex receipt recheck without self-deadlock', () => {
    const project = makeProject()
    const stem = 'TASK_42_site_session_recheck'
    const writers = join(project.root, 'orchestrator', '.cache', 'tasks', 'finalizations', '.writers')
    const finalizations = join(project.root, 'orchestrator', '.cache', 'tasks', 'finalizations')
    const sessionId = 'ws-fixture-site-session-00000042'
    mkdirSync(writers, { recursive: true })
    writeArtifact(project, 'backlog', stem, backlogDoc(stem))
    runRegen(project)
    const sourceRevision = revision(project, stem, 'backlog')
    const lease = writerLeases.acquire(writers, {
      kind: 'task-session', stem, key: 'task:' + stem, sessionId,
      ownerPid: process.pid, pendingChild: true, rootDir: project.root,
    })
    writerLeases.updateChildPid(lease, process.pid)
    canonicalTaskLock(project, stem, 'task-prep', sessionId)
    try {
      const result = run(project, [
        'ask', '--stem', stem, '--input', '-', '--source-revision', sourceRevision,
      ], {
        TASK_TRANSITION_TEST_UNLEASED: '0',
        ORCHESTRATOR_WRITER_SESSION_ID: sessionId,
        ORCHESTRATOR_WRITER_LEASE_ID: lease.leaseId,
        ORCHESTRATOR_WRITER_DELEGATION_TOKEN: lease.delegationToken,
        FINALIZE_PROJECT_ROOT: project.root,
        FINALIZE_STATE_DIR: finalizations,
      }, Buffer.from(pendingDoc(stem)))
      parseSuccess(result)
      assertFresh(project, stem, 'pending')
      assertNoRecoveryResidue(project)
    } finally {
      writerLeases.release(lease)
    }
  })

  check('parent writer authority promotes only its exact split child under a child lock', () => {
    const project = makeProject()
    const parent = 'TASK_60_figma_parent'
    const child = 'TASK_61_figma_child'
    const sessionId = 'ws-fixture-session-00000003'
    writeArtifact(project, 'backlog', child, splitBacklogDoc(child, parent))
    runRegen(project)
    const sourceRevision = revision(project, child, 'backlog')
    const input = writeInput(project, 'delegated-child-todo.md', todoDoc(child))
    canonicalTaskLock(project, child, 'task-prep', sessionId)

    parseSuccess(run(project, [
      'promote', '--stem', child, '--authority-stem', parent, '--input', input,
      '--source-revision', sourceRevision,
    ], { TASK_TRANSITION_TEST_SESSION_ID: sessionId }))
    assertFresh(project, child, 'todo')
    assertNoRecoveryResidue(project)

    const badProject = makeProject()
    const badChild = 'TASK_62_wrong_parent'
    writeArtifact(badProject, 'backlog', badChild, splitBacklogDoc(badChild, 'TASK_99_someone_else'))
    runRegen(badProject)
    const badRevision = revision(badProject, badChild, 'backlog')
    const badInput = writeInput(badProject, 'wrong-parent-todo.md', todoDoc(badChild))
    canonicalTaskLock(badProject, badChild, 'task-prep', sessionId)
    parseFailure(run(badProject, [
      'promote', '--stem', badChild, '--authority-stem', parent, '--input', badInput,
      '--source-revision', badRevision,
    ], { TASK_TRANSITION_TEST_SESSION_ID: sessionId }), 2, 'WRITER_AUTHORITY_SCOPE_MISMATCH')
    assertFresh(badProject, badChild, 'backlog')
  })

  check('foundation prerequisite child follows the delegated promotion contract with typed refusals', () => {
    const parent = 'TASK_70_foundation_parent'
    const sessionId = 'ws-fixture-session-00000004'
    const intentHash = 'sha256:' + 'ab'.repeat(32)
    const foundationSourceBlock = (source) => [
      '## Source', '', '- Kind: ' + source.kind, '- Type: ' + source.type,
      '- Ref: ' + source.ref, '- Fingerprint: ' + source.fingerprint,
    ].join('\n')
    const foundationBacklogDoc = (stem, originParent, source) =>
      `# TASK ${numberOf(stem)} — Foundation prerequisite\n\n${foundationSourceBlock(source)}\n\nInstall the canonical lean test foundation.\n\n## Origin\n\n- split from ${originParent}\n`
    const foundationTodoDoc = (stem, originParent, source) =>
      todoDoc(stem, { title: 'Foundation prerequisite' }).trimEnd().replace(sourceBlock(stem), foundationSourceBlock(source)) +
      `\n\n## Origin\n\n- split from ${originParent}\n`

    // POSITIVE: the canonical child (Origin parent == Source Ref == delegating
    // parent, factory fingerprint) promotes under parent writer authority.
    const good = makeProject()
    const child = 'TASK_71_foundation_child'
    const goodSource = taskSourceContract.testFoundationPrerequisite(parent, intentHash)
    writeArtifact(good, 'backlog', child, foundationBacklogDoc(child, parent, goodSource))
    runRegen(good)
    const goodRevision = revision(good, child, 'backlog')
    const goodInput = writeInput(good, 'foundation-child-todo.md', foundationTodoDoc(child, parent, goodSource))
    canonicalTaskLock(good, child, 'task-prep', sessionId)
    parseSuccess(run(good, [
      'promote', '--stem', child, '--authority-stem', parent, '--input', goodInput,
      '--source-revision', goodRevision,
    ], { TASK_TRANSITION_TEST_SESSION_ID: sessionId }))
    assertFresh(good, child, 'todo')
    assertNoRecoveryResidue(good)

    // NEGATIVE (layer 0, index publisher): a child whose Source Ref names a
    // foreign parent while the Origin bullet points at the delegating parent
    // cannot even be published into the INDEX — the canonical regen publisher
    // fails loud on the TASK_SOURCE_LINEAGE_MISMATCH error finding.
    const forged = makeProject()
    const forgedChild = 'TASK_72_forged_ref'
    const forgedSource = taskSourceContract.testFoundationPrerequisite('TASK_99_someone_else', intentHash)
    writeArtifact(forged, 'backlog', forgedChild, foundationBacklogDoc(forgedChild, parent, forgedSource))
    const forgedRegen = spawnSync(process.env.PYTHON || 'python3', [REGEN], {
      cwd: forged.root, env: envFor(forged), encoding: 'utf8', maxBuffer: 8 * 1024 * 1024, timeout: 60000,
    })
    assert.notEqual(forgedRegen.status, 0, 'the index publisher must refuse forged foundation provenance')
    assert.match(String(forgedRegen.stderr) + String(forgedRegen.stdout), /TASK_SOURCE_LINEAGE_MISMATCH/)

    // NEGATIVE (layer 1, admission on live bytes): swap the published clean
    // bytes for forged provenance AFTER the index publication — the promote
    // admission re-reads live bytes and refuses BEFORE authority/mutation
    // work (INDEX_* staleness alone never admits a blocked doc), keeping the
    // inner typed ref gate pure defense-in-depth.
    writeArtifact(forged, 'backlog', forgedChild, foundationBacklogDoc(forgedChild, parent,
      taskSourceContract.testFoundationPrerequisite(parent, intentHash)))
    runRegen(forged)
    writeArtifact(forged, 'backlog', forgedChild, foundationBacklogDoc(forgedChild, parent, forgedSource))
    canonicalTaskLock(forged, forgedChild, 'task-prep', sessionId)
    const forgedResult = run(forged, [
      'promote', '--stem', forgedChild, '--authority-stem', parent,
      '--input', writeInput(forged, 'forged-ref-todo.md', foundationTodoDoc(forgedChild, parent, forgedSource)),
      '--source-revision', 'sha256:' + '0'.repeat(64),
    ], { TASK_TRANSITION_TEST_SESSION_ID: sessionId })
    parseFailure(forgedResult, 1, 'TRANSITION_PRECONDITION_FAILED')
    assert.match(String(forgedResult.stderr), /TASK_SOURCE_LINEAGE_MISMATCH/,
      'the refusal names the forged-provenance blocker')
    const forgedAfter = validate(forged, { stem: forgedChild, checkIndex: false })
    assert.equal(forgedAfter.observedState, 'backlog', 'the forged child never leaves backlog')

    // NEGATIVE (authority scope): a findings-clean foundation child of a
    // DIFFERENT parent cannot be promoted under this parent's authority.
    const foreign = makeProject()
    const foreignChild = 'TASK_73_foreign_parent'
    const foreignSource = taskSourceContract.testFoundationPrerequisite('TASK_99_someone_else', intentHash)
    writeArtifact(foreign, 'backlog', foreignChild, foundationBacklogDoc(foreignChild, 'TASK_99_someone_else', foreignSource))
    runRegen(foreign)
    const foreignRevision = revision(foreign, foreignChild, 'backlog')
    canonicalTaskLock(foreign, foreignChild, 'task-prep', sessionId)
    parseFailure(run(foreign, [
      'promote', '--stem', foreignChild, '--authority-stem', parent,
      '--input', writeInput(foreign, 'foreign-parent-todo.md', foundationTodoDoc(foreignChild, 'TASK_99_someone_else', foreignSource)),
      '--source-revision', foreignRevision,
    ], { TASK_TRANSITION_TEST_SESSION_ID: sessionId }), 2, 'WRITER_AUTHORITY_SCOPE_MISMATCH')
    assertFresh(foreign, foreignChild, 'backlog')
  })

  check('tampered private source backup blocks recovery and preserves the sole copy', () => {
    const project = makeProject()
    const stem = 'TASK_63_tampered_backup'
    writeArtifact(project, 'backlog', stem, backlogDoc(stem, 'Original sole copy'))
    runRegen(project)
    const sourceRevision = revision(project, stem, 'backlog')
    const input = writeInput(project, 'tampered-backup-todo.md', todoDoc(stem))
    const crashed = run(project, [
      'promote', '--stem', stem, '--input', input, '--source-revision', sourceRevision,
    ], { TASK_TRANSITION_TEST_CRASH_AFTER_DETACH: '1' })
    assert.equal(crashed.status, 86, crashed.stderr + crashed.stdout)
    const transitionDir = join(project.root, 'orchestrator', '.cache', 'tasks', 'transitions')
    const markerName = readdirSync(transitionDir).find((name) => /^tr-[a-f0-9]{36}\.json$/.test(name))
    const marker = JSON.parse(readFileSync(join(transitionDir, markerName), 'utf8'))
    writeFileSync(marker.detached.backlog.backup, backlogDoc(stem, 'Forged replacement'))

    parseFailure(run(project, ['recover', '--stem', stem]), 1, 'TRANSITION_BACKUP_CHANGED')
    assert.equal(existsSync(artifact(project, 'backlog', stem)), false)
    assert.equal(readFileSync(marker.detached.backlog.backup, 'utf8'), backlogDoc(stem, 'Forged replacement'))
  })

  check('missing reopen history blocks committed cleanup while preserving the done backup', () => {
    const project = makeProject()
    const stem = 'TASK_64_reopen_history_fence'
    writeArtifact(project, 'done', stem, doneDoc(stem))
    runRegen(project)
    const sourceRevision = revision(project, stem, 'done')
    parseFailure(run(project, [
      'reopen', '--stem', stem, '--source-revision', sourceRevision,
    ], { TASK_TRANSITION_TEST_FAIL_CLEANUP: '1' }), 4, 'TRANSITION_COMMITTED_RECOVERY_REQUIRED')
    const transitionDir = join(project.root, 'orchestrator', '.cache', 'tasks', 'transitions')
    const markerName = readdirSync(transitionDir).find((name) => /^tr-[a-f0-9]{36}\.json$/.test(name))
    const marker = JSON.parse(readFileSync(join(transitionDir, markerName), 'utf8'))
    unlinkSync(marker.history.path)

    parseFailure(run(project, ['recover', '--stem', stem]), 1, 'REOPEN_HISTORY_CHANGED')
    assert.equal(existsSync(marker.detached.done.backup), true,
      'the original done generation must remain until immutable history is proven')
    assert.equal(existsSync(artifact(project, 'todo', stem)), true)
  })

  check('crash while releasing the transition guard is recoverable and fences the next operation', () => {
    const project = makeProject()
    const stem = 'TASK_35_guard_release_crash'
    const backlog = backlogDoc(stem, 'Guard release crash source')
    const firstPending = pendingDoc(stem)
    writeArtifact(project, 'backlog', stem, backlog)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'backlog')
    const firstInput = writeInput(project, 'guard-release-first.md', firstPending)

    const crashed = run(project, [
      'ask', '--stem', stem, '--input', firstInput, '--source-revision', sourceRevision,
    ], { TASK_TRANSITION_TEST_CRASH_AFTER_GUARD_DETACH: '1' })
    assert.equal(crashed.status, 88, crashed.stderr + crashed.stdout)
    assert.equal(readFileSync(artifact(project, 'backlog', stem), 'utf8'), backlog)
    assert.equal(readFileSync(artifact(project, 'pending', stem), 'utf8'), firstPending)
    assertFresh(project, stem, 'pending')

    const guardDir = join(project.root, 'orchestrator', '.cache', 'tasks', 'transitions', '.guards')
    const retained = readdirSync(guardDir).filter((name) => name.startsWith('.' + stem + '.guard-recovery-'))
    assert.equal(retained.length, 1, 'crashed release must retain exactly one private guard generation')
    const publishedGuard = join(guardDir, stem + '.json')
    assert.equal(existsSync(publishedGuard), false)
    // Emulate a second crash after no-clobber restoration linked the same
    // generation back to its public name but before removing the private name.
    linkSync(join(guardDir, retained[0]), publishedGuard)

    const secondPending = pendingDoc(stem, { round: 2, question: 2, gapCount: 1 })
    const secondInput = writeInput(project, 'guard-release-second.md', secondPending)
    const pendingRevision = revision(project, stem, 'pending')
    parseFailure(run(project, [
      'ask', '--stem', stem, '--input', secondInput, '--source-revision', pendingRevision,
    ]), 1, 'TRANSITION_GUARD_RECOVERY_REQUIRED')
    assert.equal(readFileSync(artifact(project, 'pending', stem), 'utf8'), firstPending)

    const recovered = parseSuccess(run(project, ['recover', '--stem', stem]))
    assert.deepEqual(recovered.recovered, [])
    assert.deepEqual(readdirSync(guardDir).filter((name) => name.includes(stem)), [])

    const resumed = parseSuccess(run(project, [
      'ask', '--stem', stem, '--input', secondInput, '--source-revision', pendingRevision,
    ]))
    assert.equal(resumed.state, 'pending')
    assert.equal(readFileSync(artifact(project, 'pending', stem), 'utf8'), secondPending)
    assertFresh(project, stem, 'pending')
    assertNoRecoveryResidue(project)
  })

  check('forged or multiple private guard generations fail closed without touching task state', () => {
    const project = makeProject()
    const stem = 'TASK_36_guard_recovery_forgery'
    const backlog = backlogDoc(stem, 'Guard recovery forgery source')
    writeArtifact(project, 'backlog', stem, backlog)
    runRegen(project)
    const guardDir = join(project.root, 'orchestrator', '.cache', 'tasks', 'transitions', '.guards')
    mkdirSync(guardDir, { recursive: true })
    const first = join(guardDir, '.' + stem + '.guard-recovery-' + 'a'.repeat(36))
    writeFileSync(first, '{}\n')

    parseFailure(run(project, ['recover', '--stem', stem]), 1, 'TRANSITION_GUARD_INVALID')
    assert.equal(readFileSync(first, 'utf8'), '{}\n')
    assert.equal(readFileSync(artifact(project, 'backlog', stem), 'utf8'), backlog)
    assertFresh(project, stem, 'backlog')

    const second = join(guardDir, '.' + stem + '.guard-recovery-' + 'b'.repeat(36))
    writeFileSync(second, '{}\n')
    parseFailure(run(project, ['recover', '--stem', stem]), 1, 'TRANSITION_GUARD_RECOVERY_AMBIGUOUS')
    assert.equal(readFileSync(first, 'utf8'), '{}\n')
    assert.equal(readFileSync(second, 'utf8'), '{}\n')
    assert.equal(readFileSync(artifact(project, 'backlog', stem), 'utf8'), backlog)
    assertFresh(project, stem, 'backlog')
  })

  check('invalid pending proposal is rejected before altering its backlog source', () => {
    const project = makeProject()
    const stem = 'TASK_40_invalid_ask'
    const backlog = backlogDoc(stem)
    writeArtifact(project, 'backlog', stem, backlog)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'backlog')
    const invalidInput = writeInput(project, 'invalid-pending.md', 'not pending frontmatter\n')

    parseFailure(run(project, [
      'ask', '--stem', stem, '--input', invalidInput, '--source-revision', sourceRevision,
    ]), 1, 'PENDING_WRITE_CONTRACT_INVALID')
    assert.equal(readFileSync(artifact(project, 'backlog', stem), 'utf8'), backlog)
    assert.equal(existsSync(artifact(project, 'pending', stem)), false)
    assert.equal(invalidInput.bytes.toString('utf8'), 'not pending frontmatter\n')
    assertFresh(project, stem, 'backlog')
    assertNoRecoveryResidue(project)
  })

  check('invalid todo proposal restores both members of a pending source pair', () => {
    const project = makeProject()
    const stem = 'TASK_41_invalid_promote'
    const backlog = backlogDoc(stem)
    const pending = pendingDoc(stem)
    writeArtifact(project, 'backlog', stem, backlog)
    writeArtifact(project, 'pending', stem, pending)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'pending')
    const invalidInput = writeInput(project, 'invalid-todo.md', `# TASK 41 — Missing runnable sections\n`)

    parseFailure(run(project, [
      'promote', '--stem', stem, '--input', invalidInput, '--source-revision', sourceRevision,
    ]), 1, 'TRANSITION_POSTCONDITION_FAILED')
    assert.equal(readFileSync(artifact(project, 'backlog', stem), 'utf8'), backlog)
    assert.equal(readFileSync(artifact(project, 'pending', stem), 'utf8'), pending)
    assert.equal(existsSync(artifact(project, 'todo', stem)), false)
    assert.equal(invalidInput.bytes.toString('utf8'), `# TASK 41 — Missing runnable sections\n`)
    assertFresh(project, stem, 'pending')
    assertNoRecoveryResidue(project)
  })

  check('recovery rejects a forged detached path without touching either file', () => {
    const project = makeProject()
    const stem = 'TASK_42_forged_recovery'
    const backlog = backlogDoc(stem)
    writeArtifact(project, 'backlog', stem, backlog)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'backlog')
    const transitionDir = join(project.root, 'orchestrator', '.cache', 'tasks', 'transitions')
    mkdirSync(transitionDir, { recursive: true })
    const transactionId = 'tr-' + 'a'.repeat(36)
    const outside = join(project.root, 'must-not-move.txt')
    writeFileSync(outside, 'sentinel\n')
    const marker = markerBase({
      transactionId, operation: 'drop', stem, sourceState: 'backlog', sourceRevision,
      intendedHash: null, sourceArtifacts: { backlog: fileProof(artifact(project, 'backlog', stem)) },
    })
    marker.detached = { backlog: { source: artifact(project, 'backlog', stem), backup: outside, proof: null } }
    writeFileSync(join(transitionDir, transactionId + '.json'), JSON.stringify(marker, null, 2) + '\n')

    parseFailure(run(project, ['recover', '--stem', stem]), 1, 'TRANSITION_MARKER_INVALID')
    assert.equal(readFileSync(outside, 'utf8'), 'sentinel\n')
    assert.equal(readFileSync(artifact(project, 'backlog', stem), 'utf8'), backlog)
    assertFresh(project, stem, 'backlog')
  })

  check('stat-only source proofs are accepted only for oversized drop recovery', () => {
    const project = makeProject()
    const stem = 'TASK_42_stat_only_edit_forgery'
    const original = backlogDoc(stem)
    writeArtifact(project, 'backlog', stem, original)
    runRegen(project)
    const transactionId = 'tr-' + 'f'.repeat(36)
    const transitionDir = join(project.root, 'orchestrator', '.cache', 'tasks', 'transitions')
    mkdirSync(transitionDir, { recursive: true })
    const proof = metadataProof(artifact(project, 'backlog', stem))
    proof.size = 9 * 1024 * 1024
    const marker = markerBase({
      transactionId, operation: 'edit', stem, sourceState: 'backlog',
      sourceRevision: revision(project, stem, 'backlog'),
      intendedHash: core.sha256(Buffer.from(backlogDoc(stem, 'Never published'))),
      sourceArtifacts: { backlog: proof },
    })
    writeFileSync(join(transitionDir, transactionId + '.json'), JSON.stringify(marker, null, 2) + '\n')

    parseFailure(run(project, ['recover', '--stem', stem]), 1, 'TRANSITION_MARKER_INVALID')
    assert.equal(readFileSync(artifact(project, 'backlog', stem), 'utf8'), original)
    assertFresh(project, stem, 'backlog')
  })

  check('recovery never accepts a valid-looking target whose bytes changed after publication', () => {
    const project = makeProject()
    const stem = 'TASK_43_recovery_target_race'
    const original = backlogDoc(stem, 'Original')
    const intended = backlogDoc(stem, 'Intended')
    const replacement = backlogDoc(stem, 'Concurrent replacement')
    writeArtifact(project, 'backlog', stem, original)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'backlog')
    const transactionId = 'tr-' + 'b'.repeat(36)
    const transitionDir = join(project.root, 'orchestrator', '.cache', 'tasks', 'transitions')
    const privateDir = join(transitionDir, '.private', transactionId)
    mkdirSync(privateDir, { recursive: true })
    const sourceProof = fileProof(artifact(project, 'backlog', stem))
    const backup = join(privateDir, 'backlog.md')
    renameSync(artifact(project, 'backlog', stem), backup)
    const publication = join(privateDir, 'target-backlog.md')
    writeFileSync(publication, intended)
    linkSync(publication, artifact(project, 'backlog', stem))
    const publicationProof = fileProof(publication)
    const targetProof = fileProof(artifact(project, 'backlog', stem))
    unlinkSync(artifact(project, 'backlog', stem))
    writeArtifact(project, 'backlog', stem, replacement)
    const marker = markerBase({
      transactionId, operation: 'edit', stem, sourceState: 'backlog', sourceRevision,
      intendedHash: core.sha256(Buffer.from(intended)), sourceArtifacts: { backlog: sourceProof },
    })
    marker.phase = 'target-published'
    marker.detached = { backlog: { source: artifact(project, 'backlog', stem), backup, proof: fileProof(backup) } }
    marker.targetArtifact = {
      column: 'backlog', path: artifact(project, 'backlog', stem), publication,
      publicationProof, targetProof,
    }
    writeFileSync(join(transitionDir, transactionId + '.json'), JSON.stringify(marker, null, 2) + '\n')

    parseFailure(run(project, ['recover', '--stem', stem]), 1, 'RECOVERY_TARGET_CHANGED')
    assert.equal(readFileSync(artifact(project, 'backlog', stem), 'utf8'), replacement)
    assert.equal(readFileSync(backup, 'utf8'), original)
  })

  check('prepared same-state recovery rolls back without deleting the untouched source', () => {
    const project = makeProject()
    const stem = 'TASK_44_prepared_edit_recovery'
    const original = backlogDoc(stem, 'Untouched prepared source')
    writeArtifact(project, 'backlog', stem, original)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'backlog')
    const transactionId = 'tr-' + 'c'.repeat(36)
    const transitionDir = join(project.root, 'orchestrator', '.cache', 'tasks', 'transitions')
    mkdirSync(transitionDir, { recursive: true })
    const marker = markerBase({
      transactionId, operation: 'edit', stem, sourceState: 'backlog', sourceRevision,
      intendedHash: core.sha256(Buffer.from(backlogDoc(stem, 'Never published'))),
      sourceArtifacts: { backlog: fileProof(artifact(project, 'backlog', stem)) },
    })
    writeFileSync(join(transitionDir, transactionId + '.json'), JSON.stringify(marker, null, 2) + '\n')

    const recovered = parseSuccess(run(project, ['recover', '--stem', stem]))
    assert.deepEqual(recovered.recovered, [{ transactionId, resolution: 'rolled-back' }])
    assert.equal(readFileSync(artifact(project, 'backlog', stem), 'utf8'), original)
    assertFresh(project, stem, 'backlog')
    assertNoRecoveryResidue(project)
  })

  for (const [offset, stage] of ['after-replace-candidate', 'after-replace-wal', 'after-replace-detach', 'after-replace-publish'].entries()) {
    check(`marker replacement protocol recovers a hard crash at ${stage}`, () => {
      const project = makeProject()
      const stem = `TASK_${48 + offset}_marker_wal_${offset}`
      const backlog = backlogDoc(stem, 'Marker WAL crash source')
      const pending = pendingDoc(stem)
      writeArtifact(project, 'backlog', stem, backlog)
      runRegen(project)
      const sourceRevision = revision(project, stem, 'backlog')
      const input = writeInput(project, `marker-wal-${offset}.md`, pending)

      parseFailure(run(project, [
        'ask', '--stem', stem, '--input', input, '--source-revision', sourceRevision,
      ], { TASK_TRANSITION_TEST_FAIL_CLEANUP: '1' }), 4, 'TRANSITION_COMMITTED_RECOVERY_REQUIRED')
      const transitionDir = join(project.root, 'orchestrator', '.cache', 'tasks', 'transitions')
      const markerName = readdirSync(transitionDir).find((name) => /^tr-[a-f0-9]{36}\.json$/.test(name))
      assert.ok(markerName, 'committed transition must retain its canonical recovery marker')
      const target = join(transitionDir, markerName)
      const sentinel = join(project.root, `marker-wal-crash-${offset}`)
      const crashed = run(project, ['recover', '--stem', stem], {
        FINALIZE_FS_TEST_CRASH_STAGE: stage,
        FINALIZE_FS_TEST_CRASH_TARGET: target,
        FINALIZE_FS_TEST_CRASH_SENTINEL: sentinel,
        FINALIZE_FS_TEST_ROOT: project.root,
      })
      assert.equal(crashed.status, 88, crashed.stderr + crashed.stdout)
      assert.ok(existsSync(sentinel), 'hard-crash failpoint did not fire')
      const residue = readdirSync(transitionDir)
      if (stage === 'after-replace-candidate') {
        assert.ok(residue.includes(`.${markerName}.replace-reservation.json`),
          'durable pre-WAL reservation must survive the candidate crash')
        assert.equal(residue.includes(`.${markerName}.replace-wal.json`), false,
          'candidate failpoint must exercise the pre-WAL interval')
      } else {
        assert.ok(residue.includes(`.${markerName}.replace-wal.json`), 'durable replace WAL must survive the crash')
      }
      if (stage === 'after-replace-detach') {
        assert.equal(existsSync(target), false, 'fixture must exercise the missing-marker window')
        assert.ok(residue.some((name) => name.startsWith(`.${markerName}.replace-detached-`)))
      }

      const inspected = parseSuccess(run(project, ['inspect-integrity', '--stem', stem]), { requireOk: false })
      assert.ok(inspected.findings.some((item) => item.code === 'TRANSITION_MARKER_RECOVERY_REQUIRED'),
        'read-only integrity scan must make durable replace recovery visible')
      assert.ok(inspected.snapshotInputs.some((item) => item.kind === 'marker-replace'),
        'integrity snapshot must bind the exact private replacement generation')
      const recovered = parseSuccess(run(project, ['recover', '--stem', stem]))
      assert.equal(recovered.recovered.length, 1)
      assert.equal(recovered.recovered[0].resolution, 'completed-forward')
      assertFresh(project, stem, 'pending')
      assertNoRecoveryResidue(project)
    })
  }

  check('recovery rejects forged runtime-detached paths before moving any bytes', () => {
    const project = makeProject()
    const stem = 'TASK_45_forged_runtime_recovery'
    const original = backlogDoc(stem)
    writeArtifact(project, 'backlog', stem, original)
    runRegen(project)
    const sourceRevision = revision(project, stem, 'backlog')
    const transactionId = 'tr-' + 'd'.repeat(36)
    const transitionDir = join(project.root, 'orchestrator', '.cache', 'tasks', 'transitions')
    const privateDir = join(transitionDir, '.private', transactionId)
    mkdirSync(privateDir, { recursive: true })
    const outside = join(project.root, 'runtime-sentinel.txt')
    const backup = join(privateDir, 'runtime-lock.json')
    writeFileSync(outside, 'outside sentinel\n')
    writeFileSync(backup, 'forged backup\n')
    const marker = markerBase({
      transactionId, operation: 'drop', stem, sourceState: 'backlog', sourceRevision,
      intendedHash: null, sourceArtifacts: { backlog: fileProof(artifact(project, 'backlog', stem)) },
    })
    marker.runtimeDetached = {
      lock: { source: outside, backup, sourceProof: fileProof(outside), proof: fileProof(backup) },
    }
    writeFileSync(join(transitionDir, transactionId + '.json'), JSON.stringify(marker, null, 2) + '\n')

    parseFailure(run(project, ['recover', '--stem', stem]), 1, 'TRANSITION_MARKER_INVALID')
    assert.equal(readFileSync(outside, 'utf8'), 'outside sentinel\n')
    assert.equal(readFileSync(backup, 'utf8'), 'forged backup\n')
    assert.equal(readFileSync(artifact(project, 'backlog', stem), 'utf8'), original)
  })

  check('malformed transition-guard and lock-release recovery prefixes fail closed', () => {
    const project = makeProject()
    const stem = 'TASK_46_malformed_recovery_prefix'
    writeArtifact(project, 'backlog', stem, backlogDoc(stem))
    runRegen(project)
    const sourceRevision = revision(project, stem, 'backlog')
    const guards = join(project.root, 'orchestrator', '.cache', 'tasks', 'transitions', '.guards')
    mkdirSync(guards, { recursive: true })
    writeFileSync(join(guards, `.${stem}.guard-recovery-not-canonical`), 'unsafe\n')
    parseFailure(run(project, ['drop', '--stem', stem, '--source-revision', sourceRevision]), 1, 'TRANSITION_GUARD_RECOVERY_UNSAFE')
    unlinkSync(join(guards, `.${stem}.guard-recovery-not-canonical`))

    const locks = join(project.root, 'orchestrator', '.cache', 'tasks', 'locks')
    mkdirSync(locks, { recursive: true })
    writeFileSync(join(locks, `.${stem}.json.release-not-canonical`), 'unsafe\n')
    parseFailure(run(project, ['drop', '--stem', stem, '--source-revision', sourceRevision]), 1, 'LOCK_RELEASE_RECOVERY_UNSAFE')
    assertFresh(project, stem, 'backlog')
  })

  check('decimal stat proofs above Number.MAX_SAFE_INTEGER stay distinct in recovery markers', () => {
    const project = makeProject()
    const stem = 'TASK_47_lossless_decimal_proof'
    writeArtifact(project, 'backlog', stem, backlogDoc(stem))
    runRegen(project)
    const transactionId = 'tr-' + 'e'.repeat(36)
    const transitionDir = join(project.root, 'orchestrator', '.cache', 'tasks', 'transitions')
    mkdirSync(transitionDir, { recursive: true })
    const proof = fileProof(artifact(project, 'backlog', stem))
    proof.dev = '9007199254740993'
    proof.ino = '9007199254740995'
    const value = markerBase({
      transactionId, operation: 'edit', stem, sourceState: 'backlog',
      sourceRevision: revision(project, stem, 'backlog'),
      intendedHash: core.sha256(Buffer.from(backlogDoc(stem, 'unused'))),
      sourceArtifacts: { backlog: proof },
    })
    writeFileSync(join(transitionDir, transactionId + '.json'), JSON.stringify(value, null, 2) + '\n')
    const recovered = parseSuccess(run(project, ['recover', '--stem', stem]))
    assert.equal(recovered.recovered[0].resolution, 'rolled-back')
    assertFresh(project, stem, 'backlog')
  })

  check('replace WAL never overwrites a foreign canonical generation during recovery', () => {
    const project = makeProject()
    const directory = join(project.root, 'replace-race')
    mkdirSync(directory)
    const target = join(directory, 'marker.json')
    const replacement = join(directory, 'foreign.json')
    const sentinel = join(project.root, 'replace-race-fired')
    writeFileSync(target, 'owned old marker\n')
    writeFileSync(replacement, 'foreign canonical marker\n')
    const base = {
      version: 1, authorityRoot: project.root,
      canonicalRoot: join(TASKS_DIR, '..', '..'), fixture: true,
    }
    const observed = spawnSync(process.env.PYTHON || 'python3', [FS_BOUNDARY, 'fs-op'], {
      input: JSON.stringify({ ...base, action: 'stat', path: target }), encoding: 'utf8',
    })
    const proof = JSON.parse(observed.stdout).result.stat
    const replaced = spawnSync(process.env.PYTHON || 'python3', [FS_BOUNDARY, 'fs-op'], {
      input: JSON.stringify({
        ...base, action: 'replace', path: target, expected: proof,
        rawBase64: Buffer.from('intended new marker\n').toString('base64'), mode: 0o600, maxBytes: 1024,
      }),
      encoding: 'utf8',
      env: {
        ...process.env,
        FINALIZE_FS_TEST_STAGE: 'before-replace', FINALIZE_FS_TEST_TARGET: target,
        FINALIZE_FS_TEST_REPLACEMENT: replacement, FINALIZE_FS_TEST_ROOT: project.root,
        FINALIZE_FS_TEST_SENTINEL: sentinel,
      },
    })
    assert.equal(replaced.status, 0, replaced.stderr)
    const rejected = JSON.parse(replaced.stdout)
    assert.equal(rejected.ok, false, replaced.stdout)
    assert.equal(rejected.error.code, 'RECOVERY_REQUIRED')
    assert.equal(readFileSync(target, 'utf8'), 'foreign canonical marker\n')
    assert.equal(readFileSync(target + '.test-displaced', 'utf8'), 'owned old marker\n')
    assert.ok(readdirSync(directory).includes('.marker.json.replace-wal.json'))

    const recovery = spawnSync(process.env.PYTHON || 'python3', [FS_BOUNDARY, 'fs-op'], {
      input: JSON.stringify({
        ...base, action: 'recover-replaces', path: directory, maxEntries: 100, maxBytes: 1024,
      }),
      encoding: 'utf8',
    })
    assert.equal(recovery.status, 0, recovery.stderr)
    assert.equal(JSON.parse(recovery.stdout).error.code, 'RECOVERY_REQUIRED')
    assert.equal(readFileSync(target, 'utf8'), 'foreign canonical marker\n')
  })

  check('orphan or malformed replace artifacts fail closed without touching the target', () => {
    const project = makeProject()
    const directory = join(project.root, 'replace-orphan')
    mkdirSync(directory)
    const target = join(directory, 'marker.json')
    writeFileSync(target, 'canonical marker\n')
    const orphan = join(directory, `.marker.json.replace-candidate-${'a'.repeat(32)}`)
    writeFileSync(orphan, 'unowned candidate\n')
    const request = {
      version: 1, action: 'recover-replaces', authorityRoot: project.root, path: directory,
      maxEntries: 100, maxBytes: 1024, canonicalRoot: join(TASKS_DIR, '..', '..'), fixture: true,
    }
    const recovery = spawnSync(process.env.PYTHON || 'python3', [FS_BOUNDARY, 'fs-op'], {
      input: JSON.stringify(request), encoding: 'utf8',
    })
    assert.equal(recovery.status, 0, recovery.stderr)
    const envelope = JSON.parse(recovery.stdout)
    assert.equal(envelope.ok, false, recovery.stdout)
    assert.equal(envelope.error.code, 'RECOVERY_REQUIRED')
    assert.equal(readFileSync(target, 'utf8'), 'canonical marker\n')

    unlinkSync(orphan)
    const malformed = join(directory, '.marker.json.replace-unknown')
    writeFileSync(malformed, 'foreign private artifact\n')
    const malformedRecovery = spawnSync(process.env.PYTHON || 'python3', [FS_BOUNDARY, 'fs-op'], {
      input: JSON.stringify(request), encoding: 'utf8',
    })
    assert.equal(malformedRecovery.status, 0, malformedRecovery.stderr)
    assert.equal(JSON.parse(malformedRecovery.stdout).error.code, 'RECOVERY_REQUIRED')
    assert.equal(readFileSync(malformed, 'utf8'), 'foreign private artifact\n')
    assert.equal(readFileSync(target, 'utf8'), 'canonical marker\n')
  })

  check('replace recovery preserves a tampered private candidate and the old canonical marker', () => {
    const project = makeProject()
    const directory = join(project.root, 'replace-tamper')
    mkdirSync(directory)
    const target = join(directory, 'marker.json')
    const sentinel = join(project.root, 'replace-tamper-crash')
    writeFileSync(target, 'owned old marker\n')
    const base = {
      version: 1, authorityRoot: project.root,
      canonicalRoot: join(TASKS_DIR, '..', '..'), fixture: true,
    }
    const observed = spawnSync(process.env.PYTHON || 'python3', [FS_BOUNDARY, 'fs-op'], {
      input: JSON.stringify({ ...base, action: 'stat', path: target }), encoding: 'utf8',
    })
    const proof = JSON.parse(observed.stdout).result.stat
    const crashed = spawnSync(process.env.PYTHON || 'python3', [FS_BOUNDARY, 'fs-op'], {
      input: JSON.stringify({
        ...base, action: 'replace', path: target, expected: proof,
        rawBase64: Buffer.from('intended new marker\n').toString('base64'), mode: 0o600, maxBytes: 1024,
      }),
      encoding: 'utf8',
      env: {
        ...process.env,
        FINALIZE_FS_TEST_CRASH_STAGE: 'after-replace-candidate',
        FINALIZE_FS_TEST_CRASH_TARGET: target,
        FINALIZE_FS_TEST_CRASH_SENTINEL: sentinel,
        FINALIZE_FS_TEST_ROOT: project.root,
      },
    })
    assert.equal(crashed.status, 88, crashed.stderr + crashed.stdout)
    assert.ok(readdirSync(directory).includes('.marker.json.replace-reservation.json'))
    assert.equal(readdirSync(directory).includes('.marker.json.replace-wal.json'), false)
    const candidateName = readdirSync(directory).find((name) => name.startsWith('.marker.json.replace-candidate-'))
    assert.ok(candidateName)
    const foreignCandidate = join(directory, 'foreign-candidate.json')
    writeFileSync(foreignCandidate, 'foreign candidate bytes\n')
    renameSync(foreignCandidate, join(directory, candidateName))
    const recovery = spawnSync(process.env.PYTHON || 'python3', [FS_BOUNDARY, 'fs-op'], {
      input: JSON.stringify({
        ...base, action: 'recover-replaces', path: directory, maxEntries: 100, maxBytes: 1024,
      }),
      encoding: 'utf8',
    })
    assert.equal(recovery.status, 0, recovery.stderr)
    const envelope = JSON.parse(recovery.stdout)
    assert.equal(envelope.ok, false, recovery.stdout)
    assert.equal(envelope.error.code, 'RECOVERY_REQUIRED')
    assert.equal(readFileSync(target, 'utf8'), 'owned old marker\n')
    assert.equal(readFileSync(join(directory, candidateName), 'utf8'), 'foreign candidate bytes\n')
  })

  check('pre-WAL recovery discards an interrupted candidate prefix and preserves the canonical marker', () => {
    const project = makeProject()
    const directory = join(project.root, 'replace-partial-candidate')
    mkdirSync(directory)
    const target = join(directory, 'marker.json')
    const sentinel = join(project.root, 'replace-partial-candidate-crash')
    const intended = 'intended replacement payload with a durable suffix\n'
    writeFileSync(target, 'owned old marker\n')
    const base = {
      version: 1, authorityRoot: project.root,
      canonicalRoot: join(TASKS_DIR, '..', '..'), fixture: true,
    }
    const observed = spawnSync(process.env.PYTHON || 'python3', [FS_BOUNDARY, 'fs-op'], {
      input: JSON.stringify({ ...base, action: 'stat', path: target }), encoding: 'utf8',
    })
    const proof = JSON.parse(observed.stdout).result.stat
    const crashed = spawnSync(process.env.PYTHON || 'python3', [FS_BOUNDARY, 'fs-op'], {
      input: JSON.stringify({
        ...base, action: 'replace', path: target, expected: proof,
        rawBase64: Buffer.from(intended).toString('base64'), mode: 0o600, maxBytes: 1024,
      }),
      encoding: 'utf8',
      env: {
        ...process.env,
        FINALIZE_FS_TEST_CRASH_STAGE: 'after-replace-candidate',
        FINALIZE_FS_TEST_CRASH_TARGET: target,
        FINALIZE_FS_TEST_CRASH_SENTINEL: sentinel,
        FINALIZE_FS_TEST_ROOT: project.root,
      },
    })
    assert.equal(crashed.status, 88, crashed.stderr + crashed.stdout)
    const candidateName = readdirSync(directory).find((name) => name.startsWith('.marker.json.replace-candidate-'))
    assert.ok(candidateName)
    const candidate = join(directory, candidateName)
    writeFileSync(candidate, intended.slice(0, 11), { mode: 0o600 })

    const recovery = spawnSync(process.env.PYTHON || 'python3', [FS_BOUNDARY, 'fs-op'], {
      input: JSON.stringify({
        ...base, action: 'recover-replaces', path: directory, maxEntries: 100, maxBytes: 1024,
      }),
      encoding: 'utf8',
    })
    assert.equal(recovery.status, 0, recovery.stderr)
    const envelope = JSON.parse(recovery.stdout)
    assert.equal(envelope.ok, true, recovery.stdout)
    assert.deepEqual(envelope.result.recovered, [])
    assert.equal(readFileSync(target, 'utf8'), 'owned old marker\n')
    assert.equal(readdirSync(directory).some((name) => name.includes('.replace-')), false)
  })

  await checkAsync('directory recovery waits for an in-flight replacement WAL handoff', async () => {
    const project = makeProject()
    const directory = join(project.root, 'replace-mutex-serialization')
    mkdirSync(directory)
    const target = join(directory, 'marker.json')
    const sentinel = join(project.root, 'replace-mutex-pause-ready')
    const intended = 'serialized replacement payload\n'
    writeFileSync(target, 'owned old marker\n')
    const base = {
      version: 1, authorityRoot: project.root,
      canonicalRoot: join(TASKS_DIR, '..', '..'), fixture: true,
    }
    const observed = spawnSync(process.env.PYTHON || 'python3', [FS_BOUNDARY, 'fs-op'], {
      input: JSON.stringify({ ...base, action: 'stat', path: target }), encoding: 'utf8',
    })
    assert.equal(observed.status, 0, observed.stderr)
    const proof = JSON.parse(observed.stdout).result.stat
    const writer = spawnBoundary({
      ...base, action: 'replace', path: target, expected: proof,
      rawBase64: Buffer.from(intended).toString('base64'), mode: 0o600, maxBytes: 1024,
    }, {
      FINALIZE_FS_TEST_PAUSE_STAGE: 'after-replace-candidate',
      FINALIZE_FS_TEST_PAUSE_TARGET: target,
      FINALIZE_FS_TEST_PAUSE_SENTINEL: sentinel,
      FINALIZE_FS_TEST_PAUSE_MS: '900',
      FINALIZE_FS_TEST_ROOT: project.root,
    })
    await waitUntil(() => existsSync(sentinel), 'replace writer never reached its pre-WAL pause')
    const recovery = spawnBoundary({
      ...base, action: 'recover-replaces', path: directory, maxEntries: 100, maxBytes: 1024,
    })
    await new Promise((resolve) => setTimeout(resolve, 120))
    assert.equal(recovery.settled(), false,
      'global recovery must wait behind the directory replace mutex')
    const [writerResult, recoveryResult] = await Promise.all([writer.done, recovery.done])
    assert.equal(writerResult.status, 0, writerResult.stderr + writerResult.stdout)
    assert.equal(recoveryResult.status, 0, recoveryResult.stderr + recoveryResult.stdout)
    assert.equal(JSON.parse(writerResult.stdout).ok, true, writerResult.stdout)
    assert.equal(JSON.parse(recoveryResult.stdout).ok, true, recoveryResult.stdout)
    assert.equal(readFileSync(target, 'utf8'), intended)
    assert.equal(readdirSync(directory).some((name) => name.includes('.replace-')), false)
  })

  check('pre-WAL reservation discards a byte-identical new inode from its owned random namespace', () => {
    const project = makeProject()
    const directory = join(project.root, 'replace-reserved-namespace')
    mkdirSync(directory)
    const target = join(directory, 'marker.json')
    const sentinel = join(project.root, 'replace-reserved-namespace-crash')
    const intended = 'intended new marker\n'
    writeFileSync(target, 'owned old marker\n')
    const base = {
      version: 1, authorityRoot: project.root,
      canonicalRoot: join(TASKS_DIR, '..', '..'), fixture: true,
    }
    const observed = spawnSync(process.env.PYTHON || 'python3', [FS_BOUNDARY, 'fs-op'], {
      input: JSON.stringify({ ...base, action: 'stat', path: target }), encoding: 'utf8',
    })
    const proof = JSON.parse(observed.stdout).result.stat
    const crashed = spawnSync(process.env.PYTHON || 'python3', [FS_BOUNDARY, 'fs-op'], {
      input: JSON.stringify({
        ...base, action: 'replace', path: target, expected: proof,
        rawBase64: Buffer.from(intended).toString('base64'), mode: 0o600, maxBytes: 1024,
      }),
      encoding: 'utf8',
      env: {
        ...process.env,
        FINALIZE_FS_TEST_CRASH_STAGE: 'after-replace-candidate',
        FINALIZE_FS_TEST_CRASH_TARGET: target,
        FINALIZE_FS_TEST_CRASH_SENTINEL: sentinel,
        FINALIZE_FS_TEST_ROOT: project.root,
      },
    })
    assert.equal(crashed.status, 88, crashed.stderr + crashed.stdout)
    const candidateName = readdirSync(directory).find((name) => name.startsWith('.marker.json.replace-candidate-'))
    assert.ok(candidateName)
    const originalCandidateIno = lstatSync(join(directory, candidateName)).ino
    const claimant = join(directory, 'byte-identical-claimant.json')
    writeFileSync(claimant, intended, { mode: 0o600 })
    renameSync(claimant, join(directory, candidateName))
    assert.notEqual(lstatSync(join(directory, candidateName)).ino, originalCandidateIno,
      'fixture must replace the candidate with a distinct inode generation')

    const recovery = spawnSync(process.env.PYTHON || 'python3', [FS_BOUNDARY, 'fs-op'], {
      input: JSON.stringify({
        ...base, action: 'recover-replaces', path: directory, maxEntries: 100, maxBytes: 1024,
      }),
      encoding: 'utf8',
    })
    assert.equal(recovery.status, 0, recovery.stderr)
    const envelope = JSON.parse(recovery.stdout)
    assert.equal(envelope.ok, true, recovery.stdout)
    assert.deepEqual(envelope.result.recovered, [])
    assert.equal(readFileSync(target, 'utf8'), 'owned old marker\n',
      'pre-WAL recovery discards the private claimant but never publishes it')
    assert.equal(readdirSync(directory).some((name) => name.includes('.replace-')), false)
  })

  check('openat scan never observes a redirected ancestor through swap-away and swap-back', () => {
    const project = makeProject()
    const target = join(project.root, 'scan-root')
    const replacement = join(project.root, 'scan-replacement')
    mkdirSync(target)
    mkdirSync(replacement)
    writeFileSync(join(target, 'owned'), 'owned\n')
    writeFileSync(join(replacement, 'redirected'), 'redirected\n')
    const request = {
      version: 1, action: 'list', authorityRoot: project.root, path: target,
      maxEntries: 10, canonicalRoot: join(TASKS_DIR, '..', '..'), fixture: true,
    }
    const result = spawnSync(process.env.PYTHON || 'python3', [FS_BOUNDARY, 'fs-op'], {
      input: JSON.stringify(request), encoding: 'utf8', env: {
        ...process.env, TASK_FS_TEST_SWAP_PATH: target, TASK_FS_TEST_SWAP_WITH: replacement,
        TASK_FS_TEST_ROOT: project.root, TASK_FS_TEST_SWAP_RESTORE_BEFORE_VERIFY: '1',
      },
    })
    assert.equal(result.status, 0, result.stderr)
    const envelope = JSON.parse(result.stdout)
    assert.equal(envelope.ok, false, result.stdout)
    assert.equal(envelope.error.code, 'DIRECTORY_CHANGED')
    assert.deepEqual(readdirSync(target), ['owned'])
    assert.deepEqual(readdirSync(replacement), ['redirected'])
  })

  check('proof-bound empty-directory removal preserves a foreign replacement', () => {
    const project = makeProject()
    const target = join(project.root, 'owned-empty-directory')
    const replacement = join(project.root, 'foreign-directory')
    const sentinel = join(project.root, 'remove-directory-hook-fired')
    mkdirSync(target)
    mkdirSync(replacement)
    writeFileSync(join(replacement, 'foreign.txt'), 'must survive\n')
    const base = {
      version: 1, authorityRoot: project.root,
      canonicalRoot: join(TASKS_DIR, '..', '..'), fixture: true,
    }
    const observed = spawnSync(process.env.PYTHON || 'python3', [FS_BOUNDARY, 'fs-op'], {
      input: JSON.stringify({ ...base, action: 'stat', path: target }), encoding: 'utf8',
    })
    assert.equal(observed.status, 0, observed.stderr)
    const observedEnvelope = JSON.parse(observed.stdout)
    assert.equal(observedEnvelope.ok, true, observed.stdout)

    const removed = spawnSync(process.env.PYTHON || 'python3', [FS_BOUNDARY, 'fs-op'], {
      input: JSON.stringify({
        ...base, action: 'remove-empty-dir', path: target,
        expected: observedEnvelope.result.stat,
      }),
      encoding: 'utf8',
      env: {
        ...process.env,
        FINALIZE_FS_TEST_STAGE: 'before-remove-empty-dir',
        FINALIZE_FS_TEST_TARGET: target,
        FINALIZE_FS_TEST_REPLACEMENT: replacement,
        FINALIZE_FS_TEST_ROOT: project.root,
        FINALIZE_FS_TEST_SENTINEL: sentinel,
      },
    })
    assert.equal(removed.status, 0, removed.stderr)
    const removedEnvelope = JSON.parse(removed.stdout)
    assert.equal(removedEnvelope.ok, false, removed.stdout)
    assert.equal(removedEnvelope.error.code, 'DIRECTORY_CHANGED')
    assert.equal(readFileSync(join(target, 'foreign.txt'), 'utf8'), 'must survive\n')
    assert.ok(existsSync(target + '.test-displaced'), 'owned empty generation remains preserved as recovery proof')
  })

  check('stat-only oversized detach preserves a replacement introduced at the mutation boundary', () => {
    const project = makeProject()
    const directory = join(project.root, 'oversized-detach-race')
    mkdirSync(directory)
    const source = join(directory, 'source.md')
    const replacement = join(directory, 'replacement.md')
    const backup = join(directory, 'private-backup.md')
    const sentinel = join(project.root, 'oversized-detach-hook-fired')
    writeFileSync(source, '')
    writeFileSync(replacement, '')
    truncateSync(source, 40 * 1024 * 1024)
    truncateSync(replacement, 41 * 1024 * 1024)
    const sourceInode = lstatSync(source, { bigint: true }).ino
    const replacementInode = lstatSync(replacement, { bigint: true }).ino
    const base = {
      version: 1, authorityRoot: project.root,
      canonicalRoot: join(TASKS_DIR, '..', '..'), fixture: true,
    }
    const moved = spawnSync(process.env.PYTHON || 'python3', [FS_BOUNDARY, 'fs-op'], {
      input: JSON.stringify({
        ...base, action: 'move', source, target: backup,
        expected: metadataProof(source), maxBytes: 1,
      }),
      encoding: 'utf8',
      env: {
        ...process.env,
        FINALIZE_FS_TEST_STAGE: 'before-move',
        FINALIZE_FS_TEST_TARGET: source,
        FINALIZE_FS_TEST_REPLACEMENT: replacement,
        FINALIZE_FS_TEST_ROOT: project.root,
        FINALIZE_FS_TEST_SENTINEL: sentinel,
      },
    })
    assert.equal(moved.status, 0, moved.stderr)
    const envelope = JSON.parse(moved.stdout)
    assert.equal(envelope.ok, false, moved.stdout)
    assert.equal(envelope.error.code, 'ENTRY_CHANGED')
    assert.equal(lstatSync(source, { bigint: true }).ino, replacementInode)
    assert.equal(lstatSync(source + '.test-displaced', { bigint: true }).ino, sourceInode)
    assert.equal(existsSync(backup), false)
  })

  if (failures.length) {
    console.error(`${failures.length} of ${checks} transition helper checks failed.`)
    process.exitCode = 1
  } else {
    console.log(`All transition helper tests passed (${checks} checks).`)
  }
} finally {
  for (const root of roots.reverse()) rmSync(root, { recursive: true, force: true, maxRetries: 3, retryDelay: 20 })
}
