# Задачи сервисных ядер

- [x] SDK-independent typed CrashReporting API/core.
- [x] SDK-independent typed Messaging API/core.
- [x] Per-event crash context вместо global custom keys.
- [x] Cancellation/failure/NotReady semantics без silent fallbacks.
- [x] Serial, bounded token callbacks, no per-refresh coroutine.
- [x] Shared Koin composition module и deferred handler; migrate existing consumers.
- [x] Android bootstrap/service и Swift bootstrap/adapters.
- [x] Remove legacy FirebaseProvider/providers/bridge.
- [x] Self-review rounds, regression red/green и fixes.
- [x] Final core/analytics/shared tests + Android debug/release + XCFramework device/simulator.
- [x] Swift application build + final source/config checks.
- [x] Evidence report и финальный review verdict.

Remote crash/push/SDK delivery tests не выполняются. Независимый внешний review не заявляется.

## CrashReporting API refactor

- [x] Типизированная матрица ключей и явные reportException/logBreadcrumb.
- [x] Разделение публичных типов и internal policies по файлам.
- [x] Обновление Kotlin/Swift consumers, без compatibility aliases.
- [x] Self-review: uniqueness по имени, строгие фабрики и bridge limits вне цикла.
- [x] Подтвердить итоговые сборки, тесты и Swift device/simulator.

## Messaging API refactor

- [x] Явные tokenState/tokenSyncStatus/fetchToken и provider/synchronizer contracts.
- [x] Типизированные причины и internal token rules; один публичный тип на файл.
- [x] Обновление Kotlin/Swift consumers без compatibility aliases.
- [x] Self-review: opaque token, single validation, callback/fetch isolation и DI cycle.
- [x] Подтвердить Android/iOS tests, app builds и Swift generated header.
