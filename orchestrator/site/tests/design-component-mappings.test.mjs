#!/usr/bin/env node
// design-component-mappings.test.mjs — CAS + identity-resolution pins for the
// component mapping registry mutations (server/design-component-mappings.js),
// the cheap component signals (server/design-component-state.js), and the
// §33.4-12 restart proof: startupReconcile over an unchanged workspace keeps
// the published comparison and leaves no writer-lease tail; an offline source
// edit latches project-dirty. Every mutation resolves ids against a real
// committed generation fixture; nothing here trusts client fields.

import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { chmodSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync, symlinkSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join, dirname } from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const REPO = join(HERE, '..', '..', '..')
const root = mkdtempSync(join(tmpdir(), 'design-component-mappings-'))
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
const fsModule = require('node:fs')
const generation = require('../server/figma-generation.js')
const componentMappings = require('../server/design-component-mappings.js')
const componentState = require('../server/design-component-state.js')
const componentCompare = require('../server/design-component-compare.js')
const figmaSync = require('../server/figma-sync.js')

// design-component-mappings resolves its ESM contracts + ajv relative to
// paths.PROJECT_ROOT — mirror the real figma modules into the fixture root.
mkdirSync(join(orchestrator, 'figma', 'schemas'), { recursive: true })
for (const name of ['component-mappings.schema.json']) {
  writeFileSync(join(orchestrator, 'figma', 'schemas', name), readFileSync(join(REPO, 'orchestrator', 'figma', 'schemas', name)))
}
mkdirSync(join(orchestrator, 'figma', 'components'), { recursive: true })
for (const name of ['mapping-contract.mjs', 'limits.mjs', 'error-codes.mjs']) {
  writeFileSync(join(orchestrator, 'figma', 'components', name), readFileSync(join(REPO, 'orchestrator', 'figma', 'components', name)))
}
mkdirSync(join(orchestrator, 'figma', 'runtime'), { recursive: true })
for (const name of ['canonical-json.mjs', 'typed-error.mjs', 'adapter-config.mjs', 'adapter-registry.mjs', 'component-extraction.mjs', 'token-extraction.mjs', 'project-identity.mjs', 'input-snapshot.mjs', 'glob.mjs', 'error-codes.mjs', 'run-plan.mjs']) {
  writeFileSync(join(orchestrator, 'figma', 'runtime', name), readFileSync(join(REPO, 'orchestrator', 'figma', 'runtime', name)))
}
writeFileSync(join(orchestrator, 'figma', 'runtime', 'project-identity.cjs'),
  readFileSync(join(REPO, 'orchestrator', 'figma', 'runtime', 'project-identity.cjs')))
writeFileSync(join(orchestrator, 'figma', 'runtime', 'adapter-config-identity.cjs'),
  readFileSync(join(REPO, 'orchestrator', 'figma', 'runtime', 'adapter-config-identity.cjs')))
mkdirSync(join(orchestrator, 'figma', 'adapters', 'component-manifest'), { recursive: true })
writeFileSync(join(orchestrator, 'figma', 'adapters', 'component-manifest', 'components.mjs'),
  readFileSync(join(REPO, 'orchestrator', 'figma', 'adapters', 'component-manifest', 'components.mjs')))
mkdirSync(join(orchestrator, 'figma', 'adapters', 'kotlin-compose'), { recursive: true })
for (const name of ['tokens.mjs', 'components.mjs']) {
  writeFileSync(join(orchestrator, 'figma', 'adapters', 'kotlin-compose', name), readFileSync(join(REPO, 'orchestrator', 'figma', 'adapters', 'kotlin-compose', name)))
}
mkdirSync(join(orchestrator, 'figma', 'adapters', 'json-tokens'), { recursive: true })
writeFileSync(join(orchestrator, 'figma', 'adapters', 'json-tokens', 'tokens.mjs'),
  readFileSync(join(REPO, 'orchestrator', 'figma', 'adapters', 'json-tokens', 'tokens.mjs')))
mkdirSync(join(orchestrator, 'figma', 'tokens'), { recursive: true })
for (const name of ['project-inventory-contract.mjs', 'limits.mjs', 'error-codes.mjs']) {
  writeFileSync(join(orchestrator, 'figma', 'tokens', name), readFileSync(join(REPO, 'orchestrator', 'figma', 'tokens', name)))
}
writeFileSync(join(orchestrator, 'figma', 'components', 'design-inventory-contract.mjs'),
  readFileSync(join(REPO, 'orchestrator', 'figma', 'components', 'design-inventory-contract.mjs')))
writeFileSync(join(orchestrator, 'figma', 'components', 'capture-normalizer.mjs'),
  readFileSync(join(REPO, 'orchestrator', 'figma', 'components', 'capture-normalizer.mjs')))
writeFileSync(join(orchestrator, 'figma', 'components', 'project-inventory-contract.mjs'),
  readFileSync(join(REPO, 'orchestrator', 'figma', 'components', 'project-inventory-contract.mjs')))
mkdirSync(join(orchestrator, 'figma', 'node_modules'), { recursive: true })
symlinkSync(join(REPO, 'orchestrator', 'figma', 'node_modules', 'ajv'), join(orchestrator, 'figma', 'node_modules', 'ajv'))
// kotlin-compose extractor resolves tree-sitter from the scripts tree; link it too.
for (const dep of ['tree-sitter', 'tree-sitter-kotlin']) {
  try { symlinkSync(join(REPO, 'orchestrator', 'figma', 'node_modules', dep), join(orchestrator, 'figma', 'node_modules', dep)) } catch {}
}
mkdirSync(join(orchestrator, 'figma', 'scripts'), { recursive: true })
for (const rel of ['compose-model']) {
  try { symlinkSync(join(REPO, 'orchestrator', 'figma', 'scripts', rel), join(orchestrator, 'figma', 'scripts', rel)) } catch {}
}

const { normalizeCapture } = await import(join(REPO, 'orchestrator', 'figma', 'components', 'capture-normalizer.mjs'))
const { compareComponents } = await import(join(REPO, 'orchestrator', 'figma', 'components', 'comparator.mjs'))
const { suggestComponentTasks } = await import(join(REPO, 'orchestrator', 'figma', 'components', 'task-suggestions.mjs'))
const { emptyMappingRegistry } = await import(join(REPO, 'orchestrator', 'figma', 'components', 'mapping-contract.mjs'))
const { projectInventorySemanticHash } = await import(join(REPO, 'orchestrator', 'figma', 'components', 'project-inventory-contract.mjs'))
const { loadAdapterConfig } = await import(join(REPO, 'orchestrator', 'figma', 'runtime', 'adapter-config.mjs'))
const { extractProjectComponents } = await import(join(REPO, 'orchestrator', 'figma', 'runtime', 'component-extraction.mjs'))

const sha = (bytes) => 'sha256:' + createHash('sha256').update(bytes).digest('hex')
const jsonBytes = (value) => Buffer.from(JSON.stringify(value, null, 2) + '\n')

// ── design capture fixture (mirrors component-acceptance.test.mjs) ──────────
const capture = {
  schemaVersion: 2,
  provider: 'figma',
  providerIdentity: { fileKeyFingerprint: 'sha256:' + 'a'.repeat(64), branchKey: 'none', libraryOriginPolicy: 'local-authoritative' },
  scope: { kind: 'all-pages' },
  pages: [{ pageId: '1:1', name: 'Components' }],
  entities: [
    {
      nodeId: '10:1', pageId: '1:1', kind: 'component-set', name: 'AppButton', idQuality: 'stable',
      properties: [
        { propertyId: 'p:size', name: 'Size', type: 'variant', idQuality: 'stable', options: ['Small', 'Large'], defaultValue: 'Small' },
        { propertyId: 'p:enabled', name: 'Enabled', type: 'boolean', idQuality: 'stable', defaultValue: true },
        { propertyId: 'p:label', name: 'Label', type: 'text', idQuality: 'stable' },
      ],
      variants: [
        { nodeId: '10:2', name: 'Size=Small', assignments: { 'p:size': 'Small' }, isDefault: true },
        { nodeId: '10:3', name: 'Size=Large', assignments: { 'p:size': 'Large' } },
      ],
      expectedVariantCount: 2, nestedRefs: [], boundVariables: [],
    },
  ],
  visual: [],
  witness: {
    startedAt: '2026-01-01T00:00:00.000Z', finishedAt: '2026-01-01T00:00:05.000Z',
    providerRevisionBefore: 'r1', providerRevisionAfter: 'r1',
    consistency: 'proven', completeness: 'complete',
    requestedPageIds: ['1:1'], readPageIds: ['1:1'],
    expectedEntityCount: 1, readEntityCount: 1,
    truncated: false, permissionDegraded: false, limitsHit: [],
  },
}
const designInventory = normalizeCapture(capture, sha(JSON.stringify(capture)))
const buttonSet = designInventory.components[0]

// ── project fixture inside the temp root (component-manifest adapter) ───────
mkdirSync(join(root, 'design', 'components'), { recursive: true })
mkdirSync(join(root, 'design', 'components', 'nested'), { recursive: true })
const manifestBody = {
  schemaVersion: 2,
  components: [{
    name: 'AppButton', symbol: 'ui/button', visibility: 'public',
    props: [
      { name: 'size', kind: 'enum', values: ['Small', 'Large'], default: 'Small' },
      { name: 'enabled', kind: 'boolean', default: true },
      { name: 'label', kind: 'text', required: true },
    ],
  }],
}
writeFileSync(join(root, 'design', 'components', 'manifest.json'), JSON.stringify(manifestBody, null, 2))
writeFileSync(join(orchestrator, 'figma', 'project-adapters.json'), JSON.stringify({
  schemaVersion: 2,
  adapters: [{
    id: 'fixture-manifest', kind: 'component-manifest', version: 2, enabled: true,
    capabilities: ['components'], platform: 'web', authority: 'handwritten',
    components: { roots: ['design/components'], include: ['**/*.json'], exclude: [], visibility: ['public'] },
  }],
}, null, 2))
const configState = loadAdapterConfig({ projectRoot: root })
assert.equal(configState.state, 'configured')
const extraction = extractProjectComponents({ projectRoot: root, config: configState.config, configHash: configState.componentConfigHash })
const projectInventory = extraction.inventories[0]
const analysisIndex = {
  schemaVersion: 2,
  configHash: configState.componentConfigHash,
  adapters: [{
    adapterId: projectInventory.adapterId, platform: projectInventory.platform,
    role: `project-component-inventory:${projectInventory.adapterId}`,
    inventoryHash: projectInventorySemanticHash(projectInventory),
    scopeFingerprint: projectInventory.scopeFingerprint, complete: true,
  }],
  complete: true,
}
const { report } = compareComponents({
  designInventory,
  projectInventories: [projectInventory],
  analysisIndex,
  mappingRegistry: emptyMappingRegistry(designInventory.scopeId),
  baseline: null,
  tokenSnapshot: { report: null, registry: null },
  context: { designGenerationId: 'gen-' + 'f'.repeat(32), adapterConfigHash: configState.componentConfigHash, adapterConfigFileHash: configState.componentConfigFileHash },
})
const suggestions = { schemaVersion: 2, comparisonSemanticHash: report.semanticHash, byDesignComponent: [] }
const taskSuggestions = suggestComponentTasks(report)

// ── committed generation fixture ────────────────────────────────────────────
const generationId = 'gen-' + '1'.repeat(32)
const artifactDefs = []
function addArtifact(group, domain, role, logicalPath, bytes) {
  const source = logicalPath.split('/').pop()
  const path = 'orchestrator/figma/manifests/artifacts/' + generationId + '/' + group + '/' + source
  const file = join(root, path)
  mkdirSync(dirname(file), { recursive: true })
  writeFileSync(file, bytes)
  artifactDefs.push({
    role, group, domain, path, logicalPath,
    hash: sha(bytes), schemaVersion: generation.artifactContractVersion(role), persistence: 'committed',
    required: true, size: bytes.length
  })
}
const DRIFT_DIR = generation.COMPONENT_COMPARISON_REPORT_DIR
addArtifact('components', 'components', 'design-component-inventory', generation.COMPONENT_INVENTORY_LOGICAL_PATH, jsonBytes(designInventory))
addArtifact('drift', 'component-drift', 'project-component-analysis-index', DRIFT_DIR + 'analysis-index.json', jsonBytes(analysisIndex))
addArtifact('drift', 'component-drift', 'project-component-inventory:fixture-manifest', DRIFT_DIR + 'project-inventory-fixture-manifest.json', jsonBytes(projectInventory))
addArtifact('drift', 'component-drift', 'component-mapping-snapshot', DRIFT_DIR + 'mapping-snapshot.json', jsonBytes(emptyMappingRegistry(designInventory.scopeId)))
addArtifact('drift', 'component-drift', 'component-comparison', DRIFT_DIR + 'comparison.json', jsonBytes(report))
addArtifact('drift', 'component-drift', 'component-mapping-suggestions', DRIFT_DIR + 'suggestions.json', jsonBytes(suggestions))
addArtifact('drift', 'component-drift', 'component-task-suggestions', DRIFT_DIR + 'task-suggestions.json', jsonBytes(taskSuggestions))
const createdAt = '2026-07-20T12:00:00.000Z'
const manifest = {
  schemaVersion: 2,
  generationId,
  accountFingerprint: 'sha256:' + 'a'.repeat(64),
  fileKeyFingerprint: 'sha256:' + 'b'.repeat(64),
  createdAt,
  syncJobId: 'fsj-' + '2'.repeat(32),
  updatedDomains: ['component-drift', 'components'],
  syncGroups: {
    components: { status: 'completed', updated: 1, unchanged: 0, warnings: 0 },
    drift: { status: 'completed', updated: 6, unchanged: 0, warnings: 0 }
  },
  groups: ['components', 'drift'],
  domains: [
    { id: 'component-drift', group: 'drift', inputFingerprint: 'sha256:' + 'c'.repeat(64), syncedAt: createdAt, sourceGenerationId: generationId },
    { id: 'components', group: 'components', inputFingerprint: 'sha256:' + 'd'.repeat(64), syncedAt: createdAt, sourceGenerationId: generationId }
  ],
  artifacts: artifactDefs,
  counters: { updated: 7, unchanged: 0, warnings: 0 }
}
assert.equal(generation.validateManifest(manifest, generationId), true, 'fixture manifest must validate')
const manifestBytes = jsonBytes(manifest)
writeFileSync(join(orchestrator, 'figma', 'manifests', 'generations', generationId + '.json'), manifestBytes)
writeFileSync(join(orchestrator, 'figma', 'manifests', 'current-generation.json'), jsonBytes({
  schemaVersion: 2, generationId, manifestHash: sha(manifestBytes), committedAt: '2026-07-20T12:00:01.000Z'
}))
const active = generation.current()
assert.equal(active.ok, true, 'fixture generation must resolve: ' + (active.error || ''))
assert.equal(active.mode, 'generation')

let checks = 0
async function check(name, fn) { await fn(); checks++; console.log(`ok ${checks} - ${name}`) }

const upsert = (overrides) => Object.assign({
  op: 'upsert-mapping',
  designComponentId: buttonSet.designComponentId,
  implementations: [{ adapterId: 'fixture-manifest', relation: 'direct', projectComponentIds: ['fixture-manifest:symbol:ui/button'], required: true }],
  propertyMappings: [
    { designPropertyId: 'p:size', adapterId: 'fixture-manifest', projectPropertyId: 'param:size', valueMap: { Small: 'Small', Large: 'Large' } },
    { designPropertyId: 'p:enabled', adapterId: 'fixture-manifest', projectPropertyId: 'param:enabled', valueMap: { true: 'true', false: 'false' } },
  ],
}, overrides || {})
let operationCounter = 0
const nextOperationId = () => 'mop-' + (++operationCounter).toString(16).padStart(32, '0')
const designGenerationRevision = active.pointer.manifestHash
const projectInventoryRevision = artifactDefs.find((row) => row.role === 'project-component-analysis-index').hash
const mutationRequest = (request) => Object.assign({
  operationId: nextOperationId(),
  expectedMappingRevision: 0,
  expectedDesignGenerationRevision: designGenerationRevision,
  expectedProjectInventoryRevision: projectInventoryRevision,
  expectedComparisonSemanticHash: report.semanticHash,
  operations: [upsert()]
}, request || {})
const mutate = (request) => componentMappings.mutate(Object.assign({
}, mutationRequest(request)))
const writersDir = join(cache, 'finalizations', '.writers')
const leaseTail = () => { try { return readdirSync(writersDir).filter((name) => !name.startsWith('.')) } catch { return [] } }

try {
  await check('CMP-MAP-CAS: an absent registry reads as revision 0 and is not "present"', async () => {
    const listed = await componentMappings.get()
    assert.equal(listed.ok, true, JSON.stringify(listed).slice(0, 300))
    assert.equal(listed.present, false)
    assert.equal(listed.revision, 0)
    assert.equal(componentState.readComponentSignals().mappingState, 'absent')
  })

  await check('CMP-MAP-CAS: exact request shape is enforced', async () => {
    const bad = await componentMappings.mutate({ expectedRevision: 0, operations: [upsert()] })
    assert.equal(bad.ok, false)
    assert.equal(bad.status, 400)
    assert.equal(bad.error, 'bad-component-mapping-request')
  })

  await check('CMP-MAP-CAS: stale design generation and project inventory revisions are atomic conflicts', async () => {
    const designStale = await mutate({ expectedDesignGenerationRevision: 'sha256:' + '8'.repeat(64) })
    assert.equal(designStale.error, 'COMPONENT_DESIGN_GENERATION_CONFLICT')
    const projectStale = await mutate({ expectedProjectInventoryRevision: 'sha256:' + '7'.repeat(64) })
    assert.equal(projectStale.error, 'COMPONENT_PROJECT_INVENTORY_CONFLICT')
    assert.equal(componentState.readComponentSignals().mappingState, 'absent')
  })

  await check('CMP-MAP-CAS: a stale comparison hash blocks confirmation (COMPONENT_FINDING_STALE)', async () => {
    const stale = await mutate({ expectedComparisonSemanticHash: 'sha256:' + '9'.repeat(64) })
    assert.equal(stale.ok, false)
    assert.equal(stale.error, 'COMPONENT_FINDING_STALE', JSON.stringify(stale))
  })

  await check('CMP-MAP: server rejects a target absent from the published analysis', async () => {
    const missing = await mutate({ operations: [upsert({ implementations: [{ adapterId: 'fixture-manifest', relation: 'direct', projectComponentIds: ['fixture-manifest:symbol:ui/nope'], required: true }] })] })
    assert.equal(missing.ok, false)
    assert.equal(missing.error, 'COMPONENT_MAPPING_TARGET_MISSING')
  })

  await check('CMP-MAP: a property binding citing an undeclared project property is refused', async () => {
    const missing = await mutate({
      operations: [upsert({
        propertyMappings: [{ designPropertyId: 'p:size', adapterId: 'fixture-manifest', projectPropertyId: 'param:ghost' }],
      })]
    })
    assert.equal(missing.ok, false)
    assert.equal(missing.error, 'COMPONENT_MAPPING_TARGET_MISSING')
  })

  await check('CMP-MAP-IDENTITY: external mappings cannot invent a zero scope or adapter-name platform', async () => {
    const missingIdentity = await mutate({
      operations: [upsert({
        implementations: [{ adapterId: 'external-library', relation: 'external', projectComponentIds: [], externalRef: 'vendor/Button', required: true }],
        propertyMappings: []
      })]
    })
    assert.equal(missingIdentity.error, 'COMPONENT_MAPPING_INVALID')
    assert.match(missingIdentity.detail, /externalPlatform and externalScopeFingerprint/)
    assert.equal(componentState.readComponentSignals().mappingState, 'absent')
  })

  await check('CMP-MAP-CAS: a valid confirmation writes revision 1 with server-resolved platform + scope fingerprint', async () => {
    const request = mutationRequest({})
    const confirmed = await componentMappings.mutate(request)
    assert.equal(confirmed.ok, true, JSON.stringify(confirmed).slice(0, 400))
    assert.equal(confirmed.revision, 1)
    const written = JSON.parse(readFileSync(componentMappings.MAPPING_FILE, 'utf8'))
    assert.equal(written.revision, 1)
    assert.equal(written.designScopeId, designInventory.scopeId)
    assert.equal(written.mappings.length, 1)
    assert.equal(written.mappings[0].designComponentId, buttonSet.designComponentId)
    assert.equal(written.mappings[0].implementations[0].platform, 'web', 'platform is server-resolved from the analysis, never client input')
    assert.equal(written.mappings[0].implementations[0].projectScopeFingerprint, projectInventory.scopeFingerprint)
    assert.equal(written.mappings[0].provenance.kind, 'user-confirmed')
    assert.equal(written.operationReceipts.length, 1)
    assert.equal(componentState.readComponentSignals().mappingRevision, 1)
    const pointerBytes = readFileSync(generation.POINTER_FILE)
    writeFileSync(generation.POINTER_FILE, '{ temporarily unavailable')
    try {
      const replay = await componentMappings.mutate(request)
      assert.equal(replay.ok, true)
      assert.equal(replay.replayed, true)
      assert.equal(replay.requestHash, confirmed.requestHash)
    } finally {
      writeFileSync(generation.POINTER_FILE, pointerBytes)
    }
    assert.equal(JSON.parse(readFileSync(componentMappings.MAPPING_FILE, 'utf8')).revision, 1)
  })

  await check('CMP-MAP-IDEMPOTENCY: reusing an operation id for different bytes is a typed no-write conflict', async () => {
    const request = mutationRequest({ expectedMappingRevision: 1 })
    request.operationId = JSON.parse(readFileSync(componentMappings.MAPPING_FILE, 'utf8')).operationReceipts[0].operationId
    const conflict = await componentMappings.mutate(request)
    assert.equal(conflict.error, 'COMPONENT_MAPPING_OPERATION_CONFLICT')
    assert.equal(JSON.parse(readFileSync(componentMappings.MAPPING_FILE, 'utf8')).revision, 1)
  })

  await check('CMP-MAP: set-render-class stamps the owner policy; null clears to the tombstone', async () => {
    const listed = await componentMappings.get()
    const mappingId = listed.mappings[0].mappingId
    const stamped = await mutate({
      expectedMappingRevision: 1,
      expectedComparisonSemanticHash: report.semanticHash,
      operations: [{ op: 'set-render-class', mappingId, renderClass: 'canvas', reason: 'flat canvas render' }]
    })
    assert.equal(stamped.ok, true, JSON.stringify(stamped).slice(0, 300))
    let written = JSON.parse(readFileSync(componentMappings.MAPPING_FILE, 'utf8'))
    assert.equal(written.mappings[0].visualPolicy.renderClass, 'canvas')
    assert.equal(written.mappings[0].visualPolicy.by, 'owner')
    const cleared = await mutate({
      expectedMappingRevision: 2,
      expectedComparisonSemanticHash: report.semanticHash,
      operations: [{ op: 'set-render-class', mappingId, renderClass: null }]
    })
    assert.equal(cleared.ok, true)
    written = JSON.parse(readFileSync(componentMappings.MAPPING_FILE, 'utf8'))
    assert.equal(written.mappings[0].visualPolicy.renderClass, null, 'clearing keeps the owner tombstone, not field deletion')
    assert.equal(written.revision, 3)
  })

  await check('CMP-MAP-TOMBSTONE: retirement stamps reason, actor, and lastSeenDisplayName', async () => {
    const listed = await componentMappings.get()
    const mappingId = listed.mappings[0].mappingId
    const retired = await mutate({
      expectedMappingRevision: 3,
      expectedComparisonSemanticHash: report.semanticHash,
      operations: [{ op: 'retire-mapping', mappingId, reason: 'design component retired upstream' }]
    })
    assert.equal(retired.ok, true, JSON.stringify(retired).slice(0, 300))
    const written = JSON.parse(readFileSync(componentMappings.MAPPING_FILE, 'utf8'))
    assert.equal(written.mappings[0].state, 'retired')
    assert.equal(written.mappings[0].retirement.lastSeenDisplayName, 'AppButton')
    assert.equal(written.revision, 4)
  })

  await check('CMP-MAP-DISP: design-side disposition validates the id and round-trips through removal', async () => {
    const added = await mutate({
      expectedMappingRevision: 4,
      expectedComparisonSemanticHash: report.semanticHash,
      operations: [{ op: 'add-disposition', side: 'design', designComponentId: buttonSet.designComponentId, kind: 'intentionally-design-only', reason: 'design-side only for now' }]
    })
    assert.equal(added.ok, true, JSON.stringify(added).slice(0, 300))
    const written = JSON.parse(readFileSync(componentMappings.MAPPING_FILE, 'utf8'))
    assert.equal(written.dispositions.length, 1)
    const removed = await mutate({
      expectedMappingRevision: 5,
      expectedComparisonSemanticHash: report.semanticHash,
      operations: [{ op: 'remove-disposition', dispositionId: written.dispositions[0].dispositionId }]
    })
    assert.equal(removed.ok, true)
    const unknown = await mutate({
      expectedMappingRevision: 6,
      expectedComparisonSemanticHash: report.semanticHash,
      operations: [{ op: 'add-disposition', side: 'design', designComponentId: 'figma-component:' + 'a'.repeat(16) + ':none:99:9', kind: 'ignored', reason: 'x' }]
    })
    assert.equal(unknown.ok, false)
    assert.equal(unknown.error, 'COMPONENT_FINDING_STALE')
  })

  await check('CMP-MAP-CORRUPT: a malformed registry blocks reads and writes, never reads as empty', async () => {
    const before = readFileSync(componentMappings.MAPPING_FILE)
    writeFileSync(componentMappings.MAPPING_FILE, '{ broken')
    try {
      assert.equal(componentState.readComponentSignals().mappingState, 'invalid')
      const listed = await componentMappings.get()
      assert.equal(listed.ok, false)
      assert.equal(listed.error, 'COMPONENT_MAPPING_INVALID')
      const write = await mutate({ expectedMappingRevision: 6 })
      assert.equal(write.ok, false)
      assert.equal(write.error, 'COMPONENT_MAPPING_INVALID')
    } finally {
      writeFileSync(componentMappings.MAPPING_FILE, before)
    }
  })

  await check('CMP-MAP-SCOPE: a foreign-scope registry blocks authoring until an explicit onboard-fresh', async () => {
    const before = readFileSync(componentMappings.MAPPING_FILE)
    const foreign = JSON.parse(before.toString('utf8'))
    foreign.designScopeId = 'figma:file:' + '9'.repeat(16) + ':branch:none:components:' + '9'.repeat(16)
    foreign.mappings = []
    foreign.dispositions = []
    writeFileSync(componentMappings.MAPPING_FILE, JSON.stringify(foreign, null, 2) + '\n')
    try {
      const blocked = await mutate({ expectedMappingRevision: 6 })
      assert.equal(blocked.ok, false)
      assert.equal(blocked.error, 'COMPONENT_DESIGN_SCOPE_CHANGED')
      const onboarded = await mutate({
        expectedMappingRevision: 6,
        expectedComparisonSemanticHash: null,
        operations: [{ op: 'onboard-fresh' }]
      })
      assert.equal(onboarded.ok, true, JSON.stringify(onboarded).slice(0, 300))
      const written = JSON.parse(readFileSync(componentMappings.MAPPING_FILE, 'utf8'))
      assert.equal(written.designScopeId, designInventory.scopeId)
      assert.equal(written.revision, 7)
      assert.deepEqual(written.mappings, [])
    } finally {
      writeFileSync(componentMappings.MAPPING_FILE, before)
    }
  })

  await check('CMP-MAP-GENERATION: corrupt immutable design bytes require resync, never empty state', async () => {
    const entry = artifactDefs.find((row) => row.role === 'design-component-inventory')
    const file = join(root, entry.path)
    const original = readFileSync(file)
    writeFileSync(file, '{corrupt-component-inventory')
    try {
      const listed = await componentMappings.get()
      assert.equal(listed.ok, false)
      assert.equal(listed.error, 'COMPONENT_GENERATION_RESYNC_REQUIRED')
      const write = await mutate({ expectedMappingRevision: 6 })
      assert.equal(write.ok, false)
      assert.equal(write.error, 'COMPONENT_GENERATION_RESYNC_REQUIRED')
    } finally {
      writeFileSync(file, original)
    }
  })

  // ── §33.4-12: restart proof ───────────────────────────────────────────────
  await check('RESTART: completed mutations leave no writer-lease tail', async () => {
    assert.deepEqual(leaseTail(), [], 'every CAS mutation must release its component-mappings writer lease')
  })

  await check('RESTART: startupReconcile over an unchanged workspace keeps the comparison (not dirty)', async () => {
    componentState.clearProjectDirty()
    const result = await componentCompare.init()
    assert.equal(result.ok, true, JSON.stringify(result))
    assert.equal(result.dirty, false, 'unchanged inputs must not invalidate the published comparison')
    assert.equal(componentState.readComponentSignals().projectDirty, false)
    // The published comparison is still served from the same durable generation.
    const again = generation.current()
    assert.equal(again.ok, true)
    assert.equal(again.manifest.artifacts.some((row) => row.role === 'component-comparison'), true)
  })

  await check('WATCHER: an existing nested-root edit latches project-dirty on every platform', async () => {
    const originalWatch = fsModule.watch
    fsModule.watch = function () {
      return { close() {} }
    }
    try {
      await componentCompare.init()
      componentState.clearProjectDirty()
      writeFileSync(join(root, 'design', 'components', 'nested', 'late.json'), '{"changed":true}\n')
      for (let i = 0; i < 40 && !componentState.readComponentSignals().projectDirty; i++) {
        await new Promise((resolve) => setTimeout(resolve, 100))
      }
      assert.equal(componentState.readComponentSignals().projectDirty, true,
        'the bounded polling proof must catch a nested edit even when fs.watch drops every event')
      assert.equal(componentState.readComponentSignals().projectDirtyReason, 'watcher')
    } finally {
      fsModule.watch = originalWatch
    }
  })

  await check('DOMAIN IDENTITY: a component-only adapter config edit cannot invalidate token drift', async () => {
    const configFile = join(orchestrator, 'figma', 'project-adapters.json')
    const beforeBytes = readFileSync(configFile)
    const tokenBefore = figmaSync.driftDomainInputs('tokens')
    const componentBefore = figmaSync.driftDomainInputs('components')
    const edited = JSON.parse(beforeBytes.toString('utf8'))
    edited.adapters[0].components.visibility = ['public', 'internal']
    writeFileSync(configFile, JSON.stringify(edited, null, 2) + '\n')
    try {
      const tokenAfter = figmaSync.driftDomainInputs('tokens')
      const componentAfter = figmaSync.driftDomainInputs('components')
      assert.equal(tokenAfter.fingerprint, tokenBefore.fingerprint)
      assert.notEqual(componentAfter.fingerprint, componentBefore.fingerprint)
      assert.equal(componentState.readComponentSignals().configFileHash,
        loadAdapterConfig({ projectRoot: root }).componentConfigFileHash)
    } finally {
      writeFileSync(configFile, beforeBytes)
    }
  })

  await check('RESTART: an offline source edit latches project-dirty on the next startup', async () => {
    const tokenBefore = figmaSync.driftDomainInputs('tokens')
    const componentBefore = figmaSync.driftDomainInputs('components')
    assert.equal(tokenBefore.ok, true)
    assert.equal(componentBefore.ok, true)
    const manifestPath = join(root, 'design', 'components', 'manifest.json')
    const edited = JSON.parse(readFileSync(manifestPath, 'utf8'))
    edited.components[0].props.push({ name: 'tone', kind: 'enum', values: ['A', 'B'] })
    writeFileSync(manifestPath, JSON.stringify(edited, null, 2))
    const tokenAfter = figmaSync.driftDomainInputs('tokens')
    const componentAfter = figmaSync.driftDomainInputs('components')
    assert.equal(tokenAfter.fingerprint, tokenBefore.fingerprint,
      'component sources/registry must never participate in the token comparison fingerprint')
    assert.notEqual(componentAfter.fingerprint, componentBefore.fingerprint)
    const result = await componentCompare.init()
    assert.equal(result.ok, true, JSON.stringify(result))
    assert.equal(result.dirty, true, 'an offline change must be detected at startup')
    assert.equal(componentState.readComponentSignals().projectDirty, true)
    assert.deepEqual(leaseTail(), [], 'reconcile is read-only — no writer lease may remain')
  })

  console.log(`design-component-mappings: ${checks} checks passed`)
} finally {
  rmSync(root, { recursive: true, force: true })
}
// init() arms fs watchers over the fixture adapter roots; exit explicitly.
process.exit(0)
