# Compose Multiplatform resource ownership

MUST inspect [the provider build](../../../../design-system/resources/provider/build.gradle.kts),
`design-system/resources/provider/src/commonMain/composeResources/` and the actual
locale directories before editing. Compose Resources generates Res; feature UI
resources belong in this shared provider. MUST NOT introduce a feature resource
bundle as an incidental exception to module ownership. A split requires an explicit
architecture decision; feature size alone is not permission.

## Android packaging

The provider currently enables `androidResources.enable = true` inside
`kotlin { android { ... } }`. MUST retain that opt-in: the shared convention does
not enable Android resources globally. Inspect the pinned source DSL before
editing; MUST NOT copy the former `androidLibrary` recipe as the current signature.
The notification-manager's Android resources have their separate platform owner.
A build verifies compilation; actual resource packaging/use requires its relevant
inspection or manual scenario, not an inferred runtime success.

## Strings and plurals

- Every new user-facing string/plural MUST have a real translation in every existing
  supported locale, including unfinished features. No silent English fallback.
- Keys MUST be descriptive snake_case, with a semantic prefix where ambiguous;
  MUST NOT use numbered placeholder names such as title1.
- Format arguments MUST use explicit positional placeholders (`%1$s`, `%2$d`).
  Locale variants may reorder positions but MUST preserve argument indices/types.
  MUST NOT concatenate localized sentence fragments or assume named formatting.
- Strings remain in each locale's strings.xml. Rich-text presentation belongs in
  the owning AnnotatedString consumer; MUST NOT insert HTML markup into resources.
- The repository currently has no plurals.xml. Add a complete `<resources>` document
  in default and every existing locale only when needed. Quantity categories MUST
  follow the specific locale's plural rules, not one copied four-category list.
- Composables use AppTokens.strings; ViewModels use StringProvider; resource-backed
  state uses UiText. [accessors.md](accessors.md) owns the actual accessor boundaries.

## Imagery and fonts

- Product UI icons MUST be AppIcon ImageVector extensions with real path data.
  Raster illustrations/photos use WebP by default; PNG needs a documented
  lossless/tooling reason. MUST NOT substitute raster/vector drawables for UI icons.
- Raster names MUST be descriptive snake_case without ic_/img_/bg_ prefixes.
  Keep one resource per asset file; no sprite sheets or texture atlases.
- Font file names MUST match the existing family/weight convention. Registration
  MUST map each used weight to its actual file in the existing typography factory.
  [typography.md](typography.md) owns token/font-factory changes.

For a real addition follow [cookbook-resource.md](cookbook-resource.md). Review
locale key/placeholder parity, actual generated identifiers and affected callers.
Use scoped checks under [verification policy](../../../../CLAUDE.md#verification-policy);
MUST NOT require full app builds for every isolated resource/document edit or claim
resource runtime coverage from a green build.
