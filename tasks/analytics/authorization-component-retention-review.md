# Retention внутри компонентов — 2026-10-12

> Исторический отчёт: описывает реализацию и проверки этого этапа, включая
> позднее заменённые API/provider/retention. Текущие правила —
> [feature-standard](feature-standard.md), фактическое покрытие и scope —
> [авторизация](authorization-coverage.md). Результаты ниже не подтверждают
> последующие изменения.

AuthAnalyticsRetention.kt удалён. AuthProcessComponent и SplashComponent владеют
private createAnalytics, private typed RetainedAnalytics и private ключом.
Новых base classes/hooks/delegates и module-wide extensions нет. Дочерние
Login/Registration компоненты получают тот же Authorization через конструктор.

Порядок сохранён: consume snapshot → getOrCreate retained instance → route FlowId
check (Authorization) → register актуального save → создание/получение ViewModel.
StateKeeper/InstanceKeeper ключи authorization_analytics_flow и
 authorization_session_flow сохранены; snapshot сериализация не менялась.
DI resolution выполняется только при создании нового retained instance.
Analytics implementation не получил UI/Essenty зависимостей.

Self-review охватил обоих владельцев, передачу в дочерние компоненты, порядок
инициализации, восстановление и active docs/skill. Новая привязка
проверена по исходникам.

Проверки: static audit 116 Kotlin файлов (delimiter balance, удалённые usages,
private ownership, initialization order, retention keys, FlowId invariant,
child propagation, dependency boundaries, active documentation), skill YAML,
git diff --check — exit 0. Android/iOS compilation/KSP/builds не запускались
по текущему указанию пользователя. Firebase/provider calls/uploads отсутствуют.

## Удаление holders

Последующее упрощение: custom RetainedAnalytics и companion objects удалены.
Используется retainedSimpleInstance(key = ANALYTICS_KEY), а ключи вынесены в private
file-level const val с прежними значениями. API проверен по локальным исходникам
Essenty 2.5.0: retainedInstance ограничен InstanceKeeper.Instance; для обычного
сценария подходит retainedSimpleInstance, использующий library SimpleInstance.
Собственных adapters/casts не добавлено. У сценариев нет disposal ресурсов.
Порядок consume → retain → invariant → register save → ViewModel сохранён.

Повторный self-review обоих компонентов и static audit 116 Kotlin файлов,
skill YAML и git diff --check прошли (exit 0). Compilation/runtime не
проверялись, Android/iOS builds не запускались по указанию пользователя.

## Удержание по типу сценария

retainedSimpleInstance<AuthAnalytics.Authorization>/Session использует typeOf<T>()
как ключ InstanceKeeper (проверено по локальным исходникам Essenty 2.5.0).
Явный тип интерфейса указан, чтобы ключ не зависел от типа implementation.
В каждом владельце только один сценарий данного типа; ключи ViewModels отличаются.
Прежние persistent string keys сохранены, private const переименован в
ANALYTICS_STATE_KEY и используется только consume/register StateKeeper.
Сам InstanceKeeper не сериализуется: замена его ключа не меняет persistent snapshot
format; in-process hot swap старых holders этим изменением не поддерживается.
Статические проверки type keys/StateKeeper/save/order/FlowId invariant и
 git diff --check — exit 0. Android/iOS builds не запускались.
