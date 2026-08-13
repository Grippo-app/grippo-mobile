'use strict';

// Generation-aware contract reader and the single atomic publication helper.
// A valid current-generation.json is the sole snapshot authority.

var fs = require('fs');
var path = require('path');
var crypto = require('crypto');
var paths = require('./paths');
var fileGuards = require('./file-guards');

var MANIFESTS_DIR = path.join(paths.API_CONTRACT_DIR, 'manifests');
var GENERATIONS_DIR = path.join(MANIFESTS_DIR, 'generations');
var ARTIFACTS_DIR = path.join(MANIFESTS_DIR, 'generation-artifacts');
var POINTER_FILE = path.join(MANIFESTS_DIR, 'current-generation.json');
var GENERATION_RE = /^gen-[0-9]{8}T[0-9]{6}Z-[0-9a-f]{12}$/;
var HASH_RE = /^sha256:[0-9a-f]{64}$/;
var AREA_RE = /^[a-z0-9][a-z0-9-]*$/;
var MAX_POINTER = 8 * 1024;
var MAX_MANIFEST = 512 * 1024;
var MAX_ARTIFACT = 10 * 1024 * 1024;
var GENERATION_CLEANUP_ENTRIES_MAX = 1024;
var PUBLICATION_ERRORS = Object.freeze({
  'artifact-parent-unsafe': 'generation-artifact-invalid',
  'artifact-file-unsafe': 'generation-artifact-invalid',
  'artifact-file-raced': 'generation-artifact-invalid',
  'short-read': 'generation-artifact-invalid',
  'artifact-directory-parent-unsafe': 'generation-directory-unsafe',
  'artifact-directory-unsafe': 'generation-directory-unsafe',
  'artifact-directory-entry-limit': 'generation-directory-unsafe',
  'artifact-directory-raced': 'generation-directory-unsafe',
  'json-object-required': 'generation-manifest-invalid',
  'artifact-path-outside-project': 'generation-artifact-path-invalid',
  'artifact-path-invalid': 'generation-artifact-path-invalid',
  'artifact-path-outside-generation': 'generation-artifact-path-invalid',
  'generation-artifact-size-limit': 'generation-artifact-invalid',
  'generation-area-set-invalid': 'generation-area-set-invalid',
  'generation-manifest-invalid': 'generation-manifest-invalid',
  'generation-publication-verification-failed': 'generation-publication-failed'
});

function readScope(projectRoot) {
  var root = path.resolve(projectRoot);
  var contractDir = path.join(root, 'orchestrator', 'api-contract');
  var manifestsDir = path.join(contractDir, 'manifests');
  return {
    projectRoot: root,
    manifestsDir: manifestsDir,
    generationsDir: path.join(manifestsDir, 'generations'),
    artifactsDir: path.join(manifestsDir, 'generation-artifacts'),
    pointerFile: path.join(manifestsDir, 'current-generation.json'),
    reportsDir: path.join(root, 'orchestrator', '.cache', 'api-contract', 'reports')
  };
}
var CONTROL_READ_SCOPE = readScope(paths.PROJECT_ROOT);
CONTROL_READ_SCOPE.reportsDir = path.join(paths.API_CONTRACT_CACHE_DIR, 'reports');

function sha(bytes) { return 'sha256:' + crypto.createHash('sha256').update(bytes).digest('hex'); }
function publicationError(error) {
  var code = error && typeof error.message === 'string' ? error.message : '';
  return PUBLICATION_ERRORS[code] || 'generation-publication-failed';
}
function exactKeys(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}
function sameFileGeneration(left, right) {
  return left && right && left.isFile() && right.isFile() && !left.isSymbolicLink() && !right.isSymbolicLink() &&
    left.dev === right.dev && left.ino === right.ino && left.mode === right.mode && left.nlink === right.nlink &&
    left.size === right.size && left.mtimeNs === right.mtimeNs && left.ctimeNs === right.ctimeNs;
}
function sameDirectoryGeneration(left, right) {
  return left && right && left.isDirectory() && right.isDirectory() && !left.isSymbolicLink() && !right.isSymbolicLink() &&
    left.dev === right.dev && left.ino === right.ino && left.mode === right.mode && left.nlink === right.nlink &&
    left.mtimeNs === right.mtimeNs && left.ctimeNs === right.ctimeNs;
}
function parentAnchored(file, readContext) {
  var scope = readContext || CONTROL_READ_SCOPE;
  var root = scope.projectRoot, parent = path.resolve(path.dirname(file));
  var rel = path.relative(root, parent);
  if (rel === '..' || rel.startsWith('..' + path.sep) || path.isAbsolute(rel)) return false;
  return fs.realpathSync(parent) === path.join(fs.realpathSync(root), rel);
}
function safeBytes(file, max, optional, readContext) {
  var fd;
  try {
    if (!parentAnchored(file, readContext)) throw new Error('artifact-parent-unsafe');
    var before = fs.lstatSync(file, { bigint: true });
    if (!before.isFile() || before.isSymbolicLink() || before.nlink !== 1n || before.size > BigInt(max)) throw new Error('artifact-file-unsafe');
    fd = fs.openSync(file, fs.constants.O_RDONLY | (fs.constants.O_NOFOLLOW || 0));
    var opened = fs.fstatSync(fd, { bigint: true });
    if (!sameFileGeneration(before, opened)) throw new Error('artifact-file-raced');
    var bytes = Buffer.alloc(Number(opened.size)), offset = 0;
    while (offset < bytes.length) { var count = fs.readSync(fd, bytes, offset, bytes.length - offset, offset); if (!count) throw new Error('short-read'); offset += count; }
    var afterFd = fs.fstatSync(fd, { bigint: true }), afterPath = fs.lstatSync(file, { bigint: true });
    if (!sameFileGeneration(opened, afterFd) || !sameFileGeneration(opened, afterPath) ||
        !parentAnchored(file, readContext)) throw new Error('artifact-file-raced');
    return bytes;
  } catch (e) { if (optional && e && e.code === 'ENOENT') return null; throw e; }
  finally { if (fd !== undefined) try { fs.closeSync(fd); } catch (ignore) {} }
}
function safeDirectoryHasEntries(directory, maxEntries, accept, readContext) {
  try {
    if (!parentAnchored(directory, readContext)) throw new Error('artifact-directory-parent-unsafe');
    var before = fs.lstatSync(directory, { bigint: true });
    if (!before.isDirectory() || before.isSymbolicLink()) throw new Error('artifact-directory-unsafe');
    var names = fs.readdirSync(directory);
    if (names.length > maxEntries) throw new Error('artifact-directory-entry-limit');
    var after = fs.lstatSync(directory, { bigint: true });
    if (!sameDirectoryGeneration(before, after) || !parentAnchored(directory, readContext)) throw new Error('artifact-directory-raced');
    return accept ? names.some(accept) : names.length > 0;
  } catch (error) {
    if (error && error.code === 'ENOENT') return false;
    throw error;
  }
}
function generationEvidencePresent(readContext) {
  var scope = readContext || CONTROL_READ_SCOPE;
  return safeDirectoryHasEntries(scope.generationsDir, 512,
    function (name) { return name !== '.gitkeep'; }, scope) ||
    safeDirectoryHasEntries(scope.artifactsDir, 512,
      function (name) { return name !== '.gitkeep'; }, scope);
}

function cleanupTree(directory, budget, depth) {
  if (depth > 8 || budget.count >= GENERATION_CLEANUP_ENTRIES_MAX) return false;
  var listed = fileGuards.boundedDirectoryNamesUnder(
    paths.PROJECT_ROOT, directory, GENERATION_CLEANUP_ENTRIES_MAX - budget.count
  );
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
      if (!cleanupTree(target, budget, depth + 1) ||
          !fileGuards.removeEmptyDirectoryUnder(paths.PROJECT_ROOT, directory, target)) return false;
    } else return false;
  }
  return true;
}

function cleanupGeneration(generationId) {
  if (!GENERATION_RE.test(String(generationId || ''))) return false;
  var directory = path.join(ARTIFACTS_DIR, generationId);
  var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, ARTIFACTS_DIR, directory);
  if (inspected && inspected.status !== 'missing') {
    if (inspected.status !== 'present' || !inspected.stat || !inspected.stat.isDirectory() || inspected.stat.isSymbolicLink()) return false;
    if (!cleanupTree(directory, { count: 0 }, 0) ||
        !fileGuards.removeEmptyDirectoryUnder(paths.PROJECT_ROOT, ARTIFACTS_DIR, directory)) return false;
  }
  return fileGuards.unlinkRegularFileUnder(paths.PROJECT_ROOT, GENERATIONS_DIR,
    path.join(GENERATIONS_DIR, generationId + '.json'), { allowMissing: true });
}

// An interrupted create can leave only the owned generation directory (and
// its empty areas child). That empty shape contains no contract evidence. Heal
// it before the fail-closed missing-pointer check; any file, unknown entry or
// unsafe inode remains evidence and still blocks reads.
function recoverEmptyGenerationResidue() {
  var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, ARTIFACTS_DIR, 512);
  if (!listed.ok) throw new Error('artifact-directory-unsafe');
  for (var i = 0; i < listed.names.length; i++) {
    var name = listed.names[i];
    if (name === '.gitkeep' || !GENERATION_RE.test(name)) continue;
    var directory = path.join(ARTIFACTS_DIR, name);
    var children = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, directory, 2);
    if (!children.ok) continue;
    if (children.names.length === 0) {
      fileGuards.removeEmptyDirectoryUnder(paths.PROJECT_ROOT, ARTIFACTS_DIR, directory);
      continue;
    }
    if (children.names.length !== 1 || children.names[0] !== 'areas') continue;
    var areas = path.join(directory, 'areas');
    var areaChildren = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, areas, 1);
    if (!areaChildren.ok || areaChildren.names.length) continue;
    if (fileGuards.removeEmptyDirectoryUnder(paths.PROJECT_ROOT, directory, areas)) {
      fileGuards.removeEmptyDirectoryUnder(paths.PROJECT_ROOT, ARTIFACTS_DIR, directory);
    }
  }
}
function parseObject(bytes) {
  var parsed = JSON.parse(bytes.toString('utf8'));
  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) throw new Error('json-object-required');
  return parsed;
}
function inside(root, file) {
  var rel = path.relative(path.resolve(root), path.resolve(file));
  return rel === '' || (rel && !rel.startsWith('..' + path.sep) && rel !== '..' && !path.isAbsolute(rel));
}
function projectRelative(file) {
  var rel = path.relative(paths.PROJECT_ROOT, file);
  if (!rel || rel.startsWith('..' + path.sep) || rel === '..' || path.isAbsolute(rel)) throw new Error('artifact-path-outside-project');
  return rel.split(path.sep).join('/');
}
function resolveArtifactPath(relativePath, generationId, persistence, readContext) {
  var scope = readContext || CONTROL_READ_SCOPE;
  if (typeof relativePath !== 'string' || !relativePath || relativePath.indexOf('\\') >= 0 || relativePath.split('/').some(function (part) { return !part || part === '.' || part === '..'; })) {
    throw new Error('artifact-path-invalid');
  }
  var absolute = path.resolve(scope.projectRoot, relativePath);
  var allowed = persistence === 'committed'
    ? path.join(scope.artifactsDir, generationId)
    : scope.reportsDir;
  if (!inside(allowed, absolute) || absolute === allowed) throw new Error('artifact-path-outside-generation');
  return absolute;
}

function validTimestamp(value, nullable) {
  return nullable && value === null ? true : typeof value === 'string' && Number.isFinite(Date.parse(value));
}

function validatePointer(value) {
  return exactKeys(value, ['committedAt', 'generationId', 'manifestHash', 'schemaVersion']) && value.schemaVersion === 1 &&
    GENERATION_RE.test(String(value.generationId || '')) && HASH_RE.test(String(value.manifestHash || '')) && validTimestamp(value.committedAt, false);
}

function validateManifestShape(value, expectedId, readContext) {
  var fields = ['artifacts', 'committedAt', 'createdAt', 'currentHash', 'environmentId', 'generationId', 'previousHash',
    'schemaVersion', 'sourceFingerprint', 'sourceKind', 'state'];
  if (!exactKeys(value, fields) || value.schemaVersion !== 1 || value.generationId !== expectedId || !GENERATION_RE.test(value.generationId) ||
      ['local', 'dev', 'stage', 'prod'].indexOf(value.environmentId) < 0 || ['openapi', 'postman'].indexOf(value.sourceKind) < 0 ||
      !HASH_RE.test(String(value.sourceFingerprint || '')) || (value.previousHash !== null && !HASH_RE.test(String(value.previousHash || ''))) ||
      !HASH_RE.test(String(value.currentHash || '')) || ['staged', 'committed'].indexOf(value.state) < 0 ||
      !validTimestamp(value.createdAt, false) || !validTimestamp(value.committedAt, true) || !Array.isArray(value.artifacts) ||
      value.artifacts.length < 2 || value.artifacts.length > 260) return false;
  var seen = Object.create(null), seenPath = Object.create(null);
  for (var i = 0; i < value.artifacts.length; i++) {
    var row = value.artifacts[i];
    var supportedArtifactSchema = row && row.schemaVersion ===
      (row.role === 'change-report' ? 2 : 1);
    if (!exactKeys(row, ['hash', 'path', 'persistence', 'required', 'role', 'schemaVersion', 'size']) ||
        typeof row.role !== 'string' || !/^(normalized-spec|source-descriptor|inventory|area:[a-z0-9][a-z0-9-]*|change-report|refresh-report)$/.test(row.role) ||
        seen[row.role] || !supportedArtifactSchema ||
        !Number.isSafeInteger(row.size) || row.size < 0 || row.size > MAX_ARTIFACT ||
        !HASH_RE.test(String(row.hash || '')) || ['committed', 'runtime'].indexOf(row.persistence) < 0 || typeof row.required !== 'boolean' ||
        seenPath[row.path]) return false;
    var committedRole = row.role === 'normalized-spec' || row.role === 'source-descriptor' || row.role === 'inventory' || row.role.indexOf('area:') === 0;
    var runtimeRole = row.role === 'change-report' || row.role === 'refresh-report';
    if ((committedRole && (row.persistence !== 'committed' || row.required !== true)) ||
        (runtimeRole && (row.persistence !== 'runtime' || row.required !== false))) return false;
    seen[row.role] = 1; seenPath[row.path] = 1;
    try { resolveArtifactPath(row.path, value.generationId, row.persistence, readContext); } catch (e) { return false; }
  }
  if (!seen.inventory ||
      (value.sourceKind === 'openapi' && (!seen['normalized-spec'] || seen['source-descriptor'])) ||
      (value.sourceKind === 'postman' && (!seen['source-descriptor'] || seen['normalized-spec']))) return false;
  return true;
}

function validateGeneration(pointer, manifest, manifestBytes, readContext) {
  var scope = readContext || CONTROL_READ_SCOPE;
  if (!validatePointer(pointer) || sha(manifestBytes) !== pointer.manifestHash ||
      !validateManifestShape(manifest, pointer.generationId, scope) ||
      manifest.state !== 'committed' || manifest.committedAt !== pointer.committedAt) return { ok: false, error: 'generation-contract-invalid' };
  var artifacts = Object.create(null), inventory = null, optionalArtifactIssues = [];
  for (var i = 0; i < manifest.artifacts.length; i++) {
    var row = manifest.artifacts[i], absolute;
    try { absolute = resolveArtifactPath(row.path, manifest.generationId, row.persistence, scope); }
    catch (e) { return { ok: false, error: 'generation-artifact-path-invalid' }; }
    var bytes;
    try { bytes = safeBytes(absolute, Math.min(MAX_ARTIFACT, row.size), !row.required, scope); }
    catch (e2) {
      if (!row.required) {
        artifacts[row.role] = null;
        optionalArtifactIssues.push({ role: row.role, error: 'generation-optional-artifact-invalid' });
        continue;
      }
      return { ok: false, error: 'generation-artifact-invalid', role: row.role };
    }
    if (bytes === null) { artifacts[row.role] = null; continue; }
    if (bytes.length !== row.size || sha(bytes) !== row.hash) {
      if (!row.required) {
        artifacts[row.role] = null;
        optionalArtifactIssues.push({ role: row.role, error: 'generation-optional-artifact-hash-mismatch' });
        continue;
      }
      return { ok: false, error: 'generation-artifact-hash-mismatch', role: row.role };
    }
    artifacts[row.role] = absolute;
    if (row.role === 'inventory') {
      try { inventory = parseObject(bytes); } catch (e3) { return { ok: false, error: 'generation-inventory-invalid' }; }
    }
  }
  if (!inventory) return { ok: false, error: 'generation-inventory-missing' };
  var inventoryAreas = Object.keys(inventory.areas || {}).sort();
  var areaCount = inventory.stats && Number.isInteger(inventory.stats.areas) ? inventory.stats.areas : inventoryAreas.length;
  var presentAreas = Object.keys(artifacts).filter(function (role) { return role.indexOf('area:') === 0 && artifacts[role]; })
    .map(function (role) { return role.slice(5); }).sort();
  if (areaCount !== inventoryAreas.length || inventoryAreas.join('\0') !== presentAreas.join('\0')) {
    return { ok: false, error: 'generation-area-set-invalid' };
  }
  if (sha(safeBytes(artifacts.inventory, MAX_ARTIFACT, false, scope)) !== manifest.currentHash) return { ok: false, error: 'generation-current-hash-invalid' };
  return { ok: true, mode: 'generation', pointer: pointer, manifest: manifest, artifacts: artifacts, inventory: inventory,
    snapshotHash: manifest.currentHash, environmentId: manifest.environmentId,
    optionalArtifactIssues: optionalArtifactIssues };
}

function currentFromScope(readContext, recoverEmptyResidue, expectedPointerHash) {
  var scope = readContext || CONTROL_READ_SCOPE;
  var pointerBytes;
  try { pointerBytes = safeBytes(scope.pointerFile, MAX_POINTER, true, scope); }
  catch (e) { return { ok: false, mode: 'invalid', error: 'generation-pointer-invalid' }; }
  if (expectedPointerHash !== undefined &&
      sha(pointerBytes === null ? Buffer.alloc(0) : pointerBytes) !== expectedPointerHash) {
    return { ok: false, mode: 'invalid', error: 'generation-pointer-pin-mismatch' };
  }
  if (pointerBytes === null) {
    try {
      if (recoverEmptyResidue) recoverEmptyGenerationResidue();
      if (generationEvidencePresent(scope)) return { ok: false, mode: 'invalid', error: 'generation-pointer-missing' };
    } catch (evidenceError) {
      return { ok: false, mode: 'invalid', error: 'generation-evidence-invalid' };
    }
    return { ok: true, mode: 'none', snapshotHash: null, environmentId: null, inventory: null, artifacts: {} };
  }
  var pointer;
  try { pointer = parseObject(pointerBytes); }
  catch (e2) { return { ok: false, mode: 'invalid', error: 'generation-pointer-invalid' }; }
  if (!validatePointer(pointer)) return { ok: false, mode: 'invalid', error: 'generation-pointer-invalid' };
  var manifestFile = path.join(scope.generationsDir, pointer.generationId + '.json'), manifestBytes, manifest;
  try { manifestBytes = safeBytes(manifestFile, MAX_MANIFEST, false, scope); manifest = parseObject(manifestBytes); }
  catch (e3) { return { ok: false, mode: 'invalid', error: 'generation-manifest-invalid' }; }
  return validateGeneration(pointer, manifest, manifestBytes, scope);
}

function current() {
  return currentFromScope(CONTROL_READ_SCOPE, true);
}

// Task helpers need the committed generation that is physically present in
// their immutable execution checkout, even when a control-plane refresh has
// advanced after provisioning. This reader shares the exact validation
// predicates above but has no cleanup/publication side effects.
function currentAtProjectRoot(projectRoot, expectedPointerHash) {
  if (expectedPointerHash !== undefined && !HASH_RE.test(String(expectedPointerHash || ''))) {
    return { ok: false, mode: 'invalid', error: 'generation-pointer-pin-invalid' };
  }
  var scope;
  try {
    scope = readScope(projectRoot);
    var rootStat = fs.lstatSync(scope.projectRoot);
    if (!rootStat.isDirectory() || rootStat.isSymbolicLink()) {
      return { ok: false, mode: 'invalid', error: 'generation-project-root-invalid' };
    }
  } catch (error) {
    return { ok: false, mode: 'invalid', error: 'generation-project-root-invalid' };
  }
  return currentFromScope(scope, false, expectedPointerHash);
}

function readArea(name) {
  if (!AREA_RE.test(String(name || ''))) return null;
  var snapshot = current();
  if (!snapshot.ok) return { present: false, invalid: true, error: snapshot.error };
  var file = snapshot.mode === 'generation' ? snapshot.artifacts['area:' + name] : null;
  if (!file) return { present: false };
  try { return Object.assign({}, parseObject(safeBytes(file, MAX_ARTIFACT, false)), { present: true }); }
  catch (e) { return { present: false, invalid: true, error: 'generation-artifact-invalid' }; }
}

function atomicWrite(file, bytes, mode) {
  var maxBytes = file === POINTER_FILE ? MAX_POINTER : (path.dirname(file) === GENERATIONS_DIR ? MAX_MANIFEST : MAX_ARTIFACT);
  if (bytes.length > maxBytes) throw new Error('generation-artifact-size-limit');
  var published = fileGuards.atomicReplaceRegularFileResult(paths.PROJECT_ROOT, path.dirname(file), file, bytes,
    { create: true, directoryMode: 0o755, mode: mode || 0o644, maxBytes: maxBytes });
  if (!published.ok) throw new Error(published.code || 'generation-write-failed');
  return published;
}

function copyArtifact(source, target) {
  var bytes = safeBytes(source, MAX_ARTIFACT, false);
  atomicWrite(target, bytes, 0o644);
  return { bytes: bytes, size: bytes.length, hash: sha(bytes) };
}

function createGenerationId() {
  var stamp = new Date().toISOString().replace(/[-:]/g, '').replace(/\.\d{3}Z$/, 'Z');
  return 'gen-' + stamp + '-' + crypto.randomBytes(6).toString('hex');
}

function publish(options) {
  options = options || {};
  var generationId = options.generationId || createGenerationId();
  if (!GENERATION_RE.test(generationId) || ['local', 'dev', 'stage', 'prod'].indexOf(options.environmentId) < 0 ||
      ['openapi', 'postman'].indexOf(options.sourceKind) < 0 || !HASH_RE.test(String(options.sourceFingerprint || ''))) {
    return { ok: false, error: 'generation-options-invalid' };
  }
  var before = current();
  if (!before.ok) return { ok: false, error: before.error };
  if ((options.expectedSnapshotHash || null) !== (before.snapshotHash || null)) return { ok: false, error: 'write-conflict' };
  var previousPointerBytes = null;
  try { previousPointerBytes = safeBytes(POINTER_FILE, MAX_POINTER, true); }
  catch (pointerReadError) { return { ok: false, error: 'generation-pointer-invalid' }; }
  var pointerPublished = false;
  var publishedPointerBytes = null;
  var destination = path.join(ARTIFACTS_DIR, generationId);
  try {
    fs.lstatSync(destination);
    return { ok: false, error: 'generation-id-conflict' };
  } catch (destinationError) {
    if (!destinationError || destinationError.code !== 'ENOENT') return { ok: false, error: 'generation-id-conflict' };
  }
  if (!fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, destination, { create: true, mode: 0o755 })) {
    return { ok: false, error: 'generation-directory-unsafe' };
  }
  var artifacts = [], inventoryCopy;
  function committed(role, source, relative) {
    var target = path.join(destination, relative), copied = copyArtifact(source, target);
    var row = { role: role, path: projectRelative(target), schemaVersion: 1, size: copied.size,
      hash: copied.hash, persistence: 'committed', required: true };
    artifacts.push(row); return copied;
  }
  try {
    if (options.sourceKind === 'openapi') committed('normalized-spec', options.specFile, path.join('spec', 'openapi.json'));
    else committed('source-descriptor', options.sourceDescriptorFile, 'source-descriptor.json');
    inventoryCopy = committed('inventory', options.inventoryFile, 'endpoint-inventory.json');
    var areaNames = fs.readdirSync(options.areasDir).filter(function (name) { return name.endsWith('.json'); }).map(function (name) { return name.slice(0, -5); }).sort();
    if (areaNames.length > 256 || areaNames.some(function (name) { return !AREA_RE.test(name); })) throw new Error('generation-area-set-invalid');
    areaNames.forEach(function (name) { committed('area:' + name, path.join(options.areasDir, name + '.json'), path.join('areas', name + '.json')); });
    [
      ['change-report', options.changeReportFile],
      ['refresh-report', options.refreshReportFile]
    ].forEach(function (entry) {
      if (!entry[1]) return;
      var runtimeMax = entry[0] === 'change-report' ? MAX_ARTIFACT : 512 * 1024;
      var bytes = safeBytes(entry[1], runtimeMax, false);
      artifacts.push({ role: entry[0], path: projectRelative(entry[1]),
        schemaVersion: entry[0] === 'change-report' ? 2 : 1, size: bytes.length,
        hash: sha(bytes), persistence: 'runtime', required: false });
    });
    var inventory = parseObject(inventoryCopy.bytes);
    var inventoryAreaNames = Object.keys(inventory.areas || {}).sort();
    var inventoryAreas = inventory.stats && Number.isInteger(inventory.stats.areas) ? inventory.stats.areas : inventoryAreaNames.length;
    if (inventoryAreas !== inventoryAreaNames.length || inventoryAreaNames.join('\0') !== areaNames.join('\0')) {
      throw new Error('generation-area-set-invalid');
    }
    var now = new Date().toISOString();
    var manifest = { schemaVersion: 1, generationId: generationId, environmentId: options.environmentId,
      sourceKind: options.sourceKind, sourceFingerprint: options.sourceFingerprint, previousHash: before.snapshotHash || null,
      currentHash: inventoryCopy.hash, state: 'committed', createdAt: options.createdAt || now, committedAt: now, artifacts: artifacts };
    if (!validateManifestShape(manifest, generationId)) throw new Error('generation-manifest-invalid');
    var manifestBytes = Buffer.from(JSON.stringify(manifest, null, 2) + '\n');
    atomicWrite(path.join(GENERATIONS_DIR, generationId + '.json'), manifestBytes, 0o644);
    var pointer = { schemaVersion: 1, generationId: generationId, manifestHash: sha(manifestBytes), committedAt: now };
    publishedPointerBytes = Buffer.from(JSON.stringify(pointer, null, 2) + '\n');
    atomicWrite(POINTER_FILE, publishedPointerBytes, 0o644);
    pointerPublished = true;
    var after = current();
    if (!after.ok || after.mode !== 'generation' || after.manifest.generationId !== generationId) throw new Error(after.error || 'generation-publication-verification-failed');
    return { ok: true, generationId: generationId, currentHash: after.snapshotHash, previousHash: before.snapshotHash || null,
      manifest: after.manifest, pointer: after.pointer };
  } catch (e2) {
    if (pointerPublished) {
      try {
        if (previousPointerBytes === null) {
          if (!fileGuards.unlinkRegularFileMatchingUnder(paths.PROJECT_ROOT, MANIFESTS_DIR, POINTER_FILE, MAX_POINTER, publishedPointerBytes)) {
            throw new Error('pointer-delete-unproven');
          }
        }
        else atomicWrite(POINTER_FILE, previousPointerBytes, 0o644);
      } catch (rollbackError) {
        return { ok: false, error: 'generation-pointer-rollback-failed', abandonedGenerationId: generationId };
      }
    }
    // Pointer is deliberately never rolled forward on any failure. Once any
    // published pointer has been restored, this writer still owns the new
    // generation id and can remove its bounded tree immediately.
    var cleaned = cleanupGeneration(generationId);
    return Object.assign({ ok: false, error: publicationError(e2) },
      cleaned ? {} : { abandonedGenerationId: generationId });
  }
}

function generationIdsForClear() {
  var ids = Object.create(null);
  var artifactNames = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, ARTIFACTS_DIR, 512);
  var manifestNames = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, GENERATIONS_DIR, 512);
  if (!artifactNames.ok || !manifestNames.ok) return null;
  for (var i = 0; i < artifactNames.names.length; i++) {
    var artifactName = artifactNames.names[i];
    if (artifactName === '.gitkeep') continue;
    if (!GENERATION_RE.test(artifactName)) return null;
    ids[artifactName] = true;
  }
  for (var j = 0; j < manifestNames.names.length; j++) {
    var manifestName = manifestNames.names[j];
    if (manifestName === '.gitkeep') continue;
    var match = /^(gen-[0-9]{8}T[0-9]{6}Z-[0-9a-f]{12})\.json$/.exec(manifestName);
    if (!match) return null;
    ids[match[1]] = true;
  }
  return Object.keys(ids).sort();
}

function clearAll(expectedSnapshotHash) {
  if (expectedSnapshotHash !== null && !HASH_RE.test(String(expectedSnapshotHash || ''))) {
    return { ok: false, error: 'write-conflict' };
  }
  var before = current();
  if (before.ok && (before.snapshotHash || null) !== expectedSnapshotHash) return { ok: false, error: 'write-conflict' };
  if (!before.ok && expectedSnapshotHash !== null) return { ok: false, error: 'write-conflict' };
  var ids = generationIdsForClear();
  if (!ids) return { ok: false, error: 'snapshot-invalid' };
  for (var i = 0; i < ids.length; i++) {
    if (!cleanupGeneration(ids[i])) return { ok: false, error: 'snapshot-invalid' };
  }
  var pointerBytes;
  try { pointerBytes = safeBytes(POINTER_FILE, MAX_POINTER, true); }
  catch (pointerError) { return { ok: false, error: 'snapshot-invalid' }; }
  if (pointerBytes !== null && !fileGuards.unlinkRegularFileMatchingUnder(
    paths.PROJECT_ROOT, MANIFESTS_DIR, POINTER_FILE, MAX_POINTER, pointerBytes
  )) return { ok: false, error: 'snapshot-invalid' };
  var after = current();
  return after.ok && after.mode === 'none'
    ? { ok: true }
    : { ok: false, error: 'snapshot-invalid' };
}

module.exports = {
  POINTER_FILE: POINTER_FILE,
  sha: sha,
  current: current,
  currentAtProjectRoot: currentAtProjectRoot,
  readArea: readArea,
  createGenerationId: createGenerationId,
  publish: publish,
  clearAll: clearAll
};
