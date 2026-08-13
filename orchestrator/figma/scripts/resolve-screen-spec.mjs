// resolve-screen-spec.mjs — normalizes figma:screens *.spec.json into deterministic comparison input.
//
// Usage:
//   node scripts/resolve-screen-spec.mjs <stem> [--out <path>]
//
// Env:
//   FIGMA_SCREEN_CACHE_ROOT or FIGMA_SPEC_SCREENS_DIR — override screens cache root (must match if both are set)
//   FIGMA_RESOLVED_SPEC_OUT — output path
//   Token truth is resolved only from the current observed catalog and its
//   exact effective binding snapshot.

import { existsSync, readdirSync, readFileSync } from 'node:fs'
import { join } from 'node:path'
import Ajv from 'ajv'
import { figmaPath, figmaScreensRoot, isDirectRun, executionFigmaOutputPath, writeFigmaRuntimeFile } from './_util.mjs'
import { assertTaskStem, fileHash } from './report-utils.mjs'
import { loadObservedTokenDomain, loadPublishedBindingSnapshot } from './lib/observed-token-domain.mjs'
import { contextKey } from '../tokens/source-contract.mjs'

const currentSpecSchema = JSON.parse(readFileSync(figmaPath('token-schemas', 'spec.schema.json'), 'utf8'))
const validateCurrentSpec = new Ajv({ allErrors: true, allowUnionTypes: true }).compile(currentSpecSchema)

function argStem() {
  const idx = process.argv.indexOf('--stem')
  if (idx >= 0 && process.argv[idx + 1]) return process.argv[idx + 1]
  return process.argv.find((a, i) => i > 1 && !a.startsWith('--')) || ''
}

function valueAfter(flag) {
  const idx = process.argv.indexOf(flag)
  return idx >= 0 ? process.argv[idx + 1] : ''
}

function readJson(path) {
  return JSON.parse(readFileSync(path, 'utf8'))
}

function tokenInner(ref) {
  const m = /^\{(.+)\}$/.exec(String(ref || '').trim())
  return m ? m[1] : ''
}

function normalizeTokenKey(raw) {
  return String(raw || '').trim().replace(/^\{|\}$/g, '').replace(/^AppTokens\./, '').replace(/[\/\s-]+/g, '.')
}

function normalizeAppTokenPath(raw, kind = 'colors') {
  const s = normalizeTokenKey(raw)
  if (!s) return null
  if (/^(colors|dp|typography)\./.test(s)) return s
  return `${kind}.${s}`
}

function loadTokenMappings() {
  const byProviderName = new Map()
  const fixtures = {
    fixtureCatalogFile: process.env.FIGMA_OBSERVED_TOKEN_CATALOG,
    fixtureSourceIndexFile: process.env.FIGMA_OBSERVED_TOKEN_SOURCE_INDEX,
    fixtureBindingSnapshotFile: process.env.FIGMA_TOKEN_BINDING_SNAPSHOT
  }
  const domain = loadObservedTokenDomain(fixtures)
  const publishedBindings = loadPublishedBindingSnapshot(fixtures)
  if (!domain.present || !publishedBindings.present) return byProviderName
  if (publishedBindings.snapshot.observedCatalogHash !== domain.catalog.semanticHash) {
    throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED: binding snapshot does not pin the current observed catalog')
  }
  const bindings = new Map(publishedBindings.snapshot.bindings.map((binding) => [
    binding.observedTokenKey + '\u0000' + binding.contextKey, binding
  ]))
  for (const token of domain.catalog.tokens) {
    if (token.presenceStatus !== 'active') continue
    const rows = []
    for (const coordinate of token.coordinates) {
      if (coordinate.status !== 'consistent' || coordinate.values.length !== 1) continue
      const key = contextKey(coordinate.context)
      const binding = bindings.get(token.observedTokenKey + '\u0000' + key)
      if (!binding || binding.targetState !== 'present') continue
      rows.push({
        context: coordinate.context,
        appToken: normalizeAppTokenPath(binding.projectSemanticPath.join('.'),
          coordinate.values[0].kind === 'color' ? 'colors' : 'dp')
      })
    }
    if (rows.length) byProviderName.set(token.providerName, rows)
  }
  return byProviderName
}

function appTokenFor(ref, theme, tokenMap = new Map()) {
  const inner = tokenInner(ref)
  if (!inner) return null
  const candidates = tokenMap.get(inner) || []
  const themed = candidates.filter((row) => row.context.theme === theme)
  const selected = themed.length ? themed : candidates
  const values = [...new Set(selected.map((row) => row.appToken).filter(Boolean))]
  return values.length === 1 ? values[0] : null
}

function normalizePaint(value, tokenMap, theme) {
  if (typeof value === 'string') {
    if (/^\{.+\}$/.test(value)) return { raw: value, tokenRef: value, appToken: appTokenFor(value, theme, tokenMap) }
    return { raw: value, resolvedValue: String(value).toUpperCase() }
  }
  if (value && typeof value === 'object') {
    const out = { raw: value }
    if (value.tokenRef) { out.tokenRef = value.tokenRef; out.appToken = appTokenFor(value.tokenRef, theme, tokenMap) }
    if (value.resolvedValue) out.resolvedValue = String(value.resolvedValue).toUpperCase()
    return out
  }
  return { raw: value }
}

function elementIdentity(el) {
  return el.stableId || el.figmaNodeId
}

function projectedElements(spec) {
  return spec.elements
}

function normalizeSpec(spec, file, tokenMap = new Map()) {
  const elements = projectedElements(spec)
  return {
    file,
    schemaVersion: 2,
    sourceElementCount: Array.isArray(spec.elements) ? spec.elements.length : 0,
    screen: spec.screen,
    theme: spec.theme,
    frameSizeDp: spec.frameSizeDp,
    elements: elements.map((el) => ({
      identity: elementIdentity(el),
      stableId: el.stableId || null,
      figmaNodeId: el.figmaNodeId || null,
      name: el.name,
      bboxDp: el.bboxDp,
      fills: (el.fills || []).map((v) => normalizePaint(v, tokenMap, spec.theme)),
      strokes: (el.strokes || []).map((stroke) => ({ color: normalizePaint(stroke.color, tokenMap, spec.theme), widthDp: stroke.widthDp })),
      text: el.text ?? null,                 // carry visible text through normalization (content-parity contract; matches projectedElements)
      textStyle: el.textStyle || null,
	      cornerRadiusDp: el.cornerRadiusDp ?? null,
	      paddingDp: el.paddingDp || null,
	      gapsToSiblingsDp: el.gapsToSiblingsDp || {},
	      componentSetName: el.componentSetName || null,
	      componentSetNodeId: el.componentSetNodeId || null,
	      variantProps: el.variantProps || null,
	    })),
	  }
	}

export function loadResolvedSpecs({ stem, screensRoot = figmaScreensRoot() }) {
  stem = assertTaskStem(stem)
  const screensDir = join(screensRoot, stem)
  // Collect read/parse failures instead of throwing: a corrupt spec.json (or a screensDir
  // that exists but is a file / unreadable) must surface as a structured BLOCKER in the
  // caller's report, not crash compare-screen-spec with an opaque FATAL and no artifact.
  const unreadable = []
  let files = []
  if (existsSync(screensDir)) {
    try {
      files = readdirSync(screensDir).filter((f) => f.endsWith('.spec.json')).sort()
    } catch (e) {
      unreadable.push({ file: screensDir, message: e.message, kind: 'SCREENS_DIR_UNREADABLE' })
    }
  }
  let tokenMap = new Map()
  try { tokenMap = loadTokenMappings() }
  catch (error) { unreadable.push({ file: null, message: error.message, kind: 'TOKEN_MAPPING_UNREADABLE' }) }
  const inputHashes = Object.fromEntries(files.map((f) => [join(screensDir, f), fileHash(join(screensDir, f))]))
  const specs = []
  for (const f of files) {
    let raw
    try {
      raw = readJson(join(screensDir, f))
    } catch (e) {
      unreadable.push({ file: f, message: e.message, kind: 'SPEC_UNREADABLE' })
      continue
    }
    // A parseable-but-non-object body (literal null, an array, a scalar) would otherwise
    // throw later inside normalizeSpec (spec.elements on null) — outside this guard — and
    // FATAL with no report. Treat it as unreadable so it surfaces as a structured BLOCKER.
    if (!raw || typeof raw !== 'object' || Array.isArray(raw)) {
      unreadable.push({ file: f, message: 'spec body is not a JSON object', kind: 'SPEC_UNREADABLE' })
      continue
    }
    if (!validateCurrentSpec(raw)) {
      const detail = (validateCurrentSpec.errors || []).slice(0, 8).map((error) => `${error.instancePath || '/'} ${error.message}`).join('; ')
      unreadable.push({ file: f, message: `spec violates the current schemaVersion 2 contract (${detail})`, kind: 'SPEC_INVALID' })
      continue
    }
    specs.push(normalizeSpec(raw, f, tokenMap))
  }
  return {
    schemaVersion: 1,
    taskStem: stem,
    screensDir,
    inputHashes,
    specs,
    unreadable,
  }
}

function atomicWrite(path, data) {
  writeFigmaRuntimeFile(path, JSON.stringify(data, null, 2) + '\n')
}

if (isDirectRun(import.meta.url)) {
  let stem
  try { stem = assertTaskStem(argStem()) } catch {
    console.error('usage: node scripts/resolve-screen-spec.mjs <stem> [--out <path>]')
    process.exit(1)
  }
  const screensRoot = figmaScreensRoot()
  const out = executionFigmaOutputPath(
    valueAfter('--out') || process.env.FIGMA_RESOLVED_SPEC_OUT || figmaPath('reports', `resolved-spec-${stem}.json`))
  const resolved = loadResolvedSpecs({ stem, screensRoot })
  atomicWrite(out, resolved)
  console.log(`resolve-screen-spec: ${stem} ${resolved.specs.length} spec file(s) -> ${out}`)
  process.exit(resolved.unreadable.length ? 1 : 0)
}
