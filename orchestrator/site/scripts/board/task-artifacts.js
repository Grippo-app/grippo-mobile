import { dom } from '../dom.js';

const el = dom.el;

const ARTIFACT_LABEL_KEYS = Object.freeze([
  'taskDetails.artifact.label.appValidation',
  'taskDetails.artifact.label.designDeclaration',
  'taskDetails.artifact.label.figmaEvidence',
  'taskDetails.artifact.label.visualComparison'
]);

function artifactImageUrl(stem, item) {
  const id = item.target && item.target.guardedDownloadId;
  if (item.source === 'app-run-validation' && /^shot-[a-f0-9]{36}$/.test(String(id || ''))) {
    return '/api/app-run/screenshots/' + encodeURIComponent(id);
  }
  const reportHash = item.metadata && item.metadata.reportHash;
  if (item.source === 'figma-evidence' && typeof id === 'string' && id &&
      /^sha256:[a-f0-9]{64}$/.test(String(reportHash || ''))) {
    return '/api/figma/compare-artifact?stem=' + encodeURIComponent(stem) +
      '&id=' + encodeURIComponent(id) + '&reportHash=' + encodeURIComponent(reportHash);
  }
  return null;
}

function artifactLabel(item, options) {
  const key = item.metadata && item.metadata.labelKey;
  let label = item.label;
  const translatedKey = ARTIFACT_LABEL_KEYS.includes(key) ? key : null;
  if (translatedKey) {
    const translated = options.t(translatedKey);
    if (translated !== translatedKey) label = translated;
  }
  const labelStatus = item.metadata && item.metadata.labelStatus;
  if (labelStatus) {
    const statusKey = 'taskDetails.artifact.status.' + labelStatus;
    const translatedStatus = options.t(statusKey);
    label += ' · ' + (translatedStatus === statusKey ? labelStatus : translatedStatus);
  }
  return label;
}

function artifactItem(stem, item, options) {
  const imageUrl = item.kind === 'screenshot' ? artifactImageUrl(stem, item) : null;
  const label = artifactLabel(item, options);
  const row = el('li', {
    class: 'task-details__artifact task-details__artifact--' + item.status +
      (imageUrl ? ' task-details__artifact--preview' : ''),
    attrs: { 'data-artifact-id': item.id, tabindex: '-1' }
  });
  if (imageUrl) {
    const preview = el('figure', { class: 'task-details__artifact-preview' });
    preview.appendChild(el('img', {
      class: 'task-details__artifact-image',
      attrs: {
        src: imageUrl,
        alt: label,
        loading: 'lazy',
        decoding: 'async'
      }
    }));
    row.appendChild(preview);
  }
  const main = el('div', { class: 'task-details__artifact-main' });
  const isDesignIssue = item.source === 'task-design' &&
    item.metadata && item.metadata.issueKind;
  main.appendChild(el('strong', {
    text: isDesignIssue
      ? options.t('taskDetails.designIssue.title') : label
  }));
  main.appendChild(el('span', {
    class: 'task-details__artifact-kind',
    text: options.t('taskDetails.artifact.kind.' + item.kind.replace(/-/g, '_'))
  }));
  if (isDesignIssue) {
    const issueKey = 'taskDetails.designIssue.kind.' + item.metadata.issueKind;
    const translated = options.t(issueKey);
    main.appendChild(el('span', {
      class: 'task-details__artifact-detail',
      text: (translated === issueKey
        ? options.t('taskDetails.designIssue.kind.unknown') : translated) +
        (item.metadata.line
          ? ' · ' + options.t('taskDetails.designIssue.line', { line: item.metadata.line }) : '')
    }));
  }
  if (item.kind === 'file' && item.metadata && item.metadata.change) {
    const changeKey = 'taskDetails.artifact.change.' + item.metadata.change;
    const translated = options.t(changeKey);
    main.appendChild(el('span', {
      class: 'task-details__artifact-detail',
      text: translated === changeKey
        ? options.t('taskDetails.artifact.change.modified') : translated
    }));
  }
  if (item.kind === 'endpoint' && item.metadata) {
    const implementationKey = item.metadata.implementationStatus
      ? 'api.implementation.' + item.metadata.implementationStatus : null;
    const implementation = implementationKey ? options.t(implementationKey) : null;
    const changeKey = item.metadata.changeStatus
      ? 'api.severity.' + item.metadata.changeStatus : null;
    const change = changeKey ? options.t(changeKey) : null;
    const endpointFacts = [
      item.metadata.operationId,
      item.metadata.environmentId,
      implementation === implementationKey ? item.metadata.implementationStatus : implementation,
      change === changeKey ? item.metadata.changeStatus : change
    ].filter(Boolean);
    if (endpointFacts.length) main.appendChild(el('span', {
      class: 'task-details__artifact-detail',
      text: endpointFacts.join(' · ')
    }));
    if (item.metadata.contractHash) main.appendChild(el('code', {
      class: 'task-details__artifact-detail',
      text: item.metadata.contractHash
    }));
  }
  row.appendChild(main);
  row.appendChild(el('span', {
    class: 'task-details__artifact-status',
    text: options.t('taskDetails.artifact.status.' + item.status)
  }));
  if (imageUrl) {
    row.appendChild(el('a', {
      class: 'btn btn--sm',
      text: options.t('taskDetails.artifact.open'),
      attrs: { href: imageUrl, target: '_blank', rel: 'noopener' }
    }));
  } else if (item.target && options.onOpenTarget) {
    const button = el('button', {
      type: 'button',
      class: 'btn btn--sm',
      text: options.t('taskDetails.artifact.open')
    });
    button.addEventListener('click', function () { options.onOpenTarget(item.target); });
    row.appendChild(button);
  }
  return row;
}

export function renderTaskArtifacts(target, page, options) {
  while (target.firstChild) target.removeChild(target.firstChild);
  target.appendChild(el('h3', {
    class: 'task-details__section-title',
    text: options.t('taskDetails.artifacts.title')
  }));
  if (!page.artifacts.length) {
    target.appendChild(el('p', {
      class: 'task-details__empty',
      text: options.t('taskDetails.artifacts.empty')
    }));
  } else {
    const counts = Object.create(null);
    (page.groups || []).forEach(function (group) {
      if (group && typeof group.kind === 'string') counts[group.kind] = group.count;
    });
    const kinds = [];
    page.artifacts.forEach(function (item) {
      if (kinds.indexOf(item.kind) < 0) kinds.push(item.kind);
    });
    kinds.forEach(function (kind) {
      const section = el('section', { class: 'task-details__artifact-group' });
      section.appendChild(el('h4', {
        class: 'task-details__artifact-group-title',
        text: options.t('taskDetails.artifact.kind.' + kind.replace(/-/g, '_')) +
          (Number.isSafeInteger(counts[kind]) ? ' · ' + counts[kind] : '')
      }));
      const list = el('ul', { class: 'task-details__artifact-list' });
      page.artifacts.filter(function (item) { return item.kind === kind; }).forEach(function (item) {
        list.appendChild(artifactItem(page.stem, item, options));
      });
      section.appendChild(list);
      target.appendChild(section);
    });
  }
  if (options.extraNode) target.appendChild(options.extraNode);
  if (page.nextCursor && options.loadMore) {
    const more = el('button', {
      type: 'button', class: 'btn btn--sm task-details__load-more',
      text: options.t('taskDetails.loadMore')
    });
    more.addEventListener('click', function () { options.loadMore(page.nextCursor, more); });
    target.appendChild(more);
  }
  if (page.partial) target.appendChild(el('p', {
    class: 'banner banner--warn',
    text: options.t('taskDetails.artifacts.partial')
  }));
}
