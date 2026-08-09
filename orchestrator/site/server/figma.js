'use strict';

// ---------------------------------------------------------------------------
// Figma MCP connector readiness — PER-PROJECT account binding.
//
// The official Figma remote MCP (https://mcp.figma.com/mcp) is OAuth-ONLY: it
// rejects Personal Access Tokens (X-Figma-Token → 401). So the ONLY way to bind
// the MCP to a specific Figma account is OAuth, and the only way to make that
// binding per-project (a different account per project) is a LOCAL-scoped MCP
// server entry, authenticated independently through the interactive `/mcp` flow.
//
// This module therefore tracks TWO things from `claude mcp list` (run at the
// project root, so local-scoped servers for this project are included):
//   - local:  THIS project's own server, added by us under the name "figma"
//             (scope "Local config", stored in ~/.claude.json under the project
//             path). Its auth state is what the panel cares about.
//   - global: any OTHER figma.com connector — typically the account-level
//             managed "claude.ai Figma" connector, which appears in EVERY
//             project. When a global connector and a local one are both present
//             they compete (both sets of tools load — there is no URL dedupe),
//             so the panel warns and offers the removal command.
//
// WHICH account a connector is authenticated as is NOT in `claude mcp list`; it
// is read by calling the Figma MCP `whoami` tool. Per the golden invariant the
// server never calls Figma itself — a spawned `claude` session (key
// "figma:whoami") writes orchestrator/figma/.account.json, and account() just reads it.
//
// The probe is ASYNCHRONOUS (cp.spawn, not spawnSync): `claude mcp list`
// health-checks every connector and takes a few seconds, and the single Node
// event loop must not block on it. A background interval refreshes the cached
// snapshot; status() and the API read the cache instantly; invalidate() kicks an
// immediate re-probe.
// ---------------------------------------------------------------------------

var cp    = require('child_process');
var crypto = require('crypto');
var fs    = require('fs');
var path  = require('path');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var childEnv = require('./child-env').childEnv;
var generation = require('./figma-generation');

var PROJECT_ROOT = paths.PROJECT_ROOT;

// The name we always give this project's local MCP server. addLocalServer()
// adds it under this name; the probe keys "local vs global" off it.
var LOCAL_NAME = 'figma';
var FIGMA_MCP_URL = 'https://mcp.figma.com/mcp';

var REFRESH_MS = 10000;
var SPAWN_TIMEOUT = 15000;   // `claude mcp list` health-checks every server — allow for slow remote connectors
var ACCOUNT_RECEIPT_MAX = 32 * 1024;

// state: 'connected' | 'needs-auth' | 'local-absent' | 'misconfigured' | 'cli-missing' | 'unknown'
//   connected / needs-auth / unknown reflect the LOCAL "figma" server.
//   local-absent → CLI works but this project has no local figma server yet.
//   misconfigured → a local "figma" entry exists but points at a foreign (non-Figma) host.
var cached = snapshot('unknown');
var probing = false;   // one probe at a time (the interval + invalidate() share it)
var started = false;
var connectedSince = null; // start of the current clean connector episode
var verificationNonce = null; // binds whoami output to that exact episode
var removeExecFile = cp.execFile;
var connectorMutation = false;

function now() { return new Date().toISOString(); }
function snapshot(state, local, global) {
  return {
    state: state,
    local:  local  || { present: false, status: 'unknown' },
    global: global || { present: false, name: null, status: 'unknown' },
    connectedSince: null,
    verificationNonce: null,
    checkedAt: now()
  };
}

function identityConnectorReady(value) {
  return !!(value && value.state === 'connected' && value.local && value.local.present &&
    !(value.global && value.global.present));
}

function installSnapshot(next) {
  if (identityConnectorReady(next)) {
    if (!identityConnectorReady(cached) || !connectedSince) {
      connectedSince = next.checkedAt;
      verificationNonce = crypto.randomBytes(16).toString('hex');
    }
    next.connectedSince = connectedSince;
    next.verificationNonce = verificationNonce;
  } else {
    connectedSince = null;
    verificationNonce = null;
    next.connectedSince = null;
    next.verificationNonce = null;
  }
  cached = next;
}

// Parse one row of `claude mcp list`:
//   "<name>: <url> - <status>"
//   e.g. "claude.ai Figma: https://mcp.figma.com/mcp - ✓ Connected"
//        "figma: https://mcp.figma.com/mcp (HTTP) - ! Needs authentication"
// Tolerant of extra spacing. The status is whatever follows the LAST " - " so a
// hyphen inside the URL doesn't split the row. Returns null for non-rows (the
// "Checking MCP server health…" header line, blanks).
function parseRow(line) {
  var colon = line.indexOf(':');
  if (colon < 0) return null;
  var name = line.slice(0, colon).trim();
  var rest = line.slice(colon + 1).trim();
  var dash = rest.lastIndexOf(' - ');
  if (dash < 0) return null;
  var url = rest.slice(0, dash).trim();
  var status = rest.slice(dash + 3).trim();
  if (!name || !status) return null;
  return { name: name, url: url, status: status };
}

function normalizeMcpUrl(url) {
  return String(url || '').trim().split(/\s+/)[0].replace(/\/+$/, '');
}

function isExpectedFigmaMcpUrl(row) {
  return normalizeMcpUrl(row && row.url) === FIGMA_MCP_URL;
}

// Match Figma without false positives: a word-boundary "figma" in the name, OR
// a URL whose HOST is (a subdomain of) figma.com. This rejects a decoy such as
// "notfigma.example" that a broad /figma/ substring would wrongly match.
function isFigmaRow(row) {
  if (isExpectedFigmaMcpUrl(row)) return true;
  var m = /^https?:\/\/([^/]+)/i.exec(row.url || '');
  return !!(m && /(^|\.)figma\.com$/i.test(m[1]));
}

// Map a status string to our state. Rows show "✓ Connected" or
// "! Needs authentication"; match on the WORDS, not the glyphs (glyphs vary
// across CLI versions and terminals).
function classify(status) {
  var s = String(status || '').trim();
  if (/auth/i.test(s)) return 'needs-auth';             // "Needs authentication"
  // Fail closed: current Claude CLI failures include strings such as
  // "Connected · tools fetch failed"; substring matching also made
  // "Disconnected" and "Not connected" look healthy.
  if (/^[^A-Za-z]*Connected\s*$/i.test(s)) return 'connected';
  return 'unknown';
}

// Turn captured `claude mcp list` output into a snapshot. Splits figma rows into
// the project-local server (name === LOCAL_NAME) and any competing global
// connector, and derives the overall state from the LOCAL one.
function classifyOutput(stdout, stderr, code) {
  if (code !== 0) return snapshot('unknown');
  var lines = (stdout || '').split(/\r?\n/);
  var local = null;     // this project's own server, named "figma"
  var global = null;    // any other figma connector (managed claude.ai / user scope)
  for (var i = 0; i < lines.length; i++) {
    var row = parseRow(lines[i]);
    if (!row) continue;
    var st = classify(row.status);
    if (row.name === LOCAL_NAME) {
      // A local row literally named "figma" is this project's own MCP entry by construction,
      // regardless of where it currently points. Classify it WITHOUT the isFigmaRow host gate
      // so a foreign-host entry reads as 'misconfigured' instead of being dropped (which would
      // mis-report as 'local-absent'). The host filter still guards the global branch below.
      if (isExpectedFigmaMcpUrl(row)) {
        if (!local || st === 'connected') local = { present: true, status: st, url: normalizeMcpUrl(row.url) };
      } else {
        local = { present: true, status: 'misconfigured', url: normalizeMcpUrl(row.url) };
      }
      continue;
    }
    if (!isFigmaRow(row)) continue;
    if (!global || st === 'connected') global = { present: true, name: row.name, status: st };
  }
  // code === 0 means the CLI ran fine, so "no local figma server" is a concrete
  // 'local-absent', never 'unknown'. ('unknown' is reserved for a failed probe.)
  var state = local ? local.status : 'local-absent';
  return snapshot(state, local, global);
}

function classifyScope(stdout) {
  var match = /^\s*Scope:\s*([^\r\n]+)$/im.exec(String(stdout || ''));
  if (!match) return 'unknown';
  var value = match[1].trim();
  if (/^Local config\b/i.test(value)) return 'local';
  if (/^(?:User|Project) config\b/i.test(value)) return 'nonlocal';
  return 'unknown';
}

function applyLocalScope(next, scope) {
  if (!next || !next.local || !next.local.present) return next;
  if (scope === 'local') return next;
  if (scope === 'nonlocal') {
    var competing = next.global && next.global.present
      ? next.global
      : { present: true, name: LOCAL_NAME, status: next.local.status || 'unknown' };
    return snapshot('local-absent', null, competing);
  }
  return snapshot('unknown', {
    present: true,
    status: 'unknown',
    url: next.local.url || null
  }, next.global);
}

function readLocalScope(cb) {
  try {
    cp.execFile('claude', ['mcp', 'get', LOCAL_NAME], {
      cwd: PROJECT_ROOT,
      env: childEnv(),
      timeout: SPAWN_TIMEOUT
    }, function (err, stdout) {
      cb(err ? 'unknown' : classifyScope(stdout));
    });
  } catch (e) { cb('unknown'); }
}

function verifySnapshotScope(next, cb) {
  if (!next || !next.local || !next.local.present) { cb(next); return; }
  readLocalScope(function (scope) { cb(applyLocalScope(next, scope)); });
}

// Kick an asynchronous probe. No-op if one is already in flight (the interval
// and invalidate() both call this). Updates `cached` when the child closes.
function probe() {
  if (probing) return;
  probing = true;
  var child;
  try {
    child = cp.spawn('claude', ['mcp', 'list'], { cwd: PROJECT_ROOT, env: childEnv() });
  } catch (e) {
    probing = false;
    installSnapshot(snapshot('cli-missing'));
    return;
  }
  var out = '', err = '', done = false;
  function fail(state) {
    if (done) return; done = true;
    try { child.kill('SIGTERM'); } catch (e) {}
    setTimeout(function () { try { child.kill('SIGKILL'); } catch (e) {} }, 500);
    probing = false; installSnapshot(snapshot(state));
  }
  var timer = setTimeout(function () { fail('unknown'); }, SPAWN_TIMEOUT);
  if (child.stdout) child.stdout.on('data', function (d) { if (out.length < 524288) { out += d; } else { clearTimeout(timer); fail('unknown'); } });
  if (child.stderr) child.stderr.on('data', function (d) { if (err.length < 524288) { err += d; } else { clearTimeout(timer); fail('unknown'); } });
  child.on('error', function (e) {
    if (done) return; done = true; clearTimeout(timer); probing = false;
    installSnapshot(snapshot((e && e.code === 'ENOENT') ? 'cli-missing' : 'unknown'));
  });
  child.on('close', function (code) {
    if (done) return; done = true; clearTimeout(timer); probing = false;
    var next = classifyOutput(out, err, code);
    if (!next.local || !next.local.present) { installSnapshot(next); return; }
    // Keep the probe in-flight until scope is known: `mcp list` merges scopes and
    // a row named figma is not proof that this project's private Local config won.
    probing = true;
    verifySnapshotScope(next, function (scoped) {
      probing = false;
      installSnapshot(scoped);
    });
  });
}

// Public, non-blocking: returns the last cached snapshot.
function status() { return cached; }
function probeState() { return { probing: probing, checkedAt: cached.checkedAt }; }

// Kick an immediate re-probe (the panel's / pill's "Re-check", the live poll
// while the user authenticates, and after add-local / whoami complete). Async —
// the fresh result lands in `cached` a few seconds later and reaches clients via
// SSE.
function invalidate() { probe(); }

// An explicit owner re-check is also an identity boundary: OAuth may have changed
// accounts while the transport stayed continuously green. Rotate the generation
// immediately so the old whoami file cannot keep actions ready during the probe.
function invalidateIdentity() {
  if (identityConnectorReady(cached)) {
    connectedSince = now();
    verificationNonce = crypto.randomBytes(16).toString('hex');
    cached = Object.assign({}, cached, {
      connectedSince: connectedSince,
      verificationNonce: verificationNonce
    });
  } else {
    connectedSince = null;
    verificationNonce = null;
    cached = Object.assign({}, cached, { connectedSince: null, verificationNonce: null });
  }
  probe();
}

// ---------------------------------------------------------------------------
// Bind this project: add a LOCAL-scoped "figma" MCP server pointing at the
// Figma remote MCP. This is a CLI config write to ~/.claude.json under the
// project path (same class of call as the `claude mcp list` probe) — it does
// NOT call Figma and does NOT write project files, so the golden invariant
// holds. OAuth itself still happens through the interactive `/mcp` flow; this
// just scaffolds the server so `/mcp` has something to authenticate.
// Idempotent: an already-present local "figma" server counts as success.
// ---------------------------------------------------------------------------
function addLocalServer(cb) {
  if (connectorMutation) { cb(new Error('add-failed'), ''); return; }
  connectorMutation = true;
  function done(error, output) { connectorMutation = false; cb(error, output); }
  var args = ['mcp', 'add', '--transport', 'http', '--scope', 'local', LOCAL_NAME, FIGMA_MCP_URL];
  function verify(out) {
    cp.execFile('claude', ['mcp', 'list'], { cwd: PROJECT_ROOT, env: childEnv(), timeout: 20000 }, function (listErr, stdout, stderr) {
      if (listErr) { done(listErr, out + String(stderr || '')); return; }
      var lines = String(stdout || '').split(/\r?\n/);
      for (var i = 0; i < lines.length; i++) {
        var row = parseRow(lines[i]);
        if (row && row.name === LOCAL_NAME) {
          if (isExpectedFigmaMcpUrl(row)) {
            readLocalScope(function (scope) {
              if (scope === 'local') { done(null, out); return; }
              done(new Error(scope === 'nonlocal'
                ? 'figma MCP exists, but is not in Local config scope'
                : 'figma MCP scope could not be verified'), out);
            });
            return;
          }
          done(new Error('local figma MCP points at unexpected URL: ' + normalizeMcpUrl(row.url)), out);
          return;
        }
      }
      done(new Error('local figma MCP was not found after add'), out);
    });
  }
  try {
    cp.execFile('claude', args, { cwd: PROJECT_ROOT, env: childEnv(), timeout: 20000 }, function (err, stdout, stderr) {
      var out = String(stdout || '') + String(stderr || '');
      if (err && !/already exists/i.test(out)) { done(err, out); return; }
      verify(out);
    });
  } catch (e) { done(e, ''); }
}

// Remove only this project's Local-scoped connector. The managed/account-level
// connector is deliberately outside this capability. Logging out first clears
// the Local connector's OAuth binding; an already absent binding/server is an
// idempotent success, while an unavailable CLI fails closed because removal
// cannot be proven.
function removeLocalServer(cb) {
  if (connectorMutation) { cb(new Error('integration-failed')); return; }
  connectorMutation = true;
  function done(error) { connectorMutation = false; cb(error || null); }
  var options = { cwd: PROJECT_ROOT, env: childEnv(), timeout: 20000 };
  function absent(output) {
    return /(?:not found|does not exist|no (?:mcp )?server|is not configured|not authenticated|not logged in|no (?:oauth )?authentication|no (?:oauth )?tokens?)/i.test(String(output || ''));
  }
  try {
    removeExecFile('claude', ['mcp', 'logout', LOCAL_NAME], options, function (logoutError, stdout, stderr) {
      if (logoutError && !absent(String(stdout || '') + String(stderr || ''))) {
        done(new Error('integration-failed'));
        return;
      }
      try {
        removeExecFile('claude', ['mcp', 'remove', '--scope', 'local', LOCAL_NAME], options,
          function (removeError, stdout, stderr) {
            var output = String(stdout || '') + String(stderr || '');
            if (removeError && !absent(output)) { done(new Error('integration-failed')); return; }
            installSnapshot(snapshot('local-absent'));
            done(null);
          });
      } catch (removeThrown) { done(new Error('integration-failed')); }
    });
  } catch (logoutThrown) { done(new Error('integration-failed')); }
}
function mutationBusy() { return connectorMutation; }

// ---------------------------------------------------------------------------
// Bound-account identity. Read-only: the figma:whoami session writes
// orchestrator/figma/.account.json (handle/email/tier/seat from the Figma MCP `whoami` tool); the
// server only reads it. Returns null when not yet recorded. Never throws.
// ---------------------------------------------------------------------------
function accountTimestampCurrent(checkedAt, episodeStartedAt, nowMs) {
  if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,3})?(?:Z|[+-]\d{2}:\d{2})$/.test(String(checkedAt || ''))) return false;
  var checkedMs = Date.parse(checkedAt || '');
  var episodeMs = Date.parse(episodeStartedAt || '');
  return Number.isFinite(checkedMs) && Number.isFinite(episodeMs)
    && checkedMs + 1000 >= episodeMs
    && checkedMs <= nowMs + 5 * 60 * 1000;
}

function normalizeAccountRecord(obj, episodeStartedAt, expectedNonce, nowMs) {
  if (!obj || typeof obj !== 'object' || Array.isArray(obj)) return null;
  if (Object.keys(obj).sort().join('\0') !== ['handle', 'email', 'tier', 'seat', 'checkedAt', 'verificationNonce'].sort().join('\0')) return null;
  function str(v, max) {
    var s = typeof v === 'string' ? v.trim() : '';
    return s && s.length <= max && !/[\x00-\x1f\x7f]/.test(s) ? s : null;
  }
  var a = {
    handle: str(obj.handle, 200), email: str(obj.email, 320), tier: str(obj.tier, 100), seat: str(obj.seat, 100),
    checkedAt: str(obj.checkedAt, 64), verificationNonce: str(obj.verificationNonce, 64)
  };
  if (!expectedNonce || a.verificationNonce !== expectedNonce ||
      !accountTimestampCurrent(a.checkedAt, episodeStartedAt, nowMs) ||
      !(a.handle || a.email)) return null;
  delete a.verificationNonce;
  return a;
}

function account() {
  try {
    if (!identityConnectorReady(cached)) return null;
    var p = path.join(PROJECT_ROOT, 'orchestrator', 'figma', '.account.json');
    var hit = fileGuards.boundedRegularFileUnder(PROJECT_ROOT, path.dirname(p), p, ACCOUNT_RECEIPT_MAX);
    if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') return null;
    var obj = JSON.parse(hit.bytes.toString('utf8'));
    // A previous OAuth episode's whoami file must not become the current account
    // merely because the connector later returned to green. Allow one second for
    // agent-written timestamps that omit milliseconds.
    return normalizeAccountRecord(obj, connectedSince, verificationNonce, Date.now());
  } catch (e) { return null; }
}

function sessionAdmissionFor(key, connector, currentAccount) {
  if (typeof key !== 'string' || key.indexOf('figma:') !== 0) return null;
  if (key.indexOf('figma:rebundle:') === 0) return null;
  connector = connector || {};
  if (connector.global && connector.global.present) {
    return { error: 'figma-connector-conflict', detail: 'Remove the competing global Figma connector before starting this session.' };
  }
  if (connector.state !== 'connected' || !connector.local || !connector.local.present) {
    return { error: 'figma-connector-not-ready', detail: 'The project-local Figma MCP connector is not connected.' };
  }
  if (key === 'figma:whoami') return null;
  if (!currentAccount) {
    return { error: 'figma-account-unverified', detail: 'Verify the current Figma account before starting this session.' };
  }
  return null;
}

function sessionAdmission(key) {
  return sessionAdmissionFor(key, status(), account());
}

function init() {
  if (started) return;
  started = true;
  probe();                                    // one async probe at boot (does NOT block listen)
  setInterval(probe, REFRESH_MS);
}

// Open the native macOS Terminal already cd'd into the project with `claude` running, so the user only
// needs to type /mcp → Authenticate. macOS-only (osascript). NOT a Figma call. cb(err|null).
function openTerminal(cb) {
  if (process.platform !== 'darwin') { cb(new Error('unsupported-platform')); return; }
  function shellQuote(s) { return "'" + String(s).replace(/'/g, "'\\''") + "'"; }
  var sh = 'cd ' + shellQuote(PROJECT_ROOT) + ' && claude';
  var asStr = '"' + sh.replace(/\\/g, '\\\\').replace(/"/g, '\\"') + '"';
  cp.execFile('osascript', ['-e', 'tell application "Terminal" to do script ' + asStr, '-e', 'tell application "Terminal" to activate'], { timeout: 10000 }, function (err) { cb(err || null); });
}

// Count + freshness of the usage-scoped observed token catalog in the current sealed generation.
function tokensInfo() {
  try {
    var active = generation.current();
    if (!active.ok || active.mode !== 'generation') return null;
    var entry = active.manifest.artifacts.find(function (candidate) {
      return candidate.group === 'tokens' && candidate.role === 'observed-token-catalog';
    });
    var bytes = generation.readEntry(entry);
    var obj = bytes && JSON.parse(bytes.toString('utf8'));
    var count = obj && obj.counts && Number.isSafeInteger(obj.counts.activeTokens) ? obj.counts.activeTokens : 0;
    return count > 0 ? { count: count, mtime: Date.parse(active.manifest.createdAt) || 0 } : null;
  } catch (e) { return null; }
}

module.exports = {
  init: init,
  status: status,
  probeState: probeState,
  invalidate: invalidate,
  invalidateIdentity: invalidateIdentity,
  addLocalServer: addLocalServer,
  removeLocalServer: removeLocalServer,
  mutationBusy: mutationBusy,
  account: account,
  sessionAdmission: sessionAdmission,
  openTerminal: openTerminal,
  tokensInfo: tokensInfo,
  _test: {
    classify: classify,
    classifyOutput: classifyOutput,
    classifyScope: classifyScope,
    applyLocalScope: applyLocalScope,
    identityConnectorReady: identityConnectorReady,
    installSnapshot: installSnapshot,
    accountTimestampCurrent: accountTimestampCurrent,
    normalizeAccountRecord: normalizeAccountRecord,
    sessionAdmissionFor: sessionAdmissionFor,
    setRemoveExecFile: function (runner) { removeExecFile = typeof runner === 'function' ? runner : cp.execFile; }
  }
};
