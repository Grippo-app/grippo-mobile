// evidence-bundle.mjs — compact transport index for per-task Figma gate reports.
//
// Usage:
//   node scripts/evidence-bundle.mjs <stem> [--stage prebuild|final] [--require a,b] [--fresh]
//   (--stage final requires --fresh)
//
// Env:
//   FIGMA_REPORTS_DIR        — override reports dir
//   FIGMA_SCREEN_CACHE_ROOT or FIGMA_SPEC_SCREENS_DIR — override screens cache root (must match if both are set)
//   FIGMA_CACHE_ROOT         — override consolidated figma cache root for isolated fixtures

import { existsSync, readdirSync, readFileSync, renameSync, writeFileSync, lstatSync, realpathSync } from 'node:fs'
import { basename, dirname, extname, isAbsolute, join, resolve, sep } from 'node:path'
import { createRequire } from 'node:module'
import { FIGMA_CACHE_ROOT, PROJECT_ROOT, artifactSegment, displayPath, figmaPath, figmaScreensRoot, loadScreenshotThresholds, parseCli, pipelineRunId, readConfig, runIdPinPath } from './_util.mjs'
import { assertTaskStem, compileSchema, fileHash, schemaIssues, writeReport } from './report-utils.mjs'
import { buildFigmaMeta } from './figma-meta.mjs'
import { buildTaskObservationReceipt } from '../tokens/task-observation-receipt.mjs'
import { loadResolvedSpecs } from './resolve-screen-spec.mjs'
import { MAPPING_CONSULT_KEY, computeMappingConsultDigest } from './component-census.mjs'
import { loadDesignComponentInventory, loadComponentMappings, loadPublishedComponentAnalysis, renderClassByNodeId } from './lib/design-components.mjs'
import { readSupportedLocales } from './lib/design-locale.mjs'
import { CAPTURE_CONFIG_DISCOVERY_KEY, captureConfigDiscovery, captureConfigScopeOmissions } from './lib/capture-config-discovery.mjs'
import { readTaskMarkdown } from './lib/task-markdown.mjs'

const requireCjs = createRequire(import.meta.url)
const { parseDesignSources } = requireCjs('./design-parser.cjs')

const REPORT_PREFIXES = ['screen-cache', 'check-spec', 'capture-config', 'census', 'spec', 'spec-compare', 'screenshot']
const DEFAULT_REQUIRED = {
  prebuild: ['screen-cache', 'check-spec', 'census'],
  final: ['screen-cache', 'check-spec', 'capture-config', 'census', 'spec', 'spec-compare', 'screenshot'],
}
const USAGE = 'usage: node scripts/evidence-bundle.mjs <stem> [--stage prebuild|final] [--require a,b] [--fresh] (--stage final requires --fresh)'
const KNOWN_OVERALL = new Set(['PASS', 'WARN', 'WARNING', 'MINOR', 'MAJOR', 'SKIPPED', 'INCOMPLETE', 'REVIEW_REQUIRED', 'BLOCKER', 'FAIL', 'ERROR'])
const KNOWN_SCREENSHOT_RESULT_STATUS = new Set([
  'PASS', 'MINOR', 'MAJOR', 'BLOCKER', 'REVIEW_REQUIRED',
  'MISSING_CAPTURE', 'UNREPRESENTABLE_OVERLAY', 'MISSING_ORACLE',
  'ASPECT_MISMATCH', 'LOW_CONTENT_ORACLE', 'STALE_CAPTURE',
  'DUPLICATE_CAPTURE', 'CAPTURE_PATH_UNCONTAINED', 'CAPTURE_IS_ORACLE_COPY',
  'CAPTURE_LOCALE_MISMATCH', 'NO_INDEXED_SCREENS',
])
const FINAL_HASH_REQUIRED = new Set(['screen-cache', 'check-spec', 'capture-config', 'spec', 'spec-compare', 'screenshot'])
const REPORT_SCHEMAS = {
  'screen-cache': 'screen-cache-report.schema.json',
  'check-spec': 'check-spec-report.schema.json',
  'capture-config': 'capture-config-report.schema.json',
  census: 'census.schema.json',
  // The agent-authored spec report gets a substance floor too: mode 'gate' + a non-empty
  // per-screen verdict list (spec-fidelity-gate.md §4). Without it an empty `screens: []`
  // PASS envelope certified nothing at the final stage.
  spec: 'spec-report.schema.json',
  'spec-compare': 'spec-compare-report.schema.json',
  screenshot: 'screenshot-compare-report.schema.json',
}
// REVIEW_REQUIRED included (R2-1): a review row's artifacts are EXACTLY what the owner will
// judge — they must be present + hash-bound like any compared row's.
const SCREENSHOT_ARTIFACT_RESULT_STATUSES = new Set(['PASS', 'MINOR', 'MAJOR', 'BLOCKER', 'LOW_CONTENT_ORACLE', 'REVIEW_REQUIRED'])
const SCREENSHOT_ARTIFACT_KINDS = ['figma', 'actual', 'diff', 'overlay']
// Judge-strictness canon for the FINAL screenshot gate. compare-screenshots.mjs reads its
// severity thresholds from agent-settable env knobs (SCREENSHOT_*_THRESHOLD,
// SCREENSHOT_ASPECT_TOLERANCE, SCREENSHOT_MIN_COVERAGE) and records
// what was actually used in the report (top-level `metric` + `thresholds` via the extra
// spread). Nothing downstream re-checked them, so a weakened compare run (e.g.
// SCREENSHOT_PASS_THRESHOLD=0.2) could launder a grossly divergent screen into a PASS
// digest. The final bundle fails closed unless the recorded knobs are equal-or-stricter
// than this canon. The canonical VALUES come from the SAME committed
// screenshot-thresholds.json that compare-screenshots.mjs derives its defaults from —
// lock-step by construction, no hand-mirrored numbers; the gte/lte DIRECTIONS are
// semantics and stay in code.
const TCFG = loadScreenshotThresholds()
const CANONICAL_SCREENSHOT_METRIC = TCFG.metric
const CANONICAL_SCREENSHOT_THRESHOLDS = [
  // [thresholds key, canonical value, direction the recorded value must satisfy]
  // Direction picks the STRICTER side: a PASS/MINOR/MAJOR floor must be gte (raising the bar
  // is stricter is HARDER for the compare to reach a PASS band); a tolerance/coverage/ΔE knob
  // must be lte (a smaller value is stricter). minCoverage is lte because the #20 color-only
  // fallback fires when coverage < minCoverage — a RAISED minCoverage forces MORE screens into
  // the soft whole-frame-mean-ΔE path (SCREENSHOT_MIN_COVERAGE=1 forces EVERY screen), so a
  // recorded value ABOVE the committed minCoverage is the weakening; the recorded knob must be <= canon.
  // bgTolerance/deltaEPass gate the same color-only path (SCREENSHOT_BG_TOLERANCE=442 makes
  // every pixel background → uniform → color-only; SCREENSHOT_DELTAE_PASS=100 passes any
  // divergence), so both are lte too. Both sides read the same committed config — nothing to hand-sync.
  ['pass', TCFG.pass, 'gte'],
  ['minor', TCFG.minor, 'gte'],
  ['major', TCFG.major, 'gte'],
  ['aspectTolerance', TCFG.aspectTolerance, 'lte'],
  ['minCoverage', TCFG.minCoverage, 'lte'],
  ['bgTolerance', TCFG.bgTolerance, 'lte'],
  ['deltaEPass', TCFG.deltaEPass, 'lte'],
  // Metric-INTERNAL knobs (proven bypass): SCREENSHOT_SHIFT_RADIUS=4 + _GAUSSIAN_SIGMA=3 +
  // _AA_TOLERANCE=3 turned a genuine 0.395 BLOCKER into PASS 1.000 — a wider shift search
  // absorbs real misalignment, a fatter Gaussian blurs structure away, and a raised AA slack
  // whitewashes the diff/overlay. All three weaken UPWARD, so lte. varFloor is lte too — the
  // variance arm of the union mask keeps only pixels with localVar > VAR_FLOOR, so a RAISED
  // floor (e.g. SCREENSHOT_VAR_FLOOR=65025) hides dark-on-dark content from the mask entirely
  // and degrades deleted panels to the soft color-only path (the second proven bypass).
  // deltaEStride subsamples the ΔE00 colour axis; a raised stride starves the colour witness.
  ['shiftRadius', TCFG.shiftRadius, 'lte'],
  ['gaussianSigma', TCFG.gaussianSigma, 'lte'],
  ['aaTolerance', TCFG.aaTolerance, 'lte'],
  ['varFloor', TCFG.varFloor, 'lte'],
  ['deltaEStride', TCFG.deltaEStride, 'lte'],
  // H3 zone-gate (now a real default blocker). zoneBlocker is the per-NON-TEXT-zone SSIM floor:
  // a LOWERED floor lets a genuinely-broken non-text zone slip through, so gte. minRegionPx is
  // the min content px a zone/cell needs to be gate-eligible: a RAISED value skips small zones
  // (a broken 20x20 icon), so lte. zoneTextBlocker is deliberately NOT enforced here — it is the
  // lenient TEXT floor, so a RAISED text floor is STRICTER (fine) and a LOWERED one only widens
  // by-design font-AA leniency on text STRUCTURE (covered independently by spec-compare + ΔE),
  // never a non-text bypass vector. Both sides read the same committed config — nothing to hand-sync.
  ['zoneBlocker', TCFG.zoneBlocker, 'gte'],
  ['minRegionPx', TCFG.minRegionPx, 'lte'],
  // R1 device-chrome band exclusion (screen kind). statusBarDp/navBarDp bound the normal
  // top/bottom system-chrome exclusions; an unresolved geometry-only top strip disables the top
  // exclusion but can never expand it. A RAISED band over-masks (fewer compared pixels → weaker → easier PASS,
  // and past a point forces the soft color-only path), so both are lte their defaults; 0 disables
  // the band (compares MORE = stricter, always allowed). Both sides read the same
  // committed config — nothing to hand-sync.
  ['statusBarDp', TCFG.statusBarDp, 'lte'],
  ['navBarDp', TCFG.navBarDp, 'lte'],
  // W5-3 extra-content dilation ring: unmasked pixels within ringPx of the content-mask edge
  // are skipped by the extra-content probe (halo/gradient spill is not render-added content).
  // A RAISED ring skips MORE probe space (weaker — wide enough it blinds the whole probe), so
  // lte; 0 disables the ring (probes MORE = stricter, always allowed).
  ['extraContentRingPx', TCFG.extraContentRingPx, 'lte'],
]
const SHA256_RE = /^sha256:[a-f0-9]{64}$/i
const ARTIFACT_FILE_BY_KIND = { figma: 'figma.png', actual: 'actual.png', diff: 'diff.png', overlay: 'overlay.png', manifest: 'manifest.json' }

function issue(severity, issueKind, message, extra = {}) {
  return Object.assign({ severity, issueKind, message }, extra)
}

function readJsonSafe(path) {
  try { return JSON.parse(readFileSync(path, 'utf8')) } catch { return null }
}

function statusRank(status) {
  const s = String(status || '').toUpperCase()
  if (['BLOCKER', 'FAIL', 'ERROR'].includes(s)) return 4
  if (['INCOMPLETE', 'REVIEW_REQUIRED'].includes(s)) return 3
  if (['WARN', 'WARNING', 'MAJOR'].includes(s)) return 2
  if (['MINOR', 'SKIPPED'].includes(s)) return 1
  if (s === 'PASS') return 0
  return 4
}

function overallOf(rows) {
  const max = Math.max(0, ...rows.map((r) => statusRank(r.overall)))
  if (max >= 4) return 'BLOCKER'
  // Rank 3 splits: INCOMPLETE (something did not run — re-run it) vs REVIEW_REQUIRED (R2-1:
  // everything ran; a human decision is the missing input). Both are non-shippable, but the
  // site/board route them to DIFFERENT remedies, so the label must survive. INCOMPLETE wins
  // a tie — an un-run gate is the stronger problem.
  if (max === 3) return rows.some((r) => String(r.overall).toUpperCase() === 'INCOMPLETE') ? 'INCOMPLETE' : 'REVIEW_REQUIRED'
  if (max === 2) return 'WARN'
  return 'PASS'
}

function severity(issue) {
  return String((issue && issue.severity) || '').toUpperCase()
}

function reportIssueCounts(data) {
  const rows = Array.isArray(data && data.issues) ? data.issues : []
  return {
    blockingCount: rows.filter((i) => ['BLOCKER', 'ERROR', 'FAIL'].includes(severity(i))).length,
    warningCount: rows.filter((i) => ['WARN', 'WARNING', 'MINOR', 'MAJOR', 'REVIEW_REQUIRED'].includes(severity(i))).length,
  }
}

function reportConsistencyIssues(report, stage) {
  const issues = []
  const data = report.data || {}
  const overall = String(data.overall || '').toUpperCase()
  const counts = reportIssueCounts(data)
  const storedBlocking = Number(data.blockingCount || 0)
  const storedWarning = Number(data.warningCount || 0)
  if (storedBlocking !== counts.blockingCount) {
    issues.push(issue('BLOCKER', 'REPORT_BLOCKING_COUNT_MISMATCH', `${report.name} report blockingCount ${storedBlocking} does not match issues ${counts.blockingCount}`, { file: report.path, reportName: report.name }))
  }
  if (storedWarning !== counts.warningCount) {
    issues.push(issue('BLOCKER', 'REPORT_WARNING_COUNT_MISMATCH', `${report.name} report warningCount ${storedWarning} does not match issues ${counts.warningCount}`, { file: report.path, reportName: report.name }))
  }
  // INCOMPLETE / REVIEW_REQUIRED are legitimately blocking-BUT-incomplete overalls: a gate-mode
  // missing/stale/aspect-mismatch capture emits a BLOCKER-severity issue (so the gate exits non-zero)
  // while overallFor deliberately reports INCOMPLETE ("evidence incomplete", not "comparison failed").
  // So blockingCount>0 with an INCOMPLETE/REVIEW_REQUIRED overall is NOT a contradiction (and never a
  // fail-open — both still block ship). Only a benign PASS/WARN/SKIPPED overall hiding blockers is. This
  // matches the per-result taxonomy on line ~111.
  if (counts.blockingCount && !['BLOCKER', 'FAIL', 'ERROR', 'INCOMPLETE', 'REVIEW_REQUIRED'].includes(overall)) {
    issues.push(issue('BLOCKER', 'REPORT_OVERALL_CONTRADICTS_BLOCKERS', `${report.name} report overall ${overall || '(missing)'} contradicts blocking issues`, { file: report.path, reportName: report.name }))
  }
  if (!counts.blockingCount && counts.warningCount && overall === 'PASS') {
    issues.push(issue('BLOCKER', 'REPORT_OVERALL_CONTRADICTS_WARNINGS', `${report.name} report overall PASS contradicts warning issues`, { file: report.path, reportName: report.name }))
  }
  if (stage === 'final' && overall === 'SKIPPED') {
    issues.push(issue('BLOCKER', 'REPORT_REQUIRED_SKIPPED', `${report.name} report is required for final evidence but was skipped`, { file: report.path, reportName: report.name }))
  }
  if (stage === 'final' && !['census'].includes(report.name) && data.mode !== 'gate') {
    issues.push(issue('BLOCKER', 'REPORT_NOT_GATE_MODE', `${report.name} report is required for final evidence but mode is ${JSON.stringify(data.mode)}`, { file: report.path, reportName: report.name }))
  }
  if (stage === 'final' && data.gatePolicyVersion !== TCFG.version) {
    issues.push(issue('BLOCKER', 'REPORT_GATE_POLICY_STALE', `${report.name} report gatePolicyVersion ${JSON.stringify(data.gatePolicyVersion)} does not match current policy ${TCFG.version} — regenerate this report; old-policy evidence is never upgraded implicitly`, { file: report.path, reportName: report.name }))
  }
  if (stage === 'final' && report.name === 'capture-config' && Array.isArray(data.designLocaleEnvOverrides) && data.designLocaleEnvOverrides.length) {
    issues.push(issue('BLOCKER', 'LOCALE_ENV_OVERRIDE', `capture-config ran under fixture-only design-locale env override(s) ${data.designLocaleEnvOverrides.join(', ')} — unset every FIGMA_* locale override and re-run the screenshot + final stages`, { file: report.path, reportName: report.name }))
  }
  if (stage === 'final' && report.name === 'screenshot') {
    const inputs = data.inputs || {}
    if (!inputs.captureStartedAt) {
      issues.push(issue('BLOCKER', 'SCREENSHOT_FRESH_CAPTURE_EVIDENCE_MISSING', 'screenshot report must carry a captureStartedAt lower bound; the manifest proves membership/identity, not freshness', { file: report.path, reportName: report.name }))
    } else {
      const raw = inputs.captureStartedAt
      const startedAt = /^\d+$/.test(String(raw)) ? Number(raw) : Date.parse(raw)
      const generatedAt = Date.parse(data.generatedAt || '')
      if (!Number.isFinite(startedAt) || startedAt <= 0 || !Number.isFinite(generatedAt) || startedAt < generatedAt - 24 * 60 * 60 * 1000 || startedAt > generatedAt + 5 * 60 * 1000) {
        issues.push(issue('BLOCKER', 'SCREENSHOT_CAPTURE_BOUND_IMPLAUSIBLE', `screenshot captureStartedAt ${JSON.stringify(raw)} is not a valid recent lower bound for report generatedAt ${JSON.stringify(data.generatedAt)} — re-record and re-run the screenshot gate`, { file: report.path, reportName: report.name }))
      }
    }
    if (inputs.captureMode === 'preexisting') {
      issues.push(issue('BLOCKER', 'SCREENSHOT_SKIP_RECORD_NONCERTIFYING', 'screenshot evidence came from --skip-record: its caller-supplied timestamp can support diagnostics but cannot prove a recorder run; re-run the screenshot stage without --skip-record', { file: report.path, reportName: report.name }))
    } else if (inputs.captureMode !== 'recorded') {
      issues.push(issue('BLOCKER', 'SCREENSHOT_RECORD_PROVENANCE_MISSING', `screenshot evidence captureMode must be exactly "recorded" for final certification, got ${JSON.stringify(inputs.captureMode)} — run the canonical screenshot stage without --skip-record`, { file: report.path, reportName: report.name }))
    }
    // R5-3 — the fixture-only FIGMA_* design-locale env overrides can redirect or disarm the
    // CAPTURE_LOCALE_MISMATCH witness (the same class of hole as a SCREENSHOT_* threshold
    // weakening, which THRESHOLDS_WEAKENED owns). The comparator records them unconditionally;
    // a run that used any may never certify final evidence.
    if (Array.isArray(data.designLocaleEnvOverrides) && data.designLocaleEnvOverrides.length) {
      issues.push(issue('BLOCKER', 'LOCALE_ENV_OVERRIDE', `screenshot compare ran under fixture-only design-locale env override(s) ${data.designLocaleEnvOverrides.join(', ')} — they can redirect or disarm the capture-locale witness; unset every FIGMA_* locale override and re-run the compare + final bundle`, { file: report.path, reportName: report.name }))
    }
    for (const result of Array.isArray(data.results) ? data.results : []) {
      const status = String(result && result.status || '').toUpperCase()
      if (!KNOWN_SCREENSHOT_RESULT_STATUS.has(status)) {
        issues.push(issue('BLOCKER', 'REPORT_SCREENSHOT_RESULT_UNKNOWN', `screenshot result has unknown status ${JSON.stringify(result && result.status)}`, { file: report.path, reportName: report.name, screen: result && result.screen || undefined, theme: result && result.theme || null }))
        continue
      }
      // CAPTURE_IS_ORACLE_COPY / CAPTURE_PATH_UNCONTAINED are hard tamper blockers in
      // compare-screenshots (a copied oracle / an uncontained capture path); like BLOCKER and
      // DUPLICATE_CAPTURE they MUST coincide with a blocking overall, so a hand-edited report
      // that keeps the row but strips its issue (leaving a PASS/WARN overall) is caught here.
      if (['BLOCKER', 'DUPLICATE_CAPTURE', 'CAPTURE_IS_ORACLE_COPY', 'CAPTURE_PATH_UNCONTAINED'].includes(status) && !['BLOCKER', 'FAIL', 'ERROR'].includes(overall)) {
        issues.push(issue('BLOCKER', 'REPORT_SCREENSHOT_RESULT_CONTRADICTS_OVERALL', `screenshot result ${status} contradicts report overall ${overall || '(missing)'}`, { file: report.path, reportName: report.name, screen: result.screen || undefined, theme: result.theme || null }))
      }
      if (['INCOMPLETE', 'MISSING_CAPTURE', 'UNREPRESENTABLE_OVERLAY', 'MISSING_ORACLE', 'ASPECT_MISMATCH', 'LOW_CONTENT_ORACLE', 'STALE_CAPTURE', 'CAPTURE_LOCALE_MISMATCH', 'NO_INDEXED_SCREENS', 'REVIEW_REQUIRED'].includes(status) && !['INCOMPLETE', 'REVIEW_REQUIRED', 'BLOCKER', 'FAIL', 'ERROR'].includes(overall)) {
        issues.push(issue('BLOCKER', 'REPORT_SCREENSHOT_RESULT_CONTRADICTS_OVERALL', `screenshot result ${status} contradicts report overall ${overall || '(missing)'}`, { file: report.path, reportName: report.name, screen: result.screen || undefined, theme: result.theme || null }))
      }
      if (['MINOR', 'MAJOR'].includes(status) && overall === 'PASS') {
        issues.push(issue('BLOCKER', 'REPORT_SCREENSHOT_RESULT_CONTRADICTS_PASS', `screenshot result ${status} requires a warning overall, not PASS`, { file: report.path, reportName: report.name, screen: result.screen || undefined, theme: result.theme || null }))
      }
    }
  }
  if (stage === 'final' && report.name === 'spec-compare' && overall === 'PASS') {
    const review = (Array.isArray(data.comparisons) ? data.comparisons : []).find((row) => String(row && row.status || '').toUpperCase() === 'REVIEW')
    if (review) {
      issues.push(issue('BLOCKER', 'REPORT_SPEC_COMPARE_REVIEW_CONTRADICTS_PASS', 'spec-compare comparison status REVIEW contradicts report overall PASS', { file: report.path, reportName: report.name, screen: review.screen || undefined, theme: review.theme || null, stableId: review.stableId || undefined, elementName: review.elementName || undefined }))
    }
  }
  return issues
}

// Fail-closed strictness validation: the recorded metric must be the canonical one and
// every recorded threshold must be equal-or-stricter than canon; missing/unparseable fields
// block too (an unrecorded knob cannot prove the comparison ran at canonical strictness).
// `majorBand`, `extraContentBand`, `extraContentWarn`, and `extraContentDeltaE` are enforced
// as required evidence — compare-screenshots.mjs always records SCREENSHOT_MAJOR_BAND,
// SCREENSHOT_EXTRA_CONTENT_BAND, SCREENSHOT_EXTRA_CONTENT_WARN, and
// SCREENSHOT_EXTRA_CONTENT_DELTAE, so a `majorBand: advisory` (rollback), an
// `extraContentBand: off` (silences the render-extra-content witness), a raised
// `extraContentWarn`, or a raised `extraContentDeltaE` (widens the probe's equal-luma
// colour blind band) all fail the final gate. The numeric CANONICAL_SCREENSHOT_THRESHOLDS set
// also covers minCoverage/bgTolerance/deltaEPass — the three knobs the #20 color-only fallback
// reads — closing the SCREENSHOT_MIN_COVERAGE=1 / _BG_TOLERANCE=442 / _DELTAE_PASS=100 bypass.
// H3 zone-gate (now default ON) joins the closed set: `zoneGate` must be true (a bool check like
// majorBand — SCREENSHOT_ZONE_GATE=0 is a new bypass of the per-zone floor), zoneBlocker
// (numeric) gte 0.35, minRegionPx (numeric) lte 400. zoneTextBlocker is NOT enforced — a raised
// text floor is stricter and a lowered one only affects by-design text leniency (covered
// independently by spec-compare + the ΔE axis), never a non-text bypass vector.
function screenshotStrictnessIssues(report, stage) {
  const issues = []
  if (stage !== 'final' || report.name !== 'screenshot') return issues
  const data = report.data || {}
  const base = { file: report.path, reportName: report.name }
  if (data.metric !== CANONICAL_SCREENSHOT_METRIC) {
    issues.push(issue('BLOCKER', 'METRIC_MISMATCH', `screenshot report metric ${JSON.stringify(data.metric)} is not the required ${CANONICAL_SCREENSHOT_METRIC}`, base))
  }
  const thresholds = data.thresholds
  if (!thresholds || typeof thresholds !== 'object' || Array.isArray(thresholds)) {
    issues.push(issue('BLOCKER', 'THRESHOLDS_UNRECORDED', 'screenshot report records no thresholds — cannot prove the comparison ran at canonical strictness; re-run compare-screenshots', base))
    return issues
  }
  for (const [key, canon, dir] of CANONICAL_SCREENSHOT_THRESHOLDS) {
    const v = thresholds[key]
    if (typeof v !== 'number' || !Number.isFinite(v)) {
      issues.push(issue('BLOCKER', 'THRESHOLDS_UNRECORDED', `screenshot report thresholds.${key} is unrecorded — cannot prove the comparison ran at canonical strictness`, base))
      continue
    }
    if (dir === 'gte' ? v < canon : v > canon) {
      issues.push(issue('BLOCKER', 'THRESHOLDS_WEAKENED', `screenshot report thresholds.${key}=${v} is weaker than the canonical ${dir === 'gte' ? '>=' : '<='} ${canon} — env-weakened thresholds cannot certify final evidence`, base))
    }
  }
  if (thresholds.majorBand === undefined) {
    issues.push(issue('BLOCKER', 'THRESHOLDS_UNRECORDED', 'screenshot report thresholds.majorBand is unrecorded — cannot prove the major pixel band ran in blocking mode before project-level pixelGate routing', base))
  } else if (thresholds.majorBand !== 'block') {
    issues.push(issue('BLOCKER', 'THRESHOLDS_WEAKENED', `screenshot report thresholds.majorBand=${JSON.stringify(thresholds.majorBand)} must be "block" at the final gate (SCREENSHOT_MAJOR_BAND=advisory is a rollback knob, not a shipping mode)`, base))
  }
  // H3 zone-gate is now ON by default, so turning it OFF is a NEW bypass: the content-weighted
  // global MEAN can average a single destroyed non-text node into a PASS, and the per-zone floor
  // is the only witness. compare-screenshots.mjs ALWAYS records thresholds.zoneGate (a bool), so
  // an absent value means a pre-recording/hand-stripped report (UNRECORDED) and a non-true value
  // means SCREENSHOT_ZONE_GATE=0 was set (WEAKENED). Mirror the majorBand/maskMode bool pattern.
  if (thresholds.zoneGate === undefined) {
    issues.push(issue('BLOCKER', 'THRESHOLDS_UNRECORDED', 'screenshot report thresholds.zoneGate is unrecorded — cannot prove the per-zone floor ran at canonical strictness', base))
  } else if (thresholds.zoneGate !== true) {
    issues.push(issue('BLOCKER', 'THRESHOLDS_WEAKENED', `screenshot report thresholds.zoneGate=${JSON.stringify(thresholds.zoneGate)} must be true at the final gate (SCREENSHOT_ZONE_GATE=0 disables the per-zone floor that catches a localized break the global mean hides — a rollback knob, not a shipping mode)`, base))
  }
  // The C1 render-extra-content witness must not be silenced at final. compare-screenshots.mjs
  // ALWAYS records all three keys (thresholds.extraContentBand + thresholds.extraContentWarn +
  // thresholds.extraContentDeltaE), so treat absence as UNRECORDED and weak values as WEAKENED:
  // `off` disables the
  // RENDER_EXTRA_CONTENT probe (a rogue panel over the oracle background can then ship as a
  // clean PASS), a raised warn floor widens the blind band, and a raised ΔE00 floor blinds the
  // probe's equal-luma colour arm the same way. All three are env-settable
  // (SCREENSHOT_EXTRA_CONTENT_BAND/_WARN/_DELTAE).
  if (thresholds.extraContentBand === undefined) {
    issues.push(issue('BLOCKER', 'THRESHOLDS_UNRECORDED', 'screenshot report thresholds.extraContentBand is unrecorded — cannot prove the render-extra-content witness ran', base))
  } else if (thresholds.extraContentBand === 'off') {
    issues.push(issue('BLOCKER', 'THRESHOLDS_WEAKENED', 'screenshot report thresholds.extraContentBand="off" disables the render-extra-content witness at the final gate (SCREENSHOT_EXTRA_CONTENT_BAND=off is a calibration knob, not a shipping mode)', base))
  }
  if (thresholds.extraContentWarn === undefined) {
    issues.push(issue('BLOCKER', 'THRESHOLDS_UNRECORDED', 'screenshot report thresholds.extraContentWarn is unrecorded — cannot prove the render-extra-content floor ran at canonical strictness', base))
  } else if (typeof thresholds.extraContentWarn !== 'number' || !Number.isFinite(thresholds.extraContentWarn) || thresholds.extraContentWarn > TCFG.extraContentWarn) {
    issues.push(issue('BLOCKER', 'THRESHOLDS_WEAKENED', `screenshot report thresholds.extraContentWarn=${JSON.stringify(thresholds.extraContentWarn)} is weaker than the canonical <= ${TCFG.extraContentWarn} — a raised render-extra-content floor cannot certify final evidence`, base))
  }
  if (thresholds.extraContentDeltaE === undefined) {
    issues.push(issue('BLOCKER', 'THRESHOLDS_UNRECORDED', 'screenshot report thresholds.extraContentDeltaE is unrecorded — cannot prove the render-extra-content colour witness ran at canonical strictness', base))
  } else if (typeof thresholds.extraContentDeltaE !== 'number' || !Number.isFinite(thresholds.extraContentDeltaE) || thresholds.extraContentDeltaE > TCFG.extraContentDeltaE) {
    issues.push(issue('BLOCKER', 'THRESHOLDS_WEAKENED', `screenshot report thresholds.extraContentDeltaE=${JSON.stringify(thresholds.extraContentDeltaE)} is weaker than the canonical <= ${TCFG.extraContentDeltaE} — a raised equal-luma colour floor blinds the render-extra-content witness and cannot certify final evidence`, base))
  }
  // maskMode is REQUIRED (like the numeric set): compare-screenshots.mjs records it
  // unconditionally, so an absent value means a pre-recording/hand-stripped report. 'color'
  // drops the variance arm (dark-on-dark content vanishes from the mask — the proven
  // panels-deleted bypass); 'edge' drops the color-key arm (solid fills vanish). Only the
  // union ('variance') can certify final evidence.
  if (thresholds.maskMode === undefined) {
    issues.push(issue('BLOCKER', 'THRESHOLDS_UNRECORDED', 'screenshot report thresholds.maskMode is unrecorded — cannot prove the comparison ran at canonical strictness', base))
  } else if (thresholds.maskMode !== 'variance') {
    issues.push(issue('BLOCKER', 'THRESHOLDS_WEAKENED', `screenshot report thresholds.maskMode=${JSON.stringify(thresholds.maskMode)} must be "variance" at the final gate (color/edge drop one arm of the union content mask, hiding content from the score)`, base))
  }
  // colorAxis is recorded unconditionally too; SCREENSHOT_COLOR_AXIS=0 silences the equal-luma
  // ΔE00 colour witness (COLOR_DRIFT_REVIEW + the per-region/per-node colour advisory) — a
  // token/hue drift that keeps structure intact then leaves no trace. Advisory-grade, but the
  // witness must stay ON at final; mirror the maskMode bool pattern.
  if (thresholds.colorAxis === undefined) {
    issues.push(issue('BLOCKER', 'THRESHOLDS_UNRECORDED', 'screenshot report thresholds.colorAxis is unrecorded — cannot prove the colour-drift witness ran at canonical strictness', base))
  } else if (thresholds.colorAxis !== true) {
    issues.push(issue('BLOCKER', 'THRESHOLDS_WEAKENED', `screenshot report thresholds.colorAxis=${JSON.stringify(thresholds.colorAxis)} must be true at the final gate (SCREENSHOT_COLOR_AXIS=0 silences the equal-luma ΔE00 colour-drift witness — a rollback knob, not a shipping mode)`, base))
  }
  // pixelGate (R3) is the per-project pixel-VERDICT routing knob {strict|advisory|off}, recorded
  // unconditionally by compare-screenshots. It is NOT canon-forced to a fixed value — the
  // COMMITTED project-config `screenshotPixelGate` is a legitimate per-project choice. But the
  // RECORDED routing may never be WEAKER than that committed choice (R2-2): a per-run
  // SCREENSHOT_PIXEL_GATE=advisory on a strict project used to certify silently — the exact
  // env-downgrade class every other SCREENSHOT_* knob is already netted for. Stricter than
  // committed is always fine (the tighten-only task override lands as `strict`). An
  // absent/garbage value means a pre-recording or hand-tampered report that cannot prove
  // which routing ran.
  const PIXEL_GATE_RANK = { off: 0, advisory: 1, strict: 2 }
  const committedPixelGate = (() => {
    try { const v = readConfig('screenshotPixelGate'); return PIXEL_GATE_RANK[v] !== undefined ? v : 'strict' } catch { return 'strict' }
  })()
  if (thresholds.pixelGate === undefined) {
    issues.push(issue('BLOCKER', 'THRESHOLDS_UNRECORDED', 'screenshot report thresholds.pixelGate is unrecorded — cannot prove which pixel-verdict routing ran', base))
  } else if (!['strict', 'advisory', 'off'].includes(thresholds.pixelGate)) {
    issues.push(issue('BLOCKER', 'THRESHOLDS_WEAKENED', `screenshot report thresholds.pixelGate=${JSON.stringify(thresholds.pixelGate)} must be one of strict|advisory|off`, base))
  } else if (PIXEL_GATE_RANK[thresholds.pixelGate] < PIXEL_GATE_RANK[committedPixelGate]) {
    issues.push(issue('BLOCKER', 'THRESHOLDS_WEAKENED', `screenshot report thresholds.pixelGate=${JSON.stringify(thresholds.pixelGate)} is weaker than the committed project-config screenshotPixelGate=${JSON.stringify(committedPixelGate)} — a per-run SCREENSHOT_PIXEL_GATE downgrade cannot certify final evidence (unset the override and re-run the compare)`, base))
  }
  // regionGrid is REQUIRED too, but the strictness check applies only to a simple RxC value:
  // compare-screenshots' parseGrid only ever records a normalized 'RxC' string (garbage env is
  // exit-1 at the compare), so a non-matching value cannot come from the real pipeline and has
  // no defensible weaker/stricter direction — skip rather than guess. Both directions weaken:
  // fewer cells than the canonical 8x4 (32) coarsens per-region scores and the H3 zone floor;
  // a FINER grid (more rows/cols) shrinks every cell below the minRegionPx eligibility floor on
  // dp-sized frames (390×844 @ 32x32 ≈ 321px < 400), silently disabling the zone-gate's grid
  // fallback — the only zone floor when the spec projects no zones. So only the canonical 8x4
  // certifies.
  if (thresholds.regionGrid === undefined) {
    issues.push(issue('BLOCKER', 'THRESHOLDS_UNRECORDED', 'screenshot report thresholds.regionGrid is unrecorded — cannot prove the comparison ran at canonical strictness', base))
  } else {
    const grid = /^(\d+)x(\d+)$/.exec(String(thresholds.regionGrid))
    if (grid && (+grid[1]) * (+grid[2]) < 32) {
      issues.push(issue('BLOCKER', 'THRESHOLDS_WEAKENED', `screenshot report thresholds.regionGrid=${JSON.stringify(thresholds.regionGrid)} is coarser than the canonical 8x4 (32 cells) — a coarse grid degrades per-region resolution and cannot certify final evidence`, base))
    } else if (grid && ((+grid[1]) > 8 || (+grid[2]) > 4)) {
      issues.push(issue('BLOCKER', 'THRESHOLDS_WEAKENED', `screenshot report thresholds.regionGrid=${JSON.stringify(thresholds.regionGrid)} is finer than the canonical 8x4 — undersized cells fall below minRegionPx and neutralize the worst-zone grid fallback, so a finer grid cannot certify final evidence`, base))
    }
  }
  return issues
}

function collectReport(reportsDir, stem, prefix) {
  const path = join(reportsDir, `${prefix}-${stem}.json`)
  const publicPath = displayPath(path) || basename(path)
  if (!existsSync(path)) return { name: prefix, path: publicPath, absPath: path, exists: false, hash: null, overall: 'MISSING' }
  const json = readJsonSafe(path)
  return {
    name: prefix,
    path: publicPath,
    absPath: path,
    exists: true,
    hash: fileHash(path),
    overall: (json && json.overall) || 'UNKNOWN',
    blockingCount: (json && json.blockingCount) || 0,
    warningCount: (json && json.warningCount) || 0,
    pipelineRunId: json && json.pipelineRunId,
    data: json,
  }
}

function collectScreenFiles(screensDir) {
  if (!existsSync(screensDir)) return []
  let files = []
  try { files = readdirSync(screensDir).filter((f) => /\.(json|png)$/.test(f)).sort() } catch { files = [] }
  return files.map((f) => {
    const abs = join(screensDir, f)
    return { file: displayPath(abs) || basename(abs), name: basename(f), hash: fileHash(abs) }
  })
}

function publicReport(r) {
  const { data, absPath, ...out } = r
  return out
}

function resolveInputPath(path) {
  const p = String(path || '')
  if (!p) return ''
  if (isAbsolute(p)) return p
  const candidates = [
    resolve(PROJECT_ROOT, p),
    resolve(FIGMA_CACHE_ROOT, p),
  ]
  return candidates.find((candidate) => existsSync(candidate)) || candidates[0]
}

// `virtual:` inputHashes keys are recompute-only pins (not file paths): the value is a
// digest over DERIVED data the report consulted, re-derived here from live inputs and
// compared. Today the one such key is the census registry-consult digest — it scopes the
// registry pin to the entries the census actually matched, so an unrelated later upsert no
// longer retro-blocks the final gate, while a change to a CONSULTED entry (or a new match
// for a previously-unmatched name) still does. Capture-config uses the same mechanism to pin
// discovery-set membership (new capture tests/resources/bindings), not only already-found files.
// Unknown virtual keys fail closed.
function virtualInputIssues(report, key, expected, componentOptions) {
  const issues = []
  if (typeof expected !== 'string' || !expected.startsWith('sha256:')) {
    issues.push(issue('BLOCKER', 'REPORT_INPUT_HASH_INVALID', `${report.name} report virtual input ${key} has invalid digest ${JSON.stringify(expected)}`, { file: key, reportName: report.name }))
    return issues
  }
  if (key === MAPPING_CONSULT_KEY && report.name === 'census') {
    const consult = report.data && report.data.mappingConsult
    if (!consult || consult.version !== 2 || !Array.isArray(consult.setIds) || consult.digest !== expected) {
      issues.push(issue('BLOCKER', 'REPORT_INPUT_HASHES_INVALID', `census report ${key} pin does not match its mappingConsult record — the report is inconsistent; re-run component-census.mjs`, { file: report.path, reportName: report.name }))
      return issues
    }
    // Recompute against the LIVE truth exactly like the census read it: the
    // published design inventory + analysis and the project-owned registry.
    let truth
    try {
      const design = loadDesignComponentInventory(componentOptions)
      const mappings = loadComponentMappings(design.present ? design.inventory.scopeId : '', componentOptions)
      const analysis = loadPublishedComponentAnalysis(componentOptions)
      truth = { inventory: design.present ? design.inventory : null, registry: mappings.registry, analysis }
    } catch (error) {
      issues.push(issue('BLOCKER', 'REPORT_INPUT_HASH_MISMATCH', `component truth consulted by the census cannot be replayed: ${String(error && error.message || error).slice(0, 200)}`, { file: report.path, reportName: report.name }))
      return issues
    }
    const actual = computeMappingConsultDigest(consult.setIds, truth)
    if (actual !== expected) {
      issues.push(issue('BLOCKER', 'REPORT_INPUT_HASH_MISMATCH', `component identities consulted by the census changed since the report was written — re-run component-census.mjs under the pinned run id, then re-run this bundle`, { file: report.path, reportName: report.name, expectedHash: expected, actualHash: actual }))
    }
    return issues
  }
  if (key === CAPTURE_CONFIG_DISCOVERY_KEY && report.name === 'capture-config') {
    const witness = report.data && report.data.inputs && report.data.inputs.captureDiscovery
    if (!witness || witness.version !== 1 || witness.digest !== expected || !Array.isArray(witness.roots) || !witness.roots.length || typeof witness.screensDir !== 'string' || !witness.screensDir) {
      issues.push(issue('BLOCKER', 'REPORT_INPUT_HASHES_INVALID', `capture-config report ${key} pin does not match its inputs.captureDiscovery witness — re-run check-capture-config.mjs`, { file: report.path, reportName: report.name }))
      return issues
    }
    let supportedLocales
    try {
      supportedLocales = readSupportedLocales()
    } catch {
      issues.push(issue('BLOCKER', 'CAPTURE_LOCALE_SOURCE_UNAVAILABLE', 'supported locale configuration is missing, invalid, unreadable, unsafe, or not valid UTF-8', { file: report.path, reportName: report.name }))
      return issues
    }
    const actual = captureConfigDiscovery({ codeRoots: witness.roots, screensDir: witness.screensDir, supportedLocales })
    if (actual.digest !== expected) {
      issues.push(issue('BLOCKER', 'REPORT_INPUT_HASH_MISMATCH', 'capture-config discovery inputs changed since the report was written (capture tests, locale resources, index, or bindings) — re-run check-capture-config.mjs under the pinned run id', { file: report.path, reportName: report.name, expectedHash: expected, actualHash: actual.digest }))
    }
    for (const error of actual.errors) {
      issues.push(issue('BLOCKER', 'CAPTURE_DISCOVERY_UNREADABLE', `capture-config discovery cannot be replayed completely: ${displayPath(error.path)} (${error.code})`, { file: displayPath(error.path), reportName: report.name }))
    }

    // A narrowed witness proves freshness only for its own subset. Final evidence also
    // discovers the physical product root and requires every current capture-bearing test
    // and resource root to be represented by that subset. This preserves useful fixture and
    // multi-root scans without allowing FIGMA_CENSUS_CODE_ROOTS/--code-root to omit a module.
    const canonical = captureConfigDiscovery({ codeRoots: [PROJECT_ROOT], screensDir: witness.screensDir, supportedLocales })
    for (const error of canonical.errors) {
      issues.push(issue('BLOCKER', 'CAPTURE_DISCOVERY_CANONICAL_UNREADABLE', `canonical product capture-config discovery is incomplete: ${displayPath(error.path)} (${error.code})`, { file: displayPath(error.path), reportName: report.name }))
    }
    const omissions = captureConfigScopeOmissions(actual, canonical)
    const missingCaptures = omissions.captureFiles
    const missingResources = omissions.resourceRoots
    if (missingCaptures.length || missingResources.length) {
      const omitted = [...missingCaptures, ...missingResources].slice(0, 5).map((path) => displayPath(path))
      const more = missingCaptures.length + missingResources.length - omitted.length
      issues.push(issue('BLOCKER', 'CAPTURE_DISCOVERY_SCOPE_INCOMPLETE', `capture-config used narrowed code roots that omit ${missingCaptures.length} capture test file(s) and ${missingResources.length} resource root(s) from the canonical product scan: ${omitted.join(', ')}${more > 0 ? ` (+${more} more)` : ''}`, { file: report.path, reportName: report.name, omittedCaptureFiles: missingCaptures.map(displayPath), omittedResourceRoots: missingResources.map(displayPath) }))
    }
    return issues
  }
  issues.push(issue('BLOCKER', 'REPORT_INPUT_HASHES_INVALID', `${report.name} report carries unknown virtual input ${key}`, { file: key, reportName: report.name }))
  return issues
}

function reportFreshnessIssues(report, stage, componentOptions) {
  const issues = []
  const hashes = report.data && report.data.inputHashes
  if (!hashes || typeof hashes !== 'object' || Array.isArray(hashes)) {
    issues.push(issue('BLOCKER', 'REPORT_INPUT_HASHES_INVALID', `${report.name} report inputHashes must be an object`, { file: report.path, reportName: report.name }))
    return issues
  }
  if (stage === 'final' && FINAL_HASH_REQUIRED.has(report.name) && Object.keys(hashes).length === 0) {
    issues.push(issue('BLOCKER', 'REPORT_INPUT_HASHES_EMPTY', `${report.name} report is required for final evidence but carries no input hashes`, { file: report.path, reportName: report.name }))
  }
  if (stage === 'final' && report.name === 'capture-config' && !Object.prototype.hasOwnProperty.call(hashes, CAPTURE_CONFIG_DISCOVERY_KEY)) {
    issues.push(issue('BLOCKER', 'REPORT_INPUT_DISCOVERY_MISSING', `capture-config report carries no ${CAPTURE_CONFIG_DISCOVERY_KEY} witness — it cannot prove that no capture test/resource/binding was added after the scan`, { file: report.path, reportName: report.name }))
  }
  for (const [path, expected] of Object.entries(hashes)) {
    if (path.startsWith('virtual:')) {
      issues.push(...virtualInputIssues(report, path, expected, componentOptions))
      continue
    }
    const publicInput = displayPath(resolveInputPath(path))
    if (expected === null) {
      issues.push(issue(stage === 'final' ? 'BLOCKER' : 'WARN', 'REPORT_INPUT_MISSING', `${report.name} report input ${publicInput} was missing when the report was written`, { file: publicInput, reportName: report.name }))
      continue
    }
    if (typeof expected !== 'string' || !expected.startsWith('sha256:')) {
      issues.push(issue('BLOCKER', 'REPORT_INPUT_HASH_INVALID', `${report.name} report input ${publicInput} has invalid hash ${JSON.stringify(expected)}`, { file: publicInput, reportName: report.name }))
      continue
    }
    const abs = resolveInputPath(path)
    const actual = fileHash(abs)
    if (actual !== expected) {
      issues.push(issue('BLOCKER', 'REPORT_INPUT_HASH_MISMATCH', `${report.name} report input ${displayPath(abs)} hash changed`, { file: displayPath(abs), reportName: report.name, expectedHash: expected, actualHash: actual }))
    }
  }
  return issues
}

function pushArtifactRefs(out, set) {
  if (!set || typeof set !== 'object') return
  if (set.manifest && typeof set.manifest === 'object') out.push(set.manifest)
  const artifacts = set.artifacts && typeof set.artifacts === 'object' ? set.artifacts : {}
  for (const ref of Object.values(artifacts)) if (ref && typeof ref === 'object') out.push(ref)
}

function artifactSetCompletenessIssues(report, result, set) {
  const issues = []
  const status = String(result && result.status || '').toUpperCase()
  const base = { file: report.path, reportName: report.name, screen: result && result.screen || undefined, theme: result && (result.theme || result.themeKey) || null }
  if (!set || typeof set !== 'object') {
    issues.push(issue('BLOCKER', 'SCREENSHOT_ARTIFACT_SET_MISSING', `screenshot result ${status} must carry saved compare artifacts`, base))
    return issues
  }
  const expectedScreen = String(result && result.screen || '')
  const expectedTheme = String(result && (result.theme || result.themeKey) || '')
  const setTheme = String(set.theme || set.themeKey || '')
  if (expectedScreen && set.screen && set.screen !== expectedScreen) {
    issues.push(issue('BLOCKER', 'SCREENSHOT_ARTIFACT_SET_MISMATCH', `screenshot result ${status} artifactSet screen ${JSON.stringify(set.screen)} does not match result screen ${JSON.stringify(expectedScreen)}`, base))
  }
  if (expectedTheme && setTheme && setTheme !== expectedTheme) {
    issues.push(issue('BLOCKER', 'SCREENSHOT_ARTIFACT_SET_MISMATCH', `screenshot result ${status} artifactSet theme ${JSON.stringify(setTheme)} does not match result theme ${JSON.stringify(expectedTheme)}`, base))
  }
  const parents = new Set()
  function rememberParent(ref) {
    const refPath = String(ref && ref.path || '').replace(/\\/g, '/')
    if (refPath) parents.add(dirname(refPath))
  }
  function checkRole(slot, ref, expectedKind) {
    if (!ref || typeof ref !== 'object') return
    const pathName = basename(String(ref.path || '').replace(/\\/g, '/'))
    rememberParent(ref)
    if (ref.kind !== expectedKind || pathName !== ARTIFACT_FILE_BY_KIND[expectedKind]) {
      issues.push(issue('BLOCKER', 'SCREENSHOT_ARTIFACT_SET_ROLE_INVALID', `screenshot result ${status} artifactSet slot ${slot} must reference ${ARTIFACT_FILE_BY_KIND[expectedKind]} with kind ${expectedKind}`, Object.assign({}, base, { path: ref.path || undefined })))
    }
    if (expectedScreen && ref.screen && ref.screen !== expectedScreen) {
      issues.push(issue('BLOCKER', 'SCREENSHOT_ARTIFACT_SET_MISMATCH', `screenshot result ${status} artifact ${slot} screen ${JSON.stringify(ref.screen)} does not match result screen ${JSON.stringify(expectedScreen)}`, Object.assign({}, base, { path: ref.path || undefined })))
    }
    const refTheme = String(ref.theme || ref.themeKey || '')
    if (expectedTheme && refTheme && refTheme !== expectedTheme) {
      issues.push(issue('BLOCKER', 'SCREENSHOT_ARTIFACT_SET_MISMATCH', `screenshot result ${status} artifact ${slot} theme ${JSON.stringify(refTheme)} does not match result theme ${JSON.stringify(expectedTheme)}`, Object.assign({}, base, { path: ref.path || undefined })))
    }
  }
  if (!set.manifest || typeof set.manifest !== 'object') {
    issues.push(issue('BLOCKER', 'SCREENSHOT_ARTIFACT_SET_INCOMPLETE', `screenshot result ${status} artifactSet is missing manifest`, base))
  } else {
    checkRole('manifest', set.manifest, 'manifest')
  }
  const artifacts = set.artifacts && typeof set.artifacts === 'object' ? set.artifacts : {}
  for (const kind of SCREENSHOT_ARTIFACT_KINDS) {
    if (!artifacts[kind] || typeof artifacts[kind] !== 'object') {
      issues.push(issue('BLOCKER', 'SCREENSHOT_ARTIFACT_SET_INCOMPLETE', `screenshot result ${status} artifactSet is missing ${kind}.png`, base))
    } else {
      checkRole(kind, artifacts[kind], kind)
    }
  }
  if (parents.size > 1) {
    issues.push(issue('BLOCKER', 'SCREENSHOT_ARTIFACT_SET_MISMATCH', `screenshot result ${status} artifactSet files must live in one artifact directory`, base))
  }
  const parent = parents.size === 1 ? Array.from(parents)[0] : ''
  if (set.id && parent && basename(parent) !== set.id) {
    issues.push(issue('BLOCKER', 'SCREENSHOT_ARTIFACT_SET_MISMATCH', `screenshot result ${status} artifactSet id ${JSON.stringify(set.id)} does not match artifact directory ${JSON.stringify(basename(parent))}`, base))
  }
  return issues
}

function artifactRefsFromReport(data) {
  const out = []
  if (!data || typeof data !== 'object') return out
  if (data.artifactSet && Array.isArray(data.artifactSet.entries)) {
    for (const ref of data.artifactSet.entries) if (ref && typeof ref === 'object') out.push(ref)
  }
  if (Array.isArray(data.artifactSets)) for (const set of data.artifactSets) pushArtifactRefs(out, set)
  if (Array.isArray(data.results)) for (const row of data.results) pushArtifactRefs(out, row && row.artifactSet)
  if (Array.isArray(data.issues)) for (const row of data.issues) pushArtifactRefs(out, row && row.artifactSet)
  return out.filter((ref) => ref && typeof ref.id === 'string')
}

function resolveArtifactPath(stem, ref) {
  const p = String(ref && ref.path || '')
  if (!p || isAbsolute(p)) return { error: 'path must be cache-relative' }
  const normalized = p.replace(/\\/g, '/')
  const parts = normalized.split('/')
  if (parts.some((seg) => !seg || seg === '.' || seg === '..')) return { error: 'path contains an unsafe segment' }
  if (parts[0] !== 'artifacts' || parts[1] !== 'screenshot' || parts[2] !== artifactSegment(stem)) return { error: 'path must stay under artifacts/screenshot/<stem>' }
  const runId = String(ref && ref.pipelineRunId || '')
  if (runId && parts[3] !== artifactSegment(runId)) return { error: 'path must stay under the report pipelineRunId' }
  const expectedExt = String(ref.kind || '') === 'manifest' ? '.json' : '.png'
  if (extname(normalized).toLowerCase() !== expectedExt) return { error: `path must end with ${expectedExt}` }
  const root = resolve(FIGMA_CACHE_ROOT, 'artifacts', 'screenshot')
  const target = resolve(FIGMA_CACHE_ROOT, normalized)
  if (target !== root && !target.startsWith(root + sep)) return { error: 'path escapes screenshot artifact root' }
  return { path: target, root }
}

function reportArtifactIssues(report, stage) {
  const issues = []
  if (stage !== 'final' || report.name !== 'screenshot') return issues
  const stem = String(report.data && report.data.taskStem || '')
  for (const result of Array.isArray(report.data && report.data.results) ? report.data.results : []) {
    const status = String(result && result.status || '').toUpperCase()
    if (SCREENSHOT_ARTIFACT_RESULT_STATUSES.has(status)) {
      issues.push(...artifactSetCompletenessIssues(report, result, result && result.artifactSet))
    }
  }
  const refs = artifactRefsFromReport(report.data)
  const byId = new Map()
  const seenRefs = new Set()
  const collisionIds = new Set()
  const reportRunId = String(report.data && report.data.pipelineRunId || '')
  for (const ref of refs) {
    if (typeof ref.id !== 'string' || !/^[A-Za-z0-9_-]{1,180}$/.test(ref.id)) {
      issues.push(issue('BLOCKER', 'SCREENSHOT_ARTIFACT_ID_INVALID', 'screenshot artifact id is invalid', { file: report.path, reportName: report.name }))
      continue
    }
    ref.pipelineRunId = reportRunId
    const refHash = typeof ref.hash === 'string' ? ref.hash.toLowerCase() : ref.hash
    const prior = byId.get(ref.id)
    if (prior && (prior.path !== ref.path || prior.hash !== refHash || prior.kind !== ref.kind)) {
      if (!collisionIds.has(ref.id)) {
        issues.push(issue('BLOCKER', 'SCREENSHOT_ARTIFACT_ID_COLLISION', `screenshot artifact id ${ref.id} points to multiple artifacts`, { file: report.path, reportName: report.name, path: ref.path }))
        collisionIds.add(ref.id)
      }
      continue
    }
    byId.set(ref.id, { path: ref.path, hash: refHash, kind: ref.kind })
    const refKey = [ref.id, ref.path, refHash, ref.kind].join('\u0001')
    if (seenRefs.has(refKey)) continue
    seenRefs.add(refKey)
    const resolved = resolveArtifactPath(stem, ref)
    if (resolved.error) {
      issues.push(issue('BLOCKER', 'SCREENSHOT_ARTIFACT_PATH_INVALID', `screenshot artifact ${ref.id} ${resolved.error}`, { file: report.path, reportName: report.name, path: ref.path }))
      continue
    }
    try {
      const lst = lstatSync(resolved.path)
      if (!lst.isFile()) throw new Error('not a regular file')
      const real = realpathSync(resolved.path)
      const realRoot = realpathSync(resolved.root)
      if (real !== realRoot && !real.startsWith(realRoot + sep)) throw new Error('realpath escapes artifact root')
      if (typeof ref.hash !== 'string' || !SHA256_RE.test(ref.hash)) {
        issues.push(issue('BLOCKER', 'SCREENSHOT_ARTIFACT_HASH_INVALID', `screenshot artifact ${ref.id} has invalid hash`, { file: displayPath(resolved.path), reportName: report.name, path: ref.path }))
        continue
      }
      const expectedHash = ref.hash.toLowerCase()
      const actual = fileHash(real)
      if (actual !== expectedHash) {
        issues.push(issue('BLOCKER', 'SCREENSHOT_ARTIFACT_HASH_MISMATCH', `screenshot artifact ${ref.id} hash changed`, { file: displayPath(real), reportName: report.name, path: ref.path, expectedHash, actualHash: actual }))
      }
    } catch (e) {
      issues.push(issue('BLOCKER', 'SCREENSHOT_ARTIFACT_MISSING', `screenshot artifact ${ref.id} is missing or unreadable: ${e.message}`, { file: displayPath(resolved.path), reportName: report.name, path: ref.path }))
    }
  }
  return issues
}

// Re-compute the task's CURRENT `## Design` section hash with the exact algorithm
// check-screen-cache.mjs uses (same file-lookup order + parseDesignSources), so the final
// bundle can prove the design refs did not change after the screen-cache gate ran. The
// section hash — unlike a whole-file hash — survives the legitimate post-gate mutations
// (## Outcome append, digest injection, todo/→done/ move), so it gates ONLY on real design
// edits. done/ is consulted only when no open-state task body exists, so a
// post-ship diagnostic re-run reads the exact body that was published.
function currentDesignSourceHash(stem) {
  const explicit = !!process.env.FIGMA_SCREEN_TASK_FILE
  const files = explicit ? [process.env.FIGMA_SCREEN_TASK_FILE] : [
    join(PROJECT_ROOT, 'orchestrator', 'tasks', 'todo', `${stem}.md`),
    join(PROJECT_ROOT, 'orchestrator', 'tasks', 'backlog', `${stem}.md`),
    join(PROJECT_ROOT, 'orchestrator', 'tasks', 'pending', `${stem}.questions.md`),
  ].filter((p) => existsSync(p))
  if (!files.length) {
    const done = join(PROJECT_ROOT, 'orchestrator', 'tasks', 'done', `${stem}.md`)
    if (existsSync(done)) files.push(done)
  }
  if (!files.length) throw new Error('current task source is missing')
  const bodies = []
  for (const file of files) {
    bodies.push(readTaskMarkdown(file, { explicit }))
  }
  return parseDesignSources(bodies).sourceHash
}

// The digest's `designHash` certifies the SINGLE canonical task body that becomes the done
// file — NOT the multi-file set currentDesignSourceHash() folds together. verify-done.mjs
// re-verifies it over `done/<stem>.md` ALONE, so the certification basis must be that one
// body too: at ship time the primary file is `todo/<stem>.md` (which is renamed to done/,
// byte-identical), so `parseDesignSources([todoBody])` here == `parseDesignSources([doneBody])`
// there. Using the multi-file set instead let a lingering `pending/<stem>.questions.md`
// sidecar fold an extra body into the digest that verify-done could never reproduce → a
// false "## Design edited after certification" block on a legitimately-shipped task.
function primaryBodyDesignHash(stem) {
  const explicit = !!process.env.FIGMA_SCREEN_TASK_FILE
  const candidates = explicit ? [process.env.FIGMA_SCREEN_TASK_FILE] : [
    join(PROJECT_ROOT, 'orchestrator', 'tasks', 'todo', `${stem}.md`),
    join(PROJECT_ROOT, 'orchestrator', 'tasks', 'done', `${stem}.md`),
    join(PROJECT_ROOT, 'orchestrator', 'tasks', 'backlog', `${stem}.md`),
  ]
  const file = candidates.find((p) => existsSync(p))
  if (!file) throw new Error('current task source is missing')
  return parseDesignSources([readTaskMarkdown(file, { explicit })]).sourceHash
}

function designConsistencyIssues(reports, stem, stage) {
  const issues = []
  if (stage !== 'final') return issues
  const sc = reports.find((r) => r.name === 'screen-cache' && r.exists)
  if (!sc || !sc.data) return issues   // missing screen-cache report is already a REPORT_MISSING blocker
  const recorded = sc.data.inputs && sc.data.inputs.designSourceHash
  if (!recorded || typeof recorded !== 'string') {
    issues.push(issue('BLOCKER', 'DESIGN_SOURCE_HASH_MISSING', 'screen-cache report carries no inputs.designSourceHash — re-run check-screen-cache', { file: sc.path, reportName: sc.name }))
    return issues
  }
  let current
  try {
    current = currentDesignSourceHash(stem)
  } catch {
    issues.push(issue('BLOCKER', 'DESIGN_SOURCE_UNAVAILABLE', 'the current task Design source is unreadable, unsafe, oversized, or not valid UTF-8 — repair the task source and re-run the Figma gates', { file: sc.path, reportName: sc.name }))
    return issues
  }
  if (current !== recorded) {
    issues.push(issue('BLOCKER', 'DESIGN_CHANGED_SINCE_CHECK', `task ## Design section changed after the screen-cache gate ran (recorded ${recorded.slice(0, 18)}…, current ${String(current).slice(0, 18)}…) — re-run the figma gates`, { file: sc.path, reportName: sc.name }))
  }
  return issues
}

function modelHashCoveredByInputHashes(report, model) {
  const hashes = report && report.data && report.data.inputHashes
  if (!hashes || typeof hashes !== 'object') return false
  const modelBase = basename(String(model && model.path || ''))
  return Object.entries(hashes).some(([p, h]) => {
    if (h !== model.hash) return false
    const resolved = resolveInputPath(p)
    return basename(resolved) === modelBase || displayPath(resolved) === model.path || p === model.path
  })
}

async function main() {
  let cli
  try {
    cli = parseCli({
      allowedFlags: ['--stem', '--stage', '--require', '--fresh', '--fixture-component-inventory',
        '--fixture-component-mappings', '--fixture-component-analysis-index', '--fixture-component-analysis-dir'],
      valueFlags: ['--stem', '--stage', '--require', '--fixture-component-inventory',
        '--fixture-component-mappings', '--fixture-component-analysis-index', '--fixture-component-analysis-dir'],
      booleanFlags: ['--fresh'],
      usage: USAGE,
    })
  } catch (e) {
    console.error(e.message)
    process.exit(1)
  }
  let stem
  try { stem = assertTaskStem(cli.value('--stem') || cli.positional[0] || '') }
  catch { console.error(USAGE); process.exit(1) }
  const stage = cli.value('--stage') || 'prebuild'
  if (!['prebuild', 'final'].includes(stage)) {
    console.error(`ERROR: --stage must be prebuild|final, got ${JSON.stringify(stage)}`)
    process.exit(1)
  }
  const fresh = cli.has('--fresh')
  const componentOptions = {
    fixtureInventoryFile: cli.value('--fixture-component-inventory') || undefined,
    fixtureMappingsFile: cli.value('--fixture-component-mappings') || undefined,
    fixtureAnalysisIndexFile: cli.value('--fixture-component-analysis-index') || undefined,
    fixtureAnalysisDirectory: cli.value('--fixture-component-analysis-dir') || undefined
  }
  // The final gate's whole job is to prove the required reports are bound to THIS run and
  // their artifacts are intact. All of that (run-id/stem pinning, input-hash drift, artifact
  // realpath+hash, model-hash) lives behind `if (fresh)`, so a `final` bundle without --fresh
  // silently degrades to envelope/consistency checks only. Fail closed instead.
  if (stage === 'final' && !fresh) {
    console.error('ERROR: --stage final requires --fresh')
    process.exit(1)
  }
  const runId = pipelineRunId(stem)
  const required = (cli.value('--require') ? cli.value('--require').split(',') : DEFAULT_REQUIRED[stage])
    .map((s) => s.trim()).filter(Boolean)
  // A truthy-but-empty --require (e.g. ',' or '   ') bypasses the DEFAULT_REQUIRED fallback
  // and trims to []. With no required reports the gate certifies PASS on zero evidence — a
  // fail-open. Reject an empty set rather than degrade to a no-op.
  if (!required.length) {
    console.error('ERROR: --require resolved to an empty set')
    process.exit(1)
  }
  for (const r of required) {
    if (!REPORT_PREFIXES.includes(r)) {
      console.error(`ERROR: unknown required report prefix ${JSON.stringify(r)}`)
      process.exit(1)
    }
  }
  const reportsDir = process.env.FIGMA_REPORTS_DIR || figmaPath('reports')
  const screensRoot = figmaScreensRoot()
  const screensDir = join(screensRoot, stem)
  const reports = REPORT_PREFIXES.map((prefix) => collectReport(reportsDir, stem, prefix))
  const existingReports = reports.filter((r) => r.exists)
  const requiredExistingReports = reports.filter((r) => r.exists && required.includes(r.name))
  const issues = []
  // Oracle timestamp validity is blocking: without a parseable pull timestamp the final
  // bundle cannot distinguish a current oracle from an unbounded stale snapshot. Age past
  // the configured floor remains advisory because age alone is not proof of drift.
  // The pulled oracle is a SNAPSHOT — Figma can move on after the
  // pull, and nothing local can detect that without calling Figma (golden invariant). A
  // WARN past FIGMA_ORACLE_MAX_AGE_DAYS (default 14; 0 disables) prompts a re-pull before
  // ship, so "pixel-perfect vs a month-old mock" is a visible, deliberate choice — never
  // silent. WARN-only: age is not proof of drift, so it cannot block.
  const maxOracleAgeDays = Number(process.env.FIGMA_ORACLE_MAX_AGE_DAYS || '14')
  if (stage === 'final') {
    try {
      const idx = JSON.parse(readFileSync(join(screensDir, 'index.json'), 'utf8'))
      const now = Date.now()
      for (const [name, node] of Object.entries((idx && idx.nodes) || {})) {
        for (const variant of (node && Array.isArray(node.variants) ? node.variants : [])) {
          const ts = variant && variant.fetchedAt ? Date.parse(variant.fetchedAt) : NaN
          if (!Number.isFinite(ts)) {
            issues.push(issue('BLOCKER', 'ORACLE_PULL_TIME_INVALID', `${name}/${variant && variant.id || 'unknown'} has no parseable fetchedAt; re-pull before certifying freshness`, { screen: name, theme: variant && variant.id || null, file: join(screensDir, 'index.json') }))
            continue
          }
          const days = (now - ts) / 86400000
          if (Number.isFinite(maxOracleAgeDays) && maxOracleAgeDays > 0 && days > maxOracleAgeDays) issues.push(issue('WARN', 'ORACLE_PULL_STALE', `${name}/${variant.id} oracle was pulled ${Math.floor(days)} day(s) ago (floor ${maxOracleAgeDays}d, FIGMA_ORACLE_MAX_AGE_DAYS) — the Figma design may have moved on; re-pull before certifying against a stale mock`, { screen: name, theme: variant.id }))
        }
      }
    } catch { /* no/unreadable index → the screen-cache gate owns that failure; age adds nothing */ }
  }
  const envelopeSchema = await compileSchema(figmaPath('token-schemas', 'report-envelope.schema.json'), { gate: true })
  const reportSchemas = {}
  for (const [prefix, schemaFile] of Object.entries(REPORT_SCHEMAS)) {
    reportSchemas[prefix] = await compileSchema(figmaPath('token-schemas', schemaFile), { gate: true })
  }
  // The committed thresholds config feeding this gate's OWN canon must itself be
  // schema-valid — a hand-edited config that survives the loader's structural
  // minimum but violates the schema (wrong integer-ness, out-of-range) would
  // silently re-anchor the anti-forgery canon.
  {
    const thresholdsSchema = await compileSchema(figmaPath('token-schemas', 'screenshot-thresholds.schema.json'), { gate: true })
    const configIssues = schemaIssues(thresholdsSchema, TCFG)
    if (configIssues.length) {
      issues.push(issue('BLOCKER', 'THRESHOLDS_CONFIG_INVALID', `screenshot-thresholds.json violates its schema: ${configIssues.map((i) => `${i.path}: ${i.message}`).join('; ')}`, {}))
    }
  }
  for (const r of reports) {
    if (!r.exists && required.includes(r.name)) {
      issues.push(issue(stage === 'final' ? 'BLOCKER' : 'WARN', 'REPORT_MISSING', `${r.name} report missing`, { file: r.path, reportName: r.name }))
    }
    if (r.exists && required.includes(r.name)) {
      if (!r.data) {
        issues.push(issue('BLOCKER', 'REPORT_UNREADABLE', `${r.name} report unreadable`, { file: r.path, reportName: r.name }))
        continue
      }
      const envelopeIssues = schemaIssues(envelopeSchema, r.data, `${r.name}:`)
      for (const schemaIssue of envelopeIssues) {
        issues.push(issue('BLOCKER', 'REPORT_SCHEMA_INVALID', `${r.name} report schema invalid at ${schemaIssue.path}: ${schemaIssue.message}`, { file: r.path, reportName: r.name, path: schemaIssue.path }))
      }
      const bodySchema = reportSchemas[r.name]
      if (bodySchema) {
        const bodyIssues = schemaIssues(bodySchema, r.data, `${r.name}:`)
        for (const schemaIssue of bodyIssues) {
          issues.push(issue('BLOCKER', 'REPORT_BODY_SCHEMA_INVALID', `${r.name} report body schema invalid at ${schemaIssue.path}: ${schemaIssue.message}`, { file: r.path, reportName: r.name, path: schemaIssue.path }))
        }
      }
      const overall = String(r.data.overall || '').toUpperCase()
      if (!KNOWN_OVERALL.has(overall)) {
        issues.push(issue('BLOCKER', 'REPORT_INVALID_VERDICT', `${r.name} report has unknown overall ${JSON.stringify(r.data.overall)}`, { file: r.path, reportName: r.name }))
      }
      issues.push(...reportConsistencyIssues(r, stage))
      if (fresh) {
        if (r.data.taskStem !== stem) {
          issues.push(issue('BLOCKER', 'REPORT_STEM_MISMATCH', `${r.name} report taskStem ${JSON.stringify(r.data.taskStem)} does not match ${JSON.stringify(stem)}`, { file: r.path, reportName: r.name }))
        } else if (r.data.pipelineRunId !== runId) {
          issues.push(issue('BLOCKER', 'REPORT_STALE_RUN', `${r.name} report pipelineRunId ${JSON.stringify(r.data.pipelineRunId)} does not match ${JSON.stringify(runId)} — re-run ONLY that report under FIGMA_PIPELINE_RUN_ID=${runId} then re-run this bundle (the run id is pinned at ${displayPath(runIdPinPath(stem))}; a full evidence-clean resets it)`, { file: r.path, reportName: r.name }))
        }
        issues.push(...reportFreshnessIssues(r, stage, componentOptions))
        issues.push(...reportArtifactIssues(r, stage))
        issues.push(...screenshotStrictnessIssues(r, stage))
      }
    }
  }
  if (fresh) {
    issues.push(...designConsistencyIssues(reports, stem, stage))
    const specCompare = reports.find((r) => r.name === 'spec-compare')
	    const model = specCompare && specCompare.data && specCompare.data.implementationModel
	    if (model && model.path && model.hash) {
	      const modelPath = resolveInputPath(model.path)
	      const modelHash = fileHash(modelPath)
	      if (modelHash !== model.hash && !modelHashCoveredByInputHashes(specCompare, model)) issues.push(issue('BLOCKER', 'IMPLEMENTATION_MODEL_HASH_MISMATCH', 'spec-compare implementationModel hash does not match file', { file: displayPath(modelPath) || basename(modelPath) }))
	    }
  }
  // Same completeness invariant as the screenshot coverage check below, but for the
  // declarative spec-vs-code report: every normalized spec element must have a
  // comparison row. Otherwise a truncated/hand-edited PASS report with an empty
  // `comparisons: []` certifies only the report envelope, not the design spec.
  if (stage === 'final' && required.includes('spec-compare')) {
    const specCompare = reports.find((r) => r.name === 'spec-compare' && r.exists)
    if (specCompare && specCompare.data) {
      const covered = new Set()
      for (const row of Array.isArray(specCompare.data.comparisons) ? specCompare.data.comparisons : []) {
        if (row && row.screen != null && row.stableId != null) {
          covered.add(`${row.screen}\u0001${String(row.theme || '')}\u0001${row.stableId}`)
        }
      }
      let resolved = null
      try { resolved = loadResolvedSpecs({ stem, screensRoot }) }
      catch (error) {
        issues.push(issue('BLOCKER', 'SPEC_COVERAGE_SOURCE_INVALID', `current resolved specs are unreadable or invalid: ${error.message}`, { reportName: 'spec-compare' }))
      }
      for (const spec of (resolved && resolved.specs) || []) {
        for (const element of spec.elements || []) {
          const key = `${spec.screen}\u0001${String(spec.theme || '')}\u0001${element.identity}`
          if (!covered.has(key)) {
            issues.push(issue('BLOCKER', 'SPEC_COMPARE_COVERAGE_INCOMPLETE', `spec-compare report has no row for ${spec.screen}/${spec.theme || 'default'} element ${element.identity} — every comparable spec element must be compared`, { reportName: 'spec-compare', screen: spec.screen, theme: spec.theme || null, stableId: element.identity, elementName: element.name || undefined, file: spec.file }))
          }
        }
      }
    }
  }
  // C2(i) count-completeness: the FINAL screenshot report must carry a result row for EVERY
  // indexed node variant it should have compared. Without this, a report whose `results` cover
  // only a SUBSET of index.json (a dropped screen, a skipped dark variant, or a truncated/forged
  // report) certifies PASS on partial coverage — the gate trusts the report's own rows and never
  // checks them against the pulled node set. The current index variants array is the sole
  // expected-set source; summary url/darkUrl fields never create an alternate coverage model.
  // Skipped when index.json is absent (the screen-cache gate owns that) or screenshot is not
  // a required report.
  // R2-1/R3: per-row review state, computed at final from the sealed screenshot report + the
  // owner's hash-bound pixel-review receipt. Populated inside the final screenshot block below;
  // consumed by the overall computation, the report extra, and the digest rows.
  const pixelReview = { pending: [], resolved: [], effectiveStatus: new Map() }
  const reviewRowKey = (screen, theme) => `${screen}\u0001${String(theme || 'primary').toLowerCase()}`
  if (stage === 'final' && required.includes('screenshot')) {
    const shot = reports.find((r) => r.name === 'screenshot' && r.exists)
    if (shot && shot.data) {
      let idx = null
      try { idx = JSON.parse(readFileSync(join(screensDir, 'index.json'), 'utf8')) } catch { /* missing index → screen-cache gate owns it */ }
      const nodes = idx && idx.nodes && typeof idx.nodes === 'object' ? idx.nodes : null

      // R2-1 canon: the report's routing decisions must still be justified by the LIVE
      // committed mapping registry's visualPolicy. A row routed to review whose class the
      // owner has since CLEARED (tightening back to strict) must not stay reviewable —
      // fail closed to a re-run. The reverse (a class ADDED after the compare) only means
      // the row blocked where it could now route — stricter than required, no issue.
      const routing = shot.data.classRouting && typeof shot.data.classRouting === 'object' ? shot.data.classRouting : {}
      if (Object.keys(routing).length) {
        let liveClasses
        try {
          liveClasses = renderClassByNodeId(componentOptions)
        } catch (error) {
          liveClasses = new Map()
          issues.push(issue('BLOCKER', 'CLASS_ROUTING_SOURCE_INVALID', `cannot validate screenshot owner-review routing against LIVE component truth: ${error.message}`, { reportName: 'screenshot' }))
        }
        for (const [screen, cls] of Object.entries(routing)) {
          const node = nodes ? nodes[screen] : null
          const live = node && node.kind === 'component' && node.nodeId
            ? liveClasses.get(String(node.nodeId)) || null
            : null
          if (!live) {
            issues.push(issue('BLOCKER', 'CLASS_ROUTING_STALE', `screenshot row ${screen} was routed to owner review as renderClass ${JSON.stringify(cls)} but the LIVE mapping registry no longer classes it — the owner tightened it back to strict gating; re-run compare-screenshots under the pinned run id, then this bundle`, { reportName: 'screenshot', screen }))
          }
        }
      }

      // R3: apply the owner's hash-bound review receipt to REVIEW_REQUIRED rows. A receipt
      // row resolves its screen ONLY when every binding holds — same run id, same sealed
      // report bytes, same reviewed artifact pixels — so a re-render/re-run makes the old
      // click INERT (back to REVIEW_REQUIRED) instead of laundering new pixels under it.
      const reviewDir = process.env.FIGMA_PIXEL_REVIEW_DIR || join(PROJECT_ROOT, 'orchestrator', 'tasks', 'evidence', 'pixel-review')
      let receiptRows = []
      try {
        const receipt = JSON.parse(readFileSync(join(reviewDir, `${stem}.json`), 'utf8'))
        receiptRows = Array.isArray(receipt && receipt.rows) ? receipt.rows : []
      } catch { /* no receipt yet — every routed row stays pending */ }
      const receiptByKey = new Map()
      for (const row of receiptRows) {
        if (row && row.screen) receiptByKey.set(reviewRowKey(row.screen, row.theme), row)
      }
      for (const row of Array.isArray(shot.data.results) ? shot.data.results : []) {
        if (!row || row.status !== 'REVIEW_REQUIRED') continue
        const key = reviewRowKey(row.screen, row.theme || row.themeKey)
        const rec = receiptByKey.get(key)
        const pendingEntry = { screen: row.screen, theme: String(row.theme || row.themeKey || 'primary'), renderClass: row.renderClass || null, pixelStatus: row.pixelStatus || null }
        if (!rec) { pixelReview.pending.push(pendingEntry); continue }
        const arts = row.artifactSet && row.artifactSet.artifacts
        const artPath = (slot) => (arts && arts[slot] && (arts[slot].path || arts[slot].file)) || null
        const figmaAbs = artPath('figma') ? resolveInputPath(artPath('figma')) : null
        const actualAbs = artPath('actual') ? resolveInputPath(artPath('actual')) : null
        const bindingsOk = rec.pipelineRunId === shot.data.pipelineRunId
          && rec.reportHash === shot.hash
          && !!figmaAbs && fileHash(figmaAbs) === rec.figmaHash
          && !!actualAbs && fileHash(actualAbs) === rec.actualHash
          && ['pass', 'minor', 'fail'].includes(rec.verdict)
        if (!bindingsOk) {
          pixelReview.pending.push(pendingEntry)
          issues.push(issue('WARN', 'PIXEL_REVIEW_STALE', `pixel-review receipt for ${row.screen} no longer matches the reviewed run/artifacts — the render or run changed after the click; review again in the site's evidence tab`, { reportName: 'screenshot', screen: row.screen }))
          continue
        }
        const resolvedEntry = { screen: row.screen, theme: pendingEntry.theme, renderClass: pendingEntry.renderClass, verdict: rec.verdict, by: rec.by || 'owner', at: rec.at || null, note: rec.note || null }
        pixelReview.resolved.push(resolvedEntry)
        if (rec.verdict === 'pass') {
          // No bundle ISSUE for a clean acceptance: any issue (even informational) floors the
          // bundle overall at WARN, and an owner-accepted row is a true PASS. The audit trail
          // lives in extra.pixelReview.resolved + the receipt file + the effective digest row.
          pixelReview.effectiveStatus.set(key, 'PASS')
        } else if (rec.verdict === 'minor') {
          pixelReview.effectiveStatus.set(key, 'MINOR')
          issues.push(issue('WARN', 'PIXEL_REVIEWED_MINOR', `pixel review: ${row.screen} accepted WITH minor drift by ${resolvedEntry.by}${rec.note ? ` — ${rec.note}` : ''} (rides the reviewed-WARN ship path: a real ### Caveats bullet is required)`, { reportName: 'screenshot', screen: row.screen }))
        } else {
          pixelReview.effectiveStatus.set(key, 'BLOCKER')
          issues.push(issue('BLOCKER', 'PIXEL_REVIEW_FAILED', `pixel review: ${row.screen} REJECTED by ${resolvedEntry.by}${rec.note ? ` — ${rec.note}` : ''}; route the finding to the builder and re-run the pipeline`, { reportName: 'screenshot', screen: row.screen }))
        }
      }
      if (nodes && Object.keys(nodes).length) {
        const covered = new Set()
        for (const row of Array.isArray(shot.data.results) ? shot.data.results : []) {
          if (row && row.screen != null) covered.add(`${row.screen}\u0001${String(row.theme || row.themeKey || 'primary').toLowerCase()}`)
        }
        for (const [name, node] of Object.entries(nodes)) {
          for (const variant of (node && Array.isArray(node.variants) ? node.variants : [])) {
            const variantId = String(variant.id).toLowerCase()
            if (!covered.has(`${name}\u0001${variantId}`)) {
              issues.push(issue('BLOCKER', 'SCREENSHOT_COVERAGE_INCOMPLETE', `indexed screen ${name}/${variant.id} has no row in the final screenshot report — every pulled node variant must be compared, not a subset; re-run the screenshot gate`, { reportName: 'screenshot', screen: name, theme: variant.id }))
            }
          }
        }
      }
    }
  }
  const hasBlocker = issues.some((i) => ['BLOCKER', 'ERROR', 'FAIL'].includes(String(i.severity).toUpperCase()))
  // R3: a screenshot report whose ONLY rank-3 cause is REVIEW_REQUIRED rows that the owner's
  // receipt fully resolved contributes its EFFECTIVE overall instead — pass-resolutions drop
  // it to PASS/WARN (the PIXEL_REVIEWED* issues above keep the audit trail + the WARN/caveat
  // coupling), fail-resolutions already raised a BLOCKER issue, and ANY unresolved row keeps
  // the report at REVIEW_REQUIRED (non-shippable). Reports other than screenshot untouched.
  const effectiveReports = requiredExistingReports.map((r) => {
    if (r.name !== 'screenshot' || String(r.overall).toUpperCase() !== 'REVIEW_REQUIRED') return r
    if (pixelReview.pending.length || !pixelReview.resolved.length) return r
    if (pixelReview.resolved.some((x) => x.verdict === 'fail')) return { ...r, overall: 'BLOCKER' }
    // A resolved-pass report drops to PASS unless something ELSE still warns: the routed
    // PIXEL_REVIEW_REQUIRED warns are exactly what the pass verdict resolved, so they do not
    // count — else a clean owner accept could never yield PASS (every routed report carries
    // its own review warns by construction).
    const otherWarns = (Array.isArray(r.data && r.data.issues) ? r.data.issues : []).some((i) =>
      i && i.issueKind !== 'PIXEL_REVIEW_REQUIRED' && /^(WARN|WARNING|MINOR|MAJOR|REVIEW_REQUIRED)$/.test(String(i.severity || '').toUpperCase()))
    const stillWarn = pixelReview.resolved.some((x) => x.verdict === 'minor') || otherWarns
    return { ...r, overall: stillWarn ? 'WARN' : 'PASS' }
  })
  // Take the MAX of the required reports' own overall and an issue-derived WARN — never let a
  // bundle WARN REPLACE (mask down) a required report that is itself INCOMPLETE/REVIEW_REQUIRED.
  const baseOverall = overallOf(effectiveReports.concat(issues.length ? [{ overall: 'WARN' }] : []))
	  const overall = hasBlocker ? 'BLOCKER' : baseOverall
	  const finalFailure = ['BLOCKER', 'INCOMPLETE', 'REVIEW_REQUIRED'].includes(String(overall).toUpperCase())
  // Informational widget-class record (from the spec-compare report). A canvas-classed screen was
  // verified via the earned alias/indirect-evidence path, not the declarative one — recorded for the
  // audit trail; it does NOT change the verdict or gate anything.
  const specCompareForClass = reports.find((r) => r.name === 'spec-compare' && r.exists)
  const widgetClasses = (specCompareForClass && specCompareForClass.data && specCompareForClass.data.widgetClasses) || {}
  const verifiedAs = Object.values(widgetClasses).includes('canvas') ? 'canvas-component' : null
  const { reportPath } = writeReport({
    name: 'evidence',
    taskStem: stem,
    mode: 'transport',
    inputs: { reportsDir: displayPath(reportsDir), screensDir: displayPath(screensDir), stage, required, fresh },
    inputHashes: Object.fromEntries(existingReports.map((r) => [r.path, r.hash])),
    overall,
    issues,
    extra: {
      stage,
      requiredReports: required,
      reports: reports.map(publicReport),
      screenFiles: collectScreenFiles(screensDir),
      widgetClasses,
      verifiedAs,
      // R3: the owner-review state at THIS bundle run — pending rows keep the bundle at
      // REVIEW_REQUIRED (site renders them as review rows with buttons); resolved rows carry
      // the audited verdict + reviewer for the site + done-view.
      pixelReview: { pending: pixelReview.pending, resolved: pixelReview.resolved },
    },
	  })
	  console.log(`evidence-bundle: ${stem} ${overall} -> ${reportPath}`)

	  // H2 — machine-emit the `- Figma meta:` digest. Before, this line was hand-typed by
	  // the orchestrator agent in the appendix; now it is code-emitted from THIS run's
	  // verdict + the real report hashes, so a skipped done-gate or a hand-`mv` simply
	  // leaves no digest. Only on a final bundle that actually carried a screenshot report
	  // (final fails closed without one anyway) — the digest references it by hash.
	  if (stage === 'final') {
	    const shot = reports.find((r) => r.name === 'screenshot' && r.exists)
	    if (shot && shot.hash) {
	      let tokenReceipt
	      try {
	        tokenReceipt = buildTaskObservationReceipt({
	          taskStem: stem,
	          transactionId: process.env.FINALIZE_TRANSACTION_ID || `fin-preflight-${runId}`,
	          screensRoot,
	        })
	      } catch (error) {
	        console.error(`FATAL: token observation receipt preflight failed: ${error.message}`)
	        process.exit(1)
	      }
	      const evidenceReportHash = fileHash(resolve(reportsDir, `evidence-${stem}.json`))
	      const results = Array.isArray(shot.data && shot.data.results) ? shot.data.results : []
	      // Strip the digest's field/kv delimiters from each cell so an exotic screen name can't
	      // truncate or corrupt the `rows=` field (it is the trailing `;`-delimited field).
	      // Strip the digest field/record separators (;=) AND any CR/LF so a screen/theme/status
	      // containing a newline can't split the single-line `- Figma meta:` bullet that ship-done /
	      // verify-done parse line-wise (a broken line would truncate or invalidate the digest).
	      // `,` too: cells are comma-joined, so a literal comma in a screen name would inflate
	      // the rows= display cell count.
	      const safeCell = (s) => String(s == null ? '' : s).replace(/[;=,\r\n]+/g, '-')
	      // Cap 48 = the board's MAX_VISUAL_CHECKS — the durable audit line must not under-report
	      // exactly at the many-screens scale the pipeline targets. Mark truncation explicitly.
	      const ROWS_CAP = 48
	      // R3: the digest records EFFECTIVE row statuses — a pass/minor-reviewed row shows its
	      // resolved verdict (the receipt is the audited authority), not the routed placeholder.
	      const effStatus = (r) => pixelReview.effectiveStatus.get(reviewRowKey(r.screen, r.theme || r.themeKey)) || r.status
	      const rows = (results.length > ROWS_CAP
	        ? results.slice(0, ROWS_CAP).map((r) => `${safeCell(r.screen)}/${safeCell(r.theme || 'primary')}:${safeCell(effStatus(r))}`).concat(`+${results.length - ROWS_CAP}-more`)
	        : results.map((r) => `${safeCell(r.screen)}/${safeCell(r.theme || 'primary')}:${safeCell(effStatus(r))}`)
	      ).join(',') || 'none'
	      const problemCount = results.filter((r) => String(effStatus(r)).toUpperCase() !== 'PASS').length
	      let designHash
	      try {
	        designHash = primaryBodyDesignHash(stem)
	      } catch {
	        console.error('FATAL: current task Design source is unreadable, unsafe, oversized, or not valid UTF-8')
	        process.exit(1)
	      }
	      const digest = buildFigmaMeta({
	        taskStem: stem,
	        stage,
	        overall,
	        pipelineRunId: runId,
	        evidenceReportHash,
	        screenshotReportHash: shot.hash,
	        generatedAt: new Date().toISOString(),
	        visualChecks: results.length,
	        problemCount,
	        // Bind the certification to the ## Design section it compared — the SAME section
	        // hash designConsistencyIssues verified against the screen-cache gate above, so a
	        // post-ship ## Design edit flips verify-done red instead of keeping a stale GREEN.
	        designHash,
	        // W4-3: record which strictness regime certified this ship (thresholds `version`).
	        gatePolicyVersion: TCFG.version,
	        tokenObservationManifestHash: tokenReceipt.manifestHash,
	        rows,
	      })
	      const digestPath = join(reportsDir, `figma-meta-${stem}.txt`)
	      // tmp+rename (writeReport's idiom) and FAIL LOUD on error: a PASS bundle whose digest
	      // failed to persist must not look shippable. ship-done requires the digest file and
	      // would block anyway — but with a misleading "comparison did not run" diagnosis that
	      // routes the agent to the compare/pull fix path instead of the real disk/permission
	      // failure, so the error is surfaced here at its cause.
	      try {
	        writeFileSync(`${digestPath}.tmp`, digest + '\n')
	        renameSync(`${digestPath}.tmp`, digestPath)
	      } catch (e) {
	        console.error(`FATAL: failed to persist figma-meta digest ${displayPath(digestPath) || basename(digestPath)}: ${e.message}`)
	        process.exit(1)
	      }
	      // A clearly-delimited line so ship-done.mjs / the agent can lift it verbatim.
	      console.log(`FIGMA_META_DIGEST ${digest}`)
	    }
	  }

	  process.exit(hasBlocker || (stage === 'final' && finalFailure) ? 1 : 0)
}

main().catch((e) => {
  console.error(`FATAL: ${e.message}`)
  process.exit(1)
})
