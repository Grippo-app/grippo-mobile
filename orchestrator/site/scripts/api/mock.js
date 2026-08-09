import { dom } from '../dom.js';
import { clipboard } from '../clipboard.js';

var el = dom.el;
function statusLine(ctx, mock) {
  if (!mock || mock.state === 'stopped') return ctx.t('api.mockServer.stopped');
  if (mock.state === 'starting') return ctx.t('api.mockServer.starting');
  if (mock.state === 'crashed') return ctx.t('api.mockServer.crashed');
  return mock.staleContract
    ? ctx.t('api.mockServer.runningStale', { port: mock.port })
    : ctx.t('api.mockServer.running', { port: mock.port });
}
function loadLogs(ctx, host, serverId) {
  ctx.get('/api/api-mock/logs?serverId=' + encodeURIComponent(serverId))
    .then(function (payload) {
      var list = el('ol', { class: 'api-mock-logs' });
      (payload.items || []).forEach(function (row) {
        list.appendChild(el('li', {
          text: row.method + ' ' + row.path + ' · ' + row.status + ' · ' + row.durationMs + ' ms'
        }));
      });
      if (!(payload.items || []).length) list.appendChild(el('li', {
        text: ctx.t('api.mockServer.noLogs')
      }));
      host.replaceChildren(el('p', {
        class: 'field-help',
        text: ctx.t('api.mockServer.logBinding', {
          environment: payload.environmentId,
          generation: payload.committedGenerationId,
          hash: payload.contractHash
        })
      }), list);
      if (payload.limitations && payload.limitations.length) {
        host.appendChild(el('p', {
          class: 'api-limitations',
          text: ctx.t('api.limitations', { count: payload.limitations.length })
        }));
      }
    }).catch(function (error) {
      host.textContent = ctx.errorMessage(error);
    });
}
function startMock(ctx, diagnostics, port) {
  return ctx.post('/api/api-mock/start', {
    expectedGenerationId: diagnostics.committedGenerationId,
    contractHash: diagnostics.contractHash,
    environmentId: diagnostics.environmentId,
    portMode: port === null ? 'auto' : 'explicit',
    port: port,
    idempotencyKey: ctx.randomKey('api.mock.start.')
  });
}
function renderState(ctx, host, diagnostics, status) {
  host.replaceChildren();
  var mock = status.mock || {};
  host.appendChild(el('p', {
    class: 'api-mock-status api-mock-status--' + (mock.state || 'stopped'),
    text: statusLine(ctx, mock)
  }));
  if (mock.url) host.appendChild(el('code', { text: mock.url }));
  var actions = el('div', { class: 'api-mock-actions' });
  var mismatch = diagnostics.environmentMismatch === true;
  if (mock.state === 'starting') {
    setTimeout(function () {
      if (!ctx.isCurrent()) return;
      ctx.get('/api/api-mock/status').then(function (next) {
        renderState(ctx, host, diagnostics, next);
      }, function (error) {
        // Swallowing this killed the only loop that resolves "starting…", and the
        // branch renders no Stop/Start/Logs controls — the panel wedged forever.
        if (!ctx.isCurrent()) return;
        host.appendChild(el('p', { class: 'api-context-warning', text: ctx.errorMessage(error) }));
        var again = el('button', {
          type: 'button', class: 'btn btn--small', text: ctx.t('api.retry')
        });
        again.addEventListener('click', function () {
          ctx.get('/api/api-mock/status').then(function (next) {
            renderState(ctx, host, diagnostics, next);
          }, function () {});
        });
        host.appendChild(again);
      });
    }, 500);
    return;
  }
  if (mock.state === 'running' || mock.canStop) {
    var stop = el('button', {
      type: 'button', class: 'btn btn--danger btn--small', text: ctx.t('api.mockServer.stop')
    });
    stop.addEventListener('click', function () {
      stop.disabled = true;
      ctx.post('/api/api-mock/stop', {
        serverId: mock.serverId,
        expectedStateRevision: mock.stateRevision,
        idempotencyKey: ctx.randomKey('api.mock.stop.')
      }).then(function () {
        clipboard.toast(ctx.t('api.mockServer.stoppedToast'));
        return ctx.get('/api/api-mock/status').then(function (next) {
          renderState(ctx, host, diagnostics, next);
        });
      }).catch(function (error) {
        stop.disabled = false; clipboard.toast(ctx.errorMessage(error));
      });
    });
    actions.appendChild(stop);
    if (mock.state === 'running' && mock.staleContract) {
      var restart = el('button', {
        type: 'button', class: 'btn btn--small', text: ctx.t('api.mockServer.restart'),
        disabled: mismatch || !diagnostics.committedGenerationId
      });
      restart.addEventListener('click', function () {
        restart.disabled = true;
        stop.disabled = true;
        ctx.post('/api/api-mock/stop', {
          serverId: mock.serverId,
          expectedStateRevision: mock.stateRevision,
          idempotencyKey: ctx.randomKey('api.mock.stop.')
        }).then(function () {
          return startMock(ctx, diagnostics, null);
        }).then(function () {
          clipboard.toast(ctx.t('api.mockServer.startedToast'));
          return ctx.get('/api/api-mock/status').then(function (next) {
            renderState(ctx, host, diagnostics, next);
          });
        }).catch(function (error) {
          restart.disabled = false;
          stop.disabled = false;
          clipboard.toast(ctx.errorMessage(error));
        });
      });
      actions.appendChild(restart);
    }
    var logs = el('button', {
      type: 'button', class: 'btn btn--ghost btn--small', text: ctx.t('api.mockServer.logs')
    });
    var logHost = el('div', { class: 'api-mock-log-host' });
    logs.addEventListener('click', function () { loadLogs(ctx, logHost, mock.serverId); });
    actions.appendChild(logs);
    host.appendChild(actions);
    host.appendChild(logHost);
    if (mismatch) host.appendChild(el('p', {
      class: 'api-context-warning',
      text: ctx.t('api.mockServer.environmentBlocked')
    }));
    return;
  }
  var port = el('input', {
    type: 'number', class: 'input api-mock-port', min: 1024, max: 65535,
    placeholder: ctx.t('api.mockServer.autoPort'),
    attrs: {
      'aria-label': ctx.t('api.mockServer.port'),
      'data-api-focus': 'mock-port'
    }
  });
  var start = el('button', {
    type: 'button', class: 'btn btn--small', text: ctx.t('api.mockServer.start'),
    disabled: mismatch || !diagnostics.committedGenerationId
  });
  start.addEventListener('click', function () {
    var parsed = port.value ? Number(port.value) : null;
    if (parsed !== null && (!Number.isInteger(parsed) || parsed < 1024 || parsed > 65535)) {
      clipboard.toast(ctx.t('api.mockServer.badPort')); return;
    }
    start.disabled = true;
    startMock(ctx, diagnostics, parsed).then(function () {
      clipboard.toast(ctx.t('api.mockServer.startedToast'));
      return ctx.get('/api/api-mock/status').then(function (next) {
        renderState(ctx, host, diagnostics, next);
      });
    }).catch(function (error) {
      start.disabled = false; clipboard.toast(ctx.errorMessage(error));
    });
  });
  actions.appendChild(port);
  actions.appendChild(start);
  host.appendChild(actions);
  if (mock.serverId) {
    var historyLogs = el('button', {
      type: 'button', class: 'btn btn--ghost btn--small', text: ctx.t('api.mockServer.logs')
    });
    var historyHost = el('div', { class: 'api-mock-log-host' });
    historyLogs.addEventListener('click', function () {
      loadLogs(ctx, historyHost, mock.serverId);
    });
    actions.appendChild(historyLogs);
    host.appendChild(historyHost);
  }
  if (mismatch) host.appendChild(el('p', {
    class: 'api-context-warning', text: ctx.t('api.mockServer.environmentBlocked')
  }));
}

export const apiMock = {
  render: function (ctx, root, diagnostics) {
    var old = root.querySelector('.api-mock-server');
    var section = el('section', { class: 'api-detail-section api-mock-server' });
    section.appendChild(el('h3', { class: 'panel-section-title', text: ctx.t('api.mockServer.title') }));
    var host = el('div');
    host.appendChild(el('p', { text: ctx.t('api.loading') }));
    section.appendChild(host);
    if (old) old.replaceWith(section); else root.appendChild(section);
    ctx.get('/api/api-mock/status').then(function (status) {
      renderState(ctx, host, diagnostics, status);
    }).catch(function (error) {
      host.textContent = ctx.errorMessage(error);
    });
  }
};
