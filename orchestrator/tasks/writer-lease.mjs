#!/usr/bin/env node

// Small CLI for the standby /serve-queue worker. The in-process site uses the
// same writer-leases.cjs contract directly; this wrapper lets a prompt-driven
// standby acquire, exact-verify, renew, and release a lease without
// reimplementing JSON or atomic writes.

import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'
import { hostname } from 'node:os'
import { createHash, randomBytes } from 'node:crypto'
import writerLeases from './writer-leases.cjs'
import creationMarkerContract from './creation-marker-contract.cjs'
import editMarkerContract from './edit-marker-contract.cjs'
import taskSource from './task-source-contract.cjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const PROJECT_ROOT = resolve(process.env.FINALIZE_PROJECT_ROOT || join(HERE, '..', '..'))
const STATE_DIR = resolve(process.env.FINALIZE_STATE_DIR || process.env.ORCHESTRATOR_FINALIZATIONS_DIR ||
  join(PROJECT_ROOT, 'orchestrator', '.cache', 'tasks', 'finalizations'))
const DIR = join(STATE_DIR, '.writers')
const WRITER_AUTHORITY_ROOT = resolve(process.env.FINALIZE_WRITER_AUTHORITY_ROOT ||
  (process.env.FINALIZE_PROJECT_ROOT ? PROJECT_ROOT :
    (process.env.FINALIZE_STATE_DIR || process.env.ORCHESTRATOR_FINALIZATIONS_DIR ? dirname(STATE_DIR) : PROJECT_ROOT)))
const CREATIONS_DIR = resolve(process.env.FINALIZE_CREATIONS_DIR || process.env.ORCHESTRATOR_TASK_CREATIONS_DIR || join(dirname(STATE_DIR), 'creations'))
const EDITS_DIR = resolve(process.env.FINALIZE_EDITS_DIR || process.env.EDIT_BACKLOG_EDITS_DIR || process.env.ORCHESTRATOR_TASK_EDITS_DIR || join(dirname(STATE_DIR), 'edits'))
const HASH_RE = /^sha256:[a-f0-9]{64}$/
const NESTED_PUBLICATION_KEYS = new Set(['task:create-backlog', 'task:recover-backlog-creations'])
const MAX_GUARD_ENTRIES = 10000
const FS_BOUNDARY = join(HERE, 'finalize-lock.py')
const CANONICAL_PROJECT_ROOT = resolve(HERE, '..', '..')

function die(message, status = 1) { console.error(`writer-lease: ${message}`); process.exit(status) }
function positiveInt(raw, fallback, name) {
  if (raw === undefined) return fallback
  const value = Number(raw)
  if (!Number.isInteger(value) || value <= 0) die(`${name} must be a positive integer`)
  return value
}
function flags(args, allowed) {
  const parsed = Object.create(null)
  for (let i = 0; i < args.length; i++) {
    const flag = args[i]
    if (!Object.prototype.hasOwnProperty.call(allowed, flag)) die(`unknown argument: ${flag}`)
    const kind = allowed[flag]
    if (Object.prototype.hasOwnProperty.call(parsed, flag)) die(`duplicate argument: ${flag}`)
    if (kind === 'boolean') {
      parsed[flag] = true
      continue
    }
    if (i + 1 >= args.length || String(args[i + 1]).startsWith('--')) die(`${flag} requires a value`)
    parsed[flag] = args[++i]
  }
  return parsed
}
function boundary(action, values = {}) {
  const request = {
    version: 1, action, authorityRoot: WRITER_AUTHORITY_ROOT,
    canonicalRoot: CANONICAL_PROJECT_ROOT,
    fixture: WRITER_AUTHORITY_ROOT !== CANONICAL_PROJECT_ROOT,
    ...values,
  }
  const result = spawnSync(process.env.FINALIZE_FS_PYTHON || 'python3', [FS_BOUNDARY, 'fs-op'], {
    input: JSON.stringify(request), encoding: 'utf8', maxBuffer: 48 * 1024 * 1024, timeout: 30000,
  })
  if (result.error) throw result.error
  if (result.status !== 0) throw new Error(String(result.stderr || 'filesystem worker failed').trim())
  let envelope
  try { envelope = JSON.parse(result.stdout) } catch { throw new Error('filesystem worker returned an invalid envelope') }
  if (!envelope || envelope.version !== 1 || envelope.ok !== true) {
    const detail = envelope && envelope.error || {}
    const error = new Error(String(detail.message || 'filesystem boundary rejected the operation'))
    error.code = String(detail.code || 'FILESYSTEM_BOUNDARY_FAILED')
    throw error
  }
  return envelope.result
}
function boundedDirectoryNames(directory, limit = MAX_GUARD_ENTRIES) {
  const result = boundary('list', { path: directory, maxEntries: limit, allowMissing: false })
  return result.names
}
function stableRegularJson(file, maxBytes) {
  const result = boundary('read', { path: file, maxBytes })
  return JSON.parse(Buffer.from(result.rawBase64, 'base64').toString('utf8'))
}
function stableRegularBytes(file, maxBytes) {
  const result = boundary('read', { path: file, maxBytes })
  return Buffer.from(result.rawBase64, 'base64')
}
function scanWriterState(kind = 'lease') {
  const started = process.hrtime.bigint()
  let scan
  try { scan = writerLeases.scan(DIR, WRITER_AUTHORITY_ROOT); return scan }
  finally {
    const rawCodes = scan && Array.isArray(scan.issues) ? scan.issues.map((item) => item && item.code).filter(Boolean) : ['WRITER_LEASE_SCAN_THROWN']
    const codes = rawCodes.map((code) => /^[A-Za-z0-9_.:-]{1,80}$/.test(String(code)) ? String(code) : 'WRITER_LEASE_SCAN_INVALID')
    const summary = scan ? { active: scan.active.length, stale: scan.stale.length, codes } : { active: null, stale: null, codes }
    const event = {
      version: 1, event: 'writer-lease-scan', caller: 'writer-lease', kind,
      result: scan && scan.issues.length ? 'blocked' : 'admitted',
      snapshotHash: `sha256:${createHash('sha256').update(JSON.stringify(summary)).digest('hex')}`,
      durationMs: Number((process.hrtime.bigint() - started) / 1000000n),
      findingCodes: [...new Set(codes)].slice(0, 32),
    }
    process.stderr.write(`[writer-lease] ${JSON.stringify(event)}\n`)
  }
}
function finalizationGuardIssue(options = {}) {
  let names
  try {
    names = boundedDirectoryNames(STATE_DIR)
  } catch (error) {
    return { code: 'FINALIZATION_STATE_UNSAFE', message: error && error.message || String(error) }
  }
  const marker = names.filter((name) => name !== '.mutex.json' && name.endsWith('.json')).sort()[0]
  if (marker) return { code: 'FINALIZATION_MARKER_ACTIVE', message: `durable finalization marker exists: ${marker}` }

  const mutexPath = join(STATE_DIR, '.mutex.json')
  let mutex
  try {
    mutex = stableRegularJson(mutexPath, 4096)
  } catch (error) {
    if (error && error.code === 'PATH_MISSING') return publicationMarkerIssue(options)
    return { code: 'FINALIZATION_MUTEX_UNSAFE', message: error && error.message || String(error) }
  }
  const expectedMutexFields = ['hostname', 'invocationId', 'pid', 'processStartId', 'released', 'startedAt', 'version']
  if (!mutex || Object.keys(mutex).sort().join(',') !== expectedMutexFields.join(',') ||
      mutex.version !== 1 || !Number.isInteger(mutex.pid) || mutex.pid <= 0 ||
      typeof mutex.hostname !== 'string' || !mutex.hostname || mutex.hostname.length > 255 ||
      typeof mutex.invocationId !== 'string' || !mutex.invocationId || mutex.invocationId.length > 200 ||
      typeof mutex.startedAt !== 'string' || !Number.isFinite(Date.parse(mutex.startedAt)) ||
      typeof mutex.released !== 'boolean' ||
      (process.platform === 'linux' || process.platform === 'darwin' || process.platform === 'win32'
        ? !writerLeases.PROCESS_START_ID_RE.test(String(mutex.processStartId || ''))
        : mutex.processStartId !== null)) {
    return { code: 'FINALIZATION_MUTEX_UNSAFE', message: 'global finalization mutex owner record is invalid' }
  }
  if (mutex.released) return publicationMarkerIssue(options)
  if (mutex.hostname !== hostname()) {
    return { code: 'FINALIZATION_MUTEX_BUSY', message: 'global finalization mutex has an unprovable remote owner' }
  }
  if (process.platform === 'linux' || process.platform === 'darwin' || process.platform === 'win32') {
    if (writerLeases.processIdentityMatches(mutex.pid, mutex.processStartId)) {
      return { code: 'FINALIZATION_MUTEX_BUSY', message: 'global finalization mutex owner generation is still alive' }
    }
  } else {
    try { process.kill(mutex.pid, 0); return { code: 'FINALIZATION_MUTEX_BUSY', message: 'global finalization mutex owner is still alive' } }
    catch (error) { if (!error || error.code !== 'ESRCH') return { code: 'FINALIZATION_MUTEX_BUSY', message: 'global finalization mutex owner liveness is unknown' } }
  }
  return publicationMarkerIssue(options)
}

function creationIssue(options = {}) {
  const allowedKeyHash = options.creationKeyHash || null
  const allowAllRecovery = options.allowAllCreationRecovery === true
  if (allowedKeyHash !== null && !HASH_RE.test(allowedKeyHash)) {
    return { code: 'CREATION_RECOVERY_KEY_INVALID', message: 'creation recovery key hash is invalid' }
  }
  const allowedName = allowedKeyHash === null ? null : `${allowedKeyHash.slice('sha256:'.length)}.json`
  let names
  try {
    names = boundedDirectoryNames(CREATIONS_DIR)
  } catch (error) {
    if (error && error.code === 'PATH_MISSING') return null
    return { code: 'CREATION_MARKER_DIR_UNSAFE', message: error && error.message || String(error) }
  }
  const setEntries = []
  for (const name of names) {
    const cas = creationMarkerContract.durableCas.classifyName(name)
    if (cas !== null) return {
      code: cas === 'recovery-required' ? 'CREATION_MARKER_CAS_RECOVERY_REQUIRED' : 'CREATION_MARKER_CAS_NAME_UNSAFE',
      message: `durable creation CAS state requires exact recovery: ${name}`
    }
    if (!name.endsWith('.json')) continue
    if (!/^[a-f0-9]{64}\.json$/.test(name)) return { code: 'CREATION_MARKER_NAME_INVALID', message: `unexpected creation marker ${name}` }
    try {
      const file = join(CREATIONS_DIR, name)
      const raw = stableRegularBytes(file, creationMarkerContract.MAX_BYTES)
      const marker = JSON.parse(raw.toString('utf8'))
      creationMarkerContract.validate(marker, name)
      setEntries.push({ filename: name, value: marker })
      if (marker.status !== 'completed' && !allowAllRecovery && name !== allowedName) {
        return { code: 'CREATION_INCOMPLETE', message: `deterministic creation${marker.stem ? ` for ${marker.stem}` : ''} requires recovery` }
      }
    } catch (error) {
      return { code: 'CREATION_MARKER_INVALID', message: error && error.message || String(error) }
    }
  }
  try { creationMarkerContract.validateSet(setEntries) }
  catch (error) { return { code: 'CREATION_MARKER_INVALID', message: error && error.message || String(error) } }
  return null
}

function publicationMarkerIssue(options = {}) {
  const creation = creationIssue(options)
  if (creation) return creation
  return editMarkerContract.blockingIssue(EDITS_DIR, null, false, WRITER_AUTHORITY_ROOT)
}

function deterministicPublicationLease(row) {
  return row && (row.kind === 'runtime-build' || typeof row.key === 'string' && (
    row.key === 'task:create-backlog' ||
    row.key === 'task:recover-backlog-creations' ||
    row.key === 'task:recover-backlog-edits' ||
    row.key.startsWith('task:edit-backlog:')
  ))
}
function delegationHash(token) {
  return `sha256:${createHash('sha256').update(String(token), 'ascii').digest('hex')}`
}

// The lease is already durable when this runs. That ordering is the other half
// of the same handshake used by the in-process server: two racing writers
// cannot both pass, because whichever publishes second observes the first (and
// if both publish before either scan, both fail closed). Deterministic
// create/edit publishers are global because they rewrite shared INDEX state.
function activeWriterIssue(own) {
  const scan = scanWriterState('active-writer-guard')
  if (scan.issues.length) return {
    code: scan.issues[0].code || 'WRITER_LEASE_INVALID',
    message: scan.issues[0].message
  }
  const activeOwn = scan.active.find((row) => row.leaseId === own.leaseId)
  if (!activeOwn) return {
    code: 'WRITER_LEASE_OWNERSHIP_LOST',
    message: 'the newly published writer lease is not active'
  }
  const stem = activeOwn.stem
  const ownIsGlobal = deterministicPublicationLease(activeOwn)
  // Frozen serial safety (pipeline improvement 01, Phase 0): board-task
  // writers are mutually exclusive across stems AND drainers. A guarded
  // task-session acquire therefore loses the publish-then-scan handshake
  // against ANY other live board-task writer — a site runner session, another
  // standby execution, or an orphan surviving a dead site process — mirroring
  // the site's beginMutation predicate. Relaxing this back to per-stem is
  // allowed only atomically with per-task worktree isolation.
  const conflict = scan.active.find((row) => row.leaseId !== activeOwn.leaseId && (
    ownIsGlobal || deterministicPublicationLease(row) ||
    (activeOwn.kind === 'task-session' && row.kind === 'task-session') ||
    (stem && (row.stem === stem || row.key === `task:${stem}`))
  ))
  return conflict ? {
    code: 'WORKSPACE_WRITER_ACTIVE',
    message: `another active writer lease conflicts${conflict.stem ? ` for ${conflict.stem}` : ''} (${conflict.leaseId})`
  } : null
}

// A nested deterministic creator already runs inside one authenticated parent
// task session.  That task-scoped lease proves who may request the split, but
// it cannot serialize allocator/INDEX publication against another task stem.
// Publish a separate global lock-writer first, then admit exactly that guard
// plus one exact parent generation.  This is a publication guard, not a second
// Claude/task session.
function nestedPublicationWriterIssue(guard, parent) {
  const scan = scanWriterState('nested-publication-guard')
  if (scan.issues.length) return {
    code: scan.issues[0].code || 'WRITER_LEASE_INVALID',
    message: scan.issues[0].message
  }
  const guards = scan.active.filter((row) => row.leaseId === guard.leaseId &&
    row.token === guard.token && row.kind === 'lock-writer' && row.stem === null &&
    row.sessionId === parent.sessionId && row.key === parent.publicationKey &&
    row.unverified === false && row.expiresAt === null && row.childPid === null &&
    row.delegationHash === parent.delegationHash &&
    row.owner && row.owner.pid === parent.ownerPid && row.owner.hostname === hostname() &&
    writerLeases.processIdentityMatches(row.owner.pid, row.owner.processStartId))
  if (guards.length !== 1) return {
    code: 'PUBLICATION_GUARD_OWNERSHIP_LOST',
    message: `expected one exact active nested publication guard, found ${guards.length}`
  }

  const nowMs = Date.now()
  const parentMatches = []
  for (const row of scan.active) {
    if (row.leaseId === guard.leaseId || row.kind !== 'task-session' ||
        row.stem !== parent.stem || row.sessionId !== parent.sessionId ||
        row.unverified !== false || !row.owner || row.owner.hostname !== hostname()) continue
    if (parent.mode === 'bounded') {
      if (row.leaseId === parent.leaseId && row.token === parent.token &&
          row.expiresAt !== null && Date.parse(row.expiresAt) > nowMs &&
          writerLeases.processIdentityMatches(row.owner.pid, row.owner.processStartId)) {
        parentMatches.push({ row, proof: {
          ok: true, kind: 'bounded-receipt', caller: {
            pid: parent.ownerPid, startId: guard.record.owner.processStartId
          }, authority: { pid: row.owner.pid, startId: row.owner.processStartId }
        } })
      }
      continue
    }
    if (row.expiresAt !== null || !Number.isInteger(row.childPid) || row.childPid <= 0 ||
        row.key !== `task:${parent.stem}` ||
        row.leaseId !== parent.leaseId || row.delegationHash !== delegationHash(parent.delegationToken) ||
        !writerLeases.PROCESS_START_ID_RE.test(String(row.childProcessStartId || ''))) continue
    const proof = writerLeases.processTreeProof(
      parent.ownerPid, guard.record.owner.processStartId,
      row.childPid, row.childProcessStartId
    )
    if (proof.ok) parentMatches.push({ row, proof: { ...proof, kind: 'site-process-tree' } })
  }
  if (parentMatches.length !== 1) return {
    code: 'PUBLICATION_PARENT_AUTHORITY_INVALID',
    message: `expected one exact active authenticated parent task-session, found ${parentMatches.length}`
  }
  const allowed = new Set([guard.leaseId, parentMatches[0].row.leaseId])
  const foreign = scan.active.find((row) => !allowed.has(row.leaseId))
  if (foreign) return {
    code: 'WORKSPACE_WRITER_ACTIVE',
    message: `another active writer conflicts with nested publication${foreign.stem ? ` for ${foreign.stem}` : ''} (${foreign.leaseId})`
  }
  return { parent: parentMatches[0].row, delegation: parentMatches[0].proof }
}

function refuseGuardedLease(handle, label, issue) {
  let releaseDetail = ''
  try { writerLeases.release({ ...handle, rootDir: WRITER_AUTHORITY_ROOT }) }
  catch (releaseError) { releaseDetail = `; lease release failed: ${releaseError && releaseError.message || releaseError}` }
  die(`${label} [${issue.code}]: ${issue.message}${releaseDetail}`, 2)
}

const args = process.argv.slice(2)
const command = args.shift()

try {
  if (command === 'acquire') {
    const parsed = flags(args, {
      '--kind': 'value', '--stem': 'value', '--key': 'value', '--owner-pid': 'value',
      '--ttl-ms': 'value', '--session-id': 'value', '--guard-finalization': 'boolean'
    })
    const kind = parsed['--kind'] || 'standby-writer'
    const stem = parsed['--stem'] || null
    const key = parsed['--key'] || 'standby'
    const ownerPid = positiveInt(parsed['--owner-pid'], process.ppid, '--owner-pid')
    const ttlMs = positiveInt(parsed['--ttl-ms'], 60 * 60 * 1000, '--ttl-ms')
    const sessionId = parsed['--session-id'] || writerLeases.createSessionId()
    const handle = writerLeases.acquire(DIR, { kind, stem, key, ownerPid, ttlMs, sessionId, rootDir: WRITER_AUTHORITY_ROOT })
    if (parsed['--guard-finalization']) {
      const writerIssue = activeWriterIssue(handle)
      if (writerIssue) refuseGuardedLease(handle, 'writer guard refused acquisition', writerIssue)
      const publicationIssue = finalizationGuardIssue()
      if (publicationIssue) refuseGuardedLease(handle, 'publication guard refused acquisition', publicationIssue)
    }
    process.stdout.write(JSON.stringify({
      version: 1, leaseId: handle.leaseId, token: handle.token,
      kind: handle.record.kind, stem: handle.record.stem,
      sessionId: handle.record.sessionId, expiresAt: handle.record.expiresAt
    }) + '\n')
  } else if (command === 'acquire-publication-guard') {
    const parsed = flags(args, {
      '--key': 'value', '--owner-pid': 'value', '--parent-stem': 'value',
      '--parent-session-id': 'value', '--parent-lease-id': 'value',
      '--parent-token': 'value', '--creation-key-hash': 'value',
      '--allow-all-creation-recovery': 'boolean'
    })
    const publicationKey = parsed['--key'] || ''
    const ownerPid = positiveInt(parsed['--owner-pid'], process.ppid, '--owner-pid')
    const parentStem = parsed['--parent-stem'] || ''
    const parentSessionId = parsed['--parent-session-id'] || ''
    const parentLeaseId = parsed['--parent-lease-id'] || ''
    const parentToken = parsed['--parent-token'] || ''
    // The site capability is inherited privately. Never accept or print it as
    // an argv value, which would expose it through the process table.
    const parentDelegationToken = process.env.ORCHESTRATOR_WRITER_DELEGATION_TOKEN || ''
    const creationKeyHash = parsed['--creation-key-hash'] || null
    const allowAllCreationRecovery = parsed['--allow-all-creation-recovery'] === true
    const boundedParent = Boolean(parentLeaseId && parentToken && !parentDelegationToken)
    const siteParent = Boolean(parentLeaseId && !parentToken && /^[a-f0-9]{48}$/.test(parentDelegationToken))
    if (!NESTED_PUBLICATION_KEYS.has(publicationKey) ||
        parsed['--owner-pid'] === undefined || ownerPid !== process.ppid ||
        !taskSource.safeTaskStem(parentStem) ||
        !writerLeases.SESSION_ID_RE.test(parentSessionId) ||
        (!boundedParent && !siteParent) || !writerLeases.LEASE_ID_RE.test(parentLeaseId) ||
        (boundedParent && !/^[a-f0-9]{32,128}$/.test(parentToken)) ||
        (creationKeyHash !== null && !HASH_RE.test(creationKeyHash)) ||
        (publicationKey === 'task:create-backlog' && (creationKeyHash === null || allowAllCreationRecovery)) ||
        (publicationKey === 'task:recover-backlog-creations' && (creationKeyHash !== null || !allowAllCreationRecovery))) {
      die('acquire-publication-guard requires an exact parent receipt and matching create/recovery publication scope')
    }
    const delegationToken = randomBytes(24).toString('hex')
    const delegationHash = `sha256:${createHash('sha256').update(delegationToken, 'ascii').digest('hex')}`
    const handle = writerLeases.acquire(DIR, {
      kind: 'lock-writer', stem: null, key: publicationKey,
      ownerPid, sessionId: parentSessionId, delegationHash, rootDir: WRITER_AUTHORITY_ROOT
    })
    const parent = {
      publicationKey, ownerPid, stem: parentStem, sessionId: parentSessionId,
      leaseId: parentLeaseId, token: parentToken || null,
      delegationToken: parentDelegationToken || null,
      mode: siteParent ? 'site' : 'bounded', delegationHash
    }
    const writerIssue = nestedPublicationWriterIssue(handle, parent)
    if (!writerIssue || !writerIssue.parent) {
      refuseGuardedLease(handle, 'nested publication guard refused acquisition', writerIssue || {
        code: 'PUBLICATION_GUARD_INVALID', message: 'nested publication guard verification failed'
      })
    }
    const publicationIssue = finalizationGuardIssue({ creationKeyHash, allowAllCreationRecovery })
    if (publicationIssue) refuseGuardedLease(handle, 'nested publication guard refused acquisition', publicationIssue)
    process.stdout.write(JSON.stringify({
      version: 1,
      leaseId: handle.leaseId,
      token: handle.token,
      kind: handle.record.kind,
      stem: null,
      sessionId: handle.record.sessionId,
      key: publicationKey,
      expiresAt: null,
      delegationKind: writerIssue.delegation.kind,
      delegationToken,
      callerPid: writerIssue.delegation.caller.pid,
      callerProcessStartId: writerIssue.delegation.caller.startId,
      authorityPid: writerIssue.delegation.authority.pid,
      authorityProcessStartId: writerIssue.delegation.authority.startId,
      parentLeaseId: writerIssue.parent.leaseId,
      parentStem: writerIssue.parent.stem,
      parentSessionId: writerIssue.parent.sessionId
    }) + '\n')
  } else if (command === 'release') {
    const parsed = flags(args, { '--lease-id': 'value', '--token': 'value' })
    const leaseId = parsed['--lease-id'] || ''
    const token = parsed['--token'] || ''
    if (!writerLeases.LEASE_ID_RE.test(leaseId) || !/^[a-f0-9]{32,128}$/.test(token)) die('release requires a valid lease id and token')
    writerLeases.release({ dir: DIR, rootDir: WRITER_AUTHORITY_ROOT, path: join(DIR, leaseId + '.json'), leaseId, token })
    process.stdout.write(JSON.stringify({ released: true, leaseId }) + '\n')
  } else if (command === 'renew') {
    const parsed = flags(args, { '--lease-id': 'value', '--token': 'value', '--ttl-ms': 'value' })
    const leaseId = parsed['--lease-id'] || ''
    const token = parsed['--token'] || ''
    const ttlMs = positiveInt(parsed['--ttl-ms'], writerLeases.MAX_TTL_MS, '--ttl-ms')
    if (!writerLeases.LEASE_ID_RE.test(leaseId) || !/^[a-f0-9]{32,128}$/.test(token)) die('renew requires a valid lease id and token')
    const handle = writerLeases.renew({ dir: DIR, rootDir: WRITER_AUTHORITY_ROOT, path: join(DIR, leaseId + '.json'), leaseId, token }, ttlMs)
    process.stdout.write(JSON.stringify({ renewed: true, leaseId, expiresAt: handle.record.expiresAt }) + '\n')
  } else if (command === 'verify') {
    const parsed = flags(args, {
      '--lease-id': 'value', '--token': 'value', '--session-id': 'value',
      '--stem': 'value', '--guard-finalization': 'boolean'
    })
    const leaseId = parsed['--lease-id'] || ''
    const token = parsed['--token'] || ''
    const sessionId = parsed['--session-id'] || ''
    const stem = parsed['--stem'] || ''
    if (!writerLeases.LEASE_ID_RE.test(leaseId) || !/^[a-f0-9]{32,128}$/.test(token) ||
        !writerLeases.SESSION_ID_RE.test(sessionId) || !taskSource.safeTaskStem(stem)) {
      die('verify requires valid --lease-id, --token, --session-id and --stem')
    }
    const scan = scanWriterState('bounded-receipt')
    if (scan.issues.length) die(`lease receipt verification refused [${scan.issues[0].code || 'WRITER_LEASE_INVALID'}]: ${scan.issues[0].message}`, 2)
    const nowMs = Date.now()
    const matches = scan.active.filter((row) => row.leaseId === leaseId && row.token === token &&
      row.kind === 'task-session' && row.stem === stem && row.sessionId === sessionId &&
      row.unverified === false && row.expiresAt !== null && Date.parse(row.expiresAt) > nowMs &&
      row.owner && row.owner.hostname === hostname() &&
      writerLeases.processIdentityMatches(row.owner.pid, row.owner.processStartId))
    if (matches.length !== 1) die(`lease receipt verification refused: expected one exact active bounded task-session lease, found ${matches.length}`, 2)
    const own = matches[0]
    const conflict = scan.active.find((row) => row.leaseId !== own.leaseId &&
      (deterministicPublicationLease(row) || row.stem === stem || row.key === `task:${stem}`))
    if (conflict) die(`lease receipt verification refused: another active writer owns ${stem} (${conflict.leaseId})`, 2)
    if (parsed['--guard-finalization']) {
      const issue = finalizationGuardIssue()
      if (issue) die(`lease receipt guard refused [${issue.code}]: ${issue.message}`, 2)
    }
    process.stdout.write(JSON.stringify({
      verified: true, leaseId, sessionId, stem, kind: own.kind, expiresAt: own.expiresAt
    }) + '\n')
  } else if (command === 'scan') {
    flags(args, {})
    process.stdout.write(JSON.stringify(scanWriterState('inspection'), null, 2) + '\n')
  } else if (command === 'verify-session') {
    const parsed = flags(args, { '--session-id': 'value', '--stem': 'value', '--guard-finalization': 'boolean' })
    const sessionId = parsed['--session-id'] || ''
    const stem = parsed['--stem'] || ''
    if (!writerLeases.SESSION_ID_RE.test(sessionId) || !taskSource.safeTaskStem(stem)) {
      die('verify-session requires a valid --session-id and --stem')
    }
    const scan = scanWriterState('site-session')
    if (scan.issues.length) die(`session verification refused [${scan.issues[0].code || 'WRITER_LEASE_INVALID'}]: ${scan.issues[0].message}`, 2)
    // This command authenticates only a site-started Claude turn. Direct CLI
    // generations are TTL-bounded; accepting one through a stale/exported env
    // value would let it expire after this check but before the first mutation.
    // Site leases are non-expiring, attached to the already-spawned child, and
    // use the canonical task key. Their lifecycle is instead closed by the
    // site's result/process-tree handling.
    const matches = scan.active.filter((row) => row.kind === 'task-session' && row.stem === stem &&
      row.sessionId === sessionId && row.unverified === false && row.expiresAt === null &&
      Number.isInteger(row.childPid) && row.childPid > 0 && row.key === `task:${stem}` &&
      row.owner && row.owner.hostname === hostname() &&
      writerLeases.processIdentityMatches(row.childPid, row.childProcessStartId))
    if (matches.length !== 1) die(`session verification refused: expected one active attached site lease, found ${matches.length}`, 2)
    const own = matches[0]
    const conflict = scan.active.find((row) => row.leaseId !== own.leaseId &&
      (deterministicPublicationLease(row) || row.stem === stem || row.key === `task:${stem}`))
    if (conflict) {
      die(`session verification refused: another active writer owns ${stem} (${conflict.leaseId})`, 2)
    }
    if (parsed['--guard-finalization']) {
      const issue = finalizationGuardIssue()
      if (issue) die(`session guard refused [${issue.code}]: ${issue.message}`, 2)
    }
    process.stdout.write(JSON.stringify({ verified: true, sessionId, stem, leaseId: own.leaseId }) + '\n')
  } else {
    die('usage: writer-lease.mjs acquire --kind <task-session|standby-writer> [--stem TASK_N_name] [--owner-pid N] [--guard-finalization] | acquire-publication-guard --key task:create-backlog --owner-pid N --parent-stem TASK_N_name --parent-session-id ID --parent-lease-id ID [--parent-token TOKEN] --creation-key-hash sha256:... (site delegation token is env-only) | verify --lease-id ID --token TOKEN --session-id ID --stem TASK_N_name [--guard-finalization] | verify-session --session-id ID --stem TASK_N_name [--guard-finalization] | renew --lease-id ID --token TOKEN [--ttl-ms N] | release --lease-id ID --token TOKEN | scan')
  }
} catch (error) {
  die(error && error.message || error)
}
