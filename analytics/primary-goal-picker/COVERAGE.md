## Current contract follow-up (2026-10-14)

The [current analytics review](../COVERAGE.md#current-analytics-contract-review-2026-10-14)
records this revision and its verification limits. Typed dialog observation names
use DismissRequested across screens/dialogs, with dismiss_requested wire names/codes.
The app has not been released; all analytics schemas use initial version 1.

# Primary goal picker coverage (2026-10-13)

Scope: ProfileGoalViewModel.onPrimaryGoalPickerClick → DialogConfig.PrimaryGoalPicker
→ DialogContentComponent.Child.PrimaryGoalPicker → PrimaryGoalPickerComponent
→ retained PrimaryGoalPickerViewModel → PrimaryGoalPickerScreen. This is the only
production entry and rendered surface; no nested screens or business operations.

## Analytics source gate

| Surface / callback owner | Observation | Boundary |
|---|---|---|
| PrimaryGoalPickerScreen / Component / VM | Default null screenPresentation | Live bottom sheet, no dedicated presentation contract; no screen enum required |
| VM.onSelectGoal | PrimaryGoalPickerEvent.GoalClicked / primary_goal_picker_goal_selected v1 | Before the existing safeLaunch (state update and unchanged 300 ms delay/result direction), so an error-popup Retry never repeats it; unconditional callback |
| VM.onDismiss | PrimaryGoalPickerEvent.DismissRequested / primary_goal_picker_dismiss_requested v1 | Before unchanged Back direction; request, not completed dismissal |
| ProfileGoalViewModel | Existing Goal.PickerOpened(Primary), Goal.PrimaryConfirmed | Open intent and accepted result remain at the parent owner |

Both new schemas have empty payloads: no goal value, IDs, text or measurements.
The stateless service is resolved in the normal retainedInstance factory; shared
registers its generated module and uniquely qualified complete schema catalog.
Ordinary reporting Exceptions are isolated inside the implementation; fatal Errors
remain fatal. No business guards, timing, jobs, result contracts or error pipeline
were changed. Rapid selection callbacks retain existing behavior and are not
claimed to be debounced. Cancellation during delay can prevent result delivery;
selection observation does not imply acceptance.

Deliberate omissions: selected goal identity, dialog_view/close, host swipe/scrim/
toolbar dismissal (bypasses this VM), inferred result delivery and operation timing.
The delay is UI timing, not a business operation requiring decoration. The parent
already has ProfileScreen.Goal presentation; neither it nor foundation was changed.
Screen coverage gate: actual factory, rendered overlay and retained VM inspected;
no new independent screen, container presentation or ownerless screen enum added.

## Preservation source gate

| Owner | Field/group | Classification | Source / key | Restore/default |
|---|---|---|---|---|
| Picker VM / State | value, including selection pending delayed result | Runtime only | Existing retained VM; initial caller value | Ordinary retained recreation keeps state; process restoration closes session |
| Picker State | goals | Rebuild | GoalPrimaryGoalEnumState.entries | Fresh live session reconstructs immutable catalog |
| ProfileGoalComponent / State | Accepted selectedPrimary | Preserve, already implemented | ProfileGoalDraft / ProfileGoalState.serializer() | restoreState in retained factory; lazy saveState supplier; saved null/default takes precedence over source initialization |
| Host / Component / VM | Session, callbacks, delay job, loaders, directions, telemetry | Runtime only | Slot and step stack serializer = null | Not archived or replayed |

No local picker StateKeeper key or serialization plugin is required by the dialog
policy. The accepted result updates the existing parent draft; reopening seeds the
picker from its latest value. Parent restoration skips fresh source initialization
when an archive exists. No secrets or telemetry provenance enter saved state.
Reviewed the route factory, callback delivery, parent source initialization and
restore/save wiring; source review does not prove process-restoration runtime.

## Verification

Source/diff review covers both events, payload privacy, exception isolation,
dependency direction, injection, explicit shared registration and the preservation
inventory. No automated test sources or infrastructure were introduced.
Independent review was unavailable: no authorized separate reviewer in this task.
Runtime reporting-failure injection, native DI execution, SDK delivery and
Android/iOS restoration remain unrun.

Permitted manual checks: open picker, select a goal and verify the parent value;
reopen and check its highlight; Back leaves parent unchanged; ordinary recreation
with retained host keeps live selection; supported shell save → destroy old
lifecycle/InstanceKeeper → fresh restoration keeps the accepted parent goal and
closes the dialog. Termination before delayed result acceptance keeps the previous
parent value. iOS requires a completed matching background checkpoint; abrupt
foreground termination is not guaranteed to restore state.

Build checks: requested Android and iOS Simulator compilation for analytics and
picker UI, plus shared Android compilation. Initial sandboxed launch could not
write the Gradle wrapper lock; the authorized cache-access retry reached project
configuration and failed on an unrelated concurrent secondaryGoalPicker accessor
registration gap. A second authorized retry after that registration appeared
remained at project configuration after build-logic tasks and was interrupted
(exit 130), without reaching feature compilation. Compilation and generated DI
therefore remain unverified; no build infrastructure was altered. Final
`git diff --check` passed. Unrelated concurrent feature changes were preserved.

### Tail recheck (2026-10-13)

Re-reviewed the actual picker, parent ProfileGoal restore/save/accepted-result
wiring, runtime host boundary and shared registrations. No scoped production
findings. Additional source checks passed: unique schema names within the catalog,
wire-name length, analytics implementation dependency boundaries, two-file public
API, and single module/dependency/DI registration. `git diff --check` passed.
These checks are source-only, not runtime or generated-DI proof.

Repeated the same scoped Android/iOS Simulator/shared compilation with `--offline`.
Gradle reported seven busy daemons, progressed through up-to-date build-logic tasks
and remained in project configuration; the attempt was interrupted (exit 130)
before feature compilation. Compilation, reporting-failure injection, native
restoration and independent review remain unverified. No unrelated production
changes, automated test sources or build infrastructure changes were introduced.
