import { i18n } from '../i18n.js';
import dictionaries from './dictionaries/index.js';

// English source-of-truth dictionary. Domain ownership lives under ./dictionaries/.
i18n.register('en', dictionaries.en);
