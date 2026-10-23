# Room migration decisions

Necessary schema/migration changes are inside the authorized scope; do not request repeat
consent. Ask only when the proposed delta is scope expansion, changes a locked decision or needs
an unresolved data-retention/product fact
([scope discipline](../../../../CLAUDE.md#scope-discipline)). Current versions, migrations,
builders and schema locations are listed in [Room persistence](persistence-room.md).

1. Inspect current Database version, affected entities/DAOs, migrations, both platform builders
   and exported schemas under `data-services/database/`.
2. Specify the old→new schema and treatment of existing rows, drafts, foreign keys and indices.
   Missing release/data-retention policy is a user decision; do not infer it from fallback settings.
3. For an authorized migration use the existing `Migration<N>To<N+1>` object pattern with
   `SQLiteConnection`, register it in `DatabaseMigrations.all`, bump `@Database(version = N)` and
   include the regenerated `schemas/.../<N>.json` in the change. Git actions follow
   [delivery](../../delivery/SKILL.md).
4. Simple additive changes may use ALTER TABLE. Recreate tables only when the delta requires it;
   copy all retained data and recreate required indices. `PRAGMA defer_foreign_keys = ON`
   defers constraints until transaction completion; it does not disable integrity checks.
5. Review the migration chain and target schema against actual source. Run the permitted checks:
   the default build (its KSP pass regenerates the schema JSON) and, when the user allows running
   the app, an upgrade scenario under [runtime-verification](../../runtime-verification/SKILL.md).
   Report absent regenerated schemas and unrun migration scenarios; source review cannot prove
   successful data migration.

Do not modify destructive fallback or replace existing migrations as incidental cleanup.
