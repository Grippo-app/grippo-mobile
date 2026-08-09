import { dom } from '../dom.js';

export function createBoardHealthController(dependencies) {
  var t = dependencies.t;
  var clipboard = dependencies.clipboard;
  var boardModal = dependencies.boardModal;
  var state = dependencies.state;
  var taskIntegrity = dependencies.taskIntegrity;
  var startupRecoveryState = dependencies.startupRecoveryState;
  var startupRecoveryReason = dependencies.startupRecoveryReason;
  var boardLoadErrorText = dependencies.boardLoadErrorText;
  var finalizations = dependencies.finalizations;
  var openFinalizationModal = dependencies.openFinalizationModal;
  var workerSupport = dependencies.workerSupport;
  var getSectionElement = dependencies.getSectionElement;
  var getToolbarElement = dependencies.getToolbarElement;
  var el = dom.el;

  // Keep the compact status control mounted across SSE refreshes. Replacing
  // its button used to drop keyboard focus and could make an unchanged status
  // announce repeatedly to assistive technology.
  var healthEl = null;
  var healthButtonEl = null;
  var healthSeverityEl = null;
  var healthSummaryEl = null;
  var healthAnnouncementEl = null;
  var healthAnnouncementSignature = null;
  var healthIssues = [];

  function integritySeverityLabel(severity) {
    var safe = ['info', 'warning', 'error', 'blocker'].indexOf(severity) >= 0 ? severity : 'error';
    return t('board.integrity.severity.' + safe);
  }

  function boardHealthIssues(storeState) {
    var issues = [];
    var integrity = taskIntegrity();
    (Array.isArray(integrity.findings) ? integrity.findings : []).forEach(function (item) {
      if (!item) return;
      issues.push({
        kind: 'integrity',
        code: item.code || 'TASK_STATE_FINDING',
        severity: item.severity === 'warning' || item.severity === 'info' ? 'warning' : 'error',
        stem: item.stem || null,
        message: item.message || t('board.integrity.title'),
        recovery: item.recovery || null,
        paths: Array.isArray(item.paths) ? item.paths.slice() : [],
        pathsTruncated: item.pathsTruncated === true
      });
    });
    if (integrity.truncated === true || integrity.runtimeStatusTruncated === true) {
      issues.push({
        kind: 'diagnostics',
        code: 'TASK_DIAGNOSTICS_TRUNCATED',
        severity: 'warning',
        stem: null,
        message: t('board.status.diagnosticsTruncated'),
        recovery: t('board.status.retryHint'),
        paths: [],
        diagnosticsTruncated: true
      });
    }
    if (state.error) issues.unshift({
      kind: 'summary',
      code: 'BOARD_SUMMARY_UNAVAILABLE',
      severity: 'error',
      stem: null,
      message: boardLoadErrorText(state.error),
      recovery: t('board.status.retryHint'),
      paths: []
    });
    var limitations = state.summary && Array.isArray(state.summary.limitations)
      ? state.summary.limitations : [];
    limitations.forEach(function (code) {
      if (code === 'task-integrity' && issues.some(function (item) {
        return item.kind === 'integrity';
      })) return;
      var key = 'board.summaryLimitation.' + String(code).replace(/-/g, '_');
      var label = t(key);
      issues.push({
        kind: 'summary',
        code: String(code || 'SUMMARY_PARTIAL').toUpperCase().replace(/-/g, '_'),
        severity: 'warning',
        stem: null,
        message: label === key ? t('board.summaryLimitation.unknown') : label,
        recovery: null,
        paths: []
      });
    });
    var startup = startupRecoveryState();
    if (startup.status !== 'ready') issues.push({
      kind: 'startup',
      code: startup.reasonCode || 'STARTUP_RECOVERY_PENDING',
      severity: startup.status === 'blocked' ? 'error' : 'warning',
      stem: null,
      message: t(startup.status === 'blocked'
        ? 'board.startupRecovery.blockedDetail' : 'board.startupRecovery.pendingDetail'),
      recovery: startup.reasonCode ? startupRecoveryReason(startup.reasonCode) : null,
      paths: []
    });
    finalizations().forEach(function (fin) {
      issues.push({
        kind: 'finalization',
        code: fin.errorCode || 'FINALIZATION_REQUIRED',
        severity: fin.status === 'corrupt' || fin.recoverable === false ? 'error' : 'warning',
        stem: fin.stem || null,
        message: fin.status === 'corrupt'
          ? t('board.finalization.corrupt') : t('board.finalization.explain'),
        recovery: fin.recoverable === true ? t('board.finalization.resume') : t('board.finalization.inspect'),
        paths: [],
        finalization: fin
      });
    });
    var publication = storeState && storeState.progress &&
      Array.isArray(storeState.progress.publicationRecoveryIssues)
      ? storeState.progress.publicationRecoveryIssues : [];
    publication.forEach(function (issue) {
      var safe = issue && (issue.code === 'CREATION_INCOMPLETE' || issue.code === 'EDIT_INCOMPLETE');
      issues.push({
        kind: 'publication',
        code: issue && issue.code || 'PUBLICATION_RECOVERY_REQUIRED',
        severity: safe ? 'warning' : 'error',
        stem: issue && issue.stem || null,
        message: t(safe
          ? 'board.publicationRecovery.autoDetail' : 'board.publicationRecovery.unsafeDetail',
        { n: publication.length }),
        recovery: null,
        paths: []
      });
    });
    if (workerSupport.workerLooksOffline(storeState)) issues.push({
      kind: 'worker',
      code: 'TASK_RUNNER_OFFLINE',
      severity: 'warning',
      stem: null,
      message: t('board.status.runnerOffline'),
      recovery: null,
      paths: [],
      workerHelp: true
    });
    return issues;
  }

  function boardHealthSeverity(issues) {
    if (issues.some(function (item) { return item.severity === 'error'; })) return 'error';
    if (issues.length) return 'warning';
    return 'ok';
  }

  function boardHealthCopyText(issues, capturedAt) {
    return JSON.stringify({
      capturedAt: capturedAt,
      status: boardHealthSeverity(issues),
      issues: issues.map(function (item) {
        return {
          code: item.code,
          severity: item.severity,
          stem: item.stem,
          message: item.message,
          recovery: item.recovery,
          paths: item.paths,
          pathsTruncated: item.pathsTruncated === true,
          diagnosticsTruncated: item.diagnosticsTruncated === true
        };
      })
    }, null, 2);
  }

  function openBoardHealthModal(issues) {
    var capturedAt = new Date().toISOString();
    var body = el('div', { class: 'board-modal__body board-health-modal' });
    body.appendChild(el('h3', {
      class: 'board-modal__title',
      text: t('board.status.dialogTitle')
    }));
    body.appendChild(el('p', {
      class: 'board-health-modal__summary',
      text: issues.length
        ? t('board.status.dialogSummary', { count: issues.length })
        : t('board.status.okDetail')
    }));
    var list = el('ul', { class: 'board-health-modal__list' });
    issues.forEach(function (item) {
      var row = el('li', {
        class: 'board-health-modal__finding board-health-modal__finding--' + item.severity
      });
      var head = el('p', { class: 'board-health-modal__finding-head' });
      head.appendChild(el('code', { text: item.code }));
      head.appendChild(el('span', {
        class: 'board-health-modal__severity',
        text: integritySeverityLabel(item.severity)
      }));
      if (item.stem) head.appendChild(el('code', { text: item.stem }));
      row.appendChild(head);
      if (item.message) row.appendChild(el('p', { class: 'board-health-modal__message', text: item.message }));
      if (Array.isArray(item.paths) && item.paths.length) {
        row.appendChild(el('strong', { class: 'board-health-modal__label', text: t('board.integrity.paths') }));
        var paths = el('ul', { class: 'board-health-modal__paths' });
        item.paths.forEach(function (affectedPath) {
          paths.appendChild(el('li', {}, [el('code', { text: affectedPath })]));
        });
        row.appendChild(paths);
      }
      if (item.pathsTruncated) row.appendChild(el('p', {
        class: 'board-health-modal__truncated',
        text: t('board.status.pathsTruncated')
      }));
      if (item.recovery) {
        row.appendChild(el('strong', { class: 'board-health-modal__label', text: t('board.integrity.recovery') }));
        row.appendChild(el('p', { class: 'board-health-modal__recovery', text: item.recovery }));
      }
      if (item.finalization && item.finalization.recoverable === true) {
        var resume = el('button', {
          type: 'button', class: 'btn btn--sm', text: t('board.finalization.resume')
        });
        resume.addEventListener('click', function () {
          openFinalizationModal(item.finalization);
        });
        row.appendChild(resume);
      }
      if (item.workerHelp) {
        var help = el('button', {
          type: 'button', class: 'btn btn--sm', text: t('board.workerOffline.noticeAction')
        });
        help.addEventListener('click', workerSupport.openHelp);
        row.appendChild(help);
      }
      list.appendChild(row);
    });
    body.appendChild(list);
    var actions = el('div', { class: 'board-modal__actions' });
    var copy = el('button', {
      type: 'button', class: 'btn btn--primary', text: t('board.status.copy')
    });
    copy.addEventListener('click', function () {
      clipboard.copy(boardHealthCopyText(issues, capturedAt));
    });
    actions.appendChild(copy);
    actions.appendChild(boardModal.createCloseButton());
    body.appendChild(actions);
    boardModal.open(body);
  }

  function renderBoardStatus(storeState) {
    var sectionEl = getSectionElement();
    var toolbarEl = getToolbarElement();
    var issues = boardHealthIssues(storeState);
    var severity = boardHealthSeverity(issues);
    healthIssues = issues;
    if (!healthEl) {
      healthEl = el('section', {
        class: 'board-health',
        attrs: { role: 'region' }
      });
      healthButtonEl = el('button', {
        type: 'button',
        class: 'board-health__button',
        attrs: { 'aria-haspopup': 'dialog' }
      });
      healthButtonEl.appendChild(el('span', {
        class: 'board-health__dot',
        attrs: { 'aria-hidden': 'true' }
      }));
      healthButtonEl.appendChild(el('strong', {
        class: 'board-health__label',
        text: ''
      }));
      healthSeverityEl = el('span', { class: 'board-health__severity', text: '' });
      healthButtonEl.appendChild(healthSeverityEl);
      healthSummaryEl = el('span', { class: 'board-health__summary', text: '' });
      healthButtonEl.appendChild(healthSummaryEl);
      healthButtonEl.addEventListener('click', function () {
        openBoardHealthModal(healthIssues.slice());
      });
      healthEl.appendChild(healthButtonEl);
      healthAnnouncementEl = el('span', {
        class: 'u-visually-hidden',
        attrs: { role: 'status', 'aria-live': 'polite', 'aria-atomic': 'true' }
      });
      healthEl.appendChild(healthAnnouncementEl);
    }
    var label = t('board.status.label');
    var severityLabel = t('board.status.severity.' + severity);
    var summary = severity === 'ok'
      ? t('board.status.ok')
      : t('board.status.issueCount', { count: issues.length });
    healthEl.className = 'board-health board-health--' + severity;
    healthEl.setAttribute('aria-label', label);
    healthButtonEl.querySelector('.board-health__label').textContent = label;
    healthSeverityEl.textContent = severityLabel;
    // The healthy state is already fully expressed by the severity label.
    // Keep its equivalent summary in the accessible name, but do not render
    // the same message twice in the compact status control.
    healthSummaryEl.textContent = severity === 'ok' ? '' : summary;
    healthButtonEl.setAttribute('aria-label', label + '. ' + severityLabel + '. ' + summary);
    var announcement = severity + '\0' + issues.map(function (item) {
      return item.code + '\0' + item.severity + '\0' + (item.stem || '');
    }).join('\0');
    if (announcement !== healthAnnouncementSignature) {
      healthAnnouncementSignature = announcement;
      healthAnnouncementEl.textContent = label + '. ' + severityLabel + '. ' + summary;
    }
    if (healthEl.parentNode !== sectionEl) {
      var before = toolbarEl && toolbarEl.parentNode === sectionEl ? toolbarEl : sectionEl.firstChild;
      sectionEl.insertBefore(healthEl, before);
    }
  }

  function element() {
    return healthEl;
  }

  return {
    render: renderBoardStatus,
    element: element,
    issuesFor: boardHealthIssues,
    severityFor: boardHealthSeverity,
    copyTextFor: boardHealthCopyText
  };
}
