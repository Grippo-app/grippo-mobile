// Deterministic validator for the per-task figma:screens cache.
// Never calls Figma; it compares current task ## Design bullets against local
// .cache/figma/screens/<stem>/ artifacts and writes a JSON report.
//
// Usage:
//   node scripts/check-screen-cache.mjs <stem> [--gate|--advisory]
//
// Env:
//   FIGMA_SCREEN_TASK_FILE  — explicit task markdown path, or "-" to read the proposed task from stdin
//   FIGMA_SCREEN_CACHE_ROOT or FIGMA_SPEC_SCREENS_DIR — override screens cache root (must match if both are set)

import { createRequire } from 'node:module'
import { closeSync, existsSync, openSync, readSync, readdirSync } from 'node:fs'
import { join } from 'node:path'
import { displayPath, exists, readJson, figmaPath, figmaScreensRoot, parseCli, PROJECT_ROOT } from './_util.mjs'
import { assertTaskStem, compileSchema, fileHash, schemaIssues, writeReport } from './report-utils.mjs'
import { chromeResidue } from './lib/oracle-chrome.mjs'
import { readContainedSingleLinkFile } from '../runtime/file-safety.mjs'
import { bytesHash } from '../runtime/canonical-json.mjs'
import { captureSemanticError } from '../tokens/source-contract.mjs'
import { readTaskMarkdown } from './lib/task-markdown.mjs'

// PNG pixel size straight from the IHDR header (no image dependency in this gate).
function pngSize(path) {
  try {
    const buf = Buffer.alloc(24)
    const fd = openSync(path, 'r')
    try { if (readSync(fd, buf, 0, 24, 0) < 24) return null } finally { closeSync(fd) }
    if (!buf.subarray(0, 8).equals(Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]))) return null
    if (buf.readUInt32BE(12) !== 0x49484452) return null   // 'IHDR'
    const w = buf.readUInt32BE(16), h = buf.readUInt32BE(20)
    return w > 0 && h > 0 ? { w, h } : null
  } catch { return null }
}

const require = createRequire(import.meta.url)
const { parseDesignSources, normalizeNodeId, parseFigmaUrl } = require('./design-parser.cjs')

const SCREEN_KEY_RE = /^[A-Za-z0-9_]+$/
const USAGE = 'usage: node scripts/check-screen-cache.mjs <stem> [--gate|--advisory]'

function taskFiles(stem) {
  if (process.env.FIGMA_SCREEN_TASK_FILE) return [process.env.FIGMA_SCREEN_TASK_FILE]
  return [
    join(PROJECT_ROOT, 'orchestrator', 'tasks', 'todo', `${stem}.md`),
    join(PROJECT_ROOT, 'orchestrator', 'tasks', 'backlog', `${stem}.md`),
    join(PROJECT_ROOT, 'orchestrator', 'tasks', 'pending', `${stem}.questions.md`),
  ].filter((p) => existsSync(p))
}

function loadDesign(files) {
  const bodies = []
  const readIssues = []
  const explicit = !!process.env.FIGMA_SCREEN_TASK_FILE
  for (const file of files) {
    try {
      bodies.push(readTaskMarkdown(file, { explicit }))
    } catch {
      readIssues.push({
        kind: 'TASK_SOURCE_UNAVAILABLE',
        message: `task source ${file === '-' ? 'stdin' : (displayPath(file) || 'file')} is unreadable, unsafe, oversized, or not valid UTF-8`
      })
    }
  }
  const design = parseDesignSources(bodies)
  design.issues = readIssues.concat(design.issues || [])
  return design
}

function issue(severity, issueKind, message, extra = {}) {
  return Object.assign({ severity, issueKind, message }, extra)
}

function artifact(dir, screen, suffix) {
  return join(dir, `${screen}${suffix}`)
}

function nodeValue(node, theme) {
  if (!node || typeof node !== 'object') return { url: '', nodeId: '' }
  if (theme === 'dark') {
    return {
      url: node.darkUrl || '',
      nodeId: normalizeNodeId(node.darkNodeId || ''),
    }
  }
  return { url: node.url || '', nodeId: normalizeNodeId(node.nodeId || '') }
}

function expectedSpecTheme(theme) {
  return theme === 'dark' ? 'dark' : 'light'
}

function expectedThemes(entry) {
  const themes = entry.themes || {}
  if (themes.primary) return [{ theme: 'primary', data: themes.primary, suffix: '' }]
  const out = []
  if (themes.light) out.push({ theme: 'light', data: themes.light, suffix: '' })
  if (themes.dark) out.push({ theme: 'dark', data: themes.dark, suffix: '.dark' })
  return out
}

function indexSemanticIssues(index) {
  const out = []
  for (const [screen, node] of Object.entries((index && index.nodes) || {})) {
    if (!node || !Array.isArray(node.variants)) continue
    const ids = node.variants.map((variant) => variant.id)
    if (ids.length !== new Set(ids).size) out.push(issue('BLOCKER', 'INDEX_VARIANT_ID_DUPLICATE', `${screen} has duplicate variant ids`, { screen }))
    const files = node.variants.map((variant) => variant.imageFile)
    if (files.length !== new Set(files).size) out.push(issue('BLOCKER', 'INDEX_VARIANT_FILE_DUPLICATE', `${screen} has duplicate variant image files`, { screen }))
    for (const variant of node.variants) {
      if (!variant.imageFile.startsWith(screen) || !variant.imageFile.endsWith('.png')) {
        out.push(issue('BLOCKER', 'INDEX_VARIANT_FILE_INVALID', `${screen}/${variant.id} imageFile must be a ${screen}-prefixed PNG basename`, { screen, theme: variant.id }))
      }
      if (!variant.tokensFile.startsWith(screen) || !variant.tokensFile.endsWith('.tokens.json')) {
        out.push(issue('BLOCKER', 'INDEX_TOKEN_SIDECAR_FILE_INVALID', `${screen}/${variant.id} tokensFile must be a ${screen}-prefixed .tokens.json basename`, { screen, theme: variant.id }))
      }
    }
    const summaryMatches = (url, nodeId, fetchedAt) => node.variants.some((variant) => variant.url === url && variant.nodeId === nodeId && variant.fetchedAt === fetchedAt)
    if (node.url !== undefined && !summaryMatches(node.url, node.nodeId, node.fetchedAt)) {
      out.push(issue('BLOCKER', 'INDEX_PRIMARY_SUMMARY_MISMATCH', `${screen} primary summary does not match any current variant`, { screen }))
    }
    if (node.darkUrl !== undefined && !summaryMatches(node.darkUrl, node.darkNodeId, node.darkFetchedAt)) {
      out.push(issue('BLOCKER', 'INDEX_DARK_SUMMARY_MISMATCH', `${screen} dark summary does not match any current variant`, { screen }))
    }
  }
  return out
}

function tokenRefsInSpec(spec) {
  const out = new Set()
  const visit = (value) => {
    if (typeof value === 'string') {
      const match = /^\{([^{}]+)\}$/.exec(value)
      if (match) out.add(match[1])
      return
    }
    if (Array.isArray(value)) {
      for (const entry of value) visit(entry)
      return
    }
    if (!value || typeof value !== 'object') return
    if (typeof value.tokenRef === 'string') out.add(value.tokenRef.replace(/^\{|\}$/g, ''))
    for (const entry of Object.values(value)) visit(entry)
  }
  visit(spec)
  return out
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
  catch {
    console.error(USAGE)
    process.exit(1)
  }
  const runMode = cli.has('--advisory') ? 'advisory' : 'gate'
  const gate = runMode === 'gate'

  const screensRoot = figmaScreensRoot()
  const screensDir = join(screensRoot, stem)
  const indexPath = join(screensDir, 'index.json')
  const taskFilesForStem = taskFiles(stem)
  const issues = []
  const screens = []
  const inputHashes = {}
  const inputs = { stem, screensDir, taskFiles: taskFilesForStem, indexPath, designSourceHash: null }

  let design = { entries: [], issues: [], sourceHash: null, hasPullable: false }
  if (!taskFilesForStem.length) {
    issues.push(issue('BLOCKER', 'TASK_NOT_FOUND', `task markdown not found for ${stem}`))
  } else {
    design = loadDesign(taskFilesForStem)
    inputs.designSourceHash = design.sourceHash
    // Deliberately NOT hashing the whole task file into inputHashes: the run loop
    // legitimately mutates it AFTER this gate runs (Step 6a writes ## Outcome, ship-done
    // injects the Figma-meta digest, the sanctioned move renames todo/ → done/), so a
    // whole-file hash deadlocks every UI ship with REPORT_INPUT_HASH_MISMATCH. The real
    // dependency is the `## Design` section only — recorded as inputs.designSourceHash
    // above and re-verified by evidence-bundle --stage final (DESIGN_CHANGED_SINCE_CHECK),
    // which still fails closed on a post-check design edit.
    for (const dIssue of design.issues) {
      issues.push(issue('BLOCKER', dIssue.kind || 'INVALID_DESIGN', dIssue.message || 'invalid design bullet', { screen: dIssue.screen || null }))
    }
  }

  let index = null
  if (!exists(indexPath)) {
    if (design.hasPullable) issues.push(issue('BLOCKER', 'INDEX_MISSING', `index.json missing at ${indexPath}`))
  } else {
    inputHashes[indexPath] = fileHash(indexPath)
    try { index = readJson(indexPath) }
    catch (e) { issues.push(issue('BLOCKER', 'INDEX_UNREADABLE', `index.json unreadable: ${e.message}`)) }
  }

  if (index) {
    let validateIndex = null
    try {
      validateIndex = await compileSchema(figmaPath('token-schemas', 'screen-index.schema.json'), { gate })
    } catch (e) {
      issues.push(issue('BLOCKER', 'AJV_UNAVAILABLE', `screen-index schema validation unavailable in gate mode: ${e.message}`))
    }
    issues.push(...schemaIssues(validateIndex, index, 'index').map((i) => issue('BLOCKER', i.issueKind, i.message, { path: i.path })))
    issues.push(...indexSemanticIssues(index))
    if (index.taskStem !== stem) {
      issues.push(issue('BLOCKER', 'INDEX_STEM_MISMATCH', `index taskStem ${JSON.stringify(index.taskStem)} does not match ${JSON.stringify(stem)}`, { file: indexPath }))
    }
  }

  const nodes = (index && index.nodes && typeof index.nodes === 'object') ? index.nodes : {}
  const invalidNodeKeys = new Set(Object.keys(nodes).filter((name) => !SCREEN_KEY_RE.test(name)))
  for (const name of invalidNodeKeys) {
    issues.push(issue('BLOCKER', 'INDEX_SCREEN_KEY_INVALID', `index screen key ${JSON.stringify(name)} must match ${SCREEN_KEY_RE}; unsafe keys cannot be mapped to cache filenames`, { screen: name, file: indexPath }))
  }
  const expected = (design.entries || []).filter((e) => !e.none)
  // Cache identity is the exact Design screen name. A rename requires a fresh pull; an old
  // node with the same Figma id is not silently rebound to a different cache filename.
  function findCacheNode(entry) {
    return !invalidNodeKeys.has(entry.screen) && nodes[entry.screen]
      ? { node: nodes[entry.screen], name: entry.screen }
      : null
  }
  const matchedKeys = new Set()

  let instValidate = null
  try {
    instValidate = await compileSchema(figmaPath('token-schemas', 'instances.schema.json'), { gate })
  } catch (e) {
    issues.push(issue('BLOCKER', 'AJV_UNAVAILABLE', `instances schema validation unavailable in gate mode: ${e.message}`))
  }
  let specValidate = null
  try {
    specValidate = await compileSchema(figmaPath('token-schemas', 'spec.schema.json'), { gate })
  } catch (e) {
    issues.push(issue('BLOCKER', 'AJV_UNAVAILABLE', `spec schema validation unavailable in gate mode: ${e.message}`))
  }
  let tokenCaptureValidate = null
  try {
    tokenCaptureValidate = await compileSchema(figmaPath('schemas', 'observed-token-source-capture.schema.json'), { gate })
  } catch (e) {
    issues.push(issue('BLOCKER', 'AJV_UNAVAILABLE', `token sidecar schema validation unavailable in gate mode: ${e.message}`))
  }

  for (const entry of expected) {
    const row = { screen: entry.screen, status: 'complete', themes: {} }
    if (!SCREEN_KEY_RE.test(entry.screen)) {
      row.status = 'stale'
      issues.push(issue('BLOCKER', 'DESIGN_SCREEN_KEY_INVALID', `design screen ${JSON.stringify(entry.screen)} must match ${SCREEN_KEY_RE}; unsafe names cannot be mapped to cache filenames`, { screen: entry.screen }))
      screens.push(row)
      continue
    }
    const match = findCacheNode(entry)
    const node = match && match.node
    const cacheKey = (match && match.name) || entry.screen
    if (match) matchedKeys.add(cacheKey)
    if (!node) {
      row.status = 'missing'
      issues.push(issue('BLOCKER', 'SCREEN_MISSING_IN_INDEX', `screen missing from index: ${entry.screen}`, { screen: entry.screen }))
    }
    const tokenNamesByVariant = new Map()
    for (const variant of (node && node.variants) || []) {
      const sidecarPath = join(screensDir, variant.tokensFile)
      let sidecarBytes = null
      try {
        sidecarBytes = readContainedSingleLinkFile({
          root: screensDir,
          file: sidecarPath,
          maxBytes: 4 * 1024 * 1024
        })
      } catch (error) {
        row.status = row.status === 'complete' ? 'incomplete' : row.status
        issues.push(issue('BLOCKER', 'TOKEN_SIDECAR_UNSAFE', `${variant.tokensFile} is missing, unsafe, linked, or oversized (${error.code || error.message})`, { screen: entry.screen, theme: variant.id, file: sidecarPath }))
        continue
      }
      inputHashes[sidecarPath] = bytesHash(sidecarBytes)
      if (inputHashes[sidecarPath] !== variant.tokensHash) {
        row.status = 'stale'
        issues.push(issue('BLOCKER', 'TOKEN_SIDECAR_HASH_MISMATCH', `${variant.tokensFile} hash differs from index.json`, { screen: entry.screen, theme: variant.id, file: sidecarPath }))
        continue
      }
      let sidecar
      try { sidecar = JSON.parse(sidecarBytes.toString('utf8')) } catch (error) {
        issues.push(issue('BLOCKER', 'TOKEN_SIDECAR_UNREADABLE', `${variant.tokensFile} is not valid JSON`, { screen: entry.screen, theme: variant.id, file: sidecarPath }))
        continue
      }
      const tokenSchemaIssues = schemaIssues(tokenCaptureValidate, sidecar, variant.tokensFile)
      for (const tokenIssue of tokenSchemaIssues) {
        issues.push(issue('BLOCKER', 'TOKEN_SIDECAR_SCHEMA_INVALID', tokenIssue.message, { screen: entry.screen, theme: variant.id, file: sidecarPath, path: tokenIssue.path }))
      }
      if (tokenSchemaIssues.length) continue
      const semantic = captureSemanticError(sidecar)
      if (semantic) {
        issues.push(issue('BLOCKER', 'TOKEN_SIDECAR_SEMANTIC_INVALID', semantic, { screen: entry.screen, theme: variant.id, file: sidecarPath }))
        continue
      }
      if (sidecar.captureOperationId !== variant.captureOperationId || sidecar.captureSequence !== variant.captureSequence ||
          sidecar.source.nodeId !== variant.nodeId ||
          sidecar.source.context.theme !== variant.theme ||
          sidecar.source.context.locale !== variant.locale ||
          sidecar.source.context.platform !== variant.platform ||
          sidecar.source.origin.kind !== 'task-screen' || sidecar.source.origin.taskStem !== stem ||
          sidecar.source.origin.screenKey !== entry.screen || sidecar.source.origin.variantId !== variant.id) {
        issues.push(issue('BLOCKER', 'TOKEN_SIDECAR_VARIANT_MISMATCH', `${variant.tokensFile} source/context/operation does not match its index variant`, { screen: entry.screen, theme: variant.id, file: sidecarPath }))
        continue
      }
      tokenNamesByVariant.set(variant.id, new Set(sidecar.observations.map((observation) => observation.providerName)))
    }
    // H4-P1: a node the design declares as non-screen (`[dialog]`/`[component]`/`[overlay]`)
    // MUST persist that `kind` in index.json so the comparator/builder/viewer treat it
    // correctly; the kind must survive parsing into the cache contract.
    if (node && entry.kind && entry.kind !== 'screen') {
      const nodeKind = typeof node.kind === 'string' ? node.kind.toLowerCase() : null
      if (!nodeKind) {
        if (row.status === 'complete') row.status = 'stale'
        issues.push(issue('BLOCKER', 'KIND_MISSING_IN_INDEX', `index node for ${entry.screen} omits kind; the design declares it as [${entry.kind}] — a non-screen node must self-identify`, { screen: entry.screen }))
      } else if (nodeKind !== entry.kind) {
        row.status = 'stale'
        issues.push(issue('BLOCKER', 'KIND_MISMATCH', `kind mismatch for ${entry.screen}: design declares [${entry.kind}], index says [${nodeKind}]`, { screen: entry.screen }))
      }
    } else if (node) {
      // Reverse direction: the design declares (or defaults to) [screen] but the index
      // carries a non-screen kind — a stale pull, or the bullet's kind tag was edited after
      // the pull. One-directional checking left this half fail-open: the comparator would
      // apply dialog/component container+geometry rules to a full-bleed screen.
      const nodeKind = typeof node.kind === 'string' ? node.kind.toLowerCase() : null
      if (nodeKind && nodeKind !== 'screen') {
        row.status = 'stale'
        issues.push(issue('BLOCKER', 'KIND_MISMATCH', `kind mismatch for ${entry.screen}: design declares [screen], index says [${nodeKind}]`, { screen: entry.screen }))
      }
    }
    for (const t of expectedThemes(entry)) {
      const label = t.theme === 'primary' ? 'primary' : t.theme
      const actual = nodeValue(node, t.theme)
      // t.data.url is canonicalized by parseFigmaUrl (slug + extra query params stripped);
      // actual.url is the raw value the figma:screens session wrote into index.json, which
      // normally still carries the /<FileName> slug and a &t= session token. Canonicalize
      // both sides so a real Figma deep link does not trip a spurious URL_MISMATCH.
      const actualCanon = (parseFigmaUrl(actual.url) || {}).url || actual.url
      row.themes[label] = { expectedNodeId: t.data.nodeId, actualNodeId: actual.nodeId || null }
      if (node && t.data.url !== actualCanon) {
        row.status = 'stale'
        issues.push(issue('BLOCKER', 'URL_MISMATCH', `URL mismatch for ${entry.screen} ${label}`, { screen: entry.screen, theme: label }))
      }
      if (node && normalizeNodeId(t.data.nodeId) !== actual.nodeId) {
        row.status = 'stale'
        issues.push(issue('BLOCKER', 'NODE_ID_MISMATCH', `node-id mismatch for ${entry.screen} ${label}`, { screen: entry.screen, theme: label }))
      }

      const suffix = t.suffix
      const required = [
        { kind: 'SPEC_MISSING', path: artifact(screensDir, cacheKey, `${suffix}.spec.json`) },
        { kind: 'PNG_MISSING', path: artifact(screensDir, cacheKey, `${suffix}.png`) },
      ]
      for (const req of required) {
        if (!exists(req.path)) {
          row.status = row.status === 'complete' ? 'incomplete' : row.status
          issues.push(issue('BLOCKER', req.kind, `${req.path} missing`, { screen: entry.screen, theme: label, file: req.path }))
        } else inputHashes[req.path] = fileHash(req.path)
      }
      const specPath = artifact(screensDir, cacheKey, `${suffix}.spec.json`)
      if (exists(specPath)) {
        try {
          const spec = readJson(specPath)
          if (specValidate) {
            const specIssues = schemaIssues(specValidate, spec, `${entry.screen}${suffix}.spec.json`)
            for (const si of specIssues) issues.push(issue('BLOCKER', 'SPEC_SCHEMA_INVALID', si.message, { screen: entry.screen, theme: label, file: specPath, path: si.path }))
          }
          if (spec.screen !== cacheKey) {
            issues.push(issue('BLOCKER', 'SPEC_SCREEN_MISMATCH', `spec screen "${spec.screen}" does not match cache filename "${cacheKey}"`, { screen: entry.screen, theme: label, file: specPath }))
          }
          const matchingVariant = (node && node.variants || []).find((variant) =>
            variant.nodeId === actual.nodeId &&
            (t.theme === 'primary' || variant.theme === expectedSpecTheme(t.theme)))
          if (matchingVariant && tokenNamesByVariant.has(matchingVariant.id)) {
            const observedNames = tokenNamesByVariant.get(matchingVariant.id)
            for (const tokenRef of tokenRefsInSpec(spec)) {
              if (!observedNames.has(tokenRef)) {
                issues.push(issue('BLOCKER', 'TOKEN_SCREEN_REFERENCE_MISMATCH', `spec token reference ${JSON.stringify(tokenRef)} is absent from frozen sidecar ${matchingVariant.tokensFile}`, { screen: entry.screen, theme: label, file: specPath }))
              }
            }
          }
          // 'primary' (a plain URL, no light:/dark: tag) is theme-AGNOSTIC: the pull writes the
          // frame's actual theme (dark OR light per the pull contract — figma-actions.js). Only an
          // explicit light:/dark: tag pins an exact theme. Forcing 'light' here false-blocked a
          // dark-first product's plain-URL dark frame (fail-closed — the pull could not comply
          // without mislabeling it 'light').
          const themeOk = t.theme === 'primary'
            ? (spec.theme === 'light' || spec.theme === 'dark')
            : (spec.theme === expectedSpecTheme(t.theme))
          if (!themeOk) {
            const exp = t.theme === 'primary' ? 'light or dark' : expectedSpecTheme(t.theme)
            issues.push(issue('BLOCKER', 'SPEC_THEME_MISMATCH', `spec theme "${spec.theme}" does not match expected "${exp}"`, { screen: entry.screen, theme: label, file: specPath }))
          }
          // Every pulled PNG/spec pair represents the same frame, stamped or not. A tight aspect
          // mismatch proves a torn normalize write or a wrong export and must never reach compare.
          const frameH = spec.frameSizeDp && spec.frameSizeDp.h
          const pngPath = artifact(screensDir, cacheKey, `${suffix}.png`)
          const dims = exists(pngPath) ? pngSize(pngPath) : null
          if (exists(pngPath) && !dims) {
            row.status = row.status === 'complete' ? 'incomplete' : row.status
            issues.push(issue('BLOCKER', 'PNG_UNREADABLE', `${pngPath} is not a readable PNG with a valid IHDR size — re-pull the screens; an opaque/corrupt oracle cannot satisfy the cache gate`, { screen: entry.screen, theme: label, file: pngPath }))
          }
          if (dims && spec.frameSizeDp && spec.frameSizeDp.w > 0 && frameH > 0) {
            const aspectSpec = spec.frameSizeDp.w / frameH
            const aspectPng = dims.w / dims.h
            if (Math.abs(aspectSpec - aspectPng) / aspectSpec > 0.02) {
              row.status = 'stale'
              const stamped = spec.chromeCrop && typeof spec.chromeCrop === 'object'
              const kind = stamped ? 'CHROME_CROP_ASPECT' : 'SPEC_PNG_ASPECT_MISMATCH'
              const state = stamped ? "stamped post-crop" : 'unstamped'
              issues.push(issue('BLOCKER', kind, `${state} spec frame is ${spec.frameSizeDp.w}×${frameH}dp (aspect ${aspectSpec.toFixed(3)}) but the oracle PNG is ${dims.w}×${dims.h}px (aspect ${aspectPng.toFixed(3)}) — PNG and spec do not represent the same frame; re-pull the screens so the pair is rewritten together`, { screen: entry.screen, theme: label, file: pngPath }))
            }
          }
          // R6-2 — a chromeCrop-stamped spec is VERIFIED, not trusted: the crop's own
          // invariants must hold or the normalization is inconsistent and must be re-run.
          if (spec.chromeCrop && typeof spec.chromeCrop === 'object') {
            if ((entry.kind || 'screen') !== 'screen') {
              row.status = 'stale'
              issues.push(issue('BLOCKER', 'CHROME_CROP_KIND_MISMATCH', `chromeCrop is stamped on ${entry.screen} [${entry.kind}] even though device chrome normalization applies only to full screens — re-pull the node so the non-screen oracle/spec pair is restored without a screen-only crop`, { screen: entry.screen, theme: label, file: specPath }))
            }
            // (1) No surviving chrome: nothing may still match the chrome predicate, carry a
            // chrome layer name, or carry the "9:41" marketing time (the predicate's axiom).
            for (const r of chromeResidue(spec)) {
              row.status = 'stale'
              issues.push(issue('BLOCKER', 'CHROME_CROP_RESIDUE', `stamped spec still carries device chrome — '${r.name}': ${r.reason} (a stamped-but-still-chromed spec means the crop is inconsistent). Remedy: re-pull the screens (the figma:screens session re-runs normalize-oracle on the fresh pair — a stamped spec itself is skipped by design); if the layer is real content, rename it in Figma so the chrome predicate stops matching, then re-pull`, { screen: entry.screen, theme: label, file: specPath }))
            }
            // (2) A bad shift fails loud: no element may extend above y=0 or below frameSizeDp.h.
            const lists = [spec.elements, spec.nodes].filter(Array.isArray)
            for (const arr of lists) {
              for (const el of arr) {
                const b = el && el.bboxDp
                if (!b || typeof b !== 'object') continue
                if (b.y < -0.01 || (frameH > 0 && b.y + b.h > frameH + 0.01)) {
                  row.status = 'stale'
                  issues.push(issue('BLOCKER', 'CHROME_CROP_BAD_SHIFT', `stamped spec element '${el.name || el.stableId || '(unnamed)'}' extends outside the post-crop frame (y=${b.y}, h=${b.h}, frame h=${frameH}) — the crop shift is inconsistent; re-pull the screens so normalize-oracle re-runs cleanly`, { screen: entry.screen, theme: label, file: specPath }))
                  break
                }
              }
            }
          }
        } catch (e) {
          issues.push(issue('BLOCKER', 'SPEC_UNREADABLE', `spec unreadable: ${e.message}`, { screen: entry.screen, theme: label, file: specPath }))
        }
      }
    }

    const instPath = artifact(screensDir, cacheKey, '.instances.json')
    if (!exists(instPath)) {
      row.status = row.status === 'complete' ? 'incomplete' : row.status
      issues.push(issue('BLOCKER', 'INSTANCES_MISSING', `${instPath} missing`, { screen: entry.screen, file: instPath }))
    } else {
      inputHashes[instPath] = fileHash(instPath)
      try {
        const inst = readJson(instPath)
        const instIssues = schemaIssues(instValidate, inst, `${entry.screen}.instances.json`)
        for (const ii of instIssues) issues.push(issue(gate ? 'BLOCKER' : 'WARN', 'INSTANCES_SCHEMA_INVALID', ii.message, { screen: entry.screen, file: instPath, path: ii.path }))
      } catch (e) {
        row.status = row.status === 'complete' ? 'incomplete' : row.status
        issues.push(issue('BLOCKER', 'INSTANCES_UNREADABLE', `instances unreadable: ${e.message}`, { screen: entry.screen, file: instPath }))
      }
    }

    const ctxPath = artifact(screensDir, cacheKey, '.context.json')
    if (!exists(ctxPath)) {
      row.status = row.status === 'complete' ? 'incomplete' : row.status
      issues.push(issue('BLOCKER', 'CONTEXT_MISSING', `${ctxPath} missing`, { screen: entry.screen, file: ctxPath }))
    } else {
      inputHashes[ctxPath] = fileHash(ctxPath)
    }
    screens.push(row)
  }

  // A cache node is "extra" only if NO entry resolved to it (by name or identity) —
  // identity-matching a renamed bullet means its raw-named cache file is not extra.
  for (const nodeName of Object.keys(nodes)) {
    if (!matchedKeys.has(nodeName)) issues.push(issue('WARN', 'EXTRA_CACHE_NODE', `cache contains extra screen not requested by current task: ${nodeName}`, { screen: nodeName }))
  }

  if (!expected.length && design.hasPullable) issues.push(issue('BLOCKER', 'NO_EXPECTED_SCREENS', 'no pullable screens parsed from Design section'))

  const hasBlocker = issues.some((i) => i.severity === 'BLOCKER' || i.severity === 'ERROR')
  const hasWarn = issues.some((i) => i.severity === 'WARN' || i.severity === 'WARNING')
  const overall = hasBlocker ? 'BLOCKER' : hasWarn ? 'WARN' : 'PASS'
  const { reportPath } = writeReport({
    name: 'screen-cache',
    taskStem: stem,
    mode: runMode,
    inputs,
    inputHashes,
    overall,
    issues,
    extra: { screens },
  })

  console.log(`screen-cache: ${stem} ${overall}`)
  console.log(`Report: ${reportPath}`)
  for (const i of issues) console.log(`  [${i.severity}] ${i.issueKind}${i.screen ? ` ${i.screen}` : ''}: ${i.message}`)
  process.exit(gate && hasBlocker ? 1 : 0)
}

main().catch((e) => {
  console.error(`FATAL: ${e.message}`)
  process.exit(1)
})
