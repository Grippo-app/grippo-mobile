# Semantic color tokens

MUST inspect [AppColor](../../../../design-system/resources/provider/src/commonMain/kotlin/com/grippo/design.resources/provider/AppColor.kt)
and its current [palette](../../../../design-system/resources/provider/src/commonMain/kotlin/com/grippo/design.resources/provider/colors/AppPalette.kt),
[DarkColor](../../../../design-system/resources/provider/src/commonMain/kotlin/com/grippo/design.resources/provider/colors/DarkColor.kt)
and [LightColor](../../../../design-system/resources/provider/src/commonMain/kotlin/com/grippo/design.resources/provider/colors/LightColor.kt).
Source owns exact nested names and types; this reference does not duplicate the API.

- Consumers MUST use AppTokens.colors semantic slots. Raw product color literals
  and state opacity belong to the concrete palette/theme producers, not consumers.
  MUST NOT derive disabled/selected color opacity inline instead of a semantic slot.
  Neutral technical Color.Transparent is permitted; it does not authorize a
  hardcoded product palette.
- Add only a demanded semantic slot; update every implementation. A new group
  requires a real meaning that does not fit the existing groups and authorized scope.
- AppColor stores Color slots, not Brush. Components may assemble gradients from
  token colors; MUST NOT create a parallel consumer palette.
- Keep the existing nested-group structure. Prefer shallow semantic paths; inspect
  current deeper chart groups rather than flattening or inventing signatures.
- Both themes MUST implement the same interface. ProvideResources owns selection;
  components MUST NOT branch on theme to replace that ownership.

Review all affected consumers and [visual usage rules](../../../../design-system/CONSISTENCY.md).
Source compliance does not prove contrast, hierarchy or rendered fidelity.
Follow [visual-consistency.md](visual-consistency.md) for permitted checks.
