'use strict';

// Server-resolved batch task flow. Browser input contains finding ids only;
// titles, Markdown, Source metadata and dedup receipts are resolved here from
// the current committed Design snapshot.

var crypto = require('crypto');
var path = require('path');
var catalog = require('./design-catalog');
var taskSource = require('./task-source');
var backlogCreate = require('./backlog-create');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var tokenBindingContract = require('../../tasks/token-binding-contract.cjs');
var componentBindingContract = require('../../tasks/component-binding-contract.cjs');

var PREVIEW_TTL = 5 * 60 * 1000;
var PREVIEW_RE = /^dtp-[a-f0-9]{32}$/;
var MAX_FINDINGS = 25;
var MAX_PREVIEWS = 1000;
var previews = Object.create(null);
var createTask = backlogCreate.create;
var TASK_RESULT_ERROR_CODES = Object.freeze({
  'task-create-invalid-result': true,
  'token-binding-context-unavailable': true,
  'token-binding-evidence-unavailable': true,
  'token-binding-invalid': true,
  'token-binding-row-unavailable': true,
  'token-binding-write-failed': true,
  'component-binding-adapter-unresolved': true,
  'component-binding-context-unavailable': true,
  'component-binding-invalid': true,
  'component-binding-platform-unresolved': true,
  'component-binding-relation-unsupported': true,
  'component-binding-row-unavailable': true,
  'component-binding-spec-unavailable': true,
  'component-binding-symbol-unresolved': true,
  'component-binding-write-failed': true
});

function taskResultError(error) {
  var code = error && error.code;
  return Object.prototype.hasOwnProperty.call(TASK_RESULT_ERROR_CODES, code)
    ? code : 'task-create-failed';
}

function exact(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}
function randomId() { return 'dtp-' + crypto.randomBytes(16).toString('hex'); }
function hash(value) {
  return 'sha256:' + crypto.createHash('sha256').update(JSON.stringify(value), 'utf8').digest('hex');
}
function prune() {
  Object.keys(previews).forEach(function (id) {
    if (Date.parse(previews[id].expiresAt) <= Date.now()) delete previews[id];
  });
}
function publicFinding(row) {
  return {
    findingId: row.id, kind: row.kind, entityId: row.entityId,
    entityType: row.entityType, entityName: row.entityName, severity: row.severity, title: row.title,
    detail: row.detail, existingTask: catalog.publicTask(row.openTask)
  };
}
function findingSetHash(rows) {
  return hash(rows.map(function (row) {
    return {
      id: row.id || row.findingId,
      sourceId: row.sourceId,
      sourceFingerprint: row.sourceFingerprint
    };
  }).sort(function (a, b) { return a.id.localeCompare(b.id); }));
}
function preview(request) {
  prune();
  if (!exact(request, ['findingIds', 'expectedGenerationRevision']) ||
      !Array.isArray(request.findingIds) || !request.findingIds.length ||
      request.findingIds.length > MAX_FINDINGS ||
      !taskSource.HASH_RE.test(String(request.expectedGenerationRevision || '')) ||
      request.findingIds.some(function (id) { return typeof id !== 'string' || !/^fnd-[a-f0-9]{24}$/.test(id); })) {
    return { ok: false, status: 400, error: 'bad-design-task-preview-request' };
  }
  var unique = request.findingIds.filter(function (id, index, list) { return list.indexOf(id) === index; });
  if (unique.length !== request.findingIds.length) return { ok: false, status: 400, error: 'duplicate-finding-id' };
  var snap = catalog.snapshot();
  if (!snap.ok) return snap;
  var conflict = catalog.checkRevision(snap, request.expectedGenerationRevision);
  if (conflict) return conflict;
  if (!snap.tasks.revision) return Object.assign(catalog.publicMeta(snap), {
    ok: false, status: 409, error: 'task-index-unavailable'
  });
  if (!snap.tasks.ok) return Object.assign(catalog.publicMeta(snap), {
    ok: false, status: 409, error: 'task-origin-index-partial', malformed: snap.tasks.malformed
  });
  var create = [], existing = [], skipped = [], currentRows = [];
  unique.forEach(function (id) {
    var row = snap.findingsById[id];
    if (!row) { skipped.push({ findingId: id, reason: 'finding-stale' }); return; }
    currentRows.push(row);
    var item = publicFinding(row);
    if (row.openTask) existing.push(item);
    else create.push(item);
  });
  var setHash = findingSetHash(currentRows);
  if (Object.keys(previews).length >= MAX_PREVIEWS) {
    return Object.assign(catalog.publicMeta(snap), {
      ok: false, status: 503, error: 'design-task-preview-capacity'
    });
  }
  var id = randomId(), now = new Date().toISOString();
  var held = previews[id] = {
    id: id, createdAt: now, expiresAt: new Date(Date.parse(now) + PREVIEW_TTL).toISOString(),
    generationId: snap.committedGenerationId, generationRevision: snap.generationRevision,
    taskIndexRevision: snap.tasks.revision, findingSetHash: setHash,
    findingIds: currentRows.map(function (row) { return row.id; }),
    selectedIds: unique.slice()
  };
  return Object.assign(catalog.publicMeta(snap), {
    ok: true, status: 200,
    preview: {
      id: held.id, createdAt: held.createdAt, expiresAt: held.expiresAt,
      findingSetHash: held.findingSetHash, taskIndexRevision: held.taskIndexRevision,
      counts: { create: create.length, existing: existing.length, skipped: skipped.length },
      create: create, existing: existing, skipped: skipped
    }
  });
}
function taskTitle(finding) {
  return plainLine(finding.title || 'Review design finding', 200, 512);
}
function plainLine(value, maxCharacters, maxBytes) {
  var out = String(value == null ? '' : value).normalize('NFC')
    .replace(/[\x00-\x1f\x7f-\x9f\u2028\u2029]+/g, ' ')
    .replace(/\s+/g, ' ').trim();
  var characters = Array.from(out).slice(0, maxCharacters);
  while (characters.length && Buffer.byteLength(characters.join(''), 'utf8') > maxBytes) characters.pop();
  return characters.join('');
}
function markdownText(value, fallback, maxCharacters) {
  var line = plainLine(value, maxCharacters || 1000, 4096) || fallback;
  // Provider-owned display text is prose only. Escaping every CommonMark/GFM
  // punctuation character prevents headings, links, HTML, lists and fenced
  // blocks from becoming task structure while preserving the visible text.
  return line.replace(/([\\`*_{}\[\]<>()#+.!|>~-])/g, '\\$1');
}
// Design-origin token tasks freeze a machine-readable binding BEFORE the task
// exists; the task's Source fingerprint then pins those exact bytes
// (REQ-TASK-001/-004). Every identity below comes from the immutable
// comparison artifacts, never from display text.
function buildTokenBinding(finding, row, context) {
  var source = tokenBindingContract.bindingSourceId(finding.sourceId);
  if (!source) return null;
  if (!context) return { error: 'token-binding-context-unavailable' };
  if (!finding.tokenBinding || finding.tokenBinding.sourceId !== source.sourceId ||
      finding.tokenBinding.observedTokenKey !== row.observedTokenKey ||
      finding.tokenBinding.contextKey !== row.contextKey) {
    return { error: 'token-binding-evidence-unavailable' };
  }
  var binding = JSON.parse(JSON.stringify(finding.tokenBinding));
  var contractError = tokenBindingContract.bindingError(binding);
  if (contractError) return { error: 'token-binding-invalid', detail: contractError };
  return { binding: binding };
}
function writeTokenBinding(binding) {
  var relative = tokenBindingContract.bindingRelativePath(binding.sourceId);
  var file = path.join(paths.PROJECT_ROOT, relative);
  var bytes = Buffer.from(JSON.stringify(binding, null, 2) + '\n');
  var result = fileGuards.atomicReplaceRegularFileResult(paths.PROJECT_ROOT, path.dirname(file), file, bytes,
    { create: true, directoryMode: 0o755, mode: 0o644, maxBytes: 256 * 1024 });
  if (!result.ok) return { error: 'token-binding-write-failed' };
  return { fingerprint: tokenBindingContract.sha256(bytes), relative: relative };
}
function foldName(value) {
  return String(value == null ? '' : value).trim().toLowerCase();
}
// Design-origin component tasks freeze a machine-readable binding BEFORE the
// task exists, exactly like the token flow. Every identity comes from the
// immutable comparison artifacts and the bounded design-inventory copies in
// the binding context — never from display strings, and nothing is guessed:
// an unresolvable adapter/platform/symbol is a typed error that keeps the
// finding in Mapping Review instead of inventing an implementation target.
function buildComponentBinding(finding, row, context) {
  var source = componentBindingContract.bindingSourceId(finding.sourceId);
  if (!source) return null;
  if (!context) return { error: 'component-binding-context-unavailable' };
  var intent = source.kind.slice('component-'.length);
  var frozen = context.byDesignComponentId[row.designComponentId];
  if (!frozen) return { error: 'component-binding-spec-unavailable' };
  var strong = (Array.isArray(row.suggestions) ? row.suggestions : []).filter(function (candidate) {
    return candidate.band === 'strong';
  });
  var mappedPlatforms = Array.isArray(row.platforms) ? row.platforms : [];
  var mappedAdapterIds = mappedPlatforms.map(function (platform) { return platform.adapterId; })
    .filter(function (value, index, list) { return value && list.indexOf(value) === index; });
  var adapterId = null;
  if (mappedAdapterIds.length === 1) adapterId = mappedAdapterIds[0];
  else if (!mappedAdapterIds.length && strong.length === 1) adapterId = strong[0].adapterId;
  if (!adapterId) adapterId = context.defaultAdapterId;
  if (!adapterId) return { error: 'component-binding-adapter-unresolved' };
  var platform = context.adapterPlatforms[adapterId] || null;
  if (!platform) return { error: 'component-binding-platform-unresolved' };
  var mappedImplementation = mappedPlatforms.find(function (candidate) { return candidate.adapterId === adapterId; }) || null;
  var relation = 'direct';
  if (mappedImplementation && mappedImplementation.relation && mappedImplementation.relation !== 'direct') {
    if (['wrapper', 'composite', 'shared-implementation'].indexOf(mappedImplementation.relation) < 0) {
      return { error: 'component-binding-relation-unsupported' };
    }
    relation = mappedImplementation.relation;
  }
  var targetProjectComponentId = null;
  var intendedProjectSymbol = null;
  if (intent === 'implement') {
    if (strong.length === 1 && strong[0].sourceSymbol) {
      targetProjectComponentId = strong[0].projectComponentId;
      intendedProjectSymbol = strong[0].sourceSymbol;
    } else {
      return { error: 'component-binding-symbol-unresolved' };
    }
  } else if (intent === 'update-api' || intent === 'update-visual' || intent === 'add-platform') {
    var refs = mappedImplementation ? mappedImplementation.projectRefs.filter(function (ref) { return ref.sourceSymbol; }) : [];
    if (refs.length !== 1) return { error: 'component-binding-symbol-unresolved' };
    targetProjectComponentId = refs[0].projectComponentId;
    intendedProjectSymbol = refs[0].sourceSymbol;
  }
  // Provable exact-name property pairs only: a design variant/boolean property
  // binds when exactly one project variant property carries the same folded
  // name, and a variant value map is emitted only when EVERY design option
  // resolves to exactly one folded-equal project value.
  var intendedPropertyMappings = [];
  var projectComponent = targetProjectComponentId ? context.projectComponentsById[targetProjectComponentId] : null;
  if (projectComponent && Array.isArray(projectComponent.variantProperties)) {
    frozen.frozenSpec.properties.forEach(function (property) {
      if (property.type !== 'variant' && property.type !== 'boolean') return;
      var matches = projectComponent.variantProperties.filter(function (candidate) {
        return candidate.name && foldName(candidate.name) === foldName(property.name);
      });
      if (matches.length !== 1) return;
      var pair = { designPropertyId: property.propertyId, projectPropertyId: matches[0].projectPropertyId };
      if (property.type === 'variant') {
        if (!Array.isArray(property.options) || !property.options.length) return;
        var valueMap = {};
        var resolved = property.options.every(function (option) {
          var hits = matches[0].values.filter(function (value) { return foldName(value) === foldName(option); });
          if (hits.length !== 1) return false;
          valueMap[option] = hits[0];
          return true;
        });
        if (!resolved) return;
        pair.valueMap = valueMap;
      }
      intendedPropertyMappings.push(pair);
    });
  }
  var binding = {
    schemaVersion: 2,
    sourceId: source.sourceId,
    intent: intent,
    designComponentId: row.designComponentId,
    designScopeId: context.designScopeId,
    designGenerationId: context.designGenerationId,
    designInventoryHash: context.designInventoryHash,
    expectedKind: finding.componentBinding.kind,
    frozenStructuralHash: frozen.structuralHash,
    frozenSourceHash: frozen.sourceHash,
    frozenSpec: frozen.frozenSpec,
    intendedAdapterId: adapterId,
    intendedPlatform: platform,
    intendedRelation: relation,
    findingId: finding.componentBinding.comparatorFindingId,
    comparisonSemanticHash: context.comparisonSemanticHash,
    mappingRevision: context.mappingRevision
  };
  if (intendedProjectSymbol) binding.intendedProjectSymbol = intendedProjectSymbol;
  if (intendedPropertyMappings.length) binding.intendedPropertyMappings = intendedPropertyMappings;
  if (row.mappingId) binding.mappingId = row.mappingId;
  var contractError = componentBindingContract.bindingError(binding);
  if (contractError) return { error: 'component-binding-invalid', detail: contractError };
  return { binding: binding, nodeId: frozen.nodeId, name: frozen.name };
}
function writeComponentBinding(binding) {
  var relative = componentBindingContract.bindingRelativePath(binding.sourceId);
  var file = path.join(paths.PROJECT_ROOT, relative);
  var bytes = Buffer.from(JSON.stringify(binding, null, 2) + '\n');
  var result = fileGuards.atomicReplaceRegularFileResult(paths.PROJECT_ROOT, path.dirname(file), file, bytes,
    { create: true, directoryMode: 0o755, mode: 0o644, maxBytes: 1024 * 1024 });
  if (!result.ok) return { error: 'component-binding-write-failed' };
  return { fingerprint: componentBindingContract.sha256(bytes), relative: relative };
}
function taskBody(finding, bindingPath) {
  var inputs = [
    '- Entity type: `' + finding.entityType + '`',
    '- Entity ID: `' + finding.entityId + '`',
    '- Finding ID: `' + finding.id + '`',
    '- Sync job ID: `' + (finding.syncJobId || 'not-recorded') + '`',
    '- Current evidence: ' + markdownText(
      finding.detail, 'See Project → Design.', 1000
    )
  ];
  if (bindingPath) {
    inputs.push('- Token binding evidence: `' + bindingPath + '` (machine-readable authority; the Markdown here is descriptive only)');
  }
  var acceptance = bindingPath ? [
    '- The bound design token is implemented exactly as the frozen binding evidence describes (kind, semantic path, per-mode values).',
    '- Finalization publishes the authorized mapping together with the task; an ambiguous or stale binding blocks completion instead of guessing.',
    '- The next local token comparison reflects the change in Project → Design.',
    '- Existing supported variants are preserved; uncaptured theme or locale evidence is never fabricated.'
  ] : [
    '- The finding is resolved against the current committed design generation.',
    '- Relevant local design checks pass and the result is reflected in Project → Design.',
    '- Any unavailable evidence remains explicitly marked Not captured or Not checked.',
    '- Existing supported variants are preserved; uncaptured theme or locale evidence is never fabricated.'
  ];
  return [
    '## Goal',
    '',
    markdownText(finding.title, 'Resolve the current design finding', 200) + '.',
    '',
    '## Inputs',
    ''
  ].concat(inputs).concat([
    '',
    '## Design',
    '',
    bindingPath
      ? '- Tokens — none (design-token task; the binding evidence above is the design source, no screen pull applies)'
      : '- Design finding — none (server-resolved finding; no screen pull applies)',
    '',
    '## Acceptance',
    ''
  ]).concat(acceptance).join('\n');
}
// Component task body. The binding evidence file is the machine authority;
// the bulletless snapshot block in ## Inputs keeps the task on the pixel
// track (design-parser hasComponentSnapshot anchors on `designComponentId:`
// plus `figmaNodeId:`/`frozenStructuralHash:` lines, exactly this grammar).
function componentTaskBody(finding, bindingPath, built) {
  var name = markdownText(built.name || finding.entityName, 'Component', 300);
  return [
    '## Goal',
    '',
    markdownText(finding.title, 'Resolve the current design finding', 200) + '.',
    '',
    '## Inputs',
    '',
    '- Entity type: `' + finding.entityType + '`',
    '- Entity ID: `' + finding.entityId + '`',
    '- Finding ID: `' + finding.id + '`',
    '- Sync job ID: `' + (finding.syncJobId || 'not-recorded') + '`',
    '- Current evidence: ' + markdownText(
      finding.detail, 'See Project → Design.', 1000
    ),
    '- Component binding evidence: `' + bindingPath + '` (machine-readable authority; the Markdown here is descriptive only)',
    '',
    'designComponentId: ' + built.binding.designComponentId,
    'figmaNodeId: ' + built.nodeId,
    'frozenStructuralHash: ' + built.binding.frozenStructuralHash,
    '',
    '## Design',
    '',
    '- ' + name + ' — none (component task; binding evidence is the design source)',
    '',
    '## Acceptance',
    '',
    '- The bound design component is implemented exactly as the frozen binding evidence describes (kind, properties, allowed variants, slots).',
    '- Finalization publishes the authorized mapping together with the task; an ambiguous or stale binding blocks completion instead of guessing.',
    '- The next local component comparison reflects the change in Project → Design.',
    '- Existing supported variants are preserved; uncaptured theme or locale evidence is never fabricated.'
  ].join('\n');
}
function createOne(finding, previewId, snap) {
  var source = {
    kind: 'figma', type: finding.sourceKind, ref: finding.sourceId,
    fingerprint: finding.sourceFingerprint
  };
  var body = taskBody(finding);
  if (tokenBindingContract.bindingSourceId(finding.sourceId)) {
    var row = snap && snap.byId[finding.entityId];
    var context = snap && snap.tokens && snap.tokens.bindingContext;
    if (!row || !finding.tokenBinding) {
      throw Object.assign(new Error('token-binding-row-unavailable'), { code: 'token-binding-row-unavailable' });
    }
    var built = buildTokenBinding(finding, row, context);
    if (!built || built.error) {
      throw Object.assign(new Error(built && built.error || 'token-binding-invalid'), { code: built && built.error || 'token-binding-invalid' });
    }
    var written = writeTokenBinding(built.binding);
    if (written.error) {
      throw Object.assign(new Error(written.error), { code: written.error });
    }
    source.fingerprint = written.fingerprint;
    body = taskBody(finding, written.relative);
  } else if (componentBindingContract.bindingSourceId(finding.sourceId)) {
    var componentRow = snap && snap.byId[finding.entityId];
    var componentContext = snap && snap.components && snap.components.bindingContext;
    if (!componentRow || !finding.componentBinding) {
      throw Object.assign(new Error('component-binding-row-unavailable'), { code: 'component-binding-row-unavailable' });
    }
    var componentBuilt = buildComponentBinding(finding, componentRow, componentContext);
    if (!componentBuilt || componentBuilt.error) {
      throw Object.assign(new Error(componentBuilt && componentBuilt.error || 'component-binding-invalid'),
        { code: componentBuilt && componentBuilt.error || 'component-binding-invalid' });
    }
    var componentWritten = writeComponentBinding(componentBuilt.binding);
    if (componentWritten.error) {
      throw Object.assign(new Error(componentWritten.error), { code: componentWritten.error });
    }
    source.fingerprint = componentWritten.fingerprint;
    body = componentTaskBody(finding, componentWritten.relative, componentBuilt);
  }
  var keyHash = crypto.createHash('sha256').update(previewId + '\0' + finding.id, 'utf8').digest('hex');
  var sourceHash = crypto.createHash('sha256').update(finding.sourceId, 'utf8').digest('hex');
  return createTask({
    title: taskTitle(finding),
    body: body,
    source: source,
    idempotencyKey: 'design.create.' + keyHash,
    dedupKey: 'design.' + sourceHash,
    dedupReport: source.fingerprint
  });
}
function create(request) {
  if (!exact(request, ['previewId', 'expectedFindingSetHash', 'expectedTaskIndexRevision']) ||
      !PREVIEW_RE.test(String(request.previewId || '')) ||
      !taskSource.HASH_RE.test(String(request.expectedFindingSetHash || '')) ||
      !taskSource.HASH_RE.test(String(request.expectedTaskIndexRevision || ''))) {
    return Promise.resolve({ ok: false, status: 400, error: 'bad-design-task-create-request' });
  }
  prune();
  var held = previews[request.previewId];
  if (!held) return Promise.resolve({ ok: false, status: 409, error: 'design-task-preview-expired' });
  if (held.inFlight) return Promise.resolve({ ok: false, status: 409, error: 'design-task-preview-in-flight' });
  if (held.findingSetHash !== request.expectedFindingSetHash) {
    return Promise.resolve({ ok: false, status: 409, error: 'finding-set-conflict' });
  }
  if (held.taskIndexRevision !== request.expectedTaskIndexRevision) {
    return Promise.resolve({ ok: false, status: 409, error: 'task-conflict' });
  }
  var snap = catalog.snapshot();
  if (!snap.ok) return Promise.resolve(snap);
  if (snap.generationRevision !== held.generationRevision ||
      snap.committedGenerationId !== held.generationId) {
    delete previews[held.id];
    return Promise.resolve(Object.assign(catalog.publicMeta(snap), {
      ok: false, status: 409, error: 'design-generation-conflict'
    }));
  }
  if (!snap.tasks.ok || snap.tasks.revision !== held.taskIndexRevision) {
    delete previews[held.id];
    return Promise.resolve(Object.assign(catalog.publicMeta(snap), {
      ok: false, status: 409, error: 'task-conflict',
      currentTaskIndexRevision: snap.tasks.revision
    }));
  }
  var current = held.findingIds.map(function (id) { return snap.findingsById[id] || null; }).filter(Boolean);
  if (findingSetHash(current) !== held.findingSetHash) {
    delete previews[held.id];
    return Promise.resolve(Object.assign(catalog.publicMeta(snap), {
      ok: false, status: 409, error: 'finding-stale'
    }));
  }
  var result = { created: [], existing: [], skipped: [], failed: [] };
  held.selectedIds.forEach(function (id) {
    if (!snap.findingsById[id]) result.skipped.push({ findingId: id, reason: 'finding-stale' });
  });
  held.inFlight = true;
  return current.reduce(function (promise, finding) {
    return promise.then(function () {
      var open = snap.tasks.byRef[finding.sourceId] || [];
      if (open.length) {
        result.existing.push({ findingId: finding.id, task: catalog.publicTask(open[0]) });
        return;
      }
      return Promise.resolve().then(function () {
        return createOne(finding, held.id, snap);
      }).then(function (created) {
        if (!created || typeof created !== 'object') {
          throw Object.assign(new Error('task-create-invalid-result'), { code: 'task-create-invalid-result' });
        }
        if (created.effect === 'domain-dedup' || created.deduped) {
          result.existing.push({
            findingId: finding.id, task: catalog.publicTask(created.task), stem: created.stem
          });
        } else {
          result.created.push({
            findingId: finding.id, task: catalog.publicTask(created.task), stem: created.stem
          });
        }
      }).catch(function (error) {
        result.failed.push({
          findingId: finding.id,
          error: taskResultError(error)
        });
      });
    });
  }, Promise.resolve()).then(function () {
    delete previews[held.id];
    return Object.assign(catalog.publicMeta(snap), {
      ok: true,
      partial: result.failed.length > 0,
      status: result.failed.length ? 207 : 200,
      result: result,
      counts: {
        created: result.created.length, existing: result.existing.length,
        skipped: result.skipped.length, failed: result.failed.length
      }
    });
  }, function (error) {
    delete previews[held.id];
    return Object.assign(catalog.publicMeta(snap), {
      ok: false, status: 500,
      error: 'design-task-create-failed'
    });
  });
}
function cancel(request) {
  prune();
  if (!exact(request, ['previewId']) || !PREVIEW_RE.test(String(request.previewId || ''))) {
    return { ok: false, status: 400, error: 'bad-design-task-cancel-request' };
  }
  var held = previews[request.previewId];
  if (!held) return { ok: true, status: 200, cancelled: false };
  if (held.inFlight) return { ok: false, status: 409, error: 'design-task-preview-in-flight' };
  delete previews[request.previewId];
  return { ok: true, status: 200, cancelled: true };
}

module.exports = {
  MAX_FINDINGS: MAX_FINDINGS,
  preview: preview,
  create: create,
  cancel: cancel,
  _test: {
    taskTitle: taskTitle,
    taskBody: taskBody,
    componentTaskBody: componentTaskBody
  }
};
