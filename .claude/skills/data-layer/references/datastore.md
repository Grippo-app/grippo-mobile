# DataStore preferences

Inspect `data-services/datastore/` and the owning `data-features/local-settings/` Repository.
The service supplies storage; keys, interpretation and mutations belong to the owning Repository.

- Reuse the existing preference owner and key naming. Define type/default/meaning explicitly;
  a missing preference must not silently authorize a business action.
- Preserve current observation and Result/throwing mutation contracts. DataStore edit operations
  must preserve cancellation; bare suspend runCatching is not a compliant shortcut.
- Tokens/secrets do not become ordinary preferences. Use the existing auth owner.
- Screen drafts use the local StateKeeper pattern, not a new global preferences persistence scheme;
  apply [state preservation](../../state-preservation/SKILL.md) when behavior changes.
- Review key compatibility, atomic edits, defaults and logout clearing in actual source.
