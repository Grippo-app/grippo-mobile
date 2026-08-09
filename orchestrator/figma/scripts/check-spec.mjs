// check-spec.mjs — structural validator for spec.json files written by figma:screens.
//
// Usage:
//   node scripts/check-spec.mjs <stem> [--gate|--advisory]
//
// Env overrides:
//   FIGMA_SCREEN_CACHE_ROOT or FIGMA_SPEC_SCREENS_DIR — override screens cache root (must match if both are set)
//   FIGMA_SPEC_SCHEMA      — override schema file path (default: token-schemas/spec.schema.json)

import { exists, readJson, figmaPath, figmaScreensRoot, parseCli } from './_util.mjs'
import { assertTaskStem, compileSchema, fileHash, schemaIssues, writeReport } from './report-utils.mjs'
import { readdirSync } from 'node:fs'
import { join } from 'node:path'

const HEX_RE = /^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$/
const TOKEN_RE = /^\{[a-zA-Z0-9_-]+(\.[a-zA-Z0-9._-]+)+\}$/
// Two placeholder arms with different precision. The angle-tag arm blocks on ANY string
// (catches agent-copied template leftovers like '<node id when available>'). The word arm
// matches a leading placeholder TOKEN only when it is USED AS A MARKER — the token is the
// whole trimmed string, or is immediately followed by a marker punctuator (a colon, or a
// whitespace-flanked dash/em-dash: 'TODO: replace with final copy', 'TBD — ask designer').
// A bare \b prefix would false-block 'todo-list-item' stableIds, 'Todo card'
// layers, and 'Unknown filter' variant values. Full-string equality would miss
// multi-word placeholders such as 'TODO: replace with final copy'.
const PLACEHOLDER_TAG_RE = /<[A-Za-z][A-Za-z0-9_.:-]*>/
const PLACEHOLDER_WORD_RE = /^(TODO|PLACEHOLDER|UNKNOWN|TBD|FIXME|XXX)(\s*[:：]|\s+[—–-]\s|$)/i
const PLACEHOLDER_VALUE_ALLOW = new Set(['UNCLASSIFIED', 'unclassified', 'unresolved'])
const USAGE = 'usage: node scripts/check-spec.mjs <stem> [--gate|--advisory]'

function validateValue(v) {
  if (typeof v === 'string') return HEX_RE.test(v) || TOKEN_RE.test(v)
  if (v && typeof v === 'object') {
    if (typeof v.resolvedValue === 'string') return validateValue(v.resolvedValue)
    if (typeof v.value === 'string') return validateValue(v.value)
  }
  return false
}

function collectStrings(value, path, out) {
  if (typeof value === 'string') { out.push({ path, value }); return }
  if (Array.isArray(value)) { value.forEach((v, i) => collectStrings(v, `${path}[${i}]`, out)); return }
  if (value && typeof value === 'object') {
    for (const k of Object.keys(value)) collectStrings(value[k], path ? `${path}.${k}` : k, out)
  }
}

function issue(severity, issueKind, message, extra = {}) {
  return Object.assign({ severity, issueKind, message }, extra)
}

function elementId(el, i) {
  return (el && (el.stableId || el.figmaNodeId)) || `elements[${i}]`
}

function addCount(map, key, value) {
  if (!value) return
  if (!map.has(value)) map.set(value, [])
  map.get(value).push(key)
}

function stableJson(value) {
  if (Array.isArray(value)) return `[${value.map(stableJson).join(',')}]`
  if (value && typeof value === 'object') {
    return `{${Object.keys(value).sort().map((k) => `${JSON.stringify(k)}:${stableJson(value[k])}`).join(',')}}`
  }
  return JSON.stringify(value)
}

function isPlaceholderAllowed(_path, value) {
  return PLACEHOLDER_VALUE_ALLOW.has(value)
}

function validateV2Projection(spec, filename) {
  const issues = []
  const nodes = Array.isArray(spec.nodes) ? spec.nodes : []
  const elements = Array.isArray(spec.elements) ? spec.elements : []
  const nodeStableIds = new Map()
  const nodeFigmaIds = new Map()
  const nodeByStableId = new Map()
  const nodeByFigmaId = new Map()
  for (let i = 0; i < nodes.length; i++) {
    const node = nodes[i] || {}
    addCount(nodeStableIds, `nodes[${i}]`, node.stableId)
    addCount(nodeFigmaIds, `nodes[${i}]`, node.figmaNodeId)
    if (node.stableId && !nodeByStableId.has(node.stableId)) nodeByStableId.set(node.stableId, node)
    if (node.figmaNodeId && !nodeByFigmaId.has(node.figmaNodeId)) nodeByFigmaId.set(node.figmaNodeId, node)
  }
  for (const [stableId, paths] of nodeStableIds) {
    if (paths.length > 1) issues.push(issue('BLOCKER', 'DUPLICATE_NODE_STABLE_ID', `node stableId '${stableId}' appears ${paths.length} times`, { file: filename, path: paths.join(',') }))
  }
  for (const [figmaNodeId, paths] of nodeFigmaIds) {
    if (paths.length > 1) issues.push(issue('BLOCKER', 'DUPLICATE_NODE_FIGMA_ID', `node figmaNodeId '${figmaNodeId}' appears ${paths.length} times`, { file: filename, path: paths.join(',') }))
  }
  if (spec.rootNodeId && !nodeByStableId.has(spec.rootNodeId) && !nodeByFigmaId.has(spec.rootNodeId)) {
    issues.push(issue('BLOCKER', 'ROOT_NODE_MISSING', `rootNodeId '${spec.rootNodeId}' is not present in nodes[]`, { file: filename, path: 'rootNodeId' }))
  }
  if (!elements.length) {
    issues.push(issue('BLOCKER', 'V2_PROJECTION_MISSING', 'schemaVersion 2 spec must include a non-empty comparable elements[] projection', { file: filename, path: 'elements' }))
  }
  const elementStableIds = new Map()
  const elementFigmaIds = new Map()
  const elementResolvedIds = new Map()
  for (let i = 0; i < elements.length; i++) {
    addCount(elementStableIds, `elements[${i}]`, elements[i] && elements[i].stableId)
    addCount(elementFigmaIds, `elements[${i}]`, elements[i] && elements[i].figmaNodeId)
    addCount(elementResolvedIds, `elements[${i}]`, elements[i] && (elements[i].stableId || elements[i].figmaNodeId))
  }
  for (const [stableId, paths] of elementStableIds) {
    if (paths.length > 1) issues.push(issue('BLOCKER', 'DUPLICATE_ELEMENT_STABLE_ID', `element stable identity '${stableId}' appears ${paths.length} times`, { file: filename, path: paths.join(',') }))
    if (stableId && !nodeByStableId.has(stableId) && nodes.length) {
      issues.push(issue('BLOCKER', 'V2_PROJECTION_STABLE_ID_MISMATCH', `projected element stableId '${stableId}' is not present as a node stableId`, { file: filename, path: paths[0] }))
    }
  }
  for (const [figmaNodeId, paths] of elementFigmaIds) {
    if (paths.length > 1) issues.push(issue('BLOCKER', 'DUPLICATE_ELEMENT_FIGMA_ID', `element figmaNodeId '${figmaNodeId}' appears ${paths.length} times`, { file: filename, path: paths.join(',') }))
    if (figmaNodeId && !nodeByFigmaId.has(figmaNodeId) && nodes.length) {
      issues.push(issue('BLOCKER', 'V2_PROJECTION_FIGMA_ID_MISMATCH', `projected element figmaNodeId '${figmaNodeId}' is not present as a node figmaNodeId`, { file: filename, path: paths[0] }))
    }
  }
  for (const [id, paths] of elementResolvedIds) {
    if (paths.length > 1) issues.push(issue('BLOCKER', 'DUPLICATE_ELEMENT_COMPARISON_ID', `element comparison identity '${id}' appears ${paths.length} times`, { file: filename, path: paths.join(',') }))
  }
  const comparableFields = ['name', 'bboxDp', 'fills', 'textStyle', 'text', 'cornerRadiusDp', 'strokes', 'paddingDp', 'gapsToSiblingsDp', 'componentSetName', 'componentSetNodeId', 'variantProps']
  // 'text' is compared for element↔node projection consistency (so a v2 elements[] that drops a
  // node's `text` trips V2_PROJECTION_FIELD_MISMATCH), but excluded from comparableDataFields so
  // a text-only node is not itself FORCED to be projected.
  const comparableDataFields = comparableFields.filter((field) => !['name', 'bboxDp', 'text'].includes(field))
  const projectedStableIds = new Set()
  const projectedFigmaIds = new Set()
  for (let i = 0; i < elements.length; i++) {
    const el = elements[i] || {}
    if (el.stableId) projectedStableIds.add(el.stableId)
    if (el.figmaNodeId) projectedFigmaIds.add(el.figmaNodeId)
    const stableNode = el.stableId ? nodeByStableId.get(el.stableId) : null
    const figmaNode = el.figmaNodeId ? nodeByFigmaId.get(el.figmaNodeId) : null
    if (stableNode && figmaNode && stableNode !== figmaNode) {
      issues.push(issue('BLOCKER', 'V2_PROJECTION_ID_CONFLICT', 'projected element stableId and figmaNodeId point at different nodes', { file: filename, path: `elements[${i}]`, stableId: el.stableId || el.figmaNodeId, elementName: el.name }))
    }
    const node = stableNode || figmaNode || null
    if (!node) continue
    for (const field of comparableFields) {
      // A node field that is explicitly null (cornerRadiusDp/textStyle/paddingDp are
      // oneOf:[null,...] in the schema) is "no comparable value" — the downstream
      // comparator (compare-screen-spec.mjs) treats null and absent identically — so an
      // element omitting it is NOT a projection gap. Real value-vs-null drift still trips
      // the mismatch check below.
      const nodeVal = node[field] == null ? undefined : node[field]
      if (nodeVal !== undefined && el[field] === undefined) {
        issues.push(issue('BLOCKER', 'V2_PROJECTION_FIELD_MISSING', `projected element is missing node field '${field}'`, { file: filename, path: `elements[${i}].${field}`, stableId: el.stableId || el.figmaNodeId, elementName: el.name }))
        continue
      }
      if (el[field] === undefined || node[field] === undefined) continue
      if (stableJson(el[field]) !== stableJson(node[field])) {
        issues.push(issue('BLOCKER', 'V2_PROJECTION_FIELD_MISMATCH', `projected element field '${field}' differs from matching node`, { file: filename, path: `elements[${i}].${field}`, stableId: el.stableId || el.figmaNodeId, elementName: el.name }))
      }
    }
  }
  for (let i = 0; i < nodes.length; i++) {
    const node = nodes[i] || {}
    // null == absent here too, mirroring the per-element FIELD_MISSING check above: an explicit
    // null on a nullable field (cornerRadiusDp/textStyle/paddingDp) is "no comparable value".
    const hasComparableVisual = comparableDataFields.some((field) => node[field] != null)
    const hasComparableData = node.bboxDp != null || hasComparableVisual
    const isRoot = spec.rootNodeId && (node.stableId === spec.rootNodeId || node.figmaNodeId === spec.rootNodeId)
    if (isRoot && !hasComparableVisual) continue
    if (!hasComparableData) continue
    const projected = (node.stableId && projectedStableIds.has(node.stableId)) || (node.figmaNodeId && projectedFigmaIds.has(node.figmaNodeId))
    if (!projected) {
      issues.push(issue('BLOCKER', 'V2_PROJECTION_NODE_MISSING', 'v2 node with comparable data is missing from elements[] projection', { file: filename, path: `nodes[${i}]`, stableId: node.stableId || node.figmaNodeId, elementName: node.name }))
    }
  }
  return issues
}

function expectedFromFilename(filename) {
  const dark = filename.endsWith('.dark.spec.json')
  return {
    screen: filename.replace(/\.dark\.spec\.json$|\.spec\.json$/g, ''),
    // A non-.dark file is theme-AGNOSTIC (a plain-URL frame may be dark or light per the pull
    // contract); only .dark.spec.json pins 'dark'. Kept in sync with check-screen-cache.mjs.
    theme: dark ? 'dark' : 'primary',
  }
}

async function main() {
  let cli
  try {
    cli = parseCli({ allowedFlags: ['--stem', '--gate', '--advisory'], valueFlags: ['--stem'], booleanFlags: ['--gate', '--advisory'], usage: USAGE })
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
  catch { console.log(USAGE); process.exit(1) }
  const runMode = cli.has('--gate') ? 'gate' : 'advisory'
  const gate = runMode === 'gate'
  const screensRoot = figmaScreensRoot()
  const screensDir = join(screensRoot, stem)
  const schemaPath = process.env.FIGMA_SPEC_SCHEMA || figmaPath('token-schemas', 'spec.schema.json')

  const allIssues = []
  const files = []
  const inputHashes = {}

  if (!exists(screensDir)) {
    allIssues.push(issue('BLOCKER', 'SCREENS_DIR_MISSING', `stem '${stem}' not found in screens cache`, { file: screensDir }))
    const { reportPath } = writeReport({ name: 'check-spec', taskStem: stem, mode: runMode, inputs: { screensDir }, inputHashes, overall: 'BLOCKER', issues: allIssues, extra: { files } })
    console.log(`check-spec: ${stem} BLOCKER\nReport: ${reportPath}`)
    process.exit(gate ? 1 : 0)
  }

  let specFiles = []
  try {
    specFiles = readdirSync(screensDir).filter((f) => f.endsWith('.spec.json')).sort()
  } catch (e) {
    allIssues.push(issue('BLOCKER', 'SCREENS_DIR_UNREADABLE', e.message, { file: screensDir }))
  }
  if (!specFiles.length) allIssues.push(issue('BLOCKER', 'NO_SPEC_FILES', `no spec files found for stem '${stem}'`))

  let validate = null
  try {
    validate = await compileSchema(schemaPath, { gate })
  } catch (e) {
    allIssues.push(issue('BLOCKER', 'AJV_UNAVAILABLE', `schema validation unavailable in gate mode: ${e.message}`, { file: schemaPath }))
  }
  if (!validate && !gate) allIssues.push(issue('WARN', 'AJV_UNAVAILABLE', 'schema validation skipped in advisory mode', { file: schemaPath }))

  for (const filename of specFiles) {
    const filePath = join(screensDir, filename)
    const fileIssues = []
    inputHashes[filePath] = fileHash(filePath)
    let spec = null
    try {
      spec = readJson(filePath)
    } catch (e) {
      fileIssues.push(issue('BLOCKER', 'UNREADABLE', `unreadable or invalid JSON: ${e.message}`, { file: filename }))
      files.push({ file: filename, status: 'BLOCKER' })
      allIssues.push(...fileIssues)
      continue
    }

    fileIssues.push(...schemaIssues(validate, spec, filename).map((i) => issue('BLOCKER', i.issueKind, i.message, { file: filename, path: i.path })))
    const expected = expectedFromFilename(filename)
    if (spec.screen !== expected.screen) {
      fileIssues.push(issue('BLOCKER', 'SPEC_SCREEN_MISMATCH', `spec screen "${spec.screen}" does not match filename screen "${expected.screen}"`, { file: filename, screen: spec.screen, theme: spec.theme }))
    }
	    const themeOk = expected.theme === 'dark'
	      ? spec.theme === 'dark'
	      : (spec.theme === 'light' || spec.theme === 'dark')   // primary (non-.dark file) accepts either
	    if (!themeOk) {
	      fileIssues.push(issue('BLOCKER', 'SPEC_THEME_MISMATCH', `spec theme "${spec.theme}" does not match filename theme "${expected.theme === 'dark' ? 'dark' : 'light or dark'}"`, { file: filename, screen: spec.screen, theme: spec.theme }))
	    }
	    if (Array.isArray(spec.elements) && spec.elements.length === 0) {
	      fileIssues.push(issue('BLOCKER', 'NO_COMPARABLE_ELEMENTS', 'spec must include at least one comparable element', { file: filename, screen: spec.screen, theme: spec.theme, path: 'elements' }))
	    }
	    if (spec.schemaVersion === 2 && spec.themeMetadata && typeof spec.themeMetadata.themeKey === 'string') {
	      const themeKey = spec.themeMetadata.themeKey.toLowerCase()
	      if ((themeKey === 'light' || themeKey === 'dark') && themeKey !== spec.theme) {
	        fileIssues.push(issue('BLOCKER', 'THEME_METADATA_MISMATCH', `themeMetadata.themeKey "${spec.themeMetadata.themeKey}" contradicts spec theme "${spec.theme}"`, { file: filename, screen: spec.screen, theme: spec.theme, path: 'themeMetadata.themeKey' }))
	      }
	    }
	    fileIssues.push(...validateV2Projection(spec, filename))

    const names = new Map()
	    const stableIds = new Map()
	    const figmaNodeIds = new Map()
    for (let i = 0; i < (spec.elements || []).length; i++) {
      const el = spec.elements[i] || {}
      const id = elementId(el, i)
      const base = { file: filename, screen: spec.screen, theme: spec.theme, stableId: id, elementName: el && el.name }
      if (!el.stableId && !el.figmaNodeId) {
        fileIssues.push(issue(gate ? 'BLOCKER' : 'WARN', 'MISSING_STABLE_ID', 'element lacks stableId/figmaNodeId', Object.assign({ path: `elements[${i}]` }, base)))
      }
      if (el && el.name) names.set(el.name, (names.get(el.name) || 0) + 1)
	      if (el && el.stableId) addCount(stableIds, `elements[${i}]`, el.stableId)
	      if (el && el.figmaNodeId) addCount(figmaNodeIds, `elements[${i}]`, el.figmaNodeId)
      for (let j = 0; j < ((el && el.fills) || []).length; j++) {
        const fill = el.fills[j]
        if (!validateValue(fill)) fileIssues.push(issue('BLOCKER', 'INVALID_FILL', `invalid fill ${JSON.stringify(fill)}`, Object.assign({ path: `elements[${i}].fills[${j}]` }, base)))
      }
      for (let j = 0; j < ((el && el.strokes) || []).length; j++) {
        const st = el.strokes[j]
        if (st && !validateValue(st.color)) fileIssues.push(issue('BLOCKER', 'INVALID_STROKE', `invalid stroke color ${JSON.stringify(st.color)}`, Object.assign({ path: `elements[${i}].strokes[${j}].color` }, base)))
        if (st && typeof st.widthDp === 'number' && st.widthDp < 0) fileIssues.push(issue('BLOCKER', 'NEGATIVE_STROKE', 'negative stroke width', Object.assign({ path: `elements[${i}].strokes[${j}].widthDp` }, base)))
      }
      if (el && el.gapsToSiblingsDp && typeof el.gapsToSiblingsDp === 'object') {
        for (const [sib, gap] of Object.entries(el.gapsToSiblingsDp)) {
          if (typeof gap === 'number' && gap < 0) fileIssues.push(issue('BLOCKER', 'NEGATIVE_GAP', 'negative sibling gap', Object.assign({ path: `elements[${i}].gapsToSiblingsDp.${sib}` }, base)))
        }
      }
    }
    for (const [name, count] of names) {
      if (count > 1) fileIssues.push(issue('WARN', 'DUPLICATE_ELEMENT_NAME', `display name '${name}' appears ${count} times; stable identity is required for comparison`, { file: filename, screen: spec.screen, theme: spec.theme, elementName: name }))
    }
	    for (const [id, paths] of stableIds) {
	      if (paths.length > 1) fileIssues.push(issue('BLOCKER', 'DUPLICATE_ELEMENT_STABLE_ID', `stable identity '${id}' appears ${paths.length} times`, { file: filename, screen: spec.screen, theme: spec.theme, stableId: id, path: paths.join(',') }))
	    }
	    for (const [id, paths] of figmaNodeIds) {
	      if (paths.length > 1) fileIssues.push(issue('BLOCKER', 'DUPLICATE_ELEMENT_FIGMA_ID', `figmaNodeId '${id}' appears ${paths.length} times`, { file: filename, screen: spec.screen, theme: spec.theme, stableId: id, path: paths.join(',') }))
	    }

    const strings = []
    collectStrings(spec, '', strings)
    for (const { path, value } of strings) {
      // `.text` carries the design's VISIBLE copy verbatim — real UI copy may legitimately
      // contain angle-bracket notation ("Enter <code>"), so the angle-tag arm is exempt
      // there; the WORD arm still applies (a leading TODO:/PLACEHOLDER: in visible copy is
      // a designer leftover worth blocking).
      const isPlaceholder = (PLACEHOLDER_TAG_RE.test(value) && !path.endsWith('.text')) || PLACEHOLDER_WORD_RE.test(value.trim())
      if (isPlaceholder && !isPlaceholderAllowed(path, value)) fileIssues.push(issue('BLOCKER', 'PLACEHOLDER', `placeholder string "${value}"`, { file: filename, screen: spec.screen, theme: spec.theme, path }))
    }

    const status = fileIssues.some((i) => i.severity === 'BLOCKER') ? 'BLOCKER' : fileIssues.length ? 'WARN' : 'PASS'
    files.push({ file: filename, status, issueCount: fileIssues.length })
    allIssues.push(...fileIssues)
  }

  const hasBlocker = allIssues.some((i) => i.severity === 'BLOCKER' || i.severity === 'ERROR')
  const hasWarn = allIssues.some((i) => i.severity === 'WARN' || i.severity === 'WARNING')
  const overall = hasBlocker ? 'BLOCKER' : hasWarn ? 'WARN' : 'PASS'
  const { reportPath } = writeReport({
    name: 'check-spec',
    taskStem: stem,
    mode: runMode,
    inputs: { screensDir, schemaPath },
    inputHashes,
    overall,
    issues: allIssues,
    extra: { files },
  })

  console.log(`\ncheck-spec: ${stem} ${overall}`)
  for (const f of files) console.log(`  ${f.file}: ${f.status}${f.issueCount ? ` (${f.issueCount})` : ''}`)
  for (const i of allIssues) console.log(`  [${i.severity}] ${i.issueKind}${i.file ? ` ${i.file}` : ''}: ${i.message}`)
  console.log(`Report: ${reportPath}`)
  process.exit(gate && hasBlocker ? 1 : 0)
}

main().catch((e) => {
  console.error(`FATAL: ${e.message}`)
  process.exit(1)
})
