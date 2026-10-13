# Авторизация: итоговый аудит 2026-10-13

Проверены UI авторизации, аналитика, сохранение черновиков, цельные юзкейсы,
их DI, транспорт и Google/Apple providers. Исправлены найденные ошибки без нового
слоя orchestration, публичных analytics providers или изменения navigation contracts.
Android/iOS builds, Gradle/KSP, реальный provider UI и Firebase не запускались.

Текущее покрытие: [authorization-coverage](authorization-coverage.md).
Архитектурный стандарт: [feature-standard](feature-standard.md).
Предыдущие review-файлы описывают исторические реализации; этот отчёт относится
к текущим исходникам и отдельному свежему baseline этого прохода.

## Структура и ответственность

| Где | Ответственность |
|---|---|
| `analytics/analytics-api/authorization` | Один stateless AuthAnalytics; sibling models для закрытых событий, операций и экранов |
| `analytics/analytics-api/common/models` | Общие InputStatus, InputKeyboardAction, ControlAction |
| `analytics/authorization/internal` | Impl, encoding, typed schema matrix, payload и operation events; без UI/domain/SDK зависимостей |
| `authorization/<screen>` | Обычный MVI-комплект; VM наблюдает пользовательские callbacks и оборачивает цельную бизнес-операцию |
| `authorization/analytics` | Только преобразование существующего validation status в аналитический status |
| `authorization/<owner>/<Owner>Component` | Private companion object внизу класса: DRAFT связывает стабильный адрес архива с serializer его State |
| `foundation/models/PersistenceKey` и BaseComponent | Invariant generic metadata; protected restoreState/saveState поверх локального StateKeeper |
| `foundation/analytics` | ScreenAnalytics API и internal DI implementation; общий lifecycle hook |
| `feature-api` | Бизнес-результаты, последовательность запросов, обязательная подготовка и cleanup |
| `google-auth` / `apple-auth` | Получение credentials и владение нативным запросом |
| `backend/client` | HTTP/auth headers/refresh; безопасные транспортные логи |

Все 11 route → Component → retained VM → Render цепочек сверены с реальными
поверхностями. Восемь отдельных экранов имеют fixed typed screenPresentation;
Auth, AuthProcess и ProfileCreation остаются контейнерами с default null.
Подключение lifecycle одно — в foundation. Компоненты разрешают обычные VM
dependencies; providers, manual analytics bindings, flow snapshots и classifiers
не вернулись.

Проверены 44 Contract callbacks и два picker-result callback. Typed payloads
содержат status/enums/Boolean/count, без credentials, введённого текста, body
measurements, entity IDs и error messages. События следуют за business guards.
22 custom schemas — 10 interaction и 12 operation; кодирование и версии не менялись.

## Исправленные ошибки

| Проблема | Исправление | Проверка |
|---|---|---|
| User.Back допускал повторный logout и конкурирующую навигацию | Existing safeLaunch получил Logout loader; Back/Next/picker opens отклоняются во время операции до telemetry | Pending, repeated callbacks, immediate loader, cleanup и VM destroy/cancellation |
| Кнопка продолжения User не учитывала обязательные height/weight и busy state | Disabled при loader или любом невалидном обязательном поле | Сверка UI conditions с действительными VM guards; rendering не запускался |
| CreateProfile игнорировал false в Result.success | Цельный use case проверяет обязательные create/reload outcomes до подготовки и welcome | Два независимых false regressions: нет hydration/welcome/Home, обычный error pipeline, failed operation |
| Register с существующим профилем не загружал weight history | Добавлен WeightHistoryFeature перед Goal; один дополнительный dependency в существующем FeatureApiModule | Actual module compilation/resolution и оба business paths; failure/cancellation matrices |
| DeleteProfile игнорировал ошибку очистки настроек | clear().getOrThrow() до destructive request | Сохраняется тот же exception, delete не вызывается |
| Password input использовал Text/Sentences keyboard | Password/None/autoCorrectEnabled=false | Точный текущий KeyboardOptions expression компилируется с cached Compose; callbacks неизменны |
| ALL HTTP logging и bearer prefix раскрывали секреты | INFO без headers/bodies, auth-path logging выключен; refresh logs содержат тип ошибки вместо message | Actual BackendClient/TokenProvider/ClientLogger/DTO/parser/file logger, пять auth paths, responses и errors |
| ResponseValidator превращал fatal/non-Exception Throwable в обычную network error | Пробрасывает исходную ошибку перед обычным mapping | Actual MockEngine: identity cancellation/fatal/non-Exception/AppError; прежние ordinary mappings и success response |
| Google iOS мог потерять weak presentation provider | Strong property у владельца UI flow | Source review и актуальный локальный SDK header |
| Google iOS использовал некриптографический RNG fallback | SecRandom failure прекращает вход; убраны retries/zero-byte/Random fallbacks | Source review; реальная RNG failure injection не запускалась |
| Отмена до queued native startup могла открыть UI | Continuation guard до создания SDK request; Google cancel lookup внутри main dispatch | Независимый source review cancellation order; native runtime не запускался |
| Apple отменял корутину, но оставлял SDK request | cancel собственного controller и очистка refs по identity | SDK API доступен при текущем iOS deployment 16; source review |
| Google Android signOut оборачивал cancellation/fatal | Пробрасывает их до обычного provider exception | Source review по контракту текущего signIn |
| Неиспользуемый iOS server-client config и недостижимый PKCE plain fallback | Удалены runtime config, secure-sync/plist entry и nullable SHA wrapper; всегда S256 | Exact diff, plist parsing, сохранение Android Web config, actual pure SHA/PKCE проверки |

Refresh orchestration, mutex, backoff, persistence и failure branches не менялись.
Удалено чтение 401 body, которое выполнялось только ради логирования. Transport
harness проверяет headers и auth responses/errors, а не runtime refresh orchestration.
Новый user busy guard — бизнес-исправление; аналитика только наблюдает допущенные
действия. Required Boolean outcomes проверяются бизнесом, не telemetry models.

## Состояние и восстановление

Семь прежних archive addresses/serializers сохранены. Черновики restore внутри
retainedInstance factory, save регистрирует lazy supplier текущего VM state.
Parent draft восстанавливается до детей. Никаких per-edit route replacements,
PersistenceKey в router/state payload, публичных persistence extensions или дополнительной DI.

Сохраняются latest nonsecret invalid/empty email/name/measurements, accepted picker
values, experience, group/known/selected IDs и принятый parent draft. Пароли и
provider availability остаются transient; availability и каталоги вычисляются
из текущих источников. Loader/job/focus/dialog/analytics provenance не сохраняются.
Проверки используют save → JSON roundtrip → destroy → fresh context/InstanceKeeper;
простое удержание VM не выдаётся за process-restoration test.

## Скиллы и каноны

Canonical [feature-analytics](../../.claude/skills/feature-analytics/SKILL.md) и
[state-preservation](../../.claude/skills/state-preservation/SKILL.md) соответствуют
текущему коду. Их обязательные screen/state inventories сохранились. Общий стандарт
не требует нового provider, сценария или telemetry state на каждую фичу.

В UI/data/planner/reviewer/platform инструкциях устранены реальные противоречия:
leaf не владеет child stack, nested graph может; допустимы реальные VM dependencies
и Component DI; ResultKey ссылается на канонический serializer; test convention
использует существующий plugin. Machine reports и scaffold gate pipeline обязательны
только в настроенном orchestrator workspace. Отсутствующие tools не создают PASS.

Data-layer теперь явно покрывает составление юзкейсов, различает Result failure и
неподходящий successful payload, обязательный cleanup и явно существующий remote
best-effort, требует полную подготовку alternate paths. Password keyboard правило
записано в Compose reference. Frozen contract bodies не редактировались.

Независимый forward-review применил скиллы к схеме новой ProfileEditor-фичи:
leaf/graph classification, цельная операция, inputs/picker callbacks, catalog
selection и fresh-Component restoration. Это проектирование по инструкциям,
не реализованная и протестированная фича. Найдены и исправлены два оставшихся
противоречия: network example больше не поглощает cancellation/fatal, а FormatState
reference сохраняет latest formatter state без обещания исходной строки каждого
числового ввода. Фактические source/doc checks прошли; недоступные machine gates
и platform runtime не объявлялись выполненными.

## Проверки предыдущего итогового аудита

| Проверка | Результат |
|---|---|
| Auth/common analytics | 19 JUnit cases |
| Toolkit API/core и foundation | 67 JUnit cases, включая 14 foundation |
| Реальные auth VM/Components/domain и affected caller/error pipeline | 67 JUnit cases, включая 6 BaseViewModel error cases |
| Actual ResponseValidator tests | Четыре JUnit cases; новый fatal/non-Exception identity regression падает только на before source |
| Итого, без повторного счёта | **157 JUnit cases прошли** |
| Before UserVM/Loader с новыми тестами | Три ожидаемых behavioral failures; production/tests компилируются |
| Before CreateProfile/Delete с новыми тестами | Три ожидаемых behavioral failures; production/tests компилируются |
| Typed descriptor external consumer предыдущего аудита | Успешная компиляция без friend paths |
| Шесть неправильных external descriptor usages предыдущего аудита | Ожидаемые ошибки type/visibility/finality |
| Полный actual FeatureApiModule | 133 source inputs; strict compilation и Koin execution обоих Register paths |
| Actual HTTP transport/file logging | Пять auth DTO paths, success/400/engine errors, правильный bearer header, безопасный log; INFO control подтверждает engine-error logging |
| Actual ResponseValidator | Cancellation/fatal/direct Throwable/AppError identity, IO/ordinary mappings и success status/header/body прошли |
| Before TokenProvider.addRequestHeaders | Ожидаемая privacy failure на persisted fake bearer prefix |
| Actual pure SHA256/PKCE source | 12 inputs, padding boundaries, reference SHA256 и RFC 7636 S256 vector прошли |
| Точный Password KeyboardOptions block | Compilation exit 0; полного Composable/render теста нет |
| Native provider contracts | Source checks + local SDK headers/deployment; не native runtime |
| Canonical skills/docs | Семь SKILL validators, 28 Markdown документов, 160 local links/37 anchors; foundation drop-ins и frozen-contract hashes совпадают |

Исходники для compilation берутся из текущего repo. KSP/resource/render/native
substitutions, manifests и exclusions записаны в temporary harness reports.
Используются cached Kotlin 2.4.20, Compose/Essenty/Decompose/Ktor зависимости.
Decompose main-thread check выключен только через официальный test setting для
plain JVM. Новые зависимости/Gradle plugins ради этого аудита не добавлены.

Артефакты, exact before, команды и результаты:
`/var/folders/gs/p9xpr8w95s3g6tcyx10tqbc80000gn/T/auth-final-audit-20261013-vak1tm19/`.
Команды перечислены в [todo](todo.md#итоговый-аудит-auth--2026-10-13).

## Повторный аудит StateKeeper и Back

Перепроверены 11 Components, семь локальных drafts и их key/serializer pairs,
18 сохраняемых полей, семь transient полей, retained ownership, parent-before-child
порядок и фактические Back/Next callbacks. Пароли исключены из archive; invalid/
empty drafts сохраняются, availability/suggestions перестраиваются. Четыре State
без редактируемых данных не требуют дополнительных draft suppliers.

Найден один источник потери данных в трёх шагах: popped child уничтожается,
а повторный вход не использовал уже принятый выбор parent. Исправлены только
ProfileCreationComponent и три дочерних Components: обычные typed business inputs
используются лишь после отсутствующего local archive. Catalog seed содержит
принятые negative IDs в существующем known поле; нынешний reconciliation сохраняет
исключения и выбирает остальные/new items. Нового State, VM logic, storage,
provider, mapper, route payload или persistence framework не добавлено.

Три meaningful regressions добавлены в существующий ProfileCreationRestorationTest.
Каждый проверяет parent archive/fresh child, затем новый local выбор и второй
JSON/fresh restore при отличающемся accepted parent. Empty-first catalogs и
отсутствие новых input/selection events проверены явно. Equipment re-entry не
повторяет отменённую create operation и не создаёт hydration/welcome/Home.
Тот же финальный набор на before production даёт ровно три AssertionError;
production/tests обеих версий компилируются. Текущие **70 UI/domain/error cases
и 14 foundation cases прошли**, всего **84**. Foundation source и test bytecode
не менялись, переиспользованы с current source/artifact SHA и новым JUnit запуском;
UI скомпилирован заново (227 production/fixture inputs, 19 test inputs).

Canonical state-preservation, UI references и CLAUDE закрепляют local draft priority,
accepted parent seeds и Back/pop/re-entry. Пять точечных doc changes разрешают
ordinary business inputs без injectable dependencies в Component constructors.
Независимое source/test/canon review не нашло дополнительных дефектов.
State/VM/routes/domain/foundation/analytics не менялись этим исправлением.
Fresh baseline фиксирует отдельное параллельное изменение TokenProvider вне этого
auth-state scope; оно не редактировалось в ходе данного аудита.
Команды и ограничения —
[todo](todo.md#повторный-аудит-сохранения-auth--2026-10-13).

## Предыдущий перенос ключей внутрь компонентов

Каждый из семи компонентов-владельцев содержит private companion object в самом
низу класса с val DRAFT = PersistenceKey("прежний адрес", OwnerState.serializer()).
Отдельные *Persistence.kt удалены. restoreState(DRAFT) и saveState(DRAFT) сохраняют
прежний ownership и порядок; snapshots, VM, routes, foundation API, бизнес-логика
и события аналитики не менялись. Canonical state-preservation skill и UI примеры
описывают этот формат; дополнительный файл/матрица/объект для одного ключа не нужен.

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

## Предыдущий проход: метаданные по владельцам

После аудита StateKey переименован в PersistenceKey; централизованный AuthStateKeys
удалён. Семь владельцев имеют собственный `<Owner>Persistence.kt` рядом с MVI
файлами, включая parent ProfileCreation. State остаётся именем слепка экрана/данных;
Persistence содержит только адрес и serializer, без runtime работы, DI и extensions.

Старые archive addresses и serializers сохраняются. Методы restoreState/saveState
и порядок retained factory/lazy supplier/parent-before-children прежние. Этот
проход имеет отдельный baseline и проверки; результаты полного аудита выше
не выдаются за повторный полный platform run.

Предыдущий targeted JVM проход: **81 существующий JUnit case прошёл** — 14 foundation
и 67 auth VM/Components/domain/error pipeline. Все prerequisites собраны заново
из текущих исходников в отдельные temporary artifacts; старые foundation/UI jars
не использованы. UI compilation содержит 234 production/fixture inputs и 19 test
inputs. Проверены typed restore/save, latest draft, retained priority, sibling
isolation, JSON roundtrip и свежие Component/InstanceKeeper, parent-before-child
restoration, отсутствие secrets и runtime recomputation.

Scope и прежние substitutes/exclusions сохранены: явная test DI вместо generated
KSP extensions; cached Compose/Essenty/Decompose bytecode; fail-fast render/date/
Google platform fixtures. Android/iOS builds, native credential UI, rendering,
Gradle/KSP и Firebase не запускались. Полный предыдущий аудит из 157 cases в этом
проходе не повторялся.

Внешний consumer нового PersistenceKey скомпилирован без friend paths. Шесть
неправильных usages отвергнуты по нужным причинам: неподходящие supplier/serializer,
доступ к internal key/serializer, вызов protected saveState и наследование final
дескриптора. Все семь legacy pairs и bytewise неизменные State подтверждены отдельно.

Exact-diff review: 30 изменённых/добавленных/удалённых путей относительно свежего
baseline, без изменений VM, snapshots, routes, бизнес-логики и analytics API/impl.
Canonical validation: семь skills, 17 Markdown документов, 82 local links и 23
anchor; foundation drop-ins и UserPersistence пример совпадают с кодом. Проверены
12 свежих source/artifact manifests и git diff --check. Независимое ревью без findings.

Артефакты, before snapshot, compiler commands, logs и final-verification.json:
`/var/folders/gs/p9xpr8w95s3g6tcyx10tqbc80000gn/T/screen-persistence-20261013-xpw8zson/`.
Команды — [todo](todo.md#метаданные-сохранения-по-владельцам--2026-10-13).

## Границы готовности

Локальное покрытие не означает завершённую end-to-end поставку:

- **Подтверждённая внешняя проблема:** соседний `grippo-backend` Google verifier
  возвращает сырой SDK error.message. Локальная installed library включает JWT в
  malformed/signature error. Этот текст приходит в обычный mobile error pipeline
  и может попасть в log/CrashReporting. Нужна фиксированная публичная backend error
  без текста SDK; транспортный logging filter не исправляет exception payload.
  Подготовлен минимальный кандидат с `UnauthorizedException("Invalid Google token")`;
  шесть isolated Node scenarios прошли, baseline воспроизводит утечку, network
  вызовы запрещены harness. Backend находится вне текущего writable workspace,
  кандидат не применён: пользователь выбрал «Пока только mobile». Backend-работа
  отложена вне текущего scope; подтверждённая проблема остаётся открытой. Полный
  backend typecheck/runtime не запускался.
- **Analytics collection:** core и обе платформы начинают с Unknown. Production
  владелец перехода в Granted пока отсутствует; события не отправляются до явного
  разрешения. Это прежняя отдельно запланированная application consent policy,
  а не доказанный live Firebase сбор.
- **iOS state restoration:** `stateKeeper: nil` создаёт обычный пустой dispatcher;
  в shell отсутствует сохранение и загрузка архива до первого RootComponent. Common
  drafts готовы к supplied archive, Android bridge существует. Для iOS нужен
  отдельный app-shell lifecycle/carrier contract, согласованный с текущим SwiftUI
  WindowGroup; произвольная загрузка с диска на любом cold start здесь не добавлена
  и process restoration не обещается.
- **Непроверенный runtime:** generated KSP DI, platform compilation, нативные
  Google/Apple dialogs, rendered Compose/IME и SDK delivery требуют последующих
  разрешённых platform checks. Их отсутствие не скрывается за JVM PASS.

Optional validation alignment: backend registration ограничивает password length
до 128, текущий PasswordFormatState задаёт только minimum. Сверхдлинный ввод получает
обычную backend validation error; изменение shared validator/product hint в этот
проход не добавлялось.
