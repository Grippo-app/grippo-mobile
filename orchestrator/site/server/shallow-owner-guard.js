'use strict';

var fs = require('fs');
var path = require('path');

var DEFAULT_RECONCILE_ENTRIES = 10000;
var DEFAULT_RECONCILE_TRANSACTIONS = 1000;
var PROOF_FIELDS = ['dev', 'ino', 'mode', 'nlink', 'size', 'mtimeNs', 'ctimeNs', 'type'];
var DECIMAL_RE = /^(?:0|[1-9][0-9]*)$/;

function own(value, key) {
  return Object.prototype.hasOwnProperty.call(value, key);
}

function absoluteDirectTarget(root, file) {
  if (typeof root !== 'string' || typeof file !== 'string' ||
      path.resolve(root) !== root || path.resolve(file) !== file || file === root) return null;
  var relative = path.relative(root, file);
  if (!relative || relative === '..' || relative.indexOf('..' + path.sep) === 0 || path.isAbsolute(relative)) return null;
  var directory = path.dirname(file);
  return directory !== file ? { root: root, directory: directory, file: file } : null;
}

function integerOption(value, fallback, max) {
  value = value === undefined ? fallback : value;
  if (!Number.isSafeInteger(value) || value < 0 || value > max) throw new TypeError('invalid shallow owner guard bound');
  return value;
}

function exactProcessIdentity(value) {
  return !!value && Number.isInteger(value.pid) && value.pid > 0 && value.pid <= 0x7fffffff &&
    typeof value.processStartId === 'string' && value.processStartId.length > 0 &&
    value.processStartId.length <= 512 && !/[\u0000-\u001f\u007f]/.test(value.processStartId);
}

function proofField(stat, field) {
  if (!stat || typeof stat !== 'object') return null;
  var value;
  if (field === 'mode') value = stat.modeExact === undefined ? stat.mode : stat.modeExact;
  else if (field === 'size') value = stat.sizeExact === undefined ? stat.size : stat.sizeExact;
  else value = stat[field];
  if (field === 'type') {
    if (typeof value === 'string') return value;
    if (typeof stat.isFile === 'function' && stat.isFile()) return 'file';
    if (typeof stat.isDirectory === 'function' && stat.isDirectory()) return 'directory';
    if (typeof stat.isSymbolicLink === 'function' && stat.isSymbolicLink()) return 'symlink';
    return 'other';
  }
  try { return String(value); } catch (error) { return null; }
}

function observedType(stat) {
  if (!stat || typeof stat !== 'object') return null;
  if (typeof stat.isFile === 'function' && stat.isFile()) return 'file';
  if (typeof stat.isDirectory === 'function' && stat.isDirectory()) return 'directory';
  if (typeof stat.isSymbolicLink === 'function' && stat.isSymbolicLink()) return 'symlink';
  return 'other';
}

function observedField(stat, field) {
  if (field === 'type') return observedType(stat);
  var value = stat && stat[field];
  try { return String(value); } catch (error) { return null; }
}

function sameExactProof(observed, proof) {
  return PROOF_FIELDS.every(function (field) {
    var observedValue = observedField(observed, field), proofValue = proofField(proof, field);
    return field === 'type'
      ? observedValue === proofValue && proofValue === 'file'
      : DECIMAL_RE.test(String(observedValue || '')) && observedValue === proofValue;
  });
}

function privateMode(stat) {
  var raw = proofField(stat, 'mode');
  if (typeof raw !== 'string' || !DECIMAL_RE.test(raw)) return false;
  try { return (BigInt(raw) & 0o077n) === 0n; } catch (error) { return false; }
}

function exactSoleRegular(stat) {
  return proofField(stat, 'type') === 'file' && proofField(stat, 'nlink') === '1';
}

function canonicalRecord(bytes) {
  if (!Buffer.isBuffer(bytes)) return null;
  var value;
  try { value = JSON.parse(bytes.toString('utf8')); }
  catch (error) { return null; }
  if (!value || typeof value !== 'object' || Array.isArray(value)) return null;
  var canonical;
  try { canonical = Buffer.from(JSON.stringify(value) + '\n', 'utf8'); }
  catch (error2) { return null; }
  return canonical.equals(bytes) ? value : null;
}

function retained(action, code, details) {
  var result = {
    ok: false,
    action: action,
    code: code,
    outcome: 'retained',
    committed: false,
    alreadyMissing: false,
    uncertain: false
  };
  return Object.assign(result, details || {});
}

function missing(action) {
  return retained(action, 'owner-missing', {
    outcome: 'not-committed', alreadyMissing: true, uncertain: true
  });
}

function validDeleteEnvelope(result) {
  if (!result || typeof result !== 'object' || typeof result.ok !== 'boolean' ||
      typeof result.code !== 'string' || !result.code || typeof result.outcome !== 'string' || !result.outcome ||
      typeof result.committed !== 'boolean' || typeof result.alreadyMissing !== 'boolean' ||
      typeof result.uncertain !== 'boolean') return false;
  if (result.committed && result.alreadyMissing) return false;
  if (result.ok && result.uncertain) return false;
  if (result.ok && !result.committed && !result.alreadyMissing) return false;
  if (result.committed && !result.ok) return false;
  return true;
}

function deleteSettlement(action, kind, result) {
  if (!validDeleteEnvelope(result)) {
    return retained(action, 'owner-delete-envelope-invalid', {
      outcome: 'not-committed', uncertain: true, recordKind: kind
    });
  }
  return {
    ok: result.ok,
    action: action,
    code: result.code,
    outcome: result.outcome,
    committed: result.committed,
    alreadyMissing: result.alreadyMissing,
    uncertain: result.uncertain,
    recordKind: kind
  };
}

function create(options) {
  options = options || {};
  var guards = options.fileGuards;
  if (!guards || typeof guards.boundedRegularFileUnder !== 'function' ||
      typeof guards.inspectEntryUnder !== 'function' ||
      typeof guards.reconcileGuardTransactionsUnder !== 'function' ||
      typeof guards.unlinkRegularFileMatchingResultUnder !== 'function') {
    throw new TypeError('shallow owner guard requires exact file-guard operations');
  }
  if (typeof options.root !== 'string' || path.resolve(options.root) !== options.root) {
    throw new TypeError('shallow owner guard root must be absolute');
  }
  if (!Number.isSafeInteger(options.maxBytes) || options.maxBytes < 1) {
    throw new TypeError('shallow owner guard maxBytes must be positive');
  }
  if (typeof options.hostname !== 'string' || !options.hostname || options.hostname.length > 255) {
    throw new TypeError('shallow owner guard hostname is invalid');
  }
  if (typeof options.validateOwner !== 'function' || typeof options.validateWorker !== 'function' ||
      typeof options.processIdentityState !== 'function') {
    throw new TypeError('shallow owner guard requires record and process validators');
  }
  var platform = options.platform || process.platform;
  if (platform !== 'win32' && platform !== 'linux' && platform !== 'darwin') {
    throw new TypeError('shallow owner guard requires an exact-identity platform');
  }
  if (platform === 'win32' && typeof options.privatePathState !== 'function') {
    throw new TypeError('Windows shallow owner guard requires privatePathState');
  }
  var currentUid = options.currentUid;
  if (platform !== 'win32') {
    if (currentUid === undefined && typeof process.getuid === 'function') currentUid = process.getuid();
    if (!Number.isSafeInteger(currentUid) || currentUid < 0) {
      throw new TypeError('POSIX shallow owner guard requires the current uid');
    }
  }
  var lstatSync = options.lstatSync || fs.lstatSync;
  if (platform !== 'win32' && typeof lstatSync !== 'function') {
    throw new TypeError('POSIX shallow owner guard requires exact lstat');
  }
  var root = options.root;
  var maxBytes = options.maxBytes;
  var reconcileEntries = integerOption(options.reconcileEntries, DEFAULT_RECONCILE_ENTRIES, 100000);
  var reconcileTransactions = integerOption(options.reconcileTransactions,
    DEFAULT_RECONCILE_TRANSACTIONS, 100000);

  function localIdentity() {
    var identity;
    try { identity = typeof options.siteIdentity === 'function' ? options.siteIdentity() : options.siteIdentity; }
    catch (error) { return null; }
    return exactProcessIdentity(identity) ? {
      pid: identity.pid, processStartId: identity.processStartId
    } : null;
  }

  function privateGeneration(file, stat) {
    if (!exactSoleRegular(stat)) return false;
    if (platform === 'win32') {
      try { return options.privatePathState(file, stat) === 'private'; }
      catch (error) { return false; }
    }
    if (!privateMode(stat)) return false;
    var observed;
    try { observed = lstatSync(file, { bigint: true }); }
    catch (error2) { return false; }
    try {
      return sameExactProof(observed, stat) && observedType(observed) === 'file' &&
        String(observed.nlink) === '1' && String(observed.uid) === String(currentUid) &&
        (BigInt(observed.mode) & 0o077n) === 0n;
    } catch (error3) { return false; }
  }

  function classify(value) {
    var owner = false, worker = false;
    try {
      owner = options.validateOwner(value) === true;
      worker = options.validateWorker(value) === true;
    } catch (error) { return null; }
    if (owner === worker) return null;
    return owner ? 'owner' : 'worker';
  }

  function prepare(action, file) {
    var target = absoluteDirectTarget(root, file);
    if (!target) return retained(action, 'owner-path-unsafe');
    var reconciliation;
    try {
      reconciliation = guards.reconcileGuardTransactionsUnder(root, target.directory, {
        maxEntries: reconcileEntries, maxTransactions: reconcileTransactions
      });
    } catch (error) {
      return retained(action, 'owner-guard-reconcile-failed', { uncertain: true });
    }
    if (!reconciliation || reconciliation.ok !== true) {
      return retained(action, 'owner-guard-reconcile-failed', {
        uncertain: true,
        reconcileCode: reconciliation && typeof reconciliation.code === 'string'
          ? reconciliation.code : 'invalid-result'
      });
    }
    var read;
    try { read = guards.boundedRegularFileUnder(root, target.directory, file, maxBytes); }
    catch (error2) { read = null; }
    if (!read || !Buffer.isBuffer(read.bytes) || !read.stat) {
      var entry;
      try { entry = guards.inspectEntryUnder(root, target.directory, file); }
      catch (error3) { entry = null; }
      if (entry && entry.status === 'missing') return missing(action);
      return retained(action, 'owner-read-unsafe', { uncertain: true });
    }
    if (!privateGeneration(file, read.stat)) return retained(action, 'owner-privacy-unsafe');
    var value = canonicalRecord(read.bytes);
    if (!value) return retained(action, 'owner-canonical-invalid');
    var kind = classify(value);
    if (!kind || typeof value.hostname !== 'string' || !value.hostname || !exactProcessIdentity(value) ||
        typeof value.token !== 'string' || !value.token) {
      return retained(action, 'owner-record-invalid');
    }
    return {
      ok: true, action: action, code: 'owner-current', outcome: 'present',
      committed: false, alreadyMissing: false, uncertain: false,
      recordKind: kind, record: value, bytes: read.bytes, stat: read.stat,
      target: target, reconciliation: reconciliation
    };
  }

  function guardedDelete(action, prepared) {
    var result;
    try {
      result = guards.unlinkRegularFileMatchingResultUnder(
        root, prepared.target.directory, prepared.target.file, maxBytes,
        { bytes: prepared.bytes, proof: prepared.stat });
    } catch (error) {
      result = null;
    }
    return deleteSettlement(action, prepared.recordKind, result);
  }

  function inspect(file) {
    return prepare('inspect', file);
  }

  function release(file, token) {
    if (typeof token !== 'string' || !token) return retained('release', 'owner-token-invalid');
    var prepared = prepare('release', file);
    if (!prepared.ok) return prepared;
    var identity = localIdentity();
    if (!identity) return retained('release', 'owner-local-identity-unavailable', {
      recordKind: prepared.recordKind
    });
    var record = prepared.record;
    if (record.hostname !== options.hostname || record.pid !== identity.pid ||
        record.processStartId !== identity.processStartId || record.token !== token) {
      return retained('release', 'owner-generation-foreign', { recordKind: prepared.recordKind });
    }
    return guardedDelete('release', prepared);
  }

  function stateOf(identity) {
    if (!exactProcessIdentity(identity)) return 'invalid';
    try {
      var state = options.processIdentityState(identity.pid, identity.processStartId);
      return typeof state === 'string' ? state : 'invalid';
    } catch (error) { return 'invalid'; }
  }

  function bindingPair(value) {
    if (value === null) return null;
    return exactProcessIdentity(value) ? {
      pid: value.pid, processStartId: value.processStartId
    } : undefined;
  }

  function workerBindings(record) {
    var raw;
    try {
      raw = typeof options.workerBindings === 'function'
        ? options.workerBindings(record)
        : {
          child: record.childPid === null && record.childProcessStartId === null ? null : {
            pid: record.childPid, processStartId: record.childProcessStartId
          },
          model: record.modelPid === null && record.modelProcessStartId === null ? null : {
            pid: record.modelPid, processStartId: record.modelProcessStartId
          }
        };
    } catch (error) { return null; }
    if (!raw || typeof raw !== 'object' || Array.isArray(raw) ||
        !own(raw, 'child') || !own(raw, 'model')) return null;
    var child = bindingPair(raw.child), model = bindingPair(raw.model);
    if (child === undefined || model === undefined) return null;
    return { child: child, model: model };
  }

  function noSpawnPossible(record) {
    if (typeof options.proveNoSpawnPossible !== 'function') return false;
    try { return options.proveNoSpawnPossible(record) === true; }
    catch (error) { return false; }
  }

  function unboundModelCannotExecute(record) {
    if (typeof options.proveUnboundModelCannotExecute !== 'function') return false;
    try { return options.proveUnboundModelCannotExecute(record) === true; }
    catch (error) { return false; }
  }

  function containmentState(record) {
    if (typeof options.workerContainmentState !== 'function') return 'unverified';
    try {
      var state = options.workerContainmentState(record);
      return typeof state === 'string' ? state : 'unverified';
    } catch (error) { return 'unverified'; }
  }

  function recover(file) {
    var prepared = prepare('recover', file);
    if (!prepared.ok) return prepared;
    var record = prepared.record;
    if (record.hostname !== options.hostname) {
      return retained('recover', 'owner-host-foreign', { recordKind: prepared.recordKind });
    }
    var siteState = stateOf(record);
    if (siteState !== 'dead' && siteState !== 'reused') {
      return retained('recover', siteState === 'match' || siteState === 'pid-live'
        ? 'owner-site-active' : 'owner-site-unverified', {
        recordKind: prepared.recordKind, processState: siteState
      });
    }
    if (prepared.recordKind === 'worker') {
      var bindings = workerBindings(record);
      if (!bindings) return retained('recover', 'worker-bind-invalid', { recordKind: 'worker' });
      if (bindings.child === null && bindings.model === null) {
        if (!noSpawnPossible(record)) {
          return retained('recover', 'worker-prebind-ambiguous', { recordKind: 'worker' });
        }
      } else {
        if (bindings.child === null) {
          return retained('recover', 'worker-prebind-ambiguous', { recordKind: 'worker' });
        }
        if (bindings.model === null && !unboundModelCannotExecute(record)) {
          return retained('recover', 'worker-prebind-ambiguous', { recordKind: 'worker' });
        }
        var childState = stateOf(bindings.child);
        if (childState !== 'dead' && childState !== 'reused') {
          return retained('recover', childState === 'match' || childState === 'pid-live'
            ? 'worker-child-active' : 'worker-child-unverified', {
            recordKind: 'worker', processState: childState
          });
        }
        if (bindings.model !== null) {
          var modelState = stateOf(bindings.model);
          if (modelState !== 'dead' && modelState !== 'reused') {
            return retained('recover', modelState === 'match' || modelState === 'pid-live'
              ? 'worker-model-active' : 'worker-model-unverified', {
              recordKind: 'worker', processState: modelState
            });
          }
        }
        var drained = containmentState(record);
        if (drained !== 'drained') {
          return retained('recover', drained === 'active'
            ? 'worker-containment-active' : 'worker-containment-unverified', {
            recordKind: 'worker', containmentState: drained
          });
        }
      }
    }
    return guardedDelete('recover', prepared);
  }

  return Object.freeze({ inspect: inspect, release: release, recover: recover });
}

module.exports = Object.freeze({ create: create });
