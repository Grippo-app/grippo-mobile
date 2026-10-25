# Visual consistency

For visual intent and composition, apply the canonical
[brand-design skill](../.claude/skills/brand-design/SKILL.md). This document owns
concrete product role-to-token choices; component/token APIs remain in source.

Use `AppTokens` for colors, typography and dimensions. Keep a component's role
consistent across the dashboard, workout recording, history, profile and dialogs.
A compact navigation item and a screen heading do not need the same font size.

All product typography tokens use bundled Onest through `AppTypography`: Regular
(400), Medium (500), SemiBold (600) and Bold (700). The same family covers Cyrillic
and Latin text in both themes; each role keeps its existing size and line height.

| Role | Typography | Color |
| --- | --- | --- |
| Screen or detail heading | Existing toolbar / `h2`–`h3` | `text.primary` |
| Section heading | `h4` | `text.primary` |
| Regular card or exercise title | `h5` | `text.primary` |
| Compact item title | `h6` or `b14Semi` | `text.primary` |
| Supporting information | `b13Med`; `b14Med` for longer body text | `text.secondary` |
| Small labels and timestamps | `b11Med`–`b12Med` | `text.secondary` / `text.tertiary` |
| Primary metric | `h4`–`h5` | `text.primary` |
| Overview performance value (concise Home tile) | `h2` | `text.primary` |

## Color and state

- Filled segments, highlighted navigation and iteration-picker suggestion chips use the warm
  selection.accent/background pair. Choice cards and checked controls use neutral
  selection roles, keeping repeated choices quiet beside the primary action.
  Success/error/info and chart series keep their semantic colors; selection does
  not imply a completed workout.
- Severity backgrounds come from semantic.*Background in both concrete themes.
  Informational bubbles and badges keep neutral text where the status icon already
  supplies the accent.
- A value that moved below its own baseline is a decline, not a warning:
  `semantic.decline` / `semantic.declineBackground` (Light LightAccent.Indigo
  `414AAF`, Dark DarkAccent.Indigo `9FA8DA`, background through the same
  `lerp(background.card, decline, 0.10f / 0.14f)` recipe) and the
  `charts.indicator.decline` ramp (a lighter Indigo - `lerp(Indigo, background.card,
  0.25f)` in Light, `lerp(Indigo, Common.White, 0.25f)` in Dark - → Indigo on a
  0.2-alpha Indigo track) color the completed-workout Declined chip and bars and
  the current value of a drop on the streak and goal Growth cards; the goal Growth
  card's status label and bar keep their severity band. Indigo keeps a dip calm
  and distinct from orange warning (validation, overdue, restart), red error, the
  neutral grey of ordinary variation and the purple identities of
  `context.repetitions` and `profile.experience.beginner`, which a decline must
  never be mistaken for. It stays apart from blue `semantic.info` and the
  `charts.indicator.info` ramp in both themes: in Light a violet-leaning `414AAF`
  against the azure `1F5CC1`; in Dark a muted lavender-grey `9FA8DA` against the
  vivid `7090FF`, so the streak's OnTrack `info` reading keeps its own hue.
  The same Indigo also backs `brand.color6` (monthly trainings bars) and
  `example.forceType.hinge` (exercise force-type chip); neither shares a surface
  with a decline. `semantic.warning` keeps its other consumers. Custom framed content supplies its
  tint and background explicitly, rather than deriving opacity inside the component.
- Ordinary weights, counts, dates and instructions use neutral text colors.
- Neutral boundaries use `border.default`: Light NeutralLight.N250 and Dark
  NeutralDark.N300. `divider.subtle` and `charts.bar.baseline` keep their existing
  0.4 and 0.45 alpha derivations; focus, selection and severity retain their own roles.
- Destructive account outlines use `semantic.errorBorder`; quiet summary dividers
  use the shared `ContentDivider`. Disabled switches use their own
  `toggle.disabledTrack` and `toggle.disabledThumb` roles.
- Chart grid/axis/baseline colors and area/sparkline fill endpoints belong to
  their chart color groups. Each series retains its supplied semantic hue;
  history fills use their chart group's fade endpoints. Avatar fills/rings
  retain experience hues through the profile coefficients; `OverlayContainer`
  edges keep their host surface opaque behind a pinned slot and fade only its
  alpha toward the content. Shadow spot strength uses
  `overlay.shadowSpotAlphaMultiplier` without changing its hue.
- Volume bar charts use flat series colors and substantial columns with a small
  4 dp corner radius, distributed in even slots across the plot. Each column uses
  60% of its slot, capped at 40 dp by `metrics.charts.bar.maxWidth`; the gap keeps
  dense series distinct without making ordinary histories look sparse. Radius,
  width and density share the `metrics.charts.bar` owner. The shared
  product style omits the Y axis, grid and repeated above-bar values; a subtle
  `charts.bar.baseline` preserves the zero reference. Adaptive `b10Med` X labels
  retain `bar.axisLabelSpacing` below the plot. Exact values with units appear in
  the shared tooltip using `b12Semi` values, `b10Med` labels and Peek spacing/border
  tokens. Selection uses a modest guide and rounded outline, without a halo.
  General volume and selected-exercise history retain their supplied blue series
  meanings. All entries also have one complete formatted accessibility description.
- Time histories use a single blue contour (`charts.sparkline.lineA`) with a
  restrained same-hue fade (`fillStart/fillEnd`), a 2.5 dp line and 8 dp vertical
  breathing room. The default omits arbitrary midlines, glow, repeated dots and
  colored extrema: a minimum/maximum alone implies neither warning nor success.
  One observation is a single marker, with no invented duration or trend.
  Distinct dated observations show complete first/last dates in `b10Med`
  secondary text with `metrics.charts.sparkline.axisLabelSpacing`; values retain
  their metric's units and precision in a compact Onest tooltip. Tap and horizontal
  scrub use the same quiet guide/marker, without a halo; vertical scrolling stays
  with the host. Bodyweight and performance histories use elapsed dates on X,
  matching estimated 1RM, rather than compressing training gaps. Their complete
  descriptions provide dates, values and units to accessibility services.
- General tonnage columns use one `palette7BlueGrowth[2]` series hue; magnitude is
  conveyed by length from zero. Exercise history preserves its distinct current
  entry against quiet historical blue columns. Calendar bucket labels show actual
  week date ranges and include the year for months; bucket aggregation is unchanged.
- Area plots share the calm blue contour/fade and neutral axes, with no glow or
  competing extrema labels by default. Radar plots retain each supplied series
  hue with a restrained `charts.radar.fillAlpha`; hue does not encode severity.
  Percentage rows use flat supplied colors, visible outside values including zero,
  and exact linear bar lengths. A non-zero fill narrower than its bar's height
  draws a dot of that height (`LineIndicator` and the growth card's bars), never a
  hairline; zero stays empty. Pie slices preserve true shares including small
  values, and labels reserve measured space. Share rings keep `charts.ring.info`
  for descriptive distribution, centered within their bounds; stroke caps do not
  inflate small shares. These shapes do not imply goal completion or recovery.
- Muscles share the “Body Map” motif across exercise-library filters and rows,
  exercise anatomy, onboarding/profile muscle selection, training summaries,
  Home and statistics/details. The anatomical zone is the primary recognition
  cue, paired with complete localized muscle/group labels. Individual muscles
  highlight their exact zone through the existing front/back/split projections.
- Identity contexts (catalogue, library filters and muscle choice) share one
  neutral `muscle.highlight`: Light NeutralLight.N550; Dark NeutralDark.N700.
  Muscle groups are distinguished by their anatomical zones and localized labels,
  without categorical colors. Model `group()/color()/icon()/image(preset)`
  methods own anatomical identity; group and muscle `icon(color)` accept the
  tint supplied by a measured context without changing their anatomical zone.
- Measured contexts (Home, exercise involvement, recorded-set statistics and
  muscle comparisons) use the existing relative green
  `muscle.palette6MuscleCalm` scale. `MuscleLoadBreakdownState.color(entry)`
  maps the complete current breakdown's min/max shares to its six bins before
  any sorting or top-three truncation. A single entry uses the strongest tint;
  equal multi-entry values share the first tint, preserving the existing scale.
  Map regions, anatomical row marks and bars use the same load tint. This tint
  communicates relative load, not a group's categorical identity, selection,
  fatigue or recovery. Missing anatomy stays neutral through
  `muscle.inactive/background/outline`, without inventing zeros.
- Bar length remains the linear 0–100% share with the existing formatted value
  outside. Recorded-set distribution remains weighted by exercise involvement;
  exercise anatomy shows the same bars without a participation caption. Exclusions and
  missing-data notices remain visible, and tied leaders receive equal emphasis.
  Muscle-load details request the full available sheet height from their local
  scrolling content, including loading and empty states; switching group/muscle
  lists does not choose the sheet height. The shared dialog host keeps its policy.
  Centered `b14Med/text.secondary` date context precedes a cohesive evidence card:
  `MuscleLoadingCardStyle.Summary` uses `MetricSectionPanel.Large`, two neutral
  `h3/text.primary` counts with `b12Med/text.secondary` labels for available sets
  and workouts and a visible excluded/unknown-set
  count. The Summary is read-only, without a chevron, anatomy or a leading-share
  ring. `Overview` and `Detailed` remain variants of the same card family.
  An Outline segment selects groups versus muscles. The body map and exact share
  rows remain open on `background.dialog`; `MuscleLoadingStyle.Breakdown` avoids
  repeating the card's explanation. A Tertiary disclosure owns method detail.
  `Annotated` shows only its conditional data notices in other contexts; no share
  caption repeats the method.
  The disclosure uses `b13Med/text.secondary` methodology and data-quality detail;
  volume-only caveats are absent from this set-share surface. Loading, no workouts,
  unavailable sets and unknown anatomy have distinct content without fabricated
  zero shares or quality/recovery judgments.
- `MuscleMark` Standard uses `muscle.markSize/markPadding/markRadius`
  (32/2/8 dp). Inline uses `muscle.inlineMarkSize` (28 dp) without a background
  tile or padding, preserving the same 28 dp anatomical drawing. Summary uses
  `muscle.summaryMarkSize` (32 dp) without a tile or padding, matching the library
  metadata row's height. Library filters and exercise-group labels keep Inline
  to avoid repeated nested tiles; the library's Suggested filter chip pairs its
  label with `icons.Sparkle` at `checkSelectableCard.small.leadingIcon` (22 dp) tinted `icon.secondary`,
  centered in a `muscle.inlineMarkSize` slot without a tile, so every filter chip
  shares the Inline chips' height and baseline while selection stays on the
  CheckSelectableCard Small surface. The exercise list keeps the photo on the left
  and a complete title, image-only
  metadata and optional date in the right column, in that order. Equipment uses
  Compact images alongside 32 dp Summary anatomy drawings. A single upper-body
  group keeps its familiar anatomical crop; multiple upper-body groups share one
  existing front/back torso drawing with all involved zones highlighted. Legs
  use their own drawing when present, keeping the summary to at most two marks.
  Inline filters and Summary marks use `muscle.compactBackground/compactInactive`
  for their anatomical context: Light NeutralLight.N200/N300; Dark
  NeutralDark.N450/N500. Summary highlights use `muscle.summaryHighlight`
  (Light NeutralLight.N550; Dark NeutralDark.N700), matching the shared neutral
  identity highlight used by Inline and Standard marks. Measured anatomy retains
  its relative-load paints.
  The summary keeps `text` gaps. Both
  image types retain complete localized accessibility names without visible captions. This
  compact geometry is specific to the library summary; filters and choices share
  its neutral identity tone, while measured detail surfaces keep their load colors and labels.
  Equipment and anatomy keep `text` gaps within each group; `content` spacing
  separates the two clusters without a divider or container. The clusters wrap
  with `text` vertical gaps, matching the right column's vertical spacing. List
  dividers follow the content gutters.
- Compact equipment caps only near-white vector fill/stroke channels through
  `example.equipmentCompactHighlight`: Light is unchanged (`Color.White`);
  Dark uses the midpoint of NeutralDark.N700/N800. Native dark and middle shades,
  path geometry and alpha are preserved. Inline, Detail and Selection equipment
  keep their original vector colors.
- MuscleSelection keeps one large hero and existing group tabs, with no duplicate
  small group mark/title above the hero. Muscle rows keep their anatomical identity cues;
  the large image highlights chosen zones with `muscle.highlight`. Neutral choice
  borders, Segment selection and Switch state retain selection ownership.
  Suggestions remain a separate filter role. Goal diagnostic markers retain
  severity accents and neutral explanatory text.
- Goals share the “Course” motif: direction, training alignment and calendar
  time are separate facts. `goal.accent/background` supplies a restrained copper
  identity pair in both themes; it does not indicate selection or success.
  `GoalMark` uses `goal.markSize/iconSize/radius` (40/24/12 dp). Compass identifies
  the goal section and an unspecified goal; Muscle, Dumbbell, Flame, Repeat,
  Stack and Body identify muscle growth, strength, fat loss, returning,
  maintenance and general fitness respectively. Calendar identifies the secondary
  consistency goal. Primary and secondary pickers share the same icon mapping,
  complete labels/descriptions and neutral choice-card selection.
  Goal entry points in Profile and Home use Compass for an unspecified goal;
  a configured Profile goal uses its shared goal icon. Both keep the copper
  accent. Ordinary benefit and methodology text stays neutral.
- Bottom-bar tab identity: Home, Calendar, Dumbbell and UserOutline identify the
  Home, Calendar, Library (exercise catalog) and Profile tabs in the
  `bottomBar.activeContent/inactiveContent` tints; Dumbbell in a neutral/orange
  navigation slot with the "Library" label is the broad exercise cue, distinct
  from its copper `GoalMark` "strength" use. Tabs split 2 + 2 around the central
  Play action.
- Muscle distribution summaries show neutral unavailable information when data
  is absent, without an inferred score or status. Progress bars use linear
  proportions and outside values; pie slices preserve their actual shares.
- Timeline points use `timeline.marker/markerBackground` for a warm chronological
  anchor with a soft halo; connectors stay `divider.default`. The marker does not
  represent selection or workout completion. Keep content and rail geometry stable.
- Step progress uses its own `stepProgressIndicator.fillStart/fillEnd/track` colors:
  a restrained orange-to-copper fill and a readable neutral remaining track in both
  themes. Equal 6 dp capsules with 4 dp gaps keep toolbar geometry stable; progress
  is flow position, separate from selection and semantic success.
- Chips use a quiet card fill, thin neutral outline and their compact 8/12 dp
  shapes. Metric labels use secondary medium-weight text and values use primary
  semibold text. Values wrap to at most two lines; labels retain one line, with
  ellipsis after either limit. Context icons and category markers retain their
  meaning. Selected
  iteration-picker suggestion chips use selection.background and selection.accent
  for fill/outline with neutral text. Library filters and goal personalization
  use the neutral CheckSelectableCard Small treatment. Keep selection and click
  handling at the existing caller.
- Completed exercise progress uses `semantic.success`: `3/3 sets`. Pending or
  partial progress stays `text.secondary`. Do not add a redundant completion badge.
- Do not display a maximum weight until at least one set has been recorded.
- Keep state accents concentrated in the status, marker or stripe. Avoid repeating
  the accent across every text line in an insight card.
- CheckSelectableCard uses selection.selectedCardBackground and
  selection.selectedCardBorder for its neutral selected surface and outline.
  Large and Medium show a trailing circular marker in a reserved slot, filled
  with selection.checkedIndicator and a selection.onCheckedIndicator check when
  selected. Large places its description below the icon/title row to retain reading
  width. Labels stay neutral across all sizes. Selection animates colors without moving content. Logical
  selection stays with the caller, including single-choice filters and multi-choice chips.
- Check is the shared circular selection mark. Standalone checkboxes own their
  touch target and accessibility; cards use Check without a callback and retain
  their own interaction. Enabled unchecked marks use icon.secondary for a readable
  neutral outline; disabled marks use icon.disabled. Toggle remains the switch control.
- ToggleSelectableCard keeps background.card, border.default and neutral labels
  in both states, with the permanently reserved switch slot conveying availability.
  Checked tracks are graphite in light theme and light neutral in dark theme;
  the contrasting thumb and its position distinguish on from off.
  Concrete equipment retains its original image colors in both switch states;
  the switch alone conveys availability. Labels wrap in full; Small uses 16 dp horizontal
  and 12 dp vertical padding, while Medium retains its 64 dp image and 12 dp padding.
- Use `ButtonStyle.Primary` for starting or resuming a workout. Reserve
  `ButtonStyle.Error` for destructive actions and explicit error acknowledgement.
- Primary buttons use a restrained orange-to-copper fill from the theme's own
  accent, with light content in the light theme and graphite content in the dark
  theme. Focus, navigation and step progress share that accent, while repeated
  choice cards and checked controls use their neutral roles.
- Secondary buttons use a neutral filled surface and thin outline;
  Tertiary and Transparent retain their quieter surface and inline roles.
- Medium text buttons use the 16 dp control shape; Small text buttons use 12 dp
  corners at their 32 dp minimum height. Tertiary icon-only buttons use 8 dp
  corners; other icon button styles remain circular.
  All styles retain their existing sizes, single-line labels and loading geometry.
- Error buttons keep the semantic red fill with a dedicated button content pair:
  light content in the light theme and graphite content in the dark theme.
- Chart series, muscle maps, validation and diagnostic severity retain meaningful
  colors. Use theme-aware palettes in previews as well as production.

## Equipment recognition

- Equipment is recognized by its characteristic silhouette and complete localized
  name. `EquipmentEnumState.icon()` is the single exhaustive identity mapping for
  all equipment types, using the existing graphite equipment vectors without tint or a
  per-item color palette. Sorting, popularity, selection and exclusions do not
  change that mapping. Theme text and surfaces retain their existing neutral roles.
- Concrete apparatus, bench/rack variants and cable attachments keep their own
  images: `BARBELL` / `EZ_BAR` / `TRAP_BAR` use Barbell / EzBar / TrapBar;
  `FLAT_BENCH` / `ADJUSTABLE_BENCH` / `DECLINE_BENCH` and the three
  `*_BENCH_WITH_RACK` types use their corresponding bench vectors. `CABLE`,
  `CABLE_CROSSOVER`, `LAT_PULLDOWN` and `ROW_CABLE` use Cable, Crossover,
  LatPulldown and RowCable. Rope, StraightBar, VBar, WideGripHandle,
  CloseGripHandle and CordHandles remain six distinct attachment identities.
  `BUTTERFLY` → Butterfly keeps its handgrips; `BUTTERFLY_FOREARM` →
  ButterflyForearm has two upright forearm pads connected to the moving arms.
  These are distinct equipment identities in every card composition.
  `KETTLEBELLS` → Kettlebells uses a broad open handle and rounded cast-iron body,
  distinct from the paired DUMBBELLS → Dumbbell silhouette.
  `ASSISTED_PULL_UP_DIP` → AssistedPullUpDip keeps its counterweight stack,
  moving knee platform, upper pull-up grips and dip handles, distinct from
  PULL_UP_BAR → PullUpBar and DIP_BARS → DipBar.
  `PLATE_LOADED_ROW` → PlateLoadedRow has independently pivoting arms with
  loaded plates, handgrips, a chest pad and seat, distinct from ROW_CABLE →
  RowCable and ROW_BENCH → RowBench.
  `BELT_SQUAT` → BeltSquat has an elevated split platform, a waist belt linked
  to a pivoting loaded lever and upright handrails, distinct from SQUAT_RACK →
  SquatRack, SMITH_MACHINES → SmithMachine and HACK_SQUAT_MACHINES → HackSquatMachines.
  `PLATE_LOADED_SEATED_DIP` → PlateLoadedSeatedDip uses two downward-pressing
  side handles on plate-loaded moving arms, a low frame and an inclined seated
  support, distinct from the tall stack-based TRICEPS_MACHINES → TricepsMachines,
  DIP_BARS → DipBar and ASSISTED_PULL_UP_DIP → AssistedPullUpDip.
  Existing aliases stay intentional: DUMBBELLS → Dumbbell, AB_MACHINES →
  AbMachine, BICEPS_MACHINES → BicepsMachine, SMITH_MACHINES → SmithMachine,
  LEG_EXTENSION_MACHINES → LegExtensionMachine, LEG_CURL_MACHINES → LegCurlMachine,
  DIP_BARS → DipBar and ROMAIN_CHAIR → RomanChair. Other machine/station types
  retain their corresponding named vectors. The containing server group owns
  membership; the UI does not infer a group or apparatus from weight type,
  exercise name, image or position.
- The `EquipmentCard` family owns four explicit compositions:

  | Role | Image and geometry | Label and state |
  | --- | --- | --- |
  | Compact library metadata | `equipmentCard.compactIcon` (32 dp) cell, no tile or border; dense dumbbell/kettlebell/barbell/EZ-bar/trap-bar artwork uses `compactFreeWeightPadding` (2 dp) inside the cell; other types retain the full fit | Image only, with complete localized text semantics; one entry per distinct type in source order; full silhouettes keep their aspect ratio |
  | Inline labelled metadata | `equipmentCard.inlineIcon` (32 dp), no tile or border | Full `b13Med`, `text.secondary` name beside its image |
  | Detail information gallery | `equipmentCard.icon` (64 dp), `equipmentCard.width` (144 dp), unframed; `content` gap and caller edge gutters | Full centered `b14Semi`, `text.primary`; horizontal scrolling retains every supplied item |
  | Availability/exclusion selection | Existing ToggleSelectableCard Medium geometry: 64 dp image, 12 dp padding, 16 dp neutral frame and reserved passive switch | Full `h5`, `text.primary`; existing Switch action/state at the row, image unchanged when excluded |

- Profile and onboarding share the same selection variant and bottom-overlay
  padding. Selected IDs and their inverse exclusions remain caller-owned.
  Equipment identity does not indicate selection, recommendation, severity or
  disabled state. Group tabs keep their full localized group labels and existing
  selection treatment. Category/count summaries may use the existing equipment
  context icon and `context.equipment`; they do not substitute for concrete types.
- Missing exercise equipment data produces no apparatus image or inferred
  “no equipment” claim. The existing declared weight-type fallback remains useful
  metadata under the explicit localized “Weight type” label. Exercise-image
  loading/missing-image treatment remains with ExerciseExampleImage. Muscle
  markers, measured load scales and recommendation filters keep their own meanings.

## Shape family

- Product corner radii follow the private 8 / 12 / 16 / 20 / 28 dp scale in AppDp;
  consumers choose their existing semantic component tokens, never a raw radius.
  The size of the boundary and its nesting determine the step.
- 8 dp: badges, small chips, set cells and 32 dp icon/image slots. 12 dp: medium
  chips, Small text buttons, compact filters and 48 dp thumbnails.
- 16 dp: Medium text buttons, inputs, compact metric panels, repeated framed rows,
  focus frames, wheel selections and swipe actions. 20 dp: standalone content,
  menu, profile, recap, achievement and gallery cards. Equipment detail images
  are unframed; its selection variant uses the repeated-row shape.
- 28 dp is reserved for BrandHeader, the onboarding hero, the completed-workout
  recap hero's bottom corners and all four corners of its shared card, and
  bottom-sheet top corners. Warm card fills alone do not change a
  card's radius.
- Filled segments use a 16 dp outer shape and a 12 dp selection shape with 4 dp
  inset. Neutral navigation and utility icon buttons use `ButtonStyle.Tertiary`
  with `background.card` and `button.iconRadius` (8 dp), matching menu icon tiles:
  sheet Back/Close, toolbar Back/Profile, date arrows and workout menus.
  Other icon button styles, including the primary Play action, retain circular
  forms alongside avatars, check marks, progress capsules and switch geometry.
  Chart points/arcs retain data geometry;
  chart tooltip surfaces use the compact 8 dp shape.
- Loading card, gallery, image and button silhouettes follow their populated
  owners; text-line skeletons use the shared skeleton corner, while a bone
  standing in for a control or a tile takes that owner's shape.

## Layout and accessibility

- Profile creation's four working steps use `BrandHeaderStyle.Compact`: a warm
  illustration region with the existing 28 dp radius, leading `h3` title and
  `b14Med` supporting copy, and `contentPadding.content` vertical padding. Keep
  the neutral fields and choices aligned with the header and primary action.
  The shared centered default remains the introduction for login/registration;
  profile completion uses `BrandSplash` with its existing operation loader.
- Profile Menu uses a compact warm identity region through UserProfileHeader:
  illustration.backgroundStart/backgroundEnd on the existing user-card shape,
  h3 name and neutral supporting email. Experience avatar colors retain their
  meaning; the menu's working sections stay neutral. Separate identity from menu
  content with contentPadding.block, keeping compact section gaps at content.
  Loading name/email slots are `SkeletonText` lines of h3/b13Med with the shared
  text-line corner at the `skeleton.line` short/medium widths. The avatar slot is
  a circular `SkeletonBone`.
  Only the avatar slot shimmers; supporting name/email placeholders stay static.
- UserProfileHeader uses role variants: Profile retains the warm avatar/name/email
  region; Account shows read-only signed-in identity on `background.card`, using
  the user-card shape and `menu.contentPadding` (16 dp on all sides) to align with
  adjacent menu sections, `h5` name and secondary `b14Med` email.
  Name/email wrap completely; absent identity has an explicit neutral explanation.
- When using a framed presentation under
  [shape and hierarchy](../.claude/skills/brand-design/references/design-language.md#shape-and-hierarchy),
  standalone content cards and framed menu sections use the 20 dp shape
  and a thin neutral outline. A standalone card is its own section; rows repeated
  inside a list are compact. Compact metric panels, repeated framed rows, input
  fields and focus frames use 16 dp corners, including goal diagnostic
  breakdown panels and weight history rows. Chips, set cells and thumbnails retain
  their size-specific shapes. Open sections and journal rows remain unframed.
- Training recording places duration, tonnage and repetitions directly on
  `background.screen`, aligned to `screen.horizontalPadding`. Equal-width cells
  use `h5` primary values and `b12Med` secondary labels with content-driven height;
  `contentPadding.content` supplies the summary's vertical padding and
  `contentPadding.subContent` separates its cells. Save/Update keeps the toolbar's
  primary action; Add Exercise keeps the pinned secondary action.
- Training exercise editing keeps the exercise summary open on `background.screen`.
  Its load descriptor uses `training.exerciseLoadTypeIcon` (20 dp), `icon.secondary`
  and secondary `b12Med` text; the UI model owns its icon and localized title.
  `IterationCard.Editable` renders compact open journal rows on the same opaque
  screen surface; the list owns `divider.subtle` separators, drawn below the
  swipe-reveal content inside the draggable item and inset by the screen gutters,
  so the revealed delete action matches only the row height. A passive 32 dp
  number badge reuses `iterationCard.numberSize`, `iterationCard.radius`,
  `background.card` and secondary `b12Semi` from Recorded iterations. Two equal
  metric actions use standalone secondary `b12Med` labels and neutral `h5` values,
  with `iterationCard.editor.minActionHeight` targets and no filled or outlined
  field shape. Body-load calculations sit below the values as secondary `b12Med`
  captions; the caller-owned previous-set context uses tertiary `b12Med`, both
  aligned with the metric content.
  Warnings use `semantic.warning` only for the affected value and icon;
  actionable explanatory text stays neutral without a filled warning panel.
  Save keeps the primary button; Add Set keeps the secondary button.
- Training completion is the "before → after" screen: it shows how this workout
  moved the user's standing in their Home period, with growth in `semantic.success`
  and a dip in `semantic.decline`, never Home's cards repeated. It starts with the
  warm `TrainingRecapHero` (the only illustration region on the screen), a
  full-width region just below the status-bar overlay - the screen has no
  toolbar, and its verdict title is the screen's heading: an `h2`
  verdict title leads, the `b14Med` description line joins
  the summary and the period label through `joined_pair` ("<summary> · <period>",
  with non-breaking spaces around the dot so it never ends or starts a line, here
  or in the streak line) once the stored period is resolved. An edited older
  workout is compared with the same kind of period around its own date (a rolling
  preset ends on that day, a calendar preset is the week, month or year containing
  it). The period label is the preset name ("30 days") whenever that comparison
  period is the current Home period and its dates otherwise. The workout's own
  facts step down to
  `h3` with `b11Med` labels. The description lives on the types:
  `PerformanceVerdictState.summary(metrics, baseline)` gives
  `training_recap_first_description` for a first workout ("The baseline for your
  next workouts", "База для следующих тренировок", "Основа для наступних
  тренувань", "Base para seus próximos treinos", "Base para os seus próximos
  treinos": short enough to keep two lines with the period label) and otherwise
  `PerformanceBaselineState.comparedSummary(improved, compared)`, which names the
  selected baseline: `training_recap_compared` ("N of M metrics above your
  average") under `Average`, `training_recap_compared_previous` ("… above last
  time", "… выше прошлой", "… вище минулого", "… acima do anterior") under
  `Previous`, each shorter than its locale's Average line so a baseline switch never
  adds a line to the hero and moves the segment under the finger. Both are plurals
  on M, the compared count, in every locale ("1 of 1 metric", "2 з 4 показників",
  "1 из 1 показателя").
  "N of M" counts the metrics that
  `showsComparison()` - they vote (not Duration, not empty) and `hasComparison()` -
  through `PerformanceVerdictState.improvedCount/comparedCount`: exactly the
  growth-card rows that carry a status chip. A period where no metric
  `showsComparison()` reads as `FirstWorkout`, never as a Stable "0 of 0". The hero is pure content - the caller
  captures it as the shared image, so it holds no controls; Share sits in the
  bottom actions under Done, and the screen has no Statistics entry. The hero takes a sealed `TrainingRecapHeroStyle`:
  `Resolved(verdict, baseline, description)` once the period is known (the hero
  words the title as `PerformanceVerdictState.title(baseline)` for the baseline the
  metrics were measured against, so a title can never disagree with its verdict),
  `Pending` while the
  verdict is still being computed - it then renders the facts for real and holds
  the verdict title and description with static decorative bones of the same
  geometry (an `h2`-line title bone at `skeleton.line.medium` and a `b14Med`-line
  bone at `skeleton.line.long`); the bones carry no
  semantics - the comparison and weekly skeletons below, and the records skeleton
  while the `LoadRecords` loader runs, each carry one loading semantics node; the
  streak placeholder shown while the period computes stays silent, since it resolves
  with the comparison skeleton that already announces it. A missing value is
  not drawn as a placeholder dash: the part that would hold it is omitted.
  Every weight on
  this screen carries a space before its unit through the `shortTotal`/`shortAverage`
  family ("400 kg"): hero facts, growth card, records (`AchievementState.totalValue()`)
  and the weekly card alike; the compact `short()` form ("400kg") stays with
  set-level values and the other screens that use it. The screen then
  reads top-down without extra section headings, each card owning its title, in
  this order: hero → `TrainingRecordsCard` (or its skeleton while
  the `LoadRecords` loader runs; hidden once they resolve empty) →
  `PerformanceComparisonCard` growth card (or its skeleton; its baseline segment
  appears only with a baseline) → `WeeklyVolumeCard` (or its skeleton) →
  `TrainingStreakCardStyle.Growth` → `GoalCardStyle.Growth` when a goal exists →
  `MuscleLoadingCardStyle.Session` for this workout's own muscle load. No exercise
  spotlights, no exercise rows and no table-like summary card remain on this
  screen. `contentPadding.block` separates the hero from the cards and `content`
  separates the cards; the cards keep the `screen.horizontalPadding` gutters while
  the hero spans the screen width. The content is a lazy list, so each lower card
  composes, and plays its entrance, as it scrolls into view. Share first scrolls the
  list back to the hero, settles its backdrop (`TrainingRecapHeroState.settle()`
  ends a running entrance and drops a running verdict crossfade) and captures it
  once the settled hero has been drawn, so the shared image always shows the final
  art of its current verdict. A
  period-best verdict or the first non-empty records load fires the shared
  `KonfettiParade` once per screen instance; a started parade never re-arms. While
  saving, the same order renders as skeletons (`TrainingCompletedSkeleton`,
  whose list root carries the single loading node):
  `TrainingRecapHeroSkeleton` with static text bones on the hero's warm surface,
  then the shimmering comparison, weekly,
  streak and muscle card blocks - no records skeleton, because records are unknown
  until the save completes - and Done keeps ButtonStyle.Primary in
  `ButtonState.Loading`. The bottom actions stack full width with a `subContent`
  gap: the Primary Done above the Secondary Share (the `share` label with
  `icons.Share` as its start icon), Disabled until the verdict resolves (also while
  saving), Loading while the share operation runs and absent only in the result
  fallbacks that show no hero. The actions sit on the opaque screen surface:
  `OverlayContainer` adds its `background.screen` fade gap above the actions and
  keeps the opaque surface below it, so no card content shows between or under
  the buttons. Done keeps its resolved overlay insets in every terminal state.
  The top mirrors it: the `top` slot holds only a `statusBarsPadding()` spacer,
  so the status bar sits on the opaque screen surface with the same fade below
  it, scrolled content slides under that fade, and at rest the list starts below it.
  Missing saved details and an unconfirmed result use their quiet copy as the
  screen's `h4` `text.primary` heading below the status bar, without a success
  header.
- `TrainingRecapHero(modifier, training, style, state)` replaces the
  completion BrandHeader and the summary facts in one warm full-width region:
  `illustrationBackground` with all four corners at `brandHeader.radius`,
  `screen.horizontalPadding` plus
  `brandHeader.horizontalPadding` sides so its text starts on the cards' text edge,
  and `brandHeader.verticalPadding`. Like the last-workout recap, its top row pairs
  the text column (verdict title and description, `content` apart) with a compact
  illustration slot (`illustration.compact`) at the end, and the facts row spans
  the region below it. The slot always takes its space, also while `Pending` and in
  the skeleton, so the hero keeps its height when the verdict arrives; a resolved
  hero fills it with the verdict's decorative vector backdrop
  (`PerformanceVerdictState.rememberBackdrop()`, built in `illustration.accent` by the
  resources provider's `recap*Backdrop(accent)`) as a Compact `Illustration`:
  Record a lightning bolt with sparkles, Improved a rising ribbon arrow, Stable a
  balance beam on a fulcrum with two equal orbs, Declined a light floating
  feather, FirstWorkout a start ring with a dotted path to a large sparkle, each
  on a soft radial glow and fully inside the slot, never under text. None of them
  draws a trend or axis that could read as the user's data. It is revealed once
  per screen instance over 900 ms (fade and a `training.recap.backdrop.rise` lift)
  and crossfades in 250 ms when the verdict changes; the reveal lives in the
  caller's saveable `TrainingRecapHeroState` (`rememberTrainingRecapHeroState()`),
  so a hero that scrolls out of the lazy list and back does not replay it. It
  carries no semantics. The shared image is exactly the on-screen hero, at screen
  width with its four rounded corners over the opaque `background.screen` base,
  so every device exports the same card, as before the redesign. These
  vectors are a user-chosen departure from the 3D raster
  illustration family, made because no image generator was available;
  the column keeps `content` gaps between the top row and the facts row. The
  backdrop is the verdict's only image: no icon tile competes with it, and the
  `h2` `text.primary` verdict title takes the whole text column. The Record
  backdrop draws a bolt rather than the Trophy, which stays reserved for
  all-time records (`TrainingRecordsCard`, `AchievementCard`), so a period best
  never reads as an all-time record. The `b14Med` `text.secondary` description
  from the types follows. The verdict title and the growth card's
  Record chip share one "period best" wording per locale
  (`training_recap_verdict_record` = `performance_status_record`: "Period best",
  "Максимум периода", "Максимум періоду", "Melhor do período"), scoped to the
  period so it never reads like the all-time "Personal records" card. The Stable
  verdict speaks of the user's level ("Right on your level", "На своём уровне",
  "На своєму рівні", "No seu nível"), leaving rhythm to the streak card. The Improved
  title names the selected baseline like the description: `training_recap_verdict_improved`
  ("Stronger than usual") under `Average`, `training_recap_verdict_improved_previous`
  under `Previous` ("Stronger than last time", "Сильнее прошлой", "Сильніше за
  минуле", "Mais forte que o último" in both Portuguese bundles, agreeing with the
  workout noun of the segment and chips). Because a baseline switch can also change
  the verdict itself (Stable → Improved), the text column beside the illustration
  slot (≈200 dp at the 412 dp viewport: 412 minus the two 36 dp sides, the `content`
  row gap and the 128 dp slot) holds every title in at most two `h2` lines, and the
  128 dp slot keeps the top row at least as tall as a two-line title over a
  two-line description, so a switch does not change the hero's height at the
  default font scale (uk Average "Сильніше за звичне", matching the chips'
  "звичний", and pt Average "Mais forte que o normal", where "habitual" would wrap,
  stay short for this); a Record never occurs under
  `Previous`, and the Record,
  Stable, Declined and FirstWorkout titles name no baseline. Facts form one
  row of three equal-width columns: workout time, weight lifted and the set count
  (plain `sets`). The hero carries no buttons, because the shared image captures
  the hero layer, and its verdict title carries `heading()` semantics as the
  screen's first heading. Labels come from the metric vocabulary
  (`PerformanceMetricTypeState.Duration.label()` "Workout time",
  `Volume.label()` "Weight lifted", the same words as the growth card and Home)
  and values use the growth card's formatters (`DurationFormatState.shortMinutes`
  "65 min", `VolumeFormatState.shortTotal` "500 kg"). Fact values are `h3`
  `text.primary` with `b11Med` `text.secondary` labels; a missing workout time or
  weight omits that fact (the remaining facts share the row) and zero sets dims to
  `text.tertiary`. Each fact is one accessibility node read label first
  ("Workout time, 65 min").
  `TrainingRecapHeroSkeleton` lives beside the hero on the same warm surface:
  the title and description bones shared with `Pending` and one row of
  three fact bones (`h3` value at `skeleton.line.medium`, `b11Med` label at
  `skeleton.line.long`). The skeleton
  shares the hero's surface, rows and gaps, and every text bone is one laid-out
  line of its role tall (`SkeletonText`), so the skeleton, the `Pending` hero and
  the resolved hero are equally tall while the title and description stay within
  the illustration slot's height and each fact fits one line. Every hero bone is
  static.
  The verdict reads only the metrics that `showsComparison()`
  (`PerformanceVerdictState.of()`); Duration never claims progress or regress.
- MenuSection headings use `h4` and a 22 dp glyph centered in a 32 dp tile with
  8 dp corners. The tile lightly tints the supplied icon hue through the
  theme-owned `icon.tintedBackgroundAlpha` coefficient, preserving caller alpha;
  the glyph retains its supplied tint. Context icon colors keep their meaning.
  The enclosing section owns the grouping surface; Subtle stays unframed with a
  secondary heading. Header gaps use `content`; values stay neutral.
- Working metric panels carry no decorative illustration spot or icon tile;
  Home's welcome and last-workout recap and the completed-workout recap hero own
  the warm illustration regions.
- Training history uses compact Medium exercise rows: a 48 dp Compact image,
  `b14Semi` full wrapping title and `subContent` gaps. Completed/total sets and
  recorded maximum keep their existing summary semantics. Large recording rows
  retain their 64 dp image, `h5` title and `content` gaps. Recording and daily
  history rely on the open row's own `subContent` vertical padding, without
  another list gap or vertical wrapper. Timeline time uses `b14Semi` and duration
  uses secondary `b12Med` text.
- The monthly training digest is a statistics entry point in the common
  detail-entry family: a Large MetricSectionPanel with the `MetricOverviewHeader`
  "Month summary" title and decorative chevron, owning its `scalableClick`. The
  toolbar's month picker owns the period, so the card shows no date range; the
  calendar below owns the training days; the title avoids repeating the toolbar's
  "Workouts" wording above the hero label. One primary `h3` workout count with a secondary `b13Med`
  "Workouts" label leads, a `ContentDivider` follows, and one `MetricBreakdownRow`
  shows duration (`duration`), tonnage (`tonnage`) and sets; the digest and the
  completed-workout recap hero both label sets with the plain `sets` key. An empty month, an unavailable value and a recorded zero use the
  dimmed `text.tertiary` treatment; unavailable duration shows "—" and recorded
  zero shows "0 min". Average volume per workout is not shown.
  The history toolbar uses `divider.subtle` to separate controls from records.
- Home uses `GoalCardStyle.Overview`, `MuscleLoadingCardStyle.Overview` and
  `TrainingStreakCardStyle.Overview` for goal, muscle and streak summaries, and
  `MetricCardPresentation.Overview` for performance summaries. The streak dialog
  summary is `TrainingStreakCardStyle.Detailed`; both keep their established
  rendering unchanged beside the Growth variant.
  Detail dialogs provide the corresponding period evidence and diagnostics;
  muscle details keep the full distribution and expose the PerGroup/PerMuscle
  selector when a known group distribution exists.
  Overview includes the context needed to interpret the current value: muscle
  distribution and session/set counts, inferred training rhythm and active days,
  and a guarded comparison with previous workouts' average. Period history,
  diagnostic findings and timeline breakdowns remain available through the existing card action.
  A quiet hint under Progress identifies the summary cards as detail entry points;
  it appears only when at least one of those cards is present.
- Home card headings use `h5` and `text.primary`; section headings retain `h4`,
  while values and secondary metadata keep their distinct roles. Whole-card
  detail entries (goal, muscle, streak, performance and exercise highlights) use
  `MetricOverviewHeader`: one decorative 18 dp `icon.secondary` chevron at the
  right of the heading. Calendar and Update remain their existing explicit buttons.
- Home's initial loading and an active refresh without period content use
  noninteractive skeleton cards with the dashboard's toolbar, gutters and geometry.
  Each card uses one broad `colors.skeleton.bone` block with shared clipped shimmer,
  without simulated facts, icons or chart details. Panel geometry derives from
  the existing typography, image and spacing slots; section text placeholders
  are one laid-out line of their typography role tall and stay static. The highlights header keeps
  one animated slot, its `b13Med` hint line below the static period-picker bone
  (the larger of the `h6` line and `periodPicker.icon`); the spotlight gallery uses one coarse panel without a
  nested loading scroll. One localized indeterminate loading
  node describes the skeleton; the real period picker remains independently
  accessible for returning users once the range resolves. Known goal/unlock
  availability controls those slots.
  Cached populated content stays visible during refresh; completed empty periods
  retain their real empty presentation. No minimum loading duration is introduced.
- The skeleton family (`skeleton/`: `SkeletonBone`, `SkeletonText`, `SkeletonPanel`)
  owns that geometry: a bone is a `colors.skeleton.bone` block (the
  `divider.default` value in both themes) clipped to its `shape` with the shared
  `shimmer.highlight` band applied after clip and background. The shape defaults
  to the `dp.skeleton.radius` text-line corner; a bone
  standing in for a control or a tile takes that owner's shape (`button.medium.radius`
  for a reserved button). A text bone is one laid-out line of the real typography role it stands
  in for, measured under [typography.md](../.claude/skills/design-system/references/typography.md)
  so the populated content does not jump; the `dp.skeleton.line` scale is short
  40%, medium 60% and long 80%. Width, animation and wrapped-text rules follow the
  [skeleton family rule](../.claude/skills/design-system/references/components.md).
  `SkeletonPanel` content heights sum the same measured lines. Fixed parts keep their tokens: icons, image
  slots, the Segment minimum height, indicators, chip padding and dividers; a `MetricOverviewHeader`
  row is the larger of its `h5` line and the `metrics.overview.detailsIcon`
  chevron. A panel is the real
  `MetricSectionPanel` of its `SkeletonPanelStyle` (Small or Large) holding one
  full-width shimmering bone of the populated content height. Loading semantics
  follow the [skeleton family rule](../.claude/skills/design-system/references/components.md):
  one node per loading region, on the host root or on each section skeleton that
  loads on its own. A shared card's loading
  geometry lives beside the card as its `*CardSkeleton`
  (`PerformanceComparisonCardSkeleton` sized to the baseline segment and the four
  growth blocks with stacked bars; `TrainingStreakCardSkeleton(style)` and
  `MuscleLoadingCardSkeleton(style)` accept only the styles a loading host shows,
  every `TrainingStreakCardStyle` and `MuscleLoadingCardStyle.Loadable` (Overview,
  Summary, Session; Detailed has no loading host), with no default, so each
  host names its style: the streak Overview for Home sizes the header, the
  headline/active-days row (the flame beside the headline alone), the indicator
  and the `training_streak_current_progress` line with three Large spacers; the
  streak Growth for the completed workout sizes the header, the flame beside
  headline and cadence (`maxOf(rhythmIcon, h4 + text + b13Med)`) and the text group
  of the `b12Med` outcome and next-training lines (`text` apart) with two Large
  spacers; the
  streak Detailed for the streak dialog sizes the `h3` headline, the `b14Med`
  description and the `b13Med` active-days line with two Large spacers; the
  muscle Session for the completed workout sizes the title and the map row; the
  muscle Summary for the muscle dialog sizes the `h3` count over its `b12Med`
  label; the
  muscle Overview for Home sizes the title with its `b12Med` period context, the
  map row and the `b12Med` details hint with two Large spacers - data notices
  appear only with real data, so no skeleton reserves them;
  `TrainingRecordsCardSkeleton` sized to the records title/description and two
  record rows, each a name over a one-line kinds line, with their divider,
  `WeeklyVolumeCardSkeleton` sized to the weekly
  header row, the `h3` volume sentence, indicator and `b12Med` count,
  `TrainingRecapHeroSkeleton` on the hero's warm surface, `MuscleLoadingImagesRowSkeleton`
  sharing the front/back figure layout of `MuscleLoadingImagesRow` with bones at
  `muscle.figure.aspectRatio`, `MuscleLoadingSkeleton` with the dominant and two
  further group rows of `MuscleLoading`, and `VolumeMetricChartSkeleton` sized to
  the tonnage caption and the width-derived bar chart), consumed by every host;
  a skeleton with a single host stays private to that host until a second host
  needs it or it reuses the component's private layout.
- Dialog content loads show a private `<Feature>Skeleton` instead of the `Loader`
  spinner, which remains only for the brand splash and debug pages. Each mirrors its
  populated content with the dialog's gutters and gaps, keeps real range headers
  and carries one localized `loading` node on its root: Statistics (two
  `chip.medium` total bones, `VolumeMetricChartSkeleton`, the `h4` Muscles line over
  `MuscleLoadingSkeleton`); performance trend details (Latest row lines, a Small
  metric panel, the `h4` Recent workouts line, a `trend.historyAspectRatio`
  sparkline bone, three history rows with real dividers, a static `button.medium`
  bone); training streak details (`TrainingStreakCardSkeleton(Detailed)`, static
  `h4`/`b12Med` lines over four period blocks, a static `button.medium` bone);
  muscle loading details (`MuscleLoadingCardSkeleton(Summary)`, a static
  `segment.outline.height` bone, `MuscleLoadingImagesRowSkeleton`,
  `MuscleLoadingSkeleton`, a static `button.medium` bone); start training (the
  static fill Segment bone in `segment.fill.outRadius`, the circular
  `illustration.large` halo, the `h4` title and two-line `b14Med` description);
  draft training (the `h3` header and two-line description, the
  `metrics.panel.large.radius` summary tile, the `h4` contents line and three
  exercise-row bones); the completed exercise (`ExerciseSkeleton`: the 64 dp
  image-slot bone with `h4`/`b12Med` lines, `ExerciseSetsSkeleton` with three static
  `IterationCard.Recorded` rows and dividers, `ExerciseProgressCardSkeleton` on the
  card's real Large surface with the shared `ExerciseHistorySkeleton`, and a static
  `button.medium` bone for the details action); and the exercise-details
  Achievements tab (`ExerciseExampleDetailsAchievementsSkeleton`: a clipped row of
  `achievementCard.width` bones, one default-corner bone for the unframed
  estimated-1RM block, the `h4` History line, `VolumeMetricChartSkeleton` and three
  `exerciseCard.small` bones).
- The last-workout recap names its selected-period scope. Title/date and two
  training facts share a row with the unchanged compact journal illustration;
  exercise count and Calendar share the footer. It keeps the illustration gradient
  and large card shape, with content-driven height and complete values.
- Home muscle and training-rhythm summaries are separate full-width large panels;
  neither is stretched to a neighbor's height. Muscle distribution pairs a 144×140 dp
  front/back map with up to three real groups and their existing load shares.
  Its compact image row removes the generic weighted outer spacers, uses two
  equal image slots with an 8 dp gap and centers the complete bodies vertically.
  The muscle-details image layout retains its original default spacing.
  Map and bars use the same relative green load tint from the complete
  breakdown before choosing the three visible rows. The context counts
  selected-period workouts/sets; no share caption repeats the method. Useful anatomy and
  values precede conditional missing-data notes. A single group names its
  uniquely leading individual muscle when that fact exists. Missing groups are
  not invented zeros.
- The training-rhythm overview has the common heading/chevron and a compact row:
  the existing streak headline and cadence on the left, active-day count on the
  right, with its short localized caption. Both columns wrap naturally; the
  existing thin progress indicator follows below. Full history stays in Details.
- Training-rhythm details use one Large neutral summary: `h3` result, `b14Med`
  cadence and a single `b13Med` active-day total. With recorded activity but no
  current series, that total becomes the result and is not repeated. Recent
  evidence stays open on the dialog surface; the calculation explanation sits behind
  a Tertiary disclosure, initially collapsed, like Muscles and Performance.
  Daily history uses up to seven chronological date markers; recorded days use
  `charts.indicator.info`, blank markers mean no record rather than missed training.
  Each marker has a complete localized date and record-status description.
  Weeks and cycles use up to four newest-first ranges, blue indicators and explicit
  counted/reference days; incomplete observation is labelled, without warning or
  success colors. Cycle counts include only training days matching the inferred
  rhythm. Section headings use `h4`; the method sits behind a Tertiary disclosure
  whose body uses `b13Med`. No duplicate summary progress, mood verdict,
  observed-history counter or framed explanation competes with the evidence.
- Goal cards lead with the shared goal mark and primary goal. Training alignment
  is a neutral numerical percentage with an explicit caption and a separate
  semantic status; no completion ring suggests an achieved outcome. Missing
  scores show an unavailable value, and partial evidence retains its notice.
  Shared cards keep the compact calendar row with the actual target and remaining
  time; expiry never implies success. Analyzed-workout counts include only
  eligible sessions in the selected period.
  The sealed GoalCard family owns Overview, Assessment and Calendar styles.
  Goal details use its open Assessment style on `background.dialog`, with the shared
  copper GoalMark, an `h4` goal name, a modest neutral `h3` alignment value and a
  secondary status label with its existing semantic marker. Actual
  analyzed dates, workouts/days/sets remain quiet secondary `b12Med` context.
  At most two non-PASS diagnostics lead in severity/rule order, followed by one
  PASS fact when available. Diagnostic sections use one quiet `background.card`
  surface with the existing breakdown radius/padding and no outline. Each fact
  keeps the same text edge: a full-width `b14Semi` rule title, optional secondary
  `b12Med` muscle context, a `b14Semi` actual value and a separate secondary
  `b12Med` target line. Existing semantic dots align with the first title line;
  ContentDivider separates facts without nested cards or context indentation.
  Actual values retain the existing severity color mapping and threshold-rounding
  qualifiers. Partial-data limitations remain visible; unavailable assessments
  stay neutral.
  Its quiet Calendar style follows the useful findings, using secondary
  `b12Med` date/remaining copy, the shared Calendar icon and the existing Small
  Secondary Update action only when overdue. No second progress scale competes
  with alignment. The disclosure keeps stable order and reveals remaining checks,
  exclusions/metadata limitations and methodology with vertical expansion and
  fade over 200 ms; visible findings are not repeated inside it.
  Weekly training-day diagnostics use compact localized day/week units; their
  criterion heading carries the training context instead of each value repeating it.
  Status wording describes a course to follow or adjust without judging the user.
- Performance labels explain weight/minute and average weight; overview compares
  the latest workout with available prior measurements in the selected period.
  Numeric averages and their percentage comparison occupy separate lines so both
  remain visible within the metric panel's intrinsic height.
  Duration comparison stays neutral in both Home and Details; no duration status
  or best-duration claim is shown. Aggregate values preserve fractional means and
  format through canonical unit owners without input-field limits. On Home,
  percentage colors follow the status through
  `PerformanceMetricState.averageDeltaColor()`: `semantic.success` above usual,
  `semantic.decline` below usual, with ordinary variation staying neutral. The
  completed-workout growth card keeps its signed change in `text.secondary`
  instead, because its chip and bars already carry the status.
- Exercise highlights use the Large panel geometry (20 dp corners, 16 dp padding)
  in a horizontal gallery with regular 64 dp thumbnails,
  complete insight text and existing detail actions; a next-card peek signals more
  items without repeating a tall vertical report. Cards share the tallest content's
  natural height. The gallery scroll viewport reaches the screen edges; internal
  start/end gutters align the first and last cards with the other Home sections.
  Insight kind is conveyed by its existing localized semantic chip; the card has
  no colored edge stripe. Its image/title/chevron header shares the card hierarchy.
- MenuValueRow keeps neutral labels/values and a quiet chevron, with a 56 dp
  minimum height and 12 dp vertical padding/gaps. A loading value is a shimmering
  `b14Med` `SkeletonText` at the `skeleton.line.short` width of its value slot. Optional leading icons use a
  32 dp card-surface tile with 8 dp corners; caller-supplied channel/destructive
  colors retain their meaning. Dividers align with the text after the tile.
- MenuActionRow uses the same menu-row icon and spacing family for operations:
  `b14Semi` verb and attached secondary `b12Med` explanation, with no navigation
  chevron. One full-row Button target owns enabled/disabled/loading semantics;
  loading preserves content geometry and uses shared shimmer. Destructive verbs
  and icons retain semantic error color; helpers stay neutral. Dividers align
  after the icon tile.
- Equipment rows retain a 64 dp image slot with 12 dp surface padding on all sides.
  Highlighted controls reserve their indicator space so selection does not shift text.
- Repeated ToggleSelectableCard rows and Medium CheckSelectableCard controls use
  the 16 dp shape. Small filter choices retain 12 dp corners, and Large experience
  choices use the 20 dp card shape with their full-width description below the title row.
- Inputs already own their neutral surface and shape. Keep related fields in a
  spacing group without a second form-card frame; align their outer edges with
  the screen header and primary action. A branded hero and a standalone input
  have different roles and do not need the same corner radius.
- Inputs use a thin neutral resting outline and the warm input focus outline;
  invalid values keep semantic error styling after blur. Empty labels use b14Med,
  floating labels b12Med and values b15Med. The 50 dp minimum height grows with
  content; 16 dp horizontal and 4 dp vertical padding keep forms and compact
  weight/repetition fields in the same control family.
- Input focus reuses `border.focus`; invalid fields expose their existing error
  state to accessibility without changing validation or its blur timing.
  Labels may wrap independently of the editable value's single-line limit.
- Checked controls use the matching foreground for their fill: a light mark/thumb
  on graphite in light theme, and graphite on a light neutral fill in dark theme.
  Check marks use selection.onCheckedIndicator; disabled checks use
  selection.onDisabledIndicator over icon.disabled. Disabled switches keep their
  existing disabled roles and remain distinct from unchecked choices.
- Tabs announce selection, equipment/muscle switches announce their labels and
  checklist rows announce completion independently of decorative icons/colors.
- Toggle selection cards expose one Switch action and state at the enclosing
  row; their trailing Toggle is passive. Input clear/copy/password actions have
  separate localized labels. Read-only exercise summaries have no click target
  or detail chevron; recording/completed-workout rows retain their detail action.
- Performance details lead with the latest-workout label/date and one Small
  MetricSectionPanel summary. The existing metric icon uses `icon.secondary` beside
  a neutral `h4` value. Home card headings and the host title share the friendly
  metric name owned by `PerformanceMetricTypeState.label()` / `labelText()`.
  Earlier average and signed change use secondary `b12Med`; the average label names
  the comparison baseline once and the adjacent delta uses a short change label.
  Detailed summaries omit status chips and best-value repetitions; missing current
  data, missing earlier data and a zero earlier average have distinct quiet copy.
  Home Overview retains its title/chevron, statuses, comparisons and paired
  intrinsic-height rows; for an empty metric it shows the Detailed card's
  `performance_trend_details_value_unavailable` copy as one secondary `b12Med`
  line instead of a value. Complete localized names, units and explanations wrap.
- Performance history uses open date/value rows for up to nine previous sessions;
  the latest session appears only in the summary. Secondary `b13Med` dates, neutral
  `b14Semi` values and `subContent` vertical padding keep the rows quiet. Same-day
  sessions add secondary `b12Med` times in both summary and rows; `ContentDivider`
  separates adjacent rows only. The blue elapsed-date chart retains up to ten recent
  sessions with the compact `metrics.performance.trend.historyAspectRatio` of 3.2.
  A single session has no history section.
  A Tertiary calculation disclosure follows the content, initially collapsed with
  coordinated expand/shrink and fade. Metric definitions and their limits use
  secondary `b13Med`; chart scope and one shared comparison rule use `b12Med`.
  The chart-scope sentence appears only with a rendered chart; the comparison rule
  remains available in the disclosure even when there are too few chart points.
- The completed-workout "Before → after" growth card (`PerformanceComparisonCard`)
  is one Large MetricSectionPanel with the `h5` `performance_comparison_title`
  (`performance_comparison_first_title` when it has values but no baseline) and no
  description, then an Outline `Segment` (`SegmentWidth.EqualFill`) that
  chooses the baseline - `PerformanceBaselineState.Average` "vs average" or
  `Previous` "vs last workout" (labels from `PerformanceBaselineState.labelText()`)
  - and then growth blocks in the fixed order Volume, Repetitions, Intensity,
  Density separated by `ContentDivider`. Duration is not a row here: it never votes
  and the hero already states the workout time, so every row carries a status and
  the hero's "N of M" equals the rows. Both segment labels fit one
  line in every locale at the standard viewport: an `EqualFill` half of the Large
  panel leaves about 138 dp for the `b14Bold` selected label, so the labels stay
  short - en "vs average / vs last workout", uk "до середнього / до попереднього",
  ru "к среднему / к прошлому разу" ("к прошлой тренировке" wraps), pt
  "vs média / vs último treino" - and a new or changed label is checked against
  that width. The "Before: X" sub-line keeps
  its wording under both baselines; the metrics already carry the matching before
  value, the caller owns the selection and the card never recomputes it. Under
  `Average` a status has to clear the period's noise band (at least 5 %, wider
  when earlier sessions vary); under `Previous` a single earlier value has no
  spread, so a fixed ±5 % band decides it. The segment renders only when some
  metric `hasAverage()` - an earlier value exists under the selected baseline,
  zero included - and a block's "Before" line and its bar pair only when its own
  metric has one. Earlier values exist under both baselines as soon as the period
  has an earlier workout, so a switch never removes the selector and the user can
  always switch back. The hero reads `FirstWorkout` on a different predicate - no
  metric `showsComparison()` - and the two agree in practice, not by definition:
  every saved workout has at least one set with repetitions, so an earlier
  workout always gives Repetitions a nonzero baseline and a comparison under
  either baseline. With an earlier workout the card shows its selector and the
  hero a real verdict; without one the card is titled
  `performance_comparison_first_title` ("This workout") and shows its blocks
  without a selector, bars or a note of its own, because the hero explains the
  first workout. An
  empty card shows neither. Each block owns its
  `scalableClick` and `subContent` vertical padding and ends its first row with
  one decorative `ArrowRight` chevron (`metrics.overview.detailsIcon`,
  `icon.secondary`, vertically centered after the value column) like
  `MetricOverviewHeader`. Its first row keeps the metric icon at
  `metrics.performance.trend.icon` in `icon.secondary`, centred on the first line
  of the name (a box one measured `b13Semi` line tall, aligned to the row's top,
  like the goal diagnostic dots on their first title line), not on the label
  column, the `b13Semi`
  `text.primary` name with the `b11Med` `text.secondary` "Before: average"
  sub-line (omitted without an average) and, for every metric that
  `showsComparison()` (it votes and `hasComparison()`: a percentage change, or a
  period best, which needs none), the status chip (`PerformanceTrendChip`, worded
  for the selected baseline through `PerformanceTrendStatusState.label(baseline)`:
  "Above usual / Within usual range / Below usual" under `Average`, "Above last
  workout / Like last workout / Below last workout" under `Previous`, and "Period
  best" in either - a Record is never produced against the previous workout, and
  no preview pairs the two;
  Home's Overview `PerformanceMetricCard` shows its chip on the same
  `showsComparison()` and passes `PerformanceBaselineState.Average` to the same
  `label(baseline)`, and the detailed summary shows no chip) under that
  sub-line in the left column; the right column holds the `h5` `text.primary`
  value that counts from the average to this workout through `displayValue()`
  with the `b12Semi` signed change in `text.secondary` beneath it whenever a
  percentage exists, Record included (a period best over a zero baseline keeps its
  chip without a change): the chip and the bars already carry the status, so the
  number stays quiet and a Declined row does not repeat the decline accent three
  times. The `PerformanceComparisonCardSkeleton` sizes its four blocks with the
  chip line. Private `GrowthBars` follow across the
  block as two stacked rounded bars of `metrics.charts.indicator.height` with a
  `contentPadding.text` gap, shown only for a metric with a value and a before
  value (a row without one carries no bars), read top to bottom like
  "Before → after": the top
  "before" bar is a `charts.indicator.before` fill to the baseline - a neutral
  visible on the card yet quieter than the muted "after" fill, on the muted track
  (WCAG contrast against `background.card`: Light NeutralLight.N400 2.3:1 beside
  muted N500 4.8:1; Dark NeutralDark.N450 1.75:1, 1.5:1 on its track, beside muted
  N500 2.1:1, where the earlier N400 at 1.4:1 nearly vanished) - static during the
  entrance, so in Light a Stable pair reads apart by tone (≈2.1:1
  between the bars) as well as by position; in Dark the tones stay close (≈1.2:1),
  so they read apart mainly by position and length, the values being in text; the
  bottom "after" bar fills to this workout, its
  fill and track arriving from `charts.indicator.muted` at the status palette. Each
  pair is scaled by its own larger value, `max(before, after)`, not by the period
  best: the larger value fills its track, so +7 % reads as two nearly full bars of
  visibly different length, a period best still fills its after track and an after
  above before fills the after bar. During the entrance the after bar starts at the
  before bar's length. A row without a value or a before value draws no bars. The
  after bar's track and fill take `PerformanceMetricState.indicatorColors()`:
  `charts.indicator.success` for Record/Improved, `decline` for Declined and
  `muted` for Stable. The brand
  `charts.indicator.primary` is not a neutral here: its dominant is the warning
  orange in both themes. The entrance is a 900 ms FastOutSlowIn tween through one
  card-level `updateTransition` over a `rememberSaveable` reveal flag (played once
  per screen instance, not on every lazy re-entry): only the running entrance
  (false → true) staggers the blocks, 120 ms each. The delay comes from the
  transition's states (the current state still behind the target), not from its
  segment, which keeps reading false → true after the entrance settles, so any
  later target change, such as a baseline switch, moves every block at once
  without delay on a short 250 ms FastOutSlowIn tween: the after bar's fill, track
  and length (its pair's scale moves with the baseline) and the before bar. The
  current value does not depend on the baseline and stays put. The parts a switch
  adds or drops - the "Before" line and the bar pair with the earlier value, the
  chip and the signed change with the comparison - are `AnimatedVisibility` parts
  (`expandVertically` + `fadeIn` from the top, `shrinkVertically` + `fadeOut`
  toward it) on the same 250 ms tween, and the parts that stay but reword - the
  chip, the "Before:" line and the signed change - crossfade on it (`AnimatedContent`,
  fade only, no size animation, aligned to their reading edge), so a "Below usual"
  chip never sits on a still-green bar. A switch during the running entrance
  retargets with the transition's spring. Each part carries its own gap
  (`contentPadding.text` for the text parts, `subContent` for the bar pair, whose
  two bars stay `text` apart), so the gap animates with it, and a leaving part keeps
  its last values (a chip keeps the baseline
  wording it was measured in) until it is gone; the block's height follows the
  parts, so nothing is clipped at the old height and nothing jumps. Only the
  value text reads the animated number every frame; the bars read their animated
  values while drawing. Motion never gates the block's press target.
  An empty metric shows no value and no bars (a full "before" over an empty
  track would read as a drop to zero); without any baseline the rows show their
  values without bars under the first-workout title; without any values the title
  is followed by one secondary `b13Med` empty note and no blocks.
- `TrainingStreakCardStyle.Growth(previous)` is the completed-workout streak: the
  shared `metric_overview_streak_title` heading with chevron, a `Flame` at
  `metrics.overview.rhythmIcon` beside an `h4` `text.primary` headline over the
  `b13Med` `text.secondary` cadence, and one `b12Med` `text.secondary` outcome
  line. It draws no indicator bar: without the Overview's progress caption a bar
  would read as progress toward an unstated goal, and Overview and Details keep it.
  The headline is always `overviewHeadline()` with its unit ("5-week streak") and the
  flame keeps the mood colors (`mood.indicatorColors()`) like Overview and Detailed;
  a change is one fact in one color, so only the current length carries the
  direction. When the previous length differs the outcome line
  joins the outcome text and `training_streak_growth_headline` through
  `joined_pair` ("Streak extended · 4 → 5"), the previous length in the line's
  `text.secondary` and the current one in the direction color
  (`semantic.success` after growth, `semantic.decline` after a drop), like the goal
  score, stated as previous →
  current from the
  first frame while only the current length fades in over 900 ms behind a
  `rememberSaveable` reveal flag (once per screen instance), so it never reads
  "4 → 4"; an unchanged length shows the outcome text alone
  (`training_streak_growth_kept`), and no outcome line is shown without a
  previous state. The outcome line and the forward line form one text group at
  `contentPadding.text` below the headline row, not separated by the panel spacer. The
  second `b12Med` `text.secondary` line looks forward through
  `TrainingStreakState.nextTrainingLine()`: `training_streak_next_date` with the
  rhythm's next day (`nextTrainingDate`, contextual `DateCompose` "Tomorrow" or
  `DateDdMmm`), otherwise for a Weekly rhythm the days still due in the current
  timeline entry (`training_streak_next_weekly`, or `training_streak_week_done`
  at zero); other rhythms without a predicted day show no line. The field has no
  default: every producer states whether a next day exists.
- `GoalCardStyle.Growth(previous)` is the completed-workout goal card: the Course
  identity row shared with the Overview (`GoalIdentity`: `GoalMark`, `b12Med`
  `goal_course_title`, `h5` goal label, decorative chevron), an `h3` score line -
  the numbers in `h3`, one step below the hero's `h2` heading so the score does not
  compete with it, the percent signs and the arrow in secondary `b14Med` - that
  becomes `goal_growth_score` ("68% → 76%") when the
  previous score differs, the previous score in `text.secondary` beside the
  current one, stated from the first frame while only the current score
  fades in over 900 ms (saveable reveal, once per screen instance) and the bar fills
  from the previous score to the current one, so it never reads "68% → 68%"; its
  current score is colored by direction like the streak's current length (`semantic.success`
  when the score grew, `semantic.decline` when it dropped, `text.primary` when
  unchanged or without a previous score). The `b12Med` status label takes
  `statusColor()` and a `LineIndicator` of the score, drawn only with a score,
  takes `statusIndicatorColors()` - `charts.indicator.success/warning/error` by the
  `GoalProgressState.ON_TRACK_MIN/DRIFTING_MIN` thresholds - after a drop as well.
  How the score moved and the band it now falls in are separate facts: only the
  score numbers take `semantic.decline`, while the label and the bar keep the band of
  the current score, so a drop into the off-track band keeps its red error instead
  of reading as the calm decline. The `b12Med` analyzed-workouts line closes the
  card. A missing score shows only the not-evaluated label, with no score line
  and no bar.
- `MuscleLoadingCardStyle.Session` is the completed-workout muscle load: a Large
  panel with the `h5` shared `muscle_loading` title, the Overview's compact
  144×140 dp front/back map beside up to three group rows with their relative
  green load tint, and the conditional data notices. It shows no period
  workouts/sets context, no leading-muscle line and no details hint; empty entries
  show `muscle_load_no_data`.
- `TrainingRecordsCard` is the completed-workout "Personal records" card: a Large
  MetricSectionPanel with the `h5` `training_records_title`, one `Trophy` at
  `metrics.performance.records.trophyIcon` in `semantic.success` at the end of the
  title row (the card's single celebration accent: every row is a record, so the
  rows carry none) and the `b13Med` `text.secondary`
  `training_records_description`, then one row per exercise
  example (`TrainingRecordState`, its achievements in the order best weight, best
  tonnage, max repetitions, peak intensity) separated by `ContentDivider`: a
  `SMALL` (32 dp) `ExerciseExampleImage`, the `b13Semi` `text.primary` exercise
  name once, over one plain `b11Med` `text.secondary` line listing every kind with
  its value - `AchievementState.text()` and `totalValue()` (spaced weights such as
  "92.5 kg", repetitions as "x14"; `AchievementState.value()` keeps the compact form
  for exercise details), the kinds joined by " · " ("Best weight 22.5 kg · Best
  tonnage 380 kg · Max repetitions x10 · Peak intensity 21.1 kg") and wrapping only
  between whole kinds, without truncation (no-break spaces join each kind's words and
  value and hold the dot to the kind before it, so the line never breaks mid-phrase
  like "Max / repetitions"; at most four kinds - at the standard viewport a first
  attempt takes two lines in en, three in ru/pt and one per kind in uk - and an
  ellipsis would hide a record) - then one decorative
  `metrics.overview.detailsIcon` chevron centered on the row, while the thumbnail
  stays top-aligned with the name when the kinds wrap. The row stays
  light: no nested cells, chips or per-kind value columns. Each row owns one
  `scalableClick` to its exercise example (`onRecordClick(exerciseExample.id)`)
  with `subContent` vertical padding; its spoken text reads the name, then every
  kind in full with commas instead of the dots. The caller shows the card only when
  records exist; records are evidence, not selection, so the row carries no orange.
- `WeeklyVolumeCard` is the "This week" tonnage card: a Large MetricSectionPanel
  whose header row pairs the `h5` `weekly_volume_title` with the `b12Med`
  `text.secondary` `weekRange.short()`. With a typical week one localized
  sentence, `weekly_volume_progress` ("6 450 kg of your usual 9 800 kg", both
  values through `VolumeFormatState.shortTotal`), renders as one `AnnotatedString`
  on one baseline: the current tonnage in `h3` `text.primary` and the rest of the
  sentence in `b12Med` `text.secondary`, wherever the locale places the value.
  Without a typical week - no training in the recent weeks before this one - the
  `h3` tonnage stands alone and `weekly_volume_no_typical` ("No recent weeks to
  compare with yet.") follows; a
  `LineIndicator` of `WeeklyVolumeState.progress()` (a `Float?`, null without a
  positive typical week) uses `charts.indicator.muted` below the typical tonnage and
  `charts.indicator.success` from 100% on - never `charts.indicator.primary`,
  whose dominant is the warning orange; without a typical week there is no bar at
  all, the note alone explains the absence; the `b12Med` `weekly_volume_trainings`
  count closes the card. Zero tonnage against a positive typical week shows
  "0 kg" with an empty muted bar. A bar appears only against a positive typical
  week: a zero typical week (recent weeks without tonnage) gives no share, so
  `progress()` is null, the sentence still states both values ("0 kg of your usual
  0 kg") and there is neither a bar nor a note; it never reads as reached. No
  typical value is ever invented.
- Regular exercise thumbnails remain `ExerciseExampleImageStyle.MEDIUM` (64 dp).
  Medium history/read-only exercise rows use `COMPACT` (48 dp); small inline
  exercise references may use `SMALL` (32 dp).
- Exercise-library results use open `ExerciseExampleCardStyle.Large` rows with
  regular 64 dp thumbnails on the left. The right column has complete `h5` names,
  then an explicit usage status, followed by image-only metadata.
  A positive usage count or a recorded last-used date indicates history: a
  passive `ChipSize.Small` / `ChipInteraction.Static` uses the shared chip
  geometry and `b11Semi` value style, `background.card`, `text.primary`,
  `border.default` and a neutral `icon.secondary` Calendar. With an empty label,
  its value shows the localized last-used date, or “In your history” when the
  date is absent. The history value uses the shared two-line limit and wraps
  beside the icon. Exercises without either history signal use a plain `b12Med`, `text.secondary`
  “Not tried yet” caption. History is neither selection nor success, so it has
  no orange or feedback tint. Equipment uses the shared Compact treatment;
  missing equipment keeps an explicitly labelled declared weight type.
  All muscle groups are represented by one upper-body mark and, when needed,
  one legs mark at `muscle.summaryMarkSize` (32 dp), with neutral `icon.secondary`
  highlights and complete localized accessibility names. The equipment and
  anatomy clusters stay in the right
  column with `content` spacing between them and `text` wrapping gaps. Missing
  groups leave no empty row.
  Quiet dividers follow the content gutters; repeated taxonomy chips and outer
  card frames do not compete with selection. The usage status stays in each row
  across catalog, picker and replacement modes, preserving the supplied order
  when similarity-ranked suggestions mix used and unused exercises.
- Replacement-library targets use the passive Small reference with a complete
  `b14Semi` name and a 32 dp image inside a neutral `FocusFrame`
  (`background.card`, `border.default`), introduced by a `b12Med` caption.
  The reference is distinct from selectable results and carries no selection accent.
- The recording editor uses Medium as an open identity header: regular 64 dp
  image, full `h5` name and neutral `b12Med` category/force metadata. Its existing
  load-type line explains the actual set input, so weight type is not repeated.
  The enclosing single Button action owns a decorative `icon.secondary` chevron
  sized by `exerciseCard.chevron`; passive card variants add no detail action.
  While matching catalog metadata is unavailable, the same Medium identity uses the
  existing exercise draft's value without a click or chevron. The load-type line
  waits for its real components; identity does not disappear during that interval.
- Exercise image slots use `background.card`, a thin `border.default` boundary
  and size-specific corners: 8 dp at 32 dp, 12 dp at 48 dp, 16 dp at 64 dp and
  20 dp at 100 dp. Loading/missing/error placeholders use
  `icon.secondary` with size-specific padding, keeping the 32 dp slot readable
  in both themes. Loaded images retain their colors and crop; image slots do not
  carry selection or status emphasis.
- Use `contentPadding.block` between sections, `content` between related controls,
  `subContent` between compact items and `text` within a text group. Compact menu
  sections use `content` spacing.
- Standalone profile editors separate toolbar and introduction with `block`;
  explanations meet related choices at `content`, while the anatomy region starts
  a new section at `block`. Settings and onboarding consent use a primary `h6`
  title beside Toggle, with `content` between them. Onboarding keeps only this
  compact row. Settings adds a secondary `b13Med` explanation at `text` spacing,
  spanning the width below the row so privacy copy does not compete with the switch.
- Profile screens compose their own toolbar and guidance locally. Editor titles
  use a transparent toolbar with Back. Guidance sits outside the toolbar, uses
  `block` before it, starts at the screen gutter and uses `b14Med` /
  `text.secondary`; the following content owns its section spacing.
  Editor copy states the current task, with muscle/equipment copy describing
  enabled choices. Community uses the same quiet guidance without a second hero
  title or logo; the root menu keeps its identity card in the scrolling content.
  Settings uses the Account toolbar title to match its menu entry. Signed-in
  identity, one MenuSection containing the complete Privacy preference, and two
  open MenuActionRow operations share one scrolling body. General introductory
  copy and duplicate deletion headings are omitted. Each operation keeps its
  explanation beside its verb/icon; a text-aligned divider separates Logout from
  Delete. Account actions mutually disable during work.
- The goal editor groups primary focus, secondary goal and target date in one
  cohesive summary/editing card using `menu.radius/contentPadding/borderWidth`,
  `background.card` and `border.default`. The leading focus has a secondary
  `b13Med` Course label, shared GoalMark, primary `h4` value and secondary chevron; the following
  MenuValueRow controls share the surface and quiet dividers. Optional status
  appears only for an empty secondary goal. Invalid dates keep their error label,
  accessibility error semantics and `b12Med` explanation inside the same card.
  Routine, preferences and recovery/limitations are independent collapsed
  summary/editing cards in the same menu geometry. Each header uses a neutral
  Calendar/Tune/Body icon with `menu.headerIconContainer/headerIconRadius/headerIcon`,
  a `b14Semi` title, a secondary `b13Med` factual summary and a secondary chevron.
  A single selected option shows its complete label. Routine additionally
  summarizes one or two selected schedule/duration values; remaining summaries
  use a selected-option count. Empty groups omit absence copy.
  Only one card opens at a time. Routine exposes its three choice groups directly;
  the larger groups retain category disclosures and complete selected summaries.
  Quiet dividers and Small CheckSelectableCard choices organize expanded content.
  One scrolling body keeps the pinned Primary Save action on an opaque screen
  surface; permanent introductory and field-description copy is omitted.
- Streak and performance details use `block` before a new section heading.
  Headings meet cards or charts at `content`; performance history retains the
  compact row spacing defined above. Compact goal
  details use `content` between their summary, diagnostic and disclosure blocks,
  and from a diagnostic heading to its card. The first visible section has no
  leading separator. Statistics, streak, performance and muscle detail ranges
  share `DialogRangeHeader`: centered secondary `b14Med`,
  `dialog.horizontalPadding` gutters and `content` below, with no extra top gap
  after the toolbar. The range header owns the single gap before its body;
  muscle detail controls likewise keep one `content` gap before their scrolling content.
  Other sheet bodies start once at `dialog.top`.
- Open dialog sections use headings and spacing rather than decorative dividers.
  Statistics' muscle breakdown uses the same `h4` heading and `content` gap as
  other detail sections; expanded goal evidence has no leading rule. Dividers
  separate repeated list rows or meaningful groups inside a compound card,
  rather than the sheet's date, controls or open sections; the one exception is
  the host-edge rule that closes a pinned chip row above a scrolling list
  (Start workout Repeat), the same boundary Library draws under its header.
- Bottom-sheet toolbars reserve equal Small icon-button slots on both sides of
  the title, including when Back is absent, so titles and centered date ranges
  share the sheet's center line.
- Optional descriptions, suggestions, equipment and history leave no separator
  when absent. Present sections own the gap between them; text groups use `text`.
  Equipment galleries use `content` between cards, keeping image and card sizes.
- Training totals stack full-width LONG chips with `subContent` between present
  metrics, preserving complete localized labels and large values in both hosts.
- Scrolling sheet lists end with the bottom overlay's `block` fade gap above one
  navigation inset, through the resolved content padding; a pinned sheet action
  keeps `dialog.bottom` below it. A footer-only lazy item must not add an item gap
  to that margin. Main-tab content uses `screen.verticalPadding` at its bottom edge:
  Home dashboard/loading/welcome, Calendar daily/monthly (including empty/loading),
  Library lists/empty results and Profile content/pinned actions. Scrolling bodies
  own that margin through content padding; Main owns the bottom-bar navigation inset.
  Pinned actions still reserve their measured height through OverlayContainer.
- Editable set rows own their vertical padding; the list draws the optional divider
  below the swipe-reveal content and omits it for the final or only row. Their list
  adds no second row gap; the Sets heading
  keeps `text` bottom padding beside the first row's
  `subContent` top padding. Exercise sheets scroll long content rather than
  compressing text, images, sets or the final action.
- Start workout sheets expose available Custom, Suggested and Repeat choices
  through pinned equal-width Segment controls. All modes fill the same available
  host height so the sheet, choices and action stay in place when content changes.
  Without a choice segment, the body starts at `dialog.top`; a present segment
  keeps `block` before its body.
  Custom centers the existing empty-exercises artwork in IllustratedEmptyState
  with a Large illustration, `h4` title and secondary `b14Med` description.
  Populated modes use a left-aligned `h3` introduction and secondary `b14Med`
  explanation. The introduction stays above the exercise scroll
  area. Repeat pins its Small CheckSelectableCard historical dates after that
  introduction without a caption. The date row
  uses the full dialog width with `dialog.horizontalPadding` as internal scroll
  content padding and ends with a host-edge `ContentDivider` with a `content`
  gap on both sides, mirroring Library; the plan content retains the same gutter. Plan previews
  lead directly into ExerciseCardStyle.Planned with Compact exercise images, full
  wrapping `b14Semi` names and secondary planned sets/repetitions; they carry no
  completion counters, stale tonnage or detail chevrons.
  The single pinned Primary action names the selected next step, with measured
  OverlayContainer padding rather than a manually reserved overlay height.
  Its pinned action padding includes the single navigation inset; the action sits
  on the opaque dialog surface below the shared `block` fade over the exercises.
- Draft workout sheets use a left-aligned task heading and short secondary
  explanation. A single Large neutral metrics panel groups exercise count,
  filled/all sets and positive saved duration with the existing `TrainingStat`
  (`h5` values, secondary `b12Med` labels); zero duration is omitted. Empty drafts
  show guidance instead of a zero-value panel. Read-only Medium exercise rows
  stay open with text-aligned dividers between adjacent rows. Introduction,
  summary and contents heading stay above the exercise scroll area. Full-width
  Primary Continue and Secondary Start new stay pinned on the shared
  `background.dialog` bottom-action backing with the navigation inset: the
  container's `block` fade gap above the actions keeps both buttons on the opaque
  dialog surface.
- Workout completion confirmation keeps the host title and left-aligned secondary
  introduction. One Large neutral metrics panel groups only recorded exercise and
  set counts with `TrainingStat` (`h5` neutral values, secondary `b12Med` labels);
  planned empty sets and exercises without recorded sets do not inflate the recap.
  The shared `InputDuration` follows at `block` spacing, with a secondary `b12Med`
  adjustment hint at `text` spacing. Full-width Primary Save workout and Secondary
  Keep training actions follow at `block`, separated by `content`; warmth stays in
  the primary action, with no duplicate success header or decorative artwork.
- A completed-exercise sheet leads with an open 64 dp identity row, a complete
  `h4` name and a secondary `b12Med` session date. Their Sets heading uses
  `subContent` bottom padding before the first row's `subContent` top padding.
  The ordered sets use
  `IterationCard.Recorded` first: a small passive number badge (`iterationCard.numberSize`),
  neutral `h5` load × repetitions and quiet dividers only between rows. Actual ordinal
  prior values belong to the feature-provided trailing slot, using secondary
  `b12Med` in a fixed `iterationCard.historyContextWidth` column, introduced once
  by a short previous-date header. Each context slot reserves two laid-out
  secondary lines in loading, populated and absent-match states, its loading bone
  one line; no repeated Last time label competes with the current result.
  Bodyweight calculations stay beside their effective load. Missing per-set
  measurements remain unavailable; optional absent body mass does not hide known
  extra or assist weight.
  `IterationCard.Compact` retains the compact load/repetitions and body-calculation
  role. The Recorded variant owns its number and neutral caller-provided trailing
  slot; the shared entry point requires an explicit variant. The entity
  component contains no prior-workout or history-loader contract.
  One Large neutral metrics panel follows the sets. Its three equal cells use
  `h5` current volume/repetitions/maximum effective load and secondary `b11Med`
  labels, without repeating set count or three prior metric rows. Secondary volume
  history uses a quiet `b12Med` heading, neutral signed percentage and one short
  prior-volume/date caption. The percentage has no status fill or success meaning.
  Both caption and chart retain reserved geometry while history loads, is empty
  or contains unsupported tonnage. Skeletons are the shared `SkeletonText` caption
  line and a `SkeletonBone` chart region with the small panel corner. Shared
  clipped shimmer animates only the previous-date header and chart region; per-set and caption slots stay static
  regardless of set count. The card exposes one localized
  indeterminate node while individual bones remain decorative. No minimum loading
  duration or business animation is added.
  Chronological stored-volume entries use the existing blue growth palette: P300
  for prior entries and P500 for the selected entry. Changed assistance or added
  weight does not hide known prior results. Empty history or unsupported tonnage
  shows a neutral `BannerCard.Custom` in the same reserved chart region. Its dialog
  surface and small panel radius separate unavailable comparison from
  current facts; its icon is secondary and its copy keeps the actual reason. The compact chart retains
  `metrics.volume.chart.historyAspectRatio`, date-only labels, tooltips and the full
  selected-entry accessibility caption. It uses the same flat shared bar style as
  workout statistics and exercise-details history. Recorded volume does not imply strength
  progress. Finite nonnegative volumes are the supported chart domain; unsupported
  values show quiet explanatory copy in the same reserved region while their
  recorded numeric facts remain visible. Nonnullable stored totals retain actual
  zero values. Maximum effective load remains unavailable if any performed set
  lacks calculable load; the summary does not present a partial maximum as complete. The existing
  secondary details action remains last. Empty records keep identity and details
  without fabricated sets or summary.
- Exercise-details history entries (`ExerciseCardStyle.Small`) are repeated framed
  rows on the dialog surface: `background.card`, `border.default`,
  `exerciseCard.small.radius` (16 dp) and medium padding, with no image, name,
  tonnage or completion counter because the sheet header already identifies the
  exercise and the entry exists to show what was done. The `h6` session date
  leads; a neutral caller-owned trailing slot beside it carries the
  session-over-session tonnage change as a secondary `b12Semi` labelled signed
  percentage ("+8% volume"), omitted for the oldest entry or a zero baseline and
  without a status color. The recorded sets follow as one wrapping line of
  primary `b14Semi` "load × reps" results, each followed by a tertiary middle
  dot inside the same text so a wrapped line never starts with a separator, with
  no nested cells or frames; a missing value shows "—" and a recorded zero shows
  "0kg", and an entry without sets shows the secondary `b12Med` no-sets caption
  instead of the line. The chart axis uses the same day-and-month `DateDdMmm` vocabulary as
  the completed-exercise sheet; the tonnage history chart above the entries
  keeps the shared flat bar style with adaptive X labels.
- Keep control heights as minimums when content depends on font size. Chips, set
  cells, selectors and segmented labels must grow instead of clipping text.
- Keep one layout across system font scales: single-line button labels, centered
  toolbar titles within the action slots, and the regular dashboard/card grids.
- Keep indicator date labels in separate bounded slots and allow them to wrap.
- Let tags wrap. Do not hide a weight type just because the preceding tags are long.
- Preserve readable content when lists scroll behind a pinned action. Pass the
  bottom overlay's resolved padding into the list.

## Content dividers

- Neutral horizontal boundaries use `ContentDivider` with `divider.subtle` and
  `divider.thickness` (1 dp) in both themes. Its color is fixed by the component
  and cannot be overridden by callers. Vertical metric separators share
  those color and thickness roles. Selection indicators, timeline connectors,
  chart guides and skeleton fills retain their own roles.
- Open dialog headers, controls and sections use their shared spacing without
  an extra horizontal rule. Where another host requires a boundary between fixed
  content and a scrolling body or bottom navigation, it reaches the host edges.
  The surrounding content owns its horizontal gutter; the divider adds no inset.
- Open list rows separate only adjacent items. Their lines end at the content
  edge and start at the primary text after a decorative image or icon. Set
  numbers are content, so set-row lines span the existing row gutters. The last
  or only row has no trailing line.
- Independent framed cards and a section heading with its own body do not need
  a second boundary. Labelled splitters appear only with their section; empty
  selection groups leave no orphan line below absent tabs.

Before changing shared components, inspect their variants and whole-screen
consumers in both themes. Use the [standard viewport scope](../.claude/skills/design-system/references/visual-consistency.md#default-viewport-scope)
and long localized text; very small devices are outside default visual acceptance.
Compilation alone does not verify visual composition.
