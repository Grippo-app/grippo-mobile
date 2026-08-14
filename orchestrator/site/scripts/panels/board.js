import { dom } from '../dom.js';
import { i18n } from '../i18n.js';
import { store } from '../store.js';
import { clipboard } from '../clipboard.js';
import { helpers } from '../data/wizard-steps.js';
import { tasksApi } from '../data/tasks-api.js';
import { router } from '../router.js';
import { runControl } from '../run-control.js';
import { figmaActionError } from './figma.js';
import { terminal } from '../terminal.js';
import { FIGMA_SESSION_ACTION, screensPrompt, figmaConnected } from '../figma-actions.js';
import { evidenceIssueLabel } from '../figma/evidence-issue-presenter.js';
import { createTaskListStore } from '../board/task-list-store.js';
import {
  mergeTaskSummaryPage
} from '../board/task-summary-projection.js';
import { createBoardFormatters } from '../board/board-formatters.js';
import { createBoardToolbar } from '../board/board-toolbar.js';
import { createBoardViewportState } from '../board/board-viewport-state.js';
import { createBoardTaskListView } from '../board/board-task-list-view.js';
import { createBoardTaskCardFactory } from '../board/board-task-card-factory.js';
import { createBoardPaginationController } from '../board/board-pagination-controller.js';
import { createBoardLoadResults } from '../board/board-load-results.js';
import { createBoardLoadController } from '../board/board-load-controller.js';
import { createBoardTaskTargetController } from '../board/board-task-target-controller.js';
import { createBoardTaskInbox } from '../board/board-task-inbox.js';
import { createBoardFinalizationController } from '../board/board-finalization-controller.js';
import { createBoardIntegrationController } from '../board/board-integration-controller.js';
import { createBoardTaskNavigationController } from '../board/board-task-navigation-controller.js';
import { createBoardConfirmDialog } from '../board/board-confirm-dialog.js';
import { createBoardOpenCardFreshness } from '../board/board-open-card-freshness.js';
import { createBoardRenderController } from '../board/board-render-controller.js';
import { createBoardRefreshClock } from '../board/board-refresh-clock.js';
import { createBoardTaskDetailsShell } from '../board/board-task-details-shell.js';
import { createBoardFigmaScreensController } from '../board/board-figma-screens-controller.js';
import { createBoardTaskActionController } from '../board/board-task-action-controller.js';
import { createEnqueueResultPresenter } from '../board/enqueue-result-presenter.js';
import { createBoardReadinessPolicy } from '../board/board-readiness-policy.js';
import { createFigmaTaskReadModel } from '../board/figma-task-read-model.js';
import { taskCard } from '../board/task-card.js';
import { createTaskDetails } from '../board/task-details.js';
import { createBacklogComposer } from '../board/backlog-composer.js';
import { createBoardModalController } from '../board/modal-controller.js';
import { createBoardWorkerSupport } from '../board/worker-support.js';
import { createBoardHealthController } from '../board/board-health.js';
import { createVisualEvidenceView } from '../board/visual-evidence-view.js';
import { createVisualEvidenceStatus } from '../board/visual-evidence-status.js';
import { createVisualEvidenceRecoveryView } from '../board/visual-evidence-recovery-view.js';
import { createVisualFixTask } from '../board/visual-fix-task.js';
import { createVisualFixActionsView } from '../board/visual-fix-actions-view.js';
import { createVisualEvidenceSummaryView } from '../board/visual-evidence-summary-view.js';
import { createFigmaScreensView } from '../board/figma-screens-view.js';
import { createPixelReviewView } from '../board/pixel-review-view.js';
import { confirmDialog, promptDialog } from '../ui-dialog.js';
import {
  artifactSetReportHashOk,
  finalVisualDisplayState,
  finalVisualTrustState
} from '../board/visual-evidence-trust.js';
import { requestJson, errorCode } from '../data/request-json.js';
import { appRunControl } from '../app-run-control.js';

  // ----------------------------------------------------------------------
  // Board panel — a kanban backed by the bounded /api/tasks/summary read model
  // and rendered as four columns (backlog / pending / todo / done). Card actions
  // are the typed, server-fenced /api/tasks/actions contract; the Details modal
  // reuses the same resolved action and retains only contextual answer/live-input flows.
  //
  // The browser renders the server's validated canonical model. INDEX.json is
  // only an ordering/metadata input reconciled with task files on the server, so
  // the client neither trusts stale locations nor re-derives task state itself.
  // ----------------------------------------------------------------------

  var el = dom.el;

  function t(key, params) {
    if (i18n && typeof i18n.t === 'function') {
      return i18n.t(key, params);
    }
    return key;
  }

  export function boardRequestError(error) {
    var code = errorCode(error);
    var key = 'board.requestError.' + code;
    var translated = t(key);
    if (translated !== key) return translated;
    var commonKey = 'common.requestError.' + code;
    var common = t(commonKey);
    return common === commonKey ? t('board.requestError.unknown') : common;
  }

  var boardFormatters = createBoardFormatters({
    t: t,
    getLanguage: function () {
      return (i18n && typeof i18n.get === 'function') ? i18n.get() : 'en';
    },
    now: function () { return Date.now(); }
  });

  var sectionEl = null;
  var taskListStore = createTaskListStore();
  var state = {
    columns: null,
    summary: null,
    // Fresh, read-only /api/tasks/integrity result. This deliberately lives
    // beside Task Summary instead of only in store: /api/state is short-TTL cached for
    // SSE performance, while mutation controls must follow the latest canonical
    // filesystem verdict after a repair or a rejected enqueue.
    integrity: null,
    error: null,
    loading: false,
    loadingMore: false,
    paginationError: null,
    activeModal: null,
    escHandler: null,
    // One-shot cancel hook for Board confirmation dialogs — invoked by boardModal.close on any
    // dismiss that wasn't the confirm action.
    onModalCancel: null,
    // The open CARD modal's identity ({ folder, stem }), set by openCardModal and
    // cleared on close. Lets a post-refresh check detect when the task left
    // its folder mid-session, and lets a card-modal close restore focus by stem
    // after an SSE column rebuild detached the originating card.
    openCard: null,
    openFinalizationStem: null,
    lastFocusStem: null,
    detailsAbort: null
  };
  var boardRenderController = null;
  var boardRefreshClock = null;
  var boardReadiness = createBoardReadinessPolicy({
    t: t,
    getSnapshot: function () { return store.get(); },
    getFreshIntegrity: function () { return state.integrity; }
  });
  var boardToolbar = createBoardToolbar({
    t: t,
    el: el,
    getLanguage: function () {
      return (i18n && typeof i18n.get === 'function') ? i18n.get() : 'en';
    },
    taskListStore: taskListStore,
    getSectionElement: function () { return sectionEl; },
    schedule: setTimeout,
    cancelSchedule: clearTimeout,
    refresh: function () { boardLoadController.load({ closeOpenModal: false }); }
  });
  var boardViewportState = createBoardViewportState({
    getDocumentNode: function () { return document; },
    getViewport: function () { return window; },
    getSectionElement: function () { return sectionEl; }
  });
  var boardModal = createBoardModalController({
    t: t,
    state: state,
    getSectionElement: function () { return sectionEl; }
  });
  var boardTaskDetailsShell = createBoardTaskDetailsShell({
    t: t,
    el: el,
    createCloseButton: boardModal.createCloseButton,
    getActiveModal: function () { return state.activeModal; },
    requestError: boardRequestError
  });
  var boardConfirmDialog = createBoardConfirmDialog({
    t: t,
    el: el,
    openModal: boardModal.open,
    closeModal: boardModal.close,
    setCancelHandler: function (handler) { state.onModalCancel = handler; },
    schedule: setTimeout
  });
  // Read-only per-task projections from the latest live store snapshot. The
  // server still owns task classification, capture admission, and evidence.
  var figmaTaskReadModel = createFigmaTaskReadModel({
    getSnapshot: function () { return store.get(); }
  });
  var boardFigmaScreensController = createBoardFigmaScreensController({
    t: t,
    el: el,
    needsUnpulledScreens: function (stem) {
      return figmaTaskReadModel.needsUnpulledScreens(stem);
    },
    isConnected: function () { return figmaConnected(); },
    sessionAction: function (key, action) {
      return tasksApi.figmaSessionAction(key, action);
    },
    screenPullAction: FIGMA_SESSION_ACTION.SCREEN_PULL,
    openTerminal: function (key) { terminal.open(key); },
    toast: function (message) { clipboard.toast(message); },
    actionError: figmaActionError,
    openModal: boardModal.open,
    closeModal: boardModal.close,
    setCancelHandler: function (handler) { state.onModalCancel = handler; },
    schedule: setTimeout
  });
  var boardTaskActionController = createBoardTaskActionController({
    t: t,
    startupRecoveryBlocksMutation: boardReadiness.startupRecoveryBlocksMutation,
    needsUnpulledScreens: function (stem) {
      return figmaTaskReadModel.needsUnpulledScreens(stem);
    },
    confirmScreensBeforeRun: boardFigmaScreensController.confirmBeforeRun,
    confirm: boardConfirmDialog.open,
    executeAction: function (stem, action, confirmation) {
      // Integrate is not a queued task action: it opens the preview so the
      // owner sees the exact diff and the exact blockers before authorizing
      // one canonical commit. The card rail and the details rail must agree.
      if (action && action.kind === 'integrate') {
        boardIntegrationController.open(stem);
        return Promise.resolve({ navigation: true });
      }
      return tasksApi.executeTaskAction(stem, action, confirmation);
    },
    toast: function (message) { clipboard.toast(message); },
    presentEnqueueResult: function (response, stem) {
      enqueueResultPresenter.present(response, stem, null);
    },
    requestError: boardRequestError,
    reloadStore: function () { return store.load(); },
    reloadBoard: function () {
      boardLoadController.load({ closeOpenModal: false });
    },
    copyText: function (text) { clipboard.copy(text); },
    openSource: function (target, row) {
      boardTaskNavigationController.openSource(target, row);
    },
    openEdit: function (stem, row) { openBacklogEditModal(stem, row); },
    runDrop: function (stem, folder, action, options) {
      runDropActionFlow(stem, folder, action, options);
    },
    runReopen: function (stem, action) { runReopenActionFlow(stem, action); },
    restoreOverflowFocus: function (stem) {
      var trigger = sectionEl && sectionEl.querySelector(
        '.board-card[data-stem="' + stem + '"] [data-task-control="overflow"]'
      );
      if (trigger) { try { trigger.focus(); } catch (error) {} }
    },
    loadPrompt: function (stem, action) {
      return tasksApi.loadTaskActionPrompt(stem, action);
    }
  });
  var boardTaskInbox = createBoardTaskInbox({
    t: t,
    el: el,
    createTextNode: function (text) { return document.createTextNode(text); },
    formatTimestamp: boardFormatters.timestampLabel,
    canPublish: function () { return boardRenderController.isComplete(store.get()); },
    loadEntries: function () { return tasksApi.loadTaskInbox(); },
    publishEntry: function (id) { return tasksApi.publishTaskInbox(id); },
    openComposer: function () { backlogComposer.open({ inbox: true }); },
    toast: function (message) { clipboard.toast(message); },
    requestError: boardRequestError,
    reloadStore: function () { store.load(); },
    getSectionElement: function () { return sectionEl; },
    isBoardActive: function () { return router.current() === 'board'; },
    rerender: function () { boardRenderController.render(); }
  });
  var backlogComposer = createBacklogComposer({
    t: t,
    clipboard: clipboard,
    store: store,
    tasksApi: tasksApi,
    globalMutationBlocked: boardReadiness.globalMutationBlocked,
    createBacklogWithIntegrityFence: createBacklogWithIntegrityFence,
    closeModal: boardModal.close,
    createCloseButton: boardModal.createCloseButton,
    loadTaskInbox: boardTaskInbox.load,
    handleTaskMutationConflict: handleTaskMutationConflict,
    boardRequestError: boardRequestError,
    openModal: boardModal.open
  });
  var boardTaskNavigationController = createBoardTaskNavigationController({
    getColumns: function () { return state.columns; },
    openCard: function (folder, stem, item, section) {
      openCardModal(folder, stem, item, section);
    },
    openTerminal: function (key) { terminal.open(key); },
    openPanel: function (target) { return router.openTarget(target); },
    targetUnavailable: function () {
      clipboard.toastError(t('board.action.targetUnavailable'));
    }
  });
  var boardTaskCardFactory = createBoardTaskCardFactory({
    renderCard: taskCard,
    t: t,
    formatRelative: boardFormatters.relativeTime,
    mutationsBlocked: boardReadiness.startupRecoveryBlocksMutation,
    menuState: taskListStore,
    getSectionElement: function () { return sectionEl; },
    openDetails: function (folder, row) { openCardModal(folder, row.stem, row); },
    execute: boardTaskActionController.run,
    navigate: boardTaskNavigationController.openTarget,
    action: boardTaskActionController.overflow
  });
  var boardPaginationController = createBoardPaginationController({
    getState: function () { return state; },
    getLoadGeneration: function () { return boardLoadController.generation(); },
    getFilters: function () { return taskListStore.filters(); },
    loadSummary: function (filters) { return tasksApi.loadTaskSummary(filters); },
    mergeSummaryPage: function (current, page) {
      return mergeTaskSummaryPage(current, page);
    },
    render: function () { boardRenderController.render(); },
    reload: function () { boardLoadController.load({ closeOpenModal: false }); }
  });
  var boardLoadResults = createBoardLoadResults({
    loadSummary: function (filters) { return tasksApi.loadTaskSummary(filters); },
    loadIntegrity: function () { return tasksApi.loadTaskIntegrity(); }
  });
  var boardTaskTargetController = createBoardTaskTargetController({
    getColumns: function () { return state.columns; },
    getCurrentPanel: function () { return router.current(); },
    getHash: function () { return location.hash; },
    replaceHash: function (hash) { history.replaceState(null, '', hash); },
    openTask: function (folder, stem, item, section, target) {
      openCardModal(folder, stem, item, section, target);
    }
  });
  var boardOpenCardFreshness = createBoardOpenCardFreshness({
    t: t,
    el: el,
    getOpenCard: function () { return state.openCard; },
    getColumns: function () { return state.columns; },
    getActiveModal: function () { return state.activeModal; },
    abortDetails: function () {
      if (!state.detailsAbort) return;
      try { state.detailsAbort.abort(); } catch (abortError) {}
      state.detailsAbort = null;
    },
    openDetails: function (stem, section, item) {
      openTaskDetails(stem, section, item);
    },
    setOpenCard: function (openCard) { state.openCard = openCard; }
  });
  var boardLoadController = createBoardLoadController({
    getState: function () { return state; },
    isMounted: function () { return !!sectionEl; },
    closeModal: function () { boardModal.close(); },
    getFilters: function () { return taskListStore.filters(); },
    loadResults: boardLoadResults.load,
    render: function () { boardRenderController.render(); },
    reconcileMenus: function (stems) { taskListStore.reconcileMenus(stems); },
    requestError: boardRequestError,
    afterLoad: function () {
      boardOpenCardFreshness.neutralizeIfMoved();
      boardTaskTargetController.openRequested(true);
    }
  });
  var boardTaskListView = createBoardTaskListView({
    t: t,
    el: el,
    getSectionElement: function () { return sectionEl; },
    getState: function () { return state; },
    createCard: boardTaskCardFactory.create,
    globalMutationBlocked: boardReadiness.globalMutationBlocked,
    openBacklogComposer: function () { backlogComposer.open({ inbox: false }); },
    loadMoreTasks: boardPaginationController.loadMore
  });

  // ----------------------------------------------------------------------
  // Renderers.
  // ----------------------------------------------------------------------

  function boardLoadErrorText(err) {
    var msg;
    if (err.kind === 'fetch-failed') {
      // The most common cause is file:// — special-case that wording.
      if (location.protocol === 'file:') {
        msg = t('board.error.fileProtocol');
      } else {
        msg = t('board.error.fetchFailed');
      }
    } else if (err.kind === 'not-found') {
      msg = t('board.error.fileMissing');
    } else {
      msg = boardRequestError(err);
    }
    return msg;
  }

  // Every task-creation affordance (the main composer and the contextual
  // Figma helpers) converges here.  A modal may stay open while SSE turns the
  // canonical snapshot red, so checking only when the button was rendered is
  // insufficient: the final call boundary must fail closed as well.
  function createBacklogWithIntegrityFence(title, body, options) {
    if (boardReadiness.globalMutationBlocked()) {
      return Promise.reject({
        kind: 'task-integrity',
        status: 409,
        detail: t('board.integrity.createBlocked'),
        integrity: boardReadiness.taskIntegrity()
      });
    }
    return tasksApi.createBacklog(title, body, options);
  }

  // Fresh-lock window used by worker support while deciding whether queued
  // work still has a plausible active owner. This is presentation-only and is
  // never authority to release or replace a lock.
  var STALE_LOCK_MS = 20 * 60 * 1000;
  var workerSupport = createBoardWorkerSupport({
    t: t,
    pluralLabel: boardFormatters.pluralLabel,
    parseIso: boardFormatters.parseIso,
    clampNow: boardFormatters.clampNow,
    staleLockMs: STALE_LOCK_MS,
    copyButton: copyButton,
    boardModal: boardModal,
    store: store
  });
  var enqueueResultPresenter = createEnqueueResultPresenter({
    t: t,
    getSnapshot: function () { return store.get(); },
    workerOnlineOrBusy: workerSupport.workerOnlineOrBusy,
    cliCannotAuth: workerSupport.cliCannotAuth,
    toast: function (message) { clipboard.toast(message); }
  });
  var boardFinalizationController = createBoardFinalizationController({
    t: t,
    el: el,
    getSnapshot: function () { return store.get(); },
    hasActiveModal: function () { return !!state.activeModal; },
    getOpenStem: function () { return state.openFinalizationStem; },
    setOpenStem: function (stem) { state.openFinalizationStem = stem; },
    modal: boardModal,
    resume: function (finalization) { return tasksApi.resumeFinalization(finalization); },
    toast: function (message) { clipboard.toast(message); },
    requestError: boardRequestError,
    reloadStore: function () { store.load(); }
  });
  var boardIntegrationController = createBoardIntegrationController({
    t: t,
    el: el,
    getSnapshot: function () { return store.get(); },
    hasActiveModal: function () { return !!state.activeModal; },
    getOpenStem: function () { return state.openIntegrationStem; },
    setOpenStem: function (stem) { state.openIntegrationStem = stem; },
    modal: boardModal,
    preview: function (stem) { return tasksApi.previewIntegration(stem); },
    run: function (stem, resuming) {
      return resuming ? tasksApi.resumeIntegration(stem) : tasksApi.startIntegration(stem);
    },
    abandon: function (stem, integrationId) { return tasksApi.abandonIntegration(stem, integrationId); },
    release: function (stem) { return tasksApi.releaseWorktree(stem); },
    toast: function (message) { clipboard.toast(message); },
    requestError: boardRequestError,
    reloadStore: function () { store.load(); }
  });
  var boardHealth = createBoardHealthController({
    t: t,
    clipboard: clipboard,
    boardModal: boardModal,
    state: state,
    taskIntegrity: boardReadiness.taskIntegrity,
    startupRecoveryState: boardReadiness.startupRecoveryState,
    startupRecoveryReason: boardReadiness.startupRecoveryReason,
    boardLoadErrorText: boardLoadErrorText,
    finalizations: boardFinalizationController.list,
    openFinalizationModal: boardFinalizationController.open,
    workerSupport: workerSupport,
    getSectionElement: function () { return sectionEl; },
    getToolbarElement: boardToolbar.element
  });
  boardRenderController = createBoardRenderController({
    t: t,
    el: el,
    createTextNode: function (text) { return document.createTextNode(text); },
    getSectionElement: function () { return sectionEl; },
    getState: function () { return state; },
    getSnapshot: function () { return store.get(); },
    wizardComplete: function (storeState) {
      if (helpers && typeof helpers.wizardComplete === 'function') {
        return helpers.wizardComplete(storeState);
      }
      return false;
    },
    viewport: boardViewportState,
    toolbar: boardToolbar,
    health: boardHealth,
    inbox: boardTaskInbox,
    taskList: boardTaskListView
  });
  boardRefreshClock = createBoardRefreshClock({
    scheduleTimeout: function (callback, delay) { return window.setTimeout(callback, delay); },
    cancelTimeout: function (timer) { window.clearTimeout(timer); },
    scheduleInterval: function (callback, delay) { return window.setInterval(callback, delay); },
    isMounted: function () { return !!sectionEl; },
    isBoardActive: function () {
      return !(router && typeof router.current === 'function') || router.current() === 'board';
    },
    getSnapshot: function () { return store.get(); },
    pendingRequests: workerSupport.pendingRequests,
    reloadBoard: function (options) { boardLoadController.load(options); },
    render: boardRenderController.render
  });

  var visualEvidenceStatus = createVisualEvidenceStatus({ t: t });
  var visualEvidenceView = createVisualEvidenceView({
    el: el,
    t: t,
    pluralTemplate: boardFormatters.pluralTemplate,
    evidenceStatusClass: visualEvidenceStatus.className,
    evidenceStatusLabel: visualEvidenceStatus.label
  });
  var visualFixTask = createVisualFixTask({
    artifactHref: visualEvidenceView.artifactHref,
    visualTitle: visualEvidenceView.visualTitle
  });
  var visualFixActionsView = createVisualFixActionsView({
    el: el,
    t: t
  });
  var visualEvidenceRecoveryView = createVisualEvidenceRecoveryView({
    el: el,
    t: t,
    evidenceIssueLabel: evidenceIssueLabel,
    buildEvidenceBadge: visualEvidenceView.buildEvidenceBadge,
    copyButton: copyButton,
    onRebundle: spawnRebundleSession,
    onRebundleError: function (error) {
      clipboard.toastError(t('board.figmaEvidence.rebundle.failed', { detail: figmaActionError(error) }));
    }
  });
  var figmaScreensView = createFigmaScreensView({
    el: el,
    t: t,
    relativeTime: boardFormatters.relativeTime,
    createTextNode: function (value) { return document.createTextNode(value); }
  });
  var pixelReviewView = createPixelReviewView({
    el: el,
    t: t,
    prepareVerdict: function (verdict) {
      // "Matches" also writes a durable one-shot receipt, so it is confirmed
      // rather than fired on a single click; the other two collect their note in
      // the same themed dialog instead of an OS prompt.
      if (verdict === 'pass') {
        return confirmDialog({
          title: t('board.figmaEvidence.review.confirmPassTitle'),
          message: t('board.figmaEvidence.review.confirmPassBody'),
          confirmLabel: t('board.figmaEvidence.review.pass')
        }).then(function (accepted) { return accepted ? { note: '' } : null; });
      }
      return promptDialog({
        title: t('board.figmaEvidence.review.notePromptTitle'),
        message: t('board.figmaEvidence.review.notePrompt'),
        fieldLabel: t('board.figmaEvidence.review.notePromptTitle'),
        confirmLabel: t('board.figmaEvidence.review.submitVerdict')
      }).then(function (typed) {
        // null is a dismissal — collapsing it into '' recorded a verdict the
        // user never confirmed.
        if (typed === null) return null;
        if (verdict === 'fail' && !typed.trim()) {
          clipboard.toastError(t('board.figmaEvidence.review.noteRequired'));
          return null;
        }
        return { note: typed };
      });
    },
    submitVerdict: function (stem, row, verdict, note) {
      var headers = { 'Content-Type': 'application/json', Accept: 'application/json' };
      if (typeof window !== 'undefined' && window.__ORCHESTRATOR_CSRF__) {
        headers['x-orchestrator-csrf'] = window.__ORCHESTRATOR_CSRF__;
      }
      return requestJson('/api/figma/pixel-review', {
        method: 'POST',
        headers: headers,
        body: JSON.stringify({
          stem: stem,
          screen: row.screen,
          theme: row.theme || 'primary',
          verdict: verdict,
          note: note
        })
      }).then(function () { return spawnRebundleSession(stem); });
    },
    onSubmitError: function (error) {
      clipboard.toastError(t('board.figmaEvidence.review.failed', { detail: boardRequestError(error) }));
    }
  });
  var visualEvidenceSummaryView = createVisualEvidenceSummaryView({
    el: el,
    t: t,
    finalVisualDisplayState: finalVisualDisplayState,
    evidenceStatusClass: visualEvidenceStatus.className,
    buildEvidenceVisuals: visualEvidenceView.buildEvidenceVisuals,
    buildEvidenceBadge: visualEvidenceView.buildEvidenceBadge,
    buildCause: visualEvidenceRecoveryView.buildCause,
    buildReviewPanel: pixelReviewView.buildPanel,
    buildRerun: visualEvidenceRecoveryView.buildRerun
  });

  // ----------------------------------------------------------------------
  // Shared task target and action routing for cards and unified Details.
  // ----------------------------------------------------------------------

  // Small copy helper used by diagnostic and worker-assistance surfaces.
  function copyButton(label, getText, opts) {
    var cls = 'btn' + (opts && opts.primary ? ' btn--primary' : '');
    var btn = el('button', { type: 'button', class: cls, text: label });
    clipboard.attach(btn, getText);
    return btn;
  }

  // Canonical admission conflicts are recovery signals, not generic queue
  // failures. Close the now-stale modal, surface a precise explanation, and
  // refresh BOTH the server snapshot and the independently fresh integrity
  // verdict. `true` tells run-control that the error already has user-facing UX.
  // `options.closeModal === false` keeps an authoring form open: force-closing a
  // stale read-only card is right, but doing it to the composer discards every
  // field the user just typed, with no draft recovery anywhere.
  function handleTaskMutationConflict(err, options) {
    if (!err || ['stale-task-state', 'task-integrity', 'task-action-active'].indexOf(err.kind) < 0) return false;

    if (err.kind === 'task-integrity' && err.integrity && typeof err.integrity === 'object') {
      state.integrity = err.integrity;
    }
    if (err.kind === 'stale-task-state') {
      clipboard.toastError(t('board.conflict.stale'));
    } else if (err.kind === 'task-integrity') {
      clipboard.toastError(t('board.conflict.integrity'));
    } else {
      var active = err.active || {};
      clipboard.toastError(t('board.conflict.active', {
        action: active.action || t('board.conflict.unknownAction'),
        phase: active.phase || t('board.conflict.unknownPhase')
      }));
    }

    // A confirm modal may carry an onCancel hook that would reopen the stale
    // card. Suppress it for this programmatic conflict recovery.
    if (!options || options.closeModal !== false) boardModal.forceClose();
    boardRenderController.render();
    boardLoadController.load({ closeOpenModal: false });
    store.load();
    return true;
  }

  // Contextual answer submission keeps its validated form payload in the
  // browser, then flips to ⊡ Terminal for the live task session. Prepare, Run,
  // and Retry are deliberately absent here: their prompts and admission are
  // owned by the typed server-resolved task action endpoint.
  // opts (all optional):
  //   runLabel        — override the Run-state label.
  //   terminalLabel   — override the live-session re-entry label.
  //   isActive        — override the active check for a non-task auxiliary flow.
  //   isFinished      — override when an ended session should read as a re-run.
  //   confirm         — async pre-run gate (() => Promise<bool>) forwarded to
  //                     run-control; the Board's "screens not pulled" warning.
  //   onStarted       — accepted+drainable request handler. Receives
  //                     (response, openTerminal, { previousSessionStartedAt })
  //                     and may present a workflow-specific handoff.
  // "Pull Figma screens" — spawns the per-stem figma:screens session that caches this
  // task's per-screen design context + value specs + screenshots under
  // orchestrator/.cache/figma/screens/<stem>/ and runs the component census
  // (the implement-figma skill / orchestrator/figma/). The session reads the
  // task file itself, so the prompt needs only the stem.
  //
  // Shown ONLY for screen/dialog tasks — those whose body carries a pullable
  // `## Design` bullet — and only when the Figma MCP is bound. A non-UI task (no
  // ## Design) never gets the button. The body loads async, so we return a
  // `display:contents` slot and fill it once known. Callers pass only
  // 'backlog'/'todo' — the pending column deliberately has no standalone Pull
  // button (screens are pulled at backlog; the answers-Run confirm gate still
  // offers a pull).
  function figmaScreensButton(folder, stem) {
    var fig = store.get().figma || {};
    // The fixable needs-auth / local-absent states render the same slot with a
    // DISABLED button plus a short gate line linking to #figma (mirroring
    // figma-actions.js gateLine — the fix lives on that tab), still only for
    // tasks with a pullable ## Design bullet. Every other state (cli-missing,
    // unknown) keeps returning null — there is no one-click fix to point at.
    var figState = fig.state;
    var conflict = !!(fig.global && fig.global.present);
    if (figState !== 'connected' && figState !== 'needs-auth' && figState !== 'local-absent') return null;
    var gated = !figmaConnected();
    var slot = el('span', { class: 'board-figma-slot' });
    function fill(ok, count) {
      if (!ok || slot.firstChild) return;
      var oc = state.openCard;
      if (!oc || oc.folder !== folder || oc.stem !== stem || !state.activeModal || !document.contains(slot)) return;
      // Do not offer an action the server has already proven it must reject.
      // The modal's Design note names the exact line and issue, while the Edit
      // action remains available immediately beside this (now-empty) slot.
      if (figmaTaskReadModel.blockingDesignIssues(stem)) return;
      // B4: name the batch. >1 pullable bullet → "⤓ Pull N Figma screens" (all in one pass); a
      // single bullet keeps the plain label. Defaults to the plain label until the count resolves.
      var multi = typeof count === 'number' && count > 1;
      var pullLabel = multi ? t('board.figmaScreens.btnN', { n: count }) : t('board.figmaScreens.btn');
      if (gated) {
        var gate = el('span', { class: 'board-figma-gate' });
        var gbtn = el('button', { type: 'button', class: 'btn', text: pullLabel });
        gbtn.disabled = true;
        gate.appendChild(gbtn);
        var line = el('span', { class: 'board-figma-gate__line' });
        var gateKey = conflict
          ? 'board.figmaScreens.gateConflict'
          : (figState === 'needs-auth'
              ? 'board.figmaScreens.gateAuth'
              : (figState === 'local-absent'
                  ? 'board.figmaScreens.gateAbsent'
                  : 'board.figmaScreens.gateAccount'));
        line.appendChild(document.createTextNode(t(gateKey) + ' '));
        var link = el('a', { href: '#figma', text: t('board.figmaScreens.gateLink') });
        // The link navigates the panel UNDER the modal — close the modal so the
        // Figma tab is actually visible when the hash lands.
        link.addEventListener('click', function () { boardModal.close(); });
        line.appendChild(link);
        gate.appendChild(line);
        slot.appendChild(gate);
        return;
      }
      slot.appendChild(runControl.button({
        key: boardFigmaScreensController.sessionKey(stem),
        getPrompt: function () {
          if (!boardFigmaScreensController.isPullReady()) {
            clipboard.toastError(t('board.screensWarn.pullUnavailable'));
            return null;
          }
          return screensPrompt(stem);
        },
        onRun: function (key) { return tasksApi.figmaSessionAction(key, FIGMA_SESSION_ACTION.SCREEN_PULL); },
        onError: function (error) {
          clipboard.toastError(figmaActionError(error));
          return true;
        },
        isDisabled: function () { return !boardFigmaScreensController.isPullReady(); },
        labels: { run: pullLabel, rerun: pullLabel },
        // Green "already pulled, re-pullable" state from the per-task screens-cache
        // signal (/api/state.screensCache[stem]); run-control's refreshAll flips it
        // live when this task's figma:screens pull lands. null → default ⤓ Pull.
        status: function () {
          // The screensCache signal carries unpulled UI tasks
          // (needed:true, pulled:false) so the card chip / Run-confirm can warn.
          // The green "already pulled, re-pullable" state must stay scoped to a
          // REAL pull, so gate on `pulled` (not mere presence).
          var c = (store.get().screensCache || {})[stem];
          // Partial/stale cache diagnosis on the button itself: the slot's title
          // says WHY re-pulling is needed. Kept in this status() callback (re-run
          // on every store change via refreshAll) so it tracks the live cache;
          // display-only, every read null-guarded. run-control's button API has
          // no title hook, so the tooltip rides the wrapping slot instead.
          if (!boardFigmaScreensController.isPullReady()) {
            slot.setAttribute('title', t('board.screensWarn.pullUnavailable'));
          } else if (c && !c.pulled && c.status === 'incomplete') {
            slot.setAttribute('title', t('board.screensWarn.titleIncomplete', { n: typeof c.count === 'number' ? c.count : 0 }));
          } else if (c && !c.pulled && c.status === 'stale') {
            slot.setAttribute('title', t('board.screensWarn.titleStale'));
          } else if (multi) {
            slot.setAttribute('title', t('board.figmaScreens.pullAll', { n: count }));   // B4: pulls all N in one pass
          } else {
            slot.removeAttribute('title');
          }
          if (!c || !c.pulled) return null;
          return { done: true, label: t('board.figmaScreens.repull', { ago: c.mtime ? ' ' + boardFormatters.relativeTime(new Date(c.mtime).toISOString()) : '' }) };
        }
      }));
    }
    tasksApi.loadTaskFile(folder, stem).then(function (body) {
      if (visualFixTask.hasPullableDesign(body)) fill(true, visualFixTask.pullableBulletCount(body));
    }, function () {});
    return slot;
  }

  function sameDropImpact(a, b) {
    if (!a || !b || a.stem !== b.stem || a.state !== b.state ||
        a.sourceRevision !== b.sourceRevision || a.impactHash !== b.impactHash) return false;
    var left = Array.isArray(a.dependents) ? a.dependents.slice().sort() : [];
    var right = Array.isArray(b.dependents) ? b.dependents.slice().sort() : [];
    return JSON.stringify(left) === JSON.stringify(right);
  }

  function secondaryTaskAction(item, kind) {
    return (item && Array.isArray(item.secondaryActions) ? item.secondaryActions : []).find(function (action) {
      return action && action.kind === kind;
    }) || null;
  }

  function dropConfirmationMessage(bodyKey, stem, impact) {
    var dependents = Array.isArray(impact && impact.dependents) ? impact.dependents : [];
    var impactText = dependents.length
      ? t('board.drop.impactPresent', { dependents: dependents.map(function (dep) { return '• ' + dep; }).join('\n') })
      : t('board.drop.impactNone');
    return t(bodyKey, { stem: stem }) + '\n\n' + impactText;
  }

  // Shared two-phase Drop flow used by both Details and the card overflow.
  // It fetches and displays canonical impact, then re-fetches it after explicit
  // confirmation and executes only when the revision, hash, and dependents are
  // unchanged. The server fences the same receipt immediately before mutation.
  function runDropActionFlow(stem, folder, typedAction, options) {
    options = options || {};
    var bodyKey = folder === 'backlog' ? 'board.drop.bodyBacklog'
                : folder === 'pending' ? 'board.drop.bodyPending'
                : folder === 'done' ? 'board.drop.bodyDone'
                : folder === 'corrupt' ? 'board.drop.bodyCorrupt'
                : 'board.drop.bodyTodo';
    if (typeof options.onInspecting === 'function') options.onInspecting();
    tasksApi.loadDropImpact(stem).then(function (shownImpact) {
      if (typeof options.onInspected === 'function') options.onInspected();
      boardConfirmDialog.open({
        title: t('board.drop.title'),
        message: dropConfirmationMessage(bodyKey, stem, shownImpact),
        danger: true,
        confirmLabel: t('board.drop.confirm'),
        onCancel: options.onCancel,
        onConfirm: function () {
          tasksApi.loadDropImpact(stem).then(function (freshImpact) {
            if (!sameDropImpact(shownImpact, freshImpact)) {
              clipboard.toast(t('board.drop.impactChanged'));
              boardLoadController.load({ closeOpenModal: false });
              store.load();
              return;
            }
            var snap = store.get();
            var queued = (snap && snap.progress && Array.isArray(snap.progress.requests))
              ? snap.progress.requests : [];
            for (var d = 0; d < queued.length; d++) {
              if (queued[d] && queued[d].stem === stem && queued[d].action === 'drop') {
                // Confirming a drop that is already queued used to close the
                // dialog with no word at all, which reads as "nothing happened".
                clipboard.toast(t('board.run.queuedExists'));
                boardModal.close();
                return;
              }
            }
            var confirmation = {
              sourceRevision: freshImpact.sourceRevision,
              impactHash: freshImpact.impactHash,
              dependents: (freshImpact.dependents || []).slice().sort()
            };
            tasksApi.executeTaskAction(stem, typedAction, confirmation).then(function (resp) {
              enqueueResultPresenter.present(resp, stem, function () { boardModal.close(); });
            }, function (err) {
              if (handleTaskMutationConflict(err)) return;
              clipboard.toastError(t('board.drop.failed', { detail: boardRequestError(err) }));
            });
          }, function (err) {
            if (handleTaskMutationConflict(err)) return;
            clipboard.toastError(t('board.drop.inspectFailed', { detail: boardRequestError(err) }));
          });
        }
      });
    }, function (err) {
      if (typeof options.onInspected === 'function') options.onInspected();
      if (handleTaskMutationConflict(err)) return;
      clipboard.toastError(t('board.drop.inspectFailed', { detail: boardRequestError(err) }));
    });
  }

  function runReopenActionFlow(stem, typedAction, onCancel) {
    boardConfirmDialog.open({
      title: t('board.reopen.title'),
      message: t('board.reopen.body', { stem: stem }),
      confirmLabel: t('board.reopen.confirm'),
      onCancel: onCancel,
      onConfirm: function () {
        tasksApi.executeTaskAction(stem, typedAction, true).then(function (resp) {
          enqueueResultPresenter.present(resp, stem, function () { boardModal.close(); });
        }, function (err) {
          if (handleTaskMutationConflict(err)) return;
          clipboard.toastError(t('board.reopen.failed', { detail: boardRequestError(err) }));
        });
      }
    });
  }

  // ----------------------------------------------------------------------
  // Pulled-Figma viewer. When this task's figma:screens cache is present
  // (screensCache[stem].pulled), the task-detail modal grows a "Screens"
  // section showing each pulled screen: its screenshot (links to the Figma
  // node), name, an "Open in Figma" link, when it was pulled, a compact spec
  // line, and the component census tally. Read-only — fetched once from
  // GET /api/figma/screens?stem=<stem> and guarded by the modal token so a
  // stale fetch can't mutate a closed/reopened modal's DOM.
  // ----------------------------------------------------------------------

  // GET JSON from an /api/* endpoint, mirroring tasksApi.gitStatus / sessionEvents
  // (no-store fetch → r.json(), structured reject on failure). Kept inline here
  // because the screens viewer is the only board-panel consumer of this endpoint.
  function fetchJson(url) {
    return requestJson(url, { cache: 'no-store' });
  }

  function isProblemVisual(row) {
    return row && visualEvidenceStatus.className(row.status) !== 'pass';
  }

  function loadParentTaskMarkdown(folder, stem) {
    var sourceFolder = folder === 'pending' ? 'backlog' : folder;
    return tasksApi.loadTaskFile(sourceFolder || 'backlog', stem);
  }

  function visualFixServerState(key) {
    // Creation is synchronous and has no queue/session state. Local fixStates
    // supplies immediate feedback; the durable domain key prevents duplicates.
    return null;
  }

  function buildVisualFixActions(row, resp, stem, folder, item, fixStates, rerender, stillActive) {
    if (!isProblemVisual(row)) return null;
    if (boardReadiness.globalMutationBlocked()) {
      return visualFixActionsView.buildBlocked('global-recovery');
    }
    var trust = finalVisualTrustState(resp);
    if (!trust.usable) return visualFixActionsView.buildBlocked(trust.reason);
    var reportHash = resp && resp.visualChecks && resp.visualChecks.reportHash;
    // A problem row with no artifact set (MISSING_CAPTURE / ASPECT_MISMATCH bailed before
    // producing images) gets the same disabled-button + reason shape as the trust path.
    if (!row.artifactSet || !artifactSetReportHashOk(row.artifactSet, reportHash)) {
      return visualFixActionsView.buildBlocked('no-artifacts');
    }
    var key = visualFixTask.key(stem, row);
    var stateForKey = fixStates[key] || visualFixServerState(key) || 'idle';
    return visualFixActionsView.buildActions(stateForKey, function () {
      fixStates[key] = 'loading';
      rerender();
      loadParentTaskMarkdown(folder, stem).then(function (markdown) {
        if (typeof stillActive === 'function' && !stillActive()) return null;
        var built = visualFixTask.buildTaskBody(markdown, resp, row, stem);
        if (!built.designBullets.length || !visualFixTask.hasFigmaDesignBullet(built.designBullets)) {
          fixStates[key] = 'failed';
          clipboard.toastError(t('board.figmaEvidence.fix.noDesign'));
          rerender();
          return null;
        }
        var title = t('board.figmaEvidence.fix.title', { screen: visualFixTask.inlineTitle(row, 80) });
        return createBacklogWithIntegrityFence(title, built.body, {
          idempotencyKey: tasksApi.creationKey('figma-visual-fix', key + ':' + reportHash),
          originStem: stem, dedupKey: key, dedupReport: reportHash
        }).then(function () {
          if (typeof stillActive === 'function' && !stillActive()) return;
          fixStates[key] = 'queued';
          clipboard.toast(t('board.figmaEvidence.fix.queuedToast'));
          rerender();
        });
      }, function (error) {
        if (typeof stillActive === 'function' && !stillActive()) return;
        fixStates[key] = 'failed';
        clipboard.toastError(t('board.figmaEvidence.fix.loadFailed', { detail: boardRequestError(error) }));
        rerender();
      }).catch(function (err) {
        if (typeof stillActive === 'function' && !stillActive()) return;
        fixStates[key] = 'failed';
        clipboard.toastError(t('board.figmaEvidence.fix.failedToast', { detail: boardRequestError(err) }));
        rerender();
      });
    });
  }

  // Start a canonical LOCAL-only rebundle action — shared by the rebundle button and
  // the pixel-review verdict buttons (after a verdict, the final bundle must re-run so the
  // receipt is applied and the row state updates end-to-end).
  function spawnRebundleSession(stem) {
    var key = 'figma:rebundle:' + stem;
    var p = tasksApi.figmaSessionAction(key, FIGMA_SESSION_ACTION.REBUNDLE);
    return Promise.resolve(p).then(function (r) {
      if (r && r.sent === false) clipboard.toast(t('run.busy'));
      terminal.open(key);
    });
  }

  function buildEvidenceSection(stem, getModalToken, folder, item) {
    var sec = el('div', { class: 'board-evidence', attrs: { tabindex: '0', 'data-board-cleanup': 'evidence' } });
    var bodyEl = el('div', { class: 'board-evidence__body' });
    bodyEl.appendChild(el('p', { class: 'panel-lead', text: t('board.figmaEvidence.loading') }));
    sec.appendChild(bodyEl);
    var loadSeq = 0;
    var lastLiteKey = visualEvidenceStatus.liteKey(figmaTaskReadModel.evidence(stem));
    var off = null;
    var currentResp = null;
    var fixStates = Object.create(null);
    var filterValue = '';
    function isActive() {
      return state.activeModal === getModalToken();
    }
    function render(resp) {
      var oldFilter = bodyEl.querySelector('.board-evidence__filter');
      var restoreFilter = oldFilter && document.activeElement === oldFilter ? {
        start: oldFilter.selectionStart, end: oldFilter.selectionEnd
      } : null;
      if (oldFilter) filterValue = oldFilter.value;
      currentResp = resp;
      visualEvidenceSummaryView.render(bodyEl, resp, {
        stem: stem,
        filterValue: filterValue,
        onFilterInput: function (value) { filterValue = value; },
        buildVisualActions: function (row, evidenceResp) {
          return buildVisualFixActions(row, evidenceResp, stem, folder, item, fixStates, function () {
            render(currentResp);
          }, function () { return sec.isConnected && isActive(); });
        }
      });
      if (restoreFilter) {
        var replacement = bodyEl.querySelector('.board-evidence__filter');
        if (replacement) {
          replacement.focus();
          if (typeof replacement.setSelectionRange === 'function' &&
              restoreFilter.start !== null && restoreFilter.end !== null) {
            replacement.setSelectionRange(restoreFilter.start, restoreFilter.end);
          }
        }
      }
    }
    function loadEvidence() {
      var seq = ++loadSeq;
      tasksApi.loadFigmaEvidence(stem).then(function (resp) {
        if (seq !== loadSeq || !isActive()) return;
        render(resp);
      }, function (error) {
        if (seq !== loadSeq || !isActive()) return;
        while (bodyEl.firstChild) bodyEl.removeChild(bodyEl.firstChild);
        bodyEl.appendChild(el('p', {
          class: 'board-evidence__empty',
          text: t('board.figmaEvidence.error', { detail: boardRequestError(error) })
        }));
      });
    }
    sec.__boardCleanup = function () {
      loadSeq++;
      if (off) off();
      off = null;
    };
    off = store.on('change', function () {
      if (!isActive()) { sec.__boardCleanup(); return; }
      var nextKey = visualEvidenceStatus.liteKey(figmaTaskReadModel.evidence(stem));
      if (nextKey === lastLiteKey) return;
      lastLiteKey = nextKey;
      loadEvidence();
    });
    loadEvidence();
    return sec;
  }

  // The "Compare" tab places the visual comparison (the Figma↔app three-up + verdict)
  // above the pulled Figma designs gallery. Design and its comparison live together, so the
  // reviewer never hops between tabs to answer "what is being compared with what".
  // Task-detail IA redesign: pre-`done` columns (backlog/pending/todo) show ONLY the pulled
  // Figma screens — never the evidence/verdict/comparison machinery, which is premature before
  // the task has run. Gated purely on `pulled` (screens to show); no evidence section, no
  // "comparison not run" blocker. Returns one "Figma" tab, or null when nothing is pulled.
  function buildFigmaScreensTab(stem, getModalToken) {
    var c = figmaTaskReadModel.entry(stem);
    if (!c || !c.pulled) return null;
    var pane = el('div', { class: 'board-compare' });
    var designs = el('div', { class: 'board-compare__designs' });
    designs.appendChild(el('h4', { class: 'board-evidence__subhead', text: t('board.screens.heading') }));
    designs.appendChild(buildScreensSection(stem, getModalToken));
    pane.appendChild(designs);
    return [{ id: 'figma', labelKey: 'board.tab.figma', pane: pane }];
  }

  // Task-detail IA redesign: `done` column shows the VISUAL per-component comparison — each
  // built screen/component as Figma-ref | diff/overlay | app-render with the similarity % as a
  // first-class number. Same presence gate as before (`pulled || evidence`) so a non-UI done task
  // gets no Figma tab; the per-screen list shows for ANY final comparison that ran (grouped
  // problems-first, with a non-green problem header when not READY); an "unavailable" note shows
  // only when none ran. (The summary view is unconditionally visuals-only now — no flag.)
  function buildFigmaCompareTab(stem, getModalToken, folder, item) {
    var c = figmaTaskReadModel.entry(stem);
    if (!c || (!c.pulled && !c.evidence)) return null;
    var pane = el('div', { class: 'board-compare' });
    pane.appendChild(buildEvidenceSection(stem, getModalToken, folder, item));
    return [{ id: 'figma', labelKey: 'board.tab.figma', pane: pane }];
  }

  // Build the "Screens" section node + kick off its fetch. Returns the section
  // element; the async render is guarded by modalToken (captured by the caller
  // after boardModal.open) so a stale resolve bails on a closed/reopened modal.
  function buildScreensSection(stem, getModalToken) {
    var mounted = figmaScreensView.buildSection();
    var sec = mounted.section;
    var bodyEl = mounted.body;

    fetchJson('/api/figma/screens?stem=' + encodeURIComponent(stem)).then(function (resp) {
      if (state.activeModal !== getModalToken()) return;   // modal closed/reopened — bail
      figmaScreensView.renderResponse(bodyEl, stem, resp);
    }, function (error) {
      if (state.activeModal !== getModalToken()) return;
      figmaScreensView.renderError(bodyEl, boardRequestError(error));
    });
    return sec;
  }

  // ----------------------------------------------------------------------
  // Card-specific modals.
  // ----------------------------------------------------------------------

  function openBacklogEditModal(stem, item) {
    var editFence = secondaryTaskAction(item, 'drop');
    if (!editFence || editFence.enabled === false || boardReadiness.integrityBlocksStem(stem)) {
      clipboard.toastError(t('board.integrity.actionBlocked'));
      return;
    }
    var content = el('div', { class: 'board-modal__body' });
    content.appendChild(el('h3', { class: 'board-modal__title', text: t('board.intake.editTitle') }));
    content.appendChild(el('code', { class: 'board-modal__stem', text: stem + '.md' }));
    var message = el('p', { class: 'board-intake__note', text: t('board.intake.editLoading') });
    var textarea = el('textarea', { class: 'input board-modal__textarea board-intake__editor', attrs: { rows: '22', 'aria-label': t('board.intake.editTitle') } });
    textarea.disabled = true;
    content.appendChild(message); content.appendChild(textarea);
    var expectedHash = null;
    var actions = el('div', { class: 'board-modal__actions' });
    var save = el('button', { type: 'button', class: 'btn btn--primary', text: t('board.intake.save') });
    save.disabled = true;
    save.addEventListener('click', function () {
      if (!expectedHash) return;
      save.disabled = true; message.textContent = t('board.intake.editSaving');
      tasksApi.editBacklog(stem, expectedHash, textarea.value).then(function () {
        boardModal.close(); clipboard.toast(t('board.intake.editSaved'));
        store.load(); boardLoadController.load({ closeOpenModal: false });
      }, function (error) {
        if (handleTaskMutationConflict(error)) return;
        save.disabled = false;
        message.textContent = boardRequestError(error);
      });
    });
    actions.appendChild(save); actions.appendChild(boardModal.createCloseButton()); content.appendChild(actions);
    boardModal.open(content);
    var token = state.activeModal;
    tasksApi.loadBacklogSource(stem).then(function (result) {
      if (state.activeModal !== token) return;
      expectedHash = result.sourceHash; textarea.value = result.markdown || ''; textarea.disabled = false; save.disabled = false;
      message.textContent = t('board.intake.editHint');
    }, function (error) {
      if (state.activeModal !== token) return;
      message.textContent = boardRequestError(error);
    });
  }

  function openTaskDetails(stem, preferredSection, fallbackItem, deepTarget) {
    var loading = boardTaskDetailsShell.loading(stem, fallbackItem);
    boardModal.open(loading);
    var detailAbort = typeof AbortController !== 'undefined' ? new AbortController() : null;
    state.detailsAbort = detailAbort;
    // Lazy panes must read the CURRENT controller, not the one captured when the
    // modal opened: a staleness abort nulls it, and a captured dead signal made
    // every later tab click fail as "the local server could not be reached",
    // with a Reload button that reused the same dead signal forever.
    function detailSignal() {
      return state.detailsAbort ? state.detailsAbort.signal : undefined;
    }
    var modalToken = state.activeModal;
    tasksApi.loadTaskDetails(stem, { signal: detailAbort && detailAbort.signal }).then(function (details) {
      if (state.activeModal !== modalToken || !loading.isConnected) return;
      if (state.openCard && state.openCard.stem === stem) {
        state.openCard = {
          folder: details.state.column,
          stem: stem,
          sourceRevision: details.primaryAction.expectedSourceRevision,
          actionRevision: details.primaryAction.actionRevision
        };
      }
      var cache = figmaTaskReadModel.entry(stem);
      var reviewPending = !!(cache && cache.evidence && cache.evidence.reviewPending > 0);
      var figmaTabs = reviewPending
        ? buildFigmaCompareTab(stem, function () { return modalToken; }, details.state.column, details)
        : buildFigmaScreensTab(stem, function () { return modalToken; });
      var artifactExtraNode = el('div', { class: 'task-details__artifact-extras' });
      if (details.state.column === 'backlog' || details.state.column === 'todo') {
        var figmaPull = figmaScreensButton(details.state.column, stem);
        if (figmaPull) artifactExtraNode.appendChild(figmaPull);
      }
      if (figmaTabs && figmaTabs[0]) artifactExtraNode.appendChild(figmaTabs[0].pane);
      if (!artifactExtraNode.firstChild) artifactExtraNode = null;
      var viewRef = { value: null };

      function navigation(action) {
        var target = action && action.target;
        if (target && target.type === 'terminal' && target.key) {
          boardModal.close();
          terminal.open(target.key);
          return true;
        }
        if (action.kind === 'validate-in-app') {
          // The app-run root sits far below the board modal's layer, so opening
          // it without closing the modal put the menu behind the backdrop — the
          // click looked dead and the focus trap made it unreachable.
          boardModal.close();
          appRunControl.open({ taskStem: stem });
          return true;
        }
        if (target && target.type === 'panel') {
          boardModal.close();
          if (!router.openTarget(target)) clipboard.toastError(t('board.action.targetUnavailable'));
          return true;
        }
        if (target && target.type === 'task' && target.stem !== stem) {
          boardTaskNavigationController.openTarget(target, details);
          return true;
        }
        if (viewRef.value) {
          viewRef.value.select(boardTaskDetailsShell.sectionForTarget(target), true);
          setTimeout(function () {
            if (viewRef.value) viewRef.value.focusPreferred(target && target.section || null);
          }, 0);
        }
        return true;
      }

      function execute(action, button, input) {
        input = input || {};
        var detailsExecutionKinds = [
          'submit-answers', 'continue-live', 'retry-phase', 'resume-finalization'
        ];
        if (detailsExecutionKinds.indexOf(action.kind) < 0 && action.behavior !== 'execute') {
          navigation(action);
          return Promise.resolve({ navigation: true });
        }
        if (boardReadiness.startupRecoveryBlocksMutation()) {
          return Promise.reject({ kind: 'startup-recovery' });
        }
        button.disabled = true;
        button.setAttribute('aria-busy', 'true');
        if (action.kind === 'integrate') {
          // The Integrate button never commits on the first click: it opens the
          // preview so the owner sees the exact diff and the exact blockers
          // before authorizing one canonical commit.
          button.removeAttribute('aria-busy');
          button.disabled = false;
          boardModal.close();
          boardIntegrationController.open(stem);
          return Promise.resolve({ navigation: true });
        }
        if (action.kind === 'resume-finalization') {
          var finalization = details.recovery && details.recovery.finalization;
          if (!finalization) return Promise.reject({ kind: 'finalization-resume-unavailable' });
          return tasksApi.resumeFinalization(finalization).then(function (response) {
            button.removeAttribute('aria-busy');
            return response;
          });
        }
        return tasksApi.executeTaskAction(stem, action, null, input).then(function (response) {
          button.removeAttribute('aria-busy');
          return response;
        });
      }

      function executed(response) {
        if (!response || response.navigation) return;
        if (response.status === 'continued') {
          clipboard.toast(t('taskDetails.liveAnswer.sent'));
          boardModal.close();
          terminal.open('task:' + stem);
          Promise.resolve(store.load()).then(function () {
            boardLoadController.load({ closeOpenModal: false });
          });
          return;
        }
        // The presenter's verdict is the accurate one and lands in the same tick;
        // a fixed "started" toast here was only ever painted and clobbered.
        enqueueResultPresenter.present(response, stem, function () {
          boardModal.close();
          terminal.open('task:' + stem);
        });
        Promise.resolve(store.load()).then(function () {
          boardLoadController.load({ closeOpenModal: false });
        });
      }

      function overflow(kind, action, input) {
        if (kind === 'copy-id') {
          clipboard.copy(stem);
          return;
        }
        if (kind === 'copy-link') {
          var selectedTab = modalToken.querySelector(
            '[data-task-details-tab][aria-selected="true"]'
          );
          var linkedSection = selectedTab &&
            selectedTab.getAttribute('data-task-details-tab') ||
            preferredSection || 'overview';
          var href = location.origin + location.pathname + '#board?task=' +
            encodeURIComponent(stem) + '&tab=' +
            encodeURIComponent(linkedSection);
          if (deepTarget && deepTarget.artifactId && linkedSection === 'artifacts') {
            href += '&artifact=' + encodeURIComponent(deepTarget.artifactId);
          }
          if (deepTarget && deepTarget.checkpointId && linkedSection === 'advanced') {
            href += '&checkpoint=' + encodeURIComponent(deepTarget.checkpointId);
          }
          clipboard.copy(href);
          return;
        }
        if (kind === 'source') {
          boardTaskNavigationController.openSource(details.origin && details.origin.target, details);
          return;
        }
        if (kind === 'edit' && details.state.column === 'backlog') {
          openBacklogEditModal(stem, details);
          return;
        }
        if (kind === 'open-terminal') {
          boardModal.close();
          terminal.open('task:' + stem);
          return;
        }
        if (kind === 'export-result') {
          clipboard.copy(JSON.stringify({
            task: details.identity.stem,
            title: details.identity.title,
            status: details.outcome.status,
            completedAt: details.outcome.completedAt,
            acceptance: details.outcome.acceptance,
            caveats: details.outcome.caveats,
            followUps: details.outcome.followUps
          }, null, 2));
          return;
        }
        if (kind === 'advanced' && viewRef.value) {
          viewRef.value.select('advanced', true);
          return;
        }
        if (kind === 'copy-prompt' && action) {
          clipboard.toast(t('board.overflow.preparing_prompt'));
          if (details.primaryAction && details.primaryAction.kind === 'submit-answers' &&
              (!input || !input.answers)) {
            clipboard.toastError(t('board.paused.answerEmpty'));
            return;
          }
          tasksApi.loadTaskActionPrompt(stem, action, input).then(function (response) {
            clipboard.copy(response.text);
          }, function (error) {
            clipboard.toastError(t('board.action.failed', { detail: boardRequestError(error) }));
          });
          return;
        }
        if (kind === 'drop' && action) {
          runDropActionFlow(stem, details.state.column, action);
          return;
        }
        if (kind === 'reopen' && action) {
          runReopenActionFlow(stem, action);
          return;
        }
      }

      function staleNotice() {
        var detailsRoot = modalToken.querySelector('[data-task-details-stem="' + stem + '"]') || loading;
        if (!detailsRoot.parentNode || detailsRoot.querySelector('.task-details__stale')) return;
        var notice = el('div', { class: 'banner banner--warn task-details__stale' });
        notice.appendChild(el('span', { text: t('taskDetails.stale') }));
        var reload = el('button', {
          type: 'button', class: 'btn btn--sm', text: t('taskDetails.reload')
        });
        reload.addEventListener('click', function () {
          openTaskDetails(stem, preferredSection, fallbackItem);
        });
        notice.appendChild(reload);
        detailsRoot.insertBefore(notice, detailsRoot.querySelector('.task-details__tabs'));
      }

      var view = createTaskDetails(details, {
        t: t,
        preferredSection: preferredSection || null,
        formatTimestamp: boardFormatters.timestampLabel,
        errorText: function (error) { return boardRequestError(error); },
        loadActivity: function (cursor) { return tasksApi.loadTaskActivity(stem, {
          limit: 50, cursor: cursor || null, signal: detailSignal()
        }); },
        loadArtifacts: function (cursor) { return tasksApi.loadTaskArtifacts(stem, {
          limit: 50, cursor: cursor || null, signal: detailSignal()
        }); },
        loadAdvanced: function () {
          return tasksApi.loadTaskAdvanced(
            stem, ['raw', 'revisions', 'runtime', 'integrity', 'outcome', 'checkpoints', 'diagnostics'],
            { signal: detailSignal() }
          );
        },
        inspectLockRecovery: function () {
          return tasksApi.loadTaskLockRecovery(stem);
        },
        recoverLock: function (expectedLockHash) {
          if (boardReadiness.startupRecoveryBlocksMutation()) {
            return Promise.reject({ kind: 'startup-recovery' });
          }
          return tasksApi.recoverTaskLock(stem, expectedLockHash);
        },
        onLockRecovered: function () {
          clipboard.toast(t('board.lockRecovery.released'));
          Promise.resolve(store.load()).then(function () {
            boardLoadController.load({ closeOpenModal: false });
            openTaskDetails(stem, 'advanced', fallbackItem);
          });
        },
        loadCheckpoints: function () {
          return tasksApi.loadTaskCheckpoints(stem, { signal: detailSignal() });
        },
        previewRetry: function (action, checkpointHash) {
          return tasksApi.previewTaskRetry(stem, action, checkpointHash);
        },
        onExecute: execute,
        onExecuted: executed,
        onError: function (error) {
          if (error && (error.kind === 'action-stale' || error.kind === 'checkpoint-stale')) staleNotice();
          clipboard.toastError(t('board.action.failed', { detail: boardRequestError(error) }));
        },
        onOverflowAction: overflow,
        onRetryIntake: function (intake, button) {
          button.disabled = true;
          tasksApi.retryShallowIntake(stem, intake.sourceHash).then(function () {
            return Promise.resolve(store.load()).then(function () {
              openTaskDetails(stem, 'overview', fallbackItem);
            });
          }, function (error) {
            button.disabled = false;
            clipboard.toastError(boardRequestError(error));
          });
        },
        onDismissIntake: function (intake, button) {
          button.disabled = true;
          tasksApi.dismissShallowIntake(stem, intake.sourceHash).then(function () {
            return Promise.resolve(store.load()).then(function () {
              openTaskDetails(stem, 'overview', fallbackItem);
            });
          }, function (error) {
            button.disabled = false;
            clipboard.toastError(boardRequestError(error));
          });
        },
        onOpenTarget: function (target) {
          if (target && target.panel === 'app-run') {
            boardModal.close();
            appRunControl.open({ taskStem: stem });
            return;
          }
          if (target && target.panel === 'figma-compare') return;
          if (!router.openTarget(target)) clipboard.toastError(t('board.action.targetUnavailable'));
        },
        onClose: boardModal.close,
        onStale: staleNotice,
        confirm: boardConfirmDialog.open,
        artifactExtraNode: artifactExtraNode,
        focusArtifactId: deepTarget && deepTarget.artifactId || null,
        focusCheckpointId: deepTarget && deepTarget.checkpointId || null
      });
      viewRef.value = view;
      loading.parentNode.replaceChild(view.node, loading);
      var panel = modalToken.querySelector('.board-modal__content');
      var title = view.node.querySelector('.board-modal__title');
      if (panel && title) {
        title.id = 'board-modal-title';
        panel.setAttribute('aria-labelledby', title.id);
      }
      setTimeout(function () { view.focusPreferred(preferredSection || null); }, 0);
    }, function (error) {
      boardTaskDetailsShell.showLoadError({
        modalToken: modalToken,
        loading: loading,
        error: error,
        onRetry: function () {
          openTaskDetails(stem, preferredSection, fallbackItem, deepTarget);
        },
        getSecondaryAction: function () {
          var dropAction = secondaryTaskAction(fallbackItem, 'drop');
          if (!dropAction || dropAction.enabled === false) return null;
          return {
            label: t('board.overflow.drop'),
            className: 'btn btn--danger btn--sm',
            onClick: function () {
              runDropActionFlow(stem, fallbackItem && fallbackItem.state || null, dropAction);
            }
          };
        }
      });
    });
  }

  function openCardModal(folder, stem, item, section, deepTarget) {
    if (['backlog', 'pending', 'todo', 'done'].indexOf(folder) < 0) return;
    openTaskDetails(stem, section || null, item, deepTarget || null);
    // Record the open card's identity AFTER the per-folder opener (each calls
    // boardModal.open → boardModal.close, which nulls openCard). Lets the controller restore
    // focus by stem when an SSE rebuild detached the originating card, and lets
    // a post-refresh check neutralize the modal if the task left its folder.
    // Set here (not in boardModal.open) so non-card modals don't carry a card id.
    state.openCard = {
      folder: folder,
      stem: stem,
      sourceRevision: item && item.sourceRevision || null,
      actionRevision: item && item.primaryAction && item.primaryAction.actionRevision || null
    };
    state.lastFocusStem = stem;
  }

  // Mount / refresh entry points.
  // ----------------------------------------------------------------------

  // The board only listens to store changes while it's the active panel.
  // Reasons we re-fetch / re-render on a change:
  //   - SSE pushed a 'change' (filesystem mutation under orchestrator/tasks/
  //     or any other watched path → INDEX.json may have moved)
  //   - The wizard gate flipped (setupDone or wizardComplete went true)
  // The modal stays open across SSE-driven refreshes so the user doesn't
  // lose their place mid-copy. BoardRefreshClock coalesces event bursts while
  // retaining the last usable summary during the reload.

  function onStoreChange() {
    if (!sectionEl) return;
    if (router && typeof router.current === 'function' &&
        router.current() !== 'board') return;
    var storeState = store.get();
    if (!boardRenderController.isComplete(storeState)) {
      boardRenderController.render();
      return;
    }
    boardRefreshClock.startClock();
    // Keep a disabled/busy recovery dialog live with marker-first SSE state.
    // It enables as soon as the global mutex becomes available and disappears
    // after successful cleanup removes the marker.
    boardFinalizationController.refreshOpen();
    boardIntegrationController.refreshOpen();
    boardTaskInbox.load();
    boardRefreshClock.scheduleRefresh();
  }

  // Relative activity labels and the worker-offline warning are evaluated at
  // render time. A lock/request that sits without an SSE change would otherwise
  // leave those labels stale. This interval re-renders the board every 30 s
  // while either signal exists; it does nothing on an idle board. The interval is
  // installed once on the first mount and never cleared; the board module
  // is loaded once per page, so a second mount() (e.g. panel re-entry)
  // must not stack a new timer.

  // Installed once per page (the board module loads once). A nav away from the
  // board must dismiss any open modal, else it strands on top of the next panel
  // boardModal.close is a no-op when nothing is open (it guards on activeModal).
  var hashListenerInstalled = false;

  export const board = {
    mount: function (rootEl) {
      sectionEl = rootEl;
      boardTaskTargetController.consumeDeepLink();
      // Programmatically focusable so the M5 close-focus fallback can land here
      // when the originating card and its stem-twin are both gone.
      if (!sectionEl.hasAttribute('tabindex')) sectionEl.setAttribute('tabindex', '-1');
      store.on('change', onStoreChange);
      if (!hashListenerInstalled) {
        window.addEventListener('hashchange', function () {
          if (router.current() !== 'board') boardModal.forceClose();
        });
        hashListenerInstalled = true;
      }
      var storeState = store.get();
      boardTaskInbox.load();
      if (!boardRenderController.isComplete(storeState)) {
        boardRenderController.render();
        return;
      }
      boardLoadController.load({ closeOpenModal: true });
      boardRefreshClock.startClock();
    },
    refresh: function () {
      // On language change / re-entry we re-render with whatever summary we
      // already have; mount() and SSE 'change' handle re-fetching.
      boardRenderController.render();
      boardTaskTargetController.consumeDeepLink();
      boardTaskTargetController.openRequested(false);
    },
    openTask: function (stem) {
      if (!boardTaskTargetController.request(stem, 'overview', null)) return;
      router.go('board');
      if (!state.columns || boardTaskTargetController.openRequested(false) === false) {
        boardLoadController.load({ closeOpenModal: true });
      }
    }
  };
