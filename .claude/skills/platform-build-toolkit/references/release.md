# Release, versioning and compatibility bumps

Owner of where version numbers live and which changes require a bump.
Version changes, release builds and store uploads happen only on the user's
request.

## Where versions live

| Platform | Field | File |
|---|---|---|
| Android | `versionCode`, `versionName` | `androidApp/build.gradle.kts` (`defaultConfig`) |
| iOS | `MARKETING_VERSION`, `CURRENT_PROJECT_VERSION` | `iosApp/iosApp.xcodeproj/project.pbxproj` (both configurations) |
| iOS restoration archive | `formatVersion` | `iosApp/iosApp/ApplicationStateStore.swift` |
| Room schema | `@Database(version = N)` + exported JSON | `data-services/database/.../Database.kt`, `data-services/database/schemas/` |
| Analytics schemas | per-schema version in each `:analytics:<feature>` implementation | see [feature-analytics](../../feature-analytics/SKILL.md) |

Android and iOS versions are maintained independently and may differ.

## Compatibility bumps

- **Room**: any entity/column/index change needs a migration object registered in
  `DatabaseMigrations.all`, a version bump and the regenerated schema JSON
  committed. Without a migration the destructive fallback drops user data.
- **Additive serialized fields**: adding an optional property with a default to a
  `@Serializable` State or route is backward-compatible and needs no bump;
  removing, renaming or retyping a field does.
- **iOS restoration archive**: when a serializer, `PersistenceKey` address,
  router shape or Essenty key changes so that a saved checkpoint can no longer
  decode, increment `formatVersion` (or the build version) so the old checkpoint
  is discarded instead of failing. Ordinary refactors keep keys unchanged.
- **Analytics**: released schemas keep their meaning; semantic changes need a
  reporting decision before a version bump (owner: feature-analytics).
- **Public Swift API**: changes to exported `:shared`, `:toolkit:*:api` or
  Decompose-facing types require the XCFramework build and an `iosApp` compile.

## Release builds

- Android release uses minification and resource shrinking with
  `androidApp/proguard-rules.pro`; signing comes from `secure/keystore/`.
- iOS release runs the Crashlytics upload build phase; the release XCFramework is
  `./gradlew :shared:assembleSharedReleaseXCFramework`.
- Agents never produce or upload store artifacts; they may verify that a
  release configuration compiles when the user asks.
