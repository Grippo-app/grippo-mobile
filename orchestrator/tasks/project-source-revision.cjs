'use strict';

// Deterministic, VCS-independent project input revision used by both the app
// runner and task checkpoints. The scanner deliberately fails closed: a
// symlink, hard-linked file, changing inode, unknown profile, or exceeded
// budget never produces a partial revision.

var fs = require('fs');
var path = require('path');
var crypto = require('crypto');
var taskSourceContract = require('./task-source-contract.cjs');

var VERSION = 1;
var MAX_FILES = 20000;
var MAX_BYTES = 512 * 1024 * 1024;
var PROFILE_RE = /^(app-build|task-run)$/;
var TOP_LEVEL_BUILD_FILES = Object.freeze([
  'build.gradle', 'build.gradle.kts', 'gradle.properties', 'gradlew',
  'gradlew.bat', 'settings.gradle', 'settings.gradle.kts'
]);
var DEFAULT_APP_ROOTS = Object.freeze([
  'androidApp', 'shared', 'iosApp', 'buildSrc', 'gradle'
]);
var COMMON_APP_ROOTS = Object.freeze(['shared', 'buildSrc', 'gradle']);
var ALWAYS_FILES = Object.freeze([
  'orchestrator/project-config.md', 'orchestrator/app-run.json'
]);
var EXCLUDED_DIRS = Object.freeze({
  '.git': 1, '.gradle': 1, '.idea': 1, '.kotlin': 1, '.swiftpm': 1,
  'DerivedData': 1, 'build': 1, 'node_modules': 1, 'out': 1,
  'xcuserdata': 1, '__pycache__': 1
});
var EXCLUDED_RELATIVE_PREFIXES = Object.freeze([
  'orchestrator/.cache/',
  'orchestrator/api-contract/node_modules/',
  'orchestrator/figma/node_modules/'
]);
var EXCLUDED_FILES = Object.freeze({
  '.arch-map.json': 1,
  '.DS_Store': 1
});

function unavailable(code, detail, profile) {
  return {
    available: false,
    revision: null,
    inputCount: 0,
    contentBytes: 0,
    profile: profile || null,
    profileVersion: VERSION,
    limitations: [code],
    reasonCode: code,
    detail: String(detail || code).slice(0, 500)
  };
}

function sameStat(a, b) {
  return !!a && !!b && String(a.dev) === String(b.dev) &&
    String(a.ino) === String(b.ino) && String(a.size) === String(b.size) &&
    Number(a.mode) === Number(b.mode) && Number(a.mtimeMs) === Number(b.mtimeMs) &&
    Number(a.ctimeMs) === Number(b.ctimeMs);
}

function normalizeRelative(root, file) {
  var rel = path.relative(root, file).split(path.sep).join('/');
  if (!rel || rel === '.' || rel.indexOf('../') === 0 || path.isAbsolute(rel) ||
      rel.indexOf('\0') >= 0) {
    throw new Error('source input escaped the project root');
  }
  return rel.normalize('NFC');
}

function excludedRelative(rel) {
  if (EXCLUDED_FILES[path.posix.basename(rel)]) return true;
  for (var i = 0; i < EXCLUDED_RELATIVE_PREFIXES.length; i++) {
    if (rel.indexOf(EXCLUDED_RELATIVE_PREFIXES[i]) === 0) return true;
  }
  return false;
}

function excludedDirectory(rel, name) {
  if (!EXCLUDED_DIRS[name]) return false;
  if (name !== 'build') return true;
  if (rel.indexOf('orchestrator/') === 0) return false;
  var parts = rel.split('/');
  var sourceAnchors = {
    src: 1, Sources: 1, Resources: 1, resources: 1, res: 1, assets: 1
  };
  return !parts.some(function (part) {
    return sourceAnchors[part] || /\.xcassets$/.test(part) || /\.lproj$/.test(part);
  });
}

function appRoots(options) {
  var explicit = !!(options && Object.prototype.hasOwnProperty.call(options, 'appRoots'));
  var rows = (explicit ? COMMON_APP_ROOTS : DEFAULT_APP_ROOTS).slice();
  var configured = options && options.appRoots;
  if (configured !== undefined && !Array.isArray(configured)) {
    throw new Error('configured app roots must be an array');
  }
  if (configured && configured.length > 20) throw new Error('configured app root limit exceeded');
  (configured || []).forEach(function (root) {
    if (typeof root !== 'string' || !/^[A-Za-z0-9._-]{1,120}$/.test(root) ||
        root === '.' || root === '..' || root === 'orchestrator' ||
        EXCLUDED_DIRS[root] || EXCLUDED_FILES[root]) {
      throw new Error('configured app root is unsafe');
    }
    rows.push(root);
  });
  var unique = Object.create(null);
  return rows.filter(function (root) {
    if (unique[root]) return false;
    unique[root] = true;
    return true;
  }).sort();
}

function safeRoot(root) {
  var resolved = path.resolve(root || '');
  var configured = fs.lstatSync(resolved);
  if (!configured.isDirectory() || configured.isSymbolicLink()) {
    throw new Error('configured project root is not a real directory');
  }
  var real = fs.realpathSync(resolved);
  var stat = fs.lstatSync(real);
  if (!stat.isDirectory() || stat.isSymbolicLink()) throw new Error('project root is not a real directory');
  return { path: real, stat: stat };
}

function listTree(root, start, include, descend, budget) {
  var pending = [start];
  while (pending.length) {
    var directory = pending.pop();
    budget.directories++;
    if (budget.directories > MAX_FILES) throw new Error('source directory count limit exceeded');
    var relDir = directory === root ? '' : normalizeRelative(root, directory);
    if (fs.realpathSync(directory) !== directory) {
      throw new Error('source directory ancestor changed: ' + (relDir || '.'));
    }
    var before = fs.lstatSync(directory);
    if (!before.isDirectory() || before.isSymbolicLink()) {
      throw new Error('source directory is unsafe: ' + (relDir || '.'));
    }
    var names = fs.readdirSync(directory).sort();
    if (names.length > MAX_FILES) throw new Error('source directory entry limit exceeded');
    for (var i = names.length - 1; i >= 0; i--) {
      var name = names[i];
      if (!name || name === '.' || name === '..' || name.indexOf('\0') >= 0) {
        throw new Error('source directory contains an invalid entry');
      }
      var file = path.join(directory, name);
      var rel = normalizeRelative(root, file);
      if (excludedRelative(rel)) continue;
      if (!include(rel) && !descend(rel)) continue;
      var stat = fs.lstatSync(file);
      if (stat.isSymbolicLink()) throw new Error('source input is a symlink: ' + rel);
      if (fs.realpathSync(file) !== file) throw new Error('source input ancestor changed: ' + rel);
      if (stat.isDirectory()) {
        if (!excludedDirectory(rel, name) && descend(rel)) pending.push(file);
        continue;
      }
      if (!stat.isFile()) throw new Error('source input is not a regular file: ' + rel);
      if (String(stat.nlink) !== '1') throw new Error('source input has multiple hard links: ' + rel);
      if (include(rel)) {
        budget.files++;
        budget.bytes += Number(stat.size);
        if (budget.files > MAX_FILES) throw new Error('source file count limit exceeded');
        if (budget.bytes > MAX_BYTES) throw new Error('source content byte limit exceeded');
        budget.entries.push({ file: file, rel: rel, stat: stat });
      }
    }
    var after = fs.lstatSync(directory);
    if (!sameStat(before, after)) throw new Error('source directory changed during enumeration: ' + (relDir || '.'));
    budget.directoryEntries.push({ file: directory, rel: relDir || '.', stat: after });
  }
}

function appIncludeFactory(roots) {
  return function (rel) {
    if (ALWAYS_FILES.indexOf(rel) >= 0 || TOP_LEVEL_BUILD_FILES.indexOf(rel) >= 0) return true;
    for (var i = 0; i < roots.length; i++) {
      if (rel === roots[i] || rel.indexOf(roots[i] + '/') === 0) return true;
    }
    return false;
  };
}

function appDescendFactory(roots) {
  return function (rel) {
    if (rel === 'orchestrator') return true;
    if (rel.indexOf('orchestrator/') === 0) return false;
    for (var i = 0; i < roots.length; i++) {
      if (rel === roots[i] || rel.indexOf(roots[i] + '/') === 0) return true;
    }
    return false;
  };
}

function taskIncludeFactory(taskStem, roots) {
  var includeApp = appIncludeFactory(roots);
  return function (rel) {
    if (/^orchestrator\/[^/]+$/.test(rel)) return true;
    if (rel.indexOf('orchestrator/site/') === 0 || rel.indexOf('orchestrator/tasks/') === 0 ||
        rel.indexOf('orchestrator/contracts/') === 0 || rel.indexOf('orchestrator/skills/') === 0 ||
        rel.indexOf('orchestrator/figma/') === 0 || rel.indexOf('orchestrator/api-contract/') === 0) {
      if (!taskStem) return true;
      var taskFile = taskStem + '.md';
      var questions = taskStem + '.questions.md';
      if (rel.indexOf('orchestrator/tasks/backlog/') === 0 ||
          rel.indexOf('orchestrator/tasks/pending/') === 0 ||
          rel.indexOf('orchestrator/tasks/todo/') === 0 ||
          rel.indexOf('orchestrator/tasks/done/') === 0) {
        return path.posix.basename(rel) === taskFile || path.posix.basename(rel) === questions;
      }
      return true;
    }
    return includeApp(rel);
  };
}

function taskDescendFactory(roots) {
  var descendApp = appDescendFactory(roots);
  return function (rel) {
    if (descendApp(rel)) return true;
    if (rel === 'orchestrator') return true;
    var orchestratorRoots = [
      'orchestrator/site', 'orchestrator/tasks', 'orchestrator/contracts',
      'orchestrator/skills', 'orchestrator/figma', 'orchestrator/api-contract'
    ];
    for (var i = 0; i < orchestratorRoots.length; i++) {
      if (rel === orchestratorRoots[i] ||
          rel.indexOf(orchestratorRoots[i] + '/') === 0) return true;
    }
    return false;
  };
}

function readStable(entry) {
  var flags = fs.constants.O_RDONLY;
  if (fs.constants.O_NOFOLLOW) flags |= fs.constants.O_NOFOLLOW;
  var fd;
  try {
    fd = fs.openSync(entry.file, flags);
    var opened = fs.fstatSync(fd);
    if (!opened.isFile() || String(opened.nlink) !== '1' || !sameStat(entry.stat, opened)) {
      throw new Error('source input changed before read: ' + entry.rel);
    }
    var digest = crypto.createHash('sha256');
    var buffer = Buffer.alloc(Math.min(1024 * 1024, Math.max(1, Number(opened.size))));
    var offset = 0;
    while (offset < opened.size) {
      var count = fs.readSync(fd, buffer, 0, Math.min(buffer.length, opened.size - offset), offset);
      if (!count) throw new Error('source input ended during read: ' + entry.rel);
      digest.update(buffer.subarray(0, count));
      offset += count;
    }
    var final = fs.fstatSync(fd);
    if (!sameStat(opened, final)) throw new Error('source input changed during read: ' + entry.rel);
    return { size: Number(opened.size), digest: digest.digest('hex') };
  } finally {
    if (fd !== undefined) try { fs.closeSync(fd); } catch (_) {}
  }
}

function compute(projectRoot, options) {
  options = options || {};
  var profile = options.profile || 'app-build';
  if (!PROFILE_RE.test(profile)) return unavailable('unknown-profile', 'unsupported source revision profile', profile);
  var taskStem = options.taskStem || null;
  if (taskStem !== null && !taskSourceContract.safeTaskStem(String(taskStem))) {
    return unavailable('invalid-task-stem', 'task-run revision received an invalid task stem', profile);
  }
  var root;
  try { root = safeRoot(projectRoot); }
  catch (error) { return unavailable('unsafe-project-root', error.message, profile); }
  var roots;
  try { roots = appRoots(options); }
  catch (rootError) { return unavailable('invalid-app-root', rootError.message, profile); }
  var include = profile === 'app-build'
    ? appIncludeFactory(roots) : taskIncludeFactory(taskStem, roots);
  var descend = profile === 'app-build'
    ? appDescendFactory(roots) : taskDescendFactory(roots);

  var budget = {
    files: 0,
    directories: 0,
    bytes: 0,
    entries: [],
    directoryEntries: []
  };
  try {
    listTree(
      root.path,
      root.path,
      include,
      descend,
      budget
    );
    budget.entries.sort(function (a, b) {
      if (a.rel !== b.rel) return a.rel < b.rel ? -1 : 1;
      return 0;
    });
    for (var d = 1; d < budget.entries.length; d++) {
      if (budget.entries[d - 1].rel === budget.entries[d].rel) {
        throw new Error('normalized source paths collide: ' + budget.entries[d].rel);
      }
    }
    var hash = crypto.createHash('sha256');
    hash.update('project-source-revision\0v' + VERSION + '\0' + profile + '\0' +
      roots.join('\0') + '\0', 'utf8');
    for (var i = 0; i < budget.entries.length; i++) {
      var entry = budget.entries[i];
      var content = readStable(entry);
      var executable = (entry.stat.mode & 0o111) !== 0 ? '1' : '0';
      hash.update(entry.rel + '\0' + executable + '\0' + String(content.size) + '\0' + content.digest + '\0', 'utf8');
    }
    // Close the collection-wide race window: an early input can change after
    // its own descriptor-bound read while later inputs are still being hashed.
    // Re-prove every collected inode and directory immediately before
    // publishing the revision.
    for (var f = 0; f < budget.entries.length; f++) {
      var finalFile = fs.lstatSync(budget.entries[f].file);
      if (fs.realpathSync(budget.entries[f].file) !== budget.entries[f].file ||
          !finalFile.isFile() || finalFile.isSymbolicLink() ||
          String(finalFile.nlink) !== '1' ||
          !sameStat(budget.entries[f].stat, finalFile)) {
        throw new Error('source input changed after read: ' + budget.entries[f].rel);
      }
    }
    for (var q = 0; q < budget.directoryEntries.length; q++) {
      var finalDirectory = fs.lstatSync(budget.directoryEntries[q].file);
      if (fs.realpathSync(budget.directoryEntries[q].file) !== budget.directoryEntries[q].file ||
          !finalDirectory.isDirectory() || finalDirectory.isSymbolicLink() ||
          !sameStat(budget.directoryEntries[q].stat, finalDirectory)) {
        throw new Error('source directory changed after enumeration: ' +
          budget.directoryEntries[q].rel);
      }
    }
    var rootAfter = fs.lstatSync(root.path);
    if (!sameStat(root.stat, rootAfter)) throw new Error('project root changed during source scan');
    return {
      available: true,
      revision: 'sha256:' + hash.digest('hex'),
      inputCount: budget.files,
      contentBytes: budget.bytes,
      profile: profile,
      profileVersion: VERSION,
      limitations: [],
      reasonCode: null,
      detail: null
    };
  } catch (error) {
    var message = String(error && error.message || error);
    var code = message.indexOf('limit exceeded') >= 0 ? 'scan-limit-exceeded'
      : message.indexOf('symlink') >= 0 ? 'symlink-input'
        : message.indexOf('hard link') >= 0 ? 'hardlink-input'
          : message.indexOf('changed') >= 0 ? 'source-race'
            : 'unsafe-source-input';
    return unavailable(code, message, profile);
  }
}

module.exports = {
  VERSION: VERSION,
  MAX_FILES: MAX_FILES,
  MAX_BYTES: MAX_BYTES,
  compute: compute
};
