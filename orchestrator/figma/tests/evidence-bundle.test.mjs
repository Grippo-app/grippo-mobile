// Fixture self-test for evidence-bundle.mjs.
import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, rmSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { tmpdir } from 'node:os'
import { fileURLToPath } from 'node:url'
import { execFileSync, spawnSync } from 'node:child_process'
import assert from 'node:assert/strict'
import { MAPPING_CONSULT_KEY, computeMappingConsultDigest } from '../scripts/component-census.mjs'
import { loadScreenshotThresholds } from '../scripts/_util.mjs'
import { validComponentCapture } from './component-fixtures.mjs'
import { normalizeCapture as normalizeComponentCapture } from '../components/capture-normalizer.mjs'
import { bytesHash } from '../runtime/canonical-json.mjs'
import { sourceIdentity, validObservedCapture } from './observed-token-fixtures.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const SCRIPT = join(HERE, '..', 'scripts', 'evidence-bundle.mjs')
const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const GATE_POLICY_VERSION = loadScreenshotThresholds().version
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }

function bodyFor(name) {
  if (name === 'screen-cache') return { screens: [{ screen: 'Home', status: 'complete', themes: {} }] }
  if (name === 'check-spec') return { files: [{ file: 'Home.spec.json', status: 'PASS' }] }
  if (name === 'census') return { version: 2, screens: {}, components: [], missing: [], incomplete: [], ambiguous: [], unsupported: [], retired: [], sourceStale: [] }
  return {}
}

const figmaUrl = (nodeId) => `https://www.figma.com/design/FileKey?node-id=${String(nodeId).replace(':', '-')}`
function screenNode({ nodeId = '1:2', fetchedAt = '2026-01-01T00:00:00.000Z', darkNodeId = null, includeFetchedAt = true } = {}) {
  const primary = { id: 'primary', theme: 'light', locale: 'default', platform: 'shared', url: figmaUrl(nodeId), nodeId, imageFile: 'Home.png' }
  if (includeFetchedAt) primary.fetchedAt = fetchedAt
  const node = { kind: 'screen', url: primary.url, nodeId, variants: [primary] }
  if (includeFetchedAt) node.fetchedAt = fetchedAt
  if (darkNodeId) {
    const dark = { id: 'dark', theme: 'dark', locale: 'default', platform: 'shared', url: figmaUrl(darkNodeId), nodeId: darkNodeId, fetchedAt, imageFile: 'Home.dark.png' }
    Object.assign(node, { darkUrl: dark.url, darkNodeId, darkFetchedAt: fetchedAt })
    node.variants.push(dark)
  }
  return node
}

function writeScreenIndex(directory, nodes) {
  let ordinal = 0
  for (const [screenKey, node] of Object.entries(nodes)) {
    for (const variant of node.variants) {
      ordinal += 1
      const capture = validObservedCapture({
        source: sourceIdentity({
          nodeId: variant.nodeId,
          context: { theme: variant.theme, locale: variant.locale, platform: variant.platform },
          origin: { kind: 'task-screen', taskStem: 'TASK_1_fixture', screenKey, variantId: variant.id },
        }),
        captureOperationId: `tokop_${String(ordinal).padStart(16, '0')}`,
        captureSequence: 1,
      })
      const tokenBytes = Buffer.from(JSON.stringify(capture, null, 2) + '\n')
      Object.assign(variant, {
        tokensFile: `${screenKey}.${variant.id}.tokens.json`,
        tokensHash: bytesHash(tokenBytes),
        captureOperationId: capture.captureOperationId,
        captureSequence: capture.captureSequence,
      })
      writeFileSync(join(directory, variant.tokensFile), tokenBytes)
    }
  }
  writeFileSync(join(directory, 'index.json'), JSON.stringify({ schemaVersion: 3, taskStem: 'TASK_1_fixture', nodes }))
}

const ws = mkdtempSync(join(tmpdir(), 'evidence-bundle-'))
try {
  const reports = join(ws, 'reports')
  const screens = join(ws, 'screens', 'TASK_1_fixture')
  mkdirSync(reports, { recursive: true }); mkdirSync(screens, { recursive: true })
	  for (const name of ['screen-cache', 'check-spec', 'census']) {
	    writeFileSync(join(reports, `${name}-TASK_1_fixture.json`), JSON.stringify({
	      schemaVersion: 1,
	      gatePolicyVersion: GATE_POLICY_VERSION,
	      taskStem: 'TASK_1_fixture',
	      pipelineRunId: 'evidence-bundle-test',
	      mode: 'gate',
	      inputs: {},
	      inputHashes: {},
	      overall: 'PASS',
	      blockingCount: 0,
	      warningCount: 0,
	      issues: [],
	      generatedAt: '2026-01-01T00:00:00.000Z',
	      reportPath: `reports/${name}-TASK_1_fixture.json`,
	      ...bodyFor(name)
	    }, null, 2))
	  }
  writeFileSync(join(screens, 'Home.spec.json'), '{"screen":"Home"}\n')
  execFileSync('node', [SCRIPT, 'TASK_1_fixture'], {
    env: { ...process.env, FIGMA_REPORTS_DIR: reports, FIGMA_SCREEN_CACHE_ROOT: join(ws, 'screens') },
    stdio: 'pipe',
  })
  const bundle = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
  check('evidence bundle passes with required reports', () => assert.equal(bundle.overall, 'PASS'))
  check('evidence bundle carries report hashes', () => assert.ok(bundle.reports.find((r) => r.name === 'check-spec').hash))
  check('evidence bundle carries screen file hashes', () => assert.ok(bundle.screenFiles.find((f) => f.name === 'Home.spec.json').hash))

  // Oracle-age advisory (final stage only): an old fetchedAt → WARN ORACLE_PULL_STALE;
  // fresh or knob=0 → silent. The final run legitimately exits non-zero here (other
  // required final reports are absent) — the pin reads the written bundle regardless.
  const finalEnvBase = { ...process.env, FIGMA_REPORTS_DIR: reports, FIGMA_SCREEN_CACHE_ROOT: join(ws, 'screens') }
  const runFinal = (indexNode, extraEnv) => {
    writeScreenIndex(screens, { Home: indexNode })
    spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--stage', 'final', '--fresh'], { env: { ...finalEnvBase, FIGMA_PIPELINE_RUN_ID: 'evidence-bundle-test', ...extraEnv }, encoding: 'utf8' })
    return JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
  }
  check('final stage WARNs ORACLE_PULL_STALE on an old fetchedAt', () => {
    const b = runFinal(screenNode({ fetchedAt: '2020-01-01T00:00:00Z' }), {})
    const hit = (b.issues || []).find((i) => i.issueKind === 'ORACLE_PULL_STALE')
    assert.ok(hit && hit.severity === 'WARN', 'stale pull surfaces as WARN')
  })
  check('fresh fetchedAt / disabled knob → no ORACLE_PULL_STALE', () => {
    const fresh = runFinal(screenNode({ fetchedAt: new Date().toISOString() }), {})
    assert.ok(!(fresh.issues || []).some((i) => i.issueKind === 'ORACLE_PULL_STALE'))
    const off = runFinal(screenNode({ fetchedAt: '2020-01-01T00:00:00Z' }), { FIGMA_ORACLE_MAX_AGE_DAYS: '0' })
    assert.ok(!(off.issues || []).some((i) => i.issueKind === 'ORACLE_PULL_STALE'))
  })
  check('disabled age warning does not accept a missing oracle timestamp', () => {
    const b = runFinal(screenNode({ includeFetchedAt: false }), { FIGMA_ORACLE_MAX_AGE_DAYS: '0' })
    const hit = (b.issues || []).find((i) => i.issueKind === 'ORACLE_PULL_TIME_INVALID')
    assert.ok(hit && hit.severity === 'BLOCKER', 'missing fetchedAt must remain blocking')
  })

  // C2(i) count-completeness — the FINAL screenshot report must carry a result row for EVERY
  // indexed node variant it should have compared. A report whose results cover a SUBSET of
  // index.json (a dropped screen / a skipped dark variant / a truncated or forged report) →
  // BLOCKER SCREENSHOT_COVERAGE_INCOMPLETE. Variants (screen×theme), not base names.
  const runCoverage = (nodes, results) => {
    writeScreenIndex(screens, nodes)
    writeFileSync(join(reports, 'screenshot-TASK_1_fixture.json'), JSON.stringify({
      schemaVersion: 1, gatePolicyVersion: GATE_POLICY_VERSION, taskStem: 'TASK_1_fixture', pipelineRunId: 'evidence-bundle-test', mode: 'gate',
	      inputs: { captureStartedAt: '2026-01-01T00:00:00.000Z', captureMode: 'recorded' }, inputHashes: {}, overall: 'PASS',
      blockingCount: 0, warningCount: 0, issues: [], generatedAt: '2026-01-01T00:00:00.000Z',
      reportPath: 'reports/screenshot-TASK_1_fixture.json', results,
    }))
    spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--stage', 'final', '--fresh'], { env: { ...finalEnvBase, FIGMA_PIPELINE_RUN_ID: 'evidence-bundle-test' }, encoding: 'utf8' })
    const b = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
    rmSync(join(reports, 'screenshot-TASK_1_fixture.json'), { force: true })   // don't leak into other tests
    return b
  }
  check('C2(i): a screenshot report missing an indexed dark variant → SCREENSHOT_COVERAGE_INCOMPLETE (BLOCKER)', () => {
    const b = runCoverage(
      { Home: screenNode({ darkNodeId: '1:3' }) },
      [{ screen: 'Home', themeKey: 'primary', status: 'PASS' }],   // dark row missing
    )
    const hit = (b.issues || []).find((i) => i.issueKind === 'SCREENSHOT_COVERAGE_INCOMPLETE' && i.screen === 'Home' && i.theme === 'dark')
    assert.ok(hit && hit.severity === 'BLOCKER', 'a missing indexed dark variant must BLOCK')
  })
  check('C2(i): a screenshot report missing a whole indexed screen → SCREENSHOT_COVERAGE_INCOMPLETE', () => {
    const detail = screenNode({ nodeId: '2:2' })
    detail.variants[0].imageFile = 'Detail.png'
    const b = runCoverage(
      { Home: screenNode(), Detail: detail },
      [{ screen: 'Home', themeKey: 'primary', status: 'PASS' }],   // Detail entirely absent
    )
    assert.ok((b.issues || []).some((i) => i.issueKind === 'SCREENSHOT_COVERAGE_INCOMPLETE' && i.screen === 'Detail'))
  })
  check('C2(i): full coverage (primary + declared dark) → no SCREENSHOT_COVERAGE_INCOMPLETE', () => {
    const b = runCoverage(
      { Home: screenNode({ darkNodeId: '1:3' }) },
      [{ screen: 'Home', themeKey: 'primary', status: 'PASS' }, { screen: 'Home', theme: 'dark', status: 'PASS' }],
    )
    assert.ok(!(b.issues || []).some((i) => i.issueKind === 'SCREENSHOT_COVERAGE_INCOMPLETE'))
  })

  // Mapping-consult recompute: the census's `virtual:component-mappings-consulted` pin is
  // re-derived against the LIVE component truth (design inventory + mapping registry) — an
  // unrelated later mapping upsert must NOT block, a change to a CONSULTED identity must.
  const baseConsultInventory = normalizeComponentCapture(validComponentCapture(), 'sha256:' + 'c'.repeat(64))
  const badgeComponent = baseConsultInventory.components.find((row) => row.providerIdentity.nodeId === '30:1')
  const otherComponent = baseConsultInventory.components.find((row) => row.providerIdentity.nodeId === '20:1')
  const BADGE_ID = badgeComponent.designComponentId
  const OTHER_ID = otherComponent.designComponentId
  const consultSetIds = ['30:1']
  const consultInventoryFile = join(ws, 'consult-design-inventory.json')
  const consultMappingsFile = join(ws, 'consult-component-mappings.json')
  const absentAnalysisFile = join(ws, 'consult-analysis-absent.json')
  const writeConsultTruth = ({ badgeImplementation = 'compose-ds:symbol:ds.Badge', extraMappings = [], sameNamedTwin = false } = {}) => {
    const capture = validComponentCapture()
    if (sameNamedTwin) capture.entities.find((row) => row.nodeId === '20:1').name = 'Badge'
    const inventory = normalizeComponentCapture(capture, 'sha256:' + 'c'.repeat(64))
    writeFileSync(consultInventoryFile, JSON.stringify(inventory, null, 2))
    writeFileSync(consultMappingsFile, JSON.stringify({
      schemaVersion: 2, revision: 1, designScopeId: inventory.scopeId,
      mappings: [{
        mappingId: 'cmap-' + 'a'.repeat(24), designComponentId: BADGE_ID,
        expectedKind: badgeComponent.kind, state: 'active',
        implementations: [{ adapterId: 'compose-ds', platform: 'android-compose',
          projectScopeFingerprint: 'sha256:' + 'b'.repeat(64), relation: 'direct',
          projectComponentIds: [badgeImplementation], required: true }],
        propertyMappings: [], slotMappings: [],
        provenance: { kind: 'user-confirmed', actor: 'owner', at: '2026-01-01T00:00:00.000Z' }
      }, ...extraMappings],
      dispositions: [],
    }, null, 2))
  }
  const liveConsultDigest = () => {
    const inventory = JSON.parse(readFileSync(consultInventoryFile, 'utf8'))
    const registry = JSON.parse(readFileSync(consultMappingsFile, 'utf8'))
    return computeMappingConsultDigest(consultSetIds, { inventory, registry, analysis: { present: false } })
  }
  const writeCensusWithConsult = () => {
    const digest = liveConsultDigest()
    writeFileSync(join(reports, 'census-TASK_1_fixture.json'), JSON.stringify({
      schemaVersion: 1,
      gatePolicyVersion: GATE_POLICY_VERSION,
      taskStem: 'TASK_1_fixture',
      pipelineRunId: 'evidence-bundle-test',
      mode: 'advisory',
      inputs: {},
      inputHashes: { [MAPPING_CONSULT_KEY]: digest },
      overall: 'PASS',
      blockingCount: 0,
      warningCount: 0,
      issues: [],
      generatedAt: '2026-01-01T00:00:00.000Z',
      reportPath: 'reports/census-TASK_1_fixture.json',
      mappingConsult: { version: 2, setIds: consultSetIds, digest },
      ...bodyFor('census')
    }, null, 2))
  }
  const censusMismatch = (bundle) => (bundle.issues || []).filter((i) => i.reportName === 'census' && (i.issueKind === 'REPORT_INPUT_HASH_MISMATCH' || i.issueKind === 'REPORT_INPUT_HASHES_INVALID' || i.issueKind === 'REPORT_INPUT_HASH_INVALID'))
  const runPrebuildFresh = () => {
    spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--stage', 'prebuild', '--fresh',
      '--fixture-component-inventory', consultInventoryFile,
      '--fixture-component-mappings', consultMappingsFile,
      '--fixture-component-analysis-index', absentAnalysisFile],
    { env: { ...finalEnvBase, FIGMA_PIPELINE_RUN_ID: 'evidence-bundle-test' }, encoding: 'utf8' })
    return JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
  }
  check('consult pin: fresh bundle accepts a matching consult digest', () => {
    writeConsultTruth()
    writeCensusWithConsult()
    assert.deepEqual(censusMismatch(runPrebuildFresh()), [])
  })
  check('consult pin: an UNRELATED mapping upsert does not block', () => {
    writeConsultTruth({
      extraMappings: [{
        mappingId: 'cmap-' + 'b'.repeat(24), designComponentId: OTHER_ID,
        expectedKind: otherComponent.kind, state: 'active',
        implementations: [{ adapterId: 'compose-ds', platform: 'android-compose',
          projectScopeFingerprint: 'sha256:' + 'b'.repeat(64), relation: 'direct',
          projectComponentIds: ['compose-ds:symbol:ds.Card'], required: true }],
        propertyMappings: [], slotMappings: [],
        provenance: { kind: 'user-confirmed', actor: 'owner', at: '2026-01-01T00:00:00.000Z' }
      }],
    })
    assert.deepEqual(censusMismatch(runPrebuildFresh()), [], 'unrelated upsert retro-blocked the census pin')
  })
  check('consult pin: a CONSULTED identity change blocks with REPORT_INPUT_HASH_MISMATCH', () => {
    writeConsultTruth({ badgeImplementation: 'compose-ds:symbol:ds.BadgeV2' })
    const hits = censusMismatch(runPrebuildFresh())
    assert.ok(hits.some((i) => i.issueKind === 'REPORT_INPUT_HASH_MISMATCH'), `expected census REPORT_INPUT_HASH_MISMATCH, got ${JSON.stringify(hits)}`)
  })
  check('consult pin: identity is the owning node id — a same-named twin set never enters the digest', () => {
    writeConsultTruth()
    writeCensusWithConsult()
    // Rename another stable-id component to the same display name: it stays
    // outside the consulted node-id set and therefore outside the digest.
    writeConsultTruth({ sameNamedTwin: true })
    assert.deepEqual(censusMismatch(runPrebuildFresh()), [], 'a same-named set under an unconsulted node id must not become identity')
  })
  check('consult pin: an unknown virtual key fails closed', () => {
    const report = JSON.parse(readFileSync(join(reports, 'census-TASK_1_fixture.json'), 'utf8'))
    report.inputHashes = { 'virtual:made-up': 'sha256:' + 'a'.repeat(64) }
    writeFileSync(join(reports, 'census-TASK_1_fixture.json'), JSON.stringify(report, null, 2))
    const hits = censusMismatch(runPrebuildFresh())
    assert.ok(hits.some((i) => i.issueKind === 'REPORT_INPUT_HASHES_INVALID'), 'unknown virtual input must be a BLOCKER')
  })
} catch (e) {
  fail++; console.log(`${C.red}FAIL${C.reset} evidence run threw\n     ${e.stdout ? e.stdout.toString() : e.message}`)
} finally {
  rmSync(ws, { recursive: true, force: true })
}

console.log(`\nevidence-bundle.test: ${pass} pass, ${fail} fail`)
process.exit(fail === 0 ? 0 : 1)
