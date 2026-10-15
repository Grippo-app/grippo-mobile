# Live analytics admin synchronization

This reference owns the mandatory code ↔ admin reconciliation workflow. Apply
[feature analytics](../SKILL.md) and the project's execution/verification policy.
The outcome is a maintained, verified reporting contract for the affected scope,
including released consumers. Matching names alone is not synchronization.

## Trigger and access gate (MUST)

- The gate applies to three kinds of work:
  - an instrumentation change (events, parameters, meanings, screens, operations,
    consent or transport);
  - an analytics setup, report or audit task, even when app telemetry is
    unchanged;
  - a behavior change ([CLAUDE.md definitions](../../../../CLAUDE.md#definitions))
    whose diff changes an analytics-facing symbol. The analytics-facing symbols
    are: an event or operation model, a parameter, a schema, an encoder, a screen
    enum, a `screenPresentation` hook, a `trackEvent`/`trackOperation` call (its
    event or operation type, arguments or placement - business code inside the
    decorated block is not a trigger), a consent read/write and a transport call.
- Exceptions, each decidable from the diff:
  - A visual-only change passes with an impact assessment instead of the gate.
    The assessment names the surfaces checked - events, parameters, screen hooks,
    operations, consent - and cites the diff evidence that none changed. Invented
    example with real file names: "Visual-only: checked events, parameters,
    screen hooks, operations and consent; the diff touches only
    `ProfileBodyScreen.kt` spacing and `AppDp`, with no file under `analytics/`
    or `toolkit/analytics/` and no `screenPresentation`, `trackEvent`,
    `trackOperation` or consent change." The `gate-auditor` verifies the
    assessment against the diff; the author's statement alone
    waives nothing. If any analytics-facing symbol changed, the gate applies.
  - A small editorial change to analytics rules or records needs no gate.
  - A review the user explicitly restricted to source-only scope skips live
    access. Label the admin state uninspected and draw no live-admin conclusion.
    Do not infer source-only scope merely because the request omits browser
    instructions. Other analytics audits and reviews still attempt inspection;
    read-only scope does not authorize configuration writes.
- No admin API is configured in this repository. Unless the user supplies an API
  for the task, reconcile through the analytics admin tab the user opens in Chrome,
  operated by the `analytics-admin-auditor` agent. Attempt to discover and inspect
  that tab early in the task with the available browser tools. Use the browser's
  supported interface. Do not bypass authentication or extract credentials.
- If the browser, tab, tools or access are unavailable, explicitly ask the user to
  open the analytics admin tab for Grippo and make it accessible. Name the missing
  capability or property selection. Do not request passwords, tokens or
  screenshots as a substitute for access. Continue independent source work while
  waiting.
- Missing browser capability and missing admin access are different limitations.
  After the mandatory browser attempt or request, a user-supplied admin API may
  complete reconciliation through live reads, writes and read-back verification.
  Record the unavailable browser inspection separately. Do not call API-verified
  admin work unfinished merely because opening a tab cannot supply a missing
  browser tool.
- Verify the account/property identity and the affected streams before writing.
  The property comes from the synced Firebase configs and the user's selection in
  the admin console (see
  [secrets and configuration](../../platform-build-toolkit/references/secrets-and-config.md)).
  Never hardcode account or property ids. If that mapping is unavailable, use an
  explicit trusted user selection or confirmation plus matching stream/app
  identifiers. Otherwise ask which property to use and wait before any write. An
  open tab or its title alone is not evidence that it is the right property.
  Include Android and iOS when both emit the affected contract. Separate web data
  when the requested report concerns the mobile app. Do not infer release state
  from the repository date or app version alone.
- If neither the browser nor a user-supplied API provides admin access, record the
  attempted access, the outstanding user request and the exact reconciliation
  still needed. MUST NOT claim full synchronization or task completion when
  requested admin work is unfinished. Do not poll endlessly. A documented source
  change can be ready while the admin gate remains unfinished.
- A read-only audit with live access can finish by reporting the inspected
  configuration and actual findings. It MUST NOT claim that drift was fixed or
  that the property is fully synchronized. Its findings are not authorization to
  repair them. Without live access, admin drift remains unknown and a requested
  live audit remains incomplete. A source-only audit may finish within its
  explicitly requested source-only scope.

## Reconcile before mutating (MUST)

1. Trace the source side first:
   - the actual callers, wire schemas/encoders, parameters, allowed values and
     consent/SDK behavior;
   - where each fact becomes true. A click, a screen initialization, an operation
     completion and a persisted business outcome are different facts;
   - the identity callers and the reporting unit. Events, GA4 users, accounts and
     business entities are distinct. Counts and SDK screen context MUST NOT imply
     an account/entity or operation-owner mapping without supporting source or
     received evidence;
   - the entire decorated block, including reads and local work after a successful
     write. A failed or interrupted outcome does not by itself prove that the
     write was rejected or rolled back.
2. Inspect the admin side separately from the source side:
   - the current admin definitions, derived-event rules, key events, counting and
     value settings, and the affected reports, funnels, comparisons and audiences.
     Check received events and deployment evidence separately. Current source is
     the contract for the new implementation. Live received schemas and released
     clients remain compatibility evidence. Neither side silently overrides the
     other's semantics.
   - collected events and history versus editable admin objects. GA4/Firebase raw
     event receipt is evidence; it is not a catalog that requires manual
     registration or deletion of each wire event. Reconcile the actual
     definitions, derived rules, key marks and reporting consumers. Do not invent
     objects merely to mirror source.
   - the release status, established from trusted evidence including the owner's
     explicit statement. If the app is confirmed unreleased and the history is
     self-testing only, record that boundary: test history is not evidence of
     released consumers or a product baseline. Authorized unreleased v1
     corrections may replace admin consumers without inventing a production
     migration. They still check dependent reports and older installed test
     builds. Preserve historical meaning and raw data; this is not a permanent
     exemption after release. Label test-era reports accordingly. Record the real
     release/schema start boundary when it occurs, without inventing a launch
     date or using the app version alone as a cutoff.
3. Map each affected wire event/parameter to its admin consumer or to an explicit
   omission. Each mapping states the exact conditions, streams, release status,
   dependencies and the proposed action: keep, add, update or retire. Keep this
   mapping in the admin synchronization section of `analytics/COVERAGE.md`
   ([records](../../../../CLAUDE.md#records)), replacing the rows of the affected
   consumers. Do not register every event parameter as a dimension. Register only
   bounded useful dimensions and correctly typed, unit-labeled metrics; exclude
   secrets and high-cardinality IDs. Verify numeric aggregation (sum, average,
   count), parameter presence and any denominator against the emitting event
   population, including parameters copied into derived events. Do not read
   repeated source or derived values as distinct operations. Do not read a sum as
   average latency. An evidence-based "no admin object/action required" mapping
   is valid when no affected reporting consumer needs one. An omission MUST NOT
   waive a required consumer update, missing access or an unresolved semantic
   mismatch.
4. Resolve semantic mismatches at their real owner within the authorized scope.
   Admin rules cannot repair an event fired before success, replay
   consent-discarded data or prove a returned business ID. Do not modify business
   flow solely to satisfy a report. Clearly distinguish legacy history from
   new-release definitions. Never silently merge incompatible populations under a
   success label.

## Apply and retire carefully (MUST)

- Within an authorized implementation/setup task, add or update the affected admin
  configuration as part of delivery. Use exact source parameter names and complete
  filters. When unrelated operations share the same event name, a rule on that
  shared operation-completed event MUST include the operation/action
  discriminator. Verify each affected stream's rule; do not assume that a
  property-wide key-event setting creates stream-specific rules. Avoid duplicates
  and unrelated property cleanup.
- Set counting and monetary value deliberately. Do not invent revenue for product
  milestones. Label funnels as observed steps when consent, alternative entries or
  missing attribution prevent a complete conversion measure. Explain empty or
  new-release reports. Use an app-only comparison or filter for mobile product
  metrics.
- Removal from current code or absence in a recent date range is NOT proof of
  disuse. Before retiring any definition, rule, key event, report or audience,
  inspect the affected releases, the dependent reports, funnels, audiences and
  integrations, and the historical meaning. Record evidence and replacements. If
  usage is uncertain, retain and label it legacy, or propose retirement. Do not
  silently delete.
- Prefer reversible unpublishing or unmarking, or an additive replacement, when it
  preserves the needed history. Archiving or deleting definitions may affect
  reporting and quotas; inspect the product's actual recovery behavior. MUST
  obtain action-time confirmation for an irreversible deletion under the
  applicable tool policy, even when general analytics synchronization is
  authorized. Preserve a configuration summary before retirement. That summary is
  not a backup of historical data.
- Retiring an admin consumer does not erase collected history, and it does not
  stop released clients from emitting events. Stop future emission only through
  an authorized source/deployment change, with released-client compatibility
  accounted for. Historical event/data deletion is a separate destructive action,
  never an incidental cleanup step in code/admin synchronization.
- Synchronization does not grant permission to change access, advertising consent,
  privacy/retention policy, linked accounts, billing or production deployment.
  Follow existing authorization and tool policies. No skill may waive them.

## Verification and completion gate (MUST)

- Self-review the mapping, then apply the project's independent-review eligibility.
  One owner writes the admin configuration; reviewers check source and saved
  configuration evidence. Fix actionable findings and re-review the changed area
  before completion.
- Re-open saved settings after each coherent change, or read them back through a
  user-supplied admin API. Check the exact conditions, stream, scope,
  counting/value defaults and copied parameters. A clicked Save or a transient
  form selection is not proof of persistence. Verify affected report links,
  published visibility and filters against the resulting configuration. Also
  re-check saved names and descriptions after report-data edits, so that semantic
  and prerelease explanations are not silently lost.
- If a save/publish control is disabled, inspect its actual explanation and the
  provider's supported filters/dimensions before declaring a processing or data
  blocker. Empty data alone does not establish the cause. A compatible replacement
  MUST preserve the intended population and semantics, then pass saved read-back.
- Distinguish these states: source verified; admin configuration saved and
  re-opened; SDK/remote delivery observed; processing or release pending. Do not
  fabricate events to fill production reports, assert backfill, or treat a build
  as delivery verification. Confirm delivery only from an authorized real scenario
  and received payloads.
- Keep the resulting admin state in the admin synchronization section of
  `analytics/COVERAGE.md` ([records](../../../../CLAUDE.md#records)): the property
  and streams, each affected consumer as kept, added, updated or retired with its
  dependency decision, and what remains unsynchronized. Replace the rows of the
  affected consumers; do not append a dated narrative. Actual checks,
  processing/release limitations and the exact outstanding work go in the reply.
  No credentials or raw user payloads in evidence. Full synchronization means
  every affected consumer is reconciled and the saved configuration is verified.
  Pending delivery remains explicitly unverified.
