# Profile: сохранение состояния

Scope: вся feature `:ui-screen-features:profile`, существующие StateKeeper и
PersistenceKey, показы восьми самостоятельных экранов Profile через foundation.
Android/iOS builds, Gradle/KSP и native runtime исключены;
iOS tests не возвращаем. Common/JVM проверки разрешены. Новых persistence
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
- [x] Выполнить реальные archive → destroy → fresh Component/InstanceKeeper
  common/JVM regressions, self-review → независимое review → fixes → review.
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
| Settings | Нет editable draft | Confirmation callbacks, loaders/jobs | Без дополнительного supplier | Source assessment, существующий тест маршрутизации |
| Social | Нет editable draft | Link callbacks | Без дополнительного supplier | Source assessment, существующий тест маршрутизации |

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

## Проверки и review

Первый проход, 2026-10-13: self-review, два независимых review и повторное review после исправлений.
Найден и исправлен риск отправки устаревших known IDs при Apply до загрузки
каталога: запрос использует текущие suggestions, пустой каталог блокирует
callback и кнопку. Deprecated Goal remove/add заменены на removing/adding.
Повторные review не нашли других дефектов сохранения.

Проверки исходников первого прохода:

- Direct cached Kotlin JVM compiler: strict API и Werror, 128 actual repo sources
  + 13 host fixtures. Включены реальные Profile Components/VMs/States/routes,
  сериализаторы, форматтеры, мапперы и бизнес-use-case. Зависимости foundation,
  Essenty/Decompose и предыдущие JVM artifacts проверены по source/artifact SHA.
- JUnit: **25/25 PASS** — 22 новых commonTest сценария восстановления и три
  существующих ProfileViewModel теста маршрутизации. Root независимо повторил
  запуск и проверил актуальность source/artifact hashes. Исправлен test Main
  dispatcher старых тестов; production dispatcher не изменён.
- Реальный save → SerializableContainer JSON roundtrip → destroy lifecycle и
  InstanceKeeper → fresh ProfileComponent. Проверены business inputs, Back,
  back stack, повторный вход, retained recreation, latest lazy snapshot, runtime
  omissions, accepted empty/invalid values и отсутствие автоматических записей.
  Недостижимые через текущие non-null picker callbacks null/Empty Goal значения
  отдельно проверены через сериализованный draft в настоящем Component.
- Три компилируемые поломки только во временных production copies обнаружены
  тестами: удаление Body supplier — 4 failures; перезапись edited weight domain
  данными — 4 failures; Apply пустого muscle каталога — 1 failure. Исходники
  working tree этими проверками не изменялись; текущая версия снова 25/25 PASS.
- Kotlin PSI: все 68 Profile Kotlin/KTS файлов без syntax errors и named local
  functions. Проверены пять typed local keys, transient-поля, отсутствие raw draft
  StateKeeper bindings, recovery wrappers и лишних coroutine scopes.
- diff --check чистый. Изменены только Profile и этот task record; существующие
  staged изменения не тронуты. Root/router keys и platform shells не изменены.

Evidence directory:
`/var/folders/gs/p9xpr8w95s3g6tcyx10tqbc80000gn/T/profile-persistence-jvm-20261013-xpu9_udr/`:
`profile-production.sources.json`, `profile-tests.sources.json`, `tests.json`,
`root-junit.log`, `mutation-verification.json` и `mutations/*/junit.log`.
Scope/PSI evidence:
`/var/folders/gs/p9xpr8w95s3g6tcyx10tqbc80000gn/T/profile-state-20261013-jz2w0ox4/`.

Android/iOS builds, Gradle/KSP, native runtime и UI rendering не запускались.
JVM fixture заменяет generated resource symbols, theme facade, Screen rendering
с fail-fast сигнатурами, KSP module composition и native date formatting.
Эти проверки доказывают common restoration/business wiring, а не generated DI,
отрисовку или работу platform shell на устройстве. iOS тесты не возвращались.

Второй проход, 2026-10-13: добавлены показы восьми самостоятельных экранов через
ProfileAnalytics и существующий foundation hook. StateKeeper bindings, State
модели и бизнес-код не изменены; это проверено нормализованным сравнением исходников.
Финальный direct JVM запуск и независимый повтор root: **38/38 PASS** (прежние
25 тестов + семь UI/lifecycle сценариев + шесть module/API тестов). Проверено,
что archive/save не создаёт view, после восстановления back stack показывается
только активный экран и Back возвращает сохранённый черновик. Strict compilation,
финальные source/artifact hashes и независимый review завершены.
Полная матрица, результаты и актуальный evidence directory — в
[Profile coverage](../analytics/profile-coverage.md#review-и-проверки).

Третий проход, 2026-10-13: по решению пользователя удалены девять сохранённых
флагов редактирования из Body/Goal/Experience/Muscles/Equipments. Сохраняются
только исходные поля черновика и catalog identity. Body использует текущий
transient User как источник для сравнения; Goal/Experience и каталоги держат
минимальный private runtime baseline. Восстановленный полный снимок сохраняется
при первой загрузке, а живые обновления используют сравнение с предыдущим
источником. Выбор, равный источнику, не имеет отдельной истории редактирования.
Auth проверен целиком: аналогичных флагов нет; known IDs там необходимы для
принятых parent exclusions и не изменены. Ключи, lazy suppliers, восемь hooks,
контейнер и операции сохранены. Сопутствующая внешняя правка Body
`delay(500.milliseconds)` сохранена, а не откатывалась как часть упрощения.

При удалении полей обнаружена несовместимость старого strict Essenty archive:
неизвестные true-флаги ломают consume. Пять draft типов используют class-scoped
JsonIgnoreUnknownKeys, сохраняя известные значения без устаревших полей,
catch/fallback или отдельного parser. Profile явно зависит от serialization-json;
ExperimentalSerializationApi разрешён в общей convention по канонам.
Неверные типы известных полей по-прежнему отклоняются.

Финальные проверки:

- Self-review → два независимых cross-review → исправлена main JSON dependency →
  повторное review без замечаний. Скилл, process-death reference и coverage обновлены.
- Direct Kotlin JVM compile: strict explicit API/Werror, 141 production source
  (128 actual repo + 13 host fixtures); API/implementation/tests также проверены.
  Source/artifact SHA подтверждены. **46/46 JUnit PASS**, независимый повтор root
  тоже 46/46: 37 common Profile сценариев, три прежних routing теста и шесть
  ProfileAnalytics тестов.
- Добавлены проверки differing/equal source updates во всех пяти owners,
  редактирования до первой загрузки форм, authoritative Empty/null/default
  snapshots, отсутствия флагов в payload при encodeDefaults=true, legacy unknown
  metadata и строгого отказа при malformed известном поле. Предыдущие проверки
  back stack, lifecycle, empty catalogs, accepted results и business requests сохранены.
- Реальные прежние serializers из frozen 38-test artifacts создали полные Root
  archives всех пяти owners. Fresh текущие Components восстановили точные значения
  с изменёнными business sources через настоящий Essenty Java ZIP codec и
  nonJava codec на JVM. Для nonJava использованы 13 исходников без изменения
  содержимого, сверенных с cached official source JAR. Suite на этом codec:
  **46/46 PASS**. Это host-проверка codec, без Android/iOS runtime.
- Удаление JsonIgnoreUnknownKeys из пяти временных production copies компилируется
  и обнаруживается regression suite: ровно один failing legacy-restoration test.
  Worktree не изменялся. Known-field strict failure остаётся ожидаемым успешным тестом.
- PSI: 81 Kotlin/KTS файл, без syntax errors и named local functions. Skills YAML,
  локальные ссылки, scope, index и diff --check проверены. Новых файлов и удалений
  нет. Index не меняли; замеченное параллельное изменение staging сохранено.
  Native shells, backend и Auth не менялись.

Актуальный evidence:
`/var/folders/gs/p9xpr8w95s3g6tcyx10tqbc80000gn/T/profile-draft-simplification-20261013-iqd5oscg/`:
`final-scope.json`, `psi.log`, `skills-docs.json`; в `jvm/` — четыре source manifests,
`tests.json`, `root-junit.log`, `root-verification.json`, `root-legacy-verification.json`,
`legacy-writer-verification.json`, `legacy-compatibility-verification.json`,
`nonjava-codec.sources.json`, `legacy-mutation-verification.json` и соответствующие logs.
Gradle/KSP, Android/iOS builds, native runtime, UI rendering и Firebase не запускались;
host substitutions и границы из предыдущего прохода сохраняются.

Четвёртый проход, 2026-10-13: пользователь запретил private runtime flags,
previous-source caches и теневые State snapshots во ViewModel. Удалены
restorePending и четыре previousSource/previousSourceSelection. State,
Components, ключи, DI, экранные hooks и запросы не менялись. Новый Body заполняет
пустые поля при первом User и дальше обновляет только реальные User/history данные.
Goal/Experience получают первое значение только для нового draft через finite
Flow с существующим Processing.Infinity; pause/resume не повторяет заполнение.
Каталоги сохраняют выбор знакомых IDs, задают выбор новых по текущим исключениям
и продолжают перестраивать suggestions. Источник для заполнения — первый
доступный cache/source; последующий refresh не переписывает черновик.

Канонический state-preservation skill, ui-feature, process-death reference,
forbidden-patterns и CLAUDE актуализированы. Запрет относится и к эквивалентам,
спрятанным в State/обёртке/provider/изменяемом closure. Существующие данные State
и ordinary initial inputs остаются единственным источником решения. Self-review
и взаимное review пяти ViewModel выполнены; root проверил финальный diff.
В дополнительном review исправлены слишком широкое обещание сохранения
default-equal ранних действий в скилле и недостаточная длительность pause в тесте:
виртуальное время теперь проходит реальную 1-секундную задержку WhileActive.

Финальные проверки текущего прохода:

- Cached direct JVM compilation API/implementation/production/tests:
  2/3/141/8 sources, strict explicit API/Werror в production; source/artifact SHA
  проверены. **47/47 JUnit PASS**, независимый повтор root тоже 47/47.
- Suite проверяет политику целого draft, ранние nondefault изменения, сохранённые
  Empty/null/default, известные/новые IDs, empty cache, точные Apply/Save requests,
  Back/retention, fresh Root/Component archives и lifecycle без повторной инициализации.
  Прежние autosync expectations заменены по новому решению пользователя.
- Реальные старые Root archives всех пяти owners восстановлены в текущих fresh
  Components через Java ZIP и exact official nonJava codec hosted on JVM. Полная
  suite на nonJava codec: **47/47 PASS**. Native/iOS runtime не запускался.
- PSI: 126 Kotlin/KTS файлов, в том числе все 58 production feature/shared
  ViewModel; syntax errors, named local functions и изменяемые VM members отсутствуют.
- Временная Goal mutation Processing.Infinity → WhileActive компилируется с
  исходным ABI и даёт ровно одну ожидаемую ошибку lifecycle-теста из 47.
  Рабочие исходники не менялись; это подтверждает, что тест обнаруживает повторное заполнение.

Актуальный evidence directory:
`/var/folders/gs/p9xpr8w95s3g6tcyx10tqbc80000gn/T/profile-no-runtime-state-20261013-grvj7md2/`.
Root: `final-scope.json`, `psi.log`, `psi-sources.json`, `skills-docs.json`,
`catalog-agent-review.json`; `jvm/`: source manifests, `tests.json`, `junit.log`,
`root-junit.log`, `root-verification.json`, `legacy-compatibility-verification.json`,
`lifecycle-mutation-verification.json` и соответствующие codec logs. Без Gradle/KSP,
Android/iOS builds, UI/native runtime, Firebase, backend и staging mutations;
прежние host substitutions сохраняются.
