# `UiText`

State classes that hold strings cannot use `String` for values that depend on resources (the locale may change at runtime). `UiText` is the sealed type used in state for all localizable strings.

## Signature

```kotlin
@Stable
public sealed interface UiText {

    @Immutable
    public data class Res(
        public val value: StringResource,
        public val formatArgs: ImmutableList<Any> = persistentListOf(),
    ) : UiText

    @Immutable
    public data class Str(public val value: String) : UiText

    @Composable
    public fun text(): String = when (this) {
        is Str -> value
        is Res -> {
            val args = remember(formatArgs) { formatArgs.toTypedArray() }
            AppTokens.strings.res(value, *args)
        }
    }

    public suspend fun text(stringProvider: StringProvider): String = when (this) {
        is Str -> value
        is Res -> stringProvider.get(value, *formatArgs.toTypedArray())
    }
}
```

Lives in `:ui-core:state/formatters/UiText.kt`.

## Subtypes

| Subtype | When |
|---|---|
| `Res(value: StringResource, formatArgs: ImmutableList<Any>)` | Localized text from `strings.xml`. Format args are interpolated. |
| `Str(value: String)` | Verbatim text — a user's name, an email, a server-provided message in any language. |

`Res` carries the **resource ID**, not the resolved string. The string is resolved lazily at render time — picking up the current locale.

`Str` carries a literal `String` — the value won't change with locale.

## Resolving

### In a Composable

```kotlin
Text(text = uiText.text())
```

`text()` is `@Composable`. It calls `AppTokens.strings.res(StringResource, *args)` for `Res`, returns `value` for `Str`.

### In a ViewModel / suspend context

```kotlin
val message = uiText.text(stringProvider)
notificationManager.show(AppNotification(id, title, message))
```

The `suspend` overload accepts a `StringProvider` — useful when the text needs to be resolved before crossing a layer boundary (notifications, error reports).

## Usage in state

```kotlin
@Immutable
internal data class ErrorState(
    val title: UiText,
    val description: UiText?,
)

val s = ErrorState(
    title = UiText.Res(Res.string.error_no_internet_title),
    description = UiText.Res(Res.string.error_no_internet_description),
)
```

Or with args:

```kotlin
val s = WelcomeState(
    greeting = UiText.Res(
        Res.string.welcome_user,
        formatArgs = persistentListOf(user.name),
    ),
)
```

Or verbatim from a server response:

```kotlin
val s = ErrorState(
    title = UiText.Str(serverError.title),
    description = serverError.description?.let { UiText.Str(it) },
)
```

## Why `formatArgs: ImmutableList<Any>`

- **`ImmutableList`** for Compose stability.
- **`Any`** because format args can be `String`, `Int`, `Float`, etc. (matching the placeholders in `strings.xml`).
- **`persistentListOf()` default** for the no-args case.

## Why a `sealed interface`

The choice between Res and Str is **discrete**, not a parameter. Sealed interface + exhaustive `when` lets the compiler verify both branches are handled.

## Equality

`Res(Res.string.foo) == Res(Res.string.foo)` — true.
`Res(Res.string.foo, persistentListOf("a")) == Res(Res.string.foo, persistentListOf("b"))` — false.
`Str("Hello") == Str("Hello")` — true.

Compose uses these equality checks during recomposition; equal `UiText` instances skip re-resolution.

## Anti-patterns

- **`String` in state** for localizable values.
- **Pre-resolving `UiText` at VM construction** by injecting `StringProvider`. Pass the `UiText` raw; let the UI resolve. (Exception: when the value crosses a boundary like a notification, resolve at the boundary.)
- **`UiText.Res(Res.string.foo, listOf(arg))`** — `listOf` is mutable; use `persistentListOf`.
- **`uiText.text(stringProvider)` from a `@Composable`** — use the `@Composable` `text()` instead.
- **`UiText.Str` for a localized string from `strings.xml`.** Use `UiText.Res` so the value updates on locale change.
- **Wrapping a `String?` as `UiText.Str(it)`** — if the description is optional, use `UiText?` and check for null at render.
