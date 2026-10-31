# Visual checks and evidence

Use for affected whole-screen consumers, cross-screen audits and every visual
evidence report. Start from brand-design's
[usage/state inventory](../../brand-design/references/usage-and-states.md) and
[acceptance table](../../brand-design/references/workflow-and-review.md#design-acceptance).
[CONSISTENCY.md](../../../../design-system/CONSISTENCY.md) owns concrete product
role-to-token choices. This reference owns the checks and is the single owner of
visual evidence: what a render establishes, what is distinguished and what is reported.

## Default viewport scope

The project targets one standard phone viewport for design acceptance: **412 × 915 dp**
at the normal system font scale.

Inspect [Preview.kt](../../../../design-system/preview/src/commonMain/kotlin/com/grippo/design.preview/Preview.kt)
and [ResourceProvider.kt](../../../../design-system/core/src/commonMain/kotlin/com/grippo/design.core/internal/ResourceProvider.kt);
current shape:

- `@AppPreview` declares exactly two previews: "Phone • UK • Standard • Light"
  (412 × 915 dp, `locale = "uk"`, light) and "Phone • EN • Standard • Dark"
  (412 × 915 dp, `locale = "en"`, dark).
- Provider and locale-tag semantics follow [theme ownership](theme.md); container
  framing, image substitutions and their evidence limits follow
  [what a preview establishes](previews.md#what-a-preview-establishes).

Default visual acceptance is therefore: both AppPreview variants (uk-light and
en-dark) rendered when a render is available, plus source review of the `values-ru`
strings for length parity. No preview matrix for ru, uk-dark or en-light exists, and
none is added unless the user asks.

Very small devices and narrow-screen layouts are outside the default design scope.
MUST NOT add special layouts, shrink content, spend a review loop or block completion
because of their appearance. Do not expand the default checks into a device-size or
font-scale stress matrix; additional sizes/scales require an explicit user request,
and existing supplemental fixtures do not make those checks mandatory. Keyboard,
insets, scrolling, control semantics and real feature states still matter at the
standard size.

## Inspection scope

- Source-review every affected consumer/context. Render whole screens for the
  [significant states](../../../../CLAUDE.md#definitions) of every distinct
  inventoried role/layout/state group; call sites grouped under the inventory's
  rule share one render.
- Include neighboring headers/cards, backgrounds, toolbar/bottom-bar and pinned
  actions in both themes. Check hierarchy, semantic meaning, grouping,
  corners/padding and actual imagery at its intended size.
- When text layout changes, use the standard viewport above: the uk-light variant
  renders a longer-label locale; review the ru strings in source. Use existing
  layout owners/tokens and resolve pressure through wrapping/growing/rearrangement,
  not hidden useful text or shrunken regular imagery.
- Inspect the inventory's content boundaries, including missing images and
  zero/partial values where supported. Exercise transitions, focus/keyboard and
  gestures separately from static captures.
- Inspect scrolled content: final items/actions remain reachable above overlays;
  localized labels, toolbar actions, dates and tags do not overlap or clip.

## Evidence

What counts as visual evidence, how it is distinguished and how it is reported.

- Reconcile the final source against the
  [usage/state inventory](../../brand-design/references/usage-and-states.md#source-gate).
  Use [preview coverage](previews.md) for fixtures and the inspection scope above
  for whole screens; an isolated component render cannot verify its host.
- For a supplied design, inspect the authoritative node/artifact through the
  available supported tools. Name the exact content/state compared, preserve
  declared variant axes and leave missing design facts explicit.
- Use existing previews or authorized app screenshots. For endpoint/layout captures,
  wait for layout to settle and inspect actual title/content, scale and system overlays.
  A successful capture command is not evidence of a current frame; reacquire stale
  transition captures and avoid competing resource-heavy work that invalidates them.
  Restore device locale, font scale, viewport and theme changed for checks.
- For changed disclosures, exercise expansion, collapse and rapid reversal in the
  actual host, plus collapse after scrolling and dismissal/reopening when those
  actions are supported by the inventoried host. Motion claims need
  a real recording or intermediate captures showing content and container size
  through the transition; settled endpoints and animation API names do not prove
  smoothness. Reacquire sparse/stale captures before drawing a motion conclusion,
  and report unavailable states or recording limits without inferring a pass or
  a UI defect from missing frames.
- Note observations for the final revision, fix material mismatches and
  re-inspect affected groups after corrections. Distinguish source review, static
  renders (component render versus whole-screen render, which variants, which
  platform), runtime interaction and compilation. A token scan or green build does
  not establish visual fidelity; preview image placeholders do not verify loaded
  imagery (see [what a preview establishes](previews.md#what-a-preview-establishes)).
- Name unrendered/unexercised groups, their unavailable inputs and any unresolved
  design decision. A missing observation is reported as an evidence limit, never
  as a passed check or as proof of a defect.

Where evidence goes ([records](../../../../CLAUDE.md#records)): the compact visual
brief, the inventory and the evidence list live in the reply (the task report);
a changed role-to-token choice updates
[CONSISTENCY.md](../../../../design-system/CONSISTENCY.md); a dated visual-pass
record in `docs/records/<yyyy-mm-dd>-<slug>.md` is written only when the user asks.
Builds follow the default build rule in the
[verification policy](../../../../CLAUDE.md#verification-policy); renders on a
device go through [runtime-verification](../../runtime-verification/SKILL.md)
when the user allows running the app.
