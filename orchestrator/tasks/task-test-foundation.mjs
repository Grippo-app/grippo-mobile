#!/usr/bin/env node

// ---------------------------------------------------------------------------
// Test-foundation doctor + bootstrap coordinator engine (improvement 05,
// Phase 2). Primitives only: no task lifecycle routing lives here — task-prep
// and Step 0 call these verbs once the policy is activated.
//
//   doctor      — read-only static inspection of a generated product tree →
//                 typed state (READY | ABSENT_CAN_INSTALL | PARTIAL_CORRUPT |
//                 CONFLICTING_STACK | UNSUPPORTED_VERSION |
//                 TOOLCHAIN_UNAVAILABLE) + content-hashed inventory. It never
//                 runs Gradle; runnable proof belongs to the structural-gate
//                 bootstrap fixture and the certification executor.
//   claim/advance/inspect — durable no-clobber marker protocol under
//                 .cache/tasks/test-foundation/<intent-hex64>.json with
//                 bounded phases claimed → child-created → child-promoted →
//                 ready. Live ownership requires an exact live owner process;
//                 a stable phase never impersonates a live lease.
// ---------------------------------------------------------------------------

import { createRequire } from 'node:module';
import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';

const require = createRequire(import.meta.url);
const contract = require('./task-test-foundation-contract.cjs');

const HERE = path.dirname(new URL(import.meta.url).pathname);

const CATALOG_ALIASES = Object.freeze([
  'kotlin-test = ', 'kotlinx-coroutines-test = ', 'turbine = { module',
  'ktor-client-mock = ', 'koin-test = ', 'androidx-room-testing = ',
  'androidx-test-runner = { module', 'androidx-test-core = { module',
  'compose-ui-test-manifest = { module'
]);
const CONVENTION_SOURCES = Object.freeze([
  'KmpTestConventionPlugin.kt', 'CoroutinesTestConventionPlugin.kt',
  'FlowTestConventionPlugin.kt', 'NetworkTestConventionPlugin.kt',
  'DiTestConventionPlugin.kt', 'RoomTestConventionPlugin.kt',
  'ComposeUiTestConventionPlugin.kt', 'TestCapabilityEntryTask.kt'
]);
const REGISTRATION_IDS = Object.freeze([
  'kmp.test.convention', 'coroutines.test.convention', 'flow.test.convention',
  'network.test.convention', 'di.test.convention', 'room.test.convention',
  'compose.ui.test.convention'
]);
const ROOT_AGGREGATES = Object.freeze([
  'allHostTests', 'allIosSimulatorTests', 'allAndroidDeviceTests',
  'allScreenshotTests', 'allConfiguredTests', 'testCapabilityInventory'
]);
const CONFLICT_MARKERS = Object.freeze(['io.kotest', 'io.mockk', 'org.mockito', 'app.cash.paparazzi']);
const SUPPORTED_PINS = Object.freeze({ agp: 'agp = "9.0.1"', kotlin: 'kotlin = "2.3.21"' });
const MAX_TEXT_BYTES = 2 * 1024 * 1024;
const MAX_SOURCE_DEPTH = 8;
const MAX_SOURCE_ENTRIES = 512;

function fail(code, message) { throw new contract.TestFoundationError(code, message); }

function statFingerprint(stat) {
  return [stat.dev, stat.ino, stat.size, stat.nlink, stat.mode, stat.uid, stat.mtimeNs, stat.ctimeNs]
    .map((value) => String(value)).join(':');
}

function canonicalDirectoryRoot(rootPath, { create = false } = {}) {
  const resolved = path.resolve(rootPath);
  try {
    const stat = fs.lstatSync(resolved);
    if (!stat.isDirectory() || stat.isSymbolicLink()) fail('UNSAFE_PATH', 'root must be a real directory: ' + resolved);
  } catch (error) {
    if (error instanceof contract.TestFoundationError) throw error;
    if (error.code !== 'ENOENT' || !create) {
      if (error.code === 'ENOENT') return null;
      fail('UNREADABLE', 'root inspection failed (' + error.code + ')');
    }
    try { fs.mkdirSync(resolved, { mode: 0o700 }); }
    catch (mkdirError) { fail('UNWRITABLE', 'root create failed (' + mkdirError.code + ')'); }
  }
  return fs.realpathSync(resolved);
}

function safePathUnder(root, segments, { createDirectories = false } = {}) {
  let current = root;
  for (const segment of segments.slice(0, -1)) {
    if (!/^[A-Za-z0-9._-]+$/.test(segment) || segment === '.' || segment === '..') {
      fail('UNSAFE_PATH', 'unsafe path segment');
    }
    current = path.join(current, segment);
    try {
      const stat = fs.lstatSync(current);
      if (!stat.isDirectory() || stat.isSymbolicLink()) fail('UNSAFE_PATH', 'directory symlink/non-directory rejected: ' + current);
    } catch (error) {
      if (error instanceof contract.TestFoundationError) throw error;
      if (error.code !== 'ENOENT') fail('UNREADABLE', 'directory inspection failed (' + error.code + ')');
      if (!createDirectories) return path.join(root, ...segments);
      try { fs.mkdirSync(current, { mode: 0o700 }); }
      catch (mkdirError) { fail('UNWRITABLE', 'directory create failed (' + mkdirError.code + ')'); }
    }
  }
  return path.join(root, ...segments);
}

function readTextIfExists(root, ...segments) {
  const filePath = safePathUnder(root, segments);
  let fd;
  try { fd = fs.openSync(filePath, fs.constants.O_RDONLY | fs.constants.O_NOFOLLOW | fs.constants.O_NONBLOCK); }
  catch (error) {
    if (error.code === 'ENOENT') return null;
    if (error.code === 'ELOOP') fail('UNSAFE_PATH', 'symlink rejected: ' + filePath);
    fail('UNREADABLE', filePath + ' (' + error.code + ')');
  }
  try {
    const before = fs.fstatSync(fd, { bigint: true });
    if (!before.isFile() || before.nlink !== 1n) fail('UNSAFE_PATH', 'not a unique regular file: ' + filePath);
    if (before.size > BigInt(MAX_TEXT_BYTES)) fail('UNSAFE_PATH', 'file exceeds bounded read: ' + filePath);
    const raw = fs.readFileSync(fd, 'utf8');
    const after = fs.fstatSync(fd, { bigint: true });
    if (statFingerprint(before) !== statFingerprint(after) || BigInt(Buffer.byteLength(raw)) !== after.size) {
      fail('UNSAFE_PATH', 'file changed during bounded read: ' + filePath);
    }
    return raw;
  } finally { fs.closeSync(fd); }
}

// Bounded recursive listing of Kotlin sources under one directory. The
// single-owner audit must see the whole convention source tree: a helper in a
// package sub-directory is still a second `withHostTest` owner to AGP.
function kotlinSourcesUnder(root, segments, depth = 0) {
  if (depth > MAX_SOURCE_DEPTH) fail('UNSAFE_PATH', 'convention source tree is deeper than the bounded scan');
  const directory = path.dirname(safePathUnder(root, [...segments, '.directory-probe']));
  let entries = [];
  try { entries = fs.readdirSync(directory, { withFileTypes: true }); }
  catch (error) { return []; }
  if (entries.length > MAX_SOURCE_ENTRIES) fail('UNSAFE_PATH', 'convention source directory exceeds the bounded scan');
  const found = [];
  for (const entry of entries.sort((left, right) => left.name.localeCompare(right.name))) {
    if (entry.isSymbolicLink()) fail('UNSAFE_PATH', 'symlink rejected: ' + path.join(directory, entry.name));
    if (entry.isDirectory()) found.push(...kotlinSourcesUnder(root, [...segments, entry.name], depth + 1));
    else if (entry.isFile() && entry.name.endsWith('.kt')) found.push([...segments, entry.name]);
  }
  return found;
}

// Read-only doctor. `productRoot` must be a bootstrapped product (its own
// settings.gradle.kts); the template repository is not a doctor subject.
export function doctor({ productRoot }) {
  if (typeof productRoot !== 'string' || productRoot.length === 0) fail('OPTIONS_INVALID', 'productRoot required');
  const root = canonicalDirectoryRoot(productRoot);
  if (root === null) fail('NOT_A_PRODUCT', 'product root does not exist: ' + path.resolve(productRoot));
  const settings = readTextIfExists(root, 'settings.gradle.kts');
  if (settings === null) fail('NOT_A_PRODUCT', 'no settings.gradle.kts at ' + root + ' — the doctor inspects generated products only');

  const catalog = readTextIfExists(root, 'gradle', 'libs.versions.toml') || '';
  const registrations = readTextIfExists(root, 'build-logic', 'convention', 'build.gradle.kts') || '';
  const rootBuild = readTextIfExists(root, 'build.gradle.kts') || '';

  const inventory = {
    catalogAliases: {},
    conventionSources: {},
    registrations: {},
    rootAggregates: {},
    singleHostOwner: null,
    conflicts: [],
    pins: {},
    toolchain: {}
  };

  for (const alias of CATALOG_ALIASES) inventory.catalogAliases[alias.trim()] = catalog.includes(alias);
  for (const source of CONVENTION_SOURCES) {
    inventory.conventionSources[source] =
      readTextIfExists(root, 'build-logic', 'convention', 'src', 'main', 'kotlin', source) !== null;
  }
  for (const id of REGISTRATION_IDS) inventory.registrations[id] = registrations.includes('"' + id + '"');
  for (const aggregate of ROOT_AGGREGATES) inventory.rootAggregates[aggregate] = rootBuild.includes('"' + aggregate + '"');

  // Single-owner audit over the whole convention source tree: withHostTest may
  // appear exactly once, inside KmpTestConventionPlugin. Module scripts are out
  // of this scan's reach — the doctrine forbidding an enabler there is enforced
  // by AGP itself, which fails the configuration on the second call.
  let enablerCalls = 0;
  let enablerOutsideBase = false;
  for (const relative of kotlinSourcesUnder(root, ['build-logic', 'convention', 'src', 'main', 'kotlin'])) {
    const text = readTextIfExists(root, ...relative) || '';
    const hits = (text.match(/withHostTest\s*\{/g) || []).length;
    enablerCalls += hits;
    if (hits > 0 && relative[relative.length - 1] !== 'KmpTestConventionPlugin.kt') enablerOutsideBase = true;
  }
  inventory.singleHostOwner = enablerCalls === 1 && !enablerOutsideBase;

  for (const marker of CONFLICT_MARKERS) {
    if (catalog.includes(marker)) inventory.conflicts.push(marker);
  }
  inventory.pins.agp = catalog.includes(SUPPORTED_PINS.agp);
  inventory.pins.kotlin = catalog.includes(SUPPORTED_PINS.kotlin);
  inventory.toolchain.foojay = settings.includes('foojay-resolver-convention');

  const aliasFlags = Object.values(inventory.catalogAliases);
  const sourceFlags = Object.values(inventory.conventionSources);
  const registrationFlags = Object.values(inventory.registrations);
  const aggregateFlags = Object.values(inventory.rootAggregates);
  const allPresent = [...aliasFlags, ...sourceFlags, ...registrationFlags, ...aggregateFlags].every(Boolean);
  const nonePresent = [...aliasFlags, ...sourceFlags, ...registrationFlags, ...aggregateFlags].every((flag) => !flag);

  let state;
  if (inventory.conflicts.length > 0) state = 'CONFLICTING_STACK';
  else if (!inventory.pins.agp || !inventory.pins.kotlin) state = 'UNSUPPORTED_VERSION';
  else if (!inventory.toolchain.foojay) state = 'TOOLCHAIN_UNAVAILABLE';
  else if (allPresent && inventory.singleHostOwner) state = 'READY';
  // A foreign enabler makes the tree unsafe to install into even when every
  // foundation artifact is absent: the install would create the second
  // withHostTest owner this doctor exists to prevent.
  else if (nonePresent && enablerCalls === 0) state = 'ABSENT_CAN_INSTALL';
  else state = 'PARTIAL_CORRUPT';

  const doctorInventoryHash = contract.inventoryHashOf(inventory);
  return { state, inventory, doctorInventoryHash };
}

// ---------------------------------------------------------------------------
// Coordinator marker primitives.
// ---------------------------------------------------------------------------

function markerDir(cacheRoot, { create = false } = {}) {
  const root = canonicalDirectoryRoot(cacheRoot, { create });
  if (root === null) return null;
  const probe = safePathUnder(root, ['tasks', 'test-foundation', '.marker-probe'], { createDirectories: create });
  const dir = path.dirname(probe);
  if (create) {
    const stat = fs.lstatSync(dir);
    if (typeof process.getuid === 'function' && stat.uid !== process.getuid()) fail('UNSAFE_PATH', 'marker directory has foreign owner');
    fs.chmodSync(dir, 0o700);
  }
  return dir;
}

function markerPath(cacheRoot, intentHash, { create = false } = {}) {
  const dir = markerDir(cacheRoot, { create });
  if (dir === null) return null;
  return path.join(dir, contract.markerPathComponent(intentHash) + '.json');
}

function nowIso() { return new Date().toISOString(); }

function readMarkerRecord(filePath) {
  if (filePath === null) return null;
  let fd;
  try { fd = fs.openSync(filePath, fs.constants.O_RDONLY | fs.constants.O_NOFOLLOW | fs.constants.O_NONBLOCK); }
  catch (error) {
    if (error.code === 'ENOENT') return null;
    if (error.code === 'ELOOP') fail('UNSAFE_PATH', 'symlink marker rejected');
    fail('UNREADABLE', 'marker open failed (' + error.code + ')');
  }
  try {
    const before = fs.fstatSync(fd, { bigint: true });
    if (!before.isFile()) fail('UNSAFE_PATH', 'marker is not a regular file');
    if (before.nlink !== 1n) fail('UNSAFE_PATH', 'marker nlink must be 1');
    if ((before.mode & 0o777n) !== 0o600n) fail('UNSAFE_PATH', 'marker mode must be 0600');
    if (typeof process.getuid === 'function' && before.uid !== BigInt(process.getuid())) fail('UNSAFE_PATH', 'marker has foreign owner');
    if (before.size > BigInt(contract.MARKER_MAX_BYTES)) fail('UNSAFE_PATH', 'marker exceeds bounded size');
    const raw = fs.readFileSync(fd, 'utf8');
    const after = fs.fstatSync(fd, { bigint: true });
    if (statFingerprint(before) !== statFingerprint(after) || BigInt(Buffer.byteLength(raw)) !== after.size) {
      fail('UNSAFE_PATH', 'marker changed during bounded read');
    }
    let parsed;
    try { parsed = JSON.parse(raw); }
    catch (error) { fail('MARKER_INVALID', 'marker is not valid JSON'); }
    return { marker: contract.validateMarker(parsed), fingerprint: statFingerprint(after) };
  } finally { fs.closeSync(fd); }
}

function readMarkerFile(filePath) {
  const record = readMarkerRecord(filePath);
  return record === null ? null : record.marker;
}

function assertPathGeneration(filePath, fingerprint) {
  let stat;
  try { stat = fs.lstatSync(filePath, { bigint: true }); }
  catch (error) { fail('UNSAFE_PATH', 'marker generation disappeared (' + error.code + ')'); }
  if (!stat.isFile() || stat.isSymbolicLink() || statFingerprint(stat) !== fingerprint) {
    fail('UNSAFE_PATH', 'marker generation changed before promotion');
  }
}

export function claimFoundation({ cacheRoot, intentHash, sessionId, pid = process.pid, startedAt }) {
  const filePath = markerPath(cacheRoot, intentHash, { create: true });
  const dir = path.dirname(filePath);
  const stamp = nowIso();
  const marker = {
    version: 1,
    domain: contract.MARKER_DOMAIN,
    intentHash,
    phase: 'claimed',
    childStem: null,
    ownerSessionId: sessionId,
    ownerPid: pid,
    ownerStartedAt: startedAt || stamp,
    createdAt: stamp,
    updatedAt: stamp,
    markerHash: 'sha256:' + '0'.repeat(64)
  };
  marker.markerHash = contract.markerHashOf(marker);
  contract.validateMarker(marker);
  let fd;
  try {
    fd = fs.openSync(filePath, fs.constants.O_WRONLY | fs.constants.O_CREAT | fs.constants.O_EXCL | fs.constants.O_NOFOLLOW, 0o600);
  } catch (error) {
    if (error.code === 'EEXIST') {
      const existing = readMarkerFile(filePath);
      return { claimed: false, code: 'FOUNDATION_IN_PROGRESS', marker: existing };
    }
    fail('UNWRITABLE', 'marker create failed (' + error.code + ')');
  }
  try {
    fs.writeFileSync(fd, JSON.stringify(marker, null, 2) + '\n');
    fs.fsyncSync(fd);
    const stat = fs.fstatSync(fd, { bigint: true });
    if (!stat.isFile() || stat.nlink !== 1n || (stat.mode & 0o777n) !== 0o600n) {
      fail('UNSAFE_PATH', 'new marker did not retain owner-only unique-file invariants');
    }
  } finally { fs.closeSync(fd); }
  const dirFd = fs.openSync(dir, fs.constants.O_RDONLY);
  try { fs.fsyncSync(dirFd); } finally { fs.closeSync(dirFd); }
  return { claimed: true, code: 'FOUNDATION_CLAIMED', marker };
}

// Remove one own-format staging leftover. Anything that is not an exact
// 0600 single-link regular file owned by this uid stays untouched and keeps
// the advance fail-closed.
function discardStaleStaging(tempPath) {
  let stat;
  try { stat = fs.lstatSync(tempPath, { bigint: true }); }
  catch (error) {
    if (error.code === 'ENOENT') return;
    fail('UNWRITABLE', 'marker staging leftover cannot be inspected (' + error.code + ')');
  }
  if (!stat.isFile() || stat.nlink !== 1n || (stat.mode & 0o777n) !== 0o600n ||
      stat.uid !== BigInt(process.getuid ? process.getuid() : stat.uid)) {
    fail('UNSAFE_PATH', 'marker staging path is not an own single-link regular file: ' + tempPath);
  }
  try { fs.unlinkSync(tempPath); }
  catch (error) { if (error.code !== 'ENOENT') fail('UNWRITABLE', 'marker staging leftover cannot be removed (' + error.code + ')'); }
}

export function advanceFoundation({ cacheRoot, intentHash, sessionId, pid = process.pid, phase, childStem = null }) {
  const filePath = markerPath(cacheRoot, intentHash);
  const currentRecord = readMarkerRecord(filePath);
  if (currentRecord === null) fail('MARKER_MISSING', 'no marker for this intent');
  const current = currentRecord.marker;
  if (current.ownerSessionId !== sessionId || current.ownerPid !== pid) {
    fail('NOT_OWNER', 'only the exact claiming owner may advance the marker');
  }
  const next = {
    ...current,
    phase,
    childStem: childStem === null ? current.childStem : childStem,
    updatedAt: nowIso(),
    markerHash: 'sha256:' + '0'.repeat(64)
  };
  next.markerHash = contract.markerHashOf(next);
  contract.checkTransition(current, next);
  // No-clobber generation swap: write a sibling temp under O_EXCL, fsync,
  // rename over the owned marker, fsync the directory.
  const tempPath = filePath + '.next';
  let fd;
  try { fd = fs.openSync(tempPath, fs.constants.O_WRONLY | fs.constants.O_CREAT | fs.constants.O_EXCL | fs.constants.O_NOFOLLOW, 0o600); }
  catch (error) {
    // A staging file left by a crashed owner is not authority and is never
    // read; without this, one crash between open and rename would wedge every
    // future advance of this intent behind a permanent EEXIST.
    if (error.code !== 'EEXIST') fail('UNWRITABLE', 'marker staging failed (' + error.code + ')');
    discardStaleStaging(tempPath);
    try { fd = fs.openSync(tempPath, fs.constants.O_WRONLY | fs.constants.O_CREAT | fs.constants.O_EXCL | fs.constants.O_NOFOLLOW, 0o600); }
    catch (retryError) { fail('UNWRITABLE', 'marker staging failed (' + retryError.code + ')'); }
  }
  let promoted = false;
  try {
    fs.writeFileSync(fd, JSON.stringify(next, null, 2) + '\n');
    fs.fsyncSync(fd);
  } finally { fs.closeSync(fd); }
  try {
    assertPathGeneration(filePath, currentRecord.fingerprint);
    fs.renameSync(tempPath, filePath);
    promoted = true;
  } finally {
    if (!promoted) {
      try { fs.unlinkSync(tempPath); } catch (error) { if (error.code !== 'ENOENT') throw error; }
    }
  }
  const dirFd = fs.openSync(path.dirname(filePath), fs.constants.O_RDONLY);
  try { fs.fsyncSync(dirFd); } finally { fs.closeSync(dirFd); }
  return { advanced: true, marker: next };
}

export function inspectFoundation({ cacheRoot, intentHash }) {
  const marker = readMarkerFile(markerPath(cacheRoot, intentHash));
  if (marker === null) return { present: false, marker: null, ownerAlive: null };
  let ownerAlive = false;
  try { process.kill(marker.ownerPid, 0); ownerAlive = true; }
  catch (error) { ownerAlive = error.code === 'EPERM'; }
  return { present: true, marker, ownerAlive };
}

// ---------------------------------------------------------------------------
// CLI (used by tests and future lifecycle callers).
// ---------------------------------------------------------------------------

function cliMain(argv) {
  const [verb, ...rest] = argv;
  const options = {};
  for (let i = 0; i < rest.length; i += 2) {
    if (!rest[i].startsWith('--')) fail('CLI_INVALID', 'unknown argument: ' + rest[i]);
    options[rest[i].slice(2)] = rest[i + 1];
  }
  if (verb === 'doctor') {
    const report = doctor({ productRoot: options['product-root'] });
    process.stdout.write(JSON.stringify(report, null, 2) + '\n');
    return report.state === 'READY' || report.state === 'ABSENT_CAN_INSTALL' ? 0 : 3;
  }
  if (verb === 'claim') {
    const result = claimFoundation({
      cacheRoot: options['cache-root'], intentHash: options['intent-hash'],
      sessionId: options['session-id'], pid: options.pid ? Number(options.pid) : process.pid
    });
    process.stdout.write(JSON.stringify(result, null, 2) + '\n');
    return result.claimed ? 0 : 2;
  }
  if (verb === 'advance') {
    const result = advanceFoundation({
      cacheRoot: options['cache-root'], intentHash: options['intent-hash'],
      sessionId: options['session-id'], pid: options.pid ? Number(options.pid) : process.pid,
      phase: options.phase, childStem: options['child-stem'] || null
    });
    process.stdout.write(JSON.stringify(result, null, 2) + '\n');
    return 0;
  }
  if (verb === 'inspect') {
    const result = inspectFoundation({ cacheRoot: options['cache-root'], intentHash: options['intent-hash'] });
    process.stdout.write(JSON.stringify(result, null, 2) + '\n');
    return 0;
  }
  fail('CLI_INVALID', 'unknown verb: ' + String(verb));
}

if (process.argv[1] && path.resolve(process.argv[1]) === path.join(HERE, 'task-test-foundation.mjs')) {
  try { process.exit(cliMain(process.argv.slice(2))); }
  catch (error) {
    process.stderr.write(String(error && error.message || error) + '\n');
    process.exit(error && error.code === 'CLI_INVALID' ? 64 : 1);
  }
}
