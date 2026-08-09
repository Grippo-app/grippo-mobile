#!/usr/bin/env node
// finalize-task.mjs — atomic, resumable todo -> done publication.
//
// The durable marker is the recovery authority. Every mutating phase records
// intent first, reconciles its effect idempotently, verifies the postcondition,
// and only then advances. The task lock is released after all committed and
// derived artifacts have been checked; the marker is removed last.

import { createHash, randomBytes } from 'node:crypto'
import { readdirSync, renameSync, writeFileSync } from 'node:fs'
import { spawn, spawnSync } from 'node:child_process'
import { hostname } from 'node:os'
import { basename, dirname, isAbsolute, join, posix, relative, resolve, sep } from 'node:path'
import { fileURLToPath } from 'node:url'
import {
  installOutcomeDraft,
  logicalTaskText,
  outcomeSectionLines,
  outcomeShapeError,
  parseFigmaEnabledConfig,
} from '../figma/scripts/outcome-shape.mjs'
import writerLeases from './writer-leases.cjs'
import tokenBindingContract from './token-binding-contract.cjs'
import componentBindingContract from './component-binding-contract.cjs'
import taskSourceContract from './task-source-contract.cjs'
import testSummaryContract from './task-test-summary-contract.cjs'
import testReceiptRegistry from './task-receipt-registry.cjs'
import testInputContract from './task-test-input-contract.cjs'
import testSnapshotContract from './content-snapshot.cjs'
import creationMarkerContract from './creation-marker-contract.cjs'
import editMarkerContract from './edit-marker-contract.cjs'
import taskState from './task-state-core.cjs'
import platformSupport from './platform-support.cjs'
import { validateCommittedTaskObservationReceipt } from '../figma/tokens/task-observation-receipt.mjs'
import { validateTaskIngestionIntent } from '../figma/tokens/task-ingestion-intent.mjs'
import tokenSourceHealthCore from '../figma/runtime/token-source-health-core.cjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const PROJECT_ROOT = resolve(process.env.FINALIZE_PROJECT_ROOT || join(HERE, '..', '..'))
const TASKS_DIR = join(PROJECT_ROOT, 'orchestrator', 'tasks')
const CACHE_DIR = resolve(process.env.FINALIZE_STATE_DIR || join(PROJECT_ROOT, 'orchestrator', '.cache', 'tasks', 'finalizations'))
const LOCKS_DIR = resolve(process.env.FINALIZE_LOCKS_DIR || join(PROJECT_ROOT, 'orchestrator', '.cache', 'tasks', 'locks'))
const CREATIONS_DIR = resolve(process.env.FINALIZE_CREATIONS_DIR || join(dirname(CACHE_DIR), 'creations'))
const TEST_CERTIFICATION_DIR = resolve(process.env.ORCHESTRATOR_TEST_CERTIFICATION_DIR || join(dirname(CACHE_DIR), 'test-certification'))
const EDITS_DIR = resolve(process.env.FINALIZE_EDITS_DIR || join(dirname(CACHE_DIR), 'edits'))
const MUTEX_PATH = join(CACHE_DIR, '.mutex.json')
const WRITER_LEASES_DIR = join(CACHE_DIR, '.writers')
const PHASES = ['outcome', 'components', 'tokens', 'ship', 'index', 'arch', 'verify', 'unlock', 'cleanup']
const STEM_RE = taskState.STEM_RE
const STEM_MAX = taskState.STEM_MAX
const HASH_RE = /^sha256:[a-f0-9]{64}$/
const RUN_ID_RE = /^[A-Za-z0-9][A-Za-z0-9._-]{0,159}$/
const TEST_RUN_ID_RE = /^run-[A-Za-z0-9][A-Za-z0-9-]{0,79}$/
const TEST_SESSION_ID_RE = /^[A-Za-z0-9][A-Za-z0-9._:-]{0,162}$/
const TEST_BINDING_KEYS = Object.freeze([
  'testLockHash', 'testPolicyHash', 'testRunId', 'testSessionId',
  'testSourceSnapshotHash', 'testSummaryHash', 'testTaskInputHash',
])
const MARKER_MAX_BYTES = 256 * 1024
const CREATION_MARKER_MAX_BYTES = creationMarkerContract.MAX_BYTES
const TASK_MAX_BYTES = 8 * 1024 * 1024
const MAX_RUNTIME_ENTRIES = 10000
const FS_BOUNDARY = join(HERE, 'finalize-lock.py')
const CANONICAL_PROJECT_ROOT = resolve(HERE, '..', '..')
const host = hostname()
let tempCounter = 0
let JSON_MODE = false
let ACTIVE_MUTEX_LEASE = null
let outcomeFixtureMutated = false
const MARKER_PROOFS = new WeakMap()

class FinalizeError extends Error {
  constructor(code, message, exitCode = 1) {
    super(message)
    this.name = 'FinalizeError'
    this.code = code
    this.exitCode = exitCode
  }
}

function fail(code, message, exitCode = 1) { throw new FinalizeError(code, message, exitCode) }
function now() { return new Date().toISOString() }
function sha256(value) { return `sha256:${createHash('sha256').update(value).digest('hex')}` }
function bounded(value, n = 1200) { const s = String(value == null ? '' : value); return s.length <= n ? s : `${s.slice(0, n - 1)}…` }
function normalizeFinalizeError(error) {
  if (error instanceof FinalizeError) return error
  const boundaryCode = String(error && error.code || '')
  const changed = new Set(['ENTRY_CHANGED', 'DIRECTORY_CHANGED'])
  const unsafe = new Set([
    'ENTRY_UNSAFE', 'DIRECTORY_UNSAFE', 'PATH_OUTSIDE_AUTHORITY', 'PATH_UNSAFE',
    'ARGUMENT_INVALID', 'TEST_HOOK_INVALID', 'NO_CLOBBER_UNSUPPORTED',
  ])
  let code = null, exitCode = 1
  if (changed.has(boundaryCode)) { code = 'FINALIZATION_FILESYSTEM_CHANGED'; exitCode = 4 }
  else if (boundaryCode === 'PATH_MISSING') { code = 'FINALIZATION_PATH_DISAPPEARED'; exitCode = 4 }
  else if (boundaryCode === 'EEXIST' || boundaryCode === 'TARGET_EXISTS') { code = 'FINALIZATION_TARGET_CONFLICT'; exitCode = 4 }
  else if (boundaryCode === 'RECOVERY_REQUIRED') { code = 'FINALIZATION_FILESYSTEM_RECOVERY_REQUIRED'; exitCode = 4 }
  else if (boundaryCode === 'ENTRY_TOO_LARGE' || boundaryCode === 'DIRECTORY_TOO_LARGE') code = 'FINALIZATION_FILESYSTEM_BOUND_EXCEEDED'
  else if (unsafe.has(boundaryCode)) code = 'FINALIZATION_FILESYSTEM_UNSAFE'
  if (!code) return error
  return new FinalizeError(code, `${bounded(error && error.message || error, 900)} [filesystem boundary: ${boundaryCode}]`, exitCode)
}
function emitValidationEvent(_kind, options, started, result = null, thrown = null) {
  const durationMs = Number((process.hrtime.bigint() - started) / 1000000n)
  const rawCode = String(thrown && thrown.code || 'TASK_STATE_UNAVAILABLE')
  const code = /^[A-Za-z0-9_.:-]{1,80}$/.test(rawCode) ? rawCode : 'TASK_STATE_UNAVAILABLE'
  const base = result || {
    version: 1, ok: false, overallOk: false,
    findings: [{ code, severity: 'blocker' }],
    stats: { scanMode: null, taskBodyReads: 0 },
  }
  const measured = {
    ...base,
    scope: base.scope || options && options.stem || null,
    action: base.action || options && options.action || 'finalize',
    transition: base.transition || options && options.transition || null,
    phase: base.phase || options && options.phase || null,
    expectedState: base.expectedState || options && options.expect || null,
    stats: { ...(base.stats || {}), durationMs },
  }
  process.stderr.write('[task-state] ' + JSON.stringify(taskState.observationFor(measured, {
    caller: 'finalizer', slowThresholdMs: 100,
  })) + '\n')
}
function markerPath(stem) { return join(CACHE_DIR, `${stem}.json`) }
function snapshotPath(stem, snapshotHash) {
  if (!HASH_RE.test(String(snapshotHash || ''))) fail('MARKER_INVALID', 'snapshot hash is invalid')
  return join(CACHE_DIR, `${stem}.${snapshotHash.slice('sha256:'.length)}.outcome.md`)
}
function lockPath(stem) { return join(LOCKS_DIR, `${stem}.json`) }

function canonicalFailure(code, result) {
  const summary = (result.findings || []).filter((item) => item.severity === 'blocker' || item.severity === 'error')
    .slice(0, 8).map((item) => `${item.code}${item.paths && item.paths.length ? ` (${item.paths.join(', ')})` : ''}`).join('; ')
  fail(code, `canonical task-state validation failed${summary ? `: ${summary}` : ''}`)
}
function canonicalValidate(stem, options = {}) {
  const started = process.hrtime.bigint()
  let result
  try {
    result = taskState.validateTaskState({
      tasksDir: TASKS_DIR,
      repoRoot: PROJECT_ROOT,
      stem,
      includeRuntime: false,
      ...options,
    })
  } catch (error) {
    emitValidationEvent('state', { ...options, stem }, started, result, error)
    fail(error instanceof taskState.SnapshotRaceError ? 'TASK_STATE_SNAPSHOT_RACE' : 'TASK_STATE_CONTRACT_UNREADABLE', error.message, error.exitCode || 1)
  }
  emitValidationEvent('state', { ...options, stem }, started, result)
  if (!result.ok) canonicalFailure('TASK_STATE_INVALID', result)
  return result
}
function canonicalFinalizePre(stem) {
  const started = process.hrtime.bigint()
  let admission
  try {
    admission = taskState.validateAction({ tasksDir: TASKS_DIR, repoRoot: PROJECT_ROOT, stem, action: 'finalize', checkIndex: true, includeRuntime: false })
  } catch (error) {
    emitValidationEvent('action', { stem, action: 'finalize', phase: 'pre', transition: 'todo:done' }, started, admission, error)
    fail(error instanceof taskState.SnapshotRaceError ? 'TASK_STATE_SNAPSHOT_RACE' : 'TASK_STATE_CONTRACT_UNREADABLE', error.message, error.exitCode || 1)
  }
  emitValidationEvent('action', { stem, action: 'finalize', phase: 'pre', transition: 'todo:done' }, started, admission)
  if (!admission.ok) canonicalFailure('FINALIZE_PRECONDITION_FAILED', admission)
  return canonicalValidate(stem, { transition: 'todo:done', phase: 'pre', checkIndex: true })
}
function canonicalFinalizePost(stem, checkIndex) {
  return canonicalValidate(stem, { transition: 'todo:done', phase: 'post', checkIndex: !!checkIndex })
}
function canonicalDoneCandidate(stem, bytes) {
  const started = process.hrtime.bigint()
  let result
  try {
    result = taskState.validateTaskState({
      tasksDir: TASKS_DIR,
      repoRoot: PROJECT_ROOT,
      stem,
      transition: 'todo:done',
      phase: 'post',
      checkIndex: false,
      includeRuntime: false,
      proposal: { stem, fromState: 'todo', state: 'done', bytes: Buffer.from(bytes) },
    })
  } catch (error) {
    emitValidationEvent('proposal', { stem, action: 'finalize', phase: 'post', transition: 'todo:done' }, started, result, error)
    fail(error instanceof taskState.SnapshotRaceError ? 'TASK_STATE_SNAPSHOT_RACE' : 'OUTCOME_CONTRACT_INVALID', error.message, error.exitCode || 1)
  }
  emitValidationEvent('proposal', { stem, action: 'finalize', phase: 'post', transition: 'todo:done' }, started, result)
  if (!result.ok) canonicalFailure('OUTCOME_CANONICAL_INVALID', result)
  return result
}
function unlockProofPath(marker) { return join(LOCKS_DIR, `.finalize-${marker.stem}-${marker.transactionId}.unlock.json`) }
function unlockSourcePath(marker) {
  if (!/^[a-f0-9]{32}$/.test(String(marker.artifacts && marker.artifacts.unlockSourceToken || ''))) fail('MARKER_INVALID', 'marker unlock source token is invalid')
  return join(LOCKS_DIR, `.finalize-${marker.stem}-${marker.transactionId}-${marker.artifacts.unlockSourceToken}.unlock-source`)
}
function shipPublicationPath(marker) { return join(TASKS_DIR, 'todo', `.finalize-${marker.stem}-${marker.transactionId}.ship`) }
function shipDetachPath(marker) { return join(TASKS_DIR, 'todo', `.finalize-${marker.stem}-${marker.transactionId}.detach.md`) }
function shipReceiptStagePath(marker) { return join(CACHE_DIR, `.finalize-${marker.stem}-${marker.transactionId}.receipts`) }
function outcomeSourcePath(marker) { return join(TASKS_DIR, 'todo', `.finalize-${marker.stem}-${marker.transactionId}.outcome-source.md`) }
function outcomePublicationPath(marker) { return join(TASKS_DIR, 'todo', `.finalize-${marker.stem}-${marker.transactionId}.outcome-publication.md`) }
function outcomeDetachPath(marker) {
  const token = marker.artifacts && marker.artifacts.outcomeDetachToken
  if (!/^[a-f0-9]{32}$/.test(String(token || ''))) fail('MARKER_INVALID', 'marker Outcome detach token is invalid')
  return join(TASKS_DIR, 'todo', `.finalize-${marker.stem}-${marker.transactionId}.${token}.outcome-detach.md`)
}

function validateStem(raw) {
  const stem = String(raw || '')
  if (taskState.safeIntegerId(stem) === null) {
    fail('BAD_STEM', `stem must match ${STEM_RE} and be at most ${STEM_MAX} characters; got ${JSON.stringify(raw)}`)
  }
  return stem
}

function assertInsideProject(target, label = 'path') {
  const rel = relative(PROJECT_ROOT, resolve(target))
  if (rel === '..' || rel.startsWith(`..${sep}`) || isAbsolute(rel)) {
    fail('PATH_OUTSIDE_PROJECT', `${label} escapes the project root`)
  }
  return rel
}
function boundary(action, values = {}) {
  const request = {
    version: 1,
    action,
    authorityRoot: PROJECT_ROOT,
    canonicalRoot: CANONICAL_PROJECT_ROOT,
    fixture: PROJECT_ROOT !== CANONICAL_PROJECT_ROOT,
    ...values,
  }
  const result = spawnSync(process.env.FINALIZE_FS_PYTHON || 'python3', [FS_BOUNDARY, 'fs-op'], {
    cwd: PROJECT_ROOT,
    env: process.env,
    input: JSON.stringify(request),
    encoding: 'utf8',
    maxBuffer: 48 * 1024 * 1024,
    timeout: 30000,
  })
  if (result.error) fail('FINALIZATION_FILESYSTEM_BOUNDARY_FAILED', result.error.message)
  if (result.status === 88 && PROJECT_ROOT !== CANONICAL_PROJECT_ROOT &&
      process.env.FINALIZE_FS_TEST_CRASH_STAGE && process.env.FINALIZE_FS_TEST_CRASH_TARGET &&
      process.env.FINALIZE_FS_TEST_CRASH_SENTINEL && process.env.FINALIZE_FS_TEST_ROOT) {
    process.exit(88)
  }
  if (result.status !== 0) fail('FINALIZATION_FILESYSTEM_BOUNDARY_FAILED', bounded(result.stderr || 'filesystem worker failed', 500))
  let envelope
  try { envelope = JSON.parse(result.stdout) } catch { fail('FINALIZATION_FILESYSTEM_BOUNDARY_FAILED', 'filesystem worker returned an invalid envelope') }
  if (!envelope || envelope.version !== 1 || envelope.ok !== true) {
    const detail = envelope && envelope.error || {}
    const error = new Error(String(detail.message || 'filesystem boundary rejected the operation'))
    error.code = detail.code === 'TARGET_EXISTS' ? 'EEXIST' : String(detail.code || 'FINALIZATION_FILESYSTEM_BOUNDARY_FAILED')
    throw error
  }
  return envelope.result
}
function entryStat(path, allowMissing = true) {
  assertInsideProject(path, 'filesystem entry')
  return boundary('stat', { path, allowMissing })
}
function realDirectoryTree(directory, { create = false, required = false, label = 'directory' } = {}) {
  assertInsideProject(directory, label)
  try {
    const result = create ? boundary('ensure-dir', { path: directory }) : entryStat(directory, true)
    if (result.missing) {
      if (required) fail('DIRECTORY_MISSING', `${label} is missing`)
      return null
    }
    const st = result.stat
    if (st.kind !== 'directory') fail('DIRECTORY_UNSAFE', `${label} contains a symlink or non-directory component`)
    return st
  } catch (error) {
    if (error instanceof FinalizeError) throw error
    fail(error.code === 'DIRECTORY_CHANGED' ? 'FINALIZATION_DIRECTORY_CHANGED' : 'FINALIZATION_DIRECTORY_UNSAFE', `${label} cannot be inspected safely: ${error.message}`, error.code === 'DIRECTORY_CHANGED' ? 4 : 1)
  }
}
function sameDirectorySnapshot(left, right) {
  return left && right && left.dev === right.dev && left.ino === right.ino && left.mode === right.mode &&
    left.size === right.size && left.mtimeNs === right.mtimeNs && left.ctimeNs === right.ctimeNs
}
function boundedDirectoryNames(directory, label, { missing = 'error' } = {}) {
  try {
    const result = boundary('list', { path: directory, maxEntries: MAX_RUNTIME_ENTRIES, allowMissing: missing === 'empty' })
    return result.missing ? [] : result.names
  } catch (error) {
    if (error.code === 'DIRECTORY_TOO_LARGE') fail('FINALIZATION_DIRECTORY_TOO_LARGE', `${label} exceeds ${MAX_RUNTIME_ENTRIES} entries`)
    fail(error.code === 'DIRECTORY_CHANGED' ? 'FINALIZATION_DIRECTORY_CHANGED' : 'FINALIZATION_DIRECTORY_UNSAFE', `${label} cannot be enumerated safely: ${error.message}`, error.code === 'DIRECTORY_CHANGED' ? 4 : 1)
  }
}
function ensureDir(directory) { return realDirectoryTree(directory, { create: true, required: true, label: 'runtime directory' }) }
function reconcileReplaceDirectory(directory, maxBytes, code = 'FINALIZATION_REPLACE_RECOVERY_REQUIRED') {
  ensureDir(directory)
  try {
    return boundary('recover-replaces', {
      path: directory,
      maxEntries: MAX_RUNTIME_ENTRIES,
      maxBytes,
    })
  } catch (error) {
    fail(code, `durable replacement requires exact recovery: ${bounded(error.message || error, 500)}`, 4)
  }
}
function syncDir(path) {
  try { boundary('fsync-dir', { path }) } catch { /* directory fsync is best effort */ }
}
function atomicWrite(path, bytes, mode = 0o600) {
  ensureDir(dirname(path))
  const observed = entryStat(path, true)
  return boundary('replace', {
    path,
    expected: observed.missing ? null : observed.stat,
    rawBase64: Buffer.from(bytes).toString('base64'),
    mode,
    maxBytes: Math.max(MARKER_MAX_BYTES, TASK_MAX_BYTES, Buffer.byteLength(bytes)),
  }).stat
}
function publishImmutableSnapshot(path, bytes) {
  ensureDir(dirname(path))
  try { writeExclusiveFile(path, bytes, 0o600) }
  catch (error) {
    if (!error || error.code !== 'EEXIST') throw error
    const existing = readRegularSnapshot(path, { code: 'OUTCOME_SNAPSHOT_INVALID', maxBytes: TASK_MAX_BYTES })
    if (!existing.bytes.equals(Buffer.from(bytes))) {
      fail('OUTCOME_SNAPSHOT_CONFLICT', 'content-addressed Outcome snapshot path contains different bytes; the foreign generation was preserved')
    }
  }
  const proof = readIdentity(path, 'OUTCOME_SNAPSHOT_INVALID')
  if (proof.hash !== sha256(bytes)) fail('OUTCOME_SNAPSHOT_CONFLICT', 'published Outcome snapshot does not match its content address')
  return proof
}
function removeFileDurable(path, expected = null, code = 'FILE_CHANGED_BEFORE_DELETE') {
  const read = boundary('read', { path, maxBytes: 32 * 1024 * 1024, allowMissing: true })
  if (read.missing) return false
  if (expected) {
    const matches = expected.hash !== undefined
      ? sameOwnedFileGeneration(read.stat, expected)
      : sameExactStatProof(read.stat, expected)
    if (!matches) fail(code, `${path} changed before delete; the foreign replacement was preserved`)
  }
  // Pass the just-opened exact proof to the worker. A replacement after this
  // compare is caught again at its unlink boundary and is never deleted.
  boundary('remove', { path, expected: read.stat, maxBytes: 32 * 1024 * 1024 })
  return true
}
function sameExactStatProof(left, right) {
  return left && right && ['dev', 'ino', 'kind', 'mode', 'size', 'mtimeNs', 'ctimeNs'].every((field) => String(left[field]) === String(right[field])) &&
    (right.hash === undefined || left.hash === right.hash)
}
function writeExclusiveFile(path, bytes, mode = 0o600) {
  ensureDir(dirname(path))
  return boundary('write-exclusive', { path, rawBase64: Buffer.from(bytes).toString('base64'), mode }).stat
}
function inspectShipTempAliases(marker) {
  const publication = shipPublicationPath(marker)
  const dir = dirname(publication)
  const prefix = `${basename(publication)}.tmp.`
  const names = boundedDirectoryNames(dir, 'todo publication directory', { missing: 'empty' })
  return names.filter((name) => name.startsWith(prefix)).map((name) => {
    const path = join(dir, name)
    regularFile(path, { required: true, code: 'SHIP_TEMP_PROOF_INVALID', maxBytes: TASK_MAX_BYTES })
    return { path, proof: readIdentity(path, 'SHIP_TEMP_PROOF_INVALID') }
  })
}
function removeShipTempAliases(marker) {
  const rows = inspectShipTempAliases(marker)
  for (const row of rows) removeFileDurable(row.path, row.proof, 'SHIP_TEMP_PROOF_CHANGED')
}
function inspectReceiptStage(marker) {
  const stage = shipReceiptStagePath(marker)
  const entry = entryStat(stage, true)
  if (entry.missing) return null
  if (entry.stat.kind !== 'directory') fail('SHIP_RECEIPT_PROOF_INVALID', `${stage} must be a private transaction directory`)
  return stage
}
function removeTreeDurable(directory, depth = 0) {
  if (depth > 16) fail('PRIVATE_TREE_TOO_DEEP', `${directory} exceeds the private cleanup depth bound`)
  const listed = boundary('list', { path: directory, maxEntries: MAX_RUNTIME_ENTRIES, allowMissing: true })
  if (listed.missing) return
  for (const name of listed.names) {
    const child = join(directory, name)
    const entry = listed.entries[name]
    if (entry.kind === 'directory') removeTreeDurable(child, depth + 1)
    else if (entry.kind === 'file') removeFileDurable(child, entry, 'PRIVATE_TREE_CHANGED')
    else fail('PRIVATE_TREE_UNSAFE', `${child} is a symlink or special file`)
  }
  // Child deletion changes the directory generation metadata. Re-open the now
  // empty directory, freeze that exact post-cleanup generation, and require the
  // rmdir worker to remove only that generation.
  const empty = boundary('list', { path: directory, maxEntries: MAX_RUNTIME_ENTRIES })
  if (empty.names.length) fail('PRIVATE_TREE_CHANGED', `${directory} changed while its children were being removed`)
  boundary('remove-empty-dir', { path: directory, expected: empty.stat })
}
function removeReceiptStage(marker) {
  const stage = inspectReceiptStage(marker)
  if (!stage) return
  removeTreeDurable(stage)
}
function discardUnpublishedShipArtifacts(marker) {
  const state = taskArtifacts(marker.stem)
  if (state.columns.length !== 1 || !state.present.todo || state.present.done) {
    fail('SHIP_PROOF_CONFLICT', 'old ship proofs may be discarded only while the task exists exclusively in todo/')
  }
  if (regularFile(shipDetachPath(marker), { code: 'SHIP_DETACH_INVALID', maxBytes: TASK_MAX_BYTES })) {
    fail('SHIP_DETACH_CONFLICT', 'a private todo detachment proof exists without a published done task; refusing to discard it')
  }
  const publication = shipPublicationPath(marker)
  const candidate = regularFile(publication, { code: 'SHIP_PROOF_INVALID', maxBytes: TASK_MAX_BYTES })
  let candidateProof = null
  if (candidate) {
    const bytes = readRegular(publication, { code: 'SHIP_PROOF_INVALID', maxBytes: TASK_MAX_BYTES })
    if (sha256(logicalTaskText(bytes.toString('utf8'))) !== marker.source.intendedLogicalHash) {
      fail('SHIP_PROOF_CONFLICT', 'existing transaction publication proof does not belong to the previous finalization intent')
    }
    candidateProof = readIdentity(publication, 'SHIP_PROOF_INVALID')
    if (candidateProof.hash !== marker.source.intendedHash) fail('SHIP_PROOF_CONFLICT', 'transaction publication proof changed before cleanup validation')
  }
  // Validate every private artifact before deleting any of them, so an unsafe
  // replacement fails closed without leaving a half-discarded recovery set.
  const tempAliases = inspectShipTempAliases(marker)
  const receiptStage = inspectReceiptStage(marker)
  if (candidateProof) removeFileDurable(publication, candidateProof, 'SHIP_PROOF_CHANGED')
  for (const row of tempAliases) removeFileDurable(row.path, row.proof, 'SHIP_TEMP_PROOF_CHANGED')
  if (receiptStage) {
    removeTreeDurable(receiptStage)
  }
}
function removeTaskSnapshots(stem) {
  const names = boundedDirectoryNames(CACHE_DIR, 'finalization state directory', { missing: 'empty' })
  const prefix = `${stem}.`
  for (const name of names) {
    if (name.startsWith(prefix) && /^[A-Fa-f0-9]{64}\.outcome\.md$/.test(name.slice(prefix.length))) {
      const file = join(CACHE_DIR, name)
      const expectedHash = `sha256:${name.slice(prefix.length, -'.outcome.md'.length).toLowerCase()}`
      removeSnapshotFile(file, expectedHash)
    }
  }
}
function removeSnapshotFile(file, expectedHash) {
  const proof = readIdentity(file, 'OUTCOME_SNAPSHOT_INVALID')
  if (proof.hash !== expectedHash) fail('OUTCOME_SNAPSHOT_CONFLICT', `${file} no longer matches its content-addressed name; foreign bytes were preserved`)
  removeFileDurable(file, proof, 'OUTCOME_SNAPSHOT_CHANGED')
}
function regularFile(path, options = {}) {
  let entry
  try { entry = entryStat(path, true) }
  catch (e) { fail(options.code || 'UNSAFE_FILE', `${path} cannot be inspected safely: ${e.message}`) }
  if (entry.missing) {
    if (!options.required) return null
    fail(options.code || 'FILE_MISSING', `${path} is missing`)
  }
  const st = entry.stat
  if (st.kind !== 'file') fail(options.code || 'UNSAFE_FILE', `${path} must be a regular file, not a symlink or special file`)
  if (options.maxBytes && st.size > options.maxBytes) fail(options.code || 'FILE_TOO_LARGE', `${path} exceeds ${options.maxBytes} bytes`)
  return st
}
function readRegularSnapshot(path, options = {}) {
  try {
    const result = boundary('read', { path, maxBytes: options.maxBytes || TASK_MAX_BYTES })
    return { bytes: Buffer.from(result.rawBase64, 'base64'), stat: result.stat }
  } catch (error) {
    fail(options.code || 'UNSAFE_FILE', `${path} is unsafe, missing, oversized, or changed while being read: ${error.message}`,
      ['ENTRY_CHANGED', 'DIRECTORY_CHANGED'].includes(error.code) ? 4 : 1)
  }
}
function readRegular(path, options = {}) { return readRegularSnapshot(path, options).bytes }
function requireCurrentTokenSourceHealth(sourceIndex) {
  const healthRoot = join(PROJECT_ROOT, 'orchestrator', '.cache', 'figma', 'token-source-health')
  const pointerPath = join(healthRoot, 'current.json')
  const pointerBytes = readRegular(pointerPath, {
    code: 'TOKEN_SOURCE_HEALTH_UNAVAILABLE',
    maxBytes: 16 * 1024,
  })
  let pointer
  try { pointer = JSON.parse(pointerBytes.toString('utf8')) }
  catch (error) { fail('TOKEN_SOURCE_HEALTH_UNAVAILABLE', 'Source Health pointer is not valid JSON') }
  const pointerKeys = ['schemaVersion', 'snapshotFile', 'snapshotHash', 'healthRevision',
    'indexSemanticHash', 'updatedAt']
  if (!pointer || typeof pointer !== 'object' || Array.isArray(pointer) ||
      Object.keys(pointer).sort().join('\0') !== pointerKeys.sort().join('\0') ||
      pointer.schemaVersion !== 1 ||
      !/^health-[0-9]{1,16}-[a-f0-9]{64}\.json$/.test(String(pointer.snapshotFile || '')) ||
      !HASH_RE.test(String(pointer.snapshotHash || '')) ||
      !HASH_RE.test(String(pointer.indexSemanticHash || '')) ||
      !Number.isSafeInteger(pointer.healthRevision) || pointer.healthRevision < 0 ||
      pointer.snapshotFile !== `health-${pointer.healthRevision}-${String(pointer.snapshotHash).slice('sha256:'.length)}.json`) {
    fail('TOKEN_SOURCE_HEALTH_UNAVAILABLE', 'Source Health pointer fails the current contract')
  }
  try {
    if (new Date(pointer.updatedAt).toISOString() !== pointer.updatedAt) throw new Error('invalid')
  } catch (error) {
    fail('TOKEN_SOURCE_HEALTH_UNAVAILABLE', 'Source Health pointer timestamp is invalid')
  }
  const snapshotBytes = readRegular(join(healthRoot, 'snapshots', pointer.snapshotFile), {
    code: 'TOKEN_SOURCE_HEALTH_UNAVAILABLE',
    maxBytes: 16 * 1024 * 1024,
  })
  if (sha256(snapshotBytes) !== pointer.snapshotHash) {
    fail('TOKEN_SOURCE_HEALTH_UNAVAILABLE', 'Source Health snapshot hash does not match its pointer')
  }
  let wrapper
  try { wrapper = JSON.parse(snapshotBytes.toString('utf8')) }
  catch (error) { fail('TOKEN_SOURCE_HEALTH_UNAVAILABLE', 'Source Health snapshot is not valid JSON') }
  const wrapperKeys = ['schemaVersion', 'createdAt', 'index', 'shards']
  if (!wrapper || typeof wrapper !== 'object' || Array.isArray(wrapper) ||
      Object.keys(wrapper).sort().join('\0') !== wrapperKeys.sort().join('\0') ||
      wrapper.schemaVersion !== 1 || !wrapper.index || !Array.isArray(wrapper.shards) ||
      wrapper.index.healthRevision !== pointer.healthRevision ||
      wrapper.index.semanticHash !== pointer.indexSemanticHash) {
    fail('TOKEN_SOURCE_HEALTH_UNAVAILABLE', 'Source Health snapshot wrapper fails the current contract')
  }
  try {
    if (new Date(wrapper.createdAt).toISOString() !== wrapper.createdAt) throw new Error('invalid')
  } catch (error) {
    fail('TOKEN_SOURCE_HEALTH_UNAVAILABLE', 'Source Health snapshot timestamp is invalid')
  }
  const freshness = tokenSourceHealthCore.sourceFreshness({
    index: wrapper.index,
    shards: wrapper.shards,
  }, sourceIndex)
  if (freshness.state !== 'current') {
    fail(freshness.state === 'stale' ? 'TOKEN_SOURCE_REFRESH_FAILED' : 'TOKEN_SOURCE_HEALTH_UNAVAILABLE',
      'design-origin finalization requires current verified Source Health')
  }
}

const IDENTITY_PROOF_FIELDS = Object.freeze(['ctimeNs', 'dev', 'hash', 'ino', 'kind', 'mode', 'mtimeNs', 'size'])
function identityProof(snapshot) {
  return {
    hash: sha256(snapshot.bytes),
    dev: String(snapshot.stat.dev),
    ino: String(snapshot.stat.ino),
    kind: snapshot.stat.kind,
    mode: snapshot.stat.mode,
    size: snapshot.stat.size,
    mtimeNs: String(snapshot.stat.mtimeNs),
    ctimeNs: String(snapshot.stat.ctimeNs),
  }
}
function validIdentityProof(value) {
  const keys = value && typeof value === 'object' && !Array.isArray(value) ? Object.keys(value).sort() : []
  return keys.length === IDENTITY_PROOF_FIELDS.length && keys.every((key, index) => key === IDENTITY_PROOF_FIELDS[index]) &&
    HASH_RE.test(String(value.hash || '')) && value.kind === 'file' &&
    ['dev', 'ino'].every((key) => /^(?:0|[1-9][0-9]*)$/.test(String(value[key] || ''))) &&
    ['mtimeNs', 'ctimeNs'].every((key) => /^-?(?:0|[1-9][0-9]*)$/.test(String(value[key] || '')) && value[key] !== '-0') &&
    ['mode', 'size'].every((key) => Number.isSafeInteger(value[key]) && value[key] >= 0)
}
function sameIdentityProof(left, right) {
  const a = identityPart(left), b = identityPart(right)
  return validIdentityProof(a) && validIdentityProof(b) && IDENTITY_PROOF_FIELDS.every((key) => a[key] === b[key])
}
function sameOwnedFileGeneration(left, right) {
  const a = identityPart(left), b = identityPart(right)
  return validIdentityProof(a) && validIdentityProof(b) &&
    ['dev', 'ino', 'kind', 'mode', 'size', 'mtimeNs', 'hash'].every((key) => a[key] === b[key])
}
function identityPart(value) { return Object.fromEntries(IDENTITY_PROOF_FIELDS.map((key) => [key, value && value[key]])) }
function sameInode(left, right) { return left && right && left.dev === right.dev && left.ino === right.ino }
function readIdentity(path, code = 'OUTCOME_CAS_PROOF_INVALID') {
  return identityProof(readRegularSnapshot(path, { code, maxBytes: TASK_MAX_BYTES }))
}

function outcomeHash(text) {
  const value = String(text == null ? '' : text)
  const matches = [...value.matchAll(/^---\s*$/gm)]
  const at = matches.length ? matches[matches.length - 1].index : -1
  return sha256(at >= 0 ? value.slice(at) : '')
}

function taskArtifacts(stem) {
  const paths = {
    backlog: join(TASKS_DIR, 'backlog', `${stem}.md`),
    pending: join(TASKS_DIR, 'pending', `${stem}.questions.md`),
    todo: join(TASKS_DIR, 'todo', `${stem}.md`),
    done: join(TASKS_DIR, 'done', `${stem}.md`),
  }
  const present = {}
  for (const [column, path] of Object.entries(paths)) present[column] = !!regularFile(path, { code: 'UNSAFE_TASK_FILE' })
  return { paths, present, columns: Object.keys(present).filter((column) => present[column]) }
}
function requireOnlyColumn(stem, expected) {
  const state = taskArtifacts(stem)
  if (state.columns.length !== 1 || state.columns[0] !== expected) {
    fail('TASK_COLUMN_CONFLICT', `${stem} must exist only in ${expected}/; found ${state.columns.length ? state.columns.join(', ') : 'no task artifact'}`)
  }
  return state
}

function reconcileInterruptedShip(marker) {
  reconcileShipDetach(marker)
  const state = taskArtifacts(marker.stem)
  if (!(state.columns.length === 2 && state.present.todo && state.present.done)) return false
  const todoStat = regularFile(state.paths.todo, { required: true, code: 'UNSAFE_TASK_FILE', maxBytes: TASK_MAX_BYTES })
  const doneStat = regularFile(state.paths.done, { required: true, code: 'UNSAFE_TASK_FILE', maxBytes: TASK_MAX_BYTES })
  const done = readRegular(state.paths.done, { code: 'UNSAFE_TASK_FILE', maxBytes: TASK_MAX_BYTES }).toString('utf8')
  if (sha256(logicalTaskText(done)) !== marker.source.intendedLogicalHash) {
    fail('DONE_TASK_CHANGED', 'interrupted no-clobber publication does not match the finalization intent')
  }
  const sameTask = todoStat.dev === doneStat.dev && todoStat.ino === doneStat.ino
  const publication = regularFile(shipPublicationPath(marker), { code: 'SHIP_PROOF_INVALID', maxBytes: TASK_MAX_BYTES })
  const candidateOwnsDone = publication && publication.dev === doneStat.dev && publication.ino === doneStat.ino
  if (!sameTask && !candidateOwnsDone) {
    fail('TASK_COLUMN_CONFLICT', `${marker.stem} exists in todo/ and done/ as different files without the transaction publication proof; refusing to choose either`)
  }
  // Do not unlink todo here. ship-done owns a rename-to-private-proof detachment
  // that preserves any path replacement racing the move. This preflight only
  // proves that the interrupted two-column state belongs to this transaction.
  return true
}

function reconcileShipDetach(marker) {
  const detach = shipDetachPath(marker)
  const detached = regularFile(detach, { code: 'SHIP_DETACH_INVALID', maxBytes: TASK_MAX_BYTES })
  if (!detached) return false
  const state = taskArtifacts(marker.stem)
  if (!state.present.done) fail('SHIP_DETACH_INVALID', 'detached todo proof exists without a published done task')
  const done = regularFile(state.paths.done, { required: true, code: 'DONE_TASK_CHANGED', maxBytes: TASK_MAX_BYTES })
  const bytes = readRegular(detach, { code: 'SHIP_DETACH_INVALID', maxBytes: TASK_MAX_BYTES })
  const owned = sha256(bytes) === marker.source.intendedHash
  const doneBytes = readRegular(state.paths.done, { code: 'DONE_TASK_CHANGED', maxBytes: TASK_MAX_BYTES })
  const publication = regularFile(shipPublicationPath(marker), { code: 'SHIP_PROOF_INVALID', maxBytes: TASK_MAX_BYTES })
  const doneOwned = sha256(logicalTaskText(doneBytes.toString('utf8'))) === marker.source.intendedLogicalHash &&
    publication && publication.dev === done.dev && publication.ino === done.ino
  if (!owned || !doneOwned) {
    if (!state.present.todo) {
      const detachProof = readIdentity(detach, 'SHIP_DETACH_INVALID')
      try { boundary('link', { source: detach, target: state.paths.todo, expected: detachProof, maxBytes: TASK_MAX_BYTES }) } catch (e) {
        if (!e || e.code !== 'EEXIST') fail('SHIP_DETACH_CONFLICT', `could not restore concurrently replaced todo task from ${detach}: ${e.message}`)
      }
    }
    fail('SHIP_DETACH_CONFLICT', `${!owned ? 'todo task changed during publication' : 'published done task no longer belongs to the transaction'}; intended todo bytes are preserved at ${detach}`)
  }
  const detachProof = readIdentity(detach, 'SHIP_DETACH_INVALID')
  removeFileDurable(detach, detachProof, 'SHIP_DETACH_CHANGED')
  return true
}

function readFigmaConfig() {
  const path = join(PROJECT_ROOT, 'orchestrator', 'project-config.md')
  const bytes = readRegular(path, { code: 'PROJECT_CONFIG_INVALID', maxBytes: 1024 * 1024 })
  let enabled
  try { enabled = parseFigmaEnabledConfig(bytes.toString('utf8')) } catch (error) {
    fail('PROJECT_CONFIG_INVALID', error && error.message || 'project-config.md has an invalid figmaEnabled field')
  }
  return { enabled, hash: sha256(bytes) }
}
function readConfigBoolean(key) { return key === 'figmaEnabled' ? readFigmaConfig().enabled : false }
function verifyFigmaConfig(marker) {
  const live = readFigmaConfig()
  if (!marker.figma || typeof marker.figma.enabled !== 'boolean' || !HASH_RE.test(marker.figma.configHash)) fail('MARKER_INVALID', 'marker Figma applicability/config hash is missing')
  if (live.enabled !== marker.figma.enabled || live.hash !== marker.figma.configHash) {
    fail('PROJECT_CONFIG_CHANGED', `project-config.md changed during finalization (figmaEnabled ${marker.figma.enabled} -> ${live.enabled}); refusing to change gate applicability or its frozen configuration mid-transaction`)
  }
}

function currentRunId(stem) {
  const reports = resolve(process.env.FIGMA_REPORTS_DIR || join(PROJECT_ROOT, 'orchestrator', '.cache', 'figma', 'reports'))
  const pin = join(reports, `.run-id-${stem}`)
  const pinStat = regularFile(pin, { code: 'RUN_ID_PIN_INVALID', maxBytes: 512 })
  if (pinStat) {
    const value = readRegular(pin, { code: 'RUN_ID_PIN_INVALID', maxBytes: 512 }).toString('utf8').trim()
    if (RUN_ID_RE.test(value) && !value.includes('..')) return value
    fail('RUN_ID_PIN_INVALID', `malformed Figma run-id pin at ${pin}`)
  }
  // Generated independently from the finalization transaction id and then
  // persisted by the Figma driver through FIGMA_PIPELINE_RUN_ID.
  return `${new Date().toISOString().replace(/[-:.]/g, '')}-${stem}`.slice(0, 160)
}

function markerEtag(raw) { return sha256(raw) }
function ownerRecord(invocationId) {
  let processStartId
  try { processStartId = writerLeases.captureProcessStartId(process.pid) }
  catch (error) { fail('FINALIZATION_OWNER_IDENTITY_UNAVAILABLE', error.message) }
  if ((process.platform === 'linux' || process.platform === 'darwin' || process.platform === 'win32') &&
      !writerLeases.PROCESS_START_ID_RE.test(String(processStartId || ''))) {
    fail('FINALIZATION_OWNER_IDENTITY_UNAVAILABLE', 'exact finalization owner process generation is unavailable')
  }
  return { pid: process.pid, processStartId: processStartId || null, hostname: host, invocationId, startedAt: now() }
}
function validateMarkerShape(marker, expectedStem) {
  if (!marker || typeof marker !== 'object' || Array.isArray(marker)) fail('MARKER_INVALID', 'finalization marker must contain an object')
  if (marker.version !== 1) fail('MARKER_VERSION_UNSUPPORTED', `unsupported finalization marker version ${JSON.stringify(marker.version)}`)
  if (marker.stem !== expectedStem) fail('MARKER_STEM_MISMATCH', `marker stem ${JSON.stringify(marker.stem)} does not match ${expectedStem}`)
  if (!/^fin-[A-Za-z0-9._-]{1,160}$/.test(String(marker.transactionId || ''))) fail('MARKER_INVALID', 'marker transaction id is invalid')
  if (!Number.isInteger(marker.revision) || marker.revision < 1) fail('MARKER_INVALID', 'marker revision must be a positive integer')
  if (!['running', 'incomplete', 'completed'].includes(marker.status)) fail('MARKER_INVALID', `marker status ${JSON.stringify(marker.status)} is invalid`)
  if (!PHASES.includes(marker.phase)) fail('MARKER_INVALID', `marker phase ${JSON.stringify(marker.phase)} is invalid`)
  if (typeof marker.createdAt !== 'string' || !Number.isFinite(Date.parse(marker.createdAt)) ||
      typeof marker.updatedAt !== 'string' || !Number.isFinite(Date.parse(marker.updatedAt))) fail('MARKER_INVALID', 'marker timestamps are invalid')
  const validOwner = marker.owner === null || (marker.owner && Object.keys(marker.owner).sort().join(',') === 'hostname,invocationId,pid,processStartId,startedAt' &&
    Number.isInteger(marker.owner.pid) && marker.owner.pid > 0 &&
    typeof marker.owner.hostname === 'string' && marker.owner.hostname && typeof marker.owner.invocationId === 'string' && marker.owner.invocationId &&
    typeof marker.owner.startedAt === 'string' && Number.isFinite(Date.parse(marker.owner.startedAt)) &&
    (process.platform === 'linux' || process.platform === 'darwin' || process.platform === 'win32'
      ? writerLeases.PROCESS_START_ID_RE.test(String(marker.owner.processStartId || ''))
      : marker.owner.processStartId === null))
  if (!validOwner || (marker.status === 'running' && marker.owner === null)) fail('MARKER_INVALID', 'marker owner is invalid')
  if (!marker.source || !['originalHash', 'intendedHash', 'intendedLogicalHash', 'outcomeHash', 'snapshotHash', 'publishFromHash'].every((key) => HASH_RE.test(marker.source[key])) ||
      !marker.source.lock || marker.source.lock.present !== true ||
      !validIdentityProof(Object.fromEntries(IDENTITY_PROOF_FIELDS.map((key) => [key, marker.source.lock[key]]))) ||
      Object.keys(marker.source.lock).sort().join(',') !== ['present', ...IDENTITY_PROOF_FIELDS].sort().join(',')) {
    fail('MARKER_INVALID', 'marker source hashes/lock ownership are invalid')
  }
  if (!marker.phases || typeof marker.phases !== 'object') fail('MARKER_INVALID', 'marker phases are missing')
  if (!marker.figma || typeof marker.figma.enabled !== 'boolean' || !HASH_RE.test(marker.figma.configHash) ||
      (marker.figma.pipelineRunId !== null && typeof marker.figma.pipelineRunId !== 'string')) fail('MARKER_INVALID', 'marker Figma state is invalid')
  if (marker.figma.enabled && !RUN_ID_RE.test(String(marker.figma.pipelineRunId || ''))) fail('MARKER_INVALID', 'enabled Figma marker must carry a valid pinned pipeline run id')
  if (!marker.figma.enabled && marker.figma.pipelineRunId !== null) fail('MARKER_INVALID', 'disabled Figma marker must not carry a pipeline run id')
  for (const phase of PHASES) {
    const entry = marker.phases[phase]
    if (!entry || !['pending', 'running', 'failed', 'succeeded', 'skipped'].includes(entry.state) || !Number.isInteger(entry.attempts) || entry.attempts < 0) {
      fail('MARKER_INVALID', `marker phase state ${phase} is invalid`)
    }
  }
  if (marker.status === 'completed' && !PHASES.every((phase) => ['succeeded', 'skipped'].includes(marker.phases[phase].state))) {
    fail('MARKER_INVALID', 'completed marker does not have every phase succeeded/skipped')
  }
  if (marker.status === 'running' && marker.phases[marker.phase].state !== 'running') fail('MARKER_INVALID', 'running marker phase is not running')
  if (!marker.artifacts || typeof marker.artifacts !== 'object' || Array.isArray(marker.artifacts)) fail('MARKER_INVALID', 'marker artifacts state is invalid')
  const testBindingCount = TEST_BINDING_KEYS.filter((key) => marker.artifacts[key] !== undefined).length
  if (testBindingCount !== 0 && testBindingCount !== TEST_BINDING_KEYS.length) {
    fail('MARKER_INVALID', 'marker test evidence binding must carry the complete transaction identity')
  }
  if (testBindingCount &&
      (!TEST_BINDING_KEYS.filter((key) => key.endsWith('Hash')).every((key) => HASH_RE.test(String(marker.artifacts[key] || ''))) ||
       !TEST_RUN_ID_RE.test(String(marker.artifacts.testRunId || '')) ||
       !TEST_SESSION_ID_RE.test(String(marker.artifacts.testSessionId || '')))) {
    fail('MARKER_INVALID', 'marker test evidence binding is invalid')
  }
  if (marker.artifacts.unlockDetached !== undefined && marker.artifacts.unlockDetached !== true) fail('MARKER_INVALID', 'marker unlock proof state is invalid')
  if ((marker.artifacts.tokenMappingHash !== undefined) !== (marker.artifacts.tokenMappingRevision !== undefined)) {
    fail('MARKER_INVALID', 'marker token mapping publication state must carry hash and revision together')
  }
  if (marker.artifacts.tokenMappingHash !== undefined &&
      (!HASH_RE.test(String(marker.artifacts.tokenMappingHash)) ||
        !Number.isSafeInteger(marker.artifacts.tokenMappingRevision) || marker.artifacts.tokenMappingRevision < 1)) {
    fail('MARKER_INVALID', 'marker token mapping publication state is invalid')
  }
  if ((marker.artifacts.componentMappingHash !== undefined) !== (marker.artifacts.componentMappingRevision !== undefined)) {
    fail('MARKER_INVALID', 'marker component mapping publication state must carry hash and revision together')
  }
  if (marker.artifacts.componentMappingHash !== undefined &&
      (!HASH_RE.test(String(marker.artifacts.componentMappingHash)) ||
        !Number.isSafeInteger(marker.artifacts.componentMappingRevision) || marker.artifacts.componentMappingRevision < 1)) {
    fail('MARKER_INVALID', 'marker component mapping publication state is invalid')
  }
  const tokenReceiptKeys = ['tokenObservationManifestHash', 'tokenIngestionIntentId', 'tokenIngestionIntentHash']
  const tokenReceiptCount = tokenReceiptKeys.filter((key) => marker.artifacts[key] !== undefined).length
  if (tokenReceiptCount !== 0 && tokenReceiptCount !== tokenReceiptKeys.length) {
    fail('MARKER_INVALID', 'marker token receipt publication state must carry manifest, intent id, and intent hash together')
  }
  if (tokenReceiptCount &&
      (!HASH_RE.test(String(marker.artifacts.tokenObservationManifestHash || '')) ||
       !/^tokintent_[A-Za-z0-9_-]{16,96}$/.test(String(marker.artifacts.tokenIngestionIntentId || '')) ||
       !HASH_RE.test(String(marker.artifacts.tokenIngestionIntentHash || '')))) {
    fail('MARKER_INVALID', 'marker token receipt publication state is invalid')
  }
  if (marker.artifacts.unlockSourceToken !== undefined && !/^[a-f0-9]{32}$/.test(String(marker.artifacts.unlockSourceToken))) fail('MARKER_INVALID', 'marker unlock source token is invalid')
  if (marker.artifacts.outcomeDetachToken !== undefined && !/^[a-f0-9]{32}$/.test(String(marker.artifacts.outcomeDetachToken))) fail('MARKER_INVALID', 'marker Outcome detach token is invalid')
  for (const key of ['outcomeSourceProof', 'outcomePublicationProof', 'outcomeTargetProof']) {
    if (marker.artifacts[key] !== undefined && !validIdentityProof(marker.artifacts[key])) fail('MARKER_INVALID', `marker ${key} is invalid`)
  }
  if (['outcomeSourceProof', 'outcomePublicationProof', 'outcomeTargetProof'].some((key) => marker.artifacts[key] !== undefined) &&
      marker.artifacts.outcomeDetachToken === undefined) fail('MARKER_INVALID', 'marker Outcome proof is missing its detach token')
  if (marker.lastError !== null && (!marker.lastError || typeof marker.lastError !== 'object' || typeof marker.lastError.code !== 'string' ||
      typeof marker.lastError.message !== 'string' || typeof marker.lastError.at !== 'string' || !Number.isFinite(Date.parse(marker.lastError.at)))) {
    fail('MARKER_INVALID', 'marker lastError is invalid')
  }
  return marker
}
function readMarker(stem) {
  const path = markerPath(stem)
  const snapshot = readRegularSnapshot(path, { code: 'UNSAFE_MARKER', maxBytes: MARKER_MAX_BYTES })
  const raw = snapshot.bytes
  let marker
  try { marker = JSON.parse(raw.toString('utf8')) } catch (e) { fail('MARKER_CORRUPT', `finalization marker is not valid JSON: ${e.message}`) }
  validateMarkerShape(marker, stem)
  MARKER_PROOFS.set(marker, identityProof(snapshot))
  return { marker, raw, etag: markerEtag(raw) }
}
function writeMarker(marker) {
  if (ACTIVE_MUTEX_LEASE) assertMutexHeld(ACTIVE_MUTEX_LEASE)
  const expected = MARKER_PROOFS.get(marker)
  if (!expected) fail('MARKER_OWNERSHIP_UNPROVEN', 'cannot replace a finalization marker without its exact opened generation proof')
  marker.revision = Number(marker.revision || 0) + 1
  marker.updatedAt = now()
  validateMarkerShape(marker, marker.stem)
  const bytes = Buffer.from(`${JSON.stringify(marker, null, 2)}\n`)
  if (bytes.length > MARKER_MAX_BYTES) fail('MARKER_TOO_LARGE', `serialized finalization marker exceeds ${MARKER_MAX_BYTES} bytes`)
  const path = markerPath(marker.stem)
  boundary('replace', {
    path, expected, rawBase64: bytes.toString('base64'), mode: 0o600,
    maxBytes: MARKER_MAX_BYTES,
  })
  const published = readIdentity(path, 'MARKER_CHANGED')
  if (published.hash !== sha256(bytes)) fail('MARKER_CHANGED', 'published marker bytes do not match the intended recovery generation')
  MARKER_PROOFS.set(marker, published)
  return marker
}
function createMarkerExclusive(marker) {
  if (ACTIVE_MUTEX_LEASE) assertMutexHeld(ACTIVE_MUTEX_LEASE)
  marker.revision = Number(marker.revision || 0) + 1
  marker.updatedAt = now()
  validateMarkerShape(marker, marker.stem)
  const bytes = Buffer.from(`${JSON.stringify(marker, null, 2)}\n`)
  if (bytes.length > MARKER_MAX_BYTES) fail('MARKER_TOO_LARGE', `serialized finalization marker exceeds ${MARKER_MAX_BYTES} bytes`)
  const path = markerPath(marker.stem)
  ensureDir(dirname(path))
  const stalePrefix = `${basename(path)}.claim.`
  const staleClaims = boundedDirectoryNames(dirname(path), 'finalization state directory').filter((name) => name.startsWith(stalePrefix))
  const staleProofs = staleClaims.map((name) => {
    const file = join(dirname(path), name)
    regularFile(file, { required: true, code: 'MARKER_CLAIM_INVALID', maxBytes: MARKER_MAX_BYTES })
    return { file, proof: readIdentity(file, 'MARKER_CLAIM_INVALID') }
  })
  for (const row of staleProofs) removeFileDurable(row.file, row.proof, 'MARKER_CLAIM_CHANGED')
  const tmp = `${path}.claim.${process.pid}.${Date.now()}.${++tempCounter}.${randomBytes(4).toString('hex')}`
  let tmpProof = null
  try {
    writeExclusiveFile(tmp, bytes, 0o600)
    tmpProof = readIdentity(tmp, 'MARKER_CLAIM_INVALID')
    // Hard-link commit gives the first marker claimant O_EXCL semantics. A
    // mutex-helper death immediately before this point therefore cannot let a
    // second process overwrite the winner's recovery authority.
    try { boundary('link', { source: tmp, target: path, expected: tmpProof, maxBytes: MARKER_MAX_BYTES }) }
    catch (error) {
      if (error && error.code === 'EEXIST') throw error
      if (error && ['ENTRY_CHANGED', 'PATH_MISSING'].includes(error.code)) {
        fail('MARKER_CLAIM_CHANGED', 'private marker claim changed before no-clobber publication')
      }
      throw error
    }
  } catch (e) {
    if (e && e.code === 'EEXIST') fail('FINALIZATION_CLAIM_LOST', `a finalization marker for ${marker.stem} appeared while this invocation was claiming it`)
    throw e
  } finally {
    if (tmpProof) try { removeFileDurable(tmp, tmpProof, 'MARKER_CLAIM_CHANGED') } catch {}
  }
  const published = readIdentity(path, 'MARKER_CHANGED')
  if (published.hash !== sha256(bytes)) fail('MARKER_CHANGED', 'claimed marker bytes do not match the intended recovery generation')
  MARKER_PROOFS.set(marker, published)
  return marker
}
function removeOwnedMarker(marker) {
  const proof = MARKER_PROOFS.get(marker)
  if (!proof) fail('MARKER_OWNERSHIP_UNPROVEN', 'cannot remove a finalization marker without its exact owned generation proof')
  return removeFileDurable(markerPath(marker.stem), proof, 'MARKER_OWNERSHIP_CONFLICT')
}

function safeProjection(stem) {
  try {
    const { marker, etag } = readMarker(stem)
    return {
      version: marker.version, revision: marker.revision, etag, stem: marker.stem,
      status: marker.status, phase: marker.phase, createdAt: marker.createdAt,
      updatedAt: marker.updatedAt, lastError: marker.lastError || null,
    }
  } catch (e) {
    const error = normalizeFinalizeError(e)
    return { stem, status: 'corrupt', phase: null, errorCode: error.code || 'MARKER_CORRUPT', errorMessage: bounded(error.message, 500) }
  }
}
function listIncomplete() {
  const names = boundedDirectoryNames(CACHE_DIR, 'finalization state directory', { missing: 'empty' })
  const stems = new Set(), invalid = []
  for (const name of names) {
    if (name === '.mutex.json') continue
    const replacementStem = replacementArtifactStem(name)
    if (replacementStem) { stems.add(replacementStem); continue }
    if (name.endsWith('.json')) {
      const stem = name.slice(0, -5)
      if (taskState.safeIntegerId(stem) !== null) stems.add(stem)
      else invalid.push(stem)
    } else if (name.includes('.replace-')) invalid.push(name)
  }
  return [...stems].sort().map((stem) => markerReplacementProjection(stem, names) || safeProjection(stem)).concat(
    invalid.sort().map((stem) => ({ stem, status: 'corrupt', phase: null, errorCode: 'BAD_MARKER_NAME', errorMessage: 'unsafe marker filename' })),
  )
}

function replacementArtifactStem(name) {
  const match = /^\.(TASK_\d+_[A-Za-z0-9_]+)\.json\.replace-(?:reservation\.json|wal\.json|candidate-[a-f0-9]{32}|detached-[a-f0-9]{32})$/.exec(String(name || ''))
  return match && taskState.safeIntegerId(match[1]) !== null ? match[1] : null
}
function markerReplacementProjection(stem, knownNames = null) {
  const prefix = `.${stem}.json.replace-`
  const names = knownNames || boundedDirectoryNames(CACHE_DIR, 'finalization state directory', { missing: 'empty' })
  const artifacts = names.filter((name) => name.startsWith(prefix))
  if (!artifacts.length) return null
  const hasAuthority = artifacts.includes(`.${stem}.json.replace-reservation.json`) ||
    artifacts.includes(`.${stem}.json.replace-wal.json`)
  if (!hasAuthority || artifacts.some((name) => replacementArtifactStem(name) !== stem)) {
    return {
      stem,
      status: 'corrupt',
      phase: null,
      errorCode: 'FINALIZATION_MARKER_RECOVERY_UNSAFE',
      errorMessage: 'private finalization-marker replacement artifacts are malformed or missing durable intent',
    }
  }
  return {
    stem,
    status: 'recovery-required',
    phase: null,
    errorCode: 'FINALIZATION_MARKER_RECOVERY_REQUIRED',
    errorMessage: 'durable finalization-marker replacement requires exact recovery',
  }
}

function ownerAppearsLive(owner) {
  if (!owner || !Number.isInteger(owner.pid) || owner.pid <= 0) return false
  if (owner.hostname !== host) return true
  if (!writerLeases.PROCESS_START_ID_RE.test(String(owner.processStartId || ''))) return true
  return writerLeases.processIdentityMatches(owner.pid, owner.processStartId)
}
function assertFinalizationClaimAvailable(stem, invocationId) {
  const names = boundedDirectoryNames(CACHE_DIR, 'finalization state directory', { missing: 'empty' })
  const markerNames = names.filter((name) => name.endsWith('.json') && name !== '.mutex.json')
  const foreign = markerNames.filter((name) => name !== `${stem}.json`)
  if (foreign.length) {
    fail('FOREIGN_FINALIZATION_ACTIVE', `another recovery marker exists (${foreign.sort()[0]}); finish that transaction before starting/resuming ${stem}`)
  }
  if (!markerNames.includes(`${stem}.json`)) return
  const existing = readMarker(stem).marker
  if (existing.owner && existing.owner.invocationId !== invocationId && ownerAppearsLive(existing.owner)) {
    fail('FINALIZATION_OWNER_ACTIVE', `${stem} is still owned by live process ${existing.owner.pid}@${existing.owner.hostname}; refusing concurrent recovery`)
  }
}

async function acquireMutex(invocationId) {
  ensureDir(CACHE_DIR)
  regularFile(MUTEX_PATH, { code: 'MUTEX_INVALID', maxBytes: 4096 })
  const python = process.env.FINALIZE_LOCK_PYTHON || 'python3'
  const helper = resolve(process.env.FINALIZE_MUTEX_HELPER || join(HERE, 'finalize-lock.py'))
  const child = spawn(python, [helper, MUTEX_PATH, invocationId, PROJECT_ROOT], {
    cwd: PROJECT_ROOT,
    env: process.env,
    stdio: ['pipe', 'pipe', 'pipe'],
  })
  const lease = { child, invocationId, lost: false, releasing: false, closed: false, exitCode: null, signal: null, stdinError: null }
  // Writable-side failures are emitted on child.stdin, not ChildProcess. Keep
  // a listener installed for the full lease lifetime so a late asynchronous
  // EPIPE cannot terminate finalize-task. Before intentional release it is an
  // ownership failure; during release it triggers termination while close
  // remains the settlement proof.
  child.stdin.on('error', (error) => {
    lease.stdinError = error
    if (!lease.releasing) lease.lost = true
    try { child.kill('SIGTERM') } catch {}
  })
  const configuredWait = Number(process.env.FINALIZE_MUTEX_WAIT_MS || 900000)
  const waitMs = Number.isFinite(configuredWait) ? Math.max(1, Math.min(60 * 60 * 1000, Math.floor(configuredWait))) : 900000
  return await new Promise((resolveLock, rejectLock) => {
    let stdout = '', stderr = '', settled = false
    const timer = setTimeout(() => {
      if (settled) return
      settled = true
      try { child.kill('SIGKILL') } catch {}
      rejectLock(new FinalizeError('FINALIZATION_BUSY', `global finalization mutex was not acquired within ${waitMs}ms`))
    }, waitMs)
    function rejectOnce(error) {
      if (settled) return
      settled = true; clearTimeout(timer)
      try { child.kill('SIGKILL') } catch {}
      rejectLock(error)
    }
    child.on('error', (e) => {
      if (settled) { lease.lost = true; lease.error = e; return }
      rejectOnce(new FinalizeError('MUTEX_HELPER_FAILED', `mutex helper could not start: ${e.message}`))
    })
    child.stderr.on('data', (chunk) => { stderr = bounded(stderr + String(chunk), 2000) })
    child.stdout.on('data', (chunk) => {
      if (settled) return
      stdout += String(chunk)
      if (!stdout.includes('LOCKED\n')) return
      settled = true; clearTimeout(timer)
      resolveLock(lease)
    })
    child.on('close', (code, signal) => {
      lease.closed = true
      lease.exitCode = code
      lease.signal = signal
      if (!lease.releasing) lease.lost = true
      if (!settled) rejectOnce(new FinalizeError('MUTEX_HELPER_FAILED', `mutex helper exited before acquisition (${code == null ? signal : code})${stderr ? `: ${stderr}` : ''}`))
    })
  })
}
function assertMutexHeld(lease) {
  const child = lease && lease.child
  if (!child || lease.lost || lease.closed || child.exitCode !== null || child.signalCode !== null || child.killed || !Number.isInteger(child.pid)) {
    fail('MUTEX_OWNERSHIP_LOST', 'global finalization mutex helper exited or ownership became uncertain; recovery authority is retained')
  }
  try { process.kill(child.pid, 0) }
  catch (e) {
    if (!e || e.code !== 'EPERM') fail('MUTEX_OWNERSHIP_LOST', `global finalization mutex helper is no longer alive (${e.message}); recovery authority is retained`)
  }
  let record
  try {
    record = JSON.parse(readRegular(MUTEX_PATH, { code: 'MUTEX_INVALID', maxBytes: 4096 }).toString('utf8'))
  } catch (e) {
    fail('MUTEX_OWNERSHIP_LOST', `global finalization mutex ownership record is unreadable or unstable (${e.message}); recovery authority is retained`)
  }
  if (!record || record.released !== false || record.invocationId !== lease.invocationId || record.pid !== child.pid ||
      !writerLeases.PROCESS_START_ID_RE.test(String(record.processStartId || '')) ||
      !writerLeases.processIdentityMatches(record.pid, record.processStartId)) {
    fail('MUTEX_OWNERSHIP_LOST', 'global finalization mutex ownership record no longer identifies this invocation; recovery authority is retained')
  }
}
async function settleProcessEvents() { await new Promise((resolveTurn) => setImmediate(resolveTurn)) }
async function releaseMutex(lease) {
  if (!lease || !lease.child) return
  lease.releasing = true
  const child = lease.child
  if (lease.closed || child.exitCode !== null || child.signalCode !== null) return
  await new Promise((resolveRelease) => {
    let done = false
    let timer
    const finish = () => { if (!done) { done = true; if (timer) clearTimeout(timer); resolveRelease() } }
    child.once('close', finish)
    try { child.stdin.end('RELEASE\n') } catch { try { child.kill('SIGTERM') } catch {} }
    timer = setTimeout(() => { try { child.kill('SIGKILL') } catch {}; finish() }, 2000)
    if (typeof timer.unref === 'function') timer.unref()
  })
}

function assertWriterQuiescence(stem, cliSessionId = '') {
  const scan = writerLeases.scan(WRITER_LEASES_DIR, PROJECT_ROOT)
  if (scan.issues.length) {
    const issue = scan.issues[0]
    fail(issue.code || 'WRITER_LEASE_INVALID', `workspace writer lease state is unsafe: ${bounded(issue.message, 500)}`)
  }
  // finalize-task normally runs inside the task's own Claude turn. That owner
  // turn must retain its lease until the result event, so ignore only the
  // narrowly-typed same-stem task-session lease whose session id is inherited
  // by this finalizer. Global/Figma/other-task/unidentified writers block.
  const sameStem = scan.active.filter((row) => row.kind === 'task-session' && row.stem === stem)
  const inheritedSessionId = String(process.env.ORCHESTRATOR_WRITER_SESSION_ID || '')
  if (cliSessionId && inheritedSessionId && cliSessionId !== inheritedSessionId) {
    fail('WRITER_SESSION_ID_CONFLICT', '--writer-session-id does not match ORCHESTRATOR_WRITER_SESSION_ID')
  }
  const expectedSessionId = String(cliSessionId || inheritedSessionId)
  let allowed = null
  if (expectedSessionId) {
    if (!writerLeases.SESSION_ID_RE.test(expectedSessionId)) fail('WRITER_SESSION_ID_INVALID', 'ORCHESTRATOR_WRITER_SESSION_ID is malformed')
    const exactRows = sameStem.filter((row) => row.sessionId === expectedSessionId)
    if (exactRows.some((row) => row.unverified === true)) {
      fail('WRITER_TREE_UNVERIFIED', `writer session ${expectedSessionId} has no durable process-tree death proof and cannot authorize finalization`)
    }
    const matches = exactRows.filter((row) => row.unverified === false)
    if (matches.length !== 1) fail('WRITER_SESSION_OWNER_MISSING', `expected exactly one active same-stem writer lease for session ${expectedSessionId}; found ${matches.length}`)
    allowed = matches[0]
  }
  const blockers = scan.active.filter((row) => row !== allowed)
  if (!blockers.length) return expectedSessionId || null
  const first = blockers[0]
  const label = first.stem ? `${first.kind}:${first.stem}` : first.kind
  fail('WORKSPACE_WRITER_ACTIVE', `cannot start finalization while ${label} owns a workspace writer lease${blockers.length > 1 ? ` (+${blockers.length - 1} more)` : ''}`)
}

// Direct CLI finalization must honor deterministic-create recovery too; the
// site guard alone is not an integrity boundary. Both operations publish the
// shared INDEX. Completed receipts are idempotency history and do not block.
function assertCreationQuiescence() {
  const directory = entryStat(CREATIONS_DIR, true)
  if (directory.missing) return
  if (directory.stat.kind !== 'directory') {
    fail('CREATION_MARKER_DIR_UNSAFE', 'deterministic creation marker directory is unsafe')
  }
  const names = boundedDirectoryNames(CREATIONS_DIR, 'deterministic creation marker directory')
  const setEntries = []
  for (const name of names) {
    const cas = creationMarkerContract.durableCas.classifyName(name)
    if (cas !== null) {
      fail(cas === 'recovery-required' ? 'CREATION_MARKER_CAS_RECOVERY_REQUIRED' : 'CREATION_MARKER_CAS_NAME_UNSAFE',
        `durable creation CAS state blocks finalization: ${name}`)
    }
    if (!name.endsWith('.json')) continue
    if (!/^[a-f0-9]{64}\.json$/.test(name)) fail('CREATION_MARKER_NAME_INVALID', `unexpected deterministic creation marker ${name}`)
    const raw = readRegular(join(CREATIONS_DIR, name), { code: 'CREATION_MARKER_UNSAFE', maxBytes: CREATION_MARKER_MAX_BYTES })
    let marker
    try { marker = JSON.parse(raw.toString('utf8')) }
    catch (e) { fail('CREATION_MARKER_CORRUPT', `deterministic creation marker ${name} is invalid JSON: ${e.message}`) }
    try { creationMarkerContract.validate(marker, name) }
    catch (e) { fail('CREATION_MARKER_INVALID', `deterministic creation marker ${name} is invalid: ${e.message}`) }
    setEntries.push({ filename: name, value: marker })
    if (marker.status !== 'completed') {
      fail('CREATION_INCOMPLETE', `deterministic backlog creation${marker.stem ? ` for ${marker.stem}` : ''} must be recovered before finalization`)
    }
  }
  try { creationMarkerContract.validateSet(setEntries) }
  catch (e) { fail('CREATION_MARKER_INVALID', `deterministic creation marker set is invalid: ${e.message}`) }
}

function assertEditQuiescence(stem) {
  const issue = editMarkerContract.blockingIssue(EDITS_DIR, stem, false, PROJECT_ROOT)
  if (issue) fail(issue.code || 'EDIT_MARKER_INVALID', `backlog edit state blocks finalization: ${bounded(issue.message, 500)}`)
}

function runChild(code, command, args, extraEnv = {}) {
  if (ACTIVE_MUTEX_LEASE) assertMutexHeld(ACTIVE_MUTEX_LEASE)
  const result = spawnSync(command, args, {
    cwd: PROJECT_ROOT,
    // `--json` is a protocol: stdout must contain exactly one JSON value.
    // Route child diagnostics to our stderr without buffering them in memory.
    stdio: JSON_MODE ? ['ignore', 2, 2] : 'inherit',
    env: { ...process.env, ...extraEnv },
  })
  if (ACTIVE_MUTEX_LEASE) assertMutexHeld(ACTIVE_MUTEX_LEASE)
  if (result.error) fail(code, `${basename(command)} could not start: ${result.error.message}`)
  const status = result.status == null ? 1 : result.status
  if (status !== 0) fail(code, `${basename(command)} exited with ${status}${result.signal ? ` (${result.signal})` : ''}`, code === 'SHIP_BLOCKED' ? 2 : 1)
}

function scripts() {
  return {
    ship: resolve(process.env.FINALIZE_SHIP_SCRIPT || join(PROJECT_ROOT, 'orchestrator', 'figma', 'scripts', 'ship-done.mjs')),
    verifyDone: resolve(process.env.FINALIZE_VERIFY_DONE_SCRIPT || join(PROJECT_ROOT, 'orchestrator', 'figma', 'scripts', 'verify-done.mjs')),
    index: resolve(process.env.FINALIZE_INDEX_SCRIPT || join(PROJECT_ROOT, 'orchestrator', 'tasks', 'regen-index.py')),
    arch: resolve(process.env.FINALIZE_ARCH_SCRIPT || join(PROJECT_ROOT, 'orchestrator', 'tasks', 'regen-arch.py')),
  }
}
function requireFigmaNodeRuntime() {
  const major = Number(String(process.versions.node || '').split('.')[0])
  if (!Number.isInteger(major) || major < 22) {
    fail('NODE_VERSION_UNSUPPORTED', `Figma finalization requires Node >=22, current runtime is ${process.versions.node || 'unknown'}`)
  }
}
function indexData() {
  const path = join(TASKS_DIR, 'INDEX.json')
  regularFile(path, { required: true, code: 'INDEX_MISSING', maxBytes: 16 * 1024 * 1024 })
  try { return JSON.parse(readRegular(path, { code: 'INDEX_INVALID', maxBytes: 16 * 1024 * 1024 }).toString('utf8')) } catch (e) { if (e instanceof FinalizeError) throw e; fail('INDEX_INVALID', `INDEX.json is invalid: ${e.message}`) }
}
function verifyIndexStem(stem) {
  const index = indexData()
  const hits = []
  for (const column of ['backlog', 'pending', 'todo', 'done']) {
    const entries = Array.isArray(index[column]) ? index[column] : []
    for (const entry of entries) if (entry && entry.stem === stem) hits.push({ column, entry })
  }
  if (hits.length !== 1 || hits[0].column !== 'done') fail('INDEX_STEM_INVALID', `${stem} must occur exactly once in INDEX.json done[]; found ${hits.map((x) => x.column).join(', ') || 'nothing'}`)
  if (!hits[0].entry.outcomeStatus || hits[0].entry.outcomeStatus === 'malformed') fail('INDEX_OUTCOME_MALFORMED', `${stem} has malformed Outcome status in INDEX.json`)
}
function readSnapshot(marker) {
  const path = snapshotPath(marker.stem, marker.source.snapshotHash)
  const bytes = readRegular(path, { code: 'OUTCOME_SNAPSHOT_MISSING', maxBytes: TASK_MAX_BYTES })
  if (sha256(bytes) !== marker.source.snapshotHash) fail('OUTCOME_SNAPSHOT_CHANGED', 'persisted Outcome snapshot hash does not match the marker')
  return bytes.toString('utf8')
}
function assertLateOutcomeMatches(stem, outcomeFile) {
  const state = requireOnlyColumn(stem, 'done')
  const done = readRegular(state.paths.done, { code: 'DONE_TASK_CHANGED', maxBytes: TASK_MAX_BYTES }).toString('utf8')
  const draft = readRegular(resolve(outcomeFile), { code: 'OUTCOME_FILE_INVALID', maxBytes: 2 * 1024 * 1024 }).toString('utf8')
  let requested
  try { requested = installOutcomeDraft(done, draft) } catch (e) { fail('OUTCOME_DRAFT_INVALID', e.message) }
  const shapeError = outcomeShapeError(requested)
  if (shapeError) fail('OUTCOME_INVALID', `requested Outcome appendix is malformed: ${shapeError}`)
  if (sha256(logicalTaskText(requested)) !== sha256(logicalTaskText(done))) {
    fail('OUTCOME_TOO_LATE', '--outcome-file differs from the Outcome that already shipped; refusing to report success while ignoring caller input')
  }
}
function markerPhaseDone(marker, phase) { return ['succeeded', 'skipped'].includes(marker.phases[phase] && marker.phases[phase].state) }
function resetFrom(marker, phase) {
  const at = PHASES.indexOf(phase)
  for (let i = at; i < PHASES.length; i++) marker.phases[PHASES[i]] = { state: 'pending', attempts: marker.phases[PHASES[i]] && marker.phases[PHASES[i]].attempts || 0 }
  marker.phase = phase
  marker.status = 'incomplete'
  marker.lastError = null
}

function createMarker(stem, outcomeFile, invocationId, writerSessionId = null) {
  const state = taskArtifacts(stem)
  if (state.columns.length === 1 && state.columns[0] === 'done') {
    if (outcomeFile) assertLateOutcomeMatches(stem, outcomeFile)
    return null
  }
  if (state.columns.length !== 1 || state.columns[0] !== 'todo') {
    fail('TASK_COLUMN_CONFLICT', `${stem} must exist only in todo/ to start finalization; found ${state.columns.join(', ') || 'nothing'}`)
  }
  canonicalFinalizePre(stem)
  const todoBytes = readRegular(state.paths.todo, { code: 'TASK_MISSING', maxBytes: TASK_MAX_BYTES })
  let intended = todoBytes.toString('utf8')
  if (outcomeFile) {
    const draft = readRegular(resolve(outcomeFile), { code: 'OUTCOME_FILE_INVALID', maxBytes: 2 * 1024 * 1024 }).toString('utf8')
    try { intended = installOutcomeDraft(intended, draft) } catch (e) { fail('OUTCOME_DRAFT_INVALID', e.message) }
  }
  let shapeError
  try { shapeError = outcomeShapeError(intended) } catch (e) { fail('OUTCOME_CONTRACT_INVALID', e.message) }
  if (shapeError) fail('OUTCOME_INVALID', `Outcome appendix is malformed: ${shapeError}`)
  const snapshotBytes = Buffer.from(intended)
  if (snapshotBytes.length > TASK_MAX_BYTES) fail('OUTCOME_TASK_TOO_LARGE', `task plus Outcome exceeds the recoverable ${TASK_MAX_BYTES}-byte limit`)
  canonicalDoneCandidate(stem, snapshotBytes)
  const lock = lockPath(stem)
  if (!regularFile(lock, { code: 'UNSAFE_TASK_LOCK', maxBytes: 1024 * 1024 })) {
    fail('TASK_LOCK_MISSING', `${stem} has no active orchestrator lock; refusing to start a publication outside the owned task run`)
  }
  const lockSnapshot = readRegularSnapshot(lock, { code: 'UNSAFE_TASK_LOCK', maxBytes: 1024 * 1024 })
  let lockRecord
  try { lockRecord = JSON.parse(lockSnapshot.bytes.toString('utf8')) }
  catch (e) { fail('TASK_LOCK_INVALID', `${stem} task lock is not valid JSON: ${e.message}`) }
  if (!taskState.canonicalLockV1(lockRecord, stem) || lockRecord.stage !== 'orchestrator') {
    fail('TASK_LOCK_INVALID', `${stem} task lock is not a canonical orchestrator ownership record`)
  }
  if (writerSessionId && lockRecord.sessionId !== writerSessionId) {
    fail('TASK_LOCK_OWNER_MISMATCH', `${stem} task lock session does not match the live writer session authorizing finalization`)
  }
  const lockInfo = { present: true, ...identityProof(lockSnapshot) }
  const testArtifacts = validateTestEvidence(stem, intended, null, {
    record: lockRecord,
    hash: sha256(lockSnapshot.bytes),
  })

  const snapshotHash = sha256(snapshotBytes)
  const phases = {}
  for (const phase of PHASES) phases[phase] = { state: 'pending', attempts: 0 }
  const createdAt = now()
  // Freeze every fallible prerequisite before publishing the snapshot. The
  // snapshot is intentionally immutable, but without a marker it has no owner;
  // invalid config/run-id must therefore leave no orphan recovery artifact.
  const figmaConfig = readFigmaConfig()
  const figmaEnabled = figmaConfig.enabled
  const pipelineRunId = figmaEnabled ? currentRunId(stem) : null
  publishImmutableSnapshot(snapshotPath(stem, snapshotHash), snapshotBytes)
  const marker = {
    version: 1, revision: 0, stem,
    transactionId: `fin-${randomBytes(12).toString('hex')}`,
    status: 'incomplete', phase: 'outcome', createdAt, updatedAt: createdAt,
    owner: ownerRecord(invocationId),
    source: {
      originalHash: sha256(todoBytes), intendedHash: sha256(snapshotBytes),
      intendedLogicalHash: sha256(logicalTaskText(intended)), outcomeHash: outcomeHash(intended),
      snapshotHash, publishFromHash: sha256(todoBytes), lock: lockInfo,
    },
    figma: { enabled: figmaEnabled, configHash: figmaConfig.hash, pipelineRunId },
    phases, artifacts: testArtifacts, lastError: null,
  }
  try { createMarkerExclusive(marker) }
  catch (e) {
    // Remove only a snapshot that no published marker could reference. A
    // competing no-clobber claimant may have won with the same content hash.
    let preserveSnapshot = false
    try {
      const existing = JSON.parse(readRegular(markerPath(stem), { code: 'MARKER_INVALID', maxBytes: MARKER_MAX_BYTES }).toString('utf8'))
      preserveSnapshot = existing && existing.source && existing.source.snapshotHash === snapshotHash
    } catch {
      // A present but corrupt/unstable marker is recovery evidence. Preserve
      // the content-addressed snapshot rather than guessing that it is orphaned.
      preserveSnapshot = !entryStat(markerPath(stem), true).missing
    }
    if (!preserveSnapshot) removeSnapshotFile(snapshotPath(stem, snapshotHash), snapshotHash)
    throw e
  }
  return marker
}

function refreshPreShipIntent(marker, outcomeFile) {
  if (markerPhaseDone(marker, 'ship')) {
    if (outcomeFile) fail('OUTCOME_TOO_LATE', '--outcome-file cannot replace a task after ship succeeded')
    return
  }
  // Resolve an interrupted compare-and-detach Outcome publication before a
  // caller may refresh the intent. Otherwise a concurrent replacement could
  // be mistaken for an authorized new draft and silently adopted.
  reconcileOutcomeProofs(marker)
  const observed = taskArtifacts(marker.stem)
  if (observed.columns.length === 1 && observed.columns[0] === 'done') {
    if (outcomeFile) fail('OUTCOME_TOO_LATE', '--outcome-file cannot replace a task whose ship effect already landed')
    return
  }
  if (observed.columns.length === 2 && observed.present.todo && observed.present.done && reconcileInterruptedShip(marker)) {
    if (outcomeFile) fail('OUTCOME_TOO_LATE', '--outcome-file cannot replace a task whose ship publication already landed')
    return
  }
  if (observed.columns.length !== 1 || observed.columns[0] !== 'todo') {
    fail('TASK_COLUMN_CONFLICT', `pre-ship recovery expected todo/ or an already-landed done/ effect; found ${observed.columns.join(', ') || 'nothing'}`)
  }
  const state = observed
  let current = readRegular(state.paths.todo, { code: 'UNSAFE_TASK_FILE', maxBytes: TASK_MAX_BYTES }).toString('utf8')
  const currentHash = sha256(Buffer.from(current))
  // A crash after marker creation but before the Outcome phase legitimately
  // leaves the original (possibly Outcome-less) body in todo/. The durable
  // snapshot, not that old body, remains the recovery authority.
  if (!outcomeFile && currentHash === marker.source.intendedHash) {
    canonicalDoneCandidate(marker.stem, Buffer.from(current))
    return
  }
  if (!outcomeFile && currentHash === marker.source.originalHash) {
    if (!markerPhaseDone(marker, 'outcome')) return
    // Outcome had already committed, so seeing the original pre-Outcome bytes
    // again is a rollback of todo/, not the harmless pre-effect startup state.
    // Re-arm Outcome from the immutable snapshot; otherwise the succeeded flag
    // skips repair forever and ship rejects the old bytes on every retry.
    marker.source.publishFromHash = currentHash
    resetFrom(marker, 'outcome')
    marker.artifacts = testBindingArtifacts(marker.artifacts)
    writeMarker(marker)
    return
  }
  if (outcomeFile) {
    const draft = readRegular(resolve(outcomeFile), { code: 'OUTCOME_FILE_INVALID', maxBytes: 2 * 1024 * 1024 }).toString('utf8')
    try { current = installOutcomeDraft(current, draft) } catch (e) { fail('OUTCOME_DRAFT_INVALID', e.message) }
  }
  let shapeError
  try { shapeError = outcomeShapeError(current) } catch (e) { fail('OUTCOME_CONTRACT_INVALID', e.message) }
  if (shapeError) fail('OUTCOME_INVALID', `Outcome appendix is malformed: ${shapeError}`)
  const bytes = Buffer.from(current)
  if (bytes.length > TASK_MAX_BYTES) fail('OUTCOME_TASK_TOO_LARGE', `task plus Outcome exceeds the recoverable ${TASK_MAX_BYTES}-byte limit`)
  canonicalDoneCandidate(marker.stem, bytes)
  if (sha256(bytes) === marker.source.intendedHash) return
  // An Outcome-only refresh retains the pre-Outcome identity; any task-body
  // change fails here and requires a fresh certification run before recovery
  // may publish a replacement transaction intent.
  verifyTestEvidence(marker, current)
  const previousSnapshotHash = marker.source.snapshotHash
  const nextSnapshotHash = sha256(bytes)
  // A publication candidate and staged receipts are bound to the old logical
  // intent. They are reusable only when the intent hash is unchanged; carrying
  // them across an Outcome/task refresh would deadlock recovery against stale
  // bytes or, worse, publish a previously certified task.
  discardUnpublishedShipArtifacts(marker)
  // Publish immutable bytes first. Until the marker commit below succeeds,
  // recovery continues to reference the previous content-addressed snapshot.
  publishImmutableSnapshot(snapshotPath(marker.stem, nextSnapshotHash), bytes)
  failpoint('after-snapshot', 'outcome-refresh')
  marker.source.intendedHash = sha256(bytes)
  marker.source.intendedLogicalHash = sha256(logicalTaskText(current))
  marker.source.outcomeHash = outcomeHash(current)
  marker.source.snapshotHash = nextSnapshotHash
  marker.source.publishFromHash = currentHash
  resetFrom(marker, 'outcome')
  marker.artifacts = testBindingArtifacts(marker.artifacts)
  writeMarker(marker)
  if (previousSnapshotHash !== nextSnapshotHash) removeSnapshotFile(snapshotPath(marker.stem, previousSnapshotHash), previousSnapshotHash)
}

function failpoint(where, phase) {
  if (String(process.env.FINALIZE_FAILPOINT || '') !== `${where}:${phase}`) return
  console.error(`finalize-task: injected abrupt failure at ${where}:${phase}`)
  process.exit(97)
}
function beginPhase(marker, phase, invocationId) {
  marker.phase = phase
  marker.status = 'running'
  marker.owner = ownerRecord(invocationId)
  marker.lastError = null
  const previous = marker.phases[phase] || { attempts: 0 }
  marker.phases[phase] = { state: 'running', attempts: Number(previous.attempts || 0) + 1, startedAt: now() }
  writeMarker(marker)
  failpoint('after-intent', phase)
}
function finishPhase(marker, phase, state = 'succeeded', detail = null) {
  marker.phases[phase] = { ...marker.phases[phase], state, finishedAt: now() }
  if (detail) marker.phases[phase].detail = detail
  const next = PHASES[PHASES.indexOf(phase) + 1]
  if (next) marker.phase = next
  marker.status = 'incomplete'
  writeMarker(marker)
}

function outcomeProofState(marker) {
  const sourcePath = outcomeSourcePath(marker)
  const publicationPath = outcomePublicationPath(marker)
  const token = marker.artifacts.outcomeDetachToken
  if (!token) {
    if (regularFile(sourcePath, { code: 'OUTCOME_CAS_PROOF_INVALID', maxBytes: TASK_MAX_BYTES }) ||
        regularFile(publicationPath, { code: 'OUTCOME_CAS_PROOF_INVALID', maxBytes: TASK_MAX_BYTES })) {
      fail('OUTCOME_CAS_PROOF_INVALID', 'Outcome private proof exists without a durable detach token')
    }
    return { active: false, sourcePath, publicationPath, detachPath: null }
  }
  return { active: true, sourcePath, publicationPath, detachPath: outcomeDetachPath(marker) }
}

function proofAt(marker, key, path, expectedHash) {
  const current = readIdentity(path)
  if (current.hash !== expectedHash) fail('OUTCOME_CAS_PROOF_CHANGED', `${basename(path)} does not match the frozen Outcome transaction bytes`)
  const frozen = marker.artifacts[key]
  if (frozen && !sameOwnedFileGeneration(current, frozen)) fail('OUTCOME_CAS_PROOF_CHANGED', `${basename(path)} changed after its ownership proof was recorded`)
  if (!frozen) { marker.artifacts[key] = current; writeMarker(marker) }
  return current
}

function clearOutcomeProofFields(marker) {
  delete marker.artifacts.outcomeSourceProof
  delete marker.artifacts.outcomePublicationProof
  delete marker.artifacts.outcomeTargetProof
  delete marker.artifacts.outcomeDetachToken
  writeMarker(marker)
}

function cleanupOutcomeProofs(marker, targetKind) {
  const paths = outcomeProofState(marker)
  if (!paths.active) return
  const todo = join(TASKS_DIR, 'todo', `${marker.stem}.md`)
  const target = readIdentity(todo, 'TASK_CHANGED_DURING_FINALIZATION')
  const expectedTargetHash = targetKind === 'intended' ? marker.source.intendedHash : marker.source.publishFromHash
  if (target.hash !== expectedTargetHash) fail('TASK_CHANGED_DURING_FINALIZATION', 'todo task changed while reconciling Outcome ownership proofs')
  const sourcePresent = regularFile(paths.sourcePath, { code: 'OUTCOME_CAS_PROOF_INVALID', maxBytes: TASK_MAX_BYTES })
  const publicationPresent = regularFile(paths.publicationPath, { code: 'OUTCOME_CAS_PROOF_INVALID', maxBytes: TASK_MAX_BYTES })
  const detachPresent = regularFile(paths.detachPath, { code: 'OUTCOME_CAS_PROOF_INVALID', maxBytes: TASK_MAX_BYTES })
  if (!sourcePresent && !publicationPresent && !detachPresent &&
      !marker.artifacts.outcomeSourceProof && !marker.artifacts.outcomePublicationProof && !marker.artifacts.outcomeTargetProof) {
    delete marker.artifacts.outcomeDetachToken
    writeMarker(marker)
    return
  }
  const source = sourcePresent ? proofAt(marker, 'outcomeSourceProof', paths.sourcePath, marker.source.publishFromHash) : null
  const publication = publicationPresent ? proofAt(marker, 'outcomePublicationProof', paths.publicationPath, marker.source.intendedHash) : null
  if (targetKind === 'intended') {
    if (publication && !sameInode(target, publication)) fail('TASK_CHANGED_DURING_FINALIZATION', 'todo no longer identifies the transaction Outcome publication')
    if (!publication && (!marker.artifacts.outcomeTargetProof || !sameOwnedFileGeneration(target, marker.artifacts.outcomeTargetProof))) {
      fail('OUTCOME_CAS_PROOF_CHANGED', 'Outcome target proof disappeared before reconciliation')
    }
  } else if (source && !sameInode(target, source)) {
    fail('TASK_CHANGED_DURING_FINALIZATION', 'todo no longer identifies the frozen pre-Outcome source')
  }
  let detached = null
  if (detachPresent) {
    detached = readIdentity(paths.detachPath)
    const expectedSource = source || marker.artifacts.outcomeSourceProof
    if (!expectedSource || !sameInode(detached, expectedSource) || detached.hash !== marker.source.publishFromHash) {
      fail('OUTCOME_CAS_PROOF_CHANGED', 'Outcome detach path contains a foreign generation')
    }
  }
  if (detachPresent) removeFileDurable(paths.detachPath, detached, 'OUTCOME_CAS_PROOF_CHANGED')
  if (sourcePresent) removeFileDurable(paths.sourcePath, source, 'OUTCOME_CAS_PROOF_CHANGED')
  if (publicationPresent) removeFileDurable(paths.publicationPath, publication, 'OUTCOME_CAS_PROOF_CHANGED')
  clearOutcomeProofFields(marker)
}

function restoreQuarantinedOutcome(marker, paths) {
  const todo = join(TASKS_DIR, 'todo', `${marker.stem}.md`)
  if (regularFile(todo, { code: 'UNSAFE_TASK_FILE', maxBytes: TASK_MAX_BYTES })) {
    fail('TASK_CHANGED_DURING_FINALIZATION', 'a task generation appeared while restoring a quarantined Outcome source')
  }
  const retainedBefore = readIdentity(paths.detachPath)
  try { boundary('link', { source: paths.detachPath, target: todo, expected: retainedBefore, maxBytes: TASK_MAX_BYTES }) }
  catch (error) {
    if (error && error.code === 'EEXIST') fail('TASK_CHANGED_DURING_FINALIZATION', 'a task generation appeared while restoring a quarantined Outcome source')
    if (error && ['ENTRY_CHANGED', 'PATH_MISSING'].includes(error.code)) fail('OUTCOME_CAS_PROOF_CHANGED', 'quarantined Outcome source changed before restoration')
    throw error
  }
  const restored = readIdentity(todo), retained = readIdentity(paths.detachPath)
  if (!sameInode(restored, retained)) fail('OUTCOME_CAS_PROOF_CHANGED', 'quarantined task could not be restored exactly')
  removeFileDurable(paths.detachPath, retained, 'OUTCOME_CAS_PROOF_CHANGED')
  return restored
}

function reconcileOutcomeProofs(marker) {
  const paths = outcomeProofState(marker)
  if (!paths.active) return
  const todo = join(TASKS_DIR, 'todo', `${marker.stem}.md`)
  const sourcePresent = regularFile(paths.sourcePath, { code: 'OUTCOME_CAS_PROOF_INVALID', maxBytes: TASK_MAX_BYTES })
  const publicationPresent = regularFile(paths.publicationPath, { code: 'OUTCOME_CAS_PROOF_INVALID', maxBytes: TASK_MAX_BYTES })
  const detachPresent = regularFile(paths.detachPath, { code: 'OUTCOME_CAS_PROOF_INVALID', maxBytes: TASK_MAX_BYTES })
  if (!sourcePresent && !publicationPresent && !detachPresent &&
      !marker.artifacts.outcomeSourceProof && !marker.artifacts.outcomePublicationProof && !marker.artifacts.outcomeTargetProof) {
    delete marker.artifacts.outcomeDetachToken
    writeMarker(marker)
    return
  }
  const source = sourcePresent ? proofAt(marker, 'outcomeSourceProof', paths.sourcePath, marker.source.publishFromHash) : null
  if (publicationPresent) proofAt(marker, 'outcomePublicationProof', paths.publicationPath, marker.source.intendedHash)
  let target = regularFile(todo, { code: 'UNSAFE_TASK_FILE', maxBytes: TASK_MAX_BYTES }) ? readIdentity(todo, 'UNSAFE_TASK_FILE') : null

  if (detachPresent) {
    const detached = readIdentity(paths.detachPath)
    const expected = source || marker.artifacts.outcomeSourceProof
    if (!expected || !sameInode(detached, expected) || detached.hash !== marker.source.publishFromHash) {
      if (!target) target = restoreQuarantinedOutcome(marker, paths)
      fail('TASK_CHANGED_DURING_FINALIZATION', 'todo changed after its Outcome source proof was frozen; the newer generation was restored')
    }
    if (!target) target = restoreQuarantinedOutcome(marker, paths)
  }
  if (!target) fail('TASK_CHANGED_DURING_FINALIZATION', 'todo disappeared while Outcome ownership proofs were active')
  if (target.hash === marker.source.intendedHash) {
    if (publicationPresent) {
      const publication = readIdentity(paths.publicationPath)
      if (!sameInode(target, publication)) fail('TASK_CHANGED_DURING_FINALIZATION', 'todo intended bytes are not the owned Outcome publication generation')
      marker.artifacts.outcomeTargetProof = target
      writeMarker(marker)
    }
    cleanupOutcomeProofs(marker, 'intended')
    return
  }
  if (target.hash === marker.source.publishFromHash && source && sameInode(target, source)) {
    cleanupOutcomeProofs(marker, 'source')
    return
  }
  fail('TASK_CHANGED_DURING_FINALIZATION', 'todo changed while a frozen pre-Outcome source is retained; refusing to adopt or overwrite it')
}

function writeOutcomePublication(path, bytes, mode) {
  writeExclusiveFile(path, bytes, mode)
}

function maybeMutateFixtureTodo(todo) {
  const replacement = process.env.FINALIZE_TEST_REPLACE_TODO_BEFORE_OUTCOME_COMMIT
  const checkedInRoot = resolve(HERE, '..', '..')
  if (!replacement || outcomeFixtureMutated || PROJECT_ROOT === checkedInRoot) return
  outcomeFixtureMutated = true
  const bytes = readRegular(resolve(replacement), { code: 'TEST_REPLACEMENT_INVALID', maxBytes: TASK_MAX_BYTES })
  atomicWrite(todo, bytes, 0o644)
}

function effectOutcome(marker) {
  const intended = readSnapshot(marker)
  const state = taskArtifacts(marker.stem)
  if (state.columns.length === 1 && state.columns[0] === 'done') {
    const done = readRegular(state.paths.done, { code: 'DONE_TASK_CHANGED', maxBytes: TASK_MAX_BYTES }).toString('utf8')
    if (sha256(logicalTaskText(done)) !== marker.source.intendedLogicalHash) fail('DONE_TASK_CHANGED', 'done task does not match the finalization intent')
    return
  }
  if (state.columns.length !== 1 || state.columns[0] !== 'todo') fail('TASK_COLUMN_CONFLICT', `Outcome phase expected todo/ or recovered done/; found ${state.columns.join(', ') || 'nothing'}`)
  // Older/incomplete markers are re-admitted through the current canonical
  // done contract before their snapshot can ever overwrite todo or ship.
  canonicalDoneCandidate(marker.stem, intended)
  reconcileOutcomeProofs(marker)
  let current = readRegularSnapshot(state.paths.todo, { code: 'UNSAFE_TASK_FILE', maxBytes: TASK_MAX_BYTES })
  if (sha256(current.bytes) !== marker.source.intendedHash) {
    if (sha256(current.bytes) !== marker.source.publishFromHash) fail('TASK_CHANGED_DURING_FINALIZATION', 'todo task changed after the finalization intent was recorded; refusing to overwrite it')
    if (!marker.artifacts.outcomeDetachToken) {
      marker.artifacts.outcomeDetachToken = randomBytes(16).toString('hex')
      writeMarker(marker)
    }
    const paths = outcomeProofState(marker)
    const frozenTodo = identityProof(current)
    try { boundary('link', { source: state.paths.todo, target: paths.sourcePath, expected: frozenTodo, maxBytes: TASK_MAX_BYTES }) }
    catch (error) {
      if (error && error.code === 'EEXIST') {
        // A prior attempt may already own this no-clobber proof. proofAt below
        // authenticates its exact bytes and generation before it is trusted.
      } else if (error && ['ENTRY_CHANGED', 'PATH_MISSING'].includes(error.code)) {
        fail('TASK_CHANGED_DURING_FINALIZATION', 'todo changed while publishing its retained pre-Outcome proof')
      } else throw error
    }
    const source = proofAt(marker, 'outcomeSourceProof', paths.sourcePath, marker.source.publishFromHash)
    current = readRegularSnapshot(state.paths.todo, { code: 'UNSAFE_TASK_FILE', maxBytes: TASK_MAX_BYTES })
    const currentProof = identityProof(current)
    if (!sameInode(source, currentProof) || currentProof.hash !== marker.source.publishFromHash) {
      fail('TASK_CHANGED_DURING_FINALIZATION', 'todo changed while its pre-Outcome generation was being retained')
    }
    if (!regularFile(paths.publicationPath, { code: 'OUTCOME_CAS_PROOF_INVALID', maxBytes: TASK_MAX_BYTES })) {
      writeOutcomePublication(paths.publicationPath, intended, current.stat.mode & 0o777)
    }
    proofAt(marker, 'outcomePublicationProof', paths.publicationPath, marker.source.intendedHash)
    maybeMutateFixtureTodo(state.paths.todo)
    if (regularFile(paths.detachPath, { code: 'OUTCOME_CAS_PROOF_INVALID', maxBytes: TASK_MAX_BYTES })) {
      reconcileOutcomeProofs(marker)
      current = readRegularSnapshot(state.paths.todo, { code: 'UNSAFE_TASK_FILE', maxBytes: TASK_MAX_BYTES })
      if (sha256(current.bytes) !== marker.source.publishFromHash) fail('TASK_CHANGED_DURING_FINALIZATION', 'todo changed during Outcome detach recovery')
    }
    try {
      boundary('move', { source: state.paths.todo, target: paths.detachPath, expected: source, maxBytes: TASK_MAX_BYTES })
    } catch (error) {
      if (error && ['ENTRY_CHANGED', 'PATH_MISSING'].includes(error.code)) {
        fail('TASK_CHANGED_DURING_FINALIZATION', 'todo changed immediately before Outcome detach; the foreign generation was preserved')
      }
      throw error
    }
    const detached = readIdentity(paths.detachPath), retained = readIdentity(paths.sourcePath)
    if (!sameInode(detached, retained)) {
      restoreQuarantinedOutcome(marker, paths)
      fail('TASK_CHANGED_DURING_FINALIZATION', 'todo changed immediately before Outcome commit; the newer generation was restored')
    }
    failpoint('after-detach', 'outcome')
    const retainedPublication = readIdentity(paths.publicationPath)
    try { boundary('link', { source: paths.publicationPath, target: state.paths.todo, expected: retainedPublication, maxBytes: TASK_MAX_BYTES }) }
    catch (error) {
      if (error && error.code === 'EEXIST') fail('TASK_CHANGED_DURING_FINALIZATION', 'a task generation appeared before Outcome publication; both generations are preserved')
      if (error && ['ENTRY_CHANGED', 'PATH_MISSING'].includes(error.code)) fail('OUTCOME_CAS_PROOF_CHANGED', 'retained Outcome publication changed before commit')
      throw error
    }
    syncDir(dirname(state.paths.todo))
    const target = readIdentity(state.paths.todo), publication = readIdentity(paths.publicationPath)
    if (!sameInode(target, publication) || target.hash !== marker.source.intendedHash) fail('OUTCOME_PUBLISH_MISMATCH', 'published Outcome is not the retained transaction generation')
    marker.artifacts.outcomeTargetProof = target
    writeMarker(marker)
    cleanupOutcomeProofs(marker, 'intended')
  }
  const installed = readRegular(state.paths.todo, { code: 'UNSAFE_TASK_FILE', maxBytes: TASK_MAX_BYTES }).toString('utf8')
  const shapeError = outcomeShapeError(installed)
  if (shapeError) fail('OUTCOME_INVALID', `published Outcome appendix is malformed: ${shapeError}`)
  if (sha256(Buffer.from(installed)) !== marker.source.intendedHash) fail('OUTCOME_PUBLISH_MISMATCH', 'published task hash does not match the persisted intent')
}
// ── components phase ─────────────────────────────────────────────────────────
// Design-origin component tasks publish their binding-authorized mapping here,
// inside the same finalization transaction, strictly BEFORE the task moves to
// done/: a crash leaves either the old registry bytes + an unfinished marker,
// or the new bytes recorded in marker.artifacts — both exactly recoverable.
// Generic tasks only pass a touch-scoped regression gate; they never auto-map.
const COMPONENT_MAPPINGS_PATH = () => join(PROJECT_ROOT, 'orchestrator', 'figma', 'component-mappings.json')

function readComponentRegistry(contract) {
  const path = COMPONENT_MAPPINGS_PATH()
  if (entryStat(path, true).missing) return { present: false, document: null, bytes: null }
  const bytes = readRegular(path, { code: 'COMPONENT_MAPPING_INVALID', maxBytes: 8 * 1024 * 1024 })
  let document
  try { document = JSON.parse(bytes.toString('utf8')) } catch (error) {
    fail('COMPONENT_MAPPING_INVALID', `component-mappings.json is not valid JSON: ${error.message}`)
  }
  const semantic = contract.mappingRegistrySemanticError(document)
  if (semantic) fail('COMPONENT_MAPPING_INVALID', semantic)
  if (!Number.isSafeInteger(document.revision) || document.revision < 0) fail('COMPONENT_MAPPING_INVALID', 'registry revision malformed')
  return { present: true, document, bytes }
}

function writeComponentRegistry(document) {
  const path = COMPONENT_MAPPINGS_PATH()
  const bytes = Buffer.from(JSON.stringify(document, null, 2) + '\n')
  const tmp = `${path}.finalize-${randomBytes(8).toString('hex')}.tmp`
  writeFileSync(tmp, bytes, { mode: 0o644 })
  renameSync(tmp, path)
  const verify = readRegular(path, { code: 'COMPONENT_MAPPING_PUBLICATION_RECOVERY_REQUIRED', maxBytes: 8 * 1024 * 1024 })
  if (sha256(verify) !== sha256(bytes)) fail('COMPONENT_MAPPING_PUBLICATION_RECOVERY_REQUIRED', 'post-write registry bytes differ from the intent')
  return bytes
}

async function effectComponents(marker, fixtureOptions = {}) {
  verifyFigmaConfig(marker)
  if (!marker.figma.enabled) return 'skipped'
  const intentText = readSnapshot(marker)
  const parsedSource = taskSourceContract.parse(intentText)
  const ref = parsedSource.valid && parsedSource.source.kind === 'figma'
    ? componentBindingContract.bindingSourceId(parsedSource.source.ref)
    : null
  const [{ loadAdapterConfig }, extraction, comparatorModule, contract] = await Promise.all([
    import('../figma/runtime/adapter-config.mjs'),
    import('../figma/runtime/component-extraction.mjs'),
    import('../figma/components/comparator.mjs'),
    import('../figma/components/mapping-contract.mjs'),
  ])
  let configState
  try { configState = loadAdapterConfig({ projectRoot: PROJECT_ROOT }) } catch (error) {
    if (!ref) return 'skipped'
    fail('COMPONENT_TASK_BINDING_STALE', `adapter configuration is invalid: ${error.detail || error.message}`)
  }

  if (!ref) {
    // Generic task: touch-scoped regression gate only. A pre-existing gap in
    // the project inventory never blocks an unrelated task; a fresh
    // structural break relative to a previously complete published analysis
    // does, because this transaction is what introduced it.
    if (configState.state !== 'configured' || !configState.config.enabledComponentAdapters.length) return 'skipped'
    let previousComplete = null
    try {
      const componentsLib = await import('../figma/scripts/lib/design-components.mjs')
      const published = componentsLib.loadPublishedComponentAnalysis(fixtureOptions)
      previousComplete = published.present && typeof published.index.complete === 'boolean' ? published.index.complete : null
    } catch (error) {
      fail('COMPONENT_GENERATION_RESYNC_REQUIRED', `published component analysis is invalid: ${error.message}`)
    }
    try {
      const result = extraction.extractProjectComponents({
        projectRoot: PROJECT_ROOT,
        config: configState.config,
        configHash: configState.componentConfigHash,
      })
      if (!result.index.complete && previousComplete === true) {
        fail('COMPONENT_EXTRACTION_REGRESSION', 'this task left the configured component scope unparseable while the last published analysis was complete; fix the touched sources before finalizing')
      }
    } catch (error) {
      if (previousComplete === true) {
        fail('COMPONENT_EXTRACTION_REGRESSION', `component extraction broke during this task: ${error.detail || error.message}`)
      }
      process.stderr.write(`[finalize-components] extraction unavailable (${String(error.code || 'extraction-failed').slice(0, 80)}); no prior complete analysis exists, so this generic task is not blocked\n`)
    }
    return 'succeeded'
  }

  // ── design-origin binding path ────────────────────────────────────────────
  const bindingPath = resolve(PROJECT_ROOT, ...componentBindingContract.bindingRelativePath(ref.sourceId).split('/'))
  const bindingBytes = readRegular(bindingPath, { code: 'COMPONENT_TASK_BINDING_INVALID', maxBytes: 1024 * 1024 })
  if (sha256(bindingBytes) !== parsedSource.source.fingerprint) {
    fail('COMPONENT_TASK_BINDING_INVALID', 'binding evidence bytes do not match the task Source fingerprint')
  }
  let binding
  try { binding = JSON.parse(bindingBytes.toString('utf8')) } catch (error) {
    fail('COMPONENT_TASK_BINDING_INVALID', `binding evidence is not valid JSON: ${error.message}`)
  }
  const bindingProblem = componentBindingContract.bindingError(binding)
  if (bindingProblem) fail('COMPONENT_TASK_BINDING_INVALID', bindingProblem)

  const componentsLib = await import('../figma/scripts/lib/design-components.mjs')
  const design = componentsLib.loadDesignComponentInventory(fixtureOptions)
  if (!design.present) fail('COMPONENT_TASK_BINDING_STALE', 'no committed design component inventory exists; resync before finalizing this task')
  const inventory = design.inventory
  if (inventory.scopeId !== binding.designScopeId) {
    fail('COMPONENT_TASK_BINDING_STALE', `design scope changed (${binding.designScopeId} -> ${inventory.scopeId}); recreate the task from a current finding`)
  }
  const designComponent = inventory.components.find((component) => component.designComponentId === binding.designComponentId)
  if (!designComponent) fail('COMPONENT_TASK_BINDING_STALE', `design component ${binding.designComponentId} is no longer in the inventory`)
  if (designComponent.kind !== binding.expectedKind) {
    fail('COMPONENT_TASK_BINDING_STALE', `design entity kind changed to ${designComponent.kind}; the binding expected ${binding.expectedKind}`)
  }
  if (designComponent.structuralHash !== binding.frozenStructuralHash) {
    fail('COMPONENT_TASK_BINDING_STALE', 'the design component structure changed since the task was created; rebase the task on a fresh finding')
  }

  const registry = readComponentRegistry(contract)
  const registryDocument = registry.present ? registry.document : contract.emptyMappingRegistry(inventory.scopeId)

  // Idempotent resume: a crash after publication leaves our mapping with
  // task-binding provenance for this exact stem — reconcile, never re-write.
  const published = registryDocument.mappings.find((mapping) =>
    mapping.state === 'active' && mapping.designComponentId === binding.designComponentId &&
    mapping.provenance && mapping.provenance.kind === 'task-binding' && mapping.provenance.taskStem === marker.stem)
  if (published) {
    marker.artifacts.componentMappingHash = sha256(registry.bytes)
    marker.artifacts.componentMappingRevision = registryDocument.revision
    writeMarker(marker)
    return 'succeeded'
  }
  if (registryDocument.revision !== binding.mappingRevision) {
    fail('COMPONENT_TASK_BINDING_STALE', `mapping registry moved to revision ${registryDocument.revision} (binding froze ${binding.mappingRevision}); rebase the task`)
  }
  if (configState.state !== 'configured') {
    fail('COMPONENT_TASK_BINDING_STALE', 'project adapters are no longer configured')
  }
  const adapter = configState.config.enabledComponentAdapters.find((entry) => entry.id === binding.intendedAdapterId)
  if (!adapter) fail('COMPONENT_TASK_BINDING_STALE', `intended adapter ${binding.intendedAdapterId} is no longer configured/enabled`)
  if (adapter.platform !== binding.intendedPlatform) {
    fail('COMPONENT_TASK_BINDING_STALE', `intended adapter now targets platform ${adapter.platform}; the binding expected ${binding.intendedPlatform}`)
  }

  const snapshot = extraction.componentAdapterSnapshot(PROJECT_ROOT, adapter, { keepText: true })
  const projectInventory = extraction.buildComponentInventory({ adapter, snapshot, configHash: configState.componentConfigHash })
  if (!projectInventory.witness.complete) {
    fail('COMPONENT_PROJECT_INVENTORY_INCOMPLETE', 'the intended adapter scope did not scan completely; fix the touched sources first')
  }
  const candidates = projectInventory.components.filter((component) =>
    (component.fqName || component.name) === binding.intendedProjectSymbol)
  if (candidates.length !== 1) {
    fail('COMPONENT_TASK_BINDING_AMBIGUOUS', candidates.length === 0
      ? `the intended project component ${binding.intendedProjectSymbol} does not exist after this task; implement it at the intended symbol or recreate the task`
      : `the intended symbol ${binding.intendedProjectSymbol} resolves to ${candidates.length} components (overloads need an explicit Mapping Review confirmation)`)
  }
  const projectComponent = candidates[0]

  // Stage the proposed mapping on a draft and validate the comparison BEFORE
  // any canonical byte changes (§21.2).
  const draft = JSON.parse(JSON.stringify(registryDocument))
  const propertyMappings = (binding.intendedPropertyMappings || []).map((row) => ({
    designPropertyId: row.designPropertyId,
    adapterId: adapter.id,
    projectPropertyId: row.projectPropertyId,
    ...(row.valueMap !== undefined ? { valueMap: row.valueMap } : {})
  }))
  // Slot bindings the creator could freeze provably: exact-name pairs between
  // the frozen design slots and the implemented component's slots.
  const slotMappings = []
  for (const designSlot of designComponent.semanticSlots) {
    if (designSlot.kind !== 'text-property' && designSlot.kind !== 'instance-swap') continue
    const match = projectComponent.slots.filter((slot) =>
      slot.name.toLowerCase().replace(/[^a-z0-9]/g, '') === designSlot.name.toLowerCase().replace(/[^a-z0-9]/g, ''))
    if (match.length === 1) {
      slotMappings.push({ designSlotId: designSlot.slotId, adapterId: adapter.id, projectSlotId: match[0].slotId, verification: 'static' })
    }
  }
  const proposal = {
    mappingId: binding.mappingId || `cmap-${randomBytes(12).toString('hex')}`,
    designComponentId: binding.designComponentId,
    expectedKind: binding.expectedKind,
    implementations: [{
      adapterId: adapter.id,
      platform: adapter.platform,
      projectScopeFingerprint: projectInventory.scopeFingerprint,
      relation: binding.intendedRelation,
      projectComponentIds: [projectComponent.projectComponentId],
      required: true
    }],
    propertyMappings,
    slotMappings,
    state: 'active',
    provenance: { kind: 'task-binding', actor: 'finalizer', at: now(), taskStem: marker.stem },
  }
  const intent = binding.intent
  let mutated = false
  if (intent === 'implement') {
    if (draft.mappings.some((mapping) => mapping.state === 'active' && mapping.designComponentId === binding.designComponentId)) {
      fail('COMPONENT_MAPPING_CONFLICT', `design component ${binding.designComponentId} already has an active mapping owned by another flow`)
    }
    draft.mappings.push(proposal)
    mutated = true
  } else if (intent === 'remap' || intent === 'reconcile-mapping' || intent === 'update-api' || intent === 'add-platform') {
    const position = draft.mappings.findIndex((mapping) => mapping.mappingId === binding.mappingId)
    if (position < 0) fail('COMPONENT_TASK_BINDING_STALE', `mapping ${binding.mappingId} no longer exists`)
    if (intent === 'add-platform') {
      const existing = JSON.parse(JSON.stringify(draft.mappings[position]))
      if (existing.implementations.some((implementation) => implementation.adapterId === adapter.id)) {
        fail('COMPONENT_MAPPING_CONFLICT', `mapping ${binding.mappingId} already binds adapter ${adapter.id}`)
      }
      existing.implementations.push(proposal.implementations[0])
      existing.propertyMappings = existing.propertyMappings.concat(propertyMappings)
      existing.slotMappings = existing.slotMappings.concat(slotMappings)
      existing.state = 'active'
      existing.provenance = proposal.provenance
      draft.mappings[position] = existing
    } else {
      if (draft.mappings[position].visualPolicy) proposal.visualPolicy = draft.mappings[position].visualPolicy
      draft.mappings[position] = proposal
    }
    mutated = true
  }
  // update-visual changes pixels, not the mapping contract: it only passes the
  // staleness + validation gates below.
  const draftError = contract.mappingRegistrySemanticError(draft)
  if (draftError) fail('COMPONENT_MAPPING_INVALID', `proposed registry would be invalid: ${draftError}`)

  const analysisIndex = {
    schemaVersion: 2,
    configHash: configState.componentConfigHash,
    adapters: [{
      adapterId: adapter.id,
      platform: adapter.platform,
      role: `project-component-inventory:${adapter.id}`,
      inventoryHash: (await import('../figma/components/project-inventory-contract.mjs')).projectInventorySemanticHash(projectInventory),
      scopeFingerprint: projectInventory.scopeFingerprint,
      complete: true,
    }],
    complete: true,
  }
  const { report } = comparatorModule.compareComponents({
    designInventory: inventory,
    projectInventories: [projectInventory],
    analysisIndex,
    mappingRegistry: draft,
    baseline: null,
    tokenSnapshot: { report: null, bindingSnapshot: null },
    context: {
      designGenerationId: design.generationId,
      adapterConfigHash: configState.componentConfigHash,
      adapterConfigFileHash: configState.componentConfigFileHash,
    },
  })
  const row = report.rows.find((candidate) => candidate.designComponentId === binding.designComponentId)
  if (!row || !['matched', 'drifted'].includes(row.status)) {
    fail('COMPONENT_TASK_BINDING_AMBIGUOUS', `validation comparison classified the bound component as ${row ? row.status : 'absent'}${row && row.statusDetail ? ` (${row.statusDetail})` : ''}; the mapping is not publishable`)
  }
  if (row.status === 'drifted') {
    process.stderr.write(`[finalize-components] ${binding.designComponentId} publishes as drifted: the comparison will carry the drift honestly\n`)
  }

  if (mutated) {
    draft.revision = registryDocument.revision + 1
    failpoint('before-component-mapping-publish', 'components')
    const bytes = writeComponentRegistry(draft)
    failpoint('after-component-mapping-publish', 'components')
    marker.artifacts.componentMappingHash = sha256(bytes)
    marker.artifacts.componentMappingRevision = draft.revision
    writeMarker(marker)
  }
  return 'succeeded'
}
// ── tokens phase ─────────────────────────────────────────────────────────────
// Design-origin token tasks publish their authorized mapping here, inside the
// same finalization transaction, strictly BEFORE the task moves to done/: a
// crash leaves either the old registry bytes + an unfinished marker, or the
// new bytes recorded in marker.artifacts — both exactly recoverable. Generic
// tasks only pass a touch-scoped regression gate; they never auto-map.
const TOKEN_MAPPINGS_PATH = () => join(PROJECT_ROOT, 'orchestrator', 'figma', 'token-mappings.json')

function readTokenRegistry(contract, validateSchema) {
  const path = TOKEN_MAPPINGS_PATH()
  if (entryStat(path, true).missing) return { present: false, document: null, bytes: null }
  const bytes = readRegular(path, { code: 'TOKEN_MAPPING_INVALID', maxBytes: 8 * 1024 * 1024 })
  let document
  try { document = JSON.parse(bytes.toString('utf8')) } catch (error) {
    fail('TOKEN_MAPPING_INVALID', `token-mappings.json is not valid JSON: ${error.message}`)
  }
  if (typeof validateSchema === 'function' && !validateSchema(document)) {
    const first = (validateSchema.errors || [])[0]
    fail('TOKEN_MAPPING_INVALID', `token-mappings.json violates the current schema: ${
      first ? `${first.instancePath || '/'} ${first.message}` : 'unknown validation error'}`)
  }
  const semantic = contract.mappingRegistrySemanticError(document)
  if (semantic) fail('TOKEN_MAPPING_INVALID', semantic)
  if (!Number.isSafeInteger(document.revision) || document.revision < 0) fail('TOKEN_MAPPING_INVALID', 'registry revision malformed')
  return { present: true, document, bytes }
}

function writeTokenRegistry(document) {
  const path = TOKEN_MAPPINGS_PATH()
  const bytes = Buffer.from(JSON.stringify(document, null, 2) + '\n')
  const tmp = `${path}.finalize-${randomBytes(8).toString('hex')}.tmp`
  writeFileSync(tmp, bytes, { mode: 0o644 })
  renameSync(tmp, path)
  const verify = readRegular(path, { code: 'TOKEN_MAPPING_PUBLICATION_RECOVERY_REQUIRED', maxBytes: 8 * 1024 * 1024 })
  if (sha256(verify) !== sha256(bytes)) fail('TOKEN_MAPPING_PUBLICATION_RECOVERY_REQUIRED', 'post-write registry bytes differ from the intent')
  return bytes
}

async function effectTokens(marker, fixtureOptions = {}) {
  verifyFigmaConfig(marker)
  if (!marker.figma.enabled) return 'skipped'
  const intentText = readSnapshot(marker)
  const parsedSource = taskSourceContract.parse(intentText)
  const ref = parsedSource.valid && parsedSource.source.kind === 'figma'
    ? tokenBindingContract.bindingSourceId(parsedSource.source.ref)
    : null
  const [
    { loadAdapterConfig },
    extraction,
    comparatorModule,
    contract,
    observedReader,
    binderModule,
    sourceContract,
    { canonicalHash, canonicalJson },
    { createSchemaRegistry },
  ] = await Promise.all([
    import('../figma/runtime/adapter-config.mjs'),
    import('../figma/runtime/token-extraction.mjs'),
    import('../figma/tokens/comparator.mjs'),
    import('../figma/tokens/mapping-contract.mjs'),
    import('../figma/scripts/lib/observed-token-domain.mjs'),
    import('../figma/tokens/binder.mjs'),
    import('../figma/tokens/source-contract.mjs'),
    import('../figma/runtime/canonical-json.mjs'),
    import('../figma/runtime/schema-registry.mjs'),
  ])
  let configState
  try { configState = loadAdapterConfig({ projectRoot: PROJECT_ROOT }) } catch (error) {
    if (!ref) return 'skipped'
    fail('TOKEN_TASK_BINDING_STALE', `adapter configuration is invalid: ${error.detail || error.message}`)
  }

  if (!ref) {
    if (configState.state !== 'configured' || !configState.config.enabledTokenAdapters.length) return 'skipped'
    let previousComplete = null
    try {
      const published = observedReader.loadPublishedTokenAnalysis(fixtureOptions)
      previousComplete = published.present ? published.index.complete : null
    } catch (error) {
      fail('TOKEN_GENERATION_RESYNC_REQUIRED', `published token analysis is invalid: ${error.message}`)
    }
    try {
      const result = extraction.extractProjectTokens({
        projectRoot: PROJECT_ROOT,
        config: configState.config,
        configHash: configState.tokenConfigHash,
      })
      if (!result.index.complete && previousComplete === true) {
        fail('TOKEN_EXTRACTION_REGRESSION', 'this task left the configured token scope unparseable while the last published analysis was complete; fix the touched sources before finalizing')
      }
    } catch (error) {
      if (previousComplete === true) {
        fail('TOKEN_EXTRACTION_REGRESSION', `token extraction broke during this task: ${error.detail || error.message}`)
      }
      process.stderr.write(`[finalize-tokens] extraction unavailable (${String(error.code || 'extraction-failed').slice(0, 80)}); no prior complete analysis exists, so this generic task is not blocked\n`)
    }
    return 'succeeded'
  }

  // ── design-origin binding path ────────────────────────────────────────────
  const bindingPath = resolve(PROJECT_ROOT, ...tokenBindingContract.bindingRelativePath(ref.sourceId).split('/'))
  const bindingBytes = readRegular(bindingPath, { code: 'TOKEN_TASK_BINDING_INVALID', maxBytes: 256 * 1024 })
  if (sha256(bindingBytes) !== parsedSource.source.fingerprint) {
    fail('TOKEN_TASK_BINDING_INVALID', 'binding evidence bytes do not match the task Source fingerprint')
  }
  let binding
  try { binding = JSON.parse(bindingBytes.toString('utf8')) } catch (error) {
    fail('TOKEN_TASK_BINDING_INVALID', `binding evidence is not valid JSON: ${error.message}`)
  }
  const bindingProblem = tokenBindingContract.bindingError(binding)
  if (bindingProblem) fail('TOKEN_TASK_BINDING_INVALID', bindingProblem)

  const observed = observedReader.loadObservedTokenDomain(fixtureOptions)
  if (!observed.present) fail('TOKEN_TASK_BINDING_STALE', 'no committed observed token domain exists; resync before finalizing')
  if (observed.catalog.semanticHash !== binding.catalogHash ||
      observed.sourceIndex.semanticHash !== binding.sourceIndexHash) {
    fail('TOKEN_TASK_BINDING_STALE', 'the observed token catalog/source index moved since task creation')
  }
  const publishedBindings = observedReader.loadPublishedBindingSnapshot(fixtureOptions)
  if (!publishedBindings.present || publishedBindings.snapshot.semanticHash !== binding.bindingSnapshotHash) {
    fail('TOKEN_TASK_BINDING_STALE', 'the effective binding snapshot moved since task creation')
  }
  const observedToken = observed.catalog.tokens.find((token) => token.observedTokenKey === binding.observedTokenKey)
  if (!observedToken || observedToken.presenceStatus === 'not-observed') {
    fail('TOKEN_TASK_BINDING_STALE', `observed token ${binding.observedTokenKey} is no longer active`)
  }
  const coordinate = observedToken.coordinates.find((candidate) =>
    sourceContract.contextKey(candidate.context) === binding.contextKey)
  if (!coordinate || coordinate.status !== 'consistent' || coordinate.values.length !== 1) {
    fail('TOKEN_TASK_BINDING_STALE', 'the frozen observed coordinate is absent, conflicting, or unsupported')
  }
  const observedValue = coordinate.values[0]
  if (observedValue.kind !== binding.expectedKind ||
      canonicalJson(observedValue.value) !== canonicalJson(binding.frozenValue)) {
    fail('TOKEN_TASK_BINDING_STALE', 'the observed token kind or frozen value changed since task creation')
  }

  const schemaRegistry = createSchemaRegistry(join(HERE, '..', 'figma', 'schemas'))
  const registry = readTokenRegistry(contract, schemaRegistry.validate('token-mappings'))
  const registryDocument = registry.present ? registry.document : contract.emptyMappingRegistry(observed.catalog.scope)
  if (registryDocument.scope.fileKeyFingerprint !== observed.catalog.scope.fileKeyFingerprint ||
      registryDocument.scope.branchKey !== observed.catalog.scope.branchKey) {
    fail('TOKEN_MAPPING_SCOPE_CHANGED', 'mapping registry is bound to another observed-token scope')
  }

  const published = registryDocument.mappings.find((mapping) =>
    mapping.state === 'active' && mapping.observedTokenKey === binding.observedTokenKey &&
    mapping.provenance && mapping.provenance.kind === 'design-task-finalization' &&
    mapping.provenance.taskStem === marker.stem)
  if (published) {
    marker.artifacts.tokenMappingHash = sha256(registry.bytes)
    marker.artifacts.tokenMappingRevision = registryDocument.revision
    writeMarker(marker)
    return 'succeeded'
  }
  requireCurrentTokenSourceHealth(observed.sourceIndex)
  if (registryDocument.revision !== binding.mappingRevision) {
    fail('TOKEN_TASK_BINDING_STALE', `mapping registry moved to revision ${registryDocument.revision} (binding froze ${binding.mappingRevision}); rebase the task`)
  }
  if (configState.state !== 'configured') {
    fail('TOKEN_TASK_BINDING_STALE', 'project adapters are no longer configured')
  }
  const adapter = configState.config.enabledTokenAdapters.find((entry) => entry.id === binding.intendedAdapterId)
  if (!adapter) fail('TOKEN_TASK_BINDING_STALE', `intended adapter ${binding.intendedAdapterId} is no longer configured/enabled`)

  const project = extraction.extractProjectTokens({
    projectRoot: PROJECT_ROOT,
    config: configState.config,
    configHash: configState.tokenConfigHash,
  })
  const projectInventory = project.inventories.find((inventory) => inventory.adapterId === adapter.id)
  if (!projectInventory) fail('TOKEN_TASK_BINDING_STALE', `intended adapter ${adapter.id} produced no inventory`)
  if (!projectInventory.witness.complete) {
    fail('PROJECT_TOKEN_INVENTORY_INCOMPLETE', 'the intended adapter scope did not scan completely; fix the touched sources first')
  }
  const intendedJoined = binding.intendedSemanticPath.join('.')
  const candidates = projectInventory.tokens.filter((token) => token.semanticPath.join('.') === intendedJoined)
  if (candidates.length !== 1) {
    fail('TOKEN_TASK_BINDING_AMBIGUOUS', candidates.length === 0
      ? `the intended project token ${intendedJoined} does not exist after this task; implement it at the intended path or recreate the task`
      : `the intended path ${intendedJoined} resolves to ${candidates.length} tokens`)
  }
  const projectToken = candidates[0]
  if (!(projectToken.kind === binding.expectedKind ||
      binding.expectedKind === 'number' && projectToken.kind === 'dimension')) {
    fail('TOKEN_TASK_BINDING_AMBIGUOUS', `the implemented token is a ${projectToken.kind}; the binding expects ${binding.expectedKind}`)
  }
  if (!projectToken.modes[binding.intendedProjectMode]) {
    fail('TOKEN_TASK_BINDING_AMBIGUOUS', `the intended project mode ${binding.intendedProjectMode} is absent`)
  }

  const draft = JSON.parse(JSON.stringify(registryDocument))
  const intent = binding.intent
  const proposal = {
    mappingId: binding.mappingId || `map-${randomBytes(12).toString('hex')}`,
    observedTokenKey: binding.observedTokenKey,
    contextSelector: JSON.parse(binding.contextKey),
    adapterId: adapter.id,
    projectTokenIds: [projectToken.projectTokenId],
    expectedKind: binding.expectedKind,
    relation: 'one-to-one',
    state: 'active',
    provenance: { kind: 'design-task-finalization', actor: 'finalizer', at: now(), taskStem: marker.stem },
  }
  let mutated = false
  if (intent === 'implement') {
    if (draft.mappings.some((mapping) =>
      mapping.state === 'active' && mapping.observedTokenKey === binding.observedTokenKey &&
      canonicalJson(mapping.contextSelector) === canonicalJson(proposal.contextSelector))) {
      fail('TOKEN_MAPPING_CONFLICT', `observed token ${binding.observedTokenKey} already has an active mapping for this context`)
    }
    draft.mappings.push(proposal)
    mutated = true
  } else if (intent === 'reconcile-mapping') {
    const position = draft.mappings.findIndex((mapping) => mapping.mappingId === binding.mappingId)
    if (position < 0) fail('TOKEN_TASK_BINDING_STALE', `mapping ${binding.mappingId} no longer exists`)
    draft.mappings[position] = proposal
    mutated = true
  }
  const draftError = contract.mappingRegistrySemanticError(draft)
  if (draftError) fail('TOKEN_MAPPING_INVALID', `proposed registry would be invalid: ${draftError}`)

  const analysisIndexHash = canonicalHash(project.index)
  const effectiveBindings = binderModule.bindObservedTokens({
    catalog: observed.catalog,
    projectInventories: project.inventories,
    adapterConfig: { ...configState.config, tokenConfigHash: configState.tokenConfigHash },
    mappingRegistry: draft,
    projectAnalysisHash: analysisIndexHash,
  })
  const { report } = comparatorModule.compareTokens({
    observedCatalog: observed.catalog,
    sourceIndex: observed.sourceIndex,
    projectInventories: project.inventories,
    analysisIndex: project.index,
    bindingSnapshot: effectiveBindings,
    mappingRegistry: draft,
    baseline: null,
    context: {
      analysisIndexHash,
      adapterConfigHash: configState.tokenConfigHash,
      baselineHash: 'none',
      sourceFreshness: 'current',
    },
  })
  const row = report.observedRows.find((candidate) =>
    candidate.observedTokenKey === binding.observedTokenKey &&
    candidate.contextKey === binding.contextKey)
  if (!row || !['matched', 'value-drift'].includes(row.valueStatus) ||
      !['manual-bound', 'task-bound', 'auto-bound'].includes(row.bindingStatus)) {
    fail('TOKEN_TASK_BINDING_AMBIGUOUS',
      `validation classified the bound token as ${row ? `${row.bindingStatus}/${row.valueStatus}` : 'absent'}; the mapping is not publishable`)
  }
  if (row.valueStatus === 'value-drift') {
    process.stderr.write(`[finalize-tokens] ${binding.observedTokenKey} publishes as value-drift; the comparison will carry it honestly\n`)
  }

  if (mutated) {
    draft.revision = registryDocument.revision + 1
    failpoint('before-token-mapping-publish', 'tokens')
    const bytes = writeTokenRegistry(draft)
    failpoint('after-token-mapping-publish', 'tokens')
    marker.artifacts.tokenMappingHash = sha256(bytes)
    marker.artifacts.tokenMappingRevision = draft.revision
    writeMarker(marker)
  }
  return 'succeeded'
}

function effectShip(marker, scriptSet) {
  verifyFigmaConfig(marker)
  if (marker.figma.enabled) requireFigmaNodeRuntime()
  const state = taskArtifacts(marker.stem)
  if (state.present.todo) {
    canonicalDoneCandidate(marker.stem, readSnapshot(marker))
    if (ACTIVE_MUTEX_LEASE) assertMutexHeld(ACTIVE_MUTEX_LEASE)
    const result = spawnSync(process.execPath, [scriptSet.ship, marker.stem], {
      cwd: PROJECT_ROOT, stdio: JSON_MODE ? ['ignore', 2, 2] : 'inherit',
      env: {
        ...process.env,
        FIGMA_PIPELINE_RUN_ID: marker.figma.pipelineRunId || process.env.FIGMA_PIPELINE_RUN_ID || '',
        FINALIZE_PROJECT_ROOT: PROJECT_ROOT,
        FINALIZE_STATE_DIR: CACHE_DIR,
        FINALIZE_TRANSACTION_ID: marker.transactionId,
      },
    })
    if (ACTIVE_MUTEX_LEASE) assertMutexHeld(ACTIVE_MUTEX_LEASE)
    if (result.error) fail('SHIP_FAILED', `ship-done could not start: ${result.error.message}`)
    if (result.status === 2) fail('SHIP_BLOCKED', 'ship-done blocked on a required gate; keep the task in the repair iteration and resume finalization afterwards', 2)
    if (result.status !== 0) fail('SHIP_FAILED', `ship-done exited with ${result.status == null ? 1 : result.status}`)
  } else if (!(state.columns.length === 1 && state.columns[0] === 'done')) {
    fail('TASK_COLUMN_CONFLICT', `ship phase expected todo/ or recovered done/; found ${state.columns.join(', ') || 'nothing'}`)
  }
  // A crash after ship-done's atomic todo->private-proof rename can leave only
  // done visible plus the private proof. Reconcile it before requiring the
  // single-column postcondition; a foreign replacement is restored/preserved.
  reconcileShipDetach(marker)
  const doneState = requireOnlyColumn(marker.stem, 'done')
  const doneBytes = readRegular(doneState.paths.done, { code: 'DONE_TASK_CHANGED', maxBytes: TASK_MAX_BYTES })
  if (sha256(logicalTaskText(doneBytes.toString('utf8'))) !== marker.source.intendedLogicalHash) {
    fail('DONE_TASK_CHANGED', 'done task differs from the finalization intent (excluding only the code-emitted Figma meta line)')
  }
  marker.artifacts.doneHash = sha256(doneBytes)
  recordTokenReceiptArtifacts(marker)
  // Filesystem postcondition is checked before any derived state is published.
  // The marker remains durable and the task lock remains held on failure.
  canonicalFinalizePost(marker.stem, false)
}
function tokenReceiptDirectory(marker) {
  return join(TASKS_DIR, 'evidence', 'figma-ship', marker.stem)
}
function recordTokenReceiptArtifacts(marker) {
  const directory = tokenReceiptDirectory(marker)
  const manifestPath = join(directory, 'token-observations-manifest.json')
  if (!regularFile(manifestPath, { code: 'TOKEN_TASK_RECEIPT_INVALID', maxBytes: 1024 * 1024 })) return false
  let receipt
  try {
    receipt = validateCommittedTaskObservationReceipt({
      taskStem: marker.stem,
      transactionId: marker.transactionId,
      receiptDirectory: directory,
    })
  } catch (error) {
    fail('TOKEN_TASK_RECEIPT_INVALID', error.message)
  }
  const intentPath = join(directory, 'token-source-ingestion-intent.json')
  const intentBytes = readRegular(intentPath, { code: 'TOKEN_SOURCE_INGESTION_INTENT_INVALID', maxBytes: 1024 * 1024 })
  let intent
  try {
    intent = JSON.parse(intentBytes.toString('utf8'))
    validateTaskIngestionIntent(intent)
  } catch (error) {
    fail('TOKEN_SOURCE_INGESTION_INTENT_INVALID', error.message)
  }
  if (intent.taskStem !== marker.stem || intent.originTransactionId !== marker.transactionId ||
      intent.receiptManifestHash !== receipt.manifestHash) {
    fail('TOKEN_SOURCE_INGESTION_INTENT_INVALID', 'intent does not bind the finalization receipt')
  }
  marker.artifacts.tokenObservationManifestHash = receipt.manifestHash
  marker.artifacts.tokenIngestionIntentId = intent.intentId
  marker.artifacts.tokenIngestionIntentHash = sha256(intentBytes)
  return true
}
function effectIndex(marker, scriptSet) {
  const python = process.env.FINALIZE_PYTHON || 'python3'
  runChild('INDEX_REGEN_FAILED', python, [scriptSet.index])
  runChild('INDEX_CHECK_FAILED', python, [scriptSet.index, '--check'])
  verifyIndexStem(marker.stem)
  marker.artifacts.indexHash = sha256(readRegular(join(TASKS_DIR, 'INDEX.json'), { code: 'INDEX_INVALID', maxBytes: 16 * 1024 * 1024 }))
  canonicalFinalizePost(marker.stem, true)
}
function effectArch(marker, scriptSet) {
  const python = process.env.FINALIZE_PYTHON || 'python3'
  runChild('ARCH_REGEN_FAILED', python, [
    scriptSet.arch,
    '--trigger', 'task-finalization',
    '--trigger-id', marker.transactionId,
    '--task-stem', marker.stem
  ])
  runChild('ARCH_CHECK_FAILED', python, [scriptSet.arch, '--check'])
  const archPath = join(PROJECT_ROOT, 'orchestrator', '.arch-map.json')
  marker.artifacts.archHash = regularFile(archPath, { code: 'ARCH_MAP_INVALID', maxBytes: 16 * 1024 * 1024 }) ? sha256(readRegular(archPath, { code: 'ARCH_MAP_INVALID', maxBytes: 16 * 1024 * 1024 })) : null
}
// Mandatory-test interlock (improvement 05 §19.3). Every new todo -> done
// transaction binds the exact active lock and pre-Outcome task bytes to the
// deterministic summary graph before it publishes a marker or snapshot.
// Recovery rechecks the complete binding and current source snapshot.
function outcomeTestsGate(done) {
  const section = outcomeSectionLines(done, 'Build gates')
  if (!section) fail('OUTCOME_TESTS_GATE_INVALID', 'Outcome Build gates section is structurally unavailable')
  // The published row and the row this gate reads must be the same row: a
  // fenced or indented near-miss would let the human-visible Outcome disagree
  // with the machine verdict instead of failing closed.
  const matches = []
  let fenced = false
  for (const line of section) {
    if (/^\s{0,3}(```|~~~)/.test(line)) { fenced = !fenced; continue }
    if (!/`tests`[ \t]+—/.test(line)) continue
    if (fenced) fail('OUTCOME_TESTS_GATE_INVALID', 'the `tests` build-gate row must not live inside a fenced block')
    const match = /^- `tests` — (pass|fail|skipped)(?: \(([^()\r\n]{1,200})\))?$/.exec(line)
    if (!match) {
      fail('OUTCOME_TESTS_GATE_INVALID',
        'a `tests` build-gate row does not match the frozen grammar: ' + line.trim().slice(0, 120))
    }
    matches.push(match)
  }
  if (matches.length > 1) fail('OUTCOME_TESTS_GATE_INVALID', 'Outcome must contain exactly one `tests` build-gate row')
  if (matches.length === 0) return null
  const verdict = matches[0][1]
  const reason = matches[0][2] || null
  if (verdict === 'skipped' && !/^test-not-applicable: [a-z][a-z0-9-]{0,79}$/.test(String(reason || ''))) {
    fail('OUTCOME_TESTS_GATE_INVALID', 'a skipped tests gate requires `test-not-applicable: <policy-enum>`')
  }
  return { verdict, reason }
}
function testEvidenceLoader(stem, runId) {
  const base = join(TEST_CERTIFICATION_DIR, stem, runId)
  const fixed = {
    'test-summary': 'summary.json',
    'test-policy': 'policy.json',
    'source-snapshot': 'source-snapshot.json',
    'test-impact-planned': 'planned-impact.json',
    'test-impact-observed': 'observed-impact.json',
  }
  return (kind, hash) => {
    const candidates = []
    if (fixed[kind]) candidates.push(join(base, fixed[kind]))
    else if (kind === 'test-command' || kind === 'test-structural-gate') {
      const directory = join(base, kind === 'test-command' ? 'commands' : 'structural')
      let names
      try { names = readdirSync(directory) } catch (_) { return null }
      if (names.length > 640) return null
      const suffix = `-${hash.slice('sha256:'.length)}.json`
      for (const name of names) if (name.endsWith(suffix)) candidates.push(join(directory, name))
    }
    if (candidates.length !== 1) return null
    try {
      return JSON.parse(readRegular(candidates[0], {
        code: 'TEST_EVIDENCE_STALE', maxBytes: 2 * 1024 * 1024,
      }).toString('utf8'))
    } catch (_) { return null }
  }
}
function currentTestLock(stem) {
  const snapshot = readRegularSnapshot(lockPath(stem), { code: 'TEST_EVIDENCE_LOCK_STALE', maxBytes: 1024 * 1024 })
  let record
  try { record = JSON.parse(snapshot.bytes.toString('utf8')) }
  catch (e) { fail('TEST_EVIDENCE_LOCK_STALE', `active task lock is not valid JSON: ${e.message}`) }
  if (!taskState.canonicalLockV1(record, stem) || record.stage !== 'orchestrator') {
    fail('TEST_EVIDENCE_LOCK_STALE', 'active task lock is not the canonical orchestrator owner')
  }
  return { record, hash: sha256(snapshot.bytes) }
}
function testBindingArtifacts(artifacts) {
  return Object.fromEntries(TEST_BINDING_KEYS
    .filter((key) => artifacts && artifacts[key] !== undefined)
    .map((key) => [key, artifacts[key]]))
}
function validateTestEvidence(stem, done, expectedBinding = null, activeLock = null) {
  const gate = outcomeTestsGate(done)
  if (gate === null) fail('OUTCOME_TESTS_GATE_MISSING', 'new finalization requires a `tests` build-gate row backed by sealed evidence')
  if (gate.verdict === 'fail') fail('TEST_EVIDENCE_FAILED', 'a failing tests gate is never publishable; fix or descope before finalization')
  const lock = activeLock || currentTestLock(stem)
  const runId = expectedBinding ? expectedBinding.testRunId : lock.record.runId
  if (!TEST_RUN_ID_RE.test(String(runId || ''))) fail('TEST_EVIDENCE_STALE', 'active lock run id cannot address a certification summary')
  const summaryPath = join(TEST_CERTIFICATION_DIR, stem, runId, 'summary.json')
  const summaryBytes = readRegular(summaryPath, { code: 'TEST_EVIDENCE_STALE', maxBytes: 1024 * 1024 })
  let summary
  try { summary = testSummaryContract.validateSummary(JSON.parse(summaryBytes.toString('utf8'))) }
  catch (e) { fail('TEST_EVIDENCE_STALE', `sealed test summary is invalid: ${String(e.message).slice(0, 200)}`) }
  if (summary.taskStem !== stem || summary.runId !== runId ||
      summary.sessionId !== lock.record.sessionId || lock.record.runId !== runId) {
    fail('TEST_EVIDENCE_STALE', 'sealed test summary belongs to another task, run, session, or active lock')
  }
  const transitive = testReceiptRegistry.verifyReceiptId(
    testReceiptRegistry.receiptIdOf('test-summary', summary.summaryHash),
    testEvidenceLoader(stem, runId),
  )
  if (!transitive.verified) {
    fail('TEST_EVIDENCE_STALE', `sealed test summary evidence graph is invalid: ${transitive.code}`)
  }
  summary = transitive.receipt
  const taskInputHash = testInputContract.taskInputHashOf(Buffer.from(done))
  if (summary.taskInputHash !== taskInputHash) {
    fail('TEST_EVIDENCE_STALE', 'sealed summary task input does not match the exact current pre-Outcome task bytes')
  }
  const sourceManifest = testEvidenceLoader(stem, runId)('source-snapshot', summary.sourceSnapshotHash)
  let snapshotVerification
  try { snapshotVerification = testSnapshotContract.verifySnapshot(sourceManifest, { root: PROJECT_ROOT }) }
  catch (e) { fail('TEST_EVIDENCE_STALE', `sealed source snapshot cannot be reverified: ${String(e.message).slice(0, 200)}`) }
  if (!snapshotVerification.ok) fail('TEST_EVIDENCE_STALE', 'sealed source snapshot is no longer current')
  const expected = summary.verdict === 'PASS' ? 'pass' : summary.verdict === 'SKIPPED' ? 'skipped' : null
  if (expected === null) fail('TEST_EVIDENCE_FAILED', `sealed summary verdict ${summary.verdict} is not publishable`)
  if (gate.verdict !== expected) fail('OUTCOME_TESTS_GATE_MISMATCH', `Outcome tests gate says ${gate.verdict} but the sealed summary says ${summary.verdict}`)
  if (expected === 'skipped') {
    const reason = summary.verdictReasons.find((value) => value.startsWith('test-not-applicable-'))
    const expectedReason = reason ? `test-not-applicable: ${reason.slice('test-not-applicable-'.length)}` : null
    if (gate.reason !== expectedReason) {
      fail('OUTCOME_TESTS_GATE_MISMATCH', 'Outcome skipped reason does not match the typed sealed summary reason')
    }
  }
  const binding = {
    testLockHash: lock.hash,
    testPolicyHash: summary.policyHash,
    testRunId: summary.runId,
    testSessionId: summary.sessionId,
    testSourceSnapshotHash: summary.sourceSnapshotHash,
    testSummaryHash: summary.summaryHash,
    testTaskInputHash: summary.taskInputHash,
  }
  if (expectedBinding && TEST_BINDING_KEYS.some((key) => expectedBinding[key] !== binding[key])) {
    fail('TEST_EVIDENCE_STALE', 'sealed test evidence no longer matches the transaction binding')
  }
  return binding
}
function verifyTestEvidence(marker, done) {
  const binding = testBindingArtifacts(marker.artifacts)
  if (Object.keys(binding).length !== TEST_BINDING_KEYS.length) {
    fail('TEST_EVIDENCE_UNBOUND', 'finalization marker has no complete sealed test evidence binding')
  }
  let lock
  if (regularFile(lockPath(marker.stem), { code: 'TEST_EVIDENCE_LOCK_STALE', maxBytes: 1024 * 1024 })) {
    const observed = readRegularSnapshot(lockPath(marker.stem), {
      code: 'TEST_EVIDENCE_LOCK_STALE', maxBytes: 1024 * 1024,
    })
    if (sha256(observed.bytes) === binding.testLockHash) {
      lock = currentTestLock(marker.stem)
    } else {
      // Lock ownership is enforced by the finalizer's stronger inode-bound
      // unlock protocol. Do not let a foreign replacement hide its canonical
      // LOCK_OWNERSHIP_CONFLICT behind an evidence-context error.
      lock = {
        record: { runId: binding.testRunId, sessionId: binding.testSessionId },
        hash: binding.testLockHash,
      }
    }
  } else {
    // The inode-bound unlock protocol is the sole authority for distinguishing
    // its own detached lock from disappearance or a foreign replacement. The
    // evidence layer keeps verifying the marker-frozen semantic identity and
    // lets unlock surface LOCK_DISAPPEARED/LOCK_OWNERSHIP_CONFLICT precisely.
    lock = {
      record: { runId: binding.testRunId, sessionId: binding.testSessionId },
      hash: binding.testLockHash,
    }
  }
  validateTestEvidence(marker.stem, done, binding, lock)
}
function effectVerify(marker, scriptSet) {
  verifyFigmaConfig(marker)
  const doneState = requireOnlyColumn(marker.stem, 'done')
  const doneBytes = readRegular(doneState.paths.done, { code: 'DONE_TASK_CHANGED', maxBytes: TASK_MAX_BYTES })
  const done = doneBytes.toString('utf8')
  if (sha256(logicalTaskText(done)) !== marker.source.intendedLogicalHash) fail('DONE_TASK_CHANGED', 'done task changed after ship')
  if (!HASH_RE.test(String(marker.artifacts.doneHash || '')) || sha256(doneBytes) !== marker.artifacts.doneHash) {
    fail('DONE_TASK_CHANGED', 'done task bytes no longer match the exact artifact published by the ship phase')
  }
  const shapeError = outcomeShapeError(done)
  if (shapeError) fail('OUTCOME_INVALID', `done Outcome appendix is malformed: ${shapeError}`)
  verifyTestEvidence(marker, done)
  const python = process.env.FINALIZE_PYTHON || 'python3'
  runChild('INDEX_CHECK_FAILED', python, [scriptSet.index, '--check'])
  verifyIndexStem(marker.stem)
  runChild('ARCH_CHECK_FAILED', python, [scriptSet.arch, '--check'])
  if (marker.artifacts.componentMappingHash !== undefined) {
    const componentMappingBytes = readRegular(COMPONENT_MAPPINGS_PATH(), { code: 'COMPONENT_MAPPING_PUBLICATION_RECOVERY_REQUIRED', maxBytes: 8 * 1024 * 1024 })
    if (sha256(componentMappingBytes) !== marker.artifacts.componentMappingHash) {
      fail('COMPONENT_MAPPING_PUBLICATION_RECOVERY_REQUIRED', 'component-mappings.json changed after this transaction published it')
    }
  }
  if (marker.artifacts.tokenMappingHash !== undefined) {
    const mappingBytes = readRegular(TOKEN_MAPPINGS_PATH(), { code: 'TOKEN_MAPPING_PUBLICATION_RECOVERY_REQUIRED', maxBytes: 8 * 1024 * 1024 })
    if (sha256(mappingBytes) !== marker.artifacts.tokenMappingHash) {
      fail('TOKEN_MAPPING_PUBLICATION_RECOVERY_REQUIRED', 'token-mappings.json changed after this transaction published it')
    }
  }
  if (marker.artifacts.tokenObservationManifestHash !== undefined) {
    const before = {
      tokenObservationManifestHash: marker.artifacts.tokenObservationManifestHash,
      tokenIngestionIntentId: marker.artifacts.tokenIngestionIntentId,
      tokenIngestionIntentHash: marker.artifacts.tokenIngestionIntentHash,
    }
    recordTokenReceiptArtifacts(marker)
    if (Object.keys(before).some((key) => marker.artifacts[key] !== before[key])) {
      fail('TOKEN_TASK_RECEIPT_CHANGED', 'token observation receipt or ingestion intent changed after ship')
    }
  }
  if (marker.figma.enabled) {
    runChild('FIGMA_DONE_VERIFY_FAILED', process.execPath, [scriptSet.verifyDone, '--stem', marker.stem])
  }
  canonicalFinalizePost(marker.stem, true)
  marker.artifacts.verifiedAt = now()
}
function assertCapturedLock(path, stat, captured) {
  if (!captured.present) fail('LOCK_OWNERSHIP_CONFLICT', 'a task lock appeared after finalization started; refusing to remove it')
  const current = readIdentity(path, 'UNSAFE_TASK_LOCK')
  if (!sameOwnedFileGeneration(current, captured) || String(stat.dev) !== current.dev || String(stat.ino) !== current.ino) {
    fail('LOCK_OWNERSHIP_CONFLICT', 'task lock generation changed after finalization started; refusing to remove another owner\'s lock')
  }
  return current
}
function restoreDetachedLockConflict(proof, path) {
  if (regularFile(path, { code: 'UNSAFE_TASK_LOCK', maxBytes: 1024 * 1024 })) return false
  const retained = readIdentity(proof, 'UNSAFE_UNLOCK_PROOF')
  try {
    boundary('link', { source: proof, target: path, expected: retained, maxBytes: 1024 * 1024 })
    return true
  } catch (e) {
    if (e && e.code === 'EEXIST') return false
    fail('LOCK_RESTORE_FAILED', `detached conflicting lock is preserved at ${proof}, but could not be restored to ${path}: ${e.message}`)
  }
}
function restoreMovedLockConflict(privatePath, path) {
  if (!entryStat(path, true).missing) return false
  const retained = readIdentity(privatePath, 'UNSAFE_UNLOCK_SOURCE')
  try {
    boundary('move', { source: privatePath, target: path, expected: retained, maxBytes: 1024 * 1024 })
    return true
  } catch (e) {
    if (e && e.code === 'EEXIST') return false
    fail('LOCK_RESTORE_FAILED', `detached conflicting lock is preserved at ${privatePath}, but could not be restored to ${path}: ${e.message}`)
  }
}
function validateDetachedLockOrRestore(proof, proofStat, path, captured, moved = false) {
  try { assertCapturedLock(proof, proofStat, captured) }
  catch (e) {
    if (moved) restoreMovedLockConflict(proof, path)
    else restoreDetachedLockConflict(proof, path)
    throw e
  }
}
function effectUnlock(marker, scriptSet) {
  // Unlock is the final mutation boundary.  The complete derived-state gate
  // is repeated immediately before the captured owner inode can leave its
  // canonical name; done + INDEX alone cannot fence arch/registry/Figma drift.
  const path = lockPath(marker.stem)
  const proof = unlockProofPath(marker)
  const captured = marker.source.lock || { present: false, hash: null, dev: null, ino: null }
  if (!marker.artifacts.unlockSourceToken) {
    marker.artifacts.unlockSourceToken = randomBytes(16).toString('hex')
    writeMarker(marker)
  }
  const source = unlockSourcePath(marker)

  // Recover a crash after canonical -> private-source rename. The source name
  // is random and write-ahead persisted; proof publication below is
  // no-clobber, so neither a surviving retry nor a racing artifact is replaced.
  const sourceEntry = entryStat(source, true)
  if (!sourceEntry.missing) {
    const sourceAny = sourceEntry.stat
    if (sourceAny.kind !== 'file' || sourceAny.size > 1024 * 1024) {
      restoreMovedLockConflict(source, path)
      fail('UNSAFE_UNLOCK_SOURCE', `${source} contains a symlink, special file, or oversized racing lock; it was preserved/restored`)
    }
    const sourceStat = regularFile(source, { required: true, code: 'UNSAFE_UNLOCK_SOURCE', maxBytes: 1024 * 1024 })
    validateDetachedLockOrRestore(source, sourceStat, path, captured, true)
    const recoveryProof = regularFile(proof, { code: 'UNSAFE_UNLOCK_PROOF', maxBytes: 1024 * 1024 })
    if (!recoveryProof) fail('LOCK_DISAPPEARED', 'detached lock source exists without the no-clobber ownership proof; refusing to infer ownership')
    validateDetachedLockOrRestore(proof, recoveryProof, path, captured)
    if (regularFile(path, { code: 'UNSAFE_TASK_LOCK', maxBytes: 1024 * 1024 })) {
      fail('LOCK_REAPPEARED', 'a new task lock appeared while the owned lock was detached; both locks are preserved')
    }
    try { effectVerify(marker, scriptSet) }
    catch (error) {
      restoreMovedLockConflict(source, path)
      throw error
    }
    validateDetachedLockOrRestore(source,
      regularFile(source, { required: true, code: 'UNSAFE_UNLOCK_SOURCE', maxBytes: 1024 * 1024 }),
      path, captured, true)
    const ownedSource = readIdentity(source, 'UNSAFE_UNLOCK_SOURCE')
    removeFileDurable(source, ownedSource, 'LOCK_OWNERSHIP_CONFLICT')
  }

  let currentStat = regularFile(path, { code: 'UNSAFE_TASK_LOCK', maxBytes: 1024 * 1024 })
  let proofStat = regularFile(proof, { code: 'UNSAFE_UNLOCK_PROOF', maxBytes: 1024 * 1024 })

  if (marker.artifacts.unlockDetached === true) {
    if (currentStat) fail('LOCK_REAPPEARED', 'a task lock appeared after the transaction durably detached its owned lock')
    if (proofStat) {
      validateDetachedLockOrRestore(proof, proofStat, path, captured)
      const ownedProof = readIdentity(proof, 'UNSAFE_UNLOCK_PROOF')
      removeFileDurable(proof, ownedProof, 'LOCK_OWNERSHIP_CONFLICT')
    }
    return
  }

  if (!proofStat) {
    if (!currentStat) {
      fail('LOCK_DISAPPEARED', 'the owned task lock and transaction unlock proof are both absent; refusing to infer who removed the lock')
    }
    // First publish a no-clobber hard-link witness. Unlike rename, this cannot
    // overwrite a proof created by a surviving/racing process.
    const currentProof = readIdentity(path, 'UNSAFE_TASK_LOCK')
    try { boundary('link', { source: path, target: proof, expected: currentProof, maxBytes: 1024 * 1024 }) }
    catch (e) { if (!e || e.code !== 'EEXIST') fail('LOCK_PROOF_FAILED', `could not publish the no-clobber lock proof: ${e.message}`) }
    proofStat = regularFile(proof, { required: true, code: 'UNSAFE_UNLOCK_PROOF', maxBytes: 1024 * 1024 })
  }
  validateDetachedLockOrRestore(proof, proofStat, path, captured)

  currentStat = regularFile(path, { code: 'UNSAFE_TASK_LOCK', maxBytes: 1024 * 1024 })
  if (currentStat) {
    if (currentStat.dev !== proofStat.dev || currentStat.ino !== proofStat.ino) {
      fail('LOCK_OWNERSHIP_CONFLICT', 'a different canonical task lock exists beside the transaction proof; both are preserved')
    }
    effectVerify(marker, scriptSet)
    currentStat = regularFile(path, { required: true, code: 'UNSAFE_TASK_LOCK', maxBytes: 1024 * 1024 })
    proofStat = regularFile(proof, { required: true, code: 'UNSAFE_UNLOCK_PROOF', maxBytes: 1024 * 1024 })
    if (currentStat.dev !== proofStat.dev || currentStat.ino !== proofStat.ino) {
      fail('LOCK_OWNERSHIP_CONFLICT', 'task lock changed during the final derived-state verification; every generation was preserved')
    }
    assertCapturedLock(path, currentStat, captured)
    const beforeDetach = readIdentity(path, 'UNSAFE_TASK_LOCK')
    try { boundary('move', { source: path, target: source, expected: beforeDetach, maxBytes: 1024 * 1024 }) }
    catch (e) { fail('LOCK_DETACH_FAILED', `could not atomically detach the canonical task lock: ${e.message}`) }
    const movedEntry = entryStat(source, false)
    const movedAny = movedEntry.stat
    if (movedAny.kind !== 'file' || movedAny.size > 1024 * 1024) {
      restoreMovedLockConflict(source, path)
      fail('UNSAFE_UNLOCK_SOURCE', 'a symlink, special file, or oversized lock raced into the canonical path; it was preserved/restored')
    }
    const movedStat = regularFile(source, { required: true, code: 'UNSAFE_UNLOCK_SOURCE', maxBytes: 1024 * 1024 })
    validateDetachedLockOrRestore(source, movedStat, path, captured, true)
    if (regularFile(path, { code: 'UNSAFE_TASK_LOCK', maxBytes: 1024 * 1024 })) {
      fail('LOCK_REAPPEARED', 'a new task lock appeared while the owned lock was being detached; both locks are preserved')
    }
    failpoint('after-detach', 'unlock')
    const ownedSource = readIdentity(source, 'UNSAFE_UNLOCK_SOURCE')
    removeFileDurable(source, ownedSource, 'LOCK_OWNERSHIP_CONFLICT')
  } else {
    // A crash may have removed the private move-source after publishing the
    // no-clobber hard-link proof but before durably recording detachment.
    // Re-run every gate before accepting that recoverable absence; on failure
    // restore the exact owned inode to the canonical lock name.
    try { effectVerify(marker, scriptSet) }
    catch (error) {
      restoreDetachedLockConflict(proof, path)
      throw error
    }
    if (regularFile(path, { code: 'UNSAFE_TASK_LOCK', maxBytes: 1024 * 1024 })) {
      fail('LOCK_REAPPEARED', 'a task lock appeared during final derived-state verification; it is preserved')
    }
  }

  if (regularFile(path, { code: 'UNSAFE_TASK_LOCK', maxBytes: 1024 * 1024 })) {
    fail('LOCK_REAPPEARED', 'a new task lock appeared before unlock proof commit; it is preserved')
  }
  // Persist proof of detachment while the owned inode still exists at the
  // no-clobber proof path. Once durable, absence of canonical lock is owned.
  marker.artifacts.unlockDetached = true
  writeMarker(marker)
  failpoint('after-proof', 'unlock')
  const ownedProof = readIdentity(proof, 'UNSAFE_UNLOCK_PROOF')
  removeFileDurable(proof, ownedProof, 'LOCK_OWNERSHIP_CONFLICT')
}
function verifyLockAbsent(marker) {
  if (regularFile(lockPath(marker.stem), { code: 'UNSAFE_TASK_LOCK', maxBytes: 1024 * 1024 })) {
    fail('LOCK_REAPPEARED', 'a task lock exists after the ownership-safe unlock phase; refusing to remove recovery authority')
  }
}

function removeShipProofs(marker) {
  reconcileShipDetach(marker)
  const publication = shipPublicationPath(marker)
  const candidate = regularFile(publication, { code: 'SHIP_PROOF_INVALID', maxBytes: TASK_MAX_BYTES })
  if (candidate) {
    const state = requireOnlyColumn(marker.stem, 'done')
    const done = regularFile(state.paths.done, { required: true, code: 'DONE_TASK_CHANGED', maxBytes: TASK_MAX_BYTES })
    const doneBytes = readRegular(state.paths.done, { code: 'DONE_TASK_CHANGED', maxBytes: TASK_MAX_BYTES })
    if (candidate.dev !== done.dev || candidate.ino !== done.ino ||
        !HASH_RE.test(String(marker.artifacts.doneHash || '')) || sha256(doneBytes) !== marker.artifacts.doneHash) {
      fail('SHIP_PROOF_CONFLICT', 'transaction publication proof no longer identifies the exact verified done artifact')
    }
    const proof = readIdentity(publication, 'SHIP_PROOF_INVALID')
    if (proof.dev !== String(done.dev) || proof.ino !== String(done.ino) || proof.hash !== marker.artifacts.doneHash) {
      fail('SHIP_PROOF_CONFLICT', 'transaction publication proof changed before exact cleanup')
    }
    removeFileDurable(publication, proof, 'SHIP_PROOF_CHANGED')
  }
  removeShipTempAliases(marker)
  removeReceiptStage(marker)
}

function completedWithoutMarker(stem, scriptSet) {
  const state = requireOnlyColumn(stem, 'done')
  if (regularFile(lockPath(stem), { code: 'UNSAFE_TASK_LOCK', maxBytes: 1024 * 1024 })) {
    fail('DONE_WITH_LOCK_NO_MARKER', `${stem} is already in done/ but still has a lock and no ownership marker; refusing to remove it`)
  }
  const done = readRegular(state.paths.done, { code: 'DONE_TASK_CHANGED', maxBytes: TASK_MAX_BYTES }).toString('utf8')
  const shapeError = outcomeShapeError(done)
  if (shapeError) fail('OUTCOME_INVALID', `done Outcome appendix is malformed: ${shapeError}`)
  const python = process.env.FINALIZE_PYTHON || 'python3'
  runChild('INDEX_CHECK_FAILED', python, [scriptSet.index, '--check'])
  verifyIndexStem(stem)
  canonicalValidate(stem, { expect: 'done', checkIndex: true })
  runChild('ARCH_CHECK_FAILED', python, [scriptSet.arch, '--check'])
  if (readConfigBoolean('figmaEnabled')) {
    runChild('FIGMA_DONE_VERIFY_FAILED', process.execPath, [scriptSet.verifyDone, '--stem', stem])
  }
  return { alreadyComplete: true, stem }
}

function parseCli(argv) {
  const out = { stem: '', outcomeFile: '', writerSessionId: '', json: false, status: false, list: false,
    tokenFixtureOptions: {}, componentFixtureOptions: {} }
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i]
    if (arg === '--outcome-file') { out.outcomeFile = argv[++i] || ''; if (!out.outcomeFile) fail('USAGE', '--outcome-file needs a path') }
    else if (arg === '--writer-session-id') {
      out.writerSessionId = argv[++i] || ''
      if (!writerLeases.SESSION_ID_RE.test(out.writerSessionId)) fail('USAGE', '--writer-session-id needs a valid ws-* writer session id')
    }
    else if (arg === '--json') out.json = true
    else if (arg === '--status') out.status = true
    else if (arg === '--list-incomplete') out.list = true
    else if (arg === '--fixture-observed-token-catalog') {
      out.tokenFixtureOptions.fixtureCatalogFile = argv[++i] || ''
      if (!out.tokenFixtureOptions.fixtureCatalogFile) fail('USAGE', '--fixture-observed-token-catalog needs a path')
    }
    else if (arg === '--fixture-observed-token-source-index') {
      out.tokenFixtureOptions.fixtureSourceIndexFile = argv[++i] || ''
      if (!out.tokenFixtureOptions.fixtureSourceIndexFile) fail('USAGE', '--fixture-observed-token-source-index needs a path')
    }
    else if (arg === '--fixture-token-binding-snapshot') {
      out.tokenFixtureOptions.fixtureBindingSnapshotFile = argv[++i] || ''
      if (!out.tokenFixtureOptions.fixtureBindingSnapshotFile) fail('USAGE', '--fixture-token-binding-snapshot needs a path')
    }
    else if (arg === '--fixture-token-analysis-index') {
      out.tokenFixtureOptions.fixtureAnalysisIndexFile = argv[++i] || ''
      if (!out.tokenFixtureOptions.fixtureAnalysisIndexFile) fail('USAGE', '--fixture-token-analysis-index needs a path')
    }
    else if (arg === '--fixture-component-inventory') {
      out.componentFixtureOptions.fixtureInventoryFile = argv[++i] || ''
      if (!out.componentFixtureOptions.fixtureInventoryFile) fail('USAGE', '--fixture-component-inventory needs a path')
    }
    else if (arg === '--fixture-component-analysis-index') {
      out.componentFixtureOptions.fixtureAnalysisIndexFile = argv[++i] || ''
      if (!out.componentFixtureOptions.fixtureAnalysisIndexFile) fail('USAGE', '--fixture-component-analysis-index needs a path')
    }
    else if (arg === '--fixture-component-analysis-dir') {
      out.componentFixtureOptions.fixtureAnalysisDirectory = argv[++i] || ''
      if (!out.componentFixtureOptions.fixtureAnalysisDirectory) fail('USAGE', '--fixture-component-analysis-dir needs a path')
    }
    else if (arg.startsWith('--')) fail('USAGE', `unknown argument ${arg}`)
    else if (!out.stem) out.stem = arg
    else fail('USAGE', `unexpected positional argument ${arg}`)
  }
  if (out.list) {
    if (out.stem || out.status || out.outcomeFile || out.writerSessionId ||
        Object.keys(out.tokenFixtureOptions).length || Object.keys(out.componentFixtureOptions).length) {
      fail('USAGE', '--list-incomplete does not accept a stem, mutation options, or fixture inputs')
    }
  } else validateStem(out.stem)
  if (out.status && out.outcomeFile) fail('USAGE', '--status cannot be combined with --outcome-file')
  if (out.status && out.writerSessionId) fail('USAGE', '--status cannot be combined with --writer-session-id')
  if (out.status && (Object.keys(out.tokenFixtureOptions).length || Object.keys(out.componentFixtureOptions).length)) {
    fail('USAGE', '--status cannot be combined with fixture inputs')
  }
  return out
}

async function finalize(cli) {
  const invocationId = randomBytes(12).toString('hex')
  const lease = await acquireMutex(invocationId)
  ACTIVE_MUTEX_LEASE = lease
  let marker = null
  let markerCreated = false
  try {
    assertMutexHeld(lease)
    reconcileReplaceDirectory(CACHE_DIR, MARKER_MAX_BYTES, 'FINALIZATION_MARKER_RECOVERY_REQUIRED')
    assertMutexHeld(lease)
    // A writer that publishes after the mutex must see it on its mandatory
    // post-lease re-check and withdraw; a lease already visible here predates
    // the mutex and therefore wins the handshake.
    const writerSessionId = assertWriterQuiescence(cli.stem, cli.writerSessionId)
    assertCreationQuiescence()
    assertEditQuiescence(cli.stem)
    assertFinalizationClaimAvailable(cli.stem, invocationId)
    assertMutexHeld(lease)
    const path = markerPath(cli.stem)
    if (regularFile(path, { code: 'UNSAFE_MARKER', maxBytes: MARKER_MAX_BYTES })) {
      marker = readMarker(cli.stem).marker
      reconcileInterruptedShip(marker)
      if (marker.status === 'completed') {
        if (cli.outcomeFile) assertLateOutcomeMatches(cli.stem, cli.outcomeFile)
        const completedScripts = scripts()
        effectVerify(marker, completedScripts)
        await settleProcessEvents()
        assertMutexHeld(lease)
        verifyLockAbsent(marker)
        removeShipProofs(marker)
        removeTaskSnapshots(cli.stem)
        removeOwnedMarker(marker)
        return { stem: cli.stem, recoveredCompletedMarker: true }
      }
      marker.owner = ownerRecord(invocationId)
      marker.status = 'incomplete'
      writeMarker(marker)
      refreshPreShipIntent(marker, cli.outcomeFile)
      assertMutexHeld(lease)
    } else {
      marker = createMarker(cli.stem, cli.outcomeFile, invocationId, writerSessionId)
      assertMutexHeld(lease)
      if (!marker) {
        const completed = completedWithoutMarker(cli.stem, scripts())
        await settleProcessEvents()
        assertMutexHeld(lease)
        return completed
      }
      markerCreated = true
    }

    const scriptSet = scripts()
    for (const phase of PHASES.slice(0, -1)) {
      await settleProcessEvents()
      assertMutexHeld(lease)
      if (markerPhaseDone(marker, phase)) continue
      beginPhase(marker, phase, invocationId)
      let phaseState = 'succeeded'
      if (phase === 'outcome') effectOutcome(marker)
      else if (phase === 'components') phaseState = await effectComponents(marker, cli.componentFixtureOptions)
      else if (phase === 'tokens') phaseState = await effectTokens(marker, cli.tokenFixtureOptions)
      else if (phase === 'ship') effectShip(marker, scriptSet)
      else if (phase === 'index') effectIndex(marker, scriptSet)
      else if (phase === 'arch') effectArch(marker, scriptSet)
      else if (phase === 'verify') effectVerify(marker, scriptSet)
      else if (phase === 'unlock') effectUnlock(marker, scriptSet)
      await settleProcessEvents()
      assertMutexHeld(lease)
      failpoint('after-effect', phase)
      finishPhase(marker, phase, phaseState, phaseState === 'skipped' ? 'not applicable' : null)
    }

    assertMutexHeld(lease)
    beginPhase(marker, 'cleanup', invocationId)
    // Re-check after unlock immediately before deleting recovery authority.
    // This catches a concurrent derived-artifact edit or a newly-created lock.
    effectVerify(marker, scriptSet)
    await settleProcessEvents()
    assertMutexHeld(lease)
    verifyLockAbsent(marker)
    marker.phases.cleanup = { ...marker.phases.cleanup, state: 'succeeded', finishedAt: now() }
    marker.status = 'completed'
    writeMarker(marker)
    failpoint('after-effect', 'cleanup')
    assertMutexHeld(lease)
    removeShipProofs(marker)
    removeTaskSnapshots(cli.stem)
    failpoint('after-snapshots', 'cleanup')
    removeOwnedMarker(marker)
    return { stem: cli.stem, completed: true, transactionId: marker.transactionId }
  } catch (caught) {
    const e = normalizeFinalizeError(caught)
    let markerPresent = false
    if (marker) {
      try { markerPresent = !entryStat(markerPath(marker.stem), true).missing }
      catch (inspectError) {
        const normalized = normalizeFinalizeError(inspectError)
        console.error(`finalize-task: additionally failed to inspect recovery authority: ${normalized.message}`)
      }
    }
    if (marker && markerPresent) {
      try {
        assertMutexHeld(lease)
        const live = readMarker(marker.stem).marker
        if (live.transactionId !== marker.transactionId || live.revision !== marker.revision) {
          throw new FinalizeError('MARKER_OWNERSHIP_CONFLICT', 'finalization marker generation changed; foreign replacement is preserved')
        }
        const committed = marker.status === 'completed'
        marker.owner = null
        marker.lastError = { code: e.code || 'FINALIZATION_FAILED', message: bounded(e.message), at: now() }
        if (!committed) {
          marker.status = 'incomplete'
          if (marker.phase && marker.phases[marker.phase]) marker.phases[marker.phase] = { ...marker.phases[marker.phase], state: 'failed', finishedAt: now() }
        }
        writeMarker(marker)
      } catch (markerError) {
        console.error(`finalize-task: additionally failed to persist error state: ${markerError.message}`)
      }
    } else if (markerCreated) {
      // Defensive only: marker writes are atomic, so a missing marker means an
      // external actor removed the recovery authority during this invocation.
      console.error('finalize-task: recovery marker disappeared during finalization')
    }
    throw e
  } finally {
    await releaseMutex(lease)
    if (ACTIVE_MUTEX_LEASE === lease) ACTIVE_MUTEX_LEASE = null
  }
}

let cli
try { cli = parseCli(process.argv.slice(2)) }
catch (e) {
  console.error(`finalize-task: ${e.code || 'ERROR'}: ${e.message}`)
  process.exit(e.exitCode || 1)
}
JSON_MODE = cli.json

try { platformSupport.assertCanonicalTaskPlatform() }
catch (e) {
  console.error(`finalize-task: ${e.code || 'ERROR'}: ${e.message}`)
  process.exit(e.exitCode || 3)
}

if (cli.list || cli.status) {
  try {
    if (cli.list) {
      const value = listIncomplete()
      console.log(cli.json ? JSON.stringify(value) : value.map((x) => `${x.stem}\t${x.status}\t${x.phase || '-'}\t${x.errorCode || ''}`).join('\n'))
    } else {
      const replacement = markerReplacementProjection(cli.stem)
      const value = replacement || (regularFile(markerPath(cli.stem), { code: 'UNSAFE_MARKER', maxBytes: MARKER_MAX_BYTES })
        ? safeProjection(cli.stem) : { stem: cli.stem, status: 'none' })
      console.log(cli.json ? JSON.stringify(value) : `${value.stem}: ${value.status}${value.phase ? ` (${value.phase})` : ''}`)
    }
  } catch (e) {
    const error = normalizeFinalizeError(e)
    console.error(`finalize-task: ${error.code || 'ERROR'}: ${error.message}`)
    process.exit(error.exitCode || 1)
  }
} else {
  finalize(cli).then((result) => {
    if (cli.json) console.log(JSON.stringify(result))
    else if (result.alreadyComplete) console.log(`finalize-task: ${cli.stem} is already fully finalized`)
    else console.log(`finalize-task: ${cli.stem} finalized successfully`)
  }).catch((e) => {
    const error = normalizeFinalizeError(e)
    console.error(`finalize-task: ${error.code || 'FINALIZATION_FAILED'}: ${error.message}`)
    process.exit(error.exitCode || 1)
  })
}
