'use strict';

// Bounded Task List read model. Canonical task artifacts are the availability
// authority; INDEX supplies ordering hints only. Ephemeral signals stay in this
// server projection rather than INDEX.

var crypto = require('crypto');
var fs = require('fs');
var paths = require('./paths');
var stateMod = require('./state');
var appRunValidation = require('./app-run-validation');
var taskIntegrity = require('./task-integrity');
var taskRequirement = require('./task-requirement');
var taskBlockers = require('./task-blockers');
var primaryAction = require('./task-primary-action');
var taskCheckpoints = require('./task-checkpoints');
var tasksLog = require('./tasks-log');
var taskTestCertification = require('./task-test-certification');
var designCatalog = require('./design-catalog');
var apiReportState = require('./api-report-state');
var core = require('../../tasks/task-state-core.cjs');
var taskIndexSource = require('./task-source');

var COLUMNS = Object.freeze(['backlog', 'pending', 'todo', 'done']);
var PUBLIC_ERROR_CODES = Object.freeze({
  'bad-task-summary-filter': true,
  'task-summary-cursor-invalid': true,
  'task-summary-cursor-stale': true,
  'task-summary-stem-invalid': true,
  'task-index-invalid': true,
  'task-index-not-fresh': true,
  'task-state-unavailable': true,
  'task-state-invalid': true
});
var DEFAULT_LIMIT = 100;
var MAX_LIMIT = 500;
var USER_ACTIONS = Object.freeze({
  'submit-answers': 1, 'continue-live': 1, 'review-result': 1,
  'validate-in-app': 1, 'resolve-blocker': 1
});
function publicErrorCode(error) {
  var code = error && error.code;
  return Object.prototype.hasOwnProperty.call(PUBLIC_ERROR_CODES, code)
    ? code : 'task-summary-unavailable';
}
function hash(value) {
  return 'sha256:' + crypto.createHash('sha256')
    .update(typeof value === 'string' ? value : primaryAction.canonical(value), 'utf8').digest('hex');
}

function readIndex() {
  var current = taskIndexSource.readIndex();
  if (!current) throw Object.assign(new Error('task index is unavailable or invalid'), { code: 'task-index-invalid' });
  return current;
}

function canonicalByStem(validation, generatedAt) {
  var out = Object.create(null);
  if (!validation || !validation._model) {
    throw Object.assign(new Error('canonical task state is unavailable'), { code: 'task-state-unavailable' });
  }
  var model = validation._model;
  var index = core.deriveIndex(model, generatedAt);
  COLUMNS.forEach(function (column) {
    index[column].forEach(function (row) {
      out[row.stem] = canonicalRow(row);
    });
  });
  // deriveIndex intentionally excludes unreadable and structurally corrupt
  // artifacts because it is a strict publication format. The board is a
  // recovery surface, so project one bounded diagnostic row for every exact
  // canonical stem that still owns task files.
  model.artifacts.forEach(function (group, stem) {
    if (out[stem] || !group || !core.STEM_RE.test(String(stem || ''))) return;
    var state = group.state || core.classify(group);
    if (state === 'absent') return;
    var displayColumn = COLUMNS.indexOf(state) >= 0 ? state :
      COLUMNS.slice().reverse().find(function (column) { return !!group[column]; });
    if (!displayColumn) return;
    var sourceRecord = state === 'pending' && group.backlog
      ? group.backlog : group[displayColumn];
    var meta = model.metadata && model.metadata.get(stem);
    var mtimeMs = sourceRecord && sourceRecord.stat && sourceRecord.stat.mtimeMs;
    var createdAt = Number.isFinite(mtimeMs) ? new Date(mtimeMs).toISOString() : null;
    var fallback = {
      stem: stem,
      title: meta && typeof meta.title === 'string' && meta.title.trim() ? meta.title : stem,
      state: state === 'corrupt' ? 'corrupt' : displayColumn,
      sourceRevision: group.revision,
      createdAt: createdAt,
      doneAt: displayColumn === 'done' ? createdAt : null,
      origin: meta && meta.taskSource && meta.taskSource.valid ? meta.origin : null,
      dependsOn: meta && Array.isArray(meta.deps) ? meta.deps.slice() : [],
      splitFrom: meta && Array.isArray(meta.lineage) ? meta.lineage[0] || null : null,
      outcomeStatus: meta && meta.outcome ? meta.outcome.status : null,
      questionsCount: null,
      round: null,
      _displayColumn: displayColumn
    };
    out[stem] = canonicalRow(fallback);
  });
  return out;
}

function resilientRows(index, canonical) {
  var seen = Object.create(null), columns = { backlog: [], pending: [], todo: [], done: [] };
  COLUMNS.forEach(function (column) {
    var indexedRows = index && Array.isArray(index[column]) ? index[column] : [];
    indexedRows.forEach(function (indexed) {
      if (!indexed || typeof indexed.stem !== 'string' || seen[indexed.stem]) return;
      var fresh = canonical[indexed.stem];
      if (!fresh || fresh.state !== column) return;
      seen[indexed.stem] = true;
      columns[column].push(canonicalRow(fresh));
    });
  });
  Object.keys(canonical).filter(function (stem) {
    var row = canonical[stem];
    var column = row && (COLUMNS.indexOf(row.state) >= 0 ? row.state : row._displayColumn);
    return !seen[stem] && row && COLUMNS.indexOf(column) >= 0;
  }).sort(function (left, right) {
    var leftId = core.safeIntegerId(left);
    var rightId = core.safeIntegerId(right);
    if (leftId !== null && rightId !== null && leftId !== rightId) return leftId - rightId;
    return left.localeCompare(right);
  }).forEach(function (stem) {
    var row = canonicalRow(canonical[stem]);
    var column = COLUMNS.indexOf(row.state) >= 0 ? row.state : row._displayColumn;
    columns[column].push(row);
  });
  return columns;
}

function canonicalRow(fresh) {
  if (!fresh || !core.STEM_RE.test(String(fresh.stem || '')) ||
      COLUMNS.indexOf(fresh.state) < 0 && fresh.state !== 'corrupt') {
    throw Object.assign(new Error('canonical task identity is unavailable'), { code: 'task-state-invalid' });
  }
  var projected = {
    stem: fresh.stem,
    title: typeof fresh.title === 'string' && fresh.title.trim() ? fresh.title : fresh.stem,
    state: fresh.state,
    sourceRevision: fresh.sourceRevision,
    createdAt: fresh.createdAt,
    doneAt: fresh.doneAt,
    origin: fresh.origin,
    dependsOn: Array.isArray(fresh.dependsOn) ? fresh.dependsOn.slice() : fresh.dependsOn,
    splitFrom: fresh.splitFrom,
    outcomeStatus: fresh.outcomeStatus,
    questionsCount: fresh.questionsCount,
    round: fresh.round
  };
  var displayColumn = COLUMNS.indexOf(fresh._displayColumn) >= 0
    ? fresh._displayColumn : COLUMNS.indexOf(fresh.state) >= 0 ? fresh.state : null;
  Object.defineProperty(projected, '_displayColumn', {
    value: displayColumn,
    enumerable: false
  });
  return projected;
}

function rowsMap(columns) {
  var out = Object.create(null);
  COLUMNS.forEach(function (column) { columns[column].forEach(function (row) { out[row.stem] = row; }); });
  return out;
}

function dependencyProjection(row, allRows, blockingStems) {
  var items = (row.dependsOn || []).map(function (stem) {
    var target = allRows[stem];
    var accepted = !!target && target.state === 'done' && !blockingStems[stem] &&
      ['completed', 'completed-with-caveats'].indexOf(target.outcomeStatus) >= 0;
    return {
      stem: stem,
      title: target ? target.title : stem,
      state: target ? target.state : null,
      satisfied: accepted,
      missing: !target
    };
  });
  return {
    count: items.length,
    blockedCount: row.state === 'done' ? 0 : items.filter(function (item) { return !item.satisfied; }).length,
    satisfiedCount: items.filter(function (item) { return item.satisfied; }).length,
    items: items,
    blocksCount: Object.keys(allRows).filter(function (candidateStem) {
      var candidate = allRows[candidateStem];
      return candidate && Array.isArray(candidate.dependsOn) &&
        candidate.dependsOn.indexOf(row.stem) >= 0;
    }).length
  };
}

function byStem(list) {
  var out = Object.create(null);
  (Array.isArray(list) ? list : []).forEach(function (row) {
    if (row && typeof row.stem === 'string' && !out[row.stem]) out[row.stem] = row;
  });
  return out;
}

function readSourceAvailability() {
  var design = Object.create(null), coverage = Object.create(null), drift = Object.create(null);
  try {
    var designSnapshot = designCatalog.snapshot();
    if (designSnapshot && designSnapshot.ok && designSnapshot.byId) {
      Object.keys(designSnapshot.byId).slice(0, 10000).forEach(function (id) { design[id] = true; });
    }
  } catch (_) { /* Missing/corrupt optional source data means no proven target. */ }
  try {
    var coveragePlan = apiReportState.readCoverage();
    if (coveragePlan && coveragePlan.present && Array.isArray(coveragePlan.suggestions)) {
      coveragePlan.suggestions.slice(0, 10000).forEach(function (row) {
        if (row && typeof row.operationId === 'string') coverage[row.operationId] = true;
      });
    }
  } catch (_) { /* Keep the target unavailable. */ }
  try {
    var driftReport = apiReportState.readDrift();
    if (driftReport && driftReport.present && typeof driftReport.specHash === 'string' && typeof driftReport.checkedAt === 'string') {
      drift['drift:' + crypto.createHash('sha256').update(driftReport.specHash + '\0' + driftReport.checkedAt, 'utf8').digest('hex')] = true;
    }
  } catch (_) { /* Keep the target unavailable. */ }
  return { design: design, coverage: coverage, drift: drift };
}

function sourceTarget(origin, allRows, availability) {
  if (!origin) return null;
  availability = availability || { design: {}, coverage: {}, drift: {} };
  if (origin.kind === 'figma') {
    var designRef = /^design:(?:component|surface|token):((?:cmp|srf|tok)-[a-f0-9]{24}):/.exec(origin.ref);
    return {
      panel: 'design', entityId: designRef ? designRef[1] : origin.ref,
      availability: designRef && availability.design && availability.design[designRef[1]] ? 'available' : 'missing'
    };
  }
  if (origin.kind === 'api' && origin.type === 'api-work-package') return {
    panel: 'api',
    entityId: origin.ref,
    availability: 'available'
  };
  if (origin.kind === 'api') return {
    panel: 'api',
    entityId: origin.type === 'api-mismatch' ? origin.ref : 'coverage:' + origin.ref,
    availability: origin.type === 'api-mismatch'
      ? availability.drift && availability.drift[origin.ref] ? 'available' : 'missing'
      : availability.coverage && availability.coverage[origin.ref] ? 'available' : 'missing'
  };
  if (origin.kind === 'follow-up') return {
    panel: 'board', entityId: origin.ref,
    availability: allRows[origin.ref] ? 'available' : 'missing'
  };
  if (origin.kind === 'manual' && origin.type === 'architecture-finding') {
    // The current Architecture panel has no committed finding registry. Keep
    // the provenance visible but do not fabricate a deep link until a domain
    // producer can prove that the referenced finding still exists.
    return { panel: 'archmap', entityId: origin.ref, availability: 'missing' };
  }
  return null;
}

function lastActivity(row, runtime, latest) {
  if (latest && latest.event) {
    return {
      kind: latest.event.kind,
      labelKey: 'board.activity.' + latest.event.kind.replace(/-/g, '_'),
      occurredAt: latest.event.ts,
      phase: latest.event.phase || null,
      status: latest.event.status || null,
      source: 'journal'
    };
  }
  var candidates = [];
  if (runtime.request && runtime.request.createdAt) candidates.push({ kind: 'queued', at: runtime.request.createdAt, status: 'queued' });
  if (runtime.session && runtime.session.startedAt) candidates.push({ kind: 'run-started', at: runtime.session.startedAt, status: runtime.session.running ? 'running' : 'stopped' });
  if (runtime.session && runtime.session.endedAt) candidates.push({ kind: 'run-stopped', at: runtime.session.endedAt, status: runtime.session.exitCode === 0 ? 'ok' : 'fail' });
  if (runtime.finalization && runtime.finalization.updatedAt) candidates.push({ kind: 'finalization', at: runtime.finalization.updatedAt, status: runtime.finalization.status });
  candidates = candidates.filter(function (item) { return Number.isFinite(Date.parse(item.at)); }).sort(function (a, b) { return Date.parse(b.at) - Date.parse(a.at); });
  if (candidates.length) return {
    kind: candidates[0].kind,
    labelKey: 'board.activity.' + candidates[0].kind.replace(/-/g, '_'),
    occurredAt: candidates[0].at,
    phase: runtime.finalization && candidates[0].kind === 'finalization' ? runtime.finalization.phase || null : null,
    status: candidates[0].status,
    source: 'runtime'
  };
  var at = row.state === 'done' && row.doneAt ? row.doneAt : row.createdAt;
  return {
    kind: row.state === 'done' ? 'completed' : 'created',
    labelKey: row.state === 'done' ? 'board.activity.completed' : 'board.activity.created',
    occurredAt: at,
    phase: null,
    status: row.state,
    source: 'task-metadata'
  };
}

function retryCheckpoint(latest, row) {
  var event = latest && latest.event;
  if (!event || !event.meta || !event.meta.checkpointId) {
    return { candidate: null, recovery: null };
  }
  if (event.kind !== 'stop' && event.kind !== 'retry' && event.status !== 'fail') {
    return { candidate: null, recovery: null };
  }
  var status;
  try { status = taskCheckpoints.retryStatus(row.stem, event.meta.checkpointId); }
  catch (_) { status = null; }
  if (!status) return { candidate: null, recovery: null };
  var candidate = status.candidate;
  return {
    candidate: candidate ? {
      id: candidate.id,
      hash: candidate.hash,
      phase: candidate.phase,
      safePhase: candidate.safePhase,
      retryPolicy: candidate.retryPolicy
    } : null,
    recovery: candidate ? null : status.projection
  };
}

function compactSignals(row, screens) {
  var out = [];
  if (screens && screens.pulled) out.push({ kind: 'figma-screens', status: 'ready', count: Number.isSafeInteger(screens.count) ? screens.count : null });
  if (row.state === 'pending' && Number.isSafeInteger(row.questionsCount)) out.push({ kind: 'questions', status: 'pending', count: row.questionsCount });
  if (row.state === 'done' && row.outcomeStatus) out.push({ kind: 'outcome', status: row.outcomeStatus, count: null });
  return out.slice(0, 3);
}

function workerAttached(snapshot) {
  if (snapshot.runnerActive) return true;
  var at = snapshot.status && snapshot.status.worker && snapshot.status.worker.heartbeatAt;
  return !!at && Number.isFinite(Date.parse(at)) && Date.now() - Date.parse(at) < 90000;
}

// Authoritative default source for manual app-validation receipts. Reads the
// bounded app-run history through its validating reader; the directory stat
// key keeps repeated summary builds cheap because receipt files are immutable
// once published (writes and prunes both bump the directory).
var appValidationReceiptsCache = { key: null, value: null };
function readAppValidationReceipts() {
  var statKey;
  try {
    var stat = fs.statSync(paths.APP_RUN_HISTORY_DIR, { bigint: true });
    statKey = String(stat.dev) + ':' + String(stat.ino) + ':' + String(stat.mtimeNs) + ':' + String(stat.size);
  } catch (error) {
    if (error && error.code === 'ENOENT') return [];
    // Unreadable receipt authority must surface as an explicit limitation,
    // never as "no receipts".
    return { unavailable: true };
  }
  if (appValidationReceiptsCache.key === statKey) return appValidationReceiptsCache.value;
  var value;
  try {
    value = appRunValidation.history(null, 100);
  } catch (historyError) {
    return { unavailable: true };
  }
  appValidationReceiptsCache = { key: statKey, value: value };
  return value;
}

function normalizeValidationReceipts(value) {
  if (value == null) return { receipts: [], limitation: null };
  if (!Array.isArray(value) || value.length > 1000) {
    return { receipts: [], limitation: 'app-validation-receipts-invalid' };
  }
  var invalid = false;
  var receipts = value.filter(function (receipt) {
    var valid = receipt && typeof receipt === 'object' && !Array.isArray(receipt) &&
      typeof receipt.receiptId === 'string' && receipt.receiptId.length > 0 && receipt.receiptId.length <= 160 &&
      core.safeIntegerId(receipt.taskStem) !== null &&
      typeof receipt.taskSourceRevision === 'string' && /^sha256:[a-f0-9]{64}$/.test(receipt.taskSourceRevision) &&
      typeof receipt.staleTask === 'boolean' && typeof receipt.staleSource === 'boolean' &&
      ['passed', 'failed', 'partial'].indexOf(receipt.overall) >= 0 &&
      typeof receipt.createdAt === 'string' && Number.isFinite(Date.parse(receipt.createdAt));
    if (!valid) invalid = true;
    return valid;
  });
  return { receipts: receipts, limitation: invalid ? 'app-validation-receipts-invalid' : null };
}

function currentValidationReceipt(receipts, row, taskBodyRevision) {
  // Receipts pin the sha256 of the canonical task BODY file (the same bytes
  // the checklist was parsed from). row.sourceRevision is the state-scoped
  // task revision — a different hash domain — so matching requires the body
  // content hash from the canonical model. No proven body hash → no receipt
  // can be current (fail closed).
  if (typeof taskBodyRevision !== 'string' ||
      !/^sha256:[a-f0-9]{64}$/.test(taskBodyRevision)) return null;
  if (!Array.isArray(receipts) || receipts.length > 1000) return null;
  var matches = receipts.filter(function (receipt) {
    return receipt && typeof receipt === 'object' && !Array.isArray(receipt) &&
      receipt.taskStem === row.stem && receipt.taskSourceRevision === taskBodyRevision &&
      receipt.staleTask === false && receipt.staleSource === false &&
      ['passed', 'failed', 'partial'].indexOf(receipt.overall) >= 0 &&
      typeof receipt.receiptId === 'string' && receipt.receiptId.length > 0 && receipt.receiptId.length <= 160 &&
      typeof receipt.createdAt === 'string' && Number.isFinite(Date.parse(receipt.createdAt));
  });
  matches.sort(function (left, right) { return Date.parse(right.createdAt) - Date.parse(left.createdAt); });
  return matches[0] || null;
}

// Pending keeps its body in the backlog source; the pending artifact is the
// questions sidecar. Every other state owns exactly one body file.
function canonicalBodyRecord(validation, row) {
  var artifacts = validation && validation._model && validation._model.artifacts;
  var group = artifacts && typeof artifacts.get === 'function' ? artifacts.get(row.stem) : null;
  return group ? (row.state === 'pending' ? group.backlog : group[row.state]) : null;
}

function canonicalBodyText(validation, row) {
  var record = canonicalBodyRecord(validation, row);
  return record && record.readable && typeof record.text === 'string' ? record.text : '';
}

function canonicalBodyRevision(validation, row) {
  var record = canonicalBodyRecord(validation, row);
  return record && typeof record.contentHash === 'string' ? record.contentHash : null;
}

function projectRows(stableColumns, validation, snapshot, latestReader, validationReceipts, sourceAvailability) {
  var allRows = rowsMap(stableColumns);
  var blockingStems = Object.create(null);
  var findings = Array.isArray(validation && validation.findings) ? validation.findings : [];
  var globalActionFindings = [];
  var findingsByStem = Object.create(null);
  findings.forEach(function (item) {
    if (!item) return;
    var detailedStems = item.details && Array.isArray(item.details.stems)
      ? item.details.stems : [];
    if (item.stem == null && detailedStems.length === 0 &&
        !core.actionAdmission({ findings: [item] }).ok) {
      globalActionFindings.push(item);
    }
    var affected = [];
    if (item.stem) affected.push(item.stem);
    affected = affected.concat(detailedStems);
    Array.from(new Set(affected)).forEach(function (stem) {
      if (!core.STEM_RE.test(String(stem || ''))) return;
      findingsByStem[stem] = findingsByStem[stem] || [];
      findingsByStem[stem].push(item);
      if (item.severity === 'error' || item.severity === 'blocker') blockingStems[stem] = true;
    });
  });
  var locks = byStem(snapshot.progress && snapshot.progress.inProgress);
  var finalizations = byStem(snapshot.progress && snapshot.progress.finalizations);
  var requests = snapshot.progress && Array.isArray(snapshot.progress.requests) ? snapshot.progress.requests : [];
  var requestsByStem = byStem(requests);
  var sessions = snapshot.sessions || {};
  var screensMap = snapshot.screensCache || {};
  var attached = workerAttached(snapshot);
  var columns = { backlog: [], pending: [], todo: [], done: [] };
  var limitations = Object.create(null);

  COLUMNS.forEach(function (column) {
    stableColumns[column].forEach(function (row) {
      var dependencySummary = dependencyProjection(row, allRows, blockingStems);
      var request = requestsByStem[row.stem] || null;
      var session = sessions['task:' + row.stem] || null;
      var lock = locks[row.stem] || null;
      var finalization = finalizations[row.stem] || null;
      var screens = screensMap[row.stem] || null;
      var latest;
      try { latest = latestReader(row.stem); }
      catch (_) { latest = { event: null, truncated: true }; }
      if (latest && latest.truncated) limitations['task-journal-truncated'] = true;
      var checkpointState = retryCheckpoint(latest, row);
      var checkpoint = checkpointState.candidate;
      var stopped = !!(lock && session && session.running === false && session.endedAt);
      // awaitingTurn means the model is still executing the current turn; it is
      // not a request for user input. Only an explicit needs_action projection
      // (askedThisTurn) may raise the awaiting-answer blocker / needs-action
      // priority. Both states still resolve to the same terminal re-entry CTA.
      var liveAwaiting = !!(session && session.running && session.askedThisTurn);
      // A durable escalation question lives in the todo body, so it survives a
      // reaped session and a server restart. The board must offer the answer
      // rail from content alone, never from session liveness.
      var questionsState = row.state === 'todo'
        ? taskRequirement.questionsState(canonicalBodyText(validation, row)) : null;
      var questionsPending = !!(questionsState && questionsState.valid && questionsState.unanswered > 0);
      var active = !!request || !!(lock && !stopped) || !!(session && session.running) || !!(finalization && finalization.recoveryRunning);
      var figmaReviewPending = !!(screens && screens.evidence && screens.evidence.reviewPending > 0);
      var figmaDesignInvalid = !!(screens && screens.designIssues &&
        screens.designIssues.captureBlocked === true);
      var figmaDesignIssues = figmaDesignInvalid ? {
        issueCount: Number.isSafeInteger(screens.designIssues.issueCount)
          ? Math.max(0, Math.min(screens.designIssues.issueCount, 1000)) : 0,
        kinds: Array.isArray(screens.designIssues.kinds)
          ? screens.designIssues.kinds.map(function (kind) {
            return typeof kind === 'string' ? kind.toLowerCase() : '';
          }).filter(function (kind) {
            return /^[a-z][a-z0-9_-]{0,79}$/.test(kind);
          }).slice(0, 3) : [],
        first: screens.designIssues.first &&
          typeof screens.designIssues.first.kind === 'string' &&
          /^[A-Za-z][A-Za-z0-9_-]{0,79}$/.test(screens.designIssues.first.kind)
          ? {
            kind: screens.designIssues.first.kind.toLowerCase(),
            line: Number.isSafeInteger(screens.designIssues.first.line) &&
              screens.designIssues.first.line > 0
              ? screens.designIssues.first.line : null
          } : null
      } : null;
      var figmaMissing = !!(screens && screens.needed && !screens.pulled);
      var meta = validation && validation._model && validation._model.metadata && validation._model.metadata.get(row.stem);
      var manualValidationRequired = row.state === 'done' && !!(meta && meta.outcome &&
        Array.isArray(meta.outcome.acceptance) && meta.outcome.acceptance.some(function (item) { return item.verdict === 'manual'; }));
      var validationReceipt = manualValidationRequired
        ? currentValidationReceipt(validationReceipts, row, canonicalBodyRevision(validation, row)) : null;
      var validationPending = manualValidationRequired && !validationReceipt;
      var finalizationRevision = finalization ? [
        finalization.revision == null ? '' : String(finalization.revision),
        finalization.etag || '', finalization.status || '', finalization.phase || '',
        finalization.recoveryRunning === true ? 'running' : 'idle',
        finalization.recoverable === true ? 'recoverable' : 'blocked'
      ].join('|') : null;
      var liveSessionRevision = session && session.revision || (session ? primaryAction.hash({
        sessionId: session.sessionId || null,
        startedAt: session.startedAt || null,
        endedAt: session.endedAt || null,
        running: session.running === true,
        awaitingTurn: session.awaitingTurn === true,
        askedThisTurn: session.askedThisTurn === true,
        inputReady: session.inputReady === true,
        action: session.action || null
      }) : null);
      var runtime = { request: request, session: session, finalization: finalization };
      var taskFindings = globalActionFindings.concat(findingsByStem[row.stem] || [])
        .filter(function (item, index, all) { return all.indexOf(item) === index; });
      var taskHealth = taskFindings.some(function (item) {
        return item.severity === 'error' || item.severity === 'blocker';
      }) ? 'error' : taskFindings.some(function (item) {
        return item.severity === 'warning';
      }) ? 'warning' : 'ok';
      var dropVerdict = {
        scope: row.stem,
        observedState: row.state,
        sourceRevision: row.sourceRevision,
        findings: taskFindings
      };
      if (validation && validation._model) {
        Object.defineProperty(dropVerdict, '_model', {
          value: validation._model,
          enumerable: false
        });
      }
      var dropAvailable = core.dropAdmission(dropVerdict, row.stem).ok;
      var context = {
        stem: row.stem,
        state: row.state,
        findings: taskFindings,
        dependencySummary: dependencySummary,
        finalization: finalization,
        liveAwaiting: liveAwaiting,
        figmaReviewPending: figmaReviewPending,
        figmaDesignInvalid: figmaDesignInvalid,
        figmaScreensMissing: figmaMissing,
        setupIncomplete: !!(snapshot.progress && snapshot.progress.setupDone === false),
        reviewerUnavailable: row.state === 'todo' && snapshot.reviewerConfig && snapshot.reviewerConfig.state === 'invalid',
        stoppedRun: stopped,
        questionsPending: questionsPending,
        retryCheckpoint: checkpoint,
        validationPending: validationPending,
        runnerUnavailable: !active && !attached && (row.state === 'backlog' || row.state === 'todo')
      };
      var blockers = taskBlockers.fromContext(context);
      var action = primaryAction.resolve({
        stem: row.stem,
        state: row.state,
        sourceRevision: row.sourceRevision,
        blockers: blockers,
        finalization: finalization,
        active: active,
        liveAwaiting: liveAwaiting,
        questionsPending: questionsPending,
        terminalAvailable: !!(session && session.running),
        visualReview: figmaReviewPending,
        validationPending: validationPending,
        retryCheckpoint: checkpoint,
        liveSessionId: session && session.sessionId || null,
        sessionRevision: liveSessionRevision,
        sessionInputReady: !!(session && session.inputReady === true),
        finalizationRevision: finalizationRevision
      });
      var activity = lastActivity(row, runtime, latest);
      var secondaryActions = primaryAction.secondary({
        stem: row.stem,
        state: row.state,
        sourceRevision: row.sourceRevision,
        blockers: blockers,
        active: active,
        dropAvailable: dropAvailable,
        primaryAction: action,
        retryCheckpoint: checkpoint,
        liveSessionId: session && session.sessionId || null,
        sessionRevision: liveSessionRevision,
        sessionInputReady: !!(session && session.inputReady === true),
        finalizationRevision: finalizationRevision
      });
      var projected = Object.assign({}, row, {
        sourceTarget: sourceTarget(row.origin, allRows, sourceAvailability),
        runtimeStatus: {
          state: finalization ? 'finalizing' : liveAwaiting ? 'awaiting' :
            request ? (attached ? 'queued' : 'waiting-runner') :
              active ? 'running' : stopped ? 'stopped' : 'idle',
          phase: finalization && finalization.phase || lock && lock.stage || null,
          active: active,
          sessionKey: session ? 'task:' + row.stem : null
        },
        primaryBlocker: blockers[0] || null,
        blockerCount: blockers.length,
        blockers: blockers,
        taskHealth: {
          severity: taskHealth,
          findingCount: taskFindings.length
        },
        figmaDesignIssues: figmaDesignIssues,
        retryRecovery: checkpointState.recovery,
        dependencySummary: dependencySummary,
        primaryAction: action,
        secondaryActions: secondaryActions,
        lastActivity: activity,
        compactSignals: compactSignals(row, screens),
        // Server-verified test-certification projection (§19.4): the client
        // renders this typed status and never computes PASS itself.
        testCertification: (row.state === 'todo' || row.state === 'done')
          ? taskTestCertification.statusFor(row.stem, { doneTask: row.state === 'done' })
          : null
      });
      projected.finalization = finalization ? {
        stem: row.stem,
        revision: finalization.revision,
        etag: finalization.etag || null,
        status: finalization.status || null,
        phase: finalization.phase || null,
        recoverable: finalization.recoverable === true,
        recoveryRunning: finalization.recoveryRunning === true
      } : null;
      projected.appValidation = {
        required: manualValidationRequired,
        current: !!validationReceipt,
        overall: validationReceipt ? validationReceipt.overall : null
      };
      columns[column].push(projected);
    });
  });

  var projectedMap = rowsMap(columns);
  var blocksCounts = Object.create(null);
  COLUMNS.forEach(function (column) {
    columns[column].forEach(function (row) {
      var counted = Object.create(null);
      (row.dependsOn || []).forEach(function (dependencyStem) {
        if (dependencyStem === row.stem || counted[dependencyStem] || !projectedMap[dependencyStem]) return;
        counted[dependencyStem] = true;
        blocksCounts[dependencyStem] = (blocksCounts[dependencyStem] || 0) + 1;
      });
    });
  });
  COLUMNS.forEach(function (column) {
    columns[column].forEach(function (row) {
      row.dependencySummary.blocksCount = blocksCounts[row.stem] || 0;
      row.dependencySummary.items.forEach(function (item) {
        var target = projectedMap[item.stem];
        item.lastActivity = target && target.lastActivity || null;
      });
    });
  });
  return { columns: columns, limitations: Object.keys(limitations).sort() };
}

function taskNumber(stem) {
  var match = /^TASK_([1-9][0-9]*)_/.exec(stem || '');
  return match ? Number(match[1]) : Number.MAX_SAFE_INTEGER;
}

// Pin only cards that represent a live execution entry point. `active` alone
// is intentionally too broad: it also covers queued work and finalization
// recovery whose primary action may be a blocker/recovery flow instead of the
// task terminal. The card contract already resolves that precedence, so the
// exact `open-terminal` behavior is the canonical UX signal here.
function activeExecutionRank(row) {
  return row && row.runtimeStatus && row.runtimeStatus.active === true &&
    row.primaryAction && row.primaryAction.enabled !== false &&
    row.primaryAction.behavior === 'open-terminal' ? 1 : 0;
}

function sortColumns(columns, mode) {
  COLUMNS.forEach(function (column) {
    columns[column].sort(function (left, right) {
      // Active execution stays visible at the top of its own column under
      // every user-selected sort. The selected comparator still orders cards
      // within the active and inactive groups, so `recent`/`number` retain
      // their meaning instead of being replaced by a second hidden sort.
      var lx = activeExecutionRank(left), rx = activeExecutionRank(right);
      if (lx !== rx) return rx - lx;
      if (mode === 'number') return taskNumber(left.stem) - taskNumber(right.stem);
      var la = Date.parse(left.lastActivity && left.lastActivity.occurredAt || '') || 0;
      var ra = Date.parse(right.lastActivity && right.lastActivity.occurredAt || '') || 0;
      if (mode === 'recent' || column === 'done') return ra - la || taskNumber(right.stem) - taskNumber(left.stem);
      var lu = USER_ACTIONS[left.primaryAction.kind] ? 1 : 0;
      var ru = USER_ACTIONS[right.primaryAction.kind] ? 1 : 0;
      if (lu !== ru) return ru - lu;
      var lb = left.primaryBlocker ? 1 : 0, rb = right.primaryBlocker ? 1 : 0;
      if (lb !== rb) return rb - lb;
      return ra - la || taskNumber(left.stem) - taskNumber(right.stem);
    });
  });
}

function filterHash(options) {
  return hash({
    column: options.column || null, search: options.search || '', origin: options.origin || null,
    blocker: options.blocker || null, dependency: options.dependency || null, context: options.context || null,
    needsAction: options.needsAction === true, sort: options.sort || 'board'
  });
}

function encodeCursor(offset, revision, filters) {
  var payload = { offset: offset, revision: revision, filters: filters };
  var raw = Buffer.from(JSON.stringify(payload), 'utf8').toString('base64url');
  return raw + '.' + hash(raw).slice('sha256:'.length, 'sha256:'.length + 16);
}

function decodeCursor(value, revision, filters) {
  if (!value) return 0;
  if (typeof value !== 'string' || value.length > 1000) throw Object.assign(new Error('cursor is invalid'), { code: 'task-summary-cursor-invalid' });
  var pieces = value.split('.');
  if (pieces.length !== 2 || pieces[1] !== hash(pieces[0]).slice('sha256:'.length, 'sha256:'.length + 16)) {
    throw Object.assign(new Error('cursor is invalid'), { code: 'task-summary-cursor-invalid' });
  }
  var payload;
  try { payload = JSON.parse(Buffer.from(pieces[0], 'base64url').toString('utf8')); }
  catch (_) { throw Object.assign(new Error('cursor is invalid'), { code: 'task-summary-cursor-invalid' }); }
  if (!payload || payload.revision !== revision || payload.filters !== filters ||
      !Number.isSafeInteger(payload.offset) || payload.offset < 0) {
    throw Object.assign(new Error('cursor is stale'), { code: 'task-summary-cursor-stale' });
  }
  return payload.offset;
}

function applyFilters(columns, options) {
  var query = String(options.search || '').normalize('NFC').trim().toLowerCase().slice(0, 200);
  var out = { backlog: [], pending: [], todo: [], done: [] };
  COLUMNS.forEach(function (column) {
    if (options.column && options.column !== column) return;
    out[column] = columns[column].filter(function (row) {
      if (query && (row.title + ' ' + row.stem).toLowerCase().indexOf(query) < 0) return false;
      if (options.origin && (!row.origin || row.origin.kind !== options.origin)) return false;
      var blocked = row.blockers.some(function (item) { return item.severity === 'blocking'; });
      if (options.blocker === 'blocked' && !blocked) return false;
      if (options.blocker === 'unblocked' && blocked) return false;
      var dependencies = row.dependencySummary || { count: 0, blockedCount: 0 };
      if (options.dependency === 'blocked' && dependencies.blockedCount < 1) return false;
      if (options.dependency === 'satisfied' && (dependencies.count < 1 || dependencies.blockedCount > 0)) return false;
      if (options.dependency === 'none' && dependencies.count > 0) return false;
      if (options.needsAction && !USER_ACTIONS[row.primaryAction.kind]) return false;
      if (options.context && (!row.origin || row.origin.kind !== options.context)) return false;
      return true;
    });
  });
  return out;
}

function paginate(columns, offset, limit) {
  var flat = [];
  COLUMNS.forEach(function (column) { columns[column].forEach(function (row) { flat.push({ column: column, row: row }); }); });
  var page = flat.slice(offset, offset + limit);
  var out = { backlog: [], pending: [], todo: [], done: [] };
  page.forEach(function (entry) { out[entry.column].push(entry.row); });
  return { columns: out, total: flat.length, nextOffset: offset + page.length < flat.length ? offset + page.length : null };
}

function build(options, dependencies) {
  options = options || {};
  dependencies = dependencies || {};
  var indexRead = null;
  if (Object.prototype.hasOwnProperty.call(dependencies, 'indexRead')) {
    indexRead = dependencies.indexRead;
  } else {
    try { indexRead = readIndex(); }
    catch (_) { indexRead = null; }
  }
  var validation = dependencies.validation || taskIntegrity.validateAllCached();
  var snapshot = dependencies.snapshot || stateMod.deriveState();
  var latestReader = dependencies.latestReader || tasksLog.readLatest;
  var generatedAt = indexRead && indexRead.value &&
    typeof indexRead.value.generatedAt === 'string'
    ? indexRead.value.generatedAt : '1970-01-01T00:00:00Z';
  var canonical = Object.prototype.hasOwnProperty.call(dependencies, 'canonicalRows')
    ? dependencies.canonicalRows : canonicalByStem(validation, generatedAt);
  if (!canonical || typeof canonical !== 'object' || Array.isArray(canonical)) {
    throw Object.assign(new Error('canonical task rows are unavailable'), { code: 'task-state-unavailable' });
  }
  var stable = resilientRows(indexRead && indexRead.value, canonical);
  var receiptSource = Object.prototype.hasOwnProperty.call(dependencies, 'validationReceipts')
    ? dependencies.validationReceipts : readAppValidationReceipts();
  var validationReceiptInput = normalizeValidationReceipts(receiptSource);
  var projection = projectRows(stable, validation, snapshot, latestReader,
    validationReceiptInput.receipts,
    dependencies.sourceAvailability || readSourceAvailability());
  var columns = projection.columns;
  sortColumns(columns, ['board', 'recent', 'number'].indexOf(options.sort) >= 0 ? options.sort : 'board');
  var limitations = [];
  if (validation && validation.runtimeStats && validation.runtimeStats.truncated) limitations.push('runtime-scan-truncated');
  if (!indexRead) limitations.push('task-index-invalid');
  else if (!validation || validation.indexStatus !== 'fresh') {
    limitations.push('task-index-' + String(validation && validation.indexStatus || 'invalid'));
  }
  if (validation && validation.ok === false) limitations.push('task-integrity');
  limitations = limitations.concat(projection.limitations);
  if (validationReceiptInput.limitation) limitations.push(validationReceiptInput.limitation);
  limitations = Array.from(new Set(limitations));
  var revision = hash({
    index: indexRead && indexRead.revision || null,
    snapshot: validation && validation.snapshotHash || null,
    columns: columns,
    limitations: limitations
  });
  var filters = filterHash(options);
  var filtered = applyFilters(columns, options);
  var limit = Number(options.limit);
  limit = Number.isSafeInteger(limit) && limit > 0 ? Math.min(limit, MAX_LIMIT) : DEFAULT_LIMIT;
  var offset = decodeCursor(options.cursor, revision, filters);
  var page = paginate(filtered, offset, limit);
  return {
    schemaVersion: 1,
    revision: revision,
    indexSchemaVersion: indexRead && indexRead.value && indexRead.value.version || 2,
    generatedAt: generatedAt,
    columns: page.columns,
    total: page.total,
    nextCursor: page.nextOffset == null ? null : encodeCursor(page.nextOffset, revision, filters),
    partial: limitations.length > 0,
    limitations: limitations
  };
}

function single(stem, dependencies) {
  if (!core.STEM_RE.test(String(stem || '')) || core.safeIntegerId(stem) === null) {
    throw Object.assign(new Error('task stem is invalid'), { code: 'task-summary-stem-invalid' });
  }
  var summary = build({ search: stem, limit: MAX_LIMIT }, dependencies);
  for (var c = 0; c < COLUMNS.length; c++) {
    var rows = summary.columns[COLUMNS[c]];
    for (var i = 0; i < rows.length; i++) if (rows[i].stem === stem) {
      return {
        schemaVersion: summary.schemaVersion,
        revision: summary.revision,
        partial: summary.partial,
        limitations: summary.limitations,
        task: rows[i]
      };
    }
  }
  return null;
}

module.exports = Object.freeze({
  COLUMNS: COLUMNS,
  DEFAULT_LIMIT: DEFAULT_LIMIT,
  MAX_LIMIT: MAX_LIMIT,
  readIndex: readIndex,
  canonicalByStem: canonicalByStem,
  resilientRows: resilientRows,
  dependencyProjection: dependencyProjection,
  readSourceAvailability: readSourceAvailability,
  sourceTarget: sourceTarget,
  normalizeValidationReceipts: normalizeValidationReceipts,
  currentValidationReceipt: currentValidationReceipt,
  projectRows: projectRows,
  build: build,
  single: single,
  publicErrorCode: publicErrorCode,
  encodeCursor: encodeCursor,
  decodeCursor: decodeCursor
});
