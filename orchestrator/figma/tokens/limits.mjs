// Numeric budgets for the token pipeline (REQ-FIGMA-005, FIGMA_TOKENS.md §24.3,
// REQ-PERF-001). These constants are the single source the schemas mirror and
// the tests probe at boundary and overflow. Breaching any limit is a typed
// incomplete/failure, never a silent truncation.
export const TOKEN_LIMITS = Object.freeze({
  // Figma capture
  captureCollectionsMax: 64,
  captureModesMax: 64,
  captureVariablesMax: 5000,
  captureModeValuesPerVariableMax: 16,
  captureBytesMax: 8 * 1024 * 1024,
  aliasChainDepthMax: 16,

  // Adapter config
  adaptersMax: 8,
  rootsPerAdapterMax: 16,
  patternsPerAdapterMax: 32,
  authoritySymbolsMax: 16,
  projectModesMax: 16,

  // Project extraction
  projectFilesMax: 4000,
  projectFileBytesMax: 2 * 1024 * 1024,
  projectTotalBytesMax: 128 * 1024 * 1024,
  projectTokensMax: 10000,
  symbolGraphDepthMax: 16,
  aliasFanOutMax: 64,
  parseFailuresListedMax: 64,

  // Mapping registry
  mappingsMax: 5000,
  dispositionsMax: 5000,
  projectTokensPerMappingMax: 8,

  // Comparator
  comparisonRowsMax: 10000,
  projectOnlyRowsMax: 10000,
  suggestionsPerTokenMax: 5,
  lifecycleFindingsPerRowMax: 16,
  reportBlockersMax: 32,

  // Runner
  runnerTimeoutMs: 120000,
  runnerOutputBytesMax: 1024 * 1024,
  artifactBytesMax: 8 * 1024 * 1024
})
