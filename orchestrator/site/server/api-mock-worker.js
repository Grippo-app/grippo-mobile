#!/usr/bin/env node
'use strict';

// Fixed local mock runtime. All arguments are produced by api-mock.js; this
// worker never evaluates scripts, proxies requests, reads credentials, or
// resolves backend URLs.

var http = require('http');
var path = require('path');
var crypto = require('crypto');
var fileGuards = require('./file-guards');

var FIXTURE_MAX = 10 * 1024 * 1024;
var LOG_MAX = 4 * 1024 * 1024;
var READY_MAX = 4096;
var BODY_DRAIN_MAX = 1024 * 1024;

function fail(message) {
  try { process.stderr.write(String(message || 'mock worker failed').slice(0, 1000) + '\n'); }
  catch (ignore) {}
  process.exit(2);
}
function parseArgs(argv) {
  var fields = Object.create(null);
  if (argv.length !== 16) fail('invalid mock worker arguments');
  for (var i = 0; i < argv.length; i += 2) {
    if (!/^--(?:server-id|port|fixture|fixture-hash|log|ready|identity|root)$/.test(argv[i]) ||
        fields[argv[i]]) {
      fail('invalid mock worker arguments');
    }
    fields[argv[i]] = argv[i + 1];
  }
  if (!/^mock-[a-f0-9]{24}$/.test(fields['--server-id']) ||
      !/^(?:0|[1-9][0-9]{3,4})$/.test(fields['--port']) ||
      Number(fields['--port']) !== 0 && Number(fields['--port']) < 1024 ||
      Number(fields['--port']) > 65535 ||
      !/^sha256:[a-f0-9]{64}$/.test(fields['--fixture-hash']) ||
      !/^[a-f0-9]{64}$/.test(fields['--identity'])) fail('invalid mock worker identity');
  var root = path.resolve(fields['--root']);
  var fixture = path.resolve(fields['--fixture']);
  var log = path.resolve(fields['--log']);
  var ready = path.resolve(fields['--ready']);
  var fixtureRelative = path.relative(root, fixture);
  var logRelative = path.relative(root, log);
  var readyRelative = path.relative(root, ready);
  if (path.dirname(fixture) !== path.dirname(log) || path.dirname(fixture) !== path.dirname(ready) ||
      !fixtureRelative || fixtureRelative === '..' ||
      fixtureRelative.indexOf('..' + path.sep) === 0 || path.isAbsolute(fixtureRelative) ||
      !logRelative || logRelative === '..' ||
      logRelative.indexOf('..' + path.sep) === 0 || path.isAbsolute(logRelative) ||
      !readyRelative || readyRelative === '..' ||
      readyRelative.indexOf('..' + path.sep) === 0 || path.isAbsolute(readyRelative)) {
    fail('mock worker path escaped');
  }
  return {
    serverId: fields['--server-id'],
    port: Number(fields['--port']),
    fixture: fixture,
    fixtureHash: fields['--fixture-hash'],
    log: log,
    ready: ready,
    identity: fields['--identity'],
    root: root
  };
}
function readFixture(options) {
  var hit = fileGuards.boundedRegularFileUnder(
    options.root, path.dirname(options.fixture), options.fixture, FIXTURE_MAX
  );
  if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') fail('mock fixture unavailable');
  var actualHash = 'sha256:' + crypto.createHash('sha256').update(hit.bytes).digest('hex');
  if (actualHash !== options.fixtureHash) fail('mock fixture hash mismatch');
  var value;
  try { value = JSON.parse(hit.bytes.toString('utf8')); } catch (error) { fail('mock fixture invalid'); }
  var routeKeys = Object.create(null);
  if (!value || typeof value !== 'object' || Array.isArray(value) ||
      Object.keys(value).sort().join('\0') !== [
        'committedGenerationId', 'contractHash', 'environmentId', 'generatedAt',
        'routes', 'schemaVersion', 'serverId'
      ].sort().join('\0') ||
      value.schemaVersion !== 1 || value.serverId !== options.serverId ||
      !/^gen-[0-9]{8}T[0-9]{6}Z-[a-f0-9]{12}$/.test(String(value.committedGenerationId || '')) ||
      !/^sha256:[a-f0-9]{64}$/.test(String(value.contractHash || '')) ||
      ['local', 'dev', 'stage', 'prod'].indexOf(value.environmentId) < 0 ||
      !Number.isFinite(Date.parse(value.generatedAt)) ||
      !Array.isArray(value.routes) || value.routes.length > 5000 ||
      value.routes.some(function (row) {
        var routeKey = row && row.method + ' ' + row.path;
        var invalid = !row || typeof row !== 'object' || Array.isArray(row) ||
          Object.keys(row).sort().join('\0') !== [
            'body', 'contentType', 'generated', 'method', 'operationId',
            'path', 'status'
          ].sort().join('\0') ||
          typeof row.operationId !== 'string' || row.operationId.length < 1 ||
          row.operationId.length > 200 ||
          ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'].indexOf(row.method) < 0 ||
          typeof row.path !== 'string' || row.path.length < 1 || row.path.length > 2000 ||
          row.path.charAt(0) !== '/' || /[\x00-\x1f\x7f]/.test(row.path) ||
          !Number.isSafeInteger(row.status) || row.status < 100 || row.status > 599 ||
          typeof row.contentType !== 'string' || row.contentType.length > 100 ||
          !/^[A-Za-z0-9!#$&^_.+-]+\/[A-Za-z0-9!#$&^_.+-]+$/.test(row.contentType) ||
          row.generated !== true;
        if (!invalid && routeKeys[routeKey]) invalid = true;
        if (!invalid) routeKeys[routeKey] = 1;
        return invalid;
      })) fail('mock fixture invalid');
  return value;
}
function routePattern(template) {
  var names = [];
  var escaped = String(template).split('/').map(function (part) {
    var match = /^\{([^}/]{1,100})\}$/.exec(part);
    if (match) { names.push(match[1]); return '([^/]+)'; }
    return part.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  }).join('/');
  return { regex: new RegExp('^' + escaped + '$'), names: names };
}
function appendLog(options, row) {
  try {
    var bytes = Buffer.from(JSON.stringify(row) + '\n', 'utf8');
    if (bytes.length > 2048) return;
    fileGuards.appendBoundedRegularFileUnder(
      options.root, path.dirname(options.log), options.log, bytes,
      { create: true, directoryMode: 0o700, mode: 0o600, maxAppendBytes: 2048, maxBytes: LOG_MAX }
    );
  } catch (ignore) {}
}

var options = parseArgs(process.argv.slice(2));
var fixture = readFixture(options);
var routes = fixture.routes.map(function (row) {
  return Object.assign({}, row, { compiled: routePattern(row.path) });
});
var healthPath = '/__orchestrator_mock/' + options.serverId + '/' + options.identity;
var stopping = false;
var server = http.createServer(function (req, res) {
  var started = Date.now();
  var url;
  try { url = new URL(req.url, 'http://127.0.0.1'); }
  catch (error) { res.writeHead(400); res.end(); return; }
  if (url.pathname === healthPath && req.method === 'GET') {
    res.writeHead(200, {
      'content-type': 'application/json',
      'cache-control': 'no-store',
      'x-content-type-options': 'nosniff'
    });
    res.end(JSON.stringify({ serverId: options.serverId, identity: options.identity, pid: process.pid }));
    return;
  }
  if (url.pathname === healthPath && req.method === 'POST') {
    var receipt = Buffer.from(JSON.stringify({
      serverId: options.serverId,
      identity: options.identity,
      pid: process.pid,
      stopping: true
    }), 'utf8');
    res.writeHead(202, {
      'content-type': 'application/json',
      'content-length': receipt.length,
      'cache-control': 'no-store',
      'x-content-type-options': 'nosniff'
    });
    res.end(receipt, shutdown);
    return;
  }
  var total = 0;
  var bodyRejected = false;
  req.on('data', function (chunk) {
    total += chunk.length;
    if (total > BODY_DRAIN_MAX && !bodyRejected) {
      bodyRejected = true;
      var rejected = Buffer.from(JSON.stringify({ error: 'mock-request-body-too-large' }), 'utf8');
      res.writeHead(413, {
        'content-type': 'application/json',
        'content-length': rejected.length,
        'cache-control': 'no-store',
        'x-content-type-options': 'nosniff'
      });
      res.end(rejected);
      appendLog(options, {
        schemaVersion: 1,
        timestamp: new Date().toISOString(),
        method: /^[A-Z]{1,10}$/.test(String(req.method || '')) ? req.method : 'UNKNOWN',
        path: String(url.pathname || '/').slice(0, 1000),
        status: 413,
        durationMs: Math.max(0, Date.now() - started)
      });
      req.resume();
    }
  });
  req.on('error', function () {});
  req.on('end', function () {
    if (bodyRejected) return;
    var route = routes.find(function (candidate) {
      return candidate.method === req.method && candidate.compiled.regex.test(url.pathname);
    });
    var status = route ? route.status : 404;
    var payload = route ? route.body : { error: 'mock-route-not-found' };
    var bytes = Buffer.from(JSON.stringify(payload), 'utf8');
    res.writeHead(status, {
      'content-type': route ? route.contentType : 'application/json',
      'content-length': bytes.length,
      'cache-control': 'no-store',
      'x-content-type-options': 'nosniff'
    });
    res.end(bytes);
    appendLog(options, {
      schemaVersion: 1,
      timestamp: new Date().toISOString(),
      method: /^[A-Z]{1,10}$/.test(String(req.method || '')) ? req.method : 'UNKNOWN',
      path: String(url.pathname || '/').slice(0, 1000),
      status: status,
      durationMs: Math.max(0, Date.now() - started)
    });
  });
});
server.requestTimeout = 15000;
server.headersTimeout = 10000;
server.keepAliveTimeout = 5000;
server.maxHeadersCount = 100;
server.on('error', function (error) { fail(error && error.code || 'mock bind failed'); });
server.listen(options.port, '127.0.0.1', function () {
  var address = server.address();
  var receipt = {
    schemaVersion: 1,
    ready: true,
    serverId: options.serverId,
    identity: options.identity,
    port: address.port,
    pid: process.pid,
    startedAt: new Date().toISOString()
  };
  var bytes = Buffer.from(JSON.stringify(receipt, null, 2) + '\n', 'utf8');
  var published = fileGuards.atomicReplaceRegularFileResult(
    options.root, path.dirname(options.ready), options.ready, bytes,
    { create: true, directoryMode: 0o700, mode: 0o600, maxBytes: READY_MAX }
  );
  if (!published || !published.ok) fail('mock ready receipt unavailable');
  process.stdout.write(JSON.stringify(receipt) + '\n');
});
function shutdown() {
  if (stopping) return;
  stopping = true;
  server.close(function () { process.exit(0); });
  setTimeout(function () { process.exit(0); }, 1000).unref();
}
process.on('SIGTERM', shutdown);
process.on('SIGINT', shutdown);
