// End-to-end dry run for the local Figma comparison pipeline.
// No live Figma, no Gradle: the fixture synthesizes the artifacts a real pull/build would produce,
// then runs the same gate scripts and final freshness bundle.
import { cpSync, mkdtempSync, mkdirSync, readFileSync, renameSync, rmSync, symlinkSync, writeFileSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { tmpdir } from 'node:os'
import { fileURLToPath } from 'node:url'
import { execFileSync } from 'node:child_process'
import assert from 'node:assert/strict'
import { fileHash, writeReport } from '../scripts/report-utils.mjs'
import { parseFigmaMeta } from '../scripts/figma-meta.mjs'
import { bytesHash } from '../runtime/canonical-json.mjs'
import { aggregateObservedTokens } from '../tokens/catalog-aggregator.mjs'
import { bindObservedTokens } from '../tokens/binder.mjs'
import { emptyMappingRegistry } from '../tokens/mapping-contract.mjs'
import { normalizeSourceCapture } from '../tokens/source-normalizer.mjs'
import { immutablePlan, sourceIdentity, validObservedCapture } from './observed-token-fixtures.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const SOURCE_SCRIPTS = join(HERE, '..', 'scripts')
let SCRIPTS = SOURCE_SCRIPTS
const STEM = 'TASK_1_realDryRun'
const RUN_ID = 'pipeline-dry-run-1'
const FIGMA_URL = 'https://www.figma.com/design/fileKey?node-id=1-2'
process.env.FIGMA_PIPELINE_RUN_ID = RUN_ID
const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }

let Jimp
try { ({ Jimp } = await import('jimp')) } catch {
  console.log(`${C.red}FAIL${C.reset} jimp not installed — run root \`npm ci\` first`)
  process.exit(1)
}

async function png(path, captureTweak = false) {
  const width = 195
  const img = new Jimp({ width, height: 422, color: 0x101010ff })
  for (let y = 70; y < 130; y++) for (let x = 25; x < 95; x++) img.bitmap.data.set([255, 255, 255, 255], (y * width + x) << 2)
  // The capture must not be byte-identical to the oracle (same generator) — the real gate
  // rejects that as CAPTURE_IS_ORACLE_COPY. One-unit blue tweak on pixel (0,0): metrically nil.
  if (captureTweak) img.bitmap.data[2] ^= 1
  await img.write(path)
}

function run(script, args, env) {
  return execFileSync('node', [join(SCRIPTS, script), ...args], {
    env: { ...process.env, FIGMA_PIPELINE_RUN_ID: RUN_ID, ...env },
    stdio: 'pipe',
  }).toString()
}

function readReport(reports, name) {
  return JSON.parse(readFileSync(join(reports, `${name}-${STEM}.json`), 'utf8'))
}

const ws = mkdtempSync(join(tmpdir(), 'figma-pipeline-dry-run-'))
let reportsForDebug = ''
try {
  // Run child gates from a physical scratch checkout so their immutable PROJECT_ROOT
  // is the fixture product. Pointing FIGMA_CENSUS_CODE_ROOTS at a temporary subset while
  // executing from the real product would correctly trigger the final scope-omission gate.
  const tree = join(ws, 'tree')
  const fixtureFigma = join(tree, 'orchestrator', 'figma')
  SCRIPTS = join(fixtureFigma, 'scripts')
  mkdirSync(fixtureFigma, { recursive: true })
  cpSync(SOURCE_SCRIPTS, SCRIPTS, { recursive: true })
  cpSync(join(SOURCE_SCRIPTS, '..', 'token-schemas'), join(fixtureFigma, 'token-schemas'), { recursive: true })
  for (const directory of ['components', 'tokens', 'runtime', 'schemas']) {
    cpSync(join(SOURCE_SCRIPTS, '..', directory), join(fixtureFigma, directory), { recursive: true })
  }
  const fixtureSiteServer = join(tree, 'orchestrator', 'site', 'server')
  mkdirSync(fixtureSiteServer, { recursive: true })
  for (const file of ['figma-generation.js', 'file-guards.js', 'file-guard-worker.js', 'paths.js']) {
    cpSync(join(SOURCE_SCRIPTS, '..', '..', 'site', 'server', file), join(fixtureSiteServer, file))
  }
  cpSync(join(SOURCE_SCRIPTS, '..', 'screenshot-thresholds.json'), join(fixtureFigma, 'screenshot-thresholds.json'))
  symlinkSync(join(SOURCE_SCRIPTS, '..', 'node_modules'), join(fixtureFigma, 'node_modules'), 'dir')
  mkdirSync(join(tree, 'orchestrator', 'contracts'), { recursive: true })
  cpSync(join(SOURCE_SCRIPTS, '..', '..', 'contracts', 'outcome-shape.json'), join(tree, 'orchestrator', 'contracts', 'outcome-shape.json'))
  writeFileSync(join(tree, 'orchestrator', 'project-config.md'),
    'figmaEnabled: true\nscreenshotPixelGate: strict\nsupportedLocales:\n  - en\n')

  const screensRoot = join(ws, 'screens')
  const screens = join(screensRoot, STEM)
	  const reports = join(ws, 'reports')
	  const cacheRoot = join(ws, 'figma-cache')
	  reportsForDebug = reports
  const robo = join(ws, 'robo')
  const src = join(tree, 'src')
  mkdirSync(screens, { recursive: true })
  mkdirSync(reports, { recursive: true })
  mkdirSync(robo, { recursive: true })
  mkdirSync(src, { recursive: true })

  const taskFile = join(ws, `${STEM}.md`)
  writeFileSync(taskFile, `# Task\n\n## Design\n- HomeScreen — ${FIGMA_URL}\n`)
  const fetchedAt = new Date().toISOString()
  const variantId = 'light-default-shared'
  const tokenCapture = validObservedCapture({
    source: sourceIdentity({
      nodeId: '1:2',
      context: { theme: 'light', locale: 'default', platform: 'shared' },
      origin: { kind: 'task-screen', taskStem: STEM, screenKey: 'HomeScreen', variantId },
    }),
    captureOperationId: 'tokop_0123456789abcdef',
    captureSequence: 1,
    observations: [
      { providerName: 'surface.background', rawValue: '#101010', providerType: 'COLOR' },
      { providerName: 'text.primary', rawValue: '#FFFFFF', providerType: 'COLOR' },
    ],
  })
  const tokenBytes = Buffer.from(JSON.stringify(tokenCapture, null, 2) + '\n')
  writeFileSync(join(screens, 'HomeScreen.tokens.json'), tokenBytes)
  writeFileSync(join(screens, 'index.json'), JSON.stringify({
    schemaVersion: 3,
    taskStem: STEM,
    // fetchedAt is NOW: a real fresh run pulls right before it ships, and a hardcoded old
    // date would trip the final stage's ORACLE_PULL_STALE age advisory (a genuine WARN).
    nodes: { HomeScreen: {
      kind: 'screen', url: FIGMA_URL, nodeId: '1:2', fetchedAt,
      variants: [{
        id: variantId, theme: 'light', locale: 'default', platform: 'shared',
        url: FIGMA_URL, nodeId: '1:2', fetchedAt, imageFile: 'HomeScreen.png',
        specFile: 'HomeScreen.spec.json', instancesFile: 'HomeScreen.instances.json',
        tokensFile: 'HomeScreen.tokens.json',
        tokensHash: bytesHash(tokenBytes),
        captureOperationId: tokenCapture.captureOperationId,
        captureSequence: tokenCapture.captureSequence,
      }]
    } }
  }, null, 2))
  const specElements = [
    { stableId: 'home-top-bar', figmaNodeId: '1:3', name: 'TopBar', bboxDp: { x: 0, y: 0, w: 390, h: 56 }, fills: ['{surface.background}'], paddingDp: { l: 16, t: 0, r: 16, b: 0 } },
    { stableId: 'home-title-label', figmaNodeId: '1:4', name: 'TitleLabel', bboxDp: { x: 16, y: 16, w: 200, h: 24 }, fills: ['{text.primary}'], textStyle: { sizeSp: 20, weight: 700, lineHeightSp: 28, case: 'none' } },
  ]
  const spec = {
    schemaVersion: 2,
    screen: 'HomeScreen',
    frameSizeDp: { w: 390, h: 844 },
    theme: 'light',
    source: { fileKey: 'fileKey', nodeId: '1:2', url: FIGMA_URL },
    rootNodeId: 'root',
    coordinateSystem: { units: 'dp', density: 1, origin: 'frame' },
    themeMetadata: { themeKey: 'light' },
    nodes: [
      { stableId: 'root', figmaNodeId: '1:2', name: 'HomeScreen', role: 'screen', bboxDp: { x: 0, y: 0, w: 390, h: 844 } },
      ...specElements.map((element) => ({ ...element })),
    ],
    elements: specElements,
  }
  writeFileSync(join(screens, 'HomeScreen.spec.json'), JSON.stringify(spec, null, 2))
  writeFileSync(join(screens, 'HomeScreen.instances.json'), '[]\n')
  writeFileSync(join(screens, 'HomeScreen.context.json'), '{}\n')
  await png(join(screens, 'HomeScreen.png'))
  const captureStartedAt = Date.now()
  await png(join(robo, 'HomeScreenScreenshot.png'), true)

  const implFile = join(src, 'HomeScreen.kt')
  writeFileSync(implFile, `
    @Composable
    fun HomeScreen() {
      Box(Modifier.background(AppTokens.colors.surface.background).padding(horizontal = AppTokens.dp.dialog.horizontalPadding))
      Text("Home", color = AppTokens.colors.text.primary, style = AppTokens.typography.h2())
    }
  `)
  const appTokens = join(ws, 'app-tokens.json')
  writeFileSync(appTokens, JSON.stringify({
    schemaVersion: 1,
    files: [implFile],
    tokens: {
      'colors.surface.background': { refs: [{ file: implFile, line: 4 }] },
      'colors.text.primary': { refs: [{ file: implFile, line: 5 }] },
      'dp.dialog.horizontalPadding': { value: 16, refs: [{ file: implFile, line: 4 }] },
      'typography.h2': { value: { sizeSp: 20, lineHeightSp: 28, weight: 700 }, refs: [{ file: implFile, line: 5 }] }
    },
    raw: { colors: {}, dp: {}, sp: {}, fontWeights: {} }
  }, null, 2))

	  const tokenBatch = normalizeSourceCapture(tokenCapture, tokenBytes, immutablePlan(tokenCapture))
	  const observed = aggregateObservedTokens({
	    scope: {
	      fileKeyFingerprint: tokenCapture.source.fileKeyFingerprint,
	      branchKey: tokenCapture.source.branchKey,
	    },
	    batches: [tokenBatch],
	    revision: 1,
	  })
	  const tokenAdapter = {
	    id: 'fixture',
	    platform: 'shared',
	    tokens: {
	      contextMap: [{ when: tokenCapture.source.context, projectMode: 'shared' }],
	      bindingRules: [
	        { ruleId: 'surface-background', kind: 'exact-path', tokenKind: 'color', providerPath: ['surface.background'], projectPath: ['colors', 'surface', 'background'] },
	        { ruleId: 'text-primary', kind: 'exact-path', tokenKind: 'color', providerPath: ['text.primary'], projectPath: ['colors', 'text', 'primary'] },
	      ],
	    },
	  }
	  const projectInventory = {
	    adapterId: 'fixture',
	    tokens: [
	      { projectTokenId: 'fixture:colors.surface.background', kind: 'color', semanticPath: ['colors', 'surface', 'background'] },
	      { projectTokenId: 'fixture:colors.text.primary', kind: 'color', semanticPath: ['colors', 'text', 'primary'] },
	    ],
	  }
	  const bindingSnapshot = bindObservedTokens({
	    catalog: observed.catalog,
	    projectInventories: [projectInventory],
	    adapterConfig: {
	      tokenConfigHash: 'sha256:' + '1'.repeat(64),
	      enabledTokenAdapters: [tokenAdapter],
	    },
	    mappingRegistry: emptyMappingRegistry(observed.catalog.scope),
	    projectAnalysisHash: 'sha256:' + '2'.repeat(64),
	  })
	  const observedCatalogFile = join(ws, 'observed-token-catalog.json')
	  const observedSourceIndexFile = join(ws, 'observed-token-source-index.json')
	  const tokenBindingSnapshotFile = join(ws, 'token-binding-snapshot.json')
	  writeFileSync(observedCatalogFile, JSON.stringify(observed.catalog, null, 2) + '\n')
	  writeFileSync(observedSourceIndexFile, JSON.stringify(observed.index, null, 2) + '\n')
	  writeFileSync(tokenBindingSnapshotFile, JSON.stringify(bindingSnapshot, null, 2) + '\n')

	  const env = {
	    FIGMA_CACHE_ROOT: cacheRoot,
	    FIGMA_REPORTS_DIR: reports,
	    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
	    FIGMA_SPEC_SCREENS_DIR: screensRoot,
	    FIGMA_CENSUS_CODE_ROOTS: src,
	    FIGMA_OBSERVED_TOKEN_CATALOG: observedCatalogFile,
	    FIGMA_OBSERVED_TOKEN_SOURCE_INDEX: observedSourceIndexFile,
	    FIGMA_TOKEN_BINDING_SNAPSHOT: tokenBindingSnapshotFile,
	  }
	  const captureTestDir = join(src, 'androidHostTest')
	  mkdirSync(captureTestDir, { recursive: true })
	  writeFileSync(join(captureTestDir, 'HomeScreenScreenshotTest.kt'), `
    import org.junit.Test
    import org.robolectric.annotation.Config

    class HomeScreenScreenshotTest {
      @Test
      @Config(qualifiers = "w390dp-h844dp")
      fun home() {
        captureRoboImage("build/outputs/roborazzi/HomeScreenScreenshot.png") { }
      }
    }
  `)
  run('check-screen-cache.mjs', [STEM, '--gate'], { ...env, FIGMA_SCREEN_TASK_FILE: taskFile })
  run('component-census.mjs', [STEM, '--screens-dir', screensRoot, '--out', join(reports, `census-${STEM}.json`)], env)
  run('check-spec.mjs', [STEM, '--gate'], env)
  run('evidence-bundle.mjs', [STEM, '--stage', 'prebuild', '--fresh'], env)
  run('compare-screen-spec.mjs', [STEM, '--impl-file', implFile, '--gate'], { ...env, FIGMA_APP_TOKENS: appTokens })
	  run('check-capture-config.mjs', [STEM, '--gate'], env)
  // W2-1: gate mode expects the nodeId capture manifest (identity binding); a manifest-less
  // gate run is a WARN MANIFEST_ABSENT, which would drag this dry run's final PASS to WARN.
  const captureManifest = join(ws, 'capture-manifest.json')
  writeFileSync(captureManifest, JSON.stringify({ recording: { mode: 'recorded', pipelineRunId: RUN_ID }, captures: [{ captureName: 'HomeScreenScreenshot.png', path: join(robo, 'HomeScreenScreenshot.png'), nodeId: '1:2', primaryState: true }] }))
  run('compare-screenshots.mjs', [STEM, '--gate'], { ...env, FIGMA_SCREEN_TASK_FILE: taskFile, ROBORAZZI_OUTPUT_DIR: robo, SCREENSHOT_CAPTURE_STARTED_AT: String(captureStartedAt), SCREENSHOT_CAPTURE_MANIFEST: captureManifest })

  // W4-1: the validator authors the spec report via the CLI, never by hand-assembling the
  // envelope — the CLI computes counts/overall/run-id, pins the spec-compare baseline hash
  // into inputHashes, and schema-validates before writing (this dry run exercises exactly
  // the real authoring path the spec-fidelity-gate §4 prescribes).
  run('write-spec-report.mjs', [STEM, '--screen', 'HomeScreen=PASS'], env)
  run('evidence-bundle.mjs', [STEM, '--stage', 'final', '--fresh'], { ...env, FIGMA_SCREEN_TASK_FILE: taskFile })

  check('real-project-like dry run reaches final PASS evidence', () => {
	    for (const name of ['screen-cache', 'check-spec', 'capture-config', 'census', 'spec', 'spec-compare', 'screenshot', 'evidence']) {
      assert.equal(readReport(reports, name).overall, 'PASS', `${name} should PASS`)
    }
  })
  check('dry run emits a parseable final digest for the shared task-stem contract', () => {
    const digest = readFileSync(join(reports, `figma-meta-${STEM}.txt`), 'utf8')
    const meta = parseFigmaMeta(digest)
    assert.ok(meta)
    assert.equal(meta.taskStem, STEM)
  })
  check('final digest records the gate-policy version from the committed thresholds (W4-3)', () => {
    const digest = readFileSync(join(reports, `figma-meta-${STEM}.txt`), 'utf8')
    const meta = parseFigmaMeta(digest)
    const thresholds = JSON.parse(readFileSync(join(SCRIPTS, '..', 'screenshot-thresholds.json'), 'utf8'))
    assert.equal(meta.gatePolicyVersion, String(thresholds.version))
  })
  check('CLI-authored spec report pins the spec-compare baseline and stamps the policy version (W4-1)', () => {
    const spec = readReport(reports, 'spec')
    assert.equal(spec.authoredBy, 'write-spec-report-cli')
    assert.ok(Number.isInteger(spec.gatePolicyVersion) && spec.gatePolicyVersion >= 1)
    // The baseline pin is LIVE: the final bundle re-hashes spec-compare, so a fix-cycle
    // re-run of compare-screen-spec after authoring correctly re-blocks as INPUT_HASH_MISMATCH.
    const pinned = Object.keys(spec.inputHashes).find((k) => k.endsWith(`spec-compare-${STEM}.json`))
    assert.ok(pinned, 'spec report must pin spec-compare in inputHashes')
    assert.equal(spec.inputHashes[pinned], fileHash(join(reports, `spec-compare-${STEM}.json`)))
  })
  check('dry run binds screen-cache to the ## Design section, not the whole task file', () => {
    const report = readReport(reports, 'screen-cache')
    // Whole-file task hashes were removed: Step 6a/6b legitimately append to the task file,
    // so the binding is the ## Design section hash re-verified by the final bundle.
    assert.equal(report.inputHashes[taskFile], undefined)
    assert.equal(report.inputHashes.design, undefined)
    assert.ok(report.inputs.designSourceHash)
  })
  check('dry run spec-compare uses exact token value evidence', () => {
    const report = readReport(reports, 'spec-compare')
    assert.equal(report.implementation.tokenValueCount, 2)
    assert.equal(report.issues.length, 0)
  })
  check('writeReport rejects reserved envelope fields in extra', () => {
    assert.throws(() => writeReport({
      name: 'reserved-extra',
      taskStem: STEM,
      mode: 'gate',
      inputs: {},
      inputHashes: {},
      overall: 'PASS',
      issues: [],
      extra: { pipelineRunId: 'forged-run' },
      outPath: join(reports, `reserved-extra-${STEM}.json`),
    }), /reserved envelope field: pipelineRunId/)
  })

  // ── W1-7 driver: the same fixture must pass through run-figma-gates.mjs, which owns the
  // choreography (pinned run id, census consult re-check, STARTED_AT, output dirs, nodeId
  // manifest) that the raw sequence above hand-exports. Re-runs regenerate the reports under
  // the SAME run id, so the sealed state stays consistent.
  const driver = (args, extraEnv = {}) => execFileSync('node', [join(SCRIPTS, 'run-figma-gates.mjs'), STEM, ...args], {
    env: { ...process.env, FIGMA_PIPELINE_RUN_ID: RUN_ID, ...env, FIGMA_SCREEN_TASK_FILE: taskFile, ...extraEnv },
    stdio: 'pipe',
  }).toString()
  check('driver: --stage prebuild replays the pre-flight under one pinned run id', () => {
    const out = driver(['--stage', 'prebuild'])
    assert.match(out, /run id pipeline-dry-run-1/)
    for (const name of ['screen-cache', 'check-spec', 'census']) {
      assert.equal(readReport(reports, name).pipelineRunId, RUN_ID, `${name} must stay on the pinned run`)
    }
  })
  check('driver: --stage screenshot --skip-record compares with a nodeId manifest', () => {
    const out = driver(['--stage', 'screenshot', '--skip-record'], {
      SCREENSHOT_CAPTURE_STARTED_AT: String(captureStartedAt),
      ROBORAZZI_OUTPUT_DIRS: robo,
    })
    assert.match(out, /capture manifest: 1 entry/)
    assert.match(out, /comparison is diagnostic/)
    assert.equal(readReport(reports, 'screenshot').overall, 'PASS')
    assert.equal(readReport(reports, 'screenshot').inputs.captureMode, 'preexisting')
  })
  check('driver: --skip-record wraps inherited renamed-capture entries as preexisting', () => {
    const normalCapture = join(robo, 'HomeScreenScreenshot.png')
    const renamedCapture = join(robo, 'renamed-home.png')
    const inheritedManifest = join(ws, 'inherited-capture-manifest.json')
    renameSync(normalCapture, renamedCapture)
    try {
      writeFileSync(inheritedManifest, JSON.stringify({
        recording: { mode: 'recorded', pipelineRunId: RUN_ID },
        captures: [{ captureName: 'renamed-home.png', path: renamedCapture, nodeId: '1:2', primaryState: true }],
      }))
      driver(['--stage', 'screenshot', '--skip-record'], {
        SCREENSHOT_CAPTURE_STARTED_AT: String(captureStartedAt),
        ROBORAZZI_OUTPUT_DIRS: robo,
        SCREENSHOT_CAPTURE_MANIFEST: inheritedManifest,
      })
      const report = readReport(reports, 'screenshot')
      const wrapper = JSON.parse(readFileSync(join(reports, `.capture-manifest-${STEM}.json`), 'utf8'))
      assert.equal(report.overall, 'PASS')
      assert.equal(report.inputs.captureMode, 'preexisting')
      assert.equal(wrapper.recording.mode, 'preexisting')
      assert.equal(wrapper.captures.some((entry) => entry && entry.path === renamedCapture), true)
    } finally {
      renameSync(renamedCapture, normalCapture)
    }
  })
  check('driver: --stage screenshot --skip-record without STARTED_AT fails closed', () => {
    assert.throws(() => driver(['--stage', 'screenshot', '--skip-record'], { SCREENSHOT_CAPTURE_STARTED_AT: '', ROBORAZZI_OUTPUT_DIRS: robo }))
  })
  check('driver: --skip-record screenshot cannot certify final evidence', () => {
    assert.throws(() => driver(['--stage', 'final']))
    const evidence = readReport(reports, 'evidence')
    assert.equal(evidence.overall, 'BLOCKER')
    assert.ok(evidence.issues.some((issue) => issue.issueKind === 'SCREENSHOT_SKIP_RECORD_NONCERTIFYING'))
  })
  check('driver: --stage final re-seals the bundle on the pinned run', () => {
    // Restore the fixture's simulated recorder-produced manifest/report after the
    // deliberate skip-record rejection above.
    run('compare-screenshots.mjs', [STEM, '--gate'], { ...env, FIGMA_SCREEN_TASK_FILE: taskFile, ROBORAZZI_OUTPUT_DIR: robo, SCREENSHOT_CAPTURE_STARTED_AT: String(captureStartedAt), SCREENSHOT_CAPTURE_MANIFEST: captureManifest })
    driver(['--stage', 'final'])
    const evidence = readReport(reports, 'evidence')
    assert.equal(evidence.overall, 'PASS')
    assert.equal(evidence.pipelineRunId, RUN_ID)
  })
} catch (e) {
  let detail = e.stdout ? e.stdout.toString() : e.message
  if (e.stderr && e.stderr.toString()) detail += `\n${e.stderr.toString()}`
  try {
    const evidence = readFileSync(join(reportsForDebug, `evidence-${STEM}.json`), 'utf8')
    detail += `\n${evidence}`
  } catch {}
  fail++; console.log(`${C.red}FAIL${C.reset} dry-run setup threw\n     ${detail}`)
} finally {
  rmSync(ws, { recursive: true, force: true })
}

console.log(`\npipeline-dry-run.test: ${pass} pass, ${fail} fail`)
process.exit(fail === 0 ? 0 : 1)
