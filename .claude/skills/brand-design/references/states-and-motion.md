# States, themes and motion

Use the [usage/state inventory](usage-and-states.md) to establish supported states,
reachable combinations and precedence. This reference owns their visual treatment;
it does not prescribe a universal state machine.

## State treatment

| State/role | Visual responsibility |
|---|---|
| Idle | Clear affordance and quiet containment |
| Pressed | Existing transient interaction feedback |
| Focused | Visible focus through its owning treatment |
| Selected/checked | Reversible warm choice and suitable indicator |
| Disabled | Clear reduced availability without hiding content |
| Loading | Feedback for actual work, retaining meaningful content |
| Invalid/error | Actual validation/failure severity and useful explanation |
| Success/completed | Actual outcome through semantic status |
| Empty/no results | Distinguishable absence/filter result with its available next step |

MUST retain semantic distinctions and real precedence. For reachable
focus/error/disabled combinations, follow the established priority while retaining
required content and affordances. If precedence is unresolved, inspect the real
owner and callers before changing a contract.

## Selection and geometry

The caller owns logical selection, including single/multiple choice. Components
render controlled state; styling does not add a local mirror of feature selection.

Reserve an indicator slot where the design includes one. Prefer fill/outline/check
transitions with stable geometry. With equal content width, selection must not move
labels/images or cause a new wrap merely because the indicator appears. Compact
chips may use their established treatment without inheriting a large card's layout.

## Inputs and feedback

Use the shared input's fill, border, invalid treatment, label and icon roles. Inspect
reachable focus/error/disabled combinations at the drawing/layout owner before
adding an override. Extra screen-local borders, clips or backgrounds cannot stand
in for a diagnosis of the shared component.

Preserve labels, accessibility semantics, button meaning, visibility/clear actions
and keyboard/focus behavior. Error text explains an existing condition; styling does
not broaden validation or hide failure. Status text stays readable on its actual
surface without repeating strong severity across every line and frame.

## Paired themes

Changed color relationships MUST work in both existing themes through the real
[theme owners](../../design-system/references/theme.md), without a screen palette
or a theme branch that replaces token ownership. Review relationships, not merely
the presence of a slot:

- Screen/card/nested surfaces keep grouping clear.
- Primary/supporting/disabled text remains readable on actual fills.
- Boundaries, focus, selection and error remain distinguishable at edges/corners.
- Selected surfaces retain content legibility and choice semantics.
- Gradients, logos and transparent artwork keep readable edges/content without
  washing out labels or exposing a matte box.
- Semantic pairs and data series preserve their meaning and distinguishability.

## Readable content and interaction

MUST preserve hit areas, content descriptions and platform semantics. Meaningful
content cannot depend solely on color, motion or a decorative illustration.
Use content-driven minimums and existing layout owners for long localized labels,
values/units, keyboard and overlays at the
[standard viewport](../../design-system/references/visual-consistency.md#default-viewport-scope).
Do not add small-device or font-threshold layout branches for out-of-scope samples.

[Preview coverage](../../design-system/references/previews.md) owns fixtures;
[visual evidence](../../design-system/references/visual-consistency.md#evidence)
owns what renders establish and how unverified observations are reported. Fix
concrete issues through their existing owners.

## Motion

Motion explains an existing change or adds restrained brand presence. MUST NOT gate
readiness, input, loading or navigation, or add timers, requests or state for
decoration; a product timer (rest, countdown) is feature state owned by its ViewModel.

| Use | Treatment |
|---|---|
| Selection | Existing emphasis animation with stable content geometry |
| Entrance | Restrained presentation that keeps controls immediately available |
| Ambient | Subtle decorative motion confined to a purposeful brand region |
| Loading | Actual loading feedback through the current loader/component |
| Navigation | Feature proceeds when its business conditions allow, independent of decorative completion |

Inspect current specs before choosing timing/easing. Keep technical constants at
the established presentation owner; shared motion tokens require real shared demand.
Use existing Compose/lifecycle ownership and preserve platform motion preferences.
Motion needs runtime evidence, not static previews
([visual evidence](../../design-system/references/visual-consistency.md#evidence)).
