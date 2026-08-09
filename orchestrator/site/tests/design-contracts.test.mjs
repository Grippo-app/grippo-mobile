#!/usr/bin/env node

import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { createSchemaRegistry } from '../../figma/runtime/schema-registry.mjs'
import { aggregateObservedTokens } from '../../figma/tokens/catalog-aggregator.mjs'
import { bindObservedTokens, bindingSnapshotSemanticError } from '../../figma/tokens/binder.mjs'
import { compareTokens } from '../../figma/tokens/comparator.mjs'
import { emptyMappingRegistry } from '../../figma/tokens/mapping-contract.mjs'
import { normalizeSourceCapture } from '../../figma/tokens/source-normalizer.mjs'
import { immutablePlan, validObservedCapture } from '../../figma/tests/observed-token-fixtures.mjs'
import {
  COMPONENT_CAPTURE_HASH, validComponentCapture, validProjectComponentInventory,
  validComponentAnalysisIndex
} from '../../figma/tests/component-fixtures.mjs'
import { normalizeCapture as normalizeComponentCapture } from '../../figma/components/capture-normalizer.mjs'
import { compareComponents } from '../../figma/components/comparator.mjs'
import { emptyMappingRegistry as emptyComponentMappings } from '../../figma/components/mapping-contract.mjs'
import { projectInventorySemanticHash } from '../../figma/components/project-inventory-contract.mjs'
import { dictionaryFor } from './i18n-test-helpers.mjs'
import { createDesignRenderGeneration } from '../scripts/panels/design.js'

const here = dirname(fileURLToPath(import.meta.url))
const repo = join(here, '..', '..', '..')
const require = createRequire(import.meta.url)
const schemas = createSchemaRegistry(join(repo, 'orchestrator', 'figma', 'schemas'))
const hash = (char) => 'sha256:' + char.repeat(64)
let checks = 0
function check(name, fn) {
  fn()
  checks++
  console.log(`ok ${checks} - ${name}`)
}

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

check('observed token publication contracts validate with exact hashes', () => {
  assert.equal(schemas.validate('observed-token-source-index')(observed.index), true)
  assert.equal(schemas.validate('observed-token-catalog')(observed.catalog), true)
  for (const row of observed.shards) {
    assert.equal(schemas.validate('observed-token-source-shard')(row.shard), true)
  }
})

const adapterConfig = {
  tokenConfigHash: hash('a'),
  enabledTokenAdapters: [{
    id: 'fixture', platform: 'shared',
    tokens: {
      contextMap: [{ when: { theme: 'light', locale: 'default', platform: 'shared' }, projectMode: 'shared' }],
      bindingRules: [{
        ruleId: 'exact-primary', kind: 'exact-path', tokenKind: 'color',
        providerPath: ['color', 'content', 'primary'],
        projectPath: ['AppColor', 'primary']
      }]
    }
  }]
}
const projectInventory = {
  adapterId: 'fixture',
  tokens: [{
    projectTokenId: 'fixture:AppColor.primary',
    kind: 'color',
    semanticPath: ['AppColor', 'primary']
  }]
}
const tokenMappings = emptyMappingRegistry(observed.catalog.scope)
const binding = bindObservedTokens({
  catalog: observed.catalog,
  projectInventories: [projectInventory],
  adapterConfig,
  mappingRegistry: tokenMappings,
  projectAnalysisHash: hash('b')
})

check('binding is deterministic, context-exact, and schema-valid', () => {
  assert.equal(schemas.validate('token-binding-snapshot')(binding), true)
  assert.equal(bindingSnapshotSemanticError(binding), null)
  assert.equal(binding.bindings.length, 1)
  assert.equal(binding.bindings[0].contextKey, JSON.stringify({
    locale: 'default', platform: 'shared', theme: 'light'
  }))
})

const tokenReport = compareTokens({
  observedCatalog: observed.catalog,
  catalog: observed.catalog,
  sourceIndex: observed.index,
  projectInventories: [projectInventory],
  analysisIndex: {
    schemaVersion: 2,
    configHash: hash('a'),
    complete: true,
    adapters: [],
    semanticHash: hash('c')
  },
  bindingSnapshot: binding,
  mappingRegistry: tokenMappings,
  baseline: null,
  context: {
    observedGenerationId: 'gen-' + '1'.repeat(32),
    analysisIndexHash: hash('c'),
    adapterConfigHash: hash('a'),
    adapterConfigFileHash: hash('d'),
    baselineHash: 'none',
    sourceFreshness: 'current'
  }
}).report

check('token comparator emits only the current v2 report contract', () => {
  const validate = schemas.validate('token-comparison')
  assert.equal(validate(tokenReport), true, JSON.stringify(validate.errors))
  assert.equal(tokenReport.schemaVersion, 2)
  assert.equal(tokenReport.inputs.bindingSnapshotHash, binding.semanticHash)
})

const componentCapture = validComponentCapture()
const designComponents = normalizeComponentCapture(componentCapture, COMPONENT_CAPTURE_HASH)
const projectComponents = validProjectComponentInventory()
const componentAnalysis = validComponentAnalysisIndex([projectComponents], projectInventorySemanticHash)
const componentMappings = emptyComponentMappings(designComponents.scopeId)
const componentResult = compareComponents({
  designInventory: designComponents,
  projectInventories: [projectComponents],
  analysisIndex: componentAnalysis,
  mappingRegistry: componentMappings,
  baseline: null,
  tokenSnapshot: { report: tokenReport, bindingSnapshot: binding },
  context: {
    designGenerationId: 'gen-' + '2'.repeat(32),
    adapterConfigHash: hash('e'),
    adapterConfigFileHash: hash('f')
  }
})

check('component capture and comparison use only v2 contracts', () => {
  assert.equal(schemas.validate('design-component-capture')(componentCapture), true)
  assert.equal(schemas.validate('design-component-inventory')(designComponents), true)
  assert.equal(schemas.validate('component-comparison')(componentResult.report), true)
  assert.equal(componentResult.report.schemaVersion, 2)
})

check('server and UI readers name the observed domain and preserve exact token refs', () => {
  const catalogSource = readFileSync(join(repo, 'orchestrator', 'site', 'server', 'design-catalog.js'), 'utf8')
  const tokenView = readFileSync(join(repo, 'orchestrator', 'site', 'scripts', 'design', 'tokens.js'), 'utf8')
  const drawer = readFileSync(join(repo, 'orchestrator', 'site', 'scripts', 'design', 'entity-drawer.js'), 'utf8')
  assert.match(catalogSource, /observed-token-catalog/)
  assert.match(catalogSource, /token-binding-snapshot/)
  assert.match(tokenView, /context\.shouldRender\(data\)/)
  assert.match(drawer, /observedTokenKey/)
  assert.match(drawer, /designContextKey/)
  assert.doesNotMatch(drawer, /designModeId/)
  assert.doesNotMatch(catalogSource, /designModeId/)
  assert.doesNotMatch(catalogSource, /design-token-(?:capture|inventory)/)
})

check('all three locales expose current token and component actions', () => {
  for (const locale of ['en', 'ru', 'uk']) {
    const dictionary = dictionaryFor(locale)
    for (const key of [
      'design.tab.tokens',
      'design.tab.components',
      'design.analysis.action.compare',
      'design.tokens.scope.sources',
      'design.tokenSource.currentAccepted',
      'design.tokenSource.healthUnavailable',
      'design.tokenSource.confirm.reactivate-source.action',
      'design.error.token-source-cas-conflict',
    ]) {
      assert.equal(typeof dictionary[key], 'string', `${locale}:${key}`)
    }
  }
})

check('token Sources view renders exact actions, separated health state, and dialog accessibility', () => {
  const tokenView = readFileSync(join(repo, 'orchestrator', 'site', 'scripts', 'design', 'tokens.js'), 'utf8')
  const filters = readFileSync(join(repo, 'orchestrator', 'site', 'scripts', 'design', 'filters.js'), 'utf8')
  assert.match(tokenView, /\/api\/design\/token-sources/)
  assert.match(tokenView, /detach-origin/)
  assert.match(tokenView, /retire-source/)
  assert.match(tokenView, /reactivate-source/)
  assert.match(tokenView, /sourceHealthAvailable/)
  assert.match(tokenView, /currentAccepted/)
  assert.match(tokenView, /aria-labelledby/)
  assert.match(tokenView, /aria-live/)
  assert.match(tokenView, /trigger\.focus/)
  assert.match(filters, /TOKEN_SOURCE_STATUSES/)
  assert.match(filters, /token-source-confirmation-stale/)
})

check('background polling preserves focus, scroll, drafts, and exact Source Health actions', () => {
  const panel = readFileSync(join(repo, 'orchestrator', 'site', 'scripts', 'panels', 'design.js'), 'utf8')
  const banner = readFileSync(join(repo, 'orchestrator', 'site', 'scripts', 'design', 'analysis-banner.js'), 'utf8')
  const tokenView = readFileSync(join(repo, 'orchestrator', 'site', 'scripts', 'design', 'tokens.js'), 'utf8')
  const componentView = readFileSync(join(repo, 'orchestrator', 'site', 'scripts', 'design', 'components.js'), 'utf8')
  const surfaceView = readFileSync(join(repo, 'orchestrator', 'site', 'scripts', 'design', 'surfaces.js'), 'utf8')
  assert.match(panel, /renderGeneration\.isCurrent\(version\)/)
  assert.match(panel, /renderGeneration\.commit\(version\)/)
  assert.match(panel, /preserveToolbar/)
  assert.match(panel, /captureInteraction/)
  assert.match(panel, /restoreInteraction/)
  assert.match(panel, /beforeRender/)
  assert.match(panel, /if \(background\) interaction = captureInteraction\(\)/)
  assert.match(panel, /tokenOps = \[\]/)
  assert.match(panel, /componentOps = \[\]/)
  for (const source of [tokenView, componentView, surfaceView]) {
    assert.match(source, /data-design-focus/)
  }
  assert.match(banner, /source-health-unknown/)
  assert.match(banner, /source-refresh-required/)
  assert.match(banner, /compareSaved/)
  assert.match(banner, /#figma\?sync=tokens/)
})

check('unchanged background refresh keeps mounted Design pagination handlers current', () => {
  const generation = createDesignRenderGeneration()
  const mounted = generation.begin(false)
  assert.equal(generation.isCurrent(mounted), true)
  assert.equal(generation.commit(mounted), true)

  const unchangedRefresh = generation.begin(true)
  assert.equal(generation.isCurrent(mounted), true)
  assert.equal(generation.isCurrent(unchangedRefresh), true)

  const changedRefresh = generation.begin(true)
  assert.equal(generation.isCurrent(unchangedRefresh), false)
  assert.equal(generation.isCurrent(mounted), true)
  assert.equal(generation.commit(changedRefresh), true)
  assert.equal(generation.isCurrent(mounted), false)
  assert.equal(generation.isCurrent(changedRefresh), true)

  const foreground = generation.begin(false)
  assert.equal(generation.isCurrent(changedRefresh), false)
  assert.equal(generation.isCurrent(foreground), true)
})

check('untrusted provider text cannot become markup, an unsafe URL, or a project path', () => {
  const designDir = join(repo, 'orchestrator', 'site', 'scripts', 'design')
  for (const file of [
    'overview.js', 'tokens.js', 'components.js', 'surfaces.js', 'entity-drawer.js',
    'analysis-banner.js', 'filters.js'
  ]) {
    const source = readFileSync(join(designDir, file), 'utf8')
    assert.doesNotMatch(source, /\bhtml\s*:/)
    assert.doesNotMatch(source, /\.innerHTML\b|insertAdjacentHTML/)
  }
  const safety = require('../server/design-catalog.js')._test
  assert.equal(
    safety.safeFigmaUrl('https://figma.com/design/AbCd/Name?node-id=10:20&token=secret'),
    'https://www.figma.com/design/AbCd?node-id=10-20'
  )
  for (const value of [
    'javascript:alert(1)',
    'data:text/html,<script>alert(1)</script>',
    'https://figma.com.evil.example/design/AbCd'
  ]) assert.equal(safety.safeFigmaUrl(value), null)
  assert.equal(
    safety.safeFigmaUrl('https://user@figma.com/design/AbCd'),
    'https://www.figma.com/design/AbCd'
  )
  assert.equal(safety.safePath('src/design/Button.kt'), 'src/design/Button.kt')
  for (const value of [
    '/private/project/secret', 'C:\\private\\secret', '../secret', 'src/../secret',
    'src//secret'
  ]) assert.equal(safety.safePath(value), null)
})

check('token project-only classification is identical across mapping, catalog, and history', () => {
  const catalog = require('../server/design-catalog.js')
  const history = require('../server/design-history.js')._test
  assert.equal(Object.hasOwn(catalog.PROJECT_TOKEN_CLASSIFICATION, 'project-only-intentional'), true)
  assert.equal(Object.hasOwn(catalog.PROJECT_TOKEN_CLASSIFICATION, 'intentionally-project-only'), false)
  const projection = {
    id: 'tokp-' + 'a'.repeat(24),
    entityType: 'project-token',
    name: 'Local semantic token',
    status: 'project-only-intentional',
    summary: {
      kind: 'color',
      projectTokenId: 'fixture:AppColor.local',
      classification: 'project-only-intentional'
    }
  }
  assert.equal(history.validateSummary(projection), true)
  assert.equal(history.validateSummary({
    ...projection,
    status: 'intentionally-project-only',
    summary: { ...projection.summary, classification: 'intentionally-project-only' }
  }), false)
})

check('token context keys are canonical and corruption never degrades to an empty context', () => {
  const identity = require('../../figma/runtime/token-identity.cjs')
  const context = { theme: 'light', locale: 'default', platform: 'shared' }
  const key = identity.contextKey(context)
  assert.deepEqual(identity.parseContextKey(key), context)
  for (const invalid of [
    'not-json',
    '{"theme":"light","locale":"default"}',
    '{"platform":"shared","locale":"default","theme":"light"}',
    '{"locale":"default","platform":"shared","theme":"light","unknown":"value"}'
  ]) assert.throws(() => identity.parseContextKey(invalid), /TOKEN_SOURCE_CONTEXT_INVALID/)
  const catalogSource = readFileSync(
    join(repo, 'orchestrator', 'site', 'server', 'design-catalog.js'), 'utf8'
  )
  assert.match(catalogSource, /tokenIdentity\.parseContextKey\(value\)/)
  assert.doesNotMatch(catalogSource, /JSON\.parse\(value\);\s*\}\s*catch \(error\) \{\s*return \{\}/)
})

console.log(`design contracts: ${checks} checks passed`)
