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

When implementing or changing a feature, read and apply
`.claude/skills/state-preservation/SKILL.md`. Inventory every new feature screen
or affected existing state owner: required nonsecret user input, selection,
current filters and parent drafts must use the existing local StateKeeper pattern.
Rebuild data-backed state and exclude secrets/runtime state. Apply the mandatory
preservation gate through source review and permitted manual restoration checks;
changes with no state impact need only an assessment.

Do not introduce automated test sources, test folders, test dependencies or test
infrastructure unless the user explicitly requests it. Follow CLAUDE.md verification
policy: inspect actual source/diff, run permitted static/manual checks and report
unverified runtime behavior honestly.
