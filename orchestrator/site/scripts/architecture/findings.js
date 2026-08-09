import { dom } from '../dom.js';

var el = dom.el;

function evidenceReason(t, code) {
  var key = 'archmap.evidenceReason.' + code;
  var translated = t(key);
  return translated === key ? t('archmap.evidenceReason.unknown') : translated;
}

export function renderArchitectureFindings(host, response, options) {
  var t = options.t;
  if (!response || !response.rows || !response.rows.length) {
    host.appendChild(el('div', {
      class: 'architecture-empty',
      text: response && response.analysisStatus === 'partial'
        ? t('archmap.noFindingsPartial') : t('archmap.noFindings')
    }));
    return;
  }
  var list = el('div', { class: 'architecture-findings', attrs: { role: 'list' } });
  response.rows.forEach(function (finding) {
    var card = el('article', {
      class: 'architecture-finding architecture-finding--' + finding.severity,
      attrs: { role: 'listitem' }
    });
    var head = el('div', { class: 'architecture-finding__head' });
    head.appendChild(el('span', {
      class: 'architecture-severity',
      text: t('archmap.severity.' + finding.severity)
    }));
    head.appendChild(el('h3', { text: finding.title }));
    card.appendChild(head);
    card.appendChild(el('p', { text: finding.summary }));
    var meta = el('p', {
      class: 'architecture-finding__meta',
      text: t('archmap.findingMeta', {
        type: t('archmap.findingType.' + finding.type),
        confidence: t('archmap.confidence.' + finding.confidence),
        rule: finding.ruleId
      })
    });
    card.appendChild(meta);
    var actions = el('div', { class: 'architecture-finding__actions' });
    var inspect = el('button', {
      type: 'button', class: 'btn btn--small', text: t('archmap.inspect'),
      attrs: { 'data-architecture-control': 'finding-inspect-' + finding.id }
    });
    inspect.addEventListener('click', function () {
      options.onSelect(finding.affectedNodeIds[0]);
    });
    actions.appendChild(inspect);
    var task = el('button', {
      type: 'button', class: 'btn btn--small',
      text: finding.linkedTask
        ? t('archmap.openTask')
        : options.taskCreationPending[finding.id]
          ? t('archmap.creatingTask') : t('archmap.createTask'),
      disabled: !finding.linkedTask && (
        !options.taskCreationEnabled || !!options.taskCreationPending[finding.id]
      ),
      attrs: {
        title: !finding.linkedTask && !options.taskCreationEnabled
          ? t('archmap.taskUnavailable') : '',
        'data-architecture-control': 'finding-task-' + finding.id
      }
    });
    task.addEventListener('click', function () {
      if (finding.linkedTask) options.onOpenTask(finding.linkedTask.stem);
      else options.onCreateTask(finding);
    });
    actions.appendChild(task);
    card.appendChild(actions);
    var evidence = el('details', { class: 'architecture-finding__evidence' });
    evidence.appendChild(el('summary', {
      text: t('archmap.evidence', { count: finding.evidence.length })
    }));
    var evidenceList = el('ul');
    finding.evidence.forEach(function (row) {
      evidenceList.appendChild(el('li', {
        text: row.sourcePath + (row.line ? ':' + row.line : '') + ' — ' +
          evidenceReason(t, row.reasonCode)
      }));
    });
    evidence.appendChild(evidenceList);
    card.appendChild(evidence);
    list.appendChild(card);
  });
  host.appendChild(list);
  if (response.nextCursor) {
    var more = el('button', {
      type: 'button', class: 'btn architecture-load-more',
      text: t('archmap.loadMore')
    });
    more.addEventListener('click', function () { options.onMore(response.nextCursor); });
    host.appendChild(more);
  }
}
