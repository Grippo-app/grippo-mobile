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
| CreateProfile игнорировал false в Result.success | Цельный use case проверяет обязательные create/reload outcomes до подготовки и welcome | При false отсутствуют hydration/welcome/Home, сохраняется обычный error pipeline и failed operation |
| Register с существующим профилем не загружал weight history | Добавлен WeightHistoryFeature перед Goal; один дополнительный dependency в существующем FeatureApiModule | DI и оба business paths, failure/cancellation semantics |
| DeleteProfile игнорировал ошибку очистки настроек | clear().getOrThrow() до destructive request | Сохраняется тот же exception, delete не вызывается |
| Password input использовал Text/Sentences keyboard | Password/None/autoCorrectEnabled=false | Точный текущий KeyboardOptions expression компилируется с cached Compose; callbacks неизменны |
| ALL HTTP logging и bearer prefix раскрывали секреты | INFO без headers/bodies, auth-path logging выключен; refresh logs содержат тип ошибки вместо message | Actual BackendClient/TokenProvider/ClientLogger/DTO/parser/file logger, пять auth paths, responses и errors |
| ResponseValidator превращал fatal/non-Exception Throwable в обычную network error | Пробрасывает исходную ошибку перед обычным mapping | Identity cancellation/fatal/non-Exception/AppError; ordinary mappings и success response |
| Google iOS мог потерять weak presentation provider | Strong property у владельца UI flow | Source review и актуальный локальный SDK header |
| Google iOS использовал некриптографический RNG fallback | SecRandom failure прекращает вход; убраны retries/zero-byte/Random fallbacks | Source review; реальная RNG failure injection не запускалась |
| Отмена до queued native startup могла открыть UI | Continuation guard до создания SDK request; Google cancel lookup внутри main dispatch | Независимый source review cancellation order; native runtime не запускался |
| Apple отменял корутину, но оставлял SDK request | cancel собственного controller и очистка refs по identity | SDK API доступен при текущем iOS deployment 16; source review |
| Google Android signOut оборачивал cancellation/fatal | Пробрасывает их до обычного provider exception | Source review по контракту текущего signIn |
| Неиспользуемый iOS server-client config и недостижимый PKCE plain fallback | Удалены runtime config, secure-sync/plist entry и nullable SHA wrapper; всегда S256 | Exact diff, plist parsing, сохранение Android Web config, actual pure SHA/PKCE проверки |

Refresh orchestration, mutex, backoff, persistence и failure branches не менялись.
Удалено чтение 401 body, которое выполнялось только ради логирования. Безопасность auth headers/responses/errors рассмотрена отдельно от runtime refresh orchestration.
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
Supplied archive и live retention остаются разными механизмами.

## Скиллы и каноны

Canonical [feature-analytics](../../.claude/skills/feature-analytics/SKILL.md) и
[state-preservation](../../.claude/skills/state-preservation/SKILL.md) соответствуют
текущему коду. Их обязательные screen/state inventories сохранились. Общий стандарт
не требует нового provider, сценария или telemetry state на каждую фичу.

В UI/data/planner/reviewer/platform инструкциях устранены реальные противоречия:
leaf не владеет child stack, nested graph может; допустимы реальные VM dependencies
и Component DI; ResultKey ссылается на канонический serializer. Machine reports и scaffold gate pipeline обязательны
только в настроенном orchestrator workspace. Отсутствующие tools не создают PASS.

Data-layer теперь явно покрывает составление юзкейсов, различает Result failure и
неподходящий successful payload, обязательный cleanup и явно существующий remote
best-effort, требует полную подготовку alternate paths. Password keyboard правило
записано в Compose reference. Frozen contract bodies не редактировались.

Независимый forward-review применил скиллы к схеме новой ProfileEditor-фичи:
leaf/graph classification, цельная операция, inputs/picker callbacks, catalog
selection и fresh-Component restoration. Это проектирование по инструкциям,
не реализованная фича. Найдены и исправлены два оставшихся
противоречия: network example больше не поглощает cancellation/fatal, а FormatState
reference сохраняет latest formatter state без обещания исходной строки каждого
числового ввода. Фактические source/doc checks прошли; недоступные machine gates
и platform runtime не объявлялись выполненными.

## Границы готовности

Локальное покрытие не означает завершённую end-to-end поставку:

- **Подтверждённая внешняя проблема:** соседний `grippo-backend` Google verifier
  возвращает сырой SDK error.message. Локальная installed library включает JWT в
  malformed/signature error. Этот текст приходит в обычный mobile error pipeline
  и может попасть в log/CrashReporting. Нужна фиксированная публичная backend error
  без текста SDK; транспортный logging filter не исправляет exception payload.
  Подготовлен минимальный кандидат с `UnauthorizedException("Invalid Google token")`;
  Backend находится вне текущего writable workspace,
  кандидат не применён: пользователь выбрал «Пока только mobile». Backend-работа
  отложена вне текущего scope; подтверждённая проблема остаётся открытой. Полный
  backend typecheck/runtime не запускался.
- **Analytics collection:** core и обе платформы начинают с Unknown. Production
  владелец перехода в Granted пока отсутствует; события не отправляются до явного
  разрешения. Это прежняя отдельно запланированная application consent policy,
  а не доказанный live Firebase сбор.
- **iOS state restoration:** последующий проход добавил архив в app shell;
  актуальная реализация и ограничения описаны ниже. Native process restart пока
  не проверялся: platform builds/runtime остаются вне разрешённого scope.
- **Непроверенный runtime:** generated KSP DI, platform compilation, нативные
  Google/Apple dialogs, rendered Compose/IME и SDK delivery требуют последующих
  разрешённых platform checks. Эти platform checks остаются вне текущего scope.

Optional validation alignment: backend registration ограничивает password length
до 128, текущий PasswordFormatState задаёт только minimum. Сверхдлинный ввод получает
обычную backend validation error; изменение shared validator/product hint в этот
проход не добавлялось.

## iOS StateKeeper: app-shell интеграция

Один `ApplicationState` в shared владеет dispatcher и кодирует Essenty archive.
AppDelegate восстанавливает его до первого DefaultComponentContext/RootComponent,
затем передаёт тот же dispatcher дереву компонентов. Root остаётся ленивым:
background saving и active clearing не инициализируют UI и не удаляют файл до
первого восстановления. Существующий ApplicationLifecycle остаётся единственным
владельцем lifecycle; WindowGroup использует один scenePhase hook.

ApplicationStateStore атомарно сохраняет файл в Application Support. Envelope
проверяет format/application/build version и SHA256 точных UTF-8 bytes; checkpoint
потребляется до декодирования. Несовместимый или повреждённый архив начинает новый
root. Ordinary store/codec errors обрабатываются только в shell, без текста
ошибки/payload в логах; ошибки конструирования компонентов не скрываются.
Файл исключён из backup и использует iOS protection until first authentication.
Инертные UIKit shouldSave/shouldRestoreSecureApplicationState callbacks удалены.
Swift source зарегистрирован в app target и обоих канонических PBX templates.

Feature ключи, serializers, DI, screenPresentation и события не менялись.
Это восстановление завершённого background checkpoint того же build, не обещание
восстановления после внезапного завершения активного процесса. При foreground
удаляется только disk checkpoint, живое дерево и черновики сохраняются в памяти.
Изменение несовместимой схемы требует нового build либо archive format.

### Повторный аудит iOS StateKeeper

Порядок сверён с ApplicationLifecycle Essenty: чтение архива предшествует
созданию дерева, initial lifecycle delivery поставлена в main queue,
checkpoint callbacks не создают lazy root. Живой root и dispatcher сохраняются
при foreground; RootComponent конструируется вне recovery boundary.

Неудачная атомарная замена оставляет предыдущий завершённый архив. Store
использует только production параметры и платформенную iOS реализацию.
Root private(set): другие Swift-файлы читают тот же lazy instance, но не могут
заменить его отдельно от ApplicationState/BackDispatcher. BackDispatcher
неизменяемый; RootView возвращает UIViewController напрямую.

Swift syntax parse всех app sources, actual PBX/Info.plist и оба канонических
PBX templates lint, archive/lifecycle source order, ссылки и source scope
проверены. Restore/save/clear, wire format, scenePhase, component construction
и Android остались прежними. Android/iOS builds, Gradle/KSP, platform typecheck,
UIKit/scene delivery и перезапуск приложения не запускались.
