# Whole-screen visual checks

Use for affected whole-screen consumers and cross-screen audits. Start from
brand-design's [usage/state inventory](../../brand-design/references/usage-and-states.md)
and [acceptance gate](../../brand-design/references/workflow-and-review.md#design-acceptance).
[CONSISTENCY.md](../../../../design-system/CONSISTENCY.md) owns concrete product
role-to-token choices; this reference owns the checks.

## Default viewport scope

The project targets one standard phone viewport for design acceptance: **412 × 915 dp**
at the normal system font scale. Review both themes and actual supported locales
at that size through the existing AppPreview owner.

Very small devices and narrow-screen layouts are outside the default design scope.
MUST NOT add special layouts, shrink content, spend a review loop or block completion
because of their appearance. Do not expand the default checks into a device-size or
font-scale stress matrix. Additional sizes/scales require an explicit user request;
existing supplemental fixtures do not make those checks mandatory. Keyboard, insets,
scrolling, control semantics and real feature states still matter at the standard size.

## Inspection scope

- Source-review every affected consumer/context. Inspect representative whole
  screens for every distinct inventoried role/layout/state group; equivalent
  occurrences may share renders after inspection.
- Include neighboring headers/cards, backgrounds, toolbar/bottom-bar and pinned
  actions in both existing themes. Check hierarchy, semantic meaning, grouping,
  corners/padding and actual imagery at its intended size.
- When text layout changes, use the standard viewport above and a supported locale
  with longer labels. Use existing layout owners/tokens.
  Resolve pressure through wrapping/growing/rearrangement, not hidden useful text
  or shrunken regular imagery.
- Inspect applicable states, meaningful combinations and content boundaries from
  the inventory, including missing images and zero/partial values where supported.
  Exercise transitions, focus/keyboard and gestures separately from static captures.
- Inspect scrolled content: final items/actions remain reachable above overlays;
  localized labels, toolbar actions, dates and tags do not overlap or clip.

## Capture and evidence

Use existing previews or authorized app screenshots. Capture the intended state
after layout settles and inspect actual title/content, scale and system overlays.
A successful capture command is not evidence of a current frame. Reacquire stale
transition captures; avoid competing resource-heavy work when it invalidates them.

Restore device locale, font scale, viewport and theme changed for checks. Record
source/fixture mapping and distinguish component renders, whole-screen renders,
live-data checks and platforms actually inspected. Preview image placeholders do
not verify loaded imagery; see [preview limits](previews.md#what-a-preview-establishes).

Review the final diff and recheck affected groups after corrections. Report missing
renders/runtime observations specifically. Compilation establishes neither visual
quality nor iOS runtime coverage. Verification facilities and builds follow
[project policy](../../../../CLAUDE.md#verification-policy).
