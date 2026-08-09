#!/usr/bin/env node
// evidence-clean.mjs <stem> [--artifacts|--bundle-only] — the deterministic way OUT of any evidence
// half-state (H7). A partial/manual run can leave a prebuild bundle beside a fresh
// screenshot report (SUPERSEDED), mixed-run reports (MIXED_RUNS), or a stale bundle —
// confusing overlapping signals. By default this removes a task's per-task Figma reports +
// bundle (+ the code-emitted digest), so the next `--stage final` bundle starts clean.
//
//   * Always: the evidence bundle, the seven gate reports (screen-cache, check-spec,
//     capture-config, census, spec, spec-compare, screenshot), `figma-meta-<stem>.txt`, and the `.run-id-<stem>`
//     pin (the next run mints a fresh pipeline run id), all confined to the reports dir
//     under FIGMA_CACHE_ROOT.
//   * `--artifacts`: also the compare artifact tree (`artifacts/screenshot/<mapped-stem>/`).
//   * `--bundle-only`: remove only the evidence bundle + figma-meta digest, preserving the
//     seven gate reports AND the run-id pin for an immediate
//     `evidence-bundle --stage final --fresh` re-run on the same run.
//
// Confined: every target is deleted through the shared root-anchored file guard. Symlinked
// ancestors, symlink entries, special files, unbounded trees, and concurrent replacements
// fail closed. The site's lite/artifact caches are mtime+size keyed, so a successful
// clean-then-regen (new mtime) is picked up on the next read — no server restart needed.
//
// Env:
//   FIGMA_REPORTS_DIR       — override reports dir
import { lstatSync } from 'node:fs'
import { createRequire } from 'node:module'
import { dirname, join } from 'node:path'
import { FIGMA_CACHE_ROOT, artifactSegment, figmaPath, ensureContained, runIdPinPath } from './_util.mjs'
import { assertTaskStem } from './report-utils.mjs'

const REPORT_PREFIXES = ['screen-cache', 'check-spec', 'capture-config', 'census', 'spec', 'spec-compare', 'screenshot']
const USAGE = 'usage: node scripts/evidence-clean.mjs <stem> [--artifacts|--bundle-only]'
const CLEANUP_ENTRY_MAX = 4096
const CLEANUP_DEPTH_MAX = 12
const fileGuards = createRequire(import.meta.url)('../../site/server/file-guards.js')

const reportsDir = process.env.FIGMA_REPORTS_DIR || figmaPath('reports')
const removed = []
const skipped = []

function confinedTarget(target) {
  let safe
  try { safe = ensureContained(FIGMA_CACHE_ROOT, target) }
  catch { skipped.push(`${target} (path is outside FIGMA_CACHE_ROOT)`); return null }
  if (safe === FIGMA_CACHE_ROOT) {
    skipped.push(`${safe} (refusing to remove FIGMA_CACHE_ROOT itself)`)
    return null
  }
  return safe
}

function removeRegularFile(target) {
  const safe = confinedTarget(target)
  if (!safe) return false
  const parent = dirname(safe)
  const inspected = fileGuards.inspectEntryUnder(FIGMA_CACHE_ROOT, parent, safe)
  if (inspected && inspected.status === 'missing') return true
  if (!inspected || inspected.status !== 'present' || !inspected.stat ||
      !inspected.stat.isFile() || inspected.stat.isSymbolicLink()) {
    skipped.push(`${safe} (not a root-anchored regular file)`)
    return false
  }
  if (!fileGuards.unlinkRegularFileUnder(FIGMA_CACHE_ROOT, parent, safe, { allowMissing: true })) {
    skipped.push(`${safe} (guarded unlink did not commit)`)
    return false
  }
  removed.push(safe)
  return true
}

function removeDirectoryTree(target, budget, depth = 0) {
  const safe = confinedTarget(target)
  if (!safe) return false
  if (depth > CLEANUP_DEPTH_MAX || budget.count >= CLEANUP_ENTRY_MAX) {
    skipped.push(`${safe} (cleanup tree exceeds the ${CLEANUP_ENTRY_MAX}-entry/${CLEANUP_DEPTH_MAX}-level bound)`)
    return false
  }
  const parent = dirname(safe)
  const inspected = fileGuards.inspectEntryUnder(FIGMA_CACHE_ROOT, parent, safe)
  if (inspected && inspected.status === 'missing') return true
  if (!inspected || inspected.status !== 'present' || !inspected.stat ||
      !inspected.stat.isDirectory() || inspected.stat.isSymbolicLink()) {
    skipped.push(`${safe} (not a root-anchored directory)`)
    return false
  }
  const listed = fileGuards.boundedDirectoryNamesUnder(
    FIGMA_CACHE_ROOT,
    safe,
    CLEANUP_ENTRY_MAX - budget.count,
  )
  if (!listed.ok) {
    skipped.push(`${safe} (directory listing is unsafe or exceeds the cleanup bound)`)
    return false
  }
  for (const name of listed.names) {
    budget.count++
    const child = join(safe, name)
    const entry = fileGuards.inspectEntryUnder(FIGMA_CACHE_ROOT, safe, child)
    if (!entry || entry.status !== 'present' || !entry.stat) {
      skipped.push(`${child} (entry changed during guarded cleanup)`)
      return false
    }
    if (entry.stat.isFile() && !entry.stat.isSymbolicLink()) {
      if (!removeRegularFile(child)) return false
    } else if (entry.stat.isDirectory() && !entry.stat.isSymbolicLink()) {
      if (!removeDirectoryTree(child, budget, depth + 1)) return false
    } else {
      skipped.push(`${child} (symlinks and special files are not cleanup authority)`)
      return false
    }
  }
  if (!fileGuards.removeEmptyDirectoryUnder(FIGMA_CACHE_ROOT, parent, safe)) {
    skipped.push(`${safe} (directory is no longer the verified empty generation)`)
    return false
  }
  removed.push(safe)
  return true
}

function cacheRootAvailable() {
  try {
    const stat = lstatSync(FIGMA_CACHE_ROOT)
    if (stat.isDirectory() && !stat.isSymbolicLink()) return true
    skipped.push(`${FIGMA_CACHE_ROOT} (FIGMA_CACHE_ROOT is not a real directory)`)
  } catch (error) {
    if (error && error.code === 'ENOENT') return false
    skipped.push(`${FIGMA_CACHE_ROOT} (cannot inspect FIGMA_CACHE_ROOT)`)
  }
  return false
}

function stemBundleFiles(stem) {
  return [
    join(reportsDir, `evidence-${stem}.json`),
    join(reportsDir, `figma-meta-${stem}.txt`),
  ]
}

function stemReportFiles(stem) {
  const names = REPORT_PREFIXES.map((name) => join(reportsDir, `${name}-${stem}.json`))
  return stemBundleFiles(stem).concat(names, [runIdPinPath(stem)])
}

const argv = process.argv.slice(2)

const stem = argv[0]
const wantArtifacts = argv.includes('--artifacts')
const bundleOnly = argv.includes('--bundle-only')
const allowedFlags = new Set(['--artifacts', '--bundle-only'])
const unknown = argv.slice(1).find((arg) => arg.startsWith('-') && !allowedFlags.has(arg))
if (unknown) {
  console.error(`unknown argument ${unknown}\n${USAGE}`)
  process.exit(1)
}
if (wantArtifacts && bundleOnly) {
  console.error(`--artifacts and --bundle-only are mutually exclusive\n${USAGE}`)
  process.exit(1)
}
try { assertTaskStem(stem) }
catch {
  console.error(USAGE)
  process.exit(1)
}

const cacheExists = cacheRootAvailable()
if (cacheExists) {
  for (const path of (bundleOnly ? stemBundleFiles(stem) : stemReportFiles(stem))) {
    removeRegularFile(path)
  }
  if (wantArtifacts) {
    const artifactsRoot = process.env.FIGMA_COMPARE_ARTIFACTS_DIR || figmaPath('.cache', 'artifacts', 'screenshot')
    const artifactStem = artifactSegment(stem)
    removeDirectoryTree(join(artifactsRoot, artifactStem), { count: 0 })
  }
}

console.log(`evidence-clean: ${stem} — removed ${removed.length} path(s)${wantArtifacts ? ' (incl. artifacts)' : ''}${bundleOnly ? ' (bundle only)' : ''}`)
for (const p of removed) console.log(`  - ${p}`)
for (const s of skipped) console.error(`  ! skipped ${s}`)
if (skipped.length) console.error('evidence-clean: cleanup incomplete; no skipped path is treated as removed')
process.exit(skipped.length ? 1 : 0)
