(function () {
  window.App = window.App || {};
  App.data = App.data || {};

  // One entry per "change kind" row in
  // requirements/sub-agents/helpers/task-intake.md (the "Classify the
  // change" table). The orchestrator + task-intake re-classify from the
  // task text; this list is purely informational so the user can preview
  // which builders are likely to run.
  //
  // `cookbook` is a path RELATIVE to requirements/site/index.html — it
  // works as an <a href> directly because the site lives at
  // requirements/site/ and reaches the rest of requirements/ via "../".
  App.data.builders = [
    {
      kind: 'Initial data-services scaffold',
      builder: 'data-service-scaffold-builder',
      cookbook: '../02-module-structure/09-data-service-modules.md'
    },
    {
      kind: 'Brand-new top-level feature',
      builder: 'feature-module-scaffold-builder',
      cookbook: '../02-module-structure/07-ui-feature-modules.md'
    },
    {
      kind: 'New sub-screen inside an existing feature',
      builder: 'screen-builder',
      cookbook: '../14-cookbook/01-add-screen.md'
    },
    {
      kind: 'New dialog (bottom sheet)',
      builder: 'dialog-builder',
      cookbook: '../14-cookbook/02-add-dialog.md'
    },
    {
      kind: 'New domain capability + data feature',
      builder: 'data-feature-builder',
      cookbook: '../14-cookbook/03-add-data-feature.md'
    },
    {
      kind: 'New mapper file',
      builder: 'mapper-builder',
      cookbook: '../14-cookbook/04-add-mapper.md'
    },
    {
      kind: 'New API endpoint + DTO',
      builder: 'endpoint-builder',
      cookbook: '../14-cookbook/06-add-endpoint.md'
    },
    {
      kind: 'Schema change (Room migration)',
      builder: 'room-migration-builder',
      cookbook: '../14-cookbook/05-add-room-migration.md'
    },
    {
      kind: 'New string / drawable / icon / font',
      builder: 'resource-builder',
      cookbook: '../14-cookbook/07-add-resource.md'
    },
    {
      kind: 'Cross-feature navigation',
      builder: 'cross-feature-nav-builder',
      cookbook: '../14-cookbook/08-add-cross-feature-nav.md'
    }
  ];
})();
