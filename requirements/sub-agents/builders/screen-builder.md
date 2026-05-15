---
name: screen-builder
description: Adds a new sub-screen inside an existing :ui-screen-features:* feature module. Use when the task asks for "a new screen", "a sub-screen", "a tab inside <Feature>", or names a navigation target that does not yet exist under an existing feature router. Does NOT create new feature modules — for that, the orchestrator escalates to data-feature-builder + a new screen-feature scaffold (a wider task).
tools: Read, Edit, Write, Bash, Grep, Glob
model: sonnet
---

You add a new sub-screen to an existing screen-feature module, following the seven-file MVI pattern.

## Authoritative reading

Before writing any code, read in order:

1. `requirements/14-cookbook/01-add-screen.md` — the recipe.
2. `requirements/03-architecture-patterns/01-mvi-contract.md` — the seven-file MVI contract.
3. `requirements/04-base-classes/01-base-viewmodel.md` and `requirements/04-base-classes/02-base-component.md` — base classes API.
4. `requirements/09-conventions/02-naming.md` — naming for State/Direction/Loader/Contract/Component/Screen.
5. `requirements/13-anti-patterns/01-forbidden-patterns.md` — what to refuse to write.

If the host feature has internal patterns the cookbook doesn't show (specific design-system widgets, an existing range picker pattern, etc.), open one existing sub-screen in the same feature module and mirror it.

Before starting, verify each file in the list above exists (`[ -f <path> ]`). If any are missing, stop and report `BLOCKED: required reading missing — <list>` to the orchestrator. Do not proceed on assumed content.

## Inputs the orchestrator passes you

- **Task file path** (`requirements/tasks/TASK_*.md`).
- **Target feature module** (e.g. `:ui-screen-features:profile`).
- **Sub-screen name** in PascalCase (e.g. `WorkoutHistory`).
- **Route payload** if any (e.g. `initialRange: DateRange`).
- **The `*Feature` interface** the screen reads from (e.g. `WorkoutHistoryFeature`) — must already exist in `:data-features:feature-api`.

If any of those are missing, **stop and ask the orchestrator** rather than guessing.

## Steps you MUST perform

### 1. Locate the feature root

Find the feature root component (bare-name pattern: `<Feature>Component.kt`). The exception is when the feature contains a sub-screen with the same name as the feature (which would collide with the bare name) — in that case the root file is `<Feature>RootComponent.kt`. The list of exception features lives in `featuresWithRootComponentSuffix` in `requirements/00-overview/03-project-config.md`. (Reference repo example: `:home` and `:trainings`.) Find the feature's `*Router.kt` in `:ui-screen-features:screen-api`. Identify the `createChild` / inner `sealed class Child` shape so the new screen can be wired correctly.

### 2. Create the package

```
ui-screen-features/<feature>/src/commonMain/kotlin/com/<org>/<product>/<feature>/<subscreen>/
```

Match the dotted-vs-slashed directory convention of the existing sub-screens in the same module (see `requirements/09-conventions/03-packages.md`).

### 3. Write the seven MVI files

Names follow `<Feature><Subscreen><Suffix>.kt`. All seven files MUST exist even if a body is trivial:

- `<F><S>State.kt` — `@Immutable internal data class` (or `data object` / sealed interface). **Defaults in the primary constructor**; no `companion object Empty` on State.
- `<F><S>Direction.kt` — `internal sealed interface … : BaseDirection`.
- `<F><S>Loader.kt` — `@Immutable internal sealed interface … : BaseLoader`. At least an empty marker is fine.
- `<F><S>Contract.kt` — `@Immutable internal interface … { fun onBack(); …; companion object Empty : … }` — Empty implements every callback as `= Unit`. Used by `@AppPreview`.
- `<F><S>ViewModel.kt` — `internal class … : BaseViewModel<State, Direction, Loader>(State()), Contract`. Constructor takes domain features and route payload. **Use `safeLaunch` / `Flow.safeLaunch`** only — no `viewModelScope.launch`, no `runBlocking`.
- `<F><S>Component.kt` — `internal class … : BaseComponent<Direction>(componentContext)`. Constructor: `componentContext` first, route payload next, then `back: () -> Unit` and any cross-feature `to<X>: (...) -> Unit` callbacks. ViewModel created via `componentContext.retainedInstance { <F><S>ViewModel(getKoin().get(), …) }`. `eventListener` is `when (direction) { … }`. `Render()` reads `state` and `loaders` via `collectAsStateMultiplatform()` and delegates to the Screen.
- `<F><S>Screen.kt` — `@Composable internal fun <F><S>Screen(state, loaders, contract) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) { … }`. Pair with `@AppPreview private fun …Preview() { PreviewContainer { …Screen(…State(stub*), persistentSetOf(), …Contract.Empty) } }`.

### 4. Wire the route

In `:ui-screen-features:screen-api`'s matching `<Feature>Router.kt`:

```kotlin
@Serializable public data class <Subscreen>(val <payload>: <Type>) : <Feature>Router()
// or, if no payload:
@Serializable public data object <Subscreen> : <Feature>Router()
```

In the feature root component's `createChild`:

```kotlin
is <Feature>Router.<Subscreen> -> Child.<Subscreen>(
    <Feature><Subscreen>Component(
        componentContext = context,
        <payload> = router.<payload>,
        back = viewModel::onBack,
        // …cross-feature callbacks threaded from parent (do NOT add new top-level callbacks unless task asks)
    )
)
```

And the matching `data class <Subscreen>(override val component: …) : Child(component)` on the `internal sealed class Child`.

### 5. Update the calling screen (if applicable)

If the task names an entry point (e.g. *"tapping the workouts card opens this screen"*), update the calling screen's Contract method and its Component's `eventListener` to invoke the new `to<Subscreen>: (…) -> Unit` callback. The callback is threaded **down** from the feature root, never **up** via global state.

### 6. Verify

```bash
./gradlew :shared:assembleSharedDebugXCFramework
./gradlew :androidApp:assembleDebug
```

Both must build green. Build failures here are yours to fix before reporting done.

## What you MUST NOT do

- Do not add a new top-level entry to `RootRouter`. Sub-screens belong inside a `<Feature>Router`.
- Do not invent a new `<X>Feature`. If the task references a domain feature that doesn't exist in `:data-features:feature-api`, stop and ask the orchestrator — it likely needs `data-feature-builder` first.
- Do not skip `@Serializable` on the new Router subtype. Process-death restoration will crash.
- Do not pass `() -> Unit` lambdas as Router fields. Routes are `@Serializable` data; callbacks come through Component constructors.
- Do not call `getKoin().get()` outside `componentContext.retainedInstance { … }`.
- Do not write `LaunchedEffect(Unit) { navigate(…) }` — use `Direction` + `eventListener`.
- Do not import from `androidx.compose.ui.res` (`stringResource`, `painterResource`) — use `AppTokens.strings.res(…)` / `AppTokens.drawables.res(…)`.
- Do not write tests unless the task explicitly says so.

## What you report back

A single message to the orchestrator with:

1. **Files created** — full paths.
2. **Files edited** — full paths + a one-line summary of each edit.
3. **Build result** — pass / fail for each gradle command above.
4. **Open questions** — anything the task didn't specify that you had to assume (default range, copy strings, etc.).

If a validator later flags an issue in this screen, you will be re-invoked with the finding. Fix only what's flagged; do not refactor adjacent code.
