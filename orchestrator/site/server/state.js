'use strict';

// ---------------------------------------------------------------------------
// Derive the full state snapshot that /api/state returns, plus the INDEX.json
// readers the SSE poll/timing reconciler share. The process-lifetime activity
// tracker (Goal 4) lives here as a singleton so deriveState() can read it and
// the SSE poll (sse.js) can advance it — both see the same instance.
// ---------------------------------------------------------------------------

var fs    = require('fs');
var path  = require('path');
var TextDecoder = require('util').TextDecoder;
var paths = require('./paths');
var fileGuards = require('./file-guards');

var persistence = require('./persistence');
var validators  = require('./validators');
var projectCfg  = require('./project-config');
var projectConfigUpdate = require('./project-config-update');
var figmaFeatureGate = require('./figma-feature-gate');
var locksMod    = require('./locks');
var requestsMod = require('./requests');
var statusModel = require('./status');
var workerMod   = require('./worker');
var skillsMod   = require('./skills');
var runnerMod   = require('./runner');
var cliMod      = require('./cli');
var figmaMod    = require('./figma');
var figmaIntegrationMod = require('./figma-integration');
var figmaEvidenceMod = require('./figma-evidence');
var figmaScreensMod = require('./figma-screens');
var backendIntegrationMod = require('./backend-integration');
var sessionsMod = require('./sessions');
var finalizationsMod = require('./finalizations');
var integrationsMod = require('./integrations');
var worktreeManagerMod = require('./worktree-manager');
var shallowIntakeMod = require('./shallow-intake');
var editMarkersMod = require('./edit-markers');
var creationMarkersMod = require('./creation-markers');
var taskIntegrityMod = require('./task-integrity');
var startupRecoveryMod = require('./startup-recovery');
var taskSourceMod = require('./task-source');
var figmaDesignParser = require('../../figma/scripts/design-parser.cjs');

var ORCHESTRATOR_DIR = paths.ORCHESTRATOR_DIR;
var STEP_VALIDATORS  = validators.STEP_VALIDATORS;
var QUESTIONS_MAX = 8 * 1024 * 1024;
var TASKS_AUTHORITY_ROOT = path.resolve(process.env.ORCHESTRATOR_PROJECT_ROOT
  ? paths.PROJECT_ROOT
  : (process.env.ORCHESTRATOR_TASKS_DIR ? path.dirname(paths.TASKS_DIR) : paths.PROJECT_ROOT));

// One process-lifetime observer feeding /api/state's `status` block. Advanced
// once per pollLoop tick (before hashing) so its drain/activity stamps are
// current even when no browser is connected. See server/status.js.
var activityTracker = statusModel.createActivityTracker();

function tasksIndexMtime() {
  // INDEX.json's mtime is the single load-bearing signal for the Board
  // panel — the orchestrator regenerates it after every task mutation
  // (see task-prep.md Step 7 / orchestrator.md Step 6c). Including its
  // mtime in stateHash makes the SSE 'change' event fire on task
  // mutations, so the Board updates without a page reload.
  try {
    return fs.statSync(path.join(paths.TASKS_DIR, 'INDEX.json')).mtimeMs;
  } catch (e) {
    return 0;
  }
}

// Read every INDEX-derived projection from one exact source generation. The SSE
// poll shares this result with deriveState(), so a concurrent task move cannot
// combine stems from one valid revision with columns from another. Invalid or
// non-current bytes return an unavailable snapshot; empty arrays are reserved
// for a valid, authoritatively empty board.
function readIndexSnapshot() {
  var current, parsed;
  try { current = taskSourceMod.readIndex(); parsed = current && current.value; }
  catch (e) { return { available: false, revision: null, stems: null, columns: null }; }
  if (!parsed) return { available: false, revision: null, stems: null, columns: null };
  var cols = ['backlog', 'pending', 'todo', 'done'];
  var columns = { backlog: [], pending: [], todo: [], done: [] };
  var stems = [];
  var seen = Object.create(null);
  for (var c = 0; c < cols.length; c++) {
    var arr = parsed[cols[c]];
    for (var i = 0; i < arr.length; i++) {
      var e = arr[i];
      if (e && typeof e === 'object' && typeof e.stem === 'string' && e.stem) {
        columns[cols[c]].push({ stem: e.stem, doneAt: typeof e.doneAt === 'string' ? e.doneAt : null });
        if (!seen[e.stem]) {
          seen[e.stem] = true;
          stems.push(e.stem);
        }
      }
    }
  }
  return {
    available: true,
    revision: typeof current.revision === 'string' ? current.revision : null,
    stems: stems,
    columns: columns
  };
}

// Per-board-task figma:screens state for /api/state.screensCache:
//   { <stem>: { needed, pulled, status, count, expected, mtime,
//       designIssues?: { malformed, captureBlocked, issueCount, kinds, first? } } }
//   for every board stem that is a
//   UI task (`needed` — its .md carries a pullable `## Design` bullet) and/or has
//   a complete/fresh pulled cache (`pulled` — status === "complete"). Drives
//   the board: the green "re-pull" button (pulled), the amber "screens not
//   pulled" card chip + the confirm-on-Run warning (needed && !pulled). Mirrors
//   figma.tokensInfo (what-was-pulled + freshness) but PER TASK. Task bodies
//   are read through the bounded safe reader and parsed results are cached by
//   their exact content hash.
//   Everything is in the snapshot, so sse.stateHash's JSON.stringify pushes a
//   live 'change' the tick a pull lands (or a task gains/loses a Design ref).

function parseDesignSources(bodies) {
  try { return figmaDesignParser.parseDesignSources(bodies); }
  catch (e) {
    return {
      entries: [],
      issues: [{ kind: 'DESIGN_PARSER_UNAVAILABLE', line: null }],
      hasPullable: false
    };
  }
}

// The board task body for <stem>: todo/ (ready), backlog/ (pending body; answers
// live in a sidecar), then done/ for completed UI tasks whose Screens/Evidence
// tabs still need the original ## Design declaration.
function taskBodyPath(stem) {
  var todo = path.join(paths.TASKS_DIR, 'todo', stem + '.md');
  if (fs.existsSync(todo)) return todo;
  var backlog = path.join(paths.TASKS_DIR, 'backlog', stem + '.md');
  if (fs.existsSync(backlog)) return backlog;
  var done = path.join(paths.TASKS_DIR, 'done', stem + '.md');
  if (fs.existsSync(done)) return done;
  return null;
}

function pendingQuestionsPath(stem) {
  var p = path.join(paths.TASKS_DIR, 'pending', stem + '.questions.md');
  return fs.existsSync(p) ? p : null;
}

function readQuestionsSource(file) {
  try {
    var directory = path.dirname(file);
    var hit = fileGuards.boundedRegularFileUnder(TASKS_AUTHORITY_ROOT, directory, file, QUESTIONS_MAX);
    if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') return null;
    return {
      bytes: hit.bytes,
      text: new TextDecoder('utf-8', { fatal: true }).decode(hit.bytes)
    };
  } catch (error) {
    return null;
  }
}

function designSourceFor(stem) {
  var bodyPath = taskBodyPath(stem);
  if (!bodyPath) return { bodies: [], stamp: 'unavailable', unavailable: true };
  var column = path.basename(path.dirname(bodyPath));
  var body = taskSourceMod.readTask(column, stem);
  if (!body) return { bodies: [], stamp: 'unavailable', unavailable: true };
  var bodies = [body.text];
  var stamp = [column + ':' + taskSourceMod.sha(body.bytes)];
  var questionsPath = pendingQuestionsPath(stem);
  if (questionsPath) {
    var questions = readQuestionsSource(questionsPath);
    if (!questions) return { bodies: [], stamp: 'unavailable', unavailable: true };
    bodies.push(questions.text);
    stamp.push('pending:' + taskSourceMod.sha(questions.bytes));
  }
  return { bodies: bodies, stamp: stamp.join('|'), unavailable: false };
}

// content-hash-cached parse of the task's `## Design` declaration. ONE safe body read + ONE
// parse per body change now serves `needed` (hasPullable), cacheStatusFor's
// entry matching AND the designIssues authoring summary. Re-parsing occurs only
// when the safely read bytes change. hasSection is the honest "the body carries a `## Design`
// heading at all" bit, derived by the exact shared structural parser,
// so the board can distinguish a broken Design section from a non-UI task.
var designCache = Object.create(null);   // stem -> { stamp, design, hasSection }
function designInfo(stem) {
  var src = designSourceFor(stem);
  var c = designCache[stem];
  if (c && c.stamp === src.stamp) return c;
  var design = src.unavailable
    ? { entries: [], issues: [{ kind: 'DESIGN_SOURCE_UNAVAILABLE', line: null }], hasPullable: false }
    : parseDesignSources(src.bodies);
  var hasSection = !src.unavailable &&
    src.bodies.some(function (b) { return figmaDesignParser.hasDesignSection(b); });
  c = { stamp: src.stamp, design: design, hasSection: hasSection, unavailable: src.unavailable === true };
  designCache[stem] = c;
  return c;
}

// Authoring-time visibility for a malformed `## Design` section. The kind
// set is SINGLE-SOURCED from design-parser (imported below, not a hand-copied
// literal) so the board's "Design: broken" chip can never drift from what
// hasMalformedDesign() actually blocks: any of these issue kinds (or a bullet
// that is neither `none` nor pullable) means the task clearly INTENDS a Figma
// comparison but the declaration is broken — surface it on the board
// (hasPullable:false → no chip, no Pull button) before the pipeline blocks.
// UNRECOGNIZED_KIND_TAG is deliberately NOT in the parser's set — warn-grade, so
// the board keeps its normal pulling/pulled chip.
// NOTE: the ship-done/verify-done UI-by-evidence backstop (design-parser.uiTaskWithoutDesign —
// a task citing a Figma node URL / designComponentId+figmaNodeId snapshot / editing a screen-dialog file
// yet ABSENT a `## Design` bullet) is intentionally NOT surfaced here: that is a MISSING section
// (no parsed issues), an orthogonal concern from this PRESENT-but-broken set. It blocks at ship
// with an exit-2 message, not a board chip. Do NOT fold it in (it would conflate two conditions).
var MALFORMED_DESIGN_KINDS = figmaDesignParser.MALFORMED_DESIGN_KINDS;
var DESIGN_OBSERVATION_FAILURES = Object.freeze({
  DESIGN_PARSER_UNAVAILABLE: 1,
  DESIGN_SOURCE_UNAVAILABLE: 1
});
function designIssuesSummary(info) {
  var issues = Array.isArray(info.design.issues) ? info.design.issues : [];
  var entries = Array.isArray(info.design.entries) ? info.design.entries : [];
  var kinds = [];
  var seen = Object.create(null);
  for (var i = 0; i < issues.length && kinds.length < 3; i++) {
    var k = issues[i] && issues[i].kind;
    if (typeof k !== 'string' || !k || seen[k]) continue;
    seen[k] = 1;
    kinds.push(k);
  }
  var malformed = issues.some(function (x) {
    return !!(x && (MALFORMED_DESIGN_KINDS[x.kind] || DESIGN_OBSERVATION_FAILURES[x.kind]));
  }) ||
    entries.some(function (e) { return !!(e && !e.none && !Object.keys(e.themes || {}).length); });
  var out = {
    malformed: malformed,
    // screen-token-plans rejects every parser issue before reserving capture
    // sources. Publish that exact admission fact so Task Summary and the Board
    // do not mislabel a known authoring error as merely "screens missing".
    captureBlocked: issues.length > 0,
    issueCount: issues.length,
    kinds: kinds
  };
  // The Board needs only the exact issue kind and line. Parser prose and the
  // offending task text are diagnostics, not a localized browser contract.
  var first = issues.length ? issues[0] : null;
  if (first && typeof first === 'object') {
    out.first = {
      kind: typeof first.kind === 'string' ? first.kind : '',
      line: typeof first.line === 'number' ? first.line : null
    };
  }
  return out;
}

function readJsonSafe(file) {
  try { return JSON.parse(fs.readFileSync(file, 'utf8')); }
  catch (e) { return null; }
}

function normalizedNodeId(raw) {
  return String(raw || '').trim().replace(/-/g, ':');
}

function cacheStatusFor(stem, dir) {
  var design = designInfo(stem).design;   // cached — reuses screensCacheMap's parse
  var entries = (design.entries || []).filter(function (e) { return !e.none && e.themes && Object.keys(e.themes).length; });
  if (!entries.length) return { needed: false, pulled: false, status: 'not-needed', count: 0, mtime: 0, expected: 0 };
  // Pull-progress denominator: how many per-theme screen pulls the
  // `## Design` section declares (the same theme expansion the count++ loop below
  // uses: `primary` is one pull; `light`/`dark` count separately). Ships on the
  // entry so the board's "Pulling Figma… {n}/{m}" chip has an honest total.
  // Cheap: derived from the already-parsed, content-hash-cached designInfo entries.
  // Named expectedCount, NOT `expected` — the per-entry loop below declares a
  // function-scoped `var expected` (its theme-expansion array) that would
  // silently shadow/overwrite this counter.
  var expectedCount = 0;
  for (var x = 0; x < entries.length; x++) {
    var th = entries[x].themes || {};
    expectedCount += th.primary ? 1 : ((th.light ? 1 : 0) + (th.dark ? 1 : 0));
  }
  var indexPath = path.join(dir, 'index.json');
  var st;
  try { st = fs.statSync(indexPath); } catch (e) {
    // index.json is written LAST by the pull contract (screensPrompt step 4b — atomic, after
    // every per-screen file), so during a FIRST pull the spec+png pairs land BEFORE any index
    // exists. Probe them by their design-declared names so the board's "Pulling Figma… n/m"
    // chip ticks up live instead of pinning 0 for the whole session. Same confinement
    // discipline as has() below (a crafted screen name must not probe outside the task dir).
    var early = 0;
    for (var p0 = 0; p0 < entries.length; p0++) {
      var th0 = entries[p0].themes || {};
      var sufs = th0.primary ? [''] : [].concat(th0.light ? [''] : []).concat(th0.dark ? ['.dark'] : []);
      for (var s0 = 0; s0 < sufs.length; s0++) {
        var base0 = path.normalize(path.join(dir, entries[p0].screen + sufs[s0]));
        if (base0 === dir || base0.indexOf(dir + path.sep) !== 0) continue;
        try { if (fs.statSync(base0 + '.spec.json').isFile() && fs.statSync(base0 + '.png').isFile()) early++; } catch (e0) {}
      }
    }
    return { needed: true, pulled: false, status: 'missing', count: early, mtime: 0, expected: expectedCount };
  }
  var idx = readJsonSafe(indexPath);
  if (!idx || !idx.nodes || typeof idx.nodes !== 'object') return { needed: true, pulled: false, status: 'mismatched', count: 0, mtime: st.mtimeMs, expected: expectedCount };
  var status = 'complete';
  var count = 0;
  // Confine every existence probe under the task cache dir — defense-in-depth vs a
  // stray idx.nodes key like '../x' (the cache is trusted, but mirror figma-screens.js's
  // confinement on the same key set). A non-confined name reads as a missing file.
  function has(file) {
    var full = path.normalize(path.join(dir, file));
    if (full !== dir && full.indexOf(dir + path.sep) !== 0) return false;
    try { return fs.statSync(full).isFile(); } catch (e) { return false; }
  }
  // Cache identity is the exact Design screen name. A renamed bullet requires a
  // re-pull so stale files can never be silently rebound to a different entry.
  function findCacheNode(e) {
    return idx.nodes[e.screen] ? { node: idx.nodes[e.screen], name: e.screen } : null;
  }
  for (var i = 0; i < entries.length; i++) {
    var e = entries[i];
    var match = findCacheNode(e);
    if (!match) { status = 'missing'; continue; }
    var node = match.node, cacheName = match.name;
    // Kind mirror of the check-screen-cache gate (KIND_MISSING_IN_INDEX / KIND_MISMATCH, both
    // BLOCKER there, bidirectional): a bullet whose [kind] tag was edited after the pull — or a
    // kind-less index entry for a non-screen bullet — must not keep the board green
    // "pulled" while Step 1b hard-blocks. 'stale' is the honest status: the remedy is a re-pull
    // (which rewrites the index kind from the bullet), exactly what the stale tooltip says.
    var nodeKind = String(node.kind || '').toLowerCase();
    if (nodeKind ? nodeKind !== e.kind : e.kind !== 'screen') status = 'stale';
    var themes = e.themes || {};
    var expected = themes.primary ? [{ theme: 'primary', data: themes.primary, suffix: '' }]
      : []
        .concat(themes.light ? [{ theme: 'light', data: themes.light, suffix: '' }] : [])
        .concat(themes.dark ? [{ theme: 'dark', data: themes.dark, suffix: '.dark' }] : []);
    for (var j = 0; j < expected.length; j++) {
      var t = expected[j];
      var actualUrl = t.theme === 'dark' ? (node.darkUrl || '') : (node.url || '');
      var actualNodeId = normalizedNodeId(t.theme === 'dark' ? (node.darkNodeId || '') : (node.nodeId || ''));
      // Compare the CANONICAL Figma href (file-key + node-id), not the raw string,
      // so a dev-mode `&m=dev` suffix or `-`/`:` formatting can't false-flag a stale.
      var urlMatch = figmaScreensMod.canonicalFigmaHref(actualUrl) === figmaScreensMod.canonicalFigmaHref(t.data.url);
      if (!urlMatch || actualNodeId !== normalizedNodeId(t.data.nodeId)) status = 'stale';
      if (!has(cacheName + t.suffix + '.spec.json') || !has(cacheName + t.suffix + '.png')) {
        if (status === 'complete') status = 'incomplete';
      } else count++;
    }
    if (!has(cacheName + '.instances.json') || !has(cacheName + '.context.json')) {
      if (status === 'complete') status = 'incomplete';
    }
  }
  // Extra pulled screens (cache has more than the Design lists) are NOT a mismatch:
  // the needed screens are present; the extras just appear in the gallery. (A truly
  // wrong cache still fails above via findCacheNode -> 'missing'.)
  return { needed: true, pulled: status === 'complete', status: status, count: count, mtime: st.mtimeMs, expected: expectedCount };
}

function screensCacheMap(figmaEnabled, stems) {
  if (!figmaEnabled) return {};
  if (!Array.isArray(stems) || !stems.length) return {};
  var root = path.join(paths.FIGMA_CACHE_DIR, 'screens');
  var rootExists = false;
  try { rootExists = fs.statSync(root).isDirectory(); } catch (e) {}
  var out = {};
  for (var i = 0; i < stems.length; i++) {
    var stem = stems[i];
    var info = designInfo(stem);
    var needed = info.design.hasPullable;
    var status = { needed: needed, pulled: false, status: needed ? 'missing' : 'not-needed', count: 0, mtime: 0, expected: 0 };
    if (rootExists || needed) status = cacheStatusFor(stem, path.join(root, stem));
    var evidence = figmaEvidenceMod.readEvidenceLite(stem);
    if (evidence) status.evidence = evidence;
    // The mandatory-comparison signal: a UI task owes a final Figma comparison it has not yet
    // produced. Derive it from the single authoritative `evidenceState`: only a READY bundle
    // (final · !mixed · !drift · !missing/SKIPPED required) clears it, so a null/prebuild/incomplete/
    // stale/mixed bundle all surface the blocking state. Non-UI tasks (neither needed nor pulled) are
    // never flagged. RESERVED SIGNAL: the task-detail IA redesign removed the board's red
    // "comparison required" blocker that read this, so there is no in-repo client reader today; it is
    // intentionally kept as a correct, cheap field on the `/api/state` screensCache contract (the
    // enforcement itself is owned by ship-done/verify-done/the pre-commit gate + the done Figma view).
    status.comparisonMissing = (status.needed || status.pulled) &&
      !(evidence && evidence.present && evidence.evidenceState === 'READY');
    // Additive authoring signal: { malformed, captureBlocked, issueCount,
    // kinds, first? } describing the task's
    // `## Design` parse. Present ONLY when the Figma flow is enabled AND the body
    // actually carries a `## Design` section — non-Figma projects and design-less
    // tasks see no new field.
    if (figmaEnabled && (info.hasSection || info.unavailable)) status.designIssues = designIssuesSummary(info);
    if (status.needed || status.pulled || status.status !== 'not-needed' || evidence ||
        // A malformed-only Design section parses to zero pullable entries
        // ('not-needed', no evidence) — without this clause exactly the broken
        // malformed-only tasks would be dropped from the map.
        (status.designIssues && (status.designIssues.malformed || status.designIssues.issueCount > 0))) {
      out[stem] = status;
    }
  }
  return out;
}

function deriveState(observations) {
  var persisted     = persistence.readPersisted();
  var setupForm     = persisted.setupForm || {};
  var manualSteps   = persisted.manualSteps || {};

  // Committed config supplies the stable base; the persisted Setup draft
  // deterministically overlays editable form fields.
  var fromConfig = projectCfg.parseConfigForm();
  setupForm = Object.assign({}, fromConfig || {}, setupForm);
  // Reviewer mode comes only from the guarded committed config read.
  var reviewerConfigRead = projectConfigUpdate.read();
  // Never fall back to the permissive Setup parser when the guarded read
  // refuses a symlink, hardlink, malformed frontmatter, or changing file.
  var canonicalCodexEnabled = reviewerConfigRead.ok ? reviewerConfigRead.codexEnabled : null;
  setupForm = Object.assign({}, setupForm);
  delete setupForm.codexEnabled;
  if (canonicalCodexEnabled) setupForm.codexEnabled = canonicalCodexEnabled;
  var liveFigmaGate = figmaFeatureGate.current(reviewerConfigRead);
  var figmaEnabled = liveFigmaGate.enabled;
  setupForm.figmaEnabled = reviewerConfigRead.ok &&
    reviewerConfigRead.figmaEnabledState !== 'invalid' && reviewerConfigRead.figmaEnabled === true;
  var figmaIntegration = figmaEnabled ? figmaIntegrationMod.get() : null;
  if (figmaIntegration && figmaIntegration.projectFile && figmaIntegration.projectFile.state === 'selected') {
    setupForm.figmaLibraryUrl = figmaIntegration.projectFile.url;
  }

  // Each setup gate is FS-derived, but a persisted manual override
  // (manualSteps['setup:<gate>'] === true) can force it done — the SAME
  // escape-hatch the wizard's per-step "Mark done" gives. Without it a
  // false-negative validator (an unusual-but-valid config, a non-standard
  // layout) would dead-end the whole pipeline with no recourse. setupGates
  // (exposed below) carries fsDone + overridden per gate so the Setup panel can
  // paint a "mark done manually / clear" affordance and reflect an active override.
  var reqVerifiedFs     = validators.validateRequirementsVerified();
  var yamlPastedFs      = validators.validateYamlConfigPopulated();
  var agentsInstalledFs = validators.validateAgentsInstalled();
  var reqOverride       = manualSteps['setup:requirementsVerified'] === true;
  var yamlOverride      = manualSteps['setup:yamlPasted'] === true;
  var agentsOverride    = manualSteps['setup:agentsInstalled'] === true;
  var reqVerified     = reqVerifiedFs || reqOverride;
  var yamlPasted      = yamlPastedFs || yamlOverride;
  var agentsInstalled = agentsInstalledFs || agentsOverride;
  var setupDone       = reqVerified && yamlPasted && agentsInstalled;

  // Skills-runtime readiness signal (skills-only template). ADDITIVE — does NOT
  // affect setupDone.
  var skillsManifestPresent = fs.existsSync(
    path.join(ORCHESTRATOR_DIR, 'skills', '_index', 'install-manifest.json'));

  // Per-step status with three kinds:
  //   { id, kind: 'fs',     done, overridden }   — derived from filesystem;
  //                                                manual override wins.
  //   { id, kind: 'manual', done }               — user-confirmed via UI;
  //                                                no validator exists.
  // For 'fs' steps, manualSteps[id] === true forces done=true even when
  // the validator returns false. This unblocks rename/move refactors and
  // non-standard structures without giving up self-healing for the
  // normal case (just clear the override to fall back to FS detection).
  var stepStatus = {};
  var wizardStepsDone = [];

  Object.keys(STEP_VALIDATORS).forEach(function (stepId) {
    var v = STEP_VALIDATORS[stepId];
    var overridden = manualSteps[stepId] === true;
    var done;
    var kind;
    if (v === null || v === undefined) {
      kind = 'manual';
      done = overridden;
      stepStatus[stepId] = { kind: kind, done: done };
    } else {
      kind = 'fs';
      var fsDone;
      try {
        // Validators that read setupForm declare it via fn.length === 1.
        fsDone = v.length >= 1 ? !!v(setupForm) : !!v();
      } catch (e) {
        console.warn('[site] validator', stepId, 'threw:', e && e.message);
        fsDone = false;
      }
      done = fsDone || overridden;
      stepStatus[stepId] = { kind: kind, done: done, fsDone: fsDone, overridden: overridden };
    }
    if (done) wizardStepsDone.push(stepId);
  });

  // Read locks + requests once and share them with the status block so the
  // two views of the same tick can't disagree.
  var suppliedLockRead = observations && observations.lockRead;
  var lockRead = suppliedLockRead &&
    typeof suppliedLockRead.available === 'boolean' &&
    Array.isArray(suppliedLockRead.rows) &&
    (suppliedLockRead.available
      ? suppliedLockRead.errorCode === null
      : suppliedLockRead.rows.length === 0 &&
        (suppliedLockRead.errorCode === 'runtime-locks-unavailable' ||
         suppliedLockRead.errorCode === 'runtime-locks-entry-limit'))
    ? suppliedLockRead
    : locksMod.readLocksResult();
  var locks = lockRead.rows;
  var suppliedRequestRead = observations && observations.requestRead;
  var requestRead = suppliedRequestRead &&
    typeof suppliedRequestRead.ok === 'boolean' &&
    Array.isArray(suppliedRequestRead.rows) &&
    (suppliedRequestRead.ok || suppliedRequestRead.rows.length === 0 &&
      typeof suppliedRequestRead.code === 'string')
    ? suppliedRequestRead
    : requestsMod.scanRequests();
  var requests = requestRead.rows;
  var suppliedIndexRead = observations && observations.indexRead;
  var indexRead = suppliedIndexRead &&
    typeof suppliedIndexRead.available === 'boolean' &&
    (suppliedIndexRead.available
      ? Array.isArray(suppliedIndexRead.stems) &&
        suppliedIndexRead.columns && typeof suppliedIndexRead.columns === 'object'
      : suppliedIndexRead.stems === null && suppliedIndexRead.columns === null)
    ? suppliedIndexRead
    : readIndexSnapshot();
  var boardStems = indexRead.stems;
  var heartbeat = workerMod.readHeartbeat();
  var act = activityTracker.snapshot();
  var figmaStatus = liveFigmaGate.error && liveFigmaGate.error !== 'figma-disabled'
    ? { state: 'unavailable', local: null, global: null, configError: liveFigmaGate.error }
    : (figmaEnabled ? figmaMod.status() : { state: 'disabled', local: null, global: null });
  var figmaTokensInfo = figmaEnabled ? figmaMod.tokensInfo() : null;
  var figmaAccount = figmaEnabled && figmaStatus.state === 'connected' ? figmaMod.account() : null;
  var creationRecoveryState = creationMarkersMod.scan();
  var editRecoveryState = editMarkersMod.scan();
  var publicationRecoveryIssues = creationRecoveryState.blocking.map(function (issue) {
    return { kind: 'creation', code: issue.code, stem: issue.stem || null };
  }).concat(editRecoveryState.blocking.map(function (issue) {
    return { kind: 'edit', code: issue.code, stem: issue.stem || null };
  }));
  var taskIntegrity;
  try { taskIntegrity = taskIntegrityMod.publicResult(taskIntegrityMod.validateAllCached()); }
  catch (integrityError) { taskIntegrity = taskIntegrityMod.unavailableResult(); }

  return {
    version: 1,
    setup: setupForm,
    // Absolute root of the project this server serves. Surfaced so the board's
    // "worker offline" banner can print the exact `cd <projectRoot>` command
    // the user needs to start the queue drainer in the right directory.
    projectRoot: paths.PROJECT_ROOT,
    // Server-persisted UI language ('en'|'ru'|'uk'; '' = never chosen). The client
    // boots from its per-origin localStorage cache, then adopts this value —
    // localStorage alone changes with an explicitly selected port (new port = new origin),
    // so ONLY this survives across sessions. Written via POST /api/state-patch
    // { uiLang } (app.js persists the toggle).
    uiLang: persisted.uiLang || '',
    // Claude CLI readiness (installed / logged-in), global per machine. Drives
    // the header's CLI status + install/login buttons. Cached (see server/cli.js).
    cli: Object.assign({}, cliMod.status(), { jobs: cliMod.jobs() }),
    // Figma MCP connector readiness for the header pill + Figma panel. Cached
    // probe; see server/figma.js. status() carries { state, local, global } —
    // the per-project local "figma" server vs any competing global connector.
    //   account: the bound Figma identity (handle/email), recorded by the
    //            figma:whoami session into orchestrator/figma/.account.json. null until run.
    figma: Object.assign({}, figmaStatus, {
      account: figmaAccount,
      //   tokensPresent: true once an MCP export (Variables/Styles) has written the
    //   normalized snapshot the Tokens viewer reads.
      tokensPresent: !!figmaTokensInfo,
      tokensInfo: figmaTokensInfo,    // { count, mtime } from the current sealed generation
      canOpenTerminal: process.platform === 'darwin'   // the "Open terminal" button is macOS-only (osascript)
    }),
    // Process applicability is separate from the integration model. Disabled
    // projects expose no account/file receipts, but still get a truthful,
    // actionable enable or restart-required state in the Figma panel.
    figmaFeature: liveFigmaGate.feature,
    figmaIntegration: figmaIntegration,
    reviewerConfig: {
      mode: canonicalCodexEnabled === 'auto' ? 'automatic'
        : canonicalCodexEnabled === 'true' ? 'require-codex'
          : canonicalCodexEnabled === 'false' ? 'internal-only' : null,
      revision: reviewerConfigRead.ok ? reviewerConfigRead.revision : null,
      canUpdate: reviewerConfigRead.ok && typeof reviewerConfigRead.revision === 'string',
      state: reviewerConfigRead.ok && canonicalCodexEnabled ? 'ready'
        : (reviewerConfigRead.ok && reviewerConfigRead.codexFieldState === 'missing' ? 'missing' : 'invalid')
    },
    // Typed Backend integration model (non-secret). Kept separate from the
    // generation-bound API workbench projections owned by api-relations.
    backend: backendIntegrationMod.get(),
    appRunPreferences: persisted.appRunPreferences,
    // Live (+ recently finished) interactive sessions keyed by context
    // ("setup" | "task:<stem>"). The client run-control reads
    // sessions[key] to flip a Run button into a Terminal button while running,
    // and the header Sessions menu lists them. Events are polled per-key via
    // /api/session/events.
    sessions: sessionsMod.list(),
    // Per-board-task figma:screens cache state { <stem>: { needed, pulled, status,
    // count, expected, mtime, evidence?, comparisonMissing, designIssues? } } — drives the
    // board "Pull Figma screens" button's green/re-pull state and the
    // screens-not-pulled card chip + confirm-on-Run warning. designIssues
    // only appears when figmaEnabled. See screensCacheMap().
    screensCache: screensCacheMap(figmaEnabled, boardStems),
    // True when the in-process CLI runner is draining the queue itself. When so,
    // the standby /loop worker is unnecessary, so the UI shows "runner active"
    // instead of a "worker offline" warning. running/max let the pill show
    // "busy" (saturated) vs "active" (has capacity) honestly.
    // "Active" is execution readiness, not merely binary presence. A logged-
    // out/revoked local CLI leaves requests in the durable waiting-runner state
    // and does not make the standby worker stand down.
    runnerActive: typeof runnerMod.isReady === 'function'
      ? runnerMod.isReady() : runnerMod.isEnabled(),
    runnerRunning: runnerMod.runningCount(),
    runnerMax: runnerMod.MAX_PARALLEL,
    // Process-local startup barrier. Durable owner modules remain authoritative;
    // this status explains why the in-process queue runner is intentionally
    // dormant while exact recovery or its fresh integrity fence is unresolved.
    startupRecovery: startupRecoveryMod.snapshot(),
    // Canonical filesystem verdict is separate from INDEX: on corruption the
    // board continues to show the last valid index while this block explains
    // the exact affected paths and disables only unsafe mutations.
    taskIntegrity: taskIntegrity,
    // Header "Skills" pill: install state of the authored skill set
    // (.claude/skills/ + .claude/contracts/ vs the install manifest) plus the
    // screenshot-gate wiring probe. runHeld mirrors the run-gate,
    // sessions.runGateError) so the pill — not a board banner — is where a
    // held queue stops being a silent mystery. Both probes are TTL-cached —
    // safe on this hot path.
    skills: Object.assign({ runHeld: !!sessionsMod.runGateError() }, skillsMod.status()),
    progress: {
      setupDone: setupDone,
      requirementsVerified: reqVerified,
      yamlPasted: yamlPasted,
      agentsInstalled: agentsInstalled,
      // skills-runtime readiness (additive; does not gate setupDone)
      skillsManifestPresent: skillsManifestPresent,
      // Per-gate detail (fsDone = the raw validator; overridden = a manual
      // "mark done") so the Setup panel can offer the escape-hatch toggle.
      setupGates: {
        requirementsVerified: { fsDone: reqVerifiedFs, overridden: reqOverride },
        yamlPasted:           { fsDone: yamlPastedFs, overridden: yamlOverride },
        agentsInstalled:      { fsDone: agentsInstalledFs, overridden: agentsOverride }
      },
      wizardStepsDone: wizardStepsDone,
      stepStatus: stepStatus,
      inProgress: locks,
      locksAvailable: lockRead.available,
      locksErrorCode: lockRead.errorCode,
      requests: requests,
      requestsAvailable: requestRead.ok,
      requestsErrorCode: requestRead.ok ? null : requestRead.code,
      // Durable marker-first recovery state. Read independently of INDEX.json,
      // so a crash after todo -> done but before index regeneration remains
      // visible and recoverable on the Board.
      finalizations: finalizationsMod.list().map(finalizationsMod.publicProjection).filter(Boolean),
      // Integration transactions (plan §10): a sealed candidate waiting for the
      // owner's Integrate, or one mid-flight/needing recovery. Read from the
      // WAL, so an interrupted transaction stays visible across a restart.
      integrations: integrationsMod.projection().records,
      // Sealed candidates awaiting the owner's Integrate (and the ones whose
      // target moved and need a fresh run before they can be integrated).
      integrationReady: worktreeManagerMod.readyForIntegration(),
      // Advisory-only, source-hash-fenced backlog previews. Missing/failed
      // previews never alter a task column or task-prep eligibility.
      shallowIntake: shallowIntakeMod.snapshot(),
      creationRecoveries: creationRecoveryState.incomplete,
      editRecoveries: editRecoveryState.incomplete,
      publicationRecoveryIssues: publicationRecoveryIssues,
      taskTiming: persisted.taskTiming || {},
      // Per-task column spine for Task Details Activity lifecycle bands
      // (backlog→pending→todo→done). Built by the SSE poll
      // (timing.reconcileLifecycle); rides this snapshot's stringify so a
      // column move fires a 'change' SSE on its own.
      taskLifecycle: persisted.taskLifecycle || {},
      // Stems on the current board (INDEX.json, all columns) — the header
      // Sessions menu filters finished sessions against this so only
      // project-current ones show. See readIndexSnapshot().
      boardStems: boardStems,
      boardIndexAvailable: indexRead.available,
      boardIndexRevision: indexRead.available ? indexRead.revision : null,
      boardLastFetchedAt: new Date().toISOString()
    },
    status: statusModel.computeStatus({
      locks: locks,
      locksAvailable: lockRead.available,
      locksErrorCode: lockRead.errorCode,
      requests: requests,
      requestsAvailable: requestRead.ok,
      requestsErrorCode: requestRead.ok ? null : requestRead.code,
      heartbeat: heartbeat,
      lastDrainedAt: act.lastDrainedAt,
      lastActivityAt: act.lastActivityAt
    })
  };
}

module.exports = {
  activityTracker: activityTracker,
  tasksIndexMtime: tasksIndexMtime,
  readIndexSnapshot: readIndexSnapshot,
  deriveState: deriveState
};
