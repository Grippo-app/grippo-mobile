# Авторизация: матрица интеграции

Статус: реализация подключена; фактические команды текущих проверок записываются в
[todo.md](todo.md). Предыдущий полный review/testing pass и его ограничения — в
[full-review-verification.md](full-review-verification.md); он и прежние статические
отчёты относятся к состоянию кода на момент этих проверок.
Firebase delivery/sign-in/uploads не входят в текущие проверки.

## Подключение и границы

- AuthAnalytics — один consumer entry point в analytics-api с nested Authorization,
  Session и FailureClassifier. Operation/transition/enums/snapshot — models/.
- analytics/authorization содержит internal scenario/state/models/events/operation/
  schema и public annotated module. Нет UI/domain/services/SDK/core dependencies.
  Consumer EntryPoint enum кодируется внутренней AuthEntryPoints matrix.
- UI analytics/: internal stateless AuthAnalyticsProvider, отдельный
  AuthFailureClassifierImpl и существующий AuthAnalyticsIntegrationModule с @Single
  bindings/generated .module. Provider не хранит UI contexts или scenarios.
- Foundation RetainedAnalytics выполняет consume → type-key retention → identity
  check → register latest save. Provider владеет mapping/keys/serializer; прежние
  authorization_analytics_flow / authorization_session_flow остаются в его private
  companion. Mismatched owner/snapshot отклоняется, не заменяется новым flow.
- Компоненты получают analytics до retained ViewModel и передают тот же instance
  детям/VM. VM/use cases владеют бизнес-запросами и решениями; Components — реальной
  навигацией. Leaf lifecycle подключён через DI ScreenAnalytics.bind.
- Login/Credential сохраняют цельные бизнес-операции внутри развёрнутых
  trackOperation-блоков. SDK-ошибки преобразуются через safeLaunch.mapError только
  для диалога; аналитика и CrashReporting получают исходную ошибку.
  Направление выбирается только по бизнес-результату.
  Splash запускает class-level loadContent() из init: единственный safeLaunch с
  AppContent читает token, загружает muscle → equipment для no-session пути и
  повторяется после dismiss. При token вызывает private suspend loadProfile():
  узкий catch обычной ошибки только профиля возвращает на вход без диалога.
  Локальных named functions, дополнительных jobs и дублированных последовательностей нет.
- 12 custom schemas зарегистрированы один раз qualified @Single через
  AuthAnalyticsModule().module в shared, built-in screen schema не дублируется.

## События

| Факт | Схемы | Владелец | Типизированные значения | Проверки |
|---|---|---|---|---|
| Новый flow | auth_flow_started | Provider → AuthFlow factory, только создание | EntryPoint, origin, FlowId | AuthNavigationTest; AuthAnalyticsProviderTest |
| Выход | auth_flow_exited | AuthProcessComponent → trackTransition(Exit(reason)) | ExitReason, Step | AuthNavigationTest |
| Email/Google/Apple вход | auth_attempt_started/succeeded/failed/cancelled/interrupted | LoginViewModel → trackOperation(Login(method)) | Method, AuthAction.Login, duration полного входа, FailureCategory | AuthOperationsTest; AuthIntegrationTest |
| Создание аккаунта | auth_attempt_* | CredentialViewModel → trackOperation(Register) | Method.Email, AuthAction.Register, duration полного создания, FailureCategory | CredentialViewModelTest; AuthOperationsTest; AuthIntegrationTest |
| Проверка профиля существующей сессии | auth_session_check_started/completed/failed/interrupted | SplashViewModel, suspend loadProfile → trackOperation(SessionCheck) | Duration только cached/remote profile lookup; FailureCategory исходной ошибки, без outcome/stage | AuthOperationsTest; SplashViewModelTest |
| Login → Registration / Back | auth_step_changed | AuthProcessComponent → trackTransition(OpenRegistration/BackToLogin) | Step, Action/Back | AuthNavigationTest |
| Leaf screen views | built-in screen schema | Login/Credential/Splash lifecycle → ScreenAnalytics.bind | ScreenId, referrer, reason | ScreenAnalyticsTest; AuthNavigationTest; AuthIntegrationTest |

Одна попытка определяет start/terminal с общим OperationId, retry получает новый ID.
Reporting errors, consent и queue policy могут исключить отправку; delivery не гарантируется.
LoginUseCase.loginEmail/loginGoogle/loginApple и RegisterUseCase.createAccount
возвращают бизнес-Boolean после private prepare; failure подготовки — failure
полной попытки. Отдельных preparation операций/схем нет. SDK cancellation
определяется типом, coroutine cancellation — Interrupted.

Splash принимает решения через бизнес-Boolean и SplashDirection. Единственный
safeLaunch читает token; без token последовательно загружает muscle → equipment
и открывает вход. Ошибки token/reference используют стандартный pipeline и
повторяют эту последовательность только после закрытия диалога. Эти пути не
создают SessionCheck events. При token private suspend loadProfile работает в той
же корутине; SessionCheck декорирует только hasProfile (cache или remote lookup).
Узкий catch снаружи trackOperation обрабатывает обычный Exception только этой
проверки: сначала failed с исходной FailureCategory, затем немедленный
AuthProcess(SessionCheckFailed) без стандартного logger/crash/dialog пути.
CancellationException повторно выбрасывается и даёт interrupted без fallback;
fatal Error не перехватывается. Успех даёт completed, а Boolean затем выбирает
Home/CreateProfile вне try и аналитики. Навигация, fallback и остальные запросы
не входят в protected block; их ошибки сохраняют стандартный pipeline.
Один VM job и loader AppContent охватывают весь startup; Profile loader удалён.
Completed означает завершение блока, не подтверждённое восстановление сессии.
Reporting Exceptions не блокируют block/result, transition/navigation или
lifecycle; caller cancellation/fatal Error сохраняются.

SessionCheck теперь измеряет только проверку профиля при известном token.
Payload прежний; все четыре auth_session_check_* используют schema_version=3.
Прежние версии включали token/no-session пути и могли считать обработанный profile
fallback completed. В v3 population — только профиль при известном token, duration
измеряет только его проверку, ошибка профиля всегда failed. В отчётах фильтровать
или разделять schema_version=3 и прежние версии; duration, количество попыток и
terminal outcomes напрямую не сравнивать. Новых outcome/stage/схем нет.

Локальный профиль сразу ведёт в Home без запроса профиля и справочников;
это проверено cached-profile сценарием SplashViewModelTest.
Closed flow не переоткрывается, duplicate transition не меняет attribution,
Exit deduplicated.

## Атрибуция и persistence

| EntryPoint | Триггер | Origin |
|---|---|---|
| SessionCheck (internal) | Создание retained Splash-сценария; сама операция начинается только при наличии token | absent |
| LaunchWithoutSession | Splash получил null token | auth_splash |
| TokenAbsent | Root получил null token вне Auth | absent |
| SessionCheckFailed | Ошибка загрузки профиля в Splash → немедленный возврат на вход без диалога | auth_splash |
| ProfileSetupReturn | Возврат из ProfileCreation | profile_creation_user |
| RestoredScenario | Legacy route без provenance | absent |

Route сериализует собственный screen-api FlowId; только provider преобразует value
в toolkit ID. Поле flowId, shape {value} и legacy SerialName сохранены.
Login/Registration используют общий immutable FlowContext. Snapshot хранит
navigation/step/exited и legacy SerialName. Retained instance актуальнее snapshot;
recreated owner регистрирует save того же instance. Независимые owners изолированы.
Restore не повторяет start. Initial/Action/Back/Restore задаёт navigation owner;
foreground return — Restore. Destroy не user exit; Render/containers не создают views.

## Объём и совместимость

- Не трекаются field validation, reference loads, внутренние Profile/Data/Push stages,
  per-keystroke input и generic/duplicate clicks.
- Muscle → equipment выполняются последовательно в Splash safeLaunch, вне session
  operation, без coroutineScope/async; первая ошибка не запускает второй запрос.
- ProfileCreation и notification/deep-link launch attribution пока не подключены.
- Credentials/tokens/codes/input/messages/body/URL не попадают в payload.
- auth_attempt_failed v4; остальные четыре attempt events v3: success/duration/failure
  охватывают полный login/register. Setup схемы удалены. Все четыре SessionCheck
  events v3 без outcome/stage; flow_started/flow_exited/step_changed v1. Прежние
  server-only login/register timings и SessionCheck population/duration нельзя
  сравнивать с новыми напрямую.

## Проверяемые границы

- AuthSchemaTest: 12 payload/key/version fixtures.
- AuthOperationsTest: results/errors/cancellation/reporting failures/retry IDs/contexts.
- AuthNavigationTest: attribution/restore/exit/repeated/closed transitions.
- AuthAnalyticsProviderTest: recreation/latest save/mismatch/mapping/independent owners.
- AuthIntegrationTest: цельные use cases, preparation/push/error/cancellation,
  route ID boundaries/legacy JSON, generated DI, typed SDK classifier и lifecycle.
- CredentialViewModelTest: actual registration/navigation/error pipeline при
  reporting failures и ошибке подготовки, Register/Email start/terminal.
- SplashViewModelTest: actual requests/navigation/cached-profile short circuit,
  немедленный profile fallback без диалога; token/reference errors через pipeline,
  повтор только после dismiss, обязательные справочники для no-session пути,
  sequential loads/cancellation.
- RetainedAnalyticsTest: actual helper type-key retention/latest save/validation order.
- ScreenAnalyticsTest: actual lifecycle callbacks/reporting errors и DI singleton
  с независимыми owners.

Наличие тестов не означает, что они запускались или покрывают весь UI runtime.
Actual commands/results/skips и app/platform compilation относятся к текущему
verification report. Матрица фиксирует подключённые факты, не Firebase delivery.
