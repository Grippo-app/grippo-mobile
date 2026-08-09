import YAML from 'yaml'

const CONTROL_RE = /[\u0000-\u001f\u007f-\u009f]/g
const OPENAPI_CANDIDATE_LIMIT = 8
const POSTMAN_CANDIDATE_LIMIT = 20
const POSTMAN_COLLECTIONS_URL = 'https://api.getpostman.com/collections'
// KEEP IN SYNC with POSTMAN_LONG_UID in site/scripts/panels/backend.js.
const POSTMAN_UID_RE = /^[0-9]+-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

function isPostmanPageHost(host) {
  return host === 'postman.com' || host === 'www.postman.com' || host === 'postman.co' || host.endsWith('.postman.co')
}

function cleanString(value, limit) {
  return String(value == null ? '' : value).replace(CONTROL_RE, '').slice(0, limit)
}

function uniqueByUrl(rows) {
  const seen = new Set()
  return rows.filter((row) => {
    if (!row || !row.url || seen.has(row.url)) return false
    seen.add(row.url)
    return true
  })
}

function resolveReference(value, baseUrl) {
  try { return new URL(String(value), baseUrl).toString() } catch { return null }
}

function quotedPropertyValues(source, property) {
  const out = []
  const pattern = new RegExp(`(?:^|[^A-Za-z0-9_$])${property}\\s*:\\s*(['\"])([^'\"\\r\\n]+)\\1`, 'g')
  for (const match of String(source || '').matchAll(pattern)) out.push(match[2])
  return out
}

function titleForUrl(source, value) {
  const escaped = value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const before = new RegExp(`name\\s*:\\s*(['\"])([^'\"\\r\\n]+)\\1[^{}]{0,300}url\\s*:\\s*(['\"])${escaped}\\3`)
  const after = new RegExp(`url\\s*:\\s*(['\"])${escaped}\\1[^{}]{0,300}name\\s*:\\s*(['\"])([^'\"\\r\\n]+)\\2`)
  const beforeMatch = String(source || '').match(before)
  if (beforeMatch) return cleanString(beforeMatch[2], 120) || null
  const afterMatch = String(source || '').match(after)
  return afterMatch ? cleanString(afterMatch[3], 120) || null : null
}

function scriptReferences(html, baseUrl) {
  const out = []
  const pattern = /<script\b[^>]*\bsrc\s*=\s*(['"])([^'"]+)\1[^>]*>/gi
  for (const match of String(html || '').matchAll(pattern)) {
    if (!/(?:swagger-initializer|swagger-ui-init)\.js(?:$|[?#])/i.test(match[2])) continue
    const url = resolveReference(match[2], baseUrl)
    if (url) out.push(url)
  }
  return [...new Set(out)]
}

function hintsFromScript(source, baseUrl) {
  const candidates = quotedPropertyValues(source, 'url').map((value) => ({
    url: resolveReference(value, baseUrl), title: titleForUrl(source, value), kind: 'openapi',
  })).filter((row) => row.url)
  const configUrls = quotedPropertyValues(source, 'configUrl')
    .map((value) => resolveReference(value, baseUrl)).filter(Boolean)
  return { candidates: uniqueByUrl(candidates), configUrls: [...new Set(configUrls)] }
}

export function postmanUrlInfo(value) {
  let url
  try { url = new URL(String(value || '')) } catch { return null }
  const host = url.hostname.toLowerCase()
  if (host === 'api.getpostman.com') {
    const match = url.pathname.match(/^\/collections\/([^/]+)\/?$/i)
    const uid = match && POSTMAN_UID_RE.test(match[1]) ? match[1] : null
    return { detectedKind: 'postman', uid,
      resolvedUrl: uid ? `https://api.getpostman.com/collections/${uid}` : null }
  }
  if (!isPostmanPageHost(host)) return null
  const match = url.pathname.match(/\/collection\/([^/]+)(?:\/|$)/i)
  const uid = match && POSTMAN_UID_RE.test(match[1]) ? match[1] : null
  return { detectedKind: 'postman', uid,
    resolvedUrl: uid ? `https://api.getpostman.com/collections/${uid}` : null }
}

export function specUrlsFromHtml(html, baseUrl) {
  const hints = hintsFromScript(html, baseUrl)
  return {
    candidates: hints.candidates,
    configUrls: hints.configUrls,
    initializerUrls: scriptReferences(html, baseUrl),
  }
}

export function specUrlsFromSwaggerConfig(value, baseUrl) {
  let config = value
  if (typeof value === 'string') {
    try { config = JSON.parse(value) } catch { return { candidates: [], configUrls: [] } }
  }
  if (!config || typeof config !== 'object' || Array.isArray(config)) return { candidates: [], configUrls: [] }
  const candidates = []
  if (typeof config.url === 'string') {
    const url = resolveReference(config.url, baseUrl)
    if (url) candidates.push({ url, title: null, kind: 'openapi' })
  }
  if (Array.isArray(config.urls)) for (const row of config.urls) {
    if (!row || typeof row !== 'object' || typeof row.url !== 'string') continue
    const url = resolveReference(row.url, baseUrl)
    if (url) candidates.push({ url, title: cleanString(row.name, 120) || null, kind: 'openapi' })
  }
  const configUrls = []
  if (typeof config.configUrl === 'string') {
    const url = resolveReference(config.configUrl, baseUrl)
    if (url) configUrls.push(url)
  }
  return { candidates: uniqueByUrl(candidates), configUrls: [...new Set(configUrls)] }
}

export function conventionCandidates(value) {
  let url
  try { url = new URL(String(value || '')) } catch { return [] }
  const path = url.pathname === '/' ? '' : url.pathname.replace(/\/+$/, '')
  const values = [
    `${path || '/'}-json`,
    '/openapi.json',
    '/v3/api-docs',
    '/swagger/v1/swagger.json',
    '/swagger.json',
    '/api-docs',
    `${path}/openapi.json`,
  ]
  return [...new Set(values.map((pathname) => new URL(pathname.replace(/^\/\/-/, '/-'), url.origin).toString()))]
}

function normalizedCandidate(value, baseUrl, sourceUrl, pinHost) {
  let candidate
  try { candidate = new URL(String(value || ''), baseUrl) } catch { return { url: null, crossHost: false } }
  if (!['http:', 'https:'].includes(candidate.protocol) || candidate.protocol !== sourceUrl.protocol ||
      candidate.host !== pinHost || candidate.username || candidate.password || candidate.search || candidate.hash) {
    return { url: null, crossHost: candidate.host !== pinHost || candidate.protocol !== sourceUrl.protocol }
  }
  return { url: candidate.origin + candidate.pathname, crossHost: false }
}

function openApiDocument(text) {
  let doc
  try { doc = JSON.parse(text) } catch {
    try { doc = YAML.parse(text, { maxAliasCount: 100 }) } catch { return { valid: false, swagger2: false } }
  }
  if (!doc || typeof doc !== 'object' || Array.isArray(doc)) return { valid: false, swagger2: false }
  if (doc.swagger === '2.0') return { valid: false, swagger2: true }
  const valid = /^3\.(0|1)(?:\.|$)/.test(String(doc.openapi || '')) && doc.info && typeof doc.info === 'object' &&
    doc.paths && typeof doc.paths === 'object' && !Array.isArray(doc.paths)
  return { valid: !!valid, swagger2: false, title: valid ? cleanString(doc.info.title, 120) || null : null }
}

function baseResolution(extra, probedPaths) {
  return { state: 'unrecognized', ...extra, probedPaths: probedPaths.slice(0, OPENAPI_CANDIDATE_LIMIT) }
}

function discoveryError(code) {
  const error = new Error(code)
  error.backendCode = code
  return error
}

function discoveryDeadlineError() {
  const error = discoveryError('source-unreachable')
  error.discoveryDeadline = true
  return error
}

function rethrowDiscoveryDeadline(error) {
  if (error && error.discoveryDeadline === true) throw error
}

async function runPostmanDiscovery(fetchLocal, environment) {
  const info = postmanUrlInfo(environment.sourceUrl) || { detectedKind: 'postman', uid: null, resolvedUrl: null }
  if (info.resolvedUrl) return { state: 'resolved', reason: 'postman-link', method: 'postman-link',
    detectedKind: 'postman', resolvedUrl: info.resolvedUrl, probedPaths: [] }
  const sourceHost = new URL(environment.sourceUrl).hostname.toLowerCase()
  if (sourceHost === 'api.getpostman.com' || environment.id === 'local' || environment.authKind !== 'x-api-key' ||
      environment.authRef !== environment.id) {
    return { state: 'unrecognized', reason: 'postman-link', method: 'postman-link', detectedKind: 'postman', probedPaths: [] }
  }
  const response = await fetchLocal(POSTMAN_COLLECTIONS_URL,
    { kind: 'postman', allowRedirects: false, maxBytes: 10 * 1024 * 1024 })
  if (!response || response.status === 401 || response.status === 403) throw discoveryError('auth-rejected')
  if (response.status < 200 || response.status >= 300 || typeof response.text !== 'string') throw discoveryError('source-unreachable')
  let payload
  try { payload = JSON.parse(response.text) } catch { throw discoveryError('source-unreachable') }
  if (!payload || typeof payload !== 'object' || !Array.isArray(payload.collections)) throw discoveryError('source-unreachable')
  const valid = payload.collections.map((row) => {
    const uid = cleanString(row && row.uid, 120)
    if (!POSTMAN_UID_RE.test(uid)) return null
    return { uid, title: cleanString(row && row.name, 120) || uid, kind: 'postman' }
  }).filter(Boolean)
  const candidates = valid.slice(0, POSTMAN_CANDIDATE_LIMIT)
  return {
    state: candidates.length > 1 ? 'ambiguous' : 'unrecognized',
    reason: 'postman-link',
    method: 'postman-picker',
    detectedKind: 'postman',
    ...(candidates.length ? { candidates } : {}),
    ...(valid.length > POSTMAN_CANDIDATE_LIMIT ? { truncated: true } : {}),
    probedPaths: [],
  }
}

export async function runDiscovery({ fetchFn, environment, pinHost, deadlineAt, strictBody = null }) {
  const sourceUrl = new URL(environment.sourceUrl)
  const safePinHost = String(pinHost || sourceUrl.host)
  const probedPaths = []
  const hints = []
  const initializerUrls = []
  const configUrls = []
  let pageBody = strictBody
  let pageUrl = sourceUrl.origin + sourceUrl.pathname
  let crossHost = false
  let swagger2 = false
  let fetchCalls = 0

  const remaining = () => deadlineAt - Date.now()
  const fetchLocal = async (url, options) => {
    if (remaining() <= 0) throw discoveryDeadlineError()
    if (fetchCalls >= OPENAPI_CANDIDATE_LIMIT) throw new Error('discovery-request-budget')
    fetchCalls++
    let response
    try { response = await fetchFn(url, { ...options, remaining: remaining() }) }
    catch (error) {
      if (remaining() <= 0) throw discoveryDeadlineError()
      throw error
    }
    if (remaining() <= 0) throw discoveryDeadlineError()
    return response
  }

  if (environment.sourceKind === 'postman') return runPostmanDiscovery(fetchLocal, environment)

  if (strictBody == null) {
    try {
      const page = await fetchLocal(pageUrl, { kind: 'discovery', allowRedirects: true, maxBytes: 2 * 1024 * 1024 })
      if (page && page.status >= 200 && page.status < 300 && typeof page.text === 'string') {
        pageBody = page.text
        pageUrl = page.url || pageUrl
      }
    } catch (error) { rethrowDiscoveryDeadline(error); pageBody = null }
  }

  if (typeof pageBody === 'string') {
    const direct = openApiDocument(pageBody)
    if (direct.swagger2) swagger2 = true
    else if (direct.valid) hints.push({ url: pageUrl, title: direct.title, kind: 'openapi', method: 'page' })

    const config = specUrlsFromSwaggerConfig(pageBody, pageUrl)
    config.candidates.forEach((row) => hints.push({ ...row, method: 'config' }))
    configUrls.push(...config.configUrls)

    const page = specUrlsFromHtml(pageBody, pageUrl)
    page.candidates.forEach((row) => hints.push({ ...row, method: 'page' }))
    initializerUrls.push(...page.initializerUrls)
    configUrls.push(...page.configUrls)
  }

  if (!swagger2) for (const rawUrl of [...new Set(initializerUrls)]) {
    const normalized = normalizedCandidate(rawUrl, pageUrl, sourceUrl, safePinHost)
    crossHost ||= normalized.crossHost
    if (!normalized.url) continue
    try {
      const initializer = await fetchLocal(normalized.url, { kind: 'discovery', allowRedirects: false, maxBytes: 2 * 1024 * 1024 })
      if (!initializer || initializer.status < 200 || initializer.status >= 300 || typeof initializer.text !== 'string') continue
      const parsed = specUrlsFromHtml(initializer.text, normalized.url)
      parsed.candidates.forEach((row) => hints.push({ ...row, method: 'initializer' }))
      configUrls.push(...parsed.configUrls)
    } catch (error) {
      rethrowDiscoveryDeadline(error)
      /* R2: discovery failures never replace the strict failure. */
    }
  }

  if (!swagger2) for (const rawUrl of [...new Set(configUrls)]) {
    const normalized = normalizedCandidate(rawUrl, pageUrl, sourceUrl, safePinHost)
    crossHost ||= normalized.crossHost
    if (!normalized.url) continue
    try {
      const configResponse = await fetchLocal(normalized.url, { kind: 'discovery', allowRedirects: false, maxBytes: 2 * 1024 * 1024 })
      if (!configResponse || configResponse.status < 200 || configResponse.status >= 300) continue
      const parsed = specUrlsFromSwaggerConfig(configResponse.text, normalized.url)
      parsed.candidates.forEach((row) => hints.push({ ...row, method: 'config' }))
    } catch (error) { rethrowDiscoveryDeadline(error); /* R2 */ }
  }

  if (swagger2) return baseResolution({ reason: 'openapi-2-unsupported' }, probedPaths)

  const pageProducedCandidates = hints.length > 0
  if (!pageProducedCandidates) conventionCandidates(environment.sourceUrl)
    .forEach((url) => hints.push({ url, title: null, kind: 'openapi', method: 'convention' }))

  const normalizedHints = []
  for (const hint of uniqueByUrl(hints)) {
    const normalized = normalizedCandidate(hint.url, pageUrl, sourceUrl, safePinHost)
    crossHost ||= normalized.crossHost
    if (!normalized.url) continue
    normalizedHints.push({ ...hint, url: normalized.url })
  }

  const accepted = []
  let authRequired = false
  for (const hint of normalizedHints.slice(0, OPENAPI_CANDIDATE_LIMIT)) {
    if (remaining() <= 0) break
    probedPaths.push(hint.url)
    try {
      const response = await fetchLocal(hint.url, { kind: 'openapi', allowRedirects: false, maxBytes: 10 * 1024 * 1024 })
      if (!response) continue
      if (response.status === 401 || response.status === 403) { authRequired = true; continue }
      if (response.status < 200 || response.status >= 300 || typeof response.text !== 'string') continue
      const parsed = openApiDocument(response.text)
      if (parsed.swagger2) { swagger2 = true; continue }
      if (!parsed.valid) continue
      accepted.push({ url: hint.url, title: parsed.title || hint.title || null, kind: 'openapi', method: hint.method })
    } catch (error) { rethrowDiscoveryDeadline(error); /* R2 */ }
  }

  const candidates = uniqueByUrl(accepted).map(({ method, ...candidate }) => candidate)
  if (candidates.length === 1) return {
    state: 'resolved', method: accepted[0].method, resolvedUrl: candidates[0].url,
    candidates, probedPaths: probedPaths.slice(0, OPENAPI_CANDIDATE_LIMIT),
  }
  if (candidates.length > 1) return {
    state: 'ambiguous', method: pageProducedCandidates ? 'page' : 'convention',
    candidates, probedPaths: probedPaths.slice(0, OPENAPI_CANDIDATE_LIMIT),
  }
  if (swagger2) return baseResolution({ reason: 'openapi-2-unsupported' }, probedPaths)
  if (authRequired) return baseResolution({ reason: 'auth-required' }, probedPaths)
  if (crossHost) return baseResolution({ reason: 'cross-host-unsupported' }, probedPaths)
  return baseResolution({}, probedPaths)
}
