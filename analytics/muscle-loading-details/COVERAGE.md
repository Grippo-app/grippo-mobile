# Muscle loading details analytics and state assessment

Scope: `:ui-dialog-features:muscle-loading-details`, reviewed 2026-10-13.

## Surface and presentation source gate

| Route / rendered surface | Component → VM | Presentation classification |
|---|---|---|
| DialogConfig.MuscleLoadingDetails(range) → DialogContentComponent child → MuscleLoadingDetailsScreen | MuscleLoadingDetailsComponent → MuscleLoadingDetailsViewModel | Runtime bottom sheet with balance and distribution content. Default null screenPresentation; no independent screen or dedicated dialog presentation contract. |

The sole business opener is HomeViewModel.onOpenMuscleLoading: its existing
nonnull-range guard and HomeEvent.MuscleLoadingClicked remain unchanged. All
callers use the existing dialog host factory. There are no nested screens or
containers owned by this module. The Component injects the stateless analytics API
inside its retainedInstance factory. Shared registers the generated annotated
module and the uniquely qualified `muscle_loading_details_analytics` schema catalog.
No host, route, lifecycle or screen identity changes are needed. The shared balance
card distinguishes an empty group distribution (no-data text), one loaded group
(a localized factual label with the group name), and multiple groups (the existing
bias comparison). Score, status, differences and gap calculations are unchanged.
This presentation-only branch adds no control, hook, event, parameter or draft;
the existing State, Contract, Direction and all data owners remain unchanged.

## Callback and operation coverage

| Owner / callback | Typed fact / version-1 wire schema | Observed meaning |
|---|---|---|
| MuscleLoadingDetailsViewModel.onSelectMode | ModeSelected(mode) / muscle_loading_mode_selected | Every invoked selection callback, including the current mode if invoked; enum values per_group or per_muscle. Before the existing state update; no new eligibility guard. |
| MuscleLoadingDetailsViewModel.onBack | DismissRequested / muscle_loading_dismiss_requested | Callback invocation before the existing Back direction. Component BackCallback targets this VM method; no source or completed-dismissal inference. |
| init / provideMuscleLoad | No interaction or operation event | observeTrainings remains a lifecycle-aware subscription; current emissions rebuild summary using the existing use case and mapper. No finite business command or operation decorator is introduced. |

UI enum mapping stays in the local analytics package. No dates, range endpoints,
training/muscle IDs, names, load/measurement values, exception text or raw input
are encoded. Ordinary reporting Exceptions cannot block mode updates or Back;
fatal Errors remain fatal. The existing business subscription, empty-training
behavior (summary=null), error handling and cancellation are unchanged.

Omissions: dialog view/close, swipe/scrim/host Close or host Back paths that bypass
onBack, programmatic removal, entry attribution and opening-screen observations.
No synthetic events on initialization, data emissions, ordinary recreation or
resume. The requested scope instruments observable local callbacks, not host exits.

## State preservation source gate

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| MuscleLoadingDetailsViewModel | mode | Runtime only | Existing retained VM, no StateKeeper supplier | Live recreation retains the VM selection; process death discards the dialog. A new session defaults to PerGroup. No accepted result is sent to a parent. |
| MuscleLoadingDetailsViewModel | summary | Rebuild | TrainingFeature.observeTrainings(range.from, range.to), MuscleLoadingSummaryUseCase.fromTrainings and toState | Fresh session starts null; observations rebuild current data, empty trainings clear summary. |
| DialogConfig / VM | range and localized DateRangeFormatState | Runtime only / Rebuild | Runtime config seed and DateRangeFormatState.of(range) | Caller supplies the range for a newly opened session; current formatting is rebuilt. This dialog does not edit the parent range. |
| Host / Component / foundation | dialog session, callbacks, jobs, loaders, directions, scroll and telemetry provenance | Runtime only | Host slot and content stack both serializer=null; retainedInstance and existing presentation widgets | No sessions, callbacks, jobs or telemetry metadata archived. |
| Calling screen | accepted parent draft | Existing owner | No result callback or business selection returned by this detail dialog | No parent draft changes required. |

No secrets, unfinished text inputs or accepted parent values are owned here.
The canonical policy explicitly leaves live dialog selections unsaved. No new
serializer, PersistenceKey, serialization plugin, global storage or shell changes
are warranted. Source review passes the preservation gate; retained recreation
alone does not prove process restoration or lifecycle resubscription.

## Verification and manual checks

Source review traced the actual content, Contract callbacks, Component factory,
host serializer=null boundaries, both enum encoders, schema registration and the
sole VM constructor caller. Screen classification and state inventory pass source
review. The implementation imports no UI, domain, SDK or toolkit core. No tests
or verification-only production hooks were introduced.

Independent review is unperformed: no permitted independent reviewer is available
in this session. Runtime checks remain unperformed: event payload delivery,
reporting-failure isolation, actual Back priority, pause/resume, Android recreation
and process restart, and iOS checkpoint behavior.

Manual checks: open with loaded and empty training data; select PerMuscle and
PerGroup and inspect the bounded event; invoke Back and inspect dismiss_requested.
Compare swipe/host Close exits with the explicit omission. Rotate/resume with
PerMuscle selected and verify the retained selection and refreshed summary.
After actual process termination verify the old sheet is absent; reopen it and
verify PerGroup and presentation rebuilt for the caller's current range.

Initial build attempts: scoped Gradle metadata compilation for the analytics
module and UI dialog plus shared KSP were cancelled (exit 130) during project
configuration. These historical attempts supplied no compilation evidence.

Follow-up tail review: no unused models, missing module/DI references, duplicate
schema names, privacy leaks or required state-preservation gaps were found.
Scoped `git diff --check` passed again. Verification now confirms:

- `:analytics:muscle-loading-details:kspCommonMainKotlinMetadata` succeeded.
- `:analytics:muscle-loading-details:compileCommonMainKotlinMetadata` succeeded.
- `:ui-dialog-features:muscle-loading-details:compileCommonMainKotlinMetadata`
  succeeded in a separate offline build (BUILD SUCCESSFUL); UI KSP was up-to-date.
- Aggregate `compileKotlinMetadata` tasks were SKIPPED, so only the actual
  commonMain tasks above are treated as compilation evidence.
- The combined build including `:shared:kspCommonMainKotlinMetadata` failed in
  `:data-features:excluded-muscles:transformNativeMainDependenciesMetadata` with
  NoSuchFileException for a transformed nativeMain
  `androidx.savedstate-savedstate-1.4.0-nonAndroidMain-JkDvJw.klib` under that
  module's build directory. Shared KSP is not confirmed by this run. No unrelated
  infrastructure or processes were modified to bypass the failure.

CommonMain compilation does not prove Android/iOS binaries, Koin runtime startup,
SDK delivery or restoration. Those checks and independent review remain unperformed.
