'use strict';

// Server-owned Figma action prompts. The browser sends only an exact action id;
// this module imports the same ESM prompt builders used by Copy, joins them to
// the allowlisted session key, and supplies dynamic values from server state.

var path = require('path');
var pathToFileURL = require('url').pathToFileURL;
var figmaMod = require('./figma');
var requestLimits = require('./requests');
var projectConfigUpdate = require('./project-config-update');
var screenTokenPlans = require('./screen-token-plans');

var promptModulePromise = null;
var HASH_RE = /^sha256:[a-f0-9]{64}$/;
var NONCE_RE = /^[a-f0-9]{32}$/;
var JOB_RE = /^fsj-[a-f0-9]{32}$/;

function exactKeys(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}

function internalAction(key) {
  return key === 'figma:fileaccess' || key.indexOf('figma:sync-') === 0;
}

function validServerContext(key, action, context) {
  if (key === 'figma:whoami' && action === 'whoami') {
    return exactKeys(context, ['figmaFileKey', 'verificationNonce']) &&
      typeof context.figmaFileKey === 'string' && NONCE_RE.test(String(context.verificationNonce || ''));
  }
  if (key === 'figma:fileaccess' && action === 'file-access') {
    return exactKeys(context, ['figmaFileKey', 'accessNonce', 'accountFingerprint', 'receiptPath']) &&
      typeof context.figmaFileKey === 'string' && NONCE_RE.test(context.accessNonce) &&
      HASH_RE.test(context.accountFingerprint) &&
      context.receiptPath === 'orchestrator/.cache/figma/integration/file-access-' + context.accessNonce + '.json';
  }
  var match = /^figma:sync-(tokens|components)$/.exec(key);
  if (!match || action !== 'sync-' + match[1]) return false;
  var keys = ['figmaFileKey', 'jobId', 'inputFingerprint', 'fileKeyFingerprint', 'stagePath'];
  if (match[1] === 'tokens') keys.push('capturePlanPath');
  if (match[1] === 'components') keys.push('tokenCapturePlanPath');
  if (!exactKeys(context, keys)) return false;
  var valid = typeof context.figmaFileKey === 'string' && JOB_RE.test(context.jobId) &&
    HASH_RE.test(context.inputFingerprint) && HASH_RE.test(context.fileKeyFingerprint) &&
    context.stagePath === 'orchestrator/.cache/figma/generations/' + context.jobId + '/' + match[1];
  if (!valid) return false;
  return match[1] === 'tokens'
    ? context.capturePlanPath === context.stagePath + '/capture-plan.json'
    : context.tokenCapturePlanPath === context.stagePath + '/component-token-plan.json';
}

function promptModule() {
  if (!promptModulePromise) {
    promptModulePromise = import(pathToFileURL(path.join(__dirname, '..', 'scripts', 'figma-actions.js')).href);
  }
  return promptModulePromise;
}

function resolveActionWithContext(key, action, suppliedContext) {
  if (typeof action !== 'string' || !action || action.length > 40 || !/^[a-z-]+$/.test(action)) {
    return Promise.resolve({ ok: false, status: 400, error: 'bad-figma-action' });
  }
  return promptModule().then(function (prompts) {
    var figmaStatus = figmaMod.status() || {};
    var projectConfig = projectConfigUpdate.read();
    var context = Object.assign({
      figmaFileKey: projectConfig.ok && projectConfig.figmaFieldState === 'selected'
        ? projectConfig.figmaLibraryUrl
        : '',
      verificationNonce: typeof figmaStatus.verificationNonce === 'string' ? figmaStatus.verificationNonce : ''
    }, suppliedContext || {});
    var preparedTokenPlan = null;
    if (!suppliedContext && action === 'screen-pull' && typeof key === 'string' &&
        key.indexOf('figma:screens:') === 0) {
      try {
        preparedTokenPlan = screenTokenPlans.prepare(key.slice('figma:screens:'.length));
        context.screenTokenPlanPath = preparedTokenPlan.path;
      } catch (error) {
        return {
          ok: false, status: 409,
          error: screenTokenPlans.prepareErrorCode(error)
        };
      }
    }

    var prompt = prompts.figmaSessionPrompt(key, action, context);
    if (typeof prompt !== 'string' || !prompt.trim()) {
      if (preparedTokenPlan) {
        try { screenTokenPlans.fail(preparedTokenPlan.planId, 'TOKEN_TASK_ACTION_MISMATCH'); } catch (error) {}
      }
      return { ok: false, status: 400, error: 'figma-action-key-mismatch' };
    }
    if (prompt.length > requestLimits.REQUEST_PROMPT_MAX ||
        Buffer.byteLength(prompt, 'utf8') > requestLimits.REQUEST_PROMPT_MAX_BYTES ||
        prompt.indexOf('\0') >= 0) {
      if (preparedTokenPlan) {
        try { screenTokenPlans.fail(preparedTokenPlan.planId, 'TOKEN_TASK_PROMPT_INVALID'); } catch (error) {}
      }
      return { ok: false, status: 500, error: 'figma-prompt-contract-invalid' };
    }

    if ((key === 'figma:fileaccess' || key === 'figma:sync-tokens' || key === 'figma:sync-components') &&
        !prompts.parseFileKey(context.figmaFileKey)) {
      return { ok: false, status: 409, error: 'figma-file-key-invalid', detail: 'Save a valid Figma file URL or key before running this action.' };
    }
    if (key === 'figma:whoami' && !context.verificationNonce) {
      return { ok: false, status: 409, error: 'figma-verification-unavailable', detail: 'Re-check the connector before verifying its account.' };
    }

    return {
      ok: true, action: action, prompt: prompt,
      ...(preparedTokenPlan ? { screenTokenPlanId: preparedTokenPlan.planId } : {})
    };
  }, function () {
    return { ok: false, status: 500, error: 'figma-prompt-contract-unavailable' };
  });
}

function resolveAction(key, action) {
  if (typeof key === 'string' && internalAction(key)) {
    return Promise.resolve({ ok: false, status: 400, error: 'figma-internal-action-forbidden' });
  }
  return resolveActionWithContext(key, action, null);
}
function abortPreparedScreenPlan(resolved, code) {
  if (!resolved || !resolved.screenTokenPlanId) return true;
  try { return screenTokenPlans.fail(resolved.screenTokenPlanId, code || 'TOKEN_TASK_SESSION_NOT_STARTED'); }
  catch (error) { return false; }
}

// Internal orchestration rail. HTTP handlers never copy request fields into
// this context; test/sync jobs construct every value server-side.
function resolveServerAction(key, action, context) {
  if (!(internalAction(key) || key === 'figma:whoami') || !validServerContext(key, action, context)) {
    return Promise.resolve({ ok: false, status: 500, error: 'figma-server-context-invalid' });
  }
  return resolveActionWithContext(key, action, context);
}

module.exports = {
  resolveAction: resolveAction,
  resolveServerAction: resolveServerAction,
  abortPreparedScreenPlan: abortPreparedScreenPlan
};
