// Shared helpers for orchestrator/figma tooling scripts. Plain Node, zero deps.
// The golden invariant holds: nothing here calls Figma — these scripts only read/parse local files.
import { mkdirSync, readFileSync, existsSync, readdirSync, realpathSync, writeFileSync, lstatSync, renameSync } from 'node:fs'
import { createHash } from 'node:crypto'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'
import { basename, delimiter, dirname, isAbsolute, join, relative, resolve, sep } from 'node:path'
import {
  outcomeAppendixStatus as sharedOutcomeAppendixStatus,
  parseFigmaEnabledConfig,
} from './outcome-shape.mjs'
import artifactPathContract from './lib/artifact-path.cjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const require = createRequire(import.meta.url)
// Where THIS copy of the scripts lives. Since worktree isolation a run executes
// inside an isolated checkout that carries its own copy of the whole template,
// so the location can no longer be trusted to name the repository that owns the
// durable artifacts.
const LOCATION_ROOT = resolve(HERE, '..', '..', '..')

const EXECUTION_SIGNAL_KEYS = [
  'ORCHESTRATOR_EXECUTION_MANIFEST', 'ORCHESTRATOR_EXECUTION_ROOT',
  'ORCHESTRATOR_RUN_ID', 'ORCHESTRATOR_TASK_SNAPSHOT_FILE',
  'ORCHESTRATOR_TASK_SNAPSHOT_HASH', 'ORCHESTRATOR_WORKTREE_ID',
  'ORCHESTRATOR_WRITER_SESSION_ID', 'ORCHESTRATOR_WRITER_STEM',
]
function verifiedExecutionScope() {
  if (!EXECUTION_SIGNAL_KEYS.some((key) => String(process.env[key] || '').length > 0)) return null
  const manager = require(resolve(HERE, '..', '..', 'site', 'server', 'worktree-manager.js'))
  const proved = manager.executionEnvironmentContext(process.env)
  if (!proved || proved.ok !== true || !proved.context) {
    throw new Error(`${proved && proved.code || 'EXECUTION_ENVIRONMENT_UNPROVEN'}: ${proved && proved.message || 'execution environment is not manager-issued'}`)
  }
  return Object.freeze({ ...proved.context })
}
export const EXECUTION_SCOPE = verifiedExecutionScope()
const rawTaskSourceOverride = String(process.env.FIGMA_SCREEN_TASK_FILE || '')
if (EXECUTION_SCOPE && rawTaskSourceOverride &&
    resolve(rawTaskSourceOverride) !== EXECUTION_SCOPE.taskSnapshotFile) {
  throw new Error('FIGMA_SCREEN_TASK_FILE must equal the manager-owned task snapshot during an execution run')
}
// Task Design input follows the same immutable snapshot as task-orchestrator.
// The control task corpus may advance while a candidate is running; reading it
// here would bind Figma gates to a different task generation.
export const FIGMA_TASK_SOURCE_FILE = EXECUTION_SCOPE
  ? EXECUTION_SCOPE.taskSnapshotFile
  : rawTaskSourceOverride
export const FIGMA_TASK_SOURCE_EXPLICIT = !EXECUTION_SCOPE && Boolean(rawTaskSourceOverride)

// TWO roots, and the distinction is load-bearing (plan §12.2):
//   PROJECT_ROOT   — the CONTROL root. Every durable artifact belongs to it:
//                    caches, reports, screen caches, committed manifests and
//                    mappings, ship receipts, the task corpus, project-config —
//                    and the canonical branch key, which must name the TARGET
//                    branch and never the temporary candidate branch a run
//                    happens to sit on.
//   EXECUTION_ROOT — where the PRODUCT under test lives. During a run that is
//                    the isolated checkout carrying the candidate; every source
//                    scan, build and screenshot reads from here.
// Outside a run both collapse to the same directory, which is exactly the
// control root, so nothing changes for a control-root invocation.
export const PROJECT_ROOT = process.env.ORCHESTRATOR_PROJECT_ROOT
  ? resolve(process.env.ORCHESTRATOR_PROJECT_ROOT)
  : LOCATION_ROOT
export const EXECUTION_ROOT = EXECUTION_SCOPE ? EXECUTION_SCOPE.executionRoot : PROJECT_ROOT
// COMMITTED figma assets (schemas, thresholds, manifests, mappings) stay
// location-derived on purpose. Inside an execution checkout they are the base
// commit's own bytes — identical to the control root's, because a candidate may
// never contain `orchestrator/**` — so reading them here is reading the exact
// generation this run was pinned to. Only MUTABLE state (the cache below,
// reports, screen caches, receipts) is control-anchored, since that must
// survive the checkout being released.
const FIGMA_DIR = EXECUTION_SCOPE
  ? join(EXECUTION_ROOT, 'orchestrator', 'figma')
  : resolve(HERE, '..')

function fileBytesOrEmpty(file) {
  let bytes = Buffer.alloc(0)
  try { bytes = readFileSync(file) } catch (error) {
    if (!error || error.code !== 'ENOENT') throw error
  }
  return bytes
}
function hashBytes(bytes) {
  return 'sha256:' + createHash('sha256').update(bytes).digest('hex')
}
if (EXECUTION_SCOPE && hashBytes(fileBytesOrEmpty(join(FIGMA_DIR, 'manifests', 'current-generation.json'))) !==
    EXECUTION_SCOPE.figmaGenerationHash) {
  throw new Error('FIGMA_EXECUTION_GENERATION_MISMATCH: execution checkout does not contain the manifest-pinned Figma generation')
}
export const PROJECT_CONFIG_FILE = join(EXECUTION_ROOT, 'orchestrator', 'project-config.md')
const EXECUTION_PROJECT_CONFIG_BYTES = EXECUTION_SCOPE ? fileBytesOrEmpty(PROJECT_CONFIG_FILE) : null
export const PROJECT_CONFIG_HASH = EXECUTION_SCOPE
  ? hashBytes(EXECUTION_PROJECT_CONFIG_BYTES)
  : null
if (EXECUTION_SCOPE && PROJECT_CONFIG_HASH !==
    EXECUTION_SCOPE.projectConfigHash) {
  throw new Error('PROJECT_CONFIG_EXECUTION_GENERATION_MISMATCH: execution checkout does not contain the manifest-pinned project config')
}

export function isDirectRun(metaUrl, argvPath = process.argv[1]) {
  if (!argvPath) return false
  try {
    return realpathSync(fileURLToPath(metaUrl)) === realpathSync(argvPath)
  } catch {
    return fileURLToPath(metaUrl) === resolve(argvPath)
  }
}

const C = { reset: '\x1b[0m', red: '\x1b[31m', yellow: '\x1b[33m', green: '\x1b[32m', dim: '\x1b[2m' }
let pass = 0, warn = 0, fail = 0
export const ok = (m) => { pass++; console.log(`${C.green}PASS${C.reset} ${m}`) }
export const warnMsg = (m) => { warn++; console.log(`${C.yellow}WARN${C.reset} ${m}`) }
export const failMsg = (m) => { fail++; console.log(`${C.red}FAIL${C.reset} ${m}`) }
export const info = (m) => console.log(`${C.dim}····${C.reset} ${m}`)
export function summary(label) {
  console.log(`\n${label}: ${pass} pass, ${warn} warn, ${fail} fail`)
  return fail === 0 ? 0 : 1
}

export const exists = (path) => existsSync(path)
export const readJson = (path) => JSON.parse(readFileSync(path, 'utf8'))

// ── Screenshot-thresholds config ─────────────────────────────────────────────
// The committed orchestrator/figma/screenshot-thresholds.json is the ONE source
// of the screenshot gate's numeric strictness: compare-screenshots.mjs derives
// its env-knob DEFAULTS from it, and evidence-bundle.mjs derives the
// anti-forgery canon (CANONICAL_SCREENSHOT_THRESHOLDS values) from the SAME
// object — lock-step by construction, no hand-mirrored numbers. Absence or
// corruption is a HARD failure in both consumers (no fallback): a silent
// default here would let the two ends of the security boundary drift apart.
// token-schemas/screenshot-thresholds.schema.json is the ajv view of the same
// shape (compiled at the final gate); this loader enforces the structural
// minimum both consumers rely on, with the same ranges as the env knobs.
const SCREENSHOT_THRESHOLDS_PATH = join(FIGMA_DIR, 'screenshot-thresholds.json')
// [min, max, integer?] — mirrors the env-knob ranges AND the schema's
// integer-ness (a fractional integer knob must fail HERE, in both consumers,
// not survive to a parseInt truncation in compare vs a raw value in the canon).
const SCREENSHOT_THRESHOLD_RANGES = {
  pass: [0, 1], minor: [0, 1], major: [0, 1],
  aspectTolerance: [0, 1], minCoverage: [0, 1], bgTolerance: [0, 442, true], deltaEPass: [0, 100],
  shiftRadius: [0, 4, true], gaussianSigma: [0.5, 3], aaTolerance: [0, 3, true], varFloor: [0, 65025],
  deltaEStride: [1, 8, true], zoneBlocker: [0, 1], zoneTextBlocker: [0, 1], minRegionPx: [0, 1e9, true],
  statusBarDp: [0, 96], navBarDp: [0, 96], extraContentWarn: [0, 1], extraContentDeltaE: [1, 100],
  extraContentRingPx: [0, 16, true],
}
let _screenshotThresholds = null
export function loadScreenshotThresholds() {
  if (_screenshotThresholds) return _screenshotThresholds
  let cfg
  try { cfg = JSON.parse(readFileSync(SCREENSHOT_THRESHOLDS_PATH, 'utf8')) } catch (e) {
    console.error(`screenshot-thresholds config unreadable (${SCREENSHOT_THRESHOLDS_PATH}): ${e.message}`)
    process.exit(1)
  }
  const problems = []
  if (typeof cfg !== 'object' || cfg === null || Array.isArray(cfg)) problems.push('not an object')
  else {
    const expected = new Set(['version', 'metric', 'zoneGate', ...Object.keys(SCREENSHOT_THRESHOLD_RANGES)])
    for (const k of Object.keys(cfg)) if (!expected.has(k)) problems.push(`unknown key ${JSON.stringify(k)}`)
    // version = the gate-policy version (W4-3): report-utils stamps it into every report as
    // gatePolicyVersion and the final digest carries it, so a done/ task records WHICH policy
    // certified it. Bump on any strictness change; a missing/invalid version fails closed here.
    if (!Number.isInteger(cfg.version) || cfg.version < 1) problems.push(`version must be an integer >= 1 (the gate-policy version, bumped on any strictness change), got ${JSON.stringify(cfg.version)}`)
    if (cfg.metric !== 'masked-ssim-luma-v2') problems.push(`metric must be "masked-ssim-luma-v2", got ${JSON.stringify(cfg.metric)}`)
    if (cfg.zoneGate !== true) problems.push('zoneGate must be true (SCREENSHOT_ZONE_GATE=0 is a per-run rollback knob, never committable strictness)')
    for (const [k, [min, max, integer]] of Object.entries(SCREENSHOT_THRESHOLD_RANGES)) {
      const v = cfg[k]
      if (typeof v !== 'number' || !Number.isFinite(v) || v < min || v > max) problems.push(`${k} must be a number in [${min}, ${max}], got ${JSON.stringify(v)}`)
      else if (integer && !Number.isInteger(v)) problems.push(`${k} must be an INTEGER (compare-screenshots parses it with parseInt — a fraction would silently truncate), got ${JSON.stringify(v)}`)
    }
    if (!problems.length && !(cfg.pass >= cfg.minor && cfg.minor >= cfg.major)) problems.push('severity floors must be sorted: pass >= minor >= major')
  }
  if (problems.length) {
    console.error(`screenshot-thresholds config invalid (${SCREENSHOT_THRESHOLDS_PATH}):\n  - ${problems.join('\n  - ')}`)
    process.exit(1)
  }
  _screenshotThresholds = cfg
  return cfg
}

// Consolidated cache root: every ephemeral/regenerable figma artifact lives under
// orchestrator/.cache/figma/. figmaPath redirects a leading '.cache' or 'reports'
// segment there; domain-specific helpers below scope component and variables artifacts
// into their own cache subtrees. All committed roots (tokens/, manifests/,
// token-schemas/, scripts/, skill/) stay under orchestrator/figma/.
const DEFAULT_FIGMA_CACHE_DIR = join(PROJECT_ROOT, 'orchestrator', '.cache', 'figma')
function exactExecutionCacheOverride(key, fallback) {
  const raw = process.env[key]
  if (!raw) return fallback
  const candidate = resolve(raw)
  if (EXECUTION_SCOPE && candidate !== fallback) {
    throw new Error(`${key} must equal the manager-owned control-cache path during an execution run`)
  }
  return candidate
}
const FIGMA_CACHE_DIR = exactExecutionCacheOverride('FIGMA_CACHE_ROOT', DEFAULT_FIGMA_CACHE_DIR)
const DEFAULT_REPORTS_DIR = join(FIGMA_CACHE_DIR, 'reports')
const FIGMA_REPORTS_DIR = exactExecutionCacheOverride('FIGMA_REPORTS_DIR', DEFAULT_REPORTS_DIR)
const DEFAULT_SCREENS_DIR = join(FIGMA_CACHE_DIR, 'screens')
export const FIGMA_CACHE_ROOT = FIGMA_CACHE_DIR
export const figmaPath = (...parts) => {
  if (parts[0] === '.cache')  return join(FIGMA_CACHE_DIR, ...parts.slice(1))
  if (parts[0] === 'reports') return join(FIGMA_CACHE_DIR, 'reports', ...parts.slice(1))
  return join(FIGMA_DIR, ...parts)
}

export function figmaScreensRoot() {
  const cacheRoot = process.env.FIGMA_SCREEN_CACHE_ROOT
    ? exactExecutionCacheOverride('FIGMA_SCREEN_CACHE_ROOT', DEFAULT_SCREENS_DIR) : ''
  const specRoot = process.env.FIGMA_SPEC_SCREENS_DIR
    ? exactExecutionCacheOverride('FIGMA_SPEC_SCREENS_DIR', DEFAULT_SCREENS_DIR) : ''
  if (cacheRoot && specRoot && cacheRoot !== specRoot) {
    throw new Error(`FIGMA_SCREEN_CACHE_ROOT and FIGMA_SPEC_SCREENS_DIR point to different roots: ${cacheRoot} vs ${specRoot}`)
  }
  return cacheRoot || specRoot || DEFAULT_SCREENS_DIR
}

function containedPath(root, raw, label, { mustExist = true } = {}) {
  const base = resolve(root)
  const candidate = isAbsolute(String(raw || ''))
    ? resolve(String(raw))
    : resolve(base, String(raw || ''))
  const rel = relative(base, candidate)
  if (!raw || rel === '..' || rel.startsWith(`..${sep}`) || isAbsolute(rel)) {
    throw new Error(`${label} must stay under ${base}`)
  }
  if (mustExist) {
    const st = lstatSync(candidate)
    if (st.isSymbolicLink() || !st.isFile() && !st.isDirectory() ||
        st.isFile() && st.nlink !== 1) {
      throw new Error(`${label} must name one regular single-link file or real directory`)
    }
    if (realpathSync(candidate) !== join(realpathSync(base), rel)) {
      throw new Error(`${label} escapes its physical owner root`)
    }
  } else {
    let ancestor = dirname(candidate)
    while (!existsSync(ancestor) && ancestor !== dirname(ancestor)) ancestor = dirname(ancestor)
    const ancestorRel = relative(PROJECT_ROOT, ancestor)
    if (ancestorRel === '..' || ancestorRel.startsWith(`..${sep}`) || isAbsolute(ancestorRel) ||
        realpathSync(ancestor) !== join(realpathSync(PROJECT_ROOT), ancestorRel)) {
      throw new Error(`${label} has an unsafe output ancestor`)
    }
  }
  return candidate
}

export function executionProductInputPath(raw, label = 'Figma product input') {
  return EXECUTION_SCOPE ? containedPath(EXECUTION_ROOT, raw, label) : String(raw)
}

export function executionFigmaInputPath(raw, label = 'Figma runtime input') {
  return EXECUTION_SCOPE ? containedPath(FIGMA_CACHE_DIR, raw, label) : String(raw)
}

// A path recorded inside a runtime report is evidence, never authority. Resolve
// its display form against the only two task-owned input roots, then re-apply
// physical containment at the consuming boundary. Committed project inputs
// come from the execution checkout; mutable Figma inputs come from its exact
// control cache. No other worktree or host/control product path is admissible.
export function recordedFigmaInputPath(raw, label = 'Figma recorded input') {
  const value = String(raw || '')
  if (!value) return ''
  const candidates = isAbsolute(value)
    ? [resolve(value)]
    : [resolve(EXECUTION_ROOT, value), resolve(PROJECT_ROOT, value), resolve(FIGMA_CACHE_DIR, value)]
  const selected = candidates.find((candidate) => existsSync(candidate)) || candidates[0]
  if (!EXECUTION_SCOPE) return selected
  try { return executionProductInputPath(selected, label) } catch {}
  try { return executionFigmaInputPath(selected, label) } catch {}
  throw new Error(`${label} must stay under the manager-owned execution root or Figma cache`)
}

export function executionFigmaOutputPath(raw, label = 'Figma runtime output') {
  return EXECUTION_SCOPE
    ? containedPath(FIGMA_CACHE_DIR, raw, label, { mustExist: false })
    : String(raw)
}

export function writeFigmaRuntimeFile(file, data, { maxBytes = 16 * 1024 * 1024 } = {}) {
  const target = EXECUTION_SCOPE
    ? executionFigmaOutputPath(file, 'Figma runtime publication')
    : String(file)
  const bytes = Buffer.isBuffer(data) ? data : Buffer.from(String(data))
  if (!Number.isSafeInteger(maxBytes) || maxBytes < 0 || bytes.length > maxBytes) {
    throw new Error('Figma runtime publication exceeds its byte limit')
  }
  if (!EXECUTION_SCOPE) {
    mkdirSync(dirname(target), { recursive: true })
    const tmp = target + '.tmp'
    writeFileSync(tmp, bytes)
    renameSync(tmp, target)
    return target
  }
  const fileGuards = require(resolve(HERE, '..', '..', 'site', 'server', 'file-guards.js'))
  const directory = dirname(target)
  if (!fileGuards.realDirectoryUnder(PROJECT_ROOT, directory, { create: true, mode: 0o700 })) {
    throw new Error('Figma runtime publication directory is unsafe')
  }
  const published = fileGuards.atomicReplaceRegularFileResult(
    PROJECT_ROOT, directory, target, bytes,
    { create: true, directoryMode: 0o700, mode: 0o600, maxBytes },
  )
  if (!published.ok) throw new Error('Figma runtime publication failed')
  return target
}

if (EXECUTION_SCOPE) {
  const productPathLists = [
    'FIGMA_APP_TOKEN_ROOTS', 'FIGMA_APP_TOKEN_FILES',
    'FIGMA_SPEC_IMPL_ROOTS', 'FIGMA_SPEC_IMPL_FILES',
    'FIGMA_CENSUS_CODE_ROOTS', 'FIGMA_STRING_RESOURCE_ROOTS',
    'FIGMA_SECURITY_GREP_ROOTS', 'ROBORAZZI_OUTPUT_DIRS',
  ]
  for (const key of productPathLists) {
    for (const value of String(process.env[key] || '').split(delimiter).filter(Boolean)) {
      executionProductInputPath(value, key)
    }
  }
  for (const key of ['FIGMA_OBSERVED_TOKEN_CATALOG', 'FIGMA_OBSERVED_TOKEN_SOURCE_INDEX',
    'FIGMA_TOKEN_BINDING_SNAPSHOT', 'FIGMA_SPEC_SCHEMA']) {
    if (process.env[key]) throw new Error(`${key} is a fixture/diagnostic override and is forbidden during an execution run`)
  }
  for (const key of ['FIGMA_APP_TOKENS_OUT', 'FIGMA_IMPL_MODEL_OUT', 'FIGMA_RESOLVED_SPEC_OUT']) {
    if (process.env[key]) executionFigmaOutputPath(process.env[key], key)
  }
  for (const key of ['FIGMA_APP_TOKENS', 'FIGMA_IMPL_MODEL']) {
    if (process.env[key]) executionFigmaInputPath(process.env[key], key)
  }
  if (process.env.ROBORAZZI_OUTPUT_DIR) {
    executionProductInputPath(process.env.ROBORAZZI_OUTPUT_DIR, 'ROBORAZZI_OUTPUT_DIR')
  }
  if (process.env.SCREENSHOT_CAPTURE_MANIFEST) {
    recordedFigmaInputPath(process.env.SCREENSHOT_CAPTURE_MANIFEST,
      'SCREENSHOT_CAPTURE_MANIFEST')
  }
  if (process.env.FIGMA_PIXEL_REVIEW_DIR &&
      resolve(process.env.FIGMA_PIXEL_REVIEW_DIR) !==
        join(PROJECT_ROOT, 'orchestrator', 'tasks', 'evidence', 'pixel-review')) {
    throw new Error('FIGMA_PIXEL_REVIEW_DIR must equal the control-plane review authority during an execution run')
  }
  if (process.env.FIGMA_ORACLE_MAX_AGE_DAYS) {
    throw new Error('FIGMA_ORACLE_MAX_AGE_DAYS is a diagnostic override and is forbidden during an execution run')
  }
}

export function parseCli({ allowedFlags = [], valueFlags = [], booleanFlags = [], usage = 'usage unavailable' } = {}) {
  const allowed = new Set(allowedFlags)
  const value = new Set(valueFlags)
  const boolean = new Set(booleanFlags)
  const positional = []
  const values = {}
  const flags = new Set()
  for (let i = 2; i < process.argv.length; i++) {
    const arg = process.argv[i]
    if (!arg.startsWith('--')) { positional.push(arg); continue }
    if (!allowed.has(arg)) throw new Error(`unknown argument ${arg}\n${usage}`)
    flags.add(arg)
    if (value.has(arg)) {
      let next = process.argv[++i]
      if (!next || next.startsWith('--')) throw new Error(`missing value for ${arg}\n${usage}`)
      if (EXECUTION_SCOPE) {
        if (arg.startsWith('--fixture-')) {
          throw new Error(`${arg} is a fixture override and is forbidden during an execution run`)
        }
        if (['--root', '--file', '--code-root', '--impl-root', '--impl-file'].includes(arg)) {
          next = executionProductInputPath(next, arg)
        }
        if (arg === '--verdict-file') next = executionProductInputPath(next, arg)
        if (arg === '--impl-model') next = executionFigmaInputPath(next, arg)
        if (arg === '--out') next = executionFigmaOutputPath(next, arg)
        if (arg === '--screens-dir' &&
            (isAbsolute(next) ? resolve(next) : resolve(EXECUTION_ROOT, next)) !== DEFAULT_SCREENS_DIR) {
          throw new Error('--screens-dir must equal the manager-owned screen cache during an execution run')
        }
      }
      if (!values[arg]) values[arg] = []
      values[arg].push(next)
    } else if (!boolean.has(arg)) {
      throw new Error(`argument ${arg} is not configured as value or boolean flag`)
    }
  }
  return { positional, values, flags, has: (f) => flags.has(f), value: (f) => (values[f] || [])[0] || '', valuesFor: (f) => values[f] || [] }
}

// Map validated, non-empty logical identities to bounded artifact path segments. The
// logical values remain full-length in reports; only filesystem segments use
// this shared, collision-resistant mapping. Server readers and final evidence
// import the same CJS contract, preventing writer/verifier drift.
export const artifactSegment = artifactPathContract.artifactSegment

function runStemSegment(stem) { return artifactSegment(stem) }

// ── Pipeline run id (file-pinned per stem) ───────────────────────────────────
// One task run = one id across every report, or the final `--fresh` bundle rejects the set
// as a stale/mixed run. The id used to live only in the shell env — but every tool
// invocation is its own process, so an unexported FIGMA_PIPELINE_RUN_ID silently minted a
// fresh id per process (ship-done included) and the whole genuinely-fresh report set
// blocked with REPORT_STALE_RUN. The pin file makes the id durable per stem: env (when
// set) still wins and re-pins the file; otherwise the pinned id is reused; otherwise a
// fresh id is minted and pinned. evidence-clean's full clean removes the pin
// (--bundle-only keeps it — an immediate re-bundle must stay on the same run).
const RUN_ID_RE = /^[A-Za-z0-9][A-Za-z0-9._-]{0,159}$/

function assertPipelineRunId(raw, label = 'FIGMA_PIPELINE_RUN_ID') {
  const id = String(raw || '').trim()
  if (!RUN_ID_RE.test(id) || id.includes('..')) {
    throw new Error(`${label} must match ${RUN_ID_RE}, be at most 160 characters, and must not contain '..'; got ${JSON.stringify(raw)}`)
  }
  return id
}

export function runIdPinPath(stem = 'run') {
  return join(FIGMA_REPORTS_DIR, `.run-id-${runStemSegment(stem)}`)
}

function readPinnedRunId(pinPath) {
  let raw
  try { raw = readFileSync(pinPath, 'utf8').trim() } catch (error) {
    if (error && error.code === 'ENOENT') return ''
    throw error
  }
  return assertPipelineRunId(raw, 'pinned pipeline run id')
}

function writePinnedRunId(pinPath, id) {
  writeFigmaRuntimeFile(pinPath, id + '\n', { maxBytes: 1024 })
}

export function pipelineRunId(stem = 'run') {
  const pinPath = runIdPinPath(stem)
  const rawEnvId = String(process.env.FIGMA_PIPELINE_RUN_ID || '')
  if (rawEnvId.trim()) {
    const envId = assertPipelineRunId(rawEnvId)
    const pinned = readPinnedRunId(pinPath)
    if (pinned !== envId) {
      if (pinned) console.error(`re-pinning run id for ${runStemSegment(stem)}: ${pinned} -> ${envId} (env override)`)
      writePinnedRunId(pinPath, envId)
    }
    return envId
  }
  const pinned = readPinnedRunId(pinPath)
  if (pinned) return pinned
  const ts = new Date().toISOString().replace(/[-:.]/g, '')
  const minted = `${ts}-${runStemSegment(stem)}`
  writePinnedRunId(pinPath, minted)
  return minted
}

function slugSegment(raw, label = 'path segment') {
  const s = String(raw || '').trim()
  if (!/^[A-Za-z0-9][A-Za-z0-9._-]*$/.test(s)) throw new Error(`${label} must match /^[A-Za-z0-9][A-Za-z0-9._-]*$/; got ${JSON.stringify(raw)}`)
  if (s === '.' || s === '..' || s.includes('..')) throw new Error(`${label} must not contain traversal segments: ${JSON.stringify(raw)}`)
  return s
}

export function ensureContained(root, target) {
  const r = resolve(root)
  const t = resolve(target)
  const rel = relative(r, t)
  if (rel === '' || (!rel.startsWith('..') && !isAbsolute(rel))) return t
  throw new Error(`path escapes ${r}: ${target}`)
}

export function cacheRelative(path) {
  const rel = relative(FIGMA_CACHE_DIR, resolve(path))
  if (rel.startsWith('..') || isAbsolute(rel)) return null
  return rel.split(sep).join('/')
}

export function projectRelative(path) {
  const rel = relative(PROJECT_ROOT, resolve(path))
  if (rel.startsWith('..') || isAbsolute(rel)) return null
  return rel.split(sep).join('/')
}

export function displayPath(path) {
  return cacheRelative(path) || projectRelative(path) || basename(String(path || ''))
}

// slug -> camelCase id. Canonical normalized tokens are required to map one-to-one to Kotlin ids;
// collisions are rejected instead of being hidden behind order-dependent numeric suffixes.
const PASCAL = (s) => String(s).replace(/[^a-zA-Z0-9]+/g, ' ').trim().split(/\s+/)
  .map((w) => w.charAt(0).toUpperCase() + w.slice(1)).join('') || 'Default'
export const camel = (s) => { const p = PASCAL(s); return p.charAt(0).toLowerCase() + p.slice(1) }

// Shared export over outcome-shape.mjs's structural parser. The done-gates
// (ship-done/verify-done) MUST read Status through this path so Figma tooling cannot drift from
// the canonical Outcome anchor/fence/HTML-block semantics or accidentally pick up a body field.
export function outcomeAppendixStatus(md) {
  return sharedOutcomeAppendixStatus(md)
}

// Read a frontmatter scalar from orchestrator/project-config.md without a YAML dep.
// undefined = key absent; null = still the <placeholder>; otherwise the trimmed value.
// Per-task bindings manifest (screens/<stem>/bindings.json): the optional explicit
// binding artifact (screen↔impl-file, capture↔node, component↔code). Absence is the only
// null case; a present malformed or foreign file is a hard configuration error.
export function loadBindings(stem) {
  const path = join(figmaScreensRoot(), stem, 'bindings.json')
  if (!existsSync(path)) return null
  let raw
  try { raw = JSON.parse(readFileSync(path, 'utf8')) }
  catch (error) { throw new Error(`bindings.json is not valid JSON: ${error.message}`) }
  if (!raw || typeof raw !== 'object' || Array.isArray(raw) || raw.schemaVersion !== 2 || raw.stem !== stem || !Array.isArray(raw.screens)) {
    throw new Error('bindings.json must satisfy the current schemaVersion 2 contract and match the task stem — re-pull the screens cache for the current pipeline')
  }
  const topKeys = Object.keys(raw)
  if (topKeys.some((key) => !['schemaVersion', 'stem', 'screens', 'components'].includes(key))) throw new Error('bindings.json contains unknown top-level keys')
  const screenKeys = new Set(['nodeId', 'screenName', 'implFile', 'composable', 'captureBasename', 'qualifiers', 'kind'])
  const screenNames = new Set()
  for (const [index, screen] of raw.screens.entries()) {
    if (!screen || typeof screen !== 'object' || Array.isArray(screen) || typeof screen.screenName !== 'string' || !screen.screenName || Object.keys(screen).some((key) => !screenKeys.has(key))) {
      throw new Error(`bindings.json screens[${index}] is invalid`)
    }
    if (screenNames.has(screen.screenName)) throw new Error(`bindings.json contains duplicate screenName ${JSON.stringify(screen.screenName)}`)
    screenNames.add(screen.screenName)
    for (const key of ['nodeId', 'implFile', 'composable', 'captureBasename', 'qualifiers']) {
      if (screen[key] !== undefined && (typeof screen[key] !== 'string' || !screen[key])) throw new Error(`bindings.json screens[${index}].${key} is invalid`)
    }
    if (screen.implFile && EXECUTION_SCOPE) {
      screen.implFile = executionProductInputPath(screen.implFile, `bindings.json screens[${index}].implFile`)
    }
    if (screen.kind !== undefined && !['screen', 'dialog', 'component', 'overlay'].includes(screen.kind)) throw new Error(`bindings.json screens[${index}].kind is invalid`)
  }
  // Component rows are keyed by the durable design identity: two sets sharing
  // one display name are two rows. setName stays a label and may repeat.
  const components = raw.components === undefined ? [] : raw.components
  if (!Array.isArray(components)) throw new Error('bindings.json components must be an array')
  const componentKeys = new Set(['designComponentId', 'setNodeId', 'setName', 'mappingId', 'implementations'])
  const implementationKeys = new Set(['adapterId', 'platform', 'projectComponentId', 'sourcePath'])
  const designIds = new Set()
  const setNodeIds = new Set()
  for (const [index, component] of components.entries()) {
    if (!component || typeof component !== 'object' || Array.isArray(component) ||
      Object.keys(component).some((key) => !componentKeys.has(key)) ||
      ['designComponentId', 'setNodeId', 'setName', 'mappingId'].some((key) => typeof component[key] !== 'string' || !component[key]) ||
      !Array.isArray(component.implementations) || !component.implementations.length) {
      throw new Error(`bindings.json components[${index}] is invalid`)
    }
    if (designIds.has(component.designComponentId)) throw new Error(`bindings.json contains duplicate designComponentId ${JSON.stringify(component.designComponentId)}`)
    if (setNodeIds.has(component.setNodeId)) throw new Error(`bindings.json contains duplicate setNodeId ${JSON.stringify(component.setNodeId)}`)
    designIds.add(component.designComponentId)
    setNodeIds.add(component.setNodeId)
    for (const [implIndex, implementation] of component.implementations.entries()) {
      if (!implementation || typeof implementation !== 'object' || Array.isArray(implementation) ||
        Object.keys(implementation).some((key) => !implementationKeys.has(key)) ||
        ['adapterId', 'platform', 'projectComponentId'].some((key) => typeof implementation[key] !== 'string' || !implementation[key]) ||
        (implementation.sourcePath !== undefined && (typeof implementation.sourcePath !== 'string' || !implementation.sourcePath))) {
        throw new Error(`bindings.json components[${index}].implementations[${implIndex}] is invalid`)
      }
      if (implementation.sourcePath && EXECUTION_SCOPE) {
        implementation.sourcePath = executionProductInputPath(implementation.sourcePath,
          `bindings.json components[${index}].implementations[${implIndex}].sourcePath`)
      }
    }
  }
  return { screens: raw.screens, components }
}

// W2-6 — derive capture-manifest entries from explicit bindings. Pure: the caller supplies
// the exists probe; the current index variants are handled separately by the driver.
export function bindingsManifestEntries(bindings, roboDirs, existsFn) {
  const entries = []
  const boundScreens = new Set()
  for (const s of (bindings && bindings.screens) || []) {
    if (!s.captureBasename || !s.nodeId) continue
    for (const dir of roboDirs) {
      const path = join(dir, s.captureBasename)
      if (!existsFn(path)) continue
      entries.push({ captureName: s.captureBasename, path, nodeId: String(s.nodeId), primaryState: true })
      boundScreens.add(s.screenName)
    }
  }
  return { entries, boundScreens }
}

export function readConfig(key) {
  if (!EXECUTION_SCOPE && !existsSync(PROJECT_CONFIG_FILE)) return undefined
  const text = EXECUTION_SCOPE
    ? EXECUTION_PROJECT_CONFIG_BYTES.toString('utf8')
    : readFileSync(PROJECT_CONFIG_FILE, 'utf8')
  if (key === 'figmaEnabled') return parseFigmaEnabledConfig(text) ? 'true' : 'false'
  const escapedKey = String(key).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const m = text.match(new RegExp(`^${escapedKey}:[ \\t]*(.+?)[ \\t]*$`, 'm'))
  if (!m) return undefined
  const v = m[1].trim()
  return v.startsWith('<') ? null : v
}
