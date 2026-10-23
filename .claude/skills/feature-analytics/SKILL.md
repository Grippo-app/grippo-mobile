---
name: feature-analytics
description: Implement and review typed Grippo KMP feature analytics and reconcile its live analytics admin configuration with code and released schemas. Use for instrumentation, analytics reporting/admin tasks and feature behavior changes; preserve observation, privacy and coverage gates.
---

# Feature analytics

## Scope

Apply to every behavior change as defined in
[CLAUDE.md](../../../CLAUDE.md#definitions), even without a tracking request, and
to every analytics task: instrumentation, setup, reports and audits. A visual-only
change (same definitions) needs an impact assessment instead of new events: one
sentence naming the analytics-facing symbols checked (the list and a worked
example are owned by [live admin synchronization](references/admin-sync.md#trigger-and-access-gate-must)).
For an existing feature, establish the affected scope from behavior impact and
actual callers. Start with changed screens/routes, then trace changed shared
inputs, formatters, components and domain operations to the consuming screens,
containers and callbacks whose data, eligibility or observed facts change. Include
those consumers even when their seven MVI files and router entries are absent
from the diff. Bound the inventory with caller evidence; sharing a dependency
alone does not make every screen affected.
Read the current analytics contracts and the affected feature's
`analytics/<feature>/COVERAGE.md` ([records](../../../CLAUDE.md#records)). For UI
wiring read [ViewModel integration](references/viewmodel.md). Stay within the
affected scope established above and the authorized verification scope.

Analytics tasks and instrumentation changes also pass
[live admin synchronization](references/admin-sync.md). That reference owns its
trigger, exceptions, access rules and saved-configuration checks. Source review
alone does not pass it.

For collection consent, core/SDK transport, asynchronous attribution or schema
compatibility changes, read [collection and core contracts](references/collection-and-core.md).
Ordinary feature instrumentation does not require changing those owners.

For a behavior change, inventory the affected leaf routes (screens) and graph
routes (container flows), callbacks, inputs and complete operations in the
feature's coverage file. Reuse or extend the feature's single API/implementation,
and wire only the affected VMs. Before completion, reconcile the file's rows with
actual callers and record deliberate event omissions there. For a new feature,
inventory all its screens. For an existing feature, inventory all screens in the
affected scope. Apply the mandatory screen coverage gate below. The current skill,
standard and source define the contract; historical review reports do not.

## Small public API

- One stateless `<Feature>Analytics` interface per feature in `:analytics:analytics-api`
  (inspect `analytics/analytics-api/src/commonMain/kotlin/com/grippo/analytics/<feature>/`).
  Its maximal shape is three methods: `trackEvent(FeatureEvent)`,
  `suspend fun <T> trackOperation(FeatureOperation, block: suspend () -> T): T` and
  `presentScreen(FeatureScreen, reason): ScreenPresentation`. Each interface
  exposes only the methods its feature implements; inspect its declaration for
  the exact subset and defaults. `DebugAnalytics` is a screen-only example.
  Events do not perform business actions. Screen-only
  integration uses `presentScreen` and a screen enum. Add event/operation methods
  together with their actual instrumentation, without empty models or placeholder
  methods.
- Put closed event/operation families, screen enums and necessary feature enums
  in sibling `models/`. One standalone type per file. Related sealed variants stay
  together. Constructors receive direct typed facts, not nested action wrappers.
- Reuse the common `InputStatus`/`InputKeyboardAction`/`ControlAction` enums when
  suitable. Keep actual feature fields, methods, pickers and experience values
  feature-owned. Do not add a generic capability hierarchy (a tree of base
  analytics interfaces that feature services extend or are checked against),
  scenario factories, snapshots or compatibility aliases.
- Public feature events describe observations (facts the UI or an operation
  saw); they are not toolkit wire events. Internal implementation owns encoding,
  schemas, keys and stable wire codes. Public fact enums are plain enums: no
  `AnalyticsCode` or wire-string properties. Use typed keys with internal
  exhaustive enum encoders. Field/action keys accept closed types, not the open
  `AnalyticsCode` interface. No string event API, nullable parameter bag or
  helper/method/file per button.
- Keep structurally invalid combinations out of constructor types. Credential
  fields belong to credential events. Picker events accept only picker enums.
  Fixed fields such as Name need no public field selector.

## Observation boundary (MUST)

The observation boundary is the existing callback, operation or foundation hook
where a fact becomes known. Before adding an event or parameter, identify that
owner. Observe the fact there and pass only the bounded typed fact to the
analytics API.

Widget boundary: a design-system component MAY expose neutral optional callbacks
for focus, clear, IME action or visibility. Such a callback carries ordinary UI
values and imports no analytics types. It keeps the default keyboard/focus
behavior and reports to the owning ViewModel, which decides what to observe.

- Business and navigation contracts MUST remain independently necessary when the
  analytics calls are removed. MUST NOT add route, Direction or config fields,
  business types, constructor arguments or saved state solely to carry reporting
  context between owners. A genuine business change follows its owning skill and
  authorized scope.
- Observation types belong to the feature's analytics-api models. UI fact mapping
  stays in the feature's existing `analytics/` package; for example,
  `ui-screen-features/authorization/.../analytics/AuthInputMapping.kt` maps the
  existing `EmailFormatState` to `InputStatus`. MUST NOT place telemetry-only
  models in navigation, domain or shared UI-state modules to make them easier to
  transport.
- Use the existing VM injection and the foundation presentation boundary (the
  `screenPresentation` hook that `BaseComponent` binds). Neutral UI callbacks
  follow the widget boundary above. They carry no telemetry types and no
  cross-screen reporting history.
- Before completion, trace each added type, field and argument to an actual
  analytics API consumer or to an independent business/UI requirement. Reject
  unused models and reporting-only changes outside this boundary.
- Some facts need reporting context or history carried between business or
  navigation owners. Such a fact stays an explicit coverage omission until its
  reporting requirement is resolved. Do not infer it. Do not build a context
  propagation mechanism merely to complete a report.

## Business stays business

- Analytics decorates complete existing operations. The decorator preserves the
  caller's `T`, the original business exceptions and cancellation. Business alone
  decides requests, validation, retries, fallback, navigation and results. Never
  branch on telemetry.
- Invoke the complete throwing command inside the tracked business block so its
  original failure reaches the decorator. Verified example from
  `ui-screen-features/trainings/.../TrainingsViewModel.kt`:

  ```kotlin
  safeLaunch {
      analytics.trackOperation(operation = TrainingsOperation.DeleteTraining) {
          deleteTrainingUseCase.execute(id)
      }
  }
  ```

  All required work belongs inside that block. Never reshape a use case to fit
  telemetry. Direct throwing credential adapter calls stay inside the decorated
  credential operation.
- Keep whole use cases. Do not expose preparation steps or stages for timing. Do
  not add a coroutine, job, dispatcher or retry solely for analytics. Submit intent
  (the user asked) and operation start (the business block began) are different
  facts. Emit after the existing eligibility/busy guards. A rejected callback emits
  neither an accepted-intent event nor an operation event. Verify both the accepted
  and the rejected path without changing business guards.
- An ordinary reporting exception cannot prevent business or lifecycle work, and
  it cannot replace a success or an error. Keep the existing isolation boundary
  (the place where reporting failures are already caught); a fatal `Error` stays
  fatal. Foundation `ScreenAnalytics` isolates screen present/close `Exception`s.
  Feature `presentScreen` passes the typed navigation reason and the original
  presentation through, without duplicate catches, no-op fallbacks or wrappers.
- ViewModels call commands directly inside the existing `safeLaunch`.
  Follow the [error pipeline](../ui-feature/references/error-pipeline.md): no local
  recovery catch or wrapper. A pure `mapError` changes dialog presentation only,
  and `onError` runs after dismissal. Both `safeLaunch` overloads keep the same
  hooks.
- Do not add cancellation checks solely for analytics. Business cancellation follows
  the [canonical coroutine rules](../validation-gates/references/forbidden-patterns.md#coroutines).
  Keep business lambdas multiline; keep short `update` calls and simple Flow
  operators compact, per [Kotlin blocks](../validation-gates/references/code-style.md#kotlin-blocks).
  Named local functions are forbidden. Startup and retry use the existing VM scope
  and class-level private methods.

## UI wiring and events

- UI main depends on analytics-api. The implementation lives in `:analytics:<feature>`
  and imports no UI, foundation, routes, domain, use-case, service, SDK or toolkit
  core. Current module shape (inspect `analytics/home/.../HomeAnalyticsModule.kt`
  and `internal/HomeAnalyticsImpl.kt`): one public `@Module @ComponentScan` class
  and one `@Single(binds = [<Feature>Analytics::class]) internal` implementation.
  When the feature defines custom events/operations, it also has
  `@Single @Named("<feature>_analytics") internal fun provideSchema(): AnalyticsSchema`
  (Debug is screen-only and has none). `toolkit/analytics/core/.../AnalyticsModule.kt`
  builds the catalog with `scope.getAll<AnalyticsSchema>()`. `Koin.init` loads
  every module in one `modules(...)` call before it resolves anything. A feature
  module missing from `shared/.../Koin.kt` therefore silently drops its schemas:
  list every feature module there. Use the existing convention plugins, explicit
  API and generated `.module`/`@Single` DI.
- Inject the stateless feature service as `analytics` into the ViewModel through
  the Component's normal `retainedInstance` block, just like other dependencies.
  Do not pass it through Component constructors, routes or factories. Do not emit
  outside the ViewModel.
- Every independent screen MUST override `BaseViewModel.screenPresentation` with a
  `(NavigationReason) -> ScreenPresentation` lambda that calls the typed
  `presentScreen`. `BaseComponent` binds it during `doOnCreate`, before activation
  and navigation bridging. Foundation alone resolves `ScreenAnalytics` and supplies
  the lifecycle and reason.
- Graph/container VMs (hosts that only render child screens or shared chrome) MUST
  keep the default null hook: no container `screen_view`, screen binding or
  lifecycle service resolution. The VM stores no `Lifecycle` or `ComponentContext`.
  No activation-flow observer, extra job, feature provider/route matrix or manual
  bind is needed. The nullable base API supports containers; screen coverage is
  not optional.
- Keep the normal Component `retainedInstance`/ViewModel lifecycle ownership.
  Analytics adds no StateKeeper/InstanceKeeper keys, holders, snapshots or scenario
  retention (keeping a flow or funnel context alive across screens so that later
  events can refer to it). Navigation IDs may remain for navigation; analytics does
  not consume them.
- `ScreenAnalytics` owns the per-binding flag: the first activation reports
  `NavigationReason.Initial`, later activations report `Restore`. Pause or destroy
  closes the presentation. A fresh binding has no saved provenance (no record of
  how the screen came to be shown). Do not invent referrer, flow continuity, entry
  attribution or Action/Back reasons.
- Emit direct typed events in the existing Contract→ViewModel callbacks. Use the
  existing validation status and map it in the UI `analytics/` package; never add
  analytics validators. A text status emits only when the existing status changes;
  the initial Empty→Empty transition is omitted. Do not add a status cache. Picker
  confirmations, clicks and selections remain distinct facts. Programmatic
  navigation is not a fake user click.
- Dialog/picker open and accepted-result facts belong to the owning VM's existing
  callbacks, including the `onResult` closure it passes to a config. Shared
  host/config plumbing and Components do not construct events. Dialogs have no
  automatic view/close tracking and no built-in dialog view schema. Record that
  omission unless the requested scope defines a separate typed presentation
  contract and its owner.
- Keep only observed facts. Do not add transition/flow events, error classifiers,
  provider-cancel outcomes or extra orchestration for a more detailed funnel.
- Cancelling an in-surface edit (Cancel next to a field) is a `Clicked` fact on
  that control. `DismissRequested` is reserved for leaving the surface.
- Use `DismissRequested` for an observed user request to leave or close a surface.
  It is declared per feature in its `models/` and encoded as
  `<feature>_dismiss_requested`. It records intent, not completed dismissal or
  cancellation. Keep navigation Back/onBack names where they describe navigation.
  Do not rename business contracts merely to match telemetry. Programmatic pop,
  lifecycle close and successful completion are not user dismissal requests.
  A telemetry rename reconciles public events, internal action types/matrices,
  wire encoders, VM callers and current coverage together. Retain no compatibility
  alias without a released-consumer requirement.

## Mandatory screen coverage gate

Before completing a new feature or a change affecting existing screens/navigation:

1. Establish the affected scope using the caller tracing in [Scope](#scope).
   Enumerate its actual routes and rendered surfaces, including nested/tab screens
   and alternative entry paths. Trace each to its Component and retained ViewModel.
   Do not use only the FeatureScreen enum or coverage matrix as the inventory.
2. Classify each surface. An independent screen needs exactly one correctly typed
   `screenPresentation` in its owning VM; this includes Splash and standalone
   loading pages without controls. A host that only renders child screens or
   shared chrome is a container and keeps the default null. Classify by actual UI,
   not by filename, base class or the presence of a router. Dialogs and overlays
   are outside this screen requirement; they follow a dialog presentation contract
   only when a task defines one (none exists today, see UI wiring).
3. Reconcile route → surface → Component → VM → FeatureScreen → hook in the
   feature's `analytics/<feature>/COVERAGE.md`. Record container rows with their
   null-hook reason. Verify normal retained-VM injection and the shared foundation
   binding. Add no feature lifecycle wiring.
4. Reject missing hooks, wrong screen identities, screen enum entries with no
   owner and container screen views emitted alongside their child screens.
   Fix them within the task scope before marking the feature complete. An
   omissions row cannot waive a required screen hook. Report permitted checks
   and explicit unrun runtime checks in the reply.

See [ViewModel integration](references/viewmodel.md#screen-classification).
The matrix lives in `analytics/<feature>/COVERAGE.md`
([records](../../../CLAUDE.md#records)). One row per route/surface lists its
Component, VM, typed screen/hook or container reason, callbacks/inputs/operations
and omissions. The matrix sits beside the feature's preservation inventory.
Replace rows to match the current code; never append dated journal entries. When
the changed feature has no file yet, create it and list it in the index of
`analytics/COVERAGE.md`. Check results and dated narratives belong in the reply
(`docs/records/` only when the user asks for a record).

## Schemas, privacy and checks

- Centralize typed schema/key instances, explicit versions and bounded wire codes.
  Register the complete custom catalog once through a uniquely qualified `@Single`.
  Reuse the built-in `ScreenViewed` schema for screens; dialogs have none. A
  semantic change or drop needs a documented reporting
  decision; a version bump alone does not make reports compatible. A validation
  change also requires assessing status meaning and event population, even when
  the wire schema is unchanged. Do not silently add dropped funnel mechanisms
  (funnel events or classifiers that were removed; see the list under UI wiring).
- New schemas start at v1. The current unreleased catalog remains v1 by the user's
  explicit decision. Do not increment versions for each implementation correction.
  Once a schema has produced released data, assess consumers and compatibility
  before changing its meaning. Do not reset released versions to apply this rule.
- Operation decoration supplies a fresh OperationId, a monotonic duration and the
  started/completed/failed/interrupted factories. Preserve the original results and
  errors. `completed` means the caller block completed, not an inferred business
  success.
- Never encode raw text/length, credentials/tokens, entity IDs/body measurements,
  exception messages, URLs or request/response bodies. Use enums/status/counts.
  Consent/identity/SDK transport stay owned by core/application policy.
- Keep `analytics/<feature>/COVERAGE.md` as the feature's current matrix: actual
  owners, typed hooks or container reasons, callbacks/inputs/operations and
  explicit omissions. Replace rows to match the final code. Do not append a new
  section beside stale rows, and do not leave contradictory current-coverage
  claims. Cross-feature contract decisions and admin synchronization evidence go
  to `analytics/COVERAGE.md`. One feature does not prove global coverage or
  Firebase delivery.
- Self-review naming, boundaries, DI, the event matrix, privacy, error isolation
  and overhead. Fix findings and review again. Remove obsolete API, callers,
  helpers and docs.
- Verify actual payloads, results/errors/cancellation, reporting-failure injection,
  leaf VM screen coverage with the foundation lifecycle, and eligible callbacks.
  Check real code, not copied helpers. Run the permitted checks and builds under
  the [verification policy](../../../CLAUDE.md#verification-policy). Report actual
  results and skipped checks in the reply. Do not introduce automated test
  infrastructure without an explicit user request.
- Honor platform/build instructions. Static/PSI/historical checks do not prove
  generated DI, UI/native runtime or platform integration. Validate skill YAML,
  links and examples. Firebase sign-in/sends/uploads need explicit authorization.
