'use strict';

// Read-only Architecture relations for Design entities. Relations are
// intentionally best-effort: a missing architecture map never makes Design
// unavailable and raw machine paths are not exposed.

var arch = require('./arch');

function lower(value) { return String(value == null ? '' : value).toLowerCase(); }
function relationKey(value) {
  var key = lower(value).replace(/[^a-z0-9]+/g, '');
  var previous = null;
  while (key && key !== previous) {
    previous = key;
    key = key.replace(/(?:screen|dialog|overlay|component)$/i, '');
  }
  return key;
}
function cleanPath(value) {
  var path = typeof value === 'string' ? value.replace(/\\/g, '/') : '';
  if (!path || path.charAt(0) === '/' || /^[A-Za-z]:\//.test(path)) return null;
  return path.split('/').some(function (segment) {
    return !segment || segment === '.' || segment === '..';
  }) ? null : path;
}

function snapshot() {
  var loaded = arch.readValidated();
  var map = loaded && loaded.present ? loaded.map : null;
  var incoming = Object.create(null);
  var outgoing = Object.create(null);
  if (map) {
    map.nodes.forEach(function (node) {
      incoming[node.id] = [];
      outgoing[node.id] = [];
    });
    map.edges.forEach(function (edge) {
      incoming[edge.to].push(edge);
      outgoing[edge.from].push(edge);
    });
  }
  return {
    present: !!map,
    map: map,
    nodeById: loaded && loaded.nodeById || Object.create(null),
    incoming: incoming,
    outgoing: outgoing,
    structuralHash: map ? map.structuralHash : null
  };
}

function surfaceRelation(surface, state) {
  var map = state && state.map;
  if (!map) return { available: false, module: null, feature: surface.feature || null, route: surface.route || null, codeSources: [] };
  var screens = map.nodes.filter(function (node) { return node.kind === 'screen'; });
  var route = lower(surface.route).trim();
  var name = relationKey(surface.name);
  var feature = relationKey(surface.feature);
  var routeHits = route ? screens.filter(function (node) {
    return node.metadata.routes.some(function (candidate) {
      return lower(candidate).trim() === route;
    });
  }) : [];
  var candidates = routeHits.length ? routeHits : screens.filter(function (node) {
    if (name && relationKey(node.name) === name) return true;
    return (state.incoming[node.id] || []).some(function (edge) {
      var owner = edge.kind === 'owns' && state.nodeById[edge.from];
      if (!owner || owner.kind !== 'feature') return false;
      var ownership = owner.metadata.ownershipId || owner.name;
      return !!((feature && relationKey(ownership) === feature) ||
        (name && relationKey(ownership) === name));
    });
  });
  if (!candidates.length && name) {
    var componentHits = map.nodes.filter(function (node) {
      return node.kind === 'component' && relationKey(node.name) === name;
    });
    var renderedBy = [];
    componentHits.forEach(function (component) {
      (state.incoming[component.id] || []).forEach(function (edge) {
        var screen = edge.kind === 'renders' && state.nodeById[edge.from];
        if (screen && screen.kind === 'screen' &&
            !renderedBy.some(function (row) { return row.id === screen.id; })) {
          renderedBy.push(screen);
        }
      });
    });
    candidates = renderedBy;
  }
  var hit = candidates.length === 1 ? candidates[0] : null;
  if (!hit) return {
    available: true, module: null, feature: surface.feature || null,
    route: surface.route || null, codeSources: []
  };
  var ownerNodes = (state.incoming[hit.id] || []).filter(function (edge) {
    return edge.kind === 'owns';
  }).map(function (edge) {
    return state.nodeById[edge.from];
  }).filter(function (node) {
    return node && node.kind === 'feature';
  });
  var renderedNodes = (state.outgoing[hit.id] || []).filter(function (edge) {
    return edge.kind === 'renders';
  }).map(function (edge) {
    return state.nodeById[edge.to];
  }).filter(Boolean);
  var moduleNodes = renderedNodes.filter(function (node) { return node.kind === 'module'; });
  var codeSources = [hit.path].concat(renderedNodes.filter(function (node) {
    return node.kind === 'component';
  }).map(function (node) {
    return node.path;
  })).map(cleanPath).filter(Boolean);
  codeSources = Array.from(new Set(codeSources)).sort().slice(0, 20);
  return {
    available: true,
    module: moduleNodes.length === 1
      ? moduleNodes[0].metadata.gradlePath : null,
    feature: ownerNodes.length === 1
      ? ownerNodes[0].metadata.ownershipId : surface.feature || null,
    route: surface.route || hit.metadata.routes[0] || null,
    codeSources: codeSources
  };
}

// Usage joins by stable provider node id ONLY (the instances artifact carries
// the owning component-set node id). Display names are labels, never identity.
function componentUsage(component, surfaces) {
  var nodeId = component && component.nodeId ? String(component.nodeId) : null;
  if (!nodeId) return [];
  return (surfaces || []).filter(function (surface) {
    return (surface.usedComponents || []).some(function (usage) {
      return usage && usage.nodeId && String(usage.nodeId) === nodeId;
    });
  }).map(function (surface) {
    return { id: surface.id, name: surface.name, type: surface.type, route: surface.route || null };
  }).slice(0, 100);
}

module.exports = {
  snapshot: snapshot,
  surfaceRelation: surfaceRelation,
  componentUsage: componentUsage
};
