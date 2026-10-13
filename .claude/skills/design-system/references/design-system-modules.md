# Grippo design-system modules

Use settings.gradle.kts and actual module build files as authority. Do not copy
another Android DSL or introduce module settings outside convention policy.

| Module | Current responsibility |
|---|---|
| core | AppTheme, AppTokens and internal CompositionLocals/ProvideResources |
| components | Shared public Compose widgets, related style/content types and previews |
| resources:provider | Compose resources, AppColor/AppDp/AppTypography/AppString/AppDrawable/AppIcon, StringProvider contract |
| resources:provider-impl | StringProviderImpl and generated Koin module |
| preview | AppPreview and PreviewContainer |

Keep resource access through AppTokens in composition and injected StringProvider
outside it. Product UI icons are ImageVector AppIcon extensions; raster imagery
uses WebP or justified PNG. Resource locales and actual manrope font registration
come from source. Do not claim unimplemented producer overloads.

Components may depend on existing pure UI contracts/state, as actual build files
do; they must not import feature implementation or data/service business logic.
Do not extend a pure-type dependency exception to unrelated modules.
Use public visibility only for real module-boundary consumers; helpers internal/private.

Apply the existing android.library, kotlin.multiplatform and compose.multiplatform
conventions; add koin.annotation only where the module actually owns DI.
The resource provider enables `androidLibrary { androidResources.enable = true }`
for resource packaging. Do not duplicate namespace/SDK settings in feature recipes.

Read the actual previews before changing assumptions: AppPreview declares its
locale/device variants and PreviewContainer provides preview resources/theme.
A preview rendering is evidence for that exact state, not global runtime coverage.

No unused token/component scaffold. For an authorized new module, wire the exact
settings/dependencies/composition-root additions it requires; otherwise resolve
scope before changing those boundaries. Review all consumers after contract changes.
