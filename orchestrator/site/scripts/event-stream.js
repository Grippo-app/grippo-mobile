var source = null;

function connect() {
  if (source) return source;
  if (typeof EventSource === 'undefined') return null;
  try {
    source = new EventSource('/api/events');
  } catch (error) {
    source = null;
  }
  return source;
}

function on(name, handler) {
  if (typeof name !== 'string' || !name || typeof handler !== 'function') return function () {};
  var active = connect();
  if (!active) return function () {};
  active.addEventListener(name, handler);
  var subscribed = true;
  return function unsubscribe() {
    if (!subscribed) return;
    subscribed = false;
    active.removeEventListener(name, handler);
  };
}

function connection() {
  return { readyState: source ? source.readyState : -1 };
}

export const siteEvents = {
  on: on,
  connection: connection
};
