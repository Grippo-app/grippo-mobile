---
name: compose-components
description: Implement, refactor and review Grippo Compose components and UI for readable structure, consistent APIs, correct state/effect lifetimes, accessibility and justified performance. Apply to shared widgets, feature compositions and Compose helpers; feature architecture, visual intent and token contracts retain their existing owners.
---

# Compose components

Make each component understandable from its inputs to its rendered output, with
predictable local structure and changes at the smallest real owner.
Apply this skill when writing, refactoring or reviewing Compose UI or its helpers.

## Owners to apply together

- [Kotlin style](../validation-gates/references/code-style.md) owns naming,
  visibility, files and comments.
- [Design-system](../design-system/SKILL.md) owns component contracts, semantic
  tokens, resources, previews and visual evidence; [brand-design](../brand-design/SKILL.md)
  owns visual intent, composition and acceptance. Read their gates for visual work.
- [UI feature composition](../ui-feature/references/compose-rules.md) owns Screen,
  MVI, navigation and keyboard integration; [UI state](../ui-feature/references/state.md)
  owns feature models. Behavior changes apply [analytics](../feature-analytics/SKILL.md)
  and [preservation](../state-preservation/SKILL.md).

## Component implementation

Read [structure and runtime rules](references/structure-and-runtime.md) before
authoring or reviewing Compose code. It owns shared signatures, decomposition,
modifier use, stability promises, memoization, effect lifetimes, hot paths and
interaction semantics. Keep local helpers proportional to a real responsibility;
a consistent structure does not require a separate function for every visual block.

For every changed component, inspect its declaration, private implementation and
all affected production callers. Trace inputs, visual variants, state ownership,
callbacks and enclosing layout. Classify each proposed change: readability,
runtime correctness, measured/source-demonstrated performance issue, or visual
change. Preserve observable behavior unless the task authorizes changing it.

Use the [token owners](../design-system/references/tokens.md) and
[dimension rules](../design-system/references/dp.md) to resolve product values by
meaning. Technical constants and preview data need role assessment, not automatic
tokenization. Apply [component placement/contracts](../design-system/references/components.md)
when extracting or changing public APIs.

## Full component audits

When asked to review a component directory exhaustively, enumerate every production
source file, including private helpers, models, modifiers, utilities and platform
actuals. Read each file and its affected callers individually. Track one decision
per file in the task conversation: keep with reason, fix with reason, or blocked
with the missing evidence. A healthy file needs no edit; a bulk text substitution
or keyword scan does not establish that a component was reviewed.

Implement coherent slices with the owning token/caller migrations. Self-review
the resulting source and diff per file, then use the canonical
[independent review loop](../task-orchestrator/SKILL.md#execution-loop). Reconcile
the final inventory so that no file disappears between review and delivery.
Durable reports follow the project's [records policy](../../../CLAUDE.md#records).

## Completion gate

- Signature, body and helper boundaries satisfy the reference; departures have
  a concrete contract or readability reason.
- State/identity, remember keys, effect keys/cleanup and latest callbacks were
  traced through changing inputs; annotations state true contracts.
- Hot gesture/draw/layout work and lazy identity were inspected; performance
  claims name the actual repeated work or observed evidence.
- Interaction, focus, selection and accessibility remain coherent at their
  owning target; all affected callers still satisfy the contract.
- Product values, meaningful states and enclosing compositions pass the owning
  design-system/brand gates when the change is visual.
- Self-review and independent review are complete; full audits reconcile every
  production file. Report actual checks and unverified runtime/visual outcomes
  through [validation-gates](../validation-gates/SKILL.md).
