# Debug analytics coverage

Current scope, 2026-10-14: RootRouter.Debug → DebugComponent → retained
DebugViewModel → DebugScreen (Logger and General tabs).

- Independent screen: DebugScreen.Debug / `debug`, exactly one VM screenPresentation.
  BaseComponent/foundation owns Initial/Restore and pause/destroy close.
- DebugAnalytics is injected in retainedInstance; its annotated module is registered
  once in shared. Built-in screen_view is reused; no custom catalog or empty event API.
- Control/operation telemetry remains an explicit omission: logs, category/menu
  selection, clearLogs and generateTraining. No push token, log content, exercise
  measurements, credentials or entity IDs enter screen telemetry.
- State: the menu and log-category selections are a local screen draft (table
  below). The push token must never enter an analytics or StateKeeper payload.
- Permitted manual checks: open from Profile, pause/resume, Back and fresh re-entry;
  each activation produces one view when consent is granted, none when denied.

Verification is recorded in the current review section of ../COVERAGE.md.

## Preservation inventory

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| DebugComponent / DebugViewModel | `selected` (Logger/General tab) | Preserve | `DRAFT = PersistenceKey("DebugDraft", DebugState.serializer())`; `restoreState(DRAFT) ?: DebugState()` (non-null); `saveState(DRAFT) { viewModel.state.value }` | Default `DebugMenu.Logger`; a restored tab renders directly without replaying `onSelect` |
| DebugViewModel | `logger.selectedCategory` | Preserve | Same draft (`LoggerState` is `@Serializable`) | Default null; `loadLogs` keeps the saved category when the reloaded log file still has it, otherwise falls back to the first category |
| DebugViewModel | `logger.categories`, `logger.logsByCategory` | Rebuild | `@Transient`; `AppLogger.logFileContentsByCategory()` in `init`, after `clearLogs` and on switching to the Logger tab | Default empty; `logs` is derived from the map and the selected category |
| DebugViewModel | `pushToken` | Runtime only (secret-like device token) | `@Transient`; `AuthorizationFeature.getPushToken()` in `init` | Default null; never archived |
| DebugViewModel | `DebugLoader.Logs` / `GenerateTraining` jobs | Runtime only | `safeLaunch` | Not saved; log loading restarts on a fresh VM |
