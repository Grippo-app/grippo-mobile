#!/usr/bin/env node

import assert from 'node:assert/strict'
import { mkdtempSync, mkdirSync, readFileSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const REPO = join(HERE, '..', '..', '..')
const root = mkdtempSync(join(tmpdir(), 'figma-component-token-atomic-'))
mkdirSync(join(root, 'orchestrator', 'figma', 'manifests'), { recursive: true })
mkdirSync(join(root, 'orchestrator', '.cache', 'figma'), { recursive: true })

process.env.ORCHESTRATOR_PROJECT_ROOT = root
process.env.RUNNER_DISABLED = '1'

const require = createRequire(import.meta.url)
const generation = require('../server/figma-generation.js')
const sync = require('../server/figma-sync.js')
const tokenJobs = require('../server/figma-token-jobs.js')
const limits = require('../../figma/runtime/program-limits.cjs')
const { aggregateObservedTokens } =
  await import(join(REPO, 'orchestrator', 'figma', 'tokens', 'catalog-aggregator.mjs'))
const { normalizeSourceCapture } =
  await import(join(REPO, 'orchestrator', 'figma', 'tokens', 'source-normalizer.mjs'))
const { validObservedCapture, immutablePlan } =
  await import(join(REPO, 'orchestrator', 'figma', 'tests', 'observed-token-fixtures.mjs'))
const { normalizeCapture } =
  await import(join(REPO, 'orchestrator', 'figma', 'components', 'capture-normalizer.mjs'))
const { compareComponents } =
  await import(join(REPO, 'orchestrator', 'figma', 'components', 'comparator.mjs'))
const { projectInventorySemanticHash } =
  await import(join(REPO, 'orchestrator', 'figma', 'components', 'project-inventory-contract.mjs'))
const {
  validComponentCapture,
  COMPONENT_CAPTURE_HASH,
  validProjectComponentInventory,
  validComponentAnalysisIndex,
  validComponentRegistry,
  comparatorContext
} =
  await import(join(REPO, 'orchestrator', 'figma', 'tests', 'component-fixtures.mjs'))

const jsonBytes = (value) => Buffer.from(JSON.stringify(value, null, 2) + '\n')
const hash = (char) => 'sha256:' + char.repeat(64)
const fileFingerprint = hash('a')
let jobSequence = 1
let checks = 0

function check(name, fn) {
  fn()
  checks++
  console.log(`ok ${checks} - ${name}`)
}

async function checkAsync(name, fn) {
  await fn()
  checks++
  console.log(`ok ${checks} - ${name}`)
}

function stageArtifact(source, logicalPath, role, bytes) {
  return {
    source,
    logicalPath,
    role,
    persistence: 'committed',
    required: true,
    schemaVersion: generation.artifactContractVersion(role),
    bytes,
    hash: generation.sha(bytes),
    size: bytes.length
  }
}

const observedCapture = validObservedCapture()
const observedBatch = normalizeSourceCapture(
  observedCapture,
  Buffer.from(JSON.stringify(observedCapture), 'utf8'),
  immutablePlan(observedCapture)
)
const observed = aggregateObservedTokens({
  scope: { fileKeyFingerprint: fileFingerprint, branchKey: 'none' },
  batches: [observedBatch],
  revision: 1
})
const inventory = normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)

check('component token coverage consumes only final Component Inventory v2 identity fields', () => {
  const component = inventory.components.find((row) => row.tokenRefs.length > 0)
  assert.ok(component)
  const roots = tokenJobs._test.componentCaptureRoots(inventory)
  assert.equal(roots[component.providerIdentity.nodeId], 1)
  assert.equal(roots[component.page.pageId], 1)
  assert.equal(roots[component.nodeId], undefined)
  const ref = component.tokenRefs[0]
  const coveredBatch = {
    nodeId: component.providerIdentity.nodeId,
    context: { theme: 'light', locale: 'default', platform: 'shared' },
    observations: [{
      observedTokenKey: ref.observedTokenKey,
      providerName: ref.providerName
    }]
  }
  assert.equal(tokenJobs._test.componentTokenReferenceCovered(
    component,
    coveredBatch,
    ref,
    { contextKey: () => ref.contextKey }
  ), true)
  assert.equal(tokenJobs._test.componentTokenReferenceCovered(
    { ...component, providerIdentity: { ...component.providerIdentity, nodeId: '999:1' } },
    coveredBatch,
    ref,
    { contextKey: () => ref.contextKey }
  ), false)
})

check('new component source reservations are unique and assigned by canonical sourceId order', () => {
  const reservations = [
    { captureOperationId: 'tokop_' + '1'.repeat(32), captureSequence: 1 },
    { captureOperationId: 'tokop_' + '2'.repeat(32), captureSequence: 1 }
  ]
  const first = tokenJobs._test.newComponentSourceReservationMap(
    ['otsrc:sha256:' + 'c'.repeat(64), 'otsrc:sha256:' + 'b'.repeat(64)],
    Object.create(null),
    reservations
  )
  const second = tokenJobs._test.newComponentSourceReservationMap(
    ['otsrc:sha256:' + 'b'.repeat(64), 'otsrc:sha256:' + 'c'.repeat(64)],
    Object.create(null),
    reservations
  )
  assert.deepEqual(first, second)
  assert.equal(first['otsrc:sha256:' + 'b'.repeat(64)].captureOperationId, reservations[0].captureOperationId)
  assert.equal(first['otsrc:sha256:' + 'c'.repeat(64)].captureOperationId, reservations[1].captureOperationId)
  assert.throws(() => tokenJobs._test.newComponentSourceReservationMap(
    ['otsrc:sha256:' + 'a'.repeat(64), 'otsrc:sha256:' + 'b'.repeat(64)],
    Object.create(null),
    reservations.slice(0, 1)
  ), /TOKEN_SOURCE_CAPTURE_LIMIT_EXCEEDED/)
})

const tokenArtifacts = observed.shards.map(({ role, shard }) => {
  const bucket = role.slice(-3)
  return stageArtifact(
    `sources/${bucket}.json`,
    `orchestrator/figma/tokens/sources/${bucket}.json`,
    role,
    jsonBytes(shard)
  )
}).concat([
  stageArtifact(
    'source-index.json',
    'orchestrator/figma/tokens/source-index.json',
    'observed-token-source-index',
    jsonBytes(observed.index)
  ),
  stageArtifact(
    'observed-token-catalog.json',
    'orchestrator/figma/tokens/observed-token-catalog.json',
    'observed-token-catalog',
    jsonBytes(observed.catalog)
  )
])
const componentArtifacts = [
  stageArtifact(
    'design-component-inventory.json',
    generation.COMPONENT_INVENTORY_LOGICAL_PATH,
    'design-component-inventory',
    jsonBytes(inventory)
  )
]

function completed() {
  return [{
    group: 'components',
    domain: 'components',
    inputFingerprint: hash('d'),
    stage: { artifacts: componentArtifacts.map((row) => ({ ...row, bytes: Buffer.from(row.bytes) })) }
  }, {
    group: 'tokens',
    domain: 'tokens',
    inputFingerprint: hash('d'),
    stage: { artifacts: tokenArtifacts.map((row) => ({ ...row, bytes: Buffer.from(row.bytes) })) }
  }]
}

function job(overrides = {}) {
  const suffix = String(jobSequence++).padStart(32, '0')
  return {
    id: `fsj-${suffix}`,
    scope: 'components',
    accountFingerprint: hash('b'),
    fileKeyFingerprint: fileFingerprint,
    inputFingerprint: hash('d'),
    cancelRequested: false,
    groups: [
      { group: 'components', status: 'completed', updated: componentArtifacts.length, unchanged: 0, warnings: 0 },
      { group: 'tokens', status: 'completed', updated: tokenArtifacts.length, unchanged: 0, warnings: 0 }
    ],
    ...overrides
  }
}

function pointerBytes() {
  return readFileSync(generation.POINTER_FILE)
}

function generationEvidenceCounts() {
  const active = generation.current()
  assert.equal(active.ok, true)
  assert.equal(active.mode, 'generation')
  return {
    generationId: active.manifest.generationId,
    artifactCount: active.manifest.artifacts.length,
    domains: active.manifest.domains.map((row) => row.id).sort()
  }
}

try {
  await checkAsync('valid composite publication commits one generation containing both domains', async () => {
    const generationId = await sync._test.publishComponentTokenAtomic(
      job(),
      completed(),
      { external: true, verifyInputs: () => true }
    )
    assert.match(generationId, generation.GENERATION_RE)
    assert.deepEqual(generationEvidenceCounts(), {
      generationId,
      artifactCount: tokenArtifacts.length + componentArtifacts.length,
      domains: ['components', 'tokens']
    })
  })

  await checkAsync('clean-template workspace supports offline compare and durable health/generation restart', async () => {
    const active = generation.current()
    const componentEntry = active.manifest.artifacts.find((row) => row.role === 'design-component-inventory')
    const catalogEntry = active.manifest.artifacts.find((row) => row.role === 'observed-token-catalog')
    const sourceIndexEntry = active.manifest.artifacts.find((row) => row.role === 'observed-token-source-index')
    const designInventory = JSON.parse(generation.readEntry(componentEntry).toString('utf8'))
    const catalog = JSON.parse(generation.readEntry(catalogEntry).toString('utf8'))
    const sourceIndex = JSON.parse(generation.readEntry(sourceIndexEntry).toString('utf8'))
    const recoveredCaptures = sync._test.acceptedTokenSourceCaptures(active, sourceIndex)
    assert.deepEqual(recoveredCaptures, observed.shards.flatMap(({ shard }) => shard.sources)
      .map((batch) => ({
        sourceId: batch.sourceId,
        captureOperationId: batch.captureOperationId,
        captureSequence: batch.captureSequence,
        captureEvidenceHash: batch.captureEvidenceHash
      })).sort((left, right) => left.sourceId.localeCompare(right.sourceId)))
    assert.ok(catalog.counts.activeTokens > 0)

    const project = validProjectComponentInventory()
    const analysis = validComponentAnalysisIndex([project], projectInventorySemanticHash)
    const comparison = compareComponents({
      designInventory,
      projectInventories: [project],
      analysisIndex: analysis,
      mappingRegistry: validComponentRegistry(designInventory.scopeId),
      baseline: null,
      tokenSnapshot: { report: null, registry: null },
      context: { ...comparatorContext(), designGenerationId: active.manifest.generationId }
    })
    assert.ok(comparison.report.rows.some((row) => row.status === 'matched'))

    const health = require('../server/token-source-health-store.js')
    const reservation = health.reserveMany({
      sourceIndexHash: sourceIndex.semanticHash,
      ownerId: 'fsj-' + '8'.repeat(32),
      sources: sourceIndex.sources.map((source) => ({
        sourceId: source.sourceId,
        acceptedSequence: source.acceptedBatch.captureSequence
      }))
    })
    health.complete({
      sourceIndexHash: sourceIndex.semanticHash,
      reservations: reservation.reservations,
      outcome: 'published',
      evidenceSources: reservation.reservations.map((held) => {
        const batch = observed.shards.flatMap(({ shard }) => shard.sources)
          .find((candidate) => candidate.sourceId === held.sourceId)
        assert.ok(batch)
        return {
          sourceId: held.sourceId,
          captureOperationId: held.captureOperationId,
          captureSequence: held.captureSequence,
          captureEvidenceHash: batch.captureEvidenceHash
        }
      }),
      jobId: 'fsj-' + '9'.repeat(32),
      action: 'refresh-known-token-sources',
      startedAt: new Date().toISOString(),
      summaryOutcome: 'published'
    })
    const healthHash = health.current().snapshot.index.semanticHash

    delete require.cache[require.resolve('../server/figma-generation.js')]
    delete require.cache[require.resolve('../server/token-source-health-store.js')]
    const restartedGeneration = require('../server/figma-generation.js')
    const restartedHealth = require('../server/token-source-health-store.js')
    assert.equal(restartedGeneration.current().manifest.generationId, active.manifest.generationId)
    assert.equal(restartedHealth.current().snapshot.index.semanticHash, healthHash)
  })

  const committedPointer = pointerBytes()
  const committed = generationEvidenceCounts()

  async function rejectsWithoutPointerChange(name, mutate, expected) {
    await checkAsync(name, async () => {
      const next = completed()
      const nextJob = job()
      mutate(next, nextJob)
      await assert.rejects(
        sync._test.publishComponentTokenAtomic(
          nextJob,
          next,
          { external: true, verifyInputs: () => true }
        ),
        expected
      )
      assert.deepEqual(pointerBytes(), committedPointer)
      assert.deepEqual(generationEvidenceCounts(), committed)
    })
  }

  await rejectsWithoutPointerChange(
    'missing token sibling fails before publication',
    (next) => next.splice(1, 1),
    /component-token-atomic-domain-set-incomplete/
  )
  await rejectsWithoutPointerChange(
    'duplicate component domain fails before publication',
    (next) => {
      next[1].domain = 'components'
      next[1].group = 'components'
    },
    /component-token-atomic-domain-set-incomplete/
  )
  await rejectsWithoutPointerChange(
    'artifact hash mismatch fails preflight before publication',
    (next) => { next[0].stage.artifacts[0].hash = hash('f') },
    /publication-artifact-invalid/
  )
  await rejectsWithoutPointerChange(
    'schema-invalid artifact bytes fail preflight before publication',
    (next) => {
      const artifact = next[0].stage.artifacts[0]
      artifact.bytes = jsonBytes({})
      artifact.hash = generation.sha(artifact.bytes)
      artifact.size = artifact.bytes.length
    },
    /publication-artifact-invalid/
  )
  await rejectsWithoutPointerChange(
    'non-canonical stage path fails preflight before publication',
    (next) => { next[0].stage.artifacts[0].source = '../inventory.json' },
    /publication-artifact-invalid/
  )

  await checkAsync('changed account/file/config snapshot fails CAS with no pointer change', async () => {
    await assert.rejects(
      sync._test.publishComponentTokenAtomic(
        job(),
        completed(),
        { external: true, verifyInputs: () => false }
      ),
      /plan-stale/
    )
    assert.deepEqual(pointerBytes(), committedPointer)
    assert.deepEqual(generationEvidenceCounts(), committed)
  })

  await checkAsync('cancellation publishes neither domain and leaves the pointer unchanged', async () => {
    const result = await sync._test.publishComponentTokenAtomic(
      job({ cancelRequested: true }),
      completed(),
      { external: true, verifyInputs: () => true }
    )
    assert.equal(result, null)
    assert.deepEqual(pointerBytes(), committedPointer)
    assert.deepEqual(generationEvidenceCounts(), committed)
  })

  check('composite artifact ceiling accepts 195 and rejects 196', () => {
    const atLimit = Array.from(
      { length: limits.compositePublicationArtifactsMax },
      () => ({ size: 0 })
    )
    assert.equal(sync._test.publicationBudgetError(
      atLimit,
      limits.compositePublicationArtifactsMax,
      limits.phaseBytesMax,
      'composite'
    ), null)
    assert.equal(sync._test.publicationBudgetError(
      atLimit.concat({ size: 0 }),
      limits.compositePublicationArtifactsMax,
      limits.phaseBytesMax,
      'composite'
    ), 'composite')
  })

  check('phase artifact ceiling accepts 200 and rejects 201', () => {
    const atLimit = Array.from({ length: generation.ARTIFACTS_MAX }, () => ({ size: 0 }))
    assert.equal(sync._test.publicationBudgetError(
      atLimit,
      generation.ARTIFACTS_MAX,
      limits.phaseBytesMax,
      'phase'
    ), null)
    assert.equal(sync._test.publicationBudgetError(
      atLimit.concat({ size: 0 }),
      generation.ARTIFACTS_MAX,
      limits.phaseBytesMax,
      'phase'
    ), 'phase')
  })

  check('unobserved component-owned sources are superseded instead of reported as published', () => {
    const first = {
      sourceId: 'tksrc_' + '1'.repeat(64),
      captureOperationId: 'tokop_' + '1'.repeat(32),
      captureSequence: 2
    }
    const second = {
      sourceId: 'tksrc_' + '2'.repeat(64),
      captureOperationId: 'tokop_' + '2'.repeat(32),
      captureSequence: 3
    }
    const held = { reservations: [first, second], unusedReservations: [] }
    tokenJobs._test.supersedeUnobservedComponentReservations(held, [first.sourceId])
    assert.deepEqual(held.reservations, [second])
    assert.deepEqual(held.unusedReservations, [first])
  })

  check('byte ceiling accepts exactly 64 MiB and rejects one byte more', () => {
    assert.equal(sync._test.publicationBudgetError(
      [{ size: limits.phaseBytesMax }],
      limits.compositePublicationArtifactsMax,
      limits.phaseBytesMax,
      'bytes'
    ), null)
    assert.equal(sync._test.publicationBudgetError(
      [{ size: limits.phaseBytesMax + 1 }],
      limits.compositePublicationArtifactsMax,
      limits.phaseBytesMax,
      'bytes'
    ), 'bytes')
  })
} finally {
  rmSync(root, { recursive: true, force: true })
}

console.log(`figma component+token atomic publication: ${checks} checks passed`)
