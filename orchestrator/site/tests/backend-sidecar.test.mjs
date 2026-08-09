import assert from 'node:assert/strict'
import { after, test } from 'node:test'
import { createServer } from 'node:http'
import { createServer as createHttpsServer } from 'node:https'
import { randomBytes } from 'node:crypto'
import { existsSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync, statSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { spawn, spawnSync } from 'node:child_process'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'
import {
  conventionCandidates,
  postmanUrlInfo,
  runDiscovery,
  specUrlsFromHtml,
  specUrlsFromSwaggerConfig,
} from '../../api-contract/scripts/resolve-source.mjs'

const REPO = fileURLToPath(new URL('../../../', import.meta.url))
const root = mkdtempSync(join(tmpdir(), 'backend-sidecar-'))
const contractDir = join(root, 'orchestrator', 'api-contract')
mkdirSync(contractDir, { recursive: true })
writeFileSync(join(root, 'orchestrator', 'project-config.md'), '---\nbackendContractEnabled: auto\n---\n')
process.env.ORCHESTRATOR_PROJECT_ROOT = root

const require = createRequire(import.meta.url)
const environments = require(join(REPO, 'orchestrator', 'site', 'server', 'backend-environments.js'))
const generation = require(join(REPO, 'orchestrator', 'site', 'server', 'contract-generation.js'))
const history = require(join(REPO, 'orchestrator', 'site', 'server', 'contract-history.js'))
const persistence = require(join(REPO, 'orchestrator', 'site', 'server', 'persistence.js'))
const sidecar = join(REPO, 'orchestrator', 'api-contract', 'scripts', 'backend-action.mjs')
const REFLECTED_CREDENTIAL = 'PMAK-test-api-key'
let firstRefresh = null

const TEST_TLS_KEY = `-----BEGIN PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCr/U2TBmHVIyhK
LACD/rMNrgzTqzpfi4UxZvGOb0jT7PZiKePBQkv5CuZrhMDG4U8tMimws0ABbnlQ
G4OhpZEGYADV32DQDKLz1Kienq2TMO5CI83DyL16b2GhzeUGanKbRNuVfkmMZRXV
DWIyH2rMbADZZBQTkJCXVXdFEZ03GvS3pS0p4RRHRMUOyER4NqCQ4nIfqCSV9Pe4
+gC5zsnsjx5sTGYasiB5K3YOgP5BVu0DW4D3U5hoZz/YlcgSO6uJNCw+nYYqbKWv
s4sZHnNdheRncLfB+ayzlABXKUy3N5AUVkAJZnlZ03gFMKJQT2mCq8HOXuvm1ykg
uUcLnmr7AgMBAAECggEAFSIyBw/ws5T3XW3cvltsuxCZCOVwbc8iFbj1k6/+/fOP
Lu7Ayr2PwlFhkPKmEYocBYy1bL4J7mMZywsmeJV0pZjkYWNqbQYwe8+0zJDo+T44
+K2XmigviDzpfU1kiA5KeOPUK6Fx/itprF1RpKERqX7X996BTQolikL5PeZ1MBEP
TlIHb22iYAGSvCt9gx6rKVfJOvQanEpkcmkM8bdW2wY4Lb4XdtIgYZj/z1i2OXB9
tNk/3F8cRGcKRJNqBnp5rrkfh6MpqW+Gmi6FzGuj+YjAcFBMJ85WNIPYbOJ0Qjbz
m9HA2bTRwTif5xPO4dYbxYJd5rqyPB2c8aff06jpMQKBgQDWm2kUqC/pMid5zpwC
PCF6JGbTtLvskgf46ehc7t60ujXCTG+Sr+yrKFAUw/WaXM7InhBj7NS8ESztP+a3
C/+c4f276kd4Ys/Q8FMq2hrIEipMbHogxOz+8RwHYHb91a9kyvkT6Sg78cAk2cy3
ttjvFG/Wj9Rn/LNgllfhJkSfeQKBgQDNKZNdieQ6crxgVdB2CDJS5ZIDGluXRo5m
AOYb3mJIrxaYfmVKiOOOcDBvfXRbReFGZkfAxLf/a3/rjCMcR/2ua50cSTAWeJQG
MHqqmnVeAkA5TzapjNlezKvi8TNmeq/Ao/PE7EcodL9VVzSNLoYJJTWcj/+QoueS
hZYGo0z9EwKBgGaxzbH93Zdo0a0fKlaDcb2iJ7aNGRRWuQvq2q+F+vpe1RTaQNXf
64/n5ZJaR3ALk0Q7Jzq5/P7V9Tq+xme3jSUEm8fNj0/TcQDZz+KEpiYxUx5pFnQR
6BsDQ9df2PEUzS6dVl2pyAAPalP4CVesXtjhXbc98nWkXi4NI5ZxGot5AoGBAJ2W
pd9fn7NVlmTZBbrkKOOL+0/u2xOj2pV3c0lM1GnIM68Gx38tDr3GZy38S3gggR22
Sr9DW6ISTLv9dKqwkad8eb3Uad+Y1vq6Gj8ZTbXcPMDIm9A4pG8w/4behwDclEuA
pCpK442rv5wp0+/uIH692iRcZ44N05q0ibyniueJAoGAS9Wtl8aKdosg3cYufYj8
oyS5z373/X10aQPFM7Cq1zm3+TfjpEKme/5HIAKplMn5ySRNnUdga1xdotvxc+za
BC+WxjMlGL74hB0SR+0HgSvdMIT34lBoFQCW/Db8uI+JmlgRP6srMBX7G7C2xEC9
EX9rRpb1XEM5lIMvHpD3zwg=
-----END PRIVATE KEY-----`
const TEST_TLS_CERT = `-----BEGIN CERTIFICATE-----
MIIDGjCCAgKgAwIBAgIUD7VY4WkDFp2iL66phEuRnvlVR7kwDQYJKoZIhvcNAQEL
BQAwFDESMBAGA1UEAwwJMTI3LjAuMC4xMB4XDTI2MDgwNDExNTQwMVoXDTM2MDgw
MTExNTQwMVowFDESMBAGA1UEAwwJMTI3LjAuMC4xMIIBIjANBgkqhkiG9w0BAQEF
AAOCAQ8AMIIBCgKCAQEAq/1NkwZh1SMoSiwAg/6zDa4M06s6X4uFMWbxjm9I0+z2
YinjwUJL+Qrma4TAxuFPLTIpsLNAAW55UBuDoaWRBmAA1d9g0Ayi89Sonp6tkzDu
QiPNw8i9em9hoc3lBmpym0TblX5JjGUV1Q1iMh9qzGwA2WQUE5CQl1V3RRGdNxr0
t6UtKeEUR0TFDshEeDagkOJyH6gklfT3uPoAuc7J7I8ebExmGrIgeSt2DoD+QVbt
A1uA91OYaGc/2JXIEjuriTQsPp2GKmylr7OLGR5zXYXkZ3C3wfmss5QAVylMtzeQ
FFZACWZ5WdN4BTCiUE9pgqvBzl7r5tcpILlHC55q+wIDAQABo2QwYjAdBgNVHQ4E
FgQUtrIhO5QEchUuYQk1rowuKf7BKIIwHwYDVR0jBBgwFoAUtrIhO5QEchUuYQk1
rowuKf7BKIIwDwYDVR0TAQH/BAUwAwEB/zAPBgNVHREECDAGhwR/AAABMA0GCSqG
SIb3DQEBCwUAA4IBAQAaVkzynk0s8e77J8z2yxTf/yGXReHLqS3GPT01+AqK0VaL
IscBgYIi0AWfmOV6pKEBG6MFcUuXhCBuzaVdzadi7Nrq8mj7zIDj01cH6O2HRwOW
kZphbBFiS2RULjw7NZvkUn0mNcG6/cXcLAT2/TFM0fBP9H7AApu8tYooiGC3uR6H
j0AQes+RG2QXoYu3oHmYdvXhU+hbsVfXcjlBXJfup9Npr4+TUcBfPfXwOcjt6/Xe
LCe7GS+0ePO0jw/TrA8an1qxCnUtgnHXkpwCYQRSIfuSdzg9jLbC3pf+r3yvsV8W
pL6O8bB5jz4kr/Qx7Emk31BmqdVrcLwq/MOjdgLw
-----END CERTIFICATE-----`

let openapi = JSON.parse(readFileSync(join(REPO, 'orchestrator', 'api-contract', 'scripts', 'examples', 'openapi.sample.json'), 'utf8'))
const originalOpenapi = JSON.parse(JSON.stringify(openapi))
const postman = JSON.parse(readFileSync(join(REPO, 'orchestrator', 'api-contract', 'scripts', 'examples', 'postman.sample.json'), 'utf8'))
postman.variable = [{ key: 'token', value: 'MUST_NOT_COMMIT_VARIABLE' }]
if (postman.item[0] && postman.item[0].request) {
  postman.item[0].request.header = [{ key: 'Authorization', value: 'Bearer MUST_NOT_COMMIT_HEADER' }]
  postman.item[0].request.body = { mode: 'raw', raw: '{"password":"MUST_NOT_COMMIT_BODY"}' }
}

let port = 0
let securePort = 0
const secureRequests = []
const server = createServer((req, res) => {
  if (req.url === '/openapi.json') {
    const body = JSON.stringify(openapi)
    res.writeHead(200, { 'content-type': 'application/json', 'content-length': Buffer.byteLength(body) }); res.end(body); return
  }
  if (req.url === '/openapi-3.0.json') {
    const body = JSON.stringify({ ...openapi, openapi: '3.0.3' })
    res.writeHead(200, { 'content-type': 'application/json' }); res.end(body); return
  }
  if (req.url === '/openapi-2.json') {
    res.writeHead(200, { 'content-type': 'application/json' }); res.end('{"swagger":"2.0","info":{},"paths":{}}'); return
  }
  if (req.url === '/invalid.json') { res.writeHead(200, { 'content-type': 'application/json' }); res.end('{"openapi":'); return }
  if (req.url === '/invalid.yaml') { res.writeHead(200, { 'content-type': 'application/yaml' }); res.end('openapi: ['); return }
  if (req.url === '/wrong-type') { res.writeHead(200, { 'content-type': 'text/html' }); res.end('<html></html>'); return }
  if (req.url === '/docs') {
    res.writeHead(200, { 'content-type': 'text/html' })
    res.end('<html><script>SwaggerUIBundle({url: "/linked-openapi.json"})</script></html>'); return
  }
  if (req.url === '/linked-openapi.json') {
    const body = JSON.stringify({ ...openapi, info: { ...openapi.info, title: 'Linked API' } })
    res.writeHead(200, { 'content-type': 'application/json' }); res.end(body); return
  }
  if (req.url === '/multi-docs') {
    res.writeHead(200, { 'content-type': 'text/html' })
    res.end('<script>SwaggerUIBundle({urls:[{url:"/one.json",name:"One"},{url:"/two.json",name:"Two"}]})</script>'); return
  }
  if (req.url === '/one.json' || req.url === '/two.json') {
    const title = req.url === '/one.json' ? 'One' : 'Two'
    res.writeHead(200, { 'content-type': 'application/json' })
    res.end(JSON.stringify({ ...openapi, info: { ...openapi.info, title } })); return
  }
  if (req.url === '/r2-docs') {
    res.writeHead(200, { 'content-type': 'text/html' })
    res.end('<script>SwaggerUIBundle({urls:[{url:"/unauthorized"},{url:"/missing"},{url:"/redirect-loop"}]})</script>'); return
  }
  if (req.url === '/redirect') { res.writeHead(302, { location: '/openapi.json' }); res.end(); return }
  if (req.url === '/redirect-loop') { res.writeHead(302, { location: '/redirect-loop' }); res.end(); return }
  if (req.url === '/bad-redirect') { res.writeHead(302, { location: `http://localhost:${port}/openapi.json` }); res.end(); return }
  if (req.url === '/unauthorized') { res.writeHead(401, { 'content-type': 'application/json' }); res.end('{}'); return }
  if (req.url === '/huge') { res.writeHead(200, { 'content-type': 'application/json', 'content-length': 10 * 1024 * 1024 + 1 }); res.end(); return }
  if (req.url === '/postman.json') {
    const body = JSON.stringify(postman)
    res.writeHead(200, { 'content-type': 'application/json', 'content-length': Buffer.byteLength(body) }); res.end(body); return
  }
  if (req.url === '/postman-envelope.json') {
    const body = JSON.stringify({ collection: postman })
    res.writeHead(200, { 'content-type': 'application/json', 'content-length': Buffer.byteLength(body) }); res.end(body); return
  }
  res.writeHead(404); res.end()
})
await new Promise((resolve) => server.listen(0, '127.0.0.1', () => { port = server.address().port; resolve() }))
const secureServer = createHttpsServer({ key: TEST_TLS_KEY, cert: TEST_TLS_CERT }, (req, res) => {
  secureRequests.push({ url: req.url, headers: req.headers })
  if (req.url === '/credential-docs') {
    res.writeHead(200, { 'content-type': 'text/html' })
    res.end(`<script>SwaggerUIBundle({url:"/${REFLECTED_CREDENTIAL}"})</script>`)
    return
  }
  const body = JSON.stringify(req.url === '/postman.json' ? postman : (req.url === `/${REFLECTED_CREDENTIAL}`
    ? { ...openapi, info: { ...openapi.info, title: `Reflected ${REFLECTED_CREDENTIAL}` } }
    : openapi))
  res.writeHead(200, { 'content-type': 'application/json', 'content-length': Buffer.byteLength(body) })
  res.end(body)
})
await new Promise((resolve) => secureServer.listen(0, '127.0.0.1', () => { securePort = secureServer.address().port; resolve() }))

after(async () => {
  await new Promise((resolve) => server.close(resolve))
  await new Promise((resolve) => secureServer.close(resolve))
  rmSync(root, { recursive: true, force: true })
})

function writeEnvironment(sourceKind, path) {
  writeFileSync(join(contractDir, 'environments.json'), JSON.stringify({ schemaVersion: 1,
    environments: [{ id: 'local', label: 'Local', sourceKind, sourceUrl: `http://127.0.0.1:${port}${path}`,
      postmanEnrichmentUrl: null, authRef: null }], defaultEnvironmentId: 'local' }, null, 2) + '\n')
  const state = environments.read()
  assert.equal(state.mode, 'manifest')
  return state
}
function createFirstEnvironment(sourceKind, path) {
  const missing = environments.read()
  assert.equal(missing.mode, 'missing')
  const created = environments.mutate({ operation: 'create', expectedRevision: missing.revision,
    idempotencyKey: 'environment:first-source', defaultEnvironmentId: 'local',
    environment: { id: 'local', label: 'Local', sourceKind, sourceUrl: `http://127.0.0.1:${port}${path}`,
      postmanEnrichmentUrl: null, authRef: null } })
  assert.equal(created.ok, true)
  const state = environments.read()
  assert.equal(state.mode, 'manifest')
  assert.equal(state.manifest.defaultEnvironmentId, 'local')
  return state
}
function id() { return 'job-' + randomBytes(16).toString('hex') }
function run(request, projectRoot = root, extraEnv = {}) {
  return new Promise((resolveRun) => {
    const child = spawn(process.execPath, [sidecar], { cwd: REPO,
      env: { ...process.env, ORCHESTRATOR_PROJECT_ROOT: projectRoot, ...extraEnv }, stdio: ['pipe', 'pipe', 'pipe'] })
    let stdout = '', stderr = ''
    child.stdout.on('data', (chunk) => { stdout += chunk })
    child.stderr.on('data', (chunk) => { stderr += chunk })
    child.on('close', (code) => resolveRun({ code, stdout, stderr }))
    child.stdin.end(JSON.stringify(request))
  })
}
function fixtureModuleCall(projectRoot, moduleFile, expression, request) {
  const code = `const value = require(process.env.FIXTURE_MODULE); Promise.resolve(value.${expression}(JSON.parse(require('node:fs').readFileSync(0, 'utf8')))).then((result) => process.stdout.write(JSON.stringify(result)))`
  const result = spawnSync(process.execPath, ['-e', code], { cwd: REPO,
    env: { ...process.env, ORCHESTRATOR_PROJECT_ROOT: projectRoot, FIXTURE_MODULE: moduleFile },
    input: JSON.stringify(request), encoding: 'utf8' })
  assert.equal(result.status, 0, result.stderr || result.stdout)
  return JSON.parse(result.stdout)
}
function fixtureEnvironmentState(projectRoot) {
  const code = `const value = require(process.env.FIXTURE_MODULE).read(); process.stdout.write(JSON.stringify(value))`
  const result = spawnSync(process.execPath, ['-e', code], { cwd: REPO,
    env: { ...process.env, ORCHESTRATOR_PROJECT_ROOT: projectRoot,
      FIXTURE_MODULE: join(REPO, 'orchestrator', 'site', 'server', 'backend-environments.js') }, encoding: 'utf8' })
  assert.equal(result.status, 0, result.stderr || result.stdout)
  return JSON.parse(result.stdout)
}
function runCommand(script, args) {
  return new Promise((resolveRun) => {
    const child = spawn(process.execPath, [script, ...args], { cwd: root,
      env: { ...process.env, ORCHESTRATOR_PROJECT_ROOT: root }, stdio: ['ignore', 'pipe', 'pipe'] })
    let stdout = '', stderr = ''
    child.stdout.on('data', (chunk) => { stdout += chunk })
    child.stderr.on('data', (chunk) => { stderr += chunk })
    child.on('close', (status) => resolveRun({ status, stdout, stderr }))
  })
}
async function probe(state) {
  const jobId = id()
  const result = await run({ schemaVersion: 1, jobId, action: 'contract:probe', environmentId: 'local',
    environmentRevision: state.revision, authRevision: 0, selectionRevision: 0 })
  return { ...result, jobId, report: history.readReport('probe', jobId) }
}
async function refresh(state, preview) {
  const current = generation.current()
  const jobId = id()
  const result = await run({ schemaVersion: 1, jobId, action: state.manifest.environments[0].sourceKind === 'openapi'
      ? 'contract:refresh-openapi' : 'contract:refresh-postman', environmentId: 'local',
    environmentRevision: state.revision, authRevision: 0, selectionRevision: 0,
    previewId: preview.previewId, expectedSnapshotHash: current.snapshotHash || null,
    expectedSourceFingerprint: preview.sourceFingerprint, acknowledgements: [] })
  return { ...result, jobId, report: history.readReport('refresh', jobId) }
}
async function isolatedProbe(handler, sourcePath) {
  const fixtureServer = createServer(handler)
  let fixturePort = 0
  await new Promise((resolve) => fixtureServer.listen(0, '127.0.0.1', () => { fixturePort = fixtureServer.address().port; resolve() }))
  const fixtureRoot = mkdtempSync(join(tmpdir(), 'backend-discovery-'))
  const fixtureContract = join(fixtureRoot, 'orchestrator', 'api-contract')
  mkdirSync(fixtureContract, { recursive: true })
  writeFileSync(join(fixtureRoot, 'orchestrator', 'project-config.md'), '---\nbackendContractEnabled: auto\n---\n')
  writeFileSync(join(fixtureContract, 'environments.json'), JSON.stringify({ schemaVersion: 1,
    environments: [{ id: 'local', label: 'Local', sourceKind: 'openapi', authKind: 'bearer',
      sourceUrl: `http://127.0.0.1:${fixturePort}${sourcePath}`, postmanEnrichmentUrl: null, authRef: null }],
    defaultEnvironmentId: 'local' }, null, 2) + '\n')
  try {
    const state = fixtureEnvironmentState(fixtureRoot)
    const jobId = id()
    const result = await run({ schemaVersion: 1, jobId, action: 'contract:probe', environmentId: 'local',
      environmentRevision: state.revision, authRevision: 0, selectionRevision: 0 }, fixtureRoot)
    const reportFile = join(fixtureRoot, 'orchestrator', '.cache', 'api-contract', 'reports', `probe-${jobId}.json`)
    return { ...result, report: JSON.parse(readFileSync(reportFile, 'utf8')), reportText: readFileSync(reportFile, 'utf8') }
  } finally {
    await new Promise((resolve) => fixtureServer.close(resolve))
    rmSync(fixtureRoot, { recursive: true, force: true })
  }
}
function allText(directory) {
  let out = ''
  for (const name of readdirSync(directory)) {
    const file = join(directory, name), st = statSync(file)
    if (st.isDirectory()) out += allText(file)
    else out += readFileSync(file, 'utf8')
  }
  return out
}

test('pinned requests disable Node address-family reselection', () => {
  const source = readFileSync(sidecar, 'utf8')
  assert.match(source, /autoSelectFamily:\s*false/)
  assert.match(source, /lookup:[^\n]+address\.address, address\.family/)
  assert.match(source, /boundedDnsLookup\(hostname, remaining\)/)
  assert.match(source, /child\.kill\('SIGKILL'\)/)
})

test('OpenAPI source resolver parses Swagger pages, initializer/config fixtures, conventions, and bounded deadlines without network', async () => {
  const base = 'https://api.example.test/docs'
  assert.equal(postmanUrlInfo(base), null)
  const nest = specUrlsFromHtml('<script>SwaggerUIBundle({ url: "/docs-json" })</script>', base)
  assert.deepEqual(nest.candidates.map((row) => row.url), ['https://api.example.test/docs-json'])

  const initializer = specUrlsFromHtml('<script src="/swagger-ui/swagger-initializer.js"></script>', base)
  assert.deepEqual(initializer.initializerUrls, ['https://api.example.test/swagger-ui/swagger-initializer.js'])

  const spring = specUrlsFromSwaggerConfig({ urls: [
    { url: '/v3/api-docs/public', name: 'Public' },
    { url: '/v3/api-docs/admin', name: 'Admin' },
  ], configUrl: '/v3/api-docs/swagger-config' }, base)
  assert.deepEqual(spring.candidates.map((row) => [row.url, row.title]), [
    ['https://api.example.test/v3/api-docs/public', 'Public'],
    ['https://api.example.test/v3/api-docs/admin', 'Admin'],
  ])
  assert.deepEqual(spring.configUrls, ['https://api.example.test/v3/api-docs/swagger-config'])
  assert.deepEqual(specUrlsFromHtml('<main id="spa-root"></main>', base).candidates, [])
  assert.ok(conventionCandidates(base).includes('https://api.example.test/docs-json'))

  const valid = JSON.stringify({ openapi: '3.1.0', info: { title: 'Initializer API' }, paths: {} })
  const calls = []
  const resolved = await runDiscovery({
    environment: { sourceUrl: base }, pinHost: 'api.example.test', deadlineAt: Date.now() + 1000,
    strictBody: '<script src="/swagger-ui/swagger-initializer.js"></script>',
    fetchFn: async (url, options) => {
      calls.push({ url, options })
      if (url.endsWith('swagger-initializer.js')) return { status: 200, text: 'SwaggerUIBundle({url:"/v3/api-docs"})', url }
      return { status: 200, text: valid, url }
    },
  })
  assert.equal(resolved.state, 'resolved')
  assert.equal(resolved.resolvedUrl, 'https://api.example.test/v3/api-docs')
  assert.equal(resolved.candidates[0].title, 'Initializer API')
  assert.ok(calls.every((row) => row.options.remaining > 0))

  const configCalls = []
  const resolvedFromConfig = await runDiscovery({
    environment: { sourceUrl: base }, pinHost: 'api.example.test', deadlineAt: Date.now() + 1000,
    strictBody: '<script>SwaggerUIBundle({url:"/bad.json",configUrl:"/swagger-config"})</script>',
    fetchFn: async (url) => {
      configCalls.push(url)
      if (url.endsWith('/swagger-config')) return { status: 200, text: JSON.stringify({ url: '/good.json' }), url }
      if (url.endsWith('/good.json')) return { status: 200, text: valid, url }
      return { status: 404, text: null, url }
    },
  })
  assert.equal(resolvedFromConfig.state, 'resolved')
  assert.equal(resolvedFromConfig.resolvedUrl, 'https://api.example.test/good.json')
  assert.deepEqual(configCalls, [
    'https://api.example.test/swagger-config',
    'https://api.example.test/bad.json',
    'https://api.example.test/good.json',
  ])

  let deadlineCalls = 0
  const started = Date.now()
  await assert.rejects(runDiscovery({
    environment: { sourceUrl: base }, pinHost: 'api.example.test', deadlineAt: Date.now() + 25,
    strictBody: '<script>SwaggerUIBundle({urls:[' + Array.from({ length: 12 }, (_, index) =>
      `{url:"/candidate-${index}.json"}`).join(',') + ']})</script>',
    fetchFn: async (url, options) => {
      deadlineCalls++
      await new Promise((resolve) => setTimeout(resolve, Math.min(12, options.remaining)))
      return { status: 404, text: null, url }
    },
  }), (error) => error.backendCode === 'source-unreachable')
  assert.ok(deadlineCalls <= 8)
  assert.ok(Date.now() - started < 150)
})

test('Postman source resolver classifies links and lists bounded validated collections through the injected constant-host fetch', async () => {
  const uid = '12345678-aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee'
  const api = postmanUrlInfo(`https://api.getpostman.com/collections/${uid}?ignored=yes#fragment`)
  assert.deepEqual(api, { detectedKind: 'postman', uid,
    resolvedUrl: `https://api.getpostman.com/collections/${uid}` })
  const shared = postmanUrlInfo(`https://www.postman.com/team/workspace/collection/${uid}/collection-name`)
  assert.equal(shared.uid, uid)
  assert.equal(shared.resolvedUrl, `https://api.getpostman.com/collections/${uid}`)
  assert.equal(postmanUrlInfo('https://www.postman.com/team/workspace/collection/i2uqzpp/postman-api').uid, null)
  assert.equal(postmanUrlInfo('https://www.postman.com').detectedKind, 'postman')
  assert.equal(postmanUrlInfo('https://plantin-team.postman.co/workspace/PlantIn/request/' + uid).detectedKind, 'postman')
  assert.equal(postmanUrlInfo('https://notpostman.co/workspace/collection/' + uid), null)
  assert.equal(postmanUrlInfo('https://example.test/collection/' + uid), null)

  const rows = Array.from({ length: 22 }, (_, index) => ({
    uid: `${10000000 + index}-aaaaaaaa-bbbb-cccc-dddd-${String(index).padStart(12, '0')}`,
    name: index === 0 ? '\u001bКоллекция\r' + 'x'.repeat(140) : `Collection ${index}`,
  }))
  rows.splice(4, 0, { uid: 'invalid-uid', name: 'Must not be shown' })
  const calls = []
  const listed = await runDiscovery({
    environment: { id: 'dev', sourceKind: 'postman', authKind: 'x-api-key', authRef: 'dev', sourceUrl: 'https://www.postman.com' },
    pinHost: 'www.postman.com', deadlineAt: Date.now() + 1000,
    fetchFn: async (url, options) => {
      calls.push({ url, options })
      return { status: 200, text: JSON.stringify({ collections: rows }), url }
    },
  })
  assert.equal(calls.length, 1)
  assert.equal(calls[0].url, 'https://api.getpostman.com/collections')
  assert.equal(calls[0].options.kind, 'postman')
  assert.equal(calls[0].options.allowRedirects, false)
  assert.equal(listed.state, 'ambiguous')
  assert.equal(listed.detectedKind, 'postman')
  assert.equal(listed.candidates.length, 20)
  assert.equal(listed.truncated, true)
  assert.equal(listed.candidates.some((row) => row.uid === 'invalid-uid'), false)
  assert.ok(listed.candidates[0].title.length <= 120)
  assert.equal(/[\u0000-\u001f\u007f-\u009f]/.test(listed.candidates[0].title), false)

  const empty = await runDiscovery({
    environment: { id: 'dev', sourceKind: 'postman', authKind: 'x-api-key', authRef: 'dev', sourceUrl: 'https://www.postman.com' },
    pinHost: 'www.postman.com', deadlineAt: Date.now() + 1000,
    fetchFn: async (url) => ({ status: 200, text: '{"collections":[]}', url }),
  })
  assert.equal(empty.state, 'unrecognized')
  assert.equal(empty.candidates, undefined)

  for (const status of [302, 429]) await assert.rejects(runDiscovery({
    environment: { id: 'dev', sourceKind: 'postman', authKind: 'x-api-key', authRef: 'dev', sourceUrl: 'https://www.postman.com' },
    pinHost: 'www.postman.com', deadlineAt: Date.now() + 1000,
    fetchFn: async (url) => ({ status, text: null, url }),
  }), (error) => error.backendCode === 'source-unreachable')
  await assert.rejects(runDiscovery({
    environment: { id: 'dev', sourceKind: 'postman', authKind: 'x-api-key', authRef: 'dev', sourceUrl: 'https://www.postman.com' },
    pinHost: 'www.postman.com', deadlineAt: Date.now() + 1000,
    fetchFn: async (url) => ({ status: 401, text: null, url }),
  }), (error) => error.backendCode === 'auth-rejected')

  for (const environment of [
    { id: 'local', sourceKind: 'postman', authKind: 'x-api-key', authRef: 'local', sourceUrl: 'http://127.0.0.1:8080/postman' },
    { id: 'dev', sourceKind: 'postman', authKind: 'bearer', authRef: 'dev', sourceUrl: 'https://www.postman.com' },
    { id: 'dev', sourceKind: 'postman', authKind: 'x-api-key', authRef: null, sourceUrl: 'https://www.postman.com' },
  ]) {
    let fetched = false
    const disabled = await runDiscovery({ environment, pinHost: new URL(environment.sourceUrl).host,
      deadlineAt: Date.now() + 1000, fetchFn: async () => { fetched = true; throw new Error('must not fetch') } })
    assert.equal(fetched, false)
    assert.equal(disabled.state, 'unrecognized')
    assert.equal(disabled.detectedKind, 'postman')
  }

  const sidecarSource = readFileSync(sidecar, 'utf8')
  assert.ok(sidecarSource.indexOf('postmanUrlInfo(environment.sourceUrl)') < sidecarSource.indexOf("fetchDocument(environment.sourceUrl, environment, credential, 'openapi'"))
  assert.match(sidecarSource, /postmanListing && \(environment\.authKind !== 'x-api-key' \|\| !credential \|\| !\/\^PMAK-/)
  assert.match(sidecarSource, /url\.hostname\.toLowerCase\(\) === POSTMAN_API_HOST && authKind !== 'x-api-key' \? null : credential/)
})

test('sidecar selects the credential header by authKind and rejects a non-PMAK API key before network', async () => {
  const authRoot = mkdtempSync(join(tmpdir(), 'backend-auth-kind-'))
  const authContractDir = join(authRoot, 'orchestrator', 'api-contract')
  const credentialModule = join(REPO, 'orchestrator', 'site', 'server', 'backend-credentials.js')
  mkdirSync(authContractDir, { recursive: true })
  writeFileSync(join(authRoot, 'orchestrator', 'project-config.md'), '---\nbackendContractEnabled: auto\n---\n')
  function writeAuthEnvironment(authKind, sourcePath = '/openapi.json', enrichmentPath = null) {
    writeFileSync(join(authContractDir, 'environments.json'), JSON.stringify({ schemaVersion: 1,
      environments: [{ id: 'local', label: 'Local', sourceKind: 'openapi', authKind,
        sourceUrl: `https://127.0.0.1:${securePort}${sourcePath}`,
        postmanEnrichmentUrl: enrichmentPath ? `https://127.0.0.1:${securePort}${enrichmentPath}` : null,
        authRef: 'local' }],
      defaultEnvironmentId: 'local' }, null, 2) + '\n')
    const state = fixtureEnvironmentState(authRoot)
    assert.equal(state.mode, 'manifest')
    return state
  }
  async function authenticatedProbe(state, authRevision) {
    const jobId = id()
    const result = await run({ schemaVersion: 1, jobId, action: 'contract:probe', environmentId: 'local',
      environmentRevision: state.revision, authRevision, selectionRevision: 0 }, authRoot,
    { NODE_TLS_REJECT_UNAUTHORIZED: '0' })
    const reportFile = join(authRoot, 'orchestrator', '.cache', 'api-contract', 'reports', `probe-${jobId}.json`)
    return { ...result, report: JSON.parse(readFileSync(reportFile, 'utf8')) }
  }
  try {
    let state = writeAuthEnvironment('x-api-key')
    let credential = fixtureModuleCall(authRoot, credentialModule, 'mutate', { environmentId: 'local', operation: 'set',
      secret: 'PMAK-test-api-key', expectedAuthRevision: 0, idempotencyKey: 'credential:x-api-key' })
    assert.equal(credential.ok, true)
    let before = secureRequests.length
    let tested = await authenticatedProbe(state, credential.revision)
    assert.equal(tested.code, 0, tested.stderr || tested.stdout)
    assert.equal(secureRequests.length, before + 1)
    assert.equal(secureRequests.at(-1).headers['x-api-key'], 'PMAK-test-api-key')
    assert.equal(secureRequests.at(-1).headers.authorization, undefined)

    state = writeAuthEnvironment('x-api-key', '/openapi.json', '/postman.json')
    before = secureRequests.length
    tested = await authenticatedProbe(state, credential.revision)
    assert.equal(tested.code, 0, tested.stderr || tested.stdout)
    const enrichedRequests = secureRequests.slice(before)
    assert.deepEqual(enrichedRequests.map((row) => row.url), ['/openapi.json', '/postman.json'])
    assert.equal(enrichedRequests[0].headers['x-api-key'], REFLECTED_CREDENTIAL)
    assert.equal(enrichedRequests[1].headers['x-api-key'], undefined)
    assert.equal(enrichedRequests[1].headers.authorization, undefined)

    state = writeAuthEnvironment('x-api-key', '/credential-docs')
    tested = await authenticatedProbe(state, credential.revision)
    assert.equal(tested.code, 1)
    assert.equal(tested.report.error.code, 'source-content-type')
    assert.equal(tested.report.resolution.state, 'unrecognized')
    assert.equal(JSON.stringify(tested.report).includes(REFLECTED_CREDENTIAL), false)

    state = writeAuthEnvironment('bearer')
    before = secureRequests.length
    tested = await authenticatedProbe(state, credential.revision)
    assert.equal(tested.code, 0, tested.stderr || tested.stdout)
    assert.equal(secureRequests.length, before + 1)
    assert.equal(secureRequests.at(-1).headers.authorization, 'Bearer PMAK-test-api-key')
    assert.equal(secureRequests.at(-1).headers['x-api-key'], undefined)

    credential = fixtureModuleCall(authRoot, credentialModule, 'mutate', { environmentId: 'local', operation: 'set',
      secret: 'old-backend-bearer', expectedAuthRevision: credential.revision, idempotencyKey: 'credential:old-bearer' })
    assert.equal(credential.ok, true)
    state = writeAuthEnvironment('x-api-key')
    before = secureRequests.length
    tested = await authenticatedProbe(state, credential.revision)
    assert.equal(tested.code, 1)
    assert.equal(tested.report.error.code, 'auth-invalid')
    assert.equal(secureRequests.length, before)
  } finally {
    rmSync(authRoot, { recursive: true, force: true })
  }
})

test('probe is read-only and refresh publishes a coherent OpenAPI generation', async () => {
  const state = createFirstEnvironment('openapi', '/openapi.json')
  const tested = await probe(state)
  assert.equal(tested.code, 0, tested.stderr || tested.stdout)
  assert.equal(tested.report.state, 'success')
  assert.equal(tested.report.selectionRevision, 0)
  assert.equal(generation.current().mode, 'none')
  const finalizationRuntime = join(root, 'orchestrator', '.cache', 'tasks', 'finalizations')
  const creationRuntime = join(root, 'orchestrator', '.cache', 'tasks', 'creations')
  mkdirSync(finalizationRuntime, { recursive: true })
  mkdirSync(creationRuntime, { recursive: true })
  writeFileSync(join(finalizationRuntime, '.task-state-replace.lock'), '')
  writeFileSync(join(finalizationRuntime, 'TASK_999_benign.draft.md'), '')
  writeFileSync(join(creationRuntime, '.events.jsonl'), '')
  writeFileSync(join(creationRuntime, '.mutex'), '')
  const refreshed = await refresh(state, tested.report)
  assert.equal(refreshed.code, 0, refreshed.stderr || refreshed.stdout)
  assert.ok(['success', 'partial'].includes(refreshed.report.state))
  const current = generation.current()
  assert.equal(current.ok, true)
  assert.equal(current.mode, 'generation')
  assert.equal(current.manifest.generationId, refreshed.report.committedGenerationId)
  assert.equal(current.snapshotHash, refreshed.report.currentHash)
  assert.equal(current.environmentId, 'local')
  const reportsDir = join(root, 'orchestrator', '.cache', 'api-contract', 'reports')
  const implementation = JSON.parse(readFileSync(join(reportsDir, 'implementation-map.json'), 'utf8'))
  const consumers = JSON.parse(readFileSync(join(reportsDir, 'consumer-map.json'), 'utf8'))
  assert.equal(implementation.committedGenerationId, current.manifest.generationId)
  assert.equal(implementation.contractHash, current.snapshotHash)
  assert.match(implementation.projectCodeRevision, /^sha256:[a-f0-9]{64}$/)
  assert.equal(consumers.committedGenerationId, current.manifest.generationId)
  assert.equal(consumers.analysisStatus, 'partial')
  assert.ok(consumers.limitations.includes('static-consumer-analysis-not-conclusive'))
  const changeSet = JSON.parse(readFileSync(current.artifacts['change-report'], 'utf8'))
  assert.equal(changeSet.schemaVersion, 2)
  assert.equal(current.manifest.artifacts.find((row) => row.role === 'change-report').schemaVersion, 2)
  assert.equal(changeSet.committedGenerationId, current.manifest.generationId)
  assert.equal(changeSet.currentHash, current.snapshotHash)
  const firstChangeFile = current.artifacts['change-report']
  const firstChangeBytes = readFileSync(firstChangeFile)
  const repeatedProbe = await probe(state)
  assert.equal(repeatedProbe.report.state, 'success')
  assert.deepEqual(repeatedProbe.report.delta, { added: 0, changed: 0, removed: 0, potentiallyBreaking: 0 })
  assert.equal(generation.current().manifest.generationId, refreshed.report.committedGenerationId)
  const noOpRefresh = await refresh(state, repeatedProbe.report)
  assert.equal(noOpRefresh.code, 0, noOpRefresh.stderr || noOpRefresh.stdout)
  const noOpCurrent = generation.current()
  assert.notEqual(noOpCurrent.manifest.generationId, current.manifest.generationId)
  assert.notEqual(noOpCurrent.artifacts['change-report'], firstChangeFile)
  assert.deepEqual(readFileSync(firstChangeFile), firstChangeBytes)
  firstRefresh = noOpRefresh
})

test('restart recovery preserves a durable refresh whose generation is already current', async () => {
  assert.ok(firstRefresh && firstRefresh.report.committedGenerationId)
  const startedAt = firstRefresh.report.startedAt
  await history.upsert({ jobId: firstRefresh.jobId, reportType: 'refresh', environmentId: 'local', state: 'running',
    startedAt, finishedAt: null, idempotencyKey: 'recovery:published-refresh',
    idempotencyFingerprint: 'sha256:' + 'a'.repeat(64) })
  assert.equal(await history.recoverInterrupted(), true)
  const row = history._readIndexForTests().jobs.find((item) => item.jobId === firstRefresh.jobId)
  assert.equal(row.state, firstRefresh.report.state)
  assert.equal(row.finishedAt, firstRefresh.report.finishedAt)
  assert.equal(generation.current().ok, true)
})

test('refresh rejects an environment selection revision changed after preview', async () => {
  openapi = JSON.parse(JSON.stringify(originalOpenapi))
  const state = writeEnvironment('openapi', '/openapi.json')
  const tested = await probe(state)
  assert.equal(tested.report.state, 'success')
  const pointerBefore = readFileSync(generation.POINTER_FILE)
  const persisted = persistence.readPersisted()
  persisted.backendActiveEnvironmentId = 'local'
  persisted.backendSelectionRevision = 1
  persistence.writePersisted(persisted)
  try {
    const refreshed = await refresh(state, tested.report)
    assert.equal(refreshed.code, 1)
    assert.equal(refreshed.report.error.code, 'preview-stale')
    assert.deepEqual(readFileSync(generation.POINTER_FILE), pointerBefore)
  } finally {
    const restored = persistence.readPersisted()
    restored.backendActiveEnvironmentId = ''
    restored.backendSelectionRevision = 0
    persistence.writePersisted(restored)
  }
})

test('refresh rejects a source changed after preview and leaves the pointer unchanged', async () => {
  openapi = JSON.parse(JSON.stringify(originalOpenapi))
  const state = writeEnvironment('openapi', '/openapi.json')
  const tested = await probe(state)
  assert.equal(tested.report.state, 'success')
  const pointerBefore = readFileSync(generation.POINTER_FILE)
  openapi.paths['/changed-after-probe'] = { get: { operationId: 'getChangedAfterProbe', responses: { 200: { description: 'ok' } } } }
  const refreshed = await refresh(state, tested.report)
  assert.equal(refreshed.code, 1)
  assert.equal(refreshed.report.state, 'failed')
  assert.equal(refreshed.report.error.code, 'source-changed')
  assert.equal(refreshed.report.committedGenerationId, null)
  assert.deepEqual(readFileSync(generation.POINTER_FILE), pointerBefore)
  openapi = JSON.parse(JSON.stringify(originalOpenapi))
})

test('redirect, HTTP error and size policies return typed redacted failures', async () => {
  let state = writeEnvironment('openapi', '/redirect')
  assert.equal((await probe(state)).report.state, 'success')
  state = writeEnvironment('openapi', '/bad-redirect')
  assert.equal((await probe(state)).report.error.code, 'source-redirect-forbidden')
  state = writeEnvironment('openapi', '/unauthorized')
  assert.equal((await probe(state)).report.error.code, 'auth-rejected')
  state = writeEnvironment('openapi', '/huge')
  assert.equal((await probe(state)).report.error.code, 'source-too-large')
  state = writeEnvironment('openapi', '/wrong-type')
  const wrongType = await probe(state)
  assert.equal(wrongType.code, 1)
  assert.equal(wrongType.report.error.code, 'source-content-type')
  assert.equal(wrongType.report.resolution.state, 'resolved')
  assert.equal(wrongType.report.resolution.resolvedUrl, `http://127.0.0.1:${port}/openapi.json`)
  assert.equal(wrongType.report.environmentRevision, state.revision)
  assert.equal(wrongType.report.authRevision, 0)
  assert.equal(wrongType.report.selectionRevision, 0)
  assert.ok(Number.isFinite(Date.parse(wrongType.report.checkedAt)))
  assert.match(wrongType.stdout, /"state":"failed"/)
  state = writeEnvironment('openapi', '/redirect-loop')
  assert.equal((await probe(state)).report.error.code, 'source-redirect-limit')
})

test('OpenAPI 3.0/3.1 are accepted and malformed or Swagger 2 inputs fail closed', async () => {
  let state = writeEnvironment('openapi', '/openapi-3.0.json')
  assert.equal((await probe(state)).report.state, 'success')
  state = writeEnvironment('openapi', '/invalid.json')
  assert.equal((await probe(state)).report.error.code, 'invalid-openapi')
  state = writeEnvironment('openapi', '/invalid.yaml')
  assert.equal((await probe(state)).report.error.code, 'invalid-openapi')
  state = writeEnvironment('openapi', '/openapi-2.json')
  const swagger2 = await probe(state)
  assert.equal(swagger2.report.error.code, 'invalid-openapi')
  assert.equal(swagger2.report.resolution.reason, 'openapi-2-unsupported')
})

test('sidecar discovers linked and ambiguous OpenAPI sources while preserving the original strict failure on disk', async () => {
  let state = writeEnvironment('openapi', '/docs')
  let tested = await probe(state)
  assert.equal(tested.code, 1)
  assert.equal(tested.report.error.code, 'source-content-type')
  assert.equal(tested.report.resolution.state, 'resolved')
  assert.equal(tested.report.resolution.resolvedUrl, `http://127.0.0.1:${port}/linked-openapi.json`)
  assert.equal(tested.report.resolution.candidates[0].title, 'Linked API')
  const reportFile = join(root, 'orchestrator', '.cache', 'api-contract', 'reports', `probe-${tested.jobId}.json`)
  const reportOnDisk = JSON.parse(readFileSync(reportFile, 'utf8'))
  assert.deepEqual(reportOnDisk.resolution, tested.report.resolution)

  state = writeEnvironment('openapi', '/multi-docs')
  tested = await probe(state)
  assert.equal(tested.report.error.code, 'source-content-type')
  assert.equal(tested.report.resolution.state, 'ambiguous')
  assert.deepEqual(tested.report.resolution.candidates.map((row) => row.title), ['One', 'Two'])
})

test('discovery rejects SPA catch-alls, isolates candidate errors, strips unsafe hints, and enforces the aggregate request budget', async () => {
  let tested = await isolatedProbe((_req, res) => {
    res.writeHead(200, { 'content-type': 'text/html' }); res.end('<main id="spa"></main>')
  }, '/docs')
  assert.equal(tested.code, 1)
  assert.equal(tested.report.error.code, 'source-content-type')
  assert.equal(tested.report.resolution.state, 'unrecognized')

  const unsafe = '\u001b[31mUnsafe\rTitle'
  tested = await isolatedProbe((req, res) => {
    if (req.url === '/docs') {
      res.writeHead(200, { 'content-type': 'text/html' })
      res.end(`<script>SwaggerUIBundle({urls:[{url:"/openapi.json?token=SECRET_QUERY",name:"${unsafe}"}]})</script>`); return
    }
    res.writeHead(200, { 'content-type': 'application/json' })
    res.end(JSON.stringify({ openapi: '3.1.0', info: { title: unsafe }, paths: {} }))
  }, '/docs')
  assert.equal(tested.report.error.code, 'source-content-type')
  assert.equal(tested.report.resolution.state, 'unrecognized')
  assert.equal(tested.reportText.includes('SECRET_QUERY'), false)
  assert.equal(/[\u0000-\u001f\u007f-\u009f]/.test(JSON.stringify(tested.report.resolution)), false)

  const state = writeEnvironment('openapi', '/r2-docs')
  tested = await probe(state)
  assert.equal(tested.report.error.code, 'source-content-type')
  assert.equal(tested.report.resolution.state, 'unrecognized')
  assert.equal(tested.report.resolution.reason, 'auth-required')

  const requests = []
  let sourceHits = 0
  tested = await isolatedProbe((req, res) => {
    requests.push(req.url)
    if (req.url === '/budget') {
      sourceHits++
      if (sourceHits === 1) { res.writeHead(200, { 'content-type': 'text/html' }); res.end('<html></html>'); return }
      res.writeHead(302, { location: '/budget/' }); res.end(); return
    }
    if (req.url === '/budget/') {
      const urls = Array.from({ length: 12 }, (_, index) => `{url:"/spec-${index}.json"}`).join(',')
      res.writeHead(200, { 'content-type': 'text/html' }); res.end(`<script>SwaggerUIBundle({urls:[${urls}]})</script>`); return
    }
    if (/^\/spec-\d+\.json$/.test(req.url)) {
      res.writeHead(200, { 'content-type': 'application/json' })
      res.end(JSON.stringify({ openapi: '3.1.0', info: { title: req.url }, paths: {} })); return
    }
    res.writeHead(404); res.end()
  }, '/budget')
  assert.equal(tested.report.error.code, 'source-content-type')
  assert.equal(tested.report.resolution.state, 'ambiguous')
  assert.ok(requests.length - 1 <= 8, requests.join(', '))
  assert.ok(tested.report.resolution.probedPaths.length <= 8)
})

test('headless refresh fails when the selected source is unavailable', async () => {
  const cli = join(REPO, 'orchestrator', 'api-contract', 'scripts', 'backend-cli.mjs')
  const invoke = () => runCommand(cli, ['refresh-openapi'])

  writeEnvironment('openapi', '/invalid.json')
  const invalid = await invoke()
  assert.equal(invalid.status, 1, invalid.stderr + invalid.stdout)
  assert.match(invalid.stderr, /invalid-openapi/)
  assert.match(invalid.stderr, /HINT Backend found/)

  writeEnvironment('openapi', '/missing')
  const unavailable = await invoke()
  assert.equal(unavailable.status, 1, unavailable.stderr + unavailable.stdout)
  assert.match(unavailable.stderr, /source-unreachable/)

  assert.equal(generation.current().ok, true)
})

test('contract command entrypoints reject unknown, duplicate, and incomplete arguments', () => {
  const scripts = join(REPO, 'orchestrator', 'api-contract', 'scripts')
  const cases = [
    ['backend-action.mjs', ['--unknown']],
    ['backend-cli.mjs', ['probe', '--unknown']],
    ['backend-cli.mjs', ['probe', '--best-effort']],
    ['backend-cli.mjs', ['refresh-openapi', '--environment']],
    ['backend-cli.mjs', ['refresh-openapi', '--environment', 'local', '--environment', 'local']],
    ['import-postman.mjs', ['--unknown']],
    ['import-postman.mjs', ['--file']],
    ['normalize.mjs', ['--unknown']],
    ['normalize.mjs', ['--input']],
    ['doctor.mjs', ['--unknown']],
    ['diff.mjs', ['--unknown']],
    ['verify.mjs', ['--unknown']],
    ['suggest-endpoint-tasks.mjs', ['--unknown']],
  ]
  for (const [script, args] of cases) {
    const result = spawnSync(process.execPath, [join(scripts, script), ...args], {
      cwd: root,
      env: { ...process.env, ORCHESTRATOR_PROJECT_ROOT: root },
      encoding: 'utf8',
    })
    assert.equal(result.status, 2, `${script} ${args.join(' ')}\n${result.stderr}${result.stdout}`)
  }

})

test('standalone normalize cannot create a workspace snapshot', () => {
  const normalize = join(REPO, 'orchestrator', 'api-contract', 'scripts', 'normalize.mjs')
  const input = join(REPO, 'orchestrator', 'api-contract', 'scripts', 'examples', 'openapi.sample.json')
  const result = spawnSync(process.execPath, [normalize, '--input', input], {
    cwd: root,
    env: { ...process.env, ORCHESTRATOR_PROJECT_ROOT: root },
    encoding: 'utf8',
  })
  assert.equal(result.status, 2, result.stderr + result.stdout)
  assert.match(result.stderr, /internal to the typed Backend staging flow/)
  assert.equal(existsSync(join(contractDir, 'manifests', 'endpoint-inventory.json')), false)

  const overrideAttempt = spawnSync(process.execPath, [normalize, '--input', input], {
    cwd: root,
    env: { ...process.env, ORCHESTRATOR_PROJECT_ROOT: root, ORCHESTRATOR_API_CONTRACT_DATA_DIR: contractDir,
      ORCHESTRATOR_API_CONTRACT_CACHE_DIR: join(root, 'cache') },
    encoding: 'utf8',
  })
  assert.notEqual(overrideAttempt.status, 0)
  assert.match(overrideAttempt.stderr, /reserved for sidecar-owned staging/)
  assert.equal(existsSync(join(contractDir, 'manifests', 'endpoint-inventory.json')), false)

  const cacheOnlyAttempt = spawnSync(process.execPath, [normalize, '--input', input], {
    cwd: root,
    env: { ...process.env, ORCHESTRATOR_PROJECT_ROOT: root, ORCHESTRATOR_API_CONTRACT_CACHE_DIR: contractDir },
    encoding: 'utf8',
  })
  assert.notEqual(cacheOnlyAttempt.status, 0)
  assert.match(cacheOnlyAttempt.stderr, /staging data\/cache overrides must be supplied together/)
  assert.equal(existsSync(join(contractDir, 'manifests', 'endpoint-inventory.json')), false)
})

test('Postman API envelopes unwrap only for the primary source and never for enrichment', async () => {
  let state = writeEnvironment('postman', '/postman.json')
  let tested = await probe(state)
  assert.equal(tested.code, 0, tested.stderr || tested.stdout)
  assert.equal(tested.report.state, 'success')

  state = writeEnvironment('postman', '/postman-envelope.json')
  tested = await probe(state)
  assert.equal(tested.code, 0, tested.stderr || tested.stdout)
  assert.equal(tested.report.state, 'success')

  writeFileSync(join(contractDir, 'environments.json'), JSON.stringify({ schemaVersion: 1,
    environments: [{ id: 'local', label: 'Local', sourceKind: 'openapi', authKind: 'bearer',
      sourceUrl: `http://127.0.0.1:${port}/openapi.json`,
      postmanEnrichmentUrl: `http://127.0.0.1:${port}/postman-envelope.json`, authRef: null }],
    defaultEnvironmentId: 'local' }, null, 2) + '\n')
  state = environments.read()
  assert.equal(state.mode, 'manifest')
  tested = await probe(state)
  assert.equal(tested.code, 0, tested.stderr || tested.stdout)
  assert.equal(tested.report.state, 'success')
  assert.deepEqual(tested.report.warnings, [{ code: 'enrichment-unavailable',
    message: 'Base OpenAPI is valid; Postman enrichment could not be tested.' }])
})

test('Postman bootstrap commits no raw collection or secret-bearing examples', async () => {
  const state = writeEnvironment('postman', '/postman.json')
  const tested = await probe(state)
  assert.equal(tested.report.state, 'success')
  const refreshed = await refresh(state, tested.report)
  assert.equal(refreshed.code, 0, refreshed.stderr || refreshed.stdout)
  const current = generation.current()
  assert.equal(current.ok, true)
  assert.equal(current.manifest.sourceKind, 'postman')
  assert.ok(current.artifacts['source-descriptor'])
  assert.equal(current.artifacts['normalized-spec'], undefined)
  const generationDir = join(contractDir, 'manifests', 'generation-artifacts', current.manifest.generationId)
  const committed = allText(generationDir)
  for (const marker of ['MUST_NOT_COMMIT_VARIABLE', 'MUST_NOT_COMMIT_HEADER', 'MUST_NOT_COMMIT_BODY']) {
    assert.equal(committed.includes(marker), false)
  }
})
