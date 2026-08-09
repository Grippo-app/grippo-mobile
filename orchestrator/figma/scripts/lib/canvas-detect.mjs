// canvas-detect.mjs — classify a Compose widget source as "canvas" (drawn via Canvas/DrawScope).
//
// Why this exists: a Canvas/DrawScope widget cannot call the @Composable `AppTokens.*` accessor
// inside a non-composable draw lambda, so the builder hoists the token into a `val` outside the
// draw block (`val c = AppTokens.colors.group` … `c.leaf` inside drawPath). That hoist is what the
// evidence alias resolution in extract-app-tokens (resolveAliases) reconciles, so the spec gate
// treats canvas and declarative widgets identically. This classifier does NOT gate anything — it
// only LABELS a screen as canvas for the report's `widgetClasses` (audit trail / evidence-bundle
// `verifiedAs`).
//
// Pure library: no CLI, no side effects. Detection runs on MASKED source (comments/strings/char
// literals blanked) so a `Canvas` in a comment or string never trips the classifier.

import { maskKotlinTrivia } from '../extract-app-tokens.mjs'

// Signals that a widget paints imperatively rather than declaratively. Each is matched on masked
// source. Kept deliberately narrow: a false "canvas" only widens the spec fallback (which still
// has to be EARNED by the hoist probe) and never touches the pixel gate.
const CANVAS_SIGNALS = [
  [/\bCanvas\s*\(/, 'Canvas('],            // foundation Canvas composable
  [/\.drawBehind\s*\{/, 'drawBehind'],      // Modifier.drawBehind { ... }
  [/\.drawWithContent\s*\{/, 'drawWithContent'],
  [/\.drawWithCache\s*\{/, 'drawWithCache'],
  [/\bdrawIntoCanvas\b/, 'drawIntoCanvas'],
  [/\bDrawScope\b/, 'DrawScope'],           // DrawScope receiver / extension fun
]

// classifyWidgetSource(rawText) -> { canvas: boolean, signals: string[] }
// Accepts RAW Kotlin; masks internally (idempotent on already-masked text).
export function classifyWidgetSource(rawText) {
  const s = maskKotlinTrivia(String(rawText || ''))
  const signals = []
  for (const [re, name] of CANVAS_SIGNALS) if (re.test(s)) signals.push(name)
  return { canvas: signals.length > 0, signals }
}
