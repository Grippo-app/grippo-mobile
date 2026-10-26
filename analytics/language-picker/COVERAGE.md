# Language picker coverage

Record owner: [feature-analytics](../../.claude/skills/feature-analytics/SKILL.md)
and [state-preservation](../../.claude/skills/state-preservation/SKILL.md); records
policy: [CLAUDE.md](../../CLAUDE.md#records). Cross-feature contract:
[current analytics contract review](../COVERAGE.md#current-analytics-contract-review-2026-10-14);
admin reconciliation: [Live GA4 setup review](../COVERAGE.md#live-ga4-setup-review--2026-10-14).
Rows describe the current code of `:ui-dialog-features:language-picker` and
`:analytics:language-picker`; replace them when that code changes. Typed dismissal
observations use DismissRequested with `dismiss_requested` wire names; the typed
catalog is not yet in a store build and every schema is version 1.

## Surfaces and screen hooks

| Route / rendered surface | Component → ViewModel | Presentation | Coverage |
|---|---|---|---|
| Root step: ProfileSettingsViewModel.onLanguageClick → `dialogController.open(DialogConfig.LanguagePicker(initial, title = app_language, onResult))` → DialogContentComponent child factory → LanguagePickerScreen (five endonym CheckSelectableCard Medium rows, current language checked) | LanguagePickerComponent → retained LanguagePickerViewModel | Live bottom-sheet step; default null screenPresentation, no screen enum, no dedicated dialog presentation contract | Stateless LanguagePickerAnalytics injected inside retainedInstance; Essenty BackCallback → `onDismiss`; event listener maps BackWithResult → `onResult(value)` and Back → the host back |
| Profile settings parent screen | ProfileSettingsComponent → ProfileSettingsViewModel | Existing `ProfileScreen.Settings` hook, owned by [profile](../profile/COVERAGE.md) | Parent open intent, accepted result and save operation are recorded there |
| Shared dialog slot and content stack | DialogComponent / DialogContentComponent | Containers, default null hook | Host plumbing constructs no events |

The sheet is a value picker: it has no nested steps, no asynchronous business
operation and no direct `*Feature` access. The host title comes from the caller's
`DialogConfig.LanguagePicker.title`; the screen renders no toolbar of its own.

## Callbacks and operations

| Callback owner | Observation | Eligibility and payload |
|---|---|---|
| LanguagePickerViewModel.onSelectLanguage (card tap) | LanguageClicked / `language_picker_language_selected` v1 | Emitted on every card selection callback, including the already selected language, before the existing safeLaunch `update` → 300 ms selection-feedback delay → BackWithResult. No guard exists; rapid taps keep the conflated navigation and host step guards, so counts are callback counts, not accepted-result counts. Empty payload, no language identity |
| LanguagePickerViewModel.onDismiss | DismissRequested / `language_picker_dismiss_requested` v1 | The Component's Essenty BackCallback is the only route into this callback; empty payload; a request only, no inferred cancellation or delivered close |
| Host sheet Compose BackHandler, toolbar Close, swipe, scrim | Deliberate omission | They call DialogViewModel directly and bypass this VM, so no `language_picker_dismiss_requested` is emitted on those routes |
| Selected language identity, accepted result, persistence | Deliberate omission here | The parent observes LanguageOpened, LanguageConfirmed and the SaveLanguage operation through ProfileAnalytics ([profile](../profile/COVERAGE.md)); the picker adds no reporting context to the config or result |
| Dialog viewed / closed | Deliberate omission | No dedicated presentation contract |

The 300 ms delay is UI selection feedback, not a business operation, so no
operation API or decorator exists. No view/close, raw language tag, locale,
identifier, secret or exception payload is emitted. Ordinary reporting exceptions
cannot prevent navigation (the implementation catches Exception); fatal Errors
propagate.

## Catalog

`LanguagePickerAnalyticsModule` (`@Module @ComponentScan`) binds
`LanguagePickerAnalyticsImpl` as `LanguagePickerAnalytics` and registers the two
empty-parameter v1 schemas through one qualified `language_picker_analytics`
provider; `shared/.../Koin.kt` lists `LanguagePickerAnalyticsModule().module` once,
and the module depends only on `analytics-api`. The API exposes `trackEvent` only;
the two typed events each have one VM consumer and one exhaustive encoder.

Admin synchronization: both wire events are new and not yet reconciled with the
live admin; their mapping belongs in the admin section of
[analytics/COVERAGE.md](../COVERAGE.md#admin-synchronization-evidence).

## Preservation inventory

Dialog sessions follow the canonical policy: the live selection survives ordinary
recreation through the retained VM, and the session is discarded on process death.
No local StateKeeper supplier, serializer or session restoration exists.

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| LanguagePickerComponent / LanguagePickerViewModel / LanguagePickerState (`@Immutable`, not serializable) | `value: LanguageEnumState`, including a selection pending the delayed result | Runtime only (retained in the live session) | Constructor `initial` = `DialogConfig.LanguagePicker.initial`; retainedInstance evaluates it once for a fresh VM; no DRAFT | A fresh dialog checks the caller's current effective language; ordinary recreation keeps the live value; process death closes the dialog and keeps the last saved language |
| LanguagePickerState | `languages` | Rebuild | `LanguageEnumState.entries` default | Constant catalog of the five supported locales; rebuilt for every fresh session |
| LanguagePickerScreen | Row titles | Rebuild during composition | `LanguageEnumState.title()` through `AppTokens.strings.res` | Endonyms are identical in every locale, so a language switch does not change the rows |
| ProfileSettings owner / LocalSettingsFeature | Accepted language | Rebuild (durable source, no StateKeeper copy) | `onResult` → `setLanguage` → DataStore `language` key, applied app-wide by ApplicationBootstrap → `AppLocale.update` | Each opening seeds `initial` from the resolved effective language `LanguageEnumState.of(AppLocale.current())`; owned by [profile](../profile/COVERAGE.md) |
| Dialog host / picker VM | Session, callbacks, pending result, delay job, loaders (none defined), directions, telemetry | Runtime only | Shared childSlot and childStack use `serializer = null` | Not archived or replayed; no secrets or telemetry provenance enter saved state |

## Runtime checks

Permitted manual checks: open the picker from Profile settings and
verify the effective language is checked; select another language and verify the
sheet closes after the selection feedback and the parent row and UI switch (ViewModel-preformatted date text keeps the old language until its data re-emits, an accepted limitation); select
the already effective language and verify it is saved as an explicit choice; system Back, toolbar
Close, swipe and scrim leave the language unchanged; ordinary Activity recreation
keeps the live selection; process recreation with the sheet open closes it and
keeps the last saved language; with a live sink, check both events and that a
reporting failure does not block result delivery or dismissal.
