import { dom } from '../dom.js';
import { clipboard } from '../clipboard.js';
import { apiMock } from './mock.js';

var el = dom.el;
function reportRow(ctx, name, report) {
  var row = el('li', { class: 'api-diagnostics-report' });
  row.appendChild(el('strong', { text: ctx.t('api.report.' + name) }));
  row.appendChild(el('span', {
    class: 'api-badge api-badge--' + String(report.state || 'unknown').replace(/[^a-z0-9-]/g, ''),
    text: ctx.t('api.reportState.' + (report.current ? 'current' : report.state || 'unknown'))
  }));
  if (report.generatedAt) row.appendChild(el('time', { text: report.generatedAt }));
  if (report.analyzerVersion) row.appendChild(el('code', { text: report.analyzerVersion }));
  if (report.analysisStatus) row.appendChild(el('span', {
    text: ctx.t('api.analysis.' + report.analysisStatus)
  }));
  return row;
}

export const apiDiagnostics = {
  load: function (ctx) {
    var sourceId = /^api:mismatch:mismatch-[a-f0-9]{24}$/.test(ctx.state.query || '')
      ? ctx.state.query : '';
    return ctx.get('/api/api/diagnostics' + (sourceId
      ? '?sourceId=' + encodeURIComponent(sourceId) : ''));
  },
  render: function (ctx, payload, toolbar, content) {
    toolbar.replaceChildren();
    content.replaceChildren();
    content.appendChild(el('p', {
      class: 'api-diagnostics-summary',
      text: ctx.t('api.diagnostics.' + (payload.summary || 'partial'))
    }));
    var copyDiagnostics = el('button', {
      type: 'button', class: 'btn btn--ghost btn--small',
      text: ctx.t('api.diagnostics.copy')
    });
    copyDiagnostics.addEventListener('click', function () {
      clipboard.copy(JSON.stringify({
        summary: payload.summary || null,
        activeEnvironment: payload.activeEnvironment || null,
        snapshotEnvironmentId: payload.snapshotEnvironmentId || null,
        reports: payload.reports || {},
        limitations: payload.limitations || [],
        reportPaths: payload.reportPaths || []
      }, null, 2));
    });
    content.appendChild(copyDiagnostics);
    var reports = el('section', { class: 'api-detail-section' });
    reports.appendChild(el('h3', { class: 'panel-section-title', text: ctx.t('api.diagnostics.reports') }));
    var list = el('ul', { class: 'api-diagnostics-list' });
    Object.keys(payload.reports || {}).forEach(function (name) {
      list.appendChild(reportRow(ctx, name, payload.reports[name]));
    });
    reports.appendChild(list);
    content.appendChild(reports);
    var mismatchData = payload.observedMismatches || { items: [], total: 0 };
    var mismatches = el('section', { class: 'api-detail-section' });
    mismatches.appendChild(el('h3', {
      class: 'panel-section-title',
      text: ctx.t('api.diagnostics.mismatches')
    }));
    (mismatchData.items || []).forEach(function (finding) {
      var article = el('article', { class: 'api-detail-finding' });
      var check = el('input', {
        type: 'checkbox',
        class: 'choice-input',
        checked: ctx.selected.has(finding.sourceId),
        attrs: {
          'aria-label': ctx.t('api.selection.item', { item: finding.id })
        }
      });
      check.addEventListener('change', function () {
        if (!ctx.toggleSource(finding.sourceId, check.checked)) check.checked = false;
      });
      article.appendChild(check);
      article.appendChild(el('span', {
        class: 'api-badge api-badge--' + finding.severity,
        text: ctx.driftSeverity(finding.severity)
      }));
      article.appendChild(el('strong', {
        text: ctx.driftFindingMessage(finding.kind)
      }));
      article.appendChild(el('p', {
        class: 'field-help',
        text: ctx.driftFindingSuggestion(finding.kind)
      }));
      var relation = [
        finding.operationId, finding.modelId, finding.file
      ].filter(Boolean).join(' · ');
      if (relation) article.appendChild(el('code', { text: relation }));
      if (finding.task) {
        article.appendChild(el('a', {
          class: 'api-task-chip',
          href: '#board?task=' + encodeURIComponent(finding.task.stem),
          text: ctx.t('api.task.open')
        }));
      }
      mismatches.appendChild(article);
    });
    if (!(mismatchData.items || []).length) mismatches.appendChild(el('p', {
      class: 'field-help', text: ctx.t('api.detail.noMismatches')
    }));
    if (mismatchData.truncated) mismatches.appendChild(el('p', {
      class: 'api-limitations',
      text: ctx.t('api.diagnostics.mismatchesTruncated', {
        shown: mismatchData.items.length,
        total: mismatchData.total
      })
    }));
    content.appendChild(mismatches);
    var commands = el('section', { class: 'api-detail-section' });
    commands.appendChild(el('h3', { class: 'panel-section-title', text: ctx.t('api.diagnostics.commands') }));
    (payload.commands || []).forEach(function (command) {
      var line = el('div', { class: 'api-command-line' });
      line.appendChild(el('code', { text: command }));
      var copy = el('button', {
        type: 'button', class: 'btn btn--ghost btn--small', text: ctx.t('api.mock.copy')
      });
      copy.addEventListener('click', function () { clipboard.copy(command); });
      line.appendChild(copy);
      commands.appendChild(line);
    });
    content.appendChild(commands);
    if ((payload.reportPaths || []).length) {
      var paths = el('section', { class: 'api-detail-section' });
      paths.appendChild(el('h3', {
        class: 'panel-section-title', text: ctx.t('api.diagnostics.paths')
      }));
      (payload.reportPaths || []).forEach(function (value) {
        paths.appendChild(el('code', { text: value }));
      });
      content.appendChild(paths);
    }
    if (payload.limitations && payload.limitations.length) {
      var limitations = el('section', { class: 'api-detail-section' });
      limitations.appendChild(el('h3', {
        class: 'panel-section-title', text: ctx.t('api.diagnostics.limitations')
      }));
      var limitationList = el('ul');
      var messages = payload.limitations.map(function (value) {
        return ctx.limitationMessage(value);
      }).filter(function (value, index, rows) {
        return rows.indexOf(value) === index;
      });
      messages.forEach(function (value) {
        limitationList.appendChild(el('li', { text: value }));
      });
      limitations.appendChild(limitationList);
      content.appendChild(limitations);
    }
    apiMock.render(ctx, content, payload);
  }
};
