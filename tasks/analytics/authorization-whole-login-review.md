# Цельный сценарий входа — 2026-10-12

LoginUseCase снова предоставляет loginEmail/loginGoogle/loginApple: серверный
вход, затем private prepare, возвращающий наличие профиля. Порядок запросов,
getOrThrow, условная загрузка данных и регистрация push сохранены.
LoginViewModel выполняет один trackOperation и навигацию по Boolean.
Attempt API и фабрика событий сохраняют generic T без кастов или дополнительных
обёрток. Preparation.Login удалён; подготовка регистрации не изменена.

Изменение смысла attempt фиксируется версиями: failed v3, остальные attempt v2.
Success и duration теперь охватывают подготовку, а её ошибка — failure входа.
Схемы setup остаются для registration; новые отчёты должны учитывать версии.

Саморевью: проверены три вызова VM, приватность prepare, отсутствие старых API
и login preparation, generic API/implementation/result path, схема и актуальные
примеры. Обновлены тестовые исходники для полного успеха, отсутствующего профиля,
отсутствующего push, server/preparation failure и cancellation. Статические
assert-проверки и git diff --check прошли. Сборки, KSP и тесты не запускались;
компиляция и runtime не подтверждены по ограничению пользователя.
