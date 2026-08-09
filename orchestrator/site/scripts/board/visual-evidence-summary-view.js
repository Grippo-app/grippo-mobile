export function createVisualEvidenceSummaryView(dependencies) {
  var el = dependencies.el;
  var t = dependencies.t;
  var finalVisualDisplayState = dependencies.finalVisualDisplayState;
  var evidenceStatusClass = dependencies.evidenceStatusClass;
  var buildEvidenceVisuals = dependencies.buildEvidenceVisuals;
  var buildEvidenceBadge = dependencies.buildEvidenceBadge;
  var buildCause = dependencies.buildCause;
  var buildReviewPanel = dependencies.buildReviewPanel;
  var buildRerun = dependencies.buildRerun;

  function render(bodyEl, resp, opts) {
    opts = opts || {};
    while (bodyEl.firstChild) bodyEl.removeChild(bodyEl.firstChild);
    var ranFinal = !!(resp && resp.present !== false && resp.stage === 'final');
    if (ranFinal) {
      var ready = resp.evidenceState === 'READY';
      var vis = buildEvidenceVisuals(resp, resp.stem || '', {
        trust: finalVisualDisplayState(resp),
        stem: opts.stem,
        promoteScore: true,
        groupByStatus: true,
        filterValue: opts.filterValue,
        onFilterInput: opts.onFilterInput,
        buildVisualActions: opts.buildVisualActions
      });
      if (!ready) {
        var head = el('div', { class: 'board-evidence__result board-evidence__result--problem' });
        var headStatus = evidenceStatusClass(resp.overall) === 'pass'
          ? 'INCOMPLETE'
          : (resp.overall || 'INCOMPLETE');
        head.appendChild(buildEvidenceBadge(headStatus));
        head.appendChild(el('span', {
          class: 'board-evidence__result-text',
          text: t('board.figmaEvidence.resultProblem')
        }));
        bodyEl.appendChild(head);
        var cause = buildCause(resp);
        if (cause) bodyEl.appendChild(cause);
        var review = buildReviewPanel(opts.stem || resp.stem || '', resp);
        if (review) bodyEl.appendChild(review);
        var rerun = buildRerun(opts.stem || resp.stem || '', resp);
        if (rerun) bodyEl.appendChild(rerun);
        if (vis) {
          vis.open = true;
          bodyEl.appendChild(vis);
        } else {
          bodyEl.appendChild(el('p', {
            class: 'board-evidence__empty',
            text: t('board.figmaEvidence.detailsUnavailable')
          }));
        }
        return;
      }

      var readyReview = buildReviewPanel(opts.stem || resp.stem || '', resp);
      if (readyReview) bodyEl.appendChild(readyReview);
      if (evidenceStatusClass(resp.overall) === 'warn') {
        bodyEl.appendChild(el('p', {
          class: 'board-evidence__warn-accepted',
          text: t('board.figmaEvidence.warnAccepted')
        }));
      }
      var inputDriftNames = [];
      var inputUnavailableNames = [];
      (Array.isArray(resp.inputDriftReports) ? resp.inputDriftReports : []).forEach(function (drift) {
        var name = drift && drift.name;
        var unavailable = drift && [
          'missing',
          'missing-input-hash',
          'input-hash-untrusted-or-missing',
          'design-source-hash-missing',
          'design-source-unavailable'
        ].indexOf(drift.reason) >= 0;
        var target = unavailable ? inputUnavailableNames : inputDriftNames;
        if (name && target.indexOf(name) < 0) target.push(name);
      });
      if (inputDriftNames.length) {
        bodyEl.appendChild(el('p', {
          class: 'board-evidence__input-drift',
          text: t('board.figmaEvidence.inputDriftReady', { names: inputDriftNames.join(', ') })
        }));
      }
      if (inputUnavailableNames.length) {
        bodyEl.appendChild(el('p', {
          class: 'board-evidence__input-drift',
          text: t('board.figmaEvidence.inputUnavailableReady', { names: inputUnavailableNames.join(', ') })
        }));
      }
      if (vis) {
        vis.open = true;
        bodyEl.appendChild(vis);
        return;
      }
    }
    bodyEl.appendChild(el('p', {
      class: 'board-evidence__empty',
      text: t('board.figmaEvidence.unavailable')
    }));
  }

  return { render: render };
}
