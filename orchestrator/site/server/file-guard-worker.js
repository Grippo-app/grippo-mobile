'use strict';

// This helper intentionally runs filesystem transactions in a separate
// process. Its cwd is therefore process-local: pinning it to a checked
// directory inode cannot redirect unrelated async relative I/O in the site
// server. The parent supplies the inode it observed before spawn; no operation
// is allowed until the child proves that `.` is still that exact directory.

var fs = require('fs');
var crypto = require('crypto');
var StringDecoder = require('string_decoder').StringDecoder;
var DECIMAL_RE = /^(?:0|[1-9][0-9]*)$/;
var EXACT_STAT_FIELDS = ['ctimeNs', 'dev', 'ino', 'mode', 'mtimeNs', 'nlink', 'size', 'type'];

function bigintShape(stat, hash) {
  var out = {
    dev: String(stat.dev), ino: String(stat.ino), mode: String(stat.mode), nlink: String(stat.nlink),
    size: String(stat.size), mtimeNs: String(stat.mtimeNs), ctimeNs: String(stat.ctimeNs),
    type: stat.isDirectory() ? 'directory' : (stat.isFile() ? 'file' : (stat.isSymbolicLink() ? 'symlink' : 'other'))
  };
  if (hash) out.hash = hash;
  return out;
}

function statShape(stat) { return bigintShape(stat); }

function exactBigintShape(stat, expected, type) {
  var fields = type === 'directory' ? ['dev', 'ino', 'mode'] : ['dev', 'ino', 'mode', 'nlink', 'size', 'mtimeNs', 'ctimeNs'];
  return !!stat && !!expected && stat[type === 'directory' ? 'isDirectory' : 'isFile']() && !stat.isSymbolicLink() &&
    expected.type === type && fields.every(function (field) {
      return typeof expected[field] === 'string' && String(stat[field]) === expected[field];
    });
}

function validExactProof(expected, type) {
  return !!expected && typeof expected === 'object' && !Array.isArray(expected) &&
    Object.keys(expected).sort().join('\0') === EXACT_STAT_FIELDS.slice().sort().join('\0') &&
    expected.type === type && ['dev', 'ino', 'mode', 'nlink', 'size', 'mtimeNs', 'ctimeNs'].every(function (field) {
      return typeof expected[field] === 'string' && DECIMAL_RE.test(expected[field]);
    });
}

function exactStatMatchesProof(stat, expected, type) {
  return validExactProof(expected, type) && !!stat &&
    stat[type === 'directory' ? 'isDirectory' : 'isFile']() && !stat.isSymbolicLink() &&
    ['dev', 'ino', 'mode', 'nlink', 'size', 'mtimeNs', 'ctimeNs'].every(function (field) {
      return String(stat[field]) === expected[field];
    });
}

function identityMatchesProof(stat, expected, type) {
  return validExactProof(expected, type) && !!stat &&
    stat[type === 'directory' ? 'isDirectory' : 'isFile']() && !stat.isSymbolicLink() &&
    ['dev', 'ino', 'mode'].every(function (field) { return String(stat[field]) === expected[field]; });
}

function sha256(bytes) { return 'sha256:' + crypto.createHash('sha256').update(bytes).digest('hex'); }

function sameDirectoryExpected(stat, expected) {
  return validExactProof(expected, 'directory') && exactBigintShape(stat, expected, 'directory');
}

function sameIdentity(left, right) {
  return !!left && !!right && left.dev === right.dev && left.ino === right.ino && left.mode === right.mode &&
    left.isFile() === right.isFile() && left.isDirectory() === right.isDirectory() &&
    left.isSymbolicLink() === right.isSymbolicLink();
}

function sameStableFile(left, right) {
  return sameIdentity(left, right) && left.isFile() && right.isFile() &&
    left.nlink === right.nlink && left.size === right.size &&
    left.mtimeNs === right.mtimeNs && left.ctimeNs === right.ctimeNs;
}

function sameStableDirectory(left, right) {
  return sameIdentity(left, right) && left.isDirectory() && right.isDirectory() &&
    left.nlink === right.nlink && left.size === right.size &&
    left.mtimeNs === right.mtimeNs && left.ctimeNs === right.ctimeNs;
}

function sameStableEntry(left, right) {
  return sameIdentity(left, right) && left.nlink === right.nlink && left.size === right.size &&
    left.mtimeNs === right.mtimeNs && left.ctimeNs === right.ctimeNs;
}

function safeName(value) {
  return typeof value === 'string' && value && value !== '.' && value !== '..' &&
    value.indexOf('/') < 0 && value.indexOf('\\') < 0;
}

function writeAll(fd, bytes) {
  var offset = 0;
  while (offset < bytes.length) {
    var written = fs.writeSync(fd, bytes, offset, bytes.length - offset, null);
    if (!written) throw new Error('guarded write made no progress');
    offset += written;
  }
}

function fsyncDirectoryRequired() {
  var fd;
  try {
    // Deterministic coverage for platforms/filesystems where Node cannot open
    // and flush a directory handle.  This hook is intentionally unavailable
    // outside the isolated guard test process.
    if (process.env.ORCHESTRATOR_FILE_GUARD_TEST_MODE === '1' &&
        process.env.ORCHESTRATOR_FILE_GUARD_TEST_FORCE_DIRECTORY_FSYNC_UNSUPPORTED === '1') {
      return false;
    }
    fd = fs.openSync('.', fs.constants.O_RDONLY | (fs.constants.O_DIRECTORY || 0));
    fs.fsyncSync(fd);
    return true;
  } catch (error) {
    // Directory fsync is a durability proof, not a best-effort hint. In
    // particular, Node commonly rejects directory handles on Windows. Treat
    // that as an unsupported durable mutation boundary instead of reporting a
    // false-green commit. Read-only guard operations remain available.
    return false;
  } finally {
    if (fd !== undefined) try { fs.closeSync(fd); } catch (closeError) {}
  }
}

function replacePublicRegularForTest(name, encodedBytes, label) {
  if (process.env.ORCHESTRATOR_FILE_GUARD_TEST_MODE !== '1' || typeof encodedBytes !== 'string') return null;
  var displaced = '.' + name + '-' + label + '-' + process.pid + '-' + crypto.randomBytes(12).toString('hex');
  fs.renameSync(name, displaced);
  var replacementFd = fs.openSync(name, fs.constants.O_WRONLY | fs.constants.O_CREAT |
    fs.constants.O_EXCL | (fs.constants.O_NOFOLLOW || 0), 0o600);
  try {
    writeAll(replacementFd, Buffer.from(encodedBytes, 'base64'));
    fs.fsyncSync(replacementFd);
  } finally { fs.closeSync(replacementFd); }
  fsyncDirectoryRequired();
  return displaced;
}

function boundedRegularFile(name, maxBytes, testReplaceAfterOpenBytes) {
  if (!safeName(name) || !Number.isSafeInteger(maxBytes) || maxBytes < 0) return null;
  var before, fd;
  try {
    before = fs.lstatSync(name, { bigint: true });
    if (!before.isFile() || before.isSymbolicLink() || before.nlink !== 1n ||
        before.size < 0n || before.size > BigInt(maxBytes)) return null;
    fd = fs.openSync(name, fs.constants.O_RDONLY | (fs.constants.O_NOFOLLOW || 0));
    var opened = fs.fstatSync(fd, { bigint: true });
    if (!sameStableFile(before, opened)) return null;
    replacePublicRegularForTest(name, testReplaceAfterOpenBytes, 'read-test-displaced');
    var bytes = Buffer.allocUnsafe(Number(opened.size));
    var offset = 0;
    while (offset < bytes.length) {
      var count = fs.readSync(fd, bytes, offset, bytes.length - offset, offset);
      if (!count) return null;
      offset += count;
    }
    var afterFd = fs.fstatSync(fd, { bigint: true });
    var afterPath = fs.lstatSync(name, { bigint: true });
    if (!sameStableFile(opened, afterFd) || !sameStableFile(opened, afterPath)) return null;
    return { bytes: bytes, stat: afterPath };
  } catch (error) { return null; }
  finally { if (fd !== undefined) try { fs.closeSync(fd); } catch (closeError) {} }
}

function stableRegularStat(name, testReplaceAfterOpenBytes) {
  if (!safeName(name)) return null;
  var fd;
  try {
    var before = fs.lstatSync(name, { bigint: true });
    if (!before.isFile() || before.isSymbolicLink() || before.nlink !== 1n) return null;
    fd = fs.openSync(name, fs.constants.O_RDONLY | (fs.constants.O_NOFOLLOW || 0));
    var opened = fs.fstatSync(fd, { bigint: true });
    replacePublicRegularForTest(name, testReplaceAfterOpenBytes, 'stat-test-displaced');
    var afterFd = fs.fstatSync(fd, { bigint: true });
    var after = fs.lstatSync(name, { bigint: true });
    return sameStableFile(before, opened) && sameStableFile(opened, afterFd) && sameStableFile(afterFd, after)
      ? after : null;
  } catch (error) { return null; }
  finally { if (fd !== undefined) try { fs.closeSync(fd); } catch (closeError) {} }
}

function writerRegularFile(name, maxBytes) {
  if (!safeName(name) || !Number.isSafeInteger(maxBytes) || maxBytes < 0) return null;
  var before, fd;
  try {
    before = fs.lstatSync(name, { bigint: true });
    if (!before.isFile() || before.isSymbolicLink() || before.nlink < 1n || before.nlink > 1025n ||
        before.size < 0n || before.size > BigInt(maxBytes)) return null;
    fd = fs.openSync(name, fs.constants.O_RDONLY | (fs.constants.O_NOFOLLOW || 0));
    var opened = fs.fstatSync(fd, { bigint: true });
    if (!exactBigintShape(opened, bigintShape(before), 'file')) return null;
    var bytes = Buffer.alloc(Number(opened.size));
    var offset = 0;
    while (offset < bytes.length) {
      var count = fs.readSync(fd, bytes, offset, bytes.length - offset, offset);
      if (!count) return null;
      offset += count;
    }
    var afterFd = fs.fstatSync(fd, { bigint: true });
    var after = fs.lstatSync(name, { bigint: true });
    if (!exactBigintShape(afterFd, bigintShape(opened), 'file') ||
        !exactBigintShape(after, bigintShape(opened), 'file')) return null;
    return { bytes: bytes, proof: bigintShape(after, sha256(bytes)), stat: after };
  } catch (error) { return null; }
  finally { if (fd !== undefined) try { fs.closeSync(fd); } catch (closeError) {} }
}

function writerProofMatches(actual, expected) {
  return !!actual && !!expected && ['dev', 'ino', 'mode', 'nlink', 'size', 'mtimeNs', 'ctimeNs', 'type', 'hash'].every(function (field) {
    return actual[field] === expected[field];
  });
}

// Bounded append reuses the crash-safe publication journal and may replace a
// one-megabyte event log. Its base64 payload therefore needs a larger bounded
// WAL envelope. Public no-clobber/CAS callers retain their 512 KiB API cap in
// file-guards.js; this larger internal ceiling is append-only capacity.
var GUARD_WAL_MAX = 2 * 1024 * 1024;
var GUARD_PUBLISH_MAX_BYTES = 1024 * 1024;
var GUARD_OPERATION_RE = /^[a-f0-9]{32}$/;
var GUARD_RECONCILE_MAX_ENTRIES = 100000;
var GUARD_RECONCILE_MAX_TRANSACTIONS = 4096;
var GUARD_DIRECTORY_PAGE_MAX = 4096;
var GUARD_DIRECTORY_CURSOR_MAX = 4096;

function guardCrash(request, point) {
  if (process.env.ORCHESTRATOR_FILE_GUARD_TEST_MODE === '1' && request && request.testCrashAt === point) {
    process.kill(process.pid, 'SIGKILL');
  }
}

function guardNames(name) {
  var key = crypto.createHash('sha256').update(name, 'utf8').digest('hex');
  var base = '.guard-txn-' + key;
  return { manifest: base + '.json', decision: base + '.decision.json', receipt: base + '.receipt.json' };
}

function exactObjectKeys(value, expected) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === expected.slice().sort().join('\0');
}

function guardManifestPayload(kind, intent, operationId, name, capture, targetExpected) {
  return { version: 1, kind: kind, intent: intent, operationId: operationId,
    name: name, capture: capture, targetExpected: targetExpected };
}

function guardDecisionPayload(operationId, name, decision) {
  return { version: 1, operationId: operationId, name: name, decision: decision };
}

function guardReceiptPayload(kind, operationId, name, capture, targetExpected, outcome) {
  return { version: 1, kind: kind, operationId: operationId, name: name,
    capture: capture, targetExpected: targetExpected, outcome: outcome };
}

function withGuardChecksum(payload) {
  return Object.assign({}, payload, { checksum: sha256(Buffer.from(JSON.stringify(payload), 'utf8')) });
}

function validGuardManifest(value, name) {
  if (!exactObjectKeys(value, ['version', 'kind', 'intent', 'operationId', 'name', 'capture', 'targetExpected', 'checksum']) ||
      value.version !== 1 || (value.kind !== 'file-unlink' && value.kind !== 'empty-rmdir') ||
      (value.intent !== 'delete' && value.intent !== 'delete-matching' &&
        value.intent !== 'pending-conditional') || value.name !== name ||
      (value.kind === 'empty-rmdir' && value.intent !== 'delete') ||
      !GUARD_OPERATION_RE.test(String(value.operationId || '')) ||
      value.capture !== '.guard-capture-' + value.operationId || !safeName(value.capture) ||
      !validExactProof(value.targetExpected, value.kind === 'file-unlink' ? 'file' : 'directory')) return false;
  var payload = guardManifestPayload(value.kind, value.intent, value.operationId, value.name,
    value.capture, value.targetExpected);
  return value.checksum === withGuardChecksum(payload).checksum;
}

function validGuardDecision(value, manifest) {
  if (!manifest || !exactObjectKeys(value, ['version', 'operationId', 'name', 'decision', 'checksum']) ||
      value.version !== 1 || value.operationId !== manifest.operationId || value.name !== manifest.name ||
      (value.decision !== 'delete' && value.decision !== 'restore')) return false;
  return value.checksum === withGuardChecksum(guardDecisionPayload(
    value.operationId, value.name, value.decision)).checksum;
}

function validGuardReceipt(value, manifest) {
  if (!manifest || !exactObjectKeys(value,
    ['version', 'kind', 'operationId', 'name', 'capture', 'targetExpected', 'outcome', 'checksum']) ||
      value.version !== 1 || value.kind !== manifest.kind || value.operationId !== manifest.operationId ||
      value.name !== manifest.name || value.capture !== manifest.capture ||
      (value.outcome !== 'deleted' && value.outcome !== 'restored' && value.outcome !== 'aborted') ||
      JSON.stringify(value.targetExpected) !== JSON.stringify(manifest.targetExpected)) return false;
  return value.checksum === withGuardChecksum(guardReceiptPayload(value.kind, value.operationId,
    value.name, value.capture, value.targetExpected, value.outcome)).checksum;
}

function walRegularFile(name) {
  if (!safeName(name)) return null;
  var fd;
  try {
    var before = fs.lstatSync(name, { bigint: true });
    if (!before.isFile() || before.isSymbolicLink() || before.nlink < 1n || before.nlink > 2n ||
        (process.platform !== 'win32' && (before.mode & 0o077n) !== 0n) ||
        before.size < 0n || before.size > BigInt(GUARD_WAL_MAX)) return null;
    fd = fs.openSync(name, fs.constants.O_RDONLY | (fs.constants.O_NOFOLLOW || 0));
    var opened = fs.fstatSync(fd, { bigint: true });
    if (!sameStableFile(before, opened)) return null;
    var bytes = Buffer.alloc(Number(opened.size));
    var offset = 0;
    while (offset < bytes.length) {
      var count = fs.readSync(fd, bytes, offset, bytes.length - offset, offset);
      if (!count) return null;
      offset += count;
    }
    var afterFd = fs.fstatSync(fd, { bigint: true });
    var after = fs.lstatSync(name, { bigint: true });
    return sameStableFile(opened, afterFd) && sameStableFile(afterFd, after)
      ? { bytes: bytes, stat: after } : null;
  } catch (error) { return null; }
  finally { if (fd !== undefined) try { fs.closeSync(fd); } catch (closeError) {} }
}

function walJson(name) {
  var read = walRegularFile(name);
  if (!read) return null;
  try { return { value: JSON.parse(read.bytes.toString('utf8')), bytes: read.bytes, stat: read.stat }; }
  catch (error) { return null; }
}

function missingName(name) {
  try { fs.lstatSync(name, { bigint: true }); return false; }
  catch (error) { return !!error && error.code === 'ENOENT'; }
}

var GUARD_SETTLE_WAIT = new Int32Array(new SharedArrayBuffer(4));
function guardBriefWait(milliseconds) {
  try { Atomics.wait(GUARD_SETTLE_WAIT, 0, 0, milliseconds || 1); }
  catch (error) {}
}

function settleWalPublication(finalName, validator) {
  var stageName = finalName + '.stage';
  var lastTopologyCode = 'wal-entry-unsafe';
  for (var attempt = 0; attempt < 96; attempt++) {
    var finalRead = walJson(finalName);
    var stageRead = walJson(stageName);
    if (finalRead && !validator(finalRead.value)) return { ok: false, code: 'wal-final-invalid' };
    if (stageRead && !validator(stageRead.value)) return { ok: false, code: 'wal-stage-invalid' };
    if (finalRead && stageRead) {
      if (!sameIdentity(finalRead.stat, stageRead.stat) || !finalRead.bytes.equals(stageRead.bytes)) {
        return { ok: false, code: 'wal-generations-conflict' };
      }
      if (finalRead.stat.nlink !== 2n || stageRead.stat.nlink !== 2n) {
        lastTopologyCode = 'wal-generations-conflict';
        guardBriefWait(1);
        continue;
      }
      try {
        fs.unlinkSync(stageName);
        if (!fsyncDirectoryRequired()) return { ok: false, code: 'wal-stage-unsynced' };
      } catch (stageUnlinkError) {
        if (!stageUnlinkError || stageUnlinkError.code !== 'ENOENT') {
          return { ok: false, code: 'wal-stage-cleanup-failed' };
        }
      }
      continue;
    }
    if (!finalRead && stageRead) {
      if (stageRead.stat.nlink !== 1n) {
        lastTopologyCode = 'wal-stage-link-count';
        guardBriefWait(1);
        continue;
      }
      try {
        fs.linkSync(stageName, finalName);
        if (!fsyncDirectoryRequired()) return { ok: false, code: 'wal-publish-unsynced' };
      } catch (publishError) {
        if (!publishError || publishError.code !== 'EEXIST') {
          return { ok: false, code: 'wal-publish-failed' };
        }
      }
      continue;
    }
    if (finalRead) {
      if (finalRead.stat.nlink === 1n) {
        return { ok: true, record: finalRead.value, bytes: finalRead.bytes, stat: finalRead.stat };
      }
      lastTopologyCode = 'wal-final-link-count';
      guardBriefWait(1);
      continue;
    }
    if (missingName(finalName) && missingName(stageName)) {
      return { ok: true, record: null, bytes: null, stat: null };
    }
    guardBriefWait(1);
  }
  return { ok: false, code: lastTopologyCode };
}

function publishWalRecord(request, finalName, record, validator, label) {
  var settled = settleWalPublication(finalName, validator);
  if (!settled.ok) return settled;
  var bytes = Buffer.from(JSON.stringify(record, null, 2) + '\n', 'utf8');
  if (settled.record) return settled.bytes && settled.bytes.equals(bytes)
    ? settled : { ok: false, code: 'wal-record-conflict' };
  var stageName = finalName + '.stage';
  var fd;
  try {
    fd = fs.openSync(stageName, fs.constants.O_WRONLY | fs.constants.O_CREAT | fs.constants.O_EXCL |
      (fs.constants.O_NOFOLLOW || 0), 0o600);
    writeAll(fd, bytes);
    fs.fsyncSync(fd);
    fs.closeSync(fd); fd = undefined;
    guardCrash(request, 'guard:after-' + label + '-stage');
  } catch (stageError) {
    if (fd !== undefined) try { fs.closeSync(fd); } catch (closeError) {}
    if (stageError && stageError.code === 'EEXIST') {
      var raced = settleWalPublication(finalName, validator);
      if (!raced.ok) return raced;
      return raced.record && raced.bytes && raced.bytes.equals(bytes)
        ? raced : { ok: false, code: 'wal-record-conflict' };
    }
    return { ok: false, code: 'wal-stage-failed' };
  }
  return settleWalPublication(finalName, validator);
}

function unlinkWalRecord(name, expectedChecksum, validator) {
  var settled = settleWalPublication(name, validator);
  if (!settled.ok) return false;
  if (!settled.record) return true;
  if (settled.record.checksum !== expectedChecksum) return false;
  try { fs.unlinkSync(name); return fsyncDirectoryRequired(); }
  catch (error) { return !!error && error.code === 'ENOENT' && missingName(name); }
}

function guardCaptureMatches(stat, manifest) {
  if (!stat || !identityMatchesProof(stat, manifest.targetExpected,
    manifest.kind === 'file-unlink' ? 'file' : 'directory')) return false;
  return String(stat.nlink) === manifest.targetExpected.nlink &&
    String(stat.size) === manifest.targetExpected.size &&
    String(stat.mtimeNs) === manifest.targetExpected.mtimeNs;
}

function guardPublicStat(name) {
  try { return { exists: true, stat: fs.lstatSync(name, { bigint: true }) }; }
  catch (error) { return error && error.code === 'ENOENT' ? { exists: false, stat: null } : { exists: true, stat: null }; }
}

function restoreFileCapture(request, capture, name, manifest) {
  try {
    var held = fs.lstatSync(capture, { bigint: true });
    if (!guardCaptureMatches(held, manifest) || held.nlink !== 1n) return false;
    fs.linkSync(capture, name);
    var live = fs.lstatSync(name, { bigint: true });
    var heldLinked = fs.lstatSync(capture, { bigint: true });
    if (!sameIdentity(live, heldLinked) || live.nlink !== 2n || heldLinked.nlink !== 2n ||
        !fsyncDirectoryRequired()) return false;
    guardCrash(request, 'guard:after-restore-link');
    fs.unlinkSync(capture);
    if (!fsyncDirectoryRequired()) return false;
    guardCrash(request, 'guard:after-restore-unlink');
    var sole = fs.lstatSync(name, { bigint: true });
    return sameIdentity(sole, live) && sole.nlink === 1n && sole.size === live.size && sole.mtimeNs === live.mtimeNs;
  } catch (error) { return false; }
}

function restoreForeignFileCapture(capture, name) {
  try {
    var held = stableRegularStat(capture);
    if (!held || held.nlink !== 1n) return false;
    fs.linkSync(capture, name);
    var live = fs.lstatSync(name, { bigint: true });
    var linked = fs.lstatSync(capture, { bigint: true });
    if (!sameIdentity(live, linked) || live.nlink !== 2n || !fsyncDirectoryRequired()) return false;
    fs.unlinkSync(capture);
    return fsyncDirectoryRequired();
  } catch (error) { return false; }
}

function restoreForeignDirectoryCapture(capture, name) {
  var placeholder = null;
  try {
    var held = fs.lstatSync(capture, { bigint: true });
    if (!held.isDirectory() || held.isSymbolicLink()) return false;
    fs.mkdirSync(name, { mode: 0o700 });
    placeholder = fs.lstatSync(name, { bigint: true });
    if (!placeholder.isDirectory() || placeholder.isSymbolicLink() || fs.readdirSync(name).length !== 0) return false;
    fs.renameSync(capture, name);
    var live = fs.lstatSync(name, { bigint: true });
    return sameIdentity(live, held) && fsyncDirectoryRequired();
  } catch (error) {
    if (placeholder) {
      try {
        var livePlaceholder = fs.lstatSync(name, { bigint: true });
        if (sameStableDirectory(placeholder, livePlaceholder) && fs.readdirSync(name).length === 0) fs.rmdirSync(name);
      } catch (cleanupError) {}
    }
    return false;
  }
}

function finishLinkedRestore(request, names, manifest, decision, capture, live) {
  if (!capture || !live || !sameIdentity(capture, live) || capture.nlink !== 2n || live.nlink !== 2n ||
      !identityMatchesProof(capture, manifest.targetExpected, 'file') ||
      capture.size.toString() !== manifest.targetExpected.size ||
      capture.mtimeNs.toString() !== manifest.targetExpected.mtimeNs) return null;
  try {
    fs.unlinkSync(manifest.capture);
    if (!fsyncDirectoryRequired()) return { ok: false, code: 'restore-unsynced' };
    guardCrash(request, 'guard:after-restore-unlink');
    var sole = fs.lstatSync(manifest.name, { bigint: true });
    if (!guardCaptureMatches(sole, manifest) || sole.nlink !== 1n) return { ok: false, code: 'restore-unverified' };
    return finishGuardTransaction(request, names, manifest, decision, 'restored');
  } catch (error) { return { ok: false, code: 'restore-failed' }; }
}

function removeExactGuardCapture(manifest) {
  if (manifest.kind === 'file-unlink') {
    var stable = stableRegularStat(manifest.capture);
    if (!stable || !guardCaptureMatches(stable, manifest) || stable.nlink !== 1n) return false;
    try { fs.unlinkSync(manifest.capture); return fsyncDirectoryRequired(); }
    catch (error) { return false; }
  }
  var fd;
  try {
    var before = fs.lstatSync(manifest.capture, { bigint: true });
    if (!guardCaptureMatches(before, manifest) || fs.readdirSync(manifest.capture).length !== 0) return false;
    fd = fs.openSync(manifest.capture, fs.constants.O_RDONLY | (fs.constants.O_DIRECTORY || 0) |
      (fs.constants.O_NOFOLLOW || 0));
    var opened = fs.fstatSync(fd, { bigint: true });
    var after = fs.lstatSync(manifest.capture, { bigint: true });
    if (!sameStableDirectory(before, opened) || !sameStableDirectory(opened, after) ||
        fs.readdirSync(manifest.capture).length !== 0) return false;
    fs.rmdirSync(manifest.capture);
    return fsyncDirectoryRequired();
  } catch (error) { return false; }
  finally { if (fd !== undefined) try { fs.closeSync(fd); } catch (closeError) {} }
}

function cleanupGuardTransaction(request, names, manifest, decision, receipt) {
  if (decision && !unlinkWalRecord(names.decision, decision.checksum,
    function (value) { return validGuardDecision(value, manifest); })) return false;
  if (!unlinkWalRecord(names.receipt, receipt.checksum,
    function (value) { return validGuardReceipt(value, manifest); })) return false;
  guardCrash(request, 'guard:after-receipt-unlink');
  if (!unlinkWalRecord(names.manifest, manifest.checksum,
    function (value) { return validGuardManifest(value, manifest.name); })) return false;
  guardCrash(request, 'guard:after-manifest-unlink');
  return true;
}

function finishGuardTransaction(request, names, manifest, decision, outcome) {
  var receipt = withGuardChecksum(guardReceiptPayload(manifest.kind, manifest.operationId,
    manifest.name, manifest.capture, manifest.targetExpected, outcome));
  var published = publishWalRecord(request, names.receipt, receipt,
    function (value) { return validGuardReceipt(value, manifest); }, 'receipt');
  if (!published.ok) return { ok: false, code: published.code };
  guardCrash(request, 'guard:after-receipt');
  return cleanupGuardTransaction(request, names, manifest, decision, receipt)
    ? { ok: true, outcome: outcome }
    : { ok: false, code: 'wal-cleanup-failed' };
}

function loadGuardTransaction(name) {
  var names = guardNames(name);
  var manifestRead = settleWalPublication(names.manifest,
    function (value) { return validGuardManifest(value, name); });
  if (!manifestRead.ok) return { ok: false, code: manifestRead.code, names: names };
  if (!manifestRead.record &&
      (!missingName(names.decision) || !missingName(names.decision + '.stage') ||
        !missingName(names.receipt) || !missingName(names.receipt + '.stage'))) {
    return { ok: false, code: 'guard-wal-orphan', names: names };
  }
  return { ok: true, names: names, manifest: manifestRead.record };
}

function guardDecision(names, manifest) {
  var loaded = settleWalPublication(names.decision,
    function (value) { return validGuardDecision(value, manifest); });
  return loaded.ok ? { ok: true, decision: loaded.record } : loaded;
}

function guardReceipt(names, manifest) {
  var loaded = settleWalPublication(names.receipt,
    function (value) { return validGuardReceipt(value, manifest); });
  return loaded.ok ? { ok: true, receipt: loaded.record } : loaded;
}

function advanceGuardTransaction(request, names, manifest, pendingMode) {
  var decisionRead = guardDecision(names, manifest);
  if (!decisionRead.ok) return decisionRead;
  var decision = decisionRead.decision;
  var receiptRead = guardReceipt(names, manifest);
  if (!receiptRead.ok) return receiptRead;
  if (receiptRead.receipt) {
    var publicAtReceipt = guardPublicStat(manifest.name);
    var captureAtReceipt = guardPublicStat(manifest.capture);
    if (captureAtReceipt.exists ||
        (receiptRead.receipt.outcome === 'restored' &&
          (!publicAtReceipt.stat || !guardCaptureMatches(publicAtReceipt.stat, manifest))) ||
        (receiptRead.receipt.outcome !== 'restored' && publicAtReceipt.stat &&
          guardCaptureMatches(publicAtReceipt.stat, manifest))) {
      return { ok: false, code: 'receipt-state-mismatch' };
    }
    return cleanupGuardTransaction(request, names, manifest, decision, receiptRead.receipt)
      ? { ok: true, outcome: receiptRead.receipt.outcome }
      : { ok: false, code: 'wal-cleanup-failed' };
  }
  var desired = (manifest.intent === 'delete' || manifest.intent === 'delete-matching') ? 'delete' :
    (decision ? decision.decision : (pendingMode === 'resume' ? 'pending' : 'restore'));
  var capture = guardPublicStat(manifest.capture);
  var live = guardPublicStat(manifest.name);
  if (capture.exists) {
    if (desired === 'restore' && manifest.kind === 'file-unlink' && capture.stat && live.stat) {
      var linkedRestore = finishLinkedRestore(request, names, manifest, decision, capture.stat, live.stat);
      if (linkedRestore) return linkedRestore;
    }
    if (!capture.stat || !guardCaptureMatches(capture.stat, manifest)) {
      if (!live.exists) {
        if (manifest.kind === 'file-unlink') restoreForeignFileCapture(manifest.capture, manifest.name);
        else restoreForeignDirectoryCapture(manifest.capture, manifest.name);
      }
      return { ok: false, code: 'capture-foreign' };
    }
    if (desired === 'pending') return { ok: true, outcome: 'detached' };
    if (desired === 'restore') {
      if (live.exists || manifest.kind !== 'file-unlink' ||
          !restoreFileCapture(request, manifest.capture, manifest.name, manifest)) {
        return { ok: false, code: 'restore-blocked' };
      }
      guardCrash(request, 'guard:after-restore');
      return finishGuardTransaction(request, names, manifest, decision, 'restored');
    }
    if (!removeExactGuardCapture(manifest)) return { ok: false, code: 'capture-remove-failed' };
    guardCrash(request, 'guard:after-remove');
    return finishGuardTransaction(request, names, manifest, decision, 'deleted');
  }
  if (live.stat && guardCaptureMatches(live.stat, manifest) &&
      !exactStatMatchesProof(live.stat, manifest.targetExpected,
        manifest.kind === 'file-unlink' ? 'file' : 'directory')) {
    return desired !== 'delete'
      ? finishGuardTransaction(request, names, manifest, decision, 'restored')
      : { ok: false, code: 'target-proof-stale' };
  }
  if (live.exists && live.stat && exactStatMatchesProof(live.stat, manifest.targetExpected,
    manifest.kind === 'file-unlink' ? 'file' : 'directory')) {
    if (desired === 'restore') return finishGuardTransaction(request, names, manifest, decision, 'restored');
    if (manifest.kind === 'file-unlink') {
      if (live.stat.nlink !== 1n) return { ok: false, code: 'target-hardlinked' };
      replacePublicRegularForTest(manifest.name, request.testReplaceBeforeCaptureBytes, 'capture-test-displaced');
    } else {
      if (fs.readdirSync(manifest.name).length !== 0) return { ok: false, code: 'target-not-empty' };
      if (process.env.ORCHESTRATOR_FILE_GUARD_TEST_MODE === '1' && request.testReplaceBeforeCapture === true) {
        var testDisplaced = '.' + manifest.name + '-rmdir-test-displaced-' + crypto.randomBytes(12).toString('hex');
        fs.renameSync(manifest.name, testDisplaced);
        fs.mkdirSync(manifest.name, { mode: 0o700 });
        fsyncDirectoryRequired();
      }
    }
    if (process.env.ORCHESTRATOR_FILE_GUARD_TEST_MODE === '1' && request.testRenameTargetAwayAndBack === true) {
      var sameInodeDisplaced = '.' + manifest.name + '-same-inode-test-' + crypto.randomBytes(12).toString('hex');
      fs.renameSync(manifest.name, sameInodeDisplaced);
      fs.renameSync(sameInodeDisplaced, manifest.name);
      fsyncDirectoryRequired();
    }
    var immediatelyBeforeDetach;
    try { immediatelyBeforeDetach = fs.lstatSync(manifest.name, { bigint: true }); }
    catch (preDetachError) { return { ok: false, code: 'target-raced' }; }
    if (!exactStatMatchesProof(immediatelyBeforeDetach, manifest.targetExpected,
      manifest.kind === 'file-unlink' ? 'file' : 'directory')) {
      return { ok: false, code: 'target-proof-stale' };
    }
    try { fs.renameSync(manifest.name, manifest.capture); }
    catch (detachError) { return { ok: false, code: 'capture-detach-failed' }; }
    if (!fsyncDirectoryRequired()) return { ok: false, code: 'capture-detach-unsynced' };
    guardCrash(request, 'guard:after-detach');
    return advanceGuardTransaction(request, names, manifest, pendingMode);
  }
  if (live.exists && !live.stat) return { ok: false, code: 'public-entry-unsafe' };
  // A foreign public generation is not evidence that the authorized exact
  // generation was deleted by this transaction.  Preserve it and record an
  // aborted outcome; callers must never receive a deletion false-green.
  if (live.exists) return finishGuardTransaction(request, names, manifest, decision, 'aborted');
  return finishGuardTransaction(request, names, manifest, decision,
    desired === 'delete' ? 'deleted' : 'aborted');
}

function startGuardTransaction(request, name, kind, intent) {
  var type = kind === 'file-unlink' ? 'file' : 'directory';
  if (!validExactProof(request.targetExpected, type)) return { ok: false, code: 'target-proof-missing' };
  var live = guardPublicStat(name);
  if (!live.stat || !exactStatMatchesProof(live.stat, request.targetExpected, type)) {
    return { ok: false, code: 'target-mismatch' };
  }
  if (kind === 'file-unlink' && live.stat.nlink !== 1n) return { ok: false, code: 'target-hardlinked' };
  if (kind === 'empty-rmdir' && fs.readdirSync(name).length !== 0) return { ok: false, code: 'target-not-empty' };
  var operationId = crypto.randomBytes(16).toString('hex');
  var capture = '.guard-capture-' + operationId;
  var manifest = withGuardChecksum(guardManifestPayload(kind, intent, operationId,
    name, capture, request.targetExpected));
  var names = guardNames(name);
  var published = publishWalRecord(request, names.manifest, manifest,
    function (value) { return validGuardManifest(value, name); }, 'manifest');
  if (!published.ok) return published;
  guardCrash(request, 'guard:after-manifest');
  return { ok: true, names: names, manifest: manifest };
}

// A no-clobber transfer is not a single rename: the destination hardlink must
// become durable before the source name can be released.  Keep its WAL
// separate from the single-name unlink/rmdir records, but use the same
// checksummed, staged publication substrate.  The WAL key is the source name
// (not source+target), so two processes cannot start competing transfers for
// one source under different destinations.
function transferNames(source) {
  var key = crypto.createHash('sha256').update(source, 'utf8').digest('hex');
  var base = '.guard-transfer-' + key;
  return { manifest: base + '.json', link: base + '.link.json', receipt: base + '.receipt.json' };
}

function transferManifestPayload(operationId, source, target, capture, sourceExpected) {
  return { version: 1, kind: 'file-transfer', operationId: operationId,
    source: source, target: target, capture: capture, sourceExpected: sourceExpected };
}

function transferLinkPayload(operationId, source, target, linkedExpected) {
  return { version: 1, operationId: operationId, source: source,
    target: target, linkedExpected: linkedExpected };
}

function transferReceiptPayload(operationId, source, target, capture, outcome, targetFinal) {
  return { version: 1, kind: 'file-transfer', operationId: operationId,
    source: source, target: target, capture: capture, outcome: outcome,
    targetFinal: targetFinal };
}

function sameTransferGenerationProof(left, right) {
  return validExactProof(left, 'file') && validExactProof(right, 'file') &&
    ['dev', 'ino', 'mode', 'size', 'mtimeNs'].every(function (field) {
      return left[field] === right[field];
    });
}

function validTransferManifest(value, source) {
  if (!exactObjectKeys(value, ['version', 'kind', 'operationId', 'source', 'target',
    'capture', 'sourceExpected', 'checksum']) || value.version !== 1 ||
      value.kind !== 'file-transfer' || value.source !== source ||
      !safeName(value.source) || !safeName(value.target) || value.source === value.target ||
      !GUARD_OPERATION_RE.test(String(value.operationId || '')) ||
      value.capture !== '.guard-transfer-capture-' + value.operationId ||
      !safeName(value.capture) || !validExactProof(value.sourceExpected, 'file') ||
      value.sourceExpected.nlink !== '1') return false;
  return value.checksum === withGuardChecksum(transferManifestPayload(value.operationId,
    value.source, value.target, value.capture, value.sourceExpected)).checksum;
}

function validTransferLink(value, manifest) {
  if (!manifest || !exactObjectKeys(value,
    ['version', 'operationId', 'source', 'target', 'linkedExpected', 'checksum']) ||
      value.version !== 1 || value.operationId !== manifest.operationId ||
      value.source !== manifest.source || value.target !== manifest.target ||
      !validExactProof(value.linkedExpected, 'file') || value.linkedExpected.nlink !== '2' ||
      !sameTransferGenerationProof(value.linkedExpected, manifest.sourceExpected)) return false;
  return value.checksum === withGuardChecksum(transferLinkPayload(value.operationId,
    value.source, value.target, value.linkedExpected)).checksum;
}

function validTransferReceipt(value, manifest) {
  if (!manifest || !exactObjectKeys(value, ['version', 'kind', 'operationId', 'source',
    'target', 'capture', 'outcome', 'targetFinal', 'checksum']) || value.version !== 1 ||
      value.kind !== 'file-transfer' || value.operationId !== manifest.operationId ||
      value.source !== manifest.source || value.target !== manifest.target ||
      value.capture !== manifest.capture ||
      ['transferred', 'source-replaced', 'aborted'].indexOf(value.outcome) < 0) return false;
  if (value.outcome === 'aborted') {
    if (value.targetFinal !== null) return false;
  } else if (!validExactProof(value.targetFinal, 'file') || value.targetFinal.nlink !== '1' ||
      !sameTransferGenerationProof(value.targetFinal, manifest.sourceExpected)) return false;
  return value.checksum === withGuardChecksum(transferReceiptPayload(value.operationId,
    value.source, value.target, value.capture, value.outcome, value.targetFinal)).checksum;
}

function transferGenerationMatches(stat, manifest) {
  return !!stat && identityMatchesProof(stat, manifest.sourceExpected, 'file') &&
    String(stat.size) === manifest.sourceExpected.size &&
    String(stat.mtimeNs) === manifest.sourceExpected.mtimeNs;
}

function loadTransferTransaction(source) {
  var names = transferNames(source);
  var manifestRead = settleWalPublication(names.manifest,
    function (value) { return validTransferManifest(value, source); });
  if (!manifestRead.ok) return { ok: false, code: manifestRead.code, names: names };
  if (!manifestRead.record) {
    // Cleanup always removes link/receipt before manifest.  Anything left in
    // the reverse state is conflicting evidence, never authority to start a
    // new transfer.
    if (!missingName(names.link) || !missingName(names.link + '.stage') ||
        !missingName(names.receipt) || !missingName(names.receipt + '.stage')) {
      return { ok: false, code: 'transfer-wal-orphan', names: names };
    }
  }
  return { ok: true, names: names, manifest: manifestRead.record };
}

function transferLink(names, manifest) {
  var loaded = settleWalPublication(names.link,
    function (value) { return validTransferLink(value, manifest); });
  return loaded.ok ? { ok: true, link: loaded.record } : loaded;
}

function transferReceipt(names, manifest) {
  var loaded = settleWalPublication(names.receipt,
    function (value) { return validTransferReceipt(value, manifest); });
  return loaded.ok ? { ok: true, receipt: loaded.record } : loaded;
}

function cleanupTransferTransaction(request, names, manifest, link, receipt) {
  if (link && !unlinkWalRecord(names.link, link.checksum,
    function (value) { return validTransferLink(value, manifest); })) return false;
  guardCrash(request, 'guard:after-transfer-link-unlink');
  if (!unlinkWalRecord(names.receipt, receipt.checksum,
    function (value) { return validTransferReceipt(value, manifest); })) return false;
  guardCrash(request, 'guard:after-transfer-receipt-unlink');
  if (!unlinkWalRecord(names.manifest, manifest.checksum,
    function (value) { return validTransferManifest(value, manifest.source); })) return false;
  guardCrash(request, 'guard:after-transfer-manifest-unlink');
  return true;
}

function transferReceiptStateMatches(manifest, receipt) {
  var capture = guardPublicStat(manifest.capture);
  if (capture.exists) return false;
  if (receipt.outcome === 'aborted') return true;
  var target = guardPublicStat(manifest.target);
  if (!target.stat || !exactStatMatchesProof(target.stat, receipt.targetFinal, 'file')) return false;
  if (receipt.outcome === 'transferred' && guardPublicStat(manifest.source).exists) return false;
  return true;
}

function finishTransferTransaction(request, names, manifest, link, outcome) {
  var targetFinal = null;
  if (outcome !== 'aborted') {
    var stableTarget = stableRegularStat(manifest.target);
    if (!stableTarget || stableTarget.nlink !== 1n ||
        !transferGenerationMatches(stableTarget, manifest)) {
      return { ok: false, code: 'transfer-final-target-unsafe' };
    }
    if (outcome === 'transferred' && !missingName(manifest.source)) {
      return { ok: false, code: 'transfer-source-not-released' };
    }
    targetFinal = statShape(stableTarget);
  }
  if (!missingName(manifest.capture)) return { ok: false, code: 'transfer-capture-remains' };
  var receipt = withGuardChecksum(transferReceiptPayload(manifest.operationId,
    manifest.source, manifest.target, manifest.capture, outcome, targetFinal));
  var published = publishWalRecord(request, names.receipt, receipt,
    function (value) { return validTransferReceipt(value, manifest); }, 'transfer-receipt');
  if (!published.ok) return published;
  guardCrash(request, 'guard:after-transfer-receipt');
  return cleanupTransferTransaction(request, names, manifest, link, receipt)
    ? { ok: true, outcome: outcome }
    : { ok: false, code: 'transfer-wal-cleanup-failed' };
}

function publishTransferLink(request, names, manifest, sourceStat, targetStat) {
  if (!sourceStat || !targetStat || !sameStableFile(sourceStat, targetStat) ||
      sourceStat.nlink !== 2n || !transferGenerationMatches(sourceStat, manifest)) {
    return { ok: false, code: 'transfer-link-unverified' };
  }
  var link = withGuardChecksum(transferLinkPayload(manifest.operationId,
    manifest.source, manifest.target, statShape(targetStat)));
  var published = publishWalRecord(request, names.link, link,
    function (value) { return validTransferLink(value, manifest); }, 'transfer-link');
  if (!published.ok) return published;
  guardCrash(request, 'guard:after-transfer-link-record');
  return { ok: true, link: link };
}

function restoreTransferCapture(request, manifest, captureStat, sourceStat) {
  try {
    if (sourceStat) {
      if (!sameIdentity(sourceStat, captureStat) || sourceStat.nlink !== 2n || captureStat.nlink !== 2n ||
          !transferGenerationMatches(sourceStat, manifest)) return false;
    } else {
      if (captureStat.nlink !== 1n) return false;
      fs.linkSync(manifest.capture, manifest.source);
      if (!fsyncDirectoryRequired()) return false;
      guardCrash(request, 'guard:after-transfer-rollback-link');
      sourceStat = fs.lstatSync(manifest.source, { bigint: true });
      captureStat = fs.lstatSync(manifest.capture, { bigint: true });
      if (!sameIdentity(sourceStat, captureStat) || sourceStat.nlink !== 2n ||
          captureStat.nlink !== 2n || !transferGenerationMatches(sourceStat, manifest)) return false;
    }
    fs.unlinkSync(manifest.capture);
    if (!fsyncDirectoryRequired()) return false;
    guardCrash(request, 'guard:after-transfer-rollback-capture-remove');
    var sole = stableRegularStat(manifest.source);
    return !!sole && sole.nlink === 1n && transferGenerationMatches(sole, manifest);
  } catch (error) { return false; }
}

function advanceTransferTransaction(request, names, manifest) {
  var receiptRead = transferReceipt(names, manifest);
  if (!receiptRead.ok) return receiptRead;
  var linkRead = transferLink(names, manifest);
  if (!linkRead.ok) return linkRead;
  var link = linkRead.link;
  if (receiptRead.receipt) {
    if (!transferReceiptStateMatches(manifest, receiptRead.receipt)) {
      return { ok: false, code: 'transfer-receipt-state-mismatch' };
    }
    return cleanupTransferTransaction(request, names, manifest, link, receiptRead.receipt)
      ? { ok: true, outcome: receiptRead.receipt.outcome }
      : { ok: false, code: 'transfer-wal-cleanup-failed' };
  }

  var source = guardPublicStat(manifest.source);
  var target = guardPublicStat(manifest.target);
  var capture = guardPublicStat(manifest.capture);
  if ((source.exists && !source.stat) || (target.exists && !target.stat) ||
      (capture.exists && !capture.stat)) return { ok: false, code: 'transfer-entry-unsafe' };
  var sourceOwned = transferGenerationMatches(source.stat, manifest);
  var targetOwned = transferGenerationMatches(target.stat, manifest);
  var captureOwned = transferGenerationMatches(capture.stat, manifest);

  if (capture.exists && !captureOwned) {
    // A racer may win the final rename window and get its generation captured.
    // Never delete it.  Restore that regular generation no-clobber when the
    // public source is free, then settle only the original target generation.
    if (source.exists || !restoreForeignFileCapture(manifest.capture, manifest.source)) {
      return { ok: false, code: 'transfer-capture-foreign' };
    }
    source = guardPublicStat(manifest.source);
    capture = guardPublicStat(manifest.capture);
    if (capture.exists) return { ok: false, code: 'transfer-capture-foreign' };
    return targetOwned && target.stat.nlink === 1n
      ? finishTransferTransaction(request, names, manifest, link, 'source-replaced')
      : finishTransferTransaction(request, names, manifest, link, 'aborted');
  }

  if (captureOwned) {
    if (targetOwned) {
      if (!sameIdentity(capture.stat, target.stat) || capture.stat.nlink !== 2n ||
          target.stat.nlink !== 2n) return { ok: false, code: 'transfer-capture-alias-unsafe' };
      if (sourceOwned) return { ok: false, code: 'transfer-source-alias-unsafe' };
      guardCrash(request, 'guard:before-transfer-capture-remove');
      try {
        fs.unlinkSync(manifest.capture);
        if (!fsyncDirectoryRequired()) return { ok: false, code: 'transfer-capture-remove-unsynced' };
      } catch (captureRemoveError) { return { ok: false, code: 'transfer-capture-remove-failed' }; }
      guardCrash(request, 'guard:after-transfer-capture-remove');
      return finishTransferTransaction(request, names, manifest, link,
        source.exists ? 'source-replaced' : 'transferred');
    }
    // The target disappeared or became foreign after source detach.  Roll the
    // exact owned generation back to source without touching that target.
    if (source.exists && !sourceOwned) return { ok: false, code: 'transfer-source-occupied' };
    if (!restoreTransferCapture(request, manifest, capture.stat, sourceOwned ? source.stat : null)) {
      return { ok: false, code: 'transfer-rollback-failed' };
    }
    return finishTransferTransaction(request, names, manifest, link, 'aborted');
  }

  if (targetOwned) {
    if (sourceOwned) {
      if (!sameIdentity(source.stat, target.stat) || source.stat.nlink !== 2n || target.stat.nlink !== 2n) {
        return { ok: false, code: 'transfer-link-state-unsafe' };
      }
      if (!link) {
        var recoveredLink = publishTransferLink(request, names, manifest, source.stat, target.stat);
        if (!recoveredLink.ok) return recoveredLink;
        link = recoveredLink.link;
      }
      if (!exactStatMatchesProof(source.stat, link.linkedExpected, 'file') ||
          !exactStatMatchesProof(target.stat, link.linkedExpected, 'file')) {
        return { ok: false, code: 'transfer-link-proof-stale' };
      }
      if (process.env.ORCHESTRATOR_FILE_GUARD_TEST_MODE === '1' &&
          typeof request.testReplaceBeforeCaptureBytes === 'string') {
        fs.unlinkSync(manifest.source);
        var foreignBytes = Buffer.from(request.testReplaceBeforeCaptureBytes, 'base64');
        var foreignFd = fs.openSync(manifest.source, fs.constants.O_WRONLY | fs.constants.O_CREAT |
          fs.constants.O_EXCL | (fs.constants.O_NOFOLLOW || 0), 0o600);
        try { writeAll(foreignFd, foreignBytes); fs.fsyncSync(foreignFd); }
        finally { fs.closeSync(foreignFd); }
        fsyncDirectoryRequired();
        return advanceTransferTransaction(request, names, manifest);
      }
      var immediateSource, immediateTarget;
      try {
        immediateSource = fs.lstatSync(manifest.source, { bigint: true });
        immediateTarget = fs.lstatSync(manifest.target, { bigint: true });
      } catch (preDetachError) { return { ok: false, code: 'transfer-pre-detach-raced' }; }
      if (!exactStatMatchesProof(immediateSource, link.linkedExpected, 'file') ||
          !exactStatMatchesProof(immediateTarget, link.linkedExpected, 'file') ||
          !sameIdentity(immediateSource, immediateTarget)) {
        return { ok: false, code: 'transfer-pre-detach-proof-stale' };
      }
      guardCrash(request, 'guard:before-transfer-source-detach');
      try { fs.renameSync(manifest.source, manifest.capture); }
      catch (detachError) { return { ok: false, code: 'transfer-source-detach-failed' }; }
      if (!fsyncDirectoryRequired()) return { ok: false, code: 'transfer-source-detach-unsynced' };
      guardCrash(request, 'guard:after-transfer-source-detach');
      return advanceTransferTransaction(request, names, manifest);
    }
    if (target.stat.nlink !== 1n) return { ok: false, code: 'transfer-target-hardlinked' };
    return finishTransferTransaction(request, names, manifest, link,
      source.exists ? 'source-replaced' : 'transferred');
  }

  if (target.exists) {
    // A no-clobber foreign target wins.  If no owned capture exists, cleanup is
    // metadata-only and cannot overwrite either public generation.
    if (sourceOwned && source.stat.nlink !== 1n) {
      return { ok: false, code: 'transfer-source-hardlinked' };
    }
    return finishTransferTransaction(request, names, manifest, link, 'aborted');
  }

  if (!source.exists) return finishTransferTransaction(request, names, manifest, link, 'aborted');
  if (!sourceOwned) return finishTransferTransaction(request, names, manifest, link, 'aborted');
  if (link) {
    // A once-linked generation whose target was externally removed no longer
    // matches the immutable post-link proof.  Preserve source and abort rather
    // than silently re-publishing after an external no-clobber decision.
    if (source.stat.nlink !== 1n) return { ok: false, code: 'transfer-source-hardlinked' };
    return finishTransferTransaction(request, names, manifest, link, 'aborted');
  }
  if (!exactStatMatchesProof(source.stat, manifest.sourceExpected, 'file') || source.stat.nlink !== 1n) {
    return { ok: false, code: 'transfer-source-proof-stale' };
  }
  guardCrash(request, 'guard:before-transfer-target-link');
  try {
    fs.linkSync(manifest.source, manifest.target);
    if (!fsyncDirectoryRequired()) return { ok: false, code: 'transfer-target-link-unsynced' };
  } catch (linkError) { return { ok: false, code: 'transfer-target-link-failed' }; }
  guardCrash(request, 'guard:after-transfer-target-link');
  var linkedSource, linkedTarget;
  try {
    linkedSource = fs.lstatSync(manifest.source, { bigint: true });
    linkedTarget = fs.lstatSync(manifest.target, { bigint: true });
  } catch (linkedReadError) { return { ok: false, code: 'transfer-target-link-raced' }; }
  var publishedLink = publishTransferLink(request, names, manifest, linkedSource, linkedTarget);
  if (!publishedLink.ok) return publishedLink;
  return advanceTransferTransaction(request, names, manifest);
}

function startTransferTransaction(request) {
  if (!validExactProof(request.sourceExpected, 'file') || request.sourceExpected.nlink !== '1') {
    return { ok: false, code: 'transfer-source-proof-missing' };
  }
  var source = guardPublicStat(request.source);
  var target = guardPublicStat(request.target);
  if (!source.stat || !exactStatMatchesProof(source.stat, request.sourceExpected, 'file') ||
      source.stat.nlink !== 1n) return { ok: false, code: 'transfer-source-mismatch' };
  if (target.exists) return { ok: false, code: 'transfer-target-exists' };
  var operationId = crypto.randomBytes(16).toString('hex');
  var capture = '.guard-transfer-capture-' + operationId;
  if (!missingName(capture)) return { ok: false, code: 'transfer-capture-exists' };
  var manifest = withGuardChecksum(transferManifestPayload(operationId,
    request.source, request.target, capture, request.sourceExpected));
  var names = transferNames(request.source);
  var published = publishWalRecord(request, names.manifest, manifest,
    function (value) { return validTransferManifest(value, request.source); }, 'transfer-manifest');
  if (!published.ok) return published;
  guardCrash(request, 'guard:after-transfer-manifest');
  return { ok: true, names: names, manifest: manifest };
}

// No-clobber publication and exact compare-and-swap share one deterministic
// target-keyed journal.  Desired bytes live in the durable manifest, so a
// recovery scan can recreate a partially-written private data stage without
// relying on the crashed caller or guessing at random temp files.
function publicationNames(name) {
  var key = crypto.createHash('sha256').update(name, 'utf8').digest('hex');
  var base = '.guard-publish-' + key;
  return { manifest: base + '.json', link: base + '.link.json', receipt: base + '.receipt.json' };
}

function publicationManifestPayload(kind, operationId, creatorToken, name, stage, oldCapture,
  replacementBytes, replacementHash, mode, expectedProof, expectedHash) {
  return { version: 1, kind: kind, operationId: operationId, creatorToken: creatorToken, name: name,
    stage: stage, oldCapture: oldCapture, replacementBytes: replacementBytes,
    replacementHash: replacementHash, mode: mode,
    expectedProof: expectedProof, expectedHash: expectedHash };
}

function publicationReceiptPayload(kind, operationId, creatorToken, name, stage, oldCapture,
  replacementHash, expectedProof, outcome, targetFinal) {
  return { version: 2, kind: kind, operationId: operationId, creatorToken: creatorToken, name: name,
    stage: stage, oldCapture: oldCapture, replacementHash: replacementHash,
    expectedProof: expectedProof, outcome: outcome, targetFinal: targetFinal };
}

function publicationLinkPayload(kind, operationId, name, stage, replacementHash, linkedExpected) {
  return { version: 1, kind: kind, operationId: operationId, name: name,
    stage: stage, replacementHash: replacementHash, linkedExpected: linkedExpected };
}

function canonicalBase64(value, maxBytes) {
  if (typeof value !== 'string') return null;
  var bytes = Buffer.from(value, 'base64');
  return bytes.length <= maxBytes && bytes.toString('base64') === value ? bytes : null;
}

function validPublicationManifest(value, name) {
  if (!exactObjectKeys(value, ['version', 'kind', 'operationId', 'creatorToken', 'name', 'stage',
    'oldCapture', 'replacementBytes', 'replacementHash', 'mode', 'expectedProof',
    'expectedHash', 'checksum']) || value.version !== 1 ||
      (value.kind !== 'file-publish' && value.kind !== 'file-cas') || value.name !== name ||
      !safeName(value.name) || !GUARD_OPERATION_RE.test(String(value.operationId || '')) ||
      !GUARD_OPERATION_RE.test(String(value.creatorToken || '')) ||
      value.stage !== '.guard-publish-data-' + value.operationId || !safeName(value.stage) ||
      !Number.isInteger(value.mode) || value.mode < 0 || value.mode > 0o777 ||
      (process.platform !== 'win32' && (value.mode & 0o077) !== 0) ||
      !/^sha256:[a-f0-9]{64}$/.test(String(value.replacementHash || ''))) return false;
  var replacement = canonicalBase64(value.replacementBytes, GUARD_PUBLISH_MAX_BYTES);
  if (!replacement || sha256(replacement) !== value.replacementHash) return false;
  if (value.kind === 'file-publish') {
    if (value.oldCapture !== null || value.expectedProof !== null || value.expectedHash !== null) return false;
  } else if (value.oldCapture !== '.guard-cas-old-' + value.operationId ||
      !safeName(value.oldCapture) || !validExactProof(value.expectedProof, 'file') ||
      value.expectedProof.nlink !== '1' ||
      !/^sha256:[a-f0-9]{64}$/.test(String(value.expectedHash || ''))) return false;
  return value.checksum === withGuardChecksum(publicationManifestPayload(value.kind,
    value.operationId, value.creatorToken, value.name, value.stage, value.oldCapture, value.replacementBytes,
    value.replacementHash, value.mode, value.expectedProof, value.expectedHash)).checksum;
}

function validPublicationLink(value, manifest) {
  if (!manifest || !exactObjectKeys(value, ['version', 'kind', 'operationId', 'name',
    'stage', 'replacementHash', 'linkedExpected', 'checksum']) || value.version !== 1 ||
      value.kind !== manifest.kind || value.operationId !== manifest.operationId ||
      value.name !== manifest.name || value.stage !== manifest.stage ||
      value.replacementHash !== manifest.replacementHash ||
      !validExactProof(value.linkedExpected, 'file') || value.linkedExpected.nlink !== '2') return false;
  return value.checksum === withGuardChecksum(publicationLinkPayload(value.kind,
    value.operationId, value.name, value.stage, value.replacementHash,
    value.linkedExpected)).checksum;
}

function validStandalonePublicationReceipt(value) {
  var receiptKeys = ['version', 'kind', 'operationId', 'name', 'stage', 'oldCapture',
    'replacementHash', 'expectedProof', 'outcome', 'targetFinal', 'checksum', 'creatorToken'];
  if (!exactObjectKeys(value, receiptKeys) || value.version !== 2 ||
      (value.kind !== 'file-publish' && value.kind !== 'file-cas') ||
      !GUARD_OPERATION_RE.test(String(value.operationId || '')) || !safeName(value.name) ||
      !GUARD_OPERATION_RE.test(String(value.creatorToken || '')) ||
      value.stage !== '.guard-publish-data-' + value.operationId ||
      !/^sha256:[a-f0-9]{64}$/.test(String(value.replacementHash || '')) ||
      (value.outcome !== 'published' && value.outcome !== 'aborted')) return false;
  if (value.kind === 'file-publish') {
    if (value.oldCapture !== null || value.expectedProof !== null) return false;
  } else if (value.oldCapture !== '.guard-cas-old-' + value.operationId ||
      !validExactProof(value.expectedProof, 'file') || value.expectedProof.nlink !== '1') return false;
  if (value.outcome === 'published') {
    if (!validExactProof(value.targetFinal, 'file') || value.targetFinal.nlink !== '1') return false;
  } else if (value.targetFinal !== null) return false;
  var payload = publicationReceiptPayload(value.kind, value.operationId, value.creatorToken,
    value.name, value.stage, value.oldCapture, value.replacementHash, value.expectedProof,
    value.outcome, value.targetFinal);
  return value.checksum === withGuardChecksum(payload).checksum;
}

function validPublicationReceipt(value, manifest) {
  return !!manifest && validStandalonePublicationReceipt(value) &&
    value.kind === manifest.kind && value.operationId === manifest.operationId &&
    value.creatorToken === manifest.creatorToken &&
    value.name === manifest.name && value.stage === manifest.stage &&
    value.oldCapture === manifest.oldCapture &&
    value.replacementHash === manifest.replacementHash &&
    JSON.stringify(value.expectedProof) === JSON.stringify(manifest.expectedProof);
}

function publicationModeMatches(stat, manifest) {
  return process.platform === 'win32' || Number(stat.mode & 0o777n) === manifest.mode;
}

function publicationRead(name) {
  return writerRegularFile(name, GUARD_PUBLISH_MAX_BYTES);
}

function publicationDesired(read, manifest) {
  return !!read && read.proof.hash === manifest.replacementHash &&
    read.bytes.equals(Buffer.from(manifest.replacementBytes, 'base64')) &&
    publicationModeMatches(read.stat, manifest);
}

function publicationExpected(read, manifest, requireExact) {
  if (!read || manifest.kind !== 'file-cas' || read.proof.hash !== manifest.expectedHash) return false;
  if (requireExact) return exactStatMatchesProof(read.stat, manifest.expectedProof, 'file');
  return identityMatchesProof(read.stat, manifest.expectedProof, 'file') &&
    String(read.stat.size) === manifest.expectedProof.size &&
    String(read.stat.mtimeNs) === manifest.expectedProof.mtimeNs &&
    String(read.stat.mode) === manifest.expectedProof.mode;
}

function publicationLinkedOwned(read, manifest, link, nlink) {
  return !!link && publicationDesired(read, manifest) &&
    identityMatchesProof(read.stat, link.linkedExpected, 'file') &&
    String(read.stat.nlink) === String(nlink) &&
    String(read.stat.size) === link.linkedExpected.size &&
    String(read.stat.mtimeNs) === link.linkedExpected.mtimeNs;
}

function publicationLinkedPair(targetRead, stageRead, manifest, link) {
  return publicationLinkedOwned(targetRead, manifest, link, 2) &&
    publicationLinkedOwned(stageRead, manifest, link, 2) &&
    sameIdentity(targetRead.stat, stageRead.stat);
}

function privatePublicationEntry(stat) {
  return !!stat && stat.isFile() && !stat.isSymbolicLink() && stat.nlink === 1n &&
    (process.platform === 'win32' || (stat.mode & 0o077n) === 0n);
}

function ensurePublicationStage(request, manifest) {
  var desired = Buffer.from(manifest.replacementBytes, 'base64');
  var existing = guardPublicStat(manifest.stage);
  if (existing.stat) {
    var existingRead = publicationRead(manifest.stage);
    if (publicationDesired(existingRead, manifest)) {
      if (existingRead.stat.nlink === 1n) return existingRead;
      if (existingRead.stat.nlink === 2n) return { raced: true };
    }
    if (!privatePublicationEntry(existing.stat)) return null;
    // Another process may be writing the just-created protocol stage. Let its
    // fsync become visible before treating a stable partial stage as crash
    // residue. PID-skewed bounds ensure concurrent helpers do not all truncate
    // the same reserved inode at once.
    var stageWaits = 24 + (process.pid % 73);
    for (var stageWait = 0; stageWait < stageWaits; stageWait++) {
      guardBriefWait(1);
      var waitedEntry = guardPublicStat(manifest.stage);
      if (!waitedEntry.stat) return null;
      var waitedRead = publicationRead(manifest.stage);
      if (publicationDesired(waitedRead, manifest)) {
        if (waitedRead.stat.nlink === 1n) return waitedRead;
        if (waitedRead.stat.nlink === 2n) return { raced: true };
      }
      if (!privatePublicationEntry(waitedEntry.stat)) return null;
    }
  } else if (existing.exists) return null;
  var fd;
  try {
    if (!existing.exists) {
      fd = fs.openSync(manifest.stage, fs.constants.O_WRONLY | fs.constants.O_CREAT |
        fs.constants.O_EXCL | (fs.constants.O_NOFOLLOW || 0), manifest.mode);
      guardCrash(request, 'guard:after-publish-data-create');
    } else {
      fd = fs.openSync(manifest.stage, fs.constants.O_WRONLY | (fs.constants.O_NOFOLLOW || 0));
      var openedExisting = fs.fstatSync(fd, { bigint: true });
      if (!sameIdentity(openedExisting, existing.stat) || !privatePublicationEntry(openedExisting)) return null;
      fs.ftruncateSync(fd, 0);
    }
    try { fs.fchmodSync(fd, manifest.mode); }
    catch (modeError) { if (process.platform !== 'win32') throw modeError; }
    writeAll(fd, desired);
    fs.fsyncSync(fd);
    var written = fs.fstatSync(fd, { bigint: true });
    fs.closeSync(fd); fd = undefined;
    var live = fs.lstatSync(manifest.stage, { bigint: true });
    if (!sameIdentity(written, live) || live.nlink !== 1n ||
        live.size !== BigInt(desired.length) || !publicationModeMatches(live, manifest)) return null;
    if (!fsyncDirectoryRequired()) return null;
    guardCrash(request, 'guard:after-publish-data-fsync');
    var verified = publicationRead(manifest.stage);
    return publicationDesired(verified, manifest) && verified.stat.nlink === 1n ? verified : null;
  } catch (error) { return null; }
  finally { if (fd !== undefined) try { fs.closeSync(fd); } catch (closeError) {} }
}

function removePublicationStage(request, manifest, targetRead, allowPartial) {
  var stageEntry = guardPublicStat(manifest.stage);
  if (!stageEntry.exists) return true;
  if (!stageEntry.stat || !stageEntry.stat.isFile() || stageEntry.stat.isSymbolicLink() ||
      (process.platform !== 'win32' && (stageEntry.stat.mode & 0o077n) !== 0n)) return false;
  var stageRead = publicationRead(manifest.stage);
  if (!allowPartial && !publicationDesired(stageRead, manifest)) return false;
  if (stageEntry.stat.nlink === 2n) {
    if (!targetRead || !sameIdentity(stageEntry.stat, targetRead.stat) ||
        !publicationDesired(targetRead, manifest)) return false;
  } else if (stageEntry.stat.nlink !== 1n) return false;
  guardCrash(request, 'guard:before-publish-stage-remove');
  try {
    fs.unlinkSync(manifest.stage);
    if (!fsyncDirectoryRequired()) return false;
  } catch (error) {
    if (!error || error.code !== 'ENOENT' || !missingName(manifest.stage)) return false;
  }
  guardCrash(request, 'guard:after-publish-stage-remove');
  return true;
}

function loadPublicationTransaction(name, preserveForeignTerminalForToken) {
  var names = publicationNames(name);
  var manifestRead = settleWalPublication(names.manifest,
    function (value) { return validPublicationManifest(value, name); });
  if (!manifestRead.ok) return { ok: false, code: manifestRead.code, names: names };
  if (!manifestRead.record) {
    if (!missingName(names.link) || !missingName(names.link + '.stage')) {
      return { ok: false, code: 'publish-wal-orphan', names: names };
    }
    var terminalRead = settleWalPublication(names.receipt, validStandalonePublicationReceipt);
    if (!terminalRead.ok) return { ok: false, code: terminalRead.code, names: names };
    if (!terminalRead.record) return { ok: true, names: names, manifest: null, terminal: null };
    if (terminalRead.record.name !== name || !terminalPublicationReceiptStateMatches(terminalRead.record)) {
      return { ok: false, code: 'publish-terminal-receipt-state-mismatch', names: names };
    }
    if (GUARD_OPERATION_RE.test(String(preserveForeignTerminalForToken || '')) &&
        terminalRead.record.creatorToken !== preserveForeignTerminalForToken) {
      return { ok: true, names: names, manifest: null,
        terminal: terminalRead.record, terminalRetained: true };
    }
    if (!unlinkWalRecord(names.receipt, terminalRead.record.checksum,
      validStandalonePublicationReceipt)) {
      return { ok: false, code: 'publish-terminal-receipt-cleanup-failed', names: names };
    }
    return { ok: true, names: names, manifest: null, terminal: terminalRead.record };
  }
  return { ok: true, names: names, manifest: manifestRead.record };
}

function publicationLink(names, manifest) {
  var loaded = settleWalPublication(names.link,
    function (value) { return validPublicationLink(value, manifest); });
  return loaded.ok ? { ok: true, link: loaded.record } : loaded;
}

function publishPublicationLink(request, names, manifest) {
  var targetRead = publicationRead(manifest.name);
  var stageRead = publicationRead(manifest.stage);
  if (!publicationDesired(targetRead, manifest) || !publicationDesired(stageRead, manifest) ||
      !sameIdentity(targetRead.stat, stageRead.stat) || targetRead.stat.nlink !== 2n ||
      stageRead.stat.nlink !== 2n) return { ok: false, code: 'publish-link-state-unsafe' };
  var linkedExpected = statShape(targetRead.stat);
  if (!exactStatMatchesProof(stageRead.stat, linkedExpected, 'file')) {
    return { ok: false, code: 'publish-link-proof-unstable' };
  }
  var link = withGuardChecksum(publicationLinkPayload(manifest.kind,
    manifest.operationId, manifest.name, manifest.stage,
    manifest.replacementHash, linkedExpected));
  var published = publishWalRecord(request, names.link, link,
    function (value) { return validPublicationLink(value, manifest); }, 'publish-link');
  if (!published.ok) return published;
  guardCrash(request, 'guard:after-publish-link-record');
  return { ok: true, link: link };
}

function publicationReceipt(names, manifest) {
  var loaded = settleWalPublication(names.receipt,
    function (value) { return validPublicationReceipt(value, manifest); });
  return loaded.ok ? { ok: true, receipt: loaded.record } : loaded;
}

function cleanupPublicationTransaction(request, names, manifest, link, receipt) {
  if (link) {
    if (!unlinkWalRecord(names.link, link.checksum,
      function (value) { return validPublicationLink(value, manifest); })) return false;
    guardCrash(request, 'guard:after-publish-link-unlink');
  }
  if (!unlinkWalRecord(names.manifest, manifest.checksum,
    function (value) { return validPublicationManifest(value, manifest.name); })) return false;
  guardCrash(request, 'guard:after-publish-manifest-unlink');
  // Keep one small, target-keyed terminal receipt until the parent confirms it
  // received an append response. A transport fallback with the same creator
  // token can then recognize the already-committed append instead of applying
  // its bytes twice. Ordinary publications and aborted appends still clean in
  // one phase.
  if (request.action === 'append-bounded' && receipt.outcome === 'published') return true;
  if (!unlinkWalRecord(names.receipt, receipt.checksum,
    validStandalonePublicationReceipt)) return false;
  guardCrash(request, 'guard:after-publish-receipt-unlink');
  return true;
}

function publicationCleanupAuthorized(request, manifest) {
  if (!request) return false;
  if (request.action === 'reconcile-guard-transactions' ||
      request.publicationToken === manifest.creatorToken) return true;
  var samePayload = request.bytes === manifest.replacementBytes &&
    ((manifest.kind === 'file-publish' &&
        (request.action === 'publish-no-clobber' || request.action === 'append-bounded')) ||
      (manifest.kind === 'file-cas' &&
        (request.action === 'compare-and-swap' || request.action === 'append-bounded') &&
        JSON.stringify(request.expectedProof) === JSON.stringify(manifest.expectedProof)));
  return samePayload;
}

function terminalPublicationReceiptStateMatches(receipt) {
  if (!missingName(receipt.stage) || (receipt.oldCapture && !missingName(receipt.oldCapture))) return false;
  if (receipt.outcome === 'aborted') return true;
  var targetRead = publicationRead(receipt.name);
  return !!targetRead && exactStatMatchesProof(targetRead.stat, receipt.targetFinal, 'file') &&
    targetRead.proof.hash === receipt.replacementHash;
}

function publicationReceiptStateMatches(manifest, receipt) {
  return validPublicationReceipt(receipt, manifest) &&
    terminalPublicationReceiptStateMatches(receipt);
}

function finishPublicationTransaction(request, names, manifest, link, outcome) {
  var targetFinal = null;
  if (outcome === 'published') {
    var targetRead = publicationRead(manifest.name);
    if (!publicationLinkedOwned(targetRead, manifest, link, 1)) {
      return { ok: false, code: 'publish-final-target-unsafe' };
    }
    targetFinal = statShape(targetRead.stat);
  }
  if (!missingName(manifest.stage) || (manifest.oldCapture && !missingName(manifest.oldCapture))) {
    return { ok: false, code: 'publish-private-evidence-remains' };
  }
  var receipt = withGuardChecksum(publicationReceiptPayload(manifest.kind,
    manifest.operationId, manifest.creatorToken, manifest.name, manifest.stage, manifest.oldCapture,
    manifest.replacementHash, manifest.expectedProof, outcome, targetFinal));
  var published = publishWalRecord(request, names.receipt, receipt,
    function (value) { return validPublicationReceipt(value, manifest); }, 'publish-receipt');
  if (!published.ok) return published;
  guardCrash(request, 'guard:after-publish-receipt');
  if (!publicationCleanupAuthorized(request, manifest)) {
    return { ok: true, outcome: outcome, stat: targetFinal, cleanupPending: true };
  }
  return cleanupPublicationTransaction(request, names, manifest, link, receipt)
    ? { ok: true, outcome: outcome, stat: targetFinal }
    : { ok: false, code: 'publish-wal-cleanup-failed' };
}

function abortPublicationTransaction(request, names, manifest, link) {
  if (manifest.oldCapture && !missingName(manifest.oldCapture)) {
    return { ok: false, code: 'cas-old-capture-retained' };
  }
  if (!removePublicationStage(request, manifest, null, !link)) {
    return { ok: false, code: 'publish-stage-cleanup-failed' };
  }
  return finishPublicationTransaction(request, names, manifest, link, 'aborted');
}

function createForeignPublicForTest(name, encodedBytes) {
  if (process.env.ORCHESTRATOR_FILE_GUARD_TEST_MODE !== '1' || typeof encodedBytes !== 'string') return;
  try { fs.unlinkSync(name); } catch (error) {}
  var bytes = Buffer.from(encodedBytes, 'base64');
  var fd = fs.openSync(name, fs.constants.O_WRONLY | fs.constants.O_CREAT |
    fs.constants.O_EXCL | (fs.constants.O_NOFOLLOW || 0), 0o600);
  try { writeAll(fd, bytes); fs.fsyncSync(fd); }
  finally { fs.closeSync(fd); }
  fsyncDirectoryRequired();
}

function createPublicationTargetLink(request, names, manifest, recordLink) {
  guardCrash(request, 'guard:before-publish-target-link');
  try {
    fs.linkSync(manifest.stage, manifest.name);
    if (!fsyncDirectoryRequired()) throw new Error('sync');
  } catch (linkError) {
    return { ok: false, code: linkError && linkError.code === 'EEXIST'
      ? 'publish-target-exists' : 'publish-target-link-failed' };
  }
  guardCrash(request, 'guard:after-publish-target-link');
  if (typeof request.testReplaceAfterPublishBytes === 'string') {
    createForeignPublicForTest(manifest.name, request.testReplaceAfterPublishBytes);
    return { ok: true, replaced: true };
  }
  if (!recordLink) return { ok: true };
  return publishPublicationLink(request, names, manifest);
}

function advancePublicationTransaction(request, names, manifest) {
  var linkRead = publicationLink(names, manifest);
  if (!linkRead.ok) return linkRead;
  var link = linkRead.link;
  var receiptRead = publicationReceipt(names, manifest);
  if (!receiptRead.ok) return receiptRead;
  if (receiptRead.receipt) {
    if (!publicationReceiptStateMatches(manifest, receiptRead.receipt)) {
      return { ok: false, code: 'publish-receipt-state-mismatch' };
    }
    if (!publicationCleanupAuthorized(request, manifest)) {
      return { ok: true, outcome: receiptRead.receipt.outcome,
        stat: receiptRead.receipt.targetFinal, cleanupPending: true };
    }
    return cleanupPublicationTransaction(request, names, manifest, link, receiptRead.receipt)
      ? { ok: true, outcome: receiptRead.receipt.outcome, stat: receiptRead.receipt.targetFinal }
      : { ok: false, code: 'publish-wal-cleanup-failed' };
  }

  var targetEntry = guardPublicStat(manifest.name);
  var stageEntry = guardPublicStat(manifest.stage);
  var oldEntry = manifest.oldCapture ? guardPublicStat(manifest.oldCapture) : { exists: false, stat: null };
  if ((targetEntry.exists && !targetEntry.stat) || (stageEntry.exists && !stageEntry.stat) ||
      (oldEntry.exists && !oldEntry.stat)) return { ok: false, code: 'publish-entry-unsafe' };
  var targetRead = targetEntry.stat && targetEntry.stat.isFile() && !targetEntry.stat.isSymbolicLink()
    ? publicationRead(manifest.name) : null;
  var stageRead = stageEntry.stat && stageEntry.stat.isFile() && !stageEntry.stat.isSymbolicLink()
    ? publicationRead(manifest.stage) : null;
  var oldRead = oldEntry.stat && oldEntry.stat.isFile() && !oldEntry.stat.isSymbolicLink()
    ? publicationRead(manifest.oldCapture) : null;
  var targetDesired = publicationDesired(targetRead, manifest);
  var stageDesired = publicationDesired(stageRead, manifest);

  // A durable link record is the authority that distinguishes our published
  // inode from a byte-identical foreign generation after the private alias is
  // removed. It also permits forward recovery if the public alias vanished.
  if (link) {
    if (oldEntry.exists && (!publicationExpected(oldRead, manifest, false) || oldRead.stat.nlink !== 1n)) {
      return { ok: false, code: 'cas-old-capture-foreign' };
    }
    if (targetEntry.exists && stageEntry.exists &&
        publicationLinkedPair(targetRead, stageRead, manifest, link)) {
      if (!removePublicationStage(request, manifest, targetRead, false)) {
        return { ok: false, code: 'publish-stage-cleanup-failed' };
      }
      return advancePublicationTransaction(request, names, manifest);
    }
    if (!targetEntry.exists && stageEntry.exists &&
        publicationLinkedOwned(stageRead, manifest, link, 1)) {
      var relinked = createPublicationTargetLink(request, names, manifest, false);
      if (!relinked.ok && relinked.code !== 'publish-target-exists') return relinked;
      return advancePublicationTransaction(request, names, manifest);
    }
    if (!stageEntry.exists && publicationLinkedOwned(targetRead, manifest, link, 1)) {
      if (manifest.kind === 'file-cas' && oldEntry.exists) {
        guardCrash(request, 'guard:before-cas-old-capture-remove');
        try {
          fs.unlinkSync(manifest.oldCapture);
          if (!fsyncDirectoryRequired()) throw new Error('sync');
        } catch (oldRemoveError) { return { ok: false, code: 'cas-old-capture-remove-failed' }; }
        guardCrash(request, 'guard:after-cas-old-capture-remove');
      }
      return finishPublicationTransaction(request, names, manifest, link, 'published');
    }
    if (manifest.kind === 'file-cas') {
      return { ok: false, code: targetEntry.exists
        ? 'cas-foreign-public' : 'cas-published-generation-missing' };
    }
    if (stageEntry.exists) {
      if (!publicationLinkedOwned(stageRead, manifest, link, 1) ||
          !removePublicationStage(request, manifest, null, false)) {
        return { ok: false, code: 'publish-stage-cleanup-failed' };
      }
    }
    return finishPublicationTransaction(request, names, manifest, link, 'aborted');
  }

  if (manifest.kind === 'file-cas' && oldEntry.exists) {
    if (!publicationExpected(oldRead, manifest, false)) {
      // A raced generation linked under our private name is removable only
      // while that exact same inode is still public. Otherwise preserve it as
      // evidence, or restore it no-clobber when the public name is absent.
      if (targetRead && oldRead && sameIdentity(targetRead.stat, oldRead.stat) &&
          targetRead.stat.nlink === 2n && oldRead.stat.nlink === 2n) {
        try { fs.unlinkSync(manifest.oldCapture); if (!fsyncDirectoryRequired()) throw new Error('sync'); }
        catch (error) { return { ok: false, code: 'cas-foreign-capture-cleanup-failed' }; }
        return abortPublicationTransaction(request, names, manifest, null);
      }
      if (!targetEntry.exists && restoreForeignFileCapture(manifest.oldCapture, manifest.name)) {
        return abortPublicationTransaction(request, names, manifest, null);
      }
      return { ok: false, code: 'cas-old-capture-foreign' };
    }
    if (targetDesired && stageDesired && sameIdentity(targetRead.stat, stageRead.stat) &&
        targetRead.stat.nlink === 2n && stageRead.stat.nlink === 2n &&
        oldRead.stat.nlink === 1n) {
      var recoveredCasLink = publishPublicationLink(request, names, manifest);
      if (!recoveredCasLink.ok) return recoveredCasLink;
      return advancePublicationTransaction(request, names, manifest);
    }
    if (targetEntry.exists) {
      if (publicationExpected(targetRead, manifest, false) && sameIdentity(targetRead.stat, oldRead.stat) &&
          targetRead.stat.nlink === 2n && oldRead.stat.nlink === 2n) {
        if (process.env.ORCHESTRATOR_FILE_GUARD_TEST_MODE === '1' &&
            typeof request.testReplaceBeforeCasDetachBytes === 'string') {
          createForeignPublicForTest(manifest.name, request.testReplaceBeforeCasDetachBytes);
          return advancePublicationTransaction(request, names, manifest);
        }
        var immediateTarget = publicationRead(manifest.name);
        var immediateOld = publicationRead(manifest.oldCapture);
        if (!publicationExpected(immediateTarget, manifest, false) ||
            !publicationExpected(immediateOld, manifest, false) ||
            !sameIdentity(immediateTarget.stat, immediateOld.stat) ||
            immediateTarget.stat.nlink !== 2n || immediateOld.stat.nlink !== 2n) {
          return { ok: false, code: 'cas-pre-detach-proof-stale' };
        }
        guardCrash(request, 'guard:before-cas-old-detach');
        try { fs.unlinkSync(manifest.name); if (!fsyncDirectoryRequired()) throw new Error('sync'); }
        catch (detachError) { return { ok: false, code: 'cas-old-detach-failed' }; }
        guardCrash(request, 'guard:after-cas-old-detach');
        return advancePublicationTransaction(request, names, manifest);
      }
      return { ok: false, code: 'cas-foreign-public' };
    }
    if (!stageDesired || stageRead.stat.nlink !== 1n) {
      stageRead = ensurePublicationStage(request, manifest);
      if (!stageRead) return { ok: false, code: 'publish-stage-unsafe' };
    }
    var casPublished = createPublicationTargetLink(request, names, manifest, true);
    if (!casPublished.ok && casPublished.code !== 'publish-target-exists') return casPublished;
    return advancePublicationTransaction(request, names, manifest);
  }

  if (manifest.kind === 'file-cas') {
    if ((targetDesired && stageDesired && sameIdentity(targetRead.stat, stageRead.stat)) ||
        (!stageEntry.exists && targetDesired)) {
      return { ok: false, code: 'cas-old-capture-missing' };
    }
    if (!targetEntry.exists || !publicationExpected(targetRead, manifest, true)) {
      return abortPublicationTransaction(request, names, manifest, null);
    }
    if (!stageDesired || stageRead.stat.nlink !== 1n) {
      stageRead = ensurePublicationStage(request, manifest);
      if (!stageRead) return { ok: false, code: 'publish-stage-unsafe' };
    }
    try {
      fs.linkSync(manifest.name, manifest.oldCapture);
      if (!fsyncDirectoryRequired()) throw new Error('sync');
    } catch (oldLinkError) {
      if (oldLinkError && oldLinkError.code === 'EEXIST') {
        return advancePublicationTransaction(request, names, manifest);
      }
      return { ok: false, code: 'cas-old-capture-link-failed' };
    }
    guardCrash(request, 'guard:after-cas-old-link');
    return advancePublicationTransaction(request, names, manifest);
  }

  // Without a durable link record, byte equality alone is never publication
  // authority. The sole recoverable published state is the exact two-name
  // alias pair, from which we first publish the immutable identity record.
  if (targetEntry.exists) {
    if (targetDesired && stageDesired && sameIdentity(targetRead.stat, stageRead.stat) &&
        targetRead.stat.nlink === 2n && stageRead.stat.nlink === 2n) {
      var recoveredLink = publishPublicationLink(request, names, manifest);
      if (!recoveredLink.ok) return recoveredLink;
      return advancePublicationTransaction(request, names, manifest);
    }
    return abortPublicationTransaction(request, names, manifest, null);
  }
  if (!stageDesired || stageRead.stat.nlink !== 1n) {
    stageRead = ensurePublicationStage(request, manifest);
    if (!stageRead) return { ok: false, code: 'publish-stage-unsafe' };
  }
  if (typeof request.testReplaceBeforePublishLinkBytes === 'string') {
    createForeignPublicForTest(manifest.name, request.testReplaceBeforePublishLinkBytes);
    return advancePublicationTransaction(request, names, manifest);
  }
  var published = createPublicationTargetLink(request, names, manifest, true);
  if (!published.ok && published.code !== 'publish-target-exists') return published;
  return advancePublicationTransaction(request, names, manifest);
}

function startPublicationTransaction(request, kind) {
  if (!GUARD_OPERATION_RE.test(String(request.publicationToken || ''))) {
    return { ok: false, code: 'invalid-publication-token' };
  }
  var replacement = canonicalBase64(request.bytes, GUARD_PUBLISH_MAX_BYTES);
  if (!replacement) return { ok: false, code: 'too-large' };
  var mode = Number.isInteger(request.mode) ? request.mode : 0o600;
  if (mode < 0 || mode > 0o777 ||
      (process.platform !== 'win32' && (mode & 0o077) !== 0)) {
    return { ok: false, code: 'invalid-mode' };
  }
  var expectedProof = null, expectedHash = null;
  if (kind === 'file-publish') {
    if (!missingName(request.name)) return { ok: false, code: 'exists' };
  } else {
    if (!validExactProof(request.expectedProof, 'file') || request.expectedProof.nlink !== '1' ||
        !Number.isSafeInteger(request.maxBytes) || request.maxBytes < 0) {
      return { ok: false, code: 'invalid-cas-request' };
    }
    var expectedRead = boundedRegularFile(request.name, request.maxBytes);
    if (!expectedRead || !exactStatMatchesProof(expectedRead.stat, request.expectedProof, 'file')) {
      return { ok: false, code: 'expected-proof-mismatch' };
    }
    if (expectedRead.bytes.length > GUARD_PUBLISH_MAX_BYTES) {
      return { ok: false, code: 'too-large' };
    }
    var expectedBytes = request.expectedKind === 'bytes'
      ? canonicalBase64(request.expectedBytes, request.maxBytes) : null;
    var accepted = request.expectedKind === 'bytes'
      ? !!expectedBytes && expectedRead.bytes.equals(expectedBytes)
      : request.expectedKind === 'sha256' && /^sha256:[a-f0-9]{64}$/.test(String(request.expectedHash || '')) &&
        sha256(expectedRead.bytes) === request.expectedHash;
    if (!accepted) return { ok: false, code: 'expected-content-mismatch' };
    expectedProof = request.expectedProof;
    expectedHash = sha256(expectedRead.bytes);
  }
  var operationId = crypto.randomBytes(16).toString('hex');
  var stage = '.guard-publish-data-' + operationId;
  var oldCapture = kind === 'file-cas' ? '.guard-cas-old-' + operationId : null;
  if (!missingName(stage) || (oldCapture && !missingName(oldCapture))) {
    return { ok: false, code: 'publish-private-name-exists' };
  }
  var manifest = withGuardChecksum(publicationManifestPayload(kind, operationId,
    request.publicationToken, request.name, stage, oldCapture, replacement.toString('base64'), sha256(replacement),
    mode, expectedProof, expectedHash));
  var names = publicationNames(request.name);
  var published = publishWalRecord(request, names.manifest, manifest,
    function (value) { return validPublicationManifest(value, request.name); }, 'publish-manifest');
  if (!published.ok) return published;
  guardCrash(request, 'guard:after-publish-manifest');
  return { ok: true, names: names, manifest: manifest };
}

function publicationFailureAfterContention(request, kind, failure, names, manifest, sameRequest) {
  if (kind !== 'file-publish') return failure;
  for (var attempt = 0; attempt < 96; attempt++) {
    var publicEntry = guardPublicStat(request.name);
    if (sameRequest && manifest && (publicEntry.exists || !missingName(names.receipt) ||
        !missingName(names.receipt + '.stage'))) {
      var retried = advancePublicationTransaction(request, names, manifest);
      if (retried.ok) return retried.outcome === 'published'
        ? { ok: true, stat: retried.stat, code: 'published' }
        : { ok: false, code: 'exists' };
    }
    var stablePublic = publicEntry.exists ? publicationRead(request.name) : null;
    if (stablePublic && stablePublic.stat.nlink === 1n) return { ok: false, code: 'exists' };
    var currentNames = publicationNames(request.name);
    if (missingName(currentNames.manifest) && missingName(currentNames.manifest + '.stage') &&
        missingName(currentNames.receipt) && missingName(currentNames.receipt + '.stage')) break;
    guardBriefWait(1);
  }
  return failure;
}

function performPublicationRequest(request, kind) {
  if (!safeName(request.name)) return { ok: false, code: 'unsafe-name' };
  var requestedBytes = canonicalBase64(request.bytes, GUARD_PUBLISH_MAX_BYTES);
  if (!requestedBytes) return { ok: false, code: 'too-large' };
  if (Number.isSafeInteger(request.maxBytes) && request.maxBytes >= 0 &&
      requestedBytes.length > request.maxBytes) return { ok: false, code: 'too-large' };
  var loaded = loadPublicationTransaction(request.name,
    request.action === 'append-bounded' ? request.publicationToken : null);
  if (!loaded.ok) return loaded;
  if (loaded.terminalRetained) return { ok: false, code: 'append-prior-receipt-pending' };
  if (loaded.terminal) {
    // Never collapse two byte-identical append calls into one merely because
    // their payload and prior proof happen to match.
    var terminalSameRequest = loaded.terminal.kind === kind &&
      (request.action === 'append-bounded'
        ? loaded.terminal.creatorToken === request.publicationToken
        : loaded.terminal.replacementHash === sha256(requestedBytes)) &&
      (kind !== 'file-cas' || JSON.stringify(loaded.terminal.expectedProof) ===
        JSON.stringify(request.expectedProof));
    if (terminalSameRequest && loaded.terminal.outcome === 'published') {
      return { ok: true, stat: loaded.terminal.targetFinal, code: 'published' };
    }
  }
  if (loaded.manifest) {
    var sameRequest = loaded.manifest.kind === kind &&
      loaded.manifest.replacementBytes === requestedBytes.toString('base64') &&
      (request.action !== 'append-bounded' ||
        loaded.manifest.creatorToken === request.publicationToken) &&
      (kind !== 'file-cas' || JSON.stringify(loaded.manifest.expectedProof) ===
        JSON.stringify(request.expectedProof));
    var recovered = advancePublicationTransaction(request, loaded.names, loaded.manifest);
    if (!recovered.ok) return publicationFailureAfterContention(request, kind, recovered,
      loaded.names, loaded.manifest, sameRequest);
    if (sameRequest && recovered.outcome === 'published') {
      return { ok: true, stat: recovered.stat, code: 'published' };
    }
    if (!sameRequest && recovered.outcome) return publicationFailureAfterContention(request,
      kind, { ok: false, code: recovered.outcome === 'published' ? 'exists' : 'publish-intent-settled' });
    // The previous durable intent has settled. The current request still has
    // to win/validate against the now-current public generation.
  }
  var started = startPublicationTransaction(request, kind);
  if (!started.ok) return publicationFailureAfterContention(request, kind, started);
  var result = advancePublicationTransaction(request, started.names, started.manifest);
  var response = result.ok && result.outcome === 'published'
    ? { ok: true, stat: result.stat, code: 'published' }
    : { ok: false, code: result.code || (result.outcome === 'aborted' ? 'exists' : 'publish-failed') };
  return response.ok ? response : publicationFailureAfterContention(request, kind, response,
    started.names, started.manifest, true);
}

function appendContentionCode(code) {
  // Retry only when the deterministic journal itself proves another guarded
  // intent won publication. A bare target/proof mismatch could be an
  // unrelated replacement owner and must never be adopted as append input.
  return ['publish-intent-settled', 'wal-record-conflict'].indexOf(code) >= 0;
}

function publicationAdvanceTransient(code) {
  return ['cas-old-capture-foreign', 'cas-foreign-public',
    'publish-link-state-unsafe', 'publish-link-proof-unstable',
    'cas-pre-detach-proof-stale'].indexOf(code) >= 0;
}

function settlePriorAppendPublication(request) {
  for (var attempt = 0; attempt < 128; attempt++) {
    var loaded = loadPublicationTransaction(request.name, request.publicationToken);
    if (!loaded.ok) return { ok: false, code: loaded.code };
    if (loaded.terminal) {
      if (loaded.terminalRetained) {
        guardBriefWait(1 + (process.pid % 3));
        continue;
      }
      if (loaded.terminal.creatorToken === request.publicationToken &&
          loaded.terminal.outcome === 'published') {
        return { ok: true, recognized: true, stat: loaded.terminal.targetFinal };
      }
      return { ok: true, recognized: false };
    }
    if (!loaded.manifest) return { ok: true, recognized: false };
    var sameInvocation = loaded.manifest.creatorToken === request.publicationToken;
    var recoveryRequest = sameInvocation ? request :
      { action: 'reconcile-guard-transactions' };
    var advanced = advancePublicationTransaction(recoveryRequest, loaded.names, loaded.manifest);
    if (!advanced.ok) {
      if (publicationAdvanceTransient(advanced.code)) {
        guardBriefWait(1 + (process.pid % 3));
        continue;
      }
      return { ok: false, code: advanced.code };
    }
    if (sameInvocation && advanced.outcome === 'published') {
      return { ok: true, recognized: true, stat: advanced.stat };
    }
    return { ok: true, recognized: false };
  }
  return { ok: false, code: 'append-publication-settle-timeout' };
}

// A bounded append is a replacement transaction, not an O_APPEND write.
// The current exact generation and content hash become a CAS precondition;
// the target-keyed publication WAL serializes helpers across processes. This
// makes the size check and mutation one crash-recoverable operation, so two
// callers can never both consume the same remaining-byte budget.
function performBoundedAppend(request) {
  var appendBytes = canonicalBase64(request.bytes, request.maxAppendBytes);
  if (!appendBytes || appendBytes.length > request.maxBytes ||
      appendBytes.length > GUARD_PUBLISH_MAX_BYTES) {
    return { ok: false, code: 'too-large' };
  }
  var prior = settlePriorAppendPublication(request);
  if (!prior.ok) return prior;
  if (prior.recognized) return { ok: true, code: 'published', stat: prior.stat };
  var callerExpected = request.expectedProof;
  var callerMissing = request.expectMissing === true;
  for (var attempt = 0; attempt < 128; attempt++) {
    var entry = guardPublicStat(request.name);
    if (entry.exists && !entry.stat) return { ok: false, code: 'existing-unsafe' };
    var current = entry.stat ? publicationRead(request.name) : null;
    if (entry.stat && (!current || current.stat.nlink !== 1n)) {
      return { ok: false, code: 'existing-unsafe' };
    }
    if (callerMissing && entry.exists) return { ok: false, code: 'expected-missing' };
    if (callerExpected && (!current ||
        !exactStatMatchesProof(current.stat, callerExpected, 'file'))) {
      return { ok: false, code: 'expected-changed' };
    }
    if (current && current.bytes.length + appendBytes.length > request.maxBytes) {
      return { ok: false, code: 'existing-unsafe' };
    }

    if (attempt === 0) {
      replacePublicRegularForTest(request.name, request.testReplaceAfterOpenBytes,
        'append-test-displaced');
      if (process.env.ORCHESTRATOR_FILE_GUARD_TEST_MODE === '1' &&
          Number.isSafeInteger(request.testPauseAfterAppendReadMs) &&
          request.testPauseAfterAppendReadMs > 0 && request.testPauseAfterAppendReadMs <= 2000) {
        guardBriefWait(request.testPauseAfterAppendReadMs);
      }
    }

    var transaction = Object.assign({}, request, {
      bytes: current ? Buffer.concat([current.bytes, appendBytes]).toString('base64') :
        appendBytes.toString('base64'),
      expectedProof: current ? statShape(current.stat) : undefined,
      expectedKind: current ? 'sha256' : undefined,
      expectedHash: current ? current.proof.hash : undefined,
      mode: request.mode || 0o600
    });
    var result = performPublicationRequest(transaction, current ? 'file-cas' : 'file-publish');
    if (result.ok) return result;
    var recovered = settlePriorAppendPublication(request);
    if (recovered.ok && recovered.recognized) {
      return { ok: true, code: 'published', stat: recovered.stat };
    }
    if (!recovered.ok && publicationAdvanceTransient(result.code)) return recovered;
    if (callerMissing && result.code === 'exists') {
      return { ok: false, code: 'expected-missing' };
    }
    if (callerExpected && ['exists', 'expected-proof-mismatch',
      'expected-content-mismatch', 'cas-foreign-public'].indexOf(result.code) >= 0) {
      return { ok: false, code: 'expected-changed' };
    }
    if (!appendContentionCode(result.code)) return result;
    guardBriefWait(1 + (process.pid % 3));
  }
  return { ok: false, code: 'append-contention-timeout' };
}

function acknowledgeBoundedAppend(request) {
  if (!safeName(request.name) || !GUARD_OPERATION_RE.test(String(request.publicationToken || '')) ||
      !validExactProof(request.targetExpected, 'file') || request.targetExpected.nlink !== '1') {
    return { ok: false, code: 'invalid-append-ack' };
  }
  var loaded = loadPublicationTransaction(request.name);
  if (!loaded.ok) return loaded;
  var finalProof = null;
  if (loaded.terminal) {
    if (loaded.terminal.version !== 2 ||
        loaded.terminal.creatorToken !== request.publicationToken ||
        loaded.terminal.outcome !== 'published') {
      return { ok: false, code: 'append-ack-token-mismatch' };
    }
    finalProof = loaded.terminal.targetFinal;
  } else if (loaded.manifest) {
    if (loaded.manifest.creatorToken !== request.publicationToken) {
      return { ok: false, code: 'append-ack-token-mismatch' };
    }
    var advanced = advancePublicationTransaction(request, loaded.names, loaded.manifest);
    if (!advanced.ok || advanced.outcome !== 'published') {
      return { ok: false, code: advanced.code || 'append-ack-not-published' };
    }
    finalProof = advanced.stat;
  } else finalProof = request.targetExpected;
  var current = publicationRead(request.name);
  return current && exactStatMatchesProof(current.stat, request.targetExpected, 'file') &&
    JSON.stringify(finalProof) === JSON.stringify(request.targetExpected)
    ? { ok: true }
    : { ok: false, code: 'append-ack-target-mismatch' };
}

function directoryCursorIdentity(stat) {
  return { dev: String(stat.dev), ino: String(stat.ino), mode: String(stat.mode), type: 'directory' };
}

function directoryCursorPayload(identity, after) {
  return { version: 1, directory: identity, after: after };
}

function validDirectoryCursorRecord(value) {
  if (!exactObjectKeys(value, ['version', 'directory', 'after', 'checksum']) || value.version !== 1 ||
      !exactObjectKeys(value.directory, ['dev', 'ino', 'mode', 'type']) ||
      value.directory.type !== 'directory' || typeof value.after !== 'string' || !value.after ||
      value.after.indexOf('/') >= 0 || value.after.indexOf('\\') >= 0 ||
      !['dev', 'ino', 'mode'].every(function (field) {
        return typeof value.directory[field] === 'string' && DECIMAL_RE.test(value.directory[field]);
      })) return false;
  return value.checksum === withGuardChecksum(directoryCursorPayload(
    value.directory, value.after)).checksum;
}

function encodeDirectoryCursor(stat, after) {
  var record = withGuardChecksum(directoryCursorPayload(directoryCursorIdentity(stat), after));
  return Buffer.from(JSON.stringify(record), 'utf8').toString('base64url');
}

function decodeDirectoryCursor(value) {
  if (typeof value !== 'string' || !value || value.length > GUARD_DIRECTORY_CURSOR_MAX ||
      !/^[A-Za-z0-9_-]+$/.test(value)) return null;
  try {
    var bytes = Buffer.from(value, 'base64url');
    if (bytes.toString('base64url') !== value || bytes.length > GUARD_DIRECTORY_CURSOR_MAX) return null;
    var record = JSON.parse(bytes.toString('utf8'));
    return validDirectoryCursorRecord(record) ? record : null;
  } catch (error) { return null; }
}

function directoryCursorMatches(stat, cursor) {
  return !!stat && !!cursor && stat.isDirectory() && !stat.isSymbolicLink() &&
    String(stat.dev) === cursor.directory.dev && String(stat.ino) === cursor.directory.ino &&
    String(stat.mode) === cursor.directory.mode;
}

function compareDirectoryNames(left, right) {
  return Buffer.compare(Buffer.from(left, 'utf8'), Buffer.from(right, 'utf8'));
}

function insertDirectoryPageName(page, name, pageSize) {
  var low = 0, high = page.length;
  while (low < high) {
    var middle = (low + high) >>> 1;
    if (compareDirectoryNames(page[middle], name) < 0) low = middle + 1;
    else high = middle;
  }
  page.splice(low, 0, name);
  if (page.length > pageSize) page.pop();
}

function boundedCurrentDirectoryPage(request) {
  if (!Number.isSafeInteger(request.pageSize) || request.pageSize < 1 ||
      request.pageSize > GUARD_DIRECTORY_PAGE_MAX ||
      !Number.isSafeInteger(request.maxScanEntries) || request.maxScanEntries < 0 ||
      request.maxScanEntries > GUARD_RECONCILE_MAX_ENTRIES ||
      (request.cursor !== null && typeof request.cursor !== 'string')) {
    return { ok: false, code: 'invalid-request', names: [], nextCursor: null,
      done: false, scanned: 0 };
  }
  var before = fs.lstatSync('.', { bigint: true });
  if (!before.isDirectory() || before.isSymbolicLink()) {
    return { ok: false, code: 'directory-unsafe', names: [], nextCursor: null,
      done: false, scanned: 0 };
  }
  var cursor = request.cursor === null ? null : decodeDirectoryCursor(request.cursor);
  if (request.cursor !== null && !cursor) {
    return { ok: false, code: 'cursor-invalid', names: [], nextCursor: null,
      done: false, scanned: 0 };
  }
  if (cursor && !directoryCursorMatches(before, cursor)) {
    return { ok: false, code: 'cursor-directory-mismatch', names: [], nextCursor: null,
      done: false, scanned: 0 };
  }
  var afterName = cursor ? cursor.after : null;
  var page = [];
  var candidateCount = 0;
  var scanned = 0;
  var dir;
  try {
    dir = fs.opendirSync('.');
    while (true) {
      var entry = dir.readSync();
      if (!entry) break;
      scanned++;
      if (scanned > request.maxScanEntries) {
        return { ok: false, code: 'scan-entry-cap-exceeded', names: [],
          nextCursor: null, done: false, scanned: scanned };
      }
      if (afterName !== null && compareDirectoryNames(entry.name, afterName) <= 0) continue;
      candidateCount++;
      insertDirectoryPageName(page, entry.name, request.pageSize);
    }
  } catch (error) {
    return { ok: false, code: 'scan-failed', names: [], nextCursor: null,
      done: false, scanned: scanned };
  } finally { if (dir) try { dir.closeSync(); } catch (closeError) {} }
  if (process.env.ORCHESTRATOR_FILE_GUARD_TEST_MODE === '1' &&
      safeName(request.testMutateDirectoryPageName)) {
    var mutationFd;
    try {
      mutationFd = fs.openSync(request.testMutateDirectoryPageName,
        fs.constants.O_WRONLY | fs.constants.O_CREAT | fs.constants.O_EXCL |
        (fs.constants.O_NOFOLLOW || 0), 0o600);
      fs.fsyncSync(mutationFd);
      fs.closeSync(mutationFd); mutationFd = undefined;
      fsyncDirectoryRequired();
    } catch (mutationError) {}
    finally { if (mutationFd !== undefined) try { fs.closeSync(mutationFd); } catch (closeMutationError) {} }
  }
  var after = fs.lstatSync('.', { bigint: true });
  if (!sameStableDirectory(before, after)) {
    return { ok: false, code: 'directory-changed', names: [], nextCursor: null,
      done: false, scanned: scanned };
  }
  var done = candidateCount <= request.pageSize;
  return { ok: true, names: page,
    nextCursor: done ? null : encodeDirectoryCursor(after, page[page.length - 1]),
    done: done, scanned: scanned };
}

function boundedCurrentDirectoryNames(maxEntries) {
  var dir;
  var names = [];
  try {
    dir = fs.opendirSync('.');
    while (true) {
      var entry = dir.readSync();
      if (!entry) return { ok: true, names: names.sort() };
      names.push(entry.name);
      if (names.length > maxEntries) {
        return { ok: false, code: 'scan-entry-cap-exceeded', scanned: names.length };
      }
    }
  } catch (error) { return { ok: false, code: 'scan-failed', scanned: names.length }; }
  finally { if (dir) try { dir.closeSync(); } catch (closeError) {} }
}

function peekWalManifest(finalName) {
  var finalRead = walJson(finalName);
  var stageRead = walJson(finalName + '.stage');
  if (finalRead && stageRead && !finalRead.bytes.equals(stageRead.bytes)) {
    return { ok: false, code: 'wal-generations-conflict' };
  }
  var selected = finalRead || stageRead;
  if (!selected) return { ok: false, code: 'wal-manifest-unreadable' };
  return { ok: true, value: selected.value };
}

function incrementGuardSummary(summary, code) {
  summary.pending++;
  summary.codes[code] = (summary.codes[code] || 0) + 1;
}

function reconcileGuardTransactions(request) {
  if (!Number.isSafeInteger(request.maxEntries) || request.maxEntries < 0 ||
      request.maxEntries > GUARD_RECONCILE_MAX_ENTRIES ||
      !Number.isSafeInteger(request.maxTransactions) || request.maxTransactions < 0 ||
      request.maxTransactions > GUARD_RECONCILE_MAX_TRANSACTIONS) {
    return { ok: false, code: 'invalid-request', scanned: 0, transactions: 0,
      reconciled: 0, pending: 0, codes: {} };
  }
  var scan = boundedCurrentDirectoryNames(request.maxEntries);
  if (!scan.ok) return { ok: false, code: scan.code, scanned: scan.scanned,
    transactions: 0, reconciled: 0, pending: 0, codes: {} };

  var candidateMap = Object.create(null);
  var captures = [];
  var unknown = [];
  scan.names.forEach(function (name) {
    if (name.indexOf('.guard-') !== 0) return;
    var guardMatch = /^(\.guard-txn-[a-f0-9]{64})(?:\.json(?:\.stage)?|\.decision\.json(?:\.stage)?|\.receipt\.json(?:\.stage)?)$/.exec(name);
    if (guardMatch) {
      candidateMap[guardMatch[1] + '.json'] = 'guard';
      return;
    }
    var transferMatch = /^(\.guard-transfer-[a-f0-9]{64})(?:\.json(?:\.stage)?|\.link\.json(?:\.stage)?|\.receipt\.json(?:\.stage)?)$/.exec(name);
    if (transferMatch) {
      candidateMap[transferMatch[1] + '.json'] = 'transfer';
      return;
    }
    var publicationMatch = /^(\.guard-publish-[a-f0-9]{64})(?:\.json(?:\.stage)?|\.link\.json(?:\.stage)?|\.receipt\.json(?:\.stage)?)$/.exec(name);
    if (publicationMatch) {
      candidateMap[publicationMatch[1] + '.json'] = 'publication';
      return;
    }
    var captureMatch = /^\.guard-(?:transfer-)?capture-([a-f0-9]{32})$/.exec(name);
    if (captureMatch) {
      captures.push({ name: name, operationId: captureMatch[1] });
      return;
    }
    var publicationPrivateMatch = /^\.guard-(?:publish-data|cas-old)-([a-f0-9]{32})$/.exec(name);
    if (publicationPrivateMatch) {
      captures.push({ name: name, operationId: publicationPrivateMatch[1] });
      return;
    }
    unknown.push(name);
  });

  var candidateNames = Object.keys(candidateMap).sort();
  // WAL candidates and unknown private entries alone are a proven lower
  // bound. Private data/capture entries are grouped by operationId only after
  // the immutable manifest is validated; reading them cannot mutate state.
  var minimumTransactionCount = candidateNames.length + unknown.length;
  if (minimumTransactionCount > request.maxTransactions) {
    return { ok: false, code: 'scan-transaction-cap-exceeded', scanned: scan.names.length,
      transactions: minimumTransactionCount, reconciled: 0,
      pending: minimumTransactionCount, codes: {} };
  }
  var candidates = candidateNames.map(function (finalName) {
    var kind = candidateMap[finalName];
    var peeked = peekWalManifest(finalName);
    var item = { finalName: finalName, kind: kind, peek: peeked, manifest: null, terminal: null };
    if (!peeked.ok && kind === 'publication') {
      var receiptName = finalName.slice(0, -'.json'.length) + '.receipt.json';
      var receiptPeek = peekWalManifest(receiptName);
      if (receiptPeek.ok && validStandalonePublicationReceipt(receiptPeek.value) &&
          publicationNames(receiptPeek.value.name).manifest === finalName) {
        item.peek = receiptPeek;
        item.terminal = receiptPeek.value;
      }
      return item;
    }
    if (!peeked.ok) return item;
    if (kind === 'guard') {
      if (safeName(peeked.value && peeked.value.name) &&
          validGuardManifest(peeked.value, peeked.value.name) &&
          guardNames(peeked.value.name).manifest === finalName) item.manifest = peeked.value;
    } else if (kind === 'transfer') {
      if (safeName(peeked.value && peeked.value.source) &&
          validTransferManifest(peeked.value, peeked.value.source) &&
          transferNames(peeked.value.source).manifest === finalName) item.manifest = peeked.value;
    } else if (kind === 'publication' && safeName(peeked.value && peeked.value.name) &&
        validPublicationManifest(peeked.value, peeked.value.name) &&
        publicationNames(peeked.value.name).manifest === finalName) item.manifest = peeked.value;
    return item;
  });
  var knownOperations = Object.create(null);
  candidates.forEach(function (item) {
    var record = item.manifest || item.terminal;
    if (record) knownOperations[record.operationId] = true;
  });
  var orphanCaptureOperations = Object.create(null);
  captures.forEach(function (capture) {
    if (!knownOperations[capture.operationId]) orphanCaptureOperations[capture.operationId] = true;
  });
  var orphanCaptureCount = Object.keys(orphanCaptureOperations).length;
  var transactionCount = candidates.length + orphanCaptureCount + unknown.length;
  if (transactionCount > request.maxTransactions) {
    return { ok: false, code: 'scan-transaction-cap-exceeded', scanned: scan.names.length,
      transactions: transactionCount, reconciled: 0, pending: transactionCount, codes: {} };
  }

  var summary = { ok: true, scanned: scan.names.length, transactions: transactionCount,
    reconciled: 0, pending: 0, codes: Object.create(null) };
  unknown.forEach(function () { incrementGuardSummary(summary, 'private-entry-unrecognized'); });
  Object.keys(orphanCaptureOperations).forEach(function () {
    incrementGuardSummary(summary, 'guard-capture-orphan');
  });
  candidates.forEach(function (item) {
    if (!item.peek.ok) {
      incrementGuardSummary(summary, item.peek.code);
      return;
    }
    if (!item.manifest && !item.terminal) {
      incrementGuardSummary(summary, 'wal-manifest-invalid');
      return;
    }
    try {
      var loaded, advanced;
      if (item.kind === 'guard') {
        loaded = loadGuardTransaction(item.manifest.name);
        if (!loaded.ok || !loaded.manifest) {
          incrementGuardSummary(summary, loaded.code || 'wal-manifest-missing');
          return;
        }
        advanced = advanceGuardTransaction(request, loaded.names, loaded.manifest, 'restore');
      } else if (item.kind === 'transfer') {
        loaded = loadTransferTransaction(item.manifest.source);
        if (!loaded.ok || !loaded.manifest) {
          incrementGuardSummary(summary, loaded.code || 'wal-manifest-missing');
          return;
        }
        advanced = advanceTransferTransaction(request, loaded.names, loaded.manifest);
      } else {
        loaded = loadPublicationTransaction((item.manifest || item.terminal).name);
        if (!loaded.ok || (!loaded.manifest && !loaded.terminal)) {
          incrementGuardSummary(summary, loaded.code || 'wal-manifest-missing');
          return;
        }
        advanced = loaded.manifest
          ? advancePublicationTransaction(request, loaded.names, loaded.manifest)
          : { ok: true, outcome: loaded.terminal.outcome };
      }
      if (!advanced.ok) {
        incrementGuardSummary(summary, advanced.code || 'guard-reconcile-failed');
        return;
      }
      summary.reconciled++;
    } catch (error) { incrementGuardSummary(summary, 'guard-reconcile-failed'); }
  });
  summary.ok = summary.pending === 0;
  if (!summary.ok) summary.code = 'guard-reconcile-incomplete';
  return summary;
}

function atomicReplace(request) {
  var name = request.name;
  if (!safeName(name)) return { ok: false, code: 'unsafe-name' };
  var initial = Buffer.from(request.bytes || '', 'base64');
  if (request.preserveExisting) {
    try {
      var existing = fs.lstatSync(name, { bigint: true });
      if (!existing.isFile() || existing.isSymbolicLink()) return { ok: false, code: 'existing-unsafe' };
      var preserved = boundedRegularFile(name, request.maxExistingBytes);
      if (!preserved) return { ok: false, code: 'existing-unstable' };
      initial = preserved.bytes;
    } catch (error) {
      if (!error || error.code !== 'ENOENT') return { ok: false, code: 'existing-read-failed' };
      initial = Buffer.alloc(0);
    }
  }
  if (Number.isSafeInteger(request.maxBytes) && request.maxBytes >= 0 && initial.length > request.maxBytes) {
    return { ok: false, code: 'too-large' };
  }
  var tmp = '.' + name + '-' + process.pid + '-' + crypto.randomBytes(12).toString('hex') + '.tmp';
  var fd;
  var published = false;
  var publishedCandidate = null;
  try {
    fd = fs.openSync(tmp, fs.constants.O_WRONLY | fs.constants.O_CREAT | fs.constants.O_EXCL |
      (fs.constants.O_NOFOLLOW || 0), request.mode || 0o600);
    if (request.mode) {
      try { fs.fchmodSync(fd, request.mode); }
      catch (modeError) { if (process.platform !== 'win32') throw modeError; }
    }
    writeAll(fd, initial);
    fs.fsyncSync(fd);
    var opened = fs.fstatSync(fd, { bigint: true });
    if (!opened.isFile()) throw new Error('guarded temp is not regular');
    fs.closeSync(fd); fd = undefined;
    fs.renameSync(tmp, name);
    published = true;
    replacePublicRegularForTest(name, request.testReplaceAfterPublishBytes, 'atomic-test-displaced');
    var live = fs.lstatSync(name, { bigint: true });
    if (!sameIdentity(live, opened) || live.nlink !== 1n || live.size !== opened.size ||
        live.mtimeNs !== opened.mtimeNs) {
      throw new Error('guarded publication identity changed');
    }
    publishedCandidate = live;
    if (process.env.ORCHESTRATOR_FILE_GUARD_TEST_MODE === '1' &&
        request.testForceAtomicUncertainAfterVerification === true) {
      throw new Error('test uncertain commit after exact verification');
    }
    if (!fsyncDirectoryRequired()) throw new Error('guarded publication directory sync failed');
    return { ok: true, code: 'published', stat: statShape(live),
      contentHash: sha256(initial), contentSize: initial.length };
  } catch (error2) {
    return published
      ? { ok: false, code: 'published-unverified', uncertain: true,
        candidateStat: statShape(publishedCandidate || opened),
        contentHash: sha256(initial), contentSize: initial.length }
      : { ok: false, code: 'write-failed', uncertain: false };
  } finally {
    if (fd !== undefined) try { fs.closeSync(fd); } catch (closeError) {}
    if (!published) try { fs.unlinkSync(tmp); } catch (cleanupError) {}
  }
}

function dispatchCurrent(request) {
  var action = request && request.action;
  var anchored = fs.lstatSync('.', { bigint: true });
  if (!sameDirectoryExpected(anchored, request && request.expected)) return { ok: false, code: 'directory-mismatch' };

  if (action === 'directory-proof') {
    return { ok: true, exists: true, stat: statShape(anchored) };
  }

  if (action === 'reconcile-guard-transactions') {
    return reconcileGuardTransactions(request);
  }

  if (action === 'writer-directory-proof') {
    var writerDir = fs.lstatSync('.', { bigint: true });
    return writerDir.isDirectory() && !writerDir.isSymbolicLink()
      ? { ok: true, proof: bigintShape(writerDir) } : { ok: false, code: 'directory-unsafe' };
  }

  if (action === 'writer-read') {
    var writerRead = writerRegularFile(request.name, request.maxBytes);
    return writerRead ? { ok: true, bytes: writerRead.bytes.toString('base64'), proof: writerRead.proof } : { ok: false, code: 'file-unsafe' };
  }

  if (action === 'writer-stage') {
    if (!safeName(request.name) || !Number.isSafeInteger(request.maxBytes) || request.maxBytes < 0) return { ok: false, code: 'invalid-request' };
    var writerBytes = Buffer.from(request.bytes || '', 'base64');
    if (writerBytes.length > request.maxBytes) return { ok: false, code: 'too-large' };
    var writerFd;
    try {
      writerFd = fs.openSync(request.name, fs.constants.O_WRONLY | fs.constants.O_CREAT | fs.constants.O_EXCL |
        (fs.constants.O_NOFOLLOW || 0), request.mode || 0o600);
      writeAll(writerFd, writerBytes);
      fs.fsyncSync(writerFd);
      var writerOpened = fs.fstatSync(writerFd, { bigint: true });
      fs.closeSync(writerFd); writerFd = undefined;
      var writerLive = fs.lstatSync(request.name, { bigint: true });
      if (!exactBigintShape(writerLive, bigintShape(writerOpened), 'file') || writerLive.nlink !== 1n ||
          !fsyncDirectoryRequired()) return { ok: false, code: 'stage-unverified' };
      var writerStaged = writerRegularFile(request.name, request.maxBytes);
      return writerStaged && writerStaged.bytes.equals(writerBytes)
        ? { ok: true, proof: writerStaged.proof } : { ok: false, code: 'stage-unverified' };
    } catch (writerStageError) { return { ok: false, code: writerStageError && writerStageError.code === 'EEXIST' ? 'exists' : 'stage-failed' }; }
    finally { if (writerFd !== undefined) try { fs.closeSync(writerFd); } catch (writerCloseError) {} }
  }

  if (action === 'writer-link') {
    if (!safeName(request.source) || !safeName(request.target) || typeof request.removeSource !== 'boolean' ||
        (request.removeSource && !safeName(request.capture))) return { ok: false, code: 'invalid-request' };
    var writerSource = writerRegularFile(request.source, request.maxBytes);
    if (!writerSource || !writerProofMatches(writerSource.proof, request.sourceProof)) return { ok: false, code: 'source-mismatch' };
    try {
      fs.linkSync(request.source, request.target);
      var writerLinkedSource = writerRegularFile(request.source, request.maxBytes);
      var writerLinkedTarget = writerRegularFile(request.target, request.maxBytes);
      var writerLinkedCount = (BigInt(writerSource.proof.nlink) + 1n).toString();
      if (!writerLinkedSource || !writerLinkedTarget || writerLinkedSource.proof.ino !== writerSource.proof.ino ||
          writerLinkedSource.proof.dev !== writerSource.proof.dev || writerLinkedTarget.proof.ino !== writerSource.proof.ino ||
          writerLinkedTarget.proof.dev !== writerSource.proof.dev || writerLinkedSource.proof.nlink !== writerLinkedCount ||
          writerLinkedTarget.proof.nlink !== writerLinkedCount || !writerLinkedTarget.bytes.equals(writerSource.bytes) ||
          !fsyncDirectoryRequired()) return { ok: false, code: 'link-unverified' };
      if (request.removeSource) {
        try { fs.lstatSync(request.capture); return { ok: false, code: 'capture-exists' }; }
        catch (captureMissing) { if (!captureMissing || captureMissing.code !== 'ENOENT') return { ok: false, code: 'capture-unsafe' }; }
        if (process.env.ORCHESTRATOR_FILE_GUARD_TEST_MODE === '1' && typeof request.testReplaceBeforeCaptureBytes === 'string') {
          fs.unlinkSync(request.source);
          var beforeCaptureBytes = Buffer.from(request.testReplaceBeforeCaptureBytes, 'base64');
          var beforeCaptureFd = fs.openSync(request.source, fs.constants.O_WRONLY | fs.constants.O_CREAT | fs.constants.O_EXCL |
            (fs.constants.O_NOFOLLOW || 0), 0o600);
          try { writeAll(beforeCaptureFd, beforeCaptureBytes); fs.fsyncSync(beforeCaptureFd); }
          finally { fs.closeSync(beforeCaptureFd); }
          fsyncDirectoryRequired();
        }
        fs.renameSync(request.source, request.capture);
        if (!fsyncDirectoryRequired()) return { ok: false, code: 'transfer-unverified' };
        if (process.env.ORCHESTRATOR_FILE_GUARD_TEST_MODE === '1' && typeof request.testForeignBytes === 'string') {
          var injected = Buffer.from(request.testForeignBytes, 'base64');
          var injectedFd = fs.openSync(request.source, fs.constants.O_WRONLY | fs.constants.O_CREAT | fs.constants.O_EXCL |
            (fs.constants.O_NOFOLLOW || 0), 0o600);
          try { writeAll(injectedFd, injected); fs.fsyncSync(injectedFd); }
          finally { fs.closeSync(injectedFd); }
          fsyncDirectoryRequired();
        }
        var captured = writerRegularFile(request.capture, request.maxBytes);
        var sourceOccupied = false;
        try { fs.lstatSync(request.source); sourceOccupied = true; }
        catch (sourceMissing) { if (!sourceMissing || sourceMissing.code !== 'ENOENT') sourceOccupied = true; }
        if (!captured || captured.proof.dev !== writerSource.proof.dev || captured.proof.ino !== writerSource.proof.ino ||
            captured.proof.nlink !== writerLinkedCount || sourceOccupied) {
          // The public source may now be a foreign generation. Never unlink it.
          // Preserve the captured inode and durable target for exact WAL recovery.
          if (captured && (captured.proof.dev !== writerSource.proof.dev || captured.proof.ino !== writerSource.proof.ino) && !sourceOccupied) {
            try { fs.linkSync(request.capture, request.source); fsyncDirectoryRequired(); }
            catch (restoreForeignError) {}
          }
          return { ok: false, code: 'source-replaced', proof: writerLinkedTarget.proof,
            captureProof: captured && captured.proof || null };
        }
        fs.unlinkSync(request.capture);
        if (!fsyncDirectoryRequired()) return { ok: false, code: 'transfer-unverified' };
      }
      var writerTarget = writerRegularFile(request.target, request.maxBytes);
      var writerFinalCount = request.removeSource ? writerSource.proof.nlink : writerLinkedCount;
      if (!writerTarget || writerTarget.proof.dev !== writerSource.proof.dev || writerTarget.proof.ino !== writerSource.proof.ino ||
          writerTarget.proof.nlink !== writerFinalCount || !writerTarget.bytes.equals(writerSource.bytes)) {
        return { ok: false, code: 'link-unverified' };
      }
      return { ok: true, proof: writerTarget.proof };
    } catch (writerLinkError) { return { ok: false, code: writerLinkError && writerLinkError.code === 'EEXIST' ? 'exists' : 'link-failed' }; }
  }

  if (action === 'writer-delete') {
    if (!safeName(request.name) || !safeName(request.quarantine) || !safeName(request.capture)) return { ok: false, code: 'invalid-request' };
    var writerDelete = writerRegularFile(request.name, request.maxBytes);
    if (!writerDelete || !writerProofMatches(writerDelete.proof, request.proof)) return { ok: false, code: 'source-mismatch' };
    try {
      fs.linkSync(request.name, request.quarantine);
      var writerHeld = writerRegularFile(request.quarantine, request.maxBytes);
      var writerDeleteLinkedCount = (BigInt(writerDelete.proof.nlink) + 1n).toString();
      if (!writerHeld || writerHeld.proof.dev !== writerDelete.proof.dev || writerHeld.proof.ino !== writerDelete.proof.ino ||
          writerHeld.proof.nlink !== writerDeleteLinkedCount || !fsyncDirectoryRequired()) return { ok: false, code: 'delete-unverified' };
      try { fs.lstatSync(request.capture); return { ok: false, code: 'capture-exists' }; }
      catch (deleteCaptureMissing) { if (!deleteCaptureMissing || deleteCaptureMissing.code !== 'ENOENT') return { ok: false, code: 'capture-unsafe' }; }
      if (process.env.ORCHESTRATOR_FILE_GUARD_TEST_MODE === '1' && typeof request.testReplaceBeforeCaptureBytes === 'string') {
        fs.unlinkSync(request.name);
        var deleteBeforeCaptureBytes = Buffer.from(request.testReplaceBeforeCaptureBytes, 'base64');
        var deleteBeforeCaptureFd = fs.openSync(request.name, fs.constants.O_WRONLY | fs.constants.O_CREAT | fs.constants.O_EXCL |
          (fs.constants.O_NOFOLLOW || 0), 0o600);
        try { writeAll(deleteBeforeCaptureFd, deleteBeforeCaptureBytes); fs.fsyncSync(deleteBeforeCaptureFd); }
        finally { fs.closeSync(deleteBeforeCaptureFd); }
        fsyncDirectoryRequired();
      }
      fs.renameSync(request.name, request.capture);
      if (!fsyncDirectoryRequired()) return { ok: false, code: 'delete-unverified' };
      if (process.env.ORCHESTRATOR_FILE_GUARD_TEST_MODE === '1' && typeof request.testForeignBytes === 'string') {
        var deleteInjected = Buffer.from(request.testForeignBytes, 'base64');
        var deleteInjectedFd = fs.openSync(request.name, fs.constants.O_WRONLY | fs.constants.O_CREAT | fs.constants.O_EXCL |
          (fs.constants.O_NOFOLLOW || 0), 0o600);
        try { writeAll(deleteInjectedFd, deleteInjected); fs.fsyncSync(deleteInjectedFd); }
        finally { fs.closeSync(deleteInjectedFd); }
        fsyncDirectoryRequired();
      }
      var deleteCaptured = writerRegularFile(request.capture, request.maxBytes);
      var deleteSourceOccupied = false;
      try { fs.lstatSync(request.name); deleteSourceOccupied = true; }
      catch (deleteSourceMissing) { if (!deleteSourceMissing || deleteSourceMissing.code !== 'ENOENT') deleteSourceOccupied = true; }
      if (!deleteCaptured || deleteCaptured.proof.dev !== writerDelete.proof.dev || deleteCaptured.proof.ino !== writerDelete.proof.ino ||
          deleteCaptured.proof.nlink !== writerDeleteLinkedCount || deleteSourceOccupied) {
        if (deleteCaptured && (deleteCaptured.proof.dev !== writerDelete.proof.dev || deleteCaptured.proof.ino !== writerDelete.proof.ino) && !deleteSourceOccupied) {
          try { fs.linkSync(request.capture, request.name); fsyncDirectoryRequired(); }
          catch (restoreDeleteForeignError) {}
        }
        return { ok: false, code: 'source-replaced', quarantineProof: writerHeld.proof,
          captureProof: deleteCaptured && deleteCaptured.proof || null };
      }
      fs.unlinkSync(request.capture);
      if (!fsyncDirectoryRequired()) return { ok: false, code: 'delete-unverified' };
      writerHeld = writerRegularFile(request.quarantine, request.maxBytes);
      if (!writerHeld || writerHeld.proof.dev !== writerDelete.proof.dev || writerHeld.proof.ino !== writerDelete.proof.ino ||
          writerHeld.proof.nlink !== writerDelete.proof.nlink) return { ok: false, code: 'delete-unverified' };
      fs.unlinkSync(request.quarantine);
      return { ok: fsyncDirectoryRequired() };
    } catch (writerDeleteError) { return { ok: false, code: writerDeleteError && writerDeleteError.code === 'EEXIST' ? 'exists' : 'delete-failed' }; }
  }

  if (action === 'publish-no-clobber') {
    return performPublicationRequest(request, 'file-publish');
  }

  if (action === 'compare-and-swap') {
    return performPublicationRequest(request, 'file-cas');
  }

  if (action === 'atomic-replace') return atomicReplace(request);

  if (action === 'append-ack') return acknowledgeBoundedAppend(request);

  if (action === 'append-bounded') {
    if (!safeName(request.name) || !Number.isSafeInteger(request.maxBytes) || request.maxBytes < 0 ||
        !Number.isSafeInteger(request.maxAppendBytes) || request.maxAppendBytes < 0 ||
        !GUARD_OPERATION_RE.test(String(request.publicationToken || '')) ||
        (request.expectedProof !== undefined && !validExactProof(request.expectedProof, 'file')) ||
        (request.expectMissing === true && request.expectedProof !== undefined)) {
      return { ok: false, code: 'invalid-append-request' };
    }
    return performBoundedAppend(request);
  }

  if (action === 'bounded-read') {
    var bounded = boundedRegularFile(request.name, request.maxBytes, request.testReplaceAfterOpenBytes);
    return bounded ? { ok: true, bytes: bounded.bytes.toString('base64'), stat: statShape(bounded.stat) } : { ok: false };
  }

  if (action === 'stat-file') {
    var fileStat = stableRegularStat(request.name, request.testReplaceAfterOpenBytes);
    return fileStat ? { ok: true, stat: statShape(fileStat) } : { ok: false };
  }

  if (action === 'inspect-entry') {
    if (!safeName(request.name)) return { ok: false };
    try {
      var inspectBefore = fs.lstatSync(request.name, { bigint: true });
      replacePublicRegularForTest(request.name, request.testReplaceAfterOpenBytes, 'inspect-test-displaced');
      var inspectAfter = fs.lstatSync(request.name, { bigint: true });
      return sameStableEntry(inspectBefore, inspectAfter)
        ? { ok: true, status: 'present', stat: statShape(inspectAfter) }
        : { ok: false, code: 'entry-changed' };
    }
    catch (inspectError) {
      if (inspectError && inspectError.code === 'ENOENT') return { ok: true, status: 'missing' };
      return { ok: false };
    }
  }

  if (action === 'directory-page') {
    return boundedCurrentDirectoryPage(request);
  }

  if (action === 'directory-names') {
    if (!Number.isSafeInteger(request.maxEntries) || request.maxEntries < 0) return { ok: false };
    var beforeDir = fs.lstatSync('.', { bigint: true });
    var handle;
    var names = [];
    try {
      handle = fs.opendirSync('.');
      var entry;
      while ((entry = handle.readSync()) !== null) {
        if (names.length >= request.maxEntries) return { ok: false, code: 'directory-entry-limit' };
        names.push(entry.name);
      }
    } finally { if (handle) try { handle.closeSync(); } catch (dirCloseError) {} }
    if (process.env.ORCHESTRATOR_FILE_GUARD_TEST_MODE === '1' && safeName(request.testMutateDirectoryName)) {
      var mutationFd = fs.openSync(request.testMutateDirectoryName, fs.constants.O_WRONLY | fs.constants.O_CREAT |
        fs.constants.O_EXCL | (fs.constants.O_NOFOLLOW || 0), 0o600);
      try { fs.fsyncSync(mutationFd); } finally { fs.closeSync(mutationFd); }
      fsyncDirectoryRequired();
    }
    var afterDir = fs.lstatSync('.', { bigint: true });
    if (!sameStableDirectory(beforeDir, afterDir)) {
      return { ok: false, code: 'directory-changed' };
    }
    return { ok: true, names: names };
  }

  if (action === 'tail') {
    if (!safeName(request.name) || !Number.isSafeInteger(request.maxBytes) || request.maxBytes < 0) return { ok: false };
    var tailBefore, tailFd;
    try {
      tailBefore = fs.lstatSync(request.name, { bigint: true });
      if (!tailBefore.isFile() || tailBefore.isSymbolicLink() || tailBefore.nlink !== 1n || tailBefore.size < 0n ||
          tailBefore.size > BigInt(Number.MAX_SAFE_INTEGER)) return { ok: false };
      tailFd = fs.openSync(request.name, fs.constants.O_RDONLY | (fs.constants.O_NOFOLLOW || 0));
      var tailOpened = fs.fstatSync(tailFd, { bigint: true });
      if (!sameStableFile(tailBefore, tailOpened)) return { ok: false };
      replacePublicRegularForTest(request.name, request.testReplaceAfterOpenBytes, 'tail-test-displaced');
      var length = Number(tailOpened.size < BigInt(request.maxBytes) ? tailOpened.size : BigInt(request.maxBytes));
      var tailBytes = Buffer.allocUnsafe(length);
      var tailOffset = 0;
      var tailPosition = Number(tailOpened.size - BigInt(length));
      while (tailOffset < length) {
        var tailCount = fs.readSync(tailFd, tailBytes, tailOffset, length - tailOffset,
          tailPosition + tailOffset);
        if (!tailCount) break;
        tailOffset += tailCount;
      }
      var tailAfterFd = fs.fstatSync(tailFd, { bigint: true });
      var tailLive = fs.lstatSync(request.name, { bigint: true });
      if (!sameStableFile(tailOpened, tailAfterFd) || !sameStableFile(tailAfterFd, tailLive)) return { ok: false };
      return { ok: true, bytes: tailBytes.subarray(0, tailOffset).toString('base64') };
    } catch (tailError) { return { ok: false }; }
    finally { if (tailFd !== undefined) try { fs.closeSync(tailFd); } catch (tailCloseError) {} }
  }

  if (action === 'transfer-no-clobber') {
    if (!safeName(request.source) || !safeName(request.target) || request.source === request.target) {
      return { ok: false, code: 'invalid-request' };
    }
    var transferLoaded = loadTransferTransaction(request.source);
    if (!transferLoaded.ok) return transferLoaded;
    if (transferLoaded.manifest) {
      if (transferLoaded.manifest.target !== request.target) {
        return { ok: false, code: 'transfer-target-mismatch' };
      }
      var transferRecovered = advanceTransferTransaction(request, transferLoaded.names,
        transferLoaded.manifest);
      return { ok: !!(transferRecovered.ok && transferRecovered.outcome === 'transferred'),
        code: transferRecovered.code || transferRecovered.outcome };
    }
    if (!request.sourceExpected) return { ok: false, code: 'transfer-source-missing' };
    var transferStarted = startTransferTransaction(request);
    if (!transferStarted.ok) return transferStarted;
    var transferResult = advanceTransferTransaction(request, transferStarted.names,
      transferStarted.manifest);
    return { ok: !!(transferResult.ok && transferResult.outcome === 'transferred'),
      code: transferResult.code || transferResult.outcome };
  }

  if (action === 'quarantine-unlink-matching') {
    if (!safeName(request.name) || !Number.isSafeInteger(request.maxBytes) || request.maxBytes < 0 ||
        (request.matchKind !== 'bytes' && request.matchKind !== 'sha256')) {
      return { ok: false, code: 'invalid-request' };
    }
    var matchingBytes = null;
    if (request.matchKind === 'bytes') {
      if (typeof request.expectedBytes !== 'string') return { ok: false, code: 'invalid-request' };
      matchingBytes = Buffer.from(request.expectedBytes, 'base64');
      if (matchingBytes.toString('base64') !== request.expectedBytes || matchingBytes.length > request.maxBytes) {
        return { ok: false, code: 'invalid-request' };
      }
    } else if (typeof request.expectedHash !== 'string' ||
        !/^sha256:[a-f0-9]{64}$/.test(request.expectedHash)) {
      return { ok: false, code: 'invalid-request' };
    }

    var matchingLoaded = loadGuardTransaction(request.name);
    if (!matchingLoaded.ok) return matchingLoaded;
    if (matchingLoaded.manifest) {
      if (matchingLoaded.manifest.kind !== 'file-unlink' ||
          (matchingLoaded.manifest.intent !== 'delete' &&
            matchingLoaded.manifest.intent !== 'delete-matching')) {
        return { ok: false, code: 'wal-kind-mismatch' };
      }
      var matchingWasPriorGeneration = !!request.targetExpected &&
        JSON.stringify(request.targetExpected) === JSON.stringify(matchingLoaded.manifest.targetExpected);
      var matchingRecovered = advanceGuardTransaction(request, matchingLoaded.names,
        matchingLoaded.manifest, 'restore');
      if (!matchingRecovered.ok) return matchingRecovered;
      if (matchingWasPriorGeneration && matchingRecovered.outcome === 'deleted') {
        return { ok: true, outcome: 'deleted' };
      }
      // A prior exact generation may have been reconciled while a new public
      // owner occupied the released name. Continue in this same anchored
      // transaction and match the caller-bound new generation; do not make the
      // caller retry through a stale private WAL.
    }
    if (!request.targetExpected) {
      var matchingMissing = missingName(request.name);
      return { ok: matchingMissing, code: matchingMissing ? 'already-missing' : 'target-unsafe' };
    }
    var matchingRead = boundedRegularFile(request.name, request.maxBytes);
    if (!matchingRead || !exactStatMatchesProof(matchingRead.stat, request.targetExpected, 'file')) {
      return { ok: false, code: 'target-proof-mismatch' };
    }
    var matchingAccepted = request.matchKind === 'bytes'
      ? matchingRead.bytes.equals(matchingBytes)
      : sha256(matchingRead.bytes) === request.expectedHash;
    if (!matchingAccepted) return { ok: false, code: 'content-mismatch' };
    var matchingStarted = startGuardTransaction(request, request.name, 'file-unlink', 'delete-matching');
    if (!matchingStarted.ok) return matchingStarted;
    var matchingResult = advanceGuardTransaction(request, matchingStarted.names,
      matchingStarted.manifest, 'restore');
    return { ok: !!(matchingResult.ok && matchingResult.outcome === 'deleted'),
      code: matchingResult.code || matchingResult.outcome };
  }

  if (action === 'quarantine-unlink') {
    if (!safeName(request.name)) return { ok: false, code: 'unsafe-name' };
    var directLoaded = loadGuardTransaction(request.name);
    if (!directLoaded.ok) return directLoaded;
    if (directLoaded.manifest) {
      if (directLoaded.manifest.kind !== 'file-unlink') return { ok: false, code: 'wal-kind-mismatch' };
      var directRecovered = advanceGuardTransaction(request, directLoaded.names,
        directLoaded.manifest, 'restore');
      return { ok: !!(directRecovered.ok && directLoaded.manifest.intent === 'delete' &&
        directRecovered.outcome === 'deleted'), code: directRecovered.code };
    }
    if (!request.targetExpected) return { ok: !!(request.allowMissing && missingName(request.name)) };
    var directStarted = startGuardTransaction(request, request.name, 'file-unlink', 'delete');
    if (!directStarted.ok) return directStarted;
    var directResult = advanceGuardTransaction(request, directStarted.names, directStarted.manifest, 'restore');
    return { ok: !!(directResult.ok && directResult.outcome === 'deleted'), code: directResult.code };
  }

  if (action === 'quarantine-detach-read') {
    if (!safeName(request.name) || !Number.isSafeInteger(request.maxBytes) || request.maxBytes < 0) {
      return { ok: false, code: 'invalid-request' };
    }
    var pendingLoaded = loadGuardTransaction(request.name);
    if (!pendingLoaded.ok) return pendingLoaded;
    var pendingManifest;
    var pendingNames;
    if (pendingLoaded.manifest) {
      if (pendingLoaded.manifest.kind !== 'file-unlink' ||
          pendingLoaded.manifest.intent !== 'pending-conditional') return { ok: false, code: 'wal-kind-mismatch' };
      pendingManifest = pendingLoaded.manifest;
      pendingNames = pendingLoaded.names;
    } else {
      if (!request.targetExpected) return { ok: false, code: 'target-missing' };
      var pendingStarted = startGuardTransaction(request, request.name, 'file-unlink', 'pending-conditional');
      if (!pendingStarted.ok) return pendingStarted;
      pendingManifest = pendingStarted.manifest;
      pendingNames = pendingStarted.names;
    }
    var pendingResult = advanceGuardTransaction(request, pendingNames, pendingManifest, 'resume');
    if (!pendingResult.ok || pendingResult.outcome !== 'detached') return pendingResult;
    var heldRead = boundedRegularFile(pendingManifest.capture, request.maxBytes);
    if (!heldRead || heldRead.stat.nlink !== 1n || !guardCaptureMatches(heldRead.stat, pendingManifest)) {
      return { ok: false, code: 'capture-read-failed' };
    }
    return {
      ok: true, quarantine: pendingManifest.capture, operationId: pendingManifest.operationId,
      bytes: heldRead.bytes.toString('base64'), stat: statShape(heldRead.stat)
    };
  }

  if (action === 'quarantine-finalize') {
    if (!safeName(request.name) || !safeName(request.quarantine) ||
        !GUARD_OPERATION_RE.test(String(request.operationId || '')) ||
        !validExactProof(request.fileExpected, 'file') ||
        (request.decision !== 'delete' && request.decision !== 'restore')) return { ok: false, code: 'invalid-request' };
    var finalizeLoaded = loadGuardTransaction(request.name);
    if (!finalizeLoaded.ok || !finalizeLoaded.manifest) return { ok: false, code: finalizeLoaded.code || 'wal-missing' };
    var finalizeManifest = finalizeLoaded.manifest;
    if (finalizeManifest.kind !== 'file-unlink' || finalizeManifest.intent !== 'pending-conditional' ||
        finalizeManifest.operationId !== request.operationId || finalizeManifest.capture !== request.quarantine) {
      return { ok: false, code: 'wal-operation-mismatch' };
    }
    var finalizeCapture = guardPublicStat(finalizeManifest.capture);
    if (!finalizeCapture.stat || !guardCaptureMatches(finalizeCapture.stat, finalizeManifest) ||
        !exactStatMatchesProof(finalizeCapture.stat, request.fileExpected, 'file')) {
      return { ok: false, code: 'capture-proof-mismatch' };
    }
    var decision = withGuardChecksum(guardDecisionPayload(finalizeManifest.operationId,
      finalizeManifest.name, request.decision));
    var decisionPublished = publishWalRecord(request, finalizeLoaded.names.decision, decision,
      function (value) { return validGuardDecision(value, finalizeManifest); }, 'decision');
    if (!decisionPublished.ok) return decisionPublished;
    guardCrash(request, 'guard:after-decision');
    var finalized = advanceGuardTransaction(request, finalizeLoaded.names, finalizeManifest, 'resume');
    return { ok: !!(finalized.ok && finalized.outcome ===
      (request.decision === 'delete' ? 'deleted' : 'restored')), code: finalized.code };
  }

  if (action === 'remove-empty-directory') {
    if (!safeName(request.name)) return { ok: false, code: 'invalid-request' };
    var emptyLoaded = loadGuardTransaction(request.name);
    if (!emptyLoaded.ok) return emptyLoaded;
    if (emptyLoaded.manifest) {
      if (emptyLoaded.manifest.kind !== 'empty-rmdir' || emptyLoaded.manifest.intent !== 'delete') {
        return { ok: false, code: 'wal-kind-mismatch' };
      }
      var emptyRecovered = advanceGuardTransaction(request, emptyLoaded.names, emptyLoaded.manifest, 'restore');
      return { ok: !!(emptyRecovered.ok && emptyRecovered.outcome === 'deleted'), code: emptyRecovered.code };
    }
    if (!request.targetExpected) return { ok: false, code: 'target-missing' };
    var emptyStarted = startGuardTransaction(request, request.name, 'empty-rmdir', 'delete');
    if (!emptyStarted.ok) return emptyStarted;
    var emptyResult = advanceGuardTransaction(request, emptyStarted.names, emptyStarted.manifest, 'restore');
    return { ok: !!(emptyResult.ok && emptyResult.outcome === 'deleted'), code: emptyResult.code };
  }

  if (action === 'fsync-file') {
    if (!safeName(request.name)) return { ok: false };
    var syncFd;
    try {
      var syncBefore = fs.lstatSync(request.name, { bigint: true });
      if (!syncBefore.isFile() || syncBefore.isSymbolicLink() || syncBefore.nlink !== 1n) return { ok: false };
      syncFd = fs.openSync(request.name, fs.constants.O_RDONLY | (fs.constants.O_NOFOLLOW || 0));
      var syncOpened = fs.fstatSync(syncFd, { bigint: true });
      if (!sameStableFile(syncBefore, syncOpened) || syncOpened.nlink !== 1n) return { ok: false };
      replacePublicRegularForTest(request.name, request.testReplaceAfterOpenBytes, 'fsync-test-displaced');
      fs.fsyncSync(syncFd);
      var syncAfterFd = fs.fstatSync(syncFd, { bigint: true });
      var syncLive = fs.lstatSync(request.name, { bigint: true });
      return { ok: sameStableFile(syncOpened, syncAfterFd) && sameStableFile(syncAfterFd, syncLive) &&
        syncLive.nlink === 1n };
    } catch (syncError) { return { ok: false }; }
    finally { if (syncFd !== undefined) try { fs.closeSync(syncFd); } catch (syncCloseError) {} }
  }

  if (action === 'fsync-directory') {
    var directorySynced = fsyncDirectoryRequired();
    return directorySynced ? { ok: true } :
      { ok: false, code: 'directory-durability-unsupported' };
  }
  return { ok: false, code: 'unknown-action' };
}

function writerWalkParts(parts, expectedProofs, create, mode) {
  if (!Array.isArray(parts) || !Array.isArray(expectedProofs) || parts.length !== expectedProofs.length) {
    return { ok: false, code: 'writer-chain-invalid' };
  }
  var proofs = [];
  for (var i = 0; i < parts.length; i++) {
    if (!safeName(parts[i])) return { ok: false, code: 'writer-component-unsafe' };
    var expected = expectedProofs[i];
    if (expected === null) {
      if (!create) return { ok: false, code: 'writer-component-unproven' };
      try {
        fs.mkdirSync(parts[i], { mode: mode || 0o700 });
        if (!fsyncDirectoryRequired()) return { ok: false, code: 'writer-component-unsynced' };
      } catch (createError) {
        // A name which appeared after the parent proved absence belongs to a
        // racing generation. Never adopt it as part of the authority chain.
        return { ok: false, code: createError && createError.code === 'EEXIST' ? 'writer-component-raced' : 'writer-component-create-failed' };
      }
    }
    var component;
    try { component = fs.lstatSync(parts[i], { bigint: true }); }
    catch (componentError) { return { ok: false, code: 'writer-component-missing' }; }
    if (!component.isDirectory() || component.isSymbolicLink() ||
        (expected !== null && !exactBigintShape(component, expected, 'directory'))) {
      return { ok: false, code: 'writer-component-mismatch' };
    }
    var actual = bigintShape(component);
    try { process.chdir(parts[i]); }
    catch (chdirError) { return { ok: false, code: 'writer-component-raced' }; }
    var pinned = fs.lstatSync('.', { bigint: true });
    if (!exactBigintShape(pinned, actual, 'directory')) return { ok: false, code: 'writer-component-raced' };
    proofs.push(actual);
  }
  return { ok: true, proofs: proofs, stat: statShape(fs.lstatSync('.', { bigint: true })) };
}

function dispatchWriter(request) {
  var rootPath = process.cwd();
  var rootBefore;
  try { rootBefore = fs.lstatSync('.', { bigint: true }); }
  catch (rootError) { return { ok: false, code: 'writer-root-unsafe' }; }
  if (!exactBigintShape(rootBefore, request.writerRootProof, 'directory')) {
    return { ok: false, code: 'writer-root-mismatch' };
  }
  var walked = writerWalkParts(request.directoryParts, request.writerComponentProofs,
    request.createDirectory === true, request.directoryMode || 0o700);
  if (!walked.ok) return walked;
  var finalDirectory = fs.lstatSync('.', { bigint: true });
  if (request.writerDirectoryProof && !exactBigintShape(finalDirectory, request.writerDirectoryProof, 'directory')) {
    return { ok: false, code: 'directory-mismatch' };
  }
  var operationRequest = Object.assign({}, request, {
    expected: walked.stat,
    directoryParts: undefined,
    createDirectory: undefined,
    allowMissingDirectory: undefined,
    directoryMode: undefined,
    writerDirectoryProof: undefined,
    writerRootProof: undefined,
    writerComponentProofs: undefined
  });
  var value = dispatchCurrent(operationRequest);
  try {
    process.chdir(rootPath);
    var rootAfter = fs.lstatSync('.', { bigint: true });
    if (!exactBigintShape(rootAfter, request.writerRootProof, 'directory')) {
      return { ok: false, code: 'writer-root-raced' };
    }
    var verified = writerWalkParts(request.directoryParts, walked.proofs, false, request.directoryMode || 0o700);
    if (!verified.ok) return { ok: false, code: 'writer-chain-raced' };
    var verifiedFinal = fs.lstatSync('.', { bigint: true });
    if (!exactBigintShape(verifiedFinal, bigintShape(finalDirectory), 'directory')) {
      return { ok: false, code: 'writer-chain-raced' };
    }
    if (value && typeof value === 'object') value.writerDirectoryProof = bigintShape(verifiedFinal);
    return value;
  } catch (revalidateError) {
    return { ok: false, code: 'writer-chain-raced' };
  }
}

function generalWalkParts(parts, expectedProofs, create, allowMissing, mode, exactExpected) {
  if (!Array.isArray(parts) || !Array.isArray(expectedProofs) || parts.length !== expectedProofs.length) {
    return { ok: false, code: 'guard-chain-invalid' };
  }
  var proofs = [];
  for (var i = 0; i < parts.length; i++) {
    if (!safeName(parts[i])) return { ok: false, code: 'guard-component-unsafe' };
    var expected = expectedProofs[i];
    if (expected === null) {
      try {
        fs.lstatSync(parts[i], { bigint: true });
        return { ok: false, code: 'guard-component-raced' };
      } catch (missingError) {
        if (!missingError || missingError.code !== 'ENOENT') return { ok: false, code: 'guard-component-unsafe' };
      }
      if (!create) return allowMissing
        ? { ok: true, exists: false, missingIndex: i, proofs: proofs }
        : { ok: false, code: 'guard-component-missing' };
      try {
        fs.mkdirSync(parts[i], { mode: mode || 0o700 });
        if (!fsyncDirectoryRequired()) return { ok: false, code: 'guard-component-unsynced' };
      } catch (createError) {
        return { ok: false, code: createError && createError.code === 'EEXIST'
          ? 'guard-component-raced' : 'guard-component-create-failed' };
      }
    } else if (!validExactProof(expected, 'directory')) {
      return { ok: false, code: 'guard-component-proof-invalid' };
    }
    var component;
    try { component = fs.lstatSync(parts[i], { bigint: true }); }
    catch (componentError) { return { ok: false, code: 'guard-component-missing' }; }
    var expectedIdentity = expected === null || identityMatchesProof(component, expected, 'directory');
    if (!component.isDirectory() || component.isSymbolicLink() || !expectedIdentity) {
      return { ok: false, code: 'guard-component-mismatch' };
    }
    if (expected !== null && exactExpected && !exactStatMatchesProof(component, expected, 'directory')) {
      return { ok: false, code: 'guard-component-proof-stale' };
    }
    var actual = statShape(component);
    try { process.chdir(parts[i]); }
    catch (chdirError) { return { ok: false, code: 'guard-component-raced' }; }
    var pinned = fs.lstatSync('.', { bigint: true });
    if (!identityMatchesProof(pinned, actual, 'directory')) return { ok: false, code: 'guard-component-raced' };
    if (exactExpected && !exactStatMatchesProof(pinned, actual, 'directory')) {
      return { ok: false, code: 'guard-component-proof-stale' };
    }
    proofs.push(actual);
  }
  return { ok: true, exists: true, proofs: proofs, stat: statShape(fs.lstatSync('.', { bigint: true })) };
}

function verifyGeneralMissing(parts, expectedProofs) {
  for (var i = 0; i < parts.length; i++) {
    var expected = expectedProofs[i];
    if (expected === null) {
      try { fs.lstatSync(parts[i], { bigint: true }); return false; }
      catch (missingError) { return !!missingError && missingError.code === 'ENOENT'; }
    }
    if (!validExactProof(expected, 'directory')) return false;
    var component;
    try { component = fs.lstatSync(parts[i], { bigint: true }); }
    catch (componentError) { return false; }
    if (!exactStatMatchesProof(component, expected, 'directory')) return false;
    try { process.chdir(parts[i]); }
    catch (chdirError) { return false; }
    if (!exactStatMatchesProof(fs.lstatSync('.', { bigint: true }), expected, 'directory')) return false;
  }
  return false;
}

function dispatchGeneral(request) {
  var rootPath = process.cwd();
  var rootBefore;
  try { rootBefore = fs.lstatSync('.', { bigint: true }); }
  catch (rootError) { return { ok: false, code: 'guard-root-unsafe' }; }
  if (!validExactProof(request.expected, 'directory') ||
      !identityMatchesProof(rootBefore, request.expected, 'directory')) {
    return { ok: false, code: 'guard-root-mismatch' };
  }
  if (!exactStatMatchesProof(rootBefore, request.expected, 'directory')) {
    return { ok: false, code: 'guard-root-proof-stale' };
  }
  var walked = generalWalkParts(request.directoryParts, request.componentProofs,
    request.createDirectory === true, request.allowMissingDirectory === true,
    request.directoryMode || 0o700, true);
  if (!walked.ok) return walked;
  if (!walked.exists) {
    try {
      process.chdir(rootPath);
      var missingRoot = fs.lstatSync('.', { bigint: true });
      if (!exactStatMatchesProof(missingRoot, request.expected, 'directory') ||
          !verifyGeneralMissing(request.directoryParts, request.componentProofs)) {
        return { ok: false, code: 'guard-missing-raced' };
      }
      return { ok: true, exists: false };
    } catch (missingRevalidateError) { return { ok: false, code: 'guard-missing-raced' }; }
  }
  var finalDirectory = fs.lstatSync('.', { bigint: true });
  var operationRequest = Object.assign({}, request, {
    expected: statShape(finalDirectory),
    directoryParts: undefined,
    componentProofs: undefined,
    createDirectory: undefined,
    allowMissingDirectory: undefined,
    directoryMode: undefined
  });
  var value = dispatchCurrent(operationRequest);
  try {
    process.chdir(rootPath);
    var rootAfter = fs.lstatSync('.', { bigint: true });
    if (!identityMatchesProof(rootAfter, request.expected, 'directory')) {
      return { ok: false, code: 'guard-root-raced' };
    }
    var verified = generalWalkParts(request.directoryParts, walked.proofs, false, false,
      request.directoryMode || 0o700, false);
    if (!verified.ok || !verified.exists) return { ok: false, code: 'guard-chain-raced' };
    var verifiedFinal = fs.lstatSync('.', { bigint: true });
    if (!sameIdentity(verifiedFinal, finalDirectory)) return { ok: false, code: 'guard-chain-raced' };
    if (value && typeof value === 'object') value.directoryStat = statShape(verifiedFinal);
    return value;
  } catch (revalidateError) {
    return { ok: false, code: 'guard-chain-raced' };
  }
}

function dispatch(request) {
  // Most calls start the helper at the trusted project-root inode and provide
  // the guarded directory as a component array. Walk, operate, then walk the
  // public chain again in this same isolated process. That gives one process
  // launch per transaction without weakening the ancestor-race fence.
  if (!request || !Array.isArray(request.directoryParts)) return dispatchCurrent(request);
  if (request.writerRootProof) return dispatchWriter(request);
  return dispatchGeneral(request);
}

function finish(value) {
  var bytes;
  try { bytes = Buffer.from(JSON.stringify(value), 'utf8'); }
  catch (error) { bytes = Buffer.from('{"ok":false,"code":"encode-failed"}', 'utf8'); }
  var offset = 0;
  while (offset < bytes.length) {
    var written = fs.writeSync(1, bytes, offset, bytes.length - offset, null);
    if (!written) break;
    offset += written;
  }
}

function serve() {
  var pending = '';
  var pendingBytes = 0;
  var decoder = new StringDecoder('utf8');
  var chunk = Buffer.allocUnsafe(64 * 1024);
  while (true) {
    var count = fs.readSync(0, chunk, 0, chunk.length, null);
    if (!count) return;
    pending += decoder.write(chunk.subarray(0, count));
    pendingBytes += count;
    if (pendingBytes > 64 * 1024 * 1024) return;
    var newline;
    while ((newline = pending.indexOf('\n')) >= 0) {
      var line = pending.slice(0, newline);
      pending = pending.slice(newline + 1);
      pendingBytes = Buffer.byteLength(pending, 'utf8');
      var response;
      var request;
      try {
        request = JSON.parse(line);
        if (!request || typeof request.rootPath !== 'string') throw new Error('root path missing');
        process.chdir(request.rootPath);
        response = dispatch(request);
      } catch (error) {
        response = { ok: false, code: 'worker-failed' };
      }
      // Never idle while pinning a mutable runtime subdirectory. This reset is
      // not authoritative; the next dispatch still validates the root inode
      // before doing any work.
      try { if (request && typeof request.rootPath === 'string') process.chdir(request.rootPath); }
      catch (resetError) {}
      finish(response);
      fs.writeSync(1, '\n');
    }
  }
}

if (process.argv[2] === '--serve') {
  try { serve(); } catch (serveError) { process.exitCode = 1; }
} else {
  try {
    var raw = fs.readFileSync(0, 'utf8');
    if (raw.length > 64 * 1024 * 1024) finish({ ok: false, code: 'input-too-large' });
    else finish(dispatch(JSON.parse(raw)));
  } catch (error) {
    finish({ ok: false, code: 'worker-failed' });
  }
}
