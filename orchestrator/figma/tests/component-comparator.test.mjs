// component-comparator.test.mjs — the decision-table pins for the pure
// component comparator and its derived task proposals (CMP-CMP-*, CMP-ABS-*,
// CMP-SUG-*, CMP-DIR-*, CMP-17.12-*, CMP-TASK-*): every status, binding-gap
// precedence, sparse-variant exactness, absence-proof gating, baseline
// direction, the §17.12 token-causality proof, determinism, and fail-closed
// limits. The comparator is pure, so every case runs on in-memory fixtures.
import assert from 'node:assert/strict'
import {
  validComponentCapture, COMPONENT_CAPTURE_HASH, validProjectComponentInventory,
  validComponentAnalysisIndex, validComponentRegistry, comparatorContext, tokenSnapshotFixture,
  emptyManifestInventory, finishProjectComponentInventory, DESIGN_ID, PROJECT_ID, ADAPTER_ID
} from './component-fixtures.mjs'
import { normalizeCapture } from '../components/capture-normalizer.mjs'
import { projectInventorySemanticHash } from '../components/project-inventory-contract.mjs'
import { compareComponents } from '../components/comparator.mjs'
import { publishableBaseline } from '../components/baseline.mjs'
import { suggestComponentTasks } from '../components/task-suggestions.mjs'
import { COMPONENT_LIMITS } from '../components/limits.mjs'

const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }

function world(mutate) {
  const state = {
    capture: validComponentCapture(),
    project: validProjectComponentInventory(),
    inventories: null,
    registry: null,
    baseline: null,
    tokenSnapshot: { report: null, registry: null }
  }
  if (mutate) mutate(state)
  const design = normalizeCapture(state.capture, COMPONENT_CAPTURE_HASH)
  const inventories = state.inventories || [state.project]
  const registry = state.registry || validComponentRegistry(design.scopeId)
  const index = validComponentAnalysisIndex(inventories, projectInventorySemanticHash)
  return {
    design, registry, inventories, state,
    run: () => compareComponents({
      designInventory: design,
      projectInventories: inventories,
      analysisIndex: index,
      mappingRegistry: registry,
      baseline: state.baseline,
      tokenSnapshot: state.tokenSnapshot,
      context: comparatorContext()
    })
  }
}
const rowOf = (report, designComponentId) => report.rows.find((row) => row.designComponentId === designComponentId)
const families = (row) => (row.findings || []).map((finding) => finding.family)
const findingOf = (row, family) => (row.findings || []).find((finding) => finding.family === family)
const buttonProject = (state) => state.project.components.find((component) => component.projectComponentId === PROJECT_ID.button)
const buttonEntity = (state) => state.capture.entities.find((entity) => entity.nodeId === '10:1')
const dropBadgeMapping = (registry) => { registry.mappings = registry.mappings.filter((mapping) => mapping.designComponentId !== DESIGN_ID.badge) }
const buttonBaseline = () => publishableBaseline(world().run().baselineCandidate, null, '2026-07-20T12:00:00.000Z')

// ── matched happy path ─────────────────────────────────────────────────────
check('CMP-CMP-STATUS: fully bound mappings over a complete scan read matched', () => {
  const { report, baselineCandidate } = world().run()
  assert.equal(rowOf(report, DESIGN_ID.button).status, 'matched')
  assert.equal(rowOf(report, DESIGN_ID.icon).status, 'matched')
  assert.equal(rowOf(report, DESIGN_ID.badge).status, 'matched')
  assert.equal(rowOf(report, DESIGN_ID.button).mappingState, 'active')
  assert.deepEqual(rowOf(report, DESIGN_ID.button).platforms.map((platform) => `${platform.platform}:${platform.state}`), ['android-compose:matched'])
  assert.deepEqual(
    baselineCandidate.entries.map((entry) => `${entry.mappingId}:${entry.status}`),
    ['cmap-' + '1'.repeat(24) + ':matched', 'cmap-' + '2'.repeat(24) + ':matched', 'cmap-' + '3'.repeat(24) + ':matched']
  )
  const buttonEntry = baselineCandidate.entries.find((entry) => entry.designComponentId === DESIGN_ID.button)
  assert.deepEqual(buttonEntry.designVariantIds, ['10:2', '10:3', '10:4'])
  assert.match(buttonEntry.designVisualHash, /^sha256:[a-f0-9]{64}$/)
})

check('CMP-CMP-STATUS: design-side unsupported entity reads unsupported with its reason', () => {
  const row = rowOf(world().run().report, DESIGN_ID.remote)
  assert.equal(row.status, 'unsupported')
  assert.equal(row.kind, 'unsupported')
  assert.match(row.statusDetail, /referenced-only/)
})

check('CMP-CMP-VISUAL: an all-external mapping is explicitly not-applicable, never matched or insufficient-evidence', () => {
  const w = world()
  const mapping = w.registry.mappings.find((entry) => entry.designComponentId === DESIGN_ID.icon)
  Object.assign(mapping.implementations[0], {
    relation: 'external',
    projectComponentIds: [],
    externalRef: 'external-library/Icon'
  })
  const row = rowOf(w.run().report, DESIGN_ID.icon)
  assert.equal(row.status, 'matched', 'external ownership does not invent semantic drift')
  assert.equal(row.dimensions.visual, 'not-applicable')
  assert.deepEqual(row.platforms.map((platform) => platform.state), ['external'])
})

check('CMP-CMP-COVERAGE: counters reconcile — unsupported counted, project-only surfaced', () => {
  const report = world().run().report
  assert.deepEqual(report.coverage, {
    designComponents: 4,
    ignoredDesignComponents: 0,
    denominator: 4,
    matched: 3,
    drifted: 0,
    unmapped: 0,
    ambiguous: 0,
    missingInProject: 0,
    missingInDesign: 0,
    designOnly: 0,
    unsupported: 1,
    projectOnly: 1
  })
  const spare = report.projectOnly.find((entry) => entry.projectComponentId === PROJECT_ID.spare)
  assert.equal(spare.classification, 'unclassified')
})

// ── unmapped + suggestions ─────────────────────────────────────────────────
check('CMP-SUG: an unmapped component surfaces one strong qualified-name candidate', () => {
  const w = world()
  dropBadgeMapping(w.registry)
  const { report, suggestions } = w.run()
  const row = rowOf(report, DESIGN_ID.badge)
  assert.equal(row.status, 'unmapped')
  assert.equal(row.suggestionsAmbiguous, false)
  assert.equal(row.suggestionsCount, 1)
  const entry = suggestions.byDesignComponent.find((item) => item.designComponentId === DESIGN_ID.badge)
  assert.equal(entry.candidates[0].projectComponentId, PROJECT_ID.badge)
  assert.equal(entry.candidates[0].band, 'strong')
  assert.ok(entry.candidates[0].signals.some((signal) => signal.kind === 'qualified-name'))
  assert.equal(entry.candidates[0].autoConfirmForbidden, true)
})

check('CMP-SUG-AMBIG: two strong candidates -> status ambiguous', () => {
  const w = world((state) => {
    const clone = JSON.parse(JSON.stringify(state.project.components.find((component) => component.projectComponentId === PROJECT_ID.badge)))
    clone.projectComponentId = `${ADAPTER_ID}:symbol:com.example.ui.MyBadge`
    clone.name = 'MyBadge'
    clone.fqName = 'com.example.ui.MyBadge'
    clone.source = { ...clone.source, symbol: 'com.example.ui.MyBadge' }
    state.project.components.push(clone)
    finishProjectComponentInventory(state.project)
  })
  dropBadgeMapping(w.registry)
  const row = rowOf(w.run().report, DESIGN_ID.badge)
  assert.equal(row.status, 'ambiguous')
  assert.equal(row.suggestionsAmbiguous, true)
})

// ── dispositions ───────────────────────────────────────────────────────────
check('CMP-CMP-STATUS: an intentionally-design-only disposition reads design-only and leaves the denominator', () => {
  const w = world()
  dropBadgeMapping(w.registry)
  w.registry.dispositions.push({
    dispositionId: 'dcp-' + '1'.repeat(24),
    target: { side: 'design', designComponentId: DESIGN_ID.badge },
    kind: 'intentionally-design-only', reason: 'design-only pattern', owner: 'owner', createdAt: '2026-07-20T11:00:00.000Z'
  })
  const report = w.run().report
  const row = rowOf(report, DESIGN_ID.badge)
  assert.equal(row.status, 'design-only')
  assert.equal(row.statusDetail, 'intentionally-design-only')
  assert.equal(row.dispositionId, 'dcp-' + '1'.repeat(24))
  assert.equal(report.coverage.ignoredDesignComponents, 1)
  assert.equal(report.coverage.denominator, 3)
})

check('CMP-CMP-STATUS: an ignored disposition reads ignored with its dispositionId', () => {
  const w = world()
  dropBadgeMapping(w.registry)
  w.registry.dispositions.push({
    dispositionId: 'dcp-' + '2'.repeat(24),
    target: { side: 'design', designComponentId: DESIGN_ID.badge },
    kind: 'ignored', reason: 'out of comparison', owner: 'owner', createdAt: '2026-07-20T11:00:00.000Z'
  })
  const row = rowOf(w.run().report, DESIGN_ID.badge)
  assert.equal(row.status, 'ignored')
  assert.equal(row.dispositionId, 'dcp-' + '2'.repeat(24))
})

check('CMP-CMP-PROJECT-ONLY: a project disposition classifies instead of hiding', () => {
  const w = world()
  w.registry.dispositions.push({
    dispositionId: 'dcp-' + '3'.repeat(24),
    target: { side: 'project', projectComponentId: PROJECT_ID.spare, adapterId: ADAPTER_ID },
    kind: 'intentionally-project-only', reason: 'internal helper', owner: 'owner', createdAt: '2026-07-20T11:00:00.000Z'
  })
  const entry = w.run().report.projectOnly.find((item) => item.projectComponentId === PROJECT_ID.spare)
  assert.equal(entry.classification, 'intentionally-project-only')
  assert.equal(entry.dispositionId, 'dcp-' + '3'.repeat(24))
})

// ── absence, scope, kind ───────────────────────────────────────────────────
check('CMP-ABS-PROJECT: implementation absent from a COMPLETE scan -> missing-in-project', () => {
  const w = world((state) => {
    state.project.components = state.project.components.filter((component) => component.projectComponentId !== PROJECT_ID.button)
    state.project.witness.filesMatched = 3
    state.project.witness.filesParsed = 3
    finishProjectComponentInventory(state.project)
  })
  const row = rowOf(w.run().report, DESIGN_ID.button)
  assert.equal(row.status, 'missing-in-project')
  assert.equal(row.mappingState, 'orphaned-project')
})

check('CMP-ABS-DESIGN: mapped design component gone + absence proof eligible -> missing-in-design', () => {
  const w = world((state) => {
    state.capture.entities = state.capture.entities.filter((entity) => entity.nodeId !== '10:1')
    state.capture.visual = state.capture.visual.filter((entry) => entry.entityNodeId !== '10:1')
    state.capture.witness.expectedEntityCount = 3
    state.capture.witness.readEntityCount = 3
  })
  const row = rowOf(w.run().report, DESIGN_ID.button)
  assert.equal(row.status, 'missing-in-design')
  assert.equal(row.mappingState, 'orphaned-design')
  assert.equal(row.change.changedSide, 'design')
})

check('CMP-ABS-DESIGN: absence proof NOT eligible -> blocker COMPONENT_DESIGN_ABSENCE_UNPROVEN, no row', () => {
  const w = world((state) => {
    state.capture.entities = state.capture.entities.filter((entity) => entity.nodeId !== '10:1')
    state.capture.visual = state.capture.visual.filter((entry) => entry.entityNodeId !== '10:1')
    state.capture.witness.expectedEntityCount = 3
    state.capture.witness.readEntityCount = 3
    delete state.capture.witness.providerRevisionBefore
    delete state.capture.witness.providerRevisionAfter
    state.capture.witness.consistency = 'unproven'
  })
  const report = w.run().report
  assert.equal(rowOf(report, DESIGN_ID.button), undefined)
  assert.ok(report.blockers.some((blocker) => blocker.code === 'COMPONENT_DESIGN_ABSENCE_UNPROVEN'))
})

check('CMP-CMP-SCOPE: mapping confirmed against another adapter scope -> unmapped + target-out-of-scope', () => {
  const w = world((state) => { state.project.scopeFingerprint = 'sha256:' + '7'.repeat(64) })
  const row = rowOf(w.run().report, DESIGN_ID.button)
  assert.equal(row.status, 'unmapped')
  assert.equal(row.mappingState, 'target-out-of-scope')
})

check('CMP-CMP-SCOPE: registry bound to another design scope -> everything unmapped + blocker, no baseline', () => {
  const w = world((state) => {
    state.registry = validComponentRegistry('figma:file:' + '0'.repeat(16) + ':branch:none:components:' + '0'.repeat(16))
  })
  const { report, baselineCandidate } = w.run()
  assert.equal(rowOf(report, DESIGN_ID.button).status, 'unmapped')
  assert.ok(report.blockers.some((blocker) => blocker.code === 'COMPONENT_DESIGN_SCOPE_CHANGED'))
  assert.equal(baselineCandidate, null)
})

check('CMP-CMP-KIND: a design kind change under a mapping is blocking + unsupported', () => {
  const w = world()
  w.registry.mappings[0].expectedKind = 'component'
  const row = rowOf(w.run().report, DESIGN_ID.button)
  assert.equal(row.status, 'unsupported')
  assert.equal(row.mappingState, 'incompatible')
  assert.equal(findingOf(row, 'design-kind-changed').severity, 'blocking')
})

// ── property/value drift families ──────────────────────────────────────────
check('CMP-CMP-VALUE: a mapped project value gone from the project -> value-removed breaking -> drifted', () => {
  const w = world((state) => {
    const size = buttonProject(state).variantProperties.find((property) => property.projectPropertyId === 'param:size')
    size.values = [{ value: 'Small' }]
  })
  const row = rowOf(w.run().report, DESIGN_ID.button)
  assert.equal(row.status, 'drifted')
  assert.equal(findingOf(row, 'value-removed').severity, 'breaking')
})

check('CMP-CMP-VALUE: a design value without a confirmed binding -> value-unmapped -> status ambiguous', () => {
  const w = world()
  delete w.registry.mappings[0].propertyMappings[0].valueMap.Large
  const row = rowOf(w.run().report, DESIGN_ID.button)
  assert.equal(row.status, 'ambiguous', 'binding gaps outrank drift — nothing may claim matched')
  assert.ok(families(row).includes('value-unmapped'))
})

check('CMP-CMP-PROP: an unbound design property -> property-unmapped -> status ambiguous', () => {
  const w = world()
  w.registry.mappings[0].propertyMappings = w.registry.mappings[0].propertyMappings.filter((mapping) => mapping.designPropertyId !== 'p-tone')
  const row = rowOf(w.run().report, DESIGN_ID.button)
  assert.equal(row.status, 'ambiguous')
  assert.ok(families(row).includes('property-unmapped'))
})

check('CMP-CMP-SLOT: an unbound design content slot -> slot-unmapped -> status ambiguous', () => {
  const w = world()
  w.registry.mappings[0].slotMappings = w.registry.mappings[0].slotMappings.filter((mapping) => mapping.designSlotId !== 'prop:p-icon')
  const row = rowOf(w.run().report, DESIGN_ID.button)
  assert.equal(row.status, 'ambiguous')
  assert.ok(families(row).includes('slot-unmapped'))
})

check('CMP-CMP-DEFAULT: a default disagreement is behavioral drift', () => {
  const w = world((state) => {
    buttonProject(state).variantProperties.find((property) => property.projectPropertyId === 'param:size').defaultValue = 'Large'
  })
  const row = rowOf(w.run().report, DESIGN_ID.button)
  assert.equal(row.status, 'drifted')
  assert.equal(findingOf(row, 'default-changed').severity, 'behavioral')
})

check('CMP-CMP-API: a bound project property gone -> project-api-removed breaking', () => {
  const w = world((state) => {
    const button = buttonProject(state)
    button.variantProperties = button.variantProperties.filter((property) => property.projectPropertyId !== 'param:size')
    finishProjectComponentInventory(state.project)
  })
  const row = rowOf(w.run().report, DESIGN_ID.button)
  assert.equal(row.status, 'drifted')
  assert.equal(findingOf(row, 'project-api-removed').severity, 'breaking')
})

check('CMP-CMP-PROP: a bound design property gone from the design side -> property-removed breaking', () => {
  const w = world((state) => {
    buttonEntity(state).properties = buttonEntity(state).properties.filter((property) => property.propertyId !== 'p-disabled')
  })
  const row = rowOf(w.run().report, DESIGN_ID.button)
  assert.equal(row.status, 'drifted')
  assert.equal(findingOf(row, 'property-removed').severity, 'breaking')
})

check('CMP-CMP-SLOT: a bound project slot gone -> slot-removed breaking', () => {
  const w = world((state) => {
    const button = buttonProject(state)
    button.slots = button.slots.filter((slot) => slot.slotId !== 'param:label')
    button.api.parameters = button.api.parameters.filter((parameter) => parameter.name !== 'label')
    finishProjectComponentInventory(state.project)
  })
  const row = rowOf(w.run().report, DESIGN_ID.button)
  assert.equal(row.status, 'drifted')
  assert.equal(findingOf(row, 'slot-removed').severity, 'breaking')
})

// ── sparse variants ────────────────────────────────────────────────────────
check('CMP-CMP-VARIANTS: expressible counts the provider tuples only — no Cartesian invention', () => {
  const row = rowOf(world().run().report, DESIGN_ID.button)
  // Size×Tone would be 4 combinations; the provider declares 3. The absent
  // Small+Danger tuple must never appear in any denominator.
  assert.deepEqual(row.dimensions.variants, { designTotal: 3, expressible: 3, provable: true })
})

check('CMP-CMP-VARIANTS: combinationsKnown=unknown -> variant-combination-unprovable, coverage never claimed', () => {
  const w = world((state) => { buttonProject(state).combinationsKnown = 'unknown' })
  const row = rowOf(w.run().report, DESIGN_ID.button)
  assert.equal(row.status, 'drifted')
  assert.ok(families(row).includes('variant-combination-unprovable'))
  assert.equal(row.dimensions.variants.provable, false)
})

check('CMP-CMP-VARIANTS: an unexpressible tuple lowers expressible without inventing findings per value', () => {
  const w = world((state) => {
    const tone = buttonProject(state).variantProperties.find((property) => property.projectPropertyId === 'param:tone')
    tone.values = [{ value: 'Neutral' }]
  })
  const row = rowOf(w.run().report, DESIGN_ID.button)
  // Danger is gone project-side: the 10:4 tuple is no longer expressible.
  assert.equal(row.dimensions.variants.expressible, 2)
  assert.equal(row.status, 'drifted')
})

// ── platform coverage ──────────────────────────────────────────────────────
check('CMP-CMP-PLATFORM: a second required platform absent from its complete scan -> drifted + missing state', () => {
  const w = world((state) => { state.inventories = [state.project, emptyManifestInventory()] })
  w.registry.mappings[0].implementations.push({
    adapterId: 'manifest-ds', platform: 'web', projectScopeFingerprint: 'sha256:' + '9'.repeat(64),
    relation: 'direct', projectComponentIds: ['manifest-ds:symbol:ui/button'], required: true
  })
  const row = rowOf(w.run().report, DESIGN_ID.button)
  assert.equal(row.status, 'drifted')
  assert.deepEqual(row.platforms.map((platform) => `${platform.platform}:${platform.state}`), ['android-compose:matched', 'web:missing'])
  assert.equal(findingOf(row, 'platform-implementation-missing').severity, 'breaking')
})

// ── baseline direction ─────────────────────────────────────────────────────
check('CMP-DIR: unchanged worlds vs baseline -> changedSide none, confidence exact', () => {
  const baseline = buttonBaseline()
  const w = world((state) => { state.baseline = baseline })
  const row = rowOf(w.run().report, DESIGN_ID.button)
  assert.deepEqual(row.change, { changedSide: 'none', designChanged: false, projectChanged: false, mappingChanged: false, confidence: 'exact' })
})

check('CMP-DIR: a design-side structural change -> changedSide design', () => {
  const baseline = buttonBaseline()
  const w = world((state) => {
    state.baseline = baseline
    buttonEntity(state).defaultVariantNodeId = '10:3'
    buttonEntity(state).variants[0].isDefault = false
    buttonEntity(state).variants[1].isDefault = true
  })
  const row = rowOf(w.run().report, DESIGN_ID.button)
  assert.deepEqual([row.change.changedSide, row.change.designChanged, row.change.projectChanged], ['design', true, false])
})

check('CMP-DIR: a project-side API change -> changedSide project', () => {
  const baseline = buttonBaseline()
  const w = world((state) => {
    state.baseline = baseline
    buttonProject(state).api.parameters.push({ name: 'elevation', kind: 'value', typeText: 'Dp', required: false, hasDefault: true, defaultText: '0.dp' })
  })
  const row = rowOf(w.run().report, DESIGN_ID.button)
  assert.equal(row.change.changedSide, 'project')
})

check('CMP-DIR: a mapping-only change -> changedSide mapping + mapping-policy-changed lifecycle', () => {
  const baseline = buttonBaseline()
  const w = world((state) => { state.baseline = baseline })
  w.registry.mappings[0].propertyMappings[0].note = 'reviewed'
  const row = rowOf(w.run().report, DESIGN_ID.button)
  assert.equal(row.change.changedSide, 'mapping')
  assert.ok(families(row).includes('mapping-policy-changed'))
  assert.equal(row.status, 'matched')
})

check('CMP-DIR: both sides changed -> changedSide both, no side wins', () => {
  const baseline = buttonBaseline()
  const w = world((state) => {
    state.baseline = baseline
    buttonEntity(state).defaultVariantNodeId = '10:3'
    buttonEntity(state).variants[0].isDefault = false
    buttonEntity(state).variants[1].isDefault = true
    buttonProject(state).api.parameters.push({ name: 'elevation', kind: 'value', typeText: 'Dp', required: false, hasDefault: true, defaultText: '0.dp' })
  })
  assert.equal(rowOf(w.run().report, DESIGN_ID.button).change.changedSide, 'both')
})

check('CMP-DIR: a new allowed variant -> allowed-variant-added additive drift', () => {
  const baseline = buttonBaseline()
  const w = world((state) => {
    state.baseline = baseline
    buttonEntity(state).variants.push({ nodeId: '10:5', name: 'Size=Small, Tone=Danger', assignments: { 'p-size': 'Small', 'p-tone': 'Danger' } })
    buttonEntity(state).expectedVariantCount = 4
  })
  const row = rowOf(w.run().report, DESIGN_ID.button)
  assert.equal(row.status, 'drifted')
  assert.equal(findingOf(row, 'allowed-variant-added').severity, 'additive')
})

check('CMP-DIR: a removed allowed variant -> allowed-variant-removed behavioral drift', () => {
  const baseline = buttonBaseline()
  const w = world((state) => {
    state.baseline = baseline
    buttonEntity(state).variants = buttonEntity(state).variants.filter((variant) => variant.nodeId !== '10:4')
    buttonEntity(state).expectedVariantCount = 2
    state.capture.visual = state.capture.visual.filter((entry) => entry.variantNodeId !== '10:4')
  })
  const row = rowOf(w.run().report, DESIGN_ID.button)
  assert.equal(row.status, 'drifted')
  assert.equal(findingOf(row, 'allowed-variant-removed').severity, 'behavioral')
})

check('CMP-DIR: a rename keeps the stable id matched with a design-renamed lifecycle finding', () => {
  const baseline = buttonBaseline()
  const w = world((state) => { state.baseline = baseline; buttonEntity(state).name = 'ButtonX' })
  const row = rowOf(w.run().report, DESIGN_ID.button)
  assert.equal(row.status, 'matched')
  assert.equal(row.change.changedSide, 'none')
  assert.ok(families(row).includes('design-renamed'))
})

check('CMP-DIR: a page move keeps the stable id matched with a design-moved lifecycle finding', () => {
  // Both worlds must request the same two-page scope: widening the scope would
  // change the scopeId and (correctly) detach the baseline.
  const withArchivePage = (state) => {
    state.capture.pages.push({ pageId: '0:2', name: 'Archive' })
    state.capture.scope.pageIds = ['0:1', '0:2']
    state.capture.witness.requestedPageIds = ['0:1', '0:2']
    state.capture.witness.readPageIds = ['0:1', '0:2']
  }
  const base = world(withArchivePage)
  const baseline = publishableBaseline(base.run().baselineCandidate, null, '2026-07-20T12:00:00.000Z')
  const w = world((state) => {
    withArchivePage(state)
    state.baseline = baseline
    buttonEntity(state).pageId = '0:2'
  })
  const row = rowOf(w.run().report, DESIGN_ID.button)
  assert.equal(row.status, 'matched')
  assert.ok(families(row).includes('design-moved'))
})

check('CMP-DIR: visual evidence drift stays on the visual axis and does not downgrade semantic/API match', () => {
  const baseline = buttonBaseline()
  const w = world((state) => {
    state.baseline = baseline
    state.capture.visual[0].sha256 = 'sha256:' + 'f'.repeat(64)
  })
  const row = rowOf(w.run().report, DESIGN_ID.button)
  assert.equal(row.status, 'matched')
  assert.equal(findingOf(row, 'visual-evidence-drift').severity, 'review')
  assert.equal(row.dimensions.visual, 'review-required')
  const tasks = suggestComponentTasks(w.run().report)
  assert.equal(tasks.proposals.find((proposal) => proposal.designComponentId === DESIGN_ID.button).intent, 'update-visual')
})

// ── §17.12 token causality ─────────────────────────────────────────────────
check('CMP-17.12: visual drift + unchanged structure + confirmed token change -> suppressed, matched', () => {
  const baseline = buttonBaseline()
  const w = world((state) => {
    state.baseline = baseline
    state.tokenSnapshot = tokenSnapshotFixture()
    state.capture.visual[0].sha256 = 'sha256:' + 'f'.repeat(64)
  })
  const row = rowOf(w.run().report, DESIGN_ID.button)
  const suppression = findingOf(row, 'underlying-token-change')
  assert.ok(suppression, 'the causality proof must be recorded as a finding')
  assert.equal(suppression.suppressesTask, true)
  assert.equal(suppression.severity, 'info')
  assert.equal(row.status, 'matched', 'a causally explained visual change is not component drift')
})

check('CMP-17.12: the suppressed row produces NO update task proposal', () => {
  const baseline = buttonBaseline()
  const w = world((state) => {
    state.baseline = baseline
    state.tokenSnapshot = tokenSnapshotFixture()
    state.capture.visual[0].sha256 = 'sha256:' + 'f'.repeat(64)
  })
  const tasks = suggestComponentTasks(w.run().report)
  assert.deepEqual(tasks.proposals.filter((proposal) => proposal.designComponentId === DESIGN_ID.button), [])
})

check('CMP-17.12: any review+ finding besides the visual drift defeats the proof -> NOT suppressed', () => {
  const baseline = buttonBaseline()
  const w = world((state) => {
    state.baseline = baseline
    state.tokenSnapshot = tokenSnapshotFixture()
    state.capture.visual[0].sha256 = 'sha256:' + 'f'.repeat(64)
    const size = buttonProject(state).variantProperties.find((property) => property.projectPropertyId === 'param:size')
    size.values = [{ value: 'Small' }]
  })
  const report = w.run().report
  const row = rowOf(report, DESIGN_ID.button)
  assert.equal(findingOf(row, 'underlying-token-change'), undefined)
  assert.equal(row.status, 'drifted')
  const tasks = suggestComponentTasks(report)
  assert.deepEqual(tasks.proposals.filter((proposal) => proposal.designComponentId === DESIGN_ID.button).map((proposal) => proposal.intent), ['update-api'])
})

check('CMP-17.12: a changed bound token WITHOUT a confirmed token mapping never suppresses', () => {
  const baseline = buttonBaseline()
  const w = world((state) => {
    state.baseline = baseline
    const snapshot = tokenSnapshotFixture()
    snapshot.bindingSnapshot.bindings = []
    state.tokenSnapshot = snapshot
    state.capture.visual[0].sha256 = 'sha256:' + 'f'.repeat(64)
  })
  const row = rowOf(w.run().report, DESIGN_ID.button)
  assert.equal(findingOf(row, 'underlying-token-change'), undefined)
  assert.equal(row.status, 'matched')
  assert.equal(row.dimensions.visual, 'review-required')
  assert.deepEqual(suggestComponentTasks(w.run().report).proposals
    .filter((proposal) => proposal.designComponentId === DESIGN_ID.button)
    .map((proposal) => proposal.intent), ['update-visual'])
})

check('CMP-17.12: unknown token freshness never suppresses independent component drift', () => {
  const baseline = buttonBaseline()
  const w = world((state) => {
    state.baseline = baseline
    const snapshot = tokenSnapshotFixture()
    snapshot.report.inputs.sourceFreshness = 'unknown'
    snapshot.report.operationalState = 'stale'
    snapshot.report.complete = false
    snapshot.report.blockers = [{
      code: 'TOKEN_SOURCE_HEALTH_UNAVAILABLE',
      detail: 'source freshness cannot be proven'
    }]
    state.tokenSnapshot = snapshot
    state.capture.visual[0].sha256 = 'sha256:' + 'f'.repeat(64)
  })
  const row = rowOf(w.run().report, DESIGN_ID.button)
  assert.equal(findingOf(row, 'underlying-token-change'), undefined)
  assert.equal(row.dimensions.visual, 'review-required')
  assert.deepEqual(suggestComponentTasks(w.run().report).proposals
    .filter((proposal) => proposal.designComponentId === DESIGN_ID.button)
    .map((proposal) => proposal.intent), ['update-visual'])
})

// ── failure modes ──────────────────────────────────────────────────────────
check('CMP-CMP-INCOMPLETE: an incomplete project inventory blocks the whole comparison (typed)', () => {
  const w = world((state) => {
    state.project.witness.parseFailures = [{ path: 'design-system/src/AppButton.kt', reason: 'parse error' }]
    state.project.witness.filesParsed = 3
    state.project.witness.complete = false
  })
  assert.throws(w.run, (error) => error.code === 'COMPONENT_PROJECT_INVENTORY_INCOMPLETE')
})

check('CMP-CMP-TRUNC: rows over the comparison limit are a typed failure, never a partial report', () => {
  const w = world()
  w.design.components = new Array(COMPONENT_LIMITS.comparisonRowsMax + 1).fill(w.design.components[0])
  assert.throws(w.run, (error) => error.code === 'COMPONENT_COMPARISON_LIMIT_EXCEEDED')
})

check('CMP-CMP-NOOP: identical inputs reproduce a byte-identical semantic hash', () => {
  const first = world().run().report
  const second = world().run().report
  assert.equal(first.semanticHash, second.semanticHash)
  assert.equal(JSON.stringify(first.rows), JSON.stringify(second.rows))
})

// ── task suggestions ───────────────────────────────────────────────────────
check('CMP-TASK: an unmapped component proposes implement; ambiguity proposes reconcile-mapping', () => {
  const w = world()
  dropBadgeMapping(w.registry)
  const tasks = suggestComponentTasks(w.run().report)
  assert.deepEqual(tasks.proposals.filter((proposal) => proposal.designComponentId === DESIGN_ID.badge).map((proposal) => proposal.intent), ['implement'])

  const ambiguous = world((state) => {
    const clone = JSON.parse(JSON.stringify(state.project.components.find((component) => component.projectComponentId === PROJECT_ID.badge)))
    clone.projectComponentId = `${ADAPTER_ID}:symbol:com.example.ui.MyBadge`
    clone.name = 'MyBadge'
    clone.fqName = 'com.example.ui.MyBadge'
    clone.source = { ...clone.source, symbol: 'com.example.ui.MyBadge' }
    state.project.components.push(clone)
    finishProjectComponentInventory(state.project)
  })
  dropBadgeMapping(ambiguous.registry)
  const ambiguousTasks = suggestComponentTasks(ambiguous.run().report)
  assert.deepEqual(ambiguousTasks.proposals.filter((proposal) => proposal.designComponentId === DESIGN_ID.badge).map((proposal) => proposal.intent), ['reconcile-mapping'])
})

check('CMP-TASK: a missing required platform proposes add-platform for that exact platform', () => {
  const w = world((state) => { state.inventories = [state.project, emptyManifestInventory()] })
  w.registry.mappings[0].implementations.push({
    adapterId: 'manifest-ds', platform: 'web', projectScopeFingerprint: 'sha256:' + '9'.repeat(64),
    relation: 'direct', projectComponentIds: ['manifest-ds:symbol:ui/button'], required: true
  })
  const tasks = suggestComponentTasks(w.run().report)
  const proposals = tasks.proposals.filter((proposal) => proposal.designComponentId === DESIGN_ID.button)
  assert.deepEqual(proposals.map((proposal) => `${proposal.intent}:${proposal.platform}`), ['add-platform:web'])
  assert.equal(proposals[0].severity, 'breaking')
})

check('CMP-TASK: visual-only drift routes to update-visual; API drift routes to update-api', () => {
  const baseline = buttonBaseline()
  const visual = world((state) => { state.baseline = baseline; state.capture.visual[0].sha256 = 'sha256:' + 'f'.repeat(64) })
  const visualTasks = suggestComponentTasks(visual.run().report)
  assert.deepEqual(visualTasks.proposals.filter((proposal) => proposal.designComponentId === DESIGN_ID.button).map((proposal) => proposal.intent), ['update-visual'])

  const api = world((state) => {
    const size = buttonProject(state).variantProperties.find((property) => property.projectPropertyId === 'param:size')
    size.values = [{ value: 'Small' }]
  })
  const apiTasks = suggestComponentTasks(api.run().report)
  assert.deepEqual(apiTasks.proposals.filter((proposal) => proposal.designComponentId === DESIGN_ID.button).map((proposal) => proposal.intent), ['update-api'])
})

check('CMP-TASK: a proven-absent design component proposes remap (code is never deleted automatically)', () => {
  const w = world((state) => {
    state.capture.entities = state.capture.entities.filter((entity) => entity.nodeId !== '10:1')
    state.capture.visual = state.capture.visual.filter((entry) => entry.entityNodeId !== '10:1')
    state.capture.witness.expectedEntityCount = 3
    state.capture.witness.readEntityCount = 3
  })
  const tasks = suggestComponentTasks(w.run().report)
  const proposals = tasks.proposals.filter((proposal) => proposal.designComponentId === DESIGN_ID.button)
  assert.deepEqual(proposals.map((proposal) => proposal.intent), ['remap'])
  assert.equal(proposals[0].mappingId, 'cmap-' + '1'.repeat(24))
})

check('CMP-TASK: unclassified project-only components propose classify-project-only', () => {
  const tasks = suggestComponentTasks(world().run().report)
  const proposals = tasks.proposals.filter((proposal) => proposal.projectComponentId === PROJECT_ID.spare)
  assert.deepEqual(proposals.map((proposal) => proposal.intent), ['classify-project-only'])
})

check('CMP-TASK-DEDUP: identical worlds produce identical dedup keys and proposal ids', () => {
  const build = () => {
    const w = world()
    dropBadgeMapping(w.registry)
    return suggestComponentTasks(w.run().report)
  }
  const first = build()
  const second = build()
  assert.equal(JSON.stringify(first), JSON.stringify(second))
  for (const proposal of first.proposals) {
    assert.match(proposal.dedupKey, /^sha256:[a-f0-9]{64}$/)
    assert.match(proposal.proposalId, /^cmpt-[a-f0-9]{24}$/)
  }
})

console.log(`\ncomponent-comparator.test: ${pass} pass, ${fail} fail`)
process.exit(fail === 0 ? 0 : 1)
