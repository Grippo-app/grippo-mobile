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
| `DialogConfig.ExerciseExample`, Default or Action mode; Information and Achievements tabs | ExerciseExampleComponent → ExerciseExampleViewModel | Live overlay; default null screenPresentation, no dedicated dialog presentation contract |

Both modes and tabs render one dialog, including loading and empty states. The
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
| Existing startup safeLaunch | LoadExample | Complete getExerciseExampleById/getOrThrow refresh block |
| Existing RecentExercises safeLaunch | LoadRecentExercises | Request/getOrThrow, state mapping, volume/one-rep-max calculation and state publication |
| Existing Achievements safeLaunch | LoadAchievements | Request/getOrThrow, state mapping and publication |

Operations use `exercise_example_task_started/completed/failed/interrupted` v1,
with closed `exercise_example_operation` codes; terminal events include duration_ms.
The shared decorator supplies fresh operation IDs and monotonic duration, isolates
reporting Exceptions and preserves return values, original errors and cancellation.
No loader, job, dispatcher, retry, eligibility guard or business outcome changes.
All seven schemas register once under the unique exercise_example_analytics qualifier.

Deliberate omissions: dialog view/close, swipe/outside dismissal, entry/referrer,
mode/action label, downstream action success, data-flow emissions and muscle summary
recomputation. No callback or routing context is added to observe unavailable facts.
No exercise IDs, titles, URLs, history counts, body measurements, weights,
achievements, request/response payloads or exception text are encoded.

## State preservation gate

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| Shared dialog host | Session, id, mode/title, action/back callbacks and nested step | Runtime only | Existing retained host; slot/step serializer = null | Ordinary recreation can retain the session; process death closes it |
| ExerciseExampleViewModel | mode | Runtime only | Existing DialogConfig input | Fresh opening uses supplied mode; no editable draft |
| ExerciseExampleViewModel | example, muscleLoad | Rebuild | ExerciseExampleFeature observer/refresh; MuscleLoadingSummaryUseCase | Fresh opening observes current data and recomputes summary |
| ExerciseExampleViewModel | recent, exerciseVolume, estimatedOneRepMax | Rebuild | ExerciseMetricsFeature, VolumeSeriesUseCase, EstimatedOneRepMaxUseCase | Existing startup fetch and mapping |
| ExerciseExampleViewModel | achievements | Rebuild | ExerciseMetricsFeature | Existing startup fetch and mapping |
| ExerciseExampleScreen | selected tab and scroll | Runtime only, presentation widget state | Existing rememberSaveable(example id) and rememberScrollState | No business draft; new dialog defaults to Information; existing saveable UI behavior unchanged |
| VM/foundation | Loaders, jobs, directions, errors, telemetry provenance | Runtime only | Existing foundation ownership | No StateKeeper supplier, secret archive, restoration flag or shadow snapshot added |
| Calling screen | Any business result of the supplied Action callback | Existing caller owner outside scope | Existing callback | This dialog returns no edited input; caller remains responsible for its own draft |

No serializer or PersistenceKey is required: the module has no editable business
state and its dialog session is intentionally unsaved. No parent drafts, routes or
DialogConfig contracts change. Retained VM ownership and one-shot startup operations
remain unchanged; source observation retains its existing lifecycle policy.

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
