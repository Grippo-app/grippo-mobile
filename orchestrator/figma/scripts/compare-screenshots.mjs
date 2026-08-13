/**
 * compare-screenshots.mjs
 *
 * Compares Roborazzi-rendered screenshots against the pulled Figma oracle(s) using masked SSIM.
 * Default mode is advisory. `--gate` makes missing/incomplete evidence a
 * blocking pre-flight failure instead of a silent pass.
 *
 * Usage:
 *   node scripts/compare-screenshots.mjs <stem> [--gate|--advisory] [--semantic]
 *
 * Environment overrides:
 *   FIGMA_SCREEN_CACHE_ROOT or FIGMA_SPEC_SCREENS_DIR — override screens cache root; the script appends <stem> (must match if both are set)
 *   ROBORAZZI_OUTPUT_DIR         — one Roborazzi output dir
 *   ROBORAZZI_OUTPUT_DIRS        — multiple output dirs, separated by the platform path delimiter
 *   SCREENSHOT_CAPTURE_MANIFEST  — capture manifest; required in gate mode. Oracle/capture
 *     identity is bound only by the stable Figma nodeId recorded in this manifest.
 *     R5: an entry MAY carry `localeTag` — the locale the harness ACTUALLY rendered with; when
 *     present AND the design language is derivable (designLocale config / lib/design-locale
 *     detection), a language mismatch is an always-BLOCKER CAPTURE_LOCALE_MISMATCH row
 *     (completeness-class: never routed by screenshotPixelGate; advisory mode blocks too).
 *     Without the optional field, locale comparison is not available.
 *   SCREENSHOT_CAPTURE_STARTED_AT — optional epoch-ms/ISO lower bound for capture mtimes
 *   FIGMA_DESIGN_LOCALE / FIGMA_SUPPORTED_LOCALES / FIGMA_STRING_RESOURCE_ROOTS — per-run
 *     overrides of the design-locale inputs (fixtures/diagnostics ONLY: when set they are
 *     recorded in the report as designLocaleEnvOverrides and the FINAL bundle hard-blocks
 *     (LOCALE_ENV_OVERRIDE) — they can redirect/disarm the locale witness, so they may never
 *     certify shipping evidence; see lib/design-locale.mjs)
 *   FIGMA_REPORTS_DIR            — override report output dir for tests
 *   NB: every numeric default below comes from the committed screenshot-thresholds.json
 *   (the ONE strictness source — see loadScreenshotThresholds in _util.mjs); the values in
 *   parentheses are what that config carries today. Env knobs are per-run overrides only.
 *   SCREENSHOT_PASS_THRESHOLD    — SSIM threshold for PASS (config `pass`: 0.90 today)
 *   SCREENSHOT_MINOR_THRESHOLD   — SSIM threshold for MINOR (config `minor`: 0.80 today)
 *   SCREENSHOT_MAJOR_THRESHOLD   — SSIM threshold for MAJOR (config `major`: 0.65 today)
 *   SCREENSHOT_BG_TOLERANCE      — RGB distance from background to count content (config `bgTolerance`: 24 today)
 *   SCREENSHOT_MIN_COVERAGE      — min oracle foreground fraction for usable compare (config `minCoverage`: 0.005 today)
 *   SCREENSHOT_ASPECT_TOLERANCE  — max render-vs-oracle aspect divergence (config `aspectTolerance`: 0.15 today)
 *   SCREENSHOT_SEMANTIC_DIFF     — 0|1; semantic-zone report fields only, advisory until calibrated
 *   FIGMA_CACHE_ROOT             — override consolidated figma cache root for isolated fixtures
 *   FIGMA_COMPARE_ARTIFACTS_DIR  — override compare artifact root; must equal FIGMA_CACHE_ROOT/artifacts/screenshot
 *   SCREENSHOT_ARTIFACT_RETENTION — compare artifact runs to retain per stem (default: 5; 0 disables pruning)
 *   SCREENSHOT_ZONE_GATE         — H3 worst-zone floor: a single broken node/cell fails the screen (config `zoneGate`: true; 0 to roll back)
 *   SCREENSHOT_ZONE_BLOCKER_THRESHOLD — per-NON-TEXT-zone SSIM floor for the zone-gate (config `zoneBlocker`: 0.35 today; provisional until calibrated)
 *   SCREENSHOT_ZONE_TEXT_BLOCKER_THRESHOLD — lenient per-TEXT-zone SSIM floor (font-AA tolerant; config `zoneTextBlocker`: 0.25 today; provisional)
 *   SCREENSHOT_MIN_REGION_PX     — min content px for a zone/cell to be eligible for the zone-gate (config `minRegionPx`: 400 today)
 *   SCREENSHOT_EXTRA_CONTENT_WARN — unmasked-divergence fraction above which RENDER_EXTRA_CONTENT fires (config `extraContentWarn`: 0.02 today)
 *   SCREENSHOT_EXTRA_CONTENT_BAND — RENDER_EXTRA_CONTENT routing: warn|block|off (default: warn — enum, not in the config)
 *   SCREENSHOT_EXTRA_CONTENT_DELTAE — ΔE00 floor for the probe's equal-luma colour arm (config `extraContentDeltaE`: 10 today)
 *   SCREENSHOT_EXTRA_CONTENT_RING_PX — W5-3 mask-dilation ring: probe skips unmasked pixels within
 *     N px of the content-mask edge (halo/gradient spill is not render-added content; config
 *     `extraContentRingPx`: 3 today; canon-pinned lte — raising it is THRESHOLDS_WEAKENED; 0 = stricter)
 *
 * Exit codes:
 *   0 — report written; no blocking gate condition
 *   1 — fatal setup/config/dependency error
 *   2 — `--gate` found blocking or incomplete visual evidence
 */

import { readFileSync, existsSync, readdirSync, statSync, lstatSync, realpathSync } from 'node:fs'
import { join, basename, delimiter, dirname, resolve, sep } from 'node:path'
import { createRequire } from 'node:module'
import { PROJECT_ROOT as REPO_ROOT, artifactSegment, cacheRelative, displayPath, figmaPath, figmaScreensRoot, isDirectRun, loadScreenshotThresholds, parseCli, pipelineRunId, readConfig, FIGMA_CACHE_ROOT, FIGMA_TASK_SOURCE_FILE, FIGMA_TASK_SOURCE_EXPLICIT, PROJECT_CONFIG_FILE, PROJECT_CONFIG_HASH } from './_util.mjs'
import { assertTaskStem, compileSchema, fileHash, schemaIssues, writeReport } from './report-utils.mjs'
import { deriveResourceRoots, languageOf, resolveDesignLocale } from './lib/design-locale.mjs'
import { detectChrome } from './lib/oracle-chrome.mjs'
import { renderClassByNodeId } from './lib/design-components.mjs'
import { readTaskMarkdown } from './lib/task-markdown.mjs'

const requireCjs = createRequire(import.meta.url)
const { parseDesignSources } = requireCjs('./design-parser.cjs')
const fileGuards = requireCjs('../../site/server/file-guards.js')

const USAGE = 'usage: node scripts/compare-screenshots.mjs <stem> [--gate|--advisory] [--semantic]'
const SCREEN_KEY_RE = /^[A-Za-z0-9_]+$/
const C1 = (0.01 * 255) ** 2
const C2 = (0.03 * 255) ** 2

const CONFIG_ERRORS = []
function numEnv(name, fallback, { min = -Infinity, max = Infinity, integer = false } = {}) {
  const raw = process.env[name] ?? String(fallback)
  const n = integer ? parseInt(raw, 10) : parseFloat(raw)
  if (!Number.isFinite(n) || n < min || n > max) CONFIG_ERRORS.push(`${name} must be ${integer ? 'an integer' : 'a number'} in [${min}, ${max}], got ${JSON.stringify(raw)}`)
  return n
}

// The committed screenshot-thresholds.json is the single source of every default
// below (hard fail if absent/invalid — no baked-in numbers to drift from the
// canon evidence-bundle derives from the SAME file). The SCREENSHOT_* env knobs
// stay as documented PER-RUN overrides layered on top; weakening past the config
// is caught at the final gate as THRESHOLDS_WEAKENED.
const TCFG = loadScreenshotThresholds()

const T_PASS = numEnv('SCREENSHOT_PASS_THRESHOLD', TCFG.pass, { min: 0, max: 1 })
const T_MINOR = numEnv('SCREENSHOT_MINOR_THRESHOLD', TCFG.minor, { min: 0, max: 1 })
const T_MAJOR = numEnv('SCREENSHOT_MAJOR_THRESHOLD', TCFG.major, { min: 0, max: 1 })
const BG_TOLERANCE = numEnv('SCREENSHOT_BG_TOLERANCE', TCFG.bgTolerance, { min: 0, max: 442, integer: true })
const MIN_COVERAGE = numEnv('SCREENSHOT_MIN_COVERAGE', TCFG.minCoverage, { min: 0, max: 1 })
const ASPECT_TOL = numEnv('SCREENSHOT_ASPECT_TOLERANCE', TCFG.aspectTolerance, { min: 0, max: 1 })
// #18: record-and-rescale absorbs up to ASPECT_TOL of aspect divergence ANISOTROPICALLY —
// resize({w,h}) force-fits BOTH axes, and scaleFactor (width-derived) can still read 1.0 for
// a pure-height mismatch. The normalized oracle/capture aspect ratio (>=1) is recorded as
// row.aspectSkew; above this floor the caller pushes WARN ASPECT_SKEW so the geometric
// squash/stretch is visible downstream instead of masquerading as a pixel diff.
const ASPECT_SKEW_WARN = 1.03
// A non-screen kind ([dialog]/[component]/[overlay]) whose aspect happens to sit near the
// oracle's can still be captured on the WRONG canvas (the full-bleed class qualifier instead
// of the bullet's own frameSizeDp) — record-and-rescale then normalizes the size away and the
// resample blur ships as an unexplained WARN-band SSIM dent. The capture recipe (fidelity-gate
// §2/§2.2) mandates a method-level @Config sized to frameSizeDp at default mdpi (1px == 1dp),
// so a native capture deviating more than this fraction from frameSizeDp on either axis gets
// a named WARN (KIND_GEOMETRY_MISMATCH) pointing at the qualifier/container, not at a phantom
// pixel bug. Deliberately a constant, like ASPECT_SKEW_WARN: advisory, no env bypass to canonize.
const KIND_GEOMETRY_TOL = 0.25
// R1 — device-chrome band exclusion (screen kind only).
// For a `screen`, deterministic top status-bar + bottom nav-bar bands are normally excluded from
// BOTH images (the #1 region-mismatch false-block). The sole strict exception is an unresolved
// geometry-only top strip: detectChrome classifies it as a suspect rather than system chrome, so
// the top band stays comparable until the owner resolves it. Heights are the standard Android
// system-bar dp, mapped to px via frameSizeDp (a bounded fraction fallback covers a spec-less screen).
// The env knobs exist for rollback/tuning only: evidence-bundle's CANONICAL_SCREENSHOT_THRESHOLDS
// canon-pins them `lte` these defaults, so a RAISED band (which over-masks → fewer compared
// pixels → weaker) surfaces as THRESHOLDS_WEAKENED at the final gate and can never launder a
// divergent screen into a PASS by blinding half the frame. 0 disables the band (compares MORE =
// stricter, always allowed). The bounded blind spot (top/bottom real content on an edge-to-edge
// screen) is accepted-by-design and far better than a whole-image garbage low-similarity blocker;
// element STRUCTURE in the band is still covered by the independent spec-compare gate.
const STATUS_BAR_DP = numEnv('SCREENSHOT_STATUS_BAR_DP', TCFG.statusBarDp, { min: 0, max: 96 })
const NAV_BAR_DP = numEnv('SCREENSHOT_NAV_BAR_DP', TCFG.navBarDp, { min: 0, max: 96 })
const ARTIFACT_RETENTION = numEnv('SCREENSHOT_ARTIFACT_RETENTION', '5', { min: 0, max: 100, integer: true })
if (!(T_PASS >= T_MINOR && T_MINOR >= T_MAJOR)) CONFIG_ERRORS.push('thresholds must be sorted: PASS >= MINOR >= MAJOR')
// H3 — worst-zone floor (default ON). The gate verdict is otherwise the content-weighted
// MEAN SSIM, so a localized-but-critical diff (one destroyed Figma node) can be averaged away
// into a PASS — a false-negative. With the zone-gate on, any single per-node zone (or grid
// cell) with enough content (`contentPx >= MIN_REGION_PX`) and `ssim` below its per-zone floor
// forces the screen to BLOCKER.
//
// "структуру строже, текст мягче" — the floor is applied PER ZONE by node kind:
//   • non-text zones (icons, solid fills, shapes, sub-elements) render faithfully in
//     Robolectric, so a non-text zone below Z_BLOCKER with 400+px is a REAL divergence that
//     SHOULD block — the strict floor.
//   • text zones get the LENIENT Z_TEXT_BLOCKER floor: Robolectric font antialiasing ≠ Figma,
//     so a correctly-placed text node scores low on pixel SSIM even when it is right. Text
//     fidelity (wrong size/weight/colour/content) is already covered by the INDEPENDENT
//     spec-compare gate (declarative, token-resolved) + the whole-frame ΔE colour axis, so the
//     PIXEL zone gate can afford to be lenient on text STRUCTURE without losing coverage —
//     the lenient floor only catches text that is structurally gone/wrong.
// The grid-cell fallback (no spec projected zones) keeps Z_BLOCKER: grid cells carry no text
// classification. Both floors are provisional until calibrated against a real corpus
// (calibrate-thresholds.mjs) — conservative-and-tunable is the point; the env knobs stay for
// rollback/tuning. SCREENSHOT_ZONE_GATE=0 restores the pre-default-on behaviour.
// 0.35 (not 0.55): an adversarial probe showed a CORRECT fine/thin-stroke icon under normal
// rasterizer AA (Robolectric vs Figma) sinks a zone to ~0.55 at the metric's own default
// GAUSSIAN_SIGMA=1.5 and ~0.39 at σ=2.0, while GENUINE localized breaks (wrong/dimmed/shifted
// element) sit at 0.04–0.30 — a floor in (0.30, 0.39) separates them. 0.35 catches real breaks
// with margin and stops false-blocking correct high-frequency iconography.
const Z_BLOCKER = numEnv('SCREENSHOT_ZONE_BLOCKER_THRESHOLD', TCFG.zoneBlocker, { min: 0, max: 1 })
// Lenient floor for TEXT zones (font-AA tolerant). Default 0.25 is uncalibrated — it is meant
// only to catch text that is structurally gone/wrong, not to police glyph-level pixel fidelity.
const Z_TEXT_BLOCKER = numEnv('SCREENSHOT_ZONE_TEXT_BLOCKER_THRESHOLD', TCFG.zoneTextBlocker, { min: 0, max: 1 })
const MIN_REGION_PX = numEnv('SCREENSHOT_MIN_REGION_PX', TCFG.minRegionPx, { min: 0, integer: true })

function boolEnv(name, fallback = false) {
  const raw = process.env[name]
  if (raw == null || raw === '') return fallback
  if (['1', 'true', 'yes'].includes(String(raw).toLowerCase())) return true
  if (['0', 'false', 'no'].includes(String(raw).toLowerCase())) return false
  CONFIG_ERRORS.push(`${name} must be 0|1|true|false, got ${JSON.stringify(raw)}`)
  return fallback
}

function enumEnv(name, fallback, allowed) {
  const raw = process.env[name] ?? fallback
  if (!allowed.includes(raw)) { CONFIG_ERRORS.push(`${name} must be one of ${allowed.join('|')}, got ${JSON.stringify(raw)}`); return fallback }
  return raw
}

// ---------------------------------------------------------------------------
// masked-ssim-luma-v2 metric knobs.
// ---------------------------------------------------------------------------
// MAJOR band (score in [MAJOR, MINOR)) — 'block' (default): a MAJOR row is a gate BLOCKER.
// Promoted from advisory 2026-07-02 per the §6.5 dark-regime calibration (the band fires only
// on genuine structural divergence — 3px+ offsets, heavy blur) and the strict pixel-perfect
// doctrine. Rollback knob: SCREENSHOT_MAJOR_BAND=advisory restores the reviewed-WARN routing.
const MAJOR_BAND = enumEnv('SCREENSHOT_MAJOR_BAND', 'block', ['block', 'advisory'])
// R3 — per-project pixel-verdict routing. The pixel SIMILARITY verdict (SSIM bands, per-zone floor,
// color-only, render-extra-content) is per-project configurable and STRICT BY DEFAULT (every
// bootstrap fails closed on pixel drift; a project may deliberately downgrade):
//   • strict (default): the pixel similarity verdict may BLOCK (the pre-R3 behaviour).
//   • advisory: the pixel comparison computes + is shown, and a similarity divergence is
//     a WARN — never blocks. The STRICT fidelity signal is the design-agnostic spec-structure gate
//     (R2, compare-screen-spec) + the completeness/anti-forgery net below, both ALWAYS ON.
//     The downgrade for a design language a single global threshold over-blocks.
//   • off: computed + shown, but similarity findings are suppressed entirely.
// CRITICAL — this ONLY routes the pixel-SIMILARITY verdict. It never touches:
//   (1) the completeness / anti-forgery blockers (MISSING_*, ASPECT_MISMATCH, STALE_CAPTURE,
//       CAPTURE_IS_ORACLE_COPY/PATH_UNCONTAINED, DUPLICATE_CAPTURE, PRIMARY_STATE_UNCONFIRMED,
//       CAPTURE_LOCALE_MISMATCH, coverage) — those ALWAYS block regardless of the mode; nor
//   (2) the metric-strictness canon-pins in evidence-bundle (the comparison always RUNS at full
//       canonical strictness — sigma, mask, thresholds unchanged — only the verdict is routed).
// The DEFAULT is the committed project-config `screenshotPixelGate` (read directly, so a project's
// value actually takes effect even if the orchestrator forgot to export the env — the finding
// was that relying on the agent to set it silently degraded `strict` to `advisory`, a fail-open).
// The committed config must carry a current value. The SCREENSHOT_PIXEL_GATE env still overrides
// it for a per-run choice and is validated by enumEnv below.
const configPixelGate = (() => {
  try {
    const value = readConfig('screenshotPixelGate')
    if (value === 'strict' || value === 'advisory' || value === 'off') return value
    CONFIG_ERRORS.push(`project-config screenshotPixelGate must be strict|advisory|off, got ${JSON.stringify(value)}`)
  } catch (error) {
    CONFIG_ERRORS.push(`project-config screenshotPixelGate is unreadable: ${error.message}`)
  }
  return 'strict'
})()
const PIXEL_GATE = enumEnv('SCREENSHOT_PIXEL_GATE', configPixelGate, ['strict', 'advisory', 'off'])
// Similarity-verdict issue kinds the knob routes. Completeness/anti-forgery kinds are NOT here —
// they always block. (Already-WARN kinds like SSIM_MINOR / COLOR_DRIFT_REVIEW are harmless to list.)
const PIXEL_SIMILARITY_KINDS = new Set(['SSIM_BLOCKER', 'SSIM_MAJOR', 'SSIM_MAJOR_UNCALIBRATED', 'SSIM_MINOR', 'ZONE_SSIM_BLOCKER', 'COLOR_ONLY_MISMATCH', 'COLOR_ONLY_DIVERGENT_REGION', 'RENDER_EXTRA_CONTENT', 'COLOR_DRIFT_REVIEW'])
const METRIC_NAME = 'masked-ssim-luma-v2'
const GAUSSIAN_SIGMA = numEnv('SCREENSHOT_GAUSSIAN_SIGMA', TCFG.gaussianSigma, { min: 0.5, max: 3 })
const SHIFT_RADIUS = numEnv('SCREENSHOT_SHIFT_RADIUS', TCFG.shiftRadius, { min: 0, max: 4, integer: true })
const VAR_FLOOR = numEnv('SCREENSHOT_VAR_FLOOR', TCFG.varFloor, { min: 0, max: 65025 })
const MASK_MODE = enumEnv('SCREENSHOT_MASK_MODE', 'variance', ['variance', 'color', 'edge'])
const AA_TOLERANCE = numEnv('SCREENSHOT_AA_TOLERANCE', TCFG.aaTolerance, { min: 0, max: 3, integer: true })
const SCREENSHOT_DEBUG = boolEnv('SCREENSHOT_DEBUG', false)
const ZONE_GATE = boolEnv('SCREENSHOT_ZONE_GATE', TCFG.zoneGate)   // H3 worst-zone floor — config-true; SCREENSHOT_ZONE_GATE=0 to roll back (final gate reds it)
function parseGrid(raw, fallbackRows, fallbackCols) {
  const m = /^(\d+)x(\d+)$/.exec(String(raw || ''))
  if (!m) { if (raw != null && raw !== '') CONFIG_ERRORS.push(`SCREENSHOT_REGION_GRID must be RxC, got ${JSON.stringify(raw)}`); return [fallbackRows, fallbackCols] }
  return [Math.max(1, Math.min(32, +m[1])), Math.max(1, Math.min(32, +m[2]))]
}
const [REGION_ROWS, REGION_COLS] = parseGrid(process.env.SCREENSHOT_REGION_GRID, 8, 4)
// Perceptual colour axis: hand-rolled CIEDE2000 (ΔE00) per region — no dependency.
// Advisory: it adds report fields, never repurposes `score` and never changes `overall`.
const COLOR_AXIS = boolEnv('SCREENSHOT_COLOR_AXIS', true)
const DELTAE_PASS = numEnv('SCREENSHOT_DELTAE_PASS', TCFG.deltaEPass, { min: 0, max: 100 })
// ΔE is a per-region MEAN, so a strided subsample is statistically equivalent and
// keeps CIEDE2000's transcendentals off the hot path. Stride 2 = 1/4 the ΔE evals.
const DELTAE_STRIDE = numEnv('SCREENSHOT_DELTAE_STRIDE', TCFG.deltaEStride, { min: 1, max: 8, integer: true })
// C1-conservative: the v2 content mask is ORACLE-side only, so divergence confined to the
// oracle's background plane — content the RENDER ADDED (a rogue panel, a wrong-colour solid
// fill) — never enters `score`: a proven false-PASS. This additive probe measures the luma
// divergence OUTSIDE the mask at the winning offset and flags RENDER_EXTRA_CONTENT above the
// warn floor; score/mask semantics stay untouched. Band routing: warn (default) → WARN,
// block → BLOCKER, off → the row field is still recorded, no issue (calibration runs).
const EXTRA_CONTENT_WARN = numEnv('SCREENSHOT_EXTRA_CONTENT_WARN', TCFG.extraContentWarn, { min: 0, max: 1 })
const EXTRA_CONTENT_BAND = enumEnv('SCREENSHOT_EXTRA_CONTENT_BAND', 'warn', ['warn', 'block', 'off'])
// The probe's luma arm alone is blind to an equal-LUMINANCE wrong-COLOUR region in unmasked
// space (a saturated red panel where the oracle has equal-luma dark blue background): luma
// matches, and the ΔE00 colour axis samples only INSIDE the mask, so nothing witnessed it.
// The chroma arm counts such a pixel too — cheap dist2-style RGB pre-filter (squared distance
// > 24², the BG_TOLERANCE scale) then a ΔE00 > EXTRA_CONTENT_DELTAE confirm. Raising the
// floor widens the blind band, so the final bundle enforces the recorded value <= 10.
const EXTRA_CONTENT_DELTAE = numEnv('SCREENSHOT_EXTRA_CONTENT_DELTAE', TCFG.extraContentDeltaE, { min: 1, max: 100 })
// W5-3 — mask-dilation ring. The probe counts EVERY divergent unmasked pixel, but blur halos /
// gradient spill hugging real content land exactly there and read as render-added content over
// designed whitespace (2.8% observed vs the 2.0% floor on a correct card). Pixels within this
// many px of the oracle content-mask edge are skipped — a real rogue panel in OPEN background
// sits far from any mask edge and still trips at the same floor. RAISING the ring skips more
// pixels = weaker, so the final bundle canon-pins the recorded value lte the committed one;
// 0 disables the ring (compares MORE = stricter, always allowed). Spec-driven per-node crops
// remain rejected (the R1 laundering review) — the ring is geometry-blind and bounded.
const EXTRA_CONTENT_RING_PX = numEnv('SCREENSHOT_EXTRA_CONTENT_RING_PX', TCFG.extraContentRingPx, { min: 0, max: 16, integer: true })

function severity(score) {
  if (score >= T_PASS) return 'PASS'
  if (score >= T_MINOR) return 'MINOR'
  if (score >= T_MAJOR) return 'MAJOR'
  return 'BLOCKER'
}

// H3 — worst-zone floor: find a localized critical diff the content-weighted MEAN would hide.
// Prefer per-Figma-node zones (semantic); fall back to grid cells when no spec produced zones.
// The floor is PER ZONE by node kind ("структуру строже, текст мягче"): a TEXT zone is judged
// against the lenient Z_TEXT_BLOCKER (Robolectric font-AA ≠ Figma → text zones score low on
// pixel SSIM even when correct; wrong text is still caught by the independent spec-compare gate
// + ΔE axis), a non-text zone against the strict Z_BLOCKER. Grid cells carry no text
// classification, so the grid fallback keeps Z_BLOCKER. Returns the worst offender (lowest
// ssim) below its own floor, with `isText` carried on for the caller's message, or null.
function worstZoneViolation(res) {
  if (!res) return null
  const cells = []
  if (Array.isArray(res.zones)) {
    for (const z of res.zones) {
      const floor = z.isText ? Z_TEXT_BLOCKER : Z_BLOCKER
      if (typeof z.ssim === 'number' && (z.contentPx || 0) >= MIN_REGION_PX && z.ssim < floor) {
        cells.push({ ssim: z.ssim, contentPx: z.contentPx, label: z.stableId || z.role || z.name || 'node', isText: !!z.isText })
      }
    }
  }
  // The grid floor must not be suppressed by a zones array that yielded no scoreable cell
  // (empty spec projection / every node below MIN_REGION_PX): zones REFINE the floor, they
  // don't replace it. Fall through to grid cells whenever zones produced no candidate. Grid
  // cells have no text classification, so they keep the strict Z_BLOCKER (isText false).
  if (!cells.length && Array.isArray(res.regions)) {
    for (const r of res.regions) {
      if (typeof r.score === 'number' && (r.contentPx || 0) >= MIN_REGION_PX && r.score < Z_BLOCKER) {
        cells.push({ ssim: r.score, contentPx: r.contentPx, label: `cell r${r.row}c${r.col}`, isText: false })
      }
    }
  }
  if (!cells.length) return null
  cells.sort((a, b) => a.ssim - b.ssim)
  return cells[0]
}

function issue(severity, issueKind, message, extra = {}) {
  return Object.assign({ severity, issueKind, message }, extra)
}

function artifactRoot() {
  const root = resolve(process.env.FIGMA_COMPARE_ARTIFACTS_DIR || figmaPath('.cache', 'artifacts', 'screenshot'))
  if (process.env.FIGMA_COMPARE_ARTIFACTS_DIR) {
    const rel = cacheRelative(root)
    if (rel !== 'artifacts/screenshot') {
      CONFIG_ERRORS.push('FIGMA_COMPARE_ARTIFACTS_DIR must equal FIGMA_CACHE_ROOT/artifacts/screenshot so final evidence can validate cache-relative artifact paths')
    }
  }
  // A fresh project may not have created the consolidated cache root yet. Prove
  // and materialize that root before using it as the authority for descendants:
  // the project root is the anchor for the canonical cache, while an explicit
  // out-of-project fixture override is allowed only as a direct child of its
  // already-real parent. In both cases a symlink at the cache-root name is
  // rejected rather than followed.
  const cacheAnchor = fileGuards.isUnder(REPO_ROOT, FIGMA_CACHE_ROOT)
    ? REPO_ROOT
    : dirname(FIGMA_CACHE_ROOT)
  const cacheReady = fileGuards.realDirectoryUnder(
    cacheAnchor, FIGMA_CACHE_ROOT, { create: true, mode: 0o700 },
  )
  if (!cacheReady || !fileGuards.realDirectoryUnder(FIGMA_CACHE_ROOT, root, { create: true, mode: 0o700 })) {
    CONFIG_ERRORS.push('FIGMA compare artifact root is not a real root-anchored cache directory')
  }
  return root
}

const ARTIFACT_FILE_MAX_BYTES = 32 * 1024 * 1024
function writeArtifactFile(target, bytes) {
  const payload = Buffer.isBuffer(bytes) ? bytes : Buffer.from(String(bytes))
  const published = fileGuards.atomicReplaceRegularFileResult(
    FIGMA_CACHE_ROOT, dirname(target), target, payload,
    { create: true, directoryMode: 0o700, mode: 0o600, maxBytes: ARTIFACT_FILE_MAX_BYTES },
  )
  if (!published.ok) throw new Error(`artifact publication refused for ${basename(target)}: ${published.code}`)
}

async function writeArtifactImage(image, target) {
  writeArtifactFile(target, await image.getBuffer('image/png'))
}

function artifactPublicPath(file) {
  return cacheRelative(file) || displayPath(file)
}

function statEntry(file) {
  try {
    const st = statSync(file)
    return { bytes: st.isFile() ? st.size : 0 }
  } catch {
    return { bytes: 0 }
  }
}

function imageEntry({ id, kind, file, screen, theme, status, width, height }) {
  return Object.assign({
    id,
    kind,
    screen,
    theme,
    themeKey: theme,
    status,
    path: artifactPublicPath(file),
    hash: fileHash(file),
    width,
    height,
    mime: 'image/png',
  }, statEntry(file))
}

function artifactManifestEntry({ id, file }) {
  return Object.assign({
    id,
    kind: 'manifest',
    path: artifactPublicPath(file),
    hash: fileHash(file),
    mime: 'application/json',
  }, statEntry(file))
}

function writePixel(data, i, r, g, b, a = 255) {
  data[i] = r
  data[i + 1] = g
  data[i + 2] = b
  data[i + 3] = a
}

async function writeCompareArtifacts(Jimp, {
  root,
  stem,
  runId,
  sequence,
  screen,
  theme,
  status,
  score,
  coverage,
  metric,
  oraclePath,
  roboPath,
  oracleImage,
  renderImage,
  alignment = { dx: 0, dy: 0 },
}) {
  if (!oracleImage || !renderImage) return null
  const stemSeg = artifactSegment(stem)
  const runSeg = artifactSegment(runId)
  const groupSeg = `${String(sequence).padStart(3, '0')}-${artifactSegment(screen)}-${artifactSegment(theme)}`
  const dir = join(root, stemSeg, runSeg, groupSeg)

  const W = oracleImage.bitmap.width, H = oracleImage.bitmap.height
  const figmaPng = join(dir, 'figma.png')
  const actualPng = join(dir, 'actual.png')
  const diffPng = join(dir, 'diff.png')
  const overlayPng = join(dir, 'overlay.png')
  await writeArtifactImage(oracleImage, figmaPng)
  await writeArtifactImage(renderImage, actualPng)

  const diff = new Jimp({ width: W, height: H, color: 0x101010ff })
  const overlay = new Jimp({ width: W, height: H, color: 0x101010ff })
  const od = oracleImage.bitmap.data, rd = renderImage.bitmap.data
  const dd = diff.bitmap.data, vd = overlay.bitmap.data
  // The diff/overlay read the render at the SSIM-winning alignment offset so the
  // picture explains the number; actual.png itself stays the unshifted real capture.
  const dx = (alignment && alignment.dx) | 0, dy = (alignment && alignment.dy) | 0
  const diffFloor = 6 + 2 * AA_TOLERANCE
  const overlayFloor = diffFloor + 4
  for (let y = 0; y < H; y++) {
    let sy = y + dy; if (sy < 0) sy = 0; else if (sy >= H) sy = H - 1
    for (let x = 0; x < W; x++) {
      const i = (y * W + x) << 2
      let sx = x + dx; if (sx < 0) sx = 0; else if (sx >= W) sx = W - 1
      const j = (sy * W + sx) << 2
      const dr = Math.abs(od[i] - rd[j])
      const dg = Math.abs(od[i + 1] - rd[j + 1])
      const db = Math.abs(od[i + 2] - rd[j + 2])
      const delta = Math.max(dr, dg, db)
      if (delta < diffFloor) {
        writePixel(dd, i, 18, 18, 18)
      } else {
        const heat = Math.min(255, delta * 3)
        writePixel(dd, i, 255, Math.max(0, 210 - heat), 0)
      }
      const br = Math.round((od[i] + rd[j]) / 2)
      const bg = Math.round((od[i + 1] + rd[j + 1]) / 2)
      const bb = Math.round((od[i + 2] + rd[j + 2]) / 2)
      if (delta >= overlayFloor) writePixel(vd, i, 255, Math.round(bg * 0.35), Math.round(bb * 0.35))
      else writePixel(vd, i, br, bg, bb)
    }
  }
  await writeArtifactImage(diff, diffPng)
  await writeArtifactImage(overlay, overlayPng)

  const baseId = groupSeg
  const artifacts = {
    figma: imageEntry({ id: `${baseId}-figma`, kind: 'figma', file: figmaPng, screen, theme, status, width: W, height: H }),
    actual: imageEntry({ id: `${baseId}-actual`, kind: 'actual', file: actualPng, screen, theme, status, width: W, height: H }),
    diff: imageEntry({ id: `${baseId}-diff`, kind: 'diff', file: diffPng, screen, theme, status, width: W, height: H }),
    overlay: imageEntry({ id: `${baseId}-overlay`, kind: 'overlay', file: overlayPng, screen, theme, status, width: W, height: H }),
  }
  const manifestPath = join(dir, 'manifest.json')
  const manifest = {
    schemaVersion: 1,
    kind: 'screenshot-compare-artifact-manifest',
    id: baseId,
    taskStem: stem,
    pipelineRunId: runId,
    screen,
    theme,
    themeKey: theme,
    status,
    score: score ?? null,
    coverage: coverage ?? null,
    metric,
    dimensions: { width: W, height: H },
    source: {
      figma: displayPath(oraclePath),
      actual: displayPath(roboPath),
      figmaHash: fileHash(oraclePath),
      actualHash: fileHash(roboPath),
    },
    artifacts,
    generatedAt: new Date().toISOString(),
  }
  writeArtifactFile(manifestPath, JSON.stringify(manifest, null, 2) + '\n')
  return {
    schemaVersion: 1,
    id: baseId,
    baseDir: artifactPublicPath(dir),
    screen,
    theme,
    themeKey: theme,
    status,
    score: score ?? null,
    coverage: coverage ?? null,
    dimensions: { width: W, height: H },
    manifest: artifactManifestEntry({ id: `${baseId}-manifest`, file: manifestPath }),
    artifacts,
  }
}

// Emit artifacts for a comparison that could NOT produce a per-pixel diff: the oracle and
// the render are different sizes (ASPECT_MISMATCH) or there is no render at all
// (MISSING_CAPTURE). Writes figma.png always and actual.png when a render exists — at their
// OWN native sizes — and NO diff/overlay (there is no valid pixel correspondence to draw).
// Same manifest / artifact-entry / hashing shape as writeCompareArtifacts (minus diff/overlay),
// so the server's vetted `compareArtifactFile` path treats these bytes identically (each ref
// is independently hash-bound + path-confined). Deliberately a SEPARATE writer so the
// happy-path writeCompareArtifacts stays byte-for-byte untouched. `status` stays the failure
// status (ASPECT_MISMATCH / MISSING_CAPTURE) — this only makes the reference visible, it never
// changes the verdict. Score/coverage are null (no comparison was computed).
async function writeReferenceArtifacts(Jimp, {
  root, stem, runId, sequence, screen, theme, status, metric,
  oraclePath, roboPath, oracleImage, renderImage,
}) {
  if (!oracleImage) return null
  const stemSeg = artifactSegment(stem)
  const runSeg = artifactSegment(runId)
  const groupSeg = `${String(sequence).padStart(3, '0')}-${artifactSegment(screen)}-${artifactSegment(theme)}`
  const dir = join(root, stemSeg, runSeg, groupSeg)

  const W = oracleImage.bitmap.width, H = oracleImage.bitmap.height
  const figmaPng = join(dir, 'figma.png')
  await writeArtifactImage(oracleImage, figmaPng)
  const artifacts = {
    figma: imageEntry({ id: `${groupSeg}-figma`, kind: 'figma', file: figmaPng, screen, theme, status, width: W, height: H }),
  }
  const source = { figma: displayPath(oraclePath), figmaHash: fileHash(oraclePath) }
  if (renderImage) {
    const actualPng = join(dir, 'actual.png')
    await writeArtifactImage(renderImage, actualPng)
    artifacts.actual = imageEntry({ id: `${groupSeg}-actual`, kind: 'actual', file: actualPng, screen, theme, status, width: renderImage.bitmap.width, height: renderImage.bitmap.height })
    source.actual = displayPath(roboPath)
    source.actualHash = roboPath ? fileHash(roboPath) : null
  }
  const manifestPath = join(dir, 'manifest.json')
  const manifest = {
    schemaVersion: 1,
    kind: 'screenshot-compare-artifact-manifest',
    id: groupSeg,
    taskStem: stem,
    pipelineRunId: runId,
    screen,
    theme,
    themeKey: theme,
    status,
    score: null,
    coverage: null,
    metric,
    dimensions: { width: W, height: H },
    source,
    artifacts,
    generatedAt: new Date().toISOString(),
  }
  writeArtifactFile(manifestPath, JSON.stringify(manifest, null, 2) + '\n')
  return {
    schemaVersion: 1,
    id: groupSeg,
    baseDir: artifactPublicPath(dir),
    screen,
    theme,
    themeKey: theme,
    status,
    score: null,
    coverage: null,
    dimensions: { width: W, height: H },
    manifest: artifactManifestEntry({ id: `${groupSeg}-manifest`, file: manifestPath }),
    artifacts,
  }
}

const ARTIFACT_PRUNE_ENTRY_MAX = 4096
const ARTIFACT_PRUNE_DEPTH_MAX = 12
function removeArtifactDirectory(target, budget, depth = 0) {
  if (depth > ARTIFACT_PRUNE_DEPTH_MAX || budget.count >= ARTIFACT_PRUNE_ENTRY_MAX) return false
  const parent = dirname(target)
  const inspected = fileGuards.inspectEntryUnder(FIGMA_CACHE_ROOT, parent, target)
  if (inspected && inspected.status === 'missing') return true
  if (!inspected || inspected.status !== 'present' || !inspected.stat ||
      !inspected.stat.isDirectory() || inspected.stat.isSymbolicLink()) return false
  const listed = fileGuards.boundedDirectoryNamesUnder(
    FIGMA_CACHE_ROOT, target, ARTIFACT_PRUNE_ENTRY_MAX - budget.count,
  )
  if (!listed.ok) return false
  for (const name of listed.names) {
    budget.count += 1
    const child = join(target, name)
    const entry = fileGuards.inspectEntryUnder(FIGMA_CACHE_ROOT, target, child)
    if (!entry || entry.status !== 'present' || !entry.stat) return false
    if (entry.stat.isFile() && !entry.stat.isSymbolicLink()) {
      if (!fileGuards.unlinkRegularFileUnder(FIGMA_CACHE_ROOT, target, child, { allowMissing: true })) return false
    } else if (entry.stat.isDirectory() && !entry.stat.isSymbolicLink()) {
      if (!removeArtifactDirectory(child, budget, depth + 1)) return false
    } else return false
  }
  return fileGuards.removeEmptyDirectoryUnder(FIGMA_CACHE_ROOT, parent, target)
}

function pruneArtifactRuns(root, stem, activeRunId, keepRuns) {
  if (!keepRuns) return
  const stemDir = join(root, artifactSegment(stem))
  const activeRunSeg = artifactSegment(activeRunId)
  const listed = fileGuards.boundedDirectoryNamesUnder(
    FIGMA_CACHE_ROOT, stemDir, ARTIFACT_PRUNE_ENTRY_MAX,
  )
  if (!listed.ok || listed.exists === false) return
  const rows = []
  for (const name of listed.names) {
    const dir = join(stemDir, name)
    const entry = fileGuards.inspectEntryUnder(FIGMA_CACHE_ROOT, stemDir, dir)
    if (!entry || entry.status !== 'present' || !entry.stat ||
        !entry.stat.isDirectory() || entry.stat.isSymbolicLink()) return
    rows.push({ name, dir, mtimeMs: entry.stat.mtimeMs })
  }
  rows.sort((a, b) => b.mtimeMs - a.mtimeMs)
  const keep = new Set(rows.some((row) => row.name === activeRunSeg) ? [activeRunSeg] : [])
  for (const row of rows) {
    if (keep.size >= keepRuns) break
    keep.add(row.name)
  }
  for (const row of rows) {
    if (!keep.has(row.name)) removeArtifactDirectory(row.dir, { count: 0 })
  }
}

async function loadJimp() {
  try {
    const { Jimp } = await import('jimp')
    return Jimp
  } catch {
    console.error('ERROR: jimp not installed. Run `npm install` in orchestrator/figma/ first.')
    process.exit(1)
  }
}

function backgroundColor(data, W, H) {
  const hist = new Map()
  for (let y = 0; y < H; y += 2) {
    for (let x = 0; x < W; x += 2) {
      const i = (y * W + x) << 2
      const key = ((data[i] >> 3) << 10) | ((data[i + 1] >> 3) << 5) | (data[i + 2] >> 3)
      hist.set(key, (hist.get(key) ?? 0) + 1)
    }
  }
  let bestKey = 0, bestN = -1
  for (const [k, n] of hist) if (n > bestN) { bestN = n; bestKey = k }
  return [((bestKey >> 10) & 31) << 3, ((bestKey >> 5) & 31) << 3, (bestKey & 31) << 3]
}

function dist2(data, i, bg) {
  const dr = data[i] - bg[0], dg = data[i + 1] - bg[1], db = data[i + 2] - bg[2]
  return dr * dr + dg * dg + db * db
}

// ---------------------------------------------------------------------------
// masked-ssim-luma-v2 helpers. Hand-rolled Float32 separable convolution — NEVER
// Jimp.gaussian()/convolution()/blur() (orders of magnitude slower). The two-pass
// separable kernel (radius = ceil(3·sigma)) is the metric's hot path; keep it lean.
// ---------------------------------------------------------------------------
function toLuma(data, n) {
  const out = new Float32Array(n)
  for (let p = 0; p < n; p++) { const i = p << 2; out[p] = 0.299 * data[i] + 0.587 * data[i + 1] + 0.114 * data[i + 2] }
  return out
}
function gaussKernel(sigma) {
  const radius = Math.max(1, Math.ceil(sigma * 3))
  const k = new Float32Array(2 * radius + 1)
  let sum = 0
  for (let i = -radius; i <= radius; i++) { const v = Math.exp(-(i * i) / (2 * sigma * sigma)); k[i + radius] = v; sum += v }
  for (let i = 0; i < k.length; i++) k[i] /= sum
  return { k, radius }
}
function sepBlur(src, W, H, k, radius, tmp) {
  if (!tmp) tmp = new Float32Array(W * H)
  const out = new Float32Array(W * H)
  for (let y = 0; y < H; y++) {
    const row = y * W
    for (let x = 0; x < W; x++) {
      let acc = 0
      for (let t = -radius; t <= radius; t++) { let xx = x + t; if (xx < 0) xx = 0; else if (xx >= W) xx = W - 1; acc += src[row + xx] * k[t + radius] }
      tmp[row + x] = acc
    }
  }
  for (let y = 0; y < H; y++) {
    for (let x = 0; x < W; x++) {
      let acc = 0
      for (let t = -radius; t <= radius; t++) { let yy = y + t; if (yy < 0) yy = 0; else if (yy >= H) yy = H - 1; acc += tmp[yy * W + x] * k[t + radius] }
      out[y * W + x] = acc
    }
  }
  return out
}
function shiftLuma(src, W, H, dx, dy) {
  if (!dx && !dy) return src
  const out = new Float32Array(W * H)
  for (let y = 0; y < H; y++) {
    let sy = y + dy; if (sy < 0) sy = 0; else if (sy >= H) sy = H - 1
    for (let x = 0; x < W; x++) {
      let sx = x + dx; if (sx < 0) sx = 0; else if (sx >= W) sx = W - 1
      out[y * W + x] = src[sy * W + sx]
    }
  }
  return out
}
// Pick the (dx,dy) in [-R..R]^2 that minimises masked mean-absolute luma error —
// recovers the ≤R px sub-pixel jitter between two rasterizers. Cheap O(N)/offset.
function bestOffset(lumaO, lumaR, W, H, mask, R) {
  let best = { dx: 0, dy: 0, err: Infinity }
  for (let dy = -R; dy <= R; dy++) {
    for (let dx = -R; dx <= R; dx++) {
      let err = 0, cnt = 0
      for (let y = 0; y < H; y++) {
        let sy = y + dy; if (sy < 0 || sy >= H) continue
        for (let x = 0; x < W; x++) {
          const p = y * W + x
          if (!mask[p]) continue
          let sx = x + dx; if (sx < 0 || sx >= W) continue
          err += Math.abs(lumaO[p] - lumaR[sy * W + sx]); cnt++
        }
      }
      if (cnt) { const e = err / cnt; if (e < best.err - 1e-6) best = { dx, dy, err: e } }
    }
  }
  return best
}

// ---------------------------------------------------------------------------
// Direction B colour axis: sRGB(8-bit) → CIELAB(D65) → CIEDE2000 (ΔE00). Hand-
// rolled (no dependency); Sharma's closed form, validated against his test
// vectors in compare-screenshots.test.mjs. ΔE00 ≈ 1 is one just-noticeable diff.
// Inputs are treated as sRGB: both PNGs are assumed sRGB; a non-sRGB ICC
// profile would make ΔE meaningless, but Jimp decodes to sRGB-assumed bytes.
// ---------------------------------------------------------------------------
function srgbToLinear(c) { c /= 255; return c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4 }
function rgbToLab(r, g, b) {
  const R = srgbToLinear(r), G = srgbToLinear(g), B = srgbToLinear(b)
  let X = (R * 0.4124564 + G * 0.3575761 + B * 0.1804375) / 0.95047
  let Y = (R * 0.2126729 + G * 0.7151522 + B * 0.0721750) / 1.00000
  let Z = (R * 0.0193339 + G * 0.1191920 + B * 0.9503041) / 1.08883
  const f = (t) => (t > 216 / 24389 ? Math.cbrt(t) : (24389 / 27 * t + 16) / 116)
  const fx = f(X), fy = f(Y), fz = f(Z)
  return [116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz)]
}
const D2R = Math.PI / 180
function deltaE00(lab1, lab2) {
  const [L1, a1, b1] = lab1, [L2, a2, b2] = lab2
  const C1 = Math.hypot(a1, b1), C2 = Math.hypot(a2, b2)
  const Cbar = (C1 + C2) / 2
  const G = 0.5 * (1 - Math.sqrt(Cbar ** 7 / (Cbar ** 7 + 25 ** 7)))
  const a1p = (1 + G) * a1, a2p = (1 + G) * a2
  const C1p = Math.hypot(a1p, b1), C2p = Math.hypot(a2p, b2)
  const hp = (b, ap) => { if (b === 0 && ap === 0) return 0; let h = Math.atan2(b, ap) / D2R; return h < 0 ? h + 360 : h }
  const h1p = hp(b1, a1p), h2p = hp(b2, a2p)
  const dLp = L2 - L1
  const dCp = C2p - C1p
  let dhp = 0
  if (C1p * C2p !== 0) { dhp = h2p - h1p; if (dhp > 180) dhp -= 360; else if (dhp < -180) dhp += 360 }
  const dHp = 2 * Math.sqrt(C1p * C2p) * Math.sin((dhp / 2) * D2R)
  const Lbp = (L1 + L2) / 2, Cbp = (C1p + C2p) / 2
  let hbp
  if (C1p * C2p === 0) hbp = h1p + h2p
  else if (Math.abs(h1p - h2p) > 180) hbp = (h1p + h2p + 360) / 2
  else hbp = (h1p + h2p) / 2
  const T = 1 - 0.17 * Math.cos((hbp - 30) * D2R) + 0.24 * Math.cos((2 * hbp) * D2R) + 0.32 * Math.cos((3 * hbp + 6) * D2R) - 0.20 * Math.cos((4 * hbp - 63) * D2R)
  const dTheta = 30 * Math.exp(-(((hbp - 275) / 25) ** 2))
  const Rc = 2 * Math.sqrt(Cbp ** 7 / (Cbp ** 7 + 25 ** 7))
  const Sl = 1 + (0.015 * (Lbp - 50) ** 2) / Math.sqrt(20 + (Lbp - 50) ** 2)
  const Sc = 1 + 0.045 * Cbp
  const Sh = 1 + 0.015 * Cbp * T
  const Rt = -Math.sin((2 * dTheta) * D2R) * Rc
  return Math.sqrt((dLp / Sl) ** 2 + (dCp / Sc) ** 2 + (dHp / Sh) ** 2 + Rt * (dCp / Sc) * (dHp / Sh))
}

// R1 device-chrome exclusion. Returns deterministic top/bottom exclusion heights for a `screen`
// only (a dialog/component/overlay has no system chrome). The strict R6 detector is consulted only
// to disable the top exclusion when status-bar-like geometry is unresolved; it never expands a band.
//
// HARD ceilings (constants — not env, not spec) on the band as a fraction of the frame: the
// LARGEST a legitimate system bar occupies (a status bar is ~2.5-4% of a phone's height, a nav
// bar ~5-8%). These make the band UNCORRUPTABLE by the AGENT-AUTHORED spec: `frameSizeDp` cannot
// be re-derived from the design by the final gate, so a hostile/units-wrong tiny frameSizeDp.h
// would otherwise balloon the dp→px scale and blind a large slice of a REAL screen (a proven
// laundering exploit). Whatever frameSizeDp says, the band can never exceed these caps.
const STATUS_BAND_CAP_FRACTION = 0.06
const NAV_BAND_CAP_FRACTION = 0.09
function chromeBandsPx(kind, spec, W, H) {
  // A chromeCrop stamp proves normalize-oracle already removed device chrome and shifted the
  // remaining spec/PNG to app-content coordinates. Masking again would hide the app's own first
  // 24dp / last 48dp (for example a top app bar or bottom action row).
  if (kind !== 'screen' || H <= 0 || spec?.chromeCrop) return { top: 0, bottom: 0 }
  // Trust frameSizeDp for the dp→px scale ONLY when it is CONSISTENT with the oracle's real aspect
  // (both axes yield ~the same px-per-dp). A corrupt/units-wrong frameSizeDp.h makes perH ≫ perW →
  // inconsistent → fall back to the fixed cap fractions. Even a CONSISTENT-but-tiny frame is still
  // bounded by the caps below, so the consistency check is a refinement; the caps are the backstop.
  const fd = spec && spec.frameSizeDp
  let pxPerDp = null
  if (fd && fd.w > 0 && fd.h > 0 && W > 0) {
    const perW = W / fd.w, perH = H / fd.h
    if (perW > 0 && perH > 0 && Math.max(perW, perH) / Math.min(perW, perH) <= 1.5) pxPerDp = perH
  }
  const statusPx = pxPerDp ? STATUS_BAR_DP * pxPerDp : STATUS_BAND_CAP_FRACTION * H
  const navPx = pxPerDp ? NAV_BAR_DP * pxPerDp : NAV_BAND_CAP_FRACTION * H
  // A geometry-only top strip is deliberately NOT normalized by normalize-oracle: it may be
  // the app's own header, so IOS_CHROME_SUSPECTED asks for owner resolution. Do not then hide
  // that same ambiguous strip behind the generic status-bar mask. Comparing it is the strict,
  // evidence-preserving fallback; the bottom band remains independent.
  const topSuspected = detectChrome(spec).suspects.length > 0
  const top = topSuspected ? 0 : Math.max(0, Math.min(Math.round(STATUS_BAND_CAP_FRACTION * H), Math.round(statusPx)))
  const bottom = Math.max(0, Math.min(Math.round(NAV_BAND_CAP_FRACTION * H), Math.round(navPx)))
  return { top, bottom }
}

// NOTE (R1 scope): a spec-driven CONTENT CROP for [dialog]/[overlay] was intentionally NOT shipped.
// An adversarial review proved it a laundering surface (a single small element bbox could crop away
// most of the frame) AND a net weakening of the legitimate shape-A case (a composed-over-host
// dialog's host content SHOULD match by the content-parity rule — excluding it from strict SSIM is
// weaker, not stricter). Dialogs already compare "content-only" safely: the existing oracle-side
// union content mask excludes a uniform scrim (low variance → background) automatically, and the
// device-chrome band mask above is correctly SCREEN-ONLY (a dialog has no system status/nav bar),
// so its own content — including any top row a screen's status band would blind — is fully compared.

// W5-3: box-dilate the content mask by r px (two separable sliding-window passes, O(n)) — the
// extra-content probe skips pixels inside the dilated footprint (mask + ring), so blur halos /
// gradient spill hugging real content never count as render-added content, while a rogue panel
// in OPEN background sits far from any mask edge and still trips at the same floor.
function dilateMask(mask, W, H, r) {
  if (!r) return mask
  const tmp = new Uint8Array(mask.length)
  const out = new Uint8Array(mask.length)
  for (let y = 0; y < H; y++) {
    const row = y * W
    let cnt = 0
    for (let x = -r; x < W; x++) {
      if (x + r < W && mask[row + x + r]) cnt++
      if (x - r - 1 >= 0 && mask[row + x - r - 1]) cnt--
      if (x >= 0) tmp[row + x] = cnt > 0 ? 1 : 0
    }
  }
  for (let x = 0; x < W; x++) {
    let cnt = 0
    for (let y = -r; y < H; y++) {
      if (y + r < H && tmp[(y + r) * W + x]) cnt++
      if (y - r - 1 >= 0 && tmp[(y - r - 1) * W + x]) cnt--
      if (y >= 0) out[y * W + x] = cnt > 0 ? 1 : 0
    }
  }
  return out
}

// Hardened metric: luma + separable-Gaussian SSIM, union (color∨variance) content
// mask, sub-pixel shift search, record-and-rescale, per-region scores, no clamp.
async function compareImages(Jimp, oraclePath, roboPath, spec, kind) {
  let oracle, render
  try {
    oracle = await Jimp.read(oraclePath)
    render = await Jimp.read(roboPath)
  } catch (e) {
    return { score: null, status: 'REVIEW_REQUIRED', error: `failed to read images: ${e.message}` }
  }
  let W = oracle.bitmap.width, H = oracle.bitmap.height
  // Native (pre-rescale) capture size: record-and-rescale silently normalizes a wrong-canvas
  // capture toward the oracle, so the caller's kind-geometry check needs the size the capture
  // was ACTUALLY taken at, not the post-fit one.
  const renderNative = { w: render.bitmap.width, h: render.bitmap.height }
  let scaleFactor = 1
  let aspectSkew = null
  if (render.bitmap.width !== W || render.bitmap.height !== H) {
    const aO = W / H, aR = render.bitmap.width / render.bitmap.height
    if (Math.abs(aO - aR) / aO > ASPECT_TOL) {
      // Different sizes → no valid per-pixel comparison, but surface BOTH images (each at its
      // own native size) so the Done view can show the design next to the wrong-shaped render.
      // `referenceOnly` tells the caller to use writeReferenceArtifacts (figma+actual, no diff).
      // R6-3 backstop hint (diagnostic only — the row still bails ASPECT_MISMATCH, never
      // routable): an unnormalized cache whose oracle is TALLER than the capture by an
      // iOS-chrome-sized band (status bar ~44-47dp + home indicator ~34dp) names its likely
      // cause instead of today's silent mystery. Chrome-stamped specs never carry the hint.
      let chromeHint = ''
      if (spec && spec.frameSizeDp && spec.frameSizeDp.h > 0 && !spec.chromeCrop) {
        const dpPerPx = spec.frameSizeDp.h / H
        const renderHAtOracleWidth = render.bitmap.height * (W / render.bitmap.width)
        const extraDp = (H - renderHAtOracleWidth) * dpPerPx
        if (extraDp >= 40 && extraDp <= 90) chromeHint = ` — IOS_CHROME_SUSPECTED: the oracle is ~${Math.round(extraDp)}dp taller than the capture, likely embedded iOS device chrome (the "9:41" status bar / home indicator); re-pull the screens so normalize-oracle strips it at the pull boundary — never crop the capture and never lower thresholds`
      }
      return { score: null, status: 'ASPECT_MISMATCH', error: `aspect mismatch: oracle ${W}x${H} vs render ${render.bitmap.width}x${render.bitmap.height}${chromeHint}`, artifactImages: { oracle, render }, referenceOnly: true }
    }
    // #18: the within-tolerance resize below is anisotropic — record the normalized aspect
    // skew (>=1) BEFORE the force-fit so the distortion survives into the report row.
    aspectSkew = aO >= aR ? aO / aR : aR / aO
    // Record-and-rescale: downscale the LARGER toward the smaller, never upscale.
    if (render.bitmap.width * render.bitmap.height >= W * H) {
      scaleFactor = W / render.bitmap.width
      render = render.resize({ w: W, h: H })
    } else {
      scaleFactor = render.bitmap.width / W
      oracle = oracle.resize({ w: render.bitmap.width, h: render.bitmap.height })
      W = oracle.bitmap.width; H = oracle.bitmap.height
    }
  }

  const od = oracle.bitmap.data, rd = render.bitmap.data
  const n = W * H
  const blurTmp = new Float32Array(n)   // shared horizontal-pass scratch reused by every sepBlur
  const lumaO = toLuma(od, n), lumaR = toLuma(rd, n)

  // R1 normalization — exclude the device-chrome bands (screen kind only) BY CONSTRUCTION so an
  // oracle and a capture that cover DIFFERENT regions (a status bar the designer drew in only one
  // image) can't score a garbage low-similarity. An excluded band pixel is invisible to EVERY arm:
  // it is never `content` (drops out of SSIM score / zones / regions / ΔE and the color-only
  // verdict) and never a RENDER_EXTRA_CONTENT candidate (the probe skips it too) — that is correct
  // for the band ONLY, because a status/nav bar legitimately differs between oracle and capture.
  // The bands are hard-capped (chromeBandsPx), so no spec can blind more than STATUS_CAP+NAV_CAP.
  const excluded = new Uint8Array(n)
  const bands = chromeBandsPx(kind, spec, W, H)
  let excludedPx = 0
  if (bands.top || bands.bottom) {
    for (let y = 0; y < H; y++) {
      if (!(y < bands.top || y >= H - bands.bottom)) continue
      const rowBase = y * W
      for (let x = 0; x < W; x++) { excluded[rowBase + x] = 1; excludedPx++ }
    }
  }

  // Union content mask: color-key OR local luma variance. The color key keeps
  // solid fills; the variance term recovers dark-on-dark content the key is blind to, so
  // dark UIs no longer mask their content as background (the named saturation bug).
  const bg = backgroundColor(od, W, H)
  const tol2 = BG_TOLERANCE * BG_TOLERANCE
  const { k: vk, radius: vr } = gaussKernel(1.2)
  const muL = sepBlur(lumaO, W, H, vk, vr, blurTmp)
  const lumaO2 = new Float32Array(n)
  for (let p = 0; p < n; p++) lumaO2[p] = lumaO[p] * lumaO[p]
  const muL2 = sepBlur(lumaO2, W, H, vk, vr, blurTmp)
  const mask = new Uint8Array(n)
  let maskedPx = 0
  for (let p = 0; p < n; p++) {
    const localVar = Math.max(0, muL2[p] - muL[p] * muL[p])
    let content
    if (MASK_MODE === 'color') content = dist2(od, p << 2, bg) > tol2
    else if (MASK_MODE === 'edge') content = localVar > VAR_FLOOR
    else content = (dist2(od, p << 2, bg) > tol2) || (localVar > VAR_FLOOR)   // 'variance' = union (default)
    if (content && !excluded[p]) { mask[p] = 1; maskedPx++ }   // R1: an excluded chrome/scrim pixel is never content
  }
  // Coverage is the content fraction of the COMPARABLE area (the frame minus the R1-excluded
  // device-chrome band) — NOT the full frame. Dividing by the full frame would understate coverage
  // by the excluded fraction and could tip a sparse-but-real screen below MIN_COVERAGE into the
  // softer color-only path purely because a band was masked.
  const comparablePx = n - excludedPx
  const coverage = comparablePx > 0 ? maskedPx / comparablePx : 0
  if (!maskedPx || coverage < MIN_COVERAGE) {
    // A legitimately-uniform oracle (solid [component] fill) can never grow structural
    // content, so LOW_CONTENT_ORACLE here would deadlock the gate forever on deterministic
    // input. A solid fill's fidelity IS its colour: degrade to a whole-frame mean-ΔE00
    // verdict (PASS ≤ DELTAE_PASS, else BLOCKER).
    const labCache = new Map()
    const labAt = (data, i) => {
      const key = (data[i] << 16) | (data[i + 1] << 8) | data[i + 2]
      let v = labCache.get(key)
      if (v === undefined) { v = rgbToLab(data[i], data[i + 1], data[i + 2]); labCache.set(key, v) }
      return v
    }
    // Dilution guard: the verdict below is a whole-frame MEAN ΔE00, so sparse-but-severe
    // divergence — e.g. a bright wrong panel over ~3% of an otherwise-matching uniform fill —
    // averages down under DELTAE_PASS and would PASS while the mask/extra-content probe never
    // runs (this branch returns early). Track, alongside the mean, the FRACTION of sampled
    // pixels whose per-pixel ΔE00 exceeds 10 (a clearly-visible local divergence); above
    // COLOR_DIVERGENT_FRACTION it fails BLOCKER regardless of the mean. A genuinely-uniform
    // matching component has ~0 divergent pixels and still PASSes.
    const COLOR_DIVERGENT_DELTAE = 10
    const COLOR_DIVERGENT_FRACTION = 0.02
    let deSum = 0, deN = 0, divergentN = 0
    for (let y = 0; y < H; y += DELTAE_STRIDE) {
      const rowBase = y * W
      for (let x = 0; x < W; x += DELTAE_STRIDE) {
        if (excluded[rowBase + x]) continue   // R1: chrome/scrim never enters the color-only verdict either
        const i = (rowBase + x) << 2
        const de = deltaE00(labAt(od, i), labAt(rd, i))
        deSum += de; deN++
        if (de > COLOR_DIVERGENT_DELTAE) divergentN++
      }
    }
    const meanDeltaE = deN ? deSum / deN : 0
    const colorDivergentFraction = deN ? divergentN / deN : 0
    const colorDivergent = colorDivergentFraction > COLOR_DIVERGENT_FRACTION
    return {
      score: null, coverage, mode: 'color-only', meanDeltaE, colorDivergentFraction,
      status: (meanDeltaE <= DELTAE_PASS && !colorDivergent) ? 'PASS' : 'BLOCKER',
      colorDivergent, chromeExcludedPx: excludedPx,
      error: null, artifactImages: { oracle, render },
      dimensions: { width: W, height: H }, alignment: { dx: 0, dy: 0 }, scaleFactor, aspectSkew, renderNative,
    }
  }

  // Per-Figma-node zones: project each spec element's bboxDp into pixel space and
  // paint a node-id map (smaller, more-specific nodes win on overlap), so SSIM/ΔE can
  // be scored per declared element — "which element diverged", not just "which grid cell".
  let nodeMap = null, nodeMeta = null, nodeSsim = null, nodeN = null, nodeDe = null, nodeDeN = null
  let offFrameElements = null
  if (spec && spec.frameSizeDp && spec.frameSizeDp.w > 0 && spec.frameSizeDp.h > 0 && Array.isArray(spec.elements) && spec.elements.length) {
    const pxX = W / spec.frameSizeDp.w, pxY = H / spec.frameSizeDp.h
    const els = spec.elements.filter((e) => e && e.bboxDp && e.bboxDp.w > 0 && e.bboxDp.h > 0)
    const order = els.map((_, i) => i).sort((a, b) => (els[b].bboxDp.w * els[b].bboxDp.h) - (els[a].bboxDp.w * els[a].bboxDp.h))
    nodeMap = new Int32Array(n).fill(-1)
    nodeMeta = []
    const offFrame = []
    for (const oi of order) {
      const e = els[oi]
      const rx0 = Math.round(e.bboxDp.x * pxX), ry0 = Math.round(e.bboxDp.y * pxY)
      const rx1 = Math.round((e.bboxDp.x + e.bboxDp.w) * pxX), ry1 = Math.round((e.bboxDp.y + e.bboxDp.h) * pxY)
      const x0 = Math.max(0, rx0), y0 = Math.max(0, ry0)
      const x1 = Math.min(W, rx1), y1 = Math.min(H, ry1)
      // A projection clipped fully (or mostly) away means the bbox is not frame-relative
      // (canvas-absolute coords) or overflows the frame — record it instead of silently
      // dropping the element from zone scoring (the caller emits ZONE_OFF_FRAME).
      const projArea = Math.max(0, rx1 - rx0) * Math.max(0, ry1 - ry0)
      const clipArea = Math.max(0, x1 - x0) * Math.max(0, y1 - y0)
      if (projArea > 0 && clipArea < projArea * 0.5) offFrame.push(e.stableId || e.name || 'unnamed')
      if (x1 <= x0 || y1 <= y0) continue
      const mi = nodeMeta.length
      for (let yy = y0; yy < y1; yy++) { const rr = yy * W; for (let xx = x0; xx < x1; xx++) nodeMap[rr + xx] = mi }
      // isText: an element carrying a textStyle, or whose role reads as text — drives the zone
      // gate's lenient per-text floor (Robolectric font-AA ≠ Figma). Kept declarative + role
      // string-match so a spec that only sets `role` (no textStyle) still classifies.
      // GLYPH-rendering roles only (their font-AA is the flaky part) — NOT container components
      // like chip/badge/tag/icon whose SHAPE/background must stay under the strict floor. Covers
      // the numeric/label family (value/number/amount/stat/…) that dominates an analytics UI and
      // would otherwise get the strict non-text floor + font-AA false-blocks.
      const isText = !!(e.textStyle) || /text|label|title|heading|subtitle|body|caption|paragraph|value|number|numeric|amount|price|count|stat|metric|mono|code|overline|display|hint|placeholder|helper|error|link|name|email|username|time|date/i.test(String(e.role || ''))
      // isGlyph is STRICTER than isText: only a node the pull explicitly marked with a
      // `textStyle` is a proven glyph node whose ΔE is font-substitution noise (W5-2). The
      // broad role-substring `isText` drives the lenient SSIM zone floor (a calibration
      // choice), but the ΔE quarantine uses isGlyph so a role-only CONTAINER (e.g. role
      // "name" on a coloured card) never blinds the equal-luma colour witness — the safe
      // direction (keep pixels IN the witness unless we are sure they are glyphs).
      const isGlyph = !!(e.textStyle)
      nodeMeta.push({ stableId: e.stableId || null, role: e.role || null, name: e.name || null, isText, isGlyph, x: x0, y: y0, w: x1 - x0, h: y1 - y0 })
    }
    if (offFrame.length) offFrameElements = offFrame
    if (!nodeMeta.length) { nodeMap = null; nodeMeta = null }
    else {
      nodeSsim = new Float64Array(nodeMeta.length); nodeN = new Float64Array(nodeMeta.length)
      nodeDe = COLOR_AXIS ? new Float64Array(nodeMeta.length) : null
      nodeDeN = COLOR_AXIS ? new Float64Array(nodeMeta.length) : null
    }
  }

  // Sub-pixel shift search, then Gaussian-windowed SSIM at the winning offset.
  const best = bestOffset(lumaO, lumaR, W, H, mask, SHIFT_RADIUS)
  const lumaRsh = shiftLuma(lumaR, W, H, best.dx, best.dy)
  const { k, radius } = gaussKernel(GAUSSIAN_SIGMA)
  const OO = new Float32Array(n), RR = new Float32Array(n), OR = new Float32Array(n)
  for (let p = 0; p < n; p++) { OO[p] = lumaO[p] * lumaO[p]; RR[p] = lumaRsh[p] * lumaRsh[p]; OR[p] = lumaO[p] * lumaRsh[p] }
  const muO = sepBlur(lumaO, W, H, k, radius, blurTmp), muR = sepBlur(lumaRsh, W, H, k, radius, blurTmp)
  const muOO = sepBlur(OO, W, H, k, radius, blurTmp), muRR = sepBlur(RR, W, H, k, radius, blurTmp), muOR = sepBlur(OR, W, H, k, radius, blurTmp)

  const rowsH = H / REGION_ROWS, colsW = W / REGION_COLS
  const regionSum = new Float64Array(REGION_ROWS * REGION_COLS), regionW = new Float64Array(REGION_ROWS * REGION_COLS)
  const regionDe = COLOR_AXIS ? new Float64Array(REGION_ROWS * REGION_COLS) : null   // per-region ΔE00 sum
  const regionDeN = COLOR_AXIS ? new Float64Array(REGION_ROWS * REGION_COLS) : null  // per-region ΔE sample count (strided)
  // Memoise sRGB→Lab by packed 24-bit colour — UIs reuse few distinct colours, so
  // the transcendental conversion runs a handful of times, not per pixel. Unconditional:
  // the colour axis AND the extra-content probe's chroma arm below both consult it.
  const labCache = new Map()
  const labOf = (data, i) => {
    const key = (data[i] << 16) | (data[i + 1] << 8) | data[i + 2]
    let v = labCache.get(key)
    if (v === undefined) { v = rgbToLab(data[i], data[i + 1], data[i + 2]); labCache.set(key, v) }
    return v
  }
  let ssum = 0, wsum = 0
  // Text-aware ΔE: substituted-font glyphs land on different pixels, so oracle-text vs
  // render-background pairs inflate ΔE 29-51 on token-correct screens. GLYPH pixels (nodeMap +
  // isGlyph = a node the pull marked with a textStyle) aggregate into a SEPARATE whole-frame
  // textDeltaE report field; the region ΔE — and the worstRegionDeltaE that decides
  // COLOR_DRIFT_REVIEW — is NON-GLYPH content only. Deliberately uses the STRICT isGlyph, not
  // the broad role-substring isText: a role-only container never leaves the colour witness. A
  // A spec-less run has no nodeMap, so every pixel stays in the witness.
  let textDeSum = 0, textDeN = 0
  for (let y = 0; y < H; y++) {
    const ry = Math.min(REGION_ROWS - 1, (y / rowsH) | 0)
    let sy = y + best.dy; if (sy < 0) sy = 0; else if (sy >= H) sy = H - 1
    const deRow = COLOR_AXIS && (y % DELTAE_STRIDE === 0)
    for (let x = 0; x < W; x++) {
      const p = y * W + x
      if (!mask[p]) continue
      const mo = muO[p], mr = muR[p]
      const vO = muOO[p] - mo * mo, vR = muRR[p] - mr * mr, cov = muOR[p] - mo * mr
      const s = ((2 * mo * mr + C1) * (2 * cov + C2)) / ((mo * mo + mr * mr + C1) * (vO + vR + C2))
      const ridx = ry * REGION_COLS + Math.min(REGION_COLS - 1, (x / colsW) | 0)
      regionSum[ridx] += s; regionW[ridx] += 1
      ssum += s; wsum += 1
      const ni = nodeMap ? nodeMap[p] : -1
      if (ni >= 0) { nodeSsim[ni] += s; nodeN[ni] += 1 }
      if (deRow && x % DELTAE_STRIDE === 0) {
        // ΔE00 between the oracle pixel and the render pixel at the SSIM-winning offset.
        const i = p << 2
        let sx = x + best.dx; if (sx < 0) sx = 0; else if (sx >= W) sx = W - 1
        const j = (sy * W + sx) << 2
        const de = deltaE00(labOf(od, i), labOf(rd, j))
        if (ni >= 0) { nodeDe[ni] += de; nodeDeN[ni] += 1 }
        if (ni >= 0 && nodeMeta[ni].isGlyph) { textDeSum += de; textDeN += 1 }
        else { regionDe[ridx] += de; regionDeN[ridx] += 1 }
      }
    }
  }
  const score = wsum ? ssum / wsum : 0   // weighted mean over content; NOT clamped
  // C1-conservative: probe the oracle-BACKGROUND plane the mask deliberately excludes from
  // `score`. Fraction of unmasked pixels whose luma diverges > 24 (the BG_TOLERANCE scale)
  // from the shift-aligned render — render-ADDED content is exactly what only this sees.
  // Chroma arm (C1 equal-luma residual): a pixel whose luma MATCHES can still be a wrong
  // COLOUR (equal-luma saturated panel over the oracle background) — count it too when the
  // cheap RGB pre-filter trips AND ΔE00 at the same shift-aligned offset confirms it. The
  // pre-filter keeps the hot path cheap (unmasked space is mostly identical background), and
  // the pair-keyed ΔE memo makes even a large solid divergent region a handful of evals.
  // Strided ×2 like ΔE: a mean fraction, statistically equivalent, half the rows/cols.
  // W5-3: pixels within EXTRA_CONTENT_RING_PX of the mask edge are skipped (dilation ring —
  // halo/gradient spill hugging real content is not render-added content), and the divergence
  // is ALSO tallied per 8×4 grid region so one halo-dense card region stays distinguishable
  // from frame-wide added content in the report.
  const probeSkip = EXTRA_CONTENT_RING_PX > 0 ? dilateMask(mask, W, H, EXTRA_CONTENT_RING_PX) : mask
  const extraDeMemo = new Map()
  const extraRegionCnt = new Float64Array(REGION_ROWS * REGION_COLS)
  const extraRegionTot = new Float64Array(REGION_ROWS * REGION_COLS)
  let extraCnt = 0, extraTot = 0
  for (let y = 0; y < H; y += 2) {
    const rowBase = y * W
    const ry = Math.min(REGION_ROWS - 1, (y / rowsH) | 0)
    let sy = y + best.dy; if (sy < 0) sy = 0; else if (sy >= H) sy = H - 1
    for (let x = 0; x < W; x += 2) {
      const p = rowBase + x
      if (probeSkip[p] || excluded[p]) continue   // R1: the device-chrome band legitimately differs; W5-3: the dilation ring is not probe space
      const ridx = ry * REGION_COLS + Math.min(REGION_COLS - 1, (x / colsW) | 0)
      extraTot++; extraRegionTot[ridx]++
      if (Math.abs(lumaO[p] - lumaRsh[p]) > 24) { extraCnt++; extraRegionCnt[ridx]++; continue }
      const i = p << 2
      let sx = x + best.dx; if (sx < 0) sx = 0; else if (sx >= W) sx = W - 1
      const j = (sy * W + sx) << 2
      const dr = od[i] - rd[j], dg = od[i + 1] - rd[j + 1], db = od[i + 2] - rd[j + 2]
      if (dr * dr + dg * dg + db * db <= 576) continue   // 24² — below the BG_TOLERANCE scale is background noise
      const key = ((od[i] << 16) | (od[i + 1] << 8) | od[i + 2]) * 16777216 + ((rd[j] << 16) | (rd[j + 1] << 8) | rd[j + 2])
      let de = extraDeMemo.get(key)
      if (de === undefined) { de = deltaE00(labOf(od, i), labOf(rd, j)); extraDeMemo.set(key, de) }
      if (de > EXTRA_CONTENT_DELTAE) { extraCnt++; extraRegionCnt[ridx]++ }
    }
  }
  const extraContentFraction = extraTot ? extraCnt / extraTot : 0
  const extraContentRegions = []
  for (let r = 0; r < REGION_ROWS; r++) {
    for (let c = 0; c < REGION_COLS; c++) {
      const idx = r * REGION_COLS + c
      if (!extraRegionCnt[idx]) continue
      extraContentRegions.push({ row: r, col: c, fraction: extraRegionCnt[idx] / extraRegionTot[idx], divergentPx: extraRegionCnt[idx] })
    }
  }
  extraContentRegions.sort((a, b) => b.fraction - a.fraction)
  const regions = []
  let worst = 1, worstDe = 0
  for (let r = 0; r < REGION_ROWS; r++) {
    for (let c = 0; c < REGION_COLS; c++) {
      const idx = r * REGION_COLS + c
      if (!regionW[idx]) continue
      const rs = regionSum[idx] / regionW[idx]
      if (rs < worst) worst = rs
      const region = { row: r, col: c, x: Math.round(c * colsW), y: Math.round(r * rowsH), w: Math.round(colsW), h: Math.round(rowsH), score: rs, contentPx: regionW[idx] }
      if (COLOR_AXIS && regionDeN[idx] > 0) { const de = regionDe[idx] / regionDeN[idx]; region.deltaE = de; if (de > worstDe) worstDe = de }
      regions.push(region)
    }
  }
  let zones = null
  if (nodeMeta) {
    zones = []
    for (let m = 0; m < nodeMeta.length; m++) {
      if (!nodeN[m]) continue   // a declared node with no content pixels (off-screen / fully masked) — skip
      const z = nodeMeta[m], zssim = nodeSsim[m] / nodeN[m]
      const zone = { stableId: z.stableId, role: z.role, name: z.name, isText: z.isText, bboxPx: { x: z.x, y: z.y, w: z.w, h: z.h }, ssim: zssim, contentPx: nodeN[m], status: severity(zssim) }
      if (COLOR_AXIS && nodeDeN[m] > 0) zone.deltaE = nodeDe[m] / nodeDeN[m]
      zones.push(zone)
    }
    zones.sort((a, b) => a.ssim - b.ssim)   // worst element first
  }
  const out = {
    score, worstRegion: worst, regions, coverage, error: null, extraContentFraction,
    alignment: { dx: best.dx, dy: best.dy }, scaleFactor, aspectSkew, renderNative, chromeExcludedPx: excludedPx,
    artifactImages: { oracle, render }, dimensions: { width: W, height: H },
  }
  if (extraContentRegions.length) out.extraContentRegions = extraContentRegions.slice(0, 12)   // W5-3: worst halo/panel regions first, bounded
  if (zones) out.zones = zones
  if (offFrameElements) out.offFrameElements = offFrameElements
  if (COLOR_AXIS) {
    // Advisory colour axis: separates token/colour drift from structure. `score` is never
    // touched; REVIEW surfaces as a WARN issue (COLOR_DRIFT_REVIEW) in the caller — a WARN,
    // never a blocker, because ΔE thresholds are uncalibrated for gating.
    // W5-2: worstRegionDeltaE is NON-TEXT content only (text ΔE is font-substitution noise —
    // reported separately as textDeltaE, never a drift witness), and REVIEW fires only when
    // the STRUCTURAL band is PASS/MINOR — the stated design intent: the colour axis is the
    // lone witness exactly when structure matches; a structurally-flagged row keeps the ΔE
    // numbers as report fields without stacking a noise WARN on top.
    out.worstRegionDeltaE = worstDe
    if (textDeN) out.textDeltaE = textDeSum / textDeN
    const band = severity(score)
    out.colorStatus = (worstDe > DELTAE_PASS && (band === 'PASS' || band === 'MINOR')) ? 'REVIEW' : 'PASS'
  }
  return out
}

function realpathOrNull(p) {
  try { return realpathSync(p) } catch { return null }
}

function pathInsideRoot(p, root) {
  return p === root || p.startsWith(root + sep)
}

function capturePathContained(p, roots) {
  const abs = resolve(p)
  const rootList = (roots || []).map((root) => resolve(root))
  if (!rootList.some((root) => pathInsideRoot(abs, root))) return false
  if (!existsSync(abs)) return true
  let real = null
  try {
    lstatSync(abs)
    real = realpathOrNull(abs)
  } catch {
    return false
  }
  if (!real) return false
  return rootList.some((root) => {
    const rr = realpathOrNull(root) || root
    return pathInsideRoot(real, rr)
  })
}

function findRoborazziDirs() {
  const raw = process.env.ROBORAZZI_OUTPUT_DIRS
    ? process.env.ROBORAZZI_OUTPUT_DIRS.split(delimiter)
    : process.env.ROBORAZZI_OUTPUT_DIR
      ? [process.env.ROBORAZZI_OUTPUT_DIR]
      : []
  const dirs = [...new Set(raw.filter(Boolean).map((p) => resolve(p)))]
  const missing = dirs.filter((p) => !existsSync(p))
  if (missing.length) throw new Error(`declared Roborazzi output dir does not exist: ${missing.join(', ')}`)
  return dirs
}

function loadManifest() {
  const p = process.env.SCREENSHOT_CAPTURE_MANIFEST
  if (!p) return { path: null, entries: null, recording: null, error: null }
  try {
    const json = JSON.parse(readFileSync(p, 'utf8'))
    if (!json || typeof json !== 'object' || Array.isArray(json) || !Array.isArray(json.captures)) {
      throw new Error('manifest must be an object with a captures array')
    }
    const topKeys = Object.keys(json)
    const unknownTop = topKeys.filter((key) => !['captures', 'recording'].includes(key))
    if (unknownTop.length) throw new Error(`manifest has unknown top-level key(s): ${unknownTop.join(', ')}`)
    const allowedEntryKeys = new Set(['captureName', 'path', 'nodeId', 'variantId', 'primaryState', 'localeTag'])
    for (let i = 0; i < json.captures.length; i++) {
      const entry = json.captures[i]
      if (!entry || typeof entry !== 'object' || Array.isArray(entry)) throw new Error(`captures[${i}] must be an object`)
      const unknown = Object.keys(entry).filter((key) => !allowedEntryKeys.has(key))
      if (unknown.length) throw new Error(`captures[${i}] has unknown key(s): ${unknown.join(', ')}`)
      if (typeof entry.captureName !== 'string' || !entry.captureName || basename(entry.captureName) !== entry.captureName) throw new Error(`captures[${i}].captureName must be a basename`)
      if (typeof entry.path !== 'string' || !entry.path) throw new Error(`captures[${i}].path must be a non-empty path`)
      if (typeof entry.nodeId !== 'string' || !/^[0-9]+:[0-9]+$/.test(entry.nodeId)) throw new Error(`captures[${i}].nodeId must match <digits>:<digits>`)
      if (typeof entry.primaryState !== 'boolean') throw new Error(`captures[${i}].primaryState must be boolean`)
      if (entry.variantId !== undefined && (typeof entry.variantId !== 'string' || !entry.variantId)) throw new Error(`captures[${i}].variantId must be a non-empty string`)
      if (entry.localeTag !== undefined && (typeof entry.localeTag !== 'string' || !entry.localeTag.trim())) throw new Error(`captures[${i}].localeTag must be a non-empty string`)
    }
    const entries = json.captures
    const recording = json.recording && typeof json.recording === 'object' && !Array.isArray(json.recording) ? json.recording : null
    if (json.recording !== undefined && recording === null) throw new Error('manifest.recording must be an object')
    return { path: p, entries, recording, error: null }
  } catch (e) {
    return { path: p, entries: null, recording: null, error: e.message }
  }
}

// Bind oracle→capture only by the stable Figma node-id from the current manifest contract.
// File names are diagnostics, never an identity source.
function manifestEntriesByNodeId(manifest, nodeId) {
  if (!manifest.entries || !nodeId) return []
  const want = String(nodeId)
  return manifest.entries.filter((entry) => entry.nodeId === want)
}

function manifestEntryPathUnchecked(manifest, entry) {
  if (!entry || typeof entry !== 'object') return null
  const manifestDir = manifest.path ? dirname(resolve(manifest.path)) : process.cwd()
  return resolve(manifestDir, entry.path)
}

// Containment: a manifest may only bind captures INSIDE the declared Roborazzi output dirs.
// Without this, a manifest entry can point the gate at ANY file on disk — including the
// pulled oracle itself — and score oracle-vs-oracle to a guaranteed PASS. A path outside
// the roots resolves to null here; the caller flags it (CAPTURE_PATH_UNCONTAINED).
function manifestEntryPath(manifest, entry, roots) {
  const p = manifestEntryPathUnchecked(manifest, entry)
  if (!p) return null
  return capturePathContained(p, roots) ? p : null
}

function stateEvidence(entry) {
  return entry && entry.primaryState === true
    ? { ok: true, note: null }
    : { ok: false, note: 'manifest marks capture as non-primary state' }
}

function declaredOracleNames(node) {
  return Array.isArray(node && node.variants) ? node.variants.map((variant) => variant.imageFile) : []
}

function captureNameForVariant(screen, imageFile) {
  const suffix = imageFile.slice(screen.length, -4)
  return imageFile.startsWith(screen) && imageFile.endsWith('.png') && (suffix === '' || suffix.startsWith('.'))
    ? `${screen}Screenshot${suffix}.png`
    : null
}

function variantsFor(screen, node, screensDir) {
  return node.variants.map((variant) => {
    const oracle = join(screensDir, variant.imageFile)
    return {
      id: variant.id,
      theme: variant.id,
      oracle,
      capture: captureNameForVariant(screen, variant.imageFile),
      nodeId: variant.nodeId,
      kind: node.kind,
      missingOracle: !existsSync(oracle),
    }
  })
}

function indexSemanticProblems(index) {
  const problems = []
  for (const [screen, node] of Object.entries((index && index.nodes) || {})) {
    if (!node || !Array.isArray(node.variants)) continue
    const ids = node.variants.map((variant) => variant.id)
    const files = node.variants.map((variant) => variant.imageFile)
    if (ids.length !== new Set(ids).size) problems.push({ kind: 'INDEX_VARIANT_ID_DUPLICATE', message: `${screen} has duplicate variant ids`, screen })
    if (files.length !== new Set(files).size) problems.push({ kind: 'INDEX_VARIANT_FILE_DUPLICATE', message: `${screen} has duplicate variant image files`, screen })
    for (const variant of node.variants) {
      if (!variant.imageFile.startsWith(screen) || !variant.imageFile.endsWith('.png')) problems.push({ kind: 'INDEX_VARIANT_FILE_INVALID', message: `${screen}/${variant.id} imageFile must be a ${screen}-prefixed PNG basename`, screen, theme: variant.id })
    }
    const matches = (url, nodeId, fetchedAt) => node.variants.some((variant) => variant.url === url && variant.nodeId === nodeId && variant.fetchedAt === fetchedAt)
    if (node.url !== undefined && !matches(node.url, node.nodeId, node.fetchedAt)) problems.push({ kind: 'INDEX_PRIMARY_SUMMARY_MISMATCH', message: `${screen} primary summary does not match any current variant`, screen })
    if (node.darkUrl !== undefined && !matches(node.darkUrl, node.darkNodeId, node.darkFetchedAt)) problems.push({ kind: 'INDEX_DARK_SUMMARY_MISMATCH', message: `${screen} dark summary does not match any current variant`, screen })
  }
  return problems
}

// H4-P1: a declared `overlay` (a popup/sheet drawn over a dimmed host) has no representable
// isolated full-bleed capture in Phase 1, so an absent capture for it is an explicit
// fail-closed UNREPRESENTABLE_OVERLAY block — distinct from a generic MISSING_CAPTURE (which
// a dialog/component CAN still satisfy with an isolated render). Reuses the same
// capture-absent trigger, only the label/severity reason differs.
function missingCaptureStatus(v) {
  return v && v.kind === 'overlay' ? 'UNREPRESENTABLE_OVERLAY' : 'MISSING_CAPTURE'
}

// W5-5 — the font families Robolectric's bundled set can honestly render (Roboto family +
// the generic aliases + Noto fallbacks). Prefix-matched case-insensitively so variants
// ("Roboto Condensed", "Noto Sans Symbols") stay bundled; anything else substitutes.
const ROBOLECTRIC_BUNDLED_FONT_RE = /^(roboto|noto|droid|sans-serif|serif|monospace|cursive|casual)\b/i

function oraclePngFiles(screensDir) {
  try { return readdirSync(screensDir).filter((n) => /\.png$/i.test(n)).sort() } catch { return [] }
}

function captureStartedAtMs() {
  const raw = process.env.SCREENSHOT_CAPTURE_STARTED_AT
  if (!raw) return null
  // A non-positive epoch (raw '0' or ISO '1970-01-01T00:00:00Z' -> 0) is truthy enough to
  // satisfy the gate's "fresh-capture evidence present" precondition yet makes staleByStartedAt
  // a no-op (its `if (!minMs) return false`), silently disabling the startedAt staleness bound.
  // Treat it as a config error (exit 1) rather than a silent disable.
  let value = null
  if (/^\d+$/.test(String(raw))) {
    const n = Number(raw)
    if (n > 0) value = n
    else CONFIG_ERRORS.push(`SCREENSHOT_CAPTURE_STARTED_AT must be a positive epoch-ms timestamp, got ${JSON.stringify(raw)}`)
  } else {
    const parsed = Date.parse(raw)
    if (Number.isFinite(parsed) && parsed > 0) value = parsed
    else CONFIG_ERRORS.push(`SCREENSHOT_CAPTURE_STARTED_AT must be epoch milliseconds or ISO timestamp, got ${JSON.stringify(raw)}`)
  }
  if (value == null) return null
  const now = Date.now()
  if (value < now - 24 * 60 * 60 * 1000) {
    CONFIG_ERRORS.push(`SCREENSHOT_CAPTURE_STARTED_AT is older than 24h (${JSON.stringify(raw)}) — it cannot attest a fresh certifying record run; record again or pass the actual recent run start`)
    return null
  }
  if (value > now + 5 * 60 * 1000) {
    CONFIG_ERRORS.push(`SCREENSHOT_CAPTURE_STARTED_AT is more than 5m in the future (${JSON.stringify(raw)})`)
    return null
  }
  return value
}

function staleByStartedAt(path, minMs) {
  if (!minMs) return false
  try { return statSync(path).mtimeMs + 1 < minMs } catch { return false }
}

function incompleteStatus(status) {
  return ['INCOMPLETE', 'MISSING_CAPTURE', 'UNREPRESENTABLE_OVERLAY', 'MISSING_ORACLE', 'ASPECT_MISMATCH', 'LOW_CONTENT_ORACLE', 'STALE_CAPTURE', 'DUPLICATE_CAPTURE', 'CAPTURE_LOCALE_MISMATCH', 'NO_INDEXED_SCREENS'].includes(status)
}

function overallFor(results, mode, issues = []) {
  const statuses = results.map((r) => r.status)
  if (statuses.includes('BLOCKER') || statuses.includes('DUPLICATE_CAPTURE')) return 'BLOCKER'
  if (issues.some((i) => String(i.severity || '').toUpperCase() === 'BLOCKER')) return 'BLOCKER'
  if (mode === 'gate' && statuses.some(incompleteStatus)) return 'INCOMPLETE'
  if (statuses.includes('REVIEW_REQUIRED')) return 'REVIEW_REQUIRED'
  if (statuses.some(incompleteStatus)) return 'INCOMPLETE'
  if (statuses.includes('MAJOR') || statuses.includes('MINOR') || issues.some((i) => /^WARN/i.test(String(i.severity || '')))) return 'WARN'
  if (statuses.some((s) => s === 'PASS' || s === 'MINOR')) return 'PASS'
  return statuses.length ? 'SKIPPED' : 'PASS'
}

async function main() {
  let cli
  try {
    cli = parseCli({
      allowedFlags: ['--stem', '--gate', '--advisory', '--semantic', '--fixture-component-inventory', '--fixture-component-mappings'],
      valueFlags: ['--stem', '--fixture-component-inventory', '--fixture-component-mappings'],
      booleanFlags: ['--gate', '--advisory', '--semantic'],
      usage: USAGE,
    })
  } catch (e) {
    console.error(e.message)
    process.exit(1)
  }
  if (cli.has('--gate') && cli.has('--advisory')) {
    console.error('ERROR: choose only one of --gate or --advisory')
    process.exit(1)
  }
  let stem
  try { stem = assertTaskStem(cli.value('--stem') || cli.positional[0] || '') } catch {
    console.error(USAGE)
    process.exit(1)
  }
  const mode = cli.has('--gate') ? 'gate' : 'advisory'
  const semanticEnabled = cli.has('--semantic') || boolEnv('SCREENSHOT_SEMANTIC_DIFF', false)
  const runId = pipelineRunId(stem)
  if (!process.env.FIGMA_PIPELINE_RUN_ID) process.env.FIGMA_PIPELINE_RUN_ID = runId
  // R2-3 tighten-only task override: a `- gate: strict` bullet in the task's ## Design forces
  // strict pixel-verdict routing for THIS task's runs regardless of env/config. Max()-only by
  // construction — design-parser gives the weakening direction no grammar (any other `gate:`
  // value is a malformed design blocked at the cache gate), so this lookup can never soften.
  // Task file resolution mirrors check-screen-cache's (env override first, then task columns).
  const taskGate = (() => {
    const explicit = FIGMA_TASK_SOURCE_EXPLICIT
    const candidates = FIGMA_TASK_SOURCE_FILE
      ? [FIGMA_TASK_SOURCE_FILE]
      : [
          join(REPO_ROOT, 'orchestrator', 'tasks', 'todo', `${stem}.md`),
          join(REPO_ROOT, 'orchestrator', 'tasks', 'backlog', `${stem}.md`),
          join(REPO_ROOT, 'orchestrator', 'tasks', 'pending', `${stem}.questions.md`),
        ]
    let found = false
    let unavailable = false
    for (const p of candidates) {
      if (p !== '-' && !existsSync(p)) {
        if (explicit) unavailable = true
        continue
      }
      found = true
      try {
        if (parseDesignSources([readTaskMarkdown(p, { explicit })]).gateOverride === 'strict') return 'strict'
      } catch {
        unavailable = true
      }
    }
    if (unavailable || (mode === 'gate' && !found)) {
      CONFIG_ERRORS.push('current task Design source is missing, unreadable, unsafe, oversized, or not valid UTF-8')
    }
    return null
  })()
  const pixelGate = taskGate === 'strict' ? 'strict' : PIXEL_GATE
  if (taskGate === 'strict' && PIXEL_GATE !== 'strict') {
    console.log(`pixelGate: strict — the task's \`- gate: strict\` bullet overrides ${PIXEL_GATE} (tighten-only)`)
  }
  const artifactsRoot = artifactRoot()
  // Compute startedAtMs HERE (alongside the other config inputs) so a non-positive epoch it
  // rejects into CONFIG_ERRORS is caught by the gate below — captureStartedAtMs() reads only
  // env, so the earlier call is side-effect-free, and this is the sole CONFIG_ERRORS inspection.
  const startedAtMs = captureStartedAtMs()
  if (CONFIG_ERRORS.length) {
    for (const e of CONFIG_ERRORS) console.error(`ERROR: ${e}`)
    process.exit(1)
  }

  const screensRoot = figmaScreensRoot()
  const screensDir = join(screensRoot, stem)
  const indexPath = join(screensDir, 'index.json')
  if (!existsSync(indexPath)) {
    console.error(`ERROR: index.json not found at ${indexPath}. Run figma:screens first.`)
    process.exit(1)
  }

  let index
  try { index = JSON.parse(readFileSync(indexPath, 'utf8')) }
  catch (e) { console.error(`ERROR: could not read ${indexPath}: ${e.message}`); process.exit(1) }

  const issues = []
  let validateIndex, validateSpec
  try {
    validateIndex = await compileSchema(figmaPath('token-schemas', 'screen-index.schema.json'), { gate: true })
    validateSpec = await compileSchema(figmaPath('token-schemas', 'spec.schema.json'), { gate: true })
  } catch (e) {
    console.error(`ERROR: current screenshot dependency schema validation unavailable: ${e.message}`)
    process.exit(1)
  }
  const indexValidation = schemaIssues(validateIndex, index, 'index:')
  for (const problem of indexValidation) {
    issues.push(issue('BLOCKER', problem.issueKind, problem.message, { file: indexPath, path: problem.path }))
  }
  const indexSemantics = indexSemanticProblems(index)
  for (const problem of indexSemantics) {
    issues.push(issue('BLOCKER', problem.kind, problem.message, { file: indexPath, screen: problem.screen, theme: problem.theme }))
  }
  if (index.taskStem !== stem) {
    issues.push(issue('BLOCKER', 'INDEX_STEM_MISMATCH', `index taskStem ${JSON.stringify(index.taskStem)} does not match ${JSON.stringify(stem)}`, { file: indexPath }))
  }
  const indexUsable = indexValidation.length === 0 && indexSemantics.length === 0 && index.taskStem === stem
  const nodes = indexUsable ? index.nodes : {}
  const screenNames = Object.keys(nodes)
  const invalidScreenNames = screenNames.filter((name) => !SCREEN_KEY_RE.test(name))
  const safeScreenNames = screenNames.filter((name) => SCREEN_KEY_RE.test(name))
  const inputHashes = { [indexPath]: fileHash(indexPath) }
  // Render-class routing source = the CAS mapping registry's visualPolicy joined to owning
  // design node ids. A malformed registry/inventory is a BLOCKER (never silently strict).
  let liveRenderClasses = new Map()
  const mappingRegistryPath = cli.value('--fixture-component-mappings') || join(REPO_ROOT, 'orchestrator', 'figma', 'component-mappings.json')
  const renderClassOptions = {
    fixtureInventoryFile: cli.value('--fixture-component-inventory') || undefined,
    fixtureMappingsFile: cli.value('--fixture-component-mappings') || undefined
  }
  try { liveRenderClasses = renderClassByNodeId(renderClassOptions) }
  catch (error) {
    issues.push(issue('BLOCKER', 'RENDER_CLASS_SOURCE_INVALID', `render-class source is unreadable: ${String(error.message || error).slice(0, 300)}`, { file: mappingRegistryPath }))
  }
  if (existsSync(mappingRegistryPath)) inputHashes[mappingRegistryPath] = fileHash(mappingRegistryPath)
  const oracleFiles = oraclePngFiles(screensDir)
  const indexedOracles = new Set(safeScreenNames.flatMap((s) => declaredOracleNames(nodes[s])))
  for (const name of oracleFiles.filter((n) => !indexedOracles.has(n))) {
    const file = join(screensDir, name)
    inputHashes[file] = fileHash(file)
    // #22: an on-disk oracle PNG no longer derivable from index.nodes is a STALE remnant of a
    // previous pull (theme/bullet downgraded, screen removed) — nothing in the pipeline deletes
    // it (re-pull only writes; evidence-clean skips screens/), so a gate BLOCKER here blocked
    // the task FOREVER on perfect captures. WARN with the remedy instead. A missing oracle for
    // a DECLARED node stays a BLOCKER (MISSING_ORACLE below) — that path is untouched.
    issues.push(issue('WARN', 'STALE_ORACLE_FILE', `stale oracle PNG not referenced by any current index node/theme: ${name} — left over from a previous pull; delete the file (or re-pull via figma:screens) and re-run the comparison`, { file }))
  }
  let roboDirs
  try { roboDirs = findRoborazziDirs() }
  catch (e) { console.error(`ERROR: ${e.message}`); process.exit(1) }
  const manifest = loadManifest()
  // R5-3 — the design language, resolved LAZILY on the first manifest entry that records a
  // `localeTag` (the resource-root walk + spec re-read is deferred until needed).
  // Sources: the committed `designLocale` config key, else deterministic detection over ALL of
  // this stem's specs (lib/design-locale.mjs). Underivable → null → no cross-check here: the
  // fail-closed CAPTURE_LOCALE_UNDERIVABLE gate belongs to check-capture-config (Step 4.6b·0),
  // which runs BEFORE the render — this comparator check is the last-line witness, not the gate.
  // Anti-forgery, both flanks: (1) every input the decision read (spec files, the consulted
  // strings.xml, project-config for a declared locale) joins inputHashes so the final bundle's
  // re-hash net witnesses a post-compare edit that would flip the derived language; (2) the
  // fixture-only FIGMA_* locale env overrides are RECORDED in the report and hard-block the
  // final bundle (LOCALE_ENV_OVERRIDE — the THRESHOLDS_WEAKENED pattern): a hostile export at
  // compare time can redirect or disarm this witness, so it may never certify final evidence.
  const localeEnvOverrides = ['FIGMA_DESIGN_LOCALE', 'FIGMA_SUPPORTED_LOCALES', 'FIGMA_STRING_RESOURCE_ROOTS'].filter((k) => process.env[k])
  let designLocaleMemo
  const designLocaleFor = () => {
    if (designLocaleMemo !== undefined) return designLocaleMemo
    designLocaleMemo = null
    try {
      const specs = []
      for (const f of readdirSync(screensDir).filter((n) => n.endsWith('.spec.json'))) {
        const p = join(screensDir, f)
        try { specs.push(JSON.parse(readFileSync(p, 'utf8'))); inputHashes[p] = inputHashes[p] || fileHash(p) } catch { /* unreadable spec cannot vote */ }
      }
      const r = resolveDesignLocale({ specs, resourceRoots: deriveResourceRoots([REPO_ROOT]) })
      if (r.reason === 'source-unavailable') {
        issues.push(issue('BLOCKER', 'CAPTURE_LOCALE_SOURCE_UNAVAILABLE', 'locale configuration or string resources are unreadable, unsafe, or not valid UTF-8'))
        return designLocaleMemo
      }
      for (const f of r.resourceFiles || []) { try { inputHashes[f] = inputHashes[f] || fileHash(f) } catch { /* consulted file vanished — the re-hash net will catch it */ } }
      // Both explicit and detected outcomes depend on project-config: supportedLocales controls
      // the vote and adding/removing designLocale changes which branch wins.
      try {
        inputHashes[PROJECT_CONFIG_FILE] = inputHashes[PROJECT_CONFIG_FILE] ||
          PROJECT_CONFIG_HASH || fileHash(PROJECT_CONFIG_FILE)
      } catch { /* absent config cannot influence the decision */ }
      if (r.language) {
        designLocaleMemo = { language: r.language, source: r.source }
      }
    } catch {
      issues.push(issue('BLOCKER', 'CAPTURE_LOCALE_SOURCE_UNAVAILABLE', 'locale configuration or string resources are missing, unreadable, unsafe, or not valid UTF-8'))
      designLocaleMemo = null
    }
    return designLocaleMemo
  }
  if (manifest.error) issues.push(issue('BLOCKER', 'MANIFEST_INVALID', `capture manifest invalid: ${manifest.error}`, { file: manifest.path }))
  if (manifest.path) inputHashes[manifest.path] = fileHash(manifest.path)
  if (mode === 'gate' && !process.env.SCREENSHOT_CAPTURE_STARTED_AT) {
    issues.push(issue('BLOCKER', 'SCREENSHOT_CAPTURE_EVIDENCE_MISSING', 'gate mode requires SCREENSHOT_CAPTURE_STARTED_AT so stale Roborazzi outputs cannot pass as fresh evidence; SCREENSHOT_CAPTURE_MANIFEST proves membership/identity, not freshness'))
  }
  // Identity binding is mandatory in gate mode. The canonical driver emits the manifest;
  // without it there is deliberately no filename-based recovery path.
  if (mode === 'gate' && !manifest.path) {
    issues.push(issue('BLOCKER', 'MANIFEST_ABSENT', 'gate run requires SCREENSHOT_CAPTURE_MANIFEST. Use `run-figma-gates.mjs <stem> --stage screenshot`, which emits the nodeId manifest by construction'))
  }

  const results = []
  const artifactSets = []
  const artifactEntries = []
  let artifactSeq = 0
  // Oracle-only reference artifact for a plain MISSING_CAPTURE row — makes the Figma design
  // visible in the Done view even though the app produced no screenshot to compare. Registers
  // it with the run's artifact set/entries so it flows through the vetted serving path.
  // UNREPRESENTABLE_OVERLAY stays fail-closed (no reference). Returns the set or null; never throws.
  const emitOracleReference = async (v, screen, status) => {
    if (status !== 'MISSING_CAPTURE' || !v.oracle || !existsSync(v.oracle)) return null
    let oracleImage
    try { oracleImage = await Jimp.read(v.oracle) } catch { return null }
    const set = await writeReferenceArtifacts(Jimp, {
      root: artifactsRoot, stem, runId, sequence: ++artifactSeq,
      screen, theme: v.theme, status, metric: METRIC_NAME,
      oraclePath: v.oracle, roboPath: null, oracleImage, renderImage: null,
    })
    if (set) { artifactSets.push(set); artifactEntries.push(...Object.values(set.artifacts)) }
    return set
  }
  console.log(`\nScreenshot comparison (${METRIC_NAME}) — stem: ${stem} [${mode}${semanticEnabled ? ', semantic advisory' : ''}]`)
  console.log('-'.repeat(72))

  if (screenNames.length === 0) {
    console.log('No screens in index — nothing to compare.')
    results.push({ screen: '(index)', theme: null, themeKey: 'none', status: 'NO_INDEXED_SCREENS', reason: 'index.nodes is empty', file: indexPath })
    issues.push(issue(mode === 'gate' ? 'BLOCKER' : 'WARN', 'NO_INDEXED_SCREENS', 'index.json has no nodes; screenshot comparison would otherwise compare zero screens', { file: indexPath }))
  } else if (roboDirs.length === 0) {
    issues.push(issue(mode === 'gate' ? 'BLOCKER' : 'WARN', 'ROBORAZZI_OUTPUT_MISSING', 'no Roborazzi output dir found'))
  }

  for (const screen of invalidScreenNames) {
    const reason = `index screen key ${JSON.stringify(screen)} must match ${SCREEN_KEY_RE}; unsafe keys cannot be mapped to oracle/capture filenames`
    console.log(`  ${screen}: BLOCKER (${reason})`)
    results.push({ screen, theme: null, themeKey: 'none', status: 'BLOCKER', reason, file: indexPath })
    issues.push(issue('BLOCKER', 'INDEX_SCREEN_KEY_INVALID', reason, { screen, file: indexPath }))
  }

  const Jimp = safeScreenNames.length ? await loadJimp() : null
  for (const screen of safeScreenNames) {
    const variants = variantsFor(screen, nodes[screen], screensDir)
    for (const v of variants) {
      const tag = `${screen} [${v.theme}]`
      if (v.missingOracle) {
        const msg = `oracle missing: ${basename(v.oracle)}`
        console.log(`  ${tag}: MISSING_ORACLE (${msg})`)
        results.push({ screen, theme: v.theme, themeKey: v.theme, status: 'MISSING_ORACLE', reason: msg, oraclePath: v.oracle })
        issues.push(issue(mode === 'gate' ? 'BLOCKER' : 'WARN', 'MISSING_ORACLE', msg, { screen, theme: v.theme, file: v.oracle }))
        continue
      }
      inputHashes[v.oracle] = fileHash(v.oracle)

      // Stable node identity is the only binding path. When several declared variants share
      // one Figma node, variantId is mandatory to prevent one capture from certifying another.
      let nodeEntries = manifestEntriesByNodeId(manifest, v.nodeId)
      const twinVariant = variants.some((o) => o !== v && o.nodeId && v.nodeId && o.nodeId === v.nodeId)
      nodeEntries = nodeEntries.filter((entry) => entry.variantId === undefined || entry.variantId === v.id)
      if (twinVariant) nodeEntries = nodeEntries.filter((entry) => entry.variantId === v.id)
      const nodeDistinctPaths = [...new Set(nodeEntries.map((e) => manifestEntryPath(manifest, e, roboDirs)).filter(Boolean))]
      if (nodeEntries.length > 1) {
        const msg = `manifest binds node ${v.nodeId}, variant ${v.id} more than once — ambiguous identity`
        console.log(`  ${tag}: DUPLICATE_CAPTURE (${msg})`)
        results.push({ screen, theme: v.theme, themeKey: v.theme, status: 'DUPLICATE_CAPTURE', reason: msg, oraclePath: v.oracle, nodeId: v.nodeId, capturePaths: nodeDistinctPaths })
        issues.push(issue('BLOCKER', 'DUPLICATE_CAPTURE', msg, { screen, theme: v.theme, nodeId: v.nodeId, files: nodeDistinctPaths }))
        continue
      }
      const nodeEntry = nodeEntries[0] || null
      const nodePath = nodeEntry ? manifestEntryPath(manifest, nodeEntry, roboDirs) : null
      if (!nodeEntry) {
        const status = missingCaptureStatus(v)
        const msg = status === 'UNREPRESENTABLE_OVERLAY'
          ? `declared [overlay] node ${v.nodeId}, variant ${v.id} has no capture identity entry in the manifest — produce one per the overlay recipe (gate spec §3.5)`
          : `manifest has no capture identity entry for node ${v.nodeId}, variant ${v.id}`
        console.log(`  ${tag}: ${status} (${msg})`)
        const refSet = await emitOracleReference(v, screen, status)
        const row = { screen, theme: v.theme, themeKey: v.theme, status, reason: msg, oraclePath: v.oracle, nodeId: v.nodeId, kind: v.kind }
        if (refSet) row.artifactSet = refSet
        results.push(row)
        issues.push(issue(mode === 'gate' ? 'BLOCKER' : 'WARN', status, msg, { screen, theme: v.theme, nodeId: v.nodeId, variantId: v.id, artifactSet: refSet }))
        continue
      }
      if (!nodePath) {
        // The manifest names a capture file OUTSIDE every declared Roborazzi output dir —
        // an uncontained binding can substitute an arbitrary image for the app's render.
        const rejected = manifestEntryPathUnchecked(manifest, nodeEntry)
        const msg = `manifest binds node ${v.nodeId} to '${rejected}' which lies outside every Roborazzi output dir — uncontained capture path`
        console.log(`  ${tag}: CAPTURE_PATH_UNCONTAINED (${msg})`)
        results.push({ screen, theme: v.theme, themeKey: v.theme, status: 'CAPTURE_PATH_UNCONTAINED', reason: msg, oraclePath: v.oracle, nodeId: v.nodeId })
        issues.push(issue('BLOCKER', 'CAPTURE_PATH_UNCONTAINED', msg, { screen, theme: v.theme, nodeId: v.nodeId, file: rejected }))
        continue
      }

      if (!existsSync(nodePath)) {
        const status = missingCaptureStatus(v)
        const msg = status === 'UNREPRESENTABLE_OVERLAY'
          ? `declared [overlay] node ${v.nodeId} has no representable capture ('${basename(nodePath)}' absent) — produce one per the overlay recipe (gate spec §3.5: composed-over-host, or popup-only for an isolated popup frame)`
          : `manifest binds node ${v.nodeId} to '${basename(nodePath)}' but that capture file is absent`
        console.log(`  ${tag}: ${status} (${msg})`)
        const refSet = await emitOracleReference(v, screen, status)
        const row = { screen, theme: v.theme, themeKey: v.theme, status, reason: msg, oraclePath: v.oracle, nodeId: v.nodeId, kind: v.kind }
        if (refSet) row.artifactSet = refSet
        results.push(row)
        issues.push(issue(mode === 'gate' ? 'BLOCKER' : 'WARN', status, msg, { screen, theme: v.theme, nodeId: v.nodeId, captureName: nodeEntry.captureName, artifactSet: refSet }))
        continue
      }
      const roboPath = nodePath
      const entry = nodeEntry
      inputHashes[roboPath] = fileHash(roboPath)
      // Tamper check: a capture that is BYTE-IDENTICAL to its oracle is a copy, not a render
      // (two independent renderers never produce byte-identical PNGs) — the oracle-as-capture
      // forgery scores a guaranteed 1.0. Cheap: both hashes are already computed.
      if (inputHashes[roboPath] && inputHashes[roboPath] === inputHashes[v.oracle]) {
        const msg = `capture '${basename(roboPath)}' is byte-identical to the oracle — a copied oracle cannot certify the app's render`
        console.log(`  ${tag}: CAPTURE_IS_ORACLE_COPY (${msg})`)
        results.push({ screen, theme: v.theme, themeKey: v.theme, status: 'CAPTURE_IS_ORACLE_COPY', reason: msg, oraclePath: v.oracle, roboPath })
        issues.push(issue('BLOCKER', 'CAPTURE_IS_ORACLE_COPY', msg, { screen, theme: v.theme, file: roboPath }))
        continue
      }
      if (staleByStartedAt(roboPath, startedAtMs)) {
        const msg = `capture '${entry.captureName}' predates current Roborazzi run`
        console.log(`  ${tag}: STALE_CAPTURE (${msg})`)
        results.push({ screen, theme: v.theme, themeKey: v.theme, status: 'STALE_CAPTURE', reason: msg, oraclePath: v.oracle, roboPath })
        issues.push(issue('BLOCKER', 'STALE_CAPTURE', msg, { screen, theme: v.theme, file: roboPath }))
        continue
      }
      // R5-3 — capture↔design language cross-check. When the manifest entry records the locale
      // the harness ACTUALLY rendered with (`localeTag`) and the design language is derivable,
      // a language mismatch bails the row with a NAMED cause BEFORE the meaningless low-SSIM
      // comparison (a wholesale text-language swap zeroes every text zone). Completeness-class:
      // always a BLOCKER (like STALE_CAPTURE — advisory mode and screenshotPixelGate never
      // route it). Without localeTag, or when design language is underivable, this final
      // witness has no comparison; the qualifier gate remains authoritative.
      const localeTag = entry && typeof entry === 'object' && entry.localeTag != null ? String(entry.localeTag).trim() : ''
      if (localeTag) {
        const dl = designLocaleFor()
        if (dl && languageOf(localeTag) !== dl.language) {
          const msg = `capture rendered locale '${localeTag}' but the design language is '${dl.language}' (${dl.source === 'config' ? 'declared via designLocale' : 'derived from spec texts × string resources'}) — a text-language swap makes every text zone diverge; fix the @Config locale segment (run check-capture-config --fix, now locale-aware) and re-record; never re-pull a "convenient" oracle`
          console.log(`  ${tag}: CAPTURE_LOCALE_MISMATCH (${msg})`)
          results.push({ screen, theme: v.theme, themeKey: v.theme, status: 'CAPTURE_LOCALE_MISMATCH', reason: msg, oraclePath: v.oracle, roboPath, localeTag, designLocale: dl.language })
          issues.push(issue('BLOCKER', 'CAPTURE_LOCALE_MISMATCH', msg, { screen, theme: v.theme, file: roboPath }))
          continue
        }
      }
      const state = stateEvidence(entry)
      if (!state.ok) {
        const msg = `primary-state evidence missing: ${state.note}`
        console.log(`  ${tag}: REVIEW_REQUIRED (${msg})`)
        results.push({ screen, theme: v.theme, themeKey: v.theme, status: 'REVIEW_REQUIRED', reason: msg, oraclePath: v.oracle, roboPath })
        issues.push(issue(mode === 'gate' ? 'BLOCKER' : 'WARN', 'PRIMARY_STATE_UNCONFIRMED', msg, { screen, theme: v.theme, file: roboPath }))
        continue
      }

      // Per-Figma-node zones (luma-v2): feed the resolved screen spec when present.
      // Reading it makes the score depend on it, so it joins inputHashes (the final
      // bundle re-hashes inputs and fails closed). Absent spec → grid regions only.
      let spec = null
      const specPath = v.oracle.replace(/\.png$/, '.spec.json')
      if (existsSync(specPath)) {
        inputHashes[specPath] = fileHash(specPath)
        try { spec = JSON.parse(readFileSync(specPath, 'utf8')) }
        catch (error) {
          const msg = `screen spec is unreadable: ${error.message}`
          results.push({ screen, theme: v.theme, themeKey: v.theme, status: 'BLOCKER', reason: msg, oraclePath: v.oracle })
          issues.push(issue('BLOCKER', 'SPEC_UNREADABLE', msg, { screen, theme: v.theme, file: specPath }))
          continue
        }
        const specProblems = schemaIssues(validateSpec, spec, 'spec:')
        if (specProblems.length) {
          const msg = `screen spec violates the current schema (${specProblems[0].path}: ${specProblems[0].message})`
          results.push({ screen, theme: v.theme, themeKey: v.theme, status: 'BLOCKER', reason: msg, oraclePath: v.oracle })
          for (const problem of specProblems) issues.push(issue('BLOCKER', problem.issueKind, problem.message, { screen, theme: v.theme, file: specPath, path: problem.path }))
          continue
        }
      }
      // W5-5 — substituted fonts (advisory, INFORMATIONAL — a verdict-neutral report FIELD, not
      // an issue). Robolectric renders bundled fonts only (capture doctrine), so a design family
      // outside the bundled set is GUARANTEED to substitute — its glyph-pixel divergence is real
      // but expected, and text fidelity is owned by spec-compare + the lenient text zone floor +
      // the W5-2 text-ΔE quarantine, never by glyph pixels. Recorded as `row.substitutedFonts[]`
      // (like textDeltaE) so the site can surface it WITHOUT flipping overall PASS→WARN and
      // forcing `completed-with-caveats` on every UI task of any non-Roboto/Noto design — the
      // font is the owner's choice, not a fixable defect. Populated only when the pull supplied
      // `textStyle.fontFamily` (optional contract field); a spec without it records nothing.
      let substitutedFonts = null
      if (spec && Array.isArray(spec.elements)) {
        const foreign = [...new Set(spec.elements
          .map((e) => e && e.textStyle && typeof e.textStyle.fontFamily === 'string' ? e.textStyle.fontFamily.trim() : '')
          .filter((fam) => fam && !ROBOLECTRIC_BUNDLED_FONT_RE.test(fam)))]
        if (foreign.length) substitutedFonts = foreign
      }
      const res = await compareImages(Jimp, v.oracle, roboPath, spec, v.kind)
      let artifactSet = null
      if (res.error) {
        const status = res.status || 'REVIEW_REQUIRED'
        if (res.artifactImages) {
          // referenceOnly (ASPECT_MISMATCH: unequal sizes) → the no-diff writer; otherwise
          // (LOW_CONTENT_ORACLE: same size) the full writer as before.
          const writeArtifacts = res.referenceOnly ? writeReferenceArtifacts : writeCompareArtifacts
          artifactSet = await writeArtifacts(Jimp, {
            root: artifactsRoot,
            stem,
            runId,
            sequence: ++artifactSeq,
            screen,
            theme: v.theme,
            status,
            score: res.score,
            coverage: res.coverage,
            metric: METRIC_NAME,
            oraclePath: v.oracle,
            roboPath,
            oracleImage: res.artifactImages.oracle,
            renderImage: res.artifactImages.render,
            alignment: res.alignment,
          })
          if (artifactSet) {
            artifactSets.push(artifactSet)
            artifactEntries.push(...Object.values(artifactSet.artifacts))
          }
        }
        console.log(`  ${tag}: ${status} (${res.error})`)
        const row = { screen, theme: v.theme, themeKey: v.theme, status, score: null, error: res.error, coverage: res.coverage, oraclePath: v.oracle, roboPath }
        if (v.nodeId) row.nodeId = v.nodeId
        if (artifactSet) row.artifactSet = artifactSet
        results.push(row)
        issues.push(issue(mode === 'gate' && (incompleteStatus(status) || status === 'REVIEW_REQUIRED') ? 'BLOCKER' : 'WARN', status, res.error, { screen, theme: v.theme, file: roboPath, artifactSet }))
        continue
      }

      // #20: a color-only row (uniform-fill oracle) carries its own PASS/BLOCKER verdict —
      // there is no SSIM score to band.
      const colorOnly = res.mode === 'color-only'
      let status = colorOnly ? res.status : severity(res.score)
      // H3 — worst-zone floor (opt-in). A high global mean can hide one destroyed node; if a
      // single content-bearing zone is below Z_BLOCKER, fail the screen. Set BEFORE the
      // artifact write + row + issues so it propagates through overallFor unchanged.
      const zoneHit = (ZONE_GATE && !colorOnly && status !== 'BLOCKER') ? worstZoneViolation(res) : null
      if (zoneHit) status = 'BLOCKER'
      artifactSet = await writeCompareArtifacts(Jimp, {
        root: artifactsRoot,
        stem,
        runId,
        sequence: ++artifactSeq,
        screen,
        theme: v.theme,
        status,
        score: res.score,
        coverage: res.coverage,
        metric: METRIC_NAME,
        oraclePath: v.oracle,
        roboPath,
        oracleImage: res.artifactImages.oracle,
        renderImage: res.artifactImages.render,
        alignment: res.alignment,
      })
      if (artifactSet) {
        artifactSets.push(artifactSet)
        artifactEntries.push(...Object.values(artifactSet.artifacts))
      }
      const detail = colorOnly
        ? `color-only mean ΔE00=${res.meanDeltaE.toFixed(2)}, content=${(res.coverage * 100).toFixed(1)}%`
        : `ssim=${res.score.toFixed(3)}, content=${(res.coverage * 100).toFixed(0)}%`
      console.log(`  ${tag}: ${status} (${detail})`)
      const row = { screen, theme: v.theme, themeKey: v.theme, status, score: res.score, coverage: res.coverage, metric: METRIC_NAME, oraclePath: v.oracle, roboPath }
      if (colorOnly) { row.mode = 'color-only'; row.meanDeltaE = res.meanDeltaE; row.colorDivergentFraction = res.colorDivergentFraction }
      if (v.nodeId) row.nodeId = v.nodeId
      // Optional semantic detail fields are omitted when the metric did not derive them.
      if (Array.isArray(res.regions)) row.regions = res.regions
      if (typeof res.worstRegion === 'number') row.worstRegion = res.worstRegion
      if (res.alignment) row.alignment = res.alignment
      if (typeof res.scaleFactor === 'number') row.scaleFactor = res.scaleFactor
      if (typeof res.aspectSkew === 'number') row.aspectSkew = res.aspectSkew   // #18 anisotropic-rescale skew (>=1; only when a rescale happened)
      if (typeof res.extraContentFraction === 'number') row.extraContentFraction = res.extraContentFraction   // C1 unmasked-divergence probe
      if (Array.isArray(res.extraContentRegions)) row.extraContentRegions = res.extraContentRegions   // W5-3 per-region breakdown (halo-dense vs frame-wide)
      if (typeof res.textDeltaE === 'number') row.textDeltaE = res.textDeltaE   // W5-2 text ΔE — font-substitution noise, reported separately, never a drift witness
      if (substitutedFonts) row.substitutedFonts = substitutedFonts   // W5-5 informational field — verdict-neutral (never flips overall)
      if (typeof res.chromeExcludedPx === 'number' && res.chromeExcludedPx > 0) row.chromeExcludedPx = res.chromeExcludedPx   // R1 device-chrome / kind-crop pixels excluded from the compare
      if (typeof res.worstRegionDeltaE === 'number') row.worstRegionDeltaE = res.worstRegionDeltaE   // colour axis (advisory)
      if (res.colorStatus) row.colorStatus = res.colorStatus
      if (Array.isArray(res.zones)) {
        // Per-Figma-node scores (advisory). Report size must stay bounded: keep the 24
        // worst zones (the array arrives sorted ssim-asc); zonesTruncated = dropped count.
        row.zones = res.zones.slice(0, 24)
        if (res.zones.length > 24) row.zonesTruncated = res.zones.length - 24
      }
      if (artifactSet) row.artifactSet = artifactSet
      // The applied floor is per-zone by kind: a text zone was judged against the lenient
      // Z_TEXT_BLOCKER, a non-text zone / grid cell against the strict Z_BLOCKER.
      const zoneFloor = zoneHit && zoneHit.isText ? Z_TEXT_BLOCKER : Z_BLOCKER
      if (zoneHit) row.zoneFloorHit = { ssim: zoneHit.ssim, contentPx: zoneHit.contentPx, label: zoneHit.label, isText: !!zoneHit.isText, threshold: zoneFloor }
      results.push(row)
      if (zoneHit) issues.push(issue('BLOCKER', 'ZONE_SSIM_BLOCKER', `localized ${zoneHit.isText ? 'text zone' : 'zone'} '${zoneHit.label}' SSIM ${zoneHit.ssim.toFixed(3)} below ${zoneHit.isText ? 'text ' : ''}zone floor ${zoneFloor} (global mean ${res.score.toFixed(3)} would otherwise pass)`, { screen, theme: v.theme, file: roboPath, artifactSet }))
      else if (colorOnly && status === 'BLOCKER') {
        // A sparse-but-severe local divergence trips the divergent-pixel-fraction guard even
        // when the whole-frame MEAN ΔE00 stayed under DELTAE_PASS (dilution). Distinct reason
        // so the fix path knows this is a localized wrong-region hit, not a global colour drift.
        if (res.colorDivergent) issues.push(issue('BLOCKER', 'COLOR_ONLY_DIVERGENT_REGION', `uniform-fill oracle (content ${(res.coverage * 100).toFixed(1)}%): ${(res.colorDivergentFraction * 100).toFixed(1)}% of sampled pixels diverge ΔE00>10 (floor 2.0%) — sparse local divergence the mean ΔE00 ${res.meanDeltaE.toFixed(2)} dilutes`, { screen, theme: v.theme, file: roboPath, artifactSet }))
        else issues.push(issue('BLOCKER', 'COLOR_ONLY_MISMATCH', `uniform-fill oracle (content ${(res.coverage * 100).toFixed(1)}%): whole-frame mean ΔE00 ${res.meanDeltaE.toFixed(2)} exceeds ${DELTAE_PASS}`, { screen, theme: v.theme, file: roboPath, artifactSet }))
      }
      else if (status === 'BLOCKER') issues.push(issue('BLOCKER', 'SSIM_BLOCKER', `masked SSIM ${res.score.toFixed(3)} below ${T_MAJOR}`, { screen, theme: v.theme, file: roboPath, artifactSet }))
      else if (status === 'MAJOR') {
        if (mode === 'gate' && MAJOR_BAND === 'block') issues.push(issue('BLOCKER', 'SSIM_MAJOR', `masked SSIM ${res.score.toFixed(3)} below ${T_MINOR} (MAJOR band blocks; SCREENSHOT_MAJOR_BAND=advisory to downgrade)`, { screen, theme: v.theme, file: roboPath, artifactSet }))
        else issues.push(issue('WARN', 'SSIM_MAJOR_UNCALIBRATED', `masked SSIM ${res.score.toFixed(3)} below ${T_MINOR}; reviewer calibration required`, { screen, theme: v.theme, file: roboPath, artifactSet }))
      }
      else if (status === 'MINOR') issues.push(issue('WARN', 'SSIM_MINOR', `masked SSIM ${res.score.toFixed(3)} below ${T_PASS}`, { screen, theme: v.theme, file: roboPath, artifactSet }))
      // Independent of the SSIM chain (NOT else-if): equal-luma chroma drift keeps status
      // PASS — exactly the case where the colour axis is the only witness. WARN, not a
      // blocker: ΔE gating stays uncalibrated, but the drift must reach overall/caveats.
      // W5-2: colorStatus is already band-gated in the metric (PASS/MINOR structure only,
      // non-text ΔE only); the status check here additionally covers a zone-floor-blocked
      // row — a structurally-flagged screen never stacks the colour-noise WARN on top.
      if (res.colorStatus === 'REVIEW' && (status === 'PASS' || status === 'MINOR')) {
        issues.push(issue('WARN', 'COLOR_DRIFT_REVIEW', `worst non-text region ΔE00 ${res.worstRegionDeltaE.toFixed(1)} with structure SSIM ${res.score.toFixed(3)} — possible equal-luma hue/token drift`, { screen, theme: v.theme, file: roboPath, artifactSet }))
      }
      // C1-conservative: divergence OUTSIDE the oracle-side content mask — content the render
      // ADDED over the oracle's background. Independent of the SSIM chain (NOT else-if): the
      // mask excludes exactly these pixels, so `score` (and status) can be a clean PASS while
      // a rogue panel covers half the frame — this issue is the only witness.
      if (EXTRA_CONTENT_BAND !== 'off' && typeof res.extraContentFraction === 'number' && res.extraContentFraction > EXTRA_CONTENT_WARN) {
        issues.push(issue(EXTRA_CONTENT_BAND === 'block' ? 'BLOCKER' : 'WARN', 'RENDER_EXTRA_CONTENT', `render diverges on ${(res.extraContentFraction * 100).toFixed(1)}% of oracle-background pixels (warn floor ${(EXTRA_CONTENT_WARN * 100).toFixed(1)}%) — the render added content the oracle-side mask cannot score; diff.png shows where`, { screen, theme: v.theme, file: roboPath, artifactSet }))
      }
      // #18: a within-tolerance aspect mismatch was force-fit anisotropically by the
      // record-and-rescale — surface the geometric distortion (scaleFactor alone is
      // width-derived and reads 1.0 for a pure-height mismatch), so a low score points
      // at frame-vs-capture geometry, not at a phantom pixel bug.
      if (typeof res.aspectSkew === 'number' && res.aspectSkew > ASPECT_SKEW_WARN) {
        issues.push(issue('WARN', 'ASPECT_SKEW', `anisotropic rescale: oracle/capture aspect skew ${res.aspectSkew.toFixed(3)} exceeds ${ASPECT_SKEW_WARN} — content is squashed/stretched, not uniformly scaled; check the Figma frame geometry against the capture size`, { screen, theme: v.theme, file: roboPath, artifactSet }))
      }
      // Kind-geometry check: an aspect-similar [dialog]/[component]/[overlay] captured on the
      // wrong canvas survives the ASPECT gate, and record-and-rescale hides the size error —
      // the only trace is a WARN-band SSIM dent with no cause named. Compare the NATIVE capture
      // size against the bullet's own frameSizeDp (recipe: 1px == 1dp at default mdpi). WARN,
      // not BLOCKER: a deliberate density override is legal; the message names the real fix.
      if (v.kind && v.kind !== 'screen' && spec && spec.frameSizeDp && spec.frameSizeDp.w > 0 && spec.frameSizeDp.h > 0 && res.renderNative) {
        const devW = Math.abs(res.renderNative.w - spec.frameSizeDp.w) / spec.frameSizeDp.w
        const devH = Math.abs(res.renderNative.h - spec.frameSizeDp.h) / spec.frameSizeDp.h
        if (Math.max(devW, devH) > KIND_GEOMETRY_TOL) {
          issues.push(issue('WARN', 'KIND_GEOMETRY_MISMATCH', `[${v.kind}] capture is ${res.renderNative.w}x${res.renderNative.h}px but its design frame is ${spec.frameSizeDp.w}x${spec.frameSizeDp.h}dp (recipe: 1px == 1dp) — likely captured in the full-bleed screen container or under the wrong @Config qualifier instead of its own frameSizeDp (fidelity-gate §2 container-by-kind)`, { screen, theme: v.theme, file: roboPath, artifactSet }))
        }
      }
      // Spec elements whose projection fell (mostly) outside the frame would otherwise
      // vanish from zone scoring with zero trace — canvas-absolute bboxDp disables the
      // whole per-node layer silently. One WARN per screen, listing the offenders.
      if (Array.isArray(res.offFrameElements) && res.offFrameElements.length) {
        issues.push(issue('WARN', 'ZONE_OFF_FRAME', `${res.offFrameElements.length} spec element(s) project mostly/fully outside the frame (bboxDp must be frame-relative): ${res.offFrameElements.join(', ')}`, { screen, theme: v.theme, file: roboPath }))
      }
    }
  }

  // R3 — route the pixel-SIMILARITY verdict per the per-project knob (default strict). Everything
  // above was computed at full canonical strictness; here we ONLY route similarity findings so the
  // report stays self-consistent (no BLOCKER row hidden behind a WARN overall → the fail-closed
  // evidence-bundle net is untouched). Completeness / anti-forgery rows carry their OWN distinct
  // status strings (MISSING_*, ASPECT_MISMATCH, CAPTURE_IS_ORACLE_COPY, …) and BLOCKER issues — a
  // plain BLOCKER/MAJOR/MINOR row status and the PIXEL_SIMILARITY_KINDS issues are the ONLY
  // pixel-similarity signals, and the only thing this touches. advisory: a BLOCKER row → MAJOR
  // (raw kept in pixelStatus) and a similarity BLOCKER issue → WARN. off: similarity rows → PASS
  // and similarity issues dropped (still computed + shown via the artifacts, no verdict).
  const classRouting = {}   // R2-1: screen -> renderClass for rows routed to owner review
  if (pixelGate !== 'strict') {
    for (const r of results) {
      if (['BLOCKER', 'MAJOR', 'MINOR'].includes(r.status)) {
        r.pixelStatus = r.status
        r.status = pixelGate === 'off' ? 'PASS' : (r.status === 'BLOCKER' ? 'MAJOR' : r.status)
      }
    }
    const routed = []
    for (const it of issues) {
      if (!PIXEL_SIMILARITY_KINDS.has(it.issueKind)) { routed.push(it); continue }
      if (pixelGate === 'off') continue
      const sev = String(it.severity || '').toUpperCase()
      routed.push(sev === 'BLOCKER' ? Object.assign({}, it, { severity: 'WARN', message: `${it.message} (screenshotPixelGate: advisory — advisory, does not block)` }) : it)
    }
    issues.length = 0
    issues.push(...routed)
  } else {
    // R2-1: per-row class routing (STRICT mode only; advisory/off above stay byte-identical).
    // The pixel metric is provably blind on canvas (imperative draw, live data) and glass
    // (blur/gradient) component classes — real-pair corpus: genuine passes score 0.27–0.40
    // while a real fail sits INSIDE the minor range. Instead of false-blocking (or relaxing
    // the whole project to advisory), a CLASSED component row's similarity verdict routes to
    // REVIEW_REQUIRED: non-shippable (overallFor/bundle/ship all rank it non-shippable
    // already) until the OWNER's hash-bound review receipt resolves it (R3). Class source =
    // the CAS mapping registry's visualPolicy (owner-set via Mapping Review); rows are
    // matched index-node → mapping only by the durable owning design node id.
    // Completeness/anti-forgery statuses are not similarity verdicts and can never route.
    const classFor = (screen) => {
      const node = nodes[screen]
      if (!node || node.kind !== 'component') return null
      return node.nodeId ? liveRenderClasses.get(String(node.nodeId)) || null : null
    }
    for (const r of results) {
      if (!['BLOCKER', 'MAJOR', 'MINOR'].includes(r.status)) continue
      const cls = classFor(r.screen)
      if (!cls) continue
      r.pixelStatus = r.status
      r.status = 'REVIEW_REQUIRED'
      r.renderClass = cls
      classRouting[r.screen] = cls
    }
    const routedScreens = new Set(Object.keys(classRouting))
    if (routedScreens.size) {
      const routed = []
      for (const it of issues) {
        if (!PIXEL_SIMILARITY_KINDS.has(it.issueKind) || !routedScreens.has(it.screen)) { routed.push(it); continue }
        routed.push(Object.assign({}, it, {
          severity: 'WARN',
          issueKind: 'PIXEL_REVIEW_REQUIRED',
          pixelKind: it.issueKind,
          message: `${it.message} (renderClass: ${classRouting[it.screen]} — the pixel metric is blind on this class; the OWNER decides in the site's evidence tab)`,
        }))
      }
      issues.length = 0
      issues.push(...routed)
    }
  }

  console.log('-'.repeat(72))
  const overall = overallFor(results, mode, issues)
  console.log(`Overall: ${overall}\n`)
  if (artifactEntries.length) pruneArtifactRuns(artifactsRoot, stem, runId, ARTIFACT_RETENTION)

  // Aggregate per-Figma-node zones into the advisory semantic block when --semantic.
  // Bounded so a many-node screen can't bloat the report; worst (lowest SSIM) first.
  const semanticZones = [], semanticFindings = []
  if (semanticEnabled) {
    for (const r of results) {
      if (!Array.isArray(r.zones)) continue
      for (const z of r.zones) {
        semanticZones.push({ screen: r.screen, theme: r.theme, stableId: z.stableId, role: z.role, ssim: z.ssim, deltaE: z.deltaE, status: z.status, bboxPx: z.bboxPx })
        if (z.status !== 'PASS') semanticFindings.push({ screen: r.screen, theme: r.theme, stableId: z.stableId, role: z.role, kind: 'NODE_LOW_SSIM', ssim: z.ssim, deltaE: z.deltaE, status: z.status })
      }
    }
    semanticZones.sort((a, b) => a.ssim - b.ssim)
    semanticFindings.sort((a, b) => a.ssim - b.ssim)
  }
  // Fix-brief: a RANKED "start here" digest for the builder's next cycle. The report already
  // carries every fact (zones, colour axis, issue reasons), but scattered — a failed run cost
  // the builder a full re-read to find the worst offender. Ranking: hard incomplete statuses
  // (nothing rendered/comparable — fix these before pixel work) → zone-floor hits and worst
  // zones (ssim asc) → whole-frame band misses → colour-only witnesses. Bounded; advisory.
  const fixBrief = []
  for (const r of results) {
    if (incompleteStatus(r.status) || r.status === 'REVIEW_REQUIRED' || r.status === 'CAPTURE_IS_ORACLE_COPY' || r.status === 'CAPTURE_PATH_UNCONTAINED') {
      fixBrief.push({ rank: 0, screen: r.screen, theme: r.theme, hint: `${r.status}: ${r.reason || r.error || 'no comparable capture'} — fix the capture/oracle wiring before any pixel work` })
      continue
    }
    if (r.zoneFloorHit) fixBrief.push({ rank: 1, screen: r.screen, theme: r.theme, hint: `zone '${r.zoneFloorHit.label}' SSIM ${r.zoneFloorHit.ssim.toFixed(3)} under its ${r.zoneFloorHit.isText ? 'text ' : ''}floor ${r.zoneFloorHit.threshold} — this single element decides the verdict; start here` })
    if (Array.isArray(r.zones)) {
      for (const z of r.zones.slice(0, 3)) {
        if (z.status === 'PASS') continue
        fixBrief.push({ rank: 2 + (typeof z.ssim === 'number' ? z.ssim : 1), screen: r.screen, theme: r.theme, hint: `element '${z.stableId || z.label || z.role || 'unnamed'}' SSIM ${typeof z.ssim === 'number' ? z.ssim.toFixed(3) : '?'}${typeof z.deltaE === 'number' ? `, ΔE00 ${z.deltaE.toFixed(1)}` : ''} — compare its zone in diff.png against the spec entry` })
      }
    }
    if ((r.status === 'BLOCKER' || r.status === 'MAJOR') && typeof r.score === 'number' && !r.zoneFloorHit) fixBrief.push({ rank: 3, screen: r.screen, theme: r.theme, hint: `whole-frame SSIM ${r.score.toFixed(3)} in the ${r.status} band — broad divergence: check container/theme/geometry first, then per-element zones` })
    if (r.mode === 'color-only' && r.status !== 'PASS') fixBrief.push({ rank: 3.5, screen: r.screen, theme: r.theme, hint: `uniform-fill oracle diverges by colour (mean ΔE00 ${typeof r.meanDeltaE === 'number' ? r.meanDeltaE.toFixed(2) : '?'}) — wrong background/token, not a layout bug` })
    if (r.colorStatus === 'REVIEW') fixBrief.push({ rank: 4, screen: r.screen, theme: r.theme, hint: `structure matches but worst-region ΔE00 ${typeof r.worstRegionDeltaE === 'number' ? r.worstRegionDeltaE.toFixed(1) : '?'} — likely a colour-token drift (check fills against the spec tokens)` })
    if (typeof r.extraContentFraction === 'number' && r.extraContentFraction > EXTRA_CONTENT_WARN) fixBrief.push({ rank: 4.5, screen: r.screen, theme: r.theme, hint: `render adds content on ${(r.extraContentFraction * 100).toFixed(1)}% of oracle-background pixels — something extra is drawn that the design does not show (see diff.png)` })
  }
  fixBrief.sort((a, b) => a.rank - b.rank)
  const fixBriefOut = fixBrief.slice(0, 20).map(({ screen, theme, hint }) => ({ screen, theme, hint }))
  if (fixBriefOut.length && overall !== 'PASS') {
    console.log('Fix first:')
    for (const f of fixBriefOut.slice(0, 6)) console.log(`  ${f.screen} [${f.theme}] — ${f.hint}`)
    if (fixBriefOut.length > 6) console.log(`  … ${fixBriefOut.length - 6} more in report.fixBrief`)
  }

  const extra = {
    metric: METRIC_NAME,
    fixBrief: fixBriefOut,
    // The metric-INTERNAL knobs (shiftRadius…regionGrid) are recorded too: they are just as
    // gate-deciding as the severity bands (SHIFT_RADIUS=4 + GAUSSIAN_SIGMA=3 + AA_TOLERANCE=3
    // turned a genuine 0.395 BLOCKER into PASS 1.000; MASK_MODE=color / a raised VAR_FLOOR
    // empty the variance arm of the content mask so deleted dark-on-dark content degrades to
    // the soft color-only path). The final evidence bundle canon-checks every recorded value
    // (evidence-bundle.mjs CANONICAL_SCREENSHOT_THRESHOLDS — keep in lock-step).
    thresholds: { pass: T_PASS, minor: T_MINOR, major: T_MAJOR, bgTolerance: BG_TOLERANCE, minCoverage: MIN_COVERAGE, aspectTolerance: ASPECT_TOL, majorBand: MAJOR_BAND, colorAxis: COLOR_AXIS, deltaEPass: DELTAE_PASS, zoneGate: ZONE_GATE, zoneBlocker: Z_BLOCKER, zoneTextBlocker: Z_TEXT_BLOCKER, minRegionPx: MIN_REGION_PX, extraContentWarn: EXTRA_CONTENT_WARN, extraContentBand: EXTRA_CONTENT_BAND, extraContentDeltaE: EXTRA_CONTENT_DELTAE, extraContentRingPx: EXTRA_CONTENT_RING_PX, aspectSkewWarn: ASPECT_SKEW_WARN, kindGeometryTol: KIND_GEOMETRY_TOL, statusBarDp: STATUS_BAR_DP, navBarDp: NAV_BAR_DP, pixelGate, shiftRadius: SHIFT_RADIUS, gaussianSigma: GAUSSIAN_SIGMA, aaTolerance: AA_TOLERANCE, varFloor: VAR_FLOOR, maskMode: MASK_MODE, deltaEStride: DELTAE_STRIDE, regionGrid: `${REGION_ROWS}x${REGION_COLS}` },
    classRouting,
    // R5-3 audit record: present only when a manifest localeTag triggered the resolution.
    ...(designLocaleMemo ? { designLocale: designLocaleMemo } : {}),
    // R5-3 anti-forgery: fixture-only locale env overrides are recorded UNCONDITIONALLY when
    // set (even if they blanked the detection into a no-op) — the final bundle blocks on this.
    ...(localeEnvOverrides.length ? { designLocaleEnvOverrides: localeEnvOverrides } : {}),
    semantic: {
      enabled: semanticEnabled,
      status: semanticEnabled ? 'ADVISORY_UNCALIBRATED' : 'DISABLED',
      metric: semanticEnabled ? (semanticZones.length ? 'semantic-node-ssim-v1' : 'semantic-masked-ssim-v1') : null,
      promoted: false,
      zones: semanticZones.slice(0, 200),
      findings: semanticFindings.slice(0, 100),
    },
    results,
    artifactSets,
  }
  if (artifactEntries.length) {
    extra.artifactSet = {
      schemaVersion: 1,
      kind: 'screenshot-compare-artifact-set',
      root: artifactPublicPath(join(artifactsRoot, artifactSegment(stem), artifactSegment(runId))),
      retention: { keepRuns: ARTIFACT_RETENTION },
      entries: artifactEntries,
    }
  }
  const { reportPath } = writeReport({
    name: 'screenshot',
    taskStem: stem,
    mode,
    inputs: { screensDir, indexPath, roborazziDirs: roboDirs, manifestPath: manifest.path, captureStartedAt: process.env.SCREENSHOT_CAPTURE_STARTED_AT || null, captureMode: manifest.recording && manifest.recording.mode || null },
    inputHashes,
    overall,
    issues,
    extra,
  })
  console.log(`Report: ${reportPath}`)
  process.exit(mode === 'gate' && ['BLOCKER', 'INCOMPLETE', 'REVIEW_REQUIRED'].includes(overall) ? 2 : 0)
}

// Pure colour-axis helpers are exported for unit validation (Sharma ΔE00 vectors);
// main() runs only when invoked as the CLI entry, so importing the module is side-effect-free.
export { deltaE00 }

if (isDirectRun(import.meta.url)) {
  main().catch((e) => {
    console.error(`FATAL: ${e.message}`)
    process.exit(1)
  })
}
