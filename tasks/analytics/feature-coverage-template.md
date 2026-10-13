# <Фича>: матрица аналитики

Дата/scope: <текущее изменение>. Стандарт — [feature-standard](feature-standard.md).
Stateless `<Feature>Analytics`: typed trackEvent + whole trackOperation + leaf views.

## Подключение

- API/models: <path>; internal implementation/matrix/schema: <path>.
- Leaf ViewModels/screenPresentation: <paths>; feature implementation registration: <path>.
- Component retained VM injects analytics как остальные deps; foundation binds hook.
- Обычный retainedInstance/ViewModel lifecycle сохраняется; analytics state/keys отсутствуют.

## Маршруты

| Route / surface | Screen / container | Component → ViewModel | FeatureScreen | Hook / причина null |
|---|---|---|---|---|
| <actual route / rendered page> | <role> | <actual owners> | <typed screen / absent for container> | <screenPresentation / container null reason> |

У каждого отдельного экрана обязателен ровно один screenPresentation в owning VM;
контейнеры оставляют default null. Для новой фичи включить все её экраны, для
изменения — все экраны затронутого scope. Сверить таблицу с routes, factories и
Render, включая nested/tab screens и альтернативные входы; не выводить полноту
только из FeatureScreen enum. Проверить отсутствие неверных screen identities,
enum entries без owner и screen views контейнера вместе с дочерним экраном. Пропущенный screen hook
блокирует завершение и не может быть оформлен как omission. См.
[обязательную проверку](../../.claude/skills/feature-analytics/SKILL.md#mandatory-screen-coverage-gate).

Initial — первый показ binding, Restore — последующая активация. Не приписывать
процессу сохранённые flow/referrer; dialogs/overlays имеют отдельный declared scope.

## Controls и inputs

| Callback / trigger | Typed FeatureEvent / wire name | Owner | Safe facts / omission | Check |
|---|---|---|---|---|
| <existing callback> | <closed direct description> | <Contract→VM> | <enum/status/Boolean/count> | <source/manual check> |

Text status compares existing old/new validation: same status omitted, без cache.
Picker confirmations/clicks/selections always distinct. Нет raw values/length/body
measurements/entity IDs. Eligibility guards и keyboard/focus поведение прежние.
Проверить invalid/busy callbacks и смену validation semantics/population. Dialog
open/result — owning VM; dedicated views/close — explicit omission либо отдельно
запрошенный typed contract/owner. onDismiss не означает cancellation/confirmation.

## Complete operations

| Business block | Typed FeatureOperation / wire family | Owner | Results/errors | Check |
|---|---|---|---|---|
| <whole existing sequence> | <operation> | <existing coroutine> | <preserved caller T/error/cancellation> | <source/manual check> |

OperationId/duration/start-terminal создаёт toolkit. Analytics не решает navigation,
validation/retry/fallback; programmatic navigation не fake user click.
Указать actual return/error contract: existing Result.getOrThrow внутри block или
direct-value throwing call. Сохранить цельную последовательность и исходную ошибку.

## Omissions и версии

Перечислить сознательно пропущенные события/данные и report migration при смене
schema semantics. Не добавлять flow/restore/classifier plumbing ради матрицы.
Reference loads/provider/system internals/overlay details/other features — только
если scope явно включает их. Catalog и wire parameters не дублировать по слоям.

## Проверка

Actual commands/results: <paths>. Payload/business isolation/reporting failures/
cancellation/leaf VM hooks/eligible callbacks/lifecycle: <run scopes>. YAML/links/examples: <results>.
Explicit unrun: <Gradle/KSP/platform/UI/native/Firebase>. PSI/static
review не означает runtime success; исторические результаты не подтверждают diff.
