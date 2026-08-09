'use strict';

// Deterministic first-sync discovery for the usage-scoped Figma variable
// pipeline. Normal refreshes are driven by the committed source index. On a
// clean template there is no index yet, so seed exact node/context identities
// from the configured Figma node plus valid task ## Design declarations.
// Figma's conventional 0:1 document/page root is useful for access probes, but
// the provider's variable-definition capture may reject it. Prefer concrete
// task nodes whenever they
// exist so one unusable root cannot poison an otherwise valid atomic refresh.
//
// This module never calls Figma and never writes project state. It only builds
// the same immutable source rows that the normal server-owned capture planner
// already knows how to reserve, capture, normalize, and publish.

var path = require('path');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var designParser = require(path.join(paths.ORCHESTRATOR_DIR, 'figma', 'scripts', 'design-parser.cjs'));
var identity = require(path.join(paths.ORCHESTRATOR_DIR, 'figma', 'runtime', 'token-identity.cjs'));

var HASH_RE = /^sha256:[a-f0-9]{64}$/;
var STEM_RE = /^TASK_[1-9][0-9]*_[A-Za-z0-9_]+$/;
var TASK_MAX = 128 * 1024;
var TASK_FILES_MAX = 1024;
var SOURCES_MAX = 128;
var ORIGINS_MAX = 64;
var TASK_COLUMNS = Object.freeze([
  { name: 'backlog', suffix: '.md' },
  { name: 'pending', suffix: '.questions.md' },
  { name: 'todo', suffix: '.md' },
  { name: 'done', suffix: '.md' }
]);

function configuredNodeId(figmaLibraryUrl) {
  var parsed;
  try { parsed = new URL(String(figmaLibraryUrl || '')); }
  catch (error) { throw new Error('TOKEN_SOURCE_BOOTSTRAP_INVALID'); }
  var values = parsed.searchParams.getAll('node-id');
  if (values.length > 1) throw new Error('TOKEN_SOURCE_BOOTSTRAP_INVALID');
  var raw = values.length ? values[0] : '0-1';
  if (!/^[0-9]+[:-][0-9]+$/.test(raw)) throw new Error('TOKEN_SOURCE_BOOTSTRAP_INVALID');
  return raw.replace('-', ':');
}

function taskStem(name, suffix) {
  if (typeof name !== 'string' || name.slice(-suffix.length) !== suffix) return null;
  var stem = name.slice(0, -suffix.length);
  return STEM_RE.test(stem) ? stem : null;
}

function themeRecord(theme) {
  if (theme === 'primary') return { theme: 'unknown', variantId: 'unknown-default-shared' };
  if (theme === 'light') return { theme: 'light', variantId: 'light-default-shared' };
  if (theme === 'dark') return { theme: 'dark', variantId: 'dark-default-shared' };
  return null;
}

function taskBodies() {
  var rows = [];
  TASK_COLUMNS.forEach(function (column) {
    var directory = path.join(paths.TASKS_DIR, column.name);
    var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, directory, TASK_FILES_MAX);
    if (!listed.ok) {
      var entry = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, path.dirname(directory), directory);
      if (entry && entry.status === 'missing') return;
      throw new Error('TOKEN_SOURCE_BOOTSTRAP_INPUT_UNSAFE');
    }
    listed.names.slice().sort().forEach(function (name) {
      var stem = taskStem(name, column.suffix);
      if (!stem) return;
      if (rows.length >= TASK_FILES_MAX) throw new Error('TOKEN_SOURCE_BOOTSTRAP_LIMIT_EXCEEDED');
      var file = path.join(directory, name);
      var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, directory, file, TASK_MAX);
      if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') {
        throw new Error('TOKEN_SOURCE_BOOTSTRAP_INPUT_UNSAFE');
      }
      rows.push({ stem: stem, body: hit.bytes.toString('utf8') });
    });
  });
  return rows;
}

function addSource(byId, input, origin) {
  var source = {
    fileKeyFingerprint: input.fileKeyFingerprint,
    branchKey: input.branchKey,
    nodeId: input.nodeId,
    kind: 'screen',
    context: input.context,
    origin: origin
  };
  source.sourceId = identity.sourceIdFor(source);
  var existing = byId[source.sourceId];
  if (!existing) {
    byId[source.sourceId] = {
      sourceId: source.sourceId,
      lifecycle: 'active',
      nodeId: source.nodeId,
      kind: source.kind,
      context: source.context,
      origin: origin,
      origins: [origin],
      acceptedSequence: 0
    };
    return;
  }
  var key = identity.canonical(origin);
  if (!existing.origins.some(function (candidate) {
    return identity.canonical(candidate) === key;
  })) {
    if (existing.origins.length >= ORIGINS_MAX) throw new Error('TOKEN_SOURCE_BOOTSTRAP_LIMIT_EXCEEDED');
    existing.origins.push(origin);
  }
}

function discover(options) {
  options = options || {};
  if (!options.scope || !HASH_RE.test(String(options.scope.fileKeyFingerprint || '')) ||
      typeof options.scope.branchKey !== 'string' || !options.scope.branchKey ||
      typeof options.figmaFileKey !== 'string' || !options.figmaFileKey ||
      typeof options.figmaLibraryUrl !== 'string' || !options.figmaLibraryUrl) {
    throw new Error('TOKEN_SOURCE_BOOTSTRAP_INVALID');
  }
  var byId = Object.create(null);
  var rootNodeId = configuredNodeId(options.figmaLibraryUrl);

  taskBodies().forEach(function (task) {
    var parsed;
    try { parsed = designParser.parseDesignSources([task.body]); }
    catch (error) { return; }
    // A malformed task stays visible to the task admission flow, but cannot
    // contribute a partially trusted bootstrap source set.
    if (!parsed || !parsed.hasPullable || parsed.issues.length) return;
    parsed.entries.filter(function (entry) { return !entry.none; }).forEach(function (entry) {
      Object.keys(entry.themes || {}).sort().forEach(function (theme) {
        var target = entry.themes[theme];
        var variant = themeRecord(theme);
        if (!target || !target.ok || target.fileKey !== options.figmaFileKey || !variant) return;
        addSource(byId, {
          fileKeyFingerprint: options.scope.fileKeyFingerprint,
          branchKey: options.scope.branchKey,
          nodeId: target.nodeId,
          context: { theme: variant.theme, locale: 'default', platform: 'shared' }
        }, {
          kind: 'task-screen',
          taskStem: task.stem,
          screenKey: entry.screen,
          variantId: variant.variantId
        });
      });
    });
  });

  // Keep an explicitly configured concrete frame/node as a source. The
  // conventional 0:1 root is retained only as a last-resort source when there
  // are no concrete task nodes at all; that produces an honest provider error
  // instead of silently publishing an empty catalog.
  if (rootNodeId !== '0:1' || !Object.keys(byId).length) {
    addSource(byId, {
      fileKeyFingerprint: options.scope.fileKeyFingerprint,
      branchKey: options.scope.branchKey,
      nodeId: rootNodeId,
      context: { theme: 'unknown', locale: 'default', platform: 'shared' }
    }, {
      kind: 'project-config',
      configField: 'figmaLibraryUrl'
    });
  }

  var sources = Object.keys(byId).sort().map(function (sourceId) {
    var source = byId[sourceId];
    source.origins.sort(function (left, right) {
      var leftKey = identity.canonical(left);
      var rightKey = identity.canonical(right);
      return leftKey < rightKey ? -1 : leftKey > rightKey ? 1 : 0;
    });
    source.origin = source.origins[0];
    return source;
  });
  if (!sources.length) throw new Error('TOKEN_SOURCE_BOOTSTRAP_EMPTY');
  if (sources.length > SOURCES_MAX) throw new Error('TOKEN_SOURCE_BOOTSTRAP_LIMIT_EXCEEDED');
  return {
    sources: sources,
    rootNodeId: rootNodeId,
    sourceCount: sources.length
  };
}

module.exports = Object.freeze({
  configuredNodeId: configuredNodeId,
  discover: discover,
  _test: Object.freeze({
    taskStem: taskStem,
    themeRecord: themeRecord
  })
});
