'use strict';

// Cross-process publication handshake for workspace writers.
//
// A writer publishes one small, uniquely-named lease BEFORE it re-checks the
// finalization marker/mutex. finalize-task acquires its kernel mutex first and
// then scans these leases. Therefore either:
//   - the writer sees the finalizer and withdraws its lease before mutating, or
//   - the finalizer sees the already-published writer and refuses to start.
// There is no check -> spawn/write gap in which both sides can proceed.

var fs = require('fs');
var path = require('path');
var os = require('os');
var crypto = require('crypto');
var childProcess = require('child_process');
var fileGuards = require('../site/server/file-guards');
var windowsRuntimeProof = require('../site/server/windows-runtime-proof');
var taskSource = require('./task-source-contract.cjs');

var VERSION = 1;
var MAX_BYTES = 32 * 1024;
var MAX_TTL_MS = 60 * 60 * 1000;
var PENDING_CHILD_REASON = 'writer child binding is pending';
var LEASE_ID_RE = /^wr-[A-Za-z0-9][A-Za-z0-9._-]{15,159}$/;
var SESSION_ID_RE = /^ws-[A-Za-z0-9][A-Za-z0-9._-]{15,159}$/;
var PROCESS_START_ID_RE = /^psid-v1:(?:linux|darwin|win32):[a-f0-9]{64}$/;
var DELEGATION_HASH_RE = /^sha256:[a-f0-9]{64}$/;
var KINDS = {
  'task-session': 1,
  'workspace-session': 1,
  'figma-writer': 1,
  'standby-writer': 1,
  'lock-writer': 1,
  // Typed project-level writers. Every subsystem receives an exact kind;
  // callers cannot smuggle an arbitrary site-write label through this registry.
  'site-config': 1,
  'figma-sync': 1,
  'token-mappings': 1,
  'component-mappings': 1,
  'contract-refresh': 1,
  'api-mock-lifecycle': 1,
  'api-change-review': 1,
  'architecture-generate': 1,
  'runtime-build': 1
};
var MAX_PROCESS_BYTES = 16 * 1024;
var MAX_ANCESTRY_DEPTH = 64;
var MAX_ANCESTRY_MS = 3000;
var MAX_LEASE_ENTRIES = 1024;
var MAX_SCAN_MS = 8000;
var MAX_SCAN_RECORD_BYTES = 256 * 1024;
var OPERATION_ID_RE = /^[a-f0-9]{32}$/;
var RECOVERY_NAME_RE = /^\.wr-[A-Za-z0-9][A-Za-z0-9._-]{15,159}\.[a-f0-9]{32}\.lease-recovery$/;
var MUTATION_LOCK_RE = /^\.wr-[A-Za-z0-9][A-Za-z0-9._-]{15,159}\.mutation-lock$/;
var MUTATION_STAGE_RE = /^\.wr-[A-Za-z0-9][A-Za-z0-9._-]{15,159}\.[a-f0-9]{32}\.(?:manifest|receipt)\.mutation-stage$/;
var MUTATION_CANDIDATE_RE = /^\.wr-[A-Za-z0-9][A-Za-z0-9._-]{15,159}\.[a-f0-9]{32}\.lease-candidate$/;
var MUTATION_PUBLISHED_RE = /^\.wr-[A-Za-z0-9][A-Za-z0-9._-]{15,159}\.[a-f0-9]{32}\.mutation-published$/;
var MUTATION_CLEANUP_RE = /^\.wr-[A-Za-z0-9][A-Za-z0-9._-]{15,159}\.[a-f0-9]{32}\.(?:(?:lock|manifest|receipt|candidate|recovery)|reconcile-[a-f0-9]{24}\.(?:lock|manifest|receipt|candidate|recovery))\.mutation-(?:cleanup|capture)$/;
var MUTATION_PREFIX_RE = /^\.wr-/;
var MAX_MUTATION_BYTES = 192 * 1024;
var MUTATION_OPERATIONS = { 'child-binding': 1, 'unverified-mark': 1, renewal: 1, release: 1 };
var OPERATION_SLUG = { 'child binding': 'child-binding', 'unverified mark': 'unverified-mark', renewal: 'renewal', release: 'release' };
var linuxBootIdCache;
var darwinBootIdCache;
var darwinPythonCache;
var DARWIN_PROC_INFO_SCRIPT = [
  'import ctypes,sys',
  'class B(ctypes.Structure):',
  " _fields_=[('flags',ctypes.c_uint32),('status',ctypes.c_uint32),('xstatus',ctypes.c_uint32),('pid',ctypes.c_uint32),('ppid',ctypes.c_uint32),('uid',ctypes.c_uint32),('gid',ctypes.c_uint32),('ruid',ctypes.c_uint32),('rgid',ctypes.c_uint32),('svuid',ctypes.c_uint32),('svgid',ctypes.c_uint32),('rfu',ctypes.c_uint32),('comm',ctypes.c_char*16),('name',ctypes.c_char*32),('nfiles',ctypes.c_uint32),('pgid',ctypes.c_uint32),('pjobc',ctypes.c_uint32),('tdev',ctypes.c_uint32),('tpgid',ctypes.c_uint32),('nice',ctypes.c_int32),('sec',ctypes.c_uint64),('usec',ctypes.c_uint64)]",
  "p=int(sys.argv[1]); b=B(); f=ctypes.CDLL('/usr/lib/libproc.dylib',use_errno=True).proc_pidinfo",
  'f.argtypes=[ctypes.c_int,ctypes.c_int,ctypes.c_uint64,ctypes.c_void_p,ctypes.c_int]; f.restype=ctypes.c_int',
  'n=f(p,3,0,ctypes.byref(b),ctypes.sizeof(b))',
  "sys.exit(3) if ctypes.sizeof(b)!=136 or n!=136 or b.pid!=p or b.sec<=0 or b.usec>=1000000 else print(f'{b.pid} {b.ppid} {b.pgid} {b.sec} {b.usec}')"
].join('\n');

function now() { return new Date().toISOString(); }
function monotonicMs() { return Number(process.hrtime.bigint() / 1000000n); }
function randomId(prefix) { return prefix + '-' + crypto.randomBytes(18).toString('hex'); }
function createLeaseId() { return randomId('wr'); }
function createSessionId() { return randomId('ws'); }
function validPid(pid) { return Number.isInteger(pid) && pid > 0 && pid <= 0x7fffffff; }
function pidAlive(pid) {
  if (!validPid(pid)) return false;
  try { process.kill(pid, 0); return true; }
  // Only ESRCH proves absence. Permission and unexpected kernel errors are an
  // unknown owner state and must remain fail-closed.
  catch (e) { return !(e && e.code === 'ESRCH'); }
}
function processGroupAlive(pid) {
  if (process.platform === 'win32' || !validPid(pid)) return false;
  try { process.kill(-pid, 0); return true; }
  catch (e) { return !(e && e.code === 'ESRCH'); }
}
function processStartDigest(platform, parts) {
  return 'psid-v1:' + platform + ':' + crypto.createHash('sha256')
    .update(['writer-process-v1', platform].concat(parts).join('\0'), 'utf8').digest('hex');
}
function delegationHash(token) {
  return 'sha256:' + crypto.createHash('sha256').update(String(token), 'ascii').digest('hex');
}
function readBoundedFile(file, maxBytes) {
  var flags = fs.constants.O_RDONLY;
  if (fs.constants.O_NOFOLLOW) flags |= fs.constants.O_NOFOLLOW;
  var fd;
  try {
    fd = fs.openSync(file, flags);
    var chunks = [], total = 0;
    while (true) {
      var chunk = Buffer.alloc(Math.min(4096, maxBytes + 1 - total));
      var count = fs.readSync(fd, chunk, 0, chunk.length, null);
      if (!count) break;
      total += count;
      if (total > maxBytes) throw new Error('process metadata exceeds the size limit');
      chunks.push(chunk.slice(0, count));
    }
    return Buffer.concat(chunks, total);
  } finally {
    if (fd !== undefined) try { fs.closeSync(fd); } catch (e) {}
  }
}
function linuxBootId() {
  if (linuxBootIdCache) return linuxBootIdCache;
  var value = readBoundedFile('/proc/sys/kernel/random/boot_id', 128).toString('ascii').trim().toLowerCase();
  if (!/^[a-f0-9-]{16,80}$/.test(value)) throw new Error('Linux boot identity is invalid');
  linuxBootIdCache = value;
  return value;
}
function parseLinuxStat(raw, expectedPid) {
  raw = raw.toString('utf8').trim();
  var close = raw.lastIndexOf(')');
  var prefix = close > 0 ? raw.slice(0, close + 1).match(/^(\d+) \(.*\)$/s) : null;
  if (!prefix || Number(prefix[1]) !== expectedPid) throw new Error('Linux process metadata is invalid');
  var fields = raw.slice(close + 1).trim().split(/\s+/);
  // The suffix starts at proc(5) field 3 (state); starttime is field 22.
  if (fields.length < 20 || !/^\d+$/.test(fields[1]) || !/^\d+$/.test(fields[2]) || !/^\d+$/.test(fields[19])) {
    throw new Error('Linux process metadata is incomplete');
  }
  return {
    pid: expectedPid,
    ppid: Number(fields[1]),
    pgid: Number(fields[2]),
    startId: processStartDigest('linux', [linuxBootId(), String(expectedPid), fields[19]])
  };
}
function linuxProcessInfo(pid) {
  var file = '/proc/' + pid + '/stat';
  try { return parseLinuxStat(readBoundedFile(file, MAX_PROCESS_BYTES), pid); }
  catch (e) {
    if (e && (e.code === 'ENOENT' || e.code === 'ESRCH')) return null;
    throw e;
  }
}
function boundedSpawn(file, args) {
  var result = childProcess.spawnSync(file, args, {
    encoding: 'utf8', timeout: 1000, maxBuffer: MAX_PROCESS_BYTES,
    env: { PATH: '/usr/bin:/bin:/usr/sbin:/sbin', LANG: 'C', LC_ALL: 'C' }
  });
  if (result.error) throw result.error;
  return result;
}
function darwinBootId() {
  if (darwinBootIdCache) return darwinBootIdCache;
  var result = boundedSpawn('/usr/sbin/sysctl', ['-n', 'kern.bootsessionuuid']);
  if (result.status !== 0) throw new Error('cannot read Darwin boot identity');
  var value = String(result.stdout || '').trim().toLowerCase();
  if (!/^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/.test(value)) throw new Error('Darwin boot identity is invalid');
  darwinBootIdCache = value;
  return darwinBootIdCache;
}
function darwinPython() {
  if (darwinPythonCache) return darwinPythonCache;
  var configured = process.env.ORCHESTRATOR_WRITER_PYTHON || '';
  if (configured && !path.isAbsolute(configured)) {
    throw new Error('ORCHESTRATOR_WRITER_PYTHON must be an absolute executable path');
  }
  var candidates = [];
  if (configured) candidates.push(configured);
  if (process.env.VIRTUAL_ENV && path.isAbsolute(process.env.VIRTUAL_ENV)) {
    candidates.push(path.join(process.env.VIRTUAL_ENV, 'bin', 'python3'));
  }
  candidates.push('/opt/homebrew/bin/python3', '/usr/local/bin/python3', '/usr/bin/python3');
  for (var i = 0; i < candidates.length; i++) {
    try {
      var resolved = fs.realpathSync(candidates[i]);
      var st = fs.lstatSync(resolved);
      if (!st.isFile() || st.isSymbolicLink()) continue;
      fs.accessSync(resolved, fs.constants.X_OK);
      darwinPythonCache = resolved;
      return resolved;
    } catch (e) {
      if (configured && i === 0) throw new Error('configured writer identity Python is unavailable: ' + e.message);
    }
  }
  throw new Error('no absolute Python 3 runtime is available for Darwin process identity');
}
function darwinProcessInfo(pid) {
  var result = boundedSpawn(darwinPython(), ['-c', DARWIN_PROC_INFO_SCRIPT, String(pid)]);
  if (result.status !== 0 || !String(result.stdout || '').trim()) return null;
  var line = String(result.stdout).trim().replace(/\s+/g, ' ');
  var match = line.match(/^(\d+) (\d+) (\d+) (\d+) (\d+)$/);
  if (!match || Number(match[1]) !== pid || Number(match[4]) <= 0 || Number(match[5]) >= 1000000) {
    throw new Error('Darwin process metadata is invalid');
  }
  return {
    pid: pid,
    ppid: Number(match[2]),
    pgid: Number(match[3]),
    startId: processStartDigest('darwin', [darwinBootId(), String(pid), match[4], match[5]])
  };
}
function processInfo(pid) {
  if (!validPid(pid)) return null;
  if (process.platform === 'linux') return linuxProcessInfo(pid);
  if (process.platform === 'darwin') return darwinProcessInfo(pid);
  return null;
}
function captureProcessStartId(pid) {
  if (process.platform === 'win32') return windowsRuntimeProof.captureProcessStartId(pid);
  var info;
  try { info = processInfo(pid); }
  catch (e) {
    if (pidAlive(pid) && (process.platform === 'linux' || process.platform === 'darwin')) throw e;
    return null;
  }
  if (info) return info.startId;
  if (pidAlive(pid) && (process.platform === 'linux' || process.platform === 'darwin')) {
    throw new Error('cannot capture process-start identity for live PID ' + pid);
  }
  return null;
}
function processInstanceState(pid, recordedStartId, cache) {
  if (!validPid(pid)) return 'dead';
  if (process.platform === 'win32') {
    var windowsKey = String(pid), verdict;
    try {
      if (cache && Object.prototype.hasOwnProperty.call(cache, windowsKey)) verdict = cache[windowsKey];
      else {
        verdict = windowsRuntimeProof.inspectProcess(pid);
        if (cache) cache[windowsKey] = verdict;
      }
    } catch (windowsError) { return 'unknown'; }
    if (!verdict || verdict.status === 'unknown' || verdict.status === 'unsupported') return 'unknown';
    if (verdict.status === 'dead') return 'dead';
    if (verdict.status !== 'live' || !windowsRuntimeProof.PSID_RE.test(String(verdict.processStartId || ''))) return 'unknown';
    if (!windowsRuntimeProof.PSID_RE.test(String(recordedStartId || ''))) return 'pid-live';
    return verdict.processStartId === recordedStartId ? 'match' : 'reused';
  }
  if (!PROCESS_START_ID_RE.test(String(recordedStartId || ''))) return pidAlive(pid) ? 'pid-live' : 'dead';
  var key = String(pid);
  var info;
  try {
    if (cache && Object.prototype.hasOwnProperty.call(cache, key)) info = cache[key];
    else {
      info = processInfo(pid);
      if (cache) cache[key] = info;
    }
  } catch (e) { return pidAlive(pid) ? 'unknown' : 'dead'; }
  if (!info) return pidAlive(pid) ? 'unknown' : 'dead';
  return info.startId === recordedStartId ? 'match' : 'reused';
}
function processIdentityState(pid, recordedStartId) {
  if (process.platform === 'win32') return windowsRuntimeProof.processIdentityState(pid, recordedStartId);
  if (process.platform !== 'linux' && process.platform !== 'darwin') {
    if (!validPid(pid) || !pidAlive(pid)) return 'dead';
    return PROCESS_START_ID_RE.test(String(recordedStartId || '')) ? 'unsupported' : 'pid-live';
  }
  return processInstanceState(pid, recordedStartId, null);
}
function processIdentityMatches(pid, recordedStartId) {
  // Preserve the pre-existing liveness contract on platforms where this
  // module has no start-generation primitive. Delegable nested site authority
  // still fails closed because processTreeProof requires exact IDs.
  if (process.platform === 'win32') return windowsRuntimeProof.processIdentityMatches(pid, recordedStartId);
  if (process.platform !== 'linux' && process.platform !== 'darwin') return pidAlive(pid);
  return processInstanceState(pid, recordedStartId, null) === 'match';
}
function processTreeProof(descendantPid, descendantStartId, ancestorPid, ancestorStartId) {
  if (process.platform === 'win32') {
    return windowsRuntimeProof.processTreeProof(
      descendantPid, descendantStartId, ancestorPid, ancestorStartId);
  }
  if (!validPid(descendantPid) || !validPid(ancestorPid) ||
      !PROCESS_START_ID_RE.test(String(descendantStartId || '')) ||
      !PROCESS_START_ID_RE.test(String(ancestorStartId || ''))) {
    return { ok: false, reason: 'exact process-start identity is missing' };
  }
  var chain = [], seen = Object.create(null), currentPid = descendantPid;
  var deadline = monotonicMs() + MAX_ANCESTRY_MS;
  try {
    for (var depth = 0; depth <= MAX_ANCESTRY_DEPTH; depth++) {
      if (monotonicMs() > deadline) return { ok: false, reason: 'process ancestry verification exceeded its deadline' };
      if (seen[currentPid]) return { ok: false, reason: 'process ancestry contains a cycle' };
      seen[currentPid] = true;
      var info = processInfo(currentPid);
      if (!info) return { ok: false, reason: 'process ancestry ended before the delegated authority' };
      if (depth === 0 && info.startId !== descendantStartId) {
        return { ok: false, reason: 'caller PID belongs to another process generation' };
      }
      chain.push(info);
      if (currentPid === ancestorPid) {
        if (info.startId !== ancestorStartId) {
          return { ok: false, reason: 'authority PID belongs to another process generation' };
        }
        // Re-read every edge. A death, reparent, or reuse during traversal is
        // an ambiguous proof and therefore cannot mint a delegation receipt.
        for (var i = 0; i < chain.length; i++) {
          if (monotonicMs() > deadline) return { ok: false, reason: 'process ancestry verification exceeded its deadline' };
          var again = processInfo(chain[i].pid);
          if (!again || again.startId !== chain[i].startId || again.ppid !== chain[i].ppid) {
            return { ok: false, reason: 'process ancestry changed while being verified' };
          }
        }
        return { ok: true, depth: depth, caller: chain[0], authority: info };
      }
      if (!validPid(info.ppid) || info.ppid === currentPid) break;
      currentPid = info.ppid;
    }
  } catch (e) {
    return { ok: false, reason: bounded(e && e.message || e, 240) };
  }
  return { ok: false, reason: 'caller is outside the delegated site process tree' };
}
function bounded(value, max) {
  var s = String(value == null ? '' : value);
  max = max || 500;
  return s.length <= max ? s : s.slice(0, max - 1) + '…';
}
function sameDirectoryIdentity(a, b) {
  return a && b && a.dev === b.dev && a.ino === b.ino &&
    a.isDirectory() && b.isDirectory() && !a.isSymbolicLink() && !b.isSymbolicLink();
}
function pathContained(root, target) {
  var relative = path.relative(root, target);
  return relative === '' || (relative !== '..' && relative.slice(0, 3) !== '..' + path.sep && !path.isAbsolute(relative));
}
function inferredScopeRoot(dir) {
  var target = path.resolve(dir);
  var candidates = [process.env.FINALIZE_PROJECT_ROOT, process.env.ORCHESTRATOR_PROJECT_ROOT];
  for (var i = 0; i < candidates.length; i++) {
    if (!candidates[i]) continue;
    var candidate = path.resolve(candidates[i]);
    if (pathContained(candidate, target)) return candidate;
  }
  // With no configured project root, the parent of finalizations is the
  // caller-supplied trust boundary. Production callers pass their project root
  // and therefore also fence .cache/tasks/finalizations.
  return path.dirname(path.dirname(target));
}
function scopeRoot(dir, supplied) {
  var value = supplied && typeof supplied === 'object' ? supplied.rootDir : supplied;
  var root = path.resolve(value || inferredScopeRoot(dir));
  var target = path.resolve(dir);
  if (!pathContained(root, target)) throw new Error('writer lease path escapes its authority root');
  return root;
}
function revalidateSafeDir(proof) {
  for (var i = 0; i < proof.identities.length; i++) {
    var current = fs.lstatSync(proof.identities[i].path, { bigint: true });
    if (!sameDirectoryIdentity(proof.identities[i].stat, current)) {
      throw new Error('writer lease directory ancestry changed identity');
    }
  }
  return proof;
}
function inspectSafeDir(dir, suppliedRoot) {
  var target = path.resolve(dir);
  var root = scopeRoot(target, suppliedRoot);
  var rootStat = fs.lstatSync(root, { bigint: true });
  if (rootStat.isSymbolicLink() || !rootStat.isDirectory()) {
    throw new Error('writer lease authority root must be a real directory');
  }
  var identities = [{ path: root, stat: rootStat }];
  var relative = path.relative(root, target);
  var parts = relative ? relative.split(path.sep) : [];
  var currentPath = root;
  for (var i = 0; i < parts.length; i++) {
    if (!parts[i] || parts[i] === '.' || parts[i] === '..') throw new Error('writer lease path is invalid');
    currentPath = path.join(currentPath, parts[i]);
    var st;
    try { st = fs.lstatSync(currentPath, { bigint: true }); }
    catch (e) {
      if (!e || e.code !== 'ENOENT') throw e;
      var missingProof = { root: root, dir: target, identities: identities, missing: true };
      revalidateSafeDir(missingProof);
      return missingProof;
    }
    if (st.isSymbolicLink() || !st.isDirectory()) {
      throw new Error('writer lease path component must be a real directory: ' + bounded(parts[i], 120));
    }
    identities.push({ path: currentPath, stat: st });
  }
  var proof = { root: root, dir: target, identities: identities, missing: false };
  revalidateSafeDir(proof);
  return proof;
}
function guardedDirectory(dir, suppliedRoot, create) {
  var target = path.resolve(dir);
  var root = scopeRoot(target, suppliedRoot);
  var ancestry = inspectSafeDir(target, root);
  if (ancestry.missing && create !== true) throw new Error('writer lease directory is missing');
  var directoryProof = fileGuards.writerLeaseDirectoryProofUnder(root, target, { create: create === true, mode: 0o700 });
  if (!directoryProof) throw new Error('writer lease directory cannot be pinned to its authority root');
  return { root: root, dir: target, directoryProof: directoryProof };
}
function guardedNames(proof) {
  var result = fileGuards.writerLeaseNamesUnder(proof.root, proof.dir, MAX_LEASE_ENTRIES, proof.directoryProof);
  if (!result.ok) {
    var error = new Error('writer lease directory cannot be enumerated safely: ' + String(result.code || 'unknown'));
    if (result.code === 'directory-entry-limit') error.writerLeaseCode = 'WRITER_LEASE_SCAN_LIMIT';
    throw error;
  }
  return result.names.slice().sort();
}
function proofHash(bytes) { return 'sha256:' + crypto.createHash('sha256').update(bytes).digest('hex'); }
function validProof(value, allowDirectory) {
  var fields = ['ctimeNs', 'dev', 'hash', 'ino', 'mode', 'mtimeNs', 'nlink', 'size', 'type'];
  if (!value || typeof value !== 'object' || Array.isArray(value) || Object.keys(value).sort().join('\0') !== fields.join('\0')) return false;
  if (value.type !== 'file' || !/^sha256:[a-f0-9]{64}$/.test(String(value.hash || ''))) return false;
  return ['ctimeNs', 'dev', 'ino', 'mode', 'mtimeNs', 'nlink', 'size'].every(function (field) {
    return typeof value[field] === 'string' && /^(?:0|[1-9][0-9]*)$/.test(value[field]);
  });
}
function proofExact(left, right) {
  return validProof(left) && validProof(right) && Object.keys(left).every(function (field) { return left[field] === right[field]; });
}
function proofLineage(left, right) {
  return validProof(left) && validProof(right) && ['dev', 'ino', 'mode', 'size', 'mtimeNs', 'hash', 'type'].every(function (field) {
    return left[field] === right[field];
  });
}
function guardedRead(proof, file, maxBytes) {
  return fileGuards.writerLeaseReadUnder(proof.root, proof.dir, file, maxBytes, proof.directoryProof);
}
function guardedStage(proof, file, bytes, maxBytes) {
  var result = fileGuards.writerLeaseStageUnder(proof.root, proof.dir, file, bytes, {
    directoryProof: proof.directoryProof, maxBytes: maxBytes, mode: 0o600
  });
  if (!result.ok) throw new Error('writer lease stage publication failed: ' + String(result.code || 'unknown'));
  return result.proof;
}
function guardedLink(proof, source, target, sourceProof, removeSource, capture, maxBytes) {
  var result = fileGuards.writerLeaseLinkUnder(proof.root, proof.dir, source, target, {
    directoryProof: proof.directoryProof, sourceProof: sourceProof, removeSource: removeSource === true,
    capture: capture, maxBytes: maxBytes
  });
  if (!result.ok) {
    var error = new Error('writer lease exact link/transfer failed: ' + String(result.code || 'unknown'));
    error.writerLeaseCode = result.code || 'link-failed';
    throw error;
  }
  return result.proof;
}
function guardedDelete(proof, file, fileProof, quarantine, capture, maxBytes) {
  var result = fileGuards.writerLeaseDeleteUnder(proof.root, proof.dir, file, quarantine, {
    directoryProof: proof.directoryProof, proof: fileProof, capture: capture, maxBytes: maxBytes
  });
  if (!result.ok) {
    var error = new Error('writer lease exact deletion failed: ' + String(result.code || 'unknown'));
    error.writerLeaseCode = result.code || 'delete-failed';
    throw error;
  }
}
function leasePath(dir, id) {
  if (!LEASE_ID_RE.test(String(id || ''))) throw new Error('invalid writer lease id');
  return path.join(dir, id + '.json');
}
function validateRecord(record, expectedId) {
  if (!record || typeof record !== 'object' || Array.isArray(record) || record.version !== VERSION) return 'writer lease must be a v1 object';
  var fields = ['childPid', 'childProcessStartId', 'createdAt', 'delegationHash', 'expiresAt', 'key', 'kind',
    'leaseId', 'owner', 'sessionId', 'stem', 'token', 'unverified', 'unverifiedReason', 'updatedAt', 'version'];
  if (!exactFields(record, fields) || !exactFields(record.owner, ['hostname', 'pid', 'processStartId', 'startedAt'])) {
    return 'writer lease fields do not match the exact v1 contract';
  }
  if (!LEASE_ID_RE.test(String(record.leaseId || '')) || (expectedId && record.leaseId !== expectedId)) return 'writer lease id is invalid';
  if (!/^[a-f0-9]{32,128}$/.test(String(record.token || ''))) return 'writer lease token is invalid';
  if (!Object.prototype.hasOwnProperty.call(KINDS, record.kind)) return 'writer lease kind is invalid';
  if (record.stem !== null && !taskSource.safeTaskStem(record.stem)) return 'writer lease stem is invalid';
  if (record.sessionId !== null && !SESSION_ID_RE.test(String(record.sessionId || ''))) return 'writer lease session id is invalid';
  if (record.key !== null && (typeof record.key !== 'string' || !record.key || record.key.length > 240 || /[\0\r\n]/.test(record.key))) return 'writer lease key is invalid';
  if (!record.owner || !validPid(record.owner.pid) || typeof record.owner.hostname !== 'string' || !record.owner.hostname ||
      typeof record.owner.startedAt !== 'string' || !Number.isFinite(Date.parse(record.owner.startedAt))) return 'writer lease owner is invalid';
  if (Object.prototype.hasOwnProperty.call(record.owner, 'processStartId') && record.owner.processStartId !== null &&
      !PROCESS_START_ID_RE.test(String(record.owner.processStartId || ''))) return 'writer lease owner process-start identity is invalid';
  if (record.childPid !== null && !validPid(record.childPid)) return 'writer lease child pid is invalid';
  if (Object.prototype.hasOwnProperty.call(record, 'childProcessStartId') && record.childProcessStartId !== null &&
      !PROCESS_START_ID_RE.test(String(record.childProcessStartId || ''))) return 'writer lease child process-start identity is invalid';
  if (record.childPid === null && record.childProcessStartId != null) return 'writer lease child process identity has no pid';
  if (Object.prototype.hasOwnProperty.call(record, 'delegationHash') && record.delegationHash !== null &&
      !DELEGATION_HASH_RE.test(String(record.delegationHash || ''))) return 'writer lease delegation hash is invalid';
  if (typeof record.unverified !== 'boolean') return 'writer lease process-tree proof state is invalid';
  if (record.unverifiedReason !== null && (typeof record.unverifiedReason !== 'string' || !record.unverifiedReason || record.unverifiedReason.length > 500)) return 'writer lease unverified reason is invalid';
  if ((record.unverified && record.unverifiedReason === null) || (!record.unverified && record.unverifiedReason !== null)) return 'writer lease unverified state/reason is inconsistent';
  if (typeof record.createdAt !== 'string' || !Number.isFinite(Date.parse(record.createdAt)) ||
      typeof record.updatedAt !== 'string' || !Number.isFinite(Date.parse(record.updatedAt))) return 'writer lease timestamps are invalid';
  if (record.expiresAt !== null && (typeof record.expiresAt !== 'string' || !Number.isFinite(Date.parse(record.expiresAt)))) return 'writer lease expiry is invalid';
  return null;
}
function readRecord(file, expectedId, guarded) {
  if (!guarded) throw new Error('writer lease reads require an anchored directory proof');
  var held = guardedRead(guarded, file, MAX_BYTES);
  if (!held) {
    var missing = new Error('writer lease is missing or unsafe');
    // The anchored inventory is the only authority for absence. Callers that
    // need idempotent missing handling must inspect that inventory first.
    missing.code = 'ENOENT_OR_UNSAFE';
    throw missing;
  }
  var value;
  try { value = JSON.parse(held.bytes.toString('utf8')); }
  catch (parseError) { throw new Error('writer lease is not valid JSON: ' + parseError.message); }
  var shape = validateRecord(value, expectedId);
  if (shape) throw new Error(shape);
  return { record: value, proof: held.proof, raw: held.bytes, rawBytes: held.bytes.length };
}
function handleScope(handle) {
  if (!handle || !handle.dir || !handle.path || !handle.leaseId) throw new Error('writer lease handle is invalid');
  var dir = path.resolve(handle.dir);
  var expected = leasePath(dir, handle.leaseId);
  if (path.resolve(handle.path) !== expected) throw new Error('writer lease handle path is outside its lease directory');
  return { dir: dir, rootDir: scopeRoot(dir, handle.rootDir) };
}
function testHook(handle, stage, details) {
  if (handle && typeof handle._testHook === 'function') handle._testHook(stage, details || {});
}
function tokenHash(token) { return proofHash(Buffer.from(String(token || ''), 'ascii')); }
function exactFields(value, fields) {
  return value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === fields.slice().sort().join('\0');
}
function recordBytes(record) { return Buffer.from(JSON.stringify(record, null, 2) + '\n'); }
function recordFromBytes(bytes, expectedId) {
  var value;
  try { value = JSON.parse(bytes.toString('utf8')); }
  catch (error) { throw new Error('writer lease WAL record bytes are not valid JSON'); }
  var shape = validateRecord(value, expectedId);
  if (shape) throw new Error(shape);
  return value;
}
function mutationPaths(dir, leaseId, operationId) {
  var prefix = '.' + leaseId + '.' + operationId + '.';
  return {
    lock: path.join(dir, '.' + leaseId + '.mutation-lock'),
    manifestStage: path.join(dir, prefix + 'manifest.mutation-stage'),
    candidate: path.join(dir, prefix + 'lease-candidate'),
    recovery: path.join(dir, prefix + 'lease-recovery'),
    receiptStage: path.join(dir, prefix + 'receipt.mutation-stage'),
    receipt: path.join(dir, prefix + 'mutation-published'),
    manifestCapture: path.join(dir, prefix + 'manifest.mutation-capture'),
    candidateCapture: path.join(dir, prefix + 'candidate.mutation-capture'),
    recoveryCapture: path.join(dir, prefix + 'recovery.mutation-capture'),
    receiptCapture: path.join(dir, prefix + 'receipt.mutation-capture'),
    lockCapture: path.join(dir, prefix + 'lock.mutation-capture'),
    manifestCleanup: path.join(dir, prefix + 'manifest.mutation-cleanup'),
    candidateCleanup: path.join(dir, prefix + 'candidate.mutation-cleanup'),
    recoveryCleanup: path.join(dir, prefix + 'recovery.mutation-cleanup'),
    receiptCleanup: path.join(dir, prefix + 'receipt.mutation-cleanup'),
    lockCleanup: path.join(dir, prefix + 'lock.mutation-cleanup')
  };
}
function mutationArtifactNames(names, leaseId) {
  var exactPrefix = '.' + leaseId + '.';
  var lockName = '.' + leaseId + '.mutation-lock';
  return names.filter(function (name) {
    return name === lockName || name.slice(0, exactPrefix.length) === exactPrefix;
  }).sort();
}
function mutationArtifactIdentity(name) {
  var lock = /^\.(wr-[A-Za-z0-9][A-Za-z0-9._-]{15,159})\.mutation-lock$/.exec(name);
  if (lock && LEASE_ID_RE.test(lock[1])) return { leaseId: lock[1], operationId: null };
  var operation = /^\.(wr-[A-Za-z0-9][A-Za-z0-9._-]{15,159})\.([a-f0-9]{32})\.(?:lease-(?:candidate|recovery)|(?:manifest|receipt)\.mutation-stage|mutation-published|(?:(?:lock|manifest|receipt|candidate|recovery)|reconcile-[a-f0-9]{24}\.(?:lock|manifest|receipt|candidate|recovery))\.mutation-(?:cleanup|capture))$/.exec(name);
  return operation && LEASE_ID_RE.test(operation[1])
    ? { leaseId: operation[1], operationId: operation[2] } : null;
}
function mutationHook(handle, operation, stage, details) {
  var label = operation + ':' + stage;
  testHook(handle, label, details || {});
  if (process.env.WRITER_LEASE_FAILPOINT === label) {
    try { process.kill(process.pid, 'SIGKILL'); }
    catch (error) { process.exit(86); }
  }
}
function validMutationOwner(value) {
  return exactFields(value, ['hostname', 'pid', 'processStartId']) && validPid(value.pid) &&
    typeof value.hostname === 'string' && value.hostname && value.hostname.length <= 255 &&
    PROCESS_START_ID_RE.test(String(value.processStartId || ''));
}
function validateManifest(value, expectedLeaseId, expectedDir) {
  var fields = ['candidateName', 'candidateProof', 'createdAt', 'leaseId', 'mutationOwner', 'newBytesBase64',
    'newHash', 'oldBytesBase64', 'oldHash', 'oldProof', 'operation', 'operationId', 'phase',
    'publishedName', 'recoveryName', 'tokenHash', 'version'];
  if (!exactFields(value, fields) || value.version !== 1 || value.phase !== 'prepared' || value.leaseId !== expectedLeaseId ||
      !OPERATION_ID_RE.test(String(value.operationId || '')) || !MUTATION_OPERATIONS[value.operation] ||
      !validMutationOwner(value.mutationOwner) || !/^sha256:[a-f0-9]{64}$/.test(String(value.tokenHash || '')) ||
      !/^sha256:[a-f0-9]{64}$/.test(String(value.oldHash || '')) || !validProof(value.oldProof) ||
      typeof value.oldBytesBase64 !== 'string' || typeof value.createdAt !== 'string' || !Number.isFinite(Date.parse(value.createdAt))) {
    throw new Error('writer lease mutation manifest is invalid');
  }
  var oldBytes = Buffer.from(value.oldBytesBase64, 'base64');
  if (oldBytes.toString('base64') !== value.oldBytesBase64 || oldBytes.length > MAX_BYTES || proofHash(oldBytes) !== value.oldHash ||
      value.oldProof.hash !== value.oldHash || value.oldProof.nlink !== '1') throw new Error('writer lease mutation old generation proof is invalid');
  if (typeof expectedDir !== 'string' || !path.isAbsolute(expectedDir) || path.resolve(expectedDir) !== expectedDir) {
    throw new Error('writer lease mutation authority directory is invalid');
  }
  var paths = mutationPaths(expectedDir, value.leaseId, value.operationId);
  if (value.recoveryName !== paths.recovery || value.publishedName !== paths.receipt ||
      (value.candidateName !== null && value.candidateName !== paths.candidate)) {
    throw new Error('writer lease mutation artifact path is outside its pinned directory');
  }
  if (value.operation === 'release') {
    if (value.newBytesBase64 !== null || value.newHash !== null || value.candidateName !== null || value.candidateProof !== null) {
      throw new Error('writer lease release manifest carries a candidate generation');
    }
  } else {
    if (typeof value.newBytesBase64 !== 'string' || !/^sha256:[a-f0-9]{64}$/.test(String(value.newHash || '')) ||
        value.candidateName !== paths.candidate || !validProof(value.candidateProof)) throw new Error('writer lease update manifest candidate is invalid');
    var nextBytes = Buffer.from(value.newBytesBase64, 'base64');
    if (nextBytes.toString('base64') !== value.newBytesBase64 || nextBytes.length > MAX_BYTES || proofHash(nextBytes) !== value.newHash ||
        value.candidateProof.hash !== value.newHash || value.candidateProof.nlink !== '1') throw new Error('writer lease mutation candidate proof is invalid');
  }
  recordFromBytes(oldBytes, value.leaseId);
  if (value.newBytesBase64 !== null) recordFromBytes(Buffer.from(value.newBytesBase64, 'base64'), value.leaseId);
  return value;
}
function validateReceipt(value, manifest) {
  var fields = ['leaseId', 'newHash', 'oldHash', 'operation', 'operationId', 'phase', 'publishedAt',
    'publishedProof', 'recoveryProof', 'tokenHash', 'version'];
  if (!exactFields(value, fields) || value.version !== 1 || value.phase !== 'published' ||
      value.leaseId !== manifest.leaseId || value.operationId !== manifest.operationId || value.operation !== manifest.operation ||
      value.tokenHash !== manifest.tokenHash || value.oldHash !== manifest.oldHash || value.newHash !== manifest.newHash ||
      typeof value.publishedAt !== 'string' || !Number.isFinite(Date.parse(value.publishedAt)) || !validProof(value.recoveryProof) ||
      !proofLineage(value.recoveryProof, manifest.oldProof)) {
    throw new Error('writer lease mutation publish receipt is invalid');
  }
  if (manifest.operation === 'release') {
    if (value.publishedProof !== null) throw new Error('writer lease release receipt claims a file generation');
  } else if (!validProof(value.publishedProof) || !proofLineage(value.publishedProof, manifest.candidateProof)) {
    throw new Error('writer lease mutation publish receipt does not bind its candidate inode');
  }
  return value;
}
function deleteMutationArtifact(proof, paths, role, file, maxBytes) {
  var read = guardedRead(proof, file, maxBytes);
  if (!read) return false;
  guardedDelete(proof, file, read.proof, paths[role + 'Cleanup'], paths[role + 'Capture'], maxBytes);
  return true;
}
function publishReceipt(handle, operation, proof, paths, manifest, recoveryProof, publishedProof) {
  var receipt = {
    version: 1, operationId: manifest.operationId, leaseId: manifest.leaseId, operation: manifest.operation,
    phase: 'published', tokenHash: manifest.tokenHash, oldHash: manifest.oldHash, newHash: manifest.newHash,
    recoveryProof: recoveryProof, publishedProof: publishedProof, publishedAt: now()
  };
  var bytes = Buffer.from(JSON.stringify(receipt, null, 2) + '\n');
  mutationHook(handle, operation, 'before-publish-receipt', { receipt: paths.receipt });
  var stagedProof = guardedStage(proof, paths.receiptStage, bytes, MAX_MUTATION_BYTES);
  mutationHook(handle, operation, 'after-receipt-stage', { receiptStage: paths.receiptStage });
  guardedLink(proof, paths.receiptStage, paths.receipt, stagedProof, true, paths.receiptCapture, MAX_MUTATION_BYTES);
  return receipt;
}
function mutateOwned(handle, operation, current, nextRecord, proof) {
  var slug = OPERATION_SLUG[operation];
  if (!slug) throw new Error('writer lease mutation operation is invalid');
  var names = guardedNames(proof);
  var existing = mutationArtifactNames(names, handle.leaseId);
  if (existing.length) {
    try { reconcileMutationInternal(handle); }
    catch (recoveryError) {
      throw new Error('writer lease mutation is already in progress or requires recovery: ' + existing[0] + '; ' + recoveryError.message);
    }
    throw new Error('writer lease prior mutation was reconciled; retry against the fresh canonical generation');
  }
  var operationId = crypto.randomBytes(16).toString('hex');
  var paths = mutationPaths(proof.dir, handle.leaseId, operationId);
  var ownerStart = captureProcessStartId(process.pid);
  if (!PROCESS_START_ID_RE.test(String(ownerStart || ''))) throw new Error('writer lease mutation owner generation is unavailable');
  var oldBytes = current.raw;
  if (!current.proof || current.proof.nlink !== '1') throw new Error('writer lease canonical generation has unexpected hard links');
  var nextBytes = nextRecord === null ? null : recordBytes(nextRecord);
  var candidateProof = null;
  if (nextBytes) {
    candidateProof = guardedStage(proof, paths.candidate, nextBytes, MAX_BYTES);
    if (candidateProof.nlink !== '1') throw new Error('writer lease candidate generation has unexpected hard links');
    mutationHook(handle, operation, 'after-candidate-stage', { candidate: paths.candidate });
  }
  var manifest = {
    version: 1, operationId: operationId, leaseId: handle.leaseId, operation: slug, phase: 'prepared',
    tokenHash: tokenHash(handle.token), mutationOwner: { pid: process.pid, hostname: os.hostname(), processStartId: ownerStart },
    oldHash: proofHash(oldBytes), oldProof: current.proof, oldBytesBase64: oldBytes.toString('base64'),
    newHash: nextBytes ? proofHash(nextBytes) : null, candidateName: nextBytes ? paths.candidate : null,
    candidateProof: candidateProof, newBytesBase64: nextBytes ? nextBytes.toString('base64') : null,
    recoveryName: paths.recovery, publishedName: paths.receipt, createdAt: now()
  };
  validateManifest(manifest, handle.leaseId, proof.dir);
  var manifestBytes = Buffer.from(JSON.stringify(manifest, null, 2) + '\n');
  var manifestStageProof = guardedStage(proof, paths.manifestStage, manifestBytes, MAX_MUTATION_BYTES);
  mutationHook(handle, operation, 'after-manifest-stage', { stage: paths.manifestStage });
  mutationHook(handle, operation, 'before-lock-publish', { lock: paths.lock });
  guardedLink(proof, paths.manifestStage, paths.lock, manifestStageProof, true, paths.manifestCapture, MAX_MUTATION_BYTES);
  mutationHook(handle, operation, 'after-lock-publish', { lock: paths.lock });
  mutationHook(handle, operation, 'after-read', { file: handle.path });
  var liveCurrent = readRecord(handle.path, handle.leaseId, proof);
  if (liveCurrent.record.token !== handle.token || !liveCurrent.raw.equals(oldBytes) || !proofExact(liveCurrent.proof, current.proof)) {
    throw new Error('writer lease replacement raced with ' + operation);
  }
  mutationHook(handle, operation, 'before-detach', { file: handle.path, recovery: paths.recovery });
  var recoveryProof = guardedLink(proof, handle.path, paths.recovery, liveCurrent.proof, true, paths.recoveryCapture, MAX_BYTES);
  mutationHook(handle, operation, 'after-detach', { file: handle.path, recovery: paths.recovery });
  var publishedProof = null;
  if (nextBytes) {
    mutationHook(handle, operation, 'before-publish', { file: handle.path, recovery: paths.recovery });
    var candidate = guardedRead(proof, paths.candidate, MAX_BYTES);
    if (!candidate || !proofExact(candidate.proof, candidateProof) || !candidate.bytes.equals(nextBytes)) {
      throw new Error('writer lease candidate generation changed before publication');
    }
    try {
      publishedProof = guardedLink(proof, paths.candidate, handle.path, candidate.proof, true, paths.candidateCapture, MAX_BYTES);
    } catch (publishError) {
      throw new Error('writer lease ' + operation + ' could not publish without clobbering a racing generation: ' + publishError.message);
    }
    mutationHook(handle, operation, 'after-publish', { file: handle.path, recovery: paths.recovery });
    var published = readRecord(handle.path, handle.leaseId, proof);
    if (!published.raw.equals(nextBytes) || !proofExact(published.proof, publishedProof) || !proofLineage(published.proof, candidateProof)) {
      throw new Error('published writer lease generation was replaced before CAS commit');
    }
  }
  publishReceipt(handle, operation, proof, paths, manifest, recoveryProof, publishedProof);
  mutationHook(handle, operation, 'after-publish-receipt', { receipt: paths.receipt });
  mutationHook(handle, operation, 'before-recovery-unlink', { recovery: paths.recovery });
  deleteMutationArtifact(proof, paths, 'recovery', paths.recovery, MAX_BYTES);
  mutationHook(handle, operation, 'after-recovery-unlink', { recovery: paths.recovery });
  deleteMutationArtifact(proof, paths, 'receipt', paths.receipt, MAX_MUTATION_BYTES);
  mutationHook(handle, operation, 'after-receipt-unlink', { receipt: paths.receipt });
  mutationHook(handle, operation, 'before-lock-unlink', { lock: paths.lock });
  deleteMutationArtifact(proof, paths, 'lock', paths.lock, MAX_MUTATION_BYTES);
  mutationHook(handle, operation, 'after-lock-unlink', { lock: paths.lock });
  if (nextRecord !== null) handle.record = nextRecord;
  return nextRecord === null ? true : handle;
}

function generationGone(record) {
  if (!record || !record.owner || record.owner.hostname !== os.hostname()) return false;
  var cache = Object.create(null);
  var ownerState = processInstanceState(record.owner.pid, record.owner.processStartId, cache);
  if (ownerState !== 'dead' && ownerState !== 'reused') return false;
  if (record.childPid === null) return !(record.unverified && record.unverifiedReason === PENDING_CHILD_REASON);
  var childState = processInstanceState(record.childPid, record.childProcessStartId, cache);
  if (childState !== 'dead' && childState !== 'reused') return false;
  if ((record.kind === 'task-session' || record.kind === 'workspace-session' ||
      record.kind === 'runtime-build') && childState !== 'reused' &&
      processGroupAlive(record.childPid)) return false;
  return true;
}
function exactOwnerGone(record) {
  if (!record || !record.owner || record.owner.hostname !== os.hostname() ||
      !PROCESS_START_ID_RE.test(String(record.owner.processStartId || ''))) return false;
  var state = processInstanceState(record.owner.pid, record.owner.processStartId, Object.create(null));
  return state === 'dead' || state === 'reused';
}
function validChildBindingTransition(oldRecord, newRecord) {
  if (!oldRecord || !newRecord || oldRecord.childPid !== null || oldRecord.childProcessStartId !== null ||
      oldRecord.unverified !== true || oldRecord.unverifiedReason !== PENDING_CHILD_REASON ||
      !validPid(newRecord.childPid) || !PROCESS_START_ID_RE.test(String(newRecord.childProcessStartId || '')) ||
      newRecord.unverified !== false || newRecord.unverifiedReason !== null) return false;
  var expected = JSON.parse(JSON.stringify(oldRecord));
  expected.childPid = newRecord.childPid;
  expected.childProcessStartId = newRecord.childProcessStartId;
  expected.unverified = false;
  expected.unverifiedReason = null;
  expected.updatedAt = newRecord.updatedAt;
  return JSON.stringify(expected) === JSON.stringify(newRecord);
}
function manifestGenerationsGone(manifest) {
  if (manifest.mutationOwner.hostname !== os.hostname()) return false;
  var ownerState = processInstanceState(manifest.mutationOwner.pid, manifest.mutationOwner.processStartId, Object.create(null));
  if (ownerState !== 'dead' && ownerState !== 'reused') return false;
  var oldRecord = recordFromBytes(Buffer.from(manifest.oldBytesBase64, 'base64'), manifest.leaseId);
  var newRecord = manifest.newBytesBase64 === null ? null :
    recordFromBytes(Buffer.from(manifest.newBytesBase64, 'base64'), manifest.leaseId);
  // A durable child-binding manifest closes the pending spawn gap itself: it
  // predeclares the one exact child generation which the old pending row could
  // not yet name. Once the mutation actor and that bound child/PGID are gone,
  // retaining the pending generation as "unknown forever" would turn every
  // attach crash into a manual writer-directory repair.
  if (manifest.operation === 'child-binding' && validChildBindingTransition(oldRecord, newRecord)) {
    return generationGone(newRecord);
  }
  if (!generationGone(oldRecord)) return false;
  if (newRecord !== null && !generationGone(newRecord)) return false;
  return true;
}
function regexEscape(value) { return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&'); }
function reconciliationRolePattern(paths, role) {
  var candidate = path.basename(paths.candidate);
  var match = /^\.(wr-[A-Za-z0-9][A-Za-z0-9._-]{15,159})\.([a-f0-9]{32})\.lease-candidate$/.exec(candidate);
  if (!match) throw new Error('writer lease mutation path set is invalid');
  return new RegExp('^\\.' + regexEscape(match[1]) + '\\.' + match[2] +
    '\\.reconcile-[a-f0-9]{24}\\.' + regexEscape(role) + '\\.mutation-(?:cleanup|capture)$');
}
function roleFiles(names, paths, role) {
  var bases = [paths[role], paths[role + 'Cleanup'], paths[role + 'Capture']].filter(Boolean)
    .map(function (file) { return path.basename(file); });
  var dynamic = reconciliationRolePattern(paths, role);
  return names.filter(function (name) { return bases.indexOf(name) >= 0 || dynamic.test(name); })
    .map(function (name) { return path.join(path.dirname(paths.lock), name); });
}
function readRole(proof, names, paths, role, maxBytes) {
  var files = roleFiles(names, paths, role), reads = files.map(function (file) {
    var held = guardedRead(proof, file, maxBytes);
    if (!held) throw new Error('writer lease mutation ' + role + ' artifact is unsafe');
    return { file: file, bytes: held.bytes, proof: held.proof };
  });
  if (!reads.length) return null;
  for (var i = 1; i < reads.length; i++) {
    if (!reads[i].bytes.equals(reads[0].bytes) || !proofLineage(reads[i].proof, reads[0].proof)) {
      throw new Error('writer lease mutation has conflicting ' + role + ' artifacts');
    }
  }
  return { primary: reads[0], all: reads };
}
function readReceiptStage(proof, names, paths) {
  if (names.indexOf(path.basename(paths.receiptStage)) < 0) return null;
  var held = guardedRead(proof, paths.receiptStage, MAX_MUTATION_BYTES);
  if (!held) throw new Error('writer lease mutation staged receipt is unsafe');
  return { file: paths.receiptStage, bytes: held.bytes, proof: held.proof };
}
function deleteExactArtifact(proof, manifest, role, item, maxBytes) {
  var random = crypto.randomBytes(12).toString('hex');
  var prefix = '.' + manifest.leaseId + '.' + manifest.operationId + '.reconcile-' + random + '.' + role;
  guardedDelete(proof, item.file, item.proof, path.join(proof.dir, prefix + '.mutation-cleanup'),
    path.join(proof.dir, prefix + '.mutation-capture'), maxBytes);
}
function reconciliationCapture(proof, manifest, role) {
  return path.join(proof.dir, '.' + manifest.leaseId + '.' + manifest.operationId + '.reconcile-' +
    crypto.randomBytes(12).toString('hex') + '.' + role + '.mutation-capture');
}
function deleteRole(proof, manifest, role, value, maxBytes) {
  if (!value) return;
  // Delete aliases one at a time, always by the freshly re-read exact proof.
  value.all.forEach(function (item) {
    var live = guardedRead(proof, item.file, maxBytes);
    if (!live) return;
    deleteExactArtifact(proof, manifest, role, { file: item.file, proof: live.proof }, maxBytes);
  });
}
function assertKnownLinkCount(items, label) {
  items = (items || []).filter(Boolean);
  if (!items.length) return;
  var seen = Object.create(null);
  items.forEach(function (item) {
    if (!item.file || seen[item.file]) throw new Error('writer lease ' + label + ' link inventory is ambiguous');
    seen[item.file] = true;
  });
  var expected = String(items.length);
  items.forEach(function (item) {
    if (!item.proof || item.proof.nlink !== expected) {
      throw new Error('writer lease ' + label + ' inode has an untracked hard link');
    }
  });
}
function restoreOldGeneration(proof, manifest, recovery) {
  var canonical = leasePath(proof.dir, manifest.leaseId);
  var live = guardedRead(proof, recovery.primary.file, MAX_BYTES);
  if (!live || !live.bytes.equals(Buffer.from(manifest.oldBytesBase64, 'base64')) || !proofLineage(live.proof, manifest.oldProof)) {
    throw new Error('writer lease detached old generation cannot be restored exactly');
  }
  var capture = path.join(proof.dir, '.' + manifest.leaseId + '.' + manifest.operationId + '.recovery.mutation-capture');
  var restoredProof = guardedLink(proof, recovery.primary.file, canonical, live.proof, false, capture, MAX_BYTES);
  var restored = guardedRead(proof, canonical, MAX_BYTES);
  if (!restored || !restored.bytes.equals(live.bytes) || !proofLineage(restored.proof, manifest.oldProof) || !proofLineage(restoredProof, manifest.oldProof)) {
    throw new Error('writer lease old generation restore was not stable');
  }
  return restored;
}
function rollForwardDeadChildBinding(proof, handle, manifest, paths, canonical, candidate, recovery) {
  if (!candidate || (!canonical && !recovery)) {
    throw new Error('writer lease child binding cannot roll forward without its candidate and old generation');
  }
  if (canonical && recovery) {
    // A worker crash may leave the old inode at both canonical and recovery
    // names. Remove only the exact canonical alias; the durable recovery alias
    // remains available if this reconciliation process dies on the next step.
    deleteExactArtifact(proof, manifest, 'recovery', { file: handle.path, proof: canonical.proof }, MAX_BYTES);
  } else if (canonical) {
    guardedLink(proof, handle.path, paths.recovery, canonical.proof, true,
      reconciliationCapture(proof, manifest, 'recovery'), MAX_BYTES);
  }
  var liveCandidate = guardedRead(proof, candidate.primary.file, MAX_BYTES);
  if (!liveCandidate || !liveCandidate.bytes.equals(Buffer.from(manifest.newBytesBase64, 'base64')) ||
      !proofLineage(liveCandidate.proof, manifest.candidateProof)) {
    throw new Error('writer lease child-binding candidate changed during recovery');
  }
  var publishedProof = guardedLink(proof, candidate.primary.file, handle.path, liveCandidate.proof, true,
    reconciliationCapture(proof, manifest, 'candidate'), MAX_BYTES);
  var liveRecovery = readRole(proof, guardedNames(proof), paths, 'recovery', MAX_BYTES);
  if (!liveRecovery) throw new Error('writer lease child-binding recovery inode disappeared');
  publishReceipt(handle, 'child binding', proof, paths, manifest, liveRecovery.primary.proof, publishedProof);
}
function extractManifestFromNames(proof, names, leaseId) {
  var lock = path.join(proof.dir, '.' + leaseId + '.mutation-lock');
  var candidates = names.filter(function (name) {
    return name === path.basename(lock) || name.slice(0, ('.' + leaseId + '.').length) === '.' + leaseId + '.' &&
      (MUTATION_STAGE_RE.test(name) || MUTATION_CLEANUP_RE.test(name));
  }).map(function (name) { return path.join(proof.dir, name); });
  var parsed = [];
  candidates.forEach(function (file) {
    var held = guardedRead(proof, file, MAX_MUTATION_BYTES);
    if (!held) return;
    try {
      var value = JSON.parse(held.bytes.toString('utf8'));
      validateManifest(value, leaseId, proof.dir);
      parsed.push({ file: file, value: value, proof: held.proof, bytes: held.bytes });
    } catch (error) {}
  });
  if (!parsed.length) throw new Error('writer lease mutation manifest is missing or malformed');
  var operationIds = Array.from(new Set(parsed.map(function (row) { return row.value.operationId; })));
  if (operationIds.length !== 1) throw new Error('writer lease mutation contains multiple operation generations');
  var first = parsed[0];
  parsed.forEach(function (row) {
    if (!row.bytes.equals(first.bytes) || !proofLineage(row.proof, first.proof)) {
      throw new Error('writer lease mutation manifest aliases conflict');
    }
  });
  assertKnownLinkCount(parsed, 'manifest');
  return { manifest: first.value, aliases: parsed };
}
function artifactAllowed(name, manifest, paths) {
  var allowed = Object.keys(paths).map(function (key) { return path.basename(paths[key]); });
  if (allowed.indexOf(name) >= 0) return true;
  var prefix = '.' + manifest.leaseId + '.' + manifest.operationId + '.reconcile-';
  return name.slice(0, prefix.length) === prefix && MUTATION_CLEANUP_RE.test(name);
}
function reconcileMutationInternal(handle) {
  var scope = handleScope(handle);
  var proof = guardedDirectory(scope.dir, scope.rootDir, false);
  var names = guardedNames(proof);
  var artifacts = mutationArtifactNames(names, handle.leaseId);
  if (!artifacts.length) return { reconciled: false, state: 'clean', leaseId: handle.leaseId };
  var extracted = extractManifestFromNames(proof, names, handle.leaseId);
  var manifest = extracted.manifest;
  if (manifest.tokenHash !== tokenHash(handle.token)) throw new Error('writer lease mutation token does not authenticate this generation');
  var oldRecord = recordFromBytes(Buffer.from(manifest.oldBytesBase64, 'base64'), manifest.leaseId);
  if (oldRecord.token !== handle.token) throw new Error('writer lease mutation old generation belongs to another token');
  if (!manifestGenerationsGone(manifest)) throw new Error('writer lease mutation owner or child process generation may still be alive');
  var paths = mutationPaths(proof.dir, manifest.leaseId, manifest.operationId);
  artifacts.forEach(function (name) {
    if (!artifactAllowed(name, manifest, paths)) throw new Error('writer lease mutation contains an unexpected or malformed artifact: ' + bounded(name, 180));
  });
  var recovery = readRole(proof, names, paths, 'recovery', MAX_BYTES);
  var candidate = readRole(proof, names, paths, 'candidate', MAX_BYTES);
  var receipt = readRole(proof, names, paths, 'receipt', MAX_MUTATION_BYTES);
  var receiptStage = readReceiptStage(proof, names, paths);
  if (recovery && (!recovery.primary.bytes.equals(Buffer.from(manifest.oldBytesBase64, 'base64')) ||
      !proofLineage(recovery.primary.proof, manifest.oldProof))) throw new Error('writer lease recovery artifact is not the exact old generation');
  if (candidate && (manifest.newBytesBase64 === null || !candidate.primary.bytes.equals(Buffer.from(manifest.newBytesBase64, 'base64')) ||
      !proofLineage(candidate.primary.proof, manifest.candidateProof))) throw new Error('writer lease candidate artifact is not the predeclared generation');
  var receiptValue = null;
  if (receipt) {
    try { receiptValue = JSON.parse(receipt.primary.bytes.toString('utf8')); }
    catch (error) { throw new Error('writer lease mutation receipt is malformed'); }
    validateReceipt(receiptValue, manifest);
  }
  var receiptStageValue = null;
  if (receiptStage) {
    try { receiptStageValue = JSON.parse(receiptStage.bytes.toString('utf8')); }
    catch (error2) { throw new Error('writer lease mutation staged receipt is malformed'); }
    validateReceipt(receiptStageValue, manifest);
    if (receipt && (!receiptStage.bytes.equals(receipt.primary.bytes) || !proofLineage(receiptStage.proof, receipt.primary.proof))) {
      throw new Error('writer lease mutation staged and published receipts conflict');
    }
  }
  names = guardedNames(proof);
  var canonicalName = handle.leaseId + '.json';
  var canonical = null;
  if (names.indexOf(canonicalName) >= 0) canonical = guardedRead(proof, handle.path, MAX_BYTES);
  if (names.indexOf(canonicalName) >= 0 && !canonical) throw new Error('writer lease canonical generation is unsafe');
  var oldBytes = Buffer.from(manifest.oldBytesBase64, 'base64');
  var newBytes = manifest.newBytesBase64 === null ? null : Buffer.from(manifest.newBytesBase64, 'base64');
  var canonicalOld = !!canonical && canonical.bytes.equals(oldBytes) && proofLineage(canonical.proof, manifest.oldProof);
  var canonicalNew = !!canonical && newBytes && canonical.bytes.equals(newBytes) && proofLineage(canonical.proof, manifest.candidateProof);
  if (canonical && !canonicalOld && !canonicalNew) throw new Error('writer lease canonical path contains a foreign generation');
  assertKnownLinkCount((canonicalOld ? [{ file: handle.path, proof: canonical.proof }] : []).concat(recovery ? recovery.all : []), 'old-generation');
  assertKnownLinkCount((canonicalNew ? [{ file: handle.path, proof: canonical.proof }] : []).concat(candidate ? candidate.all : []), 'candidate-generation');
  assertKnownLinkCount((receipt ? receipt.all : []).concat(receiptStage ? [receiptStage] : []), 'receipt');
  if (receipt && recovery && !proofLineage(receiptValue.recoveryProof, recovery.primary.proof)) {
    throw new Error('writer lease publish receipt conflicts with the retained recovery inode');
  }
  if (receipt && manifest.operation !== 'release' &&
      (!canonicalNew || !proofLineage(receiptValue.publishedProof, canonical.proof))) {
    throw new Error('writer lease publish receipt conflicts with the canonical candidate inode');
  }
  if (manifest.operation === 'child-binding' && candidate && !receipt &&
      (canonicalOld || (!canonical && recovery))) {
    rollForwardDeadChildBinding(proof, handle, manifest, paths, canonical, candidate, recovery);
    return reconcileMutationInternal(handle);
  }
  var state;
  if (manifest.operation === 'release') {
    if (receipt && canonical) throw new Error('writer lease committed release still has a canonical generation');
    if (canonicalOld && recovery && !receipt) state = 'release-detach-aborted';
    else if (canonicalOld && !recovery && !receipt) state = 'release-aborted-before-detach';
    else if (!canonical && recovery && !receipt) { restoreOldGeneration(proof, manifest, recovery); state = 'release-detach-restored'; }
    else if (!canonical && receipt) state = 'release-completed';
    else if (!canonical && !recovery && !receipt && !receiptStage) state = 'release-completed-cleanup';
    else throw new Error('writer lease release mutation is in an unsafe artifact lattice');
  } else {
    if (canonicalOld && receipt) throw new Error('writer lease committed update still exposes the old generation');
    if (canonicalOld && recovery && !receipt) state = 'update-detach-aborted';
    else if (canonicalOld && !recovery && !receipt) state = 'update-aborted-before-detach';
    else if (!canonical && recovery && !receipt) { restoreOldGeneration(proof, manifest, recovery); state = 'update-detach-restored'; }
    else if (canonicalNew) state = 'update-published';
    else throw new Error('writer lease update mutation is in an unsafe artifact lattice');
  }
  // Refresh after a possible restore, then remove only exact private artifacts.
  names = guardedNames(proof);
  recovery = readRole(proof, names, paths, 'recovery', MAX_BYTES);
  candidate = readRole(proof, names, paths, 'candidate', MAX_BYTES);
  receipt = readRole(proof, names, paths, 'receipt', MAX_MUTATION_BYTES);
  receiptStage = readReceiptStage(proof, names, paths);
  deleteRole(proof, manifest, 'recovery', recovery, MAX_BYTES);
  deleteRole(proof, manifest, 'candidate', candidate, MAX_BYTES);
  deleteRole(proof, manifest, 'receipt', receipt, MAX_MUTATION_BYTES);
  if (receiptStage) {
    var liveStage = guardedRead(proof, receiptStage.file, MAX_MUTATION_BYTES);
    if (liveStage && liveStage.bytes.equals(receiptStage.bytes)) {
      deleteExactArtifact(proof, manifest, 'receipt', { file: receiptStage.file, proof: liveStage.proof }, MAX_MUTATION_BYTES);
    }
  }
  // Manifest aliases/lock are deleted last; each must still contain the exact
  // authenticated manifest bytes.
  extracted.aliases.forEach(function (alias) {
    var live = guardedRead(proof, alias.file, MAX_MUTATION_BYTES);
    if (live && live.bytes.equals(alias.bytes)) deleteExactArtifact(proof, manifest, 'manifest', { file: alias.file, proof: live.proof }, MAX_MUTATION_BYTES);
  });
  var remaining = mutationArtifactNames(guardedNames(proof), handle.leaseId);
  if (remaining.length) throw new Error('writer lease mutation reconciliation retained ambiguous artifacts: ' + remaining[0]);
  var settledNames = guardedNames(proof);
  var settledCanonical = settledNames.indexOf(handle.leaseId + '.json') >= 0 ? guardedRead(proof, handle.path, MAX_BYTES) : null;
  if (settledCanonical && settledCanonical.proof.nlink !== '1') {
    throw new Error('writer lease reconciled canonical generation has an untracked hard link');
  }
  if (state.indexOf('release-completed') === 0) {
    if (settledCanonical) throw new Error('writer lease release reconciliation did not preserve committed absence');
  } else if (state === 'update-published') {
    if (!settledCanonical || !settledCanonical.bytes.equals(newBytes) || !proofLineage(settledCanonical.proof, manifest.candidateProof)) {
      throw new Error('writer lease update reconciliation lost the published candidate generation');
    }
  } else if (!settledCanonical || !settledCanonical.bytes.equals(oldBytes) || !proofLineage(settledCanonical.proof, manifest.oldProof)) {
    throw new Error('writer lease mutation reconciliation did not restore the old generation');
  }
  return { reconciled: true, state: state, leaseId: handle.leaseId, operationId: manifest.operationId };
}
function orphanCandidateOperationId(name, leaseId) {
  var match = new RegExp('^\\.' + regexEscape(leaseId) + '\\.([a-f0-9]{32})\\.(?:lease-candidate|candidate\\.mutation-(?:capture|cleanup)|reconcile-[a-f0-9]{24}\\.candidate\\.mutation-(?:capture|cleanup))$').exec(name);
  return match && match[1] || null;
}
function orphanCandidateSet(artifacts, leaseId) {
  if (!artifacts.length) return null;
  var operationIds = artifacts.map(function (name) { return orphanCandidateOperationId(name, leaseId); });
  if (operationIds.some(function (value) { return !value; })) return null;
  var unique = Array.from(new Set(operationIds));
  return unique.length === 1 ? unique[0] : null;
}
function reconcileOrphanCandidate(handle, proof, names) {
  var artifacts = mutationArtifactNames(names, handle.leaseId);
  var operationId = orphanCandidateSet(artifacts, handle.leaseId);
  if (!operationId) throw new Error('writer lease mutation manifest is missing or malformed');
  var heldAliases = artifacts.map(function (name) {
    var file = path.join(proof.dir, name);
    var held = guardedRead(proof, file, MAX_BYTES);
    if (!held) throw new Error('writer lease orphan candidate is unsafe');
    return { file: file, bytes: held.bytes, proof: held.proof };
  });
  for (var aliasIndex = 1; aliasIndex < heldAliases.length; aliasIndex++) {
    if (!heldAliases[aliasIndex].bytes.equals(heldAliases[0].bytes) ||
        !proofLineage(heldAliases[aliasIndex].proof, heldAliases[0].proof)) {
      throw new Error('writer lease orphan candidate aliases conflict');
    }
  }
  var record = recordFromBytes(heldAliases[0].bytes, handle.leaseId);
  if (record.token !== handle.token || tokenHash(record.token) !== tokenHash(handle.token)) throw new Error('writer lease orphan candidate belongs to another token');
  var canonicalName = handle.leaseId + '.json';
  var refreshedNames = guardedNames(proof);
  var canonical = refreshedNames.indexOf(canonicalName) >= 0 ? guardedRead(proof, handle.path, MAX_BYTES) : null;
  if (refreshedNames.indexOf(canonicalName) >= 0 && !canonical) throw new Error('writer lease orphan candidate canonical path is unsafe');
  // Before acquisition publishes the canonical name, its private pending
  // candidate cannot have been returned to a caller and therefore cannot have
  // authorized a child spawn. Once the exact owner generation is gone, that
  // one state may be aborted even though a generic pending record deliberately
  // cannot prove generationGone(). A published pending canonical remains
  // fail-closed because a spawn may already have occurred before PID attach.
  var unpublishedPendingAcquire = !canonical && record.childPid === null &&
    record.childProcessStartId === null && record.unverified === true &&
    record.unverifiedReason === PENDING_CHILD_REASON && exactOwnerGone(record);
  if (!generationGone(record) && !unpublishedPendingAcquire) {
    throw new Error('writer lease orphan candidate process generation may still be alive');
  }
  var candidatePublished = !!canonical && canonical.bytes.equals(heldAliases[0].bytes) &&
    proofLineage(canonical.proof, heldAliases[0].proof);
  var canonicalRecord = null;
  if (canonical && !candidatePublished) {
    canonicalRecord = recordFromBytes(canonical.bytes, handle.leaseId);
    if (canonicalRecord.token !== handle.token) {
      throw new Error('writer lease orphan candidate conflicts with a foreign canonical generation');
    }
  }
  assertKnownLinkCount(heldAliases.concat(candidatePublished ? [{ file: handle.path, proof: canonical.proof }] : []), 'orphan-candidate');
  if (canonical && !candidatePublished && canonical.proof.nlink !== '1') {
    throw new Error('writer lease canonical generation has an untracked hard link');
  }
  if (canonicalRecord && validChildBindingTransition(canonicalRecord, record)) {
    // Child binding is the one update whose old generation cannot be safely
    // retained after the lease owner and the predeclared child are both gone:
    // the old row intentionally has no child identity and would therefore stay
    // fail-closed forever.  A crash immediately after candidate staging occurs
    // before the normal durable manifest exists.  The exact old and candidate
    // inodes, token, operation id, transition shape and dead owner/child proofs
    // are nevertheless sufficient to finish preparing that same WAL record.
    //
    // Reconciliation itself can have left several *known* candidate aliases.
    // Consolidate those aliases one durable deletion at a time before recording
    // the candidate proof, so the manifest still truthfully starts at nlink=1.
    if (heldAliases.length > 1) {
      var redundant = heldAliases[heldAliases.length - 1];
      deleteExactArtifact(proof, { leaseId: handle.leaseId, operationId: operationId }, 'candidate',
        { file: redundant.file, proof: redundant.proof }, MAX_BYTES);
      return reconcile(handle);
    }
    if (!validMutationOwner({
      pid: canonicalRecord.owner.pid,
      hostname: canonicalRecord.owner.hostname,
      processStartId: canonicalRecord.owner.processStartId
    })) {
      throw new Error('writer lease orphan child binding has no exact mutation-owner generation');
    }
    var paths = mutationPaths(proof.dir, handle.leaseId, operationId);
    var manifest = {
      version: 1, operationId: operationId, leaseId: handle.leaseId, operation: 'child-binding', phase: 'prepared',
      tokenHash: tokenHash(handle.token),
      mutationOwner: {
        pid: canonicalRecord.owner.pid,
        hostname: canonicalRecord.owner.hostname,
        processStartId: canonicalRecord.owner.processStartId
      },
      oldHash: proofHash(canonical.bytes), oldProof: canonical.proof,
      oldBytesBase64: canonical.bytes.toString('base64'),
      newHash: proofHash(heldAliases[0].bytes), candidateName: paths.candidate,
      candidateProof: heldAliases[0].proof, newBytesBase64: heldAliases[0].bytes.toString('base64'),
      recoveryName: paths.recovery, publishedName: paths.receipt, createdAt: now()
    };
    validateManifest(manifest, handle.leaseId, proof.dir);
    var manifestBytes = Buffer.from(JSON.stringify(manifest, null, 2) + '\n');
    var manifestProof = guardedStage(proof, paths.manifestStage, manifestBytes, MAX_MUTATION_BYTES);
    guardedLink(proof, paths.manifestStage, paths.lock, manifestProof, true,
      paths.manifestCapture, MAX_MUTATION_BYTES);
    return reconcileMutationInternal(handle);
  }
  var pseudoManifest = { leaseId: handle.leaseId, operationId: operationId };
  heldAliases.forEach(function (alias) {
    var live = guardedRead(proof, alias.file, MAX_BYTES);
    if (live && live.bytes.equals(alias.bytes) && proofLineage(live.proof, alias.proof)) {
      deleteExactArtifact(proof, pseudoManifest, 'candidate', { file: alias.file, proof: live.proof }, MAX_BYTES);
    }
  });
  var remaining = mutationArtifactNames(guardedNames(proof), handle.leaseId);
  if (remaining.length) throw new Error('writer lease orphan candidate reconciliation retained ambiguous artifacts');
  return { reconciled: true, state: candidatePublished ? 'orphan-candidate-published' :
    (canonical ? 'orphan-update-candidate-aborted' : 'orphan-candidate-aborted'),
    leaseId: handle.leaseId, operationId: operationId };
}
function reconcile(handle) {
  if (!handle || !handle.path || !handle.leaseId || !handle.token) throw new Error('writer lease reconciliation requires an exact handle');
  var scope = handleScope(handle);
  var proof = guardedDirectory(scope.dir, scope.rootDir, false);
  var names = guardedNames(proof);
  var artifacts = mutationArtifactNames(names, handle.leaseId);
  if (orphanCandidateSet(artifacts, handle.leaseId)) {
    return reconcileOrphanCandidate(handle, proof, names);
  }
  return reconcileMutationInternal(handle);
}
function reconcileStaleMutations(dir, suppliedRoot) {
  dir = path.resolve(dir);
  var proof;
  try {
    var ancestry = inspectSafeDir(dir, suppliedRoot);
    if (ancestry.missing) return { reconciled: [], blocked: [] };
    proof = guardedDirectory(dir, suppliedRoot, false);
  }
  catch (error) {
    throw error;
  }
  var names = guardedNames(proof);
  var ids = Object.create(null);
  var unassigned = [];
  names.forEach(function (name) {
    if (!MUTATION_PREFIX_RE.test(name)) return;
    var identity = mutationArtifactIdentity(name);
    if (identity) ids[identity.leaseId] = true;
    else unassigned.push(name);
  });
  var reconciledRows = [], blocked = unassigned.sort().map(function (name) {
    return { leaseId: null, code: 'WRITER_LEASE_RECOVERY_BLOCKED',
      message: 'writer lease mutation artifact has no unambiguous generation identity: ' + bounded(name, 180) };
  });
  Object.keys(ids).sort().forEach(function (leaseId) {
    var artifacts = mutationArtifactNames(names, leaseId);
    if (!artifacts.length) return;
    try {
      if (orphanCandidateSet(artifacts, leaseId)) {
        var orphan = guardedRead(proof, path.join(dir, artifacts[0]), MAX_BYTES);
        if (!orphan) throw new Error('writer lease orphan candidate is unsafe');
        var orphanRecord = recordFromBytes(orphan.bytes, leaseId);
        reconciledRows.push(reconcileOrphanCandidate({ dir: dir, rootDir: proof.root, path: leasePath(dir, leaseId), leaseId: leaseId, token: orphanRecord.token }, proof, names));
      } else {
        var extracted = extractManifestFromNames(proof, names, leaseId);
        var oldRecord = recordFromBytes(Buffer.from(extracted.manifest.oldBytesBase64, 'base64'), leaseId);
        reconciledRows.push(reconcileMutationInternal({ dir: dir, rootDir: proof.root, path: leasePath(dir, leaseId), leaseId: leaseId, token: oldRecord.token }));
      }
    } catch (error) {
      blocked.push({ leaseId: leaseId, code: 'WRITER_LEASE_RECOVERY_BLOCKED', message: bounded(error && error.message || error, 300) });
    }
  });

  // A clean canonical lease can outlive both its site owner and its bound
  // writer child when the site is terminated after the last CAS mutation but
  // before the normal close handler releases the lease. Mutation-WAL recovery
  // alone cannot see that state: there are no private artifacts left to
  // reconcile, so the otherwise valid canonical row would block every writer
  // forever. Re-scan the pinned directory after WAL reconciliation and release
  // only generations whose exact local owner, exact child, and (for detached
  // Claude sessions) whole process group are all proven gone.
  //
  // The release itself uses the normal durable CAS path. mutateOwned() binds
  // the exact bytes + inode proof again immediately before detach, so a racing
  // replacement is preserved and reported as blocked instead of being unlinked.
  var refreshedNames = guardedNames(proof);
  refreshedNames.filter(function (name) { return name.endsWith('.json'); }).sort().forEach(function (name) {
    var leaseId = name.slice(0, -5);
    if (!LEASE_ID_RE.test(leaseId) || mutationArtifactNames(refreshedNames, leaseId).length) return;
    try {
      var current = readRecord(path.join(dir, name), leaseId, proof);
      // Preserve every lease which the canonical scanner still considers
      // active, including a bounded generation whose owner is gone but whose
      // explicit TTL protection has not expired yet.
      if (active(current.record, os.hostname(), Date.now(), Object.create(null)) ||
          !generationGone(current.record)) return;
      var handle = {
        dir: dir, rootDir: proof.root, path: leasePath(dir, leaseId),
        leaseId: leaseId, token: current.record.token, record: current.record
      };
      mutateOwned(handle, 'release', current, null, proof);
      reconciledRows.push({ reconciled: true, state: 'stale-canonical-released', leaseId: leaseId });
    } catch (error) {
      blocked.push({ leaseId: leaseId, code: 'WRITER_LEASE_RECOVERY_BLOCKED',
        message: bounded(error && error.message || error, 300) });
    }
  });
  return { reconciled: reconciledRows, blocked: blocked };
}
function acquire(dir, options) {
  options = options || {};
  dir = path.resolve(dir);
  var proof = guardedDirectory(dir, options.rootDir, true);
  var id = options.leaseId || createLeaseId();
  var created = now();
  var ttlMs = Number(options.ttlMs || 0);
  var ownerPid = validPid(options.ownerPid) ? options.ownerPid : process.pid;
  var ownerHostname = options.hostname || os.hostname();
  var childPid = validPid(options.childPid) ? options.childPid : null;
  var localOwner = ownerHostname === os.hostname();
  var ownerProcessStartId = localOwner ? captureProcessStartId(ownerPid) : null;
  var childProcessStartId = localOwner && childPid !== null ? captureProcessStartId(childPid) : null;
  var delegationToken = null;
  var recordDelegationHash = options.delegationHash || null;
  if (options.pendingChild === true && recordDelegationHash === null) {
    delegationToken = crypto.randomBytes(24).toString('hex');
    recordDelegationHash = delegationHash(delegationToken);
  }
  var record = {
    version: VERSION,
    leaseId: id,
    token: crypto.randomBytes(24).toString('hex'),
    kind: options.kind,
    stem: options.stem || null,
    sessionId: options.sessionId || null,
    key: options.key || null,
    owner: {
      pid: ownerPid,
      hostname: ownerHostname,
      startedAt: created,
      processStartId: ownerProcessStartId
    },
    childPid: childPid,
    childProcessStartId: childProcessStartId,
    delegationHash: recordDelegationHash,
    unverified: options.pendingChild === true,
    unverifiedReason: options.pendingChild === true ? PENDING_CHILD_REASON : null,
    createdAt: created,
    updatedAt: created,
    expiresAt: Number.isFinite(ttlMs) && ttlMs > 0 ? new Date(Date.now() + Math.min(ttlMs, MAX_TTL_MS)).toISOString() : null
  };
  var shape = validateRecord(record, id);
  if (shape) throw new Error(shape);
  var file = leasePath(dir, id);
  var bytes = Buffer.from(JSON.stringify(record, null, 2) + '\n');
  var acquireOperationId = crypto.randomBytes(16).toString('hex');
  var acquirePaths = mutationPaths(dir, id, acquireOperationId);
  var candidateProof = guardedStage(proof, acquirePaths.candidate, bytes, MAX_BYTES);
  mutationHook(null, 'acquire', 'after-candidate-stage', { candidate: acquirePaths.candidate });
  var acquiredProof = guardedLink(proof, acquirePaths.candidate, file, candidateProof, true,
    acquirePaths.candidateCapture, MAX_BYTES);
  var publishedRead = readRecord(file, id, proof);
  if (!publishedRead.raw.equals(bytes) || !proofExact(publishedRead.proof, acquiredProof) ||
      !proofLineage(publishedRead.proof, candidateProof)) {
    throw new Error('new writer lease generation was replaced before acquisition committed');
  }
  return {
    dir: dir, rootDir: proof.root, path: file, leaseId: id, token: record.token, record: record,
    // Never serialize this capability into the lease row. The site copies it
    // only into the exact just-spawned child environment; the durable record
    // carries its SHA-256 verifier.
    delegationToken: delegationToken
  };
}
function updateChildPidUnlocked(handle, childPid) {
  if (!handle || !validPid(childPid)) throw new Error('writer lease child pid is invalid');
  var childProcessStartId = captureProcessStartId(childPid);
  if ((process.platform === 'linux' || process.platform === 'darwin' || process.platform === 'win32') && !childProcessStartId) {
    throw new Error('writer lease child process-start identity is unavailable');
  }
  var scope = handleScope(handle);
  var proof = guardedDirectory(scope.dir, scope.rootDir, true);
  var current = readRecord(handle.path, handle.leaseId, proof);
  if (current.record.token !== handle.token) throw new Error('writer lease ownership changed');
  if (current.record.childPid !== null) throw new Error('writer lease child is already bound');
  if (current.record.unverified && current.record.unverifiedReason !== PENDING_CHILD_REASON) {
    throw new Error('refusing to clear a non-pending unverified writer lease');
  }
  var next = JSON.parse(JSON.stringify(current.record));
  next.childPid = childPid;
  next.childProcessStartId = childProcessStartId;
  // Child binding and pending-proof clearance are one durable replacement. A
  // site crash can therefore expose either an unverified pending lease or the
  // exact child PID/PGID, never the unsafe null+verified intermediate state.
  next.unverified = false;
  next.unverifiedReason = null;
  next.updatedAt = now();
  var shape = validateRecord(next, handle.leaseId);
  if (shape) throw new Error(shape);
  return mutateOwned(handle, 'child binding', current, next, proof);
}
function markUnverifiedUnlocked(handle, reason) {
  if (!handle) throw new Error('writer lease handle is missing');
  var scope = handleScope(handle);
  var proof = guardedDirectory(scope.dir, scope.rootDir, true);
  var current = readRecord(handle.path, handle.leaseId, proof);
  if (current.record.token !== handle.token) throw new Error('writer lease ownership changed');
  var next = JSON.parse(JSON.stringify(current.record));
  next.unverified = true;
  next.unverifiedReason = bounded(reason || 'writer process tree death was not proven', 500);
  next.updatedAt = now();
  var shape = validateRecord(next, handle.leaseId);
  if (shape) throw new Error(shape);
  return mutateOwned(handle, 'unverified mark', current, next, proof);
}
function renewUnlocked(handle, ttlMs) {
  if (!handle) throw new Error('writer lease handle is missing');
  ttlMs = Number(ttlMs);
  if (!Number.isFinite(ttlMs) || !Number.isInteger(ttlMs) || ttlMs <= 0) throw new Error('writer lease renewal TTL is invalid');
  var scope = handleScope(handle);
  var proof = guardedDirectory(scope.dir, scope.rootDir, true);
  var current = readRecord(handle.path, handle.leaseId, proof);
  if (current.record.token !== handle.token) throw new Error('writer lease ownership changed');
  if (current.record.unverified) throw new Error('refusing to renew an unverified writer lease');
  if (current.record.expiresAt === null) throw new Error('writer lease is not a renewable bounded lease');
  if (Date.parse(current.record.expiresAt) <= Date.now()) throw new Error('writer lease already expired; acquire a fresh guarded lease');
  if (current.record.owner.hostname !== os.hostname() ||
      !processIdentityMatches(current.record.owner.pid, current.record.owner.processStartId)) {
    throw new Error('refusing to renew a writer lease whose owner process generation is not proven');
  }
  var next = JSON.parse(JSON.stringify(current.record));
  next.updatedAt = now();
  next.expiresAt = new Date(Date.now() + Math.min(ttlMs, MAX_TTL_MS)).toISOString();
  var shape = validateRecord(next, handle.leaseId);
  if (shape) throw new Error(shape);
  return mutateOwned(handle, 'renewal', current, next, proof);
}
function releaseUnlocked(handle) {
  if (!handle || !handle.path || !handle.leaseId || !handle.token) return false;
  var scope = handleScope(handle);
  var proof = guardedDirectory(scope.dir, scope.rootDir, true);
  var names = guardedNames(proof);
  var artifacts = mutationArtifactNames(names, handle.leaseId);
  if (artifacts.length) throw new Error('writer lease mutation is already in progress or retained for recovery');
  var canonicalName = path.basename(handle.path);
  if (names.indexOf(canonicalName) < 0) return true;
  var current = readRecord(handle.path, handle.leaseId, proof);
  if (current.record.token !== handle.token) throw new Error('refusing to release a writer lease owned by another generation');
  mutationHook(handle, 'release', 'before-unlink', { file: handle.path });
  return mutateOwned(handle, 'release', current, null, proof);
}
function updateChildPid(handle, childPid) {
  return updateChildPidUnlocked(handle, childPid);
}
function markUnverified(handle, reason) {
  return markUnverifiedUnlocked(handle, reason);
}
function renew(handle, ttlMs) {
  return renewUnlocked(handle, ttlMs);
}
function release(handle) {
  if (!handle || !handle.path || !handle.leaseId || !handle.token) return false;
  return releaseUnlocked(handle);
}
function processStateMayOwn(state) { return state === 'match' || state === 'pid-live' || state === 'unknown'; }
function active(record, localHost, nowMs, processCache) {
  var ownerState, childState;
  if (record.unverified) {
    // A retained lease is fail-closed while any recorded owner/tree may live.
    // After a site restart on POSIX, however, a bound local child PID is also
    // its detached PGID. Proving owner, leader, and that whole PGID absent is a
    // sufficient automatic tombstone reconciliation; keeping it active forever
    // would require manual cache deletion after every interrupted cancel.
    if (record.owner.hostname !== localHost) return true;
    ownerState = processInstanceState(record.owner.pid, record.owner.processStartId, processCache);
    if (processStateMayOwn(ownerState)) return true;
    if (!validPid(record.childPid)) return true; // spawn->attach gap: no tree identity to prove dead
    childState = processInstanceState(record.childPid, record.childProcessStartId, processCache);
    if (processStateMayOwn(childState)) return true;
    if (process.platform === 'win32') return true; // no durable Job handle/proof in a restarted site
    // Claude sessions and runtime builds are spawned as detached group
    // leaders. Other writer kinds do not carry a durable promise that childPid
    // is also the PGID, so they remain fail-closed instead of treating an
    // ESRCH group probe as proof that every possible descendant is gone.
    if (record.kind !== 'task-session' && record.kind !== 'workspace-session' &&
        record.kind !== 'runtime-build') return true;
    // A different start identity proves PID/PGID reuse. The original group had
    // to disappear before that numeric ID could become a new process leader.
    return childState !== 'reused' && processGroupAlive(record.childPid);
  }
  if (record.owner.hostname !== localHost) return true;
  ownerState = processInstanceState(record.owner.pid, record.owner.processStartId, processCache);
  childState = processInstanceState(record.childPid, record.childProcessStartId, processCache);
  if (processStateMayOwn(ownerState) || processStateMayOwn(childState)) return true;
  // Site Claude sessions and runtime builds are detached process-group
  // leaders. If the site and leader crash but a tool descendant survives, the
  // positive child PID is no longer live; probing its original PGID keeps the
  // durable lease active until the entire inherited writer tree is gone.
  if ((record.kind === 'task-session' || record.kind === 'workspace-session' ||
      record.kind === 'runtime-build') &&
      childState !== 'reused' && processGroupAlive(record.childPid)) return true;
  return record.expiresAt !== null && Date.parse(record.expiresAt) > nowMs;
}
function scan(dir, suppliedRoot) {
  dir = path.resolve(dir);
  var localHost = os.hostname();
  var names, scanStarted = monotonicMs(), guardedProof;
  try {
    var proof = inspectSafeDir(dir, suppliedRoot);
    if (proof.missing) return { active: [], stale: [], issues: [] };
    guardedProof = guardedDirectory(dir, suppliedRoot, false);
    names = guardedNames(guardedProof);
  } catch (e) {
    if (e && e.code === 'ENOENT') return { active: [], stale: [], issues: [] };
    return { active: [], stale: [], issues: [{ active: true, code: e && e.writerLeaseCode || 'WRITER_LEASE_DIR_UNSAFE', message: bounded(e && e.message || e) }] };
  }
  var activeRows = [], staleRows = [], issues = [], nowMs = Date.now(), processCache = Object.create(null);
  var recordBytes = 0;
  names.filter(function (name) { return RECOVERY_NAME_RE.test(name); }).sort().forEach(function (name) {
    issues.push({ active: true, code: 'WRITER_LEASE_RECOVERY_REQUIRED', message: 'writer lease CAS recovery artifact requires reconciliation: ' + bounded(name, 180) });
  });
  names.filter(function (name) { return MUTATION_LOCK_RE.test(name); }).sort().forEach(function (name) {
    issues.push({ active: true, code: 'WRITER_LEASE_MUTATION_LOCKED', message: 'writer lease mutation lock requires reconciliation: ' + bounded(name, 180) });
  });
  names.filter(function (name) { return MUTATION_STAGE_RE.test(name) || MUTATION_CANDIDATE_RE.test(name) ||
    MUTATION_PUBLISHED_RE.test(name) || MUTATION_CLEANUP_RE.test(name); }).sort().forEach(function (name) {
    issues.push({ active: true, code: 'WRITER_LEASE_RECOVERY_REQUIRED', message: 'writer lease mutation artifact requires reconciliation: ' + bounded(name, 180) });
  });
  names.filter(function (name) {
    return MUTATION_PREFIX_RE.test(name) && !RECOVERY_NAME_RE.test(name) && !MUTATION_LOCK_RE.test(name) &&
      !MUTATION_STAGE_RE.test(name) && !MUTATION_CANDIDATE_RE.test(name) && !MUTATION_PUBLISHED_RE.test(name) && !MUTATION_CLEANUP_RE.test(name);
  }).sort().forEach(function (name) {
    issues.push({ active: true, code: 'WRITER_LEASE_ARTIFACT_UNSAFE', message: 'writer lease mutation prefix has an unsafe or partial name: ' + bounded(name, 180) });
  });
  names.filter(function (name) {
    return !name.endsWith('.json') && !MUTATION_PREFIX_RE.test(name) && !RECOVERY_NAME_RE.test(name) &&
      !MUTATION_LOCK_RE.test(name) && !MUTATION_STAGE_RE.test(name) && !MUTATION_CANDIDATE_RE.test(name) &&
      !MUTATION_PUBLISHED_RE.test(name) && !MUTATION_CLEANUP_RE.test(name);
  }).sort().forEach(function (name) {
    issues.push({ active: true, code: 'WRITER_LEASE_NAME_UNSAFE', message: 'unexpected writer lease directory entry: ' + bounded(name, 180) });
  });
  names = names.filter(function (name) { return name.endsWith('.json'); }).sort();
  for (var nameIndex = 0; nameIndex < names.length; nameIndex++) {
    var name = names[nameIndex];
    if (monotonicMs() - scanStarted > MAX_SCAN_MS) {
      issues.push({ active: true, code: 'WRITER_LEASE_SCAN_LIMIT', message: 'writer lease scan exceeded its time limit' });
      break;
    }
    var id = name.slice(0, -5);
    if (!LEASE_ID_RE.test(id)) {
      issues.push({ active: true, code: 'WRITER_LEASE_NAME_UNSAFE', message: 'unsafe writer lease filename: ' + bounded(name, 180) });
      continue;
    }
    try {
      var read = readRecord(path.join(dir, name), id, guardedProof);
      if (!read.proof || read.proof.nlink !== '1') throw new Error('writer lease canonical generation has unexpected hard links');
      recordBytes += read.rawBytes;
      if (recordBytes > MAX_SCAN_RECORD_BYTES) {
        issues.push({ active: true, code: 'WRITER_LEASE_SCAN_LIMIT', message: 'writer lease scan exceeds the total byte limit' });
        break;
      }
      var row = read.record;
      if (active(row, localHost, nowMs, processCache)) activeRows.push(row);
      else staleRows.push(row);
    } catch (e) {
      // A failed bounded read can still have consumed up to MAX_BYTES. Count
      // it so many malformed rows cannot bypass the aggregate scan budget.
      recordBytes += MAX_BYTES;
      issues.push({ active: true, code: 'WRITER_LEASE_INVALID', leaseId: id, message: bounded(e && e.message || e) });
      if (recordBytes > MAX_SCAN_RECORD_BYTES) {
        issues.push({ active: true, code: 'WRITER_LEASE_SCAN_LIMIT', message: 'writer lease scan exceeds the total byte limit' });
        break;
      }
    }
  }
  return { active: activeRows, stale: staleRows, issues: issues };
}

module.exports = {
  VERSION: VERSION,
  MAX_BYTES: MAX_BYTES,
  MAX_TTL_MS: MAX_TTL_MS,
  PENDING_CHILD_REASON: PENDING_CHILD_REASON,
  LEASE_ID_RE: LEASE_ID_RE,
  SESSION_ID_RE: SESSION_ID_RE,
  PROCESS_START_ID_RE: PROCESS_START_ID_RE,
  createLeaseId: createLeaseId,
  createSessionId: createSessionId,
  captureProcessStartId: captureProcessStartId,
  processIdentityState: processIdentityState,
  processIdentityMatches: processIdentityMatches,
  processTreeProof: processTreeProof,
  validateRecord: validateRecord,
  acquire: acquire,
  updateChildPid: updateChildPid,
  markUnverified: markUnverified,
  renew: renew,
  release: release,
  reconcile: reconcile,
  reconcileStaleMutations: reconcileStaleMutations,
  scan: scan
};
