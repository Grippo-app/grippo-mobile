#!/usr/bin/env bash
# End-to-end Architecture Map v2 generator regression suite.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TASKS_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
GEN="$TASKS_DIR/regen-arch.py"
ANALYSIS="$TASKS_DIR/architecture_analysis.py"
export PYTHONDONTWRITEBYTECODE=1

pass=0
fail=0
ok()  { echo "  ok  : $1"; pass=$((pass + 1)); }
bad() { echo "  FAIL: $1"; fail=$((fail + 1)); }

FIX=""
ABSENT=""
WRONGCWD=""
cleanup() {
  [ -n "$FIX" ] && rm -rf "$FIX"
  [ -n "$ABSENT" ] && rm -rf "$ABSENT"
  [ -n "$WRONGCWD" ] && rm -rf "$WRONGCWD"
}
trap cleanup EXIT

mk() { mkdir -p "$(dirname "$1")"; cat > "$1"; }
R() { python3 "$GEN" "$@"; }

FIX="$(mktemp -d 2>/dev/null || mktemp -d -t archv2)"
cd "$FIX" || exit 2
J="orchestrator/.arch-map.json"

mk settings.gradle.kts <<'EOF'
rootProject.name = "demo"
include(
  ":shared",
  ":ui-screen-features:screen-api",
  ":ui-screen-features:home",
)
include(":data-features:feature-api")
include(":data-features:note")
include(":data-features:note-tag")
include(":data-services:backend")
include(":data-services:database")
include(":toolkit:logger")
include(":orphan")
// include(":dead-commented")
/* include(":dead-block") */
include(":shared")
EOF

mk orchestrator/project-config.md <<'EOF'
---
apiClassName: "DemoApi"
productPackage: com.demo.app
featuresWithRootComponentSuffix: []
---
EOF

mk orchestrator/architecture-rules.json <<'EOF'
{
  "schemaVersion": 1,
  "dependencyRules": [
    {
      "id": "ui-must-not-depend-on-infrastructure",
      "fromLayer": "ui",
      "disallowToLayer": "infrastructure",
      "fromPlatform": null,
      "toPlatform": null,
      "fromModule": null,
      "toModule": null,
      "severity": "error"
    }
  ],
  "rootModules": ["shared", "toolkit/**"],
  "standaloneModules": []
}
EOF

mk shared/build.gradle.kts <<'EOF'
plugins { kotlin("multiplatform") }
EOF
mk ui-screen-features/screen-api/build.gradle.kts <<'EOF'
plugins { kotlin("multiplatform") }
EOF
mk ui-screen-features/home/build.gradle.kts <<'EOF'
plugins { kotlin("multiplatform") }
commonMainImplementation(project(":data-services:backend"))
EOF
mk data-features/feature-api/build.gradle.kts <<'EOF'
plugins { kotlin("multiplatform") }
EOF
mk data-features/note/build.gradle.kts <<'EOF'
plugins { kotlin("multiplatform") }
val documentation = "project(fakeDocumentationDependency)"
commonMainImplementation(project(":data-features:note-tag"))
implementation(project(dynamicModule))
EOF
mk data-features/note-tag/build.gradle.kts <<'EOF'
plugins { kotlin("multiplatform") }
commonMainImplementation(project(":data-features:note"))
EOF
mk data-services/backend/build.gradle.kts <<'EOF'
plugins { kotlin("multiplatform") }
EOF
mk data-services/database/build.gradle.kts <<'EOF'
plugins { kotlin("multiplatform") }
EOF
mk toolkit/logger/build.gradle.kts <<'EOF'
plugins { kotlin("multiplatform") }
implementation(project(dynamicModule))
EOF
mk orphan/build.gradle.kts <<'EOF'
plugins { kotlin("multiplatform") }
EOF

mk shared/src/commonMain/kotlin/com/demo/app/App.kt <<'EOF'
package com.demo.app
/*
  outer comment
  /* nested comment with class GhostRepository */
*/
class NoteUseCase(private val repository: NoteRepository)
EOF
mk ui-screen-features/screen-api/src/commonMain/kotlin/com/demo/app/screenapi/HomeRouter.kt <<'EOF'
package com.demo.app.screenapi
import kotlinx.serialization.Serializable
sealed interface HomeRouter {
    @Serializable data object Feed : HomeRouter
    @Serializable data class Detail(val id: String) : HomeRouter
}
EOF
mk ui-screen-features/home/src/commonMain/kotlin/com/demo/app/home/HomeComponent.kt <<'EOF'
package com.demo.app.home
class HomeComponent
EOF
mk data-features/feature-api/src/commonMain/kotlin/com/demo/app/featureapi/Features.kt <<'EOF'
package com.demo.app.featureapi
interface NoteFeature
interface NoteTagFeature
EOF
mk data-features/note/src/commonMain/kotlin/com/demo/app/note/NoteRepository.kt <<'EOF'
package com.demo.app.note
class NoteRepository {
    fun save(entity: NoteEntity) = Unit
}
EOF
mk data-features/note-tag/src/commonMain/kotlin/com/demo/app/notetag/NoteTagRepository.kt <<'EOF'
package com.demo.app.notetag
class NoteTagRepository
EOF
mk data-services/backend/src/commonMain/kotlin/com/demo/app/backend/DemoApi.kt <<'EOF'
package com.demo.app.backend
class DemoApi {
    suspend fun getNotes(): List<String> = emptyList()
    suspend fun createNote() {}
}
EOF
mk data-services/database/src/commonMain/kotlin/com/demo/app/database/Database.kt <<'EOF'
package com.demo.app.database
@Database(version = 7, entities = [
    NoteEntity::class,
    NoteTagEntity::class,
])
abstract class DemoDatabase
val decoy = listOf(FakeEntity::class)
EOF
mk orphan/src/commonMain/kotlin/com/demo/app/orphan/Orphan.kt <<'EOF'
package com.demo.app.orphan
class Orphan
EOF

echo "== v2 generation =="
R >/dev/null 2>&1
rc=$?
[ $rc -eq 0 ] && ok "generator exits 0" || bad "generator exit $rc"
[ -f "$J" ] && ok "canonical map written" || bad "canonical map missing"

assert_py() {
  local label="$1" expr="$2"
  if python3 - "$J" "$ANALYSIS" "$expr" <<'PY'
import importlib.util,json,sys
with open(sys.argv[1], encoding="utf-8") as fh:
    d=json.load(fh)
spec=importlib.util.spec_from_file_location("architecture_analysis", sys.argv[2])
m=importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
ok=bool(eval(sys.argv[3], {"d":d,"m":m}))
raise SystemExit(0 if ok else 1)
PY
  then ok "$label"; else bad "$label"; fi
}

assert_py "schemaVersion 2 only" "d['schemaVersion']==2 and 'version' not in d"
assert_py "closed v2 envelope" "set(d)=={'schemaVersion','generatedAt','generatedAtRevision','structuralHash','generatorVersion','analysis','summary','nodes','edges','findings'}"
assert_py "semantic validator accepts output" "m.validate_map(d) is d"
if python3 - "$J" "$ANALYSIS" <<'PY'
import importlib.util,json,sys
with open(sys.argv[1], encoding="utf-8") as fh:
    value=json.load(fh)
spec=importlib.util.spec_from_file_location("architecture_analysis", sys.argv[2])
module=importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
value["generatedAt"]="2026-02-30T25:61:61Z"
try:
    module.validate_map(value)
except module.ArchitectureError:
    raise SystemExit(0)
raise SystemExit(1)
PY
then ok "impossible UTC timestamp is rejected"; else bad "impossible UTC timestamp was accepted"; fi
if python3 - "$J" "$ANALYSIS" <<'PY'
import copy,importlib.util,json,sys
with open(sys.argv[1], encoding="utf-8") as fh:
    original=json.load(fh)
spec=importlib.util.spec_from_file_location("architecture_analysis", sys.argv[2])
module=importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
cases=[]
zero_year=copy.deepcopy(original); zero_year["generatedAt"]="0000-01-01T00:00:00Z"; cases.append(zero_year)
wrong_status=copy.deepcopy(original); wrong_status["analysis"]["status"]="complete"; cases.append(wrong_status)
boolean_count=copy.deepcopy(original); boolean_count["summary"]["findingsBySeverity"]["error"]=True; cases.append(boolean_count)
false_unknown=copy.deepcopy(original); false_unknown["summary"]["databaseEntities"]=None; cases.append(false_unknown)
bad_path=copy.deepcopy(original); bad_path["nodes"][0]["path"]="."; bad_path["structuralHash"]=module.structural_hash(bad_path); cases.append(bad_path)
unknown_reason=copy.deepcopy(original); unknown_reason["findings"][0]["evidence"][0]["reasonCode"]="future-legacy-reason"; unknown_reason["structuralHash"]=module.structural_hash(unknown_reason); cases.append(unknown_reason)
for value in cases:
    try:
        module.validate_map(value)
    except module.ArchitectureError:
        continue
    raise SystemExit(1)
PY
then ok "semantic state/path/count mismatches are rejected"; else bad "semantic state/path/count mismatch was accepted"; fi
assert_py "structural hash verifies" "d['structuralHash']==m.structural_hash(d)"
assert_py "nodes/edges/findings sorted" "all([a==sorted(a,key=lambda x:x['id']) for a in (d['nodes'],d['edges'],d['findings'])])"
assert_py "10 modules deduped/comments ignored" "d['summary']['modules']==10"
assert_py "screen and feature ownership extracted" "any(n['kind']=='screen' and n['name']=='Home' for n in d['nodes']) and any(e['kind']=='owns' and dct[e['to']]['kind']=='screen' for e in d['edges'] for dct in [{n['id']:n for n in d['nodes']}])"
assert_py "routes retained as searchable metadata" "next(n for n in d['nodes'] if n['kind']=='screen')['metadata']['routes']==['Detail','Feed']"
assert_py "repositories extracted without prefix collision" "{n['metadata']['className'] for n in d['nodes'] if n['kind']=='repository'}=={'NoteRepository','NoteTagRepository'}"
assert_py "nested comments and dependency strings are ignored" "not any(n['name']=='GhostRepository' for n in d['nodes']) and d['analysis']['coverage']['unsupportedDependencyExpressions']==2"
assert_py "API methods extracted" "next(n for n in d['nodes'] if n['kind']=='api')['metadata']['methods']==['createNote','getNotes']"
assert_py "Room entities block-scoped" "{n['metadata']['entityClass'] for n in d['nodes'] if n['kind']=='database-entity'}=={'NoteEntity','NoteTagEntity'}"
assert_py "database summary is known" "d['summary']['databaseEntities']==2"
assert_py "supported dependency edges extracted" "sum(e['kind']=='depends-on' for e in d['edges'])==3"
assert_py "unsupported Gradle expressions remain exact when adjacent" "d['analysis']['status']=='partial' and d['analysis']['coverage']['unsupportedDependencyExpressions']==2"
assert_py "cycle finding produced" "any(f['type']=='dependency-cycle' for f in d['findings'])"
assert_py "forbidden dependency finding produced" "any(f['type']=='forbidden-dependency' and f['severity']=='error' for f in d['findings'])"
assert_py "orphan exception/root rules applied" "any(f['type']=='orphan-module' and any(n['name']==':orphan' and n['id'] in f['affectedNodeIds'] for n in d['nodes']) for f in d['findings'])"
assert_py "partial unused repository is not error" "all(f['severity']!='error' for f in d['findings'] if f['type']=='unused-repository')"
assert_py "finding fingerprints and firstSeen revisions are hashes" "all(m.HASH_RE.fullmatch(f['fingerprint']) and m.HASH_RE.fullmatch(f['firstSeenRevision']) for f in d['findings'])"
[ -f orchestrator/.cache/architecture/input-receipt.json ] && ok "canonical input receipt published" || bad "input receipt missing"
assert_py "v2 diff exposes exact totals and truncation state" "all(all([m.valid_compact_diff(x),x['schemaVersion']==2,not x['truncated'],all(v==0 for v in x['changeTotals'].values())]) for x in [m.json.load(open('orchestrator/.cache/architecture/history/'+m.os.listdir('orchestrator/.cache/architecture/history')[0]))])"
[ -f orchestrator/.cache/architecture/latest-diff.json ] && ok "latest diff pointer published" || bad "latest diff missing"

echo "== freshness and deterministic structure =="
first_struct="$(python3 -c "import json;print(json.load(open('$J'))['structuralHash'])")"
first_revision="$(python3 -c "import json;print(json.load(open('$J'))['generatedAtRevision'])")"
R --check >/dev/null 2>&1 && ok "--check fresh after generation" || bad "--check unexpectedly stale"
printf '\n// comment-only source edit\n' >> ui-screen-features/home/src/commonMain/kotlin/com/demo/app/home/HomeComponent.kt
if R --check-json > check.json 2>/dev/null; then
  bad "content revision edit must stale the map"
else
  reason="$(python3 -c "import json;print(json.load(open('check.json'))['reason'])")"
  [ "$reason" = "source-revision-drift" ] && ok "content edit stales trusted source revision" || bad "wrong stale reason: $reason"
fi
candidate_struct="$(python3 -c "import json;print(json.load(open('check.json'))['expectedHash'])")"
[ "$candidate_struct" = "$first_struct" ] && ok "comment edit preserves structural hash" || bad "comment edit changed structural hash"
R >/dev/null 2>&1
second_struct="$(python3 -c "import json;print(json.load(open('$J'))['structuralHash'])")"
second_revision="$(python3 -c "import json;print(json.load(open('$J'))['generatedAtRevision'])")"
[ "$second_struct" = "$first_struct" ] && ok "regen remains structurally deterministic" || bad "regen structural hash drifted"
[ "$second_revision" != "$first_revision" ] && ok "regen records changed input revision" || bad "input revision did not change"

cat >> ui-screen-features/screen-api/src/commonMain/kotlin/com/demo/app/screenapi/HomeRouter.kt <<'EOF'
sealed interface ArchiveHolder { @Serializable data object Archive : HomeRouter }
EOF
R --check-json > structural.json 2>/dev/null
rc=$?
[ $rc -eq 1 ] && ok "structural edit is stale" || bad "structural edit check exit $rc"
changed_struct="$(python3 -c "import json;print(json.load(open('structural.json'))['expectedHash'])")"
[ "$changed_struct" != "$second_struct" ] && ok "structural edit changes structural hash" || bad "structural hash ignored new route"
R --trigger task-finalization --trigger-id fin-test --task-stem TASK_1_demo >/dev/null 2>&1
[ -f orchestrator/.cache/architecture/latest-task-diff.json ] && ok "task finalization pointer published" || bad "task diff pointer missing"
assert_py "task-linked diff identity retained" "m.json.load(open('orchestrator/.cache/architecture/latest-task-diff.json'))['taskStem']=='TASK_1_demo'"

echo "== hardening and failure preservation =="
before_hash="$(shasum -a 256 "$J" | awk '{print $1}')"
ln -s HomeComponent.kt ui-screen-features/home/src/commonMain/kotlin/com/demo/app/home/Evil.kt
R >/dev/null 2>&1
rc=$?
[ $rc -eq 1 ] && ok "symlinked source is rejected" || bad "symlinked source exit $rc"
after_hash="$(shasum -a 256 "$J" | awk '{print $1}')"
[ "$after_hash" = "$before_hash" ] && ok "failed generation preserves canonical map" || bad "failed generation replaced map"
rm ui-screen-features/home/src/commonMain/kotlin/com/demo/app/home/Evil.kt

cp settings.gradle.kts settings.good
printf '\ninclude(dynamicModule)\n' >> settings.gradle.kts
R >/dev/null 2>&1
rc=$?
[ $rc -eq 1 ] && ok "unsupported settings include is rejected" || bad "unsupported settings include exit $rc"
after_settings_hash="$(shasum -a 256 "$J" | awk '{print $1}')"
[ "$after_settings_hash" = "$before_hash" ] && ok "invalid settings preserve canonical map" || bad "invalid settings replaced map"
mv settings.good settings.gradle.kts

cp orchestrator/architecture-rules.json rules.good
python3 - <<'PY'
import json
p='orchestrator/architecture-rules.json'
d=json.load(open(p)); d['unknown']=True
open(p,'w').write(json.dumps(d))
PY
R >/dev/null 2>&1
rc=$?
[ $rc -eq 1 ] && ok "unknown rules field is rejected" || bad "invalid rules exit $rc"
after_rules_hash="$(shasum -a 256 "$J" | awk '{print $1}')"
[ "$after_rules_hash" = "$before_hash" ] && ok "invalid rules preserve canonical map" || bad "invalid rules replaced map"
mv rules.good orchestrator/architecture-rules.json

typed_before_hash="$(shasum -a 256 "$J" | awk '{print $1}')"
R --expected-source-revision \
  sha256:0000000000000000000000000000000000000000000000000000000000000000 >/dev/null 2>&1
rc=$?
[ $rc -eq 1 ] && ok "expected source revision fences generation" || bad "source revision conflict exit $rc"
expected_after_hash="$(shasum -a 256 "$J" | awk '{print $1}')"
[ "$expected_after_hash" = "$typed_before_hash" ] && ok "source conflict preserves canonical map" || bad "source conflict replaced map"

R --trigger manual-refresh \
  --trigger-id archjob-00000000000000000000000000000000 >/dev/null 2>&1
rc=$?
[ $rc -eq 1 ] && ok "typed site generation requires exact writer lease" || bad "unguarded typed generation exit $rc"
typed_after_hash="$(shasum -a 256 "$J" | awk '{print $1}')"
[ "$typed_after_hash" = "$typed_before_hash" ] && ok "lost writer lease preserves canonical map" || bad "lost writer lease replaced map"

echo "== concurrent same-tree publication =="
crc=0
for _ in 1 2 3; do
  R >/dev/null 2>&1 & p1=$!
  R >/dev/null 2>&1 & p2=$!
  wait $p1; r1=$?
  wait $p2; r2=$?
  [ $r1 -eq 0 ] && [ $r2 -eq 0 ] || crc=1
done
[ $crc -eq 0 ] && ok "concurrent generators both publish valid generations" || bad "concurrent generator failed"
assert_py "map remains valid after concurrency" "m.validate_map(d) is d"

echo "== absence and invocation guards =="
ABSENT="$(mktemp -d 2>/dev/null || mktemp -d -t archabsent)"
mkdir -p "$ABSENT/orchestrator"
( cd "$ABSENT" && python3 "$GEN" >/dev/null 2>&1 )
[ $? -eq 0 ] && ok "pre-bootstrap absence exits 0" || bad "pre-bootstrap absence failed"
[ ! -f "$ABSENT/orchestrator/.arch-map.json" ] && ok "pre-bootstrap writes no map" || bad "pre-bootstrap wrote a map"
WRONGCWD="$(mktemp -d 2>/dev/null || mktemp -d -t archwrong)"
( cd "$WRONGCWD" && python3 "$GEN" >/dev/null 2>&1 )
[ $? -eq 1 ] && ok "wrong cwd is rejected" || bad "wrong cwd was accepted"

echo
echo "------------------------------------------------------------"
echo "test-regen-arch.sh: $pass passed, $fail failed"
[ $fail -eq 0 ] && { echo "OK"; exit 0; } || { echo "FAILED"; exit 1; }
