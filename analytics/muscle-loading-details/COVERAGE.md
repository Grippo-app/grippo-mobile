# Muscle loading details analytics and state assessment

Scope: `:ui-dialog-features:muscle-loading-details`.

## Surface and presentation source gate

| Route / rendered surface | Component → VM | Presentation classification |
|---|---|---|
| DialogConfig.MuscleLoadingDetails(range) → DialogContentComponent child → MuscleLoadingDetailsScreen | MuscleLoadingDetailsComponent → MuscleLoadingDetailsViewModel | Runtime bottom sheet with factual leading-set-share and distribution content. Default null screenPresentation; no independent screen or dedicated dialog presentation contract. |

The sole business opener is HomeViewModel.onOpenMuscleLoading: its existing
nonnull-range guard and HomeEvent.MuscleLoadingClicked remain unchanged. All
callers use the existing dialog host factory. There are no nested screens or
containers owned by this module. The Component injects the stateless analytics API
inside its retainedInstance factory. Shared registers the generated annotated
module and the uniquely qualified `muscle_loading_details_analytics` schema catalog.
No host, route, lifecycle or screen identity changes are needed. The shared focus
card shows the actual largest weighted-set share and all tied leading groups,
without a balance-quality score, gap recommendation or fatigue/recovery estimate.
An empty distribution shows neutral no-data text. Positive anatomy shares allocate
each accepted recorded set linearly; recorded resistance × repetitions is a
separate distribution of measured work. Positive-repetition sets with valid supplied
measurements retain their set shares when required load fields are missing; their
entire volume is omitted and counted as unmeasured. Required fields come from the
observed example components; historical body weight and multiplier must be paired,
and unknown examples or unexpected components cannot supply measured volume.
No current weight or catalogue multiplier is substituted. Invalid sets, unmeasured
sets and accepted sets missing anatomy are counted explicitly and explained by data
notices. Body fields follow domain validation: supplied body weight and multiplier
must be finite and positive, with no invented multiplier cap. A negative effective
machine resistance or nonfinite recorded body product leaves the performed set
intact but makes its physical volume unmeasured. Required external zero is invalid;
optional external zero and zero extra/assistance remain allowed. Only available mapped exercise details enter the calculation. The existing set-share note and available-set label scope the analysis to
available exercise details; no raw relation count,
completeness flag or inferred repetitions/weights are retained. No control, hook, event or parameter is
added by these calculated values or explanatory notices.

## Callback and operation coverage

| Owner / callback | Typed fact / version-1 wire schema | Observed meaning |
|---|---|---|
| MuscleLoadingDetailsViewModel.onSelectMode | ModeSelected(mode) / muscle_loading_mode_selected | Every invoked selection callback, including the current mode if invoked; enum values per_group or per_muscle. Before the existing state update; no new eligibility guard. |
| MuscleLoadingDetailsViewModel.onBack | DismissRequested / muscle_loading_dismiss_requested | Callback invocation before the existing Back direction. Component BackCallback targets this VM method; no source or completed-dismissal inference. |
| init / provideMuscleLoad | No interaction or operation event | observeTrainings flatMapLatest observes the referenced anatomy through observeFromTrainings; training or anatomy emissions rebuild summary and notices through the mapper. No finite business command or operation decorator is introduced. |

UI enum mapping stays in the local analytics package. No dates, range endpoints,
training/muscle IDs, names, load/measurement values, exception text or raw input
are encoded. Ordinary reporting Exceptions cannot block mode updates or Back;
fatal Errors remain fatal. The subscription publishes an empty summary for an empty training list, so the
focus card and distribution show a visible no-data state. Error handling and
cancellation retain the foundation pipeline.

Omissions: dialog view/close, swipe/scrim/host Close or host Back paths that bypass
onBack, programmatic removal, entry attribution and opening-screen observations.
No synthetic events on initialization, data emissions, ordinary recreation or
resume. The requested scope instruments observable local callbacks, not host exits.

## State preservation source gate

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| MuscleLoadingDetailsViewModel | mode | Runtime only | Existing retained VM, no StateKeeper supplier | Live recreation retains the VM selection; process death discards the dialog. A new session defaults to PerGroup. No accepted result is sent to a parent. |
| MuscleLoadingDetailsViewModel | summary | Rebuild | TrainingFeature.observeTrainings(range.from, range.to) → flatMapLatest(MuscleLoadingSummaryUseCase.observeFromTrainings) → toState | Fresh session starts null; training and anatomy emissions rebuild shares, excluded/unassigned/unmeasured set counts, the available-details scope in the set-share note and set-count label, and a visible empty summary when no work is available. |
| DialogConfig / VM | range and localized DateRangeFormatState | Runtime only / Rebuild | Runtime config seed and DateRangeFormatState.of(range), rendered through short() | Caller supplies the range for a newly opened session; short() rebuilds localized text from the current composition locale even while the dialog is retained. This dialog does not edit the parent range. |
| Host / Component / foundation | dialog session, callbacks, jobs, loaders, directions, scroll and telemetry provenance | Runtime only | Host slot and content stack both serializer=null; retainedInstance and existing presentation widgets | No sessions, callbacks, jobs or telemetry metadata archived. |
| Calling screen | accepted parent draft | Existing owner | No result callback or business selection returned by this detail dialog | No parent draft changes required. |

No secrets, unfinished text inputs or accepted parent values are owned here.
The canonical policy explicitly leaves live dialog selections unsaved. No new
serializer, PersistenceKey, serialization plugin, global storage or shell changes
are warranted. Source review passes the preservation gate; retained recreation
alone does not prove process restoration or lifecycle resubscription.

## Verification boundary

The coverage inventory concerns the callback, observer and preservation source
contracts. No tests or verification-only production hooks are introduced.
Runtime rendering, lifecycle resubscription, native saved-state behavior and
analytics delivery require their own observations; task-specific build and runtime
evidence belongs in the task reply rather than a dated coverage journal.
