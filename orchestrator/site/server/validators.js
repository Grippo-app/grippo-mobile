'use strict';

// ---------------------------------------------------------------------------
// Filesystem gates + wizard-step validators. These probe <project-root>/ for
// the deliverables of each launch.md step. Anything they don't recognize is
// "not done"; the UI shows a grey indicator and you can override via the
// manual flag.
//
//   - The three "setup is ready" gates (template present: config + install
//     manifest + launch playbook, YAML config populated, skills installed
//     under .claude/skills/) → setupDone.
//   - STEP_VALIDATORS: per-wizard-step done predicates, keyed by the same
//     `id` scripts/data/wizard-steps.js uses.
// ---------------------------------------------------------------------------

var path   = require('path');
var paths  = require('./paths');
var fsutil = require('./fsutil');

var ORCHESTRATOR_DIR = paths.ORCHESTRATOR_DIR;
var PROJECT_ROOT     = paths.PROJECT_ROOT;

var exists           = fsutil.exists;
var readUtf8         = fsutil.readUtf8;
var fileContains     = fsutil.fileContains;
var countFilesIn     = fsutil.countFilesIn;
var findRecursive    = fsutil.findRecursive;
var findContentMatch = fsutil.findContentMatch;

// Threshold for `validateAgentsInstalled`. The bootstrap installs the core
// skills into .claude/skills/ (11 directories via
// orchestrator/skills/install-skills.sh). The gate uses `>=`, so a project with
// extra (user-added) skills still passes; bump in lockstep with the skill count.
var SKILLS_MIN_COUNT = 11;

// ---------------------------------------------------------------------------
// Setup-level validators (the three "setup is ready" flags + the derived
// setupDone gate that controls the Wizard panel).
// ---------------------------------------------------------------------------

var PLACEHOLDER_RE = /<(Product|product|product-domain|typeface|org|firstDomain|Entity|X|x|IosFrameworkName|iosFrameworkName)>/;

function validateRequirementsVerified() {
  // "Template present" signal: the live config, installed-skills manifest,
  // and launch playbook.
  return exists(path.join(ORCHESTRATOR_DIR, 'project-config.md'))
      && exists(path.join(ORCHESTRATOR_DIR, 'skills', '_index', 'install-manifest.json'))
      && exists(path.join(ORCHESTRATOR_DIR, 'launch.md'));
}

function validateYamlConfigPopulated() {
  var p = path.join(ORCHESTRATOR_DIR, 'project-config.md');
  var text = readUtf8(p);
  if (text === null || text === undefined) return false;
  // Normalize CRLF → LF first: a config authored on Windows has "\r\n---\r\n"
  // fences, so the "\n---\n" delimiter search below would miss and the gate
  // would never flip. Stripping \r makes the check line-ending agnostic.
  text = text.replace(/\r\n/g, '\n').replace(/\r/g, '\n');
  // A leading UTF-8 BOM (U+FEFF) would push "---" off offset 0 and fail the
  // check below; strip it after the line-ending normalization.
  text = text.replace(/^\uFEFF/, '');
  // Frontmatter is between the first two "---" delimiters at the top.
  if (text.indexOf('---') !== 0) return false;
  var end = text.indexOf('\n---\n', 3);
  // Also accept a closing fence at end-of-text: a config whose final "---"
  // sits at EOF (no trailing newline) ends in "\n---" + optional whitespace,
  // which the "\n---\n" search above can't match.
  if (end < 0) {
    var tail = text.search(/\n---\s*$/);
    if (tail < 3) return false;
    end = tail;
  }
  var fm = text.substring(3, end);
  // Reject if any placeholder still in the frontmatter (e.g. "<Product>")
  // OR if any required key is missing.
  if (PLACEHOLDER_RE.test(fm)) return false;
  var required = ['productName:', 'productPackage:', 'backendHost:', 'applicationId:'];
  for (var i = 0; i < required.length; i++) {
    if (fm.indexOf(required[i]) < 0) return false;
  }
  return true;
}

function validateAgentsInstalled() {
  // Skills era: the deploy step installs skill DIRECTORIES into .claude/skills/
  // (each holds a SKILL.md). Count those canonical skill directories only.
  var dir = path.join(PROJECT_ROOT, '.claude', 'skills');
  var fs = require('fs');
  var n = 0;
  try {
    fs.readdirSync(dir, { withFileTypes: true }).forEach(function (e) {
      if (e.isDirectory() && fs.existsSync(path.join(dir, e.name, 'SKILL.md'))) n++;
    });
  } catch (e) { return false; }
  return n >= SKILLS_MIN_COUNT;
}

// ---------------------------------------------------------------------------
// Wizard step validators. A null entry in STEP_VALIDATORS means "no validator
// — done is whatever the user confirmed in persisted.manualSteps". A function
// entry derives done from the filesystem; persisted.manualSteps[id]
// additionally serves as a per-step override that forces done=true even when
// the validator is false (see deriveState in state.js).
//
// Steps that follow the canonical shape "Step N drops file X under module dir
// Y, containing class Z" are declared as data in STEP_CHECKS — adding such a
// step is a one-row change. Steps with bespoke logic declare a full function.
// ---------------------------------------------------------------------------

// STEP_CHECKS — each entry is either a single check `{ dir, file, regex }`
// or an array of them (AND'ed). Declarative checks require their canonical
// `dir/file`; regex scans are reserved for bespoke validators below.
var STEP_CHECKS = {
  // Step 3 already scaffolds build.gradle.kts for every module, so each
  // implementation step's marker has to look for actual code, not the
  // scaffold. Fingerprints survive in-place file renames.
  '4': {
    // BaseViewModel is the canonical file; the regex also catches the
    // other Base* spine classes (BaseRouter/Loader/Result/...) which
    // all belong to Step 4's deliverable set.
    dir: 'ui-core/foundation', file: 'BaseViewModel.kt',
    regex: /\bclass\s+Base\w+\b/
  },
  '5': [
    { dir: 'toolkit/logger',      file: 'AppLogger.kt',
      regex: /\b(?:class|object)\s+AppLogger\b/ },
    // ApiErrorParser is the canonical `:toolkit:http-client` artefact —
    // a `HttpClient.kt` filename would collide with Ktor's own type.
    { dir: 'toolkit/http-client', file: 'ApiErrorParser.kt',
      regex: /\b(?:class|object)\s+ApiErrorParser\b/ }
  ],
  '6': {
    dir: 'design-system/core', file: 'AppTokens.kt',
    regex: /\b(?:class|object)\s+AppTokens\b/
  },
  '7': [
    { dir: 'data-services/backend',  file: 'BackendClient.kt',
      regex: /\b(?:class|object)\s+BackendClient\b/ },
    // Room's @Database annotation is the most reliable fingerprint —
    // it survives renames like Database → AppDatabase.
    { dir: 'data-services/database', file: 'Database.kt',
      regex: /@Database\s*\(/ }
  ],
  '9': {
    dir: 'shared', file: 'RootComponent.kt',
    regex: /\bclass\s+RootComponent\b/
  },
  // Step 4.5 lays the error contracts: `AppError` + `ErrorProvider` in
  // `:ui-core:error:error-provider`, plus a partial seed of `AppErrorState`
  // in `:ui-core:state` (package `core.state.error`). Both files must
  // exist for the step to be considered done.
  '4.5': [
    { dir: 'ui-core/error/error-provider', file: 'AppError.kt',
      regex: /\bsealed\s+class\s+AppError\b/ },
    { dir: 'ui-core/state',                file: 'AppErrorState.kt',
      regex: /\bsealed\s+class\s+AppErrorState\b/ }
  ],
  // Step 7.5 implements the `:ui-dialog-features:dialog-api` contract
  // module. `DialogConfig` is the canonical fingerprint — it's the
  // sealed class every concrete dialog config subtypes.
  '7.5': {
    dir: 'ui-dialog-features/dialog-api', file: 'DialogConfig.kt',
    regex: /\bsealed\s+class\s+DialogConfig\b/
  },
  // Step 7.6 implements `:ui-core:error:error-provider-impl` — the
  // module that closes the loop from `AppError` to the error dialog.
  '7.6': {
    dir: 'ui-core/error/error-provider-impl', file: 'ErrorProviderImpl.kt',
    regex: /\bclass\s+ErrorProviderImpl\b/
  },
  // Step 7.7 implements `:ui-dialog-features:error-display` — the MVI
  // dialog that renders the error pipeline's output. The public
  // `ErrorDisplayComponent` factory is the canonical fingerprint.
  '7.7': {
    dir: 'ui-dialog-features/error-display', file: 'ErrorDisplayComponent.kt',
    regex: /\bErrorDisplayComponent\b/
  }
};

function runCheck(check) {
  var p = path.join(PROJECT_ROOT, check.dir);
  return findRecursive(p, check.file);
}

function runChecks(stepId) {
  var spec = STEP_CHECKS[stepId];
  if (!spec) return false;
  var checks = Array.isArray(spec) ? spec : [spec];
  for (var i = 0; i < checks.length; i++) {
    if (!runCheck(checks[i])) return false;
  }
  return true;
}

// Adapter — STEP_VALIDATORS expects functions, STEP_CHECKS holds data.
function checked(stepId) { return function () { return runChecks(stepId); }; }

// ---- Validator functions for shapes that don't fit STEP_CHECKS -----------

function validateStep2() {
  return exists(path.join(PROJECT_ROOT, 'gradle', 'libs.versions.toml'))
      && exists(path.join(PROJECT_ROOT, 'gradle.properties'))
      && exists(path.join(PROJECT_ROOT, 'settings.gradle.kts'))
      && exists(path.join(PROJECT_ROOT, 'build-logic', 'build.gradle.kts'))
      && exists(path.join(PROJECT_ROOT, 'gradlew'))
      && exists(path.join(PROJECT_ROOT, 'gradle', 'wrapper', 'gradle-wrapper.jar'));
}

function validateStep3() {
  var settings = path.join(PROJECT_ROOT, 'settings.gradle.kts');
  // Module marker — any one of the foundation toolkit modules wired in.
  return fileContains(settings, ':toolkit:context')
      && fileContains(settings, ':ui-core:foundation');
}

function validateStep8(setupForm) {
  var first = (setupForm && setupForm.firstDomain ? String(setupForm.firstDomain) : '').toLowerCase();
  if (!first) return false;
  // Guard against path-injection / odd characters before joining into a path.
  // firstDomain is user input persisted to .cache/site/.site-state.json; the slug must
  // be safe to use as a directory name.
  if (!/^[a-z0-9_-]+$/.test(first)) return false;
  // Step 8 lands a slice across data-features + ui-screen-features for <firstDomain>.
  return exists(path.join(PROJECT_ROOT, 'data-features', first))
      && exists(path.join(PROJECT_ROOT, 'ui-screen-features', first));
}

function validateStep10() {
  // Android App.kt is a tiny Application subclass; the regex matches
  // the `: Application()` superclass marker so a rename like
  // `App.kt` → `<Product>App.kt` still resolves.
  return exists(path.join(PROJECT_ROOT, 'androidApp', 'build.gradle.kts'))
      && runCheck({ dir: 'androidApp', file: 'App.kt',
                    regex: /:\s*Application\s*\(\s*\)/ });
}

function validateStep11(setupForm) {
  // Auto-skipped (= done) when iOS is disabled in setup.
  if (setupForm && setupForm.iosEnabled === false) return true;
  // A runnable iOS shell needs the Xcode project, a SHARED scheme (what makes
  // the iosApp run config appear in Android Studio and lets `xcodebuild -scheme
  // iosApp` resolve), and the @main entry point. A lone iOSApp.swift is not
  // enough to prove the shell is runnable.
  return exists(path.join(PROJECT_ROOT, 'iosApp', 'iosApp.xcodeproj', 'project.pbxproj'))
      && exists(path.join(PROJECT_ROOT, 'iosApp', 'iosApp.xcodeproj', 'xcshareddata', 'xcschemes', 'iosApp.xcscheme'))
      && findContentMatch(path.join(PROJECT_ROOT, 'iosApp'), /@main\s+struct\b/);
}

// Step 12 — the foundation-integrity gate, made real server-side. A stubbed
// load-bearing branch (e.g. DialogConfig.ErrorDisplay -> createChild) compiles
// green — TODO(...) returns Nothing — then crashes with NotImplementedError
// the first time any error fires, so Step 12 is an 'fs' step: auto-run
// genuinely gates on the ✓ path instead of trusting a clean turn, and the step
// stays not-done (auto-run pauses 'unverified') while a stub survives.
//
// The scan itself lives in foundation-stub-scan.js — ONE comment/string-aware
// implementation shared with the CLI gate the bootstrap agent runs in Step 12
// (`node orchestrator/site/server/foundation-stub-scan.js`), so the agent's
// gate and this ✓ cannot disagree: the moment the CLI passes, the wizard marks
// the step done on its own. Comment-awareness matters — a raw regex flags the
// very comments agents write about the trap, permanently wedging the ✓.
var foundationStubScan = require('./foundation-stub-scan');

function validateStep12() {
  return foundationStubScan.scanFoundationStubs(PROJECT_ROOT).length === 0;
}

function validateStep13() {
  return exists(path.join(PROJECT_ROOT, 'CLAUDE.md'));
}

// stepId → either a validator function (called with optional setupForm) or
// null (no validator — done depends purely on persisted.manualSteps).
// `checked(<id>)` dispatches to the declarative STEP_CHECKS entry above.
// Steps 0, 1, 1.5 are intentionally absent — the Setup panel's "Copy as
// Claude prompt" CTA already collects the context, has the agent read
// orchestrator/, and writes project-config.md in one shot, so the
// wizard starts at Step 2.
var STEP_VALIDATORS = {
  '2':   validateStep2,
  '3':   validateStep3,
  '4':   checked('4'),
  '4.5': checked('4.5'),
  '5':   checked('5'),
  '6':   checked('6'),
  '7':   checked('7'),
  '7.5': checked('7.5'),
  '7.6': checked('7.6'),
  '7.7': checked('7.7'),
  '8':   validateStep8,
  '9':   checked('9'),
  '10':  validateStep10,
  '11':  validateStep11,
  '12':  validateStep12,                 // e2e verify + foundation-integrity stub gate
  '13':  validateStep13,
  '14':  validateAgentsInstalled
};

module.exports = {
  PLACEHOLDER_RE: PLACEHOLDER_RE,
  validateRequirementsVerified: validateRequirementsVerified,
  validateYamlConfigPopulated: validateYamlConfigPopulated,
  validateAgentsInstalled: validateAgentsInstalled,
  STEP_VALIDATORS: STEP_VALIDATORS
};
