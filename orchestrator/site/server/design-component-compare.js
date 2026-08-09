'use strict';

var freshness = require('./design-domain-freshness');
var state = require('./design-component-state');

module.exports = freshness.create({
  capability: 'components',
  analysisSchemaVersion: 2,
  state: state,
  indexRole: 'project-component-analysis-index',
  generationError: 'COMPONENT_GENERATION_RESYNC_REQUIRED',
  extractionModule: 'component-extraction.mjs',
  contractModule: 'components/project-inventory-contract.mjs',
  enabledAdaptersKey: 'enabledComponentAdapters',
  snapshotFunction: 'componentAdapterSnapshot',
  scopeFunction: 'componentScopeFingerprint',
  buildFunction: 'buildComponentInventory',
  watchRootKeys: ['roots', 'previewRoots', 'screenshotTestRoots']
});
