# Авторизация: текущее покрытие аналитики

Scope 2026-10-13: typed stateless API в VM и общий foundation screen hook. Стандарт —
[feature-standard](feature-standard.md); история и команды — [todo](todo.md).
Прежние scenario/retention/provider проверки относятся к своим реализациям.
Актуальные изолированные JVM/VM проверки перечислены ниже. Android/iOS builds,
Gradle/KSP/generated DI, rendering/native SDK runtime и Firebase не входят
в текущий запуск.

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

Existing eligibility/busy guards стоят до intent event; analytics не меняет
validation/доступность controls. Rejected Login.Email/Registration.Submit/User.Next/
Experience.Next не создают accepted intent или operation events. В этом проходе
четыре event calls перенесены после прежних guards без изменения payload/schema;
прежние counts, включавшие rejected callbacks, не считать сопоставимыми напрямую.
Neutral optional Input callbacks сохраняют default
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

Аудит после переименования в Registration — 2026-10-13:

- Independently reviewed naming, all 11 MVI/route/Component/VM/Render chains,
  44 leaf callbacks, 2 picker results, typed events/operations, schemas and DI.
  Eight leaf hooks match AuthScreen; three containers keep null. Old Credential
  screen names/paths and feature providers/retention/manual bindings are absent.
- Removed unused authorization serialization compiler plugin and dateUtils
  dependency; datetime belongs to commonTest. serialization.core stays in
  commonMain for childStack/route serializer signatures. screen-api still owns
  its serialization plugin and generated route serializers.
- LogoutUseCase now unwraps the mandatory local settings clear result. Its original
  failure reaches safeLaunch before token logout/navigation; analytics reports failed.
  Remote push deletion remains best effort for ordinary returned failures, preserving
  local logout when the network is unavailable. No new wrappers/jobs/recovery.
- Fresh isolated JVM compilation/tests: Auth 19 + UI 44 = **63 passing tests**.
  UI contains 39 repository cases and 5 temporary cases: Login (2), AuthProcess
  lifecycle (1), and the affected ProfileSettings logout caller (2).
  Both local-clear regressions fail against the pre-fix use case and pass after
  the fix; remote cleanup failure still permits a successful logout.
- Independent review of production changes and regression tests has no blockers.
  Existing profile-draft limitation remains outside this cleanup: parent VM state
  is retained in memory, while restored child routes do not carry the entered draft.
  Process recreation can therefore lose data and return Completed to an earlier step.
  This audit does not claim that profile draft restoration is complete.

Fresh logs/scripts, red/green results, inventory and baseline:
`/tmp/auth-registration-audit-20261013-rch4oat_/`.
Android/iOS builds, Gradle/KSP/generated DI, rendering/native SDK runtime and Firebase
were not run. JVM fixtures substitute generated UI/resources/platform signatures
and test DI composition; dependency configuration is checked by source review.

### Предыдущая проверка покрытий

Повторный аудит 2026-10-13 после обязательного screen coverage gate:

- Все 11 actual route/Component/retained VM/Render цепочек сверены независимо:
  8 отдельных экранов с matching typed hook, 3 контейнера с default null.
  Source inventory проверяет bijection AuthScreen ↔ hook; provider/retention/manual
  bind/activation observers в feature отсутствуют. Functional production defects
  не найдены; исправлены устаревший foundation KDoc и неточная coverage формулировка.
- Все 44 реальных Contract callbacks и 2 picker results прослежены до события;
  guards, whole-operation boundaries, payload/privacy, DI/catalog и сохранение
  результата/ошибок проверены по исходникам. Отсутствующие UI controls и намеренные
  omissions перечислены выше; общий код не создаёт выдуманных user events.
- Усилены repository Registration и selection tests: exact event order/count,
  focus/clear/IME/visibility/status/field/screen, group callbacks, selected/count
  после изменения state и отсутствие events от catalog reconciliation.
- Repository ProfileCreationComponentTest проходит все 5 actual steps: Initial и
  Restore каждого, no container views, close once, Next/selection/picker/Back
  facts, pending CreateProfileUseCase и Home после полного business completion.
- Свежая isolated JVM compilation/tests: Auth 19 + toolkit/API/core/foundation 62
  + Auth UI 40 = **121 passing tests**. UI: 37 repository tests и 3 temporary JVM
  tests — 2 Login email/guards/reporting-isolation и 1 actual AuthProcessComponent
  Login→Registration→hardware Back, pause/resume/destroy без parent views.
  Скомпилирован 221 production/fixture source и 12 test sources.
  Native provider methods и Render не вызываются; substitutions/ограничения
  прежнего harness ниже остаются применимыми. DI extensions — test composition,
  поэтому generated KSP wiring проверено только source review.

Свежие logs/reports/command scripts, screen inventory и baseline:
`/tmp/auth-coverage-review-20261013-rjz9am53/`.
Android/iOS builds, Gradle/KSP/generated DI, rendering/native SDK runtime и Firebase
не запускались. Обычная reporting isolation проверена в разрешённом JVM scope.

### Предыдущий полный проход

Строгий review 2026-10-13: три независимых направления (API/matrix, UI/lifecycle,
core/adapters), исправления и повторное перекрёстное ревью. Сравнение с началом
прохода подтверждает прежние business bodies/guards и wiring платформ.

- Cached Kotlin 2.4.20 K2JVMCompiler с strict explicit API: toolkit API (37 файлов),
  common API (3), Auth API (8), implementation (12), core с JVM lock actual и
  foundation (14 actual production sources). Compose/Essenty/Decompose dependencies
  взяты из локального cache. Auth — OK (19 tests), toolkit/API/core/foundation —
  OK (62 tests), всего 81. Проверены все public enum wire values, schema/key
  identity/versions, обычные reporting Exceptions, cancellation, очередь/concurrency,
  fairness, shutdown во время SDK callback и screen lifecycle/null graph hook.
- Actual Auth UI commonTests — OK (36 tests); две изолированные проверки actual
  LoginViewModel дают ещё 2, итого UI — 38 и общий результат — 119.
  Скомпилирован 221 production/fixture source и 11 test sources. Исполняются
  настоящие VMs, use cases, validation, state/mappers, Components/navigation/lifecycle.
  Rejected/eligible Login, Registration, User и Experience проверены отдельно;
  busy guard и reporting-failure isolation Login не меняют бизнес/navigation.
  Возврат четырёх прежних event positions в temporary baseline artifact вызывает
  ровно четыре AssertionError; текущий artifact повторно проходит 38 tests.
- Изолированный UI harness подменяет только generated resources/visual assets,
  неисполняемые Render/platform composition signatures, Google/NativeContext JVM
  actuals для email-only тестов и KSP extensions явной test DI composition настоящих
  реализаций. Native credential actions/rendering не вызываются. Native Looper
  diagnostic Decompose отключён через его settings для JVM; это не проверка
  native main thread. Actual Apple Android no-platform implementation используется
  только как dependency. Generated DI и platform SDK корректность этим не доказаны.
- External consumer без friend paths: protected hook override компилируется;
  internal binder/protected presenter/internal implementation закрыты.
  Public fact wire getters отсутствуют; неверные credential/picker combinations
  и другой enum в typed key дают ожидаемые compile errors (6 negative cases).
- Kotlin PSI: 239 профильных файлов, syntax errors = 0, named local functions = 0.
  Четыре production VM отличаются только положением event call после прежних
  guards; остальные business/data/platform wiring sources сохранены. Native debug
  opt-in перенесён в общую convention configuration, без изменения debug policy.
- Skill frontmatter/metadata YAML, maintained Markdown links/fences и
  git diff --check прошли. Canonical skill/standard уточняют plain public enums,
  internal exhaustive encoders/closed keys и eligible/rejected callbacks.

Команды и отчёты — в [todo](todo.md). Прежние 78/76/16 относятся к своим этапам.
Не запускались Gradle/KSP/generated modules/catalog composition, Auth Screen.kt
rendering, native SDK runtime, Android/iOS builds или Firebase. Изолированные
JVM/Component fixtures не подтверждают платформенный UI runtime. Consent policy
остаётся отдельной задачей: production сбор выключен до явного Granted.
