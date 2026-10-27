# Error display analytics and recreation policy

Scope: `ui-dialog-features/error-display` and the shared error delivery/dismissal owner.

## Surface and observation inventory

| Route / surface | Component → ViewModel | Presentation policy |
|---|---|---|
| DialogConfig.ErrorDisplay, opened by the existing error provider | ErrorDisplayComponent → retained ErrorDisplayViewModel → ErrorDisplayScreen | Live bottom-sheet overlay, a root session or a step pushed above an open dialog; default null screenPresentation; no dedicated dialog presentation contract |
| Shared slot / content stack | DialogComponent / DialogContentComponent | Containers; no added presentation |

Screen coverage source gate: no independent screen is introduced or changed.
The error overlay follows the existing dialog policy; it requires no screen enum,
screen hook or feature lifecycle wiring. Analytics is injected through the normal
retainedInstance factory. Shared registers the generated annotated module and its
uniquely qualified `error_display_analytics` schema catalog once.

`BaseViewModel.safeLaunch` asks `ErrorProvider.retry(error)` (for `ErrorRecovery.Retry`)
or `ErrorProvider.inform(error)` (for `ErrorRecovery.Close`), which open
`DialogConfig.ErrorDisplay(mode, onClose)` with `Mode.Retry(onRetry)` or
`Mode.Inform`. `onRetry` completes the failed operation's pending
`ErrorDecision.Retry`; `onClose` completes the close. The host child factory runs the
mode's `onRetry` before finishing the step, and the first completion wins, so a Retry
is never followed by a close. The failed operation's Job waits for that decision and
repeats or ends itself; only `ErrorRecovery.Close(onClose)` runs a continuation after a
close. The host treats it like any opening: with no sheet it starts a session, over
a presented sheet it is pushed onto that sheet's stack, and while a sheet is closing it
waits in the FIFO `pending` queue. `ErrorDisplay` never matches another config in
`hasSameContentAs`, so the host's duplicate check never drops an error. These host
changes emit no synthetic presentation, retry, dismissal or result facts.

| Callback / path | Observation | Meaning and eligibility |
|---|---|---|
| ErrorDisplayViewModel.onAction in `ErrorDisplayModeState.Retry` | RetryRequested / error_display_retry_requested v1 | Primary button labelled Retry (`Mode.Retry`); emitted before the Retry direction. Intent, not a successful retry; it names no operation or error. If the failed operation's Job was cancelled while the popup was open (ViewModel destroyed or its collection cancelled) it is still emitted, but no attempt follows and no `onClose` runs |
| ErrorDisplayViewModel.onDismiss | DismissRequested / error_display_dismiss_requested v1 | Emitted before the unchanged Back direction |
| ErrorDisplayViewModel.onAction in `ErrorDisplayModeState.Inform` (Got it button) | DismissRequested through onDismiss | Rendered only for `Mode.Inform` popups, opened for `ErrorRecovery.Close` and by the foundation fallback for a failure outside the attempt (practically unreachable) (TrainingCompleted save, Registration submit, ProfileCompleted creation, ProfileBody apply, Debug generate training) |
| Component BackCallback | DismissRequested through onDismiss when dispatched to this component | No inferred button/back source; host Back can bypass the leaf |
| Host Back, toolbar back arrow or Close, swipe/outside dismissal, programmatic closure | Deliberate omission | May bypass the leaf; shared plumbing does not construct feature events. They run `onClose`, never the retry |
| Open intent, dialog view/close, retry outcome | Deliberate omission | No owning feature callback/presentation contract in this scope; the repeated operation reports its own attempt through its feature's `trackOperation` |

The payloads have no parameters: no error type/classifier, message, title,
exception, URL, route ID, operation, input length, credential or reporting history
is sent. There is no asynchronous business operation to decorate and no operation
API. Ordinary reporting Exceptions are isolated by the implementation and cannot
block navigation or the retry. Fatal Errors propagate. Repeated leaf callbacks
remain repeated observations; no exactly-once telemetry guard is introduced.

## Preservation inventory

| Owner | Field/group | Preserve / Rebuild / Runtime only | Source / restoration and check |
|---|---|---|---|
| ErrorDisplayComponent / VM | ErrorDisplayState.error, ErrorDisplayState.mode | Runtime only; retained in live session | Existing error route input and `ErrorDisplayModeState.of(config.mode)` seed a fresh VM; retainedInstance keeps the current VM while its InstanceKeeper survives |
| ErrorDisplayScreen | Localized description, button label | Rebuild | Existing AppErrorState.description() / UiText.text(); `ErrorDisplayModeState.labelText()` (`retry_btn` or `got_it_btn`) at rendering |
| Dialog host / component | Live session, pushed steps, FIFO pending openings, Back callback, retry and close callbacks | Runtime only | Retained DialogState; slot and content stack use serializer = null; fresh process has no restored dialog session, pending error or callback |
| Foundation VM | Jobs suspended on a pending `ErrorDecision` (Retry) or an `inform` popup (Close), per-attempt loaders and telemetry | Runtime only | No StateKeeper supplier, snapshot, secret or analytics provenance added |

Preservation source gate: the dialog has no editable inputs, selections, filters,
child results or accepted parent draft. Error presentation and its retry/close
callbacks are runtime session data, explicitly excluded by the canonical policy.
Adding a supplier would not restore the unsaved session and could archive raw
error content. No serializer, shell bridge or persistent owner is needed.
Ordinary recreation retains the existing live owner when InstanceKeeper survives;
actual process death discards the error dialog. Reopening uses the current error.
Restoration emits no synthetic retry/dismissal/input event.

## Verification

Source/diff review covers the callback chain from `safeLaunch` to the host, the
exclusive retry/close paths, pushed and queued errors, event encoding, payload
privacy, reporting isolation, retained VM injection, dependency boundaries and
shared DI registration. No automated test sources, dependencies or infrastructure
were added. Check results, runtime scenarios and admin synchronization state belong
in the task reply and in the admin synchronization section of
[analytics/COVERAGE.md](../COVERAGE.md).
