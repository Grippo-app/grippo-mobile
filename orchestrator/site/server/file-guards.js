'use strict';

var fs = require('fs');
var path = require('path');
var cp = require('child_process');
var crypto = require('crypto');
var isTestInjectionKey = require('./child-env').isTestInjectionKey;

var WORKER = path.join(__dirname, 'file-guard-worker.js');
var CANONICAL_PROJECT_ROOT = path.resolve(__dirname, '..', '..', '..');
var WORKER_INPUT_MAX = 64 * 1024 * 1024;
var WORKER_OUTPUT_MAX = 96 * 1024 * 1024;
var PUBLICATION_MAX_BYTES = 512 * 1024;
var LARGE_PUBLICATION_MAX_BYTES = 4 * 1024 * 1024;
var ORIGINAL_SPAWN_SYNC = cp.spawnSync;
// Workers are root-bound. A small LRU pool avoids respawning on every project/cache switch.
var persistentWorkers = Object.create(null);
var persistentBusy = false;
var persistentDisabled = false;
var PERSISTENT_WORKER_LIMIT = 8;
var DECIMAL_RE = /^(?:0|[1-9][0-9]*)$/;
var EXACT_STAT_FIELDS = ['ctimeNs', 'dev', 'ino', 'mode', 'mtimeNs', 'nlink', 'size', 'type'];

function isUnder(root, target) {
  return target === root || target.indexOf(root + path.sep) === 0;
}

function rawType(stat) {
  return stat.isDirectory() ? 'directory' : (stat.isFile() ? 'file' :
    (stat.isSymbolicLink() ? 'symlink' : 'other'));
}

function exactStatShape(stat) {
  if (!stat || typeof stat !== 'object') return null;
  if (typeof stat.isDirectory === 'function') {
    var raw = {
      dev: String(stat.dev), ino: String(stat.ino), mode: String(stat.mode), nlink: String(stat.nlink),
      size: String(stat.size), mtimeNs: String(stat.mtimeNs), ctimeNs: String(stat.ctimeNs), type: rawType(stat)
    };
    return EXACT_STAT_FIELDS.every(function (field) {
      return field === 'type' || DECIMAL_RE.test(raw[field]);
    }) ? raw : null;
  }
  var shaped = {
    dev: stat.dev, ino: stat.ino,
    mode: typeof stat.modeExact === 'string' ? stat.modeExact : stat.mode,
    nlink: stat.nlink,
    size: typeof stat.sizeExact === 'string' ? stat.sizeExact : stat.size,
    mtimeNs: stat.mtimeNs, ctimeNs: stat.ctimeNs, type: stat.type
  };
  if ((shaped.type !== 'directory' && shaped.type !== 'file' && shaped.type !== 'symlink' && shaped.type !== 'other') ||
      !['dev', 'ino', 'mode', 'nlink', 'size', 'mtimeNs', 'ctimeNs'].every(function (field) {
        return typeof shaped[field] === 'string' && DECIMAL_RE.test(shaped[field]);
      })) return null;
  return shaped;
}

function exactFieldsEqual(left, right, fields) {
  var a = exactStatShape(left), b = exactStatShape(right);
  return !!a && !!b && fields.every(function (field) { return a[field] === b[field]; });
}

function sameStat(a, b) {
  return exactFieldsEqual(a, b, EXACT_STAT_FIELDS);
}

function sameDirectoryIdentity(a, b) {
  return exactFieldsEqual(a, b, ['dev', 'ino', 'mode', 'type']) && exactStatShape(a).type === 'directory';
}

function bigintDirectoryShape(stat) {
  var proof = exactStatShape(stat);
  return proof && proof.type === 'directory' ? proof : null;
}

function safeDerivedNumber(decimal, overflow) {
  if (typeof decimal !== 'string' || !DECIMAL_RE.test(decimal)) return null;
  var value = BigInt(decimal);
  return value <= BigInt(Number.MAX_SAFE_INTEGER) ? Number(value) : overflow;
}

function nsToMs(decimal) {
  if (typeof decimal !== 'string' || !DECIMAL_RE.test(decimal)) return null;
  var ns = BigInt(decimal), whole = ns / 1000000n, remainder = ns % 1000000n;
  if (whole > BigInt(Number.MAX_SAFE_INTEGER)) return Infinity;
  return Number(whole) + Number(remainder) / 1000000;
}

function hydratedStat(value) {
  var exact = exactStatShape(value);
  if (!exact || Object.keys(value).sort().join('\0') !== EXACT_STAT_FIELDS.slice().sort().join('\0')) return null;
  var out = {
    // Authority-bearing identity fields stay lossless. Numeric companions are
    // explicitly derived display/compatibility values and are never used by
    // this module for filesystem authorization.
    dev: exact.dev, ino: exact.ino, nlink: exact.nlink,
    mtimeNs: exact.mtimeNs, ctimeNs: exact.ctimeNs,
    mode: safeDerivedNumber(exact.mode, null), modeExact: exact.mode,
    size: safeDerivedNumber(exact.size, Infinity), sizeExact: exact.size,
    nlinkNumber: safeDerivedNumber(exact.nlink, Infinity),
    mtimeMs: nsToMs(exact.mtimeNs), ctimeMs: nsToMs(exact.ctimeNs),
    type: exact.type
  };
  out.isDirectory = function () { return out.type === 'directory'; };
  out.isFile = function () { return out.type === 'file'; };
  out.isSymbolicLink = function () { return out.type === 'symlink'; };
  return out;
}

function sameFileIdentity(stat, expected) {
  return exactFieldsEqual(stat, expected, ['dev', 'ino', 'mode', 'type']) && exactStatShape(stat).type === 'file';
}

function directChild(directory, target) {
  if (typeof directory !== 'string' || typeof target !== 'string') return false;
  var directoryAbs = path.resolve(directory);
  var targetAbs = path.resolve(target);
  if (directory !== directoryAbs || target !== targetAbs) return false;
  return path.dirname(targetAbs) === directoryAbs && path.basename(targetAbs) !== '.' && path.basename(targetAbs) !== '..';
}

// Pure naming contract for scoped integrity. Consumers can inspect every
// deterministic target-keyed WAL name without copying hash/prefix internals;
// operation-id private captures remain discoverable through the referenced
// manifest fields and the exported private prefixes.
function guardTransactionEvidenceForTarget(directory, target) {
  if (!directChild(directory, target)) return null;
  var name = path.basename(target);
  var key = crypto.createHash('sha256').update(name, 'utf8').digest('hex');
  var deleteBase = '.guard-txn-' + key;
  var publicationBase = '.guard-publish-' + key;
  function evidence(kind, role, entryName) {
    return { kind: kind, role: role, name: entryName, path: path.join(directory, entryName) };
  }
  var entries = [
    evidence('delete', 'manifest', deleteBase + '.json'),
    evidence('delete', 'manifest-stage', deleteBase + '.json.stage'),
    evidence('delete', 'decision', deleteBase + '.decision.json'),
    evidence('delete', 'decision-stage', deleteBase + '.decision.json.stage'),
    evidence('delete', 'receipt', deleteBase + '.receipt.json'),
    evidence('delete', 'receipt-stage', deleteBase + '.receipt.json.stage'),
    evidence('publication', 'manifest', publicationBase + '.json'),
    evidence('publication', 'manifest-stage', publicationBase + '.json.stage'),
    evidence('publication', 'link', publicationBase + '.link.json'),
    evidence('publication', 'link-stage', publicationBase + '.link.json.stage'),
    evidence('publication', 'receipt', publicationBase + '.receipt.json'),
    evidence('publication', 'receipt-stage', publicationBase + '.receipt.json.stage')
  ];
  return {
    version: 1, target: target, directory: directory, name: name, entries: entries,
    privatePrefixes: {
      deleteCapture: '.guard-capture-', publicationData: '.guard-publish-data-',
      casOld: '.guard-cas-old-', atomicStage: '.' + name + '-'
    },
    atomicStageSuffix: '.tmp'
  };
}

function classifyGuardTransactionEvidenceForTarget(directory, target, entryName) {
  if (typeof entryName !== 'string' || !entryName || entryName.indexOf('/') >= 0 || entryName.indexOf('\\') >= 0) {
    return null;
  }
  var evidence = guardTransactionEvidenceForTarget(directory, target);
  if (!evidence) return null;
  for (var i = 0; i < evidence.entries.length; i++) {
    if (evidence.entries[i].name === entryName) return Object.assign({}, evidence.entries[i]);
  }
  var atomicPrefix = evidence.privatePrefixes.atomicStage;
  if (entryName.indexOf(atomicPrefix) === 0 && entryName.slice(-evidence.atomicStageSuffix.length) === evidence.atomicStageSuffix) {
    var middle = entryName.slice(atomicPrefix.length, -evidence.atomicStageSuffix.length);
    if (/^[1-9][0-9]*-[a-f0-9]{24}$/.test(middle)) {
      return { kind: 'atomic', role: 'stage', name: entryName, path: path.join(directory, entryName) };
    }
  }
  return null;
}

function workerEnvironment(cwd) {
  var env = Object.assign({}, process.env);
  // The helper is a trusted local transaction engine, not another application
  // entry point. Do not let inherited preload/search hooks execute in it.
  delete env.NODE_OPTIONS;
  delete env.NODE_PATH;
  // Fault injection belongs only to explicitly relocated test fixtures. A
  // shell variable must never be able to turn a canonical durable operation
  // into a simulated crash, swap, or unsupported-filesystem result.
  if (path.resolve(cwd) === CANONICAL_PROJECT_ROOT) {
    Object.keys(env).forEach(function (key) {
      if (isTestInjectionKey(key)) delete env[key];
    });
  }
  return env;
}

function outputLimitForBytes(maxBytes) {
  if (!Number.isSafeInteger(maxBytes) || maxBytes < 0) return 1024 * 1024;
  return Math.min(WORKER_OUTPUT_MAX, Math.max(1024 * 1024, Math.ceil(maxBytes * 4 / 3) + 1024 * 1024));
}

function sha256(bytes) {
  return 'sha256:' + crypto.createHash('sha256').update(bytes).digest('hex');
}

function builtInSpawnAvailable() {
  var source;
  try { source = Function.prototype.toString.call(cp.spawn); }
  catch (error) { return false; }
  // Tests deliberately replace child_process.spawn with application-child
  // fakes. Do not let the transaction transport appear as a Claude/npm spawn
  // to those callers; one-shot spawnSync remains the portable fallback.
  return cp.spawn && cp.spawn.name === 'spawn' && cp.spawn.length === 3 &&
    source.indexOf('normalizeSpawnArguments') >= 0 && source.indexOf('new ChildProcess') >= 0;
}

function resetPersistentWorker(expected) {
  Object.keys(persistentWorkers).forEach(function (cwd) {
    var held = persistentWorkers[cwd];
    if (!held || (expected && held.child !== expected)) return;
    delete persistentWorkers[cwd];
    try { held.child.stdin.destroy(); } catch (stdinError) {}
    try { held.child.stdout.destroy(); } catch (stdoutError) {}
    try { held.child.kill('SIGTERM'); } catch (killError) {}
  });
}

function startPersistentWorker(cwd) {
  var held = persistentWorkers[cwd];
  if (held && held.child && held.child.exitCode == null) {
    held.usedAt = Date.now();
    return held;
  }
  if (held) resetPersistentWorker(held.child);
  if (persistentDisabled || !builtInSpawnAvailable()) return null;
  var active = Object.keys(persistentWorkers);
  if (active.length >= PERSISTENT_WORKER_LIMIT) {
    active.sort(function (a, b) { return persistentWorkers[a].usedAt - persistentWorkers[b].usedAt; });
    resetPersistentWorker(persistentWorkers[active[0]].child);
  }
  var child;
  try {
    child = cp.spawn(process.execPath, [WORKER, '--serve'], {
      cwd: cwd, stdio: ['pipe', 'pipe', 'ignore'], env: workerEnvironment(cwd), windowsHide: true
    });
    var inputHandle = child.stdin && child.stdin._handle;
    var outputHandle = child.stdout && child.stdout._handle;
    if (!inputHandle || !outputHandle || typeof inputHandle.fd !== 'number' ||
        typeof outputHandle.fd !== 'number' || typeof inputHandle.setBlocking !== 'function' ||
        typeof outputHandle.setBlocking !== 'function') throw new Error('blocking helper pipes unavailable');
    child.stdout.pause();
    inputHandle.setBlocking(true);
    outputHandle.setBlocking(true);
    var worker = {
      child: child, cwd: cwd, inputFd: inputHandle.fd, outputFd: outputHandle.fd,
      readRemainder: Buffer.alloc(0), usedAt: Date.now()
    };
    persistentWorkers[cwd] = worker;
    child.on('error', function () { resetPersistentWorker(child); });
    child.on('exit', function () { resetPersistentWorker(child); });
    if (typeof child.unref === 'function') child.unref();
    if (child.stdin && typeof child.stdin.unref === 'function') child.stdin.unref();
    if (child.stdout && typeof child.stdout.unref === 'function') child.stdout.unref();
    return worker;
  } catch (error) {
    if (child) {
      try { child.kill('SIGTERM'); } catch (killError) {}
    }
    persistentDisabled = true;
    return null;
  }
}

function writeBlocking(fd, bytes) {
  var offset = 0;
  while (offset < bytes.length) {
    var written = fs.writeSync(fd, bytes, offset, bytes.length - offset, null);
    if (!written) throw new Error('worker request write made no progress');
    offset += written;
  }
}

function readPersistentLine(worker, maxOutputBytes) {
  var fd = worker.outputFd;
  var chunks = [];
  var total = 0;
  if (worker.readRemainder.length) {
    var existingNewline = worker.readRemainder.indexOf(10);
    if (existingNewline >= 0) {
      var immediate = worker.readRemainder.subarray(0, existingNewline).toString('utf8');
      worker.readRemainder = worker.readRemainder.subarray(existingNewline + 1);
      return immediate;
    }
    chunks.push(worker.readRemainder);
    total = worker.readRemainder.length;
    worker.readRemainder = Buffer.alloc(0);
  }
  while (true) {
    if (total > maxOutputBytes) throw new Error('worker response exceeded limit');
    var block = Buffer.allocUnsafe(64 * 1024);
    var count = fs.readSync(fd, block, 0, block.length, null);
    if (!count) throw new Error('worker response pipe closed');
    var received = block.subarray(0, count);
    var newline = received.indexOf(10);
    if (newline >= 0) {
      if (newline) { chunks.push(received.subarray(0, newline)); total += newline; }
      worker.readRemainder = received.subarray(newline + 1);
      if (total > maxOutputBytes) throw new Error('worker response exceeded limit');
      return Buffer.concat(chunks, total).toString('utf8');
    }
    chunks.push(received);
    total += count;
  }
}

function oneShotWorker(cwd, body, maxOutputBytes) {
  var result;
  try {
    result = cp.spawnSync(process.execPath, [WORKER], {
      cwd: cwd, input: body, encoding: 'utf8', env: workerEnvironment(cwd), windowsHide: true,
      maxBuffer: maxOutputBytes
    });
  } catch (spawnError) { return null; }
  if (!result || result.error || result.status !== 0 || typeof result.stdout !== 'string') return null;
  try { return JSON.parse(result.stdout); }
  catch (parseError) { return null; }
}

function callWorker(cwd, expected, request, maxOutputBytes) {
  if (!expected || typeof cwd !== 'string' || !path.isAbsolute(cwd)) return null;
  var rootProof = exactStatShape(expected);
  if (!rootProof || rootProof.type !== 'directory') return null;
  var body;
  try {
    body = JSON.stringify(Object.assign({}, request || {}, { expected: rootProof, rootPath: cwd }));
  } catch (encodeError) { return null; }
  if (Buffer.byteLength(body, 'utf8') > WORKER_INPUT_MAX) return null;
  var outputLimit = Math.min(WORKER_OUTPUT_MAX, Math.max(1024 * 1024, maxOutputBytes || 1024 * 1024));
  // A patched spawnSync is an intentional deterministic race hook in the
  // adversarial tests; honor it with an isolated one-shot transaction.
  if (cp.spawnSync !== ORIGINAL_SPAWN_SYNC || persistentBusy) return oneShotWorker(cwd, body, outputLimit);
  var worker = startPersistentWorker(cwd);
  if (!worker) return oneShotWorker(cwd, body, outputLimit);
  persistentBusy = true;
  try {
    writeBlocking(worker.inputFd, Buffer.from(body + '\n', 'utf8'));
    return JSON.parse(readPersistentLine(worker, outputLimit));
  } catch (transportError) {
    resetPersistentWorker(worker.child);
    return oneShotWorker(cwd, body, outputLimit);
  } finally { persistentBusy = false; }
}

function callUnder(root, directory, request, maxOutputBytes) {
  if (typeof root !== 'string' || typeof directory !== 'string') return null;
  var rootAbs = path.resolve(root);
  var directoryAbs = path.resolve(directory);
  if (root !== rootAbs || directory !== directoryAbs || !isUnder(rootAbs, directoryAbs)) return null;
  request = request || {};
  var parts = path.relative(rootAbs, directoryAbs).split(path.sep).filter(Boolean);
  var lastStale = null;
  var authorityRoot = null;
  var authorityComponents = null;
  // Exact ctime/mtime/nlink proofs are deliberately strict, but sibling
  // transactions can legitimately age the same directory inode between the
  // parent snapshot and worker admission. Retry only the worker's explicit
  // "same exact identity, stale metadata" verdict. A different inode, symlink,
  // missing-component appearance, or any post-operation uncertainty is never
  // retried, because doing so could adopt a foreign generation or duplicate a
  // mutation.
  for (var attempt = 0; attempt < 32; attempt++) {
    var rootStat;
    try { rootStat = fs.lstatSync(rootAbs, { bigint: true }); }
    catch (rootError) { return null; }
    if (!rootStat.isDirectory() || rootStat.isSymbolicLink()) return null;
    var rootProof = exactStatShape(rootStat);
    if (!rootProof) return null;
    if (!authorityRoot) authorityRoot = rootProof;
    else if (!sameDirectoryIdentity(authorityRoot, rootProof)) return lastStale;
    var componentProofs = [], current = rootAbs, missing = false;
    for (var i = 0; i < parts.length; i++) {
      current = path.join(current, parts[i]);
      if (missing) { componentProofs.push(null); continue; }
      try {
        var component = fs.lstatSync(current, { bigint: true });
        if (!component.isDirectory() || component.isSymbolicLink()) return null;
        var componentProof = exactStatShape(component);
        if (!componentProof) return null;
        if (authorityComponents) {
          // A stale-proof retry which now sees a component previously proved
          // absent is the same absence->EEXIST race reported by the worker's
          // create path. Preserve that exact classification so a caller may do
          // a fresh non-mutating proof; changed existing identities still fail
          // with their original stale result and are never re-admitted.
          if (authorityComponents[i] === null) return { ok: false, code: 'guard-component-raced' };
          if (!sameDirectoryIdentity(authorityComponents[i], componentProof)) return lastStale;
        }
        componentProofs.push(componentProof);
      } catch (componentError) {
        if (!componentError || componentError.code !== 'ENOENT' ||
            (request.createDirectory !== true && request.allowMissingDirectory !== true)) return null;
        if (authorityComponents && authorityComponents[i] !== null) return lastStale;
        missing = true;
        componentProofs.push(null);
      }
    }
    if (!authorityComponents) authorityComponents = componentProofs.slice();
    var result = callWorker(rootAbs, rootStat, Object.assign({}, request, {
      directoryParts: parts, componentProofs: componentProofs
    }), maxOutputBytes);
    if (result && !result.ok &&
        (result.code === 'guard-root-proof-stale' || result.code === 'guard-component-proof-stale')) {
      lastStale = result;
      continue;
    }
    if (result && result.ok && result.exists !== false) {
      var directoryStat = hydratedStat(result.directoryStat);
      if (!directoryStat || !directoryStat.isDirectory() || directoryStat.isSymbolicLink()) return null;
    }
    return result;
  }
  return lastStale;
}

function callWriterUnder(root, directory, request, maxOutputBytes) {
  if (typeof root !== 'string' || typeof directory !== 'string') return null;
  var rootAbs = path.resolve(root);
  var directoryAbs = path.resolve(directory);
  if (root !== rootAbs || directory !== directoryAbs || !isUnder(rootAbs, directoryAbs)) return null;
  var rootBig;
  try {
    rootBig = fs.lstatSync(rootAbs, { bigint: true });
  } catch (rootError) { return null; }
  if (!rootBig.isDirectory() || rootBig.isSymbolicLink()) return null;
  var parts = path.relative(rootAbs, directoryAbs).split(path.sep).filter(Boolean);
  var componentProofs = [];
  var current = rootAbs;
  var missing = false;
  for (var i = 0; i < parts.length; i++) {
    current = path.join(current, parts[i]);
    if (missing) { componentProofs.push(null); continue; }
    try {
      var component = fs.lstatSync(current, { bigint: true });
      if (!component.isDirectory() || component.isSymbolicLink()) return null;
      componentProofs.push(bigintDirectoryShape(component));
    } catch (componentError) {
      if (!componentError || componentError.code !== 'ENOENT' || request.createDirectory !== true) return null;
      missing = true;
      componentProofs.push(null);
    }
  }
  return callWorker(rootAbs, rootBig, Object.assign({}, request || {}, {
    directoryParts: parts,
    writerRootProof: bigintDirectoryShape(rootBig),
    writerComponentProofs: componentProofs
  }), maxOutputBytes);
}

// Verify (and optionally create) every component from an isolated child whose
// cwd starts at the exact checked project-root inode. The child may use chdir:
// unlike process.chdir() in the long-lived server, it cannot redirect queued
// libuv work in any other process.
function realDirectoryUnder(root, candidate, options) {
  options = options || {};
  if (typeof root !== 'string' || !root || typeof candidate !== 'string' || !candidate) return null;
  var rootAbs = path.resolve(root);
  var targetAbs = path.resolve(candidate);
  if (root !== rootAbs || candidate !== targetAbs || !isUnder(rootAbs, targetAbs)) return null;
  var walked = callUnder(rootAbs, targetAbs, {
    action: 'directory-proof', createDirectory: !!options.create,
    allowMissingDirectory: !!options.allowMissing, directoryMode: options.mode || 0o700
  });
  if (!walked || !walked.ok) {
    // A narrow internal diagnostic channel lets a caller distinguish the one
    // benign create race it may re-prove from durability, ancestor, transport,
    // and post-operation failures which must remain fail-closed. The ordinary
    // return contract stays null-on-failure for every existing consumer.
    if (options.failure && typeof options.failure === 'object' && !Array.isArray(options.failure)) {
      try { options.failure.code = walked && typeof walked.code === 'string' ? walked.code : 'guard-unavailable'; }
      catch (failureSinkError) {}
    }
    return null;
  }
  if (!walked.exists) return options.allowMissing ? { path: targetAbs, stat: null, exists: false } : null;
  var walkedStat = hydratedStat(walked.stat);
  if (!walkedStat || !walkedStat.isDirectory() || walkedStat.isSymbolicLink()) return null;
  if (options.create && targetAbs !== rootAbs) {
    // Creation may have happened in a tree concurrently detached from its
    // public name. A fresh root-anchored walk proves that the configured path
    // still reaches that exact final inode before reporting success.
    var verified = callUnder(rootAbs, targetAbs, {
      action: 'directory-proof', createDirectory: false, allowMissingDirectory: false
    });
    var verifiedStat = verified && verified.ok && verified.exists ? hydratedStat(verified.stat) : null;
    if (!verifiedStat || !sameDirectoryIdentity(walkedStat, verifiedStat)) return null;
    walkedStat = verifiedStat;
  }
  return { path: targetAbs, stat: walkedStat, exists: true };
}

function directoryIdentityCurrent(root, directory, snapshot) {
  var current = realDirectoryUnder(root, directory);
  return !!current && sameDirectoryIdentity(snapshot && snapshot.stat, current.stat);
}

function snapshotDirectory(root, directory, options) {
  var snapshot = realDirectoryUnder(root, directory, options || {});
  return snapshot && snapshot.exists && snapshot.stat ? snapshot : null;
}

function publishNoClobberRegularFileUnder(root, directory, target, bytes, options) {
  options = options || {};
  if (!directChild(directory, target)) return { ok: false, code: 'unsafe-path' };
  bytes = Buffer.isBuffer(bytes) ? bytes : Buffer.from(String(bytes == null ? '' : bytes), 'utf8');
  // Large publication is an explicit capability for closed contracts such as
  // the bounded candidate receipt. Ordinary callers retain the historical
  // 512 KiB ceiling even if they accidentally pass a larger maxBytes value.
  var publicationLimit = options.allowLargePayload === true
    ? LARGE_PUBLICATION_MAX_BYTES : PUBLICATION_MAX_BYTES;
  if (bytes.length > publicationLimit ||
      (Number.isSafeInteger(options.maxBytes) && options.maxBytes >= 0 && bytes.length > options.maxBytes)) {
    return { ok: false, code: 'too-large' };
  }
  var result = callUnder(root, directory, {
    action: 'publish-no-clobber', name: path.basename(target), bytes: bytes.toString('base64'),
    publicationToken: crypto.randomBytes(16).toString('hex'),
    maxBytes: options.maxBytes, mode: options.mode || 0o600,
    createDirectory: options.create !== false, directoryMode: options.directoryMode || 0o700
  });
  if (!result) return { ok: false, code: 'directory-unsafe' };
  if (result.ok) {
    result.stat = hydratedStat(result.stat);
    if (!result.stat || !result.stat.isFile() || result.stat.isSymbolicLink()) {
      return { ok: false, code: 'worker-envelope-invalid' };
    }
  }
  return result;
}

// Crash-safe exact compare-and-swap. The caller must bind the expected full
// stat proof and either its bytes or SHA-256; the worker re-reads and validates
// both before a durable replacement intent can detach that generation.
function compareAndSwapRegularFileUnder(root, directory, target, maxBytes, expected, replacement, options) {
  options = options || {};
  if (!directChild(directory, target) || !Number.isSafeInteger(maxBytes) || maxBytes < 0 ||
      !expected || typeof expected !== 'object' || Array.isArray(expected)) {
    return { ok: false, code: 'invalid-request' };
  }
  var proof = exactStatShape(expected.proof);
  if (!proof || proof.type !== 'file' || proof.nlink !== '1') {
    return { ok: false, code: 'invalid-proof' };
  }
  var expectedKind, expectedBytes, expectedHash;
  if (Buffer.isBuffer(expected.bytes) || typeof expected.bytes === 'string') {
    if (expected.sha256 !== undefined) return { ok: false, code: 'invalid-expected' };
    expectedKind = 'bytes';
    expectedBytes = Buffer.isBuffer(expected.bytes) ? expected.bytes : Buffer.from(expected.bytes, 'utf8');
    if (expectedBytes.length > maxBytes) return { ok: false, code: 'too-large' };
  } else if (typeof expected.sha256 === 'string' && /^sha256:[a-f0-9]{64}$/.test(expected.sha256)) {
    expectedKind = 'sha256';
    expectedHash = expected.sha256;
  } else return { ok: false, code: 'invalid-expected' };
  var replacementBytes = Buffer.isBuffer(replacement)
    ? replacement : Buffer.from(String(replacement == null ? '' : replacement), 'utf8');
  var publicationLimit = options.allowLargePayload === true
    ? LARGE_PUBLICATION_MAX_BYTES : PUBLICATION_MAX_BYTES;
  if (replacementBytes.length > maxBytes || replacementBytes.length > publicationLimit) {
    return { ok: false, code: 'too-large' };
  }
  var request = {
    action: 'compare-and-swap', name: path.basename(target), maxBytes: maxBytes,
    publicationToken: crypto.randomBytes(16).toString('hex'),
    expectedProof: proof, expectedKind: expectedKind,
    bytes: replacementBytes.toString('base64'), mode: options.mode || 0o600,
    allowMissingDirectory: true
  };
  if (expectedKind === 'bytes') request.expectedBytes = expectedBytes.toString('base64');
  else request.expectedHash = expectedHash;
  var result = callUnder(root, directory, request,
    outputLimitForBytes(Math.max(maxBytes, replacementBytes.length)));
  if (!result) return { ok: false, code: 'directory-unsafe' };
  if (result.ok) {
    result.stat = hydratedStat(result.stat);
    if (!result.stat || !result.stat.isFile() || result.stat.isSymbolicLink() || result.stat.nlink !== '1') {
      return { ok: false, code: 'worker-envelope-invalid' };
    }
  }
  return result;
}

function quarantineUnlinkRegularFileUnder(root, directory, file, options) {
  options = options || {};
  if (!directChild(directory, file)) return false;
  var targetProof = null;
  try {
    var targetStat = fs.lstatSync(file, { bigint: true });
    if (!targetStat.isFile() || targetStat.isSymbolicLink()) return false;
    targetProof = exactStatShape(targetStat);
  } catch (targetError) {
    if (!targetError || targetError.code !== 'ENOENT') return false;
  }
  var result = callUnder(root, directory, {
    action: 'quarantine-unlink', name: path.basename(file), allowMissing: !!options.allowMissing,
    allowMissingDirectory: true, targetExpected: targetProof
  });
  return !!(result && result.ok);
}

// Conditional deletion is split into two fail-safe child transactions. The
// first child atomically detaches and reads the exact inode. The predicate runs
// in the parent. A second child then deletes or no-clobber-restores that same
// quarantined inode. If the public ancestor changes between the two, no second
// child receives authority and the private evidence is retained.
function unlinkRegularFileIfUnder(root, directory, file, maxBytes, predicate) {
  if (!directChild(directory, file) || typeof predicate !== 'function' ||
      !Number.isSafeInteger(maxBytes) || maxBytes < 0) return false;
  var targetProof = null;
  try {
    var targetStat = fs.lstatSync(file, { bigint: true });
    if (!targetStat.isFile() || targetStat.isSymbolicLink()) return false;
    targetProof = exactStatShape(targetStat);
  } catch (targetError) {
    if (!targetError || targetError.code !== 'ENOENT') return false;
  }
  var detached = callUnder(root, directory, {
    action: 'quarantine-detach-read', name: path.basename(file), maxBytes: maxBytes,
    allowMissingDirectory: true, targetExpected: targetProof
  }, outputLimitForBytes(maxBytes));
  if (!detached || !detached.ok || !detached.stat || typeof detached.bytes !== 'string') return false;
  var heldStat = hydratedStat(detached.stat);
  var bounded = { bytes: Buffer.from(detached.bytes, 'base64'), stat: heldStat };
  var accepted = false;
  try { accepted = !!heldStat && heldStat.nlink === '1' && predicate(bounded) === true; }
  catch (predicateError) { accepted = false; }
  var finalized = callUnder(root, directory, {
    action: 'quarantine-finalize', name: path.basename(file), quarantine: detached.quarantine,
    operationId: detached.operationId,
    fileExpected: detached.stat, decision: accepted ? 'delete' : 'restore',
    allowMissingDirectory: true
  });
  return !!(accepted && finalized && finalized.ok);
}

function fsyncRegularFileUnder(root, directory, file) {
  if (!directChild(directory, file)) return false;
  var result = callUnder(root, directory, {
    action: 'fsync-file', name: path.basename(file), allowMissingDirectory: true
  });
  return !!(result && result.ok);
}

function fsyncDirectoryUnder(root, directory) {
  var result = callUnder(root, directory, { action: 'fsync-directory', allowMissingDirectory: true });
  return !!(result && result.ok);
}

// Remove only the exact empty direct-child directory generation observed by
// this caller. The worker first detaches that generation to a private random
// name, so rmdir never targets a public name which a racer can replace.
function removeEmptyDirectoryUnder(root, parent, target, expectedProof) {
  if (!directChild(parent, target)) return false;
  var proof = expectedProof === undefined ? null : exactStatShape(expectedProof);
  if (expectedProof !== undefined && (!proof || proof.type !== 'directory')) return false;
  try {
    var observed = fs.lstatSync(target, { bigint: true });
    var observedProof = exactStatShape(observed);
    if (!observedProof || observedProof.type !== 'directory' || observed.isSymbolicLink()) return false;
    if (proof && !sameStat(observedProof, proof)) {
      // Still enter the worker when a durable prior transaction may own the
      // supplied proof; it will preserve this public replacement and reconcile
      // only its exact private capture.
    } else proof = observedProof;
  } catch (targetError) {
    if (!targetError || targetError.code !== 'ENOENT') return false;
  }
  var result = callUnder(root, parent, {
    action: 'remove-empty-directory', name: path.basename(target), targetExpected: proof
  });
  return !!(result && result.ok);
}

function atomicReplaceResult(root, directory, target, options) {
  options = options || {};
  if (!directChild(directory, target)) return { ok: false, code: 'unsafe-path', uncertain: false };
  var supplied = options.initialBytes === undefined ? Buffer.alloc(0) :
    (Buffer.isBuffer(options.initialBytes) ? options.initialBytes : Buffer.from(String(options.initialBytes), 'utf8'));
  if (supplied.length > WORKER_INPUT_MAX / 2) return { ok: false, code: 'too-large', uncertain: false };
  var result = callUnder(root, directory, {
    action: 'atomic-replace', name: path.basename(target), bytes: supplied.toString('base64'),
    preserveExisting: !!options.preserveExisting,
    maxExistingBytes: options.maxExistingBytes,
    maxBytes: options.maxBytes,
    mode: options.mode || 0o600,
    createDirectory: options.create !== false,
    directoryMode: options.directoryMode || 0o700
  });
  if (!result || typeof result.ok !== 'boolean') {
    return { ok: false, code: 'directory-unsafe', uncertain: false };
  }
  if (result.ok) {
    var resultStat = hydratedStat(result.stat);
    var directoryStat = hydratedStat(result.directoryStat);
    if (!resultStat || !resultStat.isFile() || resultStat.isSymbolicLink() || resultStat.nlink !== '1' ||
        !directoryStat || !directoryStat.isDirectory() || directoryStat.isSymbolicLink() ||
        !/^sha256:[a-f0-9]{64}$/.test(String(result.contentHash || '')) ||
        !Number.isSafeInteger(result.contentSize) || result.contentSize < 0 ||
        resultStat.sizeExact !== String(result.contentSize)) {
      return { ok: false, code: 'worker-envelope-invalid', uncertain: false };
    }
    return {
      ok: true, code: 'published', uncertain: false, selfRecognized: false,
      snapshot: { path: directory, stat: directoryStat, exists: true },
      stat: resultStat, contentHash: result.contentHash, contentSize: result.contentSize
    };
  }
  if (result.code !== 'published-unverified') {
    return { ok: false, code: typeof result.code === 'string' ? result.code : 'write-failed', uncertain: false };
  }

  // A rename may have committed even when the worker could not finish its
  // durability proof. Self-recognition is authorized only by the exact inode
  // proof captured after rename plus the worker-bound content hash. A
  // byte-identical replacement with another inode remains uncertain.
  var candidateStat = hydratedStat(result.candidateStat);
  var contentHash = String(result.contentHash || '');
  var contentSize = result.contentSize;
  var uncertain = {
    ok: false, code: 'published-unverified', uncertain: true, selfRecognized: false,
    candidateStat: candidateStat, contentHash: contentHash, contentSize: contentSize
  };
  if (result.uncertain !== true || !candidateStat || !candidateStat.isFile() ||
      candidateStat.isSymbolicLink() || candidateStat.nlink !== '1' ||
      !/^sha256:[a-f0-9]{64}$/.test(contentHash) ||
      !Number.isSafeInteger(contentSize) || contentSize < 0 ||
      candidateStat.sizeExact !== String(contentSize)) return uncertain;
  var recognized = boundedRegularFileUnder(root, directory, target, contentSize);
  if (!recognized || !sameStat(recognized.stat, candidateStat) ||
      recognized.bytes.length !== contentSize || sha256(recognized.bytes) !== contentHash) return uncertain;
  if (!fsyncRegularFileUnder(root, directory, target) || !fsyncDirectoryUnder(root, directory)) return uncertain;
  var verified = boundedRegularFileUnder(root, directory, target, contentSize);
  var snapshot = snapshotDirectory(root, directory);
  if (!verified || !snapshot || !sameStat(verified.stat, candidateStat) ||
      verified.bytes.length !== contentSize || sha256(verified.bytes) !== contentHash) return uncertain;
  return {
    ok: true, code: 'published-self-recognized', uncertain: false, selfRecognized: true,
    snapshot: snapshot, stat: verified.stat, contentHash: contentHash, contentSize: contentSize
  };
}

// Publication happens in the isolated transaction child. The parent opens the
// final path without create/truncate, writes nothing, and accepts the descriptor
// only when fstat + lstat + a fresh root-anchored directory walk all identify the
// exact inode returned by the child. O_APPEND restores the descriptor-at-end
// contract across the process boundary.
function openAtomicReplaceRegularFile(root, directory, target, options) {
  var published = atomicReplaceResult(root, directory, target, options || {});
  if (!published || !published.ok || !published.stat) return null;
  var fd;
  try {
    fd = fs.openSync(target, fs.constants.O_WRONLY | fs.constants.O_APPEND | (fs.constants.O_NOFOLLOW || 0));
    var opened = fs.fstatSync(fd, { bigint: true });
    var live = fs.lstatSync(target, { bigint: true });
    if (!sameStat(opened, live) || !sameFileIdentity(opened, published.stat) || opened.nlink !== 1n ||
        !sameFileIdentity(live, published.stat) || live.nlink !== 1n ||
        !directoryIdentityCurrent(root, directory, published.snapshot)) throw new Error('published descriptor identity changed');
    return fd;
  } catch (error) {
    if (fd !== undefined) try { fs.closeSync(fd); } catch (closeError) {}
    return null;
  }
}

function atomicReplaceRegularFile(root, directory, target, bytes, options) {
  return atomicReplaceRegularFileResult(root, directory, target, bytes, options).ok;
}

function atomicReplaceRegularFileResult(root, directory, target, bytes, options) {
  options = Object.assign({}, options || {}, { initialBytes: bytes });
  return atomicReplaceResult(root, directory, target, options);
}

function appendBoundedRegularFileUnder(root, directory, target, bytes, options) {
  options = options || {};
  if (!directChild(directory, target)) return { ok: false, code: 'unsafe-path' };
  bytes = Buffer.isBuffer(bytes) ? bytes : Buffer.from(String(bytes == null ? '' : bytes), 'utf8');
  if (!Number.isSafeInteger(options.maxBytes) || options.maxBytes < 0 ||
      !Number.isSafeInteger(options.maxAppendBytes) || options.maxAppendBytes < 0 ||
      bytes.length > options.maxAppendBytes || bytes.length > options.maxBytes) {
    return { ok: false, code: 'too-large' };
  }
  var expectedProof = options.expectedProof === undefined ? undefined : exactStatShape(options.expectedProof);
  if (expectedProof !== undefined && (!expectedProof || expectedProof.type !== 'file' || expectedProof.nlink !== '1')) {
    return { ok: false, code: 'expected-proof-invalid' };
  }
  var request = {
    action: 'append-bounded', name: path.basename(target), bytes: bytes.toString('base64'),
    publicationToken: crypto.randomBytes(16).toString('hex'),
    maxBytes: options.maxBytes, maxAppendBytes: options.maxAppendBytes,
    mode: options.mode || 0o600, createDirectory: options.create !== false,
    directoryMode: options.directoryMode || 0o700
  };
  if (expectedProof !== undefined) request.expectedProof = expectedProof;
  if (options.expectMissing === true) request.expectMissing = true;
  if (expectedProof !== undefined && request.expectMissing) return { ok: false, code: 'expected-state-invalid' };
  var result = callUnder(root, directory, request);
  if (!result) return { ok: false, code: 'directory-unsafe' };
  if (result.ok) {
    var acknowledged = callUnder(root, directory, {
      action: 'append-ack', name: path.basename(target),
      publicationToken: request.publicationToken, targetExpected: result.stat,
      allowMissingDirectory: true
    });
    if (!acknowledged || !acknowledged.ok) result.cleanupPending = true;
    result.stat = hydratedStat(result.stat);
    if (!result.stat || !result.stat.isFile() || result.stat.isSymbolicLink()) {
      return { ok: false, code: 'worker-envelope-invalid' };
    }
  }
  return result;
}

function boundedRegularFileUnder(root, directory, file, maxBytes) {
  if (!directChild(directory, file) || !Number.isSafeInteger(maxBytes) || maxBytes < 0) return null;
  var result = callUnder(root, directory, {
    action: 'bounded-read', name: path.basename(file), maxBytes: maxBytes,
    allowMissingDirectory: true
  }, outputLimitForBytes(maxBytes));
  if (!result || !result.ok || typeof result.bytes !== 'string' || !result.stat) return null;
  var stat = hydratedStat(result.stat);
  return stat && stat.isFile() && !stat.isSymbolicLink()
    ? { bytes: Buffer.from(result.bytes, 'base64'), stat: stat } : null;
}

function statRegularFileUnder(root, directory, file) {
  if (!directChild(directory, file)) return null;
  var result = callUnder(root, directory, {
    action: 'stat-file', name: path.basename(file), allowMissingDirectory: true
  });
  if (!result || !result.ok || !result.stat) return null;
  var stat = hydratedStat(result.stat);
  return stat && stat.isFile() && !stat.isSymbolicLink() ? stat : null;
}

function inspectEntryUnder(root, directory, file) {
  if (!directChild(directory, file)) return { status: 'unsafe' };
  var result = callUnder(root, directory, {
    action: 'inspect-entry', name: path.basename(file), allowMissingDirectory: true
  });
  if (!result || !result.ok) return { status: 'unsafe' };
  // The anchored worker reports a missing parent directory as `exists:false`
  // because no final entry can be inspected. Normalize that proof to the same
  // public absence shape as a missing leaf. Callers must not mistake the lack
  // of a directory for hypothetical WAL files at deterministic names.
  if (result.exists === false) return { status: 'missing' };
  if (result.status === 'present') {
    result.stat = hydratedStat(result.stat);
    if (!result.stat) return { status: 'unsafe' };
  }
  return result;
}

function unlinkRegularFileUnder(root, directory, file, options) {
  return quarantineUnlinkRegularFileUnder(root, directory, file, options);
}

function matchingUnlinkEnvelope(ok, code, outcome, committed, alreadyMissing, uncertain) {
  return {
    ok: ok, code: code, outcome: outcome, committed: committed,
    alreadyMissing: alreadyMissing, uncertain: uncertain
  };
}

// Remove only a sole regular-file generation whose bounded bytes (or SHA-256)
// match the caller's expectation, and preserve the worker's terminal outcome.
// A supplied proof remains the request authority even if the public name has
// since disappeared or been replaced: this lets a retry recognize an exact
// durable delete WAL without ever granting authority over the new generation.
function unlinkRegularFileMatchingResultUnder(root, directory, file, maxBytes, expected) {
  var invalid = function (code) {
    return matchingUnlinkEnvelope(false, code || 'invalid-request', 'not-committed',
      false, false, false);
  };
  if (!directChild(directory, file) || !Number.isSafeInteger(maxBytes) || maxBytes < 0) {
    return invalid('invalid-request');
  }
  var matchKind, expectedBytes, expectedHash, suppliedProof = null;
  if (Buffer.isBuffer(expected) || typeof expected === 'string') {
    matchKind = 'bytes';
    expectedBytes = Buffer.isBuffer(expected) ? expected : Buffer.from(expected, 'utf8');
  } else if (expected && typeof expected === 'object' && !Array.isArray(expected)) {
    if (expected.proof !== undefined) {
      suppliedProof = exactStatShape(expected.proof);
      if (!suppliedProof || suppliedProof.type !== 'file' || suppliedProof.nlink !== '1') {
        return invalid('invalid-proof');
      }
    }
    if (Buffer.isBuffer(expected.bytes) || typeof expected.bytes === 'string') {
      if (expected.sha256 !== undefined) return invalid('invalid-expected');
      matchKind = 'bytes';
      expectedBytes = Buffer.isBuffer(expected.bytes) ? expected.bytes : Buffer.from(expected.bytes, 'utf8');
    } else if (typeof expected.sha256 === 'string' && /^sha256:[a-f0-9]{64}$/.test(expected.sha256)) {
      matchKind = 'sha256';
      expectedHash = expected.sha256;
    } else return invalid('invalid-expected');
  } else return invalid('invalid-expected');
  if (matchKind === 'bytes' && expectedBytes.length > maxBytes) return invalid('too-large');

  var targetExpected = suppliedProof;
  var initiallyMissing = false;
  if (!targetExpected) {
    try {
      var observed = fs.lstatSync(file, { bigint: true });
      if (!observed.isFile() || observed.isSymbolicLink() || observed.nlink !== 1n) {
        return invalid('target-unsafe');
      }
      targetExpected = exactStatShape(observed);
      if (!targetExpected) return invalid('target-unsafe');
    } catch (targetError) {
      if (!targetError || targetError.code !== 'ENOENT') return invalid('target-unsafe');
      initiallyMissing = true;
    }
  } else {
    try { fs.lstatSync(file, { bigint: true }); }
    catch (proofTargetError) {
      if (!proofTargetError || proofTargetError.code !== 'ENOENT') return invalid('target-unsafe');
      initiallyMissing = true;
    }
  }
  var request = {
    action: 'quarantine-unlink-matching', name: path.basename(file), maxBytes: maxBytes,
    matchKind: matchKind, targetExpected: targetExpected, allowMissingDirectory: true
  };
  if (matchKind === 'bytes') request.expectedBytes = expectedBytes.toString('base64');
  else request.expectedHash = expectedHash;
  var result = callUnder(root, directory, request, outputLimitForBytes(maxBytes));
  if (!result || typeof result !== 'object') return invalid('worker-envelope-invalid');
  var terminal = result.outcome || result.code;
  if (result.ok === true && terminal === 'deleted') {
    return matchingUnlinkEnvelope(true, 'deleted', 'deleted', true, false, false);
  }
  if (result.ok === true && result.code === 'already-missing') {
    return matchingUnlinkEnvelope(true, 'already-missing', 'already-missing', false, true, false);
  }
  if (result.ok !== false) return invalid('worker-envelope-invalid');

  // Once a proof-bound request reaches the worker, absence without a matching
  // WAL cannot prove who removed that exact generation. Preserve the
  // idempotent boolean result via alreadyMissing, but make the structured
  // contract explicitly non-committed and uncertain.
  if (suppliedProof && initiallyMissing && result.code === 'target-proof-mismatch') {
    var after = inspectEntryUnder(root, directory, file);
    if (after && after.status === 'missing') {
      return matchingUnlinkEnvelope(false, 'delete-commit-unproven', 'not-committed',
        false, true, true);
    }
  }
  return matchingUnlinkEnvelope(false,
    typeof result.code === 'string' && result.code ? result.code : 'guard-operation-failed',
    'not-committed', false, false, false);
}

// Boolean convenience facade: absence remains an idempotent success,
// while callers that need to settle ownership ambiguity use the result API.
function unlinkRegularFileMatchingUnder(root, directory, file, maxBytes, expected) {
  var result = unlinkRegularFileMatchingResultUnder(root, directory, file, maxBytes, expected);
  return !!(result && (result.ok || result.alreadyMissing));
}

function boundedDirectoryNamesUnder(root, directory, maxEntries) {
  if (!Number.isSafeInteger(maxEntries) || maxEntries < 0) return { ok: false, names: [] };
  var result = callUnder(root, directory, {
    action: 'directory-names', maxEntries: maxEntries, allowMissingDirectory: true
  }, Math.min(WORKER_OUTPUT_MAX, Math.max(1024 * 1024, maxEntries * 1024)));
  if (result && result.ok && result.exists === false) {
    return { ok: true, exists: false, names: [] };
  }
  if (!result || !result.ok || !Array.isArray(result.names)) {
    return { ok: false, code: result && result.code || 'directory-unsafe', names: [] };
  }
  return result;
}

// Return at most one lexicographically ordered page while scanning under a
// separate explicit cap. The opaque cursor is bound to the directory's exact
// dev/ino/mode identity but deliberately survives metadata changes caused by
// deleting a prior page, so bounded cleanup can make progress without ever
// materializing the complete directory listing.
function boundedDirectoryPageUnder(root, directory, options) {
  options = options || {};
  var pageSize = options.pageSize;
  var maxScanEntries = options.maxScanEntries;
  var cursor = options.cursor === undefined ? null : options.cursor;
  var failure = function (code, scanned) {
    return { ok: false, exists: true, code: code, names: [], nextCursor: null,
      done: false, scanned: Number.isSafeInteger(scanned) && scanned >= 0 ? scanned : 0 };
  };
  if (!Number.isSafeInteger(pageSize) || pageSize < 1 || pageSize > 4096 ||
      !Number.isSafeInteger(maxScanEntries) || maxScanEntries < 0 || maxScanEntries > 100000 ||
      (cursor !== null && (typeof cursor !== 'string' || !cursor || cursor.length > 4096))) {
    return failure('invalid-request', 0);
  }
  var result = callUnder(root, directory, {
    action: 'directory-page', pageSize: pageSize, maxScanEntries: maxScanEntries,
    cursor: cursor, allowMissingDirectory: true
  }, Math.min(WORKER_OUTPUT_MAX, Math.max(1024 * 1024, pageSize * 1024)));
  if (result && result.ok && result.exists === false) {
    return { ok: true, exists: false, names: [], nextCursor: null, done: true, scanned: 0 };
  }
  if (!result || result.ok !== true) {
    return failure(result && typeof result.code === 'string' ? result.code : 'directory-unsafe',
      result && result.scanned);
  }
  var directoryStat = hydratedStat(result.directoryStat);
  if (!directoryStat || !directoryStat.isDirectory() || directoryStat.isSymbolicLink() ||
      !Array.isArray(result.names) || result.names.length > pageSize ||
      !Number.isSafeInteger(result.scanned) || result.scanned < result.names.length ||
      typeof result.done !== 'boolean' ||
      (result.done ? result.nextCursor !== null :
        (typeof result.nextCursor !== 'string' || !result.nextCursor || result.nextCursor.length > 4096)) ||
      (!result.done && result.names.length === 0) ||
      !result.names.every(function (name, index) {
        return typeof name === 'string' && !!name && name.indexOf('/') < 0 && name.indexOf('\\') < 0 &&
          (index === 0 || Buffer.compare(Buffer.from(result.names[index - 1], 'utf8'),
            Buffer.from(name, 'utf8')) < 0);
      })) return failure('worker-envelope-invalid', 0);
  return {
    ok: true, exists: true, names: result.names.slice(), nextCursor: result.nextCursor,
    done: result.done, scanned: result.scanned, directoryStat: directoryStat
  };
}

// Reconcile crash-safe private file-guard transactions without exposing or
// parsing their WAL in callers. Both directory entries and transaction groups
// are independently capped. Valid exact transactions advance; corrupt,
// foreign, and orphan evidence is preserved and reported only as code counts.
function reconcileGuardTransactionsUnder(root, directory, options) {
  options = options || {};
  var maxEntries = options.maxEntries;
  var maxTransactions = options.maxTransactions;
  var failure = function (code) {
    return { ok: false, exists: true, code: code, scanned: 0,
      transactions: 0, reconciled: 0, pending: 0, codes: {} };
  };
  if (!Number.isSafeInteger(maxEntries) || maxEntries < 0 ||
      !Number.isSafeInteger(maxTransactions) || maxTransactions < 0) return failure('invalid-request');
  var result = callUnder(root, directory, {
    action: 'reconcile-guard-transactions', maxEntries: maxEntries,
    maxTransactions: maxTransactions, allowMissingDirectory: true
  });
  if (result && result.ok && result.exists === false) {
    return { ok: true, exists: false, scanned: 0, transactions: 0,
      reconciled: 0, pending: 0, codes: {} };
  }
  if (!result || typeof result.ok !== 'boolean' ||
      !['scanned', 'transactions', 'reconciled', 'pending'].every(function (field) {
        return Number.isSafeInteger(result[field]) && result[field] >= 0;
      }) || !result.codes || typeof result.codes !== 'object' || Array.isArray(result.codes) ||
      !Object.keys(result.codes).every(function (code) {
        return /^[a-z0-9-]+$/.test(code) && Number.isSafeInteger(result.codes[code]) && result.codes[code] > 0;
      })) return failure('worker-envelope-invalid');
  var summary = {
    ok: result.ok, exists: true,
    scanned: result.scanned, transactions: result.transactions,
    reconciled: result.reconciled, pending: result.pending, codes: Object.assign({}, result.codes)
  };
  if (typeof result.code === 'string') summary.code = result.code;
  return summary;
}

function tailRegularFileUnder(root, directory, file, maxBytes) {
  if (!directChild(directory, file) || !Number.isSafeInteger(maxBytes) || maxBytes < 0) return null;
  var result = callUnder(root, directory, {
    action: 'tail', name: path.basename(file), maxBytes: maxBytes,
    allowMissingDirectory: true
  }, outputLimitForBytes(maxBytes));
  if (!result || !result.ok || typeof result.bytes !== 'string') return null;
  return Buffer.from(result.bytes, 'base64');
}

function transferFileNoClobberSameDirectoryUnder(root, directory, source, target, expected) {
  if (!directChild(directory, source) || !directChild(directory, target) || source === target) return false;
  var sourceExpected = null;
  if (expected !== undefined) {
    if (!expected || typeof expected !== 'object' || Array.isArray(expected)) return false;
    sourceExpected = exactStatShape(expected.proof);
    if (!sourceExpected || sourceExpected.type !== 'file' || sourceExpected.nlink !== '1') return false;
  } else {
    try {
      var observed = fs.lstatSync(source, { bigint: true });
      // nlink=2 and even a foreign entry type can be a recovery state of a
      // durable transaction.  Only a sole regular file grants authority to
      // start; every other shape enters the worker with no new authority so an
      // existing WAL may reconcile it safely.
      if (observed.isFile() && !observed.isSymbolicLink() && observed.nlink === 1n) {
        sourceExpected = exactStatShape(observed);
      }
    } catch (sourceError) {
      // A missing source may be the durable post-detach state of an earlier
      // transfer.  Still enter the worker so it can reconcile its deterministic
      // WAL; without a WAL it will fail closed.
      if (!sourceError || sourceError.code !== 'ENOENT') return false;
    }
  }
  var result = callUnder(root, directory, {
    action: 'transfer-no-clobber', source: path.basename(source), target: path.basename(target),
    sourceExpected: sourceExpected, allowMissingDirectory: true
  });
  return !!(result && result.ok);
}

// Exact writer-lease WAL primitives. These deliberately expose only the four
// operations needed by the lease transaction lattice; every call is executed
// by the inode-pinned worker and may be bound to one bigint directory proof.
function writerLeaseDirectoryProofUnder(root, directory, options) {
  options = options || {};
  var result = callWriterUnder(root, directory, {
    action: 'writer-directory-proof', createDirectory: options.create === true,
    directoryMode: options.mode || 0o700, allowMissingDirectory: options.allowMissing === true
  });
  return result && result.ok && result.proof ? result.proof : null;
}

function writerLeaseReadUnder(root, directory, file, maxBytes, directoryProof) {
  if (!directChild(directory, file) || !Number.isSafeInteger(maxBytes) || maxBytes < 0 || !directoryProof) return null;
  var result = callWriterUnder(root, directory, {
    action: 'writer-read', name: path.basename(file), maxBytes: maxBytes,
    writerDirectoryProof: directoryProof, allowMissingDirectory: true
  }, outputLimitForBytes(maxBytes));
  if (!result || !result.ok || typeof result.bytes !== 'string' || !result.proof) return null;
  return { bytes: Buffer.from(result.bytes, 'base64'), proof: result.proof };
}

function writerLeaseNamesUnder(root, directory, maxEntries, directoryProof) {
  if (!Number.isSafeInteger(maxEntries) || maxEntries < 0 || !directoryProof) return { ok: false, names: [] };
  var result = callWriterUnder(root, directory, {
    action: 'directory-names', maxEntries: maxEntries, writerDirectoryProof: directoryProof,
    allowMissingDirectory: true
  }, Math.min(WORKER_OUTPUT_MAX, Math.max(1024 * 1024, maxEntries * 1024)));
  return result && result.ok && Array.isArray(result.names) ? result : { ok: false, code: result && result.code || 'directory-unsafe', names: [] };
}

function writerLeaseStageUnder(root, directory, file, bytes, options) {
  options = options || {};
  if (!directChild(directory, file) || !options.directoryProof) return { ok: false, code: 'invalid-request' };
  bytes = Buffer.isBuffer(bytes) ? bytes : Buffer.from(String(bytes == null ? '' : bytes), 'utf8');
  var maxBytes = Number.isSafeInteger(options.maxBytes) ? options.maxBytes : bytes.length;
  if (maxBytes < 0 || bytes.length > maxBytes) return { ok: false, code: 'too-large' };
  var result = callWriterUnder(root, directory, {
    action: 'writer-stage', name: path.basename(file), bytes: bytes.toString('base64'), maxBytes: maxBytes,
    mode: options.mode || 0o600, writerDirectoryProof: options.directoryProof,
    allowMissingDirectory: true
  }, outputLimitForBytes(maxBytes));
  return result || { ok: false, code: 'directory-unsafe' };
}

function writerLeaseLinkUnder(root, directory, source, target, options) {
  options = options || {};
  if (!directChild(directory, source) || !directChild(directory, target) || !options.directoryProof || !options.sourceProof) {
    return { ok: false, code: 'invalid-request' };
  }
  if (options.removeSource === true && !directChild(directory, options.capture)) return { ok: false, code: 'invalid-request' };
  var result = callWriterUnder(root, directory, {
    action: 'writer-link', source: path.basename(source), target: path.basename(target),
    sourceProof: options.sourceProof, removeSource: options.removeSource === true,
    capture: options.capture ? path.basename(options.capture) : undefined,
    maxBytes: options.maxBytes, writerDirectoryProof: options.directoryProof,
    allowMissingDirectory: true,
    testForeignBytes: options.testForeignBytes,
    testReplaceBeforeCaptureBytes: options.testReplaceBeforeCaptureBytes
  }, outputLimitForBytes(options.maxBytes));
  return result || { ok: false, code: 'directory-unsafe' };
}

function writerLeaseDeleteUnder(root, directory, file, quarantine, options) {
  options = options || {};
  if (!directChild(directory, file) || !directChild(directory, quarantine) || !directChild(directory, options.capture) ||
      !options.directoryProof || !options.proof) {
    return { ok: false, code: 'invalid-request' };
  }
  var result = callWriterUnder(root, directory, {
    action: 'writer-delete', name: path.basename(file), quarantine: path.basename(quarantine),
    capture: path.basename(options.capture), proof: options.proof, maxBytes: options.maxBytes,
    writerDirectoryProof: options.directoryProof, allowMissingDirectory: true,
    testForeignBytes: options.testForeignBytes,
    testReplaceBeforeCaptureBytes: options.testReplaceBeforeCaptureBytes
  });
  return result || { ok: false, code: 'directory-unsafe' };
}

function boundedRegularFile(file, maxBytes) {
  var before, fd;
  try {
    before = fs.lstatSync(file, { bigint: true });
    if (!before.isFile() || before.isSymbolicLink() || before.nlink !== 1n || !Number.isSafeInteger(maxBytes) ||
        before.size < 0n || before.size > BigInt(maxBytes)) return null;
    fd = fs.openSync(file, fs.constants.O_RDONLY | (fs.constants.O_NOFOLLOW || 0));
    var opened = fs.fstatSync(fd, { bigint: true });
    if (!opened.isFile() || !sameStat(before, opened)) return null;
    var bytes = Buffer.allocUnsafe(Number(opened.size));
    var offset = 0;
    while (offset < bytes.length) {
      var count = fs.readSync(fd, bytes, offset, bytes.length - offset, offset);
      if (!count) return null;
      offset += count;
    }
    var afterFd = fs.fstatSync(fd, { bigint: true });
    var afterPath = fs.lstatSync(file, { bigint: true });
    if (!sameStat(opened, afterFd) || !sameStat(opened, afterPath)) return null;
    return { bytes: bytes, stat: hydratedStat(exactStatShape(afterPath)) };
  } catch (error) { return null; }
  finally { if (fd !== undefined) try { fs.closeSync(fd); } catch (closeError) {} }
}

function realFileUnder(root, candidate, options) {
  options = options || {};
  if (typeof root !== 'string' || !root || typeof candidate !== 'string' || !candidate) return null;
  var rootAbs = path.resolve(root);
  var targetAbs = path.resolve(candidate);
  if (root !== rootAbs || candidate !== targetAbs || !isUnder(rootAbs, targetAbs) || targetAbs === rootAbs) return null;
  var directory = path.dirname(targetAbs);
  var stat = statRegularFileUnder(rootAbs, directory, targetAbs);
  if (!stat || (Number.isSafeInteger(options.maxBytes) && stat.size > options.maxBytes)) return null;
  return { path: targetAbs, stat: stat };
}

module.exports = {
  isUnder: isUnder,
  guardTransactionEvidenceForTarget: guardTransactionEvidenceForTarget,
  classifyGuardTransactionEvidenceForTarget: classifyGuardTransactionEvidenceForTarget,
  realFileUnder: realFileUnder,
  realDirectoryUnder: realDirectoryUnder,
  boundedRegularFile: boundedRegularFile,
  boundedRegularFileUnder: boundedRegularFileUnder,
  statRegularFileUnder: statRegularFileUnder,
  inspectEntryUnder: inspectEntryUnder,
  unlinkRegularFileUnder: unlinkRegularFileUnder,
  unlinkRegularFileMatchingUnder: unlinkRegularFileMatchingUnder,
  unlinkRegularFileMatchingResultUnder: unlinkRegularFileMatchingResultUnder,
  openAtomicReplaceRegularFile: openAtomicReplaceRegularFile,
  atomicReplaceRegularFile: atomicReplaceRegularFile,
  atomicReplaceRegularFileResult: atomicReplaceRegularFileResult,
  appendBoundedRegularFileUnder: appendBoundedRegularFileUnder,
  publishNoClobberRegularFileUnder: publishNoClobberRegularFileUnder,
  compareAndSwapRegularFileUnder: compareAndSwapRegularFileUnder,
  unlinkRegularFileIfUnder: unlinkRegularFileIfUnder,
  fsyncRegularFileUnder: fsyncRegularFileUnder,
  fsyncDirectoryUnder: fsyncDirectoryUnder,
  removeEmptyDirectoryUnder: removeEmptyDirectoryUnder,
  sameDirectoryIdentity: sameDirectoryIdentity,
  boundedDirectoryNamesUnder: boundedDirectoryNamesUnder,
  boundedDirectoryPageUnder: boundedDirectoryPageUnder,
  reconcileGuardTransactionsUnder: reconcileGuardTransactionsUnder,
  tailRegularFileUnder: tailRegularFileUnder,
  transferFileNoClobberSameDirectoryUnder: transferFileNoClobberSameDirectoryUnder
  ,writerLeaseDirectoryProofUnder: writerLeaseDirectoryProofUnder
  ,writerLeaseReadUnder: writerLeaseReadUnder
  ,writerLeaseNamesUnder: writerLeaseNamesUnder
  ,writerLeaseStageUnder: writerLeaseStageUnder
  ,writerLeaseLinkUnder: writerLeaseLinkUnder
  ,writerLeaseDeleteUnder: writerLeaseDeleteUnder
};
