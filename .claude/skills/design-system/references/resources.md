# Compose Multiplatform resource ownership

Inspect [the provider build](../../../../design-system/resources/provider/build.gradle.kts),
`design-system/resources/provider/src/commonMain/composeResources/` and the actual
locale directories before editing. Compose Resources generates `Res`; feature UI
resources belong in this shared provider. A feature resource bundle is not an
incidental exception to module ownership: a split requires an explicit architecture
decision, and feature size alone is not permission.

Current shape, for orientation (source owns changes):

- Locales are exactly `values`, `values-uk` and `values-ru`, 759 keys each with
  full key parity. There is no `plurals.xml`. Every format argument is positional.
- `drawable/` holds nine WebP files with alpha and no PNG, all named without
  prefixes: `bench_incline`, `dumbbell`, `plate` (1536 × 1024); `muscles`
  (1024 × 1024); `empty_exercises`, `empty_sets`, `empty_workouts`,
  `error_dialog`, `onboarding_hero` (768 × 768).

## Android packaging

The provider enables `androidResources.enable = true` inside `kotlin { android { ... } }`.
Keep that opt-in: the shared convention does not enable Android resources globally.
Inspect the current source DSL before editing. The notification-manager's Android
resources have their separate platform owner. A build verifies compilation; actual
resource packaging/use needs its relevant inspection or manual scenario.

## Strings and plurals

- Every new user-facing string MUST have a real translation in every existing
  locale, including unfinished features: the agent authors the Ukrainian and
  Russian translations itself for every new key. A missing key or English
  placeholder text in `values-uk`/`values-ru` is a defect.
- Format arguments MUST use explicit positional placeholders (`%1$s`, `%2$d`).
  Locale variants may reorder positions but keep argument indices and types.
  Do not concatenate localized sentence fragments or assume named formatting.
- Keys are descriptive snake_case with a semantic prefix where ambiguous, never
  numbered placeholder names such as `title1`.
- Strings stay in each locale's `strings.xml`. Rich-text presentation belongs in
  the owning AnnotatedString consumer; resources carry no HTML markup.
- When a plural is first needed, add a complete `plurals.xml` `<resources>` document
  in the default and every existing locale, with the quantity categories of each
  specific locale rather than one copied four-category list.
- Composables use AppTokens.strings; ViewModels use StringProvider; resource-backed
  state uses UiText. [accessors.md](accessors.md) owns the accessor boundaries.

## Imagery and fonts

- Product UI icons MUST be AppIcon ImageVector extensions with real path data; a
  raster or drawable substitute for a UI icon is a defect.
- Raster illustrations/photos use WebP by default; PNG needs a documented
  lossless/tooling reason. Names are descriptive snake_case without `ic_`/`img_`/`bg_`
  prefixes, one resource per asset file, no sprite sheets or texture atlases.
- Font file names follow the existing family/weight convention, and the
  registration in the typography factory maps each used weight to its actual file.
  [typography.md](typography.md) owns token/font-factory changes.

For a real addition follow [cookbook-resource.md](cookbook-resource.md). Review
locale key/placeholder parity, actual generated identifiers and affected callers.
Builds follow the default build rule in the
[verification policy](../../../../CLAUDE.md#verification-policy); a green build
proves compilation, not resource packaging or rendering, which
[runtime-verification](../../runtime-verification/SKILL.md) establishes when the
user allows running the app.
