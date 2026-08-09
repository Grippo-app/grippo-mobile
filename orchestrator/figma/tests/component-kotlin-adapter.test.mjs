// component-kotlin-adapter.test.mjs — pins for the built-in kotlin-compose
// component extractor (CMP-KT-*) over inline Kotlin sources: @Composable
// discovery + visibility filter, params/defaults, enum/sealed/boolean variant
// properties, slots, state/Modifier classification, nested deps vs ambiguous
// overloads, token deps via the tokens authorities, wrapperOf, overload
// discriminators, preview/screenshot evidence, parse fail-closed, and
// determinism. Plus the component-manifest adapter (CMP-MANIFEST-*), the
// non-Compose proof of the same inventory contract.
import assert from 'node:assert/strict'
import { extractComponents, KOTLIN_COMPOSE_COMPONENTS_EXTRACTOR_VERSION } from '../adapters/kotlin-compose/components.mjs'
import { extractComponents as extractManifestComponents, COMPONENT_MANIFEST_EXTRACTOR_VERSION } from '../adapters/component-manifest/components.mjs'
import { adapterImplementation } from '../runtime/adapter-registry.mjs'
import { buildComponentInventory } from '../runtime/component-extraction.mjs'
import { projectInventorySemanticError, projectInventorySemanticHash } from '../components/project-inventory-contract.mjs'
import { sha256Text } from '../scripts/report-utils.mjs'

const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }

const ADAPTER_ID = 'compose-ds'
const id = (fq) => `${ADAPTER_ID}:symbol:${fq}`
const file = (path, text) => ({ path, text, hash: sha256Text(text) })

const MAIN = file('ds/src/AppButton.kt', `package com.example.ui

import androidx.compose.runtime.Composable
import com.example.design.AppPalette

enum class ButtonSize { Small, Large }

sealed class ButtonTone {
    object Neutral : ButtonTone()
    object Danger : ButtonTone()
}

@Composable
fun AppButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: ButtonSize = ButtonSize.Small,
    tone: ButtonTone = ButtonTone.Neutral,
    enabled: Boolean = true,
    state: ButtonState = ButtonState.Idle,
    icon: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val accent = AppPalette.error400
    AppIcon()
    content()
}

@Composable
private fun SecretButton(label: String) {
}

@Composable
internal fun InternalChrome() {
}

@Composable
fun PrimaryButton(label: String) {
    AppButton(label = label, onClick = {})
}
`)

const ICON = file('ds/src/AppIcon.kt', `package com.example.ui

import androidx.compose.runtime.Composable

@Composable
fun AppIcon(modifier: Modifier = Modifier) {
}

@Composable
fun AppChip(label: String) {
}

@Composable
fun AppChip(label: String, selected: Boolean) {
}

@Composable
fun ChipRow() {
    AppChip("x")
}
`)

const PREVIEW = file('ds/previews/AppButtonPreviews.kt', `package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun AppButtonPreview() {
    AppButton(label = "Hi", onClick = {}) { }
}
`)

const SHOT = file('ds/screenshot/AppButtonTest.kt', `package com.example.ui

class AppButtonScreenshotTest {
    fun snap() {
        AppButton(label = "Hi", onClick = {}) { }
    }
}
`)

const BROKEN = file('ds/src/Broken.kt', 'fun ( broken {{{')

const componentsConfig = () => ({
  roots: ['ds/src'], include: ['**/*.kt'], exclude: [], visibility: ['public'],
  previewRoots: ['ds/previews'], screenshotTestRoots: ['ds/screenshot']
})
const tokensConfig = () => ({
  roots: ['tokens'], include: ['**/*.kt'], exclude: [], modes: ['shared'],
  authorities: { color: { contracts: [], implementations: [], primitiveContainers: ['com.example.design.AppPalette'] } }
})
const extract = (overrides) => extractComponents({
  files: [MAIN, ICON],
  previewFiles: [PREVIEW],
  screenshotTestFiles: [SHOT],
  componentsConfig: componentsConfig(),
  tokensConfig: tokensConfig(),
  adapterId: ADAPTER_ID,
  platform: 'android-compose',
  ...(overrides || {})
})
const componentOf = (extraction, name) => extraction.components.find((component) => component.name === name)

check('CMP-KT: registry serves the component extractors for both shipped kinds', () => {
  const kotlin = adapterImplementation('kotlin-compose')
  assert.equal(kotlin.componentsExtractorVersion, KOTLIN_COMPOSE_COMPONENTS_EXTRACTOR_VERSION)
  assert.equal(kotlin.extractComponents, extractComponents)
  assert.equal(KOTLIN_COMPOSE_COMPONENTS_EXTRACTOR_VERSION, 'kotlin-compose-components-v2')
  const manifest = adapterImplementation('component-manifest')
  assert.equal(manifest.componentsExtractorVersion, COMPONENT_MANIFEST_EXTRACTOR_VERSION)
  assert.equal(manifest.extractComponents, extractManifestComponents)
  assert.equal(manifest.extractTokens, undefined, 'component-manifest ships no token capability')
})

check('CMP-KT-DISCOVERY: only allowed-visibility @Composable functions become components', () => {
  const extraction = extract()
  assert.deepEqual(extraction.components.map((component) => component.projectComponentId), [
    id('com.example.ui.AppButton'),
    ...extraction.components.filter((component) => component.name === 'AppChip').map((component) => component.projectComponentId),
    id('com.example.ui.AppIcon'),
    id('com.example.ui.ChipRow'),
    id('com.example.ui.PrimaryButton')
  ])
  assert.equal(extraction.components.some((component) => component.name === 'SecretButton'), false, 'private excluded')
  assert.equal(extraction.components.some((component) => component.name === 'InternalChrome'), false, 'internal excluded under visibility [public]')
  assert.equal(extraction.components.some((component) => component.name === 'AppButtonPreview'), false, '@Preview functions are never components')
})

check('CMP-KT-DISCOVERY: internal visibility is included exactly when configured', () => {
  const extraction = extract({ componentsConfig: { ...componentsConfig(), visibility: ['public', 'internal'] } })
  const internal = componentOf(extraction, 'InternalChrome')
  assert.ok(internal)
  assert.equal(internal.visibility, 'internal')
  assert.equal(extraction.components.some((component) => component.name === 'SecretButton'), false, 'private stays excluded')
})

check('CMP-KT-API: parameters carry kind/requiredness/defaults exactly', () => {
  const button = componentOf(extract(), 'AppButton')
  assert.deepEqual(
    button.api.parameters.map((parameter) => `${parameter.name}:${parameter.kind}:req=${parameter.required}`),
    [
      'label:text:req=true',
      'onClick:callback:req=true',
      'modifier:modifier:req=false',
      'size:value:req=false',
      'tone:value:req=false',
      'enabled:value:req=false',
      'state:state:req=false',
      'icon:content-lambda:req=false',
      'content:content-lambda:req=true'
    ]
  )
  assert.equal(button.api.parameters.find((parameter) => parameter.name === 'size').defaultText, 'ButtonSize.Small')
  assert.equal(button.api.parameters.find((parameter) => parameter.name === 'label').hasDefault, false)
})

check('CMP-KT-VARIANTS: enum, sealed, and boolean parameters become variant properties with defaults', () => {
  const button = componentOf(extract(), 'AppButton')
  assert.deepEqual(button.variantProperties, [
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
      projectPropertyId: 'param:enabled', name: 'enabled', source: 'boolean',
      values: [{ value: 'false' }, { value: 'true' }],
      defaultValue: 'true', defaultKnown: true
    }
  ])
})

check('CMP-KT-STATE: a State-suffixed user type is classified state, never a guessed variant', () => {
  const button = componentOf(extract(), 'AppButton')
  assert.equal(button.api.parameters.find((parameter) => parameter.name === 'state').kind, 'state')
  assert.equal(button.variantProperties.some((property) => property.projectPropertyId === 'param:state'), false)
})

check('CMP-KT-SLOTS: text/content/callback parameters become slots with exact requiredness', () => {
  const button = componentOf(extract(), 'AppButton')
  assert.deepEqual(button.slots, [
    { slotId: 'param:label', kind: 'text', name: 'label', required: true },
    { slotId: 'param:onClick', kind: 'callback', name: 'onClick', required: true },
    { slotId: 'param:icon', kind: 'content', name: 'icon', required: false },
    { slotId: 'param:content', kind: 'content', name: 'content', required: true }
  ])
})

check('CMP-KT-DEPS: nested in-scope calls resolve; token navigation resolves through the authorities', () => {
  const button = componentOf(extract(), 'AppButton')
  assert.deepEqual(button.dependencies, [
    { kind: 'component', targetProjectComponentId: id('com.example.ui.AppIcon'), symbol: 'AppIcon' },
    { kind: 'token', projectTokenId: `${ADAPTER_ID}:com.example.design.AppPalette.error400`, path: 'ds/src/AppButton.kt', line: 13 }
  ])
})

check('CMP-KT-DEPS: a call to an overloaded name stays an unresolved edge with a limitation', () => {
  const extraction = extract()
  const chipRow = componentOf(extraction, 'ChipRow')
  assert.deepEqual(chipRow.dependencies, [{ kind: 'component', symbol: 'AppChip' }])
  assert.ok(extraction.limitations.includes('ambiguous-nested-call:com.example.ui.ChipRow:AppChip'))
})

check('CMP-KT-OVERLOAD: overloads get unique deterministic discriminators', () => {
  const first = extract()
  const chips = first.components.filter((component) => component.name === 'AppChip')
  assert.equal(chips.length, 2)
  assert.ok(chips.every((chip) => /^[a-f0-9]{8}$/.test(chip.overloadDiscriminator)))
  assert.notEqual(chips[0].overloadDiscriminator, chips[1].overloadDiscriminator)
  const second = extract()
  assert.deepEqual(
    second.components.filter((component) => component.name === 'AppChip').map((chip) => chip.projectComponentId),
    chips.map((chip) => chip.projectComponentId)
  )
})

check('CMP-KT-WRAPPER: a single-delegation body is recorded as wrapperOf evidence', () => {
  const primary = componentOf(extract(), 'PrimaryButton')
  assert.equal(primary.wrapperOf, id('com.example.ui.AppButton'))
  assert.deepEqual(primary.dependencies, [
    { kind: 'component', targetProjectComponentId: id('com.example.ui.AppButton'), symbol: 'AppButton' }
  ])
  assert.equal(componentOf(extract(), 'AppButton').wrapperOf, undefined, 'a multi-statement body is never a wrapper')
})

check('CMP-KT-EVIDENCE: preview call-graph and screenshot-test files attach to the called components', () => {
  const button = componentOf(extract(), 'AppButton')
  assert.deepEqual(button.evidence.previews, [
    { symbol: 'AppButtonPreview', path: 'ds/previews/AppButtonPreviews.kt', line: 6 }
  ])
  assert.deepEqual(button.evidence.screenshotTests, [
    { path: 'ds/screenshot/AppButtonTest.kt', className: 'AppButtonScreenshotTest' }
  ])
  assert.deepEqual(componentOf(extract(), 'AppIcon').evidence.previews, [], 'evidence never leaks to uncalled components')
})

check('CMP-KT-SOURCE: source anchors carry the repo-relative path, FQ symbol, and file hash', () => {
  const button = componentOf(extract(), 'AppButton')
  assert.deepEqual(button.source, {
    path: 'ds/src/AppButton.kt', line: 13,
    symbol: 'com.example.ui.AppButton', fileHash: MAIN.hash
  })
})

check('CMP-KT-FAILCLOSED: a parse-error file lands in parseFailures and makes the witness incomplete', () => {
  const adapter = {
    id: ADAPTER_ID, kind: 'kotlin-compose', version: 2, enabled: true,
    capabilities: ['components'], platform: 'android-compose', authority: 'handwritten',
    components: componentsConfig(), tokens: tokensConfig()
  }
  const snapshot = {
    main: { files: [MAIN, ICON, BROKEN], rootsMissing: [], fingerprint: 'main' },
    previews: { files: [PREVIEW], rootsMissing: [], fingerprint: 'previews' },
    screenshots: { files: [SHOT], rootsMissing: [], fingerprint: 'screenshots' },
    branchKey: 'none'
  }
  const inventory = buildComponentInventory({ adapter, snapshot, configHash: 'sha256:' + 'd'.repeat(64) })
  assert.deepEqual(inventory.witness.parseFailures, [{ path: 'ds/src/Broken.kt', reason: 'parse error at 1:0' }])
  assert.equal(inventory.witness.filesParsed, 2)
  assert.equal(inventory.witness.complete, false, 'a parse failure must never masquerade as a complete scan')
  assert.equal(projectInventorySemanticError(inventory), null)
  const lineEndingOnly = structuredClone(inventory)
  for (const component of lineEndingOnly.components) component.source.fileHash = 'sha256:' + 'e'.repeat(64)
  assert.equal(projectInventorySemanticHash(lineEndingOnly), projectInventorySemanticHash(inventory),
    'exact source byte hashes are evidence, not semantic drift')
  assert.ok(inventory.components.length >= 5, 'other files still extract')
})

check('CMP-KT-DETERMINISM: reversed input file order serializes byte-identically', () => {
  const forward = extract()
  const reversed = extract({ files: [ICON, MAIN] })
  assert.equal(JSON.stringify(forward), JSON.stringify(reversed))
})

// ── component-manifest adapter ─────────────────────────────────────────────
const CORE_MANIFEST = file('web/components/core.json', JSON.stringify({
  schemaVersion: 2,
  components: [
    {
      name: 'Button', symbol: 'ui/button', visibility: 'public',
      props: [
        { name: 'size', kind: 'enum', values: ['small', 'large'], default: 'small' },
        { name: 'disabled', kind: 'boolean', default: false },
        { name: 'label', kind: 'text', required: true },
        { name: 'icon', kind: 'content' },
        { name: 'onClick', kind: 'callback', required: true }
      ],
      uses: { components: ['ui/icon'], tokens: ['fixture-json:palette.error.400'], framework: ['react'] },
      evidence: { screenshotTests: ['src/button.test.tsx'] }
    }
  ]
}))
const ICONS_MANIFEST = file('web/components/icons.json', JSON.stringify({
  schemaVersion: 2,
  components: [{ name: 'Icon', symbol: 'ui/icon', visibility: 'public', props: [] }]
}))
const extractManifest = (files) => extractManifestComponents({
  files, componentsConfig: {}, adapterId: 'manifest-ds', platform: 'web'
})

check('CMP-MANIFEST: a valid manifest extracts inventory rows with props, slots, and deps', () => {
  const extraction = extractManifest([CORE_MANIFEST, ICONS_MANIFEST])
  assert.deepEqual(extraction.parseFailures, [])
  assert.deepEqual(extraction.components.map((component) => component.projectComponentId), [
    'manifest-ds:symbol:ui/button', 'manifest-ds:symbol:ui/icon'
  ])
  const button = extraction.components[0]
  assert.equal(button.kind, 'manifest-component')
  assert.deepEqual(button.variantProperties, [
    {
      projectPropertyId: 'param:size', name: 'size', source: 'enum',
      values: [{ value: 'small' }, { value: 'large' }],
      defaultValue: 'small', defaultKnown: true
    },
    {
      projectPropertyId: 'param:disabled', name: 'disabled', source: 'boolean',
      values: [{ value: 'false' }, { value: 'true' }],
      defaultValue: 'false', defaultKnown: true
    }
  ])
  assert.deepEqual(button.slots, [
    { slotId: 'param:label', kind: 'text', name: 'label', required: true },
    { slotId: 'param:icon', kind: 'content', name: 'icon', required: false },
    { slotId: 'param:onClick', kind: 'callback', name: 'onClick', required: true }
  ])
  assert.deepEqual(button.evidence.screenshotTests, [{ path: 'src/button.test.tsx' }])
})

check('CMP-MANIFEST: component deps resolve across manifests by exact symbol', () => {
  const extraction = extractManifest([CORE_MANIFEST, ICONS_MANIFEST])
  assert.deepEqual(extraction.components[0].dependencies, [
    { kind: 'component', targetProjectComponentId: 'manifest-ds:symbol:ui/icon', symbol: 'ui/icon' },
    { kind: 'token', projectTokenId: 'fixture-json:palette.error.400' },
    { kind: 'framework', symbol: 'react' }
  ])
  const alone = extractManifest([CORE_MANIFEST])
  assert.deepEqual(alone.components[0].dependencies[0], { kind: 'component', symbol: 'ui/icon' })
})

check('CMP-MANIFEST: malformed JSON is a parse failure, never an empty inventory', () => {
  const extraction = extractManifest([file('web/components/bad.json', '{ nope')])
  assert.equal(extraction.components.length, 0)
  assert.equal(extraction.parseFailures.length, 1)
  assert.match(extraction.parseFailures[0].reason, /not valid JSON/)
})

check('CMP-MANIFEST: a duplicate prop fails that component without poisoning others', () => {
  const duplicate = file('web/components/dup.json', JSON.stringify({
    schemaVersion: 2,
    components: [{ name: 'X', symbol: 'ui/x', visibility: 'public', props: [{ name: 'a', kind: 'boolean' }, { name: 'a', kind: 'text' }] }]
  }))
  const extraction = extractManifest([duplicate, ICONS_MANIFEST])
  assert.match(extraction.parseFailures[0].reason, /declared twice/)
  assert.deepEqual(extraction.components.map((component) => component.name), ['Icon'])
})

check('CMP-MANIFEST: an enum default outside its values is a parse failure', () => {
  const bad = file('web/components/enum.json', JSON.stringify({
    schemaVersion: 2,
    components: [{ name: 'Y', symbol: 'ui/y', visibility: 'public', props: [{ name: 's', kind: 'enum', values: ['a'], default: 'zzz' }] }]
  }))
  const extraction = extractManifest([bad])
  assert.match(extraction.parseFailures[0].reason, /default is outside its values/)
})

check('CMP-MANIFEST-DETERMINISM: reversed manifest order serializes byte-identically', () => {
  const forward = extractManifest([CORE_MANIFEST, ICONS_MANIFEST])
  const reversed = extractManifest([ICONS_MANIFEST, CORE_MANIFEST])
  assert.equal(JSON.stringify(forward), JSON.stringify(reversed))
})

console.log(`\ncomponent-kotlin-adapter.test: ${pass} pass, ${fail} fail`)
process.exit(fail === 0 ? 0 : 1)
