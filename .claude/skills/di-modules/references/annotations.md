# Koin Annotations — detailed conventions

## Configuration

In `KoinAnnotationConventionPlugin` (`:build-logic/convention`):

```kotlin
extensions.getByType<KspExtension>().apply {
    arg("KOIN_CONFIG_CHECK", "false")
}
```

`KOIN_CONFIG_CHECK = false` disables a sanity check that does not yet handle every
Koin Annotations case the project relies on. Re-enable once the upstream issue you
hit is fixed; if it never bites you, leave it off.

KSP generated sources land at
`build/generated/ksp/metadata/commonMain/kotlin`. The convention plugin adds this
as a `commonMain` source root:

```kotlin
kotlinExt.sourceSets.named("commonMain").configure {
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
    dependencies {
        implementation(libs.findLibrary("koin.core").get())
        api(libs.findLibrary("koin.annotations").get())
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.findLibrary("koin.ksp.compiler").get())
}
```

The KSP compiler dep is added to `kspCommonMainMetadata` so it runs on the shared
metadata target (the common compilation that all platforms see).

KSP tasks have an explicit dependency:

```kotlin
project.afterEvaluate {
    tasks.withType(KspAATask::class.java).configureEach {
        if (name != "kspCommonMainKotlinMetadata") {
            dependsOn("kspCommonMainKotlinMetadata")
        }
    }
}
```

The platform-specific KSP tasks (`kspAndroid`, `kspIosArm64`, ...) wait for the
common metadata task to finish — needed for proper module discovery.

## `@Module` and `@ComponentScan`

```kotlin
@Module
@ComponentScan
public class NotesFeatureModule
```

- `@Module` marks the class. The KSP processor finds it and generates an extension
  property `NotesFeatureModule.module: org.koin.core.module.Module`.
- `@ComponentScan` (no args) scans the module class's package and sub-packages for
  `@Single` / `@Factory` / `@Scoped` annotations.
- `@ComponentScan("package.name")` to scan a specific package — rare; default is
  the module's own package.

To compose modules:

```kotlin
@Module(includes = [BackendModule::class, DatabaseModule::class])
@ComponentScan
public class NotesFeatureModule
```

Including `BackendModule` transitively pulls in everything `BackendModule`
provides. When `:shared/Koin.kt` registers `NotesFeatureModule().module`, both
this and its includes are wired.

**Include propagation:** if A includes B and B includes C, registering A pulls in
B and C. You don't need to add B and C separately.

## `@Single`

```kotlin
@Single
public class <Product>Api internal constructor(private val client: BackendClient) { ... }

@Single(binds = [NoteRepository::class])
internal class NoteRepositoryImpl(...) : NoteRepository { ... }
```

- Without `binds`: registered under its concrete type. `inject<<Product>Api>()`
  works.
- With `binds = [Interface::class]`: registered under the **interface** type.
  `inject<NoteRepository>()` works; `inject<NoteRepositoryImpl>()` does **not**
  (the impl is internal anyway).

**Multiple interfaces:**

```kotlin
@Single(binds = [Foo::class, Bar::class])
internal class FooBarImpl : Foo, Bar
```

Now `inject<Foo>()` and `inject<Bar>()` both return the same instance.

## `@Factory`

```kotlin
@Factory(binds = [OperationManager::class])
internal class OperationManagerImpl(
    @InjectedParam val coroutineScope: CoroutineScope,
) : OperationManager { ... }
```

Each call to `inject<OperationManager> { parametersOf(scope) }` returns a **new**
instance with the passed parameter. Per-ViewModel scope is the canonical use case.

## `@Scoped`

```kotlin
@Scoped(binds = [SessionService::class])
internal class SessionServiceImpl : SessionService { ... }
```

Singleton within a custom Koin scope (`Koin.createScope(...)`). This template
does not currently use custom scopes — `@Single` + `@Factory` cover its needs. If
you add scopes (e.g. per-user session), `@Scoped` is the annotation.

## `@InjectedParam`

```kotlin
internal class FooViewModel(
    @InjectedParam val initialId: String,
    private val barFeature: BarFeature,
) : BaseViewModel<...>(...)
```

Parameters annotated `@InjectedParam` are not resolved from Koin — they come from
`parametersOf(...)` at `get()` time:

```kotlin
// Component
override val viewModel = componentContext.retainedInstance {
    getKoin().get<FooViewModel> { parametersOf(initialId) }
}
```

Order matters — `@InjectedParam` parameters are matched **positionally** with
`parametersOf` arguments. Non-`@InjectedParam` parameters are resolved from Koin.

Note: most VMs in this project use `getKoin().get()` explicitly for each dep
rather than registering the VM in Koin. `@InjectedParam` is used only for
low-level infrastructure like `OperationManager`.

## Module includes — what's typical

| Module | Typical includes |
|---|---|
| `:toolkit:*` | None or another toolkit module |
| `:data-services:database` | `ContextModule` (for `NativeContext`) |
| `:data-services:backend` | `HttpModule`, `DatabaseModule`, `SerializationModule` |
| `:data-services:datastore` | `ContextModule` |
| `:data-features:<x>` | Includes the data-service modules that this feature actually uses. The most common pair is `BackendModule, DatabaseModule` (network + Room cache). Variations seen in the reference: `:insights` includes only `BackendModule` (no DAO — derived metrics computed from other features); `:local-settings` includes only `DataStoreModule` (no network — preferences only). Don't mechanically copy `[BackendModule, DatabaseModule]` — list what the feature reads/writes. |
| `:design-system:resources:provider-impl` | None (just Compose Resources) |
| `:shared` (composition root) | None — the root |

## `Koin.init` setup

```kotlin
public object Koin {
    public fun init(
        appDeclaration: KoinAppDeclaration = {},
    ): KoinApplication = KoinPlatformTools.defaultContext().startKoin {
        appDeclaration()
        modules(
            ContextModule().module,
            DatabaseModule().module,
            DataStoreModule().module,
            BackendModule().module,
            GoogleAuthModule().module,        // optional — register only for the chosen auth providers
            AppleAuthModule().module,         // optional — register only for the chosen auth providers
            CoreModule().module,
            DialogModule().module,
            AuthorizationFeatureModule().module,
            ErrorModule().module,
            UserFeatureModule().module,
            // ... every feature module
            ResourcesProviderModule().module,
            SerializationModule().module,
            HttpModule().module,
            ImageLoaderModule().module,
        )
    }
}
```

- **`KoinPlatformTools.defaultContext().startKoin { ... }`** is the
  multiplatform-safe way to start Koin (no Android `Context` baked in).
- **`appDeclaration()`** is the platform escape: Android calls
  `Koin.init { androidContext(this); androidLogger() }`; iOS calls `Koin.init {}`.
- **Every module is listed explicitly.** Adding a new module elsewhere requires
  adding it here.

The full composition-root file, platform call sites, ordering, and the
add-a-module checklist live in [`composition-root.md`](composition-root.md).

## Anti-patterns

- **Hand-written `module { single<NoteFeature> { NoteFeatureImpl(get(), get()) } }`**
  for new code. Use annotations.
- **Forgetting `binds = [Interface::class]`** on an impl class. The interface
  won't be wired; consumers can't `inject<Interface>()`.
- **Two annotated impls binding the same interface** without disambiguation.
  Conflicts at startup. Use `named("...")` qualifier if you really need two.
- **`@Single` on a class that holds per-call state** (e.g. a per-request HTTP
  context). Use `@Factory`.
- **`@Factory` on a stateless service.** Wasteful.
- **`@Module` without `@ComponentScan` for a module that relies on separate
  annotated classes.** Without `@ComponentScan`, KSP does **not** scan the
  module's package; only provider methods declared **inside** the `@Module` class
  body are wired. The omission is legitimate in two distinct shapes: (a)
  **providers-only** modules whose `@Module` class body contains the full set of
  `@Single internal fun ...` providers (e.g. `GoogleAuthModule`, `AppleAuthModule`);
  and (b) the lone **hand-DSL exception**, `FeatureApiModule`, which hand-rolls
  the `.module` property via `@get:JvmName("module") public val module: ModuleObject
  = module { single { ... } }` to compose cross-feature UseCases — KSP doesn't
  generate the property at all for that module. The omission is a bug for the
  typical feature shape (separate `*RepositoryImpl` / `*FeatureImpl` classes
  annotated with `@Single`).
- **Forgetting to add `<X>Module().module` to `Koin.init`.** Runtime "no
  definition found" failure for everything the module would have provided.

## `FeatureApiModule` — the one hand-DSL exception (verbatim template)

`FeatureApiModule` lives in `:data-features:feature-api` and registers
cross-feature **UseCases** (e.g. `LoginUseCase`, `GenerateTrainingUseCase`) that
depend on multiple `*Feature` interfaces. Because UseCases are pure aggregates
over several Features (not implementations of their own interface), the annotation
flow (`@Single(binds = [SomeFeature::class]) class FooImpl`) doesn't fit — each
UseCase would need its own `binds` target and would clash with the
`@Single`-per-impl rule. The team chose the hand-DSL escape hatch for this single
module.

Reference shape (commonMain, package `com.<org>.<product>.data.features.api`):

```kotlin
import org.koin.core.annotation.Module
import org.koin.dsl.module
import kotlin.jvm.JvmName
import org.koin.core.module.Module as ModuleObject

@Module
public class FeatureApiModule {

    @get:JvmName("module")
    public val module: ModuleObject = module {
        single {
            LoginUseCase(
                authorizationFeature = get(),
                userFeature = get(),
                // ... every constructor parameter is resolved via get()
            )
        }
        single { RegisterUseCase(authorizationFeature = get(), userFeature = get(), /* ... */) }
        single { LogoutUseCase(authorizationFeature = get(), localSettingsFeature = get()) }
        // ... one `single { <UseCase>(...) }` block per UseCase exposed to the UI
    }
}
```

Five rules to copy this correctly:

1. **`@Module` annotation (no `@ComponentScan`).** Required so KSP still detects
   the module class. Without `@Module`, `Koin.init` can't enumerate it.
2. **`@get:JvmName("module")` on the `val module`.** Without this, KSP's
   annotation processor renames the getter on the JVM target and
   `FeatureApiModule().module` from `Koin.init` fails to resolve.
3. **`public val module: ModuleObject = module { ... }`.** The type must be the
   `org.koin.core.module.Module` interface (aliased to `ModuleObject` because the
   same file imports the `@Module` annotation with the same simple name — Kotlin
   can't have two unqualified `Module` types in scope).
4. **`import kotlin.jvm.JvmName` + `import org.koin.core.module.Module as
   ModuleObject`.** Both imports are mandatory; omitting either is a compile error.
5. **Don't add a second `@Module` class in `:data-features:feature-api`.** This
   module is intentionally the sole `module { ... }` site in the project;
   everything else uses `@Single`/`@Factory`/`@Scoped` annotations.

`FeatureApiModule().module` is enumerated in `:shared/Koin.kt` like any other
module — see [`composition-root.md`](composition-root.md). Do **not** treat this
template as a license to write `module { ... }` for any other feature; the
carve-out is specifically for UseCase aggregates over multiple `*Feature`
interfaces.
