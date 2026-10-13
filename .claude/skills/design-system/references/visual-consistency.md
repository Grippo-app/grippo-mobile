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
  long titles, missing images and zero values; use representative preview state.
- Check scrolled content too: the final item must remain reachable above the
  pinned action. Check title/action overlap, separate date slots, tag wrapping
  and button labels at enlarged fonts.
- Verify semantic color consistency between a component and its screen header.
  A token-only static scan cannot prove hierarchy, contrast or readable layout.

## Reliable capture and cleanup

Use permitted rendered previews or authorized app screenshots and compare the
actual content with the design when relevant. Wait for the intended state and
layout to settle. Verify the image’s
actual title/content and absence of system overlays; a ready log or successful
capture command alone does not prove that the frame is current. Cold-launch
and recapture stale transition frames. Avoid resource-heavy builds during
emulator captures when they cause timing failures or system dialogs.

Do not add capture harnesses, production hooks or recording dependencies. Restore device locale, font scale,
viewport and theme after checks; retain requested preview variants. Record the
preview/source mapping and distinguish component captures, whole-screen
captures, live-data checks and platforms actually inspected. Do not claim iOS
visual coverage from compilation or complete runtime coverage from previews.

Before delivery, check removed-component callers, imports, locale placeholders,
callback-bearing stability annotations, and the final diff. Run platform builds only when permitted by the user’s scope. Report specific evidence
and remaining limits rather than asserting absolute certainty.
