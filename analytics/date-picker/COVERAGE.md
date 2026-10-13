## Current contract follow-up (2026-10-14)

The [current analytics review](../COVERAGE.md#current-analytics-contract-review-2026-10-14)
records this revision and its verification limits. Typed dialog observation names
use DismissRequested across screens/dialogs, with dismiss_requested wire names/codes.
The app has not been released; all analytics schemas use initial version 1.

# Date picker analytics and state preservation

Scope: `ui-dialog-features/date-picker`, reviewed on 2026-10-13. The user chose
the existing dialog policy: retain unfinished selection during Activity recreation;
after process death close the dialog and restore accepted results in its parent.

## Surfaces and observation owners

| Route / surface | Component → ViewModel | Presentation policy |
|---|---|---|
| DialogConfig.DatePicker, from Profile Goal or Daily calendar | DatePickerComponent → DatePickerViewModel | Runtime overlay; default null screenPresentation; no dedicated dialog view/close contract |
| ProfileRouter.Goal, accepted date owner | ProfileGoalComponent → ProfileGoalViewModel | Existing ProfileScreen.Goal hook; unchanged |
| Trainings calendar, Daily mode, accepted date owner | TrainingsComponent → TrainingsViewModel | Existing TrainingsScreen.Calendar hook; unchanged |
| Shared dialog slot/content stack | DialogComponent / DialogContentComponent | Containers; no screen presentation added |

The stateless DatePickerAnalytics service is resolved only in the normal retained
VM factory. Its annotated module and uniquely qualified schema catalog are wired
in shared. This scope has no asynchronous business operation to decorate.

| Callback | Observation | Eligibility / meaning |
|---|---|---|
| onSubmitClick | date_picker_submit, version 1 | Only DateTimeFormatState.Valid; invalid/empty callbacks neither emit nor navigate; observes submit before the host delivers the result |
| onDismiss, including Component system Back | date_picker_dismiss_requested, version 1 | Observed callback only, without inferring a dismissal reason or host completion |
| onSelectDate | Deliberately omitted | Wheel synchronization and initial composition also invoke select; these are not reliable user interaction observations |
| Parent date picker open / accepted onResult | Existing parent analytics | Profile Goal and Daily calendar retain their existing open/accepted events; no context is transported through routes |

Both custom payloads have no parameters. No date, display string, goal value,
entity ID, input length, secret or telemetry provenance is recorded. Ordinary
reporting exceptions cannot prevent submit/dismiss. No operation API, screen enum,
synthetic restoration event or lifecycle job is added. Host swipe/outside dismissal,
programmatic closure and dedicated dialog visibility remain explicit omissions;
they do not invoke the leaf's onDismiss and are not inferred as user cancellation.

The follow-up precision correction preserves the original full time in wheel
selection and validity. Submit still means an eligible confirmation callback;
no status/date parameter or event is added, and both schemas remain version 1.
Dates at a subminute lower bound are now correctly eligible instead of being
rejected after truncation, so later submit counts can include these formerly
blocked confirmations. This is an eligibility correction, not a new funnel step.

## Preservation inventory

| Owner | Field/group | Policy | Source / restoration and check |
|---|---|---|---|
| DatePickerViewModel | value, including unfinished selection | Runtime session / retained | Existing retainedInstance keeps the VM across supported Activity recreation; a fresh dialog uses its parent's current accepted date |
| DatePickerViewModel | limitations | Runtime session | Existing caller input; no separate saved owner |
| DateWheelPicker | day/month/year scroll positions | UI presentation | Initial list indexes now match selected day/month/year instead of index zero; logical selection remains in VM state |
| DateWheelPicker | Complete time of the current value | Preserve during date-only UI synchronization | Reconstructed dates and validity checks use value.time, including seconds/nanoseconds; recreation no longer truncates the retained value or rejects a subminute lower bound |
| ProfileGoalViewModel | selectedTarget | Preserve at parent | ProfileGoalComponent DRAFT / ProfileGoalDraft and ProfileGoalState.serializer(); onResult updates the saved field; fresh picker uses selectedTarget |
| TrainingsViewModel | date, period | Preserve at parent | TrainingsComponent DRAFT / TrainingsDraft and TrainingsState.serializer(); accepted date applies the anchor; fresh picker uses date.from |
| Both parents | Date limitations, catalogs and formatted presentation | Rebuild | Existing current sources/transient fields; no new duplicate snapshot |
| All affected owners | Callbacks, dialog sessions, jobs/loaders, analytics lifecycle | Runtime only | Host slot and step serializers remain null; no new archive fields or suppliers |

Source review traces accepted callbacks to existing parent suppliers and serializers,
retained ownership and fresh dialog seeds. No secret or runtime state is added to
an archive. The parent screen hooks satisfy screen coverage; the picker is an
overlay, not an additional independent screen. Restoration does not replay submit
or dismissal callbacks. A pending result during sheet dismissal remains runtime
state until the host delivers it to the parent.

## Verification

Source/diff self-review covers exhaustive event mapping, catalog/DI registration,
reporting isolation, valid/empty/invalid submit eligibility, actual caller routes,
parent persistence and selected wheel indexes. No automated test infrastructure
was added. Scoped inline source invariants and `git diff --check` passed. The
initial combined iOS Simulator compilation executed both date-picker compile tasks
successfully, but its shared verification failed in unrelated transformed metadata
outputs: first screen-api, then exercise-examples reported missing `.klib` files
while other Gradle builds were active in the same checkout. The first build was
cancelled while waiting for the configuration-cache lock; subsequent runs used
`--no-configuration-cache` without changing project settings. The final isolated
run of `:ui-dialog-features:date-picker:compileKotlinIosSimulatorArm64` and
`:analytics:date-picker:compileKotlinIosSimulatorArm64` passed (BUILD SUCCESSFUL).
Generated Koin source was inspected: it binds DatePickerAnalyticsImpl to
DatePickerAnalytics and provides the qualified date_picker_analytics schema.
Follow-up tail review fixed loss of seconds/nanoseconds in DateWheelPicker and
added this record to the main analytics coverage index. Source gates and
`git diff --check` passed on the corrected revision. The final
`./gradlew --no-configuration-cache :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64`
passed (BUILD SUCCESSFUL, 3m 48s), including actual Android and iOS Simulator
compilation of date-picker. Android APK assembly and shared Simulator compilation
are now verified; the earlier missing metadata failures are historical. Device
and simulator framework linking, installation and native runtime behavior were
not checked by these compilation/assembly tasks.

Independent review was not performed: no permitted independent local reviewer was
available, and subagent delegation was not authorized. Native runtime checks were
not run. Remaining manual checks: choose a noninitial date → recreate Activity →
verify the same selection and visible wheel indexes; confirm in each parent →
save archive → destroy lifecycle and InstanceKeeper → fresh context/VM restore →
verify the accepted date and fresh picker seed; kill with an unconfirmed picker →
verify the dialog closes and the previous accepted parent date restores. Also check
month-end/leap-year boundaries, invalid/empty submit, Back versus swipe, actual SDK
payload delivery and reporting-failure isolation. iOS depends on a completed valid
background checkpoint; abrupt active termination has no restoration guarantee.
