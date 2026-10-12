# UI AnalyticsProvider — 2026-10-12

> Исторический отчёт: описывает реализацию и проверки этого этапа, включая
> позднее заменённые API/provider/retention. Текущие правила —
> [feature-standard](feature-standard.md), фактическое покрытие и scope —
> [авторизация](authorization-coverage.md). Результаты ниже не подтверждают
> последующие изменения.

## Реализация

В корне ui-screen-features/authorization находится internal AnalyticsProvider —
обычный stateless класс с AuthAnalytics dependency. Существующий
AuthAnalyticsIntegrationModule предоставляет его через annotated @Single method;
shared уже подключает generated .module этого модуля, новая регистрация не нужна.
Provider не хранит ComponentContext, StateKeeper, InstanceKeeper или текущий
scenario. Два метода authorization(context, route)/session(context) возвращают
retained сценарий, скрывая route mapping и persistent keys.

Компоненты получают analytics одним DI-вызовом до ViewModel initialization.
Дочерние компоненты и ViewModels получают тот же сценарий через constructor.
Во всех найденных source usages переменные/параметры authAnalytics переименованы
в analytics; контракт AuthAnalytics не переименовывался.

Сохранены прежние StateKeeper keys и serialization format, явные typeOf keys
retainedSimpleInstance<AuthAnalytics.Authorization/Session>, route FlowId check,
consume → retain → check → register latest save order. Новых scopes/retries,
mutable singletons, base components или feature extensions нет. UI зависимости
остаются в UI-модуле; analytics implementation/API не получили provider.

## Self-review и проверки

Просмотрены root provider, обе component call sites, передачи leaf/VM, DI module,
обновлённые named arguments и tests, skill/standard/coverage/integration guide.
AnalyticsProviderTest использует actual provider + DefaultComponentContext,
StateKeeperDispatcher/InstanceKeeperDispatcher: retained identity при recreation,
актуальное сохранение после recreation, fresh snapshot restore без повторного
flow start/exit, ошибочная смена route ID, session isolation и все пять entry points.
AuthIntegrationTest дополнен singleton DI-resolution для provider. Эти тесты
написаны, но не выполнялись по действующему ограничению пользователя.

Static audit 118 Kotlin files: delimiters, naming, stateless provider, exact type
keys, persistent keys/save/invariant, DI binding, initialization order, child
propagation, analytics layer dependency boundaries и active docs — exit 0.
Skill frontmatter YAML и git diff --check — exit 0. Constructors DefaultComponentContext
и retainedSimpleInstance сверены с locally installed Decompose/Essenty source jars.
Compilation/KSP/Android/iOS builds/tests не запускались; runtime поведение не
подтверждено. Firebase sends/sign-in/uploads не выполнялись.
