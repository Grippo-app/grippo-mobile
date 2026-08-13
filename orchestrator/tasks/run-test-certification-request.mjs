#!/usr/bin/env node

// Canonical orchestrator activation hook for deterministic test certification.
// Usage (from the product root):
//   node orchestrator/tasks/run-test-certification-request.mjs --request <relative-json>
// The request selects only hash-bound inputs, inventory task paths and policy
// gate ids. It cannot supply executable programs, shell fragments, a verdict,
// receipts or a summary.

import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import { spawnSync } from 'node:child_process';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

import { certifyCommand, certifyStructuralGate } from './run-test-certification.mjs';
import { aggregateAndSeal } from './aggregate-test-certification.mjs';

const require = createRequire(import.meta.url);
const policyContract = require('./task-test-policy-contract.cjs');
const impactContract = require('./task-test-impact-contract.cjs');
const snapshotContract = require('./content-snapshot.cjs');
const capabilityContract = require('./task-test-capability-contract.cjs');
const taskInputContract = require('./task-test-input-contract.cjs');
const fileGuards = require('../site/server/file-guards');

const HERE = path.dirname(fileURLToPath(import.meta.url));
const HASH_RE = /^sha256:[0-9a-f]{64}$/;
const ID_RE = /^[a-z][a-z0-9-]{0,79}$/;
const LANES = new Set(['android-device', 'common', 'host', 'ios-simulator', 'screenshot', 'structural']);
const TIERS = new Set(['affected-closure', 'builder-feedback', 'certification-direct', 'full-suite', 'owner-module', 'platform-lanes']);
const PARSERS = new Set(['junit-xml', 'kotlin-native-xml', 'android-connected-xml', 'roborazzi-report']);
const ROOT_AGGREGATES = Object.freeze({
  host: ':allHostTests',
  'ios-simulator': ':allIosSimulatorTests',
  'android-device': ':allAndroidDeviceTests',
  screenshot: ':allScreenshotTests',
  full: ':allConfiguredTests'
});

class TestCertificationRequestError extends Error {
  constructor(code, message) {
    super(code + ': ' + message);
    this.name = 'TestCertificationRequestError';
    this.code = code;
  }
}

function fail(code, message) { throw new TestCertificationRequestError(code, message); }
function sortedUnique(values) { return [...new Set(values)].sort(); }
function exactKeys(value, expected, label) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) fail('REQUEST_INVALID', label + ' must be an object');
  const actual = Object.keys(value).sort();
  const wanted = [...expected].sort();
  if (actual.length !== wanted.length || actual.some((key, index) => key !== wanted[index])) {
    fail('REQUEST_INVALID', label + ' keys must be exactly ' + wanted.join(','));
  }
}
function safeRelative(value, label) {
  if (typeof value !== 'string' || value.length === 0 || value.length > 1024 || value.includes('\0') ||
      value.includes('\\') || path.isAbsolute(value) || value.split('/').some((part) => !part || part === '.' || part === '..')) {
    fail('REQUEST_INVALID', label + ' must be a bounded normalized relative path');
  }
  return value;
}
function canonicalRoot(root) {
  const resolved = path.resolve(root);
  const stat = fs.lstatSync(resolved);
  if (stat.isSymbolicLink() || !stat.isDirectory()) {
    fail('ROOT_INVALID', 'product root must be a real canonical directory');
  }
  return fs.realpathSync.native(resolved);
}
function safeBytes(root, relative, label, maxBytes = 16 * 1024 * 1024) {
  const segments = safeRelative(relative, label).split('/');
  let current = root;
  for (let index = 0; index < segments.length - 1; index++) {
    current = path.join(current, segments[index]);
    const stat = fs.lstatSync(current);
    if (stat.isSymbolicLink() || !stat.isDirectory()) fail('INPUT_UNSAFE', label + ' has an unsafe ancestor');
  }
  const file = path.join(root, ...segments);
  const guarded = fileGuards.boundedRegularFileUnder(root, path.dirname(file), file, maxBytes);
  if (!guarded) fail('INPUT_UNSAFE', label + ' or an ancestor changed while reading');
  return guarded.bytes;
}
function safeJson(root, relative, label) {
  const bytes = safeBytes(root, relative, label);
  try { return JSON.parse(bytes.toString('utf8')); }
  catch (error) { fail('INPUT_INVALID', label + ' is not valid JSON'); }
}

function validateToolchain(toolchain) {
  exactKeys(toolchain, ['gradle', 'kotlin', 'agp', 'jdk', 'os', 'arch'], 'toolchain');
  for (const value of Object.values(toolchain)) {
    if (typeof value !== 'string' || value.length === 0 || value.length > 40) fail('REQUEST_INVALID', 'toolchain value grammar');
  }
}

function validateRequest(request) {
  exactKeys(request, ['version', 'identity', 'taskInputHash', 'sourceManifestPath', 'plannedImpactPath',
    'observedImpactPath', 'capabilityInventoryPath', 'commands', 'structuralGateIds', 'toolchain',
    'executionRootKind'], 'request');
  if (request.version !== 1) fail('REQUEST_INVALID', 'unsupported request version');
  exactKeys(request.identity, ['taskStem', 'runId', 'sessionId', 'lockStage', 'lockHash'], 'identity');
  if (!/^TASK_[1-9][0-9]*_[A-Za-z0-9_]+$/.test(String(request.identity.taskStem)) ||
      !/^run-[A-Za-z0-9][A-Za-z0-9-]{6,79}$/.test(String(request.identity.runId)) ||
      !/^ws-[A-Za-z0-9][A-Za-z0-9._-]{15,159}$/.test(String(request.identity.sessionId)) ||
      request.identity.lockStage !== 'orchestrator' || !HASH_RE.test(String(request.identity.lockHash))) {
    fail('REQUEST_INVALID', 'identity must exactly match a canonical orchestrator lock');
  }
  if (!HASH_RE.test(String(request.taskInputHash))) fail('REQUEST_INVALID', 'taskInputHash grammar');
  for (const key of ['sourceManifestPath', 'plannedImpactPath', 'observedImpactPath', 'capabilityInventoryPath']) {
    safeRelative(request[key], key);
  }
  validateToolchain(request.toolchain);
  if (!['shared-serial', 'task-worktree', 'integrated'].includes(request.executionRootKind)) {
    fail('REQUEST_INVALID', 'unknown executionRootKind');
  }
  if (!Array.isArray(request.commands) || request.commands.length > 128) fail('REQUEST_INVALID', 'commands bounds');
  for (const command of request.commands) {
    exactKeys(command, ['suite', 'lane', 'tier', 'taskPaths', 'reportInputs', 'timeoutMs', 'continueOnFailure'], 'command');
    if (!ID_RE.test(String(command.suite)) || !LANES.has(command.lane) || !TIERS.has(command.tier)) {
      fail('REQUEST_INVALID', 'command suite/lane/tier grammar');
    }
    if (!Array.isArray(command.taskPaths) || command.taskPaths.length === 0 || command.taskPaths.length > 64 ||
        new Set(command.taskPaths).size !== command.taskPaths.length) fail('REQUEST_INVALID', 'command taskPaths bounds');
    if (!Array.isArray(command.reportInputs) || command.reportInputs.length > 64) fail('REQUEST_INVALID', 'reportInputs bounds');
    for (const input of command.reportInputs) {
      exactKeys(input, ['path', 'parser'], 'reportInput');
      safeRelative(input.path, 'reportInput.path');
      if (!PARSERS.has(input.parser)) fail('REQUEST_INVALID', 'unknown report parser');
    }
    if (!Number.isSafeInteger(command.timeoutMs) || command.timeoutMs < 1000 || command.timeoutMs > 2 * 60 * 60 * 1000 ||
        typeof command.continueOnFailure !== 'boolean') fail('REQUEST_INVALID', 'command timeout/continue grammar');
  }
  if (!Array.isArray(request.structuralGateIds) || request.structuralGateIds.length > 16 ||
      new Set(request.structuralGateIds).size !== request.structuralGateIds.length ||
      request.structuralGateIds.some((gateId) => !ID_RE.test(String(gateId)))) {
    fail('REQUEST_INVALID', 'structuralGateIds bounds');
  }
  return request;
}

function verifyLock(productRoot, identity) {
  const result = spawnSync(process.execPath, [path.join(HERE, 'task-lock.mjs'), 'verify',
    '--stem', identity.taskStem, '--stage', identity.lockStage, '--run-id', identity.runId,
    '--session-id', identity.sessionId, '--expected-hash', identity.lockHash], {
    cwd: productRoot, env: {
      ...process.env,
      ORCHESTRATOR_PROJECT_ROOT: productRoot,
      ORCHESTRATOR_TASKS_DIR: path.join(productRoot, 'orchestrator', 'tasks'),
      ORCHESTRATOR_LOCKS_DIR: path.join(productRoot, 'orchestrator', '.cache', 'tasks', 'locks')
    },
    encoding: 'utf8', timeout: 30000, maxBuffer: 1024 * 1024
  });
  if (result.error || result.status !== 0) {
    fail('LOCK_VERIFICATION_FAILED', String(result.error && result.error.message || result.stderr || 'lock verification failed').slice(0, 500));
  }
}

function resolveExecutionRoot(controlRoot, request, options = {}) {
  if (request.executionRootKind !== 'task-worktree') return controlRoot;
  const environment = options.environment || process.env;
  const manager = options.manager || require('../site/server/worktree-manager.js');
  const resolved = manager.executionEnvironmentContext(environment);
  if (!resolved || resolved.ok !== true || !resolved.context) {
    fail('EXECUTION_ROOT_UNPROVEN', String(resolved && resolved.message ||
      'task-worktree execution environment could not be proven').slice(0, 500));
  }
  const context = resolved.context;
  if (environment.ORCHESTRATOR_WRITER_STEM !== request.identity.taskStem ||
      context.runId !== request.identity.runId || context.controlRoot !== controlRoot) {
    fail('EXECUTION_ROOT_MISMATCH', 'task-worktree binding differs from the certification identity');
  }
  return canonicalRoot(context.executionRoot);
}

function validateExecutionPlan(request, observed, inventory, policy) {
  if (inventory.inventoryHash !== observed.capabilityInventoryHash) {
    fail('INVENTORY_MISMATCH', 'observed impact binds a different capability inventory');
  }
  const inventoryTasks = capabilityContract.allowedTaskPaths(inventory);
  const allowed = sortedUnique([...inventoryTasks, ...Object.values(ROOT_AGGREGATES)]);
  const laneTasks = {};
  for (const module of inventory.modules) {
    for (const [lane, descriptor] of Object.entries(module.lanes)) {
      if (!laneTasks[lane]) laneTasks[lane] = [];
      laneTasks[lane].push(descriptor.taskPath);
    }
  }
  for (const command of request.commands) {
    for (const taskPath of command.taskPaths) {
      if (!allowed.includes(taskPath)) fail('ALLOWLIST_VIOLATION', 'task path is absent from the sealed capability inventory: ' + taskPath);
      const laneAllowed = command.tier === 'full-suite' ? [ROOT_AGGREGATES.full]
        : command.lane === 'common'
          ? [...(laneTasks.host || []), ROOT_AGGREGATES.host]
          : [...(laneTasks[command.lane] || []), ROOT_AGGREGATES[command.lane]].filter(Boolean);
      if (!laneAllowed.includes(taskPath)) {
        fail('ALLOWLIST_VIOLATION', 'task path is not owned by the declared lane/tier: ' + taskPath);
      }
    }
  }
  const requiredLanes = [...new Set(observed.behaviors.flatMap((behavior) => behavior.requiredLanes))];
  const commandLanes = new Set(request.commands.map((command) => command.lane));
  for (const lane of requiredLanes.filter((value) => value !== 'structural')) {
    if (!commandLanes.has(lane)) fail('PLAN_INCOMPLETE', 'no command planned for required lane: ' + lane);
  }
  const suites = new Set(request.commands.map((command) => command.suite));
  for (const suite of observed.requiredSuites) {
    if (!suites.has(suite)) fail('PLAN_INCOMPLETE', 'no command planned for required suite: ' + suite);
  }
  if (observed.fullSuiteRequired && !request.commands.some((command) => command.tier === 'full-suite')) {
    fail('PLAN_INCOMPLETE', 'observed impact requires a full-suite command');
  }
  const expectedGates = observed.testNotApplicable !== null
    ? observed.notApplicableValidators
    : requiredLanes.includes('structural') ? ['bootstrap-foundation-fixture'] : [];
  if (request.structuralGateIds.join(',') !== [...expectedGates].sort().join(',')) {
    fail('PLAN_INCOMPLETE', 'structural gates must exactly match the policy-derived set');
  }
  if (request.structuralGateIds.some((gateId) => !policy.structuralGateIds.includes(gateId))) {
    fail('ALLOWLIST_VIOLATION', 'structural gate is absent from policy');
  }
  if (observed.testNotApplicable !== null && request.commands.length > 0) {
    fail('PLAN_INCOMPLETE', 'typed N/A cannot execute command receipts');
  }
  const structuralOnly = observed.testNotApplicable === null &&
    request.commands.length === 0 &&
    observed.requiredSuites.length === 0 &&
    requiredLanes.length > 0 &&
    requiredLanes.every((lane) => lane === 'structural') &&
    request.structuralGateIds.length > 0;
  if (observed.testNotApplicable === null && request.commands.length === 0 && !structuralOnly) {
    fail('PLAN_INCOMPLETE', 'executable impact requires command receipts');
  }
  return allowed;
}

export { resolveExecutionRoot, validateExecutionPlan };

async function runCertificationRequest({ productRoot, request }) {
  const root = canonicalRoot(productRoot);
  const validRequest = validateRequest(request);
  verifyLock(root, validRequest.identity);
  const executionRoot = resolveExecutionRoot(root, validRequest);
  const taskRelative = 'orchestrator/tasks/todo/' + validRequest.identity.taskStem + '.md';
  const actualTaskInputHash = taskInputContract.taskInputHashOf(
    safeBytes(root, taskRelative, 'canonical task input', 8 * 1024 * 1024));
  if (actualTaskInputHash !== validRequest.taskInputHash) {
    fail('TASK_INPUT_STALE', 'request taskInputHash does not match the current pre-Outcome task bytes');
  }
  const policy = policyContract.validatePolicy(safeJson(root, 'orchestrator/tasks/test-policy.json', 'policy'));
  const sourceManifest = snapshotContract.validateManifest(safeJson(root, validRequest.sourceManifestPath, 'sourceManifest'));
  const plannedImpact = impactContract.validateImpact(safeJson(root, validRequest.plannedImpactPath, 'plannedImpact'), { policy });
  const observedImpact = impactContract.validateImpact(safeJson(root, validRequest.observedImpactPath, 'observedImpact'), { policy });
  impactContract.checkWidening(plannedImpact, observedImpact, { policy });
  const inventory = capabilityContract.validateInventory(safeJson(root, validRequest.capabilityInventoryPath, 'capabilityInventory'));
  const allowedTaskPaths = validateExecutionPlan(validRequest, observedImpact, inventory, policy);
  if (sourceManifest.snapshotHash !== observedImpact.sourceSnapshotHash ||
      validRequest.taskInputHash !== observedImpact.taskInputHash ||
      validRequest.identity.taskStem !== observedImpact.taskStem || validRequest.identity.runId !== observedImpact.runId) {
    fail('CONTEXT_MISMATCH', 'request, impact and source snapshot identities differ');
  }

  const certificationRoot = path.join(root, 'orchestrator', '.cache', 'tasks', 'test-certification');
  const identity = validRequest.identity;
  const hashes = {
    taskInputHash: validRequest.taskInputHash,
    sourceSnapshotHash: sourceManifest.snapshotHash,
    impactHash: observedImpact.impactHash,
    policyHash: policy.policyHash
  };
  for (let index = 0; index < validRequest.commands.length; index++) {
    const command = validRequest.commands[index];
    await certifyCommand({
      certificationRoot, productRoot: executionRoot, taskPaths: command.taskPaths, allowedTaskPaths,
      suite: command.suite, tier: command.tier, lane: command.lane, identity, hashes,
      toolchain: validRequest.toolchain, reportInputs: command.reportInputs,
      timeoutMs: command.timeoutMs, continueOnFailure: command.continueOnFailure,
      executionRootKind: validRequest.executionRootKind, ordinal: String(index).padStart(3, '0'),
      testsRequired: true
    });
  }
  for (let index = 0; index < validRequest.structuralGateIds.length; index++) {
    await certifyStructuralGate({
      certificationRoot, productRoot: executionRoot, gateId: validRequest.structuralGateIds[index], identity, hashes,
      ordinal: String(index).padStart(3, '0')
    });
  }
  return aggregateAndSeal({
    certificationRoot, productRoot: executionRoot, identity,
    taskInputHash: validRequest.taskInputHash, sourceManifest, policy, plannedImpact, observedImpact
  });
}

function parseCli(argv) {
  if (argv.length !== 2 || argv[0] !== '--request') fail('CLI_INVALID', 'usage: --request <relative-json>');
  return safeRelative(argv[1], 'request path');
}

if (process.argv[1] && path.resolve(process.argv[1]) === path.join(HERE, 'run-test-certification-request.mjs')) {
  try {
    const productRoot = canonicalRoot(process.cwd());
    const requestPath = parseCli(process.argv.slice(2));
    const result = await runCertificationRequest({ productRoot, request: safeJson(productRoot, requestPath, 'request') });
    process.stdout.write(JSON.stringify(result, null, 2) + '\n');
  } catch (error) {
    process.stderr.write(String(error && error.message || error) + '\n');
    process.exitCode = 1;
  }
}
