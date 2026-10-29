---
name: state-preservation
description: Assess and preserve Grippo drafts, restoration and form synchronization for behavior changes and visual-only impact assessments. Keep secrets out of saved state.
---

# Feature state preservation

Apply to every behavior change in [CLAUDE.md](../../../CLAUDE.md#definitions).
Inspect all screens and parent flows for a new feature, or every affected owner
and field for an existing one. A visual-only change needs a one-sentence impact
assessment naming the Components, ViewModels, States and fields confirmed
unaffected. Stay within requested feature and platform scope.

Read affected routes/Components/ViewModels/States, then load the relevant owner:

| Change or decision | Required reference |
|---|---|
| Draft wiring, nullable initial state or a concrete inventory example | [Draft recipes](references/draft-recipes.md) |
| Navigation archive, fresh-owner restoration or platform limits | [Process-death restoration](../ui-feature/references/process-death.md) |
| Serializable UI state or transient fields | [State serialization](../ui-feature/references/state.md#process-death-safety-must) |

## Mandatory preservation gate

Before completing feature work:

1. Inventory actual rendered screens, nested flows, route inputs, editable fields,
   accepted picker results, selections, filters and parent data needed later.
   Inspect callbacks, initialization, Back/pop and re-entry after child
   destruction, not only serializers.
2. Classify each meaningful field/group in `analytics/<feature>/COVERAGE.md`
   ([records](../../../CLAUDE.md#records)); create the file when absent.
   Nonsecret user-owned screen drafts MUST survive supported saved-state
   restoration with the latest value, including invalid or explicitly empty
   input. Route seeds cannot replace a saved draft. Live dialog sessions and
   their unfinished input remain unsaved; owning screens/parents preserve accepted
   results. State without editable data needs no extra `saveState` supplier.
3. Implement preservation at its existing owner under
   [ownership](#ownership-and-implementation). Rebuild data-backed state from its
   real source; do not serialize all state or move screen drafts to a global
   store. Inspect VM members, closures and State fields against the
   [synchronization invariants](#data-backed-form-synchronization).
4. Review save → serialization roundtrip → destroy old lifecycle/InstanceKeeper →
   fresh ComponentContext/InstanceKeeper → restore. When runtime verification is
   permitted, check that sequence manually: exact unfinished inputs/selections,
   Back, parent-to-child values and final business inputs. A destroyed/re-entered
   child must receive accepted parent values without an archive. A retained
   instance or State-only copy proves no wiring. Check pause/resume without
   repeated initialization, including lifecycle-aware subscriptions' actual
   `WhileActive` disconnect grace. Source review proves no runtime resubscription.
5. Reconcile inventory and checks. Reject missing fields, stale route-prefill
   restoration, secrets and duplicate ownership. Honor platform/build restrictions
   and report actual checks, shell limitations and unverified scenarios.

Replace inventory rows to match current code; do not append dated entries:

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|

## Ownership and implementation

- Define one Component-owned `PersistenceKey<T>` as `val DRAFT` in its private
  companion at the bottom. It pairs a final invariant archive address and matching
  serializer. Never derive addresses from class names, `toString`, route IDs or
  instance identity, or place descriptors in State, Router, DI or a registry.
- Restore with `restoreState(DRAFT)` inside the existing `retainedInstance`
  factory and pass typed `initialState` to the VM. Register
  `saveState(DRAFT) { viewModel.state.value }` once during Component construction;
  use these helpers instead of raw draft StateKeeper consume/register calls.
  A data-backed form may accept nullable `initialState` to distinguish no archive
  from an all-default snapshot; `BaseViewModel` receives complete non-null state.
- Restore accepted parent data before constructing children. Precedence is
  retained VM → local saved draft → accepted parent/route inputs → defaults.
  Fresh children without archives receive accepted parent values as business
  inputs. Each owner saves its own draft; do not move destroyed children's
  unfinished drafts to parent/global state or add callback synchronization,
  providers, public extensions, generic BaseViewModel persistence or per-edit
  route replacement. Edits/saves keep the existing VM and presentation.
- Use existing `@Serializable` State when it cleanly represents the draft. Reuse
  `ImmutableListSerializer` and `FormatState`; mark rebuilt/runtime fields
  `@Transient` with safe defaults and enable the module's serialization plugin.
  Saved fields need stable defaults.
  Evaluate dynamic fresh defaults once, only without a saved draft: omitted
  date-dependent defaults must not change a restored value. Saved-field changes
  follow [compatibility bumps](../platform-build-toolkit/references/release.md#compatibility-bumps).
- Passwords, tokens, provider credentials/codes and other secrets MUST NOT enter
  StateKeeper archives or route payloads. Serializable support is no permission.
  Inspect serialized payloads and empty/default secret inputs after fresh restore;
  this mechanism need not clear normal retained memory.
- Loaders, jobs, dialog/error sessions, callbacks, focus and telemetry provenance
  (`ScreenAnalytics` Initial/Restore history) are runtime only. Presentation-only
  widget state follows its UI policy. Rebuild catalogs, localized presentation
  and provider availability from current sources.
- Restore business State directly, without replayed control/input callbacks,
  synthetic analytics, automatic authentication submission or serialized jobs.
  Existing startup/completion operations remain business-owned; verify their
  cancellation/policy without adding retry machinery.
- StateKeeper restores supplied archives, not every cold start. Apply
  [platform limits](../ui-feature/references/process-death.md#ios-specifics),
  including build-bound iOS checkpoints; add no feature storage/lifecycle bridge.
  Dialog sessions stay unsaved; accepted results belong to the screen draft.

## Initialization from persistent settings

Committed settings rebuild from their existing persistent owner.
[DataStore preferences](../data-layer/references/datastore.md) owns preference
storage and account/installation scope; use `LocalSettingsFeature` for its
preferences. Durable business records, including Room training drafts, retain
their [data-layer owner](../data-layer/SKILL.md). StateKeeper bridges recreation
and supported restoration. Preserve business time anchors such as start/end
instants; rebuild ticking text in the Screen.

A committed durable selection needs no duplicate StateKeeper copy solely for
restoration; record its source. Preserve unfinished screen input separately when
it exists: late source emissions must not erase it, and an old draft must not
overwrite a known decision. Controls committing every accepted change need no
unfinished draft; analytics consent follows
[collection policy](../feature-analytics/references/collection-and-core.md#collection-and-consent).
Choose the smallest meaningful State; two fields per settings control are not
required.

For asynchronous settings, review the interval before the first value. Dependent
requests MUST NOT treat fallback as restored selection; apply fallback only once
absence is known. Dependent controls wait or preserve permitted early edits.
Inspect rendering and callback eligibility, using meaningful State and existing
loading/eligibility mechanisms; add no restoration flag or unrelated control block.

## Data-backed form synchronization

Current State owns the open draft. A fresh form seeds once from its chosen
business source's first non-null emission; record that product decision in
coverage. A restored snapshot keeps Empty, null and default values as well.
Choose initialization through the existing nullable `initialState` boundary,
without another mode/source snapshot. Preserve nondefault edits made before
first source data; a choice equal to a fresh default has no separate edit history
and may receive initial source data. An explicit Edit action seeds from current
State at that action and needs no first-emission seeding.

MUST NOT keep private runtime flags, previous-source caches or shadow snapshots
for restoration, edit detection or synchronization, including `restorePending`,
`previousSource` and `previousSourceSelection`. Equivalent data cannot move into
State, a wrapper, provider or mutable closure. Persist actual values without
`hasEdited`, `selectionEdited` or `dirty` metadata; derive conditions from existing
values with State and `update`.

Later observations rebuild display data/catalogs without resynchronizing inputs.
Fresh/restored drafts may stay older than server values while open. Add no field
provenance or automatic merge machinery. One-shot initialization must not restart
on pause/resume; use the [initialization recipe](references/draft-recipes.md#one-shot-form-initialization).

Known/selected or excluded catalog IDs are business identity, not edit flags:
preserve known choices and accepted parent exclusions, seed new IDs from the
current business source, discard IDs absent from a nonempty current catalog and
keep the draft across empty emissions. Verify early edits, restored defaults,
pause/resume, changed sources, empty catalogs and final requests.

## Self-review

Review actual source and inventory, fix findings, then review the resulting diff.
Keep rows current. Report preserved fields, rebuilt sources, secret exclusions,
permitted checks and unverified restoration scenarios; source review is no
runtime proof.
