'use strict';

// ---------------------------------------------------------------------------
// The integration transaction owner (pipeline improvement 01, §10/§19.1).
//
// One record in orchestrator/.cache/tasks/integrations/<STEM>.json is one
// attempt to move ONE sealed candidate into the canonical target branch as ONE
// commit, together with every global artifact the finalizer publishes for that
// task. The record is a write-ahead log: every phase records its INTENT before
// its effect, and after a restart each phase is re-proven from PHYSICAL state
// (git objects, refs, the index, the finalization marker) — never from what
// the record hoped had happened.
//
// Three doctrines this module never bends:
//   1. It never rolls back. A postcondition that does not match its pin makes
//      the record 'recovery-required' and leaves every byte in place.
//   2. It never stashes, commits or reverts the user's own work to satisfy a
//      precondition. Blocking paths are reported exactly; the owner decides.
//   3. It performs no git mutation itself. server/git-mutations.js is the
//      single mutation owner; this module drives it and proves the results.
//
// Serialization (§12.1/§17): at most ONE active integration exists repository
// wide. The finalizer child runs through the finalizations supervisor, so an
// integration and a finalization recovery can never run at the same time.
// ---------------------------------------------------------------------------

var cp = require('child_process');
var crypto = require('crypto');
var fs = require('fs');
var os = require('os');
var path = require('path');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var gitMutations = require('./git-mutations');
var publicationGuard = require('./integration-publication-guard');
var worktreeManager = require('./worktree-manager');
var worktreeContract = require('../../tasks/worktree-record-contract.cjs');
var candidateContract = require('../../tasks/candidate-receipt-contract.cjs');
var contract = require('../../tasks/integration-record-contract.cjs');
var writerLeases = require('../../tasks/writer-leases.cjs');

var DIR = paths.INTEGRATIONS_DIR;
var AUTHORITY_ROOT = paths.WORKTREE_RECORDS_AUTHORITY_ROOT;
var MAX_RECORDS = 1000;
var GIT_TIMEOUT_MS = 10 * 1000;
var GIT_MAX_BUFFER = 48 * 1024 * 1024;
var STEM_RE = /^TASK_[1-9][0-9]{0,15}_[A-Za-z0-9_]{1,120}$/;
// The two control-owned paths a user may legitimately have dirty when the
// Integrate button is pressed (§10.2/§11). Everything else blocks with its
// exact path.
var TASKS_TODO_PREFIX = 'orchestrator/tasks/todo/';
var TASK_INDEX_PATH = 'orchestrator/tasks/INDEX.json';
var MAX_SUBJECT_BYTES = 72;
// In-memory ownership proofs are deliberately not part of the closed WAL
// schema. Every parsed record object is bound to the exact inode/stat+bytes
// generation it came from; a writer may replace only that generation.
var recordGenerations = new WeakMap();

// Read-only git. Every mutation goes through git-mutations; this allowlist
// contains no verb that can write an object, a ref or the index.
var GIT_ALLOWED_PREFIXES = [['rev-parse'], ['symbolic-ref'], ['cat-file'], ['ls-files']];
function runGit(args, cwd) {
  if (!Array.isArray(args) || !args.length ||
      !GIT_ALLOWED_PREFIXES.some(function (prefix) {
        return prefix.every(function (token, index) { return args[index] === token; });
      })) {
    return { ok: false, code: 'GIT_ARGS_NOT_ALLOWED', stdout: '' };
  }
  var env = {};
  Object.keys(process.env).forEach(function (key) {
    if (key.indexOf('GIT_') !== 0) env[key] = process.env[key];
  });
  env.GIT_TERMINAL_PROMPT = '0';
  env.GIT_OPTIONAL_LOCKS = '0';
  env.LC_ALL = 'C';
  var result;
  try {
    result = cp.spawnSync('git', args, { cwd: cwd || paths.PROJECT_ROOT, env: env,
      timeout: GIT_TIMEOUT_MS, maxBuffer: GIT_MAX_BUFFER, encoding: 'utf8' });
  } catch (error) { return { ok: false, code: 'GIT_SPAWN_FAILED', stdout: '' }; }
  if (result.error || result.signal || typeof result.status !== 'number') {
    return { ok: false, code: 'GIT_UNAVAILABLE', stdout: '' };
  }
  return { ok: result.status === 0, code: result.status === 0 ? null : 'GIT_EXIT_' + result.status,
    stdout: result.stdout || '', stderr: result.stderr || '' };
}
function refCommit(ref) {
  var probe = runGit(['rev-parse', '-q', '--verify', ref]);
  return probe.ok ? probe.stdout.trim() : null;
}
function now() { return new Date().toISOString(); }
function bounded(value, limit) { return String(value == null ? '' : value).slice(0, limit || 400); }
function ownerIdentity() {
  // The process-start identity is what makes this owner PROVABLE: a pid
  // alone is recycled, and a record whose owner cannot be re-identified
  // is a claim rather than evidence. writerLeases owns the capture, so
  // the records and the leases judge liveness by the same measure.
  var startId = null;
  try { startId = writerLeases.captureProcessStartId(process.pid); } catch (error) { startId = null; }
  return { hostname: os.hostname(), pid: process.pid, processStartId: startId,
    startedAt: new Date(Date.now() - Math.floor(process.uptime() * 1000)).toISOString() };
}

// --------------------------------------------------------------------------
// Record store. Reads are bounded and validated; an unreadable or invalid
// record is reported, never deleted and never presumed absent.
// --------------------------------------------------------------------------

function recordFile(stem) { return path.join(DIR, stem + '.json'); }

function readOne(stem) {
  if (!STEM_RE.test(String(stem || ''))) return { ok: false, code: 'INTEGRATION_STEM_INVALID' };
  var bounded_ = fileGuards.boundedRegularFileUnder(AUTHORITY_ROOT, DIR, recordFile(stem), contract.MAX_BYTES);
  if (!bounded_) return { ok: false, code: 'INTEGRATION_RECORD_ABSENT' };
  try {
    var record = contract.validateBytes(bounded_.bytes);
    recordGenerations.set(record, { bytes: bounded_.bytes, proof: bounded_.stat });
    return { ok: true, record: record };
  }
  catch (error) { return { ok: false, code: 'INTEGRATION_RECORD_INVALID', message: bounded(error.message) }; }
}

function list() {
  var out = { records: [], invalid: [], unavailable: null };
  var listed = fileGuards.boundedDirectoryNamesUnder(AUTHORITY_ROOT, DIR, MAX_RECORDS);
  if (!listed || !listed.ok) {
    // An absent directory is an empty store; anything else is unprovable state.
    if (listed && listed.code === 'missing') return out;
    var exists = true;
    try { fs.lstatSync(DIR); } catch (error) { exists = false; }
    if (!exists) return out;
    out.unavailable = (listed && listed.code) || 'unreadable';
    return out;
  }
  listed.names.filter(function (name) { return /^[A-Za-z0-9._-]{1,200}\.json$/.test(name); }).sort()
    .forEach(function (name) {
      var stem = name.slice(0, -'.json'.length);
      var got = readOne(stem);
      if (got.ok) out.records.push(got.record);
      else out.invalid.push({ name: name, code: got.code, message: got.message || '' });
    });
  return out;
}

function writeRecord(record, noClobber, expectedRecord) {
  // Capture the source generation before mutating the caller's object. The
  // serialized replacement has a new updatedAt/hash, while the CAS must still
  // compare against the bytes that were actually read.
  var sourceGeneration = recordGenerations.get(expectedRecord || record) || null;
  record.updatedAt = now();
  record.recordHash = contract.recordHash(record);
  try { contract.validate(record); }
  catch (error) { return { ok: false, code: 'INTEGRATION_RECORD_INVALID', message: bounded(error.message) }; }
  var file = recordFile(record.stem);
  var bytes = Buffer.from(JSON.stringify(record) + '\n', 'utf8');
  if (noClobber) {
    var published = fileGuards.publishNoClobberRegularFileUnder(
      AUTHORITY_ROOT, DIR, file, bytes,
      { create: true, directoryMode: 0o700, mode: 0o600, maxBytes: contract.MAX_BYTES });
    if (!published || !published.ok) {
      if (published && (published.code === 'exists' || published.code === 'already-exists')) {
        return { ok: false, code: 'INTEGRATION_ALREADY_ACTIVE',
          message: 'an integration record already exists' };
      }
      return { ok: false, code: 'INTEGRATION_RECORD_UNWRITABLE',
        message: 'the integration record could not be published durably' };
    }
    recordGenerations.set(record, { bytes: bytes, proof: published.stat });
    return { ok: true, record: record };
  }
  if (!sourceGeneration) {
    return { ok: false, code: 'INTEGRATION_RECORD_CONFLICT',
      message: 'the integration record generation is not owned by this writer' };
  }
  var swapped = fileGuards.compareAndSwapRegularFileUnder(
    AUTHORITY_ROOT, DIR, file, contract.MAX_BYTES,
    { proof: sourceGeneration.proof, bytes: sourceGeneration.bytes }, bytes, { mode: 0o600 });
  if (!swapped || !swapped.ok) {
    return { ok: false, code: 'INTEGRATION_RECORD_CONFLICT',
      message: 'the integration record changed before this WAL transition could be published' };
  }
  recordGenerations.set(record, { bytes: bytes, proof: swapped.stat });
  return { ok: true, record: record };
}

function dirMtime() {
  try { return fs.statSync(DIR).mtimeMs; } catch (error) { return 0; }
}

// --------------------------------------------------------------------------
// WAL phase lattice helpers. Intent before effect, proof after effect, and a
// mismatch is recovery evidence — never a rollback and never a guess.
// --------------------------------------------------------------------------

// `phase` always names the FURTHEST phase that carries an intent — never the
// phase currently being worked on. A resume legitimately re-enters an earlier
// phase to re-prove it (its effect landed, its proof did not), and naming that
// earlier phase would contradict the intents still recorded ahead of it.
function furthestIntendedPhase(record) {
  var furthest = contract.PHASES[0];
  contract.PHASES.forEach(function (name) {
    if (record.phases[name].intentAt !== null) furthest = name;
  });
  return furthest;
}
function markIntent(record, phase) {
  if (record.phases[phase].intentAt === null) record.phases[phase].intentAt = now();
  record.phase = furthestIntendedPhase(record);
  return writeRecord(record, false);
}
function markProven(record, phase) {
  // Intent before effect is the whole point of the lattice: a proof without a
  // recorded intent would be a schema violation discovered only when the
  // record is next read, so it fails here, loudly, at its source.
  if (record.phases[phase].intentAt === null) {
    return { ok: false, code: 'INTEGRATION_PHASE_UNINTENDED',
      message: 'phase ' + phase + ' was proven without a recorded intent' };
  }
  if (record.phases[phase].provenAt === null) record.phases[phase].provenAt = now();
  record.phase = furthestIntendedPhase(record);
  return writeRecord(record, false);
}
function recoveryRequired(record, reason) {
  record.status = 'recovery-required';
  var written = writeRecord(record, false);
  if (!written.ok) return written;
  journalIntegration(record, 'escalate', 'Integration stopped at phase ' +
    record.phase + ' and needs recovery: ' + reason);
  return { ok: false, code: 'INTEGRATION_RECOVERY_REQUIRED', message: bounded(reason),
    record: written.ok ? written.record : record };
}
function refuse(code, message, detail, paths_) {
  return { ok: false, code: code, message: bounded(message), detail: detail || null,
    paths: Array.isArray(paths_) ? paths_.slice(0, 50) : [] };
}

// --------------------------------------------------------------------------
// §10.2 preconditions. Nothing is mutated to make them pass; every blocker
// carries the exact paths or the exact reason the owner has to act on.
// --------------------------------------------------------------------------

// The Outcome draft a run leaves for the transaction, named by the GENERATION
// that produced it. A bare `<stem>.draft.md` is bound to nothing: after a
// reopen, or after a run that was abandoned and re-run, the previous
// generation's Outcome would be published verbatim for a candidate it never
// described. The worktree id is already in the run child's environment
// (ORCHESTRATOR_WORKTREE_ID), so the binding costs the run nothing and the
// transaction can prove it.
function outcomeDraftFile(stem, worktreeId) {
  return path.join(paths.FINALIZATIONS_DIR, stem + '.' + worktreeId + '.draft.md');
}

function blocker(code, message, list_) {
  return { code: code, message: bounded(message), paths: Array.isArray(list_) ? list_.slice(0, 50) : [] };
}

// The transaction's trace in the task History (§19.3). Best effort by design:
// a journal that cannot be written must never stop a publication, and the WAL
// record remains the authority for what happened. But without this the single
// most consequential step in a task's life — the canonical commit — left no
// mark on the surface an operator actually reads.
function journalIntegration(record, status, detail) {
  // task-journal is ESM and this module is CJS, so the append is asynchronous
  // and deliberately unawaited: the trace follows the transaction, it never
  // gates it.
  var event = {
    kind: 'note', stem: record.stem, ts: now(), phase: 'integration',
    status: status, detail: bounded(detail, 200), meta: {}
  };
  try {
    var href = require('url').pathToFileURL(
      path.join(paths.ORCHESTRATOR_DIR, 'tasks', 'task-journal.mjs')).href;
    import(href).then(function (journal) {
      try { journal.appendEvent(event, record.stem); } catch (error) {}
    }, function () {});
  } catch (error) { /* the transaction is never blocked by its own trace */ }
}

function preconditions(stem, publicationToken) {
  var blockers = [];
  var context = { record: null, receipt: null, outcomeDraft: null, dirtyAllowed: [], candidatePaths: [] };
  if (!STEM_RE.test(String(stem || ''))) {
    return { ok: false, blockers: [blocker('stem-invalid', 'the task stem is not canonical')], context: context };
  }

  // Environment: unfinished git operation, detached/unborn HEAD, submodules,
  // sparse checkout, shallow history, content filters, case collisions, disk.
  var prechecks = worktreeManager.environmentPrechecks(paths.PROJECT_ROOT);
  prechecks.findings.forEach(function (finding) {
    if (finding.severity === 'blocker') blockers.push(blocker('environment', finding.code + ': ' + finding.message));
  });

  // Repository-wide integration mutex.
  var publishing = publicationGuard.issue();
  if (publishing.active && publishing.token !== publicationToken) {
    blockers.push(blocker('integration-busy', 'another process is publishing the integration WAL'));
  }
  var store = list();
  if (store.unavailable) blockers.push(blocker('records-unavailable', 'the integration record store is unreadable'));
  store.invalid.forEach(function (entry) {
    blockers.push(blocker('records-invalid', entry.name + ': ' + entry.code));
  });
  var foreign = store.records.filter(function (item) {
    return item.stem !== stem && (item.status === 'active' || item.status === 'recovery-required');
  });
  if (foreign.length) {
    blockers.push(blocker('integration-busy', foreign[0].stem + ' holds the integration mutex at phase ' + foreign[0].phase));
  }

  // The sealed generation and its receipt. An unprovable ownership store is a
  // separate blocker from a proven absence: the first says "ask again", the
  // second says "there is nothing to integrate".
  var generation = worktreeManager.activeRecordFor(stem);
  if (!generation.ok) {
    blockers.push(blocker('records-unavailable', generation.message));
    return { ok: false, blockers: blockers, context: context };
  }
  var record = generation.record;
  if (!record) {
    blockers.push(blocker('candidate-absent', stem + ' has no materialized execution generation'));
    return { ok: false, blockers: blockers, context: context };
  }
  context.record = record;
  if (record.status !== 'ready-for-integration') {
    blockers.push(blocker('candidate-not-sealed', stem + ' is ' + record.status + ', not ready-for-integration'));
  }
  var receipt = worktreeManager.candidateReceipt(record.worktreeId);
  if (!receipt) {
    blockers.push(blocker('receipt-absent', 'the candidate receipt is missing or invalid'));
    return { ok: false, blockers: blockers, context: context };
  }
  context.receipt = receipt;
  context.candidatePaths = receipt.entries.map(function (entry) { return entry.path; });

  // The candidate must still be exactly where it was sealed.
  var candidateCommit = refCommit(record.candidateRef);
  if (candidateCommit !== receipt.candidateCommit) {
    blockers.push(blocker('candidate-moved', record.candidateRef + ' no longer carries the sealed candidate commit'));
  }

  // Candidate base equals current target HEAD, and the control root is on it.
  var head = runGit(['symbolic-ref', '-q', 'HEAD']);
  var headRef = head.ok ? head.stdout.trim() : null;
  if (headRef !== record.targetRef) {
    blockers.push(blocker('target-not-checked-out',
      'the control root is on ' + (headRef || 'a detached HEAD') + ', not ' + record.targetRef));
  }
  // Everything this generation was pinned against — the target ref AND the
  // Figma / API generations the run's gates were green against. The manager
  // RECORDS the verdict, so a superseded candidate stops being projected as
  // 'ready' the moment anyone asks; detecting it here and only returning a
  // blocker left the board offering Integrate for a candidate that can never
  // be integrated, and made the revalidation branch of the board unreachable.
  var superseded = worktreeManager.revalidationIssueFor(record);
  if (superseded) {
    if (superseded.record) { record = superseded.record; context.record = record; }
    blockers.push(blocker('target-drifted', superseded.message));
  }

  // No foreign staged changes: the index must describe exactly the base tree.
  var indexTree = gitMutations.controlIndexTree();
  if (indexTree === null) blockers.push(blocker('index-unreadable', 'the control index could not be read'));
  else if (indexTree !== record.baseTree && indexTree !== receipt.candidateTree) {
    blockers.push(blocker('index-not-clean', 'the control index carries staged changes that are not this transaction'));
  }

  // Dirty working-tree paths, discovered base-relative through a manager-owned
  // index (never the control index, whose assume-unchanged bits could hide one).
  var dirty = gitMutations.controlPathsDifferingFrom(record.baseTree);
  if (!dirty.ok) {
    blockers.push(blocker('worktree-unreadable', 'the control working tree could not be compared to the base: ' + dirty.code));
  } else {
    var candidateFolded = Object.create(null);
    context.candidatePaths.forEach(function (entry) { candidateFolded[entry.toLowerCase()] = entry; });
    var offending = [];
    dirty.paths.forEach(function (entry) {
      // A path this transaction is about to write may never be dirty first:
      // the candidate was verified against committed bytes, so the user's own
      // bytes would be silently overwritten (§11). Case-folded, because APFS
      // folds and a collision is the same file.
      if (Object.prototype.hasOwnProperty.call(candidateFolded, entry.toLowerCase())) {
        offending.push(entry);
        return;
      }
      // The current task's own source file: the finalizer rewrites it inside
      // this very transaction, so its dirty bytes are the transaction's input.
      if (entry === TASKS_TODO_PREFIX + stem + '.md') { context.dirtyAllowed.push(entry); return; }
      // INDEX.json is DERIVED: the finalizer regenerates it from the file
      // system during prepare, so whatever is dirty here provably does not
      // survive into the commit.
      if (entry === TASK_INDEX_PATH) { context.dirtyAllowed.push(entry); return; }
      offending.push(entry);
    });
    if (offending.length) {
      blockers.push(blocker('dirty-control-root',
        'commit, stash or revert these paths before integrating', offending.sort()));
    }
  }

  // The task lock proves the run that produced this candidate still owns the
  // task; the finalizer refuses to publish without it.
  var lockState = taskLockState(stem, record.runId);
  if (!lockState.ok) {
    blockers.push(blocker(lockState.code === 'TASK_LOCK_OWNER_MISMATCH' ?
      'task-lock-owner-mismatch' : lockState.code === 'TASK_LOCK_INVALID' ?
        'task-lock-invalid' : 'task-lock-unreadable', lockState.message));
  } else if (!lockState.present) {
    blockers.push(blocker('task-lock-absent', stem + ' has no active orchestrator lock'));
  }

  // §10.2: the current task's own todo file may be dirty ONLY because the
  // canonical validator proves it is one coherent, in-lifecycle task in the
  // `todo` state and nothing else. Without that proof "the current task's own
  // file" is just a path pattern, and arbitrary bytes — a half-written edit, a
  // merge conflict, another task's content pasted in — would be published as
  // this task's source. The validator is the same authority every transition
  // uses; it is asked here, in the control root, before anything is applied.
  if (context.dirtyAllowed.indexOf(TASKS_TODO_PREFIX + stem + '.md') >= 0) {
    var verdict;
    try {
      verdict = require('../../tasks/task-state-core.cjs').validateTaskState({
        tasksDir: paths.TASKS_DIR, repoRoot: paths.PROJECT_ROOT,
        stem: stem, expect: 'todo', checkIndex: false, includeRuntime: false
      });
    } catch (error) {
      verdict = { ok: false, findings: [{ message: bounded(error && error.message || error) }] };
    }
    if (!verdict.ok) {
      blockers.push(blocker('task-source-unproven',
        'the uncommitted ' + stem + '.md is not a provable single-task lifecycle delta: ' +
        bounded((verdict.findings && verdict.findings[0] && verdict.findings[0].message) || 'validation failed'),
        [TASKS_TODO_PREFIX + stem + '.md']));
    }
  }

  // The Outcome draft the run wrote into the control cache is the finalizer's
  // input; without it the transaction has nothing to publish. It must be THIS
  // generation's draft — a draft left by an older generation describes work
  // this candidate does not contain.
  var draft = outcomeDraftFile(stem, record.worktreeId);
  try {
    if (fs.lstatSync(draft).isFile()) context.outcomeDraft = draft;
  } catch (error) { context.outcomeDraft = null; }
  if (!context.outcomeDraft) {
    blockers.push(blocker('outcome-draft-absent',
      'this generation left no Outcome draft at ' + draft));
  }

  // §10.2: the canonical commit runs the control hooks, and the local
  // screenshot-gate net IS a pre-commit hook. Publishing while it is unwired
  // would ship an uncompared UI task to done/ with the net believed active.
  // Same single policy the run admission uses, so the two can never disagree.
  var netIssue = require('./git').enforcementNetIssue();
  if (netIssue) blockers.push(blocker('hooks-unwired', netIssue));

  // The user's own Git identity (§10.5): never invented, never defaulted.
  if (!gitMutations.configuredIdentity()) {
    blockers.push(blocker('git-identity-missing', 'configure git user.name and user.email before integrating'));
  }

  // Writer quiescence, checked BEFORE anything is applied. The finalizer's
  // prepare half refuses while a control writer holds the workspace, and by
  // then the candidate would already be in the control root — a half-applied
  // tree plus an active WAL, which blocks every board mutation until an
  // operator resumes. Refusing here costs nothing and mutates nothing.
  var quiescence = finalizationsModule().controlWriterIssue();
  if (quiescence) {
    blockers.push(blocker('workspace-writer-active', quiescence.message));
  }

  // §16: "execution A is incompatible with cleanup A". Phase 9 REMOVES this
  // generation's checkout, so an app-run building inside it must be finished
  // first — otherwise the transaction publishes its commit and then either
  // deletes a tree out from under a live build or fails cleanup after the fact.
  // The release path re-proves this at the moment it deletes; refusing here
  // just makes the common case a precondition instead of a late recovery.
  var building = finalizationsModule().executionWriterIssue(record.worktreeId);
  if (building) blockers.push(blocker('execution-busy', building.message));

  return { ok: blockers.length === 0, blockers: blockers, context: context };
}

// Lazily required: finalizations requires this module back for its mutation
// gate, so a top-level require would close the cycle at load time.
function finalizationsModule() { return require('./finalizations'); }



// --------------------------------------------------------------------------
// Read-only preview (§10.2/§19.2): the exact diff the transaction would
// publish, the exact blockers, and the honest statement that dirty user bytes
// are NOT part of it.
// --------------------------------------------------------------------------

function preview(stem) {
  var existing = readOne(stem);
  if (existing.ok && existing.record.status !== 'completed') {
    return { ok: true, stem: stem, state: existing.record.status === 'recovery-required' ? 'recovery-required' : 'in-flight',
      integration: projectRecord(existing.record), blockers: [], candidate: null };
  }
  if (!existing.ok && existing.code === 'INTEGRATION_RECORD_INVALID') {
    return { ok: false, code: existing.code, message: existing.message };
  }
  var verdict = preconditions(stem);
  var receipt = verdict.context.receipt;
  // A superseded generation is its own state, not a generic block: the board
  // offers a release for it, and Integrate is not merely refused but pointless.
  // preconditions() has just RECORDED that verdict, so the record and this
  // projection can never disagree.
  var supersededState = verdict.context.record &&
    verdict.context.record.status === 'revalidation-required';
  return {
    ok: true, stem: stem,
    state: verdict.ok ? 'ready' : (supersededState ? 'revalidation-required' : 'blocked'),
    blockers: verdict.blockers,
    integration: existing.ok ? projectRecord(existing.record) : null,
    candidate: receipt ? {
      worktreeId: receipt.worktreeId, runId: receipt.runId,
      candidateCommit: receipt.candidateCommit, candidateTree: receipt.candidateTree,
      baseCommit: receipt.baseCommit, diffHash: receipt.diffHash,
      entries: receipt.entries.map(function (entry) {
        return { path: entry.path, operation: entry.operation, renameFrom: entry.renameFrom };
      }),
      // §11 requires this to be stated, not implied.
      dirtyBytesExcluded: verdict.context.dirtyAllowed.slice()
    } : null
  };
}

function projectRecord(record) {
  return {
    integrationId: record.integrationId, stem: record.stem, runId: record.runId,
    worktreeId: record.worktreeId, phase: record.phase, status: record.status,
    targetRef: record.target.ref, baseCommit: record.target.baseCommit,
    candidateCommit: record.candidate.commit, diffHash: record.candidate.diffHash,
    publishedCommit: record.commitPin.publishedCommit,
    preparedPaths: record.finalizerPrepared === null ? null : record.finalizerPrepared.length,
    createdAt: record.createdAt, updatedAt: record.updatedAt
  };
}

// --------------------------------------------------------------------------
// Commit message (§10.5, owner decision): bounded deterministic subject plus
// machine trailers. The author is the repository's configured user; nothing
// about the tooling appears in canonical history.
// --------------------------------------------------------------------------

function commitMessage(record) {
  var match = /^TASK_([1-9][0-9]{0,15})_(.+)$/.exec(record.stem);
  var id = match ? match[1] : '0';
  var title = (match ? match[2] : record.stem).replace(/_/g, ' ').trim();
  var subject = 'TASK_' + id + ': ' + title;
  while (Buffer.byteLength(subject, 'utf8') > MAX_SUBJECT_BYTES) {
    title = title.slice(0, -1).trim();
    subject = 'TASK_' + id + ': ' + title;
  }
  return subject + '\n\n' +
    'Task-Stem: ' + record.stem + '\n' +
    'Run-Id: ' + record.runId + '\n' +
    'Candidate-Diff: ' + record.candidate.diffHash + '\n' +
    'Integration-Id: ' + record.integrationId + '\n';
}

// The CLOSED set of tracked artifacts a finalizer prepare is allowed to
// produce (§10.4). Anything else that differs from the applied candidate — the
// owner's unrelated work in progress, another task's file, a stray product
// file written while the transaction ran — is NOT part of this task and must
// never enter its commit. The set is matched exactly, never by prefix, except
// for the stem's own ship-receipt tree.
function finalizerOwnedPath(stem, value) {
  if (candidateContract.unrepresentablePath(value)) return false;
  if (!candidateContract.controlOwnedPath(value)) return false;
  if (value === 'orchestrator/tasks/done/' + stem + '.md') return true;
  if (value === TASKS_TODO_PREFIX + stem + '.md') return true;
  if (value === TASK_INDEX_PATH) return true;
  if (value === 'orchestrator/.arch-map.json') return true;
  if (value === 'orchestrator/figma/component-mappings.json') return true;
  if (value === 'orchestrator/figma/token-mappings.json') return true;
  return value.indexOf('orchestrator/tasks/evidence/figma-ship/' + stem + '/') === 0;
}

// The exact transaction-owned control artifacts the finalizer produced,
// derived from the PHYSICAL tree rather than from what the finalizer said it
// wrote — then narrowed to the closed owned set BEFORE a single path is
// staged. Staging first and judging afterwards would leave foreign paths in
// the owner's index with the transaction wedged.
function preparedPathPins(stem, candidateTree) {
  var entries = gitMutations.controlPathsDifferingFrom(candidateTree);
  if (!entries.ok) return { ok: false, code: entries.code };
  var foreign = entries.paths.filter(function (entry) { return !finalizerOwnedPath(stem, entry); });
  if (foreign.length) {
    return { ok: false, code: 'INTEGRATION_FOREIGN_ARTIFACTS',
      paths: foreign.slice(0, 50),
      message: 'the control root carries changes this transaction does not own: ' + foreign.slice(0, 5).join(', ') };
  }
  if (!entries.paths.length) {
    return { ok: false, code: 'INTEGRATION_PREPARE_EMPTY', paths: [],
      message: 'the finalizer published no tracked artifact' };
  }
  var staged = gitMutations.stageTransactionPaths({ paths: entries.paths });
  if (!staged.ok) return staged;
  var diff = gitMutations.treeEntries(candidateTree, staged.indexTree);
  if (diff === null) return { ok: false, code: 'INTEGRATION_DIFF_FAILED' };
  var pins = diff.map(function (entry) {
    return { path: entry.path, hash: worktreeContract.digest(entry) };
  });
  // Re-assert on the staged result: the tree is what actually gets committed.
  var trespass = pins.filter(function (pin) { return !finalizerOwnedPath(stem, pin.path); });
  if (trespass.length) {
    return { ok: false, code: 'INTEGRATION_FOREIGN_ARTIFACTS',
      paths: trespass.map(function (pin) { return pin.path; }).slice(0, 50),
      message: 'the staged tree carries artifacts this transaction does not own' };
  }
  return { ok: true, pins: pins, indexTree: staged.indexTree };
}

// --------------------------------------------------------------------------
// Physical proofs. After a restart every phase that recorded an intent is
// re-decided from what is actually on disk. A proof that holds marks the phase
// done; a proof that fails re-runs the (idempotent) effect; a proof that is
// impossible makes the record recovery-required. Nothing is ever assumed.
// --------------------------------------------------------------------------

// The finalization marker, as a discriminated result. "The file is not there"
// and "the file is there but I could not read or parse it" are opposite facts,
// and collapsing them is a fail-open: phase 8 reads a missing marker as PROOF
// that the confirm already ran, and phase 9 reads it as permission to release
// the generation. Absence is proven by the directory's own inventory — the same
// authority finalizations uses — never by a failed read. The shape is checked
// by the marker's owner, so a blob that merely looks marker-like proves nothing.
//   { ok: true,  marker }        proven present and shape-valid
//   { ok: true,  marker: null }  proven absent
//   { ok: false, code }          unprovable: refuse
function finalizationMarker(stem) {
  var name = stem + '.json';
  var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, paths.FINALIZATIONS_DIR, 4096);
  if (!listed || !listed.ok) {
    return { ok: false, code: 'MARKER_DIR_UNREADABLE',
      message: 'the finalizations directory could not be listed' };
  }
  if (listed.names.indexOf(name) < 0) return { ok: true, marker: null };
  var file = path.join(paths.FINALIZATIONS_DIR, name);
  var bounded_ = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, paths.FINALIZATIONS_DIR, file, 256 * 1024);
  if (!bounded_) {
    return { ok: false, code: 'MARKER_UNREADABLE',
      message: 'the finalization marker of ' + stem + ' is unreadable, oversized or unsafe' };
  }
  var value;
  try { value = JSON.parse(bounded_.bytes.toString('utf8')); }
  catch (error) {
    return { ok: false, code: 'MARKER_INVALID',
      message: 'the finalization marker of ' + stem + ' is not valid JSON' };
  }
  var shapeError = finalizationsModule().markerShapeError(value, stem);
  if (shapeError) {
    return { ok: false, code: 'MARKER_INVALID',
      message: 'the finalization marker of ' + stem + ' is invalid: ' + shapeError };
  }
  return { ok: true, marker: value };
}
// A lock name that is absent from an exact directory inventory is absent. A
// name that exists but cannot be opened as one bounded regular file is foreign
// evidence, not absence: accepting a symlink/FIFO here would release the
// generation after confirm while a new authority entry already occupies the
// canonical lock path.
function taskLockState(stem, expectedRunId) {
  var name = stem + '.json';
  var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, paths.LOCKS_DIR, 4096);
  if (!listed || !listed.ok) {
    return { ok: false, code: 'TASK_LOCK_DIR_UNREADABLE',
      message: 'the task lock directory could not be listed safely' };
  }
  if (listed.names.indexOf(name) < 0) return { ok: true, present: false };
  var file = path.join(paths.LOCKS_DIR, name);
  var bounded_ = fileGuards.boundedRegularFileUnder(
    paths.PROJECT_ROOT, paths.LOCKS_DIR, file, 1024 * 1024);
  if (!bounded_) {
    return { ok: false, code: 'TASK_LOCK_UNSAFE',
      message: 'the task lock is unreadable, oversized or unsafe' };
  }
  var value;
  try { value = JSON.parse(bounded_.bytes.toString('utf8')); }
  catch (error) {
    return { ok: false, code: 'TASK_LOCK_INVALID',
      message: 'the task lock is not valid JSON' };
  }
  var taskState = require('../../tasks/task-state-core.cjs');
  if (!taskState.canonicalLockV1(value, stem) || value.stage !== 'orchestrator') {
    return { ok: false, code: 'TASK_LOCK_INVALID',
      message: 'the task lock is not a canonical orchestrator ownership record' };
  }
  if (expectedRunId && value.runId !== expectedRunId) {
    return { ok: false, code: 'TASK_LOCK_OWNER_MISMATCH',
      message: 'the task lock belongs to a different execution run' };
  }
  return { ok: true, present: true, record: value };
}

function candidateReceiptMatches(record, receipt) {
  return receipt.worktreeId === record.worktreeId &&
    receipt.runId === record.runId && receipt.stem === record.stem &&
    receipt.candidateCommit === record.candidate.commit &&
    receipt.candidateTree === record.candidate.tree &&
    receipt.diffHash === record.candidate.diffHash &&
    receipt.receiptHash === record.candidate.receiptHash &&
    receipt.baseCommit === record.target.baseCommit &&
    receipt.baseTree === record.target.baseTree &&
    receipt.inputs.targetRef === record.target.ref &&
    receipt.inputs.targetCommit === record.target.baseCommit;
}

function pathPinsBetween(fromTree, toTree) {
  var entries = gitMutations.treeEntries(fromTree, toTree);
  if (entries === null) return null;
  return entries.map(function (entry) {
    return { path: entry.path, hash: worktreeContract.digest(entry) };
  });
}

function samePathPins(left, right) {
  if (!Array.isArray(left) || !Array.isArray(right) || left.length !== right.length) return false;
  var a = left.slice().sort(function (x, y) { return x.path < y.path ? -1 : x.path > y.path ? 1 : 0; });
  var b = right.slice().sort(function (x, y) { return x.path < y.path ? -1 : x.path > y.path ? 1 : 0; });
  return a.every(function (entry, index) {
    return entry.path === b[index].path && entry.hash === b[index].hash;
  });
}
// The commit a published target ref carries, when it matches this WAL's pins
// exactly. This is how a crash between `commit-tree` and the ref update, or
// between the ref update and the record write, is resolved without guessing.
function publishedCommitMatching(record) {
  var observed = refCommit(record.target.ref);
  if (observed === null || observed === record.target.baseCommit) return null;
  var probe = runGit(['cat-file', 'commit', observed]);
  if (!probe.ok) return null;
  var header = String(probe.stdout || '').split('\n\n')[0].split('\n');
  var parents = header.filter(function (line) { return line.indexOf('parent ') === 0; });
  if (parents.length !== 1 || parents[0] !== 'parent ' + record.target.baseCommit) return null;
  var treeLine = header.find(function (line) { return line.indexOf('tree ') === 0; }) || '';
  var tree = treeLine.indexOf('tree ') === 0 ? treeLine.slice('tree '.length) : '';
  if (!worktreeContract.COMMIT_RE.test(tree)) return null;
  if (worktreeContract.digest(tree) !== record.commitPin.stagedTreeHash) return null;
  // Parent + tree alone cannot tell OUR commit apart from someone else's commit
  // of the same staged index — and this repository has a background sweep that
  // commits exactly that. The pinned message is what makes the commit ours.
  var body = gitMutations.commitMessageBody(observed);
  if (body === null) return null;
  if (worktreeContract.digest(gitMutations.normalizedMessage(body)) !== record.commitPin.messageHash) return null;
  return observed;
}

// --------------------------------------------------------------------------
// The driver. One call advances the WAL as far as it can without blocking on
// a finalizer child; when a child is needed it is spawned through the
// finalizations supervisor and the driver resumes from its exit.
// --------------------------------------------------------------------------

function advance(stem, done) {
  var got = readOne(stem);
  if (!got.ok) return done(refuse(got.code, got.message || 'the integration record is unavailable'));
  var record = got.record;
  if (record.status === 'completed') return done({ ok: true, completed: true, integration: projectRecord(record) });
  if (record.status === 'recovery-required') {
    return done(refuse('INTEGRATION_RECOVERY_REQUIRED', 'this integration needs manual recovery', projectRecord(record)));
  }
  var receipt = worktreeManager.candidateReceipt(record.worktreeId);
  if (!receipt || !candidateReceiptMatches(record, receipt)) {
    return done(recoveryRequired(record, 'the candidate receipt no longer matches the recorded candidate'));
  }

  // §11: the uncommitted bytes the owner was allowed to keep are the ones this
  // transaction promised NOT to publish. They were pinned when it began; if one
  // changed since, the promise no longer describes reality — a resumed
  // transaction would publish an Outcome or a task source the owner has edited
  // since it was inspected. Pinning them and never comparing made the pin
  // decorative, which is worse than not having it.
  // These pins protect the hand-off TO the finalizer. Once its intent is
  // durable, prepare owns these paths: it consumes the task source and
  // regenerates INDEX.json. Comparing them to the pre-prepare bytes after that
  // effect mistakes the transaction's own output for a foreign edit. The
  // prepared artifacts are narrowed and hash-pinned separately in phase 5.
  if (record.phases['finalizer-preparing'].intentAt === null) {
    var dirtyDrift = record.controlSnapshot.dirtyAllowedPaths.filter(function (pin) {
      return hashOfWorkingFile(pin.path) !== pin.hash;
    });
    if (dirtyDrift.length) {
      return done(refuse('INTEGRATION_DIRTY_INPUT_CHANGED',
        'an uncommitted path this transaction was allowed to keep has changed since it began',
        projectRecord(record), dirtyDrift.map(function (pin) { return pin.path; })));
    }
  }

  // ---- phase 2/3: the candidate product diff lands in the control root ----
  if (record.phases['product-applied'].provenAt === null) {
    var applyIntent = markIntent(record, 'product-applying');
    if (!applyIntent.ok) return done(applyIntent);
    var applied = gitMutations.applyCandidate({ baseTree: record.target.baseTree, candidateTree: record.candidate.tree });
    if (!applied.ok) {
      // A refusal here has touched nothing: the check runs before the apply.
      return done(refuse(applied.code, applied.message, projectRecord(record)));
    }
    var proveApply = markProven(record, 'product-applying');
    if (!proveApply.ok) return done(proveApply);
    var appliedIntent = markIntent(record, 'product-applied');
    if (!appliedIntent.ok) return done(appliedIntent);
    if (gitMutations.controlIndexTree() !== record.candidate.tree) {
      return done(recoveryRequired(record, 'the control index does not describe the candidate tree after apply'));
    }
    var proveApplied = markProven(record, 'product-applied');
    if (!proveApplied.ok) return done(proveApplied);
  }

  // ---- phase 4: the finalizer prepares every global artifact ----
  if (record.phases['finalizer-preparing'].provenAt === null) {
    var markerState = finalizationMarker(stem);
    if (!markerState.ok) return done(recoveryRequired(record, markerState.message));
    var marker = markerState.marker;
    var prepIntent = markIntent(record, 'finalizer-preparing');
    if (!prepIntent.ok) return done(prepIntent);
    // The marker's succeeded bit is recovery intent, not a fresh proof of the
    // current bytes. Always re-enter prepare: its owner revalidates done,
    // INDEX, arch, mappings and receipts before it reports `prepared` again.
    var draft = outcomeDraftFile(stem, record.worktreeId);
    var draftArgs = [];
    if (!marker) {
      try { if (fs.lstatSync(draft).isFile()) draftArgs = ['--outcome-file', draft]; }
      catch (error) { draftArgs = []; }
    }
    return spawnFinalizer(record, ['--mode', 'prepare'].concat(draftArgs), function (outcome) {
      if (!outcome.ok) {
        // A refused gate keeps the transaction exactly where it is: the
        // owner repairs the task and resumes. Only physical damage is
        // recovery, and the finalizer marks that itself.
        return done(refuse('INTEGRATION_PREPARE_FAILED', outcome.message, projectRecord(record)));
      }
      var currentPrep = readOne(stem);
      if (!currentPrep.ok) return done(refuse(currentPrep.code, currentPrep.message));
      var provePrep = currentPrep.record.phases['finalizer-preparing'].provenAt === null ?
        markProven(currentPrep.record, 'finalizer-preparing') : currentPrep;
      if (!provePrep.ok) return done(provePrep);
      advance(stem, done);
    });
  }

  // ---- phase 5: pin the exact artifacts the finalizer produced ----
  if (record.phases['finalizer-prepared'].provenAt === null) {
    var preparedIntent = markIntent(record, 'finalizer-prepared');
    if (!preparedIntent.ok) return done(preparedIntent);
    var pins = preparedPathPins(stem, record.candidate.tree);
    if (!pins.ok) {
      // Foreign changes in the control root are the OWNER's to resolve, exactly
      // like a dirty precondition: nothing is staged, nothing is reverted, and
      // the transaction stays resumable once they are out of the way.
      if (pins.code === 'INTEGRATION_FOREIGN_ARTIFACTS') {
        return done({ ok: false, code: 'INTEGRATION_BLOCKED', message: pins.message,
          blockers: [blocker('dirty-control-root',
            'commit, stash or revert these paths, then resume the integration', pins.paths)] });
      }
      return done(recoveryRequired(record, 'the prepared artifacts could not be pinned: ' + pins.code));
    }
    var pinnedPaths = pins.pins.map(function (pin) { return pin.path; });
    var donePath = 'orchestrator/tasks/done/' + stem + '.md';
    // A newly-created task source can be untracked at the candidate base. In
    // that case prepare still consumes the working todo file, but there is no
    // todo entry in the candidate tree to delete. The done artifact is always
    // required; a tracked todo deletion, when one exists, is already part of
    // the exact candidate-to-prepared pin set.
    if (pinnedPaths.indexOf(donePath) < 0) {
      return done(recoveryRequired(record,
        'the finalizer did not publish ' + donePath + ' as a transaction artifact'));
    }
    record.finalizerPrepared = pins.pins;
    var provePrepared = markProven(record, 'finalizer-prepared');
    if (!provePrepared.ok) return done(provePrepared);
  }

  // ---- phase 6/7: ONE canonical commit ----
  if (record.phases['commit-published'].provenAt === null) {
    var message = commitMessage(record);
    // The commit's path set is the union of the candidate's own paths and the
    // finalizer's prepared artifacts — the two are disjoint by construction
    // (sealing forbids control-owned paths in a candidate).
    var expectedPaths = candidatePathsOf(receipt)
      .concat(record.finalizerPrepared.map(function (pin) { return pin.path; }));
    if (record.commitPin.publishedCommit === null) {
      var adopted = record.phases['commit-publishing'].intentAt !== null ? publishedCommitMatching(record) : null;
      if (adopted) {
        // The ref moved before the record could record it: adopt the physical
        // commit rather than publishing a second one.
        record.commitPin.publishedCommit = adopted;
        var proveAdopted = markProven(record, 'commit-publishing');
        if (!proveAdopted.ok) return done(proveAdopted);
      } else {
        var readied = gitMutations.prepareIntegrationCommit({
          targetRef: record.target.ref, expectedParent: record.target.baseCommit,
          message: message, expectedPaths: expectedPaths
        });
        if (!readied.ok) return done(refuse(readied.code, readied.message, projectRecord(record)));
        // The candidate tree already contains the exact sealed product bytes.
        // Therefore the ONLY allowed delta from it to the commit tree is the
        // exact finalizer diff pinned in phase 5. Comparing paths alone would
        // let a hook or another process replace bytes on an expected path.
        var observedPrepared = pathPinsBetween(record.candidate.tree, readied.stagedTree);
        if (observedPrepared === null) {
          return done(recoveryRequired(record, 'the prepared transaction tree could not be inspected'));
        }
        if (!samePathPins(record.finalizerPrepared, observedPrepared)) {
          var recordedByPath = Object.create(null);
          record.finalizerPrepared.forEach(function (pin) { recordedByPath[pin.path] = pin.hash; });
          var observedByPath = Object.create(null);
          observedPrepared.forEach(function (pin) { observedByPath[pin.path] = pin.hash; });
          var driftPaths = Object.keys(Object.assign({}, recordedByPath, observedByPath)).filter(function (entry) {
            return recordedByPath[entry] !== observedByPath[entry];
          }).sort();
          return done(refuse('INTEGRATION_PREPARED_ARTIFACT_DRIFT',
            'the staged transaction bytes no longer match the finalizer-prepared pins',
            projectRecord(record), driftPaths));
        }
        // A commit-msg hook may have rewritten the message; the pin — and the
        // commit — must carry what the hook left, not what it was handed.
        var finalMessage = readied.message;
        record.commitPin.stagedTreeHash = worktreeContract.digest(readied.stagedTree);
        record.commitPin.messageHash = worktreeContract.digest(gitMutations.normalizedMessage(finalMessage));
        record.commitPin.expectedParent = record.target.baseCommit;
        var publishIntent = markIntent(record, 'commit-publishing');
        if (!publishIntent.ok) return done(publishIntent);
        var committed = gitMutations.commitIntegration({
          targetRef: record.target.ref, expectedParent: record.target.baseCommit,
          stagedTree: readied.stagedTree, message: finalMessage, expectedPaths: expectedPaths
        });
        if (!committed.ok) {
          // The ref may still have moved while the error was produced.
          var late = publishedCommitMatching(record);
          if (!late) return done(refuse(committed.code, committed.message, projectRecord(record)));
          record.commitPin.publishedCommit = late;
        } else {
          record.commitPin.publishedCommit = committed.commit;
        }
        var provePublishing = markProven(record, 'commit-publishing');
        if (!provePublishing.ok) return done(provePublishing);
      }
    } else if (record.phases['commit-publishing'].provenAt === null) {
      var provePinned = markProven(record, 'commit-publishing');
      if (!provePinned.ok) return done(provePinned);
    }
    var publishedIntent = markIntent(record, 'commit-published');
    if (!publishedIntent.ok) return done(publishedIntent);
    // The tree the published commit actually carries must hash to the tree the
    // WAL pinned BEFORE the commit existed. That comparison — pin first, then
    // the full structural re-verification — is what makes an adopted commit as
    // trustworthy as one this process published itself.
    var publishedTree = treeOfCommit(record.commitPin.publishedCommit);
    if (publishedTree === null || worktreeContract.digest(publishedTree) !== record.commitPin.stagedTreeHash) {
      return done(recoveryRequired(record, 'the published commit does not carry the pinned tree'));
    }
    var verified = gitMutations.verifyPublishedCommit({
      targetRef: record.target.ref, commit: record.commitPin.publishedCommit,
      expectedParent: record.target.baseCommit, stagedTree: publishedTree,
      expectedPaths: expectedPaths
    });
    if (!verified.ok) return done(recoveryRequired(record, 'the published commit does not match the WAL: ' + verified.code));
    var provePublished = markProven(record, 'commit-published');
    if (!provePublished.ok) return done(provePublished);
  }

  // ---- phase 8: the finalizer confirms the commit and releases the lock ----
  if (record.phases['finalizer-confirming'].provenAt === null) {
    var confirmIntent = markIntent(record, 'finalizer-confirming');
    if (!confirmIntent.ok) return done(confirmIntent);
    // Marker absence is only an effect, never proof. Re-enter confirm even
    // after cleanup: the finalizer independently re-reads the WAL, commit and
    // current artifacts before the integration records this phase as proven.
    return spawnFinalizer(record, ['--mode', 'confirm',
      '--integration-id', record.integrationId,
      '--integration-commit', record.commitPin.publishedCommit], function (outcome) {
      if (!outcome.ok) return done(refuse('INTEGRATION_CONFIRM_FAILED', outcome.message, projectRecord(record)));
      var currentConfirm = readOne(stem);
      if (!currentConfirm.ok) return done(refuse(currentConfirm.code, currentConfirm.message));
      var proveConfirmed = currentConfirm.record.phases['finalizer-confirming'].provenAt === null ?
        markProven(currentConfirm.record, 'finalizer-confirming') : currentConfirm;
      if (!proveConfirmed.ok) return done(proveConfirmed);
      advance(stem, done);
    });
  }

  // ---- phase 9: cleanup is authorized ----
  var completedIntent = markIntent(record, 'completed');
  if (!completedIntent.ok) return done(completedIntent);
  var releaseMarker = finalizationMarker(stem);
  if (!releaseMarker.ok) return done(recoveryRequired(record, releaseMarker.message));
  var releaseLock = taskLockState(stem);
  if (!releaseLock.ok) return done(recoveryRequired(record, releaseLock.message));
  if (releaseLock.present || releaseMarker.marker !== null) {
    return done(recoveryRequired(record, 'the finalizer confirmed but its lock or marker is still present'));
  }
  var released = worktreeManager.release(record.worktreeId);
  if (!released.ok) {
    return done(refuse('INTEGRATION_CLEANUP_FAILED', released.message || released.code, projectRecord(record)));
  }
  record.status = 'completed';
  journalIntegration(record, 'ok', 'Integrated as commit ' +
    String(record.commitPin.publishedCommit).slice(0, 12));
  // The draft was published; leaving it behind is what let a stale one be
  // republished in the first place. Best effort — a failure here cannot undo a
  // committed transaction, and the generation binding already makes a leftover
  // draft unusable by any later generation. Keep even this non-authoritative
  // cleanup rooted: a raced finalizations-directory symlink must never turn a
  // cache cleanup into deletion of an external same-name file.
  fileGuards.unlinkRegularFileUnder(paths.PROJECT_ROOT, paths.FINALIZATIONS_DIR,
    outcomeDraftFile(stem, record.worktreeId), { allowMissing: true });
  var proveCompleted = markProven(record, 'completed');
  if (!proveCompleted.ok) return done(proveCompleted);
  return done({ ok: true, completed: true, commit: record.commitPin.publishedCommit,
    integration: projectRecord(proveCompleted.record) });
}

function candidatePathsOf(receipt) {
  return receipt.entries.map(function (entry) { return entry.path; })
    .concat(receipt.entries.filter(function (entry) { return entry.renameFrom; })
      .map(function (entry) { return entry.renameFrom; }));
}
function treeOfCommit(commit) {
  if (!worktreeContract.COMMIT_RE.test(String(commit || ''))) return null;
  var probe = runGit(['rev-parse', commit + '^{tree}']);
  return probe.ok && worktreeContract.COMMIT_RE.test(probe.stdout.trim()) ? probe.stdout.trim() : null;
}

// The finalizer always runs through the finalizations supervisor: one child
// registry, one timeout policy, one process-group termination proof — and, by
// construction, no integration can run while a finalization recovery does.
function spawnFinalizer(record, extraArgs, settled) {
  var finalizations = require('./finalizations');
  var started = finalizations.runFinalizer(record.stem, {
    extraArgs: extraArgs,
    onSettled: function (outcome) { settled(outcome); }
  });
  if (!started.ok || started.accepted === false) {
    settled({ ok: false, message: started.error ? started.error + (started.detail ? ': ' + started.detail : '')
      : 'the finalizer is already running' });
  }
}

// --------------------------------------------------------------------------
// Public entry points.
// --------------------------------------------------------------------------

// Begin a transaction: publish the WAL no-clobber (that publication IS the
// repository-wide integration mutex) and drive it.
function begin(stem, done) {
  var existing = readOne(stem);
  if (existing.ok) {
    if (existing.record.status === 'completed') {
      // A completed record is history. It must not outlive the generation it
      // integrated: a reopened task gets a NEW run, a NEW worktree and a NEW
      // sealed candidate, and that generation deserves its own transaction.
      // Only a PROVEN new generation reopens this stem. An unreadable store is
      // not evidence of one, so it keeps the completed record standing.
      var live = worktreeManager.activeRecordFor(stem);
      if (!live.ok || !live.record || live.record.worktreeId === existing.record.worktreeId) {
        return done(refuse('INTEGRATION_ALREADY_COMPLETED', stem + ' has already been integrated',
          projectRecord(existing.record)));
      }
    } else {
      return advance(stem, done);
    }
  }
  if (existing.code === 'INTEGRATION_RECORD_INVALID') {
    return done(refuse(existing.code, existing.message));
  }
  var guard = publicationGuard.acquire();
  if (!guard.ok) return done(refuse(guard.code, guard.message));
  var result = null;
  var wal = null;
  var resumeExisting = false;
  var released = false;
  try {
    // Re-read every decision while the cross-stem publication guard is held.
    // This closes the old TOCTOU where two processes both observed an empty
    // store and atomically created two different <stem>.json files.
    existing = readOne(stem);
    if (existing.ok && existing.record.status !== 'completed') {
      resumeExisting = true;
    } else if (!existing.ok && existing.code === 'INTEGRATION_RECORD_INVALID') {
      result = refuse(existing.code, existing.message);
    } else if (existing.ok && existing.record.status === 'completed') {
      var currentGeneration = worktreeManager.activeRecordFor(stem);
      if (!currentGeneration.ok || !currentGeneration.record ||
          currentGeneration.record.worktreeId === existing.record.worktreeId) {
        result = refuse('INTEGRATION_ALREADY_COMPLETED', stem + ' has already been integrated',
          projectRecord(existing.record));
      }
    }
    if (!result && !resumeExisting) {
      var verdict = preconditions(stem, guard.token);
      if (!verdict.ok) {
        result = { ok: false, code: 'INTEGRATION_BLOCKED', message: 'preconditions are not met',
          blockers: verdict.blockers };
      } else {
        var record = verdict.context.record;
        var receipt = verdict.context.receipt;
        var phases = {};
        contract.PHASES.forEach(function (name) { phases[name] = { intentAt: null, provenAt: null }; });
        var createdAt = now();
        phases.prepared = { intentAt: createdAt, provenAt: createdAt };
        wal = {
          version: 1,
          integrationId: 'ig-' + crypto.randomBytes(16).toString('hex'),
          stem: stem, runId: record.runId, worktreeId: record.worktreeId,
          phase: 'prepared', status: 'active',
          candidate: { commit: receipt.candidateCommit, tree: receipt.candidateTree,
            diffHash: receipt.diffHash, receiptHash: receipt.receiptHash },
          target: { ref: record.targetRef, baseCommit: record.baseCommit, baseTree: record.baseTree },
          controlSnapshot: { headCommit: record.baseCommit,
            dirtyAllowedPaths: verdict.context.dirtyAllowed.map(function (entry) {
              return { path: entry, hash: hashOfWorkingFile(entry) };
            }).filter(function (pin) { return pin.hash !== null; }) },
          commitPin: { stagedTreeHash: null, messageHash: null, expectedParent: null, publishedCommit: null },
          finalizerPrepared: null,
          phases: phases, owner: ownerIdentity(),
          createdAt: createdAt, updatedAt: createdAt,
          recordHash: 'sha256:' + '0'.repeat(64)
        };
        // No-clobber unless the record being replaced is a COMPLETED one for a
        // previous generation, which was re-proven under the guard above.
        var replacingCompleted = existing.ok && existing.record.status === 'completed';
        var published = writeRecord(wal, !replacingCompleted,
          replacingCompleted ? existing.record : null);
        if (!published.ok) result = published;
      }
    }
  } finally {
    released = publicationGuard.release(guard);
  }
  if (!released) {
    return done(refuse('INTEGRATION_PUBLICATION_GUARD_RELEASE_FAILED',
      'the WAL publication guard could not be released safely'));
  }
  if (result) return done(result);
  if (resumeExisting) return advance(stem, done);
  journalIntegration(wal, 'info', 'Integration started for candidate ' +
    String(wal.candidate.commit).slice(0, 12) + ' on ' + wal.target.ref);
  return advance(stem, done);
}

function hashOfWorkingFile(relative) {
  try {
    var bytes = fs.readFileSync(path.join(paths.PROJECT_ROOT, relative));
    return 'sha256:' + crypto.createHash('sha256').update(bytes).digest('hex');
  } catch (error) { return null; }
}

// Manual recovery is intentionally narrow: before any canonical commit exists,
// a phase-5 classifier bug can be reconciled from the physical prepared tree.
// Every artifact is re-derived, restricted to the finalizer-owned set and
// hash-pinned before the WAL is allowed to become active again.
function reconcilePreparedRecovery(record) {
  if (record.phase !== 'finalizer-prepared' || record.finalizerPrepared !== null ||
      record.commitPin.publishedCommit !== null ||
      record.phases['finalizer-preparing'].provenAt === null ||
      record.phases['finalizer-prepared'].intentAt === null ||
      record.phases['finalizer-prepared'].provenAt !== null ||
      record.phases['commit-publishing'].intentAt !== null) return null;
  var markerState = finalizationMarker(record.stem);
  if (!markerState.ok || markerState.marker === null) return null;
  var receipt = worktreeManager.candidateReceipt(record.worktreeId);
  if (!receipt || !candidateReceiptMatches(record, receipt)) return null;
  var pins = preparedPathPins(record.stem, record.candidate.tree);
  if (!pins.ok) return null;
  var donePath = 'orchestrator/tasks/done/' + record.stem + '.md';
  if (!pins.pins.some(function (pin) { return pin.path === donePath; })) return null;
  record.finalizerPrepared = pins.pins;
  record.status = 'active';
  return markProven(record, 'finalizer-prepared');
}

// Continue an interrupted transaction. Identical to begin() for an existing
// record: every phase re-proves itself physically before anything moves.
function resume(stem, done) {
  var existing = readOne(stem);
  if (!existing.ok) return done(refuse(existing.code, existing.message || 'no integration to resume'));
  if (existing.record.status === 'recovery-required') {
    var reconciled = reconcilePreparedRecovery(existing.record);
    if (!reconciled || !reconciled.ok) {
      return done(refuse('INTEGRATION_RECOVERY_REQUIRED', 'this integration needs manual recovery',
        projectRecord(existing.record)));
    }
  }
  return advance(stem, done);
}

// §10.4 recovery, the operator's half. A 'recovery-required' record is real
// evidence and this NEVER deletes, rolls back or repairs anything: it records
// that a human looked at the record, decided the transaction will not continue,
// and released the repository-wide mutex it was holding. Everything the
// transaction already did stays exactly where it is — including a published
// commit, which is why the decision is a human's and not a timer's.
//
// The exact integrationId must be supplied. That is the whole confirmation: it
// cannot be pressed on a record the operator has not actually read, and it
// cannot land on a record that changed under them.
function abandon(stem, integrationId) {
  var existing = readOne(stem);
  if (!existing.ok) return refuse(existing.code, existing.message || 'no integration to abandon');
  var record = existing.record;
  if (record.status !== 'recovery-required') {
    return refuse('INTEGRATION_NOT_RECOVERABLE',
      'only a recovery-required transaction can be abandoned; this one is ' + record.status,
      projectRecord(record));
  }
  if (record.integrationId !== integrationId) {
    return refuse('INTEGRATION_ID_MISMATCH',
      'the integration id does not match the record on disk', projectRecord(record));
  }
  record.status = 'abandoned';
  var written = writeRecord(record, false);
  if (!written.ok) return refuse(written.code, written.message);
  journalIntegration(record, 'blocked', 'Integration abandoned by the operator at phase ' + record.phase);
  return { ok: true, stem: stem, integration: projectRecord(record) };
}

// The repository-wide mutex probe for other writers (board mutations, runner
// admission). An unreadable store blocks: unprovable state is never free.
function activeIssue() {
  var publishing = publicationGuard.issue();
  if (publishing.active) return { active: true, reason: publishing.reason, stem: null };
  var store = list();
  if (store.unavailable) return { active: true, reason: 'integration-records-unreadable', stem: null };
  if (store.invalid.length) return { active: true, reason: 'integration-records-invalid', stem: null };
  var busy = store.records.find(function (record) { return record.status === 'active'; });
  if (busy) return { active: true, reason: 'integration-active', stem: busy.stem, phase: busy.phase };
  var broken = store.records.find(function (record) { return record.status === 'recovery-required'; });
  if (broken) return { active: true, reason: 'integration-recovery-required', stem: broken.stem, phase: broken.phase };
  return { active: false, reason: null, stem: null };
}

// Board projection: every unfinished transaction, plus the completed one for
// the stem being looked at.
function projection() {
  var store = list();
  return {
    version: 1,
    unavailable: store.unavailable,
    invalid: store.invalid.map(function (entry) { return { name: entry.name, code: entry.code }; }),
    records: store.records.map(projectRecord)
  };
}

function integrationIntegrityFinding(code, stem, message) {
  return { code: code, severity: 'error', stem: stem || null, paths: [DIR],
    message: message,
    recovery: 'Resume the exact integration through the Board; never clear integration state by age or by hand.' };
}
function scanIntegrity(scope) {
  var stem = typeof scope === 'string' ? scope : scope && scope.stem || null;
  var out = { version: 1, owner: 'integrations', statuses: [], findings: [], snapshotInputs: [], truncated: false };
  var store = list();
  if (store.unavailable) {
    out.findings.push(integrationIntegrityFinding('INTEGRATION_STORE_UNREADABLE', null,
      'the integration record store is unreadable: ' + store.unavailable));
    return out;
  }
  store.invalid.forEach(function (entry) {
    out.findings.push(integrationIntegrityFinding('INTEGRATION_RECORD_INVALID', null,
      entry.name + ': ' + entry.code));
  });
  store.records.filter(function (record) { return !stem || record.stem === stem; }).forEach(function (record) {
    out.statuses.push({ stem: record.stem, status: record.status, phase: record.phase });
    out.snapshotInputs.push({ path: recordFile(record.stem), hash: record.recordHash });
    if (record.status === 'recovery-required') {
      out.findings.push(integrationIntegrityFinding('INTEGRATION_RECOVERY_REQUIRED', record.stem,
        record.integrationId + ' stopped at phase ' + record.phase + ' and needs recovery'));
    }
    // An abandoned record is history the operator signed for: it is reported in
    // `statuses` above, but it is no longer a finding and no longer blocks.

  });
  return out;
}

module.exports = {
  DIR: DIR,
  readOne: readOne,
  list: list,
  dirMtime: dirMtime,
  preconditions: preconditions,
  preview: preview,
  abandon: abandon,
  projection: projection,
  commitMessage: commitMessage,
  publishedCommitMatching: publishedCommitMatching,
  activeIssue: activeIssue,
  scanIntegrity: scanIntegrity,
  begin: begin,
  resume: resume,
  advance: advance
};
