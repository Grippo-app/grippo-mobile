---
name: state-preservation
description: Implement and review Grippo feature state preservation when creating or changing screens, forms, selections, filters, navigation or initialization. Preserve user-owned drafts with Decompose StateKeeper, rebuild data-backed state, exclude secrets, and verify restoration with fresh Components and ViewModels.
---

# Feature state preservation

Apply to every behavior change as defined in
[CLAUDE.md](../../../CLAUDE.md#definitions). For a new feature, inspect all its
screens and parent flows. For an existing feature, inspect every affected owner
and field. A visual-only change (same definitions) needs an impact assessment
instead of new drafts. The assessment is one sentence naming the owners inspected
(Components, ViewModels, States) and the fields confirmed unaffected. Stay within
the requested feature and platform scope.

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
2. Classify each meaningful field/group in the feature's preservation inventory
   in `analytics/<feature>/COVERAGE.md` ([records](../../../CLAUDE.md#records)).
   Create the file when the changed feature has none. User-owned nonsecret screen
   drafts MUST survive supported saved-state restoration. Live dialog sessions and
   their unfinished input remain unsaved; the owning screen or parent preserves
   the accepted results. Preserve the latest value, including invalid or
   explicitly empty input. A route seed (the initial value a route passes in)
   cannot replace a saved draft. State with no editable data needs no extra
   supplier (no `saveState` lambda).
3. Implement the required preservation at its existing owner using the pattern
   below. Keep data-backed state rebuilt from its existing source. Do not serialize
   all state indiscriminately, and do not move ordinary screen drafts to a global
   store. Inspect ViewModel members, closures and State fields. Reject restoration
   or edit flags, previous-source caches and equivalent shadow data (a private copy
   of earlier data kept to compare against later) hidden behind helpers.
4. Review the real wiring: save → serialization roundtrip → destroy the old
   lifecycle and InstanceKeeper → fresh ComponentContext/InstanceKeeper → restore.
   When runtime verification is permitted, check this sequence manually. Check the
   exact unfinished input/selection, Back, parent-to-child values and final
   business inputs. Where a child can be destroyed and entered again, verify that
   accepted parent values seed its fresh state without an archive. Retained
   instances or a State-only copy are insufficient proof of wiring. Review
   pause/resume without repeating form initialization, including the actual
   disconnect grace of lifecycle-aware subscriptions (the delay before a
   `WhileActive` stream stops after the screen leaves RESUMED). Source review
   alone does not prove runtime resubscription.
5. Reconcile the inventory with the implementation and the permitted checks.
   Reject missing required fields, stale route-prefill restoration, secret leakage
   and duplicate ownership. Report actual checks and shell limitations in the
   reply. Honor platform/build restrictions.

The inventory table in `analytics/<feature>/COVERAGE.md` has these columns.
Replace its rows to match the current code rather than appending dated entries:

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|

Verified example rows for the trainings calendar (`TrainingsComponent`,
`TrainingsViewModel`, `TrainingsState`; re-inspect before copying):

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| TrainingsComponent / TrainingsViewModel | `date` (selected range) and `period` (Daily/Monthly) | Preserve | Component-owned `DRAFT` with address `TrainingsDraft` and `TrainingsState.serializer()` | Component passes `restoreState(DRAFT)` and the route's `day`; for a null archive the ViewModel applies the route seed (Daily on `TrainingsRouter.Trainings.day` through `TrainingsState.selectDay`, coerced into `limitations`) or otherwise the current-month Monthly default, and a saved draft wins over that seed; it rebuilds range-derived presentation through `selectRange`; check the restored month and mode on a fresh Component |
| TrainingsViewModel | `limitations`, monthly `weeks`/`digest`, daily `items` | Rebuild | `@Transient`; `DateTimeUtils.trailingYear()` and the `observeTrainings` flow | Recomputed from the restored range; never in the archive |
| TrainingsViewModel | `TrainingsLoader.Trainings` and the range refresh step | Runtime only | `loaders` flow; `withLoader` inside the `mapLatest` refresh chain | Not saved; the refresh runs again for the restored range |

## Ownership and implementation

- **Archive key.** Screen routes carry navigation identity and initial inputs.
  Define one foundation `PersistenceKey<T>` as `val DRAFT` in a private companion
  object at the bottom of the owning Component. The key is the descriptor: it
  pairs the stable archive address with its matching serializer, and it is final
  and invariant (the address does not change). Keep descriptors at their owning
  Components rather than in a separate descriptor registry. Never derive an
  address from class names, `toString`, route IDs or live-instance identity. The descriptor is metadata; it does not belong in State
  or Router payloads or in DI.
- **Wiring.** The owning Component calls the protected `restoreState(DRAFT)`
  inside the existing `retainedInstance` factory and passes the typed
  `initialState` to a fresh VM. It calls `saveState(DRAFT) { viewModel.state.value }`
  once during construction. That call registers a lazy local supplier (the lambda
  the shell invokes when it saves); the shell's `StateKeeper.save` creates the
  snapshot. Use these helpers rather than raw draft `StateKeeper.consume`/`register`
  calls. The descriptor needs no aliases, extensions or DI. Child
  `ComponentContext`s isolate identical descriptors for separate live owners. A
  data-backed form may accept a nullable `initialState` to distinguish an absent
  archive from a saved snapshot, including an all-default snapshot.
  `BaseViewModel` still receives a complete non-null state. Evaluate dynamic
  fresh defaults once.
- **Parent and child.** Restore accepted parent-flow data before constructing the
  children that use it. Pass already accepted parent values to a fresh child with
  no local archive as ordinary business initial inputs. Restoration precedence:
  the retained VM, then the local saved draft, then accepted parent/route inputs,
  then defaults. Parent state keeps accepted business values. Do not move the
  unfinished drafts of destroyed children into parent or global state for this
  mechanism. Each owner saves its own draft; add no extra callback
  synchronization, provider, public extension, generic `BaseViewModel`
  persistence or per-edit route replacement. Edits and saves keep the current VM
  and screen presentation unchanged.
- **Serialization.** Use the existing `@Serializable` state when it represents
  the draft cleanly. Mark rebuilt and runtime fields `@Transient` with safe
  defaults. Reuse `ImmutableListSerializer` and the existing `FormatState` types.
  Enable the owning module's serialization plugin.
- **Defaults.** Saved fields need stable defaults. A date-dependent default can be
  omitted during encoding and then change on restoration. Apply dynamic initial
  values only when no saved draft exists.
- **Changing saved fields** follows
  [compatibility bumps](../platform-build-toolkit/references/release.md#compatibility-bumps).
- **Rebuilt data.** Rebuild catalogs, localized presentation and provider
  availability from their current sources. Preserve catalog selection intent
  across an initial empty emission. Saved known/selected or excluded IDs
  distinguish deselected items from new items.
- **Secrets.** Passwords, authentication tokens, provider credentials/codes and
  other secrets MUST NOT enter StateKeeper archives or route payloads.
  Serializable type support does not authorize persistence. Verify that the
  serialized payload omits those fields and that secret inputs are empty or
  default after fresh restoration. Normal retained memory does not need to be
  cleared by this mechanism.
- **Runtime state.** Loaders, jobs, dialog/error sessions, callbacks, focus and
  telemetry provenance (the Initial/Restore reason and similar reporting history
  held by `ScreenAnalytics`) are runtime state. Do not add them to business
  snapshots. Presentation-only widget state has its own UI policy and is not a
  business draft.
- **Restoration side effects.** Restoration initializes business state directly.
  It does not replay input/control callbacks, emit synthetic analytics,
  auto-submit authentication or serialize jobs. Restarting existing startup or
  completion operations remains business-owned. Verify cancellation and the
  current operation policy without inventing retry machinery.
- **Platform limits.** StateKeeper restores supplied archives; it does not
  guarantee every cold start. Android supplies saved state through
  `retainedComponent`. iOS restores one validated background checkpoint before
  constructing the root, then keeps that root live through foreground
  transitions. Abrupt active termination before a completed checkpoint is not
  guaranteed. Feature keys and local ownership stay unchanged. Add no feature
  storage or lifecycle bridge. See the reference's
  [iOS policy](../ui-feature/references/process-death.md#ios-specifics), including
  the build check that discards another build's checkpoint. Dialog sessions stay
  unsaved. Accepted results belong to the owning screen's draft.

Inspect [PersistenceKey](../../../ui-core/foundation/src/commonMain/kotlin/com/grippo/core/foundation/models/PersistenceKey.kt)
for the descriptor and [TrainingsComponent](../../../ui-screen-features/trainings/src/commonMain/kotlin/com/grippo/trainings/trainings/TrainingsComponent.kt),
[TrainingsViewModel](../../../ui-screen-features/trainings/src/commonMain/kotlin/com/grippo/trainings/trainings/TrainingsViewModel.kt)
and [TrainingsState](../../../ui-screen-features/trainings/src/commonMain/kotlin/com/grippo/trainings/trainings/TrainingsState.kt)
for a nullable initial-state boundary with rebuilt fields. Discover affected
owners from the source rather than using a fixed Component/State census.

## Initialization from persistent settings

A value that must outlive the screen or the session ("remember for next time")
is a `LocalSettingsFeature` preference owned by the data layer. It is
account-scoped unless the request says otherwise. StateKeeper drafts only bridge
recreation and supported restoration. Time anchors such as start or end instants
are business values; preserve them like any other draft field. The ticking text
derived from them is rebuilt in the Screen.

A selection already committed to an existing durable settings owner need not gain
an additional StateKeeper copy solely for restoration. Record that source in the
inventory. This does not replace local preservation of unfinished screen drafts.

Keep an unfinished selection distinct from the committed source when both exist.
Persist unfinished input with the screen draft and rebuild the committed source.
Do not let a late source emission erase the input, and do not let an old draft
overwrite a known decision. Controls that commit each accepted change need no
separate unfinished draft; analytics consent follows its
[collection policy](../feature-analytics/references/collection-and-core.md#collection-and-consent).
Choose the smallest state representation that preserves these meanings; this
does not mandate two fields for every settings control.

When restored settings arrive asynchronously, review the interval before their
first value as well as the loaded state. Dependent requests MUST NOT use a
fallback as though it were the restored selection. Apply the fallback once
absence is known. Dependent controls either wait for that value or preserve
permitted early edits under the existing draft policy. Inspect the actual render
conditions and callback eligibility, not only the subscription. Reuse meaningful
State and the existing loading or eligibility mechanisms. Do not add restoration
flags or block unrelated controls.

## Data-backed form synchronization

The current State owns the draft while the screen is open. Populate a fresh form
once from its business source. A restored snapshot keeps its saved values,
including Empty, null and default values. Use the existing `initialState`
boundary (the constructor argument: absent archive versus restored snapshot) to
choose the initialization, without storing another mode or source snapshot.
Preserve nondefault values entered before the first source arrives. A choice
equal to a fresh default carries no separate edit history, so it may receive
initial source data. A draft that starts on an explicit user action (for example
an Edit tap) seeds from the current State at that action. It needs no
first-emission seeding.

ViewModels MUST NOT keep private runtime flags, previous-source caches or shadow
snapshots (a private copy of earlier data kept to compare against later) for
restoration, edit detection or synchronization. This includes `restorePending`,
`previousSource` and `previousSourceSelection`, regardless of type. Do not move
equivalent flags or copies into State, a wrapper, a provider or a mutable closure.
Persist actual values without `hasEdited`/`selectionEdited`/`dirty` metadata. Use
State and `update` for feature data, and derive conditions from existing values.

Which business source seeds a field (for example the profile record or the
latest history entry) is a product decision recorded in the feature's coverage
file; a fresh form seeds once from that source's first non-null emission.
Later source observations rebuild the actual display data and catalogs without
resynchronizing form inputs. Both fresh and restored drafts may therefore remain
older than server values while open. Do not reconstruct field provenance (a
record of whether each field came from the user or from the source), and do not
add automatic merge machinery. A one-shot initialization does not restart on
ordinary pause/resume. Use the existing VM-owned `safeLaunch`, or
`Processing.Infinity` for a finite Flow, rather than a lifecycle-repeated
subscription.

Catalog known/selected IDs are business identity, not edit flags. Keep them where
they are required to distinguish deselected known items from new items and to
preserve accepted parent exclusions. Preserve choices for known IDs. Seed new IDs
from the current business source. Discard IDs absent from a nonempty current
catalog. Empty catalog emissions do not erase the draft. Verify first-load edits,
restored defaults, pause/resume, changed sources, empty catalogs and final
requests.

## Self-review

Review the actual source and inventory, fix findings, then inspect the resulting diff
again. Leave the inventory rows in `analytics/<feature>/COVERAGE.md` matching the
final code. Report the fields preserved, sources rebuilt, secret exclusions, permitted
checks and unverified restoration scenarios in the reply, without claiming runtime
proof from source.
