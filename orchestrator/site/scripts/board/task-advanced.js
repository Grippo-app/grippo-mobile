import { dom } from '../dom.js';

const el = dom.el;

function disclosure(title, content) {
  const node = el('details', { class: 'task-details__advanced-section' });
  node.appendChild(el('summary', { text: title }));
  node.appendChild(content);
  return node;
}

function recoveryReasonKey(reason) {
  if (reason === 'owner-active') return 'board.lockRecovery.ownerActive';
  if (reason === 'writer-active') return 'board.lockRecovery.writerActive';
  if (reason === 'owner-host-foreign') return 'board.lockRecovery.foreignHost';
  if (reason === 'owner-kind-unsupported') return 'board.lockRecovery.unsupportedOwner';
  // The server emits five more verdicts; without their own sentence they all read
  // as "no proof — check again", including the two that a re-check can never clear.
  if (reason === 'owner-identity-unavailable') return 'board.lockRecovery.identityUnavailable';
  if (reason === 'owner-liveness-unavailable') return 'board.lockRecovery.livenessUnavailable';
  if (reason === 'writer-authority-unavailable') return 'board.lockRecovery.writerAuthorityUnavailable';
  if (reason === 'recovery-authority-missing') return 'board.lockRecovery.authorityMissing';
  if (reason === 'release-recovery-required') return 'board.lockRecovery.releaseRecoveryRequired';
  return 'board.lockRecovery.unavailable';
}

// Owner kind and hostname live in immutable lock bytes: re-running the read-only
// owner check returns the same verdict forever, so offering "check again" for
// them is a loop with no exit.
function terminalRecoveryReason(reason) {
  // Owner kind and hostname come from immutable lock bytes; a retained release
  // generation is only cleared by the recover-release CLI. None of the three
  // can change because the user pressed the read-only check again.
  return reason === 'owner-kind-unsupported' || reason === 'owner-host-foreign' ||
    reason === 'release-recovery-required';
}

// Phase two refuses for reasons the owner check can never resolve. Repeating the
// owner check on those is a loop, so each one gets its own sentence — and the
// generic "the owner could not be proven" is reserved for the case it describes.
function releaseFailureKey(error) {
  var code = error && error.reasonCode;
  if (code === 'LOCK_OWNER_RECOVERY_STATE_INVALID') return 'board.lockRecovery.stateInvalid';
  if (code === 'LOCK_OWNER_RECOVERY_STATE_CHANGED' ||
      code === 'LOCK_RELEASE_POSTCONDITION_CHANGED' ||
      code === 'LOCK_CHANGED' ||
      code === 'LOCK_IDENTITY_MISMATCH') return 'board.lockRecovery.stateChanged';
  if (code === 'LOCK_OWNER_RECOVERY_STATE_UNAVAILABLE' ||
      code === 'LOCK_RELEASE_POSTCONDITION_UNAVAILABLE') return 'board.lockRecovery.stateUnavailable';
  if (code === 'LOCK_RELEASE_POSTCONDITION_FAILED') return 'board.lockRecovery.postconditionFailed';
  if (code === 'LOCK_RELEASE_RECOVERY_REQUIRED') return 'board.lockRecovery.releaseRecoveryRequired';
  return 'board.lockRecovery.releaseFailed';
}

function lockRecovery(runtime, options) {
  if (!runtime || !runtime.lock || typeof options.inspectLockRecovery !== 'function' ||
      typeof options.recoverLock !== 'function') return null;
  const t = options.t;
  const panel = el('section', {
    class: 'task-details__lock-recovery',
    attrs: { 'data-task-lock-recovery': 'true' }
  });
  const status = el('p', {
    class: 'task-details__lock-status',
    text: t('board.lockRecovery.blocked')
  });
  const action = el('button', {
    type: 'button',
    class: 'btn btn--sm',
    text: t('board.lockRecovery.check'),
    attrs: { title: t('board.lockRecovery.checkTooltip') }
  });
  let inspectedHash = null;

  function resetAfterFailure(key, error) {
    inspectedHash = null;
    // role first: a live region that does not exist when its text changes is
    // not announced.
    status.setAttribute('role', 'alert');
    status.textContent = t(key, { detail: options.errorText(error) });
    action.textContent = t('board.lockRecovery.retry');
    action.title = t('board.lockRecovery.recheckTooltip');
    action.disabled = false;
    action.removeAttribute('aria-busy');
  }

  function inspect() {
    inspectedHash = null;
    action.disabled = true;
    action.setAttribute('aria-busy', 'true');
    action.textContent = t('board.lockRecovery.checking');
    options.inspectLockRecovery().then(function (result) {
      if (!panel.isConnected) return;
      action.disabled = false;
      action.removeAttribute('aria-busy');
      if (result.recoverable === true) {
        inspectedHash = result.lockHash;
        status.removeAttribute('role');
        status.textContent = t('board.lockRecovery.ownerGone');
        action.textContent = t('board.lockRecovery.release');
        action.title = t('board.lockRecovery.releaseTooltip');
        action.classList.add('btn--danger');
        return;
      }
      status.textContent = t(recoveryReasonKey(result.reason));
      action.classList.remove('btn--danger');
      if (terminalRecoveryReason(result.reason)) {
        action.hidden = true;
        return;
      }
      action.textContent = t('board.lockRecovery.retry');
      action.title = t('board.lockRecovery.recheckTooltip');
    }, function (error) {
      if (!panel.isConnected) return;
      resetAfterFailure('board.lockRecovery.checkFailed', error);
    });
  }

  function release() {
    const expectedHash = inspectedHash;
    inspectedHash = null;
    action.disabled = true;
    action.setAttribute('aria-busy', 'true');
    action.textContent = t('board.lockRecovery.releasing');
    options.recoverLock(expectedHash).then(function (result) {
      if (!panel.isConnected) return;
      // The detach does not publish the INDEX; say so rather than letting the
      // board look canonical while it is not.
      status.removeAttribute('role');
      status.textContent = result && result.indexStatus && result.indexStatus !== 'fresh'
        ? t('board.lockRecovery.releasedIndexStale')
        : t('board.lockRecovery.released');
      action.hidden = true;
      action.removeAttribute('aria-busy');
      if (typeof options.onLockRecovered === 'function') options.onLockRecovered(result);
    }, function (error) {
      if (!panel.isConnected) return;
      action.classList.remove('btn--danger');
      // The lease-unsettled response reports a lock that WAS detached; calling
      // that "recovery was not proven" is a lie about a completed mutation.
      if (error && error.recovered === true) {
        status.removeAttribute('role');
        status.textContent = t('board.lockRecovery.releasedLeaseUnsettled');
        action.hidden = true;
        action.removeAttribute('aria-busy');
        if (typeof options.onLockRecovered === 'function') options.onLockRecovered(null);
        return;
      }
      resetAfterFailure(releaseFailureKey(error), error);
    });
  }

  action.addEventListener('click', function () {
    if (inspectedHash) release();
    else inspect();
  });
  panel.appendChild(status);
  panel.appendChild(action);
  return panel;
}

export function renderTaskAdvanced(target, page, options) {
  while (target.firstChild) target.removeChild(target.firstChild);
  const t = options.t;
  target.appendChild(el('h3', {
    class: 'task-details__section-title',
    text: t('taskDetails.advanced.title')
  }));
  const sections = page.sections || {};
  if (sections.raw) {
    const raw = el('div');
    raw.appendChild(el('h4', { text: t('taskDetails.advanced.taskMarkdown') }));
    raw.appendChild(el('pre', { class: 'task-details__raw', text: sections.raw.taskMarkdown || '' }));
    if (sections.raw.questionsMarkdown) {
      raw.appendChild(el('h4', { text: t('taskDetails.advanced.questionsMarkdown') }));
      raw.appendChild(el('pre', { class: 'task-details__raw', text: sections.raw.questionsMarkdown }));
    }
    target.appendChild(disclosure(t('taskDetails.advanced.raw'), raw));
  }
  if (sections.revisions) target.appendChild(disclosure(
    t('taskDetails.advanced.revisions'),
    el('pre', { class: 'task-details__raw', text: JSON.stringify(sections.revisions, null, 2) })
  ));
  if (sections.runtime) {
    const runtime = el('div');
    const recovery = lockRecovery(sections.runtime, options);
    if (recovery) runtime.appendChild(recovery);
    runtime.appendChild(el('pre', {
      class: 'task-details__raw',
      text: JSON.stringify(sections.runtime, null, 2)
    }));
    target.appendChild(disclosure(t('taskDetails.advanced.runtime'), runtime));
  }
  if (sections.integrity) target.appendChild(disclosure(
    t('taskDetails.advanced.integrity'),
    el('pre', { class: 'task-details__raw', text: JSON.stringify(sections.integrity, null, 2) })
  ));
  if (sections.outcome) target.appendChild(disclosure(
    t('taskDetails.advanced.outcome'),
    el('pre', { class: 'task-details__raw', text: JSON.stringify(sections.outcome, null, 2) })
  ));
  if (sections.diagnostics) target.appendChild(disclosure(
    t('taskDetails.advanced.diagnostics'),
    el('pre', { class: 'task-details__raw', text: JSON.stringify(sections.diagnostics, null, 2) })
  ));
  if (sections.checkpoints) {
    const list = el('div', { class: 'task-details__checkpoint-list' });
    (sections.checkpoints.checkpoints || []).forEach(function (checkpoint) {
      list.appendChild(el('pre', {
        class: 'task-details__raw task-details__checkpoint',
        text: JSON.stringify(checkpoint, null, 2),
        attrs: { 'data-checkpoint-id': checkpoint.checkpointId, tabindex: '-1' }
      }));
    });
    if (!list.firstChild) list.appendChild(el('p', {
      class: 'task-details__empty', text: t('taskDetails.advanced.noCheckpoints')
    }));
    target.appendChild(disclosure(t('taskDetails.advanced.checkpoints'), list));
  }
}
