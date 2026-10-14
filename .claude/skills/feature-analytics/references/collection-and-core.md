# Collection, attribution and core contracts

Read for changes to collection policy, analytics core/SDK adapters, operation
attribution or schema compatibility. Apply the [feature skill](../SKILL.md) for
instrumentation and its coverage gate. This reference owns analytics-specific
policy; storage and UI implementation follow their existing skills.

## Ownership and source

Inspect current source rather than copying signatures or catalog counts:

- `toolkit/analytics/api` and `core`: collection, identity, catalog, operation
  decorators, queue/worker and presentation contracts.
- `shared/.../bootstrap/ApplicationBootstrap.kt`: application-lifetime consent
  observation; `shared/.../Koin.kt`: catalog/implementation composition.
- `data-features/local-settings` and `feature-api`: durable consent meaning and
  mutation contract; Profile Settings: current consent UI consumer. Login and
  onboarding are entry paths without consent mutation.
- `androidApp`, `data-services/firebase` and `iosApp`: platform initialization,
  disabled SDK defaults and adapter attachment.

Do not move SDK policy into feature analytics implementations. Core validation
and transport diagnostics must not expose raw payloads, user inputs or secrets.

## Collection and consent

Grippo currently collects optional usage analytics after an explicit saved opt-in.
Missing storage and saved false map to Denied; saved true maps to Granted. A pending
UI read is separate from both and cannot authorize collection. Platform SDK defaults
remain disabled until shared policy is applied. A selected draft or successful
login does not grant permission. Changing this policy requires an explicit product
decision; this reference does not claim jurisdiction/store compliance.

Consent is currently an installation preference in LocalSettings. Preserve both
saved true and saved false across account cleanup with clearUserSettings; do not
add backend fields or account synchronization without an authorized requirement.
UI reads the real source and persists each accepted consent-control change through
the normal error pipeline; there is no separate consent draft. Rebuild the transient
UI value from LocalSettings under
[state preservation](../../state-preservation/SKILL.md#initialization-from-persistent-settings).
Login has no consent control. Consent remains editable in Profile Settings; the
new onboarding is currently a blank-content placeholder with Get started, not a
consent request. Its display flag never grants analytics permission. Consent
controls are deliberately unreported.

Review every actual entry path, including native provider sign-in and registration
navigation. Login continuation does not write consent or require agreement;
untouched absence stays disabled. Settings read/write failures use their existing
business error pipeline and must not be swallowed as reporting failures.

Unknown/Denied events are discarded, never buffered for later consent or replayed.
The bounded pre-sink queue is for already-permitted events only. Granting consent
does not synthesize old interactions or a new screen view. Revocation invalidates
queued work; an SDK call already in flight cannot be retracted. Do not promise
instant remote deletion or zero in-flight sends from a local policy change.

## Asynchronous attribution

An operation may finish after navigation changes the active screen. Current
operation schemas omit active navigation context so their lifecycle events cannot
inherit whichever screen happens to be visible at completion. Keep the complete
operation's typed identity and fresh OperationId; do not infer origin from mutable
global context or add route/saved-state provenance to recover it.

Review schema context rules and actual encoding, not only the caller's screen hook.
User interactions may carry their directly observed screen; operation attribution
requires its own explicit contract if the reporting requirement changes.

## Verification limits

For an affected policy/core change, trace: disabled startup → source decision →
core update → SDK configuration → eligible enqueue/send. Review source-read/write
failure, revocation, identity change, stale presentation close and worker revision
checks where affected. Preserve business results, exceptions and cancellation.
Use permitted checks under CLAUDE.md; do not add test infrastructure by implication.

Distinguish observation, enqueue, adapter acceptance and remote delivery. Source
review and compilation establish none of the runtime delivery guarantees. Record
actual adapter/runtime checks separately, including any unverified scheduling
interval after consent persistence. Sign-in, sends and uploads require their
existing authorization; documentation review alone does not authorize them.
