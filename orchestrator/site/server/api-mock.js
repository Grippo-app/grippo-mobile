'use strict';

var fs = require('fs');
var path = require('path');
var crypto = require('crypto');
var cp = require('child_process');
var http = require('http');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var relations = require('./api-relations');
var catalog = require('./api-catalog');
var writerLeases = require(path.join(paths.ORCHESTRATOR_DIR, 'tasks', 'writer-leases.cjs'));

var WORKER = path.join(__dirname, 'api-mock-worker.js');
var STATE_MAX = 256 * 1024;
var READY_MAX = 4096;
var FIXTURE_MAX = 10 * 1024 * 1024;
var LOG_MAX = 4 * 1024 * 1024;
var LOG_RESPONSE_MAX = 1024 * 1024;
var INSTANCE_RETENTION = 20;
var FIXTURE_NODE_MAX = 20000;
var DEFAULT_LIMIT = 100;
var MAX_LIMIT = 500;
var KEY_RE = /^[A-Za-z0-9_.:-]{16,200}$/;
var SERVER_RE = /^mock-[a-f0-9]{24}$/;
var HASH_RE = /^sha256:[a-f0-9]{64}$/;
var cursorSecret = crypto.randomBytes(32);
var activeChild = null;
var lifecycle = Promise.resolve();

function mockEnvironment() {
  var allowed = ['PATH', 'TMPDIR', 'TMP', 'TEMP', 'LANG', 'LC_ALL', 'SYSTEMROOT', 'WINDIR'];
  var env = {};
  allowed.forEach(function (key) {
    if (typeof process.env[key] === 'string') env[key] = process.env[key];
  });
  return env;
}

function exact(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}
function sha(value) {
  return 'sha256:' + crypto.createHash('sha256')
    .update(Buffer.isBuffer(value) ? value : Buffer.from(String(value), 'utf8')).digest('hex');
}
function ensureDirectories() {
  if (!fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, paths.API_MOCK_DIR, { create: true, mode: 0o700 }) ||
      !fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, paths.API_MOCK_INSTANCES_DIR, { create: true, mode: 0o700 })) {
    throw new Error('api-mock-storage-unsafe');
  }
}
function readJson(file, maximum) {
  try {
    var hit = fileGuards.boundedRegularFileUnder(
      paths.PROJECT_ROOT, path.dirname(file), file, maximum
    );
    if (!hit) {
      var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, path.dirname(file), file);
      if (inspected && inspected.status === 'missing') return null;
      throw new Error('unsafe');
    }
    if (!hit.stat || String(hit.stat.nlink) !== '1') throw new Error('unsafe');
    return JSON.parse(hit.bytes.toString('utf8'));
  } catch (error) {
    if (error && error.code === 'ENOENT') return null;
    throw Object.assign(new Error('api-mock-state-invalid'), { code: 'api-mock-state-invalid' });
  }
}
function writeBytes(file, bytes, maximum) {
  ensureDirectories();
  if (bytes.length > maximum) throw Object.assign(new Error('api-mock-payload-too-large'), {
    code: 'api-mock-payload-too-large'
  });
  var result = fileGuards.atomicReplaceRegularFileResult(
    paths.PROJECT_ROOT, path.dirname(file), file, bytes,
    { create: true, directoryMode: 0o700, mode: 0o600, maxBytes: maximum }
  );
  if (!result || !result.ok) throw Object.assign(new Error('api-mock-write-failed'), {
    code: 'api-mock-write-failed'
  });
}
function writeJson(file, value, maximum) {
  writeBytes(file, Buffer.from(JSON.stringify(value, null, 2) + '\n', 'utf8'), maximum);
}
function validState(value) {
  if (!exact(value, [
    'committedGenerationId', 'contractHash', 'environmentId', 'identity', 'pid',
    'port', 'processStartId', 'schemaVersion', 'serverId', 'startIdempotencyKey',
    'startedAt', 'state', 'stateRevision', 'stopIdempotencyKey', 'stopReason',
    'stoppedAt'
  ])) return false;
  var unbound = (value.state === 'starting' || value.state === 'crashed') &&
    value.port === 0 && value.pid === null && value.processStartId === null;
  var bound = Number.isSafeInteger(value.port) && value.port >= 1024 &&
    value.port <= 65535 && Number.isSafeInteger(value.pid) && value.pid > 0 &&
    writerLeases.PROCESS_START_ID_RE.test(String(value.processStartId || ''));
  var lifecycleValid =
    value.state === 'starting' && value.stoppedAt === null &&
      value.stopReason === null && value.stopIdempotencyKey === null ||
    value.state === 'running' && bound && value.stoppedAt === null &&
      value.stopReason === null && value.stopIdempotencyKey === null ||
    value.state === 'stopped' && bound && value.stoppedAt !== null &&
      value.stopReason === 'user' && value.stopIdempotencyKey !== null ||
    value.state === 'crashed' && value.stoppedAt !== null &&
      value.stopReason !== null;
  return value.schemaVersion === 1 && SERVER_RE.test(String(value.serverId || '')) &&
    ['starting', 'running', 'stopped', 'crashed'].indexOf(value.state) >= 0 &&
    HASH_RE.test(String(value.stateRevision || '')) &&
    HASH_RE.test(String(value.contractHash || '')) &&
    ['local', 'dev', 'stage', 'prod'].indexOf(value.environmentId) >= 0 &&
    /^gen-[0-9]{8}T[0-9]{6}Z-[a-f0-9]{12}$/.test(String(value.committedGenerationId || '')) &&
    (unbound || bound) && lifecycleValid &&
    /^[a-f0-9]{64}$/.test(String(value.identity || '')) &&
    KEY_RE.test(String(value.startIdempotencyKey || '')) &&
    (value.stopIdempotencyKey === null || KEY_RE.test(String(value.stopIdempotencyKey || ''))) &&
    Number.isFinite(Date.parse(value.startedAt)) &&
    (value.stoppedAt === null || Number.isFinite(Date.parse(value.stoppedAt))) &&
    (value.stopReason === null || [
      'user', 'process-exited', 'identity-proof-unavailable',
      'health-proof-unavailable', 'ready-receipt-invalid',
      'start-failed', 'start-timeout'
    ].indexOf(value.stopReason) >= 0);
}
function readState() {
  ensureDirectories();
  var state = readJson(paths.API_MOCK_STATE_FILE, STATE_MAX);
  if (state === null) return null;
  if (!validState(state) || state.stateRevision !== revision(state)) {
    throw Object.assign(new Error('api-mock-state-invalid'), {
    code: 'api-mock-state-invalid'
  });
  }
  return state;
}
function readIndex() {
  var index = readJson(paths.API_MOCK_INDEX_FILE, STATE_MAX);
  if (index === null) return { schemaVersion: 1, instances: [] };
  if (!exact(index, ['instances', 'schemaVersion']) || index.schemaVersion !== 1 ||
      !Array.isArray(index.instances) || index.instances.length > INSTANCE_RETENTION ||
      index.instances.some(function (row, position) {
        return !exact(row, [
          'committedGenerationId', 'contractHash', 'environmentId', 'serverId',
          'startedAt', 'state', 'stoppedAt'
        ]) || !SERVER_RE.test(String(row.serverId || '')) ||
          index.instances.findIndex(function (item) {
            return item.serverId === row.serverId;
          }) !== position ||
          !HASH_RE.test(String(row.contractHash || '')) ||
          !/^gen-[0-9]{8}T[0-9]{6}Z-[a-f0-9]{12}$/.test(String(row.committedGenerationId || '')) ||
          ['local', 'dev', 'stage', 'prod'].indexOf(row.environmentId) < 0 ||
          ['starting', 'running', 'stopped', 'crashed'].indexOf(row.state) < 0 ||
          !Number.isFinite(Date.parse(row.startedAt)) ||
          (row.stoppedAt !== null && !Number.isFinite(Date.parse(row.stoppedAt)));
      })) {
    throw Object.assign(new Error('api-mock-index-invalid'), { code: 'api-mock-index-invalid' });
  }
  return index;
}
function revision(value) {
  var copy = Object.assign({}, value);
  delete copy.stateRevision;
  return sha(JSON.stringify(copy));
}
function cleanupInstance(serverId) {
  if (!SERVER_RE.test(String(serverId || ''))) return false;
  var files = instancePaths(serverId);
  var targets = [
    [files.fixture, FIXTURE_MAX],
    [files.log, LOG_MAX],
    [files.ready, READY_MAX]
  ];
  var names;
  try {
    names = fileGuards.boundedDirectoryNamesUnder(
      paths.PROJECT_ROOT, files.directory, 4
    );
  } catch (error) { return false; }
  if (!names || !names.ok || names.names.some(function (name) {
    return ['fixture.json', 'ready.json', 'requests.jsonl'].indexOf(name) < 0;
  })) return false;
  var observed = targets.map(function (target) {
    try {
      var hit = fileGuards.boundedRegularFileUnder(
        paths.PROJECT_ROOT, files.directory, target[0], target[1]
      );
      if (!hit) {
        var inspected = fileGuards.inspectEntryUnder(
          paths.PROJECT_ROOT, files.directory, target[0]
        );
        return inspected && inspected.status === 'missing'
          ? { target: target, missing: true } : null;
      }
      return hit.stat && String(hit.stat.nlink) === '1'
        ? { target: target, hit: hit } : null;
    } catch (error) { return null; }
  });
  if (observed.some(function (row) { return !row; })) return false;
  var safe = observed.every(function (row) {
    if (row.missing) return true;
    try {
      return fileGuards.unlinkRegularFileMatchingUnder(
          paths.PROJECT_ROOT, files.directory, row.target[0], row.target[1],
          { bytes: row.hit.bytes, proof: row.hit.stat }
        );
    } catch (error) { return false; }
  });
  if (!safe) return false;
  try {
    return fileGuards.removeEmptyDirectoryUnder(
      paths.PROJECT_ROOT, paths.API_MOCK_INSTANCES_DIR, files.directory
    );
  } catch (error) { return false; }
}
function storeState(value) {
  value.stateRevision = revision(value);
  if (!validState(value)) throw Object.assign(new Error('api-mock-state-invalid'), {
    code: 'api-mock-state-invalid'
  });
  writeJson(paths.API_MOCK_STATE_FILE, value, STATE_MAX);
  var index = readIndex();
  index.instances = index.instances.filter(function (row) {
    return row.serverId !== value.serverId;
  });
  index.instances.unshift({
    serverId: value.serverId,
    committedGenerationId: value.committedGenerationId,
    contractHash: value.contractHash,
    environmentId: value.environmentId,
    state: value.state,
    startedAt: value.startedAt,
    stoppedAt: value.stoppedAt
  });
  var evicted = index.instances.slice(INSTANCE_RETENTION);
  index.instances = index.instances.slice(0, INSTANCE_RETENTION);
  writeJson(paths.API_MOCK_INDEX_FILE, index, STATE_MAX);
  // Retention cleanup is best-effort and evidence-preserving: known files are
  // removed only when their exact bounded bytes still match. An unexpected
  // entry leaves the old directory intact instead of widening deletion scope.
  evicted.forEach(function (row) {
    if (row.serverId !== value.serverId) cleanupInstance(row.serverId);
  });
  return value;
}
function publicState(state, snapshot) {
  if (!state) return {
    state: 'stopped', serverId: null, stateRevision: null, port: null,
    url: null, committedGenerationId: null, contractHash: null,
    environmentId: null, staleContract: false, startedAt: null, stoppedAt: null,
    stopReason: null, canStop: false
  };
  return {
    state: state.state,
    serverId: state.serverId,
    stateRevision: state.stateRevision,
    port: state.port,
    url: state.state === 'running' ? 'http://127.0.0.1:' + state.port : null,
    committedGenerationId: state.committedGenerationId,
    contractHash: state.contractHash,
    environmentId: state.environmentId,
    staleContract: !!(snapshot && (snapshot.ok === false || snapshot.empty ||
      snapshot.environmentMismatch ||
      snapshot.committedGenerationId !== state.committedGenerationId ||
       snapshot.contractHash !== state.contractHash ||
       snapshot.environmentId !== state.environmentId)),
    startedAt: state.startedAt,
    stoppedAt: state.stoppedAt || null,
    stopReason: state.stopReason || null,
    canStop: state.state === 'running' ||
      state.state === 'crashed' && state.pid !== null &&
        writerLeases.PROCESS_START_ID_RE.test(String(state.processStartId || '')) &&
        processIdentityMatches(state)
  };
}
function probe(state, timeout) {
  return new Promise(function (resolve) {
    if (!state || state.state !== 'running') { resolve(false); return; }
    var settled = false;
    var request = http.get({
      host: '127.0.0.1',
      port: state.port,
      path: '/__orchestrator_mock/' + state.serverId + '/' + state.identity,
      timeout: timeout || 500
    }, function (response) {
      var chunks = [], total = 0;
      response.on('data', function (chunk) {
        total += chunk.length;
        if (total > 4096) { request.destroy(); return; }
        chunks.push(chunk);
      });
      response.on('end', function () {
        if (settled) return;
        settled = true;
        try {
          var body = JSON.parse(Buffer.concat(chunks).toString('utf8'));
          resolve(response.statusCode === 200 && body.serverId === state.serverId &&
            body.identity === state.identity && body.pid === state.pid);
        } catch (error) { resolve(false); }
      });
    });
    request.on('timeout', function () { request.destroy(); });
    request.on('error', function () { if (!settled) { settled = true; resolve(false); } });
  });
}
function requestShutdown(state, timeout) {
  return new Promise(function (resolve) {
    if (!state || !Number.isSafeInteger(state.port)) { resolve(false); return; }
    var settled = false;
    var request = http.request({
      host: '127.0.0.1',
      port: state.port,
      path: '/__orchestrator_mock/' + state.serverId + '/' + state.identity,
      method: 'POST',
      timeout: timeout || 750,
      headers: { 'content-length': '0' }
    }, function (response) {
      var chunks = [], total = 0;
      response.on('data', function (chunk) {
        total += chunk.length;
        if (total > 4096) { request.destroy(); return; }
        chunks.push(chunk);
      });
      response.on('end', function () {
        if (settled) return;
        settled = true;
        try {
          var body = JSON.parse(Buffer.concat(chunks).toString('utf8'));
          resolve(response.statusCode === 202 && body.serverId === state.serverId &&
            body.identity === state.identity && body.pid === state.pid && body.stopping === true);
        } catch (error) { resolve(false); }
      });
    });
    request.on('timeout', function () { request.destroy(); });
    request.on('error', function () {
      if (!settled) { settled = true; resolve(false); }
    });
    request.end();
  });
}
function processIdentityMatches(state) {
  try {
    return !!state && writerLeases.processIdentityState(
      state.pid, state.processStartId
    ) === 'match';
  } catch (error) { return false; }
}
function readReady(state) {
  var value = readJson(instancePaths(state.serverId).ready, READY_MAX);
  if (value === null) return null;
  return exact(value, [
    'identity', 'pid', 'port', 'ready', 'schemaVersion', 'serverId', 'startedAt'
  ]) && value.schemaVersion === 1 && value.ready === true &&
    value.serverId === state.serverId && value.identity === state.identity &&
    Number.isSafeInteger(value.pid) && value.pid > 0 &&
    Number.isSafeInteger(value.port) && value.port >= 1024 && value.port <= 65535 &&
    Number.isFinite(Date.parse(value.startedAt)) ? value : false;
}
function recoverStarting(state) {
  var ready;
  try { ready = readReady(state); }
  catch (error) { ready = false; }
  if (ready) {
    state.state = 'running';
    state.pid = ready.pid;
    state.port = ready.port;
    return probe(state, 750).then(function (alive) {
      if (alive) {
        try {
          state.processStartId = writerLeases.captureProcessStartId(state.pid);
          alive = writerLeases.PROCESS_START_ID_RE.test(String(state.processStartId || ''));
        } catch (error) { alive = false; }
      }
      if (!alive) {
        state.state = 'crashed';
        if (!state.processStartId) {
          state.pid = null;
          state.port = 0;
        }
        state.stoppedAt = new Date().toISOString();
        state.stopReason = 'identity-proof-unavailable';
      }
      storeState(state);
      return state;
    });
  }
  if (ready === false || Date.now() - Date.parse(state.startedAt) > 10000) {
    state.state = 'crashed';
    state.stoppedAt = new Date().toISOString();
    state.stopReason = ready === false ? 'ready-receipt-invalid' : 'start-timeout';
    storeState(state);
  }
  return Promise.resolve(state);
}
function fixtureCap() {
  return Object.assign(new Error('api-mock-fixture-cap'), {
    code: 'api-mock-fixture-cap'
  });
}
function safeFixtureValue(value, key, budget, depth) {
  if (budget.nodes >= 1000 || depth > 8) throw fixtureCap();
  budget.nodes++;
  if (/token|secret|password|authorization|credential|api[-_]?key|cookie|signature/i
    .test(String(key || ''))) return '[redacted]';
  if (value === null || typeof value === 'boolean' || typeof value === 'number') return value;
  if (typeof value === 'string') {
    if (/^Bearer\s+/i.test(value) || /^eyJ[A-Za-z0-9_-]{10,}\./.test(value) ||
        /(?:token|secret|password|api[_-]?key)=/i.test(value)) return '[redacted]';
    if (Array.from(value).length > 1000) throw fixtureCap();
    return value;
  }
  if (Array.isArray(value)) {
    if (value.length > 20) throw fixtureCap();
    return value.map(function (item) {
      return safeFixtureValue(item, key, budget, depth + 1);
    });
  }
  if (value && typeof value === 'object') {
    var out = Object.create(null);
    var keys = Object.keys(value).sort();
    if (keys.length > 100) throw fixtureCap();
    keys.forEach(function (childKey) {
      out[childKey] = safeFixtureValue(value[childKey], childKey, budget, depth + 1);
    });
    return out;
  }
  return null;
}
function generatedValue(modelId, schemas, seen, depth, budget) {
  if (!modelId || depth > 6 || seen[modelId]) return {};
  if (budget.nodes >= FIXTURE_NODE_MAX) throw fixtureCap();
  budget.nodes++;
  var schema = schemas && Object.prototype.hasOwnProperty.call(schemas, modelId)
    ? schemas[modelId] : null;
  if (!schema || !Array.isArray(schema.fields)) {
    throw Object.assign(new Error('api-mock-area-unavailable'), {
      code: 'api-mock-area-unavailable'
    });
  }
  if (schema.fields.length > 100) throw fixtureCap();
  seen[modelId] = 1;
  var value = Object.create(null);
  schema.fields.forEach(function (field) {
    if (budget.nodes >= FIXTURE_NODE_MAX) throw fixtureCap();
    budget.nodes++;
    var out = null;
    var fieldName = String(field.jsonName || field.name || '');
    if (/token|secret|password|authorization|credential|api[-_]?key|cookie|signature/i.test(fieldName)) {
      out = '[redacted]';
    } else if (Array.isArray(field.enum) && field.enum.length) {
      out = safeFixtureValue(field.enum[0], fieldName, { nodes: 0 }, 0);
    }
    else if (typeof field.type === 'string' && field.type.indexOf('ref:') === 0) {
      out = generatedValue(field.type.slice(4), schemas, seen, depth + 1, budget);
    } else if (field.type === 'array') {
      out = field.itemsRef
        ? [generatedValue(field.itemsRef, schemas, seen, depth + 1, budget)] : [];
    } else if (field.type === 'boolean') out = false;
    else if (field.type === 'integer' || field.type === 'number') out = 0;
    else if (field.type === 'string') out = field.format === 'uuid'
      ? '00000000-0000-0000-0000-000000000000' : 'string';
    else out = {};
    budget.bytes += Buffer.byteLength(JSON.stringify(out), 'utf8');
    if (budget.bytes > FIXTURE_MAX - (1024 * 1024)) throw fixtureCap();
    value[fieldName] = out;
  });
  delete seen[modelId];
  return value;
}
function readArea(snapshot, area) {
  var roleName = 'area:' + area;
  var role = snapshot.current.artifacts[roleName];
  var manifestRow = snapshot.current.manifest.artifacts.find(function (row) {
    return row.role === roleName;
  });
  if (!role || !manifestRow || manifestRow.size > FIXTURE_MAX) return null;
  var hit = fileGuards.boundedRegularFileUnder(
    paths.PROJECT_ROOT, path.dirname(role), role, manifestRow.size
  );
  if (!hit || !hit.stat || String(hit.stat.nlink) !== '1' ||
      hit.bytes.length !== manifestRow.size || sha(hit.bytes) !== manifestRow.hash) {
    return null;
  }
  try {
    var value = JSON.parse(hit.bytes.toString('utf8'));
    return relations.validArea(value, area) ? value : null;
  } catch (error) {
    return null;
  }
}
function buildFixture(snapshot, serverId) {
  var byArea = Object.create(null);
  var budget = { nodes: 0, bytes: 0 };
  var routes = (snapshot.inventory.endpoints || []).slice(0, 5001).map(function (endpoint) {
    if (!byArea[endpoint.area]) byArea[endpoint.area] = readArea(snapshot, endpoint.area);
    if (!byArea[endpoint.area]) {
      throw Object.assign(new Error('api-mock-area-unavailable'), {
        code: 'api-mock-area-unavailable'
      });
    }
    var schemas = byArea[endpoint.area] && byArea[endpoint.area].schemas || {};
    var statuses = Object.keys(endpoint.response || {}).sort(function (left, right) {
      var l = /^2/.test(left) ? 0 : 1, r = /^2/.test(right) ? 0 : 1;
      return l - r || left.localeCompare(right);
    });
    var selectedStatus = statuses[0] || '200';
    var response = endpoint.response && endpoint.response[selectedStatus] || null;
    var body = response && response.schemaRef
      ? generatedValue(response.schemaRef, schemas, Object.create(null), 0, budget)
      : {};
    if (response && response.array) body = [body];
    return {
      operationId: endpoint.operationId,
      method: endpoint.method,
      path: endpoint.path,
      status: /^\d{3}$/.test(selectedStatus) ? Number(selectedStatus) : 200,
      contentType: response && response.contentType || 'application/json',
      generated: true,
      body: body
    };
  });
  if (routes.length > 5000) throw Object.assign(new Error('api-mock-route-cap'), {
    code: 'api-mock-route-cap'
  });
  return {
    schemaVersion: 1,
    serverId: serverId,
    committedGenerationId: snapshot.committedGenerationId,
    contractHash: snapshot.contractHash,
    environmentId: snapshot.environmentId,
    generatedAt: new Date().toISOString(),
    routes: routes
  };
}
function instancePaths(serverId) {
  var directory = path.join(paths.API_MOCK_INSTANCES_DIR, serverId);
  return {
    directory: directory,
    fixture: path.join(directory, 'fixture.json'),
    log: path.join(directory, 'requests.jsonl'),
    ready: path.join(directory, 'ready.json')
  };
}
function spawnWorker(state, fixtureFile, fixtureHash, logFile, requestedPort) {
  return new Promise(function (resolve, reject) {
    var args = [
      WORKER,
      '--server-id', state.serverId,
      '--port', String(requestedPort),
      '--fixture', fixtureFile,
      '--fixture-hash', fixtureHash,
      '--log', logFile,
      '--ready', instancePaths(state.serverId).ready,
      '--identity', state.identity,
      '--root', paths.PROJECT_ROOT
    ];
    var child = cp.spawn(process.execPath, args, {
      cwd: paths.PROJECT_ROOT,
      env: mockEnvironment(),
      stdio: ['ignore', 'pipe', 'pipe'],
      shell: false,
      detached: process.platform !== 'win32'
    });
    activeChild = child;
    var settled = false, stdout = Buffer.alloc(0), stderr = Buffer.alloc(0);
    var timer = setTimeout(function () {
      if (settled) return;
      settled = true;
      try { child.kill('SIGTERM'); } catch (ignore) {}
      reject(Object.assign(new Error('api-mock-start-timeout'), { code: 'api-mock-start-timeout' }));
    }, 5000);
    child.stdout.on('data', function (chunk) {
      stdout = Buffer.concat([stdout, chunk]);
      if (stdout.length > 8192) {
        child.kill('SIGTERM');
        if (!settled) {
          settled = true; clearTimeout(timer);
          reject(Object.assign(new Error('api-mock-protocol-invalid'), { code: 'api-mock-protocol-invalid' }));
        }
        return;
      }
      var newline = stdout.indexOf(10);
      if (newline < 0 || settled) return;
      var ready;
      try { ready = JSON.parse(stdout.subarray(0, newline).toString('utf8')); } catch (error) {}
      if (!exact(ready, [
        'identity', 'pid', 'port', 'ready', 'schemaVersion', 'serverId', 'startedAt'
      ]) || ready.schemaVersion !== 1 || ready.ready !== true ||
          ready.serverId !== state.serverId || ready.identity !== state.identity ||
          ready.pid !== child.pid || !Number.isSafeInteger(ready.port) ||
          ready.port < 1024 || ready.port > 65535 ||
          !Number.isFinite(Date.parse(ready.startedAt))) {
        settled = true; clearTimeout(timer); child.kill('SIGTERM');
        reject(Object.assign(new Error('api-mock-protocol-invalid'), { code: 'api-mock-protocol-invalid' }));
        return;
      }
      var processStartId;
      try { processStartId = writerLeases.captureProcessStartId(child.pid); }
      catch (error2) {}
      if (!writerLeases.PROCESS_START_ID_RE.test(String(processStartId || ''))) {
        settled = true; clearTimeout(timer); child.kill('SIGTERM');
        reject(Object.assign(new Error('api-mock-process-identity-unavailable'), {
          code: 'api-mock-process-identity-unavailable'
        }));
        return;
      }
      settled = true; clearTimeout(timer);
      child.unref();
      if (child.stdout && typeof child.stdout.unref === 'function') child.stdout.unref();
      if (child.stderr && typeof child.stderr.unref === 'function') child.stderr.unref();
      resolve({
        child: child, port: ready.port, pid: ready.pid,
        processStartId: processStartId
      });
    });
    child.stderr.on('data', function (chunk) {
      if (stderr.length < 8192) stderr = Buffer.concat([stderr, chunk]).subarray(0, 8192);
    });
    child.on('error', function (error) {
      if (!settled) {
        settled = true; clearTimeout(timer);
        reject(Object.assign(error, { code: 'api-mock-start-failed' }));
      }
    });
    child.on('exit', function () {
      if (!settled) {
        settled = true; clearTimeout(timer);
        reject(Object.assign(new Error(stderr.toString('utf8') || 'api-mock-start-failed'), {
          code: 'api-mock-start-failed'
        }));
      }
      if (activeChild === child) activeChild = null;
      serialized(function () {
        try {
          var current = readState();
          if (current && current.serverId === state.serverId &&
              (current.state === 'starting' ||
                current.state === 'running' && current.pid === child.pid)) {
            current.state = 'crashed';
            current.stoppedAt = new Date().toISOString();
            current.stopReason = 'process-exited';
            storeState(current);
          }
        } catch (ignore) {}
        return true;
      }).catch(function () {});
    });
  });
}
function meta(snapshot) {
  return snapshot && snapshot.ok ? relations.meta(snapshot) : {
    schemaVersion: 1, committedGenerationId: null, contractHash: null,
    environmentId: null, projectCodeRevision: null, limitations: ['contract-generation-invalid']
  };
}
function stateInvalid(snapshot) {
  return Object.assign(meta(snapshot), {
    ok: false, status: 409, error: 'api-mock-state-invalid'
  });
}
function statusImpl() {
  var snapshot = relations.snapshot();
  var state;
  try { state = readState(); }
  catch (error) { return Promise.resolve(stateInvalid(snapshot)); }
  if (state && state.state === 'starting') {
    return recoverStarting(state).then(function (recovered) {
      return Object.assign(meta(snapshot), {
        ok: true, status: 200, mock: publicState(recovered, snapshot)
      });
    }, function () { return stateInvalid(snapshot); });
  }
  if (!state || state.state !== 'running') return Promise.resolve(Object.assign(meta(snapshot), {
    ok: true, status: 200, mock: publicState(state, snapshot)
  }));
  if (!processIdentityMatches(state)) {
    state.state = 'crashed';
    state.stoppedAt = new Date().toISOString();
    state.stopReason = 'identity-proof-unavailable';
    try { storeState(state); }
    catch (error) { return Promise.resolve(stateInvalid(snapshot)); }
    return Promise.resolve(Object.assign(meta(snapshot), {
      ok: true, status: 200, mock: publicState(state, snapshot)
    }));
  }
  return probe(state, 500).then(function (alive) {
    if (!alive) {
      state.state = 'crashed';
      state.stoppedAt = new Date().toISOString();
      state.stopReason = 'health-proof-unavailable';
      try { storeState(state); }
      catch (error) { return stateInvalid(snapshot); }
    }
    return Object.assign(meta(snapshot), {
      ok: true, status: 200, mock: publicState(state, snapshot)
    });
  });
}
function startImpl(request) {
  if (!exact(request, [
    'contractHash', 'environmentId', 'expectedGenerationId',
    'idempotencyKey', 'port', 'portMode'
  ]) || !HASH_RE.test(String(request.contractHash || '')) ||
      !/^gen-[0-9]{8}T[0-9]{6}Z-[a-f0-9]{12}$/.test(String(request.expectedGenerationId || '')) ||
      !['local', 'dev', 'stage', 'prod'].includes(request.environmentId) ||
      !['auto', 'explicit'].includes(request.portMode) ||
      !KEY_RE.test(String(request.idempotencyKey || '')) ||
      (request.portMode === 'auto' ? request.port !== null :
        !Number.isSafeInteger(request.port) || request.port < 1024 || request.port > 65535)) {
    return Promise.resolve({ ok: false, status: 400, error: 'bad-api-mock-start-request' });
  }
  var snapshot = relations.snapshot();
  if (!snapshot.ok) return Promise.resolve(snapshot);
  if (snapshot.empty || snapshot.committedGenerationId !== request.expectedGenerationId ||
      snapshot.contractHash !== request.contractHash ||
      snapshot.environmentId !== request.environmentId) {
    return Promise.resolve(Object.assign(meta(snapshot), {
      ok: false, status: 409, error: 'api-generation-conflict'
    }));
  }
  if (snapshot.environmentMismatch) return Promise.resolve(Object.assign(meta(snapshot), {
    ok: false, status: 409, error: 'environment-mismatch'
  }));
  var existing;
  try { existing = readState(); }
  catch (error) { return Promise.resolve(stateInvalid(snapshot)); }
  if (existing && existing.state === 'starting') {
    return recoverStarting(existing).then(function (recovered) {
      if (recovered.state === 'crashed') return startImpl(request);
      return Object.assign(meta(snapshot), {
        ok: false, status: 409, error: 'api-mock-starting',
        mock: publicState(recovered, snapshot)
      });
    }, function () { return stateInvalid(snapshot); });
  }
  if (existing && existing.state === 'running') {
    if (!processIdentityMatches(existing)) {
      existing.state = 'crashed';
      existing.stoppedAt = new Date().toISOString();
      existing.stopReason = 'identity-proof-unavailable';
      try { storeState(existing); }
      catch (identityError) { return Promise.resolve(stateInvalid(snapshot)); }
      return startImpl(request);
    }
    return probe(existing, 750).then(function (alive) {
      if (alive) {
        if (existing.startIdempotencyKey === request.idempotencyKey &&
            existing.committedGenerationId === request.expectedGenerationId &&
            existing.contractHash === request.contractHash &&
            existing.environmentId === request.environmentId) return statusImpl();
        return Object.assign(meta(snapshot), {
          ok: false, status: 409, error: 'api-mock-already-running',
          mock: publicState(existing, snapshot)
        });
      }
      existing.state = 'crashed';
      existing.stoppedAt = new Date().toISOString();
      existing.stopReason = 'health-proof-unavailable';
      try { storeState(existing); }
      catch (error2) { return stateInvalid(snapshot); }
      return startImpl(request);
    });
  }
  if (existing && existing.state === 'crashed' && processIdentityMatches(existing)) {
    return Promise.resolve(Object.assign(meta(snapshot), {
      ok: false, status: 409, error: 'api-mock-process-still-running',
      mock: publicState(existing, snapshot)
    }));
  }
  var serverId = 'mock-' + crypto.randomBytes(12).toString('hex');
  var files = instancePaths(serverId);
  if (!fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, files.directory, { create: true, mode: 0o700 })) {
    return Promise.resolve(Object.assign(meta(snapshot), {
      ok: false, status: 503, error: 'api-mock-storage-unsafe'
    }));
  }
  var state = {
    schemaVersion: 1,
    serverId: serverId,
    state: 'starting',
    stateRevision: null,
    committedGenerationId: snapshot.committedGenerationId,
    contractHash: snapshot.contractHash,
    environmentId: snapshot.environmentId,
    port: 0,
    pid: null,
    processStartId: null,
    identity: crypto.randomBytes(32).toString('hex'),
    startedAt: new Date().toISOString(),
    stoppedAt: null,
    stopReason: null,
    startIdempotencyKey: request.idempotencyKey,
    stopIdempotencyKey: null
  };
  var fixture;
  var fixtureBytes;
  try {
    fixture = buildFixture(snapshot, serverId);
    fixtureBytes = Buffer.from(JSON.stringify(fixture, null, 2) + '\n', 'utf8');
    writeBytes(files.fixture, fixtureBytes, FIXTURE_MAX);
    storeState(state);
  } catch (error2) {
    var persisted = false;
    try {
      var current = readState();
      persisted = !!(current && current.serverId === serverId);
    } catch (ignore) {}
    if (!persisted) cleanupInstance(serverId);
    var knownError = error2 && [
      'api-mock-area-unavailable', 'api-mock-fixture-cap',
      'api-mock-payload-too-large', 'api-mock-route-cap',
      'api-mock-storage-unsafe'
    ].indexOf(error2.code) >= 0 ? error2.code : 'api-mock-start-failed';
    return Promise.resolve(Object.assign(meta(snapshot), {
      ok: false,
      status: knownError === 'api-mock-storage-unsafe' ? 503 : 409,
      error: knownError
    }));
  }
  return spawnWorker(
    state, files.fixture, sha(fixtureBytes), files.log,
    request.portMode === 'auto' ? 0 : request.port
  )
    .then(function (ready) {
      state.port = ready.port;
      state.pid = ready.pid;
      state.processStartId = ready.processStartId;
      state.state = 'running';
      try { storeState(state); }
      catch (error) {
        try { ready.child.kill('SIGTERM'); } catch (ignore) {}
        throw error;
      }
      return Object.assign(meta(snapshot), {
        ok: true, status: 201, mock: publicState(state, snapshot)
      });
    }).catch(function (error) {
      try {
        state.state = 'crashed';
        state.stoppedAt = new Date().toISOString();
        state.stopReason = 'start-failed';
        storeState(state);
      } catch (ignore) {}
      return Object.assign(meta(snapshot), {
        ok: false, status: 503, error: 'api-mock-start-failed'
      });
    });
}
function waitUntilGone(state, attempts) {
  return probe(state, 150).then(function (alive) {
    var identityGone = false;
    try {
      identityGone = writerLeases.processIdentityState(
        state.pid, state.processStartId
      ) !== 'match';
    } catch (error) {}
    if (!alive && identityGone) return true;
    if (attempts <= 1) return false;
    return new Promise(function (resolve) {
      setTimeout(resolve, 75);
    }).then(function () { return waitUntilGone(state, attempts - 1); });
  });
}
function stopImpl(request) {
  if (!exact(request, ['expectedStateRevision', 'idempotencyKey', 'serverId']) ||
      !SERVER_RE.test(String(request.serverId || '')) ||
      !HASH_RE.test(String(request.expectedStateRevision || '')) ||
      !KEY_RE.test(String(request.idempotencyKey || ''))) {
    return Promise.resolve({ ok: false, status: 400, error: 'bad-api-mock-stop-request' });
  }
  var snapshot = relations.snapshot(), state;
  try { state = readState(); }
  catch (error) {
    return Promise.resolve(Object.assign(meta(snapshot), {
      ok: false, status: 409, error: 'api-mock-state-invalid'
    }));
  }
  if (!state || state.serverId !== request.serverId) return Promise.resolve(Object.assign(meta(snapshot), {
    ok: false, status: 404, error: 'api-mock-not-found'
  }));
  var stoppableCrash = state.state === 'crashed' &&
    state.pid !== null && processIdentityMatches(state);
  if (state.state !== 'running' && !stoppableCrash) {
    if (state.stopIdempotencyKey === request.idempotencyKey) return Promise.resolve(Object.assign(meta(snapshot), {
      ok: true, status: 200, mock: publicState(state, snapshot)
    }));
    return Promise.resolve(Object.assign(meta(snapshot), {
      ok: false, status: 409, error: 'api-mock-not-running'
    }));
  }
  if (state.stateRevision !== request.expectedStateRevision) return Promise.resolve(Object.assign(meta(snapshot), {
    ok: false, status: 409, error: 'api-mock-state-conflict',
    mock: publicState(state, snapshot)
  }));
  return Promise.resolve(processIdentityMatches(state)).then(function (proved) {
    if (!proved) {
      state.state = 'crashed';
      state.stoppedAt = new Date().toISOString();
      state.stopReason = 'identity-proof-unavailable';
      state.stopIdempotencyKey = request.idempotencyKey;
      storeState(state);
      return Object.assign(meta(snapshot), {
        ok: false, status: 409, error: 'api-mock-stop-failed',
        mock: publicState(state, snapshot)
      });
    } else {
      return requestShutdown(state, 750).then(function (accepted) {
        if (!accepted) {
          return Object.assign(meta(snapshot), {
            ok: false, status: 409, error: 'api-mock-stop-failed',
            mock: publicState(state, snapshot)
          });
        }
        return waitUntilGone(state, 24).then(function (gone) {
          if (!gone) {
            return Object.assign(meta(snapshot), {
              ok: false, status: 503, error: 'api-mock-stop-timeout'
          });
        }
        state.state = 'stopped';
        state.stoppedAt = new Date().toISOString();
        state.stopReason = 'user';
        state.stopIdempotencyKey = request.idempotencyKey;
        storeState(state);
          return Object.assign(meta(snapshot), {
            ok: true, status: 200, mock: publicState(state, snapshot)
          });
        });
      });
    }
  }).catch(function () {
    return Object.assign(meta(snapshot), {
      ok: false, status: 503, error: 'api-mock-stop-failed'
    });
  });
}
function serialized(work) {
  var guarded = function () {
    var handle;
    try {
      writerLeases.reconcileStaleMutations(
        paths.WRITER_LEASES_DIR, paths.WRITER_AUTHORITY_ROOT
      );
      handle = writerLeases.acquire(paths.WRITER_LEASES_DIR, {
        kind: 'api-mock-lifecycle',
        key: 'api-mock:lifecycle',
        ownerPid: process.pid,
        pendingChild: false,
        ttlMs: 30000,
        rootDir: paths.WRITER_AUTHORITY_ROOT
      });
      var scan = writerLeases.scan(paths.WRITER_LEASES_DIR, paths.WRITER_AUTHORITY_ROOT);
      var conflict = scan.issues.length || scan.stale.length ||
        scan.active.some(function (row) {
        return row.leaseId !== handle.leaseId;
      });
      if (conflict) {
        writerLeases.release(handle);
        return { ok: false, status: 409, error: 'api-mock-lifecycle-busy' };
      }
    } catch (error) {
      if (handle) try { writerLeases.release(handle); } catch (ignore) {}
      return { ok: false, status: 409, error: 'api-mock-lifecycle-busy' };
    }
    return Promise.resolve().then(work).finally(function () {
      try { writerLeases.release(handle); } catch (ignore) {}
    });
  };
  var next = lifecycle.then(guarded, guarded);
  lifecycle = next.catch(function () {});
  return next;
}
function start(request) {
  return serialized(function () { return startImpl(request); });
}
function stop(request) {
  return serialized(function () { return stopImpl(request); });
}
function status() {
  return serialized(statusImpl);
}
function cursorSignature(body) {
  return crypto.createHmac('sha256', cursorSecret).update(body).digest('hex');
}
function encodeCursor(value) {
  var body = Buffer.from(JSON.stringify(value)).toString('base64url');
  return body + '.' + cursorSignature(body);
}
function decodeCursor(value) {
  var match = /^([A-Za-z0-9_-]{1,1500})\.([a-f0-9]{64})$/.exec(String(value || ''));
  if (!match) return null;
  var expected = cursorSignature(match[1]);
  if (!crypto.timingSafeEqual(Buffer.from(expected), Buffer.from(match[2]))) return null;
  try { return JSON.parse(Buffer.from(match[1], 'base64url').toString('utf8')); }
  catch (error) { return null; }
}
function logs(params) {
  params = params || {};
  if (Object.keys(params).some(function (key) {
    return ['cursor', 'limit', 'serverId'].indexOf(key) < 0;
  }) || !SERVER_RE.test(String(params.serverId || '')) ||
      params.limit && (!/^[1-9][0-9]{0,2}$/.test(String(params.limit)) ||
        Number(params.limit) > MAX_LIMIT)) {
    return { ok: false, status: 400, error: 'bad-api-mock-logs-query' };
  }
  var limit = params.limit ? Number(params.limit) : DEFAULT_LIMIT;
  var index;
  try { index = readIndex(); }
  catch (error) { return { ok: false, status: 409, error: 'api-mock-index-invalid' }; }
  var current;
  try { current = readState(); } catch (ignore) { current = null; }
  var instance = index.instances.find(function (row) {
    return row.serverId === params.serverId;
  }) || (current && current.serverId === params.serverId ? current : null);
  if (!instance) {
    return { ok: false, status: 404, error: 'api-mock-not-found' };
  }
  var files = instancePaths(params.serverId), hit;
  try {
    hit = fileGuards.boundedRegularFileUnder(
      paths.PROJECT_ROOT, files.directory, files.log, LOG_MAX
    );
    if (hit && (!hit.stat || String(hit.stat.nlink) !== '1')) {
      return { ok: false, status: 409, error: 'api-mock-log-invalid' };
    }
    if (!hit) {
      var inspected = fileGuards.inspectEntryUnder(
        paths.PROJECT_ROOT, files.directory, files.log
      );
      if (!inspected || inspected.status !== 'missing') {
        return { ok: false, status: 409, error: 'api-mock-log-invalid' };
      }
    }
  } catch (error) {
    if (error && error.code === 'ENOENT') hit = null;
    else return { ok: false, status: 409, error: 'api-mock-log-invalid' };
  }
  var bytes = hit ? hit.bytes : Buffer.alloc(0);
  var logHash = sha(bytes);
  var invalidRow = false;
  var rows = bytes.toString('utf8').split('\n').filter(Boolean).map(function (line) {
    try {
      var row = JSON.parse(line);
      if (!exact(row, [
        'durationMs', 'method', 'path', 'schemaVersion', 'status', 'timestamp'
      ]) || row.schemaVersion !== 1 ||
        !Number.isFinite(Date.parse(row.timestamp)) ||
        typeof row.path !== 'string' || row.path.length < 1 || row.path.length > 1000 ||
        row.path.charAt(0) !== '/' || /[\x00-\x1f\x7f]/.test(row.path) ||
        !/^[A-Z]{1,10}$/.test(row.method) || !Number.isSafeInteger(row.status) ||
        row.status < 100 || row.status > 599 ||
        !Number.isSafeInteger(row.durationMs) || row.durationMs < 0 ||
        row.durationMs > 86400000) {
        invalidRow = true;
        return null;
      }
      return {
        schemaVersion: 1,
        timestamp: row.timestamp,
        method: row.method,
        path: row.path,
        status: row.status,
        durationMs: row.durationMs
      };
    } catch (error) {
      invalidRow = true;
      return null;
    }
  }).filter(Boolean);
  if (invalidRow) return { ok: false, status: 409, error: 'api-mock-log-invalid' };
  var offset = 0;
  if (params.cursor) {
    var cursor = decodeCursor(params.cursor);
    if (!cursor || cursor.serverId !== params.serverId || cursor.logHash !== logHash ||
        !Number.isSafeInteger(cursor.offset) || cursor.offset < 0 || cursor.offset > rows.length) {
      return { ok: false, status: 409, error: 'api-mock-cursor-invalid' };
    }
    offset = cursor.offset;
  }
  var items = [], index = offset;
  while (index < rows.length && items.length < limit) {
    if (Buffer.byteLength(JSON.stringify(items.concat([rows[index]])), 'utf8') >
        LOG_RESPONSE_MAX - 8192) break;
    items.push(rows[index++]);
  }
  return {
    ok: true,
    status: 200,
    schemaVersion: 1,
    serverId: params.serverId,
    committedGenerationId: instance.committedGenerationId,
    contractHash: instance.contractHash,
    environmentId: instance.environmentId,
    items: items,
    page: {
      returned: items.length,
      total: rows.length,
      nextCursor: index < rows.length ? encodeCursor({
        serverId: params.serverId, logHash: logHash, offset: index
      }) : null,
      responseTruncated: index < rows.length && items.length < limit
    },
    limitations: bytes.length >= LOG_MAX - 2048 ? ['api-mock-log-cap'] : []
  };
}
function init() {
  try { ensureDirectories(); }
  catch (error) { return Promise.resolve(false); }
  return status().then(function (result) {
    return !!(result && result.ok);
  });
}
function killAll() {
  try {
    var state = readState();
    if (!state || ['starting', 'running', 'crashed'].indexOf(state.state) < 0) return;
    // Shutdown is allowed only for a child still owned by this process. An
    // adopted durable worker intentionally survives a Site restart.
    if (activeChild && (state.pid === null || activeChild.pid === state.pid)) {
      activeChild.kill('SIGTERM');
    }
  } catch (ignore) {}
}

module.exports = {
  init: init,
  status: status,
  start: start,
  stop: stop,
  logs: logs,
  killAll: killAll,
  _test: {
    buildFixture: buildFixture,
    publicState: publicState,
    readArea: readArea,
    safeFixtureValue: safeFixtureValue,
    generatedValue: generatedValue
  }
};
