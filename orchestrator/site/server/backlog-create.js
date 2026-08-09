'use strict';

// Deterministic backlog publication controller. Identity, task bytes and INDEX
// publication are owned by create-backlog.py; Claude is deliberately absent
// from this path. The site publishes a writer lease before allowing the helper
// to receive its input, closing the finalizer check -> mutation race.

var cp = require('child_process');
var fs = require('fs');
var path = require('path');
var crypto = require('crypto');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var finalizations = require('./finalizations');
var childEnv = require('./child-env').childEnv;
var writerLeases = require('../../tasks/writer-leases.cjs');
var taskSource = require('../../tasks/task-source-contract.cjs');

var KEY_RE = /^[A-Za-z0-9_.:-]{16,240}$/;
var STEM_RE = /^TASK_([0-9]+)_[A-Za-z0-9_]+$/;
var HASH_RE = /^sha256:[a-f0-9]{64}$/i;
var TITLE_MAX_BYTES = 512;
var BODY_MAX_BYTES = 64 * 1024;
// A complete task includes the canonical heading and structural markdown in
// addition to the user body. Keep edit validation aligned with the durable
// create/edit helpers' full-file envelope, not only the create body's limit.
var TASK_STORAGE_MAX_BYTES = BODY_MAX_BYTES + 4096;
var OUTPUT_MAX_BYTES = 256 * 1024;
var STDERR_MAX_BYTES = 16 * 1024;
var TREE_VERIFY_MS = 5000;
var TREE_POLL_MS = 50;
var TIMEOUT_MS = (function () {
  var n = Number(process.env.CREATE_BACKLOG_TIMEOUT_MS || 120000);
  return Number.isFinite(n) ? Math.max(1000, Math.min(10 * 60 * 1000, Math.floor(n))) : 120000;
})();

var children = new Set();
var recoveryPromise = null;
var recoveryCombined = null;
var editRecoveryPromise = null;
var publicationRecoveryPromise = null;
var CREATION_EVENTS = path.join(paths.TASK_CREATIONS_DIR, '.events.jsonl');
var creationEventFailureReported = false;
var PUBLIC_CREATE_ERRORS = Object.freeze({
  'bad-json': true,
  'bad-input': 'bad-task-create-request',
  'unknown-field': 'bad-task-create-request',
  'bad-title': true,
  'bad-body': true,
  'bad-idempotency-key': 'bad-task-create-request',
  'bad-origin-stem': 'bad-task-create-request',
  'origin-conflict': 'task-source-invalid',
  'bad-dedup-key': true,
  'bad-dedup-report': true,
  'bad-task-source': true,
  'source-origin-conflict': 'task-source-invalid',
  'task-source-conflict': 'task-source-invalid',
  'IDEMPOTENCY_CONFLICT': true,
  'DEDUP_CONFLICT': true,
  'WORKSPACE_WRITER_ACTIVE': true,
  'create-protocol-invalid': true,
  'create-stdin-failed': true,
  'writer-tree-unverified': true,
  'CREATE_OUTPUT_TOO_LARGE': true,
  'CREATE_TIMEOUT': true,
  'ENOENT': 'create-runtime-unavailable',
  'finalization-active': true,
  'writer-lease-unavailable': true,
  'writer-lease-release-failed': true,
  'writer-lease-update-failed': true
});
var PUBLIC_EDIT_ERRORS = Object.freeze({
  'bad-json': true,
  'bad-input': 'bad-backlog-edit-request',
  'bad-edit-input': 'bad-backlog-edit-request',
  'bad-stem': true,
  'bad-source-hash': true,
  'bad-markdown': true,
  'SOURCE_CHANGED': true,
  'TASK_NOT_IDLE_BACKLOG': true,
  'EDIT_RECOVERY_REQUIRED': true,
  'WORKSPACE_WRITER_ACTIVE': true,
  'create-protocol-invalid': true,
  'create-stdin-failed': true,
  'writer-tree-unverified': true,
  'CREATE_OUTPUT_TOO_LARGE': true,
  'CREATE_TIMEOUT': true,
  'ENOENT': 'create-runtime-unavailable',
  'finalization-active': true,
  'writer-lease-unavailable': true,
  'writer-lease-release-failed': true,
  'writer-lease-update-failed': true
});

function publicErrorCode(error, allowed, fallback) {
  var code = error && error.code;
  if (!Object.prototype.hasOwnProperty.call(allowed, code)) return fallback;
  return allowed[code] === true ? code : allowed[code];
}
function publicCreateErrorCode(error) {
  return publicErrorCode(error, PUBLIC_CREATE_ERRORS, 'create-failed');
}
function publicEditErrorCode(error) {
  return publicErrorCode(error, PUBLIC_EDIT_ERRORS, 'backlog-edit-failed');
}

function creationEventFailure(code) {
  if (creationEventFailureReported) return;
  creationEventFailureReported = true;
  try { process.stderr.write('[creation-events] append refused: ' + String(code || 'runtime-unsafe').slice(0, 80) + '\n'); }
  catch (error) {}
}

function appendCreationEvent(kind, data) {
  try {
    var line = Buffer.from(JSON.stringify(Object.assign({ timestamp: new Date().toISOString(), event: kind }, data || {})) + '\n');
    if (line.length > 2048) { creationEventFailure('event-too-large'); return false; }
    var appended = fileGuards.appendBoundedRegularFileUnder(
      paths.TASK_CREATIONS_AUTHORITY_ROOT, paths.TASK_CREATIONS_DIR, CREATION_EVENTS, line,
      { create: true, directoryMode: 0o700, mode: 0o600, maxAppendBytes: 2048, maxBytes: 1024 * 1024 }
    );
    if (!appended || !appended.ok) { creationEventFailure(appended && appended.code); return false; }
    return true;
  } catch (error2) { creationEventFailure(error2 && error2.code || 'append-failed'); return false; }
}

function utf8Size(value) { return Buffer.byteLength(String(value == null ? '' : value), 'utf8'); }
function unicodeScalarCount(value) {
  var count = 0;
  for (var i = 0; i < value.length; i++) {
    var unit = value.charCodeAt(i);
    if (unit >= 0xD800 && unit <= 0xDBFF) {
      if (i + 1 >= value.length) return -1;
      var next = value.charCodeAt(i + 1);
      if (next < 0xDC00 || next > 0xDFFF) return -1;
      i++;
    } else if (unit >= 0xDC00 && unit <= 0xDFFF) return -1;
    count++;
  }
  return count;
}
function validEditStem(value) {
  return taskSource.safeTaskStem(value);
}
function validationError(code, detail) {
  var error = new Error(detail || code);
  error.code = code;
  error.httpStatus = 400;
  return error;
}
function validateInput(input) {
  if (!input || typeof input !== 'object' || Array.isArray(input)) throw validationError('bad-input');
  // `source` is an internal server/domain envelope. The public HTTP handler
  // rejects that field and resolves Manual/Follow-up itself before entering
  // this deterministic controller.
  var allowed = { title: 1, body: 1, idempotencyKey: 1, originStem: 1, dedupKey: 1, dedupReport: 1, source: 1 };
  var unknown = Object.keys(input).filter(function (key) { return !allowed[key]; });
  if (unknown.length) throw validationError('unknown-field', 'unknown field: ' + unknown[0]);
  if (typeof input.title !== 'string') throw validationError('bad-title', 'title is required');
  var titleChars = unicodeScalarCount(input.title);
  if (titleChars < 0 || titleChars > 200 || utf8Size(input.title) > TITLE_MAX_BYTES ||
      /[\u0000-\u001f\u007f-\u009f\u2028\u2029]/.test(input.title) || /[\u202a-\u202e\u2066-\u2069]/.test(input.title)) {
    throw validationError('bad-title', 'title contains unsafe characters or exceeds the UTF-8 limit');
  }
  var title = input.title.normalize('NFC').trim();
  if (!title) throw validationError('bad-title', 'title is required');
  if (typeof input.body !== 'string' || unicodeScalarCount(input.body) < 0 || utf8Size(input.body) > BODY_MAX_BYTES ||
      /[\u0000-\u0008\u000b\u000c\u000e-\u001f]/.test(input.body)) {
    throw validationError('bad-body', 'body must be valid UTF-8 text within the byte and control-character limits');
  }
  var body = input.body.replace(/\r\n/g, '\n').replace(/\r/g, '\n');
  if (typeof input.idempotencyKey !== 'string' || !KEY_RE.test(input.idempotencyKey)) throw validationError('bad-idempotency-key');
  if (input.originStem != null && (input.originStem.length > 120 || !taskSource.safeTaskStem(input.originStem))) {
    throw validationError('bad-origin-stem');
  }
  if (input.originStem != null && /^##[ \t]+Origin[ \t]*$/m.test(body)) throw validationError('origin-conflict');
  if (input.dedupKey != null && (typeof input.dedupKey !== 'string' || !/^[A-Za-z0-9_.:-]{1,240}$/.test(input.dedupKey))) {
    throw validationError('bad-dedup-key');
  }
  if (input.dedupReport != null && (typeof input.dedupReport !== 'string' || !HASH_RE.test(input.dedupReport))) {
    throw validationError('bad-dedup-report');
  }
  if (input.dedupReport != null && input.dedupKey == null) throw validationError('bad-dedup-report');
  var source = taskSource.validate(input.source);
  if (!source) throw validationError('bad-task-source');
  if (source.kind === 'follow-up' && source.type === 'task-split' && source.ref !== (input.originStem || null)) {
    throw validationError('source-origin-conflict');
  }
  if (taskSource.realSourceHeadings(body).length) throw validationError('task-source-conflict');
  var renderedBytes = utf8Size('# TASK 1 — ' + title + '\n\n' + taskSource.render(source) + (body ? '\n\n' + body : '') + '\n');
  if (renderedBytes > TASK_STORAGE_MAX_BYTES) throw validationError('bad-body', 'rendered task exceeds the storage limit');
  return {
    title: title,
    body: body,
    source: source,
    idempotencyKey: input.idempotencyKey,
    originStem: input.originStem || null,
    dedupKey: input.dedupKey || null,
    dedupReport: input.dedupReport ? input.dedupReport.toLowerCase() : null
  };
}

function validateEditInput(input) {
  if (!input || typeof input !== 'object' || Array.isArray(input)) throw validationError('bad-input');
  var keys = Object.keys(input).sort();
  if (keys.join(',') !== 'expectedSourceHash,markdown,stem') throw validationError('bad-edit-input');
  if (!validEditStem(input.stem)) throw validationError('bad-stem');
  if (typeof input.expectedSourceHash !== 'string' || !HASH_RE.test(input.expectedSourceHash)) throw validationError('bad-source-hash');
  if (typeof input.markdown !== 'string' || !input.markdown || unicodeScalarCount(input.markdown) < 0 || input.markdown.indexOf('\0') >= 0) {
    throw validationError('bad-markdown');
  }
  var markdown = input.markdown.replace(/\r\n/g, '\n').replace(/\r/g, '\n');
  if (!markdown.endsWith('\n')) markdown += '\n';
  if (utf8Size(markdown) > TASK_STORAGE_MAX_BYTES) throw validationError('bad-markdown');
  return { stem: input.stem, expectedSourceHash: input.expectedSourceHash.toLowerCase(), markdown: markdown };
}

function killTree(child, signal) {
  if (!child || !child.pid) return;
  if (process.platform !== 'win32') {
    try { process.kill(-child.pid, signal); return; } catch (error) {}
  }
  try { child.kill(signal); } catch (error2) {}
}

// A detached POSIX helper owns its whole process group. `close` only proves
// that the leader and its stdio are gone; a descendant may still be mutating
// the workspace. Never withdraw the writer lease until ESRCH proves the group
// empty. The production Python helper is single-process on Windows (its INDEX
// generator is executed in-process), so the child's close event is the proof
// available there.
function processTreeGone(child, closed) {
  if (!child || !child.pid) return true;
  if (process.platform === 'win32') return !!closed;
  try { process.kill(-child.pid, 0); return false; }
  catch (error) { return !!(error && error.code === 'ESRCH'); }
}

function parseProtocol(stdout) {
  var lines = String(stdout || '').split(/\r?\n/).map(function (line) { return line.trim(); }).filter(Boolean);
  if (lines[0] !== 'READY') {
    var readyError = new Error('deterministic helper did not publish READY');
    readyError.code = 'create-protocol-invalid';
    throw readyError;
  }
  lines.shift();
  if (lines.length !== 1) {
    var error = new Error('create helper did not emit exactly one JSON result');
    error.code = 'create-protocol-invalid';
    throw error;
  }
  var parsed;
  try { parsed = JSON.parse(lines[0]); }
  catch (error2) { var invalid = new Error('create helper returned malformed JSON'); invalid.code = 'create-protocol-invalid'; throw invalid; }
  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
    var shape = new Error('create helper result must be an object'); shape.code = 'create-protocol-invalid'; throw shape;
  }
  return parsed;
}

function exactKeys(value, keys) {
  return value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join(',') === keys.slice().sort().join(',');
}
function validTaskEntry(value, stem, column) {
  if (!value || typeof value !== 'object' || Array.isArray(value) || value.stem !== stem || typeof value.title !== 'string' || !value.title) return false;
  var allowed = ['stem', 'title', 'state', 'sourceRevision', 'createdAt', 'splitFrom', 'round', 'questionsCount', 'doneAt', 'outcomeStatus', 'dependsOn', 'origin'];
  if (Object.keys(value).some(function (key) { return allowed.indexOf(key) < 0; })) return false;
  if (value.state != null && value.state !== column) return false;
  if (value.sourceRevision != null && !HASH_RE.test(String(value.sourceRevision))) return false;
  if (value.origin != null && (!value.origin || typeof value.origin !== 'object' || Array.isArray(value.origin) ||
      Object.keys(value.origin).sort().join(',') !== 'fingerprint,kind,ref,type' ||
      !taskSource.validate(value.origin))) return false;
  if (value.splitFrom != null && !taskSource.safeTaskStem(value.splitFrom)) return false;
  if (value.dependsOn != null && (!Array.isArray(value.dependsOn) || value.dependsOn.some(function (dep) { return !taskSource.safeTaskStem(dep); }))) return false;
  if (column === 'pending' && (!Number.isInteger(value.round) || !Number.isInteger(value.questionsCount))) return false;
  if (column === 'done') return typeof value.doneAt === 'string';
  return typeof value.createdAt === 'string';
}
function validCreateSuccess(value) {
  var keys = ['ok', 'created', 'deduped', 'effect', 'stem', 'number', 'column', 'task', 'sourceHash', 'transactionId', 'replayed'];
  if (!exactKeys(value, keys) || value.ok !== true || typeof value.created !== 'boolean' || typeof value.deduped !== 'boolean' ||
      typeof value.replayed !== 'boolean' || !['created', 'domain-dedup'].includes(value.effect) ||
      !taskSource.safeTaskStem(value.stem) || !Number.isSafeInteger(value.number) || value.number < 1 ||
      !['backlog', 'pending', 'todo', 'done'].includes(value.column) || !HASH_RE.test(String(value.sourceHash || '')) ||
      !/^[a-f0-9]{32}$/.test(String(value.transactionId || '')) || !validTaskEntry(value.task, value.stem, value.column)) return false;
  var match = /^TASK_([0-9]+)_/.exec(value.stem);
  if (!match || match[1] !== String(value.number)) return false;
  if (value.effect === 'domain-dedup') return value.created === false && value.deduped === true;
  if (value.created) return value.column === 'backlog' && value.deduped === false && value.replayed === false;
  return value.deduped === true && value.replayed === true;
}
function validEditSuccess(value) {
  return exactKeys(value, ['ok', 'changed', 'stem', 'column', 'previousSourceHash', 'sourceHash', 'transactionId', 'recovered', 'task']) && value.ok === true &&
    typeof value.changed === 'boolean' && taskSource.safeTaskStem(value.stem) && value.column === 'backlog' &&
    HASH_RE.test(String(value.previousSourceHash || '')) && HASH_RE.test(String(value.sourceHash || '')) &&
    /^[a-f0-9]{32}$/.test(String(value.transactionId || '')) && value.recovered === false && validTaskEntry(value.task, value.stem, 'backlog') &&
    (value.changed ? value.previousSourceHash !== value.sourceHash : value.previousSourceHash === value.sourceHash);
}
function validEditRecoverySuccess(value) {
  if (!exactKeys(value, ['ok', 'mode', 'scanned', 'alreadyCompleted', 'recoveredCount', 'recovered']) || value.ok !== true ||
      value.mode !== 'recover-all' || !Number.isSafeInteger(value.scanned) || value.scanned < 0 ||
      !Number.isSafeInteger(value.alreadyCompleted) || value.alreadyCompleted < 0 || !Number.isSafeInteger(value.recoveredCount) ||
      value.recoveredCount < 0 || !Array.isArray(value.recovered) || value.recovered.length !== value.recoveredCount ||
      value.scanned !== value.alreadyCompleted + value.recoveredCount) return false;
  var seen = Object.create(null);
  return value.recovered.every(function (row) {
    if (!exactKeys(row, ['stem', 'sourceHash', 'changed', 'transactionId']) || !taskSource.safeTaskStem(row.stem) ||
        !HASH_RE.test(String(row.sourceHash || '')) || typeof row.changed !== 'boolean' ||
        !/^[a-f0-9]{32}$/.test(String(row.transactionId || '')) || seen[row.transactionId]) return false;
    seen[row.transactionId] = true; return true;
  });
}
function validRecoverySuccess(value) {
  if (!exactKeys(value, ['ok', 'mode', 'scanned', 'alreadyCompleted', 'recoveredCount', 'recovered']) || value.ok !== true ||
      value.mode !== 'recover-all' || !Number.isSafeInteger(value.scanned) || value.scanned < 0 ||
      !Number.isSafeInteger(value.alreadyCompleted) || value.alreadyCompleted < 0 || !Number.isSafeInteger(value.recoveredCount) ||
      value.recoveredCount < 0 || !Array.isArray(value.recovered) || value.recovered.length !== value.recoveredCount ||
      value.scanned !== value.alreadyCompleted + value.recoveredCount) return false;
  var seen = Object.create(null);
  return value.recovered.every(function (row) {
    if (!exactKeys(row, ['stem', 'number', 'effect', 'transactionId']) || !taskSource.safeTaskStem(row.stem) ||
        !Number.isSafeInteger(row.number) || row.number < 1 || !['created', 'domain-dedup'].includes(row.effect) ||
        !/^[a-f0-9]{32}$/.test(String(row.transactionId || '')) || seen[row.transactionId]) return false;
    seen[row.transactionId] = true;
    var match = /^TASK_([0-9]+)_/.exec(row.stem);
    return !!match && match[1] === String(row.number);
  });
}

function runHelper(input, mode) {
  mode = mode || {};
  var payload = null;
  if (mode.edit) {
    try { payload = validateEditInput(input); } catch (editError) { return Promise.reject(editError); }
  } else if (!mode.recoverAll && !mode.recoverEdits) {
    try { payload = validateInput(input); }
    catch (error) {
      appendCreationEvent('creation-failed', { errorCode: error.code || 'validation-failed', durationMs: 0 });
      return Promise.reject(error);
    }
  }

  var creationKeyHash = payload && !mode.edit
    ? 'sha256:' + crypto.createHash('sha256').update(payload.idempotencyKey, 'ascii').digest('hex')
    : null;
  var operationStarted = Date.now();
  if (!mode.edit && !mode.recoverAll && !mode.recoverEdits) appendCreationEvent('creation-started', { keyHash: creationKeyHash });
  var leaseStart = finalizations.beginMutation({
    kind: 'workspace-session',
    stem: mode.edit ? payload.stem : null,
    key: mode.recoverAll ? 'task:recover-backlog-creations' : (mode.recoverEdits ? 'task:recover-backlog-edits' : (mode.edit ? 'task:edit-backlog:' + payload.stem : 'task:create-backlog')),
    creationKeyHash: creationKeyHash,
    // A combined startup authority resolves creation first and edit second.
    // Both bypasses apply only to valid incomplete receipts; either contract's
    // corrupt state remains fail-closed in its blockingIssue implementation.
    allowAllCreationRecovery: mode.recoverAll === true || mode.recoverEdits === true,
    allowAllEditRecovery: mode.recoverAll === true || mode.recoverEdits === true,
    requireSoleWriter: true
  });
  if (!leaseStart.ok) {
    var blocked = new Error(leaseStart.detail || 'task publication is blocked');
    blocked.code = leaseStart.error || 'publication-blocked';
    blocked.httpStatus = 409;
    if (!mode.edit && !mode.recoverAll && !mode.recoverEdits) appendCreationEvent('creation-failed', {
      keyHash: creationKeyHash, errorCode: blocked.code, durationMs: Math.max(0, Date.now() - operationStarted)
    });
    return Promise.reject(blocked);
  }
  if (typeof mode.preflight === 'function') {
    try {
      if (mode.preflight(payload) !== true) {
        throw Object.assign(new Error('task publication preflight rejected the source'), {
          code: 'publication-preflight-rejected',
          httpStatus: 409
        });
      }
    } catch (preflightError) {
      if (!finalizations.endMutation(leaseStart.handle)) {
        preflightError = Object.assign(
          new Error('task publication preflight failed and its writer lease could not be released safely'),
          { code: 'writer-lease-release-failed', httpStatus: 503 }
        );
      }
      if (!mode.edit && !mode.recoverAll && !mode.recoverEdits) {
        appendCreationEvent('creation-failed', {
          keyHash: creationKeyHash,
          errorCode: preflightError.code || 'publication-preflight-failed',
          durationMs: Math.max(0, Date.now() - operationStarted)
        });
      }
      return Promise.reject(preflightError);
    }
  }

  return new Promise(function (resolve, reject) {
    var python = process.env.CREATE_BACKLOG_PYTHON || 'python3';
    var editMode = mode.edit || mode.recoverEdits;
    var script = editMode
      ? (process.env.EDIT_BACKLOG_SCRIPT || path.join(paths.ORCHESTRATOR_DIR, 'tasks', 'edit-backlog.py'))
      : (process.env.CREATE_BACKLOG_SCRIPT || path.join(paths.ORCHESTRATOR_DIR, 'tasks', 'create-backlog.py'));
    var child;
    var stdout = Buffer.alloc(0), stderr = Buffer.alloc(0);
    var settled = false, timedOut = false, outputExceeded = false, attached = false, closed = false;
    var forcedError = null;
    var timer;

    function finish(error, result) {
      if (settled) return;
      settled = true;
      if (timer) clearTimeout(timer);
      children.delete(child);
      if (!processTreeGone(child, closed)) {
        finalizations.retainMutation(leaseStart.handle, 'deterministic backlog helper exited but process-tree death is unproven');
        if (!error) {
          error = new Error('deterministic task result is durable, but the helper process tree is still observable; publication remains fail-closed');
          error.code = 'writer-tree-unverified';
          error.httpStatus = 503;
        }
      } else if (!finalizations.endMutation(leaseStart.handle) && !error) {
          error = new Error('deterministic task was published but its writer lease could not be released safely');
          error.code = 'writer-lease-release-failed';
          error.httpStatus = 503;
      }
      if (!mode.edit && !mode.recoverAll && !mode.recoverEdits) {
        appendCreationEvent(error ? 'creation-failed' : 'creation-completed', {
          keyHash: creationKeyHash,
          stem: result && result.stem || error && error.result && error.result.stem || null,
          errorCode: error ? String(error.code || 'create-failed').slice(0, 80) : null,
          durationMs: Math.max(0, Date.now() - operationStarted),
          replayed: result ? result.replayed === true : false
        });
      }
      if (error) reject(error); else resolve(result);
    }

    function finishAfterTreeProof(error, result) {
      if (settled) return;
      var deadline = Date.now() + TREE_VERIFY_MS;
      (function probe() {
        if (settled) return;
        if (processTreeGone(child, closed) || Date.now() >= deadline) {
          finish(error, result);
          return;
        }
        var verifyTimer = setTimeout(probe, TREE_POLL_MS);
        if (typeof verifyTimer.unref === 'function') verifyTimer.unref();
      })();
    }

    // Register every event listener before PID attachment. spawn() reports an
    // ENOENT asynchronously; attaching first left a window where that error was
    // unhandled and the promise/lease remained pending forever.
    function registerChildHandlers() {
      child.stdin.on('error', function (error) {
        if (settled) return;
        error.code = 'create-stdin-failed';
        forcedError = forcedError || error;
        killTree(child, 'SIGKILL');
      });
      child.stdout.on('data', function (chunk) {
        if (stdout.length + chunk.length > OUTPUT_MAX_BYTES) {
          outputExceeded = true;
          killTree(child, 'SIGKILL');
          return;
        }
        stdout = Buffer.concat([stdout, chunk]);
      });
      child.stderr.on('data', function (chunk) {
        if (stderr.length >= STDERR_MAX_BYTES) return;
        stderr = Buffer.concat([stderr, chunk.slice(0, STDERR_MAX_BYTES - stderr.length)]);
      });
      child.on('error', function (error) {
        error.code = error.code || 'create-spawn-failed';
        forcedError = forcedError || error;
        if (!child.pid) finish(error);
      });
      child.on('close', function (code, signal) {
        if (settled) return;
        closed = true;
        if (forcedError) { finishAfterTreeProof(forcedError); return; }
        if (timedOut || outputExceeded) {
          var boundedError = new Error(outputExceeded
            ? 'deterministic backlog helper exceeded its output limit'
            : 'deterministic backlog creation timed out');
          boundedError.code = outputExceeded ? 'CREATE_OUTPUT_TOO_LARGE' : 'CREATE_TIMEOUT';
          boundedError.httpStatus = 500;
          finishAfterTreeProof(boundedError);
          return;
        }
        var result;
        try { result = parseProtocol(stdout.toString('utf8')); }
        catch (protocolError) { finishAfterTreeProof(protocolError); return; }
        if (code !== 0) {
          var helperError = result && result.error && typeof result.error === 'object' ? result.error : null;
          var message = helperError && helperError.message ? String(helperError.message) : stderr.toString('utf8').trim();
          var error = new Error(message || 'deterministic backlog creation failed');
          error.code = (helperError && helperError.code) || (result && result.code) || 'CREATE_FAILED';
          if (error.code === 'IDEMPOTENCY_CONFLICT' || error.code === 'DEDUP_CONFLICT' || error.code === 'SOURCE_CHANGED' ||
              error.code === 'TASK_NOT_IDLE_BACKLOG' || error.code === 'EDIT_RECOVERY_REQUIRED' || error.code === 'WORKSPACE_WRITER_ACTIVE') error.httpStatus = 409;
          else if (/^EDIT_WRITER_/.test(error.code) || error.code === 'EDIT_RECOVERY_SCAN_FAILED') error.httpStatus = 503;
          else if (code === 2 && result && result.recoverable !== true) error.httpStatus = 400;
          else error.httpStatus = 500;
          error.result = result;
          finishAfterTreeProof(error);
          return;
        }
        var validSuccess = mode.recoverAll ? validRecoverySuccess(result) :
          (mode.recoverEdits ? validEditRecoverySuccess(result) : (mode.edit ? validEditSuccess(result) : validCreateSuccess(result)));
        if (!validSuccess) {
          var invalid = new Error('create helper success result is incomplete');
          invalid.code = 'create-protocol-invalid';
          finishAfterTreeProof(invalid);
          return;
        }
        finishAfterTreeProof(null, result);
      });
    }

    var helperEnvironment = Object.assign(childEnv(), {
      CREATE_BACKLOG_PROJECT_ROOT: paths.PROJECT_ROOT,
      CREATE_BACKLOG_AUTHORITY_ROOT: paths.PROJECT_ROOT,
      CREATE_BACKLOG_TASKS_DIR: paths.TASKS_DIR,
      CREATE_BACKLOG_CACHE_DIR: path.dirname(paths.TASK_CREATIONS_DIR),
      CREATE_BACKLOG_FINALIZATIONS_DIR: paths.FINALIZATIONS_DIR,
      CREATE_BACKLOG_OWN_WRITER_LEASE_ID: leaseStart.handle.leaseId,
      CREATE_BACKLOG_NODE: process.execPath,
      EDIT_BACKLOG_EDITS_DIR: paths.TASK_EDITS_DIR,
      EDIT_BACKLOG_FINALIZATIONS_DIR: paths.FINALIZATIONS_DIR,
      EDIT_BACKLOG_OWN_WRITER_LEASE_ID: leaseStart.handle.leaseId,
      EDIT_BACKLOG_NODE: process.execPath
    });
    // These values are minted by the helper for each exact validator child.
    // Never inherit a shell/controller context into an unrelated publication.
    delete helperEnvironment.ORCHESTRATOR_ACTIVE_CREATION_CONTEXT;
    delete helperEnvironment.ORCHESTRATOR_ACTIVE_EDIT_CONTEXT;
    delete helperEnvironment.ORCHESTRATOR_COMBINED_PUBLICATION_RECOVERY;
    if (mode.combinedPublicationRecovery === true) {
      helperEnvironment.ORCHESTRATOR_COMBINED_PUBLICATION_RECOVERY = '1';
    }
    try {
      child = cp.spawn(python, [script].concat(mode.recoverAll || mode.recoverEdits ? ['--recover-all'] : []), {
        cwd: paths.PROJECT_ROOT,
        env: helperEnvironment,
        detached: process.platform !== 'win32',
        stdio: ['pipe', 'pipe', 'pipe']
      });
    } catch (spawnError) {
      spawnError.code = spawnError.code || 'create-spawn-failed';
      finish(spawnError);
      return;
    }
    children.add(child);
    registerChildHandlers();

    // spawn() reports ENOENT/EACCES asynchronously and returns a ChildProcess
    // without a PID. There is no process tree to attach in that case; calling
    // attachMutationChild(undefined) fabricates a 503 attach failure and masks
    // the real 500 spawn error. The already-registered one-shot error handler
    // calls finish(), which proves no PID, withdraws this exact lease and
    // settles the request once.
    if (!child.pid) return;

    var attachedResult = finalizations.attachMutationChild(leaseStart.handle, child.pid);
    if (!attachedResult.ok) {
      // A tiny helper can exit between spawn() and process-start capture. If
      // its detached group is already provably gone, there is no live tree to
      // attach or kill; let the registered close handler classify its actual
      // protocol result and withdraw the still-pending exact lease. Every
      // unproved/live attach failure remains a 503 below.
      if (processTreeGone(child, closed)) return;
      finalizations.retainMutation(leaseStart.handle, 'deterministic backlog helper could not be attached; process-tree death is unproven');
      killTree(child, 'SIGKILL');
      var attachError = new Error(attachedResult.detail || 'could not bind deterministic backlog helper');
      attachError.code = attachedResult.error || 'writer-lease-update-failed';
      attachError.httpStatus = 503;
      forcedError = attachError;
      return;
    }
    attached = true;
    var protocolPayload = mode.edit ? {
      version: 1,
      stem: payload.stem,
      expectedSourceHash: payload.expectedSourceHash,
      markdown: payload.markdown
    } : payload ? {
      version: 1,
      title: payload.title,
      body: payload.body,
      source: payload.source,
      key: payload.idempotencyKey,
      originStem: payload.originStem,
      dedupKey: payload.dedupKey,
      dedupReport: payload.dedupReport
    } : null;
    try { child.stdin.end(protocolPayload ? JSON.stringify(protocolPayload) + '\n' : ''); }
    catch (stdinError) {
      forcedError = stdinError;
      forcedError.code = 'create-stdin-failed';
      killTree(child, 'SIGKILL');
    }

    timer = setTimeout(function () {
      timedOut = true;
      killTree(child, 'SIGTERM');
      setTimeout(function () { if (!settled) killTree(child, 'SIGKILL'); }, 1000).unref();
    }, TIMEOUT_MS);
    if (typeof timer.unref === 'function') timer.unref();
  });
}

function create(input, options) {
  options = options || {};
  if (!options || typeof options !== 'object' || Array.isArray(options) ||
      Object.keys(options).some(function (key) { return key !== 'preflight'; }) ||
      options.preflight !== undefined && typeof options.preflight !== 'function') {
    return Promise.reject(validationError('bad-create-options'));
  }
  return runHelper(input, {
    recoverAll: false,
    preflight: options.preflight
  });
}
function recoverAllInternal(combined) {
  combined = combined === true;
  if (recoveryPromise) {
    if (recoveryCombined === combined) return recoveryPromise;
    // A standalone and a combined caller require different child authority.
    // Serialize the exact modes rather than accidentally reusing a helper that
    // lacks (or unexpectedly carries) the private combined recovery request.
    return recoveryPromise.then(function () { return recoverAllInternal(combined); },
      function () { return recoverAllInternal(combined); });
  }
  recoveryCombined = combined;
  recoveryPromise = runHelper(null, { recoverAll: true, combinedPublicationRecovery: combined }).finally(function () {
    recoveryPromise = null; recoveryCombined = null;
  });
  return recoveryPromise;
}
function recoverAll() { return recoverAllInternal(false); }
function edit(input) { return runHelper(input, { edit: true }); }
function recoverEdits() {
  if (editRecoveryPromise) return editRecoveryPromise;
  editRecoveryPromise = runHelper(null, { recoverEdits: true }).finally(function () { editRecoveryPromise = null; });
  return editRecoveryPromise;
}
function recoverPublications() {
  if (publicationRecoveryPromise) return publicationRecoveryPromise;
  // Create recovery must run first: the edit helper's durable contract rejects
  // every incomplete creation before entering its publication loop. During
  // this phase only exact, valid, disjoint edit receipts are projected as
  // queued; the second phase then recovers them under its ordinary authority.
  publicationRecoveryPromise = recoverAllInternal(true).then(function (creations) {
    return recoverEdits().then(function (edits) { return { creations: creations, edits: edits }; });
  }).finally(function () { publicationRecoveryPromise = null; });
  return publicationRecoveryPromise;
}

function pathStamp(file) {
  try {
    var st = fs.lstatSync(file);
    return [st.isSymbolicLink() ? 'link' : (st.isDirectory() ? 'dir' : (st.isFile() ? 'file' : 'other')),
      st.dev, st.ino, st.size, st.mtimeMs].join(':');
  } catch (error) { return error && error.code === 'ENOENT' ? 'missing' : 'unreadable'; }
}
function directoryStamp(dir) {
  var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, dir, 2048);
  if (!listed.ok) return 'unavailable:' + String(listed.code || 'directory-unsafe');
  var names = listed.names.sort();
  return pathStamp(dir) + '|' + names.map(function (name) {
    return name + '=' + pathStamp(path.join(dir, name));
  }).join('|');
}
function recoverySignature(creationState, editState, writerState) {
  var taskStems = Object.create(null);
  (creationState.incomplete || []).concat(editState.incomplete || []).forEach(function (row) {
    if (row && row.stem) taskStems[row.stem] = true;
  });
  var leases = writerState;
  if (!leases) {
    try { leases = writerLeases.scan(paths.WRITER_LEASES_DIR, paths.WRITER_AUTHORITY_ROOT); }
    catch (error) { leases = { active: [], stale: [], issues: [{ code: 'WRITER_LEASE_SCAN_FAILED' }] }; }
  }
  var state = {
    creation: creationState,
    edit: editState,
    creationFiles: directoryStamp(paths.TASK_CREATIONS_DIR),
    editFiles: directoryStamp(paths.TASK_EDITS_DIR),
    index: pathStamp(path.join(paths.TASKS_DIR, 'INDEX.json')),
    finalizations: directoryStamp(paths.FINALIZATIONS_DIR),
    writerLeases: leases,
    tasks: Object.keys(taskStems).sort().map(function (stem) {
      return [stem, pathStamp(path.join(paths.TASKS_DIR, 'backlog', stem + '.md'))];
    })
  };
  return crypto.createHash('sha256').update(JSON.stringify(state)).digest('hex');
}
function recoverablePublicationState(creationState, editState) {
  var pending = creationState.incomplete.length || editState.incomplete.length;
  return !!pending && creationState.blocking.every(function (row) { return row.code === 'CREATION_INCOMPLETE'; }) &&
    editState.blocking.every(function (row) { return row.code === 'EDIT_INCOMPLETE'; });
}
function recoveryGeneration(creationState, editState) {
  return JSON.stringify({
    creation: creationState.incomplete.map(function (row) { return [row.keyHash || '', row.transactionId || '']; }).sort(),
    edit: editState.incomplete.map(function (row) { return [row.stem || '', row.transactionId || '']; }).sort()
  });
}
function createRecoveryController(options) {
  options = options || {};
  var creationScan = options.creationScan || require('./creation-markers').scan;
  var editScan = options.editScan || require('./edit-markers').scan;
  var recover = options.recover || recoverPublications;
  var writerReconcile = options.writerReconcile || function () {
    return writerLeases.reconcileStaleMutations(paths.WRITER_LEASES_DIR, paths.WRITER_AUTHORITY_ROOT);
  };
  var writerScan = options.writerScan || function () {
    return writerLeases.scan(paths.WRITER_LEASES_DIR, paths.WRITER_AUTHORITY_ROOT);
  };
  var onWriterReconciled = options.onWriterReconciled || function () {};
  var onRecovered = options.onRecovered || function () {};
  var onError = options.onError || function () {};
  var onAttemptSettled = options.onAttemptSettled || function () {};
  var intervalMs = Number(options.intervalMs || process.env.PUBLICATION_RECOVERY_POLL_MS || 10000);
  intervalMs = Number.isFinite(intervalMs) ? Math.max(25, Math.min(60000, Math.floor(intervalMs))) : 10000;
  var retryMs = Number(options.retryMs || process.env.PUBLICATION_RECOVERY_RETRY_MS || 30000);
  retryMs = Number.isFinite(retryMs) ? Math.max(25, Math.min(10 * 60 * 1000, Math.floor(retryMs))) : 30000;
  var retryMaxMs = Number(options.retryMaxMs || process.env.PUBLICATION_RECOVERY_RETRY_MAX_MS || 5 * 60 * 1000);
  retryMaxMs = Number.isFinite(retryMaxMs) ? Math.max(retryMs, Math.min(60 * 60 * 1000, Math.floor(retryMaxMs))) : 5 * 60 * 1000;
  var retryDelay = retryMs;
  var lastSignature = null, retryAt = 0, writerRetryAt = 0, writerRetryDelay = retryMs;
  var inFlight = null, timer = null, stopped = false, attemptSequence = 0;

  function settled(sequence, ok, code, detail) {
    var outcome = {
      version: 1,
      sequence: sequence,
      ok: ok === true,
      code: ok === true ? null : String(code || 'PUBLICATION_RECOVERY_FAILED').slice(0, 100),
      detail: ok === true ? null : String(detail || 'Startup publication recovery did not settle safely.').slice(0, 500)
    };
    try { onAttemptSettled(outcome); }
    catch (callbackError) {
      console.error('[site] publication recovery settlement callback failed:', callbackError && callbackError.message || callbackError);
    }
    return outcome;
  }

  function reportFailure(sequence, error) {
    try { onError(error); } catch (callbackError) {}
    settled(sequence, false, error && error.code, error && error.message || error);
    return Promise.resolve(null);
  }

  function unsafePublicationIssue(creationState, editState) {
    var rows = (creationState && Array.isArray(creationState.blocking) ? creationState.blocking : [])
      .concat(editState && Array.isArray(editState.blocking) ? editState.blocking : []);
    var unsafe = rows.filter(function (row) {
      return row && row.code !== 'CREATION_INCOMPLETE' && row.code !== 'EDIT_INCOMPLETE';
    });
    if (!unsafe.length) return null;
    var error = new Error('publication recovery contains unsafe or malformed durable state: ' + unsafe.map(function (row) {
      return String(row.code || 'PUBLICATION_RECOVERY_UNSAFE') + (row.stem ? ' (' + row.stem + ')' : '');
    }).join('; '));
    error.code = 'PUBLICATION_RECOVERY_UNSAFE';
    return error;
  }

  function attempt(force) {
    if (stopped) return Promise.resolve(null);
    if (inFlight) return inFlight;
    var sequence = ++attemptSequence;
    if (force || Date.now() >= writerRetryAt) {
      try {
        var writerResult = writerReconcile();
        if (!writerResult || !Array.isArray(writerResult.reconciled) || !Array.isArray(writerResult.blocked)) {
          throw new Error('writer lease reconciliation returned an invalid result');
        }
        if (writerResult.blocked.length) {
          var writerError = new Error('writer lease reconciliation remains blocked: ' +
            writerResult.blocked.map(function (row) { return row.leaseId + ' (' + row.message + ')'; }).join('; '));
          writerError.code = 'WRITER_LEASE_RECOVERY_BLOCKED';
          throw writerError;
        }
        writerRetryAt = 0;
        writerRetryDelay = retryMs;
        if (writerResult.reconciled.length) onWriterReconciled(writerResult);
      } catch (writerRecoveryError) {
        writerRetryAt = Date.now() + writerRetryDelay;
        writerRetryDelay = Math.min(retryMaxMs, writerRetryDelay * 2);
        return reportFailure(sequence, writerRecoveryError);
      }
    } else return Promise.resolve(null);
    var creationState, editState, writerState;
    try {
      creationState = creationScan();
      editState = editScan();
      writerState = writerScan();
      if (!writerState || !Array.isArray(writerState.active) ||
          !Array.isArray(writerState.stale) || !Array.isArray(writerState.issues) ||
          writerState.issues.length) {
        var writerScanError = new Error('writer lease recovery scan returned unsafe state');
        writerScanError.code = 'WRITER_LEASE_RECOVERY_BLOCKED';
        throw writerScanError;
      }
    } catch (scanError) {
      if (!scanError.code) scanError.code = 'PUBLICATION_RECOVERY_SCAN_FAILED';
      return reportFailure(sequence, scanError);
    }
    var unsafeIssue = unsafePublicationIssue(creationState, editState);
    if (unsafeIssue) return reportFailure(sequence, unsafeIssue);
    var signature = recoverySignature(creationState, editState, writerState);
    var stateChanged = signature !== lastSignature;
    if (!recoverablePublicationState(creationState, editState)) {
      lastSignature = signature;
      settled(sequence, true, null, null);
      return Promise.resolve(null);
    }
    // A still-incomplete WAL inside its bounded retry window is not a clean
    // startup verdict. Preserve the preceding blocked outcome until the exact
    // generation is retried; never transiently tell the runner that recovery
    // completed merely because backoff is active.
    if (!force && signature === lastSignature && Date.now() < retryAt) return Promise.resolve(null);
    if (!force && signature === lastSignature && retryAt === 0) {
      var incompleteError = new Error('publication recovery returned without settling its exact durable generation');
      incompleteError.code = 'PUBLICATION_RECOVERY_INCOMPLETE';
      retryAt = Date.now() + retryDelay;
      retryDelay = Math.min(retryMaxMs, retryDelay * 2);
      return reportFailure(sequence, incompleteError);
    }
    if (stateChanged) retryDelay = retryMs;
    lastSignature = signature;
    var generation = recoveryGeneration(creationState, editState);
    // An incomplete publication marker can be the ordinary in-flight state of
    // the exact live publisher. Starting recovery beside that writer is not
    // useful (the recovery helper must refuse it) and, more importantly, the
    // transient recovery lease can invalidate the publisher's final composite
    // runtime snapshot after its task and INDEX are already durable. Wait for
    // the live owner to settle, then recover only if the WAL remains.
    if (writerState.active.length) {
      retryAt = Date.now() + retryDelay;
      retryDelay = Math.min(retryMaxMs, retryDelay * 2);
      settled(sequence, false, 'finalization-active',
        'publication recovery is waiting for the active workspace writer to settle');
      return Promise.resolve(null);
    }
    inFlight = Promise.resolve().then(recover).then(function (result) {
      retryAt = 0;
      retryDelay = retryMs;
      onRecovered(result);
      settled(sequence, true, null, null);
      return result;
    }).catch(function (error) {
      // A valid WAL has no safe manual bypass. Retry every controller/runtime
      // failure at bounded exponential intervals even when no tracked file
      // changes (missing helper, transient I/O, timeout, or writer contention).
      // Corrupt marker sets never enter this branch: scanner eligibility above
      // keeps them permanently fail-closed and visible for repair.
      retryAt = Date.now() + retryDelay;
      retryDelay = Math.min(retryMaxMs, retryDelay * 2);
      try { onError(error); } catch (callbackError) {}
      settled(sequence, false, error && error.code, error && error.message || error);
      return null;
    }).finally(function () {
      // Record the settled, post-attempt state so lastError/mtime writes made by
      // the failed helper do not form a hot retry loop. A genuinely new WAL
      // generation is not suppressed and will be picked up by the next poll.
      try {
        var afterCreation = creationScan(), afterEdit = editScan();
        lastSignature = recoveryGeneration(afterCreation, afterEdit) === generation
          ? recoverySignature(afterCreation, afterEdit, writerScan()) : null;
      } catch (error) { lastSignature = null; }
      inFlight = null;
    });
    return inFlight;
  }
  function arm() {
    if (stopped || timer) return;
    timer = setInterval(function () { attempt(false); }, intervalMs);
    if (typeof timer.unref === 'function') timer.unref();
  }
  function start() { return attempt(true).finally(arm); }
  function stop() { stopped = true; if (timer) clearInterval(timer); timer = null; }
  return { start: start, poll: function () { return attempt(false); }, stop: stop };
}

function killAll() {
  children.forEach(function (child) { killTree(child, 'SIGTERM'); });
  setTimeout(function () { children.forEach(function (child) { killTree(child, 'SIGKILL'); }); }, 1000).unref();
}

module.exports = {
  TASK_STORAGE_MAX_BYTES: TASK_STORAGE_MAX_BYTES,
  validateInput: validateInput,
  validateEditInput: validateEditInput,
  create: create,
  edit: edit,
  recoverPublications: recoverPublications,
  createRecoveryController: createRecoveryController,
  _appendCreationEvent: appendCreationEvent,
  publicCreateErrorCode: publicCreateErrorCode,
  publicEditErrorCode: publicEditErrorCode,
  killAll: killAll
};
