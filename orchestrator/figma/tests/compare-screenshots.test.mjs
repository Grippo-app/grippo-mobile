// Fixture self-test for compare-screenshots.mjs (masked-SSIM screenshot gate) — 0 quota, no Figma.
// Synthesizes dark-UI PNG pairs in a temp dir, runs the script with env overrides, and asserts the
// per-(screen, variant) verdict. Guards the densest gate logic (background mask, per-channel SSIM,
// oracle-coverage SKIP, aspect guard, and strict nodeId identity binding) across primary/dark inputs.
// Run: node orchestrator/figma/tests/compare-screenshots.test.mjs
import { copyFileSync, mkdtempSync, mkdirSync, writeFileSync, rmSync, readFileSync, readdirSync, statSync } from 'node:fs'
import { join, dirname, delimiter, relative } from 'node:path'
import { tmpdir } from 'node:os'
import { fileURLToPath } from 'node:url'
import { execFileSync, spawnSync } from 'node:child_process'
import assert from 'node:assert/strict'
import { artifactSegment } from '../scripts/_util.mjs'
import { validComponentCapture, COMPONENT_CAPTURE_HASH } from './component-fixtures.mjs'
import { normalizeCapture as normalizeComponentCapture } from '../components/capture-normalizer.mjs'
import { bytesHash } from '../runtime/canonical-json.mjs'
import { sourceIdentity, validObservedCapture } from './observed-token-fixtures.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const SCRIPT = join(HERE, '..', 'scripts', 'compare-screenshots.mjs')

const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }

let Jimp
try { ({ Jimp } = await import('jimp')) } catch {
  console.log(`${C.red}FAIL${C.reset} jimp not installed — run root \`npm ci\` first`)
  process.exit(1)
}

// Direction B: validate the hand-rolled CIEDE2000 against Sharma's reference vectors.
import { deltaE00 } from '../scripts/compare-screenshots.mjs'
const SHARMA = [
  [[50, 2.6772, -79.7751], [50, 0, -82.7485], 2.0425],
  [[50, -1.3802, -84.2814], [50, 0, -82.7485], 1.0000],
  [[50, 0, 0], [50, -1, 2], 2.3669],
  [[60.2574, -34.0099, 36.2677], [60.4626, -34.1751, 39.4387], 1.2644],
  [[63.0109, -31.0961, -5.8663], [62.8187, -29.7946, -4.0864], 1.2630],
]
check('ΔE00: matches Sharma CIEDE2000 reference vectors (+ identity/symmetry)', () => {
  for (const [a, b, e] of SHARMA) assert.ok(Math.abs(deltaE00(a, b) - e) < 2e-3, `ΔE00(${a},${b})=${deltaE00(a, b)} ≠ ${e}`)
  assert.equal(deltaE00([40, 10, -5], [40, 10, -5]), 0)
  assert.ok(Math.abs(deltaE00(SHARMA[0][0], SHARMA[0][1]) - deltaE00(SHARMA[0][1], SHARMA[0][0])) < 1e-9)
})

// Dark canvas + optional white rect at [x0,y0,x1,y1] as fractions of W/H (proportional → a same-aspect
// resize keeps content aligned). Each PNG spec: { name, w, h, rect } (rect null = empty/background-only).
async function png(path, { w, h, rect, boxes, _captureTweak }) {
  const img = new Jimp({ width: w, height: h, color: 0x101010ff })
  const data = img.bitmap.data
  const fill = (x0, y0, x1, y1, r, g, b) => {
    for (let y = Math.max(0, y0 | 0); y < Math.min(h, y1 | 0); y++)
      for (let x = Math.max(0, x0 | 0); x < Math.min(w, x1 | 0); x++)
        data.set([r, g, b, 255], (y * w + x) << 2)
  }
  if (rect) fill(w * rect[0], h * rect[1], w * rect[2], h * rect[3], 255, 255, 255)
  // boxes: absolute-pixel rects [x0,y0,x1,y1, r?, g?, b?] (default white) — for px-precise probes.
  if (boxes) for (const b of boxes) fill(b[0], b[1], b[2], b[3], b[4] ?? 255, b[5] ?? 255, b[6] ?? 255)
  // Fixture oracles + captures share this generator, so identical specs would produce
  // byte-identical PNGs — which the real gate rejects as CAPTURE_IS_ORACLE_COPY (a copied
  // oracle can't certify a render). Capture files get a 1-unit blue tweak on pixel (0,0):
  // visually/metrically nil, byte-distinct — the way any real renderer's output would be.
  if (_captureTweak) data[2] ^= 1
  await img.write(path)
}

// Build a one-screen ("Home") workspace from oracle + capture specs, run the script.
async function writeImageOrRaw(path, spec) {
  if (Object.prototype.hasOwnProperty.call(spec, 'raw')) { writeFileSync(path, spec.raw); return }
  await png(path, spec)
}

function listFiles(root) {
  const out = []
  function walk(dir) {
    let names = []
    try { names = readdirSync(dir) } catch { return }
    for (const name of names) {
      const file = join(dir, name)
      const st = statSync(file)
      if (st.isDirectory()) walk(file)
      else out.push({ path: relative(root, file).replace(/\\/g, '/'), bytes: st.size })
    }
  }
  walk(root)
  return out.sort((a, b) => a.path.localeCompare(b.path))
}

function currentIndex(taskStem, indexNodes, oracles) {
  const oracleNames = new Set(oracles.map((oracle) => oracle.name))
  const nodes = {}
  let sequence = 0
  for (const [screen, raw] of Object.entries(indexNodes)) {
    sequence++
    const node = raw && typeof raw === 'object' ? raw : {}
    const explicit = ['url', 'nodeId', 'fetchedAt', 'darkUrl', 'darkNodeId', 'darkFetchedAt']
      .some((key) => Object.prototype.hasOwnProperty.call(node, key))
    const primary = explicit ? !!(node.url || node.nodeId) : oracleNames.has(`${screen}.png`) || !oracleNames.has(`${screen}.dark.png`)
    const dark = explicit ? !!(node.darkUrl || node.darkNodeId) : oracleNames.has(`${screen}.dark.png`)
    const fetchedAt = '2026-01-01T00:00:00.000Z'
    const url = (nodeId) => `https://www.figma.com/design/FileKey?node-id=${String(nodeId).replace(':', '-')}`
    const out = { kind: node.kind || 'screen', variants: [] }
    if (primary) {
      const nodeId = String(node.nodeId || `${sequence}:2`)
      Object.assign(out, { url: url(nodeId), nodeId, fetchedAt })
      out.variants.push({ id: 'primary', theme: 'light', locale: 'default', platform: 'shared', url: url(nodeId), nodeId, fetchedAt, imageFile: `${screen}.png` })
    }
    if (dark) {
      const nodeId = String(node.darkNodeId || `${sequence}:3`)
      Object.assign(out, { darkUrl: url(nodeId), darkNodeId: nodeId, darkFetchedAt: fetchedAt })
      out.variants.push({ id: 'dark', theme: 'dark', locale: 'default', platform: 'shared', url: url(nodeId), nodeId, fetchedAt, imageFile: `${screen}.dark.png` })
    }
    nodes[screen] = out
  }
  return { schemaVersion: 3, taskStem, nodes }
}

function writeTokenSidecars(index, screenDirectory) {
  let sequence = 0
  for (const [screenKey, node] of Object.entries(index.nodes)) {
    for (const variant of node.variants) {
      sequence++
      const source = sourceIdentity({
        nodeId: variant.nodeId,
        context: { theme: variant.theme, locale: variant.locale, platform: variant.platform },
        origin: { kind: 'task-screen', taskStem: index.taskStem, screenKey, variantId: variant.id },
      })
      const capture = validObservedCapture({
        source,
        captureOperationId: `tokop_${sequence.toString(16).padStart(16, '0')}`,
        captureSequence: 1,
      })
      const bytes = Buffer.from(JSON.stringify(capture, null, 2) + '\n')
      variant.tokensFile = `variant-${sequence}.tokens.json`
      variant.tokensHash = bytesHash(bytes)
      variant.captureOperationId = capture.captureOperationId
      variant.captureSequence = capture.captureSequence
      writeFileSync(join(screenDirectory, variant.tokensFile), bytes)
    }
  }
}

function currentSpec(name, raw) {
  const screen = raw.screen || name.replace(/\.spec\.json$/, '')
  const frameSizeDp = raw.frameSizeDp
  const theme = raw.theme === 'dark' ? 'dark' : 'light'
  const sourceNodeId = '1:2'
  const elements = (Array.isArray(raw.elements) && raw.elements.length ? raw.elements : [{
    stableId: 'frame', name: 'Frame', bboxDp: { x: 0, y: 0, w: frameSizeDp.w, h: frameSizeDp.h },
  }]).map((element, index) => {
    const out = { ...element, stableId: element.stableId || element.figmaNodeId || `element-${index + 1}`, name: element.name || element.stableId || element.figmaNodeId || `Element ${index + 1}` }
    if (out.textStyle && Object.hasOwn(out.textStyle, 'fontSize')) {
      const sizeSp = out.textStyle.fontSize
      out.textStyle = { sizeSp, weight: 400, lineHeightSp: sizeSp * 1.25, case: 'as-is' }
    }
    return out
  })
  return {
    schemaVersion: 2,
    screen,
    frameSizeDp,
    theme,
    source: { fileKey: 'FileKey', nodeId: sourceNodeId },
    rootNodeId: sourceNodeId,
    nodes: elements.map((element) => ({ ...element })),
    elements,
    coordinateSystem: { units: 'dp', density: 1, origin: 'frame' },
    themeMetadata: { themeKey: theme },
    ...(raw.chromeCrop ? { chromeCrop: raw.chromeCrop } : {}),
  }
}

async function run({ oracles, captures, args = [], manifest = null, manifestRaw = null, duplicateCapture = false, indexNodes = { Home: {} }, env: envOverrides = {}, oldArtifactRuns = [], artifactRootSuffix = null, specs = [], copyOracleAsCapture = false, noDefaultManifest = false, allowFailure = false, taskStem = 'TASK_1_fixture', pipelineRunId = 'compare-screenshots-test' }) {
  const ws = mkdtempSync(join(tmpdir(), 'cmp-ss-'))
  let appliedDefaultManifest = false
  try {
	    const sdir = join(ws, 'screens', taskStem), rdir = join(ws, 'robo'), reportDir = join(ws, 'reports'), rdir2 = join(ws, 'robo2'), cacheRoot = join(ws, 'figma-cache'), artifactRoot = join(cacheRoot, 'artifacts', 'screenshot')
    mkdirSync(sdir, { recursive: true }); mkdirSync(rdir, { recursive: true }); mkdirSync(reportDir, { recursive: true })
    if (duplicateCapture) mkdirSync(rdir2, { recursive: true })
    for (const oldRun of oldArtifactRuns) {
      const oldDir = join(artifactRoot, artifactSegment(taskStem), artifactSegment(oldRun))
      mkdirSync(oldDir, { recursive: true })
      writeFileSync(join(oldDir, 'old.txt'), 'old artifact run\n')
    }
	    const index = currentIndex(taskStem, indexNodes, oracles)
	    writeTokenSidecars(index, sdir)
	    writeFileSync(join(sdir, 'index.json'), JSON.stringify(index))
	    for (const o of oracles) await writeImageOrRaw(join(sdir, o.name), o)
	    for (const sp of specs) writeFileSync(join(sdir, sp.name), JSON.stringify(currentSpec(sp.name, sp.data)))
	    for (const c of captures) {
	      const spec = Object.prototype.hasOwnProperty.call(c, 'raw') ? c : { ...c, _captureTweak: true }
	      await writeImageOrRaw(join(rdir, c.name), spec)
	      if (duplicateCapture) await writeImageOrRaw(join(rdir2, c.name), spec)
    }
	    // Forgery probe: the capture IS the oracle file, byte for byte.
	    if (copyOracleAsCapture) copyFileSync(join(sdir, 'Home.png'), join(rdir, 'HomeScreenshot.png'))
	    const taskFile = join(ws, 'task.md')
	    writeFileSync(taskFile, '')
	    const env = { ...process.env, FIGMA_CACHE_ROOT: cacheRoot, FIGMA_SPEC_SCREENS_DIR: join(ws, 'screens'), FIGMA_REPORTS_DIR: reportDir, FIGMA_SCREEN_TASK_FILE: taskFile, FIGMA_PIPELINE_RUN_ID: pipelineRunId == null ? '' : pipelineRunId }
    // Existing metric/R1 probes assert the STRICT pixel verdict (SSIM/zone/color BLOCKERs); the
    // gate default comes from the committed project-config (R3; `strict` in the template, but a
    // project may downgrade), so pin strict here to stay config-independent. R3 mode tests override it.
    env.SCREENSHOT_PIXEL_GATE = 'strict'
    if (artifactRootSuffix) env.FIGMA_COMPARE_ARTIFACTS_DIR = join(cacheRoot, artifactRootSuffix)
    env.ROBORAZZI_OUTPUT_DIR = rdir
    if (duplicateCapture) env.ROBORAZZI_OUTPUT_DIRS = [rdir, rdir2].join(delimiter)
    if (manifest || manifestRaw != null) {
      const mp = join(ws, 'manifest.json')
      writeFileSync(mp, manifestRaw != null ? manifestRaw : JSON.stringify(manifest))
      env.SCREENSHOT_CAPTURE_MANIFEST = mp
    } else if (!noDefaultManifest) {
      // Every normal fixture uses the exact current manifest identity contract. A capture
      // with the wrong name is intentionally absent rather than recovered by basename.
      const entries = []
      for (const [screen, node] of Object.entries(index.nodes)) {
        for (const variant of node.variants) {
          const suffix = variant.imageFile.slice(screen.length, -4)
          const captureName = `${screen}Screenshot${suffix}.png`
          for (const dir of duplicateCapture ? [rdir, rdir2] : [rdir]) {
            const path = join(dir, captureName)
            if (readdirSync(dir).includes(captureName)) entries.push({ captureName, path, nodeId: variant.nodeId, variantId: variant.id, primaryState: true })
          }
        }
      }
      const mp = join(ws, 'manifest.json')
      writeFileSync(mp, JSON.stringify({ captures: entries }))
      env.SCREENSHOT_CAPTURE_MANIFEST = mp
      appliedDefaultManifest = true
    }
    if (args.includes('--gate') && (!env.SCREENSHOT_CAPTURE_MANIFEST || appliedDefaultManifest) && !Object.prototype.hasOwnProperty.call(envOverrides, 'SCREENSHOT_CAPTURE_STARTED_AT')) {
      env.SCREENSHOT_CAPTURE_STARTED_AT = String(Date.now() - 1000)
    }
    const fixtureInventory = envOverrides.fixtureComponentInventory
    const fixtureMappings = envOverrides.fixtureComponentMappings
    const actualEnvOverrides = { ...envOverrides }
    delete actualEnvOverrides.fixtureComponentInventory
    delete actualEnvOverrides.fixtureComponentMappings
    Object.assign(env, actualEnvOverrides)
    const fixtureArgs = []
    if (fixtureInventory) fixtureArgs.push('--fixture-component-inventory', fixtureInventory)
    if (fixtureMappings) fixtureArgs.push('--fixture-component-mappings', fixtureMappings)
    let stdout = ''
    let status = 0
    try {
      stdout = execFileSync('node', [SCRIPT, taskStem, ...args, ...fixtureArgs], { env, stdio: 'pipe' }).toString()
    } catch (e) {
      if (!allowFailure) throw e
      status = e.status
      stdout = (e.stdout || '').toString()
    }
    const report = JSON.parse(readFileSync(join(reportDir, `screenshot-${taskStem}.json`), 'utf8'))
    return { stdout, report, artifactFiles: listFiles(artifactRoot), status }
  } finally {
    rmSync(ws, { recursive: true, force: true })
  }
}

async function runExit({ oracles, captures, args = [], manifest = null, manifestRaw = null, indexNodes = { Home: {} }, env = {}, artifactRootSuffix = null, specs = [] }) {
  try {
    await run({ oracles, captures, args, manifest, manifestRaw, indexNodes, env, artifactRootSuffix, specs })
    return 0
  } catch (e) {
    return e.status
  }
}

const CENTER = [0.4, 0.4, 0.6, 0.6], CORNER = [0.05, 0.05, 0.25, 0.25]
const P = (rect, w = 120, h = 240) => ({ w, h, rect })           // shorthand: a 120x240 dark canvas
const validCapture = (entry = {}) => {
  const captureName = entry.captureName || (entry.path ? entry.path.split(/[\\/]/).pop() : 'HomeScreenshot.png')
  const variantId = entry.variantId || (captureName.includes('.dark.') ? 'dark' : 'primary')
  return { captureName, path: entry.path || `robo/${captureName}`, nodeId: entry.nodeId || '1:2', variantId, primaryState: true, ...entry }
}

try {
  // primary theme (<Screen>.png ↔ <Screen>Screenshot.png)
  const sIdentical = await run({ oracles: [{ name: 'Home.png', ...P(CENTER) }], captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }] })
  const longStem = 'TASK_250_zadacha_na_uluchshenii_flou_diagnostiki_i_telemetrii_terminaldiagnosticscreen'
  const sLongIdentity = await run({
    oracles: [{ name: 'Home.png', ...P(CENTER) }],
    captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }],
    taskStem: longStem,
    pipelineRunId: null,
  })
  const sMoved     = await run({ oracles: [{ name: 'Home.png', ...P(CENTER) }], captures: [{ name: 'HomeScreenshot.png', ...P(CORNER) }] })
  const sWrongName = await run({ oracles: [{ name: 'Home.png', ...P(CENTER) }], captures: [{ name: 'OtherScreenshot.png', ...P(CENTER) }] })
  const sEmpty     = await run({ oracles: [{ name: 'Home.png', ...P(null) }],   captures: [{ name: 'HomeScreenshot.png', ...P([0.2, 0.2, 0.8, 0.8]) }] })
  // Uniform-fill oracles degrade to a colour-only verdict instead of a
  // LOW_CONTENT_ORACLE gate deadlock.
  const sSolidMatch = await run({ oracles: [{ name: 'Home.png', ...P(null) }], captures: [{ name: 'HomeScreenshot.png', ...P(null) }] })
  const sSolidGateExit = await runExit({ oracles: [{ name: 'Home.png', ...P(null) }], captures: [{ name: 'HomeScreenshot.png', ...P(null) }], args: ['--gate'] })
  // FIX 2 (color-only dilution guard): a uniform oracle whose render carries a SMALL bright
  // divergent block. The box (24x30=720px) is ~2.5% of the 120x240 frame (28800px): above the
  // 2% divergent-fraction floor, but a pure-white spike over only 2.5% of an otherwise-matching
  // dark fill dilutes the whole-frame MEAN ΔE00 to ~2.5 — UNDER DELTAE_PASS=3. The mean-only
  // verdict would therefore PASS; the divergent-pixel-fraction guard (>2% at ΔE00>10) BLOCKS it.
  const sSolidSparseDivergent = await run({
    oracles: [{ name: 'Home.png', ...P(null) }],
    captures: [{ name: 'HomeScreenshot.png', w: 120, h: 240, boxes: [[20, 20, 44, 50, 255, 255, 255]] }],
  })
  // FIX 2 counter-case: a uniform oracle + a UNIFORM render whose colour differs by a tiny,
  // below-ΔE00>10 amount everywhere → mean under DELTAE_PASS AND ~0 divergent pixels → PASS.
  const sSolidTinyDrift = await run({
    oracles: [{ name: 'Home.png', ...P(null) }],
    captures: [{ name: 'HomeScreenshot.png', w: 120, h: 240, boxes: [[0, 0, 120, 240, 17, 16, 16]] }],
  })
  const sAspect    = await run({ oracles: [{ name: 'Home.png', ...P(CENTER, 240, 120) }], captures: [{ name: 'HomeScreenshot.png', ...P(CENTER, 120, 240) }] })
  const sRescale   = await run({ oracles: [{ name: 'Home.png', ...P(CENTER, 240, 480) }], captures: [{ name: 'HomeScreenshot.png', ...P(CENTER, 200, 400) }] })
  // adaptive: both themes pulled → two comparisons; dark-only → one (no primary line)
	  const sBoth = await run({
	    oracles:  [{ name: 'Home.png', ...P(CENTER) }, { name: 'Home.dark.png', ...P(CENTER) }],
	    captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }, { name: 'HomeScreenshot.dark.png', ...P(CENTER) }],
	  })
	  const sDarkOnly = await run({ oracles: [{ name: 'Home.dark.png', ...P(CENTER) }], captures: [{ name: 'HomeScreenshot.dark.png', ...P(CENTER) }] })
	  const sDarkOnlyStalePrimary = await run({
	    oracles: [{ name: 'Home.png', ...P(CENTER) }, { name: 'Home.dark.png', ...P(CENTER) }],
	    captures: [{ name: 'HomeScreenshot.dark.png', ...P(CENTER) }],
	    indexNodes: { Home: { darkUrl: 'https://figma.example/dark', darkNodeId: '1:2' } },
	  })
	  const sDuplicate = await run({ oracles: [{ name: 'Home.png', ...P(CENTER) }], captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }], duplicateCapture: true })
		  const sManifestStale = await run({ oracles: [{ name: 'Home.png', ...P(CENTER) }], captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }], manifest: { captures: [] } })
		  const sOracleCopy = await run({ oracles: [{ name: 'Home.png', ...P(CENTER) }], captures: [], copyOracleAsCapture: true })
		  const sUncontained = await run({ oracles: [{ name: 'Home.png', ...P(CENTER) }], captures: [], indexNodes: { Home: { nodeId: '1:2' } }, manifest: { captures: [validCapture({ nodeId: '1:2', path: '../screens/TASK_1_fixture/Home.png' })] } })
		  const sManifestOtherPath = await run({ oracles: [{ name: 'Home.png', ...P(CENTER) }], captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }], manifest: { captures: [validCapture({ path: '/tmp/other-module/HomeScreenshot.png' })] } })
		  const sManifestBarePath = await run({ oracles: [{ name: 'Home.png', ...P(CENTER) }], captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }], manifest: { captures: [validCapture({ path: 'HomeScreenshot.png' })] } })
		  const sManifestCaptureName = await run({ oracles: [{ name: 'Home.png', ...P(CENTER) }], captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }], manifest: { captures: [validCapture({ captureName: 'HomeScreenshot.png' })] } })
			  const sManifestFalsePrimary = await run({ oracles: [{ name: 'Home.png', ...P(CENTER) }], captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }], manifest: { captures: [validCapture({ primaryState: false })] } })
			  const sManifestRetiredAlias = await run({ oracles: [{ name: 'Home.png', ...P(CENTER) }], captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }], manifest: { captures: [{ ...validCapture(), loaded: true }] } })
			  const sStartedAtStale = await run({ oracles: [{ name: 'Home.png', ...P(CENTER) }], captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }], env: { SCREENSHOT_CAPTURE_STARTED_AT: String(Date.now() + 60000) } })
			  const ancientStartedAtExit = await runExit({ oracles: [{ name: 'Home.png', ...P(CENTER) }], captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }], args: ['--gate'], env: { SCREENSHOT_CAPTURE_STARTED_AT: '1' } })
  const sSemantic = await run({ oracles: [{ name: 'Home.png', ...P(CENTER) }], captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }], args: ['--semantic'] })
			  const sRetention = await run({
			    oracles: [{ name: 'Home.png', ...P(CENTER) }],
			    captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }],
			    oldArtifactRuns: ['old-a', 'old-b'],
			    env: { SCREENSHOT_ARTIFACT_RETENTION: '1' },
			  })
			  // GENUINE zero-artifact run: MISSING_CAPTURE (wrong capture name) AND an unreadable oracle,
			  // so even the Step-B reference emit (emitOracleReference → Jimp.read) produces nothing.
			  // The retention safety guard (`if (artifactEntries.length)`) must then NOT prune history.
			  const sNoArtifactRetention = await run({
			    oracles: [{ name: 'Home.png', raw: 'not-a-png' }],
			    captures: [{ name: 'OtherScreenshot.png', ...P(CENTER) }],
			    oldArtifactRuns: ['old-a', 'old-b'],
			    env: { SCREENSHOT_ARTIFACT_RETENTION: '1' },
			  })
			  const missingGateExit = await runExit({ oracles: [{ name: 'Home.png', ...P(CENTER) }], captures: [{ name: 'OtherScreenshot.png', ...P(CENTER) }], args: ['--gate'] })
			  const missingFreshExit = await runExit({ oracles: [{ name: 'Home.png', ...P(CENTER) }], captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }], args: ['--gate'], env: { SCREENSHOT_CAPTURE_STARTED_AT: '' } })
			  const outsideArtifactRootExit = await runExit({ oracles: [{ name: 'Home.png', ...P(CENTER) }], captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }], env: { FIGMA_COMPARE_ARTIFACTS_DIR: join(tmpdir(), 'outside-compare-artifacts') } })
			  const nestedArtifactRootExit = await runExit({ oracles: [{ name: 'Home.png', ...P(CENTER) }], captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }], artifactRootSuffix: join('artifacts', 'screenshot', 'nested') })
			  const badRunIdExit = await runExit({ oracles: [{ name: 'Home.png', ...P(CENTER) }], captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }], env: { FIGMA_PIPELINE_RUN_ID: 'run/1' } })
			  const emptyIndexExit = await runExit({ oracles: [], captures: [], args: ['--gate'], indexNodes: {} })
		  const unreadableManifestExit = await runExit({ oracles: [{ name: 'Home.png', ...P(CENTER) }], captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }], args: ['--gate'], manifestRaw: '{not-json' })
		  const malformedSpec = await run({
		    oracles: [{ name: 'Home.png', ...P(CENTER) }], captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }],
		    args: ['--gate'], allowFailure: true,
		    specs: [{ name: 'Home.spec.json', data: { screen: 'Home', theme: 'light', frameSizeDp: { w: -1, h: 240 } } }],
		  })
		  const orphanOracleExit = await runExit({ oracles: [{ name: 'Home.png', ...P(CENTER) }], captures: [], args: ['--gate'], indexNodes: {} })
	  const corruptCaptureExit = await runExit({ oracles: [{ name: 'Home.png', ...P(CENTER) }], captures: [{ name: 'HomeScreenshot.png', raw: 'not-a-png' }], args: ['--gate'] })
	  // These fixtures isolate the MAJOR-band routing: the moved CORNER rect scores far below the
	  // now-default-ON zone floor (grid fallback Z_BLOCKER=0.35), so ZONE_SSIM_BLOCKER would fire
	  // first and mask the band under test. Pin SCREENSHOT_ZONE_GATE=0 here so the SSIM band is the
	  // sole verdict driver — the zone gate has its own dedicated pins below.
	  const majorEnv = { SCREENSHOT_PASS_THRESHOLD: '0.09', SCREENSHOT_MINOR_THRESHOLD: '0.08', SCREENSHOT_MAJOR_THRESHOLD: '0.05', SCREENSHOT_ZONE_GATE: '0' }
	  // Default (strict): a MAJOR row BLOCKS in gate mode (SSIM_MAJOR blocker, exit 2).
	  const sMajorBlocksExit = await runExit({ oracles: [{ name: 'Home.png', ...P(CENTER) }], captures: [{ name: 'HomeScreenshot.png', ...P(CORNER) }], args: ['--gate'], env: majorEnv })
	  // Rollback knob: SCREENSHOT_MAJOR_BAND=advisory restores the reviewed-WARN routing.
	  const sMajorAdvisory = await run({
    oracles: [{ name: 'Home.png', ...P(CENTER) }],
    captures: [{ name: 'HomeScreenshot.png', ...P(CORNER) }],
    args: ['--gate'],
    env: { ...majorEnv, SCREENSHOT_MAJOR_BAND: 'advisory' },
  })

  // C5: one nodeId legitimately serving BOTH variants must not turn both captures into a
  // false DUPLICATE_CAPTURE — the current manifest disambiguates them by variantId.
  const c5Nodes = { Home: { url: 'https://f/x?node-id=7-70', nodeId: '7:70', darkUrl: 'https://f/x?node-id=7-70', darkNodeId: '7:70' } }
  const sSharedNodeByPath = await run({
    oracles: [{ name: 'Home.png', ...P(CENTER) }, { name: 'Home.dark.png', ...P(CENTER) }],
    captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }, { name: 'HomeScreenshot.dark.png', ...P(CENTER) }],
    indexNodes: c5Nodes,
    manifest: { captures: [
      validCapture({ nodeId: '7:70', path: 'robo/HomeScreenshot.png' }),
      validCapture({ nodeId: '7:70', path: 'robo/HomeScreenshot.dark.png' }),
    ] },
  })
  const sSharedNodeByTheme = await run({
    oracles: [{ name: 'Home.png', ...P(CENTER) }, { name: 'Home.dark.png', ...P(CENTER) }],
    captures: [{ name: 'renamedA.png', ...P(CENTER) }, { name: 'renamedB.png', ...P(CENTER) }],
    indexNodes: c5Nodes,
    manifest: { captures: [
      validCapture({ nodeId: '7:70', variantId: 'primary', path: 'robo/renamedA.png' }),
      validCapture({ nodeId: '7:70', variantId: 'dark', path: 'robo/renamedB.png' }),
    ] },
  })
  const sSharedNodeDup = await run({
    oracles: [{ name: 'Home.png', ...P(CENTER) }],
    captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }],
    duplicateCapture: true,
    indexNodes: { Home: { url: 'https://f/x', nodeId: '7:70' } },
    manifest: { captures: [
      validCapture({ nodeId: '7:70', variantId: 'primary', path: 'robo/HomeScreenshot.png' }),
      validCapture({ nodeId: '7:70', variantId: 'primary', path: 'robo2/HomeScreenshot.png' }),
    ] },
  })
  const sSharedNodeWrongVariant = await run({
    oracles: [{ name: 'Home.png', ...P(CENTER) }],
    captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }],
    indexNodes: { Home: { url: 'https://f/x', nodeId: '7:70' } },
    manifest: { captures: [
      validCapture({ nodeId: '7:70', variantId: 'dark', path: 'robo/other1.png' }),
      validCapture({ nodeId: '7:70', variantId: 'dark', path: 'robo/other2.png' }),
      validCapture({ captureName: 'HomeScreenshot.png' }),
    ] },
  })
  check('C5: shared nodeId binds per-variant by explicit variantId (no false DUPLICATE_CAPTURE)', () => {
    assert.match(sSharedNodeByPath.stdout, /Home \[primary\]: PASS/)
    assert.match(sSharedNodeByPath.stdout, /Home \[dark\]: PASS/)
    assert.ok(!sSharedNodeByPath.report.issues.some((i) => i.issueKind === 'DUPLICATE_CAPTURE'))
  })
  check('C5: explicit variantId disambiguates RENAMED captures for a shared nodeId', () => {
    assert.match(sSharedNodeByTheme.stdout, /Home \[primary\]: PASS/)
    assert.match(sSharedNodeByTheme.stdout, /Home \[dark\]: PASS/)
    assert.ok(!sSharedNodeByTheme.report.issues.some((i) => i.issueKind === 'DUPLICATE_CAPTURE'))
  })
  check('C5: SAME-variant multi-binding is still a genuine DUPLICATE_CAPTURE blocker', () => {
    assert.match(sSharedNodeDup.stdout, /Home \[primary\]: DUPLICATE_CAPTURE/)
    assert.ok(sSharedNodeDup.report.issues.some((i) => i.issueKind === 'DUPLICATE_CAPTURE' && i.severity === 'BLOCKER'))
  })
  check('C5: a wrong variantId never falls back to capture-name discovery', () => {
    assert.match(sSharedNodeWrongVariant.stdout, /Home \[primary\]: MISSING_CAPTURE/)
  })

  // C5 single-entry theft: the same nodeId serves BOTH themes but only the LIGHT capture
  // exists (ONE manifest entry, no theme field). Unfiltered, that entry binds to the dark
  // variant too and "certifies" a dark render that was never captured — the dark variant
  // must instead fail closed as MISSING_CAPTURE.
  const sSharedNodeSingleEntry = await run({
    oracles: [{ name: 'Home.png', ...P(CENTER) }, { name: 'Home.dark.png', ...P(CENTER) }],
    captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }],
    indexNodes: c5Nodes,
    manifest: { captures: [validCapture({ nodeId: '7:70', path: 'robo/HomeScreenshot.png' })] },
  })
  check('C5: single unthemed entry on a shared nodeId cannot certify the OTHER theme', () => {
    assert.match(sSharedNodeSingleEntry.stdout, /Home \[primary\]: PASS/)
    assert.match(sSharedNodeSingleEntry.stdout, /Home \[dark\]: MISSING_CAPTURE/)
    assert.ok(sSharedNodeSingleEntry.report.issues.some((i) => i.issueKind === 'MISSING_CAPTURE'), 'dark surfaces as MISSING_CAPTURE')
  })

  // KIND_GEOMETRY_MISMATCH: an aspect-similar [component] captured on the WRONG canvas —
  // record-and-rescale normalizes the size away, so the ASPECT gate is blind and the only
  // honest witness is the named WARN pointing at the qualifier/container (recipe: 1px == 1dp).
  const kindSpec = { name: 'Home.spec.json', data: { frameSizeDp: { w: 120, h: 240 } } }
  const sKindWrongCanvas = await run({
    oracles: [{ name: 'Home.png', ...P(CENTER) }],
    captures: [{ name: 'HomeScreenshot.png', ...P(CENTER, 160, 320) }],
    indexNodes: { Home: { kind: 'component' } },
    specs: [kindSpec],
  })
  const sKindRightCanvas = await run({
    oracles: [{ name: 'Home.png', ...P(CENTER) }],
    captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }],
    indexNodes: { Home: { kind: 'component' } },
    specs: [kindSpec],
  })
  const sScreenWrongCanvas = await run({
    oracles: [{ name: 'Home.png', ...P(CENTER) }],
    captures: [{ name: 'HomeScreenshot.png', ...P(CENTER, 160, 320) }],
    specs: [kindSpec],
  })
  check('[component] on an aspect-similar wrong canvas → WARN KIND_GEOMETRY_MISMATCH (recorded tol)', () => {
    const hit = sKindWrongCanvas.report.issues.find((i) => i.issueKind === 'KIND_GEOMETRY_MISMATCH')
    assert.ok(hit, 'named issue present')
    assert.equal(hit.severity, 'WARN')
    assert.equal(sKindWrongCanvas.report.thresholds.kindGeometryTol, 0.25)
  })
  check('[component] captured at its own frameSizeDp → no KIND_GEOMETRY_MISMATCH', () => {
    assert.ok(!sKindRightCanvas.report.issues.some((i) => i.issueKind === 'KIND_GEOMETRY_MISMATCH'))
  })
  check('screen kind is exempt from the kind-geometry check (full-bleed canvas is its own)', () => {
    assert.ok(!sScreenWrongCanvas.report.issues.some((i) => i.issueKind === 'KIND_GEOMETRY_MISMATCH'))
  })

  // --- R1: device-chrome band exclusion (screen-only, hard-capped) ---
  // A realistic 200x800dp frame (1px == 1dp): the top status band is min(24dp, 6%*H=48) = 24px and
  // the bottom nav band min(48dp, 9%*H=72) = 48px. Body content sits safely between (y 200..600).
  const R1_FRAME = { frameSizeDp: { w: 200, h: 800 }, screen: 'Home' }
  const R1_BODY = [[40, 200, 160, 600]]                  // matching content in BOTH images
  const R1_STATUSBAR = [0, 0, 200, 24]                   // a 24px status bar the designer drew (oracle only)
  // (1) oracle WITH a drawn status bar vs a render WITHOUT one → the top band is excluded, so the
  //     region mismatch never scores: PASS. This is the #1 false-block R1 kills.
  const sBarMasked = await run({
    oracles: [{ name: 'Home.png', w: 200, h: 800, boxes: [...R1_BODY, R1_STATUSBAR] }],
    captures: [{ name: 'HomeScreenshot.png', w: 200, h: 800, boxes: R1_BODY }],
    specs: [{ name: 'Home.spec.json', data: R1_FRAME }],
  })
  // (2) SAME fixture with the band DISABLED (mutation proof): the status-bar mismatch now scores
  //     and the top region trips the floor → BLOCKER. Proves the band mask is load-bearing.
  const sBarUnmasked = await run({
    oracles: [{ name: 'Home.png', w: 200, h: 800, boxes: [...R1_BODY, R1_STATUSBAR] }],
    captures: [{ name: 'HomeScreenshot.png', w: 200, h: 800, boxes: R1_BODY }],
    specs: [{ name: 'Home.spec.json', data: R1_FRAME }],
    env: { SCREENSHOT_STATUS_BAR_DP: '0', SCREENSHOT_NAV_BAR_DP: '0' },
  })
  // (3) oracle WITHOUT a status bar vs the same render → also PASS (masking a clean pair is a no-op).
  const sNoBar = await run({
    oracles: [{ name: 'Home.png', w: 200, h: 800, boxes: R1_BODY }],
    captures: [{ name: 'HomeScreenshot.png', w: 200, h: 800, boxes: R1_BODY }],
    specs: [{ name: 'Home.spec.json', data: R1_FRAME }],
  })
  const sSuspectedTop = await run({
    oracles: [{ name: 'Home.png', w: 200, h: 800, boxes: [...R1_BODY, R1_STATUSBAR] }],
    captures: [{ name: 'HomeScreenshot.png', w: 200, h: 800, boxes: R1_BODY }],
    specs: [{ name: 'Home.spec.json', data: { ...R1_FRAME, elements: [{ stableId: 'top-strip', name: 'AppHeader', bboxDp: { x: 0, y: 0, w: 200, h: 24 } }] } }],
    env: { SCREENSHOT_NAV_BAR_DP: '0' },
  })
  check('R1: a drawn status bar (oracle) vs none (render) compares clean — band excluded → PASS', () => {
    assert.equal(sBarMasked.report.overall, 'PASS')
    assert.equal(sBarMasked.report.results[0].status, 'PASS')
    assert.ok(sBarMasked.report.results[0].chromeExcludedPx > 0, 'chrome pixels recorded as excluded')
    assert.equal(sBarMasked.report.thresholds.statusBarDp, 24)
    assert.equal(sBarMasked.report.thresholds.navBarDp, 48)
  })
  check('R1 mutation: with the band DISABLED the same status-bar mismatch BLOCKS (band is load-bearing)', () => {
    assert.notEqual(sBarUnmasked.report.overall, 'PASS')
    assert.ok(sBarUnmasked.report.issues.some((i) => i.severity === 'BLOCKER'), 'a blocker fires without the band mask')
  })
  check('R1: a status-bar-free pair still PASSes (masking a clean screen is a no-op)', () => {
    assert.equal(sNoBar.report.overall, 'PASS')
  })
  check('R1 fail-closed: an ambiguous top strip is compared instead of hidden by the status-bar mask', () => {
    assert.notEqual(sSuspectedTop.report.overall, 'PASS')
    assert.equal(sSuspectedTop.report.results[0].chromeExcludedPx || 0, 0)
  })

  // (4) KIND-AWARE: the band mask is SCREEN-ONLY. A thin top element (24px, inside the screen band)
  //     that the render drops → a [screen] masks it (PASS) but a [dialog] compares it (BLOCK),
  //     because a dialog has no system status bar to normalize away. Same pixels, only kind differs.
  const R1_TOPEL = [0, 0, 200, 24]                       // dialog's OWN top content (e.g. a title row)
  const kindAwareIn = { oracles: [{ name: 'X.png', w: 200, h: 800, boxes: [...R1_BODY, R1_TOPEL] }], captures: [{ name: 'XScreenshot.png', w: 200, h: 800, boxes: R1_BODY }] }
  const sTopScreen = await run({ ...kindAwareIn, indexNodes: { X: {} }, specs: [{ name: 'X.spec.json', data: { screen: 'X', frameSizeDp: { w: 200, h: 800 } } }] })
  const sTopDialog = await run({ ...kindAwareIn, indexNodes: { X: { kind: 'dialog' } }, specs: [{ name: 'X.spec.json', data: { screen: 'X', frameSizeDp: { w: 200, h: 800 } } }] })
  check('R1 kind-aware: a [screen] masks a thin top row (system-bar zone) → PASS', () => {
    assert.equal(sTopScreen.report.overall, 'PASS')
  })
  check('R1 kind-aware: the SAME thin top divergence in a [dialog] is COMPARED (no band) → BLOCKS', () => {
    assert.notEqual(sTopDialog.report.overall, 'PASS')
    assert.ok((sTopDialog.report.results[0].chromeExcludedPx || 0) === 0, 'a dialog gets NO device-chrome band')
  })

  // R6 runs after R1 in the real pull flow: once normalize-oracle stamps chromeCrop, y=0 is
  // APP content rather than device chrome. Applying a symmetric band again would hide
  // a real top-row regression even though both inputs are now chrome-free.
  const sTopChromeCropped = await run({
    ...kindAwareIn,
    indexNodes: { X: {} },
    specs: [{ name: 'X.spec.json', data: { screen: 'X', frameSizeDp: { w: 200, h: 800 }, chromeCrop: { topDp: 47, bottomDp: 34, matched: ['9:41', 'Home Indicator'], at: 'T' } } }],
  })
  check('R6 integration: a chromeCrop-stamped screen compares its app-content edges (no second mask)', () => {
    assert.notEqual(sTopChromeCropped.report.overall, 'PASS')
    assert.equal(sTopChromeCropped.report.results[0].chromeExcludedPx || 0, 0)
  })

  // (5) FAIL-CLOSED cap (adversarial finding): a hostile/units-wrong tiny frameSizeDp.h cannot
  //     blind a real screen. A 120px header break + a corrupt frameSizeDp.h=120 (image is 800px):
  //     the aspect-inconsistency check rejects the bad scale and the caps bound the band, so the
  //     header break is still COMPARED → BLOCKS. Proves a spec cannot launder a break via R1.
  const R1_HEADER = [0, 0, 200, 120]
  const sCorruptFrame = await run({
    oracles: [{ name: 'Home.png', w: 200, h: 800, boxes: [[40, 200, 160, 600], R1_HEADER] }],
    captures: [{ name: 'HomeScreenshot.png', w: 200, h: 800, boxes: [[40, 200, 160, 600]] }],
    specs: [{ name: 'Home.spec.json', data: { screen: 'Home', frameSizeDp: { w: 200, h: 120 } } }],
  })
  check('R1 fail-closed: a corrupt tiny frameSizeDp.h cannot blind a real screen — header break BLOCKS', () => {
    assert.notEqual(sCorruptFrame.report.overall, 'PASS')
    // the excluded band stays bounded by the caps (<=6%+9%=15% of the frame), never the 30% the
    // old unbounded per-band clamp allowed a corrupt spec to reach.
    assert.ok((sCorruptFrame.report.results[0].chromeExcludedPx || 0) <= 0.16 * 200 * 800, 'excluded band bounded by the hard caps')
  })

  // --- R3: per-project pixel-verdict routing (screenshotPixelGate strict|advisory|off) ---
  const R3_ORACLE = [{ name: 'Home.png', w: 120, h: 240, boxes: [[40, 80, 100, 160]] }]
  const R3_CAPTURE = [{ name: 'HomeScreenshot.png', w: 120, h: 240, boxes: [[10, 20, 46, 66]] }]   // grossly moved+resized → very low SSIM
  const r3Base = { oracles: R3_ORACLE, captures: R3_CAPTURE, args: ['--gate'] }
  // r3Strict blocks under --gate (exit 2 → run() would throw), so read its report in advisory-CLI
  // mode (still BLOCKER overall) and take the exit code from runExit.
  const r3Strict = await run({ oracles: R3_ORACLE, captures: R3_CAPTURE, env: { SCREENSHOT_PIXEL_GATE: 'strict' } })
  const r3StrictExit = await runExit({ ...r3Base, env: { SCREENSHOT_PIXEL_GATE: 'strict' } })
  const r3Advisory = await run({ ...r3Base, env: { SCREENSHOT_PIXEL_GATE: 'advisory' } })
  const r3AdvisoryExit = await runExit({ ...r3Base, env: { SCREENSHOT_PIXEL_GATE: 'advisory' } })
  const r3Off = await run({ ...r3Base, env: { SCREENSHOT_PIXEL_GATE: 'off' } })
  // completeness/anti-forgery (a missing capture) must ALWAYS block, even in advisory.
  const r3MissingAdvisoryExit = await runExit({ oracles: R3_ORACLE, captures: [], args: ['--gate'], env: { SCREENSHOT_PIXEL_GATE: 'advisory' } })
  check('R3 strict: a gross pixel divergence BLOCKS (exit 2)', () => {
    assert.equal(r3StrictExit, 2)
    assert.equal(r3Strict.report.overall, 'BLOCKER')
    assert.equal(r3Strict.report.thresholds.pixelGate, 'strict')
  })
  check('R3 advisory: the SAME divergence is a WARN, never blocks (exit 0) — 3-frame evidence still shown', () => {
    assert.equal(r3AdvisoryExit, 0)
    assert.equal(r3Advisory.report.overall, 'WARN')
    assert.equal(r3Advisory.report.thresholds.pixelGate, 'advisory')
    const row = r3Advisory.report.results[0]
    assert.notEqual(row.status, 'BLOCKER', 'row status routed off BLOCKER so the report is self-consistent')
    assert.ok(row.pixelStatus, 'raw pixel verdict preserved in pixelStatus for display')
    assert.ok(!r3Advisory.report.issues.some((i) => String(i.severity).toUpperCase() === 'BLOCKER'), 'no BLOCKER issue in advisory')
    assert.ok(r3Advisory.report.issues.some((i) => /^WARN/i.test(String(i.severity))), 'divergence surfaced as WARN')
    assert.ok(row.artifactSet && row.artifactSet.artifacts && row.artifactSet.artifacts.diff, 'diff artifact still written')
  })
  check('R3 off: the SAME divergence is suppressed (exit 0, PASS) but the artifacts are still computed + shown', () => {
    assert.equal(r3Off.report.overall, 'PASS')
    const row = r3Off.report.results[0]
    assert.equal(row.status, 'PASS')
    assert.ok(row.pixelStatus, 'raw pixel verdict still recorded for display')
    assert.ok(!r3Off.report.issues.some((i) => String(i.issueKind || '').startsWith('SSIM') || String(i.issueKind || '').startsWith('ZONE_SSIM') || String(i.issueKind || '').startsWith('COLOR_ONLY')), 'no pixel-similarity issue emitted in off mode')
    assert.ok(row.artifactSet && row.artifactSet.artifacts && row.artifactSet.artifacts.diff, 'diff artifact still written (compute + show)')
  })
  check('R3 fail-closed: completeness/anti-forgery ALWAYS blocks — advisory does not launder a missing capture', () => {
    assert.notEqual(r3MissingAdvisoryExit, 0)
  })

  // --- R2-3: tighten-only per-task `- gate: strict` bullet ---
  // A task whose ## Design carries `- gate: strict` forces strict routing even when the env
  // says advisory (max()-only: the weakening direction has no grammar — design-parser blocks
  // any other gate value as a malformed design, pinned in check-screen-cache.test).
  const gateWs = mkdtempSync(join(tmpdir(), 'cmp-gate-'))
  const gateTaskFile = join(gateWs, 'task.md')
  writeFileSync(gateTaskFile, '# T\n\n## Design\n\n- Home — https://www.figma.com/design/fileKey?node-id=1-2\n- gate: strict\n')
  const r23 = await run({ oracles: R3_ORACLE, captures: R3_CAPTURE, env: { SCREENSHOT_PIXEL_GATE: 'advisory', FIGMA_SCREEN_TASK_FILE: gateTaskFile } })
  check('R2-3: `- gate: strict` in the task overrides an advisory env — divergence BLOCKS, strict recorded', () => {
    assert.equal(r23.report.overall, 'BLOCKER')
    assert.equal(r23.report.thresholds.pixelGate, 'strict')
    assert.ok(r23.report.results[0].status === 'BLOCKER' || r23.report.results[0].status === 'MAJOR', 'row keeps its raw strict verdict')
  })
  rmSync(gateWs, { recursive: true, force: true })

  const unavailableTaskWs = mkdtempSync(join(tmpdir(), 'cmp-task-unavailable-'))
  try {
    const unavailableTask = join(unavailableTaskWs, 'task.md')
    writeFileSync(unavailableTask, Buffer.from([0xff, 0xfe, 0xfd]))
    const unavailableTaskRun = spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--gate'], {
      env: { ...process.env, FIGMA_SCREEN_TASK_FILE: unavailableTask },
      encoding: 'utf8'
    })
    check('R2-3: an unreadable task source fails setup instead of silently dropping a strict override', () => {
      assert.equal(unavailableTaskRun.status, 1)
      assert.match(unavailableTaskRun.stderr, /current task Design source is missing, unreadable, unsafe, oversized, or not valid UTF-8/)
    })
  } finally {
    rmSync(unavailableTaskWs, { recursive: true, force: true })
  }

  // --- R2-1: classed component rows route similarity verdicts to REVIEW_REQUIRED (strict) ---
  // The same gross divergence that BLOCKS a declarative row goes to OWNER review when the
  // index node is component-kind AND the ACTIVE mapping's visualPolicy classes it canvas/glass
  // (the CAS mapping registry is the routing source; rows join only on the owning node id).
  const rcWs = mkdtempSync(join(tmpdir(), 'cmp-rc-'))
  const rcInventoryFile = join(rcWs, 'design-component-inventory.json')
  const rcMappingsFile = join(rcWs, 'component-mappings.json')
  const writeRcTruth = ({ nodeId = '10:1', renderClass = 'canvas' } = {}) => {
    const capture = validComponentCapture()
    capture.entities[0].nodeId = nodeId
    capture.entities[0].name = 'Home'
    for (const visual of capture.visual) if (visual.entityNodeId === '10:1') visual.entityNodeId = nodeId
    const inventory = normalizeComponentCapture(capture, COMPONENT_CAPTURE_HASH)
    const component = inventory.components.find((row) => row.providerIdentity.nodeId === nodeId)
    writeFileSync(rcInventoryFile, JSON.stringify(inventory, null, 2))
    writeFileSync(rcMappingsFile, JSON.stringify({
      schemaVersion: 2, revision: 1, designScopeId: inventory.scopeId,
      mappings: [{
        mappingId: 'cmap-' + 'a'.repeat(24), designComponentId: component.designComponentId,
        expectedKind: component.kind, state: 'active',
        implementations: [{ adapterId: 'compose-ds', platform: 'android-compose',
          projectScopeFingerprint: 'sha256:' + 'b'.repeat(64), relation: 'direct',
          projectComponentIds: ['compose-ds:symbol:ds.Home'], required: true }],
        propertyMappings: [], slotMappings: [],
        ...(renderClass ? { visualPolicy: { renderClass, by: 'owner', at: '2026-01-01T00:00:00.000Z' } } : {}),
        provenance: { kind: 'user-confirmed', actor: 'owner', at: '2026-01-01T00:00:00.000Z' }
      }],
      dispositions: [],
    }, null, 2))
  }
  const rcEnvFor = () => ({ fixtureComponentInventory: rcInventoryFile, fixtureComponentMappings: rcMappingsFile })
  const rcNodes = { Home: { kind: 'component', nodeId: '10:1' } }
  writeRcTruth()
  const r21 = await run({ oracles: R3_ORACLE, captures: R3_CAPTURE, indexNodes: rcNodes, env: { SCREENSHOT_PIXEL_GATE: 'strict', ...rcEnvFor() } })
  const r21Exit = await runExit({ oracles: R3_ORACLE, captures: R3_CAPTURE, args: ['--gate'], indexNodes: rcNodes, env: { SCREENSHOT_PIXEL_GATE: 'strict', ...rcEnvFor() } })
  check('R2-1: classed component row → REVIEW_REQUIRED (raw verdict kept, class recorded, non-shippable exit)', () => {
    assert.equal(r21.report.overall, 'REVIEW_REQUIRED')
    const row = r21.report.results[0]
    assert.equal(row.status, 'REVIEW_REQUIRED')
    assert.ok(['BLOCKER', 'MAJOR', 'MINOR'].includes(row.pixelStatus), 'raw pixel verdict preserved')
    assert.equal(row.renderClass, 'canvas')
    assert.deepEqual(r21.report.classRouting, { Home: 'canvas' })
    assert.ok(!r21.report.issues.some((i) => String(i.severity).toUpperCase() === 'BLOCKER'), 'no BLOCKER similarity issue survives routing')
    assert.ok(r21.report.issues.some((i) => i.issueKind === 'PIXEL_REVIEW_REQUIRED' && i.pixelKind), 'similarity issues rewritten to PIXEL_REVIEW_REQUIRED with the raw kind kept')
    assert.equal(r21Exit, 2, 'gate mode exits non-zero — review is NON-shippable until the owner decides')
  })
  writeRcTruth({ renderClass: null })
  const r21NoClass = await run({ oracles: R3_ORACLE, captures: R3_CAPTURE, indexNodes: rcNodes, env: { SCREENSHOT_PIXEL_GATE: 'strict', ...rcEnvFor() } })
  check('R2-1 control: unclassed component row keeps the strict BLOCKER (empty classRouting)', () => {
    assert.equal(r21NoClass.report.overall, 'BLOCKER')
    assert.equal(r21NoClass.report.results[0].status, 'BLOCKER')
    assert.deepEqual(r21NoClass.report.classRouting, {})
  })
  writeRcTruth({ nodeId: '9:9' })
  const r21WrongId = await run({ oracles: R3_ORACLE, captures: R3_CAPTURE, indexNodes: rcNodes, env: { SCREENSHOT_PIXEL_GATE: 'strict', ...rcEnvFor() } })
  check('R2-1 identity: a class on another owning node id never routes the row', () => {
    assert.equal(r21WrongId.report.overall, 'BLOCKER')
    assert.deepEqual(r21WrongId.report.classRouting, {})
  })
  // Completeness/anti-forgery can never route: a MISSING capture on a CLASSED row still blocks.
  writeRcTruth()
  const r21MissingExit = await runExit({ oracles: R3_ORACLE, captures: [], args: ['--gate'], indexNodes: rcNodes, env: { SCREENSHOT_PIXEL_GATE: 'strict', ...rcEnvFor() } })
  check('R2-1 fail-closed: MISSING_CAPTURE on a classed row is NOT routable to review', () => {
    assert.notEqual(r21MissingExit, 0)
  })
  // A malformed routing source fails closed instead of silently reading strict.
  writeFileSync(rcMappingsFile, '{broken')
  const r21Broken = await run({ oracles: R3_ORACLE, captures: R3_CAPTURE, indexNodes: rcNodes, env: { SCREENSHOT_PIXEL_GATE: 'strict', ...rcEnvFor() } })
  check('R2-1 fail-closed: a malformed mapping registry is a RENDER_CLASS_SOURCE_INVALID blocker', () => {
    assert.ok(r21Broken.report.issues.some((i) => i.issueKind === 'RENDER_CLASS_SOURCE_INVALID' && i.severity === 'BLOCKER'),
      JSON.stringify(r21Broken.report.issues.map((i) => i.issueKind)))
  })
  rmSync(rcWs, { recursive: true, force: true })

  // --- masked-ssim-luma-v2 probes ---
  // A 60x80 rect shifted by 3px: a perceptually-invisible offset that a pixel-aligned
  // metric penalises. The shift search should recover it.
  const shiftBox = (s) => [[30 + s, 80 + s, 90 + s, 160 + s]]
  const sShiftV2 = await run({ oracles: [{ name: 'Home.png', w: 120, h: 240, boxes: shiftBox(0) }], captures: [{ name: 'HomeScreenshot.png', w: 120, h: 240, boxes: shiftBox(2) }] })
  check('tamper: capture byte-identical to oracle → CAPTURE_IS_ORACLE_COPY blocker', () => {
    assert.equal(sOracleCopy.report.results[0].status, 'CAPTURE_IS_ORACLE_COPY')
    assert.ok(sOracleCopy.report.issues.some((i) => i.issueKind === 'CAPTURE_IS_ORACLE_COPY' && i.severity === 'BLOCKER'))
  })
  check('tamper: manifest path outside Roborazzi dirs → CAPTURE_PATH_UNCONTAINED blocker', () => {
    assert.equal(sUncontained.report.results[0].status, 'CAPTURE_PATH_UNCONTAINED')
    assert.ok(sUncontained.report.issues.some((i) => i.issueKind === 'CAPTURE_PATH_UNCONTAINED' && i.severity === 'BLOCKER'))
  })
  check('primary: identical → PASS', () => { assert.match(sIdentical.stdout, /Home \[primary\]: PASS/); assert.equal(sIdentical.report.overall, 'PASS') })
  check('v2: report carries luma-v2 metric + per-region scores + alignment', () => {
    assert.equal(sIdentical.report.metric, 'masked-ssim-luma-v2')
    const row = sIdentical.report.results[0]
    assert.equal(row.metric, 'masked-ssim-luma-v2')
    assert.ok(Array.isArray(row.regions) && row.regions.length >= 1, 'regions populated')
    assert.ok(typeof row.worstRegion === 'number')
    assert.ok(row.alignment && typeof row.alignment.dx === 'number' && typeof row.alignment.dy === 'number')
  })
  check('v2: ±2px sub-pixel shift is recovered to PASS', () => {
    const v2 = sShiftV2.report.results[0]
    assert.equal(v2.status, 'PASS', `v2 should recover the 2px shift, got ${v2.status} (${v2.score})`)
    assert.ok(v2.alignment.dx !== 0 || v2.alignment.dy !== 0, 'v2 found a non-zero alignment offset')
  })
  // Direction B end-to-end: a near-luma-preserving chroma drift — structure stays high,
  // the colour axis flags it. Oracle grey block vs a chroma-shifted block of ~equal luma.
  const sColorDrift = await run({
    oracles: [{ name: 'Home.png', w: 120, h: 240, boxes: [[30, 80, 90, 160, 128, 128, 128]] }],
    captures: [{ name: 'HomeScreenshot.png', w: 120, h: 240, boxes: [[30, 80, 90, 160, 150, 118, 130]] }],
  })
  const cdr = sColorDrift.report.results[0]
  check('v2 colour axis (B): a near-luma chroma drift passes structure but ΔE flags it', () => {
    assert.equal(cdr.status, 'PASS', 'structure unaffected by a luma-preserving chroma shift')
    assert.equal(cdr.colorStatus, 'REVIEW')
    assert.ok(cdr.worstRegionDeltaE > 3, `worst ΔE ${cdr.worstRegionDeltaE} should exceed DE_PASS=3`)
    assert.ok(cdr.regions.some((r) => r.deltaE > 3), 'a region carries an elevated ΔE')
  })
  check('#16: colour REVIEW pushes a WARN COLOR_DRIFT_REVIEW issue → overall WARN (never a blocker)', () => {
    const ci = sColorDrift.report.issues.find((i) => i.issueKind === 'COLOR_DRIFT_REVIEW')
    assert.ok(ci, 'COLOR_DRIFT_REVIEW issue present')
    assert.equal(ci.severity, 'WARN')
    assert.match(ci.message, /ΔE00/)
    assert.match(ci.message, /SSIM/)
    assert.equal(sColorDrift.report.overall, 'WARN', 'chroma drift can no longer ship as a clean PASS')
    assert.equal(cdr.status, 'PASS', 'row status (score) itself stays untouched')
  })
  check('v2 colour axis (B): identical pair is colour-clean + thresholds reported', () => {
    assert.equal(sIdentical.report.results[0].colorStatus, 'PASS')
    assert.ok(sIdentical.report.results[0].worstRegionDeltaE < 0.5)
    assert.equal(sIdentical.report.thresholds.deltaEPass, 3)
    assert.equal(sIdentical.report.thresholds.colorAxis, true)
    assert.ok(!sIdentical.report.issues.some((i) => i.issueKind === 'COLOR_DRIFT_REVIEW'), 'no drift issue on a clean pair')
  })
  check('metric-internal knobs recorded in thresholds at canonical defaults (evidence-bundle canon-checks them at final)', () => {
    // The proven softened-judge bypass (SHIFT_RADIUS=4 + GAUSSIAN_SIGMA=3 + AA_TOLERANCE=3 →
    // BLOCKER 0.395 became PASS 1.000; MASK_MODE=color / VAR_FLOOR=65025 hid deleted panels)
    // is closed by recording the knobs here and failing the FINAL bundle on any non-canon value.
    const t = sIdentical.report.thresholds
    assert.equal(t.shiftRadius, 2)
    assert.equal(t.gaussianSigma, 1.5)
    assert.equal(t.aaTolerance, 1)
    assert.equal(t.varFloor, 12)
    assert.equal(t.maskMode, 'variance')
    assert.equal(t.deltaEStride, 2)
    assert.equal(t.regionGrid, '8x4')
  })
  // C1-conservative: the ORACLE-side mask cannot score content the RENDER adds over the
  // oracle's background — a small oracle rect + a capture with a big bright box in
  // oracle-background space keeps structure at PASS (the proven false-PASS) while the
  // unmasked-divergence probe must flag it (RENDER_EXTRA_CONTENT), band-routed.
  const extraOracle = { name: 'Home.png', w: 120, h: 240, boxes: [[30, 30, 60, 60]] }
  const extraCapture = { name: 'HomeScreenshot.png', w: 120, h: 240, boxes: [[30, 30, 60, 60], [10, 100, 110, 220]] }
  const sExtra = await run({ oracles: [extraOracle], captures: [extraCapture] })
  const sExtraOff = await run({ oracles: [extraOracle], captures: [extraCapture], env: { SCREENSHOT_EXTRA_CONTENT_BAND: 'off' } })
  const sExtraBlockExit = await runExit({ oracles: [extraOracle], captures: [extraCapture], args: ['--gate'], env: { SCREENSHOT_EXTRA_CONTENT_BAND: 'block' } })
  check('C1: render-added content over oracle background → WARN RENDER_EXTRA_CONTENT (score/status stay PASS)', () => {
    const row = sExtra.report.results[0]
    assert.equal(row.status, 'PASS', 'oracle-side mask still scores structure as PASS — the false-PASS this probe witnesses')
    assert.ok(typeof row.extraContentFraction === 'number' && row.extraContentFraction > 0.02, `fraction ${row.extraContentFraction} must exceed the 0.02 default floor`)
    const ei = sExtra.report.issues.find((i) => i.issueKind === 'RENDER_EXTRA_CONTENT')
    assert.ok(ei, 'RENDER_EXTRA_CONTENT issue present')
    assert.equal(ei.severity, 'WARN')
    assert.match(ei.message, /oracle-background/)
    assert.equal(sExtra.report.overall, 'WARN', 'extra content can no longer ship as a clean PASS')
    assert.equal(sExtra.report.thresholds.extraContentBand, 'warn')
    assert.ok(sIdentical.report.results[0].extraContentFraction < 0.02, 'identical pair stays clean')
    assert.ok(!sIdentical.report.issues.some((i) => i.issueKind === 'RENDER_EXTRA_CONTENT'), 'no issue on a clean pair')
  })
  check('C1: SCREENSHOT_EXTRA_CONTENT_BAND=off silences the issue (fraction still recorded); block → gate exit 2', () => {
    assert.ok(!sExtraOff.report.issues.some((i) => i.issueKind === 'RENDER_EXTRA_CONTENT'), 'off band pushes no issue')
    assert.equal(sExtraOff.report.overall, 'PASS')
    assert.ok(sExtraOff.report.results[0].extraContentFraction > 0.02, 'probe field recorded even when off (calibration runs)')
    assert.equal(sExtraBlockExit, 2, 'block band promotes to BLOCKER → gate exit 2')
  })
  // C1 equal-luma residual: the render adds a wrong-HUE region whose LUMINANCE matches the
  // oracle background — dark red (40,4,10) vs the 0x101010 bg: luma 15.45 vs 16 (|Δ|=0.55,
  // invisible to the luma arm's >24 floor) while ΔE00 = 16.7. The probe's chroma arm
  // (RGB pre-filter 756 > 24², then ΔE00 > SCREENSHOT_EXTRA_CONTENT_DELTAE=10) is the ONLY
  // witness: the mask excludes the region from `score` and the ΔE axis samples only inside it.
  const chromaCapture = { name: 'HomeScreenshot.png', w: 120, h: 240, boxes: [[30, 30, 60, 60], [10, 100, 110, 220, 40, 4, 10]] }
  const sExtraChroma = await run({ oracles: [extraOracle], captures: [chromaCapture] })
  const sExtraChromaRaised = await run({ oracles: [extraOracle], captures: [chromaCapture], env: { SCREENSHOT_EXTRA_CONTENT_DELTAE: '100' } })
  check('C1 equal-luma: wrong-colour region with matched luma in unmasked space → RENDER_EXTRA_CONTENT', () => {
    const row = sExtraChroma.report.results[0]
    assert.equal(row.status, 'PASS', 'structure inside the mask is untouched — the false-PASS the chroma arm witnesses')
    assert.ok(typeof row.extraContentFraction === 'number' && row.extraContentFraction > 0.02, `fraction ${row.extraContentFraction} must exceed the 0.02 floor via the chroma arm`)
    assert.ok(sExtraChroma.report.issues.some((i) => i.issueKind === 'RENDER_EXTRA_CONTENT'), 'RENDER_EXTRA_CONTENT issue present')
    assert.equal(sExtraChroma.report.thresholds.extraContentDeltaE, 10, 'ΔE floor recorded in thresholds')
    assert.ok(sIdentical.report.results[0].extraContentFraction < 0.02, 'identical pair stays clean under the extended predicate')
    assert.ok(!sIdentical.report.issues.some((i) => i.issueKind === 'RENDER_EXTRA_CONTENT'), 'no issue on a clean pair')
  })
  check('C1 equal-luma: a raised SCREENSHOT_EXTRA_CONTENT_DELTAE blinds the chroma arm (knob is wired; final gate rejects it)', () => {
    assert.ok(sExtraChromaRaised.report.results[0].extraContentFraction < 0.02, 'ΔE 16.7 < raised floor 100 → not counted')
    assert.ok(!sExtraChromaRaised.report.issues.some((i) => i.issueKind === 'RENDER_EXTRA_CONTENT'), 'no issue at the raised floor')
    assert.equal(sExtraChromaRaised.report.thresholds.extraContentDeltaE, 100, 'weakened floor recorded — evidence-bundle THRESHOLDS_WEAKENED catches it at final')
  })
  // Per-Figma-node zones (C1): two declared nodes (top button, bottom card); the render
  // keeps the top block and drops the bottom one. The diverged element must be isolated.
  const sNodes = await run({
    oracles: [{ name: 'Home.png', w: 120, h: 240, boxes: [[30, 30, 90, 80], [30, 150, 90, 200]] }],
    captures: [{ name: 'HomeScreenshot.png', w: 120, h: 240, boxes: [[30, 30, 90, 80]] }],
    args: ['--semantic'],
    specs: [{ name: 'Home.spec.json', data: { screen: 'Home', theme: 'light', frameSizeDp: { w: 120, h: 240 }, elements: [
      { stableId: 'topBtn', name: 'Top', role: 'button', bboxDp: { x: 30, y: 30, w: 60, h: 50 } },
      { stableId: 'bottomCard', name: 'Bottom', role: 'card', bboxDp: { x: 30, y: 150, w: 60, h: 50 } },
    ] } }],
  })
  check('v2 per-node zones (C1): SSIM scored per Figma element; the diverged node is isolated', () => {
    const row = sNodes.report.results[0]
    assert.ok(Array.isArray(row.zones) && row.zones.length === 2, 'two node zones present')
    const top = row.zones.find((z) => z.stableId === 'topBtn'), bottom = row.zones.find((z) => z.stableId === 'bottomCard')
    assert.ok(top && bottom, 'both declared nodes scored')
    assert.equal(top.role, 'button')
    assert.ok(top.ssim > bottom.ssim + 0.2, `matching node ${top.ssim} must beat the diverged node ${bottom.ssim}`)
    assert.ok(top.bboxPx && top.bboxPx.w === 60 && top.bboxPx.h === 50, 'bbox projected to px')
  })
  check('v2 per-node zones (C1): --semantic aggregates node zones + flags the bad element', () => {
    assert.equal(sNodes.report.semantic.metric, 'semantic-node-ssim-v1')
    assert.ok(sNodes.report.semantic.zones.length >= 2)
    assert.ok(sNodes.report.semantic.findings.some((f) => f.stableId === 'bottomCard' && f.status !== 'PASS'), 'diverged node flagged')
  })
  // #50: elements projecting (mostly) outside the frame — canvas-absolute coords — must be
  // flagged (WARN ZONE_OFF_FRAME) instead of silently vanishing from zone scoring.
  const offBoxes = [[30, 30, 90, 80]]
  const sOffFrame = await run({
    oracles: [{ name: 'Home.png', w: 120, h: 240, boxes: offBoxes }],
    captures: [{ name: 'HomeScreenshot.png', w: 120, h: 240, boxes: offBoxes }],
    specs: [{ name: 'Home.spec.json', data: { screen: 'Home', theme: 'light', frameSizeDp: { w: 120, h: 240 }, elements: [
      { stableId: 'topBtn', bboxDp: { x: 30, y: 30, w: 60, h: 50 } },
      { stableId: 'ghostBtn', bboxDp: { x: 2000, y: 30, w: 60, h: 50 } },   // fully off-frame (absolute coords)
      { stableId: 'edgeBtn', bboxDp: { x: 100, y: 30, w: 60, h: 50 } },     // 33% in-frame (<50%)
    ] } }],
  })
  // #54: row.zones is capped at the 24 worst so a many-node spec cannot bloat the report.
  const manyBoxes = [], manyEls = []
  for (let i = 0; i < 30; i++) {
    // by=28+row*30 keeps every row clear of the R1 device-chrome bands (top 24px status / bottom
    // 36px nav on this 120x240dp frame) so all 30 zones are scored — the cap, not the mask, bounds it.
    const bx = 4 + (i % 5) * 24, by = 28 + ((i / 5) | 0) * 30
    manyBoxes.push([bx, by, bx + 16, by + 16])
    manyEls.push({ stableId: `el${i}`, bboxDp: { x: bx, y: by, w: 16, h: 16 } })
  }
  const sManyZones = await run({
    oracles: [{ name: 'Home.png', w: 120, h: 240, boxes: manyBoxes }],
    captures: [{ name: 'HomeScreenshot.png', w: 120, h: 240, boxes: manyBoxes }],
    specs: [{ name: 'Home.spec.json', data: { screen: 'Home', theme: 'light', frameSizeDp: { w: 120, h: 240 }, elements: manyEls } }],
  })
  check('#54: row.zones capped at the 24 worst + zonesTruncated (report stays bounded)', () => {
    const row = sManyZones.report.results[0]
    assert.equal(row.zones.length, 24, '30 declared zones capped to 24')
    assert.equal(row.zonesTruncated, 6)
    for (let i = 1; i < row.zones.length; i++) assert.ok(row.zones[i - 1].ssim <= row.zones[i].ssim, 'kept zones are the worst (ssim-asc)')
    const few = sNodes.report.results[0]
    assert.equal(few.zonesTruncated, undefined, 'no truncation marker when under the cap')
  })
  check('#50: off-frame spec elements → WARN ZONE_OFF_FRAME listing the offenders (not a silent drop)', () => {
    const zi = sOffFrame.report.issues.find((i) => i.issueKind === 'ZONE_OFF_FRAME')
    assert.ok(zi, 'ZONE_OFF_FRAME issue present')
    assert.equal(zi.severity, 'WARN')
    assert.match(zi.message, /ghostBtn/)
    assert.match(zi.message, /edgeBtn/)
    assert.doesNotMatch(zi.message, /topBtn/, 'fully in-frame element not flagged')
    const row = sOffFrame.report.results[0]
    assert.ok(Array.isArray(row.zones) && row.zones.some((z) => z.stableId === 'topBtn'), 'in-frame zone still scored')
    assert.ok(!row.zones.some((z) => z.stableId === 'ghostBtn'), 'fully off-frame element yields no zone')
    assert.equal(sOffFrame.report.overall, 'WARN')
  })

  // ── H3 zone-gate (default ON): "структуру строже, текст мягче" ─────────────────────────────
  // A large mid-grey block dominates the frame (global mean stays in the PASS band ~0.93) while
  // one small declared element (24x24 = 576px content, >= MIN_REGION_PX) diverges locally
  // (dimmed white→grey). The content-weighted global SSIM would otherwise average the local
  // break away — the zone gate is the only witness. Same DIVERGED pixels in every case; only the
  // spec `role`/`textStyle` (→ isText) changes, so these isolate the per-zone floor by kind.
  const zoneBg = [[4, 60, 116, 200, 90, 90, 90]]          // large matching block — keeps the global mean PASS (kept above the R1 nav band at y204)
  // The probed element sits at y30 — below the R1 top status band (24px on this 120x240dp frame)
  // so it is fully scored: this now doubles as proof the zone gate still fires for legitimately
  // placed content post-R1 (only genuine under-status-bar pixels are excluded).
  const zoneOracleEl = [10, 30, 34, 54, 255, 255, 255]     // 24x24 white element present in the oracle
  const zoneDim64 = [10, 30, 34, 54, 64, 64, 64]           // dimmed in the render → zone SSIM ~0.30 (in [0.25,0.35))
  const zoneShift6 = [16, 36, 40, 60, 255, 255, 255]       // shifted+resized → zone SSIM ~0.15 (< 0.25, grossly wrong)
  const zoneBgEl = { stableId: 'bg', role: 'container', bboxDp: { x: 4, y: 60, w: 112, h: 176 } }
  const zoneSpec = (el) => [{ name: 'Home.spec.json', data: { screen: 'Home', theme: 'light', frameSizeDp: { w: 120, h: 240 }, elements: [el, zoneBgEl] } }]
  // (a) NON-TEXT element (role 'icon', no textStyle), local SSIM ~0.30 < the strict 0.35 floor →
  //     ZONE_SSIM_BLOCKER fires even though the global mean would PASS. Zone gate is ON by default.
  const zNonTextExit = await runExit({
    oracles: [{ name: 'Home.png', w: 120, h: 240, boxes: [...zoneBg, zoneOracleEl] }],
    captures: [{ name: 'HomeScreenshot.png', w: 120, h: 240, boxes: [...zoneBg, zoneDim64] }],
    args: ['--gate'],
    specs: zoneSpec({ stableId: 'iconEl', role: 'icon', bboxDp: { x: 10, y: 30, w: 24, h: 24 } }),
  })
  // Advisory run of the SAME non-text case, to inspect the report (runExit only yields the code).
  const zNonText = await run({
    oracles: [{ name: 'Home.png', w: 120, h: 240, boxes: [...zoneBg, zoneOracleEl] }],
    captures: [{ name: 'HomeScreenshot.png', w: 120, h: 240, boxes: [...zoneBg, zoneDim64] }],
    specs: zoneSpec({ stableId: 'iconEl', role: 'icon', bboxDp: { x: 10, y: 30, w: 24, h: 24 } }),
  })
  check('zone-gate (a): a localized NON-TEXT divergence the global mean hides → ZONE_SSIM_BLOCKER (gate ON by default, exit 2)', () => {
    assert.equal(zNonTextExit, 2, 'zone gate is ON by default → the localized non-text break fails the screen')
    const row = zNonText.report.results[0]
    assert.equal(row.status, 'BLOCKER')
    assert.ok(typeof row.score === 'number' && row.score >= 0.90, `global mean ${row.score} is in the PASS band — the dilution the zone gate catches`)
    assert.ok(row.zoneFloorHit && row.zoneFloorHit.label === 'iconEl' && row.zoneFloorHit.isText === false, 'the non-text icon zone is the offender')
    assert.equal(row.zoneFloorHit.threshold, 0.35, 'non-text zone judged against the strict 0.35 floor')
    const zi = zNonText.report.issues.find((i) => i.issueKind === 'ZONE_SSIM_BLOCKER')
    assert.ok(zi && zi.severity === 'BLOCKER', 'ZONE_SSIM_BLOCKER present')
    assert.match(zi.message, /localized zone/)
  })
  // (b) TEXT element (role 'label'), the SAME local SSIM ~0.30 → the lenient 0.25 text floor lets
  //     it through (Robolectric font-AA ≠ Figma). Text structure is covered by spec-compare + ΔE.
  const zTextLenient = await run({
    oracles: [{ name: 'Home.png', w: 120, h: 240, boxes: [...zoneBg, zoneOracleEl] }],
    captures: [{ name: 'HomeScreenshot.png', w: 120, h: 240, boxes: [...zoneBg, zoneDim64] }],
    args: ['--gate'],
    specs: zoneSpec({ stableId: 'labelEl', role: 'label', bboxDp: { x: 10, y: 30, w: 24, h: 24 } }),
  })
  // (b2) TEXT via textStyle (role 'chip' — not a textish role) proves the textStyle arm of isText.
  const zTextStyleLenient = await run({
    oracles: [{ name: 'Home.png', w: 120, h: 240, boxes: [...zoneBg, zoneOracleEl] }],
    captures: [{ name: 'HomeScreenshot.png', w: 120, h: 240, boxes: [...zoneBg, zoneDim64] }],
    args: ['--gate'],
    specs: zoneSpec({ stableId: 'chipEl', role: 'chip', textStyle: { fontSize: 14 }, bboxDp: { x: 10, y: 30, w: 24, h: 24 } }),
  })
  check('zone-gate (b): the SAME divergence in a TEXT zone does NOT block — lenient 0.25 text floor', () => {
    const row = zTextLenient.report.results[0]
    assert.equal(row.status, 'PASS', 'a text zone at SSIM ~0.30 clears the lenient 0.25 floor')
    assert.ok(!zTextLenient.report.issues.some((i) => i.issueKind === 'ZONE_SSIM_BLOCKER'), 'no zone blocker for the text zone')
    const z = row.zones.find((z) => z.stableId === 'labelEl')
    assert.ok(z && z.isText === true, 'the label zone is classified isText')
    assert.ok(z.ssim < 0.35 && z.ssim > 0.25, `zone SSIM ${z.ssim} sits between the text (0.25) and non-text (0.35) floors — the exact leniency band`)
    // Gate exit is 0 for this screen (the color axis pushes an advisory WARN, never a blocker).
    assert.notEqual(zTextLenient.report.overall, 'BLOCKER', 'text-only leniency keeps the screen shippable')
    // textStyle arm: a non-textish role still classifies as text via textStyle → same leniency.
    const z2 = zTextStyleLenient.report.results[0].zones.find((z) => z.stableId === 'chipEl')
    assert.ok(z2 && z2.isText === true, 'textStyle marks the element as text even with a non-textish role')
    assert.equal(zTextStyleLenient.report.results[0].status, 'PASS')
  })
  // (c) TEXT element grossly wrong (SSIM ~0.15 < the lenient 0.25 floor) → STILL blocks. Leniency
  //     is only font-AA slack, not a licence to drop/mangle text entirely.
  const zTextGrossExit = await runExit({
    oracles: [{ name: 'Home.png', w: 120, h: 240, boxes: [...zoneBg, zoneOracleEl] }],
    captures: [{ name: 'HomeScreenshot.png', w: 120, h: 240, boxes: [...zoneBg, zoneShift6] }],
    args: ['--gate'],
    specs: zoneSpec({ stableId: 'labelEl', role: 'label', bboxDp: { x: 10, y: 30, w: 24, h: 24 } }),
  })
  const zTextGross = await run({
    oracles: [{ name: 'Home.png', w: 120, h: 240, boxes: [...zoneBg, zoneOracleEl] }],
    captures: [{ name: 'HomeScreenshot.png', w: 120, h: 240, boxes: [...zoneBg, zoneShift6] }],
    specs: zoneSpec({ stableId: 'labelEl', role: 'label', bboxDp: { x: 10, y: 30, w: 24, h: 24 } }),
  })
  check('zone-gate (c): a GROSSLY-wrong text zone (SSIM below the lenient floor) still blocks', () => {
    assert.equal(zTextGrossExit, 2, 'text below the 0.25 lenient floor is structurally gone/wrong → still fails')
    const row = zTextGross.report.results[0]
    assert.equal(row.status, 'BLOCKER')
    assert.ok(row.zoneFloorHit && row.zoneFloorHit.isText === true, 'the text zone is the offender')
    assert.equal(row.zoneFloorHit.threshold, 0.25, 'judged against the lenient text floor — and still trips it')
    const zi = zTextGross.report.issues.find((i) => i.issueKind === 'ZONE_SSIM_BLOCKER')
    assert.ok(zi && /localized text zone/.test(zi.message), 'message names it a text zone')
  })
  // Rollback: SCREENSHOT_ZONE_GATE=0 restores the pre-default-on behaviour (no zone blocker even
  // for a stark local non-text break); the knob stays for tuning/rollback.
  const zGateOff = await run({
    oracles: [{ name: 'Home.png', w: 120, h: 240, boxes: [...zoneBg, zoneOracleEl] }],
    captures: [{ name: 'HomeScreenshot.png', w: 120, h: 240, boxes: [...zoneBg, zoneDim64] }],
    specs: zoneSpec({ stableId: 'iconEl', role: 'icon', bboxDp: { x: 10, y: 30, w: 24, h: 24 } }),
    env: { SCREENSHOT_ZONE_GATE: '0' },
  })
  check('zone-gate rollback: SCREENSHOT_ZONE_GATE=0 disables the floor (the non-text break no longer blocks)', () => {
    const row = zGateOff.report.results[0]
    assert.equal(row.status, 'PASS', 'gate off → the localized break is averaged into the PASS global mean again')
    assert.ok(!zGateOff.report.issues.some((i) => i.issueKind === 'ZONE_SSIM_BLOCKER'))
    assert.equal(zGateOff.report.thresholds.zoneGate, false, 'the disabled gate is recorded in thresholds')
  })
  // Thresholds record: the zone knobs (incl. the new zoneTextBlocker) flow into the report so the
  // final evidence bundle can canon-check them. Default gate ON.
  check('zone-gate: thresholds record zoneGate:true + zoneBlocker/zoneTextBlocker/minRegionPx (canon-checked at final)', () => {
    const t = sIdentical.report.thresholds
    assert.equal(t.zoneGate, true, 'zone gate ships ON by default')
    assert.equal(t.zoneBlocker, 0.35)
    assert.equal(t.zoneTextBlocker, 0.25)
    assert.equal(t.minRegionPx, 400)
  })

  check('artifacts: PASS comparison writes figma/actual/diff/overlay PNGs and manifest', () => {
    const files = sIdentical.artifactFiles.map((f) => f.path)
    assert.ok(files.find((p) => /\/figma\.png$/.test(p)))
    assert.ok(files.find((p) => /\/actual\.png$/.test(p)))
    assert.ok(files.find((p) => /\/diff\.png$/.test(p)))
    assert.ok(files.find((p) => /\/overlay\.png$/.test(p)))
    assert.ok(files.find((p) => /\/manifest\.json$/.test(p)))
    const row = sIdentical.report.results[0]
    assert.equal(row.artifactSet.artifacts.figma.kind, 'figma')
    assert.match(row.artifactSet.artifacts.diff.hash, /^sha256:/)
    assert.equal(sIdentical.report.artifactSet.entries.length, 4)
  })
  check('artifacts: long canonical stem + auto run id retain full identities and use bounded paths', () => {
    assert.equal(sLongIdentity.report.taskStem, longStem)
    assert.equal(longStem.length, 86)
    assert.ok(sLongIdentity.report.pipelineRunId.length > 80, 'auto run id must exercise the long-segment path')
    const ref = sLongIdentity.report.results[0].artifactSet.artifacts.figma
    const parts = ref.path.split('/')
    assert.equal(parts[2], artifactSegment(longStem))
    assert.equal(parts[3], artifactSegment(sLongIdentity.report.pipelineRunId))
    assert.equal(parts[2].length, 80)
    assert.equal(parts[3].length, 80)
    assert.ok(sLongIdentity.artifactFiles.some((file) => file.path === parts.slice(2).join('/')))
  })
  check('primary: moved content → BLOCKER', () => assert.match(sMoved.stdout, /Home \[primary\]: BLOCKER/))
  check('artifacts: failing comparison attaches artifactSet to result and issue', () => {
    assert.ok(sMoved.report.results[0].artifactSet.artifacts.overlay)
    assert.ok(sMoved.report.issues.find((i) => i.issueKind === 'SSIM_BLOCKER' && i.artifactSet && i.artifactSet.artifacts && i.artifactSet.artifacts.diff))
  })
  check('primary: wrong capture name → MISSING_CAPTURE (no fuzzy mis-score)', () => assert.match(sWrongName.stdout, /Home \[primary\]: MISSING_CAPTURE/))
  // Fix-brief: a failed run carries a ranked builder digest; a clean run carries none.
  check('fix-brief: BLOCKER run carries ranked hints + prints "Fix first:"; PASS run stays empty', () => {
    assert.ok(Array.isArray(sMoved.report.fixBrief) && sMoved.report.fixBrief.length >= 1, 'BLOCKER run has fixBrief entries')
    assert.match(sMoved.stdout, /Fix first:/)
    assert.deepEqual(sIdentical.report.fixBrief, [])
    assert.ok(!/Fix first:/.test(sIdentical.stdout))
    const missing = sWrongName.report.fixBrief.find((f) => /MISSING_CAPTURE/.test(f.hint))
    assert.ok(missing, 'incomplete statuses rank first with the wiring hint')
  })
  check('#20: uniform oracle + diverging render → colour-only BLOCKER', () => {
    assert.match(sEmpty.stdout, /Home \[primary\]: BLOCKER \(color-only/)
    const row = sEmpty.report.results[0]
    assert.equal(row.mode, 'color-only')
    assert.equal(row.score, null, 'no SSIM score in colour-only mode')
    assert.ok(row.meanDeltaE > 3, `mean ΔE ${row.meanDeltaE} must exceed DELTAE_PASS`)
    // A 60%-of-frame divergent block trips BOTH guards; FIX2's divergent-region guard takes
    // precedence and emits COLOR_ONLY_DIVERGENT_REGION, but a mean-only BLOCKER
    // (COLOR_ONLY_MISMATCH) is equally acceptable — either is a color-only BLOCKER.
    assert.ok(sEmpty.report.issues.some((i) => ['COLOR_ONLY_MISMATCH', 'COLOR_ONLY_DIVERGENT_REGION'].includes(i.issueKind) && i.severity === 'BLOCKER'))
  })
  check('#20: uniform oracle + matching solid render → colour-only PASS, gate exit 0 (deadlock released)', () => {
    const row = sSolidMatch.report.results[0]
    assert.equal(row.status, 'PASS')
    assert.equal(row.mode, 'color-only')
    assert.ok(row.meanDeltaE < 1, `matching fill mean ΔE ${row.meanDeltaE} ~ 0`)
    assert.ok(typeof row.coverage === 'number' && row.coverage < 0.005, 'coverage recorded on the row')
    assert.equal(sSolidMatch.report.overall, 'PASS')
    assert.equal(sSolidGateExit, 0, 'no LOW_CONTENT_ORACLE INCOMPLETE deadlock in gate mode')
  })
  check('FIX2: uniform oracle + sparse bright divergent block → color-only BLOCKER via colorDivergentFraction (mean would dilute-PASS)', () => {
    assert.match(sSolidSparseDivergent.stdout, /Home \[primary\]: BLOCKER \(color-only/)
    const row = sSolidSparseDivergent.report.results[0]
    assert.equal(row.mode, 'color-only')
    assert.equal(row.score, null, 'no SSIM score in colour-only mode')
    assert.ok(typeof row.colorDivergentFraction === 'number' && row.colorDivergentFraction > 0.02, `divergent fraction ${row.colorDivergentFraction} must exceed the 0.02 floor`)
    assert.ok(row.meanDeltaE < 3, `mean ΔE ${row.meanDeltaE} is diluted below DELTAE_PASS — the dilution this guard witnesses`)
    const ci = sSolidSparseDivergent.report.issues.find((i) => i.issueKind === 'COLOR_ONLY_DIVERGENT_REGION')
    assert.ok(ci && ci.severity === 'BLOCKER', 'distinct COLOR_ONLY_DIVERGENT_REGION blocker present')
    assert.equal(sSolidSparseDivergent.report.overall, 'BLOCKER')
  })
  check('FIX2: uniform oracle + tiny uniform drift (below ΔE00>10 everywhere) stays color-only PASS', () => {
    const row = sSolidTinyDrift.report.results[0]
    assert.equal(row.status, 'PASS')
    assert.equal(row.mode, 'color-only')
    assert.ok(row.colorDivergentFraction === 0 || row.colorDivergentFraction < 0.02, `tiny drift divergent fraction ${row.colorDivergentFraction} stays under the floor`)
    assert.equal(sSolidTinyDrift.report.overall, 'PASS', 'a matching-enough uniform component still passes (happy path preserved)')
  })
  check('primary: gross aspect mismatch → ASPECT_MISMATCH (no silent distort)', () => assert.match(sAspect.stdout, /Home \[primary\]: ASPECT_MISMATCH/))
  check('primary: same-aspect render is scored (aspect guard allows it)', () => assert.match(sRescale.stdout, /Home \[primary\]: \w+ \(ssim=/))
  // #18: a WITHIN-tolerance aspect divergence (oracle 200x400 vs capture 210x400 → 5%) is
  // force-fit anisotropically by the record-and-rescale — the distortion must be recorded
  // (row.aspectSkew, normalized >=1) and WARNed above 1.03 instead of staying invisible.
  const sSkew = await run({ oracles: [{ name: 'Home.png', ...P(CENTER, 200, 400) }], captures: [{ name: 'HomeScreenshot.png', ...P(CENTER, 210, 400) }] })
  check('#18: within-tolerance anisotropic rescale → row.aspectSkew (~1.05) + WARN ASPECT_SKEW', () => {
    const row = sSkew.report.results[0]
    assert.ok(typeof row.aspectSkew === 'number' && row.aspectSkew > 1.04 && row.aspectSkew < 1.06, `skew ${row.aspectSkew} should be ~1.05`)
    assert.ok(typeof row.score === 'number', 'row is still scored (aspect within tolerance)')
    const ai = sSkew.report.issues.find((i) => i.issueKind === 'ASPECT_SKEW')
    assert.ok(ai, 'ASPECT_SKEW issue present')
    assert.equal(ai.severity, 'WARN')
    assert.match(ai.message, /1\.050/)
    assert.equal(sSkew.report.thresholds.aspectSkewWarn, 1.03)
  })
  check('#18: UNIFORM rescale (equal aspects) records skew 1 and pushes NO ASPECT_SKEW', () => {
    assert.equal(sRescale.report.results[0].aspectSkew, 1, 'uniform record-and-rescale → skew exactly 1')
    assert.ok(!sRescale.report.issues.some((i) => i.issueKind === 'ASPECT_SKEW'))
    assert.equal(sIdentical.report.results[0].aspectSkew, undefined, 'no rescale → no skew field on the row')
  })
  check('adaptive: BOTH themes pulled → primary AND dark both compared (PASS)', () => {
    assert.match(sBoth.stdout, /Home \[primary\]: PASS/); assert.match(sBoth.stdout, /Home \[dark\]: PASS/)
  })
	  check('adaptive: dark-only → dark compared, NO primary line', () => {
	    assert.match(sDarkOnly.stdout, /Home \[dark\]: PASS/); assert.doesNotMatch(sDarkOnly.stdout, /Home \[primary\]/)
	  })
	  check('bail with an oracle emits a diff-less REFERENCE artifactSet (figma-only for MISSING_CAPTURE)', () => {
	    // Step B: a MISSING_CAPTURE row (oracle present, capture absent) now surfaces the design
	    // reference so the Done view can show it. It carries ONLY the figma slot — no actual (no
	    // capture), no diff/overlay (no pixel correspondence) — and the schema now allows that.
	    const row = sWrongName.report.results.find((r) => r.status === 'MISSING_CAPTURE')
	    assert.ok(row && row.artifactSet, 'MISSING_CAPTURE row carries a reference artifactSet')
	    assert.ok(row.artifactSet.artifacts.figma, 'reference set has the figma oracle')
	    assert.equal(row.artifactSet.artifacts.actual, undefined, 'no capture → no actual')
	    assert.equal(row.artifactSet.artifacts.diff, undefined, 'diff-less bail → no diff')
	    assert.equal(row.artifactSet.artifacts.overlay, undefined, 'diff-less bail → no overlay')
	    assert.equal(sWrongName.report.artifactSets.length, 1, 'exactly the one reference set')
	  })
	  check('bail with an oracle AND a render (ASPECT_MISMATCH) emits figma+actual, still no diff', () => {
	    const row = sAspect.report.results.find((r) => r.status === 'ASPECT_MISMATCH')
	    assert.ok(row && row.artifactSet, 'ASPECT_MISMATCH row carries a reference artifactSet')
	    assert.ok(row.artifactSet.artifacts.figma && row.artifactSet.artifacts.actual, 'figma + actual at native sizes')
	    assert.equal(row.artifactSet.artifacts.diff, undefined, 'unequal sizes → no diff')
	    assert.equal(row.artifactSet.artifacts.overlay, undefined, 'unequal sizes → no overlay')
	  })
	  check('adaptive: dark-only index ignores stale primary PNG instead of comparing it', () => {
	    assert.match(sDarkOnlyStalePrimary.stdout, /Home \[dark\]: PASS/)
	    assert.doesNotMatch(sDarkOnlyStalePrimary.stdout, /Home \[primary\]/)
	    // #22: the theme-downgrade remnant is a WARN with a remedy, no longer the ORACLE_NOT_IN_INDEX blocker.
	    const st = sDarkOnlyStalePrimary.report.issues.find((i) => i.issueKind === 'STALE_ORACLE_FILE')
	    assert.ok(st, 'STALE_ORACLE_FILE issue present')
	    assert.equal(st.severity, 'WARN')
	  })
  // #22: a stale oracle PNG left behind by a theme/bullet downgrade (unreferenced by any
  // current index node/theme) must WARN with a remedy — NOT permanently BLOCK the gate while
  // the declared screen's comparison is perfect. run() with --gate doubles as the exit-0 pin.
  const sStaleOracleFile = await run({
    oracles: [{ name: 'Home.png', ...P(CENTER) }, { name: 'Ghost.png', ...P(CENTER) }],
    captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }],
    args: ['--gate'],
  })
  check('#22: unreferenced oracle PNG → WARN STALE_ORACLE_FILE with remedy, gate exit 0 (no forever-BLOCKER)', () => {
    const si = sStaleOracleFile.report.issues.find((i) => i.issueKind === 'STALE_ORACLE_FILE')
    assert.ok(si, 'STALE_ORACLE_FILE issue present')
    assert.equal(si.severity, 'WARN')
    assert.match(si.message, /Ghost\.png/)
    assert.match(si.message, /delete|re-pull/)
    assert.ok(!sStaleOracleFile.report.issues.some((i) => i.issueKind === 'ORACLE_NOT_IN_INDEX'), 'old blocking kind is gone')
    assert.match(sStaleOracleFile.stdout, /Home \[primary\]: PASS/)
    assert.equal(sStaleOracleFile.report.overall, 'WARN', 'stale remnant downgrades overall to WARN, not BLOCKER')
  })
	  check('ROBORAZZI_OUTPUT_DIRS duplicate identity → DUPLICATE_CAPTURE', () => assert.match(sDuplicate.stdout, /Home \[primary\]: DUPLICATE_CAPTURE/))
		  check('manifest without the node identity entry → MISSING_CAPTURE', () => assert.match(sManifestStale.stdout, /Home \[primary\]: MISSING_CAPTURE/))
		  check('manifest path outside declared output roots is rejected', () => assert.match(sManifestOtherPath.stdout, /Home \[primary\]: CAPTURE_PATH_UNCONTAINED/))
		  check('manifest relative path outside declared output roots is rejected', () => assert.match(sManifestBarePath.stdout, /Home \[primary\]: CAPTURE_PATH_UNCONTAINED/))
		  check('exact current manifest entry binds the capture by nodeId', () => assert.match(sManifestCaptureName.stdout, /Home \[primary\]: PASS/))
			  check('explicit primaryState=false requires review', () => {
			    assert.match(sManifestFalsePrimary.stdout, /Home \[primary\]: REVIEW_REQUIRED/)
			    assert.ok(sManifestFalsePrimary.report.issues.some((i) => i.issueKind === 'PRIMARY_STATE_UNCONFIRMED'))
			  })
			  check('obsolete manifest state aliases are rejected instead of interpreted', () => {
			    assert.ok(sManifestRetiredAlias.report.issues.some((i) => i.issueKind === 'MANIFEST_INVALID'))
			    assert.match(sManifestRetiredAlias.stdout, /Home \[primary\]: MISSING_CAPTURE/)
			  })
			  check('capture older than current run start is stale', () => assert.match(sStartedAtStale.stdout, /Home \[primary\]: STALE_CAPTURE/))
			  check('an ancient positive capture lower bound cannot certify stale output', () => assert.equal(ancientStartedAtExit, 1))
  check('--semantic writes advisory semantic contract fields without changing PASS', () => {
    assert.equal(sSemantic.report.overall, 'PASS')
    assert.equal(sSemantic.report.semantic.enabled, true)
    assert.equal(sSemantic.report.semantic.status, 'ADVISORY_UNCALIBRATED')
    assert.equal(sSemantic.report.results[0].themeKey, 'primary')
  })
			  check('artifact retention keeps only the active run when keepRuns=1', () => {
			    assert.ok(sRetention.artifactFiles.find((f) => f.path.includes('/compare-screenshots-test/')))
			    assert.ok(!sRetention.artifactFiles.find((f) => f.path.includes('/old-a/') || f.path.includes('/old-b/')))
			  })
			  check('artifact retention does not prune previous runs when current run produced NO artifacts (even after the Step-B reference emit — unreadable oracle)', () => {
			    assert.ok(sNoArtifactRetention.artifactFiles.find((f) => f.path.includes('/old-a/old.txt')))
			    assert.ok(sNoArtifactRetention.artifactFiles.find((f) => f.path.includes('/old-b/old.txt')))
			  })
			  check('--gate returns exit 2 on incomplete evidence', () => assert.equal(missingGateExit, 2))
			  check('--gate returns exit 2 without fresh capture evidence', () => assert.equal(missingFreshExit, 2))
			  check('external FIGMA_COMPARE_ARTIFACTS_DIR is rejected before writing non-cache evidence', () => assert.equal(outsideArtifactRootExit, 1))
			  check('nested FIGMA_COMPARE_ARTIFACTS_DIR is rejected so final paths stay canonical', () => assert.equal(nestedArtifactRootExit, 1))
			  check('FIGMA_PIPELINE_RUN_ID with non-path-safe punctuation is rejected', () => assert.equal(badRunIdExit, 1))
		  check('--gate returns exit 2 when index.nodes is empty', () => assert.equal(emptyIndexExit, 2))
	  check('--gate returns exit 2 on unreadable manifest', () => assert.equal(unreadableManifestExit, 2))
	  check('--gate blocks a present malformed current spec instead of using grid regions', () => {
	    assert.equal(malformedSpec.status, 2)
	    assert.ok(malformedSpec.report.issues.some((issue) => issue.severity === 'BLOCKER' && /^SCHEMA_/.test(issue.issueKind)))
	    assert.equal(malformedSpec.report.results[0].status, 'BLOCKER')
	  })
	  // #22: with an EMPTY index the exit-2 comes from NO_INDEXED_SCREENS — the orphan oracle
	  // itself is only a STALE_ORACLE_FILE WARN now (see the dedicated #22 check below).
	  check('--gate returns exit 2 when index.nodes is empty despite an on-disk oracle PNG', () => assert.equal(orphanOracleExit, 2))
	  check('--gate returns exit 2 on corrupt image evidence', () => assert.equal(corruptCaptureExit, 2))
	  check('MAJOR band blocks in gate mode by default (SSIM_MAJOR, exit 2)', () => assert.equal(sMajorBlocksExit, 2))
	  check('SCREENSHOT_MAJOR_BAND=advisory restores the reviewed-WARN routing', () => { assert.match(sMajorAdvisory.stdout, /Home \[primary\]: MAJOR/); assert.equal(sMajorAdvisory.report.overall, 'WARN') })

	  // W2-1: gate mode requires the exact capture manifest. The harness emits current
	  // nodeId-bound entries for every other gate case; this pair pins absence itself.
	  const sNoManifest = await run({ oracles: [{ name: 'Home.png', ...P(CENTER) }], captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }], args: ['--gate'], noDefaultManifest: true, allowFailure: true })
	  check('W2-1: gate without a manifest → BLOCKER MANIFEST_ABSENT', () => {
	    const hit = (sNoManifest.report.issues || []).find((i) => i.issueKind === 'MANIFEST_ABSENT')
	    assert.ok(hit && hit.severity === 'BLOCKER', 'MANIFEST_ABSENT must block a manifest-less gate run')
	    assert.equal(sNoManifest.report.overall, 'BLOCKER')
	    assert.equal(sNoManifest.status, 2)
	  })
	  const sWithManifest = await run({ oracles: [{ name: 'Home.png', ...P(CENTER) }], captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }], args: ['--gate'] })
	  check('W2-1: gate with a manifest → no MANIFEST_ABSENT', () => {
	    assert.ok(!(sWithManifest.report.issues || []).some((i) => i.issueKind === 'MANIFEST_ABSENT'))
	  })

  // R5-3: capture↔design locale cross-check via the manifest's `localeTag`. Completeness-class
  // — always a BLOCKER (advisory CLI mode and screenshotPixelGate=advisory never route it);
  // absent localeTag or an underivable design language = no locale comparison.
  const localeRes = mkdtempSync(join(tmpdir(), 'cmp-locale-'))
  mkdirSync(join(localeRes, 'values'), { recursive: true })
  mkdirSync(join(localeRes, 'values-uk'), { recursive: true })
  writeFileSync(join(localeRes, 'values', 'strings.xml'), '<resources><string name="diag">Diagnostics</string><string name="term">Terminals</string></resources>')
  writeFileSync(join(localeRes, 'values-uk', 'strings.xml'), '<resources><string name="diag">Діагностика</string><string name="term">Термінали</string></resources>')
  const UK_SPEC = { name: 'Home.spec.json', data: { screen: 'Home', frameSizeDp: { w: 120, h: 240 }, theme: 'light', elements: [
    { stableId: 'e1', name: 't1', bboxDp: { x: 0, y: 0, w: 10, h: 10 }, text: 'Діагностика' },
    { stableId: 'e2', name: 't2', bboxDp: { x: 0, y: 10, w: 10, h: 10 }, text: 'Термінали' },
  ] } }
  const LOCALE_ENV = { FIGMA_STRING_RESOURCE_ROOTS: localeRes, FIGMA_SUPPORTED_LOCALES: 'en,uk', SCREENSHOT_CAPTURE_STARTED_AT: String(Date.now() - 1000) }
  const LOC_FIXTURE = (tag) => ({
    oracles: [{ name: 'Home.png', ...P(CENTER) }],
    captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }],
    specs: [UK_SPEC],
    manifest: { captures: [validCapture({ captureName: 'HomeScreenshot.png', ...(tag ? { localeTag: tag } : {}) })] },
  })
  const locMismatchExit = await runExit({ ...LOC_FIXTURE('en'), args: ['--gate'], env: LOCALE_ENV })
  check('R5-3: localeTag en vs uk design → CAPTURE_LOCALE_MISMATCH, gate exit 2', () => assert.equal(locMismatchExit, 2))
  const locMismatchAdvisoryGateExit = await runExit({ ...LOC_FIXTURE('en'), args: ['--gate'], env: { ...LOCALE_ENV, SCREENSHOT_PIXEL_GATE: 'advisory' } })
  check('R5-3: screenshotPixelGate=advisory does NOT route the mismatch (completeness-class, still exit 2)', () => assert.equal(locMismatchAdvisoryGateExit, 2))
  const locMismatchAdvisory = await run({ ...LOC_FIXTURE('en'), env: LOCALE_ENV })
  check('R5-3: mismatch blocks even in ADVISORY mode — BLOCKER issue + named row before any SSIM', () => {
    assert.match(locMismatchAdvisory.stdout, /Home \[primary\]: CAPTURE_LOCALE_MISMATCH/)
    const row = locMismatchAdvisory.report.results.find((r) => r.screen === 'Home')
    assert.equal(row.status, 'CAPTURE_LOCALE_MISMATCH')
    assert.equal(row.localeTag, 'en')
    assert.equal(row.designLocale, 'uk')
    assert.equal(row.score ?? null, null, 'no SSIM computed for a locale-bailed row')
    const hit = locMismatchAdvisory.report.issues.find((i) => i.issueKind === 'CAPTURE_LOCALE_MISMATCH')
    assert.ok(hit && hit.severity === 'BLOCKER')
    assert.match(hit.message, /check-capture-config --fix/)
    assert.equal(locMismatchAdvisory.report.overall, 'BLOCKER')
    assert.deepEqual(locMismatchAdvisory.report.designLocale, { language: 'uk', source: 'detected' })
  })
  const locMatch = await run({ ...LOC_FIXTURE('uk'), args: ['--gate'], env: LOCALE_ENV })
  check('R5-3: localeTag uk matches the uk design → clean PASS', () => {
    assert.equal(locMatch.report.overall, 'PASS')
    assert.ok(!locMatch.report.issues.some((i) => i.issueKind === 'CAPTURE_LOCALE_MISMATCH'))
  })
  check('R5-3 anti-forgery: fixture-only locale env overrides are RECORDED in the report (the final bundle blocks on them)', () => {
    assert.deepEqual(locMatch.report.designLocaleEnvOverrides.sort(), ['FIGMA_STRING_RESOURCE_ROOTS', 'FIGMA_SUPPORTED_LOCALES'])
    assert.ok(!sWithManifest.report.designLocaleEnvOverrides, 'a run without the env knobs records nothing')
  })
  const locWithoutTag = await run({ ...LOC_FIXTURE(null), args: ['--gate'], env: LOCALE_ENV })
  check('R5-3: manifest without optional localeTag → no locale check', () => {
    assert.equal(locWithoutTag.report.overall, 'PASS')
    assert.ok(!locWithoutTag.report.issues.some((i) => /LOCALE/.test(String(i.issueKind))))
  })
  const locNoSignal = await run({
    ...LOC_FIXTURE('en'),
    specs: [{ name: 'Home.spec.json', data: { screen: 'Home', frameSizeDp: { w: 120, h: 240 }, theme: 'light', elements: [{ stableId: 'e1', name: 't1', bboxDp: { x: 0, y: 0, w: 10, h: 10 } }] } }],
    args: ['--gate'],
    env: LOCALE_ENV,
  })
  check('R5-3: underivable design language (textless spec) → no comparator check (check-capture-config owns UNDERIVABLE)', () => {
    assert.equal(locNoSignal.report.overall, 'PASS')
    assert.ok(!locNoSignal.report.issues.some((i) => /LOCALE/.test(String(i.issueKind))))
  })
  rmSync(localeRes, { recursive: true, force: true })

  // R6-3: the ASPECT_MISMATCH bail names its likely cause when an unnormalized oracle
  // is taller than the capture by an iOS-chrome-sized band (40–90dp). Diagnostic only — the
  // row still bails ASPECT_MISMATCH; a chrome-stamped spec or an out-of-band delta stays silent.
  const CHROME_SPEC = (extra = {}) => ({ name: 'Home.spec.json', data: { screen: 'Home', frameSizeDp: { w: 200, h: 300 }, theme: 'light', elements: [{ stableId: 'e1', name: 't1', bboxDp: { x: 0, y: 0, w: 10, h: 10 } }], ...extra } })
  const chromeBail = await run({
    oracles: [{ name: 'Home.png', w: 200, h: 300, rect: CENTER }],
    captures: [{ name: 'HomeScreenshot.png', w: 200, h: 253, rect: CENTER }],
    specs: [CHROME_SPEC()],
  })
  check('R6-3: 47dp-taller un-stamped oracle → ASPECT_MISMATCH bail names IOS_CHROME_SUSPECTED + the re-pull remedy', () => {
    const row = chromeBail.report.results.find((r) => r.screen === 'Home')
    assert.equal(row.status, 'ASPECT_MISMATCH')
    assert.match(row.error, /IOS_CHROME_SUSPECTED/)
    assert.match(row.error, /~47dp/)
    assert.match(row.error, /re-pull the screens so normalize-oracle strips it/)
  })
  const chromeStamped = await run({
    oracles: [{ name: 'Home.png', w: 200, h: 300, rect: CENTER }],
    captures: [{ name: 'HomeScreenshot.png', w: 200, h: 253, rect: CENTER }],
    specs: [CHROME_SPEC({ chromeCrop: { topDp: 47, bottomDp: 0, matched: ['9:41'], at: 'T' } })],
  })
  check('R6-3 control: a chrome-STAMPED spec never carries the hint (still a plain ASPECT_MISMATCH)', () => {
    const row = chromeStamped.report.results.find((r) => r.screen === 'Home')
    assert.equal(row.status, 'ASPECT_MISMATCH')
    assert.ok(!/IOS_CHROME_SUSPECTED/.test(row.error), row.error)
  })
  const chromeOutOfBand = await run({
    oracles: [{ name: 'Home.png', w: 200, h: 300, rect: CENTER }],
    captures: [{ name: 'HomeScreenshot.png', w: 200, h: 150, rect: CENTER }],
    specs: [CHROME_SPEC()],
  })
  check('R6-3 control: a 150dp-taller oracle (outside the chrome band) stays a plain ASPECT_MISMATCH', () => {
    const row = chromeOutOfBand.report.results.find((r) => r.screen === 'Home')
    assert.equal(row.status, 'ASPECT_MISMATCH')
    assert.ok(!/IOS_CHROME_SUSPECTED/.test(row.error), row.error)
  })

  // W5-3 — mask-dilation ring: a blur-halo hugging real content (divergent pixels within 3px of
  // the mask edge) is NOT render-added content; the same pixels DO count with the ring disabled
  // (the stricter direction, always allowed), which is the mechanism proof. The rogue-panel
  // fixture above (sExtra, far from any mask edge) keeps tripping at the default ring.
  // Geometry measured against the real metric (halo-sweep): the oracle's own variance mask
  // extends ~2px past the box edge, so a 3px-thick gray-44 band at offset 2 sits just OUTSIDE
  // the mask (divergent: luma 44 vs bg 16 → Δ28 > 24) yet fully INSIDE the 3px dilation ring —
  // ring on: fraction 0; ring off: ~1.8%.
  const haloOracle = { name: 'Home.png', w: 120, h: 240, boxes: [[30, 80, 90, 160]] }
  const haloCapture = { name: 'HomeScreenshot.png', w: 120, h: 240, boxes: [
    [30, 80, 90, 160],
    [25, 75, 95, 78, 44, 44, 44], [25, 162, 95, 165, 44, 44, 44],   // 3px halo band at offset 2: top/bottom
    [25, 78, 28, 162, 44, 44, 44], [92, 78, 95, 162, 44, 44, 44],   // left/right
  ] }
  const haloFloor = { SCREENSHOT_EXTRA_CONTENT_WARN: '0.005' }   // lowered floor = stricter (allowed) — makes the contrast decisive
  const sHalo = await run({ oracles: [haloOracle], captures: [haloCapture], env: haloFloor })
  const sHaloNoRing = await run({ oracles: [haloOracle], captures: [haloCapture], env: { ...haloFloor, SCREENSHOT_EXTRA_CONTENT_RING_PX: '0' } })
  check('W5-3: an edge halo passes with the default dilation ring (not render-added content)', () => {
    const row = sHalo.report.results[0]
    assert.equal(row.status, 'PASS')
    assert.ok(row.extraContentFraction < 0.005, `halo must be ring-skipped, got fraction ${row.extraContentFraction}`)
    assert.ok(!sHalo.report.issues.some((i) => i.issueKind === 'RENDER_EXTRA_CONTENT'))
    assert.equal(sHalo.report.thresholds.extraContentRingPx, 3, 'the ring knob is recorded for the canon net')
  })
  check('W5-3: the SAME halo trips with the ring disabled (0 = stricter, allowed) + per-region breakdown reported', () => {
    const row = sHaloNoRing.report.results[0]
    assert.ok(row.extraContentFraction > 0.005, `without the ring the halo must count, got ${row.extraContentFraction}`)
    assert.ok(sHaloNoRing.report.issues.some((i) => i.issueKind === 'RENDER_EXTRA_CONTENT'))
    assert.ok(Array.isArray(row.extraContentRegions) && row.extraContentRegions.length, 'per-region extra-content breakdown present')
    assert.ok(row.extraContentRegions[0].fraction >= (row.extraContentRegions.at(-1) || row.extraContentRegions[0]).fraction, 'worst region first')
  })

  // W5-2 — text-aware ΔE: the SAME chroma drift that fires COLOR_DRIFT_REVIEW on non-text
  // content is QUARANTINED when the divergent zone is a TEXT element (font substitution noise):
  // no WARN, textDeltaE reported separately, non-text worstRegionDeltaE stays clean.
  const TEXT_SPEC = { name: 'Home.spec.json', data: { screen: 'Home', frameSizeDp: { w: 120, h: 240 }, theme: 'light', elements: [
    { stableId: 'title', name: 'Title', role: 'text', textStyle: { sizeSp: 14, weight: 400, lineHeightSp: 18, case: 'none' }, bboxDp: { x: 25, y: 75, w: 70, h: 90 } },
  ] } }
  const sTextDrift = await run({
    oracles: [{ name: 'Home.png', w: 120, h: 240, boxes: [[30, 80, 90, 160, 128, 128, 128]] }],
    captures: [{ name: 'HomeScreenshot.png', w: 120, h: 240, boxes: [[30, 80, 90, 160, 150, 118, 130]] }],
    specs: [TEXT_SPEC],
  })
  check('W5-2: chroma drift confined to a TEXT element → no COLOR_DRIFT_REVIEW; textDeltaE reported separately', () => {
    const row = sTextDrift.report.results[0]
    assert.equal(row.status, 'PASS')
    assert.ok(!sTextDrift.report.issues.some((i) => i.issueKind === 'COLOR_DRIFT_REVIEW'), 'text ΔE is font-substitution noise, never a drift witness')
    assert.ok(typeof row.textDeltaE === 'number' && row.textDeltaE > 3, `textDeltaE must carry the drift (${row.textDeltaE})`)
    assert.ok((row.worstRegionDeltaE || 0) < 3, `non-text worstRegionDeltaE must stay clean (${row.worstRegionDeltaE})`)
  })
  const sBrokenNoDrift = await run({
    oracles: [{ name: 'Home.png', w: 120, h: 240, rect: CENTER }],
    captures: [{ name: 'HomeScreenshot.png', w: 120, h: 240, boxes: [[10, 10, 50, 50, 255, 60, 60]] }],
  })
  check('W5-2: a structurally-BROKEN row keeps ΔE as report fields but never stacks the COLOR_DRIFT_REVIEW WARN', () => {
    const row = sBrokenNoDrift.report.results[0]
    assert.ok(row.status === 'BLOCKER' || row.status === 'MAJOR', `structure must be flagged (${row.status})`)
    assert.ok(!sBrokenNoDrift.report.issues.some((i) => i.issueKind === 'COLOR_DRIFT_REVIEW'), 'no colour-noise WARN on an already-flagged row')
    assert.ok(typeof row.worstRegionDeltaE === 'number', 'ΔE numbers stay recorded as fields')
  })

  // W5-5 — substituted fonts: a design family outside Robolectric's bundled set is NAMED as a
  // VERDICT-NEUTRAL report field `row.substitutedFonts[]` (not a WARN issue — the font is the
  // owner's choice, not a fixable defect; a WARN would flip overall PASS→WARN and force
  // completed-with-caveats on every UI task of any non-Roboto/Noto design). Bundled → no field.
  const FONT_SPEC = (family) => ({ name: 'Home.spec.json', data: { screen: 'Home', frameSizeDp: { w: 120, h: 240 }, theme: 'light', elements: [
    { stableId: 'title', name: 'Title', textStyle: { sizeSp: 14, weight: 400, lineHeightSp: 18, case: 'none', fontFamily: family }, bboxDp: { x: 30, y: 80, w: 60, h: 80 } },
  ] } })
  const sForeignFont = await run({ oracles: [{ name: 'Home.png', ...P(CENTER) }], captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }], specs: [FONT_SPEC('SF Pro Text')] })
  const sBundledFont = await run({ oracles: [{ name: 'Home.png', ...P(CENTER) }], captures: [{ name: 'HomeScreenshot.png', ...P(CENTER) }], specs: [FONT_SPEC('Roboto Condensed')] })
  check('W5-5: a non-bundled design font → verdict-neutral substitutedFonts field; overall stays PASS; bundled → no field', () => {
    const row = sForeignFont.report.results[0]
    assert.deepEqual(row.substitutedFonts, ['SF Pro Text'])
    assert.equal(sForeignFont.report.overall, 'PASS', 'an informational font note must NOT flip a clean PASS to WARN')
    assert.ok(!sForeignFont.report.issues.some((i) => i.issueKind === 'SUBSTITUTED_FONT'), 'no verdict-affecting issue is emitted')
    assert.ok(!sBundledFont.report.results[0].substitutedFonts, 'a bundled family records nothing')
    assert.equal(sBundledFont.report.overall, 'PASS')
  })
} finally {
}

console.log(`\ncompare-screenshots.test: ${pass} pass, ${fail} fail`)
process.exit(fail === 0 ? 0 : 1)
