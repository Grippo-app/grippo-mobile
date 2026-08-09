'use strict';

// Runtime authority for test-capability-inventory.schema.json. Gradle owns
// production of this artifact; Node owns strict shape/hash validation and the
// exact allowlist of executable task paths consumed by certification.

const crypto = require('crypto');

const DOMAIN = 'test-capability-inventory';
const HASH_RE = /^sha256:[0-9a-f]{64}$/;
const MODULE_RE = /^:[A-Za-z0-9._-]+(?::[A-Za-z0-9._-]+)*$/;
const TASK_RE = /^:[A-Za-z0-9._-]+(?::[A-Za-z0-9._-]+)*:[A-Za-z0-9]+$/;
const CAPABILITIES = new Set(['base', 'compose-ui', 'coroutines', 'coverage', 'di', 'flow', 'network', 'room', 'screenshot']);
const LANES = new Set(['android-device', 'host', 'ios-simulator', 'screenshot']);

class TestCapabilityError extends Error {
  constructor(code, message) {
    super(code + ': ' + message);
    this.name = 'TestCapabilityError';
    this.code = code;
  }
}

function fail(code, message) { throw new TestCapabilityError(code, message); }
function sha256(value) { return 'sha256:' + crypto.createHash('sha256').update(value).digest('hex'); }
function canonicalJson(value) {
  if (value === null || typeof value !== 'object') return JSON.stringify(value);
  if (Array.isArray(value)) return '[' + value.map(canonicalJson).join(',') + ']';
  return '{' + Object.keys(value).sort().map((key) => JSON.stringify(key) + ':' + canonicalJson(value[key])).join(',') + '}';
}
function inventoryHashOf(inventory) {
  const model = {};
  for (const key of Object.keys(inventory)) if (key !== 'inventoryHash') model[key] = inventory[key];
  return sha256(DOMAIN + '\0' + canonicalJson(model));
}

function exactKeys(value, expected, label) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) fail('INVENTORY_INVALID', label + ' must be an object');
  const actual = Object.keys(value).sort();
  const wanted = [...expected].sort();
  if (actual.length !== wanted.length || actual.some((key, index) => key !== wanted[index])) {
    fail('INVENTORY_INVALID', label + ' keys must be exactly ' + wanted.join(','));
  }
}

function validateInventory(inventory) {
  exactKeys(inventory, ['version', 'domain', 'generatedBy', 'modules', 'inventoryHash'], 'inventory');
  if (inventory.version !== 1 || inventory.domain !== DOMAIN) fail('INVENTORY_INVALID', 'unsupported inventory contract');
  if (!/^:[A-Za-z0-9._:-]*[A-Za-z0-9]$/.test(String(inventory.generatedBy))) {
    fail('INVENTORY_INVALID', 'generatedBy task path grammar');
  }
  if (!Array.isArray(inventory.modules) || inventory.modules.length > 2000) fail('INVENTORY_INVALID', 'modules bounds');
  let previous = null;
  for (const module of inventory.modules) {
    exactKeys(module, ['path', 'capabilities', 'lanes'], 'module');
    if (!MODULE_RE.test(String(module.path))) fail('INVENTORY_INVALID', 'module path grammar');
    if (previous !== null && !(previous < module.path)) fail('INVENTORY_INVALID', 'modules must be strictly sorted');
    previous = module.path;
    if (!Array.isArray(module.capabilities) || module.capabilities.length > 16 ||
        new Set(module.capabilities).size !== module.capabilities.length) fail('INVENTORY_INVALID', 'capabilities bounds');
    for (const capability of module.capabilities) {
      if (!CAPABILITIES.has(capability)) fail('INVENTORY_INVALID', 'unknown capability: ' + capability);
    }
    if (Object.keys(module.lanes || {}).length > 0 && !module.capabilities.includes('base')) {
      fail('INVENTORY_INVALID', 'configured lanes require the base capability');
    }
    if (!module.lanes || typeof module.lanes !== 'object' || Array.isArray(module.lanes)) {
      fail('INVENTORY_INVALID', 'lanes must be an object');
    }
    for (const [laneName, lane] of Object.entries(module.lanes)) {
      if (!LANES.has(laneName)) fail('INVENTORY_INVALID', 'unknown lane: ' + laneName);
      if (!lane || typeof lane !== 'object' || Array.isArray(lane)) fail('INVENTORY_INVALID', 'lane must be an object');
      const keys = Object.keys(lane).sort();
      if (!keys.includes('taskPath') || keys.some((key) => !['compilation', 'sourceSet', 'taskPath'].includes(key))) {
        fail('INVENTORY_INVALID', 'lane keys');
      }
      if (!TASK_RE.test(String(lane.taskPath))) fail('INVENTORY_INVALID', 'lane task path grammar');
      for (const key of ['sourceSet', 'compilation']) {
        if (key in lane && (typeof lane[key] !== 'string' || lane[key].length > 100)) {
          fail('INVENTORY_INVALID', 'lane ' + key + ' grammar');
        }
      }
    }
  }
  if (!HASH_RE.test(String(inventory.inventoryHash)) || inventoryHashOf(inventory) !== inventory.inventoryHash) {
    fail('HASH_MISMATCH', 'inventoryHash does not match inventory content');
  }
  return Object.freeze(JSON.parse(JSON.stringify(inventory)));
}

function allowedTaskPaths(inventory) {
  const valid = validateInventory(inventory);
  return [...new Set(valid.modules.flatMap((module) => Object.values(module.lanes).map((lane) => lane.taskPath)))].sort();
}

module.exports = { DOMAIN, TestCapabilityError, canonicalJson, inventoryHashOf, validateInventory, allowedTaskPaths };
