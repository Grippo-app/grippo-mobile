## Current contract follow-up (2026-10-14)

The [current analytics review](../COVERAGE.md#current-analytics-contract-review-2026-10-14)
records this revision and its verification limits. Typed dialog observation names
use DismissRequested across screens/dialogs, with dismiss_requested wire names/codes.
The app has not been released; all analytics schemas use initial version 1.
This feature's operation schemas use version 1 and omit visible-screen context;
feature operation codes and operation_id own outcome correlation. Earlier build
statements below describe their original source revisions, not released contracts.

# Exercise example dialog coverage

Scope: `ui-dialog-features:exercise-example`, reviewed 2026-10-13.

## Presentation gate

| Route / surface | Component → VM | Presentation classification |
|---|---|---|
| `DialogConfig.ExerciseExampleDetails`, Default or Action mode; Information and Achievements tabs | ExerciseExampleDetailsComponent → ExerciseExampleDetailsViewModel | Live overlay; default null screenPresentation, no dedicated dialog presentation contract |

Both modes and tabs render one dialog, including loading, empty, available and
unavailable 1RM states. The
shared DialogContentComponent also creates it for nested Exercise detail actions.
There are no independent screens or child screen VMs in this module. The existing
shared foundation presentation boundary is unchanged. Analytics is injected in the
normal retainedInstance factory; shared registers ExerciseExampleAnalyticsModule.

## Observed callbacks and operations

| Owner / callback | Event / operation | Boundary |
|---|---|---|
| VM `onDismiss` | `exercise_example_dismiss_requested` | Existing hardware Back callback before Back direction; no inferred reason |
| Segment → Contract → VM `onTabSelected` | `exercise_example_tab_selected`, bounded information/achievements enum | Real selection callback, including clicking the already selected tab; initial/default/restored widget selection emits nothing |
| VM `onAction` | `exercise_example_action_clicked` | Before existing Action direction; button rendered only in Action mode; label and downstream result omitted |
| Existing startup safeLaunch | LoadExample | Complete getExerciseExampleById refresh block; intrinsic example/component domain values, child ownership and unique primary IDs are validated before insertion |
| Finite observeRecentExercises Flow combined with the example observer under Processing.Infinity | LoadRecentExercises | Request, chronological history mapping and volume publication; response rows must belong to the requested example and nested iterations to their enclosing exercise, with unique primary IDs. Runs once per VM, then its result feeds the observed-example one-rep-max calculation |
| Existing Achievements safeLaunch | LoadAchievements | Request, state mapping and publication |

Operations use `exercise_example_task_started/completed/failed/interrupted` v1,
with closed `exercise_example_operation` codes; terminal events include duration_ms.
The shared decorator supplies fresh operation IDs and monotonic duration, isolates
reporting Exceptions and preserves return values, original errors and cancellation.
Analytics wiring introduces no additional loader, job, dispatcher or retry.
The LoadRecentExercises boundary covers the request, chronological recent-record
mapping, volume calculation and their State publication. Estimated 1RM now derives
downstream from the emitted records combined with the observed example; it is
outside this operation's completion/error boundary. The operation's completed
outcome therefore confirms recent-record and volume publication, not completion
of the dependent 1RM calculation.
All seven schemas register once under the unique exercise_example_analytics qualifier.

Deliberate omissions: dialog view/close, swipe/outside dismissal, entry/referrer,
mode/action label, downstream action success, data-flow emissions and muscle summary
recomputation and local chart tooltip/exploration. No callback or routing context
is added to observe unavailable facts.
No exercise IDs, titles, URLs, history counts, body measurements, weights,
achievements, request/response payloads or exception text are encoded.

## State preservation gate

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| Shared dialog host | Session, id, mode/title, action/back callbacks and nested step | Runtime only | Existing retained host; slot/step serializer = null | Ordinary recreation can retain the session; process death closes it |
| ExerciseExampleDetailsViewModel | mode | Runtime only | Existing DialogConfig input | Fresh opening uses supplied mode; no editable draft |
| ExerciseExampleDetailsViewModel | example, muscleLoad | Rebuild | ExerciseExampleFeature observer/refresh; pure MuscleLoadingSummaryUseCase.fromExerciseExample(observedExample) | Fresh opening observes current data and derives anatomy directly, without re-reading a separate cache snapshot |
| ExerciseExampleDetailsViewModel | recent, exerciseVolume, estimatedOneRepMax (dated source set and External/Effective load basis) | Rebuild | ExerciseMetricsFeature, VolumeSeriesUseCase, EstimatedOneRepMaxUseCase with the observed route example and its component requirements | Startup fetch rebuilds chronological records; combining them with the current example recalculates valid 1–10-rep estimates; no eligible estimates, including empty history, show an explicit explanation |
| ExerciseExampleDetailsViewModel | Recent-record source | Rebuild | Finite request Flow combined with the observed example through Processing.Infinity | Runs once per VM; an empty result remains a known empty sample. No member cache/MutableStateFlow or loading flag; the independent metadata observation survives a failed history request |
| ExerciseExampleDetailsViewModel | achievements | Rebuild | ExerciseMetricsFeature | Existing startup fetch and mapping |
| ExerciseExampleDetailsScreen | selected tab and scroll | Runtime only, presentation widget state | Existing rememberSaveable(example id) and rememberScrollState | No business draft; new dialog defaults to Information; existing saveable UI behavior unchanged |
| VM/foundation | Loaders, jobs, directions, errors, telemetry provenance | Runtime only | Existing foundation ownership | No StateKeeper supplier, secret archive, restoration flag or shadow snapshot added |
| Calling screen | Any business result of the supplied Action callback | Existing caller owner outside scope | Existing callback | This dialog returns no edited input; caller remains responsible for its own draft |

No serializer or PersistenceKey is required: the module has no editable business
state and its dialog session is intentionally unsaved. No parent drafts, routes or
DialogConfig contracts change. Retained VM ownership and one-shot startup operations
remain unchanged; source observation retains its existing lifecycle policy.
Recent records are explicitly sorted by creation time and stable ID: newest-first
for history cards and oldest-first for volume and estimated 1RM. Remote response
order does not define the latest value or align dates by accident.
The history tab uses the shared flat volume bar style with exact tooltip units
and a complete series accessibility description. Its existing chronological
series uses each recent record's formatted session date as the presentation label;
dates are formatted in composition so locale changes update tooltip labels and
the compact history cards. The
categorical workout-statistics series retains exercise-name labels.
Compact and row summaries exclude pending sets from their maximum as before,
but withhold that maximum when a completed set has an unknown recorded load.
For a bodyweight exercise, both recorded body weight and coefficient are required
before its effective load can be known.
They do not substitute zero or publish a partial maximum from the known sets;
a genuinely recorded zero remains visible. Maximum labels use the same localized
read-only weight formatter as the estimated maximum.
The estimated 1RM card uses the strongest valid set per record for the route's
exercise ID. Singles preserve the recorded load; 2–10 reps use the mean of Epley
and Brzycki. Dates, source weight/reps and external versus effective body-inclusive
load remain rebuilt metadata. The sparkline uses recorded time intervals, dated
labels and weight-unit tooltips; its history peak is bounded by the fetched
history. Read-only weight labels use the active locale's decimal separator and
a space before the unit. Component requirements reject missing mandatory assistance/added weight
and fields unsupported by the example; optional absence alone means zero. Body
weight and coefficient always come from the recorded set, never today's profile
or catalog coefficient. The finite request Flow combines its emitted records with the existing example
observer, so either arrival order and later catalog updates recalculate the
estimate without a member cache or a second request. Processing.Infinity keeps
that one-time source from restarting after pause/resume; independent metadata
observation retains its WhileActive lifetime and survives a failed history load. Empty
estimates, including an entirely empty history, show eligibility information
without a fabricated zero or confidence score. No new interaction or event is
introduced.
Tap and horizontal exploration
stay local to the chart; recent records and exerciseVolume keep their existing
rebuild sources. No top-level VM State field, draft, callback, wire name, parameter,
schema, consent or transport contract changes. LoadRecentExercises now completes
before the combined one-rep-max calculation. The root admin mapping records this
boundary and its live consumer reconciliation; earlier-build duration comparisons
are not established.

## Verification

Source/diff review covers all callbacks, both modes, tabs, error/cancellation
boundaries, DI registration, schema catalog and state inventory. Fresh verification results:

- `git diff --check`: passed.
- Combined analytics/UI/shared Android attempt: analytics module compiled and
  generated Koin bindings/schema qualifier were inspected; the attempt then failed
  in foundation transformNativeMainDependenciesMetadata because generated
  commonMain.json was absent. No foundation source changes made.
- Retried `:ui-dialog-features:exercise-example:compileAndroidMain
  --no-configuration-cache`: BUILD SUCCESSFUL (54s), including the existing previews
  and neutral tab callback. Existing opt-in warning remains.
- Retried `:shared:compileAndroidMain --no-configuration-cache`: failed in
  toolkit:http-client transformNativeMainDependenciesMetadata because its generated
  commonMain.json was absent (26s). Shared source/registration was reviewed, but
  shared compilation is not confirmed. Parallel Gradle activity and a cache-lock
  wait were observed; the cause of missing generated files is not established.
- iOS compilation/XCFramework and device/runtime execution were not run.

### Follow-up residual review (2026-10-13)

Rechecked actual Contract/Screen/VM callers, both dialog modes, typed tab mapping,
all seven schemas, complete operation boundaries, generated Koin bindings and
unique composition-root registration. No scoped source fixes were required; the
no-draft/no-screen-hook assessment remains valid for this live overlay.

- Fresh `git diff --check`: passed.
- Fresh combined analytics/UI/shared attempt: failed in unrelated
  `:analytics:confirmation:kspCommonMainKotlinMetadata` with NoSuchFileException
  for its generated Kotlin org directory. Shared compilation remains unconfirmed.
- Fresh scoped `:analytics:exercise-example:compileAndroidMain
  :ui-dialog-features:exercise-example:compileAndroidMain --no-configuration-cache`:
  BUILD SUCCESSFUL (52s). Both target compile tasks were UP-TO-DATE; Gradle checked
  their current inputs and ran the necessary dependency work.
- No device/iOS/runtime checks were added; omissions below still apply.

Runtime checks not executed: default/action opening, both tabs/reselection,
hardware Back versus host swipe/outside dismissal, successful/failed/cancelled
loads, injected reporting failure, ordinary Activity recreation, pause/resume and
Android/iOS process restart. A process restart must close this live dialog; a fresh
opening must rebuild data. Source review and compilation do not prove native
lifecycle behavior or Firebase delivery. No automated test infrastructure added.
