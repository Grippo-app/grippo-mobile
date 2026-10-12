# Повторный аудит изоляции — 2026-10-12

Повторно прочитаны RegisterUseCase/CredentialViewModel, navigation FlowId,
AnalyticsProvider restore/retain/check/save, generic operation execution,
flow transitions, lifecycle bridge, schema matrix и актуальные тестовые исходники.

Новых бизнес-зависимостей от аналитических результатов не обнаружено.
Убран избыточный параметр flow в private AuthFlow.report: используется собственный
context. Legacy serializer name навигационного ID документирован явно.
Тестовые исходники расширены: register profile/no profile × push/no push,
server failure и cancellation для обоих use cases, границы ID (1..64 и синтаксис),
caller cancellation во время terminal reporting, fatal reporting Error.

Сохранены sequential business requests, исходные business exceptions и generic
результат, fatal Error semantics, route wire shape, StateKeeper keys, identity
checks и неизменная DI/retention структура. Новых обёрток/публичного API нет.
Обычный reporting Exception изолирован; нарушение route/state invariant по-прежнему
отклоняется явно, не подменяется новым сценарием. Доставка событий при reporting
ошибках не гарантируется, бизнес от неё не зависит.

Повторные статические проверки /tmp/verify_three.py, удалённых символов и internal
imports, YAML/документные проверки и git diff --check прошли. Эти проверки не
разрешают Kotlin типы полностью. Сборки Android/iOS, KSP, lint и тесты не запускались;
компиляция и runtime не подтверждены. Firebase не использовался.
