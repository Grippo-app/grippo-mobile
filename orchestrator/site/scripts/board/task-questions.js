import { dom } from '../dom.js';

const el = dom.el;

function clearQuestionError(field) {
  const error = field && field.querySelector('[data-question-error]');
  if (error) error.remove();
  Array.from(field ? field.querySelectorAll('[aria-invalid="true"]') : []).forEach(function (input) {
    input.removeAttribute('aria-invalid');
  });
}

export function taskQuestions(work, t, options) {
  const root = el('section', {
    class: 'task-details__current-work task-details__questions',
    attrs: { 'data-task-section': 'questions', 'aria-labelledby': 'task-details-questions-title' }
  });
  root.appendChild(el('h3', {
    id: 'task-details-questions-title',
    class: 'task-details__section-title',
    text: t('taskDetails.questions.title')
  }));
  // A durable escalation question can outlive the run that asked it. When the
  // stopped run still holds its lock, answering has to wait for the canonical
  // recovery, so say that here instead of leaving a form with a foreign CTA.
  if (options && options.notice) {
    root.appendChild(el('p', { class: 'banner banner--warn', text: t(options.notice) }));
  }
  const payload = work && work.questions;
  if (!payload || !payload.valid) {
    root.appendChild(el('p', {
      class: 'banner banner--warn',
      text: t('taskDetails.questions.unavailable')
    }));
    return { node: root, read: function () { return null; } };
  }
  if (!payload.questions.length) {
    root.appendChild(el('p', {
      class: 'task-details__empty',
      text: t('taskDetails.questions.empty')
    }));
  }
  payload.questions.forEach(function (question) {
    const field = el('fieldset', {
      class: 'task-details__question',
      attrs: { 'data-question-id': String(question.id) }
    });
    field.appendChild(el('legend', {
      class: 'task-details__question-title',
      text: 'Q' + question.id + ' — ' + question.text
    }));
    if (question.type === 'choice' || question.type === 'multiselect') {
      const inputType = question.type === 'choice' ? 'radio' : 'checkbox';
      question.options.forEach(function (option) {
        const label = el('label', { class: 'task-details__question-option' });
        const cfg = { type: inputType };
        const input = el('input', {
          type: cfg.type,
          class: 'choice-input',
          attrs: {
            name: 'task-details-question-' + question.id,
            value: option.id,
            'data-question-option': option.id
          }
        });
        if (question.answer && question.answer.split(/[,\s]+/).indexOf(option.id) >= 0) input.checked = true;
        label.appendChild(input);
        label.appendChild(el('span', { text: option.label || option.id }));
        field.appendChild(label);
      });
      const custom = el('textarea', {
        class: 'input task-details__question-text',
        attrs: {
          rows: '2',
          'data-question-text': String(question.id),
          placeholder: t('taskDetails.questions.custom'),
          'aria-label': t('taskDetails.questions.custom')
        }
      });
      const answeredIds = String(question.answer || '').split(/[,\s]+/).filter(Boolean);
      if (question.answer && !answeredIds.every(function (id) {
        return question.options.some(function (option) { return option.id === id; });
      })) custom.value = question.answer;
      custom.addEventListener('input', function () {
        if (!custom.value.trim()) return;
        Array.from(field.querySelectorAll('[data-question-option]')).forEach(function (input) {
          input.checked = false;
        });
        clearQuestionError(field);
      });
      Array.from(field.querySelectorAll('[data-question-option]')).forEach(function (input) {
        input.addEventListener('change', function () {
          if (input.checked) {
            custom.value = '';
            clearQuestionError(field);
          }
        });
      });
      field.appendChild(custom);
    } else {
      const input = el('textarea', {
        class: 'input task-details__question-text',
        attrs: {
          rows: '3',
          'data-question-text': String(question.id),
          'aria-label': question.text
        }
      });
      input.value = question.answer || '';
      input.addEventListener('input', function () {
        if (input.value.trim()) clearQuestionError(field);
      });
      field.appendChild(input);
    }
    root.appendChild(field);
  });
  return {
    node: root,
    read: function () {
      let firstInvalid = null;
      const answers = payload.questions.map(function (question) {
        const field = root.querySelector('[data-question-id="' + question.id + '"]');
        const textInput = field && field.querySelector('[data-question-text]');
        const text = textInput ? textInput.value.trim() : '';
        const selected = field ? Array.from(field.querySelectorAll('[data-question-option]:checked'))
          .map(function (input) { return input.value; }) : [];
        const priorError = field && field.querySelector('[data-question-error]');
        if (priorError) priorError.remove();
        if (textInput) textInput.removeAttribute('aria-invalid');
        Array.from(field ? field.querySelectorAll('[data-question-option]') : []).forEach(function (input) {
          input.removeAttribute('aria-invalid');
        });
        if (!text && !selected.length && field) {
          const error = el('p', {
            class: 'task-details__question-error',
            text: t('board.requestError.task-answer-invalid'),
            attrs: { 'data-question-error': 'true', role: 'alert' }
          });
          field.appendChild(error);
          const focusTarget = textInput || field.querySelector('[data-question-option]');
          if (focusTarget) {
            focusTarget.setAttribute('aria-invalid', 'true');
            if (!firstInvalid) firstInvalid = focusTarget;
          }
        }
        return { questionId: question.id, optionIds: text ? [] : selected, text: text };
      });
      if (firstInvalid) {
        firstInvalid.focus();
        return null;
      }
      return answers;
    },
    round: payload.round,
    revision: payload.revision
  };
}
