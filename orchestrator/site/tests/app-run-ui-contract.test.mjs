import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { dictionaryFor } from './i18n-test-helpers.mjs';

const site = fileURLToPath(new URL('../', import.meta.url));
const read = (relative) => fs.readFileSync(path.join(site, relative), 'utf8');
const index = read('index.html');
const app = read('scripts/app.js');
const control = read('scripts/app-run-control.js');
const menu = read('scripts/app-run-menu.js');
const drawer = read('scripts/app-run-drawer.js');
const validation = read('scripts/app-run-validation.js');
const styles = read('styles/components.css');
const board = read('scripts/panels/board.js');
const surface = read('scripts/design/entity-drawer.js');
const iosRunReference = read('../skills/platform-build-toolkit/references/ios-app-project.md');

assert.match(index, /id="app-run-root"/);
assert.match(app, /appRunControl\.init\(\)/);
assert.match(control, /matchMedia\('\(max-width: 640px\)'\)/);
assert.match(control, /siteEvents\.on\('app-run-status'/);
assert.match(control, /siteEvents\.on\('open',[\s\S]{0,160}refresh\(false\)/);
assert.match(control, /expectedProjectSourceRevision/);
assert.match(control, /confirmationToken/);
assert.match(control, /devices\/preview[\s\S]{0,400}idempotencyKey: idempotency\(\)/);
assert.match(control, /captureScreenshot/);
assert.match(menu, /aria-haspopup|role: 'dialog'/);
assert.match(menu, /last-build/);
assert.match(menu, /onCreate/);
assert.match(menu, /runAfterCreation/);
assert.match(menu, /unsupportedPhysicalDevices/);
assert.match(menu, /href: '#setup'/);
assert.match(control, /job\.result\.displayName/);
assert.doesNotMatch(control, /row\.displayName === job\.result\.displayName/);
assert.match(control, /state\.selection\.targetId = preferred \? preferred\.id : null/);
assert.doesNotMatch(control, /targetId\s*=\s*preferred[^;\n]*current\.devices\[0\]/);
assert.match(control, /device\.stableHint === preference\.targetStableHint/);
assert.doesNotMatch(control, /Math\.random/);
assert.match(control, /requestFocus\('menu'\)/);
assert.match(control, /requestFocus\('drawer'\)/);
assert.match(control, /requestFocus\('primary'\)/);
assert.match(control, /target\.focus\(\)/);
assert.match(drawer, /onCancel/);
assert.match(drawer, /onRestart/);
assert.match(drawer, /onScreenshot/);
assert.match(drawer, /onValidate/);
assert.match(drawer, /Intl\.NumberFormat/);
assert.match(drawer, /session\.applicationId/);
assert.match(drawer, /session\.appProjectSourceRevision/);
assert.match(drawer, /stage\.startedAt/);
assert.match(validation, /appRun\.addScreenshot/);
assert.match(validation, /aria-live/);
assert.match(validation, /appRun\.validationResult/);
assert.match(validation, /maxlength: '1000'/);
assert.match(validation, /screenshotIds/);
assert.match(validation, /result\.journalRecorded === false/);
assert.match(styles, /\.app-run-split/);
assert.match(styles, /\.app-run-primary::before/);
assert.match(styles, /\.app-run-primary--active::before/);
assert.match(control, /app-run-primary--active/);
assert.match(control, /appRun\.openRunStatus/);
assert.match(menu, /label \+= ' — ' \+ reason\(platform\.reasonCode\)/);
assert.match(styles, /@media \(max-width: 640px\)/);
assert.match(styles, /@media \(prefers-reduced-motion: reduce\)/);
assert.match(styles, /\.status-strip \{ padding-right: 250px; \}/);
assert.match(board, /appRunControl\.open\(\{ taskStem: stem \}\)/);
assert.match(surface, /appRunControl\.open\(\{ surfaceId: item\.id \}\)/);
assert.doesNotMatch(surface, /\/api\/design\/preview/);
assert.match(surface, /if \(item\.preview && item\.preview\.reason\)/);
assert.match(iosRunReference, /--udid/);
assert.match(iosRunReference, /--configuration-id/);
assert.match(iosRunReference, /--derived-data-path/);
assert.match(iosRunReference, /--build-only/);
assert.match(iosRunReference, /--install-only/);
assert.doesNotMatch(iosRunReference, /simctl (?:install|launch) booted/);
assert.doesNotMatch(iosRunReference, /\[A-Fa-f0-9-\]\{20,80\}/);
assert.doesNotMatch(iosRunReference, /find [^\n]*\\.app[^\n]*head/);

for (const locale of ['en', 'ru', 'uk']) {
  const dictionary = dictionaryFor(locale);
  for (const key of [
    'appRun.runApp',
    'appRun.validationTitle',
    'appRun.addScreenshot',
    'appRun.runAfterCreation',
    'appRun.validationJournalUnavailable',
    'appRun.unsupportedPhysicalDevices',
    'appRun.stage.launching',
  ]) {
    assert.equal(typeof dictionary[key], 'string', `${locale}:${key}`);
  }
}
