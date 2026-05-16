# Tasks

Drop one Markdown file per task here. The orchestrator (`requirements/sub-agents/helpers/orchestrator.md`) picks it up when the parent Claude session is asked to run it.

## File naming

```
TASK_<N>_<short_title_in_snake_case>.md
```

`<N>` is the next available integer (zero-padded if you prefer). Examples:

- `TASK_1_note_archive_screen.md`
- `TASK_2_notifications_data_feature.md`
- `TASK_3_tag_picker_dialog.md`

## File shape

A task is a contract, not a plan. State what to build and what "done" looks like. Do **not** write the implementation steps — the agents read `requirements/` for that.

All four sections — `## Goal`, `## Inputs`, `## Acceptance`, `## Out of scope` — are **required**. A TASK file missing any of them is rejected as BLOCKED by `task-intake`.

```markdown
# TASK N — <title>

## Goal
One paragraph. What capability does the user gain when this lands?

## Inputs
- Data this depends on (existing `*Feature` interface, existing route, existing widget).
- Where the entry point lives (which screen launches it, which menu item, which deeplink).
- Any constraints not derivable from `requirements/` (product-specific copy, design figure references, accessibility callouts).

## Acceptance
- Bulleted, observable. "Screen renders X, Y, Z." "Tapping the chart pushes route W."
- Includes the build gate: "`./gradlew :shared:assembleSharedDebugXCFramework` and `./gradlew :androidApp:assembleDebug` both green."

## Out of scope
- What the task does NOT cover. Cuts off scope creep. **Required** — write "nothing else" if the boundary is trivial, but the section must be present.

## Depends on (optional)
- TASK_2 — must be in `tasks/done/` before this task runs.
```

The `Depends on` section is optional. List the prerequisite task file stems (`TASK_<N>_<title>`, no `.md` extension). `task-intake` verifies each listed file is in `requirements/tasks/done/`; if not, the orchestrator returns `BLOCKED: depends on incomplete tasks: <list>` and refuses to run.

## Example

A complete reference task ships at `requirements/tasks/TASK_0_example_note_archive.md.example`. Copy it (drop the `.example` suffix), rename per the next available `<N>`, and edit to fit your task.

## How execution works

The TASK file MUST include a `## Out of scope` section. Even if the section is short ("nothing else"), an explicit boundary prevents builders from drifting into adjacent files. Tasks without this section are returned BLOCKED by `task-intake`.

1. Drop the file here.
2. Tell Claude: *"Run task TASK_N_<title>.md."*
3. Parent invokes `helpers/orchestrator`, which:
   - Calls `helpers/task-intake` to classify the change and pick the builders.
   - Runs each builder.
   - Runs every applicable validator. Failures route back to the builder.
   - Hands off to an external reviewer — `helpers/codex-review-loop` when the Codex plugin is installed (and `codexEnabled` in `00-overview/03-project-config.md` permits), otherwise `helpers/internal-reviewer` as the Claude-backed fallback.
   - Routes any reviewer findings back through the responsible builders and loops.
   - Reports done only when every validator passes and the external reviewer is clean.

See `requirements/sub-agents/README.md` for the full flow.

## After a task ships

The orchestrator **always** moves the completed TASK file to `requirements/tasks/done/<file>` as the final step of a successful run (see `requirements/sub-agents/helpers/orchestrator.md` Step 6). This is the project convention — not an opt-in:

- `requirements/tasks/` holds only **in-flight** tasks.
- `requirements/tasks/done/` is the audit trail of what shipped (preserved in git history).
- The orchestrator skips the move only when a task ends in escalation, not completion.

If you want to discard a task without running it, delete the file before invoking the orchestrator. Once a task is in `done/`, leave it there — `task-intake` will refuse to re-run a moved file.

If the task surfaced architecture drift, update the relevant chapter under `requirements/<NN>-<area>/` manually.
