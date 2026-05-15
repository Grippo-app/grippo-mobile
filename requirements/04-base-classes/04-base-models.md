# Marker Interfaces and Identity Types

The `:ui-core:foundation:models` package contains the marker interfaces that constrain `BaseViewModel`, `BaseComponent`, and the cross-component result mechanism.

## `BaseDirection`

```kotlin
public interface BaseDirection
```

Marker for ViewModel navigation intents. Every screen's `<Name>Direction.kt` declares:

```kotlin
internal sealed interface <Name>Direction : BaseDirection {
    data object Back : <Name>Direction
    data class OpenFoo(val param: T) : <Name>Direction
}
```

- `sealed interface` — compiler-enforced exhaustiveness in `eventListener`.
- Subtypes: `data object` (no params) or `data class` (with params).
- **No `@Serializable`** — Directions are emitted and consumed immediately; they don't survive process death (the router stack does).

## `BaseLoader`

```kotlin
public interface BaseLoader
```

Marker for active async operations. Every screen's `<Name>Loader.kt` declares:

```kotlin
@Immutable
internal sealed interface <Name>Loader : BaseLoader {
    @Immutable data object LoadingX : <Name>Loader
    @Immutable data object SavingY : <Name>Loader
}
```

- `@Immutable` on the sealed interface and every subtype — required for Compose stability inference (loaders flow through the UI as `ImmutableSet<Loader>`).
- Subtypes are `@Immutable data object` (no params is typical; params allowed if the UI needs to differentiate concurrent ops of the same type).
- A screen with no async ops still declares the interface (empty body): `@Immutable internal sealed interface FooLoader : BaseLoader`.

## `BaseRouter`

```kotlin
public interface BaseRouter
```

Marker for navigation route classes in `:ui-screen-features:screen-api`:

```kotlin
@Serializable
public sealed class RootRouter : BaseRouter {
    @Serializable public data object Home : RootRouter()
    @Serializable public data class Profile(val value: ProfileRouter) : RootRouter()
}
```

- `@Serializable` on the sealed class and every subtype — required for process-death restoration.
- Subtypes carry only **serializable** payloads (no callbacks, no `@Composable` types).

## `BaseResult`

```kotlin
public interface BaseResult

public data class Result<T : Any>(public val data: T) : BaseResult
```

Marker for cross-component result types. `Result<T>` wraps every payload carried through `ResultManager` — `sendResult<T : Any>(key, data: T)` channels `Result(data)` (which is itself the `BaseResult`). The producer passes the raw payload; the consumer subscribes to `Result<T>` and unwraps `.data`.

Action-protocol types live nested inside the producer's `*Router.<Screen>`, as a plain sealed interface (no `BaseResult` extension):

```kotlin
@Serializable
public sealed class TrainingRouter : BaseRouter {
    @Serializable public data class Exercise(val exercise: ExerciseState) : TrainingRouter() {

        public sealed interface Action {
            public data class Sync(val exercise: ExerciseState) : Action
            public data class Remove(val id: String) : Action
        }
    }
}
```

Producer and consumer:

```kotlin
sendResult(key = ResultKeys.create("exercise"), data = TrainingRouter.Exercise.Action.Sync(exercise))
// → channels Result(action): a BaseResult

observeResult<Result<TrainingRouter.Exercise.Action>>(key) { result ->
    when (val action = result.data) { /* ... */ }
}
```

Primitive payloads use the same shape:

```kotlin
sendResult(key = ResultKeys.create("rating"), data = 5)
observeResult<Result<Int>>(key) { result -> println(result.data) }
```

There is **one** pattern: `Result<T>` wraps everything. Keep `Action` as a plain sealed interface so `sendResult` can infer `T` directly.

## `ResultKey<T : BaseResult>`

```kotlin
public data class ResultKey<T : BaseResult>(public val key: String) {
    override fun toString(): String = key
}

public object ResultKeys {
    public fun <T : BaseResult> create(key: String): ResultKey<T> = ResultKey(key)
}
```

- The generic `T` makes `sendResult` / `observeResult` type-safe; mismatched types fail at compile time.
- The string `key` is the channel identifier — choose distinct keys per protocol (`"exercise"`, `"training-result"`, `"rating"`).
- Two `ResultKey<T>(key = "x")` instances are equal if their strings match — `data class` equality.

Idiomatic factory:

```kotlin
val Exercise: ResultKey<Result<TrainingRouter.Exercise.Action>> = ResultKeys.create("exercise")
```

## `ComponentIdentifier`

```kotlin
public interface ComponentIdentifier

public data object NoneIdentifier : ComponentIdentifier
```

Marker for distinguishing component instances. Rarely set in practice; the default is `NoneIdentifier`. Used by `BaseComponent`'s constructor as a tag for cases where multiple instances of the same component class coexist in a stack and need distinct identities (e.g. for `ResultKey` namespacing).

If a feature needs identifiers, declare a sealed subtype:

```kotlin
public sealed interface TrainingIdentifier : ComponentIdentifier {
    public data class ForExercise(val id: String) : TrainingIdentifier
}
```

Most components leave `identifier = NoneIdentifier` (the default).

## Why these interfaces exist

| Interface | Purpose |
|---|---|
| `BaseDirection` | Type constraint on `BaseViewModel<S, D, L>` and `BaseComponent<D>`'s `eventListener` |
| `BaseLoader` | Type constraint on `BaseViewModel<S, D, L>`; ensures only @Immutable types in loaders |
| `BaseRouter` | Documents that a sealed class is a navigation router; aids discovery in IDE |
| `BaseResult` | Type constraint on `ResultKey<T>`; ensures only safe-to-channel types are sent |

The markers are intentionally **empty** — they exist for type discipline, not behavior. The discipline is enforced by `BaseViewModel`'s generics and the `ResultKey` API.

## Rules

- **Don't extend `BaseDirection` with state** — Directions are transient. Persisted intent belongs in the route, not the direction.
- **Don't extend `BaseLoader` with state** — Loaders are tags. If you need to communicate progress, add a field to State and update via `update { ... }`.
- **Don't extend `BaseRouter` outside `:ui-screen-features:screen-api`** — routers are public API.
- **Don't extend `BaseResult` yourself.** The only intended implementer is `Result<T>` (in `:ui-core:foundation`). Action protocols are plain sealed interfaces nested inside the producer's `*Router.<Screen>` in `:ui-screen-features:screen-api`; `sendResult` wraps them in `Result<T>` automatically.
- **One `ResultKey` per protocol, not per subtype.** A `TrainingRouter.Exercise.Action` sealed interface uses one `ResultKey<Result<TrainingRouter.Exercise.Action>>`, not one per subtype.
