# Глубокий аудит аналитики авторизации

> Исторический отчёт: описывает реализацию и проверки этого этапа, включая
> позднее заменённые API/provider/retention. Текущие правила —
> [feature-standard](feature-standard.md), фактическое покрытие и scope —
> [авторизация](authorization-coverage.md). Результаты ниже не подтверждают
> последующие изменения.

## Проверенный scope

Все 14 production файлов analytics: facade/contracts/module, четыре event families,
schema/event matrices, operation execution, navigation/state, validation и failures.
Login/Registration/Splash Components и ViewModel, Auth/AuthProcess containers, serializable
AuthRouter/AuthorizationEntryPoint, Root token-absence routing, shared qualified Koin wiring.
LoginUseCase/RegisterUseCase и их domain preparation contract; вызовы Google/Apple providers
и typed cancellation. Общие trackOperation/TrackedOperation и lifecycle screen binding;
core schema aggregation, bounded queues и consent подтверждаются существующими suites.
OAuth signIn cancellation/fatal boundaries и Apple provider DI также проверены в SDK adapters.

Это self-review по correctness, architecture, readability, privacy и performance,
плюс отдельный concurrency pass. Независимый внешний review и runtime profiling не заявляются.

## Найдено и исправлено

1. PreparationFailed принимал независимые preparation_stage/failure_stage: они могли
   противоречить друг другу. Второй аргумент удалён; failureStage закреплён за enum
   AuthPreparationStage, factory больше не содержит дублирующий when.
2. Failed attempt/session принимали этапы чужой операции. AuthEventMatrix проверяет
   допустимые failure stages при создании конкретного события. Матрица outcome/stage сохранена.
3. AuthInitialDataLoad/AuthSessionCheck создавали временные объекты без состояния ради
   одного execute. Заменены двумя internal suspend functions; coroutineScope/async и
   caller cancellation сохранены. AuthExecution остаётся одним объектом на сценарий:
   он реализует domain execution port и несёт immutable attribution.
4. Login/Register копировали одинаковую profile/data/push preparation policy.
   Одна internal prepareAuthorizationSession владеет последовательностью; список реальных
   data requests остаётся у каждого UseCase. Login загружает weight history, Register — нет.
5. Facade mode проверялся только при screen/navigation binding. Все business operations
   теперь проверяют session/authorization scope до provider/domain вызовов и событий.
   Private checked casts завершаются явной programming error; никакого default context/retry.
   Один consumer facade сохранён; generics, новые публичные scope-классы и дополнительные
   DI-модули ради этого не введены.
6. AuthNavigationEvent включал rejected form, которое не является переходом навигации.
   Семейство переименовано в AuthFlowEvent: события относятся к сценарию. Wire names неизменны.
7. Удалён неиспользуемый Serialization JSON production dependency;
   commonMain напрямую объявляет serialization core.
8. Удалены formatting tails/trailing whitespace и redundant wildcard-covered imports
   в analytics; cleanup ограничен integration-owned files, чужие изменения не сбрасывались.

9. OAuth signIn boundaries оборачивали CancellationException; Google iOS превращал
   её в user Cancelled, включая interruption при живом caller. Coroutine cancellation
   теперь пробрасывается без замены. Google token exchange/JSON/decode guards также
   не маскируют cancellation/fatal; внешние signIn guards Android/iOS и Apple callback
   mapper не превращают fatal Error в обычный auth failure. SDK user cancellation
   остаётся отдельным typed outcome по platform domain/code. Live OAuth UI не запускалась.
10. AppleAuthProvider имел три UNUSED_PARAMETER на обоих targets, а AppleAuthModule
   ради них включал Context/HTTP/Serialization modules. Удалены NativeContext/HttpClient/Json
   из expect/actual constructor и provider factory; удалены пять ненужных Gradle dependencies.
   Qualified/generated Koin pattern сохранён. Apple provider создаётся
   из своего module без networking/context/JSON bindings или SDK signIn.

## Повторный review pass

- У экранных consumers только AuthAnalytics + typed business arguments/results;
  прямых imports внутреннего flow, event/schema factories или toolkit Analytics нет.
- Stateful facade принадлежит Component scenario, не app singleton. ViewModel retention
  сохраняет immutable operation context; recreated Component восстанавливает navigation state.
  Facade/navigation не хранят ComponentContext, StateKeeper или lifecycle после constructor/binding.
- Flow ID закреплён за route identity и StateKeeper; restore не создаёт второй flow_started.
  Back проходит через navigation owner; container/Render не отправляют screen views.
  Explicit flow exit deduplicated, destruction не объявляется user exit.
- Initial data children остаются внутри coroutineScope: request failure отменяет sibling,
  а caller cancellation отдельно кодируется interrupted. Никаких GlobalScope, дополнительных
  scopes/jobs/dispatchers/retry loops/queues фасад не создаёт.
- Server-confirmed attempt success отделён от profile/data/push preparation failure.
  Stage wrapper возвращает original exception и suppressed reporting defects в прежний UI path.
  Session profile failure сохраняет существующий redirect; token failure/cancellation не скрываются.
- Event constructors принимают enums/DurationMillis; password/email/token/code/message/body
  не кодируются. Разбор ошибок не опирается на локализованный текст. Это проверка analytics
  payloads; политика общих application error logs/CrashReporting этим аудитом не меняется.
- Schema/Key instances и qualified annotated module едины; Koin generated .module сохранён.
  Семантический wire contract v1 из 17 схем не менялся при группировке/переименованиях.
- Статически проверены bounded payloads и отсутствие I/O в factories. Точные CPU/heap цифры
  не измерялись; заявление «нулевой overhead» не делается.

## Сохранённые явные политики

RestoredScenario нужен для чтения старого saved route без provenance; он не придумывает
источник. Unexpected — явная категория неклассифицированной ошибки. PushUnavailable
сохраняет существующий nullable best-effort token contract: готовность/SDK failure отдельно
не различаются этим API; backend registration failure — отдельный failed stage.
Consent/identity/logout/profile creation telemetry не расширялись. Data-services Firebase
transport сохранён. Реальные SDK sends, Firebase Console, app launch, sign-in/token retrieval
и iOS Release не используются в проверках.

## Проверки

Финальный Gradle прогон после завершения source/dependency edits:
BUILD SUCCESSFUL 4m 5s, `/tmp/grippo-auth-audit-complete.log`.

Android Debug/Release и Shared Debug XCFramework (device/simulator ARM64): passed.
Lint Debug/Release: 0 errors/fatal, 14 прежних warnings в каждой конфигурации.
Headers обоих slices: AuthFlowState/AuthExecution/AuthEventSchema/AuthEventMatrix/
AuthStageException/AuthSessionFlow/AuthAttemptEvent/AuthFlowEvent не экспортируются.
`git diff --check` и проверка trailing whitespace в integration-owned sources: passed.
Swift unsigned Debug simulator: BUILD SUCCEEDED, exit 0, `/tmp/grippo-auth-audit-swift-simulator.log`.
Swift unsigned Debug device: BUILD SUCCEEDED, exit 0, `/tmp/grippo-auth-audit-swift-device.log`.
Прежние Xcode warnings: bundled libicu object target 18.5 при deployment 16.0,
skipped AppIntents metadata; errors отсутствуют. Runtime profiling/SDK UI не запускались.
Android mapping upload исключён; Swift конфигурация только Debug.
