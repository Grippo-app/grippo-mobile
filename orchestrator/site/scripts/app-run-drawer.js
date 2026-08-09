import { dom } from './dom.js';
import { i18n } from './i18n.js';
import { appRunErrorMessage } from './app-run-errors.js';

var el = dom.el;
function t(key, params) { return i18n.t(key, params); }
function stateLabel(state) {
  var key = 'appRun.state.' + state;
  var value = t(key);
  return value === key ? t('appRun.state.unknown') : value;
}
function durationLabel(milliseconds) {
  return new Intl.NumberFormat(undefined, {
    style: 'unit', unit: 'second', unitDisplay: 'short', maximumFractionDigits: 0
  }).format(Math.max(0, Math.round(milliseconds / 1000)));
}
function stageTiming(stage) {
  var values = [];
  if (stage.startedAt) values.push(new Date(stage.startedAt).toLocaleTimeString());
  if (stage.durationMs !== null) values.push(durationLabel(stage.durationMs));
  return values.join(' · ');
}

function render(options) {
  var status = options.status || {};
  var job = status.job, session = status.session;
  var drawer = el('aside', {
    class: 'app-run-drawer',
    attrs: { role: 'dialog', 'aria-modal': 'false', 'aria-label': t('appRun.drawer') }
  });
  var close = el('button', { type: 'button', class: 'btn btn--ghost btn--small', text: t('appRun.close') });
  close.addEventListener('click', options.onClose);
  drawer.appendChild(el('div', { class: 'app-run-drawer__head' }, [
    el('h2', { text: job && job.state === 'running' ? t('appRun.running') : t('appRun.drawer') }),
    close
  ]));
  if (status.integrity && !status.integrity.ok) {
    drawer.appendChild(el('p', { class: 'banner banner--warn', text: t('appRun.recoveryRequired') }));
  }
  if (!job) {
    drawer.appendChild(el('p', { class: 'app-run-hint', text: t('appRun.noRuns') }));
    return drawer;
  }
  drawer.appendChild(el('p', {
    class: 'app-run-summary',
    text: [job.platform, job.variantId, stateLabel(job.state)].filter(Boolean).join(' · ')
  }));
  if (job.stages && job.stages.length) {
    var stages = el('ol', { class: 'app-run-stages' });
    job.stages.forEach(function (stage) {
      stages.appendChild(el('li', { class: 'app-run-stage app-run-stage--' + stage.status }, [
        el('span', { class: 'app-run-stage__mark', text: stage.status === 'success' ? '✓' : stage.status === 'failed' ? '!' : stage.status === 'running' ? '●' : '○' }),
        el('span', {
          class: 'app-run-stage__label',
          text: t('appRun.stage.' + stage.id) === 'appRun.stage.' + stage.id
            ? t('appRun.state.unknown') : t('appRun.stage.' + stage.id)
        }),
        stage.startedAt ? el('time', {
          attrs: { datetime: stage.startedAt },
          text: stageTiming(stage)
        }) : null,
        el('small', { text: t('appRun.stageStatus.' + stage.status) })
      ]));
    });
    drawer.appendChild(stages);
  }
  if (job.errorCode) {
    drawer.appendChild(el('p', {
      class: 'banner banner--warn',
      text: appRunErrorMessage({ kind: job.errorCode })
    }));
  }
  if (session) {
    drawer.appendChild(el('dl', { class: 'app-run-session' }, [
      el('dt', { text: t('appRun.device') }), el('dd', { text: session.deviceSummary }),
      el('dt', { text: t('appRun.application') }), el('dd', { text: session.applicationId }),
      el('dt', { text: t('appRun.source') }), el('dd', { text: session.sourceState === 'current' ? t('appRun.current') : t('appRun.sourceChanged') }),
      el('dt', { text: t('appRun.sourceRevision') }), el('dd', {
        attrs: { title: session.appProjectSourceRevision },
        text: session.appProjectSourceRevision.slice(0, 19) + '…'
      }),
      el('dt', { text: t('appRun.started') }), el('dd', { text: new Date(session.launchedAt).toLocaleString() })
    ]));
  }
  var actions = el('div', { class: 'app-run-drawer__actions' });
  // Every request-firing action hands its own button to the handler, which holds it
  // disabled until the request settles; two clicks otherwise race one another and
  // come back as a state conflict the user never caused.
  if (status.actions && status.actions.canCancel) {
    var cancel = el('button', { type: 'button', class: 'btn', text: t('appRun.cancel') });
    cancel.addEventListener('click', function () { options.onCancel(cancel); });
    actions.appendChild(cancel);
  }
  if (job) {
    var logs = el('button', { type: 'button', class: 'btn', text: t('appRun.logs') });
    logs.addEventListener('click', options.onLogs); actions.appendChild(logs);
  }
  if (status.actions && status.actions.canStop) {
    var stop = el('button', { type: 'button', class: 'btn btn--danger', text: t('appRun.stop') });
    stop.addEventListener('click', function () { options.onStop(stop); });
    actions.appendChild(stop);
  }
  if (status.actions && status.actions.canRestart) {
    var restart = el('button', { type: 'button', class: 'btn', text: t('appRun.restart') });
    restart.addEventListener('click', function () { options.onRestart(restart); });
    actions.appendChild(restart);
  }
  if (status.actions && status.actions.canScreenshot) {
    var shot = el('button', { type: 'button', class: 'btn', text: t('appRun.screenshot') });
    shot.addEventListener('click', function () { options.onScreenshot(shot); });
    actions.appendChild(shot);
  }
  if (session && session.taskStem && status.integrity && status.integrity.ok) {
    var validate = el('button', { type: 'button', class: 'btn btn--primary', text: t('appRun.validateTask') });
    validate.addEventListener('click', options.onValidate); actions.appendChild(validate);
  }
  drawer.appendChild(actions);
  if (options.screenshotUrl) {
    drawer.appendChild(el('a', { href: options.screenshotUrl, target: '_blank', rel: 'noopener', text: t('appRun.openScreenshot') }));
  }
  return drawer;
}

export const appRunDrawer = { render: render };
