# Feature contracts, collection and core

Read the matching section for feature API/models, encoding, schemas, analytics
DI/catalog, collection policy, core/SDK adapters or asynchronous attribution.
Apply the [feature skill](../SKILL.md) for observation invariants and completion
gates; use [ViewModel integration](viewmodel.md) for caller wiring.

## Feature API and schema contracts

- Keep one stateless `<Feature>Analytics` interface per feature in
  `:analytics:analytics-api`; inspect
  `analytics/analytics-api/src/commonMain/kotlin/com/grippo/analytics/<feature>/`.
  Its maximal shape is `trackEvent(FeatureEvent)`,
  `suspend fun <T> trackOperation(FeatureOperation, block: suspend () -> T): T`
  and `presentScreen(FeatureScreen, reason): ScreenPresentation`. Inspect the
  actual interface for its implemented subset and defaults. `DebugAnalytics`
  is screen-only. Add event/operation methods with real instrumentation, not empty
  models or placeholder methods; events perform no business actions.
- Keep closed event/operation families, screen enums and necessary feature enums
  in sibling `models/`, one standalone type per file and related sealed variants
  together. Constructors accept direct typed facts, not nested action wrappers.
  Enforce valid combinations in types: credential fields belong to credential
  events, picker events accept picker enums, and fixed Name needs no selector.
- Reuse `InputStatus`, `InputKeyboardAction` and `ControlAction` where their
  meanings fit. Actual feature fields, methods, pickers and experience values
  remain feature-owned. Add no generic capability hierarchy, scenario factory,
  snapshot or compatibility alias.
- Public events are observations, not toolkit wire events. Public fact enums are
  plain enums, without `AnalyticsCode` or wire-string properties. Internal
  implementation owns typed schema/key instances, wire keys/codes and exhaustive
  enum encoders. Field/action keys accept closed types, not open `AnalyticsCode`.
  Add no string event API, nullable parameter bag or helper/method/file per button.

### Implementation and catalog composition

UI depends on analytics-api. `:analytics:<feature>` imports no UI, foundation,
routes, domain, use-case, service, SDK or toolkit core code. Inspect
`analytics/home/.../HomeAnalyticsModule.kt` and `internal/HomeAnalyticsImpl.kt`:
one public `@Module @ComponentScan` class and one
`@Single(binds = [<Feature>Analytics::class]) internal` implementation.

For custom events/operations, provide the complete custom catalog once through a
uniquely qualified `@Single @Named("<feature>_analytics")` schema provider. Debug
has no custom schema. Reuse built-in `ScreenViewed` for screens; dialogs have none.
Inspect `toolkit/analytics/core/.../AnalyticsModule.kt`: the catalog uses
`scope.getAll<AnalyticsSchema>()`. `shared/.../Koin.kt` loads every feature module
in one `modules(...)` call before resolution; an omitted module silently drops
its schemas. Keep the explicit list complete. Use existing convention plugins,
explicit API and generated `.module`/`@Single` DI; no feature integration module,
classifier or provider is needed for VM wiring.

### Schemas and compatibility

Centralize typed schemas/keys, explicit versions and bounded wire codes. A semantic
change or drop needs a documented reporting decision; a version bump alone does
not make reports compatible. A validation change also assesses status meaning and
event population, even when the wire schema is unchanged. Do not silently restore
removed funnel events/classifiers or other unobserved mechanisms.

New schemas start at v1. The current unreleased catalog remains v1 by the user's
explicit decision; implementation corrections do not each increment it. Once a
schema has produced released data, assess consumers and compatibility before
changing meaning. Do not reset released versions to apply the unreleased rule.

Operation decoration supplies a fresh OperationId, monotonic duration and
started/completed/failed/interrupted factories. Preserve the caller's result,
errors and cancellation under the [business invariants](../SKILL.md#business-and-event-invariants).

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
  `OnboardingViewModel` observes the durable consent from `init`; in parallel, its
  preparation load initializes consent to granted only when onboarding was never
  shown, then sets the shown flag. On a first onboarding the observation may emit
  Denied before the default write lands, then Granted. Its control is the shared
  `Toggle` (`components/OnboardingAnalyticsConsent.kt`);
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
initializes absent consent to granted immediately on opening, with its consent control
on by default. Existing saved true/false decisions are preserved atomically; each
accepted consent change is persisted immediately. This is an opt-out onboarding
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
observes the durable choice; restored onboarding routes rebuild this value instead
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
