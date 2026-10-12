# Авторизация: матрица интеграции

Статус: реализация подключена и проверена текущим полным review/testing pass.
Результаты Android/iOS tests/builds и ограничения записаны в
[full-review-verification.md](full-review-verification.md). Предыдущие статические
отчёты и исторические builds относятся к прежним версиям.
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
- 12 custom schemas зарегистрированы один раз qualified @Single через
  AuthAnalyticsModule().module в shared, built-in screen schema не дублируется.

## События

| Факт | Схемы | Владелец | Типизированные значения | Проверки |
|---|---|---|---|---|
| Новый flow | auth_flow_started | Provider → AuthFlow factory, только создание | EntryPoint, origin, FlowId | AuthNavigationTest; AuthAnalyticsProviderTest |
| Выход | auth_flow_exited | AuthProcessComponent → trackTransition(Exit(reason)) | ExitReason, Step | AuthNavigationTest |
| Email/Google/Apple вход | auth_attempt_started/succeeded/failed/cancelled/interrupted | LoginViewModel → trackOperation(Login(method)) | Method, AuthAction.Login, duration полного входа, FailureCategory | AuthOperationsTest; AuthIntegrationTest |
| Создание аккаунта | auth_attempt_* | CredentialViewModel → trackOperation(Register) | Method.Email, AuthAction.Register, duration полного создания, FailureCategory | CredentialViewModelTest; AuthOperationsTest; AuthIntegrationTest |
| Проверка сессии | auth_session_check_started/completed/failed/interrupted | SplashViewModel → trackOperation(SessionCheck) | Duration; FailureCategory необработанной ошибки, без outcome/stage | AuthOperationsTest; SplashViewModelTest |
| Login → Registration / Back | auth_step_changed | AuthProcessComponent → trackTransition(OpenRegistration/BackToLogin) | Step, Action/Back | AuthNavigationTest |
| Leaf screen views | built-in screen schema | Login/Credential/Splash lifecycle → ScreenAnalytics.bind | ScreenId, referrer, reason | ScreenAnalyticsTest; AuthNavigationTest; AuthIntegrationTest |

Одна попытка определяет start/terminal с общим OperationId, retry получает новый ID.
Reporting errors, consent и queue policy могут исключить отправку; delivery не гарантируется.
LoginUseCase.loginEmail/loginGoogle/loginApple и RegisterUseCase.createAccount
возвращают бизнес-Boolean после private prepare; failure подготовки — failure
полной попытки. Отдельных preparation операций/схем нет. SDK cancellation
определяется типом, coroutine cancellation — Interrupted.

Splash принимает решения через SplashDirection. Ошибка профиля сохраняет
бизнес-fallback; SessionCheck completed означает завершение блока, включая этот
fallback, не подтверждённое восстановление сессии. Необработанная ошибка даёт failed
и сохраняет error pipeline. Reporting Exceptions не блокируют block/result,
transition/navigation или lifecycle; caller cancellation/fatal Error сохраняются.
Closed flow не переоткрывается, duplicate transition не меняет attribution,
Exit deduplicated.

## Атрибуция и persistence

| EntryPoint | Триггер | Origin |
|---|---|---|
| SessionCheck (internal) | Создание Splash | absent |
| LaunchWithoutSession | Splash получил null token | auth_splash |
| TokenAbsent | Root получил null token вне Auth | absent |
| SessionCheckFailed | Splash получил profile load failure | auth_splash |
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
  охватывают полный login/register. Setup схемы удалены. Session completed/failed
  v2 без outcome/stage; остальные пять схем v1. Прежние server-only timings нельзя
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
- SplashViewModelTest: actual requests/navigation/fallback/sequential loads/cancellation.
- RetainedAnalyticsTest: actual helper type-key retention/latest save/validation order.
- ScreenAnalyticsTest: actual lifecycle callbacks/reporting errors и DI singleton
  с независимыми owners.

Наличие тестов не означает, что они запускались или покрывают весь UI runtime.
Actual commands/results/skips и app/platform compilation относятся к текущему
verification report. Матрица фиксирует подключённые факты, не Firebase delivery.
