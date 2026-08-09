import { dom } from './dom.js';
import { i18n } from './i18n.js';
import { confirmDialog } from './ui-dialog.js';

var el = dom.el;
function t(key, params) { return i18n.t(key, params); }
function overallLabel(value) {
  var key = 'appRun.overall.' + value;
  var translated = t(key);
  return translated === key ? t('appRun.overall.unknown') : translated;
}

function open(options) {
  var dialog = el('dialog', { class: 'app-run-validation', attrs: { 'aria-labelledby': 'app-run-validation-title' } });
  var title = el('h2', { id: 'app-run-validation-title', text: t('appRun.validationTitle') });
  var close = el('button', { type: 'button', class: 'btn btn--ghost btn--small', text: t('appRun.close') });
  var body = el('div', { class: 'app-run-validation__body' }, [el('p', { text: t('appRun.loading') })]);
  dialog.appendChild(el('div', { class: 'app-run-dialog__head' }, [title, close]));
  dialog.appendChild(body);
  document.body.appendChild(dialog);
  // A save in flight is already being written server-side, so closing over it would
  // hide the receipt outcome the user just produced. `closed` additionally stops
  // every continuation from writing into a node that left the document.
  var closed = false, saving = false;
  function finish() {
    if (saving) return;
    closed = true;
    try { dialog.close(); } catch (_) {}
    dialog.remove();
  }
  close.addEventListener('click', finish);
  dialog.addEventListener('cancel', function (event) { event.preventDefault(); finish(); });
  dialog.showModal();
  options.load().then(function (data) {
    if (closed) return;
    body.replaceChildren();
    var previous = Object.create(null);
    if (data.latestReceipt) {
      body.appendChild(el('p', {
        class: 'app-run-hint',
        text: t('appRun.latestValidation', {
          result: overallLabel(data.latestReceipt.overall),
          date: new Date(data.latestReceipt.createdAt).toLocaleString()
        })
      }));
      if (data.latestReceipt.staleSource || data.latestReceipt.staleTask) {
        body.appendChild(el('p', { class: 'banner banner--warn', text: t('appRun.validationStale') }));
      }
      // Reopening to amend one item must not reset the rest: a save always submits
      // every row, so rebuilding them as `not-tested` with an empty note rewrites the
      // durable receipt and drops the results and notes that were already recorded.
      // Item ids are only comparable while the task revision is unchanged, so a
      // stale-task receipt describes a different checklist and is not carried over.
      if (!data.latestReceipt.staleTask) {
        (data.latestReceipt.checklist || []).forEach(function (row) {
          previous[row.itemId] = row;
        });
      }
    }
    if (!data.items || !data.items.length) {
      body.appendChild(el('p', { class: 'app-run-hint', text: t('appRun.noManualChecks') }));
    }
    var rows = [];
    (data.items || []).forEach(function (item) {
      var saved = previous[item.itemId] || null;
      var result = el('select', {
        class: 'input', attrs: { 'aria-label': t('appRun.validationResult') }
      }, [
        el('option', { value: 'not-tested', text: t('appRun.notTested') }),
        el('option', { value: 'pass', text: t('appRun.pass') }),
        el('option', { value: 'fail', text: t('appRun.fail') })
      ]);
      var note = el('textarea', {
        class: 'input',
        attrs: {
          rows: '2', maxlength: '1000', placeholder: t('appRun.note'),
          'aria-label': t('appRun.note')
        }
      });
      var screenshotStatus = el('span', {
        class: 'app-run-validation__screenshot-status',
        attrs: { 'aria-live': 'polite' },
        text: ''
      });
      var addScreenshot = el('button', {
        type: 'button', class: 'btn btn--small', text: t('appRun.addScreenshot')
      });
      var row = el('fieldset', { class: 'app-run-validation__item' }, [
        el('legend', { text: item.text }),
        item.notes && item.notes.length ? el('ul', {}, item.notes.map(function (text) { return el('li', { text: text }); })) : null,
        result, note, el('div', { class: 'app-run-validation__screenshot' }, [addScreenshot, screenshotStatus])
      ]);
      var rowState = { item: item, result: result, note: note, screenshotIds: [] };
      if (saved) {
        result.value = saved.result;
        note.value = saved.note || '';
        // Screenshot ids are checked against the saving session, so ids from an
        // earlier session would be rejected on submit; only same-session ones carry.
        if (data.eligibleSession && data.latestReceipt.sessionId === data.eligibleSession.sessionId) {
          rowState.screenshotIds = (saved.screenshotIds || []).slice(0, 10);
          if (rowState.screenshotIds.length) {
            screenshotStatus.textContent = t('appRun.screenshotAdded', { count: rowState.screenshotIds.length });
          }
        }
      }
      addScreenshot.disabled = !data.eligibleSession || rowState.screenshotIds.length >= 10;
      addScreenshot.addEventListener('click', function () {
        if (rowState.screenshotIds.length >= 10) return;
        addScreenshot.disabled = true;
        options.capture().then(function (capture) {
          rowState.screenshotIds.push(capture.screenshot.screenshotId);
          screenshotStatus.textContent = t('appRun.screenshotAdded', { count: rowState.screenshotIds.length });
          addScreenshot.disabled = rowState.screenshotIds.length >= 10;
        }).catch(function (error) {
          screenshotStatus.textContent = options.error(error);
          addScreenshot.disabled = false;
        });
      });
      rows.push(rowState);
      body.appendChild(row);
    });
    var message = el('p', { class: 'app-run-validation__message', attrs: { 'aria-live': 'polite' } });
    var save = el('button', { type: 'button', class: 'btn btn--primary', text: t('appRun.saveValidation') });
    var saveAllowed = !!(data.eligibleSession) && rows.length > 0;
    save.disabled = !saveAllowed;
    // A failure must not read like a success. Errors take the same warn banner the
    // load-error path uses, announce assertively, and scroll themselves into view
    // from under a long checklist.
    function setMessage(text, isError) {
      message.className = 'app-run-validation__message' + (isError ? ' banner banner--warn' : '');
      message.setAttribute('aria-live', isError ? 'assertive' : 'polite');
      if (isError) message.setAttribute('role', 'alert');
      else message.removeAttribute('role');
      message.textContent = text;
      message.scrollIntoView({ block: 'nearest' });
    }
    // Close is held only while a write is in flight; Save comes back once the write
    // settles, because amending the checklist and saving again is accepted (the
    // revision is derived from the task source and its items, not from the receipt).
    function setBusy(active) {
      saving = active;
      save.disabled = active || !saveAllowed;
      close.disabled = active;
    }
    if (!data.eligibleSession) {
      body.appendChild(el('p', { class: 'app-run-hint', text: t('appRun.validationNeedsSession') }));
    }
    save.addEventListener('click', function () {
      if (rows.some(function (row) {
        return new TextEncoder().encode(row.note.value).length > 1000;
      })) {
        setMessage(t('appRun.noteTooLong'), true);
        return;
      }
      setBusy(true);
      var payload = {
        taskStem: data.taskStem,
        expectedTaskSourceRevision: data.taskSourceRevision,
        sessionId: data.eligibleSession.sessionId,
        expectedSessionRevision: data.eligibleSession.sessionRevision,
        validationRevision: data.validationRevision,
        items: rows.map(function (row) {
          return { itemId: row.item.itemId, result: row.result.value, note: row.note.value || null, screenshotIds: row.screenshotIds };
        }),
        acknowledgeStaleTask: false,
        idempotencyKey: options.idempotency()
      };
      function submit(value) {
        return options.save(value).catch(function (error) {
          // The acknowledgement is answered a turn later, so the rethrow follows it
          // into the .then: a declined acknowledgement must still reject with the
          // original error — that rejection is what releases the busy state and puts
          // the stale-task reason in the message banner.
          if (error.confirmationRequired !== true) throw error;
          return confirmDialog({
            title: t('appRun.saveValidation'),
            message: t('appRun.confirmStaleTask')
          }).then(function (accepted) {
            if (!accepted) throw error;
            return options.save(Object.assign({}, value, {
              acknowledgeStaleTask: true,
              idempotencyKey: options.idempotency()
            }));
          });
        });
      }
      submit(payload).then(function (result) {
        if (closed) return;
        setBusy(false);
        var text = t('appRun.validationSaved', {
          result: overallLabel(result.receipt.overall)
        });
        if (result.receipt.staleSource || result.receipt.staleTask) {
          text += ' · ' + t('appRun.validationStale');
        }
        if (result.journalRecorded === false) {
          text += ' · ' + t('appRun.validationJournalUnavailable');
        }
        setMessage(text, false);
      }).catch(function (error) {
        if (closed) return;
        setBusy(false);
        setMessage(options.error(error), true);
      });
    });
    body.appendChild(message); body.appendChild(save);
  }).catch(function (error) {
    if (closed) return;
    body.replaceChildren(el('p', { class: 'banner banner--warn', text: options.error(error) }));
  });
}

export const appRunValidation = { open: open };
