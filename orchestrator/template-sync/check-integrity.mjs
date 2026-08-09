#!/usr/bin/env node
// check-integrity.mjs — read-only vendored-copy integrity report.
//
// Reads the committed orchestrator/template-manifest.json (stamped at bootstrap /
// by sync-from-template.sh --apply; see _generate_template_manifest.py for the
// lifecycle rules) and reports, fully offline:
//   modified  — a template-owned file whose bytes differ from the stamp
//   missing   — a stamped file that no longer exists
//   extra     — a file the template never shipped (exclusion rules come FROM the
//               manifest, so per-product files are never flagged)
//   stamp age — how old the copy is (staleness needs no network to be useful)
// With TEMPLATE_ROOT set (an optional env var pointing at a live template
// checkout), it additionally diffs against the template's CURRENT state by
// invoking the template-side generator with --print (behind/local-only lists).
//
// An ABSENT manifest is an explicit `unstamped copy` verdict — never a guess,
// never a fallback. This tool never writes, never touches git.
//
// Exit codes: 0 clean · 1 findings (drift or unstamped) · 2 internal/usage error.

import { createHash } from 'node:crypto'
import { existsSync, lstatSync, readFileSync, readdirSync, realpathSync } from 'node:fs'
import { dirname, join, sep } from 'node:path'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'

const HERE = dirname(fileURLToPath(import.meta.url))
const ROOT = join(HERE, '..', '..')
const MANIFEST = join(ROOT, 'orchestrator', 'template-manifest.json')

// ── pure verdict logic (testable without a filesystem) ─────────────────────
// inputs: { manifest: {files, stampedAt, templateCommit} | null,
//           current: {relpath: sha256} | null,
//           templateFiles: {relpath: sha256} | null,  // live template, optional
//           nowMs: number }
// returns: [{ level: 'ok'|'info'|'warn', msg }]
export function templateIntegrityFindings({ manifest, current, templateFiles, nowMs }) {
  const findings = []
  if (!manifest) {
    findings.push({
      level: 'warn',
      msg: 'unstamped copy — orchestrator/template-manifest.json is absent; stamp it: python3 orchestrator/template-sync/_generate_template_manifest.py (bootstrap does this at launch Step 14)',
    })
    return findings
  }
  const stamped = manifest.files || {}
  const modified = [], missing = []
  for (const rel of Object.keys(stamped)) {
    if (!(rel in current)) missing.push(rel)
    else if (current[rel] !== stamped[rel]) modified.push(rel)
  }
  const extra = Object.keys(current).filter((rel) => !(rel in stamped))
  const list = (arr) => arr.slice(0, 15).join(', ') + (arr.length > 15 ? ` … (+${arr.length - 15} more)` : '')
  if (modified.length) findings.push({ level: 'warn', msg: `${modified.length} template-owned file(s) locally MODIFIED vs the stamp: ${list(modified)}` })
  if (missing.length) findings.push({ level: 'warn', msg: `${missing.length} stamped file(s) MISSING: ${list(missing)}` })
  if (extra.length) findings.push({ level: 'warn', msg: `${extra.length} file(s) the template never shipped (extra): ${list(extra)}` })
  const stampMs = Date.parse(manifest.stampedAt || '')
  if (!Number.isNaN(stampMs)) {
    const days = Math.floor((nowMs - stampMs) / 86400000)
    // stampCommit = HEAD of the tree the stamp was taken FROM: the template's
    // commit after a sync --apply, the product's own commit at bootstrap Step 14.
    findings.push({ level: 'info', msg: `stamped ${manifest.stampedAt} (${days} day(s) ago) @ stamp commit ${String(manifest.stampCommit || 'unknown').slice(0, 19)}` })
  } else {
    findings.push({ level: 'warn', msg: `manifest carries no parseable stampedAt (${JSON.stringify(manifest.stampedAt)})` })
  }
  if (templateFiles) {
    const behind = Object.keys(templateFiles).filter((rel) => stamped[rel] !== templateFiles[rel])
    const retired = Object.keys(stamped).filter((rel) => !(rel in templateFiles))
    if (behind.length) findings.push({ level: 'warn', msg: `copy is BEHIND the live template: ${behind.length} file(s) differ from ${'TEMPLATE_ROOT'}: ${list(behind)}` })
    if (retired.length) findings.push({ level: 'info', msg: `${retired.length} stamped file(s) no longer exist in the live template (retired upstream): ${list(retired)}` })
    if (!behind.length && !retired.length) findings.push({ level: 'ok', msg: 'copy matches the live template exactly' })
  }
  if (!modified.length && !missing.length && !extra.length) {
    findings.unshift({ level: 'ok', msg: `self-integrity clean: all ${Object.keys(stamped).length} stamped files match` })
  }
  return findings
}

// ── impure gather ───────────────────────────────────────────────────────────
function matchers(excludes) {
  const ex = excludes || { prefixes: [], exact: [], basenames: [], basenameSuffixes: [], basenamePrefixes: [] }
  const baseExcluded = (name) =>
    ex.basenames.includes(name) ||
    ex.basenameSuffixes.some((s) => name.endsWith(s)) ||
    ex.basenamePrefixes.some((p) => name.startsWith(p))
  const relExcluded = (rel) => ex.exact.includes(rel) || ex.prefixes.some((p) => rel.startsWith(p))
  return { baseExcluded, relExcluded }
}

function collectCurrent(root, excludes) {
  const { baseExcluded, relExcluded } = matchers(excludes)
  const out = {}
  const walk = (dir) => {
    for (const name of readdirSync(dir).sort()) {
      if (baseExcluded(name)) continue
      const full = join(dir, name)
      // Normalize the PLATFORM separator only — a bare backslash is a legal
      // filename character on POSIX and the python side preserves it.
      const rel = full.slice(root.length + 1).split(sep).join('/')
      const st = lstatSync(full)
      // Never follow or silently omit unsafe entries. A sentinel differs from
      // every sha256 in the stamp, so an expected path becomes MODIFIED and an
      // unexpected path becomes EXTRA without reading outside the copy.
      if (st.isSymbolicLink()) { out[rel] = 'unsafe:symlink'; continue }
      if (st.isDirectory()) {
        if (!relExcluded(rel + '/')) walk(full)
        continue
      }
      if (relExcluded(rel)) continue
      if (!st.isFile()) { out[rel] = 'unsafe:special'; continue }
      out[rel] = createHash('sha256').update(readFileSync(full)).digest('hex')
    }
  }
  const orchestratorRoot = join(root, 'orchestrator')
  const rootStat = lstatSync(orchestratorRoot)
  if (rootStat.isSymbolicLink() || !rootStat.isDirectory()) throw new Error('orchestrator root is not a real directory')
  walk(orchestratorRoot)
  return out
}

function liveTemplateFiles(templateRoot) {
  const gen = join(templateRoot, 'orchestrator', 'template-sync', '_generate_template_manifest.py')
  if (!existsSync(gen)) {
    return { error: `TEMPLATE_ROOT set but ${gen} does not exist` }
  }
  const r = spawnSync('python3', [gen, '--print', '--root', templateRoot], { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 })
  if (r.status !== 0) return { error: `template-side generator failed: ${r.error ? r.error.message : (r.stderr || r.stdout || `exit ${r.status}`)}` }
  try { return { files: JSON.parse(r.stdout).files || {} } } catch (e) { return { error: `template-side manifest unparseable: ${e.message}` } }
}

function main() {
  let manifest = null
  if (existsSync(MANIFEST)) {
    try { manifest = JSON.parse(readFileSync(MANIFEST, 'utf8')) } catch (e) {
      console.error(`check-integrity: manifest unreadable (${MANIFEST}): ${e.message}`)
      process.exit(2)
    }
  }
  let current = null
  if (manifest) {
    // An unreadable directory is an I/O error (exit 2), never a drift verdict —
    // and the python generator hard-fails on the same condition, so the two
    // sides stay consistent.
    try { current = collectCurrent(ROOT, manifest.excludes) } catch (e) {
      console.error(`check-integrity: cannot walk the tree: ${e.message}`)
      process.exit(2)
    }
  }
  let templateFiles = null
  if (manifest && process.env.TEMPLATE_ROOT) {
    const live = liveTemplateFiles(process.env.TEMPLATE_ROOT)
    if (live.error) { console.error(`check-integrity: ${live.error}`); process.exit(2) }
    templateFiles = live.files
  }
  const findings = templateIntegrityFindings({ manifest, current, templateFiles, nowMs: Date.now() })
  const mark = { ok: '  ok', info: 'info', warn: 'WARN' }
  for (const f of findings) console.log(`${mark[f.level] || f.level}: ${f.msg}`)
  process.exit(findings.some((f) => f.level === 'warn') ? 1 : 0)
}

// Import-as-module (tests) vs direct run. Realpath BOTH sides: node realpaths
// the ESM entry but argv[1] keeps the invoked spelling, so a symlinked checkout
// (or macOS /tmp → /private/tmp) would otherwise make the guard silently no-op —
// exit 0 with no output, indistinguishable from a clean check.
const directRun = (() => {
  if (!process.argv[1]) return false
  try { return realpathSync(process.argv[1]) === realpathSync(fileURLToPath(import.meta.url)) } catch { return false }
})()
if (directRun) main()
