# Live analytics admin synchronization

This reference owns the mandatory code ↔ admin reconciliation workflow. Apply
[feature analytics](../SKILL.md) and the project's execution/verification policy.
The outcome is a maintained, verified reporting contract for the affected scope,
including released consumers; matching names alone is not synchronization.

## Trigger and access gate (MUST)

- Apply to instrumentation changes (events, parameters, meanings, screens,
  operations, consent or transport), analytics setup/reporting and analytics audits.
  Feature work that affects telemetry also triggers this gate. Visual-only work
  with a documented no-analytics-impact assessment and editorial-only instruction
  edits require an impact assessment only: no browser access attempt, access request
  or live admin mutation gate. A user explicitly restricting an audit/review to
  source-only scope also exempts its live access gate; label admin state uninspected
  and make no live-admin conclusions. Do not infer source-only scope merely because
  the request omits browser instructions. Other analytics audits/reviews still
  attempt inspection, but read-only scope does not authorize configuration writes.
- Early in the task, attempt to discover and inspect the relevant browser tab with
  available browser/computer tools. Prefer an authorized connected admin API for
  configuration when available, but still attempt browser inspection. Use the
  browser's supported interface; do not bypass authentication or extract credentials.
- If the browser/tab/tools/access are unavailable, explicitly ask the user to open
  the analytics admin tab for Grippo and make it accessible. Name the missing
  capability or property selection; do not request passwords, tokens or screenshots
  as a substitute for access. Continue independent source work while waiting.
- Missing browser capability and missing admin access are different limitations.
  After the mandatory browser attempt/request, an authorized admin API may complete
  reconciliation through live reads, writes and read-back verification. Record the
  unavailable browser inspection separately; do not call API-verified admin work
  unfinished merely because opening a tab cannot supply a missing browser tool.
- Verify account/property identity and affected streams against project config
  before writing. If that mapping is absent/unavailable, use an explicit trusted
  user selection/confirmation plus matching stream/app identifiers. Otherwise ask
  which property to use and wait before writes. An open tab/title alone is not
  evidence that it is the right property.
  Include Android and iOS when both emit the affected contract. Separate web data
  when the requested report concerns the mobile app. Do not hardcode account IDs
  or infer release state from the repository date or app version alone.
- If neither browser nor authorized API provides admin access, record the attempted access, outstanding user
  request and exact reconciliation still needed. MUST NOT claim full synchronization
  or task completion when requested admin work is unfinished. Do not poll endlessly.
  A documented source change can be ready while the admin gate remains unfinished.
- A read-only audit with live access can finish by reporting inspected configuration
  and actual findings; it MUST NOT claim that drift was fixed or the property fully
  synchronized. Its findings are not authorization to repair them. Without live
  access, admin drift remains unknown and a requested live audit remains incomplete.
  A source-only audit may finish within its explicitly requested source-only scope.

## Reconcile before mutating (MUST)

1. Trace actual callers, wire schemas/encoders, parameters, allowed values and
   consent/SDK behavior. Identify where each fact becomes true: a click, screen
   initialization, operation completion and a persisted business outcome differ.
   Verify identity callers and the reporting unit: events, GA4 users, accounts and
   business entities are distinct. Counts and SDK screen context MUST NOT imply an
   account/entity or operation-owner mapping without supporting source/received evidence.
   Trace the entire decorated block, including reads/local work after a successful
   write; failed/interrupted does not by itself prove that the write was rejected
   or rolled back.
2. Inspect current admin definitions, derived-event rules, key events, counting and
   value settings, affected reports/funnels/comparisons/audiences. Check received
   events and deployment evidence separately. Current source is the contract for
   the new implementation; live received schemas and released clients remain
   compatibility evidence. Neither side silently overrides the other's semantics.
   Distinguish collected events/history from editable admin objects: GA4/Firebase
   raw event receipt is evidence, not a catalog requiring manual registration or
   deletion of each wire event. Reconcile actual definitions, derived rules, key
   marks and reporting consumers; do not invent objects merely to mirror source.
   Establish release status from trusted evidence, including the owner's explicit
   statement. If the app is confirmed unreleased and history is self-testing only,
   record that boundary: test history is not evidence of released consumers or a
   product baseline. Authorized unreleased v1 corrections may replace admin
   consumers without inventing a production migration, but still check dependent
   reports and older installed test builds. Preserve historical meaning and raw
   data; this is not a permanent exemption after release. Label test-era reports
   accordingly and record the real release/schema start boundary when it occurs,
   without inventing a launch date or using app version alone as a cutoff.
3. In the existing task/coverage record, map each affected wire event/parameter to
   its admin consumer or explicit omission, exact conditions, streams, release
   status, dependencies and proposed action: keep/add/update/retire. Do not register
   every event parameter as a dimension: bounded useful dimensions and correctly
   typed/unit-labeled metrics only; exclude secrets and high-cardinality IDs.
   Verify numeric aggregation (sum/average/count), parameter presence and any
   denominator against the emitting event population, including parameters copied
   into derived events. Repeated source/derived values
   MUST NOT be interpreted as distinct operations or a sum as average latency.
   An evidence-based "no admin object/action required" mapping is valid when no
   affected reporting consumer needs one. An omission MUST NOT waive a required
   consumer update, missing access or an unresolved semantic mismatch.
4. Resolve semantic mismatches at their real owner within authorized scope. Admin
   rules cannot repair an event fired before success, replay consent-discarded data
   or prove a returned business ID. Do not modify business flow solely to satisfy
   a report. Clearly distinguish legacy history from new-release definitions;
   never silently merge incompatible populations under a success label.

## Apply and retire carefully (MUST)

- Within an authorized implementation/setup task, add or update the affected admin
  configuration as part of delivery. Use exact source parameter names and complete
  filters; shared operation-completed events MUST include the operation/action
  discriminator when unrelated operations share the same event name. Verify each
  affected stream's rule rather than assuming a property-wide key-event setting
  creates stream-specific rules. Avoid duplicates and unrelated property cleanup.
- Set counting and monetary value deliberately. Do not invent revenue for product
  milestones. Label funnels as observed steps when consent, alternative entries or
  missing attribution prevent a complete conversion measure. Explain empty/new
  release reports and use an app-only comparison/filter for mobile product metrics.
- Removal from current code or absence in a recent date range is NOT proof of
  disuse. Before retiring any definition, rule, key event, report or audience,
  inspect affected releases, dependent reports/funnels/audiences/integrations and
  historical meaning. Record evidence and replacements. If usage is uncertain,
  retain and label legacy, or propose retirement; do not silently delete.
- Prefer reversible unpublishing/unmarking or an additive replacement when it
  preserves needed history. Archiving/deleting definitions may affect reporting
  and quotas; inspect the product's actual recovery behavior. MUST obtain action-
  time confirmation for irreversible deletion under applicable tool policy, even
  when general analytics synchronization is authorized. Preserve a configuration
  summary before retirement; it is not a backup of historical data.
- Retiring an admin consumer does not erase collected history or stop released
  clients from emitting events. Stop future emission only through an authorized
  source/deployment change, with released-client compatibility accounted for.
  Historical event/data deletion is a separate destructive action, never an
  incidental cleanup step in code/admin synchronization.
- Synchronization does not grant permission to change access, advertising consent,
  privacy/retention policy, linked accounts, billing or production deployment.
  Follow existing authorization and tool policies; no skill may waive them.

## Verification and completion gate (MUST)

- Self-review the mapping, then apply the project's independent-review eligibility.
  One owner writes the admin; reviewers check source and saved configuration evidence.
  Fix actionable findings and re-review the changed area before completion.
- Re-open saved settings after each coherent change (or read them back through the
  authorized admin API), including exact conditions,
  stream, scope, counting/value defaults and copied parameters. A clicked Save or
  a transient form selection is not proof of persistence. Verify affected report
  links, published visibility and filters against the resulting configuration;
  also re-check saved names/descriptions after report-data edits, so semantic and
  prerelease explanations are not silently lost.
- If a save/publish control is disabled, inspect its actual explanation and the
  provider's supported filters/dimensions before declaring a processing or data
  blocker. Empty data alone does not establish the cause. A compatible replacement
  MUST preserve the intended population and semantics, then pass saved read-back.
- Distinguish: source verified; admin configuration saved/re-opened; SDK/remote
  delivery observed; processing or release pending. Do not fabricate events to fill
  production reports, assert backfill, or treat a build as delivery verification.
  Confirm delivery only from an authorized real scenario and received payloads.
- Record property/streams, changed/retained/retired items, dependency decisions,
  actual checks, processing/release limitations and exact outstanding work in the
  existing task/coverage record. No credentials or raw user payloads in evidence.
  Full synchronization means every affected consumer is reconciled and saved
  configuration is verified; pending delivery remains explicitly unverified.
