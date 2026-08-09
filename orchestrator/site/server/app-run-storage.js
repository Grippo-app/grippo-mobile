'use strict';

var fs = require('fs');
var path = require('path');
var crypto = require('crypto');
var paths = require('./paths');
var fileGuards = require('./file-guards');

var AUTHORITY_ROOT = path.resolve(process.env.ORCHESTRATOR_APP_RUN_AUTHORITY_ROOT ||
  (process.env.ORCHESTRATOR_APP_RUN_DIR ? path.dirname(paths.APP_RUN_DIR) : paths.PROJECT_ROOT));
var ID_RE = /^(?:job|session|artifact|receipt|shot|history)-[a-f0-9]{24,64}$/;
var MAX_JSON = 256 * 1024;
var MAX_ENTRIES = 1000;
var MAX_SCREENSHOT_ENTRIES = 12000;
var DIRECTORIES = [
  paths.APP_RUN_DIR, paths.APP_RUN_JOBS_DIR, paths.APP_RUN_SESSIONS_DIR,
  paths.APP_RUN_ARTIFACTS_DIR, paths.APP_RUN_HISTORY_DIR,
  paths.APP_RUN_SCREENSHOTS_DIR, paths.APP_RUN_LOGS_DIR
];
var OWNED_TEMP_RE = /^\.(?:(?:(?:job|session|artifact|receipt|shot|history)-[a-f0-9]{24,64}\.(?:json|png))|index\.json)-[1-9][0-9]*-[a-f0-9]{24}\.tmp$/;
var CAPTURE_TEMP_RE = /^\.capture-[a-f0-9]{24}\.png$/;

function inside(root, target) {
  var rel = path.relative(root, target);
  return rel === '' || (rel.indexOf('..' + path.sep) !== 0 && rel !== '..' && !path.isAbsolute(rel));
}

function exactStatValue(stat, field) {
  var exact = stat && stat[field + 'Exact'];
  return String(exact === undefined ? stat && stat[field] : exact);
}

function sameIdentity(left, right) {
  return !!left && !!right &&
    exactStatValue(left, 'dev') === exactStatValue(right, 'dev') &&
    exactStatValue(left, 'ino') === exactStatValue(right, 'ino');
}

function ensureDirectory(directory) {
  directory = path.resolve(directory);
  if (!inside(AUTHORITY_ROOT, directory)) throw new Error('app-run storage escaped its authority root');
  var secureRoot = path.resolve(paths.APP_RUN_DIR);
  if (!inside(AUTHORITY_ROOT, secureRoot) || !inside(secureRoot, directory)) {
    throw new Error('app-run storage directory escaped its runtime root');
  }
  var proof = fileGuards.realDirectoryUnder(AUTHORITY_ROOT, directory, {
    create: true,
    mode: 0o700
  });
  if (!proof || !proof.stat) throw new Error('app-run storage directory is unsafe');
  if (process.platform === 'win32') return;
  var flags = fs.constants.O_RDONLY | (fs.constants.O_NOFOLLOW || 0) |
    (fs.constants.O_DIRECTORY || 0);
  var fd;
  try {
    fd = fs.openSync(directory, flags);
    var opened = fs.fstatSync(fd, { bigint: true });
    if (!opened.isDirectory() || !sameIdentity(proof.stat, opened)) {
      throw new Error('app-run storage directory changed before permission hardening');
    }
    if ((Number(opened.mode) & 0o077) !== 0) fs.fchmodSync(fd, 0o700);
    var hardened = fs.fstatSync(fd, { bigint: true });
    if (!sameIdentity(opened, hardened) || (Number(hardened.mode) & 0o077) !== 0) {
      throw new Error('app-run storage directory permissions are unsafe');
    }
  } finally {
    if (fd !== undefined) fs.closeSync(fd);
  }
  var current = fileGuards.realDirectoryUnder(AUTHORITY_ROOT, directory);
  if (!current || !sameIdentity(proof.stat, current.stat)) {
    throw new Error('app-run storage directory changed after permission hardening');
  }
}

function removeObservedFile(directory, file, maximum) {
  var held = fileGuards.boundedRegularFileUnder(AUTHORITY_ROOT, directory, file, maximum);
  if (!held || (process.platform !== 'win32' && (held.stat.mode & 0o077) !== 0)) {
    throw new Error('app-run storage file is unsafe or oversized');
  }
  var result = fileGuards.unlinkRegularFileMatchingResultUnder(
    AUTHORITY_ROOT,
    directory,
    file,
    maximum,
    {
      proof: held.stat,
      sha256: 'sha256:' + crypto.createHash('sha256').update(held.bytes).digest('hex')
    }
  );
  if (!result || (!result.ok && !result.alreadyMissing)) {
    throw new Error('app-run storage file changed before deletion');
  }
  return result.ok === true;
}

function temporaryPath(file) {
  file = path.resolve(file);
  return path.dirname(file) === path.resolve(paths.APP_RUN_SCREENSHOTS_DIR) &&
    CAPTURE_TEMP_RE.test(path.basename(file)) ? file : null;
}

function hardenTemporary(file, maximum) {
  file = temporaryPath(file);
  if (!file || !Number.isSafeInteger(maximum) || maximum < 0) {
    throw new Error('invalid app-run temporary path');
  }
  var directory = path.dirname(file);
  var inspected = fileGuards.inspectEntryUnder(AUTHORITY_ROOT, directory, file);
  if (inspected && inspected.status === 'missing') return false;
  if (!inspected || inspected.status !== 'present' || !inspected.stat ||
      !inspected.stat.isFile() || inspected.stat.isSymbolicLink() ||
      inspected.stat.nlink !== '1' || inspected.stat.size > maximum) {
    throw new Error('app-run temporary file is unsafe or oversized');
  }
  var flags = fs.constants.O_RDONLY | (fs.constants.O_NOFOLLOW || 0);
  var fd;
  try {
    fd = fs.openSync(file, flags);
    var opened = fs.fstatSync(fd, { bigint: true });
    if (!opened.isFile() || opened.isSymbolicLink() || opened.nlink !== 1n ||
        opened.size > BigInt(maximum) || !sameIdentity(inspected.stat, opened)) {
      throw new Error('app-run temporary file changed before permission hardening');
    }
    if (process.platform !== 'win32' && (Number(opened.mode) & 0o077) !== 0) {
      fs.fchmodSync(fd, 0o600);
    }
    var hardened = fs.fstatSync(fd, { bigint: true });
    if (!hardened.isFile() || hardened.nlink !== 1n ||
        hardened.size > BigInt(maximum) || !sameIdentity(opened, hardened) ||
        (process.platform !== 'win32' && (Number(hardened.mode) & 0o077) !== 0)) {
      throw new Error('app-run temporary file permissions are unsafe');
    }
  } finally {
    if (fd !== undefined) fs.closeSync(fd);
  }
  var held = fileGuards.boundedRegularFileUnder(AUTHORITY_ROOT, directory, file, maximum);
  if (!held || !sameIdentity(inspected.stat, held.stat) ||
      (process.platform !== 'win32' && (held.stat.mode & 0o077) !== 0)) {
    throw new Error('app-run temporary file changed after permission hardening');
  }
  return true;
}

function cleanupOwnedTemps(directory) {
  var listed = fileGuards.boundedDirectoryNamesUnder(
    AUTHORITY_ROOT, directory,
    directory === paths.APP_RUN_SCREENSHOTS_DIR ? MAX_SCREENSHOT_ENTRIES : MAX_ENTRIES);
  if (!listed || !listed.ok || listed.exists === false) {
    throw new Error('app-run storage directory enumeration is unsafe');
  }
  listed.names.filter(function (name) {
    return OWNED_TEMP_RE.test(name) || CAPTURE_TEMP_RE.test(name);
  }).forEach(function (name) {
    var file = path.join(directory, name);
    if (CAPTURE_TEMP_RE.test(name)) hardenTemporary(file, 25 * 1024 * 1024);
    removeObservedFile(directory, file, 25 * 1024 * 1024);
  });
}

function init() {
  DIRECTORIES.forEach(function (directory) {
    ensureDirectory(directory);
    var recovery = fileGuards.reconcileGuardTransactionsUnder(AUTHORITY_ROOT, directory, {
      maxEntries: directory === paths.APP_RUN_SCREENSHOTS_DIR
        ? MAX_SCREENSHOT_ENTRIES : MAX_ENTRIES,
      maxTransactions: 100
    });
    if (!recovery || !recovery.ok || recovery.pending !== 0) {
      throw new Error('app-run storage transaction recovery is incomplete');
    }
    cleanupOwnedTemps(directory);
  });
}

function fileFor(directory, id, suffix) {
  if (!ID_RE.test(String(id || ''))) throw new Error('invalid app-run storage id');
  var file = path.join(directory, id + (suffix || '.json'));
  if (!inside(path.resolve(directory), path.resolve(file))) throw new Error('app-run storage path escaped');
  return file;
}

function writeFileAtomic(file, bytes, maximum) {
  bytes = Buffer.isBuffer(bytes) ? bytes : Buffer.from(bytes);
  if (bytes.length > maximum) throw new Error('app-run storage payload exceeds its size limit');
  var directory = path.dirname(file);
  ensureDirectory(directory);
  var existing = fileGuards.inspectEntryUnder(AUTHORITY_ROOT, directory, file);
  if (!existing || existing.status === 'unsafe' ||
      (existing.status === 'present' &&
        (!existing.stat || !existing.stat.isFile() || existing.stat.isSymbolicLink() ||
         existing.stat.nlink !== '1' || existing.stat.size > maximum))) {
    throw new Error('existing app-run storage file is unsafe');
  }
  var result = fileGuards.atomicReplaceRegularFileResult(
    AUTHORITY_ROOT,
    directory,
    file,
    bytes,
    {
      maxBytes: maximum,
      maxExistingBytes: maximum,
      mode: 0o600,
      directoryMode: 0o700
    }
  );
  var expectedHash = 'sha256:' + crypto.createHash('sha256').update(bytes).digest('hex');
  if (!result || !result.ok || result.contentSize !== bytes.length ||
      result.contentHash !== expectedHash) {
    throw new Error('app-run storage atomic publication failed');
  }
}

function writeJson(directory, id, value, maximum) {
  var bytes = Buffer.from(JSON.stringify(value, null, 2) + '\n', 'utf8');
  writeFileAtomic(fileFor(directory, id, '.json'), bytes, maximum || MAX_JSON);
}

function readFileSafe(file, maximum) {
  var directory = path.dirname(file);
  var inspected = fileGuards.inspectEntryUnder(AUTHORITY_ROOT, directory, file);
  if (inspected && inspected.status === 'missing') {
    var missing = new Error('app-run storage file is missing');
    missing.code = 'ENOENT';
    throw missing;
  }
  var held = fileGuards.boundedRegularFileUnder(AUTHORITY_ROOT, directory, file, maximum);
  if (!held || (process.platform !== 'win32' && (held.stat.mode & 0o077) !== 0)) {
    throw new Error('app-run storage file is unsafe or oversized');
  }
  return held.bytes;
}

function readJson(directory, id, maximum) {
  try { return JSON.parse(readFileSafe(fileFor(directory, id, '.json'), maximum || MAX_JSON).toString('utf8')); }
  catch (error) { if (error && error.code === 'ENOENT') return null; throw error; }
}

function list(directory, prefix, suffix) {
  ensureDirectory(directory);
  suffix = suffix || '.json';
  if (suffix !== '.json' && suffix !== '.png') {
    throw new Error('unsupported app-run storage listing suffix');
  }
  var listed = fileGuards.boundedDirectoryNamesUnder(
    AUTHORITY_ROOT, directory,
    directory === paths.APP_RUN_SCREENSHOTS_DIR ? MAX_SCREENSHOT_ENTRIES : MAX_ENTRIES);
  if (!listed || !listed.ok || listed.exists === false) {
    throw new Error('app-run storage directory enumeration is unsafe');
  }
  return listed.names.filter(function (name) {
    return name.slice(-suffix.length) === suffix &&
      ID_RE.test(name.slice(0, -suffix.length)) &&
      (!prefix || name.indexOf(prefix + '-') === 0);
  }).sort().map(function (name) { return name.slice(0, -suffix.length); });
}

function remove(directory, id, suffix, maximum) {
  var file = fileFor(directory, id, suffix || '.json');
  var inspected = fileGuards.inspectEntryUnder(AUTHORITY_ROOT, directory, file);
  if (inspected && inspected.status === 'missing') return false;
  return removeObservedFile(directory, file, maximum || MAX_JSON);
}

function removeTemporary(file, maximum) {
  file = temporaryPath(file);
  if (!file) {
    throw new Error('invalid app-run temporary path');
  }
  var directory = path.dirname(file);
  var inspected = fileGuards.inspectEntryUnder(AUTHORITY_ROOT, directory, file);
  if (inspected && inspected.status === 'missing') return false;
  return removeObservedFile(directory, file, maximum);
}

function randomId(prefix) {
  return prefix + '-' + crypto.randomBytes(18).toString('hex');
}

module.exports = {
  AUTHORITY_ROOT: AUTHORITY_ROOT,
  ID_RE: ID_RE,
  MAX_JSON: MAX_JSON,
  MAX_SCREENSHOT_ENTRIES: MAX_SCREENSHOT_ENTRIES,
  init: init,
  ensureDirectory: ensureDirectory,
  fileFor: fileFor,
  writeFileAtomic: writeFileAtomic,
  writeJson: writeJson,
  readFileSafe: readFileSafe,
  readJson: readJson,
  list: list,
  remove: remove,
  hardenTemporary: hardenTemporary,
  removeTemporary: removeTemporary,
  randomId: randomId
};
