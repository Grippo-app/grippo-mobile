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

## Читаемость ViewModel — 2026-10-12

- [x] Упростить чтение Login/Credential/Splash, сохранив цельные операции, SDK-ошибки и выбор направления по бизнес-результату.
- [x] Оставить muscle → equipment напрямую в Splash init/safeLaunch; развернуть непустые Kotlin-блоки.
- [x] Почистить профильные ViewModel авторизации и профиля; убрать промежуточные коллекции и mutable-копию для toggle без изменения правил выбора.
- [x] Удалить повторную обработку save result в TrainingCompleted; развернуть блоки TrainingRecording без изменения исполняемого содержимого.
- [x] Обновить Kotlin style, UI/analytics skills, стандарт и coverage; проверить YAML, 19 локальных ссылок и Markdown fences.
- [x] Self-review и независимый повторный review; git diff --check.
- [x] Android host tests: authorization 25, profile 3, training 20; iOS simulator authorization tests 25; failures/errors/skipped = 0.
- [x] iOS simulator compilation authorization/profile/training; повторить training tests/compilation после последней правки форматирования.

Первая проверка остановилась на внутренней ошибке incremental Kotlin compiler
в compileAndroidHostTest. Очистка только build-результатов authorization и повтор
прошли успешно; Gradle-настройки не менялись. Полные приложения, Firebase sends и
SDK sign-in в этом проходе не проверялись.

## Единая обработка ошибок ViewModel — 2026-10-12

- [x] Проверить все 58 feature/root/dialog ViewModel и общий BaseViewModel; убрать локальные catches и Result failure подмены.
- [x] Добавить чистый safeLaunch.mapError только для диалога; сохранять исходную ошибку для аналитики, logger и CrashReporting, а onError — после dismiss.
- [x] Splash повторяет полную загрузку только после закрытия ошибки; запросы остаются в init, обязательные справочники нельзя пропустить. Старый SessionCheckFailed остаётся только для saved routes.
- [x] Login сохраняет SDK presentation mapping без локального catch; TrainingCompleted использует исходную ошибку общего pipeline, лишний StringProvider и три строковых ресурса удалены.
- [x] Обновить UI/analytics skills, error pipeline, стандарт и coverage; убрать старые carve-outs локальной обработки ошибок.
- [x] Self-review, независимое повторное review, XML/YAML/Markdown проверки и git diff --check.
- [x] Android host tests: foundation 24, analytics authorization 19, UI authorization 27, training 20; iOS simulator: foundation 13, analytics authorization 19, UI authorization 27. Failures/errors/skipped = 0.
- [x] Android/iOS simulator compilation shared со всеми потребителями BaseViewModel; Gradle BUILD SUCCESSFUL (5m 42s).

Первые проверки выявили недостающий coroutines-test в foundation commonTest и
конфликт имени переменной в Splash test fixture; оба исправлены. Проверка SDK
sign-in, Firebase sends и полные app builds не входят в этот проход.

Команда: `./gradlew :ui-core:foundation:testAndroidHostTest :ui-core:foundation:iosSimulatorArm64Test :analytics:authorization:testAndroidHostTest :analytics:authorization:iosSimulatorArm64Test :ui-screen-features:authorization:testAndroidHostTest :ui-screen-features:authorization:iosSimulatorArm64Test :ui-screen-features:training:testAndroidHostTest :ui-screen-features:training:compileKotlinIosSimulatorArm64 :shared:compileAndroidMain :shared:compileKotlinIosSimulatorArm64 --offline`.
Лог: `/tmp/viewmodel-error-pipeline-verification.log`. Сохранились прежние warnings
global opt-ins, expect/actual beta и deprecated BackHandler; новых настроек сборки
или подавления warnings не добавлялось.

## Уточнение поведения Splash — 2026-10-12

Пользователь уточнил: ошибка загрузки профиля должна немедленно возвращать на
вход без диалога. Этот пункт заменяет соответствующее поведение предыдущего
прохода; `SessionCheckFailed` снова имеет активный источник в Splash.

- [x] Добавить узкий foundation `withErrorRecovery`, без нового scope/job: обычное Exception возвращает бизнес-fallback, CancellationException/Error и ошибка recovery распространяются.
- [x] В Splash восстановление охватывает только профиль; токен и обязательные muscle → equipment сохраняют общий pipeline и повтор после закрытия диалога. Запросы справочников остаются в init/safeLaunch.
- [x] Бизнес возвращает SplashDirection; аналитика остаётся декоратором и фиксирует completed execution при обработанном fallback. Формат событий и версии схем не меняются.
- [x] Обновить UI/analytics skills, validation rules, стандарт, guide, coverage и точный BaseViewModel drop-in.
- [x] Добавить тесты прямого профильного fallback, observeUser failure, cancellation/destruction и foundation recovery; сохранить token/reference dialog/retry checks.
- [x] Self-review и независимый повторный review; YAML/links/fences/drop-in checks и git diff --check.
- [x] Android host tests: foundation 28, analytics authorization 19, UI authorization 30; iOS simulator: foundation 17, analytics authorization 19, UI authorization 30. Failures/errors/skipped = 0.
- [x] Android/iOS simulator compilation shared; Gradle BUILD SUCCESSFUL (8m 47s, 1988 tasks).

Команда: `./gradlew :ui-core:foundation:testAndroidHostTest :ui-core:foundation:iosSimulatorArm64Test :analytics:authorization:testAndroidHostTest :analytics:authorization:iosSimulatorArm64Test :ui-screen-features:authorization:testAndroidHostTest :ui-screen-features:authorization:iosSimulatorArm64Test :shared:compileAndroidMain :shared:compileKotlinIosSimulatorArm64 --offline`.
Лог: `/tmp/splash-profile-recovery-verification.log`.

Сохранились прежние предупреждения global opt-ins, expect/actual beta и deprecated
BackHandler. Firebase delivery/sign-in/uploads и полные app builds не выполнялись.

## Два запуска Splash — 2026-10-12

Пользователь выбрал два `safeLaunch` и упрощение SessionCheck ради чистоты кода.
Этот проход заменяет предыдущий вариант с `withErrorRecovery`.

- [x] Удалить helper из BaseViewModel, Splash, тестов и активных инструкций.
- [x] Добавить optional `safeLaunch.handleError` для явной немедленной обработки обычных исключений без log/crash/dialog; сохранить стандартное поведение по умолчанию и post-dismiss `onError`.
- [x] Сохранить cancellation; fatal Error из операции обходит custom handler/mapError, а обычная ошибка handler идёт через стандартный pipeline.
- [x] Разделить token/no-session/reference и profile launches в init; сохранить последовательные обязательные muscle → equipment, обычный retry после dismiss и прямой профильный возврат на вход.
- [x] Использовать разные AppContent/Profile loaders и отображение любого активного loader.
- [x] SessionCheck декорирует только бизнес-hasProfile при имеющемся token; профильная ошибка даёт failed до UI fallback. Token/no-session/reference paths не создают session events.
- [x] Поднять четыре SessionCheck schemas до v3 и обновить fixture; явно разделять прежние и новые population/duration/terminal outcomes в отчётах по schema_version.
- [x] Обновить UI/analytics skills, error rules, стандарт, guide, coverage и точный BaseViewModel drop-in; проверить YAML/39 links/fences.
- [x] Self-review и независимый повторный review API, error policies, loaders, lifecycle, tests и документации.
- [x] Android host tests: foundation 29, analytics authorization 19, UI authorization 30; iOS simulator: foundation 18, analytics authorization 19, UI authorization 30. Failures/errors/skipped = 0.
- [x] Android/iOS simulator shared compilation и финальный diff-check; Gradle BUILD SUCCESSFUL (6m 47s, 1984 tasks).

Первый gate остановился на выводе типа setOf в новом тесте loader; явно задан
общий SplashLoader. Production-код и настройки сборки из-за этого не менялись.
Первый лог: `/tmp/splash-two-launches-verification.log`.
Повторный лог: `/tmp/splash-two-launches-verification-final.log`.

Команда: `./gradlew :ui-core:foundation:testAndroidHostTest :ui-core:foundation:iosSimulatorArm64Test :analytics:authorization:testAndroidHostTest :analytics:authorization:iosSimulatorArm64Test :ui-screen-features:authorization:testAndroidHostTest :ui-screen-features:authorization:iosSimulatorArm64Test :shared:compileAndroidMain :shared:compileKotlinIosSimulatorArm64 --offline`.
Сохранились прежние global opt-in/expect-actual/BackHandler warnings. Firebase
delivery/sign-in/uploads и полные app builds не выполнялись.

## Область функций и компактные лямбды — 2026-10-12

Этот проход уточняет прежнее правило форматирования: короткие state updates и
простые Flow transformations не нужно разворачивать. Named local functions
запрещены; callbacks/DSL остаются разрешены.

- [x] Перенести Splash loadContent/loadProfile на уровень private методов класса; init вызывает loadContent, повтор использует тот же метод. Сохранить обе error policies, loaders, последовательность запросов и SessionCheck v3.
- [x] Удалить локальную обёртку finish из DialogContentComponent; вызывать существующий dialogNavigation.finish напрямую, сохраняя отложенные results.
- [x] Свернуть короткие update/Flow лямбды в foundation и 11 затронутых ViewModel; 54 правки ViewModel отличаются от исходников только whitespace.
- [x] Обновить Kotlin/UI/analytics skills, validation rules, стандарт, guide и coverage; убрать прежний carve-out для локальной startup-функции.
- [x] Self-review и независимый повторный review поведения Splash, dialog callbacks и форматирования; YAML, 44 ссылки, fences и точный BaseViewModel drop-in проверены.
- [x] Android host tests: foundation 29, UI authorization 30, profile 3, training 20, shared 24 — всего 106; iOS simulator: foundation 18, UI authorization 30 — всего 48. Failures/errors/skipped = 0.
- [x] Android/iOS simulator compilation shared и затронутых модулей; Gradle BUILD SUCCESSFUL (1m 23s, 2117 tasks); git diff --check.

Команда: `./gradlew :ui-core:foundation:testAndroidHostTest :ui-core:foundation:iosSimulatorArm64Test :ui-screen-features:authorization:testAndroidHostTest :ui-screen-features:authorization:iosSimulatorArm64Test :ui-screen-features:profile:testAndroidHostTest :ui-screen-features:training:testAndroidHostTest :shared:testAndroidHostTest :shared:compileKotlinIosSimulatorArm64 --offline`.
Лог: `/tmp/analytics-local-functions-verification.log`.

Изменённый UI/foundation/shared scope проверен на локальные объявления функций;
это не массовая миграция остальных старых модулей. Сохранились прежние
global opt-in/expect-actual/BackHandler warnings. Firebase sends/sign-in/uploads
и полные app builds в этом проходе не выполнялись.

## Все ViewModel: структурная чистота — 2026-10-12

- [x] Прочитать все 59 production ViewModel-файлов (58 конкретных и BaseViewModel): 23 dialog, 21 screen вне authorization, 11 authorization, 3 shared, 1 foundation.
- [x] Подтвердить отсутствие named local functions; companion libraryResultKey и обычные callbacks не являются локальными объявлениями. Не вводить новые helper layers.
- [x] Почистить 19 файлов: короткие state updates/Flow predicates компактны, business/control/multi-operation blocks развёрнуты; сложные атомарные updates не разбивать.
- [x] Удалить пустой Root connectivity observer/handler и его constructor injection; сохранить Home → deeplink → clear порядок навигации. Connectivity module не удалять.
- [x] Убрать одноразовый notification forwarding helper, устаревший FlowPreview opt-in и 7 init-only constructor properties; сохранить параметры и существующее DI.
- [x] Упростить foundation update/Flow return/block forwarding; полностью удалить no-op processing только у блочного safeLaunch. Flow lifecycle modes и общий error/loader/cancellation pipeline сохранить.
- [x] Обновить API в CLAUDE и base-classes; проверить 10 локальных ссылок, fences и точный BaseViewModel drop-in.
- [x] Self-review и независимое ревью всех 59 исходников/19 изменённых файлов относительно снимка начала прохода; порядок запросов, результатов, callbacks и error policies сохранён.
- [x] Android host tests: foundation 29, authorization 30, exercises 10, main 11, profile 3, training 20, shared 24 — всего 127. iOS simulator: foundation 18, authorization 30 — всего 48. Failures/errors/skipped = 0; training reports новее последней source правки.
- [x] Android Debug assemble.
- [x] iOS Shared Debug XCFramework (simulator + device) и финальный diff-check; Gradle BUILD SUCCESSFUL (7m 55s, 3218 tasks).

Команда: `./gradlew :ui-core:foundation:testAndroidHostTest :ui-core:foundation:iosSimulatorArm64Test :ui-screen-features:authorization:testAndroidHostTest :ui-screen-features:authorization:iosSimulatorArm64Test :ui-screen-features:exercises:testAndroidHostTest :ui-screen-features:main:testAndroidHostTest :ui-screen-features:profile:testAndroidHostTest :ui-screen-features:training:testAndroidHostTest :shared:testAndroidHostTest :androidApp:assembleDebug :shared:assembleSharedDebugXCFramework --offline`.
Лог: `/tmp/all-viewmodels-cleanup-verification.log`.

Сохранились прежние global opt-in/expect-actual/BackHandler/smallBinary warnings и
предупреждение о ненужном safe call в существующем training test. Новых Gradle
настроек или suppressions не добавлено; Firebase sends/sign-in/uploads не выполнялись.

Analytics impact: операции, transitions, attribution и payload не менялись;
authorization coverage сохраняет фактическое поведение. Рефакторинг сохраняет
бизнес- и error-семантику. Отдельные прежние необработанные Result в Home
(getTrainings, getExerciseExamples, setHomeWelcomeStatus) и StartTraining
(getTrainings) отмечены как отдельный поведенческий вопрос: изменение их error
policy не включено в этот структурный проход.

## Один запуск Splash — 2026-10-12

Этот проход заменяет прежнее решение с двумя `safeLaunch` и `handleError`.

- [x] Сделать `loadProfile` private suspend методом внутри единственного `safeLaunch`; сохранить непрерывный `AppContent` loader и последовательные запросы справочников.
- [x] Ограничить try только проверкой профиля снаружи аналитического декоратора: обычная Exception возвращает на вход без диалога, CancellationException пробрасывается. Успешная навигация, ошибки токена/справочников и fatal Error сохраняют стандартный путь.
- [x] Удалить неиспользуемые `SplashLoader.Profile` и foundation `handleError`, включая тесты удалённого API; сохранить mapError и post-dismiss onError.
- [x] Проверить профильный fallback, отмену/уничтожение, loader, token/reference dialog и retry; добавить проверки fatal профильной ошибки и отказа reporting при бизнес-успехе/ошибке.
- [x] Обновить skills, стандарт, guide и coverage. SessionCheck сохраняет профильную границу v3 и исходную ошибку до fallback; схемы/параметры не менялись.
- [x] Self-review и независимый review; YAML, 12 Markdown-файлов, 44 локальные ссылки, fences, точный BaseViewModel drop-in и Splash example, отсутствие устаревших active API и финальный diff-check.
- [x] Android host tests: foundation 25, UI authorization 32 — всего 57. iOS simulator: foundation 14, UI authorization 32 — всего 46. Failures/errors/skipped = 0.
- [x] Android/iOS simulator shared compilation; Gradle BUILD SUCCESSFUL (3m 5s, 1977 tasks).

Команда: `./gradlew :ui-core:foundation:testAndroidHostTest :ui-core:foundation:iosSimulatorArm64Test :ui-screen-features:authorization:testAndroidHostTest :ui-screen-features:authorization:iosSimulatorArm64Test :shared:compileAndroidMain :shared:compileKotlinIosSimulatorArm64 --offline`.
Лог: `/tmp/splash-single-launch-verification.log`.
Сохранилось прежнее предупреждение deprecated BackHandler. Полные app builds и
Firebase sends/sign-in/uploads в этом проходе не выполнялись.

## Splash: стандартная обработка ошибки профиля — 2026-10-12

Пользователь выбрал убрать локальный catch и обрабатывать ошибку профиля через
обычный `safeLaunch`. Этот проход заменяет прежний немедленный возврат на вход.

- [x] Удалить try/catch и import CancellationException из Splash; оставить suspend loadProfile, единственный safeLaunch/AppContent и последовательные muscle → equipment для no-session пути.
- [x] Начальная загрузка и post-dismiss повтор используют один private class-level loadContent; каждый повтор заново читает token. Успешный бизнес-Boolean выбирает Home/CreateProfile, ошибка проходит logger → CrashReporting → ErrorProvider.
- [x] Аналитика остаётся декоратором профильной операции: исходная ошибка даёт failed, повтор получает новый OperationId в прежнем session flow. Schema v3/payload не менялись.
- [x] Сохранить SessionCheckFailed в сериализуемых routes/API и bounded wire codes для восстановления legacy state; текущий Splash этот вход не создаёт.
- [x] Обновить тесты remote/cache-flow ошибок, исходного crash/dialog error и deferred retry; проверить повтор после потери token, fresh paired operation IDs, callback после destroy, reporting failures, cancellation и fatal errors.
- [x] Обновить 12 skills/standard/coverage документов, удалить прежнее разрешение локального UI catch. Единый onError/mapError контракт обеих перегрузок сохранён.
- [x] Self-review и независимый review; YAML, 44 локальные ссылки, Markdown fences, точный BaseViewModel drop-in/Splash example, отсутствие local catch/active legacy entry producer и git diff --check.
- [x] Android host tests: foundation 25, UI authorization 34 — всего 59. iOS simulator: foundation 14, UI authorization 34 — всего 48. Failures/errors/skipped = 0.
- [x] Android/iOS simulator shared compilation; Gradle BUILD SUCCESSFUL (3m 22s, 1977 tasks).

Команда: `./gradlew :ui-core:foundation:testAndroidHostTest :ui-core:foundation:iosSimulatorArm64Test :ui-screen-features:authorization:testAndroidHostTest :ui-screen-features:authorization:iosSimulatorArm64Test :shared:compileAndroidMain :shared:compileKotlinIosSimulatorArm64 --offline`.
Лог: `/tmp/splash-standard-error-verification.log`.

Прежние global opt-in/expect-actual/BackHandler warnings сохранены. TokenProvider
refresh/invalidation и неоднозначный getUser Boolean контракт не менялись; их
ранее найденные проблемы отмечены в coverage как отдельная работа. Полные app
builds и Firebase sends/sign-in/uploads в этом проходе не выполнялись.

## Очистка восстановленных файлов из stash — 2026-10-12

Пользователь подтвердил, что из stash случайно вернулся устаревший код, и
попросил удалить его по всему затронутому дереву без Android/iOS билдов.

- [x] Удалить `SessionRestoration`, старый `SessionPreparation` и тест прежней initial-data обвязки. Сохранить текущие запросы Splash и цельные Login/Register use case.
- [x] Удалить прежний `analytics/feature-api`, плоские дубли implementation, старые preparation/stage/outcome модели и UI bindings/provider/state. Сохранить `analytics-api`, internal implementation folders, `AuthAnalyticsProvider` и актуальные тесты интеграции.
- [x] Удалить старые `PlatformServicesModule/Runtime`, `shared/main`, устаревшие toolkit API/config/locks/tests, неиспользуемые Profile/Main helpers и прежние navigation resources. Сохранить актуальную main shell, internal expect/actual и Android/Swift bootstrap wiring.
- [x] Исправить внутреннюю опечатку `MussingEquipments`; проверить оставшиеся точечные изменения читаемости. Не менять бизнес-логику, route identity, StateKeeper keys, snapshot SerialName, wire-коды или версии схем.
- [x] Self-review и независимый review всех 67 удалений: 2143 строки устаревшего кода/тестов/ресурсов. Проверить актуальных потребителей, замены и DI перед удалением.
- [x] Статические проверки: нет импортов или неразрешённых same-package ссылок на удалённые типы; нет duplicate top-level типов в затронутых module/source sets; все 94 включённых Gradle-модуля имеют build files, literal project accessors разрешаются в settings.
- [x] Проверить границы analytics/API и screen-api, отсутствие старых символов в production/test source и действующих skills/standard/coverage; просмотреть 11 authorization ViewModel и актуальные provider/Splash тесты. Scan 59 ViewModel не нашёл локальных именованных функций; найденный `libraryResultKey` — метод companion, он сохранён.
- [x] Финальный `git diff --check` без ошибок. Скиллы и стандарт уже описывают текущую структуру; новых правил или примеров устаревшей реализации в них нет.

Статический отчёт: `/tmp/stash-cleanup-static-verification.json`.
В этом проходе Gradle, Android/iOS сборки и тестовые раннеры не запускались.
Результаты предыдущих запусков выше относятся к предыдущим изменениям, а не
подтверждают компиляцию после этой очистки. Действующие cancellation guards
toolkit cores не удалялись как неиспользуемый код: они относятся к текущему
контракту отмены и требуют отдельного поведенческого изменения.

## Авторизация и аналитика после очистки stash — 2026-10-12

- [x] Заново просмотреть DI/API/scenarios/schema, Auth/Splash/Login/Credential/ProfileCreation routes/components, все 11 authorization ViewModel и цельные Login/Register use case. Проверить annotated bindings, public API dependencies и shared `.module` enumeration по исходникам.
- [x] Упростить одноэлементные childStack через `initialConfiguration`; убрать повторное хранение analytics/input arguments в leaf/completion components. По локальному Decompose source это тот же restore/save path.
- [x] Удалить мёртвый `ToBodyWithName` и нечитавшиеся payloads ProfileCreation Directions. Бизнес-данные остаются в immutable VM state; typed callbacks, router destinations и snapshot wire shape не меняются.
- [x] Удалить вредный fallback «пустой выбор → выбрать всё» в мышцах/оборудовании. Reconciliation выполняется атомарно в update: снятые отметки сохраняются, отсутствующие ID удаляются, новые ID выбираются.
- [x] Ограничить login/register одним активным submit через существующие loaders. Блокировать cross-method/repeated clicks и переход к регистрации во время входа; согласовать LoginScreen Loading/Disabled без новых flags/jobs/helpers.
- [x] Убрать четыре manual ensureActive из общего operation decorator. Он исполняет block напрямую, сохраняет T и исходную business exception; выброшенная CancellationException даёт interrupted и пробрасывается. Явно снять старое обещание pre-gate уже отменённого caller; отменённый Job остаётся отменённым, даже если несуспендящий блок вернул T. Это намеренное выравнивание с business-decoration правилом, не механическое сохранение прежних guard fixtures.
- [x] Обновить contract/tests источники: direct cancellation identity, cancelled Job + non-suspending block result, completion-reporting cancellation; сохранить suspension cancellation, original failures, reporting/fatal isolation и paired IDs. Добавить четыре actual-VM regression cases для профильного выбора/state и повторной регистрации. Убрать local named helper из RetainedAnalyticsTest.
- [x] Сохранить legacy SessionCheckFailed enum/mapping и пояснить его роль; текущий Splash его не создаёт. Splash error/retry policy, use-case request order, StateKeeper keys, serializers, 12 custom schemas и версии не изменены.
- [x] Актуализировать feature-analytics skill, стандарт, integration guide и coverage для точной границы cancellation и новых business safeguards/omissions; новые events не добавлены.
- [x] Self-review + независимые review loops после исправлений: operation decorator/fixtures, атомарный выбор, lifecycle activation/generics, Directions/state ownership и loaders/UI согласованы по реальным контрактам.
- [x] Статические проверки: duplicate top-level types по module/source set = 0; неизвестные literal Gradle project accessors = 0; все 94 included modules существуют; 27 schema/event/model/route files побайтно совпадают с baseline. Provider mapping/keys неизменны; removed-symbol scans и relevant production scope/ensureActive/local-function scans чистые.
- [x] Проверить 19 локальных Markdown links, balanced fences и неизменённый YAML frontmatter; финальный `git diff --check` без ошибок.

Статический отчёт: `/tmp/auth-post-stash-static-verification.json`.
Gradle, Android/iOS builds, Kotlin test runners, KSP regeneration и Firebase sends
в этом проходе не выполнялись. Новые regression sources ещё не скомпилированы и
не запускались; статический review не подменяет эти проверки.
`quick_validate.py` недоступен из-за отсутствия PyYAML в system/bundled Python;
зависимости не устанавливались, frontmatter не менялся и проверен against baseline.
Отдельная transport cancellation policy сервисных workers не менялась: она
различает CancellationException активного SDK sink и реальную отмену worker.
Оставшиеся worker guards вне operation decorator требуют отдельного пересмотра
этого контракта; этот проход не утверждает отсутствие ensureActive во всём toolkit.
