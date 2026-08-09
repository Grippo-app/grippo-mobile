// Numeric budgets for the component pipeline (REQ-FIGMA-005, REQ-PERF-001,
// FIGMA_COMPONENTS.md §27.3). These constants are the single source the
// schemas mirror and the tests probe at boundary and overflow. Breaching any
// limit is a typed incomplete/failure, never a silent truncation.
export const COMPONENT_LIMITS = Object.freeze({
  // Figma capture
  capturePagesMax: 64,
  captureEntitiesMax: 2000,
  capturePropertiesPerEntityMax: 40,
  captureVariantsPerEntityMax: 500,
  captureVariantsTotalMax: 20000,
  captureDependencyEdgesPerEntityMax: 64,
  captureTokenRefsPerEntityMax: 128,
  captureBytesMax: 8 * 1024 * 1024,
  captureVisualEntriesMax: 2000,

  // Project extraction (input snapshot budgets are shared with tokens via
  // takeInputSnapshot limits below)
  projectFilesMax: 4000,
  projectFileBytesMax: 2 * 1024 * 1024,
  projectTotalBytesMax: 128 * 1024 * 1024,
  projectComponentsMax: 4000,
  parametersPerComponentMax: 64,
  projectVariantValuesPerPropertyMax: 64,
  slotsPerComponentMax: 64,
  dependencyEdgesPerComponentMax: 64,
  symbolGraphDepthMax: 16,
  parseFailuresListedMax: 64,

  // Mapping registry
  mappingsMax: 2000,
  dispositionsMax: 2000,
  implementationsPerMappingMax: 8,
  projectComponentsPerImplementationMax: 8,
  propertyMappingsPerMappingMax: 40,
  valueMapEntriesPerPropertyMax: 64,
  slotMappingsPerMappingMax: 64,

  // Comparator
  comparisonRowsMax: 4000,
  projectOnlyRowsMax: 4000,
  findingsPerRowMax: 64,
  suggestionsPerComponentMax: 5,
  taskProposalsMax: 200,
  reportBlockersMax: 32,

  // Runner
  runnerTimeoutMs: 120000,
  runnerOutputBytesMax: 1024 * 1024,
  artifactBytesMax: 8 * 1024 * 1024
})
