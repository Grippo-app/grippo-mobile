import assert from 'node:assert/strict'
import { after, test } from 'node:test'
import { createHash } from 'node:crypto'
import { chmodSync, existsSync, linkSync, mkdirSync, readFileSync, rmSync, symlinkSync, unlinkSync, writeFileSync } from 'node:fs'
import { mkdtempSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { spawn } from 'node:child_process'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const REPO = fileURLToPath(new URL('../../../', import.meta.url))
const root = mkdtempSync(join(tmpdir(), 'backend-config-'))
const contractDir = join(root, 'orchestrator', 'api-contract')
mkdirSync(contractDir, { recursive: true })
writeFileSync(join(root, 'orchestrator', 'project-config.md'), '---\nbackendContractEnabled: auto\nopenApiSpecUrl: <openapi-spec-url>\n---\n')
writeFileSync(join(contractDir, 'environments.json'), JSON.stringify({
  schemaVersion: 1,
  environments: [
    { id: 'local', label: 'Local', sourceKind: 'openapi', sourceUrl: 'http://127.0.0.1:8080/openapi.json', postmanEnrichmentUrl: null, authRef: null },
    { id: 'dev', label: 'Dev', sourceKind: 'openapi', sourceUrl: 'https://dev.example/openapi.json', postmanEnrichmentUrl: null, authRef: 'dev' }
  ],
  defaultEnvironmentId: 'dev'
}, null, 2) + '\n')
process.env.ORCHESTRATOR_PROJECT_ROOT = root

const require = createRequire(import.meta.url)
const apiRequire = createRequire(join(REPO, 'orchestrator', 'api-contract', 'package.json'))
const Ajv = apiRequire('ajv')
const environments = require(join(REPO, 'orchestrator', 'site', 'server', 'backend-environments.js'))
const credentials = require(join(REPO, 'orchestrator', 'site', 'server', 'backend-credentials.js'))
const history = require(join(REPO, 'orchestrator', 'site', 'server', 'contract-history.js'))
const backendIntegration = require(join(REPO, 'orchestrator', 'site', 'server', 'backend-integration.js'))
const contractJobs = require(join(REPO, 'orchestrator', 'site', 'server', 'contract-job.js'))
const paths = require(join(REPO, 'orchestrator', 'site', 'server', 'paths.js'))
const writerLeases = require(join(REPO, 'orchestrator', 'tasks', 'writer-leases.cjs'))

after(() => rmSync(root, { recursive: true, force: true }))

test('environment manifest rejects URL, secret and shape hazards and enforces CAS/idempotency', () => {
  const state = environments.read()
  assert.equal(state.mode, 'manifest')
  assert.equal(state.manifest.environments[0].authKind, 'bearer')
  const validateManifest = new Ajv({ allErrors: true }).compile(JSON.parse(readFileSync(join(REPO,
    'orchestrator', 'api-contract', 'contract-schemas', 'environments.schema.json'), 'utf8')))
  assert.equal(validateManifest(state.manifest), true, JSON.stringify(validateManifest.errors))
  const legacyRevision = 'sha256:' + createHash('sha256').update(JSON.stringify({
    schemaVersion: 1,
    environments: state.manifest.environments.map(({ authKind, ...environment }) => environment),
    defaultEnvironmentId: state.manifest.defaultEnvironmentId
  })).digest('hex')
  assert.notEqual(state.revision, legacyRevision)
  const base = { id: 'stage', label: 'Stage', sourceKind: 'openapi', sourceUrl: 'https://stage.example/openapi.json', postmanEnrichmentUrl: null, authRef: null }
  const invalid = [
    { ...base, sourceUrl: 'http://stage.example/openapi.json' },
    { ...base, sourceUrl: 'https://stage.example/openapi.json?token=x' },
    { ...base, sourceUrl: 'https://stage.example/openapi.json#x' },
    { ...base, sourceUrl: 'https://user:pass@stage.example/openapi.json' },
    { ...base, sourceUrl: 'https://127.0.0.1/openapi.json' },
    { ...base, sourceKind: 'postman', postmanEnrichmentUrl: 'https://stage.example/postman.json' },
    { ...base, id: 'local', sourceUrl: 'https://stage.example/openapi.json' },
    { ...base, authRef: 'dev' },
    { ...base, token: 'must-not-be-accepted' }
  ]
  for (const candidate of invalid) assert.equal(environments.validateEnvironment(candidate).ok, false)
  assert.equal(environments.validateEnvironment({ ...base, authKind: 'x' }).error, 'auth-kind-invalid')
  assert.equal(environments.validateEnvironment({ ...base, authKind: 'x-api-key' }).value.authKind, 'x-api-key')
  assert.equal(environments.validateEnvironment({ ...base, authKind: 'x-api-key', token: 'must-not-be-accepted' }).error,
    'environment-fields-invalid')
  assert.equal(environments.validateEnvironment({ ...base, label: 'я'.repeat(33) }).ok, false)
  assert.equal(environments.validateEnvironment({ ...base, id: 'local', sourceUrl: 'http://localhost:8080/openapi.json', authRef: 'local' }).ok, false)
  const atPath = environments.validateEnvironment({ ...base,
    sourceUrl: 'https://stage.example/contracts/team@v1/openapi.json',
    postmanEnrichmentUrl: 'https://stage.example/contracts/team@v1/postman.json' })
  assert.equal(atPath.ok, true)
  assert.equal(validateManifest({ schemaVersion: 1, environments: [atPath.value], defaultEnvironmentId: 'stage' }), true,
    JSON.stringify(validateManifest.errors))

  const request = { operation: 'upsert', environment: base, expectedRevision: state.revision,
    idempotencyKey: 'environment:test-stage', defaultEnvironmentId: 'dev' }
  const written = environments.mutate(request)
  assert.equal(written.ok, true)
  assert.equal(written.manifest.environments.find((environment) => environment.id === 'stage').authKind, 'bearer')
  const canonical = JSON.parse(readFileSync(join(contractDir, 'environments.json'), 'utf8'))
  assert.ok(canonical.environments.every((environment) => Object.keys(environment).length === 7))
  assert.ok(canonical.environments.every((environment) => environment.authKind === 'bearer'))
  assert.equal(environments.mutate(request), written)
  assert.equal(environments.mutate({ ...request, environment: { ...base, label: 'Other' } }).error, 'idempotency-conflict')
  assert.equal(environments.mutate({ ...request, idempotencyKey: 'environment:stale-stage' }).error, 'environment-revision-conflict')
  const keyed = environments.mutate({ ...request, expectedRevision: written.revision,
    idempotencyKey: 'environment:stage-x-api-key', environment: { ...base, authKind: 'x-api-key' } })
  assert.equal(keyed.ok, true)
  assert.equal(keyed.manifest.environments.find((environment) => environment.id === 'stage').authKind, 'x-api-key')
  const reset = environments.mutate({ ...request, expectedRevision: keyed.revision,
    idempotencyKey: 'environment:stage-reset-bearer' })
  assert.equal(reset.ok, true)
  assert.equal(reset.manifest.environments.find((environment) => environment.id === 'stage').authKind, 'bearer')
  assert.equal(JSON.parse(readFileSync(join(contractDir, 'environments.json'), 'utf8')).environments
    .find((environment) => environment.id === 'stage').authKind, 'bearer')
})

test('credential API never returns the value and ignores obsolete root token files', async () => {
  const dev = environments.environmentById(environments.read(), 'dev')
  let status = credentials.publicStatus(dev)
  const secret = 'super-private-test-token'
  const setResult = await credentials.mutate({ environmentId: 'dev', operation: 'set', secret,
    expectedAuthRevision: status.revision, idempotencyKey: 'credential:set-dev' })
  assert.equal(setResult.ok, true)
  assert.equal(JSON.stringify(setResult).includes(secret), false)
  const secretFile = join(contractDir, '.secrets', 'dev.token')
  assert.equal(existsSync(secretFile), true)
  assert.equal((await import('node:fs')).statSync(secretFile).mode & 0o777, 0o600)
  assert.equal(credentials.publicStatus(dev).state, 'configured')
  assert.equal(credentials.publicStatus({ ...dev, authKind: 'x-api-key' }).kind, 'x-api-key')
  const replay = await credentials.mutate({ environmentId: 'dev', operation: 'set', secret,
    expectedAuthRevision: status.revision, idempotencyKey: 'credential:set-dev' })
  assert.equal(replay.replayed, true)
  assert.equal((await credentials.mutate({ environmentId: 'dev', operation: 'set', secret: 'different',
    expectedAuthRevision: replay.revision, idempotencyKey: 'credential:set-dev' })).error, 'idempotency-conflict')
  assert.equal((await credentials.mutate({ environmentId: 'dev', operation: 'set', secret: 'bad\nvalue',
    expectedAuthRevision: replay.revision, idempotencyKey: 'credential:bad-value' })).error, 'credential-value-invalid')

  status = credentials.publicStatus(dev)
  const deleted = await credentials.mutate({ environmentId: 'dev', operation: 'delete',
    expectedAuthRevision: status.revision, idempotencyKey: 'credential:delete-dev' })
  assert.equal(deleted.ok, true)

  mkdirSync(dirname(secretFile), { recursive: true })
  const hardlinkSource = join(root, 'hardlink-token')
  writeFileSync(hardlinkSource, 'hardlinked')
  chmodSync(hardlinkSource, 0o600)
  linkSync(hardlinkSource, secretFile)
  assert.equal(credentials.publicStatus(dev).state, 'invalid')
  unlinkSync(secretFile); unlinkSync(hardlinkSource)
  symlinkSync(join(root, 'missing-target'), secretFile)
  assert.equal(credentials.publicStatus(dev).state, 'invalid')
  unlinkSync(secretFile)

  const retiredTokenFile = join(contractDir, '.api-token')
  writeFileSync(retiredTokenFile, 'retired-private-token')
  chmodSync(retiredTokenFile, 0o600)
  status = credentials.publicStatus(dev)
  assert.equal(status.state, 'missing')
  assert.equal(Object.hasOwn(status, 'legacy'), false)
  assert.equal(Object.hasOwn(status, 'legacyConflict'), false)
  assert.equal(credentials.migrate, undefined)
  assert.equal(existsSync(retiredTokenFile), true)
})

test('environment writes refuse a foreign project writer lease', () => {
  const handle = writerLeases.acquire(paths.WRITER_LEASES_DIR, { kind: 'site-config', key: 'test:foreign-writer',
    ownerPid: process.pid, ttlMs: 60000, rootDir: paths.WRITER_AUTHORITY_ROOT })
  try {
    const state = environments.read()
    const result = environments.mutate({ operation: 'upsert', expectedRevision: state.revision,
      idempotencyKey: 'environment:foreign-writer', defaultEnvironmentId: state.manifest.defaultEnvironmentId,
      environment: { id: 'prod', label: 'Prod', sourceKind: 'openapi', sourceUrl: 'https://api.example/openapi.json',
        postmanEnrichmentUrl: null, authRef: null } })
    assert.equal(result.ok, false)
    assert.equal(result.error, 'writer-lease-conflict')
    assert.equal(environments.environmentById(environments.read(), 'prod'), null)
  } finally { writerLeases.release(handle) }
})

test('full Backend reset refuses a foreign writer before deleting any integration state', async () => {
  const handle = writerLeases.acquire(paths.WRITER_LEASES_DIR, { kind: 'site-config', key: 'test:foreign-reset-writer',
    ownerPid: process.pid, ttlMs: 60000, rootDir: paths.WRITER_AUTHORITY_ROOT })
  try {
    const before = backendIntegration.get()
    const result = await backendIntegration.reset({ expectedEnvironmentRevision: before.environmentRevision,
      expectedSnapshotHash: before.snapshot.hash || null, expectedStateRevision: before.selectionRevision,
      idempotencyKey: 'backend-reset:foreign-writer' })
    assert.equal(result.ok, false)
    assert.equal(result.error, 'writer-lease-conflict')
    assert.equal(existsSync(join(contractDir, 'environments.json')), true)
    assert.equal(backendIntegration.get().environmentRevision, before.environmentRevision)
  } finally { writerLeases.release(handle) }
})

test('credential mutex serializes competing site and sidecar processes', async () => {
  const dev = environments.environmentById(environments.read(), 'dev')
  const revision = credentials.publicStatus(dev).revision
  const childCode = [
    "let input = ''",
    "process.stdin.setEncoding('utf8')",
    "process.stdin.on('data', (chunk) => { input += chunk })",
    "process.stdin.on('end', async () => {",
    "  const store = require(process.env.BACKEND_CREDENTIAL_MODULE)",
    "  const result = await store.mutate(JSON.parse(input))",
    "  process.stdout.write(JSON.stringify(result))",
    "})"
  ].join(';')
  function run(request) {
    return new Promise((resolve, reject) => {
      const child = spawn(process.execPath, ['-e', childCode], { env: { ...process.env,
        ORCHESTRATOR_PROJECT_ROOT: root,
        BACKEND_CREDENTIAL_MODULE: join(REPO, 'orchestrator', 'site', 'server', 'backend-credentials.js') },
        stdio: ['pipe', 'pipe', 'pipe'] })
      let out = '', err = ''
      child.stdout.on('data', (chunk) => { out += chunk })
      child.stderr.on('data', (chunk) => { err += chunk })
      child.on('error', reject)
      child.on('close', (code) => code === 0 ? resolve(JSON.parse(out)) : reject(new Error(err || `child exited ${code}`)))
      child.stdin.end(JSON.stringify(request))
    })
  }
  const [first, second] = await Promise.all([
    run({ environmentId: 'dev', operation: 'set', secret: 'concurrent-token-one', expectedAuthRevision: revision,
      idempotencyKey: 'credential:concurrent-one' }),
    run({ environmentId: 'dev', operation: 'set', secret: 'concurrent-token-two', expectedAuthRevision: revision,
      idempotencyKey: 'credential:concurrent-two' })
  ])
  assert.equal([first, second].filter((row) => row.ok).length, 1)
  assert.deepEqual([first, second].filter((row) => !row.ok).map((row) => row.error), ['auth-revision-conflict'])
  assert.equal(credentials.publicStatus(dev).revision, revision + 1)
})

test('runtime reports are bounded, no-follow and structurally redacted', () => {
  const jobId = 'job-' + 'a'.repeat(32)
  history.writeReport('probe', jobId, { schemaVersion: 1, reportType: 'probe', jobId, state: 'failed',
    environmentId: 'dev', startedAt: new Date().toISOString(), finishedAt: new Date().toISOString(),
    authorization: 'Bearer should-never-land', error: { code: 'x', message: 'Bearer should-never-land' } })
  const report = history.readReport('probe', jobId)
  const serialized = JSON.stringify(report)
  assert.equal(serialized.includes('should-never-land'), false)
  assert.equal(Object.hasOwn(report, 'authorization'), false)
  assert.throws(() => history.reportFile('probe', '../escape'))
})

test('changing away from and back to an environment keeps the old preview stale', async () => {
  const dev = environments.environmentById(environments.read(), 'dev')
  const auth = credentials.publicStatus(dev)
  const jobId = 'job-' + 'c'.repeat(32)
  const now = new Date().toISOString()
  history.writeReport('probe', jobId, { schemaVersion: 1, reportType: 'probe', jobId, state: 'success',
    environmentId: 'dev', sourceKind: 'openapi', environmentRevision: environments.read().revision,
    authRevision: auth.revision, selectionRevision: 0, previewId: 'preview-' + 'd'.repeat(32),
    sourceFingerprint: 'sha256:' + 'e'.repeat(64), snapshotFingerprint: null,
    checkedAt: now, expiresAt: new Date(Date.now() + 300000).toISOString(),
    sourceSummary: { title: 'Dev', kind: 'openapi', version: '3.1.0', endpointCount: 1 },
    delta: { added: 1, changed: 0, removed: 0, potentiallyBreaking: 0 }, authState: 'configured', warnings: [],
    startedAt: now, finishedAt: now })
  await history.upsert({ jobId, reportType: 'probe', environmentId: 'dev', state: 'success', startedAt: now, finishedAt: now,
    idempotencyKey: 'preview:environment-switch', idempotencyFingerprint: 'sha256:' + 'f'.repeat(64) })
  assert.equal(backendIntegration.get().preview.fresh, true)
  assert.equal(backendIntegration.select({ environmentId: 'local', expectedStateRevision: 0 }).ok, true)
  assert.equal(backendIntegration.select({ environmentId: 'dev', expectedStateRevision: 1 }).ok, true)
  assert.equal(backendIntegration.get().preview.fresh, false)
})

test('startup recovery marks unproven running jobs interrupted', async () => {
  const jobId = 'job-' + '1'.repeat(32)
  const startedAt = new Date().toISOString()
  await history.upsert({ jobId, reportType: 'probe', environmentId: 'local', state: 'running', startedAt, finishedAt: null,
    idempotencyKey: 'recovery:running-job', idempotencyFingerprint: 'sha256:' + '2'.repeat(64) })
  assert.equal(await history.recoverInterrupted(), true)
  const row = history._readIndexForTests().jobs.find((item) => item.jobId === jobId)
  assert.equal(row.state, 'interrupted')
  assert.equal(history.readReport('probe', jobId).error.code, 'job-interrupted')
})

test('a newer failed discovery report suppresses an older refresh result', async () => {
  const environment = environments.environmentById(environments.read(), 'dev')
  const auth = credentials.publicStatus(environment)
  const refreshJobId = 'job-' + '4'.repeat(32)
  const probeJobId = 'job-' + '5'.repeat(32)
  const refreshAt = new Date(Date.now() - 2000).toISOString()
  const checkedAt = new Date().toISOString()
  history.writeReport('refresh', refreshJobId, { schemaVersion: 1, reportType: 'refresh', jobId: refreshJobId,
    state: 'success', environmentId: 'dev', startedAt: refreshAt, finishedAt: refreshAt,
    sourceFingerprint: 'sha256:' + '6'.repeat(64), currentHash: 'sha256:' + '7'.repeat(64), warnings: [] })
  await history.upsert({ jobId: refreshJobId, reportType: 'refresh', environmentId: 'dev', state: 'success',
    startedAt: refreshAt, finishedAt: refreshAt, idempotencyKey: 'refresh:before-discovery',
    idempotencyFingerprint: 'sha256:' + '8'.repeat(64) })
  history.writeReport('probe', probeJobId, { schemaVersion: 1, reportType: 'probe', jobId: probeJobId,
    state: 'failed', environmentId: 'dev', sourceKind: 'openapi', startedAt: checkedAt, finishedAt: checkedAt,
    environmentRevision: environments.read().revision, authRevision: auth.revision, selectionRevision: 2, checkedAt,
    resolution: { state: 'resolved', resolvedUrl: 'https://dev.example/openapi.json',
      candidates: [{ url: 'https://dev.example/openapi.json', title: 'Dev', kind: 'openapi' }] },
    error: { code: 'source-content-type', message: 'strict failure' }, warnings: [] })
  await history.upsert({ jobId: probeJobId, reportType: 'probe', environmentId: 'dev', state: 'failed',
    startedAt: checkedAt, finishedAt: checkedAt, idempotencyKey: 'probe:after-refresh',
    idempotencyFingerprint: 'sha256:' + '9'.repeat(64) })
  const integration = backendIntegration.get()
  assert.equal(integration.preview.state, 'failed')
  assert.equal(integration.preview.resolution.resolvedUrl, 'https://dev.example/openapi.json')
  assert.equal(integration.latestRefresh, null)
})

test('public resolution drops overlong URLs instead of changing the validated Apply target', () => {
  const longUrl = 'https://dev.example/' + 'a'.repeat(220) + '/openapi.json'
  const report = contractJobs.publicReport({ resolution: { state: 'resolved', resolvedUrl: longUrl,
    candidates: [{ url: longUrl, title: 'Long', kind: 'openapi' }], probedPaths: [longUrl] } })
  assert.equal(report.resolution.state, 'unrecognized')
  assert.equal(report.resolution.resolvedUrl, undefined)
  assert.equal(report.resolution.candidates, undefined)
  assert.equal(report.resolution.probedPaths, undefined)
  assert.equal(JSON.stringify(report).includes(longUrl.slice(0, 200)), false)
})

test('corrupt credential state and history index fail closed without being overwritten', async () => {
  const stateFile = credentials.STATE_FILE
  mkdirSync(dirname(stateFile), { recursive: true })
  writeFileSync(stateFile, '{"schemaVersion":1,"revisions":')
  chmodSync(stateFile, 0o600)
  const dev = environments.environmentById(environments.read(), 'dev')
  assert.equal(credentials.publicStatus(dev).state, 'invalid')
  assert.equal(credentials.publicStatus({ ...dev, authKind: 'x-api-key' }).kind, 'x-api-key')
  const rejected = await credentials.mutate({ environmentId: 'dev', operation: 'set', secret: 'must-not-write',
    expectedAuthRevision: 0, idempotencyKey: 'credential:corrupt-state' })
  assert.equal(rejected.error, 'credential-state-invalid')
  assert.equal(readFileSync(stateFile, 'utf8'), '{"schemaVersion":1,"revisions":')
  unlinkSync(stateFile)

  const indexFile = join(history.REPORTS_DIR, 'history-index.json')
  writeFileSync(indexFile, '{"schemaVersion":1,"jobs":[')
  assert.equal(history.list(null, 20).error, 'history-index-invalid')
  await assert.rejects(history.upsert({ jobId: 'job-' + '9'.repeat(32), reportType: 'probe', environmentId: 'dev',
    state: 'failed', startedAt: new Date().toISOString(), finishedAt: new Date().toISOString(),
    idempotencyKey: 'history:corrupt-index', idempotencyFingerprint: 'sha256:' + '8'.repeat(64) }), /history-index-invalid/)
  assert.equal(readFileSync(indexFile, 'utf8'), '{"schemaVersion":1,"jobs":[')
  unlinkSync(indexFile)

  rmSync(history.REPORTS_DIR, { recursive: true, force: true })
  const redirectedReports = join(root, 'redirected-reports')
  mkdirSync(redirectedReports)
  symlinkSync(redirectedReports, history.REPORTS_DIR, 'dir')
  const redirectedJob = 'job-' + '7'.repeat(32)
  assert.throws(() => history.writeReport('probe', redirectedJob, { schemaVersion: 1, reportType: 'probe',
    jobId: redirectedJob, state: 'failed', environmentId: 'dev', startedAt: new Date().toISOString(),
    finishedAt: new Date().toISOString(), error: { code: 'x', message: 'x' } }))
  assert.equal(existsSync(join(redirectedReports, 'probe-' + redirectedJob + '.json')), false)
  unlinkSync(history.REPORTS_DIR)
  mkdirSync(history.REPORTS_DIR, { recursive: true })
})

test('a project without a manifest or retired root artifacts can create its first source', () => {
  unlinkSync(join(contractDir, 'environments.json'))
  const before = environments.read()
  assert.equal(before.mode, 'missing')
  assert.equal(backendIntegration.get().actions.canCreateSource, true)
  const result = environments.mutate({ operation: 'create', expectedRevision: before.revision,
    idempotencyKey: 'environment:create-first', defaultEnvironmentId: 'local',
    environment: { id: 'local', label: 'Local', sourceKind: 'openapi',
      sourceUrl: 'http://127.0.0.1:8080/openapi.json', postmanEnrichmentUrl: null, authRef: null } })
  assert.equal(result.ok, true)
  assert.equal(environments.read().manifest.defaultEnvironmentId, 'local')
  assert.equal(backendIntegration.get().actions.canCreateSource, false)
})
