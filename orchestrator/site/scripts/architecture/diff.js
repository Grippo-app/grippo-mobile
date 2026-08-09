import { dom } from '../dom.js';

var el = dom.el;

export function renderArchitectureDiff(host, response, options) {
  var t = options.t;
  if (response && response.error) {
    host.appendChild(el('div', {
      class: 'banner banner--warn',
      text: t('archmap.diffUnavailable')
    }));
    return;
  }
  if (!response || !response.present || !response.diff) return;
  var diff = response.diff;
  var changes = diff.changes;
  var totals = diff.changeTotals || {};
  var total = ['nodesAdded', 'nodesRemoved', 'edgesAdded', 'edgesRemoved',
    'findingsIntroduced', 'findingsResolved', 'ownershipChanges']
    .reduce(function (count, key) {
      return count + (Number.isInteger(totals[key]) ? totals[key] : (changes[key] || []).length);
    }, 0);
  var details = el('details', { class: 'architecture-diff' });
  var label = diff.baselineCreated
    ? t('archmap.diff.baseline')
    : t('archmap.diff.summary', { count: total });
  details.appendChild(el('summary', { text: label }));
  var meta = [
    diff.taskStem || diff.triggerId,
    diff.createdAt,
    diff.followedByChanges ? t('archmap.diff.followedByChanges') : null
  ].filter(Boolean).join(' · ');
  details.appendChild(el('p', { class: 'architecture-diff__meta', text: meta }));
  if (diff.truncated) {
    details.appendChild(el('p', {
      class: 'architecture-diff__meta',
      text: t('archmap.diff.truncated')
    }));
  }
  [
    ['nodesAdded', 'archmap.diff.nodesAdded'],
    ['nodesRemoved', 'archmap.diff.nodesRemoved'],
    ['edgesAdded', 'archmap.diff.edgesAdded'],
    ['edgesRemoved', 'archmap.diff.edgesRemoved'],
    ['findingsIntroduced', 'archmap.diff.findingsIntroduced'],
    ['findingsResolved', 'archmap.diff.findingsResolved'],
    ['ownershipChanges', 'archmap.diff.ownershipChanges']
  ].forEach(function (row) {
    var count = Number.isInteger(totals[row[0]])
      ? totals[row[0]] : (changes[row[0]] || []).length;
    if (!count) return;
    details.appendChild(el('p', { text: t(row[1], { count: count }) }));
  });
  host.appendChild(details);
}
