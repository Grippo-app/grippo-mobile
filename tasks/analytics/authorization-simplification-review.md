# Авторизация: сокращение объёма аналитики

> Исторический отчёт: описывает реализацию и проверки этого этапа, включая
> позднее заменённые API/provider/retention. Текущие правила —
> [feature-standard](feature-standard.md), фактическое покрытие и scope —
> [авторизация](authorization-coverage.md). Результаты ниже не подтверждают
> последующие изменения.

## Решение и изменения

Сохранены основные факты: Login/Register attempts, whole preparation, session check,
flow start/exit, navigation и leaf screen views. Сохранены typed failure categories,
provider cancellation, Provider/Server attribution, coroutine interruption и
исходный error pipeline. Snapshot/route identity и retention не упрощались ценой
рассинхронизации ViewModel/presentation.

Намеренно убраны второстепенные данные:
- auth_form_rejected: событие на каждое неверное поле. Удалены API rejectForm,
  FormStatus, UI formatter mapping, Field/Rule types/keys и event schema.
  Validation и отображение ошибок продолжают работать как раньше.
- Analytics wrapper для начальных справочников. Удалены Session.loadInitialData,
  InitialData/Completed codes и callback factory; Splash выполняет мышцы →
  оборудование напрямую в существующем safeLaunch. Ошибка/отмена продолжают
  обрабатываться pipeline; отдельного analytics terminal для этих запросов нет.

AuthExecution использовался только Authorization scenario: его методы перенесены
в AuthorizationAnalyticsImpl, дополнительный execution instance удалён.
AuthPreparationEvents теперь AnalyticsOperationEvents<Boolean> без generic T,
outcome lambda и InitialData mode check. Обе preparation kinds допускают оба
profile outcomes: отдельная runtime kind/outcome matrix больше не нужна.
Stage matrix для попытки и session сохранена, потому что она всё ещё имеет
реальные недопустимые combinations.

UI integration состоит из двух файлов:
- AuthAnalyticsState: route mapping и обязательное StateKeeper/InstanceKeeper ownership.
- AuthAnalyticsIntegrationModule: annotated module с @Single provider и internal
  stateless classifier object. Убраны отдельный classifier class, companion bridge
  и ComponentScan для единственного binding. Generated .module подключён в shared.

Новый delegate, feature scopes, workers, retries, flags, aliases и fallback paths
не добавлены. UI business requests остаются в ViewModels/use cases.

## Совместимость и проверки

Каталог теперь содержит 16 custom schemas v1. Значения и смысл оставшихся событий
не менялись. Старые auth_form_rejected и preparation_kind=initial_data перестают
поступать; historical reports сохраняют старые данные. Replay/dual-write нет.
State keys, legacy snapshot SerialName, serialization shape и FlowId сохранены.

Coverage, standard и canonical skill обновлены: второстепенная
детализация не должна автоматически разрастаться вокруг каждого действия фичи.

Выполнены self-review и повторный статический проход по consumers, API, DI,
schemas, enum/method references, lifetime, privacy и exceptions.
Статические checks: catalog names и counts, отсутствующие obsolete symbols,
скобки изменённых Kotlin files, skill frontmatter, Markdown fences, git diff --check.

По прямому запросу пользователя Gradle/Xcode builds НЕ запускались.
Это не подтверждение компиляции/KSP или runtime поведения: platform wiring
требует последующего разрешённого прогона. Предыдущие passing
reports относятся к прежнему implementation. Firebase sends/sign-in/uploads не выполнялись.

## Повторный проход после сокращения

Найден оставшийся consumer API хвост: AuthAnalytics.Action после удаления form
rejection больше не используется потребителем. Enum перенесён в internal AuthAction
в существующем AuthTypes.kt; auth_action ключ и login/register codes сохранены.
NavigationReason.Action
является независимым toolkit типом и сохранён без переименования.

Повторно проверены API consumers, annotated module/Single binding, route mapping,
retained state/save, business callbacks, classifiers, stage/outcome encoding,
schema catalog и active skills/docs. Новых workers/scopes/retries или
подтверждённых дефектов в этих частях не найдено. Historical review reports
содержат старые названия по смыслу своей истории и не переписаны.
Статические symbol/enum/catalog/format checks выполнены после последней правки.
Android/iOS builds по-прежнему не запускались; компиляция/KSP не подтверждены.
