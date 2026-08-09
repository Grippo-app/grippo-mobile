// Headless wrapper around the same typed sidecar used by the site. It never
// accepts a URL or credential. Sources resolve only through environments.json.

import { randomBytes } from 'node:crypto'
import { spawnSync } from 'node:child_process'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { createRequire } from 'node:module'

const HERE = dirname(fileURLToPath(import.meta.url))
const PROJECT_ROOT = resolve(process.env.ORCHESTRATOR_PROJECT_ROOT || resolve(HERE, '..', '..', '..'))
process.env.ORCHESTRATOR_PROJECT_ROOT = PROJECT_ROOT
const require = createRequire(import.meta.url)
const serverDir = resolve(HERE, '..', '..', 'site', 'server')
const environments = require(join(serverDir, 'backend-environments.js'))
const credentials = require(join(serverDir, 'backend-credentials.js'))
const generation = require(join(serverDir, 'contract-generation.js'))
const history = require(join(serverDir, 'contract-history.js'))
const persistence = require(join(serverDir, 'persistence.js'))

function die(message) {
  console.error(`FAIL ${message}`)
  process.exit(1)
}
function usage(message) {
  console.error(`FAIL ${message}\nusage: backend-cli.mjs probe|refresh-openapi|refresh-postman [--environment id] [--refresh-base-without-enrichment]`)
  process.exit(2)
}
function parseArgs(argv) {
  let verb = 'probe', index = 0
  if (argv[0] && !argv[0].startsWith('--')) { verb = argv[0]; index = 1 }
  if (!['probe', 'refresh-openapi', 'refresh-postman'].includes(verb)) usage(`unknown action ${verb}`)
  const out = { verb, environmentId: null, baseOnly: false }
  const seen = new Set()
  while (index < argv.length) {
    const flag = argv[index++]
    if (!['--environment', '--refresh-base-without-enrichment'].includes(flag)) usage(`unknown argument ${flag}`)
    if (seen.has(flag)) usage(`duplicate argument ${flag}`)
    seen.add(flag)
    if (flag === '--environment') {
      const value = argv[index++]
      if (!value || value.startsWith('--')) usage('--environment requires an id')
      out.environmentId = value
    } else out.baseOnly = true
  }
  if (out.baseOnly && verb !== 'refresh-openapi') usage('--refresh-base-without-enrichment is valid only for refresh-openapi')
  return out
}
function jobId() { return 'job-' + randomBytes(16).toString('hex') }
function run(request) {
  const result = spawnSync(process.execPath, [join(HERE, 'backend-action.mjs')], {
    cwd: PROJECT_ROOT, env: { ...process.env, ORCHESTRATOR_PROJECT_ROOT: PROJECT_ROOT },
    input: JSON.stringify(request), encoding: 'utf8', maxBuffer: 4 * 1024 * 1024,
  })
  if (result.stdout) process.stdout.write(result.stdout)
  if (result.status !== 0) {
    const reportType = request.action === 'contract:probe' ? 'probe' : 'refresh'
    const report = history.readReport(reportType, request.jobId)
    const code = report && report.error && report.error.code
    const resolution = report && report.resolution
    if (resolution) {
      if (resolution.resolvedUrl) console.error(`HINT Backend found ${resolution.resolvedUrl}; apply it in Integrations -> Backend and test again.`)
      else if (Array.isArray(resolution.candidates) && resolution.candidates.length) {
        console.error(`HINT Backend found ${resolution.candidates.length} possible sources; choose one in Integrations -> Backend.`)
      } else console.error('HINT Open Integrations -> Backend for the source-resolution guidance.')
    }
    die(`typed Backend sidecar failed${code ? ` (${code})` : ''}; inspect its versioned runtime report`)
  }
}

const { verb, environmentId: environmentIdArg, baseOnly } = parseArgs(process.argv.slice(2))

const envState = environments.read()
if (envState.mode !== 'manifest') die('environments.json is required for typed Backend actions')
const environmentId = environmentIdArg || envState.manifest.defaultEnvironmentId
const environment = environments.environmentById(envState, environmentId)
if (!environment) die(`environment ${environmentId} does not exist`)
if (verb === 'refresh-openapi' && environment.sourceKind !== 'openapi') die('selected environment is not OpenAPI-backed')
if (verb === 'refresh-postman' && environment.sourceKind !== 'postman') die('selected environment is not a Postman bootstrap source')
const auth = credentials.publicStatus(environment)
if (auth.state === 'missing' || auth.state === 'invalid') die(`credential state is ${auth.state}`)

const probeJobId = jobId()
const selectionRevision = persistence.readPersisted().backendSelectionRevision || 0
run({ schemaVersion: 1, jobId: probeJobId, action: 'contract:probe', environmentId,
  environmentRevision: envState.revision, authRevision: auth.revision, selectionRevision })
const preview = history.readReport('probe', probeJobId)
if (!preview || preview.state !== 'success') die('probe did not produce a valid preview')
if (verb === 'probe') {
  console.log(`PASS preview ${preview.previewId}: ${preview.delta.added} added, ${preview.delta.changed} changed, ${preview.delta.removed} removed`)
  process.exit(0)
}

const current = generation.current()
if (!current.ok) die(`current generation is invalid (${current.error})`)
const refreshJobId = jobId()
run({ schemaVersion: 1, jobId: refreshJobId,
  action: environment.sourceKind === 'openapi' ? 'contract:refresh-openapi' : 'contract:refresh-postman',
  environmentId, environmentRevision: envState.revision, authRevision: auth.revision,
  selectionRevision,
  previewId: preview.previewId, expectedSnapshotHash: current.snapshotHash || null,
  expectedSourceFingerprint: preview.sourceFingerprint,
  acknowledgements: baseOnly ? ['refresh-base-without-enrichment'] : [] })
const result = history.readReport('refresh', refreshJobId)
if (!result || !result.committedGenerationId) die('refresh did not publish a committed generation')
console.log(`PASS generation ${result.committedGenerationId}: ${result.addedEndpoints} added, ${result.changedEndpoints} changed, ${result.removedEndpoints} removed`)
