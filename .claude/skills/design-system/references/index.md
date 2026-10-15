# Design-system references

Read only the affected topics. Linked producers own current APIs; these references
own implementation constraints. Apply the completion gate in [SKILL.md](../SKILL.md).

## Implementation

| Topic | Reference |
|---|---|
| AppTokens facade, token/category ownership and non-composable access | [Tokens](tokens.md) |
| Color roles and concrete theme contracts | [Color](color.md) |
| Dimensions, scales and adaptation ownership | [Dimensions](dp.md) |
| Text styles and font registration | [Typography](typography.md) |
| String, drawable, icon and StringProvider APIs | [Accessors](accessors.md) |
| Theme/CompositionLocals and effective locale behavior | [Theme](theme.md) |
| Shared widgets, contracts, state/interaction ownership and placement | [Components](components.md) |
| Compose resources, packaging, localization and asset conventions | [Resources](resources.md) |
| Adding a string/plural, drawable, icon or font | [Addition recipe](cookbook-resource.md) |
| Image loading and placeholders | [Images](image-loader.md) |
| Module responsibilities and dependency boundaries | [Modules](design-system-modules.md) |
| Preview helpers, fixtures and coverage | [Previews](previews.md) |
| Whole-screen checks and capture handling | [Visual consistency](visual-consistency.md) |

## Canonical rules outside this skill

| Topic | Owner |
|---|---|
| Visual intent/composition and mandatory usage/state analysis | [brand-design](../../brand-design/SKILL.md) |
| Product artwork style, generation and asset acceptance | [illustrations](../../illustrations/SKILL.md) |
| Kotlin style, naming, visibility and existing package conventions | [CLAUDE.md](../../../../CLAUDE.md#code-style-and-naming) |
| Compose signatures, stability, state models and side effects | [Compose rules](../../ui-feature/references/compose-rules.md) |
| Feature extraction and design-system versus compose-libs | [Module ownership](../../ui-feature/references/module-structure.md) |
| Cross-cut forbidden patterns | [Validation rules](../../validation-gates/references/forbidden-patterns.md) |
| Scope boundaries and unresolved decisions | [Scope policy](../../../../CLAUDE.md#scope-discipline), [questions](../../validation-gates/references/when-to-stop-and-ask.md) |
