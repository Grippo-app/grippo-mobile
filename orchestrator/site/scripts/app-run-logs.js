import { dom } from './dom.js';
import { i18n } from './i18n.js';
import { errorCode } from './data/request-json.js';
import { appRunErrorMessage } from './app-run-errors.js';

var el = dom.el;
// Retrying is only honest while the failure can still clear on its own. A pruned or
// unknown job and a rejected request never will, so the loop stops there and states
// the real reason instead of promising a retry that can only repeat itself.
var TERMINAL = Object.freeze({
  'job-not-found': 1,
  'not-found': 1,
  'bad-log-query': 1,
  'bad-log-cursor': 1,
  'bad-log-limit': 1,
  'bad-host': 1
});
function t(key) { return i18n.t(key); }
function phaseLabel(value) {
  var key = 'appRun.stage.' + value;
  var translated = t(key);
  return translated === key ? t('appRun.state.unknown') : translated;
}

function open(options) {
  var dialog = el('dialog', { class: 'app-run-logs', attrs: { 'aria-labelledby': 'app-run-logs-title' } });
  var title = el('h2', { id: 'app-run-logs-title', text: t('appRun.logs') });
  var close = el('button', { type: 'button', class: 'btn btn--ghost btn--small', text: t('appRun.close') });
  var output = el('ol', { class: 'app-run-log-lines', attrs: { 'aria-live': 'off' } });
  var scope = el('p', { class: 'app-run-hint', text: t('appRun.logsScope') });
  dialog.appendChild(el('div', { class: 'app-run-dialog__head' }, [title, close]));
  dialog.appendChild(scope); dialog.appendChild(output);
  document.body.appendChild(dialog);
  var cursor = null, stopped = false;
  var failure = null;
  function poll() {
    if (stopped) return;
    options.load(cursor).then(function (result) {
      if (failure) { failure.remove(); failure = null; }
      // Reading a build error means scrolling up, so measure before appending and
      // follow new lines only for a view that was already parked at the bottom.
      var atBottom = (output.scrollTop + output.clientHeight >= output.scrollHeight - 24);
      (result.rows || []).forEach(function (row) {
        output.appendChild(el('li', { class: 'app-run-log-line app-run-log-line--' + row.source }, [
          el('span', { class: 'app-run-log-line__phase', text: phaseLabel(row.phase) }),
          el('code', { text: row.text })
        ]));
      });
      cursor = result.nextCursor || cursor;
      if (atBottom) output.scrollTop = output.scrollHeight;
      setTimeout(poll, 1500);
    }).catch(function (error) {
      if (TERMINAL[errorCode(error)]) {
        stopped = true;
        if (failure) failure.remove();
        failure = el('p', {
          class: 'banner banner--warn',
          attrs: { role: 'alert' },
          text: appRunErrorMessage(error)
        });
        dialog.insertBefore(failure, output);
        return;
      }
      if (!failure) {
        failure = el('p', {
          class: 'banner banner--warn',
          attrs: { role: 'status' },
          text: t('appRun.logsUnavailable')
        });
        dialog.insertBefore(failure, output);
      }
      setTimeout(poll, 3000);
    });
  }
  function finish() {
    stopped = true;
    try { dialog.close(); } catch (_) {}
    dialog.remove();
  }
  close.addEventListener('click', finish);
  dialog.addEventListener('cancel', function (event) { event.preventDefault(); finish(); });
  dialog.showModal(); poll();
}

export const appRunLogs = { open: open };
