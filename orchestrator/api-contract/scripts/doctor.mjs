// contract:doctor — health check for the sidecar. Never calls the backend. PASS/WARN/FAIL, exit 0/1.
import { PROJECT_ROOT, ok, warnMsg, failMsg, info, summary, exists, readJson, contractPath, currentContractFiles, readConfig } from './_util.mjs'
import { statSync, readdirSync } from 'node:fs'
import { createRequire } from 'node:module'
import { join } from 'node:path'

if (process.argv.length !== 2) {
  process.stderr.write('contract:doctor accepts no arguments\n')
  process.exit(2)
}

// Toolchain — package.json is the sole runtime contract. A below-floor Node
// cannot be treated as an advisory state because npm scripts may then fail in
// a later, unrelated-looking step.
const major = Number(process.versions.node.split('.')[0])
let engineFloor = null
try {
  const declared = readJson(contractPath('package.json'))?.engines?.node
  const match = /^>=(\d+)$/.exec(String(declared || ''))
  if (!match) throw new Error('engines.node must use the exact >=<major> contract')
  engineFloor = Number(match[1])
  ok('package.json present and valid')
} catch (error) {
  failMsg(`package.json missing or invalid (${error.message})`)
}
if (engineFloor !== null) {
  major >= engineFloor
    ? ok(`Node ${process.versions.node} (>=${engineFloor})`)
    : failMsg(`Node ${process.versions.node} below engines.node floor >=${engineFloor}`)
}

// Committed structure (reports/ now lives under orchestrator/.cache/api-contract/reports — runtime-created, not committed)
for (const d of ['scripts', 'contract-schemas', 'manifests']) {
  exists(contractPath(d)) ? ok(`${d}/ present`) : failMsg(`${d}/ missing`)
}
const schemaFiles = ['inventory.schema.json', 'area.schema.json', 'environments.schema.json',
  'credential-state.schema.json', 'generation-manifest.schema.json', 'generation-pointer.schema.json',
  'change-set.schema.json', 'implementation-map.schema.json', 'consumer-map.schema.json']
for (const f of schemaFiles) {
  exists(contractPath('contract-schemas', f)) ? ok(`${f} present`) : failMsg(`${f} missing`)
}

// Config gate (tri-state)
const enabled = readConfig('backendContractEnabled')
if (enabled === undefined) failMsg('backendContractEnabled not found in project-config.md')
else if (enabled === 'true') ok('backendContractEnabled: true (snapshot required)')
else if (enabled === 'auto') ok('backendContractEnabled: auto (validated snapshot used when present; snapshot absence is non-blocking)')
else if (enabled === 'false') info('backendContractEnabled: false — contract tooling is off; nothing to run')
else failMsg(`backendContractEnabled: ${enabled} (expected auto/true/false)`)

const environmentsFile = contractPath('environments.json')
let environmentState = null
if (exists(environmentsFile)) {
  try {
    const require = createRequire(import.meta.url)
    const serverDir = join(PROJECT_ROOT, 'orchestrator', 'site', 'server')
    const environmentStore = require(join(serverDir, 'backend-environments.js'))
    const credentialStore = require(join(serverDir, 'backend-credentials.js'))
    environmentState = environmentStore.read()
    if (environmentState.mode !== 'manifest') {
      const errorCode = typeof environmentState.error === 'string' && environmentState.error
        ? environmentState.error
        : 'environment-reader-contract-invalid'
      failMsg(`environments.json is invalid (${errorCode})`)
    }
    else {
      ok(`environments.json valid (${environmentState.manifest.environments.length} canonical source(s))`)
      for (const environment of environmentState.manifest.environments) {
        const status = credentialStore.publicStatus(environment)
        if (status.state === 'invalid') failMsg(`${environment.id} credential file is unsafe or invalid`)
        else if (status.state === 'missing') warnMsg(`${environment.id} requires a credential but none is configured`)
        else ok(`${environment.id} authentication: ${status.state}`)
      }
    }
  } catch (e) { failMsg(`canonical Backend configuration is unreadable: ${e.message}`) }
} else {
  warnMsg('environments.json is absent; configure a Backend source before probing or refreshing')
}

let AjvCtor = null
try { ({ default: AjvCtor } = await import('ajv')) } catch (e) {
  failMsg(`ajv unavailable; the current contract pipeline requires schema validation — run root \`npm ci\` (${e.message})`)
}
if (AjvCtor) {
  try {
    const Ajv = AjvCtor
    const ajv = new Ajv({ allErrors: true })
    ajv.addFormat('date-time', { type: 'string', validate: (value) =>
      /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,9})?Z$/.test(value) && Number.isFinite(Date.parse(value)) })
    let validateEnvironments = null
    for (const f of schemaFiles) {
      const validate = ajv.compile(readJson(contractPath('contract-schemas', f)))
      if (f === 'environments.schema.json') validateEnvironments = validate
      ok(`${f} compiles`)
    }
    if (exists(environmentsFile) && validateEnvironments) {
      const instance = readJson(environmentsFile)
      validateEnvironments(instance)
        ? ok('environments.json is schema-valid')
        : failMsg(`environments.json fails its schema: ${(validateEnvironments.errors || []).slice(0, 3).map((e) => `${e.instancePath || '/'} ${e.message}`).join('; ')}`)
    }
  } catch (e) {
    failMsg(`contract schema file invalid/unreadable: ${e.message}`)
  }
}

// Snapshot presence + age
const current = currentContractFiles()
if (current.invalid) failMsg(`current generation is invalid (${current.error})`)
const invPath = current.inventory
let inventory = null
if (!invPath || !exists(invPath)) {
  if (enabled === 'true') failMsg('no valid snapshot generation but backendContractEnabled: true REQUIRES one — configure environments.json, then Test + Refresh in Backend (or run the matching typed contract:probe/contract:refresh-* commands)')
  else info('no valid snapshot generation — configure environments.json, then Test + Refresh in Backend (or use the typed contract:probe/contract:refresh-* commands)')
} else {
  try { inventory = readJson(invPath) } catch (e) { failMsg(`endpoint-inventory.json unreadable: ${e.message}`) }
}
if (inventory) {
  const src = inventory.source || {}
  ok(`snapshot present (kind ${src.kind}, ${((inventory.stats || {}).endpoints) ?? '?'} endpoints)`)
  if (src.fetchedAt) {
    const days = Math.floor((Date.now() - Date.parse(src.fetchedAt)) / 86400000)
    if (Number.isFinite(days) && days > 14) warnMsg(`snapshot is ${days} days old (fetched ${src.fetchedAt}) — the backend may have moved; Test the source and refresh from a fresh preview`)
    else ok(`snapshot age: ${Number.isFinite(days) ? `${days} day(s)` : 'unknown'}`)
  } else info('snapshot has no fetchedAt (Postman source); switch the environment to OpenAPI when a spec exists')

  // Quick ajv validity (inventory only — contract:verify does the full pass)
    if (AjvCtor) {
      try {
        const Ajv = AjvCtor
        const validate = new Ajv({ allErrors: true }).compile(readJson(contractPath('contract-schemas', 'inventory.schema.json')))
        validate(inventory) ? ok('inventory is schema-valid (quick ajv check)') : failMsg(`inventory fails its schema: ${(validate.errors || []).slice(0, 3).map((e) => `${e.instancePath || '/'} ${e.message}`).join('; ')}`)
      } catch (e) {
        failMsg(`inventory schema validation failed: ${e.message}`)
      }
    }

  // Area slice count vs stats
  const areasDir = current.areasDir
  const sliceCount = exists(areasDir) ? readdirSync(areasDir).filter((f) => f.endsWith('.json')).length : 0
  const declaredAreas = (inventory.stats || {}).areas
  sliceCount === declaredAreas
    ? ok(`area slices: ${sliceCount} file(s) match stats.areas`)
    : failMsg(`manifests/areas/ has ${sliceCount} slice file(s) but stats.areas says ${declaredAreas}`)

  // Drift report freshness
  const driftPath = contractPath('reports', 'drift.json')
  if (!exists(driftPath)) info('no drift report yet — run `npm run contract:diff`')
  else {
    try {
      const drift = readJson(driftPath)
      if ((drift.specHash || null) === (src.specHash || null)) ok('drift report matches the snapshot specHash')
      else warnMsg('drift report is stale (specHash differs from the snapshot) — re-run `npm run contract:diff`')
    } catch (e) { warnMsg(`.cache/api-contract/reports/drift.json unreadable (${e.message})`) }
  }
}

// Raw snapshot size (context economy — agents read manifests/, not this file)
const specPath = current.spec
if (specPath && exists(specPath)) {
  const mb = statSync(specPath).size / (1024 * 1024)
  mb > 2
    ? warnMsg(`spec/openapi.json is ${mb.toFixed(1)} MB (>2 MB) — agents must read manifests/ slices, never this file`)
    : ok(`spec/openapi.json present (${mb.toFixed(2)} MB)`)
} else info('no normalized OpenAPI artifact yet (committed by the first successful OpenAPI refresh)')

process.exit(summary('contract:doctor'))
