# Trainings calendar analytics coverage

Record owner: [feature-analytics](../../.claude/skills/feature-analytics/SKILL.md)
and [state-preservation](../../.claude/skills/state-preservation/SKILL.md); records
policy: [CLAUDE.md](../../CLAUDE.md#records). Cross-feature contract and
verification limits: [current analytics contract review](../COVERAGE.md#current-analytics-contract-review-2026-10-14);
admin reconciliation: [Live GA4 setup review](../COVERAGE.md#live-ga4-setup-review--2026-10-14).
Rows describe the current code of `:ui-screen-features:trainings`, `TrainingsRouter`
in `:ui-screen-features:screen-api` and `:analytics:trainings`; replace them when
that code changes. Typed dismissal observations use DismissRequested with
`dismiss_requested` wire names; operation schemas omit visible-screen context and
correlate outcomes by `operation_id`; the typed catalog is not yet in a store build and every schema is version 1.

## Surfaces and screen hooks

| Route / rendered surface | Component → ViewModel | Typed screen / hook |
|---|---|---|
| `MainRouter.Calendar` tab (bottom-bar item; `close = back`) and pushed `RootRouter.Trainings` (`RootDirection.Trainings`; `close = onBack`) → `TrainingsRouter.Trainings`; Daily and Monthly modes including loading/empty content are one surface | TrainingsComponent → TrainingsViewModel | `TrainingsScreen.Calendar` / `trainings_calendar`; exactly one screenPresentation; the only enum entry |
| Trainings root stack | TrainingsRootComponent → TrainingsRootViewModel | Default null; renders only the child stack; forwards Back and `toEditTraining(id)` → `toTraining(StageState.Edit(id))` without events |
| Overlays opened by the calendar VM: TrainingMenuPicker, Exercise, Statistics.Trainings(range), DatePicker, MonthPicker, Statistics.Training(id) | Shared dialog host / respective dialog VMs | Live overlays, no dedicated presentation contract; picker and menu `onResult` fire only on BackWithResult, so the calendar observes accepted results only |

TrainingsComponent resolves TrainingsAnalytics inside `retainedInstance`;
foundation alone binds the lifecycle, emits Initial/Restore and closes on
pause/destroy. Editing a training leaves this module through the existing callback
to the Training flow, whose presentation is its own ([training](../training/COVERAGE.md)).
Root and main containers keep their null hooks.

## Callbacks and operations

| VM observation boundary | Typed event / wire | Eligibility / facts |
|---|---|---|
| onTrainingMenuClick(id) | TrainingMenuClicked / `trainings_training_menu_clicked` | No guard; entity ID not encoded |
| Menu `onResult` | DeleteSelected / EditSelected / DetailsSelected (`trainings_delete_selected`, `trainings_edit_selected`, `trainings_details_selected`) | Accepted menu result before the existing delete command, edit navigation or overview |
| onExerciseClick(id) | ExerciseClicked / `trainings_exercise_clicked` | No exercise ID |
| onDailyDigestViewStats | DigestClicked / `trainings_digest_clicked` | Opens statistics with `period.effectiveRange(date.from, limitations)`, matching refresh, observation, calendar and digest; range not encoded |
| onSelectPeriod(id) | DailyPeriodSelected / MonthlyPeriodSelected | Only after the same-period and unknown-ID rejections |
| onOpenDateSelector | DatePickerClicked / MonthPickerClicked | The current mode selects the picker |
| Date/MonthPicker `onResult` | DateConfirmed / MonthConfirmed | Accepted result, including an unchanged value (observed accepted result; an unchanged anchor leaves state untouched and triggers no new request); no date encoded |
| Next/Previous arrows → shiftDate(±1) | NextDateSelected / PreviousDateSelected | After coercion into the limitations, rejected only when the shifted range equals the current one; a clipped range that still differs does emit |
| onOpenDaily(date) | DailySelected / `trainings_daily_selected` | No VM guard; the UI permits only current-month days with training records; switches to Daily directly |
| onBack | DismissRequested / `trainings_dismiss_requested` | Leaf callback only; the container forwards navigation without a synthetic event |
| rangeFlow (`period.effectiveRange(date.from, limitations)`, distinctUntilChanged) → `mapLatest` → refreshTrainings, per-request safeLaunch under the Trainings loader | RefreshTrainings / `trainings_task_*` {trainings_operation = refresh_trainings} | Entire `getTrainings(range)` block (throwing Unit command); an unchanged effective range does not retrigger from presentation rebuilds. A failed request reaches the standard error pipeline without terminating later range refreshes; no automatic retry. Range/lifecycle cancellation cancels and joins the request before replacing the loader |
| deleteTraining(id) safeLaunch | DeleteTraining / `trainings_task_*` {delete_training} | Entire `DeleteTrainingUseCase.execute(id)` block |

All 16 interaction schemas are parameterless (`trainings_training_menu_clicked`,
`trainings_delete_selected`, `trainings_edit_selected`, `trainings_details_selected`,
`trainings_exercise_clicked`, `trainings_digest_clicked`, `trainings_daily_selected`,
`trainings_date_picker_clicked`, `trainings_date_confirmed`,
`trainings_month_picker_clicked`, `trainings_month_confirmed`,
`trainings_daily_period_selected`, `trainings_monthly_period_selected`,
`trainings_next_date_selected`, `trainings_previous_date_selected`,
`trainings_dismiss_requested`). The operation schemas `trainings_task_started`
[trainings_operation] and `trainings_task_completed/failed/interrupted`
[trainings_operation, duration_ms] carry the two closed codes and the monotonic
terminal duration. Toolkit supplies fresh operation identity, reporting exception
isolation (`trackEvent` catches Exception), original result/error identity and
CancellationException propagation (interrupted). No new coroutine, retry, busy
guard or business validation exists for telemetry. `TrainingsAnalyticsModule`
registers the 20 schemas through one qualified `trainings_analytics` provider and
binds the stateless service; `shared/.../Koin.kt` lists it once.

`TrainingsState.selectRange` atomically sets the date and replaces the period with
an empty Daily or a `Monthly.from(range, empty)` grid with an empty digest; it is
used at initialization, period change, a changed picker anchor and arrow
navigation, so changed ranges never show stale items until the DAO emits.

## Omissions and privacy

Dedicated dialog views/closes and dismissal reasons, DAO observation operations,
local digest/timeline calculations and formatting, referrers/flow attribution,
transition events and programmatic restore clicks are not tracked. Digest selection
is inclusive by `createdAt`; every selected saved session counts, even when its
exercise relations are unavailable. Saved duration and volume supply the totals;
invalid negative/nonfinite source values make that metric unavailable instead of
producing a partial sum. Sets count only available mapped exercise details and are labelled Sets, including zero; no raw relation count or completeness flag is retained. The use case no longer derives active days or an average volume; the state mapper drops the range, so the card presents only the workout count, duration, tonnage and sets, and the toolbar's month picker owns the period. Read-only total formatters preserve recorded
zero and multi-day durations. No raw dates, inputs, lengths,
entity IDs, measurements, secrets, exception messages or request bodies are sent.
Monthly's effective range ignores the limitations while Daily coerces its request
range; this is business behavior, not telemetry.

The digest change leaves Trainings events, operations, models, parameters, schemas,
encoders, screen identities/hooks, tracking arguments and placement, consent and
transport unchanged. Live admin synchronization is therefore not triggered.

The refresh error-boundary change moves `trackOperation(RefreshTrainings)` into
the per-request `safeLaunch`; this placement change triggers live admin
reconciliation for `trainings_task_*` and their consumers, despite unchanged
operation codes and schemas. Scoped live reconciliation retained the existing
mobile event-family diagnostics and terminal-duration metric without admin writes.
Plural `trainings_operation` remains deliberately unregistered, so the saved report
cannot separate refresh_trainings from delete_training; singular training_operation
and home_operation are different contracts. No retry, refresh-only success rate or
current-APK SDK receipt is claimed. The root [admin synchronization record](../COVERAGE.md#live-ga4-setup-review--2026-10-14)
owns the verified consumer mapping and delivery limits.

## Preservation inventory

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| TrainingsComponent / TrainingsViewModel | `period` (Daily/Monthly discriminator) | Preserve | `DRAFT = PersistenceKey("TrainingsDraft", TrainingsState.serializer())`; nullable initialState; `saveState` of the latest VM state; `TrainingsTimelinePeriod` is a serializable sealed interface whose payload fields are all `@Transient` | Type-stable default `Monthly()`; restored and fresh drafts both pass through `selectRange(draft.date)` |
| Same | `date` (DateRange) incl. accepted picker results, arrow and day selection | Preserve | Same key; serializable DateRange; no default, cannot be elided | The dynamic current-month `Monthly().defaultRange()` applies only without a saved draft; picker results (`applyAnchor`), arrows and day taps all write this field |
| Same | `limitations` | Rebuild | `@Transient`; `DateTimeUtils.trailingYear()` | Recomputed per construction |
| Same | Daily `items` | Rebuild | `@Transient`; `observeTrainings` plus digest/timeline use cases | Cleared by `selectRange` |
| Same | Monthly `monthReference`, `digest`, `weeks` | Rebuild | `@Transient`; `Monthly.from(range, timeline)` | `selectRange` builds the saved-month grid before DAO data arrives; the observed effective range rebuilds matching digest and calendar counts, including saved sessions without attached exercises; none of these values is archived |
| Monthly calendar / DigestCard | Weekday labels and digest metric labels | Rebuild in composition | `DateCompose.rememberWeekDayShortLabels()` from existing typed dates; digest labels and formatted values from `DigestState` (`trainingsCount`, `duration`, `total`, `totalSets`; no range, training-day or average-volume fields) | Current locale updates retained content without rebuilding the VM; weekday labels are no longer stored in Monthly state. The month picker in the toolbar owns the displayed period. Date bounds, archived fields and callback owners are unchanged |
| Same | Period `id`, `text` (localized) | Rebuild | `@Transient` | Rebuilt from the current locale |
| Same | Trainings loader, Flow jobs, dialog sessions/callbacks | Runtime only | Outer `withLoader`, range-driven `mapLatest`, per-request `safeLaunch`, DialogController | A failed refresh leaves the range subscription available for later selections. Range replacement, lifecycle suspension or VM destruction cancels and joins its request before releasing the loader; jobs are never archived. Live pickers are unsaved; the accepted date/month lands in `state.date` |
| TrainingsRootComponent | Child stack | Preserve (navigation) | `TrainingsRouter.serializer()`, key `TrainingsRootComponent` | Data-object root state: no root draft and nothing to seed a re-entered child; fresh entry without an archive uses the current-month Monthly default |

The local supplier saves the latest VM state lazily on StateKeeper.save; no second
owner, archive key or storage exists. No secrets exist in this draft. Restoration
initializes state directly without invoking control callbacks; init restarts the
existing range request/observation. Back/pop may destroy the child draft; fresh
re-entry uses the default with no parent draft to seed. Pause/resume uses the
existing activation/subscription policy with a one-second disconnect grace; no
restoration flags, caches or `var` members exist in these ViewModels.

## Verification

Source gates for the current revision were reconciled against the routes, rendered
modes/controls, callback guards, event/schema consumers, complete operation
boundaries, retained injection, generated DI, draft fields, source initialization
and lifecycle subscription (read-only audit on 2026-10-15; no build was part of
it). Checks recorded for the implementing revisions (2026-10-13):

- Calendar instrumentation and recreation preservation: inline source invariants
  and scoped `git diff --check` passed; `:analytics:trainings:compileAndroidMain
  :ui-screen-features:trainings:compileAndroidMain --offline` passed on the final
  revision including common KSP and the generated qualified catalog (a preceding
  attempt failed in resources:provider cache cleanup while another build wrote new
  files; the scoped retry passed without deleting caches); metadata-only compile
  tasks were skipped and do not count as compilation.
- Follow-up quality review resolved one finding: arrows and accepted date/month
  changes previously updated the date while leaving old Daily items or the Monthly
  grid/digest until a DAO emission; `TrainingsState.selectRange` now clears them
  atomically at all four boundaries, with no business guard, coercion policy, wire
  schema or serialized field change. Final checks: source invariants, scoped
  `git diff --check`, `:analytics:trainings` and `:ui-screen-features:trainings`
  `compileAndroidMain` plus `compileKotlinIosSimulatorArm64` together offline
  (BUILD SUCCESSFUL, 1m 12s); generated Koin retains the service binding and
  qualified catalog; remaining compiler warnings belong to existing global opt-ins.
- Scoped Home and Training flows review: authorized `:androidApp:assembleDebug`
  and `:shared:assembleSharedDebugXCFramework` passed. The training list refresh
  updates valid records, retains cached incomplete records and returns the shared
  `Network.IncompleteResponse` failure, mapped by the standard ErrorProvider, for incomplete hydration; missing IDs
  prevent range deletion; calendar presentation rebuilds do not restart requests
  for an unchanged range. These data-boundary changes add no screen draft or
  event/operation payload.
- Independent review was not performed for those revisions (no authorized
  separate reviewer); self-review found no remaining blocking source defects. No
  automated test sources, infrastructure or verification-only production hooks
  were introduced.

Not verified: the save → serialize → destroy → restore roundtrip on Android and
iOS; ScreenAnalytics Initial/Restore delivery; precedence between the root and
child BackCallbacks; resume behavior after the whileActive grace;
reporting-failure injection; SDK delivery; generated Koin content at runtime.
Permitted manual checks: select a historical month, switch to Daily and pick a
date; save and serialize the shell archive, destroy the lifecycle and
InstanceKeeper, construct fresh contexts/VMs and verify exact mode/range, rebuilt
records, the initial saved-month calendar, Back/edit-and-return and final refresh
inputs; confirm an unchanged period/unknown ID and arrow limits emit no accepted
selection while an unchanged picker confirmation emits its observed confirmation;
verify unconfirmed pickers disappear, range replacement/destroy interrupts the
refresh, a failed delete uses the standard error pipeline and a throwing reporter
does not block actions. Android requires a delivered saved-state archive; iOS a
completed valid background checkpoint from the same build; abrupt active iOS
termination has no restoration guarantee.
