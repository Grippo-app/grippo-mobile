# Visual consistency

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

- Ordinary weights, counts, dates and instructions use neutral text colors.
- Completed exercise progress uses `semantic.success`: `3/3 sets`. Pending or
  partial progress stays `text.secondary`. Do not add a redundant completion badge.
- Do not display a maximum weight until at least one set has been recorded.
- Keep state accents concentrated in the status, marker or stripe. Avoid repeating
  the accent across every text line in an insight card.
- Use `ButtonStyle.Primary` for starting or resuming a workout. Reserve
  `ButtonStyle.Error` for destructive actions and explicit error acknowledgement.
- Chart series, muscle maps, validation and diagnostic severity retain meaningful
  colors. Use theme-aware palettes in previews as well as production.

## Layout and accessibility

- Regular exercise thumbnails remain `ExerciseExampleImageStyle.MEDIUM` (64 dp).
  Compact inline exercise references may use the existing small variant.
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
consumers in both themes. Include supported phone sizes and long localized text.
Compilation alone does not verify visual composition.
