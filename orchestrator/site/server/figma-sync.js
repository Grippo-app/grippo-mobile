'use strict';

// Server-owned independent-scope planner and job runner. Browser requests
// contain plan ids only; scope prompts, staging paths, artifact allowlists and
// publication order are all resolved here.

var crypto = require('crypto');
var path = require('path');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var finalizations = require('./finalizations');
var figma = require('./figma');
var sessions = require('./sessions');
var actions = require('./figma-session-actions');
var configUpdate = require('./project-config-update');
var testJobs = require('./figma-test-job');
var generation = require('./figma-generation');
var history = require('./figma-sync-history');
var syncErrors = require('./figma-sync-errors');
var designHistory = require('./design-history');
var tokenJobs = require('./figma-token-jobs');
var tokenHealth = require('./token-source-health-store');
var tokenSourceBootstrap = require('./token-source-bootstrap');
var tokenState = require('./design-token-state');
var componentState = require('./design-component-state');
var projectAdaptersBootstrap = require('./project-adapters-bootstrap');
var programLimits = require(path.join(paths.ORCHESTRATOR_DIR, 'figma', 'runtime', 'program-limits.cjs'));
var tokenIdentity = require(path.join(paths.ORCHESTRATOR_DIR, 'figma', 'runtime', 'token-identity.cjs'));
var projectIdentity = null;
var adapterConfigIdentity = null;
var adapterConfigSchemaValidate = null;

function projectBranchKey() {
  if (!projectIdentity) projectIdentity = require(path.join(paths.ORCHESTRATOR_DIR, 'figma', 'runtime', 'project-identity.cjs'));
  return projectIdentity.projectBranchKey(paths.PROJECT_ROOT);
}

function domainConfigIdentity(document, capability) {
  if (!adapterConfigIdentity) adapterConfigIdentity = require(path.join(paths.ORCHESTRATOR_DIR, 'figma', 'runtime', 'adapter-config-identity.cjs'));
  var canonical = adapterConfigIdentity.capabilityJson(document, capability);
  return { hash: generation.sha(Buffer.from(canonical, 'utf8')), size: Buffer.byteLength(canonical, 'utf8') };
}
function validateAdapterConfigSchema(document) {
  if (!adapterConfigSchemaValidate) {
    var ajvModule = require(path.join(paths.ORCHESTRATOR_DIR, 'figma', 'node_modules', 'ajv'));
    var Ajv = ajvModule.default || ajvModule;
    var schema = require(path.join(paths.ORCHESTRATOR_DIR, 'figma', 'schemas', 'project-adapters.schema.json'));
    adapterConfigSchemaValidate = new Ajv({ allErrors: true, strict: false }).compile(schema);
  }
  return adapterConfigSchemaValidate(document);
}

var PLAN_TTL = 5 * 60 * 1000;
var ACTION_TIMEOUT = 20 * 60 * 1000;
var PLAN_RE = /^fsp-[a-f0-9]{32}$/;
var JOB_RE = history.JOB_RE;
var SCOPE_SET = { tokens: 1, components: 1, drift: 1 };
var ESTIMATED_READS = { tokens: 4, components: 8, drift: 0 };
// The drift scope is a local deterministic runner: it never resolves a
// session action and never reaches Figma or an AI session.
var SCOPE_ACTION = { tokens: 'sync-tokens', components: 'sync-components' };
var SCOPE_SESSION = { tokens: 'figma:sync-tokens', components: 'figma:sync-components' };
// A raw Figma file snapshot can legitimately exceed 8 MiB. Keep each guarded
// read below the file-guard worker's bounded base64 response envelope while
// retaining the independent aggregate cap below.
var INPUT_FILE_MAX = 64 * 1024 * 1024;
var INPUT_TOTAL_MAX = 128 * 1024 * 1024;
var INPUT_FILES_MAX = 2000;
var STAGE_MANIFEST_MAX = 256 * 1024;
var STAGE_ARTIFACTS_MAX = 200;
var STAGE_TOTAL_MAX = 64 * 1024 * 1024;
var STAGE_CLEANUP_ENTRIES_MAX = 25000;
var GENERATION_RETENTION = 10;
var GENERATION_CLEANUP_ENTRIES_MAX = 12000;
var INPUT_ERROR_CODES = Object.freeze({
  'figma-code-input-path-invalid': 1,
  'figma-code-input-file-unsafe': 1,
  'figma-code-input-scan-limit': 1,
  'figma-adapter-config-invalid': 1,
  'figma-adapter-capability-invalid': 1,
  'figma-adapter-config-unsafe': 1,
  'figma-input-directory-unsafe': 1,
  'figma-input-scan-limit': 1,
  'figma-input-file-unsafe': 1,
  'figma-input-file-too-large': 1,
  'figma-sync-scope-invalid': 1,
  'figma-sync-identity-invalid': 1,
  'design-generation-invalid': 1,
  'figma-drift-domain-invalid': 1,
  'token-domain-resync-required': 1,
  'token-source-index-unavailable': 1,
  'token-source-index-invalid': 1,
  'token-source-scope-mismatch': 1,
  'TOKEN_SOURCE_BOOTSTRAP_INVALID': 1,
  'TOKEN_SOURCE_BOOTSTRAP_EMPTY': 1,
  'TOKEN_SOURCE_BOOTSTRAP_INPUT_UNSAFE': 1,
  'TOKEN_SOURCE_BOOTSTRAP_LIMIT_EXCEEDED': 1,
  'TOKEN_SOURCE_RESERVATION_MISSING': 1,
  'TOKEN_GENERATION_RESYNC_REQUIRED': 1,
  'TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE': 1
});
var RETENTION_ERROR_CODES = Object.freeze({
  'generation-retention-directory-invalid': 1,
  'generation-retention-entry-invalid': 1,
  'generation-retention-manifest-invalid': 1,
  'generation-retention-cleanup-failed': 1
});
var ROLE_RE = /^[a-z0-9][a-z0-9._:-]{0,127}$/;
var SOURCE_RE = /^[A-Za-z0-9][A-Za-z0-9._/-]{0,399}$/;
var plans = Object.create(null);
var jobs = Object.create(null);
var activeJobId = null;
var latestTerminalRecord = null;
var recovering = true;
var recoveryError = null;
var tokenHealthRecoveryError = null;
var tokenHealthRecoveryPromise = null;
var testActive = function () { return false; };
var notify = function () {};
var tokenIngestionTimer = null;
var tokenIngestionRunning = null;
var resetPaused = false;

function now() { return new Date().toISOString(); }
function randomId(prefix) { return prefix + '-' + crypto.randomBytes(16).toString('hex'); }
function hashObject(value) { return generation.sha(Buffer.from(JSON.stringify(value), 'utf8')); }
function closedErrorCode(error, allowed, fallback) {
  var code = error && typeof error.message === 'string' ? error.message : '';
  return Object.prototype.hasOwnProperty.call(allowed, code) ? code : fallback;
}
function scopedErrorCode(group, error) {
  return syncErrors.classify(group, error);
}
function exact(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) && Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}
function emit(job) {
  try { notify('figma-sync-job', { job: publicJob(job) }); } catch (error) {}
}
function safeRel(file) {
  var rel = path.relative(paths.PROJECT_ROOT, file).split(path.sep).join('/');
  return rel && rel !== '..' && rel.indexOf('../') !== 0 ? rel : null;
}
function artifactGroup(relative) {
  return generation.logicalGroup(relative);
}
function addCodeInput(relative, rows, budget) {
  if (typeof relative !== 'string' || !relative || path.isAbsolute(relative) || relative.indexOf('\\') >= 0 ||
      relative.split('/').some(function (segment) { return !segment || segment === '.' || segment === '..'; })) {
    throw new Error('figma-code-input-path-invalid');
  }
  var file = path.join(paths.PROJECT_ROOT, relative), rel = safeRel(file);
  if (rel !== relative) throw new Error('figma-code-input-path-invalid');
  var directory = path.dirname(file);
  var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, directory, file);
  if (inspected && inspected.status === 'missing') {
    rows.push({ path: relative, group: 'code', hash: null, size: 0, missing: true });
    return;
  }
  if (!inspected || inspected.status !== 'present' || !inspected.stat || !inspected.stat.isFile() ||
      inspected.stat.isSymbolicLink() || String(inspected.stat.nlink) !== '1') throw new Error('figma-code-input-file-unsafe');
  if (!Number.isSafeInteger(inspected.stat.size) || inspected.stat.size < 0 || inspected.stat.size > INPUT_FILE_MAX ||
      inspected.stat.size > INPUT_TOTAL_MAX - budget.bytes) throw new Error('figma-code-input-scan-limit');
  var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, directory, file, INPUT_FILE_MAX);
  if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') throw new Error('figma-code-input-file-unsafe');
  budget.files++; budget.bytes += hit.bytes.length;
  if (budget.files > INPUT_FILES_MAX || budget.bytes > INPUT_TOTAL_MAX) throw new Error('figma-code-input-scan-limit');
  rows.push({ path: relative, group: 'code', hash: generation.sha(hit.bytes), size: hit.bytes.length });
}
// The drift scope compares the active generation with project sources, so
// its plan/start/publish fingerprint must pin every comparison input the
// local runners will read: the adapter config, both mapping registries, and
// every file under each enabled adapter's configured roots (token roots plus
// component/preview/screenshot-test roots). The walk is a superset (no
// include/exclude filtering) — a change to any file under a configured root
// honestly invalidates the plan; the runner applies the exact
// include/exclude projection itself.
function adapterRootsFromDocument(document, capability) {
  if (!validateAdapterConfigSchema(document)) throw new Error('figma-adapter-config-invalid');
  if (capability !== undefined && capability !== 'tokens' && capability !== 'components') {
    throw new Error('figma-adapter-capability-invalid');
  }
  var roots = [];
  var pushRoot = function (root) {
    if (typeof root !== 'string' || !root || root.charAt(0) === '/' || root.indexOf('\\') >= 0 ||
        root.split('/').some(function (segment) { return !segment || segment === '.' || segment === '..'; })) {
      throw new Error('figma-adapter-config-invalid');
    }
    if (roots.indexOf(root) < 0) roots.push(root);
  };
  document.adapters.forEach(function (adapter) {
    if (!adapter.enabled) return;
    if ((!capability || capability === 'tokens') && adapter.capabilities.indexOf('tokens') >= 0) {
      adapter.tokens.roots.forEach(pushRoot);
    }
    if ((!capability || capability === 'components') && adapter.capabilities.indexOf('components') >= 0) {
      ['roots', 'previewRoots', 'screenshotTestRoots'].forEach(function (key) {
        (adapter.components[key] || []).forEach(pushRoot);
      });
    }
  });
  return roots.sort();
}
function adapterRootsForFingerprint(capability) {
  var configFile = path.join(paths.PROJECT_ROOT, 'orchestrator', 'figma', 'project-adapters.json');
  var directory = path.dirname(configFile);
  var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, directory, configFile);
  if (inspected && inspected.status === 'missing') return [];
  var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, directory, configFile, 256 * 1024);
  if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') throw new Error('figma-adapter-config-unsafe');
  var document;
  try { document = JSON.parse(hit.bytes.toString('utf8')); }
  catch (error) { throw new Error('figma-adapter-config-invalid'); }
  return adapterRootsFromDocument(document, capability);
}
function walkCodeInputs(root, rows, budget) {
  var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, root, INPUT_FILES_MAX - budget.entries);
  if (!listed.ok) {
    var rootEntry = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, path.dirname(root), root);
    if (rootEntry && rootEntry.status === 'missing') return;
    throw new Error('figma-input-directory-unsafe');
  }
  listed.names.sort(function (a, b) { return a.localeCompare(b); });
  for (var i = 0; i < listed.names.length; i++) {
    if (budget.files >= INPUT_FILES_MAX || budget.bytes >= INPUT_TOTAL_MAX) throw new Error('figma-input-scan-limit');
    budget.entries++;
    if (budget.entries > INPUT_FILES_MAX) throw new Error('figma-input-scan-limit');
    var file = path.join(root, listed.names[i]);
    var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, root, file);
    if (!inspected || inspected.status !== 'present' || !inspected.stat || inspected.stat.isSymbolicLink()) throw new Error('figma-input-file-unsafe');
    if (inspected.stat.isDirectory()) { walkCodeInputs(file, rows, budget); continue; }
    if (!inspected.stat.isFile()) continue;
    if (/\.(?:lock|tmp)$/.test(listed.names[i])) continue;
    var rel = safeRel(file);
    if (!rel) continue;
    if (!Number.isSafeInteger(inspected.stat.size) || inspected.stat.size < 0) throw new Error('figma-input-file-unsafe');
    if (inspected.stat.size > INPUT_FILE_MAX) throw new Error('figma-input-file-too-large');
    if (inspected.stat.size > INPUT_TOTAL_MAX - budget.bytes) throw new Error('figma-input-scan-limit');
    var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, root, file, INPUT_FILE_MAX);
    if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') throw new Error('figma-input-file-unsafe');
    budget.files++; budget.bytes += hit.bytes.length;
    if (budget.bytes > INPUT_TOTAL_MAX) throw new Error('figma-input-scan-limit');
    rows.push({ path: rel, group: 'code', hash: generation.sha(hit.bytes), size: hit.bytes.length });
  }
}
function addCompareInputs(rows, budget, capability) {
  var configRelative = 'orchestrator/figma/project-adapters.json';
  var configRow = rows.length;
  addCodeInput(configRelative, rows, budget);
  if (capability && rows[configRow] && !rows[configRow].missing) {
    var configFile = path.join(paths.PROJECT_ROOT, configRelative);
    var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, path.dirname(configFile), configFile, 256 * 1024);
    if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') throw new Error('figma-adapter-config-unsafe');
    var document;
    try { document = JSON.parse(hit.bytes.toString('utf8')); }
    catch (error) { throw new Error('figma-adapter-config-invalid'); }
    try {
      var identity = domainConfigIdentity(document, capability);
      rows[configRow] = { path: configRelative + '#' + capability, group: 'config',
        hash: identity.hash, size: identity.size };
    } catch (error) { throw new Error('figma-adapter-config-invalid'); }
  }
  if (!capability || capability === 'tokens') addCodeInput('orchestrator/figma/token-mappings.json', rows, budget);
  if (!capability || capability === 'components') addCodeInput('orchestrator/figma/component-mappings.json', rows, budget);
  adapterRootsForFingerprint(capability).forEach(function (root) {
    walkCodeInputs(path.join(paths.PROJECT_ROOT, root), rows, budget);
  });
}
function inputSnapshot(identity, scope, activeGeneration) {
  if (!Object.prototype.hasOwnProperty.call(SCOPE_SET, scope)) throw new Error('figma-sync-scope-invalid');
  if (!identity || typeof identity.accountFingerprint !== 'string' || typeof identity.fileKeyFingerprint !== 'string') {
    throw new Error('figma-sync-identity-invalid');
  }
  if (!activeGeneration || !activeGeneration.ok ||
      activeGeneration.mode !== 'none' && activeGeneration.mode !== 'generation') {
    throw new Error('design-generation-invalid');
  }
  var rows = [], budget = { entries: 0, files: 0, bytes: 0 };
  var branchKey = null;
  if (scope === 'drift') {
    addCompareInputs(rows, budget);
    branchKey = projectBranchKey();
  }
  rows.sort(function (a, b) { return a.path.localeCompare(b.path); });
  return {
    fingerprint: hashObject({ scope: scope, configRevision: identity.configRevision, accountFingerprint: identity.accountFingerprint,
      fileKeyFingerprint: identity.fileKeyFingerprint,
      branchKey: branchKey,
      generationRevision: activeGeneration.mode === 'generation' ? activeGeneration.pointer.manifestHash : null,
      inputs: rows }),
    files: rows
  };
}

function domainGenerationInputs(activeGeneration, capability) {
  var roles = capability === 'tokens'
    ? ['observed-token-source-index', 'observed-token-catalog', 'token-baseline']
    : ['design-component-inventory', 'component-baseline', 'token-comparison', 'token-mapping-snapshot'];
  return roles.map(function (role) {
    var entry = activeGeneration.manifest.artifacts.find(function (candidate) { return candidate.role === role; });
    return { role: role, hash: entry ? entry.hash : null, sourceGenerationId: entry ? entry.path.split('/')[4] || null : null };
  });
}

// Exact per-domain identity used by standalone compare jobs. Unlike the
// interactive all-drift plan, this deliberately excludes the other domain:
// component roots/registry never invalidate token comparison, while the
// component identity includes the exact token comparison+mapping pair used
// for directed token-causality analysis.
function domainInputSnapshot(identity, capability, activeGeneration) {
  if (capability !== 'tokens' && capability !== 'components') throw new Error('figma-drift-domain-invalid');
  var rows = [], budget = { entries: 0, files: 0, bytes: 0 };
  addCompareInputs(rows, budget, capability);
  rows.sort(function (a, b) { return a.path.localeCompare(b.path); });
  var branchKey = projectBranchKey();
  return {
    fingerprint: hashObject({
      domain: capability + '-drift',
      accountFingerprint: identity.accountFingerprint,
      fileKeyFingerprint: identity.fileKeyFingerprint,
      branchKey: branchKey,
      generationInputs: domainGenerationInputs(activeGeneration, capability),
      inputs: rows
    }),
    files: rows
  };
}
function quotaWarnings(account) {
  var tier = String(account && account.tier || '').toLowerCase(), seat = String(account && account.seat || '').toLowerCase();
  return tier === 'starter' || /\b(?:view|viewer|collab)\b/.test(seat)
    ? [{ code: 'quota-risk', message: 'The verified Figma plan or seat may have a limited MCP read quota.' }] : [];
}
function connectorSessionActive() {
  try {
    var active = sessions.list();
    return Object.keys(active).some(function (key) {
      return active[key] && active[key].running && (key.indexOf('figma:') === 0 || key.indexOf('task:') === 0);
    });
  } catch (error) { return true; }
}
function readiness() {
  var config = configUpdate.read();
  if (!config.ok) return { ok: false, error: config.error || 'project-config-unavailable', config: config };
  if (config.figmaFieldState === 'invalid') return { ok: false, error: 'file-invalid', config: config };
  if (!config.figmaFileKey) return { ok: false, error: 'file-missing', config: config };
  var connector = figma.status();
  if (connector && connector.global && connector.global.present) return { ok: false, error: 'connector-conflict', config: config };
  if (!connector || connector.state === 'unknown') return { ok: false, error: 'connector-unavailable', config: config };
  if (connector.state === 'needs-auth') return { ok: false, error: 'auth-required', config: config };
  if (connector.state === 'misconfigured') return { ok: false, error: 'connector-conflict', config: config };
  if (connector.state !== 'connected' || !connector.local || !connector.local.present) return { ok: false, error: 'connector-missing', config: config };
  var account = figma.account(), accountHash = testJobs.accountFingerprint(account);
  if (!account || !accountHash || !testJobs.fresh(account.checkedAt, testJobs.ACCOUNT_TTL)) return { ok: false, error: 'account-stale', config: config };
  var access = testJobs.accessFor(config.figmaFileKey, accountHash);
  if (!access || access.state !== 'verified') return { ok: false, error: access && access.reasonCode || 'access-unverified', config: config };
  return { ok: true, config: config, account: account, accountFingerprint: accountHash, access: access };
}
function comparisonSourceAvailable(activeGeneration) {
  return !!(activeGeneration && activeGeneration.ok && activeGeneration.mode === 'generation' &&
    activeGeneration.manifest && Array.isArray(activeGeneration.manifest.artifacts) &&
    activeGeneration.manifest.artifacts.some(function (entry) {
      return entry && (entry.role === 'observed-token-catalog' || entry.role === 'design-component-inventory');
    }));
}

function tokenSourceBucket(sourceId) {
  try { return tokenIdentity.sourceBucket(sourceId); }
  catch (error) { return -1; }
}

function tokenSourceSnapshot(activeGeneration, expectedScope, includeRetiredSourceIds) {
  if (!activeGeneration || !activeGeneration.ok) throw new Error('design-generation-invalid');
  if (activeGeneration.mode === 'none') {
    var emptyRevision = hashObject({ state: 'uninitialized', scope: expectedScope });
    return { sourceIndexHash: emptyRevision, revision: 0, scope: expectedScope, sources: [],
      fingerprint: hashObject({ state: 'uninitialized', scope: expectedScope }) };
  }
  var entry = activeGeneration.manifest.artifacts.find(function (candidate) {
    return candidate.role === 'observed-token-source-index';
  });
  if (!entry) throw new Error('token-domain-resync-required');
  var bytes = generation.readEntry(entry), index;
  if (!bytes) throw new Error('token-source-index-unavailable');
  try { index = JSON.parse(bytes.toString('utf8')); } catch (error) { throw new Error('token-source-index-invalid'); }
  if (!index || index.schemaVersion !== 1 || !/^sha256:[a-f0-9]{64}$/.test(String(index.semanticHash || '')) ||
      !index.scope || index.scope.fileKeyFingerprint !== expectedScope.fileKeyFingerprint ||
      index.scope.branchKey !== expectedScope.branchKey || !Array.isArray(index.sources)) {
    throw new Error('token-source-scope-mismatch');
  }
  var includeRetired = Object.create(null);
  (includeRetiredSourceIds || []).forEach(function (sourceId) { includeRetired[sourceId] = 1; });
  var sources = index.sources.filter(function (source) {
    return source.lifecycle === 'active' ||
      source.lifecycle === 'retired' && includeRetired[source.sourceId];
  }).map(function (source) {
    if (!source.origins || !source.origins.length || tokenSourceBucket(source.sourceId) < 0) throw new Error('token-source-index-invalid');
    return {
      sourceId: source.sourceId,
      lifecycle: source.lifecycle,
      nodeId: source.nodeId,
      kind: source.kind,
      context: source.context,
      origin: source.origins[0],
      origins: source.origins,
      acceptedSequence: source.acceptedBatch.captureSequence
    };
  }).sort(function (left, right) { return left.sourceId.localeCompare(right.sourceId); });
  return {
    sourceIndexHash: index.semanticHash,
    revision: index.revision,
    scope: index.scope,
    sources: sources,
    fingerprint: hashObject({ sourceIndexHash: index.semanticHash, revision: index.revision, sources: sources })
  };
}

function tokenCapturePlan(snapshot, accountFingerprint, reservations) {
  var bySource = Object.create(null);
  reservations.forEach(function (reservation) { bySource[reservation.sourceId] = reservation; });
  return {
    sourceIndexHash: snapshot.sourceIndexHash,
    revision: snapshot.revision + 1,
    scope: snapshot.scope,
    records: snapshot.sources.map(function (source) {
      var reservation = bySource[source.sourceId];
      if (!reservation) throw new Error('TOKEN_SOURCE_RESERVATION_MISSING');
      var captureOperationId = reservation.captureOperationId;
      var captureSequence = reservation.captureSequence;
      var fullSource = {
        sourceId: source.sourceId,
        fileKeyFingerprint: snapshot.scope.fileKeyFingerprint,
        branchKey: snapshot.scope.branchKey,
        nodeId: source.nodeId,
        kind: source.kind,
        context: source.context,
        origin: source.origin
      };
      return {
        bucket: tokenSourceBucket(source.sourceId),
        captureOperationId: captureOperationId,
        captureSequence: captureSequence,
        accountFingerprint: accountFingerprint,
        connectorRevision: 'figma-mcp-session-v1',
        semanticPreflightHash: hashObject({
          captureOperationId: captureOperationId,
          captureSequence: captureSequence,
          sourceId: source.sourceId
        }),
        source: fullSource,
        origins: source.origins
      };
    })
  };
}
function bootstrapTokenSourceSnapshot(snapshot, context) {
  if (snapshot.sources.length) return snapshot;
  var bootstrap = tokenSourceBootstrap.discover({
    scope: snapshot.scope,
    figmaFileKey: context.fileKey,
    figmaLibraryUrl: context.figmaLibraryUrl
  });
  var sources = bootstrap.sources;
  return {
    sourceIndexHash: snapshot.sourceIndexHash,
    revision: snapshot.revision,
    scope: snapshot.scope,
    sources: sources,
    fingerprint: hashObject({
      sourceIndexHash: snapshot.sourceIndexHash,
      revision: snapshot.revision,
      sources: sources
    })
  };
}
function componentTokenCapturePlan(snapshot, accountFingerprint, reservations, prospectiveReservations) {
  var bySource = Object.create(null);
  reservations.forEach(function (reservation) { bySource[reservation.sourceId] = reservation; });
  return {
    schemaVersion: 1,
    sourceIndexHash: snapshot.sourceIndexHash,
    revision: snapshot.revision + 1,
    scope: snapshot.scope,
    accountFingerprint: accountFingerprint,
    connectorRevision: 'figma-mcp-session-v1',
    componentSourceScopeId: 'component-scope:' + hashObject({
      fileKeyFingerprint: snapshot.scope.fileKeyFingerprint,
      branchKey: snapshot.scope.branchKey,
      scope: 'all-pages'
    }).slice('sha256:'.length),
    newSourceReservations: prospectiveReservations.map(function (reservation) {
      return {
        captureOperationId: reservation.captureOperationId,
        captureSequence: reservation.captureSequence
      };
    }),
    knownSources: snapshot.sources.map(function (source) {
      var reservation = bySource[source.sourceId];
      if (!reservation) throw new Error('TOKEN_SOURCE_RESERVATION_MISSING');
      return {
        sourceId: source.sourceId,
        captureOperationId: reservation.captureOperationId,
        captureSequence: reservation.captureSequence,
        componentOwned: (source.origins || []).some(function (origin) {
          return origin.kind === 'component-inventory';
        }),
        origins: source.origins || []
      };
    }).sort(function (left, right) { return left.sourceId.localeCompare(right.sourceId); }),
    captureShardFiles: Array.from({ length: 128 }, function (_, bucket) {
      return 'component-token-intake/' + String(bucket).padStart(3, '0') + '.json';
    })
  };
}
function tokenHealthErrorCode(value) {
  return syncErrors.tokenHealthCode(value);
}
function publishedTokenSourceIndexHash(generationId) {
  var active = generation.current();
  if (!active.ok || active.mode !== 'generation' || active.manifest.generationId !== generationId) return null;
  var entry = active.manifest.artifacts.find(function (candidate) {
    return candidate.domain === 'tokens' && candidate.role === 'observed-token-source-index';
  });
  var bytes = entry && generation.readEntry(entry), value;
  if (!bytes) return null;
  try { value = JSON.parse(bytes.toString('utf8')); } catch (error) { return null; }
  return value && generation.HASH_RE.test(String(value.semanticHash || '')) ? value.semanticHash : null;
}
function settleTokenHealth(job, outcome, options) {
  options = options || {};
  if (!job.tokenHealthReservation || job.tokenHealthSettled) return true;
  var errorCode = tokenHealthErrorCode(options.errorCode);
  try {
    tokenHealth.complete({
      sourceIndexHash: options.sourceIndexHash || job.tokenCapturePlan.sourceIndexHash,
      reservations: job.tokenHealthReservation.reservations,
      prospectiveReservations: job.tokenHealthReservation.prospectiveReservations || [],
      outcome: outcome,
      evidenceSources: options.evidenceSources,
      errorCode: errorCode,
      retryable: options.retryable !== false,
      jobId: job.id,
      startedAt: job.startedAt,
      summaryOutcome: options.summaryOutcome,
      action: job.scope === 'components' ? 'component-token-capture' : 'refresh-known-token-sources',
      unusedReservations: job.tokenHealthReservation.unusedReservations || []
    });
    job.tokenHealthSettled = true;
    return true;
  } catch (error) {
    job.tokenHealthRecoveryRequired = true;
    job.messages = job.messages.concat([
      'Token design publication state is intact, but source health requires recovery: ' +
      String(error && error.message || error).slice(0, 180)
    ]).slice(-history.MESSAGE_MAX);
    return false;
  }
}
function scopeContext(scope) {
  var activeGeneration = generation.current();
  if (!activeGeneration.ok) return { ok: false, error: 'design-generation-invalid' };
  if (scope === 'drift') {
    if (!comparisonSourceAvailable(activeGeneration)) return { ok: false, error: 'design-source-not-synced' };
    return {
      ok: true,
      activeGeneration: activeGeneration,
      configRevision: null,
      accountFingerprint: activeGeneration.manifest.accountFingerprint,
      fileKeyFingerprint: activeGeneration.manifest.fileKeyFingerprint,
      fileKey: null,
      account: null
    };
  }
  var ready = readiness();
  if (!ready.ok) return { ok: false, error: ready.error };
  return {
    ok: true,
    activeGeneration: activeGeneration,
    configRevision: ready.config.revision,
    accountFingerprint: ready.accountFingerprint,
    fileKeyFingerprint: testJobs.fileKeyFingerprint(ready.config.figmaFileKey),
    fileKey: ready.config.figmaFileKey,
    figmaLibraryUrl: ready.config.figmaLibraryUrl,
    account: ready.account
  };
}
function prunePlans() {
  Object.keys(plans).forEach(function (id) { if (Date.parse(plans[id].expiresAt) <= Date.now()) delete plans[id]; });
}
function plan(request) {
  if (resetPaused) return { ok: false, status: 409, error: 'figma-sync-active' };
  request = request || {};
  var requestShape = exact(request, ['scope']) || exact(request, ['scope', 'domain']);
  var scoped = requestShape && typeof request.scope === 'string' &&
    Object.prototype.hasOwnProperty.call(SCOPE_SET, request.scope) ? request.scope : null;
  var comparisonDomain = Object.prototype.hasOwnProperty.call(request, 'domain') ? request.domain : null;
  if (!scoped || comparisonDomain !== null &&
      (scoped !== 'drift' || comparisonDomain !== 'tokens' && comparisonDomain !== 'components')) {
    return { ok: false, status: 400, error: 'bad-sync-plan-request' };
  }
  if (recovering) return { ok: false, status: 409, error: 'figma-sync-recovering' };
  if (recoveryError) return { ok: false, status: 409, error: 'figma-sync-recovery-failed' };
  if (activeJobId && jobs[activeJobId]) return { ok: false, status: 409, error: 'sync-already-running' };
  if (testActive()) return { ok: false, status: 409, error: 'figma-test-active' };
  if (connectorSessionActive()) return { ok: false, status: 409, error: 'figma-session-active' };
  prunePlans();
  // The template owns one conventional Compose layout. Its first Figma action
  // materializes the strict project-owned adapter contract automatically.
  // Existing/non-standard configs are never overwritten.
  var adapterBootstrap = projectAdaptersBootstrap.ensure();
  if (!adapterBootstrap.ok && adapterBootstrap.error) {
    return { ok: false, status: 409, error: adapterBootstrap.error };
  }
  var context = scopeContext(scoped);
  if (!context.ok) return { ok: false, status: 409, error: context.error };
  var inputs;
  var tokenSources = null;
  try {
    inputs = comparisonDomain
      ? domainInputSnapshot(context, comparisonDomain, context.activeGeneration)
      : inputSnapshot(context, scoped, context.activeGeneration);
    if (scoped === 'tokens' || scoped === 'components') {
      tokenSources = tokenSourceSnapshot(context.activeGeneration, {
        fileKeyFingerprint: context.fileKeyFingerprint,
        branchKey: projectBranchKey()
      });
      if (scoped === 'tokens') tokenSources = bootstrapTokenSourceSnapshot(tokenSources, context);
    }
  }
  catch (error) {
    return { ok: false, status: 409, error: closedErrorCode(error, INPUT_ERROR_CODES, 'figma-input-scan-failed') };
  }
  var id = randomId('fsp'), createdAt = now();
  var warnings = scoped === 'drift' ? [] : quotaWarnings(context.account);
  var value = plans[id] = {
    id: id,
    fingerprint: inputs.fingerprint,
    groups: scoped === 'components' ? ['components', 'tokens'] : [scoped],
    scope: scoped,
    comparisonDomain: comparisonDomain,
    mode: 'targeted',
    estimatedReads: scoped === 'tokens' ? tokenSources.sources.length :
      scoped === 'components' ? Math.max(ESTIMATED_READS.components, tokenSources.sources.filter(function (source) {
        return source.kind === 'component';
      }).length) : ESTIMATED_READS[scoped],
    warnings: warnings,
    createdAt: createdAt,
    expiresAt: new Date(Date.parse(createdAt) + PLAN_TTL).toISOString(),
    configRevision: context.configRevision,
    accountFingerprint: context.accountFingerprint,
    fileKeyFingerprint: context.fileKeyFingerprint,
    fileKey: context.fileKey,
    tokenSourceFingerprint: tokenSources && tokenSources.fingerprint
  };
  return { ok: true, status: 200, plan: publicPlan(value) };
}
function startSourceReactivation(request) {
  if (resetPaused) return { ok: false, status: 409, error: 'figma-sync-active' };
  if (!exact(request || {}, [
    'sourceId', 'expectedGenerationRevision', 'expectedSourceIndexHash',
    'expectedSourceIndexRevision', 'mutationId', 'jobId'
  ]) || !/^otsrc:sha256:[a-f0-9]{64}$/.test(String(request.sourceId || '')) ||
      !generation.HASH_RE.test(String(request.expectedGenerationRevision || '')) ||
      !generation.HASH_RE.test(String(request.expectedSourceIndexHash || '')) ||
      !Number.isSafeInteger(request.expectedSourceIndexRevision) || request.expectedSourceIndexRevision < 0 ||
      !/^tsm_[A-Za-z0-9_-]{16,96}$/.test(String(request.mutationId || '')) ||
      !JOB_RE.test(String(request.jobId || ''))) {
    return { ok: false, status: 400, error: 'bad-token-source-reactivation-request' };
  }
  var existing = jobs[request.jobId] ? publicJob(jobs[request.jobId]) : history.read(request.jobId);
  if (existing) return { ok: true, status: 202, job: existing };
  if (recovering) return { ok: false, status: 409, error: 'figma-sync-recovering' };
  if (recoveryError) return { ok: false, status: 409, error: 'figma-sync-recovery-failed' };
  if (activeJobId && jobs[activeJobId]) return { ok: false, status: 409, error: 'sync-already-running' };
  if (testActive()) return { ok: false, status: 409, error: 'figma-test-active' };
  if (connectorSessionActive()) return { ok: false, status: 409, error: 'figma-session-active' };
  prunePlans();
  var context = scopeContext('tokens');
  if (!context.ok) return { ok: false, status: 409, error: context.error };
  if (context.activeGeneration.mode !== 'generation' ||
      context.activeGeneration.pointer.manifestHash !== request.expectedGenerationRevision) {
    return { ok: false, status: 409, error: 'token-source-cas-conflict' };
  }
  var inputs, tokenSources;
  try {
    inputs = inputSnapshot(context, 'tokens', context.activeGeneration);
    tokenSources = tokenSourceSnapshot(context.activeGeneration, {
      fileKeyFingerprint: context.fileKeyFingerprint,
      branchKey: projectBranchKey()
    }, [request.sourceId]);
  } catch (error) {
    return { ok: false, status: 409, error: closedErrorCode(error, INPUT_ERROR_CODES, 'figma-input-scan-failed') };
  }
  if (tokenSources.sourceIndexHash !== request.expectedSourceIndexHash ||
      tokenSources.revision !== request.expectedSourceIndexRevision) {
    return { ok: false, status: 409, error: 'token-source-cas-conflict' };
  }
  var target = tokenSources.sources.find(function (source) { return source.sourceId === request.sourceId; });
  if (!target || target.lifecycle !== 'retired') {
    return { ok: false, status: 409, error: 'token-source-not-retired' };
  }
  var id = randomId('fsp'), createdAt = now();
  var warnings = quotaWarnings(context.account);
  plans[id] = {
    id: id,
    fingerprint: inputs.fingerprint,
    groups: ['tokens'],
    scope: 'tokens',
    comparisonDomain: null,
    mode: 'targeted-reactivation',
    estimatedReads: tokenSources.sources.length,
    warnings: warnings,
    createdAt: createdAt,
    expiresAt: new Date(Date.parse(createdAt) + PLAN_TTL).toISOString(),
    configRevision: context.configRevision,
    accountFingerprint: context.accountFingerprint,
    fileKeyFingerprint: context.fileKeyFingerprint,
    fileKey: context.fileKey,
    tokenSourceFingerprint: tokenSources.fingerprint,
    reactivationSourceId: request.sourceId
  };
  return start({
    planId: id,
    warningsAcknowledged: warnings.map(function (warning) { return warning.code; })
  }, { jobId: request.jobId });
}
function publicPlan(value) {
  return { id: value.id, fingerprint: value.fingerprint, scope: value.scope, groups: value.groups.slice(), mode: value.mode, estimatedReads: value.estimatedReads,
    comparisonDomain: value.comparisonDomain || null,
    warnings: value.warnings.map(function (warning) { return { code: warning.code }; }), createdAt: value.createdAt, expiresAt: value.expiresAt };
}
function publicJob(job) {
  if (!job) return null;
  return {
    id: job.id, revision: job.revision, state: job.state, phase: job.phase, progress: job.progress,
    groups: job.groups.map(function (group) { return Object.assign({}, group); }),
    startedAt: job.startedAt, finishedAt: job.finishedAt, committedGenerationId: job.committedGenerationId,
    result: job.result, errorCode: job.errorCode, comparisonDomain: job.comparisonDomain || null
  };
}
function historyRecord(job) {
  var finished = job.finishedAt;
  return {
    schemaVersion: 1, id: job.id, startedAt: job.startedAt, finishedAt: finished,
    committedGenerationId: job.committedGenerationId, accountFingerprint: job.accountFingerprint,
    fileKeyFingerprint: job.fileKeyFingerprint, planGroups: job.planGroups.slice(),
    groups: job.groups.map(function (group) { return { group: group.group, status: group.status, updated: group.updated, unchanged: group.unchanged, warnings: group.warnings }; }),
    result: job.result, errorCode: job.errorCode,
    durationMs: finished ? Math.max(0, Date.parse(finished) - Date.parse(job.startedAt)) : null,
    messages: job.messages.slice(-history.MESSAGE_MAX)
  };
}
function remember(job) {
  return history.write(historyRecord(job)).then(function () { return true; }, function (error) {
    recoveryError = 'sync-history-runtime-failed';
    console.error('[figma-sync] history write failed:', error && error.message || error);
    return false;
  });
}
function syncHistoryError() {
  var error = new Error('sync-history-unavailable');
  error.figmaSyncFatal = true;
  return error;
}
function publicMessage(job, message) {
  var value = String(message == null ? '' : message);
  if (job && job.fileKey) value = value.split(job.fileKey).join('[file-key-redacted]');
  value = history.redact(value, 'message', 0);
  return typeof value === 'string' ? value.slice(0, 2048) : '';
}
function push(job, message) { job.messages.push(publicMessage(job, message)); if (job.messages.length > history.MESSAGE_MAX) job.messages.shift(); job.revision++; emit(job); remember(job); }
function waitFor(check, timeoutMs) {
  return new Promise(function (resolve, reject) {
    var started = Date.now();
    function tick() {
      var result;
      try { result = check(); } catch (error) { reject(error); return; }
      if (result && result.done) { resolve(result.value); return; }
      if (Date.now() - started > timeoutMs) { reject(new Error('sync-action-timeout')); return; }
      var timer = setTimeout(tick, 500); if (typeof timer.unref === 'function') timer.unref();
    }
    tick();
  });
}
function runGroupSession(job, group, stagePath) {
  var key = SCOPE_SESSION[group], action = SCOPE_ACTION[group];
  var context = {
    jobId: job.id, inputFingerprint: job.inputFingerprint,
    fileKeyFingerprint: job.fileKeyFingerprint, stagePath: safeRel(stagePath)
  };
  if (group !== 'drift') context.figmaFileKey = job.fileKey;
  if (group === 'tokens') context.capturePlanPath = safeRel(path.join(stagePath, 'capture-plan.json'));
  if (group === 'components') context.tokenCapturePlanPath = safeRel(path.join(stagePath, 'component-token-plan.json'));
  return actions.resolveServerAction(key, action, context).then(function (resolved) {
    if (!resolved.ok) throw new Error(resolved.error || 'sync-action-invalid');
    return waitFor(function () { var current = sessions.status(key); return sessions.settled(current) ? { done: true } : { done: false }; }, 60 * 1000).then(function () {
      var started = sessions.start(key, { action: resolved.action, prompt: resolved.prompt, runtimeOnly: true });
      if (!started || !started.running || started.error) throw new Error('sync-session-start-refused');
      return waitFor(function () {
        var current = sessions.status(key);
        return sessions.settled(current) ? { done: true } : { done: false };
      }, ACTION_TIMEOUT);
    });
  });
}
function safeStageFile(base, relative) {
  if (!SOURCE_RE.test(String(relative || '')) || relative.charAt(0) === '/' || relative.indexOf('\\') >= 0 || relative.split('/').indexOf('..') >= 0) return null;
  var file = path.resolve(base, relative), rel = path.relative(base, file);
  return rel && rel !== '..' && !rel.startsWith('..' + path.sep) && !path.isAbsolute(rel) ? file : null;
}
function allowedScopeArtifact(scope, entry) {
  var group = artifactGroup(entry.logicalPath), domain = generation.logicalDomain(entry.logicalPath);
  if (group !== scope && !(scope === 'components' && group === 'tokens')) return false;
  var canonical = { group: group, domain: domain, role: entry.role, logicalPath: entry.logicalPath };
  if (scope === 'tokens') {
    return domain === 'tokens' &&
      (entry.role === 'observed-token-source-index' ||
        entry.role === 'observed-token-catalog' ||
        /^observed-token-source-shard:[0-9]{3}$/.test(entry.role)) &&
      generation.canonicalRolePath(canonical);
  }
  if (scope === 'components') {
    return (domain === 'components' &&
        (entry.role === 'design-component-inventory' ||
          /^component-visual-evidence:[a-f0-9]{32}$/.test(entry.role)) ||
      domain === 'tokens' &&
        (entry.role === 'observed-token-source-index' ||
          entry.role === 'observed-token-catalog' ||
          /^observed-token-source-shard:[0-9]{3}$/.test(entry.role))) &&
      generation.canonicalRolePath(canonical);
  }
  // The drift scope publishes two independent domains: the token comparison
  // group and the component comparison group.
  if (domain === 'token-drift') {
    return (entry.role === 'token-comparison' ||
      entry.role === 'project-token-analysis-index' ||
      entry.role === 'token-binding-snapshot' ||
      entry.role === 'token-mapping-snapshot' ||
      entry.role === 'token-baseline' ||
      /^project-token-inventory:[a-z0-9][a-z0-9-]{0,63}$/.test(entry.role)) &&
      generation.canonicalRolePath(canonical);
  }
  if (domain === 'component-drift') {
    return (entry.role === 'component-comparison' ||
      entry.role === 'project-component-analysis-index' ||
      entry.role === 'component-mapping-snapshot' ||
      entry.role === 'component-mapping-suggestions' ||
      entry.role === 'component-task-suggestions' ||
      entry.role === 'component-baseline' ||
      /^project-component-inventory:[a-z0-9][a-z0-9-]{0,63}$/.test(entry.role)) &&
      generation.canonicalRolePath(canonical);
  }
  return false;
}
function validateStage(job, group, directory) {
  var manifestFile = path.join(directory, 'artifacts.json');
  var manifestHit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, directory, manifestFile, STAGE_MANIFEST_MAX);
  if (!manifestHit || !manifestHit.stat || String(manifestHit.stat.nlink) !== '1') throw new Error('stage-manifest-missing');
  var value;
  try { value = JSON.parse(manifestHit.bytes.toString('utf8')); } catch (error) { throw new Error('stage-manifest-invalid'); }
  if (!exact(value, ['schemaVersion', 'jobId', 'group', 'inputFingerprint', 'fileKeyFingerprint', 'artifacts', 'messages']) ||
      value.schemaVersion !== 1 || value.jobId !== job.id || value.group !== group || value.inputFingerprint !== job.inputFingerprint ||
      value.fileKeyFingerprint !== job.fileKeyFingerprint || !Array.isArray(value.artifacts) || !value.artifacts.length || value.artifacts.length > STAGE_ARTIFACTS_MAX ||
      !Array.isArray(value.messages) || value.messages.length > 100 || value.messages.some(function (message) {
        return typeof message !== 'string' || message.length > 2048 || /[\x00-\x08\x0b\x0c\x0e-\x1f\x7f]/.test(message);
      })) throw new Error('stage-manifest-contract-invalid');
  var roles = Object.create(null), logicalPaths = Object.create(null), sources = Object.create(null), total = 0, requiredRole = false;
  var artifacts = value.artifacts.map(function (entry) {
    if (!exact(entry, ['source', 'logicalPath', 'role', 'persistence', 'required', 'schemaVersion']) || !ROLE_RE.test(String(entry.role || '')) || roles[entry.role] ||
        (entry.persistence !== 'committed' && entry.persistence !== 'runtime') || typeof entry.required !== 'boolean' ||
        (entry.required && entry.persistence !== 'committed') ||
        entry.schemaVersion !== generation.artifactContractVersion(entry.role) || typeof entry.logicalPath !== 'string' ||
        (entry.logicalPath.indexOf('orchestrator/figma/') !== 0 && entry.logicalPath.indexOf('orchestrator/.cache/figma/') !== 0) ||
        entry.logicalPath.length > 500 || logicalPaths[entry.logicalPath] || !allowedScopeArtifact(group, entry) ||
        entry.logicalPath.split('/').indexOf('..') >= 0 ||
        !generation.projectFile(entry.logicalPath) ||
        generation.projectRelative(generation.projectFile(entry.logicalPath)) !== entry.logicalPath) throw new Error('stage-artifact-contract-invalid');
    var file = safeStageFile(directory, entry.source);
    var normalizedSource = file && path.relative(directory, file).split(path.sep).join('/');
    if (!file || file === manifestFile || normalizedSource !== entry.source || sources[normalizedSource]) throw new Error('stage-artifact-path-invalid');
    var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, path.dirname(file), file, generation.ARTIFACT_MAX);
    if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') throw new Error('stage-artifact-unsafe');
    total += hit.bytes.length; if (total > STAGE_TOTAL_MAX) throw new Error('stage-artifact-size-limit');
    roles[entry.role] = 1; logicalPaths[entry.logicalPath] = 1; sources[normalizedSource] = 1;
    if (generation.requiredRole({ group: group, role: entry.role, logicalPath: entry.logicalPath,
      required: entry.required, persistence: entry.persistence })) requiredRole = true;
    return { source: entry.source, logicalPath: entry.logicalPath, role: entry.role, persistence: entry.persistence, required: entry.required,
      schemaVersion: entry.schemaVersion, bytes: hit.bytes, hash: generation.sha(hit.bytes), size: hit.bytes.length };
  });
  if (!requiredRole) throw new Error('stage-required-role-missing');
  var messages = value.messages.slice();
  return { artifacts: artifacts, messages: messages };
}
function atomic(file, bytes, max, mode) {
  var result = fileGuards.atomicReplaceRegularFileResult(paths.PROJECT_ROOT, path.dirname(file), file, bytes,
    { create: true, directoryMode: mode === 0o600 ? 0o700 : 0o755, mode: mode, maxBytes: max });
  if (!result.ok) throw new Error(result.code || 'figma-generation-write-failed');
}
function cleanupStageDirectory(directory, budget, depth) {
  if (depth > 12 || budget.count >= STAGE_CLEANUP_ENTRIES_MAX) return false;
  var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, directory, STAGE_CLEANUP_ENTRIES_MAX - budget.count);
  if (!listed.ok) return false;
  for (var i = 0; i < listed.names.length; i++) {
    budget.count++;
    var target = path.join(directory, listed.names[i]);
    var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, directory, target);
    if (!inspected || inspected.status === 'missing') continue;
    if (inspected.status !== 'present' || !inspected.stat) return false;
    if (inspected.stat.isFile() && !inspected.stat.isSymbolicLink()) {
      if (!fileGuards.unlinkRegularFileUnder(paths.PROJECT_ROOT, directory, target, { allowMissing: true })) return false;
    } else if (inspected.stat.isDirectory() && !inspected.stat.isSymbolicLink()) {
      if (!cleanupStageDirectory(target, budget, depth + 1) || !fileGuards.removeEmptyDirectoryUnder(paths.PROJECT_ROOT, directory, target)) return false;
    } else return false;
  }
  return true;
}
function cleanupJobStage(jobId) {
  if (!JOB_RE.test(String(jobId || ''))) return false;
  var parent = path.join(paths.FIGMA_CACHE_DIR, 'generations');
  var directory = path.join(parent, jobId);
  var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, parent, directory);
  if (inspected && inspected.status === 'missing') return true;
  if (!inspected || inspected.status !== 'present' || !inspected.stat || !inspected.stat.isDirectory() || inspected.stat.isSymbolicLink()) return false;
  return cleanupStageDirectory(directory, { count: 0 }, 0) &&
    fileGuards.removeEmptyDirectoryUnder(paths.PROJECT_ROOT, parent, directory);
}
function cleanupOwnedDirectory(parent, name, pattern) {
  if (!pattern.test(String(name || ''))) return false;
  var directory = path.join(parent, name);
  var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, parent, directory);
  if (inspected && inspected.status === 'missing') return true;
  if (!inspected || inspected.status !== 'present' || !inspected.stat || !inspected.stat.isDirectory() || inspected.stat.isSymbolicLink()) return false;
  return cleanupStageDirectory(directory, { count: 0 }, 0) && fileGuards.removeEmptyDirectoryUnder(paths.PROJECT_ROOT, parent, directory);
}
function cleanupUnpublishedGeneration(generationId) {
  if (!generation.GENERATION_RE.test(String(generationId || ''))) return false;
  var manifestRemoved = fileGuards.unlinkRegularFileUnder(paths.PROJECT_ROOT, generation.GENERATIONS_DIR,
    generation.manifestFile(generationId), { allowMissing: true });
  var committedRemoved = cleanupOwnedDirectory(generation.ARTIFACTS_DIR, generationId, generation.GENERATION_RE);
  var runtimeRemoved = cleanupOwnedDirectory(path.join(paths.FIGMA_CACHE_DIR, 'generations'), generationId, generation.GENERATION_RE);
  return manifestRemoved && committedRemoved && runtimeRemoved;
}
function generationNames(directory, kind) {
  var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, directory, GENERATION_CLEANUP_ENTRIES_MAX);
  if (!listed.ok) {
    var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, path.dirname(directory), directory);
    if (inspected && inspected.status === 'missing') return [];
    throw new Error('generation-retention-directory-invalid');
  }
  return listed.names.map(function (name) {
    var match = kind === 'manifest' ? /^(gen-[a-f0-9]{32})\.json$/.exec(name) : /^(gen-[a-f0-9]{32})$/.exec(name);
    if (match) return match[1];
    if (name === '.gitkeep' || kind === 'runtime' && JOB_RE.test(name)) return null;
    throw new Error('generation-retention-entry-invalid');
  }).filter(Boolean);
}
function sourceGenerationIdsForRetention(manifest, generationId) {
  if (!generation.validateManifest(manifest, generationId)) {
    throw new Error('generation-retention-manifest-invalid');
  }
  var seen = Object.create(null);
  return manifest.domains.map(function (lineage) {
    return lineage.sourceGenerationId;
  }).filter(function (sourceGenerationId) {
    if (seen[sourceGenerationId]) return false;
    seen[sourceGenerationId] = 1;
    return true;
  });
}
function cleanupSupersededGenerations(currentGenerationId, currentManifest) {
  try {
    var retainedManifests = Object.create(null), retainedArtifacts = Object.create(null);
    retainedManifests[currentGenerationId] = 1; retainedArtifacts[currentGenerationId] = 1;
    var recentIds = Object.keys(jobs).map(function (id) { return jobs[id]; }).filter(function (job) {
      return !!job.committedGenerationId;
    }).sort(function (left, right) {
      return Date.parse(right.finishedAt || right.startedAt) - Date.parse(left.finishedAt || left.startedAt);
    }).map(function (job) { return job.committedGenerationId; })
      .concat(history.retainedGenerationIds(GENERATION_RETENTION - 1));
    var retainedCount = 0;
    recentIds.some(function (id) {
      if (!retainedManifests[id]) { retainedManifests[id] = 1; retainedArtifacts[id] = 1; retainedCount++; }
      return retainedCount >= GENERATION_RETENTION - 1;
    });
    function retainSources(manifest, generationId) {
      sourceGenerationIdsForRetention(manifest, generationId).forEach(function (sourceGenerationId) {
        retainedArtifacts[sourceGenerationId] = 1;
      });
    }
    retainSources(currentManifest, currentGenerationId);
    Object.keys(retainedManifests).forEach(function (id) {
      if (id === currentGenerationId && currentManifest) return;
      var file = generation.manifestFile(id);
      var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, generation.GENERATIONS_DIR, file, generation.MANIFEST_MAX);
      if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') {
        throw new Error('generation-retention-manifest-invalid');
      }
      var manifest;
      try { manifest = JSON.parse(hit.bytes.toString('utf8')); }
      catch (error) { throw new Error('generation-retention-manifest-invalid'); }
      retainSources(manifest, id);
    });
    var manifestIds = generationNames(generation.GENERATIONS_DIR, 'manifest');
    var artifactIds = generationNames(generation.ARTIFACTS_DIR, 'artifact');
    var runtimeIds = generationNames(path.join(paths.FIGMA_CACHE_DIR, 'generations'), 'runtime');
    var removed = 0;
    for (var i = 0; i < artifactIds.length; i++) {
      if (!retainedArtifacts[artifactIds[i]] && !cleanupOwnedDirectory(generation.ARTIFACTS_DIR, artifactIds[i], generation.GENERATION_RE)) {
        throw new Error('generation-retention-cleanup-failed');
      }
      if (!retainedArtifacts[artifactIds[i]]) removed++;
    }
    for (var r = 0; r < runtimeIds.length; r++) {
      if (!retainedArtifacts[runtimeIds[r]] && !cleanupOwnedDirectory(path.join(paths.FIGMA_CACHE_DIR, 'generations'), runtimeIds[r], generation.GENERATION_RE)) {
        throw new Error('generation-retention-cleanup-failed');
      }
      if (!retainedArtifacts[runtimeIds[r]]) removed++;
    }
    for (var m = 0; m < manifestIds.length; m++) {
      if (!retainedManifests[manifestIds[m]] && !fileGuards.unlinkRegularFileUnder(paths.PROJECT_ROOT, generation.GENERATIONS_DIR,
        generation.manifestFile(manifestIds[m]), { allowMissing: true })) throw new Error('generation-retention-cleanup-failed');
      if (!retainedManifests[manifestIds[m]]) removed++;
    }
    return { ok: true, removed: removed };
  } catch (error) {
    return { ok: false, error: closedErrorCode(error, RETENTION_ERROR_CODES, 'generation-retention-cleanup-failed') };
  }
}
function currentInputs(job) {
  var context = scopeContext(job.scope);
  if (!context.ok || context.configRevision !== job.configRevision || context.accountFingerprint !== job.accountFingerprint ||
      context.fileKeyFingerprint !== job.fileKeyFingerprint) return null;
  var inputs = job.comparisonDomain
    ? domainInputSnapshot(context, job.comparisonDomain, context.activeGeneration)
    : inputSnapshot(context, job.scope, context.activeGeneration);
  return inputs.fingerprint === job.inputFingerprint ? inputs : null;
}
function clearPublishedComparisonDirtyState(completed) {
  var domains = (completed || []).map(function (result) { return result && (result.domain || result.group); });
  if (domains.indexOf('token-drift') >= 0) tokenState.clearProjectDirty();
  if (domains.indexOf('component-drift') >= 0) componentState.clearProjectDirty();
}
function publicationBudgetError(entries, artifactLimit, byteLimit, code) {
  if (!Array.isArray(entries) || entries.length > artifactLimit) return code;
  var total = 0;
  for (var index = 0; index < entries.length; index++) {
    if (!Number.isSafeInteger(entries[index].size) || entries[index].size < 0) return code;
    total += entries[index].size;
    if (!Number.isSafeInteger(total) || total > byteLimit) return code;
  }
  return null;
}
function canonicalStageSource(source) {
  if (!SOURCE_RE.test(String(source || '')) || source.indexOf('\\') >= 0 ||
      source.split('/').some(function (segment) { return !segment || segment === '.' || segment === '..'; })) return false;
  var base = path.resolve(paths.FIGMA_CACHE_DIR, 'publication-preflight');
  var resolved = safeStageFile(base, source);
  return !!resolved && path.relative(base, resolved).split(path.sep).join('/') === source;
}
function publicationPreflight(job, completed) {
  if (!job || !Array.isArray(completed) || !completed.length) return 'publication-domain-set-invalid';
  var roles = Object.create(null), domainGroups = Object.create(null), artifacts = [];
  for (var resultIndex = 0; resultIndex < completed.length; resultIndex++) {
    var result = completed[resultIndex];
    var domain = result && (result.domain || result.group);
    if (!result || !SCOPE_SET[result.group] && result.group !== 'surfaces' ||
        typeof domain !== 'string' || domainGroups[domain] ||
        !result.stage || !Array.isArray(result.stage.artifacts) || !result.stage.artifacts.length) {
      return 'publication-domain-set-invalid';
    }
    domainGroups[domain] = result.group;
    for (var artifactIndex = 0; artifactIndex < result.stage.artifacts.length; artifactIndex++) {
      var artifact = result.stage.artifacts[artifactIndex];
      if (!exact(artifact, [
        'source', 'logicalPath', 'role', 'persistence', 'required',
        'schemaVersion', 'bytes', 'hash', 'size'
      ]) || roles[artifact.role] || !canonicalStageSource(artifact.source) ||
          !Buffer.isBuffer(artifact.bytes) || artifact.size !== artifact.bytes.length ||
          artifact.hash !== generation.sha(artifact.bytes) ||
          artifact.schemaVersion !== generation.artifactContractVersion(artifact.role) ||
          (artifact.persistence !== 'committed' && artifact.persistence !== 'runtime') ||
          typeof artifact.required !== 'boolean' ||
          artifact.required && artifact.persistence !== 'committed' ||
          generation.logicalGroup(artifact.logicalPath) !== result.group ||
          generation.logicalDomain(artifact.logicalPath) !== domain ||
          !allowedScopeArtifact(result.group, artifact) ||
          !generation.artifactBytesContractValid(artifact.role, artifact.bytes)) {
        return 'publication-artifact-invalid';
      }
      roles[artifact.role] = 1;
      artifacts.push(Object.assign({ domain: domain }, artifact));
    }
  }
  var phaseError = publicationBudgetError(
    artifacts, generation.ARTIFACTS_MAX, STAGE_TOTAL_MAX, 'publication-phase-budget-exceeded');
  if (phaseError) return phaseError;
  var composite = artifacts.filter(function (artifact) {
    return artifact.domain === 'tokens' || artifact.domain === 'components';
  });
  return publicationBudgetError(
    composite, programLimits.compositePublicationArtifactsMax,
    programLimits.phaseBytesMax, 'component-token-composite-budget-exceeded');
}
function publish(job, completed, options) {
  options = options || {};
  var preflightError = publicationPreflight(job, completed);
  if (preflightError) return Promise.reject(new Error(preflightError));
  return new Promise(function (resolve, reject) {
    function attempt() {
      if (recoveryError) { reject(syncHistoryError()); return; }
      if (job.cancelRequested) { resolve(null); return; }
      var lease;
      try {
        lease = finalizations.beginMutation({ kind: 'figma-sync', key: 'figma-sync:' + job.id, pendingChild: false, requireSoleWriter: true });
      } catch (error) { reject(error); return; }
      if (!lease.ok) {
        if (options.external === true) {
          var externalTimer = setTimeout(attempt, 1000);
          if (typeof externalTimer.unref === 'function') externalTimer.unref();
          return;
        }
        job.state = 'queued'; job.phase = 'waiting-for-project-writer'; job.revision++; emit(job);
        remember(job).then(function (remembered) {
          if (!remembered || recoveryError) { reject(syncHistoryError()); return; }
          var timer = setTimeout(attempt, 1000); if (typeof timer.unref === 'function') timer.unref();
        });
        return;
      }
      var generationId = null, pointerPublished = false;
      try {
        if (options.verifyInputs ? options.verifyInputs() !== true : !currentInputs(job)) throw new Error('plan-stale');
        var previous = generation.current();
        if (!previous.ok) throw new Error(previous.error || 'design-generation-invalid');
        generationId = randomId('gen');
        var entries = [], plannedDomains = Object.create(null), domainsById = Object.create(null);
        var publishedAt = now();
        completed.forEach(function (result) {
          var domain = result.domain || result.group;
          plannedDomains[domain] = 1;
          domainsById[domain] = { id: domain, group: result.group, inputFingerprint: result.inputFingerprint || job.inputFingerprint,
            syncedAt: publishedAt, sourceGenerationId: generationId };
          result.stage.artifacts.forEach(function (artifact) {
            var root = artifact.persistence === 'committed'
              ? path.join(generation.ARTIFACTS_DIR, generationId, result.group)
              : path.join(paths.FIGMA_CACHE_DIR, 'generations', generationId, result.group);
            var destination = path.join(root, artifact.source);
            atomic(destination, artifact.bytes, generation.ARTIFACT_MAX, artifact.persistence === 'committed' ? 0o644 : 0o600);
            entries.push({ role: artifact.role, group: result.group, domain: domain, path: safeRel(destination), logicalPath: artifact.logicalPath,
              hash: artifact.hash, schemaVersion: artifact.schemaVersion, persistence: artifact.persistence, required: artifact.required, size: artifact.size });
          });
        });
        // Carry exact domains, not coarse groups. In particular, refreshing the
        // global drift domain must never discard per-task surface-drift domains.
        var previousCompatible = previous.ok && previous.mode === 'generation' &&
          previous.manifest.accountFingerprint === job.accountFingerprint &&
          previous.manifest.fileKeyFingerprint === job.fileKeyFingerprint;
        if (previousCompatible) {
          previous.manifest.artifacts.forEach(function (entry) {
            var domain = entry.domain;
            if (plannedDomains[domain]) return;
            var bytes = generation.readEntry(entry);
            if (!bytes) {
              if (entry.persistence === 'committed' && entry.required) throw new Error('generation-carry-forward-invalid');
              return;
            }
            var priorLineage = previous.manifest.domains.find(function (lineage) {
              return lineage.id === domain;
            });
            if (!priorLineage) throw new Error('generation-carry-forward-domain-invalid');
            var sourceGenerationId = priorLineage.sourceGenerationId;
            var oldPrefix = entry.persistence === 'committed'
              ? 'orchestrator/figma/manifests/artifacts/' + sourceGenerationId + '/' + entry.group + '/'
              : 'orchestrator/.cache/figma/generations/' + sourceGenerationId + '/' + entry.group + '/';
            if (entry.path.indexOf(oldPrefix) !== 0 || !entry.path.slice(oldPrefix.length)) throw new Error('generation-carry-forward-path-invalid');
            entries.push({ role: entry.role, group: entry.group, domain: domain, path: entry.path, logicalPath: entry.logicalPath,
              hash: entry.hash, schemaVersion: entry.schemaVersion, persistence: entry.persistence, required: entry.required, size: entry.size });
            if (!domainsById[domain]) {
              domainsById[domain] = Object.assign({}, priorLineage);
            }
          });
        }
        var publicationEntries = entries.filter(function (entry) { return plannedDomains[entry.domain]; });
        var publicationBytes = publicationEntries.reduce(function (sum, entry) { return sum + entry.size; }, 0);
        if (publicationEntries.length > generation.ARTIFACTS_MAX || publicationBytes > STAGE_TOTAL_MAX) {
          throw new Error('publication-phase-budget-exceeded');
        }
        var compositeEntries = entries.filter(function (entry) {
          return entry.domain === 'tokens' || entry.domain === 'components';
        });
        var compositeBytes = compositeEntries.reduce(function (sum, entry) { return sum + entry.size; }, 0);
        if (compositeEntries.length > programLimits.compositePublicationArtifactsMax ||
            compositeBytes > programLimits.phaseBytesMax) {
          throw new Error('component-token-composite-budget-exceeded');
        }
        var counters = job.groups.reduce(function (out, group) { out.updated += group.updated; out.unchanged += group.unchanged; out.warnings += group.warnings; return out; }, { updated: 0, unchanged: 0, warnings: 0 });
        var manifestGroups = ['tokens', 'components', 'surfaces', 'drift'].filter(function (group) {
          return entries.some(function (entry) { return entry.group === group; });
        });
        var syncGroups = Object.create(null);
        job.groups.forEach(function (group) {
          syncGroups[group.group] = { status: group.status, updated: group.updated, unchanged: group.unchanged, warnings: group.warnings };
        });
        var manifest = { schemaVersion: 2, generationId: generationId,
          accountFingerprint: job.accountFingerprint, fileKeyFingerprint: job.fileKeyFingerprint, createdAt: publishedAt,
          syncJobId: job.id, updatedDomains: completed.map(function (result) { return result.domain || result.group; }), syncGroups: syncGroups,
          groups: manifestGroups, domains: Object.keys(domainsById).sort().map(function (id) { return domainsById[id]; }),
          artifacts: entries, counters: counters };
        if (!generation.validateManifest(manifest, generationId)) throw new Error('generation-manifest-invalid');
        var manifestBytes = Buffer.from(JSON.stringify(manifest, null, 2) + '\n');
        atomic(generation.manifestFile(generationId), manifestBytes, generation.MANIFEST_MAX, 0o644);
        var pointer = { schemaVersion: 2, generationId: generationId, manifestHash: generation.sha(manifestBytes), committedAt: now() };
        if (!generation.validatePointer(pointer)) throw new Error('generation-pointer-invalid');
        atomic(generation.POINTER_FILE, Buffer.from(JSON.stringify(pointer, null, 2) + '\n'), generation.POINTER_MAX, 0o644);
        pointerPublished = true;
        var verified = generation.current();
        if (!verified.ok || verified.mode !== 'generation' || verified.manifest.generationId !== generationId) throw new Error('generation-postcondition-failed');
        // The watcher latch is only a cheap stale signal. Clear it after the
        // exact domain bytes and pointer have passed their publication
        // postcondition; failed/cancelled sibling domains remain dirty.
        clearPublishedComparisonDirtyState(completed);
        var cleanup = cleanupSupersededGenerations(generationId, manifest);
        if (!cleanup.ok) {
          recoveryError = 'generation-retention-cleanup-failed';
          job.messages = job.messages.concat(['Generation retention cleanup failed; further sync is blocked until recovery.']).slice(-history.MESSAGE_MAX);
        }
        resolve(generationId);
      } catch (error) {
        if (generationId && !pointerPublished && !cleanupUnpublishedGeneration(generationId)) {
          reject(new Error('unpublished-generation-cleanup-failed'));
        } else reject(error);
      }
      finally { finalizations.endMutation(lease.handle); }
    }
    attempt();
  });
}
function publishComponentTokenAtomic(job, completed, options) {
  var domains = Array.isArray(completed) ? completed.map(function (result) {
    return result && (result.domain || result.group);
  }).sort() : [];
  var groupState = Object.create(null);
  (job && Array.isArray(job.groups) ? job.groups : []).forEach(function (row) {
    groupState[row.group] = row.status;
  });
  if (domains.length !== 2 || domains[0] !== 'components' || domains[1] !== 'tokens' ||
      groupState.components !== 'completed' || groupState.tokens !== 'completed') {
    return Promise.reject(new Error('component-token-atomic-domain-set-incomplete'));
  }
  return publish(job, completed, options);
}
function commitSemanticNoOp(job) {
  return new Promise(function (resolve, reject) {
    function attempt() {
      if (recoveryError) { reject(syncHistoryError()); return; }
      if (job.cancelRequested) { reject(new Error('sync-cancelled')); return; }
      var lease;
      try {
        lease = finalizations.beginMutation({
          kind: 'figma-sync',
          key: 'figma-sync-no-op:' + job.id,
          pendingChild: false,
          requireSoleWriter: true
        });
      } catch (error) { reject(error); return; }
      if (!lease.ok) {
        var timer = setTimeout(attempt, 1000);
        if (typeof timer.unref === 'function') timer.unref();
        return;
      }
      try {
        if (!currentInputs(job)) throw new Error('plan-stale');
        var active = generation.current();
        if (!active.ok || active.mode !== 'generation') {
          throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED');
        }
        var sourceIndexHash = publishedTokenSourceIndexHash(active.manifest.generationId);
        if (sourceIndexHash !== job.semanticNoOp.sourceIndexHash) {
          throw new Error('TOKEN_SOURCE_SET_CHANGED');
        }
        if (!settleTokenHealth(job, 'no-op', {
          sourceIndexHash: sourceIndexHash,
          evidenceSources: job.semanticNoOp.healthEvidence,
          summaryOutcome: 'no-op'
        })) {
          throw new Error('TOKEN_HEALTH_RECOVERY_REQUIRED');
        }
        job.committedGenerationId = active.manifest.generationId;
        resolve(active.manifest.generationId);
      } catch (error) {
        reject(error);
      } finally {
        finalizations.endMutation(lease.handle);
      }
    }
    attempt();
  });
}
function finish(job, result, rawErrorCode) {
  if (!cleanupJobStage(job.id)) job.messages = job.messages.concat(['Staging cleanup could not be verified; no committed generation was removed.']).slice(-history.MESSAGE_MAX);
  job.state = result === 'success' || result === 'partial' ? 'completed' : result;
  job.phase = 'finished'; job.result = result; job.errorCode = rawErrorCode ? syncErrors.publicCode(rawErrorCode) : null; job.finishedAt = now(); job.progress = 100; job.revision++;
  if (activeJobId === job.id) activeJobId = null;
  latestTerminalRecord = historyRecord(job);
  // Terminal UI projections read the durable history (including
  // domain-specific failure details). Emit only after that record has been
  // attempted so the first refresh cannot race back to a stale "never
  // synced" state. remember() resolves false on failure and sets the recovery
  // latch; the failed terminal state is still emitted honestly.
  remember(job).then(function (remembered) {
    emit(job);
    if (remembered && result === 'success' && job.committedGenerationId &&
        (job.scope === 'tokens' || job.scope === 'components')) {
      // Provider refresh and deterministic project comparison are one user
      // action. The follow-up uses the committed generation, costs zero Figma
      // reads, and still has its own durable job/history/cancellation record.
      setImmediate(function () {
        requestDriftComparison('post-' + job.scope + '-sync',
          job.scope === 'tokens' ? 'tokens' : null);
      });
    }
  });
}
function run(job) {
  var stageRoot = path.join(paths.FIGMA_CACHE_DIR, 'generations', job.id);
  var completed = [];
  job.planGroups.reduce(function (promise, group, index) {
    return promise.then(function () {
      if (job.cancelRequested) return;
      var row = job.groups.find(function (item) { return item.group === group; });
      row.status = 'running'; job.state = 'running'; job.result = 'running'; job.phase = 'syncing-' + group; job.progress = Math.floor(index / job.planGroups.length * 80); job.revision++; emit(job);
      return remember(job).then(function (remembered) {
        if (!remembered || recoveryError) throw syncHistoryError();
        var groupDir = path.join(stageRoot, group);
        if (!fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, groupDir, { create: true, mode: 0o700 })) throw new Error('stage-directory-unsafe');
        var execution;
        if (group === 'drift') {
          // Local deterministic comparison — no session, no Figma, no AI.
          var active = generation.current();
          if (!active.ok || active.mode !== 'generation') throw new Error(active.error || 'design-generation-invalid');
          execution = tokenJobs.executeDriftScope(job, groupDir, active, job.comparisonDomain).then(function (outcome) {
            if (outcome && outcome.fatal) {
              (outcome.messages || []).forEach(function (message) { push(job, message); });
              throw new Error(outcome.fatal);
            }
            if (outcome && outcome.partialErrors && outcome.partialErrors.length) {
              job.partialErrors = (job.partialErrors || []).concat(outcome.partialErrors);
            }
          });
        } else {
          if (group === 'tokens') {
            execution = tokenJobs.prepareTokenCaptureStage(job, groupDir).then(function (prepared) {
              if (prepared.sourceCount === 0) throw new Error('TOKEN_SOURCE_CAPTURE_INCOMPLETE');
              return runGroupSession(job, group, groupDir)
                .then(function () { return tokenJobs.executeTokensStage(job, groupDir); });
            });
          } else {
            execution = (group === 'components'
              ? tokenJobs.prepareComponentCaptureStage(job, groupDir)
              : Promise.resolve()).then(function () {
              return runGroupSession(job, group, groupDir);
            }).then(function () {
              if (group === 'components') return tokenJobs.executeComponentsStage(job, groupDir);
              return null;
            });
          }
        }
        return execution.then(function (outcome) {
          if (outcome && outcome.noOp) return outcome;
          if (outcome && outcome.healthEvidence) {
            job.tokenHealthEvidence = outcome.healthEvidence;
          }
          return validateStage(job, group, groupDir);
        });
      }).then(function (stage) {
        row.status = 'completed'; row.updated = stage.artifacts.length;
        row.warnings = stageWarningCount(stage.messages);
        stage.messages.forEach(function (message) { push(job, message); });
        if (stage.noOp) {
          row.unchanged = stage.sourceCount;
          job.semanticNoOp = {
            sourceIndexHash: stage.sourceIndexHash,
            healthEvidence: stage.healthEvidence
          };
          return;
        }
        if (group === 'drift') {
          var byDomain = Object.create(null);
          stage.artifacts.forEach(function (artifact) {
            var domain = generation.logicalDomain(artifact.logicalPath);
            (byDomain[domain] = byDomain[domain] || []).push(artifact);
          });
          Object.keys(byDomain).sort().forEach(function (domain) {
            completed.push({ group: group, domain: domain,
              inputFingerprint: job.domainInputFingerprints && job.domainInputFingerprints[domain],
              stage: { artifacts: byDomain[domain], messages: [] } });
          });
        } else if (group === 'components') {
          var componentDomains = Object.create(null);
          stage.artifacts.forEach(function (artifact) {
            var domain = generation.logicalDomain(artifact.logicalPath);
            (componentDomains[domain] = componentDomains[domain] || []).push(artifact);
          });
          if (!componentDomains.components || !componentDomains.tokens) {
            throw new Error('component-token-atomic-domain-set-incomplete');
          }
          row.updated = componentDomains.components.length;
          var tokenRow = job.groups.find(function (item) { return item.group === 'tokens'; });
          tokenRow.status = 'completed';
          tokenRow.updated = componentDomains.tokens.length;
          ['components', 'tokens'].forEach(function (domain) {
            completed.push({
              group: domain,
              domain: domain,
              inputFingerprint: job.inputFingerprint,
              stage: { artifacts: componentDomains[domain], messages: [] }
            });
          });
        } else {
          completed.push({ group: group, stage: stage });
        }
      }).catch(function (error) {
        if (error && error.figmaSyncFatal) throw error;
        var code = scopedErrorCode(group, error);
        if (group === 'tokens' || group === 'components') {
          settleTokenHealth(job, 'failed', {
            errorCode: code,
            retryable: !job.cancelRequested,
            summaryOutcome: job.cancelRequested ? 'cancelled' : 'failed'
          });
        }
        row.status = 'failed'; row.warnings++;
        if (group === 'components') {
          var failedTokenRow = job.groups.find(function (item) { return item.group === 'tokens'; });
          failedTokenRow.status = 'failed';
          failedTokenRow.warnings++;
        }
        job.errorCode = code;
        push(job, group + ' failed [' + code + ']: ' + String(error && error.message || error).slice(0, 300));
      });
    });
  }, Promise.resolve()).then(function () {
    if (!completed.length) {
      if (job.semanticNoOp && !job.cancelRequested && job.groups.every(function (group) {
        return group.status === 'completed';
      })) {
        return commitSemanticNoOp(job).then(function () {
          finish(job, 'success', null);
        });
      }
      if (job.tokenHealthReservation && !job.tokenHealthSettled) {
        settleTokenHealth(job, 'failed', {
          errorCode: job.cancelRequested ? syncErrors.PUBLIC_CODES.syncCancelled : job.errorCode,
          retryable: !job.cancelRequested,
          summaryOutcome: job.cancelRequested ? 'cancelled' : 'failed'
        });
      }
      finish(job, job.cancelRequested ? 'cancelled' : 'failed',
        job.cancelRequested ? 'sync-cancelled' : job.errorCode || 'sync-no-valid-groups');
      return;
    }
    // Persist the partial verdict before pointer publication. If the process
    // dies after the pointer switch but before finish(), startup recovery can
    // reconstruct an honest partial result instead of laundering it to green.
    if (job.partialErrors && job.partialErrors.length) job.errorCode = syncErrors.publicCode(job.partialErrors[0]);
    job.phase = 'publishing'; job.progress = 85; job.revision++; emit(job);
    return remember(job).then(function (remembered) {
      if (!remembered || recoveryError) throw syncHistoryError();
      return job.scope === 'components'
        ? publishComponentTokenAtomic(job, completed)
        : publish(job, completed);
    }).then(function (generationId) {
      if (!generationId) { finish(job, 'cancelled', 'sync-cancelled'); return; }
      job.committedGenerationId = generationId;
      if (job.tokenHealthReservation) {
        var sourceIndexHash = publishedTokenSourceIndexHash(generationId);
        var activeAfterPublish = generation.current();
        var healthOk = sourceIndexHash && activeAfterPublish.ok && activeAfterPublish.mode === 'generation' &&
          settleTokenHealth(job, job.tokenHealthReservation.reservations.length ? 'published' : 'no-op', {
            sourceIndexHash: sourceIndexHash,
            evidenceSources: job.tokenHealthEvidence,
            summaryOutcome: job.tokenHealthReservation.reservations.length ? 'published' : 'no-op'
          });
        if (!healthOk) {
          job.partialErrors = (job.partialErrors || []).concat([syncErrors.PUBLIC_CODES.tokenHealthRecoveryRequired]);
        }
      }
      // A published generation with a failed sibling domain is an honest
      // partial run: the committed domains stand, the failed one keeps its
      // typed code and never claims "everything compared".
      var terminalResult = job.partialErrors && job.partialErrors.length ? 'partial' : 'success';
      var terminalCode = terminalResult === 'partial' ? job.partialErrors[0] : null;
      return designHistory.recordCurrent().then(function (historyResult) {
        if (!historyResult || !historyResult.ok) {
          push(job, 'Design history could not be recorded: ' +
            String(historyResult && (historyResult.reason || historyResult.error) || 'unknown').slice(0, 160));
        }
        finish(job, terminalResult, terminalCode);
      }, function (historyError) {
        push(job, 'Design history could not be recorded: ' +
          String(historyError && historyError.message || historyError).slice(0, 160));
        finish(job, terminalResult, terminalCode);
      });
    });
  }).catch(function (error) {
    var publicFailure = scopedErrorCode(null, error);
    if (job.tokenHealthReservation && !job.tokenHealthSettled) {
      settleTokenHealth(job, 'failed', {
        errorCode: publicFailure,
        retryable: true,
        summaryOutcome: 'failed'
      });
    }
    finish(job, 'failed', publicFailure);
  });
}
function start(request, internal) {
  if (resetPaused) return { ok: false, status: 409, error: 'figma-sync-active' };
  if (!exact(request || {}, ['planId', 'warningsAcknowledged']) || !PLAN_RE.test(String(request.planId || '')) ||
      !Array.isArray(request.warningsAcknowledged) || request.warningsAcknowledged.length > 20 || !request.warningsAcknowledged.every(function (code) { return typeof code === 'string' && /^[a-z0-9-]{1,80}$/.test(code); })) {
    return { ok: false, status: 400, error: 'bad-sync-start-request' };
  }
  if (recovering) return { ok: false, status: 409, error: 'figma-sync-recovering' };
  if (recoveryError) return { ok: false, status: 409, error: 'figma-sync-recovery-failed' };
  prunePlans();
  if (activeJobId && jobs[activeJobId]) return jobs[activeJobId].planId === request.planId ? { ok: true, status: 202, job: publicJob(jobs[activeJobId]) } : { ok: false, status: 409, error: 'sync-already-running' };
  var selected = plans[request.planId];
  if (!selected) return { ok: false, status: 409, error: 'plan-stale' };
  if (testActive()) return { ok: false, status: 409, error: 'figma-test-active' };
  if (connectorSessionActive()) return { ok: false, status: 409, error: 'figma-session-active' };
  var acknowledged = Object.create(null); request.warningsAcknowledged.forEach(function (code) { acknowledged[code] = 1; });
  if (selected.warnings.some(function (warning) { return !acknowledged[warning.code]; })) return { ok: false, status: 409, error: 'sync-warning-confirmation-required' };
  var context = scopeContext(selected.scope);
  if (!context.ok) return { ok: false, status: 409, error: context.error };
  var inputs;
  var currentTokenSources = null;
  try {
    inputs = selected.comparisonDomain
      ? domainInputSnapshot(context, selected.comparisonDomain, context.activeGeneration)
      : inputSnapshot(context, selected.scope, context.activeGeneration);
    if (selected.scope === 'tokens' || selected.scope === 'components') {
      currentTokenSources = tokenSourceSnapshot(context.activeGeneration, {
        fileKeyFingerprint: context.fileKeyFingerprint,
        branchKey: projectBranchKey()
      }, selected.reactivationSourceId ? [selected.reactivationSourceId] : []);
      if (selected.scope === 'tokens' && !selected.reactivationSourceId) {
        currentTokenSources = bootstrapTokenSourceSnapshot(currentTokenSources, context);
      }
    }
  } catch (error) {
    return { ok: false, status: 409, error: closedErrorCode(error, INPUT_ERROR_CODES, 'figma-input-scan-failed') };
  }
  if (inputs.fingerprint !== selected.fingerprint || context.configRevision !== selected.configRevision ||
      context.accountFingerprint !== selected.accountFingerprint || context.fileKeyFingerprint !== selected.fileKeyFingerprint ||
      (selected.scope === 'tokens' || selected.scope === 'components') &&
        currentTokenSources.fingerprint !== selected.tokenSourceFingerprint) {
    return { ok: false, status: 409, error: 'plan-stale' };
  }
  Object.keys(jobs).map(function (jobId) { return jobs[jobId]; }).filter(function (job) { return job.finishedAt; })
    .sort(function (a, b) { return Date.parse(b.finishedAt) - Date.parse(a.finishedAt); }).slice(history.RETENTION)
    .forEach(function (job) { delete jobs[job.id]; });
  var domainInputFingerprints = null;
  if (selected.scope === 'drift') {
    try {
      domainInputFingerprints = {};
      if (!selected.comparisonDomain || selected.comparisonDomain === 'tokens') {
        domainInputFingerprints['token-drift'] = domainInputSnapshot(context, 'tokens', context.activeGeneration).fingerprint;
      }
      if (!selected.comparisonDomain || selected.comparisonDomain === 'components') {
        domainInputFingerprints['component-drift'] = domainInputSnapshot(context, 'components', context.activeGeneration).fingerprint;
      }
    } catch (error) {
      return { ok: false, status: 409, error: closedErrorCode(error, INPUT_ERROR_CODES, 'figma-input-scan-failed') };
    }
  }
  var id = internal && JOB_RE.test(String(internal.jobId || ''))
    ? internal.jobId : randomId('fsj');
  if (jobs[id] || history.read(id)) {
    return { ok: false, status: 409, error: 'sync-job-id-conflict' };
  }
  var startedAt = now();
  var tokenHealthReservation = null;
  var capturePlan = null;
  if (selected.scope === 'tokens' || selected.scope === 'components') {
    if (tokenHealthRecoveryError) {
      beginTokenHealthRecovery();
      return { ok: false, status: 409, error: 'TOKEN_SOURCE_HEALTH_RECOVERY_IN_PROGRESS' };
    }
    try {
      var reservableSources = currentTokenSources.sources.slice();
      if (selected.scope === 'components') {
        var reservableById = Object.create(null);
        reservableSources.forEach(function (source) { reservableById[source.sourceId] = 1; });
        tokenHealth.highWatermarks(currentTokenSources.sourceIndexHash).forEach(function (source) {
          if (reservableById[source.sourceId]) return;
          reservableById[source.sourceId] = 1;
          reservableSources.push({
            sourceId: source.sourceId,
            acceptedSequence: source.acceptedSequence,
            origins: []
          });
        });
        reservableSources.sort(function (left, right) { return left.sourceId.localeCompare(right.sourceId); });
      }
      tokenHealthReservation = tokenHealth.reserveMany({
        sourceIndexHash: currentTokenSources.sourceIndexHash,
        ownerId: id,
        prospectiveCount: selected.scope === 'components' ? 128 : 0,
        sources: reservableSources.map(function (source) {
          return { sourceId: source.sourceId, acceptedSequence: source.acceptedSequence };
        })
      });
      capturePlan = selected.scope === 'tokens'
        ? tokenCapturePlan(currentTokenSources, selected.accountFingerprint, tokenHealthReservation.reservations)
        : componentTokenCapturePlan(Object.assign({}, currentTokenSources, { sources: reservableSources }),
          selected.accountFingerprint,
          tokenHealthReservation.reservations, tokenHealthReservation.prospectiveReservations);
      if (selected.scope === 'components') {
        var componentOwned = Object.create(null);
        capturePlan.knownSources.forEach(function (source) {
          if (source.componentOwned) componentOwned[source.sourceId] = 1;
        });
        tokenHealthReservation.unusedReservations = tokenHealthReservation.reservations.filter(function (reservation) {
          return !componentOwned[reservation.sourceId];
        });
        tokenHealthReservation.reservations = tokenHealthReservation.reservations.filter(function (reservation) {
          return !!componentOwned[reservation.sourceId];
        });
      }
    } catch (error) {
      return {
        ok: false,
        status: 409,
        error: closedErrorCode(error, INPUT_ERROR_CODES, 'TOKEN_SOURCE_HEALTH_RESERVATION_FAILED')
      };
    }
  }
  var jobGroups = selected.groups.slice();
  var job = jobs[id] = { id: id, revision: 1, planId: selected.id, state: 'queued', phase: 'queued', progress: 0,
    groups: jobGroups.map(function (group) { return { group: group, status: 'pending', updated: 0, unchanged: 0, warnings: 0 }; }),
    planGroups: selected.scope === 'components' ? ['components'] : selected.groups.slice(),
    messages: [], startedAt: startedAt, finishedAt: null, committedGenerationId: null,
    result: 'queued', errorCode: null, cancelRequested: false, inputFingerprint: selected.fingerprint,
    scope: selected.scope, configRevision: selected.configRevision, accountFingerprint: selected.accountFingerprint,
    fileKeyFingerprint: selected.fileKeyFingerprint, fileKey: selected.fileKey,
    comparisonDomain: selected.comparisonDomain, domainInputFingerprints: domainInputFingerprints,
    tokenCapturePlan: capturePlan, tokenHealthReservation: tokenHealthReservation,
    tokenHealthSettled: false, tokenHealthRecoveryRequired: false };
  activeJobId = id; emit(job);
  remember(job).then(function (remembered) {
    if (remembered && !recoveryError) run(job);
    else finish(job, 'failed', 'sync-history-unavailable');
  });
  return { ok: true, status: 202, job: publicJob(job) };
}
function cancel(request) {
  if (!exact(request || {}, ['jobId', 'expectedRevision']) || !JOB_RE.test(String(request.jobId || '')) || !Number.isSafeInteger(request.expectedRevision) || request.expectedRevision < 1) {
    return { ok: false, status: 400, error: 'bad-sync-cancel-request' };
  }
  var job = jobs[request.jobId];
  if (!job || activeJobId !== job.id) return { ok: false, status: 404, error: 'sync-job-not-active' };
  if (job.revision !== request.expectedRevision) return { ok: false, status: 409, error: 'sync-job-revision-conflict', currentRevision: job.revision };
  job.cancelRequested = true; job.revision++;
  job.messages = job.messages.concat(['Cancellation requested; the active scope may finish, but its result will not be published.']).slice(-history.MESSAGE_MAX);
  emit(job); remember(job);
  return { ok: true, status: 202, job: publicJob(job) };
}
function get(id) { return jobs[id] ? publicJob(jobs[id]) : history.read(id); }
function active() { return activeJobId && jobs[activeJobId] ? publicJob(jobs[activeJobId]) : null; }
function busy() { return recovering || !!recoveryError || !!active(); }
function resetReady() {
  return !recovering && !active() && !tokenIngestionRunning && !tokenHealthRecoveryPromise;
}
function beginReset() {
  if (!resetReady()) return false;
  resetPaused = true;
  return true;
}
function endReset() { resetPaused = false; }
function clearRuntime() {
  if (!resetReady()) return false;
  plans = Object.create(null);
  jobs = Object.create(null);
  activeJobId = null;
  latestTerminalRecord = null;
  recoveryError = null;
  tokenHealthRecoveryError = null;
  tokenHealthRecoveryPromise = null;
  tokenState.clearProjectDirty();
  componentState.clearProjectDirty();
  return true;
}
function recoveryState() { return recovering ? 'recovering' : recoveryError ? 'failed' : 'ready'; }
function latestTerminal() { return latestTerminalRecord ? Object.assign({}, latestTerminalRecord) : null; }

function validExternalDomainResult(result, inputFingerprint) {
  if (!result || ['tokens', 'surfaces', 'drift'].indexOf(result.group) < 0 ||
      typeof result.domain !== 'string' || result.inputFingerprint !== inputFingerprint ||
      !result.stage || !Array.isArray(result.stage.artifacts) || !result.stage.artifacts.length ||
      result.stage.artifacts.length > generation.ARTIFACTS_MAX) return false;
  if (result.group === 'tokens' && result.domain !== 'tokens' ||
      result.group === 'surfaces' && result.domain.indexOf('surface:') !== 0 ||
      result.group === 'drift' && result.domain !== 'token-drift' && result.domain !== 'component-drift' &&
        result.domain.indexOf('surface-drift:') !== 0) return false;
  var roles = Object.create(null), logicalPaths = Object.create(null), sources = Object.create(null), required = false, total = 0;
  for (var i = 0; i < result.stage.artifacts.length; i++) {
    var artifact = result.stage.artifacts[i];
    if (!artifact || !Buffer.isBuffer(artifact.bytes) || !ROLE_RE.test(String(artifact.role || '')) || roles[artifact.role] ||
        !SOURCE_RE.test(String(artifact.source || '')) || artifact.source.indexOf('\\') >= 0 ||
        artifact.source.split('/').indexOf('..') >= 0 || sources[artifact.source] ||
        typeof artifact.logicalPath !== 'string' || artifact.logicalPath.length > 500 || logicalPaths[artifact.logicalPath] ||
        artifact.persistence !== 'committed' || typeof artifact.required !== 'boolean' ||
        artifact.schemaVersion !== generation.artifactContractVersion(artifact.role) ||
        artifact.size !== artifact.bytes.length || artifact.size > generation.ARTIFACT_MAX ||
        artifact.hash !== generation.sha(artifact.bytes) || generation.logicalGroup(artifact.logicalPath) !== result.group ||
        generation.logicalDomain(artifact.logicalPath) !== result.domain) return false;
    total += artifact.size;
    if (total > STAGE_TOTAL_MAX) return false;
    var contractEntry = {
      group: result.group, domain: result.domain, role: artifact.role,
      logicalPath: artifact.logicalPath, required: artifact.required, persistence: artifact.persistence
    };
    if (generation.requiredDomainRole(contractEntry)) required = true;
    roles[artifact.role] = 1; logicalPaths[artifact.logicalPath] = 1; sources[artifact.source] = 1;
  }
  return required;
}
function externalCommittedGeneration(request, result) {
  var active = generation.current();
  if (!active.ok) throw new Error(active.error || 'design-generation-invalid');
  if (active.mode !== 'generation' || active.manifest.syncJobId !== request.id) return null;
  var manifest = active.manifest;
  var lineage = manifest.schemaVersion === 2 && manifest.domains.find(function (entry) {
    return entry.id === result.domain;
  });
  var syncNames = manifest.syncGroups ? Object.keys(manifest.syncGroups) : [];
  var summary = manifest.syncGroups && manifest.syncGroups[result.group];
  var expectedArtifacts = result.stage.artifacts.map(function (artifact) {
    return {
      role: artifact.role, logicalPath: artifact.logicalPath, hash: artifact.hash, size: artifact.size,
      persistence: artifact.persistence, required: artifact.required, schemaVersion: artifact.schemaVersion
    };
  }).sort(function (left, right) { return left.logicalPath.localeCompare(right.logicalPath); });
  var committedArtifacts = manifest.schemaVersion === 2 ? manifest.artifacts.filter(function (artifact) {
    return artifact.domain === result.domain;
  }).map(function (artifact) {
    return {
      role: artifact.role, logicalPath: artifact.logicalPath, hash: artifact.hash, size: artifact.size,
      persistence: artifact.persistence, required: artifact.required, schemaVersion: artifact.schemaVersion
    };
  }).sort(function (left, right) { return left.logicalPath.localeCompare(right.logicalPath); }) : [];
  if (manifest.schemaVersion !== 2 || manifest.accountFingerprint !== request.accountFingerprint ||
      manifest.fileKeyFingerprint !== request.fileKeyFingerprint ||
      !lineage || lineage.group !== result.group || lineage.inputFingerprint !== request.inputFingerprint ||
      lineage.sourceGenerationId !== manifest.generationId ||
      manifest.updatedDomains.length !== 1 || manifest.updatedDomains[0] !== result.domain ||
      syncNames.length !== 1 || syncNames[0] !== result.group || !summary || summary.status !== 'completed' ||
      summary.updated !== expectedArtifacts.length || summary.unchanged !== 0 || summary.warnings !== 0 ||
      manifest.counters.updated !== expectedArtifacts.length || manifest.counters.unchanged !== 0 || manifest.counters.warnings !== 0 ||
      JSON.stringify(committedArtifacts) !== JSON.stringify(expectedArtifacts)) {
    throw new Error('external-domain-publication-recovery-mismatch');
  }
  if (request.verifyInputs() !== true) throw new Error('plan-stale');
  return manifest.generationId;
}

// Publish one server-validated task domain through the same pointer-last
// immutable-generation transaction as the explicit global scopes. The source
// fingerprint is rechecked after the sole-writer lease is acquired, so a
// second pull cannot race the snapshot being committed.
function publishDomains(request) {
  if (!request || !JOB_RE.test(String(request.id || '')) ||
      !generation.HASH_RE.test(String(request.accountFingerprint || '')) ||
      !generation.HASH_RE.test(String(request.fileKeyFingerprint || '')) ||
      !generation.HASH_RE.test(String(request.inputFingerprint || '')) ||
      !Array.isArray(request.completed) || request.completed.length !== 1 ||
      typeof request.verifyInputs !== 'function') {
    return Promise.reject(new Error('external-domain-publication-invalid'));
  }
  if (recovering) return Promise.reject(new Error('figma-sync-recovering'));
  if (recoveryError) return Promise.reject(syncHistoryError());
  var result = request.completed[0];
  if (!validExternalDomainResult(result, request.inputFingerprint)) {
    return Promise.reject(new Error('external-domain-publication-invalid'));
  }
  var committed;
  try { committed = externalCommittedGeneration(request, result); }
  catch (error) { return Promise.reject(error); }
  if (committed) return Promise.resolve(committed);
  var groupRow = {
    group: result.group, status: 'completed', updated: result.stage.artifacts.length,
    unchanged: 0, warnings: 0
  };
  var job = {
    id: request.id, planGroups: [result.group], groups: [groupRow],
    cancelRequested: false, messages: [], inputFingerprint: request.inputFingerprint,
    accountFingerprint: request.accountFingerprint,
    fileKeyFingerprint: request.fileKeyFingerprint
  };
  return publish(job, request.completed, {
    external: true,
    verifyInputs: request.verifyInputs
  });
}
function committedRecovery(record) {
  var active = generation.current();
  if (!active.ok) throw new Error('figma-generation-recovery-invalid');
  if (active.mode !== 'generation') return null;
  var manifest = active.manifest;
  if (manifest.syncJobId !== record.id) {
    var sourceIndexHash = publishedTokenSourceIndexHash(manifest.generationId);
    if (!sourceIndexHash) return null;
    var held;
    try { held = tokenHealth.peek(); }
    catch (error) {
      if (/^TOKEN_SOURCE_HEALTH_/.test(String(error && error.message || ''))) return null;
      throw error;
    }
    return semanticNoOpCommittedRecovery(record, active, sourceIndexHash,
      held && held.snapshot);
  }
  if (manifest.syncJobId !== record.id || manifest.accountFingerprint !== record.accountFingerprint ||
      manifest.fileKeyFingerprint !== record.fileKeyFingerprint || !Array.isArray(manifest.updatedDomains) ||
      !manifest.syncGroups) return null;
  var updatedGroups = manifest.updatedDomains.map(function (domain) {
    var lineage = manifest.domains.find(function (candidate) { return candidate.id === domain; });
    return lineage && lineage.group;
  }).filter(function (group, index, list) { return group && list.indexOf(group) === index; });
  if (updatedGroups.some(function (group) { return record.planGroups.indexOf(group) < 0; }) ||
      record.planGroups.some(function (group) { return !manifest.syncGroups[group]; })) return null;
  return {
    generationId: manifest.generationId,
    updatedGroups: updatedGroups,
    groups: JSON.parse(JSON.stringify(manifest.syncGroups)),
    partial: record.errorCode !== null,
    finishedAt: active.pointer.committedAt
  };
}
function semanticNoOpCommittedRecovery(record, active, sourceIndexHash, healthSnapshot) {
  if (!record || !active || !active.manifest || !healthSnapshot || !healthSnapshot.index ||
      !Array.isArray(record.planGroups) || record.planGroups.length !== 1 ||
      record.planGroups[0] !== 'tokens' ||
      active.manifest.accountFingerprint !== record.accountFingerprint ||
      active.manifest.fileKeyFingerprint !== record.fileKeyFingerprint ||
      healthSnapshot.index.sourceIndexRevision !== sourceIndexHash ||
      !Array.isArray(healthSnapshot.index.jobSummaries)) return null;
  var summary = healthSnapshot.index.jobSummaries.find(function (candidate) {
    return candidate.jobId === record.id &&
      candidate.action === 'refresh-known-token-sources' &&
      candidate.startedAt === record.startedAt &&
      candidate.outcome === 'no-op';
  });
  if (!summary || !Number.isSafeInteger(summary.sourceCount) || summary.sourceCount < 0 ||
      !summary.finishedAt || Date.parse(summary.finishedAt) < Date.parse(record.startedAt)) return null;
  return {
    generationId: active.manifest.generationId,
    updatedGroups: ['tokens'],
    groups: {
      tokens: {
        status: 'completed',
        updated: 0,
        unchanged: summary.sourceCount,
        warnings: 0
      }
    },
    partial: false,
    finishedAt: summary.finishedAt
  };
}
function acceptedTokenSourceCaptures(active, index) {
  if (!active || active.mode !== 'generation' || !index || !Array.isArray(index.sources) ||
      !Array.isArray(index.shards)) throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED');
  var entriesByRole = Object.create(null);
  active.manifest.artifacts.forEach(function (entry) {
    if (entry.domain !== 'tokens' || entry.role.indexOf('observed-token-source-shard:') !== 0) return;
    if (entriesByRole[entry.role]) throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED');
    entriesByRole[entry.role] = entry;
  });
  var descriptorByRole = Object.create(null), batchesBySource = Object.create(null);
  index.shards.forEach(function (descriptor) {
    var roleMatch = /^observed-token-source-shard:([0-9]{3})$/.exec(String(descriptor && descriptor.role || ''));
    if (!roleMatch || Number(roleMatch[1]) >= 128 || descriptorByRole[descriptor.role]) {
      throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED');
    }
    var entry = entriesByRole[descriptor.role], bytes = entry && generation.readEntry(entry), shard;
    if (!bytes) throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED');
    try { shard = JSON.parse(bytes.toString('utf8')); } catch (error) {
      throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED');
    }
    var shardPayload = Object.assign({}, shard);
    delete shardPayload.semanticHash;
    if (!shard || shard.bucket !== Number(roleMatch[1]) ||
        shard.semanticHash !== descriptor.hash ||
        tokenIdentity.hash(shardPayload) !== shard.semanticHash ||
        !Array.isArray(shard.sources) || shard.sources.length !== descriptor.sourceCount) {
      throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED');
    }
    var priorSourceId = null;
    shard.sources.forEach(function (batch) {
      var expectedSourceId;
      try {
        expectedSourceId = tokenIdentity.sourceIdFor({
          fileKeyFingerprint: batch.fileKeyFingerprint,
          branchKey: batch.branchKey,
          nodeId: batch.nodeId,
          context: batch.context
        });
      } catch (error) { throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED'); }
      var semanticPayload = {
        schemaVersion: batch.schemaVersion,
        sourceId: batch.sourceId,
        provider: batch.provider,
        providerCapability: batch.providerCapability,
        fileKeyFingerprint: batch.fileKeyFingerprint,
        branchKey: batch.branchKey,
        nodeId: batch.nodeId,
        kind: batch.kind,
        context: batch.context,
        identityQuality: batch.identityQuality,
        observations: batch.observations
      };
      if (batch.sourceId !== expectedSourceId ||
          tokenIdentity.sourceBucket(batch.sourceId) !== shard.bucket ||
          priorSourceId !== null && priorSourceId.localeCompare(batch.sourceId) >= 0 ||
          batchesBySource[batch.sourceId] ||
          tokenIdentity.hash(semanticPayload) !== batch.batchSemanticHash) {
        throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED');
      }
      priorSourceId = batch.sourceId;
      batchesBySource[batch.sourceId] = { role: descriptor.role, batch: batch };
    });
    descriptorByRole[descriptor.role] = descriptor;
  });
  if (Object.keys(entriesByRole).length !== index.shards.length ||
      Object.keys(descriptorByRole).length !== index.shards.length ||
      index.counts.shards !== index.shards.length) {
    throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED');
  }
  var indexPayload = Object.assign({}, index);
  delete indexPayload.semanticHash;
  if (tokenIdentity.hash(indexPayload) !== index.semanticHash) {
    throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED');
  }
  var previousSourceId = null, activeCount = 0, retiredCount = 0;
  var accepted = index.sources.map(function (source, ordinal) {
    var hit = batchesBySource[source.sourceId], batch = hit && hit.batch;
    if (!batch || source.ordinal !== ordinal ||
        previousSourceId !== null && previousSourceId.localeCompare(source.sourceId) >= 0 ||
        source.acceptedBatch.shardRole !== hit.role ||
        source.acceptedBatch.batchSemanticHash !== batch.batchSemanticHash ||
        source.acceptedBatch.captureSequence !== batch.captureSequence ||
        source.acceptedBatch.observationCount !== batch.observations.length ||
        source.nodeId !== batch.nodeId || source.kind !== batch.kind ||
        tokenIdentity.canonical(source.context) !== tokenIdentity.canonical(batch.context) ||
        tokenIdentity.canonical(source.origins) !== tokenIdentity.canonical(batch.origins) ||
        index.scope.fileKeyFingerprint !== batch.fileKeyFingerprint ||
        index.scope.branchKey !== batch.branchKey) {
      throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED');
    }
    previousSourceId = source.sourceId;
    if (source.lifecycle === 'active') activeCount++;
    else if (source.lifecycle === 'retired') retiredCount++;
    else throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED');
    return {
      sourceId: source.sourceId,
      captureOperationId: batch.captureOperationId,
      captureSequence: batch.captureSequence,
      captureEvidenceHash: batch.captureEvidenceHash
    };
  });
  if (Object.keys(batchesBySource).length !== accepted.length ||
      index.counts.active !== activeCount || index.counts.retired !== retiredCount) {
    throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED');
  }
  return accepted;
}
function readRecoveryJson(root, file, maxBytes) {
  var resolvedRoot = path.resolve(root), resolvedFile = path.resolve(file);
  var relative = path.relative(resolvedRoot, resolvedFile);
  if (!relative || relative === '..' || relative.indexOf('..' + path.sep) === 0 ||
      path.isAbsolute(relative)) {
    throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE');
  }
  var rootState = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, path.dirname(root), root);
  if (rootState && rootState.status === 'missing') return null;
  if (!rootState || rootState.status !== 'present' || !rootState.stat ||
      !rootState.stat.isDirectory() || rootState.stat.isSymbolicLink()) {
    throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE');
  }
  var directory = path.dirname(resolvedFile);
  var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, directory, resolvedFile);
  if (inspected && inspected.status === 'missing') return null;
  var hit = fileGuards.boundedRegularFileUnder(
    paths.PROJECT_ROOT, directory, resolvedFile, maxBytes);
  if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') {
    throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE');
  }
  try { return JSON.parse(hit.bytes.toString('utf8')); }
  catch (error) { throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE'); }
}
function syncStageRecoveryReservations(historyById) {
  var root = path.join(paths.FIGMA_CACHE_DIR, 'generations');
  var rootEntry = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, path.dirname(root), root);
  if (rootEntry && rootEntry.status === 'missing') {
    return { reservations: [], prospectiveReservations: [] };
  }
  if (!rootEntry || rootEntry.status !== 'present' || !rootEntry.stat ||
      !rootEntry.stat.isDirectory() || rootEntry.stat.isSymbolicLink()) {
    throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE');
  }
  var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, root, history.RETENTION);
  if (!listed.ok) throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE');
  var reservations = [], prospectiveReservations = [];
  listed.names.forEach(function (name) {
    if (!JOB_RE.test(name) || !historyById[name]) {
      throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE');
    }
    var jobRoot = path.join(root, name);
    var jobEntry = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, root, jobRoot);
    if (!jobEntry || jobEntry.status !== 'present' || !jobEntry.stat ||
        !jobEntry.stat.isDirectory() || jobEntry.stat.isSymbolicLink()) {
      throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE');
    }
    var tokenRoot = path.join(jobRoot, 'tokens');
    var tokenIndex = readRecoveryJson(tokenRoot, path.join(tokenRoot, 'capture-plan.json'),
      programLimits.stageManifestBytesMax);
    if (tokenIndex) {
      if (!exact(tokenIndex, ['schemaVersion', 'sourceIndexHash', 'revision', 'scope',
        'sourceCount', 'planShardFiles', 'captureShardFiles']) ||
          tokenIndex.schemaVersion !== 1 ||
          !generation.HASH_RE.test(String(tokenIndex.sourceIndexHash || '')) ||
          !Number.isSafeInteger(tokenIndex.revision) || tokenIndex.revision < 0 ||
          !exact(tokenIndex.scope, ['fileKeyFingerprint', 'branchKey']) ||
          !generation.HASH_RE.test(String(tokenIndex.scope.fileKeyFingerprint || '')) ||
          typeof tokenIndex.scope.branchKey !== 'string' || !tokenIndex.scope.branchKey ||
          !Array.isArray(historyById[name].planGroups) ||
          historyById[name].planGroups.indexOf('tokens') < 0 ||
          !Array.isArray(tokenIndex.planShardFiles) ||
          !Array.isArray(tokenIndex.captureShardFiles) ||
          tokenIndex.planShardFiles.length !== tokenIndex.captureShardFiles.length ||
          tokenIndex.planShardFiles.length > 128 ||
          !Number.isSafeInteger(tokenIndex.sourceCount) || tokenIndex.sourceCount < 0 ||
          tokenIndex.sourceCount > programLimits.tokenSourceRecordsMax) {
        throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE');
      }
      var count = 0, seen = Object.create(null), seenPlanFiles = Object.create(null);
      tokenIndex.planShardFiles.forEach(function (relative, shardIndex) {
        if (!/^capture-plan\/[0-9]{3}\.json$/.test(String(relative || '')) ||
            Number(relative.slice(13, 16)) >= 128 || seenPlanFiles[relative] ||
            tokenIndex.captureShardFiles[shardIndex] !== relative.replace('capture-plan/', 'capture-intake/')) {
          throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE');
        }
        seenPlanFiles[relative] = 1;
        var shard = readRecoveryJson(tokenRoot, path.join(tokenRoot, relative),
          programLimits.artifactBytesMax);
        if (!exact(shard, ['schemaVersion', 'bucket', 'scope', 'sourceIndexHash', 'records']) ||
            shard.schemaVersion !== 1 ||
            shard.sourceIndexHash !== tokenIndex.sourceIndexHash ||
            tokenIdentity.canonical(shard.scope) !== tokenIdentity.canonical(tokenIndex.scope) ||
            !Array.isArray(shard.records) || !shard.records.length ||
            shard.bucket !== Number(relative.slice(13, 16))) {
          throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE');
        }
        shard.records.forEach(function (record) {
          var source = record && record.source;
          var validSource = false;
          try {
            validSource = exact(source, ['sourceId', 'fileKeyFingerprint', 'branchKey', 'nodeId',
              'kind', 'context', 'origin']) &&
              source.sourceId === tokenIdentity.sourceIdFor(source);
          } catch (error) { validSource = false; }
          if (!exact(record, ['captureOperationId', 'captureSequence', 'accountFingerprint',
            'connectorRevision', 'semanticPreflightHash', 'source', 'origins']) ||
              !validSource || seen[source.sourceId] ||
              source.fileKeyFingerprint !== tokenIndex.scope.fileKeyFingerprint ||
              source.branchKey !== tokenIndex.scope.branchKey ||
              tokenIdentity.sourceBucket(source.sourceId) !== shard.bucket ||
              !/^tokop_[A-Za-z0-9_-]{16,96}$/.test(String(record.captureOperationId || '')) ||
              !Number.isSafeInteger(record.captureSequence) || record.captureSequence < 1 ||
              !generation.HASH_RE.test(String(record.accountFingerprint || '')) ||
              record.connectorRevision !== 'figma-mcp-session-v1' ||
              !Array.isArray(record.origins) || !record.origins.length ||
              tokenIdentity.canonical(record.origins[0]) !== tokenIdentity.canonical(source.origin) ||
              record.semanticPreflightHash !== tokenIdentity.hash({
                captureOperationId: record.captureOperationId,
                captureSequence: record.captureSequence,
                sourceId: source.sourceId
              })) {
            throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE');
          }
          seen[source.sourceId] = 1;
          count++;
          reservations.push({
            sourceId: source.sourceId,
            captureOperationId: record.captureOperationId,
            captureSequence: record.captureSequence,
            at: historyById[name].startedAt
          });
        });
      });
      if (count !== tokenIndex.sourceCount) {
        throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE');
      }
    }
    var componentRoot = path.join(jobRoot, 'components');
    var componentPlan = readRecoveryJson(componentRoot,
      path.join(componentRoot, 'component-token-plan.json'), programLimits.stageManifestBytesMax);
    if (componentPlan) {
      if (!exact(componentPlan, ['schemaVersion', 'sourceIndexHash', 'revision', 'scope',
        'accountFingerprint', 'connectorRevision', 'componentSourceScopeId',
        'newSourceReservations', 'knownSources', 'captureShardFiles']) ||
          componentPlan.schemaVersion !== 1 ||
          !generation.HASH_RE.test(String(componentPlan.sourceIndexHash || '')) ||
          !Number.isSafeInteger(componentPlan.revision) || componentPlan.revision < 0 ||
          !exact(componentPlan.scope, ['fileKeyFingerprint', 'branchKey']) ||
          !generation.HASH_RE.test(String(componentPlan.scope.fileKeyFingerprint || '')) ||
          typeof componentPlan.scope.branchKey !== 'string' || !componentPlan.scope.branchKey ||
          !generation.HASH_RE.test(String(componentPlan.accountFingerprint || '')) ||
          componentPlan.connectorRevision !== 'figma-mcp-session-v1' ||
          !/^component-scope:[a-f0-9]{64}$/.test(String(componentPlan.componentSourceScopeId || '')) ||
          !Array.isArray(historyById[name].planGroups) ||
          historyById[name].planGroups.indexOf('components') < 0 ||
          !Array.isArray(componentPlan.knownSources) ||
          !Array.isArray(componentPlan.newSourceReservations) ||
          componentPlan.newSourceReservations.length !== 128 ||
          !Array.isArray(componentPlan.captureShardFiles) ||
          componentPlan.captureShardFiles.length !== 128) {
        throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE');
      }
      var known = Object.create(null);
      componentPlan.knownSources.forEach(function (row) {
        if (!exact(row, ['sourceId', 'captureOperationId', 'captureSequence',
          'componentOwned', 'origins']) || known[row.sourceId] ||
            tokenIdentity.sourceBucket(row.sourceId) < 0 ||
            !/^tokop_[A-Za-z0-9_-]{16,96}$/.test(String(row.captureOperationId || '')) ||
            !Number.isSafeInteger(row.captureSequence) || row.captureSequence < 1 ||
            typeof row.componentOwned !== 'boolean' || !Array.isArray(row.origins)) {
          throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE');
        }
        known[row.sourceId] = 1;
        reservations.push({
          sourceId: row.sourceId,
          captureOperationId: row.captureOperationId,
          captureSequence: row.captureSequence,
          at: historyById[name].startedAt
        });
      });
      var prospective = Object.create(null);
      componentPlan.newSourceReservations.forEach(function (row) {
        if (!exact(row, ['captureOperationId', 'captureSequence']) ||
            prospective[row.captureOperationId] || row.captureSequence !== 1 ||
            !/^tokop_[A-Za-z0-9_-]{16,96}$/.test(String(row.captureOperationId || ''))) {
          throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE');
        }
        prospective[row.captureOperationId] = 1;
        prospectiveReservations.push({
          captureOperationId: row.captureOperationId,
          captureSequence: 1,
          reservedAt: historyById[name].startedAt,
          ownerId: name
        });
      });
      componentPlan.captureShardFiles.forEach(function (relative, bucket) {
        if (relative !== 'component-token-intake/' + String(bucket).padStart(3, '0') + '.json') {
          throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE');
        }
      });
    }
  });
  return {
    reservations: reservations,
    prospectiveReservations: prospectiveReservations
  };
}
function reconcileTokenHealthAfterRestart(active) {
  var listed = history.list(null, history.RETENTION);
  if (!listed.ok || listed.nextCursor !== null) {
    throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE');
  }
  var terminalJobs = Object.create(null);
  listed.items.forEach(function (record) {
    if (record.result === 'queued' || record.result === 'running' || !record.finishedAt) {
      throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE');
    }
    terminalJobs[record.id] = { result: record.result, finishedAt: record.finishedAt };
  });
  var sourceIndexHash = null, acceptedSources = [];
  if (active.mode === 'generation') {
    var entry = active.manifest.artifacts.find(function (candidate) {
      return candidate.role === 'observed-token-source-index';
    });
    if (entry) {
      var bytes = generation.readEntry(entry), index;
      if (!bytes) throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED');
      try { index = JSON.parse(bytes.toString('utf8')); } catch (error) {
        throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED');
      }
      if (!index || !generation.HASH_RE.test(String(index.semanticHash || '')) ||
          !Array.isArray(index.sources)) {
        throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED');
      }
      sourceIndexHash = index.semanticHash;
      acceptedSources = acceptedTokenSourceCaptures(active, index);
    }
  }
  return tokenHealth.reconcileSyncReservations({
    terminalJobs: terminalJobs,
    ownerScanComplete: true,
    sourceIndexHash: sourceIndexHash,
    acceptedSources: acceptedSources
  });
}
function recoverTokenHealthAfterRestart(active) {
  var listed = history.list(null, history.RETENTION);
  if (!listed.ok || listed.nextCursor !== null) {
    return Promise.reject(new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE'));
  }
  var historyById = Object.create(null);
  listed.items.forEach(function (record) { historyById[record.id] = record; });
  var sourceIndexHash = null, acceptedSources = [];
  if (active.mode === 'generation') {
    var entry = active.manifest.artifacts.find(function (candidate) {
      return candidate.role === 'observed-token-source-index';
    });
    if (entry) {
      var bytes = generation.readEntry(entry), index;
      try { index = bytes && JSON.parse(bytes.toString('utf8')); }
      catch (error) { throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED'); }
      if (!index) throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED');
      sourceIndexHash = index.semanticHash;
      acceptedSources = acceptedTokenSourceCaptures(active, index);
    }
  }
  if (!sourceIndexHash) return Promise.reject(new Error('TOKEN_SOURCE_HEALTH_RECOVERY_UNPROVEN'));
  var stageRows = syncStageRecoveryReservations(historyById);
  var screenRows = require('./screen-token-plans').recoveryReservations();
  return require('./token-source-ingestion').recoveryReservations().then(function (ingestionRows) {
    tokenHealth.recoverExact({
      sourceIndexHash: sourceIndexHash,
      acceptedSources: acceptedSources,
      reservations: stageRows.reservations.concat(screenRows, ingestionRows),
      prospectiveReservations: stageRows.prospectiveReservations
    });
    return reconcileTokenHealthAfterRestart(active);
  });
}
function beginTokenHealthRecovery() {
  if (tokenHealthRecoveryPromise) return tokenHealthRecoveryPromise;
  var active = generation.current();
  if (!active.ok) return Promise.resolve(false);
  tokenHealthRecoveryPromise = recoverTokenHealthAfterRestart(active).then(function () {
    tokenHealthRecoveryError = null;
    return true;
  }).catch(function (error) {
    tokenHealthRecoveryError = String(error && error.message ||
      'TOKEN_SOURCE_HEALTH_RECOVERY_FAILED').split(':')[0];
    return false;
  }).finally(function () {
    tokenHealthRecoveryPromise = null;
  });
  return tokenHealthRecoveryPromise;
}
function init(options) {
  notify = options && typeof options.notify === 'function' ? options.notify : function () {};
  testActive = options && typeof options.testActive === 'function' ? options.testActive : function () { return false; };
  recovering = true; recoveryError = null; tokenHealthRecoveryError = null;
  tokenHealthRecoveryPromise = null;
  return history.recoverInterrupted(committedRecovery).then(function (count) {
    var active = generation.current();
    if (!active.ok) throw new Error('figma-generation-recovery-invalid');
    var healthRecovery = Promise.resolve();
    try {
      require('./screen-token-plans').reconcileReservations();
      reconcileTokenHealthAfterRestart(active);
    } catch (error) {
      if (!/^TOKEN_(?:SOURCE|GENERATION)_/.test(String(error && error.message || ''))) throw error;
      healthRecovery = recoverTokenHealthAfterRestart(active).catch(function (recoveryFailure) {
        tokenHealthRecoveryError = String(recoveryFailure && recoveryFailure.message ||
          error.message).split(':')[0];
      });
    }
    return healthRecovery.then(function () {
      if (active.mode === 'generation') {
        var cleanup = cleanupSupersededGenerations(active.manifest.generationId, active.manifest);
        if (!cleanup.ok) throw new Error(cleanup.error || 'generation-retention-cleanup-failed');
      }
      recovering = false; latestTerminalRecord = history.latest();
      return designHistory.recordCurrent();
    }).then(function (result) {
      if (result && (result.ok || result.reason === 'generation-not-committed')) return count;
      console.error('[figma-sync] design history baseline failed:',
        result && (result.reason || result.error) || 'unknown');
      return count;
    }, function (error) {
      console.error('[figma-sync] design history baseline failed:', error && error.message || error);
      return count;
    }).then(function (countAfterHistory) {
      var ingestion = require('./token-source-ingestion');
      return ingestion.reconcile({
        publishDomains: publishDomains,
        requestDriftComparison: requestDriftComparison
      }).then(function (results) {
        var retryable = results.filter(function (row) { return row && row.state === 'failed-retryable'; }).length;
        if (retryable) console.error('[figma-sync] token ingestion has ' + retryable + ' retryable intent(s)');
        if (!tokenIngestionTimer) {
          tokenIngestionTimer = setInterval(function () {
            if (resetPaused || tokenIngestionRunning || recovering || recoveryError) return;
            tokenIngestionRunning = ingestion.reconcile({
              publishDomains: publishDomains,
              requestDriftComparison: requestDriftComparison
            }).catch(function (error) {
              console.error('[figma-sync] token ingestion reconciliation failed:', error && error.message || error);
            }).finally(function () { tokenIngestionRunning = null; });
          }, 30000);
          if (typeof tokenIngestionTimer.unref === 'function') tokenIngestionTimer.unref();
        }
        return countAfterHistory;
      });
    });
  }).catch(function (error) {
    recovering = false; recoveryError = 'sync-history-recovery-failed'; throw error;
  });
}

function driftDomainInputs(capability) {
  var context = scopeContext('drift');
  if (!context.ok) return context;
  var inputs;
  try { inputs = domainInputSnapshot(context, capability, context.activeGeneration); }
  catch (error) {
    return { ok: false, error: closedErrorCode(error, INPUT_ERROR_CODES, 'figma-input-scan-failed') };
  }
  return {
    ok: true,
    fingerprint: inputs.fingerprint,
    accountFingerprint: context.accountFingerprint,
    fileKeyFingerprint: context.fileKeyFingerprint,
    activeGeneration: context.activeGeneration
  };
}

// Internal post-commit invalidation consumer. It enters the same durable
// compare plan/job/history/cancellation lifecycle as the user-facing Compare
// action; it never starts a domain-private in-memory writer.
function stageWarningCount(messages) {
  return (messages || []).filter(function (message) {
    return /\b(?:comparison failed|comparison skipped|requires recovery|could not be recorded)\b/i.test(String(message || ''));
  }).length;
}

function requestDriftComparison(reason, domain) {
  if (resetPaused) return Promise.resolve({ ok: false, error: 'figma-sync-active' });
  if (domain !== undefined && domain !== null && domain !== 'tokens' && domain !== 'components') {
    return Promise.resolve({ ok: false, error: 'figma-drift-domain-invalid' });
  }
  var current = active();
  if (current && (current.comparisonDomain === null || current.comparisonDomain === domain) &&
      Array.isArray(current.groups) &&
      current.groups.some(function (row) { return row.group === 'drift'; })) {
    return Promise.resolve({ ok: true, reused: true, job: publicJob(current), reason: String(reason || '').slice(0, 120) });
  }
  var request = { scope: 'drift' };
  if (domain) request.domain = domain;
  var planned = plan(request);
  if (!planned || !planned.ok) return Promise.resolve({ ok: false, error: planned && planned.error || 'design-comparison-unavailable' });
  var started = start({ planId: planned.plan.id, warningsAcknowledged: [] });
  return Promise.resolve(started && started.ok
    ? Object.assign({ reason: String(reason || '').slice(0, 120) }, started)
    : { ok: false, error: 'design-comparison-unavailable' });
}

module.exports = {
  JOB_RE: JOB_RE,
  driftDomainInputs: driftDomainInputs,
  requestDriftComparison: requestDriftComparison,
  init: init,
  plan: plan,
  start: start,
  cancel: cancel,
  get: get,
  active: active,
  busy: busy,
  resetReady: resetReady,
  beginReset: beginReset,
  endReset: endReset,
  clearRuntime: clearRuntime,
  comparisonSourceAvailable: comparisonSourceAvailable,
  recoveryState: recoveryState,
  latestTerminal: latestTerminal,
  publishDomains: publishDomains,
  cleanupExternalStage: cleanupJobStage,
  startSourceReactivation: startSourceReactivation,
  _test: Object.freeze({
    publicationBudgetError: publicationBudgetError,
    publishComponentTokenAtomic: publishComponentTokenAtomic,
    acceptedTokenSourceCaptures: acceptedTokenSourceCaptures,
    adapterRootsFromDocument: adapterRootsFromDocument,
    bootstrapTokenSourceSnapshot: bootstrapTokenSourceSnapshot,
    stageWarningCount: stageWarningCount,
    semanticNoOpCommittedRecovery: semanticNoOpCommittedRecovery,
    sourceGenerationIdsForRetention: sourceGenerationIdsForRetention,
    syncStageRecoveryReservations: syncStageRecoveryReservations
  })
};
