'use strict';

// ---------------------------------------------------------------------------
// Read-only git working-tree observation. Task Details uses this bounded view
// to surface tracked working-tree changes while a task is active or stopped.
// sessions.js also uses `enforcementWiring()` below to observe whether the
// pre-commit verify-done net is wired.
//
// GOLDEN INVARIANT: the server only OBSERVES — it never mutates the tree.
// Every command here is read-only (`git status`, `git rev-parse`,
// `git config --get`). There is no reset / stash / checkout / clean — and no
// `git config` WRITE: an unwired enforcement net is refused + reported, never
// silently self-healed. This module must never gain a mutating git call.
//
// Like the rest of the server's process spawns (claude, npm), git is invoked
// with spawnSync + a short timeout. The changed-tree projection calls it only
// from the lazy Artifacts endpoint; it is not part of deriveState().
// ---------------------------------------------------------------------------

var cp    = require('child_process');
var paths = require('./paths');

var PROJECT_ROOT = paths.PROJECT_ROOT;
var MAX_FILES    = 100;   // cap the payload — the user reviews, doesn't need the full list
var TIMEOUT_MS   = 5000;

// Run a read-only git command in the project root. Returns stdout on exit 0,
// or null on any failure (not a repo, git missing, timeout, non-zero exit).
function runReadOnly(args) {
  try {
    var r = cp.spawnSync('git', args, { cwd: PROJECT_ROOT, encoding: 'utf8', timeout: TIMEOUT_MS });
    if (!r || r.status !== 0) return null;
    return r.stdout || '';
  } catch (e) {
    return null;
  }
}

// { available:false }                                   — not a git repo / git unavailable
// { available:true, branch, count, truncated, files[] } — files: { status, path }
function statusSummary() {
  // --untracked-files=no: this projection is about tracked changes left by an
  // active/stopped run. NUL records avoid quote/unescape ambiguity for spaces
  // and non-ASCII names. Rename/copy records carry the destination first and
  // one additional NUL-delimited source path, which is intentionally skipped.
  var porcelain = runReadOnly(['status', '--porcelain=v1', '-z', '--untracked-files=no']);
  if (porcelain === null || porcelain === undefined) return { available: false };

  var branchRaw = runReadOnly(['rev-parse', '--abbrev-ref', 'HEAD']);
  var branch = branchRaw != null ? branchRaw.trim() : null;

  var files = [];
  var records = porcelain.split('\0');
  for (var i = 0; i < records.length; i++) {
    var record = records[i];
    if (!record) continue;
    var code = record.slice(0, 2);
    var filePath = record.slice(3);
    if (!filePath) continue;
    files.push({ status: code.trim() || '?', path: filePath });
    if (/[RC]/.test(code)) i += 1;
  }
  var total = files.length;
  return {
    available: true,
    branch: branch,
    count: total,
    truncated: total > MAX_FILES,
    files: files.slice(0, MAX_FILES)
  };
}

// Screenshot-gate enforcement wiring — read-only probe of the local net.
// The pre-commit verify-done hook only runs when git resolves core.hooksPath to
// the tracked hooks dir; unwired, a bare `git mv` ships an uncompared UI task
// (exactly how a vendored product shipped hand-moved done files past the gate).
// The probe shape mirrors figma/scripts/doctor.mjs (enforcementWiringFindings
// inputs) and skills/install-skills.sh — keep the three in sync.
//
// Cached (TTL below): consumed on the deriveState hot path and per run-start;
// a spawnSync per SSE poll would violate this module's "off any hot path" rule.
// The TTL also means wiring the hook is picked up within seconds, no restart.
var HOOKS_PATH_EXPECTED = 'orchestrator/skills/checks/hooks';
var WIRING_TTL_MS = 15 * 1000;
var wiringCache = null;   // { at, value }

function probeEnforcementWiring() {
  var inGit = runReadOnly(['rev-parse', '--is-inside-work-tree']) !== null;
  var hooksPath = '';
  if (inGit) {
    // `git config --get` exits 1 when the key is unset → runReadOnly returns
    // null → treated as '' (UNSET), matching doctor.mjs's read.
    var out = runReadOnly(['config', '--get', 'core.hooksPath']);
    hooksPath = out === null ? '' : out.trim();
  }
  return {
    inGit: inGit,
    hooksPath: hooksPath,
    expected: HOOKS_PATH_EXPECTED,
    wired: inGit && hooksPath === HOOKS_PATH_EXPECTED
  };
}

function enforcementWiring() {
  var now = Date.now();
  if (wiringCache && (now - wiringCache.at) < WIRING_TTL_MS) return wiringCache.value;
  var value = probeEnforcementWiring();
  wiringCache = { at: now, value: value };
  return value;
}

module.exports = {
  statusSummary: statusSummary,
  enforcementWiring: enforcementWiring
};
