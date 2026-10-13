# Авторизация: текущее покрытие аналитики

Scope 2026-10-13: typed stateless API в VM и общий foundation screen hook. Стандарт —
[feature-standard](feature-standard.md); история и команды — [todo](todo.md).
Последний полный проход — [итоговый аудит](authorization-review.md), включая
бизнес-исправления, providers, безопасность логов и границы end-to-end готовности.
Прежние scenario/retention/provider проверки относятся к своим реализациям.
Последующий app-shell проход добавил iOS StateKeeper checkpoint без изменений
analytics events и feature ключей. Текущая политика и проверки —
[iOS StateKeeper](authorization-review.md#повторный-аудит-ios-statekeeper).
Ниже записи об отсутствии iOS bridge относятся к прежним датированным проходам.
Android/iOS builds, Gradle/KSP/generated DI, rendering/native SDK runtime
и Firebase не входят в текущий запуск.

## Подключение

- Один AuthAnalytics: trackEvent(AuthEvent), generic trackOperation(AuthOperation,
  block), presentScreen(AuthScreen, reason=Initial). Он stateless @Single.
- API — AuthAnalytics + models/ для AuthEvent/AuthOperation/AuthScreen и нужных
  credential/method/picker/experience enums. Общие InputStatus/InputKeyboardAction/
  ControlAction — common/models. Нет capability interfaces/action wrappers.
- Восемь public enums не раскрывают wire-коды. Internal AuthEncoding содержит
  exhaustive mappings; field/action keys ограничены InputField/InteractionAction.
  Schema names/versions и wire values сохранены.
- Implementation — internal AuthAnalyticsImpl, AuthPayloadEvent, AuthEventMatrix,
  AuthOperationEvents, typed schema/encoding models; public только annotated module.
  Нет UI/domain/services/SDK/core dependencies, flow state или classifiers.
- UI analytics/ содержит только AuthInputMapping. Все typed events/operations и
  screen declarations принадлежат VM. Component retainedInstance resolves
  analytics=getKoin().get() как остальные deps; без analytics constructor params.
- Все восемь отдельных экранов имеют обязательный screenPresentation, вызывая presentScreen с
  fixed AuthScreen и foundation reason. BaseComponent existing doOnCreate calls
  internal VM.bindScreen(lifecycle); ScreenAnalytics владеет Initial/Restore/close.
  Graph null hook означает отсутствие binding и lifecycle-service resolution.
  VM не получает Lifecycle/ComponentContext и не наблюдает activationFlow ради views.
- Обычная Component/retainedInstance/ViewModel ownership сохранена. Analytics не
  использует StateKeeper/InstanceKeeper keys/holders/snapshots. Существующие route
  FlowId/entryPoint обслуживают navigation identity и не читаются аналитикой.
- Generated modules подключаются shared; platform sink/consent/bootstrap unchanged.
  Custom catalog содержит 22 schemas: 10 interaction + 12 operation.

## Маршруты и показы

| Route | Роль / AuthScreen | Leaf VM hook |
|---|---|---|
| AuthComponent host | graph, без отдельного screen_view | AuthViewModel: отсутствует |
| AuthRouter.Splash | leaf / Splash | SplashViewModel.screenPresentation |
| AuthRouter.AuthProcess | graph, без отдельного screen_view | AuthProcessViewModel: отсутствует |
| AuthProcessRouter.Login | leaf / Login | LoginViewModel.screenPresentation |
| AuthProcessRouter.Registration | leaf / Registration | RegistrationViewModel.screenPresentation |
| AuthRouter.ProfileCreation | graph, без отдельного screen_view | ProfileCreationViewModel: отсутствует |
| ProfileCreationRouter.User | leaf / User | UserViewModel.screenPresentation |
| ProfileCreationRouter.Experience | leaf / Experience | ExperienceViewModel.screenPresentation |
| ProfileCreationRouter.ExcludedMuscles | leaf / ExcludedMuscles | ExcludedMusclesViewModel.screenPresentation |
| ProfileCreationRouter.MissingEquipments | leaf / MissingEquipments | MissingEquipmentsViewModel.screenPresentation |
| ProfileCreationRouter.Completed | leaf / Completed | ProfileCompletedViewModel.screenPresentation |

Проверены все 11 Component→VM→Render цепочек: 8 leaves с fixed screenPresentation
и analytics DI, 3 graph VM с default null. Наличие ViewModel, файла *Screen.kt или
BaseComposeScreen само по себе не делает контейнер самостоятельным analytics screen.
Auth/AuthProcess показывают child stack; ProfileCreation добавляет общий
неинтерактивный индикатор прогресса поверх текущего child. Отдельной страницы
у этих контейнеров нет. Их screen_view дал бы дополнительные показы и конкурирующий
screen context при том же пользовательском экране. Если у контейнера появится
самостоятельная страница или действие, покрытие оценивается по этой новой поверхности.

8 leaves, один общий bind в foundation. Feature provider/route matrix/integration
module удалены; Components/routes/factories/Render не содержат analytics wiring.
Первый RESUMED binding — Initial,
следующие — Restore; pause/destroy closes presentation. Новый binding начинает
Initial без process-restored attribution. Referrer/Action/Back не угадываются.
Graph/container screen_view не создаётся; destroy не считается user exit.

## Callbacks и typed facts

44 существующих Contract callbacks и два picker results сохранены.

| Leaf | AuthEvent family / safe facts |
|---|---|
| Splash | Back |
| Login | InputChanged/Focused/KeyboardAction(Email/Password), InputCleared(Email); password visible Boolean; Submit(Method), Register, Back |
| Registration | InputChanged/Focused/KeyboardAction(Email/Password), InputCleared(Email); password visible Boolean; Submit, Back |
| User | NameInputChanged/Focused/Cleared/KeyboardAction; PickerOpened/Confirmed(ProfilePicker.Height/Weight, status); Control(Next/Back) |
| Experience | Selected(ProfileExperience), Control(Next/Back) |
| ExcludedMuscles | GroupSelected, SelectionChanged(selected, count), Control(Next/Back) |
| MissingEquipments | GroupSelected, SelectionChanged(selected, count), Control(Next/Back) |
| Completed | Back; создание profile запускает init, submit control отсутствует |

Constructors содержат прямые enum/status/Boolean/count, не InputAction/SelectionChange
wrappers. Fixed Name не выбирается caller. Feature facts не реализуют toolkit
AnalyticsEvent: internal matrix создаёт wire payload. Invalid credential/picker
field combinations ограничены типами, без runtime constructor guards для этих пар.

Text InputChanged emits только когда existing old/new InputStatus отличаются.
Cache/«первый edit всегда виден» больше нет; Empty→Empty и другие равные статусы
не создают события. Picker confirmations/clicks/selections всегда distinct.
Это изменение population событий ввода, даже при прежнем payload/schema v1;
новые и прежние объёмы не считать напрямую сопоставимыми.

InputPassword не содержит Clear control: password InputCleared caller отсутствует
и событие не выдумывается. IME Next/Done наблюдают keyboard action, не запускают
submit. Для текстовых полей наблюдается смена validation status, а не каждый
символ/равный status; это намеренное покрытие текущего простого API.

Business eligibility/busy guards стоят до intent event. Analytics не выбирает
validation/доступность controls. Rejected Login.Email/Registration.Submit/User.Next/
Experience.Next не создают accepted intent или operation events. В прежнем проходе
четыре event calls перенесены после прежних guards без изменения payload/schema;
прежние counts, включавшие rejected callbacks, не считать сопоставимыми напрямую.
Последний business audit добавил Logout loader и guards User.Back/Next/picker opens:
повторное или конкурирующее действие во время logout не создаёт event/dialog/navigation.
User continue disabled при busy или любом невалидном Name/Weight/Height.
Neutral optional Input callbacks сохраняют focus/clear/IME callback behavior.
Password keyboard исправлен на Password/None/autoCorrectEnabled=false.
Raw text/length/name/email/password/height/weight,
selected entity IDs и exception text в payload не передаются. Programmatic Back
после dialog dismiss не вызывает instrumented user Back callback.

## Цельные операции

| AuthOperation | Бизнес-блок | Wire family |
|---|---|---|
| Login(Method) | Credentials + complete LoginUseCase.loginEmail/loginGoogle/loginApple | auth_attempt_started/succeeded/failed/interrupted |
| Register | Complete RegisterUseCase.createAccount с private preparation | auth_attempt_*; Method.Email, action Register |
| SessionCheck | Только hasProfile при наличии token, в Splash safeLaunch | auth_session_check_started/completed/failed/interrupted |
| CreateProfile | CreateProfileUseCase.execute + setHomeWelcomeStatus(PendingCelebration) | profile_operation_started/completed/failed/interrupted; action Create |
| Logout | Complete LogoutUseCase.execute из User.Back | profile_operation_*; action Logout |

Toolkit decorator задаёт OperationId/duration/start-terminal. Original T/errors/
cancellation сохраняются; navigation/retry остаются снаружи и решаются бизнесом.
Completed означает завершение caller block, не интерпретированный telemetry outcome.
SDK cancellation отдельно не классифицируется: обычный exception даёт failed;
выброшенная CancellationException даёт interrupted и пробрасывается.

Splash сохраняет один AppContent safeLaunch, sequential muscles→equipment для
no-session, cached-profile short circuit и обычный error/dialog/on-dismiss retry.
Login/register use cases не делятся на preparation stages. Profile selection
reconciliation/busy guards/navigation order сохранены. Reporting Exceptions не
мешают бизнесу; fatal Error не превращается в продуктовый failure.

## Схемы и сознательные omissions

Interaction schemas/keys/codes v1 сохранены. 12 operation schemas используют
старые имена, duration/action/method fields. failure_category удалён;
failed versions: auth_attempt_failed=5, auth_session_check_failed=4,
profile_operation_failed=2. Остальные operation versions unchanged.
Данные/отчёты по failed v5/v4/v2 не объединять с прежними категориями без явного
решения; version bump не мигрирует старые отчёты.

Удалены шесть flow start/exit/step-change schemas и auth_attempt_cancelled.
Не собираются exact flow funnels/process-restored continuity/referrers/entry origins,
granular error categories/provider-cancel outcomes. Route identity остаётся бизнес-
навигацией. Reference loads, internal preparation stages, external SDK/system UI,
launch attribution и dedicated picker/error dialog views/cancel/close не добавлены.
8 leaf coverage не означает покрытие других фич. Consent default/core queue/SDK
adapters не менялись; Firebase delivery не проверяется этим scope.

## Текущее ревью

Проверены DI, типизированные маршруты, Component → ViewModel → Render,
показы всех восьми самостоятельных экранов и отсутствие container views.
Business операции остаются цельными; reporting не определяет результат,
навигацию, retry или доступность пользовательских действий.

StateKeeper сохраняет latest nonsecret draft каждого владельца через локальный
PersistenceKey в companion object Component. Password/token/runtime данные
не попадают в архив. Retained VM имеет приоритет над restored snapshot;
Back/pop удаляет локальный draft, принятые родителем значения остаются в parent.
Ключи и serializer wire shape сохранены. iOS оболочка использует background
checkpoint, описанный в [итоговом аудите](authorization-review.md#ios-statekeeper-app-shell-интеграция).

Последние проходы выполнены без Android/iOS builds, Gradle/KSP,
UI/native runtime и Firebase delivery.
