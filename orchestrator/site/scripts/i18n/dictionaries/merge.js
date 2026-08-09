export function mergeLocaleDictionaries(locale, domains) {
  if (!['en', 'ru', 'uk'].includes(locale)) {
    throw new Error(`unsupported locale: ${String(locale)}`);
  }
  if (!Array.isArray(domains) || domains.length === 0) {
    throw new Error('locale domains must be a non-empty array');
  }
  const merged = Object.create(null);
  const domainNames = new Set();
  for (const item of domains) {
    if (!Array.isArray(item) || item.length !== 2) {
      throw new Error('locale domain entry must be [name, translations]');
    }
    const [domain, translations] = item;
    if (typeof domain !== 'string' || domain.length === 0 || domainNames.has(domain)) {
      throw new Error(`invalid or duplicate locale domain: ${String(domain)}`);
    }
    domainNames.add(domain);
    const dictionary = translations?.[locale];
    if (
      !translations ||
      typeof translations !== 'object' ||
      !Object.hasOwn(translations, locale) ||
      !dictionary ||
      typeof dictionary !== 'object' ||
      Array.isArray(dictionary)
    ) {
      throw new Error(`missing ${locale} dictionary for domain ${domain}`);
    }
    const entries = Object.entries(dictionary);
    if (entries.length === 0) {
      throw new Error(`empty ${locale} dictionary for domain ${domain}`);
    }
    for (const [key, value] of entries) {
      if (key.length === 0 || typeof value !== 'string') {
        throw new Error(`invalid ${locale} locale entry in domain ${domain}: ${key}`);
      }
      if (Object.hasOwn(merged, key)) {
        throw new Error(`duplicate ${locale} locale key: ${key}`);
      }
      merged[key] = value;
    }
  }
  return Object.freeze(merged);
}
