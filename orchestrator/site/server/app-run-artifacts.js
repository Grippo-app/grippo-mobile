'use strict';

var fs = require('fs');
var path = require('path');
var crypto = require('crypto');
var paths = require('./paths');
var storage = require('./app-run-storage');
var iosRunner = require('./ios-runner');

var HASH_RE = /^sha256:[a-f0-9]{64}$/;
var APP_ID_RE = /^[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z][A-Za-z0-9_]*)+$/;
var ARTIFACT_FIELDS = [
  'schemaVersion', 'artifactId', 'platform', 'variantId', 'appProjectSourceRevision',
  'buildInputFingerprint', 'runConfigHash', 'applicationId', 'toolchainFingerprint',
  'worktreeId', 'executionRunId', 'candidateTree',
  'artifactRelativePath', 'artifactHash', 'artifactSize', 'targetArchitectures', 'builtAt'
];

function exactKeys(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}

function safeInstant(value) {
  return typeof value === 'string' && Number.isFinite(Date.parse(value)) &&
    new Date(value).toISOString() === value;
}

function safeRelative(value) {
  if (typeof value !== 'string' || !value || value.length > 500 || path.posix.isAbsolute(value) ||
      value.indexOf('\\') >= 0 || value.indexOf('\0') >= 0) return false;
  var normalized = path.posix.normalize(value);
  return normalized === value && normalized !== '..' && normalized.indexOf('../') !== 0;
}

function validate(value) {
  var controlScoped = value && value.worktreeId === null && value.executionRunId === null &&
    value.candidateTree === null;
  var executionScoped = value && /^wt-[a-f0-9]{32}$/.test(String(value.worktreeId || '')) &&
    /^[0-9]{1,16}-[a-z0-9]{1,32}$/.test(String(value.executionRunId || '')) &&
    /^[a-f0-9]{40}$/.test(String(value.candidateTree || ''));
  if (!exactKeys(value, ARTIFACT_FIELDS) || value.schemaVersion !== 1 ||
      !/^artifact-[a-f0-9]{36}$/.test(String(value.artifactId || '')) ||
      ['android', 'ios'].indexOf(value.platform) < 0 ||
      !/^[a-z][a-z0-9-]{0,31}$/.test(String(value.variantId || '')) ||
      !HASH_RE.test(String(value.appProjectSourceRevision || '')) ||
      !HASH_RE.test(String(value.buildInputFingerprint || '')) ||
      value.buildInputFingerprint !== value.appProjectSourceRevision ||
      !HASH_RE.test(String(value.runConfigHash || '')) ||
      !HASH_RE.test(String(value.toolchainFingerprint || '')) ||
      !HASH_RE.test(String(value.artifactHash || '')) ||
      (!controlScoped && !executionScoped) ||
      !safeRelative(value.artifactRelativePath) ||
      !Number.isSafeInteger(value.artifactSize) || value.artifactSize < 1 ||
      !Array.isArray(value.targetArchitectures) || value.targetArchitectures.length > 20 ||
      value.targetArchitectures.some(function (x) { return typeof x !== 'string' || !/^[A-Za-z0-9._-]{1,80}$/.test(x); }) ||
      typeof value.applicationId !== 'string' || value.applicationId.length > 240 ||
      !APP_ID_RE.test(value.applicationId) ||
      !safeInstant(value.builtAt)) return 'artifact manifest is invalid';
  return null;
}

function toolchainFingerprint(platform, tools) {
  var identities = Object.keys(tools || {}).filter(function (key) {
    return key !== 'sdkRoot' && key !== 'resolutionError' && typeof tools[key] === 'string';
  }).sort().map(function (key) {
    var file = tools[key], stat;
    try {
      var real = fs.realpathSync(file);
      stat = fs.lstatSync(real);
      if (!stat.isFile() || stat.isSymbolicLink()) return key + ':unsafe';
      return [
        key, real, stat.dev, stat.ino, stat.mode, stat.size,
        Math.floor(stat.mtimeMs), Math.floor(stat.ctimeMs)
      ].join(':');
    }
    catch (_) { return key + ':missing'; }
  });
  return 'sha256:' + crypto.createHash('sha256').update(platform + '\0' + identities.join('\0')).digest('hex');
}

function create(input) {
  var id = storage.randomId('artifact');
  var execution = input.execution === undefined ? null : input.execution;
  if (execution !== null && (!exactKeys(execution,
      ['worktreeId', 'runId', 'executionRoot', 'candidateTree']) ||
      !/^wt-[a-f0-9]{32}$/.test(String(execution.worktreeId || '')) ||
      !/^[0-9]{1,16}-[a-z0-9]{1,32}$/.test(String(execution.runId || '')) ||
      !path.isAbsolute(String(execution.executionRoot || '')) ||
      !/^[a-f0-9]{40}$/.test(String(execution.candidateTree || '')))) {
    throw new Error('artifact execution scope is invalid');
  }
  var scopeRoot = execution ? execution.executionRoot : paths.PROJECT_ROOT;
  var scopeReal = fs.realpathSync(scopeRoot);
  var artifactReal = fs.realpathSync(input.artifact.path);
  var relative = path.relative(scopeReal, artifactReal).split(path.sep).join('/');
  if (!safeRelative(relative)) throw new Error('artifact is outside its exact product root');
  var value = {
    schemaVersion: 1,
    artifactId: id,
    platform: input.platform,
    variantId: input.variantId,
    appProjectSourceRevision: input.sourceRevision,
    buildInputFingerprint: input.sourceRevision,
    runConfigHash: input.runConfigHash,
    applicationId: input.artifact.applicationId,
    toolchainFingerprint: toolchainFingerprint(input.platform, input.tools),
    worktreeId: execution ? execution.worktreeId : null,
    executionRunId: execution ? execution.runId : null,
    candidateTree: execution ? execution.candidateTree : null,
    artifactRelativePath: relative,
    artifactHash: input.artifact.hash,
    artifactSize: input.artifact.size,
    targetArchitectures: input.artifact.targetArchitectures || [],
    builtAt: new Date().toISOString()
  };
  var issue = validate(value);
  if (issue) throw new Error(issue);
  storage.writeJson(paths.APP_RUN_ARTIFACTS_DIR, id, value);
  var retentionIssues = prune(id);
  if (retentionIssues.length) {
    var retentionError = new Error('app-run artifact retention could not settle exact owned files');
    retentionError.code = 'artifact-invalid';
    throw retentionError;
  }
  return value;
}

function read(id) {
  var value = storage.readJson(paths.APP_RUN_ARTIFACTS_DIR, id);
  if (!value) return null;
  var issue = validate(value);
  if (issue) throw new Error(issue);
  return value;
}

function artifactPath(manifest, expected) {
  var executionScoped = manifest.worktreeId !== null;
  if (executionScoped && (!expected || expected.worktreeId !== manifest.worktreeId ||
      expected.executionRunId !== manifest.executionRunId ||
      expected.candidateTree !== manifest.candidateTree ||
      !path.isAbsolute(String(expected.executionRoot || '')))) {
    var mismatch = new Error('artifact execution scope does not match the current job');
    mismatch.code = 'artifact-scope-mismatch';
    throw mismatch;
  }
  var scopeRoot = executionScoped ? expected.executionRoot : paths.PROJECT_ROOT;
  var candidate = path.resolve(scopeRoot, manifest.artifactRelativePath.split('/').join(path.sep));
  var rel = path.relative(scopeRoot, candidate);
  if (rel === '..' || rel.indexOf('..' + path.sep) === 0 || path.isAbsolute(rel)) {
    throw new Error('artifact path escaped its exact product root');
  }
  var scopeReal = fs.realpathSync(scopeRoot);
  var canonicalExpected = path.resolve(scopeReal,
    path.relative(path.resolve(scopeRoot), candidate));
  var candidateReal = fs.realpathSync(candidate);
  if (candidateReal !== canonicalExpected) throw new Error('artifact path contains an untrusted symlink ancestor');
  return candidate;
}

function containedBy(root, candidate) {
  var rootReal = fs.realpathSync(root);
  var candidateReal = fs.realpathSync(candidate);
  var rel = path.relative(rootReal, candidateReal);
  return rel === '' || (rel !== '..' && rel.indexOf('..' + path.sep) !== 0 && !path.isAbsolute(rel));
}

function architectureCompatible(platform, manifestArchitectures, expected) {
  if (platform === 'ios') return !expected.runtimeKind || manifestArchitectures.indexOf(expected.runtimeKind) >= 0;
  if (!expected.targetArchitecture || manifestArchitectures.length === 0) return true;
  var aliases = {
    arm64: ['arm64', 'arm64-v8a'],
    'arm64-v8a': ['arm64-v8a', 'arm64'],
    x86_64: ['x86_64'],
    x86: ['x86']
  };
  var accepted = aliases[expected.targetArchitecture] || [expected.targetArchitecture];
  return manifestArchitectures.some(function (value) { return accepted.indexOf(value) >= 0; });
}

function hashRegularFile(file, expectedSize) {
  var before = fs.lstatSync(file);
  if (!before.isFile() || before.isSymbolicLink() || String(before.nlink) !== '1') {
    throw new Error('artifact is unsafe');
  }
  if (before.size !== expectedSize) {
    var sizeMismatch = new Error('artifact size does not match its manifest');
    sizeMismatch.code = 'artifact-hash-mismatch';
    throw sizeMismatch;
  }
  var flags = fs.constants.O_RDONLY;
  if (fs.constants.O_NOFOLLOW) flags |= fs.constants.O_NOFOLLOW;
  var fd;
  try {
    fd = fs.openSync(file, flags);
    var opened = fs.fstatSync(fd);
    if (!opened.isFile() || String(opened.nlink) !== '1' ||
        String(opened.dev) !== String(before.dev) || String(opened.ino) !== String(before.ino) ||
        opened.size !== before.size || Number(opened.mtimeMs) !== Number(before.mtimeMs) ||
        Number(opened.ctimeMs) !== Number(before.ctimeMs)) throw new Error('artifact changed before hashing');
    var digest = crypto.createHash('sha256'), buffer = Buffer.alloc(1024 * 1024), offset = 0;
    while (offset < opened.size) {
      var count = fs.readSync(fd, buffer, 0, Math.min(buffer.length, opened.size - offset), offset);
      if (!count) throw new Error('artifact changed while hashing');
      digest.update(buffer.subarray(0, count)); offset += count;
    }
    var final = fs.fstatSync(fd);
    if (String(final.dev) !== String(opened.dev) || String(final.ino) !== String(opened.ino) ||
        final.size !== opened.size || Number(final.mtimeMs) !== Number(opened.mtimeMs) ||
        Number(final.ctimeMs) !== Number(opened.ctimeMs)) throw new Error('artifact changed while hashing');
    return 'sha256:' + digest.digest('hex');
  } finally {
    if (fd !== undefined) fs.closeSync(fd);
  }
}

function verify(manifest, expected) {
  var issue = validate(manifest);
  if (issue) return { ok: false, error: 'artifact-invalid', detail: issue };
  if (expected && (manifest.platform !== expected.platform || manifest.variantId !== expected.variantId ||
      manifest.applicationId !== expected.applicationId || manifest.runConfigHash !== expected.runConfigHash)) {
    return { ok: false, error: 'artifact-config-mismatch' };
  }
  var file;
  try { file = artifactPath(manifest, expected); }
  catch (error) { return { ok: false, error: error && error.code === 'artifact-scope-mismatch'
    ? 'artifact-scope-mismatch' : 'artifact-invalid', detail: error.message }; }
  try {
    if (expected && expected.allowedBuildRoot && !containedBy(expected.allowedBuildRoot, file)) {
      return { ok: false, error: 'artifact-path-mismatch' };
    }
    if (expected && expected.toolchainFingerprint &&
        manifest.toolchainFingerprint !== expected.toolchainFingerprint) {
      return { ok: false, error: 'artifact-toolchain-mismatch' };
    }
    if (expected && !architectureCompatible(manifest.platform, manifest.targetArchitectures, expected)) {
      return { ok: false, error: 'artifact-architecture-mismatch' };
    }
    if (manifest.platform === 'android') {
      var digest = hashRegularFile(file, manifest.artifactSize);
      if (digest !== manifest.artifactHash) return { ok: false, error: 'artifact-hash-mismatch' };
    } else {
      var appStat = fs.lstatSync(file);
      if (!appStat.isDirectory() || appStat.isSymbolicLink()) return { ok: false, error: 'artifact-invalid' };
      var tree = iosRunner.hashAppTree(file);
      if (tree.hash !== manifest.artifactHash || tree.size !== manifest.artifactSize) {
        return { ok: false, error: 'artifact-hash-mismatch' };
      }
    }
  } catch (error) {
    return {
      ok: false,
      error: error && error.code === 'artifact-hash-mismatch'
        ? 'artifact-hash-mismatch' : 'artifact-invalid',
      detail: String(error && error.message || error).slice(0, 300)
    };
  }
  return { ok: true, path: file, manifest: manifest };
}

function latest(platform, variantId) {
  var rows = [];
  var invalidIds = [];
  storage.list(paths.APP_RUN_ARTIFACTS_DIR, 'artifact').forEach(function (id) {
    try {
      var manifest = read(id);
      if (manifest.platform === platform && manifest.variantId === variantId) rows.push(manifest);
    } catch (_) { invalidIds.push(id); }
  });
  if (invalidIds.length) {
    var error = new Error('stored app-run artifact manifests require recovery');
    error.code = 'artifact-invalid';
    throw error;
  }
  rows.sort(function (a, b) { return Date.parse(b.builtAt) - Date.parse(a.builtAt); });
  return rows[0] || null;
}

function prune(activeArtifactId) {
  var issues = [];
  var rows = storage.list(paths.APP_RUN_ARTIFACTS_DIR, 'artifact').map(function (id) {
    try { return { id: id, value: read(id) }; } catch (_) { return { id: id, value: null }; }
  });
  rows.filter(function (row) { return !row.value; }).forEach(function (row) {
    // Corrupt authority is recovery evidence, not disposable retention data.
    // Preserve it and force the owner to expose recovery-required.
    issues.push(row.id);
  });
  rows.filter(function (row) { return !!row.value; }).sort(function (a, b) {
    return Date.parse(b.value && b.value.builtAt || 0) - Date.parse(a.value && a.value.builtAt || 0);
  }).slice(20).forEach(function (row) {
    if (row.id !== activeArtifactId) {
      try { storage.remove(paths.APP_RUN_ARTIFACTS_DIR, row.id); }
      catch (_) { issues.push(row.id); }
    }
  });
  return issues;
}

module.exports = {
  HASH_RE: HASH_RE,
  validate: validate,
  toolchainFingerprint: toolchainFingerprint,
  create: create,
  read: read,
  verify: verify,
  latest: latest,
  prune: prune,
  artifactPath: artifactPath
};
