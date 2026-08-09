import assert from 'node:assert/strict'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { validateAdapterConfig } from '../runtime/adapter-config.mjs'
import { createSchemaRegistry } from '../runtime/schema-registry.mjs'
import { aggregateObservedTokens } from '../tokens/catalog-aggregator.mjs'
import { bindObservedTokens, bindingSnapshotSemanticError } from '../tokens/binder.mjs'
import { emptyMappingRegistry } from '../tokens/mapping-contract.mjs'
import { normalizeSourceCapture } from '../tokens/source-normalizer.mjs'
import { sourceIdFor } from '../tokens/source-contract.mjs'
import {
  FILE_FINGERPRINT,
  immutablePlan,
  validObservedCapture
} from './observed-token-fixtures.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const schemas = createSchemaRegistry(join(HERE, '..', 'schemas'))
const hash = (char) => 'sha256:' + char.repeat(64)
const scope = { fileKeyFingerprint: FILE_FINGERPRINT, branchKey: 'none' }
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
function adapterDocument() {
  return {
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
        authorities: { color: { contracts: ['com.example.design.AppColor'] } },
        contextMap: [{ when: { theme: 'light', platform: 'shared' }, projectMode: 'light' }],
        bindingRules: [{
          ruleId: 'colors',
          kind: 'prefix-map',
          tokenKind: 'color',
          providerPrefix: ['color'],
          projectPrefix: ['AppColor'],
          caseTransform: 'camel',
          excludeExact: [],
          excludePrefix: []
        }]
      }
    }]
  }
}
function adapterConfig() {
  const root = mkdtempSync(join(tmpdir(), 'binder-adapter-'))
  try {
    return validateAdapterConfig(adapterDocument(), {
      projectRoot: root,
      schemaValidate: schemas.validate('project-adapters')
    })
  } finally {
    rmSync(root, { recursive: true, force: true })
  }
}
function catalog() {
  const capture = validObservedCapture()
  const batch = normalizeSourceCapture(capture, Buffer.from(JSON.stringify(capture)), immutablePlan(capture))
  return aggregateObservedTokens({ scope, batches: [batch], revision: 1 }).catalog
}
function inventory(tokens = [{
  projectTokenId: 'compose-design-system:AppColor.content.primary',
  kind: 'color',
  semanticPath: ['AppColor', 'content', 'primary']
}]) {
  return {
    adapterId: 'compose-design-system',
    tokens
  }
}
function bind(options = {}) {
  return bindObservedTokens({
    catalog: options.catalog || catalog(),
    projectInventories: options.projectInventories || [inventory()],
    adapterConfig: options.adapterConfig || adapterConfig(),
    mappingRegistry: options.mappingRegistry || emptyMappingRegistry(scope),
    taskBindings: options.taskBindings || [],
    projectAnalysisHash: hash('d')
  })
}

check('configured rule produces one type-safe context-mapped binding', () => {
  const snapshot = bind()
  assert.equal(schemas.validate('token-binding-snapshot')(snapshot), true, JSON.stringify(schemas.validate('token-binding-snapshot').errors))
  assert.equal(bindingSnapshotSemanticError(snapshot), null)
  assert.equal(snapshot.bindings.length, 1)
  assert.equal(snapshot.bindings[0].authority, 'rule')
  assert.equal(snapshot.bindings[0].projectMode, 'light')
  assert.equal(snapshot.bindings[0].targetState, 'present')
})

check('semantic validator rejects a tampered snapshot hash', () => {
  const snapshot = bind()
  snapshot.semanticHash = hash('f')
  assert.equal(bindingSnapshotSemanticError(snapshot), 'binding snapshot semanticHash mismatch')
})

check('deterministic target address remains authoritative when project entity is absent', () => {
  const snapshot = bind({ projectInventories: [inventory([])] })
  assert.equal(snapshot.bindings.length, 1)
  assert.equal(snapshot.bindings[0].targetState, 'expected-missing')
  assert.equal(Object.hasOwn(snapshot.bindings[0], 'projectTokenId'), false)
  assert.deepEqual(snapshot.bindings[0].projectSemanticPath, ['AppColor', 'content', 'primary'])
})

check('two entities at one exact target address are ambiguous', () => {
  const snapshot = bind({
    projectInventories: [inventory([
      {
        projectTokenId: 'compose-design-system:AppColor.content.primary.a',
        kind: 'color',
        semanticPath: ['AppColor', 'content', 'primary']
      },
      {
        projectTokenId: 'compose-design-system:AppColor.content.primary.b',
        kind: 'color',
        semanticPath: ['AppColor', 'content', 'primary']
      }
    ])]
  })
  assert.equal(snapshot.bindings.length, 0)
  assert.equal(snapshot.conflicts[0].code, 'TOKEN_BINDING_AMBIGUOUS')
})

check('manual mapping and configured rule disagreement is an authority conflict', () => {
  const observed = catalog()
  const color = observed.tokens.find((row) => row.providerName === 'color/content/primary')
  const registry = emptyMappingRegistry(scope)
  registry.revision = 1
  registry.mappings.push({
    mappingId: 'map-' + '1'.repeat(24),
    observedTokenKey: color.observedTokenKey,
    contextSelector: { theme: 'light', platform: 'shared' },
    adapterId: 'compose-design-system',
    projectTokenIds: ['compose-design-system:AppColor.other'],
    expectedKind: 'color',
    relation: 'one-to-one',
    state: 'active',
    provenance: {
      kind: 'manual-review',
      actor: 'owner',
      at: '2026-07-23T10:00:00.000Z'
    }
  })
  const snapshot = bind({
    catalog: observed,
    mappingRegistry: registry,
    projectInventories: [inventory([{
      projectTokenId: 'compose-design-system:AppColor.other',
      kind: 'color',
      semanticPath: ['AppColor', 'other']
    }, {
      projectTokenId: 'compose-design-system:AppColor.content.primary',
      kind: 'color',
      semanticPath: ['AppColor', 'content', 'primary']
    }])]
  })
  assert.equal(snapshot.conflicts.some((row) => row.code === 'TOKEN_BINDING_AUTHORITY_CONFLICT'), true)
})

check('value equality alone never creates a binding', () => {
  const snapshot = bind({
    projectInventories: [inventory([{
      projectTokenId: 'compose-design-system:SameValueButDifferentMeaning',
      kind: 'color',
      semanticPath: ['SameValueButDifferentMeaning'],
      modes: { light: { resolved: { kind: 'color', space: 'srgb', hex: '#336699FF' } } }
    }])]
  })
  assert.equal(snapshot.bindings[0].targetState, 'expected-missing')
  assert.equal(snapshot.suggestions.length, 0)
})

check('source-conflicting coordinates cannot bind', () => {
  const first = validObservedCapture({
    observations: [{ providerName: 'color/content/primary', rawValue: '#336699', providerType: 'COLOR' }]
  })
  const secondSource = {
    ...first.source,
    nodeId: '10:99',
    origin: {
      kind: 'task-screen',
      taskStem: 'TASK_99_Conflict',
      screenKey: 'Conflict',
      variantId: 'light-default-shared'
    }
  }
  secondSource.sourceId = sourceIdFor(secondSource)
  const second = validObservedCapture({
    source: secondSource,
    captureSequence: 2,
    observations: [{ providerName: 'color/content/primary', rawValue: '#FFFFFF', providerType: 'COLOR' }]
  })
  const batches = [first, second].map((capture) =>
    normalizeSourceCapture(capture, Buffer.from(JSON.stringify(capture)), immutablePlan(capture)))
  const conflictingCatalog = aggregateObservedTokens({ scope, batches, revision: 2 }).catalog
  const snapshot = bind({ catalog: conflictingCatalog })
  assert.equal(snapshot.bindings.length, 0)
  assert.equal(snapshot.conflicts[0].code, 'TOKEN_SOURCE_CONFLICT')
})

if (!process.exitCode) console.log(`token binder: ${passed} checks passed`)
