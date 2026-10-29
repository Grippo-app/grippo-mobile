---
name: feature-analytics
description: Implement and review typed Grippo analytics, screen coverage, privacy and live admin reconciliation. Apply to behavior changes, visual-only impact assessments and analytics tasks.
---

# Feature analytics

## Scope

Apply to every behavior change in [CLAUDE.md](../../../CLAUDE.md#definitions),
even without a tracking request, and to analytics instrumentation, setup, reports
and audits. A visual-only change needs an impact assessment instead of new events:
one sentence naming the analytics-facing symbols checked; use the list and example
in [the admin trigger](references/admin-sync.md#trigger-and-access-gate-must).

Establish affected scope from behavior impact and actual callers. Start with
changed screens/routes, then trace changed shared inputs, formatters, components
and domain operations to consuming screens, containers and callbacks whose data,
eligibility or observed facts change. Include those consumers even when their MVI
files and router entries are absent from the diff. Bound the inventory with caller
evidence; a shared dependency alone does not make every screen affected.

Read current analytics contracts and the affected feature's
`analytics/<feature>/COVERAGE.md` ([records](../../../CLAUDE.md#records)). Inventory
all screens for a new feature and all affected screens for an existing feature,
including callbacks, inputs, complete operations and deliberate event omissions.
Reuse the feature's single API/implementation and wire only affected VMs. Stay
within affected behavior and authorized verification scope. Current skills,
standards and source define the contract; historical review reports do not.

Read the matching section when its condition applies:

| Change or decision | Required reference |
|---|---|
| VM injection, screen hook, events or operation wiring | [ViewModel integration](references/viewmodel.md) |
| Feature API/models, encoders, schemas or analytics DI/catalog | [Feature contracts](references/collection-and-core.md#feature-api-and-schema-contracts) |
| Validation/status mapping, event meaning or removal, schema compatibility/versioning | [Semantics and compatibility](references/collection-and-core.md#schemas-and-compatibility) |
| Collection consent or core/SDK transport | [Source map](references/collection-and-core.md#ownership-and-source), [consent policy](references/collection-and-core.md#collection-and-consent), [verification limits](references/collection-and-core.md#verification-limits) |
| Asynchronous attribution | [Attribution contract](references/collection-and-core.md#asynchronous-attribution), [verification limits](references/collection-and-core.md#verification-limits) |
| Determine admin-gate applicability | [Trigger and access gate](references/admin-sync.md#trigger-and-access-gate-must) |
| Admin gate applies | [Full live synchronization workflow](references/admin-sync.md) |

Admin synchronization has its own mandatory trigger, exceptions, access rules and
saved-configuration gate in that reference. Assess its applicability for every
task under this skill; source review alone does not pass an applicable live gate.

## Observation boundary (MUST)

Identify the existing callback, operation or foundation hook where each added fact
becomes known. Observe it there and pass only bounded typed facts to analytics.

- Business/navigation contracts remain independently necessary after analytics
  calls are removed. Do not add route, Direction, config, business-type,
  constructor or saved-state fields solely to carry reporting context.
- Observation models belong to the feature's analytics-api; UI fact mapping stays
  in its existing UI `analytics/` package. Do not put telemetry-only types in
  navigation, domain or shared UI state to transport them between owners.
- A design-system widget may expose neutral optional focus, clear, IME or
  visibility callbacks with ordinary UI values, preserving default keyboard/focus
  behavior. It imports no analytics types; the owning VM decides what to observe.
- Use normal VM injection and the foundation `screenPresentation` boundary.
  Telemetry adds no lifecycle services in a feature, reporting history between
  screens, StateKeeper/InstanceKeeper keys, holders, snapshots or funnel retention.
  Navigation IDs stay navigation-owned and are not analytics inputs.
- Trace every added type, field and argument to a real analytics API consumer or
  an independent business/UI requirement. Reject unused models and reporting-only
  changes outside the boundary. A fact needing unresolved context/history across
  owners remains an explicit coverage omission; do not infer it or build a
  propagation mechanism to complete a report.

## Business and event invariants

- Decorate the complete existing throwing operation, including required
  credential acquisition, validation, requests and local work. Preserve its `T`,
  original business errors and cancellation. `completed` means the caller block
  completed; it does not infer business success. Business owns retries, fallback,
  navigation and results; never branch on telemetry, expose preparation stages
  for timing or reshape a use case for reporting.
- Emit accepted intent and operation start after existing eligibility/busy guards.
  Rejected callbacks emit neither. Analytics adds no coroutine, job, dispatcher,
  retry or cancellation check. Invoke commands in the existing `safeLaunch` under
  [the error pipeline](../ui-feature/references/error-pipeline.md); preserve its
  overloads and [retry placement](../ui-feature/references/error-pipeline.md#retry-and-close).
- Reporting `Exception`s cannot block business/lifecycle work or replace its
  success/error. Keep the existing isolation boundary; fatal `Error`s stay fatal.
  Foundation isolates screen present/close failures. Feature `presentScreen`
  passes the typed reason and original presentation through without duplicate
  catches, no-op fallbacks or wrappers.
- Emit direct typed facts in existing Contract → VM callbacks. Reuse validation
  status, with mapping in UI `analytics/`; add no analytics validator or status
  cache. Emit text status only when it changes, omitting initial Empty → Empty.
  Picker confirmations, clicks and selections are distinct; programmatic
  navigation is not a user click. Keep only observed facts: no inferred
  transition/flow events, error classifiers or provider-cancel outcomes.
- The owning VM observes dialog/picker open and accepted-result callbacks,
  including its config's `onResult`. Shared hosts/config plumbing and Components
  construct no events. Dialogs have no automatic view/close tracking or built-in
  schema: record the omission unless authorized scope defines a separate typed
  presentation contract and owner. Event naming and dismissal recipes live in
  [ViewModel integration](references/viewmodel.md#event-meaning-and-dismissal).

## Mandatory screen coverage gate

Before completing a new feature or a change affecting existing screens/navigation:

1. Establish affected scope through [Scope](#scope). Enumerate actual routes and
   rendered surfaces, including nested/tab screens and alternative entries. Trace
   each to its Component and retained VM; an enum or old matrix is no inventory.
2. Classify by actual content. An independent screen, including Splash or a
   standalone loading page, needs exactly one matching typed `screenPresentation`
   in its owning VM. A host rendering only children/shared chrome is a container
   and keeps default null. Filename, base class and router presence prove neither.
   Dialogs/overlays follow only an explicitly defined dialog presentation contract.
3. Reconcile route → surface → Component → VM → FeatureScreen → hook in
   `analytics/<feature>/COVERAGE.md`. Give containers their null-hook reason.
   Verify normal retained-VM injection and foundation binding, without feature
   lifecycle wiring. Read [classification details](references/viewmodel.md#screen-classification)
   when a host also displays its own content.
4. Reject missing hooks, wrong identities, screen enum entries without owners and
   container views emitted beside their children. Fix them within scope; an
   omissions row cannot waive a screen hook. Report permitted checks and explicit
   unrun runtime checks in the reply.

The current matrix has one row per route/surface with Component, VM, typed
screen/hook or container reason, callbacks/inputs/operations and omissions, beside
the preservation inventory. Replace stale rows. If the file is absent, create it
and add its pointer to `analytics/COVERAGE.md`; do not append dated narratives.
Cross-feature contracts and admin evidence belong in the root coverage file;
one feature proves neither global coverage nor Firebase delivery.

## Privacy and completion checks

- Never encode raw text/length, credentials/tokens, entity IDs/body measurements,
  exception messages, URLs or request/response bodies. Use enums/status/counts.
  Consent, identity and SDK transport remain core/application policy.
- Review naming, API boundaries, DI, schemas, the event matrix, privacy, reporting
  isolation and overhead; fix findings and review again. Remove obsolete API,
  callers, helpers and docs. Validation/status and event-semantic changes, including
  those without a wire-schema diff, apply the compatibility rules alongside schema changes in
  [feature contracts](references/collection-and-core.md#schemas-and-compatibility).
- Verify real payloads, results/errors/cancellation, reporting-failure injection,
  eligible/rejected callbacks and affected leaf hooks with foundation lifecycle.
  Check actual code rather than copied helpers. Run the permitted checks under
  [verification policy](../../../CLAUDE.md#verification-policy) and report actual
  results/unrun checks. Static/PSI/historical checks do not establish generated
  DI, UI/native runtime, platform integration or remote delivery.
- Reconcile current coverage and pass the applicable
  [admin completion gate](references/admin-sync.md#verification-and-completion-gate-must).
  Validate skill YAML, links and examples when editing these rules. Add no
  automated application test infrastructure without an explicit request.
  Firebase sign-in, sends and uploads need explicit authorization.
