import { dom } from '../dom.js';
import { taskActionPane } from './task-action-pane.js';
import { taskOverview } from './task-overview.js';
import {
  initialTaskDetailsSection,
  normalizeTaskDetailsSection,
  taskDetailsSections
} from './task-details-sections.js';
import { renderTaskActivity } from './task-activity.js';
import { renderTaskArtifacts } from './task-artifacts.js';
import { renderTaskAdvanced } from './task-advanced.js';
import { taskActionBar, taskDetailsOverflow } from './task-action-bar.js';
import { runTaskRetry } from './task-retry.js';

const el = dom.el;

export function createTaskDetails(details, options) {
  let overview = null;
  let actionPane = null;
  const sections = taskDetailsSections(details);
  const root = el('div', {
    class: 'board-modal__body task-details',
    attrs: { 'data-task-details-stem': details.identity.stem, 'data-task-details-revision': details.revision }
  });
  const header = el('header', { class: 'task-details__header' });
  const titleRow = el('div', { class: 'task-details__title-row' });
  const identity = el('div', { class: 'task-details__identity' });
  identity.appendChild(el('h3', {
    class: 'board-modal__title task-details__title',
    text: (details.identity.number ? '#' + details.identity.number + ' · ' : '') + details.identity.title
  }));
  identity.appendChild(el('code', {
    class: 'board-modal__stem task-details__stem',
    text: details.identity.stem
  }));
  titleRow.appendChild(identity);
  const badges = el('div', { class: 'task-details__badges' });
  badges.appendChild(el('span', {
    class: 'task-details__badge task-details__badge--state',
    text: options.t('taskDetails.state.' + details.state.display.replace(/-/g, '_'))
  }));
  badges.appendChild(el('span', {
    class: 'task-details__badge',
    text: options.t('board.origin.' + details.origin.kind.replace(/-/g, '_'))
  }));
  titleRow.appendChild(badges);
  titleRow.appendChild(taskDetailsOverflow(details, {
    t: options.t,
    onAction: function (kind, action) {
      let input = null;
      if (kind === 'copy-prompt' && details.primaryAction &&
          details.primaryAction.kind === 'submit-answers' && actionPane && actionPane.questions) {
        const answers = actionPane.questions.read();
        if (!answers) {
          options.onError({ kind: 'task-answer-invalid' });
          return;
        }
        input = {
          answers: answers,
          questionRound: actionPane.questions.round,
          expectedQuestionsRevision: actionPane.questions.revision
        };
      }
      options.onOverflowAction(kind, action, input);
    }
  }));
  header.appendChild(titleRow);
  root.appendChild(header);

  overview = taskOverview(details, options);
  actionPane = taskActionPane(details, options);
  const operationalFacts = overview.node.querySelector('.task-details__facts');
  if (operationalFacts) header.appendChild(operationalFacts);
  const panes = {
    overview: overview.node,
    activity: el('div', {
      class: 'task-details__lazy', text: options.t('taskDetails.loading'),
      attrs: { role: 'status', 'aria-live': 'polite' }
    }),
    artifacts: el('div', {
      class: 'task-details__lazy', text: options.t('taskDetails.loading'),
      attrs: { role: 'status', 'aria-live': 'polite' }
    }),
    advanced: el('div', {
      class: 'task-details__lazy', text: options.t('taskDetails.loading'),
      attrs: { role: 'status', 'aria-live': 'polite' }
    })
  };
  if (actionPane) panes.action = actionPane.node;
  const loaded = { action: !!actionPane, overview: true, activity: false, artifacts: false, advanced: false };
  const loading = { activity: false, artifacts: false, advanced: false };
  const pages = { activity: null, artifacts: null, advanced: null };
  const tabs = el('div', {
    class: 'board-modal__tabs task-details__tabs',
    attrs: { role: 'tablist', 'aria-label': options.t('taskDetails.tabsLabel') }
  });
  const paneWrap = el('div', { class: 'task-details__panes' });

  function mergedPage(section, current, next) {
    if (!current) return next;
    if (section === 'activity') {
      const mergedGroups = [];
      const byId = Object.create(null);
      (current.groups || []).concat(next.groups || []).forEach(function (group) {
        const prior = byId[group.id];
        if (!prior) {
          const copy = Object.assign({}, group, {
            children: (group.children || []).slice(),
            events: (group.events || []).slice()
          });
          byId[group.id] = copy;
          mergedGroups.push(copy);
          return;
        }
        prior.status = group.status === 'info' ? prior.status : group.status;
        prior.startedAt = prior.startedAt || group.startedAt;
        prior.endedAt = group.endedAt || prior.endedAt;
        prior.durationMs = group.durationMs == null ? prior.durationMs : group.durationMs;
        prior.retryCount = Math.max(prior.retryCount || 0, group.retryCount || 0);
        prior.stopReason = group.stopReason || prior.stopReason;
        prior.checkpointId = group.checkpointId || prior.checkpointId;
        prior.reportId = group.reportId || prior.reportId;
        prior.children = Array.from(new Set(
          (prior.children || []).concat(group.children || [])
        ));
        prior.events = (prior.events || []).concat(group.events || []);
      });
      return Object.assign({}, next, {
        summary: next.summary || current.summary,
        outcomeDigest: current.outcomeDigest || next.outcomeDigest,
        events: (current.events || []).concat(next.events || []),
        groups: mergedGroups,
        partial: current.partial || next.partial,
        limitations: Array.from(new Set(
          (current.limitations || []).concat(next.limitations || [])
        )).sort()
      });
    }
    if (section === 'artifacts') return Object.assign({}, next, {
      groups: current.groups || next.groups,
      artifacts: (current.artifacts || []).concat(next.artifacts || []),
      partial: current.partial || next.partial,
      limitations: Array.from(new Set(
        (current.limitations || []).concat(next.limitations || [])
      )).sort()
    });
    return next;
  }

  function paint(section, page) {
    pages[section] = page;
    const renderOptions = Object.assign({}, options, {
      loadMore: page.nextCursor ? function (cursor, button) {
        if (loading[section]) return;
        loading[section] = true;
        button.disabled = true;
        const loader = section === 'activity' ? options.loadActivity : options.loadArtifacts;
        loader(cursor).then(function (next) {
          if (!root.isConnected) return;
          loading[section] = false;
          paint(section, mergedPage(section, pages[section], next));
        }, function (error) {
          loading[section] = false;
          button.disabled = false;
          options.onError(error);
        });
      } : null
    });
    if (section === 'activity') renderTaskActivity(panes.activity, page, renderOptions);
    if (section === 'artifacts') renderTaskArtifacts(panes.artifacts, page, Object.assign({}, renderOptions, {
      extraNode: options.artifactExtraNode || null
    }));
    if (section === 'advanced') renderTaskAdvanced(panes.advanced, page, renderOptions);
  }

  function load(section) {
    if (loaded[section] || loading[section]) return;
    loading[section] = true;
    const loader = section === 'activity' ? options.loadActivity :
      section === 'artifacts' ? options.loadArtifacts : options.loadAdvanced;
    loader().then(function (page) {
      if (!root.isConnected) return;
      paint(section, page);
      const focusId = section === 'artifacts' ? options.focusArtifactId :
        section === 'advanced' ? options.focusCheckpointId : null;
      if (focusId) {
        const attr = section === 'artifacts' ? 'data-artifact-id' : 'data-checkpoint-id';
        const candidate = Array.from(panes[section].querySelectorAll('[' + attr + ']')).find(function (node) {
          return node.getAttribute(attr) === focusId;
        });
        if (candidate) {
          candidate.classList.add('task-details__deep-target');
          candidate.scrollIntoView({ block: 'nearest' });
          try { candidate.focus({ preventScroll: true }); }
          catch (_) { candidate.focus(); }
        }
      }
      loaded[section] = true;
      loading[section] = false;
    }, function (error) {
      if (!root.isConnected) return;
      while (panes[section].firstChild) panes[section].removeChild(panes[section].firstChild);
      panes[section].appendChild(el('p', {
        class: 'banner banner--warn',
        text: options.errorText(error),
        attrs: { role: 'alert' }
      }));
      const retry = el('button', {
        type: 'button',
        class: 'btn btn--sm task-details__load-more',
        text: options.t('taskDetails.reload')
      });
      retry.addEventListener('click', function () {
        while (panes[section].firstChild) panes[section].removeChild(panes[section].firstChild);
        panes[section].appendChild(el('p', {
          class: 'task-details__lazy',
          text: options.t('taskDetails.loading'),
          attrs: { role: 'status', 'aria-live': 'polite' }
        }));
        load(section);
      });
      panes[section].appendChild(retry);
      loading[section] = false;
    });
  }

  let selectedSection = 'overview';
  function select(section, focus) {
    section = normalizeTaskDetailsSection(details, section);
    selectedSection = section;
    sections.forEach(function (name) {
      const on = name === section;
      panes[name].hidden = !on;
      panes[name].setAttribute('aria-hidden', on ? 'false' : 'true');
      const tab = tabs.querySelector('[data-task-details-tab="' + name + '"]');
      tab.setAttribute('aria-selected', on ? 'true' : 'false');
      tab.setAttribute('tabindex', on ? '0' : '-1');
      tab.classList.toggle('board-modal__tab--active', on);
      if (on && focus) tab.focus();
    });
    load(section);
  }

  sections.forEach(function (section, index) {
    const tab = el('button', {
      type: 'button',
      class: 'board-modal__tab',
      id: 'task-details-tab-' + section,
      text: options.t('taskDetails.tab.' + section),
      attrs: {
        role: 'tab',
        'data-task-details-tab': section,
        'aria-controls': 'task-details-pane-' + section,
        'aria-selected': 'false',
        tabindex: '-1'
      }
    });
    tab.addEventListener('click', function () { select(section, false); });
    tab.addEventListener('keydown', function (event) {
      if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight' && event.key !== 'Home' && event.key !== 'End') return;
      event.preventDefault();
      let target = event.key === 'Home' ? 0 : event.key === 'End' ? sections.length - 1 :
        (index + (event.key === 'ArrowRight' ? 1 : -1) + sections.length) % sections.length;
      select(sections[target], true);
    });
    tabs.appendChild(tab);
    panes[section].classList.add('board-modal__tabpane', 'task-details__pane');
    panes[section].id = 'task-details-pane-' + section;
    panes[section].setAttribute('role', 'tabpanel');
    panes[section].setAttribute('aria-labelledby', 'task-details-tab-' + section);
    paneWrap.appendChild(panes[section]);
  });
  root.appendChild(tabs);
  root.appendChild(paneWrap);

  function primary(action, button) {
    if (action.kind === 'retry-phase') {
      button.disabled = true;
      options.loadCheckpoints().then(function (checkpoints) {
        return runTaskRetry({
          action: action,
          checkpoints: checkpoints,
          preview: function (current, checkpointHash) {
            return options.previewRetry(current, checkpointHash);
          },
          execute: function (current, input) { return options.onExecute(current, button, input); },
          confirm: options.confirm,
          t: options.t
        });
      }).then(options.onExecuted, function (error) {
        button.disabled = false;
        button.removeAttribute('aria-busy');
        if (!error || error.kind !== 'cancelled') options.onError(error);
      });
      return;
    }
    let input = {};
    if (action.kind === 'submit-answers') {
      if (!actionPane || !actionPane.questions) {
        options.onError({ kind: 'task-answer-invalid' });
        return;
      }
      input = {
        answers: actionPane.questions.read(),
        questionRound: actionPane.questions.round,
        expectedQuestionsRevision: actionPane.questions.revision
      };
      if (!input.answers) {
        options.onError({ kind: 'task-answer-invalid' });
        return;
      }
    }
    if (action.kind === 'continue-live') {
      if (!actionPane || !actionPane.liveAnswer) {
        options.onError({ kind: 'task-answer-invalid' });
        return;
      }
      input = actionPane.liveAnswer.read();
      if (!input) {
        options.onError({ kind: 'task-answer-invalid' });
        return;
      }
    }
    options.onExecute(action, button, input).then(function (response) {
      // The caller sets aria-busy before the request; leaving it set reports the
      // control as busy forever to assistive tech once the action has settled.
      button.removeAttribute('aria-busy');
      options.onExecuted(response);
    }, function (error) {
      button.disabled = false;
      button.removeAttribute('aria-busy');
      options.onError(error);
    });
  }

  root.appendChild(taskActionBar(details, {
    t: options.t,
    onPrimary: primary,
    onClose: options.onClose
  }));
  selectedSection = initialTaskDetailsSection(details, options.preferredSection);
  select(selectedSection, false);
  function focusCurrentWork() {
    const target = root.querySelector('[data-task-section="questions"] input, [data-task-section="questions"] textarea');
    if (!target || selectedSection !== 'action') return;
    try { target.focus({ preventScroll: true }); }
    catch (_) { target.focus(); }
  }
  function focusPreferred(preferredSection) {
    if ((preferredSection == null || preferredSection === 'questions' || preferredSection === 'action') &&
        selectedSection === 'action') {
      focusCurrentWork();
      return;
    }
    const targetSection = preferredSection === 'dependencies' ? 'dependencies' : null;
    if (!targetSection) return;
    const target = root.querySelector('[data-task-section="' + targetSection + '"]');
    if (!target) return;
    target.scrollIntoView({ block: 'start' });
    try { target.focus({ preventScroll: true }); }
    catch (_) { target.focus(); }
  }
  return {
    node: root,
    select: function (section, focus) {
      selectedSection = normalizeTaskDetailsSection(details, section);
      select(selectedSection, focus);
    },
    focusCurrentWork: focusCurrentWork,
    focusPreferred: focusPreferred
  };
}
