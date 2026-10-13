# Height picker analytics and state preservation

Scope: `:ui-dialog-features:height-picker`, reviewed 2026-10-13.

## Surface and screen coverage gate

| Entry / surface | Component → VM | Presentation classification |
|---|---|---|
| Authorization User and Profile Body → DialogConfig.HeightPicker → shared DialogContentComponent factory → HeightPickerScreen | HeightPickerComponent → retained HeightPickerViewModel | Live picker overlay; default null screenPresentation. No independent screen or screen enum is introduced; no separate dialog presentation contract |
| Shared DialogComponent / DialogContentComponent | Existing host VMs | Containers; no new presentation hook or events |

Both callers already observe Height picker open and accepted-result facts in their
own VMs. The host delivers BackWithResult through the existing session-bound
callback; this change does not infer result acceptance from a submit click.

## Event coverage and privacy

| Owner / callback | Typed event / wire schema | Eligibility and payload |
|---|---|---|
| HeightPickerViewModel.onSubmitClick | SubmitClicked / height_picker_submit_clicked v1 | Existing unconditional callback, before BackWithResult; empty parameters |
| HeightPickerViewModel.onDismiss | DismissRequested / height_picker_dismiss_requested v1 | Existing VM dismissal / Back callback, before Back; empty parameters |
| HeightPickerViewModel.onSelectHeight | Deliberate omission | BindWheel emits on initial and programmatic synchronization as well as scrolling; no user-only observation is available |

No height, measurement, identifier, raw input or exception is encoded. The
stateless HeightPickerAnalytics service is resolved only inside retainedInstance;
its internal implementation catches reporting Exceptions so result/navigation
continues, while fatal Errors remain fatal. Its uniquely qualified schema catalog
is registered once through the generated annotated module in shared Koin.
No complete asynchronous business operation exists here, so no operation API is
added. Dedicated dialog views/closes, swipe/outside dismissal and cancellation
reasons remain omissions; onDismiss is a request, not proof of delivered closure.

## Preservation inventory and source gate

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or source | Restore/default and check |
|---|---|---|---|---|
| HeightPickerComponent / VM | Unconfirmed height selection | Runtime only; retained live VM | Existing retainedInstance, no local archive | Fresh opening uses initial; unfinished dialog input is discarded after process death by canonical policy |
| HeightPickerState | Suggestions | Rebuild | HeightLimitation → DefaultHeightSuggestions | Fresh state rebuilds bounds |
| Authorization UserComponent / UserState | Accepted height | Preserve, already implemented | UserDraft / UserState.serializer() | onResult updates height; restoreState precedes fresh VM; saveState supplies current draft; re-entry uses current accepted height |
| ProfileBodyComponent / ProfileBodyState | Accepted height | Preserve, already implemented | ProfileBodyDraft / ProfileBodyState.serializer() | onResult updates height; saved initialState skips source initialization; fresh reopening uses current draft |
| Dialog host and picker | Session, callbacks, directions, focus/widget state, loaders and telemetry provenance | Runtime only | Host childSlot and childStack serializer = null | No callbacks, sessions, secrets or telemetry provenance added to archives |

Source gate: no missing required picker snapshot. Canonical state-preservation
explicitly excludes live dialog sessions and unfinished inputs. Existing parent
suppliers own accepted values, so no duplicate StateKeeper owner or serialization
plugin is needed in height-picker. Restoration does not replay picker callbacks.
Visual impact: none; existing screen preview remains applicable.

## Verification

Source/diff review checked both callbacks and unchanged business directions,
exhaustive event encoding, empty payloads, reporting isolation, dependency
boundaries, annotated binding/catalog registration and the sole VM constructor
caller. Actual factory/render paths and parent save/restore wiring were reviewed;
the scoped screen coverage and preservation source gates pass.

Runtime checks not run: wheel initialization versus scrolling; submit/result and
Back delivery with reporting failure injection; actual Koin resolution and SDK
delivery; pause/resume and ordinary recreation with retained VM; parent save →
serialization → destroy lifecycle/InstanceKeeper → fresh Component restore;
process death closes the picker while preserving accepted parent height, and
reopening seeds the accepted value. Android saved state and iOS completed background
checkpoint provide their existing guarantees; abrupt active termination is not
proven. No automated test sources or infrastructure were added.

Scoped static assertions and `git diff --check` passed. The combined Android/iOS
compile command for analytics:height-picker, ui-dialog-features:height-picker and
shared was attempted, but Gradle failed during shared script configuration on
unresolved analytics project accessors: iterationPicker, monthPicker,
muscleLoadingDetails and performanceTrendDetails. At the time of that attempt, unrelated working-tree
edits prevented task execution; this is a historical failure, not a current
missing-registration claim. Compilation and generated DI remain unverified.
No unrelated build wiring was changed to bypass the failure.

After the other module registrations appeared, compilation was retried. The retry
remained in Gradle configuration without further task output for several minutes;
it was interrupted. No compilation success is claimed.

Follow-up tail review (2026-10-13): module includes, shared dependency and generated
module registration each occur once; event names/qualifier have no duplicate
definitions; both parent accepted-height drafts remain wired. No code defect was
found. Clarified historical build failure wording and normalized changed Kotlin
file endings; `git diff --check` passed. Offline compile attempts were made for
Android/iOS: the shared-integrated run stalled at daemon startup (10 busy daemons
reported), and the scoped analytics/UI run with a single-use 2 GiB daemon stalled
at task-graph calculation. Both were interrupted; compilation/runtime verification
remains outstanding. No project build settings or unrelated modules were changed.
