'use strict';

// One process-wide Figma applicability pin. Startup owns initialization of all
// Figma recovery/watch/session services, so a false -> true config edit cannot
// safely open HTTP routes until a restart initializes those owners.

var projectConfig = require('./project-config-update');
var HASH_RE = /^sha256:[a-f0-9]{64}$/;
var ERROR_RE = /^[A-Za-z][A-Za-z0-9_-]{0,99}$/;

function publicFailure(result) {
  var status = result && Number.isInteger(result.status) &&
    result.status >= 400 && result.status <= 599 ? result.status : 500;
  var error = result && ERROR_RE.test(String(result.error || ''))
    ? result.error : 'internal';
  var out = { ok: false, status: status, error: error };
  if (result && HASH_RE.test(String(result.currentRevision || ''))) {
    out.currentRevision = result.currentRevision;
  }
  return out;
}

function configState(config) {
  if (!config || config.ok !== true || config.figmaEnabledState !== 'selected') {
    return { valid: false, enabled: false };
  }
  return { valid: true, enabled: config.figmaEnabled === true };
}

function evaluate(startupConfig, liveConfig) {
  var startup = configState(startupConfig);
  var live = configState(liveConfig);
  if (!live.valid) {
    return { enabled: false, status: 503, error: 'figma-config-invalid' };
  }
  if (!live.enabled) {
    return { enabled: false, status: 409, error: 'figma-disabled' };
  }
  if (!startup.valid || !startup.enabled) {
    return { enabled: false, status: 503, error: 'figma-restart-required' };
  }
  return { enabled: true, status: 200, error: null };
}

var startupConfig = projectConfig.read();

function startup() {
  var state = configState(startupConfig);
  return {
    enabled: state.valid && state.enabled,
    valid: state.valid,
    error: state.valid ? null : 'figma-config-invalid'
  };
}

function publicState(gate, liveConfig) {
  var state = gate.enabled ? 'enabled'
    : gate.error === 'figma-disabled' ? 'disabled'
      : gate.error === 'figma-restart-required' ? 'restart-required' : 'invalid';
  return {
    state: state,
    reasonCode: gate.error,
    configRevision: liveConfig && liveConfig.ok ? liveConfig.revision : null,
    canEnable: state === 'disabled' && liveConfig && liveConfig.ok === true &&
      liveConfig.hasFigmaEnabledField === true && liveConfig.figmaEnabledState === 'selected'
  };
}

function current(liveConfig) {
  var config = liveConfig || projectConfig.read();
  var gate = evaluate(startupConfig, config);
  gate.feature = publicState(gate, config);
  return gate;
}

function enable(request) {
  if (!request || typeof request !== 'object' || Array.isArray(request) ||
      Object.keys(request).sort().join('\0') !== 'expectedConfigRevision') {
    return { ok: false, status: 400, error: 'bad-figma-enable-request' };
  }
  var expectedRevision = String(request.expectedConfigRevision || '');
  if (!/^sha256:[a-f0-9]{64}$/.test(expectedRevision)) {
    return { ok: false, status: 400, error: 'bad-config-revision' };
  }
  var before = projectConfig.read();
  if (!before.ok || before.figmaEnabledState !== 'selected' ||
      before.hasFigmaEnabledField !== true) {
    return { ok: false, status: 409, error: 'figma-config-invalid' };
  }
  if (before.revision !== expectedRevision) {
    return {
      ok: false, status: 409, error: 'project-config-revision-conflict',
      currentRevision: before.revision
    };
  }
  var updated = before.figmaEnabled === true ? {
    ok: true, status: 200, revision: before.revision
  } : projectConfig.update({
    capability: 'figma', field: 'figmaEnabled', value: 'true',
    expectedRevision: expectedRevision
  });
  if (!updated.ok) return publicFailure(updated);
  var gate = current();
  if (gate.feature.state !== 'enabled' && gate.feature.state !== 'restart-required') {
    return { ok: false, status: 500, error: 'project-config-postcondition-failed' };
  }
  return {
    ok: true,
    status: 200,
    revision: updated.revision,
    feature: gate.feature
  };
}

module.exports = {
  startup: startup,
  current: current,
  enable: enable,
  _test: { evaluate: evaluate, publicState: publicState, publicFailure: publicFailure }
};
