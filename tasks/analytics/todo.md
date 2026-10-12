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
- [x] Проверить полные последовательности use cases, ошибки на каждом запросе и реальный RegistrationViewModel с работающим/падающим reporting.
- [x] Консолидировать canonical skill/standard/guide/coverage, актуализировать UI/DI/data/build references и testing rules.
- [x] Проверить скилл на будущем сценарии, YAML/metadata, примеры, ссылки и code fences.
- [x] Выполнить все существующие Kotlin test suites: Android 294, iOS 203; failures/errors/skipped = 0.
- [x] Android Debug/Release, R8, lint Debug/Release; Shared Debug/Release XCFramework.
- [x] Xcode unsigned Debug Simulator/device и Release Simulator; повторить Messaging tests после последней правки.
- [x] Финальные source boundary/obsolete-symbol checks и git diff --check; зафиксировать warnings/limits без Firebase sends/sign-in/uploads.

Отчёт: [full-review-verification.md](full-review-verification.md).

## Читаемость ViewModel — 2026-10-12

- [x] Упростить чтение Login/Registration/Splash, сохранив цельные операции, SDK-ошибки и выбор направления по бизнес-результату.
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

- [x] Заново просмотреть DI/API/scenarios/schema, Auth/Splash/Login/Registration/ProfileCreation routes/components, все 11 authorization ViewModel и цельные Login/Register use case. Проверить annotated bindings, public API dependencies и shared `.module` enumeration по исходникам.
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

## Унифицированные показы и действия авторизации — 2026-10-12

- [x] Разделить владельцев: граф удерживает сценарий и сообщает реальные переходы; child factory связывает lifecycle каждого leaf через stateless AuthAnalyticsProvider. Все 8 leaf подключены, AuthProcess/ProfileCreation containers не создают views. Leaf components больше не получают lifecycle/retention services.
- [x] Добавить закрытые семейства trackInteraction рядом с trackTransition/trackOperation: 35 вариантов по экранам, 44 реальных on*-callbacks восьми leaf ViewModel. Ввод, focus/clear/IME, password visibility, submit/control intent, picker open/confirmation, experience и selection описаны типизированными фактами.
- [x] Оставить design-system callbacks нейтральными и optional; сохранить focus/keyboard/clear поведение. UI использует прежнюю validation, аналитика получает только bounded status/action/enum/count. Text status повтор дедуплицируется внутри сценария до кодирования; discrete clicks/picker confirmations не дедуплицируются. Raw values/length/body metrics/entity IDs не отправляются.
- [x] Добавить самостоятельный retained ProfileCreation scenario и snapshot: отдельный flow, пять leaf steps, Forward/Back/Exit с внутренним кодированием Action/Back. Системный Back проходит через существующего владельца; неизвестный origin остаётся absent. Сохранить прежние auth/session keys, AuthorizationState wire shape и screen-api routes.
- [x] Декорировать целые CreateProfile/Logout блоки без изменения бизнес-оркестрации. Navigation/results/retry/error handling остаются бизнес-логикой. Programmatic Back после error-dialog dismiss не создаёт ложное нажатие пользователя.
- [x] Централизовать новые interaction/profile schemas, keys и codes во внутренних матрицах; catalog содержит 29 уникальных schemas (12 прежних + 10 interactions + 7 profile). Прежний AuthEventSchema побайтно сохранён; qualified annotated registration выполняется один раз.
- [x] Добавить source fixtures typed payload/privacy/input dedup, profile lifecycle/restore/closed transitions/operations/errors/cancellation, UI mapping и callbacks. Расширить настоящий provider/DI integration fixture и добавить ProfileCreationComponentTest для реального childStack, hardware Back, foreground и полного Logout.
- [x] Self-review и независимые review loops API/implementation и UI/provider/factories. Исправить пропущенную передачу сценария в MissingEquipmentsViewModel, исключить произвольные navigation reasons из public ProfileTransition и добавить недостающий Logout operation. После исправлений обязательных findings не осталось.
- [x] Актуализировать canonical feature-analytics skill, UI provider reference, CLAUDE, feature standard, guide, coverage/template; перечислить реальные owners и явные omissions. Остальные фичи, external SDK/system surfaces и отдельная visibility внутренних picker/error dialogs не объявлены покрытыми.
- [x] Статические проверки: 28/28 архитектурных checks; 15 legacy schema/snapshot/route files неизменны, duplicate top-level types отсутствуют, все 94 included Gradle modules имеют build files. Kotlin PSI разобрал 150 исходников: syntax errors = 0, named local functions = 0. Это синтаксический разбор, без компиляции проекта.
- [x] Проверить 11 действующих документов и 37 локальных links/anchors, YAML двух skills и metadata, Markdown fences. Исправить противоречивые старые UI/DI rules о прямом ScreenAnalytics lookup и разграничить business-model mapping с bounded UI→telemetry mapping. Финальный git diff --check без ошибок.

Отчёты: `/tmp/auth-unified-static-verification.json`,
`/tmp/auth-analytics-syntax-result.txt`,
`/tmp/analytics-standard-docs-verification.json`.
В этом проходе Gradle, KSP, Kotlin test runners, Android/iOS builds и Firebase
sends не запускались. Добавленные тесты не скомпилированы и не исполнены;
source review, PSI и статические проверки не подтверждают runtime/type correctness.
Предыдущие успешные платформенные проверки выше относятся к прежним изменениям.

## Общие контракты аналитики и тонкая авторизация — 2026-10-12

- [x] В существующем analytics-api добавить common/: три stateless контракта InteractionAnalytics/OperationAnalytics/TransitionAnalytics и шесть отдельных моделей InputAction/InputStatus/InputKeyboardAction/ControlAction/VisibilityChange/SelectionChange. Общие типы не зависят от авторизации, UI, domain или SDK и не добавляют runtime/DI слой.
- [x] Сценарии AuthAnalytics наследуют только подходящие общие контракты с закрытыми feature types. Сократить AuthInteraction с 35 до 22 вариантов: feature wrappers описывают экран/поле/специфический intent, переиспользуя общие факты. Не расширять допустимые операции и переходы сценариев.
- [x] Удалить старые authorization InputStatus/InputKeyboardAction, внутренние InteractionField/TextInput и повторные input encoders. Прямое использование закрытых CredentialField/ProfileField сохраняет wire codes.
- [x] Дедуплицировать только InputAction.Changed по экрану/полю/status. Confirmed всегда отправляется и не записывает status cache; явное подтверждение не скрывает первый наблюдаемый текстовый ввод. Cache ограничен существующими enum targets сценария.
- [x] Перевести шесть leaf ViewModel на общие факты, сохранив 44 on*-callbacks и два picker confirmations. Бизнес-операции, запросы, validation, error/retry/navigation/lifecycle и route snapshots не меняются; в события не добавлены сырые значения.
- [x] Сохранить 29 schema names, versions, keys и wire codes. Feature implementation владеет матрицами и state; common содержит контракты и безопасные факты без универсального registry/runtime.
- [x] Self-review и независимые review loops API/implementation и UI migration. После проверок обязательных findings не осталось.
- [x] Изолированно скомпилировать реальные исходники cached Kotlin 2.4.20 JVM compiler с strict explicit API и serialization plugin: toolkit API 37 файлов, common API 9, Auth API 17, authorization implementation 27 и тестовый набор 7 файлов. Gradle/KSP/platform builds не запускались.
- [x] Выполнить пять чистых JUnit classes: AuthNavigationTest/AuthOperationsTest/AuthInteractionTest/ProfileAnalyticsTest/CommonInteractionModelsTest — OK (32 tests), failures = 0. Проверены payload, Changed/Confirmed semantics, flow restoration, результат/ошибки/отмена операций и reporting isolation.
- [x] Проверить типовые границы настоящим компилятором: корректный consumer общих контрактов компилируется; четыре неверных обращения (чужое поле, чужой экран, неподходящая операция, raw String вместо статуса) отвергнуты четырьмя diagnostics.
- [x] Статические проверки 24/24: 659 защищённых business/UI/routes/lifecycle/transport/schema файлов неизменны; старые пути/конструкторы отсутствуют. Kotlin PSI: 156 файлов, syntax errors = 0, named local functions = 0.
- [x] Обновить canonical skill, UI skill/reference, CLAUDE, standard, guide и coverage/template для common-first интеграции. Проверить 11 действующих документов, 38 локальных links/anchors, YAML двух skills/metadata через Ruby Psych и Markdown fences. Финальный git diff --check без ошибок.

Команды: `python3 /tmp/auth-common-typecheck.py`, затем
`python3 /tmp/auth-common-typecheck.py --tests-only` для уточнённого тестового
набора. Первая попытка включала AuthSchemaTest и остановилась на отсутствующем
generated KSP .module; этот класс исключён целиком, без подмены DI или stubs.
Финальные пять compilation stages и 32 теста прошли. Тесты типовых границ
запущены отдельно тем же cached compiler.

Отчёты: `/tmp/auth-common-typecheck-result.json`,
`/tmp/auth-common-junit-result.json`, `/tmp/auth-common-api-typecheck/junit.log`,
`/tmp/auth-common-api-boundary-verification.json`,
`/tmp/auth-common-static-verification.json`, `/tmp/auth-common-syntax-result.txt`,
`/tmp/analytics-standard-docs-verification.json`.

Проверка не включает AuthSchemaTest/generated DI/catalog composition,
UI/foundation/Decompose/Compose/native fixtures, Gradle/KSP, Android/iOS builds
или Firebase delivery. Изолированный JVM запуск не подтверждает платформенную
интеграцию. quick_validate.py остаётся недоступен из-за отсутствия PyYAML;
зависимости не устанавливались, YAML проверен Ruby Psych.

## Повторный аудит общих контрактов и авторизации — 2026-10-12

- [x] Независимо перепроверить common API/models, три Auth сценария, матрицы/схемы, input cache, generic results/error isolation, provider/DI/routes, graph retention и все восемь leaf с 44 callbacks и двумя picker results. Обязательных изменений UI/business flow не найдено.
- [x] Исправить восстановление AuthorizationState: обе navigation slots должны принадлежать своим Login/Registration surfaces. Проверки выполняются вместе с route-flow invariant до state assignment/reporting; чужие referrers не запрещены как наблюдаемая attribution.
- [x] Защитить terminal snapshot от позднего показа другого leaf после Exit. Показ возвращает прежнюю navigation/presentation, но не меняет закрытое состояние. Не добавлять новые runtime wrappers/locks/dispatchers.
- [x] Добавить два regression tests: четыре некорректных restored snapshots и поздняя Registration activation после завершения. Независимое final review подтверждает совместимость валидных snapshots и отсутствие бизнес-привязок.
- [x] Удалить неиспользуемый ProfileStep serializer; сохранить Serializable у реально сохраняемого Step, snapshot shape, SerialName, keys и wire-каталог. Явно описать существующий serial owner access для mutable scenarios.
- [x] Отделить generated catalog test в AuthAnalyticsModuleTest от чистого AuthSchemaTest с 12 legacy wire fixtures. Убрать ставшие ненужными imports/пустые строки; не подменять generated DI. Pure fixture теперь компилируется и выполняется без KSP.
- [x] Расширенный PSI scan нашёл локальную malformed factory в AnalyticsTest. Перенести её на уровень класса без изменения test cases; заново скомпилировать и выполнить соответствующий JVM набор.
- [x] Актуализировать skill/standard для owner confinement, owned restored surfaces и стабильного terminal snapshot. Уточнить single-leaf binding без придуманного Step. Coverage хранит текущий результат проверки; история запусков остаётся в этом todo.
- [x] Прямой cached Kotlin 2.4.20 JVM compiler с strict explicit API и serialization plugin: toolkit API 37 файлов, common API 9, Auth API 17, authorization implementation 27; после разделения fixtures скомпилированы 8 файлов Auth/common tests. Дополнительно компилируются actual shared toolkit core с JVM lock, три foundation analytics исходника и соответствующие test sources.
- [x] AuthNavigationTest/AuthOperationsTest/AuthInteractionTest/ProfileAnalyticsTest/AuthSchemaTest/CommonInteractionModelsTest — OK (35 tests). AnalyticsOperationTest/FlowContextTest/AnalyticsTest/ScreenPresentationTest/AnalyticsParallelTest/AnalyticsConcurrencyTest/RetainedAnalyticsTest — OK (51 tests). После последнего test cleanup дополнительный набор выполнен снова. Всего 86 tests, оба актуальных JUnit scopes exit 0.
- [x] Negative regression check: убрать две защиты только из временной копии AuthFlow, скомпилировать её и запустить реальный AuthNavigationTest — 10 tests, ровно два ожидаемых failures. В production исходниках защиты сохранены; текущий исправленный набор зелёный. Первый matcher ожидал старое имя одного теста: исправлен по реальному исходнику и проверен на сохранённом выводе, без повторного запуска.
- [x] Финальные статические checks 36/36: 658 защищённых business/UI/routes/lifecycle/transport/schema файлов неизменны. Все source changes этого прохода ограничены согласованными fixes и test cleanup. PSI разобрал 242 исходника: syntax errors = 0, named local functions = 0.
- [x] YAML двух skills и metadata, Markdown fences, 38 локальных links/anchors в 11 действующих документах и git diff --check прошли после финальной правки coverage.

Команды: `python3 /tmp/analytics-deep-review-jvm.py`,
`python3 /tmp/analytics-deep-review-jvm.py --tests-only --auth-only`,
`python3 /tmp/analytics-deep-review-additional.py`,
`python3 /tmp/analytics-deep-review-regression.py`,
`python3 /tmp/validate-analytics-docs.py`,
`python3 /tmp/analytics-deep-review-static.py`, `git diff --check`.
PSI использует `/tmp/AuthAnalyticsSyntax.java` с cached compiler classpath и
`/tmp/analytics-deep-review-syntax-input.txt`.

Отчёты: `/tmp/analytics-deep-review-compilation.json`,
`/tmp/analytics-deep-review-auth-tests.json`,
`/tmp/analytics-deep-review-additional-tests.json`,
`/tmp/analytics-deep-review-regression-result.json`,
`/tmp/analytics-deep-review-static-result.json`,
`/tmp/analytics-deep-review-syntax-result.txt`,
`/tmp/analytics-standard-docs-verification.json`.
JUnit logs: `/tmp/analytics-deep-review-jvm/junit.log` и
`/tmp/analytics-deep-review-jvm/additional-tests.log`.

Gradle/KSP, generated module/config/catalog composition tests,
ScreenAnalyticsTest с generated DI dependency, actual UI component/ViewModel/Compose
fixtures, native SDK adapters, Android/iOS builds и Firebase delivery не запускались.
RetainedAnalyticsTest использует настоящую реализацию и cached Essenty/Decompose
bytecode в обычной JVM; это не платформенный UI runtime. Существующий контракт
отмены сервисного worker не менялся. quick_validate.py по-прежнему недоступен
без PyYAML; зависимости не устанавливались, YAML проверен Ruby Psych.

## Строгая иерархия аналитики и input API — 2026-10-12

- [x] Независимо сверить common contracts/facts → feature scenario API/consumer descriptions → internal event families/matrices/schemas. Public descriptions не реализуют AnalyticsEvent и не раскрывают schemas/keys; новое runtime/DI обобщение не требуется.
- [x] Закрыть недопустимые input combinations типами: InputAction.Text включает Changed/Focused/Cleared/Keyboard, Confirmed находится вне Text; Login/Registration принимают только Text. User.Input фиксирует Name, отдельный PickerConfirmed принимает Confirmed, а PickerOpened/PickerConfirmed — только ProfilePicker Height/Weight. Constructor guards/casts для этих пар не добавлены.
- [x] Убрать ставший encoding-only public Name и лишний ProfileField root. Один internal AuthInteractionSchema.NameField instance используется matrix/cache; User text и picker descriptions разделены. AuthInteraction содержит 23 варианта вместо 22; wire catalog по-прежнему 29 schemas.
- [x] Обновить UI callers и pure fixtures, сохранив 44 Contract callbacks и два picker results. Business requests/validation/navigation/error/retry/lifecycle остаются прежними; обратное снятие telemetry-only изменений UserViewModel побайтно воспроизводит исходный business code.
- [x] Заново скомпилировать реальные финальные исходники cached Kotlin 2.4.20 JVM compiler с strict explicit API: toolkit API 37 файлов, common API 9, Auth API 17, authorization implementation 27, test sources 8. Auth/common JUnit — OK (35 tests); дополнительный actual toolkit API/core/RetainedAnalytics набор в этом проходе — OK (51 tests), итого 86.
- [x] External consumer без friend paths: valid client компилируется; 25/25 negative cases дают точные ожидаемые diagnostics для scenario/action/field/subtype/sealed/internal boundaries. Это compiler checks, не дополнительные runtime tests.
- [x] Kotlin PSI: 258 actual files, syntax errors = 0, named local functions = 0. Проверить сохранность 29 уникальных schema names/keys/versions/codes: AuthEventSchema/ProfileEventSchema unchanged; AuthInteractionSchema после удаления только нового internal NameField совпадает с исходной matrix.
- [x] Отдельный final static audit прошёл 28/28 checks: 650 защищённых business/UI routes/lifecycle/platform source files неизменны. UI diff ограничен UserViewModel imports/typed constructor calls; analytics changes не добавляют бизнес-оркестрацию или runtime wrappers.
- [x] Актуализировать canonical skill, feature standard, UI provider reference и coverage: structurally valid consumer facts отделены от internal wire events; fixed Name не выходит в API. Self-review и независимые final reviews новых subtypes/examples/callers не выявили оставшихся обязательных findings.
- [x] YAML двух skills и metadata через Ruby Psych, 38 local links/anchors, Markdown fences в 11 действующих документах и git diff --check прошли.

Команды: `python3 /tmp/analytics-deep-review-jvm.py --auth-only`,
`python3 /tmp/analytics-deep-review-additional.py`,
`python3 /tmp/analytics-hierarchy-contract-check.py`,
`python3 /tmp/analytics-hierarchy-static-check.py`,
`python3 /tmp/validate-analytics-docs.py`, `git diff --check`.
PSI использует `/tmp/AuthAnalyticsSyntax.java` и
`/tmp/analytics-hierarchy-syntax-input.txt`.
Reports: `/tmp/analytics-deep-review-compilation.json`,
`/tmp/analytics-deep-review-auth-tests.json`,
`/tmp/analytics-deep-review-additional-tests.json`,
`/tmp/analytics-hierarchy-contract-result.json`,
`/tmp/analytics-hierarchy-syntax-result.txt`,
`/tmp/analytics-hierarchy-static-result.json`,
`/tmp/analytics-standard-docs-verification.json`.
JUnit logs: `/tmp/analytics-deep-review-jvm/junit.log` и
`/tmp/analytics-deep-review-jvm/additional-tests.log`.

Не запускались Android/iOS builds, Gradle/KSP/generated DI/catalog composition,
actual UI component/ViewModel/Compose runtime, полный ViewModel typecheck,
native SDK adapters или Firebase delivery. PSI/source comparisons не подменяют
typecheck/runtime. quick_validate.py недоступен без PyYAML; зависимости не
устанавливались, YAML проверен установленным Ruby Psych. История предыдущих
проходов выше не переписана.

## Stateless упрощение интеграции — 2026-10-13

- [x] Заменить сценарии/capability contracts на один stateless AuthAnalytics: typed trackEvent(AuthEvent), generic trackOperation(AuthOperation, whole block), presentScreen(AuthScreen, reason). Прямые закрытые event/operation families, восемь screen enums и необходимые feature enums; common содержит только InputStatus/InputKeyboardAction/ControlAction.
- [x] Убрать action wrappers, flow/session/profile snapshots и retention keys/holders, classifiers, transition/flow events и статусный cache. Не трогать navigation-owned FlowId/entryPoint и обычный Component retainedInstance/ViewModel lifecycle.
- [x] Упростить AuthAnalyticsProvider до provideScreen(context, typed screen/route): shared service + один foundation lifecycle binding. Сохранить восемь leaf и три factory sites, exhaustive route matrices. Первый activation Initial, последующие Restore, без saved process/referrer provenance.
- [x] Сохранить 44 Contract callbacks и два picker results. Text InputChanged сравнивает existing old/new validation status, equal Empty→Empty/Valid→Valid omitted без cache; picker confirmations/clicks/selections остаются distinct. Raw text/credentials/body values/selected IDs не добавлены.
- [x] Сохранить whole login/register/session-check/create-profile/logout business blocks, original T/errors/cancellation и reporting isolation. Удаление telemetry-only изменений восстанавливает normalized business bodies восьми leaf/graph; 55 UI state/screen/contract/direction/loader файлов byte-identical. 11 Auth VM не содержат local recovery catch/manual ensureActive/новых scope wrappers.
- [x] Custom catalog содержит 22 schemas: 10 interaction + 12 operation. Удалены шесть flow schemas, auth_attempt_cancelled и failure_category; failed versions повышены до attempt=5/session=4/profile=2. Это сознательное сокращение funnel/error детализации, не миграция прежних отчётов. InputChanged population изменилась при прежних payload/schema v1.
- [x] API/implementation сократились с 53 production файлов / 1579 строк до 21 / 614. Core queue/consent/SDK transport/bootstrap не менялись; 574 защищённых data/public routes/shared/native/core SDK файлов unchanged.
- [x] Cached Kotlin 2.4.20 K2JVMCompiler с strict explicit API скомпилировал actual toolkit API 37 файлов, common API 3, Auth API 8, implementation 10. Все текущие compiler logs без warnings/errors; production copies/stubs не подменялись.
- [x] AuthInteractionTest/AuthOperationsTest/AuthSchemaTest/AuthScreenTest/CommonInteractionModelsTest — OK (19 tests). Actual toolkit API/core, ScreenAnalyticsTest и AuthAnalyticsProviderTest lifecycle assertion bodies — OK (57 tests). Всего 76 tests, оба JUnit scopes exit 0.
- [x] Provider tests используют actual provider/routes/API/implementation/foundation ScreenAnalytics и cached Essenty/Decompose bytecode. Только generated DI resolution заменён прямой конструкцией actual классов во временном ProviderTestFactory; это не проверка KSP modules.
- [x] External consumer без friend paths: valid client компилируется; 16/16 negative cases дают точные ожидаемые diagnostics для wrong field/status/method/screen/event/operation types, sealed и internal boundaries. Kotlin PSI разобрал 206 файлов: syntax errors = 0, named local functions = 0.
- [x] Переписать canonical skill/standard/provider reference/coverage и связанные UI/DI/error правила под простой API. Удалить старые нормативные требования к scenarios/classifiers/route-ID telemetry mapping. Business dependencies по-прежнему разрешаются внутри retained VM; только stateless analytics передаёт factory после leaf binding. Исторические этапы не переписывать.
- [x] Expanded docs validator: YAML двух skills и metadata через Ruby Psych, 54 local links/anchors, закрытые Markdown fences в 17 действующих документах, git diff --check. Повторный quick_validate.py остановился только на отсутствующем PyYAML; зависимости не устанавливались.

Команды: `python3 /tmp/analytics-simple-jvm.py`,
`python3 /tmp/analytics-simple-jvm.py --tests-only`,
`python3 /tmp/analytics-simple-contract-check.py`,
`python3 /tmp/validate-analytics-docs.py`, `git diff --check`.
PSI использует `/tmp/AuthAnalyticsSyntax.java` с
`/tmp/analytics-simple-syntax-input.txt`.
Отчёты: `/tmp/analytics-simple-compilation.json`,
`/tmp/analytics-simple-auth-tests.json`, `/tmp/analytics-simple-additional-tests.json`,
`/tmp/analytics-simple-contract-result.json`, `/tmp/analytics-simple-syntax-result.txt`,
`/tmp/analytics-simple-protected-result.json`,
`/tmp/analytics-simple-verified-source-manifest.json`,
`/tmp/analytics-simple-ui-result.json`,
`/tmp/auth-stateless-ui-source-verification.json`,
`/tmp/analytics-standard-docs-verification.json`.
JUnit logs: `/tmp/analytics-simple-jvm/junit.log` и
`/tmp/analytics-simple-jvm/additional-tests.log`.

Не запускались AuthAnalyticsModuleTest/ScreenAnalyticsModuleTest и generated
module/config/catalog composition, Gradle/KSP, полный ViewModel typecheck,
остальные actual UI/Compose fixtures/native SDK runtime, Android/iOS builds
или Firebase delivery. Изолированный JVM lifecycle scope не подтверждает
платформенный UI runtime. Прежние 86/25 и платформенные проверки выше относятся
к своим версиям. quick_validate.py недоступен без PyYAML; зависимости не
устанавливались, skill YAML проверяется Ruby Psych.

## ViewModel screen hook вместо feature provider — 2026-10-13

- [x] Оставить один stateless AuthAnalytics с прежними typed trackEvent/trackOperation/presentScreen. Вся feature instrumentation находится в VM; Component resolves analytics=getKoin().get() внутри обычного retainedInstance, как остальные зависимости.
- [x] Удалить AuthAnalyticsProvider/AuthAnalyticsRoutes/AuthAnalyticsIntegrationModule и shared registration. Components/routes/factories/Render больше не передают analytics или выбирают telemetry screen; в UI analytics/ остаётся AuthInputMapping.
- [x] Восемь leaf VM объявляют screenPresentation lambda с fixed AuthScreen. Graph VM сохраняет default null: ни view, ни resolution ScreenAnalytics. VM не хранит Lifecycle/ComponentContext и не добавляет activationFlow observer/scope.
- [x] Foundation BaseViewModel предлагает optional protected `(NavigationReason) -> ScreenPresentation` hook и internal bindScreen. BaseComponent during doOnCreate binds retained VM, attachActivation, затем navigator/eventListener: init navigation не доставляется до binding. ScreenAnalytics владеет per-binding Initial/Restore и pause/destroy close; API + internal @Single через CoreModule.
- [x] Обновить canonical skill/standard/coverage/template/guide и UI/DI/constructor правила под общую foundation интеграцию. references/ui-provider.md заменён references/viewmodel.md, current links обновлены. Исключение для factory-provided Component analytics удалено; исторические provider проверки выше сохранены.
- [x] Cached Kotlin 2.4.20 K2JVMCompiler с strict explicit API: toolkit API 37 файлов, common API 3, Auth API 8, implementation 10; actual foundation 14 production files с Compose compiler plugin и real cached dependencies. JVM-only lock/logger actuals используются для изолированного JVM scope; production sources не заменялись копиями.
- [x] Auth/common — OK (19 tests); toolkit API/core + ScreenAnalyticsTest/ViewModelScreenAnalyticsTest — OK (59 tests), всего 78. Последние два foundation test files перекомпилированы, все 9 foundation tests прошли повторно; это subset общего 78. Null graph без binder DI, retained VM/new lifecycle, Initial/Restore/close и init-navigation ordering проверяются actual BaseVM/BaseComponent/ScreenAnalytics.
- [x] External consumer без friend paths: protected screenPresentation override компилируется; internal bindScreen/protected screenPresentation/internal implementation дают три ожидаемых INVISIBLE_REFERENCE diagnostics.
- [x] Kotlin PSI: 203 files, syntax errors = 0, named local functions = 0. 75 UI source proofs: восемь VM business bodies и 11 Component plumbing transformations exact, 55 protected UI файлов byte-identical, 44 callbacks и два picker handlers сохранены. 404 core/data/native source hashes unchanged; BaseVM business exact after hook/import removal, BaseComponent exact after hook/necessary ordering removal.
- [x] Независимый review нашёл старые drop-in BaseVM/BaseComponent fences в ui-feature reference. Обе копии синхронизированы с actual source, кроме `<org>/<product>` package placeholders. Остальной reference content не менялся этим исправлением.
- [x] В 25 исторических review/audit/verification отчётах добавить explicit historical/superseded note со ссылками на текущие standard/coverage. Остальной контент каждого сохранён byte-for-byte; старые результаты не переписаны как текущие.
- [x] Expanded docs validator: YAML двух skills + metadata через Ruby Psych, 107 local links/anchors, закрытые fences в 42 документах и git diff --check. Current reference renamed viewmodel.md; obsolete normative provider/factory exception rules отсутствуют.

Android/iOS builds, Gradle/KSP/generated modules, native SDK/UI runtime и Firebase
не входят в этот проход. Actual foundation/common тесты используют real cached
Essenty/Decompose/Compose bytecode и explicit actual implementation test DI,
а не generated modules. AuthAnalyticsModuleTest/ScreenAnalyticsModuleTest,
full actual Auth feature VM/Component/Compose compilation/runtime не запускались.
Исторические 76/16 относятся к provider реализации. quick_validate.py недоступен
без PyYAML; dependencies не устанавливались, YAML проверен Ruby Psych.

Команды: `python3 /tmp/analytics-vm-hook-jvm.py`,
`python3 /tmp/analytics-vm-hook-jvm.py --foundation-only`,
`python3 /tmp/analytics-vm-hook-final-foundation.py`,
`python3 /tmp/analytics-vm-hook-contract-check.py`,
`python3 /tmp/validate-analytics-docs.py`, `git diff --check`.
Отчёты: `/tmp/analytics-vm-hook-compilation.json`,
`/tmp/analytics-vm-hook-auth-tests.json`, `/tmp/analytics-vm-hook-additional-tests.json`,
`/tmp/analytics-vm-hook-foundation-final-tests.json`,
`/tmp/analytics-vm-hook-contract-verification.json`,
`/tmp/analytics-vm-hook-syntax-verification.json`,
`/tmp/analytics-vm-hook-source-verification.json`,
`/tmp/analytics-vm-hook-document-source-sync.json`,
`/tmp/analytics-vm-hook-public-route-verification.json`,
`/tmp/analytics-standard-docs-verification.json`.
JUnit logs: `/tmp/analytics-vm-hook-jvm/junit.log`,
`/tmp/analytics-vm-hook-jvm/additional-tests.log`,
`/tmp/analytics-vm-hook-jvm/foundation-final-tests.log`.

## Строгая актуализация skills — 2026-10-13

- [x] Canonical feature-analytics + VM reference/metadata согласованы с текущим stateless API, VM-only instrumentation и foundation hook. Typed facts/schema/privacy/coverage обязательны; бизнес сохраняет цельные операции, реальные return/error contracts и свой error pipeline.
- [x] UI/DI/data/platform/prep/planner/reviewer инструкции направляют в один canonical skill. Старый raw Firebase API/iOS no-op пример удалён; business layers не получают analytics dependencies. Дополнительные providers, retention, scopes/jobs/caches не требуются.
- [x] Независимая оценка Search и изменения login validation/picker выявила неоднозначность Result и shared config callbacks; исправлено. VM-owned onResult остаётся допустимым, shared plumbing не создаёт событий; dedicated dialog views/close имеют explicit scope/omission.
- [x] Противоречивые dialog/process-death примеры синхронизированы с actual API и host: open/openings/Session, in-memory configs, title/content comparison, dismiss-before-result и serializer=null. Документы не обещают отсутствующую iOS persistence.
- [x] Frontmatter и metadata проверены Ruby Psych с эквивалентными quick_validate checks, Markdown fences/новые ссылки и source examples сверены. git diff --check прошёл; 238 профильных code/config hashes неизменны, BaseVM/BaseComponent reference copies совпадают с source.

Это docs-only проход: Android/iOS builds, Gradle/KSP и runtime tests не запускались;
прежние 78 tests относятся к предыдущему этапу. quick_validate.py не запускается
без PyYAML; зависимости не устанавливались. Две прежние ссылки на внешний
orchestrator known-gaps register остаются вне этого scope.

Проверка: `python3 /tmp/validate-analytics-skills-strict.py`, `git diff --check`.
Отчёт: `/tmp/analytics-skills-strict-verification.json`; diff относительно начала
этого прохода: `/tmp/analytics-skills-strict-diff.patch`.


## Строгое ревью аналитики — 2026-10-13

- [x] Зафиксировать исходный dirty/staged tree и провести независимые API/matrix, UI/lifecycle и core/platform reviews; затем перекрёстно перепроверить исправления. Чужие изменения не сбрасывать/stage/commit.
- [x] Перенести четыре accepted intent events после существующих eligibility guards (Login.Email, Registration, User.Next, Experience.Next); business bodies/guards/navigation/retry сохранить. Исправить прежние invalid-submit/Next и non-null flow test expectations.
- [x] Убрать wire-code getters/AnalyticsCode из восьми public fact enums. Internal AuthEncoding exhaustively кодирует значения; InputField/InteractionAction закрывают key types, anonymous NameField удалён. Все 26 wire values и 22 schemas/versions сохранены; нового compatibility слоя нет.
- [x] Добавить typed enum encoder overload, payload проверки всех enum values и meaningful key tests; убрать obsolete public-code test. Reusable toolkit AnalyticsCode factory сохранён.
- [x] Удалить manual ensureActive в worker; cooperative yield остаётся на catch boundaries и прежнем fairness interval. Проверить active sink CancellationException и true owned shutdown во время configuration/send без лишнего fallback.
- [x] Перенести iOS ExperimentalNativeApi opt-in в existing KotlinMultiplatformConventionPlugin. Debug configuration policy не менялась; generated/native compilation не запускалась.
- [x] Исправить cleanup ProfileSelectionViewModelTest: завершить queued lifecycle/VM cancellation до stopKoin/resetMain. Добавить rejected User.Next в existing Component fixture; actual Login JVM regression проверяет invalid/eligible/busy/reporting-failure paths.
- [x] Fresh cached strict JVM compilation и tests: Auth 19 + toolkit/API/core/foundation 62 = 81; actual Auth UI commonTests 36 + два isolated Login tests = 38. Общий результат 119; fixtures/substitutions/skips явно указаны в coverage.
- [x] Red/green regression: exact baseline четырёх event positions с тем же Kotlin module-name даёт четыре AssertionError, без linkage/native failures; current artifact повторно проходит все 38 UI tests. Repository production sources не подменялись.
- [x] External consumer: protected hook override и шесть negative cases, включая wire getters/invalid field/enum-key combinations. PSI 239 профильных файлов: syntax/local functions 0. Baseline source comparisons подтверждают неизменный бизнес и platform wiring.
- [x] Уточнить canonical skill/reference/standard/guide и actual coverage: plain public enums, internal encoders/closed keys, accepted/rejected guard policy и изменение event population. YAML/local links/fences и git diff --check прошли.

Команды: `python3 /tmp/analytics-strict-review-xiomnh55/verify-jvm.py`,
`python3 /tmp/analytics-strict-review-xiomnh55/ui-jvm/verify-ui.py`,
`python3 /tmp/analytics-strict-review-xiomnh55/verify-contract.py`,
`python3 /tmp/analytics-strict-review-xiomnh55/verify-source.py`,
`python3 /tmp/analytics-strict-review-xiomnh55/verify-docs.py`, `git diff --check`.
Отчёты/логи этого прохода находятся в `/tmp/analytics-strict-review-xiomnh55/`;
baseline manifest и exact before copies сохранены отдельно от предыдущих проверок.
Подробный scope — [текущее покрытие](authorization-coverage.md#текущая-проверка).
Android/iOS builds, Gradle/KSP/generated DI, rendered/native UI/SDK runtime и Firebase
не запускались. Temporary UI harness substitutes не доказывают generated/platform
integration; production collection по-прежнему ждёт application consent policy.

## Проверка screenPresentation авторизации — 2026-10-13

- [x] Независимо сверить все 11 Component→VM→Render цепочек: 8 отдельных экранов имеют correct fixed screenPresentation и retained analytics DI; пропусков нет.
- [x] AuthViewModel/AuthProcessViewModel/ProfileCreationViewModel оставляют default null: child-stack hosts, у ProfileCreation только общий неинтерактивный progress indicator. Новых container screen_view и лишнего analytics DI не добавлять.
- [x] Дополнить coverage корневым Auth host и объяснением surface/container distinction. Production sources не менялись; проверки этого прохода source-only и docs validation, прежние 119 runtime tests не запускались повторно. Android/iOS builds и Firebase не запускались.

## Обязательное покрытие экранов в skills — 2026-10-13

- [x] В canonical feature-analytics закрепить обязательный screenPresentation каждого отдельного экрана, включая splash и nested/tab pages; graph/container остаётся default null. Nullable foundation API не означает optional screen coverage.
- [x] Перед завершением новой фичи сверять все её actual routes/factories/Render → Component → retained VM → FeatureScreen → hook; при изменении — весь affected screen scope. Missing/wrong hooks, enum без owner и container views вместе с child screens блокируют завершение, а не закрываются omission.
- [x] Согласовать UI/DI/base-class/platform/planner/reviewer инструкции, project entrypoints, стандарт и coverage template. Dialog/overlay contract остаётся отдельным scope; filename/router/base class сами по себе не определяют screen.
- [x] Независимый forward check новой Search-фичи выявил забытый Bootstrap и лишнюю SearchRoot identity; уточнение container/child duplicate views прошло повторное review. Markdown/YAML/links и рабочий diff проверены; исходники/конфигурация и foundation code examples не изменены.

Это docs-only проход: Android/iOS builds, Gradle/KSP, runtime tests и Firebase
не запускались. Свежий отчёт и diff относительно начала прохода:
`/var/folders/gs/p9xpr8w95s3g6tcyx10tqbc80000gn/T/analytics-screen-skill-20261013-3k54vafl/verification.json`
и `docs.patch` рядом. Прежние staged whitespace замечания в старых/deleted API
файлах записаны отдельно; staging не менялся.

## Повторная проверка авторизации и событий — 2026-10-13

- [x] Независимо сверить все 11 route/Component/VM/Render chains: 8 обязательных fixed screen hooks и 3 null containers; отсутствие duplicate views, feature lifecycle wiring/providers/retention и лишних jobs.
- [x] Проследить все 44 Contract callbacks и 2 picker results; проверить guards, typed payloads/privacy, 22 schemas, DI и whole operations/results/errors/cancellation. Production behavior defects не найдены.
- [x] Исправить stale foundation KDoc и синхронизировать source reference; уточнить coverage: clear только Email, password clear control отсутствует, IME не submit, text status changes не raw edits.
- [x] Усилить repository Registration/selection event assertions и ProfileCreation regression всех 5 actual steps с Initial/Restore/close once, pending business operation и Home после completion. Cleanup выполняется до завершения Koin/Main dispatcher.
- [x] Fresh isolated JVM tests: Auth 19 + toolkit/API/core/foundation 62 + Auth UI 40 = 121 passed. UI включает 37 repository cases и 3 temporary Login/AuthProcess cases; проверить actual Login↔Registration navigation/lifecycle без parent views.
- [x] Final self/independent review, current coverage и docs/source verification. Android/iOS builds, Gradle/KSP, rendering/native SDK и Firebase не запускать; substitutions и unrun checks явно записать.

Команды: `python3 /tmp/auth-coverage-review-20261013-rjz9am53/verify-jvm.py`,
`python3 /tmp/auth-coverage-review-20261013-rjz9am53/ui-jvm/verify-ui.py --production-only`,
`python3 /tmp/auth-coverage-review-20261013-rjz9am53/ui-jvm/verify-ui.py --tests-only`,
`python3 /tmp/auth-coverage-review-20261013-rjz9am53/verify-docs.py`, `git diff --check`.
Отчёты/исходный manifest и exact before copies — в этом новом temporary directory,
отдельно от прежних запусков. Подробный scope: [актуальная проверка](authorization-coverage.md#текущая-проверка).

## Переименование экрана регистрации — 2026-10-13

- [x] Перенести семь MVI-файлов в `registration/`, назвать все типы `Registration*`; обновить AuthProcessComponent, тест и документацию.
- [x] Переименовать title/description resource keys во всех трёх локалях; сохранить тексты, маршруты, serialized contracts, бизнес-логику и analytics identities.
- [x] Проверить отсутствие прежних screen names/paths, конфликты типов/ресурсов и точное сохранение кода после замены имён.
- [x] Изолированные JVM-проверки авторизации: 40 tests passed; Android/iOS builds, Gradle/KSP, rendering/native SDK и Firebase не запускались.

Команда: `python3 /tmp/registration-rename-20261013-qtku23fu/ui-jvm/verify-ui.py`.
Scope и substitutions — `ui-jvm/tests.json`, проверка переименования —
`rename-verification.json` в том же temporary directory.
