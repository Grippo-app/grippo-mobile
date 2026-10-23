# Mapper signatures and representation

Files follow existing slash-separated packages in `data-mappers/<direction>/src/commonMain/kotlin/com/grippo/`.
Name the function for its target and expose nullability honestly. Inspect the area's
source for its established name: equipment uses `toState`, `toDomain`, `toEntities`
and `toEntityOrNull`; request-model mappings use `toBody`. Reuse the name the area
already uses before adding a new one.

- Non-null input does not guarantee non-null target: unknown enum keys, missing relations or
  invalid date conversion may require null-and-drop behavior. Inspect actual constructors/parsers.
- Conversions compose inside one directional module: a collection variant calls its item variant,
  a parent mapper calls the child mappers of the same direction. Cross-direction mapping is
  composed by the Repository/ViewModel, never via mapper-module imports.
- Formatting/unit representation and presentation ordering may translate meaning without deciding
  business outcomes. Aggregation/calculation, validation and identity/time creation belong outside.
- Persist exact wire enum keys; unknown closed values drop according to the established mapping
  contract. Do not create a fallback enum solely to hide backend drift.
- Domain->DTO nullable fields reflect already-composed valid requests; the mapper does not invent
  defaults for required backend values. Every DTO field retains its explicit unchanged SerialName;
  whether a `null` is sent or omitted is decided by the DTO's `@EncodeDefault` annotation
  ([DTO/API](../../data-layer/references/dtos-and-api.md#omitted-versus-explicit-null)).
- Draft mapping receives already-created parent/child IDs and timestamps from the owner; passing an
  existing identity must not create a new record during restoration.
