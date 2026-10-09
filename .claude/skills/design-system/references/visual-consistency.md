# Visual consistency and whole-screen review

Use this reference for shared-card/layout changes and explicit cross-screen
consistency audits. Scale the review to the affected consumers; a small change
does not require capturing the entire application.

## Product rules

Read the project’s maintained [visual usage rules](../../../../design-system/CONSISTENCY.md)
for the role-to-token table, exercise summary semantics and adaptive layouts.
Keep those concrete product choices in that document rather than duplicating
another table here. Explicit user design changes take precedence.

Compare by role: screen headings, section headings, regular card titles,
compact items, primary metrics and supporting labels. Consistency does not
require giving every role the same size or eliminating meaningful chart colors.
Use neutral ordinary values and metadata; reserve accents for actions, status,
validation and data series. Check the complete screen for competing accents.

Keep readable product imagery at its intended size. Resolve text pressure with
wrapping, growing minimum heights or changing the arrangement. Do not silently
shrink exercise thumbnails to make a title fit.

## Verification scope

- Find shared-component consumers and inspect representative whole screens,
  including neighboring cards, toolbars and pinned actions in both themes.
- Include a 360 dp viewport, 150–200% font scale and a supported locale with
  longer labels when the change affects text layout. Use existing tokens for
  adaptation thresholds and dimensions.
- Exercise empty, pending, partial and completed states where relevant. Check
  long titles, missing images and zero values; use deterministic fixtures.
- Check scrolled content too: the final item must remain reachable above the
  pinned action. Check title/action overlap, separate date slots, tag wrapping
  and button labels at enlarged fonts.
- Verify semantic color consistency between a component and its screen header.
  A token-only static scan cannot prove hierarchy, contrast or readable layout.

## Reliable capture and cleanup

Use the repository’s screenshot gates when the task requires them. Additional
native captures can verify composition but do not replace a required Figma gate.
Wait for the requested fixture and its layout to settle. Verify the image’s
actual title/content and absence of system overlays; a ready log or successful
capture command alone does not prove that the frame is current. Cold-launch
and recapture stale transition frames. Avoid resource-heavy builds during
emulator captures when they cause timing failures or system dialogs.

For temporary harnesses, preserve the original entry point, keep hooks local,
and remove them before the production build. Restore device locale, font scale,
viewport and theme after checks; retain requested preview variants. Record the
fixture/source mapping and distinguish component captures, whole-screen
captures, live-data checks and platforms actually inspected. Do not claim iOS
visual coverage from compilation or complete runtime coverage from previews.

Before delivery, check removed-component callers, imports, locale placeholders,
callback-bearing stability annotations, and the final diff. Confirm the clean
Android/iOS build for the project’s supported targets. Report specific evidence
and remaining limits rather than asserting absolute certainty.
