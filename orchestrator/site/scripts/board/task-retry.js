export function runTaskRetry(options) {
  const action = options.action;
  const checkpointPage = options.checkpoints;
  const checkpoint = checkpointPage && checkpointPage.checkpoints.find(function (item) {
    return item.checkpointId === action.checkpointId;
  });
  if (!checkpoint || checkpoint.freshness && checkpoint.freshness.current === false) {
    return Promise.reject({ kind: 'checkpoint-stale' });
  }
  return options.preview(action, checkpoint.checkpointHash).then(function (preview) {
    const execute = function () {
      return options.execute(action, {
        confirmationToken: preview.confirmationToken || null
      });
    };
    if (!preview.confirmationRequired) return execute();
    return new Promise(function (resolve, reject) {
      options.confirm({
        title: options.t('taskDetails.retry.confirmTitle'),
        message: options.t('taskDetails.retry.confirmBody', {
          phase: checkpoint.retryPolicy.safePhase || checkpoint.phase
        }),
        confirmLabel: options.t('taskDetails.retry.confirm'),
        onConfirm: function () { execute().then(resolve, reject); },
        onCancel: function () { reject({ kind: 'cancelled' }); }
      });
    });
  });
}
