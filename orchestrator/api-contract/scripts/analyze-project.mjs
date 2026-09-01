// Deterministic, generation-bound API implementation/consumer analyzer.
// It never calls a backend and never executes project code. The only outputs
// are bounded runtime reports under the current control/task report scope.

import { createHash } from 'node:crypto'
import { createRequire } from 'node:module'
import { dirname, extname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import {
  EXECUTION_ROOT, EXECUTION_SCOPE, PROJECT_ROOT as CONTROL_ROOT,
  REPORTS_DIR, writeContractReport,
} from './_util.mjs'
import {
  isTestSourcePath, maskKotlinComments, maskKotlinNonCode, normalizeKotlinRoute, normalizeRoute, parseKotlinAnnotationCandidates,
  parseKotlinClientCandidates,
} from './kotlin-routes.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const SIDECAR_DIR = resolve(HERE, '..')
const CODE_PROJECT_ROOT = resolve(SIDECAR_DIR, '..', '..')
const require = createRequire(import.meta.url)
const generation = require(join(CODE_PROJECT_ROOT, 'orchestrator', 'site', 'server', 'contract-generation.js'))
const fileGuards = require(join(CODE_PROJECT_ROOT, 'orchestrator', 'site', 'server', 'file-guards.js'))
const projectInputs = require(join(CODE_PROJECT_ROOT, 'orchestrator', 'site', 'server', 'api-project-inputs.js'))
const architectureContract = require(join(
  CODE_PROJECT_ROOT, 'orchestrator', 'site', 'server', 'architecture-contract.js',
))

export const ANALYZER_VERSION = projectInputs.ANALYZER_VERSION
const REPORT_MAX = 16 * 1024 * 1024
const CANDIDATE_MAX = 200000
const CANDIDATES_PER_SOURCE_MAX = 21
const HASH_RE = /^sha256:[a-f0-9]{64}$/
const METHOD_NAMES = Object.freeze({
  get: 'GET', post: 'POST', put: 'PUT', patch: 'PATCH', delete: 'DELETE',
})

function sha(value) {
  return 'sha256:' + createHash('sha256')
    .update(Buffer.isBuffer(value) ? value : Buffer.from(String(value), 'utf8')).digest('hex')
}

function fail(code, detail) {
  const error = new Error(detail || code)
  error.code = code
  throw error
}

function routeMatches(left, right) {
  return normalizeRoute(left) === normalizeRoute(right)
}

function isTestSource(sourcePath) {
  return isTestSourcePath(sourcePath)
}

function maskComments(text) {
  let out = ''
  let state = 'code'
  for (let index = 0; index < text.length; index++) {
    const char = text[index]
    const next = text[index + 1]
    if (state === 'line') {
      if (char === '\n' || char === '\r') {
        state = 'code'
        out += char
      } else out += ' '
      continue
    }
    if (state === 'block') {
      if (char === '*' && next === '/') {
        out += '  '
        index++
        state = 'code'
      } else out += char === '\n' || char === '\r' ? char : ' '
      continue
    }
    if (state === 'single' || state === 'double' || state === 'template') {
      out += char
      if (char === '\\' && index + 1 < text.length) {
        out += text[++index]
      } else if ((state === 'single' && char === "'") ||
          (state === 'double' && char === '"') ||
          (state === 'template' && char === '`')) {
        state = 'code'
      }
      continue
    }
    if (char === '/' && next === '/') {
      out += '  '
      index++
      state = 'line'
    } else if (char === '/' && next === '*') {
      out += '  '
      index++
      state = 'block'
    } else {
      out += char
      if (char === "'") state = 'single'
      else if (char === '"') state = 'double'
      else if (char === '`') state = 'template'
    }
  }
  return out
}

function analysisText(record) {
  if (['.kt', '.kts'].includes(extname(record.path).toLowerCase())) {
    return maskKotlinComments(record.text)
  }
  return typeof record.code === 'string' ? record.code : maskComments(record.text)
}

function genericCodeOnly(text) {
  let out = ''
  let state = 'code'
  for (let index = 0; index < text.length; index++) {
    const char = text[index], next = text[index + 1]
    if (state === 'line') {
      if (char === '\n' || char === '\r') { state = 'code'; out += char } else out += ' '
    } else if (state === 'block') {
      if (char === '*' && next === '/') { out += '  '; index++; state = 'code' }
      else out += char === '\n' || char === '\r' ? char : ' '
    } else if (state !== 'code') {
      out += char === '\n' || char === '\r' ? char : ' '
      if (char === '\\' && index + 1 < text.length) {
        out += text[index + 1] === '\n' || text[index + 1] === '\r' ? text[index + 1] : ' '
        index++
      } else if (char === state) state = 'code'
    } else if (char === '/' && next === '/') {
      out += '  '; index++; state = 'line'
    } else if (char === '/' && next === '*') {
      out += '  '; index++; state = 'block'
    } else if (char === '"' || char === "'" || char === '`') {
      out += ' '; state = char
    } else out += char
  }
  return out
}

function codeOnlyText(record) {
  return ['.kt', '.kts'].includes(extname(record.path).toLowerCase())
    ? maskKotlinNonCode(record.text) : genericCodeOnly(record.text)
}

function nearbySymbol(text, offset) {
  const start = Math.max(0, offset - 800)
  const before = text.slice(start, offset)
  const patterns = [
    /(?:suspend\s+)?fun\s+([A-Za-z_][A-Za-z0-9_]*)\s*\([^)]*$/s,
    /(?:async\s+)?function\s+([A-Za-z_$][A-Za-z0-9_$]*)\s*\([^)]*$/s,
    /(?:const|let|var)\s+([A-Za-z_$][A-Za-z0-9_$]*)\s*=\s*(?:async\s*)?\([^)]*$/s,
    /(?:public|private|internal|protected)?\s*(?:async\s+)?[A-Za-z0-9_<>,?.\[\] ]+\s+([A-Za-z_][A-Za-z0-9_]*)\s*\([^)]*$/s,
  ]
  for (const pattern of patterns) {
    const match = pattern.exec(before)
    if (match) return match[1]
  }
  let nearest = null
  for (const pattern of [
    /(?:suspend\s+)?fun\s+([A-Za-z_][A-Za-z0-9_]*)\s*\(/g,
    /(?:async\s+)?function\s+([A-Za-z_$][A-Za-z0-9_$]*)\s*\(/g,
    /(?:const|let|var)\s+([A-Za-z_$][A-Za-z0-9_$]*)\s*=\s*(?:async\s*)?(?:\([^)]*\)|[A-Za-z_$][A-Za-z0-9_$]*)\s*=>/g,
    /(?:^|\n)\s*(?:(?:public|private|internal|protected|static|final|open|override)\s+)*(?:async\s+)?[A-Za-z0-9_<>,?.\[\] ]+\s+([A-Za-z_][A-Za-z0-9_]*)\s*\(/gm,
  ]) {
    let match
    while ((match = pattern.exec(before))) {
      if (!nearest || match.index > nearest.index) {
        nearest = { index: match.index, symbol: match[1] }
      }
    }
  }
  if (nearest) return nearest.symbol
  const after = text.slice(offset, Math.min(text.length, offset + 500))
  for (const pattern of [
    /(?:suspend\s+)?fun\s+([A-Za-z_][A-Za-z0-9_]*)\s*\(/,
    /(?:async\s+)?function\s+([A-Za-z_$][A-Za-z0-9_$]*)\s*\(/,
    /(?:public|private|internal|protected)?\s*(?:async\s+)?[A-Za-z0-9_<>,?.\[\] ]+\s+([A-Za-z_][A-Za-z0-9_]*)\s*\(/,
  ]) {
    const match = pattern.exec(after)
    if (match) return match[1]
  }
  return null
}

function routeCandidates(record) {
  const candidates = []
  const source = analysisText(record)
  const symbolSource = codeOnlyText(record)
  const kotlin = ['.kt', '.kts'].includes(extname(record.path).toLowerCase())
  const add = (method, route, index, evidence, normalized) => {
    if (!METHOD_NAMES[String(method).toLowerCase()] || typeof route !== 'string') return
    candidates.push({
      method: METHOD_NAMES[String(method).toLowerCase()],
      route: normalized ? normalizeRoute(route)
        : kotlin ? normalizeKotlinRoute(route) : normalizeRoute(route),
      file: record.path,
      symbol: nearbySymbol(symbolSource, index),
      confidence: 'exact',
      evidence,
    })
  }
  if (kotlin) {
    for (const candidate of parseKotlinClientCandidates(record.text)) {
      add(candidate.method, candidate.path, candidate.index,
        candidate.kind === 'request' ? 'typed request method and path' : 'typed HTTP client call', true)
    }
    for (const candidate of parseKotlinAnnotationCandidates(record.text)) {
      add(candidate.method, candidate.path, candidate.index, 'typed route annotation', true)
    }
    return candidates
  }
  const patterns = [
    {
      regex: /@(GET|POST|PUT|PATCH|DELETE)\s*\(\s*["']([^"']+)["']/gi,
      apply: (match) => add(match[1], match[2], match.index, 'typed route annotation'),
    },
    {
      regex: /@(Get|Post|Put|Patch|Delete)Mapping\s*\(\s*(?:value\s*=\s*)?["']([^"']+)["']/gi,
      apply: (match) => add(match[1], match[2], match.index, 'typed route annotation'),
    },
    {
      regex: /\[(?:Http)?(Get|Post|Put|Patch|Delete)\s*\(\s*["']([^"']+)["']/gi,
      apply: (match) => add(match[1], match[2], match.index, 'typed route annotation'),
    },
    {
      regex: /\b(get|post|put|patch|delete)\s*\(\s*["']([^"']+)["']/gi,
      apply: (match) => add(match[1], match[2], match.index, 'literal route call'),
    },
    {
      regex: /\b(?:axios|client|http)\s*\.\s*(get|post|put|patch|delete)\s*\(\s*["']([^"']+)["']/gi,
      apply: (match) => add(match[1], match[2], match.index, 'typed HTTP client call'),
    },
  ]
  for (const pattern of patterns) {
    let match
    while ((match = pattern.regex.exec(source))) pattern.apply(match)
  }
  const fetchPattern = /\bfetch\s*\(\s*["']([^"']+)["']\s*(?:,\s*\{([\s\S]{0,500}?)\})?/gi
  let fetchMatch
  while ((fetchMatch = fetchPattern.exec(source))) {
    const method = /method\s*:\s*["'](GET|POST|PUT|PATCH|DELETE)["']/i.exec(fetchMatch[2] || '')
    add(method ? method[1] : 'GET', fetchMatch[1], fetchMatch.index, 'literal fetch request')
  }
  return candidates
}

function wordPresent(text, value) {
  if (!value || !/^[A-Za-z_$][A-Za-z0-9_$.-]{0,199}$/.test(value)) return false
  const escaped = value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return new RegExp(`(^|[^A-Za-z0-9_$.-])${escaped}([^A-Za-z0-9_$.-]|$)`).test(text)
}

function candidateKey(candidate) {
  return `${candidate.file}\u0000${candidate.symbol || ''}\u0000${candidate.confidence}`
}

function evidenceIndexes(records, endpoints) {
  const operationTargets = new Set(endpoints.map((endpoint) => endpoint.operationId))
  const routeTargets = new Set(endpoints.map((endpoint) => normalizeRoute(endpoint.path)))
  const operations = Object.create(null)
  const paths = Object.create(null)
  let count = 0
  let truncated = false
  const add = (target, key, candidate) => {
    if (count >= CANDIDATE_MAX) {
      truncated = true
      return
    }
    if (!target[key]) target[key] = []
    if (target[key].length >= CANDIDATES_PER_SOURCE_MAX) {
      truncated = true
      return
    }
    target[key].push(candidate)
    count++
  }
  for (const record of records) {
    const source = analysisText(record)
    const symbolSource = codeOnlyText(record)
    const words = /[A-Za-z_$][A-Za-z0-9_$.-]{0,199}/g
    let match
    while ((match = words.exec(source))) {
      if (!operationTargets.has(match[0])) continue
      add(operations, match[0], {
        file: record.path,
        symbol: nearbySymbol(symbolSource, match.index),
        confidence: 'derived',
        evidence: 'stable operation id reference',
      })
    }
    const strings = /["']([^"'\r\n]{1,2000})["']/g
    while ((match = strings.exec(source))) {
      const value = match[1]
      if (value.charAt(0) !== '/' && !/^https?:\/\//i.test(value)) continue
      const normalized = ['.kt', '.kts'].includes(extname(record.path).toLowerCase())
        ? normalizeKotlinRoute(value) : normalizeRoute(value)
      if (!routeTargets.has(normalized)) continue
      add(paths, normalized, {
        file: record.path,
        symbol: nearbySymbol(symbolSource, match.index),
        confidence: 'heuristic',
        evidence: 'path literal without a proven method binding',
      })
    }
  }
  return { operations, paths, truncated }
}

function exactRouteIndex(routes) {
  const index = Object.create(null)
  let truncated = false
  for (const candidate of routes) {
    const key = `${candidate.method}\u0000${candidate.route}`
    if (!index[key]) index[key] = []
    if (index[key].length >= CANDIDATES_PER_SOURCE_MAX) {
      truncated = true
      continue
    }
    index[key].push(candidate)
  }
  return { index, truncated }
}

function implementationFor(endpoint, records, routes, indexes) {
  let candidates = indexes && indexes.routes
    ? (indexes.routes[`${endpoint.method}\u0000${normalizeRoute(endpoint.path)}`] || [])
    : routes.filter((candidate) =>
      candidate.method === endpoint.method && routeMatches(candidate.route, endpoint.path))
  if (!candidates.length) {
    candidates = indexes
      ? (indexes.operations[endpoint.operationId] || [])
      : records.map((record) => ({
        record,
        source: analysisText(record),
        symbolSource: codeOnlyText(record),
      })).filter(({ source }) => wordPresent(source, endpoint.operationId))
        .map(({ record, source, symbolSource }) => ({
          file: record.path,
          symbol: nearbySymbol(symbolSource, source.indexOf(endpoint.operationId)),
        confidence: 'derived',
        evidence: 'stable operation id reference',
      }))
  }
  if (!candidates.length) {
    candidates = indexes
      ? (indexes.paths[normalizeRoute(endpoint.path)] || [])
      : records.map((record) => ({
        record,
        source: analysisText(record),
        symbolSource: codeOnlyText(record),
      })).filter(({ source }) => source.includes(endpoint.path))
        .map(({ record, source, symbolSource }) => ({
          file: record.path,
          symbol: nearbySymbol(symbolSource, source.indexOf(endpoint.path)),
        confidence: 'heuristic',
        evidence: 'path literal without a proven method binding',
      }))
  }
  const unique = []
  const seen = new Set()
  for (const candidate of candidates) {
    const key = candidateKey(candidate)
    if (!seen.has(key)) {
      seen.add(key)
      unique.push(candidate)
    }
  }
  unique.sort((left, right) => left.file.localeCompare(right.file) ||
    String(left.symbol || '').localeCompare(String(right.symbol || '')))
  if (!unique.length) {
    return {
      operationId: endpoint.operationId,
      state: 'unknown',
      file: null,
      symbol: null,
      confidence: null,
      evidence: [],
      candidates: [],
    }
  }
  const best = unique[0]
  const confirmed = unique.length === 1 && (
    best.confidence === 'exact' ||
    best.confidence === 'derived' && best.symbol === endpoint.operationId
  )
  return {
    operationId: endpoint.operationId,
    state: confirmed ? 'implemented' : 'partial',
    file: best.file,
    symbol: best.symbol,
    confidence: best.confidence,
    evidence: [best.evidence].filter(Boolean),
    candidates: unique.slice(0, 20).map((candidate) => ({
      file: candidate.file,
      symbol: candidate.symbol,
      confidence: candidate.confidence,
    })),
  }
}

function architectureSnapshot() {
  const file = join(CONTROL_ROOT, 'orchestrator', '.arch-map.json')
  try {
    const hit = fileGuards.boundedRegularFileUnder(
      CONTROL_ROOT, join(CONTROL_ROOT, 'orchestrator'), file,
      architectureContract.MAX_MAP_BYTES,
    )
    if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') {
      return { present: false, hash: null, map: null }
    }
    const parsed = architectureContract.parse(hit.bytes)
    return {
      present: true,
      hash: parsed.map.structuralHash,
      map: parsed.map,
    }
  } catch (error) {
    if (error && error.code === 'ENOENT') return { present: false, hash: null, map: null }
    return { present: false, hash: null, map: null, invalid: true }
  }
}

function architectureNode(map, sourcePath) {
  const matches = map.nodes.filter((node) => node.path === sourcePath)
  return matches.length === 1 ? matches[0].id : null
}

function consumersFor(mapping, records, architecture) {
  if (!mapping.file || !mapping.symbol) return []
  const escaped = mapping.symbol.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const call = new RegExp(`(?:\\.|\\b)${escaped}\\s*\\(`)
  const consumers = []
  for (const record of records) {
    const source = codeOnlyText(record)
    if (record.path === mapping.file || !call.test(source)) continue
    const symbol = nearbySymbol(source, source.search(call))
    const archId = architecture.present ? architectureNode(architecture.map, record.path) : null
    const idSeed = `${mapping.operationId}\u0000${record.path}\u0000${symbol || ''}\u0000${archId || ''}`
    consumers.push({
      id: 'consumer-' + sha(idSeed).slice('sha256:'.length, 'sha256:'.length + 24),
      architectureId: archId,
      file: record.path,
      symbol,
      kind: archId ? 'architecture-node' : 'source-reference',
    })
    if (consumers.length >= 500) break
  }
  return consumers
}

function consumerEvidenceIndex(records, mappings, architecture) {
  const mappingsBySymbol = new Map()
  for (const mapping of mappings) {
    if (!mapping.file || !/^[A-Za-z_$][A-Za-z0-9_$]*$/.test(mapping.symbol || '')) continue
    const rows = mappingsBySymbol.get(mapping.symbol) || []
    rows.push(mapping)
    mappingsBySymbol.set(mapping.symbol, rows)
  }
  const byOperation = Object.create(null)
  const seenByOperation = Object.create(null)
  const architectureByFile = new Map()
  let count = 0
  let truncated = false
  for (const record of records) {
    const source = codeOnlyText(record)
    const calls = /(?:\.|\b)([A-Za-z_$][A-Za-z0-9_$]*)\s*\(/g
    let match
    while ((match = calls.exec(source))) {
      const targets = mappingsBySymbol.get(match[1])
      if (!targets) continue
      const symbol = nearbySymbol(source, match.index)
      let architectureId = null
      if (architecture.present) {
        if (!architectureByFile.has(record.path)) {
          architectureByFile.set(
            record.path,
            architectureNode(architecture.map, record.path),
          )
        }
        architectureId = architectureByFile.get(record.path)
      }
      for (const mapping of targets) {
        if (mapping.file === record.path) continue
        const operationId = mapping.operationId
        const identity = `${record.path}\u0000${symbol || ''}\u0000${architectureId || ''}`
        const seen = seenByOperation[operationId] ||
          (seenByOperation[operationId] = new Set())
        if (seen.has(identity)) continue
        if (count >= CANDIDATE_MAX ||
            (byOperation[operationId] || []).length >= 500) {
          truncated = true
          continue
        }
        seen.add(identity)
        const rows = byOperation[operationId] || (byOperation[operationId] = [])
        const idSeed = `${operationId}\u0000${identity}`
        rows.push({
          id: 'consumer-' + sha(idSeed).slice('sha256:'.length, 'sha256:'.length + 24),
          architectureId,
          file: record.path,
          symbol,
          kind: architectureId ? 'architecture-node' : 'source-reference',
        })
        count++
      }
    }
  }
  for (const rows of Object.values(byOperation)) {
    rows.sort((left, right) => left.file.localeCompare(right.file) ||
      String(left.symbol || '').localeCompare(String(right.symbol || '')))
  }
  return { byOperation, truncated }
}

function writeReport(name, value) {
  const file = join(REPORTS_DIR, name)
  const bytes = Buffer.from(JSON.stringify(value, null, 2) + '\n')
  if (bytes.length > REPORT_MAX) fail('analyzer-report-size-limit', name)
  try { writeContractReport(file, bytes) }
  catch (error) { fail('analyzer-report-write-failed', error.message) }
}

function analyzeProject() {
  const current = EXECUTION_SCOPE
    ? generation.currentAtProjectRoot(EXECUTION_ROOT, EXECUTION_SCOPE.apiGenerationHash)
    : generation.current()
  if (!current.ok) fail(current.error || 'generation-invalid')
  if (current.mode !== 'generation' || !current.inventory) fail('contract-missing')
  const scan = projectInputs.collect(EXECUTION_ROOT, { includeText: true })
  if (!scan.ok) fail(scan.error || 'analyzer-input-unavailable', scan.detail)
  const supportedRecords = scan.records.filter((record) =>
    projectInputs.SOURCE_EXTENSIONS[extname(record.path).toLowerCase()])
  const sourceRecords = supportedRecords.filter((record) => !isTestSource(record.path))
    .map((record) => ({ ...record, code: maskComments(record.text) }))
  const routes = []
  let candidatesTruncated = false
  for (const record of sourceRecords) {
    const next = routeCandidates(record)
    const remaining = CANDIDATE_MAX - routes.length
    if (next.length > remaining) candidatesTruncated = true
    routes.push(...next.slice(0, Math.max(0, remaining)))
  }
  const indexes = evidenceIndexes(sourceRecords, current.inventory.endpoints || [])
  if (indexes.truncated) candidatesTruncated = true
  const indexedRoutes = exactRouteIndex(routes)
  indexes.routes = indexedRoutes.index
  if (indexedRoutes.truncated) candidatesTruncated = true
  const mappings = (current.inventory.endpoints || []).map((endpoint) =>
    implementationFor(endpoint, sourceRecords, routes, indexes))
  const coverage = {
    total: mappings.length,
    implemented: mappings.filter((row) => row.state === 'implemented').length,
    partial: mappings.filter((row) => row.state === 'partial').length,
    unknown: mappings.filter((row) => row.state === 'unknown').length,
    analyzedFiles: sourceRecords.length,
  }
  const now = new Date().toISOString()
  const common = {
    schemaVersion: 1,
    analyzerVersion: ANALYZER_VERSION,
    committedGenerationId: current.manifest.generationId,
    contractHash: current.snapshotHash,
    environmentId: current.environmentId,
    projectCodeRevision: scan.projectCodeRevision,
    generatedAt: now,
  }
  const implementation = {
    ...common,
    analysisStatus: 'partial',
    coverage,
    receipt: {
      fileCount: scan.receipt.files.length,
      totalBytes: scan.totalBytes,
      directoryCount: scan.directories,
      files: scan.receipt.files,
    },
    operations: mappings.map((row) => ({
      operationId: row.operationId,
      state: row.state,
      file: row.file,
      symbol: row.symbol,
      confidence: row.confidence,
      evidence: row.evidence,
    })),
    unresolved: mappings.filter((row) => row.state === 'partial').map((row) => ({
      operationId: row.operationId,
      reason: row.candidates.length > 1 ? 'ambiguous-mapping' : 'heuristic-only',
      candidates: row.candidates,
    })),
    limitations: [
      'static-implementation-analysis-not-conclusive',
      ...(supportedRecords.length > sourceRecords.length ? ['test-sources-excluded'] : []),
      ...(candidatesTruncated ? ['static-analysis-candidate-cap'] : []),
    ],
  }
  const architecture = architectureSnapshot()
  const consumerIndex = consumerEvidenceIndex(sourceRecords, mappings, architecture)
  const consumerOperations = mappings.map((mapping) => ({
    operationId: mapping.operationId,
    analysisStatus: mapping.state === 'implemented'
      ? 'partial'
      : 'not-checked',
    consumers: consumerIndex.byOperation[mapping.operationId] || [],
  }))
  const consumers = {
    ...common,
    architectureStructuralHash: architecture.hash,
    analysisStatus: 'partial',
    operations: consumerOperations,
    limitations: [
      'static-consumer-analysis-not-conclusive',
      ...(!architecture.present
        ? [architecture.invalid ? 'architecture-map-invalid' : 'architecture-map-missing']
        : []),
      ...(consumerIndex.truncated ? ['static-analysis-candidate-cap'] : []),
    ],
  }
  writeReport('implementation-map.json', implementation)
  writeReport('consumer-map.json', consumers)
  return { implementation, consumers }
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  if (process.argv.length !== 2) {
    process.stderr.write('contract:analyze accepts no arguments\n')
    process.exit(2)
  }
  try {
    const result = analyzeProject()
    process.stdout.write(JSON.stringify({
      ok: true,
      committedGenerationId: result.implementation.committedGenerationId,
      contractHash: result.implementation.contractHash,
      projectCodeRevision: result.implementation.projectCodeRevision,
      coverage: result.implementation.coverage,
      limitations: result.consumers.limitations,
    }) + '\n')
  } catch (error) {
    process.stderr.write(String(error && error.code || 'analyzer-failed') + '\n')
    process.exit(1)
  }
}

export const _test = {
  normalizeRoute,
  routeMatches,
  isTestSource,
  nearbySymbol,
  routeCandidates,
  implementationFor,
  evidenceIndexes,
  exactRouteIndex,
  consumersFor,
  consumerEvidenceIndex,
  maskComments,
  codeOnlyText,
  sha,
  HASH_RE,
}
