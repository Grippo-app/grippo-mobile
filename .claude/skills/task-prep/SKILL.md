---
name: task-prep
description: Clarify a Grippo repository task, resolve inputs and order implementation by real dependencies before coding. Use for task preparation, an ambiguous feature request or a change spanning several owners; no task board is required.
---

# Task preparation

Read [project conventions](../../../CLAUDE.md) and the actual affected source.
Produce enough scope and acceptance criteria to implement the user's request.
MUST NOT invent product behavior, backend contracts or absent infrastructure.

## Required decisions

- Identify the intended behavior, affected modules/platforms and exclusions.
- Distinguish confirmed requirements, source-supported defaults and open decisions.
- Existing user authorization covers necessary router, DI and module changes within
  that scope. MUST ask only for new scope, changed locked decisions or missing
  information that affects correctness.
- Ask in this chat with 2–3 plain alternatives and a grounded recommended choice.
  If none is defensible, say what evidence is missing. Honor a user request for
  extra quizzes; do not manufacture uncertainty or ask the same question twice.
- Continue independent work while a question is pending; do not implement dependent
  behavior until a required answer arrives. Time passing is not approval.

## Dependency plan

1. Inspect owning source, relevant build files and callers before choosing names.
2. Route to the owning skills: [UI](../ui-feature/SKILL.md),
   [data](../data-layer/SKILL.md), [mappers](../mappers/SKILL.md),
   [DI](../di-modules/SKILL.md), [design](../design-system/SKILL.md),
   [platform/build](../platform-build-toolkit/SKILL.md). Read only needed references.
   Designing, redesigning or visually reviewing product UI MUST also apply
   [brand-design](../brand-design/SKILL.md): establish the surface's purpose,
   composition, existing shared owners and meaningful states before choosing
   widgets. Keep its brief in the current task context, not a mandatory new file.
   Requested product artwork MUST route to [illustrations](../illustrations/SKILL.md)
   for style, reuse and asset acceptance; design-system retains resource/UI ownership.
   Distinguish standalone delivery from screen integration before planning consumers.
3. Order by actual producers and consumers: a screen needs its used contracts,
   state and widgets; an endpoint needs its verified API shape; a mapper needs
   its source/target types. Dialogs have the same real prerequisites as screens.
   Do not impose a fixed builder roster or create unused modules/capabilities.
4. Split broad work into small coherent slices with ownership and checkable outcomes.
   Assign nonoverlapping files to agents only when delegation is authorized.
   Coordinate shared files explicitly; no concurrent edits to the same owner.
5. For feature changes, apply [analytics](../feature-analytics/SKILL.md) and
   [preservation](../state-preservation/SKILL.md): enumerate actual screens,
   callbacks/operations and user-owned fields/parent drafts before implementation.
   Keep inventories in the task's existing record or report, with explicit omissions.
6. Define permitted source/manual/build checks and what they can establish.
   MUST follow the canonical [verification policy](../../../CLAUDE.md#verification-policy).
   Builds needed within task scope do not require separate consent.

## Preparation gate

A runnable slice has a clear outcome, real owners, available prerequisite types
and resolved behavior-affecting inputs. Unavailable prerequisites are reported
with the specific missing fact and the work that remains possible. A user's
prompt can be the task record; no generated reports, IDs or files are mandatory.

Hand off scope, ordered slices, accepted choices, required gates and limitations
into [local execution](../task-orchestrator/SKILL.md). Preparation does not require
approval of a plan when implementation was already requested and inputs are clear.
