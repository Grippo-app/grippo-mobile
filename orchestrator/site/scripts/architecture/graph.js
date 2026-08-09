import { dom } from '../dom.js';

var el = dom.el;
var SVG_NS = 'http://www.w3.org/2000/svg';

function svg(tag, attrs) {
  var node = document.createElementNS(SVG_NS, tag);
  Object.keys(attrs || {}).forEach(function (key) { node.setAttribute(key, attrs[key]); });
  return node;
}

function layout(nodes) {
  var layerOrder = ['ui', 'domain', 'data', 'infrastructure', 'build', 'unknown'];
  var groups = Object.create(null);
  layerOrder.forEach(function (layer) { groups[layer] = []; });
  nodes.forEach(function (node) { (groups[node.layer] || groups.unknown).push(node); });
  var positions = Object.create(null);
  var usedColumns = layerOrder.filter(function (layer) { return groups[layer].length; });
  usedColumns.forEach(function (layer, column) {
    groups[layer].sort(function (a, b) { return a.id.localeCompare(b.id); })
      .forEach(function (node, row) {
        positions[node.id] = { x: 40 + column * 240, y: 40 + row * 84 };
      });
  });
  return {
    positions: positions,
    width: Math.max(640, usedColumns.length * 240 + 80),
    height: Math.max(360, Math.max.apply(null, usedColumns.map(function (layer) {
      return groups[layer].length;
    }).concat([1])) * 84 + 80)
  };
}

export function renderArchitectureGraph(host, data, options) {
  var t = options.t;
  if (!data || !data.present) return;
  if (data.tooLarge) {
    host.appendChild(el('div', {
      class: 'banner banner--warn',
      text: t('archmap.graph.tooLarge', {
        nodes: data.nodeCount,
        edges: data.edgeCount
      })
    }));
    return;
  }
  if (!data.nodes.length) {
    host.appendChild(el('div', { class: 'architecture-empty', text: t('archmap.noResults') }));
    return;
  }
  var controls = el('div', {
    class: 'architecture-graph-toolbar',
    attrs: { role: 'group', 'aria-label': t('archmap.graph.controls') }
  });
  var frame = el('div', {
    class: 'architecture-graph',
    attrs: {
      role: 'region',
      'aria-label': t('archmap.graph.label'),
      tabindex: '0'
    }
  });
  var geometry = layout(data.nodes);
  var canvas = el('div', { class: 'architecture-graph__canvas' });
  canvas.style.width = geometry.width + 'px';
  canvas.style.height = geometry.height + 'px';
  var edges = svg('svg', {
    class: 'architecture-graph__edges',
    width: geometry.width,
    height: geometry.height,
    'aria-hidden': 'true'
  });
  data.edges.forEach(function (edge) {
    var from = geometry.positions[edge.from], to = geometry.positions[edge.to];
    if (!from || !to) return;
    edges.appendChild(svg('line', {
      x1: from.x + 90, y1: from.y + 27,
      x2: to.x + 90, y2: to.y + 27,
      class: 'architecture-graph__edge architecture-graph__edge--' + edge.kind
    }));
  });
  canvas.appendChild(edges);
  data.nodes.forEach(function (node) {
    var point = geometry.positions[node.id];
    var findingText = node.findingSeverity
      ? t('archmap.graph.finding', {
        severity: t('archmap.severity.' + node.findingSeverity)
      }) : '';
    var button = el('button', {
      type: 'button',
      class: 'architecture-graph__node architecture-graph__node--' + node.kind +
        (node.findingSeverity ? ' architecture-graph__node--finding' : ''),
      attrs: {
        title: [node.id, node.kind, findingText].filter(Boolean).join(' · '),
        'aria-label': [node.name, t('archmap.kind.' + node.kind), findingText]
          .filter(Boolean).join('. '),
        'data-architecture-control': 'graph-node-' + node.id
      }
    });
    button.appendChild(el('span', { text: node.name }));
    if (node.findingSeverity) {
      button.appendChild(el('span', {
        class: 'architecture-graph__finding',
        text: '⚠ ' + t('archmap.severity.' + node.findingSeverity)
      }));
    }
    button.style.left = point.x + 'px';
    button.style.top = point.y + 'px';
    button.addEventListener('click', function () { options.onSelect(node.id); });
    canvas.appendChild(button);
  });
  frame.appendChild(canvas);
  var viewport = options.viewport || { x: 0, y: 0, scale: 1 };
  function apply() {
    canvas.style.transform = 'translate(' + viewport.x + 'px,' + viewport.y + 'px) scale(' + viewport.scale + ')';
    options.onViewport({
      x: viewport.x,
      y: viewport.y,
      scale: viewport.scale
    });
  }
  function zoom(delta) {
    viewport.scale = Math.max(0.5, Math.min(2, viewport.scale + delta));
    apply();
  }
  [
    { label: t('archmap.graph.zoomIn'), text: '+', action: function () { zoom(0.1); } },
    { label: t('archmap.graph.zoomOut'), text: '−', action: function () { zoom(-0.1); } },
    { label: t('archmap.graph.reset'), text: t('archmap.graph.reset'), action: function () {
      viewport.x = 0;
      viewport.y = 0;
      viewport.scale = 1;
      apply();
      frame.focus();
    } }
  ].forEach(function (row) {
    var button = el('button', {
      type: 'button',
      class: 'btn btn--small',
      text: row.text,
      attrs: { 'aria-label': row.label, title: row.label }
    });
    button.addEventListener('click', row.action);
    controls.appendChild(button);
  });
  host.appendChild(controls);
  host.appendChild(frame);
  frame.addEventListener('wheel', function (event) {
    event.preventDefault();
    zoom(event.deltaY < 0 ? 0.1 : -0.1);
  }, { passive: false });
  frame.addEventListener('keydown', function (event) {
    var handled = true;
    if (event.key === 'ArrowLeft') viewport.x += 24;
    else if (event.key === 'ArrowRight') viewport.x -= 24;
    else if (event.key === 'ArrowUp') viewport.y += 24;
    else if (event.key === 'ArrowDown') viewport.y -= 24;
    else if (event.key === '+' || event.key === '=') zoom(0.1);
    else if (event.key === '-') zoom(-0.1);
    else if (event.key === '0' || event.key === 'Home') {
      viewport.x = 0;
      viewport.y = 0;
      viewport.scale = 1;
    } else handled = false;
    if (!handled) return;
    event.preventDefault();
    apply();
  });
  var drag = null;
  frame.addEventListener('pointerdown', function (event) {
    if (event.target.closest && event.target.closest('.architecture-graph__node')) return;
    drag = { x: event.clientX, y: event.clientY, ox: viewport.x, oy: viewport.y };
    frame.setPointerCapture(event.pointerId);
  });
  frame.addEventListener('pointermove', function (event) {
    if (!drag) return;
    viewport.x = drag.ox + event.clientX - drag.x;
    viewport.y = drag.oy + event.clientY - drag.y;
    apply();
  });
  function endDrag(event) {
    drag = null;
    if (event && typeof frame.hasPointerCapture === 'function' &&
        frame.hasPointerCapture(event.pointerId)) {
      frame.releasePointerCapture(event.pointerId);
    }
  }
  frame.addEventListener('pointerup', endDrag);
  frame.addEventListener('pointercancel', endDrag);
  frame.addEventListener('lostpointercapture', function () { drag = null; });
  apply();
  var fallback = el('details', { class: 'architecture-graph-fallback' });
  fallback.appendChild(el('summary', { text: t('archmap.graph.textFallback') }));
  fallback.appendChild(el('h4', { text: t('archmap.graph.textNodes') }));
  var nodeList = el('div', { class: 'architecture-graph-fallback__nodes' });
  data.nodes.forEach(function (node) {
    var button = el('button', {
      type: 'button', class: 'architecture-relation',
      text: node.name + ' · ' + t('archmap.kind.' + node.kind) +
        (node.findingSeverity
          ? ' · ⚠ ' + t('archmap.severity.' + node.findingSeverity) : ''),
      attrs: { 'data-architecture-control': 'graph-fallback-node-' + node.id }
    });
    button.addEventListener('click', function () { options.onSelect(node.id); });
    nodeList.appendChild(button);
  });
  fallback.appendChild(nodeList);
  fallback.appendChild(el('h4', { text: t('archmap.graph.textRelations') }));
  var relationList = el('ul', { class: 'architecture-graph-fallback__relations' });
  var nodeById = Object.create(null);
  data.nodes.forEach(function (node) { nodeById[node.id] = node; });
  data.edges.forEach(function (edge) {
    var from = nodeById[edge.from];
    var to = nodeById[edge.to];
    relationList.appendChild(el('li', {
      text: (from ? from.name : edge.from) + ' → ' +
        (to ? to.name : edge.to) + ' · ' + t('archmap.relation.' + edge.kind)
    }));
  });
  if (!data.edges.length) {
    relationList.appendChild(el('li', { text: t('archmap.graph.noRelations') }));
  }
  fallback.appendChild(relationList);
  host.appendChild(fallback);
}
