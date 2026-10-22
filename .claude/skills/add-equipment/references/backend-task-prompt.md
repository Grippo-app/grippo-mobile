# Backend task prompt

Generate a filled, ready-to-copy prompt after an equipment integration leaves
server work pending, including an explicit mobile-only task. Put the prompt in
the reply in the user's language. A requested saved copy is an artifact, not a
new review record. Sending it or creating another chat follows the user's
explicit instructions and the existing task-coordination rules.

An artwork-only change needs no backend task. When the matching type and catalog
row are already verified in the target environment, report that no backend work
is needed. If only source support is known, keep target availability unresolved.

## Preserve the requested outcome

Carry the user's backend intent into the opening and acceptance criteria:

- **Plan only:** request a plan of changes, checks and dependencies; do not turn
  that request into implementation.
- **Local implementation and SQL preparation:** request local source changes,
  reviewable SQL/preflight preparation and applicable local checks.

Mobile implementation or generating a handoff does not select the backend mode.
If the user has not established it, mark the outcome unresolved rather than
silently requesting implementation. Carry an established choice forward.
Route the receiving task to the backend repository's `AGENTS.md` and
`grippo-equipment-create` at `.agents/skills/grippo-equipment-create/SKILL.md`.
That skill owns the backend workflow and detailed SQL/deployment rules.

## Gather the handoff facts

Read the final client code, [identifier rules](naming.md) and
[integration map](integration-map.md). Include only relevant source paths and
checks. Classify the handoff facts explicitly:

- **Verified contract:** exact facts supported by authoritative backend evidence
  or an owner-attested contract under
  [backend-contract-client](../../backend-contract-client/SKILL.md). Name the
  evidence and its revision or the owner's statement and date. Record the API
  `type`, group `type` and stored server `name` separately; confirmation of one
  does not confirm the others. Reuse confirmed decisions without asking again.
- **Implemented client expectation:** the finalized naming tuple, including raw
  type/group keys, domain/UI members, vector file/property/builder/cache (including
  reused assets), title/hint and group resource keys, preview identity and localized
  name in the mobile working tree. Mark internal client identifiers as client-only;
  they are evidence for matching this change, not new backend fields. Client code
  does not prove deployed server support.
- **Mobile source state:** repository and inspected HEAD commit/tag, plus whether
  the working tree has staged, unstaged or untracked changes. Identify equipment
  support that depends on uncommitted files; HEAD alone does not describe them.
  Mark revision or working-tree status unknown when not inspected.
- **Build and delivery evidence:** separately list confirmed build commands and
  results with the source revision/working state they checked, released artifacts
  or versions, observed installed versions and their scope, and supported versions.
  Cite the evidence for each; leave missing build/release/install facts unknown.
  Ready source or a successful
  local build does not prove a published release or an installed client update.
- **Unresolved:** target environment, stored row/group IDs, database enum metadata,
  release support and other facts not established by the inspected evidence.
  Ask the future task to resolve them; do not invent UUIDs or use synthetic preview
  IDs as server IDs. A known real ID copied into a fixture still needs verification
  in the target catalog.

Describe the machine's distinguishing mechanism, weight-entry convention and the
existing equipment identity to preserve. Localized mobile labels are presentation
resources, distinct from the single stored server `name` and API `type`. Do not
copy a translation into an unagreed server name; mark that value unknown. Mobile
labels do not establish a new backend localization requirement. Surface actual
conflicts or missing decisions at the stage that needs them, rather than asking
for every unknown field before planning or local preparation.

Preparing the handoff, plan or local SQL requires no live database connection.
With target/access unknown, retain unresolved schema, enum name and persistent
UUIDs and request future metadata/catalog preflight under the backend skill.
Source facts can inform those checks; they do not prove the target metadata.

## Compose the task

Replace the bracketed fields below with known facts and explicit unknowns. Choose
one opening that matches the user's intent, and only its requested work and
acceptance criteria. These are prompt templates, not API or SQL examples.

Plan-only opening:

```text
Prepare only a plan for adding [equipment and mechanical distinction] as a
separately selectable Grippo backend item. Do not implement the changes.
```

Local implementation and SQL preparation opening:

```text
Implement local backend support for [equipment and mechanical distinction] as a
separately selectable item and prepare the required SQL and future preflight.
```

Continue with:

```text
[Selected opening above; if intent is unknown, state that it remains unresolved.]
Preserve [existing equipment keys/identities]. Read the backend repository's
AGENTS.md and apply grippo-equipment-create from
.agents/skills/grippo-equipment-create/SKILL.md.

Contract and mobile state:
- API type, group type and stored server name: [separate values, evidence and
  status for each; mark unagreed values unknown and retain confirmed decisions].
- Client naming tuple: [final exact names under the identifier rules; mark reuse
  and client-only fields, including synthetic preview IDs that are never server
  UUIDs; separately mark any known real fixture IDs as unverified for the target].
- Localized mobile labels and weight convention: [presentation values, separate
  from the stored server name and API type].
- Mobile repository/revision: [inspected HEAD/tag or unknown; staged/unstaged/
  untracked status; relevant uncommitted equipment changes or unknown].
- Relevant client files: [paths].
- Builds: [confirmed command/result and checked source state, or unknown].
- Releases: [confirmed artifacts/versions and evidence, or unknown].
- Installed clients and support policy: [separate observed versions/scope and
  supported versions/evidence, or unknown; neither follows from source readiness].
- Backend source facts: [source/revision or explicitly unavailable].
- Unresolved environment/data/release inputs: [concrete missing facts].

Requested result:
- [Plan only: describe changes, checks, dependencies and future target preflight.
  OR local implementation and SQL preparation: implement the local source slice,
  prepare SQL/preflight and run applicable local checks. Keep only the chosen mode.]
- A handoff, plan and local SQL preparation do not require live DB access. If the
  target is unknown, continue locally and describe future metadata/catalog checks.
  Keep schema, enum name and persistent IDs unresolved until verified; do not guess.

Requirements for the planned or implemented solution:
- Inspect the current catalog read path, equipment dictionary, persistent enum
  or equivalent storage, group relation and existing catalog-update mechanism.
- Reuse an existing matching row and ID. If absent, prepare an idempotent addition
  with its own server-generated identity in the verified group. Preserve old rows,
  IDs, exclusions and exercise associations; detect duplicates or conflicts.
- Align source and persistent dictionary support. A source enum edit alone does
  not establish stored catalog support. Follow grippo-equipment-create for local
  SQL preparation and future target metadata verification.
- Check older clients: unknown equipment types currently cause mobile catalog
  refresh to fail completeness validation. For publication, establish supported
  client versions and a compatible rollout; record missing release evidence as
  a future dependency while continuing the selected local work.
- Keep existing API shape and identity semantics. Exercise/content changes and
  new endpoints require their own demonstrated requirement; inspect current APIs
  instead of assuming an equipment-create endpoint exists.

Acceptance and delivery:
- [Plan only: deliver the reviewed plan and unresolved dependencies.
  OR local implementation and SQL preparation: deliver reviewed source/SQL,
  local check results and unresolved inputs. Keep only the chosen mode.]
- Future target acceptance, after agreed application: verify the existing
  catalog response contains exactly one new item with the exact key,
  its own ID and the correct group, alongside the preserved original equipment.
- Include in future target acceptance: check exclusions can distinguish the two
  server IDs, and document the client refresh needed to observe the row.
- Return final key, item/group IDs and environment only when verified. Separate
  prepared source/data work from applied changes and deployed availability.
- Live schema/data writes and deployment follow the backend's operations rules;
  planning or local preparation does not authorize those operations.
```

## Handoff check

The resulting prompt contains the actual key or an explicit request to resolve
it, the selected user outcome and backend skill, separate server name/localized
labels, final client naming tuple, mobile revision/dirty state and separate
build/release/install evidence. It preserves the original identities, exclusions,
exercise refs and compatibility requirements. Acceptance matches the requested
mode; target metadata/readback are future dependencies when the target is unknown,
not prerequisites for local preparation. The tuple agrees with source; unknown
values remain explicit. A future backend task can act on it without reconstructing
the mobile conversation or re-confirming settled decisions.
