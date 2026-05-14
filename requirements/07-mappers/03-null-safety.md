# Null Safety in Mappers

DTOs are entirely nullable. Entity scalar columns and domain models are non-null by default. The mappers are where the gap is bridged. The convention is:

> If a **required** field (target is non-null) is null on the source, log it and drop the record. If an **optional** field (target is nullable, or there's an equivalent server alias) is null, pass through or coalesce — do not invent a default for required columns.

## `AppLogger.Mapping.log` — the bridge

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

Behavior:

- If `value != null`: returns it unchanged.
- If `value == null`: resolves caller `(file:line)`, invokes `msg()` (lazy), writes one `[MAPPING] $msg (file:line)` entry to the file log, returns `null`.

Usage:

```kotlin
val entityId = AppLogger.Mapping.log(dto.id) { "TrainingResponse.id is null" } ?: return null
```

The `?: return null` is mandatory after every required-field log. The pattern says "this value is required; if missing, drop the record". Use a prefixed local name (`entityId`, `domainId`, `mappedId`) to avoid shadowing same-named DTO/state fields inside the receiver scope.

## What is required vs optional

A field is **required** when:

- The entity/domain model has it as a non-null field. **This is the default stance** — if the target column or property is non-null, the source must supply a value or the row is dropped.
- It is part of the primary key or a foreign-key.
- It is needed to construct any further objects (e.g. a missing `id` means the row is uninsertable).

A field is **optional** when:

- **The target field itself is nullable** (e.g. `IterationEntity.externalWeight: Float?` — weight columns are nullable, so the mapper just passes `externalWeight` through).
- It has a **server-emitted alias** that can satisfy it (`profileId ?: userId` — the response carries a legacy `userId` for backward compatibility).

Do **not** fabricate defaults like `volume ?: 0f` for non-null target columns just because the value might be missing — log it and drop the row. Volume, intensity, repetitions, updatedAt are all required in the reference mappers.

## Required field: `?: return null`

```kotlin
val entityId = AppLogger.Mapping.log(id) { "TrainingResponse.id is null" } ?: return null
val entityProfileId = AppLogger.Mapping.log(profileId ?: userId) { "TrainingResponse.profileId is null" } ?: return null
val entityCreatedAt = AppLogger.Mapping.log(createdAt) { "TrainingResponse.createdAt is null" } ?: return null
```

Each required field is checked **separately** with a **specific** log message. This lets the team diagnose **which** field went missing — not just "some field".

## Optional field: pass-through or alias coalesce

```kotlin
// Entity column itself is nullable — pass DTO value through unchanged.
externalWeight = externalWeight,
extraWeight = extraWeight,
assistWeight = assistWeight,
bodyWeight = bodyWeight,
bodyMultiplier = bodyMultiplier,

// Two equivalent server fields, take whichever is present.
val entityProfileId = AppLogger.Mapping.log(profileId ?: userId) {
    "TrainingResponse.profileId is null"
} ?: return null
```

Rules:

- If the entity field is nullable, just write `entityField = sourceField` — Kotlin already permits the null.
- If the server provides two equivalent fields (legacy alias migration), use `a ?: b` inside the `AppLogger.Mapping.log(...)` call.
- Do not invent numeric or string defaults for required columns.

## Composing nested mappers

Each level is an independent leaf mapper — the parent ID lives in the DTO of the child, so the child reads it from itself:

```kotlin
public fun TrainingResponse.toEntityOrNull(): TrainingEntity? { /* ... */ }

public fun ExerciseResponse.toEntityOrNull(): ExerciseEntity? {
    val entityId = AppLogger.Mapping.log(id) { "ExerciseResponse.id is null" } ?: return null
    val entityTrainingId = AppLogger.Mapping.log(trainingId) { "ExerciseResponse.trainingId is null" } ?: return null
    // ...
    return ExerciseEntity(id = entityId, trainingId = entityTrainingId, /* ... */)
}

// Repository
val training = dto.toEntityOrNull() ?: return null
val exercises = dto.exercises.toEntities()
val iterations = dto.exercises.flatMap { it.iterations }.toEntities()
trainingDao.insertOrReplace(training, exercises, iterations)
```

The Repository flat-maps children out of the parent DTO and feeds them to leaf `toEntities()` calls. Each child carries its own parent FK (`trainingId`, `exerciseId`) from the wire payload, so it logs and drops on its own without taking a `parentId` parameter.

## What `AppLogger.Mapping` produces

A file log entry like:

```
[MAPPING] TrainingResponse.id is null (TrainingMapper.kt:12)
```

The file log is written through `:toolkit:logger`'s `LogDispatcher` to a rolling file (Android: app cache dir; iOS: `NSTemporaryDirectory()`). Surfaced in the debug screen via `AppLogger.logFileContentsByCategory()` (returns `Map<String, List<String>>` keyed by category — `MAPPING`, `NETWORK`, `NAVIGATION`, `ERROR`, `WARNING`).

When the team sees an unexpected drop ("user's trainings list is shorter than expected"), the log entries pinpoint the missing field. Often the cause is a backend regression — a field that used to be always set is sometimes null now.

## DTO → Domain (no entity step)

Same rules as DTO → Entity. The example below is from a one-shot exercise read (the `ExerciseExample` is passed in by the caller because the wire payload doesn't include it):

```kotlin
public fun ExerciseResponse.toDomainOrNull(example: ExerciseExampleValue): Exercise? {
    val domainId = AppLogger.Mapping.log(id) { "ExerciseResponse.id is null" } ?: return null
    val domainName = AppLogger.Mapping.log(name) { "ExerciseResponse.name is null" } ?: return null
    val domainVolume = AppLogger.Mapping.log(volume) { "ExerciseResponse.volume is null" } ?: return null
    val domainRepetitions = AppLogger.Mapping.log(repetitions) { "ExerciseResponse.repetitions is null" } ?: return null
    val domainIntensity = AppLogger.Mapping.log(intensity) { "ExerciseResponse.intensity is null" } ?: return null
    val domainCreatedAt = AppLogger.Mapping.log(createdAt) { "ExerciseResponse.createdAt is null" } ?: return null

    return Exercise(
        id = domainId,
        name = domainName,
        volume = domainVolume,
        repetitions = domainRepetitions,
        intensity = domainIntensity,
        iterations = iterations.toDomain(),
        exerciseExample = example,
        createdAt = DateTimeUtils.toLocalDateTime(domainCreatedAt),
    )
}

public fun List<ExerciseResponse>.toDomain(example: ExerciseExampleValue): List<Exercise> =
    mapNotNull { it.toDomainOrNull(example) }
```

Use this direction when:

- The data is **one-shot** (analytics, achievements, reports) — no caching benefit.
- The data has no canonical entity (transient computed result, e.g. `AchievementResponse`).

For everything cached, prefer the `DTO → Entity → Domain` chain so observations work.

## Entity → Domain (scalars: no null checks; relations: yes)

```kotlin
public fun TrainingPack.toDomain(): Training = Training(
    id = training.id,
    duration = training.duration.minutes,
    createdAt = DateTimeUtils.toLocalDateTime(training.createdAt),
    volume = training.volume,
    repetitions = training.repetitions,
    intensity = training.intensity,
    exercises = exercises.toDomain(),
)

public fun ExercisePack.toDomain(): Exercise? {
    val mappedExample = AppLogger.Mapping.log(example?.toDomain()) {
        "ExercisePack exercise by ${exercise.exerciseExampleId} is null"
    } ?: return null
    // ... assemble Exercise(...)
}
```

Two cases:

- **Scalar columns** — no `?: return null`. Room enforces non-null at insert, so read them directly. If a scalar somehow is null at runtime, that's a bug — let it crash with a clear `NullPointerException`.
- **Embedded relations and enum string parses** — `AppLogger.Mapping.log(...) ?: return null`. A relation (`@Relation val example: ...?`) may be absent because the related row was deleted or never inserted; an enum string may not parse if the DB pre-dates a renamed case. Both are recoverable: drop the row and log.

The per-row mapper returns `T?` when it consumes a relation or enum-string; the plural variant uses `mapNotNull`.

## Anti-patterns

- **`!!` on a nullable DTO field.** Forbidden. Use `?: return null`.
- **Defaulting required fields to placeholder values** (`val id = dto.id ?: "missing"`). The row is invalid; drop it, don't fabricate.
- **Logging once for "the whole DTO is null"** instead of per-field. Loses diagnostic value.
- **Swallowing nulls silently.** Always log via `AppLogger.Mapping.log`.
- **Catching exceptions** to convert them into null. Not the right tool here — `try/catch` is for exceptions, `?: return null` is for nullability.
- **Mixing required-field-check style with Elvis fallback style** inconsistently. Be deliberate: required → `?: return null`; nullable target → pass-through; server-alias coalesce → `(a ?: b)` inside the same `AppLogger.Mapping.log(...) ?: return null` call.
