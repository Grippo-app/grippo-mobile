import { createHash } from 'node:crypto'
import { existsSync, readFileSync, readdirSync, realpathSync, statSync } from 'node:fs'
import { join, resolve, sep } from 'node:path'
import { deriveResourceRoots, localeValueDirs } from './design-locale.mjs'

export const CAPTURE_CONFIG_DISCOVERY_KEY = 'virtual:capture-config-discovery-v1'

const SCAN_SKIP_DIRS = new Set(['.git', '.gradle', '.idea', 'build', 'node_modules', 'orchestrator'])
const isTestDirSeg = (name) => /Test$/.test(name) || name === 'test'
const normalizedPath = (path) => resolve(path).split(sep).join('/')
const sha256 = (value) => `sha256:${createHash('sha256').update(value).digest('hex')}`
const physicalPath = (path) => {
  try { return realpathSync(path) } catch { return resolve(path) }
}
const uniquePaths = (paths) => {
  const byPhysical = new Map()
  for (const path of paths.filter(Boolean).map((value) => resolve(value))) {
    const identity = physicalPath(path)
    if (!byPhysical.has(identity)) byPhysical.set(identity, path)
  }
  return [...byPhysical.values()]
}

function discoveryError(path, error) {
  return { path: normalizedPath(path), code: String(error && error.code || 'READ_FAILED') }
}

function collectTestFiles(root, out, inTest = false, errors = [], visited = new Set()) {
  const physicalRoot = physicalPath(root)
  if (visited.has(physicalRoot)) return out
  visited.add(physicalRoot)
  let entries = []
  try { entries = readdirSync(root, { withFileTypes: true }) } catch (error) { errors.push(discoveryError(root, error)); return out }
  for (const entry of entries) {
    const path = join(root, entry.name)
    if (SCAN_SKIP_DIRS.has(entry.name)) continue
    let isDirectory = entry.isDirectory()
    let isFile = entry.isFile()
    if (entry.isSymbolicLink()) {
      try {
        const target = statSync(path)
        isDirectory = target.isDirectory()
        isFile = target.isFile()
      } catch (error) { errors.push(discoveryError(path, error)); continue }
    }
    if (isDirectory) collectTestFiles(path, out, inTest || isTestDirSeg(entry.name), errors, visited)
    else if (isFile && inTest && /\.kt$/.test(entry.name)) out.push(resolve(path))
  }
  return out
}

function fileState(path, errors) {
  if (!existsSync(path)) return null
  try { return sha256(readFileSync(path)) } catch (error) { errors.push(discoveryError(path, error)); return 'unreadable' }
}

// Hashes the SET of inputs that discovery can start consulting, not just the files it already
// selected. Non-capture Kotlin edits stay quiet, while adding/removing captureRoboImage changes
// the digest. Resource candidates include missing expected strings.xml paths, so adding a new
// locale file or composeResources/src/main/res root also invalidates the prior report.
export function captureConfigDiscovery({ codeRoots = [], screensDir = '', supportedLocales = [] } = {}) {
  const roots = uniquePaths(codeRoots).sort()
  const rawErrors = []
  const visited = new Set()
  const files = uniquePaths(roots.flatMap((root) => collectTestFiles(root, [], false, rawErrors, visited))).sort()
  const captureFiles = []
  const tests = files.map((file) => {
    let raw = null
    try { raw = readFileSync(file, 'utf8') } catch (error) { rawErrors.push(discoveryError(file, error)) }
    const captures = typeof raw === 'string' && raw.includes('captureRoboImage')
    if (captures) captureFiles.push(file)
    return [normalizedPath(file), captures ? sha256(raw) : raw == null ? 'unreadable' : 'no-capture']
  })

  const resourceRoots = uniquePaths(deriveResourceRoots(roots)).sort()
  const locales = [...new Set(supportedLocales.filter(Boolean).map(String))]
  const resources = []
  for (let index = 0; index < locales.length; index++) {
    for (const root of resourceRoots) {
      for (const dir of localeValueDirs(locales[index], index === 0)) {
        const file = join(root, dir, 'strings.xml')
        resources.push([normalizedPath(file), fileState(file, rawErrors)])
      }
    }
  }
  resources.sort((a, b) => a[0].localeCompare(b[0]))

  const resolvedScreensDir = screensDir ? resolve(screensDir) : ''
  const identity = resolvedScreensDir
    ? ['index.json', 'bindings.json'].map((name) => {
        const file = join(resolvedScreensDir, name)
        return [normalizedPath(file), fileState(file, rawErrors)]
      })
    : []
  const errors = [...new Map(rawErrors.map((error) => [`${error.path}\0${error.code}`, error])).values()]
    .sort((a, b) => `${a.path}\0${a.code}`.localeCompare(`${b.path}\0${b.code}`))
  const payload = { version: 1, roots: roots.map(normalizedPath), tests, resourceRoots: resourceRoots.map(normalizedPath), locales, resources, identity }
  if (errors.length) payload.errors = errors
  return {
    version: 1,
    digest: sha256(JSON.stringify(payload)),
    roots,
    screensDir: resolvedScreensDir,
    files,
    captureFiles,
    resourceRoots,
    errors,
  }
}

export function captureConfigScopeOmissions(effective, canonical) {
  const effectiveCaptures = new Set((effective && effective.captureFiles || []).map(physicalPath))
  const effectiveResources = new Set((effective && effective.resourceRoots || []).map(physicalPath))
  return {
    captureFiles: (canonical && canonical.captureFiles || []).filter((path) => !effectiveCaptures.has(physicalPath(path))),
    resourceRoots: (canonical && canonical.resourceRoots || []).filter((path) => !effectiveResources.has(physicalPath(path))),
  }
}
