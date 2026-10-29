# Runtime scenario evidence

Use this procedure for runtime scenarios authorized under the
[verification policy](../../../../CLAUDE.md#verification-policy). Read the
selected platform's [Android](android.md) or [iOS simulator](ios.md) recipe
before device actions.

## Preconditions

- Sign-in: agents never type credentials. The user signs in on the emulator or
  simulator before the scenarios; a scenario that needs a logged-in state is
  blocked until then and says so.
- Use the selected device identifier and literal absolute scratchpad paths under
  the platform recipe and [shared prerequisites](../SKILL.md#preconditions).

## Scenario discipline

1. Record `git rev-parse --short HEAD`, dirty-file count and the
   [task baseline/input content](../../task-orchestrator/references/change-attribution.md)
   identifying the working tree actually built. A dirty-file count alone cannot
   identify changed source. Relevant concurrent edits require rebuilding affected
   artifacts and repeating their scenarios before reusing the evidence.
2. Write each scenario as steps with an expected outcome before running it.
3. After each meaningful step take a screenshot named
   `<scenario>-<step>-<platform>-<theme>[-<locale>].png` in the session
   scratchpad (never inside the repository) and note the observed state.
4. On a failure capture the log window around it and stop that scenario; do not
   "fix" the app or retry blindly.
5. Restore every device setting you changed (appearance, locale, developer
   options, always-finish-activities) and say so.

## What runtime evidence establishes

- A screenshot proves the rendered state on that device, OS version, theme and
  locale at that revision. It does not prove other devices, font scales or motion.
- A completed restoration scenario proves the saved-state path that was used
  (Android kill/relaunch or iOS background/terminate/relaunch), not every cold start.
- Observed analytics logging proves emission from the app, not delivery to
  Firebase or admin processing.

## Completion gate

- Every requested scenario has an observed result or a concrete reason it could
  not run, with failures and missing coverage explicit.
- Evidence identifies the built inputs, selected device, OS, theme and locale;
  screenshots and log excerpts correspond to the reported steps.
- Relevant source changes did not invalidate the build or scenario evidence.
- Changed device settings are restored, or a remaining setting is reported.

## Report

```
Revision: <sha> (+<n> dirty files); Inputs: <baseline/content snapshot>; Device: <name, serial/UDID, OS>; Build: <command -> result>
| Scenario | Step | Expected | Observed | Evidence | Result |
Failures with log excerpts; Not exercised with reasons; Settings restored: yes/no
```
