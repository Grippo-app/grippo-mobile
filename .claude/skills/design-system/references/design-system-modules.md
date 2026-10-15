# Design-system module ownership

Use settings.gradle.kts and actual build files as authority.

| Module | Responsibility |
|---|---|
| core | AppTheme, AppTokens and internal CompositionLocals/ProvideResources |
| components | Shared widgets, related style/content types and previews |
| resources:provider | Compose resources, token producers and StringProvider contract |
| resources:provider-impl | StringProvider implementation and annotated DI module |
| preview | AppPreview and PreviewContainer |

Components may depend on existing pure UI contracts/state where their build files
allow it. They MUST NOT import feature implementations or data/service business
logic. A dependency exception does not authorize unrelated module access.
Visibility and extraction follow the canonical
[module rules](../../ui-feature/references/module-structure.md).

Apply existing build conventions; add Koin annotations only at actual DI owners.
[Resource policy](resources.md#android-packaging) owns the provider's Android
resource opt-in. Do not duplicate namespace/SDK settings or create unused scaffold.
New module/settings/composition-root changes require the task's authorized scope.
