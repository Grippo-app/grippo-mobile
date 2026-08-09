#!/usr/bin/env node

// Self-tests for the canonical byte-content snapshot primitive
// (orchestrator/tasks/content-snapshot.cjs, pipeline improvement 02).
// Byte-content only, fail-closed reads, frozen v1 hash domain.

import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import {
  linkSync, mkdirSync, mkdtempSync, renameSync, rmSync, symlinkSync, utimesSync, writeFileSync,
} from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const snapshot = require('../content-snapshot.cjs');

const roots = [];
const failures = [];
let checks = 0;

async function check(name, fn) {
  checks++;
  try {
    await fn();
    console.log(`PASS ${name}`);
  } catch (error) {
    failures.push({ name, error });
    console.error(`FAIL ${name}\n${error && error.stack || error}`);
  }
}

function fixture(files) {
  const root = mkdtempSync(join(tmpdir(), 'content-snapshot-'));
  roots.push(root);
  for (const [relPath, bytes] of Object.entries(files)) {
    const absolute = join(root, relPath);
    mkdirSync(join(absolute, '..'), { recursive: true });
    writeFileSync(absolute, bytes);
  }
  return root;
}

function rejects(fn, code) {
  try { fn(); }
  catch (error) {
    assert.equal(error.name, 'ContentSnapshotError', String(error && error.stack || error));
    assert.equal(error.code, code);
    return;
  }
  assert.fail(`expected ContentSnapshotError ${code}`);
}

const FILES = { 'a.txt': 'alpha\n', 'dir/b.bin': 'beta-bytes', 'dir/deep/c.kt': 'val c = 3\n' };
const PATHS = Object.keys(FILES);

await check('capture → validate → verify roundtrip is green and canonical', () => {
  const root = fixture(FILES);
  const manifest = snapshot.captureSnapshot({ root, paths: [...PATHS].reverse() });
  assert.equal(manifest.version, 1);
  assert.equal(manifest.domain, 'content-snapshot-v1');
  assert.deepEqual(manifest.entries.map((entry) => entry.path), [...PATHS].sort());
  assert.equal(manifest.entryCount, 3);
  assert.match(manifest.snapshotHash, /^sha256:[0-9a-f]{64}$/);
  assert.ok(Object.isFrozen(manifest) && Object.isFrozen(manifest.entries));
  const revalidated = snapshot.validateManifest(JSON.parse(JSON.stringify(manifest)));
  assert.equal(revalidated.snapshotHash, manifest.snapshotHash);
  const verdict = snapshot.verifySnapshot(manifest, { root });
  assert.deepEqual(verdict, { ok: true, code: 'SNAPSHOT_CURRENT', drifted: [] });
});

await check('hash is byte-content only: mtime touch keeps it, one byte breaks it', () => {
  const root = fixture(FILES);
  const before = snapshot.captureSnapshot({ root, paths: PATHS });
  utimesSync(join(root, 'a.txt'), new Date('2020-01-02T03:04:05Z'), new Date('2020-01-02T03:04:05Z'));
  const touched = snapshot.captureSnapshot({ root, paths: PATHS });
  assert.equal(touched.snapshotHash, before.snapshotHash, 'metadata-only change must not move the hash');
  writeFileSync(join(root, 'a.txt'), 'alphA\n');
  const changed = snapshot.captureSnapshot({ root, paths: PATHS });
  assert.notEqual(changed.snapshotHash, before.snapshotHash);
  const verdict = snapshot.verifySnapshot(before, { root });
  assert.equal(verdict.ok, false);
  assert.equal(verdict.code, 'SNAPSHOT_DRIFT');
  assert.deepEqual(verdict.drifted, [{ path: 'a.txt', kind: 'changed' }]);
});

await check('verify reports a missing file fail-closed', () => {
  const root = fixture(FILES);
  const manifest = snapshot.captureSnapshot({ root, paths: PATHS });
  rmSync(join(root, 'dir/b.bin'));
  const verdict = snapshot.verifySnapshot(manifest, { root });
  assert.equal(verdict.ok, false);
  assert.deepEqual(verdict.drifted, [{ path: 'dir/b.bin', kind: 'missing' }]);
});

await check('symlinks are rejected as leaf and as ancestor', () => {
  const root = fixture(FILES);
  symlinkSync(join(root, 'a.txt'), join(root, 'link.txt'));
  rejects(() => snapshot.captureSnapshot({ root, paths: ['link.txt'] }), 'SYMLINK_REJECTED');
  symlinkSync(join(root, 'dir'), join(root, 'dirlink'));
  rejects(() => snapshot.captureSnapshot({ root, paths: ['dirlink/b.bin'] }), 'SYMLINK_REJECTED');
});

await check('an ancestor swapped to an external symlink before open is rejected', () => {
  const root = fixture({ 'safe/probe.txt': 'inside-bytes' });
  const outside = fixture({ 'probe.txt': 'outside-bytes' });
  const realRoot = fs.realpathSync.native(root);
  const safe = join(realRoot, 'safe');
  const parked = join(realRoot, 'safe-parked');
  const originalLstat = fs.lstatSync;
  let swapped = false;
  fs.lstatSync = function lstatAndSwap(candidate, options) {
    const stat = originalLstat.call(fs, candidate, options);
    if (!swapped && candidate === safe) {
      swapped = true;
      renameSync(safe, parked);
      symlinkSync(outside, safe);
    }
    return stat;
  };
  try {
    rejects(() => snapshot.captureSnapshot({ root, paths: ['safe/probe.txt'] }), 'READ_RACE');
    assert.equal(swapped, true, 'the deterministic ancestor swap hook must run');
  } finally {
    fs.lstatSync = originalLstat;
  }
});

await check('hardlinks, directories and FIFOs are rejected', () => {
  const root = fixture(FILES);
  linkSync(join(root, 'a.txt'), join(root, 'hard.txt'));
  rejects(() => snapshot.captureSnapshot({ root, paths: ['a.txt'] }), 'HARDLINK_REJECTED');
  rejects(() => snapshot.captureSnapshot({ root, paths: ['dir'] }), 'NOT_REGULAR');
  execFileSync('mkfifo', [join(root, 'pipe.fifo')]);
  rejects(() => snapshot.captureSnapshot({ root, paths: ['pipe.fifo'] }), 'NOT_REGULAR');
});

await check('traversal, absolute, malformed and duplicate paths never reach the filesystem', () => {
  const root = fixture(FILES);
  rejects(() => snapshot.captureSnapshot({ root, paths: ['../escape.txt'] }), 'PATH_ESCAPE');
  rejects(() => snapshot.captureSnapshot({ root, paths: ['dir/../a.txt'] }), 'PATH_ESCAPE');
  rejects(() => snapshot.captureSnapshot({ root, paths: ['/etc/passwd'] }), 'PATH_ESCAPE');
  rejects(() => snapshot.captureSnapshot({ root, paths: ['dir//b.bin'] }), 'PATH_INVALID');
  rejects(() => snapshot.captureSnapshot({ root, paths: ['dir\\b.bin'] }), 'PATH_INVALID');
  rejects(() => snapshot.captureSnapshot({ root, paths: ['a.txt', 'a.txt'] }), 'DUPLICATE_PATH');
  rejects(() => snapshot.captureSnapshot({ root: join(root, 'missing'), paths: ['a.txt'] }), 'ROOT_INVALID');
  rejects(() => snapshot.captureSnapshot({ root, paths: ['missing.txt'] }), 'FILE_MISSING');
});

await check('bounds are enforced before hashing', () => {
  const root = fixture({ 'big.bin': Buffer.alloc(1024) });
  assert.ok(snapshot.MAX_FILE_BYTES > 0 && snapshot.MAX_TOTAL_BYTES >= snapshot.MAX_FILE_BYTES);
  rejects(() => snapshot.captureSnapshot({ root, paths: Array.from({ length: snapshot.MAX_ENTRIES + 1 }, (_, i) => `f${i}`) }), 'TOO_MANY_ENTRIES');
});

await check('validateManifest rejects every tamper and shape drift', () => {
  const root = fixture(FILES);
  const manifest = JSON.parse(JSON.stringify(snapshot.captureSnapshot({ root, paths: PATHS })));
  const tamper = (mutate, code) => {
    const copy = JSON.parse(JSON.stringify(manifest));
    mutate(copy);
    rejects(() => snapshot.validateManifest(copy), code);
  };
  tamper((m) => { m.version = 2; }, 'MANIFEST_INVALID');
  tamper((m) => { m.domain = 'task-state-snapshot-v1'; }, 'MANIFEST_INVALID');
  tamper((m) => { delete m.totalBytes; }, 'MANIFEST_INVALID');
  tamper((m) => { m.extra = true; }, 'MANIFEST_INVALID');
  tamper((m) => { m.entryCount = 2; }, 'MANIFEST_INVALID');
  tamper((m) => { m.totalBytes += 1; }, 'MANIFEST_INVALID');
  tamper((m) => { m.entries.reverse(); }, 'MANIFEST_INVALID');
  tamper((m) => { m.entries[0].hash = 'sha256:zz'; }, 'MANIFEST_INVALID');
  tamper((m) => { m.entries[0].extra = 1; }, 'MANIFEST_INVALID');
  tamper((m) => { m.entries[1].size += 1; }, 'MANIFEST_INVALID');
  tamper((m) => { m.entries[1].size += 1; m.totalBytes += 1; }, 'HASH_MISMATCH');
  tamper((m) => { m.entries[0].hash = 'sha256:' + 'a'.repeat(64); }, 'HASH_MISMATCH');
  tamper((m) => { m.snapshotHash = 'sha256:' + 'b'.repeat(64); }, 'HASH_MISMATCH');
});

await check('aggregate hash lives in its own literal domain and the formula is frozen', () => {
  const root = fixture(FILES);
  const manifest = snapshot.captureSnapshot({ root, paths: PATHS });
  const model = {
    version: manifest.version,
    domain: manifest.domain,
    entries: manifest.entries.map((entry) => ({ path: entry.path, size: entry.size, hash: entry.hash })),
    entryCount: manifest.entryCount,
    totalBytes: manifest.totalBytes
  };
  const canonical = (function canonicalJson(value) {
    if (value === null || typeof value !== 'object') return JSON.stringify(value);
    if (Array.isArray(value)) return '[' + value.map(canonicalJson).join(',') + ']';
    return '{' + Object.keys(value).sort().map((key) => JSON.stringify(key) + ':' + canonicalJson(value[key])).join(',') + '}';
  })(model);
  const expected = 'sha256:' + createHash('sha256').update('content-snapshot-v1\0' + canonical).digest('hex');
  assert.equal(manifest.snapshotHash, expected, 'v1 hash formula is frozen');
  const undomained = 'sha256:' + createHash('sha256').update(canonical).digest('hex');
  assert.notEqual(manifest.snapshotHash, undomained, 'domain prefix must separate the namespace');
  const foreign = 'sha256:' + createHash('sha256').update('task-state-snapshot-v1\0' + canonical).digest('hex');
  assert.notEqual(manifest.snapshotHash, foreign, 'never comparable with another domain');
});

await check('the frozen v1 API exposes no metadata-only or legacy entry point', () => {
  assert.deepEqual(Object.keys(snapshot).sort(), [
    'ContentSnapshotError', 'DOMAIN', 'MAX_ENTRIES', 'MAX_FILE_BYTES',
    'MAX_TOTAL_BYTES', 'VERSION', 'captureSnapshot', 'validateManifest', 'verifySnapshot'
  ]);
  assert.equal(snapshot.VERSION, 1);
  assert.equal(snapshot.DOMAIN, 'content-snapshot-v1');
});

for (const root of roots) rmSync(root, { recursive: true, force: true });

if (failures.length > 0) {
  console.error(`content-snapshot: ${failures.length}/${checks} checks failed`);
  process.exit(1);
}
console.log(`content-snapshot: ${checks} checks passed`);
