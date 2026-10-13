# Self-review loop

Дата: 2026-10-11. Область: новые API/core, adapters/bootstrap, миграция существующих consumers. Remote Firebase operations исключены.

## Round 1 — contracts и correctness

Найдены и исправлены:
- Legacy Android token fetch скрывал SDK errors и CancellationException. Typed Available/Unavailable/Failed; настоящая отмена propagates, активный transport failure остаётся explicit failure.
- Legacy Swift SDK errors выдавались за NotReady. NotReady теперь только до APNs token; SDK error — Failed.
- Android crash metadata попадал в persistent global keys. Теперь per-event CustomKeysAndValues; Swift сохраняет event context в конкретном ExceptionModel, без global keys.
- Legacy service/bridge создавал launch на каждый callback, service scope не закрывался. Новый app-lifetime core: один worker, conflated token queue; service только передаёт validated token.
- DI cycle Messaging → token handler → authorization repository → Messaging. Deferred handler resolution в composition root разрывает цикл, не перенося service locator в feature code.

## Round 2 — races, lifecycle, boundary

Найдены и исправлены:
- Fetch нового token мог блокировать завершение callback sync status. Sync target теперь отделён от результата fetch.
- Поздний non-cancellable SDK result мог публиковаться после cancellation caller. После ответа SDK проверяется coroutine activity до публикации.
- Дублирование token validation в Swift могло пропустить отличия Swift grapheme count от Kotlin length и вызвать exception через ObjC constructor. Общий non-throwing decoder используется платформенными ingress.
- Token buffer при shutdown освобождается через cancel; worker регулярно проверяет cancellation и уступает dispatcher.
- Swift traversal cause chain ограничен 32, frame conversion — 128; cycles отслеживаются. Invalid typed metadata не исчезает при try? serialization.
- Push token SDK failure не должен срывать успешный login. Существующая nullable authorization boundary сохраняет explicit best-effort policy: failure фиксируется nonfatal report без токена. Core не маскирует failure.

## Round 3 — architecture, cleanliness, costs

- Consumer dependencies только api; shared связывает core/API, не зависит от Firebase module. Transport остаётся в data-services/firebase, Swift SDK adapters — в iOSApp. Legacy singleton/provider/bridge удалены, orphan interfaces не оставлены.
- Без новых SDK versions/third-party dependencies. Crash API не зависит от coroutines; Messaging API экспортирует StateFlow из существующей coroutines dependency.
- Crash queue ≤16 plus one in-flight; one IO worker, no per-report launch и no recursive reporting. SDK work вне state lock, транспорт не вызывает core повторно. Throwable stack не подменяется новой synthetic exception при обычном report.
- Messaging: one worker + conflated slot, one fetch Mutex, platform lock только вокруг state; network выполняется вне lock. Token strings ≤4096, toString redacted. Нет raw exception/token в status/diagnostics и no automatic retries.
- CrashReportContext: ≤32 unique keys, bounded stable codes/nonnegative Long/Boolean; snapshot защищает от изменения входного списка. Ключи event-specific, глобальных mutable custom keys нет.
- Product notification rendering, account/session policy, consent/UI, SDK persistence/fatal capture остаются своими слоями. Их поведение не выдаётся за guarantee core; ограничения documented в integration-guide.md.

## Приёмка

Финальная проверка dependency graph выявила три оставшиеся legacy Firebase dependencies в UI-модулях start-training/training/authorization, хотя вызовов уже нет. Удалены: теперь единственный consumer Firebase transport module — Android application. Koin dependency транспорта перенесена в androidMain, common слой SDK wrapper больше не несёт ненужный DI runtime.

Swift name lookup: общий Messaging API и Firebase SDK экспортируют одно имя Messaging. SDK references/type annotations явно квалифицированы FirebaseMessaging.Messaging; application build проверяет эту границу.

Проверка generated Objective-C header выявила ещё одну Swift границу: вложенные классы sealed interface PushTokenState экспортируются как PushTokenStateUnavailable/PushTokenStateFailed, не как nested Swift classes. Adapter использует реальные generated names; типы значений остаются explicit.

Первый настоящий Swift build обнаружил lowercase enum member names (`platformnotready`, `sdkfailure`, `invalidtoken`) вместо Swift camelCase. Исправлено по generated header; повторный build обязателен. Android lint debug/release прошёл.

## Round 4 — установленный SDK и caller overhead

Проверены source files установленного Apple Firebase SDK: FIRCrashlytics.m → FIRCLSExceptionRecordModel → FIRCLSExceptionRecord → FIRCLSUserLoggingWriteAndCheckABFiles. Последний использует dispatch_sync(logging queue) и local file writes. Прямую submission из caller отвергли: она могла задерживать UI и удерживала state lock.

Crash core теперь использует один app-lifetime IO worker, bounded queue, conflated wake. SDK calls выполняются вне state lock, wake тоже вне lock; no per-report launch, cancellation checks + yield batch32. Active sink CancellationException — transport failure, actual cancellation остаётся cancellation. Suspend close отменяет/joins worker в NonCancellable, затем освобождает channel; native SDK fatal capture не меняется.
