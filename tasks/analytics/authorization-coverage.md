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

## Текущая проверка

### Повторный аудит сохранения состояния

Проверены все 11 владельцев: семь локальных drafts, 18 сохраняемых полей и семь
transient полей. Ключи остаются в нижних private companion objects; адреса и
serializers прежние. Auth/AuthProcess/ProfileCreation восстанавливают childStack,
Splash и Completed не получают отдельные suppliers для пустого State.

Исправлена потеря принятого выбора после Back/pop и повторного открытия шага:
ProfileCreation передаёт experience, excludedMuscleIds и missingEquipmentIds
новым дочерним Components как обычные initial business inputs. Локальный archive
имеет приоритет; без него каталог использует принятые исключения как known IDs,
поэтому пустая загрузка не стирает выбор, а остальные новые items выбираются
прежним reconciliation. State/VM/routes/use cases и analytics API не менялись.
Нового хранилища, metadata-файлов, providers или callbacks синхронизации нет.

Три новые repository regressions проверяют Back/pop, archive JSON roundtrip,
fresh Components/VMs, parent seed и второй restore более свежего локального выбора
поверх уже принятого parent. Каталоги проверяют empty → reload, без synthetic
events; возврат на Equipment не запускает отменённое создание профиля повторно.
На before production эти три сценария падают независимо; текущие **70 UI/domain/
error cases и 14 foundation cases прошли**, всего **84** без повторного счёта.

UI production/tests скомпилированы заново: 227 production/fixture inputs и 19 test
inputs. Неизменные prerequisites и foundation test bytecode использованы после
проверки актуальных source SHA и artifact SHA; foundation JUnit запущен заново.
Пять canonical документов согласованы с порядком retained VM → local archive →
accepted parent/route inputs → defaults. Skills, примеры, links/fences и exact
source scope проверены отдельно; ограничения iOS shell остаются прежними.
Android/iOS builds, Gradle/KSP, native UI/SDK, rendering и Firebase не запускались.
Артефакты и команды —
[todo](todo.md#повторный-аудит-сохранения-auth--2026-10-13).

### Предыдущий перенос ключей в компоненты

Foundation использует final invariant PersistenceKey<T> с internal address и
serializer. Каждый из семи владельцев — Login, Registration, ProfileCreation, User,
Experience, ExcludedMuscles и MissingEquipments — объявляет typed DRAFT в private
companion object в самом низу своего Component. Отдельные *Persistence.kt,
центральный AuthStateKeys и папка authorization/state удалены. restoreState(DRAFT)
и saveState(DRAFT) используют прежние archive addresses/serializers. State остаётся
слепком экрана/данных. VM/business/routes, retained factory, lazy supplier,
parent-before-children order и analytics hooks не меняются.

Свежая targeted JVM проверка после переноса: **67 существующих тестов прошли**.
Текущие auth Component/VM/domain sources скомпилированы заново: 227 production/fixture
inputs и 19 test inputs. Неизменные prerequisite foundation/API/analytics jars
переиспользованы только после проверки их artifact SHA и всех текущих source SHA.
Существующие KSP/resource/render/date/Google substitutes и exclusions прежние;
Android/iOS builds, Gradle/KSP, native UI, rendering и Firebase не запускались.
Все семь archive addresses/serializers и State snapshots сохранены; отдельные
metadata-файлы удалены. Canonical skill/examples и независимое exact-diff review
прошли, foundation API/helpers/VM/business/routes/analytics/tests не менялись.
Fresh artifacts и команды —
[todo](todo.md#ключи-сохранения-в-компонентах--2026-10-13).

### Предыдущая проверка размещения в отдельных файлах

Проверка предыдущего refactor: **81 существующий JUnit case прошёл** — 14 foundation
и 67 auth VM/Components/domain/error pipeline. Foundation и prerequisite APIs,
analytics implementation, UI sources/tests скомпилированы заново из текущего repo;
UI manifest содержит 234 production/fixture и 19 test inputs. Typed restoration,
retained priority, sibling isolation, JSON roundtrip/fresh Components и сохранение
parent/child drafts прошли. Generated KSP DI, rendering/date/Google platform runtime
заменены прежними explicit/fail-fast JVM fixtures; Android/iOS builds, native SDK UI,
Gradle и Firebase не запускались.

Внешний consumer PersistenceKey компилируется без friend paths; все шесть
неправильных type/visibility/finality usages отвергнуты по соответствующим compiler
diagnostics. Семь старых адресов/serializers и bytewise неизменные State подтверждены.
Скиллы и примеры актуальны: семь validators, 17 Markdown документов, 82 local links,
23 anchor, exact foundation drop-ins. Exact scope — 30 путей, без дрейфа VM/routes/
business/analytics. Команды и fresh artifacts —
[todo](todo.md#метаданные-сохранения-по-владельцам--2026-10-13).

### Предыдущий итоговый аудит auth

Свежий isolated JVM запуск: **157 JUnit cases прошли** — auth/common 19,
toolkit/API/core/foundation 67, actual auth UI/domain/affected caller/error pipeline
67, actual ResponseValidator четыре. UI/domain группа содержит шесть BaseViewModel
error cases; 14 foundation входят в 67 и повторно не считаются. Before UserVM/Loader
и before CreateProfile/Delete дают по три ожидаемых behavioral failures на новых
тестах. Before ResponseValidator падает только на новом fatal/non-Exception identity
regression, текущий набор из четырёх cases проходит.

CreateProfile проверяет required true outcomes до подготовки/welcome/Home.
Register existing-profile path загружает weight history перед goal/push; actual
полный FeatureApiModule строго компилируется и его Koin registration выполняет
оба profile paths. Delete не выполняется после failure обязательного clear.
User logout rejection/cancellation/loader tests проверяют реальные последствия.
UseCase results/errors/cancellation остаются бизнес-контрактами, analytics их наблюдает.

Actual BackendClient/TokenProvider/ClientLogger/DTO/parser/file-logger harness
проверил пять auth paths, корректные payload/header/result/error и отсутствие
секретов в транспортном логе. Before TokenProvider воспроизводит persisted bearer
prefix leak. Refresh runtime этот harness не проверяет; его неизменные ветви
сверены по exact before diff. Native fixes проверены по source/SDK contracts;
actual pure SHA256/PKCE и точный Password KeyboardOptions block проверены отдельно.

Ни platform builds, ни generated KSP DI, ни native/render/real sign-in, ни Firebase
не выполнялись. Production collection пока Unknown; iOS shell archive отсутствует;
соседний backend возвращает чувствительное Google verification error.message.
Все границы и конкретные исправления — в [review](authorization-review.md).
Fresh commands/logs/baseline: точный audit путь указан в review/todo.

### Предыдущий проход типизированных ключей

Типизированные ключи черновиков и общий Component API — 2026-10-13:

- Foundation owns final invariant StateKey<T>: stable archive address paired with
  the matching KSerializer<T>; both properties are internal. BaseComponent exposes
  protected restoreState(key) and saveState(key, supplier). These directly consume
  and register in the existing local StateKeeper; saveState registers a lazy
  supplier rather than writing immediately. No service/DI/provider, reflection,
  extra job, automatic VM serialization or new BaseComponent state generic.
- One internal authorization/state/AuthStateKeys matrix owns the same seven
  addresses/serializers. All seven draft owners use the typed methods. Private
  draft-key companions and repeated string/serializer bindings are gone. State
  classes, formatters, passwords/transient policy, VM/business bodies, routes,
  analytics schemas/hooks and parent-before-child restoration are unchanged.
- Deleted unused ComponentIdentifier/NoneIdentifier and BaseComponent's unused
  constructor parameter. Whole-project callers never supplied it; it provided
  neither result addressing nor archive identity. Docs/drop-in remove the false
  live-instance semantics. ResultKey and navigation childStack keys remain separate.
- Fresh strict JVM compilation: 14 foundation sources and 228 auth/fixture sources.
  **69 cases pass**: 14 foundation cases (five new) and all 55 auth/affected-caller
  cases. New actual-component regressions cover lazy latest snapshots, fresh VM
  without navigation replay, sibling contexts/reversed recreation, retained memory
  winning over an older archive, strict duplicate registration and literal legacy
  archive restore/resave. Legacy addresses and unchanged State serializers checked
  against this audit's exact before snapshot.
- An external consumer without friend paths compiles. Six negative checks reject
  another state's supplier/serializer, access to hidden address/serializer,
  external protected binding and extending StateKey. Canonical preservation and
  UI skills now require feature-local typed metadata and protected draft bindings.

Commands, logs, manifests and before snapshot:
`/tmp/component-state-keys-20261013-6f6p5hnb/`.
Android/iOS builds, Gradle/KSP/generated DI, rendered/native SDK runtime and Firebase
were not run. Existing isolated JVM substitutions remain in ui-jvm/tests.json;
foundation uses cached dependency bytecode and a test-only Decompose main-thread
check setting. Current iOS still supplies no saved-state archive bridge.

### Предыдущий полный аудит auth-состояния

Полный аудит сохранения состояния auth — 2026-10-13:

| Владелец | Сохраняем в локальном StateKeeper | Восстанавливаем из источника / оставляем только в памяти |
|---|---|---|
| Auth | Сериализованный AuthRouter/childStack | AuthState — object, отдельного draft нет |
| AuthProcess | Сериализованный AuthProcessRouter/childStack | AuthProcessState — object, отдельного draft нет |
| Login | Последний EmailFormatState: valid, invalid и empty; LoginDraft | Password transient → Empty; Google/Apple availability из текущих providers |
| Registration | Последний EmailFormatState, включая изменение/очистку prefill; RegistrationDraft | Password transient → Empty; route email только без сохранённого draft |
| Splash | Только существующая навигация | Startup token/profile/catalog источники; SplashState — object |
| ProfileCreation | Принятые name/weight/height/experience/excluded/missing IDs; ProfileCreationDraft | Parent draft восстанавливается до childStack |
| User | Последние Name/Weight/Height FormatState, включая незавершённый ввод; UserDraft | Живой picker/dialog закрыт; принятые значения остаются в draft |
| Experience | Текущий selected, в том числе до Next; ExperienceDraft | Suggestions пересоздаются из enum |
| ExcludedMuscles | Group, selected и known IDs; ExcludedMusclesDraft | Suggestions из прежнего Flow, начальная пустая выдача не стирает intent |
| MissingEquipments | Group, selected и known IDs; MissingEquipmentsDraft | Suggestions из прежнего Flow, начальная пустая выдача не стирает intent |
| Completed | Поля восстановленного parent | ProfileCompletedState — object; прежняя owned completion operation |

- Added the two missing credential-screen drafts through the existing Component
  consume/register pattern and typed initialState. Restored Invalid/Empty email
  wins over the route prefill; no per-edit route changes, persistence helpers,
  providers, new jobs or analytics lifecycle/state wiring. Profile drafts from
  the preceding fix remain intact. Auth has seven local draft suppliers.
- Password fields and Login provider flags are @Transient with safe defaults.
  Tests inspect the materialized StateKeeper payload, including inactive Login
  in the Registration Back stack, and verify secret omission and fresh empty
  password. Login recomputes provider availability; the isolated Google actual
  advertises true to distinguish recomputation from default false.
- Fresh strict explicit-API JVM compilation: 227 production/fixture sources,
  18 test sources, **55 passing UI/affected-caller cases** (48 repository and seven
  temporary JVM cases). Six new cases cover exact/cleared email, actual archive
  recreation, submit guards, Back, cancellation/no automatic replay and new
  whole operation after re-entering a password. Five actual-component regressions
  fail against this audit's pre-fix production snapshot; the separate new
  serializer check is excluded from that baseline run. No working-tree reverts.
- Canonical [state-preservation skill](../../.claude/skills/state-preservation/SKILL.md)
  requires a field inventory and fresh-component restoration verification for
  new/changed features. Entry points, UI workflow/recipe, planner/lookup and
  reviewer gate route there. Saved nonsecret drafts and current filters differ
  from rebuilt data/runtime secrets; presentation-only widget policy is separate.
- All eight leaf screen hooks, three null containers, existing route serializers,
  operation boundaries, DI and native shells remain unchanged. Save/restore adds
  no synthetic input events or screen presentations to current instances.

This audit's snapshots, commands and red/green reports:
`/tmp/auth-state-preservation-20261013-7u7aujdw/`.
No Android/iOS builds, Gradle/KSP, rendering, native SDK runtime or Firebase.
JVM substitutions are explicit in `ui-jvm/tests.json`. StateKeeper restores
supplied archives; current Android has that bridge, current iOS does not.
Arbitrary cold-start disk persistence and an iOS shell bridge are outside scope.

### Предыдущая проверка черновика профиля

Восстановление черновика создания профиля — 2026-10-13:

- Five existing Components save their business draft in their own StateKeeper:
  parent accepted values and each editable screen's latest inputs/selection.
  Fresh retained VMs receive initialState; parent restoration precedes childStack
  construction, so Completed receives the entered profile. No provider, generic
  BaseViewModel persistence, per-input navigation update or synchronization job.
- Serializable state preserves invalid NameFormatState, accepted weight/height,
  selected experience, known/selected catalog IDs and selected group. Suggestions
  are transient and rebuilt from existing Flows. Empty catalog emissions retain
  choices; nonempty updates select new IDs and reconcile removed IDs/groups.
  Next uses the saved known-minus-selected IDs even before the catalog arrives.
- The authorization serialization plugin is now required for these business-state
  serializers. Router contracts, DI, all eight leaf screen hooks and three null
  containers, event schemas and operation boundaries are unchanged. Draft saving
  emits no analytics, and restoration fabricates no input/selection events.
- Fresh strict explicit-API JVM compilation covers 227 production/fixture sources.
  **49 UI/affected-caller tests pass**: 44 repository cases and five temporary
  Login/AuthProcess/ProfileSettings cases. Five new repository cases perform
  StateKeeper.save → JSON roundtrip → destroy old lifecycle/InstanceKeeper → fresh
  context, and verify inputs, Back, both catalog drafts and exact CreateProfile
  inputs/completion/cancellation. All five fail on the pre-fix production snapshot.
- Persistence follows supplied saved state: Android already provides that bridge;
  the current iOS shell supplies none. This fixes draft restoration within a saved
  navigation tree, not disk persistence on arbitrary cold starts. Dialog sessions
  remain closed after recreation; accepted picker results belong to the draft.

Fresh scripts, before snapshots and red/green logs:
`/tmp/profile-draft-restoration-20261013-6gacna0_/`.
No Android/iOS builds, Gradle/KSP, rendering/native SDK runtime or Firebase.
JVM substitutions remain explicitly listed in `ui-jvm/tests.json`.

### Аудит после переименования в Registration

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
  At that audit, the profile-draft limitation remained outside its cleanup: parent VM state
  is retained in memory, while restored child routes do not carry the entered draft.
  Process recreation can therefore lose data and return Completed to an earlier step.
  The later draft-restoration fix is described in the current check above.

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
