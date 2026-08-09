#!/usr/bin/env node

import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const REPO = join(HERE, '..', '..', '..')
const root = mkdtempSync(join(tmpdir(), 'token-source-management-'))
const orchestrator = join(root, 'orchestrator')
const figmaFileKey = 'FixtureFileKey123'
mkdirSync(join(orchestrator, 'figma', 'manifests', 'generations'), { recursive: true })
mkdirSync(join(orchestrator, 'figma', 'manifests', 'artifacts'), { recursive: true })
mkdirSync(join(orchestrator, '.cache', 'figma'), { recursive: true })
writeFileSync(join(orchestrator, 'project-config.md'), [
  '---',
  'figmaEnabled: true',
  `figmaLibraryUrl: https://www.figma.com/design/${figmaFileKey}/Fixture`,
  '---',
  ''
].join('\n'))

process.env.ORCHESTRATOR_PROJECT_ROOT = root
process.env.RUNNER_DISABLED = '1'
const require = createRequire(import.meta.url)
const generation = require('../server/figma-generation.js')
const testJobs = require('../server/figma-test-job.js')
const sources = require('../server/design-token-sources.js')

const { aggregateObservedTokens } =
  await import(join(REPO, 'orchestrator', 'figma', 'tokens', 'catalog-aggregator.mjs'))
const { normalizeSourceCapture } =
  await import(join(REPO, 'orchestrator', 'figma', 'tokens', 'source-normalizer.mjs'))
const { sourceIdentity, validObservedCapture, immutablePlan } =
  await import(join(REPO, 'orchestrator', 'figma', 'tests', 'observed-token-fixtures.mjs'))

const sha = (bytes) => 'sha256:' + createHash('sha256').update(bytes).digest('hex')
const jsonBytes = (value) => Buffer.from(JSON.stringify(value, null, 2) + '\n')
function writeJson(file, value) {
  mkdirSync(dirname(file), { recursive: true })
  writeFileSync(file, jsonBytes(value))
}
function logicalFor(role) {
  if (role === 'observed-token-source-index') return 'orchestrator/figma/tokens/source-index.json'
  if (role === 'observed-token-catalog') return 'orchestrator/figma/tokens/observed-token-catalog.json'
  const shard = /^observed-token-source-shard:([0-9]{3})$/.exec(role)
  assert.ok(shard)
  return `orchestrator/figma/tokens/sources/${shard[1]}.json`
}

const fileFingerprint = testJobs.fileKeyFingerprint(figmaFileKey)
function publishFixture(generationId, jobId, artifacts, inputFingerprint) {
  const entries = artifacts.map((artifact, index) => {
    const bytes = artifact.bytes || jsonBytes(artifact.value)
    const name = String(index).padStart(3, '0') + '-' + artifact.role.replace(/:/g, '-') + '.json'
    const relative = `orchestrator/figma/manifests/artifacts/${generationId}/tokens/${name}`
    mkdirSync(dirname(join(root, relative)), { recursive: true })
    writeFileSync(join(root, relative), bytes)
    return {
      role: artifact.role,
      group: 'tokens',
      domain: 'tokens',
      path: relative,
      logicalPath: artifact.logicalPath || logicalFor(artifact.role),
      hash: sha(bytes),
      schemaVersion: 1,
      persistence: 'committed',
      required: true,
      size: bytes.length
    }
  })
  const createdAt = new Date().toISOString()
  const manifest = {
    schemaVersion: 2,
    generationId,
    accountFingerprint: 'sha256:' + 'b'.repeat(64),
    fileKeyFingerprint: fileFingerprint,
    createdAt,
    syncJobId: jobId,
    updatedDomains: ['tokens'],
    syncGroups: {
      tokens: { status: 'completed', updated: entries.length, unchanged: 0, warnings: 0 }
    },
    groups: ['tokens'],
    domains: [{
      id: 'tokens',
      group: 'tokens',
      inputFingerprint,
      syncedAt: createdAt,
      sourceGenerationId: generationId
    }],
    artifacts: entries,
    counters: { updated: entries.length, unchanged: 0, warnings: 0 }
  }
  assert.equal(generation.validateManifest(manifest, generationId), true)
  const manifestBytes = jsonBytes(manifest)
  writeFileSync(join(orchestrator, 'figma', 'manifests', 'generations', generationId + '.json'), manifestBytes)
  writeJson(join(orchestrator, 'figma', 'manifests', 'current-generation.json'), {
    schemaVersion: 2,
    generationId,
    manifestHash: sha(manifestBytes),
    committedAt: new Date(Date.parse(createdAt) + 1).toISOString()
  })
  return generation.current()
}

const capture = validObservedCapture({
  source: sourceIdentity({ fileKeyFingerprint: fileFingerprint })
})
const captureBytes = Buffer.from(JSON.stringify(capture), 'utf8')
const batch = normalizeSourceCapture(capture, captureBytes, immutablePlan(capture))
const observed = aggregateObservedTokens({
  scope: { fileKeyFingerprint: fileFingerprint, branchKey: 'none' },
  batches: [batch],
  revision: 1
})
const initialArtifacts = observed.shards.map((row) => ({
  role: row.role, value: row.shard
})).concat([
  { role: 'observed-token-source-index', value: observed.index },
  { role: 'observed-token-catalog', value: observed.catalog }
])
let active = publishFixture(
  'gen-' + '1'.repeat(32),
  'fsj-' + '1'.repeat(32),
  initialArtifacts,
  'sha256:' + '1'.repeat(64)
)
assert.equal(active.ok, true, active.error)

let checks = 0
function check(name, fn) {
  fn()
  checks++
  console.log(`ok ${checks} - ${name}`)
}

const journalRoot = join(orchestrator, '.cache', 'figma', 'token-source-mutations')
check('GET read model is side-effect free and exposes provenance/action state', () => {
  const before = sources.list({})
  assert.equal(before.ok, true)
  assert.equal(before.rows.length, 1)
  assert.equal(before.rows[0].state, 'active')
  assert.equal(before.rows[0].activeOrigins.length, 1)
  assert.equal(before.rows[0].affectedTokenCount, capture.observations.length)
  assert.equal(before.rows[0].actions.detachOrigin, true)
  assert.equal(before.rows[0].actions.reactivate, false)
  assert.equal(existsSync(journalRoot), false)
})

writeFileSync(join(orchestrator, 'project-config.md'), [
  '---',
  'figmaEnabled: true',
  'figmaLibraryUrl: https://www.figma.com/design/DifferentFileKey456/Fixture',
  '---',
  ''
].join('\n'))
check('scope mismatch disables detach/recapture but keeps explicit retirement available', () => {
  const mismatch = sources.list({})
  assert.equal(mismatch.ok, true)
  assert.equal(mismatch.scope.state, 'scope-mismatch')
  assert.equal(mismatch.rows[0].state, 'scope-mismatch')
  assert.deepEqual(mismatch.rows[0].actions, {
    detachOrigin: false,
    retire: true,
    reactivate: false,
    retry: false,
    reason: 'source-scope-mismatch'
  })
})
writeFileSync(join(orchestrator, 'project-config.md'), [
  '---',
  'figmaEnabled: true',
  `figmaLibraryUrl: https://www.figma.com/design/${figmaFileKey}/Fixture`,
  '---',
  ''
].join('\n'))

let generationCounter = 2
let reactivationRequest = null
function publishDomains(request) {
  assert.equal(request.verifyInputs(), true)
  const result = request.completed[0]
  const generationId = 'gen-' + String(generationCounter++).repeat(32)
  active = publishFixture(generationId, request.id, result.stage.artifacts, request.inputFingerprint)
  return Promise.resolve(generationId)
}
await sources.init({
  publishDomains,
  requestDriftComparison: () => Promise.resolve({ ok: true }),
  startReactivation: (request) => {
    reactivationRequest = request
    return {
      ok: true,
      status: 202,
      job: {
        id: request.jobId,
        state: 'queued',
        result: 'queued',
        committedGenerationId: null
      }
    }
  },
  job: () => null,
  cleanupStage: () => true
})

const first = sources.list({})
const source = first.rows[0]
const retire = {
  mutationId: 'tsm_0123456789abcdef',
  action: 'retire-source',
  sourceId: source.sourceId,
  expectedGenerationRevision: first.generationRevision,
  expectedSourceIndexHash: first.sourceIndexHash,
  expectedSourceIndexRevision: first.sourceIndexRevision,
  confirmedOriginCount: source.activeOrigins.length,
  confirmedAffectedTokenCount: source.affectedTokenCount,
  detachOrigins: true,
  reason: 'The exact screen provenance was intentionally retired.'
}
const healthRoot = join(orchestrator, '.cache', 'figma', 'token-source-health')
mkdirSync(healthRoot, { recursive: true })
writeFileSync(join(healthRoot, 'current.json'), '{corrupt-health-pointer')
const retired = await sources.mutate(retire)
check('retire keeps the semantic commit and reports health recovery when the health pointer is corrupt', () => {
  assert.equal(retired.ok, true, JSON.stringify(retired))
  assert.equal(retired.status, 202)
  assert.equal(retired.healthRecoveryRequired, true)
  const after = sources.list({})
  assert.equal(after.rows[0].state, 'retired')
  assert.equal(after.rows[0].activeOrigins.length, 0)
  assert.equal(after.rows[0].retainedOrigins.length, 1)
  assert.equal(after.currentAccepted.active, 0)
  assert.equal(after.currentAccepted.retired, 1)
  assert.equal(after.rows[0].history[0].state, 'published-health-recovery-required')
})

rmSync(join(healthRoot, 'current.json'))
await sources.reconcile()
check('restart reconciliation finishes the exact pending health commit', () => {
  const after = sources.list({})
  assert.equal(after.rows[0].history[0].state, 'published')
  assert.equal(after.sourceHealthAvailable, true)
})

await (async () => {
  const replay = await sources.mutate(retire)
  assert.equal(replay.ok, true)
  assert.equal(replay.idempotent, true)
  const conflict = await sources.mutate({ ...retire, reason: 'different bytes' })
  assert.equal(conflict.ok, false)
  assert.equal(conflict.error, 'token-source-idempotency-conflict')
  checks++
  console.log(`ok ${checks} - same mutation bytes are idempotent and different bytes conflict`)
})()

await (async () => {
  const stale = await sources.mutate({
    ...retire,
    mutationId: 'tsm_1111111111111111',
    reason: 'stale request'
  })
  assert.equal(stale.ok, false)
  assert.equal(stale.error, 'token-source-cas-conflict')
  checks++
  console.log(`ok ${checks} - stale generation/source revisions are rejected before journaling`)
})()

const retained = sources.list({})
const reactivate = await sources.mutate({
  mutationId: 'tsm_2222222222222222',
  action: 'reactivate-source',
  sourceId: retained.rows[0].sourceId,
  expectedGenerationRevision: retained.generationRevision,
  expectedSourceIndexHash: retained.sourceIndexHash,
  expectedSourceIndexRevision: retained.sourceIndexRevision,
  confirmedAffectedTokenCount: retained.rows[0].affectedTokenCount,
  reason: 'Owner requested a new exact capture.'
})
check('reactivation API schedules recapture and never flips retained bytes directly', () => {
  assert.equal(reactivate.ok, true, JSON.stringify(reactivate))
  assert.equal(reactivate.status, 202)
  assert.equal(reactivationRequest.sourceId, retained.rows[0].sourceId)
  const stillRetired = sources.list({})
  assert.equal(stillRetired.rows[0].state, 'retired')
  assert.equal(stillRetired.rows[0].history[0].state, 'running')
})

const reactivationRecordFile = join(journalRoot, 'tsm_2222222222222222.json')
sources._resetForTests()
const preparedRecord = JSON.parse(readFileSync(reactivationRecordFile, 'utf8'))
preparedRecord.state = 'prepared'
writeJson(reactivationRecordFile, preparedRecord)
let resumed = 0
await sources.init({
  publishDomains,
  requestDriftComparison: () => Promise.resolve({ ok: true }),
  startReactivation: (request) => {
    resumed++
    return {
      ok: true,
      status: 202,
      job: {
        id: request.jobId,
        state: 'queued',
        result: 'queued',
        committedGenerationId: null
      }
    }
  },
  job: (jobId) => ({
    id: jobId,
    state: 'queued',
    result: 'queued',
    committedGenerationId: null
  }),
  cleanupStage: () => true
})
check('startup resumes a durably prepared reactivation with the same deterministic job id', () => {
  const after = sources.list({})
  assert.equal(resumed, 1)
  assert.equal(after.rows[0].history[0].state, 'running')
  assert.equal(after.rows[0].history[0].jobId, preparedRecord.jobId)
})

sources._resetForTests()
await sources.init({
  publishDomains,
  requestDriftComparison: () => Promise.resolve({ ok: true }),
  startReactivation: () => assert.fail('running recovery must not start a second job'),
  job: (jobId) => ({
    id: jobId,
    state: 'completed',
    result: 'interrupted',
    errorCode: 'sync-interrupted-on-restart',
    finishedAt: new Date().toISOString(),
    committedGenerationId: null
  }),
  cleanupStage: () => true
})
check('startup turns an interrupted reactivation job into an explicit failed audit row', () => {
  const after = sources.list({})
  assert.equal(after.rows[0].history[0].state, 'failed')
  assert.equal(after.rows[0].history[0].errorCode, 'TOKEN_SOURCE_REACTIVATION_FAILED')
})

check('pagination cursors are revision-bound and malformed cursors fail closed', () => {
  assert.equal(sources.list({ cursor: 'not-a-cursor' }).error, 'bad-token-source-cursor')
  const wrongRevision = Buffer.from(JSON.stringify({
    v: 1, r: 'sha256:' + 'f'.repeat(64), s: source.sourceId
  })).toString('base64url')
  assert.equal(sources.list({ cursor: wrongRevision }).error, 'bad-token-source-cursor')
})

sources._resetForTests()
rmSync(root, { recursive: true, force: true })
console.log(`token source management: ${checks} checks passed`)
