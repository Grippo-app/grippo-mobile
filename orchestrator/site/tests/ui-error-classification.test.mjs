import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { readFileSync, readdirSync } from 'node:fs'
import { join } from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'

import '../scripts/i18n/en.js'
import { i18n } from '../scripts/i18n.js'
import { dictionaryFor } from './i18n-test-helpers.mjs'
import { requestJson } from '../scripts/data/request-json.js'
import { figmaActionError } from '../scripts/panels/figma.js'
import { presentFigmaError } from '../scripts/figma/error-presenter.js'
import { designFilters, errorMessage as designErrorMessage } from '../scripts/design/filters.js'
import { errorLabel as backendErrorMessage, normalizeSourceUrl } from '../scripts/panels/backend.js'
import { boardRequestError } from '../scripts/panels/board.js'
import {
  apiDriftFindingMessage,
  apiDriftFindingSuggestion,
  apiDriftSeverity,
  apiErrorMessage,
} from '../scripts/panels/api.js'
import { archmapErrorMessage } from '../scripts/panels/archmap.js'
import { syncFailureText } from '../scripts/figma/sync-errors.js'
import { runErrorMessage } from '../scripts/run-errors.js'
import { reviewerErrorMessage } from '../scripts/reviewer-errors.js'
import { cliErrorMessage } from '../scripts/cli-status.js'
import { appRunErrorMessage } from '../scripts/app-run-errors.js'
import { figmaSessionKind } from '../scripts/figma-session-labels.js'
import { terminal } from '../scripts/terminal.js'
import { figmaEnumText } from '../scripts/figma/enum-labels.js'
import {
  backendAuthText,
  backendPillText,
  backendStateText,
} from '../scripts/backend-labels.js'
import {
  evidenceIssueLabel,
  knownEvidenceIssueCode,
} from '../scripts/figma/evidence-issue-presenter.js'

const SITE = fileURLToPath(new URL('../', import.meta.url))
const require = createRequire(import.meta.url)
const syncErrors = require('../server/figma-sync-errors.js')
const screenTokenPlans = require('../server/screen-token-plans.js')
const taskPublication = require('../server/figma-task-publication.js')
const backlogCreate = require('../server/backlog-create.js')
const taskSummary = require('../server/task-summary.js')
const taskFiles = require('../server/task-files.js')
const shallowIntake = require('../server/shallow-intake.js')
const requests = require('../server/requests.js')
const locks = require('../server/locks.js')
const statusModel = require('../server/status.js')
const finalizations = require('../server/finalizations.js')
const syncHistory = require('../server/figma-sync-history.js')
const figmaTestJobs = require('../server/figma-test-job.js')
const tokenSourceIngestion = require('../server/token-source-ingestion.js')
const contractJobs = require('../server/contract-job.js')
const designTokenSources = require('../server/design-token-sources.js')
const designMappings = require('../server/design-mappings.js')
const designComponentMappings = require('../server/design-component-mappings.js')

async function withFetch(handler, action) {
  const previous = globalThis.fetch
  globalThis.fetch = handler
  try {
    return await action()
  } finally {
    globalThis.fetch = previous
  }
}

function response(body, status = 200) {
  return new Response(typeof body === 'string' ? body : JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json' },
  })
}

test('strict JSON transport distinguishes network, protocol, HTTP, and typed domain failures', async () => {
  await withFetch(() => { throw new TypeError('private synchronous detail') }, async () => {
    await assert.rejects(requestJson('/api/example'), (error) => error.code === 'fetch-failed' && error.status === 0)
  })
  await withFetch(async () => { throw new TypeError('private network detail') }, async () => {
    await assert.rejects(requestJson('/api/example'), (error) => error.code === 'fetch-failed' && error.message === 'fetch-failed')
  })
  await withFetch(async () => response('{broken'), async () => {
    await assert.rejects(requestJson('/api/example'), (error) => error.code === 'invalid-response' && error.status === 200)
  })
  await withFetch(async () => response({}, 404), async () => {
    await assert.rejects(requestJson('/api/example'), (error) => error.code === 'not-found' && error.status === 404)
  })
  await withFetch(async () => response({ detail: '/private/protocol/detail' }, 500), async () => {
    await assert.rejects(requestJson('/api/example'), (error) =>
      error.code === 'http-error' && error.status === 500 && !JSON.stringify(error).includes('/private/protocol/detail'))
  })
  await withFetch(async () => ({ ok: true, status: 200, text: async () => { throw new Error('private body detail') } }), async () => {
    await assert.rejects(requestJson('/api/example'), (error) => error.code === 'invalid-response' && error.status === 200)
  })
  await withFetch(async () => response({ error: 'task-integrity', detail: '/private/path', integrity: { ok: false } }, 409), async () => {
    await assert.rejects(requestJson('/api/example'), (error) => {
      assert.equal(error.code, 'task-integrity')
      assert.equal(error.kind, 'task-integrity')
      assert.deepEqual(error.integrity, { ok: false })
      assert.equal(Object.prototype.hasOwnProperty.call(error, 'body'), false)
      assert.equal(Object.prototype.hasOwnProperty.call(error, 'detail'), false)
      assert.equal(JSON.stringify(error).includes('/private/path'), false)
      return true
    })
  })
  await withFetch(async () => response({ ok: false, error: 'preview-stale' }), async () => {
    await assert.rejects(requestJson('/api/example'), (error) => error.code === 'preview-stale')
  })
  await withFetch(async () => response({ ok: true, value: 42 }), async () => {
    assert.deepEqual(await requestJson('/api/example'), { ok: true, value: 42 })
  })
})

test('strict JSON transport bounds a stalled request without exposing a new error code', async () => {
  let forwardedOptions
  await withFetch((_url, options) => {
    forwardedOptions = options
    return new Promise(() => {})
  }, async () => {
    const request = requestJson('/api/example', { cache: 'no-store', timeoutMs: 10 })
    const deadline = new Promise((_, reject) => {
      setTimeout(() => reject(new Error('request timeout was not enforced')), 250)
    })
    await assert.rejects(Promise.race([request, deadline]), (error) =>
      error.code === 'fetch-failed' && error.status === 0 && error.message === 'fetch-failed')
  })
  assert.equal(Object.prototype.hasOwnProperty.call(forwardedOptions, 'timeoutMs'), false)
  assert.equal(forwardedOptions.cache, 'no-store')
  assert.ok(forwardedOptions.signal instanceof AbortSignal)
  assert.equal(forwardedOptions.signal.aborted, true)
})

test('session diagnostics are closed localized events and never raw process text', () => {
  const secret = '/private/runtime/path: authentication token rejected'
  assert.notEqual(terminal.eventText({ kind: 'stderr', code: 'session-stderr-output', text: secret }, secret), secret)
  assert.notEqual(terminal.eventText({ kind: 'rate', code: 'session-rate-limited', text: secret }, secret), secret)
  assert.notEqual(terminal.eventText({ kind: 'error', code: 'future-internal-event', text: secret }, secret), secret)
  assert.notEqual(terminal.eventText({ kind: 'error', text: secret }, secret), secret)
  const unknown = terminal.eventText({ kind: 'error', code: 'future-internal-event' })
  assert.notEqual(terminal.eventText({ kind: 'error', code: 'prep-no-questions-continuation-refused' }), unknown)
  assert.notEqual(terminal.eventText({ kind: 'error', code: 'prep-no-questions-convergence-failed' }), unknown)

  const sessions = readFileSync(join(SITE, 'server', 'sessions.js'), 'utf8')
  assert.doesNotMatch(sessions, /pushEvent\(s, 'stderr', str/)
  assert.match(sessions, /pushEvent\(s, 'stderr', '', \{ code: 'session-stderr-output' \}\)/)
  assert.match(sessions, /pushEvent\(s, 'rate', '', \{ code: 'session-rate-limited' \}\)/)
})

test('Figma plan, job, history, and verification enums never render raw unknown keys', () => {
  const secret = 'future-state:/private/figma/path'
  for (const type of [
    'syncPlan', 'group', 'warning', 'syncState', 'groupState',
    'historyResult', 'integrationStatus', 'testPhase', 'testState', 'fileCandidate',
    'connectorState', 'connectorScope', 'recoveryState',
  ]) {
    const label = figmaEnumText(type, secret, { reads: 1, name: secret })
    assert.equal(label.includes(secret), false, type)
    assert.doesNotMatch(label, /^figma\./, type)
  }
})

test('Figma diagnostics use a closed localized field and enum vocabulary', () => {
  const source = readFileSync(join(SITE, 'scripts', 'figma', 'integration-view.js'), 'utf8')
  assert.match(source, /DIAGNOSTIC_LABEL_KEYS/)
  assert.match(source, /diagnosticValue\(key, diag\[key\]\)/)
  assert.doesNotMatch(source, /el\('dt', \{ text: key \}\)/)
  assert.doesNotMatch(source, /el\('dd', \{ text: String\(diag\[key\]\) \}\)/)
  for (const key of [
    'connectorState', 'connectorScope', 'connectorUrl', 'competingConnector',
    'checkedAt', 'syncRecoveryState', 'taskPublicationRecoveryState',
    'verificationGeneration', 'accountReceipt', 'generationPointer',
  ]) {
    assert.notEqual(i18n.t('figma.diagnostic.' + key), 'figma.diagnostic.' + key)
  }
})

test('unavailable Figma evidence inputs are not mislabeled as ordinary drift', () => {
  const summaryView = readFileSync(join(SITE, 'scripts', 'board', 'visual-evidence-summary-view.js'), 'utf8')
  const recoveryView = readFileSync(join(SITE, 'scripts', 'board', 'visual-evidence-recovery-view.js'), 'utf8')
  const evidence = readFileSync(join(SITE, 'server', 'figma-evidence.js'), 'utf8')
  assert.match(evidence, /taskSource\.readTask\(column, stem\)/)
  assert.match(evidence, /unavailable \|\| !bodies\.length/)
  assert.match(evidence, /reason: 'design-source-unavailable'/)
  assert.match(recoveryView, /board\.figmaEvidence\.cause\.inputUnavailable/)
  assert.match(summaryView, /board\.figmaEvidence\.inputUnavailableReady/)
  assert.match(i18n.t('board.figmaEvidence.cause.inputUnavailable', { names: 'screen-cache' }), /freshness is unknown/)
  assert.doesNotMatch(i18n.t('board.figmaEvidence.cause.inputUnavailable', { names: 'screen-cache' }), /changed after the check/)
})

test('Backend header and panel enums fail closed to localized unknown states', () => {
  const secret = 'future-state:/private/backend/path'
  for (const label of [
    backendStateText(secret),
    backendPillText(secret),
    backendAuthText(secret),
  ]) {
    assert.equal(label.includes(secret), false)
    assert.doesNotMatch(label, /^backend\./)
  }
  const reviewer = readFileSync(join(SITE, 'scripts', 'panels', 'reviewer.js'), 'utf8')
  assert.doesNotMatch(reviewer, /status\.codex\.reasonCode \|\| t\('codex\.value\.none'\)/)
  assert.doesNotMatch(reviewer, /t\('codex\.basis\.' \+ status\.review\.activeReviewerBasis\)/)
})

test('Backend source URL normalization strips secrets, emits ASCII hosts, and never changes source kind', () => {
  const idn = normalizeSourceUrl('https://пример.рф/docs?token=secret#fragment')
  assert.equal(idn.value, 'https://xn--e1afmkfd.xn--p1ai/docs')
  assert.equal(idn.pmatRejected, false)
  assert.equal(Object.prototype.hasOwnProperty.call(idn, 'sourceKind'), false)

  const uid = '12345678-aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee'
  const shared = normalizeSourceUrl(`https://www.postman.com/team/workspace/collection/${uid}/name?access_key=PMAT-secret`)
  assert.equal(shared.value, `https://api.getpostman.com/collections/${uid}`)
  assert.equal(shared.pmatRejected, true)

  const slug = normalizeSourceUrl('https://www.postman.com/team/workspace/collection/i2uqzpp/postman-api?foo=bar')
  assert.equal(slug.value, 'https://www.postman.com/team/workspace/collection/i2uqzpp/postman-api')
  assert.equal(slug.pmatRejected, false)
})

test('user-facing presenters localize exact domain codes and never expose diagnostic detail', () => {
  const secret = '/Users/private/repository/token.json'
  const cases = [
    figmaActionError({ kind: 'bad-figma-test-request', detail: secret }),
    figmaActionError({ kind: 'TOKEN_SOURCE_HEALTH_RESERVATION_FAILED', detail: secret }),
    designErrorMessage('TOKEN_MAPPING_OPERATION_CONFLICT'),
    designErrorMessage({ code: 'TOKEN_MAPPING_OPERATION_CONFLICT', message: secret }),
    designErrorMessage({ code: '__unknown__', message: secret }),
    designErrorMessage('component-binding-symbol-unresolved'),
    backendErrorMessage('source-url-userinfo-forbidden'),
    backendErrorMessage('credential-state-invalid'),
    boardRequestError({ kind: 'bad-title', detail: secret }),
    boardRequestError({ kind: 'unregistered-code', detail: secret }),
    apiErrorMessage({ kind: 'task-integrity', detail: secret }),
    apiErrorMessage({ kind: 'generation-artifact-hash-mismatch', detail: secret }),
    apiErrorMessage({ kind: 'contract-drift-invalid', detail: secret }),
    archmapErrorMessage({ kind: 'invalid-response', detail: secret }),
    runErrorMessage({ kind: 'session-start-refused', detail: secret }),
    reviewerErrorMessage({ kind: 'reviewer-unavailable', detail: secret }, 'status'),
    cliErrorMessage({ kind: 'cli-login-start-refused', detail: secret }),
  ]
  for (const message of cases) {
    assert.equal(typeof message, 'string')
    assert.ok(message.length > 12)
    assert.equal(message.includes(secret), false)
    assert.doesNotMatch(message, /^(?:bad-|TOKEN_|COMPONENT_|HTTP |unregistered-code)/)
  }
  assert.notEqual(figmaSessionKind('figma:future-action'), 'future-action')
  assert.notEqual(figmaSessionKind('not-a-figma-session'), 'not-a-figma-session')
  const unknownFigma = presentFigmaError('__unknown__')
  assert.equal(unknownFigma.action, null)
  assert.equal(unknownFigma.actionKey, null)
  assert.equal(presentFigmaError('integration-failed').action, null)
  assert.equal(presentFigmaError('connector-conflict').action, null)
  assert.equal(presentFigmaError('quota-risk').action, 'changeAccount')
})

test('every registered app-run error has exact English, Russian, and Ukrainian copy', () => {
  const presenter = readFileSync(join(SITE, 'scripts', 'app-run-errors.js'), 'utf8')
  const codes = [...presenter.matchAll(/^  '([^']+)': 1,/gm)].map((match) => match[1])
  assert.ok(codes.length > 70)
  for (const locale of ['en', 'ru', 'uk']) {
    const dictionary = dictionaryFor(locale)
    const translated = new Set(Object.keys(dictionary)
      .filter((key) => key.startsWith('appRun.error.'))
      .map((key) => key.slice('appRun.error.'.length)))
    assert.deepEqual(codes.filter((code) => !translated.has(code)), [], locale)
  }
})

test('the latest Figma connection-check attempt is separate and uses typed localized state', () => {
  const source = readFileSync(join(SITE, 'scripts', 'figma', 'integration-view.js'), 'utf8')
  assert.match(source, /var testAttempt = model\.test/)
  assert.match(source, /presentFigmaError\(testAttempt\.reasonCode\)\.bodyKey/)
  assert.match(source, /figmaEnumText\('testState', testAttempt\.state\)/)
  assert.doesNotMatch(source, /text:\s*testAttempt\.reasonCode/)
  for (const state of ['running', 'completed', 'failed', 'unknown']) {
    assert.notEqual(i18n.t('figma.testState.' + state), 'figma.testState.' + state)
  }
  assert.doesNotMatch(i18n.t('figma.syncIntent.blocked.account-stale'), /\bcomparison\b/i)
  assert.doesNotMatch(i18n.t('figma.syncIntent.blocked.access-unverified'), /\bcomparison\b/i)
})

test('generic UI errors do not redirect local failures to Figma connection recovery or expose request paths', () => {
  const requestTransport = readFileSync(join(SITE, 'scripts', 'data', 'request-json.js'), 'utf8')
  const taskTransport = readFileSync(join(SITE, 'scripts', 'data', 'tasks-api.js'), 'utf8')
  const board = readFileSync(join(SITE, 'scripts', 'panels', 'board.js'), 'utf8')
  assert.doesNotMatch(requestTransport, /error\.body\s*=/)
  assert.doesNotMatch(taskTransport, /path:\s*path/)
  assert.doesNotMatch(board, /board\.error\.fileMissing',\s*\{\s*path:/)
  assert.doesNotMatch(board, /err\.path|invalid-index-json|board\.error\.indexMissing|board\.error\.indexInvalid/)
  assert.doesNotMatch(i18n.t('figma.error.unknown.body'), /\b(?:test|connection)\b/i)
  assert.doesNotMatch(i18n.t('figma.requestError.unknown'), /\b(?:test|connection)\b/i)
  assert.doesNotMatch(i18n.t('board.error.fileMissing'), /\{path\}/)
})

test('unavailable runtime lock state is explicit, localized, and never presented as empty', () => {
  const serverLocks = readFileSync(join(SITE, 'server', 'locks.js'), 'utf8')
  const serverState = readFileSync(join(SITE, 'server', 'state.js'), 'utf8')
  const serverRequests = readFileSync(join(SITE, 'server', 'requests.js'), 'utf8')
  const browserStatus = readFileSync(join(SITE, 'scripts', 'status.js'), 'utf8')
  assert.doesNotMatch(serverLocks, /function readLocks\(\)/)
  assert.match(serverLocks, /available: false/)
  assert.match(serverLocks, /runtime-locks-unavailable/)
  assert.match(serverLocks, /runtime-locks-entry-limit/)
  assert.match(serverState, /locksAvailable: lockRead\.available/)
  assert.match(serverState, /locksErrorCode: lockRead\.errorCode/)
  assert.match(serverState, /requestsAvailable: requestRead\.ok/)
  assert.match(serverState, /requestsErrorCode: requestRead\.ok \? null : requestRead\.code/)
  assert.doesNotMatch(serverRequests, /function readRequests\(|function readActiveClaims\(/)
  assert.doesNotMatch(serverRequests, /scanRequests\(strict\)|scanActiveClaims\(strict\)/)
  assert.match(serverRequests, /return \{ ok: false, code: 'request-record-invalid', rows: \[\] \}/)
  assert.match(serverState, /function readIndexSnapshot\(\)/)
  assert.doesNotMatch(serverState, /function readIndexStems\(\)|function readIndexColumns\(\)/)
  assert.match(browserStatus, /lk\.available !== true/)
  assert.match(browserStatus, /status\.worker\.unknownHint/)
  assert.match(browserStatus, /errorCode === 'request-record-unsafe'/)
  for (const key of [
    'status.locks.unavailable',
    'status.locks.entryLimit',
    'status.worker.unknown',
    'status.worker.unknownHint',
    'status.queue.unavailable',
    'status.queue.invalid',
    'status.queue.limit',
    'status.activity.unknownHint',
  ]) {
    const message = i18n.t(key)
    assert.notEqual(message, key)
    assert.ok(message.length > 12, key)
  }

  const unavailable = statusModel.computeStatus({
    locks: [],
    locksAvailable: false,
    locksErrorCode: 'runtime-locks-unavailable',
    requests: [],
  })
  assert.equal(unavailable.locks.available, false)
  assert.equal(unavailable.locks.errorCode, 'runtime-locks-unavailable')
  assert.equal(unavailable.locks.count, 0)

  const tracker = statusModel.createActivityTracker()
  tracker.update({ locks: [], locksAvailable: true, requests: [] }, 1000)
  tracker.update({ locks: [], locksAvailable: false, requests: [] }, 2000)
  tracker.update({
    locks: [{ stem: 'TASK_1_example', stage: 'orchestrator', startedAt: null }],
    locksAvailable: true,
    requests: [],
  }, 3000)
  assert.equal(tracker.snapshot().lastActivityAt, null, 'recovery establishes a baseline, not a fabricated lock event')
  tracker.update({
    locks: [{ stem: 'TASK_2_next', stage: 'orchestrator', startedAt: null }],
    locksAvailable: true,
    requests: [],
  }, 4000)
  assert.equal(tracker.snapshot().lastActivityAt, 4000)

  const requestTracker = statusModel.createActivityTracker()
  requestTracker.update({ locks: [], locksAvailable: true, requests: [], requestsAvailable: true }, 1000)
  requestTracker.update({ locks: [], locksAvailable: true, requests: [], requestsAvailable: false }, 2000)
  requestTracker.update({
    locks: [],
    locksAvailable: true,
    requests: [{ id: '1000-recovered' }],
    requestsAvailable: true,
  }, 3000)
  assert.equal(requestTracker.snapshot().lastActivityAt, null, 'queue recovery establishes a baseline')
  requestTracker.update({
    locks: [],
    locksAvailable: true,
    requests: [],
    requestsAvailable: true,
  }, 4000)
  assert.equal(requestTracker.snapshot().lastDrainedAt, 4000)
})

test('obsolete Figma recovery actions and unreachable error copy are absent', () => {
  const presenter = readFileSync(join(SITE, 'scripts', 'figma', 'error-presenter.js'), 'utf8')
  const panel = readFileSync(join(SITE, 'scripts', 'panels', 'figma.js'), 'utf8')
  assert.doesNotMatch(presenter, /plan-stale|sync-partial|recalculate|reviewDetails|fixConnection|reviewPlan/)
  assert.doesNotMatch(panel, /recalculate|reviewDetails|fixConnection|reviewPlan/)
  for (const key of [
    'figma.action.fixConnection',
    'figma.action.reviewPlan',
    'figma.action.recalculate',
    'figma.action.reviewDetails',
    'figma.error.plan-stale.title',
    'figma.error.sync-partial.title',
  ]) {
    assert.equal(i18n.t(key), key)
  }
})

test('Figma recovery controls obey the server-owned action gates', () => {
  const source = readFileSync(join(SITE, 'scripts', 'figma', 'integration-view.js'), 'utf8')
  assert.match(source, /function recoveryActionAllowed\(model, action\)/)
  assert.match(source, /actions\.canTest === true/)
  assert.match(source, /actions\.canChangeAccount !== false/)
  assert.match(source, /actions\.canChangeFile === true/)
  assert.match(source, /!recoveryActionAllowed\(model, view\.action\)/)
})

test('Design source and parser failures remain visible typed blockers instead of empty success', () => {
  const state = readFileSync(join(SITE, 'server', 'state.js'), 'utf8')
  const designCatalog = readFileSync(join(SITE, 'server', 'design-catalog.js'), 'utf8')
  const parser = readFileSync(join(SITE, '..', 'figma', 'scripts', 'design-parser.cjs'), 'utf8')
  const taskSource = readFileSync(join(SITE, '..', 'tasks', 'task-source-contract.cjs'), 'utf8')
  assert.doesNotMatch(state, /catch \([^)]*\) \{ return \{ entries: \[\], issues: \[\], hasPullable: false \}; \}/)
  assert.match(state, /kind: 'DESIGN_PARSER_UNAVAILABLE'/)
  assert.match(state, /kind: 'DESIGN_SOURCE_UNAVAILABLE'/)
  assert.match(state, /MALFORMED_DESIGN_KINDS\[x\.kind\] \|\| DESIGN_OBSERVATION_FAILURES\[x\.kind\]/)
  assert.doesNotMatch(parser, /function auditedNoneSubjects[\s\S]*?catch \([^)]*\) \{ return \[\]; \}/)
  assert.doesNotMatch(parser, /function auditedNoneCount[\s\S]*?catch \([^)]*\) \{ return 0; \}/)
  assert.doesNotMatch(taskSource, /function realSourceHeadings[\s\S]*?catch \([^)]*\) \{ return \[\]; \}/)
  for (const code of ['design_parser_unavailable', 'design_source_unavailable']) {
    const label = i18n.t('taskDetails.designIssue.kind.' + code)
    assert.notEqual(label, 'taskDetails.designIssue.kind.' + code)
    assert.ok(label.length > 20)
  }
  const summary = readFileSync(join(SITE, 'server', 'task-summary.js'), 'utf8')
  const details = readFileSync(join(SITE, 'scripts', 'board', 'task-overview.js'), 'utf8')
  assert.match(summary, /figmaDesignIssues:\s*figmaDesignIssues/)
  assert.match(details, /taskDetails\.designIssue\.kind\./)
})

test('task-integrity machine codes and severities stay closed and severity copy is localized', () => {
  const board = readFileSync(join(SITE, 'scripts', 'panels', 'board.js'), 'utf8')
  const boardHealth = readFileSync(join(SITE, 'scripts', 'board', 'board-health.js'), 'utf8')
  const server = readFileSync(join(SITE, 'server', 'task-integrity.js'), 'utf8')
  assert.match(board, /createBoardHealthController\(\{/)
  assert.doesNotMatch(boardHealth, /text:\s*item\.severity/)
  assert.match(boardHealth, /integritySeverityLabel\(item\.severity\)/)
  assert.match(server, /\^\[A-Z\]\[A-Z0-9_\]\{2,79\}\$/)
  for (const severity of ['info', 'warning', 'error', 'blocker']) {
    const label = i18n.t('board.integrity.severity.' + severity)
    assert.notEqual(label, 'board.integrity.severity.' + severity)
  }
})

test('every registered Design error code has a localized message', () => {
  const filters = readFileSync(join(SITE, 'scripts', 'design', 'filters.js'), 'utf8')
  const start = filters.indexOf('function errorMessage')
  const end = filters.indexOf('function request(', start)
  const codes = [...filters.slice(start, end).matchAll(/'([^']+)': 1/g)].map((match) => match[1])
  for (const code of codes) {
    assert.notEqual(designErrorMessage(code), designErrorMessage('__unknown__'), code)
  }
})

test('unknown Design enum states collapse to localized unknown instead of raw values', () => {
  const secret = 'future-state:/private/provider/path'
  for (const presenter of [
    designFilters.statusText,
    designFilters.tokenStatusText,
    designFilters.tokenKindText,
    designFilters.componentStatusText,
    designFilters.componentKindText,
  ]) {
    assert.equal(presenter(secret).includes(secret), false)
  }
  for (const [prefix, unknownKey] of [
    ['design.readiness.', 'design.readiness.unknown'],
    ['design.freshness.', 'design.freshness.unknown'],
    ['design.finding.', 'design.finding.unknown'],
    ['design.change.', 'design.change.unknown'],
    ['design.limitation.', 'design.limitation.unknown'],
    ['design.surface.', 'design.surface.unknown'],
    ['design.previewReason.', 'design.previewReason.unknown'],
    ['design.comparison.', 'design.comparison.unknown'],
    ['design.tokenSource.state.', 'design.tokenSource.state.unknown'],
    ['design.tokenSource.action.', 'design.tokenSource.action.unknown'],
    ['design.tokenSource.mutationState.', 'design.tokenSource.mutationState.unknown'],
    ['design.tokenSource.actionReason.', 'design.tokenSource.actionReason.unknown'],
    ['design.tokenSource.failure.', 'design.tokenSource.failure.unknown'],
    ['board.column.', 'board.column.unknown'],
  ]) {
    const label = designFilters.localizedEnum(prefix, secret, unknownKey)
    assert.equal(label.includes(secret), false, prefix)
    assert.equal(label, i18n.t(unknownKey), prefix)
  }
  for (const code of [
    'TOKEN_CAPTURE_CANCELLED',
    'TOKEN_SOURCE_CAPTURE_INCOMPLETE',
    'TOKEN_GENERATION_RESYNC_REQUIRED',
    'TOKEN_HEALTH_RECOVERY_REQUIRED',
    'TOKEN_CAPTURE_FAILED',
    'TOKEN_CAPTURE_INTERRUPTED',
  ]) {
    const label = designFilters.localizedEnum(
      'design.tokenSource.failure.',
      code,
      'design.tokenSource.failure.unknown',
    )
    assert.notEqual(label, i18n.t('design.tokenSource.failure.unknown'), code)
    assert.equal(label.includes(code), false, code)
  }
  for (const file of [
    'scripts/design/filters.js',
    'scripts/design/components.js',
    'scripts/design/entity-drawer.js',
    'scripts/design/overview.js',
    'scripts/design/surfaces.js',
    'scripts/design/tokens.js',
  ]) {
    const source = readFileSync(join(SITE, file), 'utf8')
    assert.doesNotMatch(source, /String\((?:status|kind|value)\s*\|\|\s*t\('design\.unknown'\)\)/, file)
    assert.doesNotMatch(source, /return\s+value === 'light' \|\| value === 'dark'[^;]+:\s*String\(value\)/, file)
  }
  const overview = readFileSync(join(SITE, 'scripts', 'design', 'overview.js'), 'utf8')
  assert.doesNotMatch(overview, /design-readiness--'\s*\+\s*bannerState/)
  assert.match(overview, /design-readiness--'\s*\+\s*designFilters\.cssToken\(bannerState\)/)
  for (const forbidden of [
    "t('design.readiness.' + data.readiness.state)",
    "t('design.freshness.' + data.freshness.state)",
    "t('design.finding.' + item.kind",
    "t('design.change.' + item.kind)",
    "t('design.limitation.' + item)",
    "t('design.surface.' + row.type)",
    "t('design.previewReason.' + preview.reason)",
    "t('design.comparison.' + comparison.mode)",
    "t('design.tokenSource.state.' + state)",
    "t('design.tokenSource.action.' + row.action)",
    "t('design.tokenSource.mutationState.' + row.state)",
    "t('design.tokenSource.actionReason.' + source.actions.reason)",
    "t('design.tokenSource.failure.' + source.latestFailure.code)",
    "t('board.column.' + task.column)",
  ]) {
    const sources = [
      'scripts/design/entity-drawer.js',
      'scripts/design/overview.js',
      'scripts/design/surfaces.js',
      'scripts/design/tokens.js',
    ].map((file) => readFileSync(join(SITE, file), 'utf8')).join('\n')
    assert.equal(sources.includes(forbidden), false, forbidden)
  }
})

test('every statically produced Design API error has exact localized copy', () => {
  const codes = new Set()
  for (const entry of readdirSync(join(SITE, 'server'), { withFileTypes: true })) {
    if (!entry.isFile() || !/^design-.*\.js$/.test(entry.name)) continue
    const source = readFileSync(join(SITE, 'server', entry.name), 'utf8')
    for (const match of source.matchAll(/\berror:\s*'([^']+)'/g)) codes.add(match[1])
  }
  const http = readFileSync(join(SITE, 'server', 'http.js'), 'utf8')
  const start = http.indexOf('function designQuery')
  const end = http.indexOf('function handleFigmaPixelReview', start)
  assert.ok(start >= 0 && end > start)
  for (const match of http.slice(start, end).matchAll(/\berror:\s*'([^']+)'/g)) codes.add(match[1])
  const unknown = designErrorMessage('__unknown__')
  for (const code of codes) {
    assert.notEqual(designErrorMessage(code), unknown, code)
  }
})

test('Board response-contract failures have exact localized messages', () => {
  const codes = [
    'invalid-task-summary-json',
    'invalid-task-integrity-json',
    'invalid-lock-recovery-json',
    'invalid-drop-impact-json',
    'invalid-log-json',
    'invalid-figma-evidence-json',
    'invalid-backlog-source-json',
    'invalid-figma-history-json',
    'bad-cursor',
    'sync-history-index-invalid',
    'sync-history-record-invalid',
    'task-action-operation-invalid',
    'task-action-failed',
    'task-summary-not-found',
    'task-index-invalid',
    'task-index-not-fresh',
    'task-state-unavailable',
    'task-summary-cursor-stale',
    'bad-lock-recovery-request',
    'task-lock-recovery-unavailable',
    'task-lock-recovery-internal',
    'request-reservation-release-failed',
    'dedup-key-conflict',
    'dedup-key-active',
    'task-session-busy',
    'finalization-corrupt',
    'finalization-changed',
    'finalization-unavailable',
    'finalization-resume-failed',
    'finalization-state-invalid',
    'finalization-owner-unavailable',
    'finalization-runtime-unsupported',
    'finalization-timeout',
    'finalization-process-failed',
    'finalization-step-failed',
    'design-generation-invalid',
    'screen-cache-invalid',
    'screen-drift-invalid',
    'ship-drift-invalid',
  ]
  const unknown = boardRequestError({ kind: '__unknown__' })
  for (const code of codes) {
    assert.notEqual(boardRequestError({ kind: code }), unknown, code)
  }
})

test('every statically produced Figma evidence issue is classified and localized', () => {
  const roots = [
    join(SITE, '..', 'figma', 'scripts'),
    join(SITE, 'server', 'figma-evidence.js'),
  ]
  const files = []
  function visit(target) {
    if (target.endsWith('.js') || target.endsWith('.mjs')) {
      files.push(target)
      return
    }
    for (const entry of readdirSync(target, { withFileTypes: true })) {
      if (entry.isDirectory()) visit(join(target, entry.name))
      else if (entry.isFile() && /\.(?:mjs|js)$/.test(entry.name)) files.push(join(target, entry.name))
    }
  }
  for (const root of roots) visit(root)
  const codes = new Set()
  for (const file of files) {
    const source = readFileSync(file, 'utf8')
    for (const match of source.matchAll(/\bissue\([^,\n]+,\s*'([A-Z][A-Z0-9_]+)'/g)) codes.add(match[1])
    for (const match of source.matchAll(/\bissueKind\s*:\s*'([A-Z][A-Z0-9_]+)'/g)) codes.add(match[1])
  }
  const unknown = evidenceIssueLabel('__unknown__')
  assert.ok(codes.size > 150, 'the production evidence issue surface was not discovered')
  for (const code of codes) {
    assert.equal(knownEvidenceIssueCode(code), true, code)
    assert.notEqual(evidenceIssueLabel(code), unknown, code)
  }
  const recoveryView = readFileSync(join(SITE, 'scripts', 'board', 'visual-evidence-recovery-view.js'), 'utf8')
  assert.match(recoveryView, /evidenceIssueLabel\(rawKind\)/)
  assert.doesNotMatch(recoveryView, /if \(issue\.message\)/)
})

test('every statically produced immediate Figma API error has localized copy', () => {
  const files = [
    'server/figma.js',
    'server/figma-test-job.js',
    'server/figma-sync.js',
    'server/figma-sync-history.js',
    'server/figma-generation.js',
    'server/figma-screens.js',
    'server/figma-session-actions.js',
    'server/figma-task-publication.js',
    'server/project-config-update.js',
  ]
  const codes = new Set()
  for (const file of files) {
    const source = readFileSync(join(SITE, file), 'utf8')
    for (const match of source.matchAll(/\berror:\s*'([^']+)'/g)) codes.add(match[1])
  }
  const http = readFileSync(join(SITE, 'server', 'http.js'), 'utf8')
  for (const [startMarker, endMarker] of [
    ['function handleFigmaIntegration', 'function designQuery'],
    ['function handleFigmaRecheck', '// --- Generic interactive sessions'],
  ]) {
    const start = http.indexOf(startMarker)
    const end = http.indexOf(endMarker, start)
    assert.ok(start >= 0 && end > start, `missing Figma HTTP region: ${startMarker}`)
    for (const match of http.slice(start, end).matchAll(/\berror:\s*'([^']+)'/g)) codes.add(match[1])
  }
  for (const code of [
    'figma-drift-domain-invalid',
    'TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE',
    'generation-retention-directory-invalid',
    'generation-retention-entry-invalid',
    'generation-retention-manifest-invalid',
    'generation-retention-cleanup-failed',
    'project-config-unsafe',
    'project-config-frontmatter-missing',
    'project-config-frontmatter-malformed',
    'writer-lease-unavailable',
    'writer-lease-update-failed',
    'session-busy',
    'session-start-refused',
    'TOKEN_TASK_STEM_INVALID',
    'TOKEN_TASK_SOURCE_UNSAFE',
    'TOKEN_TASK_SOURCE_MISSING',
    'TOKEN_TASK_BINDING_UNAVAILABLE',
    'TOKEN_TASK_FILE_SCOPE_MISMATCH',
    'TOKEN_TASK_CONTEXT_INVALID',
    'TOKEN_SOURCE_SCOPE_MISMATCH',
    'TOKEN_TASK_DESIGN_INVALID',
    'TOKEN_TASK_SOURCE_LIMIT_EXCEEDED',
    'TOKEN_TASK_SOURCE_DUPLICATE',
    'TOKEN_TASK_PLAN_INVALID',
    'TOKEN_TASK_PLAN_WRITE_FAILED',
    'TOKEN_TASK_PLAN_UNAVAILABLE',
    'bad-sync-job-id',
    'sync-job-not-found',
    'figma-client-prompt-forbidden',
    'bad-figma-action-request',
  ]) codes.add(code)
  const unknown = figmaActionError({ kind: '__unknown__' })
  for (const code of codes) {
    assert.notEqual(figmaActionError({ kind: code }), unknown, code)
  }
})

test('screen planning and task publication never promote arbitrary exception text to public codes', () => {
  assert.equal(
    screenTokenPlans.prepareErrorCode(new Error('TOKEN_TASK_SOURCE_MISSING')),
    'TOKEN_TASK_SOURCE_MISSING'
  )
  assert.equal(
    screenTokenPlans.prepareErrorCode(new Error('/private/workspace/sensitive.txt')),
    'TOKEN_TASK_PLAN_UNAVAILABLE'
  )
  assert.equal(
    taskPublication.publicationErrorCode(new Error('task-publication-completion-proof-missing')),
    'task-publication-completion-proof-missing'
  )
  assert.equal(
    taskPublication.publicationErrorCode(new Error('/private/workspace/sensitive.txt')),
    'task-publication-validation-failed'
  )
})

test('task and recovery owners collapse arbitrary internal failures before HTTP publication', () => {
  const secret = '/private/workspace/sensitive.txt'
  assert.equal(backlogCreate.publicCreateErrorCode({ code: secret }), 'create-failed')
  assert.equal(backlogCreate.publicEditErrorCode({ code: secret }), 'backlog-edit-failed')
  assert.equal(backlogCreate.publicCreateErrorCode({ code: 'bad-idempotency-key' }), 'bad-task-create-request')
  assert.equal(backlogCreate.publicEditErrorCode({ code: 'bad-edit-input' }), 'bad-backlog-edit-request')
  assert.equal(backlogCreate.publicCreateErrorCode({ code: 'create-stdin-failed' }), 'create-stdin-failed')
  assert.equal(designTokenSources.publicErrorCode({ message: secret }), 'TOKEN_SOURCE_MUTATION_FAILED')
  assert.equal(
    designTokenSources.publicErrorCode({ message: 'TOKEN_SOURCE_MUTATION_DIRECTORY_UNSAFE' }),
    'TOKEN_SOURCE_MUTATION_DIRECTORY_UNSAFE'
  )
  assert.equal(designMappings.publicErrorCode({ message: secret }), 'token-mapping-write-failed')
  assert.equal(designComponentMappings.publicErrorCode({ message: secret }), 'component-mapping-write-failed')
  assert.equal(taskSummary.publicErrorCode({ code: secret }), 'task-summary-unavailable')
  assert.equal(taskFiles.publicErrorCode({ code: secret }), 'task-file-unavailable')
  assert.equal(shallowIntake.publicErrorCode({ code: secret }, 'intake-retry-failed'), 'intake-retry-failed')
  assert.equal(shallowIntake.publicFailureCode(secret), 'INTAKE_FAILED')
  const intakeProjection = shallowIntake.publicProjection({
    version: 1, stem: 'TASK_1_example', sourceHash: 'sha256:' + '1'.repeat(64),
    createdAt: '2026-07-24T00:00:00.000Z', status: 'failed',
    requestId: 'intake-' + '2'.repeat(32), attempt: 1,
    errorCode: secret, retryable: true
  })
  assert.equal(intakeProjection.errorCode, 'INTAKE_FAILED')
  assert.equal(JSON.stringify(intakeProjection).includes(secret), false)
  assert.equal(requests.publicReservationErrorCode({ code: secret }), 'request-reservation-failed')
  assert.equal(requests.publicCancellationErrorCode({ code: secret }), 'request-cancel-failed')
  assert.equal(locks.publicRecoveryReasonCode(secret), 'LOCK_RECOVERY_FAILED')
  assert.equal(finalizations.publicErrorCode(secret), 'finalization-unavailable')
  assert.equal(figmaTestJobs.publicReasonCode(secret), 'integration-failed')
  assert.equal(figmaTestJobs.publicReasonCode('figma-action-timeout'), 'connection-test-timeout')
  const tokenCodes = { TOKEN_ERROR_CODES: { TOKEN_SOURCE_SCOPE_MISMATCH: 'TOKEN_SOURCE_SCOPE_MISMATCH' } }
  assert.equal(
    tokenSourceIngestion._retryableCode({ message: 'TOKEN_SOURCE_SCOPE_MISMATCH' }, tokenCodes),
    'TOKEN_SOURCE_SCOPE_MISMATCH'
  )
  assert.equal(
    tokenSourceIngestion._retryableCode({ message: 'TOKEN_SOURCE_SCOPE_MISMATCH: ' + secret }, tokenCodes),
    'TOKEN_TASK_INGESTION_FAILED'
  )
  const projected = finalizations.publicProjection({
    version: 1, stem: 'TASK_1_example', status: 'corrupt', state: 'corrupt',
    phase: null, observedColumn: 'unknown', revision: null, etag: null,
    createdAt: null, updatedAt: null, recoveryRunning: false, recoverable: false,
    errorCode: secret, errorMessage: secret
  })
  assert.equal(projected.errorCode, 'finalization-unavailable')
  assert.equal(Object.prototype.hasOwnProperty.call(projected, 'errorMessage'), false)
  assert.equal(JSON.stringify(projected).includes(secret), false)
  const historyProjection = syncHistory.publicRecord({
    schemaVersion: 1, id: 'fsj-' + '1'.repeat(32),
    startedAt: '2026-07-24T00:00:00.000Z', finishedAt: '2026-07-24T00:00:01.000Z',
    committedGenerationId: null, accountFingerprint: 'sha256:' + '2'.repeat(64),
    fileKeyFingerprint: 'sha256:' + '3'.repeat(64), planGroups: ['tokens'],
    groups: [{ group: 'tokens', status: 'failed', updated: 0, unchanged: 0, warnings: 0 }],
    result: 'failed', errorCode: 'token-sync-failed', durationMs: 1000,
    messages: [secret]
  })
  assert.equal(Object.prototype.hasOwnProperty.call(historyProjection, 'messages'), false)
  assert.equal(JSON.stringify(historyProjection).includes(secret), false)
})

test('JSON transport failures stay distinct from domain-owner failures at HTTP boundaries', () => {
  const http = readFileSync(join(SITE, 'server', 'http.js'), 'utf8')
  for (const owner of [
    'reviewerMod.settings',
    'designTokenSourcesMod.mutate',
    'designMappingsMod.mutate',
    'designComponentMappingsMod.mutate',
    'designTaskActionsMod.create',
    'backendCredentialsMod.mutate',
    'contractJobMod.startProbe',
    'contractJobMod.startRefresh',
  ]) {
    assert.equal(http.includes(`readJsonBody(req).then(${owner}).then(`), false, owner)
  }
  assert.match(http, /designTokenSourcesMod\.publicErrorCode\(error\)/)
  assert.match(http, /designMappingsMod\.publicErrorCode\(error\)/)
  assert.match(http, /designComponentMappingsMod\.publicErrorCode\(error\)/)
})

test('every statically produced Backend action error has localized copy', () => {
  const files = [
    'server/backend-environments.js',
    'server/backend-credentials.js',
    'server/backend-integration.js',
    'server/contract-job.js',
    'server/contract-generation.js',
    'server/contract-history.js',
  ]
  const codes = new Set()
  for (const file of files) {
    const source = readFileSync(join(SITE, file), 'utf8')
    for (const match of source.matchAll(/\berror:\s*'([^']+)'/g)) codes.add(match[1])
  }
  const sidecar = readFileSync(
    join(SITE, '..', 'api-contract', 'scripts', 'backend-action.mjs'),
    'utf8'
  )
  for (const match of sidecar.matchAll(/typedError\('([^']+)'/g)) codes.add(match[1])
  const safeMessageStart = sidecar.indexOf('function safeMessage')
  const safeMessageEnd = sidecar.indexOf('function atomicPrivateWrite', safeMessageStart)
  for (const match of sidecar.slice(safeMessageStart, safeMessageEnd).matchAll(/'([a-z][a-z0-9-]+)':\s*'/g)) {
    codes.add(match[1])
  }
  const unknown = backendErrorMessage('__unknown__')
  codes.add('bad-job-id')
  codes.add('job-not-found')
  codes.add('job-interrupted')
  codes.add('generation-publication-incomplete')
  codes.add('sidecar-output-limit')
  codes.add('job-progress-limit')
  codes.add('sidecar-warning')
  for (const code of codes) {
    assert.notEqual(backendErrorMessage(code), unknown, code)
  }
})

test('every static server error is localized or contained by an exact public projection', () => {
  const presenters = [
    (code) => figmaActionError({ kind: code }),
    (code) => designErrorMessage(code),
    (code) => backendErrorMessage(code),
    (code) => boardRequestError({ kind: code }),
    (code) => apiErrorMessage({ kind: code }),
    (code) => archmapErrorMessage({ kind: code }),
    (code) => runErrorMessage({ kind: code }),
    (code) => reviewerErrorMessage({ kind: code }, 'status'),
    (code) => cliErrorMessage({ kind: code }),
    (code) => appRunErrorMessage({ kind: code }),
  ]
  const unknown = presenters.map((presenter) => presenter('__unregistered__'))
  const codes = new Set()
  for (const entry of readdirSync(join(SITE, 'server'), { withFileTypes: true })) {
    if (!entry.isFile() || !entry.name.endsWith('.js')) continue
    const source = readFileSync(join(SITE, 'server', entry.name), 'utf8')
    for (const match of source.matchAll(/\berror\s*:\s*['"]([^'"]+)['"]/g)) codes.add(match[1])
  }
  const unpresented = [...codes].filter((code) =>
    presenters.every((presenter, index) => presenter(code) === unknown[index])
  ).sort()
  assert.deepEqual(unpresented, ['invalid-session-key'])

  const sessions = readFileSync(join(SITE, 'server', 'sessions.js'), 'utf8')
  const http = readFileSync(join(SITE, 'server', 'http.js'), 'utf8')
  const arch = readFileSync(join(SITE, 'server', 'arch.js'), 'utf8')
  const archPanel = readFileSync(join(SITE, 'scripts', 'panels', 'archmap.js'), 'utf8')
  assert.match(sessions, /error: 'invalid-session-key'/)
  assert.match(http, /function publicSessionStartError\(status\)[\s\S]{0,240}SESSION_START_ERRORS\[code\]/)
  assert.match(http, /!st \|\| !st\.running \|\| st\.error[\s\S]{0,240}error: publicSessionStartError\(st\)/)
  assert.doesNotMatch(arch, /error: 'parse-error'/)
  assert.match(archPanel, /overviewData\.error[\s\S]{0,160}t\('archmap\.parseError'\)/)
})

test('Backend job projection publishes only closed codes and strips diagnostic prose', () => {
  const secret = '/private/workspace/credential.txt'
  assert.equal(contractJobs.publicFailureCode(secret), 'sidecar-failed')
  assert.deepEqual(contractJobs.publicFailure({ code: 'auth-rejected', message: secret }), {
    code: 'auth-rejected',
  })
  const report = contractJobs.publicReport({
    schemaVersion: 1,
    reportType: 'probe',
    jobId: 'job-' + '1'.repeat(32),
    state: 'failed',
    environmentId: 'dev',
    startedAt: '2026-07-24T00:00:00.000Z',
    finishedAt: '2026-07-24T00:00:01.000Z',
    error: { code: secret, message: secret },
    warnings: [{ code: secret, message: secret }],
    resolution: {
      state: 'trusted-state-must-not-pass',
      reason: 'auth-required\u001b\r',
      method: 'page\n',
      resolvedUrl: 'https://user:password@example.test/openapi.json?token=SECRET#fragment',
      detectedKind: 'openapi\u007f',
      candidates: Array.from({ length: 25 }, (_, index) => ({
        url: `https://example.test/spec-${index}.json?credential=SECRET`,
        uid: `uid-${index}\u001b`, title: `Коллекция ${index}\r`, kind: 'openapi', unknown: secret,
      })),
      probedPaths: Array.from({ length: 12 }, (_, index) => `https://example.test/path-${index}?secret=SECRET`),
      truncated: true,
      unknown: secret,
    },
  })
  assert.deepEqual(report.error, { code: 'sidecar-failed' })
  assert.deepEqual(report.warnings, [{ code: 'sidecar-warning' }])
  assert.equal(JSON.stringify(report).includes(secret), false)
  assert.equal(report.resolution.state, 'resolved')
  assert.equal(report.resolution.resolvedUrl, 'https://example.test/openapi.json')
  assert.equal(report.resolution.candidates.length, 20)
  assert.equal(report.resolution.probedPaths.length, 8)
  assert.equal(report.resolution.candidates[0].title, 'Коллекция 0')
  assert.equal(JSON.stringify(report.resolution).includes('SECRET'), false)
  assert.equal(/[\u0000-\u001f\u007f-\u009f]/.test(JSON.stringify(report.resolution)), false)
  assert.equal(Object.prototype.hasOwnProperty.call(report.resolution, 'unknown'), false)
})

test('API contract route validation has exact localized copy', () => {
  const unknown = apiErrorMessage({ kind: '__unknown__' })
  assert.notEqual(apiErrorMessage({ kind: 'bad-area' }), unknown)
})

test('API drift diagnostics use exact localized kinds instead of raw report prose', () => {
  const producer = readFileSync(
    join(SITE, '..', 'api-contract', 'scripts', 'diff.mjs'),
    'utf8'
  )
  const codes = [...producer.matchAll(/\badd\('[A-Z]+', '([a-z][a-z0-9-]+)'/g)]
    .map((match) => match[1])
  assert.ok(codes.length >= 9)
  const unknownMessage = apiDriftFindingMessage('__unknown__')
  const unknownSuggestion = apiDriftFindingSuggestion('__unknown__')
  for (const code of codes) {
    assert.notEqual(apiDriftFindingMessage(code), unknownMessage, code)
    assert.notEqual(apiDriftFindingSuggestion(code), unknownSuggestion, code)
  }
  for (const severity of ['ERROR', 'WARNING', 'INFO']) {
    assert.notEqual(apiDriftSeverity(severity), apiDriftSeverity('__unknown__'), severity)
  }
  const panel = readFileSync(join(SITE, 'scripts', 'panels', 'api.js'), 'utf8')
  assert.doesNotMatch(panel, /api-finding__msg', text: String\(f\.message\)/)
  assert.doesNotMatch(panel, /text: String\(f\.suggestion\)/)
  assert.doesNotMatch(panel, /api-finding__kind', text: String\(f\.kind\)/)
})

test('Figma sync failures use a closed public vocabulary and every public code is localized', () => {
  assert.equal(
    syncErrors.classify('tokens', { tokenCode: 'TOKEN_SOURCE_CAPTURE_INCOMPLETE' }),
    'token-source-capture-incomplete'
  )
  assert.equal(
    syncErrors.classify('components', { componentCode: 'COMPONENT_DESIGN_SCOPE_CHANGED' }),
    'component-design-capture-inconsistent'
  )
  assert.equal(
    syncErrors.classifyComparison('tokens', 'PROJECT_ADAPTERS_UNCONFIGURED'),
    'project-adapters-unconfigured'
  )
  assert.equal(
    syncErrors.classify('tokens', { message: '/private/workspace/secret.txt' }),
    'token-sync-failed'
  )
  assert.equal(
    syncErrors.classify(null, { message: '/private/workspace/secret.txt' }),
    'unknown'
  )
  assert.equal(syncErrors.tokenHealthCode('/private/workspace/secret.txt'), 'TOKEN_CAPTURE_FAILED')
  for (const code of Object.values(syncErrors.PUBLIC_CODES)) {
    const message = syncFailureText(code)
    assert.equal(typeof message, 'string')
    assert.ok(message.length > 12, code)
    assert.notEqual(message, 'figma.syncFailure.' + code, code)
  }
})

test('browser JSON clients do not erase parse failures or render raw action diagnostics', () => {
  const files = [
    'scripts/data/tasks-api.js',
    'scripts/reviewer-api.js',
    'scripts/store.js',
    'scripts/design/filters.js',
    'scripts/design/entity-drawer.js',
    'scripts/panels/api.js',
    'scripts/panels/archmap.js',
    'scripts/panels/backend.js',
    'scripts/panels/board.js',
  ]
  const source = files.map((file) => readFileSync(join(SITE, file), 'utf8')).join('\n')
  assert.doesNotMatch(source, /\.json\(\)\.catch\(function \(\) \{ return \{\}; \}\)/)
  assert.doesNotMatch(source, /error\.detail\s*=/)
  assert.doesNotMatch(source, /error\.(?:payload|detail)\s*=\s*requestError/)
  assert.doesNotMatch(source, /clipboard\.toast\(\(error && \(error\.detail \|\| error\.kind\)\)/)
  assert.doesNotMatch(source, /clipboard\.toast\(t\([^)]*\{ detail: \(err && \(err\.detail \|\| err\.kind\)\)/)
  assert.doesNotMatch(source, /text:\s*t\('design\.staleDetail'\)\s*\+\s*' · '\s*\+\s*error\.message/)
  const syncSource = [
    'server/figma-sync.js',
    'server/figma-token-jobs.js',
  ].map((file) => readFileSync(join(SITE, file), 'utf8')).join('\n')
  assert.doesNotMatch(syncSource, /error && error\.message[\s\S]{0,120}replace\(\/\[\^a-z0-9\]/)
  assert.doesNotMatch(syncSource, /String\(result\.code[\s\S]{0,120}toLowerCase\(\)/)
  assert.doesNotMatch(syncSource, /\berror:\s*error\.message/)
  const syncServer = readFileSync(join(SITE, 'server', 'figma-sync.js'), 'utf8')
  const publicPlanBlock = syncServer.slice(syncServer.indexOf('function publicPlan'), syncServer.indexOf('function publicJob'))
  const publicJobBlock = syncServer.slice(syncServer.indexOf('function publicJob'), syncServer.indexOf('function historyRecord'))
  assert.doesNotMatch(publicPlanBlock, /message:\s*warning\.message/)
  assert.doesNotMatch(publicJobBlock, /messages:\s*job\.messages/)
  const contractGeneration = readFileSync(join(SITE, 'server', 'contract-generation.js'), 'utf8')
  const projectConfig = readFileSync(join(SITE, 'server', 'project-config-update.js'), 'utf8')
  const designTasks = readFileSync(join(SITE, 'server', 'design-task-actions.js'), 'utf8')
  const sessionActions = readFileSync(join(SITE, 'server', 'figma-session-actions.js'), 'utf8')
  const taskPublicationSource = readFileSync(join(SITE, 'server', 'figma-task-publication.js'), 'utf8')
  const sessions = readFileSync(join(SITE, 'server', 'sessions.js'), 'utf8')
  const tokenMappings = readFileSync(join(SITE, 'server', 'design-mappings.js'), 'utf8')
  const componentMappings = readFileSync(join(SITE, 'server', 'design-component-mappings.js'), 'utf8')
  const http = readFileSync(join(SITE, 'server', 'http.js'), 'utf8')
  assert.doesNotMatch(contractGeneration, /error:\s*e2\s*&&\s*e2\.message/)
  assert.doesNotMatch(projectConfig, /error:\s*(?:error|readError|parseError).*\.message/)
  assert.doesNotMatch(designTasks, /error:\s*String\(error[\s\S]{0,100}\.message/)
  assert.doesNotMatch(sessionActions, /error:\s*error\s*&&\s*error\.message/)
  assert.doesNotMatch(taskPublicationSource, /error:\s*error\s*&&\s*error\.message/)
  assert.doesNotMatch(sessions, /design publication failed:[\s\S]{0,80}publication\.error/)
  assert.doesNotMatch(tokenMappings, /typedFailure\([^)]*error\s*&&\s*error\.message/)
  assert.doesNotMatch(componentMappings, /typedFailure\([^)]*error\s*&&\s*error\.message/)
  assert.doesNotMatch(http, /error: 'session-start-refused',\s*detail:/)
  assert.doesNotMatch(http, /jsonResponse\([^;\n]*\bdetail:\s*String\([^;\n]*\.message/)
  assert.doesNotMatch(http, /\berror:\s*(?:e|err|error)\.message/)
  assert.doesNotMatch(http, /\berror:\s*String\(error && error\.code/)
  const board = readFileSync(join(SITE, 'scripts', 'panels', 'board.js'), 'utf8')
  const recoveryView = readFileSync(join(SITE, 'scripts', 'board', 'visual-evidence-recovery-view.js'), 'utf8')
  assert.doesNotMatch(board, /label === key \? String\(rawKind\)/)
  assert.doesNotMatch(board, /label === key \? String\(kind\)/)
  assert.doesNotMatch(board, /lbl === k \? (?:phase|status) : lbl/)
  assert.doesNotMatch(board, /first\.message/)
  assert.doesNotMatch(board, /\{\s*code:\s*intake\.errorCode/)
  assert.match(recoveryView, /evidenceIssueLabel\(rawKind\)/)
  const state = readFileSync(join(SITE, 'server', 'state.js'), 'utf8')
  const designIssueProjection = state.slice(
    state.indexOf('function designIssuesSummary'),
    state.indexOf('function readJsonSafe')
  )
  assert.match(designIssueProjection, /captureBlocked:\s*issues\.length > 0/)
  assert.doesNotMatch(designIssueProjection, /\bmessage:/)
  assert.doesNotMatch(designIssueProjection, /\bvalue:/)
  const tokensPanel = readFileSync(join(SITE, 'scripts', 'design', 'tokens.js'), 'utf8')
  const componentsPanel = readFileSync(join(SITE, 'scripts', 'design', 'components.js'), 'utf8')
  const surfacesPanel = readFileSync(join(SITE, 'scripts', 'design', 'surfaces.js'), 'utf8')
  const entityDrawer = readFileSync(join(SITE, 'scripts', 'design', 'entity-drawer.js'), 'utf8')
  const designPanel = readFileSync(join(SITE, 'scripts', 'panels', 'design.js'), 'utf8')
  assert.doesNotMatch(tokensPanel, /blocker\.detail/)
  assert.doesNotMatch(componentsPanel, /blocker\.detail/)
  assert.doesNotMatch(tokensPanel, /tokenBlocker\.generic',\s*\{\s*code:/)
  assert.doesNotMatch(componentsPanel, /componentBlocker\.generic',\s*\{\s*code:/)
  for (const source of [tokensPanel, componentsPanel, surfacesPanel, entityDrawer, designPanel]) {
    assert.doesNotMatch(source, /\berror\.message\b/)
  }
  const historyView = readFileSync(join(SITE, 'scripts', 'figma', 'history-view.js'), 'utf8')
  assert.doesNotMatch(historyView, /\+\s*item\.errorCode/)
  assert.doesNotMatch(historyView, /item\.messages/)
  assert.doesNotMatch(historyView, /figma\.history\.error/)
  assert.match(historyView, /text: state\.errorText/)
  const creationMarkers = readFileSync(join(SITE, 'server', 'creation-markers.js'), 'utf8')
  const editMarkers = readFileSync(join(SITE, 'server', 'edit-markers.js'), 'utf8')
  const designCatalog = readFileSync(join(SITE, 'server', 'design-catalog.js'), 'utf8')
  assert.doesNotMatch(creationMarkers, /updatedAt:\s*record\.updatedAt,\s*errorCode:/)
  assert.doesNotMatch(editMarkers, /updatedAt:\s*marker\.updatedAt,\s*errorCode:/)
  assert.doesNotMatch(state, /publicationRecoveryIssues[\s\S]{0,600}message:\s*issue\.message/)
  const latestSyncAttempt = designCatalog.slice(
    designCatalog.indexOf('function latestSyncAttempt'),
    designCatalog.indexOf('function syncFailureState')
  )
  assert.doesNotMatch(latestSyncAttempt, /\bdetail:/)
  assert.doesNotMatch(designCatalog, /return \{ code: safeString\(blocker && blocker\.code, 80\), detail:/)
  const autoRun = readFileSync(join(SITE, 'scripts', 'auto-run.js'), 'utf8')
  assert.doesNotMatch(autoRun, /detail:\s*String\(e && e\.message/)
  assert.doesNotMatch(autoRun, /detail:\s*st\.detail/)
  assert.doesNotMatch(autoRun, /String\(e && e\.message/)
  assert.match(autoRun, /errorCode\(e\) === 'session-busy'/)
})

test('the root state loader fails closed and boot renders a localized retry state', () => {
  const store = readFileSync(join(SITE, 'scripts', 'store.js'), 'utf8')
  const app = readFileSync(join(SITE, 'scripts', 'app.js'), 'utf8')
  assert.match(store, /invalid-state-response/)
  assert.doesNotMatch(store, /console\.error\('\[store\] \/api\/state failed/)
  assert.match(store, /function load\(\)[\s\S]*?}, function \(err\) \{[\s\S]*?throw err;[\s\S]*?return inflightLoad;/)
  assert.match(app, /common\.requestError\.' \+ code/)
  assert.match(app, /setAttribute\('role', 'alert'\)/)
  assert.match(app, /t\('common\.retry'\)/)
  assert.equal(Object.hasOwn(dictionaryFor('en'), 'common.error'), false)
})

test('session run failures use typed localized messages instead of one generic error', () => {
  const unknown = runErrorMessage({ kind: '__unknown__' })
  for (const code of [
    'figma-net-unwired',
    'finalization-active',
    'session-start-refused',
    'session-busy',
    'bad-key',
    'bad-prompt',
    'task-session-start-forbidden',
    'no-text',
    'terminal-input-queue-full',
    'session-runtime-unsafe',
    'writer-termination-pending',
    'runtime-only-contract-invalid',
    'conversation-only-contract-invalid',
    'workspace-writer-lease-refused',
    'workspace-writer-lease-attach-failed',
    'session-spawn-failed',
    'initial-prompt-refused',
    'task-answer-snapshot-unavailable',
    'task-answer-integrity-unavailable',
    'task-answer-integrity-invalid',
    'task-writer-lease-unavailable',
    'task-lock-owner-check-unavailable',
    'task-lock-owner-mismatch',
    'task-answer-not-requested',
    'task-integrity-unavailable',
    'task-answer-snapshot-changed',
    'task-turn-refused',
  ]) {
    assert.notEqual(runErrorMessage({ kind: code }), unknown, code)
  }
  const source = readFileSync(join(SITE, 'scripts', 'run-control.js'), 'utf8')
  assert.doesNotMatch(source, /t\('run\.failed'\)/)
  assert.match(source, /runErrorMessage\(err\)/)
  assert.equal(
    figmaActionError({ kind: 'writer-termination-pending' }),
    runErrorMessage({ kind: 'writer-termination-pending' }),
    'Figma screen pulls preserve typed session-runtime failures'
  )
  const http = readFileSync(join(SITE, 'server', 'http.js'), 'utf8')
  assert.match(http, /function publicSessionStartError\(status\)/)
  assert.match(http, /error: publicSessionStartError\(st\)/)
})

test('reviewer status, activity, and settings failures have one typed presenter', () => {
  const unknown = reviewerErrorMessage({ kind: '__unknown__' }, 'settings')
  for (const code of [
    'reviewer-unavailable',
    'bad-reviewer-recheck',
    'bad-activity-query',
    'bad-activity-state',
    'bad-limit',
    'bad-reviewer-settings',
    'idempotency-conflict',
    'project-busy',
    'config-conflict',
    'config-invalid',
    'reviewer-settings-failed',
    'stale-activity-cursor',
  ]) {
    assert.notEqual(reviewerErrorMessage({ kind: code }, 'settings'), unknown, code)
  }
  const panel = readFileSync(join(SITE, 'scripts', 'panels', 'reviewer.js'), 'utf8')
  assert.match(panel, /reviewerErrorMessage\(error, 'status'\)/)
  assert.match(panel, /reviewerErrorMessage\(error, 'activity'\)/)
  assert.match(panel, /reviewerErrorMessage\(error, 'settings'\)/)
})

test('CLI actions reject refused starts and render exact localized causes', () => {
  const unknown = cliErrorMessage({ kind: '__unknown__' })
  for (const code of [
    'cli-install-start-refused',
    'cli-login-start-refused',
    'cli-login-code-refused',
    'cli-runtime-unsafe',
    'cli-login-spawn-failed',
    'bad-kind',
    'no-code',
  ]) {
    assert.notEqual(cliErrorMessage({ kind: code }), unknown, code)
  }
  const http = readFileSync(join(SITE, 'server', 'http.js'), 'utf8')
  assert.match(http, /error: 'cli-install-start-refused'/)
  assert.match(http, /error: 'cli-login-start-refused'/)
  assert.match(http, /error: 'cli-login-code-refused'/)
  assert.doesNotMatch(http, /open-failed', detail: msg/)
  const cli = readFileSync(join(SITE, 'server', 'cli.js'), 'utf8')
  assert.doesNotMatch(cli, /error: 'cli-runtime-unsafe:/)
})

test('diagnostic CLIs classify missing reader codes instead of printing a generic error', () => {
  const resolver = readFileSync(join(SITE, '..', 'api-contract', 'scripts', 'resolve-current.mjs'), 'utf8')
  const doctor = readFileSync(join(SITE, '..', 'api-contract', 'scripts', 'doctor.mjs'), 'utf8')
  assert.doesNotMatch(resolver, /unknown error/)
  assert.doesNotMatch(doctor, /unknown error/)
  assert.match(resolver, /generation-reader-contract-invalid/)
  assert.match(doctor, /environment-reader-contract-invalid/)
})
