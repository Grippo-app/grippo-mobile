---
name: feature-analytics
description: Implement and review typed Grippo KMP feature analytics and reconcile its live analytics admin configuration with code and released schemas. Use for instrumentation, analytics reporting/admin tasks and feature behavior changes; preserve observation, privacy and coverage gates.
---

# Feature analytics

## Scope

Apply when feature behavior changes, even without an explicit tracking request.
Visual-only changes need an impact assessment, not new events. Read `CLAUDE.md`, current analytics contracts and the affected
coverage matrix. For UI wiring read [ViewModel integration](references/viewmodel.md).
Stay within the requested feature and verification scope.

Operational analytics tasks MUST read and pass
[live admin synchronization](references/admin-sync.md). This includes affected
instrumentation during feature work: attempt browser inspection early, ask the
user to open the correct analytics admin tab if unavailable, reconcile affected
definitions/key events/reports with code and released schemas, and re-open saved
settings to verify them, or use the gate's authorized live API/read-back alternative.
Source review alone MUST NOT substitute for this gate.
Editorial-only analytics instruction changes and visual-only feature work with a
documented no-analytics-impact assessment need that assessment only, with no browser
access gate or admin mutations. Analytics setup/report changes still require the
gate even when app telemetry is unchanged. Review-only requests permit inspection,
not unrequested writes; an explicitly user-restricted source-only review follows
the owning gate's access exemption and labels admin state uninspected.

For collection consent, core/SDK transport, asynchronous attribution or schema
compatibility changes, read [collection and core contracts](references/collection-and-core.md).
Ordinary feature instrumentation does not require changing those owners.

For a behavior change, inventory affected leaf/graph routes, callbacks, inputs and
complete operations in the coverage matrix. Reuse or extend the feature's single
API/implementation; wire only the affected VMs. Before completion reconcile the
matrix with actual callers, record deliberate event omissions and permitted checks.
For a new feature, inventory all its screens; for an existing feature, inventory
all screens in the affected scope. Apply the mandatory screen coverage gate below.
Current skill/standard/source define the contract; historical review reports do not.

## Small public API

- One stateless `<Feature>Analytics` interface in `:analytics:analytics-api`:
  `trackEvent(FeatureEvent)`, generic `trackOperation(FeatureOperation, block)` and
  `presentScreen(FeatureScreen, reason)`. Events do not perform business actions.
  Expose only implemented capabilities in the requested scope. Screen-only
  integration uses presentScreen and a screen enum; add event/operation methods
  with their actual instrumentation, without empty models or placeholder methods.
- Put closed event/operation families, screen enums and necessary feature enums
  in sibling `models/`. One standalone type per file; related sealed variants stay
  together. Constructors receive direct typed facts, not nested action wrappers.
- Reuse common InputStatus/InputKeyboardAction/ControlAction enums when suitable.
  Keep actual feature fields/methods/pickers/experience values feature-owned.
  No generic capability hierarchy, scenario factories, snapshots or compatibility aliases.
- Public feature events describe observations; they are not toolkit wire events.
  Internal implementation owns encoding, schemas, keys and stable wire codes.
  Public fact enums are plain enums: no AnalyticsCode or wire-string properties.
  Use typed keys with internal exhaustive enum encoders; field/action keys accept
  closed types, not the open AnalyticsCode interface.
  No string event API, nullable parameter bag or helper/method/file per button.
- Keep structurally invalid combinations out of constructor types: credential
  fields belong to credential events, picker events accept only picker enums,
  and fixed fields such as Name need no public field selector.

## Observation boundary (MUST)

Before adding an event or parameter, identify the existing callback, operation or
foundation hook where the fact becomes known. Observe it at that owner; pass only
the bounded typed fact to the analytics API.

- Business/navigation contracts MUST remain independently necessary when analytics
  calls are removed. MUST NOT add route/Direction/config fields, business types,
  constructor arguments or saved state solely to carry reporting context between
  owners. A genuine business change follows its owning skill and authorized scope.
- Observation types belong to the feature's analytics-api models; UI fact mapping
  stays in its existing analytics/ package. MUST NOT place telemetry-only models
  in navigation, domain or shared UI-state modules to make them easier to transport.
- Use the existing VM injection and foundation presentation boundary. Neutral UI
  callbacks MAY expose real interactions under the widget rules below; they carry
  ordinary UI values and no telemetry types or cross-screen reporting history.
- Before completion MUST trace each added type/field/argument to either an actual
  analytics API consumer or an independent business/UI requirement. Reject unused
  models and reporting-only changes outside the observation boundary. A fact that
  requires transporting reporting context/history between business or navigation
  owners MUST remain an explicit coverage omission until its reporting requirement
  is resolved; do not infer it or build a context propagation mechanism merely to
  complete a report. Neutral interaction callbacks to their owning VM follow the
  permitted widget boundary above.

## Business stays business

- Analytics decorates complete existing operations and preserves caller `T`,
  original business exceptions and cancellation. Business alone decides requests,
  validation, retries, fallback, navigation and results. Never branch on telemetry.
- Preserve the existing return/error contract. For a Result-returning call, place
  its existing `getOrThrow()` inside the tracked business block so failure reaches
  the decorator. Direct-value throwing use cases remain direct; never reshape a
  use case to fit telemetry or treat a returned Result.failure as business completion.
- Keep whole use cases; do not expose preparation/stages for timing or add a
  coroutine/job/dispatcher/retry solely for analytics. Submit intent and operation
  start are different facts. Emit after existing eligibility/busy guards.
  Rejected callbacks emit neither accepted intent nor operation events; verify
  both accepted and rejected paths without changing business guards.
- Ordinary reporting exceptions cannot prevent business/lifecycle or replace
  success/errors. Keep the existing isolation boundary; fatal Error stays fatal.
  Foundation ScreenAnalytics isolates screen present/close Exceptions. Feature
  presentScreen passes typed navigation and the original presentation through,
  without duplicate catches, no-op fallbacks or wrappers.
- ViewModels unwrap Result with `getOrThrow()` inside existing `safeLaunch`.
  Follow the [error pipeline](../ui-feature/references/error-pipeline.md): no local
  recovery catch/wrapper; pure mapError changes dialog presentation only, onError
  runs after dismissal. Both safeLaunch overloads keep the same hooks.
- Do not add cancellation checks solely for analytics; business cancellation follows
  the [canonical coroutine rules](../validation-gates/references/forbidden-patterns.md#coroutines).
  Keep business lambdas multiline;
  short update/simple Flow operators compact. Named local functions are forbidden.
  Startup/retry uses the existing VM scope and class-level private methods.

## UI wiring and events

- UI main depends on analytics-api; implementation lives in `:analytics:<feature>`
  and imports no UI/foundation/routes/domain/use-case/service/SDK or toolkit core.
  Keep implementation internal, with one public annotated module. Use existing
  convention plugins, explicit API and generated `.module`/`@Single` DI.
- Inject the stateless feature service as `analytics` into the ViewModel through
  the Component's normal retainedInstance block, just like other dependencies.
  Do not pass it through Component constructors/routes/factories or emit outside VM.
- Every independent screen MUST override BaseViewModel.screenPresentation with
  `(NavigationReason) -> ScreenPresentation`, calling typed presentScreen.
  BaseComponent binds it during doOnCreate before activation/navigation bridging.
  Foundation alone resolves ScreenAnalytics and supplies lifecycle/reason.
- Graph/container VMs MUST keep the default null hook: no container screen_view,
  screen binding or lifecycle service resolution. VM stores no Lifecycle/ComponentContext.
  No activation-flow observer, extra job, feature provider/route matrix or manual
  bind is needed.
  The nullable base API supports containers; screen coverage is not optional.
- Keep normal Component retainedInstance/ViewModel lifecycle ownership. Analytics
  adds no StateKeeper/InstanceKeeper keys, holders, snapshots or scenario retention.
  Navigation IDs may remain for navigation; analytics does not consume them.
- ScreenAnalytics owns the per-binding flag: first activation Initial, later Restore.
  Pause/destroy closes the presentation. A fresh binding has no saved provenance.
  Do not invent referrer, flow continuity, entry attribution or Action/Back reasons.
- Emit direct typed events in existing Contract→ViewModel callbacks. Use existing
  validation status; map it in UI analytics/, never add analytics validators.
  Text status emits only when the existing status changes; initial Empty→Empty
  is omitted. Do not add a status cache. Picker confirmations/clicks/selections
  remain distinct; programmatic navigation is not a fake user click.
- Neutral optional design-system callbacks may expose focus/clear/IME/visibility;
  they import no analytics types and preserve default keyboard/focus behavior.
- Dialog/picker open and accepted-result facts belong to the owning VM's existing
  callbacks, including its onResult closure passed to a config. Shared host/config
  plumbing and Components do not construct events. Built-in dialog
  schemas add no automatic views/close tracking: record that omission unless the
  requested scope defines a separate typed presentation contract and owner.
- Keep only observed facts. Do not add transition/flow events, error classifiers,
  provider-cancel outcomes or extra orchestration for a more detailed funnel.
- Use DismissRequested for an observed user request to leave/close a surface,
  encoded as dismiss_requested. It records intent, not completed dismissal or
  cancellation. Keep navigation Back/onBack names where they describe navigation;
  do not rename business contracts merely to match telemetry. Programmatic pop,
  lifecycle close and successful completion are not user dismissal requests.
  A telemetry rename must reconcile public events, internal action types/matrices,
  wire encoders, VM callers and current coverage together; retain no compatibility
  alias without a released-consumer requirement.

## Mandatory screen coverage gate

Before completing a new feature or an affected screen/navigation change:

1. Enumerate actual routes and rendered surfaces, including nested/tab screens
   and alternative entry paths. Trace each to its Component and retained ViewModel;
   do not use only the FeatureScreen enum or coverage matrix as the inventory.
2. Classify each surface: an independent screen needs exactly one correctly typed
   screenPresentation in its owning VM, including Splash and standalone loading
   pages without controls. A host that only renders child screens or
   shared chrome is a container and keeps default null. Classify by actual UI,
   not filename, base class or presence of a router. Dialogs/overlays follow their
   separately scoped presentation contract, not this screen requirement.
3. Reconcile route → surface → Component → VM → FeatureScreen → hook in coverage.
   Record container rows with their null-hook reason. Verify normal retained-VM
   injection and the shared foundation binding; add no feature lifecycle wiring.
4. Reject missing hooks, wrong screen identities, screen enum entries with no
   owner and container screen views emitted alongside their child screens.
   Fix them within the task scope before marking the feature complete;
   an omissions row cannot waive a required
   screen hook. Record permitted checks and explicit unrun runtime checks.

See [ViewModel integration](references/viewmodel.md#screen-classification).
Create/update coverage in the current task or feature record; no separate template
is required. Record route/surface, Component, VM, typed screen/hook or container
reason, callbacks/inputs/operations, omissions and checks.

## Schemas, privacy and checks

- Centralize typed schema/key instances, explicit versions and bounded wire codes.
  Register the complete custom catalog once through a uniquely qualified @Single;
  reuse built-in screen/dialog schemas. Semantic changes/drops need a documented
  reporting decision; a version bump alone does not make reports compatible.
  Validation changes also require assessing status meaning/event population even
  when the wire schema is unchanged. Do not silently add dropped funnel mechanisms.
- New schemas start at v1. The current unreleased catalog remains v1 by the user's
  explicit decision; do not increment versions for each implementation correction.
  Once a schema has produced released data, assess consumers and compatibility
  before changing its meaning; do not reset released versions to apply this rule.
- Operation decoration supplies fresh OperationId, monotonic duration and
  started/completed/failed/interrupted factories. Preserve original results/errors;
  completed means the caller block completed, not an inferred business success.
- Never encode raw text/length, credentials/tokens, entity IDs/body measurements,
  exception messages, URLs or request/response bodies. Use enums/status/counts.
  Consent/identity/SDK transport stay owned by core/application policy.
- Maintain route/control/input/operation coverage with actual owners and explicit
  omissions. One feature does not prove global coverage or Firebase delivery.
  Reconcile existing summaries and omission lists with the final scoped matrix;
  appending a new section must not leave contradictory current-coverage claims.
  Keep historical results explicitly scoped to their original change.
- Self-review naming/boundaries/DI/event matrix/privacy/error isolation and overhead;
  fix findings and review again. Remove obsolete API/callers/helpers/docs.
- Verify actual payloads, results/errors/cancellation, reporting-failure injection,
  leaf VM screen coverage/foundation lifecycle and eligible callbacks. Check real code, not copied
  helpers. Run permitted static/manual checks and builds; record actual results/skips.
  Do not introduce automated test infrastructure without an explicit user request.
- Honor platform/build instructions. Static/PSI/historical checks do not prove
  generated DI, UI/native runtime or platform integration. Validate skill YAML,
  links and examples. Firebase sign-in/sends/uploads need explicit authorization.
