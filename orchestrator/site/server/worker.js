'use strict';

// ---------------------------------------------------------------------------
// Worker liveness (heartbeat). The standby /loop session (serve-queue.md)
// writes orchestrator/.cache/tasks/worker/heartbeat.json at the start of every pass:
//
//   { "at": "<ISO time>", "passTokenHash": "sha256:<64 hex>",
//     "projectRoot": "<abs path>", "state": "ready|claimed", "version": 1 }
//
// The raw pass token is returned only by begin-pass. This file contains its
// one-way hash; claim-next atomically replaces the ready generation before it
// receives queue authority. The server deliberately exposes only `at` and
// `projectRoot`, never the pass-token hash or internal state.
//
// This is the one honest "is a drainer attached" signal the site has — the
// process itself is invisible (it runs in the desktop app's sandboxed VM).
// The server only reads + exposes the absolute `at` timestamp; the client
// (scripts/status.js) decides online/stale against its own clock, the same
// way it derives every other relative time — so the SSE change-hash never
// churns on wall-clock alone (the timestamp is second-granular and only moves
// when the worker actually writes).
//
// During a long task the loop is deep in a skill-driven Claude run and stops
// heart-beating; the client covers that gap with the active-lock signal ("busy"), so a
// stale heartbeat mid-task does NOT read as offline.
// ---------------------------------------------------------------------------

var fs     = require('fs');
var paths  = require('./paths');
var fsutil = require('./fsutil');

var HEARTBEAT_FILE = paths.HEARTBEAT_FILE;

// A heartbeat is a tiny JSON object; anything larger is treated as corrupt and
// ignored (defensive — the file is machine-written, never user-supplied).
var HEARTBEAT_MAX_BYTES = 4096;

// Returns { at, projectRoot } | null. Null when the file is absent, unreadable,
// oversized, unparseable, missing a valid `at`, or stamped for a DIFFERENT
// project root than the one this server serves (a heartbeat that doesn't
// belong here must never read as "our worker is online").
function readHeartbeat() {
  var raw = fsutil.readUtf8(HEARTBEAT_FILE);
  if (raw === null || raw === undefined) return null;
  if (raw.length > HEARTBEAT_MAX_BYTES) return null;
  var parsed;
  try { parsed = JSON.parse(raw); } catch (e) { return null; }
  if (!parsed || typeof parsed !== 'object') return null;

  var at = typeof parsed.at === 'string' ? parsed.at : null;
  if (!at || isNaN(Date.parse(at))) return null;

  var projectRoot = typeof parsed.projectRoot === 'string' ? parsed.projectRoot : null;
  // Defensive cross-project guard. The heartbeat lives under THIS project's
  // .cache/tasks/worker/, so a mismatch should never happen — but if it does (a
  // copied tree, a misconfigured loop), refuse to report it as our worker.
  if (projectRoot && projectRoot !== paths.PROJECT_ROOT) return null;

  return { at: at, projectRoot: projectRoot };
}

module.exports = {
  readHeartbeat: readHeartbeat
};
