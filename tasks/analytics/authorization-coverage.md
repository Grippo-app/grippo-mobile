# Авторизация: текущее покрытие аналитики

Scope 2026-10-13: typed stateless API в VM и общий foundation screen hook. Стандарт —
[feature-standard](feature-standard.md); история и команды — [todo](todo.md).
Прежние scenario/retention/provider проверки относятся к своим реализациям.
Новый hook проверен изолированно: 78 JVM tests и три negative visibility cases.
Android/iOS builds, Gradle/KSP/generated DI, actual Auth UI/native runtime и Firebase
не входят в текущий запуск.

## Подключение

- Один AuthAnalytics: trackEvent(AuthEvent), generic trackOperation(AuthOperation,
  block), presentScreen(AuthScreen, reason=Initial). Он stateless @Single.
- API — AuthAnalytics + models/ для AuthEvent/AuthOperation/AuthScreen и нужных
  credential/method/picker/experience enums. Общие InputStatus/InputKeyboardAction/
  ControlAction — common/models. Нет capability interfaces/action wrappers.
- Implementation — internal AuthAnalyticsImpl, AuthPayloadEvent, AuthEventMatrix,
  AuthOperationEvents, typed schema/encoding models; public только annotated module.
  Нет UI/domain/services/SDK/core dependencies, flow state или classifiers.
- UI analytics/ содержит только AuthInputMapping. Все typed events/operations и
  screen declarations принадлежат VM. Component retainedInstance resolves
  analytics=getKoin().get() как остальные deps; без analytics constructor params.
- Восемь leaf VM override optional screenPresentation, вызывая presentScreen с
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
| AuthRouter.Splash | leaf / Splash | SplashViewModel.screenPresentation |
| AuthRouter.AuthProcess | graph, без view | отсутствует |
| AuthProcessRouter.Login | leaf / Login | LoginViewModel.screenPresentation |
| AuthProcessRouter.Registration | leaf / Registration | CredentialViewModel.screenPresentation |
| AuthRouter.ProfileCreation | graph, без view | отсутствует |
| ProfileCreationRouter.User | leaf / User | UserViewModel.screenPresentation |
| ProfileCreationRouter.Experience | leaf / Experience | ExperienceViewModel.screenPresentation |
| ProfileCreationRouter.ExcludedMuscles | leaf / ExcludedMuscles | ExcludedMusclesViewModel.screenPresentation |
| ProfileCreationRouter.MissingEquipments | leaf / MissingEquipments | MissingEquipmentsViewModel.screenPresentation |
| ProfileCreationRouter.Completed | leaf / Completed | ProfileCompletedViewModel.screenPresentation |

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
| Login | InputChanged/Focused/Cleared/KeyboardAction(CredentialField); password visible Boolean; Submit(Method), Register, Back |
| Registration | Credential input/status/focus/clear/IME, password visible; Submit, Back |
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

Existing eligibility/busy guards стоят до intent event; analytics не меняет
validation/доступность controls. Neutral optional Input callbacks сохраняют default
focus/keyboard/clear behavior. Raw text/length/name/email/password/height/weight,
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

## Текущая проверка

Cached Kotlin 2.4.20 K2JVMCompiler с strict explicit API скомпилировал toolkit API
(37 файлов), common API (3), Auth API (8), implementation (10) и 14 actual foundation
production sources с Compose compiler plugin и настоящими cached dependencies.

- AuthInteractionTest/AuthOperationsTest/AuthSchemaTest/AuthScreenTest/
  CommonInteractionModelsTest — OK (19 tests).
- Actual toolkit API/core + ScreenAnalyticsTest/ViewModelScreenAnalyticsTest —
  OK (59 tests), всего 78. Foundation проверяет null hook без resolution binder,
  retained VM с новым lifecycle, Initial/Restore/close и init navigation после bind.
  Последние два foundation test sources перекомпилированы, 9 tests прошли повторно;
  они входят в 78, а не прибавляются к ним. Unit-test DI constructs actual implementations.
- External consumer без friend paths: protected hook override компилируется;
  три попытки обращения к internal bindScreen/protected screenPresentation/internal
  implementation дают ожидаемый INVISIBLE_REFERENCE.
- Kotlin PSI: 203 файла, syntax errors = 0, named local functions = 0.
  75 source proofs подтверждают восемь прежних business VM bodies, 11 Component
  plumbing changes, 55 byte-identical UI state/contract/screen/direction/loader
  файлов и 44 callbacks. Два picker handlers unchanged apart from hook/imports.
  404 protected toolkit core/data/native source hashes unchanged. BaseVM business
  exact после снятия hook/imports, BaseComponent — после hook/necessary ordering.

Команды и отчёты — в [todo](todo.md). Предыдущие 76/16 относятся к provider wiring.
Не запускались AuthAnalyticsModuleTest/ScreenAnalyticsModuleTest и generated DI/
catalog composition, Gradle/KSP, compilation/runtime actual Auth feature VM/
Components/Compose screens, native SDK, Android/iOS builds или Firebase.
Изолированные actual foundation lifecycle tests не подтверждают platform UI runtime.
