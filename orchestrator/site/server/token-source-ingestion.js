'use strict';

// Startup/post-finalization reconciler for committed task token receipts.
// It never calls Figma. Each intent is revalidated against its immutable
// committed receipt, rebased by the trusted runner onto the latest compatible
// token source set, and published through figma-sync's pointer-last writer.

var crypto = require('crypto');
var path = require('path');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var generation = require('./figma-generation');
var tokenJobs = require('./figma-token-jobs');
var health = require('./token-source-health-store');
var screenPlans = require('./screen-token-plans');
var projectIdentity = require(path.join(paths.ORCHESTRATOR_DIR, 'figma', 'runtime', 'project-identity.cjs'));
var pathToFileURL = require('url').pathToFileURL;

var ROOT = path.join(paths.FIGMA_CACHE_DIR, 'token-source-ingestion');
var STAGES_ROOT = path.join(paths.FIGMA_CACHE_DIR, 'token-source-ingestion-stages');
var RECEIPTS_ROOT = path.join(paths.TASKS_DIR, 'evidence', 'figma-ship');
var INTENT_RE = /^tokintent_[A-Za-z0-9_-]{16,96}$/;
var FILE_RE = /^(tokintent_[A-Za-z0-9_-]{16,96})\.json$/;
var MAX_INTENTS = 1024;
var INTENT_MAX = 1024 * 1024;
var CLEANUP_MAX = 4096;
var receiptModulePromise = null;
var intentModulePromise = null;
var errorCodesModulePromise = null;
var running = null;

function modules() {
  if (!receiptModulePromise) receiptModulePromise = import(pathToFileURL(path.join(
    paths.ORCHESTRATOR_DIR, 'figma', 'tokens', 'task-observation-receipt.mjs')).href);
  if (!intentModulePromise) intentModulePromise = import(pathToFileURL(path.join(
    paths.ORCHESTRATOR_DIR, 'figma', 'tokens', 'task-ingestion-intent.mjs')).href);
  if (!errorCodesModulePromise) errorCodesModulePromise = import(pathToFileURL(path.join(
    paths.ORCHESTRATOR_DIR, 'figma', 'tokens', 'error-codes.mjs')).href);
  return Promise.all([receiptModulePromise, intentModulePromise, errorCodesModulePromise]);
}
function exact(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}
function intentFile(intentId) {
  if (!INTENT_RE.test(String(intentId || ''))) throw new Error('TOKEN_SOURCE_INGESTION_INTENT_INVALID');
  return path.join(ROOT, intentId + '.json');
}
function readRecord(intentId) {
  var file = intentFile(intentId);
  var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, ROOT, file, INTENT_MAX);
  if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') throw new Error('TOKEN_SOURCE_INGESTION_INTENT_UNSAFE');
  var value;
  try { value = JSON.parse(hit.bytes.toString('utf8')); } catch (error) {
    throw new Error('TOKEN_SOURCE_INGESTION_INTENT_INVALID');
  }
  return { file: file, bytes: hit.bytes, proof: hit.stat, value: value };
}
function transition(record, state, extras) {
  var next = JSON.parse(JSON.stringify(record.value));
  next.state = state;
  delete next.resultGenerationId;
  delete next.errorCode;
  Object.keys(extras || {}).forEach(function (key) { next[key] = extras[key]; });
  var bytes = Buffer.from(JSON.stringify(next, null, 2) + '\n');
  return modules().then(function (loaded) {
    loaded[1].validateTaskIngestionIntent(next);
    var result = fileGuards.compareAndSwapRegularFileUnder(
      paths.PROJECT_ROOT, ROOT, record.file, INTENT_MAX,
      { proof: record.proof, bytes: record.bytes }, bytes, { mode: 0o600 }
    );
    if (!result.ok) throw new Error('TOKEN_SOURCE_INGESTION_INTENT_CONFLICT');
    return readRecord(next.intentId);
  });
}
function quiescent(intent) {
  var marker = path.join(paths.FINALIZATIONS_DIR, intent.taskStem + '.json');
  var lock = path.join(paths.LOCKS_DIR, intent.taskStem + '.json');
  var markerState = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, paths.FINALIZATIONS_DIR, marker);
  var lockState = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, paths.LOCKS_DIR, lock);
  if (!markerState || markerState.status === 'unsafe' || !lockState || lockState.status === 'unsafe') {
    throw new Error('TOKEN_SOURCE_INGESTION_QUIESCENCE_UNSAFE');
  }
  return markerState.status === 'missing' && lockState.status === 'missing';
}
function receiptDirectory(intent) {
  var expected = 'orchestrator/tasks/evidence/figma-ship/' + intent.taskStem + '/token-observations-manifest.json';
  if (intent.receiptManifestPath !== expected) throw new Error('TOKEN_SOURCE_INGESTION_INTENT_INVALID');
  return path.join(RECEIPTS_ROOT, intent.taskStem);
}
function reservations(intent, sourceIds) {
  var selected = Object.create(null);
  sourceIds.forEach(function (sourceId) { selected[sourceId] = 1; });
  return intent.sources.filter(function (row) { return selected[row.sourceId]; }).map(function (row) {
    return {
      sourceId: row.sourceId,
      captureOperationId: row.captureOperationId,
      captureSequence: row.captureSequence
    };
  });
}
function settleHealth(intent, result, generationId, outcome) {
  var accepted = reservations(intent, result.acceptedSources || []);
  var superseded = reservations(intent, result.supersededSources || []);
  var evidenceBySource = Object.create(null);
  (result.healthEvidence || []).forEach(function (row) { evidenceBySource[row.sourceId] = row; });
  function evidenceFor(rows) {
    return rows.map(function (reservation) {
      var evidence = evidenceBySource[reservation.sourceId];
      if (!evidence || evidence.captureOperationId !== reservation.captureOperationId ||
          evidence.captureSequence !== reservation.captureSequence) {
        throw new Error('TOKEN_SOURCE_HEALTH_EVIDENCE_INVALID');
      }
      return evidence;
    });
  }
  var common = {
    sourceIndexHash: result.sourceIndexHash,
    jobId: 'fsj-' + crypto.createHash('sha256').update(intent.intentId).digest('hex').slice(0, 32),
    action: 'task-ingestion',
    startedAt: new Date().toISOString()
  };
  if (accepted.length) health.complete(Object.assign({}, common, {
    reservations: accepted,
    evidenceSources: evidenceFor(accepted),
    outcome: outcome,
    summaryOutcome: outcome,
  }));
  if (superseded.length) health.complete(Object.assign({}, common, {
    reservations: superseded,
    evidenceSources: evidenceFor(superseded),
    outcome: 'no-op',
    summaryOutcome: 'superseded',
  }));
  screenPlans.consumeReceipt(intent.taskStem, intent.sources);
  return generationId;
}
function activeSourceIndexHash() {
  var active = generation.current();
  if (!active.ok || active.mode !== 'generation') throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED');
  var entry = active.manifest.artifacts.find(function (row) { return row.role === 'observed-token-source-index'; });
  var bytes = entry && generation.readEntry(entry);
  var value;
  try { value = bytes && JSON.parse(bytes.toString('utf8')); } catch (error) {
    throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED');
  }
  if (!value || !generation.HASH_RE.test(String(value.semanticHash || ''))) {
    throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED');
  }
  return value.semanticHash;
}
function cleanupTree(directory, parent, budget) {
  if (budget.count >= CLEANUP_MAX) return false;
  var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, directory, CLEANUP_MAX - budget.count);
  if (!listed.ok) return false;
  for (var i = 0; i < listed.names.length; i++) {
    budget.count++;
    var target = path.join(directory, listed.names[i]);
    var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, directory, target);
    if (!inspected || inspected.status !== 'present' || !inspected.stat) return false;
    if (inspected.stat.isFile() && !inspected.stat.isSymbolicLink()) {
      if (!fileGuards.unlinkRegularFileUnder(paths.PROJECT_ROOT, directory, target, { allowMissing: true })) return false;
    } else if (inspected.stat.isDirectory() && !inspected.stat.isSymbolicLink()) {
      if (!cleanupTree(target, directory, budget) ||
          !fileGuards.removeEmptyDirectoryUnder(paths.PROJECT_ROOT, directory, target)) return false;
    } else return false;
  }
  if (parent) return true;
  return true;
}
function cleanupStage(stageDir) {
  var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, STAGES_ROOT, stageDir);
  if (inspected && inspected.status === 'missing') return true;
  if (!inspected || inspected.status !== 'present' || !inspected.stat ||
      !inspected.stat.isDirectory() || inspected.stat.isSymbolicLink()) return false;
  return cleanupTree(stageDir, null, { count: 0 }) &&
    fileGuards.removeEmptyDirectoryUnder(paths.PROJECT_ROOT, STAGES_ROOT, stageDir);
}
function cleanupAbandonedStages() {
  var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, STAGES_ROOT, MAX_INTENTS);
  if (!listed.ok) throw new Error('TOKEN_SOURCE_INGESTION_STAGE_DIRECTORY_UNSAFE');
  listed.names.forEach(function (name) {
    if (!/^tokintent_[A-Za-z0-9_-]{16,96}-[a-f0-9]{16}$/.test(name) ||
        !cleanupStage(path.join(STAGES_ROOT, name))) {
      throw new Error('TOKEN_SOURCE_INGESTION_STAGE_RECOVERY_REQUIRED');
    }
  });
}
function retryableCode(error, errorCodesModule) {
  var raw = String(error && (error.tokenCode || error.message) || '');
  var codes = errorCodesModule && errorCodesModule.TOKEN_ERROR_CODES;
  return codes && Object.prototype.hasOwnProperty.call(codes, raw) && codes[raw] === raw
    ? raw : 'TOKEN_TASK_INGESTION_FAILED';
}
function processRecord(record, callbacks) {
  return modules().then(function (loaded) {
    var receiptApi = loaded[0], intentApi = loaded[1];
    intentApi.validateTaskIngestionIntent(record.value);
    if (['published', 'reconciled', 'superseded', 'failed-terminal'].indexOf(record.value.state) >= 0) {
      return { state: record.value.state, skipped: true };
    }
    if (!quiescent(record.value)) return { state: record.value.state, deferred: true };
    if (record.value.state === 'failed-retryable' &&
        record.value.errorCode === 'TOKEN_SOURCE_SCOPE_MISMATCH') {
      var scopeActive = generation.current();
      if (!scopeActive.ok || scopeActive.mode !== 'generation' ||
          scopeActive.manifest.fileKeyFingerprint !== record.value.scope.fileKeyFingerprint ||
          projectIdentity.projectBranchKey(paths.PROJECT_ROOT) !== record.value.scope.branchKey) {
        return { state: record.value.state, deferred: true, error: record.value.errorCode };
      }
    }
    var receipt = receiptApi.validateCommittedTaskObservationReceipt({
      taskStem: record.value.taskStem,
      transactionId: record.value.originTransactionId,
      receiptDirectory: receiptDirectory(record.value),
      expectedManifestHash: record.value.receiptManifestHash
    });
    if (receipt.scope.fileKeyFingerprint !== record.value.scope.fileKeyFingerprint ||
        receipt.scope.branchKey !== record.value.scope.branchKey) {
      throw new Error('TOKEN_SOURCE_SCOPE_CHANGED');
    }
    var active = generation.current();
    if (!active.ok || active.mode !== 'generation' ||
        active.manifest.fileKeyFingerprint !== record.value.scope.fileKeyFingerprint ||
        projectIdentity.projectBranchKey(paths.PROJECT_ROOT) !== record.value.scope.branchKey) {
      var scopeError = new Error('TOKEN_SOURCE_SCOPE_MISMATCH');
      throw scopeError;
    }
    return transition(record, 'processing').then(function (processing) {
      var stageId = processing.value.intentId + '-' + crypto.randomBytes(8).toString('hex');
      var stageDir = path.join(STAGES_ROOT, stageId);
      return Promise.resolve().then(function () {
        return tokenJobs.executeTaskTokenIngestion({
          intent: processing.value,
          sidecars: receipt.sidecars.map(function (row) { return { basename: row.basename, bytes: row.bytes }; }),
          stageDir: stageDir
        });
      }).then(function (result) {
        if (result.noOp) {
          var finalState = result.acceptedSources.length ? 'reconciled' : 'superseded';
          settleHealth(processing.value, result, result.active.manifest.generationId, 'no-op');
          return transition(processing, finalState, { resultGenerationId: result.active.manifest.generationId });
        }
        var jobId = 'fsj-' + crypto.createHash('sha256').update(processing.value.intentId).digest('hex').slice(0, 32);
        var preparedManifestHash = result.active.pointer.manifestHash;
        return callbacks.publishDomains({
          id: jobId,
          accountFingerprint: result.active.manifest.accountFingerprint,
          fileKeyFingerprint: processing.value.scope.fileKeyFingerprint,
          inputFingerprint: processing.value.receiptManifestHash,
          completed: [result.domainResult],
          verifyInputs: function () {
            var current = generation.current();
            if (!current.ok || current.mode !== 'generation' ||
                current.pointer.manifestHash !== preparedManifestHash ||
                current.manifest.fileKeyFingerprint !== processing.value.scope.fileKeyFingerprint ||
                projectIdentity.projectBranchKey(paths.PROJECT_ROOT) !== processing.value.scope.branchKey) return false;
            try {
              var latest = readRecord(processing.value.intentId);
              return latest.value.state === 'processing' &&
                latest.value.receiptManifestHash === processing.value.receiptManifestHash;
            } catch (error) { return false; }
          }
        }).then(function (generationId) {
          settleHealth(processing.value, result, generationId, 'published');
          return transition(processing, 'published', { resultGenerationId: generationId }).then(function (published) {
            return Promise.resolve(callbacks.requestDriftComparison('task-token-ingestion')).then(function () {
              return published;
            }, function () { return published; });
          });
        });
      }).finally(function () {
        if (!cleanupStage(stageDir)) console.error('[token-ingestion] stage cleanup could not be verified:', stageDir);
      });
    });
  }).then(function (result) {
    return result && result.value ? { state: result.value.state, intentId: result.value.intentId } : result;
  }).catch(function (error) {
    var terminal = error && error.terminal === true;
    return modules().then(function (loaded) {
      var code = retryableCode(error, loaded[2]);
      var latest;
      try { latest = readRecord(record.value.intentId); } catch (readError) { throw error; }
      if (['published', 'reconciled', 'superseded', 'failed-terminal'].indexOf(latest.value.state) >= 0) throw error;
      return transition(latest, terminal ? 'failed-terminal' : 'failed-retryable', { errorCode: code }).then(function () {
        health.complete({
          sourceIndexHash: activeSourceIndexHash(),
          reservations: reservations(record.value, record.value.sources.map(function (row) { return row.sourceId; })),
          outcome: 'failed',
          errorCode: code,
          retryable: !terminal,
          jobId: 'fsj-' + crypto.createHash('sha256').update(record.value.intentId).digest('hex').slice(0, 32),
          action: 'task-ingestion',
          startedAt: new Date().toISOString(),
          summaryOutcome: 'failed'
        });
        return { state: terminal ? 'failed-terminal' : 'failed-retryable', error: code };
      });
    });
  });
}
function reconcileOnce(callbacks) {
  if (!callbacks || typeof callbacks.publishDomains !== 'function' ||
      typeof callbacks.requestDriftComparison !== 'function') {
    return Promise.reject(new Error('TOKEN_SOURCE_INGESTION_CALLBACKS_INVALID'));
  }
  if (!fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, ROOT, { create: true, mode: 0o700 }) ||
      !fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, STAGES_ROOT, { create: true, mode: 0o700 })) {
    return Promise.reject(new Error('TOKEN_SOURCE_INGESTION_DIRECTORY_UNSAFE'));
  }
  cleanupAbandonedStages();
  var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, ROOT, MAX_INTENTS);
  if (!listed.ok) return Promise.reject(new Error('TOKEN_SOURCE_INGESTION_DIRECTORY_UNSAFE'));
  var ids = listed.names.map(function (name) {
    var match = FILE_RE.exec(name);
    if (!match) throw new Error('TOKEN_SOURCE_INGESTION_ENTRY_INVALID');
    return match[1];
  }).sort();
  var results = [];
  return ids.reduce(function (chain, id) {
    return chain.then(function () {
      return processRecord(readRecord(id), callbacks).then(function (result) { results.push(result); });
    });
  }, Promise.resolve()).then(function () { return results; });
}
function reconcile(callbacks) {
  if (running) return running;
  running = reconcileOnce(callbacks).finally(function () { running = null; });
  return running;
}
function recoveryReservations() {
  var rootEntry = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, path.dirname(ROOT), ROOT);
  var records = [];
  if (!(rootEntry && rootEntry.status === 'missing')) {
    if (!rootEntry || rootEntry.status !== 'present' || !rootEntry.stat ||
        !rootEntry.stat.isDirectory() || rootEntry.stat.isSymbolicLink()) {
      return Promise.reject(new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE'));
    }
    var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, ROOT, MAX_INTENTS);
    if (!listed.ok) return Promise.reject(new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE'));
    records = listed.names.map(function (name) {
      var match = FILE_RE.exec(name);
      if (!match) throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE');
      return readRecord(match[1]).value;
    });
  }
  return modules().then(function (loaded) {
    var receiptApi = loaded[0], intentApi = loaded[1];
    var byTask = Object.create(null), rows = [];
    records.forEach(function (intent) {
      intentApi.validateTaskIngestionIntent(intent);
      if (byTask[intent.taskStem]) throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE');
      byTask[intent.taskStem] = intent;
      var receipt = receiptApi.validateCommittedTaskObservationReceipt({
        taskStem: intent.taskStem,
        transactionId: intent.originTransactionId,
        receiptDirectory: receiptDirectory(intent),
        expectedManifestHash: intent.receiptManifestHash
      });
      var sourceRows = Object.create(null);
      receipt.sidecars.forEach(function (sidecar) {
        sourceRows[sidecar.capture.source.sourceId] = sidecar;
      });
      intent.sources.forEach(function (source) {
        var sidecar = sourceRows[source.sourceId];
        if (!sidecar || sidecar.capture.captureOperationId !== source.captureOperationId ||
            sidecar.capture.captureSequence !== source.captureSequence ||
            sidecar.batch.batchSemanticHash !== source.semanticHash) {
          throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE');
        }
        rows.push({
          sourceId: source.sourceId,
          captureOperationId: source.captureOperationId,
          captureSequence: source.captureSequence,
          at: sidecar.capture.witness.finishedAt,
          successAt: sidecar.capture.witness.finishedAt,
          captureEvidenceHash: sidecar.batch.captureEvidenceHash
        });
      });
    });
    var receiptsEntry = fileGuards.inspectEntryUnder(
      paths.PROJECT_ROOT, path.dirname(RECEIPTS_ROOT), RECEIPTS_ROOT);
    if (receiptsEntry && receiptsEntry.status === 'missing') {
      if (records.length) throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE');
      return rows;
    }
    if (!receiptsEntry || receiptsEntry.status !== 'present' || !receiptsEntry.stat ||
        !receiptsEntry.stat.isDirectory() || receiptsEntry.stat.isSymbolicLink()) {
      throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE');
    }
    var receiptDirs = fileGuards.boundedDirectoryNamesUnder(
      paths.PROJECT_ROOT, RECEIPTS_ROOT, MAX_INTENTS);
    if (!receiptDirs.ok) throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE');
    receiptDirs.names.forEach(function (name) {
      var directory = path.join(RECEIPTS_ROOT, name);
      var manifest = path.join(directory, 'token-observations-manifest.json');
      var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, directory, manifest);
      if (inspected && inspected.status === 'missing') return;
      if (!byTask[name]) throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE');
    });
    return rows;
  });
}

module.exports = {
  reconcile: reconcile,
  recoveryReservations: recoveryReservations,
  readRecord: readRecord,
  ROOT: ROOT,
  STAGES_ROOT: STAGES_ROOT,
  _retryableCode: retryableCode
};
