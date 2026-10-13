# Month picker analytics and state preservation

Scope: `ui-dialog-features/month-picker`, 2026-10-13.

## Surface and callback inventory

| Route / surface | Component → VM | Presentation policy |
|---|---|---|
| DialogConfig.MonthPicker, calendar Monthly mode | MonthPickerComponent → MonthPickerViewModel | Runtime overlay; default null screenPresentation; no dedicated dialog presentation contract |
| Accepted month owner, calendar tab | TrainingsComponent → TrainingsViewModel | Existing TrainingsScreen.Calendar presentation hook; unchanged |
| Shared dialog slot / content stack | DialogComponent / DialogContentComponent | Containers; no screen presentation |

| Callback | Observation / schema | Meaning |
|---|---|---|
| onSubmitClick | SubmitClicked / month_picker_submit_clicked v1 | Eligible Valid confirmation callback, before BackWithResult; Empty/Invalid callbacks return without events or navigation |
| onDismiss, including system Back | DismissRequested / month_picker_dismiss_requested v1 | Observed request only, without inferring cancellation or delivered closure |
| onSelectMonth | Deliberately omitted | LaunchedEffect and wheel synchronization invoke this during initialization as well as user scrolling; no reliable user provenance |
| Parent onOpenDateSelector / onResult | Existing TrainingsEvent.MonthPickerClicked / MonthConfirmed | Actual parent open and accepted result; no context propagated to dialog |

Both custom payloads are empty. No raw date/month/year, formatter text, entity ID,
secret, input length or telemetry history is encoded. The internal exhaustive
matrix registers both schemas once under month_picker_analytics. Shared wires the
annotated module; only the retained VM factory resolves MonthPickerAnalytics.
Ordinary reporting Exceptions are isolated, while fatal Errors remain fatal.
No asynchronous operation exists to decorate. Host swipe/outside dismissal,
programmatic closure and dialog visibility are explicit omissions, not inferred
leaf interactions. No operation models, screen enum or lifecycle observer is added.

## Preservation inventory

| Owner | Field/group | Preserve / Rebuild / Runtime only | Source / restoration and check |
|---|---|---|---|
| MonthPickerComponent / VM | value, including unfinished selection | Retain in live session; runtime only in process archive | Existing retainedInstance; fresh dialog seeds from parent current date.from |
| MonthPickerState | limitations / date format | Runtime caller inputs | DialogConfig.MonthPicker; no duplicate supplier |
| MonthWheelPicker | selectedYear/month and list positions | UI presentation rebuilt from retained value | List initial indexes now match selected month/year, avoiding an initial index-zero observation overwriting retained selection |
| MonthWheelPicker | time precision and bounded month anchor | Rebuild from current value | Full LocalTime retained, including seconds/nanoseconds; selected month anchor clamps to exact caller bounds rather than skipping the VM update |
| TrainingsComponent / VM | accepted date and period | Preserve at parent | DRAFT / TrainingsDraft / TrainingsState.serializer(); onResult applies anchor into date; fresh VM restores before fallback |
| TrainingsState | limitations, calendar/digest/timeline/localized labels | Rebuild | Transient fields and existing domain sources; restored date rebuilds Monthly presentation |
| Dialog host / all affected owners | callbacks, unfinished sessions, pending result, jobs/loaders and telemetry lifecycle | Runtime only | Slot and step serializers remain null; no new saved fields or keys |

The source preservation gate traces the sole caller through host result delivery,
parent applyAnchor/update, lazy saveState supplier and restoreState in the fresh
retained VM factory. No StateKeeper snapshot is added to the dialog: process death
closes live sessions; accepted values belong to the screen. Configuration recreation
retains the VM. Reopening uses current accepted parent values without replaying
analytics callbacks. No secrets or reporting provenance enter an archive.
A result queued during sheet closing is runtime state until delivered to parent.
Android needs supplied saved state; iOS needs a completed valid background checkpoint,
with no guarantee after abrupt active termination.

## Verification

Source/diff self-review, scoped inline source invariants and git diff --check passed.
No automated test sources, dependencies or infrastructure added. Source coverage
classifies the overlay and containers correctly and checks the existing independent
calendar presentation hook. Review checks empty payloads, exhaustive schema mapping,
reporting exception isolation, eligible navigation/result behavior and local ownership.
Generated Koin source binds MonthPickerAnalyticsImpl to MonthPickerAnalytics and
registers the qualified month_picker_analytics schema.

The first analytics build was cancelled while waiting for a shared Gradle cache
lock (confirmed in its daemon thread dump). A retry initially rejected combining
project-cache-dir with filesystem watching; the subsequent invocation disabled
watching without changing project settings. In that combined run both
:analytics:month-picker:compileAndroidMain and compileKotlinIosSimulatorArm64
completed, but the overall build failed in unrelated :ui-core:foundation KSP
while normalizing a crash-reporting metadata file. Concurrent builds were running
in the same checkout. Shared KSP was therefore not verified by that attempt.

Final isolated UI verification passed (BUILD SUCCESSFUL, 2m 2s):
`./gradlew --offline --no-watch-fs --no-configuration-cache --configure-on-demand --project-cache-dir /tmp/grippo-month-picker-cache :ui-dialog-features:month-picker:compileAndroidMain :ui-dialog-features:month-picker:compileKotlinIosSimulatorArm64`.
This proves scoped Android and iOS Simulator compilation, not APK/framework linking,
application startup, SDK delivery or restoration runtime behavior. Shared module
registration was source-reviewed; shared compilation was unverified at that stage
and is covered by the successful tail-review build below. Runtime DI/startup remain
unverified. No independent reviewer/subagent was used.

Unrun manual checks: scroll to a noninitial month/year → recreate Activity → verify
selection and visible indexes; confirm → archive roundtrip → destroy lifecycle and
InstanceKeeper → fresh context/VM restore → verify calendar and fresh picker seed;
kill with unconfirmed picker → verify closed dialog and last accepted calendar range.
Also verify month/year boundaries, disabled submit, Back versus swipe, reporting
failure isolation and actual SDK delivery. Builds do not prove these runtime checks.


## Tail review (2026-10-13)

Fixed two findings: direct Empty/Invalid submit callbacks now return before
analytics/navigation, matching the existing disabled button; month synchronization
keeps complete LocalTime and clamps the candidate date-time to exact bounds.
Previously, minute truncation or an inherited time before the lower bound could
silently skip select while the visible wheel changed month. The month/year list
indexes continue to seed from the retained business value. No extra snapshot or
restoration flag is introduced.

Submit schema remains v1 with the same empty payload and callback meaning.
Eligibility is tightened for rejected direct callbacks, so such calls no longer
contribute submit counts. Boundary selections that previously failed to update
VM state now correctly produce the selected month anchor; no synthetic selection
event is added.

Final verification passed (BUILD SUCCESSFUL, 5m 16s):
`./gradlew --offline --no-watch-fs --no-configuration-cache --configure-on-demand --project-cache-dir /tmp/grippo-month-picker-cache :analytics:month-picker:compileAndroidMain :analytics:month-picker:compileKotlinIosSimulatorArm64 :ui-dialog-features:month-picker:compileAndroidMain :ui-dialog-features:month-picker:compileKotlinIosSimulatorArm64 :shared:compileAndroidMain :shared:compileKotlinIosSimulatorArm64`.
Both changed UI compile tasks and shared Android compilation executed; analytics
and shared iOS Simulator compilation reused valid cached outputs. This closes the earlier shared compilation limitation and
checks the generated module accessor in the composition root. Final scoped source
gates and git diff --check passed. Native runtime checks remain unrun, including
fresh StateKeeper archive restoration, Activity recreation, boundary wheel changes,
SDK delivery and reporting-failure injection. Add the manual lower-bound case
with nonzero seconds/nanoseconds and a selected-month time earlier than the bound:
visible month/year and VM/result must agree, with the exact boundary retained.

