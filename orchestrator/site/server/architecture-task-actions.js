'use strict';

// Two-phase, server-resolved Architecture finding task creation. The browser
// supplies only current immutable identities; title, Markdown and Source are
// derived again from the validated current map immediately before creation.

var crypto = require('crypto');
var arch = require('./arch');
var contract = require('./architecture-contract');
var taskSource = require('./task-source');
var backlogCreate = require('./backlog-create');

var PREVIEW_RE = /^atp-[a-f0-9]{32}$/;
var TTL_MS = 5 * 60 * 1000;
var MAX_PREVIEWS = 1000;
var previews = Object.create(null);

function exact(value, fields) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === fields.slice().sort().join('\0');
}
function id() { return 'atp-' + crypto.randomBytes(16).toString('hex'); }
function prune() {
  Object.keys(previews).forEach(function (key) {
    if (Date.parse(previews[key].expiresAt) <= Date.now()) delete previews[key];
  });
}
function publicTask(item) {
  if (!item) return null;
  return {
    stem: item.stem,
    title: item.title || item.stem,
    column: item.column || item.state || null
  };
}
function findingSnapshot(finding) {
  return {
    id: finding.id,
    fingerprint: finding.fingerprint,
    type: finding.type,
    severity: finding.severity,
    title: finding.title,
    summary: finding.summary,
    affectedNodeIds: finding.affectedNodeIds,
    evidence: finding.evidence,
    ruleId: finding.ruleId
  };
}
function currentContext() {
  var snapshot = arch.readValidated();
  if (!snapshot.present) return { ok: false, status: 409, error: 'architecture-map-unavailable' };
  var tasks = taskSource.scanOpen();
  if (!tasks.revision || !tasks.ok || !tasks.historyOk) {
    return {
      ok: false, status: 409, error: tasks.revision
        ? 'architecture-task-index-partial' : 'architecture-task-index-unavailable'
    };
  }
  return { ok: true, snapshot: snapshot, tasks: tasks };
}
function preview(request) {
  prune();
  if (!exact(request, [
    'expectedStructuralHash', 'expectedTaskIndexRevision', 'findingId', 'fingerprint'
  ]) ||
      !contract.ID_RE.test(String(request.findingId || '')) ||
      !contract.HASH_RE.test(String(request.fingerprint || '')) ||
      !contract.HASH_RE.test(String(request.expectedStructuralHash || '')) ||
      !contract.HASH_RE.test(String(request.expectedTaskIndexRevision || ''))) {
    return { ok: false, status: 400, error: 'bad-architecture-task-preview-request' };
  }
  var context = currentContext();
  if (!context.ok) return context;
  if (context.snapshot.map.structuralHash !== request.expectedStructuralHash) {
    return { ok: false, status: 409, error: 'architecture-structural-conflict',
      currentStructuralHash: context.snapshot.map.structuralHash };
  }
  if (context.tasks.revision !== request.expectedTaskIndexRevision) {
    return { ok: false, status: 409, error: 'architecture-task-index-conflict',
      currentTaskIndexRevision: context.tasks.revision };
  }
  var finding = context.snapshot.map.findings.find(function (row) {
    return row.id === request.findingId && row.fingerprint === request.fingerprint;
  });
  if (!finding) return { ok: false, status: 409, error: 'architecture-finding-stale' };
  var existing = (context.tasks.allByRef[finding.id] || [])[0] || null;
  var reusable = Object.keys(previews).map(function (key) { return previews[key]; })
    .find(function (item) {
      return !item.inFlight &&
        item.structuralHash === context.snapshot.map.structuralHash &&
        item.taskIndexRevision === context.tasks.revision &&
        item.findingId === finding.id && item.fingerprint === finding.fingerprint;
    });
  if (!reusable && Object.keys(previews).length >= MAX_PREVIEWS) {
    return { ok: false, status: 503, error: 'architecture-task-preview-capacity' };
  }
  var previewId = reusable ? reusable.id : id();
  var now = new Date().toISOString();
  if (reusable) {
    // A repeated preview request is a fresh user intent. Reuse the bounded
    // capability id, but renew its full decision window so the subsequent
    // confirm action cannot expire immediately.
    reusable.createdAt = now;
    reusable.expiresAt = new Date(Date.parse(now) + TTL_MS).toISOString();
  } else {
    previews[previewId] = {
      id: previewId,
      createdAt: now,
      expiresAt: new Date(Date.parse(now) + TTL_MS).toISOString(),
      structuralHash: context.snapshot.map.structuralHash,
      taskIndexRevision: context.tasks.revision,
      findingId: finding.id,
      fingerprint: finding.fingerprint,
      inFlight: false
    };
  }
  return {
    ok: true, status: 200,
    structuralHash: context.snapshot.map.structuralHash,
    taskIndexRevision: context.tasks.revision,
    preview: {
      id: previewId,
      createdAt: now,
      expiresAt: previews[previewId].expiresAt,
      action: existing ? 'existing' : 'create',
      finding: findingSnapshot(finding),
      existingTask: publicTask(existing)
    }
  };
}
function plain(value, max) {
  var normalized = String(value == null ? '' : value).normalize('NFC')
    .replace(/[\x00-\x1f\x7f-\x9f\u2028\u2029]+/g, ' ')
    .replace(/\s+/g, ' ').trim();
  return Array.from(normalized).slice(0, max).join('');
}
function markdown(value, fallback, max) {
  return (plain(value, max) || fallback)
    .replace(/([\\`*_{}\[\]<>()#+.!|>~-])/g, '\\$1');
}
function taskTitle(finding) {
  return plain('Resolve architecture finding: ' + finding.title, 200);
}
function taskBody(finding, snapshot) {
  var nodeById = snapshot.nodeById;
  var affected = finding.affectedNodeIds.map(function (nodeId) {
    var node = nodeById[nodeId];
    return '- `' + nodeId.replace(/`/g, '') + '` — ' +
      markdown(node && node.name, 'Unknown architecture entity', 200);
  });
  var evidence = finding.evidence.map(function (row) {
    var location = row.sourcePath + (row.line ? ':' + row.line : '');
    return '- `' + location.replace(/`/g, '') + '` — ' +
      markdown(row.reasonCode, 'architecture-evidence', 100);
  });
  return [
    '## Goal',
    '',
    markdown(finding.title, 'Resolve architecture finding', 200) + '.',
    '',
    '## Architecture finding',
    '',
    '- Finding: `' + finding.id + '`',
    '- Type: `' + finding.type + '`',
    '- Severity: `' + finding.severity + '`',
    '- Rule: `' + finding.ruleId + '`',
    '- Structural hash: `' + snapshot.map.structuralHash + '`',
    '',
    markdown(finding.summary, 'Review the current architecture finding.', 1000),
    '',
    '## Affected entities',
    '',
    affected.join('\n'),
    '',
    '## Evidence',
    '',
    evidence.join('\n'),
    '',
    '## Acceptance criteria',
    '',
    '- The finding is no longer present after regenerating Architecture Map v2.',
    '- Architecture analysis remains complete, or every remaining limitation is explicitly documented.',
    '- Relevant architecture and regression tests pass.'
  ].join('\n');
}
function create(request) {
  prune();
  if (!exact(request, ['expectedStructuralHash', 'expectedTaskIndexRevision', 'previewId']) ||
      !PREVIEW_RE.test(String(request.previewId || '')) ||
      !contract.HASH_RE.test(String(request.expectedStructuralHash || '')) ||
      !contract.HASH_RE.test(String(request.expectedTaskIndexRevision || ''))) {
    return Promise.resolve({ ok: false, status: 400, error: 'bad-architecture-task-create-request' });
  }
  var held = previews[request.previewId];
  if (!held) return Promise.resolve({ ok: false, status: 409, error: 'architecture-task-preview-expired' });
  if (held.inFlight) return Promise.resolve({ ok: false, status: 409, error: 'architecture-task-preview-in-flight' });
  if (held.structuralHash !== request.expectedStructuralHash ||
      held.taskIndexRevision !== request.expectedTaskIndexRevision) {
    delete previews[held.id];
    return Promise.resolve({ ok: false, status: 409, error: 'architecture-task-preview-conflict' });
  }
  var context = currentContext();
  if (!context.ok) return Promise.resolve(context);
  if (context.snapshot.map.structuralHash !== held.structuralHash) {
    delete previews[held.id];
    return Promise.resolve({ ok: false, status: 409, error: 'architecture-structural-conflict',
      currentStructuralHash: context.snapshot.map.structuralHash });
  }
  if (context.tasks.revision !== held.taskIndexRevision) {
    delete previews[held.id];
    return Promise.resolve({ ok: false, status: 409, error: 'architecture-task-index-conflict',
      currentTaskIndexRevision: context.tasks.revision });
  }
  var finding = context.snapshot.map.findings.find(function (row) {
    return row.id === held.findingId && row.fingerprint === held.fingerprint;
  });
  if (!finding) {
    delete previews[held.id];
    return Promise.resolve({ ok: false, status: 409, error: 'architecture-finding-stale' });
  }
  var existing = (context.tasks.allByRef[finding.id] || [])[0] || null;
  if (existing) {
    delete previews[held.id];
    return Promise.resolve({
      ok: true, status: 200, result: 'existing',
      findingId: finding.id, task: publicTask(existing)
    });
  }
  held.inFlight = true;
  var source = {
    kind: 'manual',
    type: 'architecture-finding',
    ref: finding.id,
    fingerprint: finding.fingerprint
  };
  if (!taskSource.validate(source)) {
    delete previews[held.id];
    return Promise.resolve({ ok: false, status: 500, error: 'architecture-task-source-invalid' });
  }
  var keyHash = crypto.createHash('sha256')
    .update(held.id + '\0' + finding.id, 'utf8').digest('hex');
  var sourceHash = crypto.createHash('sha256').update(finding.id, 'utf8').digest('hex');
  return backlogCreate.create({
    title: taskTitle(finding),
    body: taskBody(finding, context.snapshot),
    source: source,
    idempotencyKey: 'architecture.create.' + keyHash,
    dedupKey: 'architecture.' + sourceHash,
    dedupReport: finding.fingerprint
  }).then(function (created) {
    delete previews[held.id];
    if (!created || typeof created !== 'object') {
      return { ok: false, status: 500, error: 'architecture-task-create-invalid-result' };
    }
    return {
      ok: true, status: 200,
      result: created.effect === 'domain-dedup' || created.deduped ? 'existing' : 'created',
      findingId: finding.id,
      stem: created.stem || null,
      task: publicTask(created.task) || (created.stem ? { stem: created.stem, title: taskTitle(finding), column: 'backlog' } : null)
    };
  }, function (error) {
    delete previews[held.id];
    try {
      process.stderr.write('[architecture] task creation failed: ' +
        String(error && error.code || 'create-failed').slice(0, 100) + '\n');
    } catch (_) {}
    return {
      ok: false,
      status: Number.isInteger(error && error.httpStatus) ? error.httpStatus : 500,
      error: backlogCreate.publicCreateErrorCode(error)
    };
  });
}
function cancel(request) {
  prune();
  if (!exact(request, ['previewId']) || !PREVIEW_RE.test(String(request.previewId || ''))) {
    return { ok: false, status: 400, error: 'bad-architecture-task-cancel-request' };
  }
  var held = previews[request.previewId];
  if (!held) return { ok: true, status: 200, cancelled: false };
  if (held.inFlight) return { ok: false, status: 409, error: 'architecture-task-preview-in-flight' };
  delete previews[request.previewId];
  return { ok: true, status: 200, cancelled: true };
}

module.exports = {
  PREVIEW_RE: PREVIEW_RE,
  preview: preview,
  create: create,
  cancel: cancel,
  _test: {
    taskTitle: taskTitle,
    taskBody: taskBody
  }
};
