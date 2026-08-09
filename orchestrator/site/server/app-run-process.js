'use strict';

var fs = require('fs');
var path = require('path');
var childProcess = require('child_process');
var redaction = require('./app-run-redaction');
var writerLeases = require('../../tasks/writer-leases.cjs');
var paths = require('./paths');

var DEFAULT_TIMEOUT = 120000;
var MAX_OUTPUT_BYTES = 2 * 1024 * 1024;
var SAFE_NAME_RE = /^[A-Za-z0-9._+-]{1,100}$/;
var ownedDetached = new WeakMap();

function executable(candidate) {
  if (!candidate || typeof candidate !== 'string') return null;
  var resolved;
  try {
    resolved = fs.realpathSync(candidate);
    var stat = fs.lstatSync(resolved);
    if (!stat.isFile() || stat.isSymbolicLink()) return null;
    fs.accessSync(resolved, fs.constants.X_OK);
    return resolved;
  } catch (_) { return null; }
}

function resolveExecutable(name, candidates, envPath) {
  var rows = Array.isArray(candidates) ? candidates.slice() : [];
  if (path.isAbsolute(String(name || ''))) rows.unshift(name);
  var seen = Object.create(null);
  for (var i = 0; i < rows.length; i++) {
    var hit = executable(rows[i]);
    if (hit && !seen[hit]) return hit;
    if (hit) seen[hit] = true;
  }
  if (!SAFE_NAME_RE.test(String(name || ''))) return null;
  var pathHits = [];
  String(envPath || process.env.PATH || '').split(path.delimiter).filter(Boolean).forEach(function (dir) {
    if (!path.isAbsolute(dir)) return;
    var hit = executable(path.join(dir, name));
    if (hit && !seen[hit]) {
      seen[hit] = true;
      pathHits.push(hit);
    }
  });
  // A server-owned canonical candidate has precedence. PATH is only the final
  // discovery tier and is executable authority, so two distinct PATH hits are
  // ambiguous rather than an order-dependent choice.
  return pathHits.length === 1 ? pathHits[0] : null;
}

function minimalEnv(extra) {
  var allowed = [
    'PATH', 'LANG', 'LC_ALL', 'TMPDIR', 'HOME', 'JAVA_HOME',
    'ANDROID_HOME', 'ANDROID_SDK_ROOT', 'DEVELOPER_DIR'
  ];
  var env = Object.create(null);
  allowed.forEach(function (key) {
    if (typeof process.env[key] === 'string') env[key] = process.env[key];
  });
  env.LANG = env.LANG || 'C.UTF-8';
  env.LC_ALL = env.LC_ALL || env.LANG;
  Object.keys(extra || {}).forEach(function (key) {
    if (allowed.indexOf(key) >= 0 && typeof extra[key] === 'string' &&
        Buffer.byteLength(extra[key], 'utf8') <= 8192 &&
        extra[key].indexOf('\0') < 0) env[key] = extra[key];
  });
  return env;
}

function validInvocation(exe, argv, cwd) {
  if (!path.isAbsolute(String(exe || '')) || !Array.isArray(argv) || argv.length > 128 ||
      !path.isAbsolute(String(cwd || ''))) throw new Error('invalid typed process invocation');
  var verifiedExecutable = executable(exe);
  if (!verifiedExecutable || verifiedExecutable !== exe) throw new Error('typed process executable is no longer trusted');
  var cwdReal = fs.realpathSync(cwd);
  var projectReal = fs.realpathSync(paths.PROJECT_ROOT);
  var rel = path.relative(projectReal, cwdReal);
  if (rel === '..' || rel.indexOf('..' + path.sep) === 0 || path.isAbsolute(rel)) {
    throw new Error('typed process cwd is outside the project root');
  }
  argv.forEach(function (arg) {
    if (typeof arg !== 'string' || arg.length > 8192 || arg.indexOf('\0') >= 0) {
      throw new Error('invalid typed process argument');
    }
  });
}

function bounded(value) {
  var bytes = Buffer.from(String(value || ''), 'utf8');
  if (bytes.length <= MAX_OUTPUT_BYTES) return bytes.toString('utf8');
  return bytes.slice(bytes.length - MAX_OUTPUT_BYTES).toString('utf8');
}

function typedInput(value) {
  if (value === undefined) return undefined;
  if (Buffer.isBuffer(value)) {
    if (value.length > 8 * 1024 * 1024) throw new Error('typed process input is oversized');
    return value;
  }
  if (typeof value !== 'string' || Buffer.byteLength(value, 'utf8') > 8 * 1024 * 1024 ||
      value.indexOf('\0') >= 0) {
    throw new Error('typed process input is invalid');
  }
  return value;
}

function runSync(spec) {
  validInvocation(spec.executable, spec.argv || [], spec.cwd);
  var input = typedInput(spec.input);
  var result = childProcess.spawnSync(spec.executable, spec.argv || [], {
    cwd: spec.cwd,
    env: minimalEnv(spec.env),
    shell: false,
    input: input,
    encoding: 'utf8',
    timeout: Math.max(1000, Math.min(Number(spec.timeoutMs || DEFAULT_TIMEOUT), 30 * 60 * 1000)),
    maxBuffer: MAX_OUTPUT_BYTES,
    windowsHide: true
  });
  return {
    ok: !result.error && result.status === 0,
    status: Number.isInteger(result.status) ? result.status : null,
    signal: result.signal || null,
    timedOut: !!(result.error && result.error.code === 'ETIMEDOUT'),
    // Bounded raw output is private parser input. Redaction happens before
    // any line is persisted or published; replacing serials/UDIDs here would
    // destroy the exact identifiers that discovery must validate.
    stdout: bounded(result.stdout),
    stderr: bounded(result.stderr),
    errorCode: result.error ? String(result.error.code || 'spawn-failed') : null
  };
}

function terminate(child) {
  if (!child || !Number.isInteger(child.pid)) return;
  try {
    if (process.platform !== 'win32') process.kill(-child.pid, 'SIGTERM');
    else child.kill('SIGTERM');
  } catch (_) {}
  // The process group is one we just spawned (or an identity-verified owned
  // emulator). Escalate before returning control so a timed-out build cannot
  // outlive the writer lease and race a later project mutation.
  try {
    if (process.platform !== 'win32') process.kill(-child.pid, 'SIGKILL');
    else child.kill('SIGKILL');
  } catch (_) {}
}

function processGroupAlive(child) {
  if (!child || !Number.isInteger(child.pid) || process.platform === 'win32') return false;
  try {
    process.kill(-child.pid, 0);
    return true;
  } catch (error) {
    return !error || error.code !== 'ESRCH';
  }
}

function afterTerminationDrains(child, callback) {
  if (process.platform === 'win32') { callback(); return; }
  function poll() {
    if (!processGroupAlive(child)) { callback(); return; }
    setTimeout(poll, 50);
  }
  poll();
}

function exactProcessIdentity(child) {
  var startId = writerLeases.captureProcessStartId(child.pid);
  if (!writerLeases.PROCESS_START_ID_RE.test(String(startId || ''))) {
    var error = new Error('Exact child process-start identity is unavailable');
    error.code = 'process-identity-unavailable';
    throw error;
  }
  return { pid: child.pid, processStartId: startId };
}

function run(spec) {
  validInvocation(spec.executable, spec.argv || [], spec.cwd);
  var input = typedInput(spec.input);
  return new Promise(function (resolve) {
    var child;
    var stdout = '', stderr = '', settled = false, timer = null;
    var timedOut = false, aborted = false, terminationRequested = false, childError = null;
    var pendingLines = { stdout: '', stderr: '' };
    var overlongLines = { stdout: false, stderr: false };
    function deliverLine(source, value) {
      if (typeof spec.onLine !== 'function') return;
      try {
        spec.onLine(source, redaction.line(value));
      } catch (lineError) {
        childError = lineError;
        terminationRequested = true;
        terminate(child);
      }
    }
    function feedLines(source, value) {
      var text = String(value || '');
      if (overlongLines[source]) {
        var boundary = text.indexOf('\n');
        if (boundary < 0) return;
        deliverLine(source, 'Output line omitted because it exceeded the safe limit.');
        overlongLines[source] = false;
        text = text.slice(boundary + 1);
      }
      var parts = (pendingLines[source] + text).split('\n');
      pendingLines[source] = parts.pop();
      parts.forEach(function (line) {
        if (line.charAt(line.length - 1) === '\r') line = line.slice(0, -1);
        if (Buffer.byteLength(line, 'utf8') > 16384) {
          deliverLine(source, 'Output line omitted because it exceeded the safe limit.');
        } else if (line) {
          deliverLine(source, line);
        }
      });
      if (Buffer.byteLength(pendingLines[source], 'utf8') > 16384) {
        pendingLines[source] = '';
        overlongLines[source] = true;
      }
    }
    function flushLines() {
      ['stdout', 'stderr'].forEach(function (source) {
        if (overlongLines[source]) {
          deliverLine(source, 'Output line omitted because it exceeded the safe limit.');
        } else if (pendingLines[source]) {
          deliverLine(source, pendingLines[source].replace(/\r$/, ''));
        }
        pendingLines[source] = '';
        overlongLines[source] = false;
      });
    }
    function emit(source, chunk) {
      var text = String(chunk || '');
      if (source === 'stdout') stdout = bounded(stdout + text);
      else stderr = bounded(stderr + text);
      feedLines(source, text);
    }
    function finish(result) {
      if (settled) return;
      settled = true;
      if (timer) clearTimeout(timer);
      if (spec.signal && abort) spec.signal.removeEventListener('abort', abort);
      resolve(result);
    }
    function abort() {
      aborted = true;
      terminationRequested = true;
      terminate(child);
    }
    try {
      if (typeof spec.beforeSpawn === 'function') spec.beforeSpawn();
      child = childProcess.spawn(spec.executable, spec.argv || [], {
        cwd: spec.cwd,
        env: minimalEnv(spec.env),
        shell: false,
        detached: process.platform !== 'win32',
        stdio: ['pipe', 'pipe', 'pipe'],
        windowsHide: true
      });
    } catch (error) {
      finish({ ok: false, status: null, signal: null, timedOut: false, stdout: '', stderr: '',
        errorCode: String(error && error.code || 'spawn-failed') });
      return;
    }
    child.stdout.on('data', function (chunk) { emit('stdout', chunk); });
    child.stderr.on('data', function (chunk) { emit('stderr', chunk); });
    child.stdin.on('error', function () {});
    child.on('error', function (error) { childError = error; });
    child.on('close', function (status, signal) {
      flushLines();
      var value = {
        ok: !terminationRequested && !childError && status === 0,
        status: Number.isInteger(status) ? status : null,
        signal: signal || null,
        timedOut: timedOut,
        stdout: stdout,
        stderr: stderr,
        errorCode: timedOut ? 'timeout' : aborted ? 'cancelled'
          : childError ? String(childError.code || 'spawn-failed') : null
      };
      if (terminationRequested) afterTerminationDrains(child, function () { finish(value); });
      else finish(value);
    });
    if (typeof spec.onSpawn === 'function') {
      try {
        spec.onSpawn(exactProcessIdentity(child));
      } catch (spawnFenceError) {
        childError = spawnFenceError;
        terminationRequested = true;
        terminate(child);
      }
    }
    if (!terminationRequested) {
      if (input !== undefined) child.stdin.end(input);
      else child.stdin.end();
    } else {
      child.stdin.destroy();
    }
    var timeout = Math.max(1000, Math.min(Number(spec.timeoutMs || DEFAULT_TIMEOUT), 30 * 60 * 1000));
    timer = setTimeout(function () {
      timedOut = true;
      terminationRequested = true;
      terminate(child);
    }, timeout);
    if (spec.signal) {
      if (spec.signal.aborted) abort();
      else spec.signal.addEventListener('abort', abort, { once: true });
    }
  });
}

function runBinary(spec) {
  validInvocation(spec.executable, spec.argv || [], spec.cwd);
  return new Promise(function (resolve) {
    var child, chunks = [], stderr = '', total = 0, settled = false, timer;
    var timedOut = false, aborted = false, outputLimited = false;
    var terminationRequested = false, childError = null;
    function finish(value) {
      if (settled) return;
      settled = true;
      if (timer) clearTimeout(timer);
      if (spec.signal && abort) spec.signal.removeEventListener('abort', abort);
      resolve(value);
    }
    function abort() {
      aborted = true;
      terminationRequested = true;
      terminate(child);
    }
    try {
      if (typeof spec.beforeSpawn === 'function') spec.beforeSpawn();
      child = childProcess.spawn(spec.executable, spec.argv || [], {
        cwd: spec.cwd, env: minimalEnv(spec.env), shell: false,
        detached: process.platform !== 'win32', windowsHide: true,
        stdio: ['ignore', 'pipe', 'pipe']
      });
    } catch (error) {
      finish({ ok: false, status: null, bytes: null, stderr: '', errorCode: String(error && error.code || 'spawn-failed') });
      return;
    }
    child.stdout.on('data', function (chunk) {
      total += chunk.length;
      if (total > (spec.maxBytes || 25 * 1024 * 1024)) {
        outputLimited = true;
        terminationRequested = true;
        terminate(child); return;
      }
      chunks.push(Buffer.from(chunk));
    });
    child.stderr.on('data', function (chunk) { stderr = bounded(stderr + String(chunk)); });
    child.on('error', function (error) { childError = error; });
    child.on('close', function (status) {
      var within = !outputLimited && total <= (spec.maxBytes || 25 * 1024 * 1024);
      var value = {
        ok: !terminationRequested && !childError && status === 0 && within,
        status: Number.isInteger(status) ? status : null,
        bytes: !terminationRequested && !childError && status === 0 && within
          ? Buffer.concat(chunks, total) : null,
        stderr: stderr,
        errorCode: timedOut ? 'timeout' : aborted ? 'cancelled' : outputLimited ? 'output-limit'
          : childError ? String(childError.code || 'spawn-failed') : null
      };
      if (terminationRequested) afterTerminationDrains(child, function () { finish(value); });
      else finish(value);
    });
    if (typeof spec.onSpawn === 'function') {
      try {
        spec.onSpawn(exactProcessIdentity(child));
      } catch (spawnFenceError) {
        childError = spawnFenceError;
        terminationRequested = true;
        terminate(child);
      }
    }
    timer = setTimeout(function () {
      timedOut = true;
      terminationRequested = true;
      terminate(child);
    }, Math.max(1000, Math.min(Number(spec.timeoutMs || DEFAULT_TIMEOUT), 30 * 60 * 1000)));
    if (spec.signal) {
      if (spec.signal.aborted) abort();
      else spec.signal.addEventListener('abort', abort, { once: true });
    }
  });
}

function startDetached(spec) {
  validInvocation(spec.executable, spec.argv || [], spec.cwd);
  var child = childProcess.spawn(spec.executable, spec.argv || [], {
    cwd: spec.cwd, env: minimalEnv(spec.env), shell: false,
    detached: process.platform !== 'win32', windowsHide: true,
    stdio: ['ignore', 'ignore', 'ignore']
  });
  child.unref();
  var identity;
  try {
    identity = Object.freeze(exactProcessIdentity(child));
  } catch (identityError) {
    terminate(child);
    throw identityError;
  }
  ownedDetached.set(identity, child);
  // A trusted executable can still disappear between validation and exec.
  // Consume that asynchronous spawn error so it cannot crash the server; the
  // caller's bounded readiness probe will then return the typed start failure.
  child.once('error', function () { ownedDetached.delete(identity); });
  return identity;
}

function terminateIdentity(identity) {
  var owned = identity && typeof identity === 'object' && ownedDetached.get(identity);
  if (owned) {
    ownedDetached.delete(identity);
    terminate(owned);
    return true;
  }
  if (!identity || !Number.isSafeInteger(identity.pid) || identity.pid <= 0 ||
      !writerLeases.PROCESS_START_ID_RE.test(String(identity.processStartId || '')) ||
      !writerLeases.processIdentityMatches(identity.pid, identity.processStartId)) return false;
  terminate({ pid: identity.pid });
  return true;
}

module.exports = {
  MAX_OUTPUT_BYTES: MAX_OUTPUT_BYTES,
  resolveExecutable: resolveExecutable,
  minimalEnv: minimalEnv,
  runSync: runSync,
  run: run,
  runBinary: runBinary,
  startDetached: startDetached,
  terminateIdentity: terminateIdentity,
  terminate: terminate
};
