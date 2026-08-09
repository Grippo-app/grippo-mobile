var COPY_PROMPT_LABELS = Object.freeze([
  'board.overflow.copy_prepare_prompt',
  'board.overflow.copy_run_prompt',
  'board.overflow.copy_retry_prompt'
]);

function secondaryFor(row, kind) {
  return (Array.isArray(row.secondaryActions) ? row.secondaryActions : []).find(function (action) {
    return action && action.kind === kind;
  }) || null;
}

function add(items, kind, labelKey, group, serverAction, tone) {
  items.push({
    kind: kind,
    labelKey: labelKey,
    group: group,
    serverAction: serverAction || null,
    tone: tone || null,
    separated: false
  });
}

export function taskOverflowItems(row) {
  row = row || {};
  var items = [];
  var drop = secondaryFor(row, 'drop');
  var reopen = secondaryFor(row, 'reopen');
  var copyPrompt = secondaryFor(row, 'copy-prompt');

  if (row.sourceTarget && row.sourceTarget.availability === 'available') {
    add(items, 'source', 'board.overflow.open_source', 'navigation');
  }

  if (row.state === 'backlog' && row.runtimeStatus && !row.runtimeStatus.active &&
      drop && drop.enabled !== false) {
    add(items, 'edit', 'board.overflow.edit', 'tools');
  }
  add(items, 'copy-id', 'board.overflow.copy_id', 'tools');
  if (copyPrompt && copyPrompt.enabled !== false &&
      COPY_PROMPT_LABELS.indexOf(copyPrompt.labelKey) >= 0) {
    add(items, 'copy-prompt', copyPrompt.labelKey, 'tools', copyPrompt);
  }

  if (row.state === 'done') {
    if (reopen && reopen.enabled !== false) {
      add(items, 'reopen', 'board.overflow.reopen', 'lifecycle', reopen);
    }
  }
  if (drop && drop.enabled !== false) {
    add(items, 'drop', 'board.overflow.drop', 'lifecycle', drop, 'danger');
  }

  var previousGroup = null;
  items.forEach(function (item, index) {
    item.separated = index > 0 && item.group !== previousGroup;
    previousGroup = item.group;
  });
  return items;
}
