export function createBoardFormatters(dependencies) {
  var t = dependencies.t;
  var getLanguage = dependencies.getLanguage;
  var now = dependencies.now;

  // Count-key selection is intentionally limited to the dictionaries Board
  // currently owns: Russian uses one/few/many; every other UI language uses
  // the existing one/other family.
  function pluralCategory(n) {
    var language = getLanguage();
    if (language === 'ru') {
      var m10 = n % 10;
      var m100 = n % 100;
      if (m10 === 1 && m100 !== 11) return 'one';
      if (m10 >= 2 && m10 <= 4 && (m100 < 12 || m100 > 14)) return 'few';
      return 'many';
    }
    return n === 1 ? 'one' : 'other';
  }

  function pluralLabel(prefix, n) {
    return t(prefix + '.' + pluralCategory(n), { n: n });
  }

  function pluralTemplate(prefix, n, params) {
    return t(prefix + '.' + pluralCategory(n), Object.assign({ n: n }, params || {}));
  }

  function parseIso(value) {
    if (!value) return null;
    var parsed = new Date(value);
    if (isNaN(parsed.getTime())) return null;
    return parsed;
  }

  // A future-dated lock (clock skew or NFS metadata) must not suppress the
  // worker-offline signal indefinitely. Worker support consumes this clamp as
  // presentation evidence only; it never authorizes lock mutation.
  function clampNow(milliseconds) {
    return Math.min(milliseconds, now());
  }

  function relativeTime(iso) {
    var parsed = parseIso(iso);
    if (parsed == null) return t('board.timeUnknown');
    var diff = now() - parsed.getTime();
    if (diff < 0) diff = 0;
    var seconds = Math.round(diff / 1000);
    if (seconds < 30) return t('time.justNow');
    if (seconds < 60) return t('time.secondsAgo', { s: seconds });
    var minutes = Math.round(seconds / 60);
    if (minutes < 60) return t('time.minutesAgo', { m: minutes });
    var hours = Math.round(minutes / 60);
    if (hours < 24) return t('time.hoursAgo', { h: hours });
    var days = Math.round(hours / 24);
    return t('time.daysAgo', { d: days });
  }

  function timestampLabel(iso) {
    var parsed = parseIso(iso);
    if (!parsed) return iso || '';
    var language = getLanguage();
    var locale = language === 'ru' ? 'ru-RU' : language === 'uk' ? 'uk-UA' : 'en-US';
    var absolute;
    try {
      absolute = parsed.toLocaleString(locale, {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (error) {
      absolute = iso;
    }
    return relativeTime(iso) + ' · ' + absolute;
  }

  return {
    pluralLabel: pluralLabel,
    pluralTemplate: pluralTemplate,
    parseIso: parseIso,
    clampNow: clampNow,
    relativeTime: relativeTime,
    timestampLabel: timestampLabel
  };
}
