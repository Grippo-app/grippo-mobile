# Проверка нового ядра аналитики

> Исторический отчёт: описывает реализацию и проверки этого этапа, включая
> позднее заменённые API/provider/retention. Текущие правила —
> [feature-standard](feature-standard.md), фактическое покрытие и scope —
> [авторизация](authorization-coverage.md). Результаты ниже не подтверждают
> последующие изменения.

Дата: 2026-10-11. Фичи пока не подключены.

## Историческая проверка сборок

По просьбе пользователя отправка в Firebase исключена; release Crashlytics
mapping upload отключён. Найденные дефекты и расходы описаны в [review](review.md).
Android debug/release с R8 и shared Debug XCFramework device/simulator были
успешно собраны. Swift Debug Simulator build завершён BUILD SUCCEEDED;
приложение в этом проходе не устанавливалось и не запускалось.

Логи сборок: `/tmp/grippo-analytics-review-accepted.log`,
`/tmp/grippo-analytics-review-swift.log`.
Проверены Android merged manifests debug/release: Application bootstrap и пять
disabled defaults; те же defaults сверены в Info.plist Swift приложения.
Legacy analytics API, временные probes и benchmark отсутствуют в production.

## Обзор кода

Проверены гонки, читаемость, направление зависимостей, данные и память.
Исправлено временное выставление Denied при identity update, которое могло
сбрасывать SDK данные. Удалены прежние вызовы из четырёх ViewModel и публичный
FirebaseAnalyticsProvider. Crashlytics/Messaging сохранены. Фичи получают только
Analytics API; Firebase maps доступны лишь на границе транспорта.

## Практические ограничения

Queue хранится в памяти, максимум 128; нет обещания offline durable delivery, replay или exactly-once. SDK acceptance отдельно от удалённой доставки. Уже начавшийся вызов SDK нельзя отозвать. До отдельной интеграции consent-фичи запуск всегда Unknown, сбор выключен; persistence согласия и ранний bootstrap до SDK auto-init остаются её задачей. Статические defaults не перекрывают ранее сохранённый SDK grant до инициализации приложения. Автоматическое screen reporting потребуется отключить при ручной интеграции навигации.
