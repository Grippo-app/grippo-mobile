## Menu picker dialog (2026-10-13)

Scope: `DialogConfig.TrainingMenuPicker` → `DialogContentComponent.Child.MenuPicker`
→ `MenuPickerComponent` → retained `MenuPickerViewModel` → `MenuPickerScreen`.
The sole production factory supplies `TrainingMenu.entries`; there are no nested
screens, alternate production entries or asynchronous operations in this module.

### Screen and callback coverage

| Surface / owner | Typed observation / hook | Boundary |
|---|---|---|
| MenuPickerScreen / MenuPickerComponent / MenuPickerViewModel | Default null screenPresentation | Live bottom-sheet overlay; no separate presentation contract, no screen enum or screen_view |
| onItemClick | MenuPickerEvent.ItemClicked / menu_picker_item_clicked v1 | Existing unconditional callback, before unchanged BackWithResult(item); empty payload |
| onDismiss | MenuPickerEvent.DismissRequested / menu_picker_dismiss_requested v1 | Existing component BackCallback, before unchanged Back; request does not imply delivered dismissal |

The stateless MenuPickerAnalytics is injected in the normal retainedInstance
factory. Its internal implementation isolates ordinary reporting Exceptions;
fatal Errors remain fatal. One qualified schema catalog is registered through
MenuPickerAnalyticsModule in shared. No reporting state, lifecycle jobs, route
arguments or business guards were added. Rendering and existing previews are unchanged.

Deliberate omissions: item identity is omitted from the generic picker event.
The actual TrainingMenuPicker caller already observes DetailsSelected, EditSelected
and DeleteSelected in TrainingsViewModel's accepted-result callback; these are
separate observations from a picker click. Raw IDs, localized text, URLs and
entity data never enter this payload. Opening remains owned by the caller's
TrainingMenuClicked event. No inferred result delivery, cancellation, operation,
dialog_view or dialog_close events are emitted. Host swipe/scrim/toolbar dismissal
bypasses this VM callback and remains outside this module's observation boundary.

### State preservation inventory

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| MenuPickerComponent / MenuPickerState | items | Rebuild for a fresh live session; retained in existing VM during ordinary recreation | Caller-supplied TrainingMenu.entries; no draft key | Fresh VM copies items into ImmutableList; no editable draft or pending selection |
| Dialog host / MenuPickerComponent | Session, onResult and back callbacks | Runtime only | Session slot and step stack both serializer = null | Process restoration discards dialog; retained host owns ordinary recreation |
| MenuPickerViewModel | Click result | Runtime direction, no local selection draft | Existing BackWithResult(item) → guarded host finish → caller | Accepted action navigates or invokes the caller's existing operation; picker adds no parent data |
| Foundation | Loaders, navigation, analytics lifetime | Runtime only | Existing BaseViewModel / BaseComponent | No archive fields or telemetry provenance |

Preservation source gate: inspected State, VM members, callbacks, component
retention, production factory and both host serializers. No user-owned unfinished
input exists; no StateKeeper supplier or serialization plugin is required.
No secrets are persisted. Parent screen drafts remain owned by their screens;
this menu returns an action, not a form value requiring a new saved field.

Verification: source/diff review covers the event matrix, empty payloads, unchanged
callback order/results, exception isolation, dependency direction and generated
module registration. Runtime checks remain unrun: item click and Back with a live
sink, reporting-failure injection, rapid result dispatch, ordinary recreation and
Android/iOS process restoration. Permitted manual checks: open menu and recreate
while the retained host lives; verify items and callbacks; select each action;
restart from a supported saved shell archive and verify the menu stays closed.
Firebase delivery and native shell execution are not established by compilation.

### Original implementation verification

Menu-picker verification outcome: permitted source checks passed for both event
variants, callback-before-direction ordering, empty payload privacy, Exception-only
isolation, API/implementation dependency boundaries, explicit shared registration
and absence of unnecessary draft persistence. `git diff --check` passed.
Scoped Gradle command requested analytics and UI Android/iOS simulator compilation
plus shared Android compilation. The first attempt failed during configuration on
an unrelated concurrent `projects.analytics.iterationPicker` registration gap.
After that registration appeared, both normal and offline retries stalled during
project configuration without reaching compilation and were interrupted (exit 130).
At that point compilation, generated DI and platform runtime were unverified; no unrelated
build infrastructure or working-tree changes were modified to force verification.


### Follow-up review (2026-10-13)

Moved this feature record out of the shared coverage file and removed the displaced
verification paragraph from beneath other features. The shared index now links here.
No production defect was found in the menu-picker diff. Rechecked event ownership
and order, unique schema names and qualifier, empty payloads, retained DI injection,
implementation boundaries and the no-draft dialog preservation assessment.
`git diff --check` passed. Inspected the actual KSP-generated module: it binds
MenuPickerAnalyticsImpl to MenuPickerAnalytics and registers the qualified schema.

Separate scoped verification completed with `BUILD SUCCESSFUL in 6s` (exit 0):
`:analytics:menu-picker:compileAndroidMain` and
`:analytics:menu-picker:compileKotlinIosSimulatorArm64`. The generated module and
both compilation outputs were reused from the preceding integrated attempt.

The integrated UI attempt reached analytics compilation on both targets, then
failed in `:ui-core:foundation:kspCommonMainKotlinMetadata` with a KSP FIR resolution
error (`RAW_FIR` → `TYPES`, `SEALED_CLASS_INHERITORS`). Foundation source was not
changed by this task. UI Android/iOS compilation and shared integration therefore
remain unverified. Runtime restoration, reporting-failure injection and Firebase
delivery remain unrun. No automated test sources or verification-only production
hooks were added.
