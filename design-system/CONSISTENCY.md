# Visual consistency

For visual intent and composition, apply the canonical
[brand-design skill](../.claude/skills/brand-design/SKILL.md). This document owns
concrete product role-to-token choices; component/token APIs remain in source.

Use `AppTokens` for colors, typography and dimensions. Keep a component's role
consistent across the dashboard, workout recording, history, profile and dialogs.
A compact navigation item and a screen heading do not need the same font size.

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
  supplies the accent. Custom framed content supplies its tint and background
  explicitly, rather than deriving opacity inside the component.
- Ordinary weights, counts, dates and instructions use neutral text colors.
- Destructive account outlines use `semantic.errorBorder`; quiet summary dividers
  use the shared `ContentDivider`. Disabled switches use their own
  `toggle.disabledTrack` and `toggle.disabledThumb` roles.
- Chart grid/axis/baseline colors and area/sparkline fill endpoints belong to
  their chart color groups. Dynamic series gradients retain the supplied series
  hue and use their chart group's blend/opacity coefficients. Avatar fills/rings
  retain experience hues through the profile coefficients; bottom action fades
  retain their host surface hue through `overlay.bottomActionAlpha`. Shadow spot
  strength uses `overlay.shadowSpotAlphaMultiplier` without changing its hue.
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
- Sparkline extrema use `charts.sparkline.minimumMarker` and `maximumMarker`
  data colors. A minimum or maximum alone does not imply warning or success,
  including in bodyweight history.
- Detailed muscle-distribution rows keep the same calm data hues as the map;
  rank alone carries no success/warning/error meaning. Goal diagnostic stripes
  use their severity accent directly while their explanatory text stays neutral.
- Empty muscle-balance summaries show neutral unavailable information without an
  inferred score or status. The top-two load difference is neutral data, not a
  warning on its own; the existing balance status/ring conveys the assessment.
  Progress bars use linear proportions and outside
  values; pie slices preserve their actual shares.
- Timeline points use `timeline.marker/markerBackground` for a warm chronological
  anchor with a soft halo; connectors stay `divider.default`. The marker does not
  represent selection or workout completion. Keep content and rail geometry stable.
- Step progress uses its own `stepProgressIndicator.fillStart/fillEnd/track` colors:
  a restrained orange-to-copper fill and a readable neutral remaining track in both
  themes. Equal 6 dp capsules with 4 dp gaps keep toolbar geometry stable; progress
  is flow position, separate from selection and semantic success.
- Chips use a quiet card fill, thin neutral outline and their compact 8/12 dp
  shapes. Metric labels use secondary medium-weight text and values use primary
  semibold text; context icons and category markers retain their meaning. Selected
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
  Unselected equipment uses icon.secondary
  rather than disabled styling. Labels wrap in full; Small uses 16 dp horizontal
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

## Shape family

- Product corner radii follow the private 8 / 12 / 16 / 20 / 28 dp scale in AppDp;
  consumers choose their existing semantic component tokens, never a raw radius.
  The size of the boundary and its nesting determine the step.
- 8 dp: badges, small chips, set cells and 32 dp icon/image slots. 12 dp: medium
  chips, Small text buttons, compact filters and 48 dp thumbnails.
- 16 dp: Medium text buttons, inputs, compact metric panels, repeated framed rows,
  focus frames, wheel selections and swipe actions. 20 dp: standalone content,
  menu, profile, recap, equipment, achievement and gallery cards.
- 28 dp is reserved for BrandHeader, the onboarding hero and bottom-sheet top
  corners. Warm card fills alone do not change a card's radius.
- Filled segments use a 16 dp outer shape and a 12 dp selection shape with 4 dp
  inset. Neutral navigation and utility icon buttons use `ButtonStyle.Tertiary`
  with `background.card` and `button.iconRadius` (8 dp), matching menu icon tiles:
  sheet Back/Close, toolbar Back/Profile, date arrows and workout menus.
  Other icon button styles, including the primary Play action, retain circular
  forms alongside avatars, check marks, progress capsules and switch geometry.
  Chart points/arcs retain data geometry;
  chart tooltip surfaces use the compact 8 dp shape.
- Loading card, gallery, image and button silhouettes follow their populated
  owners; text-line skeletons keep their own small corner radius.

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
  Loading name/email slots derive their heights from the same h3/b13Med line
  heights; HeaderLoading tokens retain their width fractions and corner radius.
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
  fields and focus frames use 16 dp corners, including goal insights, diagnostic
  breakdown panels and weight history rows. Chips, set cells and thumbnails retain
  their size-specific shapes. Open sections and journal rows remain unframed.
- Training recording places duration, tonnage and repetitions directly on
  `background.screen`, aligned to `screen.horizontalPadding`. Equal-width cells
  use `h5` primary values and `b12Med` secondary labels with content-driven height;
  `contentPadding.content` supplies the summary's vertical padding and
  `contentPadding.subContent` separates its cells. Save/Update keeps the toolbar's
  primary action; Add Exercise keeps the pinned secondary action.
- Training exercise editing keeps the exercise summary open on `background.screen`.
  Sets are compact open journal rows on the same opaque screen surface, separated
  by `divider.subtle`; they have no card outline or filled value fields. The set
  number sits beside two independent metric actions, using the content-driven
  `iterationCard.editor.numberMinWidth` minimum. Numbers, labels and previous-set
  context use secondary `b12Med`; values use neutral `h5`. Metric targets retain
  `iterationCard.editor.minActionHeight` without a filled or outlined field shape.
  Warnings use `semantic.warning` only for the affected value and icon;
  actionable explanatory text stays neutral without a filled warning panel.
  Save keeps the primary button; Add Set keeps the secondary button.
- Training completion starts with a logo-free Compact BrandHeader confirming a
  saved workout or an update; its owned warm surface carries the emotional accent.
  One neutral Large MetricSectionPanel groups the session's detail action using
  the common 20 dp card shape, card surface and thin outline. Duration/sets and
  tonnage/repetitions form two equal rows: primary `h5` values and secondary
  `b11Med` labels, with complete wrapping values and `divider.subtle` separators.
  Its `h5` section title has a decorative `icon.secondary` detail chevron.
  A secondary `b12Med` caption identifies the optional PerGroup breakdown as
  estimated muscle-load shares; missing muscle data leaves the totals visible.
  Exercises stay open Medium rows on the screen surface, with only their owner's
  vertical padding; `contentPadding.block` separates the header, summary and list.
  Done retains ButtonStyle.Primary and resolved overlay insets in every terminal
  state. Missing saved details use secondary `b14Med` copy; an unconfirmed result
  uses the same quiet copy without a success header.
- MenuSection headings use `h4` and a 22 dp glyph centered in a 32 dp tile with
  8 dp corners. The tile lightly tints the supplied icon hue through the
  theme-owned `icon.tintedBackgroundAlpha` coefficient, preserving caller alpha;
  the glyph retains its supplied tint. Context icon colors keep their meaning.
  The enclosing section owns the grouping surface; Subtle stays unframed with a
  secondary heading. Header gaps use `content`; values stay neutral.
- DigestHeader retains its neutral 32 dp icon tile with 8 dp corners and the
  `context.volume` glyph; working metric panels carry no decorative illustration
  spot. Home's welcome and last-workout recap own the warm illustration regions.
- Training history uses compact Medium exercise rows: a 48 dp Compact image,
  `b14Semi` full wrapping title and `subContent` gaps. Completed/total sets and
  recorded maximum keep their existing summary semantics. Large recording rows
  retain their 64 dp image, `h5` title and `content` gaps. Recording, daily history
  and completion rely on the open row's own `subContent` vertical padding, without
  another list gap or vertical wrapper. Timeline time uses `b14Semi` and duration
  uses secondary `b12Med` text.
- The monthly training digest uses the Small MetricSectionPanel with an inline
  `h4` volume and tertiary `b12Med` label. Its date, total heading and three
  breakdown metrics stay visible; empty values retain their dimmed treatment.
  The history toolbar uses `divider.subtle` to separate controls from records.
- Home uses `MetricCardPresentation.Overview` for goal, muscle, streak and
  performance summaries; goal, streak and performance dialogs retain `Detailed`,
  while muscle details keep the full distribution and PerGroup/PerMuscle selector.
  Overview includes the context needed to interpret the current value: muscle
  distribution and session/set counts, inferred training rhythm and active days,
  and a guarded comparison with previous workouts' average. Full history, best
  values and timeline breakdowns remain available through the existing card action.
  A quiet hint under Progress identifies the summary cards as detail entry points;
  it appears only when at least one of those cards is present.
- Home card headings use `h5` and `text.primary`; section headings retain `h4`,
  while values and secondary metadata keep their distinct roles. Whole-card
  detail entries (goal, muscle, streak, performance and exercise highlights) use
  `MetricOverviewHeader`: one decorative 18 dp `icon.secondary` chevron at the
  right of the heading. Calendar and Update remain their existing explicit buttons.
- Home's initial loading and an active refresh without period content use
  noninteractive skeleton cards with the dashboard's toolbar, gutters and geometry.
  Neutral `divider.default` slots use the shared shimmer after clip/background;
  text slots follow typography line heights. One localized indeterminate loading
  node describes the skeleton; the real period picker remains independently
  accessible for returning users once the range resolves. Known goal/unlock
  availability controls those slots.
  Cached populated content stays visible during refresh; completed empty periods
  retain their real empty presentation. No minimum loading duration is introduced.
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
  Map and bars share colors derived from the full calm muscle palette before
  selecting rows. Percentages are labelled as estimated-load shares; the context
  counts selected-period workouts/sets. A single group names its most-loaded
  individual muscle when that fact exists. Missing groups are not invented zeros.
- The training-rhythm overview has the common heading/chevron and a compact row:
  the existing streak headline and cadence on the left, active-day count on the
  right, with its short localized caption. Both columns wrap naturally; the
  existing thin progress indicator follows below. Full history stays in Details.
- Goal ring percentages are labelled as goal fit, distinct from elapsed days.
  Home pairs the ring with the primary goal/status, analyzed-workout count scoped
  to the selected period and the actual target date. Only sessions eligible for
  goal analysis contribute to that count; it is not the lifetime workout total.
  Overview content determines its own height without filler spacers; the existing
  remaining/overdue message and Update eligibility are preserved.
  Performance labels explain weight/minute and average weight; overview compares
  the latest workout with the prior positive-workout average only when available.
  Numeric averages and their percentage comparison occupy separate lines so both
  remain visible within the metric panel's intrinsic height.
  Duration comparison stays neutral and omits the numeric average truncated to
  whole minutes; its always-Stable chip is omitted in Home.
- Exercise highlights use the Large panel geometry (20 dp corners, 16 dp padding)
  in a horizontal gallery with regular 64 dp thumbnails,
  complete insight text and existing detail actions; a next-card peek signals more
  items without repeating a tall vertical report. Cards share the tallest content's
  natural height. The gallery scroll viewport reaches the screen edges; internal
  start/end gutters align the first and last cards with the other Home sections.
  Insight kind is conveyed by its existing localized semantic chip; the card has
  no colored edge stripe. Its image/title/chevron header shares the card hierarchy.
- MenuValueRow keeps neutral labels/values and a quiet chevron, with a 56 dp
  minimum height and 12 dp vertical padding/gaps. Optional leading icons use a
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
- Detailed performance cards keep the metric name beside its icon and the status
  below the value. Home uses the common title/chevron header; names, units,
  comparison text and statuses wrap naturally. Its paired metric cards share an
  intrinsic-height row, so localized headings can grow without uneven card bottoms.
- Regular exercise thumbnails remain `ExerciseExampleImageStyle.MEDIUM` (64 dp).
  Medium history/read-only exercise rows use `COMPACT` (48 dp); small inline
  exercise references may use `SMALL` (32 dp).
- Exercise-library results use open `ExerciseExampleCardStyle.Large` rows with
  regular 64 dp thumbnails, complete `h5` names, neutral `b13Med` equipment names
  and `b12Med` last-used dates only when present. Missing equipment falls back to
  the declared weight type. Quiet dividers align with the text; repeated taxonomy
  chips, outer card frames and never-used footers do not compete with selection.
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
  `b13Med` label, primary `h4` value and secondary chevron; the following
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
- Goal, streak and performance details use `block` before a new section heading
  and `content` from the heading to its card or between related entries. The first
  visible section has no leading separator. Full-width sheet ranges retain
  `dialog.horizontalPadding`; body content starts once at `dialog.top`.
- Optional descriptions, suggestions, equipment and history leave no separator
  when absent. Present sections own the gap between them; text groups use `text`.
  Equipment galleries use `content` between cards, keeping image and card sizes.
- Training totals stack full-width LONG chips with `subContent` between present
  metrics, preserving complete localized labels and large values in both hosts.
- Scrolling sheet lists own their final `dialog.bottom` through content padding,
  with one navigation inset. A footer-only lazy item must not add an item gap to
  that margin. The actionless library uses `screen.verticalPadding` at its end;
  pinned actions still reserve their measured height through BottomOverlayContainer.
- Editable set rows own their vertical padding and optional divider; the list omits it
  for the final or only row. Their list adds no second row gap; the Sets heading
  keeps `text` bottom padding beside the first row's
  `subContent` top padding. Exercise sheets scroll long content rather than
  compressing text, images, sets or the final action.
- Start workout sheets expose available Custom, Suggested and Repeat choices
  through pinned equal-width Segment controls. All modes fill the same available
  host height so the sheet, choices and action stay in place when content changes.
  Custom centers the existing empty-exercises artwork in IllustratedEmptyState
  with a Large illustration, `h4` title and secondary `b14Med` description.
  Populated modes use a left-aligned `h3` introduction and secondary `b14Med`
  explanation. Repeat places its Small CheckSelectableCard historical dates
  after that introduction, in the same scroll area as the exercises. The date row
  uses the full dialog width with `dialog.horizontalPadding` as internal scroll
  content padding; its label and the plan content retain the same gutter. Plan previews
  lead directly into ExerciseCardStyle.Planned with Compact exercise images, full
  wrapping `b14Semi` names and secondary planned sets/repetitions; they carry no
  completion counters, stale tonnage or detail chevrons.
  The single pinned Primary action names the selected next step, with measured
  BottomOverlayContainer padding rather than a manually reserved overlay height.
  Its pinned action padding, including the single navigation inset, uses an opaque
  dialog surface so scrolled rows cannot show through the action region.
- Draft workout sheets use a left-aligned task heading and short secondary
  explanation. A single Large neutral metrics panel groups exercise count,
  filled/all sets and positive saved duration with the existing `TrainingStat`
  (`h5` values, secondary `b12Med` labels); zero duration is omitted. Empty drafts
  show guidance instead of a zero-value panel. Read-only Medium exercise rows
  stay open with text-aligned dividers between adjacent rows. Introduction,
  summary and exercises share one scroll area. Full-width Primary Continue and
  Secondary Start new stay pinned on an opaque `background.dialog` footer that
  includes the navigation inset, with a secondary consequence caption; Edit
  distinguishes discarded draft edits from the unchanged saved workout.
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
  `IterationCard.Recorded` first: a small passive number badge (`iterationCard.historyNumberSize`),
  neutral `h5` load × repetitions and quiet dividers only between rows. Actual ordinal
  prior values belong to the feature-provided trailing slot, using secondary `b12Med` in a fixed `iterationCard.historyContextWidth`
  column, introduced once by a short previous-date header. Each context slot
  reserves two secondary line heights in loading, populated and absent-match
  states; no repeated Last time label competes with the current result.
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
  or contains unsupported tonnage. Skeletons use `divider.default`, shared clipped
  shimmer and typography-derived text slots; the card exposes one localized
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
  `divider.thickness` (1 dp) in both themes. Vertical metric separators share
  those color and thickness roles. Selection indicators, timeline connectors,
  chart guides and skeleton fills retain their own roles.
- Boundaries between fixed headers or controls and scrolling content, including
  bottom navigation, reach the host edges. The surrounding content owns its
  horizontal gutter; the divider does not add another inset.
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
