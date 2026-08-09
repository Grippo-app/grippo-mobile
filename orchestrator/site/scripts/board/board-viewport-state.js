export function createBoardViewportState(dependencies) {
  var getDocumentNode = dependencies.getDocumentNode;
  var getViewport = dependencies.getViewport;
  var getSectionElement = dependencies.getSectionElement;

  function captureFocus() {
    var documentNode = getDocumentNode();
    var sectionElement = getSectionElement();
    var active = documentNode.activeElement;
    if (!active || !sectionElement.contains(active)) return null;
    var result = {
      filter: active.getAttribute && active.getAttribute('data-board-filter'),
      stem: null,
      control: active.getAttribute && active.getAttribute('data-task-control'),
      start: typeof active.selectionStart === 'number' ? active.selectionStart : null,
      end: typeof active.selectionEnd === 'number' ? active.selectionEnd : null
    };
    var card = active.closest && active.closest('.board-card[data-stem]');
    if (card) result.stem = card.getAttribute('data-stem');
    return result;
  }

  function restoreFocus(snapshot) {
    if (!snapshot) return;
    var documentNode = getDocumentNode();
    var sectionElement = getSectionElement();
    var target = null;
    if (snapshot.filter) {
      target = sectionElement.querySelector('[data-board-filter="' + snapshot.filter + '"]');
    } else if (snapshot.stem && snapshot.control) {
      target = sectionElement.querySelector('.board-card[data-stem="' + snapshot.stem + '"] [data-task-control="' + snapshot.control + '"]:not([disabled])');
    }
    if (!target && snapshot.stem && snapshot.control && snapshot.control.indexOf('overflow-') === 0) {
      var trigger = sectionElement.querySelector('.board-card[data-stem="' + snapshot.stem + '"] [data-task-control="overflow"]');
      if (trigger && trigger.getAttribute('aria-expanded') === 'true') trigger.click();
      target = trigger;
    }
    if (!target) return;
    // A preserved toolbar control never lost focus. Calling focus() again while
    // its native select popup is open is browser-dependent and can dismiss it.
    if (target === documentNode.activeElement) return;
    try {
      target.focus();
      if (snapshot.start != null && typeof target.setSelectionRange === 'function') {
        target.setSelectionRange(snapshot.start, snapshot.end);
      }
    } catch (error) {}
  }

  function captureScroll() {
    var viewport = getViewport();
    var sectionElement = getSectionElement();
    var columnScroll = {};
    var oldBodies = sectionElement.querySelectorAll('.board-column__body');
    for (var i = 0; i < oldBodies.length; i++) {
      if (!oldBodies[i].scrollTop) continue;
      var columnHost = oldBodies[i].parentNode;
      var folder = columnHost && columnHost.getAttribute
        ? columnHost.getAttribute('data-folder')
        : null;
      if (folder) columnScroll[folder] = oldBodies[i].scrollTop;
    }
    return {
      columns: columnScroll,
      pageX: viewport.pageXOffset,
      pageY: viewport.pageYOffset
    };
  }

  // Runs synchronously in the same task as the rebuild, so the browser never
  // paints a reset-to-top frame. The browser clamps values for shrunk columns.
  function restoreScroll(snapshot) {
    var viewport = getViewport();
    var sectionElement = getSectionElement();
    var columns = sectionElement.querySelectorAll('.board-column');
    for (var i = 0; i < columns.length; i++) {
      var folder = columns[i].getAttribute('data-folder');
      if (!folder || !snapshot.columns[folder]) continue;
      var body = columns[i].querySelector('.board-column__body');
      if (body) body.scrollTop = snapshot.columns[folder];
    }
    if (viewport.pageXOffset !== snapshot.pageX || viewport.pageYOffset !== snapshot.pageY) {
      viewport.scrollTo(snapshot.pageX, snapshot.pageY);
    }
  }

  return {
    captureFocus: captureFocus,
    restoreFocus: restoreFocus,
    captureScroll: captureScroll,
    restoreScroll: restoreScroll
  };
}
