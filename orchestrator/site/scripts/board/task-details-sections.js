const BASE_SECTIONS = Object.freeze(['overview', 'activity', 'artifacts', 'advanced']);

function hasActionSection(details) {
  return !!(details && details.primaryAction && details.primaryAction.attentionRequired === true);
}

export function taskDetailsSections(details) {
  return hasActionSection(details)
    ? ['action'].concat(BASE_SECTIONS)
    : BASE_SECTIONS.slice();
}

export function normalizeTaskDetailsSection(details, section) {
  const sections = taskDetailsSections(details);
  if (section === 'questions') return sections[0] === 'action' ? 'action' : 'overview';
  if (section === 'dependencies') return 'overview';
  if (section === 'validation') return sections[0] === 'action' ? 'action' : 'overview';
  return sections.indexOf(section) >= 0 ? section : 'overview';
}

export function initialTaskDetailsSection(details, preferredSection) {
  if (preferredSection == null || preferredSection === '') {
    return hasActionSection(details) ? 'action' : 'overview';
  }
  return normalizeTaskDetailsSection(details, preferredSection);
}
