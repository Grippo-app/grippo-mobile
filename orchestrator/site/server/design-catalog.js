'use strict';

// Generation-bound, normalized read model for Project -> Design. One snapshot
// resolves the current pointer once and every projection in that snapshot is
// built from that exact generation. Before the first committed generation the
// API returns an explicit empty "none" state and never mixes independently
// updated pre-generation files into the Design workspace.

var crypto = require('crypto');
var generation = require('./figma-generation');
var figmaScreens = require('./figma-screens');
var tokenState = require('./design-token-state');
var componentState = require('./design-component-state');
var relations = require('./design-relations');
var preview = require('./design-preview');
var taskSource = require('./task-source');
var syncHistory = require('./figma-sync-history');
var tokenIdentity = require('../../figma/runtime/token-identity.cjs');

var DEFAULT_LIMIT = 100;
var MAX_LIMIT = 500;
var RESPONSE_MAX = 1024 * 1024;
var MAX_TOKEN_ROWS = 10000;
var MAX_TOKEN_MODES = 50;
var MAX_COMPONENT_ROWS = 10000;
var MAX_SURFACE_ROWS = 10000;
var ANALYZER_VERSION = 'design-catalog-v2';
var snapshotCache = null;
var STATUS = Object.assign(Object.create(null), {
  healthy: 1, drifted: 1, missing: 1, unknown: 1
});
// Token rows carry the comparator's own status vocabulary — the server never
// recomputes business status from raw fields (REQ-CMP-002/REQ-UI-001).
var TOKEN_STATUS = Object.assign(Object.create(null), {
  matched: 1, 'value-drift': 1, 'missing-in-project': 1, 'not-compared': 1,
  unbound: 1, 'ambiguous-binding': 1, 'authority-conflict': 1,
  'context-map-required': 1, 'source-conflict': 1, unsupported: 1,
  'not-observed': 1, ignored: 1
});
var TOKEN_CHANGED_SIDE = Object.assign(Object.create(null), {
  none: 1, design: 1, project: 1, both: 1, mapping: 1, unknown: 1
});
// Comparator-owned project-only classification axis (token-comparator-v1).
var PROJECT_TOKEN_CLASSIFICATION = Object.assign(Object.create(null), {
  unclassified: 1, superseded: 1, 'project-only-intentional': 1
});
// Component rows carry the component comparator's own status vocabulary
// (component-comparison schemaVersion 2); the catalog only projects it.
var COMPONENT_STATUS = Object.assign(Object.create(null), {
  matched: 1, drifted: 1, unmapped: 1, ambiguous: 1, 'missing-in-project': 1,
  'missing-in-design': 1, 'design-only': 1, ignored: 1, unsupported: 1
});
var COMPONENT_CHANGED_SIDE = Object.assign(Object.create(null), {
  none: 1, design: 1, project: 1, both: 1, mapping: 1, unknown: 1
});
var PROJECT_COMPONENT_CLASSIFICATION = Object.assign(Object.create(null), {
  unclassified: 1, 'intentionally-project-only': 1, external: 1, deprecated: 1,
  superseded: 1, deferred: 1, ignored: 1
});
var COMPONENT_MAPPING_STATE = Object.assign(Object.create(null), {
  active: 1, 'target-out-of-scope': 1, incompatible: 1, 'orphaned-project': 1, 'orphaned-design': 1
});
var COMPONENT_VISUAL = Object.assign(Object.create(null), {
  'not-run': 1, 'not-applicable': 1, 'insufficient-evidence': 1,
  matched: 1, 'review-required': 1, drifted: 1
});

function hash(value) {
  return 'sha256:' + crypto.createHash('sha256').update(
    Buffer.isBuffer(value) ? value : Buffer.from(JSON.stringify(value), 'utf8')
  ).digest('hex');
}
function stableId(kind, value) {
  return kind + '-' + crypto.createHash('sha256').update(String(value), 'utf8').digest('hex').slice(0, 24);
}
function safeString(value, max) {
  var out = typeof value === 'string' ? value.replace(/[\x00-\x1f\x7f]/g, '').normalize('NFC') : '';
  return out.length > (max || 500) ? out.slice(0, max || 500) : out;
}
function safePath(value) {
  var out = safeString(value, 500).replace(/\\/g, '/');
  if (!out || out.charAt(0) === '/' || /^[A-Za-z]:\//.test(out)) return null;
  return out.split('/').some(function (segment) {
    return !segment || segment === '.' || segment === '..';
  }) ? null : out;
}
function safeFigmaUrl(value) {
  return figmaScreens.canonicalFigmaHref(value);
}
function revisionMeta(active, codeSideRevision) {
  if (!active.ok) {
    return { ok: false, status: 409, error: 'design-generation-invalid' };
  }
  var generationRevision = active.mode === 'generation'
    ? active.pointer.manifestHash
    : hash({ mode: 'none', analyzer: ANALYZER_VERSION });
  return {
    ok: true,
    schemaVersion: 1,
    committedGenerationId: active.mode === 'generation' ? active.manifest.generationId : null,
    generationRevision: generationRevision,
    generationMode: active.mode === 'generation' ? 'generation' : 'none',
    designManifest: {
      figmaArtifactInputFingerprint: active.mode === 'generation' ? generation.sourceFingerprint(active.manifest) : null,
      codeSideRevision: codeSideRevision
    }
  };
}
function checkRevision(snapshot, expected) {
  if (expected && expected !== snapshot.generationRevision) {
    return {
      ok: false, status: 409, error: 'design-generation-conflict',
      schemaVersion: 1, committedGenerationId: snapshot.committedGenerationId,
      generationRevision: snapshot.generationRevision, generationMode: snapshot.generationMode
    };
  }
  return null;
}
function jsonRole(active, role) {
  if (active.mode !== 'generation') return null;
  var entry = active.manifest.artifacts.find(function (row) { return row.role === role; });
  if (!entry) return null;
  var bytes = generation.readEntry(entry);
  if (!bytes) {
    var missing = new Error('DESIGN_GENERATION_ARTIFACT_INVALID');
    missing.code = 'DESIGN_GENERATION_ARTIFACT_INVALID';
    throw missing;
  }
  try { return JSON.parse(bytes.toString('utf8')); }
  catch (error) {
    var invalid = new Error('DESIGN_GENERATION_ARTIFACT_INVALID');
    invalid.code = 'DESIGN_GENERATION_ARTIFACT_INVALID';
    throw invalid;
  }
}
function domainLineage(active, id) {
  if (active.mode !== 'generation' || !active.manifest || !Array.isArray(active.manifest.domains)) return null;
  return active.manifest.domains.find(function (row) { return row.id === id; }) || null;
}
function safeModeValues(byMode) {
  var out = Object.create(null);
  if (!byMode || typeof byMode !== 'object' || Array.isArray(byMode)) return out;
  Object.keys(byMode).sort().slice(0, MAX_TOKEN_MODES).forEach(function (mode) {
    var publicMode = safeString(mode, 120);
    var value = safeString(String(byMode[mode]), 500);
    if (publicMode && !Object.prototype.hasOwnProperty.call(out, publicMode)) out[publicMode] = value;
  });
  return out;
}
function tokenSuggestion(raw) {
  if (!raw || typeof raw !== 'object') return null;
  return {
    projectTokenId: safeString(raw.projectTokenId, 400),
    adapterId: safeString(raw.adapterId, 64),
    confidence: safeString(raw.confidence, 40),
    competitors: Number.isSafeInteger(raw.competitors) ? raw.competitors : 0,
    signals: (Array.isArray(raw.signals) ? raw.signals : []).slice(0, 8).map(function (signal) {
      return { signal: safeString(signal && signal.signal, 40), detail: safeString(signal && signal.detail, 300) };
    })
  };
}
// The comparator's immutable report is the sole authority for token statuses.
// The catalog only projects it, joins live currency signals, and never
// synthesizes a missing/matched verdict for an absent row (REQ-CMP-008).
function tokenComparisonState(active) {
  var report = active.mode === 'generation' ? jsonRole(active, 'token-comparison') : null;
  if (!report || typeof report !== 'object' || report.schemaVersion !== 2 ||
      !Array.isArray(report.observedRows) || !Array.isArray(report.projectOnly) ||
      !report.inputs || typeof report.inputs !== 'object') {
    return { present: false, report: null };
  }
  return { present: true, report: report };
}
function latestSyncAttempt(group) {
  var record = syncHistory.latestForGroup(group);
  if (!record) return null;
  var groupState = Array.isArray(record.groups) ? record.groups.find(function (row) {
    return row && row.group === group;
  }) : null;
  return {
    id: safeString(record.id, 40),
    result: safeString(record.result, 20),
    groupStatus: safeString(groupState && groupState.status, 20),
    errorCode: safeString(record.errorCode, 80) || null,
    finishedAt: safeString(record.finishedAt, 40) || null
  };
}
function syncFailureState(attempt, domain) {
  if (!attempt || attempt.groupStatus !== 'failed' ||
      attempt.result !== 'failed' && attempt.result !== 'partial') return null;
  var code = attempt.errorCode;
  var prefix = domain === 'tokens' ? 'token-source-' : 'component-design-';
  if (code === prefix + 'access-degraded') return 'access-degraded';
  if (code === prefix + 'capture-incomplete') return 'capture-incomplete';
  if (code === prefix + 'capture-inconsistent') return 'capture-inconsistent';
  if (code === prefix + 'capture-invalid') return 'capture-invalid';
  return 'sync-failed';
}
function projectSyncFailure(base, attempt, domain) {
  var state = syncFailureState(attempt, domain);
  return state ? Object.assign(base, {
    state: state,
    reason: attempt.errorCode || 'sync-failed',
    checkedAt: attempt.finishedAt,
    syncAttempt: attempt,
    action: { scope: domain, href: '#figma?sync=' + domain }
  }) : null;
}
function tokenAnalysis(active, comparison, tokenSignals, syncAttempt) {
  var catalog = active.mode === 'generation' ? jsonRole(active, 'observed-token-catalog') : null;
  var sourceIndex = active.mode === 'generation' ? jsonRole(active, 'observed-token-source-index') : null;
  var domainReady = !!catalog && !!sourceIndex &&
    catalog.sourceIndexHash === sourceIndex.semanticHash;
  var comparisonLineage = domainLineage(active, 'token-drift');
  var base = {
    target: 'tokens',
    state: 'unavailable',
    reason: 'source-not-synced',
    adapters: tokenSignals.adapters,
    designSynced: domainReady,
    checkedAt: comparisonLineage && comparisonLineage.syncedAt || null,
    blockers: [],
    action: { scope: 'tokens', href: '#figma?sync=tokens' }
  };
  var failedSync = projectSyncFailure(base, syncAttempt, 'tokens');
  if (failedSync) return failedSync;
  if (!domainReady) return base;
  if (tokenSignals.adapters.state === 'unconfigured') {
    return Object.assign(base, { state: 'adapters-unconfigured', reason: 'project-adapters-unconfigured', action: { scope: 'adapters' } });
  }
  if (tokenSignals.adapters.state === 'unreadable') {
    return Object.assign(base, { state: 'adapters-invalid', reason: 'project-adapter-config-invalid', action: { scope: 'adapters' } });
  }
  if (tokenSignals.mappingState === 'invalid') {
    return Object.assign(base, { state: 'mapping-invalid', reason: 'token-mapping-registry-invalid', action: { scope: 'mappings' } });
  }
  if (!comparison.present) {
    return Object.assign(base, { state: 'not-checked', reason: 'comparison-not-run', action: { scope: 'drift' } });
  }
  var inputs = comparison.report.inputs;
  var blockers = (Array.isArray(comparison.report.blockers) ? comparison.report.blockers : []).slice(0, 32).map(function (blocker) {
    return { code: safeString(blocker && blocker.code, 80) };
  });
  base.blockers = blockers;
  base.comparisonSemanticHash = generation.HASH_RE.test(String(comparison.report.semanticHash || ''))
    ? comparison.report.semanticHash : null;
  base.action = { scope: 'drift' };
  var designStale = inputs.observedCatalogHash !== catalog.semanticHash ||
    inputs.sourceIndexHash !== sourceIndex.semanticHash;
  var mappingStale = tokenSignals.mappingRevision !== null && inputs.mappingRevision !== tokenSignals.mappingRevision;
  var configStale = tokenSignals.configFileHash !== null && inputs.adapterConfigHash !== tokenSignals.configFileHash;
  var projectStale = tokenSignals.projectDirty === true;
  if (designStale) return Object.assign(base, { state: 'stale-design', reason: 'design-inventory-newer-than-comparison' });
  if (mappingStale) return Object.assign(base, { state: 'stale-mapping', reason: 'mapping-revision-changed' });
  if (configStale) return Object.assign(base, { state: 'stale-config', reason: 'adapter-config-changed' });
  if (projectStale) return Object.assign(base, { state: 'stale-project', reason: 'project-sources-changed' });
  if (inputs.sourceFreshness === 'unknown') {
    return Object.assign(base, { state: 'source-health-unknown', reason: 'source-health-unavailable' });
  }
  if (inputs.sourceFreshness === 'stale') {
    return Object.assign(base, { state: 'source-refresh-required', reason: 'source-refresh-required' });
  }
  if (comparison.report.complete !== true || comparison.report.operationalState !== 'current') {
    return Object.assign(base, { state: 'blocked', reason: 'token-comparison-blocked' });
  }
  return Object.assign(base, { state: 'current', reason: 'comparison-current' });
}
// Everything the server-side task creator needs to freeze a machine-readable
// design-origin binding: exact identities and revisions from the immutable
// artifacts, never from display strings.
function tokenBindingContext(active, report) {
  var catalog = jsonRole(active, 'observed-token-catalog');
  var sourceIndex = jsonRole(active, 'observed-token-source-index');
  var bindings = jsonRole(active, 'token-binding-snapshot');
  if (!catalog || !sourceIndex || !bindings || !report || report.complete !== true ||
      report.operationalState !== 'current' || !report.inputs ||
      report.inputs.sourceFreshness !== 'current') return null;
  var index = jsonRole(active, 'project-token-analysis-index');
  var adapterModes = Object.create(null);
  var projectPathById = Object.create(null);
  if (index && Array.isArray(index.adapters)) {
    index.adapters.slice(0, 8).forEach(function (row) {
      var inventory = jsonRole(active, row.role);
      if (!inventory || !Array.isArray(inventory.tokens)) return;
      adapterModes[row.adapterId] = Array.isArray(inventory.modes) ? inventory.modes.slice(0, 16) : [];
      inventory.tokens.slice(0, MAX_TOKEN_ROWS).forEach(function (token) {
        if (token && typeof token.projectTokenId === 'string' && Array.isArray(token.semanticPath)) {
          projectPathById[token.projectTokenId] = token.semanticPath.slice(0, 16);
        }
      });
    });
  }
  return {
    observedScope: catalog.scope,
    catalogHash: report.inputs.observedCatalogHash,
    sourceIndexHash: report.inputs.sourceIndexHash,
    bindingSnapshotHash: report.inputs.bindingSnapshotHash,
    comparisonSemanticHash: report.semanticHash,
    mappingRevision: report.inputs.mappingRevision,
    adapterModes: adapterModes,
    defaultAdapterId: index && Array.isArray(index.adapters) && index.adapters.length === 1
      ? index.adapters[0].adapterId : null,
    projectPathById: projectPathById
  };
}
function tokenRows(active, tokenSignals, syncAttempt) {
  var comparison = tokenComparisonState(active);
  var analysis = tokenAnalysis(active, comparison, tokenSignals, syncAttempt);
  if (!comparison.present) {
    return { rows: [], projectOnly: [], coverage: null, analysis: analysis, bindingContext: null,
      limitations: analysis.state === 'unavailable' ? ['tokens-not-available'] : [] };
  }
  var report = comparison.report;
  var catalog = jsonRole(active, 'observed-token-catalog');
  var bindingSnapshot = jsonRole(active, 'token-binding-snapshot');
  var mappingSnapshot = jsonRole(active, 'token-mapping-snapshot');
  var index = jsonRole(active, 'project-token-analysis-index');
  if (!catalog || !bindingSnapshot || !mappingSnapshot || !index) {
    return { rows: [], projectOnly: [], coverage: null, analysis: Object.assign(analysis, {
      state: 'unavailable', reason: 'token-drift-domain-incomplete'
    }), bindingContext: null, limitations: ['token-drift-domain-incomplete'] };
  }
  var limitations = [], truncated = false;
  var tokenByKey = new Map(catalog.tokens.map(function (token) { return [token.observedTokenKey, token]; }));
  var projectById = new Map();
  index.adapters.forEach(function (adapter) {
    var inventory = jsonRole(active, adapter.role);
    (inventory && inventory.tokens || []).forEach(function (token) {
      projectById.set(token.projectTokenId, { adapterId: adapter.adapterId, token: token });
    });
  });
  var bindingByKey = new Map(bindingSnapshot.bindings.map(function (binding) {
    return [binding.observedTokenKey + '\0' + binding.contextKey, binding];
  }));
  var suggestionsByKey = new Map();
  bindingSnapshot.suggestions.forEach(function (suggestion) {
    var key = suggestion.observedTokenKey + '\0' + suggestion.contextKey;
    var list = suggestionsByKey.get(key) || [];
    list.push(suggestion);
    suggestionsByKey.set(key, list);
  });
  function parsedContext(value) {
    try { return tokenIdentity.parseContextKey(value); }
    catch (error) {
      var invalid = new Error('DESIGN_GENERATION_ARTIFACT_INVALID');
      invalid.code = 'DESIGN_GENERATION_ARTIFACT_INVALID';
      throw invalid;
    }
  }
  function selectorMatches(context, selector) {
    return Object.keys(selector || {}).every(function (key) { return context[key] === selector[key]; });
  }
  function displayValue(value) {
    if (value === undefined) return null;
    try { return safeString(JSON.stringify(value), 500); } catch (error) { return safeString(String(value), 500); }
  }
  function statusOf(raw) {
    if (raw.lifecycleStatus === 'not-observed') return 'not-observed';
    if (raw.sourceStatus === 'source-conflict') return 'source-conflict';
    if (raw.sourceStatus === 'unsupported') return 'unsupported';
    if (raw.bindingStatus !== 'manual-bound' && raw.bindingStatus !== 'task-bound' &&
        raw.bindingStatus !== 'auto-bound' && raw.bindingStatus !== 'expected-target-missing') {
      return raw.bindingStatus;
    }
    return raw.valueStatus;
  }
  function changedSideOf(direction) {
    if (direction === 'design-observation-changed') return 'design';
    if (direction === 'project-changed') return 'project';
    if (direction === 'both-changed') return 'both';
    if (direction === 'unchanged') return 'none';
    return 'unknown';
  }
  var rows = [];
  report.observedRows.slice(0, MAX_TOKEN_ROWS).forEach(function (raw) {
    if (!raw || typeof raw !== 'object') return;
    var status = statusOf(raw);
    if (!Object.prototype.hasOwnProperty.call(TOKEN_STATUS, status)) {
      limitations.push('token-row-status-unknown');
      return;
    }
    var token = tokenByKey.get(raw.observedTokenKey);
    if (!token) {
      limitations.push('token-row-catalog-join-missing');
      return;
    }
    var context = parsedContext(raw.contextKey);
    var key = raw.observedTokenKey + '\0' + raw.contextKey;
    var binding = bindingByKey.get(key) || null;
    var mappings = mappingSnapshot.mappings.filter(function (mapping) {
      return mapping.state === 'active' && mapping.observedTokenKey === raw.observedTokenKey &&
        selectorMatches(context, mapping.contextSelector);
    });
    var mapping = mappings.length === 1 ? mappings[0] : null;
    var disposition = mappingSnapshot.dispositions.find(function (candidate) {
      return candidate.target.side === 'observed' &&
        candidate.target.observedTokenKey === raw.observedTokenKey;
    }) || null;
    var project = raw.projectTokenId ? projectById.get(raw.projectTokenId) : null;
    var kind = null;
    var coordinate = token.coordinates.find(function (candidate) {
      return JSON.stringify(Object.keys(candidate.context).sort().reduce(function (out, name) {
        out[name] = candidate.context[name]; return out;
      }, {})) === raw.contextKey;
    });
    if (coordinate && coordinate.values.length === 1) kind = coordinate.values[0].kind;
    var changedSide = changedSideOf(raw.direction);
    var designDisplay = displayValue(raw.designValue);
    var projectDisplay = displayValue(raw.projectValue);
    var figmaValues = Object.create(null);
    var codeValues = Object.create(null);
    if (designDisplay !== null) figmaValues[raw.contextKey] = designDisplay;
    if (projectDisplay !== null && raw.projectMode) codeValues[raw.projectMode] = projectDisplay;
    var projectRefs = [];
    if (binding) {
      projectRefs.push({
        projectTokenId: safeString(binding.projectTokenId, 400) || null,
        present: binding.targetState === 'present',
        sourcePath: project ? safePath(project.token.source && project.token.source.path) : null,
        sourceSymbol: project ? safeString(project.token.source && project.token.source.symbol, 400) || null : null,
        intendedSemanticPath: binding.projectSemanticPath.slice(0, 32)
      });
    }
    var row = {
      id: stableId('tok', raw.observedTokenKey + '\0' + raw.contextKey),
      entityType: 'token',
      findingId: safeString(raw.findingId, 40) || null,
      observedTokenKey: safeString(raw.observedTokenKey, 160),
      contextKey: safeString(raw.contextKey, 256),
      context: context,
      name: safeString(raw.providerName, 300),
      displayPath: token.displayPath.slice(0, 32),
      kind: safeString(kind, 20),
      status: status,
      statusDetail: [raw.lifecycleStatus, raw.sourceStatus, raw.bindingStatus, raw.valueStatus].join(' · '),
      lifecycleStatus: raw.lifecycleStatus,
      sourceStatus: raw.sourceStatus,
      bindingStatus: raw.bindingStatus,
      valueStatus: raw.valueStatus,
      direction: raw.direction,
      changedSide: changedSide,
      change: {
        changedSide: changedSide,
        designChanged: raw.direction === 'design-observation-changed' || raw.direction === 'both-changed',
        projectChanged: raw.direction === 'project-changed' || raw.direction === 'both-changed',
        mappingChanged: false,
        confidence: raw.direction === 'unknown-no-baseline' ? 'none' : 'exact'
      },
      mappingId: mapping ? mapping.mappingId : null,
      mappingState: raw.bindingStatus,
      relation: mapping ? mapping.relation : binding && binding.relation || null,
      adapterId: safeString(raw.adapterId || binding && binding.adapterId, 64) || null,
      dispositionId: disposition ? disposition.dispositionId : null,
      frozenValue: raw.designValue,
      figmaValues: figmaValues,
      codeValues: codeValues,
      projectRefs: projectRefs,
      modeResults: [{
        designContextKey: raw.contextKey,
        projectMode: safeString(raw.projectMode, 60) || null,
        result: raw.valueStatus,
        designValue: designDisplay,
        projectValue: projectDisplay
      }],
      lifecycle: [{ kind: raw.lifecycleStatus, detail: 'Observed-token lifecycle; not file-wide deletion proof.' }],
      suggestions: (suggestionsByKey.get(key) || []).slice(0, 5).map(tokenSuggestion).filter(Boolean),
      limitations: raw.limitations.slice(0, 16).map(function (item) { return safeString(String(item), 200); }),
      lastChanged: analysis.checkedAt,
      openTask: null
    };
    row.searchText = [row.name, row.observedTokenKey, row.contextKey, row.kind, row.status,
      row.lifecycleStatus, row.sourceStatus, row.bindingStatus, row.valueStatus, row.direction, row.adapterId]
      .concat(row.projectRefs.map(function (ref) {
        return [ref.projectTokenId, ref.sourcePath, ref.sourceSymbol].filter(Boolean).join(' ');
      })).filter(Boolean).join(' ').slice(0, 20000).toLowerCase();
    rows.push(row);
  });
  if (report.observedRows.length > MAX_TOKEN_ROWS) {
    truncated = true;
    limitations.push('token-catalog-truncated');
  }
  var projectOnly = [];
  report.projectOnly.slice(0, MAX_TOKEN_ROWS).forEach(function (raw) {
    if (!raw || typeof raw !== 'object') return;
    var project = projectById.get(raw.projectTokenId);
    var disposition = mappingSnapshot.dispositions.find(function (candidate) {
      return candidate.target.side === 'project' &&
        candidate.target.adapterId === raw.adapterId &&
        candidate.target.projectTokenId === raw.projectTokenId;
    }) || null;
    var entry = {
      id: stableId('tokp', raw.projectTokenId),
      entityType: 'project-token',
      findingId: safeString(raw.findingId, 40) || null,
      projectTokenId: safeString(raw.projectTokenId, 400),
      adapterId: safeString(raw.adapterId, 64),
      name: safeString(raw.displayName, 300),
      kind: safeString(raw.kind, 20),
      layer: project ? safeString(project.token.layer, 40) || null : null,
      classification: disposition && disposition.kind === 'project-only-intentional'
        ? 'project-only-intentional' : safeString(raw.classification, 40),
      dispositionId: disposition ? disposition.dispositionId : null,
      sourcePath: project ? safePath(project.token.source && project.token.source.path) : null,
      sourceSymbol: project ? safeString(project.token.source && project.token.source.symbol, 400) || null : null,
      suggestions: [],
      openTask: null
    };
    entry.searchText = [entry.name, entry.projectTokenId, entry.adapterId, entry.kind, entry.layer,
      entry.classification, entry.sourcePath, entry.sourceSymbol]
      .filter(Boolean).join(' ').slice(0, 20000).toLowerCase();
    projectOnly.push(entry);
  });
  if (report.projectOnly.length > MAX_TOKEN_ROWS) {
    truncated = true;
    limitations.push('token-project-only-truncated');
  }
  var coverage = report.coverage && typeof report.coverage === 'object' ? {
    label: report.coverage.label,
    denominator: report.coverage.denominator,
    matched: report.coverage.matched,
    percent: report.coverage.denominator > 0
      ? Math.round(report.coverage.matched * 1000 / report.coverage.denominator) / 10 : null,
    valueDrift: report.coverage.valueDrift,
    missingInProject: report.coverage.missingInProject,
    unbound: report.coverage.unbound,
    excludedUnsupported: report.coverage.excludedUnsupported,
    excludedConflicting: report.coverage.excludedConflicting,
    excludedContext: report.coverage.excludedContext,
    notObserved: report.coverage.notObserved,
    projectOnly: report.coverage.projectOnly,
    partial: truncated
  } : null;
  if (analysis.blockers.length) limitations.push('token-comparison-blockers');
  if (analysis.state !== 'current' && analysis.state !== 'not-checked') limitations.push('token-comparison-stale');
  return {
    rows: rows,
    projectOnly: projectOnly,
    coverage: coverage,
    analysis: analysis,
    bindingContext: tokenBindingContext(active, report),
    limitations: limitations.filter(function (value, index, list) { return list.indexOf(value) === index; })
  };
}
// The component comparator's immutable report is the sole authority for
// component statuses. The catalog only projects it, joins live currency
// signals, and never synthesizes a verdict for an absent row.
function componentComparisonState(active) {
  var report = active.mode === 'generation' ? jsonRole(active, 'component-comparison') : null;
  if (!report || typeof report !== 'object' || report.schemaVersion !== 2 ||
      !Array.isArray(report.rows) || !Array.isArray(report.projectOnly) ||
      !report.inputs || typeof report.inputs !== 'object') {
    return { present: false, report: null };
  }
  return { present: true, report: report };
}
function componentAnalysis(active, comparison, componentSignals, syncAttempt) {
  var designEntry = active.mode === 'generation' ? active.manifest.artifacts.find(function (entry) {
    return entry.role === 'design-component-inventory';
  }) : null;
  var componentsLineage = domainLineage(active, 'components');
  var comparisonLineage = domainLineage(active, 'component-drift');
  var base = {
    target: 'components',
    state: 'unavailable',
    reason: 'source-not-synced',
    adapters: componentSignals.adapters,
    designSynced: !!designEntry,
    checkedAt: comparisonLineage && comparisonLineage.syncedAt || null,
    blockers: [],
    action: { scope: 'components', href: '#figma?sync=components' }
  };
  var failedSync = projectSyncFailure(base, syncAttempt, 'components');
  if (failedSync) return failedSync;
  if (!designEntry) return base;
  if (componentSignals.adapters.state === 'unconfigured') {
    return Object.assign(base, { state: 'adapters-unconfigured', reason: 'project-adapters-unconfigured', action: { scope: 'adapters' } });
  }
  if (componentSignals.adapters.state === 'unreadable') {
    return Object.assign(base, { state: 'adapters-invalid', reason: 'project-adapter-config-invalid', action: { scope: 'adapters' } });
  }
  if (componentSignals.mappingState === 'invalid') {
    return Object.assign(base, { state: 'mapping-invalid', reason: 'component-mapping-registry-invalid', action: { scope: 'mappings' } });
  }
  if (!comparison.present) {
    return Object.assign(base, { state: 'not-checked', reason: 'comparison-not-run', action: { scope: 'drift' } });
  }
  var inputs = comparison.report.inputs;
  var blockers = (Array.isArray(comparison.report.blockers) ? comparison.report.blockers : []).slice(0, 32).map(function (blocker) {
    return { code: safeString(blocker && blocker.code, 80) };
  });
  base.blockers = blockers;
  base.comparisonSemanticHash = generation.HASH_RE.test(String(comparison.report.semanticHash || ''))
    ? comparison.report.semanticHash : null;
  base.action = { scope: 'drift' };
  var designStale = componentsLineage && inputs.designGenerationId !== componentsLineage.sourceGenerationId;
  var mappingStale = componentSignals.mappingRevision !== null && inputs.mappingRevision !== componentSignals.mappingRevision;
  var configStale = componentSignals.configFileHash !== null && inputs.adapterConfigFileHash !== componentSignals.configFileHash;
  var projectStale = componentSignals.projectDirty === true;
  if (designStale) return Object.assign(base, { state: 'stale-design', reason: 'design-inventory-newer-than-comparison' });
  if (mappingStale) return Object.assign(base, { state: 'stale-mapping', reason: 'mapping-revision-changed' });
  if (configStale) return Object.assign(base, { state: 'stale-config', reason: 'adapter-config-changed' });
  if (projectStale) return Object.assign(base, { state: 'stale-project', reason: 'project-sources-changed' });
  return Object.assign(base, { state: 'current', reason: 'comparison-current' });
}
// Everything the server-side task creator needs to freeze a machine-readable
// design-origin component binding: exact identities, hashes and bounded spec
// copies from the immutable artifacts, never from display strings.
function componentBindingContext(active, report) {
  var design = jsonRole(active, 'design-component-inventory');
  if (!design || !report) return null;
  var index = jsonRole(active, 'project-component-analysis-index');
  var adapterPlatforms = Object.create(null);
  var projectComponentsById = Object.create(null);
  if (index && Array.isArray(index.adapters)) {
    index.adapters.slice(0, 8).forEach(function (row) {
      if (!row || typeof row.adapterId !== 'string') return;
      adapterPlatforms[row.adapterId] = safeString(row.platform, 60) || null;
      var inventory = jsonRole(active, row.role);
      if (!inventory || !Array.isArray(inventory.components)) return;
      inventory.components.slice(0, MAX_COMPONENT_ROWS).forEach(function (component) {
        if (!component || typeof component.projectComponentId !== 'string') return;
        projectComponentsById[component.projectComponentId] = {
          adapterId: row.adapterId,
          sourceSymbol: component.source && safeString(component.source.symbol, 320) || null,
          variantProperties: (Array.isArray(component.variantProperties) ? component.variantProperties : []).slice(0, 40).map(function (property) {
            return {
              projectPropertyId: safeString(property && property.projectPropertyId, 220),
              name: safeString(property && property.name, 200),
              values: (Array.isArray(property && property.values) ? property.values : []).slice(0, 64).map(function (value) {
                return safeString(String(value), 200);
              })
            };
          })
        };
      });
    });
  }
  var byDesignComponentId = Object.create(null);
  (Array.isArray(design.components) ? design.components : []).slice(0, MAX_COMPONENT_ROWS).forEach(function (component) {
    if (!component || typeof component.designComponentId !== 'string') return;
    var visual = component.visualEvidence && typeof component.visualEvidence === 'object' ? component.visualEvidence : null;
    byDesignComponentId[component.designComponentId] = {
      name: safeString(component.name, 300),
      kind: component.kind === 'component-set' ? 'component-set' : 'component',
      nodeId: component.providerIdentity && safeString(component.providerIdentity.nodeId, 80) || null,
      structuralHash: generation.HASH_RE.test(String(component.structuralHash || '')) ? component.structuralHash : null,
      sourceHash: generation.HASH_RE.test(String(component.sourceHash || '')) ? component.sourceHash : null,
      frozenSpec: {
        name: safeString(component.name, 300),
        properties: (Array.isArray(component.properties) ? component.properties : []).slice(0, 40).map(function (property) {
          var out = {
            propertyId: safeString(property && property.propertyId, 160),
            name: safeString(property && property.name, 200),
            type: safeString(property && property.type, 40)
          };
          if (property && Array.isArray(property.options)) {
            out.options = property.options.slice(0, 64).map(function (value) { return safeString(String(value), 200); });
          }
          if (property && property.defaultValue !== undefined &&
              (typeof property.defaultValue === 'string' || typeof property.defaultValue === 'boolean')) {
            out.defaultValue = typeof property.defaultValue === 'string'
              ? safeString(property.defaultValue, 500) : property.defaultValue;
          }
          return out;
        }),
        variants: (Array.isArray(component.variants) ? component.variants : []).slice(0, 500).map(function (variant) {
          var assignments = Object.create(null);
          if (variant && variant.assignments && typeof variant.assignments === 'object' && !Array.isArray(variant.assignments)) {
            Object.keys(variant.assignments).sort().slice(0, 40).forEach(function (key) {
              var cleanKey = safeString(key, 160);
              if (cleanKey && !Object.prototype.hasOwnProperty.call(assignments, cleanKey)) {
                assignments[cleanKey] = safeString(String(variant.assignments[key]), 200);
              }
            });
          }
          return {
            variantId: safeString(variant && variant.variantId, 80),
            assignments: assignments,
            isDefault: !!(variant && variant.isDefault)
          };
        }),
        slots: (Array.isArray(component.semanticSlots) ? component.semanticSlots : []).slice(0, 128).map(function (slot) {
          return {
            slotId: safeString(slot && slot.slotId, 260),
            kind: safeString(slot && slot.kind, 40),
            name: safeString(slot && slot.name, 200)
          };
        }),
        tokenRefs: (Array.isArray(component.tokenRefs) ? component.tokenRefs : []).slice(0, 128).map(function (ref) {
          return {
            observedTokenKey: safeString(ref && ref.observedTokenKey, 80),
            contextKey: safeString(ref && ref.contextKey, 256),
            sourceId: safeString(ref && ref.sourceId, 80),
            providerName: safeString(ref && ref.providerName, 512),
            field: safeString(ref && ref.field, 120)
          };
        })
      },
      visualHashes: (visual && Array.isArray(visual.entries) ? visual.entries : []).slice(0, 64).map(function (entry) {
        var match = /^sha256:([a-f0-9]{64})$/.exec(String(entry && entry.imageHash || ''));
        return match ? match[1].slice(0, 32) : null;
      }).filter(Boolean)
    };
  });
  var componentsLineage = domainLineage(active, 'components');
  return {
    designScopeId: safeString(design.scopeId, 200),
    designGenerationId: componentsLineage ? componentsLineage.sourceGenerationId : active.manifest.generationId,
    designInventoryHash: report.inputs.designInventoryHash,
    comparisonSemanticHash: report.semanticHash,
    mappingRevision: report.inputs.mappingRevision,
    defaultAdapterId: index && Array.isArray(index.adapters) && index.adapters.length === 1
      ? index.adapters[0].adapterId : null,
    adapterPlatforms: adapterPlatforms,
    projectComponentsById: projectComponentsById,
    byDesignComponentId: byDesignComponentId
  };
}
function componentFinding(raw) {
  if (!raw || typeof raw !== 'object') return null;
  var out = {
    findingId: safeString(raw.findingId, 40),
    family: safeString(raw.family, 60),
    severity: safeString(raw.severity, 20),
    detail: safeString(raw.detail, 500)
  };
  if (raw.designPropertyId) out.designPropertyId = safeString(raw.designPropertyId, 160);
  if (raw.designSlotId) out.designSlotId = safeString(raw.designSlotId, 260);
  if (raw.observedTokenKey) out.observedTokenKey = safeString(raw.observedTokenKey, 80);
  if (raw.adapterId) out.adapterId = safeString(raw.adapterId, 64);
  if (raw.platform) out.platform = safeString(raw.platform, 60);
  if (raw.suppressesTask === true) out.suppressesTask = true;
  return out;
}
function componentSuggestionCandidate(raw) {
  if (!raw || typeof raw !== 'object') return null;
  return {
    projectComponentId: safeString(raw.projectComponentId, 400),
    adapterId: safeString(raw.adapterId, 64),
    platform: safeString(raw.platform, 60),
    band: ['strong', 'moderate', 'weak'].indexOf(raw.band) >= 0 ? raw.band : 'weak',
    signals: (Array.isArray(raw.signals) ? raw.signals : []).slice(0, 8).map(function (signal) {
      return { kind: safeString(signal && signal.kind, 40), detail: safeString(signal && signal.detail, 300) };
    }),
    matchedProperties: (Array.isArray(raw.matchedProperties) ? raw.matchedProperties : []).slice(0, 40).map(function (item) {
      return safeString(String(item), 400);
    }),
    unmatchedProperties: (Array.isArray(raw.unmatchedProperties) ? raw.unmatchedProperties : []).slice(0, 40).map(function (item) {
      return safeString(String(item), 400);
    }),
    conflicts: (Array.isArray(raw.conflicts) ? raw.conflicts : []).slice(0, 8).map(function (item) {
      return safeString(String(item), 300);
    }),
    sourcePath: safePath(raw.sourcePath),
    sourceSymbol: safeString(raw.sourceSymbol, 320) || null
  };
}
function componentDimensions(raw) {
  var value = raw && typeof raw === 'object' ? raw : {};
  function counts(block, keys) {
    var source = block && typeof block === 'object' ? block : {};
    var out = {};
    keys.forEach(function (key) {
      out[key] = Number.isSafeInteger(source[key]) && source[key] >= 0 ? source[key] : 0;
    });
    return out;
  }
  var variants = counts(value.variants, ['designTotal', 'expressible']);
  variants.provable = !value.variants || value.variants.provable !== false;
  var testReadiness = value.testReadiness && typeof value.testReadiness === 'object' ? value.testReadiness : {};
  return {
    properties: counts(value.properties, ['designTotal', 'mapped', 'drifted', 'unsupported']),
    variants: variants,
    slots: counts(value.slots, ['designTotal', 'mapped', 'ignored']),
    dependencies: counts(value.dependencies, ['designTotal', 'resolved']),
    visual: Object.prototype.hasOwnProperty.call(COMPONENT_VISUAL, value.visual) ? value.visual : 'not-run',
    testReadiness: {
      previews: testReadiness.previews === true,
      screenshotTests: testReadiness.screenshotTests === true
    }
  };
}
// PROJECTS THE COMPARISON REPORT — never a registry, never name joins. Row
// identity is the comparator's stable designComponentId (REQ-ID-004).
function componentRows(active, componentSignals, syncAttempt) {
  var comparison = componentComparisonState(active);
  var analysis = componentAnalysis(active, comparison, componentSignals, syncAttempt);
  if (!comparison.present) {
    return {
      rows: [], projectOnly: [], coverage: null, analysis: analysis,
      bindingContext: null, analysisIndexHash: null,
      limitations: analysis.state === 'unavailable' ? ['components-not-available'] : []
    };
  }
  var report = comparison.report;
  var bindingContext = componentBindingContext(active, report);
  var suggestionsDoc = jsonRole(active, 'component-mapping-suggestions');
  var candidatesByDesignComponent = Object.create(null);
  var limitations = [], truncated = false;
  if (suggestionsDoc && suggestionsDoc.schemaVersion === 2 &&
      suggestionsDoc.comparisonSemanticHash === report.semanticHash &&
      Array.isArray(suggestionsDoc.byDesignComponent)) {
    suggestionsDoc.byDesignComponent.slice(0, MAX_COMPONENT_ROWS).forEach(function (entry) {
      if (!entry || typeof entry.designComponentId !== 'string') return;
      candidatesByDesignComponent[entry.designComponentId] =
        (Array.isArray(entry.candidates) ? entry.candidates : []).slice(0, 5)
          .map(componentSuggestionCandidate).filter(Boolean);
    });
  } else if (suggestionsDoc) {
    limitations.push('component-suggestions-stale');
  }
  var visualRoles = Object.create(null);
  active.manifest.artifacts.forEach(function (entry) {
    var match = /^component-visual-evidence:([a-f0-9]{32})$/.exec(entry.role);
    if (match) visualRoles[match[1]] = 1;
  });
  var rows = [], seenIds = Object.create(null);
  report.rows.slice(0, MAX_COMPONENT_ROWS).forEach(function (raw) {
    if (!raw || typeof raw !== 'object') return;
    var status = Object.prototype.hasOwnProperty.call(COMPONENT_STATUS, raw.status) ? raw.status : null;
    if (!status) { limitations.push('component-row-status-unknown'); return; }
    var rowId = stableId('cmp', raw.designComponentId);
    if (seenIds[rowId]) { limitations.push('component-catalog-id-collision'); return; }
    seenIds[rowId] = 1;
    var change = raw.change && typeof raw.change === 'object' ? raw.change : {};
    var changedSide = Object.prototype.hasOwnProperty.call(COMPONENT_CHANGED_SIDE, change.changedSide) ? change.changedSide : 'unknown';
    var frozen = bindingContext && bindingContext.byDesignComponentId[raw.designComponentId] || null;
    var platforms = (Array.isArray(raw.platforms) ? raw.platforms : []).slice(0, 8).map(function (platform) {
      if (!platform || typeof platform !== 'object') return null;
      return {
        adapterId: safeString(platform.adapterId, 64),
        platform: safeString(platform.platform, 60),
        relation: safeString(platform.relation, 40),
        required: platform.required === true,
        state: safeString(platform.state, 20),
        projectRefs: (Array.isArray(platform.projectRefs) ? platform.projectRefs : []).slice(0, 8).map(function (ref) {
          return {
            projectComponentId: safeString(ref && ref.projectComponentId, 400),
            present: !!(ref && ref.present),
            sourcePath: safePath(ref && ref.sourcePath),
            sourceSymbol: safeString(ref && ref.sourceSymbol, 320) || null
          };
        })
      };
    }).filter(Boolean);
    var previewImage = null;
    if (frozen && frozen.visualHashes.length) {
      previewImage = frozen.visualHashes.find(function (h32) { return visualRoles[h32]; }) || null;
    }
    var candidates = candidatesByDesignComponent[raw.designComponentId] || [];
    var row = {
      id: rowId, entityType: 'component',
      findingId: null,
      comparatorFindingId: safeString(raw.findingId, 40) || null,
      designComponentId: safeString(raw.designComponentId, 200),
      name: safeString(raw.displayName, 300),
      kind: safeString(raw.kind, 20),
      nodeId: frozen ? frozen.nodeId : null,
      status: status,
      statusDetail: safeString(raw.statusDetail, 500) || null,
      changedSide: changedSide,
      change: {
        changedSide: changedSide,
        designChanged: change.designChanged === true,
        projectChanged: change.projectChanged === true,
        mappingChanged: change.mappingChanged === true,
        confidence: change.confidence === 'exact' ? 'exact' : 'none'
      },
      mappingId: safeString(raw.mappingId, 40) || null,
      mappingState: Object.prototype.hasOwnProperty.call(COMPONENT_MAPPING_STATE, raw.mappingState) ? raw.mappingState : null,
      dispositionId: safeString(raw.dispositionId, 40) || null,
      platforms: platforms,
      platformIds: platforms.map(function (platform) { return platform.platform; })
        .filter(function (value, index, list) { return value && list.indexOf(value) === index; }),
      implementationSummary: platforms.map(function (platform) {
        var symbol = platform.projectRefs.length && platform.projectRefs[0].sourceSymbol || null;
        return platform.platform + (symbol ? ': ' + symbol : '');
      }).join(' · ').slice(0, 500),
      dimensions: componentDimensions(raw.dimensions),
      findings: (Array.isArray(raw.findings) ? raw.findings : []).slice(0, 64).map(componentFinding).filter(Boolean),
      suggestionsCount: Number.isSafeInteger(raw.suggestionsCount) ? raw.suggestionsCount : candidates.length,
      suggestionsAmbiguous: raw.suggestionsAmbiguous === true,
      suggestions: candidates,
      visualHashes: frozen ? frozen.visualHashes.filter(function (h32) { return visualRoles[h32]; }) : [],
      hasPreview: !!previewImage,
      previewImageHash: previewImage,
      limitations: (Array.isArray(raw.limitations) ? raw.limitations : []).slice(0, 16).map(function (item) {
        return safeString(String(item), 200);
      }),
      lastChanged: analysis.checkedAt, openTask: null
    };
    row.searchText = [row.name, row.designComponentId, row.kind, row.status, row.statusDetail,
      row.changedSide, row.mappingState, row.mappingId, row.nodeId]
      .concat(row.platformIds)
      .concat(platforms.map(function (platform) {
        return platform.projectRefs.map(function (ref) {
          return [ref.projectComponentId, ref.sourcePath, ref.sourceSymbol].filter(Boolean).join(' ');
        }).join(' ');
      }))
      .concat(row.findings.map(function (finding) { return finding.family; }))
      .filter(Boolean).join(' ').slice(0, 20000).toLowerCase();
    rows.push(row);
  });
  if (report.rows.length > MAX_COMPONENT_ROWS) { truncated = true; limitations.push('component-catalog-truncated'); }

  var projectOnly = [];
  report.projectOnly.slice(0, MAX_COMPONENT_ROWS).forEach(function (raw) {
    if (!raw || typeof raw !== 'object') return;
    var classification = Object.prototype.hasOwnProperty.call(PROJECT_COMPONENT_CLASSIFICATION, raw.classification)
      ? raw.classification : null;
    if (!classification) { limitations.push('component-row-status-unknown'); return; }
    var entry = {
      id: stableId('cmpp', raw.projectComponentId), entityType: 'project-component',
      findingId: null,
      projectComponentId: safeString(raw.projectComponentId, 400),
      adapterId: safeString(raw.adapterId, 64),
      platform: safeString(raw.platform, 60),
      name: safeString(raw.displayName, 300),
      kind: safeString(raw.kind, 40),
      classification: classification,
      dispositionId: safeString(raw.dispositionId, 40) || null,
      sourcePath: safePath(raw.sourcePath),
      sourceSymbol: safeString(raw.sourceSymbol, 320) || null,
      suggestionsCount: Number.isSafeInteger(raw.suggestionsCount) ? raw.suggestionsCount : 0,
      openTask: null
    };
    entry.searchText = [entry.name, entry.projectComponentId, entry.adapterId, entry.platform,
      entry.kind, entry.classification, entry.sourcePath, entry.sourceSymbol]
      .filter(Boolean).join(' ').slice(0, 20000).toLowerCase();
    projectOnly.push(entry);
  });
  if (report.projectOnly.length > MAX_COMPONENT_ROWS) { truncated = true; limitations.push('component-project-only-truncated'); }

  var coverage = report.coverage && typeof report.coverage === 'object' ? {
    denominator: report.coverage.denominator,
    matched: report.coverage.matched,
    percent: report.coverage.denominator > 0
      ? Math.round(report.coverage.matched * 1000 / report.coverage.denominator) / 10 : null,
    drifted: report.coverage.drifted,
    unmapped: report.coverage.unmapped,
    ambiguous: report.coverage.ambiguous,
    missingInProject: report.coverage.missingInProject,
    missingInDesign: report.coverage.missingInDesign,
    designOnly: report.coverage.designOnly,
    unsupported: report.coverage.unsupported,
    ignored: report.coverage.ignoredDesignComponents,
    projectOnly: report.coverage.projectOnly,
    partial: truncated
  } : null;
  if (analysis.blockers.length) limitations.push('component-comparison-blockers');
  if (analysis.state !== 'current' && analysis.state !== 'not-checked') limitations.push('component-comparison-stale');
  return {
    rows: rows,
    projectOnly: projectOnly,
    coverage: coverage,
    analysis: analysis,
    bindingContext: bindingContext,
    analysisIndexHash: generation.HASH_RE.test(String(report.inputs.analysisIndexHash || ''))
      ? report.inputs.analysisIndexHash : null,
    limitations: limitations.filter(function (value, index, list) { return list.indexOf(value) === index; })
  };
}
function surfaceRows(active, relationsOverride) {
  if (active.mode !== 'generation') {
    return { rows: [], limitations: ['surface-cache-not-available'] };
  }
  var all = figmaScreens.screensAll(active, MAX_SURFACE_ROWS);
  var rows = [], limitations = (all.limitations || []).slice(), surfaceRowsTruncated = false;
  (all.stems || []).some(function (stemGroup) {
    var nodes = (stemGroup.nodes || []).filter(function (node) { return node.kind !== 'component'; });
    if (!nodes.length) return false;
    if (rows.length >= MAX_SURFACE_ROWS) { surfaceRowsTruncated = true; return true; }
    var drift = figmaScreens.screenDrift(stemGroup.stem, active);
    limitations = limitations.concat(drift.limitations || []);
    var driftByScreen = Object.create(null);
    if (drift.present) {
      drift.screens.forEach(function (item) {
        var key = String(item.screen || '');
        if (!driftByScreen[key]) driftByScreen[key] = [];
        if (driftByScreen[key].length < 20) driftByScreen[key].push(item);
      });
    }
    return nodes.some(function (node) {
      limitations = limitations.concat(Array.isArray(node.variantLimitations) ? node.variantLimitations : []);
      if (rows.length >= MAX_SURFACE_ROWS) { surfaceRowsTruncated = true; return true; }
      var kind = node.kind === 'dialog' || node.kind === 'overlay' ? node.kind : 'screen';
      var surfaceKey = stemGroup.stem + '\0' + node.screen, id = stableId('srf', surfaceKey);
      var variants = [];
      (node.variants || []).forEach(function (variant) {
        var variantId = stableId('var', surfaceKey + '\0' + variant.id);
        variants.push({
          id: variantId, theme: safeString(variant.theme, 80), locale: safeString(variant.locale, 80),
          platform: variant.platform, captured: !!variant.hasPng, figmaUrl: safeFigmaUrl(variant.url),
          nodeId: safeString(variant.nodeId, 100) || null, fetchedAt: safeString(variant.fetchedAt, 40) || null,
          _imageFile: variant.imageFile,
          imageUrl: variant.hasPng ? '/api/design/surface-image?surfaceId=' + encodeURIComponent(id) +
            '&variantId=' + encodeURIComponent(variantId) : null
        });
      });
      var driftRows = drift.present ? (driftByScreen[node.screen] || []).map(function (item) {
        return {
          screen: safeString(item.screen, 200),
          theme: item.theme === 'dark' ? 'dark' : 'primary',
          status: ['DRIFTED', 'CLEAN', 'NOT_CHECKED'].indexOf(item.status) >= 0 ? item.status : 'NOT_CHECKED',
          changes: (Array.isArray(item.changes) ? item.changes : []).slice(0, 20).map(function (change) {
            return safeString(String(change), 200);
          })
        };
      }) : [];
      var status = driftRows.some(function (item) { return item.status === 'DRIFTED'; }) ? 'drifted' :
        drift.present && driftRows.length && driftRows.every(function (item) { return item.status === 'CLEAN'; }) ? 'healthy' : 'unknown';
      var usedComponents = (Array.isArray(node.usedComponents) ? node.usedComponents : []).slice(0, 500)
        .map(function (item) {
          if (!item || typeof item !== 'object') return null;
          var name = safeString(item.name, 200);
          return name ? {
            name: name,
            nodeId: safeString(item.nodeId, 100) || null,
            nodeUrl: safeFigmaUrl(item.nodeUrl)
          } : null;
        }).filter(Boolean);
      var themes = variants.map(function (item) { return item.theme; }).filter(function (value, index, list) {
        return list.indexOf(value) === index;
      });
      var locales = variants.map(function (item) { return item.locale; }).filter(function (value, index, list) {
        return list.indexOf(value) === index;
      });
      var platforms = variants.map(function (item) { return item.platform; }).filter(function (value, index, list) {
        return list.indexOf(value) === index;
      });
      var sourceVariant = variants.find(function (variant) { return variant.figmaUrl || variant.nodeId; }) || null;
      rows.push({
        id: id, entityType: 'surface', type: kind, name: safeString(node.screen), sourceStem: stemGroup.stem,
        feature: null, module: null, route: null, platform: platforms.length === 1 ? platforms[0] : null,
        platforms: platforms, themes: themes, locales: locales, variants: variants,
        thumbnail: variants.find(function (variant) { return variant.captured && variant.imageUrl; }) || null,
        status: status, drift: drift.present ? driftRows : null,
        figmaSource: sourceVariant ? { url: sourceVariant.figmaUrl, nodeId: sourceVariant.nodeId } : { url: null, nodeId: null },
        usedComponents: usedComponents,
        findingId: null, openTask: null,
        lastChanged: variants.map(function (item) { return item.fetchedAt; }).filter(Boolean).sort().pop() || null,
        searchText: [node.screen, kind, stemGroup.stem, node.nodeId, node.url]
          .concat(variants.map(function (item) { return [item.id, item.theme, item.locale, item.platform].join(' '); }))
          .concat(usedComponents.map(function (item) { return [item.name, item.nodeId, item.nodeUrl].filter(Boolean).join(' '); }))
          .filter(Boolean).join(' ').slice(0, 20000).toLowerCase()
      });
      return false;
    });
  });
  var arch = relationsOverride || relations.snapshot();
  rows.forEach(function (row) {
    var link = relations.surfaceRelation(row, arch);
    row.feature = link.feature; row.module = link.module; row.route = link.route; row.codeSources = link.codeSources;
    row.searchText += ' ' + [row.feature, row.module, row.route].filter(Boolean).join(' ').toLowerCase();
  });
  return {
    rows: rows,
    limitations: (all.present ? [] : ['surface-cache-not-available'])
      .concat(surfaceRowsTruncated ? ['surface-catalog-truncated'] : [])
      .concat(limitations)
      .filter(function (value, index, list) { return list.indexOf(value) === index; })
  };
}
function finding(kind, entity, title, detail, fingerprintValue) {
  var sourceId = 'design:' + entity.entityType + ':' + entity.id + ':' + kind;
  return {
    id: stableId('fnd', sourceId), kind: kind, sourceId: sourceId, sourceKind: 'design-finding',
    sourceFingerprint: hash(fingerprintValue), entityId: entity.id, entityType: entity.entityType,
    entityName: entity.name,
    severity: kind.indexOf('missing') >= 0 ? 'high' : 'medium', status: entity.status,
    title: title, detail: detail, syncJobId: null, openTask: null
  };
}
// Token findings carry the comparator's stable finding identity: the source
// id is built from the stable design/project token id plus the finding
// family, never from a display name (REQ-ID-004).
function tokenFindingKind(row) {
  if (row.status === 'value-drift') return 'token-reconcile-value';
  if (row.status === 'missing-in-project') return 'token-implement';
  return null;
}
function attachTokenFindings(snapshot, findings) {
  snapshot.tokens.rows.forEach(function (row) {
    var kind = tokenFindingKind(row);
    if (!kind) return;
    var projectRef = row.projectRefs && row.projectRefs[0];
    if (!projectRef || !row.adapterId || !row.modeResults[0] ||
        !Array.isArray(projectRef.intendedSemanticPath) || !projectRef.intendedSemanticPath.length) return;
    var sourceId = 'design:observed-token:' + row.observedTokenKey + ':' + kind;
    var item = {
      id: stableId('fnd', sourceId), kind: kind, sourceId: sourceId, sourceKind: 'design-finding',
      sourceFingerprint: hash({
        observedTokenKey: row.observedTokenKey, contextKey: row.contextKey,
        status: row.status, changedSide: row.changedSide,
        figmaValues: row.figmaValues, codeValues: row.codeValues, mappingId: row.mappingId
      }),
      entityId: row.id, entityType: 'token', entityName: row.name,
      severity: row.status === 'missing-in-project' ? 'high' : 'medium',
      status: row.status, title: tokenFindingTitle(kind, row.name), detail: row.statusDetail || row.status,
      syncJobId: null, openTask: null,
      tokenBinding: {
        schemaVersion: 2,
        sourceId: sourceId,
        intent: kind.slice('token-'.length),
        observedTokenKey: row.observedTokenKey,
        contextKey: row.contextKey,
        catalogHash: snapshot.tokens.bindingContext.catalogHash,
        sourceIndexHash: snapshot.tokens.bindingContext.sourceIndexHash,
        bindingSnapshotHash: snapshot.tokens.bindingContext.bindingSnapshotHash,
        expectedKind: row.kind,
        frozenValue: row.frozenValue,
        intendedAdapterId: row.adapterId,
        intendedSemanticPath: projectRef.intendedSemanticPath,
        intendedProjectMode: row.modeResults[0].projectMode,
        mappingId: row.mappingId,
        findingId: row.findingId,
        comparisonSemanticHash: snapshot.tokens.bindingContext.comparisonSemanticHash,
        mappingRevision: snapshot.tokens.bindingContext.mappingRevision
      }
    };
    if (!item.tokenBinding.mappingId) delete item.tokenBinding.mappingId;
    row.findingId = item.id;
    findings.push(item);
  });
  snapshot.tokens.projectOnly.forEach(function (row) {
    if (row.classification !== 'unclassified') return;
    var sourceId = 'design:project-token:' + row.projectTokenId + ':token-classify-project-only';
    var item = {
      id: stableId('fnd', sourceId), kind: 'token-classify-project-only', sourceId: sourceId, sourceKind: 'design-finding',
      sourceFingerprint: hash({ projectTokenId: row.projectTokenId, classification: row.classification }),
      entityId: row.id, entityType: 'project-token', entityName: row.name,
      severity: 'low', status: 'project-only', title: 'Classify project-only token ' + row.name,
      detail: 'project-only', syncJobId: null, openTask: null,
      tokenBinding: {
        projectTokenId: row.projectTokenId,
        adapterId: row.adapterId,
        kind: row.kind
      }
    };
    row.findingId = item.id;
    findings.push(item);
  });
}
function tokenFindingTitle(kind, name) {
  if (kind === 'token-reconcile-value') return 'Reconcile token value ' + name;
  if (kind === 'token-implement') return 'Implement design token ' + name;
  if (kind === 'token-reconcile-mapping') return 'Reconcile token mapping ' + name;
  return 'Review token ' + name;
}
// Component findings carry the comparator's stable identity: the source id is
// built from the stable designComponentId plus the intent, matching the
// component-binding-contract SOURCE_ID_RE exactly. A status with no honest
// intent mapping produces NO finding — intents are never invented.
function componentFindingKind(row) {
  if (row.status === 'unmapped') {
    return row.suggestionsAmbiguous ? 'component-reconcile-mapping' : 'component-implement';
  }
  if (row.status === 'drifted') {
    var material = row.findings.filter(function (item) { return item.severity !== 'info'; });
    return material.length && material.every(function (item) { return item.family === 'visual-evidence-drift'; })
      ? 'component-update-visual' : 'component-update-api';
  }
  if (row.status === 'missing-in-project') return 'component-implement';
  if (row.status === 'missing-in-design') return 'component-remap';
  if (row.status === 'ambiguous') return 'component-reconcile-mapping';
  return null;
}
function componentFindingTitle(kind, name) {
  if (kind === 'component-implement') return 'Implement design component ' + name;
  if (kind === 'component-update-api') return 'Update component API for ' + name;
  if (kind === 'component-update-visual') return 'Update component visuals for ' + name;
  if (kind === 'component-remap') return 'Review removed design component ' + name;
  if (kind === 'component-reconcile-mapping') return 'Reconcile component mapping ' + name;
  return 'Review component ' + name;
}
function attachComponentFindings(snapshot, findings) {
  snapshot.components.rows.forEach(function (row) {
    // Token-caused rows are suppressed entirely: the causality proof already
    // routed the work to the token domain (§17.12).
    if (row.findings.some(function (item) { return item.suppressesTask === true; })) return;
    var kind = componentFindingKind(row);
    if (!kind) return;
    if (row.kind !== 'component-set' && row.kind !== 'component') return;
    var sourceId = 'design:component:' + row.designComponentId + ':' + kind;
    var item = {
      id: stableId('fnd', sourceId), kind: kind, sourceId: sourceId, sourceKind: 'design-finding',
      sourceFingerprint: hash({
        designComponentId: row.designComponentId, status: row.status, changedSide: row.changedSide,
        mappingId: row.mappingId,
        findings: row.findings.map(function (finding) { return [finding.family, finding.severity, finding.findingId]; })
      }),
      entityId: row.id, entityType: 'component', entityName: row.name,
      severity: row.status === 'missing-in-design' || row.status === 'missing-in-project' ||
        row.findings.some(function (finding) { return finding.severity === 'breaking' || finding.severity === 'blocking'; })
        ? 'high' : 'medium',
      status: row.status, title: componentFindingTitle(kind, row.name), detail: row.statusDetail || row.status,
      syncJobId: null, openTask: null,
      componentBinding: {
        designComponentId: row.designComponentId,
        kind: row.kind,
        status: row.status,
        mappingId: row.mappingId,
        comparatorFindingId: row.comparatorFindingId,
        intent: kind.slice('component-'.length)
      }
    };
    row.findingId = item.id;
    findings.push(item);
  });
  snapshot.components.projectOnly.forEach(function (row) {
    if (row.classification !== 'unclassified') return;
    var sourceId = 'design:project-component:' + row.projectComponentId + ':component-classify-project-only';
    var item = {
      id: stableId('fnd', sourceId), kind: 'component-classify-project-only', sourceId: sourceId, sourceKind: 'design-finding',
      sourceFingerprint: hash({ projectComponentId: row.projectComponentId, classification: row.classification }),
      entityId: row.id, entityType: 'project-component', entityName: row.name,
      severity: 'low', status: 'project-only', title: 'Classify project-only component ' + row.name,
      detail: 'project-only', syncJobId: null, openTask: null,
      componentBinding: {
        projectComponentId: row.projectComponentId,
        adapterId: row.adapterId,
        kind: row.kind
      }
    };
    row.findingId = item.id;
    findings.push(item);
  });
}
function attachFindings(snapshot, tasksOverride) {
  var findings = [];
  attachTokenFindings(snapshot, findings);
  attachComponentFindings(snapshot, findings);
  snapshot.surfaces.rows.forEach(function (row) {
    if (row.status !== 'drifted') return;
    var item = finding('surface-drift', row, 'Actualize surface ' + row.name, 'Surface drift detected',
      { revision: snapshot.generationRevision, drift: row.drift });
    row.findingId = item.id; findings.push(item);
  });
  var syncJobId = snapshot.active.mode === 'generation' &&
    typeof snapshot.active.manifest.syncJobId === 'string' ? snapshot.active.manifest.syncJobId : null;
  findings.forEach(function (item) { item.syncJobId = syncJobId; });
  var tasks = tasksOverride || taskSource.scanOpen();
  findings.forEach(function (item) {
    var matches = tasks.byRef[item.sourceId] || [];
    item.openTask = matches[0] || null;
    var entity = snapshot.byId[item.entityId];
    if (entity) {
      entity.openTask = item.openTask;
      if (item.openTask) {
        entity.searchText = (entity.searchText + ' ' +
          [item.openTask.stem, item.openTask.title].filter(Boolean).join(' ').toLowerCase()).slice(0, 20000);
      }
    }
  });
  snapshot.findings = findings;
  snapshot.findingsById = Object.create(null);
  findings.forEach(function (item) { snapshot.findingsById[item.id] = item; });
  snapshot.tasks = tasks;
  snapshot.tasks.designItems = tasks.items.filter(function (item) {
    return item.source && item.source.kind === 'figma';
  });
}
function snapshot() {
  var active = generation.current();
  if (!active.ok) return revisionMeta(active, null);
  if (active.mode !== 'none' && active.mode !== 'generation') {
    return revisionMeta({ ok: false, error: 'design-generation-mode-invalid' }, null);
  }
  var relationState = relations.snapshot();
  var tasks = taskSource.scanOpen();
  var tokenSignals = tokenState.readTokenSignals();
  var componentSignals = componentState.readComponentSignals();
  var tokenSyncAttempt = latestSyncAttempt('tokens');
  var componentSyncAttempt = latestSyncAttempt('components');
  var artifactFingerprint = active.mode === 'generation' ? hash(active.manifest.artifacts.map(function (entry) {
    return {
      role: entry.role, group: entry.group, path: entry.path,
      hash: entry.hash, size: entry.size, persistence: entry.persistence
    };
  })) : null;
  var cacheKey = hash({
    analyzer: ANALYZER_VERSION,
    generationMode: active.mode,
    generationRevision: active.mode === 'generation' ? active.pointer.manifestHash : null,
    artifactFingerprint: artifactFingerprint,
    tokenSignals: {
      adapters: tokenSignals.adapters.state,
      configFileHash: tokenSignals.configFileHash,
      mappingState: tokenSignals.mappingState,
      mappingRevision: tokenSignals.mappingRevision,
      mappingFileHash: tokenSignals.mappingFileHash,
      projectDirty: tokenSignals.projectDirty
    },
    componentSignals: {
      adapters: componentSignals.adapters.state,
      configFileHash: componentSignals.configFileHash,
      mappingState: componentSignals.mappingState,
      mappingRevision: componentSignals.mappingRevision,
      mappingFileHash: componentSignals.mappingFileHash,
      projectDirty: componentSignals.projectDirty
    },
    syncAttempts: {
      tokens: tokenSyncAttempt,
      components: componentSyncAttempt
    },
    artifactAvailability: active.availability ? {
      missingOptional: active.availability.missingOptional.slice().sort(),
      missingRuntime: active.availability.missingRuntime.slice().sort()
    } : null,
    architectureFingerprint: relationState.fingerprint,
    taskRevision: tasks.revision,
    taskState: {
      ok: tasks.ok, historyOk: tasks.historyOk,
      error: tasks.error || null,
      scanLimitExceeded: !!tasks.scanLimitExceeded,
      malformed: tasks.malformed || [],
      historyMalformed: tasks.historyMalformed || []
    }
  });
  if (snapshotCache && snapshotCache.key === cacheKey) return snapshotCache.value;
  var components, surfaces, tokens;
  try {
    components = componentRows(active, componentSignals, componentSyncAttempt);
    surfaces = surfaceRows(active, relationState);
    tokens = tokenRows(active, tokenSignals, tokenSyncAttempt);
  } catch (error) {
    if (error && error.code === 'DESIGN_GENERATION_ARTIFACT_INVALID') {
      return Object.assign(revisionMeta(active, null), {
        ok: false,
        status: 409,
        error: 'design-generation-artifact-invalid',
        recoveryRequired: true
      });
    }
    throw error;
  }
  // The code-side revision derives from the published project component
  // analysis (exact adapter extraction hashes). The retired registry-driven
  // source-file hashing is absent with the registry itself.
  var codeSideRevision = hash({
    analyzer: ANALYZER_VERSION,
    analysisIndexHash: components.analysisIndexHash
  });
  var meta = revisionMeta(active, codeSideRevision);
  var value = Object.assign(meta, {
    active: active,
    tokens: tokens,
    components: components,
    surfaces: surfaces,
    byId: Object.create(null),
    analysisLimitations: []
  });
  [value.tokens, value.components, value.surfaces].forEach(function (group) {
    value.analysisLimitations = value.analysisLimitations.concat(group.limitations || []);
    group.rows.forEach(function (row) { value.byId[row.id] = row; });
  });
  value.tokens.projectOnly.forEach(function (row) { value.byId[row.id] = row; });
  value.components.projectOnly.forEach(function (row) { value.byId[row.id] = row; });
  if (active.mode === 'generation' && active.availability.missingOptional.length) {
    value.analysisLimitations.push('optional-artifacts-missing');
  }
  if (Object.keys(value.byId).length > 10000) {
    value.analysisLimitations.push('design-history-entity-limit');
  }
  attachFindings(value, tasks);
  snapshotCache = { key: cacheKey, value: value };
  return value;
}
function publicMeta(value) {
  return {
    schemaVersion: value.schemaVersion, committedGenerationId: value.committedGenerationId,
    generationRevision: value.generationRevision, generationMode: value.generationMode,
    designManifest: value.designManifest
  };
}
function publicTask(task) {
  if (!task || typeof task !== 'object') return null;
  return {
    stem: safeString(task.stem, 120),
    title: safeString(task.title, 500),
    column: safeString(task.column, 40)
  };
}
function normalizeLimit(value) {
  var parsed = Number(value);
  return Number.isInteger(parsed) ? Math.max(1, Math.min(MAX_LIMIT, parsed)) : DEFAULT_LIMIT;
}
function invalidParams(kind, params) {
  var typeAllowed = Object.assign(Object.create(null), kind === 'tokens' || kind === 'project-tokens'
    ? { color: 1, dimension: 1, typography: 1, unsupported: 1 }
    : kind === 'surfaces' ? { screen: 1, dialog: 1, overlay: 1 } : {});
  var allowed = Object.assign(Object.create(null), {
    query: 1, type: 1, status: 1, theme: 1, platform: 1, locale: 1,
    changed: 1, hasTask: 1, cursor: 1, limit: 1, expectedGenerationRevision: 1,
    _changedGenerationId: 1, _changedIds: 1
  });
  if (kind === 'tokens' || kind === 'components') allowed.changedSide = 1;
  if (kind === 'components') allowed.mappingState = 1;
  if (Object.keys(params).some(function (key) {
    return !Object.prototype.hasOwnProperty.call(allowed, key);
  })) return 'query';
  if ((kind === 'tokens' || kind === 'project-tokens') && (params.theme || params.platform || params.locale)) {
    return params.theme ? 'theme' : params.platform ? 'platform' : 'locale';
  }
  if ((kind === 'project-tokens' || kind === 'project-components') && (params.status || params.changed)) {
    return params.status ? 'status' : 'changed';
  }
  if ((kind === 'components' || kind === 'project-components') && (params.type || params.locale || params.theme)) {
    return params.type ? 'type' : params.locale ? 'locale' : 'theme';
  }
  if (kind === 'project-components' && params.changedSide) return 'changedSide';
  if (params.query != null && (typeof params.query !== 'string' ||
      params.query.normalize('NFC') !== params.query || /[\x00-\x1f\x7f]/.test(params.query) ||
      Buffer.byteLength(params.query, 'utf8') > 200)) return 'query';
  if (params.status && kind === 'tokens' && !Object.prototype.hasOwnProperty.call(TOKEN_STATUS, params.status)) return 'status';
  if (params.status && kind === 'components' && !Object.prototype.hasOwnProperty.call(COMPONENT_STATUS, params.status)) return 'status';
  if (params.status && kind !== 'tokens' && kind !== 'components' && !Object.prototype.hasOwnProperty.call(STATUS, params.status)) return 'status';
  if (params.changedSide && kind === 'tokens' && !Object.prototype.hasOwnProperty.call(TOKEN_CHANGED_SIDE, params.changedSide)) return 'changedSide';
  if (params.changedSide && kind === 'components' && !Object.prototype.hasOwnProperty.call(COMPONENT_CHANGED_SIDE, params.changedSide)) return 'changedSide';
  if (params.changedSide && kind !== 'tokens' && kind !== 'components') return 'changedSide';
  if (params.mappingState && !Object.prototype.hasOwnProperty.call(COMPONENT_MAPPING_STATE, params.mappingState)) return 'mappingState';
  if (params.type && !Object.prototype.hasOwnProperty.call(typeAllowed, params.type)) return 'type';
  if (params.theme && ['light', 'dark'].indexOf(String(params.theme)) < 0) return 'theme';
  if (params.platform) {
    var platformOk = kind === 'components' || kind === 'project-components'
      ? /^[a-z][a-z0-9-]{0,59}$/.test(String(params.platform))
      : ['shared', 'android', 'ios'].indexOf(String(params.platform)) >= 0;
    if (!platformOk) return 'platform';
  }
  if (params.locale && !/^[A-Za-z0-9][A-Za-z0-9_-]{0,39}$/.test(String(params.locale))) return 'locale';
  if (params.changed && params.changed !== 'true') return 'changed';
  if (params.hasTask && params.hasTask !== 'true') return 'hasTask';
  if (params.limit != null && !/^[1-9][0-9]{0,2}$/.test(String(params.limit))) return 'limit';
  if (params.limit != null && Number(params.limit) > MAX_LIMIT) return 'limit';
  if (params.cursor != null && (typeof params.cursor !== 'string' || params.cursor.length > 2048)) return 'cursor';
  if (params.expectedGenerationRevision && !generation.HASH_RE.test(String(params.expectedGenerationRevision))) return 'expectedGenerationRevision';
  return null;
}
function filterSignature(kind, params, revision) {
  return hash({ kind: kind, revision: revision, query: params.query || '', type: params.type || '',
    status: params.status || '', theme: params.theme || '', platform: params.platform || '',
    locale: params.locale || '', changed: params.changed || '', hasTask: params.hasTask || '',
    changedSide: params.changedSide || '', mappingState: params.mappingState || '' });
}
function cursorDecode(cursor, signature) {
  if (!cursor) return 0;
  try {
    var parsed = JSON.parse(Buffer.from(String(cursor), 'base64url').toString('utf8'));
    return parsed && parsed.signature === signature && Number.isSafeInteger(parsed.offset) && parsed.offset >= 0 ? parsed.offset : null;
  } catch (error) { return null; }
}
function matches(row, params) {
  var query = safeString(params.query || '', 200).trim().toLowerCase();
  if (query && row.searchText.indexOf(query) < 0) return false;
  if (params.type && row.type !== params.type && row.category !== params.type && row.kind !== params.type) return false;
  if (params.status && row.status !== params.status) return false;
  if (params.changedSide && row.changedSide !== params.changedSide) return false;
  if (params.mappingState && row.mappingState !== params.mappingState) return false;
  if (params.theme && params.theme !== 'any' && (!row.themes || row.themes.indexOf(params.theme) < 0)) return false;
  if (params.platform && params.platform !== 'any') {
    var platforms = row.platformIds || row.platforms || [row.platform || 'shared'];
    if (!platforms.some(function (value) { return String(value).toLowerCase() === params.platform.toLowerCase(); })) return false;
  }
  if (params.locale && params.locale !== 'any' && (!row.locales || !row.locales.some(function (value) {
    return String(value).toLowerCase() === String(params.locale).toLowerCase();
  }))) return false;
  if (String(params.changed) === 'true' && (!params._changedIds || !params._changedIds[row.id])) return false;
  if (String(params.hasTask) === 'true' && !row.openTask) return false;
  return true;
}
function stripPrivate(row, list) {
  var out = {};
  Object.keys(row).forEach(function (key) {
    if (key === 'searchText' || key === 'source' || key === 'visualHashes' ||
        key === 'usedComponents' || key === 'coverageEligible') return;
    if (list && (row.entityType === 'token' || row.entityType === 'project-token') &&
        ['projectRefs', 'modeResults', 'lifecycle', 'suggestions', 'change'].indexOf(key) >= 0) {
      if (key === 'lifecycle') out.lifecycleCount = (row.lifecycle || []).length;
      if (key === 'suggestions') out.suggestionCount = (row.suggestions || []).length;
      return;
    }
    if (list && row.entityType === 'component' &&
        ['platforms', 'findings', 'suggestions', 'change'].indexOf(key) >= 0) {
      if (key === 'findings') out.findingCount = (row.findings || []).length;
      if (key === 'suggestions') out.suggestionCount = (row.suggestions || []).length;
      return;
    }
    if (list && ['sourceStem', 'figmaSource'].indexOf(key) >= 0) return;
    if (list && (key === 'drift' || key === 'codeSources' || key === 'preview')) return;
    if (key === 'variants' && row.entityType !== 'surface' && list) return;
    if (key === 'openTask') {
      out.openTask = publicTask(row.openTask);
      return;
    }
    if (key === 'variants' && row.entityType === 'surface') {
      if (list) {
        out.variantCount = (row.variants || []).length;
        return;
      }
      var publicVariants = (row.variants || []).slice(0, 100);
      out.variants = publicVariants.map(function (variant) {
        return {
          id: variant.id, theme: variant.theme, locale: variant.locale, platform: variant.platform,
          captured: variant.captured, fetchedAt: variant.fetchedAt, imageUrl: variant.imageUrl,
          figmaUrl: variant.figmaUrl, nodeId: variant.nodeId
        };
      });
      out.variantCount = (row.variants || []).length;
      out.variantsTruncated = (row.variants || []).length > publicVariants.length;
      return;
    }
    if (key === 'thumbnail') {
      out.thumbnail = row.thumbnail ? {
        id: row.thumbnail.id, theme: row.thumbnail.theme, locale: row.thumbnail.locale,
        platform: row.thumbnail.platform, captured: row.thumbnail.captured,
        fetchedAt: row.thumbnail.fetchedAt, imageUrl: row.thumbnail.imageUrl
      } : null;
      return;
    }
    out[key] = row[key];
  });
  return out;
}
function paged(kind, group, params, snap) {
  var invalid = invalidParams(kind, params);
  if (invalid) return Object.assign(publicMeta(snap), {
    ok: false, status: 400, error: 'bad-design-query', field: invalid
  });
  var conflict = checkRevision(snap, params.expectedGenerationRevision);
  if (conflict) return conflict;
  var signature = filterSignature(kind, params, snap.generationRevision);
  var offset = cursorDecode(params.cursor, signature);
  if (offset === null) return Object.assign(publicMeta(snap), { ok: false, status: 400, error: 'design-cursor-invalid' });
  var changedUnavailable = String(params.changed) === 'true' &&
    params._changedGenerationId !== snap.committedGenerationId;
  var rows = changedUnavailable ? [] : group.rows.filter(function (row) { return matches(row, params); });
  var limit = normalizeLimit(params.limit), items = [], index = offset, sizeLimited = false;
  while (index < rows.length && items.length < limit) {
    var candidate = stripPrivate(rows[index], true);
    if (kind === 'components' && candidate.hasPreview && candidate.previewImageHash) {
      candidate.previewUrl = '/api/design/component-image?componentId=' + encodeURIComponent(candidate.id) +
        '&image=' + encodeURIComponent(candidate.previewImageHash) +
        '&expectedGenerationRevision=' + encodeURIComponent(snap.generationRevision);
    }
    var nextItems = items.concat([candidate]);
    if (Buffer.byteLength(JSON.stringify(nextItems), 'utf8') > RESPONSE_MAX - 8192) {
      if (!items.length) return Object.assign(publicMeta(snap), {
        ok: false, status: 413, error: 'design-item-too-large'
      });
      sizeLimited = true; break;
    }
    items.push(candidate); index++;
  }
  return Object.assign(publicMeta(snap), {
    ok: true, status: 200, items: items, total: rows.length,
    nextCursor: index < rows.length ? Buffer.from(JSON.stringify({ signature: signature, offset: index })).toString('base64url') : null,
    limitations: (group.limitations || [])
      .concat(sizeLimited ? ['response-size-limit'] : [])
      .concat(changedUnavailable ? ['changed-history-not-current'] : [])
      .filter(function (value, position, list) { return list.indexOf(value) === position; })
  });
}
function listTokens(params) {
  var snap = snapshot();
  if (!snap.ok) return snap;
  var result = paged('tokens', snap.tokens, params || {}, snap);
  if (result.ok) {
    result.coverage = snap.tokens.coverage;
    result.analysis = snap.tokens.analysis;
    result.projectOnlyTotal = snap.tokens.projectOnly.length;
  }
  return result;
}
function listProjectOnlyTokens(params) {
  var snap = snapshot();
  if (!snap.ok) return snap;
  var result = paged('project-tokens', { rows: snap.tokens.projectOnly, limitations: snap.tokens.limitations }, params || {}, snap);
  if (result.ok) result.analysis = snap.tokens.analysis;
  return result;
}
function tokenDetail(id, params) {
  var snap = snapshot();
  if (!snap.ok) return snap;
  var invalid = invalidDetailParams(params, false);
  if (invalid) return Object.assign(publicMeta(snap), {
    ok: false, status: 400, error: 'bad-design-detail-query', field: invalid
  });
  var conflict = checkRevision(snap, params && params.expectedGenerationRevision);
  if (conflict) return conflict;
  var row = snap.byId[id];
  if (!row || row.entityType !== 'token' && row.entityType !== 'project-token') {
    return Object.assign(publicMeta(snap), { ok: false, status: 404, error: 'token-not-found' });
  }
  var detail = stripPrivate(row, false);
  detail.tasks = ((row.findingId && snap.findingsById[row.findingId] &&
    snap.tasks.allByRef[snap.findingsById[row.findingId].sourceId]) || []).map(publicTask).filter(Boolean);
  return Object.assign(publicMeta(snap), {
    ok: true, status: 200,
    token: detail,
    analysis: snap.tokens.analysis
  });
}
function listComponents(params) {
  var snap = snapshot();
  if (!snap.ok) return snap;
  var result = paged('components', snap.components, params || {}, snap);
  if (result.ok) {
    result.coverage = snap.components.coverage;
    result.analysis = snap.components.analysis;
    result.projectOnlyTotal = snap.components.projectOnly.length;
  }
  return result;
}
function listProjectOnlyComponents(params) {
  var snap = snapshot();
  if (!snap.ok) return snap;
  var result = paged('project-components', { rows: snap.components.projectOnly, limitations: snap.components.limitations }, params || {}, snap);
  if (result.ok) result.analysis = snap.components.analysis;
  return result;
}
function listSurfaces(params) {
  var snap = snapshot();
  return snap.ok ? paged('surfaces', snap.surfaces, params || {}, snap) : snap;
}
function invalidDetailParams(params, component) {
  params = params || {};
  var allowed = Object.assign(Object.create(null), component
    ? { expectedGenerationRevision: 1, variantOffset: 1, variantLimit: 1 }
    : { expectedGenerationRevision: 1 });
  if (Object.keys(params).some(function (key) {
    return !Object.prototype.hasOwnProperty.call(allowed, key);
  })) return 'query';
  if (!generation.HASH_RE.test(String(params.expectedGenerationRevision || ''))) return 'expectedGenerationRevision';
  if (component && params.variantOffset != null && !/^(?:0|[1-9][0-9]{0,5})$/.test(String(params.variantOffset))) return 'variantOffset';
  if (component && params.variantLimit != null &&
      (!/^[1-9][0-9]{0,2}$/.test(String(params.variantLimit)) || Number(params.variantLimit) > 100)) return 'variantLimit';
  return null;
}
// Bounded design-inventory extras for the detail drawer. The row itself stays
// the comparison projection; this only adds the design-side spec view.
function componentDesignDetail(snap, row, params) {
  var design = snap.active.mode === 'generation' ? jsonRole(snap.active, 'design-component-inventory') : null;
  var found = design && Array.isArray(design.components)
    ? design.components.find(function (component) { return component && component.designComponentId === row.designComponentId; })
    : null;
  if (!found && design && Array.isArray(design.unsupportedComponents)) {
    var unsupported = design.unsupportedComponents.find(function (component) {
      return component && component.designComponentId === row.designComponentId;
    });
    if (unsupported) {
      return {
        nodeId: unsupported.providerIdentity && safeString(unsupported.providerIdentity.nodeId, 80) || null,
        idQuality: unsupported.providerIdentity && safeString(unsupported.providerIdentity.idQuality, 20) || null,
        page: unsupported.page ? { name: safeString(unsupported.page.name, 300) } : null,
        unsupportedReason: safeString(unsupported.reason, 300) || null,
        properties: [], unsupportedProperties: [],
        variants: { items: [], total: 0, nextOffset: null },
        defaultVariantId: null, defaultKnown: false,
        slots: [], dependencies: [], tokenRefs: [],
        visualEvidence: { coverage: 'none', entries: [] }
      };
    }
  }
  if (!found) return null;
  var allVariants = (Array.isArray(found.variants) ? found.variants : []).slice(0, 500).map(function (variant) {
    var assignments = Object.create(null);
    if (variant && variant.assignments && typeof variant.assignments === 'object' && !Array.isArray(variant.assignments)) {
      Object.keys(variant.assignments).sort().slice(0, 40).forEach(function (key) {
        var cleanKey = safeString(key, 160);
        if (cleanKey && !Object.prototype.hasOwnProperty.call(assignments, cleanKey)) {
          assignments[cleanKey] = safeString(String(variant.assignments[key]), 200);
        }
      });
    }
    return {
      variantId: safeString(variant && variant.variantId, 80),
      name: safeString(variant && variant.name, 300),
      assignments: assignments,
      isDefault: !!(variant && variant.isDefault)
    };
  });
  var page = Math.max(0, Number(params && params.variantOffset || 0) || 0);
  var limit = Math.min(100, normalizeLimit(params && params.variantLimit || 50));
  var visual = found.visualEvidence && typeof found.visualEvidence === 'object' ? found.visualEvidence : null;
  var visualEntries = (visual && Array.isArray(visual.entries) ? visual.entries : []).slice(0, 64).map(function (entry) {
    var match = /^sha256:([a-f0-9]{64})$/.exec(String(entry && entry.imageHash || ''));
    var h32 = match ? match[1].slice(0, 32) : null;
    var available = !!(h32 && row.visualHashes && row.visualHashes.indexOf(h32) >= 0);
    return {
      variantId: entry && entry.variantId ? safeString(entry.variantId, 80) : null,
      role: safeString(entry && entry.role, 20),
      imageUrl: available
        ? '/api/design/component-image?componentId=' + encodeURIComponent(row.id) +
          '&image=' + encodeURIComponent(h32) +
          '&expectedGenerationRevision=' + encodeURIComponent(snap.generationRevision)
        : null
    };
  });
  return {
    nodeId: found.providerIdentity && safeString(found.providerIdentity.nodeId, 80) || null,
    idQuality: found.providerIdentity && safeString(found.providerIdentity.idQuality, 20) || null,
    description: safeString(found.description, 2000) || null,
    page: found.page ? { name: safeString(found.page.name, 300) } : null,
    properties: (Array.isArray(found.properties) ? found.properties : []).slice(0, 40).map(function (property) {
      return {
        propertyId: safeString(property && property.propertyId, 160),
        name: safeString(property && property.name, 200),
        type: safeString(property && property.type, 40),
        options: (Array.isArray(property && property.options) ? property.options : []).slice(0, 64).map(function (value) {
          return safeString(String(value), 200);
        }),
        defaultValue: property && (typeof property.defaultValue === 'string' || typeof property.defaultValue === 'boolean')
          ? (typeof property.defaultValue === 'string' ? safeString(property.defaultValue, 500) : property.defaultValue)
          : null,
        defaultKnown: !!(property && property.defaultKnown)
      };
    }),
    unsupportedProperties: (Array.isArray(found.unsupportedProperties) ? found.unsupportedProperties : []).slice(0, 40).map(function (property) {
      return {
        propertyId: safeString(property && property.propertyId, 160),
        name: safeString(property && property.name, 200),
        providerType: safeString(property && property.providerType, 120) || null,
        reason: safeString(property && property.reason, 300)
      };
    }),
    variants: {
      items: allVariants.slice(page, page + limit),
      total: allVariants.length,
      nextOffset: page + limit < allVariants.length ? page + limit : null
    },
    defaultVariantId: found.defaultVariantId ? safeString(found.defaultVariantId, 80) : null,
    defaultKnown: !!found.defaultKnown,
    slots: (Array.isArray(found.semanticSlots) ? found.semanticSlots : []).slice(0, 128).map(function (slot) {
      return {
        slotId: safeString(slot && slot.slotId, 260),
        kind: safeString(slot && slot.kind, 40),
        name: safeString(slot && slot.name, 200)
      };
    }),
    dependencies: (Array.isArray(found.dependencies) ? found.dependencies : []).slice(0, 64).map(function (dependency) {
      return {
        targetDesignComponentId: dependency && dependency.targetDesignComponentId
          ? safeString(dependency.targetDesignComponentId, 200) : null,
        targetNodeId: dependency && dependency.targetNodeId ? safeString(dependency.targetNodeId, 80) : null,
        layerName: dependency && dependency.layerName ? safeString(dependency.layerName, 300) : null,
        swappable: !!(dependency && dependency.swappable),
        resolved: !!(dependency && dependency.resolved)
      };
    }),
    tokenRefs: (Array.isArray(found.tokenRefs) ? found.tokenRefs : []).slice(0, 128).map(function (ref) {
      return {
        observedTokenKey: safeString(ref && ref.observedTokenKey, 80),
        contextKey: safeString(ref && ref.contextKey, 256),
        sourceId: safeString(ref && ref.sourceId, 80),
        providerName: safeString(ref && ref.providerName, 512),
        field: safeString(ref && ref.field, 120),
        layerName: ref && ref.layerName ? safeString(ref.layerName, 300) : null
      };
    }),
    visualEvidence: {
      coverage: visual && ['none', 'partial', 'representative'].indexOf(visual.coverage) >= 0 ? visual.coverage : 'none',
      entries: visualEntries
    }
  };
}
function componentDetail(id, params, history) {
  var snap = snapshot();
  if (!snap.ok) return snap;
  var component = /^cmp-[a-f0-9]{24}$/.test(String(id || ''));
  var invalid = invalidDetailParams(params, component);
  if (invalid) return Object.assign(publicMeta(snap), {
    ok: false, status: 400, error: 'bad-design-detail-query', field: invalid
  });
  var conflict = checkRevision(snap, params && params.expectedGenerationRevision);
  if (conflict) return conflict;
  var row = snap.byId[id];
  if (!row || row.entityType !== 'component' && row.entityType !== 'project-component') {
    return Object.assign(publicMeta(snap), { ok: false, status: 404, error: 'component-not-found' });
  }
  var tasks = snap.tasks.allByRef;
  var detail = stripPrivate(row, false);
  detail.tasks = ((row.findingId && snap.findingsById[row.findingId] &&
    tasks[snap.findingsById[row.findingId].sourceId]) || []).map(publicTask).filter(Boolean);
  detail.taskHistory = {
    complete: snap.tasks.historyOk,
    reason: snap.tasks.historyOk ? null : 'task-history-index-partial'
  };
  if (row.entityType === 'component') {
    detail.design = componentDesignDetail(snap, row, params);
    detail.usage = relations.componentUsage(row, snap.surfaces.rows);
    detail.history = history && history.forEntity ? history.forEntity(row.id) : { available: false, reason: 'history-not-collected' };
  }
  return Object.assign(publicMeta(snap), {
    ok: true, status: 200,
    component: detail,
    analysis: snap.components.analysis
  });
}
function componentImageFile(componentId, image, expectedRevision) {
  var result = componentImage(componentId, image, expectedRevision);
  return result.ok ? result.file : null;
}
// Serves one content-addressed visual-evidence artifact of the active
// generation. The 32-hex image key must belong to the addressed component's
// own evidence set — no cross-component enumeration.
function componentImage(componentId, image, expectedRevision) {
  var snap = snapshot();
  if (!snap.ok) return snap;
  if (!generation.HASH_RE.test(String(expectedRevision || ''))) {
    return Object.assign(publicMeta(snap), { ok: false, status: 400, error: 'bad-design-image-revision' });
  }
  var conflict = checkRevision(snap, expectedRevision);
  if (conflict) return conflict;
  var row = snap.byId[componentId];
  var h32 = String(image || '');
  if (!row || row.entityType !== 'component' || !/^[a-f0-9]{32}$/.test(h32) ||
      !row.visualHashes || row.visualHashes.indexOf(h32) < 0 || snap.active.mode !== 'generation') {
    return Object.assign(publicMeta(snap), { ok: false, status: 404, error: 'component-image-not-found' });
  }
  var entry = snap.active.manifest.artifacts.find(function (candidate) {
    return candidate.role === 'component-visual-evidence:' + h32;
  });
  var file = entry && generation.readEntry(entry) ? generation.projectFile(entry.path) : null;
  return file ? Object.assign(publicMeta(snap), { ok: true, status: 200, file: file }) :
    Object.assign(publicMeta(snap), { ok: false, status: 404, error: 'component-image-not-found' });
}
function compatibleComparisons(variants) {
  var out = [], total = 0;
  for (var i = 0; i < variants.length; i++) for (var j = i + 1; j < variants.length; j++) {
    var left = variants[i], right = variants[j];
    if (!left.captured || !right.captured || left.platform !== right.platform) continue;
    if (left.locale === right.locale && left.theme !== right.theme) {
      total++; if (out.length < 200) out.push({ mode: 'theme', left: left.id, right: right.id });
    }
    if (left.theme === right.theme && left.locale !== right.locale) {
      total++; if (out.length < 200) out.push({ mode: 'locale', left: left.id, right: right.id });
    }
  }
  return { items: out, total: total, truncated: total > out.length };
}
function surfaceDetail(id, params, history) {
  var snap = snapshot();
  if (!snap.ok) return snap;
  var invalid = invalidDetailParams(params, false);
  if (invalid) return Object.assign(publicMeta(snap), {
    ok: false, status: 400, error: 'bad-design-detail-query', field: invalid
  });
  var conflict = checkRevision(snap, params && params.expectedGenerationRevision);
  if (conflict) return conflict;
  var row = snap.byId[id];
  if (!row || row.entityType !== 'surface') return Object.assign(publicMeta(snap), { ok: false, status: 404, error: 'surface-not-found' });
  var comparisonInfo = compatibleComparisons(row.variants || []);
  return Object.assign(publicMeta(snap), {
    ok: true, status: 200,
    surface: Object.assign(stripPrivate(row, false), {
      comparisons: comparisonInfo.items,
      comparisonCount: comparisonInfo.total,
      comparisonsTruncated: comparisonInfo.truncated,
      notCaptured: (function () {
        var pairs = Object.create(null), missing = [];
        (row.variants || []).forEach(function (variant) {
          pairs[variant.locale + '\0' + variant.platform] = { locale: variant.locale, platform: variant.platform };
        });
        Object.keys(pairs).sort().forEach(function (key) {
          ['light', 'dark'].forEach(function (theme) {
            var pair = pairs[key];
            if (!(row.variants || []).some(function (variant) {
              return variant.theme === theme && variant.locale === pair.locale && variant.platform === pair.platform;
            })) missing.push({ theme: theme, locale: pair.locale, platform: pair.platform, state: 'not-captured' });
          });
        });
        return missing;
      })(),
      evidence: { available: false, reason: 'app-evidence-not-captured' },
      preview: preview.capability(row),
      relations: { module: row.module, feature: row.feature, route: row.route, codeSources: row.codeSources || [] },
      usedComponents: (row.usedComponents || []).map(function (item) {
        return { name: item.name, nodeId: item.nodeId || null, nodeUrl: item.nodeUrl || null };
      }).slice(0, 500),
      history: history && history.forEntity ? history.forEntity(row.id) : { available: false, reason: 'history-not-collected' },
      tasks: ((row.findingId && snap.findingsById[row.findingId] &&
        snap.tasks.allByRef[snap.findingsById[row.findingId].sourceId]) || []).map(publicTask).filter(Boolean),
      taskHistory: {
        complete: snap.tasks.historyOk,
        reason: snap.tasks.historyOk ? null : 'task-history-index-partial'
      }
    })
  });
}
function surfaceImageFile(surfaceId, variantId, expectedRevision) {
  var result = surfaceImage(surfaceId, variantId, expectedRevision);
  return result.ok ? result.file : null;
}
function surfaceImage(surfaceId, variantId, expectedRevision) {
  var snap = snapshot();
  if (!snap.ok) return snap;
  if (!generation.HASH_RE.test(String(expectedRevision || ''))) {
    return Object.assign(publicMeta(snap), { ok: false, status: 400, error: 'bad-design-image-revision' });
  }
  var conflict = checkRevision(snap, expectedRevision);
  if (conflict) return conflict;
  var row = snap.byId[surfaceId];
  if (!row || row.entityType !== 'surface') {
    return Object.assign(publicMeta(snap), { ok: false, status: 404, error: 'surface-image-not-found' });
  }
  var variant = (row.variants || []).find(function (item) { return item.id === variantId && item.captured; });
  if (!variant) return Object.assign(publicMeta(snap), { ok: false, status: 404, error: 'surface-image-not-found' });
  var file;
  if (variant._imageFile && variant._imageFile !== row.name + '.png' && variant._imageFile !== row.name + '.dark.png') {
    file = figmaScreens.surfaceVariantImageFile(row.sourceStem, row.name, variant._imageFile, snap.active);
  } else {
    file = figmaScreens.screenImageFile(
      row.sourceStem, row.name, variant.theme === 'dark' ? 'dark' : 'primary', snap.active
    );
  }
  return file ? Object.assign(publicMeta(snap), { ok: true, status: 200, file: file }) :
    Object.assign(publicMeta(snap), { ok: false, status: 404, error: 'surface-image-not-found' });
}
function historyProjection() {
  var snap = snapshot();
  if (!snap.ok) return snap;
  var entityIds = Object.keys(snap.byId).sort();
  if (entityIds.length > 10000) {
    return Object.assign(publicMeta(snap), {
      ok: false, status: 422, error: 'design-history-entity-limit',
      entityCount: entityIds.length, entityLimit: 10000
    });
  }
  var entities = {};
  entityIds.forEach(function (id) {
    var row = snap.byId[id], projection = {
      id: id, entityType: row.entityType, name: row.name,
      // Project-only tokens/components carry no comparison status; their
      // classification is the change-relevant axis the history diff tracks.
      status: row.entityType === 'project-token' || row.entityType === 'project-component'
        ? row.classification : row.status,
      summary: row.entityType === 'token'
        ? { kind: row.kind, values: row.figmaValues, changedSide: row.changedSide, mappingState: row.mappingState }
        : row.entityType === 'project-token'
          ? { kind: row.kind, projectTokenId: row.projectTokenId, classification: row.classification }
          : row.entityType === 'component'
            ? { kind: row.kind, changedSide: row.changedSide, mappingState: row.mappingState, mappingId: row.mappingId }
            : row.entityType === 'project-component'
              ? { kind: row.kind, projectComponentId: row.projectComponentId, classification: row.classification }
              : { type: row.type, themes: row.themes, locales: row.locales }
    };
    entities[id] = { hash: hash(projection), projection: projection };
  });
  return Object.assign(publicMeta(snap), {
    ok: true, createdAt: snap.active.mode === 'generation' ? snap.active.manifest.createdAt : new Date().toISOString(),
    syncJobId: snap.active.mode === 'generation' ? snap.active.manifest.syncJobId || null : null,
    entities: entities
  });
}

module.exports = {
  DEFAULT_LIMIT: DEFAULT_LIMIT,
  MAX_LIMIT: MAX_LIMIT,
  TOKEN_STATUS: TOKEN_STATUS,
  TOKEN_CHANGED_SIDE: TOKEN_CHANGED_SIDE,
  PROJECT_TOKEN_CLASSIFICATION: PROJECT_TOKEN_CLASSIFICATION,
  COMPONENT_STATUS: COMPONENT_STATUS,
  COMPONENT_CHANGED_SIDE: COMPONENT_CHANGED_SIDE,
  PROJECT_COMPONENT_CLASSIFICATION: PROJECT_COMPONENT_CLASSIFICATION,
  stableId: stableId,
  snapshot: snapshot,
  listTokens: listTokens,
  listProjectOnlyTokens: listProjectOnlyTokens,
  tokenDetail: tokenDetail,
  listComponents: listComponents,
  listProjectOnlyComponents: listProjectOnlyComponents,
  listSurfaces: listSurfaces,
  componentDetail: componentDetail,
  componentImage: componentImage,
  surfaceDetail: surfaceDetail,
  surfaceImage: surfaceImage,
  historyProjection: historyProjection,
  checkRevision: checkRevision,
  publicMeta: publicMeta,
  publicTask: publicTask,
  _test: {
    safePath: safePath,
    safeFigmaUrl: safeFigmaUrl
  }
};
