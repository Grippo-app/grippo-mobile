# Grippo project instructions

MUST read [CLAUDE.md](CLAUDE.md) for architecture, scope and verification policy.
Project skills have one canonical source in `.claude/skills/`; MUST NOT create
parallel copies or treat historical reports as current rules.

Feature behavior changes (screens, dialogs, navigation, entry points, operations,
validation or outcomes) MUST apply [feature analytics](.claude/skills/feature-analytics/SKILL.md)
even without an explicit tracking request. MUST update affected coverage and pass
its screen coverage gate for every new feature screen or affected existing screen.
Independent screens require their VM's `screenPresentation`; containers keep null.
Visual-only changes require an impact assessment, not new events.

Operational analytics tasks MUST apply the feature skill's
[live admin synchronization gate](.claude/skills/feature-analytics/references/admin-sync.md).
Apply its trigger and editorial/no-impact exceptions.
MUST attempt to inspect the analytics browser tab early; if unavailable, ask the
user to open the correct property's analytics admin tab. Reconcile affected code
events and admin definitions, key events and reports, verify saved changes, and
record release/legacy compatibility. Authorized live API/read-back evidence may
complete reconciliation when browser inspection is unavailable, as the gate defines.
Missing admin access MUST remain an explicit
unfinished synchronization outcome; source/build checks cannot waive it.
Removal requires usage/dependency evidence and the owning gate's deletion safeguards.

New or changed features MUST apply [state preservation](.claude/skills/state-preservation/SKILL.md).
MUST inventory every new screen or affected state owner, preserve required nonsecret
user inputs/selections/filters/parent drafts with local StateKeeper, rebuild
data-backed presentation and exclude secrets/runtime state. MUST pass its source
gate and report permitted manual restoration checks. No-state-impact changes need
an assessment only.

MUST NOT introduce automated test sources, folders, dependencies or infrastructure
unless the user explicitly requests them. MUST follow the canonical
[verification policy](CLAUDE.md#verification-policy): source/diff review, relevant
permitted static/manual checks, scoped builds and honest runtime limitations.
