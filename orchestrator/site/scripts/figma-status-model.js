var FEATURE_STATES = Object.freeze({
  enabled: 1,
  disabled: 1,
  'restart-required': 1,
  invalid: 1
});
var CONNECTOR_STATES = Object.freeze({
  connected: 1,
  'needs-auth': 1,
  'local-absent': 1,
  misconfigured: 1,
  'cli-missing': 1,
  unknown: 1
});

export function figmaFeatureState(feature) {
  if (feature == null) return null;
  return Object.prototype.hasOwnProperty.call(FEATURE_STATES, feature.state)
    ? feature.state : 'invalid';
}

export function figmaConnectorState(connector) {
  if (!connector || !connector.state) return 'unknown';
  return Object.prototype.hasOwnProperty.call(CONNECTOR_STATES, connector.state)
    ? connector.state : 'unknown';
}

export function figmaFeaturePresentation(feature) {
  var state = figmaFeatureState(feature);
  if (state === null || state === 'enabled') {
    return { overridesConnector: false, state: state, canRecheck: true };
  }
  return {
    overridesConnector: true,
    state: state,
    displayState: state,
    labelKey: 'figma.pill.' + state,
    valueKey: 'figma.pop.feature.' + state,
    hintKey: 'figma.pop.featureHint.' + state,
    canRecheck: false
  };
}
