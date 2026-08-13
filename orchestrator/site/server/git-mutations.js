'use strict';

// ---------------------------------------------------------------------------
// THE single Git mutation owner (pipeline improvement 01, §17). Every Git
// mutation the worktree pipeline ever performs goes through this module and
// nothing else — `server/git.js` stays a read-only observer, and the
// worktree-manager only discovers. Mutation allowlist:
//   addWorktree        — namespaced branch + worktree at an exact base commit
//   removeOwnedWorktree — remove ONE manager-owned checkout (never --force)
//   releaseOwnedRef    — atomically create a durable release marker and
//                        CAS-delete ONE manager-namespace branch. The marker
//                        makes crash replay and same-commit ref ABA observable.
//   prepareCandidate   — §9.5: stage the exact authorized product paths into a
//                        MANAGER-OWNED temporary index (never the child's),
//                        build the candidate tree, derive the canonical diff
//                        manifest, run the repository hooks, and create ONE
//                        temporary candidate commit object.
//   publishCandidate   — publish that already-receipted object to the temporary
//                        candidate ref by exact compare-and-swap.
//   applyCandidate     — §10.3: apply the exact candidate diff bytes into the
//                        CONTROL index/worktree under the integration WAL
//   stageTransactionPaths — stage the finalizer's own tracked artifacts
//   commitIntegration  — §10.5: ONE canonical commit on the exact target base
//                        with the user's own configured identity, every hook
//                        run, published by CAS and re-proven afterwards
//
// Doctrine: argv-only spawn, no shell; a durable INTENT receipt is written
// before every mutation and an OUTCOME receipt after it (both no-clobber, in
// the control cache); postconditions are re-verified through fresh reads;
// `--force`, `--no-verify`, `worktree prune`, foreign refs and foreign
// worktrees are structurally unreachable. hooksPath enforcement matters only
// for commit-performing operations and lands with them (Phase 3+).
// ---------------------------------------------------------------------------

var cp = require('child_process');
var crypto = require('crypto');
var fs = require('fs');
var path = require('path');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var worktreeContract = require('../../tasks/worktree-record-contract.cjs');
var candidateContract = require('../../tasks/candidate-receipt-contract.cjs');

var GIT_TIMEOUT_MS = 60 * 1000;
var GIT_MAX_BUFFER = 8 * 1024 * 1024;
var RECEIPTS_DIR = path.join(paths.WORKTREE_RECORDS_DIR, '.mutations');
var MANAGER_REF_PREFIX = 'refs/heads/orchestrator/task/';
var RELEASE_REF_PREFIX = 'refs/orchestrator/releases/';
// §9.5: the temporary candidate commit carries a fixed local-only identity
// that never reaches canonical history (the integration commit in Phase 4
// uses the user's own configured identity).
var CANDIDATE_IDENTITY_NAME = 'Orchestrator Candidate';
var CANDIDATE_IDENTITY_EMAIL = 'orchestrator@local.invalid';

function mutationDirectory() {
  return fileGuards.realDirectoryUnder(
    paths.WORKTREE_RECORDS_AUTHORITY_ROOT, RECEIPTS_DIR,
    { create: true, mode: 0o700 });
}
function removeMutationFile(file) {
  return fileGuards.unlinkRegularFileUnder(
    paths.WORKTREE_RECORDS_AUTHORITY_ROOT, RECEIPTS_DIR, file,
    { allowMissing: true });
}

// Exact argument-prefix allowlist. Read verbs are for pre/post verification;
// the three mutating prefixes are the entire Phase-2 mutation surface.
var ALLOWED_PREFIXES = [
  ['rev-parse'],
  ['symbolic-ref'],
  ['ls-files'],
  ['config', '--get'],
  ['apply'],
  ['diff', '--check'],
  ['diff', '--name-only'],
  ['diff', '--binary'],
  ['diff-tree'],
  ['read-tree'],
  ['update-index'],
  ['write-tree'],
  ['commit-tree'],
  ['cat-file'],
  ['worktree', 'list'],
  ['worktree', 'add'],
  ['worktree', 'remove'],
  ['update-ref', '--stdin'],
  ['check-ref-format']
];
function allowed(args) {
  if (!Array.isArray(args) || !args.length) return false;
  if (args.some(function (token) { return token === '--force' || token === '-f' || token === '--no-verify'; })) return false;
  // Ref publication is the one shape whose second token is data, not a verb:
  // `update-ref <ref> <new> <old>` — always a compare-and-swap, and only ever
  // to a manager-namespace candidate branch or to a plain local target branch
  // (§10.5 publishes the ONE canonical commit exactly this way; targetRefValid
  // refuses the manager namespace, reserved names and everything remote).
  if (args[0] === 'update-ref' && args[1] !== '--stdin') {
    return args.length === 4 &&
      (managerRef(args[1]) || worktreeContract.targetRefValid(args[1])) &&
      worktreeContract.COMMIT_RE.test(String(args[2] || '')) &&
      worktreeContract.COMMIT_RE.test(String(args[3] || ''));
  }
  return ALLOWED_PREFIXES.some(function (prefix) {
    return prefix.every(function (token, index) { return args[index] === token; });
  });
}
function runGit(args, cwd, options) {
  if (!allowed(args)) return { ok: false, code: 'GIT_ARGS_NOT_ALLOWED', stdout: '', stderr: '' };
  var env = {};
  Object.keys(process.env).forEach(function (key) {
    if (key.indexOf('GIT_') !== 0) env[key] = process.env[key];
  });
  env.GIT_TERMINAL_PROMPT = '0';
  env.LC_ALL = 'C';
  // Sealing drives a MANAGER-OWNED temporary index and a fixed local-only
  // identity. Both are set here explicitly — never inherited from the ambient
  // environment, which the loop above strips.
  if (options && typeof options.indexFile === 'string') env.GIT_INDEX_FILE = options.indexFile;
  if (options && options.candidateIdentity === true) {
    env.GIT_AUTHOR_NAME = CANDIDATE_IDENTITY_NAME;
    env.GIT_AUTHOR_EMAIL = CANDIDATE_IDENTITY_EMAIL;
    env.GIT_COMMITTER_NAME = CANDIDATE_IDENTITY_NAME;
    env.GIT_COMMITTER_EMAIL = CANDIDATE_IDENTITY_EMAIL;
  }
  var result;
  try {
    result = cp.spawnSync('git', args, {
      cwd: cwd, env: env, timeout: GIT_TIMEOUT_MS, maxBuffer: GIT_MAX_BUFFER, encoding: 'utf8',
      input: options && typeof options.input === 'string' ? options.input : undefined
    });
  } catch (error) {
    return { ok: false, code: 'GIT_SPAWN_FAILED', stdout: '', stderr: String(error && error.message || '') };
  }
  if (result.error || result.signal || typeof result.status !== 'number') {
    return { ok: false, code: 'GIT_UNAVAILABLE', stdout: '', stderr: String(result.stderr || '') };
  }
  return { ok: result.status === 0, code: result.status === 0 ? null : 'GIT_EXIT_' + result.status,
    stdout: result.stdout || '', stderr: result.stderr || '' };
}

function managerRef(ref) {
  return typeof ref === 'string' && ref.indexOf(MANAGER_REF_PREFIX) === 0 &&
    worktreeContract.CANDIDATE_REF_RE.test(ref);
}
function releaseMarkerRef(worktreeId) {
  return worktreeContract.WORKTREE_ID_RE.test(String(worktreeId || ''))
    ? RELEASE_REF_PREFIX + worktreeId : null;
}
function refCommit(rootDir, ref) {
  var probe = runGit(['rev-parse', '-q', '--verify', ref], rootDir);
  return probe.ok ? probe.stdout.trim() : null;
}
function exactRefState(rootDir, ref) {
  var probe = runGit(['rev-parse', '-q', '--verify', ref], rootDir);
  if (probe.ok) return { status: 'present', commit: probe.stdout.trim() };
  if (probe.code === 'GIT_EXIT_1') return { status: 'absent', commit: null };
  return { status: 'unsafe', commit: null };
}

// Durable no-clobber receipts. A crash between intent and outcome leaves the
// intent visible to discovery/recovery; nothing here ever deletes a receipt.
function writeReceipt(kind, phase, payload) {
  var record = {
    version: 1, kind: kind, phase: phase, at: new Date().toISOString(),
    owner: { hostname: require('os').hostname(), pid: process.pid },
    payload: payload
  };
  var name = Date.now() + '-' + crypto.randomBytes(6).toString('hex') + '-' + kind + '-' + phase + '.json';
  var file = path.join(RECEIPTS_DIR, name);
  var published = fileGuards.publishNoClobberRegularFileUnder(
    paths.WORKTREE_RECORDS_AUTHORITY_ROOT, RECEIPTS_DIR, file,
    Buffer.from(JSON.stringify(record) + '\n', 'utf8'),
    { create: true, directoryMode: 0o700, mode: 0o600 });
  return published && published.ok ? file : null;
}
function fail(code, message) {
  return { ok: false, code: code, message: String(message || code).slice(0, 400) };
}

// Create the namespaced branch and worktree at an exact base commit.
// Preconditions: ref inside the manager namespace and absent; target path
// absent; base commit resolvable. Postconditions: checkout exists on the
// exact branch at the exact commit. Collisions are never reused — the caller
// mints a fresh generation instead (plan §7.3).
function addWorktree(options) {
  var rootDir = paths.PROJECT_ROOT;
  var ref = options && options.candidateRef;
  var target = options && options.targetPath;
  var base = options && options.baseCommit;
  if (!managerRef(ref)) return fail('MUTATION_REF_NOT_ALLOWED', 'candidate ref is outside the manager namespace');
  if (typeof target !== 'string' || !path.isAbsolute(target)) return fail('MUTATION_PATH_INVALID', 'target path must be absolute');
  if (typeof base !== 'string' || !worktreeContract.COMMIT_RE.test(base)) return fail('MUTATION_BASE_INVALID', 'base commit must be a full id');
  var checkFormat = runGit(['check-ref-format', ref], rootDir);
  if (!checkFormat.ok) return fail('MUTATION_REF_NOT_ALLOWED', 'git refused the candidate ref format');
  if (refCommit(rootDir, ref) !== null) return fail('MUTATION_REF_COLLISION', ref + ' already exists');
  try { fs.lstatSync(target); return fail('MUTATION_PATH_COLLISION', target + ' already exists'); }
  catch (error) { /* absent as required */ }
  if (refCommit(rootDir, base) !== base) return fail('MUTATION_BASE_UNRESOLVED', 'base commit is not resolvable');
  var intent = writeReceipt('add-worktree', 'intent', { candidateRef: ref, targetPath: target, baseCommit: base });
  if (!intent) return fail('MUTATION_RECEIPT_UNWRITABLE', 'intent receipt could not be published');
  var branch = ref.slice('refs/heads/'.length);
  var added = runGit(['worktree', 'add', '-b', branch, target, base], rootDir);
  if (!added.ok) {
    writeReceipt('add-worktree', 'failed', { candidateRef: ref, targetPath: target, baseCommit: base,
      code: added.code, stderr: String(added.stderr).slice(0, 400) });
    return fail('MUTATION_ADD_FAILED', added.stderr || added.code);
  }
  // Post-verify through fresh reads: exact branch, exact commit, exact path.
  var head = runGit(['rev-parse', 'HEAD'], target);
  var symbolic = runGit(['symbolic-ref', '-q', 'HEAD'], target);
  if (!head.ok || head.stdout.trim() !== base || !symbolic.ok || symbolic.stdout.trim() !== ref) {
    writeReceipt('add-worktree', 'postcondition-failed', { candidateRef: ref, targetPath: target, baseCommit: base });
    return fail('MUTATION_POSTCONDITION_FAILED', 'created worktree does not match the recorded intent');
  }
  writeReceipt('add-worktree', 'done', { candidateRef: ref, targetPath: target, baseCommit: base });
  return { ok: true, branch: branch, candidateRef: ref, targetPath: target, baseCommit: base };
}

// Remove ONE manager-owned checkout. The caller must have proven ownership
// (an exact record binding) before calling; this owner re-checks only the
// mechanical preconditions. `worktree remove` without --force refuses dirty
// or locked checkouts — that refusal is the desired fail-closed outcome.
function removeOwnedWorktree(options) {
  var rootDir = paths.PROJECT_ROOT;
  var target = options && options.targetPath;
  var home = null;
  try { home = fs.realpathSync.native(paths.WORKTREE_HOME).normalize('NFC'); } catch (error) {}
  if (typeof target !== 'string' || !path.isAbsolute(target)) return fail('MUTATION_PATH_INVALID', 'target path must be absolute');
  var real = null;
  try { real = fs.realpathSync.native(target).normalize('NFC'); } catch (error) {}
  if (real === null || home === null || real.indexOf(home + path.sep) !== 0) {
    return fail('MUTATION_PATH_NOT_OWNED', 'refusing to remove a checkout outside the worktree home');
  }
  var intent = writeReceipt('remove-worktree', 'intent', { targetPath: real });
  if (!intent) return fail('MUTATION_RECEIPT_UNWRITABLE', 'intent receipt could not be published');
  var removed = runGit(['worktree', 'remove', real], rootDir);
  if (!removed.ok) {
    writeReceipt('remove-worktree', 'failed', { targetPath: real, code: removed.code,
      stderr: String(removed.stderr).slice(0, 400) });
    return fail('MUTATION_REMOVE_FAILED', removed.stderr || removed.code);
  }
  var gone = true;
  try { fs.lstatSync(real); gone = false; } catch (error) {}
  if (!gone) {
    writeReceipt('remove-worktree', 'postcondition-failed', { targetPath: real });
    return fail('MUTATION_POSTCONDITION_FAILED', 'checkout still exists after removal');
  }
  writeReceipt('remove-worktree', 'done', { targetPath: real });
  return { ok: true };
}

// Atomically publish a durable release proof and CAS-delete one candidate ref.
// The two ref updates commit as one Git transaction, so recovery can distinguish
// "our delete committed" from an absent/foreign ref. The marker deliberately
// remains after release: deleting it would recreate the same crash ambiguity.
function releaseOwnedRef(options) {
  var rootDir = paths.PROJECT_ROOT;
  var ref = options && options.candidateRef;
  var expected = options && options.expectedCommit;
  var marker = releaseMarkerRef(options && options.worktreeId);
  if (!managerRef(ref)) return fail('MUTATION_REF_NOT_ALLOWED', 'ref is outside the manager namespace');
  if (marker === null) return fail('MUTATION_RELEASE_ID_INVALID', 'worktree id is not canonical');
  if (typeof expected !== 'string' || !worktreeContract.COMMIT_RE.test(expected)) {
    return fail('MUTATION_BASE_INVALID', 'expected commit must be a full id');
  }
  var currentState = exactRefState(rootDir, ref);
  var markerState = exactRefState(rootDir, marker);
  if (currentState.status === 'unsafe' || markerState.status === 'unsafe') {
    return fail('MUTATION_REF_STATE_UNSAFE', 'candidate or release-marker state is unreadable');
  }
  var current = currentState.commit;
  var marked = markerState.commit;
  if (markerState.status === 'present') {
    if (marked !== expected) {
      return fail('MUTATION_RELEASE_MARKER_FOREIGN', marker + ' carries an unexpected commit');
    }
    if (current === null) return { ok: true, releaseMarkerRef: marker, replayed: true };
    return fail('MUTATION_REF_REAPPEARED', ref + ' exists after its durable release marker');
  }
  if (currentState.status === 'present' && current !== expected) {
    return fail('MUTATION_REF_MOVED', ref + ' no longer points at the recorded commit');
  }
  var intent = writeReceipt('release-ref', 'intent', {
    worktreeId: options.worktreeId, candidateRef: ref, expectedCommit: expected, releaseMarkerRef: marker
  });
  if (!intent) return fail('MUTATION_RECEIPT_UNWRITABLE', 'intent receipt could not be published');
  var candidateOperation = currentState.status === 'present'
    ? 'delete ' + ref + ' ' + expected + '\n'
    : 'verify ' + ref + ' ' + '0'.repeat(40) + '\n';
  var transaction = 'start\n' +
    'create ' + marker + ' ' + expected + '\n' + candidateOperation +
    'prepare\ncommit\n';
  var deleted = runGit(['update-ref', '--stdin'], rootDir, { input: transaction });
  if (!deleted.ok) {
    writeReceipt('release-ref', 'failed', { worktreeId: options.worktreeId,
      candidateRef: ref, expectedCommit: expected, releaseMarkerRef: marker,
      code: deleted.code, stderr: String(deleted.stderr).slice(0, 400) });
    return fail('MUTATION_REF_DELETE_FAILED', deleted.stderr || deleted.code);
  }
  if (refCommit(rootDir, ref) !== null || refCommit(rootDir, marker) !== expected) {
    writeReceipt('release-ref', 'postcondition-failed', { worktreeId: options.worktreeId,
      candidateRef: ref, releaseMarkerRef: marker });
    return fail('MUTATION_POSTCONDITION_FAILED', 'release ref transaction postcondition failed');
  }
  writeReceipt('release-ref', 'done', { worktreeId: options.worktreeId,
    candidateRef: ref, expectedCommit: expected, releaseMarkerRef: marker });
  return { ok: true, releaseMarkerRef: marker, replayed: false,
    candidateWasAbsent: currentState.status === 'absent' };
}

// ---------------------------------------------------------------------------
// §9.5 candidate sealing. The child works in the execution root and may leave
// its index in any state; sealing NEVER trusts it. A manager-owned temporary
// index is seeded from the base tree, the exact authorized product paths are
// staged into it NUL-safely, and the candidate tree is written from THAT
// index. The result is compared tree-to-tree, so a path that was already
// dirty before the run and changed again by the task still carries its exact
// old and new blob ids — the failure mode the old set-difference footprint
// could not see.
// ---------------------------------------------------------------------------

var STATUS_MAX_ENTRIES = 20000;
// Materialized by provisioning inside every checkout (install-skills.sh); the
// manager owns them, so they are never deliverables and never violations.
var MANAGER_INSTALLED_PREFIXES = ['.claude/skills/', '.claude/contracts/'];

function parseNulList(stdout) {
  return String(stdout || '').split('\0').filter(function (entry) { return entry !== ''; });
}

// Every path whose working-tree state differs from the SEALED BASE TREE, plus
// untracked files. Deliberately NOT `git status`: status compares against
// HEAD, and after a first seal HEAD is the candidate commit — a re-seal would
// then stage only what changed since that candidate and silently drop the
// rest of the task's work. Comparing against the base tree is the invariant
// that makes a candidate always complete, and re-sealable.
function pathsDifferingFromBase(executionRoot, baseTree, indexFile) {
  // EVERY probe runs against a manager-owned index seeded from the base tree.
  // The child's index is never consulted: assume-unchanged / skip-worktree
  // bits set there would otherwise hide a real change from the receipt, and a
  // force-added ignored file would enter it.
  var options = { indexFile: indexFile };
  var seeded = runGit(['read-tree', baseTree], executionRoot, options);
  if (!seeded.ok) return { ok: false, code: 'SEAL_INDEX_UNAVAILABLE' };
  var unmerged = runGit(['ls-files', '--unmerged', '-z'], executionRoot, options);
  if (!unmerged.ok) return { ok: false, code: 'SEAL_STATUS_UNAVAILABLE' };
  if (parseNulList(unmerged.stdout).length) return { ok: false, code: 'SEAL_CONFLICT_STAGES' };
  var tracked = runGit(['diff', '--name-only', '-z', '--no-renames', baseTree], executionRoot, options);
  if (!tracked.ok) return { ok: false, code: 'SEAL_STATUS_UNAVAILABLE' };
  // No --directory: it would collapse every untracked directory into a single
  // entry. Plain --others lists untracked FILES individually and still emits a
  // nested repository as a bare directory entry, which is what the check below
  // is looking for.
  var untracked = runGit(['ls-files', '--others', '--exclude-standard', '-z'], executionRoot, options);
  if (!untracked.ok) return { ok: false, code: 'SEAL_STATUS_UNAVAILABLE' };
  var others = parseNulList(untracked.stdout);
  // A nested repository is reported as a bare directory entry; it is a typed
  // unsupported state (§18), not a mysterious path-grammar refusal.
  var nested = others.filter(function (entry) { return entry.slice(-1) === '/'; });
  if (nested.length) {
    return { ok: false, code: 'SEAL_NESTED_REPOSITORY', detail: nested.slice(0, 3).join(', ') };
  }
  var paths = parseNulList(tracked.stdout).concat(others);
  if (paths.length > STATUS_MAX_ENTRIES) return { ok: false, code: 'SEAL_STATUS_OVERSIZED' };
  return { ok: true, paths: paths };
}

// Parse `git diff-tree --raw -z -M` into canonical receipt entries.
function parseRawDiff(stdout) {
  var tokens = String(stdout || '').split('\0');
  var entries = [];
  var i = 0;
  while (i < tokens.length) {
    var meta = tokens[i];
    if (!meta || meta[0] !== ':') { i += 1; continue; }
    var fields = meta.slice(1).split(' ');
    if (fields.length < 5) return null;
    var oldMode = fields[0];
    var newMode = fields[1];
    var oldBlob = fields[2];
    var newBlob = fields[3];
    var statusField = fields[4];
    var statusLetter = statusField[0];
    var pathA = tokens[i + 1];
    if (statusLetter === 'R' || statusLetter === 'C') {
      var pathB = tokens[i + 2];
      i += 3;
      if (!pathA || !pathB) return null;
      entries.push({ path: pathB, operation: statusLetter === 'R' ? 'rename' : 'add',
        oldMode: statusLetter === 'R' ? oldMode : null,
        newMode: newMode,
        oldBlob: statusLetter === 'R' ? oldBlob : null,
        newBlob: newBlob,
        renameFrom: statusLetter === 'R' ? pathA : null });
      continue;
    }
    i += 2;
    if (!pathA) return null;
    if (statusLetter === 'A') {
      entries.push({ path: pathA, operation: 'add', oldMode: null, newMode: newMode,
        oldBlob: null, newBlob: newBlob, renameFrom: null });
    } else if (statusLetter === 'D') {
      entries.push({ path: pathA, operation: 'delete', oldMode: oldMode, newMode: null,
        oldBlob: oldBlob, newBlob: null, renameFrom: null });
    } else if (statusLetter === 'M' || statusLetter === 'T') {
      entries.push({ path: pathA,
        operation: oldBlob === newBlob ? 'mode' : 'modify',
        oldMode: oldMode, newMode: newMode, oldBlob: oldBlob, newBlob: newBlob, renameFrom: null });
    } else {
      return null; // unknown status letter: fail closed rather than guess
    }
  }
  entries.sort(function (a, b) { return a.path < b.path ? -1 : a.path > b.path ? 1 : 0; });
  return entries;
}

function candidateMessage(stem) {
  return 'candidate: ' + (stem || 'task') + '\n\nTemporary manager-owned candidate commit.\n' +
    'It never enters canonical history; Integrate publishes one canonical commit.\n';
}

// Prepare the candidate: verify the branch is still at the exact ref generation
// the manager pinned, stage the authorized product paths, build the tree, derive
// the manifest, run hooks and create the commit object. Ref publication is a
// separate mutation: the manager first durably publishes the candidate receipt
// as the write-ahead intent, so a crash can never leave a moved ref with no
// exact receipt from which to resume.
function prepareCandidate(options) {
  var rootDir = paths.PROJECT_ROOT;
  var executionRoot = options && options.executionRoot;
  var ref = options && options.candidateRef;
  var base = options && options.baseCommit;
  var baseTree = options && options.baseTree;
  var expectedRefCommit = options && options.expectedRefCommit;
  if (!managerRef(ref)) return fail('SEAL_REF_NOT_ALLOWED', 'candidate ref is outside the manager namespace');
  if (typeof executionRoot !== 'string' || !path.isAbsolute(executionRoot)) return fail('SEAL_PATH_INVALID', 'execution root must be absolute');
  if (typeof base !== 'string' || !worktreeContract.COMMIT_RE.test(base)) return fail('SEAL_BASE_INVALID', 'base commit must be a full id');
  if (typeof baseTree !== 'string' || !worktreeContract.COMMIT_RE.test(baseTree)) return fail('SEAL_BASE_INVALID', 'base tree must be a full id');
  if (typeof expectedRefCommit !== 'string' || !worktreeContract.COMMIT_RE.test(expectedRefCommit)) {
    return fail('SEAL_REF_GENERATION_INVALID', 'expected ref commit must be a full id');
  }

  // The branch must be at the exact generation the durable manager receipt
  // named. Identity strings are public and forgeable; they are never ownership
  // evidence for accepting a child-created commit.
  var current = refCommit(rootDir, ref);
  if (current === null) return fail('SEAL_REF_ABSENT', ref + ' does not exist');
  if (current !== expectedRefCommit) {
    return fail('SEAL_REF_MOVED', ref + ' moved off the exact recorded generation');
  }
  var head = runGit(['symbolic-ref', '-q', 'HEAD'], executionRoot);
  if (!head.ok || head.stdout.trim() !== ref) return fail('SEAL_HEAD_MISMATCH', 'the execution root is not on its candidate branch');

  if (!mutationDirectory()) return fail('MUTATION_RECEIPT_UNWRITABLE', 'the receipt directory is unavailable');
  var indexFile = path.join(RECEIPTS_DIR, 'seal-' + crypto.randomBytes(8).toString('hex') + '.index');
  var changed = pathsDifferingFromBase(executionRoot, baseTree, indexFile);
  function abort(code, message) {
    removeMutationFile(indexFile);
    return fail(code, message);
  }
  if (!changed.ok) {
    return abort(changed.code, changed.code === 'SEAL_CONFLICT_STAGES'
      ? 'the execution root has unmerged paths'
      : changed.code === 'SEAL_NESTED_REPOSITORY'
        ? 'the execution root contains a nested repository: ' + (changed.detail || '')
        : 'the candidate path set could not be computed');
  }

  // Deduplicate, then refuse anything control-owned or otherwise unfit BEFORE
  // a single byte is staged.
  var candidatePaths = [];
  var seenPaths = Object.create(null);
  changed.paths.forEach(function (candidate) {
    if (!candidate || seenPaths[candidate]) return;
    seenPaths[candidate] = true;
    candidatePaths.push(candidate);
  });
  if (!candidatePaths.length) return abort('SEAL_EMPTY_CANDIDATE', 'the execution root has no product changes to seal');
  // Provisioning itself installs the skill/contract copies the child needs
  // (.claude/skills, .claude/contracts). Those are MANAGER artifacts, not the
  // child writing where it must not: they are excluded, never a violation.
  candidatePaths = candidatePaths.filter(function (candidate) {
    return !MANAGER_INSTALLED_PREFIXES.some(function (prefix) { return candidate.indexOf(prefix) === 0; });
  });
  if (!candidatePaths.length) return abort('SEAL_EMPTY_CANDIDATE', 'the execution root has no product changes to seal');
  // Separate a control-owned violation (the child wrote where it must not)
  // from a path this pipeline cannot represent (control bytes, non-NFC).
  var controlOwned = candidatePaths.filter(candidateContract.controlOwnedPath);
  if (controlOwned.length) {
    return abort('SEAL_CONTROL_OWNED_PATH', 'candidate touches control-owned paths: ' + controlOwned.slice(0, 5).join(', '));
  }
  var ungrammatical = candidatePaths.filter(function (candidate) {
    return candidateContract.forbiddenProductPath(candidate);
  });
  if (ungrammatical.length) {
    return abort('SEAL_PATH_UNSUPPORTED', 'candidate contains paths this pipeline cannot represent: ' +
      ungrammatical.slice(0, 5).map(function (entry) { return JSON.stringify(entry); }).join(', '));
  }
  if (candidatePaths.length > candidateContract.MAX_ENTRIES) {
    return abort('SEAL_TOO_MANY_ENTRIES', candidatePaths.length + ' changed paths exceed the receipt bound');
  }
  // Ignored files need no gate (§9.5): discovery runs `ls-files --others
  // --exclude-standard` against a manager index seeded from the base tree, so
  // an ignored UNTRACKED path (including one the child force-added to its own
  // index) can never enter the candidate. A TRACKED file that also matches an
  // ignore rule stays a legitimate deliverable — git honours tracking over
  // ignoring, and refusing it would block real work.

  var intent = writeReceipt('seal-candidate', 'intent', { candidateRef: ref, baseCommit: base, paths: candidatePaths.length });
  if (!intent) return abort('MUTATION_RECEIPT_UNWRITABLE', 'intent receipt could not be published');

  var indexOptions = { indexFile: indexFile };
  var sealed = null;
  var hookVerdict = null;
  try {
    var staged = runGit(['update-index', '--add', '--remove', '-z', '--stdin'], executionRoot,
      Object.assign({ input: candidatePaths.join('\0') + '\0' }, indexOptions));
    if (!staged.ok) return fail('SEAL_STAGE_FAILED', staged.stderr || staged.code);

    // Hooks run BEFORE the tree is written, exactly as they would for a real
    // commit, so anything a hook stages is part of the candidate. --no-verify
    // does not exist in this pipeline.
    hookVerdict = runRepositoryPreCommitHook(executionRoot, indexFile);
    if (!hookVerdict.ok) {
      writeReceipt('seal-candidate', 'hook-failed', { candidateRef: ref, code: hookVerdict.code });
      return fail('SEAL_HOOK_FAILED', hookVerdict.message);
    }

    var written = runGit(['write-tree'], executionRoot, indexOptions);
    if (!written.ok) return fail('SEAL_TREE_FAILED', written.stderr || written.code);
    var candidateTree = written.stdout.trim();
    if (!worktreeContract.COMMIT_RE.test(candidateTree)) return fail('SEAL_TREE_FAILED', 'candidate tree id is not canonical');
    if (candidateTree === baseTree) return fail('SEAL_EMPTY_CANDIDATE', 'the candidate tree equals its base tree');

    var raw = runGit(['diff-tree', '--raw', '-r', '-z', '-M', '--no-commit-id', baseTree, candidateTree], executionRoot);
    if (!raw.ok) return fail('SEAL_DIFF_FAILED', raw.stderr || raw.code);
    var entries = parseRawDiff(raw.stdout);
    if (entries === null) return fail('SEAL_DIFF_FAILED', 'the raw diff could not be parsed canonically');
    if (entries.length > candidateContract.MAX_ENTRIES) {
      return fail('SEAL_TOO_MANY_ENTRIES', entries.length + ' entries exceed the receipt bound');
    }

    // Conflict markers are a hard refusal — they are never a deliverable.
    // Whitespace notes are reported, not fatal: blank-at-eof and trailing
    // whitespace are legitimate in Markdown and many generated files.
    var checked = runGit(['diff', '--check', baseTree, candidateTree], executionRoot);
    var checkOutput = String(checked.stdout || '') + String(checked.stderr || '');
    if (/leftover conflict marker/i.test(checkOutput)) {
      return fail('SEAL_CONFLICT_MARKERS', checkOutput.slice(0, 400));
    }
    sealed = { candidateTree: candidateTree, entries: entries,
      whitespaceNotes: checked.ok ? 0 : checkOutput.split('\n').filter(Boolean).length };
  } finally {
    removeMutationFile(indexFile);
  }

  var message = candidateMessage(options.stem);
  // One candidate commit per generation: the parent is always the sealed base,
  // so a re-seal REPLACES the previous candidate rather than chaining onto it.
  var committed = runGit(['commit-tree', sealed.candidateTree, '-p', base, '-m', message], executionRoot,
    { candidateIdentity: true });
  if (!committed.ok) return fail('SEAL_COMMIT_FAILED', committed.stderr || committed.code);
  var candidateCommit = committed.stdout.trim();
  if (!worktreeContract.COMMIT_RE.test(candidateCommit)) return fail('SEAL_COMMIT_FAILED', 'candidate commit id is not canonical');

  writeReceipt('seal-candidate', 'prepared', { candidateRef: ref, expectedRefCommit: current,
    candidateCommit: candidateCommit,
    candidateTree: sealed.candidateTree, entries: sealed.entries.length,
    hook: hookVerdict && hookVerdict.skipped ? 'absent' : 'passed',
    whitespaceNotes: sealed.whitespaceNotes || 0 });
  return { ok: true, expectedRefCommit: current,
    candidateCommit: candidateCommit, candidateTree: sealed.candidateTree,
    entries: sealed.entries, hookRan: !(hookVerdict && hookVerdict.skipped),
    whitespaceNotes: sealed.whitespaceNotes || 0 };
}

// Publish one already-receipted candidate by CAS. Idempotent replay accepts
// only the exact candidate commit; every third ref value is foreign state.
function publishCandidate(options) {
  var rootDir = paths.PROJECT_ROOT;
  var executionRoot = options && options.executionRoot;
  var ref = options && options.candidateRef;
  var expected = options && options.expectedRefCommit;
  var candidateCommit = options && options.candidateCommit;
  var candidateTree = options && options.candidateTree;
  if (!managerRef(ref)) return fail('SEAL_REF_NOT_ALLOWED', 'candidate ref is outside the manager namespace');
  if (typeof executionRoot !== 'string' || !path.isAbsolute(executionRoot)) return fail('SEAL_PATH_INVALID', 'execution root must be absolute');
  if (![expected, candidateCommit, candidateTree].every(function (value) {
    return typeof value === 'string' && worktreeContract.COMMIT_RE.test(value);
  })) return fail('SEAL_PUBLICATION_INVALID', 'candidate publication pins must be full ids');
  var current = refCommit(rootDir, ref);
  if (current !== candidateCommit) {
    if (current !== expected) return fail('SEAL_REF_MOVED', ref + ' moved off the receipted generation');
    var published = runGit(['update-ref', ref, candidateCommit, expected], rootDir);
    if (!published.ok) {
      writeReceipt('seal-candidate', 'publish-failed', { candidateRef: ref,
        expectedRefCommit: expected, candidateCommit: candidateCommit });
      return fail('SEAL_PUBLISH_FAILED', published.stderr || published.code);
    }
  }
  if (refCommit(rootDir, ref) !== candidateCommit) {
    return fail('MUTATION_POSTCONDITION_FAILED', 'the candidate ref does not carry the receipted commit');
  }
  var synced = runGit(['read-tree', candidateTree], executionRoot);
  if (!synced.ok) return fail('SEAL_INDEX_SYNC_FAILED', synced.stderr || synced.code);
  writeReceipt('seal-candidate', 'done', { candidateRef: ref, expectedRefCommit: expected,
    candidateCommit: candidateCommit, candidateTree: candidateTree });
  return { ok: true, candidateCommit: candidateCommit, candidateTree: candidateTree };
}

// Re-prove that a receipt names real immutable Git bytes. Self-hashes protect
// the JSON from accidental edits; these checks bind those fields to the commit,
// tree, parent, fixed message/identity and canonical base→candidate manifest.
function verifyCandidateReceipt(receipt) {
  try { candidateContract.validate(receipt); } catch (error) { return false; }
  var baseTree = runGit(['rev-parse', '-q', '--verify', receipt.baseCommit + '^{tree}'], paths.PROJECT_ROOT);
  if (!baseTree.ok || baseTree.stdout.trim() !== receipt.baseTree) return false;
  var probe = runGit(['cat-file', 'commit', receipt.candidateCommit], paths.PROJECT_ROOT);
  if (!probe.ok) return false;
  var rawCommit = String(probe.stdout || '');
  var split = rawCommit.indexOf('\n\n');
  if (split < 0 || rawCommit.slice(split + 2) !== candidateMessage(receipt.stem)) return false;
  var header = rawCommit.slice(0, split).split('\n');
  var trees = header.filter(function (line) { return line.indexOf('tree ') === 0; });
  var parents = header.filter(function (line) { return line.indexOf('parent ') === 0; });
  if (trees.length !== 1 || trees[0] !== 'tree ' + receipt.candidateTree ||
      parents.length !== 1 || parents[0] !== 'parent ' + receipt.baseCommit) return false;
  var signature = 'Orchestrator Candidate <orchestrator@local.invalid>';
  var author = header.find(function (line) { return line.indexOf('author ') === 0; }) || '';
  var committer = header.find(function (line) { return line.indexOf('committer ') === 0; }) || '';
  if (author.indexOf('author ' + signature + ' ') !== 0 ||
      committer.indexOf('committer ' + signature + ' ') !== 0) return false;
  var raw = runGit(['diff-tree', '--raw', '-r', '-z', '-M', '--no-commit-id',
    receipt.baseTree, receipt.candidateTree], paths.PROJECT_ROOT);
  if (!raw.ok) return false;
  var entries = parseRawDiff(raw.stdout);
  return entries !== null && JSON.stringify(entries) === JSON.stringify(receipt.entries) &&
    candidateContract.diffHashOf(entries) === receipt.diffHash;
}

// The repository's own pre-commit hook, executed against the sealed index.
// Absent hook wiring is not a silent pass: the caller decides, and the
// enforcement net (Phase 0) already refuses runs for unwired visual products.
function runRepositoryPreCommitHook(executionRoot, indexFile) {
  // The hook script comes from the CONTROL root: the checkout is
  // child-writable, so reading the gate from there would let the child
  // install or neuter its own gate and have the manager execute it.
  var hook = path.join(paths.PROJECT_ROOT, 'orchestrator', 'skills', 'checks', 'hooks', 'pre-commit');
  try { fs.lstatSync(hook); } catch (error) { return { ok: true, skipped: true }; }
  var env = {};
  Object.keys(process.env).forEach(function (key) {
    if (key.indexOf('GIT_') !== 0) env[key] = process.env[key];
  });
  env.GIT_INDEX_FILE = indexFile;
  env.LC_ALL = 'C';
  var result;
  try {
    result = cp.spawnSync('sh', [hook], { cwd: executionRoot, env: env,
      timeout: GIT_TIMEOUT_MS, maxBuffer: GIT_MAX_BUFFER, encoding: 'utf8' });
  } catch (error) {
    return { ok: false, code: 'HOOK_SPAWN_FAILED', message: String(error && error.message || '') };
  }
  if (result.error || result.signal || typeof result.status !== 'number') {
    return { ok: false, code: 'HOOK_UNAVAILABLE', message: 'the pre-commit hook did not complete' };
  }
  if (result.status !== 0) {
    return { ok: false, code: 'HOOK_REFUSED', message: String(result.stderr || result.stdout || '').slice(0, 400) };
  }
  return { ok: true };
}

// Read-only tree computation for the execution pin. It lives here because
// this module is the single owner of git invocations that write objects, and
// it uses a throwaway index so the child's own index can never influence the
// result. No ref moves and no worktree state changes.
function executionTreeOf(executionRoot, baseTree) {
  if (typeof executionRoot !== 'string' || !path.isAbsolute(executionRoot)) return null;
  if (typeof baseTree !== 'string' || !worktreeContract.COMMIT_RE.test(baseTree)) return null;
  if (!mutationDirectory()) return null;
  var indexFile = path.join(RECEIPTS_DIR, 'pin-' + crypto.randomBytes(8).toString('hex') + '.index');
  var options = { indexFile: indexFile };
  try {
    if (!runGit(['read-tree', baseTree], executionRoot, options).ok) return null;
    var changed = pathsDifferingFromBase(executionRoot, baseTree, indexFile);
    if (!changed.ok) return null;
    // Provisioning materializes manager-owned skill/contract copies inside the
    // checkout. Candidate sealing excludes them, so the execution pin must use
    // the SAME tree domain; otherwise every real gate is pinned to a tree that
    // can never equal the candidate receipt it supposedly proves.
    var pinPaths = changed.paths.filter(function (candidate) {
      return !MANAGER_INSTALLED_PREFIXES.some(function (prefix) {
        return candidate.indexOf(prefix) === 0;
      });
    });
    if (pinPaths.length) {
      var staged = runGit(['update-index', '--add', '--remove', '-z', '--stdin'], executionRoot,
        Object.assign({ input: pinPaths.join('\0') + '\0' }, options));
      if (!staged.ok) return null;
    }
    var written = runGit(['write-tree'], executionRoot, options);
    if (!written.ok) return null;
    var tree = written.stdout.trim();
    return worktreeContract.COMMIT_RE.test(tree) ? tree : null;
  } finally {
    removeMutationFile(indexFile);
  }
}

// ---------------------------------------------------------------------------
// §10 integration transaction. Three mutations, each driven by the integration
// WAL and each re-provable from physical state after a crash:
//   applyCandidate         — apply the exact candidate diff into the CONTROL
//                            index+worktree (phase `product-applying`)
//   stageTransactionPaths  — stage the finalizer's own tracked artifacts
//   commitIntegration      — ONE canonical commit on the exact target base,
//                            with the user's configured identity, all hooks
//                            run, published by compare-and-swap
// Nothing here ever resets, amends, forces or stashes: a postcondition that
// does not match its pin leaves the transaction recoverable, never "fixed".
// ---------------------------------------------------------------------------

// The tree the CONTROL index currently describes. Comparing it to a pin is how
// every integration phase proves its physical postcondition without a single
// set difference.
function controlIndexTree() {
  var written = runGit(['write-tree'], paths.PROJECT_ROOT);
  if (!written.ok) return null;
  var tree = written.stdout.trim();
  return worktreeContract.COMMIT_RE.test(tree) ? tree : null;
}

// Canonical raw diff between two trees, as receipt entries. Used both to build
// the transaction path set and to re-verify the published commit.
//
// Deliberately --no-renames: rename detection collapses a move into ONE entry
// whose `path` is the destination and whose source survives only as
// `renameFrom`. The transaction accounts for paths, and a `todo/X.md ->
// done/X.md` publication would then drop the deleted path out of the commit's
// path set entirely. Rename detection belongs to the candidate receipt, which
// is an audit artifact; here every change is a plain add/delete/modify.
function treeEntries(fromTree, toTree) {
  if (!worktreeContract.COMMIT_RE.test(String(fromTree || '')) ||
      !worktreeContract.COMMIT_RE.test(String(toTree || ''))) return null;
  var raw = runGit(['diff-tree', '--raw', '-r', '-z', '--no-renames', '--no-commit-id', fromTree, toTree], paths.PROJECT_ROOT);
  if (!raw.ok) return null;
  return parseRawDiff(raw.stdout);
}

// Every control-root path whose working tree differs from `tree`, discovered
// through a MANAGER-OWNED index seeded from that tree — never through the
// control index, whose assume-unchanged/skip-worktree bits would hide a real
// artifact from the transaction exactly as they would hide one from a candidate.
function controlPathsDifferingFrom(tree) {
  if (!worktreeContract.COMMIT_RE.test(String(tree || ''))) return { ok: false, code: 'SEAL_BASE_INVALID' };
  if (!mutationDirectory()) {
    return { ok: false, code: 'MUTATION_RECEIPT_UNWRITABLE' };
  }
  var indexFile = path.join(RECEIPTS_DIR, 'integ-' + crypto.randomBytes(8).toString('hex') + '.index');
  try {
    return pathsDifferingFromBase(paths.PROJECT_ROOT, tree, indexFile);
  } finally {
    removeMutationFile(indexFile);
  }
}

// §10.3 phase 2→3. The candidate diff is produced as BYTES by git itself and
// applied by git itself; no shell pipeline, no patch heuristics, no 3-way
// fallback that could invent a resolution. The postcondition is exact: the
// control index must afterwards describe precisely the candidate tree.
function applyCandidate(options) {
  var baseTree = options && options.baseTree;
  var candidateTree = options && options.candidateTree;
  if (!worktreeContract.COMMIT_RE.test(String(baseTree || '')) ||
      !worktreeContract.COMMIT_RE.test(String(candidateTree || ''))) {
    return fail('INTEGRATION_TREE_INVALID', 'base and candidate trees must be full ids');
  }
  if (baseTree === candidateTree) return fail('INTEGRATION_CANDIDATE_EMPTY', 'the candidate tree equals its base tree');
  var before = controlIndexTree();
  if (before === null) return fail('INTEGRATION_INDEX_UNREADABLE', 'the control index could not be written to a tree');
  // Idempotent resume: an apply that already landed is proven, not redone.
  if (before === candidateTree) return { ok: true, alreadyApplied: true, indexTree: candidateTree };
  if (before !== baseTree) {
    return fail('INTEGRATION_INDEX_NOT_AT_BASE', 'the control index does not describe the recorded base tree');
  }
  if (!mutationDirectory()) {
    return fail('MUTATION_RECEIPT_UNWRITABLE', 'the receipt directory is unavailable');
  }
  // The patch goes to a file, never through a pipe: a large binary candidate
  // must not depend on a stdout buffer bound.
  var patchFile = path.join(RECEIPTS_DIR, 'patch-' + crypto.randomBytes(8).toString('hex') + '.diff');
  try {
    // --no-renames keeps the patch to plain add/delete/modify hunks: rename
    // detection is a display concern of the receipt, never an apply semantic.
    var produced = runGit(['diff', '--binary', '--no-renames', '--no-color', '--no-ext-diff',
      '--output', patchFile, baseTree, candidateTree], paths.PROJECT_ROOT);
    if (!produced.ok) return fail('INTEGRATION_DIFF_FAILED', produced.stderr || produced.code);
    var patchStat = fileGuards.statRegularFileUnder(
      paths.WORKTREE_RECORDS_AUTHORITY_ROOT, RECEIPTS_DIR, patchFile);
    if (!patchStat || patchStat.nlinkNumber !== 1) {
      return fail('INTEGRATION_DIFF_FAILED', 'the candidate patch was not produced');
    }
    var size = patchStat.size;
    if (!size) return fail('INTEGRATION_CANDIDATE_EMPTY', 'the candidate patch is empty');
    var checked = runGit(['apply', '--check', '--index', '--binary', '--whitespace=nowarn', patchFile], paths.PROJECT_ROOT);
    if (!checked.ok) {
      writeReceipt('apply-candidate', 'check-failed', { baseTree: baseTree, candidateTree: candidateTree,
        stderr: String(checked.stderr).slice(0, 400) });
      return fail('INTEGRATION_APPLY_REFUSED', checked.stderr || checked.code);
    }
    var intent = writeReceipt('apply-candidate', 'intent', { baseTree: baseTree, candidateTree: candidateTree, bytes: size });
    if (!intent) return fail('MUTATION_RECEIPT_UNWRITABLE', 'intent receipt could not be published');
    var applied = runGit(['apply', '--index', '--binary', '--whitespace=nowarn', patchFile], paths.PROJECT_ROOT);
    if (!applied.ok) {
      writeReceipt('apply-candidate', 'failed', { baseTree: baseTree, candidateTree: candidateTree,
        stderr: String(applied.stderr).slice(0, 400) });
      return fail('INTEGRATION_APPLY_FAILED', applied.stderr || applied.code);
    }
  } finally {
    removeMutationFile(patchFile);
  }
  var after = controlIndexTree();
  if (after !== candidateTree) {
    writeReceipt('apply-candidate', 'postcondition-failed', { baseTree: baseTree,
      candidateTree: candidateTree, observed: after });
    return fail('MUTATION_POSTCONDITION_FAILED', 'the control index does not describe the candidate tree after apply');
  }
  writeReceipt('apply-candidate', 'done', { baseTree: baseTree, candidateTree: candidateTree });
  return { ok: true, alreadyApplied: false, indexTree: after };
}

// Stage the finalizer's own tracked artifacts into the CONTROL index, NUL-safely
// and by exact path. `--add --remove` covers a created done file and the removed
// todo file in one pass; a path the transaction does not own can never enter,
// because the caller derived this list from the physical tree difference.
function stageTransactionPaths(options) {
  var list = options && options.paths;
  if (!Array.isArray(list) || !list.length) return fail('INTEGRATION_PATHS_INVALID', 'no transaction paths to stage');
  // Grammar only: this stages BOTH halves of the transaction, and the
  // finalizer's half is control-owned by definition. Ownership is asserted by
  // the caller that owns each path set.
  var unfit = list.filter(function (entry) { return candidateContract.unrepresentablePath(entry); });
  if (unfit.length) return fail('INTEGRATION_PATHS_INVALID', 'transaction paths are not representable');
  var staged = runGit(['update-index', '--add', '--remove', '-z', '--stdin'], paths.PROJECT_ROOT,
    { input: list.join('\0') + '\0' });
  if (!staged.ok) return fail('INTEGRATION_STAGE_FAILED', staged.stderr || staged.code);
  var tree = controlIndexTree();
  if (tree === null) return fail('INTEGRATION_INDEX_UNREADABLE', 'the staged tree could not be written');
  return { ok: true, indexTree: tree };
}

// The user's OWN configured Git identity (§10.5). Absent identity blocks
// Integrate; nothing here invents a name, an email or a fallback.
function configuredIdentity() {
  var name = runGit(['config', '--get', 'user.name'], paths.PROJECT_ROOT);
  var email = runGit(['config', '--get', 'user.email'], paths.PROJECT_ROOT);
  if (!name.ok || !email.ok) return null;
  var identity = { name: name.stdout.trim(), email: email.stdout.trim() };
  if (!identity.name || !identity.email) return null;
  return identity;
}

// Run one hook with the same sanitized environment and working directory as a
// real control-root commit. Only the shipped pre-commit enforcement net is
// mandatory when hooksPath is wired; every other Git hook is optional by Git's
// own contract and an absent file is not an error.
function runControlHook(name, args, mandatoryWhenWired) {
  var located = runGit(['rev-parse', '--git-path', 'hooks'], paths.PROJECT_ROOT);
  if (!located.ok) return { ok: false, code: 'HOOK_UNAVAILABLE', message: 'the hooks directory could not be resolved' };
  var hooksDir = path.resolve(paths.PROJECT_ROOT, located.stdout.trim());
  var hook = path.join(hooksDir, name);
  var runnable = true;
  try {
    runnable = fs.lstatSync(hook).isFile();
    if (runnable && process.platform !== 'win32') fs.accessSync(hook, fs.constants.X_OK);
  } catch (error) { runnable = false; }
  if (!runnable) {
    if (mandatoryWhenWired && require('./git').enforcementWiring().wired) {
      return { ok: false, code: 'HOOK_MISSING', hook: name,
        message: name + ' is missing or not executable in the wired hooks directory' };
    }
    // Git ignores a non-executable optional hook. Treat it exactly like an
    // absent hook instead of trying to spawn it and turning a disabled hook
    // into a publication blocker.
    return { ok: true, skipped: true, hook: name };
  }
  var env = {};
  Object.keys(process.env).forEach(function (key) {
    if (key.indexOf('GIT_') !== 0) env[key] = process.env[key];
  });
  env.LC_ALL = 'C';
  var result;
  try {
    // Execute the hook itself so its shebang is authoritative, exactly as Git
    // does. Forcing every hook through `sh` silently breaks valid Node, Python
    // and other executable hooks.
    result = cp.spawnSync(hook, args || [], { cwd: paths.PROJECT_ROOT, env: env,
      timeout: GIT_TIMEOUT_MS, maxBuffer: GIT_MAX_BUFFER, encoding: 'utf8' });
  } catch (error) {
    return { ok: false, code: 'HOOK_SPAWN_FAILED', hook: name,
      message: String(error && error.message || '') };
  }
  if (result.error || result.signal || typeof result.status !== 'number') {
    return { ok: false, code: 'HOOK_UNAVAILABLE', hook: name, message: name + ' did not complete' };
  }
  if (result.status !== 0) {
    return { ok: false, code: 'HOOK_REFUSED', hook: name,
      message: String(result.stderr || result.stdout || '').slice(0, 600) };
  }
  return { ok: true, skipped: false, hook: name };
}

// Pre-publication hooks in Git's commit order. `prepare-commit-msg` sees the
// source `message` because the canonical message is supplied explicitly, and
// commit-msg sees the bytes left by that hook. `--no-verify` never exists.
function runControlHooks(messageFile) {
  var stages = [
    { name: 'pre-commit', args: [], mandatoryWhenWired: true },
    { name: 'prepare-commit-msg', args: [messageFile, 'message'], mandatoryWhenWired: false },
    { name: 'commit-msg', args: [messageFile], mandatoryWhenWired: false }
  ];
  var ran = [];
  for (var i = 0; i < stages.length; i++) {
    var verdict = runControlHook(stages[i].name, stages[i].args, stages[i].mandatoryWhenWired);
    if (!verdict.ok) return verdict;
    if (!verdict.skipped) ran.push(stages[i].name);
  }
  return { ok: true, hooks: ran };
}

// §10.5 step one: run every hook against the CONTROL index, then pin the tree
// the commit will carry and prove it holds nothing outside the transaction.
// Separated from publication because the WAL must record the exact tree, the
// exact message and the exact parent BEFORE the ref moves (§10.3 phase 6) —
// and because re-running it after a crash is idempotent: the same content
// yields the same tree, hence the same pin.
function prepareIntegrationCommit(options) {
  var rootDir = paths.PROJECT_ROOT;
  var targetRef = options && options.targetRef;
  var expectedParent = options && options.expectedParent;
  var message = options && options.message;
  var expectedPaths = options && options.expectedPaths;
  if (!worktreeContract.targetRefValid(targetRef)) {
    return fail('INTEGRATION_TARGET_INVALID', 'the target ref is not a plain local branch');
  }
  if (typeof expectedParent !== 'string' || !worktreeContract.COMMIT_RE.test(expectedParent)) {
    return fail('INTEGRATION_PARENT_INVALID', 'the expected parent must be a full commit id');
  }
  if (typeof message !== 'string' || !message || Buffer.byteLength(message, 'utf8') > 16 * 1024) {
    return fail('INTEGRATION_MESSAGE_INVALID', 'the commit message is empty or oversized');
  }
  if (!Array.isArray(expectedPaths) || !expectedPaths.length) {
    return fail('INTEGRATION_PATHS_INVALID', 'the transaction path set is empty');
  }
  var identity = configuredIdentity();
  if (!identity) {
    return fail('INTEGRATION_IDENTITY_MISSING',
      'git user.name and user.email must be configured; the pipeline never invents an identity');
  }
  // The control root must be ON the target branch: committing to a ref the
  // checkout is not following would leave the working tree describing a
  // different commit than HEAD.
  var head = runGit(['symbolic-ref', '-q', 'HEAD'], rootDir);
  if (!head.ok || head.stdout.trim() !== targetRef) {
    return fail('INTEGRATION_TARGET_NOT_CHECKED_OUT', 'the control root is not on the target branch');
  }
  if (refCommit(rootDir, targetRef) !== expectedParent) {
    return fail('INTEGRATION_TARGET_MOVED', targetRef + ' no longer points at the recorded base commit');
  }

  if (!mutationDirectory()) {
    return fail('MUTATION_RECEIPT_UNWRITABLE', 'the receipt directory is unavailable');
  }
  var messageFile = path.join(RECEIPTS_DIR, 'msg-' + crypto.randomBytes(8).toString('hex') + '.txt');
  var hookVerdict;
  var finalMessage = message;
  try {
    var stagedMessage = fileGuards.publishNoClobberRegularFileUnder(
      paths.WORKTREE_RECORDS_AUTHORITY_ROOT, RECEIPTS_DIR, messageFile,
      Buffer.from(message, 'utf8'),
      { create: true, directoryMode: 0o700, mode: 0o600, maxBytes: 16 * 1024 });
    if (!stagedMessage || !stagedMessage.ok) {
      return fail('MUTATION_RECEIPT_UNWRITABLE', 'the commit message could not be staged');
    }
    // Hooks run BEFORE the tree is pinned, exactly as they would for a real
    // commit, so anything a hook legitimately re-stages is part of the tree
    // that gets committed — and anything it adds outside the transaction is
    // caught by the path-set check below rather than silently shipped.
    hookVerdict = runControlHooks(messageFile);
    if (!hookVerdict.ok) {
      writeReceipt('commit-integration', 'hook-failed', { targetRef: targetRef, code: hookVerdict.code,
        hook: hookVerdict.hook || null });
      return fail('INTEGRATION_HOOK_REFUSED', (hookVerdict.hook ? hookVerdict.hook + ': ' : '') + hookVerdict.message);
    }
    // A commit-msg hook is allowed to REWRITE the message in place; git commits
    // what the hook left behind, so this owner reads it back rather than
    // committing the text the hook was asked to review.
    var finalMessageFile = fileGuards.boundedRegularFileUnder(
      paths.WORKTREE_RECORDS_AUTHORITY_ROOT, RECEIPTS_DIR, messageFile, 16 * 1024);
    if (!finalMessageFile || finalMessageFile.stat.nlinkNumber !== 1) {
      return fail('INTEGRATION_MESSAGE_INVALID', 'the commit message could not be read back');
    }
    finalMessage = finalMessageFile.bytes.toString('utf8');
    if (!finalMessage || Buffer.byteLength(finalMessage, 'utf8') > 16 * 1024) {
      return fail('INTEGRATION_MESSAGE_INVALID', 'the hook left an empty or oversized commit message');
    }
  } finally {
    removeMutationFile(messageFile);
  }

  var stagedTree = controlIndexTree();
  if (stagedTree === null) return fail('INTEGRATION_INDEX_UNREADABLE', 'the staged tree could not be written');
  var verdict = verifyStagedPathSet(expectedParent, stagedTree, expectedPaths);
  if (!verdict.ok) return verdict;
  return { ok: true, stagedTree: stagedTree, identity: identity, hooks: hookVerdict.hooks,
    message: finalMessage, entries: verdict.entries };
}

// The exact message body a commit carries, normalized the way git stores it
// (trailing newlines are not significant). Used to prove that a commit found
// on the target branch is the one this transaction pinned — parent and tree
// alone cannot tell it apart from someone else's commit of the same index.
function commitMessageBody(commit) {
  var probe = runGit(['cat-file', 'commit', commit], paths.PROJECT_ROOT);
  if (!probe.ok) return null;
  var text = String(probe.stdout || '');
  var split = text.indexOf('\n\n');
  return split < 0 ? '' : text.slice(split + 2);
}
function normalizedMessage(value) { return String(value == null ? '' : value).replace(/\n+$/, ''); }

// The staged tree may differ from the parent ONLY in the transaction's own
// paths — a hook or an external actor adding anything else is a refusal, not a
// silently larger commit.
function verifyStagedPathSet(expectedParent, stagedTree, expectedPaths) {
  var parentTree = runGit(['rev-parse', expectedParent + '^{tree}'], paths.PROJECT_ROOT);
  if (!parentTree.ok) return fail('INTEGRATION_PARENT_INVALID', 'the parent tree could not be resolved');
  var entries = treeEntries(parentTree.stdout.trim(), stagedTree);
  if (entries === null) return fail('INTEGRATION_DIFF_FAILED', 'the staged diff could not be parsed canonically');
  var expected = expectedPaths.slice().sort();
  var actual = entries.map(function (entry) { return entry.path; }).sort();
  var foreign = actual.filter(function (entry) { return expected.indexOf(entry) < 0; });
  if (foreign.length) {
    writeReceipt('commit-integration', 'foreign-paths', { foreign: foreign.slice(0, 20) });
    return fail('INTEGRATION_FOREIGN_PATHS', 'the staged tree carries paths outside the transaction: ' +
      foreign.slice(0, 5).join(', '));
  }
  var missing = expected.filter(function (entry) { return actual.indexOf(entry) < 0; });
  if (missing.length) {
    return fail('INTEGRATION_PATHS_MISSING', 'the staged tree is missing transaction paths: ' +
      missing.slice(0, 5).join(', '));
  }
  return { ok: true, entries: entries };
}

// §10.5 step two: publish ONE canonical commit carrying the exact pinned tree
// on the exact pinned parent, by compare-and-swap, and re-prove it afterwards.
// Divergence is reported as-is; this owner never resets or amends.
function commitIntegration(options) {
  var rootDir = paths.PROJECT_ROOT;
  var targetRef = options && options.targetRef;
  var expectedParent = options && options.expectedParent;
  var message = options && options.message;
  var expectedPaths = options && options.expectedPaths;
  var stagedTree = options && options.stagedTree;
  if (!worktreeContract.targetRefValid(targetRef)) {
    return fail('INTEGRATION_TARGET_INVALID', 'the target ref is not a plain local branch');
  }
  if (typeof expectedParent !== 'string' || !worktreeContract.COMMIT_RE.test(expectedParent)) {
    return fail('INTEGRATION_PARENT_INVALID', 'the expected parent must be a full commit id');
  }
  if (typeof stagedTree !== 'string' || !worktreeContract.COMMIT_RE.test(stagedTree)) {
    return fail('INTEGRATION_TREE_INVALID', 'the pinned staged tree must be a full tree id');
  }
  if (typeof message !== 'string' || !message || Buffer.byteLength(message, 'utf8') > 16 * 1024) {
    return fail('INTEGRATION_MESSAGE_INVALID', 'the commit message is empty or oversized');
  }
  if (!Array.isArray(expectedPaths) || !expectedPaths.length) {
    return fail('INTEGRATION_PATHS_INVALID', 'the transaction path set is empty');
  }
  var identity = configuredIdentity();
  if (!identity) {
    return fail('INTEGRATION_IDENTITY_MISSING',
      'git user.name and user.email must be configured; the pipeline never invents an identity');
  }
  var head = runGit(['symbolic-ref', '-q', 'HEAD'], rootDir);
  if (!head.ok || head.stdout.trim() !== targetRef) {
    return fail('INTEGRATION_TARGET_NOT_CHECKED_OUT', 'the control root is not on the target branch');
  }
  if (refCommit(rootDir, targetRef) !== expectedParent) {
    return fail('INTEGRATION_TARGET_MOVED', targetRef + ' no longer points at the recorded base commit');
  }
  // The index must still describe EXACTLY the tree the WAL pinned: anything
  // that touched it between the pin and here invalidates the transaction.
  if (controlIndexTree() !== stagedTree) {
    return fail('INTEGRATION_TREE_DRIFT', 'the control index no longer describes the pinned staged tree');
  }
  var pathVerdict = verifyStagedPathSet(expectedParent, stagedTree, expectedPaths);
  if (!pathVerdict.ok) return pathVerdict;

  var intent = writeReceipt('commit-integration', 'intent', { targetRef: targetRef,
    expectedParent: expectedParent, stagedTree: stagedTree, paths: expectedPaths.length });
  if (!intent) return fail('MUTATION_RECEIPT_UNWRITABLE', 'intent receipt could not be published');

  // No GIT_AUTHOR_*/GIT_COMMITTER_* overrides: the environment is stripped of
  // GIT_* and git reads the repository's own configured identity — the one the
  // owner already uses for every other commit in this repository.
  var committed = runGit(['commit-tree', stagedTree, '-p', expectedParent, '-m', message], rootDir);
  if (!committed.ok) return fail('INTEGRATION_COMMIT_FAILED', committed.stderr || committed.code);
  var commit = committed.stdout.trim();
  if (!worktreeContract.COMMIT_RE.test(commit)) return fail('INTEGRATION_COMMIT_FAILED', 'the commit id is not canonical');

  var published = runGit(['update-ref', targetRef, commit, expectedParent], rootDir);
  if (!published.ok) {
    writeReceipt('commit-integration', 'publish-failed', { targetRef: targetRef, commit: commit,
      stderr: String(published.stderr).slice(0, 400) });
    return fail('INTEGRATION_PUBLISH_FAILED', published.stderr || published.code);
  }
  var verdict = verifyPublishedCommit({ targetRef: targetRef, commit: commit,
    expectedParent: expectedParent, stagedTree: stagedTree, expectedPaths: expectedPaths });
  if (!verdict.ok) {
    writeReceipt('commit-integration', 'postcondition-failed', { targetRef: targetRef, commit: commit,
      code: verdict.code });
    return verdict;
  }
  // Git runs post-commit only after the commit is published and ignores its
  // exit status. Do the same: execute it, retain a receipt if it failed, and
  // let the integration driver's fresh postcondition checks classify any
  // repository mutation the hook performed. A hook failure cannot unpublish
  // the already-CASed canonical commit.
  var postCommit = runControlHook('post-commit', [], false);
  if (!postCommit.ok) {
    writeReceipt('commit-integration', 'post-hook-failed', { targetRef: targetRef, commit: commit,
      hook: postCommit.hook || 'post-commit', code: postCommit.code });
  }
  writeReceipt('commit-integration', 'done', { targetRef: targetRef, commit: commit,
    stagedTree: stagedTree, author: identity.email,
    postCommitHook: postCommit.ok && !postCommit.skipped ? 'ran' : (postCommit.skipped ? 'absent' : 'failed') });
  return { ok: true, commit: commit, stagedTree: stagedTree, identity: identity,
    postCommitHook: postCommit };
}

// Re-prove a published canonical commit from physical state alone. Called
// immediately after publication AND again on every resume: a WAL that claims a
// commit must be able to point at one that still matches it exactly.
function verifyPublishedCommit(options) {
  var rootDir = paths.PROJECT_ROOT;
  var targetRef = options && options.targetRef;
  var commit = options && options.commit;
  var expectedParent = options && options.expectedParent;
  var stagedTree = options && options.stagedTree;
  var expectedPaths = Array.isArray(options && options.expectedPaths) ? options.expectedPaths.slice().sort() : null;
  if (!worktreeContract.targetRefValid(targetRef) || !worktreeContract.COMMIT_RE.test(String(commit || ''))) {
    return fail('INTEGRATION_TARGET_INVALID', 'the published commit reference is not canonical');
  }
  if (refCommit(rootDir, targetRef) !== commit) {
    return fail('INTEGRATION_COMMIT_NOT_PUBLISHED', targetRef + ' does not carry the recorded commit');
  }
  var probe = runGit(['cat-file', 'commit', commit], rootDir);
  if (!probe.ok) return fail('INTEGRATION_COMMIT_UNREADABLE', 'the published commit could not be read');
  var header = String(probe.stdout || '').split('\n\n')[0].split('\n');
  var parents = header.filter(function (line) { return line.indexOf('parent ') === 0; });
  if (parents.length !== 1 || parents[0] !== 'parent ' + expectedParent) {
    return fail('INTEGRATION_PARENT_MISMATCH', 'the published commit does not sit on the recorded base');
  }
  var treeLine = header.find(function (line) { return line.indexOf('tree ') === 0; }) || '';
  if (treeLine !== 'tree ' + stagedTree) {
    return fail('INTEGRATION_TREE_MISMATCH', 'the published commit does not carry the recorded tree');
  }
  if (expectedPaths) {
    var parentTree = runGit(['rev-parse', expectedParent + '^{tree}'], rootDir);
    if (!parentTree.ok) return fail('INTEGRATION_PARENT_INVALID', 'the parent tree could not be resolved');
    var entries = treeEntries(parentTree.stdout.trim(), stagedTree);
    if (entries === null) return fail('INTEGRATION_DIFF_FAILED', 'the published diff could not be parsed canonically');
    var actual = entries.map(function (entry) { return entry.path; }).sort();
    if (actual.length !== expectedPaths.length ||
        actual.some(function (entry, index) { return entry !== expectedPaths[index]; })) {
      return fail('INTEGRATION_PATH_SET_MISMATCH', 'the published commit does not carry the recorded path set');
    }
  }
  return { ok: true, commit: commit, stagedTree: stagedTree };
}

module.exports = {
  RECEIPTS_DIR: RECEIPTS_DIR,
  verifyCandidateReceipt: verifyCandidateReceipt,
  executionTreeOf: executionTreeOf,
  controlIndexTree: controlIndexTree,
  controlPathsDifferingFrom: controlPathsDifferingFrom,
  treeEntries: treeEntries,
  configuredIdentity: configuredIdentity,
  commitMessageBody: commitMessageBody,
  normalizedMessage: normalizedMessage,
  applyCandidate: applyCandidate,
  stageTransactionPaths: stageTransactionPaths,
  prepareIntegrationCommit: prepareIntegrationCommit,
  commitIntegration: commitIntegration,
  verifyPublishedCommit: verifyPublishedCommit,
  prepareCandidate: prepareCandidate,
  publishCandidate: publishCandidate,
  addWorktree: addWorktree,
  removeOwnedWorktree: removeOwnedWorktree,
  releaseOwnedRef: releaseOwnedRef,
  releaseMarkerRefFor: releaseMarkerRef
};
