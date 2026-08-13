// diff.mjs — the mechanical drift core behind the `backend-contract-drift` validator
// (authoritative reference: the backend-contract-client skill, references/drift.md).
//
// Compares the committed snapshot against the hand-written mobile data layer:
//   • DTO files (**/data-services/backend/**/dto/**/*.kt): @SerialName sets vs the area slice —
//     dto-field-unknown (ERROR), server-field-missing-in-dto (WARNING + exact all-nullable
//     Kotlin suggestion), type-mismatch (ERROR — typeMatches is family-loose, a surviving
//     mismatch breaks deserialization on the wire).
//   • <Product>Api request(method=…, path=…) calls vs the inventory — endpoint-missing-server-side (ERROR).
//   • Slice-internal reality checks (no DTOs needed): nullability-mismatch (INFO — observed
//     payload evidence contradicts the declared contract,
//     the defensive DTO is justified), enum-new-value (WARNING/INFO).
// SUGGESTION-ONLY: writes the current control/task report scope's drift.json + a stdout summary,
// NEVER edits code, exits 0 in all
// report-producing paths. No snapshot -> skipped (exit 0, no report). NEVER calls the backend.
import {
  exists, readJson, contractPath, currentContractFiles, PROJECT_ROOT, EXECUTION_ROOT, EXECUTION_SCOPE, readConfig,
  info, ok, warnMsg, failMsg, summary,
} from './_util.mjs'
import { readdirSync } from 'node:fs'
import { dirname, join, relative, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { createRequire } from 'node:module'

const HERE = dirname(fileURLToPath(import.meta.url))
const require = createRequire(import.meta.url)
const generation = require(resolve(HERE, '..', '..', 'site', 'server', 'contract-generation.js'))
const projectInputs = require(resolve(HERE, '..', '..', 'site', 'server', 'api-project-inputs.js'))
const fileGuards = require(resolve(HERE, '..', '..', 'site', 'server', 'file-guards.js'))

const C = { reset: '\x1b[0m', red: '\x1b[31m', yellow: '\x1b[33m', dim: '\x1b[2m' }
const SEV_TAG = { ERROR: `${C.red}ERROR${C.reset}`, WARNING: `${C.yellow}WARNING${C.reset}`, INFO: `${C.dim}INFO${C.reset}` }
const DRIFT_REPORT_MAX = 16 * 1024 * 1024
const DRIFT_FINDING_MAX = 10000
const DRIFT_FINDING_BYTES = DRIFT_REPORT_MAX - (128 * 1024)

function boundedText(value, maximum, nullable = true) {
  if (typeof value !== 'string') return nullable ? null : ''
  const clean = value.normalize('NFC')
    .replace(/[\u0000-\u001f\u007f-\u009f\u2028\u2029]+/g, ' ')
    .replace(/\s+/g, ' ').trim()
  const bounded = Array.from(clean).slice(0, maximum).join('')
  return bounded || (nullable ? null : '')
}

function publicFinding(finding) {
  const severity = ['ERROR', 'WARNING', 'INFO'].includes(finding.severity)
    ? finding.severity : 'INFO'
  return {
    severity,
    kind: boundedText(finding.kind, 100, false) || 'observed-contract-mismatch',
    area: boundedText(finding.area, 100),
    schemaRef: boundedText(finding.schemaRef, 200),
    operationId: boundedText(finding.operationId, 200),
    field: boundedText(finding.field, 200),
    dtoFile: boundedText(finding.dtoFile, 500),
    message: boundedText(finding.message, 1000, false) || 'contract drift detected',
    suggestion: boundedText(finding.suggestion, 1000),
  }
}

function boundedFindingSet(findings) {
  const rows = []
  const limitations = []
  let bytes = 2
  const countBounded = findings.slice(0, DRIFT_FINDING_MAX)
  for (const finding of countBounded) {
    const row = publicFinding(finding)
    const rowBytes = Buffer.byteLength(JSON.stringify(row), 'utf8') + 1
    if (bytes + rowBytes > DRIFT_FINDING_BYTES) {
      limitations.push('drift-finding-byte-cap')
      break
    }
    rows.push(row)
    bytes += rowBytes
  }
  if (findings.length > DRIFT_FINDING_MAX) limitations.push('drift-finding-count-cap')
  return { rows, limitations }
}

function writeDriftReport(report) {
  const file = contractPath('reports', 'drift.json')
  const directory = dirname(file)
  const bytes = Buffer.from(JSON.stringify(report, null, 2) + '\n')
  if (bytes.length > DRIFT_REPORT_MAX ||
      !fileGuards.realDirectoryUnder(PROJECT_ROOT, directory, { create: true, mode: 0o700 })) {
    throw new Error('drift-report-size-or-directory-invalid')
  }
  const result = fileGuards.atomicReplaceRegularFileResult(
    PROJECT_ROOT, directory, file, bytes,
    { create: true, directoryMode: 0o700, mode: 0o600, maxBytes: DRIFT_REPORT_MAX },
  )
  if (!result.ok) throw new Error('drift-report-write-failed')
  return file
}

function isDtoFile(relPath) {
  const parts = relPath.split('/')
  const i = parts.indexOf('data-services')
  return i >= 0 && parts[i + 1] === 'backend' && parts.slice(i + 2, -1).includes('dto')
}
// All folder segments under `dto/` for a DTO file, e.g. `dto/auth/v2/LoginBody.kt` -> ['auth','v2'].
// Used only as a tie-breaker hint for findSchema (the area-folder ↔ slice-area join is NOT reliable
// across real projects — plural/compound folders diverge from @ApiTags/path-derived areas — so the
// authoritative pairing is the global schema name; see the backend-contract-client skill (endpoint-inventory) and findSchema).
const dtoAreaHints = (relPath) => {
  const parts = relPath.split('/')
  const i = parts.lastIndexOf('dto')
  return i >= 0 ? parts.slice(i + 1, -1) : []                   // [] when flat (dto/File.kt) or no dto/
}

// ---- Kotlin parsing -------------------------------------------------------------
// Trim a raw type capture to the type token: stop at the first TOP-LEVEL `,` (next ctor param,
// when several share a line) or default-value `=`, but NOT a comma nested inside `<…>` — so a
// multi-arg generic like `Map<String, String>` survives whole instead of truncating to `Map<String`.
function trimKotlinType(raw) {
  let depth = 0
  for (let i = 0; i < raw.length; i++) {
    const c = raw[i]
    if (c === '<') depth++
    else if (c === '>') { if (depth > 0) depth--; }
    else if (depth === 0 && (c === ',' || c === '=')) return raw.slice(0, i)
  }
  return raw
}
function parseDtoClasses(text) {
  const classes = []
  const re = /data\s+class\s+([A-Za-z0-9_]+)\s*\(/g
  let m
  while ((m = re.exec(text))) {
    let depth = 1, i = re.lastIndex
    while (i < text.length && depth > 0) { if (text[i] === '(') depth++; else if (text[i] === ')') depth--; i++ }
    const block = text.slice(re.lastIndex, i - 1)
    const fields = []
    // Capture the rest of the line after the colon, then depth-trim it (the regex stops at \n; the
    // intra-line `,`/`=` boundary is handled by trimKotlinType so generics keep their inner commas).
    const pre = /@SerialName\(\s*"([^"]+)"\s*\)(?:\s*@[A-Za-z][A-Za-z0-9_]*(?:\([^)]*(?:\([^)]*\)[^)]*)*\))?)*\s*(?:public\s+|internal\s+|private\s+)?va[lr]\s+([A-Za-z0-9_]+)\s*:\s*([^\n]+)/g
    let f
    while ((f = pre.exec(block))) {
      const type = trimKotlinType(f[3]).trim()
      fields.push({ jsonName: f[1], kotlinName: f[2], type: type.replace(/\?$/, ''), nullable: type.endsWith('?') })
    }
    if (fields.length) classes.push({ className: m[1], fields })
  }
  return classes
}
// `${userId.encode()}` -> `{userId}`, `${userId}` -> `{userId}`: capture only the leading
// identifier of the first ${...} body so an interpolation with a function call still normalizes
// to the spec's bare `{param}`. Non-interpolated segments are returned untouched by the caller.
function normalizeSegment(seg) {
  if (!seg.includes('$')) return seg
  const m = seg.match(/\$\{\s*([A-Za-z_][A-Za-z0-9_]*)/)
  return `{${(m && m[1]) || 'param'}}`
}
function parseApiEndpoints(text) {
  const out = []
  const reCall = /request\s*\(/g           // start only — capture the args by paren balance below
  const reMethod = /method\s*=\s*HttpMethod\.([A-Za-z]+)/
  const rePath = /path\s*=\s*"([^"]+)"/
  let m
  while ((m = reCall.exec(text))) {
    let depth = 1, i = reCall.lastIndex     // same balanced scan as parseDtoClasses
    while (i < text.length && depth > 0) { if (text[i] === '(') depth++; else if (text[i] === ')') depth--; i++ }
    const args = text.slice(reCall.lastIndex, i - 1)
    reCall.lastIndex = i                    // resume past this call's closing ) (don't re-scan its inner ()s)
    const mMethod = reMethod.exec(args)
    const mPath = rePath.exec(args)
    if (!mMethod || !mPath) continue
    const path = '/' + mPath[1].split('/').filter(Boolean).map(normalizeSegment).join('/')
    out.push({ method: mMethod[1].toUpperCase(), path })
  }
  return out
}
const sameShape = (a, b) => {
  const sa = a.split('/').filter(Boolean), sb = b.split('/').filter(Boolean)
  return sa.length === sb.length && sa.every((s, i) => s === sb[i] || (s.startsWith('{') && sb[i].startsWith('{')))
}

// ---- type mapping ----------------------------------------------------------------
const KOTLIN_FOR = {
  string: 'String', integer: 'Long', number: 'Double', boolean: 'Boolean', object: 'JsonObject',
}
function suggestedKotlinType(f) {
  if (f.type.startsWith('ref:')) return f.type.slice(4)
  if (f.type === 'array') return `List<${f.itemsRef || 'String'}>`
  if (f.type === 'integer') return f.format === 'int64' ? 'Long' : 'Int'
  return KOTLIN_FOR[f.type] || 'String'
}
// Pull the element type out of a Kotlin collection base, e.g. `List<Note>` -> `Note`,
// `Set<Map<String, Int>>` -> `Map<String, Int>` (matched by outermost angle-bracket pair,
// stripped of a trailing `?`). Returns null when the base is not a `Collection<…>`.
function collectionElement(kotlinBase) {
  const m = kotlinBase.match(/^(?:List|MutableList|Set)<([\s\S]+)>$/)
  return m ? m[1].trim().replace(/\?$/, '') : null
}
function typeMatches(specField, kotlinBase) {
  if (specField.type.startsWith('ref:')) {
    const ref = specField.type.slice(4)
    // dotted-boundary: exact, or a package-qualified tail (`com.x.User` ~ `User`) — but NOT
    // a name that merely ends with the ref (`SuperUser` must not satisfy `User`).
    return kotlinBase === ref || kotlinBase.endsWith('.' + ref)
  }
  switch (specField.type) {
    case 'string': return kotlinBase === 'String'
    case 'integer': return ['Int', 'Long', 'Short'].includes(kotlinBase)
    case 'number': return ['Double', 'Float'].includes(kotlinBase)
    case 'boolean': return kotlinBase === 'Boolean'
    case 'array': {
      const el = collectionElement(kotlinBase)
      if (el == null) return false                    // not a List/Set/MutableList at all
      // Recurse on the element only when the spec actually recorded its type. The normalizer
      // sets itemsRef ONLY for object-element arrays (normalize.mjs:87); a primitive-element array
      // carries no element type at all (type stays 'array', itemsRef null) — there is nothing to
      // check against, so any collection element is accepted only in this
      // genuinely unknowable case.
      if (specField.itemsRef) return typeMatches({ type: `ref:${specField.itemsRef}` }, el)
      return true
    }
    default: return true // "object" — anything goes
  }
}

;(function main() {
  if (process.argv.length !== 2) {
    process.stderr.write('contract:diff accepts no arguments\n')
    process.exit(2)
  }
  const current = currentContractFiles()
  const invPath = current.inventory
  if (current.invalid) {
    failMsg(`current generation is invalid (${current.error})`)
    process.exit(summary('contract:diff'))
  }
  if (!invPath || !exists(invPath)) {
    // Gate ladder (backend-contract-client skill, drift): under `true` the snapshot is REQUIRED — a missing one is itself an
    // error (refresh one), not a silent skip. Under `auto`/absent keep the greenfield skip (exit 0).
    if (readConfig('backendContractEnabled') === 'true') {
      failMsg('no validated current snapshot but backendContractEnabled: true REQUIRES one — use Backend Test + Refresh or typed `contract:probe` then `contract:refresh-*`')
      process.exit(1)
    }
    info('no validated current snapshot — skipped, no report')
    process.exit(0)
  }
  let inventory
  try { inventory = readJson(invPath) } catch (e) { failMsg(`endpoint-inventory.json unreadable: ${e.message}`); process.exit(summary('contract:diff')) }

  // Slice index: schema name -> { fields, areas } (copies across area slices are identical).
  // Name-keyed only — the per-area-slice index is gone with W5-8: pairing is by schema NAME, not by
  // the (unreliable) dto-folder ↔ slice-area join, so findSchema needs only the global name index.
  const areasDir = current.areasDir
  const schemaIndex = Object.create(null)
  if (exists(areasDir)) {
    for (const f of readdirSync(areasDir)) {
      if (!f.endsWith('.json')) continue
      let slice
      try { slice = readJson(join(areasDir, f)) }
      catch (e) { failMsg(`manifests/areas/${f} unreadable: ${e.message}`); process.exit(summary('contract:diff')) }
      for (const [name, sch] of Object.entries(slice.schemas || {})) {
        if (!schemaIndex[name]) schemaIndex[name] = { fields: sch.fields || [], areas: [] }
        schemaIndex[name].areas.push(slice.area)
      }
    }
  }

  const findings = []
  const add = (severity, kind, rest) => findings.push({
    severity, kind,
    area: null, schemaRef: null, operationId: null, field: null, dtoFile: null,
    message: '', suggestion: null, ...rest,
  })

  // ---- 1) DTO files vs slices ----------------------------------------------------
  const inputs = projectInputs.collect(EXECUTION_ROOT, { includeText: true })
  if (!inputs.ok) {
    failMsg(`project input receipt unavailable: ${inputs.error}`)
    process.exit(summary('contract:diff'))
  }
  const ktFiles = inputs.records.filter((record) => record.path.endsWith('.kt'))
  const dtoFiles = ktFiles.filter((record) => isDtoFile(record.path))
  const apiClassName = readConfig('apiClassName') // null while still the <Product>Api placeholder

  if (!dtoFiles.length) {
    add('INFO', 'dto-layer-absent', {
      message: 'no DTO files found under **/data-services/backend/**/dto/ — pre-data-layer: DTO<->spec checks are idle until the data layer exists',
    })
  }

  // Global schema NAME match is authoritative (W5-8): the dto-folder ↔ slice-area join key is broken
  // in real projects, so we resolve a DTO class to a schema purely by name and use the folder area
  // only to TIE-BREAK when the same candidate name would otherwise be ambiguous. Candidates are tried
  // in priority order; for the winning name, if its slice set spans several areas and one matches a
  // folder hint, the finding is attributed to that area, otherwise to the schema's first area.
  const findSchema = (className, areaHints) => {
    const candidates = [className, `${className}Response`, `${className}Body`, className.replace(/Response$/, ''), className.replace(/Body$/, '')]
      .filter((c, i, a) => c && a.indexOf(c) === i)
    const hints = new Set(areaHints)
    for (const c of candidates) {
      const entry = schemaIndex[c]
      if (!entry) continue
      // attribute to a hinted area when the schema lives in one, else its first area
      const attributedArea = entry.areas.find((a) => hints.has(a)) ?? entry.areas[0] ?? null
      return { ref: c, area: attributedArea }
    }
    return null
  }

  for (const record of dtoFiles) {
    const dtoFile = record.path
    const areaHints = dtoAreaHints(dtoFile)
    const folderArea = areaHints[0] ?? null
    let classes
    try { classes = parseDtoClasses(record.text) } catch { continue }
    for (const cls of classes) {
      const match = findSchema(cls.className, areaHints)
      if (!match) {
        add('INFO', 'dto-schema-unmatched', {
          area: folderArea, dtoFile,
          message: `DTO class ${cls.className} has no matching schema in the snapshot — local-only, renamed, or removed server-side`,
        })
        continue
      }
      const { ref, area } = match   // area = the schema's attributed slice area (authoritative), not the folder
      const spec = schemaIndex[ref]
      const specByJson = new Map(spec.fields.map((f) => [f.jsonName, f]))
      const dtoByJson = new Map(cls.fields.map((f) => [f.jsonName, f]))
      for (const f of cls.fields) {
        const sf = specByJson.get(f.jsonName)
        if (!sf) {
          add('ERROR', 'dto-field-unknown', {
            area, schemaRef: ref, field: f.jsonName, dtoFile,
            message: `${cls.className}.${f.kotlinName} (@SerialName "${f.jsonName}") does not exist in server schema ${ref} — likely renamed or removed server-side`,
            suggestion: 'verify against spec/openapi.json, then rename/remove the DTO field (and its mapper line)',
          })
        } else if (!typeMatches(sf, f.type)) {
          add('ERROR', 'type-mismatch', {
            area, schemaRef: ref, field: f.jsonName, dtoFile,
            message: `${cls.className}.${f.kotlinName}: DTO declares ${f.type}, the spec says ${sf.type}${sf.format ? ` (${sf.format})` : ''}`,
            suggestion: `expected something like ${suggestedKotlinType(sf)}? = null`,
          })
        }
      }
      for (const sf of spec.fields) {
        if (dtoByJson.has(sf.jsonName)) continue
        add('WARNING', 'server-field-missing-in-dto', {
          area, schemaRef: ref, field: sf.jsonName, dtoFile,
          message: `server schema ${ref} has field "${sf.jsonName}" that ${cls.className} does not map`,
          suggestion: `@SerialName("${sf.jsonName}") val ${sf.name}: ${suggestedKotlinType(sf)}? = null,`,
        })
      }
    }
  }

  // ---- 2) <Product>Api paths vs the inventory -------------------------------------
  if (apiClassName) {
    const apiFile = ktFiles.find((record) => {
      const parts = record.path.split('/')
      return parts[parts.length - 1] === `${apiClassName}.kt` &&
        parts.includes('data-services')
    })
    if (apiFile) {
      for (const call of parseApiEndpoints(apiFile.text)) {
        const hit = inventory.endpoints.find((e) => e.method === call.method && sameShape(e.path, call.path))
        if (!hit) {
          add('ERROR', 'endpoint-missing-server-side', {
            dtoFile: apiFile.path, operationId: `${call.method} ${call.path}`,
            message: `${apiClassName} calls ${call.method} ${call.path} but the snapshot has no such endpoint — removed server-side, renamed, or the snapshot is stale`,
            suggestion: 'refresh the snapshot with Backend Test + Refresh or typed contract:probe then contract:refresh-*; if the endpoint is really gone, remove the Api method and its callers',
          })
        }
      }
    }
  }

  // ---- 3) slice-internal reality checks (run even pre-data-layer) -----------------
  for (const [name, { fields, areas }] of Object.entries(schemaIndex)) {
    const area = areas[0] ?? null
    for (const f of fields) {
      if (f.nullable_declared === false && f.nullable_observed === true) {
        add('INFO', 'nullability-mismatch', {
          area, schemaRef: name, field: f.jsonName,
          message: `${name}.${f.jsonName}: declared non-nullable but null was observed — observed contract mismatch; the defensive all-nullable DTO is justified`,
          suggestion: `keep ${f.name} nullable in the DTO and keep the mapper's log()?:return null guard`,
        })
      }
      if (Array.isArray(f.enum) && Array.isArray(f.enum_observed)) {
        const declared = new Set(f.enum.map(String))
        const extra = f.enum_observed.filter((v) => !declared.has(String(v)))
        if (extra.length) {
          add('WARNING', 'enum-new-value', {
            area, schemaRef: name, field: f.jsonName,
            message: `${name}.${f.jsonName}: observed enum value(s) ${extra.map((v) => `"${v}"`).join(', ')} beyond the spec enum [${f.enum.join(', ')}]`,
            suggestion: 'align the shared dictionary and mapper before relying on the spec enum',
          })
        }
        const observed = new Set(f.enum_observed.map(String))
        const never = f.enum.filter((v) => !observed.has(String(v)))
        if (never.length && f.enum_observed.length) {
          add('INFO', 'enum-declared-unobserved', {
            area, schemaRef: name, field: f.jsonName,
            message: `${name}.${f.jsonName}: declared enum value(s) ${never.map((v) => `"${v}"`).join(', ')} never observed in examples`,
          })
        }
      }
    }
  }

  // ---- report ----------------------------------------------------------------------
  let committed = null
  if (current.mode === 'generation') {
    committed = generation.currentAtProjectRoot(
      EXECUTION_ROOT, EXECUTION_SCOPE ? EXECUTION_SCOPE.apiGenerationHash : undefined)
    if (!committed.ok || committed.mode !== 'generation' ||
        committed.manifest.generationId !== current.committedGenerationId ||
        committed.snapshotHash !== current.snapshotHash ||
        committed.environmentId !== current.environmentId) {
      failMsg('current generation changed during drift analysis — rerun contract:diff')
      process.exit(1)
    }
  }
  const bounded = boundedFindingSet(findings)
  const boundedFindings = bounded.rows
  const tally = { errors: 0, warnings: 0, infos: 0 }
  for (const f of boundedFindings) {
    tally[f.severity === 'ERROR' ? 'errors' : f.severity === 'WARNING' ? 'warnings' : 'infos']++
  }
  const report = {
    schemaVersion: 1,
    checkedAt: new Date().toISOString(),
    specHash: (inventory.source && inventory.source.specHash) || null,
    committedGenerationId: committed ? committed.manifest.generationId : null,
    contractHash: committed ? committed.snapshotHash : null,
    environmentId: committed ? committed.environmentId : null,
    projectCodeRevision: inputs.ok ? inputs.projectCodeRevision : null,
    analyzerVersion: projectInputs.ANALYZER_VERSION,
    limitations: (inputs.ok ? [] : [inputs.error || 'project-input-revision-unavailable'])
      .concat(bounded.limitations),
    summary: tally,
    findings: boundedFindings,
  }
  const reportFile = writeDriftReport(report)
  for (const f of boundedFindings) {
    if (f.severity === 'WARNING') warnMsg(`${f.kind} — ${f.message}`)
    else console.log(`${SEV_TAG[f.severity]} ${f.kind} — ${f.message}`)
  }
  ok(`drift report: ${tally.errors} error(s), ${tally.warnings} warning(s), ${tally.infos} info(s) -> ${relative(PROJECT_ROOT, reportFile)} (suggestion-only — a human/builder applies any fix)`)
  process.exit(summary('contract:diff')) // exit 0 — findings live in the report, not the exit code
})()
