#!/usr/bin/env node
// run-figma-gates.mjs — the ONE driver for the per-task figma gate choreography.
//
//   node scripts/run-figma-gates.mjs <stem> --stage prebuild
//   node scripts/run-figma-gates.mjs <stem> --stage screenshot [--modules :a:b,:c] [--skip-record] [--record-task T] [--no-fix]
//   node scripts/run-figma-gates.mjs <stem> --stage final
//
// Before this driver the sequence was agent-choreographed across ~10 hand-ordered commands,
// 4 env vars and 3 ordering constraints (run-loop Step 1b / validator-routing Step 4.6b) —
// each miss (an unexported run id, a forgotten SCREENSHOT_CAPTURE_STARTED_AT, census stale
// after a registry upsert, ROBORAZZI_OUTPUT_DIRS unset for a non-ui-screen-features module)
// cost a full fail-closed gate round. The driver owns exactly that choreography:
//   * one file-pinned run id (pipelineRunId) exported to every child;
//   * prebuild  = check-screen-cache --gate → component-census → check-spec --gate
//                 → evidence-bundle --stage prebuild --fresh;
//   * screenshot = check-capture-config --gate (with a --fix self-heal retry unless --no-fix)
//                 → [record via ./gradlew <module>:<record-task>, STARTED_AT exported first]
//                 → capture manifest built from index.json nodeIds (identity binding)
//                 → compare-screenshots --gate (ROBORAZZI_OUTPUT_DIRS derived from --modules);
//   * final     = census registry-consult re-check (re-runs component-census under the SAME
//                 run id when a registry upsert changed a consulted entry — the Step-3
//                 ordering trap) → evidence-bundle --stage final --fresh.
// It deliberately does NOT: author the agent spec report (validator-owned), run
// extract-compose-model/compare-screen-spec (they need plan inputs), or swallow any child's
// output — every child runs with inherited stdio and the driver exits with the first
// blocking step's code. NEVER calls Figma (golden invariant).
//
// Env (all optional; the same overrides every gate script honors):
//   FIGMA_PIPELINE_RUN_ID — explicit run id (wins over the pin, re-pins it)
//   FIGMA_REPORTS_DIR / FIGMA_SCREEN_CACHE_ROOT / FIGMA_SPEC_SCREENS_DIR / FIGMA_CACHE_ROOT
//   FIGMA_CENSUS_CODE_ROOTS — optional effective scan roots inherited by child gates; narrowing
//     remains diagnostic-safe because final evidence checks capture scope against PROJECT_ROOT.
//   SCREENSHOT_CAPTURE_STARTED_AT — REQUIRED with --skip-record (the recorder's start time);
//     without --skip-record the driver stamps it itself right before the record task. A
//     --skip-record run is diagnostic and cannot certify the final evidence bundle.

import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { delimiter, dirname, join, resolve, isAbsolute } from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { PROJECT_ROOT, bindingsManifestEntries, figmaPath, figmaScreensRoot, loadBindings, parseCli, pipelineRunId, readConfig, runIdPinPath, displayPath } from './_util.mjs'
import { MAPPING_CONSULT_KEY, computeMappingConsultDigest } from './component-census.mjs'
import { loadDesignComponentInventory, loadComponentMappings, loadPublishedComponentAnalysis } from './lib/design-components.mjs'
import { assertTaskStem } from './report-utils.mjs'

const HERE = join(fileURLToPath(import.meta.url), '..')
const USAGE = [
  'usage: node scripts/run-figma-gates.mjs <stem> --stage prebuild',
  '       node scripts/run-figma-gates.mjs <stem> --stage screenshot [--modules :a:b,:c] [--skip-record] [--record-task T] [--no-fix]',
  '       node scripts/run-figma-gates.mjs <stem> --stage final',
].join('\n')

// W6-2: the record-task SUFFIX comes from project-config (roborazziRecordTask) so a product
// with a different convention plugin never relies on the template literal; --record-task
// still overrides per run.
const DEFAULT_RECORD_TASK = readConfig('roborazziRecordTask') || 'recordRoborazziAndroidHostTest'

let cli
try {
  cli = parseCli({
    allowedFlags: ['--stage', '--modules', '--skip-record', '--record-task', '--no-fix'],
    valueFlags: ['--stage', '--modules', '--record-task'],
    booleanFlags: ['--skip-record', '--no-fix'],
    usage: USAGE,
  })
} catch (e) {
  console.error(e.message)
  process.exit(1)
}
let stem
try { stem = assertTaskStem(cli.positional[0] || '') }
catch {
  console.error(USAGE)
  process.exit(1)
}
const stage = cli.value('--stage') || ''
if (!['prebuild', 'screenshot', 'final'].includes(stage)) {
  console.error(USAGE)
  process.exit(1)
}

const reportsDir = process.env.FIGMA_REPORTS_DIR || figmaPath('reports')
const runId = pipelineRunId(stem)
console.log(`figma:gates ${stem} --stage ${stage} · run id ${runId} (pinned at ${displayPath(runIdPinPath(stem))})`)

const steps = []
function runStep(label, cmd, args, extraEnv = {}, opts = {}) {
  console.log(`\n── ${label} ──`)
  const r = spawnSync(cmd, args, {
    cwd: PROJECT_ROOT,
    stdio: 'inherit',
    env: { ...process.env, FIGMA_PIPELINE_RUN_ID: runId, ...extraEnv },
  })
  const status = r.status == null ? 1 : r.status
  steps.push({ label, status })
  if (status !== 0 && !opts.allowFail) finish(status)
  return status
}
function node(label, script, args, extraEnv, opts) {
  return runStep(label, 'node', [join(HERE, script), ...args], extraEnv, opts)
}
function finish(code) {
  console.log('\nfigma:gates summary:')
  for (const s of steps) console.log(`  ${s.status === 0 ? 'PASS' : `EXIT ${s.status}`}  ${s.label}`)
  process.exit(code)
}

// ── census mapping-consult re-check (final stage) ───────────────────────────
// The Step-1b census pins a digest over the design/mapping/analysis rows the
// identities it consulted actually touched; a finalization that changed a
// CONSULTED identity invalidates it and the final bundle would block with
// REPORT_INPUT_HASH_MISMATCH. Re-derive here and re-run the census under the
// pinned id when (and only when) the consulted identities actually drifted.
function censusConsultDrifted() {
  let report
  try { report = JSON.parse(readFileSync(join(reportsDir, `census-${stem}.json`), 'utf8')) } catch { return false }
  const consult = report && report.mappingConsult
  const expected = report && report.inputHashes && report.inputHashes[MAPPING_CONSULT_KEY]
  if (!consult || consult.version !== 2 || !Array.isArray(consult.setIds) || typeof expected !== 'string') return false
  let truth
  try {
    const design = loadDesignComponentInventory()
    const mappings = loadComponentMappings(design.present ? design.inventory.scopeId : '')
    const analysis = loadPublishedComponentAnalysis()
    truth = { inventory: design.present ? design.inventory : null, registry: mappings.registry, analysis }
  } catch { return true }
  return computeMappingConsultDigest(consult.setIds, truth) !== expected
}

// ── capture manifest (screenshot stage) — identity binding by nodeId ────────
function inheritedCaptureEntries(manifestPath) {
  if (!manifestPath) return []
  const json = JSON.parse(readFileSync(manifestPath, 'utf8'))
  if (!json || typeof json !== 'object' || Array.isArray(json) || !Array.isArray(json.captures)) {
    throw new Error('inherited capture manifest must be an object with a captures array')
  }
  const manifestDir = dirname(resolve(manifestPath))
  return json.captures.map((entry, index) => {
    if (!entry || typeof entry !== 'object' || Array.isArray(entry) || typeof entry.path !== 'string' || !entry.path) {
      throw new Error(`inherited capture manifest captures[${index}] is invalid`)
    }
    return { ...entry, path: isAbsolute(entry.path) ? entry.path : resolve(manifestDir, entry.path) }
  })
}

function buildCaptureManifest(roboDirs, recording, inheritedManifestPath = '') {
  const index = JSON.parse(readFileSync(join(figmaScreensRoot(), stem, 'index.json'), 'utf8'))
  if (!index || index.schemaVersion !== 3 || index.taskStem !== stem || !index.nodes || typeof index.nodes !== 'object' || Array.isArray(index.nodes)) {
    throw new Error('screen index must satisfy the current schemaVersion 2 contract before screenshot comparison')
  }
  const nodes = index.nodes
  // Explicit bindings and current index variants are the two declared identity sources.
  const bound = bindingsManifestEntries(loadBindings(stem), roboDirs, existsSync)
  const variantsByNodeId = new Map()
  for (const node of Object.values(nodes)) {
    for (const variant of (node && Array.isArray(node.variants) ? node.variants : [])) {
      const key = String(variant.nodeId)
      const ids = variantsByNodeId.get(key) || []
      ids.push(String(variant.id))
      variantsByNodeId.set(key, ids)
    }
  }
  const captures = bound.entries.map((entry) => {
    const ids = variantsByNodeId.get(String(entry.nodeId)) || []
    return ids.length === 1 ? { ...entry, variantId: ids[0] } : entry
  })
  for (const [screen, node] of Object.entries(nodes)) {
    if (!node || typeof node !== 'object' || !Array.isArray(node.variants)) continue
    for (const variant of node.variants) {
      const imageFile = String(variant.imageFile || '')
      const suffix = imageFile.slice(screen.length, -4)
      if (!imageFile.startsWith(screen) || !imageFile.endsWith('.png') || (suffix && !suffix.startsWith('.'))) continue
      const captureName = `${screen}Screenshot${suffix}.png`
      for (const dir of roboDirs) {
        const path = join(dir, captureName)
        if (existsSync(path)) captures.push({ captureName, path, nodeId: String(variant.nodeId), variantId: String(variant.id), primaryState: true })
      }
    }
  }
  // --skip-record may rely on a caller manifest for rename-safe capture entries. Keep those
  // entries, but never inherit its provenance: the driver owns the wrapper and stamps it
  // preexisting so a caller-asserted `recorded` mode cannot make this diagnostic run certify.
  if (recording && recording.mode === 'preexisting') {
    captures.push(...inheritedCaptureEntries(inheritedManifestPath))
  }
  const unique = []
  const seen = new Set()
  for (const entry of captures) {
    const key = JSON.stringify([entry.nodeId, entry.variantId || null, resolve(entry.path)])
    if (seen.has(key)) continue
    seen.add(key)
    unique.push(entry)
  }
  const manifestPath = join(reportsDir, `.capture-manifest-${stem}.json`)
  mkdirSync(reportsDir, { recursive: true })
  writeFileSync(manifestPath, JSON.stringify({ recording, captures: unique }, null, 2) + '\n')
  console.log(`capture manifest: ${unique.length} entr${unique.length === 1 ? 'y' : 'ies'} -> ${displayPath(manifestPath)}`)
  return manifestPath
}

if (stage === 'prebuild') {
  node('check-screen-cache --gate', 'check-screen-cache.mjs', [stem, '--gate'])
  node('component-census', 'component-census.mjs', [stem, '--screens-dir', figmaScreensRoot(), '--out', join(reportsDir, `census-${stem}.json`)])
  node('check-spec --gate', 'check-spec.mjs', [stem, '--gate'])
  node('evidence-bundle --stage prebuild --fresh', 'evidence-bundle.mjs', [stem, '--stage', 'prebuild', '--fresh'])
  finish(0)
}

if (stage === 'screenshot') {
  // 1. Static capture-config gate, with the sanctioned --fix self-heal retry.
  const cfg = node('check-capture-config --gate', 'check-capture-config.mjs', [stem, '--gate'], {}, { allowFail: true })
  if (cfg !== 0) {
    if (cli.has('--no-fix')) finish(cfg)
    node('check-capture-config --fix (self-heal)', 'check-capture-config.mjs', [stem, '--fix'], {}, { allowFail: true })
    node('check-capture-config --gate (re-check)', 'check-capture-config.mjs', [stem, '--gate'])
  }

  // 2. Record (unless the caller already did) — STARTED_AT is stamped BEFORE the record so
  //    gate mode can prove capture freshness; with --skip-record the caller must supply it.
  const modules = (cli.value('--modules') || '').split(',').map((m) => m.trim()).filter(Boolean)
  const moduleDirs = modules.map((m) => join(PROJECT_ROOT, ...m.replace(/^:/, '').split(':'), 'build', 'outputs', 'roborazzi'))
  let recording
  if (!cli.has('--skip-record')) {
    if (!modules.length) {
      console.error('screenshot stage needs --modules :a:b[,:c] to record, or --skip-record when captures already exist')
      finish(1)
    }
    process.env.SCREENSHOT_CAPTURE_STARTED_AT = String(Date.now())
    const recordTask = cli.value('--record-task') || DEFAULT_RECORD_TASK
    for (const m of modules) {
      runStep(`./gradlew ${m}:${recordTask}`, join(PROJECT_ROOT, 'gradlew'), [`${m}:${recordTask}`])
    }
    recording = {
      mode: 'recorded',
      pipelineRunId: runId,
      startedAt: process.env.SCREENSHOT_CAPTURE_STARTED_AT,
      completedAt: new Date().toISOString(),
      modules,
      recordTask,
    }
  } else if (!process.env.SCREENSHOT_CAPTURE_STARTED_AT) {
    console.error('--skip-record requires SCREENSHOT_CAPTURE_STARTED_AT in the env (the time the existing record run STARTED) — gate mode refuses stale captures without it')
    finish(1)
  } else {
    recording = {
      mode: 'preexisting',
      pipelineRunId: runId,
      assertedStartedAt: process.env.SCREENSHOT_CAPTURE_STARTED_AT,
    }
    console.log('--skip-record: comparison is diagnostic; final evidence will require a driver-observed recorder run')
  }

  // 3. Compare, with output dirs + nodeId manifest derived instead of hand-exported.
  const roboDirs = moduleDirs.filter((d) => existsSync(d))
  const compareEnv = {}
  if (roboDirs.length) compareEnv.ROBORAZZI_OUTPUT_DIRS = roboDirs.join(delimiter)
  const configuredDirs = (process.env.ROBORAZZI_OUTPUT_DIRS || process.env.ROBORAZZI_OUTPUT_DIR || '')
    .split(delimiter).map((value) => value.trim()).filter(Boolean).map((value) => resolve(value))
  const captureDirs = roboDirs.length ? roboDirs : [...new Set(configuredDirs)]
  if (!captureDirs.length) {
    console.error('screenshot stage requires --modules or an explicit ROBORAZZI_OUTPUT_DIR(S); repository-wide output discovery is not supported')
    finish(1)
  }
  const missingDirs = captureDirs.filter((dir) => !existsSync(dir))
  if (missingDirs.length) {
    console.error(`declared Roborazzi output dir does not exist: ${missingDirs.join(', ')}`)
    finish(1)
  }
  let manifestPath
  try {
    manifestPath = buildCaptureManifest(captureDirs, recording, process.env.SCREENSHOT_CAPTURE_MANIFEST || '')
  } catch (error) {
    console.error(`capture manifest build failed: ${error.message}`)
    finish(1)
  }
  // Always pass the driver-owned manifest, including an empty one. Falling back to an
  // inherited SCREENSHOT_CAPTURE_MANIFEST when discovery found no entries would let caller
  // provenance replace the canonical driver's result.
  compareEnv.SCREENSHOT_CAPTURE_MANIFEST = manifestPath
  node('compare-screenshots --gate', 'compare-screenshots.mjs', [stem, '--gate'], compareEnv)
  finish(0)
}

if (stage === 'final') {
  if (censusConsultDrifted()) {
    console.log('census registry-consult digest drifted (a consulted registry entry changed since the census) — re-running census under the pinned run id')
    node('component-census (consult re-pin)', 'component-census.mjs', [stem, '--screens-dir', figmaScreensRoot(), '--out', join(reportsDir, `census-${stem}.json`)])
  }
  node('evidence-bundle --stage final --fresh', 'evidence-bundle.mjs', [stem, '--stage', 'final', '--fresh'])
  finish(0)
}
