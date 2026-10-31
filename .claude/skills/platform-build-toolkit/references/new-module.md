# New module checklist

Owner of the end-to-end steps for adding a module. Each row names the owning
skill for the content inside the module; this file owns the wiring.

Inspect a neighboring module of the same kind before creating one and reuse its
plugin list and dependency shape; do not copy a blanket dependency scaffold.

## Any module

1. `settings.gradle.kts`: add `include(":group:name")` in the group's block.
2. `build.gradle.kts`: only convention plugin ids from the matrix in
   [convention plugins](convention-plugins.md) and catalog aliases
   (`libs.*`, `projects.*`). No `repositories {}`, SDK versions, JVM target or
   `@OptIn` configuration in the module script.
3. Package: `com.grippo.<area>.<name>` in slash-separated directories.
4. Koin: when the module has bindings, follow [DI](../../di-modules/SKILL.md)
   for its annotated module shape and register its generated `.module` explicitly in
   `shared/src/commonMain/kotlin/com/grippo/shared/Koin.kt`; add the
   `implementation(projects...)` dependency to `shared/build.gradle.kts`.
5. Build the consumers under the
   [verification policy](../../../../CLAUDE.md#verification-policy), using the
   [build target catalog](../../runtime-verification/SKILL.md#build-target-catalog).

## Screen feature (`:ui-screen-features:<feature>`)

- Routes in `:ui-screen-features:screen-api` (`@Serializable` sealed class);
  `RootRouter`/`RootDirection`/`RootViewModel`/`RootComponent.createChild` entries
  in `:shared`; a public entry Component; seven MVI files per screen.
- Analytics: a sibling `:analytics:<feature>` module (api dependency, internal
  implementation, `@Single @Named("<feature>_analytics") provideSchema()`, one
  public annotated module) registered in `Koin.kt` before the analytics core
  resolves the catalog; the UI module depends on `:analytics:analytics-api`.
- Per-feature `analytics/<feature>/COVERAGE.md` with the coverage matrix and
  preservation inventory.
- Owners: [ui-feature](../../ui-feature/SKILL.md),
  [feature-analytics](../../feature-analytics/SKILL.md),
  [state-preservation](../../state-preservation/SKILL.md).

## Dialog feature (`:ui-dialog-features:<dialog>`)

- `DialogConfig.<Name>` subtype with `title: UiText`, typed content, callbacks
  and its `hasSameContentAs` branch in `:ui-dialog-features:dialog-api`;
  `DialogContentComponent.createChild` branch and `Child` entry in `:shared`;
  `shared/build.gradle.kts` dependency.
- New dialogs use a dedicated `:analytics:<dialog>` module and depend on
  `:analytics:analytics-api`; observe events from the dialog VM's own callbacks,
  without a screen hook. Register the module in `Koin.kt`. Preserve existing
  documented owners when modifying a dialog: `confirm-training-completion`
  deliberately reuses `TrainingAnalytics`; this is not a second default for new modules.
- Owner: [dialogs](../../ui-feature/references/dialogs.md).

## Data feature (`:data-features:<feature>`)

- Public `*Feature` interface, domain models and `*UseCase`s in `feature-api`;
  internal `RepositoryImpl` + `FeatureImpl` with explicit `binds`; a
  `<Feature>FeatureModule` including only the service modules it uses
  (`BackendModule`, `DatabaseModule`, `DataStoreModule`).
- New UseCases are registered in `FeatureApiModule`'s existing DSL block.
- Mapper directions it needs live in the matching `:data-mappers:*` modules.
- Owners: [data-layer](../../data-layer/SKILL.md), [mappers](../../mappers/SKILL.md),
  [di-modules](../../di-modules/SKILL.md).

## Toolkit or compose-libs module

- Toolkit: keep pure common implementations in `commonMain`; when the contract
  needs platform-specific implementations, provide complete Android/iOS actuals;
  no data, Firebase or design-system dependencies beyond the documented
  exceptions; app-lifetime state only in a `core` module with its Koin module.
- Compose libs: no `AppTokens`, no Koin, no toolkit; style and data arrive as
  parameters. Expose the reusable API publicly; helper visibility follows
  [code style](../../validation-gates/references/code-style.md#visibility-and-files).
- Owner: [toolkit ownership](toolkit-utilities-core.md),
  [platform utilities](toolkit-utilities-platform.md).
