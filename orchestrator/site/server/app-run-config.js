'use strict';

var path = require('path');
var crypto = require('crypto');
var paths = require('./paths');
var fileGuards = require('./file-guards');

var MANIFEST_FILE = path.join(paths.PROJECT_ORCHESTRATOR_DIR || path.dirname(paths.PROJECT_CONFIG_FILE), 'app-run.json');
var SCHEMA_FILE = path.join(
  paths.ORCHESTRATOR_DIR, 'site', 'contracts', 'app-run', 'config.schema.json');
var MAX_CONFIG_BYTES = 256 * 1024;
var APP_ID_RE = /^[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z][A-Za-z0-9_]*)+$/;
var MODULE_RE = /^[A-Za-z][A-Za-z0-9_-]{0,63}$/;
var ID_RE = /^[a-z][a-z0-9-]{0,31}$/;
var XCODE_NAME_RE = /^[A-Za-z0-9._ -]{1,80}$/;
var PROJECT_PATH_RE = /^[A-Za-z0-9._-]+(?:\/[A-Za-z0-9._-]+)*\.xcodeproj$/;
var TASK_RE = /^:[A-Za-z][A-Za-z0-9_-]{0,63}:[A-Za-z][A-Za-z0-9_]{0,79}$/;
var RESERVED_ROOTS = Object.freeze({
  '.git': 1, '.gradle': 1, '.idea': 1, '.kotlin': 1, '.swiftpm': 1,
  build: 1, DerivedData: 1, node_modules: 1, orchestrator: 1, out: 1
});

function exactKeys(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}

function readRegular(file, maximum) {
  var directory = path.dirname(file);
  var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, directory, file);
  if (inspected && inspected.status === 'missing') {
    var missing = new Error('configuration file is missing');
    missing.code = 'ENOENT';
    throw missing;
  }
  var hit = fileGuards.boundedRegularFileUnder(
    paths.PROJECT_ROOT, directory, file, maximum);
  if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') {
    throw new Error('configuration file is unsafe, changing, or oversized');
  }
  return hit.bytes;
}

function frontmatter(text) {
  text = String(text || '').replace(/^\uFEFF/, '').replace(/\r\n?/g, '\n');
  var lines = text.split('\n');
  if (lines[0] !== '---') return null;
  var end = lines.indexOf('---', 1);
  return end < 0 ? null : lines.slice(1, end).join('\n');
}

function scalar(block, key) {
  var matches = [];
  String(block || '').split('\n').forEach(function (line) {
    var match = new RegExp('^' + key + ':[ \\t]*(.*)$').exec(line);
    if (match) matches.push(match[1].trim());
  });
  return { count: matches.length, value: matches.length === 1 ? matches[0] : null };
}

function invalid(code, detail) {
  return { ok: false, error: code, detail: String(detail || code).slice(0, 500) };
}

function canonicalJson(value) {
  if (Array.isArray(value)) return '[' + value.map(canonicalJson).join(',') + ']';
  if (value && typeof value === 'object') {
    return '{' + Object.keys(value).sort().map(function (key) {
      return JSON.stringify(key) + ':' + canonicalJson(value[key]);
    }).join(',') + '}';
  }
  return JSON.stringify(value);
}

function safeRelativeProjectPath(value, suffix) {
  if (typeof value !== 'string' || !value || value.length > 240 || value.indexOf('\\') >= 0 ||
      value.indexOf('\0') >= 0 || path.posix.isAbsolute(value)) return false;
  var normalized = path.posix.normalize(value);
  if (normalized !== value || normalized === '..' || normalized.indexOf('../') === 0) return false;
  if (RESERVED_ROOTS[value.split('/')[0]]) return false;
  if (!PROJECT_PATH_RE.test(value)) return false;
  return !suffix || value.slice(-suffix.length) === suffix;
}

function safeAppRoot(value) {
  return typeof value === 'string' && MODULE_RE.test(value) && !RESERVED_ROOTS[value];
}

function validLabel(value) {
  return typeof value === 'string' && value.trim() === value && value.length >= 1 &&
    value.length <= 80 && Buffer.byteLength(value, 'utf8') <= 160 &&
    !/[\x00-\x1f\x7f]/.test(value);
}

function validXcodeName(value) {
  return typeof value === 'string' && value.trim() === value && XCODE_NAME_RE.test(value);
}

function uniqueIds(rows) {
  var seen = Object.create(null);
  for (var i = 0; i < rows.length; i++) {
    if (seen[rows[i].id]) return false;
    seen[rows[i].id] = true;
  }
  return true;
}

function validateManifest(value) {
  if (!exactKeys(value, ['schemaVersion', 'android', 'ios']) || value.schemaVersion !== 1) {
    return invalid('app-run-config-invalid', 'manifest must use the exact schemaVersion-1 envelope');
  }
  if (value.android !== null) {
    if (!exactKeys(value.android, ['module', 'variants']) || !safeAppRoot(value.android.module) ||
        !Array.isArray(value.android.variants) || value.android.variants.length < 1 ||
        value.android.variants.length > 20) {
      return invalid('app-run-config-invalid', 'android run configuration is invalid');
    }
    for (var a = 0; a < value.android.variants.length; a++) {
      var variant = value.android.variants[a];
      if (!exactKeys(variant, ['id', 'label', 'assembleTaskRef']) ||
          !ID_RE.test(String(variant.id || '')) || !validLabel(variant.label) ||
          variant.assembleTaskRef !== 'project-config.androidAssembleTask') {
        return invalid('app-run-config-invalid', 'android variant is invalid');
      }
    }
    if (!uniqueIds(value.android.variants)) return invalid('app-run-config-invalid', 'android variant ids must be unique');
  }
  if (value.ios !== null) {
    if (!exactKeys(value.ios, ['project', 'scheme', 'configurations']) ||
        !safeRelativeProjectPath(value.ios.project, '.xcodeproj') ||
        !validXcodeName(value.ios.scheme) ||
        !Array.isArray(value.ios.configurations) || value.ios.configurations.length < 1 ||
        value.ios.configurations.length > 20) {
      return invalid('app-run-config-invalid', 'iOS run configuration is invalid');
    }
    for (var i = 0; i < value.ios.configurations.length; i++) {
      var config = value.ios.configurations[i];
      if (!exactKeys(config, ['id', 'label', 'configuration']) ||
          !ID_RE.test(String(config.id || '')) || !validLabel(config.label) ||
          !validXcodeName(config.configuration)) {
        return invalid('app-run-config-invalid', 'iOS configuration is invalid');
      }
    }
    if (!uniqueIds(value.ios.configurations)) return invalid('app-run-config-invalid', 'iOS configuration ids must be unique');
  }
  return { ok: true, value: value };
}

function loadProjectConfig() {
  var bytes;
  try { bytes = readRegular(paths.PROJECT_CONFIG_FILE, MAX_CONFIG_BYTES); }
  catch (error) {
    return invalid(error && error.code === 'ENOENT' ? 'project-not-generated' : 'project-config-unavailable', error.message);
  }
  var block = frontmatter(bytes.toString('utf8'));
  if (!block) return invalid('project-config-invalid', 'project config frontmatter is missing');
  var applicationField = scalar(block, 'applicationId');
  var iosField = scalar(block, 'iosEnabled');
  var androidTaskField = scalar(block, 'androidAssembleTask');
  if (applicationField.count !== 1 || iosField.count !== 1 || androidTaskField.count !== 1) {
    return invalid('project-config-invalid', 'canonical app-run fields must be unique');
  }
  var applicationId = applicationField.value;
  var iosEnabled = iosField.value;
  var androidTask = androidTaskField.value;
  if (!APP_ID_RE.test(String(applicationId || '')) || String(applicationId).indexOf('<') >= 0) {
    return invalid('project-not-generated', 'applicationId is missing or invalid');
  }
  if (iosEnabled !== 'true' && iosEnabled !== 'false') return invalid('project-config-invalid', 'iosEnabled must be true or false');
  if (!TASK_RE.test(androidTask)) return invalid('project-config-invalid', 'androidAssembleTask is invalid');
  return {
    ok: true,
    value: {
      applicationId: applicationId,
      iosEnabled: iosEnabled === 'true',
      androidAssembleTask: androidTask
    },
    revision: 'sha256:' + crypto.createHash('sha256').update(bytes).digest('hex')
  };
}

function synthesized() {
  return {
    schemaVersion: 1,
    android: {
      module: 'androidApp',
      variants: [{ id: 'debug', label: 'Debug', assembleTaskRef: 'project-config.androidAssembleTask' }]
    },
    ios: {
      project: 'iosApp/iosApp.xcodeproj',
      scheme: 'iosApp',
      configurations: [{ id: 'debug', label: 'Debug', configuration: 'Debug' }]
    }
  };
}

function load() {
  var project = loadProjectConfig();
  if (!project.ok) return project;
  var manifest;
  var source = 'default';
  try {
    manifest = JSON.parse(readRegular(MANIFEST_FILE, MAX_CONFIG_BYTES).toString('utf8'));
    source = 'manifest';
  } catch (error) {
    if (!error || error.code !== 'ENOENT') return invalid('app-run-config-invalid', error.message);
    manifest = synthesized();
  }
  var checked = validateManifest(manifest);
  if (!checked.ok) return checked;
  var android = manifest.android;
  var ios = project.value.iosEnabled ? manifest.ios : null;
  if (android && project.value.androidAssembleTask.split(':')[1] !== android.module) {
    return invalid('app-run-config-invalid',
      'android run module must match the canonical project-config assemble task');
  }
  var effective = { schemaVersion: 1, android: android, ios: ios };
  return {
    ok: true,
    project: project.value,
    projectConfigRevision: project.revision,
    manifest: effective,
    source: source,
    runConfigHash: 'sha256:' + crypto.createHash('sha256').update(canonicalJson({
      applicationId: project.value.applicationId,
      androidAssembleTask: project.value.androidAssembleTask,
      manifest: effective
    }), 'utf8').digest('hex'),
    schemaFile: SCHEMA_FILE
  };
}

function resolveVariant(config, platform, id) {
  if (!config || !config.ok || (platform !== 'android' && platform !== 'ios') || !ID_RE.test(String(id || ''))) return null;
  if (platform === 'android') {
    var variants = config.manifest.android && config.manifest.android.variants || [];
    for (var i = 0; i < variants.length; i++) {
      if (variants[i].id === id) return {
        id: variants[i].id, label: variants[i].label,
        module: config.manifest.android.module,
        assembleTask: config.project.androidAssembleTask
      };
    }
    return null;
  }
  var rows = config.manifest.ios && config.manifest.ios.configurations || [];
  for (var x = 0; x < rows.length; x++) {
    if (rows[x].id === id) return {
      id: rows[x].id, label: rows[x].label, configuration: rows[x].configuration,
      project: config.manifest.ios.project, scheme: config.manifest.ios.scheme
    };
  }
  return null;
}

function sourceRoots(config) {
  if (!config || !config.ok) return [];
  var roots = [];
  if (config.manifest.android) roots.push(config.manifest.android.module);
  if (config.manifest.ios) roots.push(config.manifest.ios.project.split('/')[0]);
  return roots.filter(function (root, index) { return roots.indexOf(root) === index; }).sort();
}

module.exports = {
  APP_ID_RE: APP_ID_RE,
  ID_RE: ID_RE,
  TASK_RE: TASK_RE,
  MANIFEST_FILE: MANIFEST_FILE,
  canonicalJson: canonicalJson,
  validateManifest: validateManifest,
  loadProjectConfig: loadProjectConfig,
  load: load,
  resolveVariant: resolveVariant,
  sourceRoots: sourceRoots,
  safeRelativeProjectPath: safeRelativeProjectPath
};
