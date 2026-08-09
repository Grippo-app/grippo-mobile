// import-postman.mjs — enrich (or bootstrap) the contract snapshot from an exported Postman
// collection (v2.1 JSON). Two modes:
//   • OpenAPI-backed inventory EXISTS -> ENRICH: match requests by method+path; saved example
//     responses/bodies feed nullable_observed / enum_observed / example per field. OpenAPI stays
//     the structural authority — unmatched requests are WARNED and left out, never added.
//     source.kind -> "merged".
//   • inventory ABSENT  -> BOOTSTRAP: synthesize the inventory + slices from the collection alone
//     (source.kind "postman", nullable_declared: null, schemaRefs <OperationId>Response/Body,
//     area = first path segment, types inferred from example JSON).
//   • Postman-only inventory EXISTS -> BOOTSTRAP refresh: replace the structural snapshot from the
//     collection again; unmatched requests are still structure until an OpenAPI pull contributes.
// This transformer runs only inside the typed sidecar's owned staging area.
import {
  exists, readJson, contractPath, atomicWrite, kebab, pascal, lowerCamel, synthOperationId,
  info, ok, warnMsg, failMsg, summary,
} from './_util.mjs'
import { readFileSync, readdirSync, unlinkSync } from 'node:fs'
import { join } from 'node:path'

const ENUM_MAX = 10
const tryParse = (s) => { if (typeof s !== 'string' || !s.trim()) return undefined; try { return JSON.parse(s) } catch { return undefined } }
const isPlainObject = (v) => v !== null && typeof v === 'object' && !Array.isArray(v)

// ---- collection walking ------------------------------------------------------
function* walkItems(items) {
  for (const it of items || []) {
    if (it && Array.isArray(it.item)) yield* walkItems(it.item)
    else if (it && it.request) yield it
  }
}
function pathOf(url) {
  if (!url) return null
  if (typeof url === 'string') return normalizeRawUrl(url)
  if (Array.isArray(url.path)) {
    const segs = url.path.map(String).filter((s) => s && !/^\{\{.*\}\}$/.test(s))
    return '/' + segs.map((s) => (s.startsWith(':') ? `{${s.slice(1)}}` : s)).join('/')
  }
  return url.raw ? normalizeRawUrl(url.raw) : null
}
function normalizeRawUrl(raw) {
  let s = String(raw).replace(/\{\{[^}]+\}\}/g, '').replace(/^https?:\/\/[^/]+/, '')
  const q = s.indexOf('?'); if (q >= 0) s = s.slice(0, q)
  if (!s.startsWith('/')) s = '/' + s
  s = s.replace(/\/{2,}/g, '/')
  s = s.split('/').map((seg) => (seg.startsWith(':') ? `{${seg.slice(1)}}` : seg)).join('/')
  if (s.length > 1 && s.endsWith('/')) s = s.slice(0, -1)
  return s
}
const authTypeOf = (auth) => {
  if (!auth || !auth.type) return null
  return auth.type === 'noauth' ? 'none' : auth.type
}
function requestsOf(col) {
  const out = []
  for (const it of walkItems(col.item)) {
    const method = String(it.request.method || 'GET').toUpperCase()
    const path = pathOf(it.request.url)
    if (!path) continue
    out.push({
      name: it.name || `${method} ${path}`, method, path,
      auth: authTypeOf(it.request.auth) ?? authTypeOf(col.auth),
      rawBody: it.request.body && it.request.body.mode === 'raw' ? it.request.body.raw : null,
      query: (it.request.url && Array.isArray(it.request.url.query)) ? it.request.url.query : [],
      responses: (Array.isArray(it.response) ? it.response : []).map((r) => ({ code: r.code, body: r.body })),
    })
  }
  return out
}
// Param-name-insensitive structural match: /notes/{id} matches /notes/{noteId}.
function sameShape(a, b) {
  const sa = a.split('/').filter(Boolean), sb = b.split('/').filter(Boolean)
  if (sa.length !== sb.length) return false
  return sa.every((seg, i) => seg === sb[i] || (seg.startsWith('{') && sb[i].startsWith('{')))
}

// ---- field observation (shared by both modes) --------------------------------
// key "<SchemaRef> <jsonName>" -> { sawNull, sawNonNull, values:Set, example }
function observe(obs, name, jsonName, v) {
  const k = `${name} ${jsonName}`
  const o = (obs[k] = obs[k] || { sawNull: false, sawNonNull: false, values: new Set(), example: undefined })
  if (v === null) { o.sawNull = true; return o }
  o.sawNonNull = true
  if (typeof v === 'string') o.values.add(v)
  if (o.example === undefined && ['string', 'number', 'boolean'].includes(typeof v)) o.example = v
  return o
}
// Free-text/UUID/date strings are not enums: collect observed values only for fields
// the spec already declares as enums, or when every value is an enum-shaped constant.
const ENUM_TOKEN = /^[A-Z][A-Z0-9_]*$/
function applyObservation(field, o) {
  if (o.sawNull) field.nullable_observed = true
  else if (o.sawNonNull && field.nullable_observed !== true) field.nullable_observed = false
  const enumish = !!field.enum || [...o.values].every((v) => ENUM_TOKEN.test(v))
  if (field.type === 'string' && o.values.size && enumish) {
    const union = new Set([...(field.enum_observed || []), ...o.values])
    if (union.size <= ENUM_MAX) {
      field.enum_observed = [...union].sort()
      delete field.enum_observed_truncated   // back under the cap on a re-import — clear any stale marker
    } else {
      // Past ENUM_MAX we cannot carry the full observed set (a high-cardinality / non-enum string field —
      // ids, free text). Drop the list, but RECORD that we truncated rather than letting enum_observed: null
      // read downstream as "never observed" — which would silently exempt the field from enum-new-value drift.
      field.enum_observed = null
      field.enum_observed_truncated = true
    }
  }
  if ((field.example === null || field.example === undefined) && o.example !== undefined) field.example = o.example
}

// =============================== ENRICH MODE ==================================
function enrich(col, fileArg) {
  const invPath = contractPath('manifests', 'endpoint-inventory.json')
  let inventory
  try { inventory = readJson(invPath) } catch (e) { failMsg(`endpoint-inventory.json unreadable: ${e.message}`); process.exit(summary('contract:postman')) }
  const areasDir = contractPath('manifests', 'areas')
  const slices = Object.create(null) // area -> slice json
  if (exists(areasDir)) {
    for (const f of readdirSync(areasDir)) {
      if (!f.endsWith('.json')) continue
      try { slices[f.replace(/\.json$/, '')] = readJson(join(areasDir, f)) } catch (e) { failMsg(`manifests/areas/${f} unreadable: ${e.message}`); process.exit(summary('contract:postman')) }
    }
  }
  // A schema can sit in several area slices (shared DTO) — index every copy and enrich them all.
  const copies = Object.create(null) // name -> [fields arrays]
  for (const slice of Object.values(slices)) {
    for (const [name, sch] of Object.entries(slice.schemas || {})) (copies[name] = copies[name] || []).push(sch.fields || [])
  }
  const fieldsOf = (name) => (copies[name] && copies[name][0]) || null

  const obs = {}
  const walkFields = (name, objIn, depth = 0) => {
    const fields = fieldsOf(name)
    if (!fields || depth > 5 || !isPlainObject(objIn)) return
    for (const f of fields) {
      if (!Object.prototype.hasOwnProperty.call(objIn, f.jsonName)) continue // absent is NOT an observed null
      const v = objIn[f.jsonName]
      observe(obs, name, f.jsonName, v)
      if (v === null) continue
      if (f.type.startsWith('ref:') && isPlainObject(v)) walkFields(f.type.slice(4), v, depth + 1)
      if (f.type === 'array' && f.itemsRef && Array.isArray(v)) for (const el of v) if (isPlainObject(el)) walkFields(f.itemsRef, el, depth + 1)
    }
  }

  let matched = 0
  const unmatched = []
  for (const r of requestsOf(col)) {
    const ep = inventory.endpoints.find((e) => e.method === r.method && sameShape(e.path, r.path))
    if (!ep) { unmatched.push(`${r.method} ${r.path}`); continue }
    matched++
    const reqJson = tryParse(r.rawBody)
    if (reqJson !== undefined && ep.request.body) {
      ep.examples.request = true
      const items = Array.isArray(reqJson) ? reqJson : [reqJson]
      for (const it of items) walkFields(ep.request.body.schemaRef, it)
    }
    for (const resp of r.responses) {
      const code = String(resp.code ?? '')
      if (!/^[0-9]{3}$/.test(code)) continue
      let entry = ep.response[code]
      if (!entry && code.startsWith('2')) entry = Object.entries(ep.response).find(([s]) => s.startsWith('2'))?.[1]
      if (!entry) continue
      const body = tryParse(resp.body)
      if (body === undefined) continue
      ep.examples.response = true
      if (!entry.schemaRef) continue
      const items = entry.array && Array.isArray(body) ? body : [body]
      for (const it of items) walkFields(entry.schemaRef, it)
    }
  }

  let enrichedFields = 0
  for (const [k, o] of Object.entries(obs)) {
    const sp = k.indexOf(' ')
    const name = k.slice(0, sp), jsonName = k.slice(sp + 1)
    for (const fields of copies[name] || []) {
      const f = fields.find((x) => x.jsonName === jsonName)
      if (f) { applyObservation(f, o); enrichedFields++ }
    }
  }

  inventory.source.kind = 'merged'
  inventory.source.postmanImportedAt = new Date().toISOString()

  atomicWrite(invPath, inventory)
  for (const [area, slice] of Object.entries(slices)) atomicWrite(contractPath('manifests', 'areas', `${area}.json`), slice)
  for (const u of unmatched) warnMsg(`not in the OpenAPI inventory: ${u} — left out (OpenAPI stays the structural authority)`)
  ok(`enriched ${Object.keys(obs).length} field(s) (${enrichedFields} slice entries) from ${matched} matched request(s) — source.kind "merged"`)
}

// ============================== BOOTSTRAP MODE ================================
function bootstrap(col, fileArg, opts = {}) {
  if (opts.refreshExisting) info('postman-only inventory exists — refreshing the bootstrap snapshot from the collection (source.kind "postman")')
  else info('no inventory yet — bootstrapping the snapshot from the collection alone (source.kind "postman")')
  const inferType = (v) => v === null ? 'string'
    : Array.isArray(v) ? 'array'
    : typeof v === 'object' ? 'object'
    : typeof v === 'number' ? (Number.isInteger(v) ? 'integer' : 'number')
    : typeof v // string | boolean

  const schemaFields = Object.create(null) // name -> fields[]
  const obs = {}
  const mergeInferred = (name, objIn) => {
    if (!isPlainObject(objIn)) return
    const fields = (schemaFields[name] = schemaFields[name] || [])
    for (const [jsonName, v] of Object.entries(objIn)) {
      let f = fields.find((x) => x.jsonName === jsonName)
      if (!f) {
        f = {
          name: lowerCamel(jsonName), jsonName, type: inferType(v), itemsRef: null, format: null,
          required: false, nullable_declared: null, nullable_observed: null,
          enum: null, enum_observed: null, example: null,
        }
        fields.push(f)
      }
      if (f.type === 'string' && v !== null && inferType(v) !== 'string') f.type = inferType(v) // first sight was null
      observe(obs, name, jsonName, v)
    }
  }

  const endpoints = []
  const seen = new Set()
  for (const r of requestsOf(col)) {
    const key = `${r.method} ${r.path}`
    if (seen.has(key)) continue
    seen.add(key)
    const operationId = synthOperationId(r.method, r.path)
    const area = kebab(String(r.path).split('/').filter(Boolean)[0]?.replace(/[{}]/g, '') || 'root') || 'root'
    const pathParams = r.path.split('/').filter((s) => s.startsWith('{')).map((s) => ({ name: s.slice(1, -1), type: 'string', required: true }))
    const query = r.query.filter((q) => q && q.key).map((q) => ({ name: String(q.key), type: 'string', required: false }))
    let body = null
    const reqJson = tryParse(r.rawBody)
    if (isPlainObject(reqJson)) {
      body = { schemaRef: `${pascal(operationId)}Body`, contentType: 'application/json' }
      mergeInferred(body.schemaRef, reqJson)
    }
    const response = {}, errors = new Set()
    let sawResponseExample = false
    for (const resp of r.responses) {
      const code = String(resp.code ?? '')
      if (!/^[0-9]{3}$/.test(code)) continue
      if (/^[45]/.test(code)) { errors.add(code); continue }
      const parsed = tryParse(resp.body)
      if (parsed === undefined) { response[code] = response[code] || { schemaRef: null, array: false }; continue }
      sawResponseExample = true
      const arr = Array.isArray(parsed)
      const items = (arr ? parsed : [parsed]).filter(isPlainObject)
      if (items.length) {
        const ref = `${pascal(operationId)}Response`
        response[code] = { schemaRef: ref, array: arr }
        for (const it of items) mergeInferred(ref, it)
      } else response[code] = { schemaRef: null, array: false }
    }
    endpoints.push({
      operationId, method: r.method, path: r.path, area,
      summary: r.name ?? null, auth: r.auth ?? null, deprecated: false,
      request: { pathParams, query, body },
      response, errors: [...errors].sort(),
      examples: { request: isPlainObject(reqJson), response: sawResponseExample },
    })
  }
  if (!endpoints.length) { failMsg('the collection contains no requests — nothing to bootstrap'); return }

  for (const [k, o] of Object.entries(obs)) {
    const sp = k.indexOf(' ')
    const f = (schemaFields[k.slice(0, sp)] || []).find((x) => x.jsonName === k.slice(sp + 1))
    if (f) applyObservation(f, o)
  }

  const areasMap = Object.create(null)
  const areaSchemas = Object.create(null)
  for (const ep of endpoints) {
    ;(areasMap[ep.area] = areasMap[ep.area] || []).push(ep.operationId)
    const refs = (areaSchemas[ep.area] = areaSchemas[ep.area] || new Set())
    if (ep.request.body) refs.add(ep.request.body.schemaRef)
    for (const rr of Object.values(ep.response)) if (rr.schemaRef) refs.add(rr.schemaRef)
  }
  const areaNames = Object.keys(areasMap).sort()
  const sliceNames = new Set()
  for (const area of areaNames) for (const name of areaSchemas[area]) sliceNames.add(name)
  const inventory = {
    schemaVersion: 1,
    source: {
      kind: 'postman', openApiUrl: null, openApiVersion: null,
      title: (col.info && col.info.name) || null, specHash: null, fetchedAt: null,
      postmanImportedAt: new Date().toISOString(),
    },
    stats: { endpoints: endpoints.length, areas: areaNames.length, schemas: sliceNames.size },
    areas: Object.fromEntries(areaNames.map((a) => [a, areasMap[a]])),
    endpoints,
  }
  atomicWrite(contractPath('manifests', 'endpoint-inventory.json'), inventory)
  const areasDir = contractPath('manifests', 'areas')
  if (opts.refreshExisting && exists(areasDir)) {
    const keep = new Set(areaNames.map((a) => `${a}.json`))
    for (const f of readdirSync(areasDir)) {
      if (!f.endsWith('.json') || keep.has(f)) continue
      try { unlinkSync(join(areasDir, f)); info(`area slice vanished from the Postman bootstrap — removed manifests/areas/${f}`) }
      catch (e) { warnMsg(`could not remove stale slice manifests/areas/${f}: ${e.message}`) }
    }
  }
  for (const area of areaNames) {
    const schemasOut = Object.create(null)
    for (const name of [...areaSchemas[area]].sort()) { schemasOut[name] = { fields: schemaFields[name] || [] } }
    atomicWrite(contractPath('manifests', 'areas', `${area}.json`), { schemaVersion: 1, area, schemas: schemasOut })
  }
  ok(`bootstrapped: ${endpoints.length} endpoints / ${areaNames.length} areas / ${sliceNames.size} schemas (structure is observed, not declared — pull an OpenAPI spec when one exists)`)
}

;(async function main() {
  const argv = process.argv.slice(2)
  if (argv.length && (argv.length !== 2 || argv[0] !== '--file' || !argv[1] || argv[1].startsWith('--'))) {
    process.stderr.write('contract:postman accepts only --file <path-or-url>\n')
    process.exit(2)
  }
  const fileOverride = argv.length === 2 ? argv[1] : null
  if (!process.env.ORCHESTRATOR_API_CONTRACT_DATA_DIR || !fileOverride) {
    process.stderr.write('Postman transformer is internal to the typed Backend staging flow\n')
    process.exit(2)
  }
  const fileArg = fileOverride

  let text
  try {
    text = readFileSync(fileArg, 'utf8')
  } catch (e) { failMsg(`cannot read staged collection: ${e.message}`); process.exit(1) }

  let col
  try { col = JSON.parse(text) } catch (e) { failMsg(`collection is not valid JSON: ${e.message}`); process.exit(1) }
  const schemaUrl = String((col.info && col.info.schema) || '')
  if (!schemaUrl.includes('v2.1')) { failMsg(`not a Postman collection v2.1 (info.schema: ${schemaUrl || 'missing'}) — export as Collection v2.1`); process.exit(1) }

  const invPath = contractPath('manifests', 'endpoint-inventory.json')
  if (exists(invPath)) {
    let inventory
    try { inventory = readJson(invPath) } catch (e) { failMsg(`endpoint-inventory.json unreadable: ${e.message}`); process.exit(summary('contract:postman')) }
    const source = inventory.source || {}
    if (source.kind === 'postman' && !source.specHash && !source.openApiVersion) bootstrap(col, fileArg, { refreshExisting: true })
    else enrich(col, fileArg)
  } else bootstrap(col, fileArg)
  process.exit(summary('contract:postman'))
})().catch((e) => { failMsg(e?.message || String(e)); process.exit(summary('contract:postman')) })
