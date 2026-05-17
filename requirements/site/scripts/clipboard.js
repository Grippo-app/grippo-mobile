(function () {
  window.App = window.App || {};

  var toastTimer = null;

  function getRegion() {
    return document.getElementById('toast-region');
  }

  function showToast(message) {
    var region = getRegion();
    if (!region) return;
    region.innerHTML = '';
    var toast = document.createElement('div');
    toast.className = 'toast';
    toast.textContent = message;
    region.appendChild(toast);
    // Force layout before adding the visible class so the transition runs.
    void toast.offsetWidth;
    toast.classList.add('toast--visible');
    if (toastTimer) clearTimeout(toastTimer);
    toastTimer = setTimeout(function () {
      toast.classList.remove('toast--visible');
      setTimeout(function () {
        if (toast.parentNode === region) {
          region.removeChild(toast);
        }
      }, 250);
    }, 1500);
  }

  function legacyCopy(text) {
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

  function copy(text) {
    var value = text == null ? '' : String(text);
    var p;
    if (navigator.clipboard && typeof navigator.clipboard.writeText === 'function') {
      p = navigator.clipboard.writeText(value).catch(function () {
        return legacyCopy(value);
      });
    } else {
      p = legacyCopy(value);
    }
    return p.then(function () {
      showToast('Copied!');
    }).catch(function () {
      showToast('Copy failed');
    });
  }

  function attach(buttonEl, getText) {
    if (!buttonEl || typeof getText !== 'function') return;
    var revertTimer = null;
    buttonEl.addEventListener('click', function () {
      copy(getText()).then(function () {
        var original = buttonEl.getAttribute('data-original-label');
        if (original == null) {
          buttonEl.setAttribute('data-original-label', buttonEl.textContent);
        }
        buttonEl.textContent = 'Copied!';
        buttonEl.classList.add('copy-btn--copied');
        if (revertTimer) clearTimeout(revertTimer);
        revertTimer = setTimeout(function () {
          var label = buttonEl.getAttribute('data-original-label');
          if (label != null) buttonEl.textContent = label;
          buttonEl.classList.remove('copy-btn--copied');
        }, 1500);
      });
    });
  }

  window.App.clipboard = {
    copy: copy,
    attach: attach
  };
})();
