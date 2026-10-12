# Новая аналитика: задачи ядра

Реализация ядра и локальные Android/iOS проверки выполнены. В текущей приёмке пользователь исключил отправку в Firebase; завершён глубокий локальный аудит (review.md). Доказательства: `verification.md`. Фичи пока не интегрировать.

## 1. Правила и границы
- [x] Описать словарь контекста, семантику попытки/успеха/ошибки, допустимые данные и владельцев событий.
- [x] Зафиксировать схему каталогов, стабильность имён и политику версии.

Приёмка: нет смешения launch source, referrer, entry point и reason; указаны границы ядра/фич.
Проверка: обзор контракта по регистрации, сохранению тренировки и уведомлению.
Зависимости: нет. Файлы: план, документ правил (2–3). Размер: S.

## 2. Типизированный API
- [x] Создать analytics:api и минимальные общие типы/схемы.
- [x] Определить расширение событиями отдельных модулей без глобальной sealed-иерархии.
- [x] Создать контракт транспорта и доступный Swift bridge без Any в контракте событий.

Приёмка: потребителю нельзя передать произвольные имя/map; причины ограничены типами; API независим от SDK/UI.
Проверка: компиляция Android/iOS API и небольшой Swift prototype с числовыми значениями.
Зависимости: 1. Файлы: Gradle/settings, 2–3 Kotlin-файла; при необходимости разбить platform prototype отдельно. Размер: M.

## Checkpoint A
- [x] Контракт прошёл обзор; Swift interoperability подтверждена сборкой приложения и числовым probe.

## 3. Контекст
- [x] Реализовать атомарный снимок общего контекста и явный неизменяемый контекст сценария.
- [x] Покрыть отсутствие источника, две независимые flow, back/restore контракт и смену пользователя.

Приёмка: события сохраняют контекст момента вызова; одна flow не меняет origin другой.
Проверка: focused host tests контекста, iOS compilation.
Зависимости: 2. Файлы: 2–3 core/context, тесты, build config (до 5). Размер: M.

## 4. Кодирование и валидация
- [x] Реализовать типизированное кодирование и каталог схем.
- [x] Проверить уникальность, границы типов, обязательность и суммарный бюджет параметров.
- [x] Добавить fixtures допустимых/недопустимых payload.

Приёмка: ошибки схем видны; release отклоняет невалидное событие; числа не становятся строками.
Проверка: focused schema/encoder tests.
Зависимости: 2–3. Файлы: encoder, validator/catalog, fixtures/tests (3–5). Размер: M.

## 5. Сбор и доставка
- [x] Реализовать ограниченную последовательную очередь, NotReady и диагностику потерь.
- [x] Реализовать policy/consent и очистку при отзыве разрешения/смене аккаунта.
- [x] Изолировать ошибки транспорта от пользовательского действия.

Приёмка: запрещённые события не буферизуются; контекст не меняется в очереди; сбой sink не ломает приложение.
Проверка: tests гонок, очереди, overflow, запрета и исключений; разбить на две задачи, если потребуется более 5 файлов.
Зависимости: 3–4. Файлы: engine/queue/policy, tests (3–5). Размер: M.

## Checkpoint B
- [x] Контекст, схемы и доставка проходят focused tests; поведение при потере/запрете документировано.

## 6. Android Firebase адаптер
- [x] Реализовать типизированный транспорт, identity/reset и сбор на уровне SDK.
- [x] Сверить SDK-лимиты и штатные события с актуальной документацией.

Приёмка: payload кодируется String/Long/Double, политика действует на SDK.
Проверка: adapter fixtures/tests и Android compilation.
Зависимости: 4–5. Файлы: adapter, build config, tests/config (3–5). Размер: M.

## 7. iOS Firebase адаптер
- [x] Реализовать Swift adapter с тем же payload, identity/reset и collection policy.
- [x] Проверить NSNumber и минимальный экспорт через shared.

Приёмка: значения и отсутствие полей совпадают с Android fixtures.
Проверка: Kotlin iOS device/simulator + Swift compilation и fixtures.
Зависимости: 4–5. Файлы: Swift adapter, export bridge, platform tests/config (3–5). Размер: M.

## 8. DI и запуск
- [x] Зарегистрировать ядро и транспорт в Koin до первых потребителей.
- [x] Применить static SDK defaults и явный Unknown до потребителей; проверить запуск root на iOS. Ранее сохранённый SDK grant до auto-init требует будущего consent bootstrap (см. verification.md).

Приёмка: cold start не теряет разрешённое первое событие и не отправляет запрещённое.
Проверка: init-order tests и запуск обоих приложений.
Зависимости: 6–7. Файлы: Koin.kt, App.kt, AppDeletage.swift, module wiring (до 5). Размер: M.

## Checkpoint C
- [x] Локальные Android/iOS проверки доставки через typed API и fixture sink. Firebase DebugView исключён пользователем из текущей приёмки; прежний SDK upload описан в verification.md.

## 9. Удаление старых вызовов
- [x] Удалить шесть старых вызовов и FirebaseProvider.Event/logEvent.
- [x] Удалить/заменить старый FirebaseAnalyticsProvider, сохранив Crashlytics/Messaging.

Приёмка: старая аналитика недоступна; регистрация/запись/сохранение работают как раньше. До интеграции фич продуктовые события отсутствуют — это явная временная граница.
Проверка: rg старых API, compilation затронутых модулей; разбить интерфейс/setup cleanup отдельно при большом diff.
Зависимости: 8. Файлы: четыре ViewModel, FirebaseProvider, transport/setup cleanup. Размер: две S/M подзадачи.

## 10. Диагностика и инструкция фич
- [x] Добавить Recording sink и ограниченный безопасный debug-журнал.
- [x] Описать пример схемы фичи, перенос origin, владельца успеха и процедуру проверки отчёта.

Приёмка: разработчик может проверить payload/отклонение локально; руководство не предлагает произвольные map/строки.
Проверка: debug/recording tests, обзор документа по одному будущему сценарию.
Зависимости: 5, 8. Файлы: debug sink, recording helper, tests, documentation (до 5). Размер: M.

## Checkpoint ядра
- [x] Focused tests и Android/iOS/Swift builds проходят; реальные команды/выводы записаны.
- [x] Выполнен code review нового контракта, контекста, политики и платформенных границ.
- [x] Старые события удалены, платформенные границы проверены; ограничения записаны. Прямой Firebase test исключён пользователем из текущей приёмки.
- [x] Завершить локальный audit и финальные Android/iOS/Swift checks; дальше отдельная задача на каждую фичу по порядку plan.md.


## Единый стандарт интеграции фич (2026-10-12)
- [x] Typed operation contract, flow/operation ID factories, общая suspend-обвязка; outcome/cancellation/correlation tests.
- [x] Atomic screen presentation API/core; context/consent/overlapping-close tests.
- [x] Leaf lifecycle bridge в foundation, без изменений базовых классов; host/native lifecycle tests.
- [x] Feature template, матрица покрытия и матрица авторизации; на этапе общей обвязки фичи не подключались (реализация ниже).
- [x] Self-review, Android Debug/Release, iOS XCFramework/Swift и test verification; фактические результаты записаны.

## Структура и понятность API

- [x] Разделить публичные типы по файлам и тематическим пакетам.
- [x] Переименовать декларации и обновить всех потребителей без compatibility aliases.
- [x] Общая матрица полей контекста и отдельные ограничения схемы.
- [x] Подтвердить тесты, Android-сборки, XCFramework и Swift обеих платформ.

## Авторизация

- [x] Матрица 17 typed events и annotated schema module.
- [x] Login Email/Google/Apple, registration и session check operations.
- [x] Profile/data/push stages отдельно от authentication success.
- [x] Typed cancellation, explicit stages, original UI errors.
- [x] Saved flow, referrer/reason, leaf views, explicit transitions/exits.
- [x] Self-review и fixes, local fake/schema/privacy/restoration fixtures.
- [x] Итоговые Android/iOS tests, apps, lint и evidence.

## Фасад авторизации

- [x] Один AuthAnalytics вместо Analytics/FlowContext/AuthFlow в экранных классах.
- [x] Четыре sealed семейства событий; компактные смысловые группы в internal.
- [x] Перенести session restoration/error policy внутрь фасада без изменения поведения.
- [x] Сохранить fixtures 17 wire schemas и добавить проверки facade boundaries.
- [x] Self-review, Android/iOS tests, app builds, lint и final evidence.

## Глубокий аудит авторизации

- [x] Прочитать facade/navigation/events/schema/operations/failure/types/validation и все consumers.
- [x] Проверить вызываемые use cases, route/root wiring, lifecycle binding и operation executor.
- [x] Убрать независимый failure stage подготовки, добавить operation-owned stage validation.
- [x] Удалить две stateless operation wrappers и объединить domain preparation policy.
- [x] Проверять mode фасада до domain calls/events; исправить event family naming и formatting tails.
- [x] Перенести JSON dependency в tests; production использует serialization core.
- [x] Проверить schema identity; сохранить coroutine cancellation/fatal на OAuth boundaries.
- [x] Удалить unused Apple provider dependencies/constructor args и проверить изолированный DI.
- [x] Final tests, Android/iOS apps, lint/header checks и evidence.

## Упрощение подготовки авторизации

- [x] Удалить AuthorizationPreparationExecution/Stage/Outcome и telemetry callbacks из feature-api.
- [x] Сохранить бизнес-порядок и ошибки обычных Login/Register prepare().
- [x] Измерять подготовку целиком через единый фасад; убрать поэтапные payload tails.
- [x] Обновить coverage, wire compatibility decision и skill/стандарт.
- [x] Завершить финальные Android/iOS тесты, сборки, lint и self-review.

## Отдельный feature analytics layer

- [x] Добавить analytics-api и authorization implementation через convention plugins.
- [x] Перенести events/matrices/state/timing; исключить UI/domain/services dependencies.
- [x] Подключить generated modules, UI classifier и typed route/form/state bindings.
- [x] Оставить business blocks и session routing у UI, обновить coverage/standard/skill.
- [x] Завершить self-review и финальные Android/iOS tests/builds/lint.


## Один consumer entry point на фичу

- [x] Вложить сценарии и feature contracts в AuthAnalytics.
- [x] Удалить отдельный public Factory и shared scenario mode/casts.
- [x] Перевести UI/StateKeeper/DI/tests, сохранить wire/state compatibility.
- [x] Обновить standard, skill и coverage matrix.
- [x] Финальный self-review и Android/iOS verification (`auth-entrypoint-review.md`).

## Повторный глубокий аудит AuthAnalytics

- [x] Проверить API, operation outcomes, cancellation, privacy, wiring и lifetime.
- [x] Сохранить analytics scenario вместе с retained ViewModel; проверить recreation/save и FlowId mismatch.
- [x] Обновить coverage, standard и canonical skill.
- [x] Финальные Android/iOS проверки и отчёт (`auth-deep-review.md`).

## Запросы у владельцев авторизации

- [x] Удалить SessionRestoration/SessionPreparation; вернуть запросы в ViewModel/use cases.
- [x] Проверить другие выносы и обновить standard, skill и coverage.
- [x] Перевести проверки Splash на фактическую ViewModel и её navigation/error pipeline.
- [x] Финальные Android/iOS tests/builds (`authorization-request-ownership-review.md`).

## Соразмерная аналитика авторизации

- [x] Убрать per-field rejection и измерение справочников вместе с API/types/schema/calls.
- [x] Убрать AuthExecution, generic preparation callback и недопустимые уже не существующие modes.
- [x] Сократить UI integration до state ownership и annotated classifier module.
- [x] Обновить fixtures, tests, coverage, standard и skill; повторный static self-review.
- Сборки/tests не запускались по запросу пользователя; отчёт authorization-simplification-review.md.

## Типизированный trackOperation — 2026-10-12

- [x] Ввести AuthOperation и перегрузки с точными Unit/Boolean/SessionResult.
- [x] Перевести Login/Register/Splash вызовы, сохранить владельцев бизнес-запросов.
- [x] Удалить Provider/Server обвязку, stage exceptions и runtime stage matrix.
- [x] Версионировать auth_attempt_failed v2, актуализировать fixtures/coverage.
- [x] Переименовать retention-файл без изменения сохранённых ключей и состояния.
- [x] Проверить DI; оставить существующий единственный binding-модуль.
- [x] Обновить skill, стандарт, integration guide; провести self-review и static checks.
- [ ] Compilation/KSP/Android/iOS tests: не запускать по текущему указанию пользователя.

Отчёт: authorization-operation-review.md.

## Retention внутри компонентов — 2026-10-12

- [x] Перенести создание и сохранение Authorization/Session в private createAnalytics владельцев.
- [x] Удалить AuthAnalyticsRetention.kt и общие extensions без новых базовых классов.
- [x] Сохранить ключи StateKeeper/InstanceKeeper, instance identity и FlowId invariant.
- [x] Адаптировать snapshot тесты и актуализировать skill/стандарт/coverage.
- [x] Self-review и статические проверки; Android/iOS builds/tests не запускать.

## Удаление custom retention holders — 2026-10-12

- [x] Удалить RetainedAnalytics и companion objects у владельцев авторизации.
- [x] Использовать retainedSimpleInstance (retainedInstance требует InstanceKeeper.Instance).
- [x] Сохранить прежние keys как private file-level const val, save/FlowId/order.
- [x] Проверить исходники Essenty, обновить skill/стандарт, выполнить static audit.
- [ ] Android/iOS builds/tests не запускать по текущему указанию.

## UI AnalyticsProvider — 2026-10-12

- [x] Переименовать scenario variables/parameters authAnalytics → analytics.
- [x] Скрыть UI retention/mapping/keys в internal AnalyticsProvider в корне фичи.
- [x] Подключить stateless provider через существующий annotated @Single DI-модуль.
- [x] Сохранить type retention, persistence format, FlowId и child/VM identity.
- [x] Добавить actual provider recreation/restore/mapping tests и DI resolution assertion.
- [x] Обновить skill/standard/guide/coverage; self-review и static checks.
- [ ] Android/iOS compilation/builds/tests не запускать по текущему указанию.

Отчёт: authorization-provider-review.md.

## Канонический skill интеграции — 2026-10-12

- [x] Переписать feature-analytics как связный текущий стандарт, убрать накопленные старые варианты.
- [x] Добавить linked reference UI provider с DI/retention/order/keys/classifier и примерами.
- [x] Закрепить analytics naming, analytics/ folder, one type/file, private companion keys.
- [x] Сохранить строгий feature API, внутренние матрицы, typed operations и границы слоёв.
- [x] Синхронизировать UI/DI skills, CLAUDE, стандарт и invocation metadata.
- [x] Self-review по current source; YAML/link/fence/example checks и git diff --check.
- [x] App builds/tests не запускать: изменение документационное и сохраняет user restrictions.

## Типизированные переходы — 2026-10-12

- [x] Ввести sealed AuthTransition в analytics-api/models.
- [x] Заменить public openRegistration/backToLogin/exit на trackTransition.
- [x] Перевести owner calls и navigation/provider/integration tests без aliases.
- [x] Сохранить event wire semantics, snapshot и exit dedup.
- [x] Закрепить transition standard в canonical skill/reference/guide/coverage.
- [x] Self-review + static source/YAML/docs checks; git diff --check.
- [ ] Android/iOS compilation/builds/tests не запускать по действующему указанию.

## Повторный аудит авторизации/аналитики — 2026-10-12

- [x] Перепроверить API/provider/transitions/error/lifecycle/DI/dependencies и skills.
- [x] Исправить EntryPoint wire fixture с несуществующим analyticsCode property.
- [x] Дедуплицировать transitions в текущий шаг и не менять flow после exit.
- [x] Добавить regression test, удалить unused dependencies/imports и formatting tails.
- [x] Консолидировать coverage и актуализировать transition/static-check правила skill.
- [x] Повторный self-review и static source/docs/YAML checks; git diff --check.
- [ ] Android/iOS builds/tests/compilation/KSP не запускать по текущему указанию.

Отчёт: authorization-final-static-review.md.

- [x] Вернуть цельные LoginUseCase.loginEmail/loginGoogle/loginApple и private prepare; убрать Preparation.Login, синхронизировать API/матрицу/тесты/стандарт. Статические проверки; сборки и тесты не запускались.

- [x] Отвязать Splash от моделей результата аналитики; удалить outcomes/stages, обновить схемы/тестовые исходники/скилл/стандарт. Статические проверки; сборки и тесты не запускались.

- [x] Изолировать три связи: цельная регистрация, screen-api FlowId с boundary mapping, reporting Exceptions не блокируют операции/переходы/lifecycle. Обновлены скиллы/стандарт/coverage; статические проверки прошли. Без билдов/тестов.

- [x] Повторный аудит registration/FlowId/reporting isolation: убран лишний flow-параметр, усилены тестовые исходники и проверены хвосты. Только статические проверки, без сборок/тестов.

- [x] Переименовать UI-провайдер в AuthAnalyticsProvider с provideAuthorization/provideSession; выделить private mapEntryPoint, синхронизировать DI/вызовы/скиллы/стандарт/тестовые исходники. Без сборок/тестов.

- [x] Общий foundation RetainedAnalytics для persisted scenarios; AuthAnalyticsProvider подключён, api serialization-core и тестовые исходники/скиллы/стандарт обновлены. Статические проверки, без сборок/тестов.

- [x] ScreenAnalytics API/internal implementation/annotated DI; удалить extensions, обновить leaf calls/тестовые исходники/скиллы/стандарт. Статические проверки, без сборок/тестов.

## Полный аудит и платформенная проверка — 2026-10-12

Последний запрос пользователя разрешил полное тестирование после прежних ограничений.
Предыдущие пункты «не запускать» выше относятся к своим историческим проходам.

- [x] Self-review и независимое ревью API/core, providers, retention/lifecycle, авторизации и Android/Swift wiring; повторный проход после исправлений.
- [x] Исправить обязательный navigation reason в DI тесте, унифицировать test convention и явно отметить coroutines inheritance opt-in.
- [x] Проверить полные последовательности use cases, ошибки на каждом запросе и реальный CredentialViewModel с работающим/падающим reporting.
- [x] Консолидировать canonical skill/standard/guide/coverage, актуализировать UI/DI/data/build references и testing rules.
- [x] Проверить скилл на будущем сценарии, YAML/metadata, примеры, ссылки и code fences.
- [x] Выполнить все существующие Kotlin test suites: Android 294, iOS 203; failures/errors/skipped = 0.
- [x] Android Debug/Release, R8, lint Debug/Release; Shared Debug/Release XCFramework.
- [x] Xcode unsigned Debug Simulator/device и Release Simulator; повторить Messaging tests после последней правки.
- [x] Финальные source boundary/obsolete-symbol checks и git diff --check; зафиксировать warnings/limits без Firebase sends/sign-in/uploads.

Отчёт: [full-review-verification.md](full-review-verification.md).
