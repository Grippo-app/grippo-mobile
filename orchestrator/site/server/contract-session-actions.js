'use strict';

// Exact typed action allowlist. No browser-owned prompt, command, URL or path
// can cross this boundary.

var ACTIONS = Object.freeze({
  probe: 'contract:probe',
  'refresh-openapi': 'contract:refresh-openapi',
  'refresh-postman': 'contract:refresh-postman',
  diff: 'contract:diff'
});

function refreshAction(sourceKind) {
  if (sourceKind === 'openapi') return ACTIONS['refresh-openapi'];
  if (sourceKind === 'postman') return ACTIONS['refresh-postman'];
  return null;
}

function valid(action) {
  return Object.keys(ACTIONS).some(function (key) { return ACTIONS[key] === action; });
}

module.exports = { ACTIONS: ACTIONS, refreshAction: refreshAction, valid: valid };
