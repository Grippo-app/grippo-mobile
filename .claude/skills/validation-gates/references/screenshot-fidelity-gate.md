# Screenshot-fidelity gate — capture spec + `figma-screenshot-validator` surface

<!-- Source of truth pinned by orchestrator/contracts/agents/figma-screenshot-validator.md.
     Capture/oracle contract verified against orchestrator/figma/scripts/compare-screenshots.mjs
     (variantsFor/findCaptures/manifestEntry/staleByStartedAt) and evidence-bundle.mjs. Roborazzi
     wiring mirrors platform-build-toolkit/references/convention-plugins.md § screenshot.test.convention
     and tech-stack.md § Screenshot test. Metric internals: orchestrator/figma/scripts/compare-screenshots.mjs
     (masked-ssim-luma-v2). -->

This file is the missing half of the gate: **how the app renders its own
screenshot (the "actual") deterministically**, so it can be compared to the
pulled Figma design (the "oracle"). Without a deterministic capture the metric
score is non-reproducible run-to-run and cannot be calibrated — that determinism
is the whole point of this spec.

The metric side is owned by `orchestrator/figma/scripts/compare-screenshots.mjs`
(`masked-ssim-luma-v2`); this file owns the **capture** side and the
**validator's frozen surface**.

---

## 1. When the gate runs

- **Mandatory** for every `figmaEnabled` task that declares a non-`none`
  `## Design` node (`[screen]` / `[dialog]` / `[component]` / `[overlay]`). The
  pulled oracle (`<Screen>.png` / `<Screen>.dark.png`) and its Roborazzi capture
  are **required inputs, not run-conditions**: a declared design node whose
  oracle or capture is missing is a **BLOCKER** (`MISSING_ORACLE` / `MISSING_CAPTURE`),
  never a silent skip — the comparison must run. It **self-skips only for a non-UI task**
  (no `figmaEnabled` / no non-`none` `## Design` bullet), which pays nothing. (Note: the
  `ship-done`/`verify-done` UI-by-evidence backstop (`design-parser.uiTaskWithoutDesign`)
  hard-blocks before `done/` — un-opt-out-able for a proven Figma node (a cited node URL, or a
  `designComponentId:` + `figmaNodeId:`/`frozenStructuralHash:` machine
  snapshot); and, structurally, a **newly CREATED UI-widget source file**
  (a `.kt`/`.swift` file at ANY depth under a `components/` dir — minus the unambiguously non-UI
  leaf dirs utils/di/mappers/modifiers — OR a screen/dialog file) with no design bullet is BLOCKED
  unless a **per-widget** audited `- <Name> — none (<why>)` naming THAT widget accounts for it —
  so a new feature-local card can no longer self-classify non-UI and ship uncompared, and one
  unrelated `— none` cannot disarm a co-resident card. The created class is **fail-closed**: a
  `### Files touched` bullet whose status word is absent or not an explicit *modified/renamed* is
  treated as created (it cannot slip to WARN on an unwritten token). A **MODIFIED** UI-widget file
  with no node cited stays an advisory NOTE (a non-visual copy/callback edit must not hard-block),
  task-globally opt-out-able via one audited `— none (<why>)` — that narrower class is the
  accepted residual. A card rendered inside a declared screen is compared
  transitively (it is in the screen's capture; content-parity catches divergence).)
- It is a **post-build** gate: it runs after Step 4.5/4.6 assemble succeeds, as the
  `figma-screenshot-validator` step (routed by `task-orchestrator` →
  `references/validator-routing.md`).
- **Run in `--gate` mode** (the `figma-screenshot-validator` step always passes `--gate`):
  missing/incomplete/blocking visual evidence is a non-zero pre-flight failure (exit 2),
  routed back through the fix path. (The bare advisory invocation only annotates and is
  NOT what the gate uses.)
  Whether a pixel-SIMILARITY divergence (SSIM band / per-zone floor / colour) BLOCKS is
  routed by the per-project `screenshotPixelGate` knob (**default `strict`** → may block;
  `advisory` → WARN, never blocks; `off` → suppressed) — see **§7.3**. The completeness /
  anti-forgery blockers and the structural gate are NEVER routed (always block). Under
  `strict`, the `MAJOR` band blocks (`SSIM_MAJOR`; the §6.5
  dark-regime calibration; `SCREENSHOT_MAJOR_BAND=advisory` is the metric-internal rollback).

---

## 2. The capture harness — `ScreenshotTest.kt`

A Robolectric host test in the feature module's **`androidHostTest`** source set,
enabled by the inert `id("screenshot.test.convention")` plugin (Roborazzi +
Robolectric 4.16 + JUnit4, JDK-21 launcher — `convention-plugins.md`). It renders
each `## Design` node's composable in **the state that bullet declares** (one `@Test`
per bullet — one bullet PER state the design provides, see §2.1; plus a dark capture
only when a dark oracle exists) and writes one PNG per capture with the exact
`<oracleBase>Screenshot.png` name the comparator expects.

**Determinism is the contract.** Two runs of the same commit MUST produce
byte-identical PNGs. That requires:

- **Fixed screen geometry + density per capture** via a Robolectric qualifier derived
  from EACH bullet's own oracle `frameSizeDp` (see §3 — the class-level `@Config` is
  only a shared default; mixed geometries need method-level `@Config`) — never the
  host default.
- **Fixed locale + layout direction — never the host default.** Robolectric defaults
  to `en-rUS`/`ldltr`; the capture locale MUST match the oracle's design language,
  pinned via the `@Config` qualifier's locale segment, plus `ldrtl` for an RTL design
  (see §3). The design language is DERIVED, not guessed: the committed
  `designLocale` config key when declared, else deterministic detection from the pulled
  spec texts × the app's string resources (`lib/design-locale.mjs`) — and the mismatch
  is mechanically NAMED, on both flanks: `check-capture-config --gate` blocks a
  wrong/absent locale segment before the render (the same `CAPTURE_CONFIG_MISSING`/
  `CAPTURE_CONFIG_DRIFT` class; underivable → `CAPTURE_LOCALE_UNDERIVABLE`, remedy:
  declare `designLocale`), and the comparator cross-checks the manifest's `localeTag`
  against the design language (`CAPTURE_LOCALE_MISMATCH`, always-BLOCKER — §4). An RTL
  design captured LTR is a guaranteed structural fail; the derived qualifier for an
  RTL language carries `ldrtl` by construction.
- **No animations / no time:** render a settled state; disable indeterminate
  progress, blinking carets, and any `LaunchedEffect`-driven animation. Use a fixed
  test clock if the screen reads time. The controls are not prose to remember — they
  are the mandatory `PreviewContainerScreenshot` wrapper + the forbidden-call list of
  **§2.2**; a capture that reaches for a live clock / animation / network image is a
  builder defect, not a gate to relax.
- **Every declared state, each vs its OWN oracle:** a screen's loaded state is the
  primary; when the design ALSO provides empty / error / loading state frames, each is
  its own `## Design` bullet, its own oracle, and its own `@Test` — see **§2.1**. Feed
  each capture its state via fakes, never the network. Record `primaryState: true` in
  the manifest (§4) on EVERY capture entry — the flag means "this capture is the settled
  target render for ITS OWN bullet's oracle" (a HomeEmpty capture is the primary render
  of the HomeEmpty oracle), NOT "this is the loaded state". The comparator's
  state-evidence check (`PRIMARY_STATE_UNCONFIRMED`, a gate-mode BLOCKER) rejects
  `primaryState: false` and any transient-sounding `state:` label. Explicit negative or
  transient evidence wins over positive aliases (`primaryState: true` cannot override
  `loaded: false` or `state: error`) — never mark a declared state bullet's capture as
  non-primary.
- **Content parity with the oracle:** the primary loaded state must render
  representative content mirroring the Figma frame — never a skeleton/empty state.
  When the oracle shows imagery, Coil/`AsyncImage` slots need deterministic fake
  images (a fixed test bitmap via the preview image-loader), NOT the flat colour
  placeholder: the comparison mask is built from the ORACLE, so designer photos and
  copy are always compared, and a flat box or a mismatched generic stub string scores
  near-zero SSIM in those zones. This is an honest limitation with a defined remedy —
  a capture whose content diverges from the oracle's content is a **legitimate
  BLOCKER/MAJOR** the builder fixes by ENRICHING the stub (a screenshot-only stub
  override next to the `@Test`, text/images aligned to the pulled `<Screen>.spec.json`
  frame content — do not mutate the shared `@AppPreview` stubs), never by skipping the
  capture or relaxing the gate. **Mechanical helper:** the pulled spec records each
  text node's visible content verbatim (`elements[].text` — the pull contract), and
  `node orchestrator/figma/scripts/check-stub-text.mjs <stem>` searches the code for
  exactly those strings — an invented stub value ("312 Mbps" where the design shows
  "128 Mbps") surfaces as a named `TEXT_NOT_IN_CODE` WARN instead of an unexplained
  low-SSIM text zone. Advisory (runtime-composed strings legitimately split design
  text); a spec pulled before the text contract simply reports nothing.
- **No real system UI:** capture the **screen composable directly** (not an Activity
  window), so there is no real status bar / nav bar. The comparator **auto-normalizes
  this by construction** (§3.6): for a `[screen]` it normally excludes bounded top
  status-bar + bottom nav-bar bands on BOTH oracle and capture. A geometry-only top strip
  that cannot be proven to be system chrome is deliberately compared instead of masked;
  resolve the named `IOS_CHROME_SUSPECTED` finding rather than hand-normalizing the capture.
  (A large layout OFFSET beyond the band still exceeds the ±2 px shift search, so keep
  the composable's own top content aligned to the frame.)
- **Bundled fonts:** rely on Robolectric's bundled fonts; do not load device fonts.

**Canonical body.** The test lives in the
screen's own package so the `internal` composable is visible, and the capture is
derived MECHANICALLY from the screen's primary `@AppPreview`: copy its stub-state
construction verbatim as the starting point (when the shared stub's content diverges
from the oracle, the content-parity rule above requires a screenshot-only enriched
stub), swap `PreviewContainer` → `PreviewContainerScreenshot`
(full-bleed), and wrap it in `captureRoboImage(<oracleBase>Screenshot.png)`.

```kotlin
// <featureModule>/src/androidHostTest/kotlin/<screen-package>/ScreenshotTest.kt
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)              // pixel-accurate raster, not the shadow canvas
@Config(sdk = [34], qualifiers = "w390dp-h844dp")    // ← SHARED DEFAULT only — each @Test derives from ITS bullet's frameSizeDp, see §3
class ScreenshotTest {

    // One @Test per `## Design` bullet (per pulled oracle). The capture file name is a
    // HARD contract (§4): "<oracleBase>Screenshot.png"  (dark oracle → "<oracleBase>Screenshot.dark.png").
    @Test
    fun exampleScreen() {
        captureRoboImage("build/outputs/roborazzi/ExampleScreenScreenshot.png") {
            PreviewContainerScreenshot {                 // full-bleed (mirrors the @AppPreview's PreviewContainer)
                ExampleScreen(                           // the screen's PRIMARY @AppPreview stub, copied verbatim
                    state = ExampleState(/* loaded fixture from the primary @AppPreview */),
                    loaders = persistentSetOf(),
                    contract = ExampleContract.Empty,
                )
            }
        }
    }
}
```

- `captureRoboImage(path) { … }` is the `roborazzi-compose` entrypoint; the trailing
  lambda is the composable content. The path is the module's Roborazzi output dir
  (`build/outputs/roborazzi/`) — the dir the validator points `ROBORAZZI_OUTPUT_DIRS`
  at (§4). `PreviewContainerScreenshot` lives in `:design-system:preview`.
- **One `@Test` per `## Design` bullet** (the `## Design` section lists which nodes
  have an oracle). Multi-state variant frames of the same composable each get their
  own `@Test` with the matching state + capture name; a `dialog`/`component` bullet
  renders that composable in isolation (the bullet's node `kind`, see the design
  grammar — container + geometry rules below). A screen that is NOT declared in `## Design` (or `## Design: none`) gets no
  capture — a legit self-skip; a **declared** screen whose oracle was not pulled is a
  `MISSING_ORACLE` BLOCKER (§1), not a benign skip.
  - **`[overlay]`/`[dialog]` bullets have their own capture shapes — see §3.5.** A full-frame
    overlay oracle (dimmed host + scrim + a popup/sheet) can NOT be matched by a bare-isolation
    capture; scaffolding one against a host-inclusive frame is exactly the
    `UNREPRESENTABLE_OVERLAY` / low-SSIM failure §3.5 exists to prevent. Pick shape A or B by
    the decision rule there — do not default a `[dialog]`/`[overlay]` bullet to the [component]
    or [screen] recipe.
- **Container + geometry by kind (mixed-geometry tasks).** The class-level `@Config`
  is only a **shared default**; when a task's bullets carry different `frameSizeDp`
  (e.g. a 390×844 `[screen]` + a 340×250 `[dialog]` + a 343×120 `[component]`), each
  `@Test` MUST pin its own geometry with a **method-level**
  `@Config(qualifiers = "w<W>dp-h<H>dp")` derived from ITS bullet's oracle
  `frameSizeDp` (Robolectric applies a method-level `@Config` over the class default);
  "one test class per distinct `frameSizeDp`" is the allowed alternative shape.
  Containers follow the kind: a `[screen]` bullet renders **full-bleed** in
  `PreviewContainerScreenshot`; a `[dialog]`/`[component]` bullet must NOT fill the
  screen-sized viewport — render it **wrap-content or explicitly sized to its own
  frame** (a `PreviewContainerComponent`-style sibling: same `AppTheme` + Coil
  handler, but `Modifier.size(<W>.dp, <H>.dp)` / wrap-content from the bullet's
  `frameSizeDp`) under its own qualifier. A dialog/component captured inside the
  full-bleed screen container at the class qualifier fills the wrong canvas and
  hard-bails `ASPECT_MISMATCH` (§3) every cycle.
- **Single capture per oracle.** Emit a dark capture (`<oracleBase>Screenshot.dark.png`)
  ONLY when a `.dark.png` oracle exists or the index node declares `darkUrl`/`darkNodeId`
  — a single-theme (e.g. all-dark) product captures once; do not emit an unconditional
  dark `@Test` (it would only produce a `MISSING_ORACLE`). Render each theme per §2.2's
  `darkTheme` rule: the primary capture passes the theme its oracle shows, the
  `.dark.png` capture always passes `darkTheme = true`.
- **Single-theme product vs a two-theme bullet — STOP AND ASK.** When only one palette
  exists (e.g. the reference's `DarkColor`-only wiring — no light palette; design-system
  skill, `references/theme.md`) and a `## Design` bullet declares `light:<url> dark:<url>`,
  the light capture cannot be honestly rendered. The builder STOPS and asks the owner —
  declare the design single-theme (drop the `light:` tag) or build the light palette
  first — and NEVER renders the dark palette against a light oracle (a guaranteed
  chronic colour fail, §2.2 theme rule).
- **Run:** `./gradlew :<module>:recordRoborazzi<SourceSet>` (KMP Android host test →
  `recordRoborazziAndroidHostTest`, NOT `recordRoborazziDebug`); export the task start
  as `SCREENSHOT_CAPTURE_STARTED_AT`. The module opts in via `id("screenshot.test.convention")`.

---

## 2.1 Multi-state capture — the default, not the happy path

Pixel-perfection must hold in **every state the design declares** — loaded, empty,
error, loading — not just the loaded happy path. This is the DOCUMENTED DEFAULT.

**The rule.** For each screen whose design provides state frames (loaded / empty /
error / loading), the builder MUST scaffold **one capture `@Test` per state**, each
compared against **ITS OWN oracle**. The default is *"capture every state the design
declares,"* NOT *"invent states."* A state the design does NOT provide a frame for is
**not captured** — there is nothing to compare, and you never fabricate an oracle for
it.

**Authoring — separate `## Design` bullets, no new grammar.** Each state is declared
as its own `## Design` bullet with a distinct screen name and its own node URL:

```markdown
## Design
- HomeLoaded — https://figma.com/…?node-id=…    (primary — loaded, compared vs its oracle)
- HomeEmpty  — https://figma.com/…?node-id=…    (empty-state frame)
- HomeError  — https://figma.com/…?node-id=…    (error-state frame)
```

The existing per-screen mechanism already treats each bullet as its own
screen + oracle + capture (§2 "One `@Test` per `## Design` bullet", §4 filename
contract) — so multi-state needs **no new machinery**. The owner / `task-prep` simply
declares the states that matter as distinct bullets; a bullet the design does not
provide a frame for is simply not written. (`## Preview states` in the task file is the
advisory signal for which states `screen-builder` covers — the captured set is the set
of `## Design` bullets.)

**Deterministic per-state stubs.** The `@Test` for each state must render **THAT
state deterministically** — the empty stub for the empty bullet, an error stub for the
error bullet, the loaded/success stub for the primary bullet — each built by the same
content-parity rule as the primary (§2 "Content parity with the oracle": mirror the
pulled `<Screen>.spec.json` frame content; screenshot-only enriched stub next to the
`@Test`, never a mutation of the shared `@AppPreview` stubs). Overlay/dialog states
follow §3.5 (composed-over-host vs popup-only) exactly as any bullet does.

**Honesty.** Capturing every state is only possible when the design HAS that state's
frame. Missing a state the design DID declare leaves that bullet's oracle
uncompared — a `MISSING_CAPTURE` BLOCKER (§1/§4), fixed by scaffolding the state's
`@Test`, never by dropping the bullet.

---

## 2.2 Deterministic capture wrapper + forbidden calls (mandatory)

Determinism is the contract (§2). To make *"forgot to stub"* structurally unlikely,
the controls are a single **copy-paste wrapper the builder MUST use**, plus a
**forbidden-call list**, plus a **build-side scan** — not prose to remember.

**The wrapper — `PreviewContainerScreenshot` bakes the four controls in.** Extend the
existing `PreviewContainerScreenshot` (§2) so every capture inherits, by construction:

- a **FROZEN clock** — a fixed `Clock`/`Instant` provided into the composition (e.g.
  via a `CompositionLocal` the screen reads), NEVER `LocalDate.now()` /
  `System.currentTimeMillis()` / `Instant.now()` inside the composable under test;
- **animations DISABLED** — no infinite / enter / exit animation running at capture:
  render the **settled** state, so the PNG is the resting frame. `AppTheme` has NO
  animations parameter (its real signature is `AppTheme(darkTheme, localeTag, content)` —
  design-system skill, `references/theme.md`); the wrapper disables animations by its
  own means — a preview-module composition local (`LocalAnimationsEnabled provides
  false`) that motion-aware components read to force `snap()` / zero-duration specs;
- a **DETERMINISTIC image loader** — a fake/test Coil `ImageLoader` returning fixed
  drawables (the preview image-loader, §2 content-parity), NEVER a network/async load;
- a **FIXED locale + layout direction** — supplied by the `@Config` qualifier's locale
  segment (`ldrtl` for RTL), per §2/§3 — NEVER `Locale.getDefault()`.

```kotlin
// :design-system:preview — the ONE wrapper every capture uses. Bakes in the 4 controls.
// AppTheme's REAL signature is AppTheme(darkTheme: Boolean, localeTag: String, content)
// (design-system skill, references/theme.md) — it has NO animations parameter.
@Composable
public fun PreviewContainerScreenshot(
    darkTheme: Boolean = true,                                            // THEME — pass what the ORACLE shows (rule below)
    modifier: Modifier = Modifier,
    clock: Clock = Clock.fixed(FIXED_CAPTURE_INSTANT, ZoneOffset.UTC),   // FROZEN — never Clock.System.now()
    imageLoader: ImageLoader = fakeCaptureImageLoader(),                 // DETERMINISTIC — fixed drawables, no network
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalClock provides clock,
        LocalImageLoader provides imageLoader,                           // Coil reads THIS loader
        LocalAnimationsEnabled provides false,                           // animations DISABLED — settled frame (preview-module local)
    ) {
        AppTheme(darkTheme = darkTheme, localeTag = "en") {              // REAL AppTheme signature — theme set by the ORACLE
            Box(modifier.fillMaxSize()) { content() }                    // locale/RTL come from the @Config qualifier (§3)
        }
    }
}
```

**THEME RULE — `darkTheme` is set per-`@Test` by the ORACLE, not by habit.**
`PreviewContainerScreenshot` / `PreviewContainerComponent` take a `darkTheme: Boolean`
parameter. The `@Test` capturing `<oracleBase>Screenshot.png` (the primary oracle)
passes the theme the ORACLE shows — `darkTheme = false` for a `light:` oracle,
`darkTheme = true` for a dark-only / primary-dark design. The `@Test` capturing
`<oracleBase>Screenshot.dark.png` ALWAYS passes `darkTheme = true`. A theme mismatch
(light oracle compared against a dark render) is a builder defect that surfaces as
chronic colour/ΔE00 diffs across the whole frame — fix the wrapper parameter, never
the gate.

```kotlin
// the @Test applies the fixed locale + layout direction via @Config (§3):
@Test
@Config(qualifiers = "ar-ldrtl-w390dp-h844dp")       // FIXED locale + RTL — never Locale.getDefault()
fun homeError() {
    captureRoboImage("build/outputs/roborazzi/HomeErrorScreenshot.png") {
        PreviewContainerScreenshot { HomeScreen(state = stubHomeError()) }   // frozen clock + fake images inherited
    }
}
```

The wrapper is the ONE construct — a `[dialog]`/`[component]`'s
`PreviewContainerComponent` sibling (§2/§3.5) carries the same four controls (the same
`darkTheme`-parameterised `AppTheme(darkTheme, localeTag)` call +
`LocalClock`/`LocalImageLoader`/`LocalAnimationsEnabled`), differing only in fill
(`Modifier.size(...)` vs full-bleed).

**FORBIDDEN in the composable under test.** None of the following may appear inside a
captured composable (or the stub it renders):

- `LocalDate.now()`, `LocalDateTime.now()`, `Instant.now()`, `System.currentTimeMillis()`, `Clock.System.now()` — read the frozen `LocalClock` instead;
- `Random()` / `Random.nextInt`/`nextX` **without a fixed seed** — seed it (`Random(0)`);
- `rememberInfiniteTransition` (or any live-driven `animate*AsState` at capture) — render the settled frame;
- live `AsyncImage` / a network Coil load — use the fake capture `ImageLoader`;
- `Locale.getDefault()` — the locale is the `@Config` qualifier.

Any of these makes the capture **NON-DETERMINISTIC** → the byte-hash differs
run-to-run → the gate surfaces it as `STALE_CAPTURE` / re-run instability. That is a
**builder defect, fixed by using the wrapper — NEVER by disabling or relaxing the
gate.**

**Mechanical enforcement — the convention plugin owns the scan (build-side).** The
`screenshot.test.convention` Gradle plugin SHOULD carry a source-scan check — a small
Gradle task (or a detekt/lint rule) — that **fails the build** when a
`*ScreenshotTest.kt` references any forbidden call above, so the determinism rule is
enforced at build, not by memory. **Honest scope:** this file specifies the
*contract* (what the scan forbids); the actual Gradle task / detekt rule is scaffolded
**per-project** in `build-logic` alongside the plugin
(`convention-plugins.md` § `screenshot.test.convention`) — until it is wired, the
forbidden list is a builder obligation the reviewer enforces, and the frozen-hash
re-run instability is the runtime backstop. The plugin is the intended owner of the
check.

---

## 3. Density / qualifier derivation

The render and the oracle must share an **aspect ratio** or the score measures
resampling noise, not fidelity — and the host default (≈320×470, near-square) does
NOT match a tall phone frame, so the comparator bails with `ASPECT_MISMATCH` (verified
by the comparator fixtures). **MANDATORY — the `@Config` qualifier is derived from the oracle's
`frameSizeDp`, never the host default.** A bare `@Config(sdk = [34])` (no `qualifiers`)
renders at ≈320×470 and yields `ASPECT_MISMATCH` for any real phone frame — so the
scaffold MUST emit the `frameSizeDp`-derived qualifier for every screen; omitting it is a
gate failure, not a stylistic choice. **This is enforced mechanically, not just here:**
`check-capture-config.mjs <stem> --gate` statically matches each spec's `frameSizeDp` to the
capturing `@Test` and BLOCKS on a missing/drifted qualifier (`CAPTURE_CONFIG_MISSING` /
`CAPTURE_CONFIG_DRIFT` — including the locale segment, with `CAPTURE_LOCALE_UNDERIVABLE`
when the design language cannot be derived and is not declared); the orchestrator runs it before the render (Step 4.6b·0) and routes
failures to the owner builder. `--fix` is a manual/builder-owned repair mode, not part of the
validator run loop. Derive it from the oracle's `frameSizeDp` (`W×H`, present per screen in the
resolved spec / screen-index):

```
qualifiers = "w<W>dp-h<H>dp"            // e.g. frameSizeDp 390×844 → "w390dp-h844dp"  (verified)
// optional density for a crisper high-res render the comparator downscales — [screen] kind ONLY:
// qualifiers = "w<W>dp-h<H>dp-xxhdpi"
// ([dialog]/[component]/[overlay] keep the default mdpi: their native capture size is checked
//  against frameSizeDp at 1px == 1dp, so a density suffix trips KIND_GEOMETRY_MISMATCH)
// non-EN design: PREPEND the locale segment (Robolectric defaults to en-rUS — see §2):
// qualifiers = "<lang>-r<REGION>-w<W>dp-h<H>dp"        // e.g. Russian design → "ru-rRU-w390dp-h844dp"
// RTL design: add the layout-direction qualifier after the locale:
// qualifiers = "<lang>-r<REGION>-ldrtl-w<W>dp-h<H>dp"
```

**Per-bullet derivation (mixed geometry).** Each `@Test`'s qualifier derives from
ITS bullet's own oracle `frameSizeDp` — the class-level `@Config` is only the shared
default for when every bullet in the class shares one frame. When geometries differ,
use a method-level `@Config(qualifiers = …)` per `@Test`, or split into one test
class per distinct `frameSizeDp` (§2 "Container + geometry by kind") — picking
neither guarantees `ASPECT_MISMATCH` for every non-matching bullet.

**Locale is part of the derivation, not an implicit default — and it is derived
mechanically.** The qualifier's locale segment is set to the oracle's design
language; the expected qualifier is `[<locale>[-ldrtl]-]w<W>dp-h<H>dp`. The design
language comes from ONE of two deterministic sources: the committed `designLocale`
config key (project-config; must be one of `supportedLocales`) when declared, else
detection by `lib/design-locale.mjs` — the pulled specs' `elements[].text` votes
against each supported locale's `values*/strings.xml` (position formatters like
`%1$s` are wildcards; the winner must match ≥ 2 strings and strictly beat every
other locale). `check-capture-config --gate` enforces the segment exactly like the
geometry (a wrong/absent locale is the same `CAPTURE_CONFIG_MISSING`/`CAPTURE_CONFIG_DRIFT`
BLOCKER class; `--fix` repairs the segment surgically). Fail-closed: votable design
text with no decisive locale — or a declared locale outside `supportedLocales` — is
`CAPTURE_LOCALE_UNDERIVABLE` with the named remedy (add `designLocale:` to
project-config, or enrich the design's text layers); a textless spec carries
no locale signal, so only geometry is enforced. An RTL language (`ar`/`he`/`fa`/`ur`)
additionally requires `ldrtl` in the derived qualifier — now enforced, not prose.

This pins the render to the design's dp frame (aspect matches; the density suffix is
optional — bare/mdpi is the verified minimum, `-xxhdpi` just renders larger). The comparator's
`masked-ssim-luma-v2` still **records-and-rescales** if the oracle's exported pixel
size differs (it downscales the larger toward the smaller and records `scaleFactor`)
— so a residual scale mismatch degrades gracefully instead of failing.
**Convention:** the Figma oracle is taken as-exported and normalized in the
comparator (`get_screenshot` is not assumed to honor an export-scale argument);
do not depend on a matched export scale.

---

## 3.5 Overlay / dialog capture — the two shapes

**The Figma reality.** An overlay/dialog oracle is usually a **FULL-FRAME** node — the
popup/sheet drawn OVER a dimmed host screen (the scrim), not the popup alone. An isolated
bare-popup capture against such a frame misses the host context → a misleading compare, and
a declared `[overlay]` with no representable capture is a fail-closed `UNREPRESENTABLE_OVERLAY`
BLOCKER (§4). `UNREPRESENTABLE_OVERLAY` remains the fail-closed outcome ONLY while no composed
capture exists — verified against `compare-screenshots.mjs`: the `kind` merely relabels a
MISSING capture, so a present capture under the standard `<OverlayName>Screenshot.png` name
enters the normal pixel pipeline and the overlay is compared exactly like any screen. That
stopgap keeps an overlay from shipping uncompared; this section is HOW the
builder captures one so it CAN ship. Two valid shapes, chosen by what the oracle actually shows.

**Decision rule.** Does the oracle include the dimmed host (scrim + host content behind the
popup)? → **shape A**. Is it an isolated popup frame with its own tight bounds (no host)? →
**shape B**. A `[dialog]` bullet behaves like `[overlay]` when drawn over a host (→ A) and like
`[component]` (§2 "Container + geometry by kind") when the oracle is an isolated frame (→ B).
Oracle-side requirement: an `[overlay]` bullet's node MUST be the full-frame overlay state
(host visible beneath the scrim), not the bare sheet component — when the designer only has a
bare-sheet node, re-declare that bullet `[component]` and capture it with the shape-B
wrap-content harness. Picking the WRONG shape — a bare popup against a host-inclusive oracle, or a host-composed
capture against a tight popup frame — IS the `UNREPRESENTABLE_OVERLAY` / low-SSIM failure; the
recipe is how you avoid it, never a way to skip the gate.

**(A) Composed-over-host** (preferred when the oracle shows the host). Capture the popup
composed over its host so the scrim + host context match the oracle. First choice: render the
HOST screen's primary stub with the overlay OPEN — the overlay-triggering state field set in
the stub — inside the same full-bleed `PreviewContainerScreenshot`, so the real component
draws its own scrim + sheet. When no state field can open the overlay deterministically,
stack host + overlay composables in a `Box` (host first, overlay on top) — so the "actual"
carries the same dimmed background the oracle drew. In BOTH variants the scrim/dim MUST come
from the real overlay component's own scrim layer, never a hand-painted translucent `Box`
impersonating it. The `@Config`
qualifier derives from the **FULL-FRAME oracle's** `frameSizeDp` (the host frame — NOT the
popup's content size), exactly as §3.

```kotlin
@Test
@Config(qualifiers = "w390dp-h844dp")            // ← the FULL-FRAME oracle's frameSizeDp (host), not the popup's
fun exampleMenuOverlay() {
    captureRoboImage("build/outputs/roborazzi/ExampleMenuScreenshot.png") {
        PreviewContainerScreenshot {             // full-bleed at the host frame
            Box(Modifier.fillMaxSize()) {    // Box-stack fallback: no state field opens the sheet deterministically
                ExampleScreen(/* settled host stub — same loaded state as its own oracle */)
                ExampleMenuSheet(/* settled popup stub */)   // the REAL overlay component — draws its OWN scrim + sheet
            }
        }
    }
}
```

**(B) Popup-only oracle** (the design provides an isolated popup node with its own frame). A
wrap-content capture of just the `Dialog`/`Sheet` at the **popup node's** `frameSizeDp` — the
[component] recipe (§2 "Container + geometry by kind"): a `PreviewContainerComponent`-style
sibling (`Modifier.size(<W>.dp, <H>.dp)` / wrap-content, no full-bleed fill) under a
method-level `@Config(qualifiers)` from the popup frame's own `frameSizeDp`. No host re-render.

```kotlin
@Test
@Config(qualifiers = "w340dp-h250dp")            // ← the popup node's own frameSizeDp
fun confirmDialog() {
    captureRoboImage("build/outputs/roborazzi/ConfirmDialogScreenshot.png") {
        PreviewContainerComponent(Modifier.size(340.dp, 250.dp)) {   // wrap-content, not screen-sized
            ConfirmDialog(/* settled popup stub */)
        }
    }
}
```

**Determinism (both shapes).** Capture the **settled state**, not a transition: no
enter/exit/slide animation, no `LaunchedEffect`-driven reveal (§2 "No animations"). The real
overlay component's scrim must render at its settled **fixed alpha** (a token, e.g.
`AppTokens.colors.scrim` — never a time- or gesture-driven value, and never a hand-painted
stand-in layer) with a **fixed elevation/shadow** so the sheet's shadow is byte-identical
run-to-run. In shape A the determinism rules apply to BOTH layers — host content parity AND
overlay content parity (§2's content-parity rule for each): the host stub is a settled loaded
state — a static stub, NEVER a live host re-render driven by effects. Everything else (locale/qualifier per §3,
`nodeId`-tagged manifest per §4, `primaryState`) is unchanged: shape A binds to the full-frame
node, shape B to the popup node.

---

## 3.6 Auto-normalization — device-chrome bands (comparator side)

The #1 instability source is an oracle and a capture covering **different regions**: the
designer may or may not draw the device status-bar / nav-bar frame, while the capture
(a bare composable, §2) never has real system UI. `compare-screenshots.mjs` normally
neutralizes this with bounded deterministic zones. The detector has one strict role:
an unresolved geometry-only top strip disables the top mask; it never enlarges a band:

- **`[screen]` only — device-chrome bands.** A fixed top **status-bar** band
  (`SCREENSHOT_STATUS_BAR_DP`, committed as `statusBarDp` — **24dp** today) and bottom **nav-bar** band
  (`SCREENSHOT_NAV_BAR_DP`, committed as `navBarDp` — **48dp** today) are excluded from the comparison on BOTH
  images. Heights map to px via the oracle's `frameSizeDp` — but **only when it is
  consistent with the oracle's real pixel aspect** (a corrupt/units-wrong `frameSizeDp.h`
  falls back to a fixed fraction), and the band is **hard-capped** to a small constant
  fraction of the frame (status ≤6%, nav ≤9%). Those caps are constants (not env, not
  spec), so a hostile or wrong `frameSizeDp` can **never** blind more than ~15% of the
  frame — closing a proven laundering exploit where a tiny `frameSizeDp.h` masked 30% of a
  real screen. An excluded band pixel is invisible to **every** arm (SSIM, per-zone floor,
  ΔE, and the `RENDER_EXTRA_CONTENT` probe) — correct for the band ONLY, because a
  status/nav bar legitimately differs between oracle and capture.
- **`[dialog]` / `[component]` / `[overlay]` — no band mask.** Non-screen kinds have no
  system chrome, so no band is applied; a dialog's OWN content (including any top row a
  screen's status band would blind) is fully compared. A dialog already compares
  "content-only" without a crop: the existing **oracle-side union content mask** excludes
  a uniform scrim (low variance → background) automatically, and a composed-over-host
  (shape-A) dialog's host content SHOULD match by the content-parity rule, so it is
  compared in full (the stricter, correct direction). A spec-driven content crop was
  deliberately NOT shipped — an adversarial review proved it both a laundering surface
  (a single small element bbox could blind most of the frame) and a net weakening of the
  shape-A case.
- **A `chromeCrop` stamp disables both bands.** `normalize-oracle` has already removed the
  device chrome and shifted the remaining PNG/spec to app-content coordinates, so masking
  again would hide the app's own first/last rows. Unstamped screen oracles retain the
  symmetric fixed-band behavior above.
- **An ambiguous top strip disables the top band.** When the detector sees status-bar-like
  geometry without the required name/`9:41` signal, it deliberately keeps the pixels and
  reports `IOS_CHROME_SUSPECTED`. The comparator also keeps that top strip comparable instead
  of hiding the unresolved evidence behind the generic status-bar mask; the bottom band stays
  independent.

The band dp knobs are **canon-pinned** in `evidence-bundle.mjs`
(`CANONICAL_SCREENSHOT_THRESHOLDS`, `statusBarDp`/`navBarDp` both `lte` their defaults): a
RAISED band over-masks (weaker) and fails the final gate as `THRESHOLDS_WEAKENED`; `0`
disables a band (compares MORE = stricter, always allowed). The bounded blind spot (real
content in the very top/bottom system-bar zone of an edge-to-edge screen) is accepted by
design — far better than a whole-image garbage low-similarity blocker — and element
**structure** in the band is still covered by the independent spec-compare gate.

---

## 3.7 Device-chrome normalization at the pull boundary

The §3.6 bands mask BOTH images at the same offsets — they cannot fix a **one-sided**
offset. A Figma oracle exported as an iOS frame WITH device chrome (the "9:41" status bar,
~44–47dp, and the home indicator, ~34dp) sits ~47px below its chrome-free Roborazzi capture:
every per-node zone compares against a shifted region (element SSIM ≈ 0) and the ±2px shift
search cannot absorb it. **The comparison contract is app CONTENT vs design CONTENT — device
chrome is not content.** So the oracle is normalized ONCE, where it enters the system:

- **`normalize-oracle.mjs <stem>`** runs as a MANDATORY step of the `figma:screens` pull
  session (after the cache write, before the `check-screen-cache` gate). Detection is a
  screen-only transform: `[dialog]` / `[component]` / `[overlay]` nodes are never cropped.
  For a `[screen]`, detection is a
  STRICT deterministic predicate (`lib/oracle-chrome.mjs`): a TOP strip is chrome iff it is
  top-anchored (y ≤ 2dp), full-width (≥ 95%), ≤ 50dp tall, AND carries a status-bar layer
  name OR the `"9:41"` marketing time (itself or a contained child — that time never appears
  as real app copy); a BOTTOM strip iff bottom-anchored, full-width, ≤ 40dp, named like a
  home indicator. The PNG is cropped by the band px (dp→px via the export scale), chrome
  elements are dropped from the spec, every remaining `bboxDp.y` shifts by −topDp (straddlers
  like an edge-to-edge background are CLAMPED, never dropped or made negative),
  `frameSizeDp.h` shrinks, and the spec is stamped with the auditable
  `chromeCrop: { topDp, bottomDp, matched, at }`. Idempotent — a second run is a byte no-op.
- **Fail-closed:** a strip matching the GEOMETRY but carrying no name/text signal is NOT
  cropped — the pull report warns `IOS_CHROME_SUSPECTED` naming the element, and the owner
  decides (rename the Figma layer so the predicate matches, then re-pull — or accept the
  pixels as content). Until resolved, the comparator does not apply its generic top band to
  that strip, so ambiguity cannot become an invisible PASS. An INCONSISTENT pair is refused,
  never guessed at: before cropping, the
  PNG aspect must match the PRE-crop spec aspect within a tight 2% rounding bound
  (`CHROME_PAIR_INCONSISTENT` WARN + re-pull remedy otherwise) — the torn-state net that
  keeps a crashed half-write from ever being double-cropped on the next run. Never fabricate
  chrome onto the capture (forgery-shaped), never absorb
  the offset into thresholds, never mask judge-side only (the spec gate's bboxDp would stay
  47px wrong).
- **The crop is verifiable, not trusted:** `check-screen-cache --gate` re-checks every
  stamped spec — surviving chrome-in-a-chrome-band-position (`CHROME_CROP_RESIDUE`; a
  mid-frame "9:41" or chrome-named layer is CONTENT and exempt), an out-of-frame bbox after a
  bad shift (`CHROME_CROP_BAD_SHIFT`), and a PNG whose aspect no longer matches the post-crop
  `frameSizeDp` within a TIGHT 2% bound (`CHROME_CROP_ASPECT` — the crop is dp-exact, so the
  comparator's loose 0.15 tolerance would be blind to a missed crop) all BLOCK. A crop stamp
  on a non-screen node (`CHROME_CROP_KIND_MISMATCH`) and a PNG without a readable PNG/IHDR
  header (`PNG_UNREADABLE`) also BLOCK rather than skipping verification; the remedy is
  a re-pull (normalize-oracle re-runs on the fresh pair — a stamped spec itself is skipped by
  design), never a builder fix.
- **Unnormalized caches diagnose themselves:** on an ASPECT_MISMATCH bail where the unstamped
  oracle is 40–90dp taller than the capture, the comparator appends the diagnostic
  `IOS_CHROME_SUSPECTED` hint (re-pull remedy) to the bail message — the row still bails
  ASPECT_MISMATCH (completeness-class, never routable). Drift flows need no re-pull
  choreography: `check-screen-drift`/`sweep-done-drift` normalize a RAW shadow re-pull IN
  MEMORY (same predicate) whenever the baseline is stamped, so a normalized baseline never
  phantom-drifts — and an unstamped raw receipt keeps its raw-vs-raw diff.

---

## 4. Capture ↔ comparator contract (do not drift — verified against the code)

`compare-screenshots.mjs` finds and validates captures by these rules
(`variantsFor`/`manifestEntriesByNodeId`/`staleByStartedAt`):

| Concern | Contract |
|---|---|
| **Oracle filename** | exactly `index.nodes.<Screen>.variants[].imageFile`; the pull normally writes `<Screen>.png`, `<Screen>.dark.png`, or `<Screen>.<variant>.png` |
| **Capture filename** | the recorder normally derives `<Screen>Screenshot[.<variant>].png`, but basename is never identity and may be renamed when the manifest records the exact path |
| **Capture identity (required)** | `manifest.nodeId === variant.nodeId`; if two variants share a node, `manifest.variantId === variant.id` is also mandatory. Missing identity → `MISSING_CAPTURE`; duplicate identity → `DUPLICATE_CAPTURE`. There is no basename recovery path. |
| **Capture location** | every root is explicitly listed in `ROBORAZZI_OUTPUT_DIR` / `ROBORAZZI_OUTPUT_DIRS`; absent/nonexistent roots are configuration errors. Every manifest path must remain inside one of them. |
| **Variant selection** | every entry in the current non-empty `variants[]` array is compared exactly once; the final bundle re-derives that same set and blocks partial reports |
| **Node kind** | a `## Design` bullet may tag the node kind as a trailing `[screen\|dialog\|component\|overlay]` (default `screen`). The kind MUST be persisted in `index.json` (`node.kind`) for any non-screen node — `check-screen-cache.mjs` BLOCKS (`KIND_MISSING_IN_INDEX` / `KIND_MISMATCH`) otherwise, so a non-screen node can no longer be silently dropped. An `[overlay]` (a popup/sheet over a dimmed host) has no representable *bare-isolation* capture: a missing capture for it → `UNREPRESENTABLE_OVERLAY` (BLOCKER, fail-closed) instead of a generic `MISSING_CAPTURE`. It never ships uncompared — the builder produces the "actual" via §3.5 (shape A composed-over-host, or shape B for a popup-only oracle). |
| **Freshness** | a capture older than `SCREENSHOT_CAPTURE_STARTED_AT` (epoch-ms/ISO, set to the Roborazzi run start) → `STALE_CAPTURE` (BLOCKER). The lower bound itself must be within 24h of the compare/report and no more than 5m in the future; an ancient positive value such as `1` is invalid evidence, not a way to bless old outputs. This mtime lower bound is a stale-output guard, not recorder provenance: final evidence requires exact `captureMode=recorded`; the canonical driver marks `--skip-record` manifests as `recording.mode=preexisting`, which final rejects with `SCREENSHOT_SKIP_RECORD_NONCERTIFYING`, while missing/unknown provenance is rejected with `SCREENSHOT_RECORD_PROVENANCE_MISSING`. Use skip only for diagnostics; certifying evidence must run the driver-owned record step. |
| **Reference artifacts on a bail (diagnostic)** | a row that can't produce a per-pixel diff still emits REFERENCE artifacts via `writeReferenceArtifacts` (separate from the happy-path `writeCompareArtifacts`, which stays byte-identical): `ASPECT_MISMATCH` → `figma` + `actual` (each at native size, NO diff/overlay); plain `MISSING_CAPTURE` → `figma` only (`UNREPRESENTABLE_OVERLAY` stays fail-closed, no reference). This is display-only — the **status/verdict is unchanged** (still a BLOCKER in gate mode). The refs are the same cache-relative, hash-bound, `ARTIFACT_FILE_BY_KIND`-named shape, so the site's vetted `compareArtifactFile` serves them identically; the Done "Figma" view shows them so a failed comparison is legible instead of blank. |
| **Manifest (REQUIRED in gate mode — carries identity)** | exact JSON `{ recording?: { ... }, captures: [{ captureName, path, nodeId, variantId?, primaryState: true|false, localeTag? }] }`. Unknown aliases/keys are rejected as `MANIFEST_INVALID`. A `--gate` run without the manifest blocks as `MANIFEST_ABSENT`; the canonical driver emits it. An absent node/variant entry → `MISSING_CAPTURE`; `primaryState:false` → `REVIEW_REQUIRED`/`PRIMARY_STATE_UNCONFIRMED`. `recording.mode=preexisting` is deliberately non-certifying. |
| **Capture locale** | an entry SHOULD carry `localeTag` — the locale the harness ACTUALLY rendered with. When present AND the design language is derivable (`designLocale` config / `lib/design-locale.mjs` detection over the stem's specs), a language mismatch bails the row as **`CAPTURE_LOCALE_MISMATCH`** BEFORE the meaningless low-SSIM comparison — completeness-class: always a BLOCKER, in advisory mode too, never routed by `screenshotPixelGate` (§7.3). Remedy: fix the `@Config` locale segment (`check-capture-config --fix` is locale-aware) and re-record — never re-pull a "convenient" oracle. Without `localeTag`, or with an underivable design language, this comparator witness has no locale comparison; the mandatory final `capture-config` report remains authoritative and hash-binds the checked Kotlin sources, specs, consulted `strings.xml`, bindings, index, and project config. Its virtual discovery digest is re-derived at final, so adding a capture-bearing test, locale resource, or binding after the scan invalidates the report. A narrowed `FIGMA_CENSUS_CODE_ROOTS`/`--code-root` witness is also compared with a canonical `PROJECT_ROOT` discovery; omitted capture files/resource roots or unreadable roots block as `CAPTURE_DISCOVERY_SCOPE_INCOMPLETE`/`CAPTURE_DISCOVERY_*_UNREADABLE`. The comparator also hashes project config for both declared and detected locale paths. Fixture-only `FIGMA_DESIGN_LOCALE`/`FIGMA_SUPPORTED_LOCALES`/`FIGMA_STRING_RESOURCE_ROOTS` env overrides are recorded by both reports and hard-block the final bundle (`LOCALE_ENV_OVERRIDE` — the THRESHOLDS_WEAKENED pattern). |

**Validator wiring (the `figma-screenshot-validator` step):**

Run id: reuse the orchestrator's `FIGMA_PIPELINE_RUN_ID` (passed in the prompt) for
`compare-screenshots` and the final bundle — never mint a new one inside this
validator, or the final `--fresh` bundle rejects the whole set as a mixed/stale
run. Safety net: the id is file-pinned at
`orchestrator/.cache/figma/reports/.run-id-<stem>`, so an env-less invocation
reuses the pinned id — but pass the explicit id anyway so a stale pin is
corrected, not inherited.

0. **Toolchain preflight (first time the gate runs for a module).** Run
   `./gradlew :<module>:verifyScreenshotToolchain` (registered by `screenshot.test.convention`).
   It eagerly resolves the JDK 21 launcher the host-test pins — proving foojay can provision
   it (download path included), NOT a `gradle.properties`/JAVA_HOME grep. A failure here is
   the loud signal that the capture would otherwise silently produce nothing. Never commit
   `org.gradle.java.installations.paths` — it pins the build to one machine and defeats foojay.
1. Run Roborazzi via Gradle for the affected `figmaEnabled` modules (record task
   start → export it as `SCREENSHOT_CAPTURE_STARTED_AT`; emit the capture manifest with
   `recording.mode=recorded` plus one entry per capture carrying
   `{ path, nodeId, primaryState: true }` — look the
   `nodeId` up from `index.json` so oracle↔capture binds by identity, not by the
   LLM-picked basename). Preferred: use `run-figma-gates.mjs --stage screenshot
   --modules ...`; `--skip-record` remains a diagnostic comparison path and cannot
   satisfy final evidence.
2. `node orchestrator/figma/scripts/compare-screenshots.mjs <stem> --gate`
   (`--gate`: missing/incomplete/blocking visual evidence ⇒ exit 2, routed to the fix
   path) → writes `screenshot-<stem>.json` + `figma.png/actual.png/diff.png/overlay.png`
   per screen×theme. The comparator reads the project's `screenshotPixelGate` config value
   (default `strict`) from `orchestrator/project-config.md` ITSELF — do NOT export
   `SCREENSHOT_PIXEL_GATE` (a per-run diagnostic override only; an exported stale value
   can silently degrade a project's `strict` to `advisory`); the pixel-similarity verdict
   routes per §7.3, and the completeness/anti-forgery blockers fire regardless.
3. `node orchestrator/figma/scripts/evidence-bundle.mjs <stem> --stage final --fresh`
   → re-hashes inputs+artifacts, schema-gates, fails closed. This is what the site's
   trust gate (`trustedFinalVisualEvidence`) consumes; do not bypass it.

---

## 5. `figma-screenshot-validator` — frozen surface

Pinned by `orchestrator/contracts/agents/figma-screenshot-validator.md`
(tools: `Read, Bash, Grep, Glob`; model `sonnet`). This skill is the contract's
source of truth; the surface below is frozen and must be preserved verbatim across
migrations.

- **Role / when it runs:** post-assemble visual-fidelity gate, **mandatory** for every
  `figmaEnabled` task with a non-`none` `## Design` node (`screen|dialog|component|overlay`);
  self-skips ONLY for a non-UI
  task (and a `figmaNodeId:`/screen-or-dialog-file task that omits `## Design` is caught by
  the `ship-done`/`verify-done` backstop, not this validator). A declared design node with a
  missing oracle/capture is a **BLOCKER** (`status: fail`),
  not a skip — the comparison must run before the task can reach `done/`.
- **Required inputs:** the pulled oracle(s) for the task's `## Design` nodes; a
  Roborazzi capture per pulled theme; a green Step-4.5 assemble.
  **BLOCKED message** when an input is missing: emit a `validator-finding` with
  `status: fail`, a precise `rule_id`/`issueKind` (`MISSING_ORACLE`,
  `MISSING_CAPTURE`, `STALE_CAPTURE`, `PRIMARY_STATE_UNCONFIRMED`), and the
  screen/theme — never a silent pass.
- **Required outputs / contract:** `screenshot-<stem>.json` (shape:
  `screenshot-compare-report.schema.json`) + the four compare artifacts per
  screen×theme, folded into the final evidence bundle. Findings normalize to
  `orchestrator/contracts/validator-finding.md`.
- **Stop-and-ask:** any build-config / threshold-promotion / dependency change
  (e.g. adding `pixelmatch`) — escalate, do not self-approve (see
  `when-to-stop-and-ask.md`).
- **Allowed writes:** the report + artifacts under `.cache/figma/`.
  **Forbidden writes:** product source, `ScreenshotTest.kt`, the Figma oracle,
  threshold defaults, or the template.
- **Required reads:** this file, `compare-screenshots.mjs`, the task `## Design`
  section, the resolved screen spec (`frameSizeDp`/theme).
- **Task footprint:** scoped to the current task's screens; no cross-task edits.
- **Spawn:** may **not** spawn other agents (the tool budget is the hard boundary).

---

## 6. JDK 21

The host-test task is pinned to a JDK-21 launcher by the convention plugin
(Roborazzi + Robolectric 4.16 runtime classes are Java-21 bytecode; the module
itself still compiles on 19). The toolchain is auto-provisioned by the
`foojay-resolver-convention` plugin — **no machine-specific JDK path** (never commit
`org.gradle.java.installations.paths`; it pins the build to one box and defeats foojay,
after which the gate silently fails to run elsewhere) (`convention-plugins.md` § JDK 21).
The convention plugin registers `verifyScreenshotToolchain` — the **authoritative** loud
preflight that resolves the real launcher (not a grep proxy); run it once when the gate is
first enabled for a module (validator wiring step 0).

---

## 6.5 Threshold calibration — dark regime

Calibrated by characterizing the metric's response to controlled perturbations of a
dark-screen corpus plus a low-content boundary probe:

| perturbation | SSIM | ΔE | band |
|---|---|---|---|
| identity | 1.000 | 0.0 | PASS |
| ±1–2 px shift | 0.976 | 3.3 | PASS (absorbed by the ±2 px search) |
| 3 px shift | 0.798 | 13.7 | MAJOR |
| box-blur r1 / r2 | 0.854 / 0.748 | 9.3 / 14.4 | MINOR / MAJOR |
| brightness ±10 % | 0.99 | ~4 | PASS (luma-normalized) |
| colour push (ΔE≈8) | 0.980 | 7.99 | PASS by SSIM; the ΔE axis flags it |
| low content (1–3 % coverage) | 1.000 | 0.0 | PASS (stable; bails <0.5 %) |

Conclusions: (1) the feared dark-on-dark **saturation does NOT manifest** — a perfect
match scores exactly 1.000 from 1 % to 37 % content (the colour∨variance union mask
carries it); (2) the **default thresholds (PASS≥0.90 / MINOR≥0.80 / MAJOR≥0.65) hold**
for the dark regime — no change; (3) global brightness/exposure shifts stay PASS while
the **ΔE axis catches colour drift SSIM misses** (dual-axis validated); (4) the ±2 px
shift tolerance is clean (≤2 px PASS, 3 px+ MAJOR). The MAJOR band is therefore
**trustworthy for dark content** and blocks in gate mode (`SSIM_MAJOR` BLOCKER;
`SCREENSHOT_MAJOR_BAND=advisory` is the rollback knob).

**Text-aware colour and mask-edge handling.** Both are pinned by fixtures: the
colour axis is **text-aware** —
`worstRegionDeltaE` (and the `COLOR_DRIFT_REVIEW` WARN it decides) samples NON-TEXT
content only, text ΔE is reported separately as `textDeltaE` (substituted-font glyphs
inflated ΔE 29–51 on token-correct screens), and the WARN fires only on a
structurally-PASS/MINOR row (the stated design intent — the colour axis is the lone
witness exactly when structure matches). The extra-content probe gained a
**mask-dilation ring** (`extraContentRingPx`, committed 3): divergent unmasked pixels
within the ring of the content-mask edge are blur-halo/gradient spill, not render-added
content; the probe also reports a per-8×4-region breakdown (`extraContentRegions`).
The thresholds `version` is 3; the ring
is canon-pinned `lte` (raising it is `THRESHOLDS_WEAKENED`, 0 is stricter and allowed).

**Capture-evidence hardening.** Negative/transient capture
state now wins over contradictory positive aliases; ambiguous status-bar-like top content is
compared instead of masked; unreadable oracle PNGs and screen-only crop stamps on non-screen
nodes fail closed; capture/locale discovery additions invalidate stale reports; and final
evidence/UI require the same canonical seven-report set. These invariants define
`gatePolicyVersion` 4.

---

## 7. What this does NOT cover

- Threshold calibration on a **broader multi-screen REAL corpus** — the dark-regime
  curve is recorded in §6.5, and the template now carries a SYNTHETIC ground-truth
  corpus (`tests/calibration/recipes.json` → `generate-calibration-mutations.mjs`)
  whose comparator verdicts are PINNED in `figma:verify` (`figma:test:calibration`) —
  a regression pin on the template's own committed numbers, including two documented
  metric limits (local uniform dim, small-solid-node displacement — both spec-compare /
  ΔE territory). What remains open is labeling REAL product pairs
  (`calibrate-thresholds.mjs --labels <file> --reports <dir>`, or `--corpus <dir>` for
  a curated corpus dir) to refine the MINOR/MAJOR midpoints and the zone floors.
- The `masked-ssim-luma-v2` metric internals — owned by `compare-screenshots.mjs`.
  That comparator also emits a CIEDE2000 ΔE colour axis (`colorStatus`,
  `worstRegionDeltaE`, per-region/zone `deltaE`) — advisory-only — and per-Figma-node
  `zones[]` scored from the resolved `<screen>.spec.json` element bboxes; the zone ΔE
  fields are advisory, but the zone SSIM feeds the GATING worst-zone floor (§7.1,
  default ON). Tuning their thresholds on a real project is the open calibration work.

### 7.1 Worst-zone floor (default ON, per-kind)

The gate verdict is otherwise the **content-weighted mean** SSIM, so a localized-but-critical
diff (one destroyed Figma node) can be averaged away into a PASS — a false-negative. The
worst-zone floor is **default ON** (`SCREENSHOT_ZONE_GATE=0` to roll back): any single zone (or
grid cell) with enough content (`contentPx >= SCREENSHOT_MIN_REGION_PX`, committed as `minRegionPx` — 400 today) and
`ssim` below its **per-kind** floor forces the screen to **BLOCKER** (`ZONE_SSIM_BLOCKER`; the
offending zone is recorded in `result.zoneFloorHit` and the report's
`thresholds.zoneGate/zoneBlocker/zoneTextBlocker/minRegionPx`). The floors are **per node kind**
("структуру строже, текст мягче"): a NON-TEXT zone uses `SCREENSHOT_ZONE_BLOCKER_THRESHOLD`
(committed as `zoneBlocker` — **0.35** today), a TEXT zone the lenient
`SCREENSHOT_ZONE_TEXT_BLOCKER_THRESHOLD` (committed as `zoneTextBlocker` — 0.25 today) —
Robolectric font-AA ≠ Figma, and text fidelity is covered independently by spec-compare
+ the ΔE colour axis. These calibrated coarse values live in ONE committed source —
`orchestrator/figma/screenshot-thresholds.json`: `compare-screenshots.mjs` derives its env-knob
defaults from it and `evidence-bundle`'s `CANONICAL_SCREENSHOT_THRESHOLDS` derives the
anti-forgery canon values from the SAME file (lock-step by construction; the gte/lte directions
stay code). The config's ajv view is `token-schemas/screenshot-thresholds.schema.json`,
compiled at the final gate (and preflighted by `figma:doctor`'s `GATE_SCHEMAS`) — a
schema-invalid config is a `THRESHOLDS_CONFIG_INVALID` BLOCKER. The TEMPLATE pins these numbers with its synthetic calibration corpus
(`figma:test:calibration` in `figma:verify`, see §7): a metric or config change that shifts any
pinned verdict fails the template build. That pin gates the template's own numbers, **never a
product**: products consume the committed values, there is **no per-product corpus dependency**,
and a single global threshold still does not generalize across design languages — pixel
similarity stays a coarse backstop routed per-project by `screenshotPixelGate` (§7.3).
`calibrate-thresholds.mjs` is the tuning tool for BOTH sides (`--corpus <dir>` for a curated
corpus; `--labels <file> --reports <dir>` for hand-labeled real pairs; three-class labels
pass|minor|fail, schema-gated by `calibration-labels.schema.json`). The final gate ENFORCES the
canon: it requires `zoneGate===true` and `zoneBlocker` at least the committed value, so
`SCREENSHOT_ZONE_GATE=0` at final surfaces as `THRESHOLDS_WEAKENED` (mirrors §6.5's
MAJOR-band-promotion). 0.35 (not the earlier 0.55) is the calibrated separation: a correct
fine/thin-stroke icon under normal rasterizer AA sinks to ~0.55 at σ=1.5, while genuine breaks
sit at 0.04–0.30 — a floor in (0.30, 0.39) separates them (see the `Z_BLOCKER` derivation in
compare-screenshots.mjs).

### 7.3 Pixel-verdict routing — `screenshotPixelGate`

The pixel-SIMILARITY verdict (the SSIM bands, the per-zone floor, colour-only,
render-extra-content) is a **coarse backstop, not an absolute judge** — a single global
threshold cannot generalize across a template's many design languages. So its verdict is routed
by the per-project `screenshotPixelGate` config key (`orchestrator/project-config.md`; read
DIRECTLY by the comparator — the `SCREENSHOT_PIXEL_GATE` env is only a per-run override),
**default `strict`**:

- **`strict`** (default) — the pixel similarity verdict may **BLOCK**;
  every bootstrap starts here so pixel drift fails closed from day one.
- **`advisory`** — a similarity divergence is a **WARN**, never a hard block; a
  `BLOCKER` row is routed to `MAJOR` (the raw band kept in `result.pixelStatus`) and the
  similarity issues to `WARN`, so the report is a self-consistent WARN. The per-project
  downgrade for a design language the global threshold demonstrably over-blocks.
- **`off`** — similarity findings are suppressed; the row is `PASS` (raw band in `pixelStatus`).

**What the knob does NOT touch (always ON, never routed):** the completeness/anti-forgery
blockers (`MISSING_ORACLE`/`MISSING_CAPTURE`/`UNREPRESENTABLE_OVERLAY`/`ASPECT_MISMATCH`/
`STALE_CAPTURE`/`DUPLICATE_CAPTURE`/`CAPTURE_IS_ORACLE_COPY`/`CAPTURE_PATH_UNCONTAINED`/
`CAPTURE_LOCALE_MISMATCH`/coverage),
the design-agnostic **structural** gate (`figma-spec-validator` / `compare-screen-spec`), and the
metric-strictness canon-pins — the comparison ALWAYS runs at full canonical strictness (sigma,
mask, thresholds unchanged); only the similarity **verdict** is routed. The **3-frame evidence
(Figma / overlay / app diff) is always computed and shown regardless of the mode.** The final
bundle records `thresholds.pixelGate` and pins it to the committed config: a recorded
routing WEAKER than the project's `screenshotPixelGate` (order `strict` > `advisory` > `off`)
is `THRESHOLDS_WEAKENED` — a per-run `SCREENSHOT_PIXEL_GATE` downgrade cannot certify final
evidence; recording STRICTER than committed is always fine. The committed config itself stays
a legitimate per-project choice — the unconditional guarantee lives in the structural gate +
the completeness/anti-forgery net.

**Per-class review routing.** Under `strict`, a COMPONENT row whose ACTIVE mapping in the
Component Mapping Registry carries `visualPolicy.renderClass: canvas|glass` (owner-set through
the site's Mapping Review `set-render-class` op — `canvas`/`glass`/`null` to clear, stamped
with by/at/reason)
routes its similarity verdict to `REVIEW_REQUIRED` instead of blocking: the raw band stays in
`result.pixelStatus`, the similarity issues become WARN `PIXEL_REVIEW_REQUIRED`, and the report
records `classRouting`. The state is NON-shippable (comparator gate exit, bundle final, ship-done
all rank it non-shippable) until the OWNER clicks a verdict in the site's evidence tab — the
click writes a hash-bound receipt (`orchestrator/tasks/evidence/pixel-review/<stem>.json`,
server-derived hashes over the sealed report + the reviewed figma/actual pixels) that the final
bundle re-validates: `pass` → PASS, `minor` → the reviewed-WARN caveat path, `fail` → BLOCKER
`PIXEL_REVIEW_FAILED` with the owner note; any re-render/re-run voids the receipt by hash.
Agents NEVER write these receipts. The bundle also re-pins routing against the LIVE mapping
registry (`CLASS_ROUTING_STALE` when the owner cleared a class after the compare). Completeness/
anti-forgery kinds are never routable, for any class.

**Tighten-only task override.** A task whose `## Design` section carries the exact
bullet `- gate: strict` forces strict routing for that task's comparator runs even on an
advisory-configured project (max()-only). The weakening direction has no grammar: any other
`gate:` value is a malformed design (`DESIGN_VALUE_RESIDUE`) blocked at the cache gate — a
task can never route itself to advisory/off.

**Known residual — the structural gate checks declared VALUES, not rendered LAYOUT.**
`compare-screen-spec` verifies each element's token / text / dp fidelity (presence + value), NOT
where the element ENDS UP on screen. A break that is purely positional (right tokens, wrong
geometry — an element in the wrong place, an overflow) is caught ONLY by the pixel gate. Under the
default `strict` it may block; under `advisory` it surfaces as a WARN (visible,
shippable-with-caveat); under `off` it is not flagged at all. That is the deliberate trade of
`off` (a project opting out of the pixel verdict entirely); `advisory` keeps the positional WARN
so the residual is not silent.
