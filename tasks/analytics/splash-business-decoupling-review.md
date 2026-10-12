# Splash: аналитика как декор — 2026-10-12

> Исторический отчёт: описывает реализацию и проверки этого этапа, включая
> позднее заменённые API/provider/retention. Текущие правила —
> [feature-standard](feature-standard.md), фактическое покрытие и scope —
> [авторизация](authorization-coverage.md). Результаты ниже не подтверждают
> последующие изменения.

Splash.restoreSession возвращает собственный SplashDirection. Сохранены проверка
токена, локальный/сетевой профиль, fallback при ошибке профиля, cancellation,
последовательная загрузка справочников только без сессии и итоговая навигация.
Analytics.Session.trackOperation сохраняет generic T без интерпретации.
Удалены модели результата сессии, internal outcomes/stages, classifier import
в Splash, параметры session_outcome/failure_stage и их старые тестовые фикстуры.

Session completed/failed схемы v2: completed обозначает завершение выполнения,
включая обработанный бизнес-fallback, не восстановленную сессию. Необработанная
ошибка даёт failed; cancellation — interrupted. Остальные session схемы v1.

Обновлены canonical skill, UI skill, стандарт, guide, coverage и тестовые исходники.
Саморевью и повторная проверка: бизнес не ветвится по аналитическим моделям;
порядок запросов/ошибок сохранён; API/implementation generic совпадают; удалённых
типов в Kotlin исходниках нет; schema fixtures синхронизированы. Статические
assert-проверки, YAML/документные проверки и git diff --check прошли.
Сборки Android/iOS, KSP и тесты не запускались: runtime/компиляция не подтверждены.
