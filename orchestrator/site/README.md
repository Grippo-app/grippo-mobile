# orchestrator/site/

## What this is

A single-page static site + local Node control server that turns
`orchestrator/launch.md` and the skill docs into a step-by-step UI.
It walks a developer through bootstrapping a fresh KMP (Kotlin Multiplatform) project against
the skills under `orchestrator/skills/` and, once the bootstrap is done, helps drive task files
through the skill pipeline.

The server does not edit product source files. It serves static files, observes
the filesystem so the UI can flip step indicators live, persists the few values
the user types, and can enqueue or spawn Claude CLI sessions that perform
product mutations unless `RUNNER_DISABLED` is set. Backlog intake is the narrow
exception on orchestration state: the server deterministically publishes the
task Markdown + canonical `INDEX.json` itself, then runs an asynchronous shallow
AI assessment that is advisory and never owns task identity or task state.

## How to run

One command, from the project root (the parent of `orchestrator/`):

```
npm start
```

Then open <http://localhost:8000/site/>. Startup fails closed if that port is
already occupied, so an accidental second server cannot silently attach to the
same project runtime on another port.

Requirements: Node 22. A root `.nvmrc` pins full checkouts, while the matching
`orchestrator/.nvmrc` travels with copied templates and CI. The Site server uses
zero npm dependencies; optional sidecar commands use their own installed
packages. Repository task-state helpers require Python 3.10 or newer, and CI
pins Python 3.10.

You can deliberately choose another port with `PORT=8001 npm start`.

Set `RUNNER_DISABLED=1` to start the server without the background queue runner
(no prep/answers/run/drop task sessions are spawned by `runner.js`). Deterministic
backlog creation still works without Claude; its separate shallow advisory
intake may report preview unavailable when no model runtime is available. Task
sessions run strictly one at a time: all of them share one working tree, so the
concurrency cap is frozen at `MAX_PARALLEL=1` in `server/runner.js` (no
environment override) until per-task worktree isolation lands. The cap is also
enforced against durable evidence, not only this process's memory: the runner
holds the queue while any board-task (`task-session`) writer lease is active
outside this live process (an orphan child of a dead site process, a standby
`/serve-queue` execution, another site process), stands down while a fresh
`.runner-alive` marker is owned by a different live process, and keeps its own
marker fresh through a CLI auth flip while one of its children still owns a
board-task writer lease (non-writer children — skills installs, read-only
terminals — do not block a standby takeover).
`task-session` writer leases are mutually exclusive across stems and drainers
at acquire time (`finalizations.beginMutation` and the guarded
`writer-lease.mjs acquire`), which is what makes the serial guarantee hold
across process boundaries.

At every server start, the runner stays closed until deterministic create/edit
publication recovery, exact writer-lease reconciliation, and one fresh composite
task-integrity scan all succeed. Recovery retries automatically with bounded
backoff. It never clears an owner by age: live, foreign, unknown, reused-PID, and
unexpired bounded generations remain protected. The current process-local verdict
is exposed as `startupRecovery` in `/api/state`; diagnostics and exact recovery
controls remain available while the background runner and Board mutation
controls are paused.

## Run the generated app

The **Run app** split button in the Site header owns the local Android-emulator
and iOS-Simulator workflow. The main button opens the current run or starts the
selected target; its options menu selects the platform, virtual device, build
variant, and build mode. A run opened from a Board task or Design surface keeps
that context for screenshots and manual validation receipts.

App Run requires a generated project and the canonical fields in
`orchestrator/project-config.md`: `applicationId`, `iosEnabled`, and
`androidAssembleTask`. The optional `orchestrator/app-run.json` overrides the
default run targets. When the file is absent, the Site uses this effective
configuration:

```json
{
  "schemaVersion": 1,
  "android": {
    "module": "androidApp",
    "variants": [
      {
        "id": "debug",
        "label": "Debug",
        "assembleTaskRef": "project-config.androidAssembleTask"
      }
    ]
  },
  "ios": {
    "project": "iosApp/iosApp.xcodeproj",
    "scheme": "iosApp",
    "configurations": [
      {
        "id": "debug",
        "label": "Debug",
        "configuration": "Debug"
      }
    ]
  }
}
```

Use an explicit manifest only when the generated project has different module,
Xcode project, scheme, configuration, or display labels. The Android `module`
must match the module in `project-config.md`'s `androidAssembleTask`. Set either
platform to `null` to disable it. If `iosEnabled` is `false`, the Site ignores
the iOS manifest section.

### Android

App Run supports Android emulators, not physical Android devices. It requires:

- a generated `androidApp/` and the repository Gradle wrapper;
- a macOS or Linux process environment; the current runner does not execute
  the native Windows `gradlew.bat` toolchain;
- one unambiguous Android SDK, resolved from `ANDROID_SDK_ROOT`,
  `ANDROID_HOME`, `local.properties`, or the standard SDK location;
- `adb`, the emulator executable, and either `apkanalyzer` or `aapt`;
- an existing AVD, or an installed system image plus `avdmanager` to create one.

Open the options menu, select Android, choose an emulator and variant, then
choose a build mode. **Rebuild & run** always builds; **Build if needed** reuses
only a verified compatible artifact; **Install last build** requires a stored
verified artifact and asks before using one older than the current source.
Stopped AVDs are booted automatically. Device creation always shows a
confirmation preview and can optionally run the app after creation.

### iOS

iOS App Run is available only on macOS. It requires:

- `iosEnabled: true`, a generated `.xcodeproj`, and the configured shared
  scheme/configuration;
- Xcode command-line tools, completed first-launch/license setup, and an
  installed iOS Simulator runtime;
- an existing Simulator, or an installed device type/runtime pair from which
  the Site can create one.

The Site builds with `xcodebuild` for `iphonesimulator`, keeps Derived Data
inside the private App Run cache, boots the selected Simulator, and opens the
Simulator application when the macOS `open` tool is available.

### Run status and troubleshooting

The run drawer shows detecting, device startup, build, artifact validation,
install, and launch stages. It also exposes bounded redacted logs, cancel,
stop, restart, screenshot, and task-checklist validation when those actions
apply.

| Symptom | Action |
| --- | --- |
| Project or platform is unavailable | Complete Setup and generate the configured Android/iOS project first. |
| Android SDK is invalid or ambiguous | Keep one valid SDK source; remove conflicting `ANDROID_SDK_ROOT`, `ANDROID_HOME`, or `sdk.dir` values. |
| Android toolchain is incomplete | Install `adb`, Emulator, and `apkanalyzer` or `aapt`; install `avdmanager` only when device creation is needed. |
| iOS requires macOS, Xcode, or a runtime | Run on macOS, finish Xcode first-launch/license setup, and install an iOS Simulator runtime. |
| Device or source is stale | Use **Refresh**, select the target again, and retry against the new source revision. |
| Project is busy | Leave **Run when the project is ready** enabled, or wait for the current project writer to finish. |
| Build, install, or launch failed | Open the run logs; raw credentials and absolute private paths are redacted. |
| Runtime recovery is required | Restart the Site once. If the issue remains, preserve `.cache` evidence and inspect diagnostics; do not delete retained records by hand. |

Runtime jobs, verified artifacts, logs, screenshots, and validation receipts
live under `orchestrator/.cache/runtime/app-run/` and are gitignored. They are
runtime evidence, not product source and not committed configuration.

## What lives where

- **App-run contracts** — `orchestrator/site/contracts/app-run/`. These schemas
  describe the optional project-level `orchestrator/app-run.json` manifest, the
  public run-job response, and manual validation receipts. The project manifest
  stays beside `project-config.md`; its schemas stay with the Site feature that
  owns the emulator/device runner.
- **Form values, manual confirmations, task timing/lifecycle history, Backend active environment, and the UI language** —
  `orchestrator/.cache/site/.site-state.json` (gitignored). Persists the Setup form, manual step
  overrides, `taskTiming`, `taskLifecycle`, `backendActiveEnvironmentId` (a workspace-local selection,
  never written to the committed source manifest), and `uiLang` (the EN/RU/UA toggle — server-persisted because
  localStorage is origin-bound and an explicitly changed port changes the origin, resetting it);
  everything else in `/api/state` is derived live.
  Steps 0 and 1 are collected by the Setup panel's Copy-as-prompt CTA, not by a checkbox.
  Step 12 gates on a live FS validator (the comment/string-aware foundation-integrity
  stub scan, `server/foundation-stub-scan.js`), not a persisted value.
  The file is
  rewritten on every POST `/api/state-patch`. To return the UI to a fresh state, either
  delete the file manually or POST `/api/reset` (the endpoint is kept
  for tooling; the UI no longer surfaces a reset button).
  Because this file is gitignored, a freshly cloned (or `/api/reset`'d)
  workspace has no saved form. `deriveState()` always uses the committed
  `orchestrator/project-config.md` as the stable form base and overlays the
  current Setup draft — see `server/project-config.js` (`parseConfigForm()`),
  the inverse of `helpers.buildYaml()`. Reviewer mode is projected only from
  the guarded project config reader.
- **Derived progress** — computed on every `/api/state` request from
  the project's actual filesystem. The Setup gate flips when
  `orchestrator/project-config.md` is populated and the
  skill toolkit is installed (`.claude/skills/` carries the current
  skills; the runtime is skills-only). Each Launch Wizard
  step flips when its concrete deliverable lands on disk
  (`settings.gradle.kts`, module dirs, `App.kt`, etc.).
- **Tasks board** — renders the last valid `orchestrator/tasks/INDEX.json` and
  the per-task `.md` files, while independently fetching a fresh bounded
  canonical verdict from `GET /api/tasks/integrity`. Invalid filesystem or
  runtime ownership state is shown in a separate banner and disables affected
  mutations; it is never hidden by collapsing duplicate artifacts into one
  card. Nothing here is persisted client-side.

## How live updates work

The server polls its own `deriveState()` every 1.5 seconds. If the
serialized snapshot changes (a file appeared, a value was saved), it
broadcasts a `change` event over Server-Sent Events on `/api/events`.
The client store re-fetches `/api/state`, every panel that subscribed
re-renders the surgical bits, and the open modal stays put.

This means: when Claude Code finishes a wizard step, the corresponding
indicator flips from ○ to ✓ within ~1.5 seconds without you reloading
the page. The same applies to a task moving across kanban columns.

If the SSE connection drops (sleep/wake, server restart), the browser
auto-reconnects per the server-advertised `retry: 1500`. The Board's
"Reload" button remains as a manual escape hatch if anything feels off.

## What it does NOT do

- No writes to product source files. The server writes to
  `orchestrator/.cache/site/.site-state.json` (user form input) and to the
  cache work directories (`.cache/tasks/runs/*.events.jsonl`,
  `.cache/tasks/runs/*.session.json`, `.cache/tasks/runs/.runner-alive`,
  `.cache/tasks/requests/<id>.json`, `.cache/tasks/creations/`,
  `.cache/tasks/edits/`, and
  `.cache/tasks/intake/`). It also owns the bounded orchestration-only write
  `tasks/backlog/<stem>.md` + canonical `tasks/INDEX.json` for deterministic
  create/edit, plus the narrow compare-and-swap update of
  `project-config.md` for allowlisted Figma/Reviewer settings. The server never
  edits product source files
  (`.kt`/`.kts`/`.swift`/Gradle/Xcode project files/etc.) and never runs
  Gradle. It does, however, spawn the `claude` CLI as a child process
  (the task runner, `server/runner.js`) to drain the Run-in-Claude
  queue — that child is what actually edits product files and runs builds. The
  shallow-intake child receives bounded task/active-card context and can only
  return schema-validated advisory JSON; its failure never rolls back creation.
  Its exact global owner is generation-bound (never age-reaped), and its private
  OS-temp scratch is cleaned through crash-recoverable guarded transactions
  only after the wrapper-only control channel has durably bound the exact direct
  model generation. The POSIX child remains behind a pre-exec gate until that
  CAS is acknowledged; Linux executes its inspected fd, while macOS executes a
  bounded, fsynced, double-verified private copy through a root-owned sandbox.
  Native Claude builds may execute only the per-request private model copy and
  the fixed root-owned `/usr/bin/security` Keychain helper; every other
  executable is denied, and the sandbox denies writes to the pinned generation.
  Script fixtures retain the no-fork profile. Before publishing drain, the
  wrapper inventories both its isolated macOS process group and host-wide exact
  process generations for the two allowed executable paths through `libproc`.
  A fork which changes session/PGID therefore remains bound to the unique model
  path or a post-baseline Keychain-helper generation until it exits.
  On POSIX, intake/lock/scratch targets must already satisfy the private-mode
  contract; non-private generations are rejected rather than modified or adopted.
  Release then requires that generation and its PGID gone (or an
  authenticated Windows Job drain). A wrapper death before model binding stays
  fail-closed. If both the Site and bound wrapper are killed on Darwin, restart
  may TERM→KILL only the unchanged private worker record's exact live model in
  that dead wrapper's isolated PGID; record replacement, PID reuse, or any
  ambiguous proof sends no signal. Unknown,
  linked, or corrupt scratch evidence is retained rather than recursively
  deleted; one retained orphan cannot block an unrelated fresh advisory.
  Mutations also still happen when you paste a prompt into Claude Code
  by hand.
- No external telemetry and no analytics. Bounded local create/intake lifecycle
  events remain under `.cache/tasks/` for recovery diagnostics. The only direct third-party network call is the optional Claude Code
  subscription usage probe in `server/cli.js`: it reads the local OAuth token from the OS keychain and
  calls Anthropic's usage endpoint to show remaining limits; failures degrade to hidden usage rows.
- Guarded event append is serialized by an exact-generation, target-keyed
  CAS/WAL transaction; its limit check and publication are one recoverable
  operation. Guarded mutations require a proven directory flush and fail
  closed on hosts where Node cannot provide one (including the current native
  Windows directory-handle path); guarded reads remain available there.
- No npm dependencies. The server is plain Node — a thin `server.js`
  entry plus a `server/` directory of single-purpose CommonJS modules:
  `paths.js`, `fsutil.js`, `file-guards.js`, `file-guard-worker.js`,
  `persistence.js`, `locks.js`, `requests.js`, `task-integrity.js`,
  `runtime-integrity.js`, `writer-lease-inspector.js`, `backlog-create.js`,
  `creation-markers.js`, `edit-markers.js`,
  `shallow-intake-contract.js`, `shallow-owner-guard.js`,
  `windows-runtime-proof.js`, `shallow-intake.js`,
  `validators.js`, `foundation-stub-scan.js` (Step 12's comment/string-aware stub scan — one implementation
  behind both the wizard ✓ validator and the CLI gate the bootstrap agent runs), `project-config.js`,
  `state.js`, `startup-recovery.js`, `sse.js`, `static.js`, `http.js`, `cli.js`, `child-env.js`, `figma.js`, `figma-session-actions.js`,
  `figma-evidence.js`, `figma-generation.js`, `figma-integration.js`, `figma-screens.js`,
  `figma-sync.js`, `figma-sync-history.js`, `figma-task-publication.js`, `figma-test-job.js`,
  `figma-token-jobs.js` (the sanitized-env spawner of the trusted runner child — token + component pipelines),
  `design-catalog.js`, `design-comparison.js`, `design-history.js`,
  `mapping-mutation.js` (shared exact operation-receipt/idempotency envelope),
  `design-mappings.js` (CAS token-mapping mutations under the sole-writer lease),
  `design-domain-freshness.js` + thin token/component configs (shared startup reconcile + adapter-root watcher; all comparison execution uses the one durable `figma-sync` lifecycle),
  `design-token-state.js` (cheap poll-safe adapter/mapping/project-dirty signals),
  `design-component-mappings.js` (CAS component-mapping mutations — the Mapping Review ops, incl. `set-render-class` — under the sole-writer lease),
  `design-component-compare.js` (component-domain freshness configuration),
  `design-component-state.js` (cheap poll-safe component adapter/mapping/project-dirty signals),
  `design-overview.js`, `design-preview.js`, `design-relations.js`,
  `design-task-actions.js`, `task-source.js`, `api-contract.js`, `api-report-state.js`,
  `api-project-inputs.js`, `api-relations.js`, `api-catalog.js`,
  `api-overview.js`, `api-changes.js`, `api-change-reviews.js`,
  `api-task-actions.js`, `api-mock.js`,
  `backend-environments.js`, `backend-credentials.js`, `backend-integration.js`, `contract-session-actions.js`,
  `contract-job.js`, `contract-history.js`, `contract-generation.js`,
  `arch.js`, `sessions.js`, `runner.js`,
  `worker.js`, `git.js`, `status.js`, `timing.js`, plus `tasks-log.js`
  (the per-task pipeline journal reader). Each server module is
  `require`/`module.exports` only. `scripts/package.json` is only the zero-dependency
  ESM boundary that lets the server import the browser's canonical Figma prompt
  builders; it defines no package scripts and requires no install.
- Figma session starts contain only `{key, figmaAction}`. The terminal is
  read-only for Figma sessions: even `needs_action` cannot reopen a free-text
  writer turn; resolve the stated precondition and rerun the typed action.
- No `file://` mode. The dev server is the only supported entry point.

## Panel summary

- **Setup** — collects project values and produces the YAML for
  `orchestrator/project-config.md`. The three setup
  gates (skills present, config populated, skill toolkit installed)
  are live FS checks, each with a manual-override escape hatch ("mark
  done manually" / "clear", OR-combined into the gate server-side) for
  when the check can't see a satisfied state. The Continue button enables
  automatically when all three flip to ✓. The "Next steps" area also
  carries the bootstrap prompt and a collapsed manual-install reference
  for `.claude/skills/`.
- **Launch Wizard** — Steps 2–14 (plus Steps 4.5, 7.5, 7.6, and 7.7). Most steps
  show a read-only ✓/○ indicator that the server flips when the step's
  deliverable lands on disk. Step 12 (end-to-end verify) gates on a live
  FS validator (the comment/string-aware foundation-integrity stub scan —
  the same `server/foundation-stub-scan.js` the step's CLI gate runs, so the
  ✓ flips on its own the moment the agent's gate passes), not a manual
  checkbox — see the auto-run note below. Steps 0, 1, and 1.5 are intentionally
  absent from the wizard: the Setup panel's "Copy as Claude prompt" CTA
  already collects that context, so the wizard starts at Step 2.
  Conditional Steps 6.5 (Figma) and 6.6 (Backend contract) intentionally
  have no wizard card — they run via the Figma and Backend panels respectively.
  When the Claude CLI is installed + signed in, the header carries a
  **Run all remaining steps** button (`scripts/auto-run.js`): it sends each
  not-yet-done step's prompt into the shared `setup` session in order,
  waiting for the turn to finish AND the step's ✓ to land before sending the
  next. It pauses (never silently skips) if a step asks a question — the user
  answers in the terminal and it continues automatically — or if a step's ✓
  never lands within the grace window (so a failed build can't cascade into
  the steps below it). Step 12 carries a live FS validator (the comment/string-aware
  foundation-integrity stub scan), so a clean turn alone does not auto-tick it — if a
  real code-position stub survives in foundation commonMain its ✓ never lands and the
  run pauses unverified; comments or strings that merely mention the stub markers
  do not hold the ✓ back.
- **Architecture** — product view over the validated Architecture Map v2.
  Overview metrics distinguish `Unknown`/partial coverage from zero; the
  URL-backed catalog searches and filters modules, features, screens and data
  entities; findings include severity, confidence and bounded evidence.
  Entity drawers answer “Used by”/“Uses”, link current findings/tasks and
  paginate precomputed relations. The latest task-linked structural diff is
  kept separate from later manual refreshes; its v2 envelope carries complete
  totals plus an explicit identifier-truncation flag, so the exact Changed
  filter fails closed when a diff is shortened. The bounded graph is a secondary
  view with zoom/pan, keyboard controls, finding text markers and a complete
  textual fallback for both entities and relations.

  `/api/architecture/*` provides validated overview/list/detail/findings/diff,
  graph and typed generation/task actions. List cursors are bound to the
  structural hash and filter hash; rows and complete responses have hard caps.
  Generation runs as a typed SSE-visible job under the shared project writer
  lease, and a child-side exact lease fence prevents publication after
  ownership loss. Job recovery validates at most 500 exact reports and retains
  every active report plus the newest 100 terminal reports. Missing but ready
  projects can generate the map; unfinished
  projects route back to Setup. Failed/interrupted jobs preserve the previous
  map as stale. Architecture accepts only the canonical v2 map; unsupported
  shapes fail closed and must be regenerated.
- **Design** — a generation-bound workbench with four stable tabs:
  **Overview**, **Tokens**, **Components**, and **Surfaces**. Every response is
  pinned to one committed design generation and carries its revision; stale
  mutations fail with a conflict instead of mixing generations. Overview shows
  freshness, readiness, coverage, component registration, drift findings and
  task links. The three catalogs share URL-backed search and filters, bounded
  cursor pagination, multi-select task preview/create, and deep-linkable entity
  drawers. Component details and surface variants/images are loaded only from
  detail endpoints. Screens, dialogs, and overlays are normalized into Surfaces
  with theme/locale/platform variants. Local code comparison starts in Design
  through the strict `/api/design/compare` adapter and stays on the active tab;
  live token/component refresh remains in Integrations → Figma. Design uses `/api/design/*` exclusively and
  returns an explicit empty pre-sync state until a committed generation exists;
  it never reads independently updated pre-generation artifacts. Board evidence
  and integration controls use their dedicated `/api/figma/*` endpoints, while
  catalog data is available only under `/api/design/*`. Figma access happens only through typed
  `figma:*` sessions — the site server never calls Figma directly. Integrations →
  Figma exposes token refresh, atomic component+token sync, and global drift.
  On the first token sync, an empty source index is bootstrapped from the
  configured Figma root and valid task `## Design` nodes; later refreshes read
  only those exact registered node/context sources. Component sync
  writes a witnessed component capture, visual evidence, and fixed-bucket token
  observation intake; the trusted runner publishes `design-component-inventory`
  together with `observed-token-source-index` / shards / `observed-token-catalog`
  under one generation pointer. The provider capability is usage-scoped and does
  not claim a file-wide Variables census. Global
  drift performs zero Figma reads and runs entirely as local deterministic jobs
  — the token comparator (`runtime/run-plan.mjs` op `token-compare`) plus the
  component comparator (op `component-compare`) — publishing the `token-drift`
  and `component-drift` domains per-domain, so a one-domain failure finishes
  `partial` instead of losing the other. The Design → Tokens tab projects the
  committed `token-comparison` artifact verbatim (design rows + project-only
  rows + coverage) with revision-signal staleness states, and Design →
  Components projects the committed `component-comparison` artifact the same
  way; Mapping Review mutations go
  through `POST /api/design/token-mappings` and
  `POST /api/design/component-mappings` (CAS `expectedRevision` +
  `expectedComparisonSemanticHash`; the component ops include `upsert-mapping`,
  `retire-mapping`, `set-render-class`, dispositions, `onboard-fresh`),
  single-domain recompare through `POST /api/design/token-compare` and
  `POST /api/design/component-compare`, and token detail/project-only listings
  through `GET /api/design/tokens/{id}` and `GET /api/design/tokens/project-only`.
  The plan API requires one explicit scope; there is no
  all-design/full-sync request. Task screens and per-screen drift remain
  task-scoped and are never enumerated by a global sync. A successful task pull
  must produce a fresh local gate proof; the server validates it while the
  session still owns its writer lease, persists a recovery marker, and publishes
  only that task domain. Publications and readers accept only the strict
  domain-lineage generation manifest v2, so updating global drift cannot replace
  a task's surface-drift domain. Bounded generation retention preserves every
  immutable source generation still referenced by current lineage.
- **API** — a generation-bound workbench with four stable tabs:
  **Overview**, **Endpoints**, **Changes**, and **Diagnostics**. It reads only
  the fully validated committed contract generation and never calls the
  backend. Overview combines environment/freshness context with implementation,
  missing, observed-mismatch, and breaking-change metrics; unavailable analysis
  stays explicitly unknown. Endpoints provides URL-backed filters, bounded
  opaque-cursor pagination, independent implementation/change/mismatch/task
  badges, and an on-demand detail drawer with request/response models,
  redacted explicit or deterministic generated examples, implementation
  evidence, consumers, changes, mismatches, and tasks. Changes applies the
  versioned directional semantic classifier and exposes bounded impact evidence.
  Reviewed acknowledgements are stored separately under a sole-writer lease;
  they reduce unresolved attention without modifying the immutable diff.
  Multi-select accepts at most 25 current missing/change/mismatch source ids;
  preview and create re-resolve all task prose, provenance, fingerprints,
  generation/report/task revisions, and deduplication on the server.
  Diagnostics owns doctor/analyze/diff commands, report freshness/limitations,
  and the optional one-per-project loopback mock server. The mock is pinned to
  its starting generation/hash/environment, serves deterministic fixtures on
  `127.0.0.1`, and keeps bounded request metadata without headers or bodies.
  The workbench exposes only the current `/api/api/*` HTTP surface.
- **Reviewer** — operational review status: canonical mode, resolved active
  reviewer, Codex installed/available readiness, fallback policy, last review,
  and bounded pending/failed activity from structured task journals.
- **Board** — kanban over the last valid `orchestrator/tasks/INDEX.json`, paired
  with the fresh read-only `/api/tasks/integrity` verdict and bounded
  deterministic backlog create/edit controls with four columns (Backlog,
  Pending, TODO, Done). A stale or structurally invalid corpus keeps the last
  valid projection visible, renders exact bounded recovery findings, and blocks
  affected mutations. Updates live via
  SSE when a task moves across columns. Each card opens a modal with
  the source `.md` and authoritative prep/run controls. The **+ New backlog item** composer collects the task
  per-section (Goal / Inputs / Design / Acceptance / Out of scope) and assembles
  the canonical markdown (`assembleBacklogBody`), so the section pattern can't be
  broken by hand; a **Raw markdown** toggle swaps in a freeform textarea for power
  users and non-standard sections, and the Design rows appear only when
  `figmaEnabled`. Submit returns after deterministic server-side numbering,
  no-clobber Markdown publication and canonical INDEX regeneration; it does not
  wait for Claude. A separate shallow intake may then show an advisory readiness
  preview, likely areas, possible duplicates and missing context without making
  the task runnable. System-generated tasks (Figma matrices, backend drift)
  bypass the composer but use the same idempotent `tasksApi.createBacklog`
  endpoint with a prebuilt body.
  Done cards show execution duration when the server
  captured both a lock `startedAt` and the matching INDEX `doneAt`.
  When the Figma MCP is bound AND the task carries a pullable `## Design`
  bullet (a `node-id=` Figma URL), the backlog, pending and todo task modals
  also carry a "Pull Figma screens" button that spawns a per-task
  `figma:screens:<stem>` session to
  populate the screen cache and its exact observed-token sidecars from a
  server-owned immutable capture plan. Each sidecar is bound into screen-index
  v3 by byte hash and reserved operation sequence. Once that task's screens are
  cached the button turns green ("Screens pulled · re-pull") — still
  clickable to re-pull; the per-task freshness comes from the
  `/api/state.screensCache` signal. That same signal drives an amber
  "screens not pulled" chip on a UI task's card and a confirm-on-Run gate.
  The task modal's read-only **Figma** tab shows per-screen mock previews +
  Figma links + spec/census summary from `/api/figma/screens` and
  `/api/figma/screen-image` before shipping. For done tasks with final visual
  evidence, the same tab switches to the saved visual-check artifacts from
  `/api/figma/evidence`, including the in-pane
  `Figma`/`Actual`/`Difference`/`Overlay` viewer served through the path-safe,
  report-hash-bound `/api/figma/compare-artifact` endpoint.
  Successful finalization commits the task observation receipt and durable
  ingestion intent in the same recoverable journal as task/component-mapping
  publication; startup reconciliation then applies the intent idempotently to
  the shared observed catalog.
- **Figma** — the INTEGRATION half only: per-project Figma-MCP binding
  (OAuth) + the file URL, with a "next → Design" pointer card. The pulls
  themselves are typed sync actions whose results land in the committed design
  generation consumed by the Design panel. The server owns the action-to-prompt
  binding but never calls Figma itself.
- **Backend** — the INTEGRATION half only: canonical Local/Dev/Stage/Prod
  sources, workspace-local environment selection, non-secret authentication
  status, read-only Test source previews, transactional Refresh contract, and
  bounded job history. The browser sends typed actions and preview ids; the
  server starts `api-contract/scripts/backend-action.mjs`, which alone performs
  the guarded live fetch. OpenAPI is primary; Postman enrichment and Copy prompt
  are under Advanced, while the source popup may select Postman bootstrap. The
  popup can save and Test in one action: Postman and team `*.postman.co` URLs
  select PMAK `x-api-key` authentication immediately, while a 401/403 from
  another source selects a bearer credential before the automatic retry.
  Endpoint coverage and client drift stay on Project → API.

The header carries a live **status indicator** fed by the server's
`status` block in `/api/state` (dev-server up, SSE connection, queue
depth, lock age, and an inferred Claude-activity label). Every signal
has a defined "no activity yet" rendering for a fresh repo.

The agent workforce is packaged as the 11 skills under
`orchestrator/skills/` (plus `implement-figma` from the Figma sidecar),
so there is no separate workforce browser panel or `/api/agents`
endpoint — the skills are invoked directly by Claude Code from
`.claude/skills/`.

## Maintenance

Editing the wizard? Edit files under `orchestrator/site/scripts/` and
`orchestrator/site/styles/`. There is no build step and no generator
file — the site is the source.

The frontend is native browser ES modules — no bundler, no globals.
`index.html` loads exactly one entry, `scripts/app.js`, the composition
root that wires the store, router, i18n, and status indicator. Every
module uses explicit `import`/`export`; there is no `window.App`. Panels
are declared once in `scripts/registry.js` (`id`, `navLabelKey`, `order`,
`group`, `panel`), with a sibling `GROUPS` list defining the sidebar
groups and their render region (the primary Board item plus labelled
Project / Integrations sections in the body; then the collapsible Start
footer group, which `app.js`
auto-collapses once the setup gate flips — a `null` group label renders
headerless, as Board does).
`app.js` reads both to generate the grouped nav and the route
`<section>`s, so adding a panel is a single registration there — no edits
to `index.html`, the nav, or the router. Each panel module
exports a singleton with the contract `{ mount(el), refresh()? }`.

### FS validators

The mapping "wizard step → filesystem predicate" lives in
`orchestrator/site/server/validators.js`, in the `STEP_VALIDATORS`
map (the three setup gates live alongside it in the same module). To
re-tune which file proves a step done, edit those functions. Keep
them inexpensive (existence + small reads); they run every 1.5 s.
Step 12's predicate delegates to `server/foundation-stub-scan.js` — the
comment/string-aware stub scan the bootstrap agent also runs as a CLI —
so tune that module, not the validator, and keep both consumers on the
one implementation.

### Sync points

A few content chunks are still mirrored in JS (the panels run in the
browser, so they can't `require()` Markdown):

- `CONFIG_BODY` in `scripts/panels/setup.js` mirrors the body of
  `orchestrator/project-config.md` (everything after the
  YAML frontmatter). The Setup panel's "Copy as Claude prompt" button
  embeds `helpers.buildYaml(setup) + CONFIG_BODY` into a prompt that
  instructs Claude Code to write the file. If you edit the real `.md`
  body, update `CONFIG_BODY` too; `tests/template-surfaces.test.mjs`
  rejects drift in the fresh source template.
- `helpers.buildYaml` lives in `scripts/data/wizard-steps.js` (the
  `helpers` object is the module's export) and is the single source of
  truth for the YAML frontmatter. Consumers (Setup preview, Setup Claude
  prompt) import `{ helpers }` and call it — do not re-implement.

## Portability

The site lives at `orchestrator/site/`, so it travels with
`orchestrator/` automatically when you copy that directory into a new
project. Run `npm start` from any KMP project that has an `orchestrator/`
checkout and the server will resolve the project root as the parent of
`orchestrator/` automatically.
