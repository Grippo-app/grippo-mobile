// Shared fixture builders for the component pipeline contract tests. Every
// builder returns a fresh deep object so tests can mutate freely; every
// fixture is valid against its schema + semantic contract unless a test
// breaks it on purpose. Mirrors token-fixtures.mjs in structure.
import { designComponentIdFor, deriveScopeId } from '../components/design-inventory-contract.mjs'

export const COMPONENT_CAPTURE_HASH = 'sha256:' + 'c'.repeat(64)
const OBSERVED_TOKEN = Object.freeze({
  primary: 'otk:sha256:' + '1'.repeat(64),
  secondary: 'otk:sha256:' + '2'.repeat(64),
  contextKey: 'theme=light|locale=und|platform=android',
  sourceId: 'otsrc:sha256:' + '3'.repeat(64)
})

const observedRef = (observedTokenKey, providerName, field, layerName) => ({
  observedTokenKey,
  contextKey: OBSERVED_TOKEN.contextKey,
  sourceId: OBSERVED_TOKEN.sourceId,
  providerName,
  field,
  ...(layerName ? { layerName } : {})
})

const PROVIDER_IDENTITY = Object.freeze({
  fileKeyFingerprint: 'sha256:' + 'a'.repeat(64),
  branchKey: 'none',
  libraryOriginPolicy: 'local-authoritative'
})

// Deterministic design identities of the fixture entities.
export const DESIGN_ID = Object.freeze({
  button: designComponentIdFor(PROVIDER_IDENTITY, '10:1'),
  icon: designComponentIdFor(PROVIDER_IDENTITY, '20:1'),
  badge: designComponentIdFor(PROVIDER_IDENTITY, '30:1'),
  remote: designComponentIdFor(PROVIDER_IDENTITY, '40:1')
})

export const ADAPTER_ID = 'compose-ds'
const PROJECT_SCOPE_FINGERPRINT = 'sha256:' + 'b'.repeat(64)
export const PROJECT_ID = Object.freeze({
  button: `${ADAPTER_ID}:symbol:com.example.ui.AppButton`,
  icon: `${ADAPTER_ID}:symbol:com.example.ui.AppIcon`,
  badge: `${ADAPTER_ID}:symbol:com.example.ui.AppBadge`,
  spare: `${ADAPTER_ID}:symbol:com.example.ui.AppSpare`
})

// A complete, consistency-proven capture: one sparse component-set (Button,
// 3 of 4 possible tuples — Small+Danger deliberately absent), one standalone
// component (Icon), one dependency-target set (Badge), one remote entity.
export function validComponentCapture() {
  return {
    schemaVersion: 2,
    provider: 'figma',
    providerIdentity: { ...PROVIDER_IDENTITY },
    scope: { kind: 'pages', pageIds: ['0:1'] },
    pages: [{ pageId: '0:1', name: 'Components' }],
    entities: [
      {
        nodeId: '10:1',
        pageId: '0:1',
        kind: 'component-set',
        name: 'Button',
        idQuality: 'stable',
        published: true,
        properties: [
          { propertyId: 'p-size', name: 'Size', type: 'variant', idQuality: 'stable', options: ['Small', 'Large'], defaultValue: 'Small' },
          { propertyId: 'p-tone', name: 'Tone', type: 'variant', idQuality: 'stable', options: ['Neutral', 'Danger'], defaultValue: 'Neutral' },
          { propertyId: 'p-disabled', name: 'Disabled', type: 'boolean', idQuality: 'stable', defaultValue: false },
          { propertyId: 'p-label', name: 'Label', type: 'text', idQuality: 'stable', defaultValue: 'Click' },
          { propertyId: 'p-icon', name: 'Icon', type: 'instance-swap', idQuality: 'stable', swapTargets: [{ targetNodeId: '20:1' }] }
        ],
        variants: [
          { nodeId: '10:2', name: 'Size=Small, Tone=Neutral', assignments: { 'p-size': 'Small', 'p-tone': 'Neutral' }, isDefault: true },
          { nodeId: '10:3', name: 'Size=Large, Tone=Neutral', assignments: { 'p-size': 'Large', 'p-tone': 'Neutral' } },
          { nodeId: '10:4', name: 'Size=Large, Tone=Danger', assignments: { 'p-size': 'Large', 'p-tone': 'Danger' } }
        ],
        expectedVariantCount: 3,
        defaultVariantNodeId: '10:2',
        nestedRefs: [
          { targetNodeId: '20:1', swappable: false, layerName: 'LeadingIcon' },
          { targetNodeId: '30:2', swappable: false, layerName: 'CountBadge' },
          { targetNodeId: '99:99', targetComponentKey: 'remote-key-1', swappable: false, remote: true }
        ],
        boundVariables: [
          observedRef(OBSERVED_TOKEN.secondary, 'Button/Background/Secondary', 'fills/color', 'bg'),
          observedRef(OBSERVED_TOKEN.primary, 'Button/Background/Primary', 'fills/color'),
          observedRef(OBSERVED_TOKEN.primary, 'Button/Background/Primary', 'fills/color')
        ],
        textLayers: [
          { nodeId: '10:8', name: 'LabelLayer', boundTextPropertyId: 'p-label' },
          { nodeId: '10:9', name: 'Caption' }
        ],
        autoLayout: 'horizontal'
      },
      {
        nodeId: '20:1', pageId: '0:1', kind: 'component', name: 'Icon', idQuality: 'stable',
        properties: [], variants: [], expectedVariantCount: 0, nestedRefs: [], boundVariables: []
      },
      {
        nodeId: '30:1', pageId: '0:1', kind: 'component-set', name: 'Badge', idQuality: 'stable',
        properties: [
          { propertyId: 'p-style', name: 'Style', type: 'variant', idQuality: 'stable', options: ['Solid', 'Outline'] }
        ],
        variants: [
          { nodeId: '30:2', name: 'Style=Solid', assignments: { 'p-style': 'Solid' } },
          { nodeId: '30:3', name: 'Style=Outline', assignments: { 'p-style': 'Outline' } }
        ],
        expectedVariantCount: 2,
        nestedRefs: [],
        boundVariables: []
      },
      {
        nodeId: '40:1', pageId: '0:1', kind: 'component', name: 'RemoteCard', idQuality: 'stable', remote: true,
        properties: [], variants: [], expectedVariantCount: 0, nestedRefs: [], boundVariables: []
      }
    ],
    visual: [
      { entityNodeId: '10:1', variantNodeId: '10:2', role: 'default', file: 'visual/button-default.png', sha256: 'sha256:' + '1'.repeat(64) },
      { entityNodeId: '10:1', variantNodeId: '10:4', role: 'extreme', file: 'visual/button-danger.png', sha256: 'sha256:' + '2'.repeat(64) },
      { entityNodeId: '20:1', variantNodeId: null, role: 'representative', file: 'visual/icon.png', sha256: 'sha256:' + '3'.repeat(64) }
    ],
    witness: {
      startedAt: '2026-07-20T10:00:00.000Z',
      finishedAt: '2026-07-20T10:00:05.000Z',
      providerRevisionBefore: 'rev-1',
      providerRevisionAfter: 'rev-1',
      consistency: 'proven',
      completeness: 'complete',
      requestedPageIds: ['0:1'],
      readPageIds: ['0:1'],
      expectedEntityCount: 4,
      readEntityCount: 4,
      truncated: false,
      permissionDegraded: false,
      limitsHit: []
    }
  }
}

function fixtureScopeId() {
  return deriveScopeId(validComponentCapture())
}

// Adapter config with both capabilities on kotlin-compose plus a components-only
// component-manifest adapter — the two shipped component-capable kinds.
export function validComponentAdapterConfig() {
  return {
    schemaVersion: 2,
    adapters: [
      {
        id: 'compose-ds',
        kind: 'kotlin-compose',
        version: 2,
        enabled: true,
        capabilities: ['tokens', 'components'],
        platform: 'android-compose',
        authority: 'handwritten',
        tokens: {
          roots: ['design-system/tokens'],
          include: ['**/*.kt'],
          exclude: ['**/build/**'],
          modes: ['shared'],
          authorities: {
            color: { contracts: ['com.example.design.AppColor'], implementations: [], primitiveContainers: ['com.example.design.AppPalette'] }
          },
          contextMap: [{ when: { theme: 'light', platform: 'android' }, projectMode: 'shared' }],
          bindingRules: []
        },
        components: {
          roots: ['design-system/components'],
          include: ['**/*.kt'],
          exclude: ['**/build/**'],
          visibility: ['public'],
          previewRoots: ['design-system/previews'],
          screenshotTestRoots: ['design-system/screenshot']
        }
      },
      {
        id: 'manifest-ds',
        kind: 'component-manifest',
        version: 2,
        enabled: true,
        capabilities: ['components'],
        platform: 'web',
        authority: 'handwritten',
        components: {
          roots: ['web/components'],
          include: ['**/*.json'],
          exclude: [],
          visibility: ['public']
        }
      }
    ]
  }
}

function projectComponents() {
  return [
    {
      projectComponentId: PROJECT_ID.badge,
      name: 'AppBadge',
      fqName: 'com.example.ui.AppBadge',
      kind: 'function-component',
      visibility: 'public',
      source: { path: 'design-system/src/AppBadge.kt', line: 6, symbol: 'com.example.ui.AppBadge', fileHash: 'sha256:' + 'e'.repeat(64) },
      api: {
        parameters: [
          { name: 'style', kind: 'value', typeText: 'BadgeStyle', required: true, hasDefault: false }
        ]
      },
      variantProperties: [
        {
          projectPropertyId: 'param:style', name: 'style', source: 'enum',
          typeSymbol: 'com.example.ui.BadgeStyle',
          values: [{ value: 'Solid' }, { value: 'Outline' }],
          defaultKnown: false
        }
      ],
      combinationsKnown: 'all',
      slots: [],
      dependencies: [],
      evidence: { previews: [], screenshotTests: [{ path: 'design-system/screenshot/AppBadgeTest.kt' }] }
    },
    {
      projectComponentId: PROJECT_ID.button,
      name: 'AppButton',
      fqName: 'com.example.ui.AppButton',
      kind: 'function-component',
      visibility: 'public',
      source: { path: 'design-system/src/AppButton.kt', line: 12, symbol: 'com.example.ui.AppButton', fileHash: 'sha256:' + 'e'.repeat(64) },
      api: {
        parameters: [
          { name: 'size', kind: 'value', typeText: 'ButtonSize', required: false, hasDefault: true, defaultText: 'ButtonSize.Small' },
          { name: 'tone', kind: 'value', typeText: 'ButtonTone', required: false, hasDefault: true, defaultText: 'ButtonTone.Neutral' },
          { name: 'disabled', kind: 'value', typeText: 'Boolean', required: false, hasDefault: true, defaultText: 'false' },
          { name: 'label', kind: 'text', typeText: 'String', required: true, hasDefault: false },
          { name: 'icon', kind: 'content-lambda', typeText: '@Composable () -> Unit', required: false, hasDefault: true, defaultText: 'null' },
          { name: 'onClick', kind: 'callback', typeText: '() -> Unit', required: true, hasDefault: false },
          { name: 'modifier', kind: 'modifier', typeText: 'Modifier', required: false, hasDefault: true, defaultText: 'Modifier' }
        ]
      },
      variantProperties: [
        {
          projectPropertyId: 'param:size', name: 'size', source: 'enum',
          typeSymbol: 'com.example.ui.ButtonSize',
          values: [{ value: 'Small' }, { value: 'Large' }],
          defaultValue: 'Small', defaultKnown: true
        },
        {
          projectPropertyId: 'param:tone', name: 'tone', source: 'sealed',
          typeSymbol: 'com.example.ui.ButtonTone',
          values: [{ value: 'Neutral' }, { value: 'Danger' }],
          defaultValue: 'Neutral', defaultKnown: true
        },
        {
          projectPropertyId: 'param:disabled', name: 'disabled', source: 'boolean',
          values: [{ value: 'false' }, { value: 'true' }],
          defaultValue: 'false', defaultKnown: true
        }
      ],
      combinationsKnown: 'all',
      slots: [
        { slotId: 'param:label', kind: 'text', name: 'label', required: true },
        { slotId: 'param:icon', kind: 'content', name: 'icon', required: false },
        { slotId: 'param:onClick', kind: 'callback', name: 'onClick', required: true }
      ],
      dependencies: [
        { kind: 'component', targetProjectComponentId: PROJECT_ID.icon, symbol: 'AppIcon' },
        { kind: 'component', targetProjectComponentId: PROJECT_ID.badge, symbol: 'AppBadge' },
        { kind: 'token', projectTokenId: 'compose-ds:com.example.design.AppPalette.error400', path: 'design-system/src/AppButton.kt', line: 20 }
      ],
      evidence: {
        previews: [{ symbol: 'AppButtonPreview', path: 'design-system/previews/AppButtonPreviews.kt', line: 8 }],
        screenshotTests: [{ path: 'design-system/screenshot/AppButtonTest.kt', className: 'AppButtonTest' }]
      }
    },
    {
      projectComponentId: PROJECT_ID.icon,
      name: 'AppIcon',
      fqName: 'com.example.ui.AppIcon',
      kind: 'function-component',
      visibility: 'public',
      source: { path: 'design-system/src/AppIcon.kt', line: 4, symbol: 'com.example.ui.AppIcon', fileHash: 'sha256:' + 'e'.repeat(64) },
      api: { parameters: [] },
      variantProperties: [],
      combinationsKnown: 'all',
      slots: [],
      dependencies: [],
      evidence: { previews: [], screenshotTests: [{ path: 'design-system/screenshot/AppIconTest.kt' }] }
    },
    {
      projectComponentId: PROJECT_ID.spare,
      name: 'AppSpare',
      fqName: 'com.example.ui.AppSpare',
      kind: 'function-component',
      visibility: 'public',
      source: { path: 'design-system/src/AppSpare.kt', line: 3, symbol: 'com.example.ui.AppSpare', fileHash: 'sha256:' + 'e'.repeat(64) },
      api: { parameters: [] },
      variantProperties: [],
      combinationsKnown: 'all',
      slots: [],
      dependencies: [],
      evidence: { previews: [], screenshotTests: [] }
    }
  ]
}

// Recompute the counts block from the rows — call after mutating components.
export function finishProjectComponentInventory(inventory) {
  let variantProperties = 0
  let slots = 0
  let dependencyEdges = 0
  for (const component of inventory.components) {
    variantProperties += component.variantProperties.length
    slots += component.slots.length
    dependencyEdges += component.dependencies.length
  }
  inventory.counts = {
    components: inventory.components.length,
    variantProperties,
    slots,
    dependencyEdges
  }
  return inventory
}

export function validProjectComponentInventory() {
  const components = projectComponents()
  return finishProjectComponentInventory({
    schemaVersion: 2,
    adapterId: ADAPTER_ID,
    adapterKind: 'kotlin-compose',
    adapterVersion: 2,
    platform: 'android-compose',
    scopeFingerprint: PROJECT_SCOPE_FINGERPRINT,
    configHash: 'sha256:' + 'd'.repeat(64),
    components,
    witness: {
      rootsConfigured: 1,
      rootsResolved: 1,
      rootsMissing: [],
      filesMatched: 4,
      filesParsed: 4,
      parseFailures: [],
      limitsHit: [],
      complete: true
    },
    counts: { components: 0, variantProperties: 0, slots: 0, dependencyEdges: 0 }
  })
}

// An extra empty inventory for a second platform (add-platform scenarios).
export function emptyManifestInventory() {
  return {
    schemaVersion: 2,
    adapterId: 'manifest-ds',
    adapterKind: 'component-manifest',
    adapterVersion: 2,
    platform: 'web',
    scopeFingerprint: 'sha256:' + '9'.repeat(64),
    configHash: 'sha256:' + 'd'.repeat(64),
    components: [],
    witness: {
      rootsConfigured: 1,
      rootsResolved: 1,
      rootsMissing: [],
      filesMatched: 0,
      filesParsed: 0,
      parseFailures: [],
      limitsHit: [],
      complete: true
    },
    counts: { components: 0, variantProperties: 0, slots: 0, dependencyEdges: 0 }
  }
}

export function validComponentAnalysisIndex(inventories, hashOf) {
  const rows = inventories.map((inventory) => ({
    adapterId: inventory.adapterId,
    platform: inventory.platform,
    role: `project-component-inventory:${inventory.adapterId}`,
    inventoryHash: hashOf(inventory),
    scopeFingerprint: inventory.scopeFingerprint,
    complete: inventory.witness.complete
  }))
  return {
    schemaVersion: 2,
    configHash: inventories[0].configHash,
    adapters: rows,
    complete: rows.every((row) => row.complete)
  }
}

const AT = '2026-07-20T11:00:00.000Z'

export function validComponentRegistry(designScopeId) {
  return {
    schemaVersion: 2,
    revision: 3,
    designScopeId,
    mappings: [
      {
        mappingId: 'cmap-' + '1'.repeat(24),
        designComponentId: DESIGN_ID.button,
        expectedKind: 'component-set',
        implementations: [
          {
            adapterId: ADAPTER_ID,
            platform: 'android-compose',
            projectScopeFingerprint: PROJECT_SCOPE_FINGERPRINT,
            relation: 'direct',
            projectComponentIds: [PROJECT_ID.button],
            required: true
          }
        ],
        propertyMappings: [
          { designPropertyId: 'p-size', adapterId: ADAPTER_ID, projectPropertyId: 'param:size', valueMap: { Small: 'Small', Large: 'Large' } },
          { designPropertyId: 'p-tone', adapterId: ADAPTER_ID, projectPropertyId: 'param:tone', valueMap: { Neutral: 'Neutral', Danger: 'Danger' } },
          { designPropertyId: 'p-disabled', adapterId: ADAPTER_ID, projectPropertyId: 'param:disabled', valueMap: { true: 'true', false: 'false' } }
        ],
        slotMappings: [
          { designSlotId: 'prop:p-label', adapterId: ADAPTER_ID, projectSlotId: 'param:label', verification: 'static' },
          { designSlotId: 'prop:p-icon', adapterId: ADAPTER_ID, projectSlotId: 'param:icon', verification: 'static' }
        ],
        state: 'active',
        provenance: { kind: 'user-confirmed', actor: 'owner', at: AT }
      },
      {
        mappingId: 'cmap-' + '2'.repeat(24),
        designComponentId: DESIGN_ID.icon,
        expectedKind: 'component',
        implementations: [
          {
            adapterId: ADAPTER_ID,
            platform: 'android-compose',
            projectScopeFingerprint: PROJECT_SCOPE_FINGERPRINT,
            relation: 'direct',
            projectComponentIds: [PROJECT_ID.icon],
            required: true
          }
        ],
        propertyMappings: [],
        slotMappings: [],
        state: 'active',
        provenance: { kind: 'user-confirmed', actor: 'owner', at: AT }
      },
      {
        mappingId: 'cmap-' + '3'.repeat(24),
        designComponentId: DESIGN_ID.badge,
        expectedKind: 'component-set',
        implementations: [
          {
            adapterId: ADAPTER_ID,
            platform: 'android-compose',
            projectScopeFingerprint: PROJECT_SCOPE_FINGERPRINT,
            relation: 'direct',
            projectComponentIds: [PROJECT_ID.badge],
            required: true
          }
        ],
        propertyMappings: [
          { designPropertyId: 'p-style', adapterId: ADAPTER_ID, projectPropertyId: 'param:style', valueMap: { Solid: 'Solid', Outline: 'Outline' } }
        ],
        slotMappings: [],
        state: 'active',
        provenance: { kind: 'user-confirmed', actor: 'owner', at: AT }
      }
    ],
    dispositions: []
  }
}

export function comparatorContext() {
  return {
    designGenerationId: 'gen-' + 'f'.repeat(32),
    adapterConfigHash: 'sha256:' + 'd'.repeat(64),
    adapterConfigFileHash: 'sha256:' + '5'.repeat(64)
  }
}

// Token snapshot for the §17.12 causality proof: the token comparator confirmed
// a design-side value drift on the variable Button binds, and the token mapping
// registry maps it onto exactly the project token AppButton depends on.
export function tokenSnapshotFixture() {
  const bindingHash = 'sha256:' + '8'.repeat(64)
  return {
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
      observedRows: [
        {
          observedTokenKey: OBSERVED_TOKEN.primary,
          contextKey: OBSERVED_TOKEN.contextKey,
          valueStatus: 'value-drift',
          direction: 'design-observation-changed'
        }
      ]
    },
    bindingSnapshot: {
      schemaVersion: 1,
      observedCatalogHash: 'sha256:' + '9'.repeat(64),
      semanticHash: bindingHash,
      bindings: [
        {
          observedTokenKey: OBSERVED_TOKEN.primary,
          contextKey: OBSERVED_TOKEN.contextKey,
          targetState: 'present',
          projectTokenId: 'compose-ds:com.example.design.AppPalette.error400'
        }
      ]
    }
  }
}

// Canonical component task binding (component-task-binding.schema.json +
// tasks/component-binding-contract.cjs).
export function validComponentTaskBinding() {
  return {
    schemaVersion: 2,
    sourceId: `design:component:${DESIGN_ID.button}:component-implement`,
    intent: 'implement',
    designComponentId: DESIGN_ID.button,
    designScopeId: fixtureScopeId(),
    designGenerationId: 'gen-' + 'f'.repeat(32),
    designInventoryHash: 'sha256:' + 'a'.repeat(64),
    expectedKind: 'component-set',
    frozenStructuralHash: 'sha256:' + 'b'.repeat(64),
    frozenSourceHash: 'sha256:' + 'c'.repeat(64),
    frozenSpec: {
      name: 'Button',
      properties: [
        { propertyId: 'p-size', name: 'Size', type: 'variant', options: ['Small', 'Large'], defaultValue: 'Small' }
      ],
      variants: [
        { variantId: '10:2', assignments: { 'p-size': 'Small' }, isDefault: true }
      ],
      slots: [
        { slotId: 'prop:p-label', kind: 'text-property', name: 'Label' }
      ],
      tokenRefs: [
        observedRef(OBSERVED_TOKEN.primary, 'Button/Background/Primary', 'fills/color')
      ]
    },
    intendedAdapterId: ADAPTER_ID,
    intendedPlatform: 'android-compose',
    intendedRelation: 'direct',
    intendedProjectSymbol: 'com.example.ui.AppButton',
    findingId: 'cmpf-' + '0'.repeat(24),
    comparisonSemanticHash: 'sha256:' + 'd'.repeat(64),
    mappingRevision: 3
  }
}
