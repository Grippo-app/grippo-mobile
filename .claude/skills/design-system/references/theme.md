# Grippo theme ownership

Read current [AppTheme](../../../../design-system/core/src/commonMain/kotlin/com/grippo/design.core/AppTheme.kt)
and [ResourceProvider](../../../../design-system/core/src/commonMain/kotlin/com/grippo/design.core/internal/ResourceProvider.kt).

The root wraps content in `AppTheme(darkTheme, localeTag, content)`; previews use
PreviewContainer. Internal ProvideResources selects DarkColor or LightColor and
provides AppIcon, AppTypography, AppString, AppDrawable and AppDp.
`localeTag` is accepted but currently unused by that provider: it does not force
resource language. Compose Resources follows platform locale. Inspect the root's
DateFormatting installation when changing locale behavior.

Consumers use AppTokens, never internal LocalApp* access. Toolkit AppTheme.current
is the system appearance source, a different symbol from this composable.
Do not invent feature-local theme providers, nested overrides or MaterialTheme
root wiring. Current public AppTheme does not expose internal vararg providers.

Theme/CompositionLocal/public category changes require existing explicit scope or
an unresolved-scope quiz. Review every provider and affected consumer; permitted
previews/manual checks must cover both themes. Report unrendered states honestly.
