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
- State impact assessment: instrumentation only. Existing menu/category selections,
  data-backed logs and runtime push token are unchanged; no new draft/state owner.
  In particular the push token must never enter an analytics or StateKeeper payload.
- Permitted manual checks: open from Profile, pause/resume, Back and fresh re-entry;
  each activation produces one view when consent is granted, none when denied.

Verification is recorded in the current review section of ../COVERAGE.md.
