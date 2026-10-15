# Typography tokens and font registration

Inspect [AppTypography](../../../../design-system/resources/provider/src/commonMain/kotlin/com/grippo/design.resources/provider/AppTypography.kt)
before adding or changing a style; consumers call `AppTokens.typography.<method>()`.
Current shape, for orientation (source owns changes): `public data object AppTypography`
with headings `h1()`…`h6()` and body styles named `b<size><Weight>()` with the
abbreviated weight suffixes `Bold`, `Semi`, `Med`, `Reg`: `b15Med`, `b14Bold`,
`b14Semi`, `b14Med`, `b13Semi`, `b13Bold`, `b13Med`, `b12Semi`, `b12Med`, `b11Bold`,
`b11Semi`, `b11Med`, `b11Reg`, `b10Bold`, `b10Semi`, `b10Med`, `b10Reg`. `manrope()`
is a top-level `internal fun` in the same file, not a member; it registers seven
Manrope TTFs (ExtraLight, Light, Regular, Medium, SemiBold, Bold, ExtraBold), of
which ExtraBold, ExtraLight and Light are used by no token.

- A new style follows the existing `hN()` / `b<size><Weight>()` naming and needs a
  real design requirement. Product text in consumers MUST use a token: an inline
  TextStyle/fontSize/fontWeight variant in a consumer is a defect. Changing an
  existing token's role requires reviewing its callers. Preview fixtures may use
  explicit sample text ([previews.md](previews.md)); fixture styling is not a
  production token substitute.
- Typography tokens are color-agnostic; text color comes from AppTokens.colors.
  Do not create a parallel Material3 Typography or use LocalTextStyle as an
  implicit substitute for an explicit product token.
- Font-resource loading stays in `manrope()`. Registered files and weights MUST
  agree; add only the weights/typefaces the authorized scope needs. A second
  typeface needs a real factory and explicit token consumers, not a generic example.
- Review each changed style in affected whole-screen consumers in both themes at
  the [standard viewport](visual-consistency.md#default-viewport-scope); extra font
  scales require an explicit request. Report unrendered in-scope states under
  [visual evidence](visual-consistency.md#evidence).

[resources.md](resources.md) owns asset conventions;
[CONSISTENCY.md](../../../../design-system/CONSISTENCY.md) owns role selection.
An annotation does not establish recomposition cost or accessibility correctness.
