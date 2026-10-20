# Resource accessors

MUST inspect the actual producer before changing its public contract:

- [AppString](../../../../design-system/resources/provider/src/commonMain/kotlin/com/grippo/design.resources/provider/AppString.kt): composable `res` overloads for `StringResource` and `PluralStringResource`.
- [AppDrawable](../../../../design-system/resources/provider/src/commonMain/kotlin/com/grippo/design.resources/provider/AppDrawable.kt): composable `res(DrawableResource)` returning Painter.
- [AppIcon](../../../../design-system/resources/provider/src/commonMain/kotlin/com/grippo/design.resources/provider/AppIcon.kt): marker for public PascalCase ImageVector extensions in the neighboring `icons/` package.
- [StringProvider](../../../../design-system/resources/provider/src/commonMain/kotlin/com/grippo/design.resources/provider/providers/StringProvider.kt): suspend `get` and `plural` for non-composable consumers.

Composable consumers MUST use AppTokens; ViewModels MUST use injected StringProvider.
Resource identifiers may also be stored in UiText.Res and passed to providers;
MUST NOT confuse storing an identifier with bypassing the facade to resolve it.
Direct Compose resource resolution belongs in these producers/implementations.

AppIcon extensions MUST contain real nonempty vector paths, not drawable substitutes.

[resources.md](resources.md) owns locale, placeholder, drawable and font conventions;
[cookbook-resource.md](cookbook-resource.md) owns addition steps. Do not maintain
another resource convention list here.
