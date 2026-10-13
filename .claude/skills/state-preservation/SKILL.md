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
   record. User-owned nonsecret drafts MUST survive supported saved-state
   restoration. Preserve the latest value, including invalid or explicitly empty
   input; a route seed cannot replace a saved draft. State with no editable data
   needs no extra supplier.
3. Implement the required preservation at its existing owner using the pattern
   below. Keep data-backed state rebuilt from its existing source. Do not serialize
   all state indiscriminately or move ordinary screen drafts to a global store.
4. Verify real save → serialization roundtrip → destroy old lifecycle and
   InstanceKeeper → fresh ComponentContext/InstanceKeeper → restore. Check exact
   unfinished input/selection, Back, parent-to-child values and final business
   inputs. Where a child can be destroyed and entered again, verify accepted
   parent values seed its fresh state without an archive. Retained instances or
   a State-only copy are insufficient proof of wiring.
5. Reconcile the inventory with implementation and tests. Reject missing required
   fields, stale route-prefill restoration, secret leakage and duplicate ownership.
   Record actual checks and shell limitations; honor no-build/test restrictions.

Use this compact inventory in the existing feature record:

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and test |
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
  retainedInstance factory, passes typed non-null `initialState` to a fresh VM and
  calls `saveState(DRAFT) { viewModel.state.value }` once during construction. This
  registers a lazy local supplier; the shell's StateKeeper.save creates the snapshot.
  Use these helpers rather than raw draft StateKeeper.consume/register calls.
  No aliases, extensions or DI are needed for the descriptor.
  Child ComponentContexts isolate identical descriptors for separate live owners.
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
- StateKeeper restores supplied archives; it is not durable storage for arbitrary
  cold starts. Verify the actual shell bridge. Current Android supplies saved state;
  current iOS does not. Dialog sessions stay unsaved; accepted results belong to
  the owning screen's draft.
