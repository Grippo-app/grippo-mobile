# `:toolkit:context` — `NativeContext`

`NativeContext` is the **only** way `commonMain` code reaches a platform handle. On Android it wraps the `Context`; on iOS it is empty.

## Shape

```kotlin
// commonMain
public expect class NativeContext

// androidMain
public actual class NativeContext(public val context: Context)

// iosMain
public actual class NativeContext
```

## Provider

```kotlin
// commonMain
@Module
@ComponentScan
public expect class ContextModule() {
    @Single
    internal fun providesNativeContext(scope: Scope): NativeContext
}

// androidMain
@Module
@ComponentScan
public actual class ContextModule actual constructor() {
    @Single
    internal actual fun providesNativeContext(scope: Scope): NativeContext =
        NativeContext(scope.get<Application>())
}

// iosMain
@Module
@ComponentScan
public actual class ContextModule actual constructor() {
    @Single
    internal actual fun providesNativeContext(scope: Scope): NativeContext =
        NativeContext()
}
```

The Android `Application` comes from `androidContext(this@App)` set in `Koin.init { ... }` — Koin's `androidContext()` plugin registers the supplied instance under its concrete `Application` type. The provider reads it via `scope.get<Application>()`. `@Module` and `@ComponentScan` must be repeated on each `actual` class — KSP runs against each platform compilation and the annotations don't propagate from `expect` to `actual`.

## Usage

```kotlin
// :data-services:database/DatabaseModule.kt
@Module(includes = [ContextModule::class])
@ComponentScan
public class DatabaseModule {
    @Single
    internal fun provideDatabase(nativeContext: NativeContext): Database =
        nativeContext.getDatabaseBuilder()
}

// :data-services:database/DatabaseBuilder.kt
// commonMain
internal expect fun NativeContext.getDatabaseBuilder(): Database

// androidMain
internal actual fun NativeContext.getDatabaseBuilder(): Database {
    val appContext = this.context.applicationContext
    val dbFile = appContext.getDatabasePath("...")
    return Room.databaseBuilder<Database>(context = appContext, name = dbFile.absolutePath)
        // ...
        .build()
}

// iosMain
internal actual fun NativeContext.getDatabaseBuilder(): Database {
    val dbPath = documentDirectory() + "/<product>_database.db"
    return Room.databaseBuilder<Database>(name = dbPath)
        // ...
        .build()
}
```

The pattern: `commonMain` declares `NativeContext.<verb>()` as `expect`; per-platform `actual` extension functions consume the platform handle.

## Why this pattern

- **Android** needs a `Context` for everything platform-touching (filesystem, system services, intents). `commonMain` cannot import `Context`.
- **iOS** doesn't have an equivalent global handle; most APIs are stateless objects (`NSFileManager.defaultManager`, `UIApplication.sharedApplication`).
- **`NativeContext`** encapsulates the asymmetry: Android implementations cast to `this.context`; iOS implementations don't need the parameter at all, but accepting `NativeContext` keeps the signature platform-neutral.

## Rules

- **`NativeContext` is the only platform-handle escape**. No other `expect/actual` class wraps `Context`.
- **The Android `Application` registered in Koin must be the `Application` instance**, not an Activity. `androidContext(this@App)` in `App.onCreate` does this — Koin registers it under its `Application` type.
- **`scope.get<Application>()` is fine inside `ContextModule`**, but DON'T pull `Application`/`Context` directly elsewhere. Use `NativeContext`.
- **iOS code doesn't import `NativeContext` for anything**. The iOS `actual` is an empty class; extension functions in `iosMain` just need to take `NativeContext` as the receiver to match the `expect` signature.

## Anti-patterns

- **`Context` import in `commonMain`.** Compile error (correctly).
- **Bypassing `NativeContext`** by exposing a `Context` provider in Koin. The Application-vs-Activity confusion bites.
- **Storing a non-Application `Context`** in `NativeContext`. Hold the Application context; pass an Activity context locally when needed.
- **iOS `NativeContext` with actual state.** Keep it empty unless there's a specific reason (e.g. holding a `UIApplication` reference).
