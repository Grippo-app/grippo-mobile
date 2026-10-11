# Авторизация: будущая матрица интеграции

Статус: proposal, **не подключено**. Общий стандарт сначала; авторизация реализуется отдельным следующим этапом. Scope первого подключения: Login Email/Google/Apple, регистрация credentials и проверка сессии. Создание профиля — следующий отдельный сценарий.

## Точки входа

| Typed entry point | Наблюдаемый факт | Owner |
|---|---|---|
| LaunchWithoutSession | Splash не нашёл сохранённый token | Auth navigation owner |
| TokenAbsent | Root наблюдает отсутствие token во время работы | Root → Auth; не называть expired без typed причины |
| LoginRegistration | Login открывает credentials | AuthProcess owner; origin Login |
| ProfileSetupReturn | Возврат из незавершённого профиля к Login | ProfileCreation → Auth owner |

LaunchSource notification/deep link передаётся только из достоверных app-entry данных. Flow context сохраняется в navigation state и не пересоздаётся при back/foreground. Смена формы не теряет provenance, а открытие самостоятельного registration scenario получает свой flow с известным parent/origin при необходимости.

## Покрытие операций

| Триггер | Предлагаемые события | Single owner | Typed параметры / outcomes | Обязательная проверка |
|---|---|---|---|---|
| Реальный вход в сценарий | auth_flow_started | Auth owner | EntryPoint через FlowContext | Initial/restore без дубля start |
| Login submit | auth_attempt_started / succeeded / failed / cancelled / interrupted | Provider + use-case coordinator, один attempt | AuthMethod Email/Google/Apple; AuthAction Login; DurationMillis; FailureStage/Category | Один итог, повторная попытка новый ID |
| Registration submit | auth_attempt_started / succeeded / failed / interrupted | Registration use-case coordinator | AuthAction Register, метод Email | Account created отличается от downstream preparation |
| Подготовка после auth | auth_preparation_started / finished / failed / interrupted | Use-case owner | Этап ProfileLoad / DataLoad / PushRegistration; typed outcome | Ошибка подготовки не объявляет auth failure |
| Splash session check | auth_session_check_started / completed / failed / interrupted | Splash coordinator | NoSession / Restored / NeedsProfile; typed failure stage | No token отличается от profile-load error |
| Явное закрытие сценария | auth_flow_exited | Auth navigation owner | AuthExitReason, last known step | Не отправлять из onDestroy |
| Submit отклонён валидацией | auth_form_rejected | Form ViewModel | Typed field/rule, без введённого значения | Только реально отклонённый submit |

## Экраны и причины

Splash, Login и Registration — leaf surfaces; Auth/AuthProcess контейнеры screen_view не отправляют. Initial/Action/Back/Restore/External предоставляет навигационный owner. Переключение Login → Registration сохраняет реальный referrer; back и foreground return различаются. Нельзя определять reason по тексту ошибки, lifecycle destroy или отсутствию token.

## До начала реализации устранить ограничения типизации

- LoginUseCase/RegisterUseCase смешивают auth, user/data loading и push registration. Нужна граница фактов/этапов, чтобы auth success фиксировался после server confirmation, а downstream failure имел свой outcome. Не добавлять второй attempt в UI и data одновременно.
- AppleAuthException сейчас не различает user cancellation типом. Добавить typed reason у provider до аналитического mapping; строку сообщения не парсить.
- Google/Apple LoginViewModel сейчас переводит provider error в AppError.Expected и теряет typed reason. Классифицировать до этого преобразования, не менять existing error UI ради tracking.
- Splash catch(Throwable) сейчас поглощает cancellation вместе с profile-load failures. При будущей интеграции сохранить реальную cancellation, иначе interrupted outcome будет ложным.
- Token absence в Root сам по себе не объясняет session expiration/logout. Передавать только наблюдаемое, дополнительные причины требуют typed domain source.

## Приватность и приёмка

Не собирать email/password/provider tokens, authorization code, push token, имя, тело запроса, полный URL и exception message. Provider cancellation и coroutine interruption различаются. Consent/UserId/reset имеют отдельного владельца и не включаются автоматически с подключением событий.

При реализации заполнить exact class/function owners и реальные fixture/test names по feature-coverage-template.md. Сейчас эта матрица описывает контракт следующего этапа, не подтверждает его покрытие.
