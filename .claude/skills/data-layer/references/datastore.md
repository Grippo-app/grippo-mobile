# DataStore — key-value preferences

Self-contained reference for the DataStore rules.

> **Illustrative domain.** Code uses `Note` / `Tag` / `User` as the generic `<Entity>` /
> `<RelatedEntity>`. Substitute identifiers from your product domain.

`:data-services:datastore` wraps AndroidX DataStore (`preferences-core`) for **small key-value
preferences**: theme override, language override, debug toggles, last-used range, onboarding seen.

It is **not** for:

- Auth tokens — those live in Room (`TokenEntity`).
- Domain data (`<entity>`s, `<related>`s, …) — Room.
- Sensitive data (PII, payment info) — Room with platform encryption.

---

## Module shape (NORMATIVE)

```
:data-services:datastore/
  src/commonMain/kotlin/com/<org>/<product>/services/datastore/
    DataStoreModule.kt
    PreferencesDataStoreBuilder.kt            // expect on NativeContext
  src/androidMain/.../PreferencesDataStoreBuilder.android.kt
  src/iosMain/.../PreferencesDataStoreBuilder.ios.kt
```

The module is intentionally tiny: one Koin module + one platform-resolved factory function. Keys
and typed accessors live with their consumers (a `*Repository` in `:data-features:*`), not inside
`:data-services:datastore`.

---

## `DataStore` factory (expect/actual on `NativeContext`) (NORMATIVE)

```kotlin
// commonMain
internal expect fun NativeContext.getPreferencesDataStore(): DataStore<Preferences>

// androidMain
internal actual fun NativeContext.getPreferencesDataStore(): DataStore<Preferences> {
    val filePath = context.filesDir.resolve("<product>_settings.preferences_pb").absolutePath
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { filePath.toPath() },
    )
}

// iosMain
internal actual fun NativeContext.getPreferencesDataStore(): DataStore<Preferences> {
    val filePath = documentDirectory() + "/<product>_settings.preferences_pb"
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { filePath.toPath() },
    )
}

private fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path)
}
```

---

## `DataStoreModule` (EXAMPLE)

```kotlin
@Module(includes = [ContextModule::class])
@ComponentScan
public class DataStoreModule {
    @Single
    internal fun providePreferencesDataStore(
        nativeContext: NativeContext,
    ): DataStore<Preferences> = nativeContext.getPreferencesDataStore()
}
```

---

## Repository integration (NORMATIVE)

`DataStore<Preferences>` is injected **directly** into the `*Repository` that owns the preference.
Typed wrappers (`ThemeStorage`, `LocaleStorage`, …) are deliberately not used — the module is
small enough that the extra layer adds noise without isolating anything meaningful.

```kotlin
@Single(binds = [LocalSettingsRepository::class])
internal class LocalSettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : LocalSettingsRepository {

    companion object {
        val RangeKey = stringPreferencesKey("range")
        val LastSuggestionShownAtKey = stringPreferencesKey("last_suggestion_shown_at")
        val WelcomeStatusKey = stringPreferencesKey("welcome_status")
    }

    override fun observeRange(): Flow<Range?> =
        dataStore.data.map { preferences -> Range.of(preferences[RangeKey]) }

    override suspend fun setRange(range: Range?): Result<Unit> = runCatching {
        dataStore.edit { preferences ->
            val key = range?.key
            if (key == null) preferences.remove(RangeKey)
            else preferences[RangeKey] = key
        }
    }

    override suspend fun clear(): Result<Unit> = runCatching {
        dataStore.edit { it.clear() }
    }
}
```

### Repository integration rules (MUST)

- **Keys live in a `companion object`** on the Repository that owns them. They are `val`s (not
  `const`) because `stringPreferencesKey(...)` builds a typed key object.
- **One Repository per feature** owns its keys. Don't share key constants across modules —
  duplication is preferable to a shared "keys" object that everyone has to import.
- **Encode non-string values to strings** (`enum.key`, `DateTimeUtils.toUtcIso(...)`) and decode
  via a domain-side `of(...)` factory. This keeps the DataStore layer schemaless while the
  Repository owns the value semantics.
- **Mutations return `Result<Unit>`** via `runCatching { dataStore.edit { ... } }` — the
  Repository contract is `Result`-shaped just like the network path.
- **Observers expose `Flow<T?>`** by `map`-ping over `dataStore.data`. No `firstOrNull()`
  snapshots — consumers want the live value.
- **A `clear()` operation** is provided when the Repository is wiped on logout.

---

## Why DataStore and not SharedPreferences (REFERENCE)

- **Multiplatform.** AndroidX DataStore (`preferences-core`) works on KMP (via Okio + the
  `Preferences` data class). SharedPreferences is Android-only.
- **Async by default.** DataStore exposes `Flow<Preferences>`; SharedPreferences is sync with a
  separate listener API.
- **No commit/apply confusion.** `edit { it[K] = v }` is the only mutation API.
- **Type-safe keys.** `stringPreferencesKey("...")` / `intPreferencesKey("...")` make accidental
  type mismatches a compile error.

## Why not Room (REFERENCE)

- Room is for structured data with relations. DataStore is for unrelated flat key-value.
- Room's overhead (schema, migrations, FKs) is wasted for a single boolean preference.
- The DataStore file is a single small protobuf — backup/restore on Android picks it up
  automatically.

---

## Keys naming (MUST)

`stringPreferencesKey("range")`, `booleanPreferencesKey("onboarding_seen")`,
`intPreferencesKey("last_used_range_kind")`. Keys:

- Are `snake_case`.
- Describe the value, not the type.
- Are namespaced via the underscore form (e.g. `debug_show_recompositions`) when two unrelated
  repositories share the single DataStore file — the template currently has none of these
  collisions.

---

## What goes in DataStore (REFERENCE)

| Setting | DataStore? |
|---|---|
| Last-used filter / range | ✅ — the template stores this as `range` |
| One-shot "welcome" / "shown at" flags | ✅ — e.g. `welcome_status`, `last_suggestion_shown_at` |
| Onboarding-seen flag | ✅ |
| Debug toggles | ✅ |
| Theme / language override | ✅ (when a per-user override is needed; the template defers to system) |
| Auth tokens | ❌ — Room (`TokenEntity`) |
| User profile | ❌ — Room (`UserEntity`) |
| Cached domain list | ❌ — Room (`<Entity>Entity`) |
| Push token | ❌ — sent to server; not persisted client-side |

---

## Anti-patterns (MUST)

- **Storing JSON blobs in DataStore.** That's structured data; use Room.
- **Multiple DataStore instances.** One per app; one `DataStoreModule`.
- **Sync access via `runBlocking { dataStore.data.first() }`** — DataStore is meant to be async.
  Either expose `suspend fun get()` or `Flow<T>`.
- **Caching values in `DataStore<Preferences>`** — DataStore already caches; manual caching
  diverges from the file.
- **Sharing DataStore with native iOS code.** The DataStore file format is Kotlin-only; iOS Swift
  code uses `UserDefaults` independently.
- **Wrapping each key in a dedicated `*Storage` class.** This template keeps keys + accessors
  on the Repository that owns them. Add a wrapper only when a single preference is consumed by
  three or more unrelated repositories.
