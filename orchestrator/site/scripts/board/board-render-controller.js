// Owns the Board root composition lifecycle. All state and child renderers are
// injected so this module cannot acquire store, router, or task authority.
export function createBoardRenderController(dependencies) {
  function isComplete(storeState) {
    if (!storeState || !storeState.progress) return false;
    if (storeState.progress.setupDone !== true) return false;
    if (typeof dependencies.wizardComplete === 'function') {
      return dependencies.wizardComplete(storeState);
    }
    return false;
  }

  function renderGate(section) {
    var banner = dependencies.el('div', { class: 'banner banner--info' });
    banner.appendChild(dependencies.createTextNode(dependencies.t('board.gate') + ' '));
    banner.appendChild(dependencies.el('a', {
      href: '#wizard', text: dependencies.t('common.openWizard')
    }));
    section.appendChild(banner);
  }

  function render() {
    var section = dependencies.getSectionElement();
    if (!section) return;
    var scrollSnapshot = dependencies.viewport.captureScroll();
    var storeState = dependencies.getSnapshot();
    var complete = isComplete(storeState);
    var toolbarElement = complete ? dependencies.toolbar.preservedElement() : null;
    var preserveToolbar = !!toolbarElement;
    var healthElement = dependencies.health.element();
    var preserveHealth = healthElement && healthElement.parentNode === section;
    var focusSnapshot = dependencies.viewport.captureFocus();

    var child = section.firstChild;
    while (child) {
      var next = child.nextSibling;
      if ((!preserveToolbar || child !== toolbarElement) &&
          (!preserveHealth || child !== healthElement)) section.removeChild(child);
      child = next;
    }

    if (!complete) {
      dependencies.health.render(storeState);
      renderGate(section);
      dependencies.inbox.render(false);
      return;
    }

    dependencies.health.render(storeState);
    dependencies.toolbar.render();
    dependencies.inbox.render(true);

    var state = dependencies.getState();
    if (state.loading && !state.columns) {
      section.appendChild(dependencies.el('p', {
        class: 'panel-lead', text: dependencies.t('board.loading')
      }));
      return;
    }
    if (state.error && !state.columns) {
      section.appendChild(dependencies.el('p', {
        class: 'board-unavailable', text: dependencies.t('board.status.boardUnavailable')
      }));
      return;
    }

    dependencies.taskList.render();
    dependencies.viewport.restoreScroll(scrollSnapshot);
    dependencies.viewport.restoreFocus(focusSnapshot);
  }

  return {
    isComplete: isComplete,
    render: render
  };
}
