# Mapping Conventions

Every mapper module follows the same shape. This document describes the function naming, signature conventions, and standard patterns.

## Function names

| Direction | Function form | Plural form |
|---|---|---|
| `Source.toEntity()` / `Source.toEntityOrNull()` | one source → one entity | `List<Source>.toEntities()` |
| `Source.toDomain()` / `Source.toDomainOrNull()` | one source → one domain | `List<Source>.toDomain()` |
| `Source.toState()` | one source → one state | `List<Source>.toState()` |
| `Source.toBody()` | one source → one request body | `List<Source>.toBody()` (same name, overloaded on receiver) |

Top-level extension functions. **No classes. No `@Single`. No DI.** Stateless.

```kotlin
public fun TrainingResponse.toEntityOrNull(): TrainingEntity? { ... }
public fun List<TrainingResponse>.toEntities(): List<TrainingEntity> = mapNotNull { it.toEntityOrNull() }

public fun TrainingPack.toDomain(): Training { ... }
public fun List<TrainingPack>.toDomain(): List<Training> = map { it.toDomain() }

public fun Training.toState(): TrainingState { ... }
public fun List<Training>.toState(): PersistentList<TrainingState> { ... }

// Body mappers source from the "submit" domain variant (Set<X>, Draft<X>), not the read variant.
public fun SetTraining.toBody(): TrainingBody { ... }
public fun List<SetExercise>.toBody(): List<ExerciseBody> = map { it.toBody() }
```

## Nullable vs non-null variants

- `toEntity()` / `toDomain()`: returns a non-null result. Use when **all required fields** are guaranteed non-null on the input.
- `toEntityOrNull()` / `toDomainOrNull()`: returns nullable. Use when the input has nullable fields (DTOs always do, and entity Packs frequently do — see "Entity → Domain" below) and a missing required field means "skip this row".

Plural variants:

- `List<Source>.toEntities()` typically uses `mapNotNull { it.toEntityOrNull() }`.
- `List<Source>.toDomain()` for Entity/Pack sources uses `map { it.toDomain() }` when the per-row mapper is non-null, or `mapNotNull { it.toDomain() }` when the per-row mapper returns nullable (e.g. `ExercisePack.toDomain(): Exercise?` because the embedded `example` relation may be absent).

## DTO → Entity canonical pattern

```kotlin
public fun TrainingResponse.toEntityOrNull(): TrainingEntity? {
    val entityId = AppLogger.Mapping.log(id) { "TrainingResponse.id is null" } ?: return null
    val entityDuration = AppLogger.Mapping.log(duration) { "TrainingResponse.duration is null" } ?: return null
    val entityCreatedAt = AppLogger.Mapping.log(createdAt) { "TrainingResponse.createdAt is null" } ?: return null
    val entityVolume = AppLogger.Mapping.log(volume) { "TrainingResponse.volume is null" } ?: return null
    val entityRepetitions = AppLogger.Mapping.log(repetitions) { "TrainingResponse.repetitions is null" } ?: return null
    val entityIntensity = AppLogger.Mapping.log(intensity) { "TrainingResponse.intensity is null" } ?: return null
    val entityUpdatedAt = AppLogger.Mapping.log(updatedAt) { "TrainingResponse.updatedAt is null" } ?: return null
    val entityProfileId = AppLogger.Mapping.log(profileId ?: userId) { "TrainingResponse.profileId is null" } ?: return null

    return TrainingEntity(
        id = entityId,
        profileId = entityProfileId,
        duration = entityDuration,
        createdAt = entityCreatedAt,
        volume = entityVolume,
        repetitions = entityRepetitions,
        intensity = entityIntensity,
        updatedAt = entityUpdatedAt,
    )
}
```

Rules:

1. **Every required field uses `AppLogger.Mapping.log(value) { msg } ?: return null`.** If the field is null, log it (so the team can diagnose backend regressions from the rolling log) and skip the row.
2. **The default stance is "required".** If an entity column is non-null, the mapper requires it — drop the row when missing rather than fabricating a value. Use an Elvis fallback **only** when the entity field itself is nullable (e.g. iteration weight columns) or when the server emits a deprecated alias that must be coalesced (`profileId ?: userId`).
3. **No business logic.** Just field translation.
4. **One `return <X>Entity(...)`** at the bottom. No intermediate `var entity = <X>Entity(...)`.
5. **Local variable names are prefixed** (`entityId`, `entityDuration`) to avoid shadowing same-named DTO fields inside the receiver scope.

## Entity → Domain canonical pattern

Scalar entity columns are non-null by contract (the dto-to-entity step already validated). Two cases still call for `AppLogger.Mapping.log`:

- An embedded `@Relation` is missing at read time (e.g. `ExercisePack.example` is null because the parent example row was deleted out from under the child) — the row is unusable, drop it.
- A column stores a string-encoded enum and the value doesn't parse (DB pre-dates a renamed enum case).

```kotlin
public fun TrainingPack.toDomain(): Training = Training(
    id = training.id,
    duration = training.duration.minutes,                          // Long (minutes) → Duration
    createdAt = DateTimeUtils.toLocalDateTime(training.createdAt), // String → LocalDateTime
    volume = training.volume,
    repetitions = training.repetitions,
    intensity = training.intensity,
    exercises = exercises.toDomain(),                              // List<ExercisePack> → List<Exercise>
)

public fun List<TrainingPack>.toDomain(): List<Training> = map { it.toDomain() }

// Relation may be missing — nullable result, plural variant uses mapNotNull.
public fun ExercisePack.toDomain(): Exercise? {
    val mappedExample = AppLogger.Mapping.log(example?.toDomain()) {
        "ExercisePack exercise by ${exercise.exerciseExampleId} is null"
    } ?: return null

    return Exercise(
        id = exercise.id,
        name = exercise.name,
        iterations = iterations.toDomain(),
        volume = exercise.volume,
        repetitions = exercise.repetitions,
        intensity = exercise.intensity,
        createdAt = DateTimeUtils.toLocalDateTime(exercise.createdAt),
        exerciseExample = mappedExample,
    )
}

public fun List<ExercisePack>.toDomain(): List<Exercise> = mapNotNull { it.toDomain() }
```

Rules:

1. **Scalar columns: no null checks.** Entities have non-null scalar columns by design — read them directly.
2. **Embedded relations: nullable.** When the per-row mapper consumes a `@Relation` field, return `T?` and use `AppLogger.Mapping.log` to drop unusable rows.
3. **Type translation happens here.** `Long` (minutes — see TrainingEntity) → `Duration`; `String` (ISO-8601) → `LocalDateTime`.
4. **Nested Packs → nested domain.** `ExercisePack.toDomain()` is called from `TrainingPack.toDomain()`.

## Domain → DTO Body

```kotlin
public fun SetTraining.toBody(): TrainingBody = TrainingBody(
    repetitions = repetitions,
    duration = duration.inWholeMinutes,
    intensity = intensity,
    volume = volume,
    exercises = exercises.toBody(),
)

public fun List<SetExercise>.toBody(): List<ExerciseBody> = map { it.toBody() }

public fun SetExercise.toBody(): ExerciseBody = ExerciseBody(
    repetitions = repetitions,
    intensity = intensity,
    volume = volume,
    exerciseExampleId = exerciseExample.id,
    iterations = iterations.toBody(),
    name = name,
)
```

Notes:

- Source type is the **submit variant** (`SetTraining`, `SetExercise`, `SetIteration`), not the read variant — body mappers convert what the form has just produced.
- The plural function name is `toBody()` (overloaded on receiver), not `toBodies()`.
- Type translation reverses: `Duration` → `Long` (minutes, matching the entity unit); `LocalDateTime` → ISO-8601 UTC string for direction-specific bodies that carry timestamps. `TrainingBody` itself has no timestamp.

## Domain → State

```kotlin
public fun Training.toState(): TrainingState = TrainingState(
    id = id,
    exercises = exercises.toState(),
    total = TrainingTotalState(
        volume = VolumeFormatState.of(volume),
        repetitions = RepetitionsFormatState.of(repetitions),
        intensity = IntensityFormatState.of(intensity),
    ),
    duration = DurationFormatState.of(duration),
    createdAt = DateTimeFormatState.of(
        value = createdAt,
        range = DateRangePresets.infinity(),
        format = DateFormat.DateOnly.DateMmmDdYyyy,
    ),
)

public fun List<Training>.toState(): PersistentList<TrainingState> =
    map { it.toState() }.toPersistentList()
```

Rules:

1. **Numerics become `*FormatState`** (`VolumeFormatState.of(...)`, `RepetitionsFormatState.of(...)`, `IntensityFormatState.of(...)`, `WeightFormatState.of(...)`).
2. **Durations become `DurationFormatState.of(...)`**, not raw formatted strings.
3. **Dates become `DateTimeFormatState.of(value, range, format)`** — never raw `LocalDateTime` and never an eagerly-formatted `String`. The `range` + `format` carry enough context for the UI to re-render on locale change.
4. **Collections become `PersistentList<XState>`** via `.toPersistentList()`. `PersistentList` is the concrete type used everywhere; consumers can still up-cast to `ImmutableList` where needed.
5. **No `UiText` here.** Mappers don't construct localized strings — that happens in the ViewModel (`stringProvider.get(...)`) or the screen (`AppTokens.strings.res(...)`).
6. **Per-row state** for list items lives in the same file: `Training.toState()` and `List<Training>.toState()`.

## State → Domain

```kotlin
public fun ExerciseState.toDomain(): SetExercise? {
    val mappedRepetitions = AppLogger.Mapping.log(total.repetitions.value) {
        "ExerciseState total.repetitions value is null (id=$id)"
    } ?: return null

    val mappedVolume = AppLogger.Mapping.log(total.volume.value) {
        "ExerciseState total.volume value is null (id=$id)"
    } ?: return null

    val mappedIntensity = AppLogger.Mapping.log(total.intensity.value) {
        "ExerciseState total.intensity value is null (id=$id)"
    } ?: return null

    val mappedCreatedAt = AppLogger.Mapping.log(createdAt.value) {
        "ExerciseState createdAt value is null (id=$id)"
    } ?: return null

    return SetExercise(
        name = name,
        iterations = iterations.toDomain(),
        exerciseExample = exerciseExample.toDomain(),
        repetitions = mappedRepetitions,
        volume = mappedVolume,
        intensity = mappedIntensity,
        createdAt = mappedCreatedAt,
    )
}

public fun List<ExerciseState>.toDomain(): List<SetExercise> =
    mapNotNull { it.toDomain() }.toPersistentList()
```

Rules:

1. **Returns `Set<X>?`** — incomplete form returns null. The ViewModel decides whether to submit. The target type is the **submit variant** (`SetExercise`, `SetIteration`, …), parallel to the body mappers.
2. **Use `AppLogger.Mapping.log(state.x.value) ?: return null`** for required values, same pattern as DTO-sourced mappers. `*FormatState.value` is the nullable underlying value (e.g. `WeightFormatState.Valid.value` vs `Empty.value = null`).
3. **Optional `*FormatState.value` passes through as nullable** if the domain field is nullable (e.g. iteration weights).
4. **No `name.trim().ifBlank { return null }`-style validation** — the `*FormatState` types already encode validity; if a field is invalid, its `.value` is null and the standard `AppLogger.Mapping.log ?: return null` step drops the record.
5. **Pure field translation only.** Volume/intensity aggregation is computed when constructing the State (e.g. in the ViewModel from iteration values), not inside the State → Domain mapper.

## `AppLogger.Mapping` helper

```kotlin
public object AppLogger {
    public object Mapping {
        public fun <T> log(value: T?, msg: () -> String): T? {
            if (value != null) return value
            val location = getCallerLocation()
            present(LogCategory.MAPPING, "${msg()} $location")
            return null
        }
    }
}
```

Returns the input value verbatim. Side effect: when null, invokes `msg()` (lazy), appends caller `(file:line)`, and writes one `[MAPPING]` line to the rolling file log. Pair it with `?: return null` at every call site — never `!!`.

## File and module layout

```
data-mappers/dto-to-entity/src/commonMain/kotlin/com/<org>/<product>/dto/entity/
  training/
    TrainingMapper.kt           # TrainingResponse.toEntityOrNull() + List variant
    ExerciseMapper.kt           # ExerciseResponse.toEntityOrNull() + List variant
    IterationMapper.kt          # IterationResponse.toEntityOrNull() + List variant
  user/
    UserMapper.kt
    GoalMapper.kt
    WeightHistoryMapper.kt
  equipment/
    EquipmentMapper.kt
    EquipmentGroupMapper.kt
```

One file per source DTO. The repository pulls fields out of the parent DTO and calls each leaf mapper independently (e.g. `dto.exercises.toEntities()`, `dto.exercises.flatMap { it.iterations }.toEntities()`), so each level lives in its own file.

## Anti-patterns

- **`!!`** on a nullable field. Forbidden. Use `?: return null` (DTO sources) or `?: error("...")` (entity sources, where null is a bug).
- **Validation inside a mapper.** Throws are wrong here; either drop the row (`?: return null`) or compute a domain-meaningful default.
- **Caching computed fields.** Mappers are stateless; cache results in the consumer if needed.
- **Returning `T` from a DTO mapper.** DTOs are nullable; use `T?` and `toXOrNull`.
- **Mapper depending on another mapper module.** Each module is isolated.
- **`@Composable` mappers.** Mappers don't know Compose. UI formatting that needs `@Composable` (e.g. `UiText.text()`) is called by the **consumer**.
- **Logging from the consumer**. Logging is `AppLogger.Mapping`'s job — it's part of the canonical pattern.
- **Conversion in the Repository or ViewModel.** Always lives in a mapper module. The cost is one function call; the win is consistency.
