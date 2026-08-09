// Tree-sitter backed Kotlin parser adapter for the Figma comparison implementation model.
// This is syntax-aware evidence, not compiler semantic analysis.

import Parser from 'tree-sitter'
import KotlinPkg from '@tree-sitter-grammars/tree-sitter-kotlin'

const Kotlin = KotlinPkg.default || KotlinPkg

let parser = null

export function parserInfo() {
  return {
    engine: 'tree-sitter-kotlin',
    treeSitter: '0.22.x',
    grammar: KotlinPkg.name || 'tree-sitter-kotlin',
    grammarVersion: '1.1.x',
  }
}

function getParser() {
  if (parser) return parser
  parser = new Parser()
  parser.setLanguage(Kotlin)
  return parser
}

function nodeText(source, node) {
  return source.slice(node.startIndex, node.endIndex)
}

function collectParseErrors(root, source) {
  const errors = []
  const walk = (node) => {
    if (!node) return
    const missing = typeof node.isMissing === 'function' ? node.isMissing() : !!node.isMissing
    if (node.type === 'ERROR' || missing) {
      errors.push({
        type: node.type,
        missing,
        start: node.startPosition,
        end: node.endPosition,
        text: missing ? '' : nodeText(source, node).slice(0, 120),
      })
    }
    for (let i = 0; i < node.childCount; i++) walk(node.child(i))
  }
  walk(root)
  return errors
}

export function parseKotlinSource(source) {
  const tree = getParser().parse(String(source || ''))
  const hasError = typeof tree.rootNode.hasError === 'function' ? tree.rootNode.hasError() : !!tree.rootNode.hasError
  const parseErrors = collectParseErrors(tree.rootNode, String(source || ''))
  if (hasError && !parseErrors.length) {
    parseErrors.push({
      type: 'HAS_ERROR',
      missing: false,
      start: tree.rootNode.startPosition,
      end: tree.rootNode.endPosition,
      text: 'root has parse errors not exposed as named ERROR nodes',
    })
  }
  return {
    tree,
    root: tree.rootNode,
    hasError,
    parseErrors,
  }
}
