import { dom } from '../dom.js';
import { i18n } from '../i18n.js';
import { store } from '../store.js';
import { clipboard } from '../clipboard.js';
import { reviewerApi } from '../reviewer-api.js';
import { reviewerErrorMessage } from '../reviewer-errors.js';
import { board } from './board.js';
import { terminal } from '../terminal.js';

var el = dom.el;
var sectionEl = null;
var status = null;
var loading = false;
var loadError = null;
var saving = false;
var settingsMessage = '';
var settingsConflict = false;
var dirtyMode = null;
var dirtyRevision = null;
var activity = {
  pending: { rows: null, nextCursor: null, loading: false },
  failed: { rows: null, nextCursor: null, loading: false }
};
var unsubscribe = null;
var refreshQueued = false;
var diagnosticsOpen = false;
var settingsRequest = null;

var MODE_OPTIONS = [
  { value: 'automatic', label: 'codex.mode.automatic.label', hint: 'codex.mode.automatic.hint' },
  { value: 'require-codex', label: 'codex.mode.require.label', hint: 'codex.mode.require.hint' },
  { value: 'internal-only', label: 'codex.mode.internal.label', hint: 'codex.mode.internal.hint' }
];

var INSTALL_MARKETPLACE = '/plugin marketplace add openai/codex-plugin-cc';
var INSTALL_PLUGIN = '/plugin install codex@openai-codex';
var ENABLE_PLUGIN = '/plugin enable codex@openai-codex';
var UPDATE_PLUGIN = '/plugin update codex@openai-codex';
var INSTALL_RELOAD = '/reload-plugins';
var INSTALL_SETUP = '/codex:setup';
var CODEX_LOGIN = '!codex login';

function t(key, params) {
  return i18n && typeof i18n.t === 'function' ? i18n.t(key, params) : key;
}

function focusKey() {
  var active = document.activeElement;
  return active && sectionEl && sectionEl.contains(active)
    ? active.getAttribute('data-reviewer-focus') : null;
}

function restoreFocus(key) {
  if (!key || !sectionEl) return;
  var candidates = sectionEl.querySelectorAll('[data-reviewer-focus]');
  var target = Array.prototype.find.call(candidates, function (candidate) {
    return candidate.getAttribute('data-reviewer-focus') === key;
  });
  if (target) target.focus();
}

function codeBlock(text, focusId) {
  var wrap = el('div', { class: 'code-block-wrapper' });
  var pre = el('pre', { class: 'code-block' });
  var code = el('code', { text: text });
  pre.appendChild(code);
  wrap.appendChild(pre);
  var button = el('button', {
    type: 'button',
    class: 'copy-btn',
    text: t('common.copy'),
    attrs: {
      'aria-label': t('common.copyAria'),
      'data-reviewer-focus': focusId
    }
  });
  clipboard.attach(button, function () { return code.textContent; });
  wrap.appendChild(button);
  return wrap;
}

function modeLabel(mode) {
  var option = MODE_OPTIONS.find(function (item) { return item.value === mode; });
  return option ? t(option.label) : t('codex.value.unknown');
}

function reviewerLabel(value) {
  var keys = {
    codex: 'codex.reviewer.codex',
    'internal-reviewer': 'codex.reviewer.internal',
    blocked: 'codex.reviewer.blocked',
    mixed: 'codex.reviewer.mixed',
    none: 'codex.reviewer.none',
    unknown: 'codex.value.unknown'
  };
  return t(keys[value] || 'codex.value.unknown');
}

function availabilityLabel(value) {
  var keys = {
    available: 'codex.availability.available',
    unavailable: 'codex.availability.unavailable',
    unknown: 'codex.availability.unknown'
  };
  return t(keys[value] || 'codex.availability.unknown');
}

function installedLabel(value) {
  var keys = {
    yes: 'codex.installed.yes',
    no: 'codex.installed.no',
    unknown: 'codex.installed.unknown'
  };
  return t(keys[value] || 'codex.installed.unknown');
}

function resultLabel(value) {
  var keys = {
    passed: 'codex.result.passed',
    failed: 'codex.result.failed',
    escalated: 'codex.result.escalated',
    unknown: 'codex.value.unknown'
  };
  return t(keys[value] || 'codex.value.unknown');
}

function relativeTime(iso) {
  var ms = Date.parse(iso || '');
  if (!Number.isFinite(ms)) return t('codex.value.never');
  var diff = Math.max(0, Date.now() - ms);
  var minutes = Math.round(diff / 60000);
  if (minutes < 1) return t('time.justNow');
  if (minutes < 60) return t('time.minutesAgo', { m: minutes });
  var hours = Math.round(minutes / 60);
  if (hours < 24) return t('time.hoursAgo', { h: hours });
  return t('time.daysAgo', { d: Math.round(hours / 24) });
}

function localizedTime(iso) {
  var ms = Date.parse(iso || '');
  if (!Number.isFinite(ms)) return t('codex.value.unknown');
  try {
    return new Intl.DateTimeFormat(i18n.get(), {
      dateStyle: 'medium',
      timeStyle: 'short'
    }).format(new Date(ms));
  } catch (error) {
    return new Date(ms).toLocaleString();
  }
}

function durationLabel(startedAt, finishedAt) {
  var start = Date.parse(startedAt || '');
  var finish = Date.parse(finishedAt || '');
  if (!Number.isFinite(start) || !Number.isFinite(finish) || finish < start) return '';
  var seconds = Math.round((finish - start) / 1000);
  if (seconds < 60) return t('codex.duration.seconds', { n: seconds });
  return t('codex.duration.minutes', { n: Math.max(1, Math.round(seconds / 60)) });
}

function reasonLabel(code) {
  var keys = {
    'codex-not-installed': 'codex.reason.codex-not-installed',
    'codex-plugin-disabled': 'codex.reason.codex-plugin-disabled',
    'codex-plugin-broken': 'codex.reason.codex-plugin-broken',
    'codex-contract-missing': 'codex.reason.codex-contract-missing',
    'codex-auth-missing': 'codex.reason.codex-auth-missing',
    'codex-invocation-failed': 'codex.reason.codex-invocation-failed',
    'codex-check-timeout': 'codex.reason.codex-check-timeout',
    'codex-check-output-limit': 'codex.reason.codex-check-failed',
    'codex-check-malformed': 'codex.reason.codex-check-failed',
    'codex-check-failed': 'codex.reason.codex-check-failed',
    'config-invalid': 'codex.reason.config-invalid',
    'require-codex-blocked': 'codex.reason.require-codex-blocked',
    'reviewer-invocation-failed': 'codex.reason.reviewer-invocation-failed',
    'review-failed': 'codex.reason.review-failed',
    'journal-partial': 'codex.reason.journal-partial',
    'activity-bounded': 'codex.reason.activity-bounded',
    'conflicting-active-events': 'codex.reason.conflicting-active-events'
  };
  return code ? t(keys[code] || 'codex.reason.unknown') : '';
}

function reviewerBasisLabel(value) {
  return t('codex.basis.' + ([
    'active-review', 'next-policy', 'conflicting-active-events', 'unavailable'
  ].indexOf(value) >= 0 ? value : 'unavailable'));
}

function recoveryCommands(reasonCode) {
  if (reasonCode === 'codex-not-installed') {
    return [INSTALL_MARKETPLACE, INSTALL_PLUGIN, INSTALL_RELOAD, INSTALL_SETUP];
  }
  if (reasonCode === 'codex-plugin-disabled') {
    return [ENABLE_PLUGIN, INSTALL_RELOAD, INSTALL_SETUP];
  }
  if (reasonCode === 'codex-plugin-broken') {
    return [UPDATE_PLUGIN, INSTALL_RELOAD, INSTALL_SETUP];
  }
  if (reasonCode === 'codex-auth-missing') {
    return [CODEX_LOGIN, INSTALL_SETUP];
  }
  return [INSTALL_SETUP];
}

function overallLabel(value) {
  var keys = {
    operational: 'codex.overall.operational',
    'attention-required': 'codex.overall.attention',
    blocked: 'codex.overall.blocked',
    'no-recent-data': 'codex.overall.noData'
  };
  return t(keys[value] || 'codex.overall.noData');
}

function summaryRow(label, value, secondary, tone) {
  var item = el('li', { class: 'reviewer-summary__row' });
  item.appendChild(el('span', { class: 'reviewer-summary__label', text: label }));
  var content = el('span', { class: 'reviewer-summary__content' });
  content.appendChild(el('strong', {
    class: 'reviewer-summary__value' + (tone ? ' reviewer-summary__value--' + tone : ''),
    text: value
  }));
  if (secondary) content.appendChild(el('small', { text: secondary }));
  item.appendChild(content);
  return item;
}

function lastReviewText(last) {
  if (!last) return { value: t('codex.activity.none'), secondary: '' };
  var duration = durationLabel(last.startedAt, last.finishedAt);
  return {
    value: last.taskStem + ' · ' + resultLabel(last.result),
    secondary: [relativeTime(last.finishedAt), duration].filter(Boolean).join(' · ')
  };
}

function activityRevisionOf(value) {
  return value && value.diagnostics && value.diagnostics.activityRevision || null;
}

function applyStatus(value) {
  var previousActivityRevision = activityRevisionOf(status);
  status = value;
  if (!previousActivityRevision || previousActivityRevision !== activityRevisionOf(value)) {
    resetActivity();
  }
}

function clearDirtySettings() {
  dirtyMode = null;
  dirtyRevision = null;
  settingsRequest = null;
  settingsConflict = false;
}

function reconcileDirtySettings() {
  if (!dirtyMode || !status) return;
  if (dirtyMode === status.config.mode) {
    clearDirtySettings();
    if (!saving) settingsMessage = '';
    return;
  }
  if (dirtyRevision && dirtyRevision !== status.config.revision) {
    settingsRequest = null;
    settingsConflict = true;
    settingsMessage = t('codex.settings.conflict');
  }
}

function openDiagnostics() {
  diagnosticsOpen = true;
  var details = sectionEl && sectionEl.querySelector('.reviewer-diagnostics');
  if (!details) return;
  details.open = true;
  details.scrollIntoView({ block: 'start', behavior: 'smooth' });
}

function diagnosticsButton() {
  var button = el('button', {
    type: 'button',
    class: 'btn btn--primary',
    text: t('codex.action.diagnostics'),
    attrs: { 'data-reviewer-focus': 'view-diagnostics' }
  });
  button.addEventListener('click', openDiagnostics);
  return button;
}

function renderHeader(root) {
  var header = el('div', { class: 'reviewer-header' });
  var title = el('div');
  title.appendChild(el('h2', { class: 'panel-title', text: t('codex.title') }));
  title.appendChild(el('p', { class: 'panel-lead', text: t('codex.lead') }));
  header.appendChild(title);
  var side = el('div', { class: 'reviewer-header__side' });
  if (status) {
    side.appendChild(el('span', {
      class: 'reviewer-badge reviewer-badge--' + status.overall,
      text: overallLabel(status.overall),
      attrs: { role: 'status' }
    }));
  }
  var actions = el('div', { class: 'reviewer-actions' });
  if (status && status.config.state !== 'ready') {
    actions.appendChild(el('a', {
      class: 'btn btn--primary',
      href: '#setup',
      text: t('codex.action.configure'),
      attrs: { 'data-reviewer-focus': 'configure' }
    }));
  } else if (status && status.config.mode === 'require-codex' && status.codex.availability !== 'available') {
    var install = el('button', {
      type: 'button',
      class: 'btn btn--primary',
      // The handler only expands the diagnostics list; "Fix Codex" promised a
      // repair this button has never performed.
      text: t('codex.action.recoverySteps'),
      attrs: { 'data-reviewer-focus': 'install' }
    });
    install.addEventListener('click', openDiagnostics);
    actions.appendChild(install);
  } else if (status && (status.review.integrityWarning || status.diagnostics.activityPartial)) {
    actions.appendChild(diagnosticsButton());
  } else if (status && status.codex.availability === 'unknown') {
    actions.appendChild(recheckButton(true));
  }
  actions.appendChild(recheckButton(false));
  side.appendChild(actions);
  header.appendChild(side);
  root.appendChild(header);
}

function recheckButton(primary) {
  var button = el('button', {
    type: 'button',
    class: 'btn ' + (primary ? 'btn--primary' : 'btn--secondary'),
    text: primary ? t('codex.action.retry') : t('codex.action.refresh'),
    attrs: { 'data-reviewer-focus': primary ? 'retry' : 'refresh' }
  });
  button.disabled = loading;
  button.addEventListener('click', function () {
    loading = true;
    loadError = null;
    render();
    reviewerApi.recheck().then(function (response) {
      applyStatus(response.reviewer);
    }).catch(function (error) {
      loadError = reviewerErrorMessage(error, 'status');
    }).finally(function () {
      loading = false;
      render();
    });
  });
  return button;
}

function renderSummary(root) {
  var list = el('ul', { class: 'reviewer-summary' });
  var last = lastReviewText(status.lastReview);
  list.appendChild(summaryRow(t('codex.summary.review'), status.review.enabled
    ? t('codex.review.enabled') : t('codex.review.unavailable')));
  list.appendChild(summaryRow(t('codex.summary.mode'), modeLabel(status.config.mode)));
  list.appendChild(summaryRow(
    t('codex.summary.activeReviewer'),
    reviewerLabel(status.review.activeReviewer),
    reviewerBasisLabel(status.review.activeReviewerBasis),
    status.review.activeReviewer === 'blocked' || status.review.activeReviewer === 'mixed' ? 'danger' : ''
  ));
  list.appendChild(summaryRow(
    t('codex.summary.codex'),
    availabilityLabel(status.codex.availability),
    status.codex.checkedAt ? t('codex.checkedAt', { time: relativeTime(status.codex.checkedAt) }) : ''
  ));
  list.appendChild(summaryRow(t('codex.summary.fallback'), status.review.fallbackPolicy === 'internal-when-not-detected'
    ? t('codex.fallback.internal') : t('codex.fallback.none')));
  list.appendChild(summaryRow(t('codex.summary.lastReview'), last.value, last.secondary));
  list.appendChild(summaryRow(t('codex.summary.queue'), t('codex.queue.summary', {
    pending: status.counts.pending,
    failed: status.counts.failed
  })));
  root.appendChild(list);
}

function renderRecovery(root) {
  if ((status.codex.availability === 'available' || status.config.mode === 'internal-only') &&
      status.config.state === 'ready' && !status.review.integrityWarning &&
      !status.diagnostics.activityPartial) return;
  var reason = status.config.mode === 'require-codex' &&
    status.codex.availability !== 'available' &&
    status.review.activeReviewer === 'blocked'
    ? 'require-codex-blocked'
    : status.config.reasonCode || status.review.integrityWarning ||
      status.diagnostics.activityReasonCode || status.codex.reasonCode;
  var banner = el('div', {
    class: 'banner ' + (status.overall === 'blocked' ? 'banner--warn' : 'banner--info')
  });
  banner.appendChild(el('strong', { text: reasonLabel(reason) || t('codex.recovery.attention') }));
  var fallbackApplied = status.review.fallbackPolicy === 'internal-when-not-detected' &&
    status.review.activeReviewer === 'internal-reviewer' &&
    (status.review.activeReviewerBasis === 'active-review' ||
      status.codex.availability !== 'available');
  if (fallbackApplied) {
    banner.appendChild(document.createTextNode(' ' + t('codex.recovery.fallbackApplied')));
  }
  root.appendChild(banner);
}

function renderModeControl(root) {
  var card = el('section', { class: 'card reviewer-settings' });
  card.appendChild(el('div', { class: 'reviewer-section-heading' }, [
    el('div', {}, [
      el('h3', { class: 'panel-section-title', text: t('codex.settings.title') }),
      el('p', { class: 'panel-lead', text: t('codex.settings.lead') })
    ])
  ]));
  var selected = dirtyMode || status.config.mode;
  var group = el('div', {
    class: 'reviewer-mode-group',
    attrs: { role: 'radiogroup', 'aria-label': t('codex.settings.title') }
  });
  MODE_OPTIONS.forEach(function (option) {
    var label = el('label', { class: 'reviewer-mode' });
    var input = el('input', {
      type: 'radio',
      class: 'choice-input',
      name: 'reviewer-mode',
      value: option.value,
      attrs: { 'data-reviewer-focus': 'mode-' + option.value }
    });
    input.checked = selected === option.value;
    input.disabled = saving || status.config.state !== 'ready';
    input.addEventListener('change', function () {
      if (option.value === status.config.mode) {
        clearDirtySettings();
      } else {
        if (!dirtyMode) dirtyRevision = status.config.revision;
        dirtyMode = option.value;
        settingsConflict = dirtyRevision !== status.config.revision;
      }
      settingsRequest = null;
      settingsMessage = settingsConflict ? t('codex.settings.conflict') : '';
      var key = 'mode-' + option.value;
      render();
      restoreFocus(key);
    });
    label.appendChild(input);
    label.appendChild(el('span', { class: 'reviewer-mode__copy' }, [
      el('strong', { text: t(option.label) }),
      el('small', { text: t(option.hint) })
    ]));
    group.appendChild(label);
  });
  card.appendChild(group);
  var footer = el('div', { class: 'reviewer-settings__footer' });
  var message = el('span', {
    class: 'reviewer-settings__message',
    text: settingsMessage,
    attrs: { 'aria-live': 'polite' }
  });
  footer.appendChild(message);
  if (settingsConflict) {
    var reload = el('button', {
      type: 'button',
      class: 'btn btn--secondary',
      text: t('codex.settings.reload'),
      attrs: { 'data-reviewer-focus': 'reload-settings' }
    });
    reload.addEventListener('click', function () {
      clearDirtySettings();
      settingsMessage = '';
      loadStatus();
    });
    footer.appendChild(reload);
  }
  var save = el('button', {
    type: 'button',
    class: 'btn btn--primary',
    text: saving ? t('codex.settings.saving') : t('codex.settings.save'),
    attrs: { 'data-reviewer-focus': 'save' }
  });
  save.disabled = saving || settingsConflict || !dirtyMode ||
    status.config.state !== 'ready' || !dirtyRevision;
  save.addEventListener('click', saveSettings);
  footer.appendChild(save);
  card.appendChild(footer);
  root.appendChild(card);
}

function saveSettings() {
  if (!dirtyMode || saving || settingsConflict || !status ||
      status.config.state !== 'ready' || !dirtyRevision) return;
  var requested = dirtyMode;
  var requestedRevision = dirtyRevision;
  if (!settingsRequest || settingsRequest.mode !== requested ||
      settingsRequest.revision !== requestedRevision) {
    settingsRequest = {
      mode: requested,
      revision: requestedRevision,
      key: reviewerApi.idempotencyKey('reviewer-settings')
    };
  }
  saving = true;
  settingsMessage = t('codex.settings.saving');
  render();
  reviewerApi.save(requested, requestedRevision, settingsRequest.key).then(function (response) {
    applyStatus(response.reviewer);
    clearDirtySettings();
    settingsMessage = t('codex.settings.saved');
    // The mutation is already confirmed by the Reviewer response. A secondary
    // store refresh must not turn that success into a false save failure.
    return store.load().catch(function () {});
  }, function (error) {
    // Keep the same idempotency key only for a transport failure with no HTTP
    // response. Typed server failures are durable responses and need a fresh
    // key after the user changes/retries the request.
    if (!error || !error.status) {
      return reviewerApi.status().then(function (value) {
        applyStatus(value);
        if (status.config.mode === requested) {
          clearDirtySettings();
          settingsMessage = t('codex.settings.saved');
        } else {
          reconcileDirtySettings();
          if (!settingsConflict) settingsMessage = reviewerErrorMessage(error, 'settings');
        }
      }, function () {
        settingsMessage = reviewerErrorMessage(error, 'settings');
      });
    }
    settingsRequest = null;
    settingsConflict = !!(error && error.code === 'config-conflict');
    settingsMessage = reviewerErrorMessage(error, 'settings');
  }).finally(function () {
    saving = false;
    render();
    restoreFocus('save');
  });
}

function rowStatus(row, kind) {
  if (kind === 'pending') {
    return row.reasonCode === 'require-codex-blocked'
      ? reasonLabel(row.reasonCode)
      : row.waitingToStart ? t('codex.activity.waitingStatus') : t('codex.activity.pendingStatus');
  }
  return reasonLabel(row.reasonCode) || resultLabel(row.status === 'blocked' ? 'escalated' : row.status);
}

function renderActivityRow(row, kind) {
  var item = el('li', { class: 'reviewer-activity__row' });
  var main = el('div', { class: 'reviewer-activity__main' });
  main.appendChild(el('strong', { text: row.taskTitle || row.taskStem }));
  main.appendChild(el('span', { class: 'reviewer-activity__stem', text: row.taskStem }));
  var meta = [
    row.reviewer && row.reviewer !== 'unknown' ? reviewerLabel(row.reviewer) : null,
    row.startedAt || row.finishedAt ? localizedTime(row.startedAt || row.finishedAt) : null,
    row.reviewAttempt ? t('codex.activity.attempt', { n: row.reviewAttempt }) : null
  ].filter(Boolean);
  if (meta.length) main.appendChild(el('small', { text: meta.join(' · ') }));
  main.appendChild(el('span', {
    class: 'reviewer-activity__status reviewer-activity__status--' + kind,
    text: rowStatus(row, kind)
  }));
  item.appendChild(main);
  var actions = el('div', { class: 'reviewer-actions' });
  var openTask = el('button', {
    type: 'button',
    class: 'btn btn--sm',
    text: t('codex.activity.openTask'),
    attrs: {
      'data-reviewer-focus': kind + '-task-' + row.taskStem + '-' + (row.reviewAttempt || 'unknown')
    }
  });
  openTask.addEventListener('click', function () { board.openTask(row.taskStem); });
  actions.appendChild(openTask);
  if (row.sessionKey) {
    var openSession = el('button', {
      type: 'button',
      class: 'btn btn--sm btn--terminal',
      text: t('codex.activity.openSession'),
      attrs: {
        'data-reviewer-focus': kind + '-session-' + row.taskStem + '-' + (row.reviewAttempt || 'unknown')
      }
    });
    openSession.addEventListener('click', function () { terminal.open(row.sessionKey); });
    actions.appendChild(openSession);
  }
  item.appendChild(actions);
  return item;
}

function rowsFor(kind) {
  return activity[kind].rows || status[kind] || [];
}

function renderActivityList(root, kind) {
  var section = el('section', { class: 'card reviewer-activity' });
  section.appendChild(el('h3', {
    class: 'panel-section-title',
    text: kind === 'pending' ? t('codex.activity.pending') : t('codex.activity.failed')
  }));
  var rows = rowsFor(kind);
  if (!rows.length) {
    section.appendChild(el('p', {
      class: 'panel-lead',
      text: kind === 'pending' ? t('codex.activity.emptyPending') : t('codex.activity.emptyFailed')
    }));
  } else {
    var list = el('ul', {
      class: 'reviewer-activity__list',
      attrs: { 'aria-label': kind === 'pending' ? t('codex.activity.pending') : t('codex.activity.failed') }
    });
    rows.forEach(function (row) { list.appendChild(renderActivityRow(row, kind)); });
    section.appendChild(list);
  }
  var total = status.counts[kind];
  if (total > rows.length || activity[kind].nextCursor) {
    var more = el('button', {
      type: 'button',
      class: 'btn btn--secondary btn--sm',
      text: activity[kind].loading ? t('codex.activity.loading') : t('codex.activity.loadMore'),
      attrs: { 'data-reviewer-focus': 'load-more-' + kind }
    });
    more.disabled = activity[kind].loading;
    more.addEventListener('click', function () { loadMore(kind); });
    section.appendChild(more);
  }
  root.appendChild(section);
}

function renderLastReview(root) {
  var card = el('section', { class: 'card reviewer-last-review' });
  card.appendChild(el('h3', {
    class: 'panel-section-title',
    text: t('codex.lastReview.title')
  }));
  if (!status.lastReview) {
    card.appendChild(el('p', { class: 'panel-lead', text: t('codex.activity.none') }));
    root.appendChild(card);
    return;
  }
  var last = status.lastReview;
  var heading = el('div', { class: 'reviewer-last-review__heading' });
  heading.appendChild(el('strong', { text: last.taskTitle || last.taskStem }));
  heading.appendChild(el('span', {
    class: 'reviewer-activity__status reviewer-activity__status--' +
      (last.result === 'passed' ? 'passed' : 'failed'),
    text: resultLabel(last.result)
  }));
  card.appendChild(heading);
  card.appendChild(el('p', {
    class: 'reviewer-last-review__meta',
    text: [
      last.taskStem,
      reviewerLabel(last.reviewer),
      localizedTime(last.finishedAt),
      durationLabel(last.startedAt, last.finishedAt)
    ].filter(Boolean).join(' · ')
  }));
  if (last.selectionFallbackUsed) {
    card.appendChild(el('p', {
      class: 'reviewer-last-review__notice',
      text: t('codex.lastReview.fallback')
    }));
  }
  var open = el('button', {
    type: 'button',
    class: 'btn btn--secondary btn--sm',
    text: t('codex.activity.openTask'),
    attrs: { 'data-reviewer-focus': 'last-review-task' }
  });
  open.addEventListener('click', function () { board.openTask(last.taskStem); });
  card.appendChild(open);
  root.appendChild(card);
}

function loadMore(kind) {
  var slot = activity[kind];
  if (slot.loading) return;
  var requestRevision = activityRevisionOf(status);
  var requestCursor = slot.nextCursor;
  slot.loading = true;
  render();
  reviewerApi.activity(kind, requestCursor, 20).then(function (response) {
    if (activity[kind] !== slot || !status ||
        activityRevisionOf(status) !== requestRevision ||
        response.revision !== requestRevision) {
      loadStatus();
      return;
    }
    if (slot.rows === null || !requestCursor) slot.rows = response.rows;
    else slot.rows = slot.rows.concat(response.rows);
    slot.nextCursor = response.nextCursor;
  }).catch(function (error) {
    if (error && error.code === 'stale-activity-cursor') {
      loadStatus();
      return;
    }
    loadError = reviewerErrorMessage(error, 'activity');
  }).finally(function () {
    slot.loading = false;
    render();
  });
}

function renderDiagnostics(root) {
  var details = el('details', { class: 'reviewer-diagnostics', open: diagnosticsOpen });
  details.addEventListener('toggle', function () { diagnosticsOpen = details.open; });
  details.appendChild(el('summary', {
    text: t('codex.diagnostics.title'),
    attrs: { 'data-reviewer-focus': 'diagnostics-summary' }
  }));
  var dl = el('dl', { class: 'reviewer-diagnostics__list' });
  [
    [t('codex.diagnostics.detector'), status.codex.detectorVersion],
    [t('codex.diagnostics.source'), status.diagnostics.detectorSource],
    [t('codex.diagnostics.installed'), installedLabel(status.codex.installed)],
    [t('codex.diagnostics.checked'), status.codex.checkedAt ? localizedTime(status.codex.checkedAt) : t('codex.value.never')],
    [t('codex.diagnostics.reason'), reasonLabel(status.codex.reasonCode) || t('codex.value.none')],
    [t('codex.diagnostics.activityRevision'), status.diagnostics.activityRevision]
  ].forEach(function (row) {
    dl.appendChild(el('div', {}, [
      el('dt', { text: row[0] }),
      el('dd', { text: row[1] })
    ]));
  });
  details.appendChild(dl);
  if (status.codex.availability !== 'available') {
    details.appendChild(el('h4', { text: t('codex.installHeading') }));
    details.appendChild(el('p', { class: 'panel-lead', text: t('codex.installLead') }));
    recoveryCommands(status.codex.reasonCode).forEach(function (command, index) {
      details.appendChild(codeBlock(command, 'copy-recovery-' + index));
    });
  }
  root.appendChild(details);
}

function resetActivity() {
  activity.pending = { rows: null, nextCursor: null, loading: false };
  activity.failed = { rows: null, nextCursor: null, loading: false };
}

function loadStatus() {
  if (loading) { refreshQueued = true; return; }
  loading = true;
  loadError = null;
  render();
  reviewerApi.status().then(function (value) {
    applyStatus(value);
    reconcileDirtySettings();
  }).catch(function (error) {
    loadError = reviewerErrorMessage(error, 'status');
  }).finally(function () {
    loading = false;
    render();
    if (refreshQueued) {
      refreshQueued = false;
      loadStatus();
    }
  });
}

function render() {
  if (!sectionEl) return;
  var activeFocus = focusKey();
  while (sectionEl.firstChild) sectionEl.removeChild(sectionEl.firstChild);
  var root = el('div', { class: 'reviewer-panel' });
  renderHeader(root);
  if (loadError) root.appendChild(el('div', { class: 'banner banner--warn', text: loadError }));
  if (!status) {
    root.appendChild(el('p', { class: 'panel-lead', text: loading ? t('codex.loading') : t('codex.error.status') }));
    sectionEl.appendChild(root);
    restoreFocus(activeFocus);
    return;
  }
  renderRecovery(root);
  renderSummary(root);
  renderModeControl(root);
  renderLastReview(root);
  var activityGrid = el('div', { class: 'reviewer-activity-grid' });
  renderActivityList(activityGrid, 'pending');
  renderActivityList(activityGrid, 'failed');
  root.appendChild(activityGrid);
  renderDiagnostics(root);
  sectionEl.appendChild(root);
  restoreFocus(activeFocus);
}

function onStoreChange() {
  if (sectionEl && !sectionEl.hidden) loadStatus();
}

export const reviewer = {
  mount: function (rootEl) {
    sectionEl = rootEl;
    if (!unsubscribe) unsubscribe = store.on('change', onStoreChange);
    render();
    loadStatus();
  },
  refresh: function () {
    render();
    loadStatus();
  }
};
