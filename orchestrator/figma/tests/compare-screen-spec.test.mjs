// Fixture self-test for compare-screen-spec.mjs — no Figma, no Gradle.
import { mkdtempSync, mkdirSync, writeFileSync as rawWriteFileSync, readFileSync, rmSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { tmpdir } from 'node:os'
import { fileURLToPath } from 'node:url'
import { execFileSync, spawnSync } from 'node:child_process'
import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { aggregateObservedTokens } from '../tokens/catalog-aggregator.mjs'
import { bindObservedTokens } from '../tokens/binder.mjs'
import { emptyMappingRegistry } from '../tokens/mapping-contract.mjs'
import { normalizeSourceCapture } from '../tokens/source-normalizer.mjs'
import { immutablePlan, validObservedCapture } from './observed-token-fixtures.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const COMPARE = join(HERE, '..', 'scripts', 'compare-screen-spec.mjs')
const EXTRACT = join(HERE, '..', 'scripts', 'extract-app-tokens.mjs')
const RESOLVE = join(HERE, '..', 'scripts', 'resolve-screen-spec.mjs')
const SPEC = join(HERE, 'spec', 'spec-valid.json')

const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }

function currentSpec(input) {
  if (input && input.schemaVersion === 2) return input
  const elements = (input.elements || []).map((element, index) => ({
    figmaNodeId: element.figmaNodeId || `fixture:${index + 2}`,
    ...element,
  }))
  return {
    schemaVersion: 2,
    ...input,
    source: { fileKey: 'fixture', nodeId: 'fixture:1' },
    rootNodeId: 'fixture-root',
    coordinateSystem: { units: 'dp', density: 1, origin: 'frame' },
    themeMetadata: { themeKey: input.theme },
    nodes: [
      { stableId: 'fixture-root', figmaNodeId: 'fixture:1', name: input.screen, role: 'screen', bboxDp: { x: 0, y: 0, ...input.frameSizeDp } },
      ...elements.map((element) => ({ ...element })),
    ],
    elements,
  }
}

function writeFileSync(file, data, options) {
  if (String(file).endsWith('.spec.json')) {
    const parsed = JSON.parse(Buffer.isBuffer(data) ? data.toString('utf8') : String(data))
    data = JSON.stringify(currentSpec(parsed), null, 2) + '\n'
  }
  return rawWriteFileSync(file, data, options)
}

function setup() {
  const ws = mkdtempSync(join(tmpdir(), 'spec-compare-'))
  const screens = join(ws, 'screens', 'TASK_1_fixture')
  const reports = join(ws, 'reports')
  mkdirSync(screens, { recursive: true }); mkdirSync(reports, { recursive: true })
  writeFileSync(join(screens, 'HomeScreen.spec.json'), readFileSync(SPEC, 'utf8'))
  const capture = validObservedCapture()
  const providerNames = [
    'surface.background', 'text.primary', 'colors.card.bg', 'colors.chip.bg',
    'colors.gauge.arc', 'colors.hard.fill', 'colors.primary', 'colors.text.primary',
    'success.600', 'success.alpha-700.10'
  ]
  const projectPath = (name) => name === 'surface.background'
    ? ['colors', 'surface', 'background']
    : name === 'text.primary' ? ['colors', 'text', 'primary'] : name.split('.')
  capture.observations = providerNames.map((providerName) => ({
    providerName, rawValue: '#111111', providerType: 'COLOR'
  }))
  capture.witness.observationCount = capture.observations.length
  const batch = normalizeSourceCapture(capture, Buffer.from(JSON.stringify(capture)), immutablePlan(capture))
  const observed = aggregateObservedTokens({
    scope: { fileKeyFingerprint: capture.source.fileKeyFingerprint, branchKey: capture.source.branchKey },
    batches: [batch],
    revision: 1
  })
  const adapter = {
    id: 'fixture', platform: 'shared',
    tokens: {
      contextMap: [{ when: capture.source.context, projectMode: 'shared' }],
      bindingRules: providerNames.map((name, index) => ({
        ruleId: 'fixture-' + index,
        kind: 'exact-path',
        tokenKind: 'color',
        providerPath: [name],
        projectPath: projectPath(name)
      }))
    }
  }
  const projectInventory = {
    adapterId: 'fixture',
    tokens: [...new Set(providerNames.map((name) => projectPath(name).join('.')))].map((path) => ({
      projectTokenId: 'fixture:' + path,
      kind: 'color',
      semanticPath: path.split('.')
    }))
  }
  const binding = bindObservedTokens({
    catalog: observed.catalog,
    projectInventories: [projectInventory],
    adapterConfig: { tokenConfigHash: 'sha256:' + '1'.repeat(64), enabledTokenAdapters: [adapter] },
    mappingRegistry: emptyMappingRegistry(observed.catalog.scope),
    projectAnalysisHash: 'sha256:' + '2'.repeat(64)
  })
  const catalogFile = join(ws, 'observed-token-catalog.json')
  const sourceIndexFile = join(ws, 'observed-token-source-index.json')
  const bindingFile = join(ws, 'token-binding-snapshot.json')
  rawWriteFileSync(catalogFile, JSON.stringify(observed.catalog, null, 2) + '\n')
  rawWriteFileSync(sourceIndexFile, JSON.stringify(observed.index, null, 2) + '\n')
  rawWriteFileSync(bindingFile, JSON.stringify(binding, null, 2) + '\n')
  process.env.FIGMA_OBSERVED_TOKEN_CATALOG = catalogFile
  process.env.FIGMA_OBSERVED_TOKEN_SOURCE_INDEX = sourceIndexFile
  process.env.FIGMA_TOKEN_BINDING_SNAPSHOT = bindingFile
  return { ws, screens, screensRoot: join(ws, 'screens'), reports }
}

// Current binding-row fixture factory. setName is a repeatable display label;
// the durable designComponentId and setNodeId are the only design identities.
function componentBindingV2(row) {
  const nodeId = String(row.setNodeId)
  return {
    designComponentId: `figma-component:${'a'.repeat(16)}:none:${nodeId}`,
    setNodeId: nodeId,
    setName: row.setName,
    mappingId: 'cmap-' + createHash('sha256').update('cmp:' + nodeId).digest('hex').slice(0, 24),
    implementations: [{
      adapterId: 'compose-ds',
      platform: 'android-compose',
      projectComponentId: `compose-ds:symbol:${row.fqName || row.component}`,
      ...(row.source ? { sourcePath: row.source } : {})
    }]
  }
}
// The component-evidence provenance note is now the durable designComponentId
// (not the display name), matching componentEvidenceSources in compare-screen-spec.mjs.
function evidenceNote(setNodeId) { return 'binding:' + componentBindingV2({ setNodeId }).designComponentId }
function writeBindings(ctx, { screens = [{ screenName: 'HomeScreen' }], components = [] } = {}) {
  writeFileSync(join(ctx.screens, 'bindings.json'), JSON.stringify({
    schemaVersion: 2, stem: 'TASK_1_fixture', screens, components: components.map(componentBindingV2)
  }, null, 2))
}

function validImplementationModel() {
  return {
    schemaVersion: 1,
    parser: { engine: 'tree-sitter-kotlin', grammar: 'kotlin' },
    inputs: { files: [], roots: [] },
    inputHashes: {},
    overall: 'PASS',
    issues: [],
    files: [],
    composables: [],
    calls: [],
    tokens: { paths: [], refs: {} },
    unresolvedRefs: []
  }
}

try {
  const good = setup()
  const goodImpl = join(good.ws, 'HomeScreen.kt')
  writeFileSync(goodImpl, `
    fun HomeScreen() {
      Box(Modifier.background(AppTokens.colors.surface.background).padding(horizontal = AppTokens.dp.dialog.horizontalPadding))
      Text(color = AppTokens.colors.text.primary, style = AppTokens.typography.h2())
    }
  `)
  const goodOut = execFileSync('node', [COMPARE, 'TASK_1_fixture', '--impl-file', goodImpl, '--gate'], {
    env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: good.screensRoot, FIGMA_REPORTS_DIR: good.reports },
  }).toString()
  const goodReport = JSON.parse(readFileSync(join(good.reports, 'spec-compare-TASK_1_fixture.json'), 'utf8'))
	  check('token-backed color implementation passes gate with unverified dp/typography warnings', () => {
	    assert.match(goodOut, /compare-screen-spec: TASK_1_fixture WARN/)
	    assert.equal(goodReport.overall, 'WARN')
	    assert.ok(goodReport.issues.find((i) => i.issueKind === 'DP_EVIDENCE_MISSING'))
	    assert.ok(goodReport.issues.find((i) => i.issueKind === 'TYPOGRAPHY_EVIDENCE_MISSING'))
	  })

  const extractOut = join(good.ws, 'app-tokens.json')
  execFileSync('node', [EXTRACT, '--file', goodImpl, '--out', extractOut])
  const extracted = JSON.parse(readFileSync(extractOut, 'utf8'))
	  check('extract-app-tokens captures AppTokens paths', () => assert.ok(extracted.tokens['colors.text.primary']))
	  const missingExtract = spawnSync('node', [EXTRACT, '--file', join(good.ws, 'missing.kt'), '--out', join(good.ws, 'missing-tokens.json')], { encoding: 'utf8' })
	  check('extract-app-tokens fails closed when an explicit implementation source is missing', () => {
	    assert.notEqual(missingExtract.status, 0)
	    assert.match(missingExtract.stderr, /implementation source is missing/)
	  })
	  const invalidUtfSource = join(good.ws, 'InvalidUtf.kt')
	  rawWriteFileSync(invalidUtfSource, Buffer.from([0xff, 0xfe, 0xfd]))
	  const invalidUtfExtract = spawnSync('node', [EXTRACT, '--file', invalidUtfSource, '--out', join(good.ws, 'invalid-utf-tokens.json')], { encoding: 'utf8' })
	  check('extract-app-tokens fails closed when implementation source is not valid UTF-8', () => {
	    assert.notEqual(invalidUtfExtract.status, 0)
	    assert.match(invalidUtfExtract.stderr, /not valid UTF-8/)
	  })
	  const commentsOnly = join(good.ws, 'CommentsOnly.kt')
	  writeFileSync(commentsOnly, `
	    // AppTokens.colors.text.primary appears only in this comment
	    val note = "AppTokens.colors.surface.background"
	  `)
	  const commentsOut = join(good.ws, 'comments-tokens.json')
	  execFileSync('node', [EXTRACT, '--file', commentsOnly, '--out', commentsOut])
	  const commentsExtracted = JSON.parse(readFileSync(commentsOut, 'utf8'))
	  check('extract-app-tokens ignores comments and strings', () => {
	    assert.deepEqual(commentsExtracted.files, [commentsOnly])
	    assert.equal(Object.keys(commentsExtracted.tokens).length, 0)
	  })

  const resolvedOut = join(good.ws, 'resolved.json')
  execFileSync('node', [RESOLVE, 'TASK_1_fixture', '--out', resolvedOut], { env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: good.screensRoot } })
  const resolved = JSON.parse(readFileSync(resolvedOut, 'utf8'))
  check('resolve-screen-spec normalizes token refs', () => assert.equal(resolved.specs[0].elements[0].fills[0].appToken, 'colors.surface.background'))

  const rootEvidence = setup()
  const rootDir = join(rootEvidence.ws, 'src')
  mkdirSync(rootDir, { recursive: true })
  const rootImpl = join(rootDir, 'HomeScreen.kt')
  writeFileSync(rootImpl, 'fun HomeScreen() { Box(Modifier.background(AppTokens.colors.surface.background)); Text(color = AppTokens.colors.text.primary) }')
  execFileSync('node', [COMPARE, 'TASK_1_fixture', '--impl-root', rootDir, '--gate'], {
    env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: rootEvidence.screensRoot, FIGMA_REPORTS_DIR: rootEvidence.reports },
    stdio: 'pipe',
  })
  const rootReport = JSON.parse(readFileSync(join(rootEvidence.reports, 'spec-compare-TASK_1_fixture.json'), 'utf8'))
  check('compare-screen-spec inputHashes include files discovered through --impl-root', () => assert.ok(rootReport.inputHashes[rootImpl]))
  rmSync(rootEvidence.ws, { recursive: true, force: true })

  const appTokensEvidence = setup()
  const appTokensPath = join(appTokensEvidence.ws, 'app-tokens.json')
  writeFileSync(appTokensPath, JSON.stringify({
    tokens: { 'colors.surface.background': { refs: [] }, 'colors.text.primary': { refs: [] } },
    raw: { colors: {}, dp: {}, sp: {}, fontWeights: {} },
    files: []
  }, null, 2))
  execFileSync('node', [COMPARE, 'TASK_1_fixture', '--gate'], {
    env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: appTokensEvidence.screensRoot, FIGMA_REPORTS_DIR: appTokensEvidence.reports, FIGMA_APP_TOKENS: appTokensPath },
    stdio: 'pipe',
  })
  const appTokensReport = JSON.parse(readFileSync(join(appTokensEvidence.reports, 'spec-compare-TASK_1_fixture.json'), 'utf8'))
  check('compare-screen-spec inputHashes include FIGMA_APP_TOKENS evidence file', () => assert.ok(appTokensReport.inputHashes[appTokensPath]))
  rmSync(appTokensEvidence.ws, { recursive: true, force: true })

  const exactTokenValues = setup()
  const exactTokensPath = join(exactTokenValues.ws, 'app-tokens-with-values.json')
  writeFileSync(exactTokensPath, JSON.stringify({
    schemaVersion: 1,
    tokens: {
      'colors.surface.background': { refs: [{ file: 'HomeScreen.kt', line: 1 }] },
      'colors.text.primary': { refs: [{ file: 'HomeScreen.kt', line: 2 }] },
      'dp.dialog.horizontalPadding': { value: 16, refs: [{ file: 'HomeScreen.kt', line: 1 }] },
      'typography.h2': { value: { sizeSp: 20, lineHeightSp: 28, weight: 700 }, refs: [{ file: 'HomeScreen.kt', line: 2 }] }
    },
    raw: { colors: {}, dp: {}, sp: {}, fontWeights: {} },
    files: []
  }, null, 2))
  execFileSync('node', [COMPARE, 'TASK_1_fixture', '--gate'], {
    env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: exactTokenValues.screensRoot, FIGMA_REPORTS_DIR: exactTokenValues.reports, FIGMA_APP_TOKENS: exactTokensPath },
    stdio: 'pipe',
  })
  const exactTokenReport = JSON.parse(readFileSync(join(exactTokenValues.reports, 'spec-compare-TASK_1_fixture.json'), 'utf8'))
  check('token-backed dp and typography pass when exact values are proven', () => {
    assert.equal(exactTokenReport.overall, 'PASS')
    assert.equal(exactTokenReport.implementation.tokenValueCount, 2)
    assert.equal(exactTokenReport.issues.some((i) => i.issueKind === 'DP_EVIDENCE_MISSING' || i.issueKind === 'TYPOGRAPHY_EVIDENCE_MISSING'), false)
  })
  rmSync(exactTokenValues.ws, { recursive: true, force: true })

  const wrongTokenValues = setup()
  const wrongTokensPath = join(wrongTokenValues.ws, 'app-tokens-with-wrong-values.json')
  writeFileSync(wrongTokensPath, JSON.stringify({
    schemaVersion: 1,
    tokens: {
      'colors.surface.background': { refs: [] },
      'colors.text.primary': { refs: [] },
      'dp.dialog.horizontalPadding': { value: 12, refs: [] },
      'typography.h2': { value: { sizeSp: 18, lineHeightSp: 24, weight: 600 }, refs: [] }
    },
    raw: { colors: {}, dp: {}, sp: {}, fontWeights: {} },
    files: []
  }, null, 2))
  execFileSync('node', [COMPARE, 'TASK_1_fixture', '--gate'], {
    env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: wrongTokenValues.screensRoot, FIGMA_REPORTS_DIR: wrongTokenValues.reports, FIGMA_APP_TOKENS: wrongTokensPath },
    stdio: 'pipe',
  })
  const wrongTokenReport = JSON.parse(readFileSync(join(wrongTokenValues.reports, 'spec-compare-TASK_1_fixture.json'), 'utf8'))
  check('wrong token values stay WARN instead of becoming a false PASS', () => {
    assert.equal(wrongTokenReport.overall, 'WARN')
    assert.ok(wrongTokenReport.issues.find((i) => i.issueKind === 'DP_EVIDENCE_MISSING'))
    assert.ok(wrongTokenReport.issues.find((i) => i.issueKind === 'TYPOGRAPHY_EVIDENCE_MISSING'))
  })
  rmSync(wrongTokenValues.ws, { recursive: true, force: true })

  rmSync(good.ws, { recursive: true, force: true })

  const bad = setup()
  const badImpl = join(bad.ws, 'HomeScreen.kt')
  writeFileSync(badImpl, 'fun HomeScreen() { Text(color = AppTokens.colors.surface.background) }')
  let status = 0
  try {
    execFileSync('node', [COMPARE, 'TASK_1_fixture', '--impl-file', badImpl, '--gate'], {
      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: bad.screensRoot, FIGMA_REPORTS_DIR: bad.reports },
      stdio: 'pipe',
    })
  } catch (e) { status = e.status }
  const badReport = JSON.parse(readFileSync(join(bad.reports, 'spec-compare-TASK_1_fixture.json'), 'utf8'))
  check('missing expected color token blocks gate', () => { assert.equal(status, 1); assert.equal(badReport.overall, 'BLOCKER'); assert.match(JSON.stringify(badReport), /MISSING_COLOR_TOKEN/) })
  rmSync(bad.ws, { recursive: true, force: true })

  const missing = setup()
  let missingStatus = 0
  try {
    execFileSync('node', [COMPARE, 'TASK_1_fixture', '--impl-file', join(missing.ws, 'Missing.kt'), '--gate'], {
      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: missing.screensRoot, FIGMA_REPORTS_DIR: missing.reports },
      stdio: 'pipe',
    })
  } catch (e) { missingStatus = e.status }
  const missingReport = JSON.parse(readFileSync(join(missing.reports, 'spec-compare-TASK_1_fixture.json'), 'utf8'))
  check('missing implementation file blocks gate', () => { assert.equal(missingStatus, 1); assert.match(JSON.stringify(missingReport), /IMPLEMENTATION_FILE_MISSING/) })
	  rmSync(missing.ws, { recursive: true, force: true })

	  const rawColor = setup()
	  const rawSpec = {
	    screen: 'HomeScreen',
	    frameSizeDp: { w: 100, h: 200 },
	    theme: 'light',
	    elements: [{ stableId: 'cta', name: 'CTA', bboxDp: { x: 0, y: 0, w: 10, h: 10 }, fills: ['#FF0000'] }]
	  }
	  writeFileSync(join(rawColor.screens, 'HomeScreen.spec.json'), JSON.stringify(rawSpec, null, 2))
	  const tokenOnlyImpl = join(rawColor.ws, 'HomeScreen.kt')
	  writeFileSync(tokenOnlyImpl, 'fun HomeScreen() { Text(color = AppTokens.colors.text.primary) }')
	  const rawOut = execFileSync('node', [COMPARE, 'TASK_1_fixture', '--impl-file', tokenOnlyImpl, '--gate'], {
	    env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: rawColor.screensRoot, FIGMA_REPORTS_DIR: rawColor.reports },
	  }).toString()
	  const rawReport = JSON.parse(readFileSync(join(rawColor.reports, 'spec-compare-TASK_1_fixture.json'), 'utf8'))
	  check('raw color expectation is not suppressed by unrelated color token usage', () => {
	    assert.match(rawOut, /COLOR_EVIDENCE_MISSING/)
	    assert.equal(rawReport.overall, 'WARN')
	  })
	  rmSync(rawColor.ws, { recursive: true, force: true })

	  // #53 pin: 8-digit spec hex must be able to match implementation color evidence.
	  const alphaColor = setup()
	  writeFileSync(join(alphaColor.screens, 'HomeScreen.spec.json'), JSON.stringify({
	    screen: 'HomeScreen',
	    frameSizeDp: { w: 100, h: 200 },
	    theme: 'light',
	    elements: [
	      { stableId: 'scrim', name: 'Scrim', bboxDp: { x: 0, y: 0, w: 100, h: 200 }, fills: ['#3B82F680'] },
	      { stableId: 'cta', name: 'CTA', bboxDp: { x: 0, y: 0, w: 10, h: 10 }, fills: ['#FF0000FF'] }
	    ]
	  }, null, 2))
	  const alphaImpl = join(alphaColor.ws, 'HomeScreen.kt')
	  writeFileSync(alphaImpl, 'fun HomeScreen() { Box(Modifier.background(Color(0x803B82F6))); Text(color = Color(0xFFFF0000)) }')
	  const alphaOut = execFileSync('node', [COMPARE, 'TASK_1_fixture', '--impl-file', alphaImpl, '--gate'], {
	    env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: alphaColor.screensRoot, FIGMA_REPORTS_DIR: alphaColor.reports },
	  }).toString()
	  const alphaReport = JSON.parse(readFileSync(join(alphaColor.reports, 'spec-compare-TASK_1_fixture.json'), 'utf8'))
	  check('8-digit spec hex matches translucent and full-alpha raw color evidence', () => {
	    assert.doesNotMatch(alphaOut, /COLOR_EVIDENCE_MISSING/)
	    assert.equal(alphaReport.overall, 'PASS')
	  })
	  const alphaEvidenceOut = join(alphaColor.ws, 'alpha-tokens.json')
	  execFileSync('node', [EXTRACT, '--file', alphaImpl, '--out', alphaEvidenceOut])
	  const alphaEvidence = JSON.parse(readFileSync(alphaEvidenceOut, 'utf8'))
	  check('extract-app-tokens stores an #RRGGBBAA key for translucent literals only', () => {
	    assert.ok(alphaEvidence.raw.colors['#3B82F6'])
	    assert.ok(alphaEvidence.raw.colors['#3B82F680'])
	    assert.ok(alphaEvidence.raw.colors['#FF0000'])
	    assert.equal(alphaEvidence.raw.colors['#FF0000FF'], undefined)
	  })
	  rmSync(alphaColor.ws, { recursive: true, force: true })

	  // Wrong alpha must NOT count as color evidence for a translucent spec value.
	  const wrongAlpha = setup()
	  writeFileSync(join(wrongAlpha.screens, 'HomeScreen.spec.json'), JSON.stringify({
	    screen: 'HomeScreen',
	    frameSizeDp: { w: 100, h: 200 },
	    theme: 'light',
	    elements: [{ stableId: 'scrim', name: 'Scrim', bboxDp: { x: 0, y: 0, w: 100, h: 200 }, fills: ['#3B82F680'] }]
	  }, null, 2))
	  const wrongAlphaImpl = join(wrongAlpha.ws, 'HomeScreen.kt')
	  writeFileSync(wrongAlphaImpl, 'fun HomeScreen() { Box(Modifier.background(Color(0xFF3B82F6))) }')
	  const wrongAlphaOut = execFileSync('node', [COMPARE, 'TASK_1_fixture', '--impl-file', wrongAlphaImpl, '--gate'], {
	    env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: wrongAlpha.screensRoot, FIGMA_REPORTS_DIR: wrongAlpha.reports },
	  }).toString()
	  check('right RGB with wrong alpha still reports COLOR_EVIDENCE_MISSING', () => {
	    assert.match(wrongAlphaOut, /COLOR_EVIDENCE_MISSING/)
	  })
	  rmSync(wrongAlpha.ws, { recursive: true, force: true })

	  const invalidV2 = setup()
	  const invalidSpec = {
	    schemaVersion: 2,
	    screen: 'HomeScreen',
	    frameSizeDp: { w: 100, h: 200 },
	    theme: 'light',
	    source: { fileKey: 'file', nodeId: '1:2' },
	    rootNodeId: 'root',
	    coordinateSystem: { units: 'dp' },
	    themeMetadata: { themeKey: 'primary' },
	    nodes: [{ stableId: 'root', figmaNodeId: '1:2', name: 'HomeScreen', bboxDp: { x: 0, y: 0, w: 100, h: 200 } }],
	    elements: []
	  }
	  writeFileSync(join(invalidV2.screens, 'HomeScreen.spec.json'), JSON.stringify(invalidSpec, null, 2))
	  const invalidImpl = join(invalidV2.ws, 'HomeScreen.kt')
	  writeFileSync(invalidImpl, 'fun HomeScreen() { Text(color = AppTokens.colors.text.primary) }')
	  let invalidStatus = 0
	  try {
	    execFileSync('node', [COMPARE, 'TASK_1_fixture', '--impl-file', invalidImpl, '--gate'], {
	      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: invalidV2.screensRoot, FIGMA_REPORTS_DIR: invalidV2.reports },
	      stdio: 'pipe',
	    })
	  } catch (e) { invalidStatus = e.status }
	  const invalidReport = JSON.parse(readFileSync(join(invalidV2.reports, 'spec-compare-TASK_1_fixture.json'), 'utf8'))
		  check('compare-screen-spec blocks invalid v2 zero-projection specs', () => {
		    assert.equal(invalidStatus, 1)
		    assert.equal(invalidReport.overall, 'BLOCKER')
		    assert.match(JSON.stringify(invalidReport.issues), /SPEC_INVALID/)
			  })
			  rmSync(invalidV2.ws, { recursive: true, force: true })

			  const componentV2 = setup()
			  const componentSpec = {
			    schemaVersion: 2,
			    screen: 'HomeScreen',
			    frameSizeDp: { w: 100, h: 200 },
			    theme: 'light',
			    source: { fileKey: 'file', nodeId: '1:2' },
			    rootNodeId: 'root',
			    coordinateSystem: { units: 'dp' },
			    themeMetadata: { themeKey: 'primary' },
			    nodes: [
			      { stableId: 'root', figmaNodeId: '1:2', name: 'HomeScreen', bboxDp: { x: 0, y: 0, w: 100, h: 200 } },
			      { stableId: 'cta', figmaNodeId: '1:3', name: 'CTA', bboxDp: { x: 0, y: 0, w: 80, h: 32 }, componentSetName: 'PrimaryButton', componentSetNodeId: '1:30', variantProps: { Size: 'Large' } }
			    ],
			    elements: [
			      { stableId: 'cta', figmaNodeId: '1:3', name: 'CTA', bboxDp: { x: 0, y: 0, w: 80, h: 32 }, componentSetName: 'PrimaryButton', componentSetNodeId: '1:30', variantProps: { Size: 'Large' } }
			    ]
			  }
			  writeFileSync(join(componentV2.screens, 'HomeScreen.spec.json'), JSON.stringify(componentSpec, null, 2))
			  const componentImpl = join(componentV2.ws, 'HomeScreen.kt')
			  writeFileSync(componentImpl, 'fun HomeScreen() { Text(color = AppTokens.colors.text.primary) }')
			  writeBindings(componentV2, { components: [{ setNodeId: '1:30', setName: 'PrimaryButton', component: 'PrimaryButton', fqName: 'fixture.PrimaryButton', source: componentImpl }] })
			  const componentResolvedOut = join(componentV2.ws, 'resolved-component.json')
			  execFileSync('node', [RESOLVE, 'TASK_1_fixture', '--out', componentResolvedOut], {
			    env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: componentV2.screensRoot },
			    stdio: 'pipe',
			  })
			  const componentResolved = JSON.parse(readFileSync(componentResolvedOut, 'utf8'))
			  check('resolve-screen-spec preserves durable componentSetNodeId for exact binding', () => {
			    assert.equal(componentResolved.specs[0].elements[0].componentSetNodeId, '1:30')
			  })
			  let componentStatus = 0
			  try {
			    execFileSync('node', [COMPARE, 'TASK_1_fixture', '--impl-file', componentImpl, '--gate'], {
			      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: componentV2.screensRoot, FIGMA_REPORTS_DIR: componentV2.reports },
			      stdio: 'pipe',
			    })
			  } catch (e) { componentStatus = e.status }
			  const componentReport = JSON.parse(readFileSync(join(componentV2.reports, 'spec-compare-TASK_1_fixture.json'), 'utf8'))
				  check('compare-screen-spec blocks unverified v2 component variants', () => {
				    assert.equal(componentStatus, 1)
				    assert.equal(componentReport.overall, 'BLOCKER')
				    assert.match(JSON.stringify(componentReport.issues), /COMPONENT_MODEL_REQUIRED|COMPONENT_VARIANT_ASSIGNMENT_UNPROVEN/)
				  })
					  rmSync(componentV2.ws, { recursive: true, force: true })

				  const ownerModelWs = setup()
				  writeFileSync(join(ownerModelWs.screens, 'HomeScreen.spec.json'), JSON.stringify({
				    screen: 'HomeScreen',
				    frameSizeDp: { w: 100, h: 200 },
				    theme: 'light',
				    elements: [{ stableId: 'cta', name: 'CTA', bboxDp: { x: 0, y: 0, w: 80, h: 32 }, componentSetName: 'PrimaryButton', componentSetNodeId: '1:30' }]
				  }, null, 2))
				  const ownerImpl = join(ownerModelWs.ws, 'HomeScreen.kt')
				  const ownerModelPath = join(ownerModelWs.ws, 'implementation-model.json')
				  writeFileSync(ownerImpl, 'fun HomeScreen() {}')
				  writeBindings(ownerModelWs, { components: [{ setNodeId: '1:30', setName: 'PrimaryButton', component: 'PrimaryButton', fqName: 'fixture.PrimaryButton', source: ownerImpl }] })
				  writeFileSync(ownerModelPath, JSON.stringify({ ...validImplementationModel(), calls: [{ callee: 'PrimaryButton', file: ownerImpl, line: 1, raw: 'PrimaryButton()', owner: 'SettingsScreen' }] }, null, 2))
				  let ownerStatus = 0
				  try {
				    execFileSync('node', [COMPARE, 'TASK_1_fixture', '--impl-file', ownerImpl, '--impl-model', ownerModelPath, '--gate'], {
				      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: ownerModelWs.screensRoot, FIGMA_REPORTS_DIR: ownerModelWs.reports },
				      stdio: 'pipe',
				    })
				  } catch (e) { ownerStatus = e.status }
				  const ownerReport = JSON.parse(readFileSync(join(ownerModelWs.reports, 'spec-compare-TASK_1_fixture.json'), 'utf8'))
				  check('component call in another screen owner does not satisfy current screen', () => {
				    assert.equal(ownerStatus, 1)
				    assert.ok(ownerReport.issues.find((i) => i.issueKind === 'MISSING_COMPONENT_CALL'))
				  })
				  rmSync(ownerModelWs.ws, { recursive: true, force: true })

				  // W2-3(a): the MVI-conventional `<Frame>Screen` composable in the screen's DECLARED
				  // implementation file BINDS (file scope + compatible-owner suffix) — the TASK_215
				  // production false-BLOCKER shape. The REAL extractor produces the model, which also
				  // pins the owner-closure emission end-to-end.
				  const sfx = setup()
				  rmSync(join(sfx.screens, 'HomeScreen.spec.json'))
				  writeFileSync(join(sfx.screens, 'Foo.spec.json'), JSON.stringify({
				    screen: 'Foo',
				    frameSizeDp: { w: 100, h: 200 },
				    theme: 'light',
				    elements: [
				      { stableId: 'cta', name: 'CTA', bboxDp: { x: 0, y: 0, w: 80, h: 32 }, componentSetName: 'PrimaryButton', componentSetNodeId: '1:30' },
				      { stableId: 'toggle', name: 'Push', bboxDp: { x: 0, y: 40, w: 40, h: 24 }, componentSetName: 'Toggle', componentSetNodeId: '1:31' }
				    ]
				  }, null, 2))
				  const sfxImpl = join(sfx.ws, 'FooScreen.kt')
				  writeFileSync(sfxImpl, [
				    '@Composable',
				    'fun FooScreen() {',
				    '  Text(color = AppTokens.colors.text.primary)',
				    '  PrimaryButton()',
				    '  DeliveryChannelsCard()',
				    '}',
				    '@Composable',
				    'private fun DeliveryChannelsCard() {',
				    '  Toggle()',
				    '}',
				    '@Composable fun PrimaryButton() {}',
				    '@Composable fun Toggle() {}',
				  ].join('\n'))
				  const sfxModelPath = join(sfx.ws, 'implementation-model.json')
				  execFileSync('node', [join(HERE, '..', 'scripts', 'extract-compose-model.mjs'), '--out', sfxModelPath, '--file', sfxImpl], { stdio: 'pipe' })
				  const sfxModel = JSON.parse(readFileSync(sfxModelPath, 'utf8'))
				  writeFileSync(join(sfx.screens, 'bindings.json'), JSON.stringify({
				    schemaVersion: 2, stem: 'TASK_1_fixture',
				    screens: [{ screenName: 'Foo', implFile: sfxImpl, composable: 'FooScreen' }],
				    components: [
				      componentBindingV2({ setNodeId: '1:30', setName: 'PrimaryButton', fqName: 'fixture.PrimaryButton', source: sfxImpl }),
				      componentBindingV2({ setNodeId: '1:31', setName: 'Toggle', fqName: 'fixture.Toggle', source: sfxImpl }),
				    ],
				  }, null, 2))
				  check('W2-3 owner-closure: the extractor re-attributes a private sub-composable call to its public invoker', () => {
				    const closure = (sfxModel.calls || []).find((c) => c.callee === 'Toggle' && c.owner === 'FooScreen' && c.ownerVia === 'DeliveryChannelsCard')
				    assert.ok(closure, `expected a Toggle closure entry owned by FooScreen via DeliveryChannelsCard, got ${JSON.stringify((sfxModel.calls || []).filter((c) => c.callee === 'Toggle'))}`)
				    assert.ok((sfxModel.calls || []).some((c) => c.callee === 'Toggle' && c.owner === 'DeliveryChannelsCard' && !c.ownerVia), 'the direct owner entry must survive alongside the closure entry')
				  })
				  let sfxStatus = 0
				  try {
				    execFileSync('node', [COMPARE, 'TASK_1_fixture', '--impl-file', sfxImpl, '--impl-model', sfxModelPath, '--gate'], {
				      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: sfx.screensRoot, FIGMA_REPORTS_DIR: sfx.reports },
				      stdio: 'pipe',
				    })
				  } catch (e) { sfxStatus = e.status }
				  const sfxReport = JSON.parse(readFileSync(join(sfx.reports, 'spec-compare-TASK_1_fixture.json'), 'utf8'))
				  check('W2-3 binding: frame `Foo` binds `FooScreen`-owned and sub-composable-owned components (no MISSING_COMPONENT_CALL)', () => {
				    assert.ok(!sfxReport.issues.some((i) => i.issueKind === 'MISSING_COMPONENT_CALL'), `unexpected MISSING_COMPONENT_CALL: ${JSON.stringify(sfxReport.issues.filter((i) => i.issueKind === 'MISSING_COMPONENT_CALL'))}`)
				    assert.equal(sfxStatus, 0, `expected exit 0, got ${sfxStatus}: ${JSON.stringify(sfxReport.issues.filter((i) => i.severity === 'BLOCKER'))}`)
				  })
				  rmSync(sfx.ws, { recursive: true, force: true })

				  // W2-4: different Figma-set and Kotlin names require an explicit task binding.
				  // Registry labels and name similarity are deliberately not identity paths.
				  const brg = setup()
				  rmSync(join(brg.screens, 'HomeScreen.spec.json'))
				  writeFileSync(join(brg.screens, 'Foo.spec.json'), JSON.stringify({
				    screen: 'Foo',
				    frameSizeDp: { w: 100, h: 200 },
				    theme: 'light',
				    elements: [{ stableId: 'banner', name: 'Banner', bboxDp: { x: 0, y: 0, w: 80, h: 32 }, componentSetName: 'Banners', componentSetNodeId: '3:4' }]
				  }, null, 2))
				  const brgImpl = join(brg.ws, 'FooScreen.kt')
				  writeFileSync(brgImpl, '@Composable\nfun FooScreen() {\n  Text(color = AppTokens.colors.text.primary)\n  Banner()\n}\n@Composable fun Banner() {}\n')
				  const brgModelPath = join(brg.ws, 'implementation-model.json')
				  execFileSync('node', [join(HERE, '..', 'scripts', 'extract-compose-model.mjs'), '--out', brgModelPath, '--file', brgImpl], { stdio: 'pipe' })
				  writeFileSync(join(brg.screens, 'bindings.json'), JSON.stringify({
				    schemaVersion: 2, stem: 'TASK_1_fixture',
				    screens: [{ screenName: 'Foo', implFile: brgImpl, composable: 'FooScreen' }],
				    components: [],
				  }, null, 2))
				  const brgRun = () => {
				    let status = 0
				    try {
				      execFileSync('node', [COMPARE, 'TASK_1_fixture', '--impl-file', brgImpl, '--impl-model', brgModelPath, '--gate'], {
				        env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: brg.screensRoot, FIGMA_REPORTS_DIR: brg.reports },
				        stdio: 'pipe',
				      })
				    } catch (e) { status = e.status }
				    return { status, report: JSON.parse(readFileSync(join(brg.reports, 'spec-compare-TASK_1_fixture.json'), 'utf8')) }
				  }
				  const brgWithout = brgRun()
				  check('W2-4 without an explicit component binding: set `Banners` blocks', () => {
				    assert.ok(brgWithout.report.issues.some((i) => i.issueKind === 'COMPONENT_BINDING_REQUIRED'), JSON.stringify(brgWithout.report.issues))
				  })
				  writeFileSync(join(brg.screens, 'bindings.json'), JSON.stringify({
				    schemaVersion: 2, stem: 'TASK_1_fixture',
				    screens: [{ screenName: 'Foo', implFile: brgImpl, composable: 'FooScreen' }],
				    components: [componentBindingV2({ setNodeId: '3:4', setName: 'Banners', fqName: 'fixture.Banner', source: brgImpl })],
				  }, null, 2))
				  const brgWith = brgRun()
				  check('W2-4 explicit task binding: `Banners` resolves to exact `Banner` and binds', () => {
				    assert.ok(!brgWith.report.issues.some((i) => i.issueKind === 'MISSING_COMPONENT_CALL'), JSON.stringify(brgWith.report.issues.filter((i) => i.issueKind === 'MISSING_COMPONENT_CALL')))
				    assert.ok(!brgWith.report.issues.some((i) => i.issueKind === 'COMPONENT_BINDING_REQUIRED'))
				    assert.equal(brgWith.status, 0, JSON.stringify(brgWith.report.issues.filter((i) => i.severity === 'BLOCKER')))
				  })
				  rmSync(brg.ws, { recursive: true, force: true })

				  // W3-1/W3-2: component-boundary color symmetry. One fixture, four pins:
				  //   (1) instance-descendant (`I…;…` figmaNodeId) unresolved token → WARN COMPONENT_COLOR_DELEGATED
				  //   (2) registry-matched reused set whose internals use a DIFFERENT token → WARN delegated
				  //   (3) W3-1 evidence merge: reused set whose source USES the expected token → NO issue at all
				  //   (4) ORDERING PIN: a hardcode contradiction on an instance-internal node stays a BLOCKER
				  //       (delegation must never launder HARDCODED_COLOR_FOR_TOKEN).
				  const dlg = setup()
				  rmSync(join(dlg.screens, 'HomeScreen.spec.json'))
				  writeFileSync(join(dlg.screens, 'Foo.spec.json'), JSON.stringify({
				    screen: 'Foo',
				    frameSizeDp: { w: 100, h: 200 },
				    theme: 'light',
				    elements: [
				      { stableId: 'desc', name: 'BadgeLabel', figmaNodeId: 'I9:9;104:1', bboxDp: { x: 0, y: 0, w: 40, h: 16 }, fills: ['{success.600}'] },
				      { stableId: 'badge', name: 'Badge', componentSetName: 'Badge', componentSetNodeId: '9:10', bboxDp: { x: 0, y: 20, w: 80, h: 24 }, fills: ['{success.alpha-700.10}'] },
				      { stableId: 'chip', name: 'Chip', componentSetName: 'Chip', componentSetNodeId: '9:11', bboxDp: { x: 0, y: 50, w: 60, h: 24 }, fills: ['{colors.chip.bg}'] },
				      { stableId: 'hard', name: 'HardLabel', figmaNodeId: 'I9:9;104:2', bboxDp: { x: 0, y: 80, w: 40, h: 16 }, fills: [{ tokenRef: '{colors.hard.fill}', resolvedValue: '#FF0000' }] }
				    ]
				  }, null, 2))
				  const dlgImpl = join(dlg.ws, 'FooScreen.kt')
				  writeFileSync(dlgImpl, '@Composable\nfun FooScreen() {\n  Text(color = AppTokens.colors.text.primary)\n  Box(color = Color(0xFFFF0000))\n  Badge()\n  Chip()\n}\n')
				  const dlgBadge = join(dlg.ws, 'Badge.kt')
				  writeFileSync(dlgBadge, 'fun Badge() { Text(color = AppTokens.colors.badge.green.contentOn) }\n')
				  const dlgChip = join(dlg.ws, 'Chip.kt')
				  writeFileSync(dlgChip, 'fun Chip() { Box(color = AppTokens.colors.chip.bg) }\n')
				  writeBindings(dlg, {
				    screens: [{ screenName: 'Foo', implFile: dlgImpl, composable: 'FooScreen' }],
				    components: [
				      { setNodeId: '9:10', setName: 'Badge', component: 'Badge', fqName: 'fixture.Badge', source: dlgBadge },
				      { setNodeId: '9:11', setName: 'Chip', component: 'Chip', fqName: 'fixture.Chip', source: dlgChip },
				    ],
				  })
				  const dlgModelPath = join(dlg.ws, 'implementation-model.json')
				  writeFileSync(dlgModelPath, JSON.stringify({ ...validImplementationModel(), calls: [
				    { callee: 'Badge', file: dlgImpl, line: 5, raw: 'Badge()', owner: 'FooScreen' },
				    { callee: 'Chip', file: dlgImpl, line: 6, raw: 'Chip()', owner: 'FooScreen' }
				  ] }, null, 2))
				  let dlgStatus = 0
				  try {
				    execFileSync('node', [COMPARE, 'TASK_1_fixture', '--impl-file', dlgImpl, '--impl-model', dlgModelPath, '--gate'], {
				      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: dlg.screensRoot, FIGMA_REPORTS_DIR: dlg.reports },
				      stdio: 'pipe',
				    })
				  } catch (e) { dlgStatus = e.status }
				  const dlgReport = JSON.parse(readFileSync(join(dlg.reports, 'spec-compare-TASK_1_fixture.json'), 'utf8'))
				  check('W3-2: instance-descendant + reused-set unresolved tokens → WARN COMPONENT_COLOR_DELEGATED (never MISSING_COLOR_TOKEN)', () => {
				    const delegated = dlgReport.issues.filter((i) => i.issueKind === 'COMPONENT_COLOR_DELEGATED')
				    assert.ok(delegated.some((i) => i.stableId === 'desc'), `descendant not delegated: ${JSON.stringify(dlgReport.issues)}`)
				    assert.ok(delegated.some((i) => i.stableId === 'badge'), 'reused-set element not delegated')
				    assert.ok(delegated.every((i) => i.severity === 'WARN'))
				    assert.ok(!dlgReport.issues.some((i) => i.issueKind === 'MISSING_COLOR_TOKEN'), JSON.stringify(dlgReport.issues.filter((i) => i.issueKind === 'MISSING_COLOR_TOKEN')))
				  })
				  check('W3-1: a reused set whose SOURCE uses the expected token clears via the evidence merge (no issue)', () => {
				    assert.ok(!dlgReport.issues.some((i) => i.stableId === 'chip' && (i.issueKind === 'COMPONENT_COLOR_DELEGATED' || i.issueKind === 'MISSING_COLOR_TOKEN')), JSON.stringify(dlgReport.issues.filter((i) => i.stableId === 'chip')))
				    assert.deepEqual(Object.values(dlgReport.inputs.componentEvidenceSources || {}).sort(), [evidenceNote('9:10'), evidenceNote('9:11')].sort())
				  })
				  check('W3-2 ordering pin: a hardcode contradiction on an instance-internal node stays a BLOCKER (exit 1)', () => {
				    const hard = dlgReport.issues.find((i) => i.issueKind === 'HARDCODED_COLOR_FOR_TOKEN' && i.stableId === 'hard')
				    assert.ok(hard && hard.severity === 'BLOCKER', JSON.stringify(dlgReport.issues.filter((i) => i.stableId === 'hard')))
				    assert.equal(dlgStatus, 1)
				  })
				  rmSync(dlg.ws, { recursive: true, force: true })

				  // index.json drives the gate for the canonical mixed-node shape: one SCREEN node + a
				  // component-kind sub-frame node + a stale leftover spec. The component node must not
				  // force multi-screen impl mapping; the leftover is skipped with a WARN (cache hygiene
				  // is check-screen-cache's), and the run needs exactly ONE impl file.
				  const idxWs = setup()
				  rmSync(join(idxWs.screens, 'HomeScreen.spec.json'))
				  const idxSpec = (name, fill) => ({ screen: name, frameSizeDp: { w: 100, h: 200 }, theme: 'light', elements: [{ stableId: name.toLowerCase(), name, bboxDp: { x: 0, y: 0, w: 10, h: 10 }, fills: [fill] }] })
				  writeFileSync(join(idxWs.screens, 'Main.spec.json'), JSON.stringify(idxSpec('Main', '{colors.text.primary}'), null, 2))
				  writeFileSync(join(idxWs.screens, 'CardA.spec.json'), JSON.stringify(idxSpec('CardA', '{colors.card.bg}'), null, 2))
				  writeFileSync(join(idxWs.screens, 'Leftover.spec.json'), JSON.stringify(idxSpec('Leftover', '{colors.text.primary}'), null, 2))
				  const fetchedAt = '2026-01-01T00:00:00.000Z'
				  const indexNode = (screen, nodeId, kind) => {
				    const url = `https://www.figma.com/design/FileKey?node-id=${nodeId.replace(':', '-')}`
				    const tokensFile = `${screen}.tokens.json`
				    const tokenBytes = Buffer.from('{}\n')
				    rawWriteFileSync(join(idxWs.screens, tokensFile), tokenBytes)
				    return { kind, url, nodeId, fetchedAt, variants: [{
				      id: 'primary', theme: 'light', locale: 'default', platform: 'shared',
				      url, nodeId, fetchedAt, imageFile: `${screen}.png`, specFile: `${screen}.spec.json`,
				      captureOperationId: 'tokop_' + '1'.repeat(16), captureSequence: 1,
				      tokensFile, tokensHash: 'sha256:' + createHash('sha256').update(tokenBytes).digest('hex')
				    }] }
				  }
				  writeFileSync(join(idxWs.screens, 'index.json'), JSON.stringify({ schemaVersion: 3, taskStem: 'TASK_1_fixture', nodes: { Main: indexNode('Main', '1:1', 'screen'), CardA: indexNode('CardA', '2:2', 'component') } }, null, 2))
				  const idxImpl = join(idxWs.ws, 'Main.kt')
				  writeFileSync(idxImpl, 'fun Main() { Text(color = AppTokens.colors.text.primary); Box(color = AppTokens.colors.card.bg) }\n')
				  let idxStatus = 0
				  try {
				    execFileSync('node', [COMPARE, 'TASK_1_fixture', '--impl-file', idxImpl, '--gate'], {
				      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: idxWs.screensRoot, FIGMA_REPORTS_DIR: idxWs.reports },
				      stdio: 'pipe',
				    })
				  } catch (e) { idxStatus = e.status }
				  const idxReport = JSON.parse(readFileSync(join(idxWs.reports, 'spec-compare-TASK_1_fixture.json'), 'utf8'))
				  check('component-kind node does not force multi-screen mapping; stale non-indexed spec blocks', () => {
				    assert.ok(!idxReport.issues.some((i) => i.issueKind === 'MULTI_SCREEN_IMPL_MAPPING_REQUIRED'), JSON.stringify(idxReport.issues))
				    const stale = idxReport.issues.find((i) => i.issueKind === 'SPEC_NOT_IN_INDEX')
				    assert.ok(stale && stale.screen === 'Leftover' && stale.severity === 'BLOCKER')
				    assert.equal(idxStatus, 1)
				  })
				  rmSync(idxWs.ws, { recursive: true, force: true })

				  // Screen maps require exact readable paths; filenames are never searched under roots.
				  const smWs = setup()
				  rmSync(join(smWs.screens, 'HomeScreen.spec.json'))
				  writeFileSync(join(smWs.screens, 'Alpha.spec.json'), JSON.stringify(idxSpec('Alpha', '{colors.text.primary}'), null, 2))
				  writeFileSync(join(smWs.screens, 'Beta.spec.json'), JSON.stringify(idxSpec('Beta', '{colors.text.primary}'), null, 2))
				  const smSrc = join(smWs.ws, 'src')
				  mkdirSync(smSrc, { recursive: true })
				  writeFileSync(join(smSrc, 'AlphaImpl.kt'), 'fun AlphaImpl() { Text(color = AppTokens.colors.text.primary) }\n')
				  writeFileSync(join(smSrc, 'BetaImpl.kt'), 'fun BetaImpl() { Text(color = AppTokens.colors.text.primary) }\n')
				  let smStatus = 0
				  try {
				    execFileSync('node', [COMPARE, 'TASK_1_fixture', '--impl-root', smSrc, '--screen-map', `Alpha=${join(smSrc, 'AlphaImpl.kt')}`, '--screen-map', `Beta=${join(smSrc, 'BetaImpl.kt')}`, '--gate'], {
				      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: smWs.screensRoot, FIGMA_REPORTS_DIR: smWs.reports },
				      stdio: 'pipe',
				    })
				  } catch (e) { smStatus = e.status }
				  const smReport = JSON.parse(readFileSync(join(smWs.reports, 'spec-compare-TASK_1_fixture.json'), 'utf8'))
				  check('exact --screen-map paths are accepted without filename recovery', () => {
				    assert.ok(!smReport.issues.some((i) => i.issueKind === 'IMPLEMENTATION_FILE_MISSING'), JSON.stringify(smReport.issues))
				    assert.equal(smStatus, 0, JSON.stringify(smReport.issues.filter((i) => i.severity === 'BLOCKER')))
				  })
				  rmSync(smWs.ws, { recursive: true, force: true })

				  // W3-4 second half: bare-basename resolution is UNIQUE-match only — two candidate files
				  // sharing the basename must keep the fail-closed blocker (a guessed pick could bind the
				  // wrong screen's implementation and certify against the wrong file).
				  const ambWs = setup()
				  rmSync(join(ambWs.screens, 'HomeScreen.spec.json'))
				  writeFileSync(join(ambWs.screens, 'Alpha.spec.json'), JSON.stringify(idxSpec('Alpha', '{colors.text.primary}'), null, 2))
				  const ambSrc = join(ambWs.ws, 'src')
				  mkdirSync(join(ambSrc, 'a'), { recursive: true })
				  mkdirSync(join(ambSrc, 'b'), { recursive: true })
				  writeFileSync(join(ambSrc, 'a', 'AlphaImpl.kt'), 'fun AlphaImpl() { Text(color = AppTokens.colors.text.primary) }\n')
				  writeFileSync(join(ambSrc, 'b', 'AlphaImpl.kt'), 'fun AlphaImpl() { Text(color = AppTokens.colors.text.primary) }\n')
				  let ambStatus = 0
				  try {
				    execFileSync('node', [COMPARE, 'TASK_1_fixture', '--impl-root', ambSrc, '--screen-map', 'Alpha=AlphaImpl.kt', '--gate'], {
				      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: ambWs.screensRoot, FIGMA_REPORTS_DIR: ambWs.reports },
				      stdio: 'pipe',
				    })
				  } catch (e) { ambStatus = e.status }
				  const ambReport = JSON.parse(readFileSync(join(ambWs.reports, 'spec-compare-TASK_1_fixture.json'), 'utf8'))
				  check('W3-4: an AMBIGUOUS bare basename (two same-named candidates) still blocks (exit 1)', () => {
				    assert.ok(ambReport.issues.some((i) => i.issueKind === 'IMPLEMENTATION_FILE_MISSING'), JSON.stringify(ambReport.issues))
				    assert.equal(ambStatus, 1)
				  })
				  rmSync(ambWs.ws, { recursive: true, force: true })

				  // W3-1 census leg: a census MAPPED row's registry.source feeds the evidence scope even
				  // with NO --registry — and the leg is demand-scoped + fail-closed: deleting the census
				  // report restores the MISSING_COLOR_TOKEN blocker unchanged.
				  const cenWs = setup()
				  rmSync(join(cenWs.screens, 'HomeScreen.spec.json'))
				  writeFileSync(join(cenWs.screens, 'Census.spec.json'), JSON.stringify({
				    screen: 'Census',
				    frameSizeDp: { w: 100, h: 200 },
				    theme: 'light',
				    elements: [
				      { stableId: 'gauge', name: 'Gauge', componentSetName: 'Gauge', componentSetNodeId: '8:8', bboxDp: { x: 0, y: 0, w: 80, h: 24 }, fills: ['{colors.gauge.arc}'] }
				    ]
				  }, null, 2))
				  const cenImpl = join(cenWs.ws, 'Census.kt')
				  writeFileSync(cenImpl, 'fun Census() { Text(color = AppTokens.colors.text.primary)\n  Gauge() }\n')
				  const cenGauge = join(cenWs.ws, 'Gauge.kt')
				  writeFileSync(cenGauge, 'fun Gauge() { Arc(color = AppTokens.colors.gauge.arc) }\n')
				  writeBindings(cenWs, {
				    screens: [{ screenName: 'Census', implFile: cenImpl, composable: 'Census' }],
				    components: [{ setNodeId: '8:8', setName: 'Gauge', component: 'Gauge', fqName: 'fixture.Gauge', source: cenGauge }],
				  })
				  const cenCensus = join(cenWs.reports, 'census-TASK_1_fixture.json')
				  writeFileSync(cenCensus, JSON.stringify({
				    taskStem: 'TASK_1_fixture',
				    components: [{ component: 'Gauge', status: 'MAPPED', screens: ['Census'], instances: 1, registry: { component: 'Gauge', source: cenGauge } }]
				  }, null, 2))
				  const cenModelPath = join(cenWs.ws, 'implementation-model.json')
				  writeFileSync(cenModelPath, JSON.stringify({ ...validImplementationModel(), calls: [
				    { callee: 'Gauge', file: cenImpl, line: 1, raw: 'Gauge()', owner: 'Census' }
				  ] }, null, 2))
				  const cenRun = () => {
				    let status = 0
				    try {
				      execFileSync('node', [COMPARE, 'TASK_1_fixture', '--impl-file', cenImpl, '--impl-model', cenModelPath, '--gate'], {
				        env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: cenWs.screensRoot, FIGMA_REPORTS_DIR: cenWs.reports },
				        stdio: 'pipe',
				      })
				    } catch (e) { status = e.status }
				    return { status, report: JSON.parse(readFileSync(join(cenWs.reports, 'spec-compare-TASK_1_fixture.json'), 'utf8')) }
				  }
				  const cenWith = cenRun()
				  check('explicit binding source clears the component token without a registry-side name join', () => {
				    assert.ok(!cenWith.report.issues.some((i) => i.issueKind === 'MISSING_COLOR_TOKEN' || i.issueKind === 'COMPONENT_COLOR_DELEGATED'), JSON.stringify(cenWith.report.issues))
				    assert.equal(cenWith.status, 0, JSON.stringify(cenWith.report.issues.filter((i) => i.severity === 'BLOCKER')))
				    assert.deepEqual(Object.values(cenWith.report.inputs.componentEvidenceSources || {}), [evidenceNote('8:8')])
				  })
				  rmSync(cenCensus)
				  const cenWithout = cenRun()
				  check('removing a census report cannot change the explicit binding evidence scope', () => {
				    assert.ok(!cenWithout.report.issues.some((i) => i.issueKind === 'MISSING_COLOR_TOKEN' || i.issueKind === 'COMPONENT_COLOR_DELEGATED'), JSON.stringify(cenWithout.report.issues))
				    assert.deepEqual(Object.values(cenWithout.report.inputs.componentEvidenceSources || {}), [evidenceNote('8:8')])
				    assert.equal(cenWithout.status, 0)
				  })
				  rmSync(cenWs.ws, { recursive: true, force: true })

				  // Multi-screen file names are not identity; every screen needs an explicit map.
				  const inf = setup()
				  rmSync(join(inf.screens, 'HomeScreen.spec.json'))
				  const infSpec = (name) => ({ screen: name, frameSizeDp: { w: 100, h: 200 }, theme: 'light', elements: [{ stableId: 't', name: 'T', bboxDp: { x: 0, y: 0, w: 10, h: 10 }, fills: ['{colors.text.primary}'] }] })
				  writeFileSync(join(inf.screens, 'Alpha.spec.json'), JSON.stringify(infSpec('Alpha'), null, 2))
				  writeFileSync(join(inf.screens, 'Beta.spec.json'), JSON.stringify(infSpec('Beta'), null, 2))
				  const infSrc = join(inf.ws, 'src')
				  mkdirSync(infSrc, { recursive: true })
				  writeFileSync(join(infSrc, 'AlphaScreen.kt'), 'fun AlphaScreen() { Text(color = AppTokens.colors.text.primary) }\n')
				  writeFileSync(join(infSrc, 'BetaScreen.kt'), 'fun BetaScreen() { Text(color = AppTokens.colors.text.primary) }\n')
				  let infStatus = 0
				  try {
				    execFileSync('node', [COMPARE, 'TASK_1_fixture', '--impl-root', infSrc, '--gate'], {
				      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: inf.screensRoot, FIGMA_REPORTS_DIR: inf.reports },
				      stdio: 'pipe',
				    })
				  } catch (e) { infStatus = e.status }
				  const infReport = JSON.parse(readFileSync(join(inf.reports, 'spec-compare-TASK_1_fixture.json'), 'utf8'))
				  check('matching `<Screen>Screen.kt` filenames do not auto-map multi-screen identity', () => {
				    assert.ok(infReport.issues.some((i) => i.issueKind === 'MULTI_SCREEN_IMPL_MAPPING_REQUIRED'), JSON.stringify(infReport.issues))
				    assert.equal(infStatus, 1)
				  })
				  rmSync(inf.ws, { recursive: true, force: true })

					  const multiScreen = setup()
				  writeFileSync(join(multiScreen.screens, 'SettingsScreen.spec.json'), JSON.stringify({
				    screen: 'SettingsScreen',
				    frameSizeDp: { w: 100, h: 200 },
				    theme: 'light',
				    elements: [{ stableId: 'settings-title', name: 'Title', bboxDp: { x: 0, y: 0, w: 10, h: 10 }, fills: ['{colors.text.primary}'] }]
				  }, null, 2))
				  const multiImpl = join(multiScreen.ws, 'HomeScreen.kt')
				  writeFileSync(multiImpl, `
				    fun HomeScreen() {
				      Text(color = AppTokens.colors.text.primary)
				    }
				  `)
				  let multiStatus = 0
				  try {
				    execFileSync('node', [COMPARE, 'TASK_1_fixture', '--impl-file', multiImpl, '--gate'], {
				      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: multiScreen.screensRoot, FIGMA_REPORTS_DIR: multiScreen.reports },
				      stdio: 'pipe',
				    })
				  } catch (e) { multiStatus = e.status }
					  const multiReport = JSON.parse(readFileSync(join(multiScreen.reports, 'spec-compare-TASK_1_fixture.json'), 'utf8'))
					  check('compare-screen-spec fails closed for multi-screen stems without impl mapping', () => {
					    assert.equal(multiStatus, 1)
					    assert.ok(multiReport.issues.find((i) => i.issueKind === 'MULTI_SCREEN_IMPL_MAPPING_REQUIRED'))
					  })
					  rmSync(multiScreen.ws, { recursive: true, force: true })

					  const mappedMulti = setup()
					  writeFileSync(join(mappedMulti.screens, 'SettingsScreen.spec.json'), JSON.stringify({
					    screen: 'SettingsScreen',
					    frameSizeDp: { w: 100, h: 200 },
					    theme: 'light',
					    elements: [{ stableId: 'settings-title', name: 'Title', bboxDp: { x: 0, y: 0, w: 10, h: 10 }, fills: ['{colors.text.primary}'] }]
					  }, null, 2))
					  const mappedHome = join(mappedMulti.ws, 'HomeScreen.kt')
					  const mappedSettings = join(mappedMulti.ws, 'SettingsScreen.kt')
					  writeFileSync(mappedHome, 'fun HomeScreen() { Box(Modifier.background(AppTokens.colors.surface.background)); Text(color = AppTokens.colors.text.primary) }')
					  writeFileSync(mappedSettings, 'fun SettingsScreen() { Text(color = AppTokens.colors.text.primary) }')
					  execFileSync('node', [COMPARE, 'TASK_1_fixture', '--impl-file', mappedHome, '--screen-map', `SettingsScreen=${mappedSettings}`, '--gate'], {
					    env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: mappedMulti.screensRoot, FIGMA_REPORTS_DIR: mappedMulti.reports },
					    stdio: 'pipe',
					  })
					  const mappedReport = JSON.parse(readFileSync(join(mappedMulti.reports, 'spec-compare-TASK_1_fixture.json'), 'utf8'))
					  check('compare-screen-spec accepts explicit multi-screen implementation mapping', () => {
					    assert.ok(String(mappedReport.inputs.screenMap.SettingsScreen).endsWith('SettingsScreen.kt'))
					    assert.equal(mappedReport.issues.some((i) => i.issueKind === 'MULTI_SCREEN_IMPL_MAPPING_REQUIRED'), false)
					  })
					  rmSync(mappedMulti.ws, { recursive: true, force: true })

					  const dualWarn = setup()
					  writeFileSync(join(dualWarn.screens, 'HomeScreen.spec.json'), JSON.stringify({
					    screen: 'HomeScreen',
					    frameSizeDp: { w: 100, h: 200 },
					    theme: 'light',
					    elements: [{ stableId: 'title', name: 'Title', bboxDp: { x: 0, y: 0, w: 10, h: 10 }, fills: ['{colors.text.primary}'] }]
					  }, null, 2))
					  const dualWarnImpl = join(dualWarn.ws, 'HomeScreen.kt')
					  const dualWarnModel = join(dualWarn.ws, 'implementation-model.json')
					  writeFileSync(dualWarnImpl, 'fun HomeScreen() { Text(color = AppTokens.colors.text.primary) }')
					  writeFileSync(dualWarnModel, JSON.stringify({ ...validImplementationModel(), unresolvedRefs: [{ kind: 'UNRESOLVED_COMPONENT_CALL', callee: 'Ghost' }] }, null, 2))
					  let unresolvedStatus = 0
					  try {
					    execFileSync('node', [COMPARE, 'TASK_1_fixture', '--impl-file', dualWarnImpl, '--impl-model', dualWarnModel, '--gate'], {
					      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: dualWarn.screensRoot, FIGMA_REPORTS_DIR: dualWarn.reports },
					      stdio: 'pipe',
					    })
					  } catch (e) { unresolvedStatus = e.status }
					  const dualWarnReport = JSON.parse(readFileSync(join(dualWarn.reports, 'spec-compare-TASK_1_fixture.json'), 'utf8'))
					  check('current comparator blocks unresolved implementation references', () => {
					    assert.equal(unresolvedStatus, 1)
					    assert.equal(dualWarnReport.overall, 'BLOCKER')
					    assert.ok(dualWarnReport.issues.some((i) => i.issueKind === 'IMPLEMENTATION_MODEL_UNRESOLVED_REFS'))
					  })
					  rmSync(dualWarn.ws, { recursive: true, force: true })

				  const mixedEmpty = setup()
		  writeFileSync(join(mixedEmpty.screens, 'EmptyScreen.spec.json'), JSON.stringify({
		    screen: 'EmptyScreen',
		    frameSizeDp: { w: 100, h: 200 },
		    theme: 'light',
		    elements: []
		  }, null, 2))
		  const mixedImpl = join(mixedEmpty.ws, 'HomeScreen.kt')
		  writeFileSync(mixedImpl, `
		    fun HomeScreen() {
		      Box(Modifier.background(AppTokens.colors.surface.background).padding(horizontal = AppTokens.dp.dialog.horizontalPadding))
		      Text(color = AppTokens.colors.text.primary, style = AppTokens.typography.h2())
		    }
		  `)
		  let mixedStatus = 0
		  try {
		    execFileSync('node', [COMPARE, 'TASK_1_fixture', '--impl-file', mixedImpl, '--gate'], {
		      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: mixedEmpty.screensRoot, FIGMA_REPORTS_DIR: mixedEmpty.reports },
		      stdio: 'pipe',
		    })
		  } catch (e) { mixedStatus = e.status }
		  const mixedReport = JSON.parse(readFileSync(join(mixedEmpty.reports, 'spec-compare-TASK_1_fixture.json'), 'utf8'))
		  check('compare-screen-spec blocks any individual empty screen spec in a multi-screen stem', () => {
		    assert.equal(mixedStatus, 1)
		    assert.equal(mixedReport.overall, 'BLOCKER')
		    assert.ok(mixedReport.issues.find((i) => i.issueKind === 'SPEC_INVALID' && i.file === 'EmptyScreen.spec.json'))
		  })
		  rmSync(mixedEmpty.ws, { recursive: true, force: true })

	  // --- R2: element-scoped binding + contradiction (component instance bound to ONE code call) ---
	  function r2Run(specData, calls, implSrc) {
	    const fixture = setup()
	    const ws = fixture.ws, screens = fixture.screens, reports = fixture.reports
	    writeFileSync(join(screens, 'HomeScreen.spec.json'), JSON.stringify(specData))
	    const implFile = join(ws, 'HomeScreen.kt'); writeFileSync(implFile, implSrc)
	    writeFileSync(join(screens, 'bindings.json'), JSON.stringify({
	      schemaVersion: 2,
	      stem: 'TASK_1_fixture',
	      screens: [{ screenName: 'HomeScreen', implFile, composable: 'HomeScreen' }],
	      components: [componentBindingV2({ setNodeId: '1:30', setName: 'PrimaryButton', fqName: 'fixture.PrimaryButton', source: implFile })],
	    }, null, 2))
	    const model = validImplementationModel(); model.calls = calls.map((call) => ({ ...call, file: call.file === 'HomeScreen.kt' ? implFile : call.file }))
	    const modelPath = join(ws, 'model.json'); writeFileSync(modelPath, JSON.stringify(model))
	    let status = 0, out = ''
	    try { out = execFileSync('node', [COMPARE, 'TASK_1_fixture', '--impl-file', implFile, '--impl-model', modelPath, '--gate'], { env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: fixture.screensRoot, FIGMA_REPORTS_DIR: reports } }).toString() }
	    catch (e) { status = e.status; out = (e.stdout || '').toString() }
	    const report = JSON.parse(readFileSync(join(reports, 'spec-compare-TASK_1_fixture.json'), 'utf8'))
	    rmSync(ws, { recursive: true, force: true })
	    return { status, out, report }
	  }
	  const r2Spec = (extra) => currentSpec({ screen: 'HomeScreen', theme: 'light', frameSizeDp: { w: 100, h: 200 }, elements: [{ stableId: 'cta', name: 'Cta', bboxDp: { x: 0, y: 0, w: 40, h: 20 }, componentSetName: 'PrimaryButton', componentSetNodeId: '1:30', ...extra }] })
	  const r2Call = (args, line = 5) => ({ callee: 'PrimaryButton', file: 'HomeScreen.kt', owner: 'HomeScreen', line, raw: 'PrimaryButton(...)', ...(args ? { args } : {}) })
	  const IMPL_TOKEN = 'fun HomeScreen() { PrimaryButton(color = AppTokens.colors.primary) }'
	  const r2Binding = (r) => r.report.comparisons.find((c) => c.stableId === 'cta').binding

	  // (a) element-precise HARDCODED-COLOR HINT — WARN (never a false BLOCKER). The bound call passes
	  //     the raw hex the spec token resolves to AND does not use the token at the call → advisory.
	  const r2Hardcoded = r2Run(r2Spec({ fills: [{ tokenRef: '{colors.primary}', resolvedValue: '#0000FFFF' }] }), [r2Call({ colors: ['#0000FFFF'] })], IMPL_TOKEN)
	  check('R2: a bound call hardcoding the spec token colour (no token at call) → ELEMENT_HARDCODED_COLOR_HINT WARN', () => {
	    const hit = r2Hardcoded.report.issues.find((i) => i.issueKind === 'ELEMENT_HARDCODED_COLOR_HINT')
	    assert.ok(hit && hit.severity === 'WARN', 'element-precise hint is advisory, never a blocker')
	    assert.match(hit.message, /Cta/)
	    assert.equal(r2Binding(r2Hardcoded), 'resolved')
	    assert.ok(!r2Hardcoded.report.issues.some((i) => i.severity === 'BLOCKER'), 'no element-scoped BLOCKER (deadlock-safe)')
	  })
	  // (b) the SAME element bound to a TOKEN-correct call (no raw colour) → no hint.
	  const r2Correct = r2Run(r2Spec({ fills: [{ tokenRef: '{colors.primary}', resolvedValue: '#0000FFFF' }] }), [r2Call({ tokens: ['AppColor.primary'] })], IMPL_TOKEN)
	  check('R2: the same element bound to a token-correct call raises NO hint', () => {
	    assert.ok(!r2Correct.report.issues.some((i) => i.issueKind === 'ELEMENT_HARDCODED_COLOR_HINT'), 'no hint when the token is used')
	    assert.equal(r2Binding(r2Correct), 'resolved')
	  })
	  // (b2) ADVERSARIAL REGRESSION: the bound call uses the token for the FILL and passes a
	  //      coincidental raw colour on an UNRELATED arg (ripple/gradient) → the token-guard suppresses
	  //      the hint. This was a confirmed false-BLOCKER; it must not even WARN now.
	  const r2Coincidental = r2Run(r2Spec({ fills: [{ tokenRef: '{colors.primary}', resolvedValue: '#0000FFFF' }] }), [r2Call({ tokens: ['AppColor.primary'], colors: ['#0000FFFF'] })], IMPL_TOKEN)
	  check('R2 regression: token-at-call + coincidental raw on an unrelated arg → NO hint (token-guard)', () => {
	    assert.ok(!r2Coincidental.report.issues.some((i) => i.issueKind === 'ELEMENT_HARDCODED_COLOR_HINT'), 'guard suppresses the coincidence')
	    assert.ok(!r2Coincidental.report.issues.some((i) => i.severity === 'BLOCKER'))
	  })
	  // (b3) ADVERSARIAL REGRESSION: a TRANSLUCENT raw colour (alpha ≠ FF) does NOT match a SOLID fill
	  //      token — alpha is compared on both sides. This was a confirmed false-match.
	  const r2Translucent = r2Run(r2Spec({ fills: [{ tokenRef: '{colors.primary}', resolvedValue: '#0000FFFF' }] }), [r2Call({ colors: ['#0000FF80'] })], IMPL_TOKEN)
	  check('R2 regression: a translucent raw colour never matches a solid fill token (alpha compared)', () => {
	    assert.ok(!r2Translucent.report.issues.some((i) => i.issueKind === 'ELEMENT_HARDCODED_COLOR_HINT'), 'alpha distinguishes scrim from fill')
	  })
	  // (c) An AMBIGUOUS binding (the component is called twice) is unresolved and fails closed.
	  const r2Ambiguous = r2Run(r2Spec({ fills: [{ tokenRef: '{colors.primary}', resolvedValue: '#0000FFFF' }] }), [r2Call({ colors: ['#0000FF'] }, 5), r2Call({ colors: ['#0000FF'] }, 9)], IMPL_TOKEN)
	  check('R2: an ambiguous binding is a BLOCKER (ELEMENT_BINDING_UNRESOLVED)', () => {
	    const blocker = r2Ambiguous.report.issues.find((i) => i.issueKind === 'ELEMENT_BINDING_UNRESOLVED')
	    assert.ok(blocker && blocker.severity === 'BLOCKER', 'unresolved binding must block')
	    assert.equal(r2Binding(r2Ambiguous), 'unresolved')
	  })
	  // (d) element-precise DP divergence — advisory WARN (spec dp may be component-internal, not a
	  //     call arg) + composed-equivalence carve-out + delegated carve-out. Never a false BLOCKER.
	  const r2Padding = { l: 0, t: 16, r: 0, b: 0 }
	  const r2DpBad = r2Run(r2Spec({ paddingDp: r2Padding }), [r2Call({ dp: [4] })], IMPL_TOKEN)
	  const r2DpComposed = r2Run(r2Spec({ paddingDp: r2Padding }), [r2Call({ dp: [8, 8] })], IMPL_TOKEN)
	  const r2DpDelegated = r2Run(r2Spec({ paddingDp: r2Padding }), [r2Call({ tokens: ['AppColor.primary'] })], IMPL_TOKEN)
	  check('R2: a bound call whose dp args diverge from the spec dp → ELEMENT_DP_DIVERGENCE WARN (never a false BLOCKER)', () => {
	    const hit = r2DpBad.report.issues.find((i) => i.issueKind === 'ELEMENT_DP_DIVERGENCE')
	    assert.ok(hit && hit.severity === 'WARN', 'dp divergence is advisory, not a blocker')
	    assert.ok(!r2DpBad.report.issues.some((i) => i.issueKind === 'ELEMENT_DP_DIVERGENCE' && i.severity === 'BLOCKER'))
	  })
	  check('R2 composed-equivalence: spec 16dp = code 8+8 is NOT flagged (subset sum matches)', () => {
	    assert.ok(!r2DpComposed.report.issues.some((i) => i.issueKind === 'ELEMENT_DP_DIVERGENCE'), 'composed sum matches')
	  })
	  check('R2 delegated: a bound call with NO dp args (delegated to the component default) is NOT flagged', () => {
	    assert.ok(!r2DpDelegated.report.issues.some((i) => i.issueKind === 'ELEMENT_DP_DIVERGENCE'), 'empty call dp is not a divergence')
	  })
  // --- Canvas widget class: hoisted-token alias handling (spec-valid.json fixture expects the
  // color tokens colors.surface.background + colors.text.primary) -------------------------------
  function canvasRun(implSrc) {
    const c = setup()
    const implFile = join(c.ws, 'HomeScreen.kt'); writeFileSync(implFile, implSrc)
    let status = 0, out = ''
    try { out = execFileSync('node', [COMPARE, 'TASK_1_fixture', '--impl-file', implFile, '--gate'], { env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: c.screensRoot, FIGMA_REPORTS_DIR: c.reports } }).toString() }
    catch (e) { status = e.status; out = (e.stdout || '').toString() }
    const report = JSON.parse(readFileSync(join(c.reports, 'spec-compare-TASK_1_fixture.json'), 'utf8'))
    rmSync(c.ws, { recursive: true, force: true })
    return { status, out, report }
  }
  const kinds = (r) => r.report.issues.map((i) => i.issueKind)

  // 1. Canvas + direct hoist → P2a resolves both tokens → NO color BLOCKER, NO fallback WARN, PASS gate.
  const canvasHoist = canvasRun(`
    fun HomeScreen() {
      val s = AppTokens.colors.surface
      val t = AppTokens.colors.text
      Canvas(Modifier) { drawRect(color = s.background); drawRect(color = t.primary) }
    }`)
  check('canvas + direct hoist: alias resolution clears MISSING_COLOR_TOKEN (gate passes, no downgrade)', () => {
    assert.equal(canvasHoist.status, 0)
    assert.ok(!kinds(canvasHoist).includes('MISSING_COLOR_TOKEN'))
    assert.equal(canvasHoist.report.widgetClasses.HomeScreen, 'canvas')   // labelled, informational
  })

  // W6-4: a TYPE-ANNOTATED hoist (`val s: AppColor.SurfaceColors = AppTokens.colors.surface`)
  // is idiomatic Kotlin — the resolver must not be brittle to the annotation form.
  const canvasTypedHoist = canvasRun(`
    fun HomeScreen() {
      val s: AppColor.SurfaceColors = AppTokens.colors.surface
      val t: AppColor.TextColors? = AppTokens.colors.text
      Canvas(Modifier) { drawRect(color = s.background); drawRect(color = t.primary) }
    }`)
  check('canvas + TYPE-ANNOTATED hoist resolves identically (no MISSING_COLOR_TOKEN)', () => {
    assert.equal(canvasTypedHoist.status, 0, JSON.stringify(canvasTypedHoist.report.issues.filter((i) => i.severity === 'BLOCKER')))
    assert.ok(!kinds(canvasTypedHoist).includes('MISSING_COLOR_TOKEN'))
  })

  // 2. Canvas + token genuinely absent → BLOCKER. The gate is not softened for canvas; the alias
  //    resolution only reconciles a REAL hoisted use.
  const canvasAbsent = canvasRun(`fun HomeScreen() { Canvas(Modifier) { drawRect(color = Color.Red) } }`)
  check('canvas + token absent: MISSING_COLOR_TOKEN BLOCKER (canvas is not an exemption)', () => {
    assert.equal(canvasAbsent.status, 1)
    assert.ok(kinds(canvasAbsent).includes('MISSING_COLOR_TOKEN'))
  })

  // 3. REGRESSION PIN (adversarial review finding): a canvas widget hoists the token but applies it
  //    via a SHADOWED lambda param (`labels.forEach { t -> ... t.primary }`), so colors.text.primary
  //    is NOT actually painted. The resolver must NOT credit the shadow → BLOCKER, not a false PASS.
  const canvasShadow = canvasRun(`
    fun HomeScreen() {
      val s = AppTokens.colors.surface
      val t = AppTokens.colors.text
      Canvas(Modifier) {
        drawRect(color = s.background)
        labels.forEach { t -> drawText(color = t.primary) }
      }
    }`)
  check('canvas + shadowed lambda param: token not really applied → MISSING_COLOR_TOKEN BLOCKER (no false PASS)', () => {
    assert.equal(canvasShadow.status, 1)
    assert.ok(kinds(canvasShadow).includes('MISSING_COLOR_TOKEN'))
  })

  // 3b. REGRESSION PIN (2nd review pass): the shadow via a DESTRUCTURING for-loop header
  //     `for ((t, x) in …)` must also be cut — `t.primary` there is the loop element, not the token.
  const canvasForShadow = canvasRun(`
    fun HomeScreen() {
      val s = AppTokens.colors.surface
      val t = AppTokens.colors.text
      Canvas(Modifier) {
        drawRect(color = s.background)
        for ((t, x) in series) { drawText(color = t.primary) }
      }
    }`)
  check('canvas + destructuring for-loop shadow: token not applied → MISSING_COLOR_TOKEN BLOCKER', () => {
    assert.equal(canvasForShadow.status, 1)
    assert.ok(kinds(canvasForShadow).includes('MISSING_COLOR_TOKEN'))
  })

  // 4. Non-direct hoist form (alias-of-alias) is NOT resolved → BLOCKER for canvas too (use a direct
  //    `val` hoist). Confirms there is no loose "indirect evidence" downgrade path.
  const canvasAliasOfAlias = canvasRun(`
    fun HomeScreen() {
      val base = AppTokens.colors
      val s = base.surface
      val t = base.text
      Canvas(Modifier) { drawRect(color = s.background); drawRect(color = t.primary) }
    }`)
  check('canvas + alias-of-alias: unresolved indirect hoist → MISSING_COLOR_TOKEN BLOCKER (no WARN downgrade)', () => {
    assert.equal(canvasAliasOfAlias.status, 1)
    assert.ok(kinds(canvasAliasOfAlias).includes('MISSING_COLOR_TOKEN'))
    assert.ok(!kinds(canvasAliasOfAlias).includes('CANVAS_COLOR_TOKEN_INDIRECT'))  // the removed WARN never appears
    assert.equal(canvasAliasOfAlias.report.widgetClasses.HomeScreen, 'canvas')
  })
		} catch (e) {
  fail++; console.log(`${C.red}FAIL${C.reset} fixture setup threw\n     ${e.stdout ? e.stdout.toString() : e.message}`)
}

console.log(`\ncompare-screen-spec.test: ${pass} pass, ${fail} fail`)
process.exit(fail === 0 ? 0 : 1)
