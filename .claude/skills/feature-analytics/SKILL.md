---
name: feature-analytics
description: Implement and review typed Grippo KMP feature analytics when screens, navigation, entry points, operations or outcomes change. Apply the feature API, independent implementation and UI provider standard, and keep the coverage matrix current even when tracking is not explicitly requested.
---

# Feature analytics

## Scope and reading

Apply alongside feature behavior changes; visual-only changes need an impact
assessment, not new events. Stay within the requested feature and verification scope.
Read `CLAUDE.md`, `tasks/analytics/feature-standard.md` and the affected coverage matrix.
For providers, retention, lifecycle, classifiers or DI, also read
[UI integration](references/ui-provider.md). Current reference: `analytics/analytics-api`,
`analytics/authorization` and `ui-screen-features/authorization/.../analytics`.
Copy the architecture, not authorization-specific facts or omissions.

## Business isolation

Business logic uses business contracts. Analytics decorates existing operations
and observes facts; it never decides navigation, fallback, retries or requests.
Removing reporting must leave the business block and its result unchanged.

- Do not put business result models in analytics-api or branch business code on
  telemetry outcomes, classification, snapshots or delivery status. A decorator
  may return caller `T` unchanged without importing or interpreting its type.
- Keep use cases complete. Login/register include private preparation; do not
  split public methods, introduce ports/stages or change orchestration for timing.
  Omit a detail that requires a new business contract solely for analytics.
- Routes own their identity types. If a route carries scenario identity, convert
  that value to toolkit `FlowId` only at the feature provider boundary; preserve
  validation and serialized shape. Never repurpose an entity/draft ID as a flow ID.
- Ordinary reporting exceptions at start/terminal, transitions and presentation
  must not prevent business work/navigation/lifecycle or replace successful results.
  Isolate them at reporting boundaries, without per-VM wrappers, retries or fake
  fallback facts. Preserve original business exceptions and caller cancellation;
  secondary reporting errors may be suppressed. Fatal `Error` remains fatal.
  State/owner invariant checks and direct schema validation remain explicit.
- Preserve the [shared UI error pipeline](../ui-feature/references/error-pipeline.md):
  ViewModels unwrap results with `getOrThrow()` inside `safeLaunch`. Unhandled
  errors retain that pipeline; pure `mapError` changes only dialog presentation,
  and `onError` runs after dismissal. Do not catch failures locally in a ViewModel
  or its helpers, or add launches/wrappers/stage flags for recovery. Decorated
  operations propagate original business errors to `safeLaunch`; cancellation
  remains cancellation. Keep recovery out of analytics and the mapper. Both
  `safeLaunch` overloads retain the same hooks, defaults and shared error pipeline.

## Layers and API

- One top-level `<Feature>Analytics` interface per feature in
  `:analytics:analytics-api`, with nested scenario interfaces and classifier ports
  only when needed. No separate public Factory, shared scenario mode or casts.
  Scenario contracts expose only their valid operations.
- Consumer operation/transition descriptions, enums and immutable snapshots live
  in sibling `models/`; one standalone type per file. Related sealed variants stay
  with their family. Encoding-only types remain internal. Reuse toolkit context,
  presentation and ID types where needed, without wrappers solely to rename them.
- Implement in `:analytics:<feature>`, with Kotlin `internal` types and one public
  annotated module for composition. Organize existing responsibilities under
  `internal/scenario`, `state`, `models`, `events`, `operation`, `schema`.
  No empty scaffolding or separate file for every event variant.
- UI main depends on analytics-api only. Feature implementations depend on that API,
  toolkit consumer API and necessary KMP libraries, never UI/foundation/Essenty/routes,
  domain/use cases, services, SDKs, toolkit core or bootstrap. Keep implementation
  tests independent too; UI test-only implementation dependencies are allowed for
  real integration checks.
- Follow convention plugins, explicit API and type-safe Gradle accessors. Public
  signatures determine `api` dependencies. Add serialization/test plugins only when
  required. Use generated `.module`, never manual production DI DSL.

## Operations, transitions and schemas

- The stateless entry-point implementation is `@Single`; creation returns an
  independent mutable scenario, never a DI-singleton scenario shared across owners.
- Use closed typed descriptions with `trackOperation(operation, block)`. Preserve
  generic `T` when events do not inspect results; use precise result contracts only
  when existing business results determine the outcome. No casts or nullable bags.
  Authorization attempts and SessionCheck preserve the caller result unchanged.
- Keep the full existing sequence inside its `safeLaunch`/use-case coroutine.
  Analytics receives no feature/use-case/SDK providers, fetches no data, and adds
  no scope, job, dispatcher or retry policy.
  Follow the [Kotlin block style](../../../CLAUDE.md#kotlin): keep the business
  block expanded across lines even when it contains a single business call.
  Short single-expression `update`/simple Flow transformations remain compact.
  No named local functions inside `init`, constructors, functions or lambdas;
  ordinary callbacks/DSLs are allowed. Startup requests normally start in `init`;
  a complete operation reused after dialog dismissal belongs in a private
  class-level method called from `init` and `onError`, using the existing VM scope.
  Do not duplicate its sequence or introduce helpers solely for analytics.
- Follow the [project coroutine rules](../validation-gates/references/forbidden-patterns.md#coroutines):
  do not add manual `ensureActive()` checks or wrappers in production, including
  analytics decorators. Preserve caller cancellation through the execution design.
- Use toolkit `trackOperation` and `AnalyticsOperationEvents<T>` for timing and IDs.
  Every attempt defines one start/terminal pair; retries receive fresh IDs.
  Reporting errors, consent and queue policy can prevent delivery of either fact. A returned
  failure must not become success: unwrap `Result` in the business block or classify
  the existing result exhaustively. A completed block with handled fallback means
  execution completed, not that the desired business outcome occurred.
- Preserve concrete SDK/user cancellation separately from coroutine interruption.
  Never classify by exception text. Do not add stage exceptions or separate
  credentials/server callbacks for telemetry; only report observed facts.
- Report navigation facts at their real owner through
  `trackTransition(FeatureTransition)`. Use an enum for payload-free variants or a
  sealed family with required payloads; implement exhaustive `when` without aliases
  for specialized methods. Keep operations, transitions and presentations separate.
  A duplicate transition must not invent a step change or overwrite attribution;
  terminal exits are deduplicated and transition facts cannot reopen a closed flow.
- Centralize schemas, event names, parameter keys and bounded wire codes in typed
  matrices. Use the same schema/key instances, explicit versions and stable codes.
  Register the complete custom catalog once via qualified annotated `@Single` in
  `<Feature>AnalyticsModule`, wired by shared before core resolution. Reuse built-in
  screen/dialog schemas. Semantic changes require a report/migration decision;
  incrementing version alone does not make them compatible.

## UI integration

- Place the internal stateless `<Feature>AnalyticsProvider`, a separately filed
  classifier when needed, and existing annotated bindings in the UI `analytics/`
  package. Provider methods are `provide<Scenario>`; authorization reference is
  `AuthAnalyticsProvider.provideAuthorization/provideSession`.
- Provider constructor receives the feature API; methods receive `ComponentContext`.
  Store no contexts or scenarios in the singleton. Hide route mapping in private
  named methods and persistent keys in its private companion; preserve key values.
- Persisted scenarios use local foundation
  `RetainedAnalytics(context).provide<Scenario, Snapshot>` with explicit scenario
  interface type, serializer, create/save and necessary identity validation.
  The helper owns consume → type-key retention → validate → register. Mapping,
  keys and snapshots stay feature-owned. Do not add custom holders, module-wide
  retention extensions, base analytics components or another provider hierarchy.
  No-persistence scenarios do not need invented snapshots/keys.
- Obtain the scenario once before its retained ViewModel. Pass the same instance
  to children/VM as `analytics`, without a feature prefix. Recreation, operations,
  navigation, leaf presentation and later saving must use that retained instance;
  a stored snapshot creates a scenario only when no retained instance exists.
- SDK/domain exceptions stay in the UI classifier. Supply the nested API port
  through annotated `@Single`; use an internal object when stateless. Map concrete
  types to bounded categories, with explicit Unexpected for unknown errors.
- Bind leaf visibility once through DI:

  ```kotlin
  getKoin().get<ScreenAnalytics>().bind(lifecycle) {
      analytics.presentScreen(step)
  }
  ```

  Omit step for a single-surface scenario. Foundation's stateless internal
  `ScreenAnalyticsImpl` is discovered by `CoreModule`; per-binding callbacks own
  presentations. No screen state in the singleton, legacy extensions, render/VM
  subscriptions or duplicate parent bindings.

## State, attribution and privacy

- Preserve snapshot wire shape, StateKeeper keys and required legacy `SerialName`.
  Validate route identity before reporting/rebinding; never silently replace a
  mismatched scenario. Type retention supports one scenario of a type per owner;
  use distinct owners/keys if actual requirements need more.
- Start at the observed entry point. New scenarios get new IDs; foreground resume
  does not start a flow. EntryPoint, launch source, origin, referrer and navigation
  reason are distinct facts. Preserve immutable identity/origin; unknown facts stay
  absent. Use observed Initial/Action/Back/Restore/External reasons. Restore denotes
  reactivation of an existing surface (including foreground) or restored state;
  it does not mean a new entry/flow.
- Each RESUMED activation is a view; pause/destroy closes it. Destroy alone does
  not mean user exit. Declare dialog/overlay visibility policy separately.
- Respect consent/identity ownership. Unknown/Denied events are not queued for
  replay; feature work cannot grant consent or reset identity.
- Never encode credentials, tokens/codes, input, exception messages, raw URLs or
  request/response bodies. Use bounded facts, validated units/IDs and documented
  cardinality. Track important outcomes/screens/transitions; per-field validation,
  reference loads, internal stages and generic clicks need a concrete reporting
  question. Avoid more instrumentation machinery than the business warrants.

## Coverage and verification

Maintain `tasks/analytics/<feature>-coverage.md` from the coverage template: actual
trigger, single owner, typed values, attribution, lifetime, outcomes and verifying
tests. Explain omissions. Inspect callbacks, Directions, use cases and restore
paths; schemas alone cannot prove integration. Remove obsolete calls, models,
schemas, helpers, bindings, fixtures and instructions together.

Self-review boundaries, naming/files, matrix identity, DI, retention, cancellation,
error isolation and overhead. Fix findings and review the final result again.
Static scans do not resolve Kotlin types or implicit operator/serializer imports;
check real contracts and examples. Historical reports remain historical.

Verify payload/outcome fixtures, original errors/cancellation, reporting-failure
injection, retries, independent scenarios, route mismatch, recreation followed by
mutation/save, leaf foreground/back/restore and generated DI. Test actual helpers
and owners, not test-only copies. Apply snapshot/save/mismatch checks only where
persistence or route-flow identity exists. Without persistence, check direct
retention across recreation; a fresh process creates a new scenario. Run affected
Android host/iOS simulator tests and platform integration checks when permitted;
broaden checks for changed app wiring.
Honor no-build/test instructions and state compilation/KSP/runtime limits explicitly.
Validate changed skill YAML, links and examples. Record actual command results;
never infer passing runtime/builds from static or historical checks.

Firebase sends/sign-in and mapping/symbol uploads require explicit authorization;
build/test authorization alone does not allow them.
