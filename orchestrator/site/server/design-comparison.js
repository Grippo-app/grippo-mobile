'use strict';

// Project -> Design owns the local code comparison UX. The underlying job is
// still published by the canonical Figma generation runner, but this adapter
// exposes one strict Design operation: compare the active immutable generation
// with product source. It never selects a live Figma scope or accepts warning
// acknowledgements. The only optional selector is an exact local comparison
// domain, so domain retries do not silently become global work.

var catalog = require('./design-catalog');
var integration = require('./figma-integration');
var sync = require('./figma-sync');

function exact(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}

function publicJob(job) {
  if (!job) return null;
  return {
    id: job.id,
    state: job.state,
    phase: job.phase,
    progress: job.progress,
    startedAt: job.startedAt,
    comparisonDomain: job.comparisonDomain || null
  };
}

function status(activeOverride, expectedDomain) {
  var model;
  try { model = integration.get(activeOverride); }
  catch (error) {
    return { state: 'blocked', reasonCode: 'integration-unavailable', job: null };
  }
  var active = model && model.sync && model.sync.active || null;
  var isComparison = !!(active && Array.isArray(active.groups) && active.groups.some(function (row) {
    return row && row.group === 'drift';
  }));
  var activeDomain = active && (active.comparisonDomain || null);
  var isSelectedComparison = isComparison && (!expectedDomain || activeDomain === null || activeDomain === expectedDomain);
  var ready = !!(model && model.actions && model.actions.canCompare);
  return {
    state: isSelectedComparison ? 'running' : ready ? 'ready' : 'blocked',
    reasonCode: isSelectedComparison ? null : model && model.compareGate && model.compareGate.reasonCode || null,
    job: isSelectedComparison ? publicJob(active) : null
  };
}

function attach(result, domain) {
  if (!result || !result.ok) return result;
  return Object.assign({}, result, { comparison: status(undefined, domain) });
}

function blocked(result) {
  return {
    ok: false,
    status: result && result.status || 409,
    error: 'design-comparison-unavailable',
    reasonCode: result && result.error || 'comparison-unavailable'
  };
}

function start(request) {
  var shape = exact(request, ['expectedGenerationRevision']) ||
    exact(request, ['expectedGenerationRevision', 'domain']);
  var domain = request && Object.prototype.hasOwnProperty.call(request, 'domain') ? request.domain : null;
  if (!shape || domain !== null && domain !== 'tokens' && domain !== 'components' ||
      !/^sha256:[a-f0-9]{64}$/.test(String(request.expectedGenerationRevision || ''))) {
    return { ok: false, status: 400, error: 'bad-design-comparison-request' };
  }
  var snap = catalog.snapshot();
  if (!snap.ok) return snap;
  var conflict = catalog.checkRevision(snap, request.expectedGenerationRevision);
  if (conflict) return conflict;

  var current = status(snap.active, domain);
  if (current.state !== 'ready') return blocked({ status: 409, error: current.reasonCode });

  var planRequest = domain ? { scope: 'drift', domain: domain } : { scope: 'drift' };
  var planned = sync.plan(planRequest);
  if (!planned || !planned.ok) return blocked(planned);
  var plan = planned.plan;
  // A future planner change must fail closed instead of silently turning the
  // Design comparison button into a live Figma read or a warning bypass.
  if (!plan || plan.scope !== 'drift' || plan.estimatedReads !== 0 ||
      (plan.comparisonDomain || null) !== domain ||
      !Array.isArray(plan.groups) || plan.groups.length !== 1 || plan.groups[0] !== 'drift' ||
      !Array.isArray(plan.warnings) || plan.warnings.length !== 0) {
    return { ok: false, status: 500, error: 'design-comparison-contract-invalid' };
  }
  var started = sync.start({ planId: plan.id, warningsAcknowledged: [] });
  return started && started.ok ? Object.assign({}, started, { comparison: status(undefined, domain) }) : blocked(started);
}

module.exports = { status: status, attach: attach, start: start };
