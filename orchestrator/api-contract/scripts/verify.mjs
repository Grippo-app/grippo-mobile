// verify.mjs — validate the committed snapshot: ajv against contract-schemas/ + referential
// integrity (areas<->endpoints both directions, body/response schemaRefs resolve to a slice entry,
// stats recount). NEVER calls the backend; reads only manifests/. No snapshot -> skipped unless
// backendContractEnabled:true requires one.
// Ajv is mandatory whenever a snapshot exists; structural checks cannot replace schema validation.
import { exists, readJson, contractPath, contractCodePath, currentContractFiles, readConfig, info, ok, warnMsg, failMsg, summary } from './_util.mjs'
import { readdirSync } from 'node:fs'
import { join } from 'node:path'

;(async function main() {
  if (process.argv.length !== 2) {
    process.stderr.write('contract:verify accepts no arguments\n')
    process.exit(2)
  }
  const gate = readConfig('backendContractEnabled') // 'true' | 'auto' | 'false' | null/undefined
  const current = currentContractFiles()
  const invPath = current.inventory
  if (current.invalid) {
    failMsg(`current generation is invalid (${current.error})`)
    process.exit(summary('contract:verify'))
  }
  if (!invPath || !exists(invPath)) {
    if (gate === 'true') failMsg('no validated current snapshot but backendContractEnabled: true REQUIRES one — use Backend Test + Refresh or typed `contract:probe` then `contract:refresh-*`')
    else info('no validated current snapshot — skipped')
    process.exit(summary('contract:verify'))
  }

  let inventory
  try { inventory = readJson(invPath) } catch (e) { failMsg(`endpoint-inventory.json unreadable: ${e.message}`); process.exit(summary('contract:verify')) }

  const areasDir = current.areasDir
  const slices = {}
  if (exists(areasDir)) {
    for (const f of readdirSync(areasDir)) {
      if (!f.endsWith('.json')) continue
      try { slices[f.replace(/\.json$/, '')] = readJson(join(areasDir, f)) } catch (e) { failMsg(`manifests/areas/${f} unreadable: ${e.message}`) }
    }
  }

  // ---- mandatory ajv schema validation --------------------------------------
  let AjvCtor = null
  try { ({ default: AjvCtor } = await import('ajv')) } catch (e) {
    failMsg(`ajv unavailable — run root \`npm ci\`; a snapshot cannot be verified without schema validation (${e.message})`)
  }
  if (AjvCtor) {
    const Ajv = AjvCtor
    const ajv = new Ajv({ allErrors: true })
    const fmt = (errs) => (errs || []).slice(0, 6).map((e) => `${e.instancePath || '/'} ${e.message}`).join('; ')
    try {
      const vInv = ajv.compile(readJson(contractCodePath('contract-schemas', 'inventory.schema.json')))
      vInv(inventory) ? ok('endpoint-inventory.json is schema-valid') : failMsg(`endpoint-inventory.json schema: ${fmt(vInv.errors)}`)
      const vArea = ajv.compile(readJson(contractCodePath('contract-schemas', 'area.schema.json')))
      for (const [stem, slice] of Object.entries(slices)) {
        vArea(slice) ? ok(`areas/${stem}.json is schema-valid`) : failMsg(`areas/${stem}.json schema: ${fmt(vArea.errors)}`)
      }
    } catch (e) {
      failMsg(`contract schema file invalid/unreadable: ${e.message}`)
    }
  }

  // ---- referential integrity --------------------------------------------------
  const endpoints = Array.isArray(inventory.endpoints) ? inventory.endpoints : []
  const areasMap = inventory.areas && typeof inventory.areas === 'object' ? inventory.areas : {}
  const byOpId = new Map(endpoints.map((e) => [e.operationId, e]))
  if (byOpId.size !== endpoints.length) failMsg('duplicate operationId(s) in endpoints')

  for (const [area, opIds] of Object.entries(areasMap)) {
    for (const opId of opIds || []) {
      const ep = byOpId.get(opId)
      if (!ep) failMsg(`areas.${area} lists "${opId}" but no such endpoint exists`)
      else if (ep.area !== area) failMsg(`areas.${area} lists "${opId}" but the endpoint says area "${ep.area}"`)
    }
  }
  for (const ep of endpoints) {
    if (!Object.hasOwn(areasMap, ep.area) ||
        !Array.isArray(areasMap[ep.area]) ||
        !areasMap[ep.area].includes(ep.operationId)) {
      failMsg(`endpoint "${ep.operationId}" (area ${ep.area}) is missing from the areas map`)
    }
    for (const group of ['pathParams', 'query']) {
      const names = new Set()
      for (const parameter of ep.request && ep.request[group] || []) {
        if (names.has(parameter.name)) {
          failMsg(`${ep.operationId} has duplicate ${group} parameter "${parameter.name}"`)
        }
        names.add(parameter.name)
      }
    }
    const errorStatuses = new Set()
    for (const status of ep.errors || []) {
      if (errorStatuses.has(status)) failMsg(`${ep.operationId} repeats error status ${status}`)
      if (Object.prototype.hasOwnProperty.call(ep.response || {}, status)) {
        failMsg(`${ep.operationId} records status ${status} as both response and error`)
      }
      errorStatuses.add(status)
    }
  }

  let refsOk = 0
  const resolveRef = (area, ref, what) => {
    const slice = slices[area]
    if (!slice || !slice.schemas ||
        !Object.prototype.hasOwnProperty.call(slice.schemas, ref)) {
      failMsg(`${what} schemaRef "${ref}" does not resolve in manifests/areas/${area}.json`)
    }
    else refsOk++
  }
  // 204/205/304 are bodyless by HTTP semantics — a missing schema there is correct, not a gap.
  const BODYLESS = new Set(['204', '205', '304'])
  for (const ep of endpoints) {
    if (ep.request && ep.request.body) resolveRef(ep.area, ep.request.body.schemaRef, `${ep.operationId} body`)
    for (const [status, r] of Object.entries(ep.response || {})) {
      if (r.schemaRef == null) {
        if (!BODYLESS.has(status)) warnMsg(`${ep.operationId} response ${status} has no schemaRef (unmodeled body)`)
      } else resolveRef(ep.area, r.schemaRef, `${ep.operationId} response ${status}`)
    }
  }
  for (const [stem, slice] of Object.entries(slices)) {
    if (slice.area !== stem) failMsg(`areas/${stem}.json declares area "${slice.area}" (file/area mismatch)`)
    if (!areasMap[stem]) warnMsg(`areas/${stem}.json has no endpoints in the inventory (orphan slice)`)
    for (const [schemaRef, schema] of Object.entries(slice.schemas || {})) {
      const fieldNames = new Set()
      for (const field of schema.fields || []) {
        if (fieldNames.has(field.jsonName)) {
          failMsg(`${stem}.${schemaRef} repeats JSON field "${field.jsonName}"`)
        }
        fieldNames.add(field.jsonName)
        if (typeof field.type === 'string' && field.type.startsWith('ref:')) {
          resolveRef(stem, field.type.slice(4), `${stem}.${schemaRef}.${field.jsonName || field.name} field`)
        }
        if (field.itemsRef) {
          resolveRef(stem, field.itemsRef, `${stem}.${schemaRef}.${field.jsonName || field.name} itemsRef`)
        }
      }
    }
  }
  if (refsOk) ok(`${refsOk} schemaRef(s) resolve to slice entries`)

  // ---- stats recount ------------------------------------------------------------
  const uniqueSchemas = new Set()
  for (const slice of Object.values(slices)) for (const name of Object.keys(slice.schemas || {})) uniqueSchemas.add(name)
  const actual = { endpoints: endpoints.length, areas: Object.keys(areasMap).length, schemas: uniqueSchemas.size }
  const declared = inventory.stats || {}
  for (const k of ['endpoints', 'areas', 'schemas']) {
    if (declared[k] !== actual[k]) failMsg(`stats.${k} says ${declared[k]} but the snapshot has ${actual[k]}`)
  }
  if (['endpoints', 'areas', 'schemas'].every((k) => declared[k] === actual[k])) {
    ok(`stats recount: ${actual.endpoints} endpoints / ${actual.areas} areas / ${actual.schemas} schemas`)
  }

  process.exit(summary('contract:verify'))
})().catch((e) => { failMsg(e?.message || String(e)); process.exit(summary('contract:verify')) })
