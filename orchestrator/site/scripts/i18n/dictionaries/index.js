import coreEN from './core/en.js';
import coreRU from './core/ru.js';
import coreUK from './core/uk.js';
import designEN from './design/en.js';
import designRU from './design/ru.js';
import designUK from './design/uk.js';
import architectureEN from './architecture/en.js';
import architectureRU from './architecture/ru.js';
import architectureUK from './architecture/uk.js';
import setupEN from './setup/en.js';
import setupRU from './setup/ru.js';
import setupUK from './setup/uk.js';
import wizardEN from './wizard/en.js';
import wizardRU from './wizard/ru.js';
import wizardUK from './wizard/uk.js';
import reviewerEN from './reviewer/en.js';
import reviewerRU from './reviewer/ru.js';
import reviewerUK from './reviewer/uk.js';
import boardEN from './board/en.js';
import boardRU from './board/ru.js';
import boardUK from './board/uk.js';
import figmaIntegrationEN from './figma-integration/en.js';
import figmaIntegrationRU from './figma-integration/ru.js';
import figmaIntegrationUK from './figma-integration/uk.js';
import backendIntegrationEN from './backend-integration/en.js';
import backendIntegrationRU from './backend-integration/ru.js';
import backendIntegrationUK from './backend-integration/uk.js';
import liveStatusEN from './live-status/en.js';
import liveStatusRU from './live-status/ru.js';
import liveStatusUK from './live-status/uk.js';
import figmaStatusEN from './figma-status/en.js';
import figmaStatusRU from './figma-status/ru.js';
import figmaStatusUK from './figma-status/uk.js';
import apiEN from './api/en.js';
import apiRU from './api/ru.js';
import apiUK from './api/uk.js';
import appRunEN from './app-run/en.js';
import appRunRU from './app-run/ru.js';
import appRunUK from './app-run/uk.js';
import taskDetailsEN from './task-details/en.js';
import taskDetailsRU from './task-details/ru.js';
import taskDetailsUK from './task-details/uk.js';

import { mergeLocaleDictionaries } from './merge.js';

export const dictionaryDomains = Object.freeze([
  Object.freeze(['core', Object.freeze({ en: coreEN, ru: coreRU, uk: coreUK })]),
  Object.freeze(['design', Object.freeze({ en: designEN, ru: designRU, uk: designUK })]),
  Object.freeze(['architecture', Object.freeze({ en: architectureEN, ru: architectureRU, uk: architectureUK })]),
  Object.freeze(['setup', Object.freeze({ en: setupEN, ru: setupRU, uk: setupUK })]),
  Object.freeze(['wizard', Object.freeze({ en: wizardEN, ru: wizardRU, uk: wizardUK })]),
  Object.freeze(['reviewer', Object.freeze({ en: reviewerEN, ru: reviewerRU, uk: reviewerUK })]),
  Object.freeze(['board', Object.freeze({ en: boardEN, ru: boardRU, uk: boardUK })]),
  Object.freeze(['figma-integration', Object.freeze({ en: figmaIntegrationEN, ru: figmaIntegrationRU, uk: figmaIntegrationUK })]),
  Object.freeze(['backend-integration', Object.freeze({ en: backendIntegrationEN, ru: backendIntegrationRU, uk: backendIntegrationUK })]),
  Object.freeze(['live-status', Object.freeze({ en: liveStatusEN, ru: liveStatusRU, uk: liveStatusUK })]),
  Object.freeze(['figma-status', Object.freeze({ en: figmaStatusEN, ru: figmaStatusRU, uk: figmaStatusUK })]),
  Object.freeze(['api', Object.freeze({ en: apiEN, ru: apiRU, uk: apiUK })]),
  Object.freeze(['app-run', Object.freeze({ en: appRunEN, ru: appRunRU, uk: appRunUK })]),
  Object.freeze(['task-details', Object.freeze({ en: taskDetailsEN, ru: taskDetailsRU, uk: taskDetailsUK })]),
]);

const dictionaries = Object.freeze(Object.fromEntries(
  ['en', 'ru', 'uk'].map((locale) => [
    locale,
    mergeLocaleDictionaries(locale, dictionaryDomains)
  ])
));

export default dictionaries;
