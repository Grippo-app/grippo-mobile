'use strict';

// ---------------------------------------------------------------------------
// Persisted (user-input) state — only the bits that cannot be derived from
// the filesystem. Everything else in /api/state is computed. Stored at
// orchestrator/.cache/site/.site-state.json (see paths.STATE_FILE).
// ---------------------------------------------------------------------------

var fs    = require('fs');
var path  = require('path');
var paths = require('./paths');
var taskSource = require('../../tasks/task-source-contract.cjs');

var STATE_FILE = paths.STATE_FILE;
var SCHEMA_VERSION = 1;

// Server-side mirror of SUPPORTED in site/scripts/i18n.js. Adding a language =
// update both lists (client renders it, server persists it).
var UI_LANGS = ['en', 'ru', 'uk'];
var SETUP_KEY_TYPES = Object.freeze(Object.assign(Object.create(null), {
  productName: 'string', backendHost: 'string', applicationId: 'string', firstDomain: 'string',
  iosFrameworkName: 'string', orgName: 'string', typefaceFactory: 'string', figmaEnabled: 'boolean',
  figmaLibraryUrl: 'string', screenshotPixelGate: 'string', iosEnabled: 'boolean',
  firebaseEnabled: 'boolean', prelaunch: 'boolean', supportedLocales: 'array', authMethods: 'array'
}));

var DEFAULT_PERSISTED = {
  schemaVersion: SCHEMA_VERSION,
  setupForm: {},
  manualSteps: {},        // { '<stepId>': true }: dialog-only confirmation
                          // for steps with a null validator, plus a manual
                          // override for FS-derived steps (overrides win).
  taskTiming: {},         // Goal 6: { '<stem>': { startedAt, doneAt?, durationMs? } }.
                          // Built by the SSE poll (see server/timing.js); survives
                          // restarts here, dropped on /api/reset like every other
                          // persisted key.
  taskLifecycle: {},      // Per-task column spine: { '<stem>': [ { column, enteredAt, source } ] }.
                          // Built by the SSE poll (server/timing.js reconcileLifecycle);
                          // drives the Task Details Activity lifecycle bands.
                          // Same persistence lifecycle as taskTiming.
  uiLang: '',             // UI language ('en'|'ru'|'uk'; '' = never chosen). Server-persisted
                          // so the choice survives an explicitly changed port: localStorage is
                          // origin-bound, and a new port = a new origin = a "reset" toggle.
                          // NOT a setup field — must never leak into the bootstrap YAML.
  backendActiveEnvironmentId: '', // Workspace-local Backend selection; never written to project config.
  backendSelectionRevision: 0,
  appRunPreferences: {
    platform: null,
    targetStableHint: null,
    variantId: null,
    buildMode: null
  }
};

function clone(v) { return JSON.parse(JSON.stringify(v)); }

function invalidState(detail) {
  var error = new Error('current site state is invalid: ' + detail);
  error.code = 'SITE_STATE_INVALID';
  return error;
}

function plainObject(value) {
  return !!value && typeof value === 'object' && !Array.isArray(value);
}

function instant(value) {
  return typeof value === 'string' && value.length > 0 && Number.isFinite(Date.parse(value));
}

function exactKeys(value, fields) {
  return plainObject(value) && Object.keys(value).sort().join('\0') === fields.slice().sort().join('\0');
}

function own(map, key) {
  return Object.prototype.hasOwnProperty.call(map, key);
}

function validatePersisted(value) {
  var fields = Object.keys(DEFAULT_PERSISTED).sort();
  if (!plainObject(value) || Object.keys(value).sort().join('\0') !== fields.join('\0') || value.schemaVersion !== SCHEMA_VERSION) {
    throw invalidState('expected the exact schemaVersion-' + SCHEMA_VERSION + ' envelope');
  }
  if (!plainObject(value.setupForm) || Object.keys(value.setupForm).some(function (key) { return !own(SETUP_KEY_TYPES, key); })) {
    throw invalidState('setupForm contains an unknown key or has the wrong shape');
  }
  if (!plainObject(value.manualSteps) || Object.keys(value.manualSteps).some(function (key) { return typeof value.manualSteps[key] !== 'boolean'; })) {
    throw invalidState('manualSteps must be a boolean map');
  }
  if (!plainObject(value.taskTiming) || !plainObject(value.taskLifecycle)) {
    throw invalidState('taskTiming and taskLifecycle must be objects');
  }
  Object.keys(value.taskTiming).forEach(function (stem) {
    var row = value.taskTiming[stem];
    if (!taskSource.safeTaskStem(stem) || !exactKeys(row, ['startedAt', 'doneAt', 'durationMs']) ||
        (row.startedAt !== null && !instant(row.startedAt)) || (row.doneAt !== null && !instant(row.doneAt)) ||
        (row.durationMs !== null && (typeof row.durationMs !== 'number' || !Number.isFinite(row.durationMs) || row.durationMs < 0))) {
      throw invalidState('taskTiming contains an invalid row');
    }
  });
  Object.keys(value.taskLifecycle).forEach(function (stem) {
    var rows = value.taskLifecycle[stem];
    if (!taskSource.safeTaskStem(stem) || !Array.isArray(rows) || !rows.length || rows.some(function (row) {
      return !exactKeys(row, ['column', 'enteredAt', 'source']) ||
        ['backlog', 'pending', 'todo', 'done'].indexOf(row.column) < 0 || !instant(row.enteredAt) ||
        (row.source !== 'agent' && row.source !== 'observed');
    })) throw invalidState('taskLifecycle contains an invalid row');
  });
  if (typeof value.uiLang !== 'string' || (value.uiLang !== '' && UI_LANGS.indexOf(value.uiLang) < 0)) {
    throw invalidState('uiLang is unsupported');
  }
  if (typeof value.backendActiveEnvironmentId !== 'string' ||
      ['', 'local', 'dev', 'stage', 'prod'].indexOf(value.backendActiveEnvironmentId) < 0) {
    throw invalidState('backendActiveEnvironmentId is unsupported');
  }
  if (!Number.isSafeInteger(value.backendSelectionRevision) || value.backendSelectionRevision < 0) {
    throw invalidState('backendSelectionRevision must be a non-negative safe integer');
  }
  if (!exactKeys(value.appRunPreferences, ['platform', 'targetStableHint', 'variantId', 'buildMode']) ||
      (value.appRunPreferences.platform !== null && ['android', 'ios'].indexOf(value.appRunPreferences.platform) < 0) ||
      (value.appRunPreferences.targetStableHint !== null && !/^hint-[a-f0-9]{32}$/.test(value.appRunPreferences.targetStableHint)) ||
      (value.appRunPreferences.variantId !== null && !/^[a-z][a-z0-9-]{0,31}$/.test(value.appRunPreferences.variantId)) ||
      (value.appRunPreferences.buildMode !== null && ['rebuild', 'if-needed', 'last-build'].indexOf(value.appRunPreferences.buildMode) < 0)) {
    throw invalidState('appRunPreferences is invalid');
  }
  return clone(value);
}

function readPersisted() {
  try {
    var raw = fs.readFileSync(STATE_FILE, 'utf8');
    var parsed = JSON.parse(raw);
    return validatePersisted(parsed);
  } catch (e) {
    if (e && e.code === 'ENOENT') return clone(DEFAULT_PERSISTED);
    if (e && e.code === 'SITE_STATE_INVALID') throw e;
    throw invalidState(e && e.message ? e.message : String(e));
  }
}

function writePersisted(state) {
  var tmp = STATE_FILE + '.tmp';
  try {
    var current = validatePersisted(state);
    fs.mkdirSync(path.dirname(STATE_FILE), { recursive: true });
    fs.writeFileSync(tmp, JSON.stringify(current, null, 2) + '\n', 'utf8');
    fs.renameSync(tmp, STATE_FILE);
  } catch (e) {
    console.error('[site] failed to write state file:', e.message);
    try { fs.unlinkSync(tmp); } catch (e2) {}
    throw e;
  }
}

module.exports = {
  DEFAULT_PERSISTED: DEFAULT_PERSISTED,
  SCHEMA_VERSION: SCHEMA_VERSION,
  SETUP_KEY_TYPES: SETUP_KEY_TYPES,
  UI_LANGS: UI_LANGS,
  clone: clone,
  readPersisted: readPersisted,
  writePersisted: writePersisted
};
