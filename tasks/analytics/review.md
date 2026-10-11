# Аудит ядра аналитики

Дата: 2026-10-11. Область: API, catalog/encoder, очередь и конкурентность, политика/identity, память/CPU, Koin и Android/iOS bootstrap, SDK boundary. Фичи не подключены. Прямую отправку в Firebase пользователь исключил из этой проверки.

## Исправления

1. Ошибка старой SDK-конфигурации очищала очередь уже нового пользователя. Теперь очистка относится только к своей revision; новая конфигурация и её события продолжают обработку.
2. CancellationException, выброшенный синхронным sink, останавливал единственный worker, оставляя новые события в очереди. Теперь проверяется реальная отмена coroutine: отменённый worker завершает работу, исключение действующего sink учитывается как transport failure. Автоматического replay/retry нет.
3. Worker без suspension мог занимать Main до полного опустошения очереди, а при постоянных producers — ещё дольше. Добавлены cancellation check и yield после 32 единиц работы; SDK calls остаются последовательными.
4. Перестановка одинаковых user properties считалась сменой identity и теряла ожидающие события. Identity кодируется один раз при обновлении, сравнение maps не зависит от порядка.
5. Отменённый caller мог прервать close до выключения sink. Shutdown выполняет join/disable в NonCancellable, не оставляя работающий worker.

6. Первый grant устанавливал identity при выключенном SDK: SDK мог игнорировать user properties. Теперь grant включает SDK, затем устанавливает identity; собственные события отправляются только после обоих вызовов. Смена identity не вызывает временный Denied. Тестовый sink воспроизводит игнорирование properties при disabled collection.

Первые пять случаев воспроизведены новыми тестами на исходной реализации: 19 core host tests, 5 failures. После исправлений эти тесты прошли. Шестой случай отдельно воспроизведён падающим regression test и исправлен.

Дополнительно: pending events при close и event, инвалидированный между dequeue и send, теперь получают явный терминальный diagnostic. Добавлена стресс-проверка учёта каждого из 2000 событий при параллельной смене identity. Общая проверка четырёх parallel producers запускается также нативно на iOS.

## Чистота и расходы

- Encoder использует подготовленный индекс ключей и required names. Один проход по параметрам, без повторного поиска каждого ключа по списку и промежуточных map/filter/distinct коллекций на каждый event.
- Regex и reserved sets создаются один раз. Reserved event names не ошибочно применяются к names параметров/user properties.
- Identity payload и anonymous clear payload переиспользуются; repeated configuration не перекодирует identity.
- Одна app-lifetime coroutine, conflated wake channel, очередь максимум 128 плюс одно извлечённое событие. Нет coroutine на каждое событие, polling/timer, дисковой очереди или неограниченного буфера.
- Schema: максимум 500 событий, 25 properties, 15 собственных параметров + 10 полей контекста. Debug journal 64 записи; в release выключен. Recording sink не зарегистрирован в production.
- API зависит от serialization-core для публичных сериализуемых типов; JSON parser перенесён в commonTest. Новых версий зависимостей нет.
- Убраны неиспользуемый runtime service-locator для Analytics и строковый debug bridge. Bootstrap экспортирует только attachSink; фичи используют constructor injection typed Analytics.
- В production-коде аналитики явные imports; нет Any в feature event API, default-to-zero/empty преобразований, nullable casts со скрытой подменой значений или automatic retries.

Временный host JVM benchmark, 20000 warmup + 100000 событий, полный контекст: 15 полей с числовыми custom fields — 0,85 мкс/event; 25 полей с числовыми custom fields — 1,10 мкс; 25 полей, включая 15 codes по 100 ASCII-символов — 4,98 мкс. Это локальный JVM замер encoder, не Android/iOS device profile, не latency SDK и не гарантия frame time. Временный benchmark удалён.

## Платформенные границы

Android: Application создаёт Koin, явно применяет Unknown к adapter и attachSink до feature consumers. FLAG_DEBUGGABLE выбирает debug strict validation/journal; release использует Reject/0. Adapter исчерпывающе кодирует sealed values в String/Long/Double Bundle.

iOS: Koin создаётся в iOSApp.init, Firebase configure и attachSink — в AppDelegate.didFinishLaunching. Root ленивый. Analytics API экспортирован через shared XCFramework, concrete engine не экспортируется как feature contract. Swift мост явно кодирует Int64/Double через NSNumber; неизвестный sealed variant вызывает preconditionFailure вместо подмены значения. DEBUG condition определяет режим в Koin.

Manifest/plist задают disabled collection и denied consent defaults; bootstrap дополнительно выключает SDK. Все advertising consent types остаются denied. Старые feature calls/Provider API отсутствуют. Product navigation/auth/consent persistence остаются отдельными фичами; гарантии раннего bootstrap при ранее сохранённом SDK grant описаны в integration-guide.md.

## Итоговые проверки

Результаты финального запуска фиксируются в verification.md. Проверяется настоящий Android APK (debug + release/R8), native iOS tests и shared framework для device/simulator, затем Swift build приложения. В этой проверке приложение с Firebase не запускается и тестовые события не отправляются. Crashlytics mapping upload исключён из release build.
