# Tasks

Drop one Markdown file per task here. The orchestrator (`requirements/sub-agents/helpers/orchestrator.md`) picks it up when the parent Claude session is asked to run it.

## File naming

```
TASK_<N>_<short_title_in_snake_case>.md
```

`<N>` is the next available integer (zero-padded if you prefer). Examples:

- `TASK_1_workout_history_screen.md`
- `TASK_2_notifications_data_feature.md`
- `TASK_3_rating_picker_dialog.md`

## File shape

A task is a contract, not a plan. State what to build and what "done" looks like. Do **not** write the implementation steps — the agents read `requirements/` for that.

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
- What the task does NOT cover. Cuts off scope creep.
```

## How execution works

1. Drop the file here.
2. Tell Claude: *"Run task TASK_N_<title>.md."*
3. Parent invokes `helpers/orchestrator`, which:
   - Calls `helpers/task-intake` to classify the change and pick the builders.
   - Runs each builder.
   - Runs every applicable validator. Failures route back to the builder.
   - Hands off to the Codex plugin for external review (if installed).
   - Routes any Codex findings through `helpers/codex-review-loop` and loops.
   - Reports done only when every validator passes and Codex has no findings.

See `requirements/sub-agents/README.md` for the full flow.

## After a task ships

- Move the task file to a `tasks/done/` subfolder (optional, for traceability) or delete it. The agents do not re-execute completed tasks.
- If the task surfaced architecture drift, run `invalidate.md` to update the requirements.
