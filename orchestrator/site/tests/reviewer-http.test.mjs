import assert from 'node:assert/strict'
import { after, test } from 'node:test'
import { createServer } from 'node:http'
import {
  appendFileSync,
  linkSync,
  mkdirSync,
  mkdtempSync,
  readFileSync,
  rmSync,
  unlinkSync,
  writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { spawn } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const REPO = fileURLToPath(new URL('../../../', import.meta.url))
const root = mkdtempSync(join(tmpdir(), 'reviewer-http-'))
const orchestrator = join(root, 'orchestrator')
const tasks = join(orchestrator, 'tasks')
const cache = join(orchestrator, '.cache')
const journal = join(cache, 'tasks', 'journal')
const stateFile = join(cache, 'site', '.site-state.json')
const configFile = join(orchestrator, 'project-config.md')
const home = join(root, 'home')

for (const dir of [
  join(tasks, 'backlog'),
  join(tasks, 'pending'),
  join(tasks, 'todo'),
  join(tasks, 'done'),
  journal,
  join(cache, 'tasks', 'runs'),
  join(cache, 'tasks', 'finalizations'),
  join(cache, 'site'),
  home,
]) mkdirSync(dir, { recursive: true })

const originalConfig = [
  '---',
  'codexEnabled: auto',
  'figmaLibraryUrl: <figma-file-url>',
  'productName: Reviewer HTTP fixture',
  '---',
  '',
  '# Fixture',
  '',
].join('\n')
writeFileSync(configFile, originalConfig)
writeFileSync(stateFile, JSON.stringify({
  schemaVersion: 1,
  setupForm: { productName: 'Persisted fixture' },
  manualSteps: {},
  taskTiming: {},
  taskLifecycle: {},
  uiLang: 'en',
  backendActiveEnvironmentId: '',
  backendSelectionRevision: 0,
  appRunPreferences: {
    platform: null,
    targetStableHint: null,
    variantId: null,
    buildMode: null,
  },
}, null, 2) + '\n')

const activeStem = 'TASK_1_http_active'
const doneStem = 'TASK_2_http_done'
const sourceFingerprint = 'sha256:' + '2'.repeat(64)
const indexRow = (stem, title, state, doneAt = null) => ({
  stem, title, state, createdAt: '2026-07-16T09:00:00.000Z', doneAt,
  sourceRevision: sourceFingerprint,
  origin: { kind: 'manual', type: 'manual', ref: `fixture:${stem}`, fingerprint: sourceFingerprint },
  dependsOn: [], splitFrom: null,
  outcomeStatus: state === 'done' ? 'completed' : null, questionsCount: null, round: null,
})
writeFileSync(join(tasks, 'INDEX.json'), JSON.stringify({
  version: 2,
  generatedAt: '2026-07-16T12:00:00.000Z',
  backlog: [],
  pending: [],
  todo: [indexRow(activeStem, 'HTTP active review', 'todo')],
  done: [indexRow(doneStem, 'HTTP done review', 'done', '2026-07-16T10:01:00.000Z')],
}, null, 2) + '\n')
writeFileSync(join(journal, activeStem + '.jsonl'), JSON.stringify({
  ts: '2026-07-16T11:00:00Z',
  stem: activeStem,
  kind: 'phase-start',
  phase: 'review',
  status: 'info',
  meta: {
    reviewer: 'internal-reviewer',
    reviewAttempt: '1',
    selectionReason: 'codex-unavailable',
    reasonCode: 'fallback-used',
  },
}) + '\n', { mode: 0o600 })
writeFileSync(join(journal, doneStem + '.jsonl'), [
  JSON.stringify({
    ts: '2026-07-16T10:00:00Z',
    stem: doneStem,
    kind: 'phase-start',
    phase: 'review',
    status: 'info',
    meta: { reviewer: 'codex', reviewAttempt: '1', selectionReason: 'codex-available' },
  }),
  JSON.stringify({
    ts: '2026-07-16T10:01:00Z',
    stem: doneStem,
    kind: 'phase-end',
    phase: 'review',
    status: 'ok',
    meta: { reviewer: 'codex', reviewAttempt: '1' },
  }),
].join('\n') + '\n', { mode: 0o600 })
writeFileSync(join(tasks, 'done', doneStem + '.md'), [
  '# TASK 2 — HTTP done review',
  '',
  '---',
  '',
  '## Outcome',
  '',
  '**Status**: completed',
  '**Completed at**: 2026-07-16T10:01:00Z',
  '**Reviewer**: codex',
  '**Review iterations**: 1',
  '',
  '### Build gates',
  '- none',
  '',
  '### Runtime verify',
  '- Gate: skipped (fixture)',
  '- Result: n/a — fixture',
  '',
  '### Acceptance trace',
  '- `fixture` — verified — complete',
  '',
  '### Caveats',
  '- none',
  '',
  '### Follow-ups',
  '- none',
  '',
  '### Files touched',
  '- none',
  '',
].join('\n'))

const reserve = createServer()
await new Promise((resolve) => reserve.listen(0, '127.0.0.1', resolve))
const port = reserve.address().port
await new Promise((resolve) => reserve.close(resolve))

const child = spawn(process.execPath, [join(REPO, 'orchestrator', 'site', 'server.js')], {
  cwd: root,
  env: {
    ...process.env,
    HOME: home,
    PATH: '/usr/bin:/bin',
    ORCHESTRATOR_PROJECT_ROOT: root,
    ORCHESTRATOR_CACHE_DIR: cache,
    ORCHESTRATOR_STATE_FILE: stateFile,
    PORT: String(port),
    RUNNER_DISABLED: '1',
  },
  stdio: ['ignore', 'pipe', 'pipe'],
})
let stdout = ''
let stderr = ''
child.stdout.on('data', (chunk) => { stdout += chunk })
child.stderr.on('data', (chunk) => { stderr += chunk })
await new Promise((resolve, reject) => {
  const timeout = setTimeout(() => reject(new Error(`server timeout\n${stdout}\n${stderr}`)), 10000)
  const poll = () => {
    if (stdout.includes('Press Ctrl+C to stop.')) {
      clearTimeout(timeout)
      resolve()
      return
    }
    if (child.exitCode !== null) {
      clearTimeout(timeout)
      reject(new Error(`server exited ${child.exitCode}\n${stdout}\n${stderr}`))
      return
    }
    setTimeout(poll, 25)
  }
  poll()
})

const base = `http://127.0.0.1:${port}`
after(async () => {
  if (child.exitCode === null) child.kill('SIGTERM')
  await Promise.race([
    new Promise((resolve) => child.once('close', resolve)),
    new Promise((resolve) => setTimeout(resolve, 3000)),
  ])
  rmSync(root, { recursive: true, force: true })
})

async function json(path, options) {
  const response = await fetch(base + path, options)
  const body = await response.json().catch(() => ({}))
  return { response, body }
}

function post(csrf, body) {
  return {
    method: 'POST',
    headers: {
      'content-type': 'application/json',
      'x-orchestrator-csrf': csrf,
      origin: base,
    },
    body: JSON.stringify(body),
  }
}

async function readUntil(reader, text, timeoutMs = 5000) {
  const decoder = new TextDecoder()
  let buffer = ''
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    const remaining = Math.max(1, deadline - Date.now())
    const result = await Promise.race([
      reader.read(),
      new Promise((_, reject) => setTimeout(() => reject(new Error(`SSE timeout waiting for ${text}`)), remaining)),
    ])
    if (result.done) throw new Error(`SSE closed while waiting for ${text}`)
    buffer += decoder.decode(result.value, { stream: true })
    if (buffer.includes(text)) return buffer
  }
  throw new Error(`SSE timeout waiting for ${text}`)
}

test('Reviewer HTTP API is canonical, guarded, redacted, paginated, and live', async () => {
  const state = await json('/api/state')
  assert.equal(state.response.status, 200)
  const csrf = state.body.csrfToken
  assert.match(csrf, /^[a-f0-9]{48}$/)
  assert.equal(state.body.reviewerConfig.mode, 'automatic')
  assert.equal(state.body.reviewerConfig.state, 'ready')
  assert.equal(state.body.reviewerConfig.canUpdate, true)
  assert.equal(state.body.setup.codexEnabled, 'auto')

  const configSibling = join(orchestrator, 'project-config-hardlink.md')
  linkSync(configFile, configSibling)
  const unsafeState = await json('/api/state')
  assert.equal(unsafeState.body.reviewerConfig.state, 'invalid')
  assert.equal(unsafeState.body.reviewerConfig.canUpdate, false)
  assert.equal(Object.hasOwn(unsafeState.body.setup, 'codexEnabled'), false)
  unlinkSync(configSibling)

  writeFileSync(configFile, originalConfig.replace('codexEnabled: auto\n', ''))
  const missingState = await json('/api/state')
  assert.equal(missingState.body.reviewerConfig.state, 'missing')
  assert.equal(missingState.body.reviewerConfig.canUpdate, true)
  assert.equal(Object.hasOwn(missingState.body.setup, 'codexEnabled'), false)
  const repairedMissing = await json('/api/reviewer/settings', post(csrf, {
    mode: 'automatic',
    expectedRevision: missingState.body.reviewerConfig.revision,
    idempotencyKey: 'reviewer-http:repair-missing',
  }))
  assert.equal(repairedMissing.response.status, 200)
  assert.equal(repairedMissing.body.reviewer.config.mode, 'automatic')
  const repairedText = readFileSync(configFile, 'utf8')
  assert.equal((repairedText.match(/^codexEnabled:/gm) || []).length, 1)
  assert.equal(repairedText.replace('codexEnabled: auto\n', ''), originalConfig.replace('codexEnabled: auto\n', ''))

  writeFileSync(configFile, originalConfig.replace('codexEnabled: auto', 'codexEnabled: unexpected'))
  const invalidState = await json('/api/state')
  assert.equal(invalidState.body.reviewerConfig.state, 'invalid')
  assert.equal(invalidState.body.reviewerConfig.canUpdate, true)
  assert.equal(Object.hasOwn(invalidState.body.setup, 'codexEnabled'), false)
  writeFileSync(configFile, originalConfig)
  assert.equal(readFileSync(configFile, 'utf8'), originalConfig)

  const status = await json('/api/reviewer/status')
  assert.equal(status.response.status, 200)
  assert.equal(status.body.schemaVersion, 1)
  assert.equal(status.body.config.mode, 'automatic')
  assert.equal(status.body.review.enabled, true)
  assert.equal(status.body.review.activeReviewer, 'internal-reviewer')
  assert.equal(status.body.review.activeReviewerBasis, 'active-review')
  assert.equal(status.body.review.fallbackPolicy, 'internal-when-not-detected')
  assert.equal(status.body.codex.availability, 'unavailable')
  assert.equal(status.body.codex.installed, 'no')
  assert.equal(status.body.counts.pending, 1)
  assert.equal(status.body.counts.failed, 0)
  assert.equal(status.body.lastReview.taskStem, doneStem)
  assert.equal(JSON.stringify(status.body).includes(root), false)
  assert.equal(JSON.stringify(status.body).includes('stderr'), false)

  let result = await json('/api/reviewer/settings', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: '{}',
  })
  assert.equal(result.response.status, 403)
  assert.equal(result.body.error, 'bad-csrf')

  const malformed = await json('/api/reviewer/settings', {
    method: 'POST',
    headers: {
      'content-type': 'application/json',
      'x-orchestrator-csrf': csrf,
    },
    body: '{',
  })
  assert.equal(malformed.response.status, 400)
  assert.deepEqual(malformed.body, { error: 'bad-json' })

  result = await json('/api/reviewer/settings', post(csrf, {
    mode: 'automatic',
    expectedRevision: status.body.config.revision,
    idempotencyKey: 'reviewer-http:unknown-field',
    command: 'rm -rf /',
  }))
  assert.equal(result.response.status, 400)
  assert.equal(result.body.error, 'bad-reviewer-settings')

  const controller = new AbortController()
  const stream = await fetch(base + '/api/events', { signal: controller.signal })
  assert.equal(stream.status, 200)
  const reader = stream.body.getReader()
  await readUntil(reader, 'event: change')

  const settingsBody = {
    mode: 'internal-only',
    expectedRevision: status.body.config.revision,
    idempotencyKey: 'reviewer-http:save-internal',
  }
  result = await json('/api/reviewer/settings', post(csrf, settingsBody))
  assert.equal(result.response.status, 200)
  assert.equal(result.body.ok, true)
  assert.equal(result.body.reviewer.config.mode, 'internal-only')
  assert.equal(result.body.reviewer.review.activeReviewer, 'internal-reviewer')
  const live = await readUntil(reader, 'event: reviewer-status')
  assert.match(live, /event: reviewer-status/)
  assert.doesNotMatch(live, /event: reviewer-activity/)
  controller.abort()

  const replay = await json('/api/reviewer/settings', post(csrf, settingsBody))
  assert.equal(replay.response.status, 200)
  assert.equal(replay.body.reviewer.config.mode, 'internal-only')

  const stale = await json('/api/reviewer/settings', post(csrf, {
    mode: 'automatic',
    expectedRevision: status.body.config.revision,
    idempotencyKey: 'reviewer-http:stale',
  }))
  assert.equal(stale.response.status, 409)
  assert.equal(stale.body.error, 'config-conflict')
  const failedKeyConflict = await json('/api/reviewer/settings', post(csrf, {
    mode: 'internal-only',
    expectedRevision: result.body.reviewer.config.revision,
    idempotencyKey: 'reviewer-http:stale',
  }))
  assert.equal(failedKeyConflict.response.status, 409)
  assert.equal(failedKeyConflict.body.error, 'idempotency-conflict')

  const idempotencyConflict = await json('/api/reviewer/settings', post(csrf, {
    mode: 'automatic',
    expectedRevision: result.body.reviewer.config.revision,
    idempotencyKey: settingsBody.idempotencyKey,
  }))
  assert.equal(idempotencyConflict.response.status, 409)
  assert.equal(idempotencyConflict.body.error, 'idempotency-conflict')

  const badRecheck = await json('/api/reviewer/recheck', post(csrf, { force: true }))
  assert.equal(badRecheck.response.status, 400)
  assert.equal(badRecheck.body.error, 'bad-reviewer-recheck')
  const recheck = await json('/api/reviewer/recheck', post(csrf, {}))
  assert.equal(recheck.response.status, 200)
  assert.equal(recheck.body.ok, true)
  assert.equal(recheck.body.reviewer.codex.availability, 'unavailable')

  const activity = await json('/api/reviewer/activity?state=pending&limit=1')
  assert.equal(activity.response.status, 200)
  assert.equal(activity.body.rows.length, 1)
  assert.equal(activity.body.rows[0].taskStem, activeStem)
  assert.equal(activity.body.rows[0].reviewer, 'internal-reviewer')
  assert.equal(JSON.stringify(activity.body).includes(root), false)
  assert.equal((await json('/api/reviewer/activity?state=unknown')).response.status, 400)
  assert.equal((await json('/api/reviewer/activity?limit=101')).response.status, 400)
  assert.equal((await json('/api/reviewer/activity?cursor=not-a-cursor')).response.status, 400)
  assert.equal((await json('/api/reviewer/activity?state=pending&state=failed')).body.error, 'bad-activity-query')
  assert.equal((await json('/api/reviewer/activity?unknown=1')).body.error, 'bad-activity-query')
  assert.equal((await json('/api/reviewer/activity?limit=01')).body.error, 'bad-activity-query')

  const journalController = new AbortController()
  const journalStream = await fetch(base + '/api/events', { signal: journalController.signal })
  const journalReader = journalStream.body.getReader()
  await readUntil(journalReader, 'event: change')
  appendFileSync(join(journal, activeStem + '.jsonl'), JSON.stringify({
    ts: '2026-07-16T11:05:00Z',
    stem: activeStem,
    kind: 'phase-end',
    phase: 'review',
    status: 'ok',
    meta: { reviewer: 'internal-reviewer', reviewAttempt: '1' },
  }) + '\n')
  const journalLive = await readUntil(journalReader, 'event: reviewer-activity')
  assert.match(journalLive, /event: reviewer-status/)
  journalController.abort()
})
