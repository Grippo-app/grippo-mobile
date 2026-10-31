# Typography tokens and font registration

Inspect [AppTypography](../../../../design-system/resources/provider/src/commonMain/kotlin/com/grippo/design.resources/provider/AppTypography.kt)
before adding or changing a style; consumers call `AppTokens.typography.<method>()`.
Current shape, for orientation (source owns changes): `public data object AppTypography`
with headings `h1()`…`h6()` and body styles named `b<size><Weight>()` with the
abbreviated weight suffixes `Bold`, `Semi`, `Med`, `Reg`; inspect the producer for
the current method inventory. `onest()`
is a top-level `internal fun` in the same file, not a member; it registers four
static Onest TTFs (Regular, Medium, SemiBold, Bold), matching the weights used by
the tokens.

- A new style follows the existing `hN()` / `b<size><Weight>()` naming and needs a
  real design requirement. Product text in consumers MUST use a token: an inline
  TextStyle/fontSize/fontWeight variant in a consumer is a defect. Changing an
  existing token's role requires reviewing its callers. Preview fixtures may use
  explicit sample text ([previews.md](previews.md)); fixture styling is not a
  production token substitute.
- A layout that must match or reserve the height of laid-out text of a role - a
  skeleton text bone, a `heightIn(min)` slot that keeps one or two lines stable
  across loading/empty/populated states, an icon aligned to a text line - takes it
  from [`rememberTextHeight(style, lines)`](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/utils/TextHeight.kt)
  (`lines` defaults to 1). A composable measuring several roles hoists one
  `rememberTextMeasurer()` and passes it to each call. MUST NOT derive a text
  height from `style.lineHeight`/`fontSize` (Android trims the outer lines, so the
  nominal value is taller than the rendered text) or hand-tune a dp token for it.
- Typography tokens are color-agnostic; text color comes from AppTokens.colors.
  Do not create a parallel Material3 Typography or use LocalTextStyle as an
  implicit substitute for an explicit product token.
- Font-resource loading stays in `onest()`. Registered files and weights MUST
  agree; add only the weights/typefaces the authorized scope needs. A second
  typeface needs a real factory and explicit token consumers, not a generic example.
- Review each changed style in affected whole-screen consumers in both themes at
  the [standard viewport](visual-consistency.md#default-viewport-scope); extra font
  scales require an explicit request. Report unrendered in-scope states under
  [visual evidence](visual-consistency.md#evidence).

[resources.md](resources.md) owns asset conventions;
[CONSISTENCY.md](../../../../design-system/CONSISTENCY.md) owns role selection.
An annotation does not establish recomposition cost or accessibility correctness.

## Empty-state text

For empty, no-results and missing-data states that replace a content region,
keep the optional title and explanatory message as distinct roles, using the
[empty-state mapping](../../../../design-system/CONSISTENCY.md#empty-state-text).
The message keeps its role when the title is omitted. Screen, sheet, section and
card hosts, and illustration sizes, MUST NOT select different typography for the
same role. Rendering belongs to the [shared empty-state family](components.md#empty-states).
Inline unavailable metric values, status labels and input placeholders retain
their own roles.

Before completing an empty-state change, inspect every affected title/message,
including text-only states and inherited component defaults, against that mapping.
