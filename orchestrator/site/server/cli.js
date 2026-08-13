'use strict';

// ---------------------------------------------------------------------------
// Claude CLI readiness. The whole system runs the user's tasks through the
// `claude` CLI, so the site needs to know — and surface in the header — whether
// the CLI is installed and authenticated. Both are GLOBAL (per machine/user,
// not per project): `claude auth login` writes to the OS keychain, so one login
// covers every project.
//
// Probing spawns processes (`claude --version`, `claude auth status`), so we
// never do it on the hot path. A background interval refreshes a cached
// snapshot; deriveState() and the API read the cache instantly. invalidate()
// forces the next refresh to re-probe (used right after an install/login).
// ---------------------------------------------------------------------------

var cp    = require('child_process');
var fs    = require('fs');
var path  = require('path');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var childEnv = require('./child-env').childEnv;

var PROJECT_ROOT = paths.PROJECT_ROOT;
var RUNS_DIR = paths.RUNS_DIR;
var REFRESH_MS = 10000;
var SPAWN_TIMEOUT = 8000;
var PROBE_OUTPUT_MAX_BYTES = 256 * 1024;

function boundedCollector(maxBytes) {
  var chunks = [];
  var size = 0;
  return {
    append: function (chunk) {
      if (size >= maxBytes) return;
      var bytes = Buffer.isBuffer(chunk) ? chunk : Buffer.from(String(chunk));
      var keep = Math.min(bytes.length, maxBytes - size);
      if (keep > 0) { chunks.push(bytes.subarray(0, keep)); size += keep; }
    },
    text: function () { return Buffer.concat(chunks, size).toString('utf8'); }
  };
}

var cached = { installed: false, version: null, loggedIn: false,
  authMethod: null, subscriptionType: null, email: null, checkedAt: null };
var probing = false;   // one probe at a time — the interval + invalidate() share it
var started = false;

// --- Subscription usage (5h + weekly limits) -------------------------------
// Read from the SAME undocumented endpoint the interactive `/usage` uses:
// GET /api/oauth/usage, authed with the OAuth token Claude Code keeps in the OS
// keychain ("Claude Code-credentials"). Response carries per-window
// { utilization (percent USED), resets_at }; the header shows remaining = 100 -
// utilization. UNDOCUMENTED + version-bound: any non-200/parse/timeout degrades
// to null so the popover simply hides the rows (never shows garbage). macOS only
// (keychain via `security`); other platforms → null.
var cachedUsage = null;        // normalized usage or null (null => rows hidden)
var usageInFlight = false;
var usageCheckedAt = 0;

// --- Auth-token health ------------------------------------------------------
// `claude auth status` only READS the keychain — it reports loggedIn:true even
// when the stored OAuth token is expired or revoked and every real API call
// fails with "401 Invalid authentication credentials". So "logged in" alone
// can't drive a green pill; the header needs a verdict on the token itself.
// Verdicts (in confidence order):
//   'expired' — keychain expiresAt is in the past AND there is no refreshToken,
//               so the CLI cannot self-heal (local check, no network);
//   'revoked' — token not yet expired, but the usage endpoint answered 401/403.
// A 200 from the usage endpoint clears the verdict; 429/5xx/network keep the
// last one (no flapping on transient noise). An expired-but-refreshable token
// is NOT flagged: the CLI refreshes lazily on its next run, so a 401 in that
// window would be a false alarm. A new token (different expiresAt) resets the
// verdict so a stale 'expired' can't outlive a successful re-login.
var authProblem = null;        // null | 'expired' | 'revoked'
var authExpiresAt = null;      // ISO of the keychain token's expiresAt, or null
var USAGE_REFRESH_MS = 60000;  // windows move slowly; 60s is plenty
var USAGE_URL = 'https://api.anthropic.com/api/oauth/usage';
var USAGE_BETA = 'oauth-2025-04-20';
var KEYCHAIN_SERVICE = 'Claude Code-credentials';
// Known windows in display order; unknown keys the API may add fall to the end
// (humanized client-side). Anything with a numeric utilization is surfaced, so
// "everything the endpoint reports" shows up without a code change here.
var USAGE_ORDER = { five_hour: 0, seven_day: 1, seven_day_opus: 2, seven_day_sonnet: 3, seven_day_oauth_apps: 4, seven_day_cowork: 5 };

// Run `claude auth status` asynchronously after a successful version probe.
// Calls back with the final snapshot when done (or on timeout/error).
function probeAuth(out, done) {
  var child;
  try {
    child = cp.spawn('claude', ['auth', 'status'], { stdio: ['ignore', 'pipe', 'ignore'], env: childEnv() });
  } catch (e) {
    out.checkedAt = new Date().toISOString();
    done(out);
    return;
  }
  var output = boundedCollector(PROBE_OUTPUT_MAX_BYTES), finished = false;
  var timer = setTimeout(function () {
    if (finished) return; finished = true; clearTimeout(timer);
    // SIGTERM, then a SIGKILL fallback so a signal-ignoring child can't wedge
    // the probe forever; finalize via done() (resets the caller's `probing`).
    try { child.kill('SIGTERM'); } catch (e) {}
    setTimeout(function () { try { child.kill('SIGKILL'); } catch (e) {} }, 500);
    out.checkedAt = new Date().toISOString();
    done(out);
  }, SPAWN_TIMEOUT);
  child.stdout.on('data', output.append);
  child.on('error', function () {
    if (finished) return; finished = true; clearTimeout(timer);
    out.checkedAt = new Date().toISOString();
    done(out);
  });
  child.on('close', function (code) {
    if (finished) return; finished = true; clearTimeout(timer);
    var buf = output.text();
    if (code === 0 && buf) {
      try {
        var j = JSON.parse(buf);
        out.loggedIn = !!j.loggedIn;
        out.authMethod = typeof j.authMethod === 'string' ? j.authMethod.slice(0, 120) : null;
        out.subscriptionType = typeof j.subscriptionType === 'string' ? j.subscriptionType.slice(0, 120) : null;
        out.email = typeof j.email === 'string' ? j.email.slice(0, 320) : null;
      } catch (e) {}
    }
    out.checkedAt = new Date().toISOString();
    // Refresh subscription usage (throttled). status() gates display on loggedIn,
    // so a logged-out probe never serves stale numbers even if cachedUsage lingers.
    if (out.loggedIn) probeUsage();
    done(out);
  });
}

// Kick an asynchronous probe. No-op if one is already in flight.
// Updates `cached` when the two-step check (version → auth) completes.
function probe() {
  if (probing) return;
  probing = true;
  var out = { installed: false, version: null, loggedIn: false,
    authMethod: null, subscriptionType: null, email: null, checkedAt: null };
  var child;
  try {
    child = cp.spawn('claude', ['--version'], { stdio: ['ignore', 'pipe', 'ignore'], env: childEnv() });
  } catch (e) {
    probing = false;
    out.checkedAt = new Date().toISOString();
    cached = out;
    return;
  }
  var output = boundedCollector(PROBE_OUTPUT_MAX_BYTES), finished = false;
  var timer = setTimeout(function () {
    if (finished) return; finished = true; clearTimeout(timer); probing = false;
    // SIGTERM, then a SIGKILL fallback so a signal-ignoring child can't wedge
    // the probe forever; reset `probing` so the next interval can re-probe.
    try { child.kill('SIGTERM'); } catch (e) {}
    setTimeout(function () { try { child.kill('SIGKILL'); } catch (e) {} }, 500);
    out.checkedAt = new Date().toISOString();
    cached = out;
  }, SPAWN_TIMEOUT);
  child.stdout.on('data', output.append);
  child.on('error', function (e) {
    if (finished) return; finished = true; clearTimeout(timer); probing = false;
    out.checkedAt = new Date().toISOString();
    cached = out;
  });
  child.on('close', function (code) {
    if (finished) return; finished = true; clearTimeout(timer);
    var buf = output.text();
    if (code === 0 && buf.trim()) {
      out.installed = true;
      out.version = (buf.trim().split(/\s+/)[0] || '').slice(0, 120) || null;
      // Second probe: auth status. probing stays true until it completes.
      probeAuth(out, function (result) { probing = false; cached = result; });
    } else {
      probing = false;
      out.checkedAt = new Date().toISOString();
      cached = out;
    }
  });
}

// Public, non-blocking: returns the last cached snapshot + current usage +
// token health. All three are gated on loggedIn so a logged-out state never
// serves stale numbers/verdicts (the pill shows plain "sign in" then), and
// ride along under state.cli.* (state.js spreads status()).
function status() {
  var li = !!(cached && cached.loggedIn);
  return Object.assign({}, cached, {
    usage: li ? cachedUsage : null,
    authProblem: li ? authProblem : null,
    authExpiresAt: li ? authExpiresAt : null
  });
}

// Kick an immediate re-probe — called after install/login completes so the
// header pill updates without waiting up to REFRESH_MS for the next interval.
// Reset the usage throttle too, so a fresh login's limits appear promptly.
function invalidate() { usageCheckedAt = 0; probe(); }

// Read + parse Claude Code's OAuth token JSON from the macOS keychain. Shells out
// to `security` (no native keychain dep). cb(token|null); token carries
// { accessToken, refreshToken, expiresAt, subscriptionType }.
function readOAuthToken(cb) {
  if (process.platform !== 'darwin') { cb(null); return; }
  var child;
  try {
    child = cp.spawn('security', ['find-generic-password', '-s', KEYCHAIN_SERVICE, '-w'],
      { stdio: ['ignore', 'pipe', 'ignore'], env: childEnv() });
  } catch (e) { cb(null); return; }
  var output = boundedCollector(PROBE_OUTPUT_MAX_BYTES), done2 = false;
  var timer = setTimeout(function () {
    if (done2) return; done2 = true;
    try { child.kill('SIGKILL'); } catch (e) {}
    cb(null);
  }, SPAWN_TIMEOUT);
  child.stdout.on('data', output.append);
  child.on('error', function () { if (done2) return; done2 = true; clearTimeout(timer); cb(null); });
  child.on('close', function (code) {
    if (done2) return; done2 = true; clearTimeout(timer);
    var buf = output.text();
    if (code !== 0 || !buf.trim()) { cb(null); return; }
    try { var j = JSON.parse(buf); cb((j && j.claudeAiOauth) ? j.claudeAiOauth : j); }
    catch (e) { cb(null); }
  });
}

// Snap an ISO timestamp to the NEAREST minute. The endpoint recomputes resets_at
// with sub-second jitter on EVERY call (e.g. ...18:00:00.341990 vs ...17:59:59.8),
// so the raw value would change the SSE state-hash on each 60s refetch and fire a
// spurious 'change' to every client. Rounding (not flooring) is required: the
// jitter straddles minute boundaries (a reset at :00 wobbles to the previous
// :59.x), and floor would flip 18:00↔17:59 — round maps the whole ±1s band to one
// minute. The popover only shows HH:MM / a date, so minute precision is lossless.
function stableReset(iso) {
  if (typeof iso !== 'string') return null;
  var ms = Date.parse(iso);
  if (isNaN(ms)) return iso;   // unparseable → leave as-is rather than drop it
  return new Date(Math.round(ms / 60000) * 60000).toISOString();
}

// Shape the raw endpoint JSON into { windows:[{key,utilization,resetsAt}], extra }.
// Only objects carrying a numeric `utilization` become rows, so null/experimental
// slots drop out. utilization is percent USED; the client renders 100 - it.
// Deliberately carries NO wall-clock "fetchedAt" — that field would churn the SSE
// state-hash every refetch (see sse.js stateHash). Returns null when empty.
function normalizeUsage(j) {
  if (!j || typeof j !== 'object') return null;
  var windows = [];
  Object.keys(j).forEach(function (k) {
    if (k === 'extra_usage') return;
    var v = j[k];
    if (v && typeof v === 'object' && typeof v.utilization === 'number') {
      windows.push({ key: k, utilization: v.utilization, resetsAt: stableReset(v.resets_at) });
    }
  });
  windows.sort(function (a, b) {
    var ai = USAGE_ORDER[a.key] != null ? USAGE_ORDER[a.key] : 50;
    var bi = USAGE_ORDER[b.key] != null ? USAGE_ORDER[b.key] : 50;
    return ai - bi || (a.key < b.key ? -1 : a.key > b.key ? 1 : 0);
  });
  var extra = null;
  var eu = j.extra_usage;
  if (eu && typeof eu === 'object' && eu.is_enabled) {
    extra = {
      enabled: true,
      utilization: (typeof eu.utilization === 'number' ? eu.utilization : null),
      usedCredits: (typeof eu.used_credits === 'number' ? eu.used_credits : null),
      monthlyLimit: (typeof eu.monthly_limit === 'number' ? eu.monthly_limit : null),
      currency: (typeof eu.currency === 'string' ? eu.currency : null)
    };
  }
  if (!windows.length && !extra) return null;
  return { windows: windows, extra: extra };
}

// Throttled usage refresh: read keychain token → GET the usage endpoint →
// normalize. Any failure (no token, non-200, parse error, timeout) sets
// cachedUsage = null so the UI hides the rows. Self-throttled to USAGE_REFRESH_MS.
// Doubles as the auth-health probe (see the authProblem block above): the token
// read feeds the local expiry rule, the HTTP status feeds the revoked rule.
function probeUsage() {
  if (usageInFlight) return;
  if (Date.now() - usageCheckedAt < USAGE_REFRESH_MS) return;
  usageInFlight = true;
  readOAuthToken(function (tok) {
    var access = tok && tok.accessToken;
    // Local expiry rule. A failed keychain read (tok null) changes nothing —
    // it's not evidence of a NEW token, so the last verdict stands.
    var expMs = (tok && typeof tok.expiresAt === 'number') ? tok.expiresAt : null;
    var expIso = (expMs != null) ? new Date(expMs).toISOString() : null;
    if (expIso && expIso !== authExpiresAt) { authExpiresAt = expIso; authProblem = null; }
    var expired = (expMs != null) && expMs < Date.now();
    var refreshable = !!(tok && tok.refreshToken);
    if (expired && !refreshable) authProblem = 'expired';
    if (!access || typeof fetch !== 'function') {
      cachedUsage = null; usageInFlight = false; usageCheckedAt = Date.now(); return;
    }
    // UA mirrors the installed CLI when its exact version was observed. Never fabricate a
    // version when the probe has not produced one.
    var ua = 'claude-cli/' + (cached && cached.version ? cached.version : 'unknown') + ' (external, cli)';
    var ctrl = new AbortController();
    var to = setTimeout(function () { try { ctrl.abort(); } catch (e) {} }, SPAWN_TIMEOUT);
    fetch(USAGE_URL, {
      signal: ctrl.signal,
      headers: { 'Authorization': 'Bearer ' + access, 'anthropic-beta': USAGE_BETA, 'User-Agent': ua }
    }).then(function (r) {
      if (!r.ok) {
        // 401/403 on a NOT-yet-expired token = server-side rejection (revoked).
        // On an expired one it's expected noise: 'expired' is already set when
        // unrefreshable, and a refreshable token heals on the CLI's next run.
        if ((r.status === 401 || r.status === 403) && !expired) authProblem = 'revoked';
        throw new Error('HTTP ' + r.status);
      }
      return r.json();
    }).then(function (j) {
      cachedUsage = normalizeUsage(j);
      authProblem = null;   // the token demonstrably works
    }).catch(function () {
      cachedUsage = null;
    }).then(function () {
      clearTimeout(to); usageInFlight = false; usageCheckedAt = Date.now();
    });
  });
}

// ---------------------------------------------------------------------------
// Install + login jobs (header buttons). Install is fire-and-forget. Login is
// interactive: `claude auth login` opens the browser and then waits for the
// user to paste an OAuth code back on stdin (the callback is hosted, not
// localhost), so we keep the child alive, surface the URL, and relay the code.
// ---------------------------------------------------------------------------

var installJob = null;   // { running, startedAt, endedAt, exitCode }
var loginChild = null;
var loginUrl = null;
var loginStartedAt = null;
var loginFd = null;          // module-scoped so killLogin() can close it
var loginTimeoutId = null;   // auto-cancel a login the user abandons
var loginPrep = null;        // `claude auth logout` child during a fresh re-login
var LOGIN_TIMEOUT_MS = 10 * 60 * 1000;
var JOB_LOG_MAX_BYTES = 1024 * 1024;
var LOGIN_OUTPUT_BUFFER_MAX = 128 * 1024;
var LOGIN_URL_MAX = 4096;

// Primary matcher: path-anchored + host-agnostic so a host/path-prefix change in
// the CLI's printed URL (e.g. claude.com/cai/... → some other host) still matches.
var OAUTH_URL_RE = /https:\/\/[^\s'"]*oauth\/authorize[^\s'"]*/i;

function ensureRunsDir() {
  return !!fileGuards.realDirectoryUnder(PROJECT_ROOT, RUNS_DIR, { create: true, mode: 0o700 });
}

function openJobLog(name) {
  return fileGuards.openAtomicReplaceRegularFile(
    PROJECT_ROOT, RUNS_DIR, path.join(RUNS_DIR, name),
    { create: true, mode: 0o600, maxBytes: 0 }
  );
}

function runtimeUnavailable() {
  return { running: false, error: 'cli-runtime-unsafe' };
}

function install() {
  if (installJob && installJob.running) return installJob;
  if (!ensureRunsDir()) return runtimeUnavailable();
  var logFd;
  logFd = openJobLog('.cli-install.log');
  if (logFd === null) return runtimeUnavailable();
  installJob = { running: true, startedAt: new Date().toISOString(), endedAt: null, exitCode: null };
  var job = installJob;
  var child;
  try {
    child = cp.spawn('npm', ['install', '-g', '@anthropic-ai/claude-code'],
      { stdio: ['ignore', 'pipe', 'pipe'], env: childEnv() });
  } catch (e) {
    try { fs.writeSync(logFd, ('spawn failed: ' + String(e && e.message || e)).slice(0, 4096) + '\n'); } catch (e2) {}
    try { fs.closeSync(logFd); } catch (e2) {}   // close even if writeSync threw (no fd leak)
    installJob.running = false; installJob.endedAt = new Date().toISOString(); installJob.exitCode = -1;
    return installJob;
  }
  var logBytes = 0;
  function appendInstallLog(chunk) {
    if (logFd == null || logBytes >= JOB_LOG_MAX_BYTES) return;
    var bytes = Buffer.isBuffer(chunk) ? chunk : Buffer.from(String(chunk));
    var keep = Math.min(bytes.length, JOB_LOG_MAX_BYTES - logBytes);
    try {
      var offset = 0;
      while (offset < keep) {
        var written = fs.writeSync(logFd, bytes, offset, keep - offset, logBytes + offset);
        if (!written) throw new Error('install log write made no progress');
        offset += written;
      }
      logBytes += keep;
      if (logBytes >= JOB_LOG_MAX_BYTES) { try { fs.closeSync(logFd); } catch (e) {} logFd = null; }
    } catch (error) { try { fs.closeSync(logFd); } catch (e2) {} logFd = null; }
  }
  if (child.stdout) child.stdout.on('data', appendInstallLog);
  if (child.stderr) child.stderr.on('data', appendInstallLog);
  child.on('exit', function (code) {
    if (installJob !== job) return;   // a newer install replaced us → don't touch shared state
    try { fs.closeSync(logFd); } catch (e) {}
    job.running = false; job.endedAt = new Date().toISOString(); job.exitCode = code;
    invalidate();
  });
  child.on('error', function () {
    if (installJob !== job) return;   // a newer install replaced us → don't touch shared state
    try { fs.closeSync(logFd); } catch (e) {}
    job.running = false; job.endedAt = new Date().toISOString(); job.exitCode = -1;
    invalidate();
  });
  return installJob;
}

// Tear down all login state idempotently: clear the abandon-timer, close the
// log fd, drop the child + url. Safe to call multiple times.
function clearLoginState() {
  if (loginTimeoutId) { clearTimeout(loginTimeoutId); loginTimeoutId = null; }
  if (loginFd != null) { try { fs.closeSync(loginFd); } catch (e) {} loginFd = null; }
  loginChild = null; loginUrl = null; loginStartedAt = null;
}

// Entry point for the header buttons. Plain login spawns `claude auth login`
// directly. `opts.fresh` (the re-login path, offered when the stored token is
// expired/revoked) runs `claude auth logout` first so `auth login` always
// follows the known first-login path (print URL → paste code) and can't hit an
// "already logged in" branch — the stored token is already dead by then, so
// clearing it costs nothing.
function login(opts) {
  if (loginChild || loginPrep) return { running: true, url: loginUrl };
  // A fresh login performs a logout subprocess first. Refuse before that spawn
  // too: every login generation must have a safely anchored log destination.
  if (!ensureRunsDir()) return runtimeUnavailable();
  if (opts && opts.fresh) {
    var lo;
    try { lo = cp.spawn('claude', ['auth', 'logout'], { stdio: 'ignore', env: childEnv() }); }
    catch (e) { return startLoginChild(); }
    loginPrep = lo;
    loginStartedAt = new Date().toISOString();   // job appears in the UI right away
    var prepTimer = setTimeout(function () { try { lo.kill('SIGKILL'); } catch (e) {} }, SPAWN_TIMEOUT);
    var next = function () {
      clearTimeout(prepTimer);
      if (loginPrep !== lo) return;   // canceled during logout → don't start a login
      loginPrep = null;
      startLoginChild();
      invalidate();                   // header flips to "sign in" promptly
    };
    lo.on('exit', next);
    lo.on('error', next);
    return { running: true };
  }
  return startLoginChild();
}

function startLoginChild() {
  if (loginChild) return { running: true, url: loginUrl };
  if (!ensureRunsDir()) return runtimeUnavailable();
  loginFd = openJobLog('.cli-login.log');
  if (loginFd === null) return runtimeUnavailable();
  var child;
  try {
    child = cp.spawn('claude', ['auth', 'login', '--claudeai'],
      { stdio: ['pipe', 'pipe', 'pipe'], env: childEnv() });
  } catch (e) {
    try { fs.closeSync(loginFd); } catch (e2) {} loginFd = null;
    return { running: false, error: 'cli-login-spawn-failed' };
  }
  loginChild = child; loginUrl = null; loginStartedAt = new Date().toISOString();
  // Auto-cancel a login the user opened but never completed (browser closed,
  // tab abandoned) so the child + pipe don't linger forever.
  loginTimeoutId = setTimeout(function () {
    if (loginChild) { console.warn('[cli] login abandoned — auto-canceling'); loginCancel(); invalidate(); }
  }, LOGIN_TIMEOUT_MS);
  var buf = '';
  var loginLogBytes = 0;
  function onData(d) {
    if (loginChild !== child) return;   // a newer login replaced us → ignore stale data
    buf += d.toString();
    if (loginFd != null) {
      try {
        var bytes = Buffer.isBuffer(d) ? d : Buffer.from(String(d));
        var keep = Math.min(bytes.length, JOB_LOG_MAX_BYTES - loginLogBytes);
        var offset = 0;
        while (offset < keep) {
          var written = fs.writeSync(loginFd, bytes, offset, keep - offset, loginLogBytes + offset);
          if (!written) throw new Error('login log write made no progress');
          offset += written;
        }
        loginLogBytes += keep;
        if (loginLogBytes >= JOB_LOG_MAX_BYTES) { try { fs.closeSync(loginFd); } catch (limitCloseError) {} loginFd = null; }
      }
      catch (e) { try { fs.closeSync(loginFd); } catch (e2) {} loginFd = null; }   // don't write to a dead fd forever
    }
    if (!loginUrl) {
      var m = buf.match(OAUTH_URL_RE);
      if (m) loginUrl = m[0].slice(0, LOGIN_URL_MAX);
    }
    if (buf.length > LOGIN_OUTPUT_BUFFER_MAX) buf = buf.slice(-LOGIN_OUTPUT_BUFFER_MAX);
  }
  child.stdout.on('data', onData);
  child.stderr.on('data', onData);
  child.stdin.on('error', function (error) {
    if (loginChild !== child) return;
    console.warn('[cli] login stdin failed:', error && error.message || error);
    try { child.kill('SIGTERM'); } catch (killError) {}
    clearLoginState();
    invalidate();
  });
  // Only the CURRENT login's child may tear down shared state — a stale child's
  // late exit must not close the new login's fd or clear loginChild.
  child.on('exit', function () { if (loginChild !== child) return; clearLoginState(); invalidate(); });
  child.on('error', function () { if (loginChild !== child) return; clearLoginState(); invalidate(); });
  return { running: true };
}

// Relay the OAuth code the user pasted from the browser into the waiting child.
function loginSubmitCode(code) {
  if (!loginChild || !loginChild.stdin || !loginChild.stdin.writable) return false;
  var c = String(code).trim();
  // Claude's browser copy can append the exact authorization URL after a BEL
  // separator (`code#state\x07https://...`). Strip only the URL captured from
  // this login generation. Any other control-bearing paste still fails closed,
  // so this cannot become a generic line/control-character sanitizer.
  var trailerSeparator = c.indexOf('\x07');
  if (trailerSeparator >= 0) {
    if (!loginUrl || c.indexOf('\x07', trailerSeparator + 1) >= 0 ||
        c.slice(trailerSeparator + 1) !== loginUrl) return false;
    c = c.slice(0, trailerSeparator);
  }
  if (c.length > 512) return false;
  // Reject control chars (newline/CR/etc.) so a crafted paste can't inject extra
  // lines into the CLI's stdin. OAuth codes are a single line of printable text.
  for (var _i = 0; _i < c.length; _i++) { if (c.charCodeAt(_i) < 0x20) return false; }
  if (!c) return false;
  try { loginChild.stdin.write(c + '\n'); return true; } catch (e) { return false; }
}

function loginCancel() {
  if (loginPrep) {
    // Cancel during the fresh-relogin logout: null the handle FIRST so the
    // child's exit callback sees the mismatch and doesn't start a login.
    var p = loginPrep;
    loginPrep = null;
    loginStartedAt = null;
    try { p.kill('SIGTERM'); } catch (e) {}
    setTimeout(function () { try { p.kill('SIGKILL'); } catch (e) {} }, 500);
  }
  if (loginChild) {
    var c = loginChild;
    try { c.kill('SIGTERM'); } catch (e) {}
    // SIGKILL fallback (parity with the session/probe kills) so a login child that
    // ignores SIGTERM doesn't linger; bound to the captured handle since
    // clearLoginState() nulls loginChild immediately below.
    setTimeout(function () { try { c.kill('SIGKILL'); } catch (e) {} }, 500);
  }
  clearLoginState();
}

// Called on server shutdown so an in-progress login doesn't orphan a child.
function killLogin() { loginCancel(); }

// Job snapshot for the header (state.cli.jobs).
function jobs() {
  return {
    install: installJob ? { running: installJob.running, exitCode: installJob.exitCode } : null,
    // loginPrep counts as running so the UI shows progress during the
    // logout step of a fresh re-login (url stays null → "opening…" hint).
    login: (loginChild || loginPrep) ? { running: true, url: loginUrl, startedAt: loginStartedAt } : null
  };
}

var LOG_TAIL_BYTES = 64 * 1024;

// Read only a bounded tail through a stable, non-following descriptor. The log
// may keep growing while a job runs; inode identity (not size/mtime equality)
// is the safety invariant, and the descriptor prevents a later path swap from
// redirecting the read.
function readJobLogTail(file) {
  var bytes = fileGuards.tailRegularFileUnder(PROJECT_ROOT, RUNS_DIR, file, LOG_TAIL_BYTES);
  return bytes ? bytes.toString('utf8') : '';
}

function readJobLog(kind) {
  var name = kind === 'install' ? '.cli-install.log' : kind === 'login' ? '.cli-login.log' : null;
  if (!name) return null;
  return readJobLogTail(path.join(RUNS_DIR, name));
}

function init() {
  if (started) return;
  started = true;
  probe();                                    // one async probe at boot (does NOT block listen)
  setInterval(probe, REFRESH_MS);
}

module.exports = {
  init: init,
  status: status,
  invalidate: invalidate,
  install: install,
  login: login,
  loginSubmitCode: loginSubmitCode,
  loginCancel: loginCancel,
  killLogin: killLogin,
  jobs: jobs,
  readJobLog: readJobLog,
  ensureRunsDir: ensureRunsDir
};
