// CLI contract fixtures: new comparison flags must fail fast when unsupported/invalid.
import { existsSync, mkdtempSync, mkdirSync, readFileSync, rmSync, symlinkSync, utimesSync, writeFileSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { tmpdir } from 'node:os'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { spawnSync } from 'node:child_process'
import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { accountSnapshotIssue, enforcementWiringFindings } from '../scripts/doctor.mjs'   // W2: pure preflight verdicts
import { isDirectRun } from '../scripts/_util.mjs'
const { uiTaskWithoutDesign, provenanceNodeForStem, provenanceSourceTouched, uiWidgetSourcesTouched } = createRequire(import.meta.url)('../scripts/design-parser.cjs')

const HERE = dirname(fileURLToPath(import.meta.url))
const COMPARE_SPEC = join(HERE, '..', 'scripts', 'compare-screen-spec.mjs')
const COMPARE_SHOTS = join(HERE, '..', 'scripts', 'compare-screenshots.mjs')
const CHECK_SPEC = join(HERE, '..', 'scripts', 'check-spec.mjs')
const CHECK_SCREEN_CACHE = join(HERE, '..', 'scripts', 'check-screen-cache.mjs')
const COMPONENT_CENSUS = join(HERE, '..', 'scripts', 'component-census.mjs')
const CAPTURE_CONFIG = join(HERE, '..', 'scripts', 'check-capture-config.mjs')
const EVIDENCE = join(HERE, '..', 'scripts', 'evidence-bundle.mjs')
const EVIDENCE_CLEAN = join(HERE, '..', 'scripts', 'evidence-clean.mjs')
const EXTRACT_MODEL = join(HERE, '..', 'scripts', 'extract-compose-model.mjs')
const WRITE_SPEC = join(HERE, '..', 'scripts', 'write-spec-report.mjs')
const SECURITY_GREP = join(HERE, '..', 'scripts', 'check-direct-figma-access.mjs')
const CALIBRATE = join(HERE, '..', 'scripts', 'calibrate-thresholds.mjs')
const CALIBRATE_GEN = join(HERE, '..', 'scripts', 'generate-calibration-mutations.mjs')
const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }

function run(args, env = {}) {
  return spawnSync('node', args, { env: { ...process.env, ...env }, encoding: 'utf8' })
}

const ws = mkdtempSync(join(tmpdir(), 'figma-cli-contracts-'))
try {
  const reports = join(ws, 'reports')
  const screensRoot = join(ws, 'screens')
  const screens = join(screensRoot, 'TASK_1_fixture')
  mkdirSync(reports, { recursive: true })
  mkdirSync(screens, { recursive: true })
  writeFileSync(join(ws, 'Screen.kt'), '@Composable fun Screen() { Text("x") }\n')

  check('direct-run detection accepts a symlinked CLI path instead of silently no-oping', () => {
    const alias = join(ws, 'component-census-alias.mjs')
    symlinkSync(COMPONENT_CENSUS, alias)
    assert.equal(isDirectRun(pathToFileURL(COMPONENT_CENSUS).href, alias), true)
    assert.equal(isDirectRun(pathToFileURL(COMPONENT_CENSUS).href, CAPTURE_CONFIG), false)
  })

  check('compare-screen-spec rejects unknown flags', () => {
    const r = run([COMPARE_SPEC, 'TASK_1_fixture', '--bogus'], { FIGMA_REPORTS_DIR: reports, FIGMA_SPEC_SCREENS_DIR: screensRoot })
    assert.notEqual(r.status, 0)
    assert.match(r.stderr, /unknown argument --bogus/)
  })

  check('calibrate-thresholds rejects unknown flags', () => {
    const r = run([CALIBRATE, '--bogus'])
    assert.notEqual(r.status, 0)
    assert.match(r.stderr, /unknown argument --bogus/)
  })

  check('generate-calibration-mutations rejects unknown flags and requires --out', () => {
    const r = run([CALIBRATE_GEN, '--bogus'])
    assert.notEqual(r.status, 0)
    assert.match(r.stderr, /unknown argument --bogus/)
    const r2 = run([CALIBRATE_GEN])
    assert.notEqual(r2.status, 0)
    assert.match(r2.stderr, /--out <dir> is required/)
  })

  check('compare-screenshots rejects unknown flags even when --semantic is present', () => {
    const r = run([COMPARE_SHOTS, 'TASK_1_fixture', '--semantic', '--bogus'], { FIGMA_REPORTS_DIR: reports, FIGMA_SPEC_SCREENS_DIR: screensRoot })
    assert.notEqual(r.status, 0)
    assert.match(r.stderr, /unknown argument --bogus/)
  })

  check('compare-screenshots rejects malformed semantic env flag', () => {
    const r = run([COMPARE_SHOTS, 'TASK_1_fixture'], { FIGMA_REPORTS_DIR: reports, FIGMA_SPEC_SCREENS_DIR: screensRoot, SCREENSHOT_SEMANTIC_DIFF: 'maybe' })
    assert.notEqual(r.status, 0)
    assert.match(r.stderr, /SCREENSHOT_SEMANTIC_DIFF/)
  })

  check('check-spec rejects typo flags instead of silently running advisory', () => {
    const r = run([CHECK_SPEC, 'TASK_1_fixture', '--gtae'], { FIGMA_REPORTS_DIR: reports, FIGMA_SPEC_SCREENS_DIR: screensRoot })
    assert.notEqual(r.status, 0)
    assert.match(r.stderr, /unknown argument --gtae/)
  })

  check('check-screen-cache rejects typo flags instead of silently choosing advisory/gate', () => {
    const r = run([CHECK_SCREEN_CACHE, 'TASK_1_fixture', '--gtae'], { FIGMA_REPORTS_DIR: reports, FIGMA_SCREEN_CACHE_ROOT: screensRoot })
    assert.notEqual(r.status, 0)
    assert.match(r.stderr, /unknown argument --gtae/)
  })

  check('component-census rejects typo flags instead of silently using default paths', () => {
    const r = run([COMPONENT_CENSUS, 'TASK_1_fixture', '--screen-dir', screensRoot], { FIGMA_REPORTS_DIR: reports })
    assert.notEqual(r.status, 0)
    assert.match(r.stderr, /unknown argument --screen-dir/)
  })

  check('check-capture-config rejects typo flags instead of silently skipping fix/gate', () => {
    const r = run([CAPTURE_CONFIG, 'TASK_1_fixture', '--fiix'], { FIGMA_REPORTS_DIR: reports, FIGMA_SPEC_SCREENS_DIR: screensRoot })
    assert.notEqual(r.status, 0)
    assert.match(r.stderr, /unknown argument --fiix/)
  })

  check('extract-compose-model rejects unknown flags', () => {
    const r = run([EXTRACT_MODEL, '--out', join(ws, 'model.json'), '--bogus'])
    assert.notEqual(r.status, 0)
    assert.match(r.stderr, /unknown argument --bogus/)
  })

  // W4-1: write-spec-report.mjs owns the clerical surface the validator used to hand-author.
  // Every rejection below used to be a fail-closed blocker at the FINAL bundle (or a silent
  // certification hole) — the contract is that they die HERE, before anything hits disk.
  const specRepEnv = { FIGMA_REPORTS_DIR: reports, FIGMA_PIPELINE_RUN_ID: 'cli-contract-run' }
  check('write-spec-report rejects unknown flags', () => {
    const r = run([WRITE_SPEC, 'TASK_2_specrep', '--bogus'], specRepEnv)
    assert.notEqual(r.status, 0)
    assert.match(r.stderr, /unknown argument --bogus/)
  })
  check('write-spec-report refuses to write without the spec-compare machine baseline', () => {
    const r = run([WRITE_SPEC, 'TASK_2_specrep', '--screen', 'Home=PASS'], specRepEnv)
    assert.notEqual(r.status, 0)
    assert.match(r.stderr, /machine baseline missing/)
    assert.match(r.stderr, /compare-screen-spec/)
    assert.equal(existsSync(join(reports, 'spec-TASK_2_specrep.json')), false)
  })
  writeFileSync(join(reports, 'spec-compare-TASK_2_specrep.json'), '{broken')
  check('write-spec-report rejects a corrupt spec-compare baseline instead of dropping its implementation witness', () => {
    const r = run([WRITE_SPEC, 'TASK_2_specrep', '--screen', 'Home=PASS'], specRepEnv)
    assert.notEqual(r.status, 0)
    assert.match(r.stderr, /machine baseline is unreadable or not valid JSON/)
  })
  writeFileSync(join(reports, 'spec-compare-TASK_2_specrep.json'), JSON.stringify({ inputs: {} }) + '\n')
  check('write-spec-report rejects a malformed --screen verdict', () => {
    const r = run([WRITE_SPEC, 'TASK_2_specrep', '--screen', 'Home=MAYBE'], specRepEnv)
    assert.notEqual(r.status, 0)
    assert.match(r.stderr, /verdict must be PASS or FAIL/)
  })
  check('write-spec-report rejects an issue naming an undeclared screen (typo catch)', () => {
    const r = run([WRITE_SPEC, 'TASK_2_specrep', '--screen', 'Home=PASS', '--issue', 'MINOR:spec.value:Hone:2dp off'], specRepEnv)
    assert.notEqual(r.status, 0)
    assert.match(r.stderr, /undeclared screen "Hone"/)
  })
  check('write-spec-report rejects a FAIL screen with no routed finding', () => {
    const r = run([WRITE_SPEC, 'TASK_2_specrep', '--screen', 'Home=FAIL'], specRepEnv)
    assert.notEqual(r.status, 0)
    assert.match(r.stderr, /marked FAIL but carries no BLOCKER\/MAJOR issue/)
  })
  check('write-spec-report rejects a routed finding on a PASS screen', () => {
    const r = run([WRITE_SPEC, 'TASK_2_specrep', '--screen', 'Home=PASS', '--issue', 'MAJOR:spec.token:Home:wrong slot'], specRepEnv)
    assert.notEqual(r.status, 0)
    assert.match(r.stderr, /routed MAJOR finding but its verdict is PASS/)
  })
  check('write-spec-report rejects combining --verdict-file with --screen/--issue', () => {
    const vf = join(ws, 'verdicts.json')
    writeFileSync(vf, JSON.stringify({ screens: [{ screen: 'Home', verdict: 'PASS' }] }))
    const r = run([WRITE_SPEC, 'TASK_2_specrep', '--verdict-file', vf, '--screen', 'Home=PASS'], specRepEnv)
    assert.notEqual(r.status, 0)
    assert.match(r.stderr, /alternative input forms/)
  })
  check('write-spec-report authors the full envelope: computed overall/counts, baseline pin, policy stamp', () => {
    const r = run([WRITE_SPEC, 'TASK_2_specrep',
      '--screen', 'Home=FAIL:token slot', '--screen', 'Detail=PASS',
      '--issue', 'MAJOR:spec.token:Home:text.secondary where spec says text.primary',
      '--issue', 'MINOR:spec.value:Detail:1dp radius nuance: acceptable'], specRepEnv)
    assert.equal(r.status, 0, r.stderr)
    const report = JSON.parse(readFileSync(join(reports, 'spec-TASK_2_specrep.json'), 'utf8'))
    assert.equal(report.mode, 'gate')
    assert.equal(report.overall, 'BLOCKER')                      // any routed finding ⇒ BLOCKER
    assert.equal(report.blockingCount, 0)                        // MAJOR counts toward warningCount
    assert.equal(report.warningCount, 2)                         // …the exact hand-math trap
    assert.equal(report.pipelineRunId, 'cli-contract-run')
    assert.ok(Number.isInteger(report.gatePolicyVersion) && report.gatePolicyVersion >= 1)
    assert.equal(report.authoredBy, 'write-spec-report-cli')
    assert.equal(report.screens.length, 2)
    assert.equal(report.screens.find((s) => s.screen === 'Home').note, 'token slot')
    const majorRow = report.issues.find((i) => i.issueKind === 'spec.token')
    assert.equal(majorRow.severity, 'MAJOR')
    // message keeps its own colons (split is bounded to the first three separators)
    assert.equal(report.issues.find((i) => i.issueKind === 'spec.value').message, '1dp radius nuance: acceptable')
    const pin = Object.keys(report.inputHashes).find((k) => k.endsWith('spec-compare-TASK_2_specrep.json'))
    assert.ok(pin, 'baseline must be pinned in inputHashes')
    assert.match(report.inputHashes[pin], /^sha256:[0-9a-f]{64}$/)
  })
  // Self-review pins: three silent-drop paths the adversarial review reproduced — each
  // authored PASS with exit 0 while the dropped/typo'd/unbacked findings demanded WARN/FAIL.
  check('write-spec-report rejects a repeated --verdict-file (first-wins would drop findings)', () => {
    const v1 = join(ws, 'verdicts-1.json')
    const v2 = join(ws, 'verdicts-2.json')
    writeFileSync(v1, JSON.stringify({ screens: [{ screen: 'Home', verdict: 'PASS' }] }))
    writeFileSync(v2, JSON.stringify({ screens: [{ screen: 'Detail', verdict: 'FAIL' }], issues: [{ severity: 'MAJOR', ruleId: 'spec.token', screen: 'Detail', message: 'wrong slot' }] }))
    const r = run([WRITE_SPEC, 'TASK_2_specrep', '--verdict-file', v1, '--verdict-file', v2], specRepEnv)
    assert.notEqual(r.status, 0)
    assert.match(r.stderr, /passed 2 times/)
  })
  check('write-spec-report rejects unknown top-level verdict-file keys (typo\'d "Issues" would drop findings)', () => {
    const vf = join(ws, 'verdicts-typo.json')
    writeFileSync(vf, JSON.stringify({ screens: [{ screen: 'Home', verdict: 'PASS' }], Issues: [{ severity: 'MINOR', ruleId: 'spec.value', screen: 'Home', message: '2dp off' }] }))
    const r = run([WRITE_SPEC, 'TASK_2_specrep', '--verdict-file', vf], specRepEnv)
    assert.notEqual(r.status, 0)
    assert.match(r.stderr, /unknown top-level key "Issues"/)
  })
  check('write-spec-report rejects a minors count unbacked by MINOR issue rows (would mint PASS instead of WARN)', () => {
    const vf = join(ws, 'verdicts-minors.json')
    writeFileSync(vf, JSON.stringify({ screens: [{ screen: 'Home', verdict: 'PASS', minors: 2 }], issues: [] }))
    const r = run([WRITE_SPEC, 'TASK_2_specrep', '--verdict-file', vf], specRepEnv)
    assert.notEqual(r.status, 0)
    assert.match(r.stderr, /declares minors=2 but carries 0 MINOR issue row/)
  })
  check('write-spec-report --verdict-file authors a Minors-only WARN report', () => {
    const vf = join(ws, 'verdicts-warn.json')
    writeFileSync(vf, JSON.stringify({
      screens: [{ screen: 'Home', verdict: 'PASS', minors: 1 }],
      issues: [{ severity: 'minor', ruleId: 'spec.value', screen: 'Home', message: '2dp off', file: 'ui/Home.kt' }],
    }))
    const r = run([WRITE_SPEC, 'TASK_2_specrep', '--verdict-file', vf], specRepEnv)
    assert.equal(r.status, 0, r.stderr)
    const report = JSON.parse(readFileSync(join(reports, 'spec-TASK_2_specrep.json'), 'utf8'))
    assert.equal(report.overall, 'WARN')
    assert.equal(report.issues[0].severity, 'MINOR')             // case-normalized to the envelope enum
    assert.equal(report.issues[0].file, 'ui/Home.kt')
    assert.equal(report.screens[0].minors, 1)
  })

  // R1-2: the owner's renderClass stamp now lives in the CAS mapping registry
  // (Mapping Review set-render-class op) — pinned by design-token-mappings-style
  // server tests and compare-screenshots.test's routing pins, not a CLI here.

  // The gate-policy version is fail-closed at the loader — a thresholds file without
  // it (for example, a stale vendored copy or a hand-edit that dropped the key) dies loudly in EVERY
  // consumer (report writers included) instead of silently stamping undefined into reports.
  check('thresholds loader rejects a config missing the gate-policy version', () => {
    const tree = join(ws, 'no-version-figma')
    mkdirSync(join(tree, 'scripts', 'lib'), { recursive: true })
    writeFileSync(join(tree, 'scripts', '_util.mjs'), readFileSync(join(HERE, '..', 'scripts', '_util.mjs')))
    // `_util.mjs` imports the shared Outcome boundary at module load. Keep this
    // isolated fixture dependency-complete so the assertion reaches the
    // screenshot-threshold contract instead of failing during ESM resolution.
    writeFileSync(join(tree, 'scripts', 'outcome-shape.mjs'), readFileSync(join(HERE, '..', 'scripts', 'outcome-shape.mjs')))
    writeFileSync(join(tree, 'scripts', 'design-parser.cjs'), readFileSync(join(HERE, '..', 'scripts', 'design-parser.cjs')))
    writeFileSync(join(tree, 'scripts', 'lib', 'artifact-path.cjs'), readFileSync(join(HERE, '..', 'scripts', 'lib', 'artifact-path.cjs')))
    const committed = JSON.parse(readFileSync(join(HERE, '..', 'screenshot-thresholds.json'), 'utf8'))
    delete committed.version
    writeFileSync(join(tree, 'screenshot-thresholds.json'), JSON.stringify(committed, null, 2))
    const r = run(['-e', `import(${JSON.stringify(join(tree, 'scripts', '_util.mjs'))}).then((u) => u.loadScreenshotThresholds())`])
    assert.notEqual(r.status, 0)
    assert.match(r.stderr, /version must be an integer >= 1/)
  })

	  check('evidence final stage fails when required reports are missing', () => {
    const r = run([EVIDENCE, 'TASK_1_fixture', '--stage', 'final', '--require', 'spec-compare,screenshot', '--fresh'], {
      FIGMA_REPORTS_DIR: reports,
      FIGMA_SCREEN_CACHE_ROOT: screensRoot,
      FIGMA_PIPELINE_RUN_ID: 'cli-contract-run',
    })
    const report = JSON.parse(readFileSync(join(reports, 'evidence-TASK_1_fixture.json'), 'utf8'))
    assert.notEqual(r.status, 0)
    assert.equal(report.overall, 'BLOCKER')
    assert.ok(report.issues.find((i) => i.issueKind === 'REPORT_MISSING' && i.reportName === 'screenshot'))
	  })

  check('evidence-clean --bundle-only preserves gate reports for final re-bundle', () => {
    const cleanStem = 'TASK_3_cleanstem'
    const reportNames = ['screen-cache', 'check-spec', 'capture-config', 'census', 'spec', 'spec-compare', 'screenshot']
    writeFileSync(join(reports, `evidence-${cleanStem}.json`), '{}\n')
    writeFileSync(join(reports, `figma-meta-${cleanStem}.txt`), 'digest\n')
    for (const name of reportNames) writeFileSync(join(reports, `${name}-${cleanStem}.json`), '{}\n')
    const r = run([EVIDENCE_CLEAN, cleanStem, '--bundle-only'], {
      FIGMA_CACHE_ROOT: ws,
      FIGMA_REPORTS_DIR: reports,
    })
    assert.equal(r.status, 0, r.stderr)
    assert.match(r.stdout, /bundle only/)
    assert.equal(existsSync(join(reports, `evidence-${cleanStem}.json`)), false)
    assert.equal(existsSync(join(reports, `figma-meta-${cleanStem}.txt`)), false)
    for (const name of reportNames) assert.equal(existsSync(join(reports, `${name}-${cleanStem}.json`)), true, `${name} report should survive`)
  })

  check('evidence-clean rejects obsolete age-based pruning and preserves cache bytes', () => {
    const screensRoot = join(ws, 'screens-retention')
    const retained = join(screensRoot, 'TASK_4_retained', 'proof.txt')
    mkdirSync(dirname(retained), { recursive: true })
    writeFileSync(retained, 'referenced evidence\n')
    const r = run([EVIDENCE_CLEAN, '--prune-stale', '1', '--yes'], {
      FIGMA_CACHE_ROOT: ws,
      FIGMA_SCREEN_CACHE_ROOT: screensRoot,
      FIGMA_REPORTS_DIR: reports,
    })
    assert.notEqual(r.status, 0)
    assert.equal(readFileSync(retained, 'utf8'), 'referenced evidence\n')
  })

  check('evidence-clean rejects a symlinked reports ancestor and preserves the external report', () => {
    const external = join(ws, 'external-reports')
    const linkedReports = join(ws, 'linked-reports')
    const cleanStem = 'TASK_5_symlinked_reports'
    mkdirSync(external, { recursive: true })
    writeFileSync(join(external, `evidence-${cleanStem}.json`), 'external evidence\n')
    symlinkSync(external, linkedReports, 'dir')
    const r = run([EVIDENCE_CLEAN, cleanStem, '--bundle-only'], {
      FIGMA_CACHE_ROOT: ws,
      FIGMA_REPORTS_DIR: linkedReports,
    })
    assert.notEqual(r.status, 0)
    assert.match(r.stderr, /not a root-anchored regular file/)
    assert.equal(readFileSync(join(external, `evidence-${cleanStem}.json`), 'utf8'), 'external evidence\n')
  })

  check('doctor never treats an invalid account file as a current-shaped snapshot', () => {
    const base = { handle: 'A', email: '', checkedAt: '2026-07-14T10:00:01Z', verificationNonce: 'a'.repeat(32) }
    assert.equal(accountSnapshotIssue(base), null)
    assert.match(accountSnapshotIssue({ ...base, verificationNonce: undefined }), /verificationNonce/)
    assert.match(accountSnapshotIssue({ ...base, handle: ' ', email: '' }), /handle\/email/)
    assert.match(accountSnapshotIssue({ ...base, checkedAt: 'July 14, 2026' }), /checkedAt/)
  })

		  check('direct Figma grep catches split Figma REST/token literals', () => {
	    const badRoot = join(ws, 'security-root')
	    mkdirSync(badRoot, { recursive: true })
	    writeFileSync(join(badRoot, 'bad.js'), `
	      const url = 'https://api.' + 'figma.com/v1/files/x'
		      const token = process.env['FIGMA_ACCESS_' + 'TOKEN']
		      const arrayUrl = ['https://api', 'figma.com/v1/files/x'].join('.')
		      const arrayUrlThree = ['https://api', 'figma', 'com/v1/files/x'].join('.')
		      const arrayHeader = ['X-Figma', 'Token'].join('-')
		    `)
	    const r = run([SECURITY_GREP], { FIGMA_SECURITY_GREP_ROOTS: badRoot })
	    assert.notEqual(r.status, 0)
	    assert.match(r.stdout + r.stderr, /figmaRestHost/)
	    assert.match(r.stdout + r.stderr, /figmaAccessToken/)
	    assert.match(r.stdout + r.stderr, /figmaHeaderToken/)
	  })

		  check('direct Figma grep catches multiline split literals', () => {
		    const badRoot = join(ws, 'security-root-multiline')
		    mkdirSync(badRoot, { recursive: true })
		    writeFileSync(join(badRoot, 'bad.js'), `
		      const url = 'https://api'
		        + '.'
		        + 'figma'
		        + '.com/v1/files/x'
		    `)
		    const r = run([SECURITY_GREP], { FIGMA_SECURITY_GREP_ROOTS: badRoot })
		    assert.notEqual(r.status, 0)
		    assert.match(r.stdout + r.stderr, /figmaRestHost/)
		  })

		  check('direct Figma grep scans file roots', () => {
		    const badFile = join(ws, 'security-file-root.js')
		    writeFileSync(badFile, `const url = 'https://api.' + 'figma.com/v1/files/x'\n`)
		    const r = run([SECURITY_GREP], { FIGMA_SECURITY_GREP_ROOTS: badFile })
		    assert.notEqual(r.status, 0)
		    assert.match(r.stdout + r.stderr, /figmaRestHost/)
		  })

	  check('direct Figma grep does not allowlist a forbidden REST call on a token-redaction line', () => {
	    const badRoot = join(ws, 'security-root-allow')
	    mkdirSync(badRoot, { recursive: true })
	    writeFileSync(join(badRoot, 'bad.js'), `const x = 'api.' + 'figma.com'; console.log('X-Figma-Token: redacted')\n`)
	    const r = run([SECURITY_GREP], { FIGMA_SECURITY_GREP_ROOTS: badRoot })
	    assert.notEqual(r.status, 0)
	    assert.match(r.stdout + r.stderr, /figmaRestHost/)
	  })

	  check('direct Figma grep catches direct and split MCP tool references', () => {
	    const badRoot = join(ws, 'security-root-mcp')
	    mkdirSync(badRoot, { recursive: true })
	    writeFileSync(join(badRoot, 'bad.js'), `const direct = 'get_variable_defs'\nconst split = 'get_variable_' + 'defs'\n`)
	    const r = run([SECURITY_GREP], { FIGMA_SECURITY_GREP_ROOTS: badRoot })
	    assert.notEqual(r.status, 0)
	    assert.match(r.stdout + r.stderr, /mcpGetVariableDefs/)
	  })
} finally {
  rmSync(ws, { recursive: true, force: true })
}

// W2: figma:doctor enforcement-wiring verdict (pure — a figmaEnabled product must wire the
// pre-commit hook, or a bare `git mv` ships an uncompared UI task).
const levels = (r) => r.map((x) => x.level).join(',')
check('W2: hook wired -> ok', () => assert.equal(levels(enforcementWiringFindings({ inGit: true, hooksPath: 'orchestrator/skills/checks/hooks' })), 'ok'))
check('W2: core.hooksPath UNSET -> fail (local net inactive)', () => { const r = enforcementWiringFindings({ inGit: true, hooksPath: '' }); assert.equal(r[0].level, 'fail'); assert.match(r[0].msg, /core\.hooksPath/) })
check('W2: unwired NON-strict (Step 6.5 bootstrap) -> warn (does not break bootstrap)', () => assert.equal(levels(enforcementWiringFindings({ inGit: true, hooksPath: '', strict: false })), 'warn'))
check('W2: not a git work-tree -> single warn (graceful, no false FAIL)', () => assert.equal(levels(enforcementWiringFindings({ inGit: false })), 'warn'))

// P0: provenance-aware backstop — a component the mapping-registry provenance
// (componentProvenanceEntries() rows) records as built from a Figma NODE cannot self-classify
// non-UI (drop its comparison) by stripping the in-text snapshot. The surviving provenance is
// consulted so the comparison can never vanish.
const NODE_INV = [{ component: 'DownUpStreamChartCard', figmaNodeId: '2207:24988', status: 'active' }]
const ANCHORLESS_INV = [{ component: 'SqfGauge', source: 'design-system/components/src/x/SqfGauge.kt' }]  // no figmaNodeId → carve-out
const STRIPPED_TASK = '# TASK\n## Goal\nbuild it\n## Design\n- none\n'   // in-text snapshot + node URL removed (Path A)
check('P0: provenanceNodeForStem matches component by stem token and returns the node', () => {
  const r = provenanceNodeForStem('TASK_205_component_downupstreamchartcard', NODE_INV)
  assert.ok(r && r.nodeId === '2207:24988' && r.component === 'DownUpStreamChartCard')
})
check('P0: a provenance row without figmaNodeId is the derive-only carve-out (no node → null)', () => {
  assert.equal(provenanceNodeForStem('TASK_9_component_sqfgauge', ANCHORLESS_INV), null)
})
check('P0: no matching component in inventory → null (non-component tasks unaffected)', () => {
  assert.equal(provenanceNodeForStem('TASK_1_feature_login', NODE_INV), null)
})
check('P0: stripped task + node-backed inventory → BLOCK (escape hatch closed)', () => {
  const v = uiTaskWithoutDesign(STRIPPED_TASK, { stem: 'TASK_205_component_downupstreamchartcard', inventory: NODE_INV })
  assert.ok(v && v.level === 'block')
  assert.match(v.reason, /2207:24988/)
})
check('P0: provenance BLOCK is not rescued by an audited `— none (reason)` opt-out', () => {
  const md = '# TASK\n## Design\n- DownUpStreamChartCard — none (canvas, no mock)\n'
  const v = uiTaskWithoutDesign(md, { stem: 'TASK_205_component_downupstreamchartcard', inventory: NODE_INV })
  assert.ok(v && v.level === 'block')   // provenance tier is BEFORE the none opt-out
})
check('P0: anchorless component + stripped task → NOT blocked by provenance (carve-out honored)', () => {
  const v = uiTaskWithoutDesign(STRIPPED_TASK, { stem: 'TASK_9_component_sqfgauge', inventory: ANCHORLESS_INV })
  assert.equal(v, null)
})
check('P0: no opts → provenance tier inert', () => {
  assert.equal(uiTaskWithoutDesign(STRIPPED_TASK), null)
})
check('P0: a pullable Design bullet short-circuits before provenance (already on the pixel track)', () => {
  const md = '# TASK\n## Design\n- DownUpStreamChartCard [component] — https://www.figma.com/design/K/x?node-id=2207-24988\n'
  assert.equal(uiTaskWithoutDesign(md, { stem: 'TASK_205_component_downupstreamchartcard', inventory: NODE_INV }), null)
})
// P0 FIX (adversarial review): the stem match must be ANCHORED to the task-TYPE segment, or a
// mid-stem `_component_` in an unrelated engineering task false-blocks it.
check('P0-fix: mid-stem `_component_` in a NON-component task does NOT match (no false block)', () => {
  assert.equal(provenanceNodeForStem('TASK_8_refactor_component_census_parser', [{ component: 'Census', figmaNodeId: '5:5' }]), null)
  assert.equal(provenanceNodeForStem('TASK_9_fix_component_drift_reads', [{ component: 'Drift', figmaNodeId: '6:6' }]), null)
})
check('P0-fix: a genuine TASK_n_component_<name> stem still matches (guarantee intact)', () => {
  const r = provenanceNodeForStem('TASK_208_component_census', [{ component: 'Census', figmaNodeId: '5:5' }])
  assert.ok(r && r.component === 'Census')
})
check('P0-fix: a multi-word snake_case component stem normalizes and matches its PascalCase entry', () => {
  const r = provenanceNodeForStem('TASK_5_component_down_up_stream_chart_card', NODE_INV)
  assert.ok(r && r.component === 'DownUpStreamChartCard')
})

// ── STRICT UI-coverage: content-based provenance (Class 3) + created/modified structural tier (Class 2)
const lvl = (v) => (v ? v.level : null)
const ft = (bullets) => '# T\n## Goal\nx\n## Design\n\n### Files touched\n' + bullets + '\n'
const SRC_INV = [{ component: 'SpeedCard', figmaNodeId: '9:9', source: 'design-system/components/src/commonMain/kotlin/ds/SpeedCard.kt' }]
const ANCHORLESS_SRC = [{ component: 'SqfGauge', source: 'design-system/components/src/x/SqfGauge.kt' }]

// Class 3 — provenance by touched source file, independent of stem shape
check('C3: provenanceSourceTouched matches a node-backed inventory source in ### Files touched', () => {
  const r = provenanceSourceTouched(ft('- `design-system/components/src/commonMain/kotlin/ds/SpeedCard.kt` — modified'), SRC_INV)
  assert.ok(r && r.nodeId === '9:9')
})
check('C3: a node-backed DS component under a NON-_component_ stem is still BLOCKED (stem hole closed)', () => {
  const md = ft('- `design-system/components/src/commonMain/kotlin/ds/SpeedCard.kt` — modified')
  assert.equal(lvl(uiTaskWithoutDesign(md, { stem: 'TASK_5_add_speedcard', inventory: SRC_INV })), 'block')
})
check('C3: node-backed source is NOT rescued by an audited `— none` (un-opt-out-able)', () => {
  const md = '# T\n## Design\n- SpeedCard — none (x)\n\n### Files touched\n- `design-system/components/src/commonMain/kotlin/ds/SpeedCard.kt` — modified\n'
  assert.equal(lvl(uiTaskWithoutDesign(md, { stem: 'TASK_5', inventory: SRC_INV })), 'block')
})
check('C3: a source row without figmaNodeId is the derive-only carve-out → not held', () => {
  assert.equal(provenanceSourceTouched(ft('- `design-system/components/src/x/SqfGauge.kt` — modified'), ANCHORLESS_SRC), null)
})
check('C3: source match is not basename-only (different module Button.kt does not false-match)', () => {
  const inv = [{ component: 'Button', figmaNodeId: '1:1', source: 'design-system/components/src/a/Button.kt' }]
  assert.equal(provenanceSourceTouched(ft('- `feature/dash/src/b/Button.kt` — modified'), inv), null)
})
check('C3: a suffix-only source path never substitutes for exact repo-relative identity', () => {
  assert.equal(provenanceSourceTouched(ft('- `src/a/Button.kt` — modified'), [{
    component: 'Button', figmaNodeId: '1:1', source: 'design-system/components/src/a/Button.kt'
  }]), null)
})

// Class 2 — structural created/modified tier
check('C2: a CREATED feature-local card (components/ path), no design → BLOCK', () => {
  assert.equal(lvl(uiTaskWithoutDesign(ft('- `ui-screen-features/dash/src/commonMain/kotlin/x/components/SpeedCard.kt` — created'), { stem: 'TASK_5_ui_speed', inventory: [] })), 'block')
})
check('C2: a created card with an audited `— none (reason)` ships (explicit account)', () => {
  const md = '# T\n## Design\n- SpeedCard — none (pure layout, no mock)\n\n### Files touched\n- `x/components/SpeedCard.kt` — created\n'
  assert.equal(uiTaskWithoutDesign(md, { stem: 'TASK_5', inventory: [] }), null)
})
check('C2: a MODIFIED card (no design) → WARN, not block (non-visual-edit tolerance)', () => {
  assert.equal(lvl(uiTaskWithoutDesign(ft('- `x/components/SpeedCard.kt` — modified'), { stem: 'TASK_5', inventory: [] })), 'warn')
})
check('C2: a created NON-UI file (data/repo) is not a widget → ships (no false block)', () => {
  assert.equal(uiTaskWithoutDesign(ft('- `x/data/SpeedRepository.kt` — created'), { stem: 'TASK_5', inventory: [] }), null)
})
check('C2: a DELETED widget file is dropped (nothing to compare)', () => {
  const w = uiWidgetSourcesTouched(ft('- `x/components/OldCard.kt` — deleted'))
  assert.deepEqual(w, { created: [], modified: [] })
})
check('C2: a task WITH a pullable Design bullet short-circuits (card compared transitively in the screen)', () => {
  const md = '# T\n## Design\n- Home [screen] — https://www.figma.com/design/K/x?node-id=1-2\n\n### Files touched\n- `x/components/SpeedCard.kt` — created\n'
  assert.equal(uiTaskWithoutDesign(md, { stem: 'TASK_5', inventory: [] }), null)
})
check('C2: a created *Screen.kt file (no node) → BLOCK (new UI surface must be accounted)', () => {
  assert.equal(lvl(uiTaskWithoutDesign(ft('- `x/home/HomeScreen.kt` — created'), { stem: 'TASK_5', inventory: [] })), 'block')
})
check('C2: uiWidgetSourcesTouched classifies created vs modified vs non-widget', () => {
  const w = uiWidgetSourcesTouched(ft('- `a/components/A.kt` — created\n- `b/BScreen.kt` — modified\n- `c/data/C.kt` — created'))
  assert.deepEqual(w.created, ['a/components/A.kt'])
  assert.deepEqual(w.modified, ['b/BScreen.kt'])
})

// ── Adversarial-review fixes (2nd pass): fail-closed status, nested components/, per-widget none
const dft = (bullets, design = '') => '# T\n## Design\n' + design + '\n### Files touched\n' + bullets + '\n'
const dlvl = (v) => (v ? v.level : null)
// A — FAIL-CLOSED status: an omitted/paraphrased status on a UI-widget path must NOT downgrade to warn
check('A-fix: a created card with NO status word → BLOCK (fail-closed, not warn)', () => {
  assert.equal(dlvl(uiTaskWithoutDesign(dft('- `x/components/SpeedCard.kt`'), { inventory: [] })), 'block')
})
check('A-fix: a paraphrased status (authored/implemented) → BLOCK', () => {
  assert.equal(dlvl(uiTaskWithoutDesign(dft('- `x/components/SpeedCard.kt` — authored'), { inventory: [] })), 'block')
  assert.equal(dlvl(uiTaskWithoutDesign(dft('- `x/components/SpeedCard.kt` — implemented the widget'), { inventory: [] })), 'block')
})
check('A-fix: an EXPLICIT `modified` status still relaxes to WARN (non-visual tolerance kept)', () => {
  assert.equal(dlvl(uiTaskWithoutDesign(dft('- `x/components/SpeedCard.kt` — modified'), { inventory: [] })), 'warn')
})
// B — nested components/ at any depth (the routine cards/ buttons/ layout, and the template layout)
check('B-fix: a created card nested under components/<group>/ → BLOCK (any depth)', () => {
  assert.equal(dlvl(uiTaskWithoutDesign(dft('- `design-system/components/cards/SpeedCard.kt` — created'), { inventory: [] })), 'block')
  assert.equal(dlvl(uiTaskWithoutDesign(dft('- `com/example/design/components/banner/BannerCard.kt` — created'), { inventory: [] })), 'block')
})
check('B/D: a non-UI helper under components/utils|di|mappers is NOT a widget (no false block)', () => {
  assert.equal(uiTaskWithoutDesign(dft('- `design/components/utils/Clip.kt` — created'), { inventory: [] }), null)
  assert.equal(uiTaskWithoutDesign(dft('- `app/components/di/AppModule.kt` — created'), { inventory: [] }), null)
})
// C — per-widget opt-out: one unrelated `— none` must NOT disarm the created block
check('C-fix: a created widget + an UNRELATED audited `— none` → still BLOCK (per-widget, not global)', () => {
  assert.equal(dlvl(uiTaskWithoutDesign(dft('- `x/components/SpeedCard.kt` — created', '- Legend — none (copy, no mock)'), { inventory: [] })), 'block')
})
check('C-fix: a created widget + a `— none` NAMING that widget → accounted (null)', () => {
  assert.equal(uiTaskWithoutDesign(dft('- `x/components/SpeedCard.kt` — created', '- SpeedCard — none (pure layout, no mock)'), { inventory: [] }), null)
})
check('C-fix: 3 created widgets + 1 unrelated `— none` → BLOCK (all must be named)', () => {
  assert.equal(dlvl(uiTaskWithoutDesign(dft('- `a/components/SpeedCard.kt` — created\n- `b/components/MacroChip.kt` — created\n- `c/components/ToggleRow.kt` — created', '- Foo — none (x)'), { inventory: [] })), 'block')
})
// SCOPING: fail-closed is for components/ CARDS only; a status-less screen-FILE edit keeps the
// deliberate weak tolerance (a non-visual copy/callback edit must not hard-block).
check('scope: a status-less SCREEN-file edit stays advisory WARN (non-visual tolerance preserved)', () => {
  assert.equal(dlvl(uiTaskWithoutDesign(dft('- `ui/HomeScreen.kt`'), { inventory: [] })), 'warn')
})
check('scope: a status-less CARD is fail-closed to BLOCK (the gap), NOT warn', () => {
  assert.equal(dlvl(uiTaskWithoutDesign(dft('- `ui/components/HomeCard.kt`'), { inventory: [] })), 'block')
})
// D-fix (template-grounded, 2nd review): the non-UI exclusion is by IMMEDIATE-PARENT dir only, and
// `internal/` is NOT excluded (the template stores real @Composable widgets under `<group>/internal/`).
check('D-fix: a created card under `<group>/internal/` → BLOCK (internal is a visibility conv, not non-UI)', () => {
  assert.equal(dlvl(uiTaskWithoutDesign(dft('- `ds/components/src/x/design.components/chart/internal/RadarChart.kt` — created'), { inventory: [] })), 'block')
})
check('D-fix: a Modifier-extension under `components/modifiers/` is NOT a widget (no false block)', () => {
  assert.equal(uiTaskWithoutDesign(dft('- `ds/components/src/x/design.components/modifiers/Shadow.kt` — created'), { inventory: [] }), null)
})
check('D-fix: exclusion is leaf-dir only — a card whose leaf is `cards/` under a mid-path `utils/` → BLOCK', () => {
  assert.equal(dlvl(uiTaskWithoutDesign(dft('- `x/components/utils/cards/RealCard.kt` — created'), { inventory: [] })), 'block')
})

console.log(`\ncli-contracts.test: ${pass} pass, ${fail} fail`)
process.exit(fail === 0 ? 0 : 1)
