# Confirmation dialog coverage

Record owner: [feature-analytics](../../.claude/skills/feature-analytics/SKILL.md)
and [state-preservation](../../.claude/skills/state-preservation/SKILL.md); records
policy: [CLAUDE.md](../../CLAUDE.md#records). Cross-feature contract and
verification limits: [current analytics contract review](../COVERAGE.md#current-analytics-contract-review-2026-10-14);
admin reconciliation: [Live GA4 setup review](../COVERAGE.md#live-ga4-setup-review--2026-10-14).
Rows describe the current code of `:ui-dialog-features:confirmation` and
`:analytics:confirmation`; replace them when that code changes. Typed dismissal
observations use DismissRequested with `dismiss_requested` wire names; the typed catalog is not yet in a store build and every schema is version 1.

## Surfaces and screen hooks

| Route / rendered surface | Component → ViewModel | Presentation | Coverage |
|---|---|---|---|
| `DialogConfig.Confirmation(title, description?, onResult)` → shared DialogContentComponent child factory → ConfirmationScreen (title, optional description, Confirm and Cancel) | ConfirmationComponent → retained ConfirmationViewModel | Runtime bottom-sheet step; default null screenPresentation, no independent screen, no dedicated dialog presentation contract | Resolves ConfirmationAnalytics inside retainedInstance; registers an Essenty BackCallback to `onBack`; its event listener maps Confirm → `onResult` and Back → the host back |
| DialogComponent / DialogContentComponent | Shared host VMs in `:shared` | Containers; no presentation hook | Slot (`DialogComponent.v3`) and step stack (`DialogContentComponent.v3`) with `serializer = null` |

The config defines no `onDismiss` and no swipe override; duplicate opens are
deduplicated by title and description. All three production callers open the same
child factory and retained VM and own the confirmed business action:

- ProfileSettingsViewModel.onDeleteAccount: emits DeleteOpened, opens
  unconditionally; `onResult` emits DeleteConfirmed and runs the DeleteAccount
  operation ([profile](../profile/COVERAGE.md)).
- TrainingRecordingViewModel.onSave: opens the unfinished-sets confirmation only
  when a pending iteration exists (UnfinishedOpened); `onResult` emits
  UnfinishedConfirmed and continues to the completion dialog
  ([training](../training/COVERAGE.md)).
- TrainingRecordingViewModel.onBack: with a nonempty draft opens the discard
  confirmation (DiscardOpened); `onResult` emits DiscardConfirmed and deletes the
  draft before leaving; an empty list exits without a dialog.

The caller's `onResult` runs in the host's release step, after the sheet is hidden.

## Callbacks and operations

| Callback / observation | Version-1 event | Meaning / exclusions |
|---|---|---|
| `onConfirm` (Confirm button) | ConfirmationEvent.ConfirmClicked / `confirmation_confirm` | Confirm intent before the existing Confirm direction; no claim of downstream operation success |
| `onBack` (Cancel button; the Component's Essenty BackCallback) | ConfirmationEvent.DismissRequested / `confirmation_dismiss_requested` | Request to leave; the source is not inferred |
| Swipe/outside dismissal, toolbar close, toolbar back arrow, the host's Compose BackHandler and host Essenty callbacks | Deliberate omission | These routes reach the host's `onDismiss`/`onRelease` without calling this VM; whether system Back reaches the child BackCallback or the host handler is not established from source |
| Dialog viewed / closed | Deliberate omission | Callback observations only; no separate dialog presentation contract or screen_view |

Both events have no parameters. Description, title, callbacks, action context and
entity IDs never enter payloads. Ordinary reporting exceptions cannot block either
direction (the implementation catches Exception); fatal Errors retain their
semantics. There are no inputs, validation/busy guards, loaders or asynchronous
business operations in this VM, so no operation model or decorator exists.
Downstream operations remain owned by the callers, where they are decorated.

## Catalog

`ConfirmationAnalyticsModule` (`@Module @ComponentScan`) binds
`ConfirmationAnalyticsImpl` as `ConfirmationAnalytics` and registers
`confirmation_confirm` and `confirmation_dismiss_requested` (both v1, empty
parameters) through one qualified `confirmation_analytics` schema provider;
`shared/.../Koin.kt` lists it once, and the module depends only on
`analytics-api`. The API exposes `trackEvent` only.

## Preservation inventory

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| ConfirmationViewModel / ConfirmationState | `description: UiText?` (read-only; State is `@Immutable`, not serializable) | Runtime only | Constructor input from the config; retainedInstance | The retained VM survives ordinary recreation; a fresh dialog is rebuilt from the caller config; no DRAFT or StateKeeper supplier |
| Shared dialog host | Session, step stack, title, `onResult`/back closures, pending result, phase | Runtime only | `serializer = null`; entries hold lambdas | Process death discards the session; `onResult` is never replayed; a swiped confirmation returns nothing to the caller |
| ProfileSettings owner | Deletion outcome; `analyticsConsent` | Runtime only / Rebuild | Durable LocalSettings observation; no DRAFT | Confirmation feeds no draft; deletion is a one-shot guarded only by its loader after confirm |
| TrainingRecording owner | `stage`, `exercises`, `startAt` the discard/unfinished confirmations act on | Preserve (existing owner) | `TrainingRecordingDraft` / `TrainingRecordingState.serializer()` | Confirmation adds no field; discard deletes the durable draft then pops; unfinished-confirm proceeds to the completion dialog without a state change |
| Confirmation VM / foundation | Directions, loaders, telemetry provenance | Runtime only | Conflated navigator | No persistence keys, flags, shadow caches, jobs, secrets or restoration-generated events; the VM has no `var` |

The module owns no editable input, selection, filter or parent draft, so its
preservation gate is this assessment: nothing is serialized and the intentionally
unsaved dialog host is unchanged. Opening a new confirmation constructs its state
from the caller; rotation retains the existing VM; supported application archive
restoration does not restore a live dialog.

## Verification

Source gates for the current revision were reconciled against the config, child
factory, all three callers, retained injection, callback order, parameter-free
schemas, exception isolation, DI registration and host serializer boundaries
(read-only audit on 2026-10-15; no build was part of it). Checks recorded for the
implementing revision (2026-10-13): source/diff review found no orphan event
models, missing callers, duplicate schema/service registration, reporting-only
routing fields or accidental draft serialization; generated Koin code binds the
service and its uniquely named schema.

- `git diff --check` passed.
- Isolated `:ui-dialog-features:confirmation:compileAndroidMain` and
  `:analytics:confirmation:compileAndroidMain` with `--no-configuration-cache`
  passed together (BUILD SUCCESSFUL, 9s, 267 tasks).
- The combined UI / analytics / shared Android attempt compiled Confirmation
  Android and common metadata, then failed outside this scope in
  `:ui-dialog-features:iteration-picker:transformNativeMainDependenciesMetadata`
  on a missing transformed material-icons-core klib; shared compilation stays
  unverified by this feature's builds. Earlier attempts failed on concurrently
  incomplete module registration, unresolved Compose references or a stalled
  configuration-cache save and do not supersede the isolated successful build.
- Independent review was not performed for that revision (no permitted
  independent reviewer was available); self-review and source gates are not a
  substitute. No automated test infrastructure was added.

Unrun runtime checks: which system-Back route fires while the sheet is presented
(the child BackCallback or the host handler); Android recreation with the dialog
open; real process death or iOS checkpoint restart closing the session without
invoking `onResult`; Confirm, Cancel and Back with a reporting failure; generated
Koin bindings at runtime; consent-gated delivery. These need application
execution and are not established by source review or compilation.
