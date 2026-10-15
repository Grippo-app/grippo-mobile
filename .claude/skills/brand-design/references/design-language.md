# Grippo visual language

Use for new composition, redesign and brand review. This reference owns visual
intent; exact tokens and product role choices stay in
[CONSISTENCY.md](../../../../design-system/CONSISTENCY.md) and current producers.
The user's accepted design may override defaults; state departures in the brief.

## Tone

Grippo translates well-made training equipment into software: substantial,
precise, approachable and useful in repeated use. Neutral structure keeps training
information readable; warmth gives focus and momentum.

| Quality | Visible result | Failure signal |
|---|---|---|
| Calm confidence | Clear hierarchy and restrained emphasis | Every block competes through scale, color or effects |
| Tactile softness | Related rounded forms, controlled layering, readable silhouettes | Arbitrary corners, nested heavy frames or plastic effects |
| Warm energy | Orange/copper at actions, selection and purposeful imagery | Accents compete across values, labels, icons and surfaces |
| Useful precision | Honest content, units, status and predictable controls | Decoration obscures state or implies unsupported outcomes |

Visual expression MUST NOT invent coaching, rewards, measurements or capabilities.

## Relationship to artwork

The [approved onboarding artwork](../../../../design-system/resources/provider/src/commonMain/composeResources/drawable/onboarding_hero.webp)
combines graphite mass, orange/copper edges, soft volume and warm light. Translate
that family into neutral surfaces, deliberate corners and a warm focal point;
it does not require metal textures, glow or 3D treatment on every control.

Artwork creation/style review follows [illustrations](../../illustrations/SKILL.md),
including visual inspection of its reference. UI layout alone does not require
art generation or export. Reuse current
[composition owners](../../design-system/references/components.md) for splash,
headers, selection and illustrated empty states. Inspect them as distinct roles,
not templates to paste wholesale. Whole-screen anchors include
[WelcomeHomeContent](../../../../ui-screen-features/home/src/commonMain/kotlin/com/grippo/home/home/components/WelcomeHomeContent.kt);
its private pieces remain examples rather than shared APIs.

## Color meaning

Choose the role before the hue. Inspect current
[semantic producers](../../design-system/references/color.md) and their consumers.

| Role | Intent |
|---|---|
| Screen/card surface | Quiet grouping and readable layering |
| Ordinary content | Primary information, supporting text and metadata |
| Main action | Clear next step through an existing button style |
| Selection | Reversible choice or active navigation |
| Semantic feedback | Actual success, error, warning or other declared severity |
| Illustration region | Purposeful warm introduction or decorative depth |
| Data/context | Distinguishable series, anatomy, equipment and contextual meaning |

MUST preserve these meanings. Selection is not completion; branding cannot recolor
success, errors, destructive actions, warnings, muscle maps or chart series into
one generic orange treatment. Do not infer a role from a group's name alone:
`AppColor.brand` is a numbered categorical accent set (`color1`, `color2`, `color4`,
`color5`, `color6`, mapped to magenta/coral/teal/sky/indigo in both themes), not the
orange brand accent; that accent lives in `selection.accent`,
`button.backgroundPrimary1`, `border.focus` and `timeline.marker`, and exercise
category colors in `example.category.compound/isolation`
([color tokens](../../design-system/references/color.md)).

Keep ordinary weights, repetitions, dates and descriptions neutral unless their
meaning warrants emphasis. Use owned semantic surface pairs; do not derive a
screen palette or state opacity from illustration pixels or local blends.

## Shape and hierarchy

Enclosing surfaces gather content; controls communicate interaction. Their forms
should share a family while expressing different roles. Standalone cards can be
softer than compact panels, fields, chips and image slots.

Prefer grouping through space and surface tone. Add a boundary where it clarifies
containment/state. Avoid multiple heavy frames around already grouped content or
outline + strong shadow + gradient on an ordinary card. Internal padding must leave
room inside curved corners and preserve intended image scale.

Use role-based typography: a leading heading states the task; sections, card titles,
metrics and metadata remain distinct. A large available style does not establish a
metric's importance. Space sections more strongly than related content and align
headers, fields, lists and actions to the intended content edges. Centered layouts
fit introductions; repeated operational content benefits from a stable reading edge.

Resolve layout pressure by removing redundant emphasis, wrapping/growing content
or improving arrangement. Do not shrink useful imagery or all text to fit one
sample. Avoid forced line breaks that balance only one locale.

## Depth, imagery and copy

Concentrate gradients/depth in purposeful brand regions. Keep ordinary working
content quiet. Review artwork at its actual display size and theme surface: complete
silhouette/shadow, readable surrounding copy, correct proportions and useful margins.
Do not stretch/crop meaningful content or add an opaque frame around a cutout merely
for decoration. Preserve provider marks, logos and equipment identity.

Headings explain the actual task; buttons name actual actions; validation explains
actual conditions. Descriptions support the next step without slogans, body judgments
or unsupported claims. Preserve approved copy/meaning unless copy edits are in scope;
new copy follows the localization owner.
