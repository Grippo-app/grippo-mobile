# Авторизация: типизированные операции — 2026-10-12

> Исторический отчёт: описывает реализацию и проверки этого этапа, включая
> позднее заменённые API/provider/retention. Текущие правила —
> [feature-standard](feature-standard.md), фактическое покрытие и scope —
> [авторизация](authorization-coverage.md). Результаты ниже не подтверждают
> последующие изменения.

## Изменение

Специализированные authenticateLogin/createAccount/prepareLogin/prepareRegistration/
checkSession заменены перегрузками trackOperation с закрытой моделью AuthOperation
в analytics-api/models. Attempt принимает Unit, Preparation — Boolean, SessionCheck
— SessionResult. Нет generic result casts, nullable параметров или второй execution
обвязки. Google/Apple credentials и server confirmation находятся в одном callback
в LoginViewModel; email/register запросы остаются в своих ViewModels. Подготовка
по-прежнему выполняется исходными use cases, восстановление сессии — SplashViewModel.

Удалены AuthStage, AuthStageException, AuthEventMatrix и AuthPreparationKind.
Provider/Server не классифицируются: auth_attempt_failed v2 содержит только method,
action, duration и category. Остальные 15 schemas остаются v1. SessionCheck/ProfileLoad
остаются наблюдаемыми результатами; preparation содержит результат всей операции.
Отчёты с фильтром Provider/Server нужно обновить; schema version не мигрирует отчёты.

AuthAnalyticsState.kt переименован в AuthAnalyticsRetention.kt; StateKeeper keys,
legacy SerialName и привязка того же retained scenario к ViewModel сохранены.
AuthAnalyticsIntegrationModule оставлен: другого существующего DI-модуля у этой
UI-фичи нет. Единственный stateless classifier binding остаётся @Single + .module.

## Self-review и повторная проверка

- Просмотрены API, descriptor hierarchy, сценарии, operation events и schemas,
  UI call sites, classifier/DI/retention, стандарт и skill.
- Исправлены остатки старых stage declarations, версии схем, imports,
  форматирование callback, obsolete stage helpers и активные примеры API.
- Проверен общий toolkit executor: исходная ошибка перекидывается без обёртки,
  reporting defect добавляется в suppressed, coroutine cancellation отдельна
  от typed SDK cancellation. Новых scopes/jobs/retries не добавлено.

## Выполненные проверки

- Python static audit: 117 Kotlin файлов, balance delimiters, отсутствие старых
  symbols/imports, отсутствие UI/domain/SDK imports в analytics implementation,
  16 schema names/versions, exact typed signatures, сохранённые
  retention keys и legacy SerialName, documentation fences. Exit 0.
- Ruby YAML validation canonical analytics skill: exit 0.
- git diff --check: exit 0.

По запросу пользователя Android/iOS builds, compilation, KSP не
запускались. Статический аудит не подтверждает компиляцию или runtime поведение.
Firebase/provider requests и uploads не выполнялись. Исторические результаты
проверок не используются как подтверждение текущего изменения.
