# Grippo theme ownership

Read current [AppTheme](../../../../design-system/core/src/commonMain/kotlin/com/grippo/design.core/AppTheme.kt)
and [ResourceProvider](../../../../design-system/core/src/commonMain/kotlin/com/grippo/design.core/internal/ResourceProvider.kt).

The root wraps content in `AppTheme(darkTheme, localeTag, content)` with
`localeTag = AppLocale.current`, the effective tag owned by
[toolkit localization](../../platform-build-toolkit/references/toolkit-utilities-core.md);
previews use PreviewContainer, which passes the configured preview `Locale` as
`localeTag` to AppTheme rather than runtime AppLocale. Internal ProvideResources selects DarkColor or
LightColor and provides AppIcon, AppTypography, `AppString(localeTag)`, AppDrawable
and AppDp through static CompositionLocals. `AppString` is a data class holding the
tag, so a locale change provides a new value and recomposes the provided subtree
without resetting its remembered state. The tag does not select the resource
language: `AppString.res` does not read it. Composition that needs the active
interface language reads `AppTokens.strings.localeTag`, which matches the rendered
strings in the same frame (`LanguageEnumState.current()` in `ui-core/state`); code
outside composition reads `AppLocale.current()`. Compose Resources follow the platform
locale at runtime, which `AppLocale.update` applies, and the `@Preview(locale = ...)`
configuration in previews; [preview limits](previews.md#what-a-preview-establishes)
define what those renders establish. Date
formatting in `toolkit/date-utils` reads `AppLocale` directly; inspect it with
`toolkit/localization` when changing locale behavior.

Consumers use AppTokens, never internal LocalApp* access. Toolkit AppTheme.current
is the system appearance source, a different symbol from this composable.
Do not invent feature-local theme providers, nested overrides or MaterialTheme
root wiring. Current public AppTheme does not expose internal vararg providers.

Theme/CompositionLocal/public category changes require existing explicit scope or
an unresolved-scope quiz ([scope discipline](../../../../CLAUDE.md#scope-discipline)).
Review every provider and affected consumer; the two AppPreview variants cover both
themes, and unrendered states are reported under
[visual evidence](visual-consistency.md#evidence).
