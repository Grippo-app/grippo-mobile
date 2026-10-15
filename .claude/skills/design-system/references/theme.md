# Grippo theme ownership

Read current [AppTheme](../../../../design-system/core/src/commonMain/kotlin/com/grippo/design.core/AppTheme.kt)
and [ResourceProvider](../../../../design-system/core/src/commonMain/kotlin/com/grippo/design.core/internal/ResourceProvider.kt).

The root wraps content in `AppTheme(darkTheme, localeTag, content)`; previews use
PreviewContainer. Internal ProvideResources selects DarkColor or LightColor and
provides AppIcon, AppTypography, AppString, AppDrawable and AppDp.
`localeTag` is accepted but unused by that provider (`@Suppress("UNUSED_PARAMETER")`):
it does not force resource language. Compose Resources follow the platform locale
at runtime and the `@Preview(locale = ...)` configuration in previews; the preview
consequence is stated once in the
[default viewport scope](visual-consistency.md#default-viewport-scope). Inspect
the root's DateFormatting installation when changing locale behavior.

Consumers use AppTokens, never internal LocalApp* access. Toolkit AppTheme.current
is the system appearance source, a different symbol from this composable.
Do not invent feature-local theme providers, nested overrides or MaterialTheme
root wiring. Current public AppTheme does not expose internal vararg providers.

Theme/CompositionLocal/public category changes require existing explicit scope or
an unresolved-scope quiz ([scope discipline](../../../../CLAUDE.md#scope-discipline)).
Review every provider and affected consumer; the two AppPreview variants cover both
themes, and unrendered states are reported under
[visual evidence](visual-consistency.md#evidence).
