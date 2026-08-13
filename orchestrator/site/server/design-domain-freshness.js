'use strict';

// Shared startup/offline freshness reconciliation for token and component
// adapters. Comparison execution/publication is intentionally NOT here: all
// manual and post-commit compares use figma-sync's one durable job/history/
// cancellation lifecycle.

var fs = require('fs');
var path = require('path');
var crypto = require('crypto');
var pathToFileURL = require('url').pathToFileURL;
var paths = require('./paths');
var fileGuards = require('./file-guards');
var generation = require('./figma-generation');
var sync = require('./figma-sync');

var WATCH_DEBOUNCE_MS = 2000;
var WATCH_DIRECTORIES_MAX = 2048;
var WATCH_ENTRIES_MAX = 20000;
var WATCH_POLL_MS = 1000;
var ANALYSIS_SCHEMA_VERSION = 2;

function exactStat(stat) {
  return [
    String(stat.dev), String(stat.ino), String(stat.mode), String(stat.nlink),
    String(stat.size), String(stat.mtimeNs), String(stat.ctimeNs),
    stat.isDirectory() ? 'directory' : stat.isFile() ? 'file'
      : stat.isSymbolicLink() ? 'symlink' : 'other'
  ];
}

function sameExactStat(left, right) {
  return exactStat(left).join('\0') === exactStat(right).join('\0');
}

function create(options) {
  var watchers = [];
  var watcherTimer = null;
  var watcherPoll = null;
  var watcherSnapshot = null;

  function startupReconcile() {
    var active = generation.current();
    if (!active.ok || active.mode !== 'generation') return Promise.resolve({ ok: true, skipped: 'no-generation' });
    var indexEntry = active.manifest.artifacts.find(function (entry) { return entry.role === options.indexRole; });
    var indexBytes = generation.readEntry(indexEntry);
    if (!indexBytes) return Promise.resolve({ ok: true, skipped: 'no-analysis' });
    var index;
    try { index = JSON.parse(indexBytes.toString('utf8')); } catch (error) {
      options.state.markProjectDirty('startup-analysis-invalid');
      return Promise.resolve({ ok: true, dirty: true, error: options.generationError });
    }
    if (!index || index.schemaVersion !== ANALYSIS_SCHEMA_VERSION || !Array.isArray(index.adapters)) {
      options.state.markProjectDirty('startup-analysis-invalid');
      return Promise.resolve({ ok: true, dirty: true, error: options.generationError });
    }
    var figmaDir = path.join(paths.ORCHESTRATOR_DIR, 'figma');
    return Promise.all([
      import(pathToFileURL(path.join(figmaDir, 'runtime', 'adapter-config.mjs')).href),
      import(pathToFileURL(path.join(figmaDir, 'runtime', options.extractionModule)).href),
      import(pathToFileURL(path.join(figmaDir, options.contractModule)).href)
    ]).then(function (loaded) {
      var configState;
      try { configState = loaded[0].loadAdapterConfig({ projectRoot: paths.PROJECT_ROOT }); }
      catch (error) { options.state.markProjectDirty('startup-config-invalid'); return { ok: true, dirty: true }; }
      if (configState.state !== 'configured') return { ok: true, skipped: 'unconfigured' };
      var dirty = false;
      configState.config[options.enabledAdaptersKey].forEach(function (adapter) {
        var row = index.adapters.find(function (candidate) { return candidate.adapterId === adapter.id; });
        if (!row) { dirty = true; return; }
        try {
          var snapshot = loaded[1][options.snapshotFunction](paths.PROJECT_ROOT, adapter, { keepText: true });
          if (loaded[1][options.scopeFunction](adapter, snapshot.branchKey) !== row.scopeFingerprint) { dirty = true; return; }
          var domainConfigHash = options.capability === 'tokens'
            ? configState.tokenConfigHash : configState.componentConfigHash;
          var inventory = loaded[1][options.buildFunction]({ adapter: adapter, snapshot: snapshot, configHash: domainConfigHash });
          if (loaded[2].projectInventorySemanticHash(inventory) !== row.inventoryHash) dirty = true;
        } catch (error) { dirty = true; }
      });
      if (dirty) options.state.markProjectDirty('startup-offline-change');
      return { ok: true, dirty: dirty };
    }).catch(function () {
      options.state.markProjectDirty('startup-reconcile-failed');
      return { ok: false, dirty: true, error: options.generationError };
    });
  }

  function closeWatchers() {
    watchers.forEach(function (watcher) { try { watcher.close(); } catch (error) {} });
    watchers = [];
    if (watcherTimer) clearTimeout(watcherTimer);
    watcherTimer = null;
    if (watcherPoll) clearInterval(watcherPoll);
    watcherPoll = null;
    watcherSnapshot = null;
  }

  function validRoot(root) {
    return typeof root === 'string' && !!root && !path.isAbsolute(root) && root.indexOf('\\') < 0 &&
      root.split('/').every(function (segment) { return !!segment && segment !== '.' && segment !== '..'; });
  }

  function boundedPollSnapshot(document) {
    var projectPath = path.resolve(paths.PROJECT_ROOT);
    var projectReal = fs.realpathSync(projectPath);
    var projectStat = fs.lstatSync(projectReal, { bigint: true });
    if (!projectStat.isDirectory() || projectStat.isSymbolicLink()) {
      throw new Error('watcher-project-root-unsafe');
    }
    var configBefore = fs.lstatSync(options.state.CONFIG_FILE, { bigint: true });
    if (!configBefore.isFile() || configBefore.isSymbolicLink() ||
        configBefore.size > BigInt(256 * 1024)) throw new Error('watcher-config-unsafe');
    var configBytes = fs.readFileSync(options.state.CONFIG_FILE);
    var configAfter = fs.lstatSync(options.state.CONFIG_FILE, { bigint: true });
    if (!sameExactStat(configBefore, configAfter)) throw new Error('watcher-config-changed');

    var parts = [
      'config',
      crypto.createHash('sha256').update(configBytes).digest('hex')
    ];
    var seen = Object.create(null);
    var budget = { directories: 0, entries: 0 };
    function walk(configuredRoot) {
      var requested = path.resolve(projectPath, configuredRoot);
      var canonicalExpected = path.resolve(projectReal, path.relative(projectPath, requested));
      var start = fs.realpathSync(requested);
      if (start !== canonicalExpected) throw new Error('watcher-root-unsafe');
      var pending = [start];
      while (pending.length) {
        var directory = pending.pop();
        if (seen[directory]) continue;
        seen[directory] = true;
        budget.directories++;
        if (budget.directories > WATCH_DIRECTORIES_MAX) {
          throw new Error('watcher-directory-limit');
        }
        var before = fs.lstatSync(directory, { bigint: true });
        if (!before.isDirectory() || before.isSymbolicLink() ||
            fs.realpathSync(directory) !== directory) {
          throw new Error('watcher-directory-unsafe');
        }
        var names = fs.readdirSync(directory).sort();
        budget.entries += names.length;
        if (budget.entries > WATCH_ENTRIES_MAX) throw new Error('watcher-entry-limit');
        parts.push('directory', path.relative(projectReal, directory), exactStat(before), names);
        for (var i = names.length - 1; i >= 0; i--) {
          var entry = path.join(directory, names[i]);
          var stat = fs.lstatSync(entry, { bigint: true });
          if (stat.isSymbolicLink() || fs.realpathSync(entry) !== entry) {
            throw new Error('watcher-entry-unsafe');
          }
          if (!stat.isDirectory() && !stat.isFile()) throw new Error('watcher-entry-unsafe');
          parts.push('entry', path.relative(projectReal, entry), exactStat(stat));
          if (stat.isDirectory()) pending.push(entry);
        }
        var after = fs.lstatSync(directory, { bigint: true });
        if (!sameExactStat(before, after)) throw new Error('watcher-directory-changed');
      }
    }
    document.adapters.slice(0, 8).forEach(function (adapter) {
      var section = adapter && adapter[options.capability];
      if (!adapter || adapter.enabled !== true || !section) return;
      options.watchRootKeys.forEach(function (key) {
        if (!Array.isArray(section[key])) return;
        section[key].slice(0, 16).forEach(function (root) {
          if (!validRoot(root)) throw new Error('watcher-root-invalid');
          walk(root);
        });
      });
    });
    var projectAfter = fs.lstatSync(projectReal, { bigint: true });
    if (!sameExactStat(projectStat, projectAfter)) throw new Error('watcher-project-root-changed');
    return crypto.createHash('sha256').update(JSON.stringify(parts)).digest('hex');
  }

  function watchAdapterRoots() {
    closeWatchers();
    var read;
    try { read = fs.readFileSync(options.state.CONFIG_FILE, 'utf8'); }
    catch (error) { options.state.markProjectDirty('watcher-config-unavailable'); return; }
    var document;
    try { document = JSON.parse(read); }
    catch (error) { options.state.markProjectDirty('watcher-config-invalid'); return; }
    if (!document || !Array.isArray(document.adapters)) {
      options.state.markProjectDirty('watcher-config-invalid');
      return;
    }
    var onChange = function () {
      // Latch immediately so a compare that starts during the debounce window
      // can clear this exact observed change after its publish postcondition.
      // The expensive watcher-tree rebuild, not correctness authority, is the
      // debounced operation.
      options.state.markProjectDirty('watcher');
      if (watcherTimer) clearTimeout(watcherTimer);
      watcherTimer = setTimeout(function () {
        watcherTimer = null;
        // Directory creation/rename changes the set that must be watched on
        // platforms without recursive fs.watch support.
        watchAdapterRoots();
      }, WATCH_DEBOUNCE_MS);
      if (typeof watcherTimer.unref === 'function') watcherTimer.unref();
    };
    var watched = Object.create(null), budget = { directories: 0 }, failed = false;
    function addWatcher(directory, recursiveMode) {
      var key = path.resolve(directory);
      if (watched[key]) return;
      if (++budget.directories > WATCH_DIRECTORIES_MAX) throw new Error('watcher-directory-limit');
      watchers.push(fs.watch(directory, { recursive: recursiveMode, persistent: false }, onChange));
      watched[key] = 1;
    }
    function watchTree(directory) {
      addWatcher(directory, false);
      var remaining = WATCH_DIRECTORIES_MAX - budget.directories;
      var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, directory, remaining);
      if (!listed.ok || listed.exists === false) throw new Error('watcher-directory-unavailable');
      listed.names.forEach(function (name) {
        var child = path.join(directory, name);
        var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, directory, child);
        if (!inspected || inspected.status !== 'present' || !inspected.stat || inspected.stat.isSymbolicLink()) {
          throw new Error('watcher-entry-unsafe');
        }
        if (inspected.stat.isDirectory()) watchTree(child);
      });
    }
    // Re-arm when project-adapters.json itself is edited or atomically
    // replaced, so changed root sets cannot leave the old watchers active.
    try {
      var configDirectory = path.dirname(options.state.CONFIG_FILE);
      var configName = path.basename(options.state.CONFIG_FILE);
      watchers.push(fs.watch(configDirectory, { recursive: false, persistent: false }, function (_event, filename) {
        if (filename == null || String(filename) === configName) onChange();
      }));
    } catch (error) { failed = true; }
    document.adapters.slice(0, 8).forEach(function (adapter) {
      var section = adapter && adapter[options.capability];
      if (!adapter || adapter.enabled !== true || !section) return;
      options.watchRootKeys.forEach(function (key) {
        if (!Array.isArray(section[key])) return;
        section[key].slice(0, 16).forEach(function (root) {
          if (!validRoot(root)) { failed = true; return; }
          try {
            var directory = path.join(paths.PROJECT_ROOT, root);
            // Use one non-recursive watcher per existing directory on every
            // platform. Recursive fs.watch is explicitly best-effort in Node
            // and has dropped nested events on macOS under load. The bounded
            // tree also gives Windows and Linux the same correctness contract;
            // parent rename events re-arm it when directories are added.
            watchTree(directory);
          } catch (error) { failed = true; }
        });
      });
    });
    if (failed) {
      options.state.markProjectDirty('watcher-unavailable');
      return;
    }
    try {
      watcherSnapshot = boundedPollSnapshot(document);
      watcherPoll = setInterval(function () {
        try {
          var next = boundedPollSnapshot(document);
          if (next !== watcherSnapshot) {
            watcherSnapshot = next;
            onChange();
          }
        } catch (error) {
          options.state.markProjectDirty('watcher-unavailable');
          if (watcherPoll) clearInterval(watcherPoll);
          watcherPoll = null;
        }
      }, WATCH_POLL_MS);
      if (typeof watcherPoll.unref === 'function') watcherPoll.unref();
    } catch (error) {
      options.state.markProjectDirty('watcher-unavailable');
    }
  }

  function ensureFresh(reason) {
    return sync.requestDriftComparison(reason);
  }

  function init() {
    return startupReconcile().then(function (result) {
      watchAdapterRoots();
      return result;
    });
  }

  return { ensureFresh: ensureFresh, init: init };
}

module.exports = { ANALYSIS_SCHEMA_VERSION: ANALYSIS_SCHEMA_VERSION, create: create };
