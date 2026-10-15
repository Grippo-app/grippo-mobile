# Tokens — the AppTokens facade

Read [AppTokens](../../../../design-system/core/src/commonMain/kotlin/com/grippo/design.core/AppTokens.kt)
and its [provider](../../../../design-system/core/src/commonMain/kotlin/com/grippo/design.core/internal/ResourceProvider.kt).
The facade uses regular public properties with composable read-only getters.
A facade getter annotated `@ReadOnlyComposable` must keep its entire call chain
within that read-only contract.

| Getter | Producer | Purpose |
|---|---|---|
| colors | AppColor, DarkColor/LightColor | Semantic product colors |
| dp | AppDp | Dimensions, spacing, radii |
| typography | AppTypography | Named text styles using manrope() |
| strings | AppString | String resources |
| drawables | AppDrawable | Raster product imagery |
| icons | AppIcon extensions | ImageVector UI icons |

Consumers read AppTokens; concrete producers own raw literals. ViewModels use
injected StringProvider, never composable getters. Do not cache CompositionLocal
values outside composition. [Theme](theme.md) owns provider/locale behavior;
[accessors](accessors.md) own resource resolution APIs.

## When to add a new token

Reuse the existing semantic slot when its meaning matches. Add a demanded slot
inside its owning group when a real consumer needs a different semantic value.
Update every theme implementation and actual caller. Do not create unused slots
or refuse a needed token merely because only one consumer currently needs it.
Product color/dimension/style literals still do not belong in consumers.

## When to add a new category

The six existing categories are the default. A new category requires authorized
scope and a concrete need that cannot fit them; ask a quiz for an unresolved
scope/design choice. Do not add aliases or alternate access paths without a real need.

Review actual producers, composition wiring and all affected consumers. Token
compliance alone does not prove visual fidelity; use permitted rendered checks.
