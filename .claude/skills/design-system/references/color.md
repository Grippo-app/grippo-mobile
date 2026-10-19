# Semantic color tokens

MUST inspect [AppColor](../../../../design-system/resources/provider/src/commonMain/kotlin/com/grippo/design.resources/provider/AppColor.kt)
and its current [palette](../../../../design-system/resources/provider/src/commonMain/kotlin/com/grippo/design.resources/provider/colors/AppPalette.kt),
[DarkColor](../../../../design-system/resources/provider/src/commonMain/kotlin/com/grippo/design.resources/provider/colors/DarkColor.kt)
and [LightColor](../../../../design-system/resources/provider/src/commonMain/kotlin/com/grippo/design.resources/provider/colors/LightColor.kt).
Source owns exact nested names and types; this reference does not duplicate the API.

Current shape, for orientation: `AppColor` has 29 top-level groups: bottomBar,
button, input, background, dialog, text, divider, timeline, semantic, overlay,
shimmer, border, illustration, bannerCard, welcomeChecklist, brand, icon, toggle,
muscle, segment, konfetti, charts, example, profile, palette, static, context,
selection, stepProgressIndicator. `AppPalette` is an `internal object` holding the
raw literals; `DarkColor` and `LightColor` are `public object : AppColor`. `brand`
is `BrandColors { color1, color2, color4, color5, color6 }` (no `color3`), a numbered
categorical accent set mapped to magenta/coral/teal/sky/indigo in both themes; the
orange brand accent lives in `selection.accent`, `button.backgroundPrimary1`,
`border.focus` and `timeline.marker`, and exercise category colors in
`example.category.compound/isolation`. `muscle.palette6MuscleCalm` and `palette.*`
are plain `List<Color>`; convert them to an immutable collection before storing
them in `@Immutable` state.

- Consumers MUST use AppTokens.colors semantic slots. Raw product color literals
  and state opacity belong to the concrete palette/theme producers, not consumers.
  MUST NOT derive disabled/selected color opacity inline instead of a semantic slot.
  Neutral technical Color.Transparent is permitted; it does not authorize a
  hardcoded product palette.
- Add only a demanded semantic slot; update every implementation. A new group
  requires a real meaning that does not fit the existing groups and authorized scope.
- AppColor owns semantic Color slots, not Brush. For a caller-supplied hue, its
  existing role may expose a demanded opacity or blend coefficient implemented by
  both themes; components may apply that coefficient without replacing the hue.
  Static colors use Color slots. Components may assemble gradients from token
  colors; MUST NOT create a parallel consumer palette.
- Keep the existing nested-group structure; sub-element and variant nesting follows
  the [hierarchy rule in dimension tokens](dp.md). Inspect current chart groups
  rather than inventing signatures.
- Both themes MUST implement the same interface. ProvideResources owns selection;
  components MUST NOT branch on theme to replace that ownership.

Review all affected consumers and [visual usage rules](../../../../design-system/CONSISTENCY.md).
Source compliance does not prove contrast, hierarchy or rendered fidelity.
Follow [visual-consistency.md](visual-consistency.md) for permitted checks.
