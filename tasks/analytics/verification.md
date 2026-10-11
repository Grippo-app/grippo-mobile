# Проверка нового ядра аналитики

Дата: 2026-10-11. Фичи пока не подключены.

## Автоматические проверки

`./gradlew :toolkit:analytics:api:testAndroidHostTest :toolkit:analytics:core:testAndroidHostTest :toolkit:analytics:api:iosSimulatorArm64Test :toolkit:analytics:core:iosSimulatorArm64Test` — BUILD SUCCESSFUL; Android 16 тестов, iOS Simulator 15, failures=0/errors=0. Общие тесты выполняются нативно на iOS; Android дополнительно проверяет 200 событий от четырёх параллельных потоков без одновременных вызовов sink.

Проверены схемы, лимиты, обязательность, типы чисел, snapshot контекста, независимость flow, сериализация/валидация восстановленных идентификаторов, Unknown/Denied, очередь до готовности, overflow, смена identity, очистка SDK properties, отзыв consent во время отправки, исключения транспорта и явный retry, строгий debug, bounded diagnostics/recording, закрытие, отсутствие временного отзыва consent при смене identity.

`./gradlew :androidApp:assembleDebug :shared:assembleSharedDebugXCFramework` — BUILD SUCCESSFUL. XCFramework содержит iosArm64 и iosSimulatorArm64.

`xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'platform=iOS Simulator,id=8A3E8CAA-5BCC-4A9C-9FB3-610B71034B0F' CODE_SIGNING_ALLOWED=NO build` — BUILD SUCCEEDED. Это проверяет настоящий Swift adapter и экспорт нового Kotlin API.

Обычный cold start обоих приложений выполнен. Android процесс жив; SDK пишет measurement disabled и analytics storage denied. iOS запускается; SDK пишет Analytics collection disabled. Симулятор не проверяет APNs и подписанный запуск на физическом устройстве.

## Обзор кода

Проверены корректность/гонки, читаемость, направление зависимостей, данные и память. Исправлено временное выставление Denied перед каждым identity update: отзыв consent мог бы сбрасывать Firebase SDK данные. Добавлен regression test. Удалены все шесть старых вызовов в четырёх ViewModel и старый публичный FirebaseAnalyticsProvider. Crashlytics/Messaging сохранены. Фичи получают только Analytics API; Firebase maps доступны лишь на границе транспорта.

## Проверка SDK delivery

Временное событие `analytics_core_probe` проходило через общий типизированный API и каталог, worker и платформенные адаптеры. Fixture: count=42 (Long/NSNumber Int64), duration=1234 (Long/NSNumber Int64), number=2.5 (Double/NSNumber Double), schema_version=1, screen_id=core_probe, referrer_screen=probe_origin, navigation_reason=action, launch_source=launcher, entry_point=verification, flow_id=core_probe_flow, origin_screen=probe_origin, operation_id=core_probe_operation. Реальные пользовательские значения не использовались.

Android SDK журнал: event logged, `_dbg=1`, `int_value`/`double_value` корректны, successful upload HTTP 204. iOS SDK журнал: event logged, `ga_debug (_dbg)=1`, числовые значения корректны, successful upload HTTP 204. Для iOS использована ad-hoc подпись симулятора (`CODE_SIGNING_ALLOWED=YES CODE_SIGN_IDENTITY=-`): неподписанный запуск давал Firebase Installations Keychain -34018.

В Chrome доступны Firebase Console и GA4 property 519002689; Firebase App ID Android потока совпадает с приложением. GA4 Streams показывают входящий трафик Android/iOS. Однако DebugView пока показывает 0 устройств: появление события в самом отчёте не подтверждено. HTTP 204 не заменяет эту проверку; checkpoint C остаётся открытым. Настройки удалённого проекта не менялись.

Временные probe/event/registration/hooks и вывод payload полностью удалены. В финальном коде тестового включения collection нет.

После удаления probe повторены `:androidApp:assembleDebug :shared:assembleSharedDebugXCFramework` (BUILD SUCCESSFUL) и Swift Simulator build с ad-hoc подписью (BUILD SUCCEEDED). `git diff --check` проходит. Поиск legacy API и временных probe/hooks в исходниках пуст. Финальный Android установлен и запущен без test extra: процесс жив, SDK collection выключен, analytics storage denied. Финальный iOS установлен и запущен без probe; Firebase debug mode отключён. Подробные тестовые log levels сброшены.

## Практические ограничения

Queue хранится в памяти, максимум 128; нет обещания offline durable delivery, replay или exactly-once. SDK acceptance отдельно от удалённой доставки. Уже начавшийся вызов SDK нельзя отозвать. До отдельной интеграции consent-фичи запуск всегда Unknown, сбор выключен; persistence согласия и ранний bootstrap до SDK auto-init остаются её задачей. Статические defaults не перекрывают ранее сохранённый SDK grant до инициализации приложения. Автоматическое screen reporting потребуется отключить при ручной интеграции навигации.
