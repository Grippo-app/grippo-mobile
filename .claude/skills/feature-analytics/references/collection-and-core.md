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
  mutation contract; Profile Settings and Onboarding are consent UI consumers.
  Login remains an entry path without consent mutation.
- `androidApp`, `data-services/firebase` and `iosApp`: platform initialization,
  disabled SDK defaults and adapter attachment.

Current shape (verified against these files; re-inspect before relying on it):

- `LocalSettingsRepositoryImpl` (`data-features/local-settings/.../data/`) stores
  consent under `booleanPreferencesKey("analytics_consent")`;
  `observeAnalyticsConsent()` maps null and false to `AnalyticsConsent.Denied`,
  true to `Granted`; `setDefaultAnalyticsConsent(granted)` writes only when the
  key is absent; `clearUserSettings()` re-applies the saved consent,
  `onboarding_shown` and `language` values after `preferences.clear()`.
- Onboarding lives in `:ui-screen-features:authorization`, package
  `com.grippo.authorization.onboarding` (no separate module).
  `OnboardingViewModel.prepare()` initializes consent to granted only when
  onboarding was never shown, then sets the shown flag, then observes the durable
  value. Its control is the shared `Check` (`components/OnboardingAnalyticsConsent.kt`);
  Profile Settings uses `Toggle`.
- `ApplicationBootstrap` (`shared/.../bootstrap/ApplicationBootstrap.kt`, a class
  constructed by `Koin.init`) collects `observeAnalyticsConsent().distinctUntilChanged()`
  on an application-lifetime scope with `retryWhen` backoff (Denied while a read
  fails) and calls `Analytics.updateCollection`.
- Core `DefaultAnalytics` (`toolkit/analytics/core/.../internal/`): `queueCapacity = 128`;
  under `Unknown`/`Denied` an event is discarded with `CollectionDisabled`; a
  collection or identity change discards the queue (`DiscardedOnPolicyChange`,
  `DiscardedOnIdentityChange`). `AnalyticsModule` sets
  `AnalyticsValidationMode.Throw` in debug, so an invalid event throws there, and
  `Reject` otherwise.

Do not move SDK policy into feature analytics implementations. Core validation
and transport diagnostics must not expose raw payloads, user inputs or secrets.

## Collection and consent

Grippo collects optional usage analytics when the installation preference is saved
as granted. By the explicit product decision of 2026-10-14, first onboarding
initializes absent consent to granted immediately on opening, with its checkbox
on by default. Existing saved true/false decisions are preserved atomically; each
accepted checkbox change is persisted immediately. This is an opt-out onboarding
policy, not a claim that the user explicitly opted in through Get started.
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
Login has no consent control. Consent is editable in Onboarding and Profile
Settings. Onboarding initializes absent consent before marking its shown flag and
reads the durable choice; restored onboarding routes rebuild this value instead
of resetting it or immediately redirecting. Get started/Back do not write consent.
The display flag itself never grants analytics permission. Consent controls and
initialization are deliberately unreported.

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
Use the permitted checks of the [verification policy](../../../../CLAUDE.md#verification-policy);
do not add test infrastructure by implication.

Distinguish observation, enqueue, adapter acceptance and remote delivery. Source
review and compilation establish none of the runtime delivery guarantees. Record
actual adapter/runtime checks separately, including any unverified scheduling
interval after consent persistence. Sign-in, sends and uploads require their
existing authorization; documentation review alone does not authorize them.
