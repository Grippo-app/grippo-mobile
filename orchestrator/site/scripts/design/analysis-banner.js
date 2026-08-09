import { dom } from '../dom.js';
import { i18n } from '../i18n.js';
import { syncFailureText } from '../figma/sync-errors.js';

var el = dom.el;
function t(key, params) { return i18n.t(key, params); }
function date(value) {
  var parsed = Date.parse(value);
  return Number.isFinite(parsed) ? new Date(parsed).toLocaleString() : '';
}
var TOKEN_STATES = [
  'unavailable', 'adapters-unconfigured', 'adapters-invalid', 'mapping-invalid',
  'not-checked', 'stale-design', 'stale-mapping', 'stale-config', 'stale-project', 'current',
  'capture-incomplete', 'capture-inconsistent', 'capture-invalid', 'access-degraded', 'sync-failed',
  'source-health-unknown', 'source-refresh-required'
];
var SOURCE_FAILURE_STATES = {
  'capture-incomplete': 1,
  'capture-inconsistent': 1,
  'capture-invalid': 1,
  'access-degraded': 1,
  'sync-failed': 1,
  'source-refresh-required': 1
};
// States whose remedy is editing a project file; the banner names the exact path.
var TOKEN_STATE_PATHS = {
  'adapters-unconfigured': 'orchestrator/figma/project-adapters.json',
  'adapters-invalid': 'orchestrator/figma/project-adapters.json',
  'mapping-invalid': 'orchestrator/figma/token-mappings.json'
};
var TOKEN_BODY_KEYS = {
  'adapters-unconfigured': 'design.analysis.tokens.body.adapters-unconfigured',
  'adapters-invalid': 'design.analysis.tokens.body.adapters-invalid',
  'mapping-invalid': 'design.analysis.tokens.body.mapping-invalid',
  'capture-incomplete': 'design.analysis.tokens.body.capture-incomplete',
  'capture-inconsistent': 'design.analysis.tokens.body.capture-inconsistent',
  'capture-invalid': 'design.analysis.tokens.body.capture-invalid',
  'access-degraded': 'design.analysis.tokens.body.access-degraded',
  'sync-failed': 'design.analysis.tokens.body.sync-failed',
  'source-health-unknown': 'design.analysis.tokens.body.source-health-unknown',
  'source-refresh-required': 'design.analysis.tokens.body.source-refresh-required',
  'stale-design': 'design.analysis.tokens.body.stale-design',
  'stale-mapping': 'design.analysis.tokens.body.stale-mapping',
  'stale-config': 'design.analysis.tokens.body.stale-config',
  'stale-project': 'design.analysis.tokens.body.stale-project'
};
// A button inside the token domain always compares only tokens. Global
// orchestration is a separate explicit operation; a domain retry must never
// pull its sibling along as a hidden side effect (REQ-ARCH-006).
function tokensBanner(analysis, comparison, onCompare) {
  analysis = analysis || { state: 'unavailable' };
  comparison = comparison || { state: 'blocked' };
  var state = TOKEN_STATES.indexOf(analysis.state) >= 0 ? analysis.state : 'unavailable';
  var banner = el('section', {
    class: 'design-analysis design-analysis--tokens design-analysis--tok-' + state,
    attrs: { 'aria-label': t('design.analysis.aria.tokens') }
  });
  var copy = el('div', { class: 'design-analysis__copy' });
  copy.appendChild(el('strong', { text: t('design.analysis.tokens.title.' + state) }));
  copy.appendChild(el('p', { text: t(TOKEN_BODY_KEYS[state] || 'design.analysis.tokens.body') }));
  if (TOKEN_STATE_PATHS[state]) {
    copy.appendChild(el('code', { class: 'design-analysis__path', text: TOKEN_STATE_PATHS[state] }));
  }
  if (analysis.syncAttempt) {
    if (analysis.syncAttempt.finishedAt) {
      copy.appendChild(el('time', {
        text: t('design.analysis.syncAttemptAt', { date: date(analysis.syncAttempt.finishedAt) }),
        attrs: { datetime: analysis.syncAttempt.finishedAt, title: analysis.syncAttempt.finishedAt }
      }));
    }
    if (analysis.syncAttempt.errorCode) {
      copy.appendChild(el('p', {
        class: 'design-analysis__status', text: syncFailureText(analysis.syncAttempt.errorCode)
      }));
    }
  }
  var checkedAt = date(analysis.checkedAt);
  if (checkedAt && state === 'current') {
    copy.appendChild(el('time', {
      text: t('design.analysis.checkedAt', { date: checkedAt }),
      attrs: { datetime: analysis.checkedAt, title: analysis.checkedAt }
    }));
  }
  banner.appendChild(copy);

  var actions = el('div', { class: 'design-analysis__actions' });
  function compareButton(label, handler, className) {
    var button = el('button', {
      type: 'button', class: className || 'btn btn--primary',
      text: comparison.state === 'running' ? t('design.analysis.action.comparing') : label,
      disabled: comparison.state !== 'ready' || typeof handler !== 'function'
    });
    button.addEventListener('click', function () {
      if (comparison.state === 'ready' && typeof handler === 'function') handler(button);
    });
    actions.appendChild(button);
    if (comparison.state === 'blocked') {
      actions.appendChild(el('span', {
        class: 'design-analysis__status', text: t('design.analysis.compareBlocked')
      }));
    }
  }
  if (state === 'unavailable') {
    actions.appendChild(el('a', {
      class: 'btn btn--primary', href: '#figma?sync=tokens',
      text: t('design.analysis.action.sync.tokens')
    }));
  } else if (state === 'source-health-unknown' || state === 'source-refresh-required') {
    if (analysis.designSynced) {
      compareButton(t('design.analysis.action.compareSaved'), onCompare, 'btn btn--ghost');
    }
    actions.appendChild(el('a', {
      class: 'btn btn--primary', href: '#figma?sync=tokens',
      text: t('design.analysis.action.recover.tokens')
    }));
    actions.appendChild(el('a', {
      class: 'btn btn--ghost', href: '#figma',
      text: t('design.analysis.action.syncDetails')
    }));
  } else if (SOURCE_FAILURE_STATES[state]) {
    actions.appendChild(el('a', {
      class: 'btn btn--primary', href: '#figma?sync=tokens',
      text: t('design.analysis.action.retry.tokens')
    }));
    if (analysis.designSynced) {
      compareButton(t('design.analysis.action.compareSaved'), onCompare, 'btn btn--ghost');
    }
    actions.appendChild(el('a', {
      class: 'btn btn--ghost', href: '#figma',
      text: t('design.analysis.action.syncDetails')
    }));
  } else if (state === 'adapters-unconfigured' || state === 'adapters-invalid') {
    actions.appendChild(el('a', {
      class: 'btn btn--ghost', href: '#figma?sync=tokens',
      text: t('design.analysis.action.refresh.tokens')
    }));
  } else if (state === 'not-checked') {
    compareButton(t('design.analysis.action.compare'), onCompare);
  } else if (state === 'stale-design' || state === 'current') {
    compareButton(t('design.analysis.action.compareAgain'), onCompare);
  } else if (state === 'stale-mapping' || state === 'stale-config' || state === 'stale-project') {
    compareButton(t('design.analysis.action.compareAgain'), onCompare);
  }
  banner.appendChild(actions);
  return banner;
}
// Component analysis banner: same state machine as tokens, but every
// compare-able state routes to the component-domain compare endpoint (never
// the Figma integration when a design inventory exists).
var COMPONENT_STATES = TOKEN_STATES;
var COMPONENT_STATE_PATHS = {
  'adapters-unconfigured': 'orchestrator/figma/project-adapters.json',
  'adapters-invalid': 'orchestrator/figma/project-adapters.json',
  'mapping-invalid': 'orchestrator/figma/component-mappings.json'
};
var COMPONENT_BODY_KEYS = {
  'adapters-unconfigured': 'design.analysis.components.body.adapters-unconfigured',
  'adapters-invalid': 'design.analysis.components.body.adapters-invalid',
  'mapping-invalid': 'design.analysis.components.body.mapping-invalid',
  'capture-incomplete': 'design.analysis.components.body.capture-incomplete',
  'capture-inconsistent': 'design.analysis.components.body.capture-inconsistent',
  'capture-invalid': 'design.analysis.components.body.capture-invalid',
  'access-degraded': 'design.analysis.components.body.access-degraded',
  'sync-failed': 'design.analysis.components.body.sync-failed',
  'source-health-unknown': 'design.analysis.components.body.source-health-unknown',
  'source-refresh-required': 'design.analysis.components.body.source-refresh-required',
  'stale-design': 'design.analysis.components.body.stale-design',
  'stale-mapping': 'design.analysis.components.body.stale-mapping',
  'stale-config': 'design.analysis.components.body.stale-config',
  'stale-project': 'design.analysis.components.body.stale-project'
};
function componentsBanner(analysis, comparison, onCompare) {
  analysis = analysis || { state: 'unavailable' };
  comparison = comparison || { state: 'blocked' };
  var state = COMPONENT_STATES.indexOf(analysis.state) >= 0 ? analysis.state : 'unavailable';
  var banner = el('section', {
    class: 'design-analysis design-analysis--components design-analysis--cmp-' + state,
    attrs: { 'aria-label': t('design.analysis.aria.components') }
  });
  var copy = el('div', { class: 'design-analysis__copy' });
  copy.appendChild(el('strong', { text: t('design.analysis.components.title.' + state) }));
  copy.appendChild(el('p', { text: t(COMPONENT_BODY_KEYS[state] || 'design.analysis.components.body') }));
  if (COMPONENT_STATE_PATHS[state]) {
    copy.appendChild(el('code', { class: 'design-analysis__path', text: COMPONENT_STATE_PATHS[state] }));
  }
  if (analysis.syncAttempt) {
    if (analysis.syncAttempt.finishedAt) {
      copy.appendChild(el('time', {
        text: t('design.analysis.syncAttemptAt', { date: date(analysis.syncAttempt.finishedAt) }),
        attrs: { datetime: analysis.syncAttempt.finishedAt, title: analysis.syncAttempt.finishedAt }
      }));
    }
    if (analysis.syncAttempt.errorCode) {
      copy.appendChild(el('p', {
        class: 'design-analysis__status', text: syncFailureText(analysis.syncAttempt.errorCode)
      }));
    }
  }
  var checkedAt = date(analysis.checkedAt);
  if (checkedAt && state === 'current') {
    copy.appendChild(el('time', {
      text: t('design.analysis.checkedAt', { date: checkedAt }),
      attrs: { datetime: analysis.checkedAt, title: analysis.checkedAt }
    }));
  }
  banner.appendChild(copy);

  var actions = el('div', { class: 'design-analysis__actions' });
  function compareButton(label, className) {
    var button = el('button', {
      type: 'button', class: className || 'btn btn--primary',
      text: comparison.state === 'running' ? t('design.analysis.action.comparing') : label,
      disabled: comparison.state !== 'ready' || typeof onCompare !== 'function'
    });
    button.addEventListener('click', function () {
      if (comparison.state === 'ready' && typeof onCompare === 'function') onCompare(button);
    });
    actions.appendChild(button);
    if (comparison.state === 'blocked') {
      actions.appendChild(el('span', {
        class: 'design-analysis__status', text: t('design.analysis.compareBlocked')
      }));
    }
  }
  if (state === 'unavailable') {
    actions.appendChild(el('a', {
      class: 'btn btn--primary', href: '#figma?sync=components',
      text: t('design.analysis.action.sync.components')
    }));
  } else if (state === 'source-health-unknown' || state === 'source-refresh-required') {
    if (analysis.designSynced) {
      compareButton(t('design.analysis.action.compareSaved'), 'btn btn--ghost');
    }
    actions.appendChild(el('a', {
      class: 'btn btn--primary', href: '#figma?sync=tokens',
      text: t('design.analysis.action.recover.tokens')
    }));
  } else if (SOURCE_FAILURE_STATES[state]) {
    actions.appendChild(el('a', {
      class: 'btn btn--primary', href: '#figma?sync=components',
      text: t('design.analysis.action.retry.components')
    }));
    if (analysis.designSynced) {
      compareButton(t('design.analysis.action.compareSaved'), 'btn btn--ghost');
    }
    actions.appendChild(el('a', {
      class: 'btn btn--ghost', href: '#figma',
      text: t('design.analysis.action.syncDetails')
    }));
  } else if (state === 'adapters-unconfigured' || state === 'adapters-invalid') {
    actions.appendChild(el('a', {
      class: 'btn btn--ghost', href: '#figma?sync=components',
      text: t('design.analysis.action.refresh.components')
    }));
  } else if (state === 'not-checked') {
    compareButton(t('design.analysis.action.compare'));
  } else if (state !== 'mapping-invalid') {
    compareButton(t('design.analysis.action.compareAgain'));
  }
  banner.appendChild(actions);
  return banner;
}

function element(kind, analysis, comparison, onCompare) {
  return kind === 'tokens'
    ? tokensBanner(analysis, comparison, onCompare)
    : componentsBanner(analysis, comparison, onCompare);
}

export const designAnalysisBanner = { element: element };
