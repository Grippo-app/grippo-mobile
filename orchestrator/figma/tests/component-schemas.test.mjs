// component-schemas.test.mjs — contract pins for the component schemas in
// orchestrator/figma/schemas/*. Every schema must compile; every fixture from
// component-fixtures.mjs must pass its schema AND semantic validator; targeted
// negative probes must be rejected by schema or semantics with the same
// failure family a consumer would see (CMP-CAP-*, CMP-INV-*, CMP-ADP-*,
// CMP-MAP-*, CMP-CMP-*, CMP-TASK-* rule ids in names).
import assert from 'node:assert/strict'
import { readFileSync, readdirSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import {
  validComponentCapture, COMPONENT_CAPTURE_HASH, validComponentAdapterConfig,
  validProjectComponentInventory, validComponentAnalysisIndex, validComponentRegistry,
  comparatorContext, validComponentTaskBinding, DESIGN_ID
} from './component-fixtures.mjs'
import { normalizeCapture } from '../components/capture-normalizer.mjs'
import { inventorySemanticError } from '../components/design-inventory-contract.mjs'
import { projectInventorySemanticError, projectInventorySemanticHash, analysisIndexSemanticError } from '../components/project-inventory-contract.mjs'
import { mappingRegistrySemanticError } from '../components/mapping-contract.mjs'
import { compareComponents } from '../components/comparator.mjs'
import { publishableBaseline } from '../components/baseline.mjs'
import { suggestComponentTasks } from '../components/task-suggestions.mjs'
import { validateAdapterConfig } from '../runtime/adapter-config.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const SCHEMAS_DIR = join(HERE, '..', 'schemas')
const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }

const { default: Ajv } = await import('ajv')
const ajv = new Ajv({ allErrors: true, strict: false })
ajv.addFormat('date-time', {
  type: 'string',
  validate(value) {
    if (typeof value !== 'string' ||
        !/^\d{4}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\d|3[01])T(?:[01]\d|2[0-3]):[0-5]\d:[0-5]\d(?:\.\d{3})?Z$/.test(value)) return false
    const parsed = new Date(value)
    return Number.isFinite(parsed.getTime()) &&
      parsed.toISOString() === (value.includes('.') ? value : value.replace('Z', '.000Z'))
  }
})
const validators = Object.create(null)
const schemas = []
for (const file of readdirSync(SCHEMAS_DIR).sort()) {
  const schema = JSON.parse(readFileSync(join(SCHEMAS_DIR, file), 'utf8'))
  schemas.push([file.replace('.schema.json', ''), schema])
}
for (const [, schema] of schemas) ajv.addSchema(schema)
for (const [name, schema] of schemas) validators[name] = ajv.getSchema(schema.$id)

function assertValid(validator, value, label) {
  const ok = validator(value)
  if (!ok) {
    const first = validator.errors[0]
    assert.fail(`${label} rejected: ${first.instancePath || '/'} ${first.message}`)
  }
}
function assertInvalid(validator, value, label) {
  assert.equal(validator(value), false, `${label} must be rejected by schema`)
}

check('every component schema compiles and is present', () => {
  for (const name of [
    'design-component-capture', 'design-component-inventory', 'project-component-inventory',
    'project-component-analysis-index', 'component-mappings', 'component-comparison',
    'component-baseline', 'component-mapping-suggestions', 'component-task-suggestions',
    'component-task-binding', 'project-adapters'
  ]) {
    assert.equal(typeof validators[name], 'function', `missing validator for ${name}`)
  }
})

// ── capture ────────────────────────────────────────────────────────────────
check('CMP-CAP: valid capture fixture passes schema + normalizes cleanly', () => {
  assertValid(validators['design-component-capture'], validComponentCapture(), 'capture')
  normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)
})

check('CMP-CAP: unknown key on capture is rejected (additionalProperties)', () => {
  const capture = validComponentCapture()
  capture.extra = 1
  assertInvalid(validators['design-component-capture'], capture, 'capture with unknown key')
})

check('CMP-CAP: retired schemaVersion 1 is rejected (const)', () => {
  const capture = validComponentCapture()
  capture.schemaVersion = 1
  assertInvalid(validators['design-component-capture'], capture, 'capture schemaVersion 1')
})

check('CMP-CAP: duplicate scope.pageIds are rejected (uniqueItems)', () => {
  const capture = validComponentCapture()
  capture.scope.pageIds = ['0:1', '0:1']
  assertInvalid(validators['design-component-capture'], capture, 'duplicate pageIds')
})

check('CMP-CAP: unknown entity kind is rejected (enum)', () => {
  const capture = validComponentCapture()
  capture.entities[0].kind = 'frame'
  assertInvalid(validators['design-component-capture'], capture, 'entity kind frame')
})

check('CMP-CAP: component entity identity cannot be name-derived', () => {
  const capture = validComponentCapture()
  capture.entities[0].idQuality = 'name-derived'
  assertInvalid(validators['design-component-capture'], capture, 'name-derived component entity')
})

check('CMP-CAP: entities over the 2000 bound are rejected by schema (maxItems)', () => {
  const capture = validComponentCapture()
  const template = capture.entities[1]
  capture.entities = Array.from({ length: 2001 }, (_, index) => ({ ...template, nodeId: `9:${index}`, name: `bulk${index}` }))
  assertInvalid(validators['design-component-capture'], capture, 'oversized capture')
})

check('CMP-CAP: variants over the per-entity 500 bound are rejected (maxItems)', () => {
  const capture = validComponentCapture()
  const template = capture.entities[0].variants[0]
  capture.entities[0].variants = Array.from({ length: 501 }, (_, index) => ({ ...template, nodeId: `10:${index + 100}` }))
  assertInvalid(validators['design-component-capture'], capture, 'oversized variants')
})

check('CMP-CAP: a malformed visual sha256 is rejected (pattern)', () => {
  const capture = validComponentCapture()
  capture.visual[0].sha256 = 'sha256:not-a-hash'
  assertInvalid(validators['design-component-capture'], capture, 'malformed visual hash')
})

check('CMP-CAP: a visual file outside visual/ is rejected (pattern)', () => {
  const capture = validComponentCapture()
  capture.visual[0].file = 'other/button.png'
  assertInvalid(validators['design-component-capture'], capture, 'escaping visual path')
})

check('CMP-CAP: visual evidence accepts 64 artifacts and rejects the 65th', () => {
  const atLimit = validComponentCapture()
  const template = atLimit.visual[0]
  atLimit.visual = Array.from({ length: 64 }, (_, index) => ({
    ...template,
    file: `visual/evidence-${String(index).padStart(2, '0')}.png`,
    sha256: 'sha256:' + index.toString(16).padStart(64, '0')
  }))
  assertValid(validators['design-component-capture'], atLimit, '64 visual artifacts')
  const over = structuredClone(atLimit)
  over.visual.push({
    ...template,
    file: 'visual/evidence-64.png',
    sha256: 'sha256:' + 'f'.repeat(64)
  })
  assert.equal(validators['design-component-capture'](over), false)
})

// ── design inventory ───────────────────────────────────────────────────────
check('CMP-INV: normalized inventory from the valid capture passes schema + semantics', () => {
  const inventory = normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)
  assertValid(validators['design-component-inventory'], inventory, 'inventory')
  assert.equal(inventorySemanticError(inventory), null)
})

check('CMP-INV: unknown key on the inventory is rejected (additionalProperties)', () => {
  const inventory = normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)
  inventory.extra = 1
  assertInvalid(validators['design-component-inventory'], inventory, 'inventory with unknown key')
})

check('CMP-INV: a malformed designComponentId is rejected (pattern)', () => {
  const inventory = normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)
  inventory.components[0].designComponentId = 'figma-component:XYZ:none:10:1'
  assertInvalid(validators['design-component-inventory'], inventory, 'malformed designComponentId')
})

check('CMP-INV: published component identity cannot be name-derived', () => {
  const inventory = normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)
  inventory.components[0].providerIdentity.idQuality = 'name-derived'
  assertInvalid(validators['design-component-inventory'], inventory, 'name-derived published component')
  assert.match(inventorySemanticError(inventory), /provider-stable node identity/)
})

check('CMP-INV: a truncated capture witness can never appear on a published inventory (const false)', () => {
  const inventory = normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)
  inventory.capture.truncated = true
  assertInvalid(validators['design-component-inventory'], inventory, 'truncated published inventory')
})

// ── adapter config ─────────────────────────────────────────────────────────
check('CMP-ADP: kotlin-compose + component-manifest component adapters pass schema + semantics', () => {
  const config = validComponentAdapterConfig()
  assertValid(validators['project-adapters'], config, 'adapter config')
  const validated = validateAdapterConfig(config, { projectRoot: '/tmp' })
  assert.deepEqual(validated.enabledComponentAdapters.map((adapter) => adapter.id), ['compose-ds', 'manifest-ds'])
  assert.deepEqual(validated.enabledTokenAdapters.map((adapter) => adapter.id), ['compose-ds'])
})

check('CMP-ADP: a tokens-only adapter without a components section stays valid', () => {
  const config = validComponentAdapterConfig()
  const adapter = config.adapters[0]
  adapter.capabilities = ['tokens']
  delete adapter.components
  config.adapters = [adapter]
  assertValid(validators['project-adapters'], config, 'tokens-only adapter')
  assert.equal(validateAdapterConfig(config, { projectRoot: '/tmp' }).enabledComponentAdapters.length, 0)
})

check('CMP-ADP: a components section without the declared capability is rejected', () => {
  const config = validComponentAdapterConfig()
  config.adapters[0].capabilities = ['tokens']
  assertInvalid(validators['project-adapters'], config, 'undeclared components section')
  assert.throws(() => validateAdapterConfig(config, { projectRoot: '/tmp' }))
})

check('CMP-ADP: the components capability without its section is rejected', () => {
  const config = validComponentAdapterConfig()
  delete config.adapters[0].components
  assertInvalid(validators['project-adapters'], config, 'capability without section')
  assert.throws(() => validateAdapterConfig(config, { projectRoot: '/tmp' }), /has no components section|components/)
})

check('CMP-ADP: json-tokens claiming the components capability is rejected by the semantic layer', () => {
  const config = validComponentAdapterConfig()
  config.adapters = [{
    ...config.adapters[1],
    id: 'json-hybrid',
    kind: 'json-tokens',
    capabilities: ['components']
  }]
  // The schema cannot know per-kind capability sets; the semantic layer must.
  assertValid(validators['project-adapters'], config, 'json-tokens with components section')
  assert.throws(() => validateAdapterConfig(config, { projectRoot: '/tmp' }),
    (error) => error.code === 'PROJECT_ADAPTER_CONFIG_INVALID' && /does not ship capability/.test(error.detail))
})

check('CMP-ADP: component-manifest claiming the tokens capability is rejected by the semantic layer', () => {
  const config = validComponentAdapterConfig()
  const manifest = config.adapters[1]
  manifest.capabilities = ['tokens', 'components']
  manifest.tokens = JSON.parse(JSON.stringify(config.adapters[0].tokens))
  assert.throws(() => validateAdapterConfig(config, { projectRoot: '/tmp' }),
    (error) => /does not ship capability/.test(error.detail))
})

check('CMP-ADP: overlapping component roots across enabled adapters are rejected', () => {
  const config = validComponentAdapterConfig()
  config.adapters[1].components.roots = ['design-system/components/web']
  assert.throws(() => validateAdapterConfig(config, { projectRoot: '/tmp' }), /overlapping components roots/)
})

// ── project inventory + analysis index ─────────────────────────────────────
check('CMP-KT: valid project inventory fixture passes schema + semantics', () => {
  const inventory = validProjectComponentInventory()
  assertValid(validators['project-component-inventory'], inventory, 'project inventory')
  assert.equal(projectInventorySemanticError(inventory), null)
})

check('CMP-KT: a duplicate variant-property value is a semantic error', () => {
  const inventory = validProjectComponentInventory()
  const size = inventory.components.find((component) => component.name === 'AppButton')
    .variantProperties.find((property) => property.projectPropertyId === 'param:size')
  size.values.push({ value: 'Small' })
  assert.match(projectInventorySemanticError(inventory), /duplicate values/)
})

check('CMP-KT: witness.complete must derive from its own evidence', () => {
  const inventory = validProjectComponentInventory()
  inventory.witness.parseFailures = [{ path: 'design-system/src/AppButton.kt', reason: 'parse error' }]
  assert.match(projectInventorySemanticError(inventory), /witness\.complete/)
})

check('CMP-KT: analysis index fixture passes schema + cross-artifact semantics', () => {
  const inventory = validProjectComponentInventory()
  const index = validComponentAnalysisIndex([inventory], projectInventorySemanticHash)
  assertValid(validators['project-component-analysis-index'], index, 'analysis index')
  assert.equal(analysisIndexSemanticError(index, new Map([[inventory.adapterId, inventory]]), {
    enabledAdapterIds: [inventory.adapterId], configHash: inventory.configHash
  }), null)
})

check('CMP-KT: analysis index rejects an inventory hash mismatch', () => {
  const inventory = validProjectComponentInventory()
  const index = validComponentAnalysisIndex([inventory], () => 'sha256:' + '9'.repeat(64))
  assert.match(analysisIndexSemanticError(index, new Map([[inventory.adapterId, inventory]]), null), /does not match its inventory/)
})

// ── mapping registry ───────────────────────────────────────────────────────
check('CMP-MAP: valid registry fixture passes schema + semantics', () => {
  const design = normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)
  const registry = validComponentRegistry(design.scopeId)
  assertValid(validators['component-mappings'], registry, 'registry')
  assert.equal(mappingRegistrySemanticError(registry), null)
})

check('CMP-MAP: a malformed mappingId is rejected by schema (pattern)', () => {
  const design = normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)
  const registry = validComponentRegistry(design.scopeId)
  registry.mappings[0].mappingId = 'map-' + '1'.repeat(24)
  assertInvalid(validators['component-mappings'], registry, 'wrong mappingId prefix')
})

check('CMP-MAP: duplicate projectComponentIds inside one implementation are rejected (uniqueItems)', () => {
  const design = normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)
  const registry = validComponentRegistry(design.scopeId)
  const implementation = registry.mappings[0].implementations[0]
  implementation.projectComponentIds = [implementation.projectComponentIds[0], implementation.projectComponentIds[0]]
  assertInvalid(validators['component-mappings'], registry, 'duplicate implementation targets')
})

check('CMP-MAP: an unknown relation is rejected (enum)', () => {
  const design = normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)
  const registry = validComponentRegistry(design.scopeId)
  registry.mappings[0].implementations[0].relation = 'clone'
  assertInvalid(validators['component-mappings'], registry, 'unknown relation')
})

check('CMP-MAP: more than 8 implementations on one mapping are rejected (maxItems)', () => {
  const design = normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)
  const registry = validComponentRegistry(design.scopeId)
  const template = registry.mappings[0].implementations[0]
  registry.mappings[0].implementations = Array.from({ length: 9 }, (_, index) => ({
    ...JSON.parse(JSON.stringify(template)), platform: `platform-${index}`
  }))
  assertInvalid(validators['component-mappings'], registry, 'too many implementations')
})

check('CMP-MAP: unknown key on a mapping row is rejected (additionalProperties)', () => {
  const design = normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)
  const registry = validComponentRegistry(design.scopeId)
  registry.mappings[0].note = 'x'
  assertInvalid(validators['component-mappings'], registry, 'mapping with unknown key')
})

// ── comparator outputs ─────────────────────────────────────────────────────
check('CMP-CMP: a full comparator run over the fixtures passes report/suggestions/tasks/baseline schemas', () => {
  const design = normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)
  const project = validProjectComponentInventory()
  const index = validComponentAnalysisIndex([project], projectInventorySemanticHash)
  const registry = validComponentRegistry(design.scopeId)
  const { report, suggestions, baselineCandidate } = compareComponents({
    designInventory: design,
    projectInventories: [project],
    analysisIndex: index,
    mappingRegistry: registry,
    baseline: null,
    tokenSnapshot: { report: null, registry: null },
    context: comparatorContext()
  })
  assertValid(validators['component-comparison'], report, 'comparison report')
  assertValid(validators['component-mapping-suggestions'], suggestions, 'suggestions')
  assertValid(validators['component-task-suggestions'], suggestComponentTasks(report), 'task suggestions')
  const baseline = publishableBaseline(baselineCandidate, null, '2026-07-20T12:00:00.000Z')
  assertValid(validators['component-baseline'], baseline, 'baseline')
  assert.equal(baseline.source.eligibleAt, '2026-07-20T12:00:00.000Z')
})

check('CMP-CMP: a baseline entry with a status outside matched/drifted is rejected (enum)', () => {
  const design = normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)
  const project = validProjectComponentInventory()
  const index = validComponentAnalysisIndex([project], projectInventorySemanticHash)
  const registry = validComponentRegistry(design.scopeId)
  const { baselineCandidate } = compareComponents({
    designInventory: design, projectInventories: [project], analysisIndex: index,
    mappingRegistry: registry, baseline: null, tokenSnapshot: { report: null, registry: null },
    context: comparatorContext()
  })
  const baseline = publishableBaseline(baselineCandidate, null, '2026-07-20T12:00:00.000Z')
  baseline.entries[0].status = 'unmapped'
  assertInvalid(validators['component-baseline'], baseline, 'baseline entry status unmapped')
})

// ── task binding ───────────────────────────────────────────────────────────
check('CMP-TASK: component task binding schema accepts a canonical binding and rejects unknown keys', () => {
  const binding = validComponentTaskBinding()
  assertValid(validators['component-task-binding'], binding, 'task binding')
  assertInvalid(validators['component-task-binding'], { ...binding, extra: true }, 'binding with unknown key')
  assertInvalid(validators['component-task-binding'], { ...binding, intent: 'do-something' }, 'binding with unknown intent')
  assertInvalid(validators['component-task-binding'], {
    ...binding,
    sourceId: `design:component:${DESIGN_ID.button}:component-classify-project-only`
  }, 'binding for a non-binding proposal family')
  assertInvalid(validators['component-task-binding'], { ...binding, designInventoryHash: 'sha256:zz' }, 'binding with malformed hash')
})

console.log(`\ncomponent-schemas.test: ${pass} pass, ${fail} fail`)
process.exit(fail === 0 ? 0 : 1)
