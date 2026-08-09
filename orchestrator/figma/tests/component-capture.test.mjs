// component-capture.test.mjs — decision-table pins for the component capture
// completeness gates and the deterministic normalizer (CMP-CAP-*, CMP-INV-*):
// witness reconciliation, publication gates (truncation / permissions / limits
// / inconsistency / empty-scope proof), slot derivation, dependency resolution
// incl. variant-node -> owning-set, token-ref dedupe, visual coverage, and
// determinism. Pure module tests — no filesystem, no clock, no runner.
import assert from 'node:assert/strict'
import { validComponentCapture, COMPONENT_CAPTURE_HASH, DESIGN_ID } from './component-fixtures.mjs'
import { normalizeCapture } from '../components/capture-normalizer.mjs'
import { captureSemanticError, captureCompletenessGate, deriveScopeId, inventorySemanticError } from '../components/design-inventory-contract.mjs'
import { canonicalJson } from '../runtime/canonical-json.mjs'

const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }

const componentOf = (inventory, nodeId) =>
  inventory.components.find((component) => component.providerIdentity.nodeId === nodeId)
const buttonEntity = (capture) => capture.entities.find((entity) => entity.nodeId === '10:1')

// ── happy path ─────────────────────────────────────────────────────────────
check('CMP-CAP-COMPLETE: the valid capture normalizes; witness echoed with absence proof', () => {
  const inventory = normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)
  assert.equal(inventory.capture.completeness, 'complete')
  assert.equal(inventory.capture.absenceProofEligible, true)
  assert.equal(inventory.capture.captureHash, COMPONENT_CAPTURE_HASH)
  assert.deepEqual(
    [inventory.counts.componentSets, inventory.counts.standaloneComponents, inventory.counts.unsupportedComponents],
    [2, 1, 1]
  )
  assert.equal(inventory.counts.capturedEntities, 4)
})

check('CMP-INV-ID: designComponentId derives from the provider node id; rename keeps identity', () => {
  const renamed = validComponentCapture()
  buttonEntity(renamed).name = 'ButtonRenamed'
  const inventory = normalizeCapture(renamed, COMPONENT_CAPTURE_HASH)
  const button = componentOf(inventory, '10:1')
  assert.equal(button.designComponentId, DESIGN_ID.button)
  assert.equal(button.name, 'ButtonRenamed')
})

check('CMP-INV-ID: name-derived component identity is rejected before normalization', () => {
  const capture = validComponentCapture()
  buttonEntity(capture).idQuality = 'name-derived'
  assert.match(captureSemanticError(capture), /provider-stable node identity/)
  assert.throws(
    () => normalizeCapture(capture, COMPONENT_CAPTURE_HASH),
    (error) => error.code === 'COMPONENT_DESIGN_CAPTURE_INVALID' && /provider-stable node identity/.test(error.detail)
  )
})

check('CMP-INV-SLOTS: slots derive as prop:/layer:/nested: — bound text layers are never doubled', () => {
  const inventory = normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)
  const button = componentOf(inventory, '10:1')
  assert.deepEqual(button.semanticSlots.map((slot) => `${slot.slotId}|${slot.kind}`), [
    'prop:p-label|text-property',
    'prop:p-icon|instance-swap',
    'layer:10:9|static-text',
    'nested:20:1|nested-component',
    'nested:30:2|nested-component'
  ])
  // the layer bound to p-label (10:8) must NOT appear as a static-text slot
  assert.equal(button.semanticSlots.some((slot) => slot.slotId === 'layer:10:8'), false)
})

check('CMP-INV-DEPS: nested refs resolve to captured entities; a variant node resolves to its owning set', () => {
  const inventory = normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)
  const dependencies = componentOf(inventory, '10:1').dependencies
  const icon = dependencies.find((dependency) => dependency.targetNodeId === '20:1')
  assert.equal(icon.resolved, true)
  assert.equal(icon.targetDesignComponentId, DESIGN_ID.icon)
  const badgeVariant = dependencies.find((dependency) => dependency.targetNodeId === '30:2')
  assert.equal(badgeVariant.resolved, true)
  assert.equal(badgeVariant.targetDesignComponentId, DESIGN_ID.badge, 'variant node must resolve to the OWNING set')
  assert.equal(badgeVariant.targetVariantId, '30:2')
  const remote = dependencies.find((dependency) => dependency.targetNodeId === '99:99')
  assert.equal(remote.resolved, false)
  assert.equal(remote.targetDesignComponentId, undefined, 'an unresolved edge never invents a target id')
})

check('CMP-INV-PROPS: an unknown provider property type lands in unsupportedProperties with evidence', () => {
  const capture = validComponentCapture()
  buttonEntity(capture).properties.push({ propertyId: 'p-magic', name: 'Magic', type: 'unknown', idQuality: 'stable', providerType: 'EXOTIC' })
  const inventory = normalizeCapture(capture, COMPONENT_CAPTURE_HASH)
  const button = componentOf(inventory, '10:1')
  assert.equal(button.properties.some((property) => property.propertyId === 'p-magic'), false)
  const unsupported = button.unsupportedProperties.find((property) => property.propertyId === 'p-magic')
  assert.equal(unsupported.providerType, 'EXOTIC')
  assert.match(unsupported.reason, /not supported/)
})

check('CMP-INV-REMOTE: a remote entity is referenced-only, never an authoritative component row', () => {
  const inventory = normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)
  assert.equal(componentOf(inventory, '40:1'), undefined)
  const remote = inventory.unsupportedComponents.find((item) => item.designComponentId === DESIGN_ID.remote)
  assert.match(remote.reason, /referenced-only/)
})

check('CMP-INV-TOKENS: bound variables dedupe and sort into stable token refs', () => {
  const inventory = normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)
  const refs = componentOf(inventory, '10:1').tokenRefs
  assert.deepEqual(refs.map((ref) => ref.observedTokenKey), [
    'otk:sha256:' + '1'.repeat(64),
    'otk:sha256:' + '2'.repeat(64)
  ])
  assert.equal(refs.length, 2, 'the duplicate bound variable must collapse to one ref')
})

check('CMP-INV-VARIANTS: sparse tuples pass through sorted with the declared default; nothing invented', () => {
  const inventory = normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)
  const button = componentOf(inventory, '10:1')
  assert.deepEqual(button.variants.map((variant) => variant.variantId), ['10:2', '10:3', '10:4'])
  assert.equal(button.variants.length, 3, 'Small+Danger does not exist and must never be invented')
  assert.equal(button.defaultVariantId, '10:2')
  assert.equal(button.defaultKnown, true)
  assert.deepEqual(button.variants.map((variant) => variant.isDefault), [true, false, false])
})

check('CMP-INV-DEFAULT: a set without any declared default stays defaultKnown=false', () => {
  const inventory = normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)
  const badge = componentOf(inventory, '30:1')
  assert.equal(badge.defaultVariantId, null)
  assert.equal(badge.defaultKnown, false)
})

check('CMP-INV-VISUAL: coverage derives from roles — representative / partial / none', () => {
  const inventory = normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)
  assert.equal(componentOf(inventory, '10:1').visualEvidence.coverage, 'representative')
  assert.equal(componentOf(inventory, '30:1').visualEvidence.coverage, 'none')
  const partial = validComponentCapture()
  partial.visual = partial.visual.filter((entry) => entry.entityNodeId !== '10:1' || entry.role === 'extreme')
  assert.equal(componentOf(normalizeCapture(partial, COMPONENT_CAPTURE_HASH), '10:1').visualEvidence.coverage, 'partial')
})

check('CMP-INV-DETERMINISM: shuffled entity order and repeated runs serialize byte-identically', () => {
  const first = normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)
  const shuffled = validComponentCapture()
  shuffled.entities.reverse()
  shuffled.visual.reverse()
  const second = normalizeCapture(shuffled, COMPONENT_CAPTURE_HASH)
  assert.equal(canonicalJson(first.components), canonicalJson(second.components))
  assert.equal(canonicalJson(first.unsupportedComponents), canonicalJson(second.unsupportedComponents))
  assert.equal(JSON.stringify(first.counts), JSON.stringify(second.counts))
})

// ── publication gates ──────────────────────────────────────────────────────
check('CMP-CAP-COMPLETE: truncated enumeration refuses publication (typed incomplete)', () => {
  const capture = validComponentCapture()
  capture.witness.truncated = true
  assert.throws(() => normalizeCapture(capture, COMPONENT_CAPTURE_HASH), (error) => error.code === 'COMPONENT_DESIGN_CAPTURE_INCOMPLETE')
})

check('CMP-CAP-COMPLETE: permission degradation is COMPONENT_DESIGN_ACCESS_DEGRADED', () => {
  const capture = validComponentCapture()
  capture.witness.permissionDegraded = true
  assert.throws(() => normalizeCapture(capture, COMPONENT_CAPTURE_HASH), (error) => error.code === 'COMPONENT_DESIGN_ACCESS_DEGRADED')
})

check('CMP-CAP-COMPLETE: a hit limit refuses publication', () => {
  const capture = validComponentCapture()
  capture.witness.limitsHit = ['response-bytes']
  assert.throws(() => normalizeCapture(capture, COMPONENT_CAPTURE_HASH), (error) => error.code === 'COMPONENT_DESIGN_CAPTURE_INCOMPLETE')
})

check('CMP-CAP-COMPLETE: completeness=incomplete refuses publication', () => {
  const capture = validComponentCapture()
  capture.witness.completeness = 'incomplete'
  assert.throws(() => normalizeCapture(capture, COMPONENT_CAPTURE_HASH), (error) => error.code === 'COMPONENT_DESIGN_CAPTURE_INCOMPLETE')
})

check('CMP-CAP-COMPLETE: a requested-but-unread page refuses publication', () => {
  const capture = validComponentCapture()
  capture.scope.pageIds = ['0:1', '0:2']
  capture.witness.requestedPageIds = ['0:1', '0:2']
  const gate = captureCompletenessGate(capture)
  assert.equal(gate.ok, false)
  assert.equal(gate.code, 'COMPONENT_DESIGN_CAPTURE_INCOMPLETE')
  assert.throws(() => normalizeCapture(capture, COMPONENT_CAPTURE_HASH), (error) => error.code === 'COMPONENT_DESIGN_CAPTURE_INCOMPLETE')
})

check('CMP-CAP-EMPTY: an empty capture without an authoritative zero count is a failure', () => {
  const capture = validComponentCapture()
  capture.entities = []
  capture.visual = []
  capture.witness.readEntityCount = 0
  capture.witness.expectedEntityCount = -1
  const gate = captureCompletenessGate(capture)
  assert.equal(gate.ok, false)
  assert.equal(gate.code, 'COMPONENT_DESIGN_CAPTURE_INCOMPLETE')
})

check('CMP-CAP-EMPTY: an authoritative zero-count empty scope publishes an empty inventory with absence proof', () => {
  const capture = validComponentCapture()
  capture.entities = []
  capture.visual = []
  capture.witness.readEntityCount = 0
  capture.witness.expectedEntityCount = 0
  const inventory = normalizeCapture(capture, COMPONENT_CAPTURE_HASH)
  assert.equal(inventory.counts.components, 0)
  assert.equal(inventory.counts.capturedEntities, 0)
  assert.equal(inventory.capture.absenceProofEligible, true, 'consistency stays proven, so absence stays provable')
})

check('CMP-CAP-CONSISTENCY: unproven consistency publishes but is NOT absence-proof eligible', () => {
  const capture = validComponentCapture()
  delete capture.witness.providerRevisionBefore
  delete capture.witness.providerRevisionAfter
  capture.witness.consistency = 'unproven'
  const inventory = normalizeCapture(capture, COMPONENT_CAPTURE_HASH)
  assert.equal(inventory.capture.absenceProofEligible, false)
})

check('CMP-CAP: a non-hash captureHash is refused before any validation work', () => {
  assert.throws(() => normalizeCapture(validComponentCapture(), 'not-a-hash'), (error) => error.code === 'COMPONENT_DESIGN_CAPTURE_INVALID')
})

// ── capture semantic errors ────────────────────────────────────────────────
check('CMP-CAP-SEM: duplicate entity node ids are rejected', () => {
  const capture = validComponentCapture()
  capture.entities.push(JSON.parse(JSON.stringify(capture.entities[1])))
  assert.match(captureSemanticError(capture), /duplicate entity nodeId/)
})

check('CMP-CAP-SEM: a variant assignment outside the declared options is rejected', () => {
  const capture = validComponentCapture()
  buttonEntity(capture).variants[0].assignments['p-size'] = 'Huge'
  assert.match(captureSemanticError(capture), /outside the declared options/)
})

check('CMP-CAP-SEM: a variant missing an assignment without captureIssue is rejected', () => {
  const capture = validComponentCapture()
  delete buttonEntity(capture).variants[0].assignments['p-tone']
  assert.match(captureSemanticError(capture), /misses an assignment .* captureIssue/)
})

check('CMP-CAP-SEM: an expectedVariantCount mismatch without captureIssue is rejected', () => {
  const capture = validComponentCapture()
  buttonEntity(capture).expectedVariantCount = 4
  assert.match(captureSemanticError(capture), /provider reported 4.*captureIssue/)
})

check('CMP-CAP-SEM: the same mismatch WITH a captureIssue degrades to an unsupported entity instead', () => {
  const capture = validComponentCapture()
  buttonEntity(capture).expectedVariantCount = 4
  buttonEntity(capture).captureIssue = 'child capture truncated by the provider'
  capture.visual = capture.visual.filter((entry) => entry.entityNodeId !== '10:1')
  assert.equal(captureSemanticError(capture), null)
  const inventory = normalizeCapture(capture, COMPONENT_CAPTURE_HASH)
  const unsupported = inventory.unsupportedComponents.find((item) => item.designComponentId === DESIGN_ID.button)
  assert.match(unsupported.reason, /truncated/)
})

check('CMP-CAP-SEM: a visual entry citing a variant of ANOTHER entity is rejected', () => {
  const capture = validComponentCapture()
  capture.visual[0].variantNodeId = '30:2'
  assert.match(captureSemanticError(capture), /outside entity/)
})

check('CMP-CAP-SEM: witness readPageIds must be a subset of requestedPageIds', () => {
  const capture = validComponentCapture()
  capture.witness.readPageIds = ['0:1', '0:7']
  assert.match(captureSemanticError(capture), /never requested/)
})

check('CMP-CAP-SEM: consistency=proven with differing before/after revisions is invalid', () => {
  const capture = validComponentCapture()
  capture.witness.providerRevisionAfter = 'rev-2'
  assert.match(captureSemanticError(capture), /identical before\/after/)
})

check('CMP-CAP-SEM: two variants with identical assignments in one set are rejected', () => {
  const capture = validComponentCapture()
  const variants = buttonEntity(capture).variants
  variants.push({ ...JSON.parse(JSON.stringify(variants[0])), nodeId: '10:5', isDefault: false })
  buttonEntity(capture).expectedVariantCount = 4
  assert.match(captureSemanticError(capture), /identical assignments/)
})

// ── published-inventory semantic errors ────────────────────────────────────
check('CMP-INV-SEM: a tampered structuralHash is rejected by recomputation', () => {
  const inventory = normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)
  componentOf(inventory, '10:1').structuralHash = 'sha256:' + '0'.repeat(64)
  assert.match(inventorySemanticError(inventory), /structuralHash does not match/)
})

check('CMP-INV-SEM: a tampered sourceHash is rejected by recomputation', () => {
  const inventory = normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)
  componentOf(inventory, '10:1').sourceHash = 'sha256:' + '0'.repeat(64)
  assert.match(inventorySemanticError(inventory), /sourceHash does not match/)
})

check('CMP-INV-SEM: defaultKnown must agree with defaultVariantId', () => {
  const inventory = normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)
  componentOf(inventory, '30:1').defaultKnown = true
  assert.match(inventorySemanticError(inventory), /defaultKnown disagrees/)
})

check('CMP-INV-SEM: a text property without its derived prop: slot is rejected', () => {
  const inventory = normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)
  const button = componentOf(inventory, '10:1')
  button.semanticSlots = button.semanticSlots.filter((slot) => slot.slotId !== 'prop:p-label')
  assert.match(inventorySemanticError(inventory), /text property .* has no derived slot/)
})

check('CMP-INV-SEM: absenceProofEligible must equal (consistency === proven)', () => {
  const inventory = normalizeCapture(validComponentCapture(), COMPONENT_CAPTURE_HASH)
  inventory.capture.absenceProofEligible = false
  assert.match(inventorySemanticError(inventory), /absenceProofEligible/)
})

// ── scope identity ─────────────────────────────────────────────────────────
check('CMP-CAP-SCOPE: scope id derives from file+branch+requested scope, never from names', () => {
  const capture = validComponentCapture()
  const scopeA = deriveScopeId(capture)
  capture.pages[0].name = 'Renamed Page'
  capture.entities[0].name = 'Renamed Button'
  assert.equal(deriveScopeId(capture), scopeA)
  const widened = validComponentCapture()
  widened.scope = { kind: 'all-pages' }
  assert.notEqual(deriveScopeId(widened), scopeA)
})

console.log(`\ncomponent-capture.test: ${pass} pass, ${fail} fail`)
process.exit(fail === 0 ? 0 : 1)
