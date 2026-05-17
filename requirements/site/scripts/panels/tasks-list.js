(function () {
  window.App = window.App || {};
  App.panels = App.panels || {};

  // ----------------------------------------------------------------------
  // DOM helpers — el() lives in scripts/dom.js (App.dom.el).
  // ----------------------------------------------------------------------

  var el = App.dom.el;

  var sectionEl = null;
  var storeUnsub = null;

  // ----------------------------------------------------------------------
  // Tiny relative-time formatter.
  //   <60s   -> "just now"
  //   <60min -> "Nm ago"
  //   <24h   -> "Nh ago"
  //   else   -> "YYYY-MM-DD" (UTC slice, no time)
  // ----------------------------------------------------------------------

  function relTime(iso) {
    if (!iso) return '';
    var then = Date.parse(iso);
    if (!isFinite(then)) return '';
    var now = Date.now();
    var diff = Math.max(0, now - then);
    var sec = Math.floor(diff / 1000);
    if (sec < 60) return 'just now';
    var min = Math.floor(sec / 60);
    if (min < 60) return min + 'm ago';
    var hr = Math.floor(min / 60);
    if (hr < 24) return hr + 'h ago';
    // Fall back to YYYY-MM-DD from the ISO string itself (avoids TZ wobble).
    return String(iso).slice(0, 10);
  }

  // ----------------------------------------------------------------------
  // State helpers.
  // ----------------------------------------------------------------------

  function readHistory() {
    var state = App.store.get();
    var prog = (state && state.progress) || {};
    var hist = prog.taskHistory;
    if (!Array.isArray(hist)) return [];
    // Shallow copy so sort doesn't mutate the cached snapshot returned by get().
    return hist.slice();
  }

  function writeHistory(next) {
    App.store.saveProgress({ taskHistory: next });
  }

  function findIndexByFilename(list, fn) {
    for (var i = 0; i < list.length; i++) {
      if (list[i] && list[i].filename === fn) return i;
    }
    return -1;
  }

  // ----------------------------------------------------------------------
  // Mutators.
  // ----------------------------------------------------------------------

  function markDone(fn) {
    var list = readHistory();
    var i = findIndexByFilename(list, fn);
    if (i < 0) return;
    list[i] = {
      n: list[i].n,
      filename: list[i].filename,
      friendlyTitle: list[i].friendlyTitle,
      createdAt: list[i].createdAt,
      status: 'done',
      doneAt: new Date().toISOString()
    };
    writeHistory(list);
    render();
  }

  function restore(fn) {
    var list = readHistory();
    var i = findIndexByFilename(list, fn);
    if (i < 0) return;
    list[i] = {
      n: list[i].n,
      filename: list[i].filename,
      friendlyTitle: list[i].friendlyTitle,
      createdAt: list[i].createdAt,
      status: 'active',
      doneAt: null
    };
    writeHistory(list);
    render();
  }

  function deleteEntry(fn) {
    var ok = window.confirm(
      'Delete this task from your local history? The file on disk is not affected.'
    );
    if (!ok) return;
    var list = readHistory();
    var next = [];
    for (var i = 0; i < list.length; i++) {
      if (list[i] && list[i].filename !== fn) next.push(list[i]);
    }
    writeHistory(next);
    render();
  }

  // ----------------------------------------------------------------------
  // Card builder.
  // ----------------------------------------------------------------------

  function buildCard(entry) {
    var card = el('div', { class: 'card task-history-card' });

    // Title — friendlyTitle (escaped via textContent in el's "text" attr).
    var title = el('strong', { class: 'task-history-title' });
    title.textContent = entry.friendlyTitle || '(untitled)';
    card.appendChild(title);

    // Filename in a <code> tag.
    var fn = el('code', { class: 'task-history-filename' });
    fn.textContent = entry.filename || '';
    card.appendChild(fn);

    // Meta line: "Created <rel>" plus "· done <rel>" when status==='done'.
    var meta = el('p', { class: 'task-history-meta' });
    meta.textContent = 'Created ' + relTime(entry.createdAt);
    if (entry.status === 'done' && entry.doneAt) {
      meta.textContent += ' · done ' + relTime(entry.doneAt);
    }
    card.appendChild(meta);

    // Action row.
    var actions = el('div', { class: 'task-history-actions' });
    if (entry.status === 'done') {
      var restoreBtn = el('button', {
        type: 'button',
        class: 'btn',
        text: 'Restore'
      });
      (function (filename) {
        restoreBtn.addEventListener('click', function () { restore(filename); });
      })(entry.filename);
      actions.appendChild(restoreBtn);
    } else {
      var doneBtn = el('button', {
        type: 'button',
        class: 'btn btn--primary',
        text: 'Mark as done'
      });
      (function (filename) {
        doneBtn.addEventListener('click', function () { markDone(filename); });
      })(entry.filename);
      actions.appendChild(doneBtn);
    }
    var delBtn = el('button', {
      type: 'button',
      class: 'btn',
      text: 'Delete'
    });
    (function (filename) {
      delBtn.addEventListener('click', function () { deleteEntry(filename); });
    })(entry.filename);
    actions.appendChild(delBtn);
    card.appendChild(actions);

    return card;
  }

  // ----------------------------------------------------------------------
  // Section builders.
  // ----------------------------------------------------------------------

  function buildSection(titleText, entries, emptyText) {
    var wrap = document.createDocumentFragment();
    wrap.appendChild(el('h3', { class: 'panel-section-title', text: titleText }));
    if (entries.length === 0) {
      wrap.appendChild(el('p', { class: 'panel-lead', text: emptyText }));
      return wrap;
    }
    var grid = el('div', { class: 'task-history-grid' });
    for (var i = 0; i < entries.length; i++) {
      grid.appendChild(buildCard(entries[i]));
    }
    wrap.appendChild(grid);
    return wrap;
  }

  // ----------------------------------------------------------------------
  // Render orchestration.
  // ----------------------------------------------------------------------

  function render() {
    if (!sectionEl) return;
    while (sectionEl.firstChild) sectionEl.removeChild(sectionEl.firstChild);

    sectionEl.appendChild(el('h2', { class: 'panel-title', text: 'Task History' }));

    var lead = el('p', { class: 'panel-lead' });
    lead.appendChild(document.createTextNode(
      'Local browser history of tasks you created in Task Form. The actual files in requirements/tasks/ are managed by you and the orchestrator — use "Mark as done" to mirror the orchestrator’s move to '
    ));
    lead.appendChild(el('code', { text: 'tasks/done/' }));
    lead.appendChild(document.createTextNode('.'));
    sectionEl.appendChild(lead);

    var list = readHistory();

    if (list.length === 0) {
      var banner = el('div', { class: 'banner banner--info' });
      banner.appendChild(document.createTextNode('No tasks yet. '));
      banner.appendChild(el('a', { href: '#tasks', text: 'Open Task Form' }));
      sectionEl.appendChild(banner);
      return;
    }

    // Split + sort.
    var active = [];
    var done = [];
    for (var i = 0; i < list.length; i++) {
      var e = list[i];
      if (!e || typeof e !== 'object') continue;
      if (e.status === 'done') done.push(e);
      else active.push(e);
    }
    // Active: most-recent createdAt first.
    active.sort(function (a, b) {
      var ta = Date.parse(a.createdAt) || 0;
      var tb = Date.parse(b.createdAt) || 0;
      return tb - ta;
    });
    // Done: most-recent doneAt first.
    done.sort(function (a, b) {
      var ta = Date.parse(a.doneAt) || 0;
      var tb = Date.parse(b.doneAt) || 0;
      return tb - ta;
    });

    var counts = el('div', {
      class: 'task-history-counts',
      text: active.length + ' active · ' + done.length + ' done'
    });
    sectionEl.appendChild(counts);

    sectionEl.appendChild(buildSection(
      'Active',
      active,
      'No active tasks. Create one in Task Form.'
    ));

    sectionEl.appendChild(buildSection(
      'Done',
      done,
      'No tasks marked as done yet.'
    ));
  }

  App.panels['tasks-list'] = {
    mount: function (rootEl) {
      sectionEl = rootEl;
      // External writes (e.g. Task Form creating a new task while this panel
      // is the active route) should trigger a re-render. mount runs once per
      // page-load, so this subscription is registered exactly once.
      if (!storeUnsub) {
        storeUnsub = App.store.on('change', function () {
          if (sectionEl) render();
        });
      }
      render();
    },
    refresh: function () {
      render();
    }
  };
})();
