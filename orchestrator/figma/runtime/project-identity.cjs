'use strict';

var fs = require('fs');
var path = require('path');

var GIT_META_MAX = 4096;

function unsafe(detail, relativePath) {
  var error = new Error('ADAPTER_INPUT_SNAPSHOT_UNSAFE: ' + String(detail || '').slice(0, 500));
  error.name = 'TypedError';
  error.code = 'ADAPTER_INPUT_SNAPSHOT_UNSAFE';
  error.detail = String(detail || '').slice(0, 500);
  error.path = String(relativePath || '').slice(0, 300);
  error.retryable = false;
  return error;
}

function lstatRequired(file, label) {
  var stat;
  try { stat = fs.lstatSync(file); } catch (error) {
    throw unsafe(label + ' unreadable: ' + (error && (error.code || error.message)), label);
  }
  if (stat.isSymbolicLink()) throw unsafe(label + ' is a symlink', label);
  return stat;
}

function boundedRegularText(file, label) {
  var stat = lstatRequired(file, label);
  if (!stat.isFile() || String(stat.nlink) !== '1') throw unsafe(label + ' is not a single-link regular file', label);
  if (stat.size > GIT_META_MAX) throw unsafe(label + ' exceeds ' + GIT_META_MAX + ' bytes', label);
  var bytes;
  try { bytes = fs.readFileSync(file); } catch (error) {
    throw unsafe(label + ' unreadable: ' + (error && (error.code || error.message)), label);
  }
  if (bytes.length !== stat.size) throw unsafe(label + ' changed during read', label);
  return bytes.toString('utf8').trim();
}

function parseHead(value) {
  var ref = /^ref:\s+(refs\/(?:heads|tags|remotes)\/(.+))$/.exec(value);
  var forbidden = ref && Array.from(ref[2]).some(function (character) {
    return character.charCodeAt(0) <= 32 || '~^:?*[\\'.indexOf(character) >= 0 || character === ']';
  });
  if (ref && !forbidden && ref[1].indexOf('..') < 0 && ref[1].indexOf('@{') < 0 && !ref[1].endsWith('/') &&
      !ref[1].split('/').some(function (segment) { return !segment || segment.startsWith('.') || segment.endsWith('.lock'); })) {
    return ref[1];
  }
  if (/^[a-f0-9]{40}(?:[a-f0-9]{24})?$/.test(value)) return value;
  throw unsafe('git HEAD is malformed', '.git/HEAD');
}

function readHead(gitDirectory) {
  var stat = lstatRequired(gitDirectory, '.git directory');
  if (!stat.isDirectory()) throw unsafe('gitdir target is not a directory', '.git');
  return parseHead(boundedRegularText(path.join(gitDirectory, 'HEAD'), '.git/HEAD'));
}

function projectBranchKey(projectRoot) {
  var root = path.resolve(projectRoot);
  var gitPath = path.join(root, '.git');
  var stat;
  try { stat = fs.lstatSync(gitPath); } catch (error) {
    if (error && error.code === 'ENOENT') return 'none';
    throw unsafe('.git unreadable: ' + (error && (error.code || error.message)), '.git');
  }
  if (stat.isSymbolicLink()) throw unsafe('.git is a symlink', '.git');
  if (stat.isDirectory()) return readHead(gitPath);
  if (!stat.isFile() || String(stat.nlink) !== '1' || stat.size > GIT_META_MAX) {
    throw unsafe('.git is not a bounded single-link directory or worktree pointer', '.git');
  }
  var pointer = boundedRegularText(gitPath, '.git');
  var match = /^gitdir:\s+([^\u0000-\u001f]+)$/.exec(pointer);
  if (!match || !match[1].trim()) throw unsafe('.git worktree pointer is malformed', '.git');
  var target = path.isAbsolute(match[1]) ? path.resolve(match[1]) : path.resolve(root, match[1]);
  return readHead(target);
}

module.exports = { projectBranchKey: projectBranchKey };
