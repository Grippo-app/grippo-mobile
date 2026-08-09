'use strict';

// Child processes should inherit the user's normal shell context (PATH, HOME,
// keychain helpers, locale), but never Figma REST/PAT secrets, writer
// capabilities, or runtime preload hooks. Figma data access for this template
// goes through the OAuth MCP session only.

function isFigmaSecretKey(key) {
  var k = String(key || '').toUpperCase();
  var allowed = {
    FIGMA_APP_TOKENS: 1,
    FIGMA_TOKEN_MODE: 1,
    FIGMA_PIPELINE_RUN_ID: 1,
    FIGMA_SPEC_SCREENS_DIR: 1,
    FIGMA_SCREEN_CACHE_ROOT: 1,
    FIGMA_REPORTS_DIR: 1,
    FIGMA_IMPL_MODEL: 1,
    FIGMA_SCREEN_IMPL_MAP: 1,
    FIGMA_SPEC_IMPL_FILES: 1,
    FIGMA_SPEC_IMPL_ROOTS: 1,
    FIGMA_APP_TOKEN_FILES: 1,
    FIGMA_APP_TOKEN_ROOTS: 1,
    FIGMA_APP_TOKENS_OUT: 1
  };
  if (allowed[k]) return false;
  if (k === 'X_FIGMA_TOKEN') return true;
  if (k.indexOf('FIGMA') < 0) return false;
  // PAT is anchored on segment boundaries so it no longer matches inside PATH (e.g. the
  // non-secret FIGMA_*_PATH config vars). TOKEN/KEY stay substrings; the allowlist above
  // (checked first) covers the legitimate FIGMA_*_TOKEN_*/_PATH config keys.
  return /(TOKEN|(?:^|_)PAT(?:_|$)|SECRET|PASSWORD|BEARER|AUTH|API[_-]?KEY|CREDENTIAL|COOKIE|SESSION|JWT|PRIVATE|KEY)/.test(k);
}

function isWriterCapabilityKey(key) {
  return /^(?:ORCHESTRATOR_WRITER_(?:SESSION_ID|STEM|LEASE_ID|LEASE_TOKEN|DELEGATION_TOKEN)|ORCHESTRATOR_TASK_PREP_NO_QUESTIONS)$/.test(String(key || ''));
}

// Crash failpoints and fixture replacement sources belong to the suites that
// spawn a script directly. Inherited from whatever shell launched the site,
// one of these would let an environment variable abort a live transaction or
// overwrite a task file mid-finalization.
function isTestInjectionKey(key) {
  var k = String(key || '');
  return k === 'FINALIZE_FAILPOINT' || k.indexOf('FINALIZE_TEST_') === 0;
}

function isRuntimeInjectionKey(key) {
  return /^(?:NODE_OPTIONS|NODE_PATH|ELECTRON_RUN_AS_NODE|LD_PRELOAD|DYLD_INSERT_LIBRARIES|DYLD_LIBRARY_PATH|BASH_ENV)$/.test(String(key || ''));
}

function childEnv(extra) {
  var env = {};
  var keys = Object.keys(process.env || {});
  for (var i = 0; i < keys.length; i++) {
    var k = keys[i];
    // Writer authority is minted per exact child by sessions.js. Never copy a
    // credential-bearing shell/site environment or runtime preload hooks into
    // unrelated subprocesses.
    if (isFigmaSecretKey(k) || isWriterCapabilityKey(k) || isRuntimeInjectionKey(k) ||
        isTestInjectionKey(k)) continue;
    env[k] = process.env[k];
  }
  if (extra && typeof extra === 'object') {
    var extraKeys = Object.keys(extra);
    for (var j = 0; j < extraKeys.length; j++) {
      var ek = extraKeys[j];
      if (isFigmaSecretKey(ek) || isRuntimeInjectionKey(ek) || isTestInjectionKey(ek)) continue;
      env[ek] = extra[ek];
    }
  }
  return env;
}

module.exports = { childEnv: childEnv, isTestInjectionKey: isTestInjectionKey };
