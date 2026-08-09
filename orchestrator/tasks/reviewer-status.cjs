#!/usr/bin/env node
'use strict';

// Deterministic, non-billable Codex readiness probe shared by the task
// orchestrator and the Site server. Active Claude-plugin state comes only from
// Claude Code's local installed-plugin inventory; cache directories are never
// treated as installations because uninstalled versions can remain there.
// Readiness comes from the active official plugin's own read-only `setup
// --json` contract, so the detector and the command that will actually run the
// review agree about CLI/app-server/auth support. The probe never starts a
// review or reads credentials itself.

var fs = require('fs');
var path = require('path');
var childProcess = require('child_process');

var DETECTOR_VERSION = 'reviewer-status-v2';
var OFFICIAL_PLUGIN_ID = 'codex@openai-codex';
var MAX_PLUGIN_OUTPUT_BYTES = 256 * 1024;
var MAX_INSTALLED_PLUGINS = 1000;
var DEFAULT_TIMEOUT_MS = 12000;
var RUNTIME_INJECTION_KEYS = [
  'NODE_OPTIONS',
  'NODE_PATH',
  'ELECTRON_RUN_AS_NODE',
  'LD_PRELOAD',
  'DYLD_INSERT_LIBRARIES',
  'DYLD_LIBRARY_PATH',
  'BASH_ENV'
];

function safeEnvironment(source, extra) {
  var env = Object.assign({}, source || {}, extra || {});
  for (var i = 0; i < RUNTIME_INJECTION_KEYS.length; i++) delete env[RUNTIME_INJECTION_KEYS[i]];
  return env;
}

function executableOnPath(name, pathValue, platform) {
  var names = platform === 'win32'
    ? [name + '.exe', name + '.cmd', name + '.bat', name]
    : [name];
  var delimiter = platform === 'win32' ? ';' : ':';
  var pieces = String(pathValue || '').split(delimiter).filter(Boolean);
  for (var i = 0; i < pieces.length; i++) {
    for (var j = 0; j < names.length; j++) {
      var candidate = path.join(pieces[i], names[j]);
      try {
        var resolved = fs.realpathSync(candidate);
        var stat = fs.lstatSync(resolved);
        if (!stat.isFile()) continue;
        fs.accessSync(resolved, platform === 'win32' ? fs.constants.F_OK : fs.constants.X_OK);
        return resolved;
      } catch (error) {}
    }
  }
  return null;
}

function result(availability, installed, checkedAt, reasonCode, source) {
  return {
    schemaVersion: 1,
    availability: availability,
    installed: installed,
    checkedAt: checkedAt,
    reasonCode: reasonCode,
    detectorVersion: DETECTOR_VERSION,
    source: source
  };
}

function commandFailure(invoked) {
  var code = invoked && invoked.error && invoked.error.code;
  if (code === 'ETIMEDOUT') return 'codex-check-timeout';
  if (code === 'ERR_CHILD_PROCESS_STDIO_MAXBUFFER' || code === 'ENOBUFS') {
    return 'codex-check-output-limit';
  }
  return 'codex-check-failed';
}

function invoke(spawn, executable, args, options, platform) {
  if (platform === 'win32' && /\.(?:cmd|bat)$/i.test(executable)) {
    // Arguments are fixed constants and the executable is an absolute path
    // resolved from PATH. Windows command shims require cmd.exe mediation.
    options = Object.assign({}, options, { shell: true });
  }
  return spawn(executable, args, options);
}

function pluginInventory(executable, spawn, options) {
  if (!executable) return { known: true, installed: false };
  var invoked = invoke(spawn, executable, ['plugin', 'list', '--json'], {
    cwd: options.projectRoot,
    encoding: 'utf8',
    timeout: options.timeoutMs,
    maxBuffer: MAX_PLUGIN_OUTPUT_BYTES,
    windowsHide: true,
    env: options.env
  }, options.platform);
  if (!invoked || invoked.error || invoked.status !== 0) {
    return { known: false, reasonCode: commandFailure(invoked) };
  }
  var parsed;
  try { parsed = JSON.parse(String(invoked.stdout || '')); }
  catch (error) { return { known: false, reasonCode: 'codex-check-malformed' }; }
  if (!Array.isArray(parsed) || parsed.length > MAX_INSTALLED_PLUGINS) {
    return { known: false, reasonCode: 'codex-check-malformed' };
  }
  var official = null;
  for (var i = 0; i < parsed.length; i++) {
    var row = parsed[i];
    if (!row || typeof row !== 'object' || Array.isArray(row) || row.id !== OFFICIAL_PLUGIN_ID) continue;
    if (official) {
      return {
        known: false,
        installed: true,
        source: 'claude-plugin',
        reasonCode: 'codex-check-malformed'
      };
    }
    if (typeof row.enabled !== 'boolean' || !Array.isArray(row.errors) ||
        typeof row.installPath !== 'string' || !row.installPath ||
        row.installPath.length > 4096 || /[\0\r\n]/.test(row.installPath)) {
      return {
        known: false,
        installed: true,
        source: 'claude-plugin',
        reasonCode: 'codex-check-malformed'
      };
    }
    official = {
      known: true,
      installed: true,
      enabled: row.enabled === true,
      broken: row.errors.length > 0,
      installPath: row.installPath,
      source: 'claude-plugin'
    };
  }
  return official || { known: true, installed: false };
}

function pluginSetupScript(installPath) {
  try {
    if (!path.isAbsolute(installPath)) return null;
    var root = fs.realpathSync(installPath);
    var rootStat = fs.lstatSync(root);
    if (!rootStat.isDirectory()) return null;
    var script = fs.realpathSync(path.join(root, 'scripts', 'codex-companion.mjs'));
    var relative = path.relative(root, script);
    if (!relative || relative === '..' || relative.indexOf('..' + path.sep) === 0 ||
        path.isAbsolute(relative)) return null;
    var scriptStat = fs.lstatSync(script);
    return scriptStat.isFile() ? script : null;
  } catch (error) {
    return null;
  }
}

function readinessContract(script, spawn, options) {
  if (!script) {
    return { known: true, ready: false, reasonCode: 'codex-contract-missing' };
  }
  var invoked = spawn(process.execPath, [script, 'setup', '--json'], {
    cwd: options.projectRoot,
    encoding: 'utf8',
    timeout: options.timeoutMs,
    maxBuffer: MAX_PLUGIN_OUTPUT_BYTES,
    windowsHide: true,
    env: options.env
  });
  if (!invoked || invoked.error) {
    return { known: false, reasonCode: commandFailure(invoked) };
  }
  if (invoked.status !== 0) {
    return { known: true, ready: false, reasonCode: 'codex-invocation-failed' };
  }
  var parsed;
  try { parsed = JSON.parse(String(invoked.stdout || '')); }
  catch (error) { return { known: false, reasonCode: 'codex-check-malformed' }; }
  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed) ||
      typeof parsed.ready !== 'boolean' ||
      !parsed.node || typeof parsed.node !== 'object' || typeof parsed.node.available !== 'boolean' ||
      !parsed.codex || typeof parsed.codex !== 'object' || typeof parsed.codex.available !== 'boolean' ||
      !parsed.auth || typeof parsed.auth !== 'object' || typeof parsed.auth.loggedIn !== 'boolean') {
    return { known: false, reasonCode: 'codex-check-malformed' };
  }
  var computedReady = parsed.node.available && parsed.codex.available && parsed.auth.loggedIn;
  if (parsed.ready !== computedReady) {
    return { known: false, reasonCode: 'codex-check-malformed' };
  }
  if (parsed.ready) return { known: true, ready: true, reasonCode: null };
  if (!parsed.node.available || !parsed.codex.available) {
    return { known: true, ready: false, reasonCode: 'codex-contract-missing' };
  }
  return { known: true, ready: false, reasonCode: 'codex-auth-missing' };
}

function detect(options) {
  options = options || {};
  var env = options.env || process.env;
  var projectRoot = path.resolve(
    options.projectRoot || env.ORCHESTRATOR_PROJECT_ROOT || path.join(__dirname, '..', '..')
  );
  var checkedAt = new Date(options.now === undefined ? Date.now() : options.now).toISOString();
  var platform = options.platform || process.platform;
  var pathValue = options.pathValue === undefined ? env.PATH : options.pathValue;
  var timeoutMs = options.timeoutMs || DEFAULT_TIMEOUT_MS;
  var spawn = options.spawnSync || childProcess.spawnSync;
  var commandEnv = safeEnvironment(env, { NO_COLOR: '1' });
  try {
    var claude = executableOnPath('claude', pathValue, platform);
    var plugin = pluginInventory(claude, spawn, {
      projectRoot: projectRoot,
      timeoutMs: timeoutMs,
      env: commandEnv,
      platform: platform
    });
    if (!plugin.known) {
      return result('unknown', plugin.installed ? 'yes' : 'unknown', checkedAt,
        plugin.reasonCode || 'codex-check-failed', plugin.source || 'none');
    }
    if (!plugin.installed) {
      return result('unavailable', 'no', checkedAt, 'codex-not-installed', 'none');
    }
    if (!plugin.enabled) {
      return result('unavailable', 'yes', checkedAt, 'codex-plugin-disabled', 'claude-plugin');
    }
    if (plugin.broken) {
      return result('unavailable', 'yes', checkedAt, 'codex-plugin-broken', 'claude-plugin');
    }
    var readiness = readinessContract(pluginSetupScript(plugin.installPath), spawn, {
      projectRoot: projectRoot,
      timeoutMs: timeoutMs,
      env: commandEnv
    });
    if (!readiness.known) {
      return result('unknown', 'yes', checkedAt,
        readiness.reasonCode || 'codex-check-failed', 'claude-plugin');
    }
    return result(readiness.ready ? 'available' : 'unavailable', 'yes', checkedAt,
      readiness.reasonCode, 'claude-plugin');
  } catch (error) {
    return result('unknown', 'unknown', checkedAt, 'codex-check-failed', 'none');
  }
}

module.exports = Object.freeze({
  DETECTOR_VERSION: DETECTOR_VERSION,
  DEFAULT_TIMEOUT_MS: DEFAULT_TIMEOUT_MS,
  safeEnvironment: safeEnvironment,
  detect: detect
});

if (require.main === module) {
  process.stdout.write(JSON.stringify(detect()) + '\n');
}
