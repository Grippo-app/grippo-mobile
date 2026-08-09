function freezeGroups(groups) {
  return Object.freeze(Object.fromEntries(
    Object.entries(groups).map(([name, files]) => [name, Object.freeze(files)])
  ));
}

export const workspacePaths = Object.freeze([
  'orchestrator/api-contract',
  'orchestrator/figma',
  'orchestrator/site',
  'orchestrator/tasks'
]);

export const requiredRootScripts = Object.freeze({
  start: 'node orchestrator/site/server.js',
  test: 'npm run verify:fast',
  lint: 'bash orchestrator/lint.sh && bash orchestrator/skills/checks/run-all.sh',
  audit: 'npm audit --audit-level=high',
  'test:syntax': 'node orchestrator/tests/check-syntax.mjs',
  'test:ownership': 'node orchestrator/tests/run-suite.mjs --check',
  'test:site': 'node orchestrator/tests/run-suite.mjs site',
  'test:api': 'node orchestrator/tests/run-suite.mjs api',
  'test:app-run': 'node orchestrator/tests/run-suite.mjs app-run',
  'test:tooling': 'node orchestrator/tests/run-suite.mjs tooling',
  'test:tasks': 'node orchestrator/tests/run-suite.mjs tasks',
  'test:figma': 'node orchestrator/tests/run-suite.mjs figma',
  'test:crash-recovery:task-state':
    'node orchestrator/tests/run-suite.mjs crash-recovery:task-state',
  'test:crash-recovery:intake':
    'node orchestrator/tests/run-suite.mjs crash-recovery:intake',
  'test:crash-recovery:runtime':
    'node orchestrator/tests/run-suite.mjs crash-recovery:runtime',
  'test:crash-recovery:finalization':
    'node orchestrator/tests/run-suite.mjs crash-recovery:finalization',
  'test:crash-recovery': 'node orchestrator/tests/run-suite.mjs crash-recovery',
  'verify:fast':
    'npm run test:syntax && npm run test:ownership && npm run test:tooling && npm run lint && npm run audit && npm run test:api && npm run test:app-run && npm run test:site',
  'verify:full':
    'npm run verify:fast && npm run test:tasks && npm run test:figma && npm run test:crash-recovery'
});

export const delegatedWorkspaceScripts = Object.freeze({
  'orchestrator/api-contract': Object.freeze({
    'contract:test': 'node ../tests/run-suite.mjs api'
  }),
  'orchestrator/figma': Object.freeze({
    'figma:verify': 'node ../tests/run-suite.mjs figma'
  }),
  'orchestrator/site': Object.freeze({
    test: 'node ../tests/run-suite.mjs site',
    'test:app-run': 'node ../tests/run-suite.mjs app-run'
  }),
  'orchestrator/tasks': Object.freeze({
    test: 'node ../tests/run-suite.mjs tasks',
    'test:crash-recovery': 'node ../tests/run-suite.mjs crash-recovery'
  })
});

export const toolingTests = Object.freeze([
  'orchestrator/template-sync/tests/test-template-sync.mjs'
]);

export const crashSiteTestNames = freezeGroups({
  'crash-recovery:task-state': [
    'file-guards-append-concurrency.test.mjs',
    'file-guards-bigint.test.mjs',
    'file-guards-publication-recovery.test.mjs',
    'lock-recovery-http.test.mjs',
    'publication-recovery.test.mjs',
    'request-reservations.test.mjs',
    'runtime-owner-self-admission.test.mjs',
    'task-state-admission.test.mjs'
  ],
  'crash-recovery:intake': [
    'backlog-http.test.mjs',
    'creation-markers.test.mjs',
    'publication-guard.test.mjs',
    'runner-contract.test.mjs',
    'runner-handoff-settlement.test.mjs',
    'shallow-intake-contract.test.mjs',
    'shallow-intake-darwin-orphan.test.mjs',
    'shallow-intake-multiprocess.test.mjs',
    'shallow-intake.test.mjs',
    'shallow-owner-guard.test.mjs',
    'standby-queue.test.mjs'
  ],
  'crash-recovery:runtime': [
    'runtime-path-safety.test.mjs',
    'runtime-scan-bounds.test.mjs',
    'session-runtime-regressions.test.mjs',
    'startup-recovery-barrier.test.mjs',
    'windows-runtime-proof-native.test.mjs',
    'windows-runtime-proof.test.mjs',
    'writer-lease-auto-recovery.test.mjs',
    'writer-lease-primitives.test.mjs'
  ],
  'crash-recovery:finalization': [
    'finalizations.test.mjs'
  ]
});

export const crashTaskTestNames = freezeGroups({
  'crash-recovery:task-state': [
    'test-index-fail-closed.mjs',
    'test-task-lock.mjs',
    'test-transition-task-state.mjs',
    'test-writer-lease-recovery.mjs',
    'test-writer-lease-windows-identity.mjs'
  ],
  'crash-recovery:intake': [
    'test-create-backlog.mjs',
    'test-edit-backlog.mjs'
  ],
  'crash-recovery:runtime': [],
  'crash-recovery:finalization': [
    'test-finalize-task.mjs',
    'test-writer-lease.mjs'
  ]
});

export const crashSuiteNames = Object.freeze(Object.keys(crashSiteTestNames));

export const leafSuiteNames = Object.freeze([
  'api',
  'app-run',
  'site',
  'tooling',
  'tasks',
  'figma',
  ...crashSuiteNames
]);

export const expectedOwnershipSummary = Object.freeze({
  api: 7,
  'app-run': 10,
  site: 28,
  tooling: 1,
  tasks: 15,
  figma: 60,
  'crash-recovery:task-state': 13,
  'crash-recovery:intake': 13,
  'crash-recovery:runtime': 8,
  'crash-recovery:finalization': 3
});
