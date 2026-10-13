# Period picker analytics and state preservation

Scope: `ui-dialog-features/period-picker`, 2026-10-13.

## Surface and callback coverage

| Route / surface | Component → VM | Presentation policy |
|---|---|---|
| DialogConfig.PeriodPicker; sole caller HomeViewModel.onOpenPeriodPicker | PeriodPickerComponent → PeriodPickerViewModel | Live overlay; default null screenPresentation; no separate dialog presentation contract |
| Home parent screen | HomeComponent → HomeViewModel | Existing HomeScreen.Home hook, unchanged |
| Home graph and shared dialog slot/content stack | HomeRootComponent; DialogComponent / DialogContentComponent | Containers, default null hook |

| Callback | Event / schema | Observation |
|---|---|---|
| onSelectRange | RangeSelected / period_picker_range_selected v1 | Actual card selection callback, including the already selected range; before existing safeLaunch/update/delay/result direction |
| onDismiss, including system Back | DismissRequested / period_picker_dismiss_requested v1 | Request only; does not infer delivered closure or cancellation |
| Home onOpenPeriodPicker / onResult | Existing HomeEvent.PeriodPickerClicked / PeriodConfirmed and HomeOperation.SavePeriod | Parent opening and accepted result/save; unchanged |

Both new payloads are empty. Raw dates, selected range values, formatter text,
identifiers, secrets and telemetry history are not encoded. The exhaustive internal
matrix registers both schemas once with period_picker_analytics; shared wires the
annotated module and the retained VM factory resolves the stateless API.
Ordinary reporting Exceptions are isolated in the implementation, fatal Errors
remain fatal. No async business use case exists inside the picker to decorate;
the existing 300 ms delay is UI timing, not a tracked operation. No eligibility
guard exists in the callback; instrumentation preserves accepted callback behavior.
Rapid taps retain existing delayed/conflated navigation behavior; selection counts
are callback counts, not guaranteed accepted-result counts.

Explicit omissions: dialog views/close, host swipe/outside/programmatic dismissal,
selected range attribution and pending-result delivery. Shared host/config plumbing
constructs no events and no reporting context is added to business contracts.
No screen enum, lifecycle observer, operation API or retention key is introduced.

## Preservation inventory

| Owner | Field/group | Policy | Source / restoration |
|---|---|---|---|
| PeriodPickerComponent / VM | value (current selection) | Runtime session only, retained during ordinary recreation | Existing retainedInstance; fresh dialog seeds caller initial |
| PeriodPickerState | suggestions and formatted range dates | Rebuild for fresh dialog | DateRangeFormatState.of and current preset defaults |
| Home / LocalSettings | confirmed range kind | Durable settings; no duplicate StateKeeper snapshot | onResult → setRange/getOrThrow → DataStore range preference; observeRange → provideRange builds current date bounds |
| Dialog host | callbacks, sessions, pending result | Runtime only | Slot/step serializer = null; process death closes dialog |
| Picker VM | delay/job/loaders and telemetry | Runtime only | Existing VM lifetime; no serialized provenance |

The source preservation gate follows the sole caller and result delivery through
DialogContentComponent.dialogNavigation.finish to Home's existing tracked save.
Home starts with an empty range and dependent requests filter null date bounds
until persisted settings resolve; the dashboard renders after existing loaders
and range checks. Once DataStore save succeeds, the confirmed period survives
process recreation and reopening seeds the current accepted kind. A result still
queued during sheet closure, or a save not completed before process death, has
no durability guarantee. No unfinished live dialog input is archived, in accordance
with the canonical state-preservation/process-death policy. No secrets enter archives.
No changes to parent/settings/shell behavior or persistence schema were needed.

## Initial verification (historical; compilation limits closed by tail review below)

Source/diff self-review checks route/surface classification, typed callback owners,
empty payloads, exhaustive schemas, reporting isolation, normal retained VM DI,
existing result/error/cancellation behavior and durable parent ownership.
No automated test sources/dependencies/infrastructure added.
Independent review is unperformed: no permitted independent reviewer was available
without agent delegation authorization; available review tooling only handles PR CI.

Scoped inline source invariants and git diff --check passed. Initial wrapper launch
was blocked by sandbox access to the Gradle cache; the authorized elevated launch
ran. Its configuration failed on unrelated shared analytics accessors while other
work was still registering modules. After those registrations appeared, the combined
build executed :analytics:period-picker:compileAndroidMain and
:analytics:period-picker:compileKotlinIosSimulatorArm64 successfully, then failed in
unchanged :ui-core:foundation:kspCommonMainKotlinMetadata while normalizing
ui-core/error/error-provider metadata. Parallel builds were active in the checkout;
this result does not verify UI/shared compilation. The remaining UI/shared retry failed in unchanged
:ui-core:state:compileCommonMainKotlinMetadata because its transformed metadata
commonMain.json file was missing (2m 34s). UI/shared compilation remains unverified;
no unrelated build infrastructure was changed. Exact invocations used
./gradlew --offline --no-watch-fs --no-configuration-cache --configure-on-demand
--project-cache-dir /tmp/grippo-period-picker-cache with the analytics, picker UI
and shared compileAndroidMain / compileKotlinIosSimulatorArm64 tasks; the final
retry requested only picker UI/shared. These dependency failures do not prove a
source defect in period-picker, nor do successful analytics tasks prove UI/runtime DI.
Final scoped source/diff self-review found no actionable defects; analytics and
preservation source gates passed. No runtime/device checks ran.
Unrun runtime checks: reporting-failure injection/SDK delivery; select a nondefault
period, wait for completed save, recreate process and verify Home plus fresh picker
seed; kill with picker open before selection delivery and verify closed dialog with
last saved period; ordinary Activity recreation should retain live selection.
Also check rapid taps, Back and swipe separately. Android supplied-state and iOS
completed-background-checkpoint rules apply to screen navigation; live dialog
sessions are never restored from process archives.


## Tail review (2026-10-13)

Re-reviewed actual API/implementation, VM callbacks, sole Home caller and durable
settings owner, host result delivery, screen classification, module registration
and schema privacy. No actionable source defects or unused reporting models were
found; production code did not require changes. Source analytics/preservation gates
and final git diff --check passed. Generated Koin source explicitly binds
PeriodPickerAnalyticsImpl to PeriodPickerAnalytics and registers the uniquely
qualified period_picker_analytics schema.

The combined tail build completed analytics Android/iOS Simulator compilation
(from cache) and executed both picker UI compile tasks successfully. It then failed
in unrelated data-features:equipment KSP on missing backend generated metadata.
The remaining shared-only retry passed (BUILD SUCCESSFUL, 5m 9s), executing
:shared:compileAndroidMain and :shared:compileKotlinIosSimulatorArm64.
This closes the initial UI/shared compilation limitations and checks the generated
module accessor in the composition root. No unrelated build infrastructure changed.

Commands used the common prefix:
`./gradlew --offline --no-watch-fs --no-configuration-cache --configure-on-demand --project-cache-dir /tmp/grippo-period-picker-cache`.
First task set: analytics:period-picker, ui-dialog-features:period-picker and shared
compileAndroidMain / compileKotlinIosSimulatorArm64. Final successful task set:
`:shared:compileAndroidMain :shared:compileKotlinIosSimulatorArm64`.

These checks prove scoped compilation, not app startup/runtime DI, SDK delivery,
reporting-failure injection, native linking or on-device restoration. Independent
review remains unperformed under the same authorization limitation. The manual
scenarios above remain unrun; no automated test infrastructure was introduced.
