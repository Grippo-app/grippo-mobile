import { requestJson } from './request-json.js';

function query(path, params) {
  var search = new URLSearchParams();
  Object.keys(params || {}).forEach(function (key) {
    var value = params[key];
    if (value !== null && value !== undefined && value !== '' && value !== false) {
      search.set(key, String(value));
    }
  });
  return path + (search.toString() ? '?' + search.toString() : '');
}

function jsonHeaders() {
  var headers = { 'content-type': 'application/json', 'accept': 'application/json' };
  if (typeof window !== 'undefined' && window.__ORCHESTRATOR_CSRF__) {
    headers['x-orchestrator-csrf'] = window.__ORCHESTRATOR_CSRF__;
  }
  return headers;
}

function post(path, body) {
  return requestJson(path, {
    method: 'POST',
    cache: 'no-store',
    headers: jsonHeaders(),
    body: JSON.stringify(body)
  });
}

function overview() {
  return requestJson('/api/architecture/overview', { cache: 'no-store' });
}
function nodes(filters) {
  return requestJson(query('/api/architecture/nodes', filters), { cache: 'no-store' });
}
function findings(filters) {
  return requestJson(query('/api/architecture/findings', filters), { cache: 'no-store' });
}
function node(id, options) {
  return requestJson(query('/api/architecture/nodes/' + encodeURIComponent(id), options), {
    cache: 'no-store'
  });
}
function graph(filters) {
  return requestJson(query('/api/architecture/graph', filters), { cache: 'no-store' });
}
function diff(scope, selector) {
  return requestJson(query('/api/architecture/diff', {
    scope: scope || 'latest',
    selector: selector || null
  }), { cache: 'no-store' });
}
function generate(expectedSourceRevision, reason) {
  return post('/api/architecture/generate', {
    expectedSourceRevision: expectedSourceRevision || null,
    reason: reason
  });
}
function job(id) {
  return requestJson('/api/architecture/jobs/' + encodeURIComponent(id), {
    cache: 'no-store'
  });
}
function previewTask(finding, structuralHash, taskIndexRevision) {
  return post('/api/architecture/tasks/preview', {
    findingId: finding.id,
    fingerprint: finding.fingerprint,
    expectedStructuralHash: structuralHash,
    expectedTaskIndexRevision: taskIndexRevision
  });
}
function createTask(previewId, structuralHash, taskIndexRevision) {
  return post('/api/architecture/tasks/create', {
    previewId: previewId,
    expectedStructuralHash: structuralHash,
    expectedTaskIndexRevision: taskIndexRevision
  });
}
function cancelTask(previewId) {
  return post('/api/architecture/tasks/cancel', { previewId: previewId });
}

export const architectureApi = {
  overview: overview,
  nodes: nodes,
  findings: findings,
  node: node,
  graph: graph,
  diff: diff,
  generate: generate,
  job: job,
  previewTask: previewTask,
  createTask: createTask,
  cancelTask: cancelTask
};
