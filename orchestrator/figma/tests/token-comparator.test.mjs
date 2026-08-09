import assert from 'node:assert/strict'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { validateAdapterConfig } from '../runtime/adapter-config.mjs'
import { canonicalHash, canonicalJson } from '../runtime/canonical-json.mjs'
import { createSchemaRegistry } from '../runtime/schema-registry.mjs'
import { publishableBaseline } from '../tokens/baseline.mjs'
import { bindObservedTokens } from '../tokens/binder.mjs'
import { aggregateObservedTokens } from '../tokens/catalog-aggregator.mjs'
import { compareTokens } from '../tokens/comparator.mjs'
import { emptyMappingRegistry } from '../tokens/mapping-contract.mjs'
import { normalizeSourceCapture } from '../tokens/source-normalizer.mjs'
import {
  FILE_FINGERPRINT,
  immutablePlan,
  validObservedCapture
} from './observed-token-fixtures.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const schemas = createSchemaRegistry(join(HERE, '..', 'schemas'))
const scope = { fileKeyFingerprint: FILE_FINGERPRINT, branchKey: 'none' }
const hash = (char) => 'sha256:' + char.repeat(64)
let passed = 0
function check(name, fn) {
  try {
    fn()
    passed++
    console.log(`PASS ${name}`)
  } catch (error) {
    console.error(`FAIL ${name}\n  ${error.stack || error.message}`)
    process.exitCode = 1
  }
}
function adapterConfig() {
  const root = mkdtempSync(join(tmpdir(), 'token-comparator-'))
  try {
    return validateAdapterConfig({
      schemaVersion: 2,
      adapters: [{
        id: 'compose-design-system',
        kind: 'kotlin-compose',
        version: 2,
        enabled: true,
        capabilities: ['tokens'],
        platform: 'android',
        authority: 'handwritten',
        tokens: {
          roots: ['design-system/src'],
          include: ['**/*.kt'],
          exclude: [],
          modes: ['light'],
          authorities: { color: { contracts: ['com.example.AppColor'] } },
          contextMap: [{ when: { theme: 'light', platform: 'shared' }, projectMode: 'light' }],
          bindingRules: [{
            ruleId: 'color-prefix',
            kind: 'prefix-map',
            tokenKind: 'color',
            providerPrefix: ['color'],
            projectPrefix: ['AppColor'],
            caseTransform: 'preserve',
            excludeExact: [],
            excludePrefix: []
          }]
        }
      }]
    }, { projectRoot: root, schemaValidate: schemas.validate('project-adapters') })
  } finally {
    rmSync(root, { recursive: true, force: true })
  }
}
function projectInventory(color = '#336699FF') {
  return {
    schemaVersion: 2,
    adapterId: 'compose-design-system',
    adapterKind: 'kotlin-compose',
    adapterVersion: 2,
    scopeFingerprint: hash('1'),
    configHash: hash('2'),
    modes: ['light'],
    tokens: [{
      projectTokenId: 'compose-design-system:AppColor.content.primary',
      kind: 'color',
      layer: 'semantic-contract',
      semanticPath: ['AppColor', 'content', 'primary'],
      displayName: 'AppColor.content.primary',
      modes: {
        light: {
          raw: { expression: color },
          resolved: { kind: 'color', value: color, colorSpace: 'srgb' }
        }
      },
      source: {
        path: 'design-system/src/AppColor.kt',
        line: 1,
        column: 0,
        symbol: 'AppColor.content.primary',
        fileHash: hash('3')
      },
      edges: [],
      limitations: []
    }, {
      projectTokenId: 'compose-design-system:AppColor.projectOnly',
      kind: 'color',
      layer: 'semantic-contract',
      semanticPath: ['AppColor', 'projectOnly'],
      displayName: 'AppColor.projectOnly',
      modes: {
        light: {
          raw: { expression: '#000000FF' },
          resolved: { kind: 'color', value: '#000000FF', colorSpace: 'srgb' }
        }
      },
      source: {
        path: 'design-system/src/AppColor.kt',
        line: 2,
        column: 0,
        symbol: 'AppColor.projectOnly',
        fileHash: hash('3')
      },
      edges: [],
      limitations: []
    }],
    witness: {
      rootsConfigured: 1,
      rootsResolved: 1,
      rootsMissing: [],
      filesMatched: 1,
      filesParsed: 1,
      parseFailures: [],
      limitsHit: [],
      complete: true
    },
    counts: { tokens: 2, unsupportedModes: 0, aliasEdges: 0 }
  }
}
function inputs(color) {
  const capture = validObservedCapture()
  const batch = normalizeSourceCapture(capture, Buffer.from(JSON.stringify(capture)), immutablePlan(capture))
  const observed = aggregateObservedTokens({ scope, batches: [batch], revision: 1 })
  const adapter = adapterConfig()
  const project = projectInventory(color)
  const mapping = emptyMappingRegistry(scope)
  const binding = bindObservedTokens({
    catalog: observed.catalog,
    projectInventories: [project],
    adapterConfig: adapter,
    mappingRegistry: mapping,
    projectAnalysisHash: hash('4')
  })
  const analysisIndex = {
    schemaVersion: 2,
    configHash: adapter.tokenConfigHash,
    adapters: [{
      adapterId: project.adapterId,
      role: 'project-token-inventory:' + project.adapterId,
      inventoryHash: canonicalHash(project),
      scopeFingerprint: project.scopeFingerprint,
      complete: true
    }],
    complete: true
  }
  return { observed, adapter, project, mapping, binding, analysisIndex }
}
function compare(color, baseline = null, sourceFreshness = 'current') {
  const data = inputs(color)
  const outcome = compareTokens({
    observedCatalog: data.observed.catalog,
    sourceIndex: data.observed.index,
    projectInventories: [data.project],
    analysisIndex: data.analysisIndex,
    bindingSnapshot: data.binding,
    mappingRegistry: data.mapping,
    baseline,
    context: {
      analysisIndexHash: canonicalHash(data.analysisIndex),
      adapterConfigHash: data.adapter.tokenConfigHash,
      baselineHash: baseline ? baseline.semanticHash : 'none',
      sourceFreshness
    }
  })
  return { ...data, ...outcome }
}

check('token comparator emits independent lifecycle/source/binding/value axes', () => {
  const result = compare('#336699FF')
  assert.equal(schemas.validate('token-comparison')(result.report), true, JSON.stringify(schemas.validate('token-comparison').errors))
  const color = result.report.observedRows.find((row) => row.providerName === 'color/content/primary')
  assert.equal(color.lifecycleStatus, 'newly-observed')
  assert.equal(color.sourceStatus, 'consistent')
  assert.equal(color.bindingStatus, 'auto-bound')
  assert.equal(color.valueStatus, 'matched')
  assert.equal(result.report.coverage.label, 'Observed token coverage')
})

check('project-only means unbound to current observations, not absent in Figma', () => {
  const result = compare('#336699FF')
  assert.equal(result.report.projectOnly.some((row) => row.projectTokenId.endsWith('projectOnly')), true)
  assert.equal(canonicalJson(result.report).includes('missing-in-design'), false)
})

check('not-observed lifecycle is explicit and never claims file-wide deletion', () => {
  const initial = inputs('#336699FF')
  const emptyCapture = validObservedCapture({ captureSequence: 2, observations: [] })
  const emptyBatch = normalizeSourceCapture(
    emptyCapture,
    Buffer.from(JSON.stringify(emptyCapture)),
    immutablePlan(emptyCapture)
  )
  const observed = aggregateObservedTokens({
    scope,
    batches: [emptyBatch],
    revision: 2,
    previousCatalog: initial.observed.catalog,
    previousIndex: initial.observed.index
  })
  const binding = bindObservedTokens({
    catalog: observed.catalog,
    projectInventories: [initial.project],
    adapterConfig: initial.adapter,
    mappingRegistry: initial.mapping,
    projectAnalysisHash: hash('4')
  })
  const outcome = compareTokens({
    observedCatalog: observed.catalog,
    sourceIndex: observed.index,
    projectInventories: [initial.project],
    analysisIndex: initial.analysisIndex,
    bindingSnapshot: binding,
    mappingRegistry: initial.mapping,
    baseline: null,
    context: {
      analysisIndexHash: canonicalHash(initial.analysisIndex),
      adapterConfigHash: initial.adapter.tokenConfigHash,
      baselineHash: 'none',
      sourceFreshness: 'current'
    }
  })
  assert.ok(outcome.report.observedRows.length > 0)
  assert.ok(outcome.report.observedRows.every((row) => row.lifecycleStatus === 'not-observed'))
  assert.ok(outcome.report.observedRows.every((row) =>
    row.limitations.includes('not-observed-is-not-file-wide-deletion')))
  assert.equal(canonicalJson(outcome.report).includes('missing-in-design'), false)
})

check('value drift is exact and preserves the project/design values', () => {
  const result = compare('#FFFFFFFF')
  const color = result.report.observedRows.find((row) => row.providerName === 'color/content/primary')
  assert.equal(color.valueStatus, 'value-drift')
  assert.equal(color.designValue.hex, '#336699FF')
  assert.equal(color.projectValue.hex, '#FFFFFFFF')
})

check('eligible baseline publishes only comparable coordinates', () => {
  const result = compare('#336699FF')
  const baseline = publishableBaseline(result.baselineCandidate, result.report, '2026-07-23T10:00:00.000Z')
  assert.equal(schemas.validate('token-baseline')(baseline), true, JSON.stringify(schemas.validate('token-baseline').errors))
  assert.equal(baseline.entries.length, 1)
})

check('unknown Source Health keeps comparison readable but blocks baseline authority', () => {
  const result = compare('#336699FF', null, 'unknown')
  assert.equal(result.report.operationalState, 'stale')
  assert.equal(result.report.complete, false)
  assert.equal(result.report.inputs.sourceFreshness, 'unknown')
  assert.ok(result.report.blockers.some((row) => row.code === 'TOKEN_SOURCE_HEALTH_UNAVAILABLE'))
  assert.equal(publishableBaseline(
    result.baselineCandidate,
    result.report,
    '2026-07-23T10:00:00.000Z'
  ), null)
})

check('baseline attributes project-only change direction without guessing', () => {
  const initial = compare('#336699FF')
  const baseline = publishableBaseline(initial.baselineCandidate, initial.report, '2026-07-23T10:00:00.000Z')
  const changed = compare('#FFFFFFFF', baseline)
  const color = changed.report.observedRows.find((row) => row.providerName === 'color/content/primary')
  assert.equal(color.direction, 'project-changed')
})

check('same inputs produce byte-stable semantic output', () => {
  assert.equal(canonicalJson(compare('#336699FF').report), canonicalJson(compare('#336699FF').report))
})

if (!process.exitCode) console.log(`token comparator: ${passed} checks passed`)
