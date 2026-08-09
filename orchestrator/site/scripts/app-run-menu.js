import { dom } from './dom.js';
import { i18n } from './i18n.js';

var el = dom.el;
function t(key, params) { return i18n.t(key, params); }

function reason(code) {
  if (!code) return '';
  var key = 'appRun.reason.' + code;
  var value = t(key);
  return value === key ? t('appRun.reason.unknown') : value;
}

function field(label, control) {
  return el('label', { class: 'app-run-field' }, [
    el('span', { class: 'app-run-field__label', text: label }),
    control
  ]);
}

function option(value, label, disabled) {
  return el('option', { value: value, text: label, disabled: disabled === true });
}

function buildPlatform(targets, selection) {
  var select = el('select', { class: 'input app-run-select', attrs: { 'aria-label': t('appRun.platform') } });
  (targets.platforms || []).forEach(function (platform) {
    var label = platform.id === 'android' ? 'Android' : 'iOS';
    if (platform.availability === 'unavailable') label += ' — ' + reason(platform.reasonCode);
    select.appendChild(option(platform.id, label, platform.availability === 'unavailable'));
  });
  select.value = selection.platform || '';
  return select;
}

function render(options) {
  var targets = options.targets || { platforms: [] };
  var selection = options.selection;
  var current = (targets.platforms || []).find(function (row) { return row.id === selection.platform; }) || targets.platforms[0];
  if (current && !selection.platform) selection.platform = current.id;
  var root = el('section', {
    class: 'app-run-menu',
    attrs: { role: 'dialog', 'aria-modal': 'false', 'aria-label': t('appRun.options') }
  });
  var head = el('div', { class: 'app-run-menu__head' }, [
    el('h2', { text: t('appRun.options') }),
    el('button', { type: 'button', class: 'btn btn--ghost btn--small', text: t('appRun.close') })
  ]);
  head.lastChild.addEventListener('click', options.onClose);
  root.appendChild(head);

  if (!targets.platforms || !targets.platforms.length) {
    root.appendChild(el('p', { class: 'banner banner--warn', text: t('appRun.noPlatforms') }));
    return root;
  }
  var platform = buildPlatform(targets, selection);
  platform.addEventListener('change', function () {
    options.onSelection({ platform: platform.value, targetId: null, variantId: null });
  });
  root.appendChild(field(t('appRun.platform'), platform));
  current = (targets.platforms || []).find(function (row) { return row.id === selection.platform; }) || current;

  if (current && current.availability !== 'available') {
    root.appendChild(el('p', {
      class: current.availability === 'unavailable' ? 'banner banner--warn' : 'app-run-hint',
      text: reason(current.reasonCode)
    }));
    if (current.availability === 'unavailable') {
      root.appendChild(el('a', {
        href: '#setup', class: 'btn btn--ghost btn--small', text: t('common.openSetup')
      }));
    }
  }
  if (current && current.diagnostics &&
      Number.isSafeInteger(current.diagnostics.unsupportedPhysicalDevices) &&
      current.diagnostics.unsupportedPhysicalDevices > 0) {
    root.appendChild(el('p', {
      class: 'app-run-hint',
      text: t('appRun.unsupportedPhysicalDevices', {
        count: current.diagnostics.unsupportedPhysicalDevices
      })
    }));
  }

  var device = el('select', { class: 'input app-run-select', attrs: { 'aria-label': t('appRun.device') } });
  device.appendChild(option('', t('appRun.chooseDevice')));
  (current && current.devices || []).forEach(function (row) {
    device.appendChild(option(row.id, row.displayName + (row.osVersion ? ' · ' + row.osVersion : '')));
  });
  device.value = selection.targetId || '';
  device.addEventListener('change', function () { options.onSelection({ targetId: device.value || null }); });
  root.appendChild(field(t('appRun.device'), device));

  var variant = el('select', { class: 'input app-run-select', attrs: { 'aria-label': t('appRun.variant') } });
  (current && current.variants || []).forEach(function (row) { variant.appendChild(option(row.id, row.label)); });
  variant.value = selection.variantId || (current && current.variants[0] && current.variants[0].id) || '';
  if (!selection.variantId && variant.value) selection.variantId = variant.value;
  variant.addEventListener('change', function () { options.onSelection({ variantId: variant.value }); });
  root.appendChild(field(t('appRun.variant'), variant));

  var mode = el('select', { class: 'input app-run-select', attrs: { 'aria-label': t('appRun.buildMode') } }, [
    option('rebuild', t('appRun.mode.rebuild')),
    option('if-needed', t('appRun.mode.if-needed')),
    option('last-build', t('appRun.mode.last-build'))
  ]);
  mode.value = selection.buildMode || 'if-needed';
  selection.buildMode = mode.value;
  mode.addEventListener('change', function () { options.onSelection({ buildMode: mode.value }); });
  root.appendChild(field(t('appRun.buildMode'), mode));

  var queue = el('input', { type: 'checkbox', class: 'choice-input', checked: selection.whenBusy === 'queue' });
  queue.addEventListener('change', function () { options.onSelection({ whenBusy: queue.checked ? 'queue' : 'fail' }); });
  root.appendChild(el('label', { class: 'app-run-check' }, [queue, el('span', { text: t('appRun.whenReady') })]));

  if (options.context && options.context.taskStem) {
    root.appendChild(el('p', { class: 'app-run-context', text: t('appRun.taskContext', { task: options.context.taskStem }) }));
  } else if (options.context && options.context.surfaceId) {
    root.appendChild(el('p', { class: 'app-run-context', text: t('appRun.surfaceContext') }));
  }

  if (current && current.availability !== 'unavailable' &&
      (!current.devices || !current.devices.length) &&
      current.creatableProfiles && current.creatableProfiles.length) {
    var profile = el('select', { class: 'input app-run-select', attrs: { 'aria-label': t('appRun.createDevice') } });
    current.creatableProfiles.forEach(function (row, index) {
      profile.appendChild(option(String(index), row.displayName + ' · ' + row.runtimeName));
    });
    var create = el('button', { type: 'button', class: 'btn', text: t('appRun.createDevice') });
    // The button travels with the request so the handler can hold it while the
    // creation is in flight; a second click would start a second device.
    create.addEventListener('click', function () {
      options.onCreate(current.creatableProfiles[Number(profile.value || 0)], create);
    });
    root.appendChild(field(t('appRun.deviceProfile'), profile));
    var runAfter = el('input', {
      type: 'checkbox', class: 'choice-input', checked: selection.runAfterCreation === true
    });
    runAfter.addEventListener('change', function () {
      options.onSelection({ runAfterCreation: runAfter.checked });
    });
    root.appendChild(el('label', { class: 'app-run-check' }, [
      runAfter, el('span', { text: t('appRun.runAfterCreation') })
    ]));
    root.appendChild(create);
  }

  var actions = el('div', { class: 'app-run-menu__actions' });
  var refresh = el('button', { type: 'button', class: 'btn btn--ghost', text: t('appRun.refresh') });
  refresh.addEventListener('click', options.onRefresh);
  var run = el('button', { type: 'button', class: 'btn btn--primary', text: t('appRun.run') });
  run.disabled = !selection.targetId || !selection.variantId || !targets.projectSourceRevision ||
    !current || current.availability === 'unavailable';
  // Same in-flight hold as Create: the handler disables this button until the start
  // request settles, so a double click cannot queue two runs.
  run.addEventListener('click', function () { options.onRun(run); });
  actions.appendChild(refresh); actions.appendChild(run); root.appendChild(actions);
  return root;
}

export const appRunMenu = { render: render };
