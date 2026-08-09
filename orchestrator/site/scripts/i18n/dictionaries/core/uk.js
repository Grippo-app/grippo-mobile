// core UI translations (uk). This module is data-only.
const dictionary = Object.freeze({
    // ---------- chrome / index.html ----------
    // The title is assembled from the project root folder name; titleBare
    // is used before the first /api/state response or without projectRoot.
    'chrome.title':           '{project} · Оркестратор',
    'chrome.titleBare':       'Оркестратор',
    'chrome.lang':            'Мова',
    'nav.setup':              'Налаштування',
    'nav.wizard':             'Майстер запуску',
    'nav.reviewer':           'Reviewer',
    'nav.board':              'Дошка',
    'nav.design':             'Дизайн',
    'nav.archmap':            'Архітектура',
    'nav.groupProject':       'Проєкт',
    'nav.groupIntegrations':  'Інтеграції',
    'nav.groupStart':         'Старт',
    'nav.groupStartHint':     'Разове налаштування',

    // ---------- common ----------
    'common.optional':          ' (необов’язково)',
    'common.required':          'Обов’язково.',
    'common.copy':              'Копіювати',
    'common.copyAria':          'Скопіювати у буфер',
    'common.openSetup':         'Відкрити налаштування',
    'common.openWizard':        'Відкрити Майстер',
    'common.openBoard':         'Відкрити Дошку',
    'common.resolveBefore':     'Перш ніж продовжити, виправте: {problems}',
    'common.saveFailed':        'Не вдалося зберегти — сервер досі працює?',
    'common.retry':             'Повторити',
    'common.confirm':           'Підтвердити',
    'common.requestError.fetch-failed': 'Не вдалося зв’язатися з локальним сервером Orchestrator. Перевірте, що він запущений, і повторіть спробу.',
    'common.requestError.invalid-response': 'Сервер повернув некоректну відповідь. Перевірте версію Orchestrator і перезавантажте сторінку.',
    'common.requestError.http-error': 'Сервер відхилив запит. Оновіть поточний стан і повторіть спробу.',
    'common.requestError.not-found': 'Запитаний об’єкт більше недоступний. Оновіть поточний стан.',
    'common.requestError.bad-json': 'Не вдалося розібрати запит. Перезавантажте сторінку й повторіть спробу.',
    'common.requestError.internal': 'Сервер безпечно зупинив операцію. Перевірте діагностику й повторіть спробу.',
    'common.requestError.invalid-state-response': 'Відповідь стану Orchestrator порушує поточний контракт. Перевірте версію шаблону й перезапустіть сервер.',
    'common.requestError.unknown': 'Запит Orchestrator безпечно зупинено з нерозпізнаної причини. Оновіть поточний стан і повторіть спробу.',

    // ---------- relative-time helpers ----------
    'time.justNow':    'щойно',
    'time.secondsAgo': '{s} с тому',
    'time.minutesAgo': '{m} хв тому',
    'time.hoursAgo':   '{h} год тому',
    'time.daysAgo':    '{d} днів тому',

    // ---------- toast (clipboard.js) ----------
    'toast.copied':       'Скопійовано!',
    'toast.copyFailed':   'Не вдалося скопіювати',
    'toast.copiedBtn':    'Скопійовано!',
});

export default dictionary;
