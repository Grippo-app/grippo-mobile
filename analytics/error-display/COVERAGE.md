# Error display analytics and recreation policy

Scope: `ui-dialog-features/error-display`, reviewed on 2026-10-13.

## Surface and observation inventory

| Route / surface | Component → ViewModel | Presentation policy |
|---|---|---|
| DialogConfig.ErrorDisplay, opened by the existing error provider | ErrorDisplayComponent → retained ErrorDisplayViewModel → ErrorDisplayScreen | Live bottom-sheet overlay; default null screenPresentation; no dedicated dialog presentation contract |
| Shared slot / content stack | DialogComponent / DialogContentComponent | Containers; no added presentation |

Screen coverage source gate: no independent screen is introduced or changed.
The error overlay follows the existing dialog policy; it requires no screen enum,
screen hook or feature lifecycle wiring. Analytics is injected through the normal
retainedInstance factory. Shared registers the generated annotated module and its
uniquely qualified `error_display_analytics` schema catalog once.

| Callback / path | Observation | Meaning and eligibility |
|---|---|---|
| ErrorDisplayViewModel.onDismiss | DismissRequested / error_display_dismiss_requested v1 | Existing unconditional callback; emitted before the unchanged Back direction |
| Got-it button | Same VM callback | Dismiss request, not a confirmed host close |
| Component BackCallback | Same VM callback when dispatched to this component | No inferred button/back source; host Back can bypass the leaf |
| Host swipe/outside dismissal, programmatic closure | Deliberate omission | May bypass onDismiss; shared plumbing does not construct feature events |
| Open intent, dialog view/close | Deliberate omission | No owning feature callback/presentation contract in this scope; no event inferred from construction |

The payload has no parameters: no error type/classifier, message, title,
exception, URL, route ID, input length, credential or reporting history is sent.
There is no asynchronous business operation to decorate and no operation API.
Ordinary reporting Exceptions are isolated by the implementation and cannot block
navigation. Fatal Errors propagate. Repeated callbacks remain repeated observations;
no exactly-once guard or new navigation policy is introduced.

## Preservation inventory

| Owner | Field/group | Preserve / Rebuild / Runtime only | Source / restoration and check |
|---|---|---|---|
| ErrorDisplayComponent / VM | ErrorDisplayState.error | Runtime only; retained in live session | Existing error route input seeds a fresh VM; retainedInstance keeps the current VM while its InstanceKeeper survives |
| ErrorDisplayScreen | Localized description | Rebuild | Existing AppErrorState.description() / UiText.text() at rendering |
| Dialog host / component | Live session, Back callback, pending dismissal | Runtime only | Slot and content stack use serializer = null; fresh process has no restored dialog session |
| Foundation VM | Jobs/loaders and telemetry | Runtime only | No StateKeeper supplier, snapshot, secret or analytics provenance added |

Preservation source gate: the dialog has no editable inputs, selections, filters,
child results or accepted parent draft. Error presentation and its dismissal
callback are runtime session data, explicitly excluded by the canonical policy.
Adding a supplier would not restore the unsaved session and could archive raw
error content. No serializer, shell bridge or persistent owner is needed.
Ordinary recreation retains the existing live owner when InstanceKeeper survives;
actual process death discards the error dialog. Reopening uses the current error.
Restoration emits no synthetic dismissal/input event. UI layout is unchanged;
existing preview and rendering consumers were inspected.

## Verification

Source/diff self-review covers the callback chain, exhaustive event encoding,
empty payload/privacy, reporting isolation, retained VM injection, dependency
boundaries and shared DI registration. Screen and preservation source gates pass.
No automated test sources, dependencies or infrastructure were added.

Independent review was unavailable: no permitted independent local reviewer was
available in this chat. Compilation results are recorded below.

Manual checks not run: Got-it and component/host Back paths, swipe/outside
closure omissions, ordinary Activity recreation with a retained error, actual
process death with no restored dialog, reporting Exception injection and real
SDK delivery. Compilation does not prove these runtime scenarios.

Verification results: inline scoped source invariants and `git diff --check`
passed. The scoped Android/iOS Simulator build, including shared integration,
was attempted with `--offline --no-configuration-cache`. It failed during shared
Gradle script configuration, before the requested compile tasks ran:
`projects.analytics.iterationPicker`, `monthPicker` and `menuPicker` were unresolved
in the current shared/build.gradle.kts. These unrelated working-tree references
were left unchanged. Compilation and generated Koin integration remain unverified.
The initial sandbox run could not write the Gradle wrapper lock; the authorized
retry reached the configuration failure above. No infrastructure/cache changes
were made to bypass it.

A later retry used the same scoped command after the missing module includes
appeared in the concurrently edited settings.gradle.kts. It made no visible
progress beyond build-logic/configuration for several minutes while Gradle
reported six busy daemons, and was interrupted (exit 130). No other builds were
stopped. This retry provides no compilation evidence; the compile and generated
DI checks remain unverified.

## Residual review on 2026-10-13

Fresh source/diff review found no orphan API/types/files, unused event variants,
duplicate DI/schema registrations, template remnants, missing callback consumer
or required state-preservation owner in the error-display scope. The button
calls the leaf VM; host/system Back may bypass it, as already documented above.
No code correction was required. Scoped inline source invariants and final
`git diff --check` passed.

Compilation was retried, first with shared integration and then narrowed to the
two error-display modules using `--offline --no-configuration-cache
--configure-on-demand`. Gradle reported nine/ten busy daemons; the narrow attempt
produced no compile-task output or analytics/error-display build directory before
it was interrupted (exit 130). Only this chat's own attempts were interrupted.
No other builds, caches or infrastructure were modified. Android/iOS compilation,
generated DI integration, independent review and the manual scenarios above remain
unverified; the retry is not a successful build.
