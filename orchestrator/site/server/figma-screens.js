'use strict';

// ---------------------------------------------------------------------------
// Per-task Figma screen-cache reader (read-only viewer feed for the task
// modal's Screens section). The figma:screens session writes the cache under
// orchestrator/.cache/figma/screens/<stem>/ (spec/instances/context/png/index —
// the implement-figma skill's screen-cache/census contract); the site itself NEVER calls Figma, it only
// reads these local files (golden invariant).
//
//   screensIndex(stem)        → a render-ready per-screen summary for the panel.
//   screenImageFile(stem, sc) → the path-safe absolute path of one screenshot
//                               PNG, or null. The screen name is whitelisted
//                               against index.json's node set (never a user-
//                               controlled filename) and confined under the
//                               task's cache dir — .cache is dot-denied by
//                               static.safeResolve, so this purpose-built reader
//                               is the only way those PNGs reach the browser.
// ---------------------------------------------------------------------------

var fs    = require('fs');
var path  = require('path');
var paths = require('./paths');
var generation = require('./figma-generation');
var fileGuards = require('./file-guards');
var taskSource = require('../../tasks/task-source-contract.cjs');
var shipDriftContract = require('../../figma/scripts/ship-drift-contract.cjs');

// Every screen cache belongs to one canonical task identity. The shared task
// contract enforces both path-safe spelling and the safe-integer id bound.
var VARIANT_RE = /^[A-Za-z0-9][A-Za-z0-9_-]{0,79}$/;
var VARIANT_FILE_RE = /^[A-Za-z0-9][A-Za-z0-9_.-]{0,159}\.png$/;
var MAX_SCREEN_NODES = 10000;
var MAX_SCREEN_DRIFT_ROWS = 10000;
var ISO_RE = /^\d{4}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\d|3[01])T(?:[01]\d|2[0-3]):[0-5]\d:[0-5]\d(?:\.\d{3})?Z$/;

function validScreenName(screen) {
  return typeof screen === 'string' && screen.length > 0 && screen.length <= 200 &&
    screen !== '.' && screen !== '..' && screen.indexOf('/') < 0 && screen.indexOf('\\') < 0 &&
    !/[\x00-\x1f\x7f]/.test(screen);
}

function canonicalFigmaHref(value) {
  if (typeof value !== 'string') return null;
  var u;
  try { u = new URL(value.trim()); } catch (e) { return null; }
  if (u.protocol !== 'https:' || !/^(www\.)?figma\.com$/i.test(u.hostname)) return null;
  var m = /^\/(design|file)\/([A-Za-z0-9]+)(?:\/[^?#]*)?$/.exec(u.pathname);
  if (!m) return null;
  var out = 'https://www.figma.com/' + m[1] + '/' + m[2];
  var node = u.searchParams.get('node-id');
  if (node) {
    if (!/^[0-9]+[:-][0-9]+$/.test(node)) return null;
    out += '?node-id=' + encodeURIComponent(node.replace(':', '-'));
  }
  return out;
}

function exactKeys(value, required, optional) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return false;
  var keys = Object.keys(value).sort();
  var allowed = required.concat(optional || []).sort();
  if (keys.some(function (key) { return allowed.indexOf(key) < 0; })) return false;
  return required.every(function (key) { return Object.prototype.hasOwnProperty.call(value, key); });
}

function canonicalUrl(value) {
  var normalized = canonicalFigmaHref(value);
  return normalized !== null && normalized === value && normalized.indexOf('https://www.figma.com/design/') === 0;
}

function validVariant(value) {
  if (!exactKeys(value,
    ['id', 'theme', 'locale', 'platform', 'url', 'nodeId', 'fetchedAt', 'imageFile',
      'tokensFile', 'tokensHash', 'captureOperationId', 'captureSequence'],
    ['specFile', 'instancesFile'])) return false;
  if (!VARIANT_RE.test(value.id) || !VARIANT_RE.test(value.theme) || !VARIANT_RE.test(value.locale) ||
      ['shared', 'android', 'ios'].indexOf(value.platform) < 0 || !canonicalUrl(value.url) ||
      typeof value.nodeId !== 'string' || !/^[0-9]+:[0-9]+$/.test(value.nodeId) || !ISO_RE.test(value.fetchedAt) ||
      !VARIANT_FILE_RE.test(value.imageFile) ||
      !/^[A-Za-z0-9][A-Za-z0-9_.-]{0,159}\.tokens\.json$/.test(value.tokensFile) ||
      !/^sha256:[a-f0-9]{64}$/.test(value.tokensHash) ||
      !/^tokop_[A-Za-z0-9_-]{16,96}$/.test(value.captureOperationId) ||
      !Number.isSafeInteger(value.captureSequence) || value.captureSequence < 1) return false;
  return ['specFile', 'instancesFile'].every(function (key) {
    return value[key] === undefined || /^[A-Za-z0-9][A-Za-z0-9_.-]{0,159}\.json$/.test(value[key]);
  });
}

function validScreenIndex(value, stem) {
  if (!exactKeys(value, ['schemaVersion', 'taskStem', 'nodes']) || value.schemaVersion !== 3 ||
      value.taskStem !== stem || !value.nodes || typeof value.nodes !== 'object' || Array.isArray(value.nodes)) return false;
  var names = Object.keys(value.nodes);
  if (!names.length || names.length > MAX_SCREEN_NODES || names.some(function (name) { return !/^[A-Za-z0-9_]+$/.test(name); })) return false;
  return names.every(function (name) {
    var node = value.nodes[name];
    if (!exactKeys(node, ['kind', 'variants'], ['url', 'nodeId', 'fetchedAt', 'darkUrl', 'darkNodeId', 'darkFetchedAt']) ||
        ['screen', 'dialog', 'component', 'overlay'].indexOf(node.kind) < 0 ||
        !Array.isArray(node.variants) || !node.variants.length || node.variants.length > 100 ||
        !node.variants.every(validVariant)) return false;
    var primary = ['url', 'nodeId', 'fetchedAt'].every(function (key) { return Object.prototype.hasOwnProperty.call(node, key); });
    var dark = ['darkUrl', 'darkNodeId', 'darkFetchedAt'].every(function (key) { return Object.prototype.hasOwnProperty.call(node, key); });
    if (!primary && !dark) return false;
    if (['url', 'nodeId', 'fetchedAt'].some(function (key) { return Object.prototype.hasOwnProperty.call(node, key); }) && !primary) return false;
    if (['darkUrl', 'darkNodeId', 'darkFetchedAt'].some(function (key) { return Object.prototype.hasOwnProperty.call(node, key); }) && !dark) return false;
    if (primary && (!canonicalUrl(node.url) || !/^[0-9]+:[0-9]+$/.test(node.nodeId) || !ISO_RE.test(node.fetchedAt))) return false;
    if (dark && (!canonicalUrl(node.darkUrl) || !/^[0-9]+:[0-9]+$/.test(node.darkNodeId) || !ISO_RE.test(node.darkFetchedAt))) return false;
    var ids = node.variants.map(function (variant) { return variant.id; });
    var files = node.variants.map(function (variant) { return variant.imageFile; });
    if (ids.length !== new Set(ids).size || files.length !== new Set(files).size ||
        files.some(function (file) { return file.indexOf(name) !== 0 || !/\.png$/.test(file); })) return false;
    function summaryMatches(url, nodeId, fetchedAt) {
      return node.variants.some(function (variant) {
        return variant.url === url && variant.nodeId === nodeId && variant.fetchedAt === fetchedAt;
      });
    }
    return (!primary || summaryMatches(node.url, node.nodeId, node.fetchedAt)) &&
      (!dark || summaryMatches(node.darkUrl, node.darkNodeId, node.darkFetchedAt));
  });
}

// The curated value spec is large; the panel only needs a compact headline.
function specSummary(spec) {
  if (!spec || typeof spec !== 'object') return { frameSizeDp: null, theme: null, elementCount: null };
  return {
    frameSizeDp: spec.frameSizeDp && typeof spec.frameSizeDp === 'object' ? spec.frameSizeDp : null,
    theme: typeof spec.theme === 'string' ? spec.theme : null,
    elementCount: Array.isArray(spec.elements) ? spec.elements.length : null
  };
}

function normalizedVariants(meta, present, screen) {
  var normalized = meta.variants.map(function (variant) {
    return {
      id: variant.id,
      theme: variant.theme.toLowerCase(),
      locale: variant.locale,
      platform: variant.platform,
      url: canonicalFigmaHref(variant.url),
      nodeId: typeof variant.nodeId === 'string' ? variant.nodeId : null,
      fetchedAt: typeof variant.fetchedAt === 'string' ? variant.fetchedAt : null,
      imageFile: variant.imageFile,
      hasPng: present(variant.imageFile)
    };
  });
  normalized.limitations = [];
  return normalized;
}

function componentUsage(instances) {
  if (!Array.isArray(instances)) return [];
  var seen = Object.create(null);
  return instances.slice(0, 10000).map(function (instance) {
    if (!instance || typeof instance !== 'object') return null;
    var name = typeof instance.componentSetName === 'string' && instance.componentSetName.trim()
      ? instance.componentSetName.trim()
      : typeof instance.name === 'string' ? instance.name.trim() : '';
    if (!name || name.length > 200) return null;
    var key = name.toLowerCase();
    if (seen[key]) return null;
    seen[key] = 1;
    return {
      name: name,
      nodeId: typeof instance.figmaNodeId === 'string' ? instance.figmaNodeId : null,
      nodeUrl: canonicalFigmaHref(instance.nodeUrl)
    };
  }).filter(Boolean).slice(0, 500);
}

function inspectGeneratedSurface(active, stem) {
  if (!active || !active.ok || active.mode !== 'generation') return { state: 'missing' };
  var prefix = 'orchestrator/.cache/figma/screens/' + stem + '/';
  var entries = active.manifest.artifacts.filter(function (entry) {
    return entry.group === 'surfaces' && entry.logicalPath.indexOf(prefix) === 0;
  });
  var byLogical = Object.create(null);
  entries.forEach(function (entry) { byLogical[entry.logicalPath] = entry; });
  var indexEntry = byLogical[prefix + 'index.json'];
  if (!indexEntry) return { state: 'missing' };
  if (indexEntry.role !== generation.surfaceIndexRole(stem)) return { state: 'invalid' };
  var bytes = generation.readEntry(indexEntry), index;
  if (!bytes) return { state: 'invalid' };
  try { index = JSON.parse(bytes.toString('utf8')); } catch (error) { return { state: 'invalid' }; }
  if (!validScreenIndex(index, stem)) return { state: 'invalid' };
  function entry(name) { return byLogical[prefix + name] || null; }
  function json(name) {
    var value = generation.readEntry(entry(name));
    if (!value) return null;
    try { return JSON.parse(value.toString('utf8')); } catch (error) { return null; }
  }
  function present(name) { return !!generation.readEntry(entry(name)); }
  return { state: 'ready', surface: { index: index, entry: entry, json: json, present: present } };
}

function generatedSurface(active, stem) {
  var inspected = inspectGeneratedSurface(active, stem);
  return inspected.state === 'ready' ? inspected.surface : null;
}

function generatedScreensIndex(active, stem, nodeLimit) {
  var inspected = inspectGeneratedSurface(active, stem);
  if (inspected.state === 'missing') return { present: false };
  if (inspected.state !== 'ready') return { present: false, error: 'screen-cache-invalid' };
  var surface = inspected.surface;
  var maxNodes = Number.isSafeInteger(nodeLimit) && nodeLimit >= 0
    ? Math.min(nodeLimit, MAX_SCREEN_NODES) : MAX_SCREEN_NODES;
  var allScreenNames = Object.keys(surface.index.nodes);
  var screenNames = allScreenNames.filter(validScreenName).sort();
  var limitations = screenNames.length > maxNodes ? ['surface-nodes-truncated'] : [];
  if (screenNames.length !== allScreenNames.length) limitations.push('surface-node-name-invalid');
  var nodes = screenNames.slice(0, maxNodes).map(function (screen) {
    var meta = surface.index.nodes[screen] || {};
    var sum = specSummary(surface.json(screen + '.spec.json'));
    var darkSpec = surface.json(screen + '.dark.spec.json');
    var darkSum = darkSpec ? specSummary(darkSpec) : null;
    var inst = surface.json(screen + '.instances.json');
    var kind = meta.kind;
    var variants = normalizedVariants(meta, surface.present, screen);
    var usedComponents = componentUsage(inst);
    return {
      screen: screen,
      kind: kind,
      url: canonicalFigmaHref(meta.url),
      nodeId: typeof meta.nodeId === 'string' ? meta.nodeId : null,
      fetchedAt: typeof meta.fetchedAt === 'string' ? meta.fetchedAt : null,
      frameSizeDp: sum.frameSizeDp,
      theme: sum.theme,
      elementCount: sum.elementCount,
      instanceCount: Array.isArray(inst) ? inst.length : null,
      hasPng: surface.present(screen + '.png'),
      variants: variants,
      variantLimitations: variants.limitations || [],
      usedComponents: usedComponents,
      darkTheme: darkSum ? {
        url: canonicalFigmaHref(meta.darkUrl),
        nodeId: typeof meta.darkNodeId === 'string' ? meta.darkNodeId : null,
        frameSizeDp: darkSum.frameSizeDp,
        theme: darkSum.theme,
        elementCount: darkSum.elementCount,
        hasPng: surface.present(screen + '.dark.png'),
        fetchedAt: typeof meta.darkFetchedAt === 'string' ? meta.darkFetchedAt : null
      } : null
    };
  });
  return {
    present: true, stem: stem, nodes: nodes, census: null,
    limitations: limitations
  };
}

// { present, stem, nodes: [ { screen, url, nodeId, fetchedAt, frameSizeDp,
//   theme, elementCount, instanceCount, hasPng, darkTheme } ], census }
function screensIndex(stem, activeOverride, nodeLimit) {
  if (!taskSource.safeTaskStem(stem)) return { present: false, error: 'bad-stem' };
  var active = activeOverride || generation.current();
  if (!active.ok) return { present: false, error: 'design-generation-invalid' };
  var maxNodes = Number.isSafeInteger(nodeLimit) && nodeLimit >= 0
    ? Math.min(nodeLimit, MAX_SCREEN_NODES) : MAX_SCREEN_NODES;
  return active.mode === 'generation'
    ? generatedScreensIndex(active, stem, maxNodes)
    : { present: false };
}

function screenImageFile(stem, screen, theme, activeOverride) {
  if (!taskSource.safeTaskStem(stem)) return null;
  if (!validScreenName(screen)) return null;
  var active = activeOverride || generation.current();
  if (!active.ok) return null;
  if (active.mode !== 'generation') return null;
  var generated = generatedSurface(active, stem);
  if (!generated || !Object.prototype.hasOwnProperty.call(generated.index.nodes, screen)) return null;
  var generatedEntry = generated.entry(screen + (theme === 'dark' ? '.dark.png' : '.png'));
  if (!generatedEntry || !generation.readEntry(generatedEntry)) return null;
  return generation.projectFile(generatedEntry.path);
}

function surfaceVariantImageFile(stem, screen, imageFile, activeOverride) {
  if (!taskSource.safeTaskStem(stem) || !validScreenName(screen) ||
      typeof imageFile !== 'string' || !VARIANT_FILE_RE.test(imageFile)) return null;
  var active = activeOverride || generation.current();
  if (!active.ok) return null;
  if (active.mode !== 'generation') return null;
  var generated = generatedSurface(active, stem);
  var meta = generated && generated.index.nodes && generated.index.nodes[screen];
  var variants = normalizedVariants(meta, generated ? generated.present : function () { return false; }, screen);
  if (!variants.some(function (variant) { return variant.imageFile === imageFile; })) return null;
  var entry = generated.entry(imageFile);
  return entry && generation.readEntry(entry) ? generation.projectFile(entry.path) : null;
}

function screensAll(activeOverride, nodeLimit) {
  var active = activeOverride || generation.current();
  if (!active.ok) return { present: false, stems: [] };
  var maxNodes = Number.isSafeInteger(nodeLimit) && nodeLimit >= 0
    ? Math.min(nodeLimit, MAX_SCREEN_NODES) : MAX_SCREEN_NODES;
  function collect(stemNames) {
    var remaining = maxNodes, values = [], limitations = [];
    stemNames.sort().some(function (stem) {
      if (remaining <= 0) {
        limitations.push('surface-nodes-truncated');
        return true;
      }
      var value = screensIndex(stem, active, remaining);
      if (value.error) {
        limitations.push(value.error);
        return false;
      }
      if (!value.present) return false;
      values.push(value);
      limitations = limitations.concat(value.limitations || []);
      remaining -= value.nodes.length;
      return false;
    });
    return {
      present: values.length > 0,
      stems: values,
      limitations: limitations.filter(function (item, index, list) {
        return list.indexOf(item) === index;
      })
    };
  }
  if (active.mode !== 'generation') return { present: false, stems: [] };
  var seen = Object.create(null);
  active.manifest.artifacts.forEach(function (entry) {
    var logical = /^orchestrator\/\.cache\/figma\/screens\/([A-Za-z0-9][A-Za-z0-9_-]*)\/index\.json$/.exec(entry.logicalPath);
    if (entry.group === 'surfaces' && logical && entry.role === generation.surfaceIndexRole(logical[1])) seen[logical[1]] = 1;
  });
  return collect(Object.keys(seen));
}

// Compact screen-drift summary for <stem> (.cache/figma/reports/screen-drift-<stem>.json,
// written by the figma:screens drift session -> check-screen-drift.mjs). Read-only local JSON;
// never calls Figma. { present:false } until a drift check has run.
function screenDrift(stem, activeOverride) {
  if (!taskSource.safeTaskStem(stem)) return { present: false };
  var active = activeOverride || generation.current();
  if (!active.ok) return { present: false };
  if (active.mode !== 'generation') return { present: false };
  var generatedEntry = active.manifest.artifacts.find(function (entry) {
    return entry.group === 'drift' && entry.logicalPath === 'orchestrator/.cache/figma/reports/screen-drift-' + stem + '.json';
  });
  if (!generatedEntry) return { present: false };
  var rep = (function () {
    var bytes = generation.readEntry(generatedEntry);
    try { return bytes && JSON.parse(bytes.toString('utf8')); } catch (error) { return null; }
  })();
  if (!rep || !Array.isArray(rep.screens)) {
    return { present: false, error: 'screen-drift-invalid', limitations: ['surface-drift-invalid'] };
  }
  var limitations = rep.screens.length > MAX_SCREEN_DRIFT_ROWS ? ['surface-drift-truncated'] : [];
  var screens = rep.screens.slice(0, MAX_SCREEN_DRIFT_ROWS).map(function (s) {
    if (!s || typeof s !== 'object' || !validScreenName(s.screen)) {
      limitations.push('surface-drift-row-invalid');
      return null;
    }
    return {
      screen: s.screen,
      theme: s.theme === 'dark' ? 'dark' : 'primary',
      status: (s.status === 'DRIFTED' || s.status === 'CLEAN' || s.status === 'NOT_CHECKED') ? s.status : 'NOT_CHECKED',
      changes: Array.isArray(s.changes) ? s.changes.slice(0, 30).map(String) : []
    };
  }).filter(Boolean);
  return {
    present: true,
    overall: (rep.overall === 'PASS' || rep.overall === 'WARN' || rep.overall === 'BLOCKER' || rep.overall === 'INCOMPLETE') ? rep.overall : 'UNKNOWN',
    generatedAt: typeof rep.generatedAt === 'string' ? rep.generatedAt : null,
    drifted: screens.filter(function (s) { return s.status === 'DRIFTED'; }).length,
    screens: screens,
    limitations: limitations.filter(function (item, index, list) {
      return list.indexOf(item) === index;
    })
  };
}

// Post-ship drift: read the committed drift-stale marker (written by
// sweep-done-drift.mjs into orchestrator/tasks/evidence/figma-ship/<stem>/) that means the Figma
// design MOVED since this task was certified. Read-only local JSON; never calls Figma.
// { present:false } until a sweep has marked this task stale.
function shipDrift(stem) {
  if (!taskSource.safeTaskStem(stem)) return { present: false, error: 'bad-stem' };
  var directory = path.join(paths.TASKS_DIR, 'evidence', 'figma-ship', stem);
  var file = path.join(directory, 'drift-stale-' + stem + '.json');
  var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, directory, file);
  if (inspected && inspected.status === 'missing') return { present: false };
  if (!inspected || inspected.status !== 'present' || !inspected.stat || !inspected.stat.isFile() ||
      inspected.stat.isSymbolicLink() || String(inspected.stat.nlink) !== '1') {
    return { present: false, error: 'ship-drift-invalid' };
  }
  var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, directory, file, shipDriftContract.MAX_BYTES);
  if (!hit) return { present: false, error: 'ship-drift-invalid' };
  var marker;
  try { marker = JSON.parse(hit.bytes.toString('utf8')); }
  catch (error) { return { present: false, error: 'ship-drift-invalid' }; }
  if (!shipDriftContract.validMarker(marker, stem)) return { present: false, error: 'ship-drift-invalid' };
  return {
    present: true,
    stale: true,
    staleAt: marker.staleAt,
    baselineRunId: marker.baselineRunId,
    driftedCount: marker.driftedCount,
    driftedScreens: marker.driftedScreens.slice(0, 30).map(function (s) {
      return {
        screen: s.screen,
        theme: s.theme,
        changes: s.changes.slice()
      };
    })
  };
}

module.exports = {
  MAX_SCREEN_DRIFT_ROWS: MAX_SCREEN_DRIFT_ROWS,
  validScreenName: validScreenName,
  validScreenIndex: validScreenIndex,
  screensIndex: screensIndex,
  screenImageFile: screenImageFile,
  surfaceVariantImageFile: surfaceVariantImageFile,
  screensAll: screensAll,
  screenDrift: screenDrift,
  shipDrift: shipDrift,
  canonicalFigmaHref: canonicalFigmaHref
};
