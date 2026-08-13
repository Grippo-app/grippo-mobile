'use strict';

// One process-wide Figma applicability pin. Startup owns initialization of all
// Figma recovery/watch/session services, so a false -> true config edit cannot
// safely open HTTP routes until a restart initializes those owners.

var projectConfig = require('./project-config-update');

function configState(config) {
  if (!config || config.ok !== true || config.figmaEnabledState === 'invalid') {
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

function current() {
  return evaluate(startupConfig, projectConfig.read());
}

module.exports = {
  startup: startup,
  current: current,
  _test: { evaluate: evaluate }
};
