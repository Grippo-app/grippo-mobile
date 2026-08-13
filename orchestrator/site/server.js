#!/usr/bin/env node
/*
 * Orchestrator site — Node zero-dep dev server (entry point).
 *
 * Single entry point for the orchestrator/site/ UI, providing:
 *   - static file serving for everything under orchestrator/ (server/static.js)
 *   - GET  /api/state          — derived snapshot (server/state.js)
 *   - GET  /api/events         — Server-Sent Events stream (server/sse.js)
 *   - POST /api/state-patch    — merge partial fields into .cache/site/.site-state.json
 *   - POST /api/reset          — clear .cache/site/.site-state.json
 *   - POST /api/tasks/actions  — submit a typed task action; prompts stay server-owned
 *   - POST /api/finalizations/resume — resume a durable task publication
 *   - GET/POST /api/reviewer/* — canonical Reviewer status, settings and activity
 *   - GET  /api/session/*      — generic interactive sessions (Setup/Wizard/Board terminals)
 *
 * CLI runner (server/runner.js): when the `claude` CLI is on PATH, the server
 * drains the request queue via interactive stream-json Claude CLI sessions
 * (`claude -p --input-format stream-json --output-format stream-json …` with
 * stdin turns — see server/sessions.js), serially, so tasks run without a
 * human keeping a Claude session open. Concurrency is frozen at one task at a
 * time (MAX_PARALLEL=1 in server/runner.js) until per-task worktree isolation
 * lands; there is no environment override. Env:
 *   RUNNER_DISABLED=1        — turn the runner off (fall back to the /loop worker)
 *
 * Run from the project root (the parent of orchestrator/):
 *
 *   node orchestrator/site/server.js
 *
 * Then open http://localhost:8000/site/.
 *
 * This file is intentionally thin: all behaviour lives in CommonJS
 * modules under orchestrator/site/server/:
 *   api-contract, arch, backend-credentials, backend-environments, backend-integration, backlog-create,
 *   child-env, cli, contract-generation, contract-history, contract-job,
 *   contract-session-actions, creation-markers, design-catalog, design-comparison,
 *   design-component-compare, design-component-mappings, design-component-state, design-history,
 *   design-mappings, design-overview, design-preview, design-relations, design-task-actions,
 *   design-token-compare, design-token-state, edit-markers, figma,
 *   figma-evidence, figma-generation, figma-integration, figma-screens,
 *   figma-session-actions, figma-sync, figma-sync-history, figma-task-publication, figma-test-job,
 *   file-guard-worker,
 *   file-guards, finalizations, fsutil, git, http, locks, paths, persistence, pixel-review,
 *   project-config, project-config-update, requests, reviewer, reviewer-activity, api-report-state,
 *   reviewer-detector, runner, runtime-integrity, sessions, shallow-intake,
 *   shallow-intake-contract, shallow-owner-guard, skills, sse, startup-recovery, state, static, status,
 *   task-integrity, task-source, tasks-log, timing, validators, windows-runtime-proof, worker,
 *   writer-lease-inspector
 * Each is require/module.exports only — no ESM, no package.json.
 * Full /api/* route surface: see server/http.js.
 */

'use strict';

var http  = require('http');
var paths = require('./server/paths');
var persistence = require('./server/persistence');
var httpMod = require('./server/http');
var sse   = require('./server/sse');
var runner = require('./server/runner');
var cli   = require('./server/cli');
var figma = require('./server/figma');
var figmaTestJob = require('./server/figma-test-job');
var figmaSync = require('./server/figma-sync');
var figmaTaskPublication = require('./server/figma-task-publication');
var designTokenCompare = require('./server/design-token-compare');
var designTokenSources = require('./server/design-token-sources');
var designComponentCompare = require('./server/design-component-compare');
var sessions = require('./server/sessions');
var finalizations = require('./server/finalizations');
var backlogCreate = require('./server/backlog-create');
var shallowIntake = require('./server/shallow-intake');
var contractJob = require('./server/contract-job');
var architectureGeneration = require('./server/architecture-generation');
var taskIntegrity = require('./server/task-integrity');
var figmaFeatureGate = require('./server/figma-feature-gate');
var startupRecovery = require('./server/startup-recovery');
var appRunner = require('./server/app-runner');
var apiMock = require('./server/api-mock');

var publicationRecoveryController = null;
var startupFigmaGate = figmaFeatureGate.startup();
var figmaEnabled = startupFigmaGate.enabled;
if (!startupFigmaGate.valid) {
  console.error('[site] FIGMA_CONFIG_INVALID: canonical figmaEnabled: true|false could not be read; Figma startup remains fail-closed.');
}

try {
  persistence.readPersisted();
} catch (stateValidationError) {
  console.error('[site] persisted state validation failed:',
    String(stateValidationError && stateValidationError.message || stateValidationError));
  process.exit(1);
}

var PORT = parseInt(process.env.PORT || '8000', 10);
if (!Number.isFinite(PORT) || PORT < 0 || PORT > 65535) {
  console.error('Invalid PORT: ' + process.env.PORT);
  process.exit(2);
}

var server = http.createServer(httpMod.handle);

server.on('error', function (e) {
  if (e.code === 'EADDRINUSE') {
    console.error('[site] port ' + PORT + ' is already in use. Refusing to start a second implicit server; stop the existing server or choose an explicit PORT.');
    process.exit(3);
    return;
  }
  throw e;
});

server.listen(PORT, '127.0.0.1', function () {
  var latestRecoveryOutcome = null;
  var startupConsumersReady = false;
  var lastStartupBarrierWarning = null;
  startupRecovery.configure({
    verify: function () {
      var result = taskIntegrity.validateAll('startup');
      var findings = Array.isArray(result && result.findings) ? result.findings.filter(function (row) {
        return row && (row.severity === 'blocker' || row.severity === 'error');
      }) : [];
      return {
        // The runner performs a fresh, task-scoped admission before every
        // claim. Local task content and INDEX drift belong in diagnostics and
        // must not prevent unrelated queued tasks from reaching that fence.
        ok: !!(result && result._model),
        reasonCode: 'startup-integrity-unavailable',
        findingCount: findings.length
      };
    },
    startRunner: function () { runner.init(); }
  });
  function settleStartupRecovery(outcome) {
    latestRecoveryOutcome = outcome || null;
    if (!startupConsumersReady) return;
    var status = startupRecovery.settle(outcome);
    if (status.status === 'blocked' && status.reasonCode !== lastStartupBarrierWarning) {
      lastStartupBarrierWarning = status.reasonCode;
      console.warn('[site] startup recovery barrier remains blocked:', status.reasonCode);
    }
    if (status.status === 'ready') lastStartupBarrierWarning = null;
  }
  // Prepare/reconcile every runtime owner before the first derived snapshot.
  // Otherwise the seed can report an uncreated shallow-intake scratch root as
  // a current integrity failure.
  try { shallowIntake.prepareRuntime(); }
  catch (intakePrepareError) {
    console.error('[site] advisory shallow-intake runtime preparation failed: ' +
      String(intakePrepareError && intakePrepareError.message || intakePrepareError));
  }
  // The global architecture scan walks the product source tree. Start it
  // without delaying recovery or blocking the HTTP/SSE event loop; state reads
  // expose the strict unavailable verdict until this promise settles.
  taskIntegrity.prewarmArchitectureState();
  if (figmaEnabled) {
    figmaTaskPublication.beginRecovery();
    sessions.configureTurnPublication(figmaTaskPublication);
  }
  sessions.init();
  finalizations.init();
  appRunner.init({
    notify: function (eventName, payload) {
      sse.broadcast(eventName, payload);
    },
    persistPreference: function (preference) {
      return sse.serializeStateWrite(function () {
        var current = persistence.readPersisted();
        current.appRunPreferences = preference;
        persistence.writePersisted(current);
      });
    }
  });
  if (figmaEnabled) {
    figmaTestJob.init({ notify: function (eventName, payload) {
      sse.broadcast(eventName, payload);
      sse.pollLoop();
    }, syncActive: figmaSync.busy, syncRecoveryState: figmaSync.recoveryState });
    figmaSync.init({ notify: function (eventName, payload) {
      sse.broadcast(eventName, payload);
      sse.pollLoop();
    }, testActive: figmaTestJob.busy }).then(function () {
      return designTokenSources.init({
        publishDomains: figmaSync.publishDomains,
        requestDriftComparison: figmaSync.requestDriftComparison,
        startReactivation: figmaSync.startSourceReactivation,
        job: figmaSync.get,
        cleanupStage: figmaSync.cleanupExternalStage
      });
    }).then(function () {
      return figmaTaskPublication.init({
        publishDomains: figmaSync.publishDomains,
        notify: function (eventName, payload) {
          sse.broadcast(eventName, payload);
          sse.pollLoop();
        }
      }).catch(function (error) {
        figmaTaskPublication.failRecovery(error);
        console.error('[site] Figma task publication recovery failed: ' + String(error && error.message || error));
      });
    }).then(function () {
      // Comparison freshness: one exact startup fingerprint pass per domain
      // over the configured adapter roots plus the advisory root watchers.
      // Read-only — it only marks the projections stale, never repairs state.
      return designTokenCompare.init().catch(function (error) {
        console.error('[site] Token comparison startup reconcile failed: ' + String(error && error.message || error));
      }).then(function () {
        return designComponentCompare.init().catch(function (error) {
          console.error('[site] Component comparison startup reconcile failed: ' + String(error && error.message || error));
        });
      });
    }).catch(function (error) {
      figmaTaskPublication.failRecovery(error);
      console.error('[site] Figma sync recovery failed: ' + String(error && error.message || error));
    });
  }
  contractJob.init({ notify: function (eventName, payload) {
    sse.broadcast(eventName, payload);
    sse.pollLoop();
  } }).catch(function (error) {
    console.error('[site] backend contract job recovery failed: ' + String(error && error.message || error));
  });
  architectureGeneration.init({ notify: function (eventName, payload) {
    sse.broadcast(eventName, payload);
    sse.pollLoop();
  } }).catch(function (error) {
    console.error('[site] architecture job recovery failed: ' + String(error && error.message || error));
  });
  apiMock.init().then(function (ok) {
    if (!ok) console.error('[site] API mock recovery is unavailable; inspect Project → API → Diagnostics.');
  }).catch(function (error) {
    console.error('[site] API mock recovery failed: ' + String(error && error.message || error));
  });

  // Seed the hash so the first /api/events client doesn't get a no-op change.
  sse.seedHash();
  setInterval(sse.pollLoop, sse.POLL_MS);
  setInterval(sse.ssePingLoop, 25000);
  // Backstop reaper: even with zero user activity, release any finalization
  // ownership record whose process group is already provably dead (ESRCH /
  // Windows Job drain) but was stranded past termination verification with no
  // marker left to trigger readOne's lazy re-probe. mutationBlocked also reaps
  // on the mutation path; this idle backstop guarantees a stranded record can
  // never wedge the Board until a restart. Release proof is ESRCH-only.
  var finalizationReaper = setInterval(function () {
    try { finalizations.reap(); }
    catch (error) { console.error('[site] finalization reaper tick failed: ' + String(error && error.message || error)); }
  }, 30000);
  if (typeof finalizationReaper.unref === 'function') finalizationReaper.unref();
  // Recover deterministic creation and edit WALs as one ordered authority:
  // creation first, edit second. This avoids mutual blocking when both kinds
  // survived a double crash; corrupt state in either contract remains closed.
  var recoveredIntake = Object.create(null);
  var shallowReady = false;
  function scheduleRecovered(rows, reason) {
    (Array.isArray(rows) ? rows : []).forEach(function (row) {
      if (!row || typeof row.stem !== 'string') return;
      if (!shallowReady) { recoveredIntake[row.stem] = reason; return; }
      try { shallowIntake.schedule(row.stem, reason, { force: true }); }
      catch (error) { shallowIntake.recordFailure(row.stem, error); }
    });
  }
  publicationRecoveryController = backlogCreate.createRecoveryController({
    onWriterReconciled: function (result) {
      if (result && result.reconciled && result.reconciled.length) {
        console.warn('[site] reconciled ' + result.reconciled.length + ' interrupted writer lease mutation(s)');
      }
    },
    onRecovered: function (result) {
      var creations = result && result.creations;
      var edits = result && result.edits;
      if (creations && creations.recoveredCount) console.warn('[site] recovered ' + creations.recoveredCount + ' interrupted backlog creation(s)');
      if (edits && edits.recoveredCount) console.warn('[site] recovered ' + edits.recoveredCount + ' interrupted backlog edit(s)');
      scheduleRecovered(creations && creations.recovered, 'creation-recovered');
      scheduleRecovered(edits && edits.recovered, 'edit-recovered');
    },
    onError: function (error) {
      console.error('[site] deterministic task publication recovery failed: ' + String(error && error.message || error));
    },
    onAttemptSettled: settleStartupRecovery
  });
  publicationRecoveryController.start().catch(function (error) {
    console.error('[site] startup publication recovery controller failed: ' + String(error && error.message || error));
    latestRecoveryOutcome = null;
  }).finally(function () {
    // Advisory model queue is isolated from the unrestricted task runner.
    try {
      shallowIntake.init();
      shallowReady = true;
      Object.keys(recoveredIntake).forEach(function (stem) {
        try { shallowIntake.schedule(stem, recoveredIntake[stem], { force: true }); }
        catch (error) { shallowIntake.recordFailure(stem, error); }
      });
      recoveredIntake = Object.create(null);
    } catch (intakeInitError) {
      console.error('[site] advisory shallow-intake initialization failed (authoritative runner remains available): ' +
        String(intakeInitError && intakeInitError.message || intakeInitError));
    }
    // CLI runner drains authoritative prep/run/drop requests only after both
    // deterministic publication recovery and a fresh composite integrity scan
    // are green. A later bounded retry can open the barrier; settle() starts the
    // runner exactly once.
    startupConsumersReady = true;
    try { settleStartupRecovery(latestRecoveryOutcome); }
    catch (startupBarrierError) {
      console.error('[site] startup recovery barrier failed: ' + String(startupBarrierError && startupBarrierError.message || startupBarrierError));
    }
  });
  // Claude CLI readiness probe (installed / logged-in) for the header.
  cli.init();
  // Figma MCP connector readiness probe (connected / needs-auth) for the header
  // pill + the Figma panel. Read-only, like cli.init(); see server/figma.js.
  if (figmaEnabled) figma.init();
  // A restart rotates the connector identity generation. Once the initial MCP
  // probe and any active writer session settle, refresh the strict account and
  // file-access receipts without requiring a manual "Check connection" click.
  if (figmaEnabled) figmaTestJob.startupVerify();

  console.log('Orchestrator site server');
  console.log('  open:          http://localhost:' + PORT + '/site/');
  console.log('  project root:  ' + paths.PROJECT_ROOT);
  console.log('  state file:    ' + paths.STATE_FILE);
  console.log('Press Ctrl+C to stop.');
});

// Don't orphan spawned `claude` processes on shutdown — kill them so they stop
// consuming the plan in the background.
['SIGINT', 'SIGTERM'].forEach(function (sig) {
  process.on(sig, function () {
    try { runner.killAll(); } catch (e) {}
    try { cli.killLogin(); } catch (e) {}
    try { sessions.killAll(); } catch (e) {}
    try { finalizations.killAll(); } catch (e) {}
    try { if (publicationRecoveryController) publicationRecoveryController.stop(); } catch (e) {}
    try { backlogCreate.killAll(); } catch (e) {}
    try { shallowIntake.killAll(); } catch (e) {}
    try { contractJob.killAll(); } catch (e) {}
    try { architectureGeneration.killAll(); } catch (e) {}
    try { appRunner.killAll(); } catch (e) {}
    try { apiMock.killAll(); } catch (e) {}
    // Grace long enough for sessions.killAll()'s SIGTERM→SIGKILL escalation (~1s)
    // to fire, so a `claude` that ignores SIGTERM is force-killed, not orphaned.
    setTimeout(process.exit.bind(null, 0), 1500);
  });
});

// A stray unhandled promise rejection — e.g. a request handler whose returned
// promise rejects after the response was already sent, with nothing awaiting it
// — must NOT crash the dev server and kill every live session. Without this
// handler Node escalates an unhandled rejection to uncaughtException (below),
// which calls process.exit(1). Log it loudly and keep serving.
process.on('unhandledRejection', function (reason) {
  console.error('[server] unhandled promise rejection (kept alive):', (reason && reason.stack) || reason);
});

// A crash must not leave headless `claude` children running (consuming the
// plan) with no parent to reap them. Kill them, then exit non-zero.
process.on('uncaughtException', function (err) {
  console.error('[server] uncaught exception:', (err && err.stack) || err);
  try { runner.killAll(); } catch (e) {}
  try { cli.killLogin(); } catch (e) {}
  try { sessions.killAll(); } catch (e) {}
  try { finalizations.killAll(); } catch (e) {}
  try { if (publicationRecoveryController) publicationRecoveryController.stop(); } catch (e) {}
  try { backlogCreate.killAll(); } catch (e) {}
  try { shallowIntake.killAll(); } catch (e) {}
  try { contractJob.killAll(); } catch (e) {}
  try { architectureGeneration.killAll(); } catch (e) {}
  try { appRunner.killAll(); } catch (e) {}
  try { apiMock.killAll(); } catch (e) {}
  // Delay exit past sessions.killAll()'s ~1s SIGKILL sweep so a crash doesn't
  // orphan a SIGTERM-ignoring `claude`. (Was an immediate exit(1).)
  setTimeout(process.exit.bind(null, 1), 1200);
});
