'use strict';

var crypto = require('crypto');
var apiCatalog = require('./api-catalog');
var appRunner = require('./app-runner');
var figmaEvidence = require('./figma-evidence');
var appValidation = require('./app-run-validation');
var git = require('./git');
var tasksLog = require('./tasks-log');
var taskSummary = require('./task-summary');
var taskRequirement = require('./task-requirement');
var taskSource = require('../../tasks/task-source-contract.cjs');

var DEFAULT_LIMIT = 50;
var MAX_LIMIT = 200;
var KINDS = Object.freeze(['file', 'figma-node', 'endpoint', 'screenshot', 'build-result', 'validation']);

function hash(value) {
  return 'sha256:' + crypto.createHash('sha256').update(JSON.stringify(value), 'utf8').digest('hex');
}

function artifact(id, kind, label, status, source, capturedAt, target, metadata) {
  return {
    id: id,
    kind: kind,
    label: String(label || '').slice(0, 500),
    status: ['current', 'stale', 'passed', 'failed', 'warning', 'unavailable'].indexOf(status) >= 0 ? status : 'unavailable',
    source: source,
    capturedAt: capturedAt || null,
    relatedAcceptanceIds: [],
    target: target || null,
    metadata: metadata || {}
  };
}

function outcomeArtifacts(source, rows) {
  var outcome = source.metadata.outcome;
  if (!outcome) return;
  (outcome.files || []).slice(0, 100).forEach(function (file, index) {
    var path = typeof file === 'string' ? file : file && file.path;
    var change = typeof file === 'object' && file ? file.change : null;
    if (!path) return;
    if (!change && outcome.sections && outcome.sections['Files touched']) {
      var escaped = String(path).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      var match = new RegExp('`' + escaped + '`[ \\t]+—[ \\t]+([a-z]+)').exec(
        outcome.sections['Files touched']
      );
      change = match && match[1];
    }
    // The canonical Outcome contract uses created|modified|deleted|renamed,
    // while the public artifact contract intentionally exposes the simpler
    // added|modified|deleted vocabulary.
    change = change === 'created' ? 'added' :
      change === 'deleted' ? 'deleted' : 'modified';
    rows.push(artifact('file-' + hash(path).slice(7, 31), 'file', path, 'current',
      'outcome', outcome.completedAt, null, { change: change }));
  });
  taskRequirement.bullets(outcome.sections && outcome.sections['Build gates'], 50).forEach(function (gate, index) {
    var parsed = /^`([^`\r\n]+)`[ \t]+—[ \t]+(pass|fail|skipped)(?:[ \t]+\(([^()\r\n]{1,200})\))?$/.exec(gate);
    var verdict = parsed && parsed[2] || null;
    var status = !outcome.valid || !parsed ? 'unavailable' :
      verdict === 'pass' ? 'passed' : verdict === 'fail' ? 'failed' : 'warning';
    rows.push(artifact('build-' + index + '-' + hash(gate).slice(7, 19), 'build-result',
      parsed ? parsed[1] : gate, status, 'outcome', outcome.completedAt, null, {
        verdict: verdict,
        note: parsed && parsed[3] || null
      }));
  });
}

function journalArtifacts(stem, source, rows, limitations, provided) {
  var log;
  try { log = provided === undefined ? tasksLog.readLog(stem) : provided; }
  catch (_) { limitations.push('task-journal-unavailable'); return; }
  if (!log || !Array.isArray(log.events)) {
    limitations.push('task-journal-unavailable');
    return;
  }
  log.events.filter(function (event) {
    return event && event.kind === 'gate';
  }).slice(-100).forEach(function (event) {
    var status = event.status === 'ok' ? 'passed' :
      event.status === 'fail' ? 'failed' :
        event.status === 'blocked' || event.status === 'escalate' ||
          event.status === 'skipped' ? 'warning' : 'current';
    rows.push(artifact(
      'journal-gate-' + hash([
        event.ts, event.phase, event.status,
        event.meta && event.meta.reportId
      ]).slice(7, 31),
      'build-result',
      [event.phase || 'pipeline', 'gate'].join(' · '),
      status,
      'task-journal',
      event.ts,
      null,
      {
        phase: event.phase || null,
        verdict: event.status || null,
        durationMs: event.durationMs === undefined ? null : event.durationMs,
        reportId: event.meta && event.meta.reportId || null,
        taskSourceRevision: source.bodyRevision
      }
    ));
  });
  if (log.truncated) limitations.push('task-journal-truncated');
}

function appRunJobArtifacts(stem, rows, limitations, provided) {
  var result;
  try { result = provided === undefined ? appRunner.taskJobs(stem, 100) : provided; }
  catch (_) { result = null; }
  if (!result || result.ok !== true || !Array.isArray(result.jobs)) {
    limitations.push('app-run-jobs-unavailable');
    return;
  }
  result.jobs.forEach(function (job) {
    var build = (job.stages || []).find(function (stage) {
      return stage && stage.id === 'building';
    });
    if (!build || build.status === 'queued') return;
    var status = build.status === 'success' ? 'passed' :
      build.status === 'failed' ? 'failed' :
        build.status === 'skipped' ? 'warning' : 'current';
    rows.push(artifact(
      'app-build-' + job.jobId,
      'build-result',
      [job.platform, job.variantId, job.buildMode].filter(Boolean).join(' · '),
      status,
      'app-run-job',
      job.finishedAt || job.updatedAt,
      { panel: 'app-run', entityId: job.jobId },
      {
        jobId: job.jobId,
        action: job.action,
        platform: job.platform,
        variantId: job.variantId,
        buildMode: job.buildMode,
        durationMs: build.durationMs,
        requestedProjectSourceRevision: job.requestedProjectSourceRevision,
        appProjectSourceRevision: job.appProjectSourceRevision,
        runConfigHash: job.runConfigHash,
        artifactId: job.artifactId
      }
    ));
  });
}

function sourceArtifacts(source, row, rows, limitations, providedApiFacts) {
  var projected = taskRequirement.requirement(source);
  projected.sources.filter(function (item) { return item.kind === 'figma-node'; }).forEach(function (item, index) {
    rows.push(artifact('figma-node-' + item.id,
      'figma-node', item.label || item.ref, 'current', 'task-design', null,
      item.target, item.metadata));
  });

  var apiItems = projected.sources.filter(function (item) {
    return item.kind === 'api' && item.type !== 'api-work-package';
  });
  var typedSourceIds = apiItems.map(function (item) { return item.ref; }).filter(function (sourceId) {
    return /^(?:api:missing:[A-Za-z0-9][A-Za-z0-9._:-]{0,199}|api:change:chg-[a-f0-9]{24}|api:mismatch:mismatch-[a-f0-9]{24})$/.test(sourceId);
  });
  if (typedSourceIds.length !== apiItems.length) limitations.push('api-task-source-invalid');
  var facts = providedApiFacts;
  if (typedSourceIds.length && facts === undefined) {
    try { facts = apiCatalog.taskSourceFacts(typedSourceIds); }
    catch (_) { facts = null; }
  }
  var factsById = Object.create(null);
  if (typedSourceIds.length) {
    if (!facts || facts.ok !== true || !Array.isArray(facts.items)) {
      limitations.push('api-task-sources-unavailable');
    } else {
      facts.items.forEach(function (item) {
        if (item && typeof item.sourceId === 'string') factsById[item.sourceId] = item;
      });
      if (facts.limitations && facts.limitations.length) {
        limitations.push('api-task-sources-partial');
      }
    }
  }
  apiItems.forEach(function (item) {
    var typed = factsById[item.ref] || null;
    rows.push(artifact('endpoint-' + item.id, 'endpoint',
      typed ? typed.label : item.label || item.ref,
      typed ? typed.status : 'unavailable',
      item.metadata && item.metadata.packageId ? 'task-api-work-package' : 'task-source',
      null,
      typed && typed.status === 'current' ? item.target : null,
      {
        type: typed ? typed.type : item.type,
        sourceId: typed ? typed.sourceId : null,
        operationId: typed ? typed.operationId : null,
        method: typed ? typed.method : null,
        path: typed ? typed.path : null,
        area: typed ? typed.area : null,
        environmentId: typed ? typed.environmentId || null : null,
        contractHash: typed ? typed.contractHash || null : null,
        committedGenerationId: typed ? typed.committedGenerationId || null : null,
        implementationStatus: typed ? typed.implementationStatus || null : null,
        changeStatus: typed ? typed.changeStatus || null : null,
        packageId: item.metadata && item.metadata.packageId || null,
        groupKey: item.metadata && item.metadata.groupKey || null
      }));
  });
  var designIssue = row.figmaDesignIssues && row.figmaDesignIssues.first;
  if (designIssue) {
    rows.push(artifact('design-issue-' + hash(designIssue.kind + ':' + designIssue.line).slice(7, 31),
      'validation', 'Design declaration', 'failed', 'task-design', null, null, {
        labelKey: 'taskDetails.artifact.label.designDeclaration',
        issueKind: designIssue.kind,
        line: designIssue.line,
        issueCount: row.figmaDesignIssues.issueCount
      }));
  }
}

function figmaArtifacts(stem, rows, limitations) {
  var evidence;
  try { evidence = figmaEvidence.readEvidence(stem); }
  catch (_) { limitations.push('figma-evidence-unavailable'); return; }
  if (!evidence || !evidence.present) return;
  rows.push(artifact('figma-evidence-' + hash(evidence.pipelineRunId || stem).slice(7, 31),
    'figma-node', 'Figma evidence', evidence.stale ? 'stale' :
      evidence.blockingCount ? 'failed' : evidence.warningCount ? 'warning' : 'passed',
    'figma-evidence', evidence.generatedAt, { panel: 'task', entityId: stem, section: 'artifacts' }, {
      overall: evidence.overall,
      labelKey: 'taskDetails.artifact.label.figmaEvidence',
      evidenceState: evidence.evidenceState,
      issueCount: evidence.issueCount,
      reviewPending: evidence.pixelReview && Array.isArray(evidence.pixelReview.pending)
        ? evidence.pixelReview.pending.length : 0
    }));
  var visual = evidence.visualChecks;
  (visual && Array.isArray(visual.entries) ? visual.entries : []).slice(0, 48).forEach(function (entry, index) {
    var set = entry.artifactSet;
    var kinds = set && set.artifacts ? Object.keys(set.artifacts) : [];
    if (!kinds.length) {
      rows.push(artifact('screenshot-status-' + index, 'screenshot',
        [entry.screen, entry.theme].filter(Boolean).join(' · ') || 'Visual comparison',
        entry.status === 'PASS' ? 'passed' : 'failed', 'figma-evidence',
        evidence.generatedAt, null, {
          status: entry.status,
          reason: entry.reason || null,
          labelKey: [entry.screen, entry.theme].filter(Boolean).length
            ? null : 'taskDetails.artifact.label.visualComparison'
        }));
      return;
    }
    kinds.forEach(function (kind) {
      var ref = set.artifacts[kind];
      rows.push(artifact('screenshot-' + String(ref.id || index + '-' + kind).slice(0, 180),
        'screenshot', [entry.screen, entry.theme, kind].filter(Boolean).join(' · '),
        entry.status === 'PASS' ? 'passed' : evidence.stale ? 'stale' : 'warning',
        'figma-evidence', evidence.generatedAt, {
          panel: 'figma-compare',
          entityId: ref.id || null,
          guardedDownloadId: ref.id || null
        }, {
          screen: entry.screen, theme: entry.theme, kind: kind,
          mime: ref.mime || null, width: ref.width || null, height: ref.height || null,
          reportHash: visual.reportHash || null
        }));
    });
  });
  if (visual && visual.truncated) limitations.push('figma-screens-truncated');
}

function validationArtifacts(stem, source, rows, limitations, provided) {
  var receipts;
  try { receipts = provided === undefined ? appValidation.history(stem, 100) : provided; }
  catch (_) { limitations.push('validation-history-unavailable'); return; }
  if (!Array.isArray(receipts)) {
    limitations.push('validation-history-unavailable');
    return;
  }
  receipts.forEach(function (receipt) {
    var current = receipt.taskSourceRevision === source.bodyRevision &&
      receipt.staleTask === false && receipt.staleSource === false;
    rows.push(artifact('validation-' + receipt.receiptId, 'validation',
      'App validation · ' + receipt.overall,
      current ? receipt.overall === 'passed' ? 'passed' : receipt.overall === 'failed' ? 'failed' : 'warning' : 'stale',
      'app-run', receipt.createdAt, { panel: 'app-run', entityId: receipt.receiptId }, {
        overall: receipt.overall,
        labelKey: 'taskDetails.artifact.label.appValidation',
        labelStatus: receipt.overall,
        current: current,
        checkedCount: Array.isArray(receipt.checklist) ? receipt.checklist.length :
          Array.isArray(receipt.items) ? receipt.items.length : null
      }));
    (receipt.checklist || receipt.items || []).forEach(function (item) {
      (item.screenshotIds || []).forEach(function (screenshotId) {
        if (!/^shot-[a-f0-9]{36}$/.test(String(screenshotId || ''))) return;
        var screenshotStatus = !current ? 'stale' :
          item.result === 'pass' ? 'passed' :
            item.result === 'fail' ? 'failed' : 'warning';
        rows.push(artifact('app-screenshot-' + screenshotId, 'screenshot',
          receipt.deviceSummary || receipt.platform || 'App validation',
          screenshotStatus, 'app-run-validation', receipt.createdAt, {
            panel: 'app-run',
            entityId: screenshotId,
            guardedDownloadId: screenshotId
          }, {
            platform: receipt.platform || null,
            labelKey: receipt.deviceSummary || receipt.platform
              ? null : 'taskDetails.artifact.label.appValidation',
            device: receipt.deviceSummary || null,
            validationReceiptId: receipt.receiptId,
            manualItemId: item.itemId || null,
            result: item.result || null,
            appProjectSourceRevision: receipt.appProjectSourceRevision || null
          }));
      });
    });
  });
}

function safeRelativePath(value) {
  return typeof value === 'string' && value.length > 0 && value.length <= 500 &&
    value.indexOf('\\') < 0 && value.indexOf('\0') < 0 &&
    !/^(?:\/|[A-Za-z]:|[A-Za-z][A-Za-z0-9+.-]*:)/.test(value) &&
    value.split('/').every(function (part) {
      return part && part !== '.' && part !== '..' &&
        !/[\x00-\x1f\x7f-\x9f\u2028\u2029]/.test(part);
    });
}

function changedTreeArtifacts(row, rows, limitations, provided) {
  var runtime = row.runtimeStatus || {};
  if (['running', 'queued', 'waiting-runner', 'awaiting', 'stopped'].indexOf(runtime.state) < 0) return;
  var summary;
  try { summary = provided === undefined ? git.statusSummary() : provided; }
  catch (_) { summary = null; }
  if (!summary || summary.available !== true || !Array.isArray(summary.files)) {
    limitations.push('active-changed-tree-unavailable');
    return;
  }
  summary.files.slice(0, 100).forEach(function (file) {
    if (!file || !safeRelativePath(file.path)) {
      limitations.push('active-changed-tree-path-redacted');
      return;
    }
    var code = String(file.status || '').slice(0, 2);
    var change = code.indexOf('D') >= 0 ? 'deleted' :
      code.indexOf('A') >= 0 ? 'added' : 'modified';
    rows.push(artifact('active-file-' + hash(file.path).slice(7, 31), 'file',
      file.path, 'warning', 'active-run-changed-tree', null, null, {
        change: change,
        scope: 'project-working-tree',
        branch: typeof summary.branch === 'string' ? summary.branch.slice(0, 200) : null
      }));
  });
  // This bounded repository observation cannot prove that every tracked change
  // belongs to one task. Surface that limitation rather than presenting it as
  // a task-owned receipt.
  limitations.push('active-changed-tree-unscoped');
  if (summary.truncated) limitations.push('active-changed-tree-truncated');
}

function encodeCursor(offset, revision, kind) {
  var raw = Buffer.from(JSON.stringify({ offset: offset, revision: revision, kind: kind || null }), 'utf8').toString('base64url');
  return raw + '.' + hash(raw).slice(7, 23);
}

function decodeCursor(value, revision, kind) {
  if (!value) return 0;
  if (typeof value !== 'string' || value.length > 1000) return null;
  var parts = value.split('.');
  if (parts.length !== 2 || parts[1] !== hash(parts[0]).slice(7, 23)) return null;
  try {
    var parsed = JSON.parse(Buffer.from(parts[0], 'base64url').toString('utf8'));
    return parsed && parsed.revision === revision && parsed.kind === (kind || null) &&
      Number.isSafeInteger(parsed.offset) && parsed.offset >= 0 ? parsed.offset : null;
  } catch (_) { return null; }
}

function groups(rows) {
  return KINDS.map(function (kind) {
    return { kind: kind, count: rows.filter(function (item) { return item.kind === kind; }).length };
  });
}

function build(stem, options, dependencies) {
  options = options || {};
  dependencies = dependencies || {};
  if (!taskSource.safeTaskStem(stem)) return { ok: false, status: 400, error: 'bad-task-stem' };
  if (options.kind && KINDS.indexOf(options.kind) < 0) return { ok: false, status: 400, error: 'bad-artifact-kind' };
  var current = dependencies.summary || taskSummary.single(stem);
  var source = dependencies.source || taskRequirement.load(stem);
  if (!current || !source) return { ok: false, status: 404, error: 'task-not-found' };
  var rows = [], limitations = [];
  outcomeArtifacts(source, rows);
  sourceArtifacts(source, current.task, rows, limitations, dependencies.apiSourceFacts);
  journalArtifacts(stem, source, rows, limitations, dependencies.taskLog);
  appRunJobArtifacts(stem, rows, limitations, dependencies.appRunJobs);
  figmaArtifacts(stem, rows, limitations);
  validationArtifacts(stem, source, rows, limitations, dependencies.validationReceipts);
  if (!source.metadata.outcome) changedTreeArtifacts(
    current.task, rows, limitations, dependencies.gitStatus
  );
  var seen = Object.create(null);
  rows = rows.filter(function (item) {
    var key = item.kind + '\0' + item.id;
    if (seen[key]) return false;
    seen[key] = true;
    return true;
  });
  rows = rows.filter(function (item) {
    return item.kind === 'screenshot';
  }).concat(rows.filter(function (item) {
    return item.kind !== 'screenshot';
  }));
  if (rows.length > 500) {
    limitations.push('artifact-limit-reached');
    rows = rows.slice(0, 500);
  }
  var revision = hash({ source: source.metadata.revision, rows: rows, limitations: limitations });
  var allGroups = groups(rows);
  if (options.kind) rows = rows.filter(function (item) { return item.kind === options.kind; });
  var limit = Number(options.limit);
  limit = Number.isSafeInteger(limit) && limit > 0 ? Math.min(limit, MAX_LIMIT) : DEFAULT_LIMIT;
  var offset = decodeCursor(options.cursor, revision, options.kind);
  if (offset == null) return { ok: false, status: 409, error: 'task-artifacts-cursor-stale' };
  var page = rows.slice(offset, offset + limit);
  return {
    ok: true,
    status: 200,
    schemaVersion: 1,
    stem: stem,
    revision: revision,
    taskSourceRevision: source.metadata.revision,
    groups: allGroups,
    artifacts: page,
    nextCursor: offset + page.length < rows.length ? encodeCursor(offset + page.length, revision, options.kind) : null,
    partial: limitations.length > 0,
    limitations: Array.from(new Set(limitations)).sort()
  };
}

function summary(stem, dependencies) {
  dependencies = dependencies || {};
  var current = dependencies.summary || taskSummary.single(stem);
  var source = dependencies.source || taskRequirement.load(stem);
  if (!current || !source) return { ok: false, status: 404, error: 'task-not-found' };
  var rows = [];
  outcomeArtifacts(source, rows);
  var limitations = [];
  sourceArtifacts(source, current.task, rows, limitations, dependencies.apiSourceFacts);
  journalArtifacts(stem, source, rows, limitations, dependencies.taskLog);
  appRunJobArtifacts(stem, rows, limitations, dependencies.appRunJobs);
  var counts = Object.create(null);
  KINDS.forEach(function (kind) { counts[kind] = 0; });
  rows.forEach(function (row) { counts[row.kind]++; });
  (current.task.compactSignals || []).forEach(function (signal) {
    if (signal.kind === 'figma-screens') {
      counts.screenshot += Number.isSafeInteger(signal.count) ? signal.count : 1;
    }
  });
  if (current.task.appValidation && current.task.appValidation.current) counts.validation++;
  var projectedGroups = KINDS.map(function (kind) {
    return { kind: kind, count: counts[kind] };
  });
  return {
    ok: true,
    revision: hash({
      source: source.metadata.revision,
      groups: projectedGroups,
      appValidation: current.task.appValidation || null
    }),
    groups: projectedGroups,
    partial: limitations.length > 0,
    limitations: Array.from(new Set(limitations)).sort()
  };
}

module.exports = Object.freeze({
  DEFAULT_LIMIT: DEFAULT_LIMIT,
  MAX_LIMIT: MAX_LIMIT,
  KINDS: KINDS,
  build: build,
  summary: summary
});
