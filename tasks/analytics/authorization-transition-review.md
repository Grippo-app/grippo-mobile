# AuthTransition — 2026-10-12

> Исторический отчёт: описывает реализацию и проверки этого этапа, включая
> позднее заменённые API/provider/retention. Текущие правила —
> [feature-standard](feature-standard.md), фактическое покрытие и scope —
> [авторизация](authorization-coverage.md). Результаты ниже не подтверждают
> последующие изменения.

Consumer API AuthAnalytics.Authorization теперь содержит один
trackTransition(AuthTransition). Sealed модель в отдельном models/AuthTransition.kt:
OpenRegistration/BackToLogin — data objects, Exit(reason: ExitReason) — data class с
обязательной non-null причиной. Нет универсального nullable parameter bag,
compatibility aliases, runtime casts или новых transition execution wrappers.
trackOperation, presentScreen и save остаются отдельными контрактами.

AuthorizationAnalyticsImpl исчерпывающе dispatch-ит три варианта в прежние методы
internal AuthFlow. Внутренний flow, event schema names/versions/values, snapshot
shape и дедупликация выхода не менялись. Все пять AuthProcess owner calls переведены;
UI по-прежнему исполняет настоящую навигацию после сообщения наблюдаемого факта.

Self-review API visibility/closed types/required payload, exhaustive dispatch,
owner transitions, wire compatibility и canonical instructions.
Skill/reference/standard/guide/coverage закрепляют trackTransition(FeatureTransition),
enum для payload-free вариантов или sealed family при обязательных данных.
Другие feature operations/presentation не объединяются в generic event method.

Выполнены static transition audit 120 Kotlin files (delimiter balance, complete
consumer migration/imports, 5 owner calls, no old public methods, schema versions),
Ruby YAML/interface validation, Python docs links/fences/source alignment,
git diff --check — exit 0. Compilation/KSP/Android/iOS builds и Firebase
requests/uploads не выполнялись. Статические проверки не подтверждают runtime.
