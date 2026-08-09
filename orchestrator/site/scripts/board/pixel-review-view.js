export function createPixelReviewView(dependencies) {
  var el = dependencies.el;
  var t = dependencies.t;
  var prepareVerdict = dependencies.prepareVerdict;
  var submitVerdict = dependencies.submitVerdict;
  var onSubmitError = dependencies.onSubmitError;

  function buildPanel(stem, resp) {
    var review = resp && resp.pixelReview;
    if (!review || !Array.isArray(review.pending) || !review.pending.length) return null;
    var safeStem = String(stem || '').trim();
    if (!/^[A-Za-z0-9][A-Za-z0-9_-]*$/.test(safeStem)) return null;
    var wrap = el('div', { class: 'board-evidence__rerun board-evidence__review' });
    wrap.appendChild(el('p', {
      class: 'board-evidence__rerun-hint',
      text: t('board.figmaEvidence.review.hint', { count: review.pending.length })
    }));
    review.pending.forEach(function (row) {
      var line = el('div', {
        class: 'board-evidence__review-row',
        attrs: { style: 'display:flex;align-items:center;gap:8px;flex-wrap:wrap;margin:4px 0;' }
      });
      var name = row.screen +
        (String(row.theme || 'primary').toLowerCase() !== 'primary' ? ' [' + row.theme + ']' : '');
      line.appendChild(el('strong', { text: name }));
      if (row.renderClass) {
        line.appendChild(el('span', {
          class: 'board-evidence__review-class',
          attrs: { style: 'font-size:11px;opacity:.75;border:1px solid currentColor;border-radius:999px;padding:0 8px;' },
          text: row.renderClass
        }));
      }
      if (row.pixelStatus) {
        line.appendChild(el('span', {
          attrs: { style: 'font-size:11px;opacity:.6;' },
          text: t('board.figmaEvidence.review.raw', { status: row.pixelStatus })
        }));
      }
      var makeButton = function (verdict, key) {
        var button = el('button', {
          type: 'button',
          class: 'btn board-evidence__review-btn',
          text: t(key)
        });
        button.addEventListener('click', function () {
          // The note is collected in a real dialog now, so preparation is async.
          // The button stays locked across prepare AND submit: a verdict writes
          // a durable one-shot receipt and must not be double-fired.
          button.disabled = true;
          Promise.resolve(prepareVerdict(verdict, row)).then(function (prepared) {
            if (!prepared) { button.disabled = false; return null; }
            return Promise.resolve(submitVerdict(safeStem, row, verdict, prepared.note || ''))
              .then(function () { button.disabled = false; });
          }, function (error) {
            button.disabled = false;
            onSubmitError(error);
          }).catch(function (error) {
            button.disabled = false;
            onSubmitError(error);
          });
        });
        line.appendChild(button);
      };
      makeButton('pass', 'board.figmaEvidence.review.pass');
      makeButton('minor', 'board.figmaEvidence.review.minor');
      makeButton('fail', 'board.figmaEvidence.review.fail');
      wrap.appendChild(line);
    });
    if (Array.isArray(review.resolved) && review.resolved.length) {
      wrap.appendChild(el('p', {
        attrs: { style: 'font-size:12px;opacity:.7;' },
        text: t('board.figmaEvidence.review.resolved', { count: review.resolved.length })
      }));
    }
    return wrap;
  }

  return { buildPanel: buildPanel };
}
