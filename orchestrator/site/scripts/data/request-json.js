function typedError(code, status, body) {
  var error = new Error(String(code || 'request-failed'));
  error.code = String(code || 'request-failed');
  error.kind = error.code;
  error.status = Number.isInteger(status) ? status : 0;
  if (body && typeof body === 'object') {
    if (body.integrity && typeof body.integrity === 'object') error.integrity = body.integrity;
    if (body.active && typeof body.active === 'object') error.active = body.active;
    if (typeof body.reasonCode === 'string') error.reasonCode = body.reasonCode;
    // A mutation that landed before its lease failed to settle must not be
    // reported to the user as a failure.
    if (body.recovered === true) error.recovered = true;
    if (body.confirmation && typeof body.confirmation === 'object') error.confirmation = body.confirmation;
    if (body.current && typeof body.current === 'object') error.current = body.current;
    if (body.confirmationRequired === true) error.confirmationRequired = true;
  }
  return error;
}

function responseCode(response, body) {
  if (body && typeof body.error === 'string' && body.error) return body.error;
  return response.status === 404 ? 'not-found' : 'http-error';
}

export function requestJson(url, options) {
  var requestOptions = Object.assign({}, options || {});
  var timeoutMs = Number.isInteger(requestOptions.timeoutMs) && requestOptions.timeoutMs > 0
    ? requestOptions.timeoutMs : 0;
  delete requestOptions.timeoutMs;
  var controller = null;
  if (timeoutMs && !requestOptions.signal && typeof AbortController !== 'undefined') {
    controller = new AbortController();
    requestOptions.signal = controller.signal;
  }
  var pending;
  try {
    pending = fetch(url, requestOptions);
  } catch (error) {
    return Promise.reject(typedError('fetch-failed', 0, null));
  }
  var parsed = pending.then(function (response) {
    return response.text().then(function (text) {
      var body;
      try {
        body = JSON.parse(text);
      } catch (error) {
        throw typedError('invalid-response', response.status, null);
      }
      if (!response.ok || body && body.ok === false) {
        throw typedError(responseCode(response, body), response.status, body);
      }
      return body;
    }, function () {
      throw typedError('invalid-response', response.status, null);
    });
  }, function () {
    throw typedError('fetch-failed', 0, null);
  });
  if (!timeoutMs) return parsed;
  var timer;
  var timedOut = new Promise(function (_resolve, reject) {
    timer = setTimeout(function () {
      if (controller) controller.abort();
      reject(typedError('fetch-failed', 0, null));
    }, timeoutMs);
  });
  return Promise.race([parsed, timedOut]).finally(function () { clearTimeout(timer); });
}

export function errorCode(error) {
  if (typeof error === 'string' && error) return error;
  if (error && typeof error.code === 'string' && error.code) return error.code;
  if (error && typeof error.kind === 'string' && error.kind) return error.kind;
  return 'unknown';
}
