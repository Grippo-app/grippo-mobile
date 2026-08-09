#!/usr/bin/env node
'use strict';

// Independent publication fence for the Architecture generator child.
//
// The site owns and renews the shared writer lease. The Python child invokes
// this helper immediately before durable history publication and again before
// replacing the canonical map. No capability is accepted through argv: the
// exact lease id/token and guarded roots are inherited privately in env.

var path = require('path');
var os = require('os');
var writerLeases = require('./writer-leases.cjs');

var TOKEN_RE = /^[a-f0-9]{32,128}$/;
var MIN_REMAINING_MS = 10 * 1000;
var CHILD_BINDING_WAIT_MS = 5 * 1000;

function contained(root, target) {
  var relative = path.relative(root, target);
  return relative === '' ||
    (relative !== '..' && relative.slice(0, 3) !== '..' + path.sep &&
      !path.isAbsolute(relative));
}

function verify(environment, childPid, nowMs) {
  var leaseId = String(environment.ORCHESTRATOR_ARCHITECTURE_LEASE_ID || '');
  var token = String(environment.ORCHESTRATOR_ARCHITECTURE_LEASE_TOKEN || '');
  var directoryRaw = String(environment.ORCHESTRATOR_ARCHITECTURE_WRITER_DIR || '');
  var authorityRaw = String(environment.ORCHESTRATOR_ARCHITECTURE_WRITER_AUTHORITY || '');
  if (!writerLeases.LEASE_ID_RE.test(leaseId) || !TOKEN_RE.test(token) ||
      !directoryRaw || !authorityRaw || !Number.isInteger(childPid) || childPid <= 0) {
    throw new Error('architecture publication lease capability is incomplete');
  }
  var directory = path.resolve(directoryRaw);
  var authority = path.resolve(authorityRaw);
  if (!contained(authority, directory)) {
    throw new Error('architecture writer registry is outside its authority root');
  }
  var scan = writerLeases.scan(directory, authority);
  if (!scan || scan.issues.length) {
    var transition = new Error('architecture writer registry cannot prove a clean state');
    transition.code = 'ARCHITECTURE_REGISTRY_TRANSITION_PENDING';
    throw transition;
  }
  var matches = scan.active.filter(function (row) {
    return row.leaseId === leaseId && row.token === token &&
      row.kind === 'architecture-generate' &&
      row.key === 'architecture-generate' &&
      row.stem === null && row.sessionId === null &&
      row.unverified === false &&
      row.owner && row.owner.hostname === os.hostname() &&
      writerLeases.processIdentityMatches(
        row.owner.pid,
        row.owner.processStartId
      ) &&
      row.childPid === childPid &&
      row.expiresAt !== null &&
      Date.parse(row.expiresAt) >= nowMs + MIN_REMAINING_MS;
  });
  if (matches.length !== 1) {
    var own = scan.active.filter(function (row) { return row.leaseId === leaseId; })[0];
    if (own && own.token === token &&
        own.kind === 'architecture-generate' &&
        own.key === 'architecture-generate' &&
        own.stem === null && own.sessionId === null &&
        own.unverified === true && own.childPid === null &&
        Date.parse(own.expiresAt) >= nowMs + MIN_REMAINING_MS) {
      var pending = new Error('architecture generator child binding is pending');
      pending.code = 'ARCHITECTURE_CHILD_BINDING_PENDING';
      throw pending;
    }
    throw new Error('exact architecture publication lease ownership was lost');
  }
  if (scan.active.some(function (row) { return row.leaseId !== leaseId; })) {
    throw new Error('another project writer is active during architecture publication');
  }
  return {
    version: 1,
    verified: true,
    leaseId: leaseId,
    expiresAt: matches[0].expiresAt
  };
}

if (require.main === module) {
  var deadline = Date.now() + CHILD_BINDING_WAIT_MS;
  (function attempt() {
    try {
      process.stdout.write(JSON.stringify(verify(process.env, process.ppid, Date.now())) + '\n');
    } catch (error) {
      if (error && (
        error.code === 'ARCHITECTURE_CHILD_BINDING_PENDING' ||
        error.code === 'ARCHITECTURE_REGISTRY_TRANSITION_PENDING'
      ) &&
          Date.now() < deadline) {
        setTimeout(attempt, 25);
        return;
      }
      process.stderr.write('architecture publication lease verification failed\n');
      process.exitCode = 2;
    }
  })();
}

module.exports = {
  verify: verify
};
