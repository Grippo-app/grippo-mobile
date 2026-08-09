import { dom } from '../dom.js';
import { i18n } from '../i18n.js';
import { syncFailureText } from './sync-errors.js';
import { figmaEnumText } from './enum-labels.js';

var el = dom.el;
function t(key, params) { return i18n && typeof i18n.t === 'function' ? i18n.t(key, params) : key; }
function date(value) { var parsed = new Date(value); return isNaN(parsed.getTime()) ? '—' : parsed.toLocaleString(); }

export function createHistoryPagination(loadPage, publish) {
  var items = [];
  var nextCursor = null;
  var loading = false;
  var error = null;
  var restartOnRetry = false;
  var generation = 0;

  function emit() {
    publish({
      items: items.slice(),
      nextCursor: nextCursor,
      loading: loading,
      error: error
    });
  }
  function request(cursor, append, requestGeneration) {
    if (loading) return null;
    loading = true;
    error = null;
    restartOnRetry = false;
    emit();
    return Promise.resolve().then(function () {
      return loadPage(cursor);
    }).then(function (page) {
      if (requestGeneration !== generation) return;
      if (append) {
        var seen = Object.create(null);
        items.forEach(function (item) { seen[item.id] = true; });
        items = items.concat(page.items.filter(function (item) {
          if (seen[item.id]) return false;
          seen[item.id] = true;
          return true;
        }));
      } else items = page.items.slice();
      nextCursor = page.nextCursor;
    }).catch(function (requestError) {
      if (requestGeneration !== generation) return;
      error = requestError;
      restartOnRetry = !!(requestError && (requestError.code === 'bad-cursor' || requestError.kind === 'bad-cursor'));
    }).then(function () {
      if (requestGeneration !== generation) return;
      loading = false;
      emit();
    });
  }
  function open() {
    generation++;
    items = [];
    nextCursor = null;
    loading = false;
    error = null;
    restartOnRetry = false;
    return request(null, false, generation);
  }
  function loadMore() {
    if (loading || !nextCursor) return null;
    return request(nextCursor, true, generation);
  }
  function retry() {
    if (loading || !error) return null;
    return restartOnRetry || !items.length ? open() : request(nextCursor, true, generation);
  }
  function dispose() {
    generation++;
    loading = false;
  }
  return { open: open, loadMore: loadMore, retry: retry, dispose: dispose };
}

export function createHistoryView(options) {
  options = options || {};
  var dialog = el('dialog', { class: 'figma-integration-dialog', attrs: { 'aria-labelledby': 'figma-history-title' } });
  var close = el('button', { type: 'button', class: 'btn btn--ghost', text: t('common.cancel') });
  close.addEventListener('click', function () { if (typeof dialog.close === 'function') dialog.close(); else dialog.removeAttribute('open'); });
  dialog.addEventListener('cancel', function (event) {
    event.preventDefault();
    if (typeof dialog.close === 'function') dialog.close(); else dialog.removeAttribute('open');
  });
  dialog.addEventListener('keydown', function (event) {
    if (event.key !== 'Escape') return;
    event.preventDefault(); event.stopPropagation();
    if (typeof dialog.close === 'function') dialog.close(); else dialog.removeAttribute('open');
  });
  dialog.addEventListener('close', function () { if (options.onClose) options.onClose(); });
  var body = el('div', {
    class: 'figma-integration-history-body', attrs: { 'aria-live': 'polite' }
  });
  var pagerFocusPending = false;
  dialog.appendChild(el('div', { class: 'figma-integration-dialog-head' }, [el('h3', { id: 'figma-history-title', text: t('figma.history.title') }), close]));
  dialog.appendChild(body);
  function open() {
    dialog._figmaTrigger = document.activeElement;
    if (typeof dialog.showModal === 'function') dialog.showModal(); else dialog.setAttribute('open', '');
    setTimeout(function () { close.focus(); }, 0);
  }
  dialog.addEventListener('close', function () {
    var trigger = dialog._figmaTrigger; dialog._figmaTrigger = null;
    if (trigger && typeof trigger.focus === 'function') setTimeout(function () { trigger.focus(); }, 0);
  });
  function render(state) {
    state = state || { items: [], nextCursor: null, loading: false, errorText: null };
    var items = state.items || [];
    while (body.firstChild) body.removeChild(body.firstChild);
    if (!items.length && state.loading) {
      body.appendChild(el('p', { text: t('figma.history.loading') }));
      return;
    }
    if (!items.length && !state.errorText) {
      body.appendChild(el('p', { text: t('figma.history.empty') }));
    }
    items.forEach(function (item) {
      var counters = (item.groups || []).reduce(function (out, group) { out.updated += group.updated || 0; out.warnings += group.warnings || 0; return out; }, { updated: 0, warnings: 0 });
      var planned = (item.planGroups || []).map(function (group) { return figmaEnumText('group', group); }).join(', ');
      var content = [
        el('strong', { text: figmaEnumText('historyResult', item.result) }),
        el('time', { text: date(item.finishedAt || item.startedAt), attrs: { datetime: item.finishedAt || item.startedAt || '', title: item.finishedAt || item.startedAt || '' } }),
        el('span', { text: t('figma.history.groups', { groups: planned }) }),
        el('span', { text: t('figma.history.counts', { updated: counters.updated, warnings: counters.warnings }) })
      ];
      if (item.errorCode) {
        content.push(el('span', {
          class: 'figma-integration-history-error',
          text: syncFailureText(item.errorCode)
        }));
      }
      body.appendChild(el('article', { class: 'figma-integration-history-item' }, content));
    });
    var action = null;
    if (state.errorText) {
      body.appendChild(el('p', {
        class: 'figma-integration-error', text: state.errorText, attrs: { role: 'alert' }
      }));
      action = el('button', {
        type: 'button', class: 'btn btn--ghost', text: t('common.retry')
      });
      action.addEventListener('click', function () {
        pagerFocusPending = true;
        if (options.onRetry) options.onRetry();
      });
      body.appendChild(action);
    } else if (state.nextCursor) {
      action = el('button', {
        type: 'button', class: 'btn btn--ghost',
        text: state.loading ? t('figma.history.loading') : t('figma.history.loadMore'),
        disabled: state.loading
      });
      action.addEventListener('click', function () {
        pagerFocusPending = true;
        if (options.onLoadMore) options.onLoadMore();
      });
      body.appendChild(action);
    }
    if (pagerFocusPending && !state.loading) {
      (action || close).focus();
      pagerFocusPending = false;
    }
  }
  return { dialog: dialog, open: open, render: render };
}
