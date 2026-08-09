'use strict';

// Windows cannot derive process generations or private authority from POSIX
// mode bits.  This adapter binds native helper verdicts to exact file-guard
// proofs and exposes a deterministic seam for non-Windows contract tests.

var cp = require('child_process');
var fs = require('fs');
var path = require('path');
var os = require('os');

var HELPER = path.resolve(__dirname, '..', '..', 'tasks', 'windows-runtime-proof.py');
var PSID_RE = /^psid-v1:win32:[a-f0-9]{64}$/;
var DECIMAL_RE = /^(?:0|[1-9][0-9]*)$/;
var PROCESS_KEYS = ['pid', 'processStartId', 'reason', 'status', 'version'];
var ANCESTRY_KEYS = ['ancestorPid', 'depth', 'descendantPid', 'reason', 'status', 'version'];
var PATH_KEYS = ['dev', 'ino', 'pathType', 'reason', 'status', 'version'];
var PROCESS_REASONS = {
  ok: 1, 'not-found': 1, exited: 1, 'open-failed': 1, 'query-failed': 1, 'invalid-time': 1,
  'boot-query-failed': 1
};
var ANCESTRY_REASONS = {
  ok: 1, deadline: 1, cycle: 1, 'process-not-found': 1, 'process-exited': 1,
  'process-open-failed': 1, 'process-query-failed': 1, 'process-invalid-time': 1,
  'process-boot-query-failed': 1, 'descendant-generation': 1, 'ancestor-generation': 1,
  'parent-newer': 1, 'ancestry-changed': 1, 'parent-query-failed': 1, 'depth-limit': 1
};
var PATH_REASONS = {
  ok: 1, missing: 1, 'open-failed': 1, reparse: 1, owner: 1, 'null-dacl': 1,
  'dacl-unprotected': 1, 'ace-type': 1, 'ace-sid': 1, 'ace-size': 1,
  'ace-principal': 1, 'current-user-access': 1, 'identity-changed': 1, 'set-dacl': 1, api: 1
};

function exactKeys(value, expected) {
  return value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === expected.slice().sort().join('\0');
}

function sortedValue(value) {
  if (Array.isArray(value)) return value.map(sortedValue);
  if (!value || typeof value !== 'object') return value;
  var out = {};
  Object.keys(value).sort().forEach(function (key) { out[key] = sortedValue(value[key]); });
  return out;
}

function canonicalLine(value) { return JSON.stringify(sortedValue(value)) + '\n'; }
function validPid(pid) { return Number.isInteger(pid) && pid > 0 && pid <= 0x7fffffff; }

function resolvePython(configured) {
  if (configured !== undefined && (typeof configured !== 'string' || !path.isAbsolute(configured))) {
    throw new Error('Windows proof Python must be an absolute executable path');
  }
  if (configured) {
    var fixed = fs.realpathSync(configured);
    var fixedStat = fs.lstatSync(fixed);
    if (!fixedStat.isFile() || fixedStat.isSymbolicLink()) throw new Error('Windows proof Python is unsafe');
    return fixed;
  }
  // Resolve PATH once through the fixed system `where.exe`, then pin that
  // concrete regular file. Python isolation disables inherited import hooks.
  var systemRoot = process.env.SystemRoot || process.env.WINDIR;
  if (!systemRoot || !path.isAbsolute(systemRoot)) throw new Error('Windows system root is unavailable');
  var where = path.join(systemRoot, 'System32', 'where.exe');
  var lookup = cp.spawnSync(where, ['python.exe'], {
    cwd: os.tmpdir(), env: helperEnvironment(), encoding: 'utf8', timeout: 3000,
    maxBuffer: 16 * 1024, windowsHide: true
  });
  if (!lookup || lookup.error || lookup.status !== 0) throw new Error('Windows Python is unavailable');
  var candidates = String(lookup.stdout || '').split(/\r?\n/).filter(Boolean);
  for (var i = 0; i < candidates.length; i++) {
    try {
      if (!path.isAbsolute(candidates[i])) continue;
      var resolved = fs.realpathSync(candidates[i]);
      var stat = fs.lstatSync(resolved);
      if (stat.isFile() && !stat.isSymbolicLink()) return resolved;
    } catch (candidateError) {}
  }
  throw new Error('Windows Python has no safe absolute executable');
}

function helperEnvironment(source) {
  source = source || process.env;
  var env = {};
  ['SystemRoot', 'WINDIR', 'PATH', 'PATHEXT', 'TEMP', 'TMP'].forEach(function (key) {
    if (source[key] != null) env[key] = source[key];
  });
  env.PYTHONIOENCODING = 'utf-8:strict';
  env.PYTHONUTF8 = '1';
  return env;
}

function defaultRunnerFactory(python) {
  var executable;
  return function (args) {
    try {
      if (!executable) executable = resolvePython(python);
      return cp.spawnSync(executable, ['-I', '-B', HELPER].concat(args), {
        cwd: os.tmpdir(), env: helperEnvironment(), encoding: 'utf8', timeout: 5000,
        maxBuffer: 16 * 1024, windowsHide: true
      });
    } catch (error) {
      return { error: error, status: null, stdout: '', stderr: '' };
    }
  };
}

function decodeResult(result) {
  if (!result || result.error || result.status !== 0 || typeof result.stdout !== 'string' ||
      Buffer.byteLength(result.stdout, 'utf8') > 16 * 1024) return null;
  var value;
  try { value = JSON.parse(result.stdout); }
  catch (error) { return null; }
  return result.stdout === canonicalLine(value) ? value : null;
}

function validProcessVerdict(value, expectedPid) {
  if (!exactKeys(value, PROCESS_KEYS) || value.version !== 1 || value.pid !== expectedPid ||
      !PROCESS_REASONS[value.reason] || ['live', 'dead', 'unknown'].indexOf(value.status) < 0) return false;
  if (value.status === 'live') return value.reason === 'ok' && PSID_RE.test(String(value.processStartId || ''));
  if (value.processStartId !== null) return false;
  if (value.status === 'dead') return value.reason === 'not-found' || value.reason === 'exited';
  return value.reason === 'open-failed' || value.reason === 'query-failed' || value.reason === 'invalid-time' ||
    value.reason === 'boot-query-failed';
}

function validAncestryVerdict(value, descendantPid, ancestorPid) {
  if (!exactKeys(value, ANCESTRY_KEYS) || value.version !== 1 ||
      value.descendantPid !== descendantPid || value.ancestorPid !== ancestorPid ||
      !ANCESTRY_REASONS[value.reason] || ['match', 'mismatch', 'unknown'].indexOf(value.status) < 0) return false;
  if (value.status === 'match') return value.reason === 'ok' && Number.isSafeInteger(value.depth) && value.depth >= 0 && value.depth <= 64;
  return value.depth === null && value.reason !== 'ok';
}

function validPathVerdict(value) {
  if (!exactKeys(value, PATH_KEYS) || value.version !== 1 || !PATH_REASONS[value.reason] ||
      ['private', 'unsafe', 'missing', 'unknown'].indexOf(value.status) < 0) return false;
  var hasIdentity = DECIMAL_RE.test(String(value.dev || '')) && DECIMAL_RE.test(String(value.ino || '')) &&
    (value.pathType === 'file' || value.pathType === 'directory');
  var emptyIdentity = value.dev === null && value.ino === null && value.pathType === null;
  if (!hasIdentity && !emptyIdentity) return false;
  if (value.status === 'private') return hasIdentity && value.reason === 'ok';
  if (value.status === 'missing') return emptyIdentity && value.reason === 'missing';
  if (value.status === 'unsafe') return hasIdentity && value.reason !== 'ok' && value.reason !== 'missing' &&
    value.reason !== 'open-failed' && value.reason !== 'api' && value.reason !== 'set-dacl';
  return value.reason === 'open-failed' || value.reason === 'api' || value.reason === 'set-dacl';
}

function expectedIdentity(expected) {
  if (!expected || typeof expected !== 'object') return null;
  var dev = typeof expected.dev === 'string' ? expected.dev : String(expected.dev);
  var ino = typeof expected.ino === 'string' ? expected.ino : String(expected.ino);
  var type = expected.type;
  return DECIMAL_RE.test(dev) && DECIMAL_RE.test(ino) && (type === 'file' || type === 'directory')
    ? { dev: dev, ino: ino, type: type } : null;
}

function create(options) {
  options = options || {};
  var platform = options.platform || process.platform;
  var run = options.runHelper || defaultRunnerFactory(options.python);

  function call(args) { return decodeResult(run(args)); }

  function inspectProcess(pid) {
    if (!validPid(pid)) return { status: 'dead', processStartId: null };
    if (platform !== 'win32') return { status: 'unsupported', processStartId: null };
    var value = call(['process', String(pid)]);
    return validProcessVerdict(value, pid) ? { status: value.status, processStartId: value.processStartId } :
      { status: 'unknown', processStartId: null };
  }

  function captureProcessStartId(pid) {
    var verdict = inspectProcess(pid);
    if (verdict.status === 'live') return verdict.processStartId;
    if (verdict.status === 'dead') return null;
    var error = new Error('cannot capture exact Windows process generation');
    error.code = 'WINDOWS_PROCESS_IDENTITY_UNAVAILABLE';
    throw error;
  }

  function processIdentityState(pid, recordedStartId) {
    if (!validPid(pid)) return 'dead';
    if (platform !== 'win32') return 'unsupported';
    var verdict = inspectProcess(pid);
    if (verdict.status === 'dead') return 'dead';
    if (verdict.status !== 'live') return 'unknown';
    if (!PSID_RE.test(String(recordedStartId || ''))) return 'pid-live';
    return verdict.processStartId === recordedStartId ? 'match' : 'reused';
  }

  function processTreeProof(descendantPid, descendantStartId, ancestorPid, ancestorStartId) {
    if (platform !== 'win32' || !validPid(descendantPid) || !validPid(ancestorPid) ||
        !PSID_RE.test(String(descendantStartId || '')) || !PSID_RE.test(String(ancestorStartId || ''))) {
      return { ok: false, reason: 'exact Windows ancestry identity is missing' };
    }
    var value = call(['ancestry', String(descendantPid), descendantStartId,
      String(ancestorPid), ancestorStartId]);
    if (!validAncestryVerdict(value, descendantPid, ancestorPid)) {
      return { ok: false, reason: 'Windows ancestry proof is unavailable' };
    }
    if (value.status !== 'match') return { ok: false, reason: value.reason };
    return {
      ok: true, depth: value.depth,
      caller: { pid: descendantPid, startId: descendantStartId, pgid: null },
      authority: { pid: ancestorPid, startId: ancestorStartId, pgid: null }
    };
  }

  function pathState(mode, target, expected) {
    if (platform !== 'win32' || typeof target !== 'string' || !path.isAbsolute(target) || target.indexOf('\0') >= 0) {
      return 'unknown';
    }
    var proof = expectedIdentity(expected);
    if (!proof) return 'unknown';
    var value = call([mode, target, proof.dev, proof.ino, proof.type]);
    if (!validPathVerdict(value)) return 'unknown';
    if (value.dev !== null && (value.dev !== proof.dev || value.ino !== proof.ino || value.pathType !== proof.type)) {
      return 'changed';
    }
    return value.status;
  }

  function privatePathState(target, expected) { return pathState('private-path', target, expected); }
  function hardenPrivatePath(target, expected) { return pathState('harden-path', target, expected); }

  function recoverStaleOwner(options) {
    options = options || {};
    var guards = options.fileGuards;
    if (platform !== 'win32' || !guards || typeof guards.boundedRegularFileUnder !== 'function' ||
        typeof guards.unlinkRegularFileMatchingUnder !== 'function' ||
        typeof guards.reconcileGuardTransactionsUnder !== 'function' || typeof options.validateOwner !== 'function' ||
        typeof options.validateWorker !== 'function' || typeof options.root !== 'string' ||
        typeof options.directory !== 'string' || typeof options.file !== 'string' ||
        !Number.isSafeInteger(options.maxBytes) || options.maxBytes < 1 || typeof options.hostname !== 'string') {
      return { code: 'invalid-request', removed: false };
    }
    // A prior process may have died after durable detach. Reconcile the
    // transaction journal before interpreting a missing public name; only the
    // journal has enough embedded authority to settle that crash boundary.
    var recovery = guards.reconcileGuardTransactionsUnder(options.root, options.directory, {
      maxEntries: Number.isSafeInteger(options.maxEntries) ? options.maxEntries : 512,
      maxTransactions: Number.isSafeInteger(options.maxTransactions) ? options.maxTransactions : 512
    });
    if (!recovery || recovery.ok !== true) return { code: 'journal-unsafe', removed: false };
    var read = guards.boundedRegularFileUnder(options.root, options.directory, options.file, options.maxBytes);
    if (!read || !read.stat || String(read.stat.nlink) !== '1') return { code: 'unreadable', removed: false };
    var privacy = privatePathState(options.file, read.stat);
    if (privacy !== 'private') return { code: 'privacy-' + privacy, removed: false };
    var record;
    try {
      record = JSON.parse(read.bytes.toString('utf8'));
      if (!Buffer.from(JSON.stringify(record) + '\n', 'utf8').equals(read.bytes)) throw new Error('non-canonical');
    } catch (error) { return { code: 'invalid-record', removed: false }; }
    var worker = options.validateWorker(record);
    if (!worker && !options.validateOwner(record)) return { code: 'invalid-record', removed: false };
    if (record.hostname !== options.hostname) return { code: 'remote', removed: false };
    var ownerState = processIdentityState(record.pid, record.processStartId);
    if (ownerState === 'match' || ownerState === 'pid-live') return { code: 'active', removed: false };
    if (ownerState !== 'dead' && ownerState !== 'reused') return { code: 'owner-unknown', removed: false };
    if (worker && record.childPid !== null) {
      var childState = processIdentityState(record.childPid, record.childProcessStartId);
      if (childState === 'match' || childState === 'pid-live') return { code: 'child-active', removed: false };
      if (childState !== 'dead' && childState !== 'reused') return { code: 'child-unknown', removed: false };
    }
    var removed = guards.unlinkRegularFileMatchingUnder(options.root, options.directory, options.file,
      options.maxBytes, { bytes: read.bytes, proof: read.stat });
    return { code: removed ? 'removed' : 'changed', removed: removed };
  }

  return {
    captureProcessStartId: captureProcessStartId,
    hardenPrivatePath: hardenPrivatePath,
    inspectProcess: inspectProcess,
    privatePathState: privatePathState,
    processIdentityMatches: function (pid, recordedStartId) {
      return processIdentityState(pid, recordedStartId) === 'match';
    },
    processIdentityState: processIdentityState,
    processTreeProof: processTreeProof,
    recoverStaleOwner: recoverStaleOwner
  };
}

var runtime = create({ python: process.env.ORCHESTRATOR_WRITER_PYTHON || undefined });
module.exports = {
  PSID_RE: PSID_RE,
  captureProcessStartId: runtime.captureProcessStartId,
  create: create,
  hardenPrivatePath: runtime.hardenPrivatePath,
  inspectProcess: runtime.inspectProcess,
  privatePathState: runtime.privatePathState,
  processIdentityMatches: runtime.processIdentityMatches,
  processIdentityState: runtime.processIdentityState,
  processTreeProof: runtime.processTreeProof,
  recoverStaleOwner: runtime.recoverStaleOwner
};
