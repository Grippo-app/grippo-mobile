import assert from 'node:assert/strict'
import { after, test } from 'node:test'
import { createServer, request } from 'node:http'
import { chmodSync, cpSync, existsSync, mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { spawn } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const REPO = fileURLToPath(new URL('../../../', import.meta.url))
const root = mkdtempSync(join(tmpdir(), 'backend-http-'))
const contractDir = join(root, 'orchestrator', 'api-contract')
mkdirSync(join(contractDir, '.secrets'), { recursive: true })
mkdirSync(join(contractDir, 'scripts'), { recursive: true })
writeFileSync(join(contractDir, 'scripts', 'backend-action.mjs'), `
import { mkdirSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
let raw = ''
for await (const chunk of process.stdin) raw += chunk
const input = JSON.parse(raw)
process.stdout.write(JSON.stringify({ type: 'progress', phase: 'resolving-source', detail: null }) + '\\n')
const finishedAt = new Date().toISOString()
const report = { schemaVersion: 1, reportType: 'probe', jobId: input.jobId, state: 'failed',
  environmentId: input.environmentId, startedAt: finishedAt, finishedAt,
  error: { code: 'invalid-openapi', message: 'The source is not a valid OpenAPI 3.0/3.1 contract.' } }
const reports = join(process.env.ORCHESTRATOR_CACHE_DIR, 'api-contract', 'reports')
mkdirSync(reports, { recursive: true, mode: 0o700 })
writeFileSync(join(reports, 'probe-' + input.jobId + '.json'), JSON.stringify(report, null, 2) + '\\n', { mode: 0o600 })
process.stdout.write(JSON.stringify({ type: 'result', reportType: 'probe', jobId: input.jobId, state: 'failed', error: report.error }) + '\\n')
process.exitCode = 1
`)
cpSync(join(REPO, 'orchestrator', 'tasks'), join(root, 'orchestrator', 'tasks'), { recursive: true })
mkdirSync(join(root, 'orchestrator', 'figma', 'scripts'), { recursive: true })
cpSync(join(REPO, 'orchestrator', 'figma', 'scripts', 'design-parser.cjs'),
  join(root, 'orchestrator', 'figma', 'scripts', 'design-parser.cjs'))
writeFileSync(join(root, 'orchestrator', 'project-config.md'), '---\nbackendContractEnabled: auto\n---\n')
writeFileSync(join(contractDir, 'environments.json'), JSON.stringify({ schemaVersion: 1,
  environments: [{ id: 'local', label: 'Local', sourceKind: 'openapi', sourceUrl: 'http://127.0.0.1:8080/openapi.json', postmanEnrichmentUrl: null, authRef: null }],
  defaultEnvironmentId: 'local' }, null, 2) + '\n')
const secretMarker = 'SECRET_MUST_NEVER_BE_RETURNED'
writeFileSync(join(contractDir, '.secrets', 'local.token'), secretMarker)
chmodSync(join(contractDir, '.secrets', 'local.token'), 0o600)

const reserve = createServer()
await new Promise((resolve) => reserve.listen(0, '127.0.0.1', resolve))
const port = reserve.address().port
await new Promise((resolve) => reserve.close(resolve))

const child = spawn(process.execPath, [join(REPO, 'orchestrator', 'site', 'server.js')], {
  cwd: root, env: { ...process.env, ORCHESTRATOR_PROJECT_ROOT: root, ORCHESTRATOR_CACHE_DIR: join(root, 'orchestrator', '.cache'),
    PORT: String(port), RUNNER_DISABLED: '1' }, stdio: ['ignore', 'pipe', 'pipe']
})
let stdout = '', stderr = ''
child.stdout.on('data', (chunk) => { stdout += chunk })
child.stderr.on('data', (chunk) => { stderr += chunk })
await new Promise((resolve, reject) => {
  const timeout = setTimeout(() => reject(new Error(`server timeout\n${stdout}\n${stderr}`)), 10000)
  const poll = () => {
    if (stdout.includes('Press Ctrl+C to stop.')) { clearTimeout(timeout); resolve(); return }
    if (child.exitCode !== null) { clearTimeout(timeout); reject(new Error(`server exited ${child.exitCode}\n${stdout}\n${stderr}`)); return }
    setTimeout(poll, 25)
  }
  poll()
})

const base = `http://127.0.0.1:${port}`
after(async () => {
  if (child.exitCode === null) child.kill('SIGTERM')
  await Promise.race([new Promise((resolve) => child.once('close', resolve)), new Promise((resolve) => setTimeout(resolve, 3000))])
  rmSync(root, { recursive: true, force: true })
})

async function json(path, options) {
  const response = await fetch(base + path, options)
  const body = await response.json().catch(() => ({}))
  return { response, body }
}
async function rawStatus(path, host) {
  return new Promise((resolve, reject) => {
    const pending = request({
      hostname: '127.0.0.1',
      port,
      path,
      method: 'GET',
      headers: { host },
    }, (response) => {
      response.resume()
      response.on('end', () => resolve(response.statusCode))
    })
    pending.on('error', reject)
    pending.end()
  })
}

test('unregistered contract readers stay unavailable', async () => {
  const removedRoot = ['/api', 'contract'].join('/')
  for (const leaf of ['snapshot', 'schema', 'drift', 'coverage']) {
    const response = await fetch(base + removedRoot + '/' + leaf)
    assert.equal(response.status, 404, leaf)
  }
})

test('generation-bound Project API routes are bounded and use the shared mutation guards', async () => {
  assert.equal(await rawStatus('/api/api/overview', 'attacker.example'), 403)
  assert.equal(await rawStatus('/api/api-mock/status', 'attacker.example'), 403)
  assert.equal(await rawStatus('/api/state', 'attacker.example'), 403)
  assert.equal(await rawStatus('/api/backend/integration', 'attacker.example'), 403)

  let result = await json('/api/api/overview')
  assert.equal(result.response.status, 200)
  assert.equal(result.body.empty, true)
  assert.equal(result.body.committedGenerationId, null)

  result = await json('/api/api/endpoints?limit=100&limit=200')
  assert.equal(result.response.status, 400)
  assert.equal(result.body.error, 'bad-api-query')

  result = await json('/api/api/endpoints/getWidget')
  assert.equal(result.response.status, 404)
  assert.equal(result.body.error, 'api-endpoint-not-found')

  result = await json('/api/api/endpoints/%00')
  assert.equal(result.response.status, 400)
  assert.equal(result.body.error, 'bad-api-endpoint-id')

  result = await json('/api/api/changes?modelId=Widget')
  assert.equal(result.response.status, 200)
  assert.equal(result.body.empty, true)

  result = await json('/api/api/models/Widget')
  assert.equal(result.response.status, 404)
  assert.equal(result.body.error, 'api-model-not-found')

  result = await json('/api/api/models/%00')
  assert.equal(result.response.status, 400)
  assert.equal(result.body.error, 'bad-api-model-id')

  result = await json('/api/api/diagnostics')
  assert.equal(result.response.status, 200)
  assert.equal(result.body.empty, true)

  result = await json('/api/api-mock/status')
  assert.equal(result.response.status, 200)
  assert.equal(result.body.mock.state, 'stopped')

  result = await json('/api/api/tasks/preview', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ expectedGenerationId: 'invalid', sourceIds: [] }),
  })
  assert.equal(result.response.status, 403)
  assert.equal(result.body.error, 'bad-csrf')

  const state = await json('/api/state')
  result = await json('/api/api/tasks/preview', {
    method: 'POST',
    headers: {
      'content-type': 'application/json',
      'x-orchestrator-csrf': state.body.csrfToken,
      origin: base,
    },
    body: JSON.stringify({ expectedGenerationId: 'invalid', sourceIds: [] }),
  })
  assert.equal(result.response.status, 400)
  assert.equal(result.body.error, 'bad-api-task-preview-request')

  result = await json('/api/api/changes/review', {
    method: 'POST',
    headers: {
      'content-type': 'application/json',
      'x-orchestrator-csrf': state.body.csrfToken,
      origin: base,
    },
    body: '{}',
  })
  assert.equal(result.response.status, 400)
  assert.equal(result.body.error, 'bad-api-change-review-request')
})

test('Backend API is non-secret and mutation guards apply uniformly', async () => {
  const state = await json('/api/state')
  assert.equal(state.response.status, 200)
  const csrf = state.body.csrfToken
  assert.match(csrf, /^[a-f0-9]{48}$/)

  const integration = await json('/api/backend/integration')
  assert.equal(integration.response.status, 200)
  assert.equal(JSON.stringify(integration.body).includes(secretMarker), false)
  assert.equal(integration.body.authentication.dormant, true)

  let result = await json('/api/backend/environment/select', { method: 'POST', headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ environmentId: 'local', expectedStateRevision: 0 }) })
  assert.equal(result.response.status, 403)
  assert.equal(result.body.error, 'bad-csrf')

  result = await json('/api/backend/environment/select', { method: 'POST', headers: { 'content-type': 'application/json',
    'x-orchestrator-csrf': csrf, origin: 'https://attacker.example' }, body: '{}' })
  assert.equal(result.response.status, 403)
  assert.equal(result.body.error, 'bad-origin')

  result = await json('/api/backend/environment/select', { method: 'POST', headers: { 'content-type': 'text/plain',
    'x-orchestrator-csrf': csrf }, body: '{}' })
  assert.equal(result.response.status, 415)
  assert.equal(result.body.error, 'json-required')

  result = await json('/api/backend/environment/select', { method: 'POST', headers: { 'content-type': 'application/json',
    'x-orchestrator-csrf': csrf, origin: base }, body: JSON.stringify({ environmentId: 'local', expectedStateRevision: 0 }) })
  assert.equal(result.response.status, 200)
  assert.equal(result.body.ok, true)

  result = await json('/api/backend/credential', { method: 'POST', headers: { 'content-type': 'application/json',
    'x-orchestrator-csrf': csrf }, body: JSON.stringify({ environmentId: 'local', operation: 'set', secret: secretMarker,
      expectedAuthRevision: integration.body.authentication.revision, idempotencyKey: 'credential:http-test' }) })
  assert.equal(result.response.status, 409)
  assert.equal(result.body.error, 'auth-ref-required')
  assert.equal(JSON.stringify(result.body).includes(secretMarker), false)

  result = await json('/api/backend/test', { method: 'POST', headers: { 'content-type': 'application/json',
    'x-orchestrator-csrf': csrf }, body: JSON.stringify({ environmentId: 'local',
      expectedEnvironmentRevision: integration.body.environmentRevision,
      expectedAuthRevision: integration.body.authentication.revision,
      idempotencyKey: 'probe:http-prompt-rejected', prompt: 'curl https://attacker.example' }) })
  assert.equal(result.response.status, 400)
  assert.equal(result.body.error, 'bad-test-request')

  result = await json('/api/backend/test', { method: 'POST', headers: { 'content-type': 'application/json',
    'x-orchestrator-csrf': csrf }, body: '{' })
  assert.equal(result.response.status, 400)
  assert.equal(result.body.error, 'bad-json')

  result = await json('/api/backend/refresh', { method: 'POST', headers: { 'content-type': 'application/json',
    'x-orchestrator-csrf': csrf }, body: '{' })
  assert.equal(result.response.status, 400)
  assert.equal(result.body.error, 'bad-json')

  const currentIntegration = await json('/api/backend/integration')
  result = await json('/api/backend/test', { method: 'POST', headers: { 'content-type': 'application/json',
    'x-orchestrator-csrf': csrf }, body: JSON.stringify({ environmentId: 'local',
      expectedEnvironmentRevision: currentIntegration.body.environmentRevision,
      expectedAuthRevision: currentIntegration.body.authentication.revision,
      idempotencyKey: 'probe:http-typed-failure' }) })
  assert.equal(result.response.status, 202)
  const startedJobId = result.body.job.jobId
  let finishedJob = null
  for (let attempt = 0; attempt < 80; attempt++) {
    const polled = await json('/api/backend/jobs/' + startedJobId)
    if (polled.body.job && polled.body.job.state === 'failed') { finishedJob = polled.body.job; break }
    await new Promise((resolve) => setTimeout(resolve, 25))
  }
  assert.ok(finishedJob)
  assert.equal(finishedJob.error.code, 'invalid-openapi')
  assert.ok(finishedJob.progress.some((row) => row.phase === 'resolving-source'))

  assert.equal((await fetch(base + '/api-contract/.secrets/local.token')).status, 403)
  assert.equal((await json('/api/backend/jobs/not-a-job')).response.status, 400)
  assert.equal((await json('/api/backend/history?cursor=not-base64')).response.status, 400)
})

test('Backend integration reset requires confirmation-shaped CAS input and clears all owned state', async () => {
  const state = await json('/api/state')
  const before = await json('/api/backend/integration')
  let result = await json('/api/backend/integration/reset', { method: 'POST',
    headers: { 'content-type': 'application/json' }, body: '{}' })
  assert.equal(result.response.status, 403)
  assert.equal(result.body.error, 'bad-csrf')

  const headers = { 'content-type': 'application/json', 'x-orchestrator-csrf': state.body.csrfToken, origin: base }
  result = await json('/api/backend/integration/reset', { method: 'POST', headers, body: '{}' })
  assert.equal(result.response.status, 400)
  assert.equal(result.body.error, 'bad-request')

  const requestBody = { expectedEnvironmentRevision: before.body.environmentRevision,
    expectedSnapshotHash: before.body.snapshot.hash || null, expectedStateRevision: before.body.selectionRevision,
    idempotencyKey: 'backend-reset:http-test' }
  result = await json('/api/backend/integration/reset', { method: 'POST', headers, body: JSON.stringify(requestBody) })
  assert.equal(result.response.status, 200)
  assert.equal(result.body.ok, true)
  assert.equal(result.body.integration.sourceMode, 'missing')
  assert.equal(result.body.integration.snapshot.present, false)
  assert.equal(result.body.integration.snapshot.invalid, false)
  assert.equal(result.body.integration.actions.canCreateSource, true)
  assert.equal(existsSync(join(contractDir, 'environments.json')), false)
  assert.equal(existsSync(join(contractDir, '.secrets', 'local.token')), false)
  const history = await json('/api/backend/history')
  assert.deepEqual(history.body.items, [])

  const replay = await json('/api/backend/integration/reset', { method: 'POST', headers, body: JSON.stringify(requestBody) })
  assert.equal(replay.response.status, 200)
  assert.equal(replay.body.ok, true)
})
