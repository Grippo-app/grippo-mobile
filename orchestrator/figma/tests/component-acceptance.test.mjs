#!/usr/bin/env node
// Template-owned component integration acceptance (FIGMA_COMPONENTS.md §33).
// Drives the REAL domain modules end-to-end over isolated fixture workspaces:
// capture → normalize → adapter extraction → mapping registry → comparator →
// baseline → task suggestions. Proves the §33.4 lifecycle scenarios and the
// §33.5 additional proof projects (alternate Compose structure, non-Compose
// manifest adapter, multi-platform mapping) without any external consumer
// repository. Scenario 11 (design-origin task publishes its mapping through
// finalization) is proven by orchestrator/tasks/tests/test-finalize-task.mjs
// (components block); scenario 12 (server restart) by the site-level
// design-component restart proof — both referenced, not duplicated here.
import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { tmpdir } from 'node:os'

import { normalizeCapture } from '../components/capture-normalizer.mjs'
import { compareComponents } from '../components/comparator.mjs'
import { publishableBaseline } from '../components/baseline.mjs'
import { suggestComponentTasks } from '../components/task-suggestions.mjs'
import { mappingRegistrySemanticError, emptyMappingRegistry } from '../components/mapping-contract.mjs'
import { projectInventorySemanticHash } from '../components/project-inventory-contract.mjs'
import { loadAdapterConfig } from '../runtime/adapter-config.mjs'
import { extractProjectComponents } from '../runtime/component-extraction.mjs'

let checks = 0
function check(name, fn) {
  return Promise.resolve().then(fn).then(() => { checks++; console.log('PASS ' + name) }, (error) => {
    error.message = name + ': ' + error.message
    throw error
  })
}
const sha = (value) => 'sha256:' + createHash('sha256').update(value).digest('hex')
const OBSERVED_TOKEN_KEY = 'otk:sha256:' + '1'.repeat(64)
const OBSERVED_CONTEXT_KEY = 'theme=light|locale=und|platform=web'
const OBSERVED_SOURCE_ID = 'otsrc:sha256:' + '2'.repeat(64)

// ── design capture fixture (same shape the sync session stages) ─────────────
function captureFixture() {
  return {
    schemaVersion: 2,
    provider: 'figma',
    providerIdentity: {
      fileKeyFingerprint: 'sha256:' + 'a'.repeat(64),
      branchKey: 'none',
      libraryOriginPolicy: 'local-authoritative',
    },
    scope: { kind: 'all-pages' },
    pages: [{ pageId: '1:1', name: 'Components' }],
    entities: [
      {
        nodeId: '10:1', pageId: '1:1', kind: 'component-set', name: 'AppButton', idQuality: 'stable',
        properties: [
          { propertyId: 'p:size', name: 'Size', type: 'variant', idQuality: 'stable', options: ['Small', 'Large'], defaultValue: 'Small' },
          { propertyId: 'p:enabled', name: 'Enabled', type: 'boolean', idQuality: 'stable', defaultValue: true },
          { propertyId: 'p:label', name: 'Label', type: 'text', idQuality: 'stable' },
          { propertyId: 'p:icon', name: 'LeadingIcon', type: 'instance-swap', idQuality: 'stable', swapTargets: [{ targetNodeId: '20:1' }] },
        ],
        variants: [
          { nodeId: '10:2', name: 'Size=Small', assignments: { 'p:size': 'Small' }, isDefault: true },
          { nodeId: '10:3', name: 'Size=Large', assignments: { 'p:size': 'Large' } },
        ],
        expectedVariantCount: 2,
        nestedRefs: [{ targetNodeId: '20:1', swappable: true, viaPropertyId: 'p:icon' }],
        boundVariables: [{
          observedTokenKey: OBSERVED_TOKEN_KEY,
          contextKey: OBSERVED_CONTEXT_KEY,
          sourceId: OBSERVED_SOURCE_ID,
          providerName: 'AppButton/Background',
          field: 'fills'
        }],
      },
      {
        nodeId: '20:1', pageId: '1:1', kind: 'component', name: 'AppIcon', idQuality: 'stable',
        properties: [], variants: [], expectedVariantCount: 0, nestedRefs: [], boundVariables: [],
      },
    ],
    visual: [{ entityNodeId: '10:1', variantNodeId: '10:2', role: 'default', file: 'visual/button.png', sha256: 'sha256:' + '1'.repeat(64) }],
    witness: {
      startedAt: '2026-01-01T00:00:00.000Z', finishedAt: '2026-01-01T00:00:05.000Z',
      providerRevisionBefore: 'r1', providerRevisionAfter: 'r1',
      consistency: 'proven', completeness: 'complete',
      requestedPageIds: ['1:1'], readPageIds: ['1:1'],
      expectedEntityCount: 2, readEntityCount: 2,
      truncated: false, permissionDegraded: false, limitsHit: [],
    },
  }
}
function inventoryOf(capture) {
  return normalizeCapture(capture, sha(JSON.stringify(capture)))
}

// ── project fixture workspaces ──────────────────────────────────────────────
const MANIFEST = {
  schemaVersion: 2,
  components: [
    {
      name: 'AppButton', symbol: 'ui/button', visibility: 'public',
      props: [
        { name: 'size', kind: 'enum', values: ['Small', 'Large'], default: 'Small' },
        { name: 'enabled', kind: 'boolean', default: true },
        { name: 'label', kind: 'text', required: true },
        { name: 'leadingIcon', kind: 'content' },
      ],
      uses: { components: ['ui/icon'], tokens: ['fixture-manifest:palette.error.400'] },
      evidence: { screenshotTests: ['src/button.test.tsx'] },
    },
    { name: 'AppIcon', symbol: 'ui/icon', visibility: 'public', props: [] },
  ],
}

function manifestWorkspace(manifest = MANIFEST, manifestRelPath = 'design/components/manifest.json') {
  const root = mkdtempSync(join(tmpdir(), 'component-acceptance-'))
  mkdirSync(join(root, 'orchestrator/figma'), { recursive: true })
  const dir = join(root, manifestRelPath, '..')
  mkdirSync(dir, { recursive: true })
  writeFileSync(join(root, manifestRelPath), JSON.stringify(manifest, null, 2))
  writeFileSync(join(root, 'orchestrator/figma/project-adapters.json'), JSON.stringify({
    schemaVersion: 2,
    adapters: [{
      id: 'fixture-manifest', kind: 'component-manifest', version: 2, enabled: true,
      capabilities: ['components'], platform: 'web', authority: 'handwritten',
      components: { roots: ['design/components'], include: ['**/*.json'], exclude: [], visibility: ['public'] },
    }],
  }, null, 2))
  return root
}

function extractFrom(root) {
  const configState = loadAdapterConfig({ projectRoot: root })
  assert.equal(configState.state, 'configured', 'fixture adapter config must load')
  const result = extractProjectComponents({ projectRoot: root, config: configState.config, configHash: configState.componentConfigHash })
  return { configState, ...result }
}

function analysisIndexOf(inventories, configHash) {
  return {
    schemaVersion: 2,
    configHash,
    adapters: inventories.map((inventory) => ({
      adapterId: inventory.adapterId,
      platform: inventory.platform,
      role: `project-component-inventory:${inventory.adapterId}`,
      inventoryHash: projectInventorySemanticHash(inventory),
      scopeFingerprint: inventory.scopeFingerprint,
      complete: inventory.witness.complete,
    })),
    complete: inventories.every((inventory) => inventory.witness.complete),
  }
}

// A contract-valid active mapping binding the AppButton set to the manifest
// implementation, with complete property + slot coverage (mirrors what the
// finalizer publishes for an implement binding).
function buttonMapping(designInventory, projectComponent, adapterId, platform, scopeFingerprint) {
  const set = designInventory.components.find((component) => component.kind === 'component-set')
  const norm = (value) => String(value).toLowerCase().replace(/[^a-z0-9]/g, '')
  const slotMappings = []
  for (const designSlot of set.semanticSlots) {
    if (designSlot.kind !== 'text-property' && designSlot.kind !== 'instance-swap') continue
    const match = projectComponent.slots.filter((slot) => norm(slot.name) === norm(designSlot.name))
    assert.equal(match.length, 1, `fixture slot pairing must be exact for ${designSlot.name}`)
    slotMappings.push({ designSlotId: designSlot.slotId, adapterId, projectSlotId: match[0].slotId, verification: 'static' })
  }
  return {
    mappingId: 'cmap-' + 'a'.repeat(24),
    designComponentId: set.designComponentId,
    expectedKind: 'component-set',
    implementations: [{
      adapterId, platform, projectScopeFingerprint: scopeFingerprint,
      relation: 'direct', projectComponentIds: [projectComponent.projectComponentId], required: true,
    }],
    propertyMappings: [
      { designPropertyId: 'p:size', adapterId, projectPropertyId: 'param:size', valueMap: { Small: 'Small', Large: 'Large' } },
      { designPropertyId: 'p:enabled', adapterId, projectPropertyId: 'param:enabled', valueMap: { true: 'true', false: 'false' } },
    ],
    slotMappings,
    state: 'active',
    provenance: { kind: 'owner-review', actor: 'acceptance-fixture', at: '2026-01-01T00:00:00.000Z' },
  }
}

// The nested AppIcon dependency must itself be mapped for a clean matched row
// (an unmapped nested dependency is honest review drift by design).
function iconMapping(designInventory, projectComponent, adapterId, platform, scopeFingerprint) {
  const icon = designInventory.components.find((component) => component.kind === 'component')
  return {
    mappingId: 'cmap-' + 'b'.repeat(24),
    designComponentId: icon.designComponentId,
    expectedKind: 'component',
    implementations: [{
      adapterId, platform, projectScopeFingerprint: scopeFingerprint,
      relation: 'direct', projectComponentIds: [projectComponent.projectComponentId], required: true,
    }],
    propertyMappings: [],
    slotMappings: [],
    state: 'active',
    provenance: { kind: 'owner-review', actor: 'acceptance-fixture', at: '2026-01-01T00:00:00.000Z' },
  }
}

function registryWith(designInventory, mappings) {
  const registry = { ...emptyMappingRegistry(designInventory.scopeId), revision: 1, mappings }
  const problem = mappingRegistrySemanticError(registry)
  assert.equal(problem, null, `fixture registry must be contract-valid: ${problem}`)
  return registry
}

function compare(designInventory, extraction, registry, extras = {}) {
  return compareComponents({
    designInventory,
    projectInventories: extraction.inventories,
    analysisIndex: analysisIndexOf(extraction.inventories, extraction.configState.componentConfigHash),
    mappingRegistry: registry,
    baseline: extras.baseline || null,
    tokenSnapshot: extras.tokenSnapshot || { report: null, bindingSnapshot: null },
    context: {
      designGenerationId: 'gen-' + 'f'.repeat(32),
      adapterConfigHash: extraction.configState.componentConfigHash,
      adapterConfigFileHash: extraction.configState.componentConfigFileHash,
    },
  })
}

const rowFor = (report, designComponentId) => report.rows.find((row) => row.designComponentId === designComponentId)

// ── shared fixtures for the manifest scenarios ──────────────────────────────
const root = manifestWorkspace()
const extraction = extractFrom(root)
const button = extraction.inventories[0].components.find((component) => component.fqName === 'ui/button')
const design = inventoryOf(captureFixture())
const set = design.components.find((component) => component.kind === 'component-set')
const scopeFp = extraction.inventories[0].scopeFingerprint
const icon = extraction.inventories[0].components.find((component) => component.fqName === 'ui/icon')
const mapping = buttonMapping(design, button, 'fixture-manifest', 'web', scopeFp)
const registry = registryWith(design, [mapping, iconMapping(design, icon, 'fixture-manifest', 'web', scopeFp)])

try {
  await check('§33.4-3/§33.3: a freshly captured set starts unmapped over an empty registry (no import, no name inference)', function () {
    const { report, suggestions } = compare(design, extraction, emptyMappingRegistry(design.scopeId))
    assert.equal(rowFor(report, set.designComponentId).status, 'unmapped')
    // Suggestions are advisory candidates only — never an authoritative mapping.
    void suggestions
    const proposals = suggestComponentTasks(report).proposals
    assert.ok(proposals.some((task) => task.intent === 'implement' && task.designComponentId === set.designComponentId))
  })

  await check('baseline sanity: a mapped clean run is matched and emits a baseline candidate', function () {
    const { report, baselineCandidate } = compare(design, extraction, registry)
    const row = rowFor(report, set.designComponentId)
    assert.equal(row.status, 'matched', JSON.stringify(row.findings))
    assert.ok(baselineCandidate, 'clean run must emit a baseline candidate')
    const published = publishableBaseline(baselineCandidate, null, '2026-01-01T00:00:00.000Z')
    assert.equal(published.designScopeId, design.scopeId)
    assert.equal(published.entries.length, 2, 'one baseline entry per active mapping (button + nested icon)')
  })

  await check('§33.4-1: renaming the Figma set keeps the mapping (identity is the node id, never the name)', function () {
    const renamed = captureFixture()
    renamed.entities[0].name = 'PrimaryButton'
    const renamedInventory = inventoryOf(renamed)
    const { report, baselineCandidate } = compare(design, extraction, registry)
    const baseline = publishableBaseline(baselineCandidate, null, '2026-01-01T00:00:00.000Z')
    void report
    const second = compare(renamedInventory, extraction, registry, { baseline })
    const row = rowFor(second.report, set.designComponentId)
    assert.notEqual(row.status, 'unmapped', 'rename must not orphan the mapping')
    assert.ok(row.findings.some((finding) => finding.family === 'design-renamed'),
      'the rename is surfaced as an informational finding')
    assert.equal(row.status, 'matched', JSON.stringify(row.findings))
  })

  await check('§33.4-2: delete + recreate under the same name never inherits the mapping', function () {
    const recreated = captureFixture()
    recreated.entities[0].nodeId = '30:1'
    recreated.entities[0].variants = [
      { nodeId: '30:2', name: 'Size=Small', assignments: { 'p:size': 'Small' }, isDefault: true },
      { nodeId: '30:3', name: 'Size=Large', assignments: { 'p:size': 'Large' } },
    ]
    recreated.visual[0].entityNodeId = '30:1'
    recreated.visual[0].variantNodeId = '30:2'
    const recreatedInventory = inventoryOf(recreated)
    const { report } = compare(recreatedInventory, extraction, registry)
    const fresh = recreatedInventory.components.find((component) => component.kind === 'component-set')
    assert.notEqual(fresh.designComponentId, set.designComponentId, 'a recreated node is a new identity')
    assert.equal(rowFor(report, fresh.designComponentId).status, 'unmapped', 'the recreated set must NOT inherit the mapping')
    const orphan = rowFor(report, set.designComponentId)
    assert.ok(orphan, 'the mapped-but-deleted identity still projects a row')
    assert.equal(orphan.status, 'missing-in-design', 'the old mapping surfaces as missing-in-design, never silently dropped')
  })

  await check('§33.4-4: absence is only concluded from a complete capture (truncated capture fails closed)', function () {
    const truncated = captureFixture()
    truncated.witness.truncated = true
    assert.throws(() => inventoryOf(truncated), /COMPONENT_DESIGN_CAPTURE_INCOMPLETE|truncated/)
  })

  await check('§33.4-5: a new project component surfaces as project-only', function () {
    const withExtra = JSON.parse(JSON.stringify(MANIFEST))
    withExtra.components.push({ name: 'AppBadge', symbol: 'ui/badge', visibility: 'public', props: [] })
    const extraRoot = manifestWorkspace(withExtra)
    try {
      const extraExtraction = extractFrom(extraRoot)
      const { report } = compare(design, extraExtraction, registry)
      assert.ok(report.projectOnly.some((entry) => entry.projectComponentId === 'fixture-manifest:symbol:ui/badge'),
        JSON.stringify(report.projectOnly))
    } finally { rmSync(extraRoot, { recursive: true, force: true }) }
  })

  await check('§33.4-6: a moved source file keeps the symbol-derived identity; a renamed symbol surfaces honestly', function () {
    const movedRoot = manifestWorkspace(MANIFEST, 'design/components/moved/relocated.json')
    try {
      const movedExtraction = extractFrom(movedRoot)
      const movedButton = movedExtraction.inventories[0].components.find((component) => component.fqName === 'ui/button')
      assert.equal(movedButton.projectComponentId, button.projectComponentId, 'identity is the symbol, not the file path')
      const { report } = compare(design, movedExtraction, registry)
      assert.equal(rowFor(report, set.designComponentId).status, 'matched')
    } finally { rmSync(movedRoot, { recursive: true, force: true }) }

    const renamed = JSON.parse(JSON.stringify(MANIFEST))
    renamed.components[0].symbol = 'ui/button-v2'
    const renamedRoot = manifestWorkspace(renamed)
    try {
      const renamedExtraction = extractFrom(renamedRoot)
      const { report } = compare(design, renamedExtraction, registry)
      const row = rowFor(report, set.designComponentId)
      assert.equal(row.status, 'missing-in-project', JSON.stringify(row.findings))
      assert.ok(row.findings.some((finding) => finding.family === 'platform-implementation-missing'),
        'the dangling implementation is reported missing, never silently rebound to the renamed symbol')
      assert.ok(report.projectOnly.some((entry) => entry.projectComponentId === 'fixture-manifest:symbol:ui/button-v2'),
        'the renamed symbol surfaces as project-only awaiting an explicit remap decision')
    } finally { rmSync(renamedRoot, { recursive: true, force: true }) }
  })

  await check('§33.4-7: design property/value/default changes surface as exact findings', function () {
    const withOption = captureFixture()
    withOption.entities[0].properties[0].options = ['Small', 'Medium', 'Large']
    const withOptionInventory = inventoryOf(withOption)
    const first = compare(withOptionInventory, extraction, registry)
    const optionRow = rowFor(first.report, set.designComponentId)
    assert.equal(optionRow.status, 'ambiguous', 'an unmapped new design value re-opens the binding')
    assert.ok(optionRow.findings.some((finding) => finding.family === 'value-unmapped'))

    const withDefault = captureFixture()
    withDefault.entities[0].properties[0].defaultValue = 'Large'
    const withDefaultInventory = inventoryOf(withDefault)
    const second = compare(withDefaultInventory, extraction, registry)
    const defaultRow = rowFor(second.report, set.designComponentId)
    assert.ok(defaultRow.findings.some((finding) => finding.family === 'default-changed'), JSON.stringify(defaultRow.findings))
    assert.equal(defaultRow.status, 'drifted')
  })

  await check('§33.4-8: sparse variant tuples never invent missing cells', function () {
    const sparse = captureFixture()
    sparse.entities[0].variants = [{ nodeId: '10:2', name: 'Size=Small', assignments: { 'p:size': 'Small' }, isDefault: true }]
    sparse.entities[0].expectedVariantCount = 1
    const sparseInventory = inventoryOf(sparse)
    const { report } = compare(sparseInventory, extraction, registry)
    const row = rowFor(report, set.designComponentId)
    assert.equal(row.dimensions.variants.designTotal, 1, 'only provider-declared tuples are counted')
    assert.equal(row.dimensions.variants.expressible, 1)
    assert.equal(row.findings.some((finding) => /missing/i.test(finding.family) && finding.family !== 'test-coverage-missing'), false,
      `no cartesian ghost findings: ${JSON.stringify(row.findings.map((finding) => finding.family))}`)
    assert.equal(row.status, 'matched', JSON.stringify(row.findings))
  })

  await check('§33.4-9: a confirmed underlying token change suppresses the component task; without proof it does not', function () {
    const baselineRun = compare(design, extraction, registry)
    const baseline = publishableBaseline(baselineRun.baselineCandidate, null, '2026-01-01T00:00:00.000Z')
    const visualChanged = captureFixture()
    visualChanged.visual[0].sha256 = 'sha256:' + '2'.repeat(64)
    const changedInventory = inventoryOf(visualChanged)
    const bindingHash = 'sha256:' + '8'.repeat(64)
    const tokenSnapshot = {
      report: {
        schemaVersion: 2,
        semanticHash: 'sha256:' + '6'.repeat(64),
        inputs: {
          observedCatalogHash: 'sha256:' + '9'.repeat(64),
          bindingSnapshotHash: bindingHash,
          sourceFreshness: 'current'
        },
        operationalState: 'current',
        blockers: [],
        complete: true,
        observedRows: [{
          observedTokenKey: OBSERVED_TOKEN_KEY,
          contextKey: OBSERVED_CONTEXT_KEY,
          valueStatus: 'value-drift',
          direction: 'design-observation-changed'
        }]
      },
      bindingSnapshot: {
        schemaVersion: 1,
        observedCatalogHash: 'sha256:' + '9'.repeat(64),
        semanticHash: bindingHash,
        bindings: [{
          observedTokenKey: OBSERVED_TOKEN_KEY,
          contextKey: OBSERVED_CONTEXT_KEY,
          targetState: 'present',
          projectTokenId: 'fixture-manifest:palette.error.400'
        }]
      }
    }
    const suppressedRun = compare(changedInventory, extraction, registry, { baseline, tokenSnapshot })
    const suppressedRow = rowFor(suppressedRun.report, set.designComponentId)
    assert.ok(suppressedRow.findings.some((finding) => finding.family === 'underlying-token-change' && finding.suppressesTask === true),
      JSON.stringify(suppressedRow.findings))
    assert.equal(suppressedRow.status, 'matched', 'token-explained visual drift does not manufacture component drift')
    assert.equal(suggestComponentTasks(suppressedRun.report).proposals.some((task) => task.designComponentId === set.designComponentId), false)

    const unprovenRun = compare(changedInventory, extraction, registry, { baseline })
    const unprovenRow = rowFor(unprovenRun.report, set.designComponentId)
    assert.equal(unprovenRow.findings.some((finding) => finding.suppressesTask === true), false)
    assert.equal(unprovenRow.status, 'matched', 'visual-only evidence drift must not rewrite semantic/API status')
    assert.equal(unprovenRow.dimensions.visual, 'review-required')
    assert.ok(suggestComponentTasks(unprovenRun.report).proposals.some((task) =>
      task.designComponentId === set.designComponentId && task.intent === 'update-visual'))
  })

  await check('§33.4-10: two sets sharing one display name stay distinct identities; the mapping never leaks by name', function () {
    const twin = captureFixture()
    twin.entities.push({
      nodeId: '40:1', pageId: '1:1', kind: 'component-set', name: 'AppButton', idQuality: 'stable',
      properties: [{ propertyId: 'p:size', name: 'Size', type: 'variant', idQuality: 'stable', options: ['Small'], defaultValue: 'Small' }],
      variants: [{ nodeId: '40:2', name: 'Size=Small', assignments: { 'p:size': 'Small' }, isDefault: true }],
      expectedVariantCount: 1, nestedRefs: [], boundVariables: [],
    })
    twin.witness.expectedEntityCount = 3
    twin.witness.readEntityCount = 3
    const twinInventory = inventoryOf(twin)
    const twinSets = twinInventory.components.filter((component) => component.name === 'AppButton')
    assert.equal(twinSets.length, 2)
    assert.notEqual(twinSets[0].designComponentId, twinSets[1].designComponentId)
    const { report } = compare(twinInventory, extraction, registry)
    assert.equal(rowFor(report, set.designComponentId).status, 'matched')
    const other = twinSets.find((component) => component.designComponentId !== set.designComponentId)
    assert.equal(rowFor(report, other.designComponentId).status, 'unmapped', 'the same-name twin must not inherit the mapping')
  })

  // ── §33.5: additional proof projects ──────────────────────────────────────
  await check('§33.5: alternate Compose structure extracts through the same generic core (no project-specific branch)', function () {
    const altRoot = mkdtempSync(join(tmpdir(), 'component-acceptance-alt-'))
    try {
      mkdirSync(join(altRoot, 'orchestrator/figma'), { recursive: true })
      mkdirSync(join(altRoot, 'ui'), { recursive: true })
      writeFileSync(join(altRoot, 'ui/Widgets.kt'), [
        'package com.alt.flat',
        '',
        'import androidx.compose.runtime.Composable',
        '',
        'enum class ChipTone { Neutral, Accent }',
        '',
        '@Composable',
        'fun FlatChip(text: String, tone: ChipTone = ChipTone.Neutral, onClick: (() -> Unit)? = null) {',
        '}',
        '',
        '@Composable',
        'internal fun FlatInternal() {}',
        '',
        '@Composable',
        'fun FlatCard(content: @Composable () -> Unit) { FlatChip(text = "x") }',
      ].join('\n') + '\n')
      writeFileSync(join(altRoot, 'orchestrator/figma/project-adapters.json'), JSON.stringify({
        schemaVersion: 2,
        adapters: [{
          id: 'alt-compose', kind: 'kotlin-compose', version: 2, enabled: true,
          capabilities: ['components'], platform: 'android-compose', authority: 'handwritten',
          components: { roots: ['ui'], include: ['**/*.kt'], exclude: [], visibility: ['public'] },
        }],
      }, null, 2))
      const altExtraction = extractFrom(altRoot)
      const inventory = altExtraction.inventories[0]
      assert.equal(inventory.witness.complete, true, JSON.stringify(inventory.witness))
      const names = inventory.components.map((component) => component.name).sort()
      assert.deepEqual(names, ['FlatCard', 'FlatChip'], 'public composables extracted, internal excluded')
      const chip = inventory.components.find((component) => component.name === 'FlatChip')
      assert.ok(chip.variantProperties.some((property) => property.name === 'tone' && property.source === 'enum'))
      const card = inventory.components.find((component) => component.name === 'FlatCard')
      assert.ok(card.dependencies.some((edge) => edge.kind === 'component' && edge.targetProjectComponentId === chip.projectComponentId),
        'nested call dependency resolves inside the alternate structure')
    } finally { rmSync(altRoot, { recursive: true, force: true }) }
  })

  await check('§33.5: multi-platform mapping — one Figma set, two implementations; a missing platform is honest drift', function () {
    const composeRoot = mkdtempSync(join(tmpdir(), 'component-acceptance-mp-'))
    try {
      mkdirSync(join(composeRoot, 'orchestrator/figma'), { recursive: true })
      mkdirSync(join(composeRoot, 'design/components'), { recursive: true })
      mkdirSync(join(composeRoot, 'app/ui'), { recursive: true })
      writeFileSync(join(composeRoot, 'design/components/manifest.json'), JSON.stringify(MANIFEST, null, 2))
      writeFileSync(join(composeRoot, 'app/ui/AppButton.kt'), [
        'package com.mp.ui',
        '',
        'import androidx.compose.runtime.Composable',
        '',
        'enum class AppButtonSize { Small, Large }',
        '',
        '@Composable',
        'fun AppButton(',
        '    label: String,',
        '    size: AppButtonSize = AppButtonSize.Small,',
        '    enabled: Boolean = true,',
        '    leadingIcon: (@Composable () -> Unit)? = null,',
        ') {',
        '}',
      ].join('\n') + '\n')
      writeFileSync(join(composeRoot, 'orchestrator/figma/project-adapters.json'), JSON.stringify({
        schemaVersion: 2,
        adapters: [
          {
            id: 'fixture-manifest', kind: 'component-manifest', version: 2, enabled: true,
            capabilities: ['components'], platform: 'web', authority: 'handwritten',
            components: { roots: ['design/components'], include: ['**/*.json'], exclude: [], visibility: ['public'] },
          },
          {
            id: 'mp-compose', kind: 'kotlin-compose', version: 2, enabled: true,
            capabilities: ['components'], platform: 'android-compose', authority: 'handwritten',
            components: { roots: ['app/ui'], include: ['**/*.kt'], exclude: [], visibility: ['public'] },
          },
        ],
      }, null, 2))
      const mpExtraction = extractFrom(composeRoot)
      assert.equal(mpExtraction.inventories.length, 2)
      const webInventory = mpExtraction.inventories.find((inventory) => inventory.adapterId === 'fixture-manifest')
      const composeInventory = mpExtraction.inventories.find((inventory) => inventory.adapterId === 'mp-compose')
      const webButton = webInventory.components.find((component) => component.fqName === 'ui/button')
      const composeButton = composeInventory.components.find((component) => component.name === 'AppButton')
      assert.ok(webButton && composeButton)

      const base = buttonMapping(design, webButton, 'fixture-manifest', 'web', webInventory.scopeFingerprint)
      const norm = (value) => String(value).toLowerCase().replace(/[^a-z0-9]/g, '')
      const composeSlots = []
      for (const designSlot of set.semanticSlots) {
        if (designSlot.kind !== 'text-property' && designSlot.kind !== 'instance-swap') continue
        const match = composeButton.slots.filter((slot) => norm(slot.name) === norm(designSlot.name))
        assert.equal(match.length, 1, `compose slot pairing must be exact for ${designSlot.name}`)
        composeSlots.push({ designSlotId: designSlot.slotId, adapterId: 'mp-compose', projectSlotId: match[0].slotId, verification: 'static' })
      }
      const multi = {
        ...base,
        implementations: [
          base.implementations[0],
          {
            adapterId: 'mp-compose', platform: 'android-compose',
            projectScopeFingerprint: composeInventory.scopeFingerprint,
            relation: 'direct', projectComponentIds: [composeButton.projectComponentId], required: true,
          },
        ],
        propertyMappings: [
          ...base.propertyMappings,
          { designPropertyId: 'p:size', adapterId: 'mp-compose', projectPropertyId: 'param:size', valueMap: { Small: 'Small', Large: 'Large' } },
          { designPropertyId: 'p:enabled', adapterId: 'mp-compose', projectPropertyId: 'param:enabled', valueMap: { true: 'true', false: 'false' } },
        ],
        slotMappings: [...base.slotMappings, ...composeSlots],
      }
      const multiRegistry = registryWith(design, [multi, iconMapping(design, webButton && webInventory.components.find((component) => component.fqName === 'ui/icon'), 'fixture-manifest', 'web', webInventory.scopeFingerprint)])
      const both = compareComponents({
        designInventory: design,
        projectInventories: mpExtraction.inventories,
        analysisIndex: analysisIndexOf(mpExtraction.inventories, mpExtraction.configState.componentConfigHash),
        mappingRegistry: multiRegistry,
        baseline: null,
        tokenSnapshot: { report: null, bindingSnapshot: null },
        context: { designGenerationId: 'gen-' + 'f'.repeat(32), adapterConfigHash: mpExtraction.configState.componentConfigHash, adapterConfigFileHash: mpExtraction.configState.componentConfigFileHash },
      })
      const bothRow = rowFor(both.report, set.designComponentId)
      assert.equal(bothRow.platforms.length, 2, JSON.stringify(bothRow.platforms))
      assert.equal(bothRow.platforms.every((platform) => platform.state === 'matched'), true, JSON.stringify(bothRow.platforms))
      assert.equal(bothRow.status, 'matched', JSON.stringify(bothRow.findings))

      // Remove the Compose implementation source: a required platform vanishes
      // from a complete scan — honest drift + an add-platform style proposal.
      rmSync(join(composeRoot, 'app/ui/AppButton.kt'))
      const shrunkExtraction = extractFrom(composeRoot)
      const shrunk = compareComponents({
        designInventory: design,
        projectInventories: shrunkExtraction.inventories,
        analysisIndex: analysisIndexOf(shrunkExtraction.inventories, shrunkExtraction.configState.componentConfigHash),
        mappingRegistry: multiRegistry,
        baseline: null,
        tokenSnapshot: { report: null, bindingSnapshot: null },
        context: { designGenerationId: 'gen-' + 'f'.repeat(32), adapterConfigHash: shrunkExtraction.configState.componentConfigHash, adapterConfigFileHash: shrunkExtraction.configState.componentConfigFileHash },
      })
      const shrunkRow = rowFor(shrunk.report, set.designComponentId)
      assert.equal(shrunkRow.status, 'drifted', JSON.stringify(shrunkRow.findings))
      assert.ok(shrunkRow.platforms.some((platform) => platform.platform === 'android-compose' && platform.state === 'missing'))
      const proposals = suggestComponentTasks(shrunk.report).proposals
      assert.ok(proposals.some((task) => task.intent === 'add-platform' || task.intent === 'implement'), JSON.stringify(proposals))
    } finally { rmSync(composeRoot, { recursive: true, force: true }) }
  })
} finally {
  rmSync(root, { recursive: true, force: true })
}

console.log(`\ncomponent-acceptance: ${checks} checks passed`)
