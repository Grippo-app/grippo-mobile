// component-mapping.test.mjs — pins for the component mapping registry
// contract (CMP-MAP-*) and the frozen component task-binding contract
// (CMP-BIND-*): identity uniqueness, relation cardinalities, implementation
// exclusivity, property/slot binding exactness, disposition exactness,
// tombstones, hash/CAS identity, empty-registry authoring, and the runtime
// invariants tasks/component-binding-contract.cjs enforces on frozen bindings.
import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import {
  validComponentCapture, COMPONENT_CAPTURE_HASH, validComponentRegistry,
  validComponentTaskBinding, DESIGN_ID, PROJECT_ID, ADAPTER_ID
} from './component-fixtures.mjs'
import { normalizeCapture } from '../components/capture-normalizer.mjs'
import { mappingRegistrySemanticError, mappingRegistryHash, emptyMappingRegistry, COMPONENT_MAPPINGS_RELATIVE_PATH } from '../components/mapping-contract.mjs'

const require = createRequire(import.meta.url)
const bindingContract = require('../../tasks/component-binding-contract.cjs')

const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }

const design = normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)
const registryOf = (mutate) => {
  const registry = validComponentRegistry(design.scopeId)
  if (mutate) mutate(registry)
  return registry
}
const AT = '2026-07-20T11:00:00.000Z'

check('CMP-MAP: the fixture registry is semantically valid and hash-stable', () => {
  assert.equal(mappingRegistrySemanticError(registryOf()), null)
  assert.equal(mappingRegistryHash(registryOf()), mappingRegistryHash(registryOf()))
  assert.equal(COMPONENT_MAPPINGS_RELATIVE_PATH, 'orchestrator/figma/component-mappings.json')
})

check('CMP-MAP: revision participates in the registry hash (CAS identity)', () => {
  const bumped = registryOf((registry) => { registry.revision += 1 })
  assert.notEqual(mappingRegistryHash(registryOf()), mappingRegistryHash(bumped))
})

check('CMP-MAP: duplicate mappingId is rejected', () => {
  const registry = registryOf((value) => { value.mappings[1].mappingId = value.mappings[0].mappingId })
  assert.match(mappingRegistrySemanticError(registry), /duplicate mappingId/)
})

check('CMP-MAP: two active mappings for one design component are rejected', () => {
  const registry = registryOf((value) => {
    const clone = JSON.parse(JSON.stringify(value.mappings[0]))
    clone.mappingId = 'cmap-' + '4'.repeat(24)
    clone.implementations[0].relation = 'shared-implementation'
    value.mappings[0].implementations[0].relation = 'shared-implementation'
    value.mappings.push(clone)
  })
  assert.match(mappingRegistrySemanticError(registry), /two active mappings/)
})

check('CMP-MAP-CARD: direct relation requires exactly one project component', () => {
  const registry = registryOf((value) => {
    value.mappings[0].implementations[0].projectComponentIds.push(PROJECT_ID.spare)
  })
  assert.match(mappingRegistrySemanticError(registry), /exactly one project component/)
})

check('CMP-MAP-CARD: composite relation requires at least two project components', () => {
  const registry = registryOf((value) => { value.mappings[0].implementations[0].relation = 'composite' })
  assert.match(mappingRegistrySemanticError(registry), /at least two project components/)
})

check('CMP-MAP-CARD: external relation carries no targets and demands externalRef', () => {
  const withTargets = registryOf((value) => { value.mappings[0].implementations[0].relation = 'external' })
  assert.match(mappingRegistrySemanticError(withTargets), /must carry no project components/)
  const withoutRef = registryOf((value) => {
    value.mappings[0].implementations[0].relation = 'external'
    value.mappings[0].implementations[0].projectComponentIds = []
    value.mappings[0].propertyMappings = []
    value.mappings[0].slotMappings = []
  })
  assert.match(mappingRegistrySemanticError(withoutRef), /requires externalRef/)
  const refOutsideExternal = registryOf((value) => { value.mappings[0].implementations[0].externalRef = 'material3 Button' })
  assert.match(mappingRegistrySemanticError(refOutsideExternal), /externalRef outside an external relation/)
})

check('CMP-MAP: one adapter or platform bound twice inside a mapping is rejected', () => {
  const adapterTwice = registryOf((value) => {
    const clone = JSON.parse(JSON.stringify(value.mappings[0].implementations[0]))
    clone.platform = 'android-tv'
    value.mappings[0].implementations.push(clone)
  })
  assert.match(mappingRegistrySemanticError(adapterTwice), /binds adapter .* twice/)
  const platformTwice = registryOf((value) => {
    const clone = JSON.parse(JSON.stringify(value.mappings[0].implementations[0]))
    clone.adapterId = 'other-ds'
    clone.projectComponentIds = ['other-ds:symbol:com.other.Button']
    value.mappings[0].implementations.push(clone)
  })
  assert.match(mappingRegistrySemanticError(platformTwice), /binds platform .* twice/)
})

check('CMP-MAP-EXCL: two exclusive claims on one project component are rejected', () => {
  const registry = registryOf((value) => {
    value.mappings[1].implementations[0].projectComponentIds = [PROJECT_ID.button]
  })
  assert.match(mappingRegistrySemanticError(registry), /without shared-implementation relations/)
})

check('CMP-MAP-EXCL: shared-implementation on EVERY claimant is the only legal multi-claim', () => {
  const registry = registryOf((value) => {
    value.mappings[0].implementations[0].relation = 'shared-implementation'
    value.mappings[1].implementations[0].relation = 'shared-implementation'
    value.mappings[1].implementations[0].projectComponentIds = [PROJECT_ID.button]
  })
  assert.equal(mappingRegistrySemanticError(registry), null)
  const mixed = registryOf((value) => {
    value.mappings[1].implementations[0].relation = 'shared-implementation'
    value.mappings[1].implementations[0].projectComponentIds = [PROJECT_ID.button]
  })
  assert.match(mappingRegistrySemanticError(mixed), /without shared-implementation relations/)
})

check('CMP-MAP: a target outside its adapter namespace is rejected', () => {
  const registry = registryOf((value) => {
    value.mappings[0].implementations[0].projectComponentIds = ['other-adapter:symbol:com.example.ui.AppButton']
  })
  assert.match(mappingRegistrySemanticError(registry), /outside adapter/)
})

check('CMP-MAP-PROP: one design property mapped twice for one adapter is rejected', () => {
  const registry = registryOf((value) => {
    value.mappings[0].propertyMappings.push({ ...JSON.parse(JSON.stringify(value.mappings[0].propertyMappings[0])), projectPropertyId: 'param:tone' })
  })
  assert.match(mappingRegistrySemanticError(registry), /maps design property .* twice/)
})

check('CMP-MAP-PROP: a property mapping citing an adapter without an implementation is rejected', () => {
  const registry = registryOf((value) => { value.mappings[0].propertyMappings[0].adapterId = 'ghost-ds' })
  assert.match(mappingRegistrySemanticError(registry), /without an implementation/)
})

check('CMP-MAP-PROP: a value both mapped and ignored is rejected', () => {
  const registry = registryOf((value) => {
    value.mappings[0].propertyMappings[0].ignoredValues = [{ value: 'Large', reason: 'not shipped on mobile' }]
  })
  assert.match(mappingRegistrySemanticError(registry), /both maps and ignores/)
})

check('CMP-MAP-PROP: a value ignored twice is rejected', () => {
  const registry = registryOf((value) => {
    delete value.mappings[0].propertyMappings[0].valueMap.Large
    value.mappings[0].propertyMappings[0].ignoredValues = [
      { value: 'Large', reason: 'not shipped' },
      { value: 'Large', reason: 'duplicate row' }
    ]
  })
  assert.match(mappingRegistrySemanticError(registry), /ignores a value twice/)
})

check('CMP-MAP-SLOT: a slot mapping must carry exactly one of projectSlotId / ignoredReason', () => {
  const both = registryOf((value) => { value.mappings[0].slotMappings[0].ignoredReason = 'decorative' })
  assert.match(mappingRegistrySemanticError(both), /exactly one of projectSlotId or ignoredReason/)
  const neither = registryOf((value) => { delete value.mappings[0].slotMappings[0].projectSlotId })
  assert.match(mappingRegistrySemanticError(neither), /exactly one of projectSlotId or ignoredReason/)
})

check('CMP-MAP-SLOT: one design slot mapped twice for one adapter is rejected', () => {
  const registry = registryOf((value) => {
    value.mappings[0].slotMappings.push(JSON.parse(JSON.stringify(value.mappings[0].slotMappings[0])))
  })
  assert.match(mappingRegistrySemanticError(registry), /maps design slot .* twice/)
})

check('CMP-MAP-TOMBSTONE: retired requires a retirement record; active must not carry one', () => {
  const retired = registryOf((value) => { value.mappings[0].state = 'retired' })
  assert.match(mappingRegistrySemanticError(retired), /retired without a retirement record/)
  const contradictory = registryOf((value) => {
    value.mappings[0].retirement = { reason: 'x', actor: 'owner', at: AT }
  })
  assert.match(mappingRegistrySemanticError(contradictory), /active but carries a retirement/)
})

check('CMP-MAP: task-binding provenance requires the task stem', () => {
  const registry = registryOf((value) => {
    value.mappings[0].provenance = { kind: 'task-binding', actor: 'finalizer', at: AT }
  })
  assert.match(mappingRegistrySemanticError(registry), /requires taskStem/)
})

check('CMP-MAP-RENDER: a render-class tombstone (null) requires by=owner', () => {
  const auto = registryOf((value) => {
    value.mappings[0].visualPolicy = { renderClass: null, by: 'auto', at: AT }
  })
  assert.match(mappingRegistrySemanticError(auto), /tombstone requires by=owner/)
  const owner = registryOf((value) => {
    value.mappings[0].visualPolicy = { renderClass: null, by: 'owner', at: AT, reason: 'owner forbids auto re-stamping' }
  })
  assert.equal(mappingRegistrySemanticError(owner), null)
})

check('CMP-MAP-DISP: a design-side disposition must carry exactly designComponentId', () => {
  const registry = registryOf((value) => {
    value.dispositions.push({
      dispositionId: 'dcp-' + '9'.repeat(24),
      target: { side: 'design', designComponentId: DESIGN_ID.remote, adapterId: ADAPTER_ID },
      kind: 'ignored', reason: 'r', owner: 'o', createdAt: AT
    })
  })
  assert.match(mappingRegistrySemanticError(registry), /exactly designComponentId/)
})

check('CMP-MAP-DISP: a project-side disposition must carry projectComponentId and adapterId', () => {
  const registry = registryOf((value) => {
    value.dispositions.push({
      dispositionId: 'dcp-' + '8'.repeat(24),
      target: { side: 'project', projectComponentId: PROJECT_ID.spare },
      kind: 'ignored', reason: 'r', owner: 'o', createdAt: AT
    })
  })
  assert.match(mappingRegistrySemanticError(registry), /projectComponentId and adapterId/)
})

check('CMP-MAP-DISP: side-inverted kinds are rejected on both sides', () => {
  const designSide = registryOf((value) => {
    value.dispositions.push({
      dispositionId: 'dcp-' + '7'.repeat(24),
      target: { side: 'design', designComponentId: DESIGN_ID.remote },
      kind: 'intentionally-project-only', reason: 'r', owner: 'o', createdAt: AT
    })
  })
  assert.match(mappingRegistrySemanticError(designSide), /cannot target the design side/)
  const projectSide = registryOf((value) => {
    value.dispositions.push({
      dispositionId: 'dcp-' + '6'.repeat(24),
      target: { side: 'project', projectComponentId: PROJECT_ID.spare, adapterId: ADAPTER_ID },
      kind: 'intentionally-design-only', reason: 'r', owner: 'o', createdAt: AT
    })
  })
  assert.match(mappingRegistrySemanticError(projectSide), /cannot target the project side/)
})

check('CMP-MAP-DISP: superseded requires supersededBy — and only superseded may carry it', () => {
  const without = registryOf((value) => {
    value.dispositions.push({
      dispositionId: 'dcp-' + '5'.repeat(24),
      target: { side: 'design', designComponentId: DESIGN_ID.remote },
      kind: 'superseded', reason: 'r', owner: 'o', createdAt: AT
    })
  })
  assert.match(mappingRegistrySemanticError(without), /requires supersededBy/)
  const misplaced = registryOf((value) => {
    value.dispositions.push({
      dispositionId: 'dcp-' + '4'.repeat(24),
      target: { side: 'design', designComponentId: DESIGN_ID.remote },
      kind: 'ignored', supersededBy: DESIGN_ID.button, reason: 'r', owner: 'o', createdAt: AT
    })
  })
  assert.match(mappingRegistrySemanticError(misplaced), /outside kind superseded/)
})

check('CMP-MAP-DISP: two dispositions on one entity are rejected', () => {
  const registry = registryOf((value) => {
    for (const suffix of ['3', '2']) {
      value.dispositions.push({
        dispositionId: 'dcp-' + suffix.repeat(24),
        target: { side: 'design', designComponentId: DESIGN_ID.remote },
        kind: 'ignored', reason: 'r', owner: 'o', createdAt: AT
      })
    }
  })
  assert.match(mappingRegistrySemanticError(registry), /two dispositions target the same entity/)
})

check('CMP-MAP-DISP: an active mapping and a disposition on one design component are rejected', () => {
  const registry = registryOf((value) => {
    value.dispositions.push({
      dispositionId: 'dcp-' + '1'.repeat(24),
      target: { side: 'design', designComponentId: DESIGN_ID.button },
      kind: 'ignored', reason: 'r', owner: 'o', createdAt: AT
    })
  })
  assert.match(mappingRegistrySemanticError(registry), /both an active mapping and a disposition/)
})

check('CMP-MAP: fresh onboarding starts from the exact empty registry shape', () => {
  const empty = emptyMappingRegistry(design.scopeId)
  assert.equal(mappingRegistrySemanticError(empty), null)
  assert.deepEqual(empty, {
    schemaVersion: 2,
    revision: 0,
    designScopeId: design.scopeId,
    operationReceipts: [],
    mappings: [],
    dispositions: []
  })
})

// ── tasks/component-binding-contract.cjs ───────────────────────────────────
check('CMP-BIND: the canonical binding is accepted', () => {
  assert.equal(bindingContract.bindingError(validComponentTaskBinding()), null)
})

check('CMP-BIND: bindingSourceId parses only design-origin component sources', () => {
  const source = bindingContract.bindingSourceId(`design:component:${DESIGN_ID.button}:component-implement`)
  assert.equal(source.designComponentId, DESIGN_ID.button)
  assert.equal(source.kind, 'component-implement')
  assert.equal(bindingContract.bindingSourceId('design:token:figma-variable:VariableID:10:1:token-implement'), null)
  assert.equal(bindingContract.bindingSourceId('garbage'), null)
})

check('CMP-BIND: implement requires intendedProjectSymbol', () => {
  const binding = validComponentTaskBinding()
  delete binding.intendedProjectSymbol
  assert.match(bindingContract.bindingError(binding), /requires intendedProjectSymbol/)
})

check('CMP-BIND: remap requires mappingId — and then needs no intendedProjectSymbol', () => {
  const binding = validComponentTaskBinding()
  binding.intent = 'remap'
  binding.sourceId = `design:component:${DESIGN_ID.button}:component-remap`
  delete binding.intendedProjectSymbol
  assert.match(bindingContract.bindingError(binding), /requires mappingId/)
  binding.mappingId = 'cmap-' + '1'.repeat(24)
  assert.equal(bindingContract.bindingError(binding), null)
})

check('CMP-BIND: intent must match the sourceId family', () => {
  const binding = validComponentTaskBinding()
  binding.intent = 'update-visual'
  binding.mappingId = 'cmap-' + '1'.repeat(24)
  assert.match(bindingContract.bindingError(binding), /intent must match its sourceId/)
})

check('CMP-BIND: designComponentId must match the sourceId', () => {
  const binding = validComponentTaskBinding()
  binding.designComponentId = DESIGN_ID.icon
  assert.match(bindingContract.bindingError(binding), /designComponentId must match its sourceId/)
})

check('CMP-BIND: frozenSpec bounds are enforced', () => {
  const properties = validComponentTaskBinding()
  properties.frozenSpec.properties = new Array(41).fill(properties.frozenSpec.properties[0])
  assert.match(bindingContract.bindingError(properties), /frozenSpec\.properties malformed/)
  const variants = validComponentTaskBinding()
  variants.frozenSpec.variants = new Array(501).fill(variants.frozenSpec.variants[0])
  assert.match(bindingContract.bindingError(variants), /frozenSpec\.variants malformed/)
})

check('CMP-BIND: unknown keys and duplicate intended property bindings are rejected', () => {
  const unknown = validComponentTaskBinding()
  unknown.extra = true
  assert.match(bindingContract.bindingError(unknown), /unknown key/)
  const duplicated = validComponentTaskBinding()
  duplicated.intendedPropertyMappings = [
    { designPropertyId: 'p-size', projectPropertyId: 'param:size' },
    { designPropertyId: 'p-size', projectPropertyId: 'param:tone' }
  ]
  assert.match(bindingContract.bindingError(duplicated), /binds one design property twice/)
})

check('CMP-BIND: the evidence path derivation is deterministic and stays inside component-bindings/', () => {
  const sourceId = validComponentTaskBinding().sourceId
  const first = bindingContract.bindingRelativePath(sourceId)
  assert.equal(first, bindingContract.bindingRelativePath(sourceId))
  assert.match(first, /^orchestrator\/tasks\/evidence\/component-bindings\/[a-f0-9]{32}\.json$/)
  assert.notEqual(first, bindingContract.bindingRelativePath(`design:component:${DESIGN_ID.icon}:component-implement`))
})

console.log(`\ncomponent-mapping.test: ${pass} pass, ${fail} fail`)
process.exit(fail === 0 ? 0 : 1)
