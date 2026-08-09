// Fixture self-test for extract-compose-model.mjs — no Figma, no Gradle.
import { mkdtempSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { tmpdir } from 'node:os'
import { fileURLToPath } from 'node:url'
import { execFileSync, spawnSync } from 'node:child_process'
import assert from 'node:assert/strict'
import { compileSchema } from '../scripts/report-utils.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const SCRIPT = join(HERE, '..', 'scripts', 'extract-compose-model.mjs')
const SCHEMA = join(HERE, '..', 'token-schemas', 'implementation-model.schema.json')
const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }

const ws = mkdtempSync(join(tmpdir(), 'compose-model-'))
try {
  const src = join(ws, 'src')
  mkdirSync(src, { recursive: true })
  const file = join(src, 'HomeScreen.kt')
  writeFileSync(file, `
    package app.home
    import androidx.compose.runtime.Composable

    @Composable
    fun HomeScreen(title: String = "Hi", enabled: Boolean = true) {
      Column {
        Text(title, color = AppTokens.colors.text.primary)
        PrimaryButton(enabled = enabled)
      }
    }
  `)
  const out = join(ws, 'model.json')
  execFileSync('node', [SCRIPT, '--file', file, '--out', out], { stdio: 'pipe' })
	  const model = JSON.parse(readFileSync(out, 'utf8'))
	  const validate = await compileSchema(SCHEMA, { gate: true })
	  check('implementation model validates against schema', () => assert.equal(validate(model), true, JSON.stringify(validate.errors || [])))
	  check('Tree-sitter model captures composable function', () => assert.ok(model.composables.find((c) => c.name === 'HomeScreen' && c.fqName === 'app.home.HomeScreen')))
	  check('Tree-sitter model captures Compose calls', () => assert.ok(model.calls.some((c) => c.callee === 'PrimaryButton')))
	  check('implementation model captures AppTokens usage', () => assert.ok(model.tokens.paths.includes('colors.text.primary')))
	  check('unknown component calls are explicit unresolved refs without semantics', () => {
	    assert.ok(model.unresolvedRefs.find((r) => r.kind === 'UNRESOLVED_COMPONENT_CALL' && r.callee === 'PrimaryButton'))
	  })

	  const componentFile = join(src, 'PrimaryButton.kt')
	  writeFileSync(componentFile, 'package app.home\n@Composable\nfun PrimaryButton(enabled: Boolean = true) {}\n')
	  const declaredOut = join(ws, 'declared-model.json')
	  execFileSync('node', [SCRIPT, '--file', file, '--file', componentFile, '--out', declaredOut], { stdio: 'pipe' })
	  const declaredModel = JSON.parse(readFileSync(declaredOut, 'utf8'))
	  check('an explicitly declared component source resolves its call without a legacy semantics reader', () => {
	    assert.equal(declaredModel.unresolvedRefs.some((r) => r.kind === 'UNRESOLVED_COMPONENT_CALL' && r.callee === 'PrimaryButton'), false)
	  })
	  const retired = spawnSync('node', [SCRIPT, '--file', file, '--component-semantics', 'retired.json', '--out', declaredOut], { encoding: 'utf8' })
	  check('the retired component-semantics option is rejected instead of dual-read', () => assert.notEqual(retired.status, 0))

  const helper = join(src, 'Helper.kt')
  writeFileSync(helper, `
    package app.home
    import androidx.compose.runtime.Composable

    @Composable
    fun HomeScreen() { Text("x") }

    fun Helper() { Text("not composable") }
  `)
  const helperOut = join(ws, 'helper-model.json')
  execFileSync('node', [SCRIPT, '--file', helper, '--out', helperOut], { stdio: 'pipe' })
  const helperModel = JSON.parse(readFileSync(helperOut, 'utf8'))
  check('plain PascalCase helper after @Composable is not marked composable', () => assert.deepEqual(helperModel.composables.map((c) => c.name), ['HomeScreen']))

  const bad = join(src, 'Broken.kt')
  writeFileSync(bad, 'package app.broken\n@Composable\nfun Broken( { Text("x") }\n')
  const badOut = join(ws, 'broken-model.json')
  execFileSync('node', [SCRIPT, '--file', bad, '--out', badOut], { stdio: 'pipe' })
  const badModel = JSON.parse(readFileSync(badOut, 'utf8'))
  check('parse errors are reported, not hidden', () => {
    assert.equal(badModel.overall, 'WARN')
    assert.ok(badModel.files[0].parseErrors.length > 0)
    assert.ok(badModel.unresolvedRefs.find((r) => r.kind === 'PARSE_ERROR'))
  })

  const missingParen = join(src, 'MissingParen.kt')
  writeFileSync(missingParen, 'package app.broken\nfun MissingParen(x: String { Text(x) }\n')
  const missingParenOut = join(ws, 'missing-paren-model.json')
  execFileSync('node', [SCRIPT, '--file', missingParen, '--out', missingParenOut], { stdio: 'pipe' })
  const missingParenModel = JSON.parse(readFileSync(missingParenOut, 'utf8'))
  check('root.hasError without named ERROR nodes is still reported', () => {
    assert.equal(missingParenModel.overall, 'WARN')
    assert.ok(missingParenModel.files[0].parseErrors.length > 0)
  })

	  const missingOut = join(ws, 'missing-model.json')
	  const missing = spawnSync('node', [SCRIPT, '--out', missingOut], { encoding: 'utf8' })
	  check('missing implementation inputs block extractor', () => assert.notEqual(missing.status, 0))

	  const advanced = join(src, 'Advanced.kt')
	  writeFileSync(advanced, `
	    package app.advanced
	    import androidx.compose.runtime.Composable

	    @Preview(showBackground = true)
	    @androidx.compose.runtime.Composable
	    private fun HiddenScreen() {
	      PrimaryButton()
	    }

	    @Composable
	    @Preview
	    fun <T> GenericScreen(item: T) {
	      GenericCard()
	    }

	    @Composable
	    fun RowScope.ItemScreen() {
	      Text("x")
	    }

	    @Preview(name = "Not a call")
	    class Color(v: Long)
	  `)
	  const advancedOut = join(ws, 'advanced-model.json')
	  execFileSync('node', [SCRIPT, '--file', advanced, '--out', advancedOut], { stdio: 'pipe' })
	  const advancedModel = JSON.parse(readFileSync(advancedOut, 'utf8'))
	  check('Compose model captures modifiers, previews, generics, and receiver composables', () => {
	    assert.deepEqual(advancedModel.composables.map((c) => c.name).sort(), ['GenericScreen', 'HiddenScreen', 'ItemScreen'])
	  })
	  check('Compose call extraction ignores annotations and non-body declarations', () => {
	    const callees = advancedModel.calls.map((c) => c.callee)
	    assert.ok(callees.includes('PrimaryButton'))
	    assert.ok(callees.includes('GenericCard'))
	    assert.ok(callees.includes('Text'))
	    assert.equal(callees.includes('Preview'), false)
	    assert.equal(callees.includes('Color'), false)
	    assert.equal(callees.includes('Composable'), false)
	  })

	  const missingRootOut = join(ws, 'missing-root-model.json')
	  const missingRoot = spawnSync('node', [SCRIPT, '--file', file, '--root', join(ws, 'missing-root'), '--out', missingRootOut], { encoding: 'utf8' })
	  const missingRootModel = JSON.parse(readFileSync(missingRootOut, 'utf8'))
	  check('missing implementation root blocks extractor even when another file is valid', () => {
	    assert.notEqual(missingRoot.status, 0)
	    assert.ok(missingRootModel.issues.find((i) => i.issueKind === 'IMPLEMENTATION_ROOT_MISSING'))
	  })

	  const deterministicA = join(ws, 'deterministic-a.json')
	  const deterministicB = join(ws, 'deterministic-b.json')
	  execFileSync('node', [SCRIPT, '--file', file, '--out', deterministicA], { stdio: 'pipe' })
	  execFileSync('node', [SCRIPT, '--file', file, '--out', deterministicB], { stdio: 'pipe' })
	  check('implementation model is deterministic for unchanged inputs', () => {
	    assert.equal(readFileSync(deterministicA, 'utf8'), readFileSync(deterministicB, 'utf8'))
	  })

	  const exprBody = join(src, 'ExpressionBody.kt')
	  writeFileSync(exprBody, `
	    package app.expr
	    import androidx.compose.runtime.Composable

	    @Composable
	    fun ExpressionScreen() =
	      Column {
	        Text("x")
	        PrimaryButton()
	      }
	  `)
	  const exprOut = join(ws, 'expression-body-model.json')
	  execFileSync('node', [SCRIPT, '--file', exprBody, '--out', exprOut], { stdio: 'pipe' })
	  const exprModel = JSON.parse(readFileSync(exprOut, 'utf8'))
		  check('multi-line expression-bodied composable keeps body calls', () => {
		    assert.ok(exprModel.composables.find((c) => c.name === 'ExpressionScreen'))
		    assert.ok(exprModel.calls.find((c) => c.callee === 'PrimaryButton'))
		  })

		  const exprBoundary = join(src, 'ExpressionBoundary.kt')
		  writeFileSync(exprBoundary, `
		    package app.expr
		    import androidx.compose.runtime.Composable

		    @Composable
		    fun ExpressionBoundary() =
		      Column {
		        Text("x")
		      }

		    val Later = { GhostButton() }
		  `)
		  const exprBoundaryOut = join(ws, 'expression-boundary-model.json')
		  execFileSync('node', [SCRIPT, '--file', exprBoundary, '--out', exprBoundaryOut], { stdio: 'pipe' })
		  const exprBoundaryModel = JSON.parse(readFileSync(exprBoundaryOut, 'utf8'))
		  check('multi-line expression-bodied composable stops before following top-level val', () => {
		    const callees = exprBoundaryModel.calls.map((c) => c.callee)
		    assert.ok(callees.includes('Column'))
		    assert.equal(callees.includes('GhostButton'), false)
		  })

		  const nested = join(src, 'Nested.kt')
		  writeFileSync(nested, `
		    package app.nested
		    import androidx.compose.runtime.Composable

		    @Composable
		    fun NestedScreen() {
		      @Composable
		      fun UnusedComposableLocal() { HiddenGhostButton() }
		      fun UnusedLocal() { GhostButton() }
		      val unusedContent = { PhantomCard() }
		      Text("real")
		    }
	  `)
	  const nestedOut = join(ws, 'nested-model.json')
	  execFileSync('node', [SCRIPT, '--file', nested, '--out', nestedOut], { stdio: 'pipe' })
	  const nestedModel = JSON.parse(readFileSync(nestedOut, 'utf8'))
		  check('call extraction ignores unused local declarations inside composable bodies', () => {
		    const callees = nestedModel.calls.map((c) => c.callee)
			    assert.ok(callees.includes('Text'))
			    assert.equal(callees.includes('GhostButton'), false)
			    assert.equal(callees.includes('HiddenGhostButton'), false)
			    assert.equal(callees.includes('PhantomCard'), false)
			    assert.equal(nestedModel.composables.some((c) => c.name === 'UnusedComposableLocal'), false)
			  })

		  const invokedLocal = join(src, 'InvokedLocal.kt')
		  writeFileSync(invokedLocal, `
		    package app.nested
		    import androidx.compose.runtime.Composable

		    @Composable
		    fun InvokedLocalScreen() {
		      fun Body() { PrimaryButton() }
		      val content = { SecondaryCard() }
		      Body()
		      Column { content() }
		    }
		  `)
		  const invokedLocalOut = join(ws, 'invoked-local-model.json')
		  execFileSync('node', [SCRIPT, '--file', invokedLocal, '--out', invokedLocalOut], { stdio: 'pipe' })
		  const invokedLocalModel = JSON.parse(readFileSync(invokedLocalOut, 'utf8'))
		  check('call extraction includes invoked local functions and lambdas', () => {
		    const callees = invokedLocalModel.calls.map((c) => c.callee)
		    assert.ok(callees.includes('Body'))
		    assert.ok(callees.includes('PrimaryButton'))
		    assert.ok(callees.includes('SecondaryCard'))
		  })

		  const expressionLocals = join(src, 'ExpressionLocals.kt')
		  writeFileSync(expressionLocals, `
		    package app.nested
		    import androidx.compose.runtime.Composable

		    @Composable
		    fun ExpressionLocalsScreen() {
		      fun UnusedBody() = GhostButton()
		      fun UsedBody() = PrimaryButton()
		      UsedBody()
		      Column { Text("real") }
		    }
		  `)
		  const expressionLocalsOut = join(ws, 'expression-locals-model.json')
		  execFileSync('node', [SCRIPT, '--file', expressionLocals, '--out', expressionLocalsOut], { stdio: 'pipe' })
		  const expressionLocalsModel = JSON.parse(readFileSync(expressionLocalsOut, 'utf8'))
			  check('call extraction handles expression-bodied local functions by invocation', () => {
		    const callees = expressionLocalsModel.calls.map((c) => c.callee)
		    assert.ok(callees.includes('UsedBody'))
		    assert.ok(callees.includes('PrimaryButton'))
		    assert.ok(callees.includes('Column'))
			    assert.equal(callees.includes('GhostButton'), false)
			  })

			  const slotLambda = join(src, 'SlotLambda.kt')
			  writeFileSync(slotLambda, `
			    package app.nested
			    import androidx.compose.runtime.Composable

			    @Composable
			    fun SlotLambdaScreen() {
			      val content: @Composable () -> Unit = { PrimaryButton() }
			      Surface(content = content)
			    }
			  `)
			  const slotLambdaOut = join(ws, 'slot-lambda-model.json')
			  execFileSync('node', [SCRIPT, '--file', slotLambda, '--out', slotLambdaOut], { stdio: 'pipe' })
			  const slotLambdaModel = JSON.parse(readFileSync(slotLambdaOut, 'utf8'))
			  check('call extraction includes local slot lambdas passed as values', () => {
			    const callees = slotLambdaModel.calls.map((c) => c.callee)
			    assert.ok(callees.includes('Surface'))
			    assert.ok(callees.includes('PrimaryButton'))
			  })

			  const otherPrivate = join(src, 'OtherPrivate.kt')
			  const crossPackage = join(src, 'CrossPackage.kt')
			  writeFileSync(otherPrivate, `
			    package app.other
			    import androidx.compose.runtime.Composable

			    @Composable
			    private fun MissingComponent() { Text("other") }
			  `)
			  writeFileSync(crossPackage, `
			    package app.cross
			    import androidx.compose.runtime.Composable

			    @Composable
			    fun CrossPackageScreen() {
			      MissingComponent()
			    }
			  `)
			  const crossPackageOut = join(ws, 'cross-package-model.json')
			  execFileSync('node', [SCRIPT, '--file', otherPrivate, '--file', crossPackage, '--out', crossPackageOut], { stdio: 'pipe' })
			  const crossPackageModel = JSON.parse(readFileSync(crossPackageOut, 'utf8'))
			  check('private same-short-name composable from another package does not resolve calls', () => {
			    assert.ok(crossPackageModel.unresolvedRefs.find((r) => r.kind === 'UNRESOLVED_COMPONENT_CALL' && r.callee === 'MissingComponent'))
			  })

			  const dedupeOut = join(ws, 'dedupe-model.json')
	  execFileSync('node', [SCRIPT, '--file', file, '--root', src, '--out', dedupeOut], { stdio: 'pipe' })
	  const dedupeModel = JSON.parse(readFileSync(dedupeOut, 'utf8'))
	  check('same implementation file is deduped across --file and --root inputs', () => {
	    assert.equal(dedupeModel.files.filter((f) => f.path.endsWith('HomeScreen.kt')).length, 1)
	    assert.equal(dedupeModel.composables.filter((c) => c.name === 'HomeScreen' && c.file.endsWith('HomeScreen.kt')).length, 1)
	  })

	  // R2 arg-extractor: structured per-call args (inline dp, multi-arg, Arrangement.spacedBy sum,
	  // token refs, Color literal); a trailing-lambda-only call has no direct paren args. Adversarial
	  // forms the risk note calls out are all covered here.
	  const argsFile = join(src, 'ArgsScreen.kt')
	  writeFileSync(argsFile, `
	    package app.args
	    import androidx.compose.runtime.Composable
	    @Composable
	    fun ArgsScreen() {
	      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
	        PrimaryButton(color = AppColor.primary, modifier = Modifier.padding(16.dp, 8.dp))
	        Card(color = Color(0xFF112233), modifier = Modifier.padding(all = AppTokens.dp.card.padding))
	        Icon(tint = Color(0xFF445566L))
	        Box { Text("16.dp is a string here, not an arg") }
	      }
	    }
	  `)
	  const argsOut = join(ws, 'args-model.json')
	  execFileSync('node', [SCRIPT, '--file', argsFile, '--out', argsOut], { stdio: 'pipe' })
	  const argsModel = JSON.parse(readFileSync(argsOut, 'utf8'))
	  const argCall = (n) => argsModel.calls.find((c) => c.callee === n)
	  check('R2 arg-extractor: inline + multi-arg dp and a token ref captured on the call', () => {
	    const btn = argCall('PrimaryButton')
	    assert.ok(btn && btn.args && Array.isArray(btn.args.dp), 'PrimaryButton carries dp args')
	    assert.deepEqual(btn.args.dp, [8, 16], 'both padding dp values captured (multi-arg, sorted)')
	    assert.ok(btn.args.tokens.includes('AppColor.primary'), 'AppColor token captured')
	    assert.ok(!btn.args.colors, 'no raw Color literal on the token-correct call')
	  })
	  check('R2 arg-extractor: Arrangement.spacedBy(N.dp) dp captured on the OWNING call, not its children', () => {
	    const col = argCall('Column')
	    assert.deepEqual(col.args.dp, [8], 'spacedBy dp captured; children dp (16/8) NOT misattributed')
	  })
	  check('R2 arg-extractor: Color(0x…) literal (alpha preserved as #RRGGBBAA) + AppTokens.dp ref captured', () => {
	    const card = argCall('Card')
	    assert.ok(card.args.colors.includes('#112233FF'), 'Color(0xFF112233) → #112233FF (alpha kept, reordered)')
	    assert.ok(card.args.tokens.includes('AppTokens.dp.card.padding'), 'AppTokens.dp ref captured')
	  })
	  check('R2 arg-extractor: a Kotlin Long-suffixed Color(0x…L) literal is captured (not dropped)', () => {
	    const icon = argCall('Icon')
	    assert.ok(icon && icon.args && icon.args.colors && icon.args.colors.includes('#445566FF'), 'Color(0xFF445566L) → #445566FF')
	  })
	  check('R2 arg-extractor: a trailing-lambda-only call has no direct paren args (string dp masked)', () => {
	    const box = argCall('Box'), text = argCall('Text')
	    assert.ok(!box.args, 'Box { … } has no args field (only a lambda body)')
	    assert.ok(!text || !text.args, 'the "16.dp" inside a STRING is trivia-masked → not a dp arg')
	  })
	  check('R2 arg-extractor: model with args still validates against the schema', () => {
	    assert.equal(validate(argsModel), true, JSON.stringify(validate.errors || []))
	  })

	  // W2-3 owner-closure: a component invoked inside a private same-file sub-composable gets a
	  // duplicate call entry re-attributed to the public invoker (`ownerVia` = the direct owner),
	  // ONE level deep only — the extractor-side half of the spec gate's owner-binding fix.
	  const closureFile = join(src, 'ClosureScreen.kt')
	  writeFileSync(closureFile, `
	    package app.closure
	    import androidx.compose.runtime.Composable

	    @Composable
	    fun ClosureScreen() {
	      Column { ChannelsCard() }
	    }

	    @Composable
	    private fun ChannelsCard() {
	      Row { ToggleChip() }
	    }

	    @Composable
	    private fun ToggleChip() {
	      Badge()
	    }
	  `)
	  const closureOut = join(ws, 'closure-model.json')
	  execFileSync('node', [SCRIPT, '--file', closureFile, '--out', closureOut], { stdio: 'pipe' })
	  const closureModel = JSON.parse(readFileSync(closureOut, 'utf8'))
	  check('W2-3 owner-closure: sub-composable call keeps its direct owner AND gains an ownerVia entry', () => {
	    const direct = closureModel.calls.find((c) => c.callee === 'ToggleChip' && c.owner === 'ChannelsCard' && !c.ownerVia)
	    const closure = closureModel.calls.find((c) => c.callee === 'ToggleChip' && c.owner === 'ClosureScreen' && c.ownerVia === 'ChannelsCard')
	    assert.ok(direct, 'direct-owner entry must survive')
	    assert.ok(closure, 'closure entry re-attributed to the public invoker with ownerVia')
	  })
	  check('W2-3 owner-closure is ONE level deep: a two-hop chain does not close to the screen', () => {
	    assert.ok(!closureModel.calls.some((c) => c.callee === 'Badge' && c.owner === 'ClosureScreen'),
	      'Badge (screen -> card -> chip -> Badge) must NOT be re-attributed to ClosureScreen')
	    assert.ok(closureModel.calls.some((c) => c.callee === 'Badge' && c.owner === 'ToggleChip' && c.ownerVia === undefined),
	      'Badge keeps its direct owner')
	  })
	  check('W2-3 owner-closure model still validates against the schema (ownerVia declared)', () => {
	    assert.equal(validate(closureModel), true, JSON.stringify(validate.errors || []))
	  })
	} catch (e) {
  fail++; console.log(`${C.red}FAIL${C.reset} compose-model setup threw\n     ${e.stdout ? e.stdout.toString() : e.message}`)
} finally {
  rmSync(ws, { recursive: true, force: true })
}

console.log(`\ncompose-model.test: ${pass} pass, ${fail} fail`)
process.exit(fail === 0 ? 0 : 1)
