# Koin Annotations — Detailed Conventions

## Configuration

In `KoinAnnotationConventionPlugin` (`:build-logic/convention`):

```kotlin
extensions.getByType<KspExtension>().apply {
    arg("KOIN_CONFIG_CHECK", "false")  // TODO wait until next version of Koin Annotations
}
```

`KOIN_CONFIG_CHECK = false` disables a sanity check that doesn't yet handle some Koin Annotations 2.3.1 cases. This is a temporary workaround — re-enable when the upstream issue is fixed.

KSP generated sources land at `build/generated/ksp/metadata/commonMain/kotlin`. The convention plugin adds this as a `commonMain` source root:

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

The KSP compiler dep is added to `kspCommonMainMetadata` so it runs on the shared metadata target (the common compilation that all platforms see).

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

The platform-specific KSP tasks (`kspAndroid`, `kspIosArm64`, ...) wait for the common metadata task to finish — needed for proper module discovery.

## `@Module` and `@ComponentScan`

```kotlin
@Module
@ComponentScan
public class TrainingsFeatureModule
```

- `@Module` marks the class. The KSP processor finds it and generates an extension property `TrainingsFeatureModule.module: org.koin.core.module.Module`.
- `@ComponentScan` (no args) scans the module class's package and sub-packages for `@Single` / `@Factory` / `@Scoped` annotations.
- `@ComponentScan("package.name")` to scan a specific package — rare; default is the module's own package.

To compose modules:

```kotlin
@Module(includes = [BackendModule::class, DatabaseModule::class])
@ComponentScan
public class TrainingsFeatureModule
```

Including `BackendModule` transitively pulls in everything `BackendModule` provides. When `:shared/Koin.kt` registers `TrainingsFeatureModule().module`, both this and its includes are wired.

**Include propagation:** if A includes B and B includes C, registering A pulls in B and C. You don't need to add B and C separately.

## `@Single`

```kotlin
@Single
public class GrippoApi internal constructor(private val client: BackendClient) { ... }

@Single(binds = [TrainingsRepository::class])
internal class TrainingsRepositoryImpl(...) : TrainingsRepository { ... }
```

- Without `binds`: registered under its concrete type. `inject<GrippoApi>()` works.
- With `binds = [Interface::class]`: registered under the **interface** type. `inject<TrainingsRepository>()` works; `inject<TrainingsRepositoryImpl>()` does **not** (the impl is internal anyway).

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

Each call to `inject<OperationManager> { parametersOf(scope) }` returns a **new** instance with the passed parameter. Per-ViewModel scope is the canonical use case.

## `@Scoped`

```kotlin
@Scoped(binds = [SessionService::class])
internal class SessionServiceImpl : SessionService { ... }
```

Singleton within a custom Koin scope (`Koin.createScope(...)`). The reference repo does not currently use custom scopes — `@Single` + `@Factory` cover its needs. If you add scopes (e.g. per-user session), `@Scoped` is the annotation.

## `@InjectedParam`

```kotlin
internal class FooViewModel(
    @InjectedParam val initialId: String,
    private val barFeature: BarFeature,
) : BaseViewModel<...>(...)
```

Parameters annotated `@InjectedParam` are not resolved from Koin — they come from `parametersOf(...)` at `get()` time:

```kotlin
// Component
override val viewModel = componentContext.retainedInstance {
    getKoin().get<FooViewModel> { parametersOf(initialId) }
}
```

Order matters — `@InjectedParam` parameters are matched **positionally** with `parametersOf` arguments. Non-`@InjectedParam` parameters are resolved from Koin.

Note: most VMs in this project use `getKoin().get()` explicitly for each dep rather than registering the VM in Koin. `@InjectedParam` is used only for low-level infrastructure like `OperationManager`.

## Module includes — what's typical

| Module | Typical includes |
|---|---|
| `:toolkit:*` | None or another toolkit module |
| `:data-services:database` | `ContextModule` (for `NativeContext`) |
| `:data-services:backend` | `HttpModule`, `DatabaseModule`, `SerializationModule` |
| `:data-services:datastore` | `ContextModule` |
| `:data-features:<x>` | `BackendModule`, `DatabaseModule` |
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
            GoogleAuthModule().module,
            AppleAuthModule().module,
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

- **`KoinPlatformTools.defaultContext().startKoin { ... }`** is the multiplatform-safe way to start Koin (no Android `Context` baked in).
- **`appDeclaration()`** is the platform escape: Android calls `Koin.init { androidContext(this); androidLogger() }`; iOS calls `Koin.init {}`.
- **Every module is listed explicitly.** Adding a new module elsewhere requires adding it here.

## Anti-patterns

- **Hand-written `module { single<TrainingsFeature> { TrainingsFeatureImpl(get(), get()) } }`** for new code. Use annotations.
- **Forgetting `binds = [Interface::class]`** on an impl class. The interface won't be wired; consumers can't `inject<Interface>()`.
- **Two annotated impls binding the same interface** without disambiguation. Conflicts at startup. Use `named("...")` qualifier if you really need two.
- **`@Single` on a class that holds per-call state** (e.g. a per-request HTTP context). Use `@Factory`.
- **`@Factory` on a stateless service.** Wasteful.
- **`@Module` without `@ComponentScan` for a module that relies on separate annotated classes.** Without `@ComponentScan`, KSP does **not** scan the module's package; only provider methods declared **inside** the `@Module` class body are wired. The omission is legitimate when a module is "providers-only" (e.g. `GoogleAuthModule`, `AppleAuthModule`, `FeatureApiModule`), but it's a bug for the typical feature shape (separate `*RepositoryImpl` / `*FeatureImpl` classes annotated with `@Single`).
- **Forgetting to add `<X>Module().module` to `Koin.init`.** Runtime "no definition found" failure for everything the module would have provided.
