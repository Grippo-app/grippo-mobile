export function createVisualFixActionsView(dependencies) {
  var el = dependencies.el;
  var t = dependencies.t;

  var BLOCKED_REASON_KEYS = {
    'missing-required': 'board.figmaEvidence.fix.blockedMissingRequired',
    'stale': 'board.figmaEvidence.fix.blockedStale',
    'hash-drift': 'board.figmaEvidence.fix.blockedHashDrift',
    'mixed-runs': 'board.figmaEvidence.fix.blockedMixedRuns',
    'no-artifacts': 'board.figmaEvidence.fix.blockedNoArtifacts',
    'global-recovery': 'board.integrity.createBlocked'
  };

  function buildBlocked(reason) {
    var key = BLOCKED_REASON_KEYS[reason] || 'board.figmaEvidence.fix.blockedGeneric';
    var wrap = el('div', { class: 'board-evidence__fix board-evidence__fix--blocked' });
    wrap.appendChild(el('p', {
      class: 'board-evidence__fix-help board-evidence__fix-blocked-reason',
      text: t(key)
    }));
    var button = el('button', {
      type: 'button',
      class: 'btn btn--ghost board-evidence__fix-btn',
      text: t('board.figmaEvidence.fix.create'),
      attrs: { title: t(key) }
    });
    button.disabled = true;
    wrap.appendChild(button);
    return wrap;
  }

  function buildActions(state, onCreate) {
    var wrap = el('div', { class: 'board-evidence__fix' });
    wrap.appendChild(el('p', {
      class: 'board-evidence__fix-help',
      text: t('board.figmaEvidence.fix.helper')
    }));
    var button = el('button', {
      type: 'button',
      class: 'btn btn--ghost board-evidence__fix-btn',
      text: state === 'queued' ? t('board.figmaEvidence.fix.queued') :
            state === 'loading' ? t('board.figmaEvidence.fix.loading') :
            state === 'failed' ? t('board.figmaEvidence.fix.failed') :
            t('board.figmaEvidence.fix.create')
    });
    button.disabled = state === 'loading' || state === 'queued';
    button.addEventListener('click', function () { onCreate(); });
    wrap.appendChild(button);
    return wrap;
  }

  return {
    buildActions: buildActions,
    buildBlocked: buildBlocked
  };
}
