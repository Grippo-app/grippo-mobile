# Secondary goal picker coverage

Scope: `:ui-dialog-features:secondary-goal-picker`, 2026-10-13.

## Surface and observation gate

`ProfileGoalViewModel.onSecondaryGoalPickerClick` opens
`DialogConfig.SecondaryGoalPicker`; the shared DialogContentComponent constructs
SecondaryGoalPickerComponent → retained SecondaryGoalPickerViewModel →
SecondaryGoalPickerScreen. There are no nested screens or other current callers.
This is a live bottom-sheet overlay, not an independent screen or container.
The VM keeps the default null screenPresentation; no screen enum or dedicated
presentation contract is introduced. Foundation still owns any screen lifecycle
binding. ProfileGoal's existing ProfileScreen.Goal hook remains unchanged.

| Owner/callback | Observation | Eligibility/payload |
|---|---|---|
| SecondaryGoalPickerViewModel.onSelectGoal | GoalClicked / secondary_goal_picker_goal_clicked v1 | Existing unconditional card callback, including clicking the current goal; before the existing safeLaunch (state update, 300ms delay and BackWithResult), so an error-popup Retry never repeats it; empty payload |
| SecondaryGoalPickerViewModel.onDismiss | DismissRequested / secondary_goal_picker_dismiss_requested v1 | Existing hardware Back callback; emitted before Back direction; empty payload |
| ProfileGoalViewModel | Existing PickerOpened(Secondary), SecondaryConfirmed | Opening callback and accepted result closure; unchanged, owned by parent |

GoalClicked observes the click, not delivered acceptance: lifecycle destruction
can cancel the existing delayed navigation. No operation API/decorator is needed
for this UI-only delay. Existing business guards, result/navigation contracts,
concurrency and timing are unchanged. No selection initialization emits events.

Deliberate omissions: goal value, dedicated view/close events, host swipe/outside
closing, inferred cancellation/success, downstream save outcomes and attribution.
The shared host/config/Component emits no telemetry. There are no raw inputs,
entity IDs, measurements, secrets, exception strings or route payloads in events.
The stateless API is injected through the existing retainedInstance factory;
shared registers its generated module and uniquely qualified schema catalog.
Ordinary reporting Exceptions cannot block selection/results/Back; fatal Errors
remain fatal.

## Preservation source gate

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| SecondaryGoalPicker VM | Current value, including selection before delayed result delivery | Runtime only | Existing retainedInstance and initial config | Retained VM wins during ordinary recreation; process death discards unfinished session |
| SecondaryGoalPickerState | Goal catalog/localized labels | Rebuild | GoalSecondaryGoalEnumState.entries and current AppTokens | Fresh dialog rebuilds catalog; no archive required |
| ProfileGoal VM/Component | Accepted selectedSecondary, including null | Preserve (already implemented) | ProfileGoalDraft / ProfileGoalState.serializer() | restoreState inside retainedInstance, lazy saveState supplier; saved state skips source initialization; accepted callback updates same draft |
| ProfileGoal VM | Fresh goal prefill | Rebuild | GoalFeature.observeGoal, existing one-shot initialization | Only absent archive starts prefill; fresh reopened picker receives current accepted selectedSecondary |
| Shared host/Component/foundation | Sessions, callbacks, directions, delay jobs/loaders, telemetry provenance | Runtime only | childSlot/childStack serializer = null | No session serialization, synthetic callback replay, restore flags or new draft supplier |

No persistence changes are required under the canonical dialog policy. Parent
accepted results already have local StateKeeper ownership; unfinished dialog
input must remain unsaved. No secrets are introduced into archives. Android
restoration requires a supplied saved-state archive; iOS requires a completed
valid background checkpoint from the same build. Abrupt active iOS termination
has no restoration guarantee.

## Verification and limits

Source review traced actual caller, result delivery, retained VM, both event/schema
branches, privacy, dependency directions, DI registration and parent save/restore.
Screen classification and state-preservation source gates passed in this scope.
Independent review was unavailable: agent delegation was not authorized; the
available review connector exposes PR CI diagnostics, not a local source reviewer.
No automated tests or verification-only production hooks were added.

Unrun manual checks: click each/current goal and inspect exactly one click event;
hardware Back emits dismiss, host swipe/outside does not; reporting Exception
injection preserves behavior; rotate during delayed selection; accept goal, save
and roundtrip archive, destroy lifecycle/InstanceKeeper, create fresh contexts/VMs,
verify parent goal and reopened picker seed; process death with unfinished picker
closes session without accepting its temporary value. Android/iOS native lifecycle,
DI runtime, callback races and SDK delivery remain unverified.

Fresh build results: UI `compileAndroidMain` and
`compileKotlinIosSimulatorArm64` passed together with `--offline
--no-configuration-cache` (BUILD SUCCESSFUL, exit 0, 2m 12s). Generated Koin output
was inspected: SecondaryGoalPickerAnalytics binding and the qualified
secondary_goal_picker_analytics schema supplier are present. Scoped inline
source invariants and `git diff --check` passed. Full shared app compilation and
runtime DI were not exercised.

Earlier attempts: sandbox wrapper lock permission failure; unrelated concurrent
startTraining accessor registration failure; then a missing error-provider
classpath snapshot during parallel build activity. The UI retry succeeded without
cache deletion or infrastructure changes. Existing global opt-in warnings remain.

Standalone final analytics verification: `:analytics:secondary-goal-picker`
`compileAndroidMain` and `compileKotlinIosSimulatorArm64` passed together
with `--offline --no-configuration-cache` (BUILD SUCCESSFUL, exit 0, 29s).
These targets had already compiled in the earlier combined run before its
unrelated UI dependency failure; the standalone run confirms a clean scoped result.

## Residual audit (2026-10-13)

Rechecked all five analytics source files, UI injection/callback consumers, unique
wire codes, exhaustive schema mapping, Gradle/shared registrations, current caller,
parent draft restoration and coverage links. No orphan models, unused APIs,
duplicate registrations, template remnants or preservation-owner gaps were found;
no production correction was needed. Fresh scoped source invariants and scoped
`git diff --check` passed. Independent review and manual/runtime limits above remain.

The fresh combined Android/iOS Simulator check reached all four secondary-goal-picker
compile targets successfully (UP-TO-DATE). The additional shared KSP integration
attempt did not complete: unchanged goal-setup-suggestion metadata compilation
failed with NoSuchFileException for analytics-api generated commonMain linkdata
while other builds were active. This is an observed missing output, not proof of
its cause. Full shared integration remains unverified; no unrelated source or
cache cleanup was applied.
