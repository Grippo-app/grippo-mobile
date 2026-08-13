'use strict';

// ---------------------------------------------------------------------------
// Shared filesystem roots. Resolved once from this module's location so every
// server module agrees on the same paths. This file lives at
// orchestrator/site/server/, so the orchestrator dir is two levels up and the
// project root is three.
// ---------------------------------------------------------------------------

var path = require('path');

// ORCHESTRATOR_DIR is the installed code/template root. Test and generated
// project fixtures may relocate mutable project data through
// ORCHESTRATOR_PROJECT_ROOT, but must not redirect code/helper resolution into
// a partial fixture that intentionally contains only task data.
var ORCHESTRATOR_DIR = path.resolve(__dirname, '..', '..');               // orchestrator/
var PROJECT_ROOT     = path.resolve(process.env.ORCHESTRATOR_PROJECT_ROOT || path.join(__dirname, '..', '..', '..')); // project root
var PROJECT_ORCHESTRATOR_DIR = process.env.ORCHESTRATOR_PROJECT_ROOT
  ? path.join(PROJECT_ROOT, 'orchestrator')
  : ORCHESTRATOR_DIR;
// The live project config (orchestrator/project-config.md).
var PROJECT_CONFIG_FILE = path.join(PROJECT_ORCHESTRATOR_DIR, 'project-config.md');
var API_CONTRACT_DIR = path.join(PROJECT_ORCHESTRATOR_DIR, 'api-contract'); // project-owned backend-contract sidecar data
// Consolidated cache root — every ephemeral/regenerable/runtime artifact lives under
// orchestrator/.cache/<subsystem>/. The leading dot
// keeps the whole tree HTTP-denied by static.safeResolve with zero extra rules.
var CACHE_DIR              = path.join(ORCHESTRATOR_DIR, '.cache');
// When the validator/site is pointed at an explicit project fixture, every
// default runtime root must follow that project as well. Falling back to this
// package's checked-in cache would mix two trust domains and could make a
// fixture verdict depend on (or expose hashes for) the canonical workspace.
var RUNTIME_CACHE_DIR      = process.env.ORCHESTRATOR_CACHE_DIR ||
  (process.env.ORCHESTRATOR_PROJECT_ROOT ? path.join(PROJECT_ROOT, 'orchestrator', '.cache') : CACHE_DIR);
var FIGMA_CACHE_DIR        = path.join(RUNTIME_CACHE_DIR, 'figma');               // was orchestrator/figma/{.cache,reports}
var API_CONTRACT_CACHE_DIR = path.join(RUNTIME_CACHE_DIR, 'api-contract');        // was orchestrator/api-contract/{.cache,reports}
var API_MOCK_DIR           = path.join(API_CONTRACT_CACHE_DIR, 'mock');
var API_MOCK_INSTANCES_DIR = path.join(API_MOCK_DIR, 'instances');
var API_MOCK_STATE_FILE    = path.join(API_MOCK_DIR, 'state.json');
var API_MOCK_INDEX_FILE    = path.join(API_MOCK_DIR, 'index.json');

var STATE_FILE       = process.env.ORCHESTRATOR_STATE_FILE || path.join(RUNTIME_CACHE_DIR, 'site', '.site-state.json');
var LOCKS_DIR        = process.env.ORCHESTRATOR_LOCKS_DIR || path.join(RUNTIME_CACHE_DIR, 'tasks', 'locks');
var REQUESTS_DIR     = process.env.ORCHESTRATOR_REQUESTS_DIR || path.join(RUNTIME_CACHE_DIR, 'tasks', 'requests');
// One durable, per-stem admission/handoff receipt.  Deriving the default from
// REQUESTS_DIR (rather than CACHE_DIR) keeps isolated HTTP/test deployments in
// one cache tree when only the queue root is overridden.
var REQUEST_RESERVATIONS_DIR = process.env.ORCHESTRATOR_REQUEST_RESERVATIONS_DIR ||
  path.join(path.dirname(REQUESTS_DIR), 'request-reservations');
// Durable, prompt-free records for queue items rejected at execution because
// their admitted task snapshot is no longer current.  Keeping these separate
// from requests/ means stale work is consumed exactly once without making it
// claimable again, while retaining a bounded audit trail.
var SUPERSEDED_DIR   = process.env.ORCHESTRATOR_SUPERSEDED_DIR || path.join(RUNTIME_CACHE_DIR, 'tasks', 'superseded');
var WORKER_DIR       = path.join(RUNTIME_CACHE_DIR, 'tasks', 'worker');                   // standby /loop liveness
var HEARTBEAT_FILE   = path.join(WORKER_DIR, 'heartbeat.json');                   // written each loop pass
var RUNS_DIR         = process.env.ORCHESTRATOR_RUNS_DIR || path.join(RUNTIME_CACHE_DIR, 'tasks', 'runs'); // CLI-runner: claimed runs + logs
var JOURNAL_DIR      = process.env.ORCHESTRATOR_JOURNAL_DIR || path.join(RUNTIME_CACHE_DIR, 'tasks', 'journal');                  // per-task pipeline event log (log-event.py)
var CHECKPOINTS_DIR  = process.env.ORCHESTRATOR_CHECKPOINTS_DIR || path.join(RUNTIME_CACHE_DIR, 'tasks', 'checkpoints');
var TEST_CERTIFICATION_DIR = process.env.ORCHESTRATOR_TEST_CERTIFICATION_DIR ||
  path.join(RUNTIME_CACHE_DIR, 'tasks', 'test-certification');       // Step 4.4 receipts/summaries (NOT test-runs: runs/ is CLI-runner territory)
var TASK_ACTION_RECEIPTS_DIR = process.env.ORCHESTRATOR_TASK_ACTION_RECEIPTS_DIR ||
  path.join(RUNTIME_CACHE_DIR, 'tasks', 'action-receipts');
var FINALIZATIONS_DIR = process.env.ORCHESTRATOR_FINALIZATIONS_DIR || path.join(RUNTIME_CACHE_DIR, 'tasks', 'finalizations'); // durable finalize-task recovery markers
var WRITER_LEASES_DIR = process.env.ORCHESTRATOR_WRITER_LEASES_DIR || path.join(FINALIZATIONS_DIR, '.writers');                  // cross-process writer/finalizer handshake
// Explicit trust boundary for component-wise writer authority traversal. An
// explicitly relocated finalizations directory uses its parent as the
// operator-supplied root; normal site runtime fences every component below
// PROJECT_ROOT.
var WRITER_AUTHORITY_ROOT = path.resolve(process.env.ORCHESTRATOR_WRITER_AUTHORITY_ROOT ||
  (process.env.ORCHESTRATOR_PROJECT_ROOT ? PROJECT_ROOT :
    (process.env.ORCHESTRATOR_FINALIZATIONS_DIR ? path.dirname(FINALIZATIONS_DIR) : PROJECT_ROOT)));
var TASKS_DIR         = process.env.ORCHESTRATOR_TASKS_DIR ||
  (process.env.ORCHESTRATOR_PROJECT_ROOT ? path.join(PROJECT_ROOT, 'orchestrator', 'tasks') : path.join(ORCHESTRATOR_DIR, 'tasks'));
// Pre-Setup task drafts live outside the canonical task corpus. They can be
// saved durably while Setup is incomplete, but cannot acquire a task number,
// enter INDEX.json, or expose Prepare/Run until explicitly published later.
var TASK_INBOX_DIR    = process.env.ORCHESTRATOR_TASK_INBOX_DIR || path.join(RUNTIME_CACHE_DIR, 'tasks', 'inbox');
var TASK_INBOX_AUTHORITY_ROOT = path.resolve(process.env.ORCHESTRATOR_TASK_INBOX_AUTHORITY_ROOT ||
  (process.env.ORCHESTRATOR_PROJECT_ROOT ? PROJECT_ROOT :
    (process.env.ORCHESTRATOR_TASK_INBOX_DIR ? path.dirname(TASK_INBOX_DIR) :
      (process.env.ORCHESTRATOR_CACHE_DIR ? RUNTIME_CACHE_DIR : PROJECT_ROOT))));
var TASK_CREATIONS_DIR = process.env.ORCHESTRATOR_TASK_CREATIONS_DIR || path.join(RUNTIME_CACHE_DIR, 'tasks', 'creations');
// Same test/operator override rule as writer authority: the normal runtime is
// rooted at PROJECT_ROOT, while an explicitly relocated creations
// directory is anchored at its explicitly supplied parent unless the operator
// also supplied a project root.
var TASK_CREATIONS_AUTHORITY_ROOT = path.resolve(process.env.ORCHESTRATOR_TASK_CREATIONS_AUTHORITY_ROOT ||
  (process.env.ORCHESTRATOR_PROJECT_ROOT ? PROJECT_ROOT :
    (process.env.ORCHESTRATOR_TASK_CREATIONS_DIR ? path.dirname(TASK_CREATIONS_DIR) : PROJECT_ROOT)));
var TASK_EDITS_DIR      = process.env.ORCHESTRATOR_TASK_EDITS_DIR || path.join(RUNTIME_CACHE_DIR, 'tasks', 'edits');
var TASK_EDITS_AUTHORITY_ROOT = path.resolve(process.env.ORCHESTRATOR_TASK_EDITS_AUTHORITY_ROOT ||
  (process.env.ORCHESTRATOR_PROJECT_ROOT ? PROJECT_ROOT :
    (process.env.ORCHESTRATOR_TASK_EDITS_DIR ? path.dirname(TASK_EDITS_DIR) : PROJECT_ROOT)));
var TASK_INTAKE_DIR    = process.env.ORCHESTRATOR_TASK_INTAKE_DIR || path.join(RUNTIME_CACHE_DIR, 'tasks', 'intake');
var TRANSITIONS_DIR    = process.env.ORCHESTRATOR_TRANSITIONS_DIR || path.join(path.dirname(LOCKS_DIR), 'transitions');
// Per-task worktree isolation (pipeline improvement 01). Control-plane RECORD
// roots live in the runtime cache like every other durable runtime record;
// the worktree TREES themselves must never live under the cache (a generic
// cache cleanup would strand shared Git metadata), so their home is a hidden
// sibling of the project root. The home override is trusted server
// configuration only — never a request/prompt-supplied value.
var WORKTREE_RECORDS_DIR = process.env.ORCHESTRATOR_WORKTREE_RECORDS_DIR ||
  path.join(RUNTIME_CACHE_DIR, 'tasks', 'worktrees');       // manager-owned worktree lifecycle records
var INTEGRATIONS_DIR = process.env.ORCHESTRATOR_INTEGRATIONS_DIR ||
  path.join(RUNTIME_CACHE_DIR, 'tasks', 'integrations');    // integration WAL markers
// Same operator-override rule as the writer authority root: normal runtime
// fences every record component below PROJECT_ROOT; an explicitly relocated
// records directory anchors at its supplied parent.
var WORKTREE_RECORDS_AUTHORITY_ROOT = path.resolve(process.env.ORCHESTRATOR_WORKTREE_RECORDS_AUTHORITY_ROOT ||
  (process.env.ORCHESTRATOR_PROJECT_ROOT ? PROJECT_ROOT :
    (process.env.ORCHESTRATOR_WORKTREE_RECORDS_DIR ? path.dirname(WORKTREE_RECORDS_DIR) : PROJECT_ROOT)));
// Execution worktree HOME: hidden sibling of the project root (§7.1). Never
// inside orchestrator/.cache; components are verified by the manager with
// lstat/realpath/inode identity before any use.
var WORKTREE_HOME = path.resolve(process.env.ORCHESTRATOR_WORKTREE_HOME ||
  path.join(path.dirname(PROJECT_ROOT), '.orchestrator-worktrees'));
var APP_RUN_DIR        = process.env.ORCHESTRATOR_APP_RUN_DIR || path.join(RUNTIME_CACHE_DIR, 'runtime', 'app-run');
var APP_RUN_JOBS_DIR   = path.join(APP_RUN_DIR, 'jobs');
var APP_RUN_SESSIONS_DIR = path.join(APP_RUN_DIR, 'sessions');
var APP_RUN_ARTIFACTS_DIR = path.join(APP_RUN_DIR, 'artifacts');
var APP_RUN_HISTORY_DIR = path.join(APP_RUN_DIR, 'history');
var APP_RUN_SCREENSHOTS_DIR = path.join(APP_RUN_DIR, 'screenshots');
var APP_RUN_LOGS_DIR   = path.join(APP_RUN_DIR, 'logs');
var APP_RUN_INDEX_FILE = path.join(APP_RUN_DIR, 'index.json');

module.exports = {
  ORCHESTRATOR_DIR: ORCHESTRATOR_DIR,
  RUNTIME_CACHE_DIR: RUNTIME_CACHE_DIR,
  PROJECT_ORCHESTRATOR_DIR: PROJECT_ORCHESTRATOR_DIR,
  PROJECT_CONFIG_FILE: PROJECT_CONFIG_FILE,
  API_CONTRACT_DIR: API_CONTRACT_DIR,
  PROJECT_ROOT: PROJECT_ROOT,
  CACHE_DIR: CACHE_DIR,
  FIGMA_CACHE_DIR: FIGMA_CACHE_DIR,
  API_CONTRACT_CACHE_DIR: API_CONTRACT_CACHE_DIR,
  API_MOCK_DIR: API_MOCK_DIR,
  API_MOCK_INSTANCES_DIR: API_MOCK_INSTANCES_DIR,
  API_MOCK_STATE_FILE: API_MOCK_STATE_FILE,
  API_MOCK_INDEX_FILE: API_MOCK_INDEX_FILE,
  STATE_FILE: STATE_FILE,
  LOCKS_DIR: LOCKS_DIR,
  REQUESTS_DIR: REQUESTS_DIR,
  REQUEST_RESERVATIONS_DIR: REQUEST_RESERVATIONS_DIR,
  SUPERSEDED_DIR: SUPERSEDED_DIR,
  WORKER_DIR: WORKER_DIR,
  HEARTBEAT_FILE: HEARTBEAT_FILE,
  RUNS_DIR: RUNS_DIR,
  JOURNAL_DIR: JOURNAL_DIR,
  CHECKPOINTS_DIR: CHECKPOINTS_DIR,
  TEST_CERTIFICATION_DIR: TEST_CERTIFICATION_DIR,
  TASK_ACTION_RECEIPTS_DIR: TASK_ACTION_RECEIPTS_DIR,
  TASKS_DIR: TASKS_DIR,
  TASK_INBOX_DIR: TASK_INBOX_DIR,
  TASK_INBOX_AUTHORITY_ROOT: TASK_INBOX_AUTHORITY_ROOT,
  TASK_CREATIONS_DIR: TASK_CREATIONS_DIR,
  TASK_CREATIONS_AUTHORITY_ROOT: TASK_CREATIONS_AUTHORITY_ROOT,
  TASK_EDITS_DIR: TASK_EDITS_DIR,
  TASK_EDITS_AUTHORITY_ROOT: TASK_EDITS_AUTHORITY_ROOT,
  TASK_INTAKE_DIR: TASK_INTAKE_DIR,
  TRANSITIONS_DIR: TRANSITIONS_DIR,
  APP_RUN_DIR: APP_RUN_DIR,
  APP_RUN_JOBS_DIR: APP_RUN_JOBS_DIR,
  APP_RUN_SESSIONS_DIR: APP_RUN_SESSIONS_DIR,
  APP_RUN_ARTIFACTS_DIR: APP_RUN_ARTIFACTS_DIR,
  APP_RUN_HISTORY_DIR: APP_RUN_HISTORY_DIR,
  APP_RUN_SCREENSHOTS_DIR: APP_RUN_SCREENSHOTS_DIR,
  APP_RUN_LOGS_DIR: APP_RUN_LOGS_DIR,
  APP_RUN_INDEX_FILE: APP_RUN_INDEX_FILE,
  FINALIZATIONS_DIR: FINALIZATIONS_DIR,
  WRITER_LEASES_DIR: WRITER_LEASES_DIR,
  WRITER_AUTHORITY_ROOT: WRITER_AUTHORITY_ROOT,
  WORKTREE_RECORDS_DIR: WORKTREE_RECORDS_DIR,
  WORKTREE_RECORDS_AUTHORITY_ROOT: WORKTREE_RECORDS_AUTHORITY_ROOT,
  INTEGRATIONS_DIR: INTEGRATIONS_DIR,
  WORKTREE_HOME: WORKTREE_HOME
};
