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

## Review

- Сверены восемь rendered leaf routes, fixed ProfileScreen и default null контейнера.
- Проверены exhaustive encoding, reason, common DI-композиция и отсутствие
  custom schemas, SDK/domain/UI dependencies и business/state изменений.
- Self-review → independent review → исправления → повторное review завершены.
- Foundation owns Initial/Restore и close; save не создаёт views или callbacks.
  Ordinary presentation/close Exceptions изолирует foundation; fatal Errors
  сохраняются. Feature implementation возвращает исходный presentation.
- Проверены Kotlin syntax, отсутствие named local functions, ссылки и metadata.
  Изменения восьми ViewModel/Component при подключении показов ограничены
  hooks/injection; State, persistence bindings и business requests сохранены.

Последующее упрощение draft synchronization удалило флаги редактирования,
runtime flags/source caches из пяти owners, сохранив данные черновика и catalog
identity. Hooks, screen IDs, lifecycle, DI, reporting isolation и event population
не меняются. Controls/inputs/operations по-прежнему вне scope.
Актуальная политика — в [StateKeeper record](../profile/state-preservation.md).

Gradle/KSP, Android/iOS builds, UI/native runtime и SDK/Firebase не запускались.
