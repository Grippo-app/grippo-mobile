# Exercise dialog analytics and state assessment

Scope: `:ui-dialog-features:exercise`, reviewed 2026-10-13.

## Surface and presentation gate

| Route / rendered surface | Component → VM | Presentation classification |
|---|---|---|
| DialogConfig.Exercise → dialog content child → ExerciseScreen | ExerciseComponent → ExerciseViewModel | Read-only runtime overlay; default null screenPresentation. No independent screen or separate dialog presentation contract. |
| ShowExampleDetails → host pushes DialogConfig.ExerciseExample | Existing ExerciseExampleComponent / VM | Nested runtime overlay outside this instrumentation scope; no new presentation contract. |

Both ordinary and nested exercise entries use the same host factory. The existing
Component resolves ExerciseAnalytics in its retainedInstance factory. Shared
registers ExerciseAnalyticsModule and its uniquely qualified `exercise_analytics`
schema catalog. No host, config, navigation or lifecycle instrumentation is added.

## Observed callbacks and schemas

| Owner / callback | Typed fact / version-1 wire schema | Eligibility and meaning |
|---|---|---|
| ExerciseViewModel.onExampleDetailsClick | ExampleDetailsClicked / exercise_example_details_clicked | After the existing exercise/example ID guard, before ShowExampleDetails; observes click, not successful downstream display. Null exercise emits nothing. |
| ExerciseViewModel.onDismiss | Dismiss / exercise_dismiss | Before the existing Back direction. The Component registers this callback with its BackCallback. Reports callback invocation without inferring user source or actual host dismissal. |

Both schemas have no parameters. No IDs, names, URLs, measurements, iteration
values or exception text are reported. Ordinary reporting Exceptions are isolated
in the implementation; fatal Errors remain fatal. Navigation and the observation
subscription retain their original business behavior. There is no finite business
command to decorate: observeExercise remains a subscription, not an operation.

Omissions: automatic dialog view/close, host Back/swipe/outside-tap dismissal,
programmatic removal, entry attribution and nested-example interaction/outcome.
Host-managed exits can bypass ExerciseViewModel.onDismiss and therefore emit no
exercise_dismiss. No synthetic events on initialization, data emissions or resume.
Calling screens retain their own open-intent observations.

## State preservation source gate

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| ExerciseViewModel | exercise with example presentation, total and iterations | Rebuild | TrainingFeature.observeExercise(id) → existing domain-to-state mapper | Fresh live session starts null and subscribes to the current source; no user-editable fields. |
| DialogConfig.Exercise | id / title / callbacks and session identity | Runtime only | Existing host slot and content stack, both serializer = null | Retained VM supports ordinary recreation; process death closes the dialog session. No StateKeeper supplier is required or added. |
| Exercise VM / foundation | loaders, directions, subscription and telemetry provenance | Runtime only | Existing foundation and retainedInstance | No jobs, flags, reporting metadata or callbacks serialized. |
| Calling screen | accepted result / draft | Existing owner | This dialog returns no editable selection or accepted result | No parent draft changes required; detail navigation uses the existing example ID. |

There are no inputs, selections, filters, parent drafts or secrets owned by this
module. No serialization plugin, PersistenceKey, archive schema or app-shell
change is warranted. An ordinary retained recreation does not rerun VM init;
a new session creates a fresh VM and rebuilds presentation. Source review is not
runtime proof of lifecycle resubscription or process restart.

## Verification

Source review checked the actual screen body, Contract callbacks, Component
factory, host nested navigation and serializer=null boundaries. The eligible and
null-data paths retain their prior guard/navigation behavior. The typed family is
exhaustively encoded into two parameterless schemas, and the implementation has
no UI/domain/SDK imports. Module registration and the sole VM constructor caller
are reconciled. The analytics surface gate and state inventory pass source review.

Runtime checks remain unperformed: callback delivery/Back priority, host swipe
omission, SDK delivery, injected reporting failure, ordinary recreation, fresh
process restart and iOS checkpoint behavior. No automated test infrastructure or
verification-only production hooks were introduced. Independent review is
unperformed: no permitted independent reviewer is available in this session.

Manual checks: open the dialog with loaded data, tap details and return; verify
one click observation per accepted callback. Verify null data cannot produce a
detail event. Invoke Back and compare the actual host/child handling with the
explicit dismissal omission. Rotate/resume while the session is live, then restart
the process: retained recreation may keep the session, process death closes it;
a newly opened session must rebuild current exercise data.

Fresh verification results:
- `git diff --check` passed.
- Ad hoc source checks passed: two unique version-1 parameterless schema names,
  transport name syntax/length, no duplicate wire declarations, implementation
  import boundaries and event placement after the existing null-data guard.
- Combined Android build of exercise UI/analytics/shared failed during project
  configuration: first `projects.analytics.heightPicker`, then on retry with
  `--no-configuration-cache`, `muscleLoadingDetails` / `performanceTrendDetails`
  were unresolved in shared/build.gradle.kts. These unrelated shared dependencies
  were changing concurrently; no unrelated build edits were made for this task.
- Isolated UI/analytics Android build with `--configure-on-demand
  --no-configuration-cache` failed before exercise compilation in
  `:compose-libs:chart:transformNativeMainDependenciesMetadata`: unreadable output,
  missing `compose-libs/chart/build/kotlinTransformedMetadataLibraries/nativeMain/nativeMain.json`.
  UI compilation and shared generated DI remain unverified; no chart/build
  infrastructure changes were made.
- Standalone `:analytics:exercise:compileAndroidMain --configure-on-demand
  --no-configuration-cache` passed (BUILD SUCCESSFUL, 51s). Generated Koin source
  binds ExerciseAnalytics to ExerciseAnalyticsImpl and registers the qualified
  schema provider. This confirms analytics compilation and generated definitions,
  not shared DI runtime or SDK delivery.

### Follow-up tail review (2026-10-13)

Re-read the actual Exercise Contract/Screen/State/Component/VM, the analytics API,
implementation/matrix/module, shared registration and dialog Back/session wiring.
No missing caller, unused observation, duplicate exercise schema/provider,
reporting-only navigation field or required draft was found in this scope.
The existing host-managed dismissal omissions remain deliberate; this is not a
complete count of all dialog exits. The previous compile success applies only to
the analytics module, not the UI or shared composition root.

Fresh combined Android retry passed shared configuration but failed in
`:toolkit:date-utils:compileCommonMainKotlinMetadata` because
`toolkit/date-utils/build/kotlinTransformedMetadataLibraries/commonMain/commonMain.json`
was missing. UI/shared compilation is still unverified. No unrelated generated
outputs or build infrastructure were changed. Runtime and independent review
limits above remain in effect.
