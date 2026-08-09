// core UI translations (en). This module is data-only.
const dictionary = Object.freeze({
    // ---------- chrome / index.html ----------
    // Title derives from the served project's root folder name (see app.js
    // applyChromeTitle): "{project} · Orchestrator". titleBare is the
    // fallback before the first /api/state lands or if projectRoot is absent.
    'chrome.title':           '{project} · Orchestrator',
    'chrome.titleBare':       'Orchestrator',
    'chrome.lang':            'Language',
    'nav.setup':              'Setup',
    'nav.wizard':             'Launch Wizard',
    'nav.reviewer':           'Reviewer',
    'nav.board':              'Board',
    'nav.design':             'Design',
    'nav.archmap':            'Architecture',
    'nav.groupProject':       'Project',
    'nav.groupIntegrations':  'Integrations',
    'nav.groupStart':         'Start',
    'nav.groupStartHint':     'One-time setup',

    // ---------- common ----------
    'common.optional':          ' (optional)',
    'common.required':          'Required.',
    'common.copy':              'Copy',
    'common.copyAria':          'Copy to clipboard',
    'common.openSetup':         'Open Setup',
    'common.openWizard':        'Open Wizard',
    'common.openBoard':         'Open Board',
    'common.resolveBefore':     'Resolve before continuing: {problems}',
    'common.saveFailed':        'Couldn’t save — is the server still running?',
    'common.retry':             'Retry',
    'common.confirm':           'Confirm',
    'common.requestError.fetch-failed': 'The local Orchestrator server could not be reached. Check that it is running and try again.',
    'common.requestError.invalid-response': 'The server returned an invalid response. Reload after checking the Orchestrator version.',
    'common.requestError.http-error': 'The server rejected the request. Refresh the current state and try again.',
    'common.requestError.not-found': 'The requested item is no longer available. Refresh the current state.',
    'common.requestError.bad-json': 'The request could not be decoded. Reload the page and try again.',
    'common.requestError.internal': 'The server failed safely. Review diagnostics and try again.',
    'common.requestError.invalid-state-response': 'The Orchestrator state response violates its current contract. Restart the server after checking the template version.',
    'common.requestError.unknown': 'The Orchestrator request failed safely for an unrecognized reason. Refresh the current state and try again.',

    // ---------- relative-time helpers ----------
    'time.justNow':    'just now',
    'time.secondsAgo': '{s}s ago',
    'time.minutesAgo': '{m}m ago',
    'time.hoursAgo':   '{h}h ago',
    'time.daysAgo':    '{d}d ago',

    // ---------- toast (clipboard.js) ----------
    'toast.copied':       'Copied!',
    'toast.copyFailed':   'Copy failed',
    'toast.copiedBtn':    'Copied!',
});

export default dictionary;
