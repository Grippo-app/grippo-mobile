export function createBoardTaskCardFactory(dependencies) {
  function closeOtherMenus(menu) {
    var section = dependencies.getSectionElement();
    var openMenus = section.querySelectorAll('.board-card__overflow-menu:not([hidden])');
    for (var i = 0; i < openMenus.length; i++) {
      if (openMenus[i] === menu) continue;
      openMenus[i].hidden = true;
      var parent = openMenus[i].parentNode;
      var trigger = parent && parent.querySelector('.board-card__overflow-trigger');
      if (trigger) trigger.setAttribute('aria-expanded', 'false');
    }
  }

  function create(folder, item) {
    // The summary DTO owns the current state. The folder argument remains for
    // task-list placement and crash-recovery projection only.
    item.state = item.state || folder;
    var mutationsBlocked = dependencies.mutationsBlocked();
    var cardItem = item;
    if (mutationsBlocked && item.primaryAction && item.primaryAction.behavior === 'execute') {
      cardItem = Object.assign({}, item, {
        primaryAction: Object.assign({}, item.primaryAction, {
          enabled: false,
          disabledReasonCode: 'startup-recovery'
        })
      });
    }
    return dependencies.renderCard(cardItem, {
      t: dependencies.t,
      formatRelative: dependencies.formatRelative,
      mutationsBlocked: mutationsBlocked,
      menuState: dependencies.menuState,
      onMenuOpen: function (stem, menu) { closeOtherMenus(menu); },
      onOpenDetails: function (row) { dependencies.openDetails(folder, row); },
      onExecute: dependencies.execute,
      onNavigate: dependencies.navigate,
      onAction: dependencies.action
    });
  }

  return { create: create };
}
