# Composition by surface

Read only sections matching the task. These patterns guide hierarchy, not new APIs
or mandatory wrappers. Use current
[shared owners](../../design-system/references/components.md) and
[product usage rules](../../../../design-system/CONSISTENCY.md).
Choose containment through [shape and hierarchy](design-language.md#shape-and-hierarchy).

## Choose intensity by purpose

| Surface | Expression | What leads |
|---|---|---|
| Splash/onboarding | Strong first impression | Identity, explanation and actual next step |
| Authentication/setup | Bounded warm introduction and quiet working area | Fields/choices and submit/continue |
| Home/summary | Warm welcome and scannable neutral content | Useful action and actual information |
| Training/history/list | Restrained brand through shape, rhythm and controls | Records, state and interaction |
| Empty content | Relevant artwork when useful, concise guidance | Actual absence and available next step |
| Error/confirmation | Compact focused interruption | Message, consequence and action |
| Chart/data detail | Quiet hierarchy and meaningful series/status colors | Comparison, units and interpretation |

Intensity follows role/density; a logo, hero or illustration is not required on
every screen, row or dialog.

## Splash and onboarding

Use the current BrandSplash owner for identity, ambient depth and loader placement.
Preparation/navigation remain with the feature; follow
[motion invariants](states-and-motion.md#motion) rather than extending the splash
for decoration. Inspect first-frame and destination timing when runtime access allows.

Onboarding explains existing benefits with a clear hierarchy and a distinct primary
action. Supporting blocks earn their space; repeated cards must not obscure the
introduction or action. Avoid another logo/title/hero layer when identity is clear.
Consent/settings remain readable controls in their agreed location; preserve their
actual defaults, persistence timing and behavior.

## Authentication and forms

Use a bounded branded introduction where useful, then align a quiet working area.
Reuse BrandHeader when its contract fits; its optional logo is not required per step.

- Group fields through spacing and common edges. Inputs own their surface/shape;
  avoid an enclosing second form-card frame by default.
- Keep title, explanation, fields and submit in one reading path. Existing styles
  distinguish the main action from providers and secondary links; preserve marks.
- Apply [input/state treatment](states-and-motion.md#inputs-and-feedback). Preserve
  field order, labels, keyboard/focus traversal, validation timing, visibility/clear
  actions, enabled conditions and operation loading.
- For multi-step setup, retain shell/header/action alignment while allowing each
  task its needed density. Reuse actual MuscleSelection/EquipmentSelection owners
  rather than duplicating grouping and state rendering in each screen.

## Home, dashboards and summaries

The welcome region introduces the surface; actual data and the useful next action
remain prominent. Keep content edges and role-based headings coherent. Reduce
repeated title/value/status emphasis.

Preserve established metric hierarchy, grids and ordering unless composition
changes are authorized. Use actual empty, partial and populated states, not richer
invented data to claim production hierarchy is sound.

## Lists, choices and equipment

Content and interaction lead; reuse common row/card/selection owners.

- Follow [selection geometry](states-and-motion.md#selection-and-geometry) and the
  real single/multiple-choice contract. Preserve useful secondary text, units,
  exclusions, counts and summaries.
- Keep equipment/exercise images readable at their intended size with padding around
  visible bounds. Inspect wide and tall subjects as well as convenient square images.
- Long content may wrap or use a fitting existing arrangement; it must not disappear
  merely for a cleaner sample. Preserve list spacing, swipe actions, scroll ownership
  and final-item clearance above pinned actions.

## Empty, loading and failure

MUST distinguish content absence, filtering/no results, loading, failure and missing
prerequisites through their actual meanings/actions. A generic empty illustration
must not replace retry or failure feedback.

Use IllustratedEmptyState for a fitting empty-list composition; use Illustration
alone when the parent owns text/actions/layout. Inspect existing scrolling/padding
before nesting compositions. Reuse artwork whose subject fits; artwork creation or
style review follows illustrations. Use existing loader/skeleton owners for real work.

## Dialogs and sheets

Keep the interruption proportionate to the task. Lead with consequence and actions;
use suitable existing artwork only where helpful. The host owns title, background
and shape, avoiding a duplicate toolbar or full-screen decorative hero.

- Preserve existing actions and their meaning/order, Back/dismiss behavior,
  acknowledgement/retry timing and result delivery.
- Use the correct button severity. Visual emphasis cannot make an ordinary
  confirmation destructive.
- An inline edit block inside a sheet (a field with Save/Cancel) keeps its own
  actions next to the field, above any pinned bottom action of the sheet; the
  pinned action keeps its meaning and is disabled or hidden only when the edit
  makes it invalid.
- Use actual content and the existing scroll/inset owners to keep fields, final items
  and actions reachable with keyboard/overlays at the standard viewport. Add scrolling for a demonstrated
  content constraint rather than a speculative second layout system.

## Data, progress and achievement

Charts, anatomy, sets and metrics communicate facts before brand. Keep scales, units,
ranges, legends and status distinctions under the
[color semantics](design-language.md#color-meaning). Pending/partial data must not
look completed; decorative progress artwork must not look like live measured data.

A real achievement can carry stronger emotion through suitable emphasis/artwork.
Keep the result and next action clear. An ordinary selection is not an achievement;
visual work does not add rewards, celebratory delays or another completion screen.

## Navigation and persistent actions

Keep toolbar/action slots and bottom navigation coherent with content. Active
navigation uses the shared choice family; inactive controls remain legible and
geometry stays stable. Pinned-action padding belongs to the existing container and
must reach the scrolling content. Routes, Back behavior and feature directions
remain with ui-feature and the authorized task.
