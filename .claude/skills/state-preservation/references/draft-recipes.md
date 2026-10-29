# Draft wiring and inventory recipes

Read when changing Component draft wiring, nullable initial-state boundaries or
restoration checks. Apply the [preservation gate](../SKILL.md#mandatory-preservation-gate)
and its [ownership invariants](../SKILL.md#ownership-and-implementation).
These recipes explain that contract; they add no second preservation mechanism.

## Component wiring

Inspect [PersistenceKey](../../../../ui-core/foundation/src/commonMain/kotlin/com/grippo/core/foundation/models/PersistenceKey.kt),
[BaseComponent](../../../../ui-core/foundation/src/commonMain/kotlin/com/grippo/core/foundation/BaseComponent.kt)
and the affected owner rather than copying a fixed Component census.

1. Put the local `DRAFT` descriptor at the owning Component. It is metadata,
   without aliases, extensions or DI. Child ComponentContexts isolate identical
   descriptors for separate live owners.
2. In its normal `retainedInstance` factory, restore the draft and pass it through
   `initialState`. A nullable argument preserves the distinction between an absent
   archive and a saved all-default snapshot; VM base state remains non-null.
3. Register the Component's lazy `saveState` supplier once. The supplier reads
   current VM State when the shell calls `StateKeeper.save`; registration itself
   creates no snapshot and replaces no live VM or screen presentation.
4. Inspect serialization and rebuilt fields together. Keep the route's seed as
   fresh-state input, not a replacement for the archived draft. Use the existing
   state/formatter serializers and module serialization plugin.

The current [TrainingsComponent](../../../../ui-screen-features/trainings/src/commonMain/kotlin/com/grippo/trainings/trainings/TrainingsComponent.kt),
[TrainingsViewModel](../../../../ui-screen-features/trainings/src/commonMain/kotlin/com/grippo/trainings/trainings/TrainingsViewModel.kt)
and [TrainingsState](../../../../ui-screen-features/trainings/src/commonMain/kotlin/com/grippo/trainings/trainings/TrainingsState.kt)
show a nullable initial-state boundary with rebuilt fields. Re-inspect before
copying: saved state wins over `TrainingsState.of(day)`, and `selectRange` rebuilds
range-derived presentation from the chosen range.

## Inventory example

These rows describe the current trainings calendar, not a global field census.
Keep the actual feature inventory in `analytics/<feature>/COVERAGE.md` under
[records](../../../../CLAUDE.md#records).

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| TrainingsComponent / TrainingsViewModel | `date` (selected range) and `period` (Daily/Monthly) | Preserve | Component-owned `DRAFT` with address `TrainingsDraft` and `TrainingsState.serializer()` | Component passes `restoreState(DRAFT)` and the route's `day`; for a null archive the ViewModel applies the route seed (Daily on `TrainingsRouter.Trainings.day` through `TrainingsState.selectDay`, coerced into `limitations`) or otherwise the current-month Monthly default, and a saved draft wins over that seed; it rebuilds range-derived presentation through `selectRange`; check the restored month and mode on a fresh Component |
| TrainingsViewModel | `limitations`, monthly `weeks`/`digest`, daily `items` | Rebuild | `@Transient`; `DateTimeUtils.trailingYear()` and the `observeTrainings` flow | Recomputed from the restored range; never in the archive |
| TrainingsViewModel | `TrainingsLoader.Trainings` and the range refresh step | Runtime only | `loaders` flow; `withLoader` inside the `mapLatest` refresh chain | Not saved; the refresh runs again for the restored range |

## One-shot form initialization

Apply [data-backed synchronization](../SKILL.md#data-backed-form-synchronization)
before selecting an existing flow operator. A fresh form's initial source read is
VM-owned: use existing `safeLaunch`, or `Processing.Infinity` for a finite Flow,
rather than a lifecycle-repeated subscription. Later observation updates display
data and catalogs without re-seeding inputs. An Edit action instead seeds from
current State at that action, with no first-emission initialization.

Review the actual `WhileActive` disconnect grace when checking pause/resume; a
subscription may remain connected briefly after leaving RESUMED. Exercise the
full fresh-owner sequence from the gate, not just a quick pause or a retained VM.
Platform archive delivery and iOS checkpoint limits remain owned by
[process-death restoration](../../ui-feature/references/process-death.md).
