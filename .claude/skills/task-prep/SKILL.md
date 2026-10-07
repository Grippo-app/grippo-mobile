---
name: task-prep
description: Prepare a task — turn a free-text backlog item into a well-formed todo. Take backlog → pending → todo, ask clarifying questions, set acceptance anchors (Automated / Manual split), do the Figma design-system-first split, classify builders and builder order, detect blockers and dependencies. Triggers on "prepare a task", "prep TASK_12", "backlog to todo", "task questions", "acceptance anchors", "split a task", "classify builders", "promote a backlog item".
---

Owns the task-intake + task-prep work. This is the
operational entrypoint between a free-text user idea and a structured task the
orchestrator can execute; it produces **questions** when scope is unclear and a
**promoted todo** when it is clear. It never writes product code. Long normative
rules live in this skill's references + frozen contracts (routed below).

## When to use

- Promote a fresh backlog file (Mode A) — `prep TASK_<N>_<title>`.
- Re-evaluate an answered questions sidecar (Mode B) — the user filled `### Answer` blocks.
- Classify a task against the change-kind taxonomy and emit its builder order.

Not this skill: writing Kotlin/JS/product code, invoking product builders or
product validators (orchestrator's job, only after the task is in `todo/`),
editing `done/`. The canonical task-state validator and transition helper are
mandatory publication infrastructure, not product validation.

## Required inputs

- **Mode A** — `orchestrator/tasks/backlog/TASK_<N>_<title>.md` with the required first line `# TASK <N> — <title>`.
- **Mode B** — the answered `orchestrator/tasks/pending/TASK_<N>_<title>.questions.md` (path or inline form; before analysis, helper-persist inline bytes verbatim, re-read and prove byte-for-byte/SHA-256 equality, then obtain a fresh canonical `sourceRevision`).
- The caller's prompt states which mode. `<STEM>` = the filename without `.md` / `.questions.md`.

## Board Prepare no-questions contract

When the caller includes the exact marker `BOARD PREP POLICY: NO QUESTIONS.`,
apply this action-scoped contract to that run only. It belongs to the
server-owned Board **Prepare** action; it is not a project/global setting and
does not change direct task-prep invocations.

- Never emit `needs_action`, invoke the `ask` transition, or publish a
  `pending/*.questions.md` sidecar.
- Resolve repo-decidable gaps from evidence. For a reversible ambiguity, choose
  the safest conservative default and record it as an `Assumed —` Input.
- For irreversible, destructive, authorization, breaking-contract, or
  missing-owner ambiguity, keep the unsafe portion out of scope and return a
  typed actionable blocker/follow-up. Do not turn it into a user question.
- If a safe runnable scope remains, compose it and `promote` to `todo`. A typed
  blocker is preferable to guessing, but it is never a request for an answer.

## Workflow

1. **Pre-flight + authority + lock** — verify the required references and select exactly one guarded writer authority: inherited site session, exact caller-owned standby lease receipt (verify it; never reacquire or release it; renew only as specified in `references/prep-flow.md`), or a newly acquired prep-owned direct lease. Run the edit-marker guard. If the caller supplied a complete task-lock receipt, verify its exact `stem`/`stage`/`runId`/`sessionId`/`lockHash`; otherwise acquire one no-clobber generation through `task-lock.mjs`. A standby acquire passes the caller receipt's exact `sessionId`, `leaseId`, and private `token`; the helper verifies that capability before and after publication without persisting or returning the token. Retain the complete lock receipt privately. For Mode B inline, first validate pending state, helper-persist the prompt's exact answer bytes through `persist-answers --input -`, require the matching receipt, and re-read exact byte/SHA-256 equality; then re-run canonical validation and retain its fresh revision for analysis and any second transition. `persist-answers` permits only Answer-body changes in the current round and rejects answer markup that changes the CommonMark-visible question/Answer heading identity; `ask` is reserved for a newly generated round and enforces the generation counters. Never analyze inline answers before that durable first transaction, and never reuse its pre-persistence revision. For every other input, validate the exact current stem plus fresh INDEX and retain the mutation `sourceRevision`. Emit `phase-start`. Never write or delete lock bytes directly.
2. **Classify (Step 1)** — reuse the change-kind taxonomy from `references/intake-classification.md` verbatim (do not re-derive); record the builder kinds implied + which prerequisite scaffolds exist.
3. **Builder order** — order builders low-layer → high per `orchestrator/contracts/builder-order.md`: toolkit/compose-lib FIRST, then data-service-scaffold → data-feature → ui-core-state → mapper → feature-module-scaffold → screen/dialog → cross-feature-nav → resource → app-shell LAST. Prepend missing prerequisites (`feature-module-scaffold` before `screen`, `ui-core-state` before `mapper`/`screen`, `data-service-scaffold` before `endpoint`/`room-migration`).
4. **Gap analysis (Step 2)** — spawn `context-finder` + `requirements-lookup` in parallel; build the gap list (missing inputs, unbounded scope, multi-feature, resource-only, migration triggers, contract-missing endpoints, missing screen designs).
5. **Decide (Step 3)** — split gaps into decidable (repo/convention/precedent answers it) vs undecidable (product intent, irreversible migration/destructive/authorization, ambiguous owner, missing artifact). Draft a grounded default for every decidable gap (record it as an `Assumed —` Input) and PROMOTE; ASK only the undecidable ones. Under `BOARD PREP POLICY: NO QUESTIONS.`, ASK is forbidden: use the action-scoped safe-default/blocker rules above and promote whenever a safe runnable scope remains. Bias toward drafting, not asking.
6. **Ask (Step 4)** — this step is unreachable under `BOARD PREP POLICY: NO QUESTIONS.` Otherwise, ask only undecidable gaps, and every question proposes a concrete default to confirm (`**Recommended**:` for choice, `**Proposed**:` for text) — never blank. Compose the complete questions sidecar in memory, then pipe its exact UTF-8 bytes to `transition-task-state.mjs ask --input -` with the exact fresh revision and writer authority. Close stdin and require its v1 pending-state receipt; never write the durable sidecar directly or write an intermediate proposal file.
7. **Compose promotion (Step 5)** — compose the standard todo shape in memory, including acceptance anchors and defensive boundaries, and freeze its proposed `## Design` section. Do not invoke `promote` until Step 5.5 has finished and every promotion-time augmentation is folded in.
8. **Figma census split + promote (Step 5.5)** — gated on `figmaEnabled: true` + any non-`none` `## Design` bullet: pipe the proposed parent bytes to the cache pre-flight through `FIGMA_SCREEN_TASK_FILE=-`, run component census, `AMBIGUOUS`/`RETIRED`/`SOURCE_STALE`/`UNSUPPORTED` → ask (owner registry/inventory remedies) in the normal direct flow, but return the exact typed remedy as a non-question blocker under `BOARD PREP POLICY: NO QUESTIONS.`; `MISSING`/`INCOMPLETE` → create each child through the canonical deterministic absent→backlog creator and then promote it through the same transition helper; never mint a number or write absent→todo. Chain the validated child stems in the parent, append preview/spec/screenshot acceptance, then pipe the final exact UTF-8 bytes to `transition-task-state.mjs promote --input -`. Require its v1 todo-state receipt. The helper alone writes todo, detaches backlog/pending, validates, rolls back, and publishes INDEX.
9. **Mode B convergence (Step 6)** — classify each answer (clear/partial/off-target/empty/SKIP), re-run gap analysis, compute `T_now`; PROMOTE at 0, ASK on strict shrinkage, ESCALATE on two stuck rounds.
10. **Verify receipt + release** — verify the helper's final postcondition/fresh-INDEX receipt and re-read the durable result. Release only the exact lock generation through `task-lock.mjs release` with the retained run/session/hash tuple **and** that final receipt's exact `state`/`sourceRevision`; on a non-transitioning BLOCKED/ESCALATE exit, first obtain an equivalent fresh green state + INDEX receipt. Require the release receipt to echo those values and a valid snapshot hash. If its atomic detach was interrupted, use idempotent `recover-release` with the same lock tuple plus a fresh final state/revision, and never delete `.release-*` by hand. Then release only a writer lease that task-prep itself acquired; a caller-owned standby lease is released by standby after the full prompt returns. A missing/mismatched lock or stale postcondition is a recovery condition, never a raw-delete no-op. Never run a second INDEX publisher. Emit `phase-end` / `stop` and return the Step-8 block.

## Stop and ask (BLOCKED / ESCALATE)

- Backlog missing the `# TASK <N> — <title>` first line → BLOCKED (don't auto-number).
- An `## Outcome` appendix present in backlog/pending/todo → BLOCKED; preserve it and reconcile through canonical finalization/reopen recovery. Never strip Outcome by hand.
- Mode B inline answer write mismatch → `BLOCKED: answer sidecar write failed — re-submit`.
- Stem already lives in another column (`todo/` or `done/`) → BLOCKED (collision; a stem is in exactly one column).
- A promoted `### Automated` bullet with no automation anchor → BLOCKED (don't silently move to Manual).
- A self-dependency or dependency cycle → BLOCKED. An ordinary dependency missing
  from `done/` stays in `## Depends on`; preparation may promote it and Run will
  remain closed until the prerequisite is accepted in `done/`.
- Screen-cache pre-flight miss (Step 5.5, non-`none` design) → BLOCKED, "Pull Figma screens".
- Convergence stall (gap count not shrinking two rounds running) → ESCALATE (split / rewrite / drop).

## References to read

Self-contained reference pack — the prep MECHANICS live in `references/`. Read
**only** these references + the frozen contracts at runtime; this skill reads no
external rule docs. Route via `references/index.md`.

| topic | read |
|---|---|
| Routing table + file map (start here) | `references/index.md` |
| Change-kind taxonomy + prerequisites + **builder order** + validator gate + intake plan | `references/intake-classification.md` |
| Full prep flow (Modes A/B, pre-flight, lock, journal, gap analysis, questions, Mode B convergence, canonical INDEX publication, output blocks) | `references/prep-flow.md` |
| Promoted todo shape — acceptance Automated/Manual split, automation anchors, defensive bullets | `references/acceptance-anchors.md` |
| Figma census design-system-first split + `## Preview states` + screenshot-gate bullets | `references/figma-split.md` |
| Every BLOCKED / ESCALATE condition — deps, collision, migration/contract/refactor, convergence stall | `references/blockers-and-dependencies.md` |
| Builder ordering / prerequisite-prepend rules (frozen contract) | `orchestrator/contracts/builder-order.md` |
| Frozen per-agent surface (name, tools, inputs, outputs, stops) | `orchestrator/contracts/agents/task-intake.md`, `orchestrator/contracts/agents/task-prep.md` |

Cross-reference (owned by another skill — see `references/index.md`):
`orchestrator/tasks/README.md` (lifecycle/board),
`../validation-gates/references/when-to-stop-and-ask.md` (stop-and-ask rules).
The cookbook, project-config, and figma screen-cache/census rules live in their
owning skills' `references/**`.

Always pre-flight every file with `[ -f <path> ]` before reading; a miss is a BLOCKED return.

## Validators / gates

No product-code validators — prep runs **before** the orchestrator pipeline.
Every durable task mutation nevertheless passes the canonical task-state
validator and deterministic transition helper. The semantic prep gates are:

- **Source provenance gate** — a real canonical `## Source` immediately after
  the H1 is immutable. Copy its exact block into every todo proposal; never
  derive it from prose, rewrite it, or put it in a pending questions sidecar.
  When Source Type is `api-work-package`, the canonical
  `## API Work Package` section is part of that immutable provenance: preserve
  the complete section byte-for-byte immediately after Source in every todo
  proposal.

- **Convergence cap** — Mode B iterates while the gap count strictly shrinks; two
  consecutive stuck rounds ESCALATE (`T_now >= T_prev` and `T_prev >= T_prev_prev`).
- **7-questions-per-round** UX cap (render limit only; not the gap total).
- **Dependency gate** — preserve unresolved dependencies during promotion;
  block only self-dependencies/cycles here. Canonical Run admission requires
  every dependency to be accepted in `done/`.
- **Cross-column collision gate** — a stem must exist in exactly one column.
- **Acceptance-anchor gate** — every `### Automated` bullet carries a file/class/gradle anchor.
- **Cache pre-flight gate** — the only hard stop in the Figma split path (non-`none` designs).

## Output contract

Return the single Step-8 Markdown block matching what happened — Mode A promoted /
questions, Mode B promoted-after-answers / more-questions, or ESCALATE (see
`references/prep-flow.md` for the exact shapes) and the frozen
[`task-prep`](../../contracts/agents/task-prep.md) contract. Builder
ordering in the plan MUST follow `orchestrator/contracts/builder-order.md`. On-disk
products: the helper-published `pending/<stem>.questions.md` sidecar (ASK path)
or `todo/<stem>.md` task (PROMOTE path), plus the helper's verified fresh
`INDEX.json`. Every promoted task body preserves the source task's canonical
`## Source` block byte-for-byte and, for Source Type `api-work-package`, its
complete canonical `## API Work Package` section byte-for-byte immediately
after Source; `## Origin` remains separate lineage.
Task-prep keeps proposal bytes in memory and delivers them only
through the helper's bounded `--input -` stdin contract; it never mutates
durable column files or stages proposal artifacts itself.
