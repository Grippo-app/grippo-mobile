# Mappers — reference index

Self-contained reference pack for mapper work. These files carry the skill's own
normative rules — it reads no external rule docs at runtime.

## Routing table

| Task kind | Read first |
|---|---|
| Add a mapper (full recipe, directional table, by-direction patterns) | [`cookbook-add-mapper.md`](cookbook-add-mapper.md) |
| Seven directions / module layout / package naming / build files | [`mapper-directions.md`](mapper-directions.md) |
| When to add a mapper / when to update DTO·Entity·Domain shapes | [`mapper-directions.md`](mapper-directions.md) § "When to add a new mapper" / "When to update …" |
| Function naming / signature conventions (`OrNull`, plural, nullable receiver) | [`mapping-conventions.md`](mapping-conventions.md) § "Function names" / "Nullable vs non-null variants" |
| DTO → Entity canonical pattern | [`mapping-conventions.md`](mapping-conventions.md) § "DTO → Entity"; [`null-safety-and-logging.md`](null-safety-and-logging.md) |
| Entity → Domain (scalars vs relations / enum parse) | [`mapping-conventions.md`](mapping-conventions.md) § "Entity → Domain"; [`null-safety-and-logging.md`](null-safety-and-logging.md) § "Entity → Domain" |
| DTO → Domain (no entity step) | [`null-safety-and-logging.md`](null-safety-and-logging.md) § "DTO → Domain (no entity step)" |
| Domain → State (`*FormatState`, `PersistentList`) | [`mapping-conventions.md`](mapping-conventions.md) § "Domain → State" |
| State → Domain (submit variant, `*FormatState.value`) | [`mapping-conventions.md`](mapping-conventions.md) § "State → Domain" |
| Domain → Entity (drafts, client ids, parent FK) | [`mapping-conventions.md`](mapping-conventions.md) § "Domain → Entity (drafts)"; [`null-safety-and-logging.md`](null-safety-and-logging.md) (drafts exception) |
| Domain → DTO Body (reverse type translation, `.key`) | [`mapping-conventions.md`](mapping-conventions.md) § "Domain → DTO Body" |
| Enum dictionaries / sealed-type round-trip (`String ↔ Enum`) | [`mapping-conventions.md`](mapping-conventions.md) § "Enum dictionaries" |
| Null-safety policy (required vs optional, log-and-drop) | [`null-safety-and-logging.md`](null-safety-and-logging.md) |
| `AppLogger.Mapping.log` — the bridge + what it produces | [`null-safety-and-logging.md`](null-safety-and-logging.md) § "AppLogger.Mapping.log — the bridge" / "What … produces" |
| Composing nested mappers / parent-scoped FK carve-out | [`null-safety-and-logging.md`](null-safety-and-logging.md) § "Composing nested mappers" |
| Mapper layer in the data flow | [`mapper-directions.md`](mapper-directions.md) § "Mapper layer in the data flow" |
| Anti-pattern: mapper-to-mapper import | [`mapper-directions.md`](mapper-directions.md) § "Anti-pattern: mapper-to-mapper import" |
| Anti-patterns (per-direction) | [`mapper-directions.md`](mapper-directions.md), [`mapping-conventions.md`](mapping-conventions.md), [`null-safety-and-logging.md`](null-safety-and-logging.md) — each § "Anti-patterns" |
