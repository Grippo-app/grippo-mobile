'use strict';

// Typed, lease-guarded Architecture Map generation jobs. Browser input never
// contains a command or path. The canonical Python generator owns staging,
// semantic validation, immutable history and atomic publication.

var childProcess = require('child_process');
var crypto = require('crypto');
var path = require('path');
var fs = require('fs');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var finalizations = require('./finalizations');
var writerLeases = require('../../tasks/writer-leases.cjs');
var arch = require('./arch');
var contract = require('./architecture-contract');

var JOB_RE = /^archjob-[a-f0-9]{32}$/;
var HASH_RE = /^sha256:[a-f0-9]{64}$/;
var TTL_MS = 60 * 1000;
var OUTPUT_MAX = 256 * 1024;
var JOB_MAX = 128 * 1024;
var JOB_SCAN_MAX = 500;
var JOB_RETAIN = 100;
var JOB_DIR = path.join(paths.PROJECT_ORCHESTRATOR_DIR, '.cache', 'architecture', 'jobs');
var GENERATOR = path.join(paths.PROJECT_ORCHESTRATOR_DIR, 'tasks', 'regen-arch.py');
var LEASE_VERIFIER = path.join(__dirname, '..', '..', 'tasks', 'architecture-lease-verify.cjs');
var jobs = Object.create(null);
var children = Object.create(null);
var activeJobId = null;
var notifier = function () {};

function exact(value, fields) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === fields.slice().sort().join('\0');
}
function id() { return 'archjob-' + crypto.randomBytes(16).toString('hex'); }
function publicError(code) {
  var known = {
    'architecture-generate-busy': 1,
    'architecture-project-not-ready': 1,
    'architecture-source-conflict': 1,
    'architecture-reason-conflict': 1,
    'architecture-writer-unavailable': 1,
    'architecture-generator-unavailable': 1,
    'architecture-generator-failed': 1,
    'architecture-generator-output-invalid': 1,
    'architecture-writer-lease-lost': 1,
    'architecture-publication-invalid': 1,
    'architecture-job-storage-unavailable': 1,
    'architecture-job-interrupted': 1
  };
  return known[code] ? code : 'architecture-generation-failed';
}
function publicJob(job) {
  if (!job) return null;
  return {
    schemaVersion: 1,
    id: job.id,
    state: job.state,
    phase: job.phase,
    reason: job.reason,
    expectedSourceRevision: job.expectedSourceRevision,
    startedAt: job.startedAt,
    finishedAt: job.finishedAt,
    structuralHash: job.structuralHash,
    generatedAtRevision: job.generatedAtRevision,
    error: job.error ? { code: publicError(job.error.code) } : null
  };
}
function validPublicJob(value) {
  if (!exact(value, [
    'schemaVersion', 'id', 'state', 'phase', 'reason',
    'expectedSourceRevision', 'startedAt', 'finishedAt', 'structuralHash',
    'generatedAtRevision', 'error'
  ]) || value.schemaVersion !== 1 || !JOB_RE.test(String(value.id || '')) ||
      ['queued', 'running', 'succeeded', 'failed', 'interrupted'].indexOf(value.state) < 0 ||
      ['missing', 'stale', 'manual'].indexOf(value.reason) < 0 ||
      !contract.exactInstant(value.startedAt, true) ||
      value.expectedSourceRevision !== null &&
        !HASH_RE.test(String(value.expectedSourceRevision || ''))) return false;
  if (value.state === 'queued') {
    return value.phase === 'acquiring-writer' && value.finishedAt === null &&
      value.structuralHash === null && value.generatedAtRevision === null &&
      value.error === null;
  }
  if (value.state === 'running') {
    return ['analyzing', 'validating-publication'].indexOf(value.phase) >= 0 &&
      value.finishedAt === null && value.structuralHash === null &&
      value.generatedAtRevision === null && value.error === null;
  }
  if (!contract.exactInstant(value.finishedAt, true) ||
      Date.parse(value.finishedAt) < Date.parse(value.startedAt)) return false;
  if (value.state === 'succeeded') {
    return value.phase === 'completed' && HASH_RE.test(String(value.structuralHash || '')) &&
      HASH_RE.test(String(value.generatedAtRevision || '')) && value.error === null;
  }
  return value.phase === 'failed' && value.structuralHash === null &&
    value.generatedAtRevision === null && exact(value.error, ['code']) &&
    /^architecture-[a-z0-9-]{1,100}$/.test(String(value.error.code || ''));
}
function terminalInstant(startedAt) {
  var started = Date.parse(startedAt);
  return new Date(Math.max(Date.now(), Number.isFinite(started) ? started : 0))
    .toISOString();
}
function pruneReports() {
  var listed;
  try {
    listed = fileGuards.boundedDirectoryNamesUnder(
      paths.PROJECT_ROOT, JOB_DIR, JOB_SCAN_MAX
    );
  } catch (error) { return false; }
  if (!listed || !listed.ok) return false;
  var names = listed.names || [];
  if (names.some(function (name) {
    return !/^archjob-[a-f0-9]{32}\.json$/.test(name);
  })) return false;
  var reports = [];
  for (var index = 0; index < names.length; index++) {
    var jobId = names[index].slice(0, -5);
    var value = get(jobId);
    if (!value || value.id !== jobId) return false;
    reports.push({ name: names[index], value: value });
  }
  var activeCount = reports.filter(function (row) {
    return !row.value.finishedAt;
  }).length;
  var keepTerminal = Math.max(0, JOB_RETAIN - activeCount);
  var terminal = reports.filter(function (row) {
    return !!row.value.finishedAt;
  }).sort(function (left, right) {
    return Date.parse(right.value.startedAt) - Date.parse(left.value.startedAt) ||
      right.value.id.localeCompare(left.value.id);
  });
  for (var drop = keepTerminal; drop < terminal.length; drop++) {
    var row = terminal[drop];
    var removed = fileGuards.unlinkRegularFileIfUnder(
      paths.PROJECT_ROOT,
      JOB_DIR,
      path.join(JOB_DIR, row.name),
      JOB_MAX,
      function (hit) {
        var current;
        try { current = JSON.parse(hit.bytes.toString('utf8')); }
        catch (error) { return false; }
        return validPublicJob(current) && current.id === row.value.id &&
          !!current.finishedAt;
      }
    );
    if (!removed) return false;
    if (jobs[row.value.id] && jobs[row.value.id].finishedAt) {
      delete jobs[row.value.id];
    }
  }
  return true;
}
function persist(job) {
  var value = publicJob(job);
  var bytes = Buffer.from(JSON.stringify(value, null, 2) + '\n');
  var file = path.join(JOB_DIR, job.id + '.json');
  var result = fileGuards.atomicReplaceRegularFileResult(
    paths.PROJECT_ROOT, JOB_DIR, file, bytes,
    { create: true, directoryMode: 0o700, mode: 0o600, maxBytes: JOB_MAX }
  );
  if (!result || !result.ok) {
    var error = new Error('architecture job report could not be persisted');
    error.code = 'architecture-job-storage-unavailable';
    throw error;
  }
}
function notify(job) {
  try { notifier('architecture-job', publicJob(job)); } catch (ignore) {}
}
function phase(job, value) {
  job.phase = value;
  persist(job);
  notify(job);
}
function terminalFailure(job, code) {
  job.state = 'failed';
  job.phase = 'failed';
  job.finishedAt = terminalInstant(job.startedAt);
  job.error = { code: code };
  job.structuralHash = null;
  job.generatedAtRevision = null;
  try {
    persist(job);
    return true;
  } catch (error) {
    if (code !== 'architecture-job-storage-unavailable') {
      job.error = { code: 'architecture-job-storage-unavailable' };
      try {
        persist(job);
      } catch (retryError) {
        console.error('[site] architecture job report storage is unavailable');
      }
    } else {
      console.error('[site] architecture job report storage is unavailable');
    }
    return false;
  }
}
function processGroupGone(child) {
  if (!child || !child.pid) return true;
  if (process.platform === 'win32') return child.exitCode !== null || child.signalCode !== null;
  try { process.kill(-child.pid, 0); return false; }
  catch (error) { return !!(error && error.code === 'ESRCH'); }
}
function settleLease(job, callback) {
  var deadline = Date.now() + 5000;
  function attempt() {
    if (processGroupGone(job.child)) {
      clearInterval(job.renewTimer);
      job.renewTimer = null;
      var released = finalizations.endMutation(job.lease);
      job.lease = null;
      callback(released);
      return;
    }
    if (Date.now() >= deadline) {
      clearInterval(job.renewTimer);
      job.renewTimer = null;
      finalizations.retainMutation(job.lease, 'architecture generator process group did not drain after child close');
      job.lease = null;
      callback(false);
      return;
    }
    var timer = setTimeout(attempt, 50);
    if (typeof timer.unref === 'function') timer.unref();
  }
  attempt();
}
function parseResult(stdout) {
  if (Buffer.byteLength(stdout, 'utf8') > OUTPUT_MAX) return null;
  var lines = String(stdout || '').split(/\r?\n/).map(function (line) {
    return line.trim();
  }).filter(Boolean);
  if (lines.length !== 1) return null;
  var value;
  try { value = JSON.parse(lines[0]); } catch (error) { return null; }
  if (!exact(value, [
    'diffId', 'generatedAtRevision', 'path', 'status', 'structuralHash', 'version'
  ]) || value.version !== 2 || value.status !== 'published' ||
      value.path !== 'orchestrator/.arch-map.json' ||
      !HASH_RE.test(String(value.structuralHash || '')) ||
      !HASH_RE.test(String(value.generatedAtRevision || '')) ||
      !contract.ID_RE.test(String(value.diffId || ''))) return null;
  return value;
}
function finish(job, code) {
  if (job.finishedAt || job.settling) return;
  job.settling = true;
  settleLease(job, function (leaseReleased) {
    delete children[job.id];
    if (activeJobId === job.id) activeJobId = null;
    var result = parseResult(job.stdout);
    if (!leaseReleased) {
      job.state = 'failed';
      job.error = { code: 'architecture-writer-lease-lost' };
    } else if (job.error) {
      job.state = 'failed';
    } else if (code !== 0 || !result) {
      job.state = 'failed';
      job.error = { code: code === 0
        ? 'architecture-generator-output-invalid' : 'architecture-generator-failed' };
    } else {
      arch.invalidate();
      var snapshot = arch.readValidated();
      if (!snapshot.present ||
          snapshot.map.structuralHash !== result.structuralHash ||
          snapshot.map.generatedAtRevision !== result.generatedAtRevision) {
        job.state = 'failed';
        job.error = { code: 'architecture-publication-invalid' };
      } else {
        job.state = 'succeeded';
        job.error = null;
        job.structuralHash = result.structuralHash;
        job.generatedAtRevision = result.generatedAtRevision;
      }
    }
    job.phase = job.state === 'succeeded' ? 'completed' : 'failed';
    job.finishedAt = terminalInstant(job.startedAt);
    try {
      persist(job);
    } catch (reportError) {
      terminalFailure(job, 'architecture-job-storage-unavailable');
    }
    notify(job);
    if (job.state === 'succeeded') {
      try { notifier('architecture-changed', {
        structuralHash: job.structuralHash,
        generatedAtRevision: job.generatedAtRevision
      }); } catch (ignore) {}
    }
  });
}
function spawn(job) {
  job.state = 'running';
  phase(job, 'analyzing');
  var child;
  try {
    var generatorArgs = [
      GENERATOR, '--trigger', 'manual-refresh', '--trigger-id', job.id
    ];
    if (job.expectedSourceRevision !== null) {
      generatorArgs.push('--expected-source-revision', job.expectedSourceRevision);
    }
    child = childProcess.spawn(
      process.env.ARCHITECTURE_PYTHON || 'python3',
      generatorArgs,
      {
        cwd: paths.PROJECT_ROOT,
        detached: process.platform !== 'win32',
        stdio: ['ignore', 'pipe', 'pipe'],
        env: Object.assign({}, process.env, {
          PYTHONDONTWRITEBYTECODE: '1',
          ORCHESTRATOR_PROJECT_ROOT: paths.PROJECT_ROOT,
          ORCHESTRATOR_NODE_EXECUTABLE: process.execPath,
          ORCHESTRATOR_ARCHITECTURE_LEASE_ID: job.lease.leaseId,
          ORCHESTRATOR_ARCHITECTURE_LEASE_TOKEN: job.lease.token,
          ORCHESTRATOR_ARCHITECTURE_WRITER_DIR: paths.WRITER_LEASES_DIR,
          ORCHESTRATOR_ARCHITECTURE_WRITER_AUTHORITY: paths.WRITER_AUTHORITY_ROOT,
          ORCHESTRATOR_ARCHITECTURE_LEASE_VERIFIER: LEASE_VERIFIER
        })
      }
    );
  } catch (error) {
    job.error = { code: 'architecture-generator-unavailable' };
    finish(job, 1);
    return;
  }
  job.child = child;
  children[job.id] = child;
  var attached = finalizations.attachMutationChild(job.lease, child.pid);
  if (!attached.ok) {
    job.error = { code: 'architecture-writer-lease-lost' };
    try { child.kill('SIGTERM'); } catch (ignore) {}
  } else {
    job.renewTimer = setInterval(function () {
      try {
        writerLeases.renew(job.lease, TTL_MS);
      } catch (error) {
        job.error = { code: 'architecture-writer-lease-lost' };
        try { child.kill('SIGTERM'); } catch (ignore) {}
      }
    }, Math.floor(TTL_MS / 3));
    if (typeof job.renewTimer.unref === 'function') job.renewTimer.unref();
  }
  child.stdout.on('data', function (chunk) {
    job.stdoutBytes += chunk.length;
    if (job.stdoutBytes > OUTPUT_MAX) {
      job.error = { code: 'architecture-generator-output-invalid' };
      try { child.kill('SIGTERM'); } catch (ignore) {}
      return;
    }
    job.stdout += chunk.toString('utf8');
  });
  // Drain stderr, retain only a count; filesystem paths and stack traces are
  // never persisted or returned to the browser.
  child.stderr.on('data', function (chunk) {
    job.stderrBytes += chunk.length;
    if (job.stderrBytes > OUTPUT_MAX) {
      job.error = { code: 'architecture-generator-output-invalid' };
      try { child.kill('SIGTERM'); } catch (ignore) {}
    }
  });
  child.on('error', function () { finish(job, 1); });
  child.on('close', function (code) {
    try {
      phase(job, 'validating-publication');
    } catch (error) {
      job.error = { code: 'architecture-job-storage-unavailable' };
    }
    finish(job, Number.isInteger(code) ? code : 1);
  });
}
function start(request) {
  if (!exact(request, ['expectedSourceRevision', 'reason']) ||
      ['missing', 'stale', 'manual'].indexOf(request.reason) < 0 ||
      request.expectedSourceRevision !== null &&
        !HASH_RE.test(String(request.expectedSourceRevision || ''))) {
    return { ok: false, status: 400, error: 'bad-architecture-generate-request' };
  }
  var overview = arch.overview();
  if (!overview.canGenerate) {
    return { ok: false, status: 409, error: 'architecture-project-not-ready' };
  }
  if (request.reason === 'missing' && overview.present ||
      request.reason === 'stale' && (!overview.present || overview.freshness.status !== 'stale')) {
    return { ok: false, status: 409, error: 'architecture-reason-conflict' };
  }
  var expectedCurrent = overview.freshness && overview.freshness.currentRevision;
  if (!HASH_RE.test(String(expectedCurrent || ''))) {
    return { ok: false, status: 503, error: 'architecture-source-unavailable' };
  }
  if (request.expectedSourceRevision !== expectedCurrent) {
    return { ok: false, status: 409, error: 'architecture-source-conflict',
      currentSourceRevision: expectedCurrent };
  }
  if (!pruneReports()) {
    return { ok: false, status: 503, error: 'architecture-job-storage-unavailable' };
  }
  if (activeJobId && jobs[activeJobId] && !jobs[activeJobId].finishedAt) {
    var active = jobs[activeJobId];
    if (active.reason === request.reason &&
        active.expectedSourceRevision === request.expectedSourceRevision) {
      return { ok: true, status: 200, replayed: true, job: publicJob(active) };
    }
    return { ok: false, status: 409, error: 'architecture-generate-busy',
      job: publicJob(active) };
  }
  var job = {
    id: id(), state: 'queued', phase: 'acquiring-writer', reason: request.reason,
    expectedSourceRevision: request.expectedSourceRevision,
    startedAt: new Date().toISOString(), finishedAt: null,
    structuralHash: null, generatedAtRevision: null, error: null,
    stdout: '', stdoutBytes: 0, stderrBytes: 0, child: null,
    lease: null, renewTimer: null, settling: false
  };
  jobs[job.id] = job;
  activeJobId = job.id;
  try { persist(job); } catch (error) {
    activeJobId = null;
    delete jobs[job.id];
    return { ok: false, status: 503, error: 'architecture-job-storage-unavailable' };
  }
  notify(job);
  var lease = finalizations.beginMutation({
    kind: 'architecture-generate',
    key: 'architecture-generate',
    pendingChild: true,
    ttlMs: TTL_MS,
    requireSoleWriter: true
  });
  if (!lease.ok) {
    activeJobId = null;
    if (!terminalFailure(job, 'architecture-writer-unavailable')) {
      return { ok: false, status: 503, error: 'architecture-job-storage-unavailable',
        job: publicJob(job) };
    }
    notify(job);
    return { ok: false, status: 409, error: 'architecture-writer-unavailable',
      job: publicJob(job) };
  }
  job.lease = lease.handle;
  try {
    spawn(job);
  } catch (error) {
    activeJobId = null;
    finalizations.endMutation(job.lease);
    job.lease = null;
    terminalFailure(job, 'architecture-job-storage-unavailable');
    notify(job);
    return { ok: false, status: 503, error: 'architecture-job-storage-unavailable',
      job: publicJob(job) };
  }
  return { ok: true, status: 202, replayed: false, job: publicJob(job) };
}
function get(jobId) {
  if (!JOB_RE.test(String(jobId || ''))) return null;
  if (jobs[jobId]) return publicJob(jobs[jobId]);
  try {
    var file = path.join(JOB_DIR, jobId + '.json');
    var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, JOB_DIR, file, JOB_MAX);
    if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') return null;
    var value = JSON.parse(hit.bytes.toString('utf8'));
    return validPublicJob(value) && value.id === jobId ? value : null;
  } catch (error) { return null; }
}
function init(options) {
  if (options && typeof options.notify === 'function') notifier = options.notify;
  // Reports are process-owned. A queued/running report left after restart is
  // marked interrupted; the durable writer lease remains the authority for
  // any still-live detached child.
  return Promise.resolve().then(function () {
    var listed = fileGuards.boundedDirectoryNamesUnder(
      paths.PROJECT_ROOT, JOB_DIR, JOB_SCAN_MAX
    );
    if (!listed || !listed.ok) {
      throw Object.assign(new Error('architecture job report directory is unavailable'), {
        code: 'architecture-job-storage-unavailable'
      });
    }
    (listed.names || []).filter(function (name) {
      return /^archjob-[a-f0-9]{32}\.json$/.test(name);
    }).forEach(function (name) {
      var jobId = name.slice(0, -5);
      var value = get(jobId);
      if (!value || value.finishedAt ||
          ['queued', 'running'].indexOf(value.state) < 0) return;
      var recovered = {
        id: value.id, state: 'interrupted', phase: 'failed', reason: value.reason,
        expectedSourceRevision: value.expectedSourceRevision, startedAt: value.startedAt,
        finishedAt: terminalInstant(value.startedAt), structuralHash: null,
        generatedAtRevision: null, error: { code: 'architecture-job-interrupted' }
      };
      persist(recovered);
      jobs[jobId] = recovered;
    });
    if (!pruneReports()) {
      throw Object.assign(new Error('architecture job report pruning failed'), {
        code: 'architecture-job-storage-unavailable'
      });
    }
  });
}
function killAll() {
  Object.keys(children).forEach(function (jobId) {
    var child = children[jobId];
    try {
      if (process.platform !== 'win32' && child.pid) process.kill(-child.pid, 'SIGTERM');
      else child.kill('SIGTERM');
    } catch (ignore) {}
  });
}

module.exports = {
  JOB_RE: JOB_RE,
  start: start,
  get: get,
  init: init,
  killAll: killAll,
  _test: {
    publicError: publicError,
    publicJob: publicJob,
    pruneReports: pruneReports,
    terminalInstant: terminalInstant
  }
};
