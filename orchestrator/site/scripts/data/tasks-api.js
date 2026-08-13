import { requestJson } from './request-json.js';

  // ----------------------------------------------------------------------
  // Thin task-data client for the Board panel.
  //
  // Task bodies and summary data are read through bounded /api/tasks/*
  // endpoints. This keeps installed UI assets separate from mutable project
  // data when ORCHESTRATOR_PROJECT_ROOT points at a generated project.
  //
  // Error shape (always rejection with a structured object):
  //   { kind: 'fetch-failed' }                            network / CORS
  //   { kind: 'not-found',           status: 404 }         missing task source
  //   { kind: 'invalid-*-json' }                            response schema/JSON
  //
  // The board panel inspects err.kind and renders an appropriate banner.
  // ----------------------------------------------------------------------

  function reject(kind, extra) {
    var err = { kind: kind };
    if (extra && typeof extra === 'object') {
      var k = Object.keys(extra);
      for (var i = 0; i < k.length; i++) err[k[i]] = extra[k[i]];
    }
    return Promise.reject(err);
  }

  function fetchText(path) {
    var resp;
    try {
      resp = fetch(path, { cache: 'no-store' });
    } catch (e) {
      return reject('fetch-failed');
    }
    return resp.then(function (r) {
      return r.text().then(function (text) {
        if (r.ok) return text;
        var body = null;
        try { body = JSON.parse(text); } catch (error) {
          return reject(r.status === 404 ? 'not-found' : 'http-error', { status: r.status });
        }
        if (body && typeof body.error === 'string' && body.error) {
          var extra = { status: r.status };
          if (body.integrity && typeof body.integrity === 'object') extra.integrity = body.integrity;
          if (body.active && typeof body.active === 'object') extra.active = body.active;
          if (typeof body.reasonCode === 'string') extra.reasonCode = body.reasonCode;
          if (body.recovered === true) extra.recovered = true;
          return reject(body.error, extra);
        }
        return reject(r.status === 404 ? 'not-found' : 'http-error', { status: r.status });
      }, function () {
        return reject('invalid-response', { status: r.status });
      });
    }, function (e) {
      return reject('fetch-failed');
    });
  }

  function validTaskStem(value) {
    if (typeof value !== 'string' || value.length > 120) return false;
    var match = /^TASK_([1-9][0-9]*)_[A-Za-z0-9_]+$/.exec(value);
    if (!match) return false;
    var number = Number(match[1]);
    return Number.isSafeInteger(number) && number > 0 && String(number) === match[1];
  }

  function loadTaskSummary(filters) {
    filters = filters || {};
    var params = new URLSearchParams();
    ['column', 'search', 'origin', 'blocker', 'dependency', 'cursor', 'context', 'sort'].forEach(function (key) {
      if (filters[key] != null && filters[key] !== '') params.set(key, String(filters[key]));
    });
    if (filters.needsAction) params.set('needsAction', 'true');
    if (filters.limit != null) params.set('limit', String(filters.limit));
    var url = '/api/tasks/summary' + (params.toString() ? '?' + params.toString() : '');
    return requestJson(url, { cache: 'no-store' }).then(function (parsed) {
      if (!parsed || parsed.schemaVersion !== 1 || typeof parsed.revision !== 'string' ||
          !parsed.columns || ['backlog', 'pending', 'todo', 'done'].some(function (column) { return !Array.isArray(parsed.columns[column]); })) {
        return reject('invalid-task-summary-json');
      }
      return parsed;
    });
  }

  function loadTaskFile(folder, stem) {
    return fetchText('/api/tasks/file?column=' + encodeURIComponent(folder) + '&stem=' + encodeURIComponent(stem));
  }

  // Fresh canonical task-corpus diagnostics. Unlike the taskIntegrity field in
  // /api/state (which is intentionally short-lived cached for the SSE hot path),
  // this endpoint validates the filesystem + INDEX on every request. The Board
  // uses it for its mutation fence and stale-index banner, so a just-repaired
  // corpus can become actionable without waiting for the state cache to expire.
  function loadTaskIntegrity() {
    var url = '/api/tasks/integrity';
    var ctrl = (typeof AbortController !== 'undefined') ? new AbortController() : null;
    var to = ctrl ? setTimeout(function () { try { ctrl.abort(); } catch (e) {} }, 15000) : null;
    return requestJson(url, {
      cache: 'no-store',
      signal: ctrl ? ctrl.signal : undefined
    }).then(function (body) {
      if (to) clearTimeout(to);
      var hasBlocking = body && Array.isArray(body.findings) && body.findings.some(function (item) {
        return item && (item.severity === 'error' || item.severity === 'blocker');
      });
      if (!body || typeof body !== 'object' || body.version !== 1 || typeof body.ok !== 'boolean' ||
          ['fresh', 'stale', 'invalid'].indexOf(body.indexStatus) < 0 ||
          !Array.isArray(body.affectedStems) || !Array.isArray(body.findings) ||
          body.ok === hasBlocking || (body.ok && body.indexStatus !== 'fresh')) {
        return reject('invalid-task-integrity-json');
      }
      return body;
    }, function (error) {
      if (to) clearTimeout(to);
      throw error;
    });
  }

  // Two-phase dead-owner recovery.  Inspection is read-only and returns an
  // exact lock hash; mutation sends only that hash back through the normal
  // CSRF/origin-protected JSON rail.  The server repeats every liveness and
  // writer-tree proof under the canonical per-stem mutex before detaching.
  function loadTaskLockRecovery(stem) {
    var url = '/api/tasks/lock-recovery?stem=' + encodeURIComponent(stem);
    return requestJson(url, { cache: 'no-store' }).then(function (body) {
      if (!body || body.version !== 1 || body.ok !== true || body.operation !== 'owner-status' ||
          body.stem !== stem || !/^sha256:[a-f0-9]{64}$/.test(body.lockHash || '') ||
          typeof body.recoverable !== 'boolean' || typeof body.reason !== 'string' ||
          typeof body.ownerState !== 'string') {
        return reject('invalid-lock-recovery-json');
      }
      return body;
    });
  }

  // Phase one of the deterministic Drop transaction. The server runs the same
  // canonical validateAction('drop')/impact hash logic as transition-task-state;
  // the response is safe to display and contains every live dependent (including
  // dependencies not projected into INDEX). This call is strictly read-only.
  function loadDropImpact(stem) {
    var url = '/api/tasks/drop-impact?stem=' + encodeURIComponent(stem);
    var ctrl = (typeof AbortController !== 'undefined') ? new AbortController() : null;
    var to = ctrl ? setTimeout(function () { try { ctrl.abort(); } catch (e) {} }, 15000) : null;
    return requestJson(url, {
      cache: 'no-store',
      signal: ctrl ? ctrl.signal : undefined
    }).then(function (body) {
      if (to) clearTimeout(to);
      var dependents = body && Array.isArray(body.dependents) ? body.dependents : null;
      var canonicalDependents = dependents && dependents.every(function (item) {
        return validTaskStem(item);
      }) && JSON.stringify(dependents) === JSON.stringify(Array.from(new Set(dependents)).sort());
      if (!body || body.version !== 1 || body.ok !== true || body.operation !== 'inspect-drop' ||
          body.stem !== stem ||
          ['backlog', 'pending', 'todo', 'done', 'corrupt'].indexOf(body.state) < 0 ||
          !canonicalDependents ||
          !/^sha256:[a-f0-9]{64}$/.test(body.sourceRevision || '') ||
          !/^sha256:[a-f0-9]{64}$/.test(body.impactHash || '')) {
        return reject('invalid-drop-impact-json');
      }
      return body;
    }, function (error) {
      if (to) clearTimeout(to);
      throw error;
    });
  }

  function validatedJson(url, code, validate, requestOptions) {
    return requestJson(url, Object.assign({ cache: 'no-store' }, requestOptions || {})).then(function (body) {
      if (!validate(body)) return reject(code);
      return body;
    });
  }

  function exactObjectKeys(value, fields) {
    return !!value && typeof value === 'object' && !Array.isArray(value) &&
      Object.keys(value).sort().join('\0') === fields.slice().sort().join('\0');
  }

  function loadTaskDetails(stem, options) {
    return validatedJson('/api/tasks/' + encodeURIComponent(stem) + '/details',
      'invalid-task-details-json', function (body) {
        return exactObjectKeys(body, [
          'advancedAvailable', 'activitySummary', 'appValidation', 'artifactSummary',
          'blockers', 'currentWork', 'dependencies', 'designIssues', 'identity',
          'lastActivity', 'limitations', 'origin', 'outcome', 'partial',
          'primaryAction', 'recovery', 'requirement', 'retryRecovery', 'revision', 'schemaVersion',
          'secondaryActions', 'state'
        ]) && body.schemaVersion === 1 && /^sha256:[a-f0-9]{64}$/.test(body.revision || '') &&
          body.identity && body.identity.stem === stem &&
          body.state && body.origin && body.primaryAction &&
          Array.isArray(body.secondaryActions) && Array.isArray(body.blockers) &&
          body.dependencies && body.requirement && body.outcome &&
          body.currentWork && ['intake', 'questions', 'awaiting-user', 'running',
            'result', 'next-action'].indexOf(body.currentWork.kind) >= 0 &&
          body.activitySummary && body.artifactSummary &&
          typeof body.partial === 'boolean' && Array.isArray(body.limitations) &&
          body.advancedAvailable === true;
      }, options && { signal: options.signal });
  }

  function detailPageUrl(stem, section, options) {
    options = options || {};
    var params = new URLSearchParams();
    ['kind', 'cursor', 'limit'].forEach(function (key) {
      if (options[key] != null && options[key] !== '') params.set(key, String(options[key]));
    });
    return '/api/tasks/' + encodeURIComponent(stem) + '/' + section +
      (params.toString() ? '?' + params.toString() : '');
  }

  function loadTaskActivity(stem, options) {
    return validatedJson(detailPageUrl(stem, 'activity', options),
      'invalid-task-activity-json', function (body) {
        return exactObjectKeys(body, [
          'events', 'groups', 'limitations', 'nextCursor', 'outcomeDigest',
          'partial', 'revision', 'schemaVersion', 'stem', 'summary'
        ]) && body.schemaVersion === 1 && body.stem === stem &&
          /^sha256:[a-f0-9]{64}$/.test(body.revision || '') && body.summary &&
          Array.isArray(body.outcomeDigest) &&
          Array.isArray(body.groups) && Array.isArray(body.events) &&
          (body.nextCursor === null || typeof body.nextCursor === 'string') &&
          typeof body.partial === 'boolean' && Array.isArray(body.limitations);
      }, options && { signal: options.signal });
  }

  function loadTaskArtifacts(stem, options) {
    return validatedJson(detailPageUrl(stem, 'artifacts', options),
      'invalid-task-artifacts-json', function (body) {
        return exactObjectKeys(body, [
          'artifacts', 'groups', 'limitations', 'nextCursor', 'partial',
          'revision', 'schemaVersion', 'stem', 'taskSourceRevision'
        ]) && body.schemaVersion === 1 && body.stem === stem &&
          /^sha256:[a-f0-9]{64}$/.test(body.revision || '') &&
          /^sha256:[a-f0-9]{64}$/.test(body.taskSourceRevision || '') &&
          Array.isArray(body.groups) && Array.isArray(body.artifacts) &&
          (body.nextCursor === null || typeof body.nextCursor === 'string') &&
          typeof body.partial === 'boolean' && Array.isArray(body.limitations);
      }, options && { signal: options.signal });
  }

  function loadTaskAdvanced(stem, sections, options) {
    var params = new URLSearchParams();
    if (Array.isArray(sections) && sections.length) params.set('sections', sections.join(','));
    return validatedJson('/api/tasks/' + encodeURIComponent(stem) + '/advanced' +
      (params.toString() ? '?' + params.toString() : ''),
      'invalid-task-advanced-json', function (body) {
        return exactObjectKeys(body, [
          'limitations', 'partial', 'schemaVersion', 'sections', 'stem'
        ]) && body.schemaVersion === 1 && body.stem === stem &&
          body.sections && typeof body.sections === 'object' &&
          !Array.isArray(body.sections) && typeof body.partial === 'boolean' &&
          Array.isArray(body.limitations);
      }, options && { signal: options.signal });
  }

  function loadTaskCheckpoints(stem, options) {
    return validatedJson('/api/tasks/' + encodeURIComponent(stem) + '/checkpoints',
      'invalid-task-checkpoints-json', function (body) {
        return exactObjectKeys(body, [
          'checkpoints', 'limitations', 'partial', 'schemaVersion', 'stem'
        ]) && body.schemaVersion === 1 && body.stem === stem &&
          Array.isArray(body.checkpoints) && typeof body.partial === 'boolean' &&
          Array.isArray(body.limitations);
      }, options && { signal: options.signal });
  }

  function loadTaskActionPrompt(stem, action, input) {
    var base = '/api/tasks/' + encodeURIComponent(stem) + '/action-prompt';
    var request = input ? postJson(base, {
      actionRevision: action && action.actionRevision,
      answers: input.answers,
      questionRound: input.questionRound,
      expectedQuestionsRevision: input.expectedQuestionsRevision
    }, 45000) : requestJson(base + '?actionRevision=' +
      encodeURIComponent(action && action.actionRevision || ''), { cache: 'no-store' });
    return request.then(function (body) {
      var keys = body && typeof body === 'object' && !Array.isArray(body)
        ? Object.keys(body).sort().join('\0') : '';
      var expected = [
        'actionRevision', 'expiresAt', 'manualFallback', 'promptHash',
        'schemaVersion', 'stem', 'taskSourceRevision', 'text'
      ].sort().join('\0');
      var expires = body && Date.parse(body.expiresAt);
      if (keys !== expected || body.schemaVersion !== 1 || body.stem !== stem ||
          body.actionRevision !== (action && action.actionRevision) ||
          typeof body.text !== 'string' || !body.text || body.text.length > 60000 ||
          !/^sha256:[a-f0-9]{64}$/.test(body.promptHash || '') ||
          !/^sha256:[a-f0-9]{64}$/.test(body.taskSourceRevision || '') ||
          !Number.isFinite(expires) || expires <= Date.now() ||
          expires > Date.now() + 5 * 60 * 1000 + 5000 ||
          body.manualFallback !== true) return reject('invalid-task-action-prompt-json');
      return body;
    });
  }

  function previewTaskRetry(stem, action, checkpointHash) {
    return postJson('/api/tasks/' + encodeURIComponent(stem) + '/retry-preview', {
      checkpointId: action && action.checkpointId,
      checkpointHash: checkpointHash,
      actionRevision: action && action.actionRevision
    }, 45000);
  }

  // GET /api/figma/evidence — compact per-task Figma comparison/evidence
  // summary for the Board modal. Detailed report JSON stays server-side; the
  // endpoint returns a bounded, display-ready summary.
  function loadFigmaEvidence(stem) {
    return validatedJson('/api/figma/evidence?stem=' + encodeURIComponent(stem),
      'invalid-figma-evidence-json', function (body) {
        return !!body && typeof body === 'object' && !Array.isArray(body);
      });
  }

  // Tiny JSON POST helper shared by the queue calls below. Resolves the
  // parsed body on any successful 2xx response, rejects with the same {kind} shape as the
  // loaders on any error.
  function postJson(url, payload, timeoutMs) {
    // A 15s timeout so the promise ALWAYS settles — a stalled connection must
    // not leave a caller's button disabled forever waiting on a hung fetch.
    var ctrl = (typeof AbortController !== 'undefined') ? new AbortController() : null;
    var to = ctrl ? setTimeout(function () { try { ctrl.abort(); } catch (e) {} }, timeoutMs || 15000) : null;
    var headers = { 'content-type': 'application/json' };
    if (typeof window !== 'undefined' && window.__ORCHESTRATOR_CSRF__) headers['x-orchestrator-csrf'] = window.__ORCHESTRATOR_CSRF__;
    return requestJson(url, {
      method: 'POST',
      headers: headers,
      body: JSON.stringify(payload || {}),
      cache: 'no-store',
      signal: ctrl ? ctrl.signal : undefined
    }).then(function (body) {
      if (to) clearTimeout(to);
      return body;
    }, function (error) {
      if (to) clearTimeout(to);
      throw error;
    });
  }

  // Deterministic backlog publication. The server owns numbering, slugging,
  // no-clobber file publication and canonical INDEX regeneration; this call
  // never waits for or delegates those effects to Claude. `idempotencyKey`
  // must be retained by the caller across HTTP retries/double-clicks.
  function createBacklog(title, body, opts) {
    opts = opts || {};
    return postJson('/api/tasks/backlog', {
      title: title,
      body: body,
      idempotencyKey: opts.idempotencyKey,
      originStem: opts.originStem || null,
      dedupKey: opts.dedupKey || null,
      dedupReport: opts.dedupReport || null
    }, 130000);
  }

  function loadTaskInbox() {
    return validatedJson('/api/tasks/inbox', 'invalid-task-inbox-json', function (body) {
      return !!body && body.schemaVersion === 1 && Array.isArray(body.entries) &&
        body.entries.every(function (entry) {
          return entry && /^INBOX_[a-f0-9]{40}$/.test(entry.id || '') &&
            typeof entry.title === 'string' &&
            typeof entry.createdAt === 'string' && Number.isFinite(Date.parse(entry.createdAt));
        });
    });
  }

  function saveTaskInbox(title, body, opts) {
    opts = opts || {};
    return postJson('/api/tasks/inbox', {
      title: title,
      body: body,
      idempotencyKey: opts.idempotencyKey
    });
  }

  function publishTaskInbox(id) {
    return postJson('/api/tasks/inbox/publish', { id: id }, 130000);
  }

  function executeTaskAction(stem, action, confirmation, input) {
    input = input || {};
    return postJson('/api/tasks/actions', {
      stem: stem,
      actionId: action && action.id,
      actionRevision: action && action.actionRevision,
      action: action && action.kind,
      expectedState: action && action.expectedState,
      expectedSourceRevision: action && action.expectedSourceRevision,
      checkpointId: action && action.checkpointId || null,
      confirmation: confirmation == null ? null : confirmation,
      confirmationToken: input.confirmationToken || null,
      answers: input.answers || null,
      questionRound: input.questionRound == null ? null : input.questionRound,
      expectedQuestionsRevision: input.expectedQuestionsRevision || null,
      liveSessionId: input.liveSessionId || null,
      expectedSessionRevision: input.expectedSessionRevision || null,
      // Task action admission caps opaque keys at 128 ASCII characters.
      // A fresh random key is already scoped by the canonical request body in
      // the durable receipt store; embedding a long stem/revision here would
      // only risk exceeding that public contract.
      idempotencyKey: input.idempotencyKey || creationKey('task-action')
    }, 45000).then(function (body) {
      var keys = body && typeof body === 'object' && !Array.isArray(body)
        ? Object.keys(body).sort().join('\0') : '';
      var expectedKeys = [
        'action', 'idempotentReplay', 'requestId', 'resultingActionRevision',
        'schemaVersion', 'sessionId', 'status', 'taskSummaryRevision'
      ].sort().join('\0');
      if (keys !== expectedKeys || body.schemaVersion !== 1 ||
          body.action !== action.kind ||
          ['accepted', 'continued', 'completed', 'already-active'].indexOf(body.status) < 0 ||
          body.requestId !== null && typeof body.requestId !== 'string' ||
          body.sessionId !== null && typeof body.sessionId !== 'string' ||
          !/^sha256:[a-f0-9]{64}$/.test(body.resultingActionRevision || '') ||
          !/^sha256:[a-f0-9]{64}$/.test(body.taskSummaryRevision || '') ||
          typeof body.idempotentReplay !== 'boolean') {
        return reject('invalid-task-action-response');
      }
      return body;
    });
  }

  function creationKey(namespace, stableValue) {
    var ns = String(namespace || 'backlog').replace(/[^A-Za-z0-9_.:-]+/g, '_').slice(0, 40) || 'backlog';
    if (stableValue == null) {
      var random = (typeof crypto !== 'undefined' && crypto.randomUUID)
        ? crypto.randomUUID()
        : Date.now().toString(36) + '-' + Math.random().toString(36).slice(2) + '-' + Math.random().toString(36).slice(2);
      return (ns + ':' + random).slice(0, 240);
    }
    var raw = String(stableValue);
    var h1 = 2166136261, h2 = 2246822519;
    for (var i = 0; i < raw.length; i++) {
      h1 ^= raw.charCodeAt(i); h1 = Math.imul(h1, 16777619);
      h2 ^= raw.charCodeAt(i); h2 = Math.imul(h2, 3266489917);
    }
    var visible = raw.replace(/[^A-Za-z0-9_.:-]+/g, '_').slice(0, 150);
    return (ns + ':' + visible + ':h' + (h1 >>> 0).toString(16) + (h2 >>> 0).toString(16)).slice(0, 240);
  }

  function retryShallowIntake(stem, sourceHash) {
    return postJson('/api/tasks/intake/retry', { stem: stem, expectedSourceHash: sourceHash });
  }

  function dismissShallowIntake(stem, sourceHash) {
    return postJson('/api/tasks/intake/dismiss', { stem: stem, expectedSourceHash: sourceHash });
  }

  function loadBacklogSource(stem) {
    return validatedJson('/api/tasks/backlog?stem=' + encodeURIComponent(stem),
      'invalid-backlog-source-json', function (body) {
        return !!body && typeof body === 'object' && !Array.isArray(body);
    });
  }

  function editBacklog(stem, expectedSourceHash, markdown) {
    return postJson('/api/tasks/backlog/edit', {
      stem: stem,
      expectedSourceHash: expectedSourceHash,
      markdown: markdown
    }, 130000);
  }

  function recoverTaskLock(stem, expectedLockHash) {
    return postJson('/api/tasks/lock-recovery', {
      stem: stem,
      expectedLockHash: expectedLockHash
    }, 45000);
  }

  // Resume one durable finalize-task transaction. Revision + raw marker ETag
  // are both required so a stale tab cannot replay an already-advanced phase.
  function resumeFinalization(finalization) {
    return postJson('/api/finalizations/resume', {
      stem: finalization && finalization.stem,
      expectedRevision: finalization && finalization.revision,
      expectedEtag: finalization && finalization.etag
    });
  }

  // The integration transaction (plan §10). Preview is read-only and returns
  // the exact candidate diff plus the exact blocking paths; start and resume
  // drive the same write-ahead log, so a repeated click is idempotent rather
  // than a second commit.
  function previewIntegration(stem) { return postJson('/api/integrations/preview', { stem: stem }); }
  function startIntegration(stem) { return postJson('/api/integrations/start', { stem: stem }, 600000); }
  function resumeIntegration(stem) { return postJson('/api/integrations/resume', { stem: stem }, 600000); }
  // The two operator exits from a wedged state. Abandon carries the exact
  // integration id: it is the confirmation that a human read the record.
  function abandonIntegration(stem, integrationId) {
    return postJson('/api/integrations/abandon', { stem: stem, integrationId: integrationId });
  }
  function releaseWorktree(stem) { return postJson('/api/worktrees/release', { stem: stem }); }

  // Generic interactive sessions (Setup / Wizard / Board task terminals). `key`
  // is the context: "setup" | "task:<stem>". `sessionStart`
  // spawns (or no-ops if already running) and optionally sends `prompt` as the
  // first turn; `sessionSend` relays a user answer; events are polled per-key.
  function sessionStart(key, prompt) { return postJson('/api/session/start', { key: key, prompt: prompt || '' }); }
  // Figma actions never send executable prompt text. The server resolves the
  // exact key/action pair against the shared prompt module and persisted state.
  function figmaSessionAction(key, action) { return postJson('/api/session/start', { key: key, figmaAction: action }); }
  function sessionSend(key, text)    { return postJson('/api/session/send', { key: key, text: text }); }
  function sessionCancel(key)        { return postJson('/api/session/cancel', { key: key }); }
  function sessionEvents(key, since) {
    return requestJson('/api/session/events?key=' + encodeURIComponent(key) + '&since=' + (since || 0), { cache: 'no-store' });
  }

  // Claude CLI control (header install/login buttons).
  function cliInstall()        { return postJson('/api/cli/install', {}); }
  function cliLogin(fresh)     { return postJson('/api/cli/login', fresh ? { fresh: true } : {}); }
  function cliLoginCode(code)  { return postJson('/api/cli/login/code', { code: code }); }
  function cliLoginCancel()    { return postJson('/api/cli/login/cancel', {}); }

  // Figma MCP connector — force an immediate re-probe of `claude mcp list`
  // (the header pill's + the Figma panel's "Re-check", and the live poll while
  // the user authenticates the project's local connector via /mcp).
  function figmaRecheck()      { return postJson('/api/figma/recheck', {}); }

  // Bind this project: add a local-scoped "figma" MCP server (server runs
  // `claude mcp add`). OAuth is still the user's interactive /mcp step.
  function figmaAddLocal()     { return postJson('/api/figma/add-local', {}); }

  // Open the native terminal with `claude` running in the project (macOS only).
  function figmaOpenTerminal() { return postJson('/api/figma/open-terminal', {}); }

  function figmaTest(expectedFileKey, force) {
    return postJson('/api/figma/test', { expectedFileKey: expectedFileKey || null, force: force === true });
  }
  function figmaIntegrationReset(expectedConfigRevision, expectedGenerationId, idempotencyKey) {
    // The server deletes the whole sync history inline before answering, and the
    // dialog's own copy promises "a few minutes". The default 15s abort turned a
    // successful reset into a "server did not respond" error, and the retry then
    // failed as an idempotency conflict against the already-bumped revision.
    return postJson('/api/figma/integration/reset', {
      expectedConfigRevision: expectedConfigRevision,
      expectedGenerationId: expectedGenerationId || null,
      idempotencyKey: idempotencyKey
    }, 600000);
  }
  function figmaFileVerify(urlOrKey, expectedConfigRevision) {
    return postJson('/api/figma/file/verify', { urlOrKey: urlOrKey, expectedConfigRevision: expectedConfigRevision });
  }
  function figmaFileSave(candidateId, expectedConfigRevision) {
    return postJson('/api/figma/file', { candidateId: candidateId, expectedConfigRevision: expectedConfigRevision });
  }
  function figmaSyncPlan(scope) {
    return postJson('/api/figma/sync/plan', { scope: scope });
  }
  function figmaSyncStart(planId, warningsAcknowledged) {
    return postJson('/api/figma/sync/start', { planId: planId, warningsAcknowledged: warningsAcknowledged || [] });
  }
  function figmaSyncCancel(jobId, expectedRevision) {
    return postJson('/api/figma/sync/cancel', { jobId: jobId, expectedRevision: expectedRevision });
  }
  function figmaSyncHistory(cursor, limit) {
    var url = '/api/figma/sync/history?limit=' + encodeURIComponent(limit || 20);
    if (cursor) url += '&cursor=' + encodeURIComponent(cursor);
    return validatedJson(url, 'invalid-figma-history-json', function (body) {
      return !!body && typeof body === 'object' && body.ok === true &&
        Array.isArray(body.items) && (body.nextCursor === null || typeof body.nextCursor === 'string');
    });
  }

  export const tasksApi = {
    loadTaskSummary: loadTaskSummary,
    loadTaskFile: loadTaskFile,
    loadTaskIntegrity: loadTaskIntegrity,
    loadTaskLockRecovery: loadTaskLockRecovery,
    loadDropImpact: loadDropImpact,
    loadTaskDetails: loadTaskDetails,
    loadTaskActivity: loadTaskActivity,
    loadTaskArtifacts: loadTaskArtifacts,
    loadTaskAdvanced: loadTaskAdvanced,
    loadTaskCheckpoints: loadTaskCheckpoints,
    loadTaskActionPrompt: loadTaskActionPrompt,
    previewTaskRetry: previewTaskRetry,
    loadFigmaEvidence: loadFigmaEvidence,
    createBacklog: createBacklog,
    loadTaskInbox: loadTaskInbox,
    saveTaskInbox: saveTaskInbox,
    publishTaskInbox: publishTaskInbox,
    executeTaskAction: executeTaskAction,
    creationKey: creationKey,
    retryShallowIntake: retryShallowIntake,
    dismissShallowIntake: dismissShallowIntake,
    loadBacklogSource: loadBacklogSource,
    editBacklog: editBacklog,
    recoverTaskLock: recoverTaskLock,
    resumeFinalization: resumeFinalization,
    previewIntegration: previewIntegration,
    startIntegration: startIntegration,
    resumeIntegration: resumeIntegration,
    abandonIntegration: abandonIntegration,
    releaseWorktree: releaseWorktree,
    cliInstall: cliInstall,
    cliLogin: cliLogin,
    cliLoginCode: cliLoginCode,
    cliLoginCancel: cliLoginCancel,
    figmaRecheck: figmaRecheck,
    figmaAddLocal: figmaAddLocal,
    figmaOpenTerminal: figmaOpenTerminal,
    figmaTest: figmaTest,
    figmaIntegrationReset: figmaIntegrationReset,
    figmaFileVerify: figmaFileVerify,
    figmaFileSave: figmaFileSave,
    figmaSyncPlan: figmaSyncPlan,
    figmaSyncStart: figmaSyncStart,
    figmaSyncCancel: figmaSyncCancel,
    figmaSyncHistory: figmaSyncHistory,
    sessionStart: sessionStart,
    figmaSessionAction: figmaSessionAction,
    sessionSend: sessionSend,
    sessionCancel: sessionCancel,
    sessionEvents: sessionEvents
  };
