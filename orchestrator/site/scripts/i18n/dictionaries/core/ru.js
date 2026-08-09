// core UI translations (ru). This module is data-only.
const dictionary = Object.freeze({
    // ---------- chrome / index.html ----------
    // Тайтл собирается из имени корневой папки проекта (см. app.js
    // applyChromeTitle): «{project} · Оркестратор». titleBare — запасной
    // вариант до первого /api/state или если projectRoot отсутствует.
    'chrome.title':           '{project} · Оркестратор',
    'chrome.titleBare':       'Оркестратор',
    'chrome.lang':            'Язык',
    'nav.setup':              'Сетап',
    'nav.wizard':             'Мастер запуска',
    'nav.reviewer':           'Reviewer',
    'nav.board':              'Доска',
    'nav.design':             'Дизайн',
    'nav.archmap':            'Архитектура',
    'nav.groupProject':       'Проект',
    'nav.groupIntegrations':  'Интеграции',
    'nav.groupStart':         'Старт',
    'nav.groupStartHint':     'Разовая настройка',

    // ---------- common ----------
    'common.optional':          ' (опционально)',
    'common.required':          'Обязательно.',
    'common.copy':              'Копировать',
    'common.copyAria':          'Скопировать в буфер',
    'common.openSetup':         'Открыть Сетап',
    'common.openWizard':        'Открыть Мастер',
    'common.openBoard':         'Открыть Доску',
    'common.resolveBefore':     'Сначала исправь: {problems}',
    'common.saveFailed':        'Не удалось сохранить — сервер ещё запущен?',
    'common.retry':             'Повторить',
    'common.confirm':           'Подтвердить',
    'common.requestError.fetch-failed': 'Не удалось связаться с локальным сервером Orchestrator. Проверьте, что он запущен, и повторите попытку.',
    'common.requestError.invalid-response': 'Сервер вернул некорректный ответ. Проверьте версию Orchestrator и перезагрузите страницу.',
    'common.requestError.http-error': 'Сервер отклонил запрос. Обновите текущее состояние и повторите попытку.',
    'common.requestError.not-found': 'Запрошенный объект больше недоступен. Обновите текущее состояние.',
    'common.requestError.bad-json': 'Не удалось разобрать запрос. Перезагрузите страницу и повторите попытку.',
    'common.requestError.internal': 'Сервер безопасно остановил операцию. Проверьте диагностику и повторите попытку.',
    'common.requestError.invalid-state-response': 'Ответ состояния Orchestrator нарушает текущий контракт. Проверьте версию шаблона и перезапустите сервер.',
    'common.requestError.unknown': 'Запрос Orchestrator безопасно остановлен по нераспознанной причине. Обновите текущее состояние и повторите попытку.',

    // ---------- relative-time helpers ----------
    'time.justNow':    'только что',
    'time.secondsAgo': '{s} сек назад',
    'time.minutesAgo': '{m} мин назад',
    'time.hoursAgo':   '{h} ч назад',
    'time.daysAgo':    '{d} дн назад',

    // ---------- toast (clipboard.js) ----------
    'toast.copied':       'Скопировано!',
    'toast.copyFailed':   'Не удалось скопировать',
    'toast.copiedBtn':    'Скопировано!',
});

export default dictionary;
