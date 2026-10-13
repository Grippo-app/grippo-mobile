# Kotlin style for UI and resource work

Follow [CLAUDE.md](../../../../CLAUDE.md#kotlin) and actual neighboring source.
Keep this reference scoped to UI/resource implementation; domain/network behavior
belongs to its owning skill rather than a generic Kotlin tutorial.

- Public APIs have explicit visibility; implementation types stay internal.
  Keep imports specific and remove unused ones in changed files.
- Business/control blocks, safeLaunch and trackOperation are multiline. Short
  single-expression update and simple Flow transformations may remain compact.
  Named local function declarations are forbidden; class/file methods and ordinary
  callback/DSL lambdas remain allowed.
- ViewModels use safeLaunch/Flow.safeLaunch and the shared error pipeline. Their
  suspend helpers preserve original errors, cancellation and fatal Errors. No
  local recovery handlers. Explicit cancellation checks follow the
  [canonical coroutine rules](../../validation-gates/references/forbidden-patterns.md#coroutines). Outside the ViewModel,
  preserve the existing Result/throwing boundary; bare suspend runCatching that
  captures cancellation/fatal Errors is not an approved recipe.
- State uses val and the project's immutable collections. Kotlin read-only
  List/Set/Map alone do not guarantee immutable contents. Do not use an annotation
  to hide mutation or copy business eligibility logic into a Composable.
- Resources/tokens follow their producer ownership. Consumer fallbacks must have
  defined semantics; required DTO values are not replaced with dummy zero/empty
  values to make a mapper or preview succeed.
- Compose experimentals are opted in by conventions. Do not add per-file opt-ins
  already covered globally or silence an unrelated warning during UI work.
- Prefer clear code to narration. Keep comments only for non-obvious invariants;
  do not embed task/quiz IDs or temporary implementation history.

Review the actual changed signatures, dependencies and callers. Use the owning
[UI error pipeline](../../ui-feature/references/error-pipeline.md),
[data error rules](../../data-layer/references/error-pipeline.md) and
[mapper constraints](../../mappers/SKILL.md) when those boundaries are affected.
