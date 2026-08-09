// Typed Backend integration sidecar. Reads one exact JSON request from stdin;
// the browser can never supply a URL, command, prompt or path. This process is
// the only component that performs network I/O. Probe writes runtime reports
// only. Refresh re-fetches, revalidates and publishes an atomic generation.

import { createHash, randomBytes } from 'node:crypto'
import { promises as dns } from 'node:dns'
import {
  chmodSync, copyFileSync, existsSync, lstatSync, mkdirSync, readFileSync,
  readdirSync, rmSync, writeFileSync,
} from 'node:fs'
import http from 'node:http'
import https from 'node:https'
import { spawn, spawnSync } from 'node:child_process'
import { isIP } from 'node:net'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { createRequire } from 'node:module'
import YAML from 'yaml'
import { classifyChanges } from './change-classifier.mjs'
import { postmanUrlInfo, runDiscovery } from './resolve-source.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const SIDECAR_DIR = resolve(HERE, '..')
const PROJECT_ROOT = resolve(process.env.ORCHESTRATOR_PROJECT_ROOT || resolve(SIDECAR_DIR, '..', '..'))
const SITE_SERVER_DIR = join(SIDECAR_DIR, '..', 'site', 'server')
const require = createRequire(import.meta.url)
const paths = require(join(SITE_SERVER_DIR, 'paths.js'))
const environmentStore = require(join(SITE_SERVER_DIR, 'backend-environments.js'))
const credentials = require(join(SITE_SERVER_DIR, 'backend-credentials.js'))
const generation = require(join(SITE_SERVER_DIR, 'contract-generation.js'))
const history = require(join(SITE_SERVER_DIR, 'contract-history.js'))
const persistence = require(join(SITE_SERVER_DIR, 'persistence.js'))
const fileGuards = require(join(SITE_SERVER_DIR, 'file-guards.js'))
const finalizations = require(join(SITE_SERVER_DIR, 'finalizations.js'))
const creationMarkers = require(join(SITE_SERVER_DIR, 'creation-markers.js'))
const editMarkers = require(join(SITE_SERVER_DIR, 'edit-markers.js'))
const writerLeases = require(join(SIDECAR_DIR, '..', 'tasks', 'writer-leases.cjs'))

const JOB_RE = history.JOB_RE
const HASH_RE = /^sha256:[0-9a-f]{64}$/
const MAX_INPUT = 64 * 1024
const MAX_SOURCE = 10 * 1024 * 1024
const MAX_DISCOVERY_BODY = 2 * 1024 * 1024
const MAX_DISCOVERY_REQUESTS = 8
const MAX_REDIRECTS = 3
const TOTAL_TIMEOUT_MS = 60 * 1000
const PREVIEW_TTL_MS = 5 * 60 * 1000
const NORMALIZER_VERSION = 'openapi-normalize-v1'
const POSTMAN_VERSION = 'postman-sanitizer-v1'
const POSTMAN_API_HOST = 'api.getpostman.com'

if (process.argv.length !== 2) {
  process.stderr.write('backend-action accepts no arguments; provide one typed request on stdin\n')
  process.exit(2)
}

function sha(value) { return 'sha256:' + createHash('sha256').update(value).digest('hex') }
function exactKeys(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0')
}
function stable(value) {
  if (Array.isArray(value)) return value.map(stable)
  if (!value || typeof value !== 'object') return value
  return Object.fromEntries(Object.keys(value).sort().map((key) => [key, stable(value[key])]))
}
function progress(phase, detail = null) {
  process.stdout.write(JSON.stringify({ type: 'progress', phase, detail }) + '\n')
}
function typedError(code, message) {
  const error = new Error(message || code)
  error.backendCode = code
  return error
}
function safeMessage(error) {
  const code = error && error.backendCode || 'sidecar-failed'
  const messages = {
    'auth-missing': 'Authentication is required.',
    'auth-invalid': 'The stored credential is invalid.',
    'auth-rejected': 'The backend rejected the credential.',
    'source-unreachable': 'The source is unavailable.',
    'source-content-type': 'The source returned an unsupported content type.',
    'source-too-large': 'The source exceeds the 10 MB limit.',
    'invalid-openapi': 'The source is not a valid OpenAPI 3.0/3.1 contract.',
    'invalid-postman': 'The source is not a valid Postman Collection v2.1.',
    'candidate-invalid': 'The generated contract candidate violates the current schemas.',
    'preview-stale': 'The tested source or credential is no longer current.',
    'source-changed': 'The source changed after the last test.',
    'write-conflict': 'Contract files changed during refresh.',
    'snapshot-invalid': 'The saved contract generation is invalid.',
    'writer-lease-conflict': 'Another workspace writer is active.',
    'environment-revision-conflict': 'Source settings changed during the job.',
    'auth-revision-conflict': 'The credential changed during the job.',
    'enrichment-unavailable': 'Postman enrichment is unavailable.',
  }
  return { code, message: messages[code] || 'The contract action failed safely.' }
}

function atomicPrivateWrite(file, value, maxBytes = 512 * 1024) {
  const bytes = Buffer.isBuffer(value) ? value : Buffer.from(String(value))
  if (bytes.length > maxBytes) throw typedError('report-size-limit')
  const published = fileGuards.atomicReplaceRegularFileResult(PROJECT_ROOT, dirname(file), file, bytes,
    { create: true, directoryMode: 0o700, mode: 0o600, maxBytes })
  if (!published.ok) throw typedError('write-conflict')
}

async function readInput() {
  const chunks = []
  let bytes = 0
  for await (const chunk of process.stdin) {
    bytes += chunk.length
    if (bytes > MAX_INPUT) throw typedError('request-too-large')
    chunks.push(chunk)
  }
  let input
  try { input = JSON.parse(Buffer.concat(chunks).toString('utf8')) } catch { throw typedError('bad-request') }
  const common = ['action', 'authRevision', 'environmentId', 'environmentRevision', 'jobId', 'schemaVersion', 'selectionRevision']
  const probe = common
  const refresh = common.concat(['acknowledgements', 'expectedSnapshotHash', 'expectedSourceFingerprint', 'previewId'])
  const expected = input && input.action === 'contract:probe' ? probe : refresh
  if (!exactKeys(input, expected) || input.schemaVersion !== 1 || !JOB_RE.test(String(input.jobId || '')) ||
      !['local', 'dev', 'stage', 'prod'].includes(input.environmentId) || !HASH_RE.test(String(input.environmentRevision || '')) ||
      !Number.isSafeInteger(input.authRevision) || input.authRevision < 0 ||
      !Number.isSafeInteger(input.selectionRevision) || input.selectionRevision < 0 ||
      !['contract:probe', 'contract:refresh-openapi', 'contract:refresh-postman'].includes(input.action)) throw typedError('bad-request')
  if (input.action !== 'contract:probe') {
    if (!history.PREVIEW_RE.test(String(input.previewId || '')) || !HASH_RE.test(String(input.expectedSourceFingerprint || '')) ||
        (input.expectedSnapshotHash !== null && !HASH_RE.test(String(input.expectedSnapshotHash || ''))) ||
        !Array.isArray(input.acknowledgements) || input.acknowledgements.length > 10 ||
        input.acknowledgements.some((code) => !/^[a-z][a-z0-9-]{0,63}$/.test(String(code)))) throw typedError('bad-request')
  }
  return input
}

function ipv4Private(address) {
  const parts = String(address).split('.').map(Number)
  if (parts.length !== 4 || parts.some((part) => !Number.isInteger(part) || part < 0 || part > 255)) return true
  const [a, b] = parts
  return a === 0 || a === 10 || a === 127 || a >= 224 ||
    (a === 100 && b >= 64 && b <= 127) || (a === 169 && b === 254) ||
    (a === 172 && b >= 16 && b <= 31) || (a === 192 && (b === 0 || b === 168)) ||
    (a === 198 && (b === 18 || b === 19 || b === 51)) || (a === 203 && b === 0)
}
function ipLoopback(address) {
  const text = String(address).toLowerCase()
  return text === '::1' || text === '0:0:0:0:0:0:0:1' || /^127\./.test(text)
}
function ipPrivate(address, family) {
  const text = String(address).toLowerCase()
  if (family === 4 || /^\d+\.\d+\.\d+\.\d+$/.test(text)) return ipv4Private(text)
  if (text.startsWith('::ffff:')) return ipv4Private(text.slice(7))
  return text === '::' || text === '::1' || /^(fc|fd|fe[89ab]|ff)/.test(text) || text.startsWith('2001:db8:')
}
function boundedDnsLookup(hostname, remaining) {
  return new Promise((resolveLookup, rejectLookup) => {
    let settled = false
    let bytes = 0
    const chunks = []
    const source = "const dns=require('node:dns');dns.lookup(process.argv[1],{all:true,verbatim:true}," +
      "(error,addresses)=>{if(error)process.exit(1);process.stdout.write(JSON.stringify(addresses))})"
    const child = spawn(process.execPath, ['-e', source, hostname], {
      env: {},
      stdio: ['ignore', 'pipe', 'ignore'],
    })
    let timer = null
    const finish = (error, value) => {
      if (settled) return
      settled = true
      if (timer) clearTimeout(timer)
      if (error) rejectLookup(error)
      else resolveLookup(value)
    }
    timer = setTimeout(() => {
      child.kill('SIGKILL')
      const error = typedError('source-unreachable')
      error.discoveryDeadline = true
      finish(error)
    }, Math.max(1, remaining))
    child.once('error', () => finish(typedError('source-unreachable')))
    child.stdout.on('data', (chunk) => {
      bytes += chunk.length
      if (bytes > 16 * 1024) {
        child.kill('SIGKILL')
        finish(typedError('source-unreachable'))
        return
      }
      chunks.push(chunk)
    })
    child.once('close', (code) => {
      if (settled) return
      let addresses
      try { addresses = JSON.parse(Buffer.concat(chunks).toString('utf8')) } catch { return finish(typedError('source-unreachable')) }
      if (code !== 0 || !Array.isArray(addresses) || !addresses.length || addresses.some((row) =>
        !row || (row.family !== 4 && row.family !== 6) || isIP(String(row.address || '')) !== row.family)) {
        return finish(typedError('source-unreachable'))
      }
      finish(null, addresses)
    })
  })
}

async function assertNetworkTarget(url, environment, configuredHost, remaining = null) {
  if (url.host !== configuredHost) throw typedError('source-redirect-forbidden')
  let addresses
  const hostname = url.hostname.replace(/^\[|\]$/g, '')
  try {
    addresses = Number.isFinite(remaining)
      ? await boundedDnsLookup(hostname, remaining)
      : await dns.lookup(hostname, { all: true, verbatim: true })
  } catch (error) {
    throw error && error.backendCode ? error : typedError('source-unreachable')
  }
  if (!addresses.length) throw typedError('source-unreachable')
  if (environment.id === 'local') {
    if (!addresses.every((row) => ipLoopback(row.address))) throw typedError('source-network-forbidden')
  } else if (addresses.some((row) => ipPrivate(row.address, row.family))) throw typedError('source-network-forbidden')
  // The exact validated address is returned to the request lookup callback.
  // This closes the DNS-check/fetch race: the connection cannot re-resolve to
  // a different (possibly private) target after policy validation.
  return addresses[0]
}
function contentTypeAllowed(value, kind) {
  const type = String(value || '').split(';')[0].trim().toLowerCase()
  if (kind === 'discovery') return type === 'application/json' || type.endsWith('+json') ||
    ['application/yaml', 'application/x-yaml', 'text/yaml', 'text/x-yaml', 'text/html',
      'application/xhtml+xml', 'application/javascript', 'text/javascript'].includes(type)
  if (kind === 'postman') return type === 'application/json' || type.endsWith('+json')
  return type === 'application/json' || type.endsWith('+json') || ['application/yaml', 'application/x-yaml', 'text/yaml', 'text/x-yaml'].includes(type)
}
function credentialForTarget(url, credential, authKind) {
  return url.hostname.toLowerCase() === POSTMAN_API_HOST && authKind !== 'x-api-key' ? null : credential
}
function requestPinned(url, address, headers, kind, remaining, maxBytes = MAX_SOURCE) {
  return new Promise((resolveRequest, rejectRequest) => {
    const client = url.protocol === 'https:' ? https : http
    let settled = false
    let connectTimer = null
    let totalTimer = null
    const finish = (error, value) => {
      if (settled) return
      settled = true
      if (connectTimer) clearTimeout(connectTimer)
      if (totalTimer) clearTimeout(totalTimer)
      if (error) rejectRequest(error)
      else resolveRequest(value)
    }
    const request = client.request(url, {
      method: 'GET', headers,
      // We already selected and policy-checked one exact address above. Newer
      // Node versions otherwise request an `all: true` lookup result so they
      // can race address families, which conflicts with a single pinned IP.
      autoSelectFamily: false,
      lookup: (_hostname, _options, callback) => callback(null, address.address, address.family),
    }, (response) => {
      if (connectTimer) { clearTimeout(connectTimer); connectTimer = null }
      const status = response.statusCode || 0
      const responseHeaders = response.headers
      if ([301, 302, 303, 307, 308].includes(status) || status === 401 || status === 403 || status < 200 || status >= 300) {
        response.destroy()
        finish(null, { status, headers: responseHeaders, text: null })
        return
      }
      if (!contentTypeAllowed(responseHeaders['content-type'], kind)) {
        response.destroy()
        finish(typedError('source-content-type'))
        return
      }
      const declared = Number(responseHeaders['content-length'])
      if (Number.isFinite(declared) && declared > maxBytes) {
        response.destroy()
        finish(typedError('source-too-large'))
        return
      }
      const chunks = []
      let total = 0
      response.on('data', (chunk) => {
        total += chunk.length
        if (total > maxBytes) {
          response.destroy()
          finish(typedError('source-too-large'))
          return
        }
        chunks.push(chunk)
      })
      response.once('end', () => finish(null, { status, headers: responseHeaders, text: Buffer.concat(chunks).toString('utf8') }))
      response.once('error', () => finish(typedError('source-unreachable')))
    })
    request.once('socket', (socket) => {
      const readyEvent = url.protocol === 'https:' ? 'secureConnect' : 'connect'
      connectTimer = setTimeout(() => request.destroy(typedError('source-unreachable')), Math.min(10_000, remaining))
      socket.once(readyEvent, () => {
        if (connectTimer) { clearTimeout(connectTimer); connectTimer = null }
      })
    })
    request.once('error', (error) => finish(error && error.backendCode ? error : typedError('source-unreachable')))
    totalTimer = setTimeout(() => request.destroy(typedError('source-unreachable')), remaining)
    request.end()
  })
}
async function fetchDocument(rawUrl, environment, credential, kind, startedAt, authKind = environment.authKind) {
  if (credential && authKind === 'x-api-key' && !/^PMAK-/.test(credential)) throw typedError('auth-invalid')
  let current = new URL(rawUrl)
  const configuredHost = current.host
  const initialProtocol = current.protocol
  for (let redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
    const address = await assertNetworkTarget(current, environment, configuredHost)
    const remaining = TOTAL_TIMEOUT_MS - (Date.now() - startedAt)
    if (remaining <= 0) throw typedError('source-unreachable')
    const headers = { accept: kind === 'postman' ? 'application/json' : 'application/json, application/yaml, text/yaml' }
    const requestCredential = credentialForTarget(current, credential, authKind)
    if (requestCredential) {
      if (current.protocol !== 'https:') throw typedError('auth-invalid')
      if (authKind === 'x-api-key') headers['x-api-key'] = requestCredential
      else headers.authorization = `Bearer ${requestCredential}`
    }
    let response
    try { response = await requestPinned(current, address, headers, kind, remaining) }
    catch (error) { throw error && error.backendCode ? error : typedError('source-unreachable') }
    if ([301, 302, 303, 307, 308].includes(response.status)) {
      if (redirects === MAX_REDIRECTS) throw typedError('source-redirect-limit')
      const location = response.headers.location
      if (!location) throw typedError('source-redirect-forbidden')
      const next = new URL(location, current)
      if (initialProtocol === 'https:' && next.protocol !== 'https:') throw typedError('source-redirect-forbidden')
      if (!['http:', 'https:'].includes(next.protocol) || next.username || next.password || next.search || next.hash) throw typedError('source-redirect-forbidden')
      current = next
      continue
    }
    if (response.status === 401 || response.status === 403) throw typedError('auth-rejected')
    if (response.status < 200 || response.status >= 300) throw typedError('source-unreachable')
    return { text: response.text, contentType: response.headers['content-type'] || '' }
  }
  throw typedError('source-redirect-limit')
}

function discoveryFetcher(environment, credential, pinHost, deadlineAt) {
  let requestCount = 0
  const sourceProtocol = new URL(environment.sourceUrl).protocol
  return async function fetchDiscovery(rawUrl, options) {
    if (credential && environment.authKind === 'x-api-key' && !/^PMAK-/.test(credential)) throw typedError('auth-invalid')
    const kind = options && options.kind === 'openapi' ? 'openapi' :
      (options && options.kind === 'postman' ? 'postman' : 'discovery')
    const postmanListing = kind === 'postman'
    if (postmanListing && (environment.authKind !== 'x-api-key' || !credential || !/^PMAK-/.test(credential))) {
      throw typedError('auth-invalid')
    }
    const targetHost = postmanListing ? POSTMAN_API_HOST : pinHost
    const targetProtocol = postmanListing ? 'https:' : sourceProtocol
    let current = new URL(rawUrl)
    const redirectLimit = options && options.allowRedirects ? MAX_REDIRECTS : 0
    for (let redirects = 0; redirects <= redirectLimit; redirects++) {
      if (current.protocol !== targetProtocol || current.host !== targetHost || current.username || current.password || current.search || current.hash) {
        throw typedError('source-redirect-forbidden')
      }
      if (requestCount >= MAX_DISCOVERY_REQUESTS) throw typedError('source-unreachable')
      let remaining = Math.min(Number(options && options.remaining) || 0, deadlineAt - Date.now())
      if (remaining <= 0) throw typedError('source-unreachable')
      const address = await assertNetworkTarget(current, environment, targetHost, remaining)
      remaining = Math.min(Number(options && options.remaining) || 0, deadlineAt - Date.now())
      if (remaining <= 0) throw typedError('source-unreachable')
      const requestCredential = credentialForTarget(current, credential, environment.authKind)
      if (requestCredential && current.protocol !== 'https:') throw typedError('auth-invalid')
      const headers = { accept: kind === 'postman' ? 'application/json' : (kind === 'openapi'
        ? 'application/json, application/yaml, text/yaml'
        : 'text/html, application/xhtml+xml, application/javascript, text/javascript, application/json, application/yaml, text/yaml') }
      if (requestCredential) {
        if (environment.authKind === 'x-api-key') headers['x-api-key'] = requestCredential
        else headers.authorization = `Bearer ${requestCredential}`
      }
      requestCount++
      const maxBytes = kind === 'discovery' ? MAX_DISCOVERY_BODY : MAX_SOURCE
      const response = await requestPinned(current, address, headers, kind, remaining, maxBytes)
      if ([301, 302, 303, 307, 308].includes(response.status)) {
        if (!options.allowRedirects || redirects === redirectLimit) return { ...response, url: current.origin + current.pathname }
        const location = response.headers.location
        if (!location) throw typedError('source-redirect-forbidden')
        const next = new URL(location, current)
        if (next.protocol !== targetProtocol || next.host !== targetHost || next.username || next.password || next.search || next.hash) {
          throw typedError('source-redirect-forbidden')
        }
        current = next
        continue
      }
      return { ...response, url: current.origin + current.pathname }
    }
    throw typedError('source-redirect-limit')
  }
}

async function discoverSource(environment, credential, started, strictBody) {
  const pinHost = new URL(environment.sourceUrl).host
  const deadlineAt = started + TOTAL_TIMEOUT_MS
  const resolution = await runDiscovery({
    fetchFn: discoveryFetcher(environment, credential, pinHost, deadlineAt),
    environment,
    pinHost,
    deadlineAt,
    strictBody,
  })
  return redactResolutionCredential(resolution, credential)
}

function credentialVariants(credential) {
  if (!credential) return []
  const raw = String(credential)
  return [...new Set([raw, encodeURI(raw), encodeURIComponent(raw)].filter(Boolean))]
}

function hasCredentialValue(value, variants) {
  const text = String(value == null ? '' : value)
  return variants.some((variant) => text.includes(variant))
}

function redactCredentialText(value, variants) {
  let text = String(value == null ? '' : value)
  for (const variant of variants) text = text.split(variant).join('[redacted]')
  return text
}

function redactResolutionCredential(resolution, credential) {
  const variants = credentialVariants(credential)
  if (!resolution || !variants.length) return resolution
  const out = { ...resolution }
  if (hasCredentialValue(out.resolvedUrl, variants)) delete out.resolvedUrl
  if (Array.isArray(out.probedPaths)) out.probedPaths = out.probedPaths
    .filter((value) => !hasCredentialValue(value, variants))
  if (Array.isArray(out.candidates)) {
    const candidates = []
    for (const row of out.candidates) {
      if (!row || typeof row !== 'object' || Array.isArray(row)) continue
      const candidate = {}
      if (row.url && !hasCredentialValue(row.url, variants)) candidate.url = row.url
      if (row.uid && !hasCredentialValue(row.uid, variants)) candidate.uid = row.uid
      if (row.title) candidate.title = redactCredentialText(row.title, variants)
      if (row.kind) candidate.kind = row.kind
      if (candidate.url || candidate.uid) candidates.push(candidate)
    }
    if (candidates.length) out.candidates = candidates
    else delete out.candidates
  }
  out.state = out.resolvedUrl ? 'resolved' : (out.candidates && out.candidates.length > 1 ? 'ambiguous' : 'unrecognized')
  return out
}

function parseOpenApi(text) {
  let doc
  try { doc = JSON.parse(text) } catch {
    try { doc = YAML.parse(text, { maxAliasCount: 100 }) } catch { throw typedError('invalid-openapi') }
  }
  if (!doc || typeof doc !== 'object' || Array.isArray(doc) || !/^3\.(0|1)(?:\.|$)/.test(String(doc.openapi || '')) ||
      !doc.info || typeof doc.info !== 'object' || !doc.paths || typeof doc.paths !== 'object' || Array.isArray(doc.paths)) throw typedError('invalid-openapi')
  return doc
}
function parsePostman(text) {
  let collection
  try { collection = JSON.parse(text) } catch { throw typedError('invalid-postman') }
  if (!collection || typeof collection !== 'object' || Array.isArray(collection) ||
      !String(collection.info && collection.info.schema || '').includes('v2.1') || !Array.isArray(collection.item)) throw typedError('invalid-postman')
  return collection
}
function parsePostmanSource(text) {
  let payload
  try { payload = JSON.parse(text) } catch { return parsePostman(text) }
  const wrapped = payload && typeof payload === 'object' && !Array.isArray(payload) &&
    !Object.prototype.hasOwnProperty.call(payload, 'info') && payload.collection
  if (wrapped && typeof payload.collection === 'object' && !Array.isArray(payload.collection) &&
      String(payload.collection.info && payload.collection.info.schema || '').includes('v2.1') &&
      Array.isArray(payload.collection.item)) return payload.collection
  return parsePostman(text)
}
function postmanResolution(info) {
  return info && info.resolvedUrl
    ? { state: 'resolved', reason: 'postman-link', method: 'postman-link', detectedKind: 'postman',
        resolvedUrl: info.resolvedUrl, probedPaths: [] }
    : { state: 'unrecognized', reason: 'postman-link', method: 'postman-link', detectedKind: 'postman', probedPaths: [] }
}
function sanitizePostman(collection) {
  const clone = JSON.parse(JSON.stringify(collection))
  delete clone.auth; delete clone.variable; delete clone.event
  const walk = (items) => (items || []).forEach((item) => {
    delete item.event; delete item.variable; delete item.auth
    if (Array.isArray(item.item)) return walk(item.item)
    if (!item.request) return
    delete item.request.auth; delete item.request.header; delete item.request.cookie; delete item.request.certificate; delete item.request.proxy
    if (item.request.url && typeof item.request.url === 'object' && Array.isArray(item.request.url.query)) {
      item.request.url.query = item.request.url.query.map((row) => ({ key: String(row && row.key || '').slice(0, 128), value: '' }))
    }
    for (const response of Array.isArray(item.response) ? item.response : []) {
      delete response.header; delete response.cookie; delete response.originalRequest
    }
  })
  walk(clone.item)
  return clone
}

function copyCurrentToStage(stageData) {
  const current = generation.current()
  if (!current.ok) throw typedError('snapshot-invalid')
  const manifests = join(stageData, 'manifests')
  mkdirSync(join(manifests, 'areas'), { recursive: true })
  if (current.mode === 'none') {
    return { ...current, previousAreas: Object.create(null) }
  }
  const previousAreas = Object.create(null)
  copyFileSync(current.artifacts.inventory, join(manifests, 'endpoint-inventory.json'))
  for (const [role, file] of Object.entries(current.artifacts)) {
    if (role.startsWith('area:') && file) {
      const area = role.slice(5)
      previousAreas[area] = JSON.parse(readFileSync(file, 'utf8'))
      copyFileSync(file, join(manifests, 'areas', `${area}.json`))
    }
  }
  if (current.artifacts['normalized-spec']) {
    mkdirSync(join(stageData, 'spec'), { recursive: true })
    copyFileSync(current.artifacts['normalized-spec'], join(stageData, 'spec', 'openapi.json'))
  }
  return { ...current, previousAreas }
}
function runTool(script, args, stageData, stageCache, failureCode) {
  const result = spawnSync(process.execPath, [join(HERE, script), ...args], {
    cwd: PROJECT_ROOT,
    env: { ...process.env, ORCHESTRATOR_PROJECT_ROOT: PROJECT_ROOT,
      ORCHESTRATOR_API_CONTRACT_DATA_DIR: stageData, ORCHESTRATOR_API_CONTRACT_CACHE_DIR: stageCache },
    encoding: 'utf8', maxBuffer: 4 * 1024 * 1024,
  })
  if (result.status !== 0) throw typedError(failureCode || (script === 'normalize.mjs' ? 'invalid-openapi' : 'invalid-postman'))
}
function derivedToolEnvironment() {
  const env = { ORCHESTRATOR_PROJECT_ROOT: PROJECT_ROOT }
  for (const key of [
    'PATH', 'TMPDIR', 'TMP', 'TEMP', 'LANG', 'LC_ALL', 'SYSTEMROOT', 'WINDIR',
    'ORCHESTRATOR_CACHE_DIR',
  ]) {
    if (typeof process.env[key] === 'string') env[key] = process.env[key]
  }
  return env
}
function sanitizeGenerated(stageData, postmanOnly, warnings) {
  const areasDir = join(stageData, 'manifests', 'areas')
  if (existsSync(areasDir)) for (const name of readdirSync(areasDir)) {
    if (!/^[a-z0-9][a-z0-9-]*\.json$/.test(name)) continue
    const file = join(areasDir, name)
    const slice = JSON.parse(readFileSync(file, 'utf8'))
    for (const schema of Object.values(slice.schemas || {})) for (const field of schema.fields || []) {
      field.example = null
      if (Array.isArray(field.enum_observed)) {
        const safe = field.enum_observed.filter((value) => typeof value !== 'string' || /^[A-Z][A-Z0-9_]{0,63}$/.test(value)).slice(0, 10)
        if (safe.length !== field.enum_observed.length) warnings.push({ code: 'postman-example-dropped', message: 'A non-enum example value was discarded.' })
        field.enum_observed = safe.length ? safe : null
      }
    }
    writeFileSync(file, JSON.stringify(slice, null, 2) + '\n')
  }
  const inventoryFile = join(stageData, 'manifests', 'endpoint-inventory.json')
  const inventory = JSON.parse(readFileSync(inventoryFile, 'utf8'))
  if (postmanOnly) for (const endpoint of inventory.endpoints || []) endpoint.summary = null
  writeFileSync(inventoryFile, JSON.stringify(inventory, null, 2) + '\n')
}

function endpointKey(endpoint) { return `${endpoint.method} ${endpoint.path}` }
function diffInventories(previous, next) {
  const before = new Map((previous && previous.endpoints || []).map((endpoint) => [endpointKey(endpoint), endpoint]))
  const after = new Map((next && next.endpoints || []).map((endpoint) => [endpointKey(endpoint), endpoint]))
  const added = [], removed = [], changed = [], breaking = []
  for (const [key, endpoint] of after) {
    if (!before.has(key)) { added.push(key); continue }
    const old = before.get(key)
    if (JSON.stringify(stable(old)) !== JSON.stringify(stable(endpoint))) {
      changed.push(key)
      const oldShape = stable({ auth: old.auth, request: old.request, response: old.response, errors: old.errors })
      const newShape = stable({ auth: endpoint.auth, request: endpoint.request, response: endpoint.response, errors: endpoint.errors })
      if (JSON.stringify(oldShape) !== JSON.stringify(newShape)) breaking.push(key)
    }
  }
  for (const key of before.keys()) if (!after.has(key)) { removed.push(key); breaking.push(key) }
  return {
    summary: { added: added.length, changed: changed.length, removed: removed.length, potentiallyBreaking: breaking.length },
    details: { added: added.slice(0, 100), changed: changed.slice(0, 100), removed: removed.slice(0, 100), potentiallyBreaking: [...new Set(breaking)].slice(0, 100) },
  }
}

function readAreas(directory) {
  const out = Object.create(null)
  if (!existsSync(directory)) return out
  for (const name of readdirSync(directory).sort()) {
    if (!/^[a-z0-9][a-z0-9-]*\.json$/.test(name)) continue
    out[name.slice(0, -5)] = JSON.parse(readFileSync(join(directory, name), 'utf8'))
  }
  return out
}

async function buildCandidate(input, environment, credential, stageRoot, allowEnrichmentFailure, discoveryContext) {
  const stageData = join(stageRoot, 'data')
  const stageCache = join(stageRoot, 'runtime')
  mkdirSync(join(stageData, 'manifests', 'areas'), { recursive: true, mode: 0o700 })
  mkdirSync(stageCache, { recursive: true, mode: 0o700 })
  const previous = copyCurrentToStage(stageData)
  const started = Date.now()
  const warnings = []
  const documentHashes = []
  const postmanInfo = discoveryContext ? postmanUrlInfo(environment.sourceUrl) : null
  progress('connecting')
  progress('authenticating')
  if (environment.sourceKind === 'openapi') {
    let source
    try {
      source = await fetchDocument(environment.sourceUrl, environment, credential, 'openapi', started)
    } catch (error) {
      if (discoveryContext && (postmanInfo || error && error.backendCode === 'source-content-type')) {
        progress('resolving-source')
        discoveryContext.resolution = postmanInfo ? postmanResolution(postmanInfo) :
          await discoverSource(environment, credential, started, null)
      }
      throw error
    }
    let doc
    try { doc = parseOpenApi(source.text) }
    catch (error) {
      if (discoveryContext && error && error.backendCode === 'invalid-openapi') {
        progress('resolving-source')
        discoveryContext.strictBody = source.text
        discoveryContext.resolution = postmanInfo ? postmanResolution(postmanInfo) :
          await discoverSource(environment, credential, started, source.text)
      }
      throw error
    }
    documentHashes.push(sha(Buffer.from(source.text)))
    progress('validating-contract')
    const rawFile = join(stageRoot, 'openapi-input.json')
    writeFileSync(rawFile, JSON.stringify(doc))
    runTool('normalize.mjs', ['--input', rawFile], stageData, stageCache)
    if (environment.postmanEnrichmentUrl) {
      try {
        const enrichmentCredential = environment.authKind === 'x-api-key' ? null : credential
        const enrichment = await fetchDocument(environment.postmanEnrichmentUrl, environment, enrichmentCredential, 'postman', started)
        const collection = sanitizePostman(parsePostman(enrichment.text))
        documentHashes.push(sha(Buffer.from(enrichment.text)))
        const collectionFile = join(stageRoot, 'postman-input.json')
        writeFileSync(collectionFile, JSON.stringify(collection))
        runTool('import-postman.mjs', ['--file', collectionFile], stageData, stageCache)
      } catch (error) {
        if (!allowEnrichmentFailure) throw typedError('enrichment-unavailable')
        documentHashes.push(null)
        warnings.push({ code: 'enrichment-unavailable', message: 'Base OpenAPI is valid; Postman enrichment could not be tested.' })
      }
    }
  } else {
    let source
    try { source = await fetchDocument(environment.sourceUrl, environment, credential, 'postman', started) }
    catch (error) {
      if (discoveryContext) {
        progress('resolving-source')
        discoveryContext.resolution = error && error.backendCode === 'auth-invalid'
          ? postmanResolution(postmanInfo)
          : await discoverSource(environment, credential, started, null)
      }
      throw error
    }
    let collection
    try { collection = sanitizePostman(parsePostmanSource(source.text)) }
    catch (error) {
      if (discoveryContext && error && error.backendCode === 'invalid-postman') {
        progress('resolving-source')
        discoveryContext.resolution = await discoverSource(environment, credential, started, source.text)
      }
      throw error
    }
    documentHashes.push(sha(Buffer.from(source.text)))
    progress('validating-contract')
    rmSync(join(stageData, 'manifests', 'endpoint-inventory.json'), { force: true })
    rmSync(join(stageData, 'manifests', 'areas'), { recursive: true, force: true })
    mkdirSync(join(stageData, 'manifests', 'areas'), { recursive: true })
    const collectionFile = join(stageRoot, 'postman-input.json')
    writeFileSync(collectionFile, JSON.stringify(collection))
    runTool('import-postman.mjs', ['--file', collectionFile], stageData, stageCache)
  }
  sanitizeGenerated(stageData, environment.sourceKind === 'postman', warnings)
  runTool('verify.mjs', [], stageData, stageCache, 'candidate-invalid')
  const inventoryFile = join(stageData, 'manifests', 'endpoint-inventory.json')
  const inventory = JSON.parse(readFileSync(inventoryFile, 'utf8'))
  progress('comparing-snapshot')
  const delta = diffInventories(previous.inventory, inventory)
  const currentHash = sha(readFileSync(inventoryFile))
  const nextAreas = readAreas(join(stageData, 'manifests', 'areas'))
  const changeSet = classifyChanges({
    previousInventory: previous.inventory,
    nextInventory: inventory,
    previousAreas: previous.previousAreas || {},
    nextAreas,
    previousHash: previous.snapshotHash || null,
    currentHash,
    environmentId: environment.id,
  })
  const descriptor = stable({ environmentId: environment.id, sourceKind: environment.sourceKind,
    sourceUrl: environment.sourceUrl, postmanEnrichmentUrl: environment.postmanEnrichmentUrl,
    documentHashes, normalizers: [NORMALIZER_VERSION, POSTMAN_VERSION] })
  const sourceFingerprint = sha(Buffer.from(JSON.stringify(descriptor)))
  return { stageData, stageCache, previous, inventory, inventoryFile, areasDir: join(stageData, 'manifests', 'areas'),
    specFile: join(stageData, 'spec', 'openapi.json'), sourceFingerprint, delta, changeSet, currentHash, warnings,
    sourceSummary: { title: inventory.source && inventory.source.title || null, kind: environment.sourceKind,
      version: environment.sourceKind === 'openapi' ? inventory.source && inventory.source.openApiVersion || null : '2.1',
      endpointCount: inventory.stats && inventory.stats.endpoints || 0 } }
}

async function acquirePublicationLease(jobId) {
  const deadline = Date.now() + TOTAL_TIMEOUT_MS
  let announced = false
  while (Date.now() < deadline) {
    let handle = null
    try {
      handle = writerLeases.acquire(paths.WRITER_LEASES_DIR, { kind: 'contract-refresh', key: 'backend:contract-refresh',
        ownerPid: process.pid, ttlMs: TOTAL_TIMEOUT_MS, rootDir: paths.WRITER_AUTHORITY_ROOT })
      const scan = writerLeases.scan(paths.WRITER_LEASES_DIR, paths.WRITER_AUTHORITY_ROOT)
      const foreign = scan.active.some((row) => row.leaseId !== handle.leaseId)
      const finalizationState = finalizations.scanIntegrity({ writerLeaseInspection: {
        active: [], stale: [], issues: [], snapshotInputs: [], stable: true, truncated: false,
      } })
      const recoveryMarkers = finalizationState.findings.length > 0 ||
        finalizationState.statuses.some((row) => row.kind !== 'writer-lease') ||
        creationMarkers.blockingIssue(null, false) !== null || editMarkers.blockingIssue(null, false) !== null
      const blocked = scan.issues.length || scan.stale.length || foreign || recoveryMarkers
      if (!blocked) return handle
    } catch { /* retry below */ }
    if (handle) try { writerLeases.release(handle) } catch {}
    if (!announced) { progress('waiting-writer-lease'); announced = true }
    await new Promise((resolveWait) => setTimeout(resolveWait, 250))
  }
  throw typedError('writer-lease-conflict')
}

function baseReport(input, state, startedAt, finishedAt) {
  return { schemaVersion: 1, reportType: input.action === 'contract:probe' ? 'probe' : 'refresh', jobId: input.jobId,
    state, environmentId: input.environmentId, startedAt, finishedAt }
}

async function runProbe(input, environment, credential, stageRoot, startedAt) {
  const discoveryContext = {}
  let candidate
  try { candidate = await buildCandidate(input, environment, credential, stageRoot, true, discoveryContext) }
  catch (error) {
    if (!discoveryContext.resolution) throw error
    const checkedAt = new Date().toISOString()
    const report = { ...baseReport(input, 'failed', startedAt, checkedAt), sourceKind: environment.sourceKind,
      environmentRevision: input.environmentRevision, authRevision: input.authRevision,
      selectionRevision: input.selectionRevision, checkedAt, resolution: discoveryContext.resolution,
      error: safeMessage(error) }
    history.writeReport('probe', input.jobId, report)
    process.exitCode = 1
    return report
  }
  const checkedAt = new Date().toISOString()
  const previewId = 'preview-' + randomBytes(16).toString('hex')
  const report = { ...baseReport(input, 'success', startedAt, checkedAt), previewId, sourceKind: environment.sourceKind,
    environmentRevision: input.environmentRevision, authRevision: input.authRevision,
    selectionRevision: input.selectionRevision,
    sourceFingerprint: candidate.sourceFingerprint, snapshotFingerprint: candidate.previous.snapshotHash || null,
    checkedAt, expiresAt: new Date(Date.now() + PREVIEW_TTL_MS).toISOString(), sourceSummary: candidate.sourceSummary,
    delta: candidate.delta.summary, authState: environment.authRef === null ? 'not-required' : 'configured', warnings: candidate.warnings }
  history.writeReport('probe', input.jobId, report)
  progress('ready-to-refresh')
  return report
}

async function runRefresh(input, environment, credential, stageRoot, startedAt) {
  const allowBaseOnly = input.acknowledgements.includes('refresh-base-without-enrichment')
  const candidate = await buildCandidate(input, environment, credential, stageRoot, allowBaseOnly, null)
  if (candidate.sourceFingerprint !== input.expectedSourceFingerprint) throw typedError('source-changed')
  if ((candidate.previous.snapshotHash || null) !== (input.expectedSnapshotHash || null)) throw typedError('write-conflict')
  if (credentials.currentRevision(environment.id) !== input.authRevision) throw typedError('auth-revision-conflict')
  const envNow = environmentStore.read()
  if (envNow.revision !== input.environmentRevision) throw typedError('environment-revision-conflict')
  if ((persistence.readPersisted().backendSelectionRevision || 0) !== input.selectionRevision) throw typedError('preview-stale')

  const generationId = generation.createGenerationId()
  const refreshReportFile = history.reportFile('refresh', input.jobId)
  const changeReport = {
    ...candidate.changeSet,
    changeSetId: 'changes-' + sha(Buffer.from(JSON.stringify(stable({
      baseChangeSetId: candidate.changeSet.changeSetId,
      committedGenerationId: generationId,
    })))).slice('sha256:'.length, 'sha256:'.length + 24),
    jobId: input.jobId,
    sourceFingerprint: candidate.sourceFingerprint,
    committedGenerationId: generationId,
  }
  let changeReportFile = null
  let changeSetPublished = false
  const sourceDescriptorFile = join(stageRoot, 'source-descriptor.json')
  if (environment.sourceKind === 'postman') atomicPrivateWrite(sourceDescriptorFile, JSON.stringify({ schemaVersion: 1,
    kind: 'postman', title: candidate.sourceSummary.title, version: candidate.sourceSummary.version,
    sourceFingerprint: candidate.sourceFingerprint }, null, 2) + '\n')

  const lease = await acquirePublicationLease(input.jobId)
  try {
    if (credentials.currentRevision(environment.id) !== input.authRevision) throw typedError('auth-revision-conflict')
    if (environmentStore.read().revision !== input.environmentRevision) throw typedError('environment-revision-conflict')
    if ((persistence.readPersisted().backendSelectionRevision || 0) !== input.selectionRevision) throw typedError('preview-stale')
    changeReportFile = history.writeChangeSet(changeReport)
    const finishedAt = new Date().toISOString()
    const successReport = { ...baseReport(input, candidate.warnings.length ? 'partial' : 'success', startedAt, finishedAt),
      result: candidate.warnings.length ? 'partial' : 'success', environmentId: environment.id,
      previousHash: candidate.previous.snapshotHash || null, currentHash: changeReport.currentHash,
      addedEndpoints: candidate.delta.summary.added, changedEndpoints: candidate.delta.summary.changed,
      removedEndpoints: candidate.delta.summary.removed,
      breakingChanges: changeReport.summary.breaking + changeReport.summary.potentiallyBreaking,
      generatedAreas: candidate.inventory.stats && candidate.inventory.stats.areas || 0, warnings: candidate.warnings,
      committedGenerationId: generationId, sourceFingerprint: candidate.sourceFingerprint }
    history.writeReport('refresh', input.jobId, successReport)
    const published = generation.publish({ generationId, environmentId: environment.id, sourceKind: environment.sourceKind,
      sourceFingerprint: candidate.sourceFingerprint, expectedSnapshotHash: input.expectedSnapshotHash,
      inventoryFile: candidate.inventoryFile, areasDir: candidate.areasDir, specFile: candidate.specFile,
      sourceDescriptorFile, changeReportFile, refreshReportFile, createdAt: startedAt })
    if (!published.ok) throw typedError(published.error === 'write-conflict' ? 'write-conflict' : 'generation-publication-failed')
    changeSetPublished = true
    if (published.currentHash !== successReport.currentHash) throw typedError('generation-publication-failed')
    // Implementation/consumer analysis is a derived, optional projection. It
    // runs only after the exact generation is committed, so it can bind its
    // report to the pointer without ever observing staged or mixed artifacts.
    // A failure leaves the contract refresh valid and the API surface explicitly
    // partial; Diagnostics exposes the typed manual analyzer command.
    spawnSync(process.execPath, [join(HERE, 'analyze-project.mjs')], {
      cwd: PROJECT_ROOT,
      env: derivedToolEnvironment(),
      encoding: 'utf8',
      maxBuffer: 1024 * 1024,
      timeout: 30000,
    })
    return successReport
  } finally {
    if (changeReportFile && !changeSetPublished) {
      try { history.discardChangeSet(changeReport.changeSetId) } catch {}
    }
    try { writerLeases.release(lease) } catch {}
  }
}

async function main() {
  let input = null
  let startedAt = new Date().toISOString()
  let stageRoot = null
  try {
    input = await readInput()
    const envState = environmentStore.read()
    if (envState.mode !== 'manifest' || envState.revision !== input.environmentRevision) throw typedError('environment-revision-conflict')
    const environment = environmentStore.environmentById(envState, input.environmentId)
    if (!environment) throw typedError('source-missing')
    if (input.action === 'contract:refresh-openapi' && environment.sourceKind !== 'openapi' ||
        input.action === 'contract:refresh-postman' && environment.sourceKind !== 'postman') throw typedError('preview-stale')
    const credential = credentials.readForJob(environment, input.authRevision)
    if (!credential.ok) throw typedError(credential.error)
    stageRoot = join(paths.API_CONTRACT_CACHE_DIR, 'staging', input.jobId)
    try {
      lstatSync(stageRoot)
      throw typedError('staging-conflict')
    } catch (stageError) {
      if (stageError && stageError.backendCode) throw stageError
      if (!stageError || stageError.code !== 'ENOENT') throw typedError('staging-conflict')
    }
    if (!fileGuards.realDirectoryUnder(PROJECT_ROOT, stageRoot, { create: true, mode: 0o700 })) throw typedError('staging-unsafe')
    chmodSync(stageRoot, 0o700)
    const report = input.action === 'contract:probe'
      ? await runProbe(input, environment, credential.secret, stageRoot, startedAt)
      : await runRefresh(input, environment, credential.secret, stageRoot, startedAt)
    const event = { type: 'result', reportType: report.reportType, jobId: input.jobId, state: report.state }
    if (report.error) event.error = report.error
    process.stdout.write(JSON.stringify(event) + '\n')
  } catch (error) {
    const failure = safeMessage(error)
    if (input && JOB_RE.test(input.jobId)) {
      const type = input.action === 'contract:probe' ? 'probe' : 'refresh'
      const finishedAt = new Date().toISOString()
      const report = { ...baseReport(input, 'failed', startedAt, finishedAt), result: type === 'refresh' ? 'failed' : undefined,
        committedGenerationId: type === 'refresh' ? null : undefined, error: failure }
      Object.keys(report).forEach((key) => report[key] === undefined && delete report[key])
      try { history.writeReport(type, input.jobId, report) } catch {}
    }
    process.stdout.write(JSON.stringify({ type: 'result', reportType: input && input.action === 'contract:probe' ? 'probe' : 'refresh',
      jobId: input && input.jobId || null, state: 'failed', error: failure }) + '\n')
    process.exitCode = 1
  } finally {
    if (stageRoot) try {
      if (fileGuards.realDirectoryUnder(PROJECT_ROOT, stageRoot, { create: false })) rmSync(stageRoot, { recursive: true, force: true })
    } catch {}
  }
}

await main()
