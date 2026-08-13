import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { test } from 'node:test'
import { assembleBacklogBody } from '../scripts/board/backlog-body.js'
import { taskOverflowItems } from '../scripts/board/task-overflow-policy.js'
import { createBoardFormatters } from '../scripts/board/board-formatters.js'
import { createBoardToolbar } from '../scripts/board/board-toolbar.js'
import { createBoardViewportState } from '../scripts/board/board-viewport-state.js'
import { createBoardTaskListView } from '../scripts/board/board-task-list-view.js'
import { createBoardTaskCardFactory } from '../scripts/board/board-task-card-factory.js'
import { createBoardPaginationController } from '../scripts/board/board-pagination-controller.js'
import { createBoardLoadResults } from '../scripts/board/board-load-results.js'
import { createBoardLoadController } from '../scripts/board/board-load-controller.js'
import { createBoardTaskTargetController } from '../scripts/board/board-task-target-controller.js'
import { createBoardTaskInbox } from '../scripts/board/board-task-inbox.js'
import { createBoardFinalizationController } from '../scripts/board/board-finalization-controller.js'
import { createBoardTaskNavigationController } from '../scripts/board/board-task-navigation-controller.js'
import { createBoardConfirmDialog } from '../scripts/board/board-confirm-dialog.js'
import { createBoardOpenCardFreshness } from '../scripts/board/board-open-card-freshness.js'
import { createBoardRenderController } from '../scripts/board/board-render-controller.js'
import { createBoardRefreshClock } from '../scripts/board/board-refresh-clock.js'
import { createBoardTaskDetailsShell } from '../scripts/board/board-task-details-shell.js'
import { createBoardFigmaScreensController } from '../scripts/board/board-figma-screens-controller.js'
import { createBoardTaskActionController } from '../scripts/board/board-task-action-controller.js'
import { createEnqueueResultPresenter } from '../scripts/board/enqueue-result-presenter.js'
import { createBoardReadinessPolicy } from '../scripts/board/board-readiness-policy.js'
import { createFigmaTaskReadModel } from '../scripts/board/figma-task-read-model.js'
import { createTaskListStore } from '../scripts/board/task-list-store.js'
import {
  TASK_COLUMNS,
  countLoadedTasks,
  findTaskInColumns,
  mergeTaskSummaryPage,
} from '../scripts/board/task-summary-projection.js'
import { createBoardWorkerSupport } from '../scripts/board/worker-support.js'
import { createBoardHealthController } from '../scripts/board/board-health.js'
import { createVisualEvidenceView } from '../scripts/board/visual-evidence-view.js'
import { createVisualEvidenceRecoveryView } from '../scripts/board/visual-evidence-recovery-view.js'
import { createVisualFixTask } from '../scripts/board/visual-fix-task.js'
import { createVisualFixActionsView } from '../scripts/board/visual-fix-actions-view.js'
import { createVisualEvidenceSummaryView } from '../scripts/board/visual-evidence-summary-view.js'
import { createVisualEvidenceStatus } from '../scripts/board/visual-evidence-status.js'
import { createFigmaScreensView } from '../scripts/board/figma-screens-view.js'
import { createPixelReviewView } from '../scripts/board/pixel-review-view.js'
import {
  artifactSetReportHashOk,
  evidenceTrustState,
  finalVisualDisplayState,
  finalVisualTrustState,
  isValidReportHash,
  reportRunIds,
} from '../scripts/board/visual-evidence-trust.js'
import { dictionaryFor } from './i18n-test-helpers.mjs'

const here = dirname(fileURLToPath(import.meta.url))
const scripts = join(here, '..', 'scripts')
const card = readFileSync(join(scripts, 'board', 'task-card.js'), 'utf8')
const blocker = readFileSync(join(scripts, 'board', 'task-blocker.js'), 'utf8')
const originBadge = readFileSync(join(scripts, 'board', 'task-origin-badge.js'), 'utf8')
const action = readFileSync(join(scripts, 'board', 'task-card-action.js'), 'utf8')
const taskDetails = readFileSync(join(scripts, 'board', 'task-details.js'), 'utf8')
const taskActionBar = readFileSync(join(scripts, 'board', 'task-action-bar.js'), 'utf8')
const overflow = readFileSync(join(scripts, 'board', 'task-overflow.js'), 'utf8')
const overflowPolicy = readFileSync(join(scripts, 'board', 'task-overflow-policy.js'), 'utf8')
const backlogComposer = readFileSync(join(scripts, 'board', 'backlog-composer.js'), 'utf8')
const modalController = readFileSync(join(scripts, 'board', 'modal-controller.js'), 'utf8')
const boardFormattersSource = readFileSync(join(scripts, 'board', 'board-formatters.js'), 'utf8')
const boardToolbarSource = readFileSync(join(scripts, 'board', 'board-toolbar.js'), 'utf8')
const boardViewportStateSource = readFileSync(
  join(scripts, 'board', 'board-viewport-state.js'), 'utf8')
const boardTaskListViewSource = readFileSync(
  join(scripts, 'board', 'board-task-list-view.js'), 'utf8')
const boardTaskCardFactorySource = readFileSync(
  join(scripts, 'board', 'board-task-card-factory.js'), 'utf8')
const boardPaginationControllerSource = readFileSync(
  join(scripts, 'board', 'board-pagination-controller.js'), 'utf8')
const boardLoadResultsSource = readFileSync(
  join(scripts, 'board', 'board-load-results.js'), 'utf8')
const boardLoadControllerSource = readFileSync(
  join(scripts, 'board', 'board-load-controller.js'), 'utf8')
const boardTaskTargetControllerSource = readFileSync(
  join(scripts, 'board', 'board-task-target-controller.js'), 'utf8')
const boardTaskInboxSource = readFileSync(
  join(scripts, 'board', 'board-task-inbox.js'), 'utf8')
const boardFinalizationControllerSource = readFileSync(
  join(scripts, 'board', 'board-finalization-controller.js'), 'utf8')
const boardTaskNavigationControllerSource = readFileSync(
  join(scripts, 'board', 'board-task-navigation-controller.js'), 'utf8')
const boardConfirmDialogSource = readFileSync(
  join(scripts, 'board', 'board-confirm-dialog.js'), 'utf8')
const boardOpenCardFreshnessSource = readFileSync(
  join(scripts, 'board', 'board-open-card-freshness.js'), 'utf8')
const boardRenderControllerSource = readFileSync(
  join(scripts, 'board', 'board-render-controller.js'), 'utf8')
const boardRefreshClockSource = readFileSync(
  join(scripts, 'board', 'board-refresh-clock.js'), 'utf8')
const boardTaskDetailsShellSource = readFileSync(
  join(scripts, 'board', 'board-task-details-shell.js'), 'utf8')
const boardFigmaScreensControllerSource = readFileSync(
  join(scripts, 'board', 'board-figma-screens-controller.js'), 'utf8')
const boardTaskActionControllerSource = readFileSync(
  join(scripts, 'board', 'board-task-action-controller.js'), 'utf8')
const enqueueResultPresenterSource = readFileSync(
  join(scripts, 'board', 'enqueue-result-presenter.js'), 'utf8')
const boardReadinessPolicySource = readFileSync(
  join(scripts, 'board', 'board-readiness-policy.js'), 'utf8')
const figmaTaskReadModelSource = readFileSync(
  join(scripts, 'board', 'figma-task-read-model.js'), 'utf8')
const taskSummaryProjectionSource = readFileSync(
  join(scripts, 'board', 'task-summary-projection.js'), 'utf8')
const workerSupportSource = readFileSync(join(scripts, 'board', 'worker-support.js'), 'utf8')
const boardHealthSource = readFileSync(join(scripts, 'board', 'board-health.js'), 'utf8')
const visualEvidenceTrustSource = readFileSync(join(scripts, 'board', 'visual-evidence-trust.js'), 'utf8')
const visualEvidenceViewSource = readFileSync(join(scripts, 'board', 'visual-evidence-view.js'), 'utf8')
const visualEvidenceRecoveryViewSource = readFileSync(join(scripts, 'board', 'visual-evidence-recovery-view.js'), 'utf8')
const visualFixTaskSource = readFileSync(join(scripts, 'board', 'visual-fix-task.js'), 'utf8')
const visualFixActionsViewSource = readFileSync(join(scripts, 'board', 'visual-fix-actions-view.js'), 'utf8')
const visualEvidenceSummaryViewSource = readFileSync(join(scripts, 'board', 'visual-evidence-summary-view.js'), 'utf8')
const visualEvidenceStatusSource = readFileSync(join(scripts, 'board', 'visual-evidence-status.js'), 'utf8')
const figmaScreensViewSource = readFileSync(join(scripts, 'board', 'figma-screens-view.js'), 'utf8')
const pixelReviewViewSource = readFileSync(join(scripts, 'board', 'pixel-review-view.js'), 'utf8')
const board = readFileSync(join(scripts, 'panels', 'board.js'), 'utf8')
const en = dictionaryFor('en')
const ru = dictionaryFor('ru')
const uk = dictionaryFor('uk')
const api = readFileSync(join(scripts, 'data', 'tasks-api.js'), 'utf8')
const router = readFileSync(join(scripts, 'router.js'), 'utf8')
const sse = readFileSync(join(here, '..', 'server', 'sse.js'), 'utf8')
const http = readFileSync(join(here, '..', 'server', 'http.js'), 'utf8')
const taskSummarySource = readFileSync(join(here, '..', 'server', 'task-summary.js'), 'utf8')
const panels = readFileSync(join(here, '..', 'styles', 'panels.css'), 'utf8')
const components = readFileSync(join(here, '..', 'styles', 'components.css'), 'utf8')
const runControl = readFileSync(join(scripts, 'run-control.js'), 'utf8')
const reviewer = readFileSync(join(scripts, 'panels', 'reviewer.js'), 'utf8')
const figmaPanel = readFileSync(join(scripts, 'panels', 'figma.js'), 'utf8')
const skillsStatus = readFileSync(join(scripts, 'skills-status.js'), 'utf8')

function fakeElement(tag, attrs = {}, children = []) {
  const listeners = Object.create(null)
  const attributes = Object.create(null)
  const node = {
    tag,
    children: [],
    style: {},
    hidden: false,
    className: '',
    textContent: '',
    attributes,
    appendChild(child) {
      this.children.push(child)
      if (child && typeof child === 'object') child.parentNode = this
      return child
    },
    removeChild(child) {
      const index = this.children.indexOf(child)
      if (index >= 0) this.children.splice(index, 1)
      if (child && typeof child === 'object') child.parentNode = null
      return child
    },
    addEventListener(type, handler) {
      listeners[type] = handler
    },
    dispatch(type) {
      if (listeners[type]) listeners[type]({ target: this })
    },
    setAttribute(name, value) {
      attributes[name] = String(value)
      if (name === 'value') this.value = String(value)
    },
    getAttribute(name) {
      return Object.hasOwn(attributes, name) ? attributes[name] : null
    },
    removeAttribute(name) {
      delete attributes[name]
    },
    focus() {
      this.focused = true
    },
  }
  node.classList = {
    contains(name) {
      return node.className.split(/\s+/).filter(Boolean).includes(name)
    },
    toggle(name, enabled) {
      const classes = new Set(node.className.split(/\s+/).filter(Boolean))
      if (enabled) classes.add(name)
      else classes.delete(name)
      node.className = [...classes].join(' ')
    },
  }
  Object.defineProperty(node, 'firstChild', {
    get() { return node.children[0] || null },
  })
  Object.defineProperty(node, 'nextSibling', {
    get() {
      if (!node.parentNode || !Array.isArray(node.parentNode.children)) return null
      const index = node.parentNode.children.indexOf(node)
      return index >= 0 ? node.parentNode.children[index + 1] || null : null
    },
  })
  for (const [key, value] of Object.entries(attrs)) {
    if (key === 'class') node.className = value
    else if (key === 'text') node.textContent = value
    else if (key === 'attrs') {
      for (const [name, attrValue] of Object.entries(value)) node.setAttribute(name, attrValue)
    } else node[key] = value
  }
  for (const child of children) if (child != null) node.appendChild(child)
  return node
}

function fakeTree(root) {
  const nodes = []
  const visit = (node) => {
    if (!node || typeof node !== 'object') return
    nodes.push(node)
    for (const child of node.children || []) visit(child)
  }
  visit(root)
  return nodes
}

test('backlog body assembler preserves the canonical empty task skeleton', () => {
  assert.equal(assembleBacklogBody({}), [
    '## Goal',
    '',
    '',
    '## Inputs',
    '',
    '- Data:',
    '- Entry point:',
    '',
    '## Acceptance',
    '',
    '### Automated',
    '',
    '- ',
    '',
    '### Manual',
    '',
    '- ',
    '',
    '## Out of scope',
    '',
    '- ',
  ].join('\n'))
})

test('backlog body assembler trims structured fields and ignores disabled Design rows', () => {
  assert.equal(assembleBacklogBody({
    figmaEnabled: false,
    goal: '  Ship it  ',
    data: ' API response ',
    entries: [' Board ', '', ' Details '],
    designRows: [{
      screen: 'Ignored',
      url: 'https://figma.com/design/a?node-id=1-2',
    }],
    automated: [' npm test ', ''],
    manual: [' Open Board '],
    outOfScope: [' Backend '],
  }), [
    '## Goal',
    '',
    'Ship it',
    '',
    '## Inputs',
    '',
    '- Data: API response',
    '- Entry point: Board',
    '- Entry point: Details',
    '',
    '## Acceptance',
    '',
    '### Automated',
    '',
    '- npm test',
    '',
    '### Manual',
    '',
    '- Open Board',
    '',
    '## Out of scope',
    '',
    '- Backend',
  ].join('\n'))
})

test('backlog body assembler emits every current Design bullet shape exactly', () => {
  assert.equal(assembleBacklogBody({
    figmaEnabled: true,
    goal: 'Visual work',
    designRows: [
      {
        screen: 'Home',
        kind: 'screen',
        url: 'https://figma.com/design/a?node-id=1-2',
      },
      {
        screen: 'Dialog',
        kind: 'DIALOG',
        url: 'light-url',
        darkUrl: 'dark-url',
      },
      {
        screen: 'DarkOnly',
        kind: 'component',
        darkUrl: 'dark-only',
      },
      {
        screen: 'NoMock',
        kind: 'overlay',
        noMock: true,
        noMockReason: ' owner (approved)\n later ',
      },
      {
        screen: '   ',
        kind: 'screen',
        url: 'ignored',
      },
    ],
  }), [
    '## Goal',
    '',
    'Visual work',
    '',
    '## Inputs',
    '',
    '- Data:',
    '- Entry point:',
    '',
    '## Design',
    '',
    '- Home — https://figma.com/design/a?node-id=1-2',
    '- Dialog [dialog] — light:light-url dark:dark-url',
    '- DarkOnly [component] — dark:dark-only',
    '- NoMock [overlay] — none (owner  approved  later)',
    '',
    '## Acceptance',
    '',
    '### Automated',
    '',
    '- ',
    '',
    '### Manual',
    '',
    '- ',
    '',
    '## Out of scope',
    '',
    '- ',
  ].join('\n'))
})

test('task card exposes one primary CTA at most and keeps status metadata non-interactive', () => {
  assert.match(card, /el\('article'/)
  assert.match(card, /el\('header'/)
  assert.match(card, /el\('footer'/)
  assert.match(card, /header\.appendChild\(taskOverflow\(row, options\)\)/)
  assert.match(card, /if \(row\.state !== 'done' && row\.primaryAction && row\.primaryAction\.enabled !== false\) \{[\s\S]*footer\.appendChild\(taskCardAction\(row, options\)\)/)
  assert.doesNotMatch(card, /footer\.appendChild\(taskOverflow/)
  assert.match(card, /card\.addEventListener\('click'/)
  assert.match(card, /closest\('button, a, input, select, textarea, \[role="button"\], \[role="menuitem"\]'\)/)
  assert.equal((card.match(/board-card__primary-action/g) || []).length, 0)
  assert.equal((action.match(/board-card__primary-action/g) || []).length, 1)
  assert.match(card, /board-card__title-button/)
  assert.match(card, /taskOverflow\(row, options\)/)
  assert.match(overflow, /\[role="menuitem"\]:not\(\[disabled\]\)/)
  assert.match(overflow, /taskOverflowItems\(row\)/)
  assert.doesNotMatch(overflow, /advanced/)
  assert.doesNotMatch(overflow, /item\.disabled/)
  assert.match(blocker, /board\.blocker\.summary\./)
  assert.match(blocker, /el\('div'/)
  assert.doesNotMatch(blocker, /el\('button'/)
  assert.doesNotMatch(blocker, /addEventListener/)
  assert.doesNotMatch(blocker, /onOpenBlocker/)
  assert.match(originBadge, /if \(origin && origin\.kind === 'follow-up'\) return null/)
  assert.match(card, /if \(originBadge\) \{[\s\S]*badges\.appendChild\(originBadge\)/)
  assert.doesNotMatch(card, /board-card__state|board\.column\.' \+ row\.state|board\.state\.tooltip/)
  assert.doesNotMatch(panels, /board-card__state/)
  assert.equal(Object.hasOwn(en, 'board.state.tooltip'), false)
  assert.equal(Object.hasOwn(ru, 'board.state.tooltip'), false)
  assert.match(originBadge, /return el\('span'/)
  assert.doesNotMatch(originBadge, /el\('button'/)
  assert.doesNotMatch(originBadge, /addEventListener/)
  assert.doesNotMatch(originBadge, /onOpenTarget/)
  assert.doesNotMatch(blocker, /title: blocker\.summary/)
  assert.match(card, /board\.signalStatus\./)
  assert.match(card, /row\.dependencySummary && row\.dependencySummary\.count > 0/)
  assert.match(action, /board\.action\.disabled\./)
  assert.match(action, /attrs\['aria-label'\] = label \+ '\. ' \+ disabledReason/)
  assert.match(action, /opensTerminal \? 'btn--terminal' : 'btn--primary'/)
  assert.match(action, /action && action\.behavior === 'open-terminal'/)
  assert.match(components, /\.btn--terminal\s*\{[\s\S]*?border-color:\s*var\(--accent\);[\s\S]*?color:\s*var\(--accent\);/)
  assert.match(taskActionBar, /action\.behavior === 'open-terminal' \? 'btn btn--terminal'/)
  assert.match(board, /onOpenTarget: function \(target\)/)
})

test('pre-Setup creation uses a separate inbox and preserves an open composer across state refreshes', () => {
  assert.match(api, /function loadTaskInbox\(\)/)
  assert.match(api, /postJson\('\/api\/tasks\/inbox'/)
  assert.match(api, /postJson\('\/api\/tasks\/inbox\/publish'/)
  assert.match(http, /snapshot\.progress\.setupDone !== true/)
  assert.match(http, /error && error\.code === 'setup-incomplete'[\s\S]*\? 'setup-incomplete'/)
  assert.match(board,
    /import \{ createBoardTaskInbox \} from '\.\.\/board\/board-task-inbox\.js'/)
  assert.equal((board.match(/from '\.\.\/board\/board-task-inbox\.js'/g) || []).length, 1)
  assert.match(board, /var boardTaskInbox = createBoardTaskInbox\(\{/)
  assert.match(boardRenderControllerSource, /dependencies\.inbox\.render\(false\)/)
  assert.match(boardTaskInboxSource, /dependencies\.openComposer/)
  assert.match(backlogComposer, /tasksApi\.saveTaskInbox/)
  assert.match(board,
    /publishEntry: function \(id\) \{ return tasksApi\.publishTaskInbox\(id\); \}/)
  assert.match(boardTaskInboxSource,
    /export function createBoardTaskInbox\(dependencies\)/)
  assert.match(boardTaskInboxSource, /dependencies\.publishEntry\(entry\.id\)/)
  assert.match(boardTaskInboxSource, /var loadGeneration = \+\+generation/)
  assert.doesNotMatch(board,
    /function (?:loadTaskInbox|publishInboxEntry|renderTaskInbox)\(|\binboxLoadGen\b/)
  assert.doesNotMatch(board,
    /inboxEntries|inboxLoaded|inboxLoading|inboxError/)
  assert.doesNotMatch(boardTaskInboxSource, /from '\.\.\/panels\/board\.js'/)
  assert.doesNotMatch(boardTaskInboxSource,
    /tasksApi|backlogComposer|clipboard|\bstore\.|\bstate\.|document|window|location|history|fetch\(/)
  assert.doesNotMatch(board,
    /if \(!wizardComplete\(storeState\)\) \{\s*forceCloseModal\(\);\s*render\(\);/)
  assert.doesNotMatch(board,
    /clipboard\.toast\(t\('board\.run\.queuedNoWorker'\)\);\s*openWorkerHelpModal\(\)/)
  assert.match(card, /row\.runtimeStatus && row\.runtimeStatus\.state === 'waiting-runner'/)
  assert.equal(ru['board.runtime.waiting_runner'], 'Ждёт runner')
})

test('board delegates post-enqueue presentation without retaining a panel-local policy', () => {
  assert.match(board,
    /import \{ createEnqueueResultPresenter \} from '\.\.\/board\/enqueue-result-presenter\.js'/)
  assert.equal((board.match(/from '\.\.\/board\/enqueue-result-presenter\.js'/g) || []).length, 1)
  assert.match(board, /var enqueueResultPresenter = createEnqueueResultPresenter\(\{/)
  assert.match(board, /getSnapshot: function \(\) \{ return store\.get\(\); \}/)
  assert.match(board, /workerOnlineOrBusy: workerSupport\.workerOnlineOrBusy/)
  assert.match(board, /cliCannotAuth: workerSupport\.cliCannotAuth/)
  assert.match(board, /toast: function \(message\) \{ clipboard\.toast\(message\); \}/)
  assert.equal((board.match(/enqueueResultPresenter\.present\(/g) || []).length, 4)
  assert.doesNotMatch(board, /function routeAfterEnqueue\(/)

  assert.match(enqueueResultPresenterSource,
    /export function createEnqueueResultPresenter\(dependencies\)/)
  assert.doesNotMatch(enqueueResultPresenterSource, /from '\.\.\/panels\/board\.js'/)
  assert.doesNotMatch(enqueueResultPresenterSource,
    /tasksApi|requestJson|runControl|boardModal|clipboard|store|state\.|document|window|fetch\(/)
})

test('enqueue result presentation preserves drainer, authentication, replay, and live-session policy', () => {
  function scenario({ snapshot, workerOnline = false, cannotAuth = false }) {
    const messages = []
    let snapshotReads = 0
    const presenter = createEnqueueResultPresenter({
      t: key => key,
      getSnapshot() {
        snapshotReads++
        return snapshot
      },
      workerOnlineOrBusy() {
        return workerOnline
      },
      cliCannotAuth() {
        return cannotAuth
      },
      toast(message) {
        messages.push(message)
      },
    })
    return { presenter, messages, reads: () => snapshotReads }
  }

  let current = scenario({ snapshot: { runnerActive: false } })
  current.presenter.present(null, 'TASK_1', () => assert.fail('no live session'))
  assert.deepEqual(current.messages, ['board.run.queuedNoWorker'])
  assert.equal(current.reads(), 1)

  current = scenario({ snapshot: { runnerActive: false }, workerOnline: true, cannotAuth: true })
  current.presenter.present({}, 'TASK_1')
  assert.deepEqual(current.messages, ['board.run.queued'])

  current = scenario({
    snapshot: { runnerActive: true, cli: { installed: true, authProblem: null } },
    cannotAuth: true,
  })
  current.presenter.present({}, 'TASK_1')
  assert.deepEqual(current.messages, ['board.run.queuedNoAuth'])

  current = scenario({
    snapshot: { runnerActive: true, cli: { installed: true, authProblem: 'expired' } },
    cannotAuth: true,
  })
  current.presenter.present({}, 'TASK_1')
  assert.deepEqual(current.messages, ['board.run.queuedAuthDead'])

  current = scenario({
    snapshot: { runnerActive: true, cli: { installed: true, authProblem: 'expired' } },
    workerOnline: true,
    cannotAuth: true,
  })
  current.presenter.present({}, 'TASK_1')
  assert.deepEqual(current.messages, ['board.run.queued'])

  for (const response of [
    { deduped: true },
    { idempotentReplay: true },
    { status: 'already-active' },
  ]) {
    current = scenario({ snapshot: { runnerActive: true } })
    current.presenter.present(response, 'TASK_1')
    assert.deepEqual(current.messages, ['board.run.queuedExists'])
  }

  current = scenario({ snapshot: { runnerActive: false } })
  let opened = 0
  current.presenter.present({
    state: {
      runnerActive: true,
      sessions: { 'task:TASK_2': { running: true } },
    },
  }, 'TASK_2', () => { opened++ })
  assert.deepEqual(current.messages, ['board.run.queued'])
  assert.equal(opened, 1)
  assert.equal(current.reads(), 0)

  current.presenter.present({
    state: {
      runnerActive: true,
      sessions: { 'task:TASK_2': { running: false } },
    },
  }, 'TASK_2', () => { opened++ })
  assert.equal(opened, 1)
})

test('board delegates readiness decisions without retaining panel-local policy', () => {
  assert.match(board,
    /import \{ createBoardReadinessPolicy \} from '\.\.\/board\/board-readiness-policy\.js'/)
  assert.equal((board.match(/from '\.\.\/board\/board-readiness-policy\.js'/g) || []).length, 1)
  assert.match(board, /var boardReadiness = createBoardReadinessPolicy\(\{/)
  assert.match(board, /t: t/)
  assert.match(board, /getSnapshot: function \(\) \{ return store\.get\(\); \}/)
  assert.match(board, /getFreshIntegrity: function \(\) \{ return state\.integrity; \}/)
  assert.doesNotMatch(board,
    /function (?:taskIntegrity|startupRecoveryState|startupRecoveryBlocksMutation|startupRecoveryReason|globalMutationBlocked|integrityBlocksStem)\(/)

  assert.match(boardReadinessPolicySource,
    /export function createBoardReadinessPolicy\(dependencies\)/)
  assert.doesNotMatch(boardReadinessPolicySource, /from '\.\.\/panels\/board\.js'/)
  assert.doesNotMatch(boardReadinessPolicySource,
    /tasksApi|requestJson|runControl|boardModal|clipboard|store|state\.|document|window|fetch\(/)
})

test('board readiness preserves integrity, startup, publication, and stem-blocking policy', () => {
  function scenario({ freshIntegrity = null, snapshot = {} } = {}) {
    let snapshotReads = 0
    let integrityReads = 0
    const policy = createBoardReadinessPolicy({
      t: key => key,
      getFreshIntegrity() {
        integrityReads++
        return freshIntegrity
      },
      getSnapshot() {
        snapshotReads++
        return snapshot
      },
    })
    return {
      policy,
      reads: () => snapshotReads,
      integrityReads: () => integrityReads,
    }
  }

  const fresh = { ok: false, findings: [{ severity: 'error', stem: 'TASK_1' }] }
  let current = scenario({
    freshIntegrity: fresh,
    snapshot: { taskIntegrity: { ok: true } },
  })
  assert.strictEqual(current.policy.taskIntegrity(), fresh)
  assert.equal(current.reads(), 0)
  assert.equal(current.integrityReads(), 1)

  const stored = { ok: false, indexStatus: 'invalid', findings: [], affectedStems: [] }
  current = scenario({ snapshot: { taskIntegrity: stored } })
  assert.strictEqual(current.policy.taskIntegrity(), stored)
  assert.equal(current.reads(), 1)
  assert.equal(current.integrityReads(), 1)

  current = scenario()
  assert.deepEqual(current.policy.taskIntegrity(), {
    ok: true,
    indexStatus: 'unchecked',
    findings: [],
    affectedStems: [],
  })

  const startup = { status: 'blocked', reasonCode: 'runner-start-failed', findingCount: 2 }
  current = scenario({ snapshot: { startupRecovery: startup } })
  assert.strictEqual(current.policy.startupRecoveryState(), startup)
  assert.equal(current.policy.startupRecoveryBlocksMutation(), true)
  assert.equal(current.policy.startupRecoveryReason('runner-start-failed'),
    'board.startupRecovery.reason.runner-start-failed')
  assert.equal(current.policy.startupRecoveryReason('invented-reason'),
    'board.startupRecovery.reason.startup-recovery-failed')

  current = scenario({ snapshot: { startupRecovery: { status: 'ready' } } })
  assert.equal(current.policy.globalMutationBlocked(), false)
  current = scenario({
    snapshot: {
      startupRecovery: { status: 'ready' },
      progress: {
        publicationRecoveryIssues: [
          { code: 'CREATION_INCOMPLETE' },
          { code: 'EDIT_INCOMPLETE' },
        ],
      },
    },
  })
  assert.equal(current.policy.globalMutationBlocked(), false)
  current = scenario({
    snapshot: {
      startupRecovery: { status: 'ready' },
      progress: { publicationRecoveryIssues: [{ code: 'FOREIGN_RECOVERY_STATE' }] },
    },
  })
  assert.equal(current.policy.globalMutationBlocked(), true)

  current = scenario({
    freshIntegrity: { ok: true, findings: [], affectedStems: [] },
    snapshot: { startupRecovery: { status: 'pending' } },
  })
  assert.equal(current.policy.integrityBlocksStem('TASK_1'), true)
  current = scenario({
    freshIntegrity: {
      ok: false,
      findings: [
        { severity: 'warning', stem: 'TASK_1' },
        { severity: 'error', stem: 'TASK_2' },
        { severity: 'blocker', details: { stems: ['TASK_3'] } },
      ],
      affectedStems: ['TASK_4'],
    },
    snapshot: { startupRecovery: { status: 'ready' } },
  })
  assert.equal(current.policy.integrityBlocksStem('TASK_1'), false)
  assert.equal(current.policy.integrityBlocksStem('TASK_2'), true)
  assert.equal(current.policy.integrityBlocksStem('TASK_3'), true)
  assert.equal(current.policy.integrityBlocksStem('TASK_4'), true)
  assert.equal(current.policy.integrityBlocksStem('TASK_5'), false)
})

test('board delegates persistent filter toolbar ownership to one current module', () => {
  assert.match(board,
    /import \{ createBoardToolbar \} from '\.\.\/board\/board-toolbar\.js'/)
  assert.equal((board.match(/from '\.\.\/board\/board-toolbar\.js'/g) || []).length, 1)
  assert.match(board, /var boardToolbar = createBoardToolbar\(\{/)
  assert.match(board, /getSectionElement: function \(\) \{ return sectionEl; \}/)
  assert.match(board,
    /refresh: function \(\) \{ boardLoadController\.load\(\{ closeOpenModal: false \}\); \}/)
  assert.match(board, /getToolbarElement: boardToolbar\.element/)
  assert.match(boardRenderControllerSource, /dependencies\.toolbar\.preservedElement\(\)/)
  assert.match(boardRenderControllerSource, /dependencies\.toolbar\.render\(\)/)
  assert.doesNotMatch(board,
    /function (?:currentToolbarLang|updateTaskFilter|filterSelect|renderToolbar)\(/)
  assert.doesNotMatch(board, /var (?:filterTimer|toolbarEl|toolbarLang) = null/)

  assert.match(boardToolbarSource, /export function createBoardToolbar\(dependencies\)/)
  assert.doesNotMatch(boardToolbarSource, /from '\.\.\/panels\/board\.js'/)
  assert.doesNotMatch(boardToolbarSource,
    /tasksApi|requestJson|runControl|boardModal|clipboard|store\.get|document|window|fetch\(/)
})

test('board delegates refresh viewport retention to one current module', () => {
  assert.match(board,
    /import \{ createBoardViewportState \} from '\.\.\/board\/board-viewport-state\.js'/)
  assert.equal((board.match(/from '\.\.\/board\/board-viewport-state\.js'/g) || []).length, 1)
  assert.match(board, /var boardViewportState = createBoardViewportState\(\{/)
  assert.match(board, /getDocumentNode: function \(\) \{ return document; \}/)
  assert.match(board, /getViewport: function \(\) \{ return window; \}/)
  assert.match(board, /getSectionElement: function \(\) \{ return sectionEl; \}/)
  assert.match(boardRenderControllerSource, /dependencies\.viewport\.captureScroll\(\)/)
  assert.match(boardRenderControllerSource, /dependencies\.viewport\.captureFocus\(\)/)
  assert.match(boardRenderControllerSource, /dependencies\.viewport\.restoreScroll\(scrollSnapshot\)/)
  assert.match(boardRenderControllerSource, /dependencies\.viewport\.restoreFocus\(focusSnapshot\)/)
  assert.doesNotMatch(board,
    /function (?:captureBoardFocus|restoreBoardFocus|restoreBoardScroll)\(/)

  assert.match(boardViewportStateSource,
    /export function createBoardViewportState\(dependencies\)/)
  assert.doesNotMatch(boardViewportStateSource, /from '\.\.\/panels\/board\.js'/)
  assert.doesNotMatch(boardViewportStateSource,
    /tasksApi|requestJson|runControl|boardModal|clipboard|\bstore\.|fetch\(/)
})

test('board task-list view renders canonical columns and pagination states', () => {
  const section = fakeElement('section')
  const cards = []
  const calls = { create: 0, loadMore: 0 }
  const state = {
    columns: {
      backlog: [{ stem: 'TASK_1_Backlog' }],
      pending: [],
      todo: [{ stem: 'TASK_2_Todo' }],
    },
    summary: {
      nextCursor: 'page-2',
      total: 7,
      columns: {
        backlog: [{ stem: 'TASK_1_Backlog' }],
        pending: [],
        todo: [{ stem: 'TASK_2_Todo' }],
        done: [],
      },
    },
    loadingMore: false,
    paginationError: null,
  }
  let mutationsBlocked = true
  const view = createBoardTaskListView({
    t: (key, params) => params ? `${key}:${params.loaded}/${params.total}` : key,
    el: fakeElement,
    getSectionElement: () => section,
    getState: () => state,
    createCard(folder, item) {
      cards.push([folder, item.stem])
      return fakeElement('article', { text: item.stem })
    },
    globalMutationBlocked: () => mutationsBlocked,
    openBacklogComposer: () => { calls.create += 1 },
    loadMoreTasks: () => { calls.loadMore += 1 },
  })

  view.render()
  assert.equal(section.children.length, 2)
  const columns = section.children[0]
  assert.equal(columns.className, 'board-columns')
  assert.deepEqual(columns.children.map(column => column.attributes['data-folder']), [
    'backlog', 'pending', 'todo', 'done',
  ])
  assert.deepEqual(cards, [
    ['backlog', 'TASK_1_Backlog'],
    ['todo', 'TASK_2_Todo'],
  ])

  const backlogHeader = columns.children[0].children[0]
  const addButton = backlogHeader.children[1]
  assert.equal(addButton.className, 'btn btn--primary board-column__add')
  assert.equal(addButton.disabled, true)
  assert.equal(addButton.attributes.title, 'board.integrity.createBlocked')
  addButton.dispatch('click')
  assert.equal(calls.create, 1)

  for (const index of [1, 3]) {
    const empty = columns.children[index].children[1].children[0]
    assert.equal(empty.className, 'board-column__empty')
    assert.equal(empty.textContent,
      `board.columnEmpty.${columns.children[index].attributes['data-folder']}`)
  }

  const pagination = section.children[1]
  assert.equal(pagination.className, 'board-pagination')
  assert.equal(pagination.children[0].textContent, 'board.filter.loaded:2/7')
  const more = pagination.children[1]
  assert.equal(more.textContent, 'board.filter.loadMore')
  assert.equal(more.disabled, false)
  assert.equal(Object.hasOwn(more.attributes, 'aria-busy'), false)
  more.dispatch('click')
  assert.equal(calls.loadMore, 1)

  section.children.length = 0
  state.loadingMore = true
  view.render()
  const loading = section.children[1].children[1]
  assert.equal(loading.textContent, 'board.filter.loadingMore')
  assert.equal(loading.disabled, true)
  assert.equal(loading.attributes['aria-busy'], 'true')

  section.children.length = 0
  state.loadingMore = false
  state.paginationError = { kind: 'fetch-failed' }
  view.render()
  assert.equal(section.children[1].children[1].textContent, 'board.filter.retryMore')

  section.children.length = 0
  state.summary.nextCursor = null
  mutationsBlocked = false
  view.render()
  assert.equal(section.children.length, 1)
  const enabledAdd = section.children[0].children[0].children[0].children[1]
  assert.equal(enabledAdd.disabled, undefined)
  assert.equal(Object.hasOwn(enabledAdd.attributes, 'title'), false)
})

test('board delegates task-list presentation to one current module', () => {
  assert.match(board,
    /import \{ createBoardTaskListView \} from '\.\.\/board\/board-task-list-view\.js'/)
  assert.equal((board.match(/from '\.\.\/board\/board-task-list-view\.js'/g) || []).length, 1)
  assert.match(board, /var boardTaskListView = createBoardTaskListView\(\{/)
  assert.match(boardRenderControllerSource, /dependencies\.taskList\.render\(\)/)
  assert.doesNotMatch(board,
    /function (?:renderColumn|renderColumns|renderPagination)\(/)
  assert.match(boardTaskListViewSource,
    /export function createBoardTaskListView\(dependencies\)/)
  assert.doesNotMatch(boardTaskListViewSource, /from '\.\.\/panels\/board\.js'/)
  assert.doesNotMatch(boardTaskListViewSource,
    /tasksApi|requestJson|runControl|boardModal|clipboard|\bstore\.|document|window|fetch\(/)
})

test('board task-card factory preserves summary ownership, recovery policy, and callbacks', () => {
  const renders = []
  const details = []
  const menuState = { owner: 'task-list-store' }
  const formatRelative = value => `relative:${value}`
  const execute = () => 'execute'
  const navigate = () => 'navigate'
  const actionHandler = () => 'action'
  let recoveryBlocked = false
  let recoveryReads = 0
  let sectionReads = 0
  let section = null
  const factory = createBoardTaskCardFactory({
    renderCard(row, options) {
      const rendered = { row, options }
      renders.push(rendered)
      return rendered
    },
    t: key => `t:${key}`,
    formatRelative,
    mutationsBlocked() {
      recoveryReads += 1
      return recoveryBlocked
    },
    menuState,
    getSectionElement() {
      sectionReads += 1
      return section
    },
    openDetails(folder, row) { details.push([folder, row]) },
    execute,
    navigate,
    action: actionHandler,
  })

  const navigationRow = {
    stem: 'TASK_7_Navigate',
    state: '',
    primaryAction: { behavior: 'navigate', enabled: true },
  }
  const navigationCard = factory.create('todo', navigationRow)
  assert.equal(navigationRow.state, 'todo')
  assert.equal(navigationCard.row, navigationRow)
  assert.equal(navigationCard.options.t('key'), 't:key')
  assert.equal(navigationCard.options.formatRelative, formatRelative)
  assert.equal(navigationCard.options.mutationsBlocked, false)
  assert.equal(navigationCard.options.menuState, menuState)
  assert.equal(navigationCard.options.onExecute, execute)
  assert.equal(navigationCard.options.onNavigate, navigate)
  assert.equal(navigationCard.options.onAction, actionHandler)
  navigationCard.options.onOpenDetails(navigationRow)
  assert.deepEqual(details, [['todo', navigationRow]])
  assert.equal(recoveryReads, 1)

  const currentMenu = { hidden: false }
  const triggerUpdates = []
  const otherMenu = {
    hidden: false,
    parentNode: {
      querySelector(selector) {
        assert.equal(selector, '.board-card__overflow-trigger')
        return {
          setAttribute(name, value) { triggerUpdates.push([name, value]) },
        }
      },
    },
  }
  const detachedMenu = { hidden: false, parentNode: null }
  let queriedSelector = null
  section = {
    querySelectorAll(selector) {
      queriedSelector = selector
      return [currentMenu, otherMenu, detachedMenu]
    },
  }
  navigationCard.options.onMenuOpen(navigationRow.stem, currentMenu)
  assert.equal(sectionReads, 1, 'the live section must be resolved when the menu opens')
  assert.equal(queriedSelector, '.board-card__overflow-menu:not([hidden])')
  assert.equal(currentMenu.hidden, false)
  assert.equal(otherMenu.hidden, true)
  assert.equal(detachedMenu.hidden, true)
  assert.deepEqual(triggerUpdates, [['aria-expanded', 'false']])

  recoveryBlocked = true
  const executeAction = { behavior: 'execute', enabled: true, token: 'same-action' }
  const executeRow = {
    stem: 'TASK_8_Execute',
    state: 'pending',
    primaryAction: executeAction,
    nested: { identity: 'preserved' },
  }
  const blockedCard = factory.create('backlog', executeRow)
  assert.notEqual(blockedCard.row, executeRow)
  assert.equal(blockedCard.row.nested, executeRow.nested)
  assert.notEqual(blockedCard.row.primaryAction, executeAction)
  assert.deepEqual(blockedCard.row.primaryAction, {
    behavior: 'execute',
    enabled: false,
    token: 'same-action',
    disabledReasonCode: 'startup-recovery',
  })
  assert.deepEqual(executeAction, {
    behavior: 'execute',
    enabled: true,
    token: 'same-action',
  }, 'the canonical summary DTO must not have its action rewritten')
  assert.equal(blockedCard.options.mutationsBlocked, true)
  assert.equal(recoveryReads, 2)
  assert.equal(renders.length, 2)
})

test('board delegates task-card assembly to one current factory', () => {
  assert.match(board,
    /import \{ createBoardTaskCardFactory \} from '\.\.\/board\/board-task-card-factory\.js'/)
  assert.equal((board.match(/from '\.\.\/board\/board-task-card-factory\.js'/g) || []).length, 1)
  assert.match(board, /var boardTaskCardFactory = createBoardTaskCardFactory\(\{/)
  assert.match(board, /createCard: boardTaskCardFactory\.create/)
  assert.doesNotMatch(board, /function makeCard\(/)
  assert.match(boardTaskCardFactorySource,
    /export function createBoardTaskCardFactory\(dependencies\)/)
  assert.doesNotMatch(boardTaskCardFactorySource, /from '\.\.\/panels\/board\.js'/)
  assert.doesNotMatch(boardTaskCardFactorySource,
    /tasksApi|requestJson|runControl|boardModal|clipboard|\bstore\.|document|window|fetch\(/)
})

test('board pagination controller preserves guards, success, revision refresh, and rejection state', async () => {
  function deferred() {
    let resolve
    let reject
    const promise = new Promise((onResolve, onReject) => {
      resolve = onResolve
      reject = onReject
    })
    return { promise, resolve, reject }
  }

  function harness(overrides = {}) {
    const state = overrides.state || {
      summary: { revision: 'rev-1', nextCursor: 'cursor-2', columns: { todo: ['old'] } },
      columns: { todo: ['old'] },
      loadingMore: false,
      paginationError: { kind: 'old-error' },
    }
    const request = deferred()
    const calls = { filters: 0, load: [], merge: [], render: 0, reload: 0 }
    let generation = overrides.generation ?? 7
    const filtered = { q: 'needle', state: 'todo' }
    const merged = { revision: 'rev-1', nextCursor: null, columns: { todo: ['old', 'new'] } }
    const controller = createBoardPaginationController({
      getState: () => state,
      getLoadGeneration: () => generation,
      getFilters() {
        calls.filters += 1
        return filtered
      },
      loadSummary(filters) {
        calls.load.push(filters)
        return request.promise
      },
      mergeSummaryPage(summary, page) {
        calls.merge.push([summary, page])
        return merged
      },
      render() { calls.render += 1 },
      reload() { calls.reload += 1 },
    })
    return {
      state,
      request,
      calls,
      filtered,
      merged,
      controller,
      setGeneration(value) { generation = value },
    }
  }

  for (const guardedState of [
    { summary: null, loadingMore: false },
    { summary: { revision: 'rev-1', nextCursor: null }, loadingMore: false },
    { summary: { revision: 'rev-1', nextCursor: 'cursor-2' }, loadingMore: true },
  ]) {
    const guarded = harness({ state: guardedState })
    assert.equal(guarded.controller.loadMore(), undefined)
    assert.deepEqual(guarded.calls, {
      filters: 0, load: [], merge: [], render: 0, reload: 0,
    })
  }

  const success = harness()
  const originalSummary = success.state.summary
  assert.equal(success.controller.loadMore(), undefined)
  assert.equal(success.state.loadingMore, true)
  assert.equal(success.state.paginationError, null)
  assert.equal(success.calls.render, 1)
  assert.equal(success.calls.filters, 1)
  assert.deepEqual(success.calls.load, [{ q: 'needle', state: 'todo', cursor: 'cursor-2' }])
  assert.deepEqual(success.filtered, { q: 'needle', state: 'todo' }, 'filters must not be mutated')
  success.request.resolve({ revision: 'rev-1', columns: { todo: ['new'] } })
  await success.request.promise
  assert.equal(success.calls.merge.length, 1)
  assert.equal(success.calls.merge[0][0], originalSummary)
  assert.deepEqual(success.calls.merge[0][1], { revision: 'rev-1', columns: { todo: ['new'] } })
  assert.equal(success.state.summary, success.merged)
  assert.equal(success.state.columns, success.merged.columns)
  assert.equal(success.state.loadingMore, false)
  assert.equal(success.calls.render, 2)
  assert.equal(success.calls.reload, 0)

  const pageDrift = harness()
  pageDrift.controller.loadMore()
  pageDrift.request.resolve({ revision: 'rev-2', columns: { todo: ['new'] } })
  await pageDrift.request.promise
  assert.equal(pageDrift.calls.reload, 1)
  assert.equal(pageDrift.calls.merge.length, 0)
  assert.equal(pageDrift.state.loadingMore, true)
  assert.equal(pageDrift.calls.render, 1)

  const rejected = harness()
  const rejection = { kind: 'offline' }
  rejected.controller.loadMore()
  rejected.request.reject(rejection)
  await assert.rejects(rejected.request.promise, error => error === rejection)
  await Promise.resolve()
  assert.equal(rejected.state.loadingMore, false)
  assert.equal(rejected.state.paginationError, rejection)
  assert.equal(rejected.calls.render, 2)

  const staleRejection = harness()
  const lateError = { kind: 'late-error' }
  staleRejection.controller.loadMore()
  staleRejection.setGeneration(8)
  staleRejection.request.reject(lateError)
  await assert.rejects(staleRejection.request.promise, error => error === lateError)
  await Promise.resolve()
  assert.equal(staleRejection.state.loadingMore, true)
  assert.equal(staleRejection.state.paginationError, null)
  assert.equal(staleRejection.calls.render, 1)

  const emptyRejection = harness()
  emptyRejection.controller.loadMore()
  emptyRejection.request.reject(null)
  await assert.rejects(emptyRejection.request.promise, error => error === null)
  await Promise.resolve()
  assert.deepEqual(emptyRejection.state.paginationError, { kind: 'fetch-failed' })
})

test('board pagination controller drops stale generations and stale summary snapshots', async () => {
  function scenario(changeBeforeResolve) {
    let resolvePage
    let generation = 3
    const state = {
      summary: { revision: 'rev-1', nextCursor: 'cursor-2', columns: { done: [] } },
      columns: { done: [] },
      loadingMore: false,
      paginationError: { kind: 'old' },
    }
    const calls = { merge: 0, render: 0, reload: 0 }
    const request = new Promise(resolve => { resolvePage = resolve })
    const controller = createBoardPaginationController({
      getState: () => state,
      getLoadGeneration: () => generation,
      getFilters: () => ({ state: 'done' }),
      loadSummary: () => request,
      mergeSummaryPage() { calls.merge += 1 },
      render() { calls.render += 1 },
      reload() { calls.reload += 1 },
    })
    controller.loadMore()
    changeBeforeResolve({ state, setGeneration(value) { generation = value } })
    resolvePage({ revision: 'rev-1', columns: { done: ['late'] } })
    return request.then(() => ({ state, calls }))
  }

  const generationStale = await scenario(context => context.setGeneration(4))
  assert.deepEqual(generationStale.calls, { merge: 0, render: 1, reload: 0 })
  assert.equal(generationStale.state.loadingMore, true)
  assert.equal(generationStale.state.paginationError, null)

  const summaryStale = await scenario(context => { context.state.summary.revision = 'rev-2' })
  assert.deepEqual(summaryStale.calls, { merge: 0, render: 1, reload: 0 })
  assert.equal(summaryStale.state.loadingMore, true)

  const summaryMissing = await scenario(context => { context.state.summary = null })
  assert.deepEqual(summaryMissing.calls, { merge: 0, render: 1, reload: 0 })
  assert.equal(summaryMissing.state.loadingMore, true)
})

test('board delegates pagination ownership to one current controller', () => {
  assert.match(board,
    /import \{ createBoardPaginationController \} from '\.\.\/board\/board-pagination-controller\.js'/)
  assert.equal((board.match(/from '\.\.\/board\/board-pagination-controller\.js'/g) || []).length, 1)
  assert.match(board, /var boardPaginationController = createBoardPaginationController\(\{/)
  assert.match(board, /loadMoreTasks: boardPaginationController\.loadMore/)
  assert.doesNotMatch(board, /function loadMoreTasks\(/)
  assert.match(boardPaginationControllerSource,
    /export function createBoardPaginationController\(dependencies\)/)
  assert.doesNotMatch(boardPaginationControllerSource, /from '\.\.\/panels\/board\.js'/)
  assert.doesNotMatch(boardPaginationControllerSource,
    /tasksApi|taskListStore|boardModal|clipboard|document|window|fetch\(/)
})

test('board load-results adapter preserves parallel settlement and exact error contracts', async () => {
  let resolveIntegrity
  let settled = false
  const filters = { state: 'todo', q: 'needle' }
  const summary = { revision: 'rev-1', columns: { todo: [] } }
  const integrity = { ok: true, findings: [] }
  const calls = []
  const adapter = createBoardLoadResults({
    loadSummary(receivedFilters) {
      calls.push(['summary', receivedFilters])
      return Promise.resolve(summary)
    },
    loadIntegrity() {
      calls.push(['integrity'])
      return new Promise(resolve => { resolveIntegrity = resolve })
    },
  })

  const pending = adapter.load(filters).then(results => {
    settled = true
    return results
  })
  assert.deepEqual(calls, [['summary', filters], ['integrity']])
  await Promise.resolve()
  await Promise.resolve()
  assert.equal(settled, false, 'the pair must wait for the slower read')
  resolveIntegrity(integrity)
  assert.deepEqual(await pending, [
    { ok: true, value: summary },
    { ok: true, value: integrity },
  ])

  const summaryError = { kind: 'offline' }
  const rejected = createBoardLoadResults({
    loadSummary: () => Promise.reject(summaryError),
    loadIntegrity: () => Promise.reject(null),
  })
  const rejectedResults = await rejected.load(filters)
  assert.equal(rejectedResults[0].ok, false)
  assert.equal(rejectedResults[0].error, summaryError)
  assert.deepEqual(rejectedResults[1], { ok: false, error: null })

  const missingSummaryError = createBoardLoadResults({
    loadSummary: () => Promise.reject(null),
    loadIntegrity: () => Promise.resolve(integrity),
  })
  assert.deepEqual(await missingSummaryError.load(filters), [
    { ok: false, error: { kind: 'fetch-failed', detail: 'unknown' } },
    { ok: true, value: integrity },
  ])
})

test('board delegates settled summary and integrity reads to one current adapter', () => {
  assert.match(board,
    /import \{ createBoardLoadResults \} from '\.\.\/board\/board-load-results\.js'/)
  assert.equal((board.match(/from '\.\.\/board\/board-load-results\.js'/g) || []).length, 1)
  assert.match(board, /var boardLoadResults = createBoardLoadResults\(\{/)
  assert.match(board, /loadResults: boardLoadResults\.load/)
  assert.doesNotMatch(board, /function loadSummaryResult\(\)/)
  assert.doesNotMatch(board, /function loadIntegrityResult\(\)/)
  assert.doesNotMatch(board, /Promise\.all\(\[summaryLoad, integrityLoad\]\)/)
  assert.match(boardLoadResultsSource,
    /export function createBoardLoadResults\(dependencies\)/)
  assert.doesNotMatch(boardLoadResultsSource, /from '\.\.\/panels\/board\.js'/)
  assert.doesNotMatch(boardLoadResultsSource,
    /tasksApi|taskListStore|boardModal|clipboard|document|window|fetch\(/)
})

test('board load controller preserves lifecycle state, settled reads, and menu reconciliation', async () => {
  function deferred() {
    let resolve
    const promise = new Promise(onResolve => { resolve = onResolve })
    return { promise, resolve }
  }

  function harness(overrides = {}) {
    const state = overrides.state || {
      columns: null,
      summary: null,
      integrity: null,
      loading: false,
      loadingMore: true,
      error: { kind: 'old-summary-error' },
      paginationError: { kind: 'old-page-error' },
    }
    const requests = overrides.requests || [deferred()]
    const calls = {
      close: 0,
      filters: 0,
      load: [],
      render: 0,
      reconcile: [],
      requestError: [],
      afterLoad: 0,
    }
    const filters = { q: 'needle', state: 'todo' }
    let mounted = overrides.mounted ?? true
    const controller = createBoardLoadController({
      getState: () => state,
      isMounted: () => mounted,
      closeModal() { calls.close += 1 },
      getFilters() {
        calls.filters += 1
        return filters
      },
      loadResults(receivedFilters) {
        calls.load.push(receivedFilters)
        return requests[calls.load.length - 1].promise
      },
      render() { calls.render += 1 },
      reconcileMenus(stems) { calls.reconcile.push(stems) },
      requestError(error) {
        calls.requestError.push(error)
        return 'localized:' + String(error && error.kind)
      },
      afterLoad() { calls.afterLoad += 1 },
    })
    return {
      state,
      requests,
      calls,
      filters,
      controller,
      setMounted(value) { mounted = value },
    }
  }

  const notMounted = harness({ mounted: false })
  assert.equal(notMounted.controller.load(), undefined)
  assert.equal(notMounted.controller.generation(), 0)
  assert.deepEqual(notMounted.calls, {
    close: 0, filters: 0, load: [], render: 0, reconcile: [], requestError: [], afterLoad: 0,
  })

  const success = harness()
  const columns = {
    backlog: [{ stem: 'TASK_1_Backlog' }],
    pending: [{ stem: 'TASK_2_Pending' }],
    todo: [{ stem: 'TASK_3_Todo' }],
    done: [{ stem: 'TASK_4_Done' }],
  }
  const summary = { revision: 'rev-1', columns }
  const integrity = { version: 1, ok: true, findings: [] }
  assert.equal(success.controller.load(), undefined)
  assert.equal(success.controller.generation(), 1)
  assert.equal(success.state.loading, true)
  assert.equal(success.state.loadingMore, false)
  assert.equal(success.state.error, null)
  assert.equal(success.calls.close, 1)
  assert.equal(success.calls.render, 1)
  assert.equal(success.calls.filters, 1)
  assert.deepEqual(success.calls.load, [success.filters])
  success.requests[0].resolve([
    { ok: true, value: summary },
    { ok: true, value: integrity },
  ])
  await success.requests[0].promise
  assert.strictEqual(success.state.summary, summary)
  assert.strictEqual(success.state.columns, columns)
  assert.strictEqual(success.state.integrity, integrity)
  assert.equal(success.state.loading, false)
  assert.equal(success.state.error, null)
  assert.equal(success.state.paginationError, null)
  assert.deepEqual(success.calls.reconcile, [[
    'TASK_1_Backlog', 'TASK_2_Pending', 'TASK_3_Todo', 'TASK_4_Done',
  ]])
  assert.equal(success.calls.render, 2)
  assert.equal(success.calls.afterLoad, 1)

  const oldColumns = { todo: [{ stem: 'TASK_5_Old' }] }
  const oldSummary = { revision: 'old', columns: oldColumns }
  const failed = harness({
    state: {
      columns: oldColumns,
      summary: oldSummary,
      integrity: { ok: true },
      loading: false,
      loadingMore: true,
      error: { kind: 'old-summary-error' },
      paginationError: { kind: 'old-page-error' },
    },
  })
  failed.controller.load({ closeOpenModal: false })
  assert.equal(failed.calls.close, 0)
  assert.equal(failed.calls.render, 0, 'refresh keeps existing columns visible while loading')
  const summaryError = { kind: 'summary-offline' }
  const integrityError = { kind: 'integrity-offline' }
  failed.requests[0].resolve([
    { ok: false, error: summaryError },
    { ok: false, error: integrityError },
  ])
  await failed.requests[0].promise
  assert.strictEqual(failed.state.summary, oldSummary)
  assert.strictEqual(failed.state.columns, oldColumns)
  assert.strictEqual(failed.state.error, summaryError)
  assert.deepEqual(failed.state.paginationError, { kind: 'old-page-error' })
  assert.deepEqual(failed.state.integrity, {
    version: 1,
    ok: false,
    scope: 'all',
    indexStatus: 'unavailable',
    affectedStems: [],
    findings: [{
      code: 'TASK_STATE_UNAVAILABLE',
      severity: 'blocker',
      stem: null,
      paths: [],
      message: 'localized:integrity-offline',
      recovery: null,
    }],
  })
  assert.deepEqual(failed.calls.requestError, [integrityError])
  assert.deepEqual(failed.calls.reconcile, [])
  assert.equal(failed.calls.render, 1)
  assert.equal(failed.calls.afterLoad, 1)

  const summaryWithIntegrityFailure = harness()
  const mixedSummary = {
    revision: 'mixed-summary',
    columns: { backlog: [], pending: [], todo: [{ stem: 'TASK_6_Mixed' }], done: [] },
  }
  const mixedIntegrityError = { kind: 'integrity-only-offline' }
  summaryWithIntegrityFailure.controller.load({ closeOpenModal: false })
  summaryWithIntegrityFailure.requests[0].resolve([
    { ok: true, value: mixedSummary },
    { ok: false, error: mixedIntegrityError },
  ])
  await summaryWithIntegrityFailure.requests[0].promise
  assert.strictEqual(summaryWithIntegrityFailure.state.summary, mixedSummary)
  assert.deepEqual(summaryWithIntegrityFailure.calls.reconcile, [['TASK_6_Mixed']])
  assert.equal(summaryWithIntegrityFailure.state.error, null)
  assert.equal(summaryWithIntegrityFailure.state.integrity.ok, false)
  assert.equal(summaryWithIntegrityFailure.state.integrity.findings[0].message,
    'localized:integrity-only-offline')

  const integrityWithSummaryFailure = harness({
    state: {
      columns: oldColumns,
      summary: oldSummary,
      integrity: null,
      loading: false,
      loadingMore: false,
      error: null,
      paginationError: { kind: 'old-page-error' },
    },
  })
  const mixedIntegrity = { version: 1, ok: true, findings: [] }
  const mixedSummaryError = { kind: 'summary-only-offline' }
  integrityWithSummaryFailure.controller.load({ closeOpenModal: false })
  integrityWithSummaryFailure.requests[0].resolve([
    { ok: false, error: mixedSummaryError },
    { ok: true, value: mixedIntegrity },
  ])
  await integrityWithSummaryFailure.requests[0].promise
  assert.strictEqual(integrityWithSummaryFailure.state.summary, oldSummary)
  assert.strictEqual(integrityWithSummaryFailure.state.columns, oldColumns)
  assert.strictEqual(integrityWithSummaryFailure.state.integrity, mixedIntegrity)
  assert.strictEqual(integrityWithSummaryFailure.state.error, mixedSummaryError)
  assert.deepEqual(integrityWithSummaryFailure.calls.reconcile, [])
})

test('board load controller drops stale settled results and exposes one pagination generation', async () => {
  function deferred() {
    let resolve
    const promise = new Promise(onResolve => { resolve = onResolve })
    return { promise, resolve }
  }
  const first = deferred()
  const second = deferred()
  const state = {
    columns: { todo: [] },
    summary: { revision: 'initial', columns: { todo: [] } },
    integrity: null,
    loading: false,
    loadingMore: false,
    error: null,
    paginationError: null,
  }
  let loads = 0
  let renders = 0
  let afterLoads = 0
  const reconciled = []
  const requestErrors = []
  const controller = createBoardLoadController({
    getState: () => state,
    isMounted: () => true,
    closeModal() {},
    getFilters: () => ({}),
    loadResults: () => [first, second][loads++].promise,
    render() { renders += 1 },
    reconcileMenus(stems) { reconciled.push(stems) },
    requestError(error) {
      requestErrors.push(error)
      return 'unavailable'
    },
    afterLoad() { afterLoads += 1 },
  })

  controller.load({ closeOpenModal: false })
  controller.load({ closeOpenModal: false })
  assert.equal(controller.generation(), 2)
  const newestSummary = { revision: 'newest', columns: { todo: [{ stem: 'TASK_2_New' }] } }
  second.resolve([
    { ok: true, value: newestSummary },
    { ok: true, value: { ok: true, source: 'newest' } },
  ])
  await second.promise
  first.resolve([
    { ok: true, value: { revision: 'stale', columns: { todo: [{ stem: 'TASK_1_Old' }] } } },
    { ok: false, error: { kind: 'stale-integrity-error' } },
  ])
  await first.promise
  assert.strictEqual(state.summary, newestSummary)
  assert.deepEqual(state.integrity, { ok: true, source: 'newest' })
  assert.equal(renders, 1)
  assert.equal(afterLoads, 1)
  assert.deepEqual(reconciled, [['TASK_2_New']])
  assert.deepEqual(requestErrors, [])
})

test('board task-target controller validates requests and opens each resolved target once', () => {
  let columns = null
  let currentPanel = 'board'
  let hash = ''
  let replaceThrows = false
  const calls = { open: [], replace: [] }
  const controller = createBoardTaskTargetController({
    getColumns: () => columns,
    getCurrentPanel: () => currentPanel,
    getHash: () => hash,
    replaceHash(value) {
      calls.replace.push(value)
      if (replaceThrows) throw new Error('history unavailable')
    },
    openTask(folder, stem, item, section, target) {
      calls.open.push([folder, stem, item, section, target])
    },
  })

  assert.equal(controller.request('TASK_0_Invalid', 'overview', null), false)
  assert.equal(controller.openRequested(false), null)
  assert.equal(controller.request('TASK_7_Target', 'activity', { artifactId: 'a' }), true)
  assert.equal(controller.request('TASK_0_Invalid', 'overview', null), false)
  assert.equal(controller.openRequested(false), null)
  columns = { todo: [] }
  currentPanel = 'reviewer'
  assert.equal(controller.openRequested(false), null)
  currentPanel = 'board'
  assert.equal(controller.openRequested(false), false)
  const item = { stem: 'TASK_7_Target', title: 'Target' }
  columns.todo.push(item)
  assert.equal(controller.openRequested(false), true)
  assert.deepEqual(calls.open, [[
    'todo', 'TASK_7_Target', item, 'activity', { artifactId: 'a' },
  ]])
  assert.equal(controller.openRequested(false), null, 'a resolved request is one-shot')

  assert.equal(controller.request('TASK_8_Missing', 'overview', null), true)
  assert.equal(controller.openRequested(true), false)
  columns.todo.push({ stem: 'TASK_8_Missing' })
  assert.equal(controller.openRequested(false), null, 'clearIfMissing discards the stale target')

  assert.equal(controller.request('TASK_13_Replaced', 'validation', null), true)
  hash = '#board?task=TASK_9_Artifact&tab=activity&artifact=screen%2Fone'
  controller.consumeDeepLink()
  assert.deepEqual(calls.replace, ['#board'])
  const artifactItem = { stem: 'TASK_9_Artifact' }
  columns.done = [artifactItem]
  assert.equal(controller.openRequested(false), true)
  assert.deepEqual(calls.open.at(-1), [
    'done', 'TASK_9_Artifact', artifactItem, 'artifacts',
    { artifactId: 'screen/one', checkpointId: null },
  ])

  const checkpoint = 'cp-' + 'a'.repeat(32)
  hash = '#board?task=TASK_10_Checkpoint&tab=questions&artifact=screen&checkpoint=' + checkpoint
  replaceThrows = true
  assert.doesNotThrow(() => controller.consumeDeepLink())
  const checkpointItem = { stem: 'TASK_10_Checkpoint' }
  columns.pending = [checkpointItem]
  assert.equal(controller.openRequested(false), true)
  assert.deepEqual(calls.open.at(-1), [
    'pending', 'TASK_10_Checkpoint', checkpointItem, 'advanced',
    { artifactId: 'screen', checkpointId: checkpoint },
  ])
})

test('board task-target deep links reject unsafe values without poisoning later requests', () => {
  let hash = '#other?task=TASK_1_Ignored'
  let columns = { backlog: [] }
  const calls = { open: [], replace: [] }
  const controller = createBoardTaskTargetController({
    getColumns: () => columns,
    getCurrentPanel: () => 'board',
    getHash: () => hash,
    replaceHash(value) { calls.replace.push(value) },
    openTask(...args) { calls.open.push(args) },
  })

  controller.consumeDeepLink()
  assert.equal(controller.openRequested(false), null)
  assert.equal(controller.request('TASK_12_Preserved', 'validation', null), true)
  hash = '#board?task=not-a-task&tab=overview'
  controller.consumeDeepLink()
  assert.deepEqual(calls.replace, [])
  const preservedItem = { stem: 'TASK_12_Preserved' }
  columns = { backlog: [preservedItem] }
  assert.equal(controller.openRequested(false), true)
  assert.deepEqual(calls.open.at(-1), [
    'backlog', 'TASK_12_Preserved', preservedItem, 'validation', null,
  ])

  hash = '#board?task=TASK_11_Safe&tab=unknown&artifact=' + 'x'.repeat(241) +
    '&checkpoint=cp-not-hex'
  controller.consumeDeepLink()
  const item = { stem: 'TASK_11_Safe' }
  columns = { backlog: [item] }
  assert.equal(controller.openRequested(false), true)
  assert.deepEqual(calls.open.at(-1), [
    'backlog', 'TASK_11_Safe', item, 'overview',
    { artifactId: null, checkpointId: null },
  ])

  const maxArtifact = 'x'.repeat(240)
  hash = '#board?task=TASK_13_MaxArtifact&tab=activity&artifact=' + maxArtifact
  controller.consumeDeepLink()
  const maxArtifactItem = { stem: 'TASK_13_MaxArtifact' }
  columns = { todo: [maxArtifactItem] }
  assert.equal(controller.openRequested(false), true)
  assert.deepEqual(calls.open.at(-1), [
    'todo', 'TASK_13_MaxArtifact', maxArtifactItem, 'artifacts',
    { artifactId: maxArtifact, checkpointId: null },
  ])

  hash = '#board?task=TASK_14_ControlArtifact&tab=activity&artifact=bad%0Avalue'
  controller.consumeDeepLink()
  const controlArtifactItem = { stem: 'TASK_14_ControlArtifact' }
  columns = { pending: [controlArtifactItem] }
  assert.equal(controller.openRequested(false), true)
  assert.deepEqual(calls.open.at(-1), [
    'pending', 'TASK_14_ControlArtifact', controlArtifactItem, 'activity',
    { artifactId: null, checkpointId: null },
  ])

  for (const [index, section] of [
    'overview', 'activity', 'artifacts', 'advanced', 'questions', 'dependencies', 'validation',
  ].entries()) {
    const stem = 'TASK_' + String(20 + index) + '_AllowedTab'
    hash = '#board?task=' + stem + '&tab=' + section
    controller.consumeDeepLink()
    const sectionItem = { stem }
    columns = { done: [sectionItem] }
    assert.equal(controller.openRequested(false), true)
    assert.deepEqual(calls.open.at(-1), [
      'done', stem, sectionItem, section,
      { artifactId: null, checkpointId: null },
    ])
  }
})

test('board task inbox owns generation, render state, and stale-result suppression', async () => {
  function deferred() {
    let resolve
    let reject
    const promise = new Promise((onResolve, onReject) => {
      resolve = onResolve
      reject = onReject
    })
    return { promise, resolve, reject }
  }

  const requests = Array.from({ length: 6 }, deferred)
  const section = fakeElement('main')
  const calls = { load: 0, rerender: 0, composer: 0 }
  let active = true
  const controller = createBoardTaskInbox({
    t: (key) => key,
    el: fakeElement,
    createTextNode: (text) => fakeElement('#text', { text }),
    formatTimestamp: (value) => 'time:' + value,
    canPublish: () => true,
    loadEntries() { return requests[calls.load++].promise },
    publishEntry: () => Promise.reject(new Error('not used')),
    openComposer() { calls.composer += 1 },
    toast() {},
    requestError: (error) => String(error && error.kind),
    reloadStore() {},
    getSectionElement: () => section,
    isBoardActive: () => active,
    rerender() { calls.rerender += 1 },
  })

  const firstLoad = controller.load()
  section.children.length = 0
  controller.render(false)
  assert.ok(fakeTree(section).some((node) => node.textContent === 'board.inbox.loading'))
  assert.ok(fakeTree(section).some((node) => node.textContent === 'board.inbox.create'))

  const secondLoad = controller.load()
  requests[1].resolve({ entries: [{ id: 'newest', title: 'Newest', createdAt: 'now' }] })
  await secondLoad
  requests[0].reject({ kind: 'stale-failure' })
  await firstLoad
  assert.equal(calls.rerender, 1, 'stale failure must not render')

  const staleSuccess = controller.load()
  const latestSuccess = controller.load()
  requests[3].resolve({ entries: [{ id: 'latest', title: 'Latest', createdAt: 'later' }] })
  await latestSuccess
  requests[2].resolve({ entries: [{ id: 'stale', title: 'Stale', createdAt: 'old' }] })
  await staleSuccess
  assert.equal(calls.rerender, 2, 'stale success must not render')
  section.children.length = 0
  controller.render(true)
  assert.ok(fakeTree(section).some((node) => node.textContent === 'Latest'))
  assert.ok(fakeTree(section).some((node) => node.textContent === 'time:later'))
  assert.equal(fakeTree(section).some((node) => node.textContent === 'Stale'), false)
  section.children.length = 0
  controller.render(false)
  const incompleteTree = fakeTree(section)
  assert.ok(incompleteTree.some((node) => node.textContent === 'board.inbox.setupDescription'))
  assert.ok(incompleteTree.some((node) => node.textContent === 'board.inbox.waitingSetup'))
  assert.equal(incompleteTree.some((node) => node.textContent === 'board.inbox.publish'), false)

  const refreshFailure = controller.load()
  section.children.length = 0
  controller.render(true)
  assert.ok(fakeTree(section).some((node) => node.textContent === 'Latest'),
    'refresh must retain prior entries while loading')
  requests[4].reject({ kind: 'offline' })
  await refreshFailure
  section.children.length = 0
  controller.render(true)
  const errorTree = fakeTree(section)
  assert.ok(errorTree.some((node) =>
    String(node.textContent || '').startsWith('board.inbox.loadFailed')))
  const retry = errorTree.find((node) => node.textContent === 'common.retry')
  assert.ok(retry)
  retry.dispatch('click')
  assert.equal(calls.load, 6)

  active = false
  requests[5].resolve({ entries: [] })
  await Promise.resolve()
  assert.equal(calls.rerender, 3, 'inactive Board must not rerender')
  section.children.length = 0
  controller.render(false)
  const emptyIncompleteTree = fakeTree(section)
  assert.ok(emptyIncompleteTree.some((node) => node.textContent === 'board.inbox.empty'))
  assert.ok(emptyIncompleteTree.some((node) =>
    node.textContent === 'board.inbox.setupDescription'))
  section.children.length = 0
  controller.render(true)
  assert.equal(section.children.length, 0,
    'complete, loaded, empty Inbox must omit the whole panel')

  const unmountedRequest = deferred()
  let unmountedRenders = 0
  const unmountedController = createBoardTaskInbox({
    t: (key) => key,
    el: fakeElement,
    createTextNode: (text) => fakeElement('#text', { text }),
    formatTimestamp: (value) => value,
    canPublish: () => false,
    loadEntries: () => unmountedRequest.promise,
    publishEntry: () => Promise.resolve(),
    openComposer() {},
    toast() {},
    requestError: () => 'error',
    reloadStore() {},
    getSectionElement: () => null,
    isBoardActive: () => true,
    rerender() { unmountedRenders += 1 },
  })
  const unmountedLoad = unmountedController.load()
  unmountedRequest.resolve({ entries: [] })
  await unmountedLoad
  assert.equal(unmountedRenders, 0, 'settled unmounted load must not rerender')
})

test('board task inbox preserves publish guards and success/failure lifecycle', async () => {
  function deferred() {
    let resolve
    let reject
    const promise = new Promise((onResolve, onReject) => {
      resolve = onResolve
      reject = onReject
    })
    return { promise, resolve, reject }
  }

  const loadRequests = [deferred(), deferred()]
  const publishRequests = [deferred(), deferred()]
  const section = fakeElement('main')
  const calls = { load: 0, publish: [], composer: 0, toast: [], reloadStore: 0 }
  let canPublish = true
  const controller = createBoardTaskInbox({
    t(key, params) {
      if (params && Object.hasOwn(params, 'stem')) return key + ':' + params.stem
      if (params && Object.hasOwn(params, 'detail')) return key + ':' + params.detail
      return key
    },
    el: fakeElement,
    createTextNode: (text) => fakeElement('#text', { text }),
    formatTimestamp: (value) => value,
    canPublish: () => canPublish,
    loadEntries() { return loadRequests[calls.load++].promise },
    publishEntry(id) {
      calls.publish.push(id)
      return publishRequests[calls.publish.length - 1].promise
    },
    openComposer() { calls.composer += 1 },
    toast(message) { calls.toast.push(message) },
    requestError: (error) => 'error:' + String(error && error.kind),
    reloadStore() { calls.reloadStore += 1 },
    getSectionElement: () => section,
    isBoardActive: () => true,
    rerender() {},
  })

  const initialLoad = controller.load()
  loadRequests[0].resolve({ entries: [
    { title: 'Missing id', createdAt: 'a' },
    { id: 'valid', title: 'Valid', createdAt: 'b' },
  ] })
  await initialLoad

  controller.render(false)
  const create = fakeTree(section).find((node) => node.textContent === 'board.inbox.create')
  assert.ok(create)
  assert.equal(fakeTree(section).some((node) => node.textContent === 'board.inbox.publish'), false)
  create.dispatch('click')
  assert.equal(calls.composer, 1)

  section.children.length = 0
  controller.render(true)
  let publishButtons = fakeTree(section).filter(
    (node) => node.textContent === 'board.inbox.publish')
  assert.equal(publishButtons.length, 2)
  publishButtons[0].dispatch('click')
  assert.deepEqual(calls.publish, [])
  assert.notEqual(publishButtons[0].disabled, true)

  canPublish = false
  publishButtons[1].dispatch('click')
  assert.deepEqual(calls.publish, [])
  assert.notEqual(publishButtons[1].disabled, true)

  canPublish = true
  publishButtons[1].dispatch('click')
  assert.deepEqual(calls.publish, ['valid'])
  assert.equal(publishButtons[1].disabled, true)
  publishRequests[0].resolve({ stem: 'TASK_9_Published' })
  await publishRequests[0].promise
  await Promise.resolve()
  assert.deepEqual(calls.toast, ['board.inbox.published:TASK_9_Published'])
  assert.equal(calls.load, 2)
  assert.equal(calls.reloadStore, 1)

  section.children.length = 0
  controller.render(true)
  publishButtons = fakeTree(section).filter((node) => node.textContent === 'board.inbox.publish')
  const failingButton = publishButtons[1]
  failingButton.dispatch('click')
  assert.equal(failingButton.disabled, true)
  publishRequests[1].reject({ kind: 'publish-offline' })
  await publishRequests[1].promise.catch(() => {})
  await Promise.resolve()
  assert.equal(failingButton.disabled, false)
  assert.equal(calls.toast.at(-1), 'board.inbox.publishFailed:error:publish-offline')

  loadRequests[1].resolve({ entries: [] })
  await loadRequests[1].promise
})

test('board finalization controller renders safe recovery and preserves resume lifecycle', async () => {
  function deferred() {
    let resolve
    let reject
    const promise = new Promise((onResolve, onReject) => {
      resolve = onResolve
      reject = onReject
    })
    return { promise, resolve, reject }
  }

  const resumeRequests = [deferred(), deferred()]
  const state = { active: false, openStem: null }
  const calls = { content: null, resume: [], toast: [], reload: 0, order: [] }
  let snapshot = { progress: { finalizations: [] } }
  const modal = {
    open(content) {
      this.close()
      calls.order.push('open')
      calls.content = content
      state.active = true
    },
    close() {
      calls.order.push('close')
      state.active = false
      state.openStem = null
    },
    createCloseButton() { return fakeElement('button', { text: 'close' }) },
  }
  const controller = createBoardFinalizationController({
    t: (key, params) => params ? key + ':' + JSON.stringify(params) : key,
    el: fakeElement,
    getSnapshot: () => snapshot,
    hasActiveModal: () => state.active,
    getOpenStem: () => state.openStem,
    setOpenStem: (stem) => { state.openStem = stem },
    modal,
    resume(finalization) {
      calls.resume.push(finalization)
      return resumeRequests[calls.resume.length - 1].promise
    },
    toast(message) {
      calls.order.push('toast')
      calls.toast.push(message)
    },
    requestError: (error) => 'error:' + String(error && error.kind),
    reloadStore() {
      calls.order.push('reload')
      calls.reload += 1
    },
  })

  assert.deepEqual(controller.list(), [])
  snapshot = { progress: { finalizations: 'invalid' } }
  assert.deepEqual(controller.list(), [])
  const recoverable = {
    stem: 'TASK_7_Recover', status: 'recoverable', recoverable: true,
    recoveryRunning: false, phase: 'ship', observedColumn: 'todo', errorCode: 'FINALIZE',
  }
  snapshot = { progress: { finalizations: [recoverable] } }
  assert.strictEqual(controller.list()[0], recoverable)
  controller.open(recoverable)
  assert.equal(state.openStem, recoverable.stem)
  let tree = fakeTree(calls.content)
  const recoverableBanner = tree.find((node) =>
    String(node.className || '').includes('banner--warn'))
  assert.ok(recoverableBanner)
  assert.equal(recoverableBanner.textContent, 'board.finalization.explain')
  assert.ok(tree.some((node) => node.textContent ===
    'node orchestrator/tasks/task-worktree.mjs integrate --stem TASK_7_Recover'))
  assert.ok(tree.some((node) => node.textContent === 'error:FINALIZE'))
  const resumeButton = tree.find((node) => node.textContent === 'board.finalization.resume')
  assert.ok(resumeButton)
  assert.equal(resumeButton.disabled, false)
  resumeButton.dispatch('click')
  assert.equal(resumeButton.disabled, true)
  assert.equal(resumeButton.textContent, 'board.finalization.resuming')
  assert.deepEqual(calls.resume, [recoverable])
  calls.order.length = 0
  resumeRequests[0].resolve({ ok: true })
  await resumeRequests[0].promise
  await Promise.resolve()
  assert.deepEqual(calls.order, ['close', 'toast', 'reload'])
  assert.deepEqual(calls.toast, ['board.finalization.started'])

  controller.open(recoverable)
  tree = fakeTree(calls.content)
  const failedResume = tree.find((node) => node.textContent === 'board.finalization.resume')
  const message = tree.find((node) => node.className === 'board-finalization__message')
  failedResume.dispatch('click')
  resumeRequests[1].reject({ kind: 'still-locked' })
  await resumeRequests[1].promise.catch(() => {})
  await Promise.resolve()
  assert.equal(failedResume.disabled, false)
  assert.equal(failedResume.textContent, 'board.finalization.resume')
  assert.equal(message.textContent, 'error:still-locked')
})

test('board finalization controller rejects unsafe CLI shapes and refreshes one live modal', () => {
  const state = { active: false, openStem: null }
  const calls = { contents: [], close: 0 }
  let snapshot = { progress: { finalizations: [] } }
  const modal = {
    open(content) {
      this.close()
      calls.contents.push(content)
      state.active = true
    },
    close() {
      calls.close += 1
      state.active = false
      state.openStem = null
    },
    createCloseButton() { return fakeElement('button', { text: 'close' }) },
  }
  const controller = createBoardFinalizationController({
    t: (key, params) => params ? key + ':' + JSON.stringify(params) : key,
    el: fakeElement,
    getSnapshot: () => snapshot,
    hasActiveModal: () => state.active,
    getOpenStem: () => state.openStem,
    setOpenStem: (stem) => { state.openStem = stem },
    modal,
    resume: () => Promise.resolve(),
    toast() {},
    requestError: (error) => String(error && error.kind),
    reloadStore() {},
  })

  for (const finalization of [
    { stem: 'TASK_1_Unsafe;rm', status: 'recoverable', recoverable: true },
    { stem: 'TASK_2_Corrupt', status: 'corrupt', recoverable: true },
    { stem: 'TASK_3_Busy', status: 'recoverable', recoverable: true, recoveryRunning: true },
    { stem: 'TASK_4_Inspect', status: 'recoverable', recoverable: false },
  ]) {
    controller.open(finalization)
    const tree = fakeTree(calls.contents.at(-1))
    const hasSafeCli = finalization.stem === 'TASK_3_Busy'
    assert.equal(tree.some((node) => String(node.textContent || '').startsWith(
      'node orchestrator/tasks/task-worktree.mjs integrate --stem ')), hasSafeCli)
    assert.equal(tree.some((node) => node.textContent === 'board.finalization.inspect'),
      !hasSafeCli)
    const banner = tree.find((node) => String(node.className || '').includes('banner--'))
    assert.ok(banner)
    assert.equal(String(banner.className).includes('banner--danger'),
      finalization.status === 'corrupt')
    assert.equal(banner.textContent, finalization.status === 'corrupt'
      ? 'board.finalization.corrupt' : 'board.finalization.explain')
    const resume = tree.find((node) =>
      node.textContent === 'board.finalization.resume' ||
      node.textContent === 'board.finalization.resuming')
    assert.equal(resume.disabled,
      finalization.status === 'corrupt' || finalization.recoveryRunning === true ||
      finalization.recoverable !== true)
  }

  const latest = {
    stem: 'TASK_5_Live', status: 'recoverable', recoverable: true, phase: 'verify',
  }
  snapshot = { progress: { finalizations: [latest] } }
  state.active = false
  state.openStem = latest.stem
  let closesBeforeGuard = calls.close
  let opensBeforeGuard = calls.contents.length
  controller.refreshOpen()
  assert.equal(calls.close, closesBeforeGuard)
  assert.equal(calls.contents.length, opensBeforeGuard)

  state.active = true
  state.openStem = null
  closesBeforeGuard = calls.close
  opensBeforeGuard = calls.contents.length
  controller.refreshOpen()
  assert.equal(calls.close, closesBeforeGuard)
  assert.equal(calls.contents.length, opensBeforeGuard)

  state.active = true
  state.openStem = latest.stem
  const opensBeforeRefresh = calls.contents.length
  controller.refreshOpen()
  assert.equal(calls.contents.length, opensBeforeRefresh + 1)
  assert.equal(state.openStem, latest.stem)
  assert.ok(fakeTree(calls.contents.at(-1)).some((node) =>
    node.textContent === 'board.finalization.phase:' + JSON.stringify({ phase: 'verify' })))

  snapshot = { progress: { finalizations: [] } }
  state.active = true
  state.openStem = latest.stem
  const opensBeforeRemoval = calls.contents.length
  controller.refreshOpen()
  assert.equal(calls.contents.length, opensBeforeRemoval)
  assert.equal(state.active, false)
  assert.equal(state.openStem, null)
})

test('board task navigation preserves target routing, fallback, and refusal behavior', () => {
  const calls = { cards: [], terminals: [], panels: [], unavailable: 0 }
  const targetItem = { stem: 'TASK_2_Target', title: 'Target' }
  let columns = { todo: [targetItem] }
  let panelResult = true
  const controller = createBoardTaskNavigationController({
    getColumns: () => columns,
    openCard(folder, stem, item, section) {
      calls.cards.push([folder, stem, item, section])
    },
    openTerminal(key) { calls.terminals.push(key) },
    openPanel(target) {
      calls.panels.push(target)
      return panelResult
    },
    targetUnavailable() { calls.unavailable += 1 },
  })
  const current = { state: 'backlog', stem: 'TASK_1_Current', title: 'Current' }

  controller.openTarget(null, current)
  controller.openTarget({ type: 'unknown' }, current)
  assert.deepEqual(calls.cards.splice(0), [
    ['backlog', current.stem, current, undefined],
    ['backlog', current.stem, current, undefined],
  ])

  controller.openTarget({ type: 'terminal', key: 'task:TASK_1_Current' }, current)
  assert.deepEqual(calls.terminals, ['task:TASK_1_Current'])
  assert.equal(calls.cards.length, 0)
  assert.equal(calls.panels.length, 0)
  assert.equal(calls.unavailable, 0)
  controller.openTarget({ type: 'panel', panel: 'wizard' }, current)
  assert.deepEqual(calls.panels.at(-1), { panel: 'wizard', entityId: null })
  assert.equal(calls.cards.length, 0)
  assert.equal(calls.terminals.length, 1)
  assert.equal(calls.unavailable, 0)
  panelResult = false
  controller.openTarget({ type: 'panel', panel: 'app-run', entityId: 'device-1' }, current)
  assert.deepEqual(calls.panels.at(-1), { panel: 'app-run', entityId: 'device-1' })
  assert.equal(calls.unavailable, 1)
  assert.equal(calls.cards.length, 0)
  assert.equal(calls.terminals.length, 1)

  controller.openTarget({ type: 'task', stem: targetItem.stem, section: 'artifacts' }, current)
  assert.deepEqual(calls.cards.at(-1), ['todo', targetItem.stem, targetItem, 'artifacts'])
  assert.equal(calls.cards.length, 1)
  assert.equal(calls.panels.length, 2)
  assert.equal(calls.terminals.length, 1)
  assert.equal(calls.unavailable, 1)
  columns = { todo: [] }
  controller.openTarget({ type: 'task', stem: targetItem.stem }, current)
  assert.equal(calls.unavailable, 2)
  assert.equal(calls.cards.length, 1)

  controller.openSource(null, current)
  controller.openSource({ availability: 'blocked', panel: 'wizard' }, current)
  assert.equal(calls.unavailable, 4)
  assert.equal(calls.cards.length, 1)
  assert.equal(calls.panels.length, 2)
  columns = { done: [targetItem] }
  controller.openSource({
    availability: 'available', panel: 'board', entityId: targetItem.stem,
  }, current)
  assert.deepEqual(calls.cards.at(-1), ['done', targetItem.stem, targetItem, null])
  assert.equal(calls.cards.length, 2)
  assert.equal(calls.panels.length, 2)
  assert.equal(calls.unavailable, 4)
  panelResult = true
  const sourceTarget = { availability: 'available', panel: 'figma', entityId: 'file-1' }
  controller.openSource(sourceTarget, current)
  assert.strictEqual(calls.panels.at(-1), sourceTarget)
  assert.equal(calls.cards.length, 2)
  assert.equal(calls.panels.length, 3)
  assert.equal(calls.unavailable, 4)
  panelResult = false
  controller.openSource(sourceTarget, current)
  assert.equal(calls.unavailable, 5)
  assert.equal(calls.cards.length, 2)
  assert.equal(calls.panels.length, 4)
  assert.equal(calls.terminals.length, 1)
})

test('board confirm dialog preserves cancel ownership, effect order, and safe focus', () => {
  const calls = {
    opened: [], closed: 0, handlers: [], scheduled: [], confirmed: 0, order: [],
  }
  let currentCancel = function staleCancel() {}
  const controller = createBoardConfirmDialog({
    t: (key) => key,
    el: fakeElement,
    openModal(content) {
      calls.order.push('open')
      calls.opened.push(content)
      currentCancel = null
    },
    closeModal() {
      calls.order.push('close')
      calls.closed += 1
      const handler = currentCancel
      currentCancel = null
      if (typeof handler === 'function') handler()
    },
    setCancelHandler(handler) {
      calls.order.push(handler === null ? 'clear-cancel' : 'set-cancel')
      calls.handlers.push(handler)
      currentCancel = handler
    },
    schedule(callback) {
      calls.scheduled.push(callback)
      callback()
    },
  })

  const onCancel = function onCancel() {}
  controller.open({
    title: 'Confirm title', message: 'Confirm body', confirmLabel: 'Proceed',
    onCancel,
    onConfirm() {
      calls.order.push('confirm')
      calls.confirmed += 1
    },
  })
  assert.strictEqual(currentCancel, onCancel, 'onCancel must be installed after modal.open resets it')
  let tree = fakeTree(calls.opened.at(-1))
  const confirm = tree.find((node) => node.textContent === 'Proceed')
  const cancel = tree.find((node) => node.textContent === 'board.confirm.cancel')
  assert.ok(confirm.focused)
  assert.equal(cancel.focused, undefined)
  calls.handlers.length = 0
  calls.closed = 0
  calls.confirmed = 0
  calls.order.length = 0
  confirm.dispatch('click')
  assert.deepEqual(calls.order, ['clear-cancel', 'close', 'confirm'])
  assert.equal(calls.handlers.at(-1), null)
  assert.equal(calls.closed, 1)
  assert.equal(calls.confirmed, 1)
  assert.equal(currentCancel, null)

  let cancelCalls = 0
  controller.open({
    danger: true,
    onConfirm() {},
    onCancel() {
      calls.order.push('on-cancel')
      cancelCalls += 1
    },
  })
  tree = fakeTree(calls.opened.at(-1))
  const dangerConfirm = tree.find((node) => String(node.className).includes('btn--danger'))
  const dangerCancel = tree.find((node) => node.textContent === 'board.confirm.cancel')
  assert.ok(dangerConfirm)
  assert.ok(dangerCancel.focused, 'danger dialog must focus Cancel')
  assert.ok(tree.some((node) => String(node.className).includes('banner--warn')))
  const closesBeforeCancel = calls.closed
  calls.order.length = 0
  dangerCancel.dispatch('click')
  assert.deepEqual(calls.order, ['close', 'on-cancel'])
  assert.equal(calls.closed, closesBeforeCancel + 1)
  assert.equal(cancelCalls, 1)

  const throwingFocus = createBoardConfirmDialog({
    t: (key) => key,
    el(tag, attrs) {
      const node = fakeElement(tag, attrs)
      if (tag === 'button') node.focus = () => { throw new Error('focus unavailable') }
      return node
    },
    openModal() {},
    closeModal() {},
    setCancelHandler() {},
    schedule(callback) { assert.doesNotThrow(callback) },
  })
  assert.doesNotThrow(() => throwingFocus.open({}))
})

test('board open-card freshness preserves current generations and neutralizes stale modals', () => {
  function modalHarness(options = {}) {
    const inserted = []
    const actionParent = {
      insertBefore(node, reference) {
        inserted.push([node, reference])
        node.parentNode = this
      },
    }
    const actions = { parentNode: actionParent }
    const buttons = [
      { disabled: false, classList: { contains: () => false } },
      { disabled: false, classList: { contains: (name) => name === 'board-modal__close-btn' } },
    ]
    const selected = options.selectedSection ? {
      getAttribute: () => options.selectedSection,
    } : null
    const panel = {
      querySelector(selector) {
        if (selector === '.board-modal__moved-notice') {
          return options.noticePresent || inserted.length ? inserted[0] && inserted[0][0] || {} : null
        }
        if (selector === '.board-modal__actions') return actions
        if (selector === '[data-task-details-tab][aria-selected="true"]') return selected
        return null
      },
      querySelectorAll() { return buttons },
    }
    return {
      inserted, actions, buttons, panel,
      modal: { querySelector: () => options.panelMissing ? null : panel },
    }
  }

  let openCard = null
  let columns = {}
  let activeModal = null
  const calls = { abort: 0, details: [], openCards: [], order: [] }
  const controller = createBoardOpenCardFreshness({
    t: (key) => key,
    el: fakeElement,
    getOpenCard: () => openCard,
    getColumns: () => columns,
    getActiveModal: () => activeModal,
    abortDetails() { calls.abort += 1 },
    openDetails(stem, section, item) {
      calls.order.push('open-details')
      calls.details.push([stem, section, item])
    },
    setOpenCard(value) {
      calls.order.push('set-open-card')
      calls.openCards.push(value)
      openCard = value
    },
  })

  activeModal = modalHarness().modal
  controller.neutralizeIfMoved()
  assert.equal(calls.abort, 0, 'a non-card modal must not require open-card state')
  openCard = { folder: 'todo', stem: 'TASK_0_Guarded' }
  activeModal = null
  controller.neutralizeIfMoved()
  assert.equal(calls.abort, 0, 'open-card state without a live modal must remain inert')
  const currentItem = {
    stem: 'TASK_1_Current', sourceRevision: 'source-1',
    primaryAction: { actionRevision: 'action-1' },
  }
  openCard = {
    folder: 'todo', stem: currentItem.stem,
    sourceRevision: 'source-1', actionRevision: 'action-1',
  }
  columns = { todo: [currentItem] }
  activeModal = modalHarness().modal
  controller.neutralizeIfMoved()
  assert.equal(calls.abort, 0, 'current generation must remain interactive')

  openCard = {
    folder: 'todo', stem: currentItem.stem, actionRevision: 'action-1',
  }
  controller.neutralizeIfMoved()
  assert.equal(calls.abort, 0, 'missing source revision alone is a wildcard')
  openCard = {
    folder: 'todo', stem: currentItem.stem, sourceRevision: 'source-1',
  }
  controller.neutralizeIfMoved()
  assert.equal(calls.abort, 0, 'missing action revision alone is a wildcard')

  const sourceChanged = modalHarness()
  openCard = {
    folder: 'todo', stem: currentItem.stem,
    sourceRevision: 'older-source', actionRevision: 'action-1',
  }
  activeModal = sourceChanged.modal
  controller.neutralizeIfMoved()
  assert.equal(calls.abort, 1, 'source-only revision drift must neutralize')
  const sourceNotice = sourceChanged.inserted[0][0]
  const sourceReload = fakeTree(sourceNotice).find(
    (node) => node.textContent === 'taskDetails.reload')
  calls.order.length = 0
  sourceReload.dispatch('click')
  assert.deepEqual(calls.order, ['open-details', 'set-open-card'])
  assert.deepEqual(calls.details.at(-1), [currentItem.stem, 'overview', currentItem])

  const actionChanged = modalHarness()
  openCard = {
    folder: 'todo', stem: currentItem.stem,
    sourceRevision: 'source-1', actionRevision: 'older-action',
  }
  activeModal = actionChanged.modal
  controller.neutralizeIfMoved()
  assert.equal(calls.abort, 2, 'action-only revision drift must neutralize')

  const movedItem = {
    stem: currentItem.stem, sourceRevision: 'source-2',
    primaryAction: { actionRevision: 'action-2' },
  }
  const moved = modalHarness({ selectedSection: 'artifacts' })
  columns = { backlog: [], done: [movedItem] }
  openCard = {
    folder: 'backlog', stem: movedItem.stem,
    sourceRevision: 'source-1', actionRevision: 'action-1',
  }
  activeModal = moved.modal
  controller.neutralizeIfMoved()
  assert.equal(calls.abort, 3)
  assert.equal(moved.buttons[0].disabled, true)
  assert.equal(moved.buttons[1].disabled, false, 'Close must remain available')
  assert.equal(moved.inserted.length, 1)
  const movedNotice = moved.inserted[0][0]
  assert.ok(fakeTree(movedNotice).some((node) => node.textContent === 'taskDetails.stale'))
  const reload = fakeTree(movedNotice).find((node) => node.textContent === 'taskDetails.reload')
  calls.order.length = 0
  reload.dispatch('click')
  assert.deepEqual(calls.order, ['open-details', 'set-open-card'])
  assert.deepEqual(calls.details.at(-1), [movedItem.stem, 'artifacts', movedItem])
  assert.deepEqual(calls.openCards.at(-1), {
    folder: 'done', stem: movedItem.stem,
    sourceRevision: 'source-2', actionRevision: 'action-2',
  })

  const missing = modalHarness()
  openCard = { folder: 'done', stem: 'TASK_9_Missing' }
  columns = { done: [] }
  activeModal = missing.modal
  controller.neutralizeIfMoved()
  assert.equal(calls.abort, 4)
  const missingTree = fakeTree(missing.inserted[0][0])
  assert.ok(missingTree.some((node) => node.textContent === 'board.modalMoved'))
  assert.equal(missingTree.some((node) => node.textContent === 'taskDetails.reload'), false)

  const alreadyNoticed = modalHarness({ noticePresent: true })
  openCard = { folder: 'done', stem: 'TASK_10_StillMissing' }
  activeModal = alreadyNoticed.modal
  controller.neutralizeIfMoved()
  assert.equal(calls.abort, 5, 'a fresh details read must abort before the notice guard')
  assert.equal(alreadyNoticed.inserted.length, 0)

  const noPanel = modalHarness({ panelMissing: true })
  openCard = { folder: 'done', stem: 'TASK_11_NoPanel' }
  activeModal = noPanel.modal
  controller.neutralizeIfMoved()
  assert.equal(calls.abort, 6, 'missing panel must still abort stale detail work')
})

test('board render controller preserves the wizard gate and exact render lifecycle', () => {
  const delegated = []
  const completion = createBoardRenderController({
    wizardComplete(snapshot) {
      delegated.push(snapshot)
      return snapshot.allow === true
    },
  })
  assert.equal(completion.isComplete(null), false)
  assert.equal(completion.isComplete({}), false)
  assert.equal(completion.isComplete({ progress: {} }), false)
  assert.equal(completion.isComplete({ progress: { setupDone: 1 }, allow: true }), false)
  assert.equal(delegated.length, 0, 'wizard helper must not run before strict Setup completion')
  const completeSnapshot = { progress: { setupDone: true }, allow: true }
  assert.equal(completion.isComplete(completeSnapshot), true)
  assert.deepEqual(delegated, [completeSnapshot])
  assert.equal(createBoardRenderController({}).isComplete(completeSnapshot), false)

  function harness({
    complete = true,
    state = { columns: {} },
    mounted = true,
    preserveToolbar = true,
  } = {}) {
    const order = []
    const section = mounted ? fakeElement('section') : null
    const toolbarNode = fakeElement('div', { class: 'toolbar' })
    const healthNode = fakeElement('div', { class: 'health' })
    const staleNode = fakeElement('div', { class: 'stale' })
    if (section) {
      section.appendChild(toolbarNode)
      section.appendChild(healthNode)
      section.appendChild(staleNode)
      const removeChild = section.removeChild
      section.removeChild = function (child) {
        order.push(`detach:${child.className}`)
        return removeChild.call(this, child)
      }
    }
    const snapshot = { progress: { setupDone: true } }
    const scrollSnapshot = { columns: { todo: 9 } }
    const focusSnapshot = { stem: 'TASK_9_Render' }
    const controller = createBoardRenderController({
      t: (key) => key,
      el: fakeElement,
      createTextNode: (text) => ({ textContent: text }),
      getSectionElement: () => section,
      getState: () => state,
      getSnapshot() {
        order.push('snapshot')
        return snapshot
      },
      wizardComplete(value) {
        order.push('wizard-complete')
        assert.equal(value, snapshot)
        return complete
      },
      viewport: {
        captureScroll() { order.push('capture-scroll'); return scrollSnapshot },
        captureFocus() { order.push('capture-focus'); return focusSnapshot },
        restoreScroll(value) { order.push('restore-scroll'); assert.equal(value, scrollSnapshot) },
        restoreFocus(value) { order.push('restore-focus'); assert.equal(value, focusSnapshot) },
      },
      toolbar: {
        preservedElement() {
          order.push('toolbar-element')
          return preserveToolbar ? toolbarNode : null
        },
        render() { order.push('toolbar-render') },
      },
      health: {
        element() { order.push('health-element'); return healthNode },
        render(value) { order.push('health-render'); assert.equal(value, snapshot) },
      },
      inbox: {
        render(enabled) { order.push(`inbox:${enabled}`) },
      },
      taskList: {
        render() { order.push('task-list') },
      },
    })
    return { controller, focusSnapshot, healthNode, order, section, staleNode, toolbarNode }
  }

  const unmounted = harness({ mounted: false })
  unmounted.controller.render()
  assert.deepEqual(unmounted.order, [], 'unmounted render must not read or mutate state')

  function assertBefore(order, earlier, later) {
    const earlierIndex = order.indexOf(earlier)
    const laterIndex = order.indexOf(later)
    assert.ok(earlierIndex >= 0 && laterIndex >= 0 && earlierIndex < laterIndex,
      `${earlier} must precede ${later}: ${order.join(', ')}`)
  }

  const gated = harness({ complete: false })
  gated.controller.render()
  const gatedFirstDetach = gated.order.find((entry) => entry.startsWith('detach:'))
  assertBefore(gated.order, 'capture-scroll', gatedFirstDetach)
  assertBefore(gated.order, 'capture-focus', gatedFirstDetach)
  assertBefore(gated.order, 'snapshot', 'wizard-complete')
  assertBefore(gated.order, 'health-render', 'inbox:false')
  for (const forbidden of [
    'toolbar-render', 'inbox:true', 'task-list', 'restore-scroll', 'restore-focus',
  ]) assert.equal(gated.order.includes(forbidden), false)
  assert.equal(gated.section.children.includes(gated.healthNode), true)
  assert.equal(gated.section.children.includes(gated.staleNode), false)
  assert.equal(gated.section.children.includes(gated.toolbarNode), false)
  const gateTree = fakeTree(gated.section)
  assert.ok(gateTree.some((node) => node.className === 'banner banner--info'))
  assert.ok(gateTree.some((node) => node.textContent === 'board.gate '))
  assert.ok(gateTree.some((node) => node.href === '#wizard' && node.textContent === 'common.openWizard'))

  for (const [state, expectedClass, expectedText] of [
    [{ loading: true, error: { kind: 'old' }, columns: null }, 'panel-lead', 'board.loading'],
    [{ error: { kind: 'fetch-failed' }, columns: null }, 'board-unavailable', 'board.status.boardUnavailable'],
  ]) {
    const initial = harness({ state })
    initial.controller.render()
    for (const forbidden of ['task-list', 'restore-scroll', 'restore-focus']) {
      assert.equal(initial.order.includes(forbidden), false)
    }
    assert.ok(fakeTree(initial.section).some((node) =>
      node.className === expectedClass && node.textContent === expectedText))
  }

  const ready = harness({ state: { loading: true, error: { kind: 'old' }, columns: { todo: [] } } })
  ready.controller.render()
  const readyFirstDetach = ready.order.find((entry) => entry.startsWith('detach:'))
  assertBefore(ready.order, 'capture-scroll', readyFirstDetach)
  assertBefore(ready.order, 'capture-focus', readyFirstDetach)
  assertBefore(ready.order, 'health-render', 'toolbar-render')
  assertBefore(ready.order, 'toolbar-render', 'inbox:true')
  assertBefore(ready.order, 'inbox:true', 'task-list')
  assertBefore(ready.order, 'task-list', 'restore-scroll')
  assertBefore(ready.order, 'restore-scroll', 'restore-focus')
  assert.deepEqual(ready.section.children, [ready.toolbarNode, ready.healthNode])

  const replaceToolbar = harness({ preserveToolbar: false })
  replaceToolbar.controller.render()
  assert.equal(replaceToolbar.section.children.includes(replaceToolbar.toolbarNode), false,
    'a stale toolbar must not survive when the toolbar owner declines preservation')
})

test('board refresh clock coalesces reloads and renders only live time-based state', () => {
  let mounted = true
  let active = true
  let snapshot = { progress: { inProgress: [] } }
  let pending = []
  let nextTimer = 1
  const cancelled = []
  const timeouts = []
  const intervals = []
  const loads = []
  const renders = []
  let snapshotReads = 0
  let pendingReads = 0
  let mountedReads = 0
  let activeReads = 0
  const clock = createBoardRefreshClock({
    scheduleTimeout(callback, delay) {
      const entry = { id: nextTimer++, callback, delay }
      timeouts.push(entry)
      return entry.id
    },
    cancelTimeout(id) { cancelled.push(id) },
    scheduleInterval(callback, delay) {
      intervals.push({ callback, delay })
      return intervals.length
    },
    isMounted() { mountedReads += 1; return mounted },
    isBoardActive() { activeReads += 1; return active },
    getSnapshot() { snapshotReads += 1; return snapshot },
    pendingRequests(value) {
      pendingReads += 1
      assert.equal(value, snapshot)
      return pending
    },
    reloadBoard(options) { loads.push(options) },
    render() { renders.push('render') },
  })

  clock.scheduleRefresh()
  clock.scheduleRefresh()
  assert.deepEqual(timeouts.map((entry) => entry.delay), [150, 150])
  assert.deepEqual(cancelled, [timeouts[0].id])
  timeouts[1].callback()
  assert.deepEqual(loads, [{ closeOpenModal: false }])

  clock.scheduleRefresh()
  assert.deepEqual(cancelled, [timeouts[0].id],
    'a completed debounce handle must be cleared before scheduling the next refresh')
  active = false
  timeouts[2].callback()
  assert.equal(loads.length, 1, 'inactive-at-fire debounce must not reload')
  active = true

  clock.scheduleRefresh()
  mounted = false
  const activeReadsBeforeUnmountedFire = activeReads
  timeouts[3].callback()
  assert.equal(activeReads, activeReadsBeforeUnmountedFire,
    'unmounted-at-fire debounce must short-circuit the active-panel check')
  assert.equal(loads.length, 1, 'unmounted-at-fire debounce must not reload')
  mounted = true

  clock.startClock()
  clock.startClock()
  assert.equal(intervals.length, 1)
  assert.equal(intervals[0].delay, 30000)

  mounted = false
  const activeReadsBeforeUnmountedTick = activeReads
  clock.tick()
  assert.equal(snapshotReads, 0, 'unmounted tick must not read the snapshot')
  assert.equal(activeReads, activeReadsBeforeUnmountedTick,
    'unmounted tick must short-circuit the active-panel check')
  mounted = true
  active = false
  clock.tick()
  assert.equal(snapshotReads, 0, 'inactive tick must not read the snapshot')
  active = true

  clock.tick()
  assert.equal(snapshotReads, 1)
  assert.equal(pendingReads, 1)
  assert.equal(renders.length, 0)

  for (const idleSnapshot of [null, {}, { progress: { inProgress: 'invalid' } }]) {
    snapshot = idleSnapshot
    pending = []
    const pendingReadsBeforeIdle = pendingReads
    const rendersBeforeIdle = renders.length
    clock.tick()
    assert.equal(pendingReads, pendingReadsBeforeIdle + 1,
      'missing or invalid in-progress state must fall through to queued requests')
    assert.equal(renders.length, rendersBeforeIdle,
      'missing or invalid in-progress state must stay idle without queued requests')
  }

  snapshot = { progress: { inProgress: [{ stem: 'TASK_1_Live' }] } }
  const pendingReadsBeforeLive = pendingReads
  clock.tick()
  assert.equal(pendingReads, pendingReadsBeforeLive,
    'live work must short-circuit pending request projection')
  assert.equal(renders.length, 1)

  snapshot = { progress: { inProgress: 'invalid' } }
  pending = [{ stem: 'TASK_2_Queued' }]
  const pendingReadsBeforeQueued = pendingReads
  clock.tick()
  assert.equal(pendingReads, pendingReadsBeforeQueued + 1)
  assert.equal(renders.length, 2)

  intervals[0].callback()
  assert.equal(renders.length, 3, 'installed interval must execute the same tick contract')
})

test('board task-details shell preserves loading, target mapping, and stale error refusal', () => {
  const close = fakeElement('button', { text: 'close' })
  let activeModal = null
  const created = []
  const insertionOrder = []
  const shell = createBoardTaskDetailsShell({
    t: (key) => key,
    el(tag, attrs) {
      const node = fakeElement(tag, attrs)
      created.push(node)
      node.insertAdjacentElement = function (position, child) {
        insertionOrder.push(`insert:${this.textContent || this.className}:${position}:${child.textContent}`)
      }
      const baseFocus = node.focus
      node.focus = function () {
        insertionOrder.push(`focus:${this.textContent}`)
        baseFocus.call(this)
      }
      return node
    },
    createCloseButton: () => close,
    getActiveModal: () => activeModal,
    requestError: (error) => `error:${error.kind}`,
  })

  const loading = shell.loading('TASK_3_Details', { title: 'Details title' })
  const loadingTree = fakeTree(loading)
  assert.ok(loadingTree.some((node) => node.className === 'board-modal__title' && node.textContent === 'Details title'))
  assert.ok(loadingTree.some((node) => node.className === 'board-modal__stem' && node.textContent === 'TASK_3_Details'))
  assert.ok(loadingTree.some((node) => node.className === 'task-details__loading' && node.textContent === 'taskDetails.loading'))
  assert.equal(loadingTree.includes(close), true)
  assert.ok(fakeTree(shell.loading('TASK_4_Fallback')).some((node) =>
    node.className === 'board-modal__title' && node.textContent === 'TASK_4_Fallback'))

  for (const [section, expected] of [
    ['questions', 'overview'], ['dependencies', 'overview'], ['validation', 'overview'],
    ['artifacts', 'artifacts'], ['advanced', 'advanced'], ['overview', 'overview'],
    ['unknown', 'overview'], [null, 'overview'],
  ]) assert.equal(shell.sectionForTarget({ section }), expected)
  assert.equal(shell.sectionForTarget(null), 'overview')

  const modalToken = {}
  activeModal = modalToken
  const paragraph = fakeElement('p', { class: 'task-details__loading' })
  paragraph.insertAdjacentElement = function (position, child) {
    insertionOrder.push(`insert:paragraph:${position}:${child.textContent}`)
  }
  const errorLoading = {
    isConnected: true,
    querySelector(selector) {
      assert.equal(selector, '.task-details__loading')
      return paragraph
    },
  }
  let retries = 0
  let secondaryRuns = 0
  let secondaryReads = 0
  shell.showLoadError({
    modalToken,
    loading: errorLoading,
    error: { kind: 'offline' },
    onRetry() { retries += 1 },
    getSecondaryAction() {
      secondaryReads += 1
      return {
        label: 'Drop',
        className: 'btn btn--danger btn--sm',
        onClick() { secondaryRuns += 1 },
      }
    },
  })
  assert.equal(paragraph.className, 'banner banner--warn')
  assert.equal(paragraph.textContent, 'error:offline')
  assert.equal(paragraph.getAttribute('role'), 'alert')
  const retry = created.find((node) => node.textContent === 'taskDetails.reload')
  const secondary = created.find((node) => node.textContent === 'Drop')
  assert.ok(retry)
  assert.ok(secondary)
  assert.deepEqual(insertionOrder, [
    'insert:paragraph:afterend:taskDetails.reload',
    'insert:taskDetails.reload:afterend:Drop',
    'focus:taskDetails.reload',
  ])
  retry.dispatch('click')
  secondary.dispatch('click')
  assert.equal(retries, 1)
  assert.equal(secondaryRuns, 1)
  assert.equal(secondaryReads, 1)

  created.length = 0
  insertionOrder.length = 0
  const retryOnlyParagraph = fakeElement('p', { class: 'task-details__loading' })
  retryOnlyParagraph.insertAdjacentElement = function (position, child) {
    insertionOrder.push(`insert:paragraph:${position}:${child.textContent}`)
  }
  let retryOnlyRuns = 0
  let retryOnlySecondaryReads = 0
  shell.showLoadError({
    modalToken,
    loading: {
      isConnected: true,
      querySelector: () => retryOnlyParagraph,
    },
    error: { kind: 'retry-only' },
    onRetry() { retryOnlyRuns += 1 },
    getSecondaryAction() {
      retryOnlySecondaryReads += 1
      return null
    },
  })
  assert.equal(retryOnlySecondaryReads, 1)
  assert.equal(created.some((node) => node.textContent === 'Drop'), false)
  const retryOnly = created.find((node) => node.textContent === 'taskDetails.reload')
  assert.ok(retryOnly)
  assert.deepEqual(insertionOrder, [
    'insert:paragraph:afterend:taskDetails.reload',
    'focus:taskDetails.reload',
  ])
  retryOnly.dispatch('click')
  assert.equal(retryOnlyRuns, 1)

  const staleParagraph = fakeElement('p', { class: 'task-details__loading' })
  const staleLoading = { isConnected: true, querySelector: () => staleParagraph }
  activeModal = {}
  shell.showLoadError({
    modalToken,
    loading: staleLoading,
    error: { kind: 'stale' },
    onRetry() { throw new Error('stale retry must not be installed') },
    getSecondaryAction() { throw new Error('stale secondary action must stay lazy') },
  })
  assert.equal(staleParagraph.className, 'task-details__loading')
  activeModal = modalToken
  staleLoading.isConnected = false
  shell.showLoadError({ modalToken, loading: staleLoading, error: { kind: 'closed' } })
  assert.equal(staleParagraph.className, 'task-details__loading')

  const missingParagraph = { isConnected: true, querySelector: () => null }
  shell.showLoadError({
    modalToken,
    loading: missingParagraph,
    error: { kind: 'missing' },
    getSecondaryAction() { throw new Error('missing secondary action must stay lazy') },
  })
})

test('Figma screens controller preserves pull readiness and the three-way Run gate', async () => {
  const created = []
  const opened = []
  const scheduled = []
  const toasts = []
  const sessions = []
  const terminals = []
  const modalOrder = []
  const effects = []
  let connected = true
  let needsScreens = false
  let connectivityReads = 0
  let needsReads = 0
  let cancelHandler = null
  let cancelInvocations = 0
  let closes = 0
  let sessionResult = Promise.resolve({ sent: true })

  const controller = createBoardFigmaScreensController({
    t(key, params) { return params && params.detail ? `${key}:${params.detail}` : key },
    el(tag, attrs) {
      const node = fakeElement(tag, attrs)
      created.push(node)
      return node
    },
    needsUnpulledScreens() { needsReads += 1; return needsScreens },
    isConnected() { connectivityReads += 1; return connected },
    sessionAction(key, action) {
      sessions.push({ key, action })
      effects.push(`session:${key}:${action}`)
      return sessionResult
    },
    screenPullAction: 'screen-pull',
    openTerminal(key) { terminals.push(key); effects.push(`terminal:${key}`) },
    toast(message) { toasts.push(message); effects.push(`toast:${message}`) },
    actionError(error) { return error.kind || 'unknown' },
    openModal(content) {
      const priorHandler = cancelHandler
      cancelHandler = null
      if (priorHandler) {
        cancelInvocations += 1
        priorHandler()
      }
      modalOrder.push('open')
      effects.push('open')
      opened.push(content)
    },
    closeModal() {
      modalOrder.push('close')
      effects.push('close')
      closes += 1
      const handler = cancelHandler
      cancelHandler = null
      if (handler) {
        cancelInvocations += 1
        handler()
      }
    },
    setCancelHandler(handler) {
      modalOrder.push(handler ? 'set' : 'clear')
      effects.push(handler ? 'set' : 'clear')
      cancelHandler = handler
    },
    schedule(callback) { scheduled.push(callback) },
  })

  assert.equal(connectivityReads, 0)
  assert.equal(needsReads, 0)
  assert.equal(controller.sessionKey('TASK_1'), 'figma:screens:TASK_1')
  assert.equal(controller.isPullReady(), true)
  const readsBeforeNoopGate = connectivityReads
  assert.equal(await controller.confirmBeforeRun('TASK_1'), true)
  assert.equal(opened.length, 0)
  assert.equal(connectivityReads, readsBeforeNoopGate)

  needsScreens = true
  sessionResult = Promise.resolve({ sent: false })
  const pullDecision = controller.confirmBeforeRun('TASK_2')
  const pullActions = opened.at(-1).children[2].children
  const [pull, runAnyway, cancel] = pullActions
  assert.equal(pull.disabled, false)
  assert.equal(runAnyway.textContent, 'board.screensWarn.runAnyway')
  assert.equal(cancel.textContent, 'board.confirm.cancel')
  assert.deepEqual(modalOrder.slice(-2), ['open', 'set'])
  assert.equal(typeof cancelHandler, 'function')
  scheduled.shift()()
  assert.equal(pull.focused, true)
  const cancelInvocationsBeforePull = cancelInvocations
  const pullEffectStart = effects.length
  pull.dispatch('click')
  assert.equal(await pullDecision, false)
  await Promise.resolve()
  await Promise.resolve()
  assert.deepEqual(sessions, [{ key: 'figma:screens:TASK_2', action: 'screen-pull' }])
  assert.deepEqual(terminals, ['figma:screens:TASK_2'])
  assert.ok(toasts.includes('run.busy'))
  assert.equal(cancelInvocations, cancelInvocationsBeforePull)
  assert.deepEqual(modalOrder.slice(-3), ['set', 'clear', 'close'])
  assert.deepEqual(effects.slice(pullEffectStart, pullEffectStart + 3), [
    'clear',
    'close',
    'session:figma:screens:TASK_2:screen-pull',
  ])
  assert.ok(effects.indexOf('toast:run.busy') <
    effects.indexOf('terminal:figma:screens:TASK_2'))
  assert.equal(closes, 1)

  const runDecision = controller.confirmBeforeRun('TASK_3')
  opened.at(-1).children[2].children[1].dispatch('click')
  assert.equal(await runDecision, true)
  assert.equal(closes, 2)

  const cancelDecision = controller.confirmBeforeRun('TASK_4')
  opened.at(-1).children[2].children[2].dispatch('click')
  assert.equal(await cancelDecision, false)
  assert.equal(closes, 3)

  const escapeDecision = controller.confirmBeforeRun('TASK_ESCAPE')
  const escapeHandler = cancelHandler
  cancelHandler = null
  escapeHandler()
  escapeHandler()
  assert.equal(await escapeDecision, false)

  const focusDecision = controller.confirmBeforeRun('TASK_FOCUS')
  const focusPull = opened.at(-1).children[2].children[0]
  focusPull.focus = function () { throw new Error('detached') }
  assert.doesNotThrow(() => scheduled.at(-1)())
  opened.at(-1).children[2].children[2].dispatch('click')
  assert.equal(await focusDecision, false)

  const forceCloseDecision = controller.confirmBeforeRun('TASK_FORCE_CLOSE')
  let forceCloseSettled = false
  forceCloseDecision.then(() => { forceCloseSettled = true })
  const forceCloseHandler = cancelHandler
  cancelHandler = null
  await Promise.resolve()
  assert.equal(forceCloseSettled, false)
  forceCloseHandler()
  assert.equal(await forceCloseDecision, false)

  const lostReadinessDecision = controller.confirmBeforeRun('TASK_5')
  const previouslyEnabledPull = opened.at(-1).children[2].children[0]
  assert.equal(previouslyEnabledPull.disabled, false)
  connected = false
  previouslyEnabledPull.dispatch('click')
  assert.equal(await lostReadinessDecision, false)
  assert.equal(sessions.length, 1)
  assert.equal(toasts.at(-1), 'board.screensWarn.pullUnavailable')
  assert.equal(closes, 5)

  assert.equal(controller.isPullReady(), false)
  controller.triggerPull('TASK_6')
  assert.equal(sessions.length, 1)
  assert.equal(toasts.at(-1), 'board.screensWarn.pullUnavailable')
  const unavailableDecision = controller.confirmBeforeRun('TASK_7')
  const unavailablePull = opened.at(-1).children[2].children[0]
  assert.equal(unavailablePull.disabled, true)
  assert.equal(unavailablePull.getAttribute('title'), 'board.screensWarn.pullUnavailable')
  opened.at(-1).children[2].children[1].dispatch('click')
  assert.equal(await unavailableDecision, true)

  connected = true
  sessionResult = Promise.reject({ kind: 'offline' })
  controller.triggerPull('TASK_8')
  await Promise.resolve()
  await Promise.resolve()
  assert.equal(toasts.at(-1), 'board.screensWarn.pullFailed:offline')
  assert.equal(terminals.includes('figma:screens:TASK_8'), false)

  const busyToastCount = toasts.filter((message) => message === 'run.busy').length
  sessionResult = Promise.resolve({ sent: true })
  controller.triggerPull('TASK_9')
  await Promise.resolve()
  await Promise.resolve()
  assert.deepEqual(sessions.at(-1), {
    key: 'figma:screens:TASK_9',
    action: 'screen-pull',
  })
  assert.equal(terminals.at(-1), 'figma:screens:TASK_9')
  assert.equal(toasts.filter((message) => message === 'run.busy').length, busyToastCount)
})

test('task action controller preserves Run admission, confirmation, and response handling', async () => {
  const toasts = []
  const executions = []
  const reloads = []
  const confirmations = []
  const requestErrors = []
  const effects = []
  let recoveryBlocked = false
  let needsScreens = false
  let screenDecision = false
  let screenConfirmations = 0
  let executionResult = Promise.resolve({ accepted: true })
  let storeResult = Promise.resolve()

  function actionButton() {
    const button = fakeElement('button')
    let disabled = false
    const setAttribute = button.setAttribute
    const removeAttribute = button.removeAttribute
    Object.defineProperty(button, 'disabled', {
      get() { return disabled },
      set(value) { disabled = value; effects.push(`disabled:${value}`) },
      configurable: true,
    })
    button.setAttribute = function (name, value) {
      effects.push(`set:${name}:${value}`)
      setAttribute.call(button, name, value)
    }
    button.removeAttribute = function (name) {
      effects.push(`remove:${name}`)
      removeAttribute.call(button, name)
    }
    return button
  }

  const presentations = []
  const controller = createBoardTaskActionController({
    t(key, params) { return params && params.detail ? `${key}:${params.detail}` : key },
    startupRecoveryBlocksMutation() { return recoveryBlocked },
    needsUnpulledScreens() { return needsScreens },
    confirmScreensBeforeRun() {
      screenConfirmations += 1
      return Promise.resolve(screenDecision)
    },
    confirm(options) { confirmations.push(options) },
    executeAction(stem, action, confirmation) {
      effects.push('execute')
      executions.push({ stem, action, confirmation })
      return executionResult
    },
    toast(message) { toasts.push(message); effects.push(`toast:${message}`) },
    presentEnqueueResult(response, stem) {
      presentations.push({ response, stem })
      effects.push('present')
    },
    requestError(error) {
      requestErrors.push(error)
      effects.push('request-error')
      return error && error.kind || String(error)
    },
    reloadStore() { reloads.push('store'); effects.push('store'); return storeResult },
    reloadBoard() { reloads.push('board'); effects.push('board') },
    copyText() {},
    openSource() {},
    openEdit() {},
    runDrop() {},
    runReopen() {},
    restoreOverflowFocus() {},
    loadPrompt() { return Promise.resolve(null) },
  })
  const row = { stem: 'TASK_10', state: 'todo' }
  const action = { kind: 'run', requiresConfirmation: true }
  const blockedButton = actionButton()

  recoveryBlocked = true
  needsScreens = true
  controller.run(row, action, blockedButton)
  assert.equal(blockedButton.disabled, true)
  assert.equal(blockedButton.getAttribute('title'), 'board.startupRecovery.actionBlocked')
  assert.equal(executions.length, 0)
  assert.equal(screenConfirmations, 0)
  assert.equal(confirmations.length, 0)
  assert.equal(toasts.at(-1), 'board.startupRecovery.actionBlocked')

  recoveryBlocked = false
  effects.length = 0
  const canceledScreenButton = actionButton()
  controller.run(row, action, canceledScreenButton)
  await Promise.resolve()
  assert.equal(executions.length, 0)
  assert.equal(screenConfirmations, 1)
  assert.equal(confirmations.length, 0)
  assert.equal(canceledScreenButton.disabled, false)
  assert.equal(canceledScreenButton.getAttribute('aria-busy'), null)
  assert.deepEqual(effects, [])

  screenDecision = true
  let resolveStore
  storeResult = new Promise((resolve) => { resolveStore = resolve })
  const screenButton = actionButton()
  controller.run(row, action, screenButton)
  await Promise.resolve()
  assert.equal(screenButton.disabled, true)
  assert.equal(screenButton.getAttribute('aria-busy'), 'true')
  assert.equal(executions.length, 1)
  assert.equal(executions[0].action, action)
  assert.equal(executions[0].confirmation, null)
  await Promise.resolve()
  await Promise.resolve()
  assert.deepEqual(reloads, ['store'])
  resolveStore()
  await Promise.resolve()
  assert.deepEqual(reloads, ['store', 'board'])
  assert.equal(presentations.length, 1, 'the card reports the enqueue verdict through the shared presenter')
  assert.equal(presentations[0].stem, 'TASK_10')
  assert.equal(toasts.includes('board.action.started'), false,
    'a fixed "started" toast would claim success for a run nothing can drain')

  needsScreens = false
  storeResult = Promise.resolve()
  reloads.length = 0
  const confirmedAction = { kind: 'drop', requiresConfirmation: true }
  const confirmedButton = actionButton()
  controller.run(row, confirmedAction, confirmedButton)
  assert.equal(confirmations.length, 1)
  assert.equal(executions.length, 1)
  assert.equal(confirmedButton.disabled, false)
  assert.equal(confirmedButton.getAttribute('aria-busy'), null)
  assert.equal(confirmations[0].title, 'board.action.confirmTitle')
  assert.equal(confirmations[0].message, 'board.action.confirmBody')
  assert.equal(confirmations[0].confirmLabel, 'board.action.confirm')
  confirmations[0].onConfirm()
  assert.equal(executions.at(-1).action, confirmedAction)
  assert.equal(executions.at(-1).confirmation, true)
  await Promise.resolve()
  await Promise.resolve()

  let successResponse = null
  const successToasts = toasts.length
  const successReloads = reloads.length
  const successRequestErrors = requestErrors.length
  const successButton = actionButton()
  controller.run(row, { kind: 'prepare', requiresConfirmation: false }, successButton, {
    onSuccess(response) { successResponse = response },
  })
  await Promise.resolve()
  assert.deepEqual(successResponse, { accepted: true })
  assert.equal(toasts.length, successToasts)
  assert.equal(reloads.length, successReloads)
  assert.equal(requestErrors.length, successRequestErrors)
  assert.equal(successButton.disabled, true)
  assert.equal(successButton.getAttribute('aria-busy'), 'true')

  for (const staleKind of ['task-action-stale', 'action-stale']) {
    effects.length = 0
    reloads.length = 0
    toasts.length = 0
    requestErrors.length = 0
    executionResult = Promise.reject({ kind: staleKind })
    const staleButton = actionButton()
    controller.run(row, { kind: 'prepare', requiresConfirmation: false }, staleButton)
    await Promise.resolve()
    await Promise.resolve()
    assert.equal(staleButton.disabled, false)
    assert.equal(staleButton.getAttribute('aria-busy'), null)
    assert.deepEqual(toasts, ['board.action.stale'])
    assert.deepEqual(reloads, ['board'])
    assert.deepEqual(requestErrors, [])
    assert.ok(effects.indexOf('disabled:false') < effects.indexOf('toast:board.action.stale'))
    assert.ok(effects.indexOf('remove:aria-busy') < effects.indexOf('board'))
  }

  effects.length = 0
  reloads.length = 0
  toasts.length = 0
  requestErrors.length = 0
  executionResult = Promise.reject({ kind: 'offline' })
  const failedButton = actionButton()
  controller.run(row, { kind: 'prepare', requiresConfirmation: false }, failedButton)
  await Promise.resolve()
  await Promise.resolve()
  assert.equal(failedButton.disabled, false)
  assert.equal(failedButton.getAttribute('aria-busy'), null)
  assert.deepEqual(toasts, ['board.action.failed:offline'])
  assert.deepEqual(reloads, [])
  assert.equal(requestErrors.length, 1)
  assert.equal(requestErrors[0].kind, 'offline')
  assert.ok(effects.indexOf('disabled:false') < effects.indexOf('request-error'))
  assert.ok(effects.indexOf('remove:aria-busy') <
    effects.indexOf('toast:board.action.failed:offline'))
})

test('task action controller keeps overflow routing and prompt failures exact', async () => {
  const calls = []
  const toasts = []
  const effects = []
  let recoveryBlocked = false
  let promptResult = Promise.resolve({ manualFallback: true, text: 'prompt text' })
  const controller = createBoardTaskActionController({
    t(key, params) { return params && params.detail ? `${key}:${params.detail}` : key },
    startupRecoveryBlocksMutation() { return recoveryBlocked },
    needsUnpulledScreens() { return false },
    confirmScreensBeforeRun() { return Promise.resolve(true) },
    confirm() {},
    executeAction() { return Promise.resolve({}) },
    toast(message) { toasts.push(message); effects.push(`toast:${message}`) },
    requestError(error) { return error.kind || String(error) },
    reloadStore() { return Promise.resolve() },
    reloadBoard() { calls.push(['reload']); effects.push('reload') },
    copyText(text) { calls.push(['copy', text]) },
    openSource(target, row) { calls.push(['source', target, row.stem]) },
    openEdit(stem, row) { calls.push(['edit', stem, row.state]) },
    runDrop(stem, state, action, options) { calls.push(['drop', stem, state, action]); options.onCancel() },
    runReopen(stem, action) { calls.push(['reopen', stem, action]) },
    restoreOverflowFocus(stem) { calls.push(['focus', stem]) },
    loadPrompt(stem, action) {
      calls.push(['prompt', stem, action])
      effects.push('load-prompt')
      return promptResult
    },
  })
  const row = { stem: 'TASK_20', state: 'backlog', sourceTarget: { panel: 'board' } }
  const serverAction = { kind: 'drop' }

  controller.overflow('copy-id', row, null)
  controller.overflow('source', row, null)
  assert.deepEqual(calls, [
    ['copy', row.stem],
    ['source', row.sourceTarget, row.stem],
  ])

  calls.length = 0
  toasts.length = 0
  recoveryBlocked = true
  controller.overflow('copy-id', row, null)
  controller.overflow('source', row, null)
  controller.overflow('edit', row, null)
  controller.overflow('reopen', row, serverAction)
  assert.deepEqual(calls, [
    ['copy', row.stem],
    ['source', row.sourceTarget, row.stem],
  ])
  assert.deepEqual(toasts, [
    'board.startupRecovery.actionBlocked',
    'board.startupRecovery.actionBlocked',
  ])

  recoveryBlocked = false
  calls.length = 0
  controller.overflow('edit', row, null)
  controller.overflow('drop', row, serverAction)
  controller.overflow('reopen', row, serverAction)
  assert.deepEqual(calls, [
    ['edit', row.stem, row.state],
    ['drop', row.stem, row.state, serverAction],
    ['focus', row.stem],
    ['reopen', row.stem, serverAction],
  ])

  toasts.length = 0
  controller.overflow('drop', row, null)
  controller.overflow('reopen', row, null)
  assert.deepEqual(toasts, [
    'board.action.targetUnavailable',
    'board.action.targetUnavailable',
  ])

  calls.length = 0
  toasts.length = 0
  effects.length = 0
  controller.overflow('copy-prompt', row, serverAction)
  assert.deepEqual(effects, [
    'toast:board.overflow.preparing_prompt',
    'load-prompt',
  ])
  await Promise.resolve()
  await Promise.resolve()
  assert.deepEqual(calls, [
    ['prompt', row.stem, serverAction],
    ['copy', 'prompt text'],
  ])

  for (const invalidResponse of [
    null,
    { manualFallback: false, text: 'wrong' },
    { manualFallback: true, text: 42 },
  ]) {
    calls.length = 0
    toasts.length = 0
    promptResult = Promise.resolve(invalidResponse)
    controller.overflow('copy-prompt', row, serverAction)
    await Promise.resolve()
    assert.deepEqual(calls, [['prompt', row.stem, serverAction]])
    assert.deepEqual(toasts, [
      'board.overflow.preparing_prompt',
      'board.action.failed:invalid-response',
    ])
  }

  for (const staleKind of ['action-stale', 'task-action-stale']) {
    calls.length = 0
    toasts.length = 0
    promptResult = Promise.reject({ kind: staleKind })
    controller.overflow('copy-prompt', row, serverAction)
    await Promise.resolve()
    await Promise.resolve()
    assert.deepEqual(calls, [
      ['prompt', row.stem, serverAction],
      ['reload'],
    ])
    assert.deepEqual(toasts, [
      'board.overflow.preparing_prompt',
      'board.action.stale',
    ])
  }

  calls.length = 0
  toasts.length = 0
  promptResult = Promise.reject({ kind: 'offline' })
  controller.overflow('copy-prompt', row, serverAction)
  await Promise.resolve()
  await Promise.resolve()
  assert.deepEqual(calls, [['prompt', row.stem, serverAction]])
  assert.deepEqual(toasts, [
    'board.overflow.preparing_prompt',
    'board.action.failed:offline',
  ])

  toasts.length = 0
  controller.overflow('unknown', row, null)
  assert.deepEqual(toasts, ['board.action.targetUnavailable'])
})

test('standalone Figma pull uses the controller key and one live readiness owner', () => {
  const start = board.indexOf('function figmaScreensButton(folder, stem) {')
  const end = board.indexOf('function sameDropImpact(a, b) {', start)
  assert.ok(start >= 0 && end > start, 'standalone Figma pull wiring bounds missing')
  const wiring = board.slice(start, end)

  assert.equal((wiring.match(
    /key: boardFigmaScreensController\.sessionKey\(stem\)/g) || []).length, 1)
  assert.equal((wiring.match(
    /boardFigmaScreensController\.isPullReady\(\)/g) || []).length, 3)
  assert.match(wiring,
    /getPrompt: function \(\) \{\s*if \(!boardFigmaScreensController\.isPullReady\(\)\)/)
  assert.match(wiring,
    /onRun: function \(key\) \{ return tasksApi\.figmaSessionAction\(key, FIGMA_SESSION_ACTION\.SCREEN_PULL\); \}/)
  assert.match(wiring,
    /isDisabled: function \(\) \{ return !boardFigmaScreensController\.isPullReady\(\); \}/)
  assert.match(wiring,
    /status: function \(\) \{[\s\S]*if \(!boardFigmaScreensController\.isPullReady\(\)\)/)
  assert.doesNotMatch(wiring, /figmaScreensSessionKey|figmaPullReady/)
})

test('board delegates root rendering, refresh timers, and details shell to one owner each', () => {
  function wiring(startMarker, endMarker, label) {
    const start = board.indexOf(startMarker)
    const end = board.indexOf(endMarker, start)
    assert.ok(start >= 0 && end > start, `${label} wiring bounds missing`)
    return board.slice(start, end)
  }

  const detailsShellWiring = wiring(
    'var boardTaskDetailsShell = createBoardTaskDetailsShell({',
    'var boardConfirmDialog = createBoardConfirmDialog({',
    'details shell')
  const renderWiring = wiring(
    'boardRenderController = createBoardRenderController({',
    'boardRefreshClock = createBoardRefreshClock({',
    'render controller')
  const refreshClockWiring = wiring(
    'boardRefreshClock = createBoardRefreshClock({',
    'var visualEvidenceStatus = createVisualEvidenceStatus({',
    'refresh clock')
  const detailsBody = wiring(
    'function openTaskDetails(stem, preferredSection, fallbackItem, deepTarget) {',
    'function openCardModal(folder, stem, item, section, deepTarget) {',
    'details body')
  const detailsNavigation = wiring(
    'function navigation(action) {',
    'function execute(action, button, input) {',
    'details navigation')
  const detailsRejectionStart = detailsBody.lastIndexOf('}, function (error) {')
  assert.ok(detailsRejectionStart >= 0, 'Task Details rejection callback missing')
  const detailsRejection = detailsBody.slice(detailsRejectionStart)
  const storeChange = wiring(
    'function onStoreChange() {',
    '// Relative activity labels',
    'store change')
  const mount = wiring(
    'mount: function (rootEl) {',
    'refresh: function () {',
    'mount')

  for (const moduleName of [
    'board-render-controller',
    'board-refresh-clock',
    'board-task-details-shell',
  ]) {
    assert.equal((board.match(new RegExp(
      `from '\\.\\.\\/board\\/${moduleName}\\.js'`, 'g')) || []).length, 1)
  }

  assert.match(detailsShellWiring, /t: t/)
  assert.match(detailsShellWiring, /el: el/)
  assert.match(detailsShellWiring, /createCloseButton: boardModal\.createCloseButton/)
  assert.match(detailsShellWiring,
    /getActiveModal: function \(\) \{ return state\.activeModal; \}/)
  assert.match(detailsShellWiring, /requestError: boardRequestError/)

  assert.match(renderWiring, /t: t/)
  assert.match(renderWiring, /el: el/)
  assert.match(renderWiring,
    /createTextNode: function \(text\) \{ return document\.createTextNode\(text\); \}/)
  assert.match(renderWiring,
    /getSectionElement: function \(\) \{ return sectionEl; \}/)
  assert.match(renderWiring, /getState: function \(\) \{ return state; \}/)
  assert.match(renderWiring,
    /getSnapshot: function \(\) \{ return store\.get\(\); \}/)
  assert.match(renderWiring,
    /wizardComplete: function \(storeState\) \{[\s\S]*helpers\.wizardComplete\(storeState\)/)
  assert.match(renderWiring, /viewport: boardViewportState/)
  assert.match(renderWiring, /toolbar: boardToolbar/)
  assert.match(renderWiring, /health: boardHealth/)
  assert.match(renderWiring, /inbox: boardTaskInbox/)
  assert.match(renderWiring, /taskList: boardTaskListView/)

  assert.match(refreshClockWiring,
    /scheduleTimeout: function \(callback, delay\) \{ return window\.setTimeout\(callback, delay\); \}/)
  assert.match(refreshClockWiring,
    /cancelTimeout: function \(timer\) \{ window\.clearTimeout\(timer\); \}/)
  assert.match(refreshClockWiring,
    /scheduleInterval: function \(callback, delay\) \{ return window\.setInterval\(callback, delay\); \}/)
  assert.doesNotMatch(refreshClockWiring,
    /schedule(?:Timeout|Interval): (?:setTimeout|setInterval)|cancelTimeout: clearTimeout/,
    'Window timer functions must not be detached and invoked with the dependency object as their receiver')
  assert.match(refreshClockWiring,
    /isMounted: function \(\) \{ return !!sectionEl; \}/)
  assert.match(refreshClockWiring,
    /isBoardActive: function \(\) \{[\s\S]*router\.current\(\) === 'board'/)
  assert.match(refreshClockWiring,
    /getSnapshot: function \(\) \{ return store\.get\(\); \}/)
  assert.match(refreshClockWiring, /pendingRequests: workerSupport\.pendingRequests/)
  assert.match(refreshClockWiring,
    /reloadBoard: function \(options\) \{ boardLoadController\.load\(options\); \}/)
  assert.match(refreshClockWiring, /render: boardRenderController\.render/)

  assert.match(detailsBody,
    /var loading = boardTaskDetailsShell\.loading\(stem, fallbackItem\);\s*boardModal\.open\(loading\);/)
  assert.match(detailsNavigation,
    /boardTaskDetailsShell\.sectionForTarget\(target\)/)
  assert.equal((detailsBody.match(/boardTaskDetailsShell\.showLoadError\(\{/g) || []).length, 1)
  assert.match(detailsRejection,
    /^}, function \(error\) \{\s*boardTaskDetailsShell\.showLoadError\(\{\s*modalToken: modalToken,\s*loading: loading,\s*error: error,/)
  assert.match(detailsRejection,
    /onRetry: function \(\) \{\s*openTaskDetails\(stem, preferredSection, fallbackItem, deepTarget\)/)
  assert.match(detailsRejection, /getSecondaryAction: function \(\)/)
  assert.match(detailsRejection,
    /var dropAction = secondaryTaskAction\(fallbackItem, 'drop'\);\s*if \(!dropAction \|\| dropAction\.enabled === false\) return null;/)
  assert.match(detailsRejection,
    /runDropActionFlow\(stem, fallbackItem && fallbackItem\.state \|\| null, dropAction\)/)
  assert.match(detailsRejection, /\}\);\s*\}\);\s*\}\s*$/,
    'Task Details rejection shell must remain inside the loadTaskDetails promise tail')

  assert.match(storeChange,
    /if \(!boardRenderController\.isComplete\(storeState\)\) \{\s*boardRenderController\.render\(\);\s*return;/)
  assert.match(storeChange,
    /boardRefreshClock\.startClock\(\);[\s\S]*boardFinalizationController\.refreshOpen\(\);\s*boardIntegrationController\.refreshOpen\(\);\s*boardTaskInbox\.load\(\);\s*boardRefreshClock\.scheduleRefresh\(\);/)
  assert.match(mount,
    /if \(!boardRenderController\.isComplete\(storeState\)\) \{\s*boardRenderController\.render\(\);\s*return;\s*\}/)
  assert.match(mount,
    /boardLoadController\.load\(\{ closeOpenModal: true \}\);\s*boardRefreshClock\.startClock\(\);/)

  assert.doesNotMatch(board,
    /function (?:wizardComplete|renderGate|detailsLoading|detailsSectionForTarget|scheduleRefresh|tickInProgressClock)\(/)
  assert.doesNotMatch(board, /^  function render\(\)/m)
  assert.doesNotMatch(board,
    /var (?:refreshTimer|tickIntervalId)\b|\bREFRESH_DEBOUNCE_MS\b|\bTICK_MS\b/)

  for (const source of [
    boardRenderControllerSource,
    boardRefreshClockSource,
    boardTaskDetailsShellSource,
  ]) {
    assert.doesNotMatch(source, /from '\.\.\/panels\/board\.js'/)
    assert.doesNotMatch(source,
      /tasksApi|boardModal|clipboard|\bstore\.|document|window|location|history|fetch\(/)
    assert.doesNotMatch(source, /TODO|FIXME|\.only\(|\.skip\(/)
  }
})

test('board delegates navigation, confirmation, and open-card freshness to one owner each', () => {
  function wiring(startMarker, endMarker, label) {
    const start = board.indexOf(startMarker)
    const end = board.indexOf(endMarker, start)
    assert.ok(start >= 0 && end > start, label + ' wiring bounds missing')
    return board.slice(start, end)
  }

  const confirmWiring = wiring(
    'var boardConfirmDialog = createBoardConfirmDialog({',
    'var boardFigmaScreensController = createBoardFigmaScreensController({',
    'confirm dialog')
  const figmaScreensWiring = wiring(
    'var boardFigmaScreensController = createBoardFigmaScreensController({',
    'var boardTaskActionController = createBoardTaskActionController({',
    'Figma screens controller')
  const taskActionWiring = wiring(
    'var boardTaskActionController = createBoardTaskActionController({',
    'var boardTaskInbox = createBoardTaskInbox({',
    'task action controller')
  const navigationWiring = wiring(
    'var boardTaskNavigationController = createBoardTaskNavigationController({',
    'var boardTaskCardFactory = createBoardTaskCardFactory({',
    'task navigation')
  const freshnessWiring = wiring(
    'var boardOpenCardFreshness = createBoardOpenCardFreshness({',
    'var boardLoadController = createBoardLoadController({',
    'open-card freshness')
  const cardFactoryWiring = wiring(
    'var boardTaskCardFactory = createBoardTaskCardFactory({',
    'var boardPaginationController = createBoardPaginationController({',
    'card factory')
  const dropWiring = wiring(
    'function runDropActionFlow(stem, folder, typedAction, options) {',
    'function runReopenActionFlow(stem, typedAction, onCancel) {',
    'Drop flow')
  const reopenWiring = wiring(
    'function runReopenActionFlow(stem, typedAction, onCancel) {',
    '// ----------------------------------------------------------------------',
    'Reopen flow')
  const detailsNavigationWiring = wiring(
    'function navigation(action) {',
    'function execute(action, button, input) {',
    'Details navigation')
  const detailsOverflowWiring = wiring(
    'function overflow(kind, action, input) {',
    'function staleNotice() {',
    'Details overflow')
  const detailsViewWiring = wiring(
    'var view = createTaskDetails(details, {',
    'viewRef.value = view;',
    'Details view')

  assert.equal((board.match(
    /from '\.\.\/board\/board-task-navigation-controller\.js'/g) || []).length, 1)
  assert.equal((board.match(
    /from '\.\.\/board\/board-confirm-dialog\.js'/g) || []).length, 1)
  assert.equal((board.match(
    /from '\.\.\/board\/board-open-card-freshness\.js'/g) || []).length, 1)
  assert.equal((board.match(
    /from '\.\.\/board\/board-figma-screens-controller\.js'/g) || []).length, 1)
  assert.equal((board.match(
    /from '\.\.\/board\/board-task-action-controller\.js'/g) || []).length, 1)

  assert.match(confirmWiring, /t: t/)
  assert.match(confirmWiring, /el: el/)
  assert.match(confirmWiring, /openModal: boardModal\.open/)
  assert.match(confirmWiring, /closeModal: boardModal\.close/)
  assert.match(confirmWiring,
    /setCancelHandler: function \(handler\) \{ state\.onModalCancel = handler; \}/)
  assert.match(confirmWiring, /schedule: setTimeout/)

  assert.match(figmaScreensWiring, /t: t/)
  assert.match(figmaScreensWiring, /el: el/)
  assert.match(figmaScreensWiring,
    /needsUnpulledScreens: function \(stem\) \{\s*return figmaTaskReadModel\.needsUnpulledScreens\(stem\);/)
  assert.match(figmaScreensWiring,
    /isConnected: function \(\) \{ return figmaConnected\(\); \}/)
  assert.match(figmaScreensWiring,
    /return tasksApi\.figmaSessionAction\(key, action\)/)
  assert.match(figmaScreensWiring, /screenPullAction: FIGMA_SESSION_ACTION\.SCREEN_PULL/)
  assert.match(figmaScreensWiring,
    /openTerminal: function \(key\) \{ terminal\.open\(key\); \}/)
  assert.match(figmaScreensWiring, /actionError: figmaActionError/)
  assert.match(figmaScreensWiring, /openModal: boardModal\.open/)
  assert.match(figmaScreensWiring, /closeModal: boardModal\.close/)
  assert.match(figmaScreensWiring,
    /setCancelHandler: function \(handler\) \{ state\.onModalCancel = handler; \}/)
  assert.match(figmaScreensWiring, /schedule: setTimeout/)

  assert.match(taskActionWiring,
    /startupRecoveryBlocksMutation: boardReadiness\.startupRecoveryBlocksMutation/)
  assert.match(taskActionWiring,
    /needsUnpulledScreens: function \(stem\) \{\s*return figmaTaskReadModel\.needsUnpulledScreens\(stem\);/)
  assert.match(taskActionWiring,
    /confirmScreensBeforeRun: boardFigmaScreensController\.confirmBeforeRun/)
  assert.match(taskActionWiring, /confirm: boardConfirmDialog\.open/)
  assert.match(taskActionWiring,
    /return tasksApi\.executeTaskAction\(stem, action, confirmation\)/)
  assert.match(taskActionWiring, /requestError: boardRequestError/)
  assert.match(taskActionWiring,
    /reloadStore: function \(\) \{ return store\.load\(\); \}/)
  assert.match(taskActionWiring,
    /boardLoadController\.load\(\{ closeOpenModal: false \}\)/)
  assert.match(taskActionWiring,
    /boardTaskNavigationController\.openSource\(target, row\)/)
  assert.match(taskActionWiring, /openBacklogEditModal\(stem, row\)/)
  assert.match(taskActionWiring, /runDropActionFlow\(stem, folder, action, options\)/)
  assert.match(taskActionWiring, /runReopenActionFlow\(stem, action\)/)
  assert.match(taskActionWiring,
    /tasksApi\.loadTaskActionPrompt\(stem, action\)/)

  assert.match(navigationWiring,
    /getColumns: function \(\) \{ return state\.columns; \}/)
  assert.match(navigationWiring,
    /openCard: function \(folder, stem, item, section\) \{\s*openCardModal\(folder, stem, item, section\);\s*\}/)
  assert.match(navigationWiring,
    /openTerminal: function \(key\) \{ terminal\.open\(key\); \}/)
  assert.match(navigationWiring,
    /openPanel: function \(target\) \{ return router\.openTarget\(target\); \}/)
  assert.match(navigationWiring,
    /targetUnavailable: function \(\) \{\s*clipboard\.toastError\(t\('board\.action\.targetUnavailable'\)\);\s*\}/)

  assert.match(freshnessWiring, /t: t/)
  assert.match(freshnessWiring, /el: el/)
  assert.match(freshnessWiring,
    /getOpenCard: function \(\) \{ return state\.openCard; \}/)
  assert.match(freshnessWiring,
    /getColumns: function \(\) \{ return state\.columns; \}/)
  assert.match(freshnessWiring,
    /getActiveModal: function \(\) \{ return state\.activeModal; \}/)
  assert.match(freshnessWiring,
    /abortDetails: function \(\) \{\s*if \(!state\.detailsAbort\) return;\s*try \{ state\.detailsAbort\.abort\(\); \} catch \(abortError\) \{\}\s*state\.detailsAbort = null;\s*\}/)
  assert.match(freshnessWiring,
    /openDetails: function \(stem, section, item\) \{\s*openTaskDetails\(stem, section, item\);\s*\}/)
  assert.match(freshnessWiring,
    /setOpenCard: function \(openCard\) \{ state\.openCard = openCard; \}/)

  assert.equal((cardFactoryWiring.match(
    /navigate: boardTaskNavigationController\.openTarget/g) || []).length, 1)
  assert.equal((cardFactoryWiring.match(
    /execute: boardTaskActionController\.run/g) || []).length, 1)
  assert.equal((cardFactoryWiring.match(
    /action: boardTaskActionController\.overflow/g) || []).length, 1)
  assert.equal((detailsNavigationWiring.match(
    /boardTaskNavigationController\.openTarget\(target, details\)/g) || []).length, 1)
  assert.equal((detailsOverflowWiring.match(
    /boardTaskNavigationController\.openSource\(details\.origin && details\.origin\.target, details\)/g) || []).length, 1)
  assert.equal((boardTaskActionControllerSource.match(
    /dependencies\.confirm\(\{/g) || []).length, 1)
  assert.equal((dropWiring.match(/boardConfirmDialog\.open\(\{/g) || []).length, 1)
  assert.equal((reopenWiring.match(/boardConfirmDialog\.open\(\{/g) || []).length, 1)
  assert.equal((detailsViewWiring.match(
    /confirm: boardConfirmDialog\.open/g) || []).length, 1)
  assert.match(taskActionWiring,
    /boardTaskNavigationController\.openSource\(target, row\)/)
  assert.match(board,
    /boardTaskNavigationController\.openTarget\(target, details\)/)
  assert.match(board,
    /boardTaskNavigationController\.openSource\(details\.origin && details\.origin\.target, details\)/)
  assert.match(board,
    /afterLoad: function \(\) \{\s*boardOpenCardFreshness\.neutralizeIfMoved\(\)/)
  assert.equal((board.match(/boardConfirmDialog\.open\(\{/g) || []).length, 2)
  assert.match(board, /confirm: boardConfirmDialog\.open/)
  assert.doesNotMatch(board,
    /function (?:openTaskTarget|openSourceTarget|confirmModal|neutralizeOpenCardIfMoved|executeTaskCardAction|runTaskCardAction|handleCardOverflow|figmaScreensSessionKey|figmaPullReady|triggerScreensPull|confirmScreensBeforeRun)\(/)

  for (const source of [
    boardTaskNavigationControllerSource,
    boardConfirmDialogSource,
    boardOpenCardFreshnessSource,
    boardFigmaScreensControllerSource,
    boardTaskActionControllerSource,
  ]) {
    assert.doesNotMatch(source, /from '\.\.\/panels\/board\.js'/)
    assert.doesNotMatch(source,
      /tasksApi|boardModal|clipboard|\bstore\.|\bstate\.|document|window|location|history|fetch\(/)
    assert.doesNotMatch(source, /TODO|FIXME|\.only\(|\.skip\(/)
  }
})

test('board delegates Inbox and finalization recovery to one current owner each', () => {
  function wiring(startMarker, endMarker, label) {
    const start = board.indexOf(startMarker)
    const end = board.indexOf(endMarker, start)
    assert.ok(start >= 0 && end > start, label + ' wiring bounds missing')
    return board.slice(start, end)
  }

  const inboxWiring = wiring(
    'var boardTaskInbox = createBoardTaskInbox({',
    'var backlogComposer = createBacklogComposer({',
    'Inbox controller')
  const backlogWiring = wiring(
    'var backlogComposer = createBacklogComposer({',
    'var boardTaskCardFactory = createBoardTaskCardFactory({',
    'backlog composer')
  const finalizationWiring = wiring(
    'var boardFinalizationController = createBoardFinalizationController({',
    'var boardHealth = createBoardHealthController({',
    'finalization controller')
  const healthWiring = wiring(
    'var boardHealth = createBoardHealthController({',
    'boardRenderController = createBoardRenderController({',
    'Board health')
  const storeChangeWiring = wiring(
    'function onStoreChange() {',
    '// Relative activity labels',
    'Board store change')
  const mountWiring = wiring(
    'mount: function (rootEl) {',
    'refresh: function () {',
    'Board mount')

  assert.match(board,
    /import \{ createBoardFinalizationController \} from '\.\.\/board\/board-finalization-controller\.js'/)
  assert.equal((board.match(/from '\.\.\/board\/board-finalization-controller\.js'/g) || []).length, 1)
  assert.match(inboxWiring, /t: t/)
  assert.match(inboxWiring, /el: el/)
  assert.match(inboxWiring,
    /createTextNode: function \(text\) \{ return document\.createTextNode\(text\); \}/)
  assert.match(inboxWiring, /formatTimestamp: boardFormatters\.timestampLabel/)
  assert.match(inboxWiring,
    /canPublish: function \(\) \{ return boardRenderController\.isComplete\(store\.get\(\)\); \}/)
  assert.match(inboxWiring,
    /loadEntries: function \(\) \{ return tasksApi\.loadTaskInbox\(\); \}/)
  assert.match(inboxWiring,
    /publishEntry: function \(id\) \{ return tasksApi\.publishTaskInbox\(id\); \}/)
  assert.match(inboxWiring,
    /openComposer: function \(\) \{ backlogComposer\.open\(\{ inbox: true \}\); \}/)
  assert.match(inboxWiring,
    /toast: function \(message\) \{ clipboard\.toast\(message\); \}/)
  assert.match(inboxWiring, /requestError: boardRequestError/)
  assert.match(inboxWiring, /reloadStore: function \(\) \{ store\.load\(\); \}/)
  assert.match(inboxWiring,
    /getSectionElement: function \(\) \{ return sectionEl; \}/)
  assert.match(inboxWiring,
    /isBoardActive: function \(\) \{ return router\.current\(\) === 'board'; \}/)
  assert.match(inboxWiring,
    /rerender: function \(\) \{ boardRenderController\.render\(\); \}/)
  assert.match(backlogWiring, /loadTaskInbox: boardTaskInbox\.load/)

  assert.match(finalizationWiring, /t: t/)
  assert.match(finalizationWiring, /el: el/)
  assert.match(finalizationWiring,
    /getSnapshot: function \(\) \{ return store\.get\(\); \}/)
  assert.match(finalizationWiring,
    /hasActiveModal: function \(\) \{ return !!state\.activeModal; \}/)
  assert.match(finalizationWiring,
    /getOpenStem: function \(\) \{ return state\.openFinalizationStem; \}/)
  assert.match(finalizationWiring,
    /setOpenStem: function \(stem\) \{ state\.openFinalizationStem = stem; \}/)
  assert.match(finalizationWiring, /modal: boardModal/)
  assert.match(finalizationWiring,
    /resume: function \(finalization\) \{ return tasksApi\.resumeFinalization\(finalization\); \}/)
  assert.match(finalizationWiring,
    /toast: function \(message\) \{ clipboard\.toast\(message\); \}/)
  assert.match(finalizationWiring, /requestError: boardRequestError/)
  assert.match(finalizationWiring,
    /reloadStore: function \(\) \{ store\.load\(\); \}/)
  assert.match(healthWiring,
    /finalizations: boardFinalizationController\.list,\s*openFinalizationModal: boardFinalizationController\.open/)
  assert.match(storeChangeWiring,
    /boardFinalizationController\.refreshOpen\(\);\s*boardIntegrationController\.refreshOpen\(\);\s*boardTaskInbox\.load\(\);\s*boardRefreshClock\.scheduleRefresh\(\);/)
  assert.match(boardRenderControllerSource,
    /if \(!complete\) \{[\s\S]*dependencies\.inbox\.render\(false\);[\s\S]*return;[\s\S]*dependencies\.inbox\.render\(true\);/)
  assert.match(mountWiring, /var storeState = store\.get\(\);\s*boardTaskInbox\.load\(\);/)
  assert.ok(
    mountWiring.indexOf('boardTaskInbox.load()') <
      mountWiring.indexOf('if (!boardRenderController.isComplete(storeState))'),
    'mount must load Inbox before the wizard gate')
  assert.doesNotMatch(board,
    /function (?:finalizations|finalizationForStem|openFinalizationModal)\(/)

  assert.match(boardFinalizationControllerSource,
    /export function createBoardFinalizationController\(dependencies\)/)
  assert.match(boardFinalizationControllerSource, /function list\(\)/)
  assert.match(boardFinalizationControllerSource, /function refreshOpen\(\)/)
  assert.match(boardFinalizationControllerSource,
    /dependencies\.modal\.open\(content\);[\s\S]*dependencies\.setOpenStem\(finalization\.stem \|\| null\)/)
  assert.doesNotMatch(boardFinalizationControllerSource, /from '\.\.\/panels\/board\.js'/)
  assert.doesNotMatch(boardFinalizationControllerSource,
    /tasksApi|boardModal|clipboard|\bstore\.|\bstate\.|document|window|location|history|fetch\(/)
  for (const source of [boardTaskInboxSource, boardFinalizationControllerSource]) {
    assert.doesNotMatch(source, /TODO|FIXME|\.only\(|\.skip\(/)
  }
})

test('board delegates load lifecycle and task targeting to one current controller each', () => {
  function wiring(startMarker, endMarker, label) {
    const start = board.indexOf(startMarker)
    const end = board.indexOf(endMarker, start)
    assert.ok(start >= 0 && end > start, label + ' wiring bounds missing')
    return board.slice(start, end)
  }

  const paginationWiring = wiring(
    'var boardPaginationController = createBoardPaginationController({',
    'var boardLoadResults = createBoardLoadResults({',
    'pagination controller')
  const loadResultsWiring = wiring(
    'var boardLoadResults = createBoardLoadResults({',
    'var boardTaskTargetController = createBoardTaskTargetController({',
    'load-results adapter')
  const targetWiring = wiring(
    'var boardTaskTargetController = createBoardTaskTargetController({',
    'var boardLoadController = createBoardLoadController({',
    'task-target controller')
  const loadWiring = wiring(
    'var boardLoadController = createBoardLoadController({',
    'var boardTaskListView = createBoardTaskListView({',
    'load controller')
  const mountMethod = wiring(
    'mount: function (rootEl) {',
    'refresh: function () {',
    'Board mount')
  const refreshMethod = wiring(
    'refresh: function () {',
    'openTask: function (stem) {',
    'Board refresh')

  assert.match(board,
    /import \{ createBoardLoadController \} from '\.\.\/board\/board-load-controller\.js'/)
  assert.match(board,
    /import \{ createBoardTaskTargetController \} from '\.\.\/board\/board-task-target-controller\.js'/)
  assert.equal((board.match(/from '\.\.\/board\/board-load-controller\.js'/g) || []).length, 1)
  assert.equal((board.match(/from '\.\.\/board\/board-task-target-controller\.js'/g) || []).length, 1)
  assert.match(paginationWiring,
    /getLoadGeneration: function \(\) \{ return boardLoadController\.generation\(\); \}/)
  assert.match(paginationWiring,
    /render: function \(\) \{ boardRenderController\.render\(\); \}/)
  assert.match(loadResultsWiring,
    /loadSummary: function \(filters\) \{ return tasksApi\.loadTaskSummary\(filters\); \}/)
  assert.match(loadResultsWiring,
    /loadIntegrity: function \(\) \{ return tasksApi\.loadTaskIntegrity\(\); \}/)
  assert.match(targetWiring, /getColumns: function \(\) \{ return state\.columns; \}/)
  assert.match(targetWiring, /getCurrentPanel: function \(\) \{ return router\.current\(\); \}/)
  assert.match(targetWiring, /getHash: function \(\) \{ return location\.hash; \}/)
  assert.match(targetWiring,
    /replaceHash: function \(hash\) \{ history\.replaceState\(null, '', hash\); \}/)
  assert.match(targetWiring,
    /openTask: function \(folder, stem, item, section, target\) \{\s*openCardModal\(folder, stem, item, section, target\);\s*\}/)
  assert.match(loadWiring, /getState: function \(\) \{ return state; \}/)
  assert.match(loadWiring, /isMounted: function \(\) \{ return !!sectionEl; \}/)
  assert.match(loadWiring, /closeModal: function \(\) \{ boardModal\.close\(\); \}/)
  assert.match(loadWiring, /getFilters: function \(\) \{ return taskListStore\.filters\(\); \}/)
  assert.match(loadWiring, /loadResults: boardLoadResults\.load/)
  assert.match(loadWiring,
    /render: function \(\) \{ boardRenderController\.render\(\); \}/)
  assert.match(loadWiring,
    /reconcileMenus: function \(stems\) \{ taskListStore\.reconcileMenus\(stems\); \}/)
  assert.match(loadWiring, /requestError: boardRequestError/)
  assert.match(loadWiring,
    /afterLoad: function \(\) \{\s*boardOpenCardFreshness\.neutralizeIfMoved\(\);\s*boardTaskTargetController\.openRequested\(true\);\s*\}/)
  assert.match(mountMethod,
    /sectionEl = rootEl;\s*boardTaskTargetController\.consumeDeepLink\(\)/)
  assert.ok(
    mountMethod.indexOf('boardTaskTargetController.consumeDeepLink()') <
      mountMethod.indexOf('if (!boardRenderController.isComplete(storeState))'),
    'mount must consume the deep link before the wizard gate')
  assert.match(mountMethod,
    /if \(!boardRenderController\.isComplete\(storeState\)\) \{[\s\S]*?return;\s*\}\s*boardLoadController\.load\(\{ closeOpenModal: true \}\)/)
  assert.match(refreshMethod,
    /boardRenderController\.render\(\);\s*boardTaskTargetController\.consumeDeepLink\(\);\s*boardTaskTargetController\.openRequested\(false\);/)
  assert.match(board, /boardLoadController\.load\(\{ closeOpenModal: false \}\)/)
  assert.match(board, /boardTaskTargetController\.request\(stem, 'overview', null\)/)
  assert.doesNotMatch(board, /function (?:loadAndRender|unavailableIntegrity|openRequestedTask|consumeTaskDeepLink)\(/)
  assert.doesNotMatch(board, /\bloadGen\b|pendingOpen(?:Stem|Section|Target)/)

  assert.match(boardLoadControllerSource,
    /export function createBoardLoadController\(dependencies\)/)
  assert.match(boardLoadControllerSource,
    /import \{ TASK_COLUMNS \} from '\.\/task-summary-projection\.js'/)
  assert.match(boardTaskTargetControllerSource,
    /export function createBoardTaskTargetController\(dependencies\)/)
  assert.match(boardTaskTargetControllerSource,
    /import \{ findTaskInColumns \} from '\.\/task-summary-projection\.js'/)
  for (const source of [boardLoadControllerSource, boardTaskTargetControllerSource]) {
    assert.doesNotMatch(source, /from '\.\.\/panels\/board\.js'/)
    assert.doesNotMatch(source,
      /tasksApi|taskListStore|boardModal|clipboard|\bstore\.|document|window|location|history|fetch\(/)
  }
})

test('board viewport state preserves focus, selection, column scroll, and page scroll', () => {
  let activeElement = null
  const documentNode = {}
  Object.defineProperty(documentNode, 'activeElement', {
    get() { return activeElement },
  })

  const queried = []
  const section = {
    contains(node) { return node === activeElement },
    querySelectorAll(selector) {
      if (selector === '.board-column__body') return oldBodies
      if (selector === '.board-column') return newColumns
      return []
    },
    querySelector(selector) {
      queried.push(selector)
      return queryResults.get(selector) || null
    },
  }
  const viewport = {
    pageXOffset: 11,
    pageYOffset: 23,
    scrollCalls: [],
    scrollTo(x, y) { this.scrollCalls.push([x, y]) },
  }
  const oldBodies = [
    { scrollTop: 0, parentNode: { getAttribute: () => 'backlog' } },
    { scrollTop: 17, parentNode: { getAttribute: () => 'pending' } },
    { scrollTop: 31, parentNode: { getAttribute: () => 'done' } },
  ]
  const restoredBodies = {
    pending: { scrollTop: 0 },
    done: { scrollTop: 0 },
  }
  const newColumns = Object.keys(restoredBodies).map(folder => ({
    getAttribute(name) { return name === 'data-folder' ? folder : null },
    querySelector(selector) {
      return selector === '.board-column__body' ? restoredBodies[folder] : null
    },
  }))
  const queryResults = new Map()
  const viewportState = createBoardViewportState({
    getDocumentNode: () => documentNode,
    getViewport: () => viewport,
    getSectionElement: () => section,
  })

  activeElement = {
    selectionStart: 2,
    selectionEnd: 5,
    getAttribute(name) {
      if (name === 'data-board-filter') return null
      if (name === 'data-task-control') return 'title'
      return null
    },
    closest(selector) {
      assert.equal(selector, '.board-card[data-stem]')
      return { getAttribute: name => name === 'data-stem' ? 'TASK_7_Demo' : null }
    },
  }
  const focusSnapshot = viewportState.captureFocus()
  assert.deepEqual(focusSnapshot, {
    filter: null,
    stem: 'TASK_7_Demo',
    control: 'title',
    start: 2,
    end: 5,
  })

  let focusCalls = 0
  const selectionCalls = []
  const replacement = {
    focus() { focusCalls += 1 },
    setSelectionRange(start, end) { selectionCalls.push([start, end]) },
  }
  const controlSelector = '.board-card[data-stem="TASK_7_Demo"] ' +
    '[data-task-control="title"]:not([disabled])'
  queryResults.set(controlSelector, replacement)
  activeElement = { unrelated: true }
  assert.equal(viewportState.restoreFocus(focusSnapshot), undefined)
  assert.equal(focusCalls, 1)
  assert.deepEqual(selectionCalls, [[2, 5]])

  activeElement = replacement
  viewportState.restoreFocus(focusSnapshot)
  assert.equal(focusCalls, 1, 'an already-focused retained control must not be refocused')

  const overflowSnapshot = {
    filter: null,
    stem: 'TASK_8_Menu',
    control: 'overflow-copy',
    start: null,
    end: null,
  }
  let triggerClicks = 0
  let triggerFocuses = 0
  const trigger = {
    getAttribute: name => name === 'aria-expanded' ? 'true' : null,
    click() { triggerClicks += 1 },
    focus() { triggerFocuses += 1 },
  }
  queryResults.set(
    '.board-card[data-stem="TASK_8_Menu"] [data-task-control="overflow"]', trigger)
  activeElement = { unrelated: true }
  viewportState.restoreFocus(overflowSnapshot)
  assert.equal(triggerClicks, 1)
  assert.equal(triggerFocuses, 1)

  const scrollSnapshot = viewportState.captureScroll()
  assert.deepEqual(scrollSnapshot, {
    columns: { pending: 17, done: 31 },
    pageX: 11,
    pageY: 23,
  })
  viewport.pageXOffset = 0
  viewport.pageYOffset = 4
  assert.equal(viewportState.restoreScroll(scrollSnapshot), undefined)
  assert.deepEqual(restoredBodies, {
    pending: { scrollTop: 17 },
    done: { scrollTop: 31 },
  })
  assert.deepEqual(viewport.scrollCalls, [[11, 23]])
})

test('board toolbar preserves filters, debounce, retention, and locale rebuild behavior', () => {
  function makeElement(tag, attrs) {
    const node = fakeElement(tag, attrs)
    const append = node.appendChild
    const remove = node.removeChild
    node.appendChild = function (child) {
      if (child.parentNode && child.parentNode !== this) child.parentNode.removeChild(child)
      child.parentNode = this
      return append.call(this, child)
    }
    node.removeChild = function (child) {
      const removed = remove.call(this, child)
      if (removed && removed.parentNode === this) removed.parentNode = null
      return removed
    }
    return node
  }

  const section = makeElement('section')
  const listStore = createTaskListStore()
  const scheduled = new Map()
  const cancelled = []
  const refreshes = []
  let nextTimer = 0
  let language = 'en'
  const toolbar = createBoardToolbar({
    t: key => key,
    el: makeElement,
    getLanguage: () => language,
    taskListStore: listStore,
    getSectionElement: () => section,
    schedule(callback, delay) {
      const id = ++nextTimer
      scheduled.set(id, { callback, delay })
      return id
    },
    cancelSchedule(id) {
      cancelled.push(id)
      scheduled.delete(id)
    },
    refresh: () => refreshes.push(listStore.filters()),
  })

  assert.equal(toolbar.render(), undefined,
    'render keeps the original command-style contract')
  const first = toolbar.element()
  assert.equal(first.className, 'board-toolbar')
  assert.strictEqual(first.parentNode, section)
  assert.strictEqual(toolbar.preservedElement(), first)
  assert.deepEqual(
    fakeTree(first)
      .filter(node => node.tag === 'select')
      .map(node => node.attributes['data-board-filter']),
    ['column', 'origin', 'blocker', 'dependency', 'context', 'sort'])

  const column = fakeTree(first).find(node =>
    node.attributes['data-board-filter'] === 'column')
  assert.deepEqual(column.children.map(option => [option.value, option.textContent]), [
    ['', 'board.filter.allStates'],
    ['backlog', 'board.column.backlog'],
    ['pending', 'board.column.pending'],
    ['todo', 'board.column.todo'],
    ['done', 'board.column.done'],
  ])

  const search = fakeTree(first).find(node =>
    node.attributes['data-board-filter'] === 'search')
  search.value = 'alpha'
  search.dispatch('input')
  assert.equal(scheduled.get(1).delay, 220)
  search.value = 'beta'
  search.dispatch('input')
  assert.deepEqual(cancelled, [1])
  assert.equal(scheduled.get(2).delay, 220)
  scheduled.get(2).callback()
  assert.equal(refreshes.length, 1)
  assert.equal(refreshes[0].search, 'beta')

  column.value = 'todo'
  column.dispatch('change')
  assert.equal(scheduled.get(3).delay, 0)
  scheduled.get(3).callback()
  assert.equal(refreshes[1].column, 'todo')
  column.dispatch('change')
  assert.equal(nextTimer, 3, 'unchanged filters must not schedule another refresh')

  assert.equal(toolbar.render(), undefined)
  assert.strictEqual(toolbar.element(), first)
  assert.equal(section.children.filter(child => child === first).length, 1)
  section.removeChild(first)
  assert.equal(toolbar.render(), undefined)
  assert.strictEqual(toolbar.element(), first)
  assert.strictEqual(first.parentNode, section)

  language = 'ru'
  assert.equal(toolbar.preservedElement(), null)
  section.removeChild(first)
  assert.equal(toolbar.render(), undefined)
  assert.notStrictEqual(toolbar.element(), first)
  assert.strictEqual(toolbar.preservedElement(), toolbar.element())
})

test('board delegates backlog composition through one current module and keeps mutation fences', () => {
  assert.match(board,
    /import \{ createBacklogComposer \} from '\.\.\/board\/backlog-composer\.js'/)
  assert.match(board, /var backlogComposer = createBacklogComposer\(\{/)
  assert.equal((board.match(/backlogComposer\.open\(/g) || []).length, 2)
  assert.doesNotMatch(board,
    /function (?:assembleBacklogBody|composerField|composerSection|composerBulletList|composerDesignList|openCreateBacklogModal)\(/)

  assert.match(backlogComposer,
    /export function createBacklogComposer\(dependencies\)/)
  assert.match(backlogComposer,
    /if \(!inboxMode && globalMutationBlocked\(\)\)/)
  assert.match(backlogComposer,
    /var createKey = tasksApi\.creationKey\(inboxMode \? 'setup-inbox' : 'board-create'\)/)
  assert.match(backlogComposer,
    /tasksApi\.saveTaskInbox\(titleInput\.value, currentBody\(\), \{ idempotencyKey: createKey \}\)/)
  assert.match(backlogComposer,
    /createBacklogWithIntegrityFence\(titleInput\.value, currentBody\(\), \{ idempotencyKey: createKey \}\)/)
  assert.match(backlogComposer, /if \(handleTaskMutationConflict\(err, \{ closeModal: false \}\)\) \{/)
  assert.match(backlogComposer, /actions\.appendChild\(createCloseButton\(\)\)/)
  assert.doesNotMatch(backlogComposer, /from '\.\.\/panels\/board\.js'/)
})

test('board delegates one modal lifecycle while preserving dismissal, cleanup, and focus safeguards', () => {
  assert.match(board,
    /import \{ createBoardModalController \} from '\.\.\/board\/modal-controller\.js'/)
  assert.match(board, /var boardModal = createBoardModalController\(\{/)
  assert.doesNotMatch(board,
    /function (?:openModal|closeModal|forceCloseModal|restoreFocusAfterClose|closeButton)\(/)
  assert.match(modalController, /export function createBoardModalController\(dependencies\)/)
  assert.match(modalController, /var pressBeganOnOverlay = false/)
  assert.match(modalController,
    /overlay\.addEventListener\('mousedown',[\s\S]*pressBeganOnOverlay = \(e\.target === overlay\)/)
  assert.match(modalController,
    /if \(e\.target === overlay && pressBeganOnOverlay\) close\(\)/)
  assert.match(modalController, /if \(e\.key === 'Escape' \|\| e\.keyCode === 27\)/)
  assert.match(modalController, /if \(e\.key === 'Tab' \|\| e\.keyCode === 9\)/)
  assert.match(modalController, /querySelectorAll\('\[data-board-cleanup\]'\)/)
  assert.match(modalController, /typeof cleanupNodes\[i\]\.__boardCleanup === 'function'/)
  assert.match(modalController, /state\.detailsAbort\.abort\(\)/)
  assert.match(modalController,
    /var onCancel = state\.onModalCancel;\s*state\.onModalCancel = null;\s*if \(typeof onCancel === 'function'\)/)
  assert.match(modalController,
    /function forceClose\(\) \{\s*state\.onModalCancel = null;\s*close\(\)/)
  assert.match(modalController, /prior && document\.contains\(prior\)/)
  assert.match(modalController, /sectionEl\.querySelector\('\.board-card\[data-stem="/)
  assert.match(modalController, /class: 'btn board-modal__close-btn'/)
  assert.match(modalController,
    /return \{\s*open: open,\s*close: close,\s*forceClose: forceClose,\s*createCloseButton: createCloseButton\s*\}/)
  assert.doesNotMatch(modalController, /from '\.\.\/panels\/board\.js'/)
})

test('board delegates one worker-support policy while preserving the current recovery help', () => {
  assert.match(board,
    /import \{ createBoardWorkerSupport \} from '\.\.\/board\/worker-support\.js'/)
  assert.match(board, /var workerSupport = createBoardWorkerSupport\(\{/)
  assert.doesNotMatch(board,
    /function (?:shellQuote|pendingRequests|workerOnlineOrBusy|cliCannotAuth|drainerAttached|workerLooksOffline|buildWorkerHelpContent|openWorkerHelpModal)\(/)
  assert.match(workerSupportSource, /var WORKER_ONLINE_MS = 90 \* 1000/)
  assert.match(workerSupportSource, /var WORKER_SKEW_MS = 10 \* 1000/)
  assert.match(workerSupportSource,
    /function cliCannotAuth\(cli\) \{\s*return !!cli && \(cli\.loggedIn === false \|\| !!cli\.authProblem\)/)
  assert.match(workerSupportSource,
    /if \(storeState && storeState\.startupRecovery && storeState\.startupRecovery\.status !== 'ready'\) return false/)
  assert.match(workerSupportSource,
    /if \(!pendingRequests\(storeState\)\.length\) return false;\s*return !workerOnlineOrBusy\(storeState\)/)
  assert.match(workerSupportSource,
    /Date\.now\(\) - clampNow\(lockMs\.getTime\(\)\) < staleLockMs/)
  assert.match(workerSupportSource, /age > -WORKER_SKEW_MS && age < WORKER_ONLINE_MS/)
  assert.match(workerSupportSource,
    /storeState && storeState\.runnerActive &&\s*!cliCannotAuth\(storeState\.cli\)/)
  assert.match(workerSupportSource, /function shellQuote\(s\)/)
  assert.match(workerSupportSource,
    /step\('board\.workerOffline\.step1', shellQuote\(projectRoot\)\)/)
  assert.match(workerSupportSource, /step\('board\.workerOffline\.step2', null\)/)
  assert.match(workerSupportSource, /step\('board\.workerOffline\.step3', '\/loop \/serve-queue'\)/)
  assert.match(workerSupportSource, /copyButton\(t\('board\.workerOffline\.copy'\)/)
  assert.match(workerSupportSource, /cli\.authProblem \? 'board\.workerOffline\.cliAuthDead'/)
  assert.match(workerSupportSource, /done\.addEventListener\('click', boardModal\.close\)/)
  assert.doesNotMatch(workerSupportSource, /from '\.\.\/panels\/board\.js'/)
})

test('board delegates one health controller while preserving every signal and mounted status invariant', () => {
  assert.match(board,
    /import \{ createBoardHealthController \} from '\.\.\/board\/board-health\.js'/)
  assert.equal((board.match(/createBoardHealthController\(\{/g) || []).length, 1)
  assert.match(board, /var boardHealth = createBoardHealthController\(\{/)
  assert.doesNotMatch(board,
    /function (?:integritySeverityLabel|boardHealthIssues|boardHealthSeverity|boardHealthCopyText|openBoardHealthModal|renderBoardStatus)\(/)
  assert.doesNotMatch(board,
    /var (?:healthEl|healthButtonEl|healthSeverityEl|healthSummaryEl|healthAnnouncementEl|healthAnnouncementSignature|healthIssues)\b/)
  assert.match(boardHealthSource, /export function createBoardHealthController\(dependencies\)/)
  assert.match(boardHealthSource, /function boardHealthIssues\(storeState\)/)
  assert.match(boardHealthSource,
    /integrity\.truncated === true \|\| integrity\.runtimeStatusTruncated === true/)
  assert.match(boardHealthSource, /code: 'TASK_DIAGNOSTICS_TRUNCATED'/)
  assert.match(boardHealthSource,
    /if \(state\.error\) issues\.unshift\(\{[\s\S]*code: 'BOARD_SUMMARY_UNAVAILABLE'/)
  assert.match(boardHealthSource,
    /state\.summary && Array\.isArray\(state\.summary\.limitations\)/)
  assert.match(boardHealthSource,
    /if \(code === 'task-integrity' && issues\.some\(function \(item\)/)
  assert.match(boardHealthSource, /var startup = startupRecoveryState\(\)/)
  assert.match(boardHealthSource, /finalizations\(\)\.forEach\(function \(fin\)/)
  assert.match(boardHealthSource,
    /Array\.isArray\(storeState\.progress\.publicationRecoveryIssues\)/)
  assert.match(boardHealthSource, /workerSupport\.workerLooksOffline\(storeState\)/)
  assert.match(boardHealthSource,
    /function boardHealthSeverity\(issues\) \{[\s\S]*item\.severity === 'error'[\s\S]*return 'warning'[\s\S]*return 'ok'/)
  assert.match(boardHealthSource,
    /function boardHealthCopyText\(issues, capturedAt\) \{[\s\S]*diagnosticsTruncated: item\.diagnosticsTruncated === true/)
  assert.match(boardHealthSource,
    /openFinalizationModal\(item\.finalization\)/)
  assert.match(boardHealthSource, /help\.addEventListener\('click', workerSupport\.openHelp\)/)
  assert.match(boardHealthSource, /clipboard\.copy\(boardHealthCopyText\(issues, capturedAt\)\)/)
  assert.match(boardHealthSource,
    /attrs: \{ role: 'status', 'aria-live': 'polite', 'aria-atomic': 'true' \}/)
  assert.match(boardHealthSource,
    /if \(announcement !== healthAnnouncementSignature\) \{[\s\S]*healthAnnouncementEl\.textContent/)
  assert.match(boardHealthSource, /var sectionEl = getSectionElement\(\)/)
  assert.match(boardHealthSource, /var toolbarEl = getToolbarElement\(\)/)
  assert.match(boardRenderControllerSource,
    /var healthElement = dependencies\.health\.element\(\);\s*var preserveHealth = healthElement && healthElement\.parentNode === section/)
  assert.match(boardRenderControllerSource,
    /\(!preserveHealth \|\| child !== healthElement\)/)
  assert.equal((boardRenderControllerSource.match(
    /dependencies\.health\.render\(storeState\)/g) || []).length, 2)
  assert.doesNotMatch(boardHealthSource, /from '\.\.\/panels\/board\.js'/)
})

test('board-health evidence keeps ordering, severity, deduplication, and copy shape stable', () => {
  const healthState = {
    error: { kind: 'summary-broken' },
    summary: { limitations: ['task-integrity', 'runtime-partial'] },
  }
  const finalization = {
    errorCode: 'FINALIZE',
    status: 'recoverable',
    recoverable: true,
    stem: 'TASK_3_finalize',
  }
  const controller = createBoardHealthController({
    t: (key) => key,
    state: healthState,
    taskIntegrity: () => ({
      findings: [
        {
          code: 'WARN',
          severity: 'warning',
          stem: 'TASK_1_warn',
          message: 'warning message',
          recovery: 'retry',
          paths: ['tasks/TASK_1_warn.md'],
          pathsTruncated: true,
        },
        { code: 'BLOCK', severity: 'blocker', message: 'blocked' },
      ],
      truncated: true,
    }),
    startupRecoveryState: () => ({ status: 'blocked', reasonCode: 'runner-start-failed' }),
    startupRecoveryReason: (code) => `reason:${code}`,
    boardLoadErrorText: (error) => `load:${error.kind}`,
    finalizations: () => [finalization],
    workerSupport: { workerLooksOffline: () => true },
  })
  const issues = controller.issuesFor({
    progress: {
      publicationRecoveryIssues: [
        { code: 'CREATION_INCOMPLETE', stem: 'TASK_4_safe' },
        { code: 'UNSAFE', stem: 'TASK_5_unsafe' },
      ],
    },
  })

  assert.deepEqual(issues.map((issue) => issue.code), [
    'BOARD_SUMMARY_UNAVAILABLE',
    'WARN',
    'BLOCK',
    'TASK_DIAGNOSTICS_TRUNCATED',
    'RUNTIME_PARTIAL',
    'runner-start-failed',
    'FINALIZE',
    'CREATION_INCOMPLETE',
    'UNSAFE',
    'TASK_RUNNER_OFFLINE',
  ])
  assert.equal(issues.filter((issue) => issue.kind === 'integrity').length, 2)
  assert.equal(issues.filter((issue) => issue.code === 'TASK_INTEGRITY').length, 0)
  assert.equal(issues.find((issue) => issue.code === 'CREATION_INCOMPLETE').severity, 'warning')
  assert.equal(issues.find((issue) => issue.code === 'UNSAFE').severity, 'error')
  assert.equal(issues.find((issue) => issue.code === 'FINALIZE').finalization, finalization)
  assert.equal(controller.severityFor([]), 'ok')
  assert.equal(controller.severityFor([{ severity: 'warning' }]), 'warning')
  assert.equal(controller.severityFor(issues), 'error')

  assert.deepEqual(JSON.parse(controller.copyTextFor([issues[1]], '2026-07-31T00:00:00.000Z')), {
    capturedAt: '2026-07-31T00:00:00.000Z',
    status: 'warning',
    issues: [{
      code: 'WARN',
      severity: 'warning',
      stem: 'TASK_1_warn',
      message: 'warning message',
      recovery: 'retry',
      paths: ['tasks/TASK_1_warn.md'],
      pathsTruncated: true,
      diagnosticsTruncated: false,
    }],
  })
})

test('board delegates one visual-evidence trust boundary without a panel-local copy', () => {
  assert.match(board,
    /from '\.\.\/board\/visual-evidence-trust\.js'/)
  assert.equal((board.match(/from '\.\.\/board\/visual-evidence-trust\.js'/g) || []).length, 1)
  assert.doesNotMatch(board,
    /function (?:isValidReportHash|artifactSetReportHashOk|reportRunIds|evidenceTrustState|finalVisualTrustState|finalVisualDisplayState)\(/)
  assert.doesNotMatch(board,
    /SHA256_REPORT_RE|resp\.schemaVersion !== 1|ids\.length > 1|opts\.allowMissingRequired/)
  assert.match(visualEvidenceTrustSource,
    /export function evidenceTrustState\(resp, opts\)/)
  assert.match(visualEvidenceTrustSource,
    /export function finalVisualDisplayState\(resp\)/)
  assert.doesNotMatch(visualEvidenceTrustSource, /from '\.\.\/panels\/board\.js'/)
})

test('board delegates localized plural and time formatting without retaining panel-local copies', () => {
  assert.match(board,
    /import \{ createBoardFormatters \} from '\.\.\/board\/board-formatters\.js'/)
  assert.equal((board.match(/from '\.\.\/board\/board-formatters\.js'/g) || []).length, 1)
  assert.match(board, /var boardFormatters = createBoardFormatters\(\{/)
  assert.doesNotMatch(board,
    /function (?:pluralCategory|pluralLabel|pluralTemplate|parseIso|clampNow|relativeTime|timestampLabel)\(/)
  for (const method of [
    'pluralLabel', 'pluralTemplate', 'parseIso', 'clampNow', 'relativeTime', 'timestampLabel',
  ]) assert.match(board, new RegExp(`boardFormatters\\.${method}`), method)

  assert.match(boardFormattersSource,
    /export function createBoardFormatters\(dependencies\)/)
  assert.doesNotMatch(boardFormattersSource, /from '\.\.\/panels\/board\.js'/)
  assert.doesNotMatch(boardFormattersSource,
    /i18n|tasksApi|requestJson|runControl|boardModal|clipboard|store|state\.|document|window|fetch\(/)
})

test('board formatters preserve plural rules, time thresholds, skew clamping, and locale labels', () => {
  var language = 'ru'
  var nowMs = Date.parse('2026-08-01T12:00:00.000Z')
  var translationCalls = []
  var formatters = createBoardFormatters({
    t: (key, params) => {
      translationCalls.push({ key, params })
      return key
    },
    getLanguage: () => language,
    now: () => nowMs,
  })

  for (const [count, category] of [
    [1, 'one'], [2, 'few'], [5, 'many'], [11, 'many'], [14, 'many'], [21, 'one'], [22, 'few'],
  ]) {
    assert.equal(formatters.pluralLabel('questions', count), `questions.${category}`)
    assert.deepEqual(translationCalls.at(-1), {
      key: `questions.${category}`,
      params: { n: count },
    })
  }
  language = 'en'
  assert.equal(formatters.pluralLabel('questions', 1), 'questions.one')
  assert.equal(formatters.pluralLabel('questions', 2), 'questions.other')
  language = 'uk'
  assert.equal(formatters.pluralLabel('questions', 2), 'questions.other')
  assert.equal(formatters.pluralTemplate('summary', 2, { shown: 1, n: 99 }), 'summary.other')
  assert.deepEqual(translationCalls.at(-1), {
    key: 'summary.other',
    params: { n: 99, shown: 1 },
  })

  assert.equal(formatters.parseIso(null), null)
  assert.equal(formatters.parseIso('not-an-iso-date'), null)
  assert.equal(formatters.parseIso('2026-08-01T11:59:30.000Z').toISOString(),
    '2026-08-01T11:59:30.000Z')
  assert.equal(formatters.clampNow(nowMs - 1), nowMs - 1)
  assert.equal(formatters.clampNow(nowMs + 1), nowMs)

  function ago(milliseconds) {
    return new Date(nowMs - milliseconds).toISOString()
  }
  assert.equal(formatters.relativeTime('invalid'), 'board.timeUnknown')
  assert.equal(formatters.relativeTime(new Date(nowMs + 60_000).toISOString()), 'time.justNow')
  assert.equal(formatters.relativeTime(ago(29_000)), 'time.justNow')
  assert.equal(formatters.relativeTime(ago(30_000)), 'time.secondsAgo')
  assert.deepEqual(translationCalls.at(-1), { key: 'time.secondsAgo', params: { s: 30 } })
  assert.equal(formatters.relativeTime(ago(90_000)), 'time.minutesAgo')
  assert.deepEqual(translationCalls.at(-1), { key: 'time.minutesAgo', params: { m: 2 } })
  assert.equal(formatters.relativeTime(ago(90 * 60_000)), 'time.hoursAgo')
  assert.deepEqual(translationCalls.at(-1), { key: 'time.hoursAgo', params: { h: 2 } })
  assert.equal(formatters.relativeTime(ago(30 * 60 * 60_000)), 'time.daysAgo')
  assert.deepEqual(translationCalls.at(-1), { key: 'time.daysAgo', params: { d: 1 } })

  var localeCalls = []
  var originalToLocaleString = Date.prototype.toLocaleString
  Date.prototype.toLocaleString = function (locale, options) {
    localeCalls.push({ locale, options })
    if (locale === 'en-US') throw new Error('formatting unavailable')
    return locale + ':absolute'
  }
  try {
    language = 'ru'
    assert.equal(formatters.timestampLabel(new Date(nowMs).toISOString()),
      'time.justNow · ru-RU:absolute')
    assert.deepEqual(localeCalls.at(-1), {
      locale: 'ru-RU',
      options: { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' },
    })
    language = 'uk'
    assert.equal(formatters.timestampLabel(new Date(nowMs).toISOString()),
      'time.justNow · uk-UA:absolute')
    language = 'en'
    assert.equal(formatters.timestampLabel(new Date(nowMs).toISOString()),
      'time.justNow · 2026-08-01T12:00:00.000Z')
    assert.equal(formatters.timestampLabel('invalid'), 'invalid')
    assert.equal(formatters.timestampLabel(null), '')
  } finally {
    Date.prototype.toLocaleString = originalToLocaleString
  }
})

test('board delegates pure Task Summary lookup, pagination merge, and loaded counts', () => {
  assert.match(board,
    /from '\.\.\/board\/task-summary-projection\.js'/)
  assert.equal((board.match(/from '\.\.\/board\/task-summary-projection\.js'/g) || []).length, 1)
  assert.doesNotMatch(board, /function (?:indexedTask|mergeSummaryPage)\(/)
  assert.match(boardTaskNavigationControllerSource,
    /findTaskInColumns\(dependencies\.getColumns\(\), target\.stem\)/)
  assert.match(boardOpenCardFreshnessSource,
    /findTaskInColumns\(columns, openCard\.stem\)/)
  assert.match(boardTaskTargetControllerSource,
    /findTaskInColumns\(columns, requested\.stem\)/)
  assert.match(board,
    /return mergeTaskSummaryPage\(current, page\)/)
  assert.match(boardPaginationControllerSource,
    /dependencies\.mergeSummaryPage\(state\.summary, page\)/)
  assert.match(boardTaskListViewSource,
    /import \{ TASK_COLUMNS, countLoadedTasks \} from '\.\/task-summary-projection\.js'/)
  assert.match(boardTaskListViewSource,
    /countLoadedTasks\(state\.summary\.columns\)/)

  assert.match(taskSummaryProjectionSource,
    /export function findTaskInColumns\(columns, stem\)/)
  assert.match(taskSummaryProjectionSource,
    /export function mergeTaskSummaryPage\(current, page\)/)
  assert.match(taskSummaryProjectionSource,
    /export function countLoadedTasks\(columns\)/)
  assert.doesNotMatch(taskSummaryProjectionSource, /from '\.\.\/panels\/board\.js'/)
  assert.doesNotMatch(taskSummaryProjectionSource,
    /tasksApi|requestJson|runControl|boardModal|clipboard|store|state\.|document|window|fetch\(|Date\./)
})

test('Task Summary projection preserves canonical order, page metadata, and input immutability', () => {
  assert.deepEqual(TASK_COLUMNS, ['backlog', 'pending', 'todo', 'done'])
  assert.equal(Object.isFrozen(TASK_COLUMNS), true)
  const backlog = { stem: 'TASK_1_same', title: 'backlog' }
  const pending = { stem: 'TASK_2_pending', title: 'pending' }
  const doneDuplicate = { stem: 'TASK_1_same', title: 'done' }
  const columns = {
    backlog: [backlog],
    pending: [pending],
    todo: 'not-an-array',
    done: [doneDuplicate],
  }

  assert.equal(findTaskInColumns(null, 'TASK_1_same'), null)
  assert.equal(findTaskInColumns(columns, 'TASK_missing'), null)
  const foundBacklog = findTaskInColumns(columns, 'TASK_1_same')
  assert.deepEqual(foundBacklog, {
    folder: 'backlog',
    item: backlog,
  })
  assert.equal(foundBacklog.item, backlog)
  assert.deepEqual(findTaskInColumns(columns, 'TASK_2_pending'), {
    folder: 'pending',
    item: pending,
  })

  const current = {
    revision: 'rev-1',
    total: 5,
    nextCursor: 'cursor-1',
    columns: {
      backlog: [{ stem: 'TASK_1' }],
      pending: [],
      todo: [{ stem: 'TASK_2' }],
      done: [],
    },
  }
  const page = {
    revision: 'rev-1',
    total: 5,
    nextCursor: null,
    partial: true,
    columns: {
      backlog: [{ stem: 'TASK_3' }],
      pending: [{ stem: 'TASK_4' }],
      done: [{ stem: 'TASK_5' }],
    },
  }
  const currentBefore = structuredClone(current)
  const pageBefore = structuredClone(page)
  const merged = mergeTaskSummaryPage(current, page)

  assert.deepEqual(current, currentBefore)
  assert.deepEqual(page, pageBefore)
  assert.notEqual(merged, page)
  assert.notEqual(merged.columns.backlog, current.columns.backlog)
  assert.notEqual(merged.columns.backlog, page.columns.backlog)
  assert.equal(merged.revision, 'rev-1')
  assert.equal(merged.total, 5)
  assert.equal(merged.nextCursor, null)
  assert.equal(merged.partial, true)
  assert.deepEqual(merged.columns, {
    backlog: [{ stem: 'TASK_1' }, { stem: 'TASK_3' }],
    pending: [{ stem: 'TASK_4' }],
    todo: [{ stem: 'TASK_2' }],
    done: [{ stem: 'TASK_5' }],
  })
  assert.equal(countLoadedTasks(merged.columns), 5)
  assert.equal(countLoadedTasks({ backlog: [], todo: [{}, {}] }), 2)
})

test('board delegates fresh per-task Figma projections without retaining panel-local selectors', () => {
  assert.match(board,
    /import \{ createFigmaTaskReadModel \} from '\.\.\/board\/figma-task-read-model\.js'/)
  assert.equal((board.match(/from '\.\.\/board\/figma-task-read-model\.js'/g) || []).length, 1)
  assert.match(board, /var figmaTaskReadModel = createFigmaTaskReadModel\(\{/)
  assert.match(board, /getSnapshot: function \(\) \{ return store\.get\(\); \}/)
  assert.doesNotMatch(board,
    /function (?:screensCacheFor|screensNeededNotPulled|blockingDesignIssuesFor|figmaEvidenceFor)\(/)
  for (const method of ['entry', 'needsUnpulledScreens', 'blockingDesignIssues', 'evidence']) {
    assert.match(board, new RegExp(`figmaTaskReadModel\\.${method}`), method)
  }

  assert.match(figmaTaskReadModelSource,
    /export function createFigmaTaskReadModel\(dependencies\)/)
  assert.doesNotMatch(figmaTaskReadModelSource, /from '\.\.\/panels\/board\.js'/)
  assert.doesNotMatch(figmaTaskReadModelSource,
    /tasksApi|requestJson|runControl|boardModal|clipboard|store|state\.|document|window|fetch\(|Date\./)
})

test('Figma task read model stays fresh and preserves exact server-projected references', () => {
  const designIssues = { captureBlocked: true, first: { kind: 'RISKY_SCREEN_NAME', line: 2 } }
  const evidence = { overall: 'WARN', stage: 'final' }
  const first = { needed: true, pulled: false, designIssues, evidence }
  let snapshot = { screensCache: { TASK_1_visual: first } }
  let reads = 0
  const model = createFigmaTaskReadModel({
    getSnapshot() {
      reads++
      return snapshot
    },
  })

  assert.equal(model.entry(null), null)
  assert.equal(reads, 0)
  assert.equal(model.entry('TASK_missing'), null)
  assert.equal(model.entry('TASK_1_visual'), first)
  assert.equal(model.needsUnpulledScreens('TASK_1_visual'), true)
  assert.equal(model.blockingDesignIssues('TASK_1_visual'), designIssues)
  assert.equal(model.evidence('TASK_1_visual'), evidence)

  snapshot = {
    screensCache: {
      TASK_1_visual: {
        needed: true,
        pulled: true,
        designIssues: { captureBlocked: false },
        evidence: null,
      },
      TASK_2_invalid: {
        needed: false,
        pulled: false,
        designIssues: 'invalid',
        evidence: 0,
      },
    },
  }
  const refreshed = model.entry('TASK_1_visual')
  assert.notEqual(refreshed, first)
  assert.equal(refreshed, snapshot.screensCache.TASK_1_visual)
  assert.equal(model.needsUnpulledScreens('TASK_1_visual'), false)
  assert.equal(model.blockingDesignIssues('TASK_1_visual'), null)
  assert.equal(model.evidence('TASK_1_visual'), null)
  assert.equal(model.needsUnpulledScreens('TASK_2_invalid'), false)
  assert.equal(model.blockingDesignIssues('TASK_2_invalid'), null)
  assert.equal(model.evidence('TASK_2_invalid'), null)

  snapshot = {}
  assert.equal(model.entry('TASK_1_visual'), null)
})

test('board delegates visual-evidence status presentation without retaining a panel-local copy', () => {
  assert.match(board,
    /import \{ createVisualEvidenceStatus \} from '\.\.\/board\/visual-evidence-status\.js'/)
  assert.equal((board.match(/from '\.\.\/board\/visual-evidence-status\.js'/g) || []).length, 1)
  assert.match(board, /var visualEvidenceStatus = createVisualEvidenceStatus\(\{ t: t \}\)/)
  assert.doesNotMatch(board,
    /function (?:evidenceLiteKey|evidenceStatusClass|evidenceStatusLabel)\(/)
  assert.match(board, /evidenceStatusClass: visualEvidenceStatus\.className/)
  assert.match(board, /evidenceStatusLabel: visualEvidenceStatus\.label/)
  assert.match(board,
    /visualEvidenceStatus\.liteKey\(figmaTaskReadModel\.evidence\(stem\)\)/)

  assert.match(visualEvidenceStatusSource,
    /export function createVisualEvidenceStatus\(dependencies\)/)
  assert.doesNotMatch(visualEvidenceStatusSource, /from '\.\.\/panels\/board\.js'/)
  assert.doesNotMatch(visualEvidenceStatusSource,
    /tasksApi|requestJson|runControl|boardModal|clipboard|store|state\.|document|window|fetch\(/)
})

test('visual-evidence status preserves snapshot keys, closed classes, and localized fallbacks', () => {
  const translations = new Map([
    ['board.figmaEvidence.status.pixel_special', 'Pixel special'],
    ['board.figmaEvidence.status.warn', 'Warning fallback'],
    ['board.figmaEvidence.status.unknown', 'Unknown fallback'],
  ])
  const status = createVisualEvidenceStatus({
    t: (key) => translations.has(key) ? translations.get(key) : key,
  })

  assert.equal(status.liteKey(null), '')
  assert.equal(status.liteKey({
    overall: 'WARN',
    stage: 'final',
    pipelineRunId: 'run-1',
    generatedAt: '2026-08-01T00:00:00.000Z',
    screenshotReportHash: 'sha256:abc',
    stale: true,
    missingRequiredCount: 2,
    blockingCount: 3,
    warningCount: 4,
    issueCount: 5,
    artifactCount: 6,
    screenshotCount: 7,
  }), [
    'WARN', 'final', 'run-1', '2026-08-01T00:00:00.000Z', 'sha256:abc', 'stale',
    2, 3, 4, 5, 6, 7,
  ].join('\u0001'))
  assert.equal(status.liteKey({}), [
    '', '', '', '', '', 'fresh', 0, 0, 0, 0, 0, 0,
  ].join('\u0001'))

  for (const value of ['PASS', 'pass']) assert.equal(status.className(value), 'pass', value)
  for (const value of ['WARN', 'WARNING', 'MINOR', 'MAJOR', 'SKIPPED']) {
    assert.equal(status.className(value), 'warn', value)
  }
  for (const value of [
    'INCOMPLETE', 'REVIEW_REQUIRED', 'ASPECT_MISMATCH', 'STALE_CAPTURE',
    'DUPLICATE_CAPTURE', 'UNREPRESENTABLE_OVERLAY', 'LOW_CONTENT_ORACLE',
    'CAPTURE_LOCALE_MISMATCH', 'NO_INDEXED_SCREENS',
  ]) assert.equal(status.className(value), 'incomplete', value)
  for (const value of ['BLOCKER', 'FAIL', 'ERROR']) assert.equal(status.className(value), 'blocker', value)
  for (const value of ['MISSING', 'MISSING_ORACLE', 'MISSING_CAPTURE']) {
    assert.equal(status.className(value), 'missing', value)
  }
  assert.equal(status.className('FUTURE_STATUS'), 'unknown')
  assert.equal(status.className(null), 'unknown')

  assert.equal(status.label('PIXEL SPECIAL'), 'Pixel special')
  assert.equal(status.label('WARNING'), 'Warning fallback')
  assert.equal(status.label('FUTURE_STATUS'), 'Unknown fallback')
  assert.equal(status.label('***'), 'Unknown fallback')
})

test('board delegates one read-only visual-evidence view without a panel-local copy', () => {
  assert.match(board,
    /import \{ createVisualEvidenceView \} from '\.\.\/board\/visual-evidence-view\.js'/)
  assert.equal((board.match(/from '\.\.\/board\/visual-evidence-view\.js'/g) || []).length, 1)
  assert.match(board, /var visualEvidenceView = createVisualEvidenceView\(\{/)
  assert.doesNotMatch(board,
    /function (?:buildEvidenceBadge|artifactKindLabel|artifactHref|comparePaneLabel|compareArtifactImg|compareStaticPane|buildCompareMiddle|buildCompareRow|visualTitle|clampUnit|visualMeta|visualEffectiveStatus|buildZonesList|buildEvidenceVisuals)\(/)
  assert.match(visualEvidenceViewSource,
    /export function createVisualEvidenceView\(dependencies\)/)
  assert.doesNotMatch(visualEvidenceViewSource, /from '\.\.\/panels\/board\.js'/)
  assert.doesNotMatch(visualEvidenceViewSource,
    /tasksApi|requestJson|runControl|spawnRebundleSession|globalMutationBlocked|boardModal|clipboard|store/)
})

test('visual-evidence view preserves URLs, trusted comparisons, metric clamps, and actions', () => {
  const t = (key, params = {}) => {
    const values = Object.entries(params).map(([name, value]) => `${name}=${value}`).join(',')
    return values ? `${key}(${values})` : key
  }
  const statusClass = (status) => String(status).toUpperCase() === 'PASS' ? 'pass' : 'blocker'
  const view = createVisualEvidenceView({
    el: fakeElement,
    t,
    pluralTemplate: (key, count, params = {}) => t(key, { n: count, ...params }),
    evidenceStatusClass: statusClass,
    evidenceStatusLabel: (status) => `status:${status}`,
  })
  const hash = `sha256:${'a'.repeat(64)}`

  assert.equal(view.artifactHref('TASK / 1', { id: 'actual?#' }, hash),
    `/api/figma/compare-artifact?stem=TASK%20%2F%201&id=actual%3F%23&reportHash=${encodeURIComponent(hash)}`)
  assert.equal(view.visualTitle({ screen: 'Home', theme: 'dark' }), 'Home · dark')
  assert.equal(view.visualTitle({}), 'board.figmaEvidence.visualUntitled')
  assert.equal(view.buildEvidenceVisuals({}, 'TASK_1', {}), null)

  const badge = view.buildEvidenceBadge('PASS')
  assert.equal(badge.className, 'board-evidence__badge board-evidence__badge--pass')
  assert.equal(badge.textContent, 'status:PASS')

  const response = {
    visualChecks: {
      reportHash: hash,
      total: 1,
      entries: [{
        screen: 'Checkout',
        theme: 'dark',
        status: 'FAIL',
        score: -0.25,
        coverage: 1.4,
        artifactSet: {
          reportHash: hash,
          artifacts: {
            figma: { id: 'figma' },
            actual: { id: 'actual' },
            diff: { id: 'diff' },
          },
        },
        zones: [{ name: 'Toolbar', status: 'FAIL', ssim: 1.2, deltaE: 2.25 }],
      }],
    },
  }
  const visual = view.buildEvidenceVisuals(response, 'TASK / 1', {
    trust: { usable: true, reason: 'ok' },
    promoteScore: true,
    groupByStatus: true,
    buildVisualActions: () => fakeElement('button', { class: 'visual-action', text: 'Fix' }),
  })
  const nodes = fakeTree(visual)
  const byClass = (name) => nodes.find((node) => node.className.split(/\s+/).includes(name))
  const allByClass = (name) => nodes.filter((node) => node.className.split(/\s+/).includes(name))

  assert.equal(byClass('board-evidence__visual-title').textContent, 'Checkout · dark')
  assert.match(byClass('board-evidence__visual-meta').textContent, /SSIM -0\.250/)
  assert.match(byClass('board-evidence__visual-meta').textContent,
    /board\.figmaEvidence\.visualCoverage\(n=100\)/)
  assert.match(byClass('board-evidence__visual-score').textContent,
    /board\.figmaEvidence\.similarity\(n=0\.0\)/)
  assert.equal(byClass('board-evidence__zone-fill').style.width, '100.0%')
  assert.equal(byClass('visual-action').textContent, 'Fix')
  assert.equal(allByClass('board-evidence__compare-mode').length, 2)
  assert.ok(nodes.some((node) => node.attributes.src &&
    node.attributes.src.includes('stem=TASK%20%2F%201&id=figma')))

  const modeButtons = allByClass('board-evidence__compare-mode')
  modeButtons[1].dispatch('click')
  assert.equal(modeButtons[1].attributes['aria-pressed'], 'true')
  const layers = allByClass('board-evidence__compare-layer')
  assert.equal(layers[0].style.display, 'none')
  assert.equal(layers[1].style.display, 'none')
  assert.equal(layers[2].style.display, '')

  const untrustedNodes = fakeTree(view.buildEvidenceVisuals(response, 'TASK / 1', {}))
  assert.ok(untrustedNodes.some((node) =>
    node.className === 'board-evidence__artifact-unavailable'))
  assert.equal(untrustedNodes.some((node) =>
    node.className.split(/\s+/).includes('board-evidence__compare-img')), false)
  assert.equal(untrustedNodes.some((node) =>
    node.className === 'board-evidence__zones'), false)
})

test('board delegates evidence cause and rerun presentation while retaining mutation ownership', () => {
  assert.match(board,
    /import \{ createVisualEvidenceRecoveryView \} from '\.\.\/board\/visual-evidence-recovery-view\.js'/)
  assert.equal((board.match(/from '\.\.\/board\/visual-evidence-recovery-view\.js'/g) || []).length, 1)
  assert.match(board, /var visualEvidenceRecoveryView = createVisualEvidenceRecoveryView\(\{/)
  assert.match(board, /onRebundle: spawnRebundleSession/)
  assert.match(board, /function spawnRebundleSession\(stem\)/)
  assert.match(board, /requestJson\('\/api\/figma\/pixel-review'/)
  assert.doesNotMatch(board,
    /function (?:buildEvidenceCause|evidenceRerunCommand|buildEvidenceRerun)\(/)
  assert.match(visualEvidenceRecoveryViewSource,
    /export function createVisualEvidenceRecoveryView\(dependencies\)/)
  assert.doesNotMatch(visualEvidenceRecoveryViewSource, /from '\.\.\/panels\/board\.js'/)
  assert.doesNotMatch(visualEvidenceRecoveryViewSource,
    /tasksApi|requestJson|FIGMA_SESSION_ACTION|terminal|clipboard|boardModal|spawnRebundleSession|document|window/)
})

test('evidence recovery view bounds causes and preserves safe rerun behavior', async () => {
  const t = (key, params = {}) => {
    const values = Object.entries(params).map(([name, value]) => `${name}=${value}`).join(',')
    return values ? `${key}(${values})` : key
  }
  const rebundles = []
  const failures = []
  let rejectRebundle = false
  const recovery = createVisualEvidenceRecoveryView({
    el: fakeElement,
    t,
    evidenceIssueLabel: (kind) => `issue:${kind}`,
    buildEvidenceBadge: (status) => fakeElement('span', {
      class: 'evidence-badge',
      text: `badge:${status}`,
    }),
    copyButton: (label, getText) => {
      const button = fakeElement('button', { class: 'copy-command', text: label })
      button.copyText = getText
      return button
    },
    onRebundle: (stem) => {
      rebundles.push(stem)
      return rejectRebundle ? Promise.reject(new Error('rebundle failed')) : Promise.resolve()
    },
    onRebundleError: (error) => failures.push(error.message),
  })

  assert.equal(recovery.buildCause(null), null)
  assert.equal(recovery.buildCause({ evidenceState: 'READY' }), null)
  const cause = recovery.buildCause({
    evidenceState: 'INCOMPLETE',
    missingRequiredReports: ['screenshot', '', 7],
    hashDriftReports: [{ name: 'spec' }, null],
    inputDriftReports: [
      { name: 'screen-cache', reason: 'missing' },
      { name: 'screen-cache', reason: 'missing-input-hash' },
      { name: 'census', reason: 'changed' },
    ],
    topIssues: [
      { severity: 'BLOCKER', report: 'spec', issueKind: 'SPEC_FAILED', message: 'private detail' },
      null,
      { severity: 'WARN', report: 'census', issueKind: 'CENSUS_STALE' },
      { severity: 'WARN', report: 'tokens', issueKind: 'TOKEN_DRIFT' },
      { severity: 'FAIL', report: 'capture', issueKind: 'CAPTURE_FAILED' },
      { severity: 'FAIL', report: 'omitted', issueKind: 'SIXTH_ISSUE' },
    ],
  })
  const causeNodes = fakeTree(cause)
  const causeText = causeNodes.map((node) => node.textContent).join('\n')
  assert.equal(cause.open, true)
  assert.match(causeText, /state=INCOMPLETE/)
  assert.match(causeText, /names=screenshot/)
  assert.match(causeText, /names=spec/)
  assert.match(causeText, /names=census/)
  assert.match(causeText, /names=screen-cache/)
  assert.match(causeText, /spec · issue:SPEC_FAILED/)
  assert.doesNotMatch(causeText, /private detail|SIXTH_ISSUE|omitted/)
  assert.equal(causeNodes.filter((node) => node.className === 'evidence-badge').length, 4)

  const hash = `sha256:${'a'.repeat(64)}`
  const response = {
    present: true,
    schemaVersion: 1,
    kind: 'figma-evidence',
    stage: 'final',
    pipelineRunId: 'run.1',
    reports: [{ pipelineRunId: 'run.1' }],
    visualChecks: { reportHash: hash },
    inputDriftReports: [{ name: 'census', reason: 'changed' }],
  }
  const command = [
    'FIGMA_PIPELINE_RUN_ID=run.1 node orchestrator/figma/scripts/evidence-clean.mjs TASK_9_ui --bundle-only',
    'FIGMA_PIPELINE_RUN_ID=run.1 node orchestrator/figma/scripts/component-census.mjs TASK_9_ui',
    'FIGMA_PIPELINE_RUN_ID=run.1 node orchestrator/figma/scripts/evidence-bundle.mjs TASK_9_ui --stage final --fresh',
  ].join('\n')
  assert.equal(recovery.rerunCommand('TASK_9_ui', response), command)
  assert.equal(recovery.rerunCommand('../TASK_9', response), '')
  assert.doesNotMatch(recovery.rerunCommand('TASK_9_ui', {
    ...response,
    pipelineRunId: 'unsafe value',
  }), /FIGMA_PIPELINE_RUN_ID/)

  const rerun = recovery.buildRerun('TASK_9_ui', response)
  const rerunNodes = fakeTree(rerun)
  assert.equal(rerunNodes.find((node) => node.className === 'board-evidence__rerun-hint').textContent,
    'board.figmaEvidence.rerun.hint')
  assert.equal(rerunNodes.find((node) => node.className === 'copy-command').copyText(), command)
  const button = rerunNodes.find((node) => node.className.includes('board-evidence__rebundle-btn'))
  button.dispatch('click')
  assert.equal(button.disabled, true)
  await Promise.resolve()
  assert.equal(button.disabled, false)
  assert.deepEqual(rebundles, ['TASK_9_ui'])

  rejectRebundle = true
  button.dispatch('click')
  await Promise.resolve()
  await Promise.resolve()
  assert.equal(button.disabled, false)
  assert.deepEqual(failures, ['rebundle failed'])
  assert.equal(recovery.buildRerun('../TASK_9', response), null)
  const untrusted = fakeTree(recovery.buildRerun('TASK_9_ui', {}))
  assert.equal(untrusted.find((node) => node.className === 'board-evidence__rerun-hint').textContent,
    'board.figmaEvidence.rerun.hintRebuild')
})

test('board delegates Figma screens presentation while retaining fetch and modal freshness ownership', () => {
  assert.match(board,
    /import \{ createFigmaScreensView \} from '\.\.\/board\/figma-screens-view\.js'/)
  assert.equal((board.match(/from '\.\.\/board\/figma-screens-view\.js'/g) || []).length, 1)
  assert.match(board, /var figmaScreensView = createFigmaScreensView\(\{/)
  assert.match(board, /function buildScreensSection\(stem, getModalToken\)/)
  assert.match(board, /fetchJson\('\/api\/figma\/screens\?stem='/)
  assert.match(board, /state\.activeModal !== getModalToken\(\)/)
  assert.doesNotMatch(board,
    /function (?:screenSpecLine|buildScreenCard|buildCensusTally)\(/)
  assert.match(figmaScreensViewSource,
    /export function createFigmaScreensView\(dependencies\)/)
  assert.doesNotMatch(figmaScreensViewSource, /from '\.\.\/panels\/board\.js'/)
  assert.doesNotMatch(figmaScreensViewSource,
    /fetchJson|tasksApi|requestJson|state\.activeModal|boardModal|clipboard|terminal|document|window/)
})

test('Figma screens view preserves cards, dark fallback, filtering, census, and image recovery', () => {
  const t = (key, params = {}) => {
    const values = Object.entries(params).map(([name, value]) => `${name}=${value}`).join(',')
    return values ? `${key}(${values})` : key
  }
  const view = createFigmaScreensView({
    el: fakeElement,
    t,
    relativeTime: (value) => `ago:${value}`,
    createTextNode: (value) => fakeElement('#text', { text: value }),
  })
  const mounted = view.buildSection()
  assert.equal(mounted.section.className, 'board-screens')
  assert.equal(mounted.section.attributes.tabindex, '0')
  assert.equal(mounted.body.children[0].textContent, 'board.screens.loading')

  view.renderResponse(mounted.body, 'TASK / 9', { present: false })
  assert.equal(mounted.body.children.length, 1)
  assert.equal(mounted.body.children[0].className, 'board-screens__empty')
  assert.equal(mounted.body.children[0].textContent, 'board.screens.empty')

  const nodes = Array.from({ length: 11 }, (_, index) => ({
    screen: index === 0 ? 'Checkout / Main' : `Screen ${index}`,
    url: index === 0 ? 'https://www.figma.com/design/AbC/App?node-id=1-2' : '',
    hasPng: index !== 1,
    theme: index === 0 ? 'light' : '',
    fetchedAt: index === 0 ? '2026-01-02T03:04:05.000Z' : '',
    frameSizeDp: index === 0 ? { w: 375, h: 812 } : null,
    elementCount: index === 0 ? 24 : null,
    instanceCount: index === 0 ? 6 : index === 1 ? 9 : null,
    darkTheme: index === 1 ? {
      hasPng: true,
      url: 'https://www.figma.com/design/AbC/App?node-id=3-4',
      screen: 'Wrong dark name',
      theme: 'dark',
      fetchedAt: '2026-02-03T04:05:06.000Z',
      frameSizeDp: { w: 412, h: 915 },
      instanceCount: 99,
    } : null,
  }))
  view.renderResponse(mounted.body, 'TASK / 9', {
    present: true,
    nodes,
    census: { mapped: 7, missing: 2, incomplete: 1, ambiguous: 3 },
  })
  const tree = fakeTree(mounted.body)
  const cards = tree.filter((node) => node.className === 'board-screens__card')
  assert.equal(cards.length, 11)
  const images = tree.filter((node) => node.className === 'board-screens__img')
  assert.equal(images[0].attributes.src,
    '/api/figma/screen-image?stem=TASK%20%2F%209&screen=Checkout%20%2F%20Main')
  assert.equal(images[0].attributes.alt, 'Checkout / Main')
  assert.equal(images[1].attributes.src,
    '/api/figma/screen-image?stem=TASK%20%2F%209&screen=Screen%201&theme=dark')
  const links = tree.filter((node) => node.tag === 'a')
  assert.equal(links[0].href, 'https://www.figma.com/design/AbC/App?node-id=1-2')
  assert.equal(links[0].attributes.target, '_blank')
  assert.equal(links[0].attributes.rel, 'noopener noreferrer')
  assert.ok(tree.some((node) => node.className === 'board-screens__spec' &&
    /375×812 · light · board\.screens\.elements\(n=24\) · board\.screens\.instances\(n=6\)/.test(node.textContent)))
  assert.ok(tree.some((node) => node.className === 'board-screens__spec' &&
    /412×915 · dark · board\.screens\.instances\(n=9\)/.test(node.textContent)))
  assert.ok(tree.some((node) => node.className === 'board-screens__pulled' &&
    node.textContent === 'board.screens.pulledAgo(ago=ago:2026-02-03T04:05:06.000Z)'))

  const fallbacks = tree.filter((node) => node.className === 'board-screens__fallback')
  images[0].dispatch('error')
  assert.equal(images[0].hidden, true)
  assert.equal(fallbacks[0].hidden, false)
  images[1].dispatch('load')
  assert.equal(fallbacks[1].hidden, true)

  const filter = tree.find((node) => node.className.includes('board-screens__filter'))
  assert.ok(filter)
  filter.value = 'checkout'
  filter.dispatch('input')
  assert.equal(cards[0].hidden, false)
  assert.equal(cards[1].hidden, true)
  filter.value = ''
  filter.dispatch('input')
  assert.equal(cards.every((card) => card.hidden === false), true)

  const censusCells = tree.filter((node) =>
    node.className.split(/\s+/).includes('board-screens__census-cell'))
  assert.equal(censusCells.length, 4)
  assert.equal(censusCells[0].className, 'board-screens__census-cell')
  assert.match(censusCells[1].className, /board-screens__census-cell--warn/)
  assert.equal(censusCells[2].className, 'board-screens__census-cell')
  assert.match(censusCells[3].className, /board-screens__census-cell--warn/)

  view.renderResponse(mounted.body, 'TASK / 9', { present: true, nodes: nodes.slice(0, 10) })
  const tenNodeTree = fakeTree(mounted.body)
  assert.equal(tenNodeTree.filter((node) => node.className === 'board-screens__card').length, 10)
  assert.equal(tenNodeTree.some((node) => node.className.includes('board-screens__filter')), false)

  view.renderError(mounted.body, 'safe detail')
  assert.equal(mounted.body.children.length, 1)
  assert.equal(mounted.body.children[0].textContent, 'board.screens.error(detail=safe detail)')
})

test('board delegates pixel-review presentation while retaining prompt, CSRF, POST, and rebundle ownership', () => {
  assert.match(board,
    /import \{ createPixelReviewView \} from '\.\.\/board\/pixel-review-view\.js'/)
  assert.equal((board.match(/from '\.\.\/board\/pixel-review-view\.js'/g) || []).length, 1)
  assert.match(board, /var pixelReviewView = createPixelReviewView\(\{/)
  assert.doesNotMatch(board, /function buildPixelReviewPanel\(stem, resp\)/)
  // A durable one-shot verdict is collected in the site's own dialog: the OS
  // prompt could not be themed, localized beyond its body, or confirmed.
  assert.doesNotMatch(board, /window\.(prompt|confirm|alert)\(/)
  assert.match(board, /promptDialog\(\{[\s\S]*?board\.figmaEvidence\.review\.notePrompt/)
  assert.match(board, /confirmDialog\(\{[\s\S]*?board\.figmaEvidence\.review\.confirmPassTitle/)
  assert.match(board, /window\.__ORCHESTRATOR_CSRF__/)
  assert.match(board, /requestJson\('\/api\/figma\/pixel-review'/)
  assert.match(board, /theme: row\.theme \|\| 'primary'/)
  assert.match(board, /return spawnRebundleSession\(stem\)/)
  assert.match(board,
    /clipboard\.toastError\(t\('board\.figmaEvidence\.review\.failed', \{ detail: boardRequestError\(error\) \}\)\)/)
  assert.match(pixelReviewViewSource, /export function createPixelReviewView\(dependencies\)/)
  assert.doesNotMatch(pixelReviewViewSource, /from '\.\.\/panels\/board\.js'/)
  assert.doesNotMatch(pixelReviewViewSource,
    /requestJson|tasksApi|FIGMA_SESSION_ACTION|spawnRebundleSession|__ORCHESTRATOR_CSRF__|clipboard|terminal|boardModal|document|window/)
})

test('pixel-review view preserves rows, verdict refusal, and async button lifecycle', async () => {
  const t = (key, params = {}) => {
    const values = Object.entries(params).map(([name, value]) => `${name}=${value}`).join(',')
    return values ? `${key}(${values})` : key
  }
  const prepared = []
  const submissions = []
  const failures = []
  let refuseFail = true
  let rejectSubmission = false
  const view = createPixelReviewView({
    el: fakeElement,
    t,
    prepareVerdict: (verdict, row) => {
      prepared.push({ verdict, screen: row.screen })
      if (verdict === 'fail' && refuseFail) return null
      return { note: verdict === 'minor' ? 'small mismatch' : verdict === 'fail' ? 'broken' : '' }
    },
    submitVerdict: (stem, row, verdict, note) => {
      submissions.push({ stem, screen: row.screen, theme: row.theme, verdict, note })
      return rejectSubmission ? Promise.reject(new Error('submit failed')) : Promise.resolve()
    },
    onSubmitError: (error) => failures.push(error.message),
  })

  assert.equal(view.buildPanel('', {}), null)
  assert.equal(view.buildPanel('../TASK_1', { pixelReview: { pending: [{ screen: 'Home' }] } }), null)
  assert.equal(view.buildPanel('TASK_1', { pixelReview: { pending: [] } }), null)

  const panel = view.buildPanel('TASK_1', {
    pixelReview: {
      pending: [
        { screen: 'Checkout', theme: 'primary', renderClass: 'canvas', pixelStatus: 'REVIEW_REQUIRED' },
        { screen: 'Profile', theme: 'dark' },
      ],
      resolved: [{ screen: 'Done' }],
    },
  })
  const tree = fakeTree(panel)
  assert.equal(panel.className, 'board-evidence__rerun board-evidence__review')
  assert.equal(tree.find((node) => node.className === 'board-evidence__rerun-hint').textContent,
    'board.figmaEvidence.review.hint(count=2)')
  const rows = tree.filter((node) => node.className === 'board-evidence__review-row')
  assert.equal(rows.length, 2)
  assert.equal(fakeTree(rows[0]).find((node) => node.tag === 'strong').textContent, 'Checkout')
  assert.equal(fakeTree(rows[1]).find((node) => node.tag === 'strong').textContent, 'Profile [dark]')
  assert.equal(tree.find((node) => node.className === 'board-evidence__review-class').textContent, 'canvas')
  assert.ok(tree.some((node) => node.textContent ===
    'board.figmaEvidence.review.raw(status=REVIEW_REQUIRED)'))
  assert.ok(tree.some((node) => node.textContent ===
    'board.figmaEvidence.review.resolved(count=1)'))

  const buttons = tree.filter((node) =>
    node.className.split(/\s+/).includes('board-evidence__review-btn'))
  assert.equal(buttons.length, 6)
  assert.deepEqual(buttons.slice(0, 3).map((button) => button.textContent), [
    'board.figmaEvidence.review.pass',
    'board.figmaEvidence.review.minor',
    'board.figmaEvidence.review.fail',
  ])

  // prepareVerdict opens a real dialog, so it may be async: the button stays
  // locked across prepare AND submit so a durable verdict cannot be double-fired.
  const flush = () => new Promise((resolve) => { setTimeout(resolve, 0) })

  buttons[0].dispatch('click')
  assert.equal(buttons[0].disabled, true)
  await flush()
  assert.equal(buttons[0].disabled, false)
  assert.deepEqual(submissions[0], {
    stem: 'TASK_1', screen: 'Checkout', theme: 'primary', verdict: 'pass', note: '',
  })

  buttons[1].dispatch('click')
  await flush()
  assert.deepEqual(submissions[1], {
    stem: 'TASK_1', screen: 'Checkout', theme: 'primary', verdict: 'minor', note: 'small mismatch',
  })

  buttons[2].dispatch('click')
  assert.equal(buttons[2].disabled, true, 'the lock is taken before the note dialog opens')
  await flush()
  assert.equal(buttons[2].disabled, false, 'a refused verdict releases the button')
  assert.equal(submissions.length, 2, 'a refused verdict submits nothing')

  refuseFail = false
  rejectSubmission = true
  buttons[5].dispatch('click')
  assert.equal(buttons[5].disabled, true)
  await flush()
  assert.equal(buttons[5].disabled, false)
  assert.deepEqual(failures, ['submit failed'])
  assert.deepEqual(prepared.map((entry) => entry.verdict), ['pass', 'minor', 'fail', 'fail'])
})

test('board delegates one pure visual-fix task specification without a panel-local copy', () => {
  assert.match(board,
    /import \{ createVisualFixTask \} from '\.\.\/board\/visual-fix-task\.js'/)
  assert.equal((board.match(/from '\.\.\/board\/visual-fix-task\.js'/g) || []).length, 1)
  assert.match(board, /var visualFixTask = createVisualFixTask\(\{/)
  assert.doesNotMatch(board,
    /function (?:safeInline|slugPart|visualFixKey|visualFixMarker|designSectionOf|isPullableDesignLine|bodyHasPullableDesign|pullableBulletCount|extractDesignSectionBullets|isFigmaDesignBullet|designBulletMatchesVisual|relevantFigmaDesignBullets|hasFigmaDesignBullet|reportHashFromBundle|buildVisualFixTaskBody)\(/)
  assert.match(visualFixTaskSource,
    /export function createVisualFixTask\(dependencies\)/)
  assert.doesNotMatch(visualFixTaskSource, /from '\.\.\/panels\/board\.js'/)
  assert.doesNotMatch(visualFixTaskSource,
    /tasksApi|requestJson|runControl|spawnRebundleSession|globalMutationBlocked|boardModal|clipboard|store|document|window/)
})

test('board delegates visual-fix action presentation while retaining trust, state, and mutation ownership', () => {
  assert.match(board,
    /import \{ createVisualFixActionsView \} from '\.\.\/board\/visual-fix-actions-view\.js'/)
  assert.equal((board.match(/from '\.\.\/board\/visual-fix-actions-view\.js'/g) || []).length, 1)
  assert.match(board, /var visualFixActionsView = createVisualFixActionsView\(\{/)
  assert.match(board, /function buildVisualFixActions\(row, resp, stem, folder, item, fixStates, rerender, stillActive\)/)
  assert.doesNotMatch(board, /var FIX_BLOCKED_REASON_KEYS = \{/)
  assert.doesNotMatch(board, /function buildVisualFixBlocked\(reason\)/)
  assert.match(board, /globalMutationBlocked\(\)/)
  assert.match(board, /finalVisualTrustState\(resp\)/)
  assert.match(board, /artifactSetReportHashOk\(row\.artifactSet, reportHash\)/)
  assert.match(board, /fixStates\[key\] = 'loading'/)
  assert.match(board, /loadParentTaskMarkdown\(folder, stem\)/)
  assert.match(board, /createBacklogWithIntegrityFence\(title, built\.body/)
  assert.match(visualFixActionsViewSource,
    /export function createVisualFixActionsView\(dependencies\)/)
  assert.doesNotMatch(visualFixActionsViewSource, /from '\.\.\/panels\/board\.js'/)
  assert.doesNotMatch(visualFixActionsViewSource,
    /tasksApi|requestJson|createBacklogWithIntegrityFence|globalMutationBlocked|finalVisualTrustState|artifactSetReportHashOk|visualFixTask|fixStates|rerender|stillActive|clipboard|boardModal|document|window/)
})

test('visual-fix actions view preserves blocked reasons, state labels, disabling, and click routing', () => {
  const view = createVisualFixActionsView({
    el: fakeElement,
    t: (key) => key,
  })
  const blockedReasons = new Map([
    ['missing-required', 'board.figmaEvidence.fix.blockedMissingRequired'],
    ['stale', 'board.figmaEvidence.fix.blockedStale'],
    ['hash-drift', 'board.figmaEvidence.fix.blockedHashDrift'],
    ['mixed-runs', 'board.figmaEvidence.fix.blockedMixedRuns'],
    ['no-artifacts', 'board.figmaEvidence.fix.blockedNoArtifacts'],
    ['global-recovery', 'board.integrity.createBlocked'],
    ['unexpected', 'board.figmaEvidence.fix.blockedGeneric'],
  ])
  for (const [reason, expectedKey] of blockedReasons) {
    const blocked = view.buildBlocked(reason)
    const nodes = fakeTree(blocked)
    const help = nodes.find((node) => node.className.includes('board-evidence__fix-blocked-reason'))
    const button = nodes.find((node) => node.className.includes('board-evidence__fix-btn'))
    assert.equal(blocked.className, 'board-evidence__fix board-evidence__fix--blocked')
    assert.equal(help.className, 'board-evidence__fix-help board-evidence__fix-blocked-reason')
    assert.equal(help.textContent, expectedKey)
    assert.equal(button.type, 'button')
    assert.equal(button.className, 'btn btn--ghost board-evidence__fix-btn')
    assert.equal(button.textContent, 'board.figmaEvidence.fix.create')
    assert.equal(button.attributes.title, expectedKey)
    assert.equal(button.disabled, true)
  }

  const stateCases = [
    ['idle', 'board.figmaEvidence.fix.create', false],
    ['loading', 'board.figmaEvidence.fix.loading', true],
    ['queued', 'board.figmaEvidence.fix.queued', true],
    ['failed', 'board.figmaEvidence.fix.failed', false],
    ['unknown', 'board.figmaEvidence.fix.create', false],
  ]
  let clicks = 0
  const clickArguments = []
  for (const [state, expectedKey, disabled] of stateCases) {
    const actions = view.buildActions(state, (...args) => {
      clicks += 1
      clickArguments.push(args)
    })
    const nodes = fakeTree(actions)
    const help = nodes.find((node) => node.className === 'board-evidence__fix-help')
    const button = nodes.find((node) => node.className.includes('board-evidence__fix-btn'))
    assert.equal(actions.className, 'board-evidence__fix')
    assert.equal(help.className, 'board-evidence__fix-help')
    assert.equal(help.textContent, 'board.figmaEvidence.fix.helper')
    assert.equal(button.type, 'button')
    assert.equal(button.className, 'btn btn--ghost board-evidence__fix-btn')
    assert.equal(button.textContent, expectedKey)
    assert.equal(button.disabled, disabled)
    if (!disabled) button.dispatch('click')
  }
  assert.equal(clicks, 3)
  assert.deepEqual(clickArguments, [[], [], []])
})

test('board delegates final visual-evidence summary presentation while retaining async lifecycle and mutations', () => {
  assert.match(board,
    /import \{ createVisualEvidenceSummaryView \} from '\.\.\/board\/visual-evidence-summary-view\.js'/)
  assert.equal((board.match(/from '\.\.\/board\/visual-evidence-summary-view\.js'/g) || []).length, 1)
  assert.match(board, /var visualEvidenceSummaryView = createVisualEvidenceSummaryView\(\{/)
  assert.doesNotMatch(board, /function renderEvidenceSummary\(bodyEl, resp, opts\)/)
  assert.match(board, /visualEvidenceSummaryView\.render\(bodyEl, resp, \{/)
  assert.match(board, /function buildEvidenceSection\(stem, getModalToken, folder, item\)/)
  assert.match(board, /tasksApi\.loadFigmaEvidence\(stem\)/)
  assert.match(board, /bodyEl\.querySelector\('\.board-evidence__filter'\)/)
  assert.match(board, /buildVisualFixActions\(row, evidenceResp, stem, folder, item, fixStates/)

  assert.match(visualEvidenceSummaryViewSource,
    /export function createVisualEvidenceSummaryView\(dependencies\)/)
  assert.doesNotMatch(visualEvidenceSummaryViewSource, /from '\.\.\/panels\/board\.js'/)
  assert.doesNotMatch(visualEvidenceSummaryViewSource,
    /tasksApi|requestJson|loadFigmaEvidence|createBacklogWithIntegrityFence|globalMutationBlocked|fixStates|store|state\.|clipboard|terminal|document|window|fetch\(/)
})

test('visual-evidence summary view preserves final, recovery, review, drift, and unavailable presentation', () => {
  const visualCalls = []
  const badgeCalls = []
  const causeCalls = []
  const reviewCalls = []
  const rerunCalls = []
  const translationCalls = []
  const filterInput = () => {}
  const buildVisualActions = () => null
  const view = createVisualEvidenceSummaryView({
    el: fakeElement,
    t: (key, params) => {
      translationCalls.push({ key, params })
      return key
    },
    finalVisualDisplayState: (resp) => ({ trustFor: resp.id }),
    evidenceStatusClass: (status) => status === 'PASS' ? 'pass' : status === 'WARN' ? 'warn' : 'problem',
    buildEvidenceVisuals: (resp, stem, options) => {
      visualCalls.push({ resp, stem, options })
      return resp.visual || null
    },
    buildEvidenceBadge: (status) => {
      badgeCalls.push(status)
      return fakeElement('span', { class: 'badge', text: status })
    },
    buildCause: (resp) => {
      causeCalls.push(resp.id)
      return resp.cause || null
    },
    buildReviewPanel: (stem, resp) => {
      reviewCalls.push({ stem, id: resp.id })
      return resp.review || null
    },
    buildRerun: (stem, resp) => {
      rerunCalls.push({ stem, id: resp.id })
      return resp.rerun || null
    },
  })

  const body = fakeElement('div', {}, [fakeElement('p', { text: 'stale' })])
  view.render(body, { id: 'absent', present: false }, { stem: 'TASK_1' })
  assert.equal(body.children.length, 1)
  assert.equal(body.children[0].className, 'board-evidence__empty')
  assert.equal(body.children[0].textContent, 'board.figmaEvidence.unavailable')
  assert.equal(visualCalls.length, 0)

  const cause = fakeElement('div', { class: 'cause' })
  const review = fakeElement('div', { class: 'review' })
  const rerun = fakeElement('div', { class: 'rerun' })
  const problem = {
    id: 'problem', stage: 'final', evidenceState: 'INCOMPLETE', overall: 'PASS', stem: 'RESP_STEM',
    cause, review, rerun,
  }
  view.render(body, problem, {
    stem: 'TASK_2', filterValue: 'home', onFilterInput: filterInput, buildVisualActions,
  })
  assert.deepEqual(body.children.map((node) => node.className), [
    'board-evidence__result board-evidence__result--problem',
    'cause',
    'review',
    'rerun',
    'board-evidence__empty',
  ])
  assert.equal(body.children[0].children[0].textContent, 'INCOMPLETE')
  assert.equal(body.children[0].children[1].textContent, 'board.figmaEvidence.resultProblem')
  assert.equal(body.children[4].textContent, 'board.figmaEvidence.detailsUnavailable')
  assert.deepEqual(badgeCalls, ['INCOMPLETE'])
  assert.deepEqual(causeCalls, ['problem'])
  assert.deepEqual(reviewCalls, [{ stem: 'TASK_2', id: 'problem' }])
  assert.deepEqual(rerunCalls, [{ stem: 'TASK_2', id: 'problem' }])
  assert.deepEqual(visualCalls[0], {
    resp: problem,
    stem: 'RESP_STEM',
    options: {
      trust: { trustFor: 'problem' },
      stem: 'TASK_2',
      promoteScore: true,
      groupByStatus: true,
      filterValue: 'home',
      onFilterInput: filterInput,
      buildVisualActions,
    },
  })

  const problemVisual = fakeElement('details', { class: 'visual' })
  view.render(body, {
    id: 'problem-visual', stage: 'final', evidenceState: 'INCOMPLETE', overall: 'BLOCKER',
    visual: problemVisual,
  }, { stem: 'TASK_3' })
  assert.equal(problemVisual.open, true)
  assert.equal(body.children.at(-1), problemVisual)
  assert.equal(body.children.some((node) => node.textContent === 'board.figmaEvidence.detailsUnavailable'), false)
  assert.deepEqual(badgeCalls, ['INCOMPLETE', 'BLOCKER'])

  const readyReview = fakeElement('div', { class: 'ready-review' })
  const readyVisual = fakeElement('details', { class: 'ready-visual' })
  const ready = {
    id: 'ready', stage: 'final', evidenceState: 'READY', overall: 'WARN', stem: 'READY_STEM',
    review: readyReview,
    visual: readyVisual,
    inputDriftReports: [
      { name: 'spec', reason: 'changed' },
      { name: 'spec', reason: 'changed-again' },
      { name: 'capture', reason: 'missing' },
      { name: 'catalog', reason: 'design-source-unavailable' },
      { name: 'capture', reason: 'missing-input-hash' },
      { reason: 'changed' },
    ],
  }
  view.render(body, ready, { stem: 'TASK_4' })
  assert.deepEqual(body.children.map((node) => node.className), [
    'ready-review',
    'board-evidence__warn-accepted',
    'board-evidence__input-drift',
    'board-evidence__input-drift',
    'ready-visual',
  ])
  assert.equal(body.children[1].textContent, 'board.figmaEvidence.warnAccepted')
  assert.equal(body.children[2].textContent, 'board.figmaEvidence.inputDriftReady')
  assert.equal(body.children[3].textContent, 'board.figmaEvidence.inputUnavailableReady')
  assert.equal(readyVisual.open, true)
  assert.deepEqual(translationCalls.filter((call) => call.key === 'board.figmaEvidence.inputDriftReady'), [
    { key: 'board.figmaEvidence.inputDriftReady', params: { names: 'spec' } },
  ])
  assert.deepEqual(translationCalls.filter((call) => call.key === 'board.figmaEvidence.inputUnavailableReady'), [
    { key: 'board.figmaEvidence.inputUnavailableReady', params: { names: 'capture, catalog' } },
  ])

  view.render(body, {
    id: 'ready-empty', stage: 'final', evidenceState: 'READY', overall: 'PASS', inputDriftReports: [],
  }, { stem: 'TASK_5' })
  assert.equal(body.children.length, 1)
  assert.equal(body.children[0].textContent, 'board.figmaEvidence.unavailable')
})

test('visual-fix task specification preserves Design parsing, identity, and durable evidence', () => {
  const fixTask = createVisualFixTask({
    visualTitle: (row) => `${row.screen || 'Untitled'} · ${row.theme || 'default'}`,
    artifactHref: (stem, artifact, reportHash) =>
      `/artifact/${encodeURIComponent(stem)}/${encodeURIComponent(artifact.id)}/${encodeURIComponent(reportHash)}`,
  })
  const hash = `sha256:${'a'.repeat(64)}`
  const bundleHash = `sha256:${'b'.repeat(64)}`
  const checkout = '- Checkout dark — https://www.figma.com/design/AbC123/App?node-id=1-2'
  const profile = '- Profile light - https://figma.com/file/XyZ789/App?node-id=3:4'
  const markdown = [
    '# Parent',
    '',
    '## Design',
    '',
    checkout,
    profile,
    '- Disabled -- none https://figma.com/design/AbC123/App?node-id=5-6',
    '',
    '## Acceptance',
    '',
    '- Not design — https://figma.com/design/AbC123/App?node-id=7-8',
  ].join('\n')

  assert.equal(fixTask.hasPullableDesign(markdown), true)
  assert.equal(fixTask.pullableBulletCount(markdown), 2)
  assert.equal(fixTask.hasPullableDesign([
    '## Design',
    '## Acceptance',
    '- Impostor — https://figma.com/design/AbC123/App?node-id=7-8',
  ].join('\n')), false)
  assert.equal(fixTask.hasPullableDesign([
    '## Design',
    '- Placeholder — <Figma node URL>',
    '- Disabled — none https://figma.com/design/AbC123/App?node-id=5-6',
  ].join('\n')), false)

  const row = {
    screen: 'Checkout',
    theme: 'dark',
    status: 'FAIL',
    score: 0.8,
    coverage: 0.9,
    reason: 'Header differs',
    artifactSet: { artifacts: { diff: { id: 'diff image' }, overlay: { id: 'overlay image' } } },
  }
  const response = {
    pipelineRunId: 'run-1',
    bundle: { hash: bundleHash },
    visualChecks: { reportHash: hash },
  }
  const built = fixTask.buildTaskBody(markdown, response, row, 'TASK 7')

  assert.equal(fixTask.key('TASK 7', row), 'task_7:checkout:dark')
  assert.equal(fixTask.inlineTitle({ screen: 'Checkout\nDialog', theme: 'dark' }, 80),
    'Checkout Dialog · dark')
  assert.deepEqual(built.designBullets, [checkout])
  assert.match(built.body,
    /<!-- figma-visual-fix key=task_7:checkout:dark stem=TASK 7 report=sha256:a{64} -->/)
  assert.match(built.body, /- Difference: \/artifact\/TASK%207\/diff%20image\/sha256%3A/)
  assert.match(built.body, /- Overlay: \/artifact\/TASK%207\/overlay%20image\/sha256%3A/)
  assert.match(built.body, /evidence hash sha256:b{64}, screenshot hash sha256:a{64}\./)
  assert.doesNotMatch(built.body, /Profile light/)

  const fallback = fixTask.buildTaskBody(markdown, response, { ...row, screen: 'Missing' }, 'TASK_8')
  assert.deepEqual(fallback.designBullets, [checkout, profile])
})

test('visual-evidence trust rejects every stale, mixed, unreadable or unbound shape', () => {
  const hash = `sha256:${'a'.repeat(64)}`
  const base = {
    present: true,
    schemaVersion: 1,
    kind: 'figma-evidence',
    stage: 'final',
    reports: [{ pipelineRunId: 'run-1' }],
    visualChecks: { reportHash: hash },
  }
  const trust = (patch = {}, opts = { visual: true, requiredStage: 'final' }) =>
    evidenceTrustState({ ...base, ...patch }, opts)

  assert.deepEqual(evidenceTrustState(null), { usable: false, reason: 'missing' })
  assert.deepEqual(trust({ present: false }), { usable: false, reason: 'missing' })
  assert.deepEqual(trust({ schemaVersion: 2 }), { usable: false, reason: 'bad-schema' })
  assert.deepEqual(trust({ kind: 'android-build' }), { usable: false, reason: 'unsupported' })
  assert.deepEqual(trust({ stage: 'checkpoint' }), { usable: false, reason: 'wrong-stage' })
  assert.deepEqual(trust({ stale: true }), { usable: false, reason: 'stale' })
  assert.deepEqual(trust({ hashDriftReports: ['evidence'] }), { usable: false, reason: 'hash-drift' })
  assert.deepEqual(trust({ missingRequiredReports: ['screenshot'] }), { usable: false, reason: 'missing-required' })
  assert.deepEqual(trust({ reports: [{ unreadable: true }] }), { usable: false, reason: 'unreadable-report' })
  assert.deepEqual(trust({ reports: [{ pipelineRunId: 'run-1' }, { pipelineRunId: 'run-2' }] }),
    { usable: false, reason: 'mixed-runs' })
  assert.deepEqual(trust({ reports: [], runIds: ['run-1', 'run-2'] }),
    { usable: false, reason: 'mixed-runs' })
  assert.deepEqual(trust({ visualChecks: { reportHash: 'sha256:short' } }),
    { usable: false, reason: 'bad-report-hash' })
  assert.deepEqual(trust(), { usable: true, reason: 'ok' })

  assert.deepEqual(reportRunIds({
    reports: [{ pipelineRunId: 'run-1' }, { pipelineRunId: 'run-1' }],
    runIds: ['ignored-fallback'],
  }), ['run-1'])
  assert.equal(isValidReportHash(hash.toUpperCase()), true)
  assert.equal(isValidReportHash(`sha512:${'a'.repeat(64)}`), false)
  assert.equal(artifactSetReportHashOk({}, hash), true)
  assert.equal(artifactSetReportHashOk({ reportHash: hash }, hash), true)
  assert.equal(artifactSetReportHashOk({ reportHash: `sha256:${'b'.repeat(64)}` }, hash), false)
  assert.equal(artifactSetReportHashOk({}, 'invalid'), false)
})

test('final visual display relaxes only completeness while fix-task trust stays strict', () => {
  const hash = `sha256:${'c'.repeat(64)}`
  const response = {
    present: true,
    schemaVersion: 1,
    kind: 'figma-evidence',
    stage: 'final',
    missingRequiredReports: ['evidence'],
    reports: [{ pipelineRunId: 'run-1' }],
    visualChecks: { reportHash: hash },
  }

  assert.deepEqual(finalVisualTrustState(response),
    { usable: false, reason: 'missing-required' })
  assert.deepEqual(finalVisualDisplayState(response), { usable: true, reason: 'ok' })
  assert.deepEqual(finalVisualDisplayState({ ...response, stale: true }),
    { usable: false, reason: 'stale' })
  assert.deepEqual(finalVisualDisplayState({ ...response, visualChecks: { reportHash: 'invalid' } }),
    { usable: false, reason: 'bad-report-hash' })
})

test('worker-support policy distinguishes recovery, auth, heartbeat, and lock freshness', () => {
  const workerSupport = createBoardWorkerSupport({
    parseIso(value) {
      if (!value) return null
      const parsed = new Date(value)
      return Number.isNaN(parsed.getTime()) ? null : parsed
    },
    clampNow(value) {
      return Math.min(value, Date.now())
    },
    staleLockMs: 20 * 60 * 1000,
  })
  const pending = {
    startupRecovery: { status: 'ready' },
    progress: { requests: [{ id: 'queued' }] },
  }
  const iso = (offset) => new Date(Date.now() + offset).toISOString()

  assert.deepEqual(workerSupport.pendingRequests(null), [])
  assert.equal(workerSupport.pendingRequests(pending), pending.progress.requests)
  assert.equal(workerSupport.cliCannotAuth(null), false)
  assert.equal(workerSupport.cliCannotAuth({ loggedIn: false }), true)
  assert.equal(workerSupport.cliCannotAuth({ loggedIn: true, authProblem: 'expired' }), true)
  assert.equal(workerSupport.cliCannotAuth({ loggedIn: true }), false)
  assert.equal(workerSupport.workerLooksOffline({
    startupRecovery: { status: 'pending' },
    progress: pending.progress,
  }), false)
  assert.equal(workerSupport.workerLooksOffline({
    startupRecovery: { status: 'ready' },
    progress: { requests: [] },
  }), false)
  assert.equal(workerSupport.workerLooksOffline({
    ...pending,
    runnerActive: true,
    cli: { loggedIn: true },
  }), false)
  assert.equal(workerSupport.workerLooksOffline({
    ...pending,
    runnerActive: true,
    cli: { loggedIn: false },
  }), true)
  assert.equal(workerSupport.workerLooksOffline({
    ...pending,
    status: { worker: { heartbeatAt: iso(-1000) } },
  }), false)
  assert.equal(workerSupport.workerLooksOffline({
    ...pending,
    status: { worker: { heartbeatAt: iso(-100 * 1000) } },
  }), true)
  assert.equal(workerSupport.workerLooksOffline({
    ...pending,
    status: { locks: { count: 1, newestStartedAt: iso(-1000) } },
  }), false)
  assert.equal(workerSupport.workerLooksOffline({
    ...pending,
    status: { locks: { count: 1, newestStartedAt: iso(-21 * 60 * 1000) } },
  }), true)
  assert.equal(workerSupport.workerLooksOffline({
    ...pending,
    status: { locks: { count: 1, newestStartedAt: iso(60 * 60 * 1000) } },
  }), false)
  assert.equal(workerSupport.drainerAttached({ runnerActive: true, cli: { loggedIn: true } }), true)
  assert.equal(workerSupport.drainerAttached({
    runnerActive: true,
    cli: { loggedIn: true, authProblem: 'expired' },
  }), false)
  assert.equal(workerSupport.drainerAttached({
    status: { worker: { heartbeatAt: iso(-1000) } },
  }), true)
  assert.equal(workerSupport.drainerAttached({
    status: { worker: { heartbeatAt: iso(-100 * 1000) } },
  }), false)
})

test('overflow policy exposes only unique available actions for each task state', () => {
  const sourceTarget = { panel: 'board', entityId: 'TASK_1_source', availability: 'available' }
  const secondary = (kind, labelKey, enabled = true) => ({ kind, labelKey, enabled })
  const row = (state, actions, extra = {}) => ({
    state,
    sourceTarget,
    runtimeStatus: { active: false },
    secondaryActions: actions,
    ...extra,
  })
  const kinds = (value) => taskOverflowItems(value).map((item) => item.kind)

  const backlog = taskOverflowItems(row('backlog', [
    secondary('drop', 'board.overflow.drop'),
    secondary('copy-prompt', 'board.overflow.copy_prepare_prompt'),
  ]))
  assert.deepEqual(backlog.map((item) => item.kind), [
    'source', 'edit', 'copy-id', 'copy-prompt', 'drop',
  ])
  assert.equal(backlog.find((item) => item.kind === 'copy-prompt').labelKey,
    'board.overflow.copy_prepare_prompt')
  assert.equal(backlog.find((item) => item.kind === 'edit').separated, true)
  assert.equal(backlog.find((item) => item.kind === 'drop').tone, 'danger')

  assert.deepEqual(kinds(row('pending', [secondary('drop', 'board.overflow.drop')])), [
    'source', 'copy-id', 'drop',
  ])
  const todo = taskOverflowItems(row('todo', [
    secondary('drop', 'board.overflow.drop'),
    secondary('copy-prompt', 'board.overflow.copy_run_prompt'),
  ]))
  assert.deepEqual(todo.map((item) => item.kind), [
    'source', 'copy-id', 'copy-prompt', 'drop',
  ])
  assert.equal(todo.find((item) => item.kind === 'copy-prompt').labelKey,
    'board.overflow.copy_run_prompt')
  assert.deepEqual(kinds(row('done', [
    secondary('reopen', 'board.overflow.reopen'),
    secondary('drop', 'board.overflow.drop'),
  ])), [
    'source', 'copy-id', 'reopen', 'drop',
  ])
  assert.deepEqual(kinds(row('corrupt', [
    secondary('drop', 'board.overflow.drop'),
  ], { sourceTarget: null })), [
    'copy-id', 'drop',
  ])

  const unavailable = taskOverflowItems(row('todo', [
    secondary('drop', 'board.overflow.drop', false),
    secondary('copy-prompt', 'board.overflow.copy_run_prompt', false),
    secondary('unknown', 'board.overflow.unknown'),
  ], { sourceTarget: { availability: 'unavailable' } }))
  assert.deepEqual(unavailable.map((item) => item.kind), ['copy-id'])
  assert.equal(new Set(unavailable.map((item) => item.kind)).size, unavailable.length)
  assert.doesNotMatch(overflowPolicy, /board\.overflow\.open_task/)
  assert.equal(Object.hasOwn(en, 'board.overflow.open_task'), false)
  assert.equal(Object.hasOwn(ru, 'board.overflow.open_task'), false)
})

test('desktop task-card chrome is compact, aligned and has no phantom mobile footer column', () => {
  assert.match(panels, /\.board-card\s*\{[\s\S]*?padding:\s*9px;[\s\S]*?gap:\s*6px;/)
  assert.match(panels, /\.board-card__header\s*\{[\s\S]*?grid-template-areas:[\s\S]*?"identity overflow"[\s\S]*?"badges badges";/)
  assert.match(panels, /\.board-card__identity\s*\{[\s\S]*?display:\s*grid;[\s\S]*?min-width:\s*0;/)
  assert.match(panels, /\.board-card\s*\{[\s\S]*?cursor:\s*pointer;/)
  assert.match(panels, /\.board-card__blocker\s*\{[\s\S]*?justify-self:\s*start;[\s\S]*?width:\s*fit-content;[\s\S]*?min-height:\s*22px;[\s\S]*?font-size:\s*0\.625rem;[\s\S]*?cursor:\s*inherit;/)
  assert.match(panels, /\.board-card__footer\s*\{[\s\S]*?width:\s*100%;[\s\S]*?margin-top:\s*auto;/)
  assert.match(panels, /\.board-card__primary-action\s*\{[\s\S]*?min-height:\s*32px;[\s\S]*?font-size:\s*0\.75rem;/)
  assert.match(panels, /\.board-card__overflow-trigger\s*\{\s*width:\s*30px;[\s\S]*?min-height:\s*30px;/)
  assert.match(panels, /\.board-card__overflow-menu\s*\{[\s\S]*?right:\s*0;[\s\S]*?top:\s*calc\(100% \+ 6px\);/)
  assert.match(panels, /\.board-card__overflow-item--separated::before/)
  assert.match(panels, /\.board-card__overflow-item--danger\s*\{\s*color:\s*var\(--banner-danger-fg\);/)
  assert.match(panels, /@media \(max-width:\s*860px\)[\s\S]*?\.board-card__title-button\s*\{[\s\S]*?min-height:\s*44px;[\s\S]*?\.board-card__footer\s*\{\s*display:\s*flex;\s*width:\s*100%;\s*\}[\s\S]*?\.board-card__primary-action\s*\{[^}]*min-height:\s*44px;[\s\S]*?\.board-card__overflow-trigger\s*\{[^}]*min-height:\s*44px;/)
  assert.doesNotMatch(panels, /\.board-card__footer\s*\{[^}]*grid-template-columns/)
  assert.doesNotMatch(panels, /button\.board-card__origin|\.board-card__blocker\s*\{\s*min-height:\s*44px/)
  assert.doesNotMatch(panels, /\.board-card__origin--follow-up/)
})

test('every terminal action button shares the accent-outline component', () => {
  assert.match(action, /opensTerminal \? 'btn--terminal' : 'btn--primary'/)
  assert.match(runControl, /btn\.classList\.add\('btn--terminal'\)/)
  assert.match(runControl, /btn\.classList\.remove\('btn--terminal'\)/)
  assert.match(reviewer, /class: 'btn btn--sm btn--terminal'/)
  assert.match(figmaPanel, /class: 'btn btn--terminal', text: t\('figma\.action\.openTerminal'\)/)
  assert.match(skillsStatus, /class: 'btn btn--terminal', text: t\('skills\.fix\.terminal'\)/)
  assert.equal((taskActionBar.match(/btn--terminal/g) || []).length, 1)
  assert.doesNotMatch(panels, /\.run-control--active\s*\{[^}]*border-color/)
})

test('board consumes the summary DTO and typed action endpoint instead of rebuilding action rules', () => {
  assert.match(board, /loadResults: boardLoadResults\.load/)
  assert.match(boardLoadControllerSource,
    /dependencies\.loadResults\(dependencies\.getFilters\(\)\)/)
  assert.match(board,
    /loadSummary: function \(filters\) \{ return tasksApi\.loadTaskSummary\(filters\); \}/)
  assert.match(boardLoadResultsSource, /dependencies\.loadSummary\(filters\)/)
  assert.match(board,
    /executeAction: function \(stem, action, confirmation\) \{[\s\S]{0,600}?return tasksApi\.executeTaskAction\(stem, action, confirmation\);/)
  assert.match(boardTaskActionControllerSource,
    /dependencies\.executeAction\(row\.stem, action, confirmation\)/)
  assert.match(boardTaskActionControllerSource,
    /onConfirm: function \(\) \{ execute\(row, action, button, true, options\); \}/)
  assert.doesNotMatch(board, /function makeLegacyCard/)
  assert.match(api, /\/api\/tasks\/summary/)
  assert.match(api, /\/api\/tasks\/actions/)
  assert.match(api, /\/api\/tasks\/file/)
  assert.doesNotMatch(api, /\.\.\/tasks\//)
  assert.doesNotMatch(api, /function loadIndex\(/)
  assert.match(api, /expectedSourceRevision/)
  assert.match(http, /inspected\.operation === 'enqueue'/)
  assert.match(http, /task-action-operation-invalid/)
  assert.match(router, /function openTarget\(target\)/)
  assert.match(board,
    /openPanel: function \(target\) \{ return router\.openTarget\(target\); \}/)
  assert.match(boardTaskNavigationControllerSource,
    /dependencies\.openCard\(\s*found\.folder,\s*found\.item\.stem,\s*found\.item,\s*target\.section \|\| null/)
  assert.match(board, /import \{ createTaskDetails \} from '\.\.\/board\/task-details\.js'/)
  assert.match(board, /function openTaskDetails\(stem, preferredSection, fallbackItem, deepTarget\)/)
  assert.match(board, /openTaskDetails\(stem, section \|\| 'overview', item, deepTarget \|\| null\)/)
  assert.match(taskDetails, /const SECTIONS = \['overview', 'activity', 'artifacts', 'advanced'\]/)
  assert.equal((taskActionBar.match(/data-task-details-primary/g) || []).length, 1)
  assert.doesNotMatch(board, /enqueueRequest\('(drop|reopen)'/)
  assert.doesNotMatch(board, /function (drop|reopen)Prompt/)
  assert.match(board, /executeTaskAction\(stem, typedAction/)
  assert.match(board, /tasksApi\.executeTaskAction\(stem, action, null, input\)/)
  assert.match(board, /tasksApi\.resumeFinalization\(finalization\)/)
  assert.match(card, /import \{ taskCardAction \} from '\.\/task-card-action\.js'/)
  assert.match(action, /else if \(typeof options\.onNavigate === 'function'\) options\.onNavigate\(action\.target, row, action\)/)
  assert.match(boardTaskNavigationControllerSource,
    /if \(target\.type === 'terminal' && target\.key\) \{\s*dependencies\.openTerminal\(target\.key\);\s*return;/)
  assert.equal(en['board.action.open_terminal'], 'Open terminal')
  assert.equal(ru['board.action.open_terminal'], 'Открыть терминал')
  assert.equal(en['board.action.fix_design'], 'Fix Design')
  assert.equal(ru['board.action.fix_design'], 'Исправить Design')
  assert.equal(en['board.action.continue_live'], 'Send answer')
  assert.equal(ru['board.action.continue_live'], 'Отправить ответ')
  assert.doesNotMatch(board, /function open(Backlog|Pending|Todo|Done)Modal\(/)
  assert.doesNotMatch(board, /tasksApi\.enqueueRequest/)
  assert.doesNotMatch(api, /function enqueueRequest\(/)
  assert.doesNotMatch(api, /enqueueRequest:\s*enqueueRequest/)
  assert.doesNotMatch(board, /function (prep|run)Prompt\(/)
  assert.doesNotMatch(board, /taskRunControl/)
  assert.doesNotMatch(board, /action === 'advanced'/)
  assert.match(board, /clipboard\.toast\(t\('board\.overflow\.preparing_prompt'\)\)/)
  assert.match(api, /function loadTaskActionPrompt\(stem, action, input\)/)
  assert.match(http, /taskActionPromptPreviewMod\.buildAnswers/)
  assert.ok((board.match(/clipboard\.copy\(response\.text\)/g) || []).length >= 1)
  assert.doesNotMatch(board + panels, /board-card__badge/)
  assert.match(taskDetails, /renderTaskActivity/)
  assert.match(taskDetails, /renderTaskArtifacts/)
})

test('runtime status has one server-owned projection and no legacy board classifier', () => {
  assert.match(taskSummarySource,
    /runtimeStatus:\s*\{\s*state: finalization \? 'finalizing' : liveAwaiting \? 'awaiting' :\s*request \? \(attached \? 'queued' : 'waiting-runner'\) :\s*active \? 'running' : stopped \? 'stopped' : 'idle'/)
  assert.match(card, /row\.runtimeStatus && row\.runtimeStatus\.state === 'waiting-runner'/)
  assert.doesNotMatch(board,
    /function (?:lockStageMatchesSession|observedSessionForLock|runStatusForLock)\(/)
  assert.doesNotMatch(board, /STALE_LOCK_DRAINER_MS/)
})

test('Figma screen capture exposes exact authoring failures before starting a session', () => {
  assert.match(figmaTaskReadModelSource, /function blockingDesignIssues\(stem\)/)
  assert.match(figmaTaskReadModelSource,
    /return issues\.captureBlocked === true \? issues : null/)
  assert.match(board,
    /if \(figmaTaskReadModel\.blockingDesignIssues\(stem\)\) return/)
  assert.match(board, /onError: function \(error\) \{\s*clipboard\.toastError\(figmaActionError\(error\)\);\s*return true;/)
  assert.match(en['figma.requestError.TOKEN_TASK_DESIGN_INVALID'], /A-Z, a-z, 0-9/)
  assert.match(ru['figma.requestError.TOKEN_TASK_DESIGN_INVALID'], /A-Z, a-z, 0-9/)
})

test('the primary todo Run uses the non-blocking missing-screens choice', () => {
  assert.match(board,
    /needsUnpulledScreens: function \(stem\) \{\s*return figmaTaskReadModel\.needsUnpulledScreens\(stem\);/)
  assert.match(boardTaskActionControllerSource,
    /if \(action\.kind === 'run' && dependencies\.needsUnpulledScreens\(row\.stem\)\)/)
  assert.match(boardTaskActionControllerSource,
    /dependencies\.confirmScreensBeforeRun\(row\.stem\)\.then\(function \(allowed\)/)
  assert.match(boardTaskActionControllerSource,
    /if \(allowed\) execute\(row, action, button, null, options\)/)
})

test('task-list controls preserve explicit filters and menu ownership across refreshes', () => {
  const store = readFileSync(join(scripts, 'board', 'task-list-store.js'), 'utf8')
  assert.match(store, /search: '', column: '', origin: '', blocker: '', dependency: '', context: '',/)
  assert.match(boardToolbarSource, /filterSelect\('column', 'board\.filter\.state'/)
  assert.match(boardToolbarSource, /filterSelect\('dependency', 'board\.filter\.dependency'/)
  assert.equal(uk['board.filter.dependency'], 'Залежності')
  assert.doesNotMatch(store, /needsAction/)
  assert.doesNotMatch(board, /board-toolbar__(check|choice|reload|result-count)|board\.filter\.needsAction|board\.filter\.results|board\.reload/)
  for (const dictionary of [en, ru]) {
    for (const key of [
      'board.filter.needsAction',
      'board.filter.results',
      'board.reload',
      'board.reloadTooltip',
    ]) {
      assert.equal(Object.hasOwn(dictionary, key), false, key)
    }
  }
  assert.match(panels, /\.board-toolbar\s*\{[\s\S]*?flex-wrap:\s*nowrap;[\s\S]*?overflow-x:\s*auto;/)
  assert.doesNotMatch(panels, /board-toolbar__(check|choice|reload|result-count)/)
  assert.match(store, /openMenuStem/)
  assert.match(boardViewportStateSource, /function captureFocus\(\)/)
  assert.match(boardViewportStateSource, /function restoreFocus\(snapshot\)/)
  assert.match(modalController,
    /card\.querySelector\('\[data-task-control="details"\]:not\(\[disabled\]\)'\)/)
  assert.match(modalController, /if \(document\.activeElement === cardControl\) return/)
  assert.match(boardToolbarSource, /var toolbarEl = null/)
  assert.match(boardRenderControllerSource, /child !== toolbarElement/)
  assert.match(boardToolbarSource, /toolbarEl\.parentNode === sectionEl/)
  assert.match(boardViewportStateSource,
    /if \(target === documentNode\.activeElement\) return/)
  assert.doesNotMatch(board, /clear\(sectionEl\);/)
  assert.doesNotMatch(board, /function clear\(node\)/)
  assert.match(boardHealthSource, /aria-live': 'polite'/)
  assert.doesNotMatch(board, /state\.summary\.nextCursor/)
  assert.match(boardPaginationControllerSource, /state\.summary\.nextCursor/)
  assert.match(boardPaginationControllerSource, /function loadMore\(\)/)
  assert.match(boardHealthSource, /function boardHealthIssues\(storeState\)/)
  assert.match(boardHealthSource, /state\.summary && Array\.isArray\(state\.summary\.limitations\)/)
  assert.doesNotMatch(board, /function renderSummaryLimitations\(\)/)
  assert.match(boardPaginationControllerSource, /page\.revision !== revision/)
  assert.match(sse, /task-journal:' \+ tasksLogMod\.revision\(\)/)
  assert.match(sse, /broadcast\('change', \{ t: Date\.now\(\) \}\)/)
})
