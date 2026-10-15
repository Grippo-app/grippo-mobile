# Typography tokens and font registration

MUST inspect [AppTypography](../../../../design-system/resources/provider/src/commonMain/kotlin/com/grippo/design.resources/provider/AppTypography.kt)
for exact methods, sizes and the current `manrope()` factory. Consumers use
`AppTokens.typography.<method>()`; do not copy a full type-scale snapshot here.

- New styles MUST follow existing `hN()` or `bNNWeight()` naming and a real design
  requirement. MUST NOT introduce inline product TextStyle/fontSize/fontWeight
  variants in consumers, or modify an existing token's role without reviewing its
  callers. Explicit representative preview fixtures follow [previews.md](previews.md);
  MUST NOT turn fixture styling into a production token substitute.
- Typography tokens remain color-agnostic; text color comes from AppTokens.colors.
  MUST NOT create a parallel Material3 Typography or use LocalTextStyle as an
  implicit substitute for an explicit product token.
- Font-resource loading stays in the producer's composable factory. Manrope's
  registered files and weights MUST agree; inspect the actual registrations.
  Add only weights/typefaces required by authorized scope. A second typeface
  needs a real factory and its explicit token consumers, not a generic example.
- Each changed style MUST be reviewed in affected whole-screen consumers in both
  themes at the [standard viewport](visual-consistency.md#default-viewport-scope).
  Extra font scales require an explicit request. Report unrendered in-scope states.

[resources.md](resources.md) owns asset conventions;
[visual usage rules](../../../../design-system/CONSISTENCY.md) own role selection.
MUST NOT infer recomposition cost or accessibility correctness from an annotation.
