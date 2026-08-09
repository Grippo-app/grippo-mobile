#!/usr/bin/env node

// ---------------------------------------------------------------------------
// Deterministic test certification executor (improvement 05, Phase 3).
//
// The security/durability boundary between "a builder claims tests ran" and
// "the orchestrator proved it". Everything here is fail-closed:
//   - argv is an allowlist: exact Gradle task paths validated against the
//     caller-supplied allowlist (capability inventory / root aggregates) plus
//     a fixed flag set — task text can NEVER become shell (no shell:true, no
//     string interpolation, args array only);
//   - the child runs in its own detached process group; timeout escalates
//     SIGTERM → SIGKILL on the whole group and an alive group after exit is a
//     typed violation, never ignored;
//   - env is a fixed allowlist; the receipt carries a fingerprint of hashed
//     values, never values. Output is redacted BEFORE persistence (parent-env
//     secret values are masked) and bounded;
//   - reports are ingested only from sealed staging copies: regular file,
//     nlink=1, bounded size, hashed before and after the copy (TOCTOU);
//   - receipts are immutable and content-addressed under the certification
//     root: started first, terminal after the group is closed;
//   - zero discovered tests, NO-SOURCE with required tests, all-skipped,
//     cache substitution on the direct tier — typed violations via
//     task-test-receipt-contract.evaluateCommandReceipt.
// ---------------------------------------------------------------------------

import { spawn } from 'node:child_process';
import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import { StringDecoder } from 'node:string_decoder';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const receiptContract = require('./task-test-receipt-contract.cjs');
const registry = require('./task-receipt-registry.cjs');
const fileGuards = require('../site/server/file-guards');

const HERE = path.dirname(new URL(import.meta.url).pathname);
const TASK_PATH_RE = /^:[A-Za-z0-9._:-]*[A-Za-z0-9]$/;
const ENV_ALLOWLIST = Object.freeze([
  'ANDROID_HOME', 'ANDROID_SDK_ROOT', 'GRADLE_USER_HOME', 'HOME', 'JAVA_HOME',
  'LANG', 'LC_ALL', 'PATH', 'SHELL', 'TMPDIR', 'USER'
]);
const SECRET_ENV_RE = /(TOKEN|SECRET|PASSWORD|CREDENTIAL|API_?KEY|AUTH)/i;
const FIXED_FLAGS = Object.freeze(['--no-daemon', '--stacktrace']);
const DIRECT_FLAGS = Object.freeze(['--rerun-tasks', '--no-build-cache']);
const OUTPUT_CAP_BYTES = 4 * 1024 * 1024;
const REPORT_MAX_BYTES = 64 * 1024 * 1024;
const REPORT_MAX_COUNT = 256;
// Receipt bounds. Identities are evidence, never a sample: a run whose reports
// exceed the receipt bound is refused with a typed bound error instead of
// sealing a receipt whose count no longer matches its concrete identities.
const MAX_TEST_IDENTITIES = 4096;
// Gradle task announcements are parsed from their own bounded buffer so a long
// build cannot push the disposition evidence past the log cap.
const MAX_TASK_LINES = 4096;
const TASK_LINE_MARKER = '> Task :';
const ORDINAL_RE = /^[0-9]{3}$/;
const REPORT_PARSERS = Object.freeze([
  'junit-xml', 'kotlin-native-xml', 'android-connected-xml', 'roborazzi-report'
]);
const EXECUTION_TIERS = Object.freeze([
  'affected-closure', 'builder-feedback', 'certification-direct', 'full-suite', 'owner-module', 'platform-lanes'
]);

class TestCertificationError extends Error {
  constructor(code, message) {
    super(code + ': ' + message);
    this.name = 'TestCertificationError';
    this.code = code;
  }
}

function fail(code, message) { throw new TestCertificationError(code, message); }

function sha256(value) {
  return 'sha256:' + crypto.createHash('sha256').update(value).digest('hex');
}

function nowIso() { return new Date().toISOString(); }

// ---------------------------------------------------------------------------
// Sanitized environment + fingerprint (names in, hashed values out).
// ---------------------------------------------------------------------------

export function sanitizedEnv(parentEnv = process.env) {
  const env = {};
  for (const key of ENV_ALLOWLIST) {
    if (typeof parentEnv[key] === 'string') env[key] = parentEnv[key];
  }
  return env;
}

export function envFingerprintOf(env) {
  const model = {};
  for (const key of Object.keys(env).sort()) model[key] = sha256(env[key]);
  return sha256('test-env-fingerprint\0' + receiptContract.canonicalJson(model));
}

// Values of secret-looking parent env vars are masked in any captured output
// even though they are never passed to the child — defense in depth against
// a build script that read them from disk.
export function redactOutput(text, parentEnv = process.env) {
  let redacted = text;
  for (const [key, value] of Object.entries(parentEnv)) {
    if (!SECRET_ENV_RE.test(key)) continue;
    if (typeof value !== 'string' || value.length < 6) continue;
    redacted = redacted.split(value).join('[REDACTED:' + key + ']');
  }
  return redacted;
}

// ---------------------------------------------------------------------------
// No-clobber content-addressed persistence.
// ---------------------------------------------------------------------------

function relativeSegments(root, target, code) {
  const relative = path.relative(root, target);
  if (!relative || relative === '..' || relative.startsWith('..' + path.sep) || path.isAbsolute(relative)) {
    fail(code, 'path escapes the owning root: ' + target);
  }
  return relative.split(path.sep);
}

function ensureRealDirectoryUnder(realRoot, target, code) {
  const resolved = path.resolve(target);
  relativeSegments(realRoot, resolved, code);
  const guarded = fileGuards.realDirectoryUnder(realRoot, resolved, { create: true, mode: 0o700 });
  if (!guarded || !guarded.exists) fail(code, 'directory chain is unsafe or raced: ' + resolved);
  return guarded.path;
}

function canonicalProductRoot(productRoot) {
  const resolved = path.resolve(productRoot);
  let stat;
  try { stat = fs.lstatSync(resolved); }
  catch (error) { fail('ROOT_INVALID', 'product root unavailable (' + error.code + ')'); }
  if (stat.isSymbolicLink() || !stat.isDirectory()) fail('ROOT_INVALID', 'product root must be a real directory');
  const real = fs.realpathSync.native(resolved);
  return real;
}

function canonicalChildPath(inputRoot, canonicalRoot, child, code) {
  const input = path.resolve(inputRoot);
  const target = path.resolve(child);
  const relative = path.relative(input, target);
  if (!relative || relative === '..' || relative.startsWith('..' + path.sep) || path.isAbsolute(relative)) {
    fail(code, 'path escapes the product root: ' + target);
  }
  return path.join(canonicalRoot, relative);
}

function runRoot(certificationRoot, taskStem, runId, productRoot) {
  if (!/^TASK_[1-9][0-9]*_[A-Za-z0-9_]+$/.test(taskStem)) fail('IDENTITY_INVALID', 'taskStem grammar');
  if (!/^run-[A-Za-z0-9][A-Za-z0-9-]{0,79}$/.test(runId)) fail('IDENTITY_INVALID', 'runId grammar');
  const certification = ensureRealDirectoryUnder(productRoot, path.resolve(certificationRoot), 'CERTIFICATION_PATH_UNSAFE');
  return ensureRealDirectoryUnder(certification, path.join(certification, taskStem, runId), 'CERTIFICATION_PATH_UNSAFE');
}

function writeSealed(filePath, bytes, owningRoot) {
  const parent = ensureRealDirectoryUnder(owningRoot, path.dirname(filePath), 'CERTIFICATION_PATH_UNSAFE');
  let fd;
  try { fd = fs.openSync(filePath, fs.constants.O_WRONLY | fs.constants.O_CREAT | fs.constants.O_EXCL | fs.constants.O_NOFOLLOW, 0o600); }
  catch (error) { fail('NO_CLOBBER', filePath + ' already exists (' + error.code + ')'); }
  try {
    fs.writeFileSync(fd, bytes);
    fs.fsyncSync(fd);
    const stat = fs.fstatSync(fd, { bigint: true });
    if (!stat.isFile() || stat.nlink !== 1n) fail('CERTIFICATION_PATH_UNSAFE', 'sealed output is not a single-link regular file');
  } finally { fs.closeSync(fd); }
  const real = fs.realpathSync.native(filePath);
  relativeSegments(owningRoot, real, 'CERTIFICATION_PATH_UNSAFE');
  if (path.dirname(real) !== parent) fail('CERTIFICATION_PATH_UNSAFE', 'sealed output parent changed during publication');
}

function persistReceipt(root, family, ordinal, receipt, certificationRoot) {
  const hex = receipt.receiptHash.slice('sha256:'.length);
  const filePath = path.join(root, family, ordinal + '-' + receipt.stage + '-' + hex + '.json');
  writeSealed(filePath, JSON.stringify(receipt, null, 2) + '\n', certificationRoot);
  return filePath;
}

// ---------------------------------------------------------------------------
// Sealed report ingestion (regular file, nlink=1, bounded, TOCTOU re-hash).
// ---------------------------------------------------------------------------

function checkRelativeReportDir(relDir) {
  if (typeof relDir !== 'string' || relDir.length === 0 || relDir.length > 1024 ||
      relDir.includes('\0') || relDir.includes('\\') || path.isAbsolute(relDir)) {
    fail('REPORT_PATH_INVALID', 'report dir must be a bounded relative path: ' + String(relDir));
  }
  const segments = relDir.split('/');
  if (segments.some((segment) => !segment || segment === '.' || segment === '..')) {
    fail('REPORT_PATH_INVALID', 'report dir contains an unsafe segment: ' + relDir);
  }
  return segments;
}

function reportDirectory(productRoot, relDir, { allowMissing }) {
  const segments = checkRelativeReportDir(relDir);
  const current = path.join(productRoot, ...segments);
  const guarded = fileGuards.realDirectoryUnder(productRoot, current, { allowMissing });
  if (!guarded) fail('REPORT_UNSAFE', 'report directory is unsafe or raced: ' + relDir);
  if (!guarded.exists) return null;
  return guarded.path;
}

function generationOf(stat) {
  return [stat.dev, stat.ino, stat.size, stat.mtimeNs, stat.ctimeNs, stat.nlink].map(String).join(':');
}

function validateReportInputs(reportInputs) {
  if (!Array.isArray(reportInputs) || reportInputs.length > 64) {
    fail('REPORT_INPUT_INVALID', 'reportInputs must be a bounded array');
  }
  const seen = new Set();
  return reportInputs.map((input) => {
    if (!input || typeof input !== 'object' || Array.isArray(input) ||
        Object.keys(input).sort().join(',') !== 'parser,path') {
      fail('REPORT_INPUT_INVALID', 'each report input must contain exactly parser,path');
    }
    checkRelativeReportDir(input.path);
    if (!REPORT_PARSERS.includes(input.parser)) {
      fail('REPORT_INPUT_INVALID', 'unknown report parser: ' + String(input.parser));
    }
    if (seen.has(input.path)) fail('REPORT_INPUT_INVALID', 'duplicate report input path: ' + input.path);
    seen.add(input.path);
    return Object.freeze({ path: input.path, parser: input.parser });
  });
}

function captureReportGenerations({ productRoot, reportInputs }) {
  const generations = new Map();
  let count = 0;
  for (const { path: relDir } of reportInputs) {
    const absDir = reportDirectory(productRoot, relDir, { allowMissing: true });
    if (!absDir) continue;
    const listing = fileGuards.boundedDirectoryNamesUnder(productRoot, absDir, REPORT_MAX_COUNT + 1);
    if (!listing.ok || listing.exists === false) fail('REPORT_UNSAFE', 'cannot enumerate report directory safely');
    const entries = listing.names.filter((name) => name.endsWith('.xml')).sort();
    for (const name of entries) {
      if (++count > REPORT_MAX_COUNT) fail('REPORT_BOUNDS', 'too many pre-existing report files');
      const file = path.join(absDir, name);
      const stat = fileGuards.statRegularFileUnder(productRoot, absDir, file);
      if (!stat || stat.nlink !== '1') {
        fail('REPORT_UNSAFE', 'unsafe pre-existing report: ' + name);
      }
      if (stat.size > REPORT_MAX_BYTES) fail('REPORT_BOUNDS', 'pre-existing report exceeds bounded size: ' + name);
      generations.set(path.join(relDir, name).replaceAll('\\', '/'), generationOf(stat));
    }
  }
  return generations;
}

function ingestReports({ productRoot, reportInputs, sealDir, baselineReports, certificationRoot }) {
  const artifacts = [];
  const identities = [];
  const totals = { discovered: 0, executed: 0, passed: 0, failed: 0, skipped: 0, aborted: 0 };
  let ordinal = 0;
  for (const { path: relDir, parser } of reportInputs) {
    const absDir = reportDirectory(productRoot, relDir, { allowMissing: true });
    if (!absDir) continue;
    const listing = fileGuards.boundedDirectoryNamesUnder(productRoot, absDir, REPORT_MAX_COUNT + 1);
    if (!listing.ok || listing.exists === false) fail('REPORT_UNSAFE', 'cannot enumerate report directory safely');
    const entries = listing.names.filter((name) => name.endsWith('.xml')).sort();
    for (const name of entries) {
      if (artifacts.length >= REPORT_MAX_COUNT) fail('REPORT_BOUNDS', 'too many report files');
      const absFile = path.join(absDir, name);
      const guarded = fileGuards.boundedRegularFileUnder(productRoot, absDir, absFile, REPORT_MAX_BYTES);
      if (!guarded || guarded.stat.nlink !== '1') fail('REPORT_UNSAFE', 'report or an ancestor changed while reading: ' + name);
      const bytes = guarded.bytes;
      const relFile = path.join(relDir, name).replaceAll('\\', '/');
      if (baselineReports.get(relFile) === generationOf(guarded.stat)) {
        fail('REPORT_STALE', 'report was not produced or replaced by this certification run: ' + relFile);
      }
      const hash = sha256(bytes);
      const sealedName = String(ordinal).padStart(3, '0') + '-' + name;
      const sealedPath = path.join(sealDir, sealedName);
      writeSealed(sealedPath, bytes, certificationRoot);
      const sealedRead = fileGuards.boundedRegularFileUnder(certificationRoot, sealDir, sealedPath, REPORT_MAX_BYTES);
      if (!sealedRead) fail('REPORT_UNSAFE', 'sealed report or an ancestor changed after publication: ' + name);
      const sealedBytes = sealedRead.bytes;
      if (sha256(sealedBytes) !== hash) fail('REPORT_UNSAFE', 'report changed during sealing: ' + name);
      const parsed = parseReport(sealedBytes.toString('utf8'), parser);
      if (parsed.tests !== parsed.identities.length ||
          parsed.failures + parsed.errors + parsed.skipped > parsed.tests) {
        fail('REPORT_INVALID', 'JUnit counts do not match the concrete testcase identities: ' + name);
      }
      totals.discovered += parsed.tests;
      totals.executed += parsed.tests - parsed.skipped;
      totals.failed += parsed.failures + parsed.errors;
      totals.skipped += parsed.skipped;
      totals.passed += parsed.tests - parsed.skipped - parsed.failures - parsed.errors;
      identities.push(...parsed.identities);
      if (identities.length > MAX_TEST_IDENTITIES) {
        fail('REPORT_BOUNDS', 'discovered test identities exceed the receipt bound of ' + MAX_TEST_IDENTITIES);
      }
      artifacts.push({
        path: path.join(relDir, name).replaceAll('\\', '/'),
        bytes: bytes.length,
        hash,
        parser
      });
      ordinal++;
    }
  }
  return { artifacts, identities, totals };
}

export function parseJUnitXml(xml) {
  const suite = /<testsuite\b[^>]*/.exec(xml);
  if (!suite) fail('REPORT_INVALID', 'JUnit report has no testsuite');
  const attr = (name) => {
    // Anchored on an attribute boundary: an unanchored name would read
    // `data_failures="0"` as `failures`.
    const match = suite && new RegExp('(?:^|\\s)' + name + '="([0-9]+)"').exec(suite[0]);
    if (!match) fail('REPORT_INVALID', 'JUnit testsuite is missing ' + name);
    const value = Number(match[1]);
    if (!Number.isSafeInteger(value) || value > 1000000) fail('REPORT_INVALID', 'JUnit ' + name + ' is out of bounds');
    return value;
  };
  const identities = [];
  for (const testcase of xml.matchAll(/<testcase\b[^>]*>/g)) {
    const classMatch = /\bclassname="([^"]+)"/.exec(testcase[0]);
    const nameMatch = /\bname="([^"]+)"/.exec(testcase[0]);
    if (!classMatch || !nameMatch || classMatch[1].length > 240 || nameMatch[1].length > 240) {
      fail('REPORT_INVALID', 'JUnit testcase identity is missing or out of bounds');
    }
    identities.push(classMatch[1] + '.' + nameMatch[1]);
  }
  const parsed = {
    tests: attr('tests'),
    failures: attr('failures'),
    errors: attr('errors'),
    skipped: attr('skipped'),
    identities
  };
  if (parsed.tests !== parsed.identities.length ||
      parsed.failures + parsed.errors + parsed.skipped > parsed.tests) {
    fail('REPORT_INVALID', 'JUnit aggregate counts do not match concrete testcase identities');
  }
  return parsed;
}

function parseKotlinNativeXml(xml) { return parseJUnitXml(xml); }
function parseAndroidConnectedXml(xml) { return parseJUnitXml(xml); }
function parseRoborazziReport(xml) { return parseJUnitXml(xml); }

export function parseReport(text, parser) {
  if (parser === 'junit-xml') return parseJUnitXml(text);
  if (parser === 'kotlin-native-xml') return parseKotlinNativeXml(text);
  if (parser === 'android-connected-xml') return parseAndroidConnectedXml(text);
  if (parser === 'roborazzi-report') return parseRoborazziReport(text);
  fail('REPORT_INPUT_INVALID', 'unknown report parser: ' + String(parser));
}

function dispositionsFromOutput(taskLines, taskPaths) {
  const output = taskLines.join('\n');
  const leafResults = [];
  for (const taskPath of taskPaths) {
    const lineRe = new RegExp('> Task ' + taskPath.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '( [A-Z-]+)?$', 'm');
    const match = lineRe.exec(output);
    const marker = match && match[1] ? match[1].trim() : null;
    let disposition = 'executed';
    if (marker === 'NO-SOURCE') disposition = 'no-source';
    else if (marker === 'FROM-CACHE') disposition = 'from-cache';
    else if (marker === 'UP-TO-DATE') disposition = 'up-to-date';
    else if (marker === 'SKIPPED') disposition = 'skipped';
    leafResults.push({ taskPath, disposition });
  }
  return leafResults;
}

// ---------------------------------------------------------------------------
// The one certification invocation.
// ---------------------------------------------------------------------------

export async function certifyCommand(options) {
  const {
    certificationRoot, productRoot, gradlewPath, taskPaths, allowedTaskPaths,
    suite, lane, identity, hashes, toolchain, reportInputs = [],
    timeoutMs = 30 * 60 * 1000, tier = 'certification-direct', executionRootKind = 'shared-serial',
    continueOnFailure = false, ordinal = '000', testsRequired = true,
    parentEnv = process.env
  } = options;
  if (!EXECUTION_TIERS.includes(tier)) fail('EXECUTION_TIER_INVALID', 'unknown execution tier: ' + String(tier));
  if (!ORDINAL_RE.test(String(ordinal))) fail('IDENTITY_INVALID', 'ordinal grammar');
  const validatedReportInputs = validateReportInputs(reportInputs);
  if (!Array.isArray(taskPaths) || taskPaths.length === 0) fail('ALLOWLIST_VIOLATION', 'no task paths');
  if (!Array.isArray(allowedTaskPaths) || allowedTaskPaths.length === 0) fail('ALLOWLIST_VIOLATION', 'an explicit allowlist is required');
  for (const taskPath of taskPaths) {
    if (!TASK_PATH_RE.test(taskPath)) fail('ALLOWLIST_VIOLATION', 'task path grammar: ' + taskPath);
    if (!allowedTaskPaths.includes(taskPath)) fail('ALLOWLIST_VIOLATION', 'task path outside the allowlist: ' + taskPath);
  }
  const canonicalRoot = canonicalProductRoot(productRoot);
  const expectedGradlew = path.join(canonicalRoot, 'gradlew');
  const gradlew = path.resolve(gradlewPath || expectedGradlew);
  if (gradlew !== expectedGradlew) fail('ALLOWLIST_VIOLATION', 'gradlew must be the product-root wrapper');
  const gradlewStat = fs.lstatSync(gradlew, { bigint: true });
  if (!gradlewStat.isFile() || gradlewStat.nlink !== 1n || (gradlewStat.mode & 0o111n) === 0n) {
    fail('ALLOWLIST_VIOLATION', 'gradlew is not an executable single-link regular file');
  }

  const certificationTarget = canonicalChildPath(productRoot, canonicalRoot, certificationRoot, 'CERTIFICATION_PATH_UNSAFE');
  const root = runRoot(certificationTarget, identity.taskStem, identity.runId, canonicalRoot);
  const canonicalCertificationRoot = fs.realpathSync.native(certificationTarget);
  const baselineReports = captureReportGenerations({ productRoot: canonicalRoot, reportInputs: validatedReportInputs });
  const env = sanitizedEnv(parentEnv);
  const envFingerprint = envFingerprintOf(env);
  const args = [...taskPaths, ...FIXED_FLAGS, ...(tier === 'certification-direct' ? DIRECT_FLAGS : [])];
  if (continueOnFailure) args.push('--continue');

  const base = {
    version: 1,
    kind: 'test-command',
    stage: 'started',
    taskStem: identity.taskStem,
    runId: identity.runId,
    sessionId: identity.sessionId,
    lockStage: identity.lockStage,
    taskInputHash: hashes.taskInputHash,
    sourceSnapshotHash: hashes.sourceSnapshotHash,
    impactHash: hashes.impactHash,
    policyHash: hashes.policyHash,
    suite,
    tier,
    lane,
    taskPaths: [...taskPaths],
    cwd: canonicalRoot,
    envFingerprint,
    toolchain,
    startedAt: nowIso(),
    startedReceiptHash: null,
    endedAt: null,
    durationMs: null,
    exitCode: null,
    signal: null,
    timedOut: false,
    disposition: 'pending',
    counts: { discovered: 0, executed: 0, passed: 0, failed: 0, skipped: 0, aborted: 0 },
    leafResults: [],
    discoveredTestIdentities: [],
    reportArtifacts: [],
    outputDigest: { bytes: 0, hash: sha256(''), redacted: true },
    retryHistory: [],
    pid: 0,
    processGroup: 0,
    executionRootKind,
    receiptHash: 'sha256:' + '0'.repeat(64)
  };

  const startedAtMs = Date.now();
  const child = spawn(gradlew, args, {
    cwd: base.cwd,
    env,
    detached: true,
    stdio: ['ignore', 'pipe', 'pipe']
  });
  await new Promise((resolve, reject) => {
    child.once('spawn', resolve);
    child.once('error', (error) => reject(new TestCertificationError(
      'EXECUTION_START_FAILED', 'wrapper spawn failed (' + (error.code || 'UNKNOWN') + ')')));
  });
  base.pid = child.pid;
  base.processGroup = child.pid;
  base.receiptHash = receiptContract.receiptHashOf(base);
  // A throw between spawn and the started receipt must not abandon a detached
  // build: nothing downstream would ever own that process group again.
  let started;
  try {
    started = receiptContract.validateCommandReceipt(base);
    persistReceipt(root, 'commands', ordinal, started, canonicalCertificationRoot);
  } catch (error) {
    try { process.kill(-child.pid, 'SIGKILL'); } catch (killError) {}
    throw error;
  }

  const outputChunks = [];
  let outputByteLength = 0;
  let truncated = false;
  const taskLines = [];
  // One decoder and one partial-line remainder PER stream: stdout and stderr
  // interleave, and a shared remainder would splice one stream's fragment onto
  // the other's line and lose the task announcement it carried.
  const captureFor = () => {
    const decoder = new StringDecoder('utf8');
    let lineRemainder = '';
    return (chunk) => {
      const bytes = Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk);
      const remaining = OUTPUT_CAP_BYTES - outputByteLength;
      if (remaining > 0) {
        const kept = bytes.subarray(0, remaining);
        outputChunks.push(kept);
        outputByteLength += kept.length;
      }
      if (bytes.length > remaining) truncated = true;
      if (taskLines.length >= MAX_TASK_LINES) return;
      // Split on \r as well: a progress update leaves the announcement mid
      // buffer, and a preceding partial write (`Configure project :lib`) would
      // otherwise glue itself onto the task line and hide the disposition.
      const parts = (lineRemainder + decoder.write(bytes)).split(/\r\n|[\n\r]/);
      lineRemainder = parts.pop().slice(-4096);
      for (const line of parts) {
        if (taskLines.length >= MAX_TASK_LINES) break;
        const marker = line.indexOf(TASK_LINE_MARKER);
        if (marker >= 0) taskLines.push(line.slice(marker));
      }
    };
  };
  child.stdout.on('data', captureFor());
  child.stderr.on('data', captureFor());

  let timedOut = false;
  let escalation = null;
  const timer = setTimeout(() => {
    timedOut = true;
    try { process.kill(-child.pid, 'SIGTERM'); } catch (error) {}
    escalation = setTimeout(() => { try { process.kill(-child.pid, 'SIGKILL'); } catch (error) {} }, 5000);
    escalation.unref();
  }, timeoutMs);

  const { exitCode, signal } = await new Promise((resolve) => {
    child.on('close', (code, sig) => resolve({ exitCode: code, signal: sig }));
  });
  clearTimeout(timer);
  // The escalation timer outlives the child otherwise, and this pid can be
  // recycled into the next certification run's own process group.
  if (escalation) clearTimeout(escalation);

  // Orphan sweep: the whole group must be dead before the terminal receipt.
  // Only ESRCH proves death — EPERM means alive but unsignalable.
  let groupAlive = false;
  try { process.kill(-child.pid, 0); groupAlive = true; }
  catch (error) { if (error.code !== 'ESRCH') groupAlive = true; }
  if (groupAlive) {
    try { process.kill(-child.pid, 'SIGKILL'); } catch (error) {}
    await new Promise((resolve) => setTimeout(resolve, 200));
    try { process.kill(-child.pid, 0); fail('ORPHAN_PROCESS', 'child process group survived the kill escalation'); }
    catch (error) {
      if (error instanceof TestCertificationError) throw error;
      if (error.code !== 'ESRCH') fail('ORPHAN_PROCESS', 'child process group could not be proven dead (' + error.code + ')');
    }
  }

  const output = Buffer.concat(outputChunks, outputByteLength).toString('utf8');
  const redacted = redactOutput(truncated ? output + '\n[TRUNCATED]' : output, parentEnv);
  let outputBytes = Buffer.from(redacted, 'utf8');
  if (outputBytes.length > OUTPUT_CAP_BYTES) {
    const marker = Buffer.from('\n[TRUNCATED-AFTER-REDACTION]', 'utf8');
    outputBytes = Buffer.concat([outputBytes.subarray(0, OUTPUT_CAP_BYTES - marker.length), marker]);
  }
  writeSealed(path.join(root, 'reports', ordinal + '-output.log'), outputBytes, canonicalCertificationRoot);

  const sealDir = path.join(root, 'reports', ordinal + '-xml');
  const { artifacts, identities, totals } = ingestReports({
    productRoot: canonicalRoot, reportInputs: validatedReportInputs, sealDir, baselineReports,
    certificationRoot: canonicalCertificationRoot
  });
  const leafDispositions = dispositionsFromOutput(taskLines, taskPaths);

  const terminal = {
    ...base,
    stage: 'terminal',
    startedReceiptHash: started.receiptHash,
    endedAt: nowIso(),
    durationMs: Math.max(0, Date.now() - startedAtMs),
    exitCode: exitCode === null ? null : exitCode,
    signal: signal || null,
    timedOut,
    disposition: timedOut ? 'aborted'
      : leafDispositions.every((leaf) => leaf.disposition === 'no-source') ? 'no-source'
      : leafDispositions.every((leaf) => leaf.disposition === 'up-to-date') ? 'up-to-date'
      : leafDispositions.every((leaf) => leaf.disposition === 'from-cache') ? 'from-cache'
      : 'executed',
    counts: { ...totals, aborted: timedOut ? 1 : 0 },
    leafResults: leafDispositions.map((leaf) => ({
      taskPath: leaf.taskPath,
      outcome: timedOut ? 'aborted'
        : leaf.disposition === 'no-source' ? 'no-source'
        : (exitCode === 0 ? 'passed' : (totals.failed > 0 ? 'failed' : 'failed')),
      disposition: leaf.disposition
    })),
    discoveredTestIdentities: identities,
    reportArtifacts: artifacts,
    outputDigest: { bytes: outputBytes.length, hash: sha256(outputBytes), redacted: true },
    receiptHash: 'sha256:' + '0'.repeat(64)
  };
  terminal.receiptHash = receiptContract.receiptHashOf(terminal);
  const sealed = receiptContract.validateCommandReceipt(terminal);
  persistReceipt(root, 'commands', ordinal, sealed, canonicalCertificationRoot);

  const evaluation = receiptContract.evaluateCommandReceipt(sealed, { testsRequired });
  return { receipt: sealed, startedReceipt: started, evaluation, receiptId: registry.receiptIdOf('test-command', sealed.receiptHash) };
}

// ---------------------------------------------------------------------------
// Structural N/A gate producer. The request contains only a policy gate id;
// executable/tool/argv are selected here from this closed table and can never
// be supplied by task text or builder output.
// ---------------------------------------------------------------------------

function structuralInvocation(gateId, canonicalRoot, taskStem) {
  if (gateId === 'bootstrap-foundation-fixture') return {
    tool: 'node orchestrator/figma/tests/general-test-toolchain.test.mjs',
    executable: process.execPath,
    args: [path.join(canonicalRoot, 'orchestrator/figma/tests/general-test-toolchain.test.mjs')]
  };
  if (gateId === 'docs-contract-gate') return {
    tool: 'python3 orchestrator/skills/checks/check_self_contained_content.py',
    executable: 'python3', args: [path.join(canonicalRoot, 'orchestrator/skills/checks/check_self_contained_content.py')]
  };
  if (gateId === 'link-integrity-gate') return {
    tool: 'python3 orchestrator/skills/checks/check_links.py',
    executable: 'python3', args: [path.join(canonicalRoot, 'orchestrator/skills/checks/check_links.py')]
  };
  if (gateId === 'task-shape-gate') return {
    tool: 'node orchestrator/tasks/validate-task-state.mjs',
    executable: process.execPath,
    args: [path.join(canonicalRoot, 'orchestrator/tasks/validate-task-state.mjs'),
      '--stem', taskStem, '--expect', 'todo', '--quiet', '--caller', 'test-certification']
  };
  fail('STRUCTURAL_GATE_INVALID', 'gate id has no trusted producer: ' + String(gateId));
}

export async function certifyStructuralGate(options) {
  const {
    certificationRoot, productRoot, gateId, identity, hashes,
    timeoutMs = 5 * 60 * 1000, ordinal = '000', parentEnv = process.env
  } = options;
  if (!ORDINAL_RE.test(String(ordinal))) fail('IDENTITY_INVALID', 'ordinal grammar');
  const canonicalRoot = canonicalProductRoot(productRoot);
  const certificationTarget = canonicalChildPath(productRoot, canonicalRoot, certificationRoot, 'CERTIFICATION_PATH_UNSAFE');
  const root = runRoot(certificationTarget, identity.taskStem, identity.runId, canonicalRoot);
  const canonicalCertificationRoot = fs.realpathSync.native(certificationTarget);
  const invocation = structuralInvocation(gateId, canonicalRoot, identity.taskStem);
  const env = sanitizedEnv(parentEnv);
  const startedAtMs = Date.now();
  const base = {
    version: 1,
    kind: 'test-structural-gate',
    stage: 'started',
    taskStem: identity.taskStem,
    runId: identity.runId,
    sessionId: identity.sessionId,
    lockStage: identity.lockStage,
    taskInputHash: hashes.taskInputHash,
    sourceSnapshotHash: hashes.sourceSnapshotHash,
    policyHash: hashes.policyHash,
    gateId,
    executionMode: 'external',
    tool: invocation.tool,
    validatorCodeHash: null,
    pid: null,
    processGroup: null,
    startedAt: nowIso(),
    startedReceiptHash: null,
    endedAt: null,
    durationMs: null,
    exitCode: null,
    signal: null,
    timedOut: false,
    result: 'pending',
    artifacts: [],
    outputDigest: { bytes: 0, hash: sha256(''), redacted: true },
    receiptHash: 'sha256:' + '0'.repeat(64)
  };
  const child = spawn(invocation.executable, invocation.args, {
    cwd: canonicalRoot, env, detached: true, stdio: ['ignore', 'pipe', 'pipe']
  });
  await new Promise((resolve, reject) => {
    child.once('spawn', resolve);
    child.once('error', (error) => reject(new TestCertificationError(
      'EXECUTION_START_FAILED', 'structural gate spawn failed (' + (error.code || 'UNKNOWN') + ')')));
  });
  base.pid = child.pid;
  base.processGroup = child.pid;
  base.receiptHash = receiptContract.receiptHashOf(base);
  let started;
  try {
    started = receiptContract.validateStructuralReceipt(base);
    persistReceipt(root, 'structural', ordinal, started, canonicalCertificationRoot);
  } catch (error) {
    try { process.kill(-child.pid, 'SIGKILL'); } catch (killError) {}
    throw error;
  }

  const outputChunks = [];
  let outputByteLength = 0;
  let truncated = false;
  const capture = (chunk) => {
    const bytes = Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk);
    const remaining = OUTPUT_CAP_BYTES - outputByteLength;
    if (remaining > 0) {
      const kept = bytes.subarray(0, remaining);
      outputChunks.push(kept);
      outputByteLength += kept.length;
    }
    if (bytes.length > remaining) truncated = true;
  };
  child.stdout.on('data', capture);
  child.stderr.on('data', capture);
  let timedOut = false;
  let escalation = null;
  const timer = setTimeout(() => {
    timedOut = true;
    try { process.kill(-child.pid, 'SIGTERM'); } catch (error) {}
    escalation = setTimeout(() => { try { process.kill(-child.pid, 'SIGKILL'); } catch (error) {} }, 5000);
    escalation.unref();
  }, timeoutMs);
  const { exitCode, signal } = await new Promise((resolve) => {
    child.on('close', (code, sig) => resolve({ exitCode: code, signal: sig }));
  });
  clearTimeout(timer);
  if (escalation) clearTimeout(escalation);

  let groupAlive = false;
  try { process.kill(-child.pid, 0); groupAlive = true; }
  catch (error) { if (error.code !== 'ESRCH') groupAlive = true; }
  if (groupAlive) {
    try { process.kill(-child.pid, 'SIGKILL'); } catch (error) {}
    await new Promise((resolve) => setTimeout(resolve, 200));
    try { process.kill(-child.pid, 0); fail('ORPHAN_PROCESS', 'structural process group survived the kill escalation'); }
    catch (error) {
      if (error instanceof TestCertificationError) throw error;
      if (error.code !== 'ESRCH') fail('ORPHAN_PROCESS', 'structural process group could not be proven dead (' + error.code + ')');
    }
  }

  const output = Buffer.concat(outputChunks, outputByteLength).toString('utf8');
  const redacted = redactOutput(truncated ? output + '\n[TRUNCATED]' : output, parentEnv);
  let outputBytes = Buffer.from(redacted, 'utf8');
  if (outputBytes.length > OUTPUT_CAP_BYTES) {
    const marker = Buffer.from('\n[TRUNCATED-AFTER-REDACTION]', 'utf8');
    outputBytes = Buffer.concat([outputBytes.subarray(0, OUTPUT_CAP_BYTES - marker.length), marker]);
  }
  writeSealed(path.join(root, 'reports', ordinal + '-structural-output.log'), outputBytes, canonicalCertificationRoot);
  const terminal = {
    ...base,
    stage: 'terminal',
    startedReceiptHash: started.receiptHash,
    endedAt: nowIso(),
    durationMs: Math.max(0, Date.now() - startedAtMs),
    exitCode: exitCode === null ? null : exitCode,
    signal: signal || null,
    timedOut,
    result: timedOut || signal ? 'aborted' : exitCode === 0 ? 'passed' : 'failed',
    outputDigest: { bytes: outputBytes.length, hash: sha256(outputBytes), redacted: true },
    receiptHash: 'sha256:' + '0'.repeat(64)
  };
  terminal.receiptHash = receiptContract.receiptHashOf(terminal);
  const sealed = receiptContract.validateStructuralReceipt(terminal);
  persistReceipt(root, 'structural', ordinal, sealed, canonicalCertificationRoot);
  return {
    receipt: sealed,
    startedReceipt: started,
    passed: sealed.result === 'passed',
    receiptId: registry.receiptIdOf('test-structural-gate', sealed.receiptHash)
  };
}

if (process.argv[1] && path.resolve(process.argv[1]) === path.join(HERE, 'run-test-certification.mjs')) {
  process.stderr.write('run-test-certification.mjs is the receipt-producer library; invoke the trusted ' +
    'run-test-certification-request.mjs orchestrator entrypoint.\n');
  process.exit(64);
}
