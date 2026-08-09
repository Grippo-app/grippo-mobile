// normalize.mjs — a sidecar-owned staging OpenAPI document -> a candidate normalized spec,
// inventory, and per-area field slices. Canonical publication copies those candidates into a
// validated immutable generation. Direct workspace publication is forbidden.
//
// OpenAPI 3.x ONLY (a "swagger": "2.0" doc fails with "convert to OpenAPI 3 first"). All OpenAPI
// variability (3.0 nullable vs 3.1 type arrays, allOf composition, inline vs $ref schemas) is
// absorbed HERE; nothing downstream depends on OpenAPI's structure. MERGE STICKINESS: existing
// area slices are loaded first and nullable_observed/enum_observed/example are carried forward
// per (schemaRef, jsonName), so a re-pull never loses Postman enrichment. NEVER calls the backend.
import {
  exists, readJson, contractPath, atomicWrite, sha256, kebab, pascal, lowerCamel,
  synthOperationId, info, ok, warnMsg, failMsg, summary, redactUrl,
} from './_util.mjs'
import { readFileSync, readdirSync, unlinkSync } from 'node:fs'
import { join } from 'node:path'
import { pathToFileURL } from 'node:url'

const METHODS = ['get', 'post', 'put', 'patch', 'delete']
const hasOwn = (value, key) => !!value && Object.prototype.hasOwnProperty.call(value, key)
const refName = (ref) => { const m = /^#\/components\/schemas\/([A-Za-z0-9_.-]+)$/.exec(String(ref || '')); return m ? m[1] : null }
const componentName = (section, ref) => {
  const m = new RegExp(`^#/components/${section}/([A-Za-z0-9_.-]+)$`).exec(String(ref || ''))
  return m ? m[1] : null
}

// Absorb 3.0 `nullable: true` and 3.1 `type: [.., "null"]` into { type, nullable }.
function baseType(schema) {
  let t = schema && schema.type, nullable = !!(schema && schema.nullable === true)
  if (Array.isArray(t)) { nullable = nullable || t.includes('null'); t = t.filter((x) => x !== 'null')[0] ?? null }
  return { type: t ?? null, nullable }
}

const resolveLike = (schemas, node, depth = 0) => {
  while (node && node.$ref && depth++ < 6) {
    const n = refName(node.$ref)
    node = n && hasOwn(schemas, n) ? schemas[n] : null
  }
  return node || {}
}
const resolveComponent = (doc, section, node, depth = 0) => {
  const bucket = doc.components && doc.components[section] ? doc.components[section] : {}
  while (node && node.$ref && depth++ < 6) {
    const n = componentName(section, node.$ref)
    node = n && hasOwn(bucket, n) ? bucket[n] : null
  }
  return node || {}
}
function isObjectLike(schemas, node) {
  const s = resolveLike(schemas, node)
  if (s.properties || s.allOf) return true
  const { type } = baseType(s)
  return type === 'object' || (type == null && !s.enum)
}

// Object "shape" (properties + required) following $ref and merging allOf. Depth-guarded.
function objectShape(schemas, node, depth = 0, seen = new Set()) {
  if (!node || typeof node !== 'object' || depth > 6) {
    return { properties: Object.create(null), required: new Set() }
  }
  if (node.$ref) {
    const name = refName(node.$ref)
    if (!name || !hasOwn(schemas, name) || seen.has(name)) {
      return { properties: Object.create(null), required: new Set() }
    }
    return objectShape(schemas, schemas[name], depth + 1, new Set([...seen, name]))
  }
  const properties = Object.create(null), required = new Set()
  for (const part of node.allOf || []) {
    const sub = objectShape(schemas, part, depth + 1, seen)
    Object.assign(properties, sub.properties)
    for (const r of sub.required) required.add(r)
  }
  Object.assign(properties, node.properties || {})
  for (const r of node.required || []) required.add(String(r))
  return { properties, required }
}

// One property -> one canonical field. `ctx.unions` collects oneOf/anyOf collapse warnings;
// `ctx.referenced` collects object-like $ref targets for the transitive area closure.
function fieldOf(ctx, schemas, schemaName, jsonName, propIn, requiredSet) {
  let p = propIn && typeof propIn === 'object' ? propIn : {}
  let nullable = false
  for (const key of ['anyOf', 'oneOf']) {
    if (!Array.isArray(p[key])) continue
    const rest = p[key].filter((b) => !(b && b.type === 'null'))
    if (rest.length === 1 && rest.length !== p[key].length) { nullable = true; p = { ...rest[0] } } // 3.1 nullable sugar
    else { ctx.unions.push(`${schemaName}.${jsonName} (${key})`); p = { type: 'object' } }          // true union -> "object"
    break
  }
  if (Array.isArray(p.allOf) && p.allOf.length === 1) {
    const wrapper = { ...p }
    delete wrapper.allOf
    p = { ...p.allOf[0], ...wrapper }
  } // "$ref + description/constraints" wrapper
  const own = baseType(p)
  nullable = nullable || own.nullable
  let type = 'object', itemsRef = null, format = p.format ?? null
  let constraintSource = p
  let en = Array.isArray(p.enum) ? [...p.enum] : null
  if (p.$ref) {
    const name = refName(p.$ref)
    const target = name && hasOwn(schemas, name) ? schemas[name] : null
    if (name && target && isObjectLike(schemas, target)) { type = `ref:${name}`; ctx.referenced.add(name) }
    else if (target) { // simple component (primitive/enum) — inline it at the use site
      const tb = baseType(target)
      type = tb.type || 'string'; nullable = nullable || tb.nullable
      en = Array.isArray(target.enum) ? [...target.enum] : en
      format = target.format ?? format
      constraintSource = { ...target, ...p }
    }
  } else if (own.type === 'array') {
    type = 'array'
    const items = p.items || {}
    const iname = items.$ref ? refName(items.$ref) : null
    if (iname && hasOwn(schemas, iname) && isObjectLike(schemas, schemas[iname])) {
      itemsRef = iname
      ctx.referenced.add(iname)
    }
  } else if (['string', 'integer', 'number', 'boolean', 'object'].includes(own.type)) type = own.type
  else if (own.type == null && en) type = typeof en[0] === 'number' ? 'number' : 'string'
  // anything else (no type, unresolvable $ref, exotic types) stays "object"
  const number = (value) => typeof value === 'number' && Number.isFinite(value) ? value : null
  const integer = (value) => Number.isSafeInteger(value) && value >= 0 ? value : null
  const exclusiveMinimum = typeof constraintSource.exclusiveMinimum === 'number'
    ? number(constraintSource.exclusiveMinimum)
    : constraintSource.exclusiveMinimum === true ? number(constraintSource.minimum) : null
  const exclusiveMaximum = typeof constraintSource.exclusiveMaximum === 'number'
    ? number(constraintSource.exclusiveMaximum)
    : constraintSource.exclusiveMaximum === true ? number(constraintSource.maximum) : null
  const rawPattern = typeof constraintSource.pattern === 'string'
    ? constraintSource.pattern.normalize('NFC') : null
  const constraints = {
    minimum: number(constraintSource.minimum),
    maximum: number(constraintSource.maximum),
    exclusiveMinimum,
    exclusiveMaximum,
    minLength: integer(constraintSource.minLength),
    maxLength: integer(constraintSource.maxLength),
    minItems: integer(constraintSource.minItems),
    maxItems: integer(constraintSource.maxItems),
    minProperties: integer(constraintSource.minProperties),
    maxProperties: integer(constraintSource.maxProperties),
    pattern: rawPattern === null ? null : rawPattern.slice(0, 2000),
    patternHash: rawPattern === null ? null : sha256(rawPattern),
  }
  return {
    name: lowerCamel(jsonName), jsonName, type, itemsRef, format,
    required: requiredSet.has(jsonName),
    nullable_declared: nullable, nullable_observed: null,
    enum: en, enum_observed: null, example: p.example ?? null, constraints,
  }
}

function authOf(doc, op) {
  const sec = op.security !== undefined ? op.security : doc.security
  if (sec === undefined || !Array.isArray(sec) || sec.length === 0) return 'none'
  const schemeName = Object.keys(sec[0] || {})[0]
  if (!schemeName) return 'none'
  const schemes = doc.components && doc.components.securitySchemes
  const scheme = hasOwn(schemes, schemeName) ? schemes[schemeName] : null
  if (!scheme) return schemeName
  if (scheme.type === 'http') return scheme.scheme === 'bearer' ? 'bearer' : (scheme.scheme || 'http')
  return scheme.type || schemeName
}

const CARRY_KEY_SEP = '\u0000'

// Existing slices -> (schemaRef, jsonName) -> carried enrichment (the merge-stickiness source).
function loadCarry() {
  const dir = contractPath('manifests', 'areas')
  const carry = Object.create(null)
  if (!exists(dir)) return carry
  for (const f of readdirSync(dir)) {
    if (!f.endsWith('.json')) continue
    const slice = readJson(join(dir, f))
    if (!slice || typeof slice !== 'object' || Array.isArray(slice) || !slice.schemas || typeof slice.schemas !== 'object' || Array.isArray(slice.schemas)) {
      throw new Error(`existing area slice ${f} is malformed`)
    }
    for (const [name, sch] of Object.entries(slice.schemas || {})) {
      for (const fld of sch.fields || []) {
        const k = `${name}${CARRY_KEY_SEP}${fld.jsonName}`
        const prev = carry[k] || {}
        carry[k] = {
          nullable_observed: prev.nullable_observed === true ? true : (fld.nullable_observed ?? prev.nullable_observed ?? null),
          enum_observed: fld.enum_observed ?? prev.enum_observed ?? null,
          enum_observed_truncated: prev.enum_observed_truncated === true ? true : (fld.enum_observed_truncated === true ? true : null),
          example: prev.example ?? fld.example ?? null,
        }
      }
    }
  }
  return carry
}

// The pull/normalize core. Returns true on success; logs via the shared PASS/WARN/FAIL helpers
// (the CALLER prints the summary and exits). opts: { rawText, sourceUrl, fetchedAt }.
function runNormalize({ rawText, sourceUrl = null, fetchedAt = null }) {
  let doc
  try { doc = JSON.parse(rawText) } catch (e) { failMsg(`spec is not valid JSON: ${e.message}`); return false }
  if (doc.swagger === '2.0') { failMsg('this is a Swagger 2.0 document — convert to OpenAPI 3 first (e.g. swagger2openapi)'); return false }
  if (!doc.openapi || !String(doc.openapi).startsWith('3')) { failMsg(`not an OpenAPI 3.x document (openapi: ${doc.openapi ?? 'missing'})`); return false }

  const schemas = (doc.components && doc.components.schemas) || {}
  const ctx = { unions: [], referenced: new Set() }

  // Fields for every object-like component (simple enum/primitive components are inlined at use sites).
  const componentFields = Object.create(null)
  for (const [name, node] of Object.entries(schemas)) {
    if (!isObjectLike(schemas, node)) continue
    const shape = objectShape(schemas, node)
    componentFields[name] = Object.entries(shape.properties).map(([jsonName, prop]) => fieldOf(ctx, schemas, name, jsonName, prop, shape.required))
  }
  // Inline (non-$ref) body/response object schemas get synthesized names — registered here too.
  const registerSynthetic = (name, node) => {
    if (componentFields[name]) return name
    const shape = objectShape(schemas, node)
    componentFields[name] = Object.entries(shape.properties).map(([jsonName, prop]) => fieldOf(ctx, schemas, name, jsonName, prop, shape.required))
    return name
  }
  const schemaRefOf = (node, synthName) => { // -> { schemaRef, array } or null when unmodeled
    if (!node || typeof node !== 'object') return null
    const { type } = baseType(node)
    if (node.$ref) {
      const name = refName(node.$ref)
      if (name && componentFields[name]) return { schemaRef: name, array: false }
      return null // $ref to a simple/unknown component — unmodeled as a body
    }
    if (type === 'array') {
      const items = node.items || {}
      if (items.$ref) {
        const name = refName(items.$ref)
        return name && componentFields[name] ? { schemaRef: name, array: true } : null
      }
      const ishape = objectShape(schemas, items)
      if (Object.keys(ishape.properties).length) return { schemaRef: registerSynthetic(synthName, items), array: true }
      return null
    }
    const shape = objectShape(schemas, node)
    if (Object.keys(shape.properties).length) return { schemaRef: registerSynthetic(synthName, node), array: false }
    return null
  }

  // Endpoints.
  const endpoints = []
  for (const [path, pathItem] of Object.entries(doc.paths || {})) {
    if (!pathItem || typeof pathItem !== 'object') continue
    const pathLevelParams = Array.isArray(pathItem.parameters) ? pathItem.parameters : []
    for (const method of METHODS) {
      const op = pathItem[method]
      if (!op || typeof op !== 'object') continue
      const operationId = op.operationId || synthOperationId(method, path)
      const firstTag = Array.isArray(op.tags) && op.tags.length ? op.tags[0] : null
      const firstSeg = String(path).split('/').filter(Boolean)[0] || 'root'
      const area = kebab(firstTag || firstSeg.replace(/[{}]/g, '')) || 'root'
      const pathParams = [], query = []
      for (const rawParam of [...pathLevelParams, ...(Array.isArray(op.parameters) ? op.parameters : [])]) {
        const p = resolveComponent(doc, 'parameters', rawParam)
        if (!p || typeof p !== 'object') continue
        const ptype = baseType(p.schema || {}).type || 'string'
        if (p.in === 'path') pathParams.push({ name: String(p.name), type: ptype, required: p.required !== false })
        else if (p.in === 'query') query.push({ name: String(p.name), type: ptype, required: p.required === true })
      }
      let body = null
      if (op.requestBody) {
        const content = (resolveComponent(doc, 'requestBodies', op.requestBody).content) || {}
        const contentType = content['application/json'] ? 'application/json' : Object.keys(content)[0]
        const node = contentType ? content[contentType].schema : null
        const r = node ? schemaRefOf(node, `${pascal(operationId)}Body`) : null
        if (r) body = { schemaRef: r.schemaRef, contentType }
        else warnMsg(`${method.toUpperCase()} ${path}: request body has no modelable JSON object schema — body recorded as null`)
      }
      const response = {}, errors = []
      for (const [status, resp] of Object.entries(op.responses || {})) {
        if (/^[45][0-9]{2}$/.test(status)) { errors.push(status); continue }
        const content = (resolveComponent(doc, 'responses', resp).content) || {}
        const contentType = content['application/json'] ? 'application/json' : Object.keys(content)[0]
        const node = contentType ? content[contentType].schema : null
        const r = node ? schemaRefOf(node, `${pascal(operationId)}Response`) : null
        response[status] = r ? { schemaRef: r.schemaRef, array: r.array } : { schemaRef: null, array: false }
      }
      endpoints.push({
        operationId, method: method.toUpperCase(), path, area,
        summary: op.summary ?? null, auth: authOf(doc, op), deprecated: op.deprecated === true,
        request: { pathParams, query, body },
        response, errors: errors.sort(),
        examples: { request: false, response: false }, // recomputed below from carried-forward observations
      })
    }
  }
  if (!endpoints.length) warnMsg('the spec declares no operations — inventory will be empty')
  if (ctx.unions.length) warnMsg(`oneOf/anyOf collapsed to type "object" (union types are not modeled): ${ctx.unions.join(', ')}`)

  // Areas map + per-area transitive schema closure.
  const areasMap = Object.create(null)
  const areaRefs = Object.create(null)
  for (const ep of endpoints) {
    ;(areasMap[ep.area] = areasMap[ep.area] || []).push(ep.operationId)
    const refs = (areaRefs[ep.area] = areaRefs[ep.area] || new Set())
    if (ep.request.body) refs.add(ep.request.body.schemaRef)
    for (const r of Object.values(ep.response)) if (r.schemaRef) refs.add(r.schemaRef)
  }
  const closure = (direct) => {
    const out = new Set(), stack = [...direct]
    while (stack.length) {
      const n = stack.pop()
      if (out.has(n) || !componentFields[n]) continue
      out.add(n)
      for (const f of componentFields[n]) {
        if (f.type.startsWith('ref:')) stack.push(f.type.slice(4))
        if (f.itemsRef) stack.push(f.itemsRef)
      }
    }
    return out
  }

  // Merge stickiness: carry forward Postman/observed enrichment before overwriting.
  const carry = loadCarry()
  let carried = 0
  const finalFields = Object.create(null)
  for (const [name, fields] of Object.entries(componentFields)) {
    finalFields[name] = fields.map((f) => {
      const c = carry[`${name}${CARRY_KEY_SEP}${f.jsonName}`]
      if (!c) return { ...f }
      if (c.nullable_observed !== null || c.enum_observed !== null || c.enum_observed_truncated === true || c.example !== null) carried++
      const merged = { ...f, nullable_observed: c.nullable_observed, enum_observed: c.enum_observed, example: c.example ?? f.example }
      if (c.enum_observed_truncated === true) merged.enum_observed_truncated = true
      return merged
    })
  }
  const hasObserved = (name) => !!finalFields[name] && finalFields[name].some((f) => f.nullable_observed !== null || f.enum_observed !== null || f.enum_observed_truncated === true || f.example !== null)
  for (const ep of endpoints) {
    ep.examples.request = !!(ep.request.body && hasObserved(ep.request.body.schemaRef))
    ep.examples.response = Object.values(ep.response).some((r) => r.schemaRef && hasObserved(r.schemaRef))
  }

  // Provenance: a re-pull over a Postman-enriched snapshot stays "merged".
  const invPath = contractPath('manifests', 'endpoint-inventory.json')
  let prev = null
  if (exists(invPath)) prev = readJson(invPath)
  const postmanImportedAt = (prev && prev.source && prev.source.postmanImportedAt) || null

  const sliceNames = new Set()
  const areaNames = Object.keys(areasMap).sort()
  const slices = Object.create(null)
  for (const area of areaNames) {
    const schemasOut = Object.create(null)
    for (const name of [...closure(areaRefs[area])].sort()) { schemasOut[name] = { fields: finalFields[name] }; sliceNames.add(name) }
    slices[area] = { schemaVersion: 1, area, schemas: schemasOut }
  }
  const inventory = {
    schemaVersion: 1,
    source: {
      kind: postmanImportedAt ? 'merged' : 'openapi',
      openApiUrl: sourceUrl ? redactUrl(sourceUrl) : null,
      openApiVersion: String(doc.openapi),
      title: (doc.info && doc.info.title) || null,
      specHash: sha256(rawText),
      fetchedAt: fetchedAt || new Date().toISOString(),
      postmanImportedAt,
    },
    stats: { endpoints: endpoints.length, areas: areaNames.length, schemas: sliceNames.size },
    areas: Object.fromEntries(areaNames.map((a) => [a, areasMap[a]])),
    endpoints,
  }

  // Writes — all atomic. spec/openapi.json is the pretty-printed committed snapshot of record.
  atomicWrite(contractPath('spec', 'openapi.json'), JSON.stringify(doc, null, 2) + '\n')
  atomicWrite(invPath, inventory)
  for (const area of areaNames) atomicWrite(contractPath('manifests', 'areas', `${area}.json`), slices[area])
  // Delete slices whose area vanished from the spec (log it — never silently orphan a file).
  const areasDir = contractPath('manifests', 'areas')
  if (exists(areasDir)) {
    if (!areaNames.length) warnMsg('spec yielded zero operations — all existing area slices will be deleted; verify the source URL or the spec file')
    for (const f of readdirSync(areasDir)) {
      if (!f.endsWith('.json')) continue
      const stem = f.replace(/\.json$/, '')
      if (!areasMap[stem]) {
        try { unlinkSync(join(areasDir, f)); info(`area "${stem}" vanished from the spec — removed manifests/areas/${f}`) }
        catch (e) { warnMsg(`could not remove stale slice manifests/areas/${f}: ${e.message}`) }
      }
    }
  }

  ok(`spec snapshot of record -> spec/openapi.json (${inventory.source.specHash.slice(0, 19)}…)`)
  ok(`inventory: ${endpoints.length} endpoints / ${areaNames.length} areas / ${sliceNames.size} schemas -> manifests/endpoint-inventory.json`)
  ok(`area slices -> manifests/areas/{${areaNames.join(', ')}}.json`)
  if (carried) info(`merge stickiness: carried forward observed enrichment on ${carried} field(s)`)
  return true
}

;(function main() {
  if (!process.argv[1] || import.meta.url !== pathToFileURL(process.argv[1]).href) return
  const argv = process.argv.slice(2)
  if (argv.length && (argv.length !== 2 || argv[0] !== '--input' || !argv[1] || argv[1].startsWith('--'))) {
    process.stderr.write('contract:normalize accepts only --input <path>\n')
    process.exit(2)
  }
  if (!process.env.ORCHESTRATOR_API_CONTRACT_DATA_DIR || argv.length !== 2) {
    process.stderr.write('contract normalizer is internal to the typed Backend staging flow\n')
    process.exit(2)
  }
  const input = argv[1]
  if (!exists(input)) { failMsg('staged OpenAPI input is missing'); process.exit(1) }
  let rawText
  try { rawText = readFileSync(input, 'utf8') } catch (e) { failMsg(`cannot read ${input}: ${e.message}`); process.exit(1) }
  runNormalize({ rawText, sourceUrl: null, fetchedAt: null })
  process.exit(summary('contract:normalize'))
})()
