#!/usr/bin/env node

import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import {
  chmodSync, mkdirSync, mkdtempSync, readFileSync, rmSync, symlinkSync, writeFileSync
} from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const REPO = join(HERE, '..', '..', '..')
const root = mkdtempSync(join(tmpdir(), 'observed-token-mappings-'))
const orchestrator = join(root, 'orchestrator')
const cache = join(orchestrator, '.cache', 'tasks')
for (const dir of ['locks', 'requests', 'request-reservations', 'runs', 'superseded', 'finalizations', 'creations', 'edits', 'intake']) {
  mkdirSync(join(cache, dir), { recursive: true })
}
if (process.platform !== 'win32') chmodSync(join(cache, 'intake'), 0o700)
mkdirSync(join(orchestrator, 'figma', 'manifests', 'generations'), { recursive: true })
mkdirSync(join(orchestrator, 'tasks'), { recursive: true })

process.env.ORCHESTRATOR_PROJECT_ROOT = root
process.env.ORCHESTRATOR_TASKS_DIR = join(orchestrator, 'tasks')
process.env.ORCHESTRATOR_LOCKS_DIR = join(cache, 'locks')
process.env.ORCHESTRATOR_REQUESTS_DIR = join(cache, 'requests')
process.env.ORCHESTRATOR_REQUEST_RESERVATIONS_DIR = join(cache, 'request-reservations')
process.env.ORCHESTRATOR_RUNS_DIR = join(cache, 'runs')
process.env.ORCHESTRATOR_SUPERSEDED_DIR = join(cache, 'superseded')
process.env.ORCHESTRATOR_FINALIZATIONS_DIR = join(cache, 'finalizations')
process.env.ORCHESTRATOR_TASK_CREATIONS_DIR = join(cache, 'creations')
process.env.ORCHESTRATOR_TASK_EDITS_DIR = join(cache, 'edits')
process.env.ORCHESTRATOR_TASK_INTAKE_DIR = join(cache, 'intake')
process.env.RUNNER_DISABLED = '1'

const require = createRequire(import.meta.url)
const generation = require('../server/figma-generation.js')
const designMappings = require('../server/design-mappings.js')
const tokenState = require('../server/design-token-state.js')

mkdirSync(join(orchestrator, 'figma', 'schemas'), { recursive: true })
writeFileSync(
  join(orchestrator, 'figma', 'schemas', 'token-mappings.schema.json'),
  readFileSync(join(REPO, 'orchestrator', 'figma', 'schemas', 'token-mappings.schema.json'))
)
mkdirSync(join(orchestrator, 'figma', 'tokens'), { recursive: true })
for (const name of ['mapping-contract.mjs']) {
  writeFileSync(join(orchestrator, 'figma', 'tokens', name), readFileSync(join(REPO, 'orchestrator', 'figma', 'tokens', name)))
}
mkdirSync(join(orchestrator, 'figma', 'runtime'), { recursive: true })
writeFileSync(
  join(orchestrator, 'figma', 'runtime', 'canonical-json.mjs'),
  readFileSync(join(REPO, 'orchestrator', 'figma', 'runtime', 'canonical-json.mjs'))
)
mkdirSync(join(orchestrator, 'figma', 'node_modules'), { recursive: true })
symlinkSync(join(REPO, 'orchestrator', 'figma', 'node_modules', 'ajv'), join(orchestrator, 'figma', 'node_modules', 'ajv'))

const { aggregateObservedTokens } = await import(join(REPO, 'orchestrator', 'figma', 'tokens', 'catalog-aggregator.mjs'))
const { bindObservedTokens } = await import(join(REPO, 'orchestrator', 'figma', 'tokens', 'binder.mjs'))
const { compareTokens } = await import(join(REPO, 'orchestrator', 'figma', 'tokens', 'comparator.mjs'))
const { emptyMappingRegistry } = await import(join(REPO, 'orchestrator', 'figma', 'tokens', 'mapping-contract.mjs'))
const { normalizeSourceCapture } = await import(join(REPO, 'orchestrator', 'figma', 'tokens', 'source-normalizer.mjs'))
const { canonicalHash } = await import(join(REPO, 'orchestrator', 'figma', 'runtime', 'canonical-json.mjs'))
const { loadAdapterConfig } = await import(join(REPO, 'orchestrator', 'figma', 'runtime', 'adapter-config.mjs'))
const { extractProjectTokens } = await import(join(REPO, 'orchestrator', 'figma', 'runtime', 'token-extraction.mjs'))
const { createSchemaRegistry } = await import(join(REPO, 'orchestrator', 'figma', 'runtime', 'schema-registry.mjs'))
const { immutablePlan, validObservedCapture } =
  await import(join(REPO, 'orchestrator', 'figma', 'tests', 'observed-token-fixtures.mjs'))

const schemas = createSchemaRegistry(join(REPO, 'orchestrator', 'figma', 'schemas'))
const sha = (bytes) => 'sha256:' + createHash('sha256').update(bytes).digest('hex')
const jsonBytes = (value) => Buffer.from(JSON.stringify(value, null, 2) + '\n')
function writeJson(file, value) {
  mkdirSync(dirname(file), { recursive: true })
  writeFileSync(file, jsonBytes(value))
}

const adapterDocument = {
  schemaVersion: 2,
  adapters: [{
    id: 'fixture-json',
    kind: 'json-tokens',
    version: 2,
    enabled: true,
    capabilities: ['tokens'],
    platform: 'shared',
    authority: 'handwritten',
    tokens: {
      roots: ['design/tokens'],
      include: ['**/*.json'],
      exclude: [],
      modes: ['shared'],
      authorities: { color: { contracts: ['AppColor'] } },
      contextMap: [{
        when: { theme: 'light', locale: 'default', platform: 'shared' },
        projectMode: 'shared'
      }],
      bindingRules: []
    }
  }]
}
writeJson(join(orchestrator, 'figma', 'project-adapters.json'), adapterDocument)
writeJson(join(root, 'design', 'tokens', 'tokens.json'), {
  AppColor: {
    primary: { value: '#336699' },
    secondary: { value: '#112233' }
  }
})
const config = loadAdapterConfig({
  projectRoot: root,
  schemaValidate: schemas.validate('project-adapters')
})
const extraction = extractProjectTokens({
  projectRoot: root,
  config: config.config,
  configHash: config.tokenConfigHash,
  schemaValidators: {
    projectInventory: schemas.validate('project-token-inventory'),
    analysisIndex: schemas.validate('project-token-analysis-index')
  }
})
const capture = validObservedCapture()
const batch = normalizeSourceCapture(capture, Buffer.from(JSON.stringify(capture)), immutablePlan(capture))
const observed = aggregateObservedTokens({
  scope: {
    fileKeyFingerprint: capture.source.fileKeyFingerprint,
    branchKey: capture.source.branchKey
  },
  batches: [batch],
  revision: 1
})
const registry = emptyMappingRegistry(observed.catalog.scope)
const bindingSnapshot = bindObservedTokens({
  catalog: observed.catalog,
  projectInventories: extraction.inventories,
  adapterConfig: { ...config.config, tokenConfigHash: config.tokenConfigHash },
  mappingRegistry: registry,
  projectAnalysisHash: canonicalHash(extraction.index)
})
const { report } = compareTokens({
  observedCatalog: observed.catalog,
  sourceIndex: observed.index,
  projectInventories: extraction.inventories,
  analysisIndex: extraction.index,
  bindingSnapshot,
  mappingRegistry: registry,
  context: {
    analysisIndexHash: canonicalHash(extraction.index),
    adapterConfigHash: config.tokenConfigHash,
    baselineHash: 'none',
    sourceFreshness: 'current'
  }
})

const generationId = 'gen-' + '1'.repeat(32)
const artifactDefs = []
function addArtifact(group, domain, role, logicalPath, value, required = true) {
  const bytes = Buffer.isBuffer(value) ? value : jsonBytes(value)
  const source = role.replace(/[^A-Za-z0-9.-]+/g, '-') + '.json'
  const relative = 'orchestrator/figma/manifests/artifacts/' + generationId + '/' + group + '/' + source
  writeJson(join(root, relative), JSON.parse(bytes.toString('utf8')))
  artifactDefs.push({
    role, group, domain, path: relative, logicalPath,
    hash: sha(bytes), schemaVersion: value.schemaVersion || 1,
    persistence: 'committed', required, size: bytes.length
  })
}
for (const entry of observed.shards) {
  const suffix = entry.role.slice(entry.role.lastIndexOf(':') + 1)
  addArtifact('tokens', 'tokens', entry.role, 'orchestrator/figma/tokens/sources/' + suffix + '.json', entry.shard)
}
addArtifact('tokens', 'tokens', 'observed-token-source-index', 'orchestrator/figma/tokens/source-index.json', observed.index)
addArtifact('tokens', 'tokens', 'observed-token-catalog', 'orchestrator/figma/tokens/observed-token-catalog.json', observed.catalog)
addArtifact('drift', 'token-drift', 'project-token-analysis-index',
  generation.TOKEN_COMPARISON_REPORT_DIR + 'analysis-index.json', extraction.index)
addArtifact('drift', 'token-drift', 'project-token-inventory:fixture-json',
  generation.TOKEN_COMPARISON_REPORT_DIR + 'project-inventory-fixture-json.json', extraction.inventories[0])
addArtifact('drift', 'token-drift', 'token-binding-snapshot',
  generation.TOKEN_COMPARISON_REPORT_DIR + 'binding-snapshot.json', bindingSnapshot)
addArtifact('drift', 'token-drift', 'token-mapping-snapshot',
  generation.TOKEN_COMPARISON_REPORT_DIR + 'mapping-snapshot.json', registry)
addArtifact('drift', 'token-drift', 'token-comparison',
  generation.TOKEN_COMPARISON_REPORT_DIR + 'comparison.json', report)

const tokenCount = artifactDefs.filter((row) => row.group === 'tokens').length
const driftCount = artifactDefs.filter((row) => row.group === 'drift').length
const createdAt = '2026-07-23T12:00:00.000Z'
const manifest = {
  schemaVersion: 2,
  generationId,
  accountFingerprint: capture.accountFingerprint,
  fileKeyFingerprint: capture.source.fileKeyFingerprint,
  createdAt,
  syncJobId: 'fsj-' + '2'.repeat(32),
  updatedDomains: ['token-drift', 'tokens'],
  syncGroups: {
    tokens: { status: 'completed', updated: tokenCount, unchanged: 0, warnings: 0 },
    drift: { status: 'completed', updated: driftCount, unchanged: 0, warnings: 0 }
  },
  groups: ['tokens', 'drift'],
  domains: [
    { id: 'token-drift', group: 'drift', inputFingerprint: 'sha256:' + 'c'.repeat(64), syncedAt: createdAt, sourceGenerationId: generationId },
    { id: 'tokens', group: 'tokens', inputFingerprint: 'sha256:' + 'd'.repeat(64), syncedAt: createdAt, sourceGenerationId: generationId }
  ],
  artifacts: artifactDefs,
  counters: { updated: artifactDefs.length, unchanged: 0, warnings: 0 }
}
assert.equal(generation.validateManifest(manifest, generationId), true, 'v2 fixture manifest must validate')
const manifestBytes = jsonBytes(manifest)
writeFileSync(join(orchestrator, 'figma', 'manifests', 'generations', generationId + '.json'), manifestBytes)
writeJson(join(orchestrator, 'figma', 'manifests', 'current-generation.json'), {
  schemaVersion: 2,
  generationId,
  manifestHash: sha(manifestBytes),
  committedAt: '2026-07-23T12:00:01.000Z'
})
const active = generation.current()
assert.equal(active.ok, true, active.error)

const color = observed.catalog.tokens.find((token) => token.providerName === 'color/content/primary')
const contextKey = report.observedRows.find((row) => row.observedTokenKey === color.observedTokenKey).contextKey
const contextSelector = JSON.parse(contextKey)
let operationCounter = 0
const nextOperationId = () => 'mop-' + (++operationCounter).toString(16).padStart(32, '0')
const designGenerationRevision = active.pointer.manifestHash
const projectInventoryRevision = artifactDefs.find((row) => row.role === 'project-token-analysis-index').hash
const upsert = (overrides = {}) => ({
  op: 'upsert-mapping',
  observedTokenKey: color.observedTokenKey,
  contextSelector,
  adapterId: 'fixture-json',
  projectTokenIds: ['fixture-json:AppColor.primary'],
  relation: 'one-to-one',
  ...overrides
})
const request = (overrides = {}) => ({
  operationId: nextOperationId(),
  expectedMappingRevision: 0,
  expectedDesignGenerationRevision: designGenerationRevision,
  expectedProjectInventoryRevision: projectInventoryRevision,
  expectedComparisonSemanticHash: report.semanticHash,
  operations: [upsert()],
  ...overrides
})

let checks = 0
async function check(name, fn) {
  await fn()
  checks++
  console.log(`ok ${checks} - ${name}`)
}

try {
  await check('absent registry is an exact scoped v2 revision zero', async () => {
    const listed = await designMappings.get()
    assert.equal(listed.ok, true)
    assert.equal(listed.present, false)
    assert.equal(listed.revision, 0)
    assert.deepEqual(listed.scope, observed.catalog.scope)
    assert.equal(listed.scopeMatchesObserved, true)
    assert.equal(tokenState.readTokenSignals().adapters.state, 'configured')
  })

  await check('stale comparison and missing project targets fail without writes', async () => {
    const stale = await designMappings.mutate(request({
      expectedComparisonSemanticHash: 'sha256:' + '9'.repeat(64)
    }))
    assert.equal(stale.error, 'TOKEN_FINDING_STALE')
    const missing = await designMappings.mutate(request({
      operations: [upsert({ projectTokenIds: ['fixture-json:AppColor.absent'] })]
    }))
    assert.equal(missing.error, 'TOKEN_MAPPING_TARGET_MISSING')
    assert.equal(tokenState.readTokenSignals().mappingState, 'absent')
  })

  let committedRequest
  await check('valid confirmation operation writes only the observed-token v2 mapping shape', async () => {
    committedRequest = request()
    const result = await designMappings.mutate(committedRequest)
    assert.equal(result.ok, true, JSON.stringify(result))
    const written = JSON.parse(readFileSync(designMappings.MAPPING_FILE, 'utf8'))
    assert.equal(written.schemaVersion, 2)
    assert.deepEqual(written.scope, observed.catalog.scope)
    assert.equal(written.mappings[0].observedTokenKey, color.observedTokenKey)
    assert.deepEqual(written.mappings[0].contextSelector, contextSelector)
    assert.equal(written.mappings[0].expectedKind, 'color')
    assert.equal(written.mappings[0].provenance.kind, 'manual-review')
    assert.equal(Object.hasOwn(written.mappings[0], 'designTokenId'), false)
    assert.equal(Object.hasOwn(written.mappings[0], 'modeMap'), false)
    assert.equal(tokenState.readTokenSignals().mappingRevision, 1)
  })

  await check('same-file receipt replays exact bytes and rejects operation-id reuse', async () => {
    const replay = await designMappings.mutate(committedRequest)
    assert.equal(replay.ok, true)
    assert.equal(replay.replayed, true)
    const conflict = await designMappings.mutate({
      ...request({ expectedMappingRevision: 1 }),
      operationId: committedRequest.operationId
    })
    assert.equal(conflict.error, 'TOKEN_MAPPING_OPERATION_CONFLICT')
  })

  await check('retirement and project-only disposition use current v2 tombstones', async () => {
    const listed = await designMappings.get()
    const retired = await designMappings.mutate(request({
      expectedMappingRevision: 1,
      expectedComparisonSemanticHash: null,
      operations: [{ op: 'retire-mapping', mappingId: listed.mappings[0].mappingId, reason: 'replaced by current system' }]
    }))
    assert.equal(retired.ok, true, JSON.stringify(retired))
    const disposition = await designMappings.mutate(request({
      expectedMappingRevision: 2,
      operations: [{
        op: 'add-disposition',
        side: 'project',
        adapterId: 'fixture-json',
        projectTokenId: 'fixture-json:AppColor.secondary',
        kind: 'project-only-intentional',
        reason: 'local semantic token'
      }]
    }))
    assert.equal(disposition.ok, true, JSON.stringify(disposition))
    const written = JSON.parse(readFileSync(designMappings.MAPPING_FILE, 'utf8'))
    assert.equal(written.mappings[0].state, 'retired')
    assert.equal(written.dispositions[0].reviewPolicy, 'on-change')
  })

  await check('foreign scope blocks authoring until explicit fresh onboarding', async () => {
    const foreign = JSON.parse(readFileSync(designMappings.MAPPING_FILE, 'utf8'))
    foreign.scope.fileKeyFingerprint = 'sha256:' + 'f'.repeat(64)
    writeJson(designMappings.MAPPING_FILE, foreign)
    const blocked = await designMappings.mutate(request({ expectedMappingRevision: 3 }))
    assert.equal(blocked.error, 'TOKEN_MAPPING_SCOPE_CHANGED')
    const onboard = await designMappings.mutate(request({
      expectedMappingRevision: 3,
      expectedComparisonSemanticHash: null,
      operations: [{ op: 'onboard-fresh' }]
    }))
    assert.equal(onboard.ok, true, JSON.stringify(onboard))
    const written = JSON.parse(readFileSync(designMappings.MAPPING_FILE, 'utf8'))
    assert.deepEqual(written.scope, observed.catalog.scope)
    assert.deepEqual(written.mappings, [])
    assert.deepEqual(written.dispositions, [])
  })

  await check('a corrupt immutable observed catalog is recovery-required, never an empty registry', async () => {
    const entry = artifactDefs.find((row) => row.role === 'observed-token-catalog')
    const file = join(root, entry.path)
    const original = readFileSync(file)
    writeFileSync(file, '{corrupt-catalog')
    try {
      const listed = await designMappings.get()
      assert.equal(listed.ok, false)
      assert.equal(listed.error, 'TOKEN_GENERATION_RESYNC_REQUIRED')
      const mutated = await designMappings.mutate(request({
        expectedMappingRevision: 4,
        expectedComparisonSemanticHash: null,
        operations: [{ op: 'onboard-fresh' }]
      }))
      assert.equal(mutated.ok, false)
      assert.equal(mutated.error, 'TOKEN_GENERATION_RESYNC_REQUIRED')
    } finally {
      writeFileSync(file, original)
    }
  })
} finally {
  rmSync(root, { recursive: true, force: true })
}

console.log(`design-token-mappings v2: ${checks} checks passed`)
