'use strict';

// ---------------------------------------------------------------------------
// Per-task worktree manager (pipeline improvement 01, §19.1/§20). This module
// owns repository identity, environment prechecks, worktree inventory
// classification (all read-only) AND the §9.2 execution-generation lifecycle:
// provision / resume / release. Every Git mutation it needs is delegated to
// the single mutation owner (server/git-mutations.js) — this file runs no
// mutating git command itself, never prunes, never forces, and never removes
// anything it cannot prove it owns. Discovery stays strictly read-only:
// everything unprovable is reported as 'unsafe' or a typed blocker — never
// guessed and never "repaired".
//
// Classification doctrine (plan §5/§7/§18):
//   - a checkout is MANAGED only when an exact active record in
//     orchestrator/.cache/tasks/worktrees/ BINDS to it completely: filesystem
//     identity (path+dev+ino), repository identity (controlProjectId and the
//     git common dir), a live materialized lifecycle status, the checkout
//     sitting on the record's own candidate branch, and a .git pointer that
//     still resolves into the recorded repository. A record that matches by
//     inode but fails any binding makes the checkout 'unsafe' — inode reuse
//     and copied record stores must never look owned;
//   - everything else — including prunable or broken entries — is FOREIGN and
//     untouchable (the developer's own linked worktrees live here);
//   - an unreadable/invalid record store is a BLOCKER: when the ownership
//     authority cannot be read, nothing may be presumed foreign-and-free;
//   - `git.js` stays the site's read-only observer for the control root; this
//     module is the only consumer of worktree-wide git state.
// This module also SEALS a candidate (§9.5): it freezes the checkout's work
// into the candidate ref and publishes the receipt the integration transaction
// consumes. The transaction itself lives in server/integrations.js, and every
// git mutation either module needs goes through server/git-mutations.js.
// ---------------------------------------------------------------------------

var cp = require('child_process');
var crypto = require('crypto');
var fs = require('fs');
var path = require('path');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var taskIntegrity = require('./task-integrity');
var worktreeContract = require('../../tasks/worktree-record-contract.cjs');
var integrationContract = require('../../tasks/integration-record-contract.cjs');
var writerLeases = require('../../tasks/writer-leases.cjs');
// Lazily required: integrations requires this module back, so a top-level
// require would close the cycle at load time.
var integrationsCache = null;
function integrationsModule() {
  if (!integrationsCache) integrationsCache = require('./integrations');
  return integrationsCache;
}
var taskCheckpointsCache = null;
function taskCheckpointsModule() {
  // task-checkpoints reads execution pins through this module, so keep the
  // reverse edge lazy and invoke it only after both modules finished loading.
  if (!taskCheckpointsCache) taskCheckpointsCache = require('./task-checkpoints');
  return taskCheckpointsCache;
}

var GIT_TIMEOUT_MS = 10 * 1000;
// Above MAX_TRACKED_PATH_BYTES so the typed oversize findings stay reachable;
// a spawn-level overflow still fails closed as GIT_UNAVAILABLE.
var GIT_MAX_BUFFER = 48 * 1024 * 1024;
var MAX_RECORD_FILES = 1000;
var MAX_RECORD_TRANSACTIONS = 100;
var MAX_WORKTREE_ENTRIES = 200;
var MAX_TRACKED_PATH_BYTES = 32 * 1024 * 1024; // ls-files scan bound
// Absolute free-space floor (owner decision: 2 GiB, expressed absolutely —
// percentage checks mislead on large volumes).
var DEFAULT_MIN_DISK_BYTES = 2 * 1024 * 1024 * 1024;
var MANAGER_NAMESPACE_PREFIX = 'refs/heads/orchestrator/task/';

// Read-only git runner. The allowlist pins the EXACT argument prefix, not
// just the subcommand, so verbs with mutating siblings (`worktree add`,
// `config` writes) cannot pass even from a coding mistake inside this module.
var GIT_ALLOWED_PREFIXES = [
  ['rev-parse'],
  ['worktree', 'list'],
  ['status', '--porcelain'],
  ['ls-files'],
  ['config', '--get'],
  ['config', '--get-regexp'],
  ['symbolic-ref'],
  ['check-ref-format'],
  ['check-attr'],
];
function allowedGitArgs(args) {
  if (!Array.isArray(args) || !args.length) return false;
  return GIT_ALLOWED_PREFIXES.some(function (prefix) {
    return prefix.every(function (token, index) { return args[index] === token; });
  });
}
function runGit(args, cwd, options) {
  if (!allowedGitArgs(args)) {
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
    if (options && typeof options.indexFile === 'string') env.GIT_INDEX_FILE = options.indexFile;
    result = cp.spawnSync('git', args, {
      cwd: cwd, env: env, timeout: GIT_TIMEOUT_MS, maxBuffer: GIT_MAX_BUFFER, encoding: 'utf8',
      input: options && typeof options.input === 'string' ? options.input : undefined
    });
  } catch (error) {
    return { ok: false, code: 'GIT_SPAWN_FAILED', stdout: '' };
  }
  if (result.error || result.signal || typeof result.status !== 'number') {
    return { ok: false, code: 'GIT_UNAVAILABLE', stdout: '' };
  }
  return { ok: result.status === 0, code: result.status === 0 ? null : 'GIT_EXIT_' + result.status,
    stdout: result.stdout || '', stderr: result.stderr || '' };
}

function identityOf(targetPath) {
  var real, stat;
  try { real = fs.realpathSync.native(targetPath); } catch (error) { return null; }
  try { stat = fs.lstatSync(real, { bigint: true }); } catch (error) { return null; }
  if (!stat.isDirectory()) return null;
  return { path: real.normalize('NFC'), dev: String(stat.dev), ino: String(stat.ino) };
}
function sameIdentity(a, b) {
  return !!a && !!b && a.dev === b.dev && a.ino === b.ino;
}

// Repository identity: the git common dir is THE repository key (both the
// control root and every linked worktree resolve to the same store).
function repositoryIdentity(rootDir) {
  var probe = runGit(['rev-parse', '--is-inside-work-tree', '--git-common-dir',
    '--show-toplevel'], rootDir);
  if (!probe.ok) return { ok: false, code: 'NOT_A_WORK_TREE' };
  var lines = probe.stdout.split('\n');
  if (lines.length < 3 || lines[0].trim() !== 'true') return { ok: false, code: 'NOT_A_WORK_TREE' };
  var commonDirRaw = lines[1].trim();
  var toplevel = lines[2].trim();
  if (!commonDirRaw || !toplevel) return { ok: false, code: 'NOT_A_WORK_TREE' };
  var commonDirPath = path.isAbsolute(commonDirRaw) ? commonDirRaw : path.resolve(rootDir, commonDirRaw);
  var commonDir = identityOf(commonDirPath);
  var toplevelIdentity = identityOf(toplevel);
  if (!commonDir || !toplevelIdentity) return { ok: false, code: 'REPOSITORY_IDENTITY_UNPROVABLE' };
  var objectFormat = runGit(['rev-parse', '--show-object-format'], rootDir);
  var format = objectFormat.ok ? objectFormat.stdout.trim() : null;
  if (format !== 'sha1') return { ok: false, code: 'OBJECT_FORMAT_UNSUPPORTED' };
  var bare = runGit(['rev-parse', '--is-bare-repository'], rootDir);
  if (!bare.ok || bare.stdout.trim() !== 'false') return { ok: false, code: 'BARE_REPOSITORY_UNSUPPORTED' };
  return {
    ok: true,
    gitCommonDirIdentity: commonDir,
    toplevel: toplevelIdentity,
    controlProjectId: worktreeContract.digest({
      path: commonDir.path, dev: commonDir.dev, ino: commonDir.ino
    })
  };
}

function fileExists(candidate) {
  try { fs.lstatSync(candidate); return true; } catch (error) { return false; }
}

// Typed environment prechecks (plan §18). Every finding is a report entry —
// the caller decides admission; this module never repairs anything.
function environmentPrechecks(rootDir) {
  var findings = [];
  function finding(code, severity, message) {
    findings.push({ code: code, severity: severity, message: String(message).slice(0, 400) });
  }
  var identity = repositoryIdentity(rootDir);
  if (!identity.ok) {
    finding(identity.code, 'blocker', 'repository identity is unprovable for ' + rootDir);
    return { findings: findings, identity: identity };
  }
  var gitDirProbe = runGit(['rev-parse', '--git-dir'], rootDir);
  var gitDir = gitDirProbe.ok ? path.resolve(rootDir, gitDirProbe.stdout.trim()) : null;
  // Unfinished repository operations (merge/rebase/cherry-pick/bisect) are
  // per-worktree state under the worktree git dir.
  if (gitDir) {
    [['MERGE_HEAD', 'GIT_MERGE_IN_PROGRESS'], ['CHERRY_PICK_HEAD', 'GIT_CHERRY_PICK_IN_PROGRESS'],
     ['REVERT_HEAD', 'GIT_REVERT_IN_PROGRESS'], ['BISECT_LOG', 'GIT_BISECT_IN_PROGRESS'],
     ['rebase-merge', 'GIT_REBASE_IN_PROGRESS'], ['rebase-apply', 'GIT_REBASE_IN_PROGRESS']
    ].forEach(function (pair) {
      if (fileExists(path.join(gitDir, pair[0]))) finding(pair[1], 'blocker', pair[0] + ' present in ' + gitDir);
    });
  } else {
    finding('GIT_DIR_UNPROVABLE', 'blocker', 'per-worktree git dir could not be resolved');
  }
  // Detached / unborn HEAD.
  var symbolic = runGit(['symbolic-ref', '-q', 'HEAD'], rootDir);
  if (!symbolic.ok) finding('DETACHED_HEAD_UNSUPPORTED', 'blocker', 'HEAD is detached or the ref is unreadable');
  else {
    var headRef = symbolic.stdout.trim();
    var born = runGit(['rev-parse', '-q', '--verify', headRef], rootDir);
    if (!born.ok) finding('UNBORN_BRANCH_UNSUPPORTED', 'blocker', headRef + ' has no commits');
  }
  // Submodules: .gitmodules OR gitlink stage entries — NEVER submodule.*
  // config (submodule.active may be set on plain repos).
  if (fileExists(path.join(rootDir, '.gitmodules'))) {
    finding('SUBMODULES_UNSUPPORTED', 'blocker', '.gitmodules present');
  } else {
    var stages = runGit(['ls-files', '--stage', '-z'], rootDir);
    if (!stages.ok) finding('INDEX_UNREADABLE', 'blocker', 'ls-files --stage failed');
    else if (stages.stdout.length > MAX_TRACKED_PATH_BYTES) finding('INDEX_OVERSIZED', 'blocker', 'tracked path listing exceeds the scan bound');
    else if (stages.stdout.split('\0').some(function (entry) { return entry.indexOf('160000 ') === 0; })) {
      finding('SUBMODULES_UNSUPPORTED', 'blocker', 'gitlink entries present in the index');
    }
  }
  // Sparse checkout.
  var sparse = runGit(['config', '--get', 'core.sparseCheckout'], rootDir);
  if (sparse.ok && sparse.stdout.trim() === 'true') finding('SPARSE_CHECKOUT_UNSUPPORTED', 'blocker', 'core.sparseCheckout=true');
  // Shallow history.
  var shallow = runGit(['rev-parse', '--is-shallow-repository'], rootDir);
  if (!shallow.ok || shallow.stdout.trim() !== 'false') finding('SHALLOW_REPOSITORY_UNSUPPORTED', 'blocker', 'history is shallow or unprovable');
  var tracked = runGit(['ls-files', '-z'], rootDir);
  var trackedPaths = null;
  if (!tracked.ok) finding('TRACKED_PATHS_UNREADABLE', 'blocker', 'ls-files failed');
  else if (tracked.stdout.length > MAX_TRACKED_PATH_BYTES) finding('TRACKED_PATHS_OVERSIZED', 'blocker', 'tracked path listing exceeds the scan bound');
  else trackedPaths = tracked.stdout.split('\0').filter(function (entry) { return entry !== ''; });
  // Content filters (LFS et al). Instead of enumerating attribute sources
  // (tree .gitattributes, info/attributes, core.attributesFile, global and
  // system files — an open set), ask git itself: check-attr resolves the
  // complete stack. Any tracked path with a set filter attribute is a typed
  // blocker; config-only filter drivers with no attribute referencing them
  // affect no path and are deliberately not flagged.
  if (trackedPaths !== null && trackedPaths.length) {
    var attrProbe = runGit(['check-attr', 'filter', '-z', '--stdin'], rootDir,
      { input: trackedPaths.join('\0') + '\0' });
    if (!attrProbe.ok) finding('ATTRIBUTES_UNPROVABLE', 'blocker', 'git check-attr failed');
    else {
      var attrTokens = attrProbe.stdout.split('\0');
      // Output is NUL-separated triplets: path, attribute, value.
      for (var attrIndex = 0; attrIndex + 2 < attrTokens.length; attrIndex += 3) {
        var attrValue = attrTokens[attrIndex + 2];
        if (attrValue !== 'unspecified' && attrValue !== 'unset') {
          finding('CONTENT_FILTERS_UNSUPPORTED', 'blocker',
            attrTokens[attrIndex] + ' carries filter=' + attrValue);
          break;
        }
      }
    }
  }
  // Case collisions among tracked paths (APFS folds case silently).
  if (trackedPaths !== null) {
    var seen = new Set(); var collision = null;
    trackedPaths.some(function (entry) {
      var folded = entry.toLowerCase();
      if (seen.has(folded)) { collision = entry; return true; }
      seen.add(folded);
      return false;
    });
    if (collision) finding('CASE_COLLIDING_PATHS_UNSUPPORTED', 'blocker', 'case-folded duplicate tracked path: ' + collision);
  }
  // Disk headroom (absolute bytes).
  try {
    var stat = fs.statfsSync(rootDir);
    var available = Number(stat.bavail) * Number(stat.bsize);
    if (!Number.isFinite(available)) finding('DISK_HEADROOM_UNPROVABLE', 'blocker', 'statfs returned a non-finite figure');
    else if (available < DEFAULT_MIN_DISK_BYTES) {
      finding('DISK_HEADROOM_LOW', 'blocker', 'free space ' + available + ' bytes is below the ' + DEFAULT_MIN_DISK_BYTES + ' byte floor');
    }
  } catch (error) {
    finding('DISK_HEADROOM_UNPROVABLE', 'blocker', 'statfs failed: ' + (error && error.message));
  }
  // Worktree home shape (never created here; absence is fine).
  try {
    var homeStat = fs.lstatSync(paths.WORKTREE_HOME);
    if (homeStat.isSymbolicLink()) finding('WORKTREE_HOME_UNSAFE', 'blocker', 'worktree home is a symlink');
    else if (!homeStat.isDirectory()) finding('WORKTREE_HOME_UNSAFE', 'blocker', 'worktree home is not a directory');
  } catch (error) { /* absent: created by Phase 2 provisioning, not here */ }
  return { findings: findings, identity: identity };
}

// `git worktree list --porcelain -z` parser. Attributes per entry:
// worktree <path> / HEAD <sha> / branch <ref> | detached / bare /
// locked [reason] / prunable [reason].
function parseWorktreeList(stdout) {
  var entries = [];
  var current = null;
  stdout.split('\0').forEach(function (token) {
    if (token === '') { if (current) { entries.push(current); current = null; } return; }
    var space = token.indexOf(' ');
    var key = space < 0 ? token : token.slice(0, space);
    var value = space < 0 ? '' : token.slice(space + 1);
    if (key === 'worktree') current = { path: value, head: null, branch: null, bare: false, detached: false, locked: null, prunable: null };
    else if (!current) return;
    else if (key === 'HEAD') current.head = value;
    else if (key === 'branch') current.branch = value;
    else if (key === 'bare') current.bare = true;
    else if (key === 'detached') current.detached = true;
    else if (key === 'locked') current.locked = value || 'locked';
    else if (key === 'prunable') current.prunable = value || 'prunable';
  });
  if (current) entries.push(current);
  return entries;
}

// Bounded, validated projection of manager records. Invalid bytes are
// reported, never deleted. Symlinked, hardlinked and oversized record files
// are rejected inside fileGuards.boundedRegularFileUnder (no-follow, nlink=1,
// byte bound) and surface here as UNREADABLE.
function readRecords(dir, authorityRoot, contract, label) {
  var out = { active: [], invalid: [], unavailable: null };
  var listed = fileGuards.boundedDirectoryNamesUnder(authorityRoot, dir, MAX_RECORD_FILES);
  if (!listed || !listed.ok) {
    out.unavailable = (listed && listed.code) || 'unreadable';
    return out;
  }
  // Publication/CAS WAL belongs to file-guards, not to either record schema.
  // A read-only projection must neither parse those JSON files as records nor
  // look through an unfinished transaction: the public name may be between
  // its old capture and replacement. Mutating lifecycle entry points reconcile
  // the exact durable protocol first; observers remain strictly fail-closed.
  if (listed.names.some(function (name) { return name.indexOf('.guard-') === 0; })) {
    out.unavailable = 'guard-transaction-pending';
    return out;
  }
  listed.names.filter(function (name) { return /^[A-Za-z0-9._-]{1,200}\.json$/.test(name); }).sort()
    .forEach(function (name) {
      var bounded = fileGuards.boundedRegularFileUnder(authorityRoot, dir, path.join(dir, name), contract.MAX_BYTES);
      if (!bounded) { out.invalid.push({ name: name, code: label + '_UNREADABLE', message: 'record is unreadable, oversized, hardlinked or unsafe' }); return; }
      try {
        var record = contract.validateBytes(bounded.bytes);
        if (label === 'WORKTREE_RECORD' && name !== record.worktreeId + '.json') {
          var filenameError = new Error('worktree record filename does not match its generation identity');
          filenameError.code = 'WORKTREE_RECORD_FILENAME_MISMATCH';
          throw filenameError;
        }
        out.active.push({ name: name, record: record, bytes: bounded.bytes, proof: bounded.stat });
      }
      catch (error) {
        out.invalid.push({ name: name, code: error.code || (label + '_INVALID'), message: String(error.message).slice(0, 300) });
      }
    });
  return out;
}

function readWorktreeRecordsForMutation() {
  var recovered = fileGuards.reconcileGuardTransactionsUnder(
    paths.WORKTREE_RECORDS_AUTHORITY_ROOT, paths.WORKTREE_RECORDS_DIR,
    { maxEntries: MAX_RECORD_FILES, maxTransactions: MAX_RECORD_TRANSACTIONS });
  if (!recovered || !recovered.ok || recovered.pending !== 0) {
    return { active: [], invalid: [], unavailable: recovered &&
      (recovered.code || Object.keys(recovered.codes || {}).sort().join(',')) ||
      'guard-transaction-recovery-unavailable' };
  }
  return readRecords(paths.WORKTREE_RECORDS_DIR, paths.WORKTREE_RECORDS_AUTHORITY_ROOT,
    worktreeContract, 'WORKTREE_RECORD');
}

// The complete binding a MANAGED classification requires (beyond the raw
// filesystem identity match). Returns null when fully bound, else the reason.
function managedBindingIssue(item, entry, checkoutIdentity, repoIdentity) {
  var record = item.record;
  if (record.executionRoot.path !== checkoutIdentity.path) return 'execution root path drifted';
  if (record.controlProjectId !== repoIdentity.controlProjectId) return 'record belongs to a different repository';
  if (!sameIdentity(record.gitCommonDirIdentity, repoIdentity.gitCommonDirIdentity)) {
    return 'record git common dir does not match this repository';
  }
  if (entry.branch !== record.candidateRef) {
    return 'checkout branch ' + (entry.branch || '(detached)') + ' is not the record candidate ref';
  }
  // The checkout's own .git pointer must still resolve into this repository
  // (plan §18: modified pointer after creation is a typed unsafe state).
  var pointer = repositoryIdentity(entry.path);
  if (!pointer.ok || !sameIdentity(pointer.gitCommonDirIdentity, repoIdentity.gitCommonDirIdentity)) {
    return 'checkout .git pointer no longer resolves into the recorded repository';
  }
  // A pointer redirected to a SIBLING worktree's admin dir keeps the same
  // common dir AND a cwd-derived toplevel, so neither catches it. The
  // checkout's own view of HEAD travels through the (possibly tampered)
  // pointer, while entry.branch above came from admin state — the two must
  // agree on the candidate ref.
  var ownHead = runGit(['symbolic-ref', '-q', 'HEAD'], entry.path);
  if (!ownHead.ok || ownHead.stdout.trim() !== record.candidateRef) {
    return 'checkout .git pointer resolves to a different worktree of this repository';
  }
  if (!sameIdentity(pointer.toplevel, checkoutIdentity)) {
    return 'checkout .git pointer resolves outside the recorded checkout';
  }
  return null;
}

// Full read-only discovery projection: identity + prechecks + classified
// worktree inventory + record health. Never mutates, never prunes.
function discover() {
  var rootDir = paths.PROJECT_ROOT;
  var prechecks = environmentPrechecks(rootDir);
  var projection = {
    version: 1,
    controlRoot: identityOf(rootDir),
    identity: prechecks.identity.ok ? {
      controlProjectId: prechecks.identity.controlProjectId,
      gitCommonDirIdentity: prechecks.identity.gitCommonDirIdentity
    } : null,
    findings: prechecks.findings,
    worktrees: [],
    records: { worktrees: null, integrations: null }
  };
  function finding(code, message, severity) {
    projection.findings.push({ code: code, severity: severity || 'blocker', message: String(message).slice(0, 400) });
  }
  var records = readRecords(paths.WORKTREE_RECORDS_DIR, paths.WORKTREE_RECORDS_AUTHORITY_ROOT,
    worktreeContract, 'WORKTREE_RECORD');
  var integrations = readRecords(paths.INTEGRATIONS_DIR, paths.WORKTREE_RECORDS_AUTHORITY_ROOT,
    integrationContract, 'INTEGRATION_RECORD');
  projection.records.worktrees = records;
  projection.records.integrations = integrations;
  // The record stores are the ownership authority: unreadable or invalid
  // state means nothing can be presumed foreign-and-free.
  if (records.unavailable) finding('WORKTREE_RECORDS_UNAVAILABLE', 'worktree record store unreadable: ' + records.unavailable);
  if (integrations.unavailable) finding('INTEGRATION_RECORDS_UNAVAILABLE', 'integration record store unreadable: ' + integrations.unavailable);
  records.invalid.forEach(function (entry) {
    finding('WORKTREE_RECORDS_INVALID', entry.name + ': ' + entry.code + ' — ' + entry.message);
  });
  integrations.invalid.forEach(function (entry) {
    finding('INTEGRATION_RECORDS_INVALID', entry.name + ': ' + entry.code + ' — ' + entry.message);
  });
  // The fail-closed crash outcome is never left implicit (plan §9.2).
  records.active.forEach(function (item) {
    if (item.record.status === 'recovery-required') {
      finding('WORKTREE_RECOVERY_REQUIRED', item.record.worktreeId + ' (' + item.record.stem + ') requires recovery');
    } else if (item.record.status === 'releasing') {
      finding('WORKTREE_RELEASE_INCOMPLETE', item.record.worktreeId + ' (' + item.record.stem + ') is mid-release');
    }
  });
  integrations.active.forEach(function (item) {
    if (item.record.status === 'recovery-required') {
      finding('INTEGRATION_RECOVERY_REQUIRED', item.record.integrationId + ' (' + item.record.stem + ') requires recovery');
    } else if (item.record.status === 'active') {
      // An unfinished integration always blocks the next one (plan §1).
      finding('INTEGRATION_INCOMPLETE', item.record.integrationId + ' (' + item.record.stem + ') is mid-flight at phase ' + item.record.phase);
    }
  });

  if (projection.identity === null) return projection;

  var listed = runGit(['worktree', 'list', '--porcelain', '-z'], rootDir);
  if (!listed.ok) {
    finding('WORKTREE_LIST_UNAVAILABLE', 'git worktree list failed');
    return projection;
  }
  var entries = parseWorktreeList(listed.stdout);
  if (entries.length > MAX_WORKTREE_ENTRIES) {
    finding('WORKTREE_LIST_OVERSIZED', entries.length + ' worktree entries exceed the bound');
    return projection;
  }
  var home = null;
  try { home = fs.realpathSync.native(paths.WORKTREE_HOME).normalize('NFC'); } catch (error) {}
  var controlIdentity = projection.controlRoot;
  var repoIdentity = prechecks.identity;
  var claimedRecordNames = new Set();
  entries.forEach(function (entry) {
    var identity = identityOf(entry.path);
    var row = {
      path: entry.path, head: entry.head, branch: entry.branch,
      bare: entry.bare, detached: entry.detached,
      locked: entry.locked, prunable: entry.prunable,
      classification: 'foreign', record: null
    };
    if (!identity) {
      // Unresolvable checkout (deleted dir, dangling entry). Still FOREIGN:
      // the manager owns nothing without an exact record match, and prunable
      // foreign entries are never touched (plan §3 addendum).
      row.classification = entry.prunable ? 'foreign' : 'unsafe';
      projection.worktrees.push(row);
      return;
    }
    if (sameIdentity(identity, controlIdentity)) {
      row.classification = 'control';
      projection.worktrees.push(row);
      return;
    }
    // Execution admission is narrower than ownership. Recovery/release records
    // can still claim their exact checkout for safe cleanup even though no new
    // child may bind there. Terminal/pre-create records claim nothing.
    var matches = records.active.filter(function (item) {
      return item.record.executionRoot !== null &&
        worktreeContract.CHECKOUT_OWNING_STATUSES.has(item.record.status) &&
        sameIdentity(item.record.executionRoot, identity);
    });
    if (matches.length > 1) {
      matches.forEach(function (item) { claimedRecordNames.add(item.name); });
      row.classification = 'unsafe';
      finding('WORKTREE_RECORD_DUPLICATE_CLAIM', matches.map(function (item) { return item.name; }).join(', ') +
        ' all claim the checkout at ' + entry.path);
      projection.worktrees.push(row);
      return;
    }
    if (matches.length === 1) {
      var item = matches[0];
      claimedRecordNames.add(item.name);
      var bindingIssue = managedBindingIssue(item, entry, identity, repoIdentity);
      if (bindingIssue) {
        row.classification = 'unsafe';
        finding('WORKTREE_RECORD_BINDING_MISMATCH', item.record.worktreeId + ': ' + bindingIssue);
      } else if (home === null || identity.path.indexOf(home + path.sep) !== 0) {
        // A managed checkout must sit inside the manager home; anything else
        // is an identity violation the manager reports (and never repairs).
        row.classification = 'unsafe';
        finding('MANAGED_WORKTREE_OUTSIDE_HOME', item.record.worktreeId + ' points outside the worktree home');
      } else {
        row.classification = 'managed';
        row.record = { name: item.name, worktreeId: item.record.worktreeId, stem: item.record.stem, status: item.record.status };
      }
      projection.worktrees.push(row);
      return;
    }
    // Before calling anything foreign: a CRASHED CREATE is not foreign. A kill
    // between `git worktree add` and the ready re-read leaves a create-intent
    // record whose candidate ref is exactly this checkout's branch — the
    // manager's own partial footprint, which provision() classifies as
    // recovery-required. Reporting it as a squatter on a foreign branch tells
    // the operator something external did this, and sends them looking for a
    // culprit that is this very manager. Real process deaths land here far more
    // often than genuine foreign checkouts do.
    var crashedCreate = records.active.find(function (item) {
      return item.record.status === 'create-intent' &&
        typeof entry.branch === 'string' && entry.branch === item.record.candidateRef;
    });
    if (crashedCreate) {
      claimedRecordNames.add(crashedCreate.name);
      row.classification = 'crashed-create';
      row.record = { name: crashedCreate.name, worktreeId: crashedCreate.record.worktreeId,
        stem: crashedCreate.record.stem, status: crashedCreate.record.status };
      finding('WORKTREE_CREATE_INTERRUPTED', crashedCreate.record.worktreeId + ' (' +
        crashedCreate.record.stem + ') was interrupted mid-create; the next provision for that ' +
        'stem classifies it as recovery-required and nothing here is ever destroyed', 'warning');
      projection.worktrees.push(row);
      return;
    }
    // No materialized record claims this checkout: FOREIGN and untouchable.
    // A foreign checkout squatting on the manager branch namespace, or any
    // checkout living INSIDE the manager home without a claiming record
    // (terminal-record drift, junk in manager territory), is still never
    // touched — but each is a typed state the operator must resolve.
    if (typeof entry.branch === 'string' && entry.branch.indexOf(MANAGER_NAMESPACE_PREFIX) === 0) {
      finding('MANAGER_NAMESPACE_BRANCH_FOREIGN', entry.path + ' sits on manager-namespace branch ' + entry.branch + ' without a record');
    }
    if (home !== null && identity.path.indexOf(home + path.sep) === 0) {
      row.classification = 'unsafe';
      finding('WORKTREE_HOME_SQUATTER', entry.path + ' lives inside the worktree home without a claiming record');
    }
    projection.worktrees.push(row);
  });
  if (!projection.worktrees.some(function (row) { return row.classification === 'control'; })) {
    finding('CONTROL_ROOT_NOT_IN_INVENTORY', 'no worktree entry matches the control root identity ' +
      (controlIdentity ? controlIdentity.path : '(unresolvable)'));
  }
  // Records whose execution root matches NO live checkout at all are orphan
  // recovery evidence (crash between add and re-read, or manual deletion).
  // Records that matched a checkout but failed binding already carry their
  // own typed finding above.
  records.active.forEach(function (item) {
    if (item.record.executionRoot === null) return;
    if (claimedRecordNames.has(item.name)) return;
    if (worktreeContract.CHECKOUT_OWNING_STATUSES.has(item.record.status)) {
      finding('WORKTREE_RECORD_ORPHANED', item.record.worktreeId + ' (' + item.record.stem + ') has no matching live checkout');
    }
  });
  return projection;
}

// ---------------------------------------------------------------------------
// Phase 2: provisioning (§9.2). One `run` gets one execution generation:
// prechecks → sealed target read → create-intent record → git-mutations
// add-worktree → verification → deterministic skill install → immutable task
// snapshot + execution manifest (§8) → 'ready' only after a full re-read.
// Every crash outcome is classifiable: resume-create (intent, nothing
// physical), ready (everything verifies), or recovery-required — never a
// guess, never age-based.
// ---------------------------------------------------------------------------

var gitMutations = require('./git-mutations');

var candidateContract = require('../../tasks/candidate-receipt-contract.cjs');

var SNAPSHOTS_DIR = path.join(paths.WORKTREE_RECORDS_DIR, '.snapshots');
var RECEIPTS_DIR = path.join(paths.WORKTREE_RECORDS_DIR, '.receipts');
var MANIFESTS_DIR = path.join(paths.WORKTREE_RECORDS_DIR, '.manifests');
var TASK_SNAPSHOT_MAX_BYTES = 512 * 1024;
var PROVISION_PATH_MAX_BYTES = 3072; // §7.1: verified before git worktree add
var SOURCE_REVISION_RE = /^sha256:[a-f0-9]{64}$/;
var EXECUTION_MANIFEST_FIELDS = [
  'apiGenerationHash', 'baseCommit', 'baseTree', 'candidateRef', 'capabilities',
  'controlProjectId', 'controlRoot', 'createdAt', 'dependencySnapshotHash',
  'executionRoot', 'figmaGenerationHash', 'gitCommonDirIdentity', 'manifestHash',
  'owner', 'projectConfigHash', 'requestId', 'runId', 'stem', 'targetRef',
  'taskSnapshotFile', 'taskSnapshotHash', 'taskSourceRevision', 'taskState',
  'version', 'worktreeId'
].sort();

function provisionFail(code, message) {
  return { ok: false, code: code, message: String(message || code).slice(0, 400) };
}
function ownerIdentity() {
  // The process-start identity is what makes this owner PROVABLE: a pid
  // alone is recycled, and a record whose owner cannot be re-identified
  // is a claim rather than evidence. writerLeases owns the capture, so
  // the records and the leases judge liveness by the same measure.
  var startId = null;
  try { startId = writerLeases.captureProcessStartId(process.pid); } catch (error) { startId = null; }
  return { hostname: require('os').hostname(), pid: process.pid, processStartId: startId,
    startedAt: new Date(Date.now() - Math.floor(process.uptime() * 1000)).toISOString() };
}
function jsonBytes(value) {
  return Buffer.from(JSON.stringify(value) + '\n', 'utf8');
}
function publishJsonNoClobber(dir, name, value, maxBytes, mode) {
  var file = path.join(dir, name);
  var published = fileGuards.publishNoClobberRegularFileUnder(
    paths.WORKTREE_RECORDS_AUTHORITY_ROOT, dir, file, jsonBytes(value), {
      create: true, directoryMode: 0o700, mode: mode || 0o600, maxBytes: maxBytes
    });
  return published && published.ok ? file : null;
}
function buildExecutionManifest(record, executionIdentity, snapshotFile) {
  var manifest = {
    version: 1, worktreeId: record.worktreeId, runId: record.runId,
    requestId: record.requestId, stem: record.stem,
    controlProjectId: record.controlProjectId,
    controlRoot: record.controlRoot, executionRoot: executionIdentity,
    gitCommonDirIdentity: record.gitCommonDirIdentity,
    targetRef: record.targetRef,
    candidateRef: record.candidateRef, baseCommit: record.baseCommit, baseTree: record.baseTree,
    taskState: 'todo', taskSourceRevision: record.taskSourceRevision,
    taskSnapshotHash: record.taskSnapshotHash, taskSnapshotFile: snapshotFile,
    projectConfigHash: record.projectConfigHash,
    dependencySnapshotHash: record.dependencySnapshotHash,
    figmaGenerationHash: record.figmaGenerationHash,
    apiGenerationHash: record.apiGenerationHash,
    capabilities: record.capabilities, createdAt: record.createdAt, owner: record.owner,
    manifestHash: 'sha256:' + '0'.repeat(64)
  };
  var manifestCopy = {};
  Object.keys(manifest).sort().forEach(function (key) {
    if (key !== 'manifestHash') manifestCopy[key] = manifest[key];
  });
  manifest.manifestHash = worktreeContract.digest(manifestCopy);
  return manifest;
}
function readExecutionManifest(record, executionIdentity) {
  var identity = executionIdentity || record.executionRoot;
  if (!identity) return null;
  var snapshotFile = path.join(SNAPSHOTS_DIR,
    record.taskSnapshotHash.slice('sha256:'.length) + '.md');
  var file = path.join(MANIFESTS_DIR, record.worktreeId + '.json');
  var bounded = fileGuards.boundedRegularFileUnder(paths.WORKTREE_RECORDS_AUTHORITY_ROOT,
    MANIFESTS_DIR, file, worktreeContract.MAX_BYTES);
  if (!bounded || bounded.stat.nlink !== '1') return null;
  var value;
  try { value = JSON.parse(bounded.bytes.toString('utf8')); } catch (error) { return null; }
  if (!value || typeof value !== 'object' || Array.isArray(value) ||
      Object.keys(value).sort().join('\0') !== EXECUTION_MANIFEST_FIELDS.join('\0') ||
      !bounded.bytes.equals(jsonBytes(value))) return null;
  var expected = buildExecutionManifest(record, identity, snapshotFile);
  if (worktreeContract.canonical(value) !== worktreeContract.canonical(expected)) return null;
  var snapshot = fileGuards.boundedRegularFileUnder(paths.WORKTREE_RECORDS_AUTHORITY_ROOT,
    SNAPSHOTS_DIR, snapshotFile, TASK_SNAPSHOT_MAX_BYTES);
  if (!snapshot || snapshot.stat.nlink !== '1' ||
      'sha256:' + crypto.createHash('sha256').update(snapshot.bytes).digest('hex') !== record.taskSnapshotHash) return null;
  return value;
}
function readWorktreeItem(worktreeId) {
  var file = path.join(paths.WORKTREE_RECORDS_DIR, worktreeId + '.json');
  var bounded = fileGuards.boundedRegularFileUnder(paths.WORKTREE_RECORDS_AUTHORITY_ROOT,
    paths.WORKTREE_RECORDS_DIR, file, worktreeContract.MAX_BYTES);
  if (!bounded) return null;
  try {
    var record = worktreeContract.validateBytes(bounded.bytes);
    if (record.worktreeId !== worktreeId) return null;
    return { name: worktreeId + '.json', record: record, bytes: bounded.bytes, proof: bounded.stat };
  }
  catch (error) { return null; }
}
// Exact lifecycle transition. Atomic rename alone is durable but not a mutex:
// two sealers can both read `ready`, both publish a candidate, and the loser can
// overwrite the winner's terminal status. The record generation (bytes+inode
// proof) is the CAS precondition, so only one caller owns `sealing`.
function casWorktreeRecord(item, next) {
  var result = fileGuards.compareAndSwapRegularFileUnder(
    paths.WORKTREE_RECORDS_AUTHORITY_ROOT, paths.WORKTREE_RECORDS_DIR,
    path.join(paths.WORKTREE_RECORDS_DIR, item.name), worktreeContract.MAX_BYTES,
    { proof: item.proof, bytes: item.bytes }, jsonBytes(next), { mode: 0o600 });
  return result && result.ok ? readWorktreeItem(next.worktreeId) : null;
}
function newWorktreeId() {
  return 'wt-' + crypto.randomBytes(16).toString('hex');
}
function candidateRefFor(stem, runId) {
  var number = /^TASK_([1-9][0-9]{0,15})_/.exec(stem)[1];
  var stemHash = crypto.createHash('sha256').update(stem, 'utf8').digest('hex').slice(0, 12);
  var short = String(runId).split('-').slice(-1)[0].slice(0, 32);
  return 'refs/heads/orchestrator/task/TASK_' + number + '-' + stemHash + '/' + short;
}
// §7.1: every home component is manager-created 0700, lstat-verified and
// symlink-free before use.
// §7.1: EVERY component of the worktree home chain is verified, not just the
// leaf. lstat alone proves the component we named is a real directory; it does
// NOT prove the path we walked is the path we think we walked — a symlinked
// ancestor resolves elsewhere and every lstat below it still passes. So each
// component is additionally realpath'd and its dev/ino compared to the lstat we
// just took: a component whose resolved identity differs from its own metadata
// is a redirect, and provisioning refuses rather than materializing a checkout
// somewhere it cannot name.
function ensureHomePath(components) {
  var current = paths.WORKTREE_HOME;
  var chain = [current].concat(components.map(function (component) {
    current = path.join(current, component);
    return current;
  }));
  // The anchor is the home's OWN resolved identity. Requiring each component's
  // realpath to equal its literal path would be wrong, not strict: a perfectly
  // legitimate ancestor may be a symlink (macOS resolves /var to /private/var
  // for every temporary directory). What must hold is that each component we
  // traverse resolves to exactly its parent's resolved path plus its name —
  // that is what a redirect breaks.
  var anchor;
  try { anchor = fs.realpathSync(chain[0]); }
  catch (error) {
    try { fs.mkdirSync(chain[0], { recursive: true, mode: 0o700 }); anchor = fs.realpathSync(chain[0]); }
    catch (again) { return null; }
  }
  for (var i = 0; i < chain.length; i++) {
    var dir = chain[i];
    try { fs.mkdirSync(dir, { mode: 0o700 }); } catch (error) {
      if (!error || error.code !== 'EEXIST') return null;
    }
    var stat;
    try { stat = fs.lstatSync(dir); } catch (error) { return null; }
    if (!stat.isDirectory() || stat.isSymbolicLink()) return null;
    var expected = i === 0 ? anchor : path.join(anchor, components[i - 1]);
    var resolved;
    try { resolved = fs.realpathSync(dir); } catch (error) { return null; }
    if (resolved !== expected) return null;
    var real;
    try { real = fs.lstatSync(resolved); } catch (error) { return null; }
    if (String(real.dev) !== String(stat.dev) || String(real.ino) !== String(stat.ino)) return null;
    anchor = resolved;
  }
  return chain[chain.length - 1];
}
function sameTaskGeneration(left, right) {
  if (!left || !right || !left.bytes.equals(right.bytes)) return false;
  return ['dev', 'ino', 'modeExact', 'sizeExact', 'mtimeNs', 'ctimeNs', 'nlink'].every(function (field) {
    return String(left.stat[field]) === String(right.stat[field]);
  });
}
function readControlTaskHeld(stem) {
  var file = path.join(paths.TASKS_DIR, 'todo', stem + '.md');
  var held = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT,
    path.join(paths.TASKS_DIR, 'todo'), file, TASK_SNAPSHOT_MAX_BYTES);
  return held && held.stat.nlink === '1' ? held : null;
}
function readControlTask(stem) {
  var held = readControlTaskHeld(stem);
  return held ? held.bytes : null;
}
// One worktree dependency pin represents the exact accepted generations, not
// merely the dependency names already embedded in the task body. A dependency
// can be reopened or have its Outcome repaired without changing the current
// task bytes; hashing only `meta.deps` therefore leaves a green run bound to a
// world that no longer satisfies Run admission.
function dependencySnapshotFromValidation(validation, stem) {
  var metadata = validation && validation._model && validation._model.metadata;
  var task = metadata && typeof metadata.get === 'function' ? metadata.get(stem) : null;
  if (!task || !Array.isArray(task.deps)) return { ok: false, code: 'DEPENDENCY_SNAPSHOT_UNAVAILABLE' };
  var dependencies = task.deps.slice().sort();
  if (new Set(dependencies).size !== dependencies.length) {
    return { ok: false, code: 'DEPENDENCY_SNAPSHOT_UNACCEPTED' };
  }
  var unresolved = Array.isArray(validation.findings) && validation.findings.some(function (item) {
    return item && item.stem === stem && item.code === 'RUN_DEPENDENCY_UNSATISFIED';
  });
  if (unresolved) return { ok: false, code: 'DEPENDENCY_SNAPSHOT_UNACCEPTED' };
  var rows = [];
  for (var i = 0; i < dependencies.length; i++) {
    var dependencyStem = dependencies[i];
    var dependency = metadata.get(dependencyStem);
    if (!dependency || dependency.state !== 'done' ||
        !SOURCE_REVISION_RE.test(String(dependency.revision || '')) ||
        !dependency.outcome || dependency.outcome.valid !== true ||
        ['completed', 'completed-with-caveats'].indexOf(dependency.outcome.status) < 0) {
      return { ok: false, code: 'DEPENDENCY_SNAPSHOT_UNACCEPTED' };
    }
    rows.push([dependencyStem, dependency.revision]);
  }
  return { ok: true, hash: 'sha256:' + crypto.createHash('sha256')
    .update('worktree-dependency-snapshot-v1\0', 'utf8')
    .update(JSON.stringify(rows), 'utf8').digest('hex') };
}
function currentDependencySnapshot(stem) {
  var validation;
  try { validation = taskIntegrity.validateAction('run', stem, 'worktree-dependency-pin'); }
  catch (error) { return { ok: false, code: 'DEPENDENCY_SNAPSHOT_UNAVAILABLE' }; }
  return dependencySnapshotFromValidation(validation, stem);
}
// Bind the raw snapshot bytes to the canonical queue sourceRevision in one
// exact filesystem generation. The two anchored reads bracket the canonical
// task-state validator, so an edit/replace/ABA during validation cannot make a
// transient snapshot inherit the admitted revision.
function proveControlTaskInput(stem, expectedSourceRevision, expectedSnapshotHash,
    expectedDependencySnapshotHash) {
  var before = readControlTaskHeld(stem);
  if (!before) return { ok: false, code: 'PROVISION_TASK_UNREADABLE' };
  var canonical;
  try { canonical = taskIntegrity.validateAction('run', stem, 'worktree-provision'); }
  catch (error) { return { ok: false, code: 'PROVISION_SOURCE_REVISION_UNAVAILABLE' }; }
  var after = readControlTaskHeld(stem);
  if (!sameTaskGeneration(before, after)) {
    return { ok: false, code: 'PROVISION_SOURCE_GENERATION_CHANGED' };
  }
  var snapshotHash = 'sha256:' + crypto.createHash('sha256').update(before.bytes).digest('hex');
  if (!canonical || canonical.observedState !== 'todo' ||
      canonical.sourceRevision !== expectedSourceRevision ||
      (expectedSnapshotHash !== null && expectedSnapshotHash !== snapshotHash)) {
    return { ok: false, code: 'PROVISION_SOURCE_REVISION_MISMATCH' };
  }
  var dependencySnapshot = dependencySnapshotFromValidation(canonical, stem);
  if (!dependencySnapshot.ok) {
    return { ok: false, code: dependencySnapshot.code === 'DEPENDENCY_SNAPSHOT_UNACCEPTED'
      ? 'PROVISION_DEPENDENCY_UNACCEPTED' : 'PROVISION_DEPENDENCY_SNAPSHOT_UNAVAILABLE' };
  }
  if (expectedDependencySnapshotHash !== null &&
      expectedDependencySnapshotHash !== dependencySnapshot.hash) {
    return { ok: false, code: 'PROVISION_DEPENDENCY_SNAPSHOT_MISMATCH' };
  }
  return { ok: true, bytes: before.bytes, snapshotHash: snapshotHash,
    dependencySnapshotHash: dependencySnapshot.hash };
}

// Provision one execution generation for a `run` of `stem`. The durable
// writer lease closes the cross-process gap between "no active record" and
// create-intent publication: random record filenames cannot provide that
// exclusion by themselves. The same per-stem arbiter also prevents overlap
// with an already running task child, while different stems remain parallel.
function provision(options) {
  var stem = options && options.stem;
  if (typeof stem !== 'string' || !worktreeContract.STEM_RE.test(stem)) {
    return provisionFail('PROVISION_STEM_INVALID', 'stem is not canonical');
  }
  var leaseRecovery;
  try { leaseRecovery = writerLeases.reconcileStaleMutations(
    paths.WRITER_LEASES_DIR, paths.WRITER_AUTHORITY_ROOT); }
  catch (error) { return provisionFail('PROVISION_LEASE_RECOVERY_UNSAFE', error && error.message); }
  if (leaseRecovery.blocked.length) return provisionFail('PROVISION_LEASE_RECOVERY_REQUIRED',
    leaseRecovery.blocked[0].message || leaseRecovery.blocked[0].code);
  var finalizations = require('./finalizations');
  var lease = finalizations.beginMutation({
    kind: 'task-session', stem: stem,
    sessionId: finalizations.createWriterSessionId(),
    key: 'worktree-provision:' + stem,
    pendingChild: false
  });
  if (!lease.ok) return provisionFail('PROVISION_LEASE_REFUSED',
    lease.detail || lease.error || 'the per-stem provision lease was refused');
  var result, thrown = null;
  try { result = provisionUnderLease(options); }
  catch (error) { thrown = error; }
  var released = finalizations.endMutation(lease.handle);
  if (thrown) throw thrown;
  if (!released) return provisionFail('PROVISION_LEASE_RELEASE_FAILED',
    'the per-stem provision lease could not be released exactly');
  return result;
}

// Fail-closed on
// every §18 blocker; a refused provision leaves at most a create-intent
// record plus mutation receipts — durable evidence, never partial trees
// pretending readiness.
function provisionUnderLease(options) {
  var stem = options && options.stem;
  var runId = options && options.runId;
  var requestId = options && options.requestId;
  var sourceRevision = options && options.sourceRevision;
  if (typeof stem !== 'string' || !/^TASK_[1-9][0-9]{0,15}_[A-Za-z0-9_]{1,120}$/.test(stem)) {
    return provisionFail('PROVISION_STEM_INVALID', 'stem is not canonical');
  }
  if (typeof runId !== 'string' || !worktreeContract.RUN_ID_RE.test(runId)) {
    return provisionFail('PROVISION_RUN_ID_INVALID', 'runId is not canonical');
  }
  if (typeof requestId !== 'string' || !worktreeContract.REQUEST_ID_RE.test(requestId)) {
    return provisionFail('PROVISION_REQUEST_ID_INVALID', 'requestId is not canonical');
  }
  if (typeof sourceRevision !== 'string' || !SOURCE_REVISION_RE.test(sourceRevision)) {
    return provisionFail('PROVISION_SOURCE_REVISION_INVALID', 'sourceRevision is not canonical');
  }
  var rootDir = paths.PROJECT_ROOT;
  var prechecks = environmentPrechecks(rootDir);
  var blockers = prechecks.findings.filter(function (row) { return row.severity === 'blocker'; });
  if (!prechecks.identity.ok || blockers.length) {
    return provisionFail('PROVISION_ENVIRONMENT_BLOCKED',
      blockers.map(function (row) { return row.code; }).join(',') || prechecks.identity.code);
  }
  var records = readWorktreeRecordsForMutation();
  if (records.unavailable || records.invalid.length) {
    return provisionFail('PROVISION_RECORDS_UNSAFE', 'worktree record store is not readable: ' +
      (records.unavailable || records.invalid[0].code));
  }
  // One active generation per task (plan §1). A live READY generation is
  // resumable (a re-Run of the same task reuses its worktree, §9.3); every
  // other live status refuses — recovery is explicit, never guessed.
  var live = records.active.find(function (item) {
    return item.record.stem === stem &&
      item.record.status !== 'completed' && item.record.status !== 'released';
  });
  if (live && live.record.status === 'ready') {
    var resumed = live.record.taskSourceRevision === sourceRevision
      ? resumeReady(live.record, sourceRevision) : null;
    if (resumed) return resumed;
    // Distinguish "inputs moved on" (a stale generation the operator releases
    // and re-runs) from "the physical generation is broken" (recovery).
    var staleInputs = false;
    var liveTaskBytes = readControlTask(live.record.stem);
    if (liveTaskBytes !== null) {
      var liveTaskHash = 'sha256:' + crypto.createHash('sha256').update(liveTaskBytes).digest('hex');
      var liveTargetProbe = runGit(['rev-parse', '-q', '--verify', live.record.targetRef], rootDir);
      var liveDependencies = currentDependencySnapshot(live.record.stem);
      staleInputs = liveTaskHash !== live.record.taskSnapshotHash ||
        !liveTargetProbe.ok || liveTargetProbe.stdout.trim() !== live.record.baseCommit ||
        liveDependencies.code === 'DEPENDENCY_SNAPSHOT_UNACCEPTED' ||
        (liveDependencies.ok && liveDependencies.hash !== live.record.dependencySnapshotHash);
    }
    if (staleInputs) {
      return provisionFail('PROVISION_GENERATION_STALE',
        live.record.worktreeId + ' was sealed against inputs that have since moved; release it to re-run');
    }
    markRecoveryRequired(live.record, 'ready generation failed re-verification at resume');
    return provisionFail('PROVISION_RESUME_FAILED',
      live.record.worktreeId + ' no longer verifies; recovery required');
  }
  if (live && live.record.status === 'create-intent') {
    // §9.2 crash outcomes: an intent with NO physical footprint (no branch,
    // no checkout, no manifest) resumes creation of the SAME generation at
    // its recorded base; any partial footprint is recovery evidence.
    if (live.record.runId !== runId || live.record.requestId !== requestId ||
        live.record.taskSourceRevision !== sourceRevision) {
      return provisionFail('PROVISION_INTENT_MISMATCH',
        live.record.worktreeId + ' belongs to a different admitted request generation');
    }
    var branchLive = runGit(['rev-parse', '-q', '--verify', live.record.candidateRef], rootDir).ok;
    var manifestLive = fileExists(path.join(MANIFESTS_DIR, live.record.worktreeId + '.json'));
    var repoKeyResume = live.record.controlProjectId.slice('sha256:'.length, 'sha256:'.length + 16);
    var resumePath = path.join(paths.WORKTREE_HOME, repoKeyResume, live.record.worktreeId);
    var checkoutLive = fileExists(resumePath);
    if (branchLive || manifestLive || checkoutLive) {
      // If Git created the checkout before the owner died, pin that exact
      // filesystem generation now. A recovery record with a null root must
      // never delete its branch while silently leaving an attached checkout.
      markRecoveryRequired(live.record, 'crashed provisioning left a partial footprint',
        checkoutLive ? identityOf(resumePath) : null);
      return provisionFail('PROVISION_RECOVERY_REQUIRED',
        live.record.worktreeId + ' left a partial footprint; recovery required');
    }
    if (live.record.controlProjectId !== prechecks.identity.controlProjectId) {
      markRecoveryRequired(live.record, 'create-intent belongs to a different repository identity');
      return provisionFail('PROVISION_RECOVERY_REQUIRED', 'create-intent repository identity mismatch');
    }
    return completeProvision(live.record, resumePath, prechecks.identity);
  }
  if (live && live.record.status === 'revalidation-required') {
    // Target drift invalidated the sealed gates (§9.6). The generation is not
    // damaged — it is superseded: release it (CLI `task-worktree.mjs release`
    // or the board action) and the next run provisions a fresh one from the
    // new target. Never silently rebase a green candidate onto a moved base.
    return provisionFail('PROVISION_REVALIDATION_REQUIRED',
      live.record.worktreeId + ' was sealed against a target that has moved; release it to run again');
  }
  if (live) {
    return provisionFail('PROVISION_GENERATION_ACTIVE',
      live.record.worktreeId + ' is still ' + live.record.status + ' for ' + stem);
  }
  // Sealed target: the control root's own named branch at this exact moment.
  var symbolic = runGit(['symbolic-ref', '-q', 'HEAD'], rootDir);
  if (!symbolic.ok) return provisionFail('PROVISION_TARGET_UNRESOLVED', 'control root HEAD is not a named branch');
  var targetRef = symbolic.stdout.trim();
  if (!worktreeContract.targetRefValid(targetRef)) {
    return provisionFail('PROVISION_TARGET_INVALID', targetRef + ' is not a plain local branch');
  }
  var baseCommitProbe = runGit(['rev-parse', '-q', '--verify', targetRef], rootDir);
  var baseTreeProbe = runGit(['rev-parse', '-q', '--verify', targetRef + '^{tree}'], rootDir);
  if (!baseCommitProbe.ok || !baseTreeProbe.ok) {
    return provisionFail('PROVISION_TARGET_UNRESOLVED', 'target commit/tree unreadable');
  }
  var baseCommit = baseCommitProbe.stdout.trim();
  var baseTree = baseTreeProbe.stdout.trim();
  // Immutable task snapshot (§8): the child never trusts a checkout or the
  // mutable control corpus for its task text.
  var taskInput = proveControlTaskInput(stem, sourceRevision, null, null);
  if (!taskInput.ok) return provisionFail(taskInput.code,
    'the task or accepted dependency generation does not match Run admission');
  var taskBytes = taskInput.bytes;
  var taskSnapshotHash = taskInput.snapshotHash;
  var repoIdentity = prechecks.identity;
  var repoKey = repoIdentity.controlProjectId.slice('sha256:'.length, 'sha256:'.length + 16);
  var worktreeId = newWorktreeId();
  var candidateRef = candidateRefFor(stem, runId);
  var executionPath = ensureHomePath([repoKey]);
  if (executionPath === null) return provisionFail('PROVISION_HOME_UNSAFE', 'worktree home components failed verification');
  executionPath = path.join(executionPath, worktreeId);
  if (Buffer.byteLength(executionPath, 'utf8') > PROVISION_PATH_MAX_BYTES) {
    return provisionFail('PROVISION_PATH_TOO_LONG', 'execution path exceeds the §7.1 bound');
  }
  var controlRoot = identityOf(rootDir);
  var now = new Date().toISOString();
  var record = {
    version: 1, worktreeId: worktreeId, runId: runId, requestId: requestId,
    stem: stem, status: 'create-intent',
    controlProjectId: repoIdentity.controlProjectId,
    gitCommonDirIdentity: repoIdentity.gitCommonDirIdentity,
    controlRoot: controlRoot,
    executionRoot: null,
    targetRef: targetRef,
    candidateRef: candidateRef,
    baseCommit: baseCommit, baseTree: baseTree,
    taskState: 'todo',
    taskSourceRevision: sourceRevision,
    taskSnapshotHash: taskSnapshotHash,
    projectConfigHash: hashFileOrEmpty(paths.PROJECT_CONFIG_FILE),
    dependencySnapshotHash: taskInput.dependencySnapshotHash,
    // §12.2/§12.3: the Figma and API generations this run is pinned to. They
    // travel into the candidate receipt, so a sync or a contract refresh landing
    // mid-run makes the sealed receipt's inputs differ from the live ones and
    // the bound gates stale — which is the whole point of pinning them.
    figmaGenerationHash: hashFileOrEmpty(path.join(paths.PROJECT_ROOT, 'orchestrator', 'figma', 'manifests', 'current-generation.json')),
    apiGenerationHash: hashFileOrEmpty(path.join(paths.API_CONTRACT_DIR, 'manifests', 'current-generation.json')),
    capabilities: [],
    owner: ownerIdentity(),
    createdAt: now, updatedAt: now,
    recordHash: 'sha256:' + '0'.repeat(64)
  };
  record.recordHash = worktreeContract.recordHash(record);
  try { worktreeContract.validate(record); } catch (error) {
    return provisionFail('PROVISION_RECORD_INVALID', error.message);
  }
  if (!publishJsonNoClobber(paths.WORKTREE_RECORDS_DIR, worktreeId + '.json', record,
      worktreeContract.MAX_BYTES, 0o600)) {
    return provisionFail('PROVISION_RECORD_UNWRITABLE', 'create-intent record could not be published');
  }
  return completeProvision(record, executionPath, repoIdentity);
}
// Shared §9.2 tail: physical creation through 'ready', used by fresh
// provisioning AND resume-create. Every pin comes from the durable record —
// a resumed generation is byte-identical to what its intent sealed.
function completeProvision(record, executionPath, repoIdentity) {
  var taskInput = proveControlTaskInput(record.stem, record.taskSourceRevision,
    record.taskSnapshotHash, record.dependencySnapshotHash);
  if (!taskInput.ok) {
    markRecoveryRequired(record, 'the sealed task/dependency input changed at (re)creation');
    return provisionFail('PROVISION_RECOVERY_REQUIRED', taskInput.code);
  }
  var taskBytes = taskInput.bytes;
  var taskSnapshotHash = taskInput.snapshotHash;
  var repoKey = record.controlProjectId.slice('sha256:'.length, 'sha256:'.length + 16);
  if (ensureHomePath([repoKey]) === null) {
    markRecoveryRequired(record, 'worktree home components failed verification');
    return provisionFail('PROVISION_HOME_UNSAFE', 'worktree home components failed verification');
  }
  var added = gitMutations.addWorktree({ candidateRef: record.candidateRef,
    targetPath: executionPath, baseCommit: record.baseCommit });
  if (!added.ok) {
    markRecoveryRequired(record, 'worktree add failed: ' + added.code);
    return provisionFail(added.code, added.message);
  }
  var executionIdentity = identityOf(executionPath);
  var pointer = repositoryIdentity(executionPath);
  var clean = runGit(['status', '--porcelain'], executionPath);
  var ownHead = runGit(['symbolic-ref', '-q', 'HEAD'], executionPath);
  if (!executionIdentity || !pointer.ok ||
      !sameIdentity(pointer.gitCommonDirIdentity, repoIdentity.gitCommonDirIdentity) ||
      !clean.ok || clean.stdout !== '' || !ownHead.ok || ownHead.stdout.trim() !== record.candidateRef) {
    markRecoveryRequired(record, 'created worktree failed verification', identityOf(executionPath));
    return provisionFail('PROVISION_VERIFY_FAILED', 'created worktree failed verification');
  }
  var installed;
  try {
    installed = cp.spawnSync('bash',
      [path.join(executionPath, 'orchestrator', 'skills', 'install-skills.sh'), executionPath, '--no-hooks'],
      { cwd: executionPath, timeout: 120 * 1000, maxBuffer: GIT_MAX_BUFFER, encoding: 'utf8' });
  } catch (error) { installed = null; }
  if (!installed || installed.status !== 0) {
    markRecoveryRequired(record, 'skill install failed in the execution root', identityOf(executionPath));
    return provisionFail('PROVISION_SKILL_INSTALL_FAILED', installed && installed.stderr);
  }
  var snapshotFile = path.join(SNAPSHOTS_DIR, taskSnapshotHash.slice('sha256:'.length) + '.md');
  var snapshotPublication = fileGuards.publishNoClobberRegularFileUnder(
    paths.WORKTREE_RECORDS_AUTHORITY_ROOT, SNAPSHOTS_DIR, snapshotFile, taskBytes,
    { create: true, directoryMode: 0o700, mode: 0o400, maxBytes: TASK_SNAPSHOT_MAX_BYTES });
  if (!snapshotPublication || !snapshotPublication.ok) {
    if (!snapshotPublication || snapshotPublication.code !== 'exists') {
      markRecoveryRequired(record, 'task snapshot could not be materialized', identityOf(executionPath));
      return provisionFail('PROVISION_SNAPSHOT_UNWRITABLE', 'task snapshot could not be materialized');
    }
    // Adopting a pre-existing content-addressed file is only safe once its
    // bytes are proven: a torn earlier write would otherwise become this
    // run's task text.
    var existing = fileGuards.boundedRegularFileUnder(paths.WORKTREE_RECORDS_AUTHORITY_ROOT,
      SNAPSHOTS_DIR, snapshotFile, TASK_SNAPSHOT_MAX_BYTES);
    var existingHash = !existing || existing.stat.nlink !== '1' ? null :
      'sha256:' + crypto.createHash('sha256').update(existing.bytes).digest('hex');
    if (existingHash !== taskSnapshotHash) {
      markRecoveryRequired(record, 'existing task snapshot does not match its content address',
        identityOf(executionPath));
      return provisionFail('PROVISION_SNAPSHOT_CORRUPT',
        'existing task snapshot does not match its content address');
    }
  }
  var manifest = buildExecutionManifest(record, executionIdentity, snapshotFile);
  var manifestFile = publishJsonNoClobber(MANIFESTS_DIR, record.worktreeId + '.json', manifest,
    worktreeContract.MAX_BYTES, 0o600);
  if (!manifestFile) {
    markRecoveryRequired(record, 'execution manifest could not be published', identityOf(executionPath));
    return provisionFail('PROVISION_MANIFEST_UNWRITABLE', 'execution manifest could not be published');
  }
  if (!readExecutionManifest(record, executionIdentity)) {
    markRecoveryRequired(record, 'execution manifest failed exact post-publication verification', identityOf(executionPath));
    return provisionFail('PROVISION_MANIFEST_INVALID', 'execution manifest failed exact verification');
  }
  var ready = Object.assign({}, record, { status: 'ready', executionRoot: executionIdentity,
    updatedAt: new Date().toISOString() });
  ready.recordHash = worktreeContract.recordHash(ready);
  var currentIntent = readWorktreeItem(record.worktreeId);
  var publishedReady = currentIntent && currentIntent.record.recordHash === record.recordHash
    ? casWorktreeRecord(currentIntent, ready) : null;
  if (!publishedReady) {
    markRecoveryRequired(record, 'ready record could not be published', identityOf(executionPath));
    return provisionFail('PROVISION_RECORD_UNWRITABLE', 'ready record could not be published');
  }
  var reread = readRecords(paths.WORKTREE_RECORDS_DIR, paths.WORKTREE_RECORDS_AUTHORITY_ROOT,
    worktreeContract, 'WORKTREE_RECORD');
  var confirmed = reread.active.find(function (item) { return item.record.worktreeId === record.worktreeId; });
  var manifestReread = confirmed ? readExecutionManifest(confirmed.record) : null;
  if (!confirmed || confirmed.record.status !== 'ready' || !manifestReread ||
      manifestReread.manifestHash !== manifest.manifestHash) {
    markRecoveryRequired(ready, 'post-publish re-read failed');
    return provisionFail('PROVISION_REREAD_FAILED', 'post-publish re-read failed');
  }
  return { ok: true, worktreeId: record.worktreeId, runId: record.runId,
    executionRoot: executionIdentity.path,
    manifestFile: manifestFile, taskSnapshotFile: snapshotFile, taskSnapshotHash: taskSnapshotHash,
    candidateRef: record.candidateRef, baseCommit: record.baseCommit,
    baseTree: record.baseTree, targetRef: record.targetRef };
}

// Re-admit an existing READY generation: full identity + manifest + snapshot
// re-verification, no mutations. Returns the same context provision() would.
function resumeReady(record, expectedSourceRevision) {
  if (record.executionRoot === null) return null;
  var liveIdentity = identityOf(record.executionRoot.path);
  if (!liveIdentity || !sameIdentity(liveIdentity, record.executionRoot) ||
      liveIdentity.path !== record.executionRoot.path) return null;
  var pointer = repositoryIdentity(record.executionRoot.path);
  if (!pointer.ok || !sameIdentity(pointer.gitCommonDirIdentity, record.gitCommonDirIdentity)) return null;
  var ownHead = runGit(['symbolic-ref', '-q', 'HEAD'], record.executionRoot.path);
  if (!ownHead.ok || ownHead.stdout.trim() !== record.candidateRef) return null;
  var manifestFile = path.join(MANIFESTS_DIR, record.worktreeId + '.json');
  var manifest = readExecutionManifest(record);
  if (!manifest) return null;
  var snapshotFile = manifest.taskSnapshotFile;
  var snapshotBytes = null;
  try { snapshotBytes = fs.readFileSync(snapshotFile); } catch (error) { return null; }
  var snapshotHash = 'sha256:' + crypto.createHash('sha256').update(snapshotBytes).digest('hex');
  if (snapshotHash !== record.taskSnapshotHash) return null;
  // A resumable generation must still be CURRENT, not merely self-consistent:
  // the live control task must hash to the sealed snapshot (owner answers or
  // edits since sealing invalidate it) and the target branch must not have
  // moved off the sealed base (that is target drift, plan §9.6).
  var liveInput = proveControlTaskInput(record.stem, expectedSourceRevision,
    record.taskSnapshotHash, record.dependencySnapshotHash);
  if (!liveInput.ok) return null;
  var liveTarget = runGit(['rev-parse', '-q', '--verify', record.targetRef], paths.PROJECT_ROOT);
  if (!liveTarget.ok || liveTarget.stdout.trim() !== record.baseCommit) return null;
  return { ok: true, resumed: true, worktreeId: record.worktreeId, runId: record.runId,
    executionRoot: record.executionRoot.path, manifestFile: manifestFile,
    taskSnapshotFile: snapshotFile, taskSnapshotHash: record.taskSnapshotHash,
    candidateRef: record.candidateRef, baseCommit: record.baseCommit,
    baseTree: record.baseTree, targetRef: record.targetRef };
}
function hashFileOrEmpty(file) {
  try {
    var bytes = fs.readFileSync(file);
    return 'sha256:' + crypto.createHash('sha256').update(bytes).digest('hex');
  } catch (error) {
    return 'sha256:' + crypto.createHash('sha256').update(Buffer.alloc(0)).digest('hex');
  }
}
// The ONE transition into 'revalidation-required' (§6.14/§9.6). A generation is
// SUPERSEDED, not damaged: its target moved, or an input it was pinned against
// was regenerated, so its green gates describe a world that no longer exists.
// The status is what makes the board stop offering Integrate and start offering
// a release, so detecting the drift without recording it — which is what the
// pre-Integrate check used to do — leaves the board claiming "ready" for a
// candidate that can never be integrated.
function markRevalidationRequired(record, reason, observedItem) {
  if (record.status === 'revalidation-required') return record;
  var item = observedItem || readWorktreeItem(record.worktreeId);
  if (!item || item.record.recordHash !== record.recordHash) return null;
  var drifted = Object.assign({}, record, { status: 'revalidation-required',
    updatedAt: new Date().toISOString() });
  drifted.recordHash = worktreeContract.recordHash(drifted);
  var published = casWorktreeRecord(item, drifted);
  if (!published) return null;
  console.warn('[worktree] ' + record.worktreeId + ' (' + record.stem +
    ') needs revalidation: ' + reason);
  return published.record;
}

// Re-prove everything a sealed generation was pinned against, and RECORD the
// verdict. Returns null when the generation is still current, else the reason
// it is not — after transitioning the record, so every later reader (the board
// projection, provision(), the CLI) sees the same superseded generation.
// The Figma and API generation hashes were pinned at provisioning and carried
// into the candidate receipt, but nothing ever compared them: a sync or a
// contract refresh mid-run left green gates describing a world that had moved.
function generationDriftIssue(record) {
  var reasons = [];
  var targetDrifted = false;
  var target = runGit(['rev-parse', '-q', '--verify', record.targetRef], paths.PROJECT_ROOT);
  if (!target.ok) return { code: 'TARGET_REF_UNAVAILABLE', message: record.targetRef + ' is unreadable' };
  if (target.stdout.trim() !== record.baseCommit) {
    targetDrifted = true;
    reasons.push(record.targetRef + ' moved off the sealed base');
  }
  var liveFigma = hashFileOrEmpty(path.join(paths.PROJECT_ROOT, 'orchestrator', 'figma',
    'manifests', 'current-generation.json'));
  var liveApi = hashFileOrEmpty(path.join(paths.API_CONTRACT_DIR, 'manifests',
    'current-generation.json'));
  if (record.figmaGenerationHash !== null && record.figmaGenerationHash !== liveFigma) {
    reasons.push('the Figma generation was regenerated after this candidate was pinned');
  }
  if (record.apiGenerationHash !== null && record.apiGenerationHash !== liveApi) {
    reasons.push('the API contract generation was regenerated after this candidate was pinned');
  }
  // Bracket the canonical dependency-closure validation with two reads of the
  // current task generation. Separate task/dependency reads leave an ABA-sized
  // gap where a changed task can lend its old bytes to a new dependency
  // verdict. The provisioning proof already closes that collection boundary,
  // so every later consumer reuses the same proof instead of approximating it.
  var liveRunInput = proveControlTaskInput(record.stem, record.taskSourceRevision,
    record.taskSnapshotHash, record.dependencySnapshotHash);
  if (!liveRunInput.ok) {
    if (liveRunInput.code.indexOf('PROVISION_DEPENDENCY_') === 0) {
      reasons.push('the accepted dependency generation changed or is no longer provable');
    } else {
      reasons.push('the canonical task changed or is no longer readable');
    }
  }
  if (hashFileOrEmpty(paths.PROJECT_CONFIG_FILE) !== record.projectConfigHash) {
    reasons.push('the project configuration changed after this generation was pinned');
  }
  if (!reasons.length) return null;
  return { code: 'GENERATION_SUPERSEDED',
    message: reasons.join('; ') + '; the candidate needs revalidation',
    targetDrifted: targetDrifted };
}

function revalidationIssueFor(record) {
  var issue = generationDriftIssue(record);
  if (!issue) return null;
  if (issue.code !== 'GENERATION_SUPERSEDED') return issue;
  // The updated record comes back so the caller's projection describes what is
  // now ON DISK. Returning only the reason left the in-memory record saying
  // 'ready-for-integration' and the board reporting a generic block.
  var updated = markRevalidationRequired(record, issue.message);
  if (!updated) {
    return { code: 'REVALIDATION_UNRECORDABLE', message: 'the superseded status could not be published' };
  }
  return { code: issue.code, message: issue.message, record: updated };
}

function markRecoveryRequired(record, reason, executionRootIdentity) {
  // Keep the pointer to whatever physically exists: a record that forgets its
  // checkout cannot be released or reasoned about later. `reason` stays in the
  // caller's log/finding text; the record schema is closed by contract.
  var identityForRecord = executionRootIdentity ||
    (record.executionRoot !== null ? record.executionRoot : null);
  var updated = Object.assign({}, record, { status: 'recovery-required',
    executionRoot: identityForRecord, updatedAt: new Date().toISOString() });
  updated.recordHash = worktreeContract.recordHash(updated);
  var current = readWorktreeItem(record.worktreeId);
  var settled = current && current.record.recordHash === record.recordHash
    ? casWorktreeRecord(current, updated) : null;
  console.warn('[worktree] ' + record.worktreeId + ' (' + record.stem + ') requires recovery: ' + reason);
  return settled ? settled.record : null;
}

// Owned cleanup after a completed/refused generation is a replayable WAL:
// record(releasing) -> remove clean checkout -> atomically marker+delete ref ->
// record(released). Only exact record ownership authorizes it. Nothing below
// the checkout is pre-deleted: Git's unforced removal is the safety proof that
// tracked changes and every unproven byte survive a refused release.
// Fail-closed: an unreadable lease store is never read as "nobody is building".
function executionLeaseIssue(worktreeId) {
  var key = writerLeases.executionLeaseKeyFor(worktreeId);
  var scan;
  try { scan = writerLeases.scan(paths.WRITER_LEASES_DIR, paths.WRITER_AUTHORITY_ROOT); }
  catch (error) {
    return { code: 'RELEASE_WRITER_STATE_UNSAFE', message: 'writer lease state is unreadable: ' + (error && error.message || error) };
  }
  if (scan.issues.length) return { code: 'RELEASE_WRITER_STATE_UNSAFE',
    message: 'writer lease state is unsafe: ' + scan.issues[0].message };
  var row = scan.active.find(function (candidate) { return candidate.key === key; });
  return row ? { code: 'RELEASE_EXECUTION_BUSY',
    message: 'an app-run holds ' + key + ' (lease ' + row.leaseId + '); the checkout cannot be removed under it' } : null;
}

var releasesInFlight = Object.create(null);
function claimRelease(item) {
  var record = item.record;
  var allowed = worktreeContract.RELEASABLE_STATUSES.has(record.status);
  if (!allowed) return { ok: false, result: provisionFail(record.status === 'sealing'
    ? 'RELEASE_SEAL_ACTIVE' : 'RELEASE_STATUS_INVALID',
  record.status === 'sealing' ? 'candidate sealing owns this generation' :
    record.worktreeId + ' is ' + record.status + ', not releasable') };
  if (releasesInFlight[record.worktreeId]) {
    return { ok: false, result: provisionFail('RELEASE_BUSY', record.worktreeId + ' already has a live release owner') };
  }
  if (record.status === 'releasing') {
    var thisOwner = ownerIdentity();
    var sameProcess = record.owner.hostname === thisOwner.hostname && record.owner.pid === thisOwner.pid &&
      record.owner.processStartId !== null && record.owner.processStartId === thisOwner.processStartId;
    if (!sameProcess) {
      if (record.owner.hostname !== require('os').hostname()) {
        return { ok: false, result: provisionFail('RELEASE_OWNER_UNPROVABLE',
          'the interrupted release owner belongs to another host') };
      }
      var ownerState = writerLeases.processIdentityState(record.owner.pid, record.owner.processStartId);
      if (ownerState !== 'dead' && ownerState !== 'reused') {
        return { ok: false, result: provisionFail(ownerState === 'match' || ownerState === 'pid-live'
          ? 'RELEASE_BUSY' : 'RELEASE_OWNER_UNPROVABLE', 'the prior release owner is not proven gone') };
      }
    }
  }
  var claimed = Object.assign({}, record, { status: 'releasing', owner: ownerIdentity(),
    updatedAt: new Date().toISOString() });
  claimed.recordHash = worktreeContract.recordHash(claimed);
  var held = casWorktreeRecord(item, claimed);
  return held ? { ok: true, item: held, startedStatus: record.status } :
    { ok: false, result: provisionFail('RELEASE_RECORD_CONFLICT',
      'another lifecycle owner changed the worktree record') };
}
function settleRelease(item, status, result) {
  var next = Object.assign({}, item.record, { status: status, updatedAt: new Date().toISOString() });
  next.recordHash = worktreeContract.recordHash(next);
  return casWorktreeRecord(item, next) ? result :
    provisionFail('RELEASE_RECORD_CONFLICT', 'the release refusal could not settle its exact record generation');
}
function ownedReleaseCommits(record, receipt) {
  var commits = Object.create(null);
  commits[record.baseCommit] = true;
  if (receipt) {
    commits[receipt.candidateCommit] = true;
    commits[receipt.expectedRefCommit] = true;
  }
  return commits;
}

// A reconnected filesystem may renumber both st_dev and inode values. Raw
// identity drift is accepted for RELEASE ONLY after re-deriving a stronger
// manager namespace + Git administrative binding. This never makes the
// generation resumable, and an arbitrary replacement directory, moved path,
// redirected .git pointer, foreign ref or foreign commit still fails closed.
function remountedReleaseIdentityProven(record, liveExecution, expectedCommit) {
  if (!record || !record.controlRoot || !record.gitCommonDirIdentity ||
      !record.executionRoot || !liveExecution || !/^[a-f0-9]{40}$/.test(String(expectedCommit || ''))) return false;
  var recorded = [record.controlRoot, record.gitCommonDirIdentity, record.executionRoot];
  var liveControl = identityOf(record.controlRoot.path);
  var liveCommon = identityOf(record.gitCommonDirIdentity.path);
  var current = [liveControl, liveCommon, liveExecution];
  if (current.some(function (identity, index) {
    return !identity || identity.path !== recorded[index].path;
  })) return false;

  var home;
  try { home = fs.realpathSync.native(paths.WORKTREE_HOME).normalize('NFC'); }
  catch (error) { return false; }
  var expectedPath = path.join(home,
    record.controlProjectId.slice('sha256:'.length, 'sha256:'.length + 16), record.worktreeId).normalize('NFC');
  if (record.executionRoot.path !== expectedPath || liveExecution.path !== expectedPath) return false;

  if (sameIdentity(liveExecution, record.executionRoot)) return false;
  var controlStable = sameIdentity(liveControl, record.controlRoot);
  var commonStable = sameIdentity(liveCommon, record.gitCommonDirIdentity);
  // The checkout may live on its own mounted volume while the control repo is
  // stable. If the repository volume also drifted, both of its authority
  // anchors must move coherently; a mixed control/common replacement is never
  // accepted as a remount.
  if (controlStable !== commonStable) return false;
  if (!controlStable && (record.controlRoot.dev !== record.gitCommonDirIdentity.dev ||
      liveControl.dev !== liveCommon.dev)) return false;

  var controlRepo = repositoryIdentity(record.controlRoot.path);
  var executionRepo = repositoryIdentity(record.executionRoot.path);
  if (!controlRepo.ok || !executionRepo.ok ||
      !sameIdentity(controlRepo.gitCommonDirIdentity, liveCommon) ||
      !sameIdentity(controlRepo.toplevel, liveControl) ||
      !sameIdentity(executionRepo.gitCommonDirIdentity, liveCommon) ||
      !sameIdentity(executionRepo.toplevel, liveExecution)) return false;
  var ownHead = runGit(['symbolic-ref', '-q', 'HEAD'], record.executionRoot.path);
  if (!ownHead.ok || ownHead.stdout.trim() !== record.candidateRef) return false;
  var ownCommit = runGit(['rev-parse', 'HEAD'], record.executionRoot.path);
  if (!ownCommit.ok || ownCommit.stdout.trim() !== expectedCommit) return false;
  var listed = runGit(['worktree', 'list', '--porcelain', '-z'], record.controlRoot.path);
  if (!listed.ok) return false;
  var entries = parseWorktreeList(listed.stdout);
  return entries.some(function (entry) {
    return entry.path === liveExecution.path && entry.branch === record.candidateRef &&
      entry.head === expectedCommit;
  });
}

function release(worktreeId) {
  var records = readWorktreeRecordsForMutation();
  if (records.unavailable || records.invalid.length) return provisionFail('RELEASE_RECORDS_UNSAFE', 'record store unreadable');
  var item = records.active.find(function (row) { return row.record.worktreeId === worktreeId; });
  if (!item) return provisionFail('RELEASE_RECORD_ABSENT', worktreeId + ' has no record');
  var record = item.record;
  if (record.status === 'released') return { ok: true };
  // §16: "execution A is incompatible with cleanup A". An app-run holds
  // `execution:<worktreeId>` for its whole build/install/launch, and its build
  // output is gitignored, so `git worktree remove` would NOT refuse — it would
  // delete the tree underneath a live Gradle/Xcode build. This is the ONE place
  // that proves it, so both the integration's release phase and the operator's
  // explicit `task-worktree.mjs release` are covered by a single check.
  var builderIssue = executionLeaseIssue(record.worktreeId);
  if (builderIssue) return provisionFail(builderIssue.code, builderIssue.message);
  var claimed = claimRelease(item);
  if (!claimed.ok) return claimed.result;
  item = claimed.item;
  record = item.record;
  releasesInFlight[worktreeId] = true;
  try {
    // Close the cached-binding race: an app-run that acquired just before our
    // claim is now visible. One that acquires after the claim must revalidate
    // against the non-materialized `releasing` record before touching the tree.
    builderIssue = executionLeaseIssue(record.worktreeId);
    if (builderIssue) return settleRelease(item, claimed.startedStatus,
      provisionFail(builderIssue.code, builderIssue.message));
    var releaseReceiptState = candidateReceiptState(record.worktreeId);
    if (releaseReceiptState.status === 'invalid' ||
        releaseReceiptState.status === 'valid' &&
        !receiptMatchesGeneration(record, releaseReceiptState.receipt)) {
      return settleRelease(item, 'recovery-required', provisionFail('RELEASE_RECEIPT_UNSAFE',
        'candidate receipt ownership is invalid or belongs to another generation'));
    }
    var releaseReceipt = releaseReceiptState.status === 'valid' ? releaseReceiptState.receipt : null;
    var ownedCommits = ownedReleaseCommits(record, releaseReceipt);
    var releaseMarkerRef = gitMutations.releaseMarkerRefFor(record.worktreeId);
    var markerPreflight = runGit(['rev-parse', '-q', '--verify', releaseMarkerRef], paths.PROJECT_ROOT);
    var refPreflight = runGit(['rev-parse', '-q', '--verify', record.candidateRef], paths.PROJECT_ROOT);
    var markerAbsent = !markerPreflight.ok && markerPreflight.code === 'GIT_EXIT_1';
    var refAbsent = !refPreflight.ok && refPreflight.code === 'GIT_EXIT_1';
    if ((!markerPreflight.ok && !markerAbsent) || (!refPreflight.ok && !refAbsent)) {
      return settleRelease(item, 'recovery-required', provisionFail('RELEASE_REF_STATE_UNSAFE',
        'candidate or release-marker state could not be read exactly'));
    }
    var expectedReleaseCommit = markerPreflight.ok ? markerPreflight.stdout.trim() :
      refPreflight.ok ? refPreflight.stdout.trim() : record.baseCommit;
    if (!ownedCommits[expectedReleaseCommit]) {
      return settleRelease(item, 'recovery-required', provisionFail('RELEASE_REF_FOREIGN',
        record.candidateRef + ' has no exact manager-owned release state'));
    }
    if (markerPreflight.ok && refPreflight.ok) {
      return settleRelease(item, 'recovery-required', provisionFail('RELEASE_REF_REAPPEARED',
        record.candidateRef + ' exists after its durable release marker'));
    }
    if (record.executionRoot !== null) {
      var live = identityOf(record.executionRoot.path);
      if (live && (sameIdentity(live, record.executionRoot) ||
          remountedReleaseIdentityProven(record, live, expectedReleaseCommit))) {
        var removed = gitMutations.removeOwnedWorktree({ targetPath: record.executionRoot.path });
        if (!removed.ok) return settleRelease(item, 'recovery-required', removed);
      } else if (fileExists(record.executionRoot.path)) {
        return settleRelease(item, 'recovery-required', provisionFail('RELEASE_PATH_REPLACED',
          record.executionRoot.path + ' no longer has the recorded filesystem identity'));
      }
    } else {
      var expectedPath = path.join(paths.WORKTREE_HOME,
        record.controlProjectId.slice('sha256:'.length, 'sha256:'.length + 16), record.worktreeId);
      if (fileExists(expectedPath)) {
        return settleRelease(item, 'recovery-required', provisionFail('RELEASE_PATH_UNPROVEN',
          expectedPath + ' exists but the recovery record does not own its filesystem identity'));
      }
    }
    var deleted = gitMutations.releaseOwnedRef({ worktreeId: record.worktreeId,
      candidateRef: record.candidateRef, expectedCommit: expectedReleaseCommit });
    if (!deleted.ok) return settleRelease(item, 'recovery-required', deleted);
    var released = Object.assign({}, record, { status: 'released', executionRoot: null,
      updatedAt: new Date().toISOString() });
    released.recordHash = worktreeContract.recordHash(released);
    if (!casWorktreeRecord(item, released)) {
      return provisionFail('RELEASE_RECORD_UNWRITABLE', 'released record could not be published');
    }
    pruneEmptyHome(record);
    return { ok: true };
  } finally {
    delete releasesInFlight[worktreeId];
  }
}

function recoverInterruptedReleases() {
  var records = readWorktreeRecordsForMutation();
  if (records.unavailable || records.invalid.length) {
    return { ok: false, recovered: [], blocked: [{ code: 'RELEASE_RECORDS_UNSAFE' }] };
  }
  var recovered = [], blocked = [];
  records.active.filter(function (row) { return row.record.status === 'releasing'; }).forEach(function (row) {
    var result = release(row.record.worktreeId);
    if (result.ok) recovered.push(row.record.worktreeId);
    else blocked.push({ worktreeId: row.record.worktreeId, code: result.code, message: result.message });
  });
  return { ok: blocked.length === 0, recovered: recovered, blocked: blocked };
}

// §7.1 step 3: after the last generation of a repository is gone, the manager's
// own <home>/<repoKey> directory is left behind for ever. Ownership-safe by
// construction: a bare rmdir removes it ONLY when it is empty, so a directory
// still holding another generation, or anything this manager did not create,
// survives untouched. Never recursive, and a failure is not an error — the
// release itself already succeeded.
function pruneEmptyHome(record) {
  if (record.executionRoot === null) return;
  var parent = path.dirname(record.executionRoot.path);
  var home;
  try { home = fs.realpathSync(paths.WORKTREE_HOME); } catch (error) { return; }
  var resolvedParent;
  try { resolvedParent = fs.realpathSync(parent); } catch (error) { return; }
  // Only ever inside our own home, and never the home itself.
  if (resolvedParent === home || resolvedParent.indexOf(home + path.sep) !== 0) return;
  try { fs.rmdirSync(resolvedParent); } catch (error) { /* not empty, or gone: both fine */ }
}

function receiptMatchesGeneration(record, receipt) {
  return !!receipt && receipt.worktreeId === record.worktreeId &&
    receipt.runId === record.runId && receipt.stem === record.stem &&
    receipt.candidateRef === record.candidateRef &&
    receipt.baseCommit === record.baseCommit && receipt.baseTree === record.baseTree &&
    receipt.inputs.taskSnapshotHash === record.taskSnapshotHash &&
    receipt.inputs.projectConfigHash === record.projectConfigHash &&
    receipt.inputs.dependencySnapshotHash === record.dependencySnapshotHash &&
    receipt.inputs.targetRef === record.targetRef &&
    receipt.inputs.targetCommit === record.baseCommit &&
    receipt.inputs.figmaGenerationHash === record.figmaGenerationHash &&
    receipt.inputs.apiGenerationHash === record.apiGenerationHash;
}
function candidateReceiptState(worktreeId) {
  var file = path.join(RECEIPTS_DIR, worktreeId + '.json');
  var inspected = fileGuards.inspectEntryUnder(paths.WORKTREE_RECORDS_AUTHORITY_ROOT, RECEIPTS_DIR, file);
  if (inspected && inspected.status === 'missing') return { status: 'absent', receipt: null };
  var bounded = fileGuards.boundedRegularFileUnder(paths.WORKTREE_RECORDS_AUTHORITY_ROOT,
    RECEIPTS_DIR, file, candidateContract.MAX_BYTES);
  if (!bounded) return { status: 'invalid', receipt: null };
  try {
    var receipt = candidateContract.validateBytes(bounded.bytes);
    if (receipt.worktreeId !== worktreeId) return { status: 'invalid', receipt: null };
    return gitMutations.verifyCandidateReceipt(receipt)
      ? { status: 'valid', receipt: receipt, bytes: bounded.bytes, proof: bounded.stat }
      : { status: 'invalid', receipt: null };
  } catch (error) { return { status: 'invalid', receipt: null }; }
}

var sealsInFlight = Object.create(null);
function claimSeal(item) {
  var record = item.record;
  if (record.status !== 'ready' && record.status !== 'sealing') {
    return { ok: false, result: provisionFail('SEAL_STATUS_INVALID',
      record.worktreeId + ' is ' + record.status + ', not ready or sealing') };
  }
  if (sealsInFlight[record.worktreeId]) {
    return { ok: false, result: provisionFail('SEAL_BUSY', record.worktreeId + ' already has a live sealing owner') };
  }
  if (record.status === 'sealing') {
    var sameProcess = record.owner.hostname === require('os').hostname() && record.owner.pid === process.pid;
    if (!sameProcess) {
      if (record.owner.hostname !== require('os').hostname()) {
        return { ok: false, result: provisionFail('SEAL_OWNER_UNPROVABLE',
          'the interrupted sealing owner belongs to another host') };
      }
      var ownerState = writerLeases.processIdentityState(record.owner.pid, record.owner.processStartId);
      if (ownerState !== 'dead' && ownerState !== 'reused') {
        return { ok: false, result: provisionFail(ownerState === 'match' || ownerState === 'pid-live'
          ? 'SEAL_BUSY' : 'SEAL_OWNER_UNPROVABLE', 'the prior sealing owner is not proven gone') };
      }
    }
  }
  var claimed = Object.assign({}, record, { status: 'sealing', owner: ownerIdentity(),
    updatedAt: new Date().toISOString() });
  claimed.recordHash = worktreeContract.recordHash(claimed);
  var held = casWorktreeRecord(item, claimed);
  return held ? { ok: true, item: held, startedStatus: record.status } :
    { ok: false, result: provisionFail('SEAL_RECORD_CONFLICT',
      'another lifecycle owner changed the worktree record') };
}
function transitionFromSeal(item, status) {
  var next = Object.assign({}, item.record, { status: status, updatedAt: new Date().toISOString() });
  next.recordHash = worktreeContract.recordHash(next);
  return casWorktreeRecord(item, next);
}
function settleSealFailure(item, status, result) {
  return transitionFromSeal(item, status) ? result :
    provisionFail('SEAL_RECORD_CONFLICT', 'the sealing refusal could not settle its exact record generation');
}

// §9.5 sealing is a small WAL: record(sealing) -> receipt(intent) -> ref(CAS)
// -> record(ready-for-integration). Every prefix is replayable from immutable
// Git bytes, and only one exact record generation may own the transition.
function seal(options) {
  var worktreeId = options && options.worktreeId;
  var records = readWorktreeRecordsForMutation();
  if (records.unavailable || records.invalid.length) return provisionFail('SEAL_RECORDS_UNSAFE', 'record store unreadable');
  var item = records.active.find(function (row) { return row.record.worktreeId === worktreeId; });
  if (!item) return provisionFail('SEAL_RECORD_ABSENT', String(worktreeId) + ' has no record');
  if (item.record.executionRoot === null) return provisionFail('SEAL_RECORD_ABSENT', 'the record has no execution root');

  var initialDrift = generationDriftIssue(item.record);
  if (initialDrift) {
    if (initialDrift.code === 'GENERATION_SUPERSEDED') {
      if (!markRevalidationRequired(item.record, initialDrift.message, item)) {
        return provisionFail('SEAL_RECORD_CONFLICT',
          'the drifted status could not claim its exact record generation');
      }
      return provisionFail(initialDrift.targetDrifted ? 'SEAL_TARGET_DRIFTED' : 'SEAL_INPUT_DRIFTED', initialDrift.message);
    }
    return provisionFail('SEAL_TARGET_UNRESOLVED', initialDrift.message);
  }
  var liveTarget = runGit(['rev-parse', '-q', '--verify', item.record.targetRef], paths.PROJECT_ROOT);

  var claimed = claimSeal(item);
  if (!claimed.ok) return claimed.result;
  item = claimed.item;
  var record = item.record;
  sealsInFlight[worktreeId] = true;
  try {
    var receiptState = candidateReceiptState(worktreeId);
    if (receiptState.status === 'invalid') {
      return settleSealFailure(item, 'recovery-required',
        provisionFail('SEAL_RECEIPT_INVALID', 'the existing candidate receipt is unsafe'));
    }
    var prior = receiptState.receipt;
    if (prior && !receiptMatchesGeneration(record, prior)) {
      return settleSealFailure(item, 'recovery-required',
        provisionFail('SEAL_RECEIPT_MISMATCH', 'the existing receipt belongs to another generation'));
    }
    var executionTree = gitMutations.executionTreeOf(record.executionRoot.path, record.baseTree);
    if (executionTree === null) return settleSealFailure(item, 'recovery-required',
      provisionFail('SEAL_TREE_UNAVAILABLE', 'the execution tree could not be computed'));
    var currentRef = runGit(['rev-parse', '-q', '--verify', record.candidateRef], paths.PROJECT_ROOT);
    if (!currentRef.ok) return settleSealFailure(item, 'recovery-required',
      provisionFail('SEAL_REF_ABSENT', record.candidateRef + ' is unreadable'));
    currentRef = currentRef.stdout.trim();

    var gate = taskCheckpointsModule().sealingGate(record.stem, record.runId, {
      worktreeId: record.worktreeId,
      baseCommit: record.baseCommit,
      baseTree: record.baseTree,
      executionTree: executionTree,
      targetRef: record.targetRef,
      targetCommit: liveTarget.stdout.trim()
    });
    if (!gate.ok) {
      // A normal failed/blocked child exit remains runnable. An interrupted
      // seal whose gate can no longer be proved is not runnable: classify its
      // receipt/ref prefix explicitly so release can clean it up. Leaving a
      // dead owner in `sealing` would make every future seal and release refuse.
      if (claimed.startedStatus === 'ready') {
        return settleSealFailure(item, 'ready', provisionFail(gate.code, gate.message));
      }
      return settleSealFailure(item, 'recovery-required',
        provisionFail(gate.code, gate.message));
    }

    var receipt = null;
    // An interrupted seal whose receipted tree is still the execution tree is
    // resumed exactly. If the tree differs, the existing receipt is the prior
    // completed generation and becomes the CAS precondition for a re-seal.
    if (claimed.startedStatus === 'sealing' && prior &&
        executionTree === prior.candidateTree &&
        (currentRef === prior.expectedRefCommit || currentRef === prior.candidateCommit)) {
      receipt = prior;
    } else {
      var expectedRefCommit = record.baseCommit;
      if (prior) {
        if (currentRef !== prior.candidateCommit) {
          return settleSealFailure(item, 'recovery-required', provisionFail('SEAL_REF_MOVED',
            record.candidateRef + ' no longer carries the prior receipted commit'));
        }
        expectedRefCommit = prior.candidateCommit;
      } else if (currentRef !== record.baseCommit) {
        return settleSealFailure(item, 'recovery-required', provisionFail('SEAL_REF_MOVED',
          record.candidateRef + ' moved without a manager receipt'));
      }
      var prepared = gitMutations.prepareCandidate({
        executionRoot: record.executionRoot.path, candidateRef: record.candidateRef,
        baseCommit: record.baseCommit, baseTree: record.baseTree,
        expectedRefCommit: expectedRefCommit, stem: record.stem
      });
      if (!prepared.ok) {
        return settleSealFailure(item, prepared.code === 'SEAL_REF_MOVED'
          ? 'recovery-required' : 'ready', prepared);
      }
      var now = new Date().toISOString();
      receipt = {
        version: 1, worktreeId: record.worktreeId, runId: record.runId, stem: record.stem,
        candidateRef: record.candidateRef,
        baseCommit: record.baseCommit, baseTree: record.baseTree,
        expectedRefCommit: prepared.expectedRefCommit,
        candidateCommit: prepared.candidateCommit, candidateTree: prepared.candidateTree,
        entries: prepared.entries,
        diffHash: candidateContract.diffHashOf(prepared.entries),
        inputs: {
          taskSnapshotHash: record.taskSnapshotHash,
          projectConfigHash: record.projectConfigHash,
          dependencySnapshotHash: record.dependencySnapshotHash,
          targetRef: record.targetRef,
          targetCommit: record.baseCommit,
          figmaGenerationHash: record.figmaGenerationHash,
          apiGenerationHash: record.apiGenerationHash
        },
        sealedAt: now, owner: ownerIdentity(),
        receiptHash: 'sha256:' + '0'.repeat(64)
      };
      receipt.receiptHash = candidateContract.receiptHash(receipt);
      try { candidateContract.validate(receipt); }
      catch (error) {
        return settleSealFailure(item, 'recovery-required',
          provisionFail('SEAL_RECEIPT_INVALID', error.message));
      }
      var receiptBytes = jsonBytes(receipt);
      var receiptFile = path.join(RECEIPTS_DIR, record.worktreeId + '.json');
      // The first receipt is no-clobber; a re-seal replaces only the exact
      // previously verified receipt generation. A byte-identical or invalid
      // inode that appears during candidate preparation is foreign and must
      // survive untouched.
      var receiptPublished = prior
        ? fileGuards.compareAndSwapRegularFileUnder(
          paths.WORKTREE_RECORDS_AUTHORITY_ROOT, RECEIPTS_DIR, receiptFile,
          candidateContract.MAX_BYTES,
          { proof: receiptState.proof, bytes: receiptState.bytes }, receiptBytes,
          { mode: 0o600, allowLargePayload: true })
        : fileGuards.publishNoClobberRegularFileUnder(
          paths.WORKTREE_RECORDS_AUTHORITY_ROOT, RECEIPTS_DIR, receiptFile, receiptBytes,
          { create: true, directoryMode: 0o700, mode: 0o600,
            maxBytes: candidateContract.MAX_BYTES, allowLargePayload: true });
      if (!receiptPublished || !receiptPublished.ok) {
        return settleSealFailure(item, 'recovery-required',
          provisionFail('SEAL_RECEIPT_CONFLICT',
            'candidate receipt generation changed before durable publication'));
      }
      var reread = candidateReceiptState(worktreeId);
      if (reread.status !== 'valid' || reread.receipt.receiptHash !== receipt.receiptHash) {
        return settleSealFailure(item, 'recovery-required',
          provisionFail('SEAL_RECEIPT_UNVERIFIED', 'candidate receipt publication could not be re-proven'));
      }
      receipt = reread.receipt;
    }

    // Re-prove inputs and the ship gate after the potentially long hook/tree/
    // commit preparation, immediately before the receipted ref effect. This is
    // the second side of the seal TOCTOU fence: config/task/Figma/API changes
    // during preparation cannot be advertised as a green candidate.
    var lateExecutionTree = gitMutations.executionTreeOf(record.executionRoot.path, record.baseTree);
    if (lateExecutionTree === null) {
      return settleSealFailure(item, 'recovery-required',
        provisionFail('SEAL_TREE_UNAVAILABLE', 'the execution tree could not be re-proven'));
    }
    if (lateExecutionTree !== receipt.candidateTree) {
      return settleSealFailure(item, 'revalidation-required',
        provisionFail('SEAL_EXECUTION_DRIFTED',
          'the execution bytes changed after candidate preparation; gates must run again'));
    }
    var lateDrift = generationDriftIssue(record);
    if (lateDrift) {
      return settleSealFailure(item, lateDrift.code === 'GENERATION_SUPERSEDED'
        ? 'revalidation-required' : 'recovery-required',
      provisionFail('SEAL_INPUT_DRIFTED', lateDrift.message));
    }
    var lateGate = taskCheckpointsModule().sealingGate(record.stem, record.runId, {
      worktreeId: record.worktreeId,
      baseCommit: record.baseCommit,
      baseTree: record.baseTree,
      executionTree: lateExecutionTree,
      targetRef: record.targetRef,
      targetCommit: record.baseCommit
    });
    if (!lateGate.ok) {
      return settleSealFailure(item, 'revalidation-required',
        provisionFail(lateGate.code, lateGate.message));
    }

    // A gate is allowed to run arbitrary project checks. Re-prove both the
    // checkout and every generation pin after it returns: a check that writes
    // tracked bytes, or an input refresh racing the check, must never make the
    // already-prepared candidate look current. This is the final fence before
    // the immutable ref CAS; no child-writable operation occurs after it.
    var finalExecutionTree = gitMutations.executionTreeOf(record.executionRoot.path, record.baseTree);
    if (finalExecutionTree === null) {
      return settleSealFailure(item, 'recovery-required',
        provisionFail('SEAL_TREE_UNAVAILABLE', 'the execution tree could not be proven after the final gate'));
    }
    if (finalExecutionTree !== receipt.candidateTree) {
      return settleSealFailure(item, 'revalidation-required',
        provisionFail('SEAL_EXECUTION_DRIFTED',
          'the execution bytes changed during the final gate; gates must run again'));
    }
    var finalDrift = generationDriftIssue(record);
    if (finalDrift) {
      return settleSealFailure(item, finalDrift.code === 'GENERATION_SUPERSEDED'
        ? 'revalidation-required' : 'recovery-required',
      provisionFail('SEAL_INPUT_DRIFTED', finalDrift.message));
    }

    var published = gitMutations.publishCandidate({
      executionRoot: record.executionRoot.path, candidateRef: record.candidateRef,
      expectedRefCommit: receipt.expectedRefCommit,
      candidateCommit: receipt.candidateCommit, candidateTree: receipt.candidateTree
    });
    if (!published.ok) {
      return settleSealFailure(item, 'recovery-required', published);
    }
    var ready = transitionFromSeal(item, 'ready-for-integration');
    if (!ready) return provisionFail('SEAL_RECORD_CONFLICT', 'the sealed status could not be published by CAS');
    return { ok: true, worktreeId: record.worktreeId, candidateCommit: receipt.candidateCommit,
      candidateTree: receipt.candidateTree, diffHash: receipt.diffHash,
      entries: receipt.entries.length, receiptHash: receipt.receiptHash };
  } finally {
    delete sealsInFlight[worktreeId];
  }
}

function recoverInterruptedSeals() {
  var records = readWorktreeRecordsForMutation();
  if (records.unavailable || records.invalid.length) {
    return { ok: false, recovered: [], blocked: [{ code: 'SEAL_RECORDS_UNSAFE' }] };
  }
  var recovered = [], blocked = [];
  records.active.filter(function (row) { return row.record.status === 'sealing'; }).forEach(function (row) {
    var result = seal({ worktreeId: row.record.worktreeId, recovery: true });
    if (result.ok) recovered.push(row.record.worktreeId);
    else blocked.push({ worktreeId: row.record.worktreeId, code: result.code, message: result.message });
  });
  return { ok: blocked.length === 0, recovered: recovered, blocked: blocked };
}

// The ONE read of the ownership authority for a single stem. §7 makes the
// record store the only source of truth about who owns what, so "unreadable"
// and "nothing here" must never collapse into the same answer: a caller that
// cannot tell them apart presumes free space and acts on it — the exact
// fail-open this module's header forbids. Every accessor below is built on
// this, and every consumer must treat `ok:false` as a refusal.
//   { ok: true,  record }        proven: the stem's materialized generation
//   { ok: true,  record: null }  proven: the stem has no materialized generation
//   { ok: false, code, message } unprovable: refuse, never presume
function materializedRecordFor(stem) {
  // A non-canonical stem provably owns nothing; that is an answer, not a doubt.
  if (!worktreeContract.STEM_RE.test(String(stem || ''))) return { ok: true, record: null };
  var records = readRecords(paths.WORKTREE_RECORDS_DIR, paths.WORKTREE_RECORDS_AUTHORITY_ROOT,
    worktreeContract, 'WORKTREE_RECORD');
  if (records.unavailable || records.invalid.length) {
    return { ok: false, code: 'WORKTREE_RECORDS_UNSAFE',
      message: 'the worktree record store is unreadable or holds invalid records' };
  }
  var matches = records.active.filter(function (row) {
    return row.record.stem === stem && worktreeContract.CHECKOUT_OWNING_STATUSES.has(row.record.status);
  });
  if (matches.length > 1) return { ok: false, code: 'WORKTREE_GENERATION_AMBIGUOUS',
    message: stem + ' has more than one checkout-owning worktree generation' };
  return { ok: true, record: matches.length === 1 &&
    worktreeContract.MATERIALIZED_STATUSES.has(matches[0].record.status) ? matches[0].record : null };
}

function releasableRecordFor(stem) {
  if (!worktreeContract.STEM_RE.test(String(stem || ''))) return { ok: true, record: null };
  var records = readRecords(paths.WORKTREE_RECORDS_DIR, paths.WORKTREE_RECORDS_AUTHORITY_ROOT,
    worktreeContract, 'WORKTREE_RECORD');
  if (records.unavailable || records.invalid.length) {
    return { ok: false, code: 'WORKTREE_RECORDS_UNSAFE',
      message: 'the worktree record store is unreadable or holds invalid records' };
  }
  var matches = records.active.filter(function (row) {
    return row.record.stem === stem && worktreeContract.CHECKOUT_OWNING_STATUSES.has(row.record.status);
  });
  if (matches.length > 1) return { ok: false, code: 'WORKTREE_GENERATION_AMBIGUOUS',
    message: stem + ' has more than one checkout-owning worktree generation' };
  return { ok: true, record: matches.length === 1 &&
    worktreeContract.RELEASABLE_STATUSES.has(matches[0].record.status) ? matches[0].record : null };
}

// The live execution pin for a stem (§6.13): the active generation, its sealed
// base, the CURRENT tree of its checkout and the live target revision. A phase
// receipt carrying this pin is invalid the moment the tree or the target
// moves — which is what makes gates re-run instead of carrying over.
//   { ok: true,  pin }        proven
//   { ok: true,  pin: null }  proven: no materialized generation for this stem
//   { ok: false, code }       unprovable: the caller must refuse, because a
//                             receipt written without a pin is exempt from the
//                             §6.13 gate FOREVER.
function executionPinFor(stem, runId) {
  if (!worktreeContract.RUN_ID_RE.test(String(runId || ''))) {
    return { ok: false, code: 'WORKTREE_RUN_INVALID',
      message: 'execution-pin lookup requires one canonical run generation' };
  }
  var got = materializedRecordFor(stem);
  if (!got.ok) return got;
  var record = got.record;
  if (!record || record.executionRoot === null) return { ok: true, pin: null };
  if (record.runId !== runId) return { ok: false, code: 'WORKTREE_RUN_MISMATCH',
    message: stem + ' materialized generation belongs to another runId' };
  // The tree comes from the single git owner (throwaway index; this module
  // runs no object-writing verb itself).
  var executionTree = gitMutations.executionTreeOf(record.executionRoot.path, record.baseTree);
  if (executionTree === null) {
    return { ok: false, code: 'EXECUTION_TREE_UNAVAILABLE',
      message: 'the execution tree of ' + record.worktreeId + ' could not be computed' };
  }
  var target = runGit(['rev-parse', '-q', '--verify', record.targetRef], paths.PROJECT_ROOT);
  if (!target.ok) {
    return { ok: false, code: 'TARGET_REF_UNAVAILABLE',
      message: record.targetRef + ' could not be resolved' };
  }
  return { ok: true, pin: {
    worktreeId: record.worktreeId,
    baseCommit: record.baseCommit,
    baseTree: record.baseTree,
    executionTree: executionTree,
    targetRef: record.targetRef,
    targetCommit: target.stdout.trim()
  } };
}


// §9.7, the operator's release. A run that ends without reaching Integrate
// leaves its generation sealed, and provision() refuses every later run for the
// stem — so without a surface here the request bounces forever and the only way
// out is a shell. This is that surface, and it proves as much as the CLI does:
// the stem must own a materialized generation, the transaction must not be
// mid-flight, and release() itself still refuses under a live build lease or a
// candidate ref this owner did not create.
function releaseFor(stem) {
  var got = releasableRecordFor(stem);
  if (!got.ok) return { ok: false, code: got.code, message: got.message };
  if (!got.record) {
    return { ok: false, code: 'RELEASE_RECORD_ABSENT', message: stem + ' has no releasable generation' };
  }
  var integration = integrationsModule().readOne(stem);
  if (integration.ok && (integration.record.status === 'active' ||
      integration.record.status === 'recovery-required')) {
    // The transaction owns this generation. Releasing it underneath a WAL would
    // destroy the tree its own recovery reads.
    return { ok: false, code: 'RELEASE_INTEGRATION_ACTIVE',
      message: stem + ' is inside an integration transaction (' + integration.record.status + ')' };
  }
  var released = release(got.record.worktreeId);
  if (!released.ok) return released;
  return { ok: true, stem: stem, worktreeId: got.record.worktreeId };
}

// §20 Phase 6: the five figures the plan requires before the concurrency cap
// is raised. Everything here is DERIVED from records this pipeline already
// writes durably — nothing new is instrumented, so the numbers cannot drift
// from the state they describe. Bounded and read-only.
function metrics() {
  var records = readRecords(paths.WORKTREE_RECORDS_DIR, paths.WORKTREE_RECORDS_AUTHORITY_ROOT,
    worktreeContract, 'WORKTREE_RECORD');
  var integrations = readRecords(paths.INTEGRATIONS_DIR, paths.WORKTREE_RECORDS_AUTHORITY_ROOT,
    integrationContract, 'INTEGRATION_RECORD');
  var out = {
    version: 1,
    unavailable: records.unavailable || integrations.unavailable || null,
    generations: { total: 0, materialized: 0, released: 0, recoveryRequired: 0, revalidationRequired: 0 },
    provisioning: { measured: 0, medianMs: null, maxMs: null, truncated: false },
    disk: { freeBytes: null, floorBytes: DEFAULT_MIN_DISK_BYTES, belowFloor: null },
    integrations: { total: 0, active: 0, completed: 0, recoveryRequired: 0 },
    // Records that exist but could not be validated. They are counted, never
    // dropped: these figures decide whether the concurrency cap is raised, and
    // a rate computed over only the healthy records reads as "nothing is wrong"
    // in exactly the situation where something is.
    invalid: { generations: 0, integrations: 0 },
    rates: { revalidation: null, integrationRecovery: null }
  };
  out.invalid.generations = records.invalid.length;
  out.invalid.integrations = integrations.invalid.length;
  if (out.unavailable) return out;

  records.active.forEach(function (item) {
    var status = item.record.status;
    out.generations.total += 1;
    if (worktreeContract.MATERIALIZED_STATUSES.has(status)) out.generations.materialized += 1;
    if (status === 'released') out.generations.released += 1;
    if (status === 'recovery-required') out.generations.recoveryRequired += 1;
    if (status === 'revalidation-required') out.generations.revalidationRequired += 1;
  });
  integrations.active.forEach(function (item) {
    out.integrations.total += 1;
    if (item.record.status === 'active') out.integrations.active += 1;
    if (item.record.status === 'completed') out.integrations.completed += 1;
    if (item.record.status === 'recovery-required') out.integrations.recoveryRequired += 1;
  });
  // Denominators include the invalid records: an unreadable generation is one
  // whose outcome we do not know, which is worse than a known bad one.
  var generationDenominator = out.generations.total + out.invalid.generations;
  var integrationDenominator = out.integrations.total + out.invalid.integrations;
  if (generationDenominator) {
    out.rates.revalidation = out.generations.revalidationRequired / generationDenominator;
  }
  if (integrationDenominator) {
    out.rates.integrationRecovery = out.integrations.recoveryRequired / integrationDenominator;
  }
  // Provisioning latency comes from the mutation owner's own intent/outcome
  // receipts — the only place a start and an end are both recorded.
  var latencies = provisioningLatencies();
  if (latencies.truncated) out.provisioning.truncated = true;
  if (latencies.length) {
    latencies.sort(function (a, b) { return a - b; });
    out.provisioning.measured = latencies.length;
    out.provisioning.medianMs = latencies[Math.floor((latencies.length - 1) / 2)];
    out.provisioning.maxMs = latencies[latencies.length - 1];
  }
  try {
    var stat = fs.statfsSync(paths.PROJECT_ROOT);
    var free = Number(stat.bavail) * Number(stat.bsize);
    if (Number.isFinite(free)) {
      out.disk.freeBytes = free;
      out.disk.belowFloor = free < DEFAULT_MIN_DISK_BYTES;
    }
  } catch (error) { /* reported as null, never guessed */ }
  return out;
}

// Pair each add-worktree intent receipt with its outcome by candidate ref.
function provisioningLatencies() {
  var dir = path.join(paths.WORKTREE_RECORDS_DIR, '.mutations');
  var listed = fileGuards.boundedDirectoryNamesUnder(paths.WORKTREE_RECORDS_AUTHORITY_ROOT, dir, MAX_RECORD_FILES);
  if (!listed || !listed.ok) {
    // The receipt directory only grows, so it WILL pass the scan bound. Report
    // that the sample is unavailable rather than presenting zero measurements
    // as "provisioning was never slow".
    var empty = [];
    empty.truncated = true;
    return empty;
  }
  var intents = Object.create(null);
  var out = [];
  listed.names.filter(function (name) { return /add-worktree-(intent|done)\.json$/.test(name); }).sort()
    .forEach(function (name) {
      var bounded = fileGuards.boundedRegularFileUnder(paths.WORKTREE_RECORDS_AUTHORITY_ROOT, dir,
        path.join(dir, name), 64 * 1024);
      if (!bounded) return;
      var receipt;
      try { receipt = JSON.parse(bounded.bytes.toString('utf8')); } catch (error) { return; }
      var ref = receipt && receipt.payload && receipt.payload.candidateRef;
      var at = receipt && Date.parse(receipt.at);
      if (typeof ref !== 'string' || !Number.isFinite(at)) return;
      if (receipt.phase === 'intent') { intents[ref] = at; return; }
      if (receipt.phase === 'done' && Number.isFinite(intents[ref])) {
        var elapsed = at - intents[ref];
        if (elapsed >= 0 && elapsed < 60 * 60 * 1000) out.push(elapsed);
        delete intents[ref];
      }
    });
  return out;
}

// The execution binding a job for this stem must run against: the isolated
// checkout carrying the candidate, or null when the stem has no materialized
// generation. A null is NOT a fallback to the control root — the caller decides
// what an unbound job means, and for a task-bound build it means refusing.
function executionBindingFor(stem) {
  var got = materializedRecordFor(stem);
  if (!got.ok) return got;
  var record = got.record;
  if (!record || record.executionRoot === null) return { ok: true, binding: null };
  return { ok: true, binding: {
    worktreeId: record.worktreeId, executionRoot: record.executionRoot.path,
    runId: record.runId, baseCommit: record.baseCommit, targetRef: record.targetRef,
    projectConfigHash: record.projectConfigHash,
    figmaGenerationHash: record.figmaGenerationHash,
    apiGenerationHash: record.apiGenerationHash } };
}

// Session boundary resolver. Callers may name only the opaque generation;
// paths, manifest locations and snapshot locations are re-derived from the
// exact manager-owned record and re-proven immediately before spawn/send.
function sessionExecutionContext(options) {
  if (!options || typeof options !== 'object' || Array.isArray(options) ||
      Object.keys(options).sort().join('\0') !== ['runId', 'sourceRevision', 'stem', 'worktreeId'].join('\0') ||
      !worktreeContract.WORKTREE_ID_RE.test(String(options.worktreeId || '')) ||
      !worktreeContract.RUN_ID_RE.test(String(options.runId || '')) ||
      !worktreeContract.STEM_RE.test(String(options.stem || '')) ||
      !SOURCE_REVISION_RE.test(String(options.sourceRevision || ''))) {
    return provisionFail('SESSION_EXECUTION_BINDING_INVALID', 'session generation identity is not canonical');
  }
  var item = readWorktreeItem(options.worktreeId);
  if (!item || item.record.status !== 'ready' ||
      item.record.worktreeId !== options.worktreeId || item.record.runId !== options.runId ||
      item.record.stem !== options.stem || item.record.taskSourceRevision !== options.sourceRevision) {
    return provisionFail('SESSION_EXECUTION_BINDING_MISMATCH',
      'session generation does not match one exact ready worktree record');
  }
  var context = resumeReady(item.record, options.sourceRevision);
  if (!context) return provisionFail('SESSION_EXECUTION_BINDING_UNPROVEN',
    'session generation identities, manifest, snapshot, branch, or target could not be re-proven');
  return { ok: true, context: context };
}

var EXECUTION_ENVIRONMENT_FIELDS = [
  'ORCHESTRATOR_EXECUTION_MANIFEST', 'ORCHESTRATOR_EXECUTION_ROOT',
  'ORCHESTRATOR_PROJECT_ROOT', 'ORCHESTRATOR_RUN_ID',
  'ORCHESTRATOR_TASK_SNAPSHOT_FILE', 'ORCHESTRATOR_TASK_SNAPSHOT_HASH',
  'ORCHESTRATOR_WORKTREE_ID', 'ORCHESTRATOR_WRITER_SESSION_ID',
  'ORCHESTRATOR_WRITER_STEM'
];

// Resolve task-facing execution helpers from evidence, not from path knobs.
// The session boundary already publishes every value below, but a child can
// accidentally carry a stale shell variable into a later helper invocation.
// Re-prove the manager record/manifest/snapshot/git binding and the exact live
// task-lock generation before exposing either root or the canonical target
// branch to Figma/API tooling.
function executionEnvironmentContext(environment) {
  var env = environment && typeof environment === 'object' ? environment : {};
  var present = EXECUTION_ENVIRONMENT_FIELDS.filter(function (key) {
    return typeof env[key] === 'string' && env[key].length > 0;
  });
  if (present.length === 0) return { ok: true, context: null };
  if (present.length !== EXECUTION_ENVIRONMENT_FIELDS.length) {
    return provisionFail('EXECUTION_ENVIRONMENT_INCOMPLETE',
      'execution helpers require one complete manager-issued environment binding');
  }
  var worktreeId = env.ORCHESTRATOR_WORKTREE_ID;
  var runId = env.ORCHESTRATOR_RUN_ID;
  var stem = env.ORCHESTRATOR_WRITER_STEM;
  if (!worktreeContract.WORKTREE_ID_RE.test(worktreeId) ||
      !worktreeContract.RUN_ID_RE.test(runId) ||
      !worktreeContract.STEM_RE.test(stem)) {
    return provisionFail('EXECUTION_ENVIRONMENT_MISMATCH',
      'execution helper generation identifiers are not canonical');
  }
  var item = readWorktreeItem(worktreeId);
  if (!item || item.record.status !== 'ready' || item.record.runId !== runId ||
      item.record.stem !== stem || item.record.executionRoot === null) {
    return provisionFail('EXECUTION_ENVIRONMENT_UNPROVEN',
      'execution helper generation does not name one exact ready worktree record');
  }
  var context = resumeReady(item.record, item.record.taskSourceRevision);
  var manifest = context ? readExecutionManifest(item.record) : null;
  if (!context || !manifest) {
    return provisionFail('EXECUTION_ENVIRONMENT_UNPROVEN',
      'execution helper record, manifest, snapshot, repository, or target proof failed');
  }
  var suppliedControl = identityOf(env.ORCHESTRATOR_PROJECT_ROOT);
  var configuredControl = identityOf(paths.PROJECT_ROOT);
  var suppliedExecution = identityOf(env.ORCHESTRATOR_EXECUTION_ROOT);
  var exactValues = env.ORCHESTRATOR_EXECUTION_MANIFEST === context.manifestFile &&
    env.ORCHESTRATOR_TASK_SNAPSHOT_FILE === context.taskSnapshotFile &&
    env.ORCHESTRATOR_TASK_SNAPSHOT_HASH === context.taskSnapshotHash &&
    env.ORCHESTRATOR_EXECUTION_ROOT === context.executionRoot;
  if (!exactValues || !suppliedControl || !configuredControl || !suppliedExecution ||
      !sameIdentity(suppliedControl, item.record.controlRoot) ||
      !sameIdentity(configuredControl, item.record.controlRoot) ||
      !sameIdentity(suppliedExecution, item.record.executionRoot)) {
    return provisionFail('EXECUTION_ENVIRONMENT_MISMATCH',
      'execution helper paths or content pins differ from the manager-owned generation');
  }
  var lock = require('./locks').lockOwnedBySession(stem,
    env.ORCHESTRATOR_WRITER_SESSION_ID);
  if (!lock || lock.owned !== true || lock.stage !== 'orchestrator' ||
      lock.runId !== runId) {
    return provisionFail('EXECUTION_TASK_LOCK_UNPROVEN',
      'execution helper has no exact live orchestrator task-lock generation');
  }
  return { ok: true, context: Object.assign({}, context, {
    controlRoot: item.record.controlRoot.path,
    targetRef: item.record.targetRef,
    candidateRef: item.record.candidateRef,
    manifestHash: manifest.manifestHash,
    projectConfigHash: item.record.projectConfigHash,
    figmaGenerationHash: item.record.figmaGenerationHash,
    apiGenerationHash: item.record.apiGenerationHash
  }) };
}

// Every stem whose generation is sealed and awaiting the owner's Integrate,
// plus the ones whose target moved and therefore need a fresh run. Cheap: it
// reads the record store only, no git.
function readyForIntegration() {
  var records = readRecords(paths.WORKTREE_RECORDS_DIR, paths.WORKTREE_RECORDS_AUTHORITY_ROOT,
    worktreeContract, 'WORKTREE_RECORD');
  if (records.unavailable || records.invalid.length) return [];
  return records.active.filter(function (row) {
    return row.record.status === 'ready-for-integration' || row.record.status === 'revalidation-required';
  }).map(function (row) {
    return { stem: row.record.stem, worktreeId: row.record.worktreeId, runId: row.record.runId,
      status: row.record.status, targetRef: row.record.targetRef };
  });
}

// Read-only projection of a sealed candidate receipt.
function candidateReceipt(worktreeId) {
  if (!worktreeContract.WORKTREE_ID_RE.test(String(worktreeId || ''))) return null;
  var state = candidateReceiptState(worktreeId);
  return state.status === 'valid' ? state.receipt : null;
}

module.exports = {
  DEFAULT_MIN_DISK_BYTES: DEFAULT_MIN_DISK_BYTES,
  seal: seal,
  recoverInterruptedReleases: recoverInterruptedReleases,
  recoverInterruptedSeals: recoverInterruptedSeals,
  activeRecordFor: materializedRecordFor,
  executionBindingFor: executionBindingFor,
  sessionExecutionContext: sessionExecutionContext,
  executionEnvironmentContext: executionEnvironmentContext,
  metrics: metrics,
  readyForIntegration: readyForIntegration,
  candidateReceipt: candidateReceipt,
  executionPinFor: executionPinFor,
  MANAGER_NAMESPACE_PREFIX: MANAGER_NAMESPACE_PREFIX,
  repositoryIdentity: repositoryIdentity,
  environmentPrechecks: environmentPrechecks,
  parseWorktreeList: parseWorktreeList,
  discover: discover,
  provision: provision,
  release: release,
  releaseFor: releaseFor,
  revalidationIssueFor: revalidationIssueFor
};
