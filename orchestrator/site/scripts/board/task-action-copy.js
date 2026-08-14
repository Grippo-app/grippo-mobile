export function taskActionDisabledReason(action, t) {
  if (!action || action.enabled !== false) return '';
  const key = 'board.action.disabled.' + String(
    action.disabledReasonCode || 'action-unavailable'
  ).replace(/-/g, '_');
  const translated = t(key);
  return translated === key ? t('board.action.disabled.action_unavailable') : translated;
}
