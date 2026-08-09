export function createTaskListStore(initial) {
  var state = Object.assign({
    search: '', column: '', origin: '', blocker: '', dependency: '', context: '',
    sort: 'board', openMenuStem: null
  }, initial || {});
  return {
    get: function () { return Object.assign({}, state); },
    set: function (key, value) {
      if (!Object.prototype.hasOwnProperty.call(state, key)) return false;
      if (state[key] === value) return false;
      state[key] = value;
      return true;
    },
    filters: function () {
      return {
        search: state.search,
        column: state.column,
        origin: state.origin,
        blocker: state.blocker,
        dependency: state.dependency,
        context: state.context,
        sort: state.sort,
        limit: 500
      };
    },
    openMenu: function (stem) { state.openMenuStem = stem || null; },
    reconcileMenus: function (stems) {
      if (state.openMenuStem && (!Array.isArray(stems) || stems.indexOf(state.openMenuStem) < 0)) {
        state.openMenuStem = null;
      }
    },
    closeMenu: function (stem) {
      if (!stem || state.openMenuStem === stem) state.openMenuStem = null;
    },
    isMenuOpen: function (stem) { return state.openMenuStem === stem; }
  };
}
