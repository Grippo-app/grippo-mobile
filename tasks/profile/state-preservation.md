# Profile: сохранение состояния

Scope: вся feature `:ui-screen-features:profile`, существующие StateKeeper и
PersistenceKey, показы восьми самостоятельных экранов Profile через foundation.
Android/iOS builds, Gradle/KSP и native runtime исключены. Новых persistence
слоёв, providers, route replacements и глобального хранения не добавляем.

## План и критерии

- [x] Проверить все восемь экранов и root: user-owned значения, data-backed
  presentation и runtime state; сохранить только необходимый черновик.
- [x] Подключить пять owners к local restore/save с private bottom companion DRAFT,
  current serializer и typed initialState в retainedInstance factory.
- [x] Сохранять значения без флагов и предыдущих источников во ViewModel.
  Новый экран заполняется один раз; восстановленный полный снимок имеет приоритет,
  включая Empty/null. Поздние обновления источника не перезаписывают черновик.
- [x] Проверить empty-first catalogs, новые/удалённые IDs, точную дату цели,
  восстановленный Back/stack, Apply/Save business inputs и retained ownership.
- [x] Выполнить self-review → независимое review → fixes → review.
- [x] Перепроверить scope, ключи, source/artifact hashes и docs; записать реальные
  результаты и непроверенные platform границы.

## Inventory

| Owner | Preserve | Rebuild / runtime | Механизм | Restore/default и проверка |
|---|---|---|---|---|
| ProfileComponent | Child stack | Container State, jobs, callbacks | Existing ProfileRouter.serializer / ProfileComponent key | Fresh root, active child и back stack |
| Menu | Нет editable draft | User, goal, counts из data flows; loaders | Без дополнительного supplier | Fresh root, актуальный User из flow |
| Body | Height/weight | Current User, history; dialogs/jobs | ProfileBodyDraft / ProfileBodyState | Saved draft / default; первая загрузка, invalid/empty, Apply |
| Goal | Primary, nullable secondary, exact target, personalization | Current limits/catalogs; dialogs/jobs | ProfileGoalDraft / ProfileGoalState | Saved draft / initial(); точная дата, empty/null, уникальность и Save |
| Experience | Selected enum | Enum suggestions; loaders/jobs | ProfileExperienceDraft / ProfileExperienceState | Saved draft / default; первая загрузка, null/current User, Apply |
| Muscles | Selected group, selected/known IDs | Current muscle catalog/domain exclusions | ProfileMusclesDraft / ProfileMusclesState | Saved draft / default; два empty-catalog archives, группа, new/excluded IDs, Apply |
| Equipments | Selected group, selected/known IDs | Current equipment catalog/domain exclusions | ProfileEquipmentsDraft / ProfileEquipmentsState | Saved draft / default; два empty-catalog archives, группа, новые IDs, Apply |
| Settings | Нет editable draft | Confirmation callbacks, loaders/jobs | Без дополнительного supplier | Source assessment, маршрутизация |
| Social | Нет editable draft | Link callbacks | Без дополнительного supplier | Source assessment, маршрутизация |

Retained VM имеет приоритет над архивом. Каждый private DRAFT находится внизу
своего Component; restore вызывается внутри retainedInstance, один saveState
читает последнее state.value. Back/pop уничтожает неотправленный локальный
черновик; повторный вход получает свежие domain-данные.

Снимок сохраняет только значения, без hasEdited/selectionEdited. Во ViewModel
нет restorePending, previousSource/previousSourceSelection и теневых копий State.
Новый экран заполняется из первого доступного источника, включая текущий cache;
предварительно введённые отличающиеся значения сохраняются. Полный restored
snapshot сохраняет все значения, включая Empty/null. Поздние source updates
обновляют необходимые User/history/catalog данные, не заменяя поля открытой формы.
Это более простая политика по решению пользователя: текущий черновик, включая
нетронутые и равные источнику поля, может оставаться старее сервера. Историю
выбора, совпавшего с default до первой загрузки, не восстанавливаем.
Known IDs в каталогах сохраняются как идентичность знакомых элементов; это
позволяет различать снятый выбор и новые элементы, пережить empty cache и
сохранить принятые parent exclusions в авторизации.
Выбор известных IDs остаётся черновиком; актуальные исключения задают выбор
только новых IDs. Empty catalog не очищает known/selected IDs.
Goal.initial() задаёт текущую дату только для нового экрана: сериализуемый default
selectedTarget стабилен, поэтому фактическая дата не теряется при кодировании.

Passwords/tokens, User/history snapshots, catalogs, dialogs, jobs, focus и
analytics provenance в draft не сохраняются. Supplied archive и live retention —
разные механизмы; supported iOS background checkpoint сохраняет ту же feature
структуру, но arbitrary active-process kill не имеет гарантии восстановления.

## Analytics impact

Пользователь подтвердил StateKeeper и показы всех экранов Profile. ProfileAnalytics
добавлен как stateless screen-only API; Menu/Body/Goal/Experience/Muscles/Equipments/
Settings/Social используют по одному typed screenPresentation. Root — контейнер
с default null. Полная матрица и актуальные проверки — в
[Profile coverage](../analytics/profile-coverage.md).
Восстановление черновиков не вызывает пользовательские callbacks, не отправляет
операции и не генерирует синтетические действия. Свежий Component получает обычный
screen view из нового foundation lifecycle binding. Save не создаёт повторного
показа. Изменение Apply eligibility учтено: без текущего каталога callback не
исполняет операцию. Controls/inputs/operations не входят в выбранный analytics scope.

## Review

Self-review, независимое review и повторный проход завершены. Исправлен риск
отправки устаревших known IDs до загрузки каталога: Apply использует текущие
suggestions, а пустой каталог блокирует callback и кнопку. Goal использует
актуальные removing/adding вместо deprecated helpers.

Девять сохранённых флагов редактирования и пять private runtime baseline/cache
полей удалены. Body заполняет пустые поля при первом User и затем обновляет только
реальные User/history данные. Goal/Experience получают первое значение только
для нового draft через конечный Flow с Processing.Infinity: pause/resume не
заполняет форму повторно. Каталоги сохраняют выбор знакомых IDs, задают выбор новых
по актуальным исключениям и продолжают перестраивать suggestions.

Для совместимости с прежними архивами пять draft типов используют class-scoped
JsonIgnoreUnknownKeys. Известные значения сохраняются без устаревших флагов,
отдельного parser и fallback; неверные типы известных полей отклоняются.
Profile явно зависит от serialization-json, необходимого этим моделям.

Проверены typed keys, serializers, lazy suppliers, transient-поля, retained
ownership и отсутствие runtime flags/source copies/local functions.
Компоненты, DI, экранные hooks и последовательности business запросов при
упрощении синхронизации не менялись. Сопутствующая правка Body
`delay(500.milliseconds)` сохранена.

Android/iOS builds, Gradle/KSP, UI/native runtime и Firebase не запускались.
