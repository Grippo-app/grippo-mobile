#!/usr/bin/env node

import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'
import { tasksApi } from '../scripts/data/tasks-api.js'
import { createBoardReadinessPolicy } from '../scripts/board/board-readiness-policy.js'
import {
  dictionaryFor,
  localeText
} from './i18n-test-helpers.mjs'

const require = createRequire(import.meta.url)
const taskRequirement = require('../server/task-requirement.js')
const HERE = path.dirname(fileURLToPath(import.meta.url))
const SITE = path.resolve(HERE, '..')
let passed = 0
let total = 0

async function check(name, fn) {
  total++
  try {
    await fn()
    passed++
    process.stdout.write(`  PASS  ${name}\n`)
  } catch (error) {
    process.stderr.write(`  FAIL  ${name}: ${error && error.stack || error}\n`)
    process.exitCode = 1
  }
}

async function withFetch(handler, fn) {
  const prior = globalThis.fetch
  globalThis.fetch = handler
  try { return await fn() }
  finally { globalThis.fetch = prior }
}

function json(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json' }
  })
}

function validOutcome() {
  return [
    '# TASK 1 — Complete',
    '',
    '```markdown',
    '---',
    '## Outcome',
    '```',
    '',
    '---',
    '',
    '## Outcome',
    '',
    '**Status**: completed',
    '**Completed at**: 2026-07-13T12:00:00Z',
    '**Reviewer**: codex',
    '**Review iterations**: 0',
    '',
    '### Build gates',
    '- none',
    '',
    '### Runtime verify',
    '- Gate: skipped (fixture)',
    '- Result: n/a — fixture',
    '',
    '### Acceptance trace',
    '- `test/contract.mjs` — verified — passed',
    '',
    '### Caveats',
    '- none',
    '',
    '### Follow-ups',
    '- none',
    '',
    '### Files touched',
    '- `src/feature.js` — modified',
    '',
  ].join('\n')
}

process.stdout.write('Task-integrity Site UI contract:\n')

await check('fresh integrity loader is no-store and returns the bounded envelope', async () => {
  await withFetch(async (url, opts) => {
    assert.equal(url, '/api/tasks/integrity')
    assert.equal(opts.cache, 'no-store')
    return json({ version: 1, ok: true, indexStatus: 'fresh', affectedStems: [], findings: [] })
  }, async () => {
    const result = await tasksApi.loadTaskIntegrity()
    assert.equal(result.ok, true)
    assert.equal(result.indexStatus, 'fresh')
  })
})

await check('contradictory integrity envelopes fail closed in the client', async () => {
  await withFetch(async () => json({
    version: 1, ok: true, indexStatus: 'stale', affectedStems: [], findings: []
  }), async () => {
    await assert.rejects(tasksApi.loadTaskIntegrity(), (error) => {
      assert.equal(error.kind, 'invalid-task-integrity-json')
      return true
    })
  })
})

await check('a truncated integrity envelope fences only explicitly affected stems', async () => {
  const policy = fs.readFileSync(
    path.join(SITE, 'scripts', 'board', 'board-readiness-policy.js'),
    'utf8')
  const start = policy.indexOf('function integrityBlocksStem(')
  const end = policy.indexOf('return {', start)
  assert.ok(start >= 0 && end > start, 'canonical stem-blocking policy missing')
  const body = policy.slice(start, end)
  assert.doesNotMatch(body, /integrity\.truncated/)
  assert.match(body, /item\.stem === stem/)
  assert.doesNotMatch(body, /item\.stem === null/)

  const readiness = createBoardReadinessPolicy({
    t: key => key,
    getFreshIntegrity: () => ({
      ok: false,
      truncated: true,
      findings: [{ severity: 'error', stem: null }],
      affectedStems: ['TASK_2'],
    }),
    getSnapshot: () => ({ startupRecovery: { status: 'ready' } }),
  })
  assert.equal(readiness.integrityBlocksStem('TASK_1'), false)
  assert.equal(readiness.integrityBlocksStem('TASK_2'), true)
})

await check('Board uses an always-present fixed status control with a copyable modal', async () => {
  const board = fs.readFileSync(path.join(SITE, 'scripts', 'panels', 'board.js'), 'utf8')
  const boardHealth = fs.readFileSync(path.join(SITE, 'scripts', 'board', 'board-health.js'), 'utf8')
  const boardRenderer = fs.readFileSync(
    path.join(SITE, 'scripts', 'board', 'board-render-controller.js'), 'utf8')
  const styles = fs.readFileSync(path.join(SITE, 'styles', 'panels.css'), 'utf8')
  assert.match(board, /createBoardHealthController\(\{/)
  assert.match(boardHealth, /function renderBoardStatus\(/)
  assert.match(boardHealth, /healthEl\.className = 'board-health board-health--'/)
  assert.match(boardHealth, /'aria-haspopup': 'dialog'/)
  assert.match(boardHealth, /var capturedAt = new Date\(\)\.toISOString\(\)/)
  assert.match(boardHealth, /clipboard\.copy\(boardHealthCopyText\(issues, capturedAt\)\)/)
  assert.match(boardHealth, /capturedAt: capturedAt/)
  assert.doesNotMatch(boardHealth, /generatedAt: new Date\(\)\.toISOString\(\)/)
  assert.match(boardRenderer,
    /var preserveHealth = healthElement && healthElement\.parentNode === section/)
  assert.match(boardRenderer, /child !== healthElement/)
  assert.match(boardHealth, /healthAnnouncementSignature/)
  assert.match(boardHealth, /TASK_DIAGNOSTICS_TRUNCATED/)
  assert.match(boardHealth, /pathsTruncated: item\.pathsTruncated === true/)
  assert.match(boardHealth, /healthSummaryEl\.textContent = severity === 'ok' \? '' : summary/)
  assert.match(styles, /\.board-health__severity\s*\{[^}]*border-left:/s)
  assert.doesNotMatch(boardRenderer,
    /renderIntegrityNotice|renderErrorBanner|renderSummaryLimitations/)
})

await check('Board status copy and translations describe bounded diagnostics without legacy banner language', async () => {
  const board = fs.readFileSync(path.join(SITE, 'scripts', 'panels', 'board.js'), 'utf8')
  const boardHealth = fs.readFileSync(path.join(SITE, 'scripts', 'board', 'board-health.js'), 'utf8')
  const styles = fs.readFileSync(path.join(SITE, 'styles', 'panels.css'), 'utf8')
  const localeDictionaries = ['en', 'ru', 'uk'].map(dictionaryFor)
  const dictionaryText = localeDictionaries
    .flatMap((dictionary) => Object.values(dictionary))
    .join('\n')
  assert.match(boardHealth, /diagnosticsTruncated: item\.diagnosticsTruncated === true/)
  for (const key of [
    'board.status.severity.ok',
    'board.status.severity.warning',
    'board.status.severity.error',
    'board.status.diagnosticsTruncated',
    'board.status.pathsTruncated',
    'board.drop.bodyDone',
    'board.drop.bodyCorrupt',
  ]) {
    assert.equal(
      localeDictionaries.filter((dictionary) => Object.hasOwn(dictionary, key)).length,
      3
    )
  }
  for (const [dictionary, label] of localeDictionaries.map((localeDictionary, index) =>
    [localeDictionary, ['Board', 'Борда', 'Дошка'][index]])) {
    assert.equal(dictionary['board.status.label'], label)
  }
  assert.doesNotMatch(dictionaryText, /integrity banner|баннер целостности|банер цілісності|banner above|баннере выше|банері вище/)
  assert.doesNotMatch(board + boardHealth + styles, /board-integrity__/)
  assert.match(board, /folder === 'done' \? 'board\.drop\.bodyDone'/)
  assert.match(board, /folder === 'corrupt' \? 'board\.drop\.bodyCorrupt'/)
})

await check('browser task client contains no duplicate Outcome or Questions parser', async () => {
  const clientSource = fs.readFileSync(path.join(SITE, 'scripts', 'data', 'tasks-api.js'), 'utf8')
  assert.equal(tasksApi.parseOutcomeAppendix, undefined)
  assert.equal(tasksApi.parseQuestions, undefined)
  assert.doesNotMatch(clientSource, /function parseOutcomeAppendix|function parseQuestions|outcome-shape\.gen/)
})

await check('Task Details client rejects response-envelope extensions instead of merging DTO generations', async () => {
  const stem = 'TASK_1_exact_details'
  const body = {
    schemaVersion: 1,
    revision: 'sha256:' + 'a'.repeat(64),
    identity: { stem },
    state: {},
    origin: {},
    primaryAction: {},
    secondaryActions: [],
    blockers: [],
    designIssues: null,
    dependencies: {},
    lastActivity: null,
    requirement: {},
    outcome: {},
    currentWork: { kind: 'next-action' },
    retryRecovery: null,
    activitySummary: {},
    artifactSummary: {},
    recovery: {},
    appValidation: {},
    advancedAvailable: true,
    partial: false,
    limitations: [],
    unexpectedGenerationField: true,
  }
  await withFetch(async () => json(body), async () => {
    await assert.rejects(tasksApi.loadTaskDetails(stem), (error) => {
      assert.equal(error.kind, 'invalid-task-details-json')
      return true
    })
  })
})

await check('server question projection uses physical structural tokens', async () => {
  const sidecar = [
    '---',
    'forTask: TASK_1_question_parser',
    'createdAt: 2026-07-14T10:00:00Z',
    'round: 1',
    '---',
    '',
    '```markdown',
    '## Q99 — fenced decoy',
    '**Type**: choice',
    '**Options**: a, b',
    '### Answer',
    'fenced answer',
    '```',
    '',
    '## Q1 — Real question',
    '',
    'The fenced example below is authored context and must survive a save.',
    '',
    '```markdown',
    '**Type**: choice',
    '**Options**: a, b',
    '### Answer',
    'not the real answer',
    '```',
    '',
    '**Type**: text',
    '',
    '### Answer',
    '',
    'real answer',
    '',
  ].join('\n')
  const parse = (raw) => taskRequirement.questions({
    questionsRaw: raw,
    questionsRevision: 'sha256:' + '1'.repeat(64),
  })
  const parsed = parse(sidecar)
  assert.equal(parsed.valid, true)
  assert.equal(parsed.questions.length, 1, 'fenced Q headings stay inert')
  assert.equal(parsed.questions[0].id, 1)
  assert.equal(parsed.questions[0].type, 'text', 'fenced Type metadata stays inert')
  assert.equal(parsed.questions[0].answer, 'real answer', 'fenced Answer heading stays inert')
  assert.equal(parse('\uFEFF' + sidecar).valid, false,
    'server question parser must not strip a sidecar BOM')
  assert.equal(parse(sidecar.replace(/\n/g, '\r\n')).valid, false,
    'server question parser must not normalize sidecar line endings')

  const splitHeading = sidecar.replace('## Q1 — Real question', '##\nQ1 — Real question')
  assert.equal(parse(splitHeading).questions.length, 0,
    'a Q heading cannot borrow its identifier from the next line')

  const splitType = sidecar
    .replace(/```markdown[\s\S]*?```\n\n/, '')
    .replace('**Type**: text', '**Type**\n: choice')
  assert.equal(parse(splitType).questions[0].type, 'text',
    'split Type metadata does not become a choice credential')

  const indentedAnswer = sidecar.replace(
    '**Type**: text\n\n### Answer\n\nreal answer',
    '**Type**: text\n\n    ### Answer\n    indented code\n\n### Answer\n\nreal answer')
  assert.equal(parse(indentedAnswer).questions[0].answer, 'real answer',
    'an indented-code Answer lookalike does not preempt the structural H3')

  const questionsUi = fs.readFileSync(path.join(SITE, 'scripts', 'board', 'task-questions.js'), 'utf8')
  assert.match(questionsUi, /if \(!payload \|\| !payload\.valid\)[\s\S]*taskDetails\.questions\.unavailable/,
    'Details must keep pending actions unavailable for a non-canonical sidecar')
})

await check('server Design presence is routed through the shared structural parser', async () => {
  const stateSource = fs.readFileSync(path.join(SITE, 'server', 'state.js'), 'utf8')
  assert.match(stateSource, /figmaDesignParser\.hasDesignSection\(b\)/)
  assert.doesNotMatch(stateSource, /DESIGN_SECTION_RE/)
})

await check('every Board task-creation affordance shares a final integrity fence', async () => {
  const board = fs.readFileSync(path.join(SITE, 'scripts', 'panels', 'board.js'), 'utf8')
  const composer = fs.readFileSync(
    path.join(SITE, 'scripts', 'board', 'backlog-composer.js'),
    'utf8')
  const wrapperStart = board.indexOf('function createBacklogWithIntegrityFence(')
  const wrapperEnd = board.indexOf('// Fresh-lock window', wrapperStart)
  assert.ok(wrapperStart >= 0 && wrapperEnd > wrapperStart, 'shared creation fence missing')
  const wrapper = board.slice(wrapperStart, wrapperEnd)
  assert.match(wrapper, /if \(boardReadiness\.globalMutationBlocked\(\)\)/)
  assert.match(wrapper, /integrity: boardReadiness\.taskIntegrity\(\)/)
  assert.match(wrapper, /Promise\.reject\(\{[\s\S]*kind: 'task-integrity'/)
  assert.equal((board.match(/tasksApi\.createBacklog\(/g) || []).length, 1,
    'all direct createBacklog calls must stay inside the shared fence')
  assert.equal((board.match(/createBacklogWithIntegrityFence\(/g) || []).length, 2,
    'Board must define the shared fence and use it for visual-fix creation')
  assert.match(board,
    /createBacklogWithIntegrityFence: createBacklogWithIntegrityFence/,
    'Board must inject the shared fence into the backlog composer')
  assert.equal((composer.match(/createBacklogWithIntegrityFence\(/g) || []).length, 1,
    'the main backlog composer must use the injected shared fence')
  const visualFixStart = board.indexOf('function buildVisualFixActions(')
  const visualFixEnd = board.indexOf('function spawnRebundleSession(', visualFixStart)
  assert.ok(visualFixStart >= 0 && visualFixEnd > visualFixStart,
    'visual-fix action bounds missing')
  const visualFix = board.slice(visualFixStart, visualFixEnd)
  assert.match(visualFix, /boardReadiness\.globalMutationBlocked\(\)/)
})

await check('drop impact loader validates and preserves the canonical CAS tuple', async () => {
  const stem = 'TASK_12_profile'
  const revision = 'sha256:' + '1'.repeat(64)
  const impactHash = 'sha256:' + '2'.repeat(64)
  await withFetch(async (url, opts) => {
    assert.equal(url, '/api/tasks/drop-impact?stem=' + encodeURIComponent(stem))
    assert.equal(opts.cache, 'no-store')
    return json({
      version: 1, ok: true, operation: 'inspect-drop', stem, state: 'todo',
      sourceRevision: revision, dependents: ['TASK_13_child'], impactHash
    })
  }, async () => {
    const result = await tasksApi.loadDropImpact(stem)
    assert.equal(result.sourceRevision, revision)
    assert.equal(result.impactHash, impactHash)
    assert.deepEqual(result.dependents, ['TASK_13_child'])
  })
})

await check('malformed drop impact fails closed in the client', async () => {
  await withFetch(async () => json({ version: 1, ok: true, operation: 'inspect-drop' }), async () => {
    await assert.rejects(tasksApi.loadDropImpact('TASK_12_profile'), (error) => {
      assert.equal(error.kind, 'invalid-drop-impact-json')
      return true
    })
  })
})

await check('browser task client has no prompt-bearing legacy enqueue rail', async () => {
  assert.equal(tasksApi.enqueueRequest, undefined)
})

await check('Board keeps Drop two-phase while destructive prompts stay server-owned', async () => {
  const board = fs.readFileSync(path.join(SITE, 'scripts', 'panels', 'board.js'), 'utf8')
  const taskPrompts = fs.readFileSync(path.join(SITE, 'server', 'task-action-prompts.js'), 'utf8')
  const dropFlow = board.slice(board.indexOf('function runDropActionFlow('), board.indexOf('function runReopenActionFlow('))
  assert.equal((dropFlow.match(/loadDropImpact\(stem\)/g) || []).length, 2)
  assert.match(dropFlow, /sameDropImpact\(shownImpact, freshImpact\)/)
  assert.match(dropFlow, /executeTaskAction\(stem, typedAction, confirmation\)/)
  assert.doesNotMatch(dropFlow, /sessionCancel|clearRequest/)
  assert.doesNotMatch(board, /function dropButton\(/)
  assert.doesNotMatch(board, /enqueueRequest\('(drop|reopen)'/)

  function functionBody(source, name, next) {
    const start = source.indexOf('function ' + name + '(')
    const end = source.indexOf('function ' + next + '(', start + 1)
    assert.ok(start >= 0 && end > start, `${name} source segment missing`)
    return source.slice(start, end)
  }
  const prep = functionBody(taskPrompts, 'prepare', 'run')
  const run = functionBody(taskPrompts, 'run', 'retry')
  const answersStart = taskPrompts.indexOf('function submitAnswers(')
  const answersEnd = taskPrompts.indexOf('module.exports', answersStart)
  assert.ok(answersStart >= 0 && answersEnd > answersStart, 'submitAnswers source segment missing')
  const answers = taskPrompts.slice(answersStart, answersEnd)
  const drop = functionBody(taskPrompts, 'drop', 'reopen')
  const reopen = functionBody(taskPrompts, 'reopen', 'cleanAnswers')
  assert.doesNotMatch(prep, /delete the backlog file|Regenerate orchestrator\/tasks\/INDEX/)
  assert.match(prep, /--input -/)
  assert.match(prep, /BOARD PREP POLICY: NO QUESTIONS\./)
  assert.match(prep, /only successful lifecycle transition is[\s\S]{0,160}transition-task-state\.mjs promote/)
  assert.doesNotMatch(prep, /`promote\|ask`/)
  assert.doesNotMatch(prep, /temporary input|<temporary-input>/)
  assert.match(run, /complete task-orchestrator skill/)
  assert.doesNotMatch(answers, /overwriting the on-disk version|regenerate orchestrator\/tasks\/INDEX/)
  assert.match(answers, /serialized server-side/)
  assert.match(answers, /transition-task-state\.mjs persist-answers/)
  assert.match(answers, /source-revision <exact-sourceRevision>/)
  assert.match(answers, /re-read and prove byte equality/)
  assert.match(answers, /newly observed sourceRevision/)
  assert.match(answers, /Never write task or INDEX files directly/)
  assert.doesNotMatch(answers, /temporary input|<temporary-input>/)
  assert.doesNotMatch(drop, /git rm|git-rm|rm -f/)
  assert.match(drop, /transition-task-state\.mjs drop/)
  assert.doesNotMatch(reopen, /mv |cp |git rm|rm -f/)
  assert.match(reopen, /transition-task-state\.mjs reopen/)
})

await check('Board prompt builders never pre-seed a lock before canonical acquire', async () => {
  const board = fs.readFileSync(path.join(SITE, 'scripts', 'panels', 'board.js'), 'utf8')
  const taskActions = fs.readFileSync(path.join(SITE, 'server', 'task-actions.js'), 'utf8')
  const taskPrompts = fs.readFileSync(path.join(SITE, 'server', 'task-action-prompts.js'), 'utf8')
  const prompts = board + '\n' + taskActions + '\n' + taskPrompts

  assert.doesNotMatch(prompts, /prependLockWrite/)
  assert.doesNotMatch(prompts, /mktemp/)
  assert.doesNotMatch(prompts, /printf\s+['"]?\{["']?stage/)
  assert.doesNotMatch(prompts, /mv\s+["']?\$TMP/)
  assert.match(prompts, /function lockDirective/)
  assert.match(prompts, /task-lock\.mjs acquire/)
  assert.match(prompts, /ORCHESTRATOR_WRITER_SESSION_ID/)
  assert.match(prompts, /complete already-held guarded writer-lease receipt/)
  assert.match(prompts, /--owner-kind standby/)
  assert.match(prompts, /--writer-lease-id/)
  assert.match(prompts, /--writer-lease-token/)
  assert.match(prompts, /pre\/post publication scans reject/)
  assert.match(prompts, /never print the token/i)
  assert.match(prompts, /do NOT acquire a second writer lease/i)
  assert.match(prompts, /Never release the standby caller\\'s lease/)
  assert.match(prompts, /Lock age is never ownership or liveness proof/)
})

await check('Board never unlinks or auto-clears a held lock from age/session heuristics', async () => {
  const board = fs.readFileSync(path.join(SITE, 'scripts', 'panels', 'board.js'), 'utf8')
  const advanced = fs.readFileSync(path.join(SITE, 'scripts', 'board', 'task-advanced.js'), 'utf8')
  const api = fs.readFileSync(path.join(SITE, 'scripts', 'data', 'tasks-api.js'), 'utf8')
  const taskActions = fs.readFileSync(path.join(SITE, 'server', 'task-actions.js'), 'utf8')
  const taskSummary = fs.readFileSync(path.join(SITE, 'server', 'task-summary.js'), 'utf8')
  const en = localeText('en')
  const ru = localeText('ru')

  assert.doesNotMatch(board, /tasksApi\.clearLock/)
  assert.doesNotMatch(board, /clearLockFirst/)
  assert.doesNotMatch(board, /confirmClearLock/)
  assert.doesNotMatch(board, /function appendLockedTaskControls|function buildLockRecoveryButton/)
  assert.match(taskSummary, /var active = !!request \|\| !!\(lock && !stopped\)/)
  assert.match(taskSummary, /primaryAction\.resolve\(\{[\s\S]*?active: active/)
  assert.match(taskActions, /current\.enabled === false/)
  assert.match(api, /GET is phase one|Two-phase dead-owner recovery/)
  assert.match(api, /expectedLockHash: expectedLockHash/)
  assert.match(board, /inspectLockRecovery:[\s\S]*tasksApi\.loadTaskLockRecovery\(stem\)/)
  assert.match(board, /recoverLock:[\s\S]*tasksApi\.recoverTaskLock\(stem, expectedLockHash\)/)
  assert.match(advanced, /if \(inspectedHash\) release\(\)[\s\S]*else inspect\(\)/)
  assert.match(advanced, /options\.recoverLock\(expectedHash\)/)
  assert.doesNotMatch(advanced, /Date\.now|startedAt|updatedAt|session.*ended/i)
  assert.doesNotMatch(en, /Clear lock & re-run|Clear orphaned lock/)
  assert.doesNotMatch(ru, /Очистить лок и перезапустить|Очистить осиротевший лок/)
  assert.match(en, /does not repair task files or INDEX/)
  assert.match(ru, /Файлы задачи и INDEX это не чинит/)
  assert.match(en, /exact local process generation and writer tree/)
  assert.match(ru, /точного поколения локального процесса и дерева writer-процессов/)
})

await check('run-control exposes a caller-owned typed error recovery hook', async () => {
  const source = fs.readFileSync(path.join(SITE, 'scripts', 'run-control.js'), 'utf8')
  assert.match(source, /opts\.onError\(err\) === true/)
  assert.match(source, /if \(handled\) return/)
})

await check('task terminal accepts every input state without reviving stale mutation authority', async () => {
  const terminal = fs.readFileSync(path.join(SITE, 'scripts', 'terminal.js'), 'utf8')
  const sessions = fs.readFileSync(path.join(SITE, 'server', 'sessions.js'), 'utf8')
  const en = localeText('en')
  const ru = localeText('ru')

  assert.match(terminal, /function enableInput\(\)[\s\S]{0,180}els\.input\.disabled = false[\s\S]{0,100}els\.sendBtn\.disabled = false/)
  assert.match(terminal, /Composition and Send stay available in every state/)
  assert.match(terminal, /status\.queuedInputCount > 0/)
  assert.match(terminal, /r && r\.queued \? 'setup\.console\.inputQueued'/)
  assert.match(terminal, /cancelBtn\.disabled = !running \|\| \(running && cancelPending\)/)
  assert.match(terminal, /if \(!running\) cancelPending = false/)
  assert.match(terminal, /setup\.console\.endedReadOnly/)
  assert.doesNotMatch(terminal, /taskInputUnavailable|endedTask/)
  assert.match(terminal, /sendFailureCode \? runErrorMessage\(\{ kind: sendFailureCode \}\) : statusText/)
  assert.match(sessions, /idleContinuationFence = captureTaskIdleContinuationFence\(s\)/)
  assert.match(sessions, /inputReady: taskInputReady\(s\)/)
  assert.match(sessions, /lockOwnedBySession\(stem, s\.writerSessionId\)/)
  assert.match(sessions, /s\.writerLease !== writerLease/)
  assert.match(sessions, /CONVERSATION_ONLY_INITIAL_PROMPT/)
  assert.match(sessions, /'--safe-mode', '--tools', 'Read,Grep,Glob'/)
  assert.match(sessions, /preserveTranscript: true/)
  assert.match(sessions, /Once a context has pending input, preserve strict submission order/)
  assert.match(en, /Session ended — type to continue safely/)
  assert.match(ru, /Сессия завершена — напишите, чтобы безопасно продолжить/)
})

if (!process.exitCode) process.stdout.write(`OK: ${passed}/${total} task-integrity UI checks passed.\n`)
