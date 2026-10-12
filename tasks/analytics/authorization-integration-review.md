# Authorization analytics integration review

> Исторический отчёт: описывает реализацию и проверки этого этапа, включая
> позднее заменённые API/provider/retention. Текущие правила —
> [feature-standard](feature-standard.md), фактическое покрытие и scope —
> [авторизация](authorization-coverage.md). Результаты ниже не подтверждают
> последующие изменения.

## Scope

Вход Email/Google/Apple, регистрация credentials, session check, их preparation stages,
показы Splash/Login/Registration, transitions, flow start/explicit exits и rejected submits.
Создание профиля, consent UI, user identity/reset, logout, новые источники deeplink/push
и реальная Firebase delivery за рамками этой интеграции.

## Архитектура

- Публичный AuthAnalyticsModule + qualified @Single AnalyticsSchema, generated .module.
- Internal модели, события, schema matrix, operation factories и navigation state в owning UI feature.
- SDK-independent feature-api делит server authentication/createAccount и prepare.
  AuthorizationPreparationExecution описывает typed этапы/результаты, без Analytics/Firebase dependency.
- UI получает AuthAnalytics через constructor injection при retainedInstance;
  Analytics/FlowContext/AuthFlow скрыты от экранных классов.
  AuthExecution использует общий trackOperation, monotonic duration и fresh OperationId.
- AuthFlow хранится в StateKeeper, EntryPoint передаётся сериализуемым route.
  Leaf lifecycle bindings находятся в Component.init, не в Render/container.

## Self-review loop

1. Граница бизнес-фактов: устранено смешивание server success и downstream failure;
   3 дублированных login preparation blocks сведены к одному prepare.
2. Ошибки/отмена: Apple NSError cancellation сохраняется common enum; Google cancellation
   больше не распознаётся по локализованному message и не запускает следующую credential strategy.
   Splash не поглощает coroutine cancellation; profile failure redirect сохраняет прежнюю UI политику.
   Stage обязателен в failed attempt/session factory: silent default stage удалён.
   При возврате original error переносятся suppressed tracking defects со stage wrapper;
   диагностическая ошибка не теряется и не заменяет domain failure.
3. Навигация: system Back не обходит владельца flow transition; actual Action/Back/Restore,
   saved FlowContext, один flow_started, один явный flow_exited, никакого exit из destroy.
   После сверки ProfileCreationComponent/UserComponent уточнён ProfileSetupReturn:
   это Back первого leaf шага; origin/referrer = profile_creation_user, не контейнер.
   FlowId добавлен в сериализуемый route identity: одинаковый entry при повторном
   возврате не переиспользует exited AuthProcess/retained VM. @EncodeDefault сохраняет ID
   всегда, без повторной генерации default при сравнении сериализации; старые route
   без ID читаются. AuthFlow использует route ID, без второго UUID на startup.
   Старый saved AuthProcess route без нового entry field остаётся читаемым: явный
   RestoredScenario с origin=null/Restore, без выдуманного исходного источника.
   Apple exception сохраняет прежний порядок message/cause; typed reason добавлен в конец.
4. Structured concurrency: initial data async children изолированы coroutineScope;
   request failure не становится ложным interrupted outcome. Caller cancellation сохраняется.
5. Privacy/performance: параметры принимают business enums/DurationMillis, PII/message не кодируются;
   нет IO в event factories, tracker jobs, runtime catalog changes или per-frame tracking.
   Existing bounded core queues/consent policy сохранены. Типы и wire names разложены по файлам/матрицам.

Проверено по skill code-review-and-quality: correctness, architecture, readability,
privacy/security, performance. Независимое внешнее review не заявляется.

Google/Apple cancellation сверена с официальными SDK contracts:
[Android GetCredentialCancellationException](https://developer.android.com/reference/androidx/credentials/exceptions/GetCredentialCancellationException),
[Apple ASAuthorizationError.canceled](https://developer.apple.com/documentation/authenticationservices/asauthorizationerror-swift.struct/code/canceled).

## Facade simplification review

- Один AuthAnalytics создаётся владельцем сценария и передаётся в Components/ViewModel.
  Stateful facade не singleton: session/authorization factories принадлежат Component scope.
- StateKeeper consume/register и Splash navigation reason перенесены внутрь internal AuthSessionFlow;
  сохранены прежние keys, поля сериализации и route FlowId. Operation context остаётся неизменным.
- Profile-load session failure превращается в typed routing result внутри facade;
  token errors возвращаются original exception, coroutine cancellation не превращается в redirect.
- Lifecycle binding остаётся один на leaf и создаётся facade-методом; Render/container показы не добавлены.
- Consumer contracts находятся в AuthContracts; event classes объединены в четыре sealed семейства.
  14 production analytics files, из них 11 internal; annotated generated module registration сохранена.
- Domain preparation contract и use cases сохранены. Facade вызывает существующие helpers
  в caller coroutine; дополнительных scope/jobs/queues или Firebase dependencies нет.
- Матрица схем v1 и wire payloads не изменены; semantic events не привязаны к Kotlin class names.
- Удалён incidental formatting вне analytics/auth consumers, без сброса пользовательских изменений.
- После переноса Kotlin Android host incremental cache сообщил DirtyData/FqNames failure;
  clean выполнен только для generated build results модуля authorization, следующий host прогон passed.
- AuthAnalyticsTest проверяет общий context операций/показов/preparation, restored form navigation,
  saved session identity, profile failure redirect, token original error и caller cancellation.

## Последующий глубокий аудит

Детальный scope, review loop, 11 исправлений и ограничения — `authorization-audit.md`.
Устранены независимый failure stage подготовки, чужие operation stages, две stateless
обёртки, domain preparation duplication и business calls в неправильном facade mode.
AuthFlowEvent отражает scope сценария. Schema identity проверяется fixtures.
JSON перенесён в tests. OAuth coroutine cancellation/fatal boundaries исправлены;
Apple provider DI больше не создаёт неиспользуемые context/HTTP/JSON dependencies.

## Evidence

- Первая компиляция owning feature Android/iOS: BUILD SUCCESSFUL, 1m 31s;
  `/tmp/grippo-auth-compile.log`.
- Первый тестовый прогон: 20 Android host + 20 iOS simulator, failures/errors 0,
  BUILD SUCCESSFUL 1m 32s; `/tmp/grippo-auth-tests.log`.
- После review fixes: полный Gradle прогон BUILD SUCCESSFUL, 8m 13s;
  `/tmp/grippo-auth-final.log`.
- После facade simplification: clean owning-feature host tests BUILD SUCCESSFUL, 30s;
  `/tmp/grippo-auth-facade-host.log`; 31 tests, failures/errors 0.
- Финальный Gradle прогон facade + affected suites/apps: BUILD SUCCESSFUL, 4m 38s;
  `/tmp/grippo-auth-facade-final.log`. Таблица ниже обновлена по финальному глубокому аудиту.

- После глубокого аудита: BUILD SUCCESSFUL 4m 5s; `/tmp/grippo-auth-audit-complete.log`.
  Все source/dependency edits завершены до этого прогона; XML failures/errors 0.

| Suite | Android host | iOS Simulator ARM64 | Failures/errors |
|---|---:|---:|---:|
| Authorization | 37 | 37 | 0 |
| Analytics API | 12 | 12 | 0 |
| Analytics core | 36 | 32 | 0 |
| Foundation lifecycle bridge | 15 | 4 | 0 |
| Shared | 24 | Не запускался в этом прогоне | 0 |
| Всего | 124 | 85 | 0 |

- Android assembleDebug/assembleRelease: passed.
- Shared Debug XCFramework: device + simulator ARM64, passed.
- Lint Debug/Release: 0 errors/fatal, 14 прежних warnings в каждой конфигурации.
- Generated headers обоих slices: AuthFlowState/AuthExecution/AuthEventSchema/
  AuthEventMatrix/AuthStageException/AuthSessionFlow/AuthAttemptEvent не экспортируются.
- Xcode unsigned Debug simulator/device: BUILD SUCCEEDED, exit code 0 в обоих случаях;
  `/tmp/grippo-auth-audit-swift-simulator.log`, `/tmp/grippo-auth-audit-swift-device.log`.
  Прежние warnings: bundled libicu object target 18.5 при deployment 16.0,
  skipped AppIntents metadata. iOS Release/runtime не проверялись.
- `git diff --check`: passed.

Qualified feature module resolution проверена AuthSchemaTest; агрегация qualified
schema groups и core catalog validation — AnalyticsModuleTest/AnalyticsTest.
Shared composition и оба platform entry points проверены сборками; production
Koin/Firebase runtime startup не запускался.

Runtime app/sign-in/token retrieval, Firebase Console и SDK sends не используются.
Android mapping upload исключён; Xcode только unsigned Debug, без запуска приложения.
