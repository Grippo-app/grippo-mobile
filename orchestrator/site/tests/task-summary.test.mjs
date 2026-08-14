import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { test } from 'node:test'

const require = createRequire(import.meta.url)
const taskSummary = require('../server/task-summary.js')
const primaryAction = require('../server/task-primary-action.js')
const taskBlockers = require('../server/task-blockers.js')
const taskActions = require('../server/task-actions.js')
const taskActionPromptPreview = require('../server/task-action-prompt-preview.js')
const taskIndexSource = require('../server/task-source.js')
const taskSource = require('../../tasks/task-source-contract.cjs')
const taskStateCore = require('../../tasks/task-state-core.cjs')

const AT = '2026-07-18T10:00:00Z'
const REV = 'sha256:' + 'a'.repeat(64)

function origin(kind, type, ref) {
  return { kind, type, ref, fingerprint: taskSource.sha256(kind + '\0' + type + '\0' + ref) }
}

function row(stem, state, patch = {}) {
  return {
    stem,
    title: patch.title || stem,
    state,
    sourceRevision: patch.sourceRevision || REV,
    createdAt: AT,
    doneAt: state === 'done' ? AT : null,
    origin: patch.origin === undefined ? origin('manual', 'manual', 'intent-' + stem) : patch.origin,
    dependsOn: patch.dependsOn || [],
    splitFrom: patch.splitFrom || null,
    outcomeStatus: state === 'done' ? (patch.outcomeStatus || 'completed') : null,
    questionsCount: state === 'pending' ? (patch.questionsCount ?? 1) : null,
    round: state === 'pending' ? (patch.round ?? 1) : null,
  }
}

function fixture() {
  const index = {
    version: 2,
    generatedAt: AT,
    backlog: [row('TASK_1_manual', 'backlog')],
    pending: [row('TASK_2_follow_up', 'pending', {
      origin: origin('follow-up', 'task-split', 'TASK_1_manual'),
      splitFrom: 'TASK_1_manual',
    })],
    todo: [
      row('TASK_3_ready', 'todo', { dependsOn: ['TASK_4_done'] }),
      row('TASK_5_blocked', 'todo', { dependsOn: ['TASK_99_missing'] }),
    ],
    done: [row('TASK_4_done', 'done')],
  }
  const validation = { indexStatus: 'fresh', findings: [], _model: null }
  const snapshot = {
    runnerActive: true,
    progress: { setupDone: true, inProgress: [], finalizations: [], requests: [], shallowIntake: {} },
    sessions: {}, screensCache: {}, reviewerConfig: { state: 'valid' }, status: { worker: {} },
  }
  const canonicalRows = Object.fromEntries(
    ['backlog', 'pending', 'todo', 'done'].flatMap((column) => index[column]).map((item) => [item.stem, item])
  )
  return {
    indexRead: { value: index, revision: 'sha256:' + 'b'.repeat(64) },
    canonicalRows,
    validation,
    validationReceipts: [],
    snapshot,
    sourceAvailability: {
      design: { ['cmp-' + '1'.repeat(24)]: true },
      coverage: { getProfile: true },
      drift: {},
    },
    latestReader: () => ({ event: null, truncated: false }),
  }
}

function flatten(summary) {
  return ['backlog', 'pending', 'todo', 'done'].flatMap((column) => summary.columns[column])
}

function actionRequest(stem, action, confirmation = null) {
  return {
    stem, actionId: action.id, actionRevision: action.actionRevision,
    action: action.kind, expectedState: action.expectedState,
    expectedSourceRevision: action.expectedSourceRevision,
    checkpointId: action.checkpointId, confirmation,
    confirmationToken: null,
    answers: null,
    questionRound: null,
    expectedQuestionsRevision: null,
    liveSessionId: action.liveSessionId || null,
    expectedSessionRevision: action.expectedSessionRevision || null,
    idempotencyKey: 'test:' + stem + ':' + action.id,
  }
}

test('summary projects dependencies, provenance, blockers and one primary action server-side', () => {
  const summary = taskSummary.build({ limit: 100 }, fixture())
  const rows = Object.fromEntries(flatten(summary).map((item) => [item.stem, item]))
  assert.equal(summary.schemaVersion, 1)
  assert.equal(summary.indexSchemaVersion, 2)
  assert.equal(summary.total, 5)
  assert.equal(rows.TASK_3_ready.dependencySummary.blockedCount, 0)
  assert.equal(rows.TASK_3_ready.primaryAction.kind, 'run')
  assert.equal(rows.TASK_5_blocked.primaryBlocker.kind, 'dependency-missing')
  assert.equal(rows.TASK_5_blocked.primaryAction.kind, 'resolve-blocker')
  assert.deepEqual(rows.TASK_2_follow_up.sourceTarget, {
    panel: 'board', entityId: 'TASK_1_manual', availability: 'available',
  })
  for (const item of Object.values(rows)) {
    assert.ok(item.primaryAction)
    assert.match(item.primaryAction.actionRevision, /^sha256:[a-f0-9]{64}$/)
    assert.equal(Object.hasOwn(item, 'absolutePath'), false)
  }
})

test('filters, stable cursor pagination and context filtering are server-owned', () => {
  const deps = fixture()
  const first = taskSummary.build({ limit: 1, sort: 'number' }, deps)
  assert.equal(flatten(first).length, 1)
  assert.ok(first.nextCursor)
  const second = taskSummary.build({ limit: 1, sort: 'number', cursor: first.nextCursor }, deps)
  assert.equal(flatten(second).length, 1)
  assert.notEqual(flatten(first)[0].stem, flatten(second)[0].stem)
  assert.throws(() => taskSummary.build({ limit: 1, sort: 'recent', cursor: first.nextCursor }, deps),
    (error) => error && error.code === 'task-summary-cursor-stale')
  const contextual = taskSummary.build({ context: 'follow-up', limit: 100 }, deps)
  assert.deepEqual(flatten(contextual).map((item) => item.stem), ['TASK_2_follow_up'])
  const blocked = taskSummary.build({ blocker: 'blocked', limit: 100 }, deps)
  assert.deepEqual(flatten(blocked).map((item) => item.stem), ['TASK_5_blocked'])
  const dependencyBlocked = taskSummary.build({ dependency: 'blocked', limit: 100 }, deps)
  assert.deepEqual(flatten(dependencyBlocked).map((item) => item.stem), ['TASK_5_blocked'])
  const dependencySatisfied = taskSummary.build({ dependency: 'satisfied', limit: 100 }, deps)
  assert.deepEqual(flatten(dependencySatisfied).map((item) => item.stem), ['TASK_3_ready'])
  const dependencyNone = taskSummary.build({ column: 'backlog', dependency: 'none', limit: 100 }, deps)
  assert.deepEqual(flatten(dependencyNone).map((item) => item.stem), ['TASK_1_manual'])
})

test('live terminal tasks are pinned within their own column under every sort mode', () => {
  const deps = fixture()
  const running = row('TASK_8_running', 'todo')
  deps.indexRead.value.todo.push(running)
  deps.canonicalRows[running.stem] = running
  deps.snapshot.sessions['task:' + running.stem] = {
    running: true, awaitingTurn: true, askedThisTurn: false, inputReady: false,
    // Older than the idle task metadata on purpose: `recent` must still pin
    // the active execution before applying time order within each rank.
    startedAt: '2026-07-18T09:00:00Z',
  }

  for (const mode of ['board', 'recent', 'number']) {
    const todo = taskSummary.build({ limit: 100, sort: mode }, deps).columns.todo
    assert.equal(todo[0].stem, running.stem, mode + ' must pin the live terminal card')
    assert.equal(todo[0].runtimeStatus.active, true)
    assert.equal(todo[0].primaryAction.behavior, 'open-terminal')
    assert.deepEqual(todo.slice(1).map((item) => item.stem).sort(), ['TASK_3_ready', 'TASK_5_blocked'])
  }
})

test('needs-action filtering distinguishes a working turn from an explicit question', () => {
  const working = fixture()
  working.snapshot.sessions['task:TASK_3_ready'] = {
    running: true, awaitingTurn: true, askedThisTurn: false, inputReady: false,
    startedAt: '2026-07-18T11:59:00Z',
  }
  const workingStems = flatten(taskSummary.build({ needsAction: true, limit: 100 }, working))
    .map((item) => item.stem)
  assert.equal(workingStems.includes('TASK_3_ready'), false)

  const waiting = fixture()
  waiting.snapshot.sessions['task:TASK_3_ready'] = {
    running: true, awaitingTurn: false, askedThisTurn: true, inputReady: true,
    startedAt: '2026-07-18T12:00:00Z',
  }
  const waitingStems = flatten(taskSummary.build({ needsAction: true, limit: 100 }, waiting))
    .map((item) => item.stem)
  assert.equal(waitingStems.includes('TASK_3_ready'), true)
})

test('source targets are resolved server-side for Design, API, Follow-up and Architecture', () => {
  const deps = fixture()
  const entity = 'cmp-' + '1'.repeat(24)
  deps.indexRead.value.backlog[0].origin = origin('figma', 'design-finding', 'design:component:' + entity + ':component-drift')
  deps.indexRead.value.todo[0].origin = origin('api', 'api-missing', 'getProfile')
  deps.indexRead.value.todo[1].origin = origin('manual', 'architecture-finding', 'arch-finding-7')
  const rows = Object.fromEntries(flatten(taskSummary.build({ limit: 100 }, deps)).map((item) => [item.stem, item]))
  assert.deepEqual(rows.TASK_1_manual.sourceTarget, { panel: 'design', entityId: entity, availability: 'available' })
  assert.deepEqual(rows.TASK_3_ready.sourceTarget, { panel: 'api', entityId: 'coverage:getProfile', availability: 'available' })
  assert.deepEqual(rows.TASK_2_follow_up.sourceTarget, { panel: 'board', entityId: 'TASK_1_manual', availability: 'available' })
  assert.deepEqual(rows.TASK_5_blocked.sourceTarget, { panel: 'archmap', entityId: 'arch-finding-7', availability: 'missing' })
  assert.deepEqual(taskSummary.sourceTarget(
    origin('api', 'api-work-package', 'api:package:pkg-' + 'a'.repeat(24)),
    {},
    {},
  ), {
    panel: 'api',
    entityId: 'api:package:pkg-' + 'a'.repeat(24),
    availability: 'available',
  })

  deps.sourceAvailability.design = {}
  deps.sourceAvailability.coverage = {}
  const staleRows = Object.fromEntries(flatten(taskSummary.build({ limit: 100 }, deps)).map((item) => [item.stem, item]))
  assert.equal(staleRows.TASK_1_manual.sourceTarget.availability, 'missing')
  assert.equal(staleRows.TASK_3_ready.sourceTarget.availability, 'missing')
})

test('stale or missing INDEX rows are reconciled from canonical task artifacts', () => {
  const indexed = fixture().indexRead.value
  const moved = row('TASK_3_ready', 'done', {
    title: 'Canonical title', outcomeStatus: 'completed-with-caveats',
    dependsOn: ['TASK_4_done'], origin: origin('api', 'api-missing', 'getProfile'),
  })
  const unindexed = row('TASK_7_unindexed', 'todo')
  const columns = taskSummary.resilientRows(indexed, {
    TASK_3_ready: moved,
    TASK_7_unindexed: unindexed,
  })
  assert.deepEqual(columns.todo.map((item) => item.stem), ['TASK_7_unindexed'])
  assert.deepEqual(columns.done.map((item) => item.stem), ['TASK_3_ready'])
})

test('the shared INDEX row contract rejects task numbers outside the safe integer range', () => {
  assert.equal(taskIndexSource.validIndexRow(
    row('TASK_9007199254740992_unsafe', 'todo'), 'todo'), false)
})

test('the shared INDEX row contract accepts only current provenance, references, and Outcome states', () => {
  for (const status of ['completed', 'completed-with-caveats', 'partially-completed']) {
    assert.equal(taskIndexSource.validIndexRow(
      row('TASK_8_current_outcome', 'done', { outcomeStatus: status }), 'done'), true)
  }
  assert.equal(taskIndexSource.validIndexRow(
    row('TASK_8_old_outcome', 'done', { outcomeStatus: 'blocked' }), 'done'), false)

  const missingDoneOutcome = row('TASK_8_missing_outcome', 'done')
  missingDoneOutcome.outcomeStatus = null
  assert.equal(taskIndexSource.validIndexRow(missingDoneOutcome, 'done'), false)

  const leakedTodoOutcome = row('TASK_8_leaked_outcome', 'todo')
  leakedTodoOutcome.outcomeStatus = 'completed'
  assert.equal(taskIndexSource.validIndexRow(leakedTodoOutcome, 'todo'), false)
  assert.equal(taskIndexSource.validIndexRow(
    row('TASK_8_missing_source', 'todo', { origin: null }), 'todo'), false)
  assert.equal(taskIndexSource.validIndexRow(
    row('TASK_8_unsafe_dependency', 'todo', { dependsOn: ['TASK_9007199254740992_unsafe'] }), 'todo'), false)
})

test('summary keeps a task visible while canonical Source is temporarily incomplete', () => {
  const deps = fixture()
  const invalid = row('TASK_8_missing_source', 'todo', { origin: null })
  deps.indexRead.value.todo.push(invalid)
  deps.canonicalRows[invalid.stem] = invalid
  const projected = taskSummary.single(invalid.stem, deps).task
  assert.equal(projected.origin, null)
  assert.equal(projected.stem, invalid.stem)
})

test('summary uses the canonical stem as a bounded display fallback for a missing title', () => {
  const deps = fixture()
  deps.canonicalRows.TASK_1_manual = { ...deps.canonicalRows.TASK_1_manual, title: '' }
  assert.equal(taskSummary.single('TASK_1_manual', deps).task.title, 'TASK_1_manual')
})

test('one malformed task is marked locally while healthy cards remain executable and deletable', () => {
  const deps = fixture()
  deps.validation.ok = false
  deps.validation.indexStatus = 'stale'
  deps.validation.findings = [
    {
      code: 'TASK_TITLE_EMPTY',
      severity: 'error',
      stem: 'TASK_5_blocked',
      paths: ['orchestrator/tasks/todo/TASK_5_blocked.md'],
    },
    {
      code: 'INDEX_STALE',
      severity: 'error',
      stem: null,
      paths: ['orchestrator/tasks/INDEX.json'],
    },
    {
      code: 'TASK_NUMBER_CONFLICT',
      severity: 'blocker',
      stem: null,
      paths: [
        'orchestrator/tasks/backlog/TASK_1_manual.md',
        'orchestrator/tasks/todo/TASK_5_blocked.md',
      ],
      details: { stems: ['TASK_1_manual', 'TASK_5_blocked'] },
    },
  ]
  const rows = Object.fromEntries(
    flatten(taskSummary.build({ limit: 100 }, deps)).map((item) => [item.stem, item])
  )
  assert.equal(rows.TASK_5_blocked.taskHealth.severity, 'error')
  assert.equal(rows.TASK_5_blocked.primaryBlocker.kind, 'task-integrity')
  assert.equal(rows.TASK_5_blocked.secondaryActions.find((item) => item.kind === 'drop').enabled, true)
  assert.equal(rows.TASK_1_manual.taskHealth.severity, 'error')
  assert.equal(rows.TASK_3_ready.taskHealth.severity, 'ok')
  assert.equal(rows.TASK_3_ready.primaryAction.kind, 'run')
  assert.equal(rows.TASK_3_ready.primaryAction.enabled, true)
})

test('drop admission tolerates repairable content/index findings but preserves ownership fences', () => {
  const target = 'TASK_7_broken'
  const base = {
    scope: target,
    observedState: 'todo',
    sourceRevision: REV,
    findings: [
      { code: 'TODO_REQUIRED_SECTION_MISSING', severity: 'error', stem: target },
      { code: 'INDEX_STALE', severity: 'error', stem: null },
    ],
  }
  assert.equal(taskStateCore.admissionForAction({ ...base, action: 'drop' }, target).ok, true)
  assert.equal(taskStateCore.admissionForAction({ ...base, action: 'run' }, target).ok, false)
  assert.equal(taskStateCore.dropAdmission(base, target).ok, true)
  assert.equal(taskStateCore.dropAdmission({
    ...base,
    findings: base.findings.concat({
      code: 'LOCK_INVALID', severity: 'blocker', stem: target,
    }),
  }, target).ok, false)
})

test('structurally corrupt task artifacts stay visible as one deletable diagnostic card', () => {
  const stem = 'TASK_7_corrupt'
  const group = {
    stem,
    number: 7,
    backlog: {
      absolutePath: '/fixture/backlog/' + stem + '.md',
      relPath: 'orchestrator/tasks/backlog/' + stem + '.md',
      readable: false,
      contentHash: 'sha256:' + '1'.repeat(64),
      stat: { mtimeMs: Date.parse(AT) },
    },
    pending: null,
    todo: {
      absolutePath: '/fixture/todo/' + stem + '.md',
      relPath: 'orchestrator/tasks/todo/' + stem + '.md',
      readable: true,
      contentHash: 'sha256:' + '2'.repeat(64),
      stat: { mtimeMs: Date.parse(AT) },
    },
    done: null,
    state: 'corrupt',
    revision: REV,
  }
  const deps = fixture()
  delete deps.canonicalRows
  deps.indexRead = null
  deps.validation = {
    ok: false,
    indexStatus: 'invalid',
    snapshotHash: 'sha256:' + '3'.repeat(64),
    findings: [{
      code: 'TASK_PRESENT_IN_MULTIPLE_STATES',
      severity: 'blocker',
      stem,
      paths: [group.backlog.relPath, group.todo.relPath],
    }],
    runtimeStats: { truncated: false },
    _model: {
      artifacts: new Map([[stem, group]]),
      metadata: new Map(),
    },
  }
  const summary = taskSummary.build({ limit: 100 }, deps)
  const cards = flatten(summary)
  assert.equal(cards.length, 1)
  assert.equal(cards[0].stem, stem)
  assert.equal(cards[0].state, 'corrupt')
  assert.equal(cards[0].taskHealth.severity, 'error')
  const drop = cards[0].secondaryActions.find((item) => item.kind === 'drop')
  assert.equal(drop.enabled, true)
  assert.equal(drop.expectedState, 'corrupt')
  const dropRequest = actionRequest(stem, drop, {
    sourceRevision: REV,
    impactHash: 'sha256:' + '4'.repeat(64),
    dependents: [],
  })
  assert.equal(taskActions.validateRequest(dropRequest).expectedState, 'corrupt')
  assert.equal(taskActions.validateRequest({ ...dropRequest, action: 'run' }), null)
})

test('primary action precedence is deterministic', () => {
  const base = { stem: 'TASK_7_priority', state: 'todo', sourceRevision: REV, blockers: [] }
  assert.equal(primaryAction.resolve({ ...base, blockers: [{ kind: 'task-integrity', severity: 'blocking', id: 'i' }], active: true }).kind, 'resolve-blocker')
  assert.equal(primaryAction.resolve({ ...base, finalization: { recoverable: true, recoveryRunning: false } }).kind, 'resume-finalization')
  assert.equal(primaryAction.resolve({
    ...base,
    active: true,
    finalization: { recoverable: true, recoveryRunning: true },
    blockers: [{ kind: 'finalization-required', severity: 'blocking', id: 'fin' }],
  }).kind, 'resolve-blocker')
  const revalidation = primaryAction.resolve({
    ...base,
    integration: { state: 'revalidation-required', actionable: false },
    active: true,
    questionsPending: true,
    terminalAvailable: false,
    blockers: [{ kind: 'awaiting-answer', severity: 'blocking', id: 'answer' }],
  })
  assert.equal(revalidation.kind, 'integrate')
  assert.equal(revalidation.labelKey, 'board.action.release_generation')
  assert.equal(revalidation.attentionRequired, true)
  assert.equal(primaryAction.resolve({
    ...base,
    integration: { state: 'revalidation-required', actionable: false },
    active: true,
    terminalAvailable: true,
  }).kind, 'open-run', 'a live terminal remains the owner until its session settles')
  assert.equal(primaryAction.resolve({ ...base, active: true }).kind, 'open-run')
  assert.equal(primaryAction.resolve({ ...base, liveAwaiting: true }).kind, 'continue-live')
  assert.equal(primaryAction.resolve({ ...base, active: true, liveAwaiting: true }).kind, 'continue-live')
  assert.deepEqual(primaryAction.resolve({ ...base, active: true }).target,
    { type: 'terminal', key: 'task:' + base.stem })
  assert.deepEqual(primaryAction.resolve({ ...base, active: true, liveAwaiting: true }).target,
    { type: 'task', stem: base.stem, section: 'questions' })
  assert.equal(primaryAction.resolve({ ...base, visualReview: true }).kind, 'review-result')
  assert.equal(primaryAction.resolve({ ...base, retryCheckpoint: { id: 'cp-1', phase: 'review' } }).kind, 'retry-phase')
  assert.equal(primaryAction.resolve({ ...base, retryCheckpoint: { id: 'cp-1', phase: 'review' } }).target.section, 'action')
  assert.equal(primaryAction.resolve({
    ...base, finalization: { recoverable: true, recoveryRunning: false },
  }).target.section, 'action')
  const stoppedRetry = primaryAction.resolve({
    ...base,
    retryCheckpoint: { id: 'cp-1', phase: 'review' },
    blockers: [{ kind: 'stopped-run', severity: 'blocking', id: 'stopped' }],
  })
  assert.equal(stoppedRetry.kind, 'resolve-blocker')
  assert.equal(stoppedRetry.behavior, 'open-details')
  assert.equal(stoppedRetry.target.section, 'advanced')
  assert.equal(primaryAction.secondary({
    ...base, active: false, primaryAction: stoppedRetry,
    blockers: [{ kind: 'stopped-run', severity: 'blocking', id: 'stopped' }],
  }).find((item) => item.kind === 'drop').enabled, true)
  const retryAction = primaryAction.resolve({ ...base, retryCheckpoint: { id: 'cp-2', phase: 'run' } })
  assert.equal(primaryAction.secondary({ ...base, active: false, primaryAction: retryAction })
    .some((item) => item.kind === 'copy-prompt'), true)
  const retryCopy = primaryAction.secondary({
    ...base, active: false, primaryAction: retryAction,
    blockers: [{ id: 'runner', kind: 'runner-unavailable', severity: 'warning' }],
  })
    .find((item) => item.kind === 'copy-prompt')
  assert.equal(retryCopy.labelKey, 'board.overflow.copy_retry_prompt')
  const prepareAction = primaryAction.resolve({ ...base, state: 'backlog' })
  assert.equal(primaryAction.secondary({ ...base, state: 'backlog', active: false, primaryAction: prepareAction })
    .some((item) => item.kind === 'copy-prompt'), true)
  const prepareCopy = primaryAction.secondary({
    ...base, state: 'backlog', active: false, primaryAction: prepareAction,
    blockers: [{ id: 'runner', kind: 'runner-unavailable', severity: 'warning' }],
  })
    .find((item) => item.kind === 'copy-prompt')
  assert.equal(prepareCopy.labelKey, 'board.overflow.copy_prepare_prompt')
  const firstFinalization = primaryAction.resolve({
    ...base, finalization: { recoverable: true, recoveryRunning: false }, finalizationRevision: 'marker-a',
  })
  const nextFinalization = primaryAction.resolve({
    ...base, finalization: { recoverable: true, recoveryRunning: false }, finalizationRevision: 'marker-b',
  })
  assert.notEqual(firstFinalization.actionRevision, nextFinalization.actionRevision)
  assert.notEqual(firstFinalization.id, nextFinalization.id)
  const disabledReopen = primaryAction.secondary({ ...base, state: 'done', active: true, primaryAction: firstFinalization })[0]
  const enabledReopen = primaryAction.secondary({ ...base, state: 'done', active: false, primaryAction: firstFinalization })[0]
  assert.notEqual(disabledReopen.actionRevision, enabledReopen.actionRevision)
  assert.notEqual(disabledReopen.id, enabledReopen.id)
})

test('the complete primary CTA matrix has stable kinds, labels, behaviors and targets', () => {
  const base = { stem: 'TASK_7_matrix', state: 'todo', sourceRevision: REV, blockers: [] }
  const cases = [
    [{ ...base, state: 'backlog' }, 'prepare', 'execute', 'task'],
    [{ ...base, state: 'backlog', intakeNeedsReview: true }, 'prepare', 'execute', 'task'],
    [{ ...base, state: 'pending' }, 'submit-answers', 'open-details', 'task'],
    [base, 'run', 'execute', 'task'],
    [{ ...base, state: 'done' }, 'review-result', 'open-details', 'task'],
    [{ ...base, state: 'done', validationPending: true }, 'validate-in-app', 'open-details', 'task'],
    [{ ...base, state: 'done', validationPending: true, appValidationTarget: {
      type: 'panel', panel: 'app-runner', entityId: base.stem, section: 'validation',
    } }, 'validate-in-app', 'open-panel', 'panel'],
    [{ ...base, visualReview: true }, 'review-result', 'open-details', 'task', 'board.action.open_visual_comparison'],
    [{ ...base, active: true }, 'open-run', 'open-terminal', 'terminal', 'board.action.open_terminal'],
    [{ ...base, liveAwaiting: true, liveSessionId: 'session-7',
      sessionRevision: REV, sessionInputReady: true },
    'continue-live', 'open-details', 'task', 'board.action.continue_live'],
    [{ ...base, retryCheckpoint: { id: 'cp-1', phase: 'review' } }, 'retry-phase', 'open-details', 'task'],
    [{ ...base, finalization: { recoverable: true, recoveryRunning: false } }, 'resume-finalization', 'open-details', 'task'],
    [{ ...base, integration: { state: 'revalidation-required', actionable: false },
      terminalAvailable: false },
    'integrate', 'execute', 'task', 'board.action.release_generation'],
    [{ ...base, active: true, finalization: { recoverable: true, recoveryRunning: true }, blockers: [
      { id: 'fin', kind: 'finalization-required', severity: 'blocking' },
    ] }, 'resolve-blocker', 'open-details', 'task', 'board.action.open_recovery'],
    [{ ...base, blockers: [{ id: 'val', kind: 'validation-required', severity: 'blocking' }] }, 'validate-in-app', 'open-details', 'task'],
    [{ ...base, state: 'pending', blockers: [{ id: 'answer', kind: 'awaiting-answer', severity: 'blocking' }] }, 'submit-answers', 'open-details', 'task'],
    [{ ...base, liveSessionId: 'session-7', sessionRevision: REV, sessionInputReady: true,
      blockers: [{ id: 'answer', kind: 'awaiting-answer', severity: 'blocking' }] },
    'continue-live', 'open-details', 'task', 'board.action.continue_live'],
    [{ ...base, blockers: [{ id: 'dep', kind: 'dependency-incomplete', severity: 'blocking', relatedTaskStem: 'TASK_4_done' }] }, 'resolve-blocker', 'open-details', 'task', 'board.action.review_dependencies'],
    [{ ...base, blockers: [{ id: 'integrity', kind: 'task-integrity', severity: 'blocking' }], active: true }, 'resolve-blocker', 'open-details', 'task', 'board.action.open_diagnostics'],
  ]
  for (const [context, kind, behavior, targetType, expectedLabelKey] of cases) {
    const action = primaryAction.resolve(context)
    assert.equal(action.kind, kind)
    assert.equal(action.labelKey, expectedLabelKey || 'board.action.' + kind.replaceAll('-', '_'))
    assert.equal(action.behavior, behavior)
    assert.equal(action.target.type, targetType)
    assert.deepEqual(Object.keys(action).sort(), [
      'actionRevision', 'attentionRequired', 'behavior', 'checkpointId', 'disabledReasonCode', 'enabled',
      'expectedSessionRevision', 'expectedSourceRevision', 'expectedState', 'id',
      'kind', 'labelKey', 'liveSessionId', 'requiresConfirmation', 'target',
    ].sort())
  }
  assert.equal(primaryAction.resolve(base).attentionRequired, false)
  assert.equal(primaryAction.resolve({ ...base, state: 'done' }).attentionRequired, false)
  const retry = primaryAction.resolve({ ...base, retryCheckpoint: { id: 'cp-1', phase: 'review' } })
  const finalization = primaryAction.resolve({
    ...base, finalization: { recoverable: true, recoveryRunning: false },
  })
  const live = primaryAction.resolve({
    ...base, liveAwaiting: true, liveSessionId: 'session-7',
    sessionRevision: REV, sessionInputReady: true,
  })
  const pending = primaryAction.resolve({ ...base, state: 'pending' })
  const revalidation = primaryAction.resolve({
    ...base, integration: { state: 'revalidation-required', actionable: false },
    terminalAvailable: false,
  })
  assert.deepEqual(retry.target, { type: 'task', stem: base.stem, section: 'action' })
  assert.deepEqual(finalization.target, { type: 'task', stem: base.stem, section: 'action' })
  assert.deepEqual(live.target, { type: 'task', stem: base.stem, section: 'questions' })
  for (const action of [retry, finalization, live, pending, revalidation]) {
    assert.equal(action.attentionRequired, true)
  }
})

test('blocker-directed primary actions open relevant context while runner absence remains queueable', () => {
  const base = { stem: 'TASK_7_targets', state: 'todo', sourceRevision: REV, blockers: [] }
  const invalidDesign = primaryAction.resolve({ ...base, blockers: [
    { id: 'figma-design', kind: 'figma-design-invalid', severity: 'blocking' },
  ] })
  assert.equal(invalidDesign.labelKey, 'board.action.fix_design')
  assert.deepEqual(invalidDesign.target, { type: 'task', stem: base.stem, section: 'artifacts' })

  const figma = primaryAction.resolve({ ...base, blockers: [
    { id: 'figma', kind: 'figma-screens-missing', severity: 'blocking' },
  ] })
  assert.equal(figma.labelKey, 'board.action.add_figma_screens')
  assert.deepEqual(figma.target, { type: 'task', stem: base.stem, section: 'artifacts' })

  const figmaReview = primaryAction.resolve({ ...base, blockers: [
    { id: 'figma-review', kind: 'figma-review-required', severity: 'blocking' },
  ] })
  assert.equal(figmaReview.labelKey, 'board.action.open_visual_comparison')
  assert.deepEqual(figmaReview.target, { type: 'task', stem: base.stem, section: 'artifacts' })

  const reviewer = primaryAction.resolve({ ...base, blockers: [
    { id: 'reviewer', kind: 'reviewer-unavailable', severity: 'blocking' },
  ] })
  assert.equal(reviewer.labelKey, 'board.action.configure_reviewer')
  assert.deepEqual(reviewer.target, { type: 'panel', panel: 'reviewer', entityId: null, section: null })

  const missingDependency = primaryAction.resolve({ ...base, blockers: [
    { id: 'missing', kind: 'dependency-missing', severity: 'blocking', relatedTaskStem: 'TASK_404_missing' },
  ] })
  assert.equal(missingDependency.labelKey, 'board.action.review_dependencies')
  assert.deepEqual(missingDependency.target, { type: 'task', stem: base.stem, section: 'dependencies' })

  const runner = primaryAction.resolve({ ...base, blockers: [
    { id: 'runner', kind: 'runner-unavailable', severity: 'warning' },
  ] })
  assert.equal(runner.kind, 'run')
  assert.equal(runner.labelKey, 'board.action.run')
  assert.equal(runner.enabled, true)
  assert.equal(runner.disabledReasonCode, null)

  const pending = primaryAction.resolve({ ...base, state: 'pending', blockers: [
    { id: 'runner', kind: 'runner-unavailable', severity: 'warning' },
  ] })
  assert.equal(pending.kind, 'submit-answers')
  assert.equal(pending.enabled, true)
})

test('dependency diagnostics allow backlog Prepare and become blocking only for todo Run', () => {
  const dependencySummary = {
    items: [{ stem: 'TASK_9_dependency', title: 'Dependency', missing: false, satisfied: false }],
  }
  const backlogBlockers = taskBlockers.fromContext({
    stem: 'TASK_7_dependent', state: 'backlog', dependencySummary,
  })
  assert.equal(backlogBlockers[0].kind, 'dependency-incomplete')
  assert.equal(backlogBlockers[0].severity, 'warning')
  const prepare = primaryAction.resolve({
    stem: 'TASK_7_dependent', state: 'backlog', sourceRevision: REV, blockers: backlogBlockers,
  })
  assert.equal(prepare.kind, 'prepare')
  assert.equal(prepare.enabled, true)

  const todoBlockers = taskBlockers.fromContext({
    stem: 'TASK_7_dependent', state: 'todo', dependencySummary,
  })
  assert.equal(todoBlockers[0].kind, 'dependency-incomplete')
  assert.equal(todoBlockers[0].severity, 'blocking')
  const run = primaryAction.resolve({
    stem: 'TASK_7_dependent', state: 'todo', sourceRevision: REV, blockers: todoBlockers,
  })
  assert.equal(run.kind, 'resolve-blocker')
  assert.equal(run.labelKey, 'board.action.review_dependencies')
})

test('queued work without an attached worker is projected as waiting-runner', () => {
  const deps = fixture()
  deps.snapshot.runnerActive = false
  deps.snapshot.status.worker = {}
  deps.snapshot.progress.requests = [{
    id: 'req-waiting-runner',
    stem: 'TASK_1_manual',
    action: 'prep',
    createdAt: '2026-07-18T12:00:00Z',
  }]
  const item = taskSummary.single('TASK_1_manual', deps).task
  assert.equal(item.runtimeStatus.state, 'waiting-runner')
  assert.equal(item.runtimeStatus.active, true)
  assert.equal(item.primaryAction.kind, 'open-run')
  assert.equal(item.primaryAction.enabled, false)
  assert.equal(item.primaryAction.disabledReasonCode, 'terminal-not-started')
})

test('a stale source refusal is historical after refresh and does not replace the fresh primary action', () => {
  const deps = fixture()
  deps.latestReader = (stem) => stem === 'TASK_3_ready' ? {
    event: {
      kind: 'queue-skipped', status: 'fail', ts: AT,
      meta: { reasonCode: 'source-revision-changed' },
    },
    truncated: false,
  } : { event: null, truncated: false }
  const current = taskSummary.single('TASK_3_ready', deps).task
  assert.equal(current.blockers.some((item) => item.kind === 'source-changed'), false)
  assert.equal(current.primaryAction.kind, 'run')
  assert.equal(current.primaryAction.enabled, true)
})

test('Prepare stays available while backlog Design diagnostics remain visible', () => {
  const deps = fixture()
  deps.snapshot.screensCache.TASK_1_manual = {
    needed: true,
    pulled: false,
    status: 'missing',
    designIssues: {
      malformed: false,
      captureBlocked: true,
      issueCount: 1,
      kinds: ['RISKY_SCREEN_NAME'],
      first: { kind: 'RISKY_SCREEN_NAME', line: 4 },
    },
  }
  const item = taskSummary.single('TASK_1_manual', deps).task
  assert.equal(item.primaryBlocker.kind, 'figma-design-invalid')
  assert.equal(item.primaryBlocker.severity, 'warning')
  assert.equal(item.blockers.some((blocker) => blocker.kind === 'figma-screens-missing'), false)
  assert.deepEqual(item.figmaDesignIssues, {
    issueCount: 1,
    kinds: ['risky_screen_name'],
    first: { kind: 'risky_screen_name', line: 4 },
  })
  assert.equal(item.primaryAction.kind, 'prepare')
  assert.equal(item.primaryAction.enabled, true)
})

test('missing Figma screens warn but do not replace the todo Run action', () => {
  const deps = fixture()
  deps.snapshot.screensCache.TASK_3_ready = {
    needed: true,
    pulled: false,
    status: 'missing',
  }
  const item = taskSummary.single('TASK_3_ready', deps).task
  assert.equal(item.primaryBlocker.kind, 'figma-screens-missing')
  assert.equal(item.primaryBlocker.severity, 'warning')
  assert.equal(item.primaryAction.kind, 'run')
  assert.equal(item.primaryAction.enabled, true)
})

function questionBody(answer = '') {
  return [
    '# TASK 3 — Ready',
    '',
    '## Goal',
    '',
    'Ship it.',
    '',
    '## Questions',
    '',
    '### Q1 — Authorize the client change?',
    '',
    '- (a) **Fix here** — smallest diff.',
    '- (b) **Split** — separate task.',
    '',
    '**Type**: choice',
    '**Options**: a, b',
    '',
    '#### Answer',
    '',
    answer,
    '',
  ].join('\n')
}

function questionFixture({ answered = false, stoppedRun = false } = {}) {
  const base = fixture()
  const text = questionBody(answered ? 'a' : '')
  base.validation = {
    indexStatus: 'fresh',
    findings: [],
    _model: {
      artifacts: new Map([['TASK_3_ready', {
        stem: 'TASK_3_ready', backlog: null, pending: null, done: null,
        todo: { readable: true, text, contentHash: REV, relPath: 'orchestrator/tasks/todo/TASK_3_ready.md' },
      }]]),
      metadata: new Map(),
    },
  }
  if (stoppedRun) {
    base.snapshot.progress.inProgress = [{ stem: 'TASK_3_ready', stage: 'orchestrator' }]
    base.snapshot.sessions = {
      'task:TASK_3_ready': {
        running: false, endedAt: AT, askedThisTurn: false, sessionId: null, inputReady: false,
      },
    }
  }
  return base
}

test('a durable in-body question raises the answer rail without any live session', () => {
  const summary = taskSummary.build({ limit: 100 }, questionFixture())
  const card = flatten(summary).find((item) => item.stem === 'TASK_3_ready')
  const awaiting = card.blockers.find((item) => item.kind === 'awaiting-answer')
  assert.ok(awaiting, 'a published question must block the card from content alone')
  assert.equal(awaiting.summary, 'Answer the open question so the task can continue.')
  assert.equal(awaiting.source, 'task', 'a durable question does not come from a session')
  assert.equal(card.primaryAction.kind, 'submit-answers')
  assert.equal(card.primaryAction.behavior, 'open-details')
  assert.equal(card.primaryAction.target.section, 'questions')
  assert.equal(card.primaryAction.expectedState, 'todo')
  assert.equal(card.runtimeStatus.active, false)
})

test('a superseded execution generation exposes release before a queued durable answer', () => {
  const deps = questionFixture()
  deps.snapshot.progress.integrationReady = [{
    stem: 'TASK_3_ready',
    worktreeId: 'wt-' + 'a'.repeat(32),
    runId: 'run-' + 'b'.repeat(32),
    status: 'revalidation-required',
    targetRef: 'refs/heads/main',
  }]
  deps.snapshot.progress.requests = [{
    id: '1786689056733-6c1f4fe6cad638e5',
    stem: 'TASK_3_ready',
    action: 'run',
    createdAt: AT,
  }]
  const card = taskSummary.single('TASK_3_ready', deps).task
  assert.equal(card.runtimeStatus.state, 'queued')
  assert.equal(card.runtimeStatus.active, true)
  assert.equal(card.primaryBlocker.kind, 'generation-outdated')
  assert.equal(card.blockers.some((item) => item.kind === 'awaiting-answer'), true)
  assert.equal(card.primaryAction.kind, 'integrate')
  assert.equal(card.primaryAction.behavior, 'execute')
  assert.equal(card.primaryAction.labelKey, 'board.action.release_generation')
})

test('an answered in-body question stops blocking the card', () => {
  const summary = taskSummary.build({ limit: 100 }, questionFixture({ answered: true }))
  const card = flatten(summary).find((item) => item.stem === 'TASK_3_ready')
  assert.equal(card.blockers.some((item) => item.kind === 'awaiting-answer'), false)
  assert.equal(card.primaryAction.kind, 'run')
})

test('a stale lock from the escalated run is recovered before the durable answer', () => {
  const summary = taskSummary.build({ limit: 100 }, questionFixture({ stoppedRun: true }))
  const card = flatten(summary).find((item) => item.stem === 'TASK_3_ready')
  const kinds = card.blockers.map((item) => item.kind)
  assert.ok(kinds.includes('awaiting-answer'), 'the question stays visible while the lock is stale')
  assert.ok(kinds.includes('stopped-run'), 'the stale lock stays visible as its own blocker')
  // Persisting the answer needs a fresh orchestrator lock, so recovery is the
  // prerequisite step rather than a submit that could never acquire one.
  assert.equal(card.primaryAction.kind, 'resolve-blocker')
  assert.equal(card.primaryAction.labelKey, 'board.action.open_run_details')
  assert.equal(card.primaryAction.target.section, 'advanced')
})

test('a lock without a live terminal never hides the answer CTA behind Open terminal', () => {
  // A run started outside the site leaves a lock and no session record, so the
  // row reads active. Offering a disabled "Open terminal" there would render
  // the question form with nothing that can submit it.
  const resolved = primaryAction.resolve({
    stem: 'TASK_3_ready', state: 'todo', sourceRevision: REV,
    blockers: taskBlockers.fromContext({ stem: 'TASK_3_ready', state: 'todo', questionsPending: true }),
    active: true, liveAwaiting: false, questionsPending: true, terminalAvailable: false,
  })
  assert.equal(resolved.kind, 'submit-answers')
  assert.equal(resolved.enabled, true)
  assert.equal(resolved.target.section, 'questions')
  // A real live terminal still wins.
  const live = primaryAction.resolve({
    stem: 'TASK_3_ready', state: 'todo', sourceRevision: REV,
    blockers: taskBlockers.fromContext({ stem: 'TASK_3_ready', state: 'todo', questionsPending: true }),
    active: true, liveAwaiting: false, questionsPending: true, terminalAvailable: true,
  })
  assert.equal(live.kind, 'open-run')
})

test('a live paused session still outranks the durable question rail', () => {
  const resolved = primaryAction.resolve({
    stem: 'TASK_3_ready', state: 'todo', sourceRevision: REV,
    blockers: taskBlockers.fromContext({
      stem: 'TASK_3_ready', state: 'todo', liveAwaiting: true, questionsPending: true,
    }),
    liveAwaiting: true, questionsPending: true,
    liveSessionId: 'sess-1', sessionRevision: REV, sessionInputReady: true,
  })
  assert.equal(resolved.kind, 'continue-live')
  assert.equal(resolved.behavior, 'open-details')
  assert.equal(resolved.attentionRequired, true)
})

test('every blocker kind is normalized, bounded, allowlisted and priority-sorted', () => {
  const stem = 'TASK_8_blockers'
  const contexts = [
    { findings: [{ code: 'OTHER', severity: 'error', stem }] },
    { findings: [{ code: 'DEPENDENCY_CYCLE', severity: 'error', stem }] },
    { finalization: { status: 'recoverable', recoverable: true, recoveryRunning: false } },
    { finalization: { status: 'corrupt', recoverable: false, recoveryRunning: false } },
    { integration: { state: 'revalidation-required' } },
    { liveAwaiting: true },
    { dependencySummary: { items: [{ stem: 'TASK_90_missing', missing: true, satisfied: false }] } },
    { dependencySummary: { items: [{ stem: 'TASK_91_waiting', title: 'Waiting', missing: false, satisfied: false }] } },
    { figmaReviewPending: true },
    { figmaDesignInvalid: true },
    { figmaScreensMissing: true },
    { setupIncomplete: true },
    { reviewerUnavailable: true },
    { stoppedRun: true, retryCheckpoint: { id: 'cp' } },
    { validationPending: true },
    { runnerUnavailable: true },
  ]
  const emitted = contexts.flatMap((context) => taskBlockers.fromContext({ stem, state: 'todo', ...context }))
  emitted.push(taskBlockers.blocker(stem, 'unknown-recovery', 'blocking', 'Unknown recovery', 'Review recovery.'))
  const awaiting = emitted.find((item) => item.kind === 'awaiting-answer')
  assert.equal(awaiting.summary, 'Answer the open question so the task can continue.')
  assert.equal(awaiting.source, 'session', 'a live paused turn still comes from the session')
  assert.deepEqual(new Set(emitted.map((item) => item.kind)), new Set(taskBlockers.KINDS))
  for (const item of emitted) {
    assert.ok(taskBlockers.KINDS.includes(item.kind))
    assert.ok(item.title.length <= 160)
    assert.ok(item.summary.length <= 320)
    assert.deepEqual(Object.keys(item).sort(), [
      'id', 'kind', 'recoverable', 'relatedTaskStem', 'severity', 'source', 'summary', 'title',
    ].sort())
  }
  const all = taskBlockers.KINDS.map((kind) => taskBlockers.blocker(stem, kind, 'blocking', kind, kind))
    .sort((left, right) => taskBlockers.PRIORITY[left.kind] - taskBlockers.PRIORITY[right.kind])
  for (let index = 1; index < all.length; index++) {
    assert.ok(taskBlockers.PRIORITY[all[index - 1].kind] <= taskBlockers.PRIORITY[all[index].kind])
  }
  const race = taskBlockers.fromContext({
    stem, state: 'todo',
    findings: [{ code: 'OTHER', severity: 'error', stem }],
    finalization: { status: 'recoverable', recoverable: true, recoveryRunning: false },
    liveAwaiting: true,
  })
  assert.equal(race[0].kind, 'task-integrity')

  const backlogSeverityRace = taskBlockers.fromContext({
    stem, state: 'backlog',
    dependencySummary: { items: [{ stem: 'TASK_90_missing', missing: true, satisfied: false }] },
    setupIncomplete: true,
  })
  assert.equal(backlogSeverityRace[0].kind, 'setup-incomplete')
  assert.equal(backlogSeverityRace[0].severity, 'blocking')
})

test('last activity honors journal, then newest runtime transition, then canonical task metadata', () => {
  const journalDeps = fixture()
  journalDeps.snapshot.progress.requests = [{ stem: 'TASK_3_ready', createdAt: '2026-07-18T11:00:00Z' }]
  journalDeps.latestReader = (stem) => stem === 'TASK_3_ready' ? {
    event: { kind: 'review-passed', ts: '2026-07-18T09:00:00Z', phase: 'review', status: 'ok' }, truncated: false,
  } : { event: null, truncated: false }
  let item = taskSummary.single('TASK_3_ready', journalDeps).task
  assert.equal(item.lastActivity.source, 'journal')
  assert.equal(item.lastActivity.kind, 'review-passed')

  const runtimeDeps = fixture()
  runtimeDeps.snapshot.progress.requests = [{ stem: 'TASK_3_ready', createdAt: '2026-07-18T11:00:00Z' }]
  runtimeDeps.snapshot.sessions['task:TASK_3_ready'] = {
    running: false, startedAt: '2026-07-18T10:30:00Z', endedAt: '2026-07-18T12:00:00Z', exitCode: 1,
  }
  item = taskSummary.single('TASK_3_ready', runtimeDeps).task
  assert.equal(item.lastActivity.source, 'runtime')
  assert.equal(item.lastActivity.kind, 'run-stopped')
  assert.equal(item.lastActivity.occurredAt, '2026-07-18T12:00:00Z')

  item = taskSummary.single('TASK_3_ready', fixture()).task
  assert.equal(item.lastActivity.source, 'task-metadata')
  assert.equal(Object.hasOwn(item.lastActivity, 'inferredLegacy'), false)
})

test('dependency projection is present in every lifecycle state and done dependencies are historical', () => {
  const deps = fixture()
  deps.indexRead.value.backlog[0].dependsOn = ['TASK_4_done']
  deps.indexRead.value.pending[0].dependsOn = ['TASK_4_done']
  deps.indexRead.value.todo[0].dependsOn = ['TASK_4_done']
  deps.indexRead.value.done[0].dependsOn = ['TASK_99_missing']
  const rows = flatten(taskSummary.build({ limit: 100 }, deps))
  for (const item of rows.filter((item) => ['TASK_1_manual', 'TASK_2_follow_up', 'TASK_3_ready', 'TASK_4_done'].includes(item.stem))) {
    assert.ok(item.dependencySummary.count > 0)
  }
  const done = rows.find((item) => item.stem === 'TASK_4_done')
  assert.equal(done.dependencySummary.items[0].missing, true)
  assert.equal(done.dependencySummary.blockedCount, 0)
  assert.equal(done.blockers.some((item) => item.kind.startsWith('dependency-')), false)
})

test('only a current non-stale app validation receipt clears manual validation', () => {
  const deps = fixture()
  // The receipt revision domain is the sha256 of the canonical done BODY file
  // (what the checklist was parsed from), never row.sourceRevision (the
  // state-scoped task revision).
  const BODY = 'sha256:' + 'd'.repeat(64)
  deps.validation._model = {
    metadata: new Map([['TASK_4_done', {
      outcome: { acceptance: [{ verdict: 'manual', text: 'Check the completed flow.' }] },
    }]]),
    artifacts: new Map([['TASK_4_done', { done: { contentHash: BODY } }]]),
  }
  const project = (receipts = []) => taskSummary.projectRows(
    deps.indexRead.value, deps.validation, deps.snapshot, deps.latestReader, receipts,
  ).columns.done.find((candidate) => candidate.stem === 'TASK_4_done')
  let item = project()
  assert.deepEqual(item.appValidation, { required: true, current: false, overall: null })
  assert.equal(item.primaryAction.kind, 'validate-in-app')
  assert.equal(item.primaryBlocker.kind, 'validation-required')

  const receipts = [{
    receiptId: 'validation-1', taskStem: 'TASK_4_done', taskSourceRevision: BODY,
    overall: 'passed', staleTask: false, staleSource: false, createdAt: '2026-07-18T12:00:00Z',
  }]
  item = project(receipts)
  assert.deepEqual(item.appValidation, { required: true, current: true, overall: 'passed' })
  assert.equal(item.primaryAction.kind, 'review-result')
  assert.equal(item.blockers.some((blocker) => blocker.kind === 'validation-required'), false)

  const wrongDomain = project([{ ...receipts[0], taskSourceRevision: REV }])
  assert.equal(wrongDomain.appValidation.current, false,
    'a receipt carrying the state-scoped row revision must never clear manual validation')

  deps.validation._model.artifacts = new Map()
  const unproven = project(receipts)
  assert.equal(unproven.appValidation.current, false,
    'no proven canonical body hash means no receipt can be current')
  deps.validation._model.artifacts = new Map([['TASK_4_done', { done: { contentHash: BODY } }]])

  receipts[0].staleTask = true
  item = project(receipts)
  assert.equal(item.appValidation.current, false)
  assert.equal(item.primaryAction.kind, 'validate-in-app')
})

test('invalid app validation receipt data is ignored and makes the summary explicitly partial', () => {
  const deps = fixture()
  deps.validationReceipts = [{ receiptId: 'broken-only' }]
  const summary = taskSummary.build({ limit: 100 }, deps)
  assert.equal(summary.partial, true)
  assert.ok(summary.limitations.includes('app-validation-receipts-invalid'))
  assert.deepEqual(taskSummary.normalizeValidationReceipts(new Array(1001)), {
    receipts: [], limitation: 'app-validation-receipts-invalid',
  })
})

test('live awaiting sessions and running finalization recovery keep secondary mutations fenced', () => {
  const working = fixture()
  working.snapshot.sessions['task:TASK_3_ready'] = {
    running: true, askedThisTurn: false, awaitingTurn: true, inputReady: false,
    startedAt: '2026-07-18T11:59:00Z',
  }
  let item = taskSummary.single('TASK_3_ready', working).task
  assert.equal(item.runtimeStatus.state, 'running')
  assert.equal(item.primaryAction.kind, 'open-run')
  assert.equal(item.primaryAction.labelKey, 'board.action.open_terminal')
  assert.equal(item.blockers.some((blocker) => blocker.kind === 'awaiting-answer'), false)

  const waiting = fixture()
  waiting.snapshot.sessions['task:TASK_3_ready'] = {
    running: true, askedThisTurn: true, awaitingTurn: false, inputReady: true,
    sessionId: 'session-live-3', revision: 'sha256:' + '9'.repeat(64),
    startedAt: '2026-07-18T12:00:00Z',
  }
  item = taskSummary.single('TASK_3_ready', waiting).task
  assert.equal(item.runtimeStatus.state, 'awaiting')
  assert.equal(item.runtimeStatus.active, true)
  assert.equal(item.primaryAction.kind, 'continue-live')
  assert.equal(item.primaryAction.labelKey, 'board.action.continue_live')
  assert.equal(item.primaryAction.behavior, 'open-details')
  assert.equal(item.primaryAction.attentionRequired, true)
  assert.deepEqual(item.primaryAction.target, {
    type: 'task', stem: 'TASK_3_ready', section: 'questions',
  })
  assert.equal(item.primaryAction.liveSessionId, 'session-live-3')
  assert.equal(item.primaryAction.expectedSessionRevision, 'sha256:' + '9'.repeat(64))
  assert.equal(item.secondaryActions.find((action) => action.kind === 'drop').enabled, false)

  const recovering = fixture()
  recovering.snapshot.progress.finalizations = [{
    stem: 'TASK_3_ready', status: 'recoverable', recoverable: true, recoveryRunning: true,
    updatedAt: '2026-07-18T12:00:00Z',
  }]
  item = taskSummary.single('TASK_3_ready', recovering).task
  assert.equal(item.primaryAction.kind, 'resolve-blocker')
  assert.equal(item.primaryAction.target.section, 'advanced')
  assert.equal(item.secondaryActions.find((action) => action.kind === 'drop').enabled, false)
})

test('bounded summaries report runtime limitations without fabricating a clean snapshot', () => {
  const deps = fixture()
  deps.validation.runtimeStats = { truncated: true }
  deps.latestReader = (stem) => ({ event: null, truncated: stem === 'TASK_3_ready' })
  const summary = taskSummary.build({ limit: 2 }, deps)
  assert.equal(flatten(summary).length, 2)
  assert.equal(summary.partial, true)
  assert.deepEqual(summary.limitations, ['runtime-scan-truncated', 'task-journal-truncated'])
})

test('a non-fresh canonical INDEX produces a partial but usable summary', () => {
  const deps = fixture()
  deps.validation.indexStatus = 'stale'
  const summary = taskSummary.build({ limit: 2 }, deps)
  assert.equal(summary.partial, true)
  assert.ok(summary.limitations.includes('task-index-stale'))
  assert.equal(flatten(summary).length, 2)
})

test('summary protocol never echoes a non-current INDEX version', () => {
  const deps = fixture()
  deps.indexRead = {
    value: { ...deps.indexRead.value, version: 99 },
    revision: deps.indexRead.revision,
  }
  const summary = taskSummary.build({ limit: 2 }, deps)
  assert.equal(summary.indexSchemaVersion, 2)
})

test('typed action admission rejects stale cards and returns only a server-owned prompt', () => {
  const deps = fixture()
  deps.snapshot.runnerActive = false
  deps.snapshot.status.worker = {}
  const current = taskSummary.single('TASK_3_ready', deps).task.primaryAction
  assert.equal(current.kind, 'run')
  assert.equal(current.enabled, true)
  const request = actionRequest('TASK_3_ready', current)
  const accepted = taskActions.inspect(request, deps)
  assert.equal(accepted.ok, true)
  assert.equal(accepted.operation, 'enqueue')
  assert.equal(accepted.request.action, 'run')
  assert.match(accepted.request.prompt, /Preserve task Source provenance/)
  assert.equal(Object.hasOwn(request, 'prompt'), false)
  const rejected = taskActions.inspect({ ...request, actionRevision: 'sha256:' + '0'.repeat(64) }, deps)
  assert.equal(rejected.ok, false)
  assert.equal(rejected.status, 409)
  assert.equal(rejected.error, 'action-stale')
  assert.equal(taskActions.inspect({ ...request, prompt: 'browser owned' }, deps).status, 400)

  const retryDeps = fixture()
  retryDeps.latestReader = () => ({
    event: { kind: 'stop', status: 'fail', phase: 'review', meta: { checkpointId: 'cp-1' } },
    truncated: false,
  })
  const retry = taskSummary.single('TASK_3_ready', retryDeps).task.primaryAction
  assert.equal(retry.kind, 'run')
  assert.equal(retry.checkpointId, null)
})

test('overflow mutations and prompt fallback use revision-fenced server actions', () => {
  const deps = fixture()
  deps.snapshot.runnerActive = false
  const ready = taskSummary.single('TASK_3_ready', deps).task
  const copy = ready.secondaryActions.find((action) => action.kind === 'copy-prompt')
  assert.equal(copy.labelKey, 'board.overflow.copy_run_prompt')
  const copied = taskActionPromptPreview.build(
    ready.stem, { actionRevision: copy.actionRevision }, deps
  )
  assert.equal(copied.ok, true)
  assert.equal(copied.manualFallback, true)
  assert.match(copied.text, /Preserve task Source provenance/)

  const impact = {
    sourceRevision: REV,
    impactHash: 'sha256:' + 'c'.repeat(64),
    dependents: ['TASK_5_blocked'],
  }
  const withDrop = {
    ...deps,
    dropInspector: () => ({
      result: { ok: false, indexStatus: 'stale' },
      admission: { ok: true, blockers: [] },
      impact
    })
  }
  const dropRow = taskSummary.single('TASK_3_ready', withDrop).task
  const drop = dropRow.secondaryActions.find((action) => action.kind === 'drop')
  const dropped = taskActions.inspect(actionRequest(dropRow.stem, drop, impact), withDrop)
  assert.equal(dropped.ok, true)
  assert.equal(dropped.request.action, 'drop')
  assert.match(dropped.request.prompt, new RegExp(impact.impactHash))
  assert.equal(taskActions.inspect(actionRequest(dropRow.stem, drop, { ...impact, impactHash: 'sha256:' + 'd'.repeat(64) }), withDrop).status, 409)

  const done = taskSummary.single('TASK_4_done', deps).task
  const reopen = done.secondaryActions.find((action) => action.kind === 'reopen')
  assert.equal(taskActions.inspect(actionRequest(done.stem, reopen), deps).error, 'reopen-confirmation-required')
  const reopened = taskActions.inspect(actionRequest(done.stem, reopen, true), deps)
  assert.equal(reopened.ok, true)
  assert.equal(reopened.request.action, 'reopen')
  assert.match(reopened.request.prompt, /exact `sourceRevision`/)
})
