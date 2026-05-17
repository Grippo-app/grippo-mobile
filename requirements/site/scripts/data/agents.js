(function () {
  window.App = window.App || {};
  App.data = App.data || {};

  // Sub-agent inventory.
  //
  // Ordering inside each role section reflects call order:
  //   - helpers: task-intake (always first) → orchestrator → context-finder
  //     → requirements-lookup → external-review pair.
  //   - builders: scaffolds first (data-service, then feature-module),
  //     then per-cookbook builders in the same order as the README table.
  //   - validators: gates that run on every task, README order preserved.
  //
  // `what` is a one-line summary distilled from each agent's frontmatter.
  // `when` is a one-line, user-facing trigger derived from the agent's body.
  // `specPath` is relative to requirements/site/index.html, so the link
  // works under file:// without any base-href trick.
  App.data.agents = [
    // ----- Helpers (call order) -------------------------------------------
    {
      name: 'task-intake',
      role: 'helper',
      what: 'Reads the task file, classifies the change, and returns the builder + validator plan.',
      when: 'Runs automatically as the orchestrator\'s first step on every task.',
      specPath: '../sub-agents/helpers/task-intake.md'
    },
    {
      name: 'orchestrator',
      role: 'helper',
      what: 'Top-level coordinator: drives task-intake → builders → validators → external reviewer to "done".',
      when: 'The one agent the user invokes — by asking "run task TASK_<N>_<title>.md".',
      specPath: '../sub-agents/helpers/orchestrator.md'
    },
    {
      name: 'context-finder',
      role: 'helper',
      what: 'Locates existing modules, files, and signatures relevant to a task so builders don\'t re-grep.',
      when: 'Invoked by the orchestrator before a builder runs; never called directly by the user.',
      specPath: '../sub-agents/helpers/context-finder.md'
    },
    {
      name: 'requirements-lookup',
      role: 'helper',
      what: 'Maps a keyword to the exact requirements/*.md chapter and line range to read.',
      when: 'Invoked by builders when they need the canonical rule for a specific topic.',
      specPath: '../sub-agents/helpers/requirements-lookup.md'
    },
    {
      name: 'codex-review-loop',
      role: 'helper',
      what: 'External-review gate: runs the Codex plugin on the diff, classifies findings, routes them back to builders.',
      when: 'Final review gate when the Codex plugin is installed and codexEnabled permits.',
      specPath: '../sub-agents/helpers/codex-review-loop.md'
    },
    {
      name: 'internal-reviewer',
      role: 'helper',
      what: 'Local senior-reviewer fallback. Same output shape as codex-review-loop so the orchestrator wiring is reviewer-agnostic.',
      when: 'Final review gate when Codex is absent or codexEnabled is false.',
      specPath: '../sub-agents/helpers/internal-reviewer.md'
    },

    // ----- Builders (one per cookbook recipe) -----------------------------
    {
      name: 'data-service-scaffold-builder',
      role: 'builder',
      what: 'One-shot scaffold for empty :data-services:backend and :data-services:database (Database version = 1).',
      when: 'Once per project, before any endpoint-builder or room-migration-builder runs on a fresh repo.',
      specPath: '../sub-agents/builders/data-service-scaffold-builder.md'
    },
    {
      name: 'feature-module-scaffold-builder',
      role: 'builder',
      what: 'Creates a brand-new empty :ui-screen-features:<name> module + RootRouter/RootComponent wiring.',
      when: 'Adding a top-level feature that does not yet exist as a :ui-screen-features:* module.',
      specPath: '../sub-agents/builders/feature-module-scaffold-builder.md'
    },
    {
      name: 'screen-builder',
      role: 'builder',
      what: 'Adds the seven-file MVI sub-screen package inside an existing :ui-screen-features:* feature.',
      when: 'Task asks for a new screen, sub-screen, or tab inside an existing feature.',
      specPath: '../sub-agents/builders/screen-builder.md'
    },
    {
      name: 'dialog-builder',
      role: 'builder',
      what: 'Adds a new :ui-dialog-features:<name> bottom-sheet module callable via DialogController.show.',
      when: 'Task asks for a new picker, bottom sheet, modal, or a popup that returns a value.',
      specPath: '../sub-agents/builders/dialog-builder.md'
    },
    {
      name: 'data-feature-builder',
      role: 'builder',
      what: 'Adds a :data-features:<name> module (feature-api + repository + impl + Koin module wired in :shared).',
      when: 'Task introduces a new domain concept the UI needs as a <X>Feature interface.',
      specPath: '../sub-agents/builders/data-feature-builder.md'
    },
    {
      name: 'mapper-builder',
      role: 'builder',
      what: 'Adds a top-level extension-function mapper file in one of the seven :data-mappers:* directions.',
      when: 'A new domain area needs a DTO↔Entity↔Domain↔State↔Body bridge.',
      specPath: '../sub-agents/builders/mapper-builder.md'
    },
    {
      name: 'endpoint-builder',
      role: 'builder',
      what: 'Adds a new method to <Product>Api + the matching <X>Response/<X>Body DTO files.',
      when: 'A repository needs to call an endpoint that does not yet exist on the client.',
      specPath: '../sub-agents/builders/endpoint-builder.md'
    },
    {
      name: 'room-migration-builder',
      role: 'builder',
      what: 'Bumps @Database(version), writes Migration<N>To<N+1>, registers it, and exports schemas.',
      when: 'An entity schema changes — requires explicit user authorization before running.',
      specPath: '../sub-agents/builders/room-migration-builder.md'
    },
    {
      name: 'resource-builder',
      role: 'builder',
      what: 'Adds strings (all locales), drawables, icons, or fonts to :design-system:resources:provider.',
      when: 'Task introduces new copy, an illustration, an icon, or a font weight.',
      specPath: '../sub-agents/builders/resource-builder.md'
    },
    {
      name: 'cross-feature-nav-builder',
      role: 'builder',
      what: 'Wires a navigation jump from one feature into another via RootDirection / RootContract / RootComponent.',
      when: 'Task asks "open <screen-in-feature-B> from <screen-in-feature-A>" across feature module boundaries.',
      specPath: '../sub-agents/builders/cross-feature-nav-builder.md'
    },

    // ----- Validators (always-on invalidators) ----------------------------
    {
      name: 'architecture-validator',
      role: 'validator',
      what: 'Verifies every changed module\'s build.gradle.kts respects the dependency-rules chapter.',
      when: 'Always — gates every task.',
      specPath: '../sub-agents/validators/architecture-validator.md'
    },
    {
      name: 'mvi-contract-validator',
      role: 'validator',
      what: 'Verifies the seven-file MVI shape and BaseViewModel / BaseComponent contract on every new screen/dialog.',
      when: 'Always — gates every task. Findings route to screen-builder or dialog-builder.',
      specPath: '../sub-agents/validators/mvi-contract-validator.md'
    },
    {
      name: 'anti-pattern-scanner',
      role: 'validator',
      what: 'Greps the diff for every forbidden pattern listed in requirements/13-anti-patterns/.',
      when: 'Always — broadest gate; runs on every task.',
      specPath: '../sub-agents/validators/anti-pattern-scanner.md'
    },
    {
      name: 'naming-convention-validator',
      role: 'validator',
      what: 'Checks file, class, function, package, and sealed-type naming + stub*() preview pattern.',
      when: 'Always — gates every task. Findings route to the builder that wrote the offending file.',
      specPath: '../sub-agents/validators/naming-convention-validator.md'
    },
    {
      name: 'di-validator',
      role: 'validator',
      what: 'Verifies @Single(binds = [...]) / @Module @ComponentScan annotations and :shared/Koin.kt registration.',
      when: 'Always — gates every task.',
      specPath: '../sub-agents/validators/di-validator.md'
    },
    {
      name: 'compose-stability-validator',
      role: 'validator',
      what: 'Verifies @Immutable state, immutable collections, no inline dp/sp/Color outside design-system, no Material3 in feature code.',
      when: 'Always — gates every task touching Compose code.',
      specPath: '../sub-agents/validators/compose-stability-validator.md'
    },
    {
      name: 'data-layer-validator',
      role: 'validator',
      what: 'Verifies DTO nullable+default, repository pattern, range reconciliation, AppLogger.Mapping.log in DTO→Entity.',
      when: 'Always — gates every task touching DTOs, repositories, or mappers.',
      specPath: '../sub-agents/validators/data-layer-validator.md'
    },
    {
      name: 'build-validator',
      role: 'validator',
      what: 'Runs the two non-negotiable build gates: iOS XCFramework assemble (when iosEnabled) and :androidApp:assembleDebug.',
      when: 'Always — last gate before external review. Catches builders that reported done while builds were broken.',
      specPath: '../sub-agents/validators/build-validator.md'
    }
  ];
})();
