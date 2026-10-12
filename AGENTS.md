# Grippo project instructions

Read `CLAUDE.md` for architecture and implementation conventions.

When implementing or changing feature behavior (screens, dialogs, navigation,
entry points, operations, validation or outcomes), read and apply
`.claude/skills/feature-analytics/SKILL.md`, even when tracking is not explicitly
requested. Keep the affected analytics coverage matrix current. Visual-only
changes require an impact assessment, not new events.
Every independent screen must have its ViewModel's `screenPresentation`;
containers keep default null. Apply the skill's mandatory screen coverage gate
to every screen of a new feature and every affected screen before completion.

The project analytics skill has one source in `.claude/skills/feature-analytics`.
