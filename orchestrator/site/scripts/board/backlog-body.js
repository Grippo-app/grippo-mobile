// Assemble the canonical task-file body from the composer's structured fields.
// The composer no longer hands the user a raw markdown textarea to (mis)edit by
// default — it collects per-section inputs and this function emits the exact
// shape the pipeline reads back: task-prep, inputs-resolver, acceptance-tracer
// and scope-leak-validator all key off the `## Goal / ## Inputs / ## Acceptance
// (### Automated + ### Manual) / ## Out of scope` headers, so a human can no
// longer break them. The section headers are an en-only structural invariant
// (same reasoning that keeps agent prompts out of the i18n dictionary) — they
// do not localize with the UI chrome. The canonical headings are documented in
// orchestrator/tasks/README.md.
//
// `## Design` is emitted only when the project is figmaEnabled and at least one
// screen row is filled — its presence marks a task as carrying screen designs.
// Its bullet syntax `- <Screen> [kind] — <light:url dark:url | url | none>` is
// the only accepted shape because the census and spec gate parse it, and
// task-prep never invents the URL. Guided rows cover both the [kind] tag and
// dark-theme URL.
//
// `## Origin` is intentionally not emitted here: deterministic generated-task
// callers may append it through the server, and `## Depends on` is left for
// task-prep to resolve rather than seeded empty. The raw-markdown escape hatch
// seeds its textarea from this same string, so the two composer modes never
// drift.
//
// Lenient by design: empty sections stay as skeleton (`- ` placeholder bullets,
// bare `- Data:` / `- Entry point:`) rather than being dropped. Creation is
// low-ceremony and task-prep shapes or asks on promotion. Only structure is
// guaranteed; content completeness stays the pipeline's job.
export function assembleBacklogBody(fields) {
  var f = fields || {};
  var out = [];

  function section(lines) {
    if (out.length) out.push('');
    for (var i = 0; i < lines.length; i++) out.push(lines[i]);
  }

  function trimValue(value) {
    return (value == null ? '' : String(value)).trim();
  }

  function bullets(items) {
    var filled = (items || []).map(trimValue).filter(Boolean);
    return filled.length
      ? filled.map(function (value) { return '- ' + value; })
      : ['- '];
  }

  var goal = trimValue(f.goal);
  section(goal ? ['## Goal', '', goal] : ['## Goal', '']);

  // Data stays a single line. Entry point can be multiple because a screen may
  // be reachable from several places.
  var data = trimValue(f.data);
  var entryLines = (
    Array.isArray(f.entries)
      ? f.entries
      : (f.entry != null ? [f.entry] : [])
  ).map(trimValue).filter(Boolean);
  var inputs = ['## Inputs', '', '- Data:' + (data ? ' ' + data : '')];
  if (entryLines.length) {
    entryLines.forEach(function (entry) {
      inputs.push('- Entry point: ' + entry);
    });
  } else {
    inputs.push('- Entry point:');
  }
  section(inputs);

  if (f.figmaEnabled) {
    var designBullets = [];
    (f.designRows || []).forEach(function (row) {
      var screen = trimValue(row && row.screen);
      if (!screen) return;

      // A named row with a URL has a design. No-mock is emitted only from the
      // explicit toggle and its owner-supplied reason; an empty URL must never
      // silently become a gate-disabling `none` opt-out.
      var kind = trimValue(row && row.kind).toLowerCase();
      var kindTag = (
        kind === 'dialog' || kind === 'component' || kind === 'overlay'
      ) ? ' [' + kind + ']' : '';
      var url = trimValue(row && row.url);
      var darkUrl = trimValue(row && row.darkUrl);
      var value;
      if (row && row.noMock) {
        // Strip parentheses and line breaks so the reason remains one audited
        // `none (<reason>)` bullet.
        var reason = trimValue(row.noMockReason)
          .replace(/[()\r\n]+/g, ' ')
          .trim();
        value = reason ? 'none (' + reason + ')' : 'none';
      } else {
        value = darkUrl
          ? (url ? 'light:' + url + ' ' : '') + 'dark:' + darkUrl
          : url;
      }
      designBullets.push('- ' + screen + kindTag + ' — ' + value);
    });
    if (designBullets.length) {
      section(['## Design', ''].concat(designBullets));
    }
  }

  section(['## Acceptance', '', '### Automated', '']
    .concat(bullets(f.automated))
    .concat(['', '### Manual', ''])
    .concat(bullets(f.manual)));

  section(['## Out of scope', ''].concat(bullets(f.outOfScope)));

  return out.join('\n');
}
