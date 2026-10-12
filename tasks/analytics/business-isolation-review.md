# Изоляция бизнеса от аналитики — 2026-10-12

> Исторический отчёт: описывает реализацию и проверки этого этапа, включая
> позднее заменённые API/provider/retention. Текущие правила —
> [feature-standard](feature-standard.md), фактическое покрытие и scope —
> [авторизация](authorization-coverage.md). Результаты ниже не подтверждают
> последующие изменения.

1. RegisterUseCase.createAccount выполняет register + private prepare и возвращает
   Boolean. CredentialViewModel измеряет один полный attempt. Удалены Preparation
   API, фабрика, события, outcome и четыре setup схемы. Теперь 12 schemas:
   attempt_failed v4, остальные attempt v3 (полный login/register), session
   completed/failed v2, остальные 5 v1.
2. Route FlowId принадлежит screen-api. Сохранены field flowId, shape {value},
   EncodeDefault, legacy SerialName и ограничения 1..64 opaque characters.
   Только AnalyticsProvider преобразует value в toolkit FlowId; snapshot и
   StateKeeper keys не менялись. В domain и routes нет analytics imports.
3. Обычные reporting Exceptions на start/completed не отменяют выполнение/результат
   блока. Failure reporting остаётся suppressed исходной business exception.
   Flow reporting не мешает construction/transitions; lifecycle reporting не
   прерывает resume/pause/destroy. Caller cancellation checks сохранены, Fatal
   Error не маскируется. Direct strict validation и invariant проверки остаются.

Обновлены skill, UI skill, standard, guide, coverage и проектные инструкции.
Тестовые исходники: полный register при profile/no profile, обе операции при
ошибках подготовки, legacy route shape + provider mapping, start/terminal
reporting failures, flow transitions при reporting failure, lifecycle failure.
Существующие cancellation/restore/DI фикстуры сохранены и синхронизированы.

Саморевью и повторные статические проверки /tmp/verify_three.py прошли, YAML,
links/examples и git diff --check прошли. Удалённых API/схем в актуальных исходниках
не осталось. Исторические review reports описывают прошлые состояния.
Android/iOS builds, KSP, lint и тесты не запускались по ограничению пользователя;
компиляция и runtime не подтверждены. Без Firebase delivery или uploads.
