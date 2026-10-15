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

## Color and state

- Generic selection uses the shared selection.accent/background pair: check cards,
  toggle cards, check marks, filled segments and highlighted navigation. Checked
  switches use the same warm palette. Success/error/info and chart series keep
  their semantic colors; selection does not imply a completed workout.
- Severity backgrounds come from semantic.*Background in both concrete themes.
  Informational bubbles and badges keep neutral text where the status icon already
  supplies the accent. Custom framed content supplies its tint and background
  explicitly, rather than deriving opacity inside the component.
- Ordinary weights, counts, dates and instructions use neutral text colors.
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
  suggestion chips use selection.background and selection.accent for fill/outline
  with neutral text. Keep selection and click handling at the existing caller.
- Completed exercise progress uses `semantic.success`: `3/3 sets`. Pending or
  partial progress stays `text.secondary`. Do not add a redundant completion badge.
- Do not display a maximum weight until at least one set has been recorded.
- Keep state accents concentrated in the status, marker or stripe. Avoid repeating
  the accent across every text line in an insight card.
- CheckSelectableCard uses a warm selected surface and outline across all sizes.
  Large and Medium show a trailing circular marker in a reserved slot, filled
  with selection.accent and a selection.onAccent check when selected. Large places
  its description below the icon/title row to retain reading width. Labels stay
  neutral across all sizes. Selection animates colors without moving content. Logical
  selection stays with the caller, including single-choice filters and multi-choice chips.
- Check is the shared circular selection mark. Standalone checkboxes own their
  touch target and accessibility; cards use Check without a callback and retain
  their own interaction. Enabled unchecked marks use icon.secondary for a readable
  neutral outline; disabled marks use icon.disabled. Toggle remains the switch control.
- ToggleSelectableCard keeps neutral labels, a warm selected surface/outline and
  a permanently reserved switch slot. Unselected equipment uses icon.secondary
  rather than disabled styling. Labels wrap in full; Small uses 16 dp horizontal
  and 12 dp vertical padding, while Medium retains its 64 dp image and 12 dp padding.
- Use `ButtonStyle.Primary` for starting or resuming a workout. Reserve
  `ButtonStyle.Error` for destructive actions and explicit error acknowledgement.
- Primary buttons use a restrained orange-to-copper fill from the theme's own
  accent, with light content in the light theme and graphite content in the dark
  theme, the same pairing as checked controls. Selection, focus, navigation and
  step progress share that accent, so each theme has one warm orange.
- Secondary buttons use a neutral filled surface and thin outline;
  Tertiary and Transparent retain their quieter surface and inline roles.
- Text buttons use the 12 dp control shape; icon-only buttons remain circular.
  All styles retain their existing sizes, single-line labels and loading geometry.
- Error buttons keep the semantic red fill with a dedicated button content pair:
  light content in the light theme and graphite content in the dark theme.
- Chart series, muscle maps, validation and diagnostic severity retain meaningful
  colors. Use theme-aware palettes in previews as well as production.

## Layout and accessibility

- Profile Menu uses a compact warm identity region through UserProfileHeader:
  illustration.backgroundStart/backgroundEnd on the existing user-card shape,
  h3 name and neutral supporting email. Experience avatar colors retain their
  meaning; the menu's working sections stay neutral. Separate identity from menu
  content with contentPadding.block, keeping compact section gaps at content.
- Standalone content cards and menu sections use the large 28 dp shape and a thin
  neutral outline. A standalone card is a card that is its own section; rows
  repeated inside a list are compact, not standalone. Nested/compact metric
  panels, repeated list rows and input fields use the 12 dp shape; chips, set
  cells and thumbnails keep the shape appropriate to their role.
  Do not put a second card frame around an already framed shared composition.
- MenuSection headings use `h4` and a 32 dp neutral icon container with a 12 dp
  shape and 22 dp glyph. Context icon colors retain their meaning. Default uses
  the screen tone inside its card; Subtle stays unframed with a card-tone icon
  container and secondary heading. Header gaps use `content`; values stay neutral.
- The digest header on Home uses the same neutral 32 dp icon tile with the
  `context.volume` glyph; working metric panels carry no decorative illustration
  spot, which stays with the welcome/hero region.
- MenuValueRow keeps neutral labels/values and a quiet chevron, with a 56 dp
  minimum height and 12 dp vertical padding/gaps. Optional leading icons use a
  32 dp card-surface tile with 12 dp corners; caller-supplied channel/destructive
  colors retain their meaning. Dividers align with the text after the tile.
- Equipment rows retain a 64 dp image slot with 12 dp surface padding on all sides.
  Highlighted controls reserve their indicator space so selection does not shift text.
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
- Checked controls use the matching foreground for their fill. The dark switch
  uses a graphite thumb on the bright orange track; check marks use `selection.onAccent`.
- Tabs announce selection, equipment/muscle switches announce their labels and
  checklist rows announce completion independently of decorative icons/colors.
- Performance cards keep the metric name beside its icon and the status below the
  value. Names, units, comparison text and statuses wrap within the existing grid.
- Regular exercise thumbnails remain `ExerciseExampleImageStyle.MEDIUM` (64 dp).
  Compact inline exercise references may use the existing small variant.
- Exercise image slots use `background.card`, a thin `border.default` boundary
  and their existing compact radii. Loading/missing/error placeholders use
  `icon.secondary` with size-specific padding, keeping the 32 dp slot readable
  in both themes. Loaded images retain their colors and crop; image slots do not
  carry selection or status emphasis.
- Use `contentPadding.block` between sections, `content` between related controls,
  `subContent` between compact items and `text` within a text group. Compact menu
  sections use `content` spacing.
- Keep control heights as minimums when content depends on font size. Chips, set
  cells, selectors and segmented labels must grow instead of clipping text.
- Keep one layout across system font scales: single-line button labels, centered
  toolbar titles within the action slots, and the regular dashboard/card grids.
- Keep indicator date labels in separate bounded slots and allow them to wrap.
- Let tags wrap. Do not hide a weight type just because the preceding tags are long.
- Preserve readable content when lists scroll behind a pinned action. Pass the
  bottom overlay's resolved padding into the list.

Before changing shared components, inspect their variants and whole-screen
consumers in both themes. Use the [standard viewport scope](../.claude/skills/design-system/references/visual-consistency.md#default-viewport-scope)
and long localized text; very small devices are outside default visual acceptance.
Compilation alone does not verify visual composition.
