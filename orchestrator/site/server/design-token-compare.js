'use strict';

var freshness = require('./design-domain-freshness');
var state = require('./design-token-state');

module.exports = freshness.create({
  capability: 'tokens',
  state: state,
  indexRole: 'project-token-analysis-index',
  generationError: 'TOKEN_GENERATION_RESYNC_REQUIRED',
  extractionModule: 'token-extraction.mjs',
  contractModule: 'tokens/project-inventory-contract.mjs',
  enabledAdaptersKey: 'enabledTokenAdapters',
  snapshotFunction: 'adapterSnapshot',
  scopeFunction: 'adapterScopeFingerprint',
  buildFunction: 'buildAdapterInventory',
  watchRootKeys: ['roots']
});
