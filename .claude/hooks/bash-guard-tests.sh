#!/bin/zsh
# Test matrix for bash-guard.py. Run from the main session (read-only agents cannot run zsh):
#   zsh .claude/hooks/bash-guard-tests.sh
GUARD="$(cd "$(dirname "$0")" && pwd)/bash-guard.py"
export CLAUDE_PROJECT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
ERR="${TMPDIR:-/tmp}/guard_err.txt"
fails=0
run() {
  local mode="$1"; local cmd="$2"; local expect="$3"
  local payload
  payload=$(jq -cn --arg c "$cmd" '{tool_name:"Bash", tool_input:{command:$c}}')
  printf "%s\n" "$payload" | python3 "$GUARD" "$mode" 2>"$ERR"
  local rc=$?
  local got="allow"; [[ $rc -eq 2 ]] && got="block"
  local mark="OK "; if [[ "$got" != "$expect" ]]; then mark="FAIL"; fails=$((fails+1)); fi
  printf "%s [%s] expect=%s got=%s :: %s\n" "$mark" "$mode" "$expect" "$got" "${cmd//$'\n'/⏎}"
  [[ "$got" != "$expect" ]] && cat "$ERR"
}
NL=$'\n'
# ---------- readonly: allowed inspection ----------
run readonly 'git diff --stat' allow
run readonly 'git diff --stat HEAD~1..HEAD' allow
run readonly 'git log --oneline -5 -- .claude' allow
run readonly 'git status --short | head -20' allow
run readonly 'git -C /Users/x/project status --short' allow
run readonly 'git show HEAD:CLAUDE.md | head -5' allow
run readonly 'git stash list' allow
run readonly 'git stash show -p' allow
run readonly 'git branch --show-current' allow
run readonly 'git tag -l "v*"' allow
run readonly 'git branch --contains HEAD' allow
run readonly 'git branch --list "feat*"' allow
run readonly 'git branch -r --merged main' allow
run readonly 'GIT_PAGER=cat git log -1' allow
run readonly 'env GIT_PAGER=cat git log -1' allow
run readonly 'grep -rn "->" ui-core | wc -l' allow
run readonly 'grep -rn ">" ui-core' allow
run readonly 'grep -rn "&&" ui-core/foundation' allow
run readonly 'grep -rn "||" ui-core' allow
run readonly 'grep -n "(" Foo.kt' allow
run readonly 'grep -rn "a > b" ui-core' allow
run readonly 'cat file.kt 2>/dev/null; echo done' allow
run readonly 'cat file.kt 2>&1 | head' allow
run readonly 'wc -l < CLAUDE.md' allow
run readonly 'find . \( -name "*.kt" -o -name "*.kts" \) -print | head' allow
run readonly 'sed -n 1,20p file.kt' allow
run readonly 'sed -E "s/a/b/" file.kt | head' allow
run readonly 'command -v jq' allow
run readonly 'nice -n 5 ls' allow
run readonly '{ ls; }' allow
run readonly 'xcrun simctl list devices booted' allow
run readonly 'adb devices' allow
run readonly 'python3 -I .claude/hooks/check-rulebook.py' allow
run readonly 'VAR=1 grep -c foo file' allow
run readonly 'ls; cat CLAUDE.md | head' allow
# ---------- readonly: blocked ----------
run readonly 'ls -la > out.txt' block
run readonly 'echo pwned >& /tmp/out.txt' block
run readonly 'sed -i "" "s/a/b/" file.kt' block
run readonly 'sed -ni "" s/a/b/ CLAUDE.md' block
run readonly 'sed -n "w /tmp/out.txt" CLAUDE.md' block
run readonly 'sort -o /tmp/out.txt CLAUDE.md' block
run readonly 'awk "BEGIN{system(\"touch /tmp/pwned\")}"' block
run readonly 'plutil -convert xml1 iosApp/iosApp/Info.plist' block
run readonly 'yq -i ".a=1" x.yaml' block
run readonly 'tree -o /tmp/out.txt .' block
run readonly 'git diff --output=/tmp/x.patch' block
run readonly 'git commit -m x' block
run readonly 'git -C . commit -m x' block
run readonly 'git -c user.name=x commit -m msg' block
run readonly 'git --git-dir=.git add -A' block
run readonly 'git -c alias.ci=commit ci -m x' block
run readonly 'git -c core.fsmonitor="touch /tmp/pwned" status' block
run readonly 'git -c diff.external=rm diff' block
run readonly 'GIT_EXTERNAL_DIFF=rm git diff' block
run readonly 'git checkout -- .' block
run readonly 'git stash' block
run readonly 'git stash -u' block
run readonly 'git stash pop' block
run readonly 'git branch feature' block
run readonly 'git branch -D feature' block
run readonly 'git tag v1.0' block
run readonly 'git remote set-url origin x' block
run readonly 'git fetch --prune origin' block
run readonly 'GIT commit -m x' block
run readonly 'timeout 60 git commit -m x' block
run readonly 'xcrun git commit -m x' block
run readonly "ls${NL}rm -rf build" block
run readonly 'rm -rf build' block
run readonly 'touch /tmp/x' block
run readonly './gradlew :androidApp:assembleDebug' block
run readonly 'xcodebuild -scheme iosApp build' block
run readonly 'echo $(cat x)' block
run readonly 'cat < <(echo hi)' block
run readonly 'xcrun simctl boot X' block
run readonly 'python3 -c "print(1)"' block
run readonly 'python3 -I /etc/evil.py' block
run readonly 'python3 -I .claude/hooks/../../evil.py' block
run readonly 'find . -name "*.kt" -delete' block
run readonly 'gh gist create CLAUDE.md --public' block
run readonly 'gh extension install owner/evil' block
run readonly 'gh alias set pwn "!rm -rf ."' block
run readonly 'PATH=/tmp/evil:$PATH git status' block
# ---------- readonly-build: verifiers ----------
run readonly-build './gradlew :androidApp:assembleDebug --console=plain' allow
run readonly-build './gradlew :ui-core:state:compileAndroidMain' allow
run readonly-build './gradlew clean' block
run readonly-build './gradlew :androidApp:assembleDebug --rerun-tasks' block
run readonly-build 'xcrun simctl boot "iPhone 17"' allow
run readonly-build 'adb install -r build/app.apk' allow
run readonly-build 'adb exec-out screencap -p > /private/tmp/shots/s1.png' allow
run readonly-build 'adb exec-out screencap -p > "$SCRATCH/s1.png"' block
run readonly-build 'adb exec-out screencap -p > shots/s1.png' block
run readonly-build "adb exec-out screencap -p > $CLAUDE_PROJECT_DIR/s1.png" block
run readonly-build 'mkdir -p /private/tmp/shots' allow
run readonly-build 'mkdir -p build/evidence' block
run readonly-build 'sed -i "" "s/a/b/" file.kt' block
run readonly-build 'rm -rf build/tmp' block
run readonly-build 'git add -A && git commit -m x' block
run readonly-build 'git -C . commit -m x' block
run readonly-build 'timeout 60 git commit -m x' block
run readonly-build 'xcrun git commit -m x' block
run readonly-build "ls${NL}git commit -m x" block
run readonly-build 'python3 -I /private/tmp/scratchpad/x.py' block
run readonly-build 'xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator build' allow
run readonly-build 'xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp clean' block
run readonly-build '~/Library/Android/sdk/emulator/emulator -avd Pixel_10_Pro -no-snapshot-load &' allow
run readonly-build 'adb shell rm -rf /sdcard/x' block
# ---------- no-git-write: implementers ----------
run no-git-write './gradlew :androidApp:assembleDebug' allow
run no-git-write './gradlew build' block
run no-git-write './GRADLEW build' block
run no-git-write './gradlew :shared:assembleSharedDebugXCFramework --console=plain' allow
run no-git-write 'git add -A && git commit -m x' block
run no-git-write 'git -C . commit -m x' block
run no-git-write 'git -c user.name=x commit -m msg' block
run no-git-write 'git -c alias.ci=commit ci -m x' block
run no-git-write 'GIT commit -m x' block
run no-git-write "ls${NL}git push origin main" block
run no-git-write 'git push origin main' block
run no-git-write 'git stash' block
run no-git-write 'git diff HEAD' allow
run no-git-write 'rm -rf build/tmp' allow
run no-git-write 'sed -i "" "s/a/b/" file.kt' allow
run no-git-write 'gh pr create --title x' block
run no-git-write 'gh pr view 12' allow
run no-git-write 'gh api repos/x/y' block
run no-git-write 'gh api -X GET repos/x/y' allow
run no-git-write 'gh api --method=GET repos/x/y' allow
# ---------- adversarial additions ----------
run readonly 'gh api -X GET repos/x --input body.json' block
run readonly 'gh api -X GET repos/x -f a=b' block
run readonly 'HOME=/tmp git status' block
run readonly 'PAGER=rm git log' block
run readonly 'GIT_PAGER=rm git log' block
run readonly 'EDITOR=rm git commit' block
run readonly 'GIT_CONFIG_GLOBAL=/tmp/x git status' block
run readonly "ls \\${NL}-la" allow
run readonly 'cat <<EOF > /tmp/x' block
run readonly '(git commit -m x)' block
run readonly 'git --work-tree=/tmp commit -m x' block
run readonly 'git switch -c feat' block
run readonly 'git checkout -b feat' block
run readonly ': > file' block
run readonly 'echo x>file' block
run readonly '/usr/bin/git commit -m x' block
run readonly 'nice --20 rm -rf x' block
run readonly 'timeout -s KILL 5 git commit -m x' block
run readonly 'curl http://x' block
run readonly 'git log --format="%h %s" -3' allow
run readonly 'grep -rn "(a|b)" ui-core' allow
run readonly 'git tag --sort=-v:refname' allow
run readonly-build 'xcodebuild archive -scheme x' block
run readonly-build 'xcrun simctl erase all' block
run readonly-build 'xcrun simctl delete unavailable' block
run readonly-build 'xcrun agvtool new-version 2' block
run readonly-build 'adb shell "rm -rf /sdcard/x"' block
run readonly-build "adb pull /sdcard/x $CLAUDE_PROJECT_DIR/build/x" block
run readonly-build 'adb pull /sdcard/x /private/tmp/shots/x' allow
run readonly-build 'adb root' block
run readonly-build 'emulator -avd x -wipe-data' block
run readonly-build './gradlew :androidApp:assembleDebug -Pandroid.injected.signing.store.file=x' block
run no-git-write 'git config --unset user.name' block
run no-git-write 'gh repo clone x' block
echo "failures: $fails"
exit $fails
