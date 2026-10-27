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

`BaseViewModel.safeLaunch` passes `ErrorRecovery.Retry` (default) or
`ErrorRecovery.Close` through `ErrorProvider.provide(exception, onRetry, onClose)`
into `DialogConfig.ErrorDisplay(onRetry, onClose)`. The config's `onRetry` is a
relaunch of the failed call, or null for `Close`; the host child factory turns it into
`finish(onRetry)`, so the relaunch runs instead of `onClose` once the error leaves.
The controller delivers openings without conflation. An error opened while an
ordinary sheet is presented is pushed above its active step; otherwise errors open a
root session or queue FIFO before the latest ordinary opening. An active error
remains until the user acts on it. These host changes emit no synthetic presentation,
retry, dismissal or result facts.

| Callback / path | Observation | Meaning and eligibility |
|---|---|---|
| ErrorDisplayViewModel.onRetry | RetryRequested / error_display_retry_requested v1 | Primary button labelled Retry, rendered only when `retryable` (config `onRetry != null`); emitted before the Retry direction. Intent, not a successful retry; it names no operation or error |
| ErrorDisplayViewModel.onDismiss | DismissRequested / error_display_dismiss_requested v1 | Emitted before the unchanged Back direction |
| Got it button | DismissRequested through onDismiss | Rendered only for `ErrorRecovery.Close` popups (TrainingCompleted save, Registration submit, ProfileCompleted creation, ProfileBody apply, Debug generate training) |
| Component BackCallback | DismissRequested through onDismiss when dispatched to this component | No inferred button/back source; host Back can bypass the leaf |
| Host Back, toolbar back arrow or Close, swipe/outside dismissal, programmatic closure | Deliberate omission | May bypass the leaf; shared plumbing does not construct feature events. They run `onClose`, never the retry |
| Open intent, dialog view/close, retry outcome | Deliberate omission | No owning feature callback/presentation contract in this scope; the relaunched operation reports its own attempt through its feature's `trackOperation` |

The payloads have no parameters: no error type/classifier, message, title,
exception, URL, route ID, operation, input length, credential or reporting history
is sent. There is no asynchronous business operation to decorate and no operation
API. Ordinary reporting Exceptions are isolated by the implementation and cannot
block navigation or the retry. Fatal Errors propagate. Repeated leaf callbacks
remain repeated observations; no exactly-once telemetry guard is introduced.

## Preservation inventory

| Owner | Field/group | Preserve / Rebuild / Runtime only | Source / restoration and check |
|---|---|---|---|
| ErrorDisplayComponent / VM | ErrorDisplayState.error, ErrorDisplayState.retryable | Runtime only; retained in live session | Existing error route input and the presence of the config's `onRetry` seed a fresh VM; retainedInstance keeps the current VM while its InstanceKeeper survives |
| ErrorDisplayScreen | Localized description, button label | Rebuild | Existing AppErrorState.description() / UiText.text(); `retry_btn` or `got_it_btn` from `retryable` at rendering |
| Dialog host / component | Live session, pushed error step, pending error FIFO, latest ordinary opening, Back callback, retry and close callbacks | Runtime only | Retained DialogState; slot and content stack use serializer = null; fresh process has no restored dialog session, pending error or callback |
| Foundation VM | Jobs/loaders, relaunch closures and telemetry | Runtime only | No StateKeeper supplier, snapshot, secret or analytics provenance added |

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
