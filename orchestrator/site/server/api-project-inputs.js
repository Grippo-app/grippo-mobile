'use strict';

// Canonical VCS-neutral input receipt for the API sidecar analyzer. The
// analyzer and the Site freshness projection call this exact module so a
// report cannot be considered current under a different file-selection rule.

var fs = require('fs');
var path = require('path');
var crypto = require('crypto');

var ANALYZER_VERSION = 'api-project-analyzer-v2';
var SOURCE_EXTENSIONS = Object.freeze({
  '.kt': 1, '.kts': 1, '.swift': 1, '.java': 1, '.js': 1,
  '.jsx': 1, '.mjs': 1, '.cjs': 1, '.ts': 1, '.tsx': 1, '.cs': 1
});
var SKIP_DIRECTORIES = Object.freeze({
  '.git': 1, '.gradle': 1, '.idea': 1, '.cache': 1, '.build': 1,
  '.next': 1, '.turbo': 1, node_modules: 1, vendor: 1, build: 1,
  dist: 1, out: 1, target: 1, DerivedData: 1
});
var FILE_MAX = 2 * 1024 * 1024;
var TOTAL_MAX = 64 * 1024 * 1024;
var FILE_COUNT_MAX = 20000;
var DIRECTORY_COUNT_MAX = 20000;

function sha(value) {
  return 'sha256:' + crypto.createHash('sha256')
    .update(Buffer.isBuffer(value) ? value : Buffer.from(String(value), 'utf8')).digest('hex');
}
function stable(value) {
  if (Array.isArray(value)) return value.map(stable);
  if (!value || typeof value !== 'object') return value;
  var out = {};
  Object.keys(value).sort().forEach(function (key) { out[key] = stable(value[key]); });
  return out;
}
function failure(code, detail) {
  return {
    ok: false, error: code, detail: String(detail || code).slice(0, 500),
    projectCodeRevision: null, records: [], receipt: null,
    totalBytes: 0, directories: 0
  };
}
function throwCode(code, detail) {
  var error = new Error(detail || code);
  error.code = code;
  throw error;
}
function sameFile(left, right) {
  return left && right && left.isFile() && right.isFile() &&
    !left.isSymbolicLink() && !right.isSymbolicLink() &&
    String(left.dev) === String(right.dev) && String(left.ino) === String(right.ino) &&
    String(left.mode) === String(right.mode) && String(left.nlink) === String(right.nlink) &&
    String(left.size) === String(right.size) && String(left.mtimeNs) === String(right.mtimeNs) &&
    String(left.ctimeNs) === String(right.ctimeNs);
}
function projectPath(root, file) {
  var rel = path.relative(root, file);
  if (!rel || rel === '..' || rel.indexOf('..' + path.sep) === 0 ||
      path.resolve(root, rel) !== path.resolve(file)) throwCode('analyzer-path-outside-project');
  var portable = rel.split(path.sep).join('/').normalize('NFC');
  if (portable.length > 500 || portable.split('/').some(function (part) {
    return !part || part === '.' || part === '..';
  })) throwCode('analyzer-path-invalid');
  return portable;
}
function safeRead(root, file) {
  var fd;
  try {
    var before = fs.lstatSync(file, { bigint: true });
    if (!before.isFile() || before.isSymbolicLink() || before.nlink !== 1n ||
        before.size > BigInt(FILE_MAX)) {
      throwCode('analyzer-source-unsafe', projectPath(root, file));
    }
    fd = fs.openSync(file, fs.constants.O_RDONLY | (fs.constants.O_NOFOLLOW || 0));
    var opened = fs.fstatSync(fd, { bigint: true });
    if (!sameFile(before, opened)) throwCode('analyzer-source-raced', projectPath(root, file));
    var bytes = Buffer.alloc(Number(opened.size));
    var offset = 0;
    while (offset < bytes.length) {
      var count = fs.readSync(fd, bytes, offset, bytes.length - offset, offset);
      if (!count) throwCode('analyzer-source-short-read', projectPath(root, file));
      offset += count;
    }
    var afterFd = fs.fstatSync(fd, { bigint: true });
    var afterPath = fs.lstatSync(file, { bigint: true });
    if (!sameFile(opened, afterFd) || !sameFile(opened, afterPath)) {
      throwCode('analyzer-source-raced', projectPath(root, file));
    }
    return bytes;
  } finally {
    if (fd !== undefined) try { fs.closeSync(fd); } catch (ignore) {}
  }
}
function collect(projectRoot, options) {
  options = options || {};
  var root = path.resolve(projectRoot || '');
  try {
    var rootStat = fs.lstatSync(root);
    if (!rootStat.isDirectory() || rootStat.isSymbolicLink()) {
      return failure('analyzer-project-root-unsafe');
    }
    root = fs.realpathSync(root);
    var records = [], directories = 0, totalBytes = 0;
    var collision = Object.create(null);
    function addFile(file) {
      var portable = projectPath(root, file);
      var folded = portable.normalize('NFC').toLocaleLowerCase('en-US');
      if (collision[folded] && collision[folded] !== portable) {
        throwCode('analyzer-path-collision', collision[folded] + ' / ' + portable);
      }
      collision[folded] = portable;
      var bytes = safeRead(root, file);
      totalBytes += bytes.length;
      if (totalBytes > TOTAL_MAX) throwCode('analyzer-byte-cap');
      if (records.length >= FILE_COUNT_MAX) throwCode('analyzer-file-cap');
      records.push({
        path: portable, size: bytes.length, hash: sha(bytes),
        text: options.includeText === false ? null : bytes.toString('utf8')
      });
    }
    function visit(directory, depth) {
      directories++;
      if (directories > DIRECTORY_COUNT_MAX) throwCode('analyzer-directory-cap');
      var before = fs.lstatSync(directory, { bigint: true });
      if (!before.isDirectory() || before.isSymbolicLink() ||
          fs.realpathSync(directory) !== directory) throwCode('analyzer-directory-unsafe');
      var entries = fs.readdirSync(directory, { withFileTypes: true })
        .sort(function (left, right) { return left.name.localeCompare(right.name); });
      for (var i = 0; i < entries.length; i++) {
        var entry = entries[i];
        if (!entry.name || entry.name.indexOf('\0') >= 0) throwCode('analyzer-path-invalid');
        var file = path.join(directory, entry.name);
        if (entry.isSymbolicLink()) {
          throwCode('analyzer-source-symlink', projectPath(root, file));
        }
        if (entry.isDirectory()) {
          if (!SKIP_DIRECTORIES[entry.name] && !(depth === 0 && entry.name === 'orchestrator')) {
            visit(file, depth + 1);
          }
          continue;
        }
        var supported = !!SOURCE_EXTENSIONS[path.extname(entry.name).toLowerCase()];
        if (entry.isFile() && supported) addFile(file);
      }
      var after = fs.lstatSync(directory, { bigint: true });
      if (String(before.dev) !== String(after.dev) || String(before.ino) !== String(after.ino) ||
          String(before.mtimeNs) !== String(after.mtimeNs) || String(before.ctimeNs) !== String(after.ctimeNs)) {
        throwCode('analyzer-directory-raced', projectPath(root, directory));
      }
    }
    visit(root, 0);
    var configFile = path.join(root, 'orchestrator', 'project-config.md');
    if (fs.existsSync(configFile)) addFile(configFile);
    records.sort(function (left, right) { return left.path.localeCompare(right.path); });
    var receipt = {
      schemaVersion: 1,
      analyzerVersion: ANALYZER_VERSION,
      files: records.map(function (record) {
        return { path: record.path, size: record.size, hash: record.hash };
      })
    };
    return {
      ok: true,
      error: null,
      detail: null,
      records: records,
      receipt: receipt,
      projectCodeRevision: sha(JSON.stringify(stable(receipt))),
      totalBytes: totalBytes,
      directories: directories
    };
  } catch (error) {
    return failure(error && error.code || 'analyzer-input-unavailable', error && error.message);
  }
}

module.exports = {
  ANALYZER_VERSION: ANALYZER_VERSION,
  SOURCE_EXTENSIONS: SOURCE_EXTENSIONS,
  collect: collect,
  sha: sha,
  stable: stable,
  _test: {
    projectPath: projectPath
  }
};
