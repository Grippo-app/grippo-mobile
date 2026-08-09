// Shared helpers for orchestrator/figma tooling scripts. Plain Node, zero deps.
// The golden invariant holds: nothing here calls Figma — these scripts only read/parse local files.
import { mkdirSync, readFileSync, existsSync, readdirSync, realpathSync, writeFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { basename, dirname, isAbsolute, join, relative, resolve, sep } from 'node:path'
import {
  outcomeAppendixStatus as sharedOutcomeAppendixStatus,
  parseFigmaEnabledConfig,
} from './outcome-shape.mjs'
import artifactPathContract from './lib/artifact-path.cjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const FIGMA_DIR = resolve(HERE, '..')                     // orchestrator/figma
export const PROJECT_ROOT = resolve(FIGMA_DIR, '..', '..') // repo root (figma -> orchestrator -> root)

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
const FIGMA_CACHE_DIR = process.env.FIGMA_CACHE_ROOT ? resolve(process.env.FIGMA_CACHE_ROOT) : DEFAULT_FIGMA_CACHE_DIR
export const FIGMA_CACHE_ROOT = FIGMA_CACHE_DIR
export const figmaPath = (...parts) => {
  if (parts[0] === '.cache')  return join(FIGMA_CACHE_DIR, ...parts.slice(1))
  if (parts[0] === 'reports') return join(FIGMA_CACHE_DIR, 'reports', ...parts.slice(1))
  return join(FIGMA_DIR, ...parts)
}

export function figmaScreensRoot() {
  const cacheRoot = process.env.FIGMA_SCREEN_CACHE_ROOT ? resolve(process.env.FIGMA_SCREEN_CACHE_ROOT) : ''
  const specRoot = process.env.FIGMA_SPEC_SCREENS_DIR ? resolve(process.env.FIGMA_SPEC_SCREENS_DIR) : ''
  if (cacheRoot && specRoot && cacheRoot !== specRoot) {
    throw new Error(`FIGMA_SCREEN_CACHE_ROOT and FIGMA_SPEC_SCREENS_DIR point to different roots: ${cacheRoot} vs ${specRoot}`)
  }
  return cacheRoot || specRoot || figmaPath('.cache', 'screens')
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
      const next = process.argv[++i]
      if (!next || next.startsWith('--')) throw new Error(`missing value for ${arg}\n${usage}`)
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
  const dir = process.env.FIGMA_REPORTS_DIR || figmaPath('reports')
  return join(dir, `.run-id-${runStemSegment(stem)}`)
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
  mkdirSync(dirname(pinPath), { recursive: true })
  writeFileSync(pinPath, id + '\n')
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
  const cfg = join(PROJECT_ROOT, 'orchestrator', 'project-config.md')
  if (!existsSync(cfg)) return undefined
  const text = readFileSync(cfg, 'utf8')
  if (key === 'figmaEnabled') return parseFigmaEnabledConfig(text) ? 'true' : 'false'
  const escapedKey = String(key).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const m = text.match(new RegExp(`^${escapedKey}:[ \\t]*(.+?)[ \\t]*$`, 'm'))
  if (!m) return undefined
  const v = m[1].trim()
  return v.startsWith('<') ? null : v
}
