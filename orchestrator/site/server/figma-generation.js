'use strict';

// Coherent Figma generation reader/publisher contract. Readers resolve one
// manifest through one exact current pointer; they never choose independently
// newest token/component/surface files.

var crypto = require('crypto');
var fs = require('fs');
var path = require('path');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var programLimits = require(path.join(paths.ORCHESTRATOR_DIR, 'figma', 'runtime', 'program-limits.cjs'));

var FIGMA_DIR = path.join(paths.PROJECT_ROOT, 'orchestrator', 'figma');
var MANIFESTS_DIR = path.join(FIGMA_DIR, 'manifests');
var GENERATIONS_DIR = path.join(MANIFESTS_DIR, 'generations');
var ARTIFACTS_DIR = path.join(MANIFESTS_DIR, 'artifacts');
var POINTER_FILE = path.join(MANIFESTS_DIR, 'current-generation.json');
var MANIFEST_MAX = programLimits.stageManifestBytesMax;
var POINTER_MAX = 16 * 1024;
var ARTIFACT_MAX = programLimits.artifactBytesMax;
var GENERATION_EVIDENCE_MAX = 4096;
var GENERATION_RE = /^gen-[a-f0-9]{32}$/;
var SYNC_JOB_RE = /^fsj-[a-f0-9]{32}$/;
var HASH_RE = /^sha256:[a-f0-9]{64}$/;
var ROLE_RE = /^[a-z0-9][a-z0-9._:-]{0,127}$/;
var GROUPS = { tokens: 1, components: 1, surfaces: 1, drift: 1 };
var GROUP_ORDER = ['tokens', 'components', 'surfaces', 'drift'];
var GLOBAL_DOMAIN_ORDER = ['tokens', 'components'];
var DOMAIN_RE = /^(?:tokens|components|token-drift|component-drift|surface:[a-z0-9][a-z0-9_-]{0,119}|surface-drift:[a-z0-9][a-z0-9_-]{0,119})$/;
var DOMAINS_MAX = 5000;
var ARTIFACTS_MAX = programLimits.manifestArtifactsMax;
var TOKEN_COMPARISON_REPORT_DIR = 'orchestrator/.cache/figma/reports/token-comparison/';
var COMPONENT_COMPARISON_REPORT_DIR = 'orchestrator/.cache/figma/reports/component-comparison/';
var COMPONENT_INVENTORY_LOGICAL_PATH = 'orchestrator/figma/components/design-component-inventory.json';
var COMPONENT_VISUAL_LOGICAL_DIR = 'orchestrator/figma/components/visual/';
// Whole-domain atomicity for the comparison domains: these roles are
// published together or not at all (the baselines are optional — a run with
// structural blockers and no prior baseline publishes without one).
var TOKEN_DRIFT_FIXED_ROLES = ['project-token-analysis-index', 'token-binding-snapshot', 'token-mapping-snapshot', 'token-comparison'];
var COMPONENT_DRIFT_FIXED_ROLES = ['project-component-analysis-index', 'component-mapping-snapshot', 'component-comparison', 'component-mapping-suggestions', 'component-task-suggestions'];
var artifactValidators = null;

var FIXED_ARTIFACT_SCHEMAS = {
  'observed-token-source-index': 'observed-token-source-index.schema.json',
  'observed-token-catalog': 'observed-token-catalog.schema.json',
  'project-token-analysis-index': 'project-token-analysis-index.schema.json',
  'token-binding-snapshot': 'token-binding-snapshot.schema.json',
  'token-mapping-snapshot': 'token-mappings.schema.json',
  'token-comparison': 'token-comparison.schema.json',
  'token-baseline': 'token-baseline.schema.json',
  'design-component-inventory': 'design-component-inventory.schema.json',
  'project-component-analysis-index': 'project-component-analysis-index.schema.json',
  'component-mapping-snapshot': 'component-mappings.schema.json',
  'component-comparison': 'component-comparison.schema.json',
  'component-mapping-suggestions': 'component-mapping-suggestions.schema.json',
  'component-task-suggestions': 'component-task-suggestions.schema.json',
  'component-baseline': 'component-baseline.schema.json'
};

function artifactSchemaName(role) {
  if (FIXED_ARTIFACT_SCHEMAS[role]) return FIXED_ARTIFACT_SCHEMAS[role];
  if (/^project-token-inventory:[a-z0-9][a-z0-9-]{0,63}$/.test(role)) return 'project-token-inventory.schema.json';
  if (/^observed-token-source-shard:[0-9]{3}$/.test(role)) return 'observed-token-source-shard.schema.json';
  if (/^project-component-inventory:[a-z0-9][a-z0-9-]{0,63}$/.test(role)) return 'project-component-inventory.schema.json';
  return null;
}

function artifactSchemaValid(role, bytes) {
  var schemaName = artifactSchemaName(role);
  if (!schemaName) return true;
  if (!artifactValidators) artifactValidators = Object.create(null);
  var validate = artifactValidators[schemaName];
  if (!validate) {
    try {
      var ajvModule = require(path.join(__dirname, '..', '..', 'figma', 'node_modules', 'ajv'));
      var Ajv = ajvModule.default || ajvModule;
      var schemasDir = path.join(__dirname, '..', '..', 'figma', 'schemas');
      var ajv = new Ajv({ allErrors: true, strict: false });
      ajv.addFormat('date-time', { type: 'string', validate: iso });
      fs.readdirSync(schemasDir).filter(function (name) { return /\.schema\.json$/.test(name); }).sort().forEach(function (name) {
        var schema = JSON.parse(fs.readFileSync(path.join(schemasDir, name), 'utf8'));
        ajv.addSchema(schema, schema.$id || name);
      });
      validate = ajv.getSchema(schemaName);
      if (!validate) return false;
      artifactValidators[schemaName] = validate;
    } catch (error) { return false; }
  }
  var document;
  try { document = JSON.parse(bytes.toString('utf8')); } catch (error) { return false; }
  return !!validate(document);
}

function sha(bytes) { return 'sha256:' + crypto.createHash('sha256').update(bytes).digest('hex'); }
function scopedRole(prefix, hashPrefix, stem) {
  var normalized = String(stem || '').toLowerCase();
  var direct = prefix + normalized;
  return direct.length <= 128 ? direct : hashPrefix + crypto.createHash('sha256').update(normalized, 'utf8').digest('hex').slice(0, 32);
}
function surfaceIndexRole(stem) { return scopedRole('surface-index:', 'surface-index-h:', stem); }
function surfaceDriftRole(stem) { return scopedRole('surface-drift:', 'surface-drift-h:', stem); }
function exact(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}
function iso(value) {
  if (typeof value !== 'string' || !/^\d{4}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\d|3[01])T(?:[01]\d|2[0-3]):[0-5]\d:[0-5]\d\.\d{3}Z$/.test(value)) return false;
  var parsed = new Date(value);
  return Number.isFinite(parsed.getTime()) && parsed.toISOString() === value;
}
function projectRelative(file) {
  var rel = path.relative(paths.PROJECT_ROOT, file).split(path.sep).join('/');
  return !rel || rel === '..' || rel.indexOf('../') === 0 || path.isAbsolute(rel) ? null : rel;
}
function projectFile(relative) {
  if (typeof relative !== 'string' || !relative || relative.indexOf('\\') >= 0 || relative.charAt(0) === '/' || relative.split('/').indexOf('..') >= 0) return null;
  var file = path.resolve(paths.PROJECT_ROOT, relative);
  var rel = path.relative(paths.PROJECT_ROOT, file);
  return rel && rel !== '..' && !rel.startsWith('..' + path.sep) && !path.isAbsolute(rel) ? file : null;
}
function safeBytes(file, directory, max, optional) {
  try {
    var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, directory, file, max);
    if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') return null;
    return hit.bytes;
  } catch (error) {
    if (optional) return null;
    throw error;
  }
}
function manifestFile(generationId) { return path.join(GENERATIONS_DIR, generationId + '.json'); }

function generationEvidencePresent() {
  return [GENERATIONS_DIR, ARTIFACTS_DIR].some(function (directory) {
    var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, directory, GENERATION_EVIDENCE_MAX);
    if (listed.ok) return listed.names.length > 0;
    var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, path.dirname(directory), directory);
    if (inspected && inspected.status === 'missing') return false;
    throw new Error('generation-evidence-invalid');
  });
}

function logicalGroup(relative) {
  if (/^orchestrator\/figma\/tokens\//.test(relative)) return 'tokens';
  if (/^orchestrator\/figma\/components\//.test(relative)) return 'components';
  if (/^orchestrator\/\.cache\/figma\/screens\//.test(relative)) return 'surfaces';
  if (/^orchestrator\/\.cache\/figma\/reports\//.test(relative)) return 'drift';
  return null;
}

function logicalDomain(relative) {
  var group = logicalGroup(relative), match;
  if (group === 'tokens' || group === 'components') return group;
  if (group === 'surfaces') {
    match = /^orchestrator\/\.cache\/figma\/screens\/([A-Za-z0-9][A-Za-z0-9_-]{0,119})\//.exec(relative);
    return match ? 'surface:' + match[1].toLowerCase() : null;
  }
  if (group !== 'drift') return null;
  if (relative.indexOf(TOKEN_COMPARISON_REPORT_DIR) === 0) return 'token-drift';
  if (relative.indexOf(COMPONENT_COMPARISON_REPORT_DIR) === 0) return 'component-drift';
  match = /^orchestrator\/\.cache\/figma\/reports\/screen-drift-([A-Za-z0-9][A-Za-z0-9_-]{0,119})\.json$/.exec(relative);
  return match ? 'surface-drift:' + match[1].toLowerCase() : null;
}

function canonicalRolePath(entry) {
  if (entry.role === 'observed-token-source-index') {
    return entry.group === 'tokens' && entry.logicalPath === 'orchestrator/figma/tokens/source-index.json';
  }
  if (entry.role === 'observed-token-catalog') {
    return entry.group === 'tokens' && entry.logicalPath === 'orchestrator/figma/tokens/observed-token-catalog.json';
  }
  var sourceShard = /^observed-token-source-shard:([0-9]{3})$/.exec(entry.role);
  if (sourceShard) {
    return Number(sourceShard[1]) < 128 && entry.group === 'tokens' &&
      entry.logicalPath === 'orchestrator/figma/tokens/sources/' + sourceShard[1] + '.json';
  }
  if (entry.role === 'design-component-inventory') {
    return entry.group === 'components' && entry.logicalPath === COMPONENT_INVENTORY_LOGICAL_PATH;
  }
  var componentVisual = /^component-visual-evidence:([a-f0-9]{32})$/.exec(entry.role);
  if (componentVisual) {
    return entry.group === 'components' && entry.logicalPath === COMPONENT_VISUAL_LOGICAL_DIR + componentVisual[1] + '.png';
  }
  if (entry.role === 'project-token-analysis-index') {
    return entry.group === 'drift' && entry.logicalPath === TOKEN_COMPARISON_REPORT_DIR + 'analysis-index.json';
  }
  if (entry.role === 'token-mapping-snapshot') {
    return entry.group === 'drift' && entry.logicalPath === TOKEN_COMPARISON_REPORT_DIR + 'mapping-snapshot.json';
  }
  if (entry.role === 'token-binding-snapshot') {
    return entry.group === 'drift' && entry.logicalPath === TOKEN_COMPARISON_REPORT_DIR + 'binding-snapshot.json';
  }
  if (entry.role === 'token-comparison') {
    return entry.group === 'drift' && entry.logicalPath === TOKEN_COMPARISON_REPORT_DIR + 'comparison.json';
  }
  if (entry.role === 'token-baseline') {
    return entry.group === 'drift' && entry.logicalPath === TOKEN_COMPARISON_REPORT_DIR + 'baseline.json';
  }
  var projectInventory = /^project-token-inventory:([a-z0-9][a-z0-9-]{0,63})$/.exec(entry.role);
  if (projectInventory) {
    return entry.group === 'drift' &&
      entry.logicalPath === TOKEN_COMPARISON_REPORT_DIR + 'project-inventory-' + projectInventory[1] + '.json';
  }
  if (entry.role === 'project-component-analysis-index') {
    return entry.group === 'drift' && entry.logicalPath === COMPONENT_COMPARISON_REPORT_DIR + 'analysis-index.json';
  }
  if (entry.role === 'component-mapping-snapshot') {
    return entry.group === 'drift' && entry.logicalPath === COMPONENT_COMPARISON_REPORT_DIR + 'mapping-snapshot.json';
  }
  if (entry.role === 'component-comparison') {
    return entry.group === 'drift' && entry.logicalPath === COMPONENT_COMPARISON_REPORT_DIR + 'comparison.json';
  }
  if (entry.role === 'component-mapping-suggestions') {
    return entry.group === 'drift' && entry.logicalPath === COMPONENT_COMPARISON_REPORT_DIR + 'suggestions.json';
  }
  if (entry.role === 'component-task-suggestions') {
    return entry.group === 'drift' && entry.logicalPath === COMPONENT_COMPARISON_REPORT_DIR + 'task-suggestions.json';
  }
  if (entry.role === 'component-baseline') {
    return entry.group === 'drift' && entry.logicalPath === COMPONENT_COMPARISON_REPORT_DIR + 'baseline.json';
  }
  var projectComponentInventory = /^project-component-inventory:([a-z0-9][a-z0-9-]{0,63})$/.exec(entry.role);
  if (projectComponentInventory) {
    return entry.group === 'drift' &&
      entry.logicalPath === COMPONENT_COMPARISON_REPORT_DIR + 'project-inventory-' + projectComponentInventory[1] + '.json';
  }
  var surface = /^surface-index:([a-z0-9][a-z0-9_-]*)$/.exec(entry.role);
  if (surface) {
    var logical = /^orchestrator\/\.cache\/figma\/screens\/([^/]+)\/index\.json$/.exec(entry.logicalPath);
    return entry.group === 'surfaces' && !!logical && logical[1].toLowerCase() === surface[1];
  }
  var surfaceHash = /^surface-index-h:([a-f0-9]{32})$/.exec(entry.role);
  if (surfaceHash) {
    var hashLogical = /^orchestrator\/.cache\/figma\/screens\/([^/]+)\/index\.json$/.exec(entry.logicalPath);
    return entry.group === 'surfaces' && !!hashLogical && surfaceIndexRole(hashLogical[1]) === entry.role;
  }
  var surfaceDrift = /^surface-drift:([a-z0-9][a-z0-9_-]*)$/.exec(entry.role);
  if (surfaceDrift) {
    var driftLogical = /^orchestrator\/\.cache\/figma\/reports\/screen-drift-([^/]+)\.json$/.exec(entry.logicalPath);
    return entry.group === 'drift' && !!driftLogical && driftLogical[1].toLowerCase() === surfaceDrift[1];
  }
  var surfaceDriftHash = /^surface-drift-h:([a-f0-9]{32})$/.exec(entry.role);
  if (surfaceDriftHash) {
    var driftHashLogical = /^orchestrator\/.cache\/figma\/reports\/screen-drift-([^/]+)\.json$/.exec(entry.logicalPath);
    return entry.group === 'drift' && !!driftHashLogical && surfaceDriftRole(driftHashLogical[1]) === entry.role;
  }
  return true;
}

function requiredRole(entry) {
  if (!entry.required || entry.persistence !== 'committed' || !canonicalRolePath(entry)) return false;
  if (entry.group === 'tokens') return entry.role === 'observed-token-catalog' ||
    entry.role === 'observed-token-source-index' ||
    entry.role.indexOf('observed-token-source-shard:') === 0;
  if (entry.group === 'components') return entry.role === 'design-component-inventory';
  if (entry.group === 'surfaces') {
    return entry.role.indexOf('surface-index:') === 0 || entry.role.indexOf('surface-index-h:') === 0;
  }
  return entry.group === 'drift' && (entry.role === 'token-comparison' || entry.role === 'component-comparison');
}

function requiredDomainRole(entry) {
  if (!requiredRole(entry)) {
    if (!entry.required || entry.persistence !== 'committed' || !canonicalRolePath(entry)) return false;
    return entry.domain && entry.domain.indexOf('surface-drift:') === 0 &&
      entry.role === surfaceDriftRole(entry.domain.slice('surface-drift:'.length));
  }
  if (entry.domain === 'tokens') return entry.role === 'observed-token-catalog' ||
    entry.role === 'observed-token-source-index';
  if (entry.domain === 'components') return entry.role === 'design-component-inventory';
  if (entry.domain === 'token-drift') return entry.role === 'token-comparison';
  if (entry.domain === 'component-drift') return entry.role === 'component-comparison';
  return entry.domain && entry.domain.indexOf('surface:') === 0 && entry.role === surfaceIndexRole(entry.domain.slice('surface:'.length));
}

function validatePointer(value) {
  return exact(value, ['schemaVersion', 'generationId', 'manifestHash', 'committedAt']) &&
    value.schemaVersion === 2 && GENERATION_RE.test(String(value.generationId || '')) &&
    HASH_RE.test(String(value.manifestHash || '')) && iso(value.committedAt);
}

function validateCounters(value) {
  return exact(value, ['updated', 'unchanged', 'warnings']) &&
    ['updated', 'unchanged', 'warnings'].every(function (key) {
      return Number.isSafeInteger(value[key]) && value[key] >= 0;
    });
}

function artifactContractVersion(role) {
  if (role === 'project-token-analysis-index' || role === 'token-mapping-snapshot' ||
      role === 'token-comparison' || role === 'token-baseline' ||
      /^project-token-inventory:/.test(role) ||
      role === 'design-component-inventory' ||
      role === 'project-component-analysis-index' ||
      role === 'component-mapping-snapshot' ||
      role === 'component-comparison' ||
      role === 'component-mapping-suggestions' ||
      role === 'component-task-suggestions' ||
      role === 'component-baseline' ||
      /^project-component-inventory:/.test(role)) return 2;
  return 1;
}

function validateArtifactV2(entry, generationId, roles, pathsSeen, logicalPathsSeen, domains) {
  if (!exact(entry, ['role', 'group', 'domain', 'path', 'logicalPath', 'hash', 'schemaVersion', 'persistence', 'required', 'size'])) return false;
  if (!ROLE_RE.test(String(entry.role || '')) || roles[entry.role] || !GROUPS[entry.group] || !DOMAIN_RE.test(String(entry.domain || '')) ||
      !domains[entry.domain] || domains[entry.domain].group !== entry.group || logicalDomain(entry.logicalPath) !== entry.domain ||
      typeof entry.path !== 'string' || !entry.path || entry.path.length > 500 ||
      typeof entry.logicalPath !== 'string' || !entry.logicalPath || entry.logicalPath.length > 500 ||
      !HASH_RE.test(String(entry.hash || '')) || entry.schemaVersion !== artifactContractVersion(entry.role) ||
      (entry.persistence !== 'committed' && entry.persistence !== 'runtime') || typeof entry.required !== 'boolean' ||
      (entry.required && entry.persistence !== 'committed') ||
      !Number.isSafeInteger(entry.size) || entry.size < 0 || entry.size > ARTIFACT_MAX) return false;
  var file = projectFile(entry.path), logical = projectFile(entry.logicalPath);
  if (!file || !logical || projectRelative(file) !== entry.path || projectRelative(logical) !== entry.logicalPath ||
      pathsSeen[entry.path] || logicalPathsSeen[entry.logicalPath]) return false;
  var sourceGenerationId = domains[entry.domain].sourceGenerationId;
  var expected = entry.persistence === 'committed'
    ? 'orchestrator/figma/manifests/artifacts/' + sourceGenerationId + '/' + entry.group + '/'
    : 'orchestrator/.cache/figma/generations/' + sourceGenerationId + '/' + entry.group + '/';
  if (entry.path.indexOf(expected) !== 0 || !entry.path.slice(expected.length) || !canonicalRolePath(entry)) return false;
  roles[entry.role] = 1; pathsSeen[entry.path] = 1; logicalPathsSeen[entry.logicalPath] = 1;
  return true;
}

function validateManifestV2(value, expectedGenerationId) {
  var keys = ['schemaVersion', 'generationId', 'accountFingerprint', 'fileKeyFingerprint', 'createdAt', 'syncJobId',
    'updatedDomains', 'syncGroups', 'groups', 'domains', 'artifacts', 'counters'];
  if (!exact(value, keys) || value.schemaVersion !== 2 || !GENERATION_RE.test(String(value.generationId || '')) ||
      expectedGenerationId && value.generationId !== expectedGenerationId ||
      !HASH_RE.test(String(value.accountFingerprint || '')) || !HASH_RE.test(String(value.fileKeyFingerprint || '')) ||
      !iso(value.createdAt) || !SYNC_JOB_RE.test(String(value.syncJobId || '')) ||
      !Array.isArray(value.groups) || !value.groups.length || value.groups.length > GROUP_ORDER.length ||
      !Array.isArray(value.domains) || !value.domains.length || value.domains.length > DOMAINS_MAX ||
      !Array.isArray(value.updatedDomains) || !value.updatedDomains.length || value.updatedDomains.length > DOMAINS_MAX ||
      !Array.isArray(value.artifacts) || !value.artifacts.length || value.artifacts.length > ARTIFACTS_MAX ||
      !value.syncGroups || typeof value.syncGroups !== 'object' || Array.isArray(value.syncGroups) || !validateCounters(value.counters)) return false;
  var groups = Object.create(null);
  for (var g = 0; g < value.groups.length; g++) {
    if (!GROUPS[value.groups[g]] || groups[value.groups[g]]) return false;
    groups[value.groups[g]] = 1;
  }
  var domains = Object.create(null);
  for (var d = 0; d < value.domains.length; d++) {
    var lineage = value.domains[d];
    if (!exact(lineage, ['id', 'group', 'inputFingerprint', 'syncedAt', 'sourceGenerationId']) ||
        !DOMAIN_RE.test(String(lineage.id || '')) || domains[lineage.id] || !GROUPS[lineage.group] ||
        !groups[lineage.group] || !HASH_RE.test(String(lineage.inputFingerprint || '')) || !iso(lineage.syncedAt) ||
        !GENERATION_RE.test(String(lineage.sourceGenerationId || ''))) return false;
    domains[lineage.id] = lineage;
  }
  var updated = Object.create(null);
  for (var u = 0; u < value.updatedDomains.length; u++) {
    var domainId = value.updatedDomains[u];
    if (!domains[domainId] || updated[domainId] || domains[domainId].sourceGenerationId !== value.generationId) return false;
    updated[domainId] = 1;
  }
  var syncNames = Object.keys(value.syncGroups), syncCounters = { updated: 0, unchanged: 0, warnings: 0 };
  if (!syncNames.length || syncNames.length > GROUP_ORDER.length) return false;
  for (var s = 0; s < syncNames.length; s++) {
    var group = syncNames[s], summary = value.syncGroups[group];
    if (!GROUPS[group] || !exact(summary, ['status', 'updated', 'unchanged', 'warnings']) || summary.status !== 'completed' ||
        !['updated', 'unchanged', 'warnings'].every(function (key) { return Number.isSafeInteger(summary[key]) && summary[key] >= 0; }) ||
        !value.updatedDomains.some(function (id) { return domains[id].group === group; })) return false;
    syncCounters.updated += summary.updated; syncCounters.unchanged += summary.unchanged; syncCounters.warnings += summary.warnings;
  }
  if (value.updatedDomains.some(function (id) { return !value.syncGroups[domains[id].group]; }) ||
      ['updated', 'unchanged', 'warnings'].some(function (key) { return syncCounters[key] !== value.counters[key]; })) return false;
  var roles = Object.create(null), pathsSeen = Object.create(null), logicalPathsSeen = Object.create(null);
  var artifactsByDomain = Object.create(null), requiredByDomain = Object.create(null), artifactGroups = Object.create(null);
  var tokenDriftRoles = Object.create(null), componentDriftRoles = Object.create(null),
    tokenDomainRoles = Object.create(null), componentVisualCount = 0;
  for (var a = 0; a < value.artifacts.length; a++) {
    var artifact = value.artifacts[a];
    if (!validateArtifactV2(artifact, value.generationId, roles, pathsSeen, logicalPathsSeen, domains)) return false;
    artifactsByDomain[artifact.domain] = (artifactsByDomain[artifact.domain] || 0) + 1;
    artifactGroups[artifact.group] = 1;
    if (artifact.domain === 'token-drift') {
      if (artifact.persistence !== 'committed' || !artifact.required && artifact.role !== 'token-baseline') return false;
      tokenDriftRoles[artifact.role] = 1;
    }
    if (artifact.domain === 'tokens') {
      if (artifact.persistence !== 'committed' || !artifact.required) return false;
      tokenDomainRoles[artifact.role] = 1;
    }
    if (artifact.domain === 'components' &&
        /^component-visual-evidence:[a-f0-9]{32}$/.test(artifact.role)) {
      componentVisualCount++;
      if (componentVisualCount > programLimits.componentVisualArtifactsMax) return false;
    }
    if (artifact.domain === 'component-drift') {
      if (artifact.persistence !== 'committed' || !artifact.required && artifact.role !== 'component-baseline') return false;
      componentDriftRoles[artifact.role] = 1;
    }
    if (requiredDomainRole(artifact)) requiredByDomain[artifact.domain] = 1;
  }
  if (Object.keys(groups).some(function (group) { return !artifactGroups[group]; }) ||
      Object.keys(artifactGroups).some(function (group) { return !groups[group]; })) return false;
  if (domains['token-drift'] && TOKEN_DRIFT_FIXED_ROLES.some(function (role) { return !tokenDriftRoles[role]; })) {
    return false;
  }
  if (domains.tokens && (!tokenDomainRoles['observed-token-source-index'] || !tokenDomainRoles['observed-token-catalog'])) {
    return false;
  }
  if (domains['component-drift'] && COMPONENT_DRIFT_FIXED_ROLES.some(function (role) { return !componentDriftRoles[role]; })) {
    return false;
  }
  return value.domains.every(function (lineage) {
    return artifactsByDomain[lineage.id] > 0 && requiredByDomain[lineage.id];
  });
}

function validateManifest(value, expectedGenerationId) {
  return validateManifestV2(value, expectedGenerationId);
}

function isPartial(manifest) {
  if (!manifest || manifest.schemaVersion !== 2 || !Array.isArray(manifest.domains)) return true;
  var present = Object.create(null);
  manifest.domains.forEach(function (lineage) { present[lineage.id] = 1; });
  return GLOBAL_DOMAIN_ORDER.some(function (domain) { return !present[domain]; });
}

function sourceFingerprint(manifest) {
  if (!manifest || manifest.schemaVersion !== 2 || !Array.isArray(manifest.domains)) return null;
  var values = manifest.domains.map(function (lineage) {
    return { id: lineage.id, inputFingerprint: lineage.inputFingerprint };
  }).sort(function (left, right) { return left.id.localeCompare(right.id); });
  return sha(Buffer.from(JSON.stringify(values), 'utf8'));
}

function current() {
  var pointerEntry = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, MANIFESTS_DIR, POINTER_FILE);
  if (pointerEntry && pointerEntry.status === 'missing') {
    try {
      if (generationEvidencePresent()) return { ok: false, error: 'generation-pointer-missing' };
    } catch (evidenceError) { return { ok: false, error: 'generation-evidence-invalid' }; }
    return { ok: true, mode: 'none', manifest: null, availability: { missingOptional: [], missingRuntime: [] } };
  }
  if (!pointerEntry || pointerEntry.status !== 'present' || !pointerEntry.stat || !pointerEntry.stat.isFile() ||
      pointerEntry.stat.isSymbolicLink() || String(pointerEntry.stat.nlink) !== '1') return { ok: false, error: 'generation-pointer-unsafe' };
  var pointerBytes = safeBytes(POINTER_FILE, MANIFESTS_DIR, POINTER_MAX, true);
  if (!pointerBytes) return { ok: false, error: 'generation-pointer-unsafe' };
  var pointer;
  try { pointer = JSON.parse(pointerBytes.toString('utf8')); } catch (error) { return { ok: false, error: 'generation-pointer-invalid' }; }
  if (!validatePointer(pointer)) return { ok: false, error: 'generation-pointer-invalid' };
  var file = manifestFile(pointer.generationId);
  var manifestBytes = safeBytes(file, GENERATIONS_DIR, MANIFEST_MAX, true);
  if (!manifestBytes || sha(manifestBytes) !== pointer.manifestHash) return { ok: false, error: 'generation-manifest-hash-mismatch' };
  var manifest;
  try { manifest = JSON.parse(manifestBytes.toString('utf8')); } catch (parseError) { return { ok: false, error: 'generation-manifest-invalid' }; }
  if (!validateManifest(manifest, pointer.generationId)) return { ok: false, error: 'generation-manifest-invalid' };
  if (pointer.schemaVersion !== manifest.schemaVersion) return { ok: false, error: 'generation-pointer-invalid' };
  if (Date.parse(pointer.committedAt) < Date.parse(manifest.createdAt)) return { ok: false, error: 'generation-pointer-invalid' };
  var missingOptional = [], missingRuntime = [];
  for (var i = 0; i < manifest.artifacts.length; i++) {
    var entry = manifest.artifacts[i];
    var artifact = projectFile(entry.path);
    var bytes = safeBytes(artifact, path.dirname(artifact), ARTIFACT_MAX, true);
    if (!bytes || bytes.length !== entry.size || sha(bytes) !== entry.hash) {
      if (entry.persistence === 'committed' && entry.required) return { ok: false, error: 'generation-required-artifact-invalid', role: entry.role };
      missingOptional.push(entry.role);
      if (entry.persistence === 'runtime') missingRuntime.push(entry.role);
    } else if (!artifactSchemaValid(entry.role, bytes)) {
      return { ok: false, error: 'generation-artifact-schema-invalid', role: entry.role };
    }
  }
  return { ok: true, mode: 'generation', pointer: pointer, manifest: manifest,
    availability: { missingOptional: missingOptional, missingRuntime: missingRuntime } };
}

function readEntry(entry) {
  if (!entry) return null;
  var file = projectFile(entry.path);
  if (!file) return null;
  var bytes = safeBytes(file, path.dirname(file), ARTIFACT_MAX, true);
  return bytes && bytes.length === entry.size && sha(bytes) === entry.hash ? bytes : null;
}

module.exports = {
  FIGMA_DIR: FIGMA_DIR,
  TOKEN_COMPARISON_REPORT_DIR: TOKEN_COMPARISON_REPORT_DIR,
  COMPONENT_COMPARISON_REPORT_DIR: COMPONENT_COMPARISON_REPORT_DIR,
  COMPONENT_INVENTORY_LOGICAL_PATH: COMPONENT_INVENTORY_LOGICAL_PATH,
  COMPONENT_VISUAL_LOGICAL_DIR: COMPONENT_VISUAL_LOGICAL_DIR,
  MANIFESTS_DIR: MANIFESTS_DIR,
  GENERATIONS_DIR: GENERATIONS_DIR,
  ARTIFACTS_DIR: ARTIFACTS_DIR,
  POINTER_FILE: POINTER_FILE,
  MANIFEST_MAX: MANIFEST_MAX,
  POINTER_MAX: POINTER_MAX,
  ARTIFACT_MAX: ARTIFACT_MAX,
  ARTIFACTS_MAX: ARTIFACTS_MAX,
  GENERATION_RE: GENERATION_RE,
  HASH_RE: HASH_RE,
  sha: sha,
  projectRelative: projectRelative,
  projectFile: projectFile,
  manifestFile: manifestFile,
  validatePointer: validatePointer,
  validateManifest: validateManifest,
  logicalGroup: logicalGroup,
  logicalDomain: logicalDomain,
  canonicalRolePath: canonicalRolePath,
  artifactContractVersion: artifactContractVersion,
  artifactBytesContractValid: artifactSchemaValid,
  requiredRole: requiredRole,
  requiredDomainRole: requiredDomainRole,
  isPartial: isPartial,
  sourceFingerprint: sourceFingerprint,
  surfaceIndexRole: surfaceIndexRole,
  surfaceDriftRole: surfaceDriftRole,
  current: current,
  readEntry: readEntry
};
