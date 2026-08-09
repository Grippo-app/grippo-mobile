'use strict';

// Bounded subprocess wrapper around the shared reviewer-status helper. The
// process boundary prevents malformed helper output or a stuck local Codex
// installation from blocking the Site indefinitely.

var childProcess = require('child_process');
var path = require('path');
var childEnv = require('./child-env').childEnv;

var HELPER = path.join(__dirname, '..', '..', 'tasks', 'reviewer-status.cjs');
var CACHE_MS = 60 * 1000;
var TIMEOUT_MS = 15000;
var MAX_OUTPUT_BYTES = 16 * 1024;
var cache = null;
var inflight = null;
var generation = 0;
var REASON_CODES = [
  'codex-auth-missing',
  'codex-check-failed',
  'codex-check-malformed',
  'codex-check-output-limit',
  'codex-check-timeout',
  'codex-contract-missing',
  'codex-invocation-failed',
  'codex-not-installed',
  'codex-plugin-broken',
  'codex-plugin-disabled'
];

function fallback(reasonCode) {
  return {
    availability: 'unknown',
    installed: 'unknown',
    checkedAt: new Date().toISOString(),
    reasonCode: reasonCode,
    detectorVersion: 'reviewer-status-v2',
    source: 'none'
  };
}

function valid(value) {
  if (!value || value.schemaVersion !== 1 ||
    ['available', 'unavailable', 'unknown'].indexOf(value.availability) < 0 ||
    ['yes', 'no', 'unknown'].indexOf(value.installed) < 0 ||
    typeof value.checkedAt !== 'string' ||
    !/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/.test(value.checkedAt) ||
    !Number.isFinite(Date.parse(value.checkedAt)) ||
    new Date(value.checkedAt).toISOString() !== value.checkedAt ||
    !(value.reasonCode === null || REASON_CODES.indexOf(value.reasonCode) >= 0) ||
    value.detectorVersion !== 'reviewer-status-v2' ||
    ['claude-plugin', 'none'].indexOf(value.source) < 0) {
    return false;
  }
  if (value.availability === 'available') {
    return value.installed === 'yes' && value.source === 'claude-plugin' &&
      value.reasonCode === null;
  }
  if (value.reasonCode === null) return false;
  if (value.installed === 'yes') return value.source === 'claude-plugin';
  if (value.installed === 'no') {
    return value.availability === 'unavailable' && value.source === 'none' &&
      value.reasonCode === 'codex-not-installed';
  }
  return value.availability === 'unknown' && value.source === 'none';
}

function run() {
  return new Promise(function (resolve) {
    var child;
    try {
      child = childProcess.spawn(process.execPath, [HELPER], {
        cwd: path.join(__dirname, '..', '..', '..'),
        env: childEnv(),
        stdio: ['ignore', 'pipe', 'pipe'],
        windowsHide: true
      });
    } catch (error) {
      resolve(fallback('codex-check-failed'));
      return;
    }
    var stdout = Buffer.alloc(0);
    var stderrBytes = 0;
    var settled = false;
    var timer = setTimeout(function () {
      if (settled) return;
      settled = true;
      try { child.kill('SIGKILL'); } catch (error) {}
      resolve(fallback('codex-check-timeout'));
    }, TIMEOUT_MS);
    function finish(value) {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      resolve(value);
    }
    child.stdout.on('data', function (chunk) {
      if (settled) return;
      if (stdout.length + chunk.length > MAX_OUTPUT_BYTES) {
        try { child.kill('SIGKILL'); } catch (error) {}
        finish(fallback('codex-check-output-limit'));
        return;
      }
      stdout = Buffer.concat([stdout, chunk]);
    });
    child.stderr.on('data', function (chunk) {
      stderrBytes += chunk.length;
      if (stderrBytes > MAX_OUTPUT_BYTES) {
        try { child.kill('SIGKILL'); } catch (error) {}
        finish(fallback('codex-check-output-limit'));
      }
    });
    child.on('error', function () { finish(fallback('codex-check-failed')); });
    child.on('close', function (code) {
      if (settled) return;
      if (code !== 0) { finish(fallback('codex-check-failed')); return; }
      var parsed;
      try { parsed = JSON.parse(stdout.toString('utf8')); }
      catch (error) { finish(fallback('codex-check-malformed')); return; }
      finish(valid(parsed) ? {
        availability: parsed.availability,
        installed: parsed.installed,
        checkedAt: parsed.checkedAt,
        reasonCode: parsed.reasonCode,
        detectorVersion: parsed.detectorVersion,
        source: parsed.source
      } : fallback('codex-check-malformed'));
    });
  });
}

function get(force) {
  var fresh = cache && Date.now() - cache.cachedAt < CACHE_MS;
  if (!force && fresh) return Promise.resolve(cache.value);
  if (inflight) return inflight;
  inflight = run().then(function (value) {
    cache = { cachedAt: Date.now(), value: value };
    generation++;
    inflight = null;
    return value;
  }, function () {
    var value = fallback('codex-check-failed');
    cache = { cachedAt: Date.now(), value: value };
    generation++;
    inflight = null;
    return value;
  });
  return inflight;
}

function cached() {
  return cache ? cache.value : null;
}

function revision() {
  return String(generation) + ':' + (cache && cache.value.checkedAt || 'none') + ':' + (inflight ? 'checking' : 'idle');
}

module.exports = {
  get: get,
  cached: cached,
  revision: revision
};
