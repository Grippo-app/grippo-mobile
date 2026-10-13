# Room migration decisions

Necessary schema/migration changes are covered by the authorized feature outcome;
MUST NOT request repeat consent. Ask only when the proposed delta expands scope,
changes a locked decision or requires an unresolved data-retention/product fact.
Apply [project scope discipline](../../../../CLAUDE.md#scope-discipline).

1. Inspect current Database version, affected entities/DAOs, migrations, both platform builders
   and exported schemas under `data-services/database/`.
2. Specify the old→new schema and treatment of existing rows, drafts, foreign keys and indices.
   Missing release/data-retention policy is a user decision; do not infer it from fallback settings.
3. For an authorized migration use the existing `Migration<N>To<N+1>` object pattern with
   `SQLiteConnection`, register it in DatabaseMigrations and update the Database version.
4. Simple additive changes may use ALTER TABLE. Recreate tables only when the delta requires it;
   copy all retained data and recreate required indices. `PRAGMA defer_foreign_keys = ON`
   defers constraints until transaction completion; it does not disable integrity checks.
5. Review the migration chain and target schema against actual source. Run platform builds or
   manual restoration/migration checks only when permitted. Report absent regenerated schemas
   and unrun runtime migration scenarios; source review cannot prove successful data migration.

Do not modify destructive fallback or replace existing migrations as incidental cleanup.
