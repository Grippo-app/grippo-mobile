// compare-screen-spec.mjs — deterministic baseline for Figma spec -> implementation declared values.
// This is a bounded scanner. It catches high-confidence token/value mismatches and writes a JSON report
// for figma-spec-validator; it does not try to infer runtime layout or parse the whole Kotlin AST.
//
// Usage:
//   node scripts/compare-screen-spec.mjs <stem> --impl-file <Screen.kt> [--impl-file <Other.kt>] [--impl-root <dir>] [--screen-map Screen=Screen.kt] [--impl-model <model.json>] [--gate|--advisory]
//
// Env:
//   FIGMA_SCREEN_CACHE_ROOT or FIGMA_SPEC_SCREENS_DIR — override screens cache root (must match if both are set)
//   FIGMA_SPEC_IMPL_FILES    — path-delimited implementation files
//   FIGMA_SPEC_IMPL_ROOTS    — path-delimited implementation roots
//   FIGMA_SCREEN_IMPL_MAP    — path-delimited Screen=Implementation.kt mappings
//   FIGMA_APP_TOKENS         — pre-extracted app token evidence JSON
//   FIGMA_IMPL_MODEL         — optional Tree-sitter implementation model for element binding

import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs'
import { delimiter, isAbsolute, join, resolve } from 'node:path'
import { PROJECT_ROOT, displayPath, figmaPath, figmaScreensRoot, loadBindings, parseCli, EXECUTION_ROOT, executionProductInputPath, executionFigmaInputPath } from './_util.mjs'
import { extractAppTokens } from './extract-app-tokens.mjs'
import { classifyWidgetSource } from './lib/canvas-detect.mjs'
import { loadResolvedSpecs } from './resolve-screen-spec.mjs'
import { assertTaskStem, compileSchema, fileHash, schemaIssues, writeReport } from './report-utils.mjs'

const USAGE = 'usage: node scripts/compare-screen-spec.mjs <stem> --impl-file <Screen.kt> [--impl-file <Other.kt>] [--impl-root <dir>] [--screen-map Screen=Screen.kt] [--impl-model <path>] [--gate|--advisory]'

// A Figma set display name and a Kotlin composable name are different identity domains.
// The task binding artifact is the sole explicit join between them. Binding rows resolve
// only through the durable set node id; display labels never participate in authority.
function buildComponentBindings(bindings) {
  const bySetNodeId = new Map()
  for (const entry of (bindings && bindings.components) || []) {
    bySetNodeId.set(String(entry.setNodeId), entry)
  }
  return {
    resolve(element) {
      if (!element || !element.componentSetNodeId) return null
      return bySetNodeId.get(String(element.componentSetNodeId)) || null
    },
    entries: (bindings && bindings.components) || []
  }
}

// The Kotlin composable simple name an implementation binding names: the leaf
// segment of the adapter-namespaced symbol identity, discriminator stripped.
function boundComposableName(binding) {
  const implementation = binding && Array.isArray(binding.implementations) ? binding.implementations[0] : null
  if (!implementation || typeof implementation.projectComponentId !== 'string') return null
  const symbol = implementation.projectComponentId.split(':symbol:')[1] || ''
  const leaf = symbol.split('#')[0].split('.').pop()
  return leaf || null
}

// DEMAND-SCOPED component evidence. Only a source declared by the task's exact id-anchored
// binding enters the evidence scope. Broad scans or display-name joins could let an
// unrelated component satisfy the element and are intentionally absent.
function componentEvidenceSources({ specs, componentBindings }) {
  const found = new Map()   // absPath -> provenance note
  const add = (source, via) => {
    if (!source) return
    const abs = isAbsolute(String(source)) ? resolve(String(source)) : resolve(EXECUTION_ROOT, String(source))
    if (!found.has(abs)) found.set(abs, via)
  }
  const wanted = new Map()   // durable set node id -> element sample
  for (const spec of specs) for (const el of (spec.elements || [])) {
    if (el && el.componentSetNodeId) {
      wanted.set(String(el.componentSetNodeId), el)
    }
  }
  for (const element of wanted.values()) {
    const binding = componentBindings.resolve(element)
    if (!binding) continue
    for (const implementation of binding.implementations || []) {
      if (implementation.sourcePath) add(implementation.sourcePath, `binding:${binding.designComponentId}`)
    }
  }
  return found
}
const SOURCE_EXT = /\.(kt|kts)$/i

function issue(severity, issueKind, message, extra = {}) {
  return Object.assign({ severity, issueKind, message }, extra)
}

function hasToken(evidence, token) {
  return !!(token && evidence.tokens && evidence.tokens[token])
}

function hasRawColor(evidence, hex) {
  const colors = (evidence.raw && evidence.raw.colors) || null
  if (!hex || !colors) return false
  const probe = String(hex).toUpperCase()
  if (colors[probe]) return true
  // 8-digit spec hex could never match the 6-digit evidence keys extract-app-tokens stores.
  // Normalize: full alpha (trailing FF) equals the 6-digit RGB key — the spec convention is
  // #RRGGBBAA (normalizePaint passes the pull's value through verbatim-uppercased). Only the
  // trailing-alpha (#RRGGBBFF) arm is sound: an alpha-first #FFRRGGBB arm would let a spec
  // fill #FF0000FF (red, #RRGGBBAA) be satisfied by an app that only uses #0000FF (blue) —
  // a cross-color false PASS for every spec color with R=FF. Translucent probes only match
  // the exact 8-digit #RRGGBBAA key that extract-app-tokens stores for non-FF literals.
  const m = /^#([0-9A-F]{8})$/.exec(probe)
  if (!m) return false
  const d = m[1]
  if (d.slice(6) === 'FF' && colors[`#${d.slice(0, 6)}`]) return true   // #RRGGBBFF
  return false
}

function dpValues(el) {
  const out = []
  if (typeof el.cornerRadiusDp === 'number') out.push(el.cornerRadiusDp)
  for (const s of el.strokes || []) if (typeof s.widthDp === 'number') out.push(s.widthDp)
  if (el.paddingDp) for (const v of Object.values(el.paddingDp)) if (typeof v === 'number') out.push(v)
  for (const v of Object.values(el.gapsToSiblingsDp || {})) if (typeof v === 'number') out.push(v)
  return [...new Set(out.filter((v) => v > 0).map((v) => String(Number(v))))]
}

function missingDpValues(evidence, values, theme) {
  return values.filter((v) => !hasDpEvidence(evidence, v, theme))
}

function weightKeys(weight) {
  const n = Number(weight)
  const names = { 100: 'Thin', 200: 'ExtraLight', 300: 'Light', 400: 'Normal', 500: 'Medium', 600: 'SemiBold', 700: 'Bold', 800: 'ExtraBold', 900: 'Black' }
  return [`W${n}`, names[n]].filter(Boolean)
}

function normalizedTokenKey(key, kind) {
  const raw = String(key || '').trim().replace(/^AppTokens\./, '').replace(/[\/\s-]+/g, '.')
  if (!raw) return ''
  return raw.startsWith(`${kind}.`) ? raw : `${kind}.${raw}`
}

function tokenWasUsed(evidence, key) {
  const normalized = String(key || '')
  const tokens = evidence.tokens || {}
  return !!tokens[normalized] || !!tokens[`AppTokens.${normalized}`]
}

function modeValue(value, theme) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return value
  if (Object.prototype.hasOwnProperty.call(value, 'value')) return value.value
  if (Object.prototype.hasOwnProperty.call(value, 'resolvedValue')) return value.resolvedValue
  const themeKey = String(theme || '').toLowerCase()
  const candidates = [themeKey, themeKey === 'light' ? 'primary' : '', 'primary', 'default', 'base'].filter(Boolean)
  for (const key of candidates) {
    if (Object.prototype.hasOwnProperty.call(value, key)) return value[key]
  }
  const numericValues = Object.values(value).filter((v) => Number.isFinite(Number(v))).map((v) => Number(v))
  if (numericValues.length && numericValues.every((v) => v === numericValues[0])) return numericValues[0]
  return value
}

function numericMatches(value, expected, theme) {
  const selected = modeValue(value, theme)
  return Number.isFinite(Number(selected)) && Number(selected) === Number(expected)
}

function weightMatches(value, expected) {
  if (Number.isFinite(Number(value))) return Number(value) === Number(expected)
  return weightKeys(expected).some((key) => String(value || '').toLowerCase() === key.toLowerCase())
}

function styleMatches(style, textStyle, field, theme) {
  const selected = modeValue(style, theme)
  if (!selected || typeof selected !== 'object') return false
  if (field === 'weight') return weightMatches(selected.weight ?? selected.fontWeight, textStyle.weight)
  return numericMatches(selected[field], textStyle[field], theme)
}

function tokenValueCandidates(evidence, kind) {
  const out = []
  const add = (key, value) => {
    const normalized = normalizedTokenKey(key, kind)
    if (normalized && tokenWasUsed(evidence, normalized)) out.push({ key: normalized, value })
  }
  for (const [key, entry] of Object.entries(evidence.tokens || {})) {
    const normalized = normalizedTokenKey(key, kind)
    if (!normalized.startsWith(`${kind}.`)) continue
    if (entry && typeof entry === 'object') {
      const value = entry.value ?? entry.resolvedValue ?? entry.resolved ?? entry.style ?? entry.textStyle ?? entry.typography ?? entry.dp ?? entry.sp
      if (value !== undefined) add(normalized, value)
    }
  }
  for (const rootKey of ['values', 'resolvedValues', 'tokenValues']) {
    const root = evidence[rootKey]
    if (!root || typeof root !== 'object') continue
    const group = root[kind] || (kind === 'dp' ? root.dimensions : null)
    if (!group || typeof group !== 'object') continue
    for (const [key, value] of Object.entries(group)) add(key, value)
  }
  return out
}

function hasDpEvidence(evidence, value, theme) {
  const rawDp = (evidence.raw && evidence.raw.dp) || {}
  if (rawDp[String(Number(value))]) return true
  return tokenValueCandidates(evidence, 'dp').some((candidate) => numericMatches(candidate.value, value, theme))
}

function hasTypographyEvidence(evidence, textStyle, field, theme) {
  if (!textStyle || typeof textStyle[field] !== 'number') return true
  if (field === 'weight') {
    const weights = evidence.raw && evidence.raw.fontWeights ? evidence.raw.fontWeights : {}
    if (weightKeys(textStyle.weight).some((key) => weights[key])) return true
  } else {
    const sp = evidence.raw && evidence.raw.sp ? evidence.raw.sp : {}
    if (sp[String(Number(textStyle[field]))]) return true
  }
  return tokenValueCandidates(evidence, 'typography').some((candidate) => styleMatches(candidate.value, textStyle, field, theme))
}

function missingTextEvidence(evidence, textStyle, theme) {
  if (!textStyle) return []
  const missing = []
  if (!hasTypographyEvidence(evidence, textStyle, 'sizeSp', theme)) missing.push(`sizeSp:${Number(textStyle.sizeSp)}`)
  if (!hasTypographyEvidence(evidence, textStyle, 'lineHeightSp', theme)) missing.push(`lineHeightSp:${Number(textStyle.lineHeightSp)}`)
  if (!hasTypographyEvidence(evidence, textStyle, 'weight', theme)) missing.push(`weight:${Number(textStyle.weight)}`)
  return missing
}

// W2-3: a call belongs to the screen when it (a) lives in the screen's DECLARED
// implementation file (explicit screen map/binding, or the sole implementation file) AND
// (b) the file was explicitly bound to that screen. The declared file boundary is the
// authority; owner-name prefixes/suffixes are not identity and are never used to rebind it.
function callInImplementationFile(call, implFile) {
  const callFile = call && call.file
  if (!callFile || !implFile) return false
  const a = isAbsolute(callFile) ? resolve(callFile) : resolve(EXECUTION_ROOT, callFile)
  const b = isAbsolute(implFile) ? resolve(implFile) : resolve(EXECUTION_ROOT, implFile)
  return a === b
}

function implementationHasComponent(model, componentName, screenOwner, implFile = null) {
  const expected = String(componentName || '')
  if (!expected || !model || !Array.isArray(model.calls)) return false
  let matching = model.calls.filter((call) => String((call && call.callee) || '') === expected)
  if (implFile) matching = matching.filter((call) => callInImplementationFile(call, implFile))
  const hasOwnerEvidence = model.calls.some((call) => call && (call.owner || call.ownerComposable))
  if (!screenOwner || !hasOwnerEvidence) return matching.length > 0
  return matching.some((call) => String(call.owner || call.ownerComposable) === String(screenOwner))
}

// R2 element→code binding: bind a spec element to ONE specific code call so the comparison can be
// element-precise instead of file-scoped. Reliable only for a component INSTANCE
// (componentSetNodeId) whose explicit binding names the callee, scoped to the screen owner when the model carries owner
// evidence. A component called MORE THAN ONCE under the owner is AMBIGUOUS → null (we cannot say
// WHICH instance is this element) → the caller blocks because identity is unresolved. Absent
// model / no id-anchored component binding → null too.
function boundCallFor(element, model, screenOwner, implFile = null, expectedComponentName = null) {
  if (!element.componentSetNodeId || !model || !Array.isArray(model.calls)) return null
  const expected = String(expectedComponentName || '')
  if (!expected) return null
  let matches = model.calls.filter((call) => String((call && call.callee) || '') === expected)
  // File scope first (W2-3): the declared implementation file is the authoritative screen
  // boundary. Owner-closure entries (ownerVia) duplicate a call per attributed owner —
  // collapse them by call site so a closure duplicate can never make a unique binding
  // read AMBIGUOUS.
  if (implFile) matches = matches.filter((call) => callInImplementationFile(call, implFile))
  const hasOwnerEvidence = model.calls.some((call) => call && (call.owner || call.ownerComposable))
  if (screenOwner && hasOwnerEvidence) {
    matches = matches.filter((call) => String(call.owner || call.ownerComposable) === String(screenOwner))
  }
  const bySite = new Map()
  for (const call of matches) bySite.set(`${call.file}:${call.line}:${call.callee}`, call)
  const sites = [...bySite.values()]
  return sites.length === 1 ? sites[0] : null
}

// The #RRGGBBAA the resolved Figma spec compares against — the pull writes resolvedValue as
// #RRGGBBAA (or #RRGGBB, which is opaque → +FF). Alpha is KEPT (matches the model's
// Color(0xAARRGGBB)→#RRGGBBAA extraction), so a translucent scrim never false-matches a solid fill.
function specRgba(hex) {
  const s = String(hex || '').trim().replace(/^#/, '').toUpperCase()
  if (/^[0-9A-F]{8}$/.test(s)) return `#${s}`
  if (/^[0-9A-F]{6}$/.test(s)) return `#${s}FF`
  return null
}

// A code token ref (AppTokens.colors.x / AppColor.x) normalized to the spec's appToken key
// (colors.x). Used to check whether a bound call ALREADY uses the expected token — if it does,
// a coincidental raw colour on some OTHER arg of the same call is not a hardcode of THIS fill.
function normalizeCodeToken(ref) {
  return String(ref || '').replace(/^AppTokens\./, '').replace(/^AppColor\./, 'colors.')
}

// A spec dp is satisfied by a bound call's dp args if it equals one of them OR a subset SUM
// (composed-equivalence: spec 16 = code 8+8; padding + Arrangement.spacedBy). Bounded subset-sum.
function dpComposable(target, values) {
  const t = Number(target)
  if (!Number.isFinite(t)) return false
  const vs = values.map(Number).filter((v) => Number.isFinite(v) && v > 0)
  if (vs.some((v) => Math.abs(v - t) < 1e-6)) return true
  if (vs.length > 12) return false   // guard the combinatorial reachable-set against a pathological arg list
  let reachable = new Set([0])
  for (const v of vs) {
    const next = new Set(reachable)
    for (const r of reachable) { const s = r + v; if (s <= t + 1e-6) next.add(s) }
    reachable = next
  }
  return [...reachable].some((s) => Math.abs(s - t) < 1e-6)
}

function loadEvidence({ files, roots, appTokensPath = '' }) {
  const p = appTokensPath
  if (p && existsSync(p)) return JSON.parse(readFileSync(p, 'utf8'))
  // resolveAliases: recover hoisted-alias token usage (`val c = AppTokens.colors.group` … `c.leaf`)
  // that a Canvas/DrawScope widget is forced into — the exact-match token scan otherwise misses the
  // full path and false-BLOCKERs. Opt-in here only; census/drift extraction is unchanged.
  return extractAppTokens({ files, roots, resolveAliases: true })
}

function collectSourceFiles(root, out = []) {
  if (!existsSync(root)) return out
  let entries = []
  try { entries = readdirSync(root, { withFileTypes: true }) } catch { return out }
  for (const entry of entries) {
    const p = join(root, entry.name)
    if (entry.isDirectory()) {
      if (!['.git', '.gradle', '.idea', 'build', 'node_modules', 'orchestrator'].includes(entry.name)) collectSourceFiles(p, out)
    } else if (entry.isFile() && SOURCE_EXT.test(entry.name)) {
      out.push(p)
    }
  }
  return out
}

function parseScreenMap(entries) {
  const out = new Map()
  const issues = []
  for (const raw of entries) {
    const idx = String(raw || '').indexOf('=')
    if (idx <= 0) {
      issues.push(issue('BLOCKER', 'SCREEN_MAP_INVALID', `screen map entry must be Screen=Implementation.kt, got ${JSON.stringify(raw)}`))
      continue
    }
    const screen = String(raw).slice(0, idx).trim()
    let file = String(raw).slice(idx + 1).trim()
    if (!screen || !file) {
      issues.push(issue('BLOCKER', 'SCREEN_MAP_INVALID', `screen map entry must include both screen and file, got ${JSON.stringify(raw)}`))
      continue
    }
    try { file = executionProductInputPath(file, `screen map for ${screen}`) }
    catch (error) {
      issues.push(issue('BLOCKER', 'SCREEN_MAP_INVALID', error.message))
      continue
    }
    out.set(screen, file)
  }
  return { map: out, issues }
}

function evidenceCount(evidence) {
  const raw = evidence.raw || {}
  return Object.keys(evidence.tokens || {}).length +
    Object.keys(raw.colors || {}).length +
    Object.keys(raw.dp || {}).length +
    Object.keys(raw.sp || {}).length +
    Object.keys(raw.fontWeights || {}).length
}

function tokenValueEvidenceCount(evidence) {
  return tokenValueCandidates(evidence, 'dp').length + tokenValueCandidates(evidence, 'typography').length
}

function pathKind(path) {
  try {
    const st = statSync(path)
    return st.isFile() ? 'file' : st.isDirectory() ? 'dir' : 'other'
  } catch {
    return 'missing'
  }
}

function modelProblemIssues(model) {
  const out = []
  const overall = String((model && model.overall) || '').toUpperCase()
  if (overall && overall !== 'PASS') out.push({ issueKind: 'IMPLEMENTATION_MODEL_NOT_PASS', message: `implementation model overall is ${overall}` })
  for (const file of (model && Array.isArray(model.files) ? model.files : [])) {
    if (file && file.parseStatus && file.parseStatus !== 'OK') {
      out.push({ issueKind: 'IMPLEMENTATION_MODEL_PARSE_ERROR', message: `implementation model file ${file.path || '(unknown)'} parseStatus is ${file.parseStatus}`, file: file.path || null })
    }
    if (file && Array.isArray(file.parseErrors) && file.parseErrors.length) {
      out.push({ issueKind: 'IMPLEMENTATION_MODEL_PARSE_ERROR', message: `implementation model file ${file.path || '(unknown)'} has ${file.parseErrors.length} parse error(s)`, file: file.path || null })
    }
  }
  if (model && Array.isArray(model.unresolvedRefs) && model.unresolvedRefs.length) {
    out.push({ issueKind: 'IMPLEMENTATION_MODEL_UNRESOLVED_REFS', message: `implementation model has ${model.unresolvedRefs.length} unresolved ref(s)` })
  }
  for (const modelIssue of (model && Array.isArray(model.issues) ? model.issues : [])) {
    const sev = String((modelIssue && modelIssue.severity) || '').toUpperCase()
    if (['BLOCKER', 'ERROR', 'FAIL', 'WARN', 'WARNING', 'REVIEW_REQUIRED'].includes(sev)) {
      out.push({ issueKind: 'IMPLEMENTATION_MODEL_ISSUES', message: `implementation model carries ${sev} issue ${modelIssue.issueKind || ''}`.trim() })
    }
  }
  const seen = new Set()
  return out.filter((row) => {
    const key = `${row.issueKind}:${row.message}:${row.file || ''}`
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

function compareElement({ spec, element, evidence, implementationModel, gate, implFile = null, screenOwner = null, componentBindings = null }) {
  const issues = []
  const identity = element.identity
  const base = { screen: spec.screen, theme: spec.theme, file: spec.file, stableId: identity, elementName: element.name }
  // W3-2: an element whose figmaNodeId carries the Figma instance-child prefix (`I<id>;…`)
  // is a node INSIDE a placed component instance — its paints are the component's internals.
  const isInstanceDescendant = /^I[^;]+;/.test(String(element.figmaNodeId || ''))
  const componentBinding = element.componentSetNodeId && componentBindings
    ? componentBindings.resolve(element)
    : null
  const expectedComponentName = boundComposableName(componentBinding)
  if (element.componentSetNodeId) {
    if (!expectedComponentName) {
      issues.push(issue('BLOCKER', 'COMPONENT_BINDING_REQUIRED', `component set ${element.componentSetName || element.componentSetNodeId} requires an id-anchored components[] binding (census + mapping registry) to its Kotlin composable`, Object.assign({ expectedComponentSet: element.componentSetName || element.componentSetNodeId }, base)))
    } else if (!implementationModel) {
      issues.push(issue(gate ? 'BLOCKER' : 'WARN', 'COMPONENT_MODEL_REQUIRED', `expected component ${element.componentSetName}; implementation model is required to compare component usage`, Object.assign({ expectedComponent: element.componentSetName }, base)))
    } else if (!implementationHasComponent(implementationModel, expectedComponentName, screenOwner || spec.screen, implFile)) {
      issues.push(issue('BLOCKER', 'MISSING_COMPONENT_CALL', `component set ${element.componentSetName} is explicitly bound to ${expectedComponentName}; implementation model has no exact matching composable call`, Object.assign({ expectedComponent: expectedComponentName, expectedComponentSet: element.componentSetName }, base)))
    }
  }
  if (element.variantProps && Object.keys(element.variantProps).length) {
    // The screen implementation model proves exact component-call identity but does not carry
    // mapping-registry property/value transforms. The universal component comparator owns that
    // semantic proof. Keep this screen-axis result explicitly unproven: it cannot become a
    // visual/semantic match, while the task's reviewed-caveat path remains available.
    issues.push(issue('WARN', 'COMPONENT_VARIANT_ASSIGNMENT_UNPROVEN', `screen evidence does not prove the mapped project property assignment for component variant ${JSON.stringify(element.variantProps)}`, Object.assign({ expectedVariantProps: element.variantProps }, base)))
  }
  for (const paint of [...(element.fills || []), ...(element.strokes || []).map((s) => s.color)]) {
    if (paint.tokenRef) {
      if (!paint.appToken) {
        issues.push(issue('BLOCKER', 'TOKEN_BINDING_UNAVAILABLE',
          `design token ${paint.tokenRef} has no unique current observed-token binding for this screen context`,
          Object.assign({ expectedTokenRef: paint.tokenRef }, base)))
        continue
      }
      if (hasToken(evidence, paint.appToken)) continue
      if (paint.resolvedValue && hasRawColor(evidence, paint.resolvedValue)) {
        // Raw color where a token is expected — a real defect, BLOCKER for every widget class
        // including canvas.
        issues.push(issue('BLOCKER', 'HARDCODED_COLOR_FOR_TOKEN', `expected ${paint.appToken}, implementation uses raw ${paint.resolvedValue}`, Object.assign({ expectedToken: paint.appToken, expectedValue: paint.resolvedValue }, base)))
      } else {
        // W3-2 delegation: the token is unresolved even after the W3-1 component-source
        // evidence merge, but the paint belongs to a REUSED component with one exact bound
        // call or to a node inside a placed instance
        // (`I…;…` figmaNodeId). The spec's tokenRef names the DESIGN token while the
        // component internally binds its own semantic slot — a screen-level re-proof is
        // structurally impossible and the value is certified by the COMPONENT's own gates.
        // WARN (rides the reviewed-WARN caveat path), never silent. ORDERING IS LOAD-BEARING:
        // the HARDCODED_COLOR_FOR_TOKEN contradiction above stays a BLOCKER even for
        // delegated elements; otherwise delegation could launder a real hardcode.
        const delegated = isInstanceDescendant ||
          (expectedComponentName && implementationModel && boundCallFor(element, implementationModel, screenOwner || spec.screen, implFile, expectedComponentName))
        if (delegated) {
          issues.push(issue('WARN', 'COMPONENT_COLOR_DELEGATED', `expected ${paint.appToken} from ${paint.tokenRef} — the paint is ${isInstanceDescendant ? 'inside a placed component instance (instance-internal node)' : `owned by the reused component ${element.componentSetName}`}; its token binding lives in the component's own source and is certified by the component's own gates (delegated)`, Object.assign({ expectedToken: paint.appToken, delegated: true }, base)))
        } else {
          // Genuine missing token — BLOCKER for every widget class. Canvas/DrawScope widgets that hoist
          // the token (`val c = AppTokens.colors.g` … `c.leaf`) are already reconciled by the evidence
          // alias resolution (loadEvidence resolveAliases), so a correct canvas widget passes hasToken
          // above; reaching here means the token is not actually used. (An unbindable hoist form such
          // as a `with(group){…}` block stays a BLOCKER — use a direct `val` hoist.)
          issues.push(issue('BLOCKER', 'MISSING_COLOR_TOKEN', `expected ${paint.appToken} from ${paint.tokenRef}`, Object.assign({ expectedToken: paint.appToken }, base)))
        }
      }
    } else if (paint.resolvedValue && !hasRawColor(evidence, paint.resolvedValue)) {
      issues.push(issue('WARN', 'COLOR_EVIDENCE_MISSING', `expected color ${paint.resolvedValue}; no matching raw color or AppTokens.colors usage found`, Object.assign({ expectedValue: paint.resolvedValue }, base)))
    }
  }

  const dps = dpValues(element)
  const missingDp = missingDpValues(evidence, dps, spec.theme)
  if (missingDp.length) {
    issues.push(issue('WARN', 'DP_EVIDENCE_MISSING', `expected dp values [${dps.join(', ')}]; missing exact raw dp literal(s) or exact AppTokens.dp value evidence: [${missingDp.join(', ')}]`, Object.assign({ expectedDp: dps, missingDp }, base)))
  }
  const missingText = missingTextEvidence(evidence, element.textStyle, spec.theme)
  if (missingText.length) {
    issues.push(issue('WARN', 'TYPOGRAPHY_EVIDENCE_MISSING', `textStyle exists in Figma spec but exact raw typography evidence or exact AppTokens.typography value evidence is missing for ${missingText.join(', ')}`, Object.assign({ missingText }, base)))
  }

  // R2 — element-scoped strict pass (PRIMARY when the element binds). The checks above are
  // FILE-SCOPED (a token/dp used ANYWHERE satisfies any element). When a component element binds
  // to ONE specific code call, assert element-precise CONTRADICTIONS the resolved model proves —
  // never mere absence (that would deadlock builders on a value delegated to a component default).
  // An unbindable component instance cannot be verified element-precisely and therefore blocks;
  // file-scoped evidence must never substitute for a missing identity binding.
  let binding = 'n/a'
  if (implementationModel && element.componentSetNodeId) {
    const bound = boundCallFor(element, implementationModel, screenOwner || spec.screen, implFile, expectedComponentName)
    if (bound) {
      binding = 'resolved'
      const boundRef = `${bound.callee}:${bound.line}`
      const ebase = Object.assign({ binding, boundCall: boundRef }, base)
      const callColors = new Set(((bound.args && bound.args.colors) || []).map((c) => String(c).toUpperCase()))
      const callTokenKeys = new Set(((bound.args && bound.args.tokens) || []).map(normalizeCodeToken))
      const callDp = (bound.args && bound.args.dp) || []
      // (a) element-precise HARDCODED-COLOR HINT — WARN, deliberately NOT a BLOCKER. The bound
      // call's colour args are an UNDIFFERENTIATED bag: the model records every Color(0x…) literal
      // in the call without knowing which parameter it was passed to. So a raw colour matching a
      // fill token's value could be a genuine hardcode OF THAT FILL, or a coincidental raw on an
      // UNRELATED arg (ripple/gradient) — indistinguishable without per-arg binding. A hard block
      // would false-block correct code (a proven deadlock). It is therefore an advisory WARN,
      // FURTHER guarded so it only fires when the call does NOT already use the expected token
      // (if the fill token IS present at the call, a coincidental sibling raw is not this fill's
      // hardcode). Alpha is compared (specRgba) so a translucent literal never false-matches a
      // solid fill. The STRICT "wrong/missing token" BLOCKER is the file-scoped pass above,
      // element-named already; this only adds the bound-call location as a hint.
      for (const paint of [...(element.fills || []), ...(element.strokes || []).map((s) => s.color)]) {
        if (paint && paint.tokenRef && paint.resolvedValue) {
          const rgba = specRgba(paint.resolvedValue)
          if (rgba && callColors.has(rgba) && !callTokenKeys.has(paint.appToken)) {
            issues.push(issue('WARN', 'ELEMENT_HARDCODED_COLOR_HINT', `element ${element.name} binds to ${bound.callee}() (${bound.file}:${bound.line}) which passes raw ${rgba} matching ${paint.appToken} and does not use that token at the call — verify it is not hardcoding the fill (may be a coincidental raw on an unrelated arg)`, Object.assign({ expectedToken: paint.appToken, expectedValue: paint.resolvedValue }, ebase)))
          }
        }
      }
      // (b) element-precise DP divergence — WARN, deliberately NOT a BLOCKER. A component
      // instance's spec dp (cornerRadius, internal padding) is usually INTERNAL to the component,
      // not passed at the call site, whereas the call's dp args are often EXTERNAL modifiers
      // (margin) — so a call-arg-vs-spec-dp mismatch is genuinely ambiguous and a hard block would
      // false-block legitimate calls and deadlock builders (the sanctioned fail-safe: uncertain →
      // WARN). It still surfaces (ships as a ### Caveats bullet) for the builder to eyeball. An
      // EMPTY call dp set (value delegated to the component default) is not flagged at all.
      const specDp = dpValues(element).map(Number).filter((v) => Number.isFinite(v) && v > 0)
      if (callDp.length && specDp.length && !specDp.some((d) => dpComposable(d, callDp))) {
        issues.push(issue('WARN', 'ELEMENT_DP_DIVERGENCE', `element ${element.name} binds to ${bound.callee}() (${bound.file}:${bound.line}) whose dp args [${callDp.join(', ')}] match none of the spec dp [${specDp.join(', ')}] nor any composed sum — verify (the spec dp may be a component-internal value, not a call arg)`, Object.assign({ expectedDp: specDp, callDp }, ebase)))
      }
    } else {
      binding = 'unresolved'
      issues.push(issue('BLOCKER', 'ELEMENT_BINDING_UNRESOLVED', `component element ${element.componentSetName} (${element.name}) could not be bound to exactly one code call — element-precise verification is required`, Object.assign({ binding, expectedComponent: element.componentSetName }, base)))
    }
  }
  return { issues, binding }
}

async function main() {
  let cli
  try {
    cli = parseCli({
      allowedFlags: ['--stem', '--impl-file', '--impl-root', '--screen-map', '--impl-model', '--gate', '--advisory'],
      valueFlags: ['--stem', '--impl-file', '--impl-root', '--screen-map', '--impl-model'],
      booleanFlags: ['--gate', '--advisory'],
      usage: USAGE,
    })
  } catch (e) {
    console.error(e.message)
    process.exit(1)
  }
  if (cli.has('--gate') && cli.has('--advisory')) {
    console.error('ERROR: choose only one of --gate or --advisory')
    process.exit(1)
  }
  let stem
  try { stem = assertTaskStem(cli.value('--stem') || cli.positional[0] || '') }
  catch { console.error(USAGE); process.exit(1) }
  const runMode = cli.has('--gate') ? 'gate' : 'advisory'
  const gate = runMode === 'gate'
  let files = [
    ...cli.valuesFor('--impl-file'),
    ...(process.env.FIGMA_SPEC_IMPL_FILES ? process.env.FIGMA_SPEC_IMPL_FILES.split(delimiter).filter(Boolean)
      .map((value) => executionProductInputPath(value, 'FIGMA_SPEC_IMPL_FILES')) : []),
  ]
  const roots = [
    ...cli.valuesFor('--impl-root'),
    ...(process.env.FIGMA_SPEC_IMPL_ROOTS ? process.env.FIGMA_SPEC_IMPL_ROOTS.split(delimiter).filter(Boolean)
      .map((value) => executionProductInputPath(value, 'FIGMA_SPEC_IMPL_ROOTS')) : []),
  ]
  const implModelPath = cli.value('--impl-model') ||
    (process.env.FIGMA_IMPL_MODEL
      ? executionFigmaInputPath(process.env.FIGMA_IMPL_MODEL, 'FIGMA_IMPL_MODEL') : '')
  const appTokensPath = process.env.FIGMA_APP_TOKENS
    ? executionFigmaInputPath(process.env.FIGMA_APP_TOKENS, 'FIGMA_APP_TOKENS') : ''
  const screensRoot = figmaScreensRoot()
  const resolved = loadResolvedSpecs({ stem, screensRoot })
  const inputHashes = Object.assign({}, resolved.inputHashes)

  const issues = []
  for (const u of (resolved.unreadable || [])) {
    const msg = u.kind === 'SCREENS_DIR_UNREADABLE' ? `screens dir unreadable: ${u.message}` : `spec file unreadable: ${u.message}`
    issues.push(issue('BLOCKER', u.kind || 'SPEC_UNREADABLE', msg, { file: u.file }))
  }

  // index.json is the pull's authoritative node registry when present. Invalid or
  // inconsistent bytes are blockers in this gate too; no stale spec is silently skipped.
  // Component-kind nodes (a screen's extracted sub-frames pulled as component references)
  // are compared but are NOT screens: they never force the multi-screen impl-mapping path.
  const indexKinds = new Map()
  let indexNodesPresent = false
  const indexPath = join(resolved.screensDir, 'index.json')
  if (existsSync(indexPath)) {
    try {
      const idx = JSON.parse(readFileSync(indexPath, 'utf8'))
      const validateIndex = await compileSchema(figmaPath('token-schemas', 'screen-index.schema.json'), { gate: true })
      const indexSchemaIssues = schemaIssues(validateIndex, idx, 'screenIndex:')
      if (indexSchemaIssues.length || idx.taskStem !== stem) {
        for (const schemaIssue of indexSchemaIssues) issues.push(issue('BLOCKER', 'SCREEN_INDEX_INVALID', `screen index schema invalid at ${schemaIssue.path}: ${schemaIssue.message}`, { file: displayPath(indexPath), path: schemaIssue.path }))
        if (idx && idx.taskStem !== stem) issues.push(issue('BLOCKER', 'SCREEN_INDEX_INVALID', `screen index taskStem must be ${stem}`, { file: displayPath(indexPath) }))
      } else {
      indexNodesPresent = true
        for (const [name, node] of Object.entries(idx.nodes)) indexKinds.set(name, node.kind)
      }
      inputHashes[indexPath] = fileHash(indexPath)
    } catch (error) {
      issues.push(issue('BLOCKER', 'SCREEN_INDEX_INVALID', `screen index is unreadable: ${error.message}`, { file: displayPath(indexPath) }))
    }
  }
  if (indexNodesPresent) {
    for (const spec of resolved.specs) {
      if (indexKinds.has(spec.screen)) continue
      issues.push(issue('BLOCKER', 'SPEC_NOT_IN_INDEX', `spec ${spec.file} (screen ${spec.screen}) is not declared in index.json`, { file: spec.file, screen: spec.screen }))
    }
  }

  if (!resolved.specs.length) issues.push(issue('BLOCKER', 'NO_SPEC_FILES', `no *.spec.json found for stem ${stem}`, { file: resolved.screensDir }))
  for (const spec of resolved.specs) {
    if (!((spec.elements || []).length)) {
      issues.push(issue('BLOCKER', 'NO_COMPARABLE_ELEMENTS', `spec ${spec.file} has no comparable elements`, { file: spec.file, screen: spec.screen, theme: spec.theme }))
    }
  }
  const screenNames = [...new Set(resolved.specs.map((spec) => spec.screen))].sort()
  // Only SCREEN-like nodes participate in the multi-screen impl-mapping requirement; a
  // component-kind node validates against the shared evidence (its own source file is in
  // scope via --impl-file / the W3-1 component-source merge) and needs no per-screen file.
  const mappingNames = screenNames.filter((name) => (!indexNodesPresent || indexKinds.has(name)) && (indexKinds.get(name) || 'screen') !== 'component')
  const screenMapEntries = [
    ...cli.valuesFor('--screen-map'),
    ...(process.env.FIGMA_SCREEN_IMPL_MAP ? process.env.FIGMA_SCREEN_IMPL_MAP.split(delimiter).filter(Boolean) : []),
  ]
  const parsedScreenMap = parseScreenMap(screenMapEntries)
  issues.push(...parsedScreenMap.issues)
  let screenImplementationMap = parsedScreenMap.map
  // bindings.json and CLI/env are two explicit input surfaces. Conflicting declarations
  // block; neither silently overrides the other.
  const taskBindings = loadBindings(stem)
  const componentBindings = buildComponentBindings(taskBindings)
  const screenOwnerMap = new Map()
  if (taskBindings) {
    for (const b of taskBindings.screens) {
      if (b.composable) screenOwnerMap.set(b.screenName, b.composable)
      if (!b.implFile) continue
      if (screenImplementationMap.has(b.screenName) && resolve(EXECUTION_ROOT, screenImplementationMap.get(b.screenName)) !== resolve(EXECUTION_ROOT, b.implFile)) {
        issues.push(issue('BLOCKER', 'SCREEN_MAP_CONFLICT', `screen ${b.screenName} has conflicting implementation mappings`, { screen: b.screenName }))
      } else screenImplementationMap.set(b.screenName, b.implFile)
    }
  }
  const unbound = mappingNames.filter((name) => !screenImplementationMap.has(name))
  if (files.length === 1 && unbound.length === 1) screenImplementationMap.set(unbound[0], files[0])
  if (mappingNames.length > 1) {
    for (const screen of mappingNames) if (!screenImplementationMap.has(screen)) {
      issues.push(issue('BLOCKER', 'MULTI_SCREEN_IMPL_MAPPING_REQUIRED', `screen ${screen} requires an explicit screen-to-implementation mapping`, { screen }))
    }
  }
  files = [...new Set([...files, ...screenImplementationMap.values()])]
  // Keep the screen boundary separate from demand-scoped component evidence added below.
  // A single declared --impl-file remains authoritative even when explicitly bound component sources
  // expand the token-evidence set.
  const screenImplementationFiles = files

  // Component-internal sources are admitted only through the task's explicit set↔code
  // binding. The registry and census have their own gates; re-joining here by display name
  // would create a second, weaker identity path.
  let componentSources = new Map()
  try { componentSources = componentEvidenceSources({ specs: resolved.specs, componentBindings }) }
  catch (error) { issues.push(issue('BLOCKER', 'COMPONENT_SOURCE_INVALID', `component binding source is unreadable: ${error.message}`)) }
  files = [...new Set([...files, ...componentSources.keys()])]

  if (!files.length && !roots.length && !appTokensPath) issues.push(issue('BLOCKER', 'NO_IMPLEMENTATION_INPUT', 'pass --impl-file/--impl-root or FIGMA_APP_TOKENS'))
  for (const f of files) {
    const kind = pathKind(f)
    if (kind !== 'file') issues.push(issue('BLOCKER', 'IMPLEMENTATION_FILE_MISSING', `implementation file is not readable: ${f}`, { file: f }))
  }
  for (const r of roots) {
    const kind = pathKind(r)
    if (kind !== 'dir') issues.push(issue('BLOCKER', 'IMPLEMENTATION_ROOT_MISSING', `implementation root is not readable: ${r}`, { file: r }))
  }
  if (appTokensPath && !existsSync(appTokensPath)) {
    issues.push(issue('BLOCKER', 'APP_TOKENS_MISSING', `FIGMA_APP_TOKENS file is not readable: ${appTokensPath}`, { file: appTokensPath }))
  }
  let implementationModel = null
  if (implModelPath && !existsSync(implModelPath)) {
    issues.push(issue(gate ? 'BLOCKER' : 'WARN', 'IMPLEMENTATION_MODEL_MISSING', `implementation model is not readable: ${implModelPath}`, { file: implModelPath }))
  } else if (implModelPath) {
    try {
      implementationModel = JSON.parse(readFileSync(implModelPath, 'utf8'))
      const validateModel = await compileSchema(figmaPath('token-schemas', 'implementation-model.schema.json'), { gate: true })
      const modelSchemaIssues = schemaIssues(validateModel, implementationModel, 'implementationModel:')
      if (modelSchemaIssues.length) {
        for (const schemaIssue of modelSchemaIssues) {
          issues.push(issue(gate ? 'BLOCKER' : 'WARN', 'IMPLEMENTATION_MODEL_SCHEMA_INVALID', `implementation model schema invalid at ${schemaIssue.path}: ${schemaIssue.message}`, { file: displayPath(implModelPath), path: schemaIssue.path }))
        }
        implementationModel = null
      } else {
        for (const problem of modelProblemIssues(implementationModel)) {
          issues.push(issue(gate ? 'BLOCKER' : 'WARN', problem.issueKind, problem.message, { file: problem.file ? displayPath(problem.file) : displayPath(implModelPath) }))
        }
      }
    } catch (e) {
      issues.push(issue(gate ? 'BLOCKER' : 'WARN', 'IMPLEMENTATION_MODEL_INVALID', `implementation model JSON is invalid: ${e.message}`, { file: displayPath(implModelPath) }))
      implementationModel = null
    }
  }

  let evidence = { tokens: {}, raw: {}, files: [] }
  try {
    evidence = loadEvidence({ files, roots, appTokensPath })
  } catch (e) {
    issues.push(issue('BLOCKER', 'APP_TOKENS_UNREADABLE', `FIGMA_APP_TOKENS evidence is unreadable: ${e.message}`, { file: appTokensPath || null }))
  }
  for (const f of files) inputHashes[f] = fileHash(f)
  for (const f of evidence.files || []) inputHashes[f] = fileHash(f)
  if (appTokensPath) inputHashes[appTokensPath] = fileHash(appTokensPath)
  if (implModelPath) inputHashes[implModelPath] = fileHash(implModelPath)
  if ((files.length || roots.length || appTokensPath) && evidenceCount(evidence) === 0) {
    issues.push(issue('BLOCKER', 'NO_IMPLEMENTATION_EVIDENCE', 'implementation inputs produced no AppTokens/raw visual evidence'))
  }
		  const comparisons = []
  // Classify each screen's implementation as a code-fact (Canvas/DrawScope) for the report's
  // widgetClasses. Purely informational: the spec gate treats canvas and declarative widgets
  // IDENTICALLY (both reconciled by evidence alias resolution) — the class is a label, not a gate.
  const widgetClasses = {}
  const widgetClassCache = new Map()
  const canvasScreen = (screen) => {
    const file = screenImplementationMap.get(screen) || (screenImplementationFiles.length === 1 ? screenImplementationFiles[0] : null)
    if (!file) return false
    if (!widgetClassCache.has(file)) {
      let source = ''
      try { source = readFileSync(file, 'utf8') } catch {}
      widgetClassCache.set(file, classifyWidgetSource(source).canvas)
    }
    return widgetClassCache.get(file)
  }
    for (const spec of resolved.specs) {
      if (canvasScreen(spec.screen)) widgetClasses[spec.screen] = 'canvas'
      // The screen's declared implementation file (screen map/binding, or the sole
      // --impl-file) is the authoritative screen boundary for component owner-binding —
      // same derivation the canvas classifier uses.
      const implFile = screenImplementationMap.get(spec.screen) || (screenImplementationFiles.length === 1 ? screenImplementationFiles[0] : null)
      for (const element of spec.elements || []) {
        const before = issues.length
        const res = compareElement({ spec, element, evidence, implementationModel, gate, implFile, screenOwner: screenOwnerMap.get(spec.screen) || spec.screen, componentBindings })
        issues.push(...res.issues)
        comparisons.push({
          screen: spec.screen,
          theme: spec.theme,
          file: spec.file,
          stableId: element.identity,
          elementName: element.name,
          status: issues.length === before ? 'PASS' : 'REVIEW',
          binding: res.binding,
        })
      }
    }

  const hasBlocker = issues.some((i) => i.severity === 'BLOCKER' || i.severity === 'ERROR')
  const hasWarn = issues.some((i) => i.severity === 'WARN' || i.severity === 'WARNING')
  const overall = hasBlocker ? 'BLOCKER' : hasWarn ? 'WARN' : 'PASS'
  const { reportPath } = writeReport({
    name: 'spec-compare',
    taskStem: stem,
    mode: runMode,
    inputs: {
      screensRoot: displayPath(screensRoot),
	      implementationFiles: files.map(displayPath),
	      implementationRoots: roots.map(displayPath),
	      screenMap: Object.fromEntries([...screenImplementationMap.entries()].map(([screen, file]) => [screen, displayPath(file)])),
	      implementationModel: implModelPath ? displayPath(implModelPath) : null,
	      componentEvidenceSources: Object.fromEntries([...componentSources.entries()].map(([abs, via]) => [displayPath(abs), via])),
      appTokens: appTokensPath ? displayPath(appTokensPath) : null,
    },
    inputHashes,
    overall,
    issues,
    extra: {
      engineVersion: 'spec-compare-v1',
	      unresolvedRefs: implementationModel && Array.isArray(implementationModel.unresolvedRefs) ? implementationModel.unresolvedRefs : [],
      implementationModel: implModelPath ? {
        path: displayPath(implModelPath),
        hash: fileHash(implModelPath),
        schemaVersion: implementationModel && implementationModel.schemaVersion,
      } : null,
      comparisons,
      widgetClasses,
	      implementation: {
	        files: evidence.files || files,
	        screenMap: Object.fromEntries([...screenImplementationMap.entries()].map(([screen, file]) => [screen, displayPath(file)])),
	        tokenCount: Object.keys(evidence.tokens || {}).length,
	        tokenValueCount: tokenValueEvidenceCount(evidence),
        rawColorCount: Object.keys((evidence.raw && evidence.raw.colors) || {}).length,
        rawDpCount: Object.keys((evidence.raw && evidence.raw.dp) || {}).length,
      },
    },
  })

  console.log(`compare-screen-spec: ${stem} ${overall}`)
  for (const i of issues) console.log(`  [${i.severity}] ${i.issueKind}${i.elementName ? ` ${i.elementName}` : ''}: ${i.message}`)
  console.log(`Report: ${reportPath}`)
  process.exit(gate && hasBlocker ? 1 : 0)
}

main().catch((e) => {
  console.error(`FATAL: ${e.message}`)
  process.exit(1)
})
