import { i18n } from './i18n.js';

  var toastTimer = null;
  var activeToast = null;

  // A toast is readable only while it is on screen, so the hold scales with the
  // message instead of using one constant tuned for "Copied!". ~14 characters
  // per second is a slow-reader floor; the localized strings run 5-15% longer
  // than English, and the ceiling keeps a long message from parking forever.
  var TOAST_MIN_MS = 3000;
  var TOAST_MAX_MS = 10000;
  var TOAST_MS_PER_CHAR = 70;
  var TOAST_FADE_MS = 250;

  function toastHoldMs(message) {
    var length = String(message == null ? '' : message).length;
    return Math.min(TOAST_MAX_MS, Math.max(TOAST_MIN_MS, length * TOAST_MS_PER_CHAR));
  }

  function getToastHost(region) {
    var dialogs = document.querySelectorAll('dialog[open]');
    // A modal <dialog> lives in the browser's top layer, which no body-level
    // z-index can overtake. Mount only the transient toast there; the shared
    // live region itself stays attached to body even if the dialog is removed.
    return dialogs.length ? dialogs[dialogs.length - 1] : region;
  }

  function getRegion() {
    return document.getElementById('toast-region');
  }

  function showToast(message, options) {
    var region = getRegion();
    if (!region) return;
    var error = !!(options && options.error);
    var host = getToastHost(region);
    if (activeToast && activeToast.parentNode) {
      activeToast.parentNode.removeChild(activeToast);
    }
    region.innerHTML = '';
    var toast = document.createElement('div');
    toast.className = error ? 'toast toast--error' : 'toast';
    toast.textContent = message;
    // A failure is not a confirmation: it interrupts the screen reader and is
    // styled as a failure, so it cannot be mistaken for "Copied!".
    toast.setAttribute('role', error ? 'alert' : 'status');
    toast.setAttribute('aria-atomic', 'true');
    host.appendChild(toast);
    activeToast = toast;
    // Force layout before adding the visible class so the transition runs.
    void toast.offsetWidth;
    toast.classList.add('toast--visible');
    var hold = toastHoldMs(message);
    function hide() {
      toast.classList.remove('toast--visible');
      setTimeout(function () {
        if (toast.parentNode) {
          toast.parentNode.removeChild(toast);
        }
        if (activeToast === toast) activeToast = null;
      }, TOAST_FADE_MS);
    }
    // The toast stays pointer-events: none (see components.css): it is centred
    // over the viewport bottom, where sticky action bars and modal footers live,
    // and a clickable toast would eat the first click on them for seconds.
    if (toastTimer) clearTimeout(toastTimer);
    toastTimer = setTimeout(hide, hold);
  }

  function showErrorToast(message) {
    showToast(message, { error: true });
  }

  function fallbackCopy(text) {
    try {
      var ta = document.createElement('textarea');
      ta.value = text;
      ta.setAttribute('readonly', '');
      ta.style.position = 'absolute';
      ta.style.left = '-9999px';
      document.body.appendChild(ta);
      ta.select();
      var ok = document.execCommand('copy');
      document.body.removeChild(ta);
      return ok ? Promise.resolve() : Promise.reject(new Error('copy failed'));
    } catch (e) {
      return Promise.reject(e);
    }
  }

  function tr(key) {
    if (i18n && typeof i18n.t === 'function') {
      return i18n.t(key);
    }
    return key;
  }

  function copy(text) {
    var value = text == null ? '' : String(text);
    var p;
    if (navigator.clipboard && typeof navigator.clipboard.writeText === 'function') {
      p = navigator.clipboard.writeText(value).catch(function () {
        return fallbackCopy(value);
      });
    } else {
      p = fallbackCopy(value);
    }
    return p.then(function () {
      showToast(tr('toast.copied'));
    }).catch(function () {
      showErrorToast(tr('toast.copyFailed'));
    });
  }

  function attach(buttonEl, getText, opts) {
    if (!buttonEl || typeof getText !== 'function') return;
    opts = opts || {};
    var revertTimer = null;
    buttonEl.addEventListener('click', function () {
      copy(getText()).then(function () {
        var original = buttonEl.getAttribute('data-original-label');
        if (original == null) {
          buttonEl.setAttribute('data-original-label', buttonEl.textContent);
        }
        if (!opts.keepLabel) buttonEl.textContent = tr('toast.copiedBtn');
        buttonEl.classList.add('copy-btn--copied');
        if (revertTimer) clearTimeout(revertTimer);
        revertTimer = setTimeout(function () {
          var label = buttonEl.getAttribute('data-original-label');
          if (!opts.keepLabel && label != null) buttonEl.textContent = label;
          buttonEl.classList.remove('copy-btn--copied');
        }, 1500);
      });
    });
  }

  export const clipboard = {
    copy: copy,
    attach: attach,
    // Exposed so non-copy actions (e.g. the Board's "Run in Claude" enqueue)
    // can surface the same transient feedback without abusing copy().
    toast: showToast,
    // Failures go through this one: assertive for screen readers, styled as a
    // failure, and never mistakable for a confirmation.
    toastError: showErrorToast
  };
