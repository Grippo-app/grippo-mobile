# Mapper signatures and representation

Files follow existing slash-separated packages in `data-mappers/<direction>/src/commonMain/kotlin/com/grippo/`.
Use `toEntityOrNull`, `toDomainOrNull`, `toState`, `toBody` and collection variants consistent with
the actual area. Name the function for its target and expose nullability honestly.

- Non-null input does not guarantee non-null target: unknown enum keys, missing relations or
  invalid date conversion may require null-and-drop behavior. Inspect actual constructors/parsers.
- Conversions compose within one directional module where appropriate. Cross-direction mapping
  is composed by the Repository/ViewModel, never via mapper-module imports.
- Formatting/unit representation and presentation ordering may translate meaning without deciding
  business outcomes. Aggregation/calculation, validation and identity/time creation belong outside.
- Persist exact wire enum keys; unknown closed values drop according to the established mapping
  contract. Do not create a fallback enum solely to hide backend drift.
- Domain→DTO nullable fields reflect already-composed valid requests; the mapper does not invent
  defaults for required backend values. Every DTO field retains explicit unchanged SerialName.

For an affected ID/time-generating mapper, inspect the caller's creation/update semantics before
moving generation. Passing an existing identity must not create a new record during restoration.
