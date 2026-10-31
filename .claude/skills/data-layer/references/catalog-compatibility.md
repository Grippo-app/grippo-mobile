# Equipment and muscle catalog compatibility

This is the catalog-specific exception to [operation outcomes](operation-outcomes.md).
Read it for equipment/muscle refresh, startup/session ordering, exercise-example
hydration and exclusion work. Preserve these rollout and availability decisions.

The equipment and muscle catalogs (`EquipmentRepositoryImpl.getEquipments`,
`MuscleRepositoryImpl.getMuscles`) store group and item rows whose `type` key this app
version does not know, so the catalog refresh keeps succeeding when the
server adds a type, and exercise-example snapshots written after that refresh find their
foreign-key parents. The entity-to-domain mappers hide those rows; an unknown group type
hides the whole group from the catalog projection, including its items of known types. Missing fields,
duplicate IDs and wrong parents still make that response incomplete. For equipment, store
and TestFlight builds without this exception and the user-visible effects of hiding are
described in the [equipment integration map](../../add-equipment/references/integration-map.md#mobile-touchpoints).
For muscles: a hidden muscle drops out of exercise-example bundles, so an example's load is
shared among its visible muscles; the user cannot select or exclude it; for a muscle whose
own type is unknown the example filters also ignore an exclusion made from a newer build,
while a known-type muscle hidden with its unknown group keeps its exclusion; and saving the
muscle selection (`ProfileMusclesViewModel.onApply`) removes a hidden muscle's exclusion
because the backend replaces the whole set, an accepted rollout risk. Builds that reject an unknown muscle type keep retrying
Splash, and builds that refresh catalogs only before sign-in fail a signed-in user's
exercise-example refresh on the missing bundle parent, so a new muscle type needs the same
compatibility decision as a new equipment type.
Splash requests no catalog. Login and Register refresh muscles, then equipment, before the
credential request, so a stored session always has a local catalog and the excluded-set
writes in their preparation find their foreign-key parents; Home's startup operation
refreshes muscles, equipment, then the exercise examples. Other writers that depend on those
parents run without their own catalog refresh: the Completed screen's and Debug's
exercise-example refresh and the profile muscle/equipment exclusion loads and saves. When the
server added a muscle or equipment row after the last successful Home catalog refresh (for
example in a process restored straight into another route, or after Home's popup was closed),
such a write fails on the missing parent (the generic error dialog and a crash report; Retry
repeats it) until a later Home instance's catalog refresh succeeds. This is an accepted risk:
the owner adds catalog reloads where they are needed.
