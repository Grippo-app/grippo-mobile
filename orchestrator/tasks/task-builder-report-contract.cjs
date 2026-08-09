'use strict';

// The sole current builder-report protocol. It accepts exactly version 1 and
// the complete current envelope. Unknown generations, missing/extra fields and
// ambiguous nested values are rejected; this module has no migration path.

const TOP_FIELDS = Object.freeze([
  'schemaVersion', 'agent', 'status', 'files_touched', 'produced_signatures',
  'blockers', 'assumptions', 'scope_deviations', 'handoff', 'tests_created',
  'tests_modified', 'behavior_anchors', 'test_cases',
  'test_capabilities_added', 'developer_runs', 'fail_before_claim',
  'additional_impact_found', 'test_not_applicable'
]);
const TEST_CASE_FIELDS = Object.freeze(['anchor', 'file', 'identity', 'lane']);
const FAIL_BEFORE_FIELDS = Object.freeze(['reason', 'testIdentity']);
const STATUSES = Object.freeze(['done', 'blocked', 'failed', 'skipped']);
const CAPABILITIES = Object.freeze([
  'base', 'compose-ui', 'coroutines', 'coverage', 'di', 'flow', 'network',
  'room', 'screenshot'
]);
const ID_RE = /^[a-z][a-z0-9-]{0,79}$/;
const ANCHOR_RE = /^test:[a-z0-9]+(?:-[a-z0-9]+)*$/;
const HANDOFF_KEY_RE = /^[A-Za-z][A-Za-z0-9_-]{0,79}$/;
const MAX_BYTES = 256 * 1024;

class BuilderReportError extends Error {
  constructor(code, message) {
    super(code + ': ' + message);
    this.name = 'BuilderReportError';
    this.code = code;
  }
}

function fail(message) { throw new BuilderReportError('BUILDER_REPORT_INVALID', message); }

function plainObject(value) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return false;
  const prototype = Object.getPrototypeOf(value);
  return prototype === Object.prototype || prototype === null;
}

function exactKeys(value, fields, label) {
  if (!plainObject(value)) fail(label + ' must be a plain object');
  const actual = Object.keys(value).sort();
  const expected = [...fields].sort();
  if (actual.length !== expected.length || actual.some((key, index) => key !== expected[index])) {
    fail(label + ' fields must be exactly ' + expected.join(','));
  }
}

function boundedString(value, label, max = 500) {
  if (typeof value !== 'string' || value.length === 0 || value.length > max || value.includes('\0')) {
    fail(label + ' must be a bounded non-empty string');
  }
}

function stringList(value, label, { maxItems = 256, maxLength = 500, pattern = null } = {}) {
  if (!Array.isArray(value) || value.length > maxItems) fail(label + ' bounds');
  const seen = new Set();
  for (const item of value) {
    boundedString(item, label + ' entry', maxLength);
    if (pattern && !pattern.test(item)) fail(label + ' entry grammar: ' + item);
    if (seen.has(item)) fail(label + ' contains a duplicate');
    seen.add(item);
  }
}

function relativePath(value, label) {
  boundedString(value, label, 500);
  if (value.startsWith('/') || value.includes('\\') || value.split('/').some((part) => !part || part === '.' || part === '..')) {
    fail(label + ' must be a normalized relative path');
  }
}

function validateBuilderReport(value, { policy } = {}) {
  exactKeys(value, TOP_FIELDS, 'builder report');
  if (value.schemaVersion !== 1) fail('schemaVersion must be exactly 1');
  if (!ID_RE.test(String(value.agent))) fail('agent grammar');
  if (!STATUSES.includes(value.status)) fail('status grammar');

  for (const field of ['files_touched', 'tests_created', 'tests_modified']) {
    stringList(value[field], field);
    value[field].forEach((entry) => relativePath(entry, field));
  }
  for (const field of ['produced_signatures', 'blockers', 'assumptions', 'scope_deviations',
    'developer_runs', 'additional_impact_found']) {
    stringList(value[field], field);
  }
  if (['blocked', 'failed'].includes(value.status) && value.blockers.length === 0) {
    fail(value.status + ' status requires blockers');
  }
  if (['done', 'skipped'].includes(value.status) && value.blockers.length > 0) {
    fail(value.status + ' status forbids blockers');
  }
  if (new Set([...value.tests_created, ...value.tests_modified]).size !==
      value.tests_created.length + value.tests_modified.length) {
    fail('a test file cannot be both created and modified');
  }

  if (!plainObject(value.handoff)) fail('handoff must be a plain object');
  if (Object.keys(value.handoff).length > 32) fail('handoff bounds');
  for (const [key, item] of Object.entries(value.handoff)) {
    if (!HANDOFF_KEY_RE.test(key)) fail('handoff key grammar');
    if (item !== null) boundedString(item, 'handoff value', 500);
  }

  stringList(value.behavior_anchors, 'behavior_anchors', { maxItems: 200, maxLength: 120, pattern: ANCHOR_RE });
  if (!Array.isArray(value.test_cases) || value.test_cases.length > 512) fail('test_cases bounds');
  const testFiles = new Set([...value.tests_created, ...value.tests_modified]);
  const identities = new Set();
  for (const testCase of value.test_cases) {
    exactKeys(testCase, TEST_CASE_FIELDS, 'test case');
    if (!value.behavior_anchors.includes(testCase.anchor)) fail('test case anchor is undeclared');
    relativePath(testCase.file, 'test case file');
    if (!testFiles.has(testCase.file)) fail('test case file is absent from tests_created/tests_modified');
    boundedString(testCase.identity, 'test case identity', 300);
    if (identities.has(testCase.identity)) fail('duplicate test case identity');
    identities.add(testCase.identity);
    if (!policy || !Array.isArray(policy.lanes) || !policy.lanes.includes(testCase.lane)) {
      fail('test case lane is absent from the current policy');
    }
  }

  stringList(value.test_capabilities_added, 'test_capabilities_added', { maxItems: 32, maxLength: 80, pattern: ID_RE });
  if (value.test_capabilities_added.some((item) => !CAPABILITIES.includes(item))) {
    fail('test capability is absent from the current policy');
  }

  if (value.fail_before_claim !== null) {
    exactKeys(value.fail_before_claim, FAIL_BEFORE_FIELDS, 'fail_before_claim');
    boundedString(value.fail_before_claim.testIdentity, 'fail_before_claim.testIdentity', 300);
    boundedString(value.fail_before_claim.reason, 'fail_before_claim.reason', 500);
    if (!identities.has(value.fail_before_claim.testIdentity)) fail('fail-before identity is not a declared test case');
  }
  if (value.test_not_applicable !== null) {
    if (!policy || !Array.isArray(policy.testNotApplicable) ||
        !policy.testNotApplicable.includes(value.test_not_applicable)) {
      fail('test_not_applicable is absent from the current policy');
    }
    if (value.test_cases.length > 0 || value.behavior_anchors.length > 0 ||
        value.tests_created.length > 0 || value.tests_modified.length > 0) {
      fail('test_not_applicable excludes executable test claims');
    }
  }

  if (Buffer.byteLength(JSON.stringify(value), 'utf8') > MAX_BYTES) fail('builder report exceeds its byte limit');
  return Object.freeze(JSON.parse(JSON.stringify(value)));
}

function parseBuilderReport(bytes, options) {
  let value;
  try {
    const text = Buffer.isBuffer(bytes)
      ? new TextDecoder('utf-8', { fatal: true }).decode(bytes)
      : String(bytes);
    if (Buffer.byteLength(text, 'utf8') > MAX_BYTES) fail('builder report exceeds its byte limit');
    value = JSON.parse(text);
  } catch (error) {
    if (error instanceof BuilderReportError) throw error;
    fail('builder report must be valid JSON');
  }
  return validateBuilderReport(value, options);
}

function readStdinBounded() {
  const fs = require('fs');
  const chunks = [];
  let total = 0;
  const buffer = Buffer.alloc(4096);
  while (total <= MAX_BYTES) {
    const count = fs.readSync(0, buffer, 0, Math.min(buffer.length, MAX_BYTES + 1 - total), null);
    if (!count) break;
    chunks.push(Buffer.from(buffer.subarray(0, count)));
    total += count;
  }
  if (total > MAX_BYTES) fail('builder report exceeds its byte limit');
  return Buffer.concat(chunks, total);
}

function main() {
  try {
    const policy = require('./task-test-policy-contract.cjs').loadPolicy();
    const report = parseBuilderReport(readStdinBounded(), { policy });
    process.stdout.write(JSON.stringify(report) + '\n');
    return 0;
  } catch (error) {
    process.stderr.write(String(error && error.message || error).slice(0, 1000) + '\n');
    return 1;
  }
}

module.exports = Object.freeze({
  TOP_FIELDS,
  TEST_CASE_FIELDS,
  STATUSES,
  CAPABILITIES,
  MAX_BYTES,
  BuilderReportError,
  validateBuilderReport,
  parseBuilderReport,
  main
});

if (require.main === module) process.exitCode = main();
