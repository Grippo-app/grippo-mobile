import { finalVisualTrustState } from './visual-evidence-trust.js';

export function createVisualEvidenceRecoveryView(dependencies) {
  var el = dependencies.el;
  var t = dependencies.t;
  var evidenceIssueLabel = dependencies.evidenceIssueLabel;
  var buildEvidenceBadge = dependencies.buildEvidenceBadge;
  var copyButton = dependencies.copyButton;
  var onRebundle = dependencies.onRebundle;
  var onRebundleError = dependencies.onRebundleError;

  // The failure cause under the non-ready header. Everything already rides in
  // the evidence response; rendering stays text-only and null-guarded.
  var EVIDENCE_CAUSE_MAX_ISSUES = 5;
  function buildEvidenceCause(resp) {
    if (!resp || typeof resp !== 'object') return null;
    var missing = Array.isArray(resp.missingRequiredReports)
      ? resp.missingRequiredReports.filter(function (n) { return typeof n === 'string' && n; }) : [];
    var drifted = Array.isArray(resp.hashDriftReports)
      ? resp.hashDriftReports.map(function (d) { return d && d.name; }).filter(Boolean) : [];
    var issues = Array.isArray(resp.topIssues) ? resp.topIssues.slice(0, EVIDENCE_CAUSE_MAX_ISSUES) : [];
    var stateRaw = typeof resp.evidenceState === 'string' ? resp.evidenceState : '';
    var stateLine = '';
    if (stateRaw && stateRaw !== 'READY') {
      var stKey = 'board.figmaEvidence.cause.state.' + stateRaw.toLowerCase();
      var stLabel = t(stKey);
      stateLine = t('board.figmaEvidence.cause.state', { state: stLabel === stKey ? stateRaw : stLabel });
    }
    if (!stateLine && !missing.length && !drifted.length && !issues.length) return null;
    var box = el('details', { class: 'board-evidence__cause' });
    box.open = true;
    box.appendChild(el('summary', { class: 'board-evidence__cause-summary', text: t('board.figmaEvidence.cause.heading') }));
    if (stateLine) box.appendChild(el('p', { class: 'board-evidence__cause-line', text: stateLine }));
    if (missing.length) {
      box.appendChild(el('p', {
        class: 'board-evidence__cause-line',
        text: t('board.figmaEvidence.cause.missingReports', { names: missing.join(', ') })
      }));
    }
    if (drifted.length) {
      box.appendChild(el('p', {
        class: 'board-evidence__cause-line',
        text: t('board.figmaEvidence.cause.hashDrift', { names: drifted.join(', ') })
      }));
    }
    // Advisory axis: report input files moved after the check. This is distinct
    // from integrity and never changes the evidence state.
    var inputDrifted = [];
    var inputUnavailable = [];
    (Array.isArray(resp.inputDriftReports) ? resp.inputDriftReports : []).forEach(function (d) {
      var n = d && d.name;
      var unavailable = d && ['missing', 'missing-input-hash', 'input-hash-untrusted-or-missing',
        'design-source-hash-missing', 'design-source-unavailable'].indexOf(d.reason) >= 0;
      var target = unavailable ? inputUnavailable : inputDrifted;
      if (n && target.indexOf(n) < 0) target.push(n);
    });
    if (inputDrifted.length) {
      box.appendChild(el('p', {
        class: 'board-evidence__cause-line board-evidence__cause-line--advisory',
        text: t('board.figmaEvidence.cause.inputDrift', { names: inputDrifted.join(', ') })
      }));
    }
    if (inputUnavailable.length) {
      box.appendChild(el('p', {
        class: 'board-evidence__cause-line board-evidence__cause-line--advisory',
        text: t('board.figmaEvidence.cause.inputUnavailable', { names: inputUnavailable.join(', ') })
      }));
    }
    for (var i = 0; i < issues.length; i++) {
      var issue = issues[i];
      if (!issue || typeof issue !== 'object') continue;
      var row = el('div', { class: 'board-evidence__cause-issue' });
      row.appendChild(buildEvidenceBadge(issue.severity));
      var parts = [];
      if (issue.report) parts.push(String(issue.report));
      // Keep the raw machine kind only in the diagnostic title. Free-form
      // report messages are intentionally not rendered.
      var rawKind = issue.issueKind ? String(issue.issueKind) : '';
      if (rawKind) parts.push(evidenceIssueLabel(rawKind));
      var text = parts.join(' · ');
      row.appendChild(el('span', {
        class: 'board-evidence__cause-issue-text',
        text: text || t('board.figmaEvidence.status.unknown'),
        attrs: rawKind ? { title: rawKind } : {}
      }));
      box.appendChild(row);
    }
    return box;
  }

  // A failed done comparison cannot re-enter the task queue. Build the exact
  // bounded local recovery sequence and refuse untrusted shell fragments.
  function evidenceRerunCommand(stem, resp) {
    var s = String(stem || '').trim();
    if (!/^[A-Za-z0-9][A-Za-z0-9_-]*$/.test(s)) return '';
    var rid = resp && typeof resp.pipelineRunId === 'string' ? resp.pipelineRunId.trim() : '';
    var prefix = /^[A-Za-z0-9][A-Za-z0-9._-]{0,159}$/.test(rid) ? 'FIGMA_PIPELINE_RUN_ID=' + rid + ' ' : '';
    var lines = [prefix + 'node orchestrator/figma/scripts/evidence-clean.mjs ' + s + ' --bundle-only'];
    var drifted = (Array.isArray(resp && resp.hashDriftReports) ? resp.hashDriftReports : [])
      .concat(Array.isArray(resp && resp.inputDriftReports) ? resp.inputDriftReports : []);
    if (drifted.some(function (d) { return d && d.name === 'census'; })) {
      lines.push(prefix + 'node orchestrator/figma/scripts/component-census.mjs ' + s);
    }
    lines.push(prefix + 'node orchestrator/figma/scripts/evidence-bundle.mjs ' + s + ' --stage final --fresh');
    return lines.join('\n');
  }

  function buildEvidenceRerun(stem, resp) {
    var cmd = evidenceRerunCommand(stem, resp);
    if (!cmd) return null;
    var wrap = el('div', { class: 'board-evidence__rerun' });
    var trust = finalVisualTrustState(resp || {});
    wrap.appendChild(el('p', { class: 'board-evidence__rerun-hint', text: t(trust.usable ? 'board.figmaEvidence.rerun.hint' : 'board.figmaEvidence.rerun.hintRebuild') }));
    var s = String(stem || '').trim();
    if (/^[A-Za-z0-9][A-Za-z0-9_-]*$/.test(s)) {
      var btn = el('button', { type: 'button', class: 'btn btn--ghost board-evidence__rebundle-btn', text: t('board.figmaEvidence.rebundle.button') });
      btn.addEventListener('click', function () {
        btn.disabled = true;
        onRebundle(s).then(function () { btn.disabled = false; }, function (error) {
          btn.disabled = false;
          onRebundleError(error);
        });
      });
      wrap.appendChild(btn);
    }
    wrap.appendChild(el('pre', { class: 'board-evidence__rerun-cmd', text: cmd }));
    wrap.appendChild(copyButton(t('board.figmaEvidence.rerun.copy'), function () { return cmd; }));
    return wrap;
  }

  return {
    buildCause: buildEvidenceCause,
    buildRerun: buildEvidenceRerun,
    rerunCommand: evidenceRerunCommand
  };
}
