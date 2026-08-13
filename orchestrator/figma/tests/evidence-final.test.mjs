// Final evidence freshness fixtures.
import { cpSync, existsSync, mkdtempSync, mkdirSync, readFileSync, readdirSync, rmSync, symlinkSync, unlinkSync, writeFileSync } from 'node:fs'
import { join, dirname, sep } from 'node:path'
import { hostname, tmpdir } from 'node:os'
import { fileURLToPath } from 'node:url'
import { execFileSync, spawnSync } from 'node:child_process'
import { createRequire } from 'node:module'
import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { fileHash } from '../scripts/report-utils.mjs'
import { injectOutcomeFigmaMeta, inspectOutcomeFigmaMeta, logicalTaskText } from '../scripts/outcome-shape.mjs'
import { caveatsHaveContent } from '../scripts/figma-meta.mjs'
import { readSupportedLocales } from '../scripts/lib/design-locale.mjs'
import { CAPTURE_CONFIG_DISCOVERY_KEY, captureConfigDiscovery } from '../scripts/lib/capture-config-discovery.mjs'
import { PROJECT_ROOT, artifactSegment, loadScreenshotThresholds } from '../scripts/_util.mjs'
import { bytesHash } from '../runtime/canonical-json.mjs'
import { sourceIdentity, validObservedCapture } from './observed-token-fixtures.mjs'

const requireCjs = createRequire(import.meta.url)
const { parseDesignSources } = requireCjs('../scripts/design-parser.cjs')
// Most fixtures use one explicit empty task file: no Design bullets, but still
// a present source. Mirror that exact one-body hash in the screen-cache report.
const DESIGN_EMPTY_HASH = parseDesignSources(['']).sourceHash

const HERE = dirname(fileURLToPath(import.meta.url))
const SCRIPT = join(HERE, '..', 'scripts', 'evidence-bundle.mjs')
const SEED = join(HERE, '..', 'scripts', 'seed-evidence-fixture.mjs')
const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }
const ARTIFACT_RESULT_STATUSES = new Set(['PASS', 'MINOR', 'MAJOR', 'BLOCKER', 'LOW_CONTENT_ORACLE', 'REVIEW_REQUIRED'])
const GATE_POLICY_VERSION = loadScreenshotThresholds().version
let cacheRoot = ''
let captureDiscoveryRoot = ''
let captureDiscoveryScreensDir = ''
let captureDiscoveryCodeRoots = []
let captureDiscoveryLocales = []
let defaultTaskFile = ''

// ship-done/verify-done import the canonical token/component readers and the
// shared task-source identity contract. Isolated product-tree fixtures
// therefore need the same runtime owners as a generated project, even when a
// particular assertion does not publish a generation.
function copyDesignReaderRuntime(targetTree) {
  const targetFigma = join(targetTree, 'orchestrator', 'figma')
  for (const directory of ['components', 'tokens', 'runtime', 'schemas']) {
    cpSync(join(HERE, '..', directory), join(targetFigma, directory), { recursive: true })
  }
  const targetTasks = join(targetTree, 'orchestrator', 'tasks')
  mkdirSync(targetTasks, { recursive: true })
  for (const file of ['api-work-package-contract.cjs', 'task-source-contract.cjs']) {
    cpSync(join(HERE, '..', '..', 'tasks', file), join(targetTasks, file))
  }
  const targetNodeModules = join(targetFigma, 'node_modules')
  if (!existsSync(targetNodeModules)) symlinkSync(join(HERE, '..', 'node_modules'), targetNodeModules, 'dir')
  const targetSiteServer = join(targetTree, 'orchestrator', 'site', 'server')
  mkdirSync(targetSiteServer, { recursive: true })
  for (const file of ['child-env.js', 'figma-generation.js', 'file-guards.js', 'file-guard-worker.js', 'paths.js']) {
    cpSync(join(HERE, '..', '..', 'site', 'server', file), join(targetSiteServer, file))
  }
}

function currentSpec(screen = 'Home') {
  const frameSizeDp = { w: 120, h: 240 }
  const element = { stableId: 'title', figmaNodeId: '1:3', name: 'Title', bboxDp: { x: 0, y: 0, w: 40, h: 20 }, fills: ['#FFFFFF'] }
  return {
    schemaVersion: 2,
    screen,
    theme: 'light',
    frameSizeDp,
    source: { fileKey: 'fixture', nodeId: '1:2' },
    rootNodeId: 'root',
    coordinateSystem: { units: 'dp', density: 1, origin: 'frame' },
    themeMetadata: { themeKey: 'light' },
    nodes: [
      { stableId: 'root', figmaNodeId: '1:2', name: screen, role: 'screen', bboxDp: { x: 0, y: 0, ...frameSizeDp } },
      { ...element },
    ],
    elements: [element],
  }
}

function writeCurrentScreenIndex(screenDirectory, taskStem = 'TASK_1_fixture', screenKey = 'Home') {
  const variantId = 'primary'
  const nodeId = '1:2'
  const fetchedAt = new Date().toISOString()
  const source = sourceIdentity({
    nodeId,
    context: { theme: 'light', locale: 'default', platform: 'shared' },
    origin: { kind: 'task-screen', taskStem, screenKey, variantId },
  })
  const capture = validObservedCapture({
    source,
    captureOperationId: 'tokop_0123456789abcdef',
    captureSequence: 1,
  })
  const tokenBytes = Buffer.from(JSON.stringify(capture, null, 2) + '\n')
  const tokensFile = 'observed.tokens.json'
  writeFileSync(join(screenDirectory, tokensFile), tokenBytes)
  writeFileSync(join(screenDirectory, 'index.json'), JSON.stringify({
    schemaVersion: 3,
    taskStem,
    nodes: {
      [screenKey]: {
        kind: 'screen',
        url: 'https://www.figma.com/design/fixture?node-id=1-2',
        nodeId,
        fetchedAt,
        variants: [{
          id: variantId,
          theme: 'light',
          locale: 'default',
          platform: 'shared',
          url: 'https://www.figma.com/design/fixture?node-id=1-2',
          nodeId,
          fetchedAt,
          imageFile: `${screenKey}.png`,
          specFile: `${screenKey}.spec.json`,
          instancesFile: `${screenKey}.instances.json`,
          tokensFile,
          tokensHash: bytesHash(tokenBytes),
          captureOperationId: capture.captureOperationId,
          captureSequence: capture.captureSequence,
        }],
      },
    },
  }, null, 2) + '\n')
}

function screenshotArtifactSet(stem, runId, screen = 'Home') {
  const baseRel = `artifacts/screenshot/${artifactSegment(stem)}/${artifactSegment(runId)}/001-home-primary`
  const baseAbs = join(cacheRoot, baseRel)
  mkdirSync(baseAbs, { recursive: true })
  const files = {
    figma: join(baseAbs, 'figma.png'),
    actual: join(baseAbs, 'actual.png'),
    diff: join(baseAbs, 'diff.png'),
    overlay: join(baseAbs, 'overlay.png'),
    manifest: join(baseAbs, 'manifest.json'),
  }
  writeFileSync(files.figma, 'figma-png-fixture\n')
  writeFileSync(files.actual, 'actual-png-fixture\n')
  writeFileSync(files.diff, 'diff-png-fixture\n')
  writeFileSync(files.overlay, 'overlay-png-fixture\n')
  writeFileSync(files.manifest, '{"schemaVersion":1}\n')
  const img = (kind) => ({
    id: `001-home-primary-${kind}`,
    kind,
    screen,
    theme: 'primary',
    themeKey: 'primary',
    status: 'PASS',
    path: `${baseRel}/${kind}.png`,
    hash: fileHash(files[kind]),
    bytes: 1,
    width: 120,
    height: 240,
    mime: 'image/png',
  })
  return {
    schemaVersion: 1,
    id: '001-home-primary',
    screen,
    theme: 'primary',
    themeKey: 'primary',
    status: 'PASS',
    score: 1,
    coverage: 0.1,
    dimensions: { width: 120, height: 240 },
    manifest: {
      id: '001-home-primary-manifest',
      kind: 'manifest',
      path: `${baseRel}/manifest.json`,
      hash: fileHash(files.manifest),
      bytes: 1,
      mime: 'application/json',
    },
    artifacts: {
      figma: img('figma'),
      actual: img('actual'),
      diff: img('diff'),
      overlay: img('overlay'),
    },
  }
}

function bodyFor(name) {
  if (name === 'screen-cache') return { screens: [{ screen: 'Home', status: 'complete', themes: {} }] }
  if (name === 'check-spec') return { files: [{ file: 'Home.spec.json', status: 'PASS' }] }
  if (name === 'capture-config') return {
    version: 1,
    fixed: [],
    designLocale: { language: null, reason: 'no-signal' },
    designLocaleEnvOverrides: [],
  }
  // The agent-authored spec report has a body schema (spec-report.schema.json):
  // mode 'gate' + a NON-EMPTY per-screen verdict list, so an empty screens:[] PASS envelope
  // can no longer certify the spec-fidelity gate.
  if (name === 'spec') return { screens: [{ screen: 'Home', verdict: 'PASS' }] }
  if (name === 'census') return { version: 2, screens: {}, components: [], missing: [], incomplete: [], ambiguous: [], unsupported: [], retired: [], sourceStale: [] }
  if (name === 'spec-compare') return {
    engineVersion: 'spec-compare-v1',
    unresolvedRefs: [],
    implementationModel: null,
    comparisons: [{ screen: 'Home', theme: 'light', file: 'Home.spec.json', stableId: 'title', elementName: 'Title', status: 'PASS' }],
    widgetClasses: {},
    implementation: { files: [], screenMap: {}, tokenCount: 0, tokenValueCount: 0, rawColorCount: 0, rawDpCount: 0 },
  }
  if (name === 'screenshot') return {
    // Canonical metric + thresholds: the final bundle fails closed on a weakened or
    // unrecorded judge, so the baseline fixture must record the canon compare-screenshots emits.
    // Color-only knobs include bgTolerance/deltaEPass and extraContentBand/_Warn;
    // all are required-or-stricter at final.
    // extraContentDeltaE is the chroma arm's ΔE00 floor (enforced <= 10 when recorded).
    // The metric-INTERNAL knobs (shiftRadius/gaussianSigma/aaTolerance/varFloor/maskMode/
    // deltaEStride/regionGrid) are also required.
    // The zone-gate knobs (zoneGate must be true, zoneBlocker gte 0.35, minRegionPx lte 400)
    // enforce the per-zone floor; zoneTextBlocker is recorded but not
    // canon-enforced (a raised text floor is stricter, a lowered one only relaxes text leniency).
    metric: 'masked-ssim-luma-v2',
    thresholds: { pass: 0.90, minor: 0.80, major: 0.65, aspectTolerance: 0.15, minCoverage: 0.005, bgTolerance: 24, deltaEPass: 3, majorBand: 'block', colorAxis: true, extraContentBand: 'warn', extraContentWarn: 0.02, extraContentDeltaE: 10, extraContentRingPx: 3, shiftRadius: 2, gaussianSigma: 1.5, aaTolerance: 1, varFloor: 12, maskMode: 'variance', deltaEStride: 2, regionGrid: '8x4', zoneGate: true, zoneBlocker: 0.35, zoneTextBlocker: 0.25, minRegionPx: 400, statusBarDp: 24, navBarDp: 48, pixelGate: 'strict' },
    semantic: { enabled: false, status: 'DISABLED', promoted: false, zones: [], findings: [] },
    results: [{ screen: 'Home', themeKey: 'primary', status: 'PASS', score: 1 }],
  }
  return {}
}

function report(name, stem, runId, overall = 'PASS', extra = {}) {
  const out = {
    schemaVersion: 1,
    gatePolicyVersion: GATE_POLICY_VERSION,
    taskStem: stem,
    pipelineRunId: runId,
    mode: 'gate',
    inputs: name === 'screenshot' ? { captureStartedAt: '2026-01-01T00:00:00.000Z', captureMode: 'recorded' }
      : name === 'screen-cache' ? { designSourceHash: DESIGN_EMPTY_HASH } : {},
    inputHashes: {},
    overall,
    blockingCount: 0,
    warningCount: 0,
    issues: [],
    reportPath: `reports/${stem}.json`,
    generatedAt: '2026-01-01T00:00:00.000Z',
    ...bodyFor(name),
    ...extra,
  }
  if (name === 'screenshot' && !extra.disableArtifacts) {
    const set = screenshotArtifactSet(stem, runId, out.results?.[0]?.screen || 'Home')
    if (Array.isArray(out.results)) {
      out.results = out.results.map((row) => {
        const status = String(row && row.status || '').toUpperCase()
        return ARTIFACT_RESULT_STATUSES.has(status) && !row.artifactSet ? { ...row, artifactSet: set } : row
      })
    }
    out.artifactSet = {
      schemaVersion: 1,
      kind: 'screenshot-compare-artifact-set',
      root: `artifacts/screenshot/${artifactSegment(stem)}/${artifactSegment(runId)}`,
      entries: Object.values(set.artifacts),
    }
    out.artifactSets = [set]
  }
  if (name === 'capture-config') {
    const codeRoots = captureDiscoveryCodeRoots.length ? captureDiscoveryCodeRoots : [captureDiscoveryRoot]
    const supportedLocales = captureDiscoveryLocales.length ? captureDiscoveryLocales : readSupportedLocales()
    const discovery = captureConfigDiscovery({ codeRoots, screensDir: captureDiscoveryScreensDir, supportedLocales })
    out.inputs = {
      screensDir: captureDiscoveryScreensDir,
      codeRoots,
      specs: 1,
      testFilesScanned: discovery.files.length,
      fixed: 0,
      captureDiscovery: { version: discovery.version, roots: discovery.roots, screensDir: discovery.screensDir, digest: discovery.digest },
    }
    out.inputHashes = { ...(out.inputHashes || {}), [CAPTURE_CONFIG_DISCOVERY_KEY]: discovery.digest }
  }
  return out
}

function writeReport(dir, name, stem, runId, extra = {}) {
  const input = join(dir, `${name}-${stem}.input`)
  writeFileSync(input, `${name}:${stem}\n`)
  const defaults = name === 'census' ? {} : { inputHashes: { [input]: fileHash(input) } }
  writeFileSync(join(dir, `${name}-${stem}.json`), JSON.stringify(report(name, stem, runId, 'PASS', { ...defaults, ...extra }), null, 2))
}

function run(args, env) {
  return spawnSync('node', [SCRIPT, ...args], {
    env: { ...process.env, FIGMA_CACHE_ROOT: cacheRoot, FIGMA_SCREEN_TASK_FILE: defaultTaskFile, ...env },
    encoding: 'utf8'
  })
}

const ws = mkdtempSync(join(tmpdir(), 'evidence-final-'))
cacheRoot = join(ws, 'figma-cache')
captureDiscoveryRoot = join(ws, 'capture-discovery-code')
captureDiscoveryScreensDir = join(ws, 'capture-discovery-screens')
captureDiscoveryCodeRoots = [PROJECT_ROOT, captureDiscoveryRoot]
captureDiscoveryLocales = readSupportedLocales()
try {
  const reports = join(ws, 'reports')
  const screensRoot = join(ws, 'screens')
  const screens = join(screensRoot, 'TASK_1_fixture')
  mkdirSync(reports, { recursive: true })
  mkdirSync(screens, { recursive: true })
  mkdirSync(join(captureDiscoveryRoot, 'commonTest'), { recursive: true })
  mkdirSync(captureDiscoveryScreensDir, { recursive: true })
  defaultTaskFile = join(ws, 'TASK_1_fixture.md')
  writeFileSync(defaultTaskFile, '')
  writeFileSync(join(screens, 'Home.spec.json'), JSON.stringify(currentSpec(), null, 2) + '\n')
  writeCurrentScreenIndex(screens)

		  for (const name of ['screen-cache', 'check-spec', 'capture-config', 'census', 'spec', 'spec-compare', 'screenshot']) writeReport(reports, name, 'TASK_1_fixture', 'run-1')
			  const initialFinal = spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--stage', 'final', '--require', 'screen-cache,check-spec,census,spec,spec-compare,screenshot', '--fresh'], {
	    env: { ...process.env, FIGMA_CACHE_ROOT: cacheRoot, FIGMA_REPORTS_DIR: reports, FIGMA_SCREEN_CACHE_ROOT: screensRoot, FIGMA_SCREEN_TASK_FILE: defaultTaskFile, FIGMA_PIPELINE_RUN_ID: 'run-1' },
	    encoding: 'utf8',
	  })
	  if (initialFinal.status !== 0) {
	    const failedBundle = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
	    throw new Error(`${initialFinal.stdout}${initialFinal.stderr}\n${JSON.stringify(failedBundle.issues, null, 2)}`)
	  }
	  const bundle = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
	  check('final evidence passes only with fresh required reports', () => assert.equal(bundle.overall, 'PASS'))

	  const longStem = 'TASK_250_zadacha_na_uluchshenii_flou_diagnostiki_i_telemetrii_terminaldiagnosticscreen'
	  const longRunId = `20260718T084821823Z-${artifactSegment(longStem)}`
	  const longScreens = join(screensRoot, longStem)
	  mkdirSync(longScreens, { recursive: true })
	  writeCurrentScreenIndex(longScreens, longStem)
	  writeReport(reports, 'screenshot', longStem, longRunId)
	  const longIdentityFinal = run([longStem, '--stage', 'final', '--require', 'screenshot', '--fresh'], {
	    FIGMA_REPORTS_DIR: reports,
	    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
	    FIGMA_PIPELINE_RUN_ID: longRunId,
	  })
	  const longIdentityBundle = JSON.parse(readFileSync(join(reports, `evidence-${longStem}.json`), 'utf8'))
	  check('final evidence accepts bounded artifact paths for a long stem and long run id', () => {
	    assert.equal(longStem.length, 86)
	    assert.ok(longRunId.length > 80)
	    assert.equal(longIdentityFinal.status, 0, longIdentityFinal.stderr)
	    assert.equal(longIdentityBundle.overall, 'PASS')
	    assert.ok(!longIdentityBundle.issues.some((i) => String(i.issueKind || '').startsWith('SCREENSHOT_ARTIFACT_')))
	  })

	  const addedCaptureTest = join(captureDiscoveryRoot, 'commonTest', 'AddedScreenshotTest.kt')
	  writeFileSync(addedCaptureTest, 'fun added() { captureRoboImage("AddedScreenshot.png") }\n')
	  const changedDiscovery = run(['TASK_1_fixture', '--stage', 'final', '--require', 'capture-config', '--fresh'], {
	    FIGMA_REPORTS_DIR: reports,
	    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
	    FIGMA_PIPELINE_RUN_ID: 'run-1',
	  })
	  const changedDiscoveryReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
	  check('final evidence rejects a capture/resource discovery set changed after the report', () => {
	    assert.notEqual(changedDiscovery.status, 0)
	    assert.ok(changedDiscoveryReport.issues.some((i) => i.issueKind === 'REPORT_INPUT_HASH_MISMATCH' && i.reportName === 'capture-config'))
	  })
	  rmSync(addedCaptureTest, { force: true })

	  rmSync(join(reports, 'spec-compare-TASK_1_fixture.json'), { force: true })
	  const defaultFinal = run(['TASK_1_fixture', '--stage', 'final', '--fresh'], {
	    FIGMA_REPORTS_DIR: reports,
	    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
	    FIGMA_PIPELINE_RUN_ID: 'run-1',
	  })
	  const defaultFinalReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('final evidence default required set includes spec, spec-compare, and screenshot', () => {
		    assert.notEqual(defaultFinal.status, 0)
		    assert.ok(defaultFinalReport.issues.find((i) => i.issueKind === 'REPORT_MISSING' && i.reportName === 'spec-compare'))
		  })
		  writeReport(reports, 'spec-compare', 'TASK_1_fixture', 'run-1')
		  rmSync(join(reports, 'spec-TASK_1_fixture.json'), { force: true })
		  const missingSpec = run(['TASK_1_fixture', '--stage', 'final', '--fresh'], {
		    FIGMA_REPORTS_DIR: reports,
		    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		    FIGMA_PIPELINE_RUN_ID: 'run-1',
		  })
		  const missingSpecReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('final evidence default required set includes validator-authored spec report', () => {
		    assert.notEqual(missingSpec.status, 0)
		    assert.ok(missingSpecReport.issues.find((i) => i.issueKind === 'REPORT_MISSING' && i.reportName === 'spec'))
		  })
		  writeReport(reports, 'spec', 'TASK_1_fixture', 'run-1')
		  rmSync(join(reports, 'capture-config-TASK_1_fixture.json'), { force: true })
		  const missingCaptureConfig = run(['TASK_1_fixture', '--stage', 'final', '--fresh'], {
		    FIGMA_REPORTS_DIR: reports,
		    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		    FIGMA_PIPELINE_RUN_ID: 'run-1',
		  })
		  const missingCaptureConfigReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('final evidence default required set includes capture-config', () => {
		    assert.notEqual(missingCaptureConfig.status, 0)
		    assert.ok(missingCaptureConfigReport.issues.find((i) => i.issueKind === 'REPORT_MISSING' && i.reportName === 'capture-config'))
		  })
		  writeReport(reports, 'capture-config', 'TASK_1_fixture', 'run-1')

		  // The site must not display a diagnostic final bundle with a caller-chosen subset as
		  // authoritative READY evidence. Point the CommonJS reader at an isolated cache and pin
		  // the cross-layer contract with the old six-report subset.
		  const siteCache = join(ws, 'site-figma-cache')
		  const siteReports = join(siteCache, 'reports')
		  const siteStem = 'TASK_999999_site_subset_final'
		  const siteLongStem = 'TASK_250_zadacha_na_uluchshenii_flou_diagnostiki_i_telemetrii_terminaldiagnosticscreen'
		  const siteLongRunId = `20260718T084821823Z-${artifactSegment(siteLongStem)}`
		  const subset = ['screen-cache', 'check-spec', 'census', 'spec', 'spec-compare', 'screenshot']
		  const canonicalSiteReports = ['screen-cache', 'check-spec', 'capture-config', 'census', 'spec', 'spec-compare', 'screenshot']
		  mkdirSync(siteReports, { recursive: true })
		  const siteInputHashes = {}
		  for (const name of canonicalSiteReports) {
		    const file = join(siteReports, `${name}-${siteStem}.json`)
		    writeFileSync(file, JSON.stringify({
		      name, taskStem: siteStem, mode: 'gate', overall: 'PASS', issues: [],
		      blockingCount: 0, warningCount: 0, pipelineRunId: 'site-run-1',
		      generatedAt: '2026-01-01T00:00:00.000Z',
		    }))
		    siteInputHashes[`reports/${name}-${siteStem}.json`] = fileHash(file)
		  }
		  writeFileSync(join(siteReports, `evidence-${siteStem}.json`), JSON.stringify({
		    name: 'evidence', taskStem: siteStem, mode: 'transport', stage: 'final',
		    overall: 'PASS', issues: [], blockingCount: 0, warningCount: 0,
		    pipelineRunId: 'site-run-1', generatedAt: '2026-01-01T00:00:00.000Z',
		    requiredReports: subset, inputHashes: siteInputHashes,
		  }))
		  execFileSync('node', [SEED, siteLongStem, siteLongRunId], {
		    env: { ...process.env, FIGMA_CACHE_ROOT: siteCache },
		    stdio: 'pipe',
		  })
		  const sitePathsModule = requireCjs.resolve('../../site/server/paths.js')
		  const siteEvidenceModule = requireCjs.resolve('../../site/server/figma-evidence.js')
		  const pixelReviewModule = requireCjs.resolve('../../site/server/pixel-review.js')
		  const sitePaths = requireCjs(sitePathsModule)
		  const priorSiteFigmaCache = sitePaths.FIGMA_CACHE_DIR
		  const priorSiteProjectRoot = sitePaths.PROJECT_ROOT
		  try {
		    sitePaths.FIGMA_CACHE_DIR = siteCache
		    sitePaths.PROJECT_ROOT = ws
		    delete requireCjs.cache[siteEvidenceModule]
		    const siteEvidence = requireCjs(siteEvidenceModule)
		    check('site refuses READY for a final bundle that declares a report subset', () => {
		      const summary = siteEvidence.readEvidence(siteStem)
		      assert.equal(summary.evidenceState, 'INCOMPLETE')
		      assert.deepEqual(summary.missingRequiredReports, [])
		      assert.equal(summary.finalRequiredReportSetMismatch, true)
		      assert.ok(summary.topIssues.some((i) => i.issueKind === 'FINAL_REQUIRED_REPORT_SET_NONCANONICAL'))
		    })
		    check('site serves hash-bound compare artifacts for a long stem and long run id', () => {
		      const summary = siteEvidence.readEvidence(siteLongStem)
		      assert.equal(summary.stage, 'final')
		      assert.equal(summary.stale, false)
		      const screenshot = summary.reports.find((report) => report.name === 'screenshot')
		      const id = `${siteLongStem}_figma`
		      const artifact = siteEvidence.compareArtifactFile(siteLongStem, id, screenshot.hash)
		      assert.ok(artifact && artifact.bytes.length > 0)
		      assert.match(artifact.file, new RegExp(`${artifactSegment(siteLongStem)}/${artifactSegment(siteLongRunId)}`))
		    })

		    const longScreenshotPath = join(siteReports, `screenshot-${siteLongStem}.json`)
		    const longScreenshot = JSON.parse(readFileSync(longScreenshotPath, 'utf8'))
		    longScreenshot.results[0].status = 'REVIEW_REQUIRED'
		    writeFileSync(longScreenshotPath, JSON.stringify(longScreenshot, null, 2) + '\n')
		    delete requireCjs.cache[pixelReviewModule]
		    const pixelReview = requireCjs(pixelReviewModule)
		    let reviewError = null
		    let reviewOutput = null
		    pixelReview.applyVerdict(siteLongStem, 'Home', 'primary', 'pass', 'long identity regression', (error, output) => {
		      reviewError = error
		      reviewOutput = output
		    })
		    check('pixel review resolves bounded artifact paths for a long logical stem', () => {
		      assert.equal(reviewError, null)
		      assert.equal(reviewOutput && reviewOutput.ok, true)
		      assert.equal(existsSync(join(ws, 'orchestrator', 'tasks', 'evidence', 'pixel-review', `${siteLongStem}.json`)), true)
		    })
		  } finally {
		    delete requireCjs.cache[pixelReviewModule]
		    delete requireCjs.cache[siteEvidenceModule]
		    sitePaths.FIGMA_CACHE_DIR = priorSiteFigmaCache
		    sitePaths.PROJECT_ROOT = priorSiteProjectRoot
		  }

		  // An empty screens:[] spec report is a PASS envelope that certifies nothing —
		  // the body schema's non-empty floor must block it at final (REPORT_BODY_SCHEMA_INVALID).
		  writeReport(reports, 'spec', 'TASK_1_fixture', 'run-1', { screens: [] })
		  const emptySpecScreens = run(['TASK_1_fixture', '--stage', 'final', '--require', 'spec', '--fresh'], {
		    FIGMA_REPORTS_DIR: reports,
		    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		    FIGMA_PIPELINE_RUN_ID: 'run-1',
		  })
		  const emptySpecScreensReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('a spec report with empty screens:[] blocks final (REPORT_BODY_SCHEMA_INVALID)', () => {
		    assert.notEqual(emptySpecScreens.status, 0)
		    assert.ok(emptySpecScreensReport.issues.find((i) => i.issueKind === 'REPORT_BODY_SCHEMA_INVALID' && i.reportName === 'spec'))
		  })
		  writeReport(reports, 'spec', 'TASK_1_fixture', 'run-1')

		  for (const [name, body] of [['screen-cache', { screens: [] }], ['check-spec', { files: [] }]]) {
		    writeReport(reports, name, 'TASK_1_fixture', 'run-1', body)
		    const emptyBody = run(['TASK_1_fixture', '--stage', 'final', '--require', name, '--fresh'], {
		      FIGMA_REPORTS_DIR: reports,
		      FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		      FIGMA_PIPELINE_RUN_ID: 'run-1',
		    })
		    const emptyBodyReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		    check(`an empty ${name} result list blocks final`, () => {
		      assert.notEqual(emptyBody.status, 0)
		      assert.ok(emptyBodyReport.issues.find((i) => i.issueKind === 'REPORT_BODY_SCHEMA_INVALID' && i.reportName === name))
		    })
		    writeReport(reports, name, 'TASK_1_fixture', 'run-1')
		  }

	  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'old-run')
	  const stale = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
    FIGMA_REPORTS_DIR: reports,
    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
    FIGMA_PIPELINE_RUN_ID: 'run-1',
  })
  const staleReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
	  check('fresh final evidence rejects stale pipelineRunId', () => {
	    assert.notEqual(stale.status, 0)
	    assert.ok(staleReport.issues.find((i) => i.issueKind === 'REPORT_STALE_RUN' && i.reportName === 'screenshot'))
	  })
		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1', { inputs: {} })
	  const noFreshScreenshot = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
	    FIGMA_REPORTS_DIR: reports,
	    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
	    FIGMA_PIPELINE_RUN_ID: 'run-1',
	  })
	  const noFreshScreenshotReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
			  check('final evidence rejects screenshot report without fresh capture evidence', () => {
		    assert.notEqual(noFreshScreenshot.status, 0)
		    assert.ok(noFreshScreenshotReport.issues.find((i) => i.issueKind === 'SCREENSHOT_FRESH_CAPTURE_EVIDENCE_MISSING'))
			  })
			  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1', { inputs: { captureStartedAt: '2026-01-01T00:00:00.000Z' } })
			  const missingCaptureMode = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
			    FIGMA_REPORTS_DIR: reports,
			    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
			    FIGMA_PIPELINE_RUN_ID: 'run-1',
			  })
			  const missingCaptureModeReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
			  check('final evidence rejects missing screenshot recorder provenance', () => {
			    assert.notEqual(missingCaptureMode.status, 0)
			    assert.ok(missingCaptureModeReport.issues.find((i) => i.issueKind === 'SCREENSHOT_RECORD_PROVENANCE_MISSING'))
			  })
			  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1', { inputs: { captureStartedAt: '2026-01-01T00:00:00.000Z', captureMode: 'future-mode' } })
			  const unknownCaptureMode = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
			    FIGMA_REPORTS_DIR: reports,
			    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
			    FIGMA_PIPELINE_RUN_ID: 'run-1',
			  })
			  const unknownCaptureModeReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
			  check('final evidence rejects unknown screenshot recorder provenance', () => {
			    assert.notEqual(unknownCaptureMode.status, 0)
			    assert.ok(unknownCaptureModeReport.issues.find((i) => i.issueKind === 'SCREENSHOT_RECORD_PROVENANCE_MISSING'))
			  })
			  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1', { inputs: { captureStartedAt: '1', captureMode: 'recorded' } })
		  const ancientCaptureBound = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
		    FIGMA_REPORTS_DIR: reports,
		    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		    FIGMA_PIPELINE_RUN_ID: 'run-1',
		  })
		  const ancientCaptureBoundReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('final evidence rejects an ancient positive capture lower bound', () => {
		    assert.notEqual(ancientCaptureBound.status, 0)
		    assert.ok(ancientCaptureBoundReport.issues.find((i) => i.issueKind === 'SCREENSHOT_CAPTURE_BOUND_IMPLAUSIBLE'))
		  })
		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1')

	  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1', { disableArtifacts: true })
	  const missingArtifacts = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
	    FIGMA_REPORTS_DIR: reports,
	    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
	    FIGMA_PIPELINE_RUN_ID: 'run-1',
	  })
	  const missingArtifactsReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('final evidence rejects scored screenshot results without saved artifacts', () => {
		    assert.notEqual(missingArtifacts.status, 0)
		    assert.ok(missingArtifactsReport.issues.find((i) => i.issueKind === 'SCREENSHOT_ARTIFACT_SET_MISSING'))
		  })

		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1')
		  const emptyArtifactSetFile = join(reports, 'screenshot-TASK_1_fixture.json')
		  const emptyArtifactSetReport = JSON.parse(readFileSync(emptyArtifactSetFile, 'utf8'))
		  emptyArtifactSetReport.results[0].artifactSet = { schemaVersion: 1, id: 'empty-artifacts', artifacts: {} }
		  writeFileSync(emptyArtifactSetFile, JSON.stringify(emptyArtifactSetReport, null, 2))
		  const emptyArtifactSet = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
		    FIGMA_REPORTS_DIR: reports,
		    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		    FIGMA_PIPELINE_RUN_ID: 'run-1',
		  })
		  const emptyArtifactSetEvidence = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('final evidence rejects empty screenshot artifactSet', () => {
		    assert.notEqual(emptyArtifactSet.status, 0)
		    assert.ok(emptyArtifactSetEvidence.issues.find((i) => i.issueKind === 'SCREENSHOT_ARTIFACT_SET_INCOMPLETE'))
		  })

		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1')
		  const incompleteArtifactSetFile = join(reports, 'screenshot-TASK_1_fixture.json')
		  const incompleteArtifactSetReport = JSON.parse(readFileSync(incompleteArtifactSetFile, 'utf8'))
		  delete incompleteArtifactSetReport.results[0].artifactSet.artifacts.overlay
		  writeFileSync(incompleteArtifactSetFile, JSON.stringify(incompleteArtifactSetReport, null, 2))
		  const incompleteArtifactSet = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
		    FIGMA_REPORTS_DIR: reports,
		    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		    FIGMA_PIPELINE_RUN_ID: 'run-1',
		  })
		  const incompleteArtifactSetEvidence = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('final evidence rejects incomplete screenshot artifactSet', () => {
		    assert.notEqual(incompleteArtifactSet.status, 0)
		    assert.ok(incompleteArtifactSetEvidence.issues.find((i) => i.issueKind === 'SCREENSHOT_ARTIFACT_SET_INCOMPLETE'))
		  })

		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1')
		  const wrongRoleArtifactSetFile = join(reports, 'screenshot-TASK_1_fixture.json')
		  const wrongRoleArtifactSetReport = JSON.parse(readFileSync(wrongRoleArtifactSetFile, 'utf8'))
		  wrongRoleArtifactSetReport.results[0].artifactSet.artifacts.diff.kind = 'actual'
		  writeFileSync(wrongRoleArtifactSetFile, JSON.stringify(wrongRoleArtifactSetReport, null, 2))
		  const wrongRoleArtifactSet = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
		    FIGMA_REPORTS_DIR: reports,
		    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		    FIGMA_PIPELINE_RUN_ID: 'run-1',
		  })
		  const wrongRoleArtifactSetEvidence = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('final evidence rejects artifactSet slots with wrong roles', () => {
		    assert.notEqual(wrongRoleArtifactSet.status, 0)
		    assert.ok(wrongRoleArtifactSetEvidence.issues.find((i) => i.issueKind === 'SCREENSHOT_ARTIFACT_SET_ROLE_INVALID'))
		  })

		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1')
		  const wrongRunArtifactFile = join(reports, 'screenshot-TASK_1_fixture.json')
		  const wrongRunArtifactReport = JSON.parse(readFileSync(wrongRunArtifactFile, 'utf8'))
		  const oldRunSet = screenshotArtifactSet('TASK_1_fixture', 'old-run')
		  wrongRunArtifactReport.results[0].artifactSet = oldRunSet
		  wrongRunArtifactReport.artifactSet = {
		    schemaVersion: 1,
		    kind: 'screenshot-compare-artifact-set',
		    root: 'artifacts/screenshot/TASK_1_fixture/old-run',
		    entries: Object.values(oldRunSet.artifacts),
		  }
		  wrongRunArtifactReport.artifactSets = [oldRunSet]
		  writeFileSync(wrongRunArtifactFile, JSON.stringify(wrongRunArtifactReport, null, 2))
		  const wrongRunArtifact = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
		    FIGMA_REPORTS_DIR: reports,
		    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		    FIGMA_PIPELINE_RUN_ID: 'run-1',
		  })
		  const wrongRunArtifactEvidence = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('final evidence rejects screenshot artifacts from another pipeline run', () => {
		    assert.notEqual(wrongRunArtifact.status, 0)
		    assert.ok(wrongRunArtifactEvidence.issues.find((i) => i.issueKind === 'SCREENSHOT_ARTIFACT_PATH_INVALID'))
		  })

		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1')
		  writeFileSync(join(cacheRoot, 'artifacts', 'screenshot', 'TASK_1_fixture', 'run-1', '001-home-primary', 'diff.png'), 'changed diff\n')
	  const artifactHashDrift = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
	    FIGMA_REPORTS_DIR: reports,
	    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
	    FIGMA_PIPELINE_RUN_ID: 'run-1',
	  })
	  const artifactHashDriftReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
	  check('final evidence rejects screenshot artifact hash drift', () => {
	    assert.notEqual(artifactHashDrift.status, 0)
	    assert.ok(artifactHashDriftReport.issues.find((i) => i.issueKind === 'SCREENSHOT_ARTIFACT_HASH_MISMATCH'))
	  })

	  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1')
	  const badArtifactPathFile = join(reports, 'screenshot-TASK_1_fixture.json')
	  const badArtifactPathReport = JSON.parse(readFileSync(badArtifactPathFile, 'utf8'))
	  badArtifactPathReport.results[0].artifactSet.artifacts.diff.id = 'bad-path-diff'
	  badArtifactPathReport.results[0].artifactSet.artifacts.diff.path = '../diff.png'
	  writeFileSync(badArtifactPathFile, JSON.stringify(badArtifactPathReport, null, 2))
	  const artifactPathInvalid = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
	    FIGMA_REPORTS_DIR: reports,
	    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
	    FIGMA_PIPELINE_RUN_ID: 'run-1',
	  })
	  const artifactPathInvalidReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
	  check('final evidence rejects screenshot artifact path traversal', () => {
	    assert.notEqual(artifactPathInvalid.status, 0)
	    assert.ok(artifactPathInvalidReport.issues.find((i) => i.issueKind === 'SCREENSHOT_ARTIFACT_PATH_INVALID'))
	  })

	  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1')
	  const artifactCollisionFile = join(reports, 'screenshot-TASK_1_fixture.json')
	  const artifactCollisionReport = JSON.parse(readFileSync(artifactCollisionFile, 'utf8'))
	  artifactCollisionReport.results[0].artifactSet.artifacts.diff.id = artifactCollisionReport.results[0].artifactSet.artifacts.figma.id
	  writeFileSync(artifactCollisionFile, JSON.stringify(artifactCollisionReport, null, 2))
	  const artifactIdCollision = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
	    FIGMA_REPORTS_DIR: reports,
	    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
	    FIGMA_PIPELINE_RUN_ID: 'run-1',
	  })
	  const artifactIdCollisionReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
	  check('final evidence rejects screenshot artifact id collisions', () => {
	    assert.notEqual(artifactIdCollision.status, 0)
	    assert.ok(artifactIdCollisionReport.issues.find((i) => i.issueKind === 'SCREENSHOT_ARTIFACT_ID_COLLISION'))
	  })
	  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1')

	  const artifactSetMismatchFile = join(reports, 'screenshot-TASK_1_fixture.json')
	  const artifactSetMismatchReport = JSON.parse(readFileSync(artifactSetMismatchFile, 'utf8'))
	  artifactSetMismatchReport.results[0].artifactSet.screen = 'Settings'
	  writeFileSync(artifactSetMismatchFile, JSON.stringify(artifactSetMismatchReport, null, 2))
	  const artifactSetMismatch = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
	    FIGMA_REPORTS_DIR: reports,
	    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
	    FIGMA_PIPELINE_RUN_ID: 'run-1',
	  })
	  const artifactSetMismatchEvidence = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
	  check('final evidence rejects artifactSet from a different screenshot row', () => {
	    assert.notEqual(artifactSetMismatch.status, 0)
	    assert.ok(artifactSetMismatchEvidence.issues.find((i) => i.issueKind === 'SCREENSHOT_ARTIFACT_SET_MISMATCH'))
	  })

	  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1')
	  const uppercaseHashFile = join(reports, 'screenshot-TASK_1_fixture.json')
	  const uppercaseHashReport = JSON.parse(readFileSync(uppercaseHashFile, 'utf8'))
	  uppercaseHashReport.results[0].artifactSet.artifacts.diff.hash = uppercaseHashReport.results[0].artifactSet.artifacts.diff.hash.replace(/^sha256:/, 'sha256:').replace(/[a-f0-9]{64}$/, (hex) => hex.toUpperCase())
	  writeFileSync(uppercaseHashFile, JSON.stringify(uppercaseHashReport, null, 2))
	  const uppercaseHash = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
	    FIGMA_REPORTS_DIR: reports,
	    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
	    FIGMA_PIPELINE_RUN_ID: 'run-1',
	  })
	  check('final evidence normalizes uppercase artifact hashes before comparing', () => {
	    assert.equal(uppercaseHash.status, 0)
	  })
	  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1')

	  const externalModelPath = join(ws, 'external-model.json')
	  writeFileSync(externalModelPath, '{"schemaVersion":1}\n')
	  const externalModelHash = fileHash(externalModelPath)
	  writeReport(reports, 'spec-compare', 'TASK_1_fixture', 'run-1', {
	    inputHashes: { [externalModelPath]: externalModelHash },
	    implementationModel: { path: 'external-model.json', hash: externalModelHash, schemaVersion: 1 },
	  })
	  const externalModel = run(['TASK_1_fixture', '--stage', 'final', '--require', 'spec-compare', '--fresh'], {
	    FIGMA_REPORTS_DIR: reports,
	    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
	    FIGMA_PIPELINE_RUN_ID: 'run-1',
	  })
	  check('final evidence accepts implementation model hash proven by inputHashes when report path is display-only', () => {
	    assert.equal(externalModel.status, 0)
	  })

	  const modelPath = join(ws, 'model.json')
	  writeFileSync(modelPath, '{"schemaVersion":1}\n')
  writeReport(reports, 'spec-compare', 'TASK_1_fixture', 'run-1', { implementationModel: { path: modelPath, hash: 'sha256:not-the-real-hash', schemaVersion: 1 } })
  const badModel = run(['TASK_1_fixture', '--stage', 'final', '--require', 'spec-compare', '--fresh'], {
    FIGMA_REPORTS_DIR: reports,
    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
    FIGMA_PIPELINE_RUN_ID: 'run-1',
  })
  const badModelReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
  check('final evidence rejects changed implementation model hash', () => {
    assert.notEqual(badModel.status, 0)
    assert.ok(badModelReport.issues.find((i) => i.issueKind === 'IMPLEMENTATION_MODEL_HASH_MISMATCH'))
  })

	  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1', { overall: 'BLOCKER', blockingCount: 1 })
  const failedRequired = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
    FIGMA_REPORTS_DIR: reports,
    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
    FIGMA_PIPELINE_RUN_ID: 'run-1',
  })
  const failedRequiredReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('final evidence exits non-zero when required report is BLOCKER', () => {
		    assert.notEqual(failedRequired.status, 0)
		    assert.equal(failedRequiredReport.overall, 'BLOCKER')
		  })

		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1', {
		    overall: 'PASS',
		    blockingCount: 1,
		    issues: [{ severity: 'BLOCKER', issueKind: 'IMPOSSIBLE_PASS', message: 'blocker hidden behind pass' }],
		  })
		  const contradictory = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
		    FIGMA_REPORTS_DIR: reports,
		    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		    FIGMA_PIPELINE_RUN_ID: 'run-1',
		  })
		  const contradictoryReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('final evidence rejects PASS reports with blocker issues', () => {
		    assert.notEqual(contradictory.status, 0)
		    assert.ok(contradictoryReport.issues.find((i) => i.issueKind === 'REPORT_OVERALL_CONTRADICTS_BLOCKERS' && i.reportName === 'screenshot'))
		  })

		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1', {
		    overall: 'PASS',
			    inputs: { captureStartedAt: '2026-01-01T00:00:00.000Z', captureMode: 'recorded' },
		    results: [{ screen: 'Home', themeKey: 'primary', status: 'STALE_CAPTURE', reason: 'forged stale result hidden behind PASS' }],
		  })
		  const staleResultPass = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
		    FIGMA_REPORTS_DIR: reports,
		    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		    FIGMA_PIPELINE_RUN_ID: 'run-1',
		  })
		  const staleResultPassReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('final evidence rejects screenshot stale result hidden behind PASS', () => {
		    assert.notEqual(staleResultPass.status, 0)
		    assert.ok(staleResultPassReport.issues.find((i) => i.issueKind === 'REPORT_SCREENSHOT_RESULT_CONTRADICTS_OVERALL' && i.reportName === 'screenshot'))
		  })

		  // A locale-mismatch row (always-BLOCKER completeness class) forged behind
		  // a PASS overall must be caught by the same contradiction guard.
		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1', {
		    overall: 'PASS',
			    inputs: { captureStartedAt: '2026-01-01T00:00:00.000Z', captureMode: 'recorded' },
		    results: [{ screen: 'Home', themeKey: 'primary', status: 'CAPTURE_LOCALE_MISMATCH', reason: 'forged locale-mismatch result hidden behind PASS' }],
		  })
		  const localeResultPass = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
		    FIGMA_REPORTS_DIR: reports,
		    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		    FIGMA_PIPELINE_RUN_ID: 'run-1',
		  })
		  const localeResultPassReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('final evidence rejects screenshot CAPTURE_LOCALE_MISMATCH hidden behind PASS', () => {
		    assert.notEqual(localeResultPass.status, 0)
		    assert.ok(localeResultPassReport.issues.find((i) => i.issueKind === 'REPORT_SCREENSHOT_RESULT_CONTRADICTS_OVERALL' && i.reportName === 'screenshot'))
		  })

		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1', {
		    results: [{ screen: 'Home', themeKey: 'primary', status: 'FUTURE_PASS' }],
		    disableArtifacts: true,
		  })
		  const unknownResult = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
		    FIGMA_REPORTS_DIR: reports,
		    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		    FIGMA_PIPELINE_RUN_ID: 'run-1',
		  })
		  const unknownResultReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('final evidence rejects an unknown screenshot result status', () => {
		    assert.notEqual(unknownResult.status, 0)
		    assert.ok(unknownResultReport.issues.find((i) => i.issueKind === 'REPORT_SCREENSHOT_RESULT_UNKNOWN' && i.reportName === 'screenshot'))
		    assert.ok(unknownResultReport.issues.find((i) => i.issueKind === 'REPORT_BODY_SCHEMA_INVALID' && i.reportName === 'screenshot'))
		  })

		  // A compare run under the fixture-only FIGMA_* locale env overrides
		  // (recorded by the comparator as designLocaleEnvOverrides) may never certify final
		  // evidence — the overrides can redirect or disarm the capture-locale witness.
		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1', {
		    overall: 'PASS',
			    inputs: { captureStartedAt: '2026-01-01T00:00:00.000Z', captureMode: 'recorded' },
		    designLocaleEnvOverrides: ['FIGMA_DESIGN_LOCALE'],
		  })
		  const localeEnvOverride = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
		    FIGMA_REPORTS_DIR: reports,
		    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		    FIGMA_PIPELINE_RUN_ID: 'run-1',
		  })
		  const localeEnvOverrideReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
			  check('final evidence rejects a compare run under FIGMA_* locale env overrides (LOCALE_ENV_OVERRIDE)', () => {
		    assert.notEqual(localeEnvOverride.status, 0)
		    const hit = localeEnvOverrideReport.issues.find((i) => i.issueKind === 'LOCALE_ENV_OVERRIDE')
		    assert.ok(hit, 'LOCALE_ENV_OVERRIDE blocker present')
			    assert.match(hit.message, /FIGMA_DESIGN_LOCALE/)
			  })

			  writeReport(reports, 'capture-config', 'TASK_1_fixture', 'run-1', {
			    designLocaleEnvOverrides: ['FIGMA_SUPPORTED_LOCALES'],
			  })
			  const captureLocaleEnvOverride = run(['TASK_1_fixture', '--stage', 'final', '--require', 'capture-config', '--fresh'], {
			    FIGMA_REPORTS_DIR: reports,
			    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
			    FIGMA_PIPELINE_RUN_ID: 'run-1',
			  })
			  const captureLocaleEnvOverrideReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
			  check('final evidence rejects capture-config locale env overrides', () => {
			    assert.notEqual(captureLocaleEnvOverride.status, 0)
			    const hit = captureLocaleEnvOverrideReport.issues.find((i) => i.issueKind === 'LOCALE_ENV_OVERRIDE' && i.reportName === 'capture-config')
			    assert.ok(hit)
			    assert.match(hit.message, /FIGMA_SUPPORTED_LOCALES/)
			  })
			  writeReport(reports, 'capture-config', 'TASK_1_fixture', 'run-1')

		  // #23 judge-strictness pins — env-weakened knobs recorded in the report must block final.
		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1', {
		    thresholds: { pass: 0.2, minor: 0.1, major: 0.05, aspectTolerance: 0.15, minCoverage: 0.005 },
		  })
		  const weakened = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
		    FIGMA_REPORTS_DIR: reports,
		    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		    FIGMA_PIPELINE_RUN_ID: 'run-1',
		  })
		  const weakenedReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('final evidence rejects env-weakened screenshot thresholds', () => {
		    assert.notEqual(weakened.status, 0)
		    assert.ok(weakenedReport.issues.find((i) => i.issueKind === 'THRESHOLDS_WEAKENED' && i.reportName === 'screenshot'))
		  })

		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1', { metric: 'unknown-metric' })
		  const mismatchedMetric = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
		    FIGMA_REPORTS_DIR: reports,
		    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		    FIGMA_PIPELINE_RUN_ID: 'run-1',
		  })
		  const mismatchedMetricReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('final evidence rejects a mismatched screenshot metric', () => {
		    assert.notEqual(mismatchedMetric.status, 0)
		    assert.ok(mismatchedMetricReport.issues.find((i) => i.issueKind === 'METRIC_MISMATCH' && i.reportName === 'screenshot'))
		  })

		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1', { thresholds: undefined })
		  const unrecorded = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
		    FIGMA_REPORTS_DIR: reports,
		    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		    FIGMA_PIPELINE_RUN_ID: 'run-1',
		  })
		  const unrecordedReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('final evidence rejects a screenshot report that records no thresholds', () => {
		    assert.notEqual(unrecorded.status, 0)
		    assert.ok(unrecordedReport.issues.find((i) => i.issueKind === 'THRESHOLDS_UNRECORDED' && i.reportName === 'screenshot'))
		  })

		  // majorBand is now recorded by compare-screenshots; an advisory rollback value is a final-gate blocker.
		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1', {
		    thresholds: { pass: 0.90, minor: 0.80, major: 0.65, aspectTolerance: 0.15, minCoverage: 0.005, majorBand: 'advisory' },
		  })
		  const advisoryBand = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
		    FIGMA_REPORTS_DIR: reports,
		    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		    FIGMA_PIPELINE_RUN_ID: 'run-1',
		  })
		  const advisoryBandReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('final evidence rejects a recorded advisory majorBand', () => {
		    assert.notEqual(advisoryBand.status, 0)
		    assert.ok(advisoryBandReport.issues.find((i) => i.issueKind === 'THRESHOLDS_WEAKENED' && i.reportName === 'screenshot'))
		  })
		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1')

		  // The color-only fallback makes minCoverage/bgTolerance/deltaEPass gate-deciding.
		  // The reviewer's proven bypass forces every screen into color-only mode and passes any
		  // divergence; each knob raised past canon must now be a final-gate THRESHOLDS_WEAKENED.
		  // The metric-INTERNAL knobs join the loop (second proven bypass): SHIFT_RADIUS=4 +
		  // GAUSSIAN_SIGMA=3 + AA_TOLERANCE=3 turned a 0.395 BLOCKER into PASS 1.000, and
		  // MASK_MODE=color / VAR_FLOOR=65025 hid deleted dark-on-dark panels from the mask.
		  // Zone-gate knobs are canonical too: zoneGate=false disables the per-zone floor,
		  // zoneBlocker lowered lets a broken non-text zone pass, minRegionPx raised skips small
		  // zones. zoneTextBlocker is present but NOT a bypass vector (see the dedicated pin below).
		  const CANON_THR = { pass: 0.90, minor: 0.80, major: 0.65, aspectTolerance: 0.15, minCoverage: 0.005, bgTolerance: 24, deltaEPass: 3, majorBand: 'block', colorAxis: true, extraContentBand: 'warn', extraContentWarn: 0.02, extraContentDeltaE: 10, extraContentRingPx: 3, shiftRadius: 2, gaussianSigma: 1.5, aaTolerance: 1, varFloor: 12, maskMode: 'variance', deltaEStride: 2, regionGrid: '8x4', zoneGate: true, zoneBlocker: 0.35, zoneTextBlocker: 0.25, minRegionPx: 400, statusBarDp: 24, navBarDp: 48, pixelGate: 'strict' }
		  for (const [label, patch] of [
		    ['minCoverage=1', { minCoverage: 1 }],
		    // Device-chrome band knobs: a raised band over-masks (weaker) → THRESHOLDS_WEAKENED.
		    ['statusBarDp=400', { statusBarDp: 400 }],
		    ['navBarDp=400', { navBarDp: 400 }],
		    ['bgTolerance=442', { bgTolerance: 442 }],
		    ['deltaEPass=100', { deltaEPass: 100 }],
		    ['extraContentBand=off', { extraContentBand: 'off' }],
		    ['extraContentWarn=0.5', { extraContentWarn: 0.5 }],
		    ['extraContentDeltaE=50', { extraContentDeltaE: 50 }],
		    ['extraContentRingPx=8', { extraContentRingPx: 8 }],   // a raised dilation ring blinds probe space
		    ['shiftRadius=4', { shiftRadius: 4 }],
		    ['gaussianSigma=3', { gaussianSigma: 3 }],
		    ['aaTolerance=3', { aaTolerance: 3 }],
		    ['varFloor=65025', { varFloor: 65025 }],
		    ['maskMode=color', { maskMode: 'color' }],
		    ['zoneGate=false', { zoneGate: false }],
		    ['zoneBlocker=0.2', { zoneBlocker: 0.2 }],
		    ['minRegionPx=4000', { minRegionPx: 4000 }],
		    // The recorded pixel-verdict routing may never be weaker than the committed
		    // project-config screenshotPixelGate — a per-run SCREENSHOT_PIXEL_GATE downgrade used
		    // to certify silently. The rows adapt to the HOST repo's committed value: a vendored
		    // product may legitimately commit `advisory` (equal-to-committed must NOT block there),
		    // so only strictly-weaker candidates are probed.
		    ...(() => {
		      const rank = { off: 0, advisory: 1, strict: 2 }
		      let committed = 'strict'
		      try {
		        const cfg = readFileSync(join(HERE, '..', '..', 'project-config.md'), 'utf8')
		        const m = /^screenshotPixelGate:\s*(\S+)$/m.exec(cfg)
		        if (m && rank[m[1]] !== undefined) committed = m[1]
		      } catch { /* absent config = the loader's strict default */ }
		      return ['advisory', 'off']
		        .filter((v) => rank[v] < rank[committed])
		        .map((v) => [`pixelGate=${v} (weaker than committed ${committed})`, { pixelGate: v }])
		    })(),
		  ]) {
		    writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1', { thresholds: { ...CANON_THR, ...patch } })
		    const weak = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
		      FIGMA_REPORTS_DIR: reports,
		      FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		      FIGMA_PIPELINE_RUN_ID: 'run-1',
		    })
		    const weakReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		    check(`final evidence rejects color-only bypass knob ${label} (THRESHOLDS_WEAKENED)`, () => {
		      assert.notEqual(weak.status, 0, `${label} must block final`)
		      assert.ok(weakReport.issues.find((i) => i.issueKind === 'THRESHOLDS_WEAKENED' && i.reportName === 'screenshot'), `${label} → THRESHOLDS_WEAKENED`)
		    })
		  }
		  // Counter-case: the canonical (all-knobs) report still PASSes — the guard is not over-tight.
		  // Reset spec-compare: an earlier test left a bad-model-hash spec-compare report on disk and
		  // the fresh block ALWAYS re-checks its implementationModel — restore a clean one so this
		  // This pin isolates the threshold guard rather than tripping on that stale report.
		  writeReport(reports, 'spec-compare', 'TASK_1_fixture', 'run-1')
		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1', { thresholds: { ...CANON_THR } })
		  const canonPass = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
		    FIGMA_REPORTS_DIR: reports,
		    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		    FIGMA_PIPELINE_RUN_ID: 'run-1',
		  })
		  const canonPassReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('a canonical screenshot report (all color-only knobs at canon) still PASSes final', () => {
		    assert.equal(canonPass.status, 0)
		    assert.equal(canonPassReport.overall, 'PASS')
		    assert.ok(!canonPassReport.issues.some((i) => i.issueKind === 'THRESHOLDS_WEAKENED'), 'no weakening flagged at canon')
		  })

		  // zoneTextBlocker is NOT a bypass vector: a LOWERED text floor only relaxes by-design text
		  // leniency (structure covered independently by spec-compare + ΔE), never a non-text zone,
		  // so it must NOT flag at final. (A raised text floor is stricter — also fine.)
		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1', { thresholds: { ...CANON_THR, zoneTextBlocker: 0.05 } })
		  const loweredTextFloor = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
		    FIGMA_REPORTS_DIR: reports,
		    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		    FIGMA_PIPELINE_RUN_ID: 'run-1',
		  })
		  const loweredTextFloorReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('zone-gate canon: a lowered zoneTextBlocker is NOT a bypass (does not flag at final)', () => {
		    assert.equal(loweredTextFloor.status, 0)
		    assert.ok(!loweredTextFloorReport.issues.some((i) => i.issueKind === 'THRESHOLDS_WEAKENED'), 'lenient text floor is by-design, never weakening')
		  })

		  // ── Owner pixel-review flow — routed rows, hash-bound receipts, class pin ────────────
		  // A REVIEW_REQUIRED row (a classed component the metric is blind on) keeps the final
		  // bundle non-shippable until the OWNER's receipt resolves it; every binding (run id,
		  // sealed report bytes, reviewed artifact pixels) must hold or the receipt is INERT.
		  const reviewDir = join(ws, 'pixel-review')
		  mkdirSync(reviewDir, { recursive: true })
		  const reviewEnv = {
		    FIGMA_REPORTS_DIR: reports,
		    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		    FIGMA_PIPELINE_RUN_ID: 'run-1',
		    FIGMA_PIXEL_REVIEW_DIR: reviewDir,
		  }
		  const reviewRow = { screen: 'Home', themeKey: 'primary', status: 'REVIEW_REQUIRED', pixelStatus: 'BLOCKER', renderClass: 'canvas', score: 0.27 }
		  const reviewWarn = { severity: 'WARN', issueKind: 'PIXEL_REVIEW_REQUIRED', pixelKind: 'SSIM_MAJOR', screen: 'Home', message: 'ssim below floor (renderClass: canvas — owner review decides)' }
		  // The routed WARN issue rides the report like the real comparator emits it — a pass
		  // verdict must still yield PASS (resolved review warns do not count as other warns).
		  const seedReviewShot = () => writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1', { overall: 'REVIEW_REQUIRED', results: [reviewRow], issues: [reviewWarn], warningCount: 1, classRouting: {} })
		  const artDir = join(cacheRoot, 'artifacts/screenshot/TASK_1_fixture/run-1/001-home-primary')
		  const artHash = (kind) => fileHash(join(artDir, `${kind}.png`))
		  const shotHash = () => fileHash(join(reports, 'screenshot-TASK_1_fixture.json'))
		  const writeReceipt = (verdict, overrides = {}) => writeFileSync(join(reviewDir, 'TASK_1_fixture.json'), JSON.stringify({
		    schemaVersion: 1, stem: 'TASK_1_fixture',
		    rows: [{ screen: 'Home', theme: 'primary', verdict, note: overrides.note || '', pipelineRunId: 'run-1', reportHash: shotHash(), figmaHash: artHash('figma'), actualHash: artHash('actual'), at: '2026-07-11T12:00:00.000Z', by: 'site-owner', ...overrides }],
		  }, null, 2))
		  const runReview = () => {
		    const r = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], reviewEnv)
		    return { status: r.status, report: JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8')) }
		  }
		  rmSync(join(reviewDir, 'TASK_1_fixture.json'), { force: true })
		  seedReviewShot()
		  const pending = runReview()
		  check('a REVIEW_REQUIRED row with no receipt keeps the final bundle non-shippable (exit 1)', () => {
		    assert.notEqual(pending.status, 0)
		    assert.equal(pending.report.overall, 'REVIEW_REQUIRED')
		    assert.equal(pending.report.pixelReview.pending.length, 1)
		    assert.equal(pending.report.pixelReview.pending[0].renderClass, 'canvas')
		  })
		  writeReceipt('pass')
		  const passed = runReview()
		  check('a hash-valid PASS receipt resolves the row — final PASS + audited resolution + effective digest', () => {
		    assert.equal(passed.status, 0, JSON.stringify(passed.report.issues))
		    assert.equal(passed.report.overall, 'PASS')
		    assert.equal(passed.report.pixelReview.resolved[0].verdict, 'pass')
		    assert.equal(passed.report.pixelReview.resolved[0].by, 'site-owner')
		    const digest = readFileSync(join(reports, 'figma-meta-TASK_1_fixture.txt'), 'utf8')
		    assert.match(digest, /Home\/primary:PASS/, 'digest records the EFFECTIVE reviewed status')
		    assert.match(digest, /problemCount=0/)
		  })
		  writeReceipt('minor', { note: 'arc trail differs' })
		  const minored = runReview()
		  check('a MINOR receipt lands on the reviewed-WARN path (overall WARN + caveat-coupled issue)', () => {
		    assert.equal(minored.report.overall, 'WARN')
		    assert.ok(minored.report.issues.some((i) => i.issueKind === 'PIXEL_REVIEWED_MINOR' && /arc trail differs/.test(i.message)))
		  })
		  writeReceipt('fail', { note: 'missing area fill' })
		  const failed = runReview()
		  check('a FAIL receipt is a routed BLOCKER carrying the owner note', () => {
		    assert.notEqual(failed.status, 0)
		    assert.equal(failed.report.overall, 'BLOCKER')
		    assert.ok(failed.report.issues.some((i) => i.issueKind === 'PIXEL_REVIEW_FAILED' && /missing area fill/.test(i.message)))
		  })
		  writeReceipt('pass')
		  writeFileSync(join(artDir, 'actual.png'), 'tampered-after-review\n')
		  const tampered = runReview()
		  check('artifact bytes changed after the click → receipt inert and the tamper blocks', () => {
		    assert.notEqual(tampered.status, 0)
		    // The artifact-integrity net catches the byte change FIRST (BLOCKER — stronger than a
		    // mere back-to-review); the receipt is independently inert (stale WARN, zero resolved).
		    assert.equal(tampered.report.overall, 'BLOCKER')
		    assert.ok(tampered.report.issues.some((i) => i.issueKind === 'PIXEL_REVIEW_STALE'))
		    assert.equal(tampered.report.pixelReview.resolved.length, 0)
		  })
		  writeFileSync(join(artDir, 'actual.png'), 'actual-png-fixture\n')   // restore
		  writeReceipt('pass', { pipelineRunId: 'another-run' })
		  const replayed = runReview()
		  check('a receipt replayed from another run is inert', () => {
		    assert.notEqual(replayed.status, 0)
		    assert.equal(replayed.report.overall, 'REVIEW_REQUIRED')
		  })
		  rmSync(join(reviewDir, 'TASK_1_fixture.json'), { force: true })
		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1', { overall: 'REVIEW_REQUIRED', results: [reviewRow], classRouting: { Home: 'canvas' } })
		  const staleClass = runReview()
		  check('a routed row whose class the live registry no longer carries → CLASS_ROUTING_STALE blocker', () => {
		    assert.notEqual(staleClass.status, 0)
		    assert.ok(staleClass.report.issues.some((i) => i.issueKind === 'CLASS_ROUTING_STALE'))
		  })
		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1')   // restore the canonical PASS report for the pins below

		  // An absent zoneGate → THRESHOLDS_UNRECORDED like the other required bools/numerics: a
		  // report that does not record the per-zone floor's on/off state cannot prove it ran.
		  const { zoneGate: _zg, ...THR_NO_ZONEGATE } = CANON_THR
		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1', { thresholds: THR_NO_ZONEGATE })
		  const unrecordedZoneGate = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
		    FIGMA_REPORTS_DIR: reports,
		    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		    FIGMA_PIPELINE_RUN_ID: 'run-1',
		  })
		  const unrecordedZoneGateReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('zone-gate canon: an absent zoneGate → THRESHOLDS_UNRECORDED', () => {
		    assert.notEqual(unrecordedZoneGate.status, 0)
		    assert.ok(unrecordedZoneGateReport.issues.some((i) => i.issueKind === 'THRESHOLDS_UNRECORDED' && /zoneGate/.test(i.message)), 'zoneGate absence flagged')
		  })
		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1')

		  // Absent metric-internal knobs → THRESHOLDS_UNRECORDED like the numeric set: a report
		  // written by a pre-recording compare-screenshots (or with the keys hand-stripped) cannot
		  // prove the mask/shift/blur ran at canonical strictness.
		  const { maskMode: _mm, regionGrid: _rg, ...THR_NO_INTERNALS } = CANON_THR
		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1', { thresholds: THR_NO_INTERNALS })
		  const unrecordedInternals = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
		    FIGMA_REPORTS_DIR: reports,
		    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		    FIGMA_PIPELINE_RUN_ID: 'run-1',
		  })
		  const unrecordedInternalsReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('final evidence rejects a screenshot report missing maskMode/regionGrid (THRESHOLDS_UNRECORDED)', () => {
		    assert.notEqual(unrecordedInternals.status, 0)
		    assert.ok(unrecordedInternalsReport.issues.some((i) => i.issueKind === 'THRESHOLDS_UNRECORDED' && /maskMode/.test(i.message)), 'maskMode absence flagged')
		    assert.ok(unrecordedInternalsReport.issues.some((i) => i.issueKind === 'THRESHOLDS_UNRECORDED' && /regionGrid/.test(i.message)), 'regionGrid absence flagged')
		  })
		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1')

		  // A FINER grid is also weakening: 32x32 cells on a dp-sized frame all fall below
		  // minRegionPx eligibility, silently disabling the worst-zone grid fallback (the only
		  // zone floor when the spec projects no zones) while every other canon pin stays green.
		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1', { thresholds: { ...CANON_THR, regionGrid: '32x32' } })
		  const finerGrid = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
		    FIGMA_REPORTS_DIR: reports,
		    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		    FIGMA_PIPELINE_RUN_ID: 'run-1',
		  })
		  const finerGridReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('final evidence rejects a FINER-than-canonical regionGrid (zone-fallback neutralized → THRESHOLDS_WEAKENED)', () => {
		    assert.notEqual(finerGrid.status, 0)
		    assert.ok(finerGridReport.issues.some((i) => i.issueKind === 'THRESHOLDS_WEAKENED' && /finer than the canonical/.test(i.message)), 'finer grid flagged')
		  })
		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1')

		  writeReport(reports, 'spec-compare', 'TASK_1_fixture', 'run-1', {
		    overall: 'PASS',
		    comparisons: [{ screen: 'Home', theme: 'light', file: 'Home.spec.json', stableId: 'title', elementName: 'Title', status: 'REVIEW', engine: 'unsupported', promoted: true }],
		  })
		  const reviewComparisonPass = run(['TASK_1_fixture', '--stage', 'final', '--require', 'spec-compare', '--fresh'], {
		    FIGMA_REPORTS_DIR: reports,
		    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		    FIGMA_PIPELINE_RUN_ID: 'run-1',
		  })
		  const reviewComparisonPassReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('final evidence rejects spec-compare REVIEW hidden behind PASS', () => {
		    assert.notEqual(reviewComparisonPass.status, 0)
		    assert.ok(reviewComparisonPassReport.issues.find((i) => i.issueKind === 'REPORT_SPEC_COMPARE_REVIEW_CONTRADICTS_PASS' && i.reportName === 'spec-compare'))
		  })

		  writeFileSync(join(screens, 'Home.spec.json'), JSON.stringify(currentSpec(), null, 2) + '\n')
		  writeReport(reports, 'spec-compare', 'TASK_1_fixture', 'run-1', { overall: 'PASS', comparisons: [] })
		  const missingComparison = run(['TASK_1_fixture', '--stage', 'final', '--require', 'spec-compare', '--fresh'], {
		    FIGMA_REPORTS_DIR: reports,
		    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		    FIGMA_PIPELINE_RUN_ID: 'run-1',
		  })
		  const missingComparisonReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('final evidence rejects spec-compare PASS missing comparison rows', () => {
		    assert.notEqual(missingComparison.status, 0)
		    assert.ok(missingComparisonReport.issues.find((i) => i.issueKind === 'SPEC_COMPARE_COVERAGE_INCOMPLETE' && i.reportName === 'spec-compare'))
		  })
		  writeFileSync(join(screens, 'Home.spec.json'), JSON.stringify(currentSpec(), null, 2) + '\n')
		  writeReport(reports, 'spec-compare', 'TASK_1_fixture', 'run-1')

		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1', { overall: 'SKIPPED' })
		  const skipped = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
		    FIGMA_REPORTS_DIR: reports,
		    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		    FIGMA_PIPELINE_RUN_ID: 'run-1',
		  })
		  const skippedReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('final evidence rejects skipped required reports', () => {
		    assert.notEqual(skipped.status, 0)
		    assert.ok(skippedReport.issues.find((i) => i.issueKind === 'REPORT_REQUIRED_SKIPPED' && i.reportName === 'screenshot'))
		  })
		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1')

	  const inputFile = join(ws, 'HomeScreen.spec.json')
	  writeFileSync(inputFile, '{"screen":"HomeScreen"}\n')
	  writeReport(reports, 'check-spec', 'TASK_1_fixture', 'run-1', { inputHashes: { [inputFile]: fileHash(inputFile) } })
	  writeFileSync(inputFile, '{"screen":"ChangedScreen"}\n')
	  const changedInput = run(['TASK_1_fixture', '--stage', 'final', '--require', 'check-spec', '--fresh'], {
	    FIGMA_REPORTS_DIR: reports,
	    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
	    FIGMA_PIPELINE_RUN_ID: 'run-1',
	  })
	  const changedInputReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
	  check('fresh final evidence rejects required report input hash drift', () => {
	    assert.notEqual(changedInput.status, 0)
	    assert.ok(changedInputReport.issues.find((i) => i.issueKind === 'REPORT_INPUT_HASH_MISMATCH' && i.reportName === 'check-spec'))
	  })

	  writeFileSync(join(reports, 'census-TASK_1_fixture.json'), JSON.stringify({ taskStem: 'TASK_1_fixture', pipelineRunId: 'run-1' }, null, 2))
	  const malformed = run(['TASK_1_fixture', '--stage', 'final', '--require', 'census', '--fresh'], {
	    FIGMA_REPORTS_DIR: reports,
	    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
	    FIGMA_PIPELINE_RUN_ID: 'run-1',
	  })
	  const malformedReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
	  check('final evidence rejects malformed required reports with unknown verdict', () => {
	    assert.notEqual(malformed.status, 0)
	    assert.ok(malformedReport.issues.find((i) => i.issueKind === 'REPORT_SCHEMA_INVALID' && i.reportName === 'census'))
	    assert.ok(malformedReport.issues.find((i) => i.issueKind === 'REPORT_INVALID_VERDICT' && i.reportName === 'census'))
	  })

	  writeFileSync(join(reports, 'census-TASK_1_fixture.json'), JSON.stringify({
	    ...report('census', 'TASK_1_fixture', 'run-1'),
	    version: undefined,
	    screens: undefined,
	    components: undefined,
	    missing: undefined,
	    incomplete: undefined,
	    ambiguous: undefined,
	  }, null, 2))
	  const bodyMalformed = run(['TASK_1_fixture', '--stage', 'final', '--require', 'census', '--fresh'], {
	    FIGMA_REPORTS_DIR: reports,
	    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
	    FIGMA_PIPELINE_RUN_ID: 'run-1',
	  })
	  const bodyMalformedReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
	  check('final evidence rejects envelope-valid but body-malformed required reports', () => {
	    assert.notEqual(bodyMalformed.status, 0)
	    assert.ok(bodyMalformedReport.issues.find((i) => i.issueKind === 'REPORT_BODY_SCHEMA_INVALID' && i.reportName === 'census'))
	  })

	  writeReport(reports, 'check-spec', 'TASK_1_fixture', 'run-1', { inputHashes: { [inputFile]: 'not-a-sha256' } })
	  const invalidHash = run(['TASK_1_fixture', '--stage', 'final', '--require', 'check-spec', '--fresh'], {
	    FIGMA_REPORTS_DIR: reports,
	    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
	    FIGMA_PIPELINE_RUN_ID: 'run-1',
	  })
	  const invalidHashReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('fresh final evidence rejects malformed inputHashes values', () => {
		    assert.notEqual(invalidHash.status, 0)
		    assert.ok(invalidHashReport.issues.find((i) => i.issueKind === 'REPORT_INPUT_HASH_INVALID' && i.reportName === 'check-spec'))
		  })

		  writeReport(reports, 'check-spec', 'TASK_1_fixture', 'run-1', { inputHashes: {} })
		  const emptyHashes = run(['TASK_1_fixture', '--stage', 'final', '--require', 'check-spec', '--fresh'], {
		    FIGMA_REPORTS_DIR: reports,
		    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		    FIGMA_PIPELINE_RUN_ID: 'run-1',
		  })
		  const emptyHashesReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('fresh final evidence rejects empty inputHashes on input-dependent reports', () => {
		    assert.notEqual(emptyHashes.status, 0)
		    assert.ok(emptyHashesReport.issues.find((i) => i.issueKind === 'REPORT_INPUT_HASHES_EMPTY' && i.reportName === 'check-spec'))
		  })

		  writeReport(reports, 'spec-compare', 'TASK_1_fixture', 'run-1', { overall: 'BLOCKER', blockingCount: 1, issues: [{ severity: 'BLOCKER', issueKind: 'OPTIONAL_BLOCKER', message: 'optional blocker' }] })
		  writeReport(reports, 'screenshot', 'TASK_1_fixture', 'run-1')
		  const optionalBlocker = run(['TASK_1_fixture', '--stage', 'final', '--require', 'screenshot', '--fresh'], {
		    FIGMA_REPORTS_DIR: reports,
		    FIGMA_SCREEN_CACHE_ROOT: screensRoot,
		    FIGMA_PIPELINE_RUN_ID: 'run-1',
		  })
		  const optionalBlockerReport = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
		  check('final evidence overall ignores optional blocker reports outside --require', () => {
		    assert.equal(optionalBlocker.status, 0)
		    assert.equal(optionalBlockerReport.overall, 'PASS')
		  })

		  const prebuildWs = join(ws, 'prebuild-incomplete')
	  const prebuildReports = join(prebuildWs, 'reports')
	  const prebuildScreensRoot = join(prebuildWs, 'screens')
	  mkdirSync(prebuildReports, { recursive: true })
	  mkdirSync(join(prebuildScreensRoot, 'TASK_1_fixture'), { recursive: true })
		  writeReport(prebuildReports, 'census', 'TASK_1_fixture', 'run-1', { overall: 'INCOMPLETE', warningCount: 1, issues: [{ severity: 'REVIEW_REQUIRED', issueKind: 'COMPONENT_MISSING', message: 'missing component' }] })
	  const prebuildIncomplete = run(['TASK_1_fixture', '--stage', 'prebuild', '--require', 'census', '--fresh'], {
	    FIGMA_REPORTS_DIR: prebuildReports,
	    FIGMA_SCREEN_CACHE_ROOT: prebuildScreensRoot,
	    FIGMA_PIPELINE_RUN_ID: 'run-1',
	  })
	  const prebuildIncompleteReport = JSON.parse(readFileSync(join(prebuildReports, 'evidence-TASK_1_fixture.json'), 'utf8'))
	  check('prebuild evidence can carry INCOMPLETE advisory reports without non-zero exit', () => {
	    assert.equal(prebuildIncomplete.status, 0)
	    assert.equal(prebuildIncompleteReport.overall, 'INCOMPLETE')
	  })
	  // Design-consistency (DESIGN_CHANGED_SINCE_CHECK) coverage — the whole-file task hash was
	  // replaced by the ## Design section hash so the legitimate Step-6a/6b appendix writes no
	  // longer deadlock the ship; these three cases pin the new contract.
	  const dcWs = join(ws, 'design-consistency')
	  const dcReports = join(dcWs, 'reports')
	  const dcScreensRoot = join(dcWs, 'screens')
	  mkdirSync(dcReports, { recursive: true })
	  const dcScreens = join(dcScreensRoot, 'TASK_1_fixture')
	  mkdirSync(dcScreens, { recursive: true })
	  writeCurrentScreenIndex(dcScreens)
	  const dcTask = join(dcWs, 'TASK_1_fixture.md')
	  const dcBody = '## Goal\n\nx\n\n## Design\n\n- Home — https://www.figma.com/design/AAA?node-id=1-2\n\n## Outcome\n\n- done\n'
	  writeFileSync(dcTask, dcBody)
	  const dcHash = parseDesignSources([dcBody]).sourceHash
	  const dcEnv = { FIGMA_REPORTS_DIR: dcReports, FIGMA_SCREEN_CACHE_ROOT: dcScreensRoot, FIGMA_PIPELINE_RUN_ID: 'run-1', FIGMA_SCREEN_TASK_FILE: dcTask }

	  for (const name of ['screen-cache', 'check-spec', 'capture-config', 'census', 'spec', 'spec-compare', 'screenshot']) writeReport(dcReports, name, 'TASK_1_fixture', 'run-1')
	  writeReport(dcReports, 'screen-cache', 'TASK_1_fixture', 'run-1', { inputs: { designSourceHash: dcHash } })
	  const dcPass = run(['TASK_1_fixture', '--stage', 'final', '--fresh'], dcEnv)
	  check('final evidence passes when the current ## Design section matches the recorded hash', () => assert.equal(dcPass.status, 0, dcPass.stdout + dcPass.stderr + readFileSync(join(dcReports, 'evidence-TASK_1_fixture.json'), 'utf8')))
	  // #45 — the emitted digest binds the certification to the ## Design section it compared.
	  check('final digest carries the certified ## Design section hash (designHash)', () => {
	    const digest = readFileSync(join(dcReports, 'figma-meta-TASK_1_fixture.txt'), 'utf8')
	    assert.ok(digest.includes(`designHash=${dcHash}`), `digest should carry designHash=${dcHash}; got: ${digest}`)
	  })

	  // safeCell: rows= cells are comma-joined, so a literal `,` in a screen name must be
	  // stripped like the ;=/CR/LF delimiters or it inflates the displayed cell count. The
	  // artifact set must carry the SAME comma name (rows/sets are screen-bound —
	  // SCREENSHOT_ARTIFACT_SET_MISMATCH otherwise), so build one and bypass the helper's
	  // hardcoded 'Home' set via disableArtifacts.
	  const commaSet = screenshotArtifactSet('TASK_1_fixture', 'run-1')
	  commaSet.screen = 'Ho,me'
	  for (const a of Object.values(commaSet.artifacts)) a.screen = 'Ho,me'
	  writeReport(dcReports, 'screenshot', 'TASK_1_fixture', 'run-1', {
	    disableArtifacts: true,
	    results: [{ screen: 'Ho,me', themeKey: 'primary', status: 'PASS', score: 1, artifactSet: commaSet }],
	    artifactSet: { schemaVersion: 1, kind: 'screenshot-compare-artifact-set', root: 'artifacts/screenshot/TASK_1_fixture/run-1', entries: Object.values(commaSet.artifacts) },
	    artifactSets: [commaSet],
	  })
	  writeCurrentScreenIndex(dcScreens, 'TASK_1_fixture', 'Ho,me')
	  const dcComma = run(['TASK_1_fixture', '--stage', 'final', '--fresh'], dcEnv)
	  check('digest rows= strips literal commas from cells (comma-joined display stays one cell per row)', () => {
	    assert.equal(dcComma.status, 0, dcComma.stdout + dcComma.stderr)
	    const digest = readFileSync(join(dcReports, 'figma-meta-TASK_1_fixture.txt'), 'utf8')
	    assert.ok(digest.includes('rows=Ho-me/primary:PASS'), `comma stripped to '-'; got: ${digest}`)
	  })
	  writeCurrentScreenIndex(dcScreens)
	  writeReport(dcReports, 'screenshot', 'TASK_1_fixture', 'run-1')

	  // The section hash must survive appendix-only mutations (the exact deadlock this replaces).
	  writeFileSync(dcTask, dcBody + '\n### Execution log\n\n- Figma meta: appended-later\n')
	  const dcAppend = run(['TASK_1_fixture', '--stage', 'final', '--fresh'], dcEnv)
	  check('appendix-only task edits do not trip the design-consistency check', () => assert.equal(dcAppend.status, 0, dcAppend.stdout + dcAppend.stderr + readFileSync(join(dcReports, 'evidence-TASK_1_fixture.json'), 'utf8')))

	  writeFileSync(dcTask, dcBody.replace('node-id=1-2', 'node-id=9-9'))
	  const dcChanged = run(['TASK_1_fixture', '--stage', 'final', '--fresh'], dcEnv)
	  const dcChangedReport = JSON.parse(readFileSync(join(dcReports, 'evidence-TASK_1_fixture.json'), 'utf8'))
	  check('a post-check ## Design edit blocks final evidence (DESIGN_CHANGED_SINCE_CHECK)', () => {
	    assert.notEqual(dcChanged.status, 0)
	    assert.ok(dcChangedReport.issues.some((i) => i.issueKind === 'DESIGN_CHANGED_SINCE_CHECK'))
	  })

	  writeFileSync(dcTask, dcBody)
	  writeReport(dcReports, 'screen-cache', 'TASK_1_fixture', 'run-1', { inputs: {} })
	  const dcMissing = run(['TASK_1_fixture', '--stage', 'final', '--fresh'], dcEnv)
	  const dcMissingReport = JSON.parse(readFileSync(join(dcReports, 'evidence-TASK_1_fixture.json'), 'utf8'))
	  check('a screen-cache report without designSourceHash blocks final evidence', () => {
	    assert.notEqual(dcMissing.status, 0)
	    assert.ok(dcMissingReport.issues.some((i) => i.issueKind === 'DESIGN_SOURCE_HASH_MISSING'))
	  })

	  writeFileSync(dcTask, Buffer.from([0xff, 0xfe, 0xfd]))
	  writeReport(dcReports, 'screen-cache', 'TASK_1_fixture', 'run-1', { inputs: { designSourceHash: dcHash } })
	  const dcUnreadable = run(['TASK_1_fixture', '--stage', 'final', '--fresh'], dcEnv)
	  const dcUnreadableReport = JSON.parse(readFileSync(join(dcReports, 'evidence-TASK_1_fixture.json'), 'utf8'))
	  check('an unreadable task source blocks final evidence instead of hashing an empty Design section', () => {
	    assert.notEqual(dcUnreadable.status, 0)
	    assert.ok(dcUnreadableReport.issues.some((i) => i.issueKind === 'DESIGN_SOURCE_UNAVAILABLE'))
	  })
	  writeFileSync(dcTask, dcBody)
	  const dcMissingTask = run(['TASK_1_fixture', '--stage', 'final', '--fresh'], {
	    ...dcEnv,
	    FIGMA_SCREEN_TASK_FILE: join(dcWs, 'missing-task.md'),
	  })
	  const dcMissingTaskReport = JSON.parse(readFileSync(join(dcReports, 'evidence-TASK_1_fixture.json'), 'utf8'))
	  check('a missing task source blocks final evidence instead of certifying an empty Design section', () => {
	    assert.notEqual(dcMissingTask.status, 0)
	    assert.ok(dcMissingTaskReport.issues.some((i) => i.issueKind === 'DESIGN_SOURCE_UNAVAILABLE'))
	  })

	  // ── Ship receipts (3b closure) ─────────────────────────────────────────────────────────
	  // ship-done.mjs must persist committed receipts at ship time so verify-done.mjs can
	  // re-bind the shipped digest in a fresh CI clone (where the gitignored reports cache is
	  // gone). ship-done/verify-done resolve orchestrator/tasks/ + project-config.md relative
	  // to their own location (PROJECT_ROOT), so the harness is a physical scratch tree: the
	  // real scripts copied verbatim, token-schemas copied (the child final bundle compiles
	  // them, gate:true), node_modules symlinked (ajv).
	  const shipWs = join(ws, 'ship-receipts')
	  const tree = join(shipWs, 'tree')
	  const SHIP_SCRIPTS = join(tree, 'orchestrator', 'figma', 'scripts')
	  cpSync(join(HERE, '..', 'scripts'), SHIP_SCRIPTS, { recursive: true })
	  cpSync(join(HERE, '..', 'token-schemas'), join(tree, 'orchestrator', 'figma', 'token-schemas'), { recursive: true })
	  copyDesignReaderRuntime(tree)
	  // The committed screenshot-thresholds.json ships with the sidecar; the copied
	  // evidence-bundle/compare scripts hard-fail without it (no-fallback by design).
	  cpSync(join(HERE, '..', 'screenshot-thresholds.json'), join(tree, 'orchestrator', 'figma', 'screenshot-thresholds.json'))
	  // A vendored product ships orchestrator/contracts/ wholesale, and ship-done.mjs
	  // runtime-reads the outcome-shape contract SCRIPT-RELATIVE (no fallback) — so the
	  // scratch tree must carry it exactly like a real bootstrapped copy does.
	  mkdirSync(join(tree, 'orchestrator', 'contracts'), { recursive: true })
	  cpSync(join(HERE, '..', '..', 'contracts', 'outcome-shape.json'), join(tree, 'orchestrator', 'contracts', 'outcome-shape.json'))
	  const shipSupportedLocales = readSupportedLocales()
	  writeFileSync(join(tree, 'orchestrator', 'project-config.md'), `figmaEnabled: true\nsupportedLocales:\n${shipSupportedLocales.map((locale) => `  - ${locale}`).join('\n')}\n`)
	  captureDiscoveryCodeRoots = [tree, captureDiscoveryRoot]
	  captureDiscoveryLocales = readSupportedLocales(tree)
	  const shipTodoDir = join(tree, 'orchestrator', 'tasks', 'todo')
	  const shipDoneDir = join(tree, 'orchestrator', 'tasks', 'done')
	  mkdirSync(shipTodoDir, { recursive: true })
	  mkdirSync(shipDoneDir, { recursive: true })

	  const SHIP_STEM = 'TASK_9_ship_receipts'
	  // Frozen Outcome-appendix shape (ship-done outcomeShapeError, 3rd parser mirror): last
	  // `---` anchor + Status/Reviewer keys + all six ### sections, each non-empty.
	  const shipBody = `# Ship receipts fixture\n\n### Execution log\n\n- Figma meta: documentation example in the task body\n\n## Design\n\n- HomeScreen — https://www.figma.com/design/fileKey?node-id=1-2\n\n---\n\n## Outcome\n\n**Status**: completed\n**Reviewer**: internal-reviewer\n\n### Build gates\n\n- none\n\n### Runtime verify\n\n- Gate: skipped (fixture has no runtime change)\n- Result: n/a — fixture has no runtime change\n\n### Acceptance trace\n\n- none\n\n### Caveats\n\n- none\n\n### Follow-ups\n\n- none\n\n### Files touched\n\n- none\n\n### Execution log\n\n- built by fixture\n`
	  const shipReports = join(shipWs, 'reports')
	  const shipScreensRoot = join(shipWs, 'screens')
	  const shipReceiptsRoot = join(shipWs, 'receipts')
	  const shipReceiptsDir = join(shipReceiptsRoot, SHIP_STEM)
	  const shipDoneFile = join(shipDoneDir, `${SHIP_STEM}.md`)
	  mkdirSync(join(shipScreensRoot, SHIP_STEM), { recursive: true })
	  writeFileSync(join(shipScreensRoot, SHIP_STEM, 'HomeScreen.spec.json'), JSON.stringify(currentSpec('HomeScreen'), null, 2) + '\n')
	  writeCurrentScreenIndex(join(shipScreensRoot, SHIP_STEM), SHIP_STEM, 'HomeScreen')
	  const finalizationStateDir = join(tree, 'orchestrator', '.cache', 'tasks', 'finalizations')
	  const integrationsDir = join(tree, 'orchestrator', '.cache', 'tasks', 'integrations')
	  // Since worktree isolation Phase 4 a task reaches done/ ONLY inside an
	  // integration transaction: ship-done re-reads the WAL and refuses unless the
	  // stem is in its finalizer-preparing phase. The fixture publishes the same
	  // minimal record shape the transaction owner writes.
	  const writeIntegrationRecord = (taskStem) => {
	    mkdirSync(integrationsDir, { recursive: true })
	    writeFileSync(join(integrationsDir, `${taskStem}.json`), JSON.stringify({
	      version: 1, stem: taskStem, status: 'active', phase: 'finalizer-preparing',
	      phases: {
	        'product-applied': { intentAt: '2026-01-01T00:00:00.000Z', provenAt: '2026-01-01T00:00:00.000Z' },
	        'finalizer-preparing': { intentAt: '2026-01-01T00:00:01.000Z', provenAt: null },
	      },
	    }) + '\n')
	  }
	  const shipEnv = (runId, receiptsRoot = shipReceiptsRoot) => {
	    const transactionId = `fin-fixture-${runId}`
	    mkdirSync(finalizationStateDir, { recursive: true })
	    // ship-done is deliberately single-owner. Authorize every current todo
	    // fixture as if this test process were finalize-task's parent process.
	    for (const file of readdirSync(shipTodoDir).filter((name) => name.endsWith('.md'))) {
	      const taskStem = file.slice(0, -3)
	      const text = readFileSync(join(shipTodoDir, file), 'utf8')
	      const intendedLogicalHash = `sha256:${createHash('sha256').update(logicalTaskText(text)).digest('hex')}`
	      writeFileSync(join(finalizationStateDir, `${taskStem}.json`), JSON.stringify({
	        version: 1, stem: taskStem, transactionId, status: 'running', phase: 'ship',
	        owner: { pid: process.pid, hostname: hostname() },
	        source: { intendedHash: `sha256:${createHash('sha256').update(text).digest('hex')}`, intendedLogicalHash },
	        figma: { enabled: true, configHash: `sha256:${createHash('sha256').update(readFileSync(join(tree, 'orchestrator', 'project-config.md'))).digest('hex')}` },
	        phases: { ship: { state: 'running' } },
	      }) + '\n')
	      writeIntegrationRecord(taskStem)
	    }
	    return {
	      ...process.env,
	      FIGMA_SCREEN_TASK_FILE: undefined,   // the tree's todo/done files ARE the task bodies
	      FIGMA_CACHE_ROOT: cacheRoot,
	      FIGMA_REPORTS_DIR: shipReports,
	      FIGMA_SCREEN_CACHE_ROOT: shipScreensRoot,
	      FIGMA_SHIP_RECEIPTS_DIR: receiptsRoot,
	      FIGMA_PIPELINE_RUN_ID: runId,
	      FINALIZE_STATE_DIR: finalizationStateDir,
	      FINALIZE_INTEGRATIONS_DIR: integrationsDir,
	      FINALIZE_TRANSACTION_ID: transactionId,
	    }
	  }
	  const seedShipRun = (runId) => {
	    mkdirSync(shipReports, { recursive: true })
	    for (const name of ['screen-cache', 'check-spec', 'capture-config', 'census', 'spec', 'spec-compare', 'screenshot']) writeReport(shipReports, name, SHIP_STEM, runId)
	    writeReport(shipReports, 'screenshot', SHIP_STEM, runId, {
	      results: [{ screen: 'HomeScreen', themeKey: 'primary', status: 'PASS', score: 1 }],
	    })
	    writeReport(shipReports, 'spec-compare', SHIP_STEM, runId, { comparisons: [
	      { screen: 'HomeScreen', theme: 'light', file: 'HomeScreen.spec.json', stableId: 'title', elementName: 'Title', status: 'PASS' },
	    ] })
	    writeReport(shipReports, 'screen-cache', SHIP_STEM, runId, { inputs: { designSourceHash: parseDesignSources([shipBody]).sourceHash } })
	    writeFileSync(join(shipTodoDir, `${SHIP_STEM}.md`), shipBody)
	  }
	  const runShip = (runId, receiptsRoot) => spawnSync('node', [join(SHIP_SCRIPTS, 'ship-done.mjs'), SHIP_STEM], { env: shipEnv(runId, receiptsRoot), encoding: 'utf8' })
	  const runVerify = () => spawnSync('node', [join(SHIP_SCRIPTS, 'verify-done.mjs')], { env: shipEnv('verify-run'), encoding: 'utf8' })
	  const findDigestLine = (md) => inspectOutcomeFigmaMeta(md).executionLines[0]?.line

	  // First ship under run-0 — only to obtain a REAL digest from a previous run of the same
	  // task (the replay artifact for the forgery pin below).
	  seedShipRun('ship-run-0')
	  const deniedEnv = shipEnv('direct-denied')
	  delete deniedEnv.FINALIZE_TRANSACTION_ID
	  const directDenied = spawnSync('node', [join(SHIP_SCRIPTS, 'ship-done.mjs'), SHIP_STEM], { env: deniedEnv, encoding: 'utf8' })
	  check('ship-done rejects direct invocation outside an active finalization transaction', () => {
	    assert.equal(directDenied.status, 1, directDenied.stdout + directDenied.stderr)
	    assert.match(directDenied.stderr, /FINALIZE_TRANSACTION_ID|finalize-task/)
	    assert.ok(existsSync(join(shipTodoDir, `${SHIP_STEM}.md`)), 'unauthorized task stays in todo/')
	  })
	  const ship0 = runShip('ship-run-0')
	  const replayDigest = ship0.status === 0 ? findDigestLine(readFileSync(shipDoneFile, 'utf8')) : null
	  check('ship-done harness: fixture UI task ships clean (run-0)', () => {
	    assert.equal(ship0.status, 0, ship0.stdout + ship0.stderr)
	    assert.ok(replayDigest, 'digest injected into done file')
	    const shipped = readFileSync(shipDoneFile, 'utf8')
	    assert.match(shipped, /Figma meta: documentation example in the task body/, 'body metadata-like prose must be preserved')
	    const inspected = inspectOutcomeFigmaMeta(shipped)
	    assert.equal(inspected.executionHeaders.length, 1)
	    assert.equal(inspected.executionLines.length, 1, 'authoritative digest must land in the final Outcome Execution log')
	    assert.notEqual(
	      logicalTaskText(shipBody),
	      logicalTaskText(shipBody.replace('- Figma meta: documentation example in the task body\n', '')),
	      'body metadata-like prose must remain part of the logical task identity'
	    )
	  })
	  check('Outcome digest injection rejects non-canonical line endings without rewriting task bytes', () => {
	    const original = '# CRLF body\r\n\r\n---\r\n\r\n## Outcome\r\n\r\n### Execution log\r\n\r\n- kept\r\n\r\n\r\n'
	    assert.throws(
	      () => injectOutcomeFigmaMeta(original, replayDigest),
	      /canonical UTF-8 without BOM and LF line endings/,
	      'ship-time metadata injection must not normalize an old line-ending representation'
	    )
	    assert.equal(original, '# CRLF body\r\n\r\n---\r\n\r\n## Outcome\r\n\r\n### Execution log\r\n\r\n- kept\r\n\r\n\r\n')
	    const noFinalNewline = '# Body\n\n---\n\n## Outcome\n\n### Execution log\n\n- unterminated content'
	    assert.equal(logicalTaskText(injectOutcomeFigmaMeta(noFinalNewline, replayDigest)), noFinalNewline)
	    assert.throws(
	      () => injectOutcomeFigmaMeta('# Body\n\n---\n\n## Outcome\n', replayDigest),
	      /must contain `### Execution log`/,
	      'missing structural section is a fixable precondition, never a hidden ship-time rewrite'
	    )
	  })
	  check('WARN caveat evidence is read only from the final Outcome Caveats section', () => {
	    const bodyCaveat = '# Body\n\n### Caveats\n\n- convincing but non-authoritative body caveat\n\n---\n\n## Outcome\n\n### Caveats\n\n- none\n\n### Follow-ups\n\n- none\n'
	    assert.equal(caveatsHaveContent(bodyCaveat), false)
	    assert.equal(caveatsHaveContent(bodyCaveat.replace('- none\n\n### Follow-ups', '- reviewed visual delta\n\n### Follow-ups')), true)
	  })

	  // (a) A fresh exact-once ship writes the certification and token receipts byte-verbatim.
	  // The prior synthetic ship is retained only as a replay-forgery input; clear its committed
	  // receipt identity before starting a distinct finalization transaction for the same stem.
	  rmSync(shipDoneFile, { force: true })
	  rmSync(shipReceiptsDir, { recursive: true, force: true })
	  seedShipRun('ship-run-1')
	  const ship1 = runShip('ship-run-1')
	  check('ship-done writes the current ship receipt set byte-verbatim', () => {
	    assert.equal(ship1.status, 0, ship1.stdout + ship1.stderr)
	    const digestLine = readFileSync(join(shipReceiptsDir, `figma-meta-${SHIP_STEM}.txt`), 'utf8').trim()
	    assert.match(digestLine, /pipelineRunId=ship-run-1/)
	    // Byte-verbatim is load-bearing: the receipts must still hash to the digest's fields.
	    const fields = Object.fromEntries(digestLine.replace(/^-\s*Figma meta:\s*/, '').split(';').map((s) => s.trim().split('=')))
	    assert.equal(fileHash(join(shipReceiptsDir, `evidence-${SHIP_STEM}.json`)), fields.evidenceReportHash)
	    assert.equal(fileHash(join(shipReceiptsDir, `screenshot-${SHIP_STEM}.json`)), fields.screenshotReportHash)
	    assert.ok(readFileSync(shipDoneFile, 'utf8').includes(digestLine), 'in-task digest == receipt digest')
	    // The census report rides the receipts too (byte-verbatim), so the site's
	    // done-view survives live-cache eviction without flipping the card.
	    assert.equal(
	      fileHash(join(shipReceiptsDir, `census-${SHIP_STEM}.json`)),
	      fileHash(join(shipReports, `census-${SHIP_STEM}.json`)),
	      'census receipt must be a byte-verbatim copy of the sealed census report'
	    )
	  })

	  // (a, proof) THE 3b-closure pin: delete the ephemeral reports cache entirely (the fresh
	  // CI clone simulation — before receipts there was NOTHING to re-bind against here) and
	  // verify-done must still re-bind the shipped digest via the committed receipts.
	  rmSync(shipReports, { recursive: true, force: true })
	  const ciVerify = runVerify()
	  check('3b closure: verify-done re-binds via committed receipts with the reports cache DELETED (CI clone)', () => {
	    assert.equal(ciVerify.status, 0, ciVerify.stdout + ciVerify.stderr)
	    assert.match(ciVerify.stdout, /audited 1 UI task\(s\) \+ 0 non-UI appendix shape\(s\), 0 violation\(s\)/)
	  })

	  const certifiedMd = readFileSync(shipDoneFile, 'utf8')
	  const certifiedDigest = findDigestLine(certifiedMd)
	  const withoutAuthoritativeDigest = certifiedMd.split(/\r?\n/).filter((line) => line.trim() !== certifiedDigest.trim()).join('\n')
	  writeFileSync(shipDoneFile, withoutAuthoritativeDigest.replace(
	    '- Figma meta: documentation example in the task body',
	    `- Figma meta: documentation example in the task body\n${certifiedDigest}`
	  ))
	  const bodyDigestVerify = runVerify()
	  check('a digest moved into the task body cannot certify a missing Outcome Execution-log digest', () => {
	    assert.equal(bodyDigestVerify.status, 2, bodyDigestVerify.stdout + bodyDigestVerify.stderr)
	    assert.match(bodyDigestVerify.stdout + bodyDigestVerify.stderr, /Execution log must contain exactly one.*found 0/)
	  })
	  writeFileSync(shipDoneFile, certifiedMd)

	  // (b) Replay forgery: swap the in-task digest for the REAL, structurally-valid green
	  // digest from the previous run of this same task (design unchanged, code changed — the
	  // exact 3b scenario). With receipts present from the real ship, the run-id mismatch IS a
	  // violation: the receipt is written by the same ship that injects the digest.
	  const shippedMd = readFileSync(shipDoneFile, 'utf8')
	  writeFileSync(shipDoneFile, injectOutcomeFigmaMeta(shippedMd, replayDigest))
	  const replayVerify = runVerify()
	  check('a green digest replayed from a previous run of the same task violates when receipts exist', () => {
	    assert.equal(replayVerify.status, 2, replayVerify.stdout + replayVerify.stderr)
	    assert.match(replayVerify.stdout + replayVerify.stderr, /replaced after the ship/)
	  })

	  // (c) Receipts are ALWAYS required (single current scenario, no reports-cache fallback).
	  // Deleting the committed receipts dir (the reports
	  // cache is already gone — CI clone) makes the shipped digest unverifiable → violation: it was
	  // replayed from another run or its receipts were deleted after ship. A done UI task with a
	  // valid digest MUST carry its committed receipts.
	  rmSync(shipReceiptsDir, { recursive: true, force: true })
	  const noReceipts = runVerify()
	  check('receipts-required: a shipped digest with its committed receipts deleted → violation (exit 2)', () => {
	    assert.equal(noReceipts.status, 2, noReceipts.stdout + noReceipts.stderr)
	    assert.match(noReceipts.stdout + noReceipts.stderr, /no committed ship receipts/)
	  })

	  // (d) Receipt write failure fails the ship LOUDLY and does NOT move the task — an
	  // unreceipted ship would silently reopen the replay gap.
	  rmSync(shipDoneFile, { force: true })
	  seedShipRun('ship-run-2')
	  const receiptsBlocker = join(shipWs, 'receipts-blocker')
	  writeFileSync(receiptsBlocker, 'not a dir\n')
	  const shipNoReceipts = runShip('ship-run-2', receiptsBlocker)
	  check('ship-done fails (exit 1) and does not move the task when receipts cannot be written', () => {
	    assert.equal(shipNoReceipts.status, 1, shipNoReceipts.stdout + shipNoReceipts.stderr)
	    assert.match(shipNoReceipts.stderr, /failed to write ship receipts/)
	    assert.ok(existsSync(join(shipTodoDir, `${SHIP_STEM}.md`)), 'task stays in todo/')
	    assert.ok(!existsSync(shipDoneFile), 'task did not reach done/')
	  })

	  rmSync(join(finalizationStateDir, `.finalize-${SHIP_STEM}-fin-fixture-ship-run-2.receipts`), { recursive: true, force: true })
	  seedShipRun('ship-stage-symlink')
	  const foreignStageTarget = join(shipWs, 'foreign-stage-target')
	  mkdirSync(foreignStageTarget, { recursive: true })
	  const foreignStageSentinel = join(foreignStageTarget, 'sentinel.txt')
	  writeFileSync(foreignStageSentinel, 'must not be touched\n')
	  const unsafeStage = join(finalizationStateDir, `.finalize-${SHIP_STEM}-fin-fixture-ship-stage-symlink.receipts`)
	  symlinkSync(foreignStageTarget, unsafeStage, 'dir')
	  const symlinkStageShip = runShip('ship-stage-symlink')
	  check('ship-done refuses a symlinked transaction receipt stage without writing through it', () => {
	    assert.equal(symlinkStageShip.status, 1, symlinkStageShip.stdout + symlinkStageShip.stderr)
	    assert.match(symlinkStageShip.stderr, /transaction receipt stage must be a real directory/)
	    assert.equal(readFileSync(foreignStageSentinel, 'utf8'), 'must not be touched\n')
	    assert.equal(readdirSync(foreignStageTarget).sort().join(','), 'sentinel.txt')
	    assert.ok(existsSync(join(shipTodoDir, `${SHIP_STEM}.md`)))
	    assert.ok(!existsSync(shipDoneFile))
	  })
		  unlinkSync(unsafeStage)

		  seedShipRun('ship-receipt-ancestor-symlink')
		  const foreignReceiptTarget = join(shipWs, 'foreign-receipt-target')
		  mkdirSync(foreignReceiptTarget, { recursive: true })
		  const foreignReceiptSentinel = join(foreignReceiptTarget, 'sentinel.txt')
		  writeFileSync(foreignReceiptSentinel, 'must not be touched\n')
		  const receiptAncestorLink = join(shipWs, 'receipt-ancestor-link')
		  symlinkSync(foreignReceiptTarget, receiptAncestorLink, 'dir')
		  const ancestorSymlinkShip = runShip('ship-receipt-ancestor-symlink', join(receiptAncestorLink, 'figma-ship'))
		  check('ship-done refuses a symlinked ship-receipt ancestor without external temp or final writes', () => {
		    assert.equal(ancestorSymlinkShip.status, 1, ancestorSymlinkShip.stdout + ancestorSymlinkShip.stderr)
		    assert.match(ancestorSymlinkShip.stderr, /ship receipts destination must be a real directory tree/)
		    assert.equal(readFileSync(foreignReceiptSentinel, 'utf8'), 'must not be touched\n')
		    assert.equal(readdirSync(foreignReceiptTarget).sort().join(','), 'sentinel.txt')
		    assert.ok(existsSync(join(shipTodoDir, `${SHIP_STEM}.md`)))
		    assert.ok(!existsSync(shipDoneFile))
		  })
		  unlinkSync(receiptAncestorLink)

		  // ── No-fallback outcome-shape contract — NEGATIVE pins ─────────────────────────────────
	  // outcomeShapeError() runtime-reads contracts/outcome-shape.json BEFORE the move with no
	  // fallback; both fail() branches (unreadable / malformed) must exit 1 loudly and leave the
	  // task in todo/. A later "simplification" that adds a silent default stays green nowhere
	  // else — these two pins are the negative half of the ruling. Contract restored after.
	  // Before the contract pins below, publication CAS regressions: the long Figma gate must never overwrite an
	  // edit that lands after its initial todo read, and a no-clobber EEXIST loser
	  // must never touch the winner's canonical receipts.
	  rmSync(join(shipTodoDir, `.finalize-${SHIP_STEM}-fin-fixture-ship-run-2.ship`), { force: true })
	  rmSync(join(finalizationStateDir, `.finalize-${SHIP_STEM}-fin-fixture-ship-run-2.receipts`), { recursive: true, force: true })

	  rmSync(shipDoneFile, { force: true })
	  seedShipRun('ship-race-edit')
	  const editRaceEnv = shipEnv('ship-race-edit')
	  const prepared = spawnSync('node', [join(SHIP_SCRIPTS, 'ship-done.mjs'), SHIP_STEM], {
	    env: { ...editRaceEnv, SHIP_DONE_FAILPOINT: 'after-candidate' }, encoding: 'utf8',
	  })
	  const orphanPublicationTmp = join(shipTodoDir, `.finalize-${SHIP_STEM}-fin-fixture-ship-race-edit.ship.tmp.crash-window`)
	  writeFileSync(orphanPublicationTmp, 'partial transaction publication bytes\n')
	  const externalEdit = shipBody + '\nexternal edit that landed after the Figma gate\n'
	  writeFileSync(join(shipTodoDir, `${SHIP_STEM}.md`), externalEdit)
	  const editRaceResume = spawnSync('node', [join(SHIP_SCRIPTS, 'ship-done.mjs'), SHIP_STEM], { env: editRaceEnv, encoding: 'utf8' })
	  check('ship-done preserves a todo edit that lands after the long gate instead of overwriting it', () => {
	    assert.equal(prepared.status, 97, prepared.stdout + prepared.stderr)
	    assert.equal(editRaceResume.status, 1, editRaceResume.stdout + editRaceResume.stderr)
	    assert.match(editRaceResume.stderr, /todo task changed/)
	    assert.equal(readFileSync(join(shipTodoDir, `${SHIP_STEM}.md`), 'utf8'), externalEdit)
	    assert.ok(!existsSync(shipDoneFile), 'conflicting edit must not be published')
	    assert.ok(!existsSync(orphanPublicationTmp), 'transaction-owned candidate temp aliases are cleaned on recovery')
	  })
	  rmSync(join(shipTodoDir, `.finalize-${SHIP_STEM}-fin-fixture-ship-race-edit.ship`), { force: true })
	  rmSync(join(finalizationStateDir, `.finalize-${SHIP_STEM}-fin-fixture-ship-race-edit.receipts`), { recursive: true, force: true })

	  rmSync(shipDoneFile, { force: true })
	  seedShipRun('ship-race-done')
	  const raceReceiptsRoot = join(shipWs, 'race-receipts')
	  const raceReceiptsDir = join(raceReceiptsRoot, SHIP_STEM)
	  mkdirSync(raceReceiptsDir, { recursive: true })
	  const sentinelReceipt = join(raceReceiptsDir, `evidence-${SHIP_STEM}.json`)
	  writeFileSync(sentinelReceipt, 'winner receipt must survive\n')
	  const doneRaceEnv = shipEnv('ship-race-done', raceReceiptsRoot)
	  const donePrepared = spawnSync('node', [join(SHIP_SCRIPTS, 'ship-done.mjs'), SHIP_STEM], {
	    env: { ...doneRaceEnv, SHIP_DONE_FAILPOINT: 'after-candidate' }, encoding: 'utf8',
	  })
	  writeFileSync(shipDoneFile, '# concurrently published foreign done task\n')
	  const doneRaceResume = spawnSync('node', [join(SHIP_SCRIPTS, 'ship-done.mjs'), SHIP_STEM], { env: doneRaceEnv, encoding: 'utf8' })
	  check('an EEXIST publication loser cannot overwrite canonical receipts', () => {
	    assert.equal(donePrepared.status, 97, donePrepared.stdout + donePrepared.stderr)
	    assert.equal(doneRaceResume.status, 1, doneRaceResume.stdout + doneRaceResume.stderr)
	    assert.match(doneRaceResume.stderr, /not the transaction-owned publication|different identities/)
	    assert.equal(readFileSync(sentinelReceipt, 'utf8'), 'winner receipt must survive\n')
	    assert.equal(readFileSync(shipDoneFile, 'utf8'), '# concurrently published foreign done task\n')
	  })
	  rmSync(join(shipTodoDir, `.finalize-${SHIP_STEM}-fin-fixture-ship-race-done.ship`), { force: true })
	  rmSync(join(finalizationStateDir, `.finalize-${SHIP_STEM}-fin-fixture-ship-race-done.receipts`), { recursive: true, force: true })
	  rmSync(shipDoneFile, { force: true })
	  rmSync(join(shipTodoDir, `${SHIP_STEM}.md`), { force: true })

	  const NF_STEM = 'TASK_11_contract_negative'
	  const nfContract = join(tree, 'orchestrator', 'contracts', 'outcome-shape.json')
	  const nfContractBytes = readFileSync(nfContract)
	  writeFileSync(join(shipTodoDir, `${NF_STEM}.md`), '# NF fixture\n\n---\n\n## Outcome\n\n**Status**: completed\n')
	  rmSync(nfContract)
	  const nfMissing = spawnSync('node', [join(SHIP_SCRIPTS, 'ship-done.mjs'), NF_STEM], { env: shipEnv('nf-missing'), encoding: 'utf8' })
	  check('no-fallback pin: ship-done exits 1 naming the contract when outcome-shape.json is MISSING', () => {
	    assert.equal(nfMissing.status, 1, nfMissing.stdout + nfMissing.stderr)
	    assert.match(nfMissing.stderr, /required outcome-shape contract is unreadable or malformed/)
	    assert.ok(existsSync(join(shipTodoDir, `${NF_STEM}.md`)), 'task stays in todo/')
	  })
	  writeFileSync(nfContract, '{"statusValid": "completed", "reviewerValid": ["codex"], "headings": ["Build gates"]}')
	  const nfCorrupt = spawnSync('node', [join(SHIP_SCRIPTS, 'ship-done.mjs'), NF_STEM], { env: shipEnv('nf-corrupt'), encoding: 'utf8' })
	  check('no-fallback pin: ship-done exits 1 on a CORRUPT contract (string-valued enum), task stays put', () => {
	    assert.equal(nfCorrupt.status, 1, nfCorrupt.stdout + nfCorrupt.stderr)
	    assert.match(nfCorrupt.stderr, /required outcome-shape contract has an invalid schema/)
	    assert.ok(existsSync(join(shipTodoDir, `${NF_STEM}.md`)), 'task stays in todo/')
	  })
	  writeFileSync(nfContract, nfContractBytes)   // restore for the pins below
	  rmSync(join(shipTodoDir, `${NF_STEM}.md`), { force: true })

	  // ── Acceptance-trace verdict enum pins ───────────────────────────────────────────────────
	  // outcomeShapeError's per-bullet verdict check (backtick-aware split + enum) had no suite pin:
	  // every other ship fixture uses `- none`, so a regression (or a botched product port of the
	  // contract's required acceptanceVerdicts key) shipped silently. Non-UI body → the shape gate
	  // is the only gate in play.
	  const G6_STEM = 'TASK_12_gate6_verdicts'
	  const g6Body = (traceBullet) => '# Gate6 fixture\n\n## Inputs\n\nnothing UI here\n\n---\n\n## Outcome\n\n**Status**: completed\n**Reviewer**: internal-reviewer\n\n### Build gates\n\n- none\n\n### Runtime verify\n\n- Gate: skipped (fixture has no runtime change)\n- Result: n/a — fixture has no runtime change\n\n### Acceptance trace\n\n- ' + traceBullet + '\n\n### Caveats\n\n- none\n\n### Follow-ups\n\n- none\n\n### Files touched\n\n- none\n\n### Execution log\n\n- x\n'
	  writeFileSync(join(shipTodoDir, `${G6_STEM}.md`), g6Body('`Board shows pill` — garbage — n'))
	  const g6Bad = spawnSync('node', [join(SHIP_SCRIPTS, 'ship-done.mjs'), G6_STEM], { env: shipEnv('g6-bad'), encoding: 'utf8' })
	  check('Gate 6 pin: a garbage acceptance-trace verdict blocks the ship BEFORE the move (exit 1)', () => {
	    assert.equal(g6Bad.status, 1, g6Bad.stdout + g6Bad.stderr)
	    assert.match(g6Bad.stderr, /invalid acceptance-trace verdict "garbage"/)
	    assert.ok(existsSync(join(shipTodoDir, `${G6_STEM}.md`)), 'task stays in todo/')
	    assert.ok(!existsSync(join(shipDoneDir, `${G6_STEM}.md`)), 'task did not reach done/')
	  })
	  writeFileSync(join(shipTodoDir, `${G6_STEM}.md`), g6Body('`Board shows pill — red when expired` — verified — matched'))
	  const g6Ok = spawnSync('node', [join(SHIP_SCRIPTS, 'ship-done.mjs'), G6_STEM], { env: shipEnv('g6-ok'), encoding: 'utf8' })
	  check('Gate 6 pin: a backtick-quoted bullet with an em-dash inside + valid verdict ships clean', () => {
	    assert.equal(g6Ok.status, 0, g6Ok.stdout + g6Ok.stderr)
	    assert.ok(existsSync(join(shipDoneDir, `${G6_STEM}.md`)), 'task moved to done/')
	  })
	  rmSync(join(shipDoneDir, `${G6_STEM}.md`), { force: true })   // keep done/ empty for the pins below

	  const NC_STEM = 'TASK_13_no_clobber_recovery'
	  writeFileSync(join(shipTodoDir, `${NC_STEM}.md`), g6Body('`No-clobber publication` — verified — fixture'))
	  const ncEnv = shipEnv('no-clobber-crash')
	  const ncCrash = spawnSync('node', [join(SHIP_SCRIPTS, 'ship-done.mjs'), NC_STEM], {
	    env: { ...ncEnv, SHIP_DONE_FAILPOINT: 'after-link' }, encoding: 'utf8',
	  })
	  check('no-clobber move leaves a recoverable transaction-owned todo+done state after a crash', () => {
	    assert.equal(ncCrash.status, 97, ncCrash.stdout + ncCrash.stderr)
	    assert.ok(existsSync(join(shipTodoDir, `${NC_STEM}.md`)))
	    assert.ok(existsSync(join(shipDoneDir, `${NC_STEM}.md`)))
	  })
	  const ncResume = spawnSync('node', [join(SHIP_SCRIPTS, 'ship-done.mjs'), NC_STEM], { env: ncEnv, encoding: 'utf8' })
	  check('no-clobber retry reconciles only the matching transaction-owned publication', () => {
	    assert.equal(ncResume.status, 0, ncResume.stdout + ncResume.stderr)
	    assert.ok(!existsSync(join(shipTodoDir, `${NC_STEM}.md`)))
	    assert.ok(existsSync(join(shipDoneDir, `${NC_STEM}.md`)))
	    assert.match(ncResume.stdout, /recovered interrupted no-clobber move/)
	  })
	  rmSync(join(shipDoneDir, `${NC_STEM}.md`), { force: true })

	  const ND_STEM = 'TASK_14_detach_conflict'
	  const ndBody = g6Body('`Detachment proof survives` — verified — fixture')
	  writeFileSync(join(shipTodoDir, `${ND_STEM}.md`), ndBody)
	  const ndEnv = shipEnv('detach-conflict')
	  const ndCrash = spawnSync('node', [join(SHIP_SCRIPTS, 'ship-done.mjs'), ND_STEM], {
	    env: { ...ndEnv, SHIP_DONE_FAILPOINT: 'after-detach' }, encoding: 'utf8',
	  })
	  const ndDone = join(shipDoneDir, `${ND_STEM}.md`)
	  const ndTodo = join(shipTodoDir, `${ND_STEM}.md`)
	  const ndDetach = join(shipTodoDir, `.finalize-${ND_STEM}-fin-fixture-detach-conflict.detach.md`)
	  rmSync(ndDone, { force: true })
	  writeFileSync(ndDone, '# foreign done replacement\n')
	  const ndResume = spawnSync('node', [join(SHIP_SCRIPTS, 'ship-done.mjs'), ND_STEM], { env: ndEnv, encoding: 'utf8' })
	  check('a replaced done file cannot consume the last intended todo detachment proof', () => {
	    assert.equal(ndCrash.status, 97, ndCrash.stdout + ndCrash.stderr)
	    assert.equal(ndResume.status, 1, ndResume.stdout + ndResume.stderr)
	    assert.match(ndResume.stderr, /published done task no longer belongs/)
	    assert.equal(readFileSync(ndDone, 'utf8'), '# foreign done replacement\n')
	    assert.equal(readFileSync(ndTodo, 'utf8'), ndBody, 'intended todo bytes must be restored without clobbering')
	    assert.ok(existsSync(ndDetach), 'private detachment proof remains for recovery')
	  })
	  rmSync(ndDone, { force: true })
	  rmSync(ndTodo, { force: true })
	  rmSync(ndDetach, { force: true })
	  rmSync(join(shipTodoDir, `.finalize-${ND_STEM}-fin-fixture-detach-conflict.ship`), { force: true })

	  // ── UI-by-evidence backstop — end-to-end pins ───────────────────────────────────────────
	  // The design-parser unit coverage exercises the pure uiTaskWithoutDesign();
	  // these prove the WIRING actually blocks in ship-done + verify-done (a mutation removing the
	  // call site must fail SOMETHING). UI-by-code body: designComponentId snapshot + a screen
	  // file in Files touched, NO pullable `## Design`; valid Outcome shape so it reaches the
	  // backstop (which runs after the shape gate). done/ is empty here (SHIP_STEM left absent by
	  // (d)), so verify-done audits only our gap fixture.
	  const GAP_STEM = 'TASK_10_ui_by_code_gap'
	  const gapBody = '# UI-by-code gap fixture\n\n## Inputs\n\ncomponent: Widget\ndesignComponentId: figma-component:aaaaaaaaaaaaaaaa:none:5:6\nfigmaNodeId: 5:6\n\n---\n\n## Outcome\n\n**Status**: completed\n**Reviewer**: internal-reviewer\n\n### Build gates\n\n- none\n\n### Runtime verify\n\n- Gate: skipped (fixture has no runtime change)\n- Result: n/a — fixture has no runtime change\n\n### Acceptance trace\n\n- none\n\n### Caveats\n\n- none\n\n### Follow-ups\n\n- none\n\n### Files touched\n\n- `ui/WidgetScreen.kt` — created\n\n### Execution log\n\n- x\n'
	  writeFileSync(join(shipTodoDir, `${GAP_STEM}.md`), gapBody)
	  const gapShip = spawnSync('node', [join(SHIP_SCRIPTS, 'ship-done.mjs'), GAP_STEM], { env: shipEnv('gap-ship'), encoding: 'utf8' })
	  check('ship-done blocks a UI-by-code task with no ## Design and does not move it', () => {
	    assert.equal(gapShip.status, 2, gapShip.stdout + gapShip.stderr)
	    assert.match(gapShip.stderr, /pixel-compared|component snapshot|## Design/)
	    assert.match(gapShip.stderr, /BLOCKED\[task-shape\]/, 'design-edit block must tag task-shape, not figma-screens (board would show a useless Pull button)')
	    // The block message carries a ready-to-paste `## Design` bullet derived from the touched file.
	    assert.match(gapShip.stderr, /Paste this into the task/, 'block message offers a one-paste fix')
	    assert.match(gapShip.stderr, /- Widget \[screen\] — <figma node url>/, 'paste bullet derived from ui/WidgetScreen.kt')
	    assert.ok(existsSync(join(shipTodoDir, `${GAP_STEM}.md`)), 'gap task stays in todo/')
	    assert.ok(!existsSync(join(shipDoneDir, `${GAP_STEM}.md`)), 'gap task did NOT reach done/')
	  })
	  const VGAP_STEM = 'TASK_11_ui_by_code_done_gap'
	  writeFileSync(join(shipDoneDir, `${VGAP_STEM}.md`), gapBody)   // same UI-by-code body, already in done/
	  const gapVerify = spawnSync('node', [join(SHIP_SCRIPTS, 'verify-done.mjs')], { env: shipEnv('gap-verify'), encoding: 'utf8' })
	  check('verify-done flags a UI-by-code done task with no ## Design', () => {
	    assert.equal(gapVerify.status, 2, gapVerify.stdout + gapVerify.stderr)
	    assert.match(gapVerify.stderr, new RegExp(VGAP_STEM))
	  })
	  // The weak filename-only signal (a `*Screen.kt` edit, no node URL / snapshot cited) is
	  // advisory — it must NOT hard-block a non-visual screen-file edit. ship-done SHIPS (exit 0)
	  // and moves the task, emitting a NOTE. (Contrast the snapshot-tier GAP task above, which blocks.)
	  const FN_STEM = 'TASK_12_filename_only_advisory'
	  const fnBody = '# filename-only advisory fixture\n\n## Goal\n\nrename a callback in the home screen (non-visual)\n\n---\n\n## Outcome\n\n**Status**: completed\n**Reviewer**: internal-reviewer\n\n### Build gates\n\n- none\n\n### Runtime verify\n\n- Gate: skipped (fixture has no runtime change)\n- Result: n/a — fixture has no runtime change\n\n### Acceptance trace\n\n- none\n\n### Caveats\n\n- none\n\n### Follow-ups\n\n- none\n\n### Files touched\n\n- `ui/HomeScreen.kt` — modified\n\n### Execution log\n\n- x\n'
	  writeFileSync(join(shipTodoDir, `${FN_STEM}.md`), fnBody)
	  const fnShip = spawnSync('node', [join(SHIP_SCRIPTS, 'ship-done.mjs'), FN_STEM], { env: shipEnv('fn-ship'), encoding: 'utf8' })
	  check('filename-only UI task ships with an advisory note, not a hard block', () => {
	    assert.equal(fnShip.status, 0, fnShip.stdout + fnShip.stderr)
	    assert.match(fnShip.stderr, /NOTE .*filename-only/, 'ship-done should emit an advisory NOTE for the weak filename tier')
	    assert.ok(!existsSync(join(shipTodoDir, `${FN_STEM}.md`)), 'filename-only task must leave todo/')
	    assert.ok(existsSync(join(shipDoneDir, `${FN_STEM}.md`)), 'filename-only task must reach done/')
	  })
	  // The erosion detector allows audited `— none (<why>)` opt-outs but catches a product that could
	  // hollow out the comparison guarantee one opt-out at a time. verify-done sums them across done/
	  // and WARNs when the per-task / corpus thresholds are crossed — advisory only, NEVER a violation
	  // (exit 0). Isolated tree (verify-done resolves done/ + config from its own PROJECT_ROOT).
	  const erosionTree = join(ws, 'erosion', 'tree')
	  const EROSION_SCRIPTS = join(erosionTree, 'orchestrator', 'figma', 'scripts')
	  cpSync(join(HERE, '..', 'scripts'), EROSION_SCRIPTS, { recursive: true })
	  copyDesignReaderRuntime(erosionTree)
	  writeFileSync(join(erosionTree, 'orchestrator', 'project-config.md'), 'figmaEnabled: true\n')
	  mkdirSync(join(erosionTree, 'orchestrator', 'contracts'), { recursive: true })
	  cpSync(join(HERE, '..', '..', 'contracts', 'outcome-shape.json'), join(erosionTree, 'orchestrator', 'contracts', 'outcome-shape.json'))
	  const erosionDoneDir = join(erosionTree, 'orchestrator', 'tasks', 'done')
	  mkdirSync(erosionDoneDir, { recursive: true })
	  const noneBody = (bullets) => `# opt-out fixture\n\n## Design\n\n${bullets}\n\n---\n\n## Outcome\n\n**Status**: completed\n**Reviewer**: internal-reviewer\n\n### Build gates\n\n- none\n\n### Runtime verify\n\n- Gate: skipped (fixture has no runtime change)\n- Result: n/a — fixture has no runtime change\n\n### Acceptance trace\n\n- none\n\n### Caveats\n\n- none\n\n### Follow-ups\n\n- none\n\n### Files touched\n\n- none\n\n### Execution log\n\n- x\n`
	  for (let i = 0; i < 4; i++) writeFileSync(join(erosionDoneDir, `TASK_EROSION_${i}.md`), noneBody(`- Screen${i} — none (no mock exists)`))
	  writeFileSync(join(erosionDoneDir, 'TASK_EROSION_heavy.md'), noneBody('- ScreenA — none (no mock)\n- ScreenB — none (no mock)'))   // 2 opt-outs in one task
	  const erosionVerify = spawnSync('node', [join(EROSION_SCRIPTS, 'verify-done.mjs')], { env: { ...process.env, FIGMA_CACHE_ROOT: cacheRoot, FIGMA_NONE_CORPUS: '3', FIGMA_NONE_PER_TASK: '2' }, encoding: 'utf8' })
	  check('audited-none opt-outs across done/ -> erosion warning (advisory, never blocks)', () => {
	    assert.equal(erosionVerify.status, 0, erosionVerify.stdout + erosionVerify.stderr)
	    assert.match(erosionVerify.stderr, /6 audited .* opt-out\(s\) across done\//, 'corpus opt-out count (4 single + 1 double)')
	    assert.match(erosionVerify.stderr, /TASK_EROSION_heavy\.md: 2 audited .* in one task/, 'per-task heavy opt-out warning')
	    assert.match(erosionVerify.stderr, /guarantee is eroding/, 'corpus 6 >= FIGMA_NONE_CORPUS=3 fires the erosion warning')
	  })

	  // ── Strict done/ audit in an isolated project tree ───────────────────────────────────────
	  const rTree = join(ws, 'strict-done-audit', 'tree')
	  const R_SCRIPTS = join(rTree, 'orchestrator', 'figma', 'scripts')
	  cpSync(join(HERE, '..', 'scripts'), R_SCRIPTS, { recursive: true })
	  copyDesignReaderRuntime(rTree)
	  writeFileSync(join(rTree, 'orchestrator', 'project-config.md'), 'figmaEnabled: true\n')
	  // the frozen-shape contract must exist in the isolated tree (outcomeShapeError loads it
	  // from PROJECT_ROOT; the non-UI shape pins below exercise that path)
	  mkdirSync(join(rTree, 'orchestrator', 'contracts'), { recursive: true })
	  cpSync(join(HERE, '..', '..', 'contracts', 'outcome-shape.json'), join(rTree, 'orchestrator', 'contracts', 'outcome-shape.json'))
	  const rDoneDir = join(rTree, 'orchestrator', 'tasks', 'done')
	  mkdirSync(rDoneDir, { recursive: true })
	  // A block-tier violator: cites a Figma node URL, declares no `## Design` bullet.
	  const badBody = (n) => `# invalid UI fixture ${n}\n\n## Inputs\n- node: https://www.figma.com/design/AAAABBBBCCCCDDDD/X?node-id=1-${n}\n\n---\n\n## Outcome\n\n**Status**: completed\n**Reviewer**: internal-reviewer\n`
	  writeFileSync(join(rDoneDir, 'TASK_R1_invalid_ui.md'), badBody(1))
	  const rEnv = { ...process.env, FIGMA_CACHE_ROOT: cacheRoot }
	  const runR = (...args) => spawnSync('node', [join(R_SCRIPTS, 'verify-done.mjs'), ...args], { env: rEnv, encoding: 'utf8' })
	  check('strict done audit blocks a UI-by-evidence task without a Design section', () => {
	    const r = runR()
	    assert.notEqual(r.status, 0, r.stdout + r.stderr)
	    assert.match(r.stdout + r.stderr, /TASK_R1_invalid_ui\.md/)
	  })
	  rmSync(join(rDoneDir, 'TASK_R1_invalid_ui.md'))

	  // ── Non-UI appendix shape (the TASK_193 class) — ship-done validates the frozen Outcome for
	  // EVERY move; the headless mirror must too, or a hand-mv'd non-UI done file escapes the net.
	  const goodAppendix = '\n---\n\n## Outcome\n\n**Status**: completed\n**Reviewer**: internal-reviewer\n\n'
	    + '### Build gates\n- none\n\n'
	    + '### Runtime verify\n- Gate: skipped (fixture has no runtime change)\n- Result: n/a — fixture has no runtime change\n\n'
	    + ['Acceptance trace', 'Caveats', 'Follow-ups', 'Files touched'].map((h) => `### ${h}\n- none\n`).join('\n')
	  check('non-UI done task with a FREE-FORM Outcome (no frozen shape) → violation (exit 2)', () => {
	    writeFileSync(join(rDoneDir, 'TASK_R2_invalid_ui.md'), badBody(2))   // keep the UI violator: the non-UI flag must appear ALONGSIDE it
	    writeFileSync(join(rDoneDir, 'TASK_R3_nonui.md'), '# non-UI fixture\n\n## Goal\nrefactor mapper\n\n## Outcome\nall good, shipped by hand\n')
	    const r = runR()
	    assert.notEqual(r.status, 0, r.stdout + r.stderr)
	    assert.match(r.stdout + r.stderr, /TASK_R3_nonui\.md/)
	    assert.match(r.stdout + r.stderr, /non-UI tasks carry the same frozen shape/)
	    rmSync(join(rDoneDir, 'TASK_R2_invalid_ui.md'))
	  })
	  check('non-UI done task with the FROZEN appendix shape → clean, counted in the summary', () => {
	    writeFileSync(join(rDoneDir, 'TASK_R3_nonui.md'), '# non-UI fixture\n\n## Goal\nrefactor mapper\n' + goodAppendix)
	    const r = runR()
	    assert.equal(r.status, 0, r.stdout + r.stderr)
	    assert.match(r.stdout, /1 non-UI appendix shape\(s\)/)
	  })
} catch (e) {
  const detail = [e.message, e.stdout?.toString(), e.stderr?.toString()].filter(Boolean).join('\n')
  fail++; console.log(`${C.red}FAIL${C.reset} evidence-final setup threw\n     ${detail}`)
} finally {
  rmSync(ws, { recursive: true, force: true })
}

console.log(`\nevidence-final.test: ${pass} pass, ${fail} fail`)
process.exit(fail === 0 ? 0 : 1)
