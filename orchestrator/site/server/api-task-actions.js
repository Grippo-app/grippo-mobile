'use strict';

// Server-resolved, generation-bound API work packages. The browser supplies
// the exact selected source ids; grouping, prose and provenance are rebuilt
// from committed local evidence at preview and create time.

var crypto = require('crypto');
var catalog = require('./api-catalog');
var relations = require('./api-relations');
var taskSource = require('./task-source');
var backlogCreate = require('./backlog-create');
var apiWorkPackage = require('../../tasks/api-work-package-contract.cjs');

var MAX_SELECTED_ITEMS = apiWorkPackage.MAX_SOURCES;
var MAX_PREVIEWS = 1000;
var PREVIEW_TTL = 5 * 60 * 1000;
var PREVIEW_RE = /^atp-[a-f0-9]{32}$/;
var previews = Object.create(null);
var REPORT_ROLES = ['changes', 'consumers', 'drift', 'implementation'];
var MODES = Object.freeze({ package: true, hotfix: true });

function exact(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}

function hash(value) {
  return 'sha256:' + crypto.createHash('sha256')
    .update(
      typeof value === 'string' || Buffer.isBuffer(value)
        ? value : JSON.stringify(value),
      'utf8'
    )
    .digest('hex');
}

function randomId() {
  return 'atp-' + crypto.randomBytes(16).toString('hex');
}

function prune() {
  Object.keys(previews).forEach(function (id) {
    if (Date.parse(previews[id].expiresAt) <= Date.now()) delete previews[id];
  });
}

function truncateScalars(value, maximum) {
  return Array.from(String(value == null ? '' : value)).slice(0, maximum).join('');
}

function cleanLine(value, fallback, max) {
  var text = String(value == null ? '' : value).normalize('NFC')
    .replace(/[\x00-\x1f\x7f-\x9f\u2028\u2029]+/g, ' ')
    .replace(/\s+/g, ' ').trim();
  text = truncateScalars(text, max || 500);
  return text || fallback;
}

function markdown(value, fallback, max) {
  return cleanLine(value, fallback, max)
    .replace(/([\\`*_{}\[\]<>()#+.!|>~-])/g, '\\$1');
}

function inlineCode(value, fallback, max) {
  return cleanLine(value, fallback, max).replace(/`/g, "'");
}

function validSourceId(value) {
  return typeof value === 'string' && value.length <= 256 &&
    apiWorkPackage.SOURCE_ID_RE.test(value);
}

function validReportHashes(value) {
  return exact(value, REPORT_ROLES) && REPORT_ROLES.every(function (role) {
    return value[role] === null ||
      taskSource.HASH_RE.test(String(value[role] || ''));
  });
}

function sameReportHashes(left, right) {
  return validReportHashes(left) && validReportHashes(right) &&
    REPORT_ROLES.every(function (role) { return left[role] === right[role]; });
}

function validPreviewRequest(request) {
  return exact(request, [
    'expectedGenerationId',
    'expectedReportHashes',
    'expectedTaskIndexRevision',
    'mode',
    'sourceIds'
  ]) &&
    typeof request.expectedGenerationId === 'string' &&
    !!MODES[request.mode] &&
    validReportHashes(request.expectedReportHashes) &&
    taskSource.HASH_RE.test(String(request.expectedTaskIndexRevision || '')) &&
    Array.isArray(request.sourceIds) &&
    request.sourceIds.length > 0 &&
    request.sourceIds.length <= MAX_SELECTED_ITEMS &&
    (request.mode !== 'hotfix' || request.sourceIds.length === 1) &&
    request.sourceIds.every(validSourceId) &&
    new Set(request.sourceIds).size === request.sourceIds.length;
}

function publicTask(row) {
  return relations.publicTask(row);
}

function publicItem(row, tasks) {
  return {
    sourceId: row.sourceId,
    type: row.type,
    title: row.title,
    summary: row.summary,
    operationId: row.operationId || null,
    areas: row.areas.slice(),
    modelIds: row.modelIds.slice(),
    existingTask: tasks && tasks.open && tasks.open.length
      ? tasks.open[0] : null
  };
}

function sourceFingerprint(row, snapshot) {
  return hash({
    schemaVersion: 2,
    sourceId: row.sourceId,
    type: row.type,
    committedGenerationId: snapshot.committedGenerationId,
    contractHash: snapshot.contractHash,
    environmentId: snapshot.environmentId,
    projectCodeRevision: snapshot.projectCodeRevision,
    reportHashes: snapshot.reportHashes,
    evidence: row.evidence,
    relations: {
      areas: row.areas,
      operationIds: row.operationIds,
      modelIds: row.modelIds,
      primaryArea: row.primaryArea,
      primaryModelId: row.primaryModelId
    }
  });
}

function uniqueSorted(values) {
  return Array.from(new Set((values || []).filter(Boolean))).sort();
}

function compareText(left, right) {
  left = String(left || '');
  right = String(right || '');
  return left < right ? -1 : left > right ? 1 : 0;
}

function related(row, normalized) {
  var areas = [];
  var operationIds = [];
  var modelIds = [];
  var primaryArea = row.evidence && row.evidence.area || null;
  var primaryModelId = row.evidence && row.evidence.modelId || null;
  function includeEndpoint(endpoint) {
    if (!endpoint) return;
    if (!primaryArea && row.operationId === endpoint.operationId) {
      primaryArea = endpoint.area;
    }
    areas.push(endpoint.area);
    operationIds.push(endpoint.operationId);
    modelIds = modelIds.concat(
      endpoint.models.requestIds || [],
      endpoint.models.responseIds || []
    );
  }
  if (row.operationId &&
      Object.prototype.hasOwnProperty.call(
        normalized.byOperation, row.operationId
      )) {
    includeEndpoint(normalized.byOperation[row.operationId]);
  }
  if (row.evidence && row.evidence.area) areas.push(row.evidence.area);
  if (row.operationId) operationIds.push(row.operationId);
  if (row.evidence && row.evidence.modelId) {
    modelIds.push(row.evidence.modelId);
    (normalized.byModel[row.evidence.modelId] || []).forEach(includeEndpoint);
  }
  row.areas = uniqueSorted(areas);
  row.operationIds = uniqueSorted(operationIds);
  row.modelIds = uniqueSorted(modelIds);
  row.primaryArea = primaryArea;
  row.primaryModelId = primaryModelId;
  return row;
}

function finishRow(row, snapshot, normalized) {
  related(row, normalized);
  row.fingerprint = sourceFingerprint(row, snapshot);
  var taskRefs = [row.sourceId];
  // Pre-workspace API coverage tasks used the raw operation id as Source.ref.
  // Preserve their coverage instead of creating a duplicate package.
  if (row.operationId) taskRefs.push(row.operationId);
  row.tasks = relations.taskProjection(snapshot.tasks, taskRefs);
  return row;
}

function resolvedRows(snapshot) {
  if (snapshot._apiTaskRows) return snapshot._apiTaskRows;
  var normalized = catalog.normalized(snapshot);
  var bySourceId = Object.create(null);

  normalized.rows.forEach(function (endpoint) {
    if (endpoint.implementation.state !== 'missing') return;
    var sourceId = relations.sourceId('missing', endpoint.operationId);
    bySourceId[sourceId] = finishRow({
      sourceId: sourceId,
      type: 'api-missing',
      operationId: endpoint.operationId,
      title: cleanLine(
        'Implement API endpoint ' + endpoint.method + ' ' + endpoint.path,
        'Implement API endpoint',
        200
      ),
      summary: 'The current project analyzer found no implementation for this committed endpoint.',
      evidence: {
        operationId: endpoint.operationId,
        method: endpoint.method,
        path: endpoint.path,
        area: endpoint.area,
        auth: endpoint.auth,
        implementation: endpoint.implementation
      }
    }, snapshot, normalized);
  });

  normalized.changes.forEach(function (change) {
    bySourceId[change.sourceId] = finishRow({
      sourceId: change.sourceId,
      type: 'api-change',
      operationId: change.operationId,
      title: cleanLine(
        'Review API change: ' +
          (change.afterSummary || change.beforeSummary || change.kind),
        'Review API change',
        200
      ),
      summary: change.afterSummary || change.beforeSummary || change.kind,
      evidence: {
        id: change.id,
        kind: change.kind,
        severity: change.severity,
        operationId: change.operationId,
        modelId: change.modelId,
        beforeSummary: change.beforeSummary,
        afterSummary: change.afterSummary,
        evidence: change.evidence
      }
    }, snapshot, normalized);
  });

  normalized.mismatches.forEach(function (mismatch) {
    bySourceId[mismatch.sourceId] = finishRow({
      sourceId: mismatch.sourceId,
      type: 'api-mismatch',
      operationId: mismatch.operationId,
      title: cleanLine(
        'Resolve observed API mismatch: ' +
          (mismatch.message || mismatch.kind),
        'Resolve observed API mismatch',
        200
      ),
      summary: mismatch.message || mismatch.kind,
      evidence: {
        id: mismatch.id,
        kind: mismatch.kind,
        severity: mismatch.severity,
        operationId: mismatch.operationId,
        area: mismatch.area,
        modelId: mismatch.modelId,
        field: mismatch.field,
        message: mismatch.message,
        suggestion: mismatch.suggestion,
        file: mismatch.file
      }
    }, snapshot, normalized);
  });

  snapshot._apiTaskRows = {
    bySourceId: bySourceId,
    rows: Object.keys(bySourceId).sort().map(function (sourceId) {
      return bySourceId[sourceId];
    })
  };
  return snapshot._apiTaskRows;
}

function resolve(snapshot, sourceId) {
  return resolvedRows(snapshot).bySourceId[sourceId] || null;
}

function groupFragment(value) {
  var text = String(value || '').toLowerCase();
  return /^[a-z0-9][a-z0-9-]{0,55}$/.test(text)
    ? text : hash(String(value)).slice(7, 31);
}

function groupFor(row, snapshot) {
  if (row.primaryArea || row.areas.length === 1) {
    var area = row.primaryArea || row.areas[0];
    return {
      key: 'area:' + groupFragment(area),
      label: 'area ' + area
    };
  }
  if (row.primaryModelId || row.modelIds.length) {
    var modelId = row.primaryModelId || row.modelIds[0];
    return {
      key: 'model:' + groupFragment(modelId),
      label: 'shared model ' + modelId
    };
  }
  if (row.operationIds.length) {
    return {
      key: 'operation:' + groupFragment(row.operationIds[0]),
      label: 'endpoint ' + row.operationIds[0]
    };
  }
  var changeSetId = snapshot.changes && snapshot.changes.current &&
    snapshot.changes.value && snapshot.changes.value.changeSetId;
  if (row.type === 'api-change' && changeSetId) {
    return {
      key: 'change-set:' + groupFragment(changeSetId),
      label: 'change set ' + changeSetId
    };
  }
  return {
    key: 'mixed:' + hash(row.sourceId).slice(7, 31),
    label: 'unscoped API finding'
  };
}

function compareRows(left, right) {
  var priorities = { 'api-change': 0, 'api-mismatch': 1, 'api-missing': 2 };
  return (priorities[left.type] - priorities[right.type]) ||
    compareText(left.operationIds[0], right.operationIds[0]) ||
    compareText(left.modelIds[0], right.modelIds[0]) ||
    compareText(left.sourceId, right.sourceId);
}

function packageFingerprint(metadata, rows, snapshot) {
  return hash({
    schemaVersion: 1,
    metadata: metadata,
    committedGenerationId: snapshot.committedGenerationId,
    contractHash: snapshot.contractHash,
    environmentId: snapshot.environmentId,
    projectCodeRevision: snapshot.projectCodeRevision,
    reportHashes: snapshot.reportHashes,
    sources: rows.map(function (row) {
      return { sourceId: row.sourceId, fingerprint: row.fingerprint };
    })
  });
}

function packageAction(group, rows, snapshot) {
  var sourceIds = rows.map(function (row) { return row.sourceId; }).sort();
  var metadata = apiWorkPackage.create(group.key, sourceIds);
  return {
    id: metadata.packageId,
    mode: 'package',
    group: group,
    rows: rows,
    sourceIds: sourceIds,
    metadata: metadata,
    title: cleanLine(
      'API work package: ' + group.label + ' (' + rows.length + ' findings)',
      'API work package',
      200
    ),
    fingerprint: packageFingerprint(metadata, rows, snapshot)
  };
}

function hotfixAction(row) {
  return {
    id: 'hotfix-' + hash(row.sourceId).slice(7, 31),
    mode: 'hotfix',
    group: null,
    rows: [row],
    sourceIds: [row.sourceId],
    metadata: null,
    title: row.title,
    fingerprint: row.fingerprint
  };
}

function publicAction(action) {
  return {
    id: action.id,
    mode: action.mode,
    title: action.title,
    group: action.group ? {
      key: action.group.key,
      label: action.group.label
    } : null,
    sourceCount: action.sourceIds.length,
    sources: action.rows.map(function (row) {
      return publicItem(row, row.tasks);
    })
  };
}

function plan(snapshot, selectedRows, mode) {
  var selectedIds = selectedRows.map(function (row) { return row.sourceId; });
  if (mode === 'hotfix') {
    var hotfixExisting = selectedRows.filter(function (row) {
      return row.tasks.open.length;
    });
    return {
      mode: mode,
      selectedIds: selectedIds,
      scopeRows: selectedRows.slice(),
      actions: selectedRows.length && !hotfixExisting.length
        ? [hotfixAction(selectedRows[0])] : [],
      existing: hotfixExisting,
      blocked: []
    };
  }

  var scopeRows = selectedRows.slice();
  if (scopeRows.length > MAX_SELECTED_ITEMS) {
    return {
      error: 'api-package-scope-too-large',
      scopeSize: scopeRows.length
    };
  }

  var existing = scopeRows.filter(function (row) {
    return row.tasks.open.length;
  });
  var uncovered = scopeRows.filter(function (row) {
    return !row.tasks.open.length;
  });
  var byGroup = Object.create(null);
  uncovered.forEach(function (row) {
    var group = groupFor(row, snapshot);
    if (!byGroup[group.key]) {
      byGroup[group.key] = { group: group, rows: [] };
    }
    byGroup[group.key].rows.push(row);
  });

  var actions = [];
  var blocked = [];
  var oversized = false;
  Object.keys(byGroup).sort().forEach(function (key) {
    if (oversized) return;
    var entry = byGroup[key];
    entry.rows.sort(compareRows);
    if (entry.rows.length < 2) {
      blocked.push({
        sourceId: entry.rows[0].sourceId,
        reason: 'api-package-requires-hotfix',
        item: publicItem(entry.rows[0], entry.rows[0].tasks)
      });
      return;
    }
    var action = packageAction(entry.group, entry.rows, snapshot);
    if (!packageFits(action, snapshot)) {
      oversized = true;
      return;
    }
    actions.push(action);
  });

  if (oversized) {
    return {
      error: 'api-package-scope-too-large',
      scopeSize: scopeRows.length
    };
  }

  return {
    mode: mode,
    selectedIds: selectedIds,
    scopeRows: scopeRows,
    actions: actions,
    existing: existing,
    blocked: blocked
  };
}

function itemSetHash(value, skipped) {
  return hash({
    schemaVersion: 2,
    mode: value.mode,
    selectedIds: value.selectedIds,
    scope: value.scopeRows.map(function (row) {
      return { sourceId: row.sourceId, fingerprint: row.fingerprint };
    }).sort(function (left, right) {
      return compareText(left.sourceId, right.sourceId);
    }),
    actions: value.actions.map(function (action) {
      return {
        id: action.id,
        mode: action.mode,
        fingerprint: action.fingerprint,
        sourceIds: action.sourceIds
      };
    }),
    existing: value.existing.map(function (row) {
      return row.sourceId;
    }).sort(),
    blocked: value.blocked.map(function (row) {
      return row.sourceId;
    }).sort(),
    skipped: (skipped || []).map(function (row) {
      return row.sourceId;
    }).sort()
  });
}

function preview(request) {
  prune();
  if (!validPreviewRequest(request)) {
    return {
      ok: false,
      status: 400,
      error: 'bad-api-task-preview-request'
    };
  }

  var snapshot = relations.snapshot({ freshInputs: true });
  if (!snapshot.ok) return snapshot;
  var conflict = relations.checkExpected(
    snapshot,
    request.expectedGenerationId
  );
  if (conflict) return conflict;
  if (snapshot.empty) {
    return Object.assign(relations.meta(snapshot), {
      ok: false,
      status: 409,
      error: 'api-generation-missing'
    });
  }
  if (!snapshot.tasks.revision || !snapshot.tasks.ok) {
    return Object.assign(relations.meta(snapshot), {
      ok: false,
      status: 409,
      error: snapshot.tasks.revision
        ? 'task-origin-index-partial' : 'task-index-unavailable'
    });
  }
  if (snapshot.tasks.revision !== request.expectedTaskIndexRevision) {
    return Object.assign(relations.meta(snapshot), {
      ok: false,
      status: 409,
      error: 'task-conflict'
    });
  }
  if (!sameReportHashes(snapshot.reportHashes, request.expectedReportHashes)) {
    return Object.assign(relations.meta(snapshot), {
      ok: false,
      status: 409,
      error: 'api-source-stale'
    });
  }

  var rows = [];
  var skipped = [];
  request.sourceIds.forEach(function (sourceId) {
    var row = resolve(snapshot, sourceId);
    if (row) rows.push(row);
    else skipped.push({ sourceId: sourceId, reason: 'api-source-stale' });
  });
  var packagePlan = plan(snapshot, rows, request.mode);
  if (packagePlan.error) {
    return Object.assign(relations.meta(snapshot), {
      ok: false,
      status: 409,
      error: packagePlan.error,
      scopeSize: packagePlan.scopeSize
    });
  }
  if (Object.keys(previews).length >= MAX_PREVIEWS) {
    return Object.assign(relations.meta(snapshot), {
      ok: false,
      status: 503,
      error: 'api-task-preview-capacity'
    });
  }

  var id = randomId();
  var now = new Date().toISOString();
  var held = previews[id] = {
    id: id,
    createdAt: now,
    expiresAt: new Date(Date.parse(now) + PREVIEW_TTL).toISOString(),
    generationId: snapshot.committedGenerationId,
    contractHash: snapshot.contractHash,
    reportHashes: Object.assign({}, snapshot.reportHashes),
    taskIndexRevision: snapshot.tasks.revision,
    itemSetHash: itemSetHash(packagePlan, skipped),
    mode: request.mode,
    selectedIds: request.sourceIds.slice()
  };
  return Object.assign(relations.meta(snapshot), {
    ok: true,
    status: 200,
    preview: {
      id: held.id,
      createdAt: held.createdAt,
      expiresAt: held.expiresAt,
      itemSetHash: held.itemSetHash,
      taskIndexRevision: held.taskIndexRevision,
      mode: held.mode,
      counts: {
        selected: rows.length,
        tasksCreate: packagePlan.actions.length,
        sourcesCreate: packagePlan.actions.reduce(
          function (count, action) {
            return count + action.sourceIds.length;
          },
          0
        ),
        sourcesExisting: packagePlan.existing.length,
        blocked: packagePlan.blocked.length,
        skipped: skipped.length
      },
      actions: packagePlan.actions.map(publicAction),
      existing: packagePlan.existing.map(function (row) {
        return publicItem(row, row.tasks);
      }),
      blocked: packagePlan.blocked,
      skipped: skipped
    }
  });
}

function hotfixBody(row, snapshot) {
  var evidence = row.evidence || {};
  var lines = [
    '## Goal',
    '',
    markdown(row.summary, 'Resolve the current API finding.', 1000) + '.',
    '',
    '## Hotfix scope',
    '',
    '- This is an explicit one-source exception, not an API work package.',
    '- Source ID: `' + row.sourceId + '`',
    '- Contract generation: `' + snapshot.committedGenerationId + '`',
    '- Contract hash: `' + snapshot.contractHash + '`',
    '- Environment: `' + snapshot.environmentId + '`'
  ];
  if (row.operationId) {
    lines.push(
      '- Operation ID: `' +
      inlineCode(row.operationId, 'unknown', 200) + '`'
    );
  }
  if (evidence.method && evidence.path) {
    lines.push(
      '- Endpoint: `' + evidence.method + ' ' +
      inlineCode(evidence.path, '/', 500) + '`'
    );
  }
  if (evidence.modelId) {
    lines.push(
      '- Model: `' + inlineCode(evidence.modelId, 'unknown', 200) + '`'
    );
  }
  if (evidence.severity) {
    lines.push(
      '- Severity: `' + inlineCode(evidence.severity, 'unknown', 40) + '`'
    );
  }
  (Array.isArray(evidence.evidence) ? evidence.evidence : [])
    .slice(0, 10)
    .forEach(function (item) {
      lines.push(
        '- Evidence: ' + markdown(item, 'See Project → API.', 1000)
      );
    });
  if (evidence.file) {
    lines.push(
      '- Project-relative evidence: `' +
      inlineCode(evidence.file, 'unknown', 500) + '`'
    );
  }
  lines.push(
    '',
    '## Acceptance',
    '',
    '- The implementation is reconciled with the current committed API contract without weakening validation.',
    '- Relevant automated tests cover the changed behavior and edge cases.',
    '- Project → API no longer reports this source as open after the appropriate local analysis is rerun.',
    '',
    '## Out of scope',
    '',
    '- Expanding this exception into unrelated API changes.',
    '- Changing the backend contract solely to hide a client-side finding.',
    '- Treating unavailable analysis as proof that no impact exists.'
  );
  return lines.join('\n');
}

function packageBody(action, snapshot) {
  var lines = [
    apiWorkPackage.render(action.metadata),
    '',
    '## Goal',
    '',
    'Reconcile this cohesive API scope with the committed contract as one delivery unit.',
    '',
    '## Inputs',
    '',
    '- Contract generation: `' + snapshot.committedGenerationId + '`',
    '- Contract hash: `' + snapshot.contractHash + '`',
    '- Environment: `' + snapshot.environmentId + '`',
    '- Group: `' + inlineCode(action.group.key, 'mixed:unknown', 100) + '`',
    '',
    '## Scope',
    ''
  ];
  action.rows.forEach(function (row) {
    var relation = [];
    if (row.operationIds.length) {
      relation.push('operations: ' + row.operationIds.join(', '));
    }
    if (row.modelIds.length) {
      relation.push('models: ' + row.modelIds.join(', '));
    }
    lines.push(
      '- `' + row.sourceId + '` — ' +
      markdown(row.summary, row.title, 1000) +
      (relation.length
        ? ' (' +
          markdown(relation.join('; '), 'related API scope', 1000) + ')'
        : '')
    );
  });
  lines.push(
    '',
    '## Delivery sequence',
    '',
    '1. Reconcile shared contract models, serialization, validation, and compatibility decisions.',
    '2. Update endpoint implementations and typed API boundaries in dependency order.',
    '3. Update known consumers, mappers, and error handling without weakening safeguards.',
    '4. Add or update focused automated tests, then rerun the current API analyses.',
    '',
    '## Acceptance',
    '',
    '- Every source ID listed in the canonical API Work Package section is resolved by the same coherent implementation.',
    '- Relevant automated tests cover changed behavior, compatibility boundaries, and edge cases.',
    '- Project → API no longer reports any listed source as open after the appropriate local analyses are rerun.',
    '- No unrelated backend-contract change is introduced solely to hide a client-side finding.',
    '',
    '## Out of scope',
    '',
    '- Treating unavailable or partial analysis as proof that no impact exists.',
    '- Silently dropping a listed source from delivery; split the task explicitly if scope must change.'
  );
  return lines.join('\n');
}

function packageCreateInput(action, snapshot, previewId) {
  var source = {
    kind: 'api',
    type: 'api-work-package',
    ref: 'api:package:' + action.metadata.packageId,
    fingerprint: action.fingerprint
  };
  return {
    title: action.title,
    body: packageBody(action, snapshot),
    source: source,
    idempotencyKey:
      'api.package.' + previewId.slice(4) + '.' +
      action.metadata.packageId.slice(4),
    dedupKey: 'api.package.' + action.metadata.packageId,
    dedupReport: action.fingerprint
  };
}

function packageFits(action, snapshot) {
  try {
    backlogCreate.validateInput(packageCreateInput(
      action,
      snapshot,
      'atp-00000000000000000000000000000000'
    ));
    return true;
  } catch (error) {
    if (error && error.code === 'bad-body') return false;
    throw error;
  }
}

function staleError(code) {
  return Object.assign(new Error(code), {
    code: code,
    httpStatus: 409
  });
}

function verifyActionUnderLease(action, expected) {
  var current = relations.snapshot({ freshInputs: true });
  if (!current.ok ||
      current.empty ||
      current.committedGenerationId !== expected.committedGenerationId ||
      current.contractHash !== expected.contractHash ||
      current.environmentId !== expected.environmentId) {
    throw staleError('api-generation-conflict');
  }
  if (!current.tasks || !current.tasks.ok || !current.tasks.revision) {
    throw staleError('task-conflict');
  }
  action.rows.forEach(function (row) {
    var resolved = resolve(current, row.sourceId);
    if (!resolved || resolved.fingerprint !== row.fingerprint) {
      throw staleError('api-source-stale');
    }
    if (resolved.tasks.open.length) {
      throw staleError('task-conflict');
    }
  });
  return true;
}

function createHotfix(action, snapshot, previewId) {
  var row = action.rows[0];
  var source = {
    kind: 'api',
    type: row.type,
    ref: row.sourceId,
    fingerprint: row.fingerprint
  };
  var identity = hash(row.sourceId).slice(7);
  return backlogCreate.create({
    title: row.title,
    body: hotfixBody(row, snapshot),
    source: source,
    idempotencyKey:
      'api.hotfix.' + previewId.slice(4) + '.' + identity.slice(0, 32),
    dedupKey: 'api.' + identity,
    dedupReport: source.fingerprint
  }, {
    preflight: function () {
      return verifyActionUnderLease(action, snapshot);
    }
  });
}

function createPackage(action, snapshot, previewId) {
  return backlogCreate.create(packageCreateInput(
    action,
    snapshot,
    previewId
  ), {
    preflight: function () {
      return verifyActionUnderLease(action, snapshot);
    }
  });
}

function createAction(action, snapshot, previewId) {
  return action.mode === 'hotfix'
    ? createHotfix(action, snapshot, previewId)
    : createPackage(action, snapshot, previewId);
}

function create(request) {
  if (!exact(request, [
    'expectedGenerationId',
    'expectedItemSetHash',
    'expectedReportHashes',
    'expectedTaskIndexRevision',
    'previewId'
  ]) ||
      !PREVIEW_RE.test(String(request.previewId || '')) ||
      !relations.GENERATION_RE.test(
        String(request.expectedGenerationId || '')
      ) ||
      !taskSource.HASH_RE.test(
        String(request.expectedItemSetHash || '')
      ) ||
      !taskSource.HASH_RE.test(
        String(request.expectedTaskIndexRevision || '')
      ) ||
      !validReportHashes(request.expectedReportHashes)) {
    return Promise.resolve({
      ok: false,
      status: 400,
      error: 'bad-api-task-create-request'
    });
  }

  prune();
  var held = previews[request.previewId];
  if (!held) {
    return Promise.resolve({
      ok: false,
      status: 409,
      error: 'api-task-preview-expired'
    });
  }
  if (held.inFlight) {
    return Promise.resolve({
      ok: false,
      status: 409,
      error: 'api-task-preview-in-flight'
    });
  }
  if (held.generationId !== request.expectedGenerationId ||
      held.itemSetHash !== request.expectedItemSetHash ||
      held.taskIndexRevision !== request.expectedTaskIndexRevision ||
      !sameReportHashes(held.reportHashes, request.expectedReportHashes)) {
    return Promise.resolve({
      ok: false,
      status: 409,
      error: 'api-task-preview-conflict'
    });
  }

  var snapshot = relations.snapshot({ freshInputs: true });
  if (!snapshot.ok) return Promise.resolve(snapshot);
  if (snapshot.empty ||
      snapshot.committedGenerationId !== held.generationId ||
      snapshot.contractHash !== held.contractHash) {
    delete previews[held.id];
    return Promise.resolve(Object.assign(relations.meta(snapshot), {
      ok: false,
      status: 409,
      error: 'api-generation-conflict'
    }));
  }
  if (!sameReportHashes(snapshot.reportHashes, held.reportHashes)) {
    delete previews[held.id];
    return Promise.resolve(Object.assign(relations.meta(snapshot), {
      ok: false,
      status: 409,
      error: 'api-source-stale'
    }));
  }
  if (!snapshot.tasks.ok ||
      snapshot.tasks.revision !== held.taskIndexRevision) {
    delete previews[held.id];
    return Promise.resolve(Object.assign(relations.meta(snapshot), {
      ok: false,
      status: 409,
      error: 'task-conflict',
      currentTaskIndexRevision: snapshot.tasks.revision
    }));
  }

  var selectedRows = [];
  var skipped = [];
  held.selectedIds.forEach(function (sourceId) {
    var row = resolve(snapshot, sourceId);
    if (row) selectedRows.push(row);
    else skipped.push({ sourceId: sourceId, reason: 'api-source-stale' });
  });
  var packagePlan = plan(snapshot, selectedRows, held.mode);
  if (packagePlan.error ||
      itemSetHash(packagePlan, skipped) !== held.itemSetHash) {
    delete previews[held.id];
    return Promise.resolve(Object.assign(relations.meta(snapshot), {
      ok: false,
      status: 409,
      error: 'api-source-stale'
    }));
  }

  var result = {
    created: [],
    existing: [],
    existingSources: packagePlan.existing.map(function (row) {
      return {
        sourceId: row.sourceId,
        task: row.tasks.open[0]
      };
    }),
    blocked: packagePlan.blocked,
    skipped: skipped,
    failed: []
  };
  var createdSources = 0;
  var dedupedSources = 0;
  held.inFlight = true;
  return packagePlan.actions.reduce(function (promise, action) {
    return promise.then(function () {
      return createAction(action, snapshot, held.id).then(function (created) {
        var output = {
          id: action.id,
          mode: action.mode,
          sourceIds: action.sourceIds.slice(),
          task: publicTask(created.task),
          stem: created.stem
        };
        if (created.effect === 'domain-dedup' || created.deduped) {
          dedupedSources += action.sourceIds.length;
          result.existing.push(output);
        } else {
          createdSources += action.sourceIds.length;
          result.created.push(output);
        }
      }).catch(function (error) {
        result.failed.push({
          id: action.id,
          mode: action.mode,
          sourceIds: action.sourceIds.slice(),
          error: error &&
            (error.code === 'api-source-stale' ||
              error.code === 'api-generation-conflict' ||
              error.code === 'task-conflict')
            ? error.code
            : backlogCreate.publicCreateErrorCode(error)
        });
      });
    });
  }, Promise.resolve()).then(function () {
    delete previews[held.id];
    var partial = result.failed.length > 0 ||
      result.skipped.length > 0 ||
      result.blocked.length > 0;
    return Object.assign(relations.meta(snapshot), {
      ok: true,
      partial: partial,
      status: partial ? 207 : 200,
      result: result,
      counts: {
        tasksCreated: result.created.length,
        tasksExisting: result.existing.length,
        sourcesCovered: createdSources,
        sourcesAlreadyCovered:
          result.existingSources.length + dedupedSources,
        blocked: result.blocked.length,
        skipped: result.skipped.length,
        failed: result.failed.length
      }
    });
  }, function () {
    delete previews[held.id];
    return Object.assign(relations.meta(snapshot), {
      ok: false,
      status: 500,
      error: 'api-task-create-failed'
    });
  });
}

function cancel(request) {
  prune();
  if (!exact(request, ['previewId']) ||
      !PREVIEW_RE.test(String(request.previewId || ''))) {
    return {
      ok: false,
      status: 400,
      error: 'bad-api-task-cancel-request'
    };
  }
  var held = previews[request.previewId];
  if (!held) return { ok: true, status: 200, cancelled: false };
  if (held.inFlight) {
    return {
      ok: false,
      status: 409,
      error: 'api-task-preview-in-flight'
    };
  }
  delete previews[request.previewId];
  return { ok: true, status: 200, cancelled: true };
}

module.exports = {
  MAX_SELECTED_ITEMS: MAX_SELECTED_ITEMS,
  preview: preview,
  create: create,
  cancel: cancel,
  _test: {
    validSourceId: validSourceId,
    validReportHashes: validReportHashes,
    validPreviewRequest: validPreviewRequest,
    hotfixBody: hotfixBody,
    packageBody: packageBody,
    plan: plan
  }
};
