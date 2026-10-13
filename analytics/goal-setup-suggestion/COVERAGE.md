# Goal setup suggestion coverage

Scope: `ui-dialog-features:goal-setup-suggestion`, reviewed 2026-10-13.

## Surface and screen coverage gate

| Route / rendered surface | Component → ViewModel | Classification / presentation |
|---|---|---|
| DialogConfig.GoalSetupSuggestion, opened by MainViewModel.startNewTraining | GoalSetupSuggestionComponent → GoalSetupSuggestionViewModel | Live bottom sheet with benefit text and two buttons; default null screenPresentation, no dedicated dialog presentation contract |

There are no nested or independent screens in this module. Shared DialogComponent
and DialogContentComponent render the live slot/step with serializer = null.
The Component injects the stateless analytics service inside retainedInstance;
shared registers its generated Koin module and qualified schema catalog once.
No feature lifecycle binding or analytics persistence is added.

## Callback / operation coverage

| Existing observation boundary | Version-1 event | Meaning |
|---|---|---|
| onConfigure | goal_setup_suggestion_configure | Configure button intent, before Configure direction |
| onLater | goal_setup_suggestion_later | Later button intent, before Later direction |
| onBack | goal_setup_suggestion_back | Registered hardware Back callback, before Back direction |

All events have empty payloads. Callbacks have no existing eligibility/busy guards;
no new guard or deduplication changes their behavior. The closed public event
family is encoded exhaustively by an internal matrix. Ordinary reporting Exceptions
are isolated in the implementation; fatal Errors remain fatal. Navigation and live
result callbacks are unchanged. There are no asynchronous business operations,
inputs or accepted picker results in this VM, so no operation decorator is needed.

Omissions: dedicated view/close telemetry, swipe/outside dismissal (host-owned),
caller opening intent, downstream goal setup/workout outcomes and cross-owner
attribution. Main's shouldSuggest/markShown remain caller-owned and outside this
module's operation coverage; button events do not claim downstream success.

## State preservation source gate

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or source | Restore/default and check |
|---|---|---|---|---|
| GoalSetupSuggestionViewModel | benefits | Rebuild | GoalSetupSuggestionState.DEFAULT_BENEFITS | Fresh VM uses static Tailored benefit; localized labels/icons render from current AppTokens |
| Component / shared dialog host | Session, callbacks, directions | Runtime only | Existing retainedInstance; host serializers null | Retained recreation keeps VM/session; process death discards the sheet and callbacks |
| GoalSetupSuggestionViewModel | User inputs, selections, filters, parent drafts | None | No editable fields or accepted values | No StateKeeper supplier, serializer or DRAFT key required |
| Caller / existing settings owner | Last suggestion shown timestamp | Existing durable source | GoalSetupSuggestionUseCase.markShown → LocalSettingsFeature | Already committed before dialog opening; no dialog snapshot needed |
| Foundation / host | Jobs, loaders, presentation provenance | Runtime only | Existing owners | No runtime state or secrets serialized |

No local business draft exists. Configure and Later invoke the existing caller
callbacks after finishing the dialog; Back dismisses without invoking those two
callbacks. There is no fresh child/parent draft seed or restoration initialization
that could replay an action. Ordinary retention and process-death restoration
are different: this dialog intentionally does not reopen from a saved archive.

## Verification

Source review traced the entry/config, shared factories, rendered buttons, hardware
Back, retained VM injection, event schemas, DI/catalog registration and settings
ownership. Screen coverage and state-preservation source gates passed within this
module. The follow-up source audit checked all three callback/event/schema mappings,
version-1 empty payloads, unique bounded event codes and exactly one settings,
shared dependency and generated-module registration. Whole-tree `git diff --check`
passed on the follow-up tree; the previously noted trailing blank line is gone.
No implementation defects were found in this module.

Confirmed follow-up results:

- `:analytics:goal-setup-suggestion:compileAndroidMain --no-configuration-cache`:
  BUILD SUCCESSFUL, exit 0, including common KSP generation. The generated module
  binds GoalSetupSuggestionAnalyticsImpl to GoalSetupSuggestionAnalytics and
  registers the qualified schema supplier; both bindings were inspected.
- `:ui-dialog-features:goal-setup-suggestion:compileAndroidMain --no-configuration-cache`:
  BUILD SUCCESSFUL, exit 0, including common KSP generation. Existing unresolved
  ExperimentalForeignApi opt-in warning remains; no convention/dependency changes.
- `:shared:kspCommonMainKotlinMetadata --no-configuration-cache`: BUILD SUCCESSFUL,
  exit 0, 1477 actionable tasks. This verifies shared metadata processing/KSP,
  not full shared platform compilation or runtime dependency resolution.

Historical unsuccessful attempts: sandbox wrapper-cache access, unrelated shared
project accessors, interrupted configuration (exit 130), missing foundation KSP
output and missing resources-provider metadata. Parallel Gradle processes and an
actual file-lock wait were observed. Those failures did not establish feature
compilation; the successful target runs above supersede that limitation. No unrelated
infrastructure was changed. iOS compilation and actual DI runtime were not exercised.

Unrun manual checks: press each button and hardware Back and inspect its event and
existing navigation; inject an ordinary reporting failure and verify navigation;
rotate with the dialog open; perform a fresh Android saved-state restart or supported
iOS background-checkpoint restart and confirm the dialog is closed without callbacks
being replayed, while the existing shown timestamp remains stored. Swipe/outside
closing must not synthesize Back. Firebase delivery and native shell behavior were
not exercised. No automated tests or verification-only production hooks were added.
