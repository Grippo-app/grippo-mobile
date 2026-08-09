'use strict';

// ---------------------------------------------------------------------------
// Canonical byte-content snapshot primitive (pipeline improvement 02).
//
// One frozen v1 API that turns an explicit file list under an explicit root
// into an immutable, content-addressed manifest. Downstream initiatives (the
// mandatory-test evidence pipeline consumes this engine for its own
// domain-separated manifests) MUST use this module instead of inventing a
// local hash engine, comparing `git status` sets, or trusting filesystem
// metadata.
//
// Hard rules, all fail-closed:
//   - every entry hash is computed over the file BYTES read through an
//     O_NOFOLLOW descriptor — there is deliberately NO metadata-only mode
//     (mtime/size shortcuts cannot exist because no API accepts stat input);
//   - only regular files with nlink === 1 are accepted: symlinks, hardlinks,
//     FIFOs, sockets, devices and directories are rejected, and every
//     intermediate path segment must be a real directory (no symlink
//     ancestors);
//   - paths are relative, normalized, '/'-separated, unique and sorted;
//     traversal ('..'), absolute paths and NUL/backslash separators are
//     rejected before any filesystem access;
//   - per-file, total-byte and entry-count bounds are enforced;
//   - a file replaced or resized mid-read is a race error, never a silently
//     wrong hash;
//   - the aggregate hash lives in its own literal domain
//     (`content-snapshot-v1\0` + canonical JSON) and MUST NEVER be compared
//     against hashes from other domains (raw bytes sha256,
//     `task-state-snapshot-v1`, `project-source-revision`, ...).
// ---------------------------------------------------------------------------

const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const fileGuards = require('../site/server/file-guards');

const VERSION = 1;
const DOMAIN = 'content-snapshot-v1';
const MAX_FILE_BYTES = 16 * 1024 * 1024;
const MAX_TOTAL_BYTES = 256 * 1024 * 1024;
const MAX_ENTRIES = 20000;
const HASH_RE = /^sha256:[0-9a-f]{64}$/;
// Relative, normalized, '/'-separated: non-empty segments of safe bytes, no
// '.'/'..' segments, no leading/trailing/double slash. Segment charset is
// deliberately conservative (portable filenames).
const SEGMENT_RE = /^[A-Za-z0-9._][A-Za-z0-9._-]*$/;
const MANIFEST_KEYS = Object.freeze(['domain', 'entries', 'entryCount', 'snapshotHash', 'totalBytes', 'version']);
const ENTRY_KEYS = Object.freeze(['hash', 'path', 'size']);

class ContentSnapshotError extends Error {
  constructor(code, message) {
    super(code + ': ' + message);
    this.name = 'ContentSnapshotError';
    this.code = code;
  }
}

function fail(code, message) { throw new ContentSnapshotError(code, message); }

function sha256(value) {
  return 'sha256:' + crypto.createHash('sha256').update(value).digest('hex');
}

function canonicalJson(value) {
  if (value === null || typeof value !== 'object') return JSON.stringify(value);
  if (Array.isArray(value)) return '[' + value.map(canonicalJson).join(',') + ']';
  return '{' + Object.keys(value).sort().map((key) => JSON.stringify(key) + ':' + canonicalJson(value[key])).join(',') + '}';
}

function checkRelPath(relPath) {
  if (typeof relPath !== 'string' || relPath.length === 0) fail('PATH_INVALID', 'path must be a non-empty string');
  if (relPath.length > 1024) fail('PATH_INVALID', 'path exceeds 1024 characters: ' + relPath.slice(0, 80));
  if (relPath.includes('\0') || relPath.includes('\\')) fail('PATH_INVALID', 'path contains a forbidden byte: ' + relPath);
  if (relPath.startsWith('/')) fail('PATH_ESCAPE', 'absolute paths are rejected: ' + relPath);
  const segments = relPath.split('/');
  for (const segment of segments) {
    if (segment === '' ) fail('PATH_INVALID', 'empty path segment: ' + relPath);
    if (segment === '.' || segment === '..') fail('PATH_ESCAPE', 'path traversal segment: ' + relPath);
    if (!SEGMENT_RE.test(segment)) fail('PATH_INVALID', 'unsupported path segment ' + JSON.stringify(segment) + ' in ' + relPath);
  }
  return segments;
}

function resolveRoot(root) {
  if (typeof root !== 'string' || root.length === 0) fail('ROOT_INVALID', 'root must be a non-empty string');
  let real;
  try { real = fs.realpathSync.native(root); }
  catch (error) { fail('ROOT_INVALID', 'root is not resolvable: ' + root + ' (' + error.code + ')'); }
  let stat;
  try { stat = fs.lstatSync(real); }
  catch (error) { fail('ROOT_INVALID', 'root vanished while resolving: ' + root); }
  if (!stat.isDirectory()) fail('ROOT_INVALID', 'root is not a directory: ' + root);
  return real;
}

// Classify obviously-invalid input before entering the canonical anchored
// reader. These checks provide stable public error codes; they do not grant
// authority. The worker below independently pins the root inode, walks every
// component and re-proves the complete public chain after reading.
function checkAncestors(realRoot, segments, relPath) {
  let current = realRoot;
  for (let i = 0; i < segments.length - 1; i++) {
    current = path.join(current, segments[i]);
    let stat;
    try { stat = fs.lstatSync(current); }
    catch (error) { fail('FILE_MISSING', 'path segment is missing: ' + relPath); }
    if (stat.isSymbolicLink()) fail('SYMLINK_REJECTED', 'symlink ancestor: ' + relPath);
    if (!stat.isDirectory()) fail('PATH_INVALID', 'non-directory ancestor: ' + relPath);
  }
  return path.join(realRoot, ...segments);
}

function readEntry(realRoot, relPath) {
  const segments = checkRelPath(relPath);
  const absolute = checkAncestors(realRoot, segments, relPath);
  let leaf;
  try { leaf = fs.lstatSync(absolute, { bigint: true }); }
  catch (error) {
    if (error.code === 'ENOENT') fail('FILE_MISSING', 'file is missing: ' + relPath);
    fail('FILE_UNREADABLE', 'stat failed for ' + relPath + ' (' + error.code + ')');
  }
  if (leaf.isSymbolicLink()) fail('SYMLINK_REJECTED', 'symlink is rejected: ' + relPath);
  if (!leaf.isFile()) fail('NOT_REGULAR', 'not a regular file: ' + relPath);
  if (leaf.nlink !== 1n) fail('HARDLINK_REJECTED', 'nlink must be 1: ' + relPath);
  if (leaf.size > BigInt(MAX_FILE_BYTES)) fail('FILE_TOO_LARGE', relPath + ' exceeds ' + MAX_FILE_BYTES + ' bytes');

  const directory = path.dirname(absolute);
  const guarded = fileGuards.boundedRegularFileUnder(realRoot, directory, absolute, MAX_FILE_BYTES);
  if (!guarded || !Buffer.isBuffer(guarded.bytes) || !guarded.stat ||
      !guarded.stat.isFile() || guarded.stat.isSymbolicLink() || guarded.stat.nlink !== '1' ||
      guarded.bytes.length !== guarded.stat.size) {
    fail('READ_RACE', relPath + ' or an ancestor changed while reading');
  }
  return { path: relPath, size: guarded.bytes.length, hash: sha256(guarded.bytes) };
}

function modelOf(entries) {
  const totalBytes = entries.reduce((sum, entry) => sum + entry.size, 0);
  return {
    version: VERSION,
    domain: DOMAIN,
    entries: entries.map((entry) => ({ path: entry.path, size: entry.size, hash: entry.hash })),
    entryCount: entries.length,
    totalBytes
  };
}

function snapshotHashOf(model) {
  return sha256(DOMAIN + '\0' + canonicalJson(model));
}

function freezeManifest(model) {
  const manifest = { ...model, snapshotHash: snapshotHashOf(model) };
  manifest.entries = Object.freeze(manifest.entries.map((entry) => Object.freeze({ ...entry })));
  return Object.freeze(manifest);
}

function captureSnapshot(options) {
  if (!options || typeof options !== 'object' || Array.isArray(options)) fail('OPTIONS_INVALID', 'captureSnapshot needs an options object');
  const { root, paths: relPaths } = options;
  if (!Array.isArray(relPaths)) fail('OPTIONS_INVALID', 'paths must be an array');
  if (relPaths.length > MAX_ENTRIES) fail('TOO_MANY_ENTRIES', relPaths.length + ' entries exceed ' + MAX_ENTRIES);
  const realRoot = resolveRoot(root);
  const seen = new Set();
  for (const relPath of relPaths) {
    checkRelPath(relPath);
    if (seen.has(relPath)) fail('DUPLICATE_PATH', 'duplicate path: ' + relPath);
    seen.add(relPath);
  }
  const sorted = [...relPaths].sort();
  const entries = [];
  let totalBytes = 0;
  for (const relPath of sorted) {
    const entry = readEntry(realRoot, relPath);
    totalBytes += entry.size;
    if (totalBytes > MAX_TOTAL_BYTES) fail('TOTAL_TOO_LARGE', 'snapshot exceeds ' + MAX_TOTAL_BYTES + ' bytes');
    entries.push(entry);
  }
  return freezeManifest(modelOf(entries));
}

function validateManifest(manifest) {
  if (!manifest || typeof manifest !== 'object' || Array.isArray(manifest)) fail('MANIFEST_INVALID', 'manifest must be an object');
  const keys = Object.keys(manifest).sort();
  if (keys.length !== MANIFEST_KEYS.length || keys.some((key, i) => key !== MANIFEST_KEYS[i])) {
    fail('MANIFEST_INVALID', 'manifest keys must be exactly ' + MANIFEST_KEYS.join(','));
  }
  if (manifest.version !== VERSION) fail('MANIFEST_INVALID', 'unsupported version: ' + manifest.version);
  if (manifest.domain !== DOMAIN) fail('MANIFEST_INVALID', 'unsupported domain: ' + manifest.domain);
  if (!Array.isArray(manifest.entries)) fail('MANIFEST_INVALID', 'entries must be an array');
  if (manifest.entries.length > MAX_ENTRIES) fail('MANIFEST_INVALID', 'entries exceed ' + MAX_ENTRIES);
  if (manifest.entryCount !== manifest.entries.length) fail('MANIFEST_INVALID', 'entryCount does not match entries');
  let totalBytes = 0;
  let previousPath = null;
  for (const entry of manifest.entries) {
    if (!entry || typeof entry !== 'object' || Array.isArray(entry)) fail('MANIFEST_INVALID', 'entry must be an object');
    const entryKeys = Object.keys(entry).sort();
    if (entryKeys.length !== ENTRY_KEYS.length || entryKeys.some((key, i) => key !== ENTRY_KEYS[i])) {
      fail('MANIFEST_INVALID', 'entry keys must be exactly ' + ENTRY_KEYS.join(','));
    }
    checkRelPath(entry.path);
    if (previousPath !== null && !(previousPath < entry.path)) fail('MANIFEST_INVALID', 'entries must be strictly sorted by path');
    previousPath = entry.path;
    if (!Number.isSafeInteger(entry.size) || entry.size < 0 || entry.size > MAX_FILE_BYTES) fail('MANIFEST_INVALID', 'entry size out of bounds: ' + entry.path);
    if (typeof entry.hash !== 'string' || !HASH_RE.test(entry.hash)) fail('MANIFEST_INVALID', 'entry hash grammar: ' + entry.path);
    totalBytes += entry.size;
  }
  if (manifest.totalBytes !== totalBytes) fail('MANIFEST_INVALID', 'totalBytes does not match entries');
  if (totalBytes > MAX_TOTAL_BYTES) fail('MANIFEST_INVALID', 'totalBytes exceeds ' + MAX_TOTAL_BYTES);
  const model = {
    version: manifest.version,
    domain: manifest.domain,
    entries: manifest.entries.map((entry) => ({ path: entry.path, size: entry.size, hash: entry.hash })),
    entryCount: manifest.entryCount,
    totalBytes: manifest.totalBytes
  };
  if (typeof manifest.snapshotHash !== 'string' || !HASH_RE.test(manifest.snapshotHash)) fail('MANIFEST_INVALID', 'snapshotHash grammar');
  if (snapshotHashOf(model) !== manifest.snapshotHash) fail('HASH_MISMATCH', 'snapshotHash does not match manifest content');
  return freezeManifest(model);
}

// Recapture every manifest path and report exact drift. Any read failure is
// drift (fail-closed), never a silent pass.
function verifySnapshot(manifest, options) {
  const valid = validateManifest(manifest);
  if (!options || typeof options !== 'object' || Array.isArray(options)) fail('OPTIONS_INVALID', 'verifySnapshot needs an options object');
  const realRoot = resolveRoot(options.root);
  const drifted = [];
  for (const entry of valid.entries) {
    let current;
    try { current = readEntry(realRoot, entry.path); }
    catch (error) {
      drifted.push({ path: entry.path, kind: error.code === 'FILE_MISSING' ? 'missing' : 'unreadable' });
      continue;
    }
    if (current.hash !== entry.hash || current.size !== entry.size) drifted.push({ path: entry.path, kind: 'changed' });
  }
  if (drifted.length > 0) return { ok: false, code: 'SNAPSHOT_DRIFT', drifted };
  return { ok: true, code: 'SNAPSHOT_CURRENT', drifted: [] };
}

module.exports = {
  VERSION,
  DOMAIN,
  MAX_FILE_BYTES,
  MAX_TOTAL_BYTES,
  MAX_ENTRIES,
  ContentSnapshotError,
  captureSnapshot,
  validateManifest,
  verifySnapshot
};
