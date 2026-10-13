# Weight picker coverage (2026-10-13)

Scope: DialogConfig.WeightPicker → DialogContentComponent.Child.WeightPicker →
WeightPickerComponent → retained WeightPickerViewModel → WeightPickerScreen.
Three production entry paths share this one live overlay:

- UserViewModel.onWeightPickerClick (profile creation).
- ProfileBodyViewModel.onWeightPickerClick (body edit).
- IterationPickerViewModel.onWeightPickerClick → PickBodyWeight direction →
  IterationPickerComponent → DialogContentComponent's onPickBodyWeight push
  (nested body-weight step inside the iteration session).

No nested independent screen is introduced by weight-picker itself.

## Analytics source gate

| Surface / callback owner | Observation | Boundary |
|---|---|---|
| WeightPickerScreen / Component / VM | Default null screenPresentation | Dialog policy; no separately defined presentation contract, screen enum or screen hook |
| VM.onSelectWeight | WeightSelected / weight_picker_weight_selected v1 | Existing callback; emit only when formatted state changes, then keep original state update |
| VM.onSubmitClick | SubmitClicked / weight_picker_submit_clicked v1 | Before unchanged BackWithResult; intent, not proof of parent acceptance |
| VM.onDismiss | DismissRequested / weight_picker_dismiss_requested v1 | Before unchanged Back; request, not proof of completed dismissal |
| UserViewModel | Existing PickerOpened(Weight), PickerConfirmed(Weight, status) | Open intent and accepted result remain parent-owned |
| ProfileBodyViewModel | Existing Body.PickerOpened(Weight), Body.PickerConfirmed(Weight, status) | Open intent and accepted result remain parent-owned |
| IterationPickerViewModel | Existing BodyWeightOpened, BodyWeightConfirmed(status) | Nested opening and accepted weight remain parent-owned |

All three schemas have empty parameters: weight/body measurements, raw inputs,
IDs and exception messages are excluded. Wheel callbacks include automatic sync
and correction; WeightSelected observes changed selection, not exclusively user
scrolls. Equal callbacks emit nothing without changing business callback behavior.
No synthetic initialization events are emitted by the VM constructor.
Normal retainedInstance injection resolves the stateless API; shared registers its
annotated generated module and uniquely named complete schema catalog. Ordinary
reporting Exceptions are isolated; fatal Errors remain fatal. Business updates,
result types, eligibility and navigation are unchanged.

Omissions: dialog_view/close, host-handled Back/swipe/scrim/toolbar dismissal (bypasses this VM),
weight value, scroll direction/column, parent acceptance and operation duration.
No asynchronous business operation exists here. Screen coverage gate passes by
source classification: one overlay, no independent page or ownerless screen enum;
no new presentation/lifecycle wiring is required. Existing independent parent
owners retain AuthScreen.User, ProfileScreen.Body and TrainingScreen.Exercise
hooks; the nested IterationPicker overlay keeps null. Host/foundation wiring is
unchanged; the same analytics injection covers direct and nested factories.

## Preservation source gate

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| WeightPicker VM | Current value, including unfinished choice | Runtime only | Existing retainedInstance; caller initial | Retained recreation keeps live selection; process death closes dialog |
| WeightPicker State | suggestions | Rebuild | DefaultWeightSuggestions / WeightLimitation | Fresh session rebuilds static catalog |
| UserComponent / UserState | Accepted weight | Preserve, existing | UserDraft / UserState.serializer() | restoreState in retained factory; lazy saveState supplier; restored draft precedes fresh defaults |
| ProfileBodyComponent / ProfileBodyState | Accepted weight before Apply | Preserve, existing | ProfileBodyDraft / ProfileBodyState.serializer() | Local archive wins; source initializes only a fresh draft; user/history rebuild transiently |
| IterationPicker VM / State | Accepted nested bodyWeight before iteration Submit | Runtime only | Existing live iteration session State.value.bodyWeight | Nested confirmation updates parent dialog; reopening uses accepted value; process death closes both dialogs |
| TrainingExerciseComponent / State | Accepted iteration including bodyWeight after iteration Submit | Preserve, existing | TrainingExerciseDraft / TrainingExerciseState.serializer() | Iteration result adds/replaces exercise.iterations; local archive wins over route seed |
| Host / Component / VM | Session, callbacks, directions, loaders, telemetry | Runtime only | Slot and step stack serializer = null | Not saved or replayed |

No local picker PersistenceKey, state serializer or storage is required under the
canonical dialog policy. Direct parent onResult updates UserState.weight or
ProfileBodyState.weight. Nested onBodyWeightSelected updates the live iteration;
only iteration Submit transfers it into TrainingExerciseState.exercise.iterations.
Reopening seeds from the latest accepted value at that owner. Confirmation inside
the nested picker alone does not preserve the unfinished iteration across process
death. No secrets or analytics provenance enter archives.
Source gate passes: all three entry/result chains, retained ownership, host
nonserialization and existing parent save/restore wiring reviewed. Parent code
remains unchanged.

## Verification

Source/diff review: typed events, exhaustive schema mapping, empty payloads,
exception isolation, dependency direction, injection and shared registration.
No automated test sources or infrastructure added. Build results recorded below.
Independent review is unperformed: no permitted independent local reviewer is
available in this chat. Self-review does not count as independent review.
Visual impact: no layout, controls, preview inputs or rendered State changed;
existing WeightPickerScreen/WeightWheelPicker previews remain applicable.
Runtime exception injection, native DI execution, SDK delivery and Android/iOS
process restoration are unrun; static review does not prove them.

Permitted manual checks: open/select/submit and verify accepted parent weight;
reopen and inspect initial selection; Back leaves parent unchanged; ordinary
recreation with retained host keeps live selection. Save shell state → destroy old
lifecycle/InstanceKeeper → restore with fresh owners: dialog closes and accepted
parent weight survives in both registration and profile. Nested flow: confirm
body weight → reopen picker within iteration and check latest seed; submit the
iteration → restore TrainingExerciseDraft and check bodyWeight. Confirm weight
without submitting iteration → process restart must discard that live iteration
and keep the previous screen draft. Unconfirmed choice must not replace parent
weight. On iOS, use a completed matching background checkpoint; abrupt foreground termination
has no restoration guarantee. Also verify equal wheel callbacks emit no selection
event, changed callbacks emit one, and reporting Exceptions do not block actions.

Build results:

- Initial metadata/shared KSP run and first combined platform run stopped during
  prolonged configuration (exit 130); no success claimed.
- Offline scoped run including shared KSP failed at
  `:compose-libs:chart:compileCommonMainKotlinMetadata` with unresolved Compose
  runtime/ui symbols. Analytics Android compilation and its KSP generation passed.
- Platform-only retry failed at `:ui-core:state:compileCommonMainKotlinMetadata`:
  missing `build/kotlinTransformedMetadataLibraries/commonMain/commonMain.json`.
  At that point UI platform and shared DI compilation were unconfirmed. Shared
  source wiring and generated weight-picker Koin bindings were inspected.
- Independent `:analytics:weight-picker:compileAndroidMain` and
  `:analytics:weight-picker:compileKotlinIosSimulatorArm64` with offline,
  no-configuration-cache, configure-on-demand and project-cache-dir
  `/tmp/grippo-weight-picker-cache`: BUILD SUCCESSFUL (10 s, exit 0).
  Existing convention opt-in markers produce unresolved-marker warnings.
- Final `git diff --check`: passed.

## Residual review (2026-10-13)

Corrected the previously incomplete entry inventory: registration, profile body
and the nested iteration body-weight picker use the same Component/VM. Traced
all parent callbacks and existing archives, including the distinction between an
accepted nested weight and an accepted whole iteration. Added the scoped record
to the shared analytics coverage index. Clarified host-handled Back omissions,
no visual impact and the unavailable independent-reviewer limitation.
No orphan models, unused events, copied goal-picker names, duplicate schemas/DI,
raw measurements or missing required local preservation owners were found in the
resulting scoped source. No business code change was needed in this review.

Fresh combined integration attempt failed before UI compilation at
`:compose-libs:konfetti:compileCommonMainKotlinMetadata`, with an internal compiler
error rooted in `ZipException: loc: reading failed`. The isolated UI retry then
passed both `:ui-dialog-features:weight-picker:compileAndroidMain` and
`:ui-dialog-features:weight-picker:compileKotlinIosSimulatorArm64` together
(BUILD SUCCESSFUL, 1m 36s, exit 0), superseding the earlier UI compilation omission.
Runtime/manual and independent-review limitations remain unchanged.

The final isolated shared integration attempt revalidated analytics Android and
iOS Simulator compilation (FROM-CACHE) but failed in the unchanged dependency
`:data-features:local-settings:transformAppleMainDependenciesMetadata`: missing
`build/kotlinTransformedMetadataLibraries/nativeMain/nativeMain.json`
(BUILD FAILED, 2m 7s, exit 1). Shared integration compilation remains unverified;
no cache deletion or unrelated infrastructure/source changes were applied. The
successful isolated UI build above remains valid. Final scoped source review and
`git diff --check` passed; no unaccepted source defect remains in weight-picker.
