---
name: state-preservation
description: Implement and review Grippo feature state preservation when creating or changing screens, forms, selections, filters, navigation or initialization. Preserve user-owned drafts with Decompose StateKeeper, rebuild data-backed state, exclude secrets, and verify restoration with fresh Components and ViewModels.
---

# Feature state preservation

Apply when implementing or changing a feature. For a new feature, inspect all its
screens and parent flows; for an existing feature, inspect every affected owner
and field. Changes that cannot affect user-owned state need an impact assessment,
not new snapshots. Stay within the requested feature and platform scope.

Read `CLAUDE.md`, the affected routes/Components/ViewModels/States and
[process-death restoration](../ui-feature/references/process-death.md) for the
implementation pattern and current platform limits. For UI state types read
[state serialization](../ui-feature/references/state.md#process-death-safety-must).

## Mandatory preservation gate

Before completing feature work:

1. Inventory actual rendered screens, nested flows, route inputs, editable fields,
   accepted picker results, selections, current filters and parent data needed by
   later steps. Inspect callbacks, initialization, Back/pop and re-entry after
   child destruction, not only serializers.
2. Classify each meaningful field/group in the feature's existing task or coverage
   record. User-owned nonsecret screen drafts MUST survive supported saved-state
   restoration. Live dialog sessions and their unfinished input remain unsaved;
   accepted results are preserved by the owning screen/parent. Preserve the latest value, including invalid or explicitly empty
   input; a route seed cannot replace a saved draft. State with no editable data
   needs no extra supplier.
3. Implement the required preservation at its existing owner using the pattern
   below. Keep data-backed state rebuilt from its existing source. Do not serialize
   all state indiscriminately or move ordinary screen drafts to a global store.
   Inspect ViewModel members, closures and State fields: reject restoration/edit
   flags, previous-source caches and equivalent shadow data hidden behind helpers.
4. Review real save → serialization roundtrip → destroy old lifecycle and
   InstanceKeeper → fresh ComponentContext/InstanceKeeper → restore wiring.
   When runtime verification is permitted, check this sequence manually. Check exact
   unfinished input/selection, Back, parent-to-child values and final business
   inputs. Where a child can be destroyed and entered again, verify accepted
   parent values seed its fresh state without an archive. Retained instances or
   a State-only copy are insufficient proof of wiring.
   Review pause/resume without repeating form initialization, including the actual
   disconnect grace of lifecycle-aware subscriptions. Source review alone does not
   prove runtime resubscription.
5. Reconcile the inventory with implementation and permitted checks. Reject missing required
   fields, stale route-prefill restoration, secret leakage and duplicate ownership.
   Record actual checks and shell limitations; honor platform/build restrictions.

Use this compact inventory in the existing feature record:

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|

## Ownership and implementation

- Screen routes carry navigation identity and initial inputs. Define one final invariant
  foundation `PersistenceKey<T>` as `val DRAFT` in a private companion object at
  the bottom of the owning Component, pairing the stable archive address with its
  matching serializer. No separate metadata file or feature-wide matrix.
  Reuse exact existing archive addresses when refactoring; never derive them from
  class names, toString, route IDs or live-instance identity. The descriptor is
  metadata and does not belong in State/Router payloads or DI.
- The owning Component calls protected `restoreState(DRAFT)` inside the existing
  retainedInstance factory, passes typed `initialState` to a fresh VM and
  calls `saveState(DRAFT) { viewModel.state.value }` once during construction. This
  registers a lazy local supplier; the shell's StateKeeper.save creates the snapshot.
  Use these helpers rather than raw draft StateKeeper.consume/register calls.
  No aliases, extensions or DI are needed for the descriptor.
  Child ComponentContexts isolate identical descriptors for separate live owners.
  A data-backed form may accept nullable initialState to distinguish an absent
  archive from a saved snapshot, including an all-default snapshot. BaseViewModel
  still receives a complete non-null state; evaluate dynamic fresh defaults once.
- Restore accepted parent-flow data before constructing children that use it.
  Pass already accepted parent values as ordinary business initial inputs to a
  fresh child with no local archive. Priority is retained VM, then local saved
  draft, then accepted parent/route inputs, then defaults. Parent state keeps
  accepted business values; do not move unfinished drafts of destroyed children
  into parent/global state for this mechanism.
  Each owner saves its own draft; no extra callback synchronization, provider,
  public extension, generic BaseViewModel persistence or per-edit route replacement.
  Edits and saves must keep the current VM and screen presentation unchanged.
- Use existing `@Serializable` state when it represents the draft cleanly. Mark
  rebuilt/runtime fields `@Transient` with safe defaults. Reuse ImmutableListSerializer
  and existing FormatState; enable the owning module's serialization plugin.
  Saved fields need stable defaults: a date-dependent default can be omitted
  during encoding and change on restoration. Apply dynamic initial values only
  when no saved draft exists.
  When removing saved fields, check the actual StateKeeper codec against an old
  archive. If only obsolete metadata is removed, class-scoped JsonIgnoreUnknownKeys
  can preserve recognized typed values without legacy fields or recovery fallbacks.
  Known fields must still reject invalid types; global opt-ins belong to convention.
- Rebuild catalogs, localized presentation and provider availability from current
  sources. Preserve catalog selection intent across an initial empty emission;
  saved known/selected or excluded IDs must distinguish deselected and new items.
- Passwords, authentication tokens, provider credentials/codes and other secrets
  MUST NOT enter StateKeeper archives or route payloads. Serializable type support
  does not authorize persistence. Verify omitted fields in the serialized payload
  and empty/default secret inputs after fresh restoration. Normal retained memory
  does not need to be cleared by this mechanism.
- Loaders, jobs, dialog/error sessions, callbacks, focus and telemetry provenance
  are runtime state. Do not add them to business snapshots. Presentation-only widget
  state has its own UI policy and is not a business draft.
- Restoration initializes business state directly; it must not replay input/control
  callbacks, emit synthetic analytics, auto-submit authentication or serialize jobs.
  Restarting existing startup/completion operations remains business-owned; verify
  cancellation and the current operation policy without inventing retry machinery.
- StateKeeper restores supplied archives; it does not guarantee every cold start.
  Android supplies saved state through retainedComponent. iOS restores one validated
  background checkpoint before constructing the root, then keeps that root live
  through foreground transitions. Abrupt active termination before a completed
  checkpoint is not guaranteed. Feature keys and local ownership stay unchanged;
  add no feature storage or lifecycle bridge. See the reference's
  [iOS policy](../ui-feature/references/process-death.md#ios-specifics), including
  format/build invalidation when archive schemas change. Dialog sessions stay
  unsaved; accepted results belong to the owning screen's draft.

## Initialization from persistent settings

A selection already committed to an existing durable settings owner need not gain
an additional StateKeeper copy solely for restoration. Record that source in the
inventory; this does not replace local preservation of unfinished screen drafts.

Keep an unfinished selection distinct from the committed source when both exist.
For example, checking an analytics opt-in draft is not a stored permission to
collect. Persist the unfinished input with the screen draft and rebuild the source;
do not let a late source emission erase the input or an old draft overwrite a known
decision. Choose the smallest state representation that preserves these meanings;
this does not mandate two fields for every settings control.

When restored settings arrive asynchronously, review the interval before their
first value as well as the loaded state. Dependent requests MUST NOT use a fallback
as though it were the restored selection; apply the fallback once absence is known.
Dependent controls must either wait for that value or preserve permitted early
edits under the existing draft policy. Inspect actual render conditions and callback
eligibility, not only the subscription. Reuse meaningful State and existing loading
or eligibility mechanisms; do not add restoration flags or block unrelated controls.

## Data-backed form synchronization

The current State owns the draft while the screen is open. Populate a fresh form
once from its business source; a restored snapshot keeps its saved values,
including Empty/null/default values. Use the existing initialState boundary to
choose initialization, without storing another mode or source snapshot. Preserve
nondefault values entered before the first source arrives. A choice equal to a
fresh default carries no separate edit history and may receive initial source data.

ViewModels MUST NOT keep private runtime flags, previous-source caches or shadow
snapshots for restoration, edit detection or synchronization. This includes
restorePending, previousSource and previousSourceSelection, regardless of type.
Do not move equivalent flags/copies into State, a wrapper, a provider or a mutable
closure. Persist actual values without hasEdited/selectionEdited/dirty metadata.
Use State/update for feature data and derive conditions from existing values.

Later source observations rebuild actual display data and catalogs, without
resynchronizing form inputs. Both fresh and restored drafts may therefore remain
older than server values while open. Do not reconstruct field provenance or add
automatic merge machinery. A one-shot initialization must not restart on ordinary
pause/resume; use the existing VM-owned safeLaunch, or Processing.Infinity for a
finite Flow, rather than a lifecycle-repeated subscription.

Catalog known/selected IDs are business identity, not edit flags: keep them where
required to distinguish deselected known items from new items and preserve accepted
parent exclusions. Preserve choices for known IDs, seed new IDs from the current
business source and discard IDs absent from a nonempty current catalog. Empty
catalog emissions must not erase the draft. Verify first-load edits, restored
defaults, pause/resume, changed sources, empty catalogs and final requests.

## Self-review

Review the actual source and inventory, fix findings, then inspect the resulting diff
again. Report the fields preserved, sources rebuilt, secret exclusions, permitted
checks and unverified restoration scenarios without claiming runtime proof from source.
