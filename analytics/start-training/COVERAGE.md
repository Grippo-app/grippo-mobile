# Start training dialog coverage

Scope: `:ui-dialog-features:start-training`, reviewed 2026-10-13.

## Surface and observation owners

| Route / rendered surface | Component → VM | Presentation policy |
|---|---|---|
| `DialogConfig.StartTraining`, opened by TrainingRecording Add initialization while exercises are null; shared dialog content factory renders StartTrainingScreen | StartTrainingComponent → retained StartTrainingViewModel | Live bottom sheet, no dedicated dialog presentation contract; default null screenPresentation, no independent screen identity |

The existing TrainingRecording parent owns StartOpened/StartConfirmed and its
Recording screen hook. Shared DialogComponent/Content host sessions and steps use
null serializers. This task adds no host, parent, screen or lifecycle wiring.

| Callback / operation | Observation | Eligibility and payload |
|---|---|---|
| onSelect(key) | start_training_option_confirmed v1 | Only after key resolves in current options; option enum empty/preset/recent. Unknown keys emit nothing and navigate nowhere |
| onBack() including registered hardware Back | start_training_back v1 | No payload; existing Back navigation is unchanged |
| Existing startup coroutine, after best-effort range refresh | BuildOptions / start_training_task_started/completed/failed/interrupted v1 | Complete local option acquisition/build: first current-cache observation, whole preset use case, mapping and state update; operation code and terminal duration only |

`StartTrainingAnalytics` is injected through the retained VM factory; its internal
implementation and qualified complete schema catalog are registered through the
single generated StartTrainingAnalyticsModule in shared. Event reporting Exceptions
are isolated; operation reporting uses the existing toolkit decorator, preserving
original return/error/cancellation and fresh operation IDs with monotonic timing.
Fatal Errors remain fatal. No IDs, raw keys, exercise values, dates, measurements,
exception text or request/response bodies are encoded.

Deliberate omissions: pager swipes, previous/next arrows, initial programmatic
focus and data population do not emit selection/confirmation. Outside/swipe host
dismissal is not synthesized as Back. Dedicated dialog view/close is not defined.
The existing `getTrainings` Result is intentionally ignored for cached fallback;
its refresh outcome is not covered by BuildOptions, and completion claims only
that local options were built, never server refresh success. No business context
or callback is added solely for analytics.

## State preservation inventory

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| StartTrainingViewModel | Options including recent identities, dates and exercises; generated preset | Rebuild on fresh dialog opening | Existing TrainingFeature range refresh/cache observation, GeneratePresetTrainingUseCase | Empty option initially; existing best-effort refresh and subsequent option construction retained; no StateKeeper supplier |
| StartTrainingScreen | Pager position, initial-focus flag, animation/haptic state | Runtime only | Existing Compose ownership | Unaccepted live dialog choice; discarded with session after process death |
| Dialog hosts / Component | Session, callbacks, Back registration, VM jobs, errors, loaders | Runtime only | Existing retainedInstance; host serializers null | Ordinary retained recreation differs from process death; no session archive or callback replay |
| TrainingRecording parent | Accepted preset/recent exercises and their cloned exercise/iteration IDs | Preserve at existing parent | TrainingRecordingComponent DRAFT / TrainingRecordingDraft; existing saveDraftTraining data source | onUseExercises updates parent's exercises and saves draft; no duplicate dialog owner |
| TrainingRecording parent | StartEmpty result | Existing business behavior | Opens exercise library; no accepted exercises yet | No fabricated empty exercise draft, changed callback or new persistence mechanism |
| Analytics/foundation | Reporting provenance and operation timing/IDs | Runtime only | Existing toolkit | Never serialized; no synthetic restoration events |

No secret input exists in this dialog. Its local selections/session are explicitly
unsaved by the state-preservation canon. Accepted data already has its screen
owner. No new serializer, PersistenceKey, restore flag or shadow snapshot is needed.

## Verification

Source review traced config, actual Add caller, shared factory, rendered controls,
retained factory, accepted parent results and its local DRAFT wiring. Screen coverage
and preservation source inventories above pass for this scoped dialog. The enum
mapping is exhaustive; unknown-key guard precedes reporting; state/navigation and
exercise cloning follow existing business paths. Schema catalog includes both
interaction schemas and all four operation outcomes. No automated test sources,
infrastructure or verification-only production hooks were added.

Static checks passed: scoped `git diff --check`, six unique event schemas with
bounded names, exactly one settings/dependency/generated-module registration,
implementation dependency boundaries and the rejected-key guard before reporting.
The initial build command used an unavailable `compileKotlinAndroid` task; corrected
tasks use `compileAndroidMain`. The combined UI/analytics/shared-KSP attempt failed
in `:ui-core:foundation:compileCommonMainKotlinMetadata` because its intermediate
`build/kotlinTransformedMetadataLibraries/commonMain/commonMain.json` was missing.
Concurrent Gradle builds were present; this is not evidence of feature compilation.
No unrelated build infrastructure or Gradle processes were changed.
The narrower UI/analytics retry generated and compiled StartTrainingAnalytics, then
failed in `:design-system:components:transformCommonMainDependenciesMetadata` with
NoSuchFileException for its transformed Compose UI skikoMain klib. The UI
compile and shared-KSP check were unverified at that point; see the follow-up below. Generated Koin bindings were
inspected: singleton StartTrainingAnalyticsImpl binds StartTrainingAnalytics and
one qualified `start_training_analytics` schema supplier binds AnalyticsSchema.
A standalone analytics build also failed before the feature task, while bundling
analytics-api Android classes: DurationPickerAnalytics.class was unavailable.
No complete initial build invocation passed. Those attempts are historical; the
follow-up below supersedes the UI/analytics compilation limitation.

### Follow-up tail audit, 2026-10-13

Re-reviewed the scoped source/diff, routes and retained VM ownership, callback
guards, bounded payloads, exception/cancellation isolation, state inventory,
schema catalog and single composition-root registration. No new implementation
findings. Fresh `git diff --check` and scoped static checks passed: six unique
bounded schemas, no stale draft/LoadOptions names and one registration per boundary.

The combined follow-up compiled both start-training modules, then failed in
`:data-services:database:transformCommonMainDependenciesMetadata` on a missing
transformed coroutines concurrentMain klib; shared KSP was not reached.
The separate final scoped command passed, exit 0, BUILD SUCCESSFUL:
`./gradlew :analytics:start-training:compileAndroidMain :ui-dialog-features:start-training:compileAndroidMain --offline --no-configuration-cache`.
Generated StartTrainingAnalytics interface binding and qualified schema binding
were inspected previously and remain unchanged. UI and analytics compilation
are now verified; shared KSP and actual runtime DI remain unverified. No unrelated
infrastructure, user changes or Gradle processes were altered.

Independent review is unperformed: agent delegation is not authorized and the
available review connector supports existing pull-request CI diagnostics, not a
local independent review. Native runtime, reporting-failure injection, cancellation
injection, Firebase delivery and iOS compilation have not been exercised.

Manual checks: confirm Empty/Preset/Recent and inspect bounded events plus original
navigation; reject an unknown key with no event; hardware Back versus outside/swipe
dismissal; fresh opening with refresh failure still builds cached options; ordinary
reporting failure must not block confirmation/Back or replace operation outcomes.
Rotate while open, then separately restart with supported Android saved state or an
iOS completed background checkpoint: the live sheet closes, accepted parent
exercises restore, and callbacks are not replayed. Pause/resume must not repeat
startup in the retained VM. These scenarios remain unrun.
