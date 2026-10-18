# Duration picker coverage

Record owner: [feature-analytics](../../.claude/skills/feature-analytics/SKILL.md)
and [state-preservation](../../.claude/skills/state-preservation/SKILL.md); records
policy: [CLAUDE.md](../../CLAUDE.md#records). Cross-feature contract and
verification limits: [current analytics contract review](../COVERAGE.md#current-analytics-contract-review-2026-10-14);
admin reconciliation: [Live GA4 setup review](../COVERAGE.md#live-ga4-setup-review--2026-10-14).
Rows describe the current code of `:ui-dialog-features:duration-picker` and
`:analytics:duration-picker`; replace them when that code changes. Typed dismissal
observations use DismissRequested with `dismiss_requested` wire names; the app is
unreleased and every schema is version 1.

## Surfaces and screen hooks

| Route / rendered surface | Component → ViewModel | Presentation | Coverage |
|---|---|---|---|
| Nested step: ConfirmTrainingCompletion's `PickDuration(initial)` direction → DialogContentComponent pushes `DialogConfig.DurationPicker(title, initial, onResult)` onto the live session → child factory → DurationPickerScreen (hours/minutes wheels, Apply) | DurationPickerComponent → retained DurationPickerViewModel | Live bottom-sheet step; default null screenPresentation, no screen enum, no dedicated dialog presentation contract | Stateless DurationPickerAnalytics injected inside retainedInstance; Essenty BackCallback → `onDismiss`; event listener maps BackWithResult → `onResult(value)` and Back → the host back |
| Direct opening through `dialogController.open(DialogConfig.DurationPicker(...))` | Same factory and owner | Same policy | Supported by the factory; no production caller constructs the config directly today, so only the nested path exists |

No independent screen or container behavior belongs to this feature. Open intent
and the accepted result are facts at the caller boundary: ConfirmTrainingCompletion
reports DurationOpened and DurationSelected, and the recording screen reports
CompletionOpened/CompletionConfirmed, all through TrainingAnalytics
([training](../training/COVERAGE.md)). The nested push is deduplicated by title
and initial value and ignored while the parent step is inactive. The factory
delivers only a nonnull result value to the caller.

## Callbacks and operations

| Callback owner | Observation | Eligibility and payload |
|---|---|---|
| DurationPickerViewModel.onSubmitClick (Apply button) | SubmitClicked / `duration_picker_submit_clicked` v1 | Only after the VM resolves `DurationFormatState.Valid`; emitted before BackWithResult with that same value. Apply is disabled for Empty/Invalid, and rejected callbacks emit nothing and navigate nowhere; empty payload, no duration value |
| DurationPickerViewModel.onDismiss | DismissRequested / `duration_picker_dismiss_requested` v1 | The Component's Essenty BackCallback is the only UI source (the screen has no Cancel); empty payload; a request only, no inferred cancellation or delivered close |
| DurationPickerViewModel.onSelectDuration | Deliberate omission | The wheel's launched effects resync and call `select` on first composition and on every programmatic synchronization as well as on scrolling; no interaction provenance is available, so no synthetic user event or telemetry history is added |
| Toolbar back arrow (visible for the nested step), the host's Compose BackHandler, toolbar close, swipe/outside | Deliberate omission | For a nested step the host's `onDismiss` pops the picker without calling this VM, so no `duration_picker_dismiss_requested` is emitted on those routes |
| Dialog viewed / closed | Deliberate omission | No dedicated presentation contract |

No asynchronous business operation exists in this VM, so no operation API or
decorator exists. No view/close, raw duration, route ID, input text, secret or
exception payload is emitted. Ordinary reporting exceptions cannot prevent
navigation (the implementation catches Exception); fatal Errors propagate.

## Catalog

`DurationPickerAnalyticsModule` (`@Module @ComponentScan`) binds
`DurationPickerAnalyticsImpl` as `DurationPickerAnalytics` and registers the two
empty-parameter v1 schemas through one qualified `duration_picker_analytics`
provider; `shared/.../Koin.kt` lists it once, and the module depends only on
`analytics-api`. The API exposes `trackEvent` only; two typed events each have one
VM consumer and one exhaustive encoder.

## Preservation inventory

The user explicitly chose the existing dialog policy: retain the live selection
through Activity recreation; discard the session after process death. No local
StateKeeper supplier, serializer, session restoration or shell change exists.

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| DurationPickerComponent / DurationPickerViewModel / DurationPickerState (`@Immutable`, not serializable) | `value: DurationFormatState`, including the unfinished selection | Runtime only (retained in the live session) | Constructor `initial` = `DurationFormatState.of(config.initial)`; retainedInstance evaluates it once for a fresh VM; no DRAFT | A fresh dialog receives the caller's business initial duration; a null or Empty initial becomes Valid (one minute, the limitation start) on first composition through the wheel sync; ordinary recreation keeps the live value; process death closes the dialog |
| DurationPickerState | `hours` 0..23, `minutes` 0..59 | Rebuild | Existing `DurationLimitation` (one minute to 23 h 59 min) | Defaults at State construction; wheel validity ranges recomputed |
| ConfirmTrainingCompletion parent dialog | Accepted picker duration | Runtime session value | Existing `onResult` → `onDurationSelected` updates the parent State; the initial duration derives from the recording's startAt, coerced to at least one minute | Reopening the picker seeds from the parent's current duration; the parent dialog is also discarded on process death; Confirm is disabled for Invalid/Empty |
| TrainingRecording owner | Confirmed duration represented by `startAt`; cleaned exercises; stage | Preserve (existing owner) | `TrainingRecordingDraft` / `TrainingRecordingState.serializer()` | The completion `onResult` sets `startAt = now - result` and drops pending iterations before navigating |
| Completed route / owner | `stage`, `exercises`, `startAt`; saved outcome | Existing route and draft | `TrainingRouter.Completed(stage, exercises, startAt)` via `replaceAll`; `TrainingCompletedDraft` | The route payload restores accepted values; the completed draft owns the saved ID |
| All dialog owners | Callbacks, session stack, jobs/loaders (none defined), focus/widget state, analytics provenance | Runtime only | Shared childSlot and childStack use `serializer = null`; the Component has no `var` | No secrets or telemetry values are serialized |
