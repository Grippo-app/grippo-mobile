import { setup } from './panels/setup.js';
import { wizard } from './panels/wizard.js';
import { reviewer } from './panels/reviewer.js';
import { board } from './panels/board.js';
import { figma } from './panels/figma.js';
import { design } from './panels/design.js';
import { archmap } from './panels/archmap.js';
import { backend } from './panels/backend.js';
import { api } from './panels/api.js';

// ----------------------------------------------------------------------
// Panel registry — the SINGLE place panels are declared. The composition
// root (app.js) reads this to build the nav, the route sections, and the
// router's panel map, so adding an integration is a one-line registration
// here rather than edits scattered across index.html + router + nav.
//
// Each entry:
//   id          route id; matches the URL hash and the `#panel-<id>` section
//   navLabelKey i18n key for the nav button label (must exist in en/ru/uk)
//   order       ascending sort order WITHIN the panel's group
//   group       which sidebar group the button renders in — see GROUPS below
//   panel       the panel module's singleton: { mount(el), refresh()? }
// ----------------------------------------------------------------------
const PANELS = [
  // РАБОТА — the daily work surface
  { id: 'board',        navLabelKey: 'nav.board',        order: 10, group: 'work',         panel: board },
  // ПРОЕКТ — read-only knowledge about the project being built
  { id: 'archmap',      navLabelKey: 'nav.archmap',      order: 20, group: 'project',      panel: archmap },
  { id: 'design',       navLabelKey: 'nav.design',       order: 21, group: 'project',      panel: design },
  { id: 'api',          navLabelKey: 'nav.api',          order: 22, group: 'project',      panel: api },
  // ИНТЕГРАЦИИ — external tools wired into the pipeline
  { id: 'figma',        navLabelKey: 'nav.figma',        order: 30, group: 'integrations', panel: figma },
  { id: 'backend',      navLabelKey: 'nav.backend',      order: 31, group: 'integrations', panel: backend },
  { id: 'reviewer',     navLabelKey: 'nav.reviewer',     order: 32, group: 'integrations', panel: reviewer },
  // подвал — one-time bootstrap, collapses once setup is done
  { id: 'setup',        navLabelKey: 'nav.setup',        order: 50, group: 'start',        panel: setup },
  { id: 'wizard',       navLabelKey: 'nav.wizard',       order: 51, group: 'start',        panel: wizard },
];

// ----------------------------------------------------------------------
// Sidebar groups — the ORDER and RENDER REGION of each nav group. app.js
// (buildChrome) iterates this list, so adding a group is a one-line edit
// here plus its i18n label.
//   region 'main'  -> a <section> in the scrolling sidebar body. A non-null
//                     labelKey renders a small-caps section header above the
//                     buttons; a null labelKey renders the section headerless
//                     (used for the primary Board item that sits at the top).
//   region 'start' -> the collapsible footer group (auto-collapses once
//                     setup is done — see syncStartGroup in app.js)
// Every panel's `group` above must match one of these ids.
// ----------------------------------------------------------------------
export const GROUPS = [
  { id: 'work',         labelKey: null,                    region: 'main' },
  { id: 'project',      labelKey: 'nav.groupProject',      region: 'main' },
  { id: 'integrations', labelKey: 'nav.groupIntegrations', region: 'main' },
  { id: 'start',        labelKey: 'nav.groupStart',        region: 'start' },
];

// Registry-ordered list (defensive copy, sorted by `order`).
export function orderedPanels() {
  return PANELS.slice().sort(function (a, b) { return a.order - b.order; });
}
