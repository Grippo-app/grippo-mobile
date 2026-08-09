'use strict';

var crypto = require('crypto');
var configUpdate = require('./project-config-update');
var detector = require('./reviewer-detector');
var activity = require('./reviewer-activity');

var MODES = {
  auto: 'automatic',
  true: 'require-codex',
  false: 'internal-only'
};
var CONFIG_VALUES = {
  automatic: 'auto',
  'require-codex': 'true',
  'internal-only': 'false'
};
var IDEMPOTENCY_TTL_MS = 10 * 60 * 1000;
var IDEMPOTENCY_MAX = 100;
var idempotency = new Map();

function exactObject(value, fields) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return false;
  var keys = Object.keys(value).sort();
  return keys.length === fields.length && fields.slice().sort().every(function (field, index) {
    return keys[index] === field;
  });
}

function configProjection() {
  var read = configUpdate.read();
  if (!read.ok) {
    return { mode: null, revision: read.revision || null, state: 'invalid', reasonCode: 'config-invalid' };
  }
  var mode = MODES[read.codexEnabled] || null;
  return {
    mode: mode,
    revision: read.revision,
    state: mode ? 'ready' : (read.codexFieldState === 'missing' ? 'missing' : 'invalid'),
    reasonCode: mode ? null : 'config-invalid'
  };
}

function nextPolicy(mode, codex) {
  if (mode === 'internal-only') return { reviewer: 'internal-reviewer', fallback: 'none' };
  if (mode === 'require-codex') {
    return codex.availability === 'available'
      ? { reviewer: 'codex', fallback: 'none' }
      : { reviewer: 'blocked', fallback: 'none' };
  }
  if (mode === 'automatic') {
    return codex.availability === 'available'
      ? { reviewer: 'codex', fallback: 'internal-when-not-detected' }
      : { reviewer: 'internal-reviewer', fallback: 'internal-when-not-detected' };
  }
  return { reviewer: 'unknown', fallback: 'none' };
}

function activeReview(activitySnapshot) {
  if (!activitySnapshot.active.length) return null;
  var reviewers = Object.create(null);
  activitySnapshot.active.forEach(function (row) {
    reviewers[row.reviewer || 'unknown'] = true;
  });
  var names = Object.keys(reviewers);
  if (names.length > 1) {
    return { reviewer: 'mixed', basis: 'conflicting-active-events', warning: 'conflicting-active-events' };
  }
  var selectedReviewer = names.length ? names[0] : 'unknown';
  var selectedRow = activitySnapshot.active.find(function (row) {
    return row.reviewer === selectedReviewer;
  });
  return {
    reviewer: selectedReviewer,
    basis: 'active-review',
    warning: null,
    selectionReason: selectedRow && selectedRow.selectionReason || null
  };
}

function publicLastReview(row) {
  if (!row) return null;
  return {
    taskStem: row.taskStem,
    taskTitle: row.taskTitle,
    reviewer: row.reviewer,
    result: row.status === 'passed' ? 'passed'
      : row.status === 'failed' ? 'failed'
        : (row.status === 'escalated' || row.status === 'blocked') ? 'escalated' : 'unknown',
    startedAt: row.startedAt,
    finishedAt: row.finishedAt,
    selectionFallbackUsed: row.selectionReason === 'codex-unavailable' || row.reasonCode === 'fallback-used'
  };
}

function overall(config, codex, activitySnapshot, active) {
  if (config.state !== 'ready') return 'attention-required';
  if (active && active.reviewer === 'mixed') return 'attention-required';
  if (config.mode === 'require-codex' && codex.availability !== 'available') {
    // A reviewer already locked in phase-start remains authoritative even if
    // current readiness changes. The next attempt needs attention, but the
    // running attempt is not retroactively blocked.
    return active ? 'attention-required' : 'blocked';
  }
  if (activitySnapshot.failed.length || activitySnapshot.partial ||
      (config.mode !== 'internal-only' && codex.availability === 'unknown')) return 'attention-required';
  if (!activitySnapshot.lastReview && !activitySnapshot.pending.length) return 'no-recent-data';
  return 'operational';
}

function aggregate(config, codex, activitySnapshot) {
  var current = activeReview(activitySnapshot);
  var policy = nextPolicy(config.mode, codex);
  var active = current || {
    reviewer: config.state === 'ready' ? policy.reviewer : 'unknown',
    basis: config.state === 'ready' ? 'next-policy' : 'unavailable',
    warning: null
  };
  var fallbackPolicy = policy.fallback;
  if (current) {
    fallbackPolicy = current.reviewer === 'internal-reviewer' &&
      current.selectionReason === 'codex-unavailable'
      ? 'internal-when-not-detected' : 'none';
  }
  return {
    schemaVersion: 1,
    overall: overall(config, codex, activitySnapshot, current),
    config: {
      mode: config.mode,
      revision: config.revision,
      state: config.state,
      reasonCode: config.reasonCode
    },
    review: {
      enabled: config.state === 'ready',
      activeReviewer: active.reviewer,
      activeReviewerBasis: active.basis,
      // Once a review starts there is no runtime substitution. Preserve the
      // Automatic fallback marker only when the structured selection says it
      // was already applied before phase-start.
      fallbackPolicy: fallbackPolicy,
      integrityWarning: active.warning
    },
    codex: {
      availability: codex.availability,
      installed: codex.installed,
      checkedAt: codex.checkedAt,
      reasonCode: codex.reasonCode,
      detectorVersion: codex.detectorVersion
    },
    lastReview: publicLastReview(activitySnapshot.lastReview),
    counts: {
      pending: activitySnapshot.pending.length,
      failed: activitySnapshot.failed.length
    },
    pending: activitySnapshot.pending.slice(0, 10),
    failed: activitySnapshot.failed.slice(0, 10),
    diagnosticsAvailable: codex.availability !== 'available' || activitySnapshot.partial ||
      config.state !== 'ready' || !!active.warning,
    diagnostics: {
      detectorSource: codex.source,
      activityPartial: activitySnapshot.partial,
      activityReasonCode: activitySnapshot.reasonCode,
      activityRevision: activitySnapshot.revision
    }
  };
}

function status(forceDetector) {
  return detector.get(forceDetector === true).then(function (codex) {
    // Detection can take several seconds. Read config after it settles so a
    // concurrent guarded settings write cannot produce a stale revision/mode
    // paired with a fresh detector result.
    return aggregate(configProjection(), codex, activity.snapshot());
  });
}

function pruneIdempotency() {
  var now = Date.now();
  idempotency.forEach(function (value, key) {
    if (!value.promise && now - value.createdAt > IDEMPOTENCY_TTL_MS) idempotency.delete(key);
  });
  while (idempotency.size > IDEMPOTENCY_MAX) {
    var removable = null;
    idempotency.forEach(function (value, key) {
      if (removable === null && !value.promise) removable = key;
    });
    if (removable === null) break;
    idempotency.delete(removable);
  }
}

function settings(body) {
  if (!exactObject(body, ['mode', 'expectedRevision', 'idempotencyKey'])) {
    return Promise.resolve({ ok: false, status: 400, error: 'bad-reviewer-settings' });
  }
  if (!Object.prototype.hasOwnProperty.call(CONFIG_VALUES, body.mode) ||
      !/^sha256:[a-f0-9]{64}$/.test(String(body.expectedRevision || '')) ||
      typeof body.idempotencyKey !== 'string' || body.idempotencyKey.length < 8 ||
      body.idempotencyKey.length > 128 || /[^\x21-\x7e]/.test(body.idempotencyKey)) {
    return Promise.resolve({ ok: false, status: 400, error: 'bad-reviewer-settings' });
  }
  pruneIdempotency();
  var canonical = JSON.stringify({ mode: body.mode, expectedRevision: body.expectedRevision });
  var bodyHash = crypto.createHash('sha256').update(canonical).digest('hex');
  var prior = idempotency.get(body.idempotencyKey);
  if (prior) {
    if (prior.bodyHash !== bodyHash) {
      return Promise.resolve({ ok: false, status: 409, error: 'idempotency-conflict' });
    }
    return prior.promise || Promise.resolve(prior.response);
  }
  if (idempotency.size >= IDEMPOTENCY_MAX) {
    var removable = null;
    idempotency.forEach(function (value, key) {
      if (removable === null && !value.promise) removable = key;
    });
    if (removable !== null) idempotency.delete(removable);
    else return Promise.resolve({ ok: false, status: 503, error: 'project-busy' });
  }
  var entry = { createdAt: Date.now(), bodyHash: bodyHash, response: null, promise: null };
  idempotency.set(body.idempotencyKey, entry);
  entry.promise = Promise.resolve().then(function () {
    var updated = configUpdate.update({
      capability: 'reviewer',
      field: 'codexEnabled',
      value: CONFIG_VALUES[body.mode],
      expectedRevision: body.expectedRevision
    });
    if (!updated.ok) {
      var mapped = updated.error === 'project-config-revision-conflict' ? 'config-conflict'
        : updated.error === 'finalization-active' || /^writer-lease-/.test(updated.error || '')
          ? 'project-busy'
          : /^project-config-(?:frontmatter|duplicate|field|unsafe|unavailable|size-limit)/.test(updated.error || '')
            ? 'config-invalid' : 'reviewer-settings-failed';
      return {
        ok: false,
        status: updated.status || 409,
        error: mapped || 'reviewer-settings-failed',
        currentRevision: updated.currentRevision || null
      };
    }
    return status(false).then(function (value) {
      return { ok: true, status: 200, reviewer: value };
    });
  }).then(function (response) {
    entry.response = response;
    entry.promise = null;
    pruneIdempotency();
    return response;
  }, function () {
    var response = { ok: false, status: 500, error: 'reviewer-settings-failed' };
    entry.response = response;
    entry.promise = null;
    pruneIdempotency();
    return response;
  });
  pruneIdempotency();
  return entry.promise;
}

function recheck() {
  return status(true).then(function (value) {
    return { ok: true, status: 200, reviewer: value };
  });
}

function statusRevision() {
  var config = configProjection();
  return [
    config.revision || config.state,
    detector.revision(),
    activity.revision()
  ].join(':');
}

function activityRevision() {
  return activity.revision();
}

module.exports = {
  status: status,
  settings: settings,
  recheck: recheck,
  activity: activity.list,
  revision: statusRevision,
  statusRevision: statusRevision,
  activityRevision: activityRevision
};
