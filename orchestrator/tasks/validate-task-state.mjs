#!/usr/bin/env node

import { createRequire } from 'node:module';
import fs from 'node:fs';
import path from 'node:path';
import { tmpdir } from 'node:os';
import { fileURLToPath } from 'node:url';

const require = createRequire(import.meta.url);
const HERE = path.dirname(fileURLToPath(import.meta.url));
const core = require('./task-state-core.cjs');
const platformSupport = require('./platform-support.cjs');
const runtimeIntegrity = require('../site/server/runtime-integrity.js');
const MAX_PROPOSAL_BYTES = 8 * 1024 * 1024;
const STDIN_CHUNK_BYTES = 64 * 1024;

function usage() {
  return [
    'usage: node orchestrator/tasks/validate-task-state.mjs [<stem>] [options]',
    '',
    '  --all                      validate the full task corpus',
    '  --stem <stem>              validate one task/dependency closure plus global identities',
    '  --expect <state>            assert absent|backlog|pending|todo|done',
    '  --action <action>           validate prep|answers|run|finalize|drop|reopen admission',
    '  --transition <from>:<to>   validate an allow-listed transition',
    '  --phase pre|post            select transition pre/postcondition',
    '  --check-index               compare INDEX.json with fresh derivation',
    '  --check-arch                global-only, read-only architecture freshness report',
    '  --proposal <path>           validate replacement bytes without mutating task state',
    '  --proposal-state <state>    proposed state: backlog|pending|todo|done',
    '  --proposal-from-state <state> current state; defaults to proposal-state',
    '  --json                      print the versioned JSON envelope',
    '  --quiet                     emit no output',
    '  --caller <name>             bounded observability caller label'
  ].join('\n');
}

function invocationError(message) {
  const error = new Error(message);
  error.exitCode = 2;
  return error;
}

function requestedOutputMode(argv) {
  // Output control is honored even when a later token makes parsing fail.
  // Quiet wins the otherwise-invalid quiet+json combination so a caller that
  // explicitly requested no output never receives usage/error text.
  return {
    quiet: argv.includes('--quiet'),
    json: !argv.includes('--quiet') && argv.includes('--json'),
    caller: 'manual'
  };
}

function parseArgs(argv) {
  const options = {
    all: false,
    stem: null,
    expect: null,
    action: null,
    transition: null,
    phase: null,
    checkIndex: false,
    checkArch: false,
    proposal: null,
    proposalState: null,
    proposalFromState: null,
    json: false,
    quiet: false,
    caller: 'manual'
  };
  const take = (index, flag) => {
    if (index + 1 >= argv.length || argv[index + 1].startsWith('--')) throw invocationError(flag + ' requires a value');
    return argv[index + 1];
  };
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];
    if (arg === '--help' || arg === '-h') { options.help = true; continue; }
    if (arg === '--all') { options.all = true; continue; }
    if (arg === '--stem') { options.stem = take(i, arg); i++; continue; }
    if (arg === '--expect') { options.expect = take(i, arg); i++; continue; }
    if (arg === '--action') { options.action = take(i, arg); i++; continue; }
    if (arg === '--transition') { options.transition = take(i, arg); i++; continue; }
    if (arg === '--phase') { options.phase = take(i, arg); i++; continue; }
    if (arg === '--check-index') { options.checkIndex = true; continue; }
    if (arg === '--check-arch') { options.checkArch = true; continue; }
    if (arg === '--proposal') { options.proposal = take(i, arg); i++; continue; }
    if (arg === '--proposal-state') { options.proposalState = take(i, arg); i++; continue; }
    if (arg === '--proposal-from-state') { options.proposalFromState = take(i, arg); i++; continue; }
    if (arg === '--json') { options.json = true; continue; }
    if (arg === '--quiet') { options.quiet = true; continue; }
    if (arg === '--caller') {
      options.caller = take(i, arg);
      if (!/^[a-z][a-z0-9-]{0,39}$/.test(options.caller)) throw invocationError('--caller is invalid');
      i++; continue;
    }
    if (arg.startsWith('-')) throw invocationError('unknown option: ' + arg);
    if (options.stem) throw invocationError('more than one task stem was provided');
    options.stem = arg;
  }
  if (options.all && options.stem) throw invocationError('--all and --stem/positional stem are mutually exclusive');
  if (options.quiet && options.json) throw invocationError('--quiet and --json are mutually exclusive');
  if (options.expect && !options.stem) throw invocationError('--expect requires --stem');
  if (options.action && !options.stem) throw invocationError('--action requires --stem');
  if (options.action && (options.expect || options.transition || options.phase)) {
    throw invocationError('--action is mutually exclusive with --expect/--transition/--phase');
  }
  if (options.transition && (!options.stem || !options.phase)) throw invocationError('--transition requires --stem and --phase');
  if (options.phase && !options.transition) throw invocationError('--phase requires --transition');
  if (!!options.proposal !== !!options.proposalState || (options.proposal && !options.stem)) throw invocationError('--proposal and --proposal-state require --stem and each other');
  if (options.proposalFromState && !options.proposal) throw invocationError('--proposal-from-state requires --proposal and --proposal-state');
  if (options.proposal && (!['backlog', 'pending', 'todo', 'done'].includes(options.proposalState) || options.checkIndex || options.transition || options.action)) {
    throw invocationError('proposal validation cannot combine with --check-index/--transition/--action');
  }
  if (options.proposal) {
    const fromState = options.proposalFromState || options.proposalState;
    const sameState = fromState === options.proposalState;
    const createCandidate = fromState === 'absent' && options.proposalState === 'backlog';
    if (!sameState && !createCandidate) throw invocationError('proposal transition must be same-state or absent:backlog');
  }
  if (!options.all && !options.stem) options.all = true;
  if (options.checkArch && (!options.all || options.stem)) throw invocationError('--check-arch is available only with global --all validation');
  return options;
}

function boundedIntegerEnv(name, fallback, min, max) {
  const raw = process.env[name];
  if (raw === undefined || raw === '') return fallback;
  if (!/^\d+$/.test(raw)) throw invocationError(`${name} must be an integer in [${min}, ${max}]`);
  const value = Number(raw);
  if (!Number.isSafeInteger(value) || value < min || value > max) throw invocationError(`${name} must be an integer in [${min}, ${max}]`);
  return value;
}

function fixtureAtomicReplaceHook() {
  const targetRaw = process.env.TASK_STATE_TEST_ATOMIC_REPLACE_TARGET;
  const sourceRaw = process.env.TASK_STATE_TEST_ATOMIC_REPLACE_SOURCE;
  if (!targetRaw && !sourceRaw) return null;
  if (!targetRaw || !sourceRaw) throw invocationError('fixture atomic-replace hook requires target and source');
  const repoRoot = path.resolve(process.env.ORCHESTRATOR_PROJECT_ROOT || process.cwd());
  const tasksDir = path.resolve(process.env.ORCHESTRATOR_TASKS_DIR || path.join(repoRoot, 'orchestrator', 'tasks'));
  const canonicalRepoRoot = path.resolve(HERE, '..', '..');
  const canonicalTasksDir = path.resolve(HERE);
  const tempRoot = path.resolve(tmpdir());
  const tempRelative = path.relative(tempRoot, repoRoot);
  const tasksRelative = path.relative(repoRoot, tasksDir);
  const isolatedTempFixture = tempRelative !== '' && tempRelative !== '..' && !tempRelative.startsWith('..' + path.sep) && !path.isAbsolute(tempRelative) &&
    tasksRelative !== '..' && !tasksRelative.startsWith('..' + path.sep) && !path.isAbsolute(tasksRelative);
  if (!isolatedTempFixture || repoRoot === canonicalRepoRoot || tasksDir === canonicalTasksDir) {
    throw invocationError('fixture atomic-replace hook is restricted to an isolated temporary project');
  }
  const target = path.resolve(targetRaw);
  const source = path.resolve(sourceRaw);
  const inside = (root, candidate) => {
    const rel = path.relative(root, candidate);
    return rel !== '' && rel !== '..' && !rel.startsWith('..' + path.sep) && !path.isAbsolute(rel);
  };
  if (!inside(tasksDir, target) || !inside(tasksDir, source) || path.dirname(target) !== path.dirname(source)) {
    throw invocationError('fixture atomic-replace hook paths must be sibling files inside the task fixture');
  }
  let fired = false;
  return ({ absolutePath, phase }) => {
    if (!fired && phase === 'before-path-revalidation' && path.resolve(absolutePath) === target) {
      fired = true;
      fs.renameSync(source, target);
    }
  };
}

function readBoundedStdin() {
  const chunks = [];
  let total = 0;
  while (total <= MAX_PROPOSAL_BYTES) {
    const capacity = Math.min(STDIN_CHUNK_BYTES, MAX_PROPOSAL_BYTES + 1 - total);
    const chunk = Buffer.allocUnsafe(capacity);
    const count = fs.readSync(0, chunk, 0, capacity, null);
    if (count === 0) break;
    chunks.push(chunk.subarray(0, count));
    total += count;
    if (total > MAX_PROPOSAL_BYTES) throw invocationError(`stdin proposal exceeds ${MAX_PROPOSAL_BYTES} bytes`);
  }
  if (total === 0) throw invocationError('stdin proposal must be non-empty');
  return Buffer.concat(chunks, total);
}

function pathWithin(root, candidate) {
  const relative = path.relative(path.resolve(root), path.resolve(candidate));
  return relative === '' || (relative !== '..' && !relative.startsWith('..' + path.sep) && !path.isAbsolute(relative));
}

function readProposal(file, repoRoot) {
  if (file === '-') return readBoundedStdin();
  const target = path.resolve(file);
  const tempRoot = path.resolve(tmpdir());
  const authorityRoot = pathWithin(repoRoot, target) ? repoRoot : pathWithin(tempRoot, target) ? tempRoot : null;
  if (!authorityRoot) throw invocationError('proposal file must be inside the project or operating-system temporary root');
  let read;
  try {
    read = core.readAnchoredFile({ path: target, authorityRoot, maxBytes: MAX_PROPOSAL_BYTES });
  } catch (error) {
    if (error instanceof core.SnapshotRaceError) throw error;
    if (error && error.code === 'ENOENT') throw invocationError('proposal file does not exist');
    if (error && ['DIRECTORY_UNSAFE', 'PATH_UNSAFE', 'PATH_OUTSIDE_AUTHORITY'].includes(error.code)) {
      throw invocationError('proposal path must use a real, in-authority directory chain');
    }
    throw error;
  }
  if (read.unsafe || read.tooLarge || !read.buffer || read.buffer.length < 1) {
    throw invocationError(`proposal must be a non-empty regular file no larger than ${MAX_PROPOSAL_BYTES} bytes`);
  }
  return read.buffer;
}

function publicErrorMessage(error, code) {
  if (code === 3) return 'Required task-state contract is unreadable or malformed.';
  if (code === 4) return 'Task-state inputs changed during validation; retry from a fresh snapshot.';
  return String(error && error.message || error).replace(/[\r\n\0]+/g, ' ').slice(0, 400);
}

function boundedErrorEnvelope(error, code) {
  const message = publicErrorMessage(error, code);
  return {
    version: 1,
    ok: false,
    scope: null,
    observedState: null,
    expectedState: null,
    transition: null,
    phase: null,
    snapshotHash: null,
    sourceRevision: null,
    indexStatus: 'unchecked',
    affectedStems: [],
    findings: [{
      code: error && error.code === 'PLATFORM_UNSUPPORTED' ? 'PLATFORM_UNSUPPORTED' :
        error && error.exitCode === 3 ? 'CONTRACT_UNREADABLE' : error && error.exitCode === 4 ? 'SNAPSHOT_RACE' : 'INVOCATION_INVALID',
      severity: 'blocker',
      stem: null,
      paths: [],
      message,
      recovery: error && error.exitCode === 4 ? 'Retry from a fresh filesystem snapshot.' : 'Correct the invocation or restore the required contract.'
    }],
    stats: { tasks: 0, files: 0, inventoryEntries: 0, taskRelatedEntries: 0, taskBodyReads: 0, taskBodyBytes: 0, scanMode: null, durationMs: 0 }
  };
}

function writeObservation(caller, result) {
  const slowThresholdMs = boundedIntegerEnv('TASK_STATE_SLOW_MS', 100, 0, 60000);
  process.stderr.write('[task-state] ' + JSON.stringify(core.observationFor(result, { caller, slowThresholdMs })) + '\n');
}

const fallbackOutput = requestedOutputMode(process.argv.slice(2));
let parsed;
try {
  parsed = parseArgs(process.argv.slice(2));
  if (parsed.help) {
    process.stdout.write(usage() + '\n');
    process.exit(0);
  }
  platformSupport.assertCanonicalTaskPlatform();
  const validationOptions = {
    stem: parsed.stem,
    expect: parsed.expect,
    transition: parsed.transition,
    phase: parsed.phase,
    checkIndex: parsed.checkIndex,
    includeRuntime: true,
    runtimeInspector: runtimeIntegrity.scanIntegrity,
    testReadHook: fixtureAtomicReplaceHook(),
    proposal: parsed.proposal ? { stem: parsed.stem, fromState: parsed.proposalFromState || parsed.proposalState,
      state: parsed.proposalState,
      bytes: readProposal(parsed.proposal, path.resolve(process.env.ORCHESTRATOR_PROJECT_ROOT || process.cwd())) } : null
  };
  const result = parsed.action
    ? core.validateAction(Object.assign({}, validationOptions, { action: parsed.action }))
    : core.validateTaskState(validationOptions);
  result.caller = parsed.caller;
  if (parsed.proposal) {
    result.proposalState = parsed.proposalState;
    result.proposalFromState = parsed.proposalFromState || parsed.proposalState;
  }
  if (parsed.checkArch) {
    const arch = core.checkArchitectureState({ repoRoot: result._model.repoRoot });
    result.derivedState = { arch };
    result.derivedOk = arch.ok;
    result.overallOk = result.ok && result.derivedOk;
  }
  const overallOk = result.overallOk === undefined ? result.ok : result.overallOk;
  if (!parsed.quiet) {
    if (parsed.json) process.stdout.write(JSON.stringify(result, null, 2) + '\n');
    else {
      const state = result.observedState === null ? 'corpus' : result.observedState;
      process.stdout.write((overallOk ? 'VALID' : 'INVALID') + ' ' + result.scope + ' (' + state + ', ' + result.findings.length + ' findings, ' + result.snapshotHash + ')\n');
      for (const item of result.findings) {
        process.stdout.write('[' + item.severity.toUpperCase() + '] ' + item.code + ': ' + item.message + (item.paths.length ? ' [' + item.paths.join(', ') + ']' : '') + '\n');
      }
      for (const item of result.derivedState && result.derivedState.arch.findings || []) {
        process.stdout.write('[' + item.severity.toUpperCase() + '] ' + item.code + ': ' + item.message + (item.paths.length ? ' [' + item.paths.join(', ') + ']' : '') + '\n');
      }
    }
    writeObservation(parsed.caller, result);
  }
  process.exitCode = overallOk ? 0 : 1;
} catch (error) {
  const code = Number.isInteger(error && error.exitCode) ? error.exitCode : (error instanceof core.ContractError ? 3 : error instanceof core.SnapshotRaceError ? 4 : 3);
  const output = parsed || fallbackOutput;
  if (!output.quiet) {
    const envelope = boundedErrorEnvelope(Object.assign(error, { exitCode: code }), code);
    if (output.json) {
      process.stdout.write(JSON.stringify(envelope, null, 2) + '\n');
      writeObservation(output.caller, envelope);
    }
    else {
      process.stderr.write('validate-task-state: ' + publicErrorMessage(error, code) + '\n');
      if (code === 2) process.stderr.write(usage() + '\n');
    }
  }
  process.exitCode = code;
}
