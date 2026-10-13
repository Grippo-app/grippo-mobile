# Crash reporting и Messaging cores

Статус: реализовано и проверено. Self-review findings закрыты; результаты в verification.md, архитектура/ограничения в integration-guide.md.

Область: отдельные typed API/core, Firebase transport, Koin/Android/iOS bootstrap, миграция существующих consumers. Новые product features и remote delivery checks не входят.

1. Контракты: diagnostic codes/attributes, opaque token, explicit unavailable/failure.
2. Crash core: bounded queue и один IO worker, serial SDK submission вне state lock, cancellation excluded, no global metadata leakage.
3. Messaging core: serial token sync, conflated updates, cancellation propagation, stale fetch protection.
4. Platform adapters/bootstrap + existing consumer migration, remove legacy provider/bridge.
5. Source review, Android debug/release, XCFramework device/simulator, Swift build.
6. Self-review loop: correctness/races/lifetimes, strictness/privacy, complexity/memory; fix all findings and repeat affected checks.

Firebase SDK owns fatal capture, persistence and delivery. CrashReporting enqueue не выполняет SDK work в caller; serial SDK submission выполняется на IO worker. Push rendering/APNs/permissions remain existing platform responsibilities.

## CrashReporting API refactor (2026-10-12)

Разделить diagnostic codes и metadata keys; ключами фиксировать Code/Long/Boolean.
Сохранить wire-значения и поведение доставки. Разделить API по ответственности,
централизовать validation/queue/native-stack policies. Обновить существующих
Kotlin/Swift consumers; подтвердить DI, Android Debug/Release и iOS
Debug simulator/device без запуска приложения и отправки в Firebase.

Завершено: [отчёт CrashReporting API](crash-api-refactor-review.md).

## Messaging API refactor

Разделить consumer state, SDK provider, backend synchronizer и startup boundary.
Централизовать token validation/redaction policy; сохранить opaque token и текущую
latest-token-wins/cancellation semantics. Обновить Kotlin/Swift consumers и проверить
app wiring, generated header и Android/iOS builds без remote SDK calls.
