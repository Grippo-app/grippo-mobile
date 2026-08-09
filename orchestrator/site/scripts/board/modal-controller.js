import { dom } from '../dom.js';

var el = dom.el;

export function createBoardModalController(dependencies) {
  var t = dependencies.t;
  var state = dependencies.state;
  var getSectionElement = dependencies.getSectionElement;

  function open(content) {
    close();
    // Remember which element had focus so we can return there on close.
    state.lastFocus = (document.activeElement && document.activeElement !== document.body)
      ? document.activeElement
      : null;
    var overlay = el('div', { class: 'board-modal' });
    var panel = el('div', {
      class: 'board-modal__content',
      attrs: { 'role': 'dialog', 'aria-modal': 'true', 'tabindex': '-1' }
    });
    panel.appendChild(content);
    var _t = panel.querySelector('.board-modal__title');
    if (_t) { _t.id = 'board-modal-title'; panel.setAttribute('aria-labelledby', 'board-modal-title'); }
    var _d = panel.querySelector('.board-modal__confirm-message');
    if (_d) { _d.id = 'board-modal-description'; panel.setAttribute('aria-describedby', 'board-modal-description'); }
    overlay.appendChild(panel);
    // Backdrop dismiss fires ONLY on a genuine click that both starts and ends
    // on the overlay. Without the mousedown guard, a text selection that begins
    // inside the dialog (mousedown on an input/textarea) and is released on the
    // backdrop dispatches a single `click` on the overlay — the nearest common
    // ancestor of the down/up targets — which silently closed the dialog and
    // discarded unsaved input (the create-task form was the worst case). Pinning
    // where the press began lets us tell a real backdrop click from a selection
    // drag that overshot the frame, while keeping click-outside-to-dismiss for
    // deliberate clicks. Applies to every board modal.
    var pressBeganOnOverlay = false;
    overlay.addEventListener('mousedown', function (e) {
      pressBeganOnOverlay = (e.target === overlay);
    });
    overlay.addEventListener('click', function (e) {
      if (e.target === overlay && pressBeganOnOverlay) close();
      pressBeganOnOverlay = false;
    });
    document.body.appendChild(overlay);
    state.activeModal = overlay;
    var FOCUSABLE = 'a[href], button:not([disabled]), input:not([disabled]), ' +
      'textarea:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])';
    state.escHandler = function (e) {
      if (e.key === 'Escape' || e.keyCode === 27) { close(); return; }
      // Trap Tab inside the dialog so keyboard users can't wander into the
      // column buttons behind the overlay (the background is not inert).
      if (e.key === 'Tab' || e.keyCode === 9) {
        var items = Array.prototype.filter.call(panel.querySelectorAll(FOCUSABLE), function (node) {
          if (!node || node.getAttribute('tabindex') === '-1') return false;
          if (node.hidden) return false;
          var cs = window.getComputedStyle ? window.getComputedStyle(node) : null;
          if (cs && (cs.display === 'none' || cs.visibility === 'hidden')) return false;
          return !!(node.offsetWidth || node.offsetHeight || node.getClientRects().length);
        });
        if (!items.length) { e.preventDefault(); panel.focus(); return; }
        var first = items[0];
        var last = items[items.length - 1];
        var active = document.activeElement;
        var inside = panel.contains(active);
        if (e.shiftKey) {
          if (!inside || active === first || active === panel) {
            e.preventDefault();
            last.focus();
          }
        } else if (!inside || active === last) {
          e.preventDefault();
          first.focus();
        }
      }
    };
    document.addEventListener('keydown', state.escHandler);
    // Focus the dialog itself so Tab cycles into the modal content, not
    // into the column buttons behind the overlay. Defer until layout so
    // the focus styles paint.
    setTimeout(function () { try { panel.focus(); } catch (e) {} }, 0);
  }

  function close() {
    if (state.detailsAbort) {
      try { state.detailsAbort.abort(); } catch (e0) {}
      state.detailsAbort = null;
    }
    if (state.activeModal && state.activeModal.parentNode) {
      var cleanupNodes = state.activeModal.querySelectorAll('[data-board-cleanup]');
      for (var i = 0; i < cleanupNodes.length; i++) {
        if (cleanupNodes[i] && typeof cleanupNodes[i].__boardCleanup === 'function') {
          try { cleanupNodes[i].__boardCleanup(); } catch (e) {}
        }
      }
      state.activeModal.parentNode.removeChild(state.activeModal);
    }
    state.activeModal = null;
    state.openCard = null;
    state.openFinalizationStem = null;
    if (state.escHandler) {
      document.removeEventListener('keydown', state.escHandler);
      state.escHandler = null;
    }
    // A confirm dialog registers an onCancel here; it fires on ANY close that
    // wasn't the confirm action (Esc / overlay-click / Cancel), letting Drop
    // re-open the originating card modal. Cleared + captured
    // before invoking so a re-open (which opens a new modal) can't re-enter it.
    // When it re-opens a modal, that modal owns focus, so skip the restore below.
    var onCancel = state.onModalCancel;
    state.onModalCancel = null;
    if (typeof onCancel === 'function') { onCancel(); return; }
    restoreFocusAfterClose();
  }

  // Close WITHOUT firing the onCancel re-open hook. Used by programmatic
  // dismissals — navigating away (hashchange) and the wizard gate flipping —
  // where a confirm's "re-open the card modal" hook would re-strand that modal
  // on top of the next view.
  // Genuine user dismissals (Esc / overlay-click / Cancel) still go through
  // close() and keep the re-open.
  function forceClose() {
    state.onModalCancel = null;
    close();
  }

  // Return focus to the card the user opened the modal from so the tab order
  // stays predictable and the keyboard user isn't dumped at <body>. The stored
  // element can be detached when an SSE column rebuild ran while the modal was
  // open: fall back to the card with the same stem, then to the column
  // container. Defensive try/catch throughout — focus() can throw on a node
  // that's mid-detach.
  function restoreFocusAfterClose() {
    var stem = state.lastFocusStem;
    var prior = state.lastFocus;
    var sectionEl = getSectionElement();
    state.lastFocus = null;
    state.lastFocusStem = null;
    if (stem && sectionEl && typeof CSS !== 'undefined' && CSS.escape) {
      var card = sectionEl.querySelector('.board-card[data-stem="' + CSS.escape(stem) + '"]');
      var priorCard = prior && prior.closest && prior.closest('.board-card[data-stem]');
      if (prior && document.contains(prior) && priorCard === card) {
        try {
          prior.focus();
          if (document.activeElement === prior) return;
        } catch (e) {}
      }
      var cardControl = card && (
        card.querySelector('[data-task-control="details"]:not([disabled])') ||
        card.querySelector('button:not([disabled]), a[href], [tabindex]:not([tabindex="-1"])')
      );
      if (cardControl) {
        try {
          cardControl.focus();
          if (document.activeElement === cardControl) return;
        } catch (e) {}
      }
    }
    // Non-card modals still return to their exact opener. For a card modal this
    // branch intentionally comes after the stem-specific control: a pointer
    // click on the card surface can leave the board section as activeElement,
    // which is not a meaningful return target.
    if (prior && document.contains(prior)) {
      try {
        prior.focus();
        if (document.activeElement === prior) return;
      } catch (e) {}
    }
    if (sectionEl) { try { sectionEl.focus(); } catch (e) {} }
  }

  function createCloseButton() {
    var btn = el('button', { type: 'button', class: 'btn board-modal__close-btn', text: t('board.modal.close') });
    btn.addEventListener('click', close);
    return btn;
  }

  return {
    open: open,
    close: close,
    forceClose: forceClose,
    createCloseButton: createCloseButton
  };
}
