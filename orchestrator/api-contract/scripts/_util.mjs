// Shared helpers for orchestrator/api-contract tooling scripts. Plain Node, zero deps.
// The helpers never call the backend. Canonical live fetches are owned by backend-action.mjs.
import { readFileSync, existsSync, writeFileSync, renameSync, mkdirSync, lstatSync, realpathSync } from 'node:fs'
import { createHash } from 'node:crypto'
import { fileURLToPath } from 'node:url'
import { dirname, join, relative, resolve, sep } from 'node:path'
import { createRequire } from 'node:module'

const HERE = dirname(fileURLToPath(import.meta.url))
const SIDECAR_DIR = resolve(HERE, '..')                        // orchestrator/api-contract (code root)
export const contractCodePath = (...parts) => join(SIDECAR_DIR, ...parts)
export const PROJECT_ROOT = resolve(process.env.ORCHESTRATOR_PROJECT_ROOT || resolve(SIDECAR_DIR, '..', '..'))
const PROJECT_CONTRACT_DIR = join(PROJECT_ROOT, 'orchestrator', 'api-contract')
const RUNTIME_CACHE_ROOT = resolve(process.env.ORCHESTRATOR_CACHE_DIR ||
  join(PROJECT_ROOT, 'orchestrator', '.cache'))
const STAGING_ROOT = join(RUNTIME_CACHE_ROOT, 'api-contract', 'staging')
function ownedStagingDir(raw, leaf) {
  const candidate = resolve(raw)
  const rel = relative(STAGING_ROOT, candidate)
  const parts = rel.split(sep)
  if (rel === '..' || rel.startsWith(`..${sep}`) || rel === '' || parts.length !== 2 ||
      !/^job-[a-f0-9]{32}$/.test(parts[0]) || parts[1] !== leaf) {
    throw new Error(`ORCHESTRATOR_API_CONTRACT_${leaf === 'data' ? 'DATA' : 'CACHE'}_DIR is reserved for sidecar-owned staging`)
  }
  const stat = lstatSync(candidate)
  const stagingRelative = relative(PROJECT_ROOT, STAGING_ROOT)
  if (stagingRelative === '..' || stagingRelative.startsWith(`..${sep}`) ||
      resolve(PROJECT_ROOT, stagingRelative) !== STAGING_ROOT) {
    throw new Error('api-contract runtime cache must stay under the project root')
  }
  const anchored = join(realpathSync(PROJECT_ROOT), stagingRelative, ...parts)
  if (!stat.isDirectory() || stat.isSymbolicLink() || realpathSync(candidate) !== anchored) {
    throw new Error(`ORCHESTRATOR_API_CONTRACT_${leaf === 'data' ? 'DATA' : 'CACHE'}_DIR staging path is unsafe`)
  }
  return { path: candidate, jobId: parts[0] }
}
const DATA_OVERRIDE = process.env.ORCHESTRATOR_API_CONTRACT_DATA_DIR
const CACHE_OVERRIDE = process.env.ORCHESTRATOR_API_CONTRACT_CACHE_DIR
if (Boolean(DATA_OVERRIDE) !== Boolean(CACHE_OVERRIDE)) {
  throw new Error('api-contract staging data/cache overrides must be supplied together')
}
const STAGING_DATA = DATA_OVERRIDE ? ownedStagingDir(DATA_OVERRIDE, 'data') : null
const STAGING_CACHE = CACHE_OVERRIDE ? ownedStagingDir(CACHE_OVERRIDE, 'runtime') : null
if (STAGING_DATA && STAGING_DATA.jobId !== STAGING_CACHE.jobId) {
  throw new Error('api-contract staging data/cache overrides must belong to the same job')
}
// Typed probe/refresh runs deterministic normalizers against an owned staging data
// root. This override is set only on the exact spawned child and keeps probe
// mode physically incapable of touching committed manifests.
const CONTRACT_DATA_DIR = STAGING_DATA ? STAGING_DATA.path : PROJECT_CONTRACT_DIR

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

export function redactUrl(value) {
  const raw = String(value || '')
  const secretKey = (key) => /token|key|secret|password|auth|signature/i.test(key) || /^sig$/i.test(key)
  try {
    const u = new URL(raw)
    if (u.username) u.username = '***'
    if (u.password) u.password = '***'
    for (const key of [...u.searchParams.keys()]) {
      if (secretKey(key)) u.searchParams.set(key, '***')
    }
    if (u.hash && u.hash.includes('=')) {
      const params = new URLSearchParams(u.hash.slice(1))
      for (const key of [...params.keys()]) {
        if (secretKey(key)) params.set(key, '***')
      }
      u.hash = params.toString()
    }
    return u.toString()
  } catch {
    return raw.replace(/([?&#](?:[^=&#]*(?:token|key|secret|password|auth|signature)|sig)=)[^&#\s]+/ig, '$1***')
  }
}

// Consolidated cache root: every ephemeral/regenerable api-contract artifact now lives under
// orchestrator/.cache/api-contract/. contractPath transparently
// redirects a leading '.cache' or 'reports' segment there; all other roots (spec/, manifests/,
// contract-schemas/, scripts/) and the guarded `.secrets/` credential store
// stay under orchestrator/api-contract/.
// This keeps every existing contractPath('.cache', …) / contractPath('reports', …) call-site correct with no edit.
const CONTRACT_CACHE_DIR = STAGING_CACHE
  ? STAGING_CACHE.path
  : join(RUNTIME_CACHE_ROOT, 'api-contract')
export const contractPath = (...parts) => {
  if (parts[0] === '.cache')  return join(CONTRACT_CACHE_DIR, ...parts.slice(1))
  if (parts[0] === 'reports') return join(CONTRACT_CACHE_DIR, 'reports', ...parts.slice(1))
  return join(CONTRACT_DATA_DIR, ...parts)
}

// Atomic write (tmp + rename) — a crashed script never leaves a half-written committed snapshot.
// Strings are written verbatim; everything else is pretty-printed JSON with a trailing newline.
export function atomicWrite(path, data) {
  mkdirSync(dirname(path), { recursive: true })
  const tmp = path + '.tmp'
  writeFileSync(tmp, typeof data === 'string' ? data : JSON.stringify(data, null, 2) + '\n')
  renameSync(tmp, path)
}

export const sha256 = (text) => 'sha256:' + createHash('sha256').update(text).digest('hex')

// Naming helpers shared by normalize/import-postman/diff so the two snapshot producers can never
// synthesize a different identity for the same endpoint/schema.
export const kebab = (s) => String(s).replace(/([a-z0-9])([A-Z])/g, '$1-$2').replace(/[^A-Za-z0-9]+/g, '-').replace(/-+/g, '-').replace(/^-|-$/g, '').toLowerCase()
export const pascal = (s) => String(s).replace(/([a-z0-9])([A-Z])/g, '$1 $2').split(/[^A-Za-z0-9]+/).filter(Boolean).map((w) => w[0].toUpperCase() + w.slice(1)).join('')
export const lowerCamel = (s) => { const p = pascal(s); return p ? p[0].toLowerCase() + p.slice(1) : String(s) }
// Canonical derived operation id when the source format omits one:
// GET /notes/{id} -> getNotesId.
export const synthOperationId = (method, path) =>
  String(method).toLowerCase() + String(path).split('/').filter(Boolean).map((seg) => pascal(seg.replace(/[{}:]/g, ''))).join('')

// Read a frontmatter scalar from orchestrator/project-config.md without a YAML dep.
// undefined = key absent; null = still the <placeholder>; otherwise the trimmed value.
export function readConfig(key) {
  const cfg = join(PROJECT_ROOT, 'orchestrator', 'project-config.md')
  if (!existsSync(cfg)) return undefined
  const m = readFileSync(cfg, 'utf8').match(new RegExp(`^${key}:\\s*(.+)$`, 'm'))
  if (!m) return undefined
  const v = m[1].trim()
  return v.startsWith('<') ? null : v
}

// Generation-aware read locations for offline consumers. Writers keep using
// contractPath against their owned staging target; readers switch to a
// fully validated current generation whenever the atomic pointer exists.
export function currentContractFiles() {
  if (process.env.ORCHESTRATOR_API_CONTRACT_DATA_DIR) {
    return { mode: 'staging', inventory: contractPath('manifests', 'endpoint-inventory.json'),
      areasDir: contractPath('manifests', 'areas'), spec: contractPath('spec', 'openapi.json'),
      snapshotHash: null, committedGenerationId: null, environmentId: null, invalid: false }
  }
  try {
    const require = createRequire(import.meta.url)
    const reader = require(resolve(HERE, '..', '..', 'site', 'server', 'contract-generation.js'))
    const current = reader.current()
    if (!current.ok) return { mode: 'invalid', inventory: null, areasDir: null, spec: null,
      snapshotHash: null, committedGenerationId: null, environmentId: null,
      invalid: true, error: current.error }
    if (current.mode === 'none') return { mode: 'none', inventory: null, areasDir: null, spec: null,
      snapshotHash: null, committedGenerationId: null, environmentId: null, invalid: false }
    if (current.mode === 'generation') {
      const areaFiles = Object.entries(current.artifacts)
        .filter(([role, file]) => role.startsWith('area:') && file).map(([, file]) => file)
      return { mode: 'generation', inventory: current.artifacts.inventory,
        areasDir: areaFiles.length ? dirname(areaFiles[0]) : join(dirname(current.artifacts.inventory), 'areas'),
        spec: current.artifacts['normalized-spec'] || null, snapshotHash: current.snapshotHash,
        committedGenerationId: current.manifest.generationId,
        environmentId: current.environmentId, invalid: false }
    }
    return { mode: 'invalid', inventory: null, areasDir: null, spec: null,
      snapshotHash: null, committedGenerationId: null, environmentId: null,
      invalid: true, error: 'generation-mode-invalid' }
  } catch {
    return { mode: 'invalid', inventory: null, areasDir: null, spec: null, snapshotHash: null,
      committedGenerationId: null, environmentId: null, invalid: true,
      error: 'generation-reader-unavailable' }
  }
}
