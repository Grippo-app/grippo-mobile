#!/usr/bin/env node
// descope-task.mjs <stem> --reason "..." [--yes]
//
// The SANCTIONED de-scope path for a UI task whose Figma comparison is being deliberately
// dropped (design deleted upstream / flat-Figma reclassification / owner decision). Before
// this tool the flow was a multi-step hand-quest — rewrite every bullet to the audited none
// grammar, scrub every body citation, hand-`rm -rf` the screens cache — with conflicting
// backstop messages and ZERO audit trail (verify-done's de-UI net went silent forever).
//
// What it does (dry-run by default; NOTHING is written without --yes):
//   1. Rewrites every `## Design` bullet to `- <Name> — none (<reason>)` (kind tags kept;
//      already-`none` bullets kept as-is). The rewritten body is re-validated through
//      design-parser — a malformed result ABORTS before any write.
//   2. Lists residual body citations (bodyCitesFigmaNode / component snapshots). While any
//      remain it exits 2 — the operator resolves prose through an authorized canonical task
//      edit (auto-editing prose is not safe); bullets/receipt still land so progress persists.
//   3. Removes the screens cache dir, CONFINED via ensureContained (no free-form rm).
//   4. Writes a COMMITTED de-scope receipt under orchestrator/tasks/evidence/descope/
//      (`{stem, reason, at, by, designBulletsBefore, screensCacheRemoved}`)
//      — the deliberate act stays auditable after the ephemeral cache is gone.
//
// Retiring a component MAPPING is not this tool's job: that is a CAS Mapping
// Review operation (retire-mapping / add-disposition) with its own reason,
// actor, and audit trail.
//
// Never calls Figma (golden invariant). Fixture overrides:
//   FIGMA_TASKS_ROOT             — orchestrator/tasks root (task file + receipts)
//   FIGMA_SCREEN_CACHE_ROOT/FIGMA_SPEC_SCREENS_DIR — screens cache root

import { existsSync, mkdirSync, readFileSync, renameSync, rmSync, writeFileSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { spawnSync } from 'node:child_process'
import { createRequire } from 'node:module'
import { PROJECT_ROOT, displayPath, ensureContained, figmaScreensRoot, parseCli } from './_util.mjs'

const requireCjs = createRequire(import.meta.url)
const designParser = requireCjs('./design-parser.cjs')
const taskState = requireCjs('../../tasks/task-state-core.cjs')
const platformSupport = requireCjs('../../tasks/platform-support.cjs')

const USAGE = 'usage: node scripts/descope-task.mjs <stem> --reason "<why>" [--yes]'

let cli
try {
  cli = parseCli({
    allowedFlags: ['--reason', '--yes', '--lease-id', '--lease-token'],
    valueFlags: ['--reason', '--lease-id', '--lease-token'],
    booleanFlags: ['--yes'],
    usage: USAGE,
  })
} catch (e) {
  console.error(e.message)
  process.exit(1)
}
const stem = cli.positional[0] || ''
const yes = cli.has('--yes')
const reasonRaw = cli.value('--reason') || ''
// The audited-none grammar forbids ')' inside the reason and the bullet separator would
// mis-split — sanitize instead of failing on punctuation.
const reason = reasonRaw.replace(/\)/g, '').replace(/\(/g, '').replace(/ — /g, ', ').replace(/\s+/g, ' ').trim()
if (taskState.safeIntegerId(stem) === null || !reason) {
  console.error(USAGE)
  console.error('a non-empty --reason is required (it becomes the audited `none (<reason>)` opt-out)')
  process.exit(1)
}
try { platformSupport.assertCanonicalTaskPlatform() }
catch (error) {
  console.error(String(error && error.message || error))
  process.exit(Number.isInteger(error && error.exitCode) ? error.exitCode : 3)
}

const tasksRoot = process.env.FIGMA_TASKS_ROOT || join(PROJECT_ROOT, 'orchestrator', 'tasks')
const projectRoot = resolve(process.env.FIGMA_DESCOPE_PROJECT_ROOT || PROJECT_ROOT)
const taskStateEntry = ['todo', 'backlog'].map((state) => ({ state, file: join(tasksRoot, state, `${stem}.md`) })).find((entry) => existsSync(entry.file))
const taskFile = taskStateEntry && taskStateEntry.file
if (!taskFile) {
  console.error(`no task file for ${stem} under ${displayPath(tasksRoot)}/{todo,backlog}/ — de-scope applies to ACTIVE tasks (a done/ task is reopened first)`)
  process.exit(1)
}
const body = readFileSync(taskFile, 'utf8')
const validationStarted = process.hrtime.bigint()
let pre
try {
  pre = taskState.validateTaskState({
    tasksDir: tasksRoot,
    repoRoot: projectRoot,
    stem,
    expect: taskStateEntry.state,
    checkIndex: true,
    includeRuntime: false,
  })
} catch (error) {
  const rawCode = String(error && error.code || 'TASK_STATE_UNAVAILABLE')
  const code = /^[A-Za-z0-9_.:-]{1,80}$/.test(rawCode) ? rawCode : 'TASK_STATE_UNAVAILABLE'
  pre = {
    version: 1, ok: false, overallOk: false, scope: stem,
    action: 'descope',
    observedState: null, expectedState: taskStateEntry.state,
    snapshotHash: null, findings: [{ code, severity: 'blocker' }],
    stats: { scanMode: 'stem-closure', taskBodyReads: 0 },
  }
  process.stderr.write('[task-state] ' + JSON.stringify(taskState.observationFor({
    ...pre,
    action: 'descope',
    stats: { ...pre.stats, durationMs: Number((process.hrtime.bigint() - validationStarted) / 1000000n) },
  }, { caller: 'manual', slowThresholdMs: 100 })) + '\n')
  console.error('task-state precondition could not be evaluated safely')
  process.exit(Number.isInteger(error && error.exitCode) ? error.exitCode : 1)
}
process.stderr.write('[task-state] ' + JSON.stringify(taskState.observationFor({
  ...pre,
  action: 'descope',
  stats: { ...(pre.stats || {}), durationMs: Number((process.hrtime.bigint() - validationStarted) / 1000000n) },
}, { caller: 'manual', slowThresholdMs: 100 })) + '\n')
if (!pre.ok) {
  const codes = pre.findings.slice(0, 12).map((finding) => finding.code).join(', ')
  console.error(`task-state precondition failed for ${stem}${codes ? ` (${codes})` : ''} — repair the exact findings before de-scope`)
  process.exit(1)
}

// ── 1. Rewrite the ## Design section ─────────────────────────────────────────
const lines = body.split(/\r?\n/)
const headingIdx = lines.findIndex((l) => /^##\s+Design\s*$/.test(l.trim()) || /^##\s+Design\b/.test(l))
if (headingIdx < 0) {
  console.error(`task ${stem} has no ## Design section — nothing to de-scope`)
  process.exit(1)
}
let endIdx = lines.length
for (let i = headingIdx + 1; i < lines.length; i++) {
  if (/^##\s+/.test(lines[i])) { endIdx = i; break }
}
const SEPARATOR_RE = / — | - | -- /
const bulletsBefore = []
const rewritten = lines.slice()
let rewrote = 0
for (let i = headingIdx + 1; i < endIdx; i++) {
  const line = lines[i]
  const m = /^(\s*-\s*)(.*\S)\s*$/.exec(line)
  if (!m) continue
  const sep = SEPARATOR_RE.exec(m[2])
  // R2-3 gate bullet (the SEPARATOR-LESS `- gate: strict`) is NOT a design node — pass it
  // through verbatim. Rewriting it to `gate: strict — none (…)` would malform the section
  // (design-parser's gate grammar accepts exactly `strict`) and ABORT the de-scope: the
  // tasks that opted into the strict gate would be the only ones that could never take the
  // sanctioned de-scope path. Harmless to keep: with every node bullet at `none` there are
  // no rows for the strict routing to act on. A SCREEN merely named `Gate: …` carries a
  // separator and rewrites normally.
  if (!sep && /^gate\s*:/i.test(m[2])) continue
  bulletsBefore.push(line.trim())
  const screenPart = sep ? m[2].slice(0, sep.index).trim() : m[2].trim()
  const valuePart = sep ? m[2].slice(sep.index + sep[0].length).trim() : ''
  if (/^none\b/i.test(valuePart)) continue   // already an opt-out — keep the author's reason
  rewritten[i] = `${m[1]}${screenPart} — none (${reason})`
  rewrote++
}
if (!bulletsBefore.length) {
  console.error(`task ${stem}'s ## Design section carries no bullets — nothing to de-scope`)
  process.exit(1)
}
const newBody = rewritten.join('\n')

// Fail-closed self-check BEFORE any write: the rewritten section must parse clean, carry the
// audited opt-out, and leave nothing pullable.
if (designParser.hasMalformedDesign(newBody)) {
  console.error('ABORT: the rewritten ## Design section would be MALFORMED (design-parser rejects it) — nothing was written; check the reason text')
  process.exit(1)
}
if (designParser.hasPullableDesign(newBody)) {
  console.error('ABORT: the rewritten ## Design section still carries a pullable bullet — nothing was written')
  process.exit(1)
}

// ── 2. Residual body citations (never auto-edited) ──────────────────────────
const residual = []
if (designParser.bodyCitesFigmaNode(newBody)) residual.push('a valid Figma node URL remains in the task body (outside ## Design) — remove or de-link it through the canonical task edit flow')
if (designParser.hasComponentSnapshot(newBody)) residual.push('a machine component snapshot (`designComponentId:` + `figmaNodeId:`) remains in the task body — remove it through the canonical task edit flow')

// ── 3/4/5. Plan + execute ────────────────────────────────────────────────────
const screensDir = join(figmaScreensRoot(), stem)
const receiptsRoot = process.env.FIGMA_DESCOPE_RECEIPTS_DIR || join(tasksRoot, 'evidence', 'descope')
const receiptPath = join(receiptsRoot, `${stem}.json`)

console.log(`descope-task ${stem} ${yes ? '' : '(DRY-RUN — re-run with --yes to apply)'}`)
console.log(`  task file:        ${displayPath(taskFile)} — ${rewrote} bullet(s) → none (${reason}); ${bulletsBefore.length - rewrote} already none`)
console.log(`  screens cache:    ${existsSync(screensDir) ? displayPath(screensDir) + ' — will be removed' : '(absent — nothing to remove)'}`)
console.log(`  de-scope receipt: ${displayPath(receiptPath)}`)
for (const r of residual) console.log(`  RESIDUAL CITATION: ${r}`)

if (!yes) process.exit(residual.length ? 2 : 0)

const tasksRuntime = join(projectRoot, 'orchestrator', '.cache', 'tasks')
const finalizationsDir = resolve(process.env.ORCHESTRATOR_FINALIZATIONS_DIR || join(tasksRuntime, 'finalizations'))
const authorityEnv = {
  ...process.env,
  ORCHESTRATOR_PROJECT_ROOT: projectRoot,
  ORCHESTRATOR_TASKS_DIR: tasksRoot,
  ORCHESTRATOR_FINALIZATIONS_DIR: finalizationsDir,
  ORCHESTRATOR_WRITER_LEASES_DIR: resolve(process.env.ORCHESTRATOR_WRITER_LEASES_DIR || join(finalizationsDir, '.writers')),
  ORCHESTRATOR_TRANSITIONS_DIR: resolve(process.env.ORCHESTRATOR_TRANSITIONS_DIR || join(tasksRuntime, 'transitions')),
  FINALIZE_PROJECT_ROOT: projectRoot,
  FINALIZE_STATE_DIR: finalizationsDir,
}
const writerLeaseCli = join(PROJECT_ROOT, 'orchestrator', 'tasks', 'writer-lease.mjs')
const transitionCli = join(PROJECT_ROOT, 'orchestrator', 'tasks', 'transition-task-state.mjs')
const taskLockCli = join(PROJECT_ROOT, 'orchestrator', 'tasks', 'task-lock.mjs')

function bounded(value, max = 1000) {
  const text = String(value || '').replace(/[\0\r]+/g, ' ').trim()
  return text.length <= max ? text : text.slice(0, max - 1) + '…'
}

function acquireAuthority() {
  const explicitId = cli.value('--lease-id') || process.env.FIGMA_DESCOPE_LEASE_ID || ''
  const explicitToken = cli.value('--lease-token') || process.env.FIGMA_DESCOPE_LEASE_TOKEN || ''
  if ((explicitId && !explicitToken) || (!explicitId && explicitToken)) throw new Error('--lease-id and --lease-token must be supplied together')
  if (explicitId) return { args: ['--lease-id', explicitId, '--lease-token', explicitToken], owned: null, sessionId: null }
  if (process.env.ORCHESTRATOR_WRITER_SESSION_ID) return { args: [], owned: null, sessionId: process.env.ORCHESTRATOR_WRITER_SESSION_ID }

  mkdirSync(finalizationsDir, { recursive: true })
  const acquired = spawnSync(process.execPath, [writerLeaseCli, 'acquire',
    '--kind', 'task-session', '--stem', stem, '--key', `task:${stem}`,
    '--owner-pid', String(process.pid), '--ttl-ms', '900000', '--guard-finalization'], {
    cwd: projectRoot, env: authorityEnv, encoding: 'utf8', timeout: 15000,
  })
  if (acquired.status !== 0) throw new Error(`writer lease refused: ${bounded(acquired.stderr || acquired.stdout)}`)
  let value
  try { value = JSON.parse(acquired.stdout) } catch (error) { throw new Error(`writer lease returned invalid JSON: ${error.message}`) }
  if (!value || !/^wr-/.test(value.leaseId || '') || !/^[a-f0-9]{32,128}$/.test(value.token || '')) throw new Error('writer lease returned an invalid authority receipt')
  return {
    args: ['--lease-id', value.leaseId, '--lease-token', value.token],
    owned: { leaseId: value.leaseId, token: value.token },
    sessionId: value.sessionId,
  }
}

function acquireTaskLock(authority) {
  // An inherited/explicit authority must already own the canonical lock. Only
  // the standalone lease created above is paired with a short-lived exact lock
  // whose session is known here and can therefore be released ownership-safely.
  if (!authority || !authority.owned) return null
  if (!/^ws-[A-Za-z0-9][A-Za-z0-9._-]{15,159}$/.test(String(authority.sessionId || ''))) {
    throw new Error('writer lease returned no canonical session id for task-lock binding')
  }
  const stage = taskStateEntry.state === 'todo' ? 'orchestrator' : 'task-prep'
  const acquired = spawnSync(process.execPath, [taskLockCli, 'acquire',
    '--stem', stem, '--stage', stage, '--session-id', authority.sessionId,
    '--owner-kind', 'direct', '--owner-id', `descope-task:${authority.sessionId}`,
    '--owner-pid', String(process.pid)], {
    cwd: projectRoot, env: authorityEnv, encoding: 'utf8', timeout: 30000,
    maxBuffer: 2 * 1024 * 1024,
  })
  if (acquired.status !== 0) throw new Error(`task lock refused: ${bounded(acquired.stderr || acquired.stdout)}`)
  let value
  try { value = JSON.parse(acquired.stdout) } catch (error) { throw new Error(`task lock returned invalid JSON: ${error.message}`) }
  if (!value || value.ok !== true || value.stem !== stem || value.stage !== stage ||
      value.sessionId !== authority.sessionId || !/^sha256:[a-f0-9]{64}$/.test(String(value.lockHash || '')) ||
      typeof value.runId !== 'string') throw new Error('task lock returned an invalid ownership receipt')
  return { runId: value.runId, sessionId: value.sessionId, lockHash: value.lockHash }
}

function releaseTaskLock(lock, finalReceipt) {
  if (!lock) return
  if (!finalReceipt || !['backlog', 'pending', 'todo'].includes(finalReceipt.state || finalReceipt.observedState) ||
      !/^sha256:[a-f0-9]{64}$/.test(String(finalReceipt.sourceRevision || ''))) {
    console.error('WARNING: de-scope task-lock release skipped: no fresh final task-state receipt')
    return
  }
  const released = spawnSync(process.execPath, [taskLockCli, 'release',
    '--stem', stem, '--run-id', lock.runId, '--session-id', lock.sessionId,
    '--expected-hash', lock.lockHash,
    '--expected-state', finalReceipt.state || finalReceipt.observedState,
    '--source-revision', finalReceipt.sourceRevision], {
    cwd: projectRoot, env: authorityEnv, encoding: 'utf8', timeout: 30000,
    maxBuffer: 2 * 1024 * 1024,
  })
  if (released.status !== 0) console.error(`WARNING: de-scope task-lock release failed: ${bounded(released.stderr || released.stdout)}`)
}

function releaseAuthority(authority) {
  if (!authority || !authority.owned) return
  const released = spawnSync(process.execPath, [writerLeaseCli, 'release',
    '--lease-id', authority.owned.leaseId, '--token', authority.owned.token], {
    cwd: projectRoot, env: authorityEnv, encoding: 'utf8', timeout: 15000,
  })
  if (released.status !== 0) console.error(`WARNING: de-scope writer lease release failed: ${bounded(released.stderr || released.stdout)}`)
}

function applyDescope() {
  let authority
  let taskLock
  let finalStateReceipt = pre
  try {
    authority = acquireAuthority()
    taskLock = acquireTaskLock(authority)
    const edited = spawnSync(process.execPath, [transitionCli, 'edit', '--stem', stem,
      '--input', '-', '--source-revision', pre.sourceRevision, ...authority.args], {
      cwd: projectRoot, env: authorityEnv, input: newBody, encoding: 'utf8', timeout: 60000,
      maxBuffer: 2 * 1024 * 1024,
    })
    if (edited.status !== 0) throw new Error(`canonical task edit failed: ${bounded(edited.stderr || edited.stdout)}`)
    let editReceipt
    try { editReceipt = JSON.parse(edited.stdout) } catch (error) { throw new Error(`canonical task edit returned invalid JSON: ${error.message}`) }
    if (!editReceipt || editReceipt.ok !== true || editReceipt.operation !== 'edit' || editReceipt.stem !== stem || editReceipt.state !== taskStateEntry.state) {
      throw new Error('canonical task edit returned an invalid success receipt')
    }
    finalStateReceipt = editReceipt
    console.log(`wrote ${displayPath(taskFile)} through canonical task-state transaction`)

    let screensCacheRemoved = false
    if (!residual.length && existsSync(screensDir)) {
      const safe = ensureContained(figmaScreensRoot(), screensDir)   // throws on traversal — no free-form rm
      rmSync(safe, { recursive: true, force: true })
      screensCacheRemoved = true
      console.log(`removed ${displayPath(safe)}`)
    } else if (residual.length && existsSync(screensDir)) {
      console.log('screens cache KEPT: residual citations remain — the de-UI backstops stay armed until the body is clean; re-run after fixing')
    }

    mkdirSync(receiptsRoot, { recursive: true })
    const receipt = {
      schemaVersion: 1,
      stem,
      reason,
      at: new Date().toISOString(),
      by: process.env.USER || 'descope-task',
      sourceRevisionBefore: pre.sourceRevision,
      sourceRevisionAfter: editReceipt.sourceRevision,
      designBulletsBefore: bulletsBefore,
      screensCacheRemoved,
      residualCitations: residual,
    }
    const tmp = receiptPath + '.tmp'
    writeFileSync(tmp, JSON.stringify(receipt, null, 2) + '\n')
    renameSync(tmp, receiptPath)
    console.log(`wrote ${displayPath(receiptPath)}`)

    if (residual.length) {
      console.error(`\nde-scope INCOMPLETE: ${residual.length} residual citation(s) above must be removed through an authorized transition-task-state edit, then re-run with --yes`)
      return 2
    }
    console.log('\nde-scope complete: bullets audited-none, cache cleared, receipt committed')
    return 0
  } catch (error) {
    console.error(`de-scope ABORTED: ${bounded(error && error.message || error)}`)
    return 1
  } finally {
    releaseTaskLock(taskLock, finalStateReceipt)
    releaseAuthority(authority)
  }
}

process.exit(applyDescope())
