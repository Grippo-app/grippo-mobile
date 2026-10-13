# Profile: показы экранов

Scope: StateKeeper и показы всех самостоятельных экранов Profile. Состояние
сохраняется по [inventory и проверкам](../profile/state-preservation.md).
Этот проход добавляет только screen telemetry через существующий foundation hook.
Android/iOS builds, Gradle/KSP, UI/native runtime и Firebase исключены.

## Routes → surfaces → owners

| Route | Surface / Component | VM | ProfileScreen / hook |
|---|---|---|---|
| Profile root | ProfileScreen renders child stack / ProfileComponent | ProfileViewModel | null; контейнер без отдельного показа |
| Menu | ProfileMenuScreen / ProfileMenuComponent | ProfileMenuViewModel | Menu |
| Body | ProfileBodyScreen / ProfileBodyComponent | ProfileBodyViewModel | Body |
| Goal | ProfileGoalScreen / ProfileGoalComponent | ProfileGoalViewModel | Goal |
| Experience | ProfileExperienceScreen / ProfileExperienceComponent | ProfileExperienceViewModel | Experience |
| Muscles | ProfileMusclesScreen / ProfileMusclesComponent | ProfileMusclesViewModel | Muscles |
| Equipments | ProfileEquipmentsScreen / ProfileEquipmentsComponent | ProfileEquipmentsViewModel | Equipments |
| Settings | ProfileSettingsScreen / ProfileSettingsComponent | ProfileSettingsViewModel | Settings |
| Social | ProfileSocialScreen / ProfileSocialComponent | ProfileSocialViewModel | Social |

Каждый leaf VM получает один ProfileAnalytics через существующий retainedInstance
Component. Единственный screenPresentation передаёт соответствующий ProfileScreen
и reason из foundation. Feature не хранит lifecycle, не вызывает bind и не вводит
analytics keys/providers/jobs. Container не разрешает ScreenAnalytics.

Foundation даёт Initial при первом resume данного binding, Restore при следующем;
pause/destroy закрывает presentation. Новый Component после archive restoration
получает новый binding с Initial. Сохранение draft не вызывает показов или callbacks.
При восстановлении сохраняется экран, а не telemetry provenance.

## API, encoding и DI

Один stateless ProfileAnalytics с presentScreen в analytics-api/profile; plain
ProfileScreen находится в models/. Internal ProfileAnalyticsImpl и exhaustive
ProfileEncoding находятся в analytics/profile. Стабильные profile_* коды
используют существующие typed ScreenId и built-in screen_view/schema.
Нового catalog/schema registration для screen-only scope не требуется.

ProfileAnalyticsModule аннотирован @Module/@ComponentScan; реализация использует
@Single(binds = [ProfileAnalytics::class]). Shared подключает generated .module.
UI зависит от analytics-api, а implementation — только от него и toolkit API
транзитивно. Android/iOS используют эту common DI-композицию.

## Controls, inputs, operations

| Surface | Existing behavior | Instrumentation этого scope |
|---|---|---|
| Menu | Открытие Body/Goal/Experience/Muscles/Equipments и Settings/Social/Debug | Только показ Menu; control clicks не подключены |
| Body | Height/weight picker open/confirmation, Apply, Back; User/history startup | Только показ Body; значения тела не входят в payload |
| Goal | Primary/secondary/date picker open/confirmation, personalization toggle, Save, Back; Goal startup | Только показ Goal; даты/выбор не входят в payload |
| Experience | Selection, Apply, Back; User startup | Только показ Experience |
| Muscles | Group/item selection, Apply, Back; catalogs/exclusions startup | Только показ Muscles; entity IDs не входят в payload |
| Equipments | Group/item selection, Apply, Back; catalogs/exclusions startup | Только показ Equipments; entity IDs не входят в payload |
| Settings | Logout, delete confirmation/accepted delete, Back | Только показ Settings; confirmation dialog telemetry отдельно |
| Social | Открытие URL, Back | Только показ Social; URLs не входят в payload |

Control/input/operation и dialog presentation events явно остаются вне выбранного
scope. Пустых event/operation моделей и fake transitions нет. Screen payload содержит
только bounded screen/reason, без business drafts и telemetry-owned navigation.
Ordinary presentation/close Exceptions изолируются foundation; fatal Errors
сохраняются. Показы не участвуют в синхронизации черновика с профилем: эта политика
остаётся business-owned и описана в [StateKeeper inventory](../profile/state-preservation.md).

## Review и проверки

- [x] Сверить восемь реальных rendered leaf routes и container null по исходникам.
- [x] Проверить exhaustive encoding, typed navigation, reason, DI и отсутствие
  custom schemas, SDK/domain/UI dependencies и business/state изменений.
- [x] Self-review → independent review → исправления → повторное review.
- [x] Проверить actual VM/foundation lifecycle: Initial/Restore, pause/destroy,
  отсутствие container/duplicate views, reporting isolation и сохранение draft.
- [x] Повторить StateKeeper regressions, строгую direct JVM-компиляцию и source/SHA
  gates; валидировать skills/docs/scope и записать реальный результат.

Проверка подключения показов, 2026-10-13:

- Self-review и два независимых review завершены. Уточнён стандарт: ошибки
  presentScreen/close изолирует foundation, feature implementation передаёт
  исходный presentation без дополнительных обёрток. Повторное review без замечаний.
- Direct cached Kotlin JVM compiler: strict explicit API и Werror для двух API
  файлов, трёх implementation файлов и 141 source production slice (128 настоящих
  исходников репозитория + 13 host fixtures). Test slice: Werror, шесть настоящих
  test/fixture файлов + два host fixtures. Source/artifact SHA проверены.
- JUnit: **38/38 PASS** — 22 StateKeeper regressions, три прежних routing теста,
  семь новых UI/lifecycle сценариев и шесть module/API тестов. Root независимо
  повторил запуск на финальных артефактах: 38/38 PASS.
- Восемь настоящих routes проверены через Component → retained VM → foundation
  lifecycle: один Initial, закрытие при pause/destroy, Restore при следующем
  resume, сохранение без повторного view. После archive → JSON roundtrip → destroy
  → fresh Component показывается только активный экран; Back восстанавливает
  черновик предыдущего экрана и создаёт его обычный первый показ.
- Recording toolkit Analytics получает typed NavigationContext; проверены
  уникальные stable IDs, default/explicit reason и отсутствие draft/flow/referrer.
  Ordinary presentation/close Exceptions не мешают редактированию, сохранению и
  следующему resume; fatal Errors сохраняются. Прямой Profile API прозрачен для
  ошибок и возвращает исходный presentation.
- Искусственный показ Profile container только во временной production copy
  компилируется и выявляется финальными тестами: 6 failures. Worktree не изменялся.
- 80 Kotlin/KTS файлов проверены через PSI: syntax errors и named local functions
  отсутствуют. Нормализованное сравнение подтверждает: изменения восьми VM и
  восьми Component — только hooks/injection; бизнес-код, State модели, persistence
  bindings и container остались прежними. Skills/YAML/local links проверены.

Evidence:
`/var/folders/gs/p9xpr8w95s3g6tcyx10tqbc80000gn/T/profile-persistence-jvm-20261013-xpu9_udr/profile-with-screens/`:
четыре `*.sources.json`, `tests.json`, `root-junit.log`, `root-verification.json`,
`screen-mutation-verification.json` и `mutations/container-screen/junit-final-tests.log`.
Scope/PSI/skills evidence:
`/var/folders/gs/p9xpr8w95s3g6tcyx10tqbc80000gn/T/profile-analytics-20261013-kzkwoodq/`.

Gradle/KSP, Android/iOS builds, UI/native runtime и SDK/Firebase не запускались.
Host fixtures заменяют generated resources, rendering с fail-fast сигнатурами,
generated module composition и native date formatting. Common DI-композиция
проверена по исходникам; generated KSP и platform runtime этими тестами не подтверждены.

Последующее упрощение draft synchronization удаляет флаги редактирования из пяти
State, сохраняя данные черновика и catalog identity. Analytics impact: hooks,
screen IDs, lifecycle, DI, reporting isolation и event population не меняются.
Controls/inputs/operations по-прежнему вне scope. Последующий проход удаляет
из пяти ViewModel также runtime flags/source caches и оставляет черновик в State;
hooks, события и аналитический API не меняются. Актуальные проверки — в
[StateKeeper record](../profile/state-preservation.md).
