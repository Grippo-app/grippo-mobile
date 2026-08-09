'use strict';

var crypto = require('crypto');
var path = require('path');
var url = require('url');
var paths = require('./paths');
var taskSource = require('./task-source');
var storage = require('./app-run-storage');
var taskState = require('../../tasks/task-state-core.cjs');

var MAX_ITEMS = 200;
var ITEM_RESULT = Object.freeze({ pass: 1, fail: 1, 'not-tested': 1 });
var RECEIPT_FIELDS = [
  'schemaVersion', 'receiptId', 'taskStem', 'taskSourceRevision', 'runJobId',
  'sessionId', 'platform', 'deviceSummary', 'artifactId',
  'appProjectSourceRevision', 'checklist', 'overall', 'staleSource',
  'staleTask', 'createdAt'
];
var stalePreviews = new Map();
var validationSnapshots = new Map();

function sha(value) {
  return 'sha256:' + crypto.createHash('sha256').update(value).digest('hex');
}

function canonicalTask(stem) {
  if (!taskSource.safeTaskStem(String(stem || ''))) return null;
  var index = taskSource.readIndex();
  if (!index) return null;
  var found = index.rows.filter(function (entry) { return entry.row.stem === stem; });
  if (found.length !== 1) return null;
  var column = found[0].column;
  // The canonical task BODY for a pending task is its backlog source file; the
  // pending/<stem>.md artifact is the questions sidecar, never checklist input.
  var task = taskSource.readTask(column === 'pending' ? 'backlog' : column, stem);
  if (!task) return null;
  return { stem: stem, column: column, task: task, revision: sha(task.bytes) };
}

function parseManual(markdown, revision) {
  var source = String(markdown || '');
  if (source.indexOf('\r') >= 0 || source.charAt(0) === '\uFEFF') {
    throw new Error('manual checklist task text is non-canonical');
  }
  var h2Scan = taskState.scanAtxHeadings(source, 2);
  var acceptance = h2Scan.headings.filter(function (row) {
    return row.name.toLowerCase() === 'acceptance';
  });
  if (acceptance.length > 1) throw new Error('manual checklist has duplicate Acceptance sections');
  if (!acceptance.length) return [];
  var acceptanceHeading = acceptance[0];
  var nextH2 = h2Scan.headings.find(function (row) { return row.start > acceptanceHeading.start; });
  var acceptanceBody = h2Scan.structural.slice(
    acceptanceHeading.nextLineStart,
    nextH2 ? nextH2.start : h2Scan.structural.length
  );
  var h3Scan = taskState.scanAtxHeadings(acceptanceBody, 3);
  var manual = h3Scan.headings.filter(function (row) {
    return row.name.toLowerCase() === 'manual';
  });
  if (manual.length > 1) throw new Error('manual checklist has duplicate Manual subsections');
  if (!manual.length) return [];
  var manualHeading = manual[0];
  var nextH3 = h3Scan.headings.find(function (row) { return row.start > manualHeading.start; });
  var lines = h3Scan.structural.slice(
    manualHeading.nextLineStart,
    nextH3 ? nextH3.start : h3Scan.structural.length
  ).split('\n');
  var current = null, items = [];
  for (var i = 0; i < lines.length; i++) {
    if (/^[ \t]*$/.test(lines[i])) continue;
    var top = /^ {0,1}[-*+][ \t]+(.+?)[ \t]*$/.exec(lines[i]);
    if (top) {
      if (items.length >= MAX_ITEMS) throw new Error('manual checklist exceeds item limit');
      var text = top[1].replace(/^[ \t]+|[ \t]+$/g, '');
      if (!text) continue;
      if (Buffer.byteLength(text, 'utf8') > 2000) throw new Error('manual checklist item is oversized');
      current = { text: text, notes: [], position: items.length };
      items.push(current);
      continue;
    }
    var nested = /^[ \t]{2,}[-*+][ \t]+(.+?)[ \t]*$/.exec(lines[i]);
    if (nested && current) {
      var note = nested[1].replace(/^[ \t]+|[ \t]+$/g, '');
      if (current.notes.length >= 20 || Buffer.byteLength(note, 'utf8') > 1000) {
        throw new Error('manual checklist notes exceed their limits');
      }
      current.notes.push(note);
      continue;
    }
    throw new Error('manual checklist contains non-bullet structural content');
  }
  return items.map(function (item) {
    return {
      itemId: 'manual-' + crypto.createHash('sha256')
        .update(revision + '\0' + item.position + '\0' + item.text).digest('hex').slice(0, 24),
      text: item.text,
      notes: item.notes
    };
  });
}

function checklist(stem) {
  var source = canonicalTask(stem);
  if (!source) return { ok: false, status: 404, error: 'task-not-found' };
  var items;
  try { items = parseManual(source.task.text, source.revision); }
  catch (error) { return { ok: false, status: 409, error: 'manual-checklist-invalid', detail: error.message }; }
  var result = {
    ok: true,
    taskStem: stem,
    taskSourceRevision: source.revision,
    // Scoped by task identity and revision: two tasks with identical (for
    // example empty) Manual sections must never share one snapshot key.
    validationRevision: sha(JSON.stringify({
      taskStem: stem, taskSourceRevision: source.revision, items: items
    })),
    items: items
  };
  var stamp = Date.now();
  validationSnapshots.forEach(function (snapshot, key) {
    if (!snapshot || snapshot.expiresMs <= stamp) validationSnapshots.delete(key);
  });
  while (validationSnapshots.size >= 100) {
    validationSnapshots.delete(validationSnapshots.keys().next().value);
  }
  validationSnapshots.set(result.validationRevision, {
    taskStem: stem,
    taskSourceRevision: source.revision,
    validationRevision: result.validationRevision,
    items: JSON.parse(JSON.stringify(items)),
    expiresMs: stamp + 15 * 60 * 1000
  });
  return result;
}

function noteValid(value) {
  return value === null || (typeof value === 'string' && Buffer.byteLength(value, 'utf8') <= 1000 &&
    value.indexOf('\0') < 0);
}

function exactKeys(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}

function overall(items) {
  if (items.some(function (item) { return item.result === 'fail'; })) return 'failed';
  if (items.length > 0 && items.every(function (item) { return item.result === 'pass'; })) return 'passed';
  return 'partial';
}

function validateReceipt(receipt) {
  if (!exactKeys(receipt, RECEIPT_FIELDS) || receipt.schemaVersion !== 1 ||
      !/^receipt-[a-f0-9]{36}$/.test(String(receipt.receiptId || '')) ||
      !taskSource.safeTaskStem(String(receipt.taskStem || '')) ||
      !/^sha256:[a-f0-9]{64}$/.test(String(receipt.taskSourceRevision || '')) ||
      !/^job-[a-f0-9]{36}$/.test(String(receipt.runJobId || '')) ||
      !/^session-[a-f0-9]{36}$/.test(String(receipt.sessionId || '')) ||
      ['android', 'ios'].indexOf(receipt.platform) < 0 ||
      typeof receipt.deviceSummary !== 'string' || receipt.deviceSummary.length < 1 ||
      Buffer.byteLength(receipt.deviceSummary, 'utf8') > 300 ||
      /[\x00-\x1f\x7f]/.test(receipt.deviceSummary) ||
      !/^artifact-[a-f0-9]{36}$/.test(String(receipt.artifactId || '')) ||
      !/^sha256:[a-f0-9]{64}$/.test(String(receipt.appProjectSourceRevision || '')) ||
      !Array.isArray(receipt.checklist) || receipt.checklist.length > MAX_ITEMS ||
      ['passed', 'failed', 'partial'].indexOf(receipt.overall) < 0 ||
      receipt.overall !== overall(receipt.checklist) ||
      typeof receipt.staleSource !== 'boolean' || typeof receipt.staleTask !== 'boolean' ||
      typeof receipt.createdAt !== 'string' || !Number.isFinite(Date.parse(receipt.createdAt)) ||
      new Date(receipt.createdAt).toISOString() !== receipt.createdAt) return 'validation receipt is invalid';
  var seenReceiptItems = Object.create(null);
  for (var i = 0; i < receipt.checklist.length; i++) {
    var item = receipt.checklist[i];
    if (!exactKeys(item, ['itemId', 'result', 'note', 'screenshotIds']) ||
        !/^manual-[a-f0-9]{24}$/.test(String(item.itemId || '')) || !ITEM_RESULT[item.result] ||
        !noteValid(item.note) || !Array.isArray(item.screenshotIds) || item.screenshotIds.length > 10 ||
        item.screenshotIds.some(function (id) { return !/^shot-[a-f0-9]{36}$/.test(String(id || '')); })) {
      return 'validation receipt item is invalid';
    }
    if (seenReceiptItems[item.itemId] || new Set(item.screenshotIds).size !== item.screenshotIds.length) {
      return 'validation receipt screenshot ids are duplicated';
    }
    seenReceiptItems[item.itemId] = true;
  }
  return null;
}

function pruneHistory(activeReceiptId) {
  var issues = [];
  var rows = storage.list(paths.APP_RUN_HISTORY_DIR, 'receipt').map(function (id) {
    try {
      var value = storage.readJson(paths.APP_RUN_HISTORY_DIR, id, 128 * 1024);
      return { id: id, value: validateReceipt(value) ? null : value };
    } catch (_) { return { id: id, value: null }; }
  });
  rows.filter(function (row) { return !row.value; }).forEach(function (row) {
    // Invalid receipts are durable evidence. Never erase them as a retention
    // side effect; surface recovery-required through the runner instead.
    issues.push(row.id);
  });
  rows.filter(function (row) { return !!row.value; }).sort(function (a, b) {
    return Date.parse(b.value && b.value.createdAt || 0) - Date.parse(a.value && a.value.createdAt || 0);
  }).slice(100).forEach(function (row) {
    if (row.id !== activeReceiptId) {
      try { storage.remove(paths.APP_RUN_HISTORY_DIR, row.id, '.json', 128 * 1024); }
      catch (_) { issues.push(row.id); }
    }
  });
  return issues;
}

async function appendJournal(receipt) {
  try {
    var journalUrl = url.pathToFileURL(path.join(paths.ORCHESTRATOR_DIR, 'tasks', 'task-journal.mjs')).href;
    var journal = await import(journalUrl);
    return journal.appendEvent({
      kind: 'note',
      stem: receipt.taskStem,
      ts: receipt.createdAt,
      phase: 'runtime-verify',
      status: receipt.overall === 'passed' ? 'ok' : receipt.overall === 'failed' ? 'fail' : 'info',
      detail: 'Manual app validation: ' + receipt.overall,
      meta: { reportId: receipt.receiptId }
    }, receipt.taskStem);
  } catch (_) { return { ok: false }; }
}

function validateSaveBody(body) {
  var keys = ['taskStem', 'expectedTaskSourceRevision', 'sessionId', 'expectedSessionRevision',
    'validationRevision', 'items', 'acknowledgeStaleTask', 'idempotencyKey'];
  if (!body || typeof body !== 'object' || Array.isArray(body) ||
      Object.keys(body).sort().join('\0') !== keys.sort().join('\0') ||
      !taskSource.safeTaskStem(String(body.taskStem || '')) ||
      !/^sha256:[a-f0-9]{64}$/.test(String(body.expectedTaskSourceRevision || '')) ||
      !/^session-[a-f0-9]{36}$/.test(String(body.sessionId || '')) ||
      !Number.isSafeInteger(body.expectedSessionRevision) || body.expectedSessionRevision < 1 ||
      !/^sha256:[a-f0-9]{64}$/.test(String(body.validationRevision || '')) ||
      !Array.isArray(body.items) || body.items.length > MAX_ITEMS ||
      typeof body.acknowledgeStaleTask !== 'boolean' ||
      !/^[A-Za-z0-9._:-]{8,120}$/.test(String(body.idempotencyKey || ''))) return 'bad-validation-request';
  var seenItems = Object.create(null);
  for (var i = 0; i < body.items.length; i++) {
    var item = body.items[i];
    if (!item || Object.keys(item).sort().join('\0') !== ['itemId', 'note', 'result', 'screenshotIds'].sort().join('\0') ||
        !/^manual-[a-f0-9]{24}$/.test(String(item.itemId || '')) || !ITEM_RESULT[item.result] ||
        !noteValid(item.note) || !Array.isArray(item.screenshotIds) || item.screenshotIds.length > 10 ||
        item.screenshotIds.some(function (id) { return !/^shot-[a-f0-9]{36}$/.test(String(id || '')); })) {
      return 'bad-validation-request';
    }
    if (seenItems[item.itemId] || new Set(item.screenshotIds).size !== item.screenshotIds.length) {
      return 'bad-validation-request';
    }
    seenItems[item.itemId] = true;
  }
  return null;
}

async function save(body, context) {
  var issue = validateSaveBody(body);
  if (issue) return { ok: false, status: 400, error: issue };
  var current = checklist(body.taskStem);
  if (!current.ok) return current;
  var snapshot = validationSnapshots.get(body.validationRevision);
  if (!snapshot || snapshot.expiresMs <= Date.now() ||
      snapshot.taskStem !== body.taskStem ||
      snapshot.taskSourceRevision !== body.expectedTaskSourceRevision) {
    validationSnapshots.delete(body.validationRevision);
    return { ok: false, status: 409, error: 'validation-stale', current: current };
  }
  var expected = Object.create(null);
  snapshot.items.forEach(function (item) { expected[item.itemId] = true; });
  if (body.items.length !== snapshot.items.length ||
      body.items.some(function (item) { return !expected[item.itemId]; })) {
    return { ok: false, status: 400, error: 'validation-items-mismatch' };
  }
  var staleTask = current.taskSourceRevision !== snapshot.taskSourceRevision;
  var staleKey = [
    body.taskStem, body.sessionId, snapshot.taskSourceRevision,
    current.taskSourceRevision, snapshot.validationRevision
  ].join('\0');
  if (staleTask && !body.acknowledgeStaleTask) {
    stalePreviews.set(staleKey, Date.now() + 5 * 60 * 1000);
    if (stalePreviews.size > 100) {
      Array.from(stalePreviews.keys()).slice(0, stalePreviews.size - 100)
        .forEach(function (key) { stalePreviews.delete(key); });
    }
    return { ok: false, status: 409, error: 'validation-stale', current: current, confirmationRequired: true };
  }
  if (staleTask && body.acknowledgeStaleTask) {
    var previewExpiry = stalePreviews.get(staleKey);
    stalePreviews.delete(staleKey);
    if (!previewExpiry || previewExpiry <= Date.now()) {
      return { ok: false, status: 409, error: 'stale-task-acknowledgement-required', current: current };
    }
  } else if (!staleTask && body.acknowledgeStaleTask) {
    return { ok: false, status: 400, error: 'unexpected-stale-task-acknowledgement' };
  }
  var session = context.resolveSession(body.sessionId, body.expectedSessionRevision);
  if (!session.ok) return session;
  if (session.session.taskStem !== body.taskStem) {
    return { ok: false, status: 400, error: 'context-mismatch' };
  }
  for (var x = 0; x < body.items.length; x++) {
    for (var s = 0; s < body.items[x].screenshotIds.length; s++) {
      var ownership = context.screenshotOwned(
        body.items[x].screenshotIds[s], body.sessionId, body.taskStem);
      if (ownership && typeof ownership === 'object' && ownership.ok === false) {
        return ownership;
      }
      if (ownership !== true) {
        return { ok: false, status: 400, error: 'screenshot-not-owned' };
      }
    }
  }
  var receiptId = storage.randomId('receipt');
  var receipt = {
    schemaVersion: 1,
    receiptId: receiptId,
    taskStem: body.taskStem,
    taskSourceRevision: snapshot.taskSourceRevision,
    runJobId: session.session.jobId,
    sessionId: session.session.sessionId,
    platform: session.session.platform,
    deviceSummary: session.session.deviceSummary,
    artifactId: session.session.artifactId,
    appProjectSourceRevision: session.session.appProjectSourceRevision,
    checklist: body.items.map(function (item) {
      return { itemId: item.itemId, result: item.result, note: item.note, screenshotIds: item.screenshotIds.slice() };
    }),
    overall: overall(body.items),
    staleSource: context.currentSourceRevision() !== session.session.appProjectSourceRevision,
    staleTask: staleTask,
    createdAt: new Date().toISOString()
  };
  var receiptIssue = validateReceipt(receipt);
  if (receiptIssue) return { ok: false, status: 500, error: 'validation-receipt-invalid' };
  if (Buffer.byteLength(JSON.stringify(receipt, null, 2) + '\n', 'utf8') > 128 * 1024) {
    return { ok: false, status: 400, error: 'validation-receipt-too-large' };
  }
  storage.writeJson(paths.APP_RUN_HISTORY_DIR, receiptId, receipt, 128 * 1024);
  var retentionIssues = pruneHistory(receiptId);
  var journal = await appendJournal(receipt);
  return {
    ok: true,
    status: 201,
    receipt: receipt,
    journalRecorded: journal && journal.ok === true,
    retentionIssues: retentionIssues
  };
}

function history(stem, limit) {
  limit = Math.max(1, Math.min(Number(limit || 20), 100));
  var rows = [], invalidIds = [];
  storage.list(paths.APP_RUN_HISTORY_DIR, 'receipt').forEach(function (id) {
    try {
      var value = storage.readJson(paths.APP_RUN_HISTORY_DIR, id, 128 * 1024);
      if (validateReceipt(value)) invalidIds.push(id);
      else if (!stem || value.taskStem === stem) rows.push(value);
    } catch (_) { invalidIds.push(id); }
  });
  if (invalidIds.length) {
    var error = new Error('Stored validation history contains invalid receipts');
    error.code = 'validation-history-invalid';
    error.recordIds = invalidIds;
    throw error;
  }
  rows.sort(function (a, b) { return Date.parse(b.createdAt) - Date.parse(a.createdAt); });
  return rows.slice(0, limit);
}

function currentReceipt(receipt, taskSourceRevision, projectSourceRevision) {
  if (!receipt) return null;
  return Object.assign({}, receipt, {
    staleTask: receipt.staleTask || receipt.taskSourceRevision !== taskSourceRevision,
    staleSource: receipt.staleSource ||
      receipt.appProjectSourceRevision !== projectSourceRevision
  });
}

module.exports = {
  MAX_ITEMS: MAX_ITEMS,
  canonicalTask: canonicalTask,
  parseManual: parseManual,
  checklist: checklist,
  validateSaveBody: validateSaveBody,
  validateReceipt: validateReceipt,
  save: save,
  history: history,
  currentReceipt: currentReceipt,
  pruneHistory: pruneHistory
};
