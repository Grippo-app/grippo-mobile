import { requestJson } from './data/request-json.js';

function headers() {
  var out = { 'content-type': 'application/json' };
  if (typeof window !== 'undefined' && window.__ORCHESTRATOR_CSRF__) {
    out['x-orchestrator-csrf'] = window.__ORCHESTRATOR_CSRF__;
  }
  return out;
}

function get(url) {
  return requestJson(url, { cache: 'no-store' });
}

function post(url, body) {
  return requestJson(url, {
    method: 'POST',
    headers: headers(),
    body: JSON.stringify(body || {}),
    cache: 'no-store'
  });
}

function idempotencyKey(prefix) {
  var suffix;
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') suffix = crypto.randomUUID();
  else suffix = Date.now().toString(36) + '-' + Math.random().toString(36).slice(2);
  return prefix + ':' + suffix;
}

export const reviewerApi = {
  status: function () { return get('/api/reviewer/status'); },
  activity: function (state, cursor, limit) {
    var params = new URLSearchParams();
    if (state) params.set('state', state);
    if (cursor) params.set('cursor', cursor);
    if (limit) params.set('limit', String(limit));
    return get('/api/reviewer/activity?' + params.toString());
  },
  save: function (mode, expectedRevision, key) {
    return post('/api/reviewer/settings', {
      mode: mode,
      expectedRevision: expectedRevision,
      idempotencyKey: key || idempotencyKey('reviewer-settings')
    });
  },
  recheck: function () { return post('/api/reviewer/recheck', {}); },
  idempotencyKey: idempotencyKey
};
