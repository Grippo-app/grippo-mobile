'use strict';

// Typed adapter seam for the shared app runner. Design publishes only a
// surface id and capability state; command, target, and path authority remains
// inside the app runner.

var appRunConfig = require('./app-run-config');

function capability(surface) {
  var config = appRunConfig.load();
  if (!config.ok) return {
    available: false,
    reason: config.error === 'project-not-generated' ? 'app-runner-not-configured' : 'app-runner-config-invalid',
    platformOptions: [],
    deepLink: surface && surface.route || null
  };
  return {
    available: true,
    reason: 'navigate-manually',
    platformOptions: [
      config.manifest.android ? 'android' : null,
      config.manifest.ios ? 'ios' : null
    ].filter(Boolean),
    deepLink: surface && surface.route || null
  };
}

module.exports = { capability: capability };
