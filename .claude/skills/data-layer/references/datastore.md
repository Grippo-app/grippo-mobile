# DataStore preferences

Inspect `data-services/datastore/` and the owning `data-features/local-settings/` Repository.
The service supplies storage; keys, interpretation and mutations belong to the owning Repository.

- Reuse the existing preference owner and key naming. Define type/default/meaning explicitly;
  a missing preference must not silently authorize a business action.
- Preserve current observation and Result/throwing mutation contracts. `LocalSettingsRepositoryImpl`
  observes through `dataStore.data.map { }` and mutates through `dataStore.edit { }` inside a
  `try` that rethrows `CancellationException` and returns other `Exception`s as `Result.failure`;
  keep that boundary and add no `runCatching`.
- Tokens/secrets do not become ordinary preferences. Use the existing auth owner.
- Screen drafts use the local StateKeeper pattern, not a new global preferences persistence scheme;
  apply [state preservation](../../state-preservation/SKILL.md) when behavior changes.
- Distinguish a pending read, an absent key and an explicitly saved default when
  behavior depends on that distinction. A Boolean storage key may map to a closed
  domain type; absence must not erase a saved refusal or manufacture agreement.
- Classify account and installation preferences before changing clear/logout/delete.
  Preserve or remove keys atomically according to their actual owner and accepted
  product policy; neither clear-all nor retain-all is a universal cleanup rule.
- Review key compatibility, atomic edits, defaults and logout clearing in actual source.
  Analytics consent follows [collection policy](../../feature-analytics/references/collection-and-core.md#collection-and-consent).
