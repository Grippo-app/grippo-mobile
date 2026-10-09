# Whole-app design-system audit — 2026-10-09

## Scope

Inspected all 140 Kotlin files in `design-system/components`, including 47 card
files, and all 53 screen files across screen and dialog features. The capture
catalog contains 70 whole-screen states from 46 leaf screen files and 180
component preview variants. Seven remaining screen files are navigation
containers; their child screens are included in the catalog.

The audit covers authorization/onboarding, dashboard, profile, recording,
exercise editing, history/calendar, statistics, goals, insights, pickers,
confirmation/error dialogs and the shared component catalog.

## Changes

- Regular list-card titles use `h5`; secondary information uses neutral text
  tokens. Exercise/example/user/selector cards follow the same hierarchy.
- Ordinary metric values and timestamps are neutral. Status stripes, badges,
  diagnostic severity, muscle maps and chart series retain meaningful accents.
  Resume workout uses the primary action style.
- Exercise cards retain 64 dp thumbnails, completed/total set progress and the
  maximum recorded weight. No maximum appears before a set is recorded. Only
  fully completed progress is green; no duplicate completion badge.
- At enlarged font sizes the timer gets its own row, paired dashboard cards
  stack, performance cards span the grid, diagnostic labels and values stack,
  and exercise summaries put each metric on its own line. Decorative workout
  artwork stops competing with enlarged text.
- Toolbars measure leading/trailing actions before centering a title. A title
  that does not fit at enlarged fonts gets a separate row. Metric breakdowns
  use two columns; indicator dates have bounded slots and can wrap. Last-used
  metadata wraps as one text block. Button labels allow two lines at enlarged
  fonts, with full-width dashboard actions.
- Selectors, chips, segmented labels and editable set cells use minimum heights;
  tags wrap. Timeline slots receive the design system's text color/style.
- Production component dimensions use `AppTokens.dp`. Chart geometry, input
  units, borders, shadow elevations and decoration offsets were moved into the
  existing token provider without changing their ordinary dimensions.
- Circular-chart labels sit outside their colored segments with neutral text,
  so their contrast does not depend on a slice color.
- Component/chart preview palettes now use theme tokens. Removed obsolete
  exercise-card implementations; the regular variants share one row layout.

The usage rules are recorded in [CONSISTENCY.md](../design-system/CONSISTENCY.md).

## Verification and evidence

The clean production build passed:

```sh
./gradlew :androidApp:assembleDebug :shared:compileKotlinIosArm64 \
  :shared:compileKotlinIosSimulatorArm64 --offline
```

`git diff --check` passed. A static scan of all 140 component source files found
zero nonzero literal `.dp` dimensions before their preview sections. Temporary
capture wrappers and the activity hook were removed from the sources before
production verification.

Screenshots use the actual Android Compose renderer on a Pixel emulator, not
mockups. Whole screens and shared component variants are captured in both
light and dark themes: 500 initial views plus 388 scrolling views.
Accessibility verification adds 72 initial views and 66 scrolling views.
The 33 fixtures affected by the final shared-component fixes were recaptured
in both themes after the final changes.
Accessibility checks use a 360 × 800 dp viewport and 100%, 150% and 200% font
scales with Ukrainian app resources. Exercise fixtures include a long Russian
name, pending, partial, completed and empty states.

Evidence directory:
`/Users/maxvoitenko/.codex/visualizations/2026/10/09/01a11fbb-cac4-71d3-b0a8-38b1ef8a5779/design-audit/`

- `gallery.html`: searchable paired light/dark screenshots.
- `final/`: full-resolution screenshots and scrolling views.
- `stress-final/`: small-phone and enlarged-font checks.
- `contacts/`: sheets used for visual inspection.
- `evidence/manifest.json`: exact screen/component fixture-to-source mapping.
- `evidence/component-inventory.md`: full component inventory.
- `evidence/production-verification.log`: production build output.

The clean APK was installed and cold-started after verification. Emulator
settings were restored to 100% font scale, its physical viewport/density,
system dark theme and no app-locale override. The ordinary app launch is
recorded in `live-app-dark.png`; no audit entry points remain in the sources.

The catalog images are preview-state screenshots. Some exercise fixtures intentionally have
no image URL and display the existing fallback. They do not cover every live
server-data combination, keyboard state or navigation transition. iOS device
and simulator targets were compiled; this audit does not claim iOS screenshot
coverage.

## Final cleanup review

- Rechecked the final diff, resource placeholders in EN/RU/UK, obsolete
  component references and temporary capture entry points; none remain.
- The production dimension scan also covered 216 screen-feature and 187
  dialog-feature Kotlin files; no nonzero literal `.dp` values were found.
- Callback-bearing `Leading` and `ExerciseCardStyle` types now use `@Stable`,
  matching the project Compose convention. Immutable singleton variants retain
  `@Immutable`. This final cleanup does not change the visual layout.
- Android and both iOS target compilations were rerun after this cleanup;
  see `evidence/final-cleanup-verification.log`.

Final review rechecked summary edge cases (empty, pending, partial, complete,
zero effective load), toolbar measurement, shared consumers and resource
placeholders. No additional blocking findings. Fresh Android/iOS verification:
`evidence/final-review-verification.log` (`BUILD SUCCESSFUL`).
