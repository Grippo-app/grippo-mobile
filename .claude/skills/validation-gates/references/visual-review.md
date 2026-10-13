# Visual fidelity review

For a changed Figma-backed screen, dialog, component or overlay, compare the
actual rendered surface with the relevant design node within the user's scope.
The project has no screenshot automation, recording plugins or image baselines.
Do not introduce them without an explicit user request.

## Inputs

- The affected surface and its declared design node/state.
- The design oracle and spec when available.
- An IDE preview or an authorized app screenshot showing that exact state.

Use the design's actual text, values, icons and sections for preview stubs.
Choose theme, locale, layout direction and frame dimensions from the design.
Reuse normal preview containers and design tokens. Do not invent content,
change business behavior or add production instrumentation for a capture.

## Review

Compare content, colors, typography, spacing, component dimensions, insets,
clipping and interaction affordances. Inspect the affected loading, empty, error,
selected and disabled states when relevant. A light preview cannot establish dark
fidelity, and one rendered state cannot prove all variants.

Keep source/spec validation separate from visual observation. A matching source
value does not prove layout or rendering. Report missing inputs and unavailable
runtime checks explicitly; do not claim a machine gate or pixel comparison passed.
Authorized screenshots and human visual review remain valid evidence.

## Output

Record the surface/state, compared design reference, preview/screenshot path,
observations, fixes and remaining unverified states in the existing task record.
A `figma-screenshot-validator` role, when available, reviews these actual inputs;
it does not install a runner or enforce absent automation contracts.
