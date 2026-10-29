#!/bin/zsh
# Test matrix for bash-guard.py. Run from the main session (read-only agents cannot run zsh):
#   zsh .claude/hooks/bash-guard-tests.sh
GUARD="$(cd "$(dirname "$0")" && pwd)/bash-guard.py"
export CLAUDE_PROJECT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
ERR="$(mktemp "${TMPDIR:-/tmp}/grippo-guard.XXXXXX")" || exit 1
trap 'rm -f "$ERR"' EXIT
fails=0
check_payload() {
  local mode="$1"; local payload="$2"; local expect="$3"; local label="$4"
  printf "%s\n" "$payload" | python3 "$GUARD" "$mode" 2>"$ERR"
  local rc=$?
  local got="error"
  case "$rc" in
    0) got="allow" ;;
    2) got="block" ;;
  esac
  local mark="OK "; if [[ "$got" != "$expect" ]]; then mark="FAIL"; fails=$((fails+1)); fi
  printf "%s [%s] expect=%s got=%s :: %s\n" "$mark" "$mode" "$expect" "$got" "${label//$'\n'/⏎}"
  [[ "$got" != "$expect" ]] && cat "$ERR"
}
run() {
  local mode="$1"; local cmd="$2"; local expect="$3"
  local payload
  payload=$(jq -cn --arg c "$cmd" '{tool_name:"Bash", tool_input:{command:$c}}') || exit 1
  check_payload "$mode" "$payload" "$expect" "$cmd"
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
run readonly-build 'adb -s emulator-5554 install -r build/app.apk' allow
run readonly-build 'adb -s emulator-5554 exec-out screencap -p > /private/tmp/shots/s1.png' allow
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
run readonly 'echo "$(touch /private/tmp/guard-probe)"' block
run readonly 'echo "`touch /private/tmp/guard-probe`"' block
run readonly 'echo "literal single quote '\''$(touch /private/tmp/guard-probe)'\''"' block
run readonly 'rg '\''$(literal) and `literal`'\'' CLAUDE.md' allow
run readonly 'echo "\$(literal)"' allow
run readonly-build 'echo "$(git add -A)"' block
run no-git-write 'echo "$(git add -A)"' block
run readonly 'rg --pre=touch pattern CLAUDE.md' block
run readonly 'rg --pre touch pattern CLAUDE.md' block
run readonly 'rg --hostname-bin=touch pattern CLAUDE.md' block
run readonly 'python3 -I .claude/hooks/bash-guard.py readonly' block
run readonly 'python3 -I .claude/hooks/check-rulebook.py --extra' block
run readonly 'adb shell input tap 10 10' block
run readonly 'adb shell am force-stop com.grippo' block
run readonly 'adb shell settings put system font_scale 2' block
run readonly 'adb shell "getprop; input tap 10 10"' block
run readonly "adb shell \"getprop${NL}input tap 10 10\"" block
run readonly 'adb shell getprop ro.build.version.release' allow
run readonly 'adb shell settings get system font_scale' allow
run readonly 'adb shell wm size' allow
run readonly 'adb shell wm size 100x100' block
run readonly 'adb shell dumpsys battery set level 1' block
run readonly 'adb -s emulator-5554 shell getprop ro.build.version.release' allow
run readonly 'adb -s emulator-5554 shell input tap 10 10' block
run readonly-build 'adb -s emulator-5554 shell rm -rf /sdcard/x' block
run readonly-build 'adb -e shell rm -rf /sdcard/x' block
run readonly-build 'adb -t 1 shell rm -rf /sdcard/x' block
run readonly-build 'adb -d install -r build/app.apk' block
run readonly-build 'adb -s physical-device install -r build/app.apk' block
run readonly-build 'adb -s emulator-5554 install -r build/app.apk' allow
run readonly-build 'adb -s emulator-5554 root' block
run readonly-build "adb -s emulator-5554 pull /sdcard/x $CLAUDE_PROJECT_DIR" block
run readonly-build "mkdir -p $CLAUDE_PROJECT_DIR" block
run readonly 'gh pr view 12' block
run readonly 'gh api -X GET repos/x/y' block
run readonly 'git tag -a name -F /private/tmp/message' block
run readonly-build 'git tag -a name -F /private/tmp/message' block
run no-git-write 'git tag -a name -F /private/tmp/message' block
run no-git-write 'git custom-alias' block
run readonly 'git branch --set-upstream-to=refs/heads/main' block
run readonly-build 'git branch --set-upstream-to=refs/heads/main' block
run no-git-write 'git branch --set-upstream-to=refs/heads/main' block
run readonly-build './gradlew :shared:build' block
run readonly-build './gradlew :shared:assemble' block
run readonly-build './gradlew :shared:check' block
run readonly-build './gradlew :shared:test' block
run readonly-build './gradlew syncSecureConfigs' block
run no-git-write './gradlew syncSecureConfigs' block
run readonly-build './gradlew :androidApp:assembleDebug --init-script=/private/tmp/init.gradle' block
run readonly-build './gradlew :androidApp:assembleDebug -I/private/tmp/init.gradle' block
check_payload readonly 'null' block 'null hook payload'
check_payload readonly '[]' block 'array hook payload'
check_payload readonly '{' block 'malformed JSON'
check_payload readonly '{"tool_name":"Bash","tool_input":{}}' block 'missing command'
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
run readonly-build 'adb -s emulator-5554 pull /sdcard/x /private/tmp/shots/x' allow
run readonly-build 'adb root' block
run readonly-build 'emulator -avd x -wipe-data' block
run readonly-build './gradlew :androidApp:assembleDebug -Pandroid.injected.signing.store.file=x' block
run no-git-write 'git config --unset user.name' block
run no-git-write 'gh repo clone x' block
# ---------- owner/capability regressions ----------
run readonly 'git branch -l guard-probe' allow
run readonly-build 'git branch --create-reflog guard-probe' block
run no-git-write 'git branch -v guard-probe' block
run readonly 'git branch -vv guard-probe' block
run readonly 'git branch --column guard-probe' block
run readonly 'git branch --sort=refname guard-probe' block
run readonly 'git branch --format="%(refname)" guard-probe' block
run readonly 'git branch --sort refname guard-probe' block
run readonly 'git branch --sort refname' allow
run readonly 'git branch -v --list "guard*"' allow
run readonly 'git branch --list --no-list guard-probe' block
run readonly 'git tag --sort=refname guard-probe' block
run readonly 'git -ccore.fsmonitor=touch status' block
run readonly 'git -cuser.name=reviewer status --short' allow
run readonly 'git --exec-path=/private/tmp status' block
run readonly 'git diff --ext-diff' block
run readonly 'git show --textconv HEAD:CLAUDE.md' block
run readonly 'git grep --open-files-in-pager=sh pattern' block
run readonly 'git grep -Osh pattern' block
run readonly 'sed -n "s/foo/bar/w /private/tmp/guard-probe" CLAUDE.md' block
run readonly 'sed -n "1w /private/tmp/guard-probe" CLAUDE.md' block
run readonly 'sed -f /private/tmp/unreviewed-script CLAUDE.md' block
run readonly 'sed --file=/private/tmp/unreviewed-script CLAUDE.md' block
run readonly 'sed "s/foo/bar/e" CLAUDE.md' block
run readonly 'sed -n "1,20p" CLAUDE.md' allow
run readonly 'sed -n -e "1,20p" -e "25,30p" CLAUDE.md' allow
run readonly 'sed -E "s|foo|bar|g" CLAUDE.md' allow
run readonly 'awk '\''BEGIN {system ("touch /private/tmp/guard-probe")}'\''' block
run readonly 'awk -f /private/tmp/unreviewed-script CLAUDE.md' block
run readonly 'awk '\''NR <= 20 {print $0}'\'' CLAUDE.md' allow
run readonly-build './gradlew :shared:publish' block
run readonly-build './gradlew :androidApp:testDebugUnitTest' block
run readonly-build './gradlew :androidApp:lintDebug' block
run readonly-build './gradlew :shared:tasks --all' allow
run readonly-build 'adb install -r build/app.apk' block
run readonly-build 'adb -e shell input tap 10 10' block
run readonly-build 'adb -s emulator-5554 emu avd name' allow
run readonly-build 'adb -s emulator-5554 shell input tap 10 10' allow
run readonly-build 'xcrun simctl io selected-simulator screenshot /private/tmp/shots/ios.png' allow
run readonly-build "xcrun simctl io selected-simulator screenshot $CLAUDE_PROJECT_DIR/ios.png" block
run readonly-build 'xcrun simctl io booted screenshot /private/tmp/shots/ios.png' block
run readonly-build 'simctl erase all' block
run readonly-build 'xcrun simctl io selected-simulator screenshot "/private/tmp/$OUT"' block
run readonly-build 'adb -s emulator-5554 pull /sdcard/x "/private/tmp/$OUT"' block
run readonly-build 'mkdir -p "/private/tmp/$OUT"' block
run readonly-build 'adb -s emulator-5554 exec-out screencap -p > "/private/tmp/$OUT"' block
run readonly-build 'xcrun simctl io selected-simulator screenshot /private/tmp/*/shot.png' block
run readonly-build 'adb -s emulator-5554 pull /sdcard/x' block
run readonly-build 'adb -s emulator-5554 pull -a /sdcard/x' block
# ---------- deep parser and supported-inspection regressions ----------
run readonly 'echo token#literal; git add -A' block
run no-git-write 'echo token#literal; git add -A' block
run readonly 'ls # comment'"$NL"'git add -A' block
run readonly 'ls # only this line is a comment'"$NL"'git status --short' allow
run readonly 'ls # inspect $(literal)' allow
run readonly "echo \\'; git add -A; echo \\'" block
run readonly 'echo \"literal; git add -A; echo \"' block
run no-git-write 'gi\'"$NL"'t add -A' block
run no-git-write "# don't parse comment quotes"$NL'gi\'"$NL"'t add -A' block
run readonly 'echo text # continued comment \'"$NL"'git add -A' allow
run readonly 'rg QSPAN999Z CLAUDE.md' allow
run readonly 'rg QSPAN0Z "literal value" CLAUDE.md' allow
run readonly-build 'echo safe > QSPAN0Z; cat "/private/tmp/guard-evidence"' block
run readonly 'echo "$(printf literal)"' block
run readonly 'rg '\''line one\'"$NL"'line two'\'' CLAUDE.md' allow
run readonly 'GIT_TRACE=/private/tmp/guard-trace git status' block
run no-git-write 'env -S "git add -A"' block
run no-git-write 'env --split-string="git add -A"' block
run no-git-write 'env --unset GIT_DIR git add -A' block
run readonly 'env --unset GIT_DIR git status --short' allow
run readonly 'env rg -S pattern CLAUDE.md' allow
run readonly '/usr/bin/time -o/private/tmp/guard-time cat CLAUDE.md' block
run readonly 'time -p cat CLAUDE.md' allow
run readonly 'nice -n5 git status --short' allow
run readonly 'timeout --signal TERM 5 git status --short' allow
run readonly 'command -v python3' allow
run readonly 'command -V git' allow
run readonly 'command -v git; git add -A' block
run readonly 'sort --compress-program=touch CLAUDE.md' block
run readonly 'file -C -m /private/tmp/guard-magic' block
run readonly 'file CLAUDE.md' allow
run readonly-build 'xcrun --sdk macosx git add -A' block
run no-git-write 'xcrun --sdk macosx git add -A' block
run readonly-build 'xcrun xcodebuild archive -scheme iosApp' block
run readonly-build 'xcrun --sdk iphonesimulator simctl erase all' block
run readonly 'xcrun --sdk iphonesimulator simctl list devices booted' allow
run readonly-build 'xcrun --sdk iphonesimulator xcodebuild -scheme iosApp build' allow
run readonly 'git -c include.path=/private/tmp/guard-config status' block
run readonly 'git -c includeIf.gitdir.path=/private/tmp/guard-config status' block
run readonly 'git -c diff.guard.textconv=sh show HEAD:CLAUDE.md' block
run readonly 'git -c diff.guard.extra.textconv=sh show HEAD:CLAUDE.md' block
run readonly 'git remote show origin' block
run readonly 'git remote show -- origin -n' block
run readonly 'git remote show -n origin' allow
run readonly 'git remote get-url show' allow
run readonly 'git diff -O /private/tmp/orderfile' allow
run readonly 'git grep -- -Osh' allow
run readonly 'git grep -e -- -Osh' block
run readonly 'git grep -f -- -Osh' block
run readonly 'git grep -ne -- -Osh' block
run readonly 'git grep -nOsh pattern' block
run readonly 'git grep -e -Osh' allow
run readonly 'git grep -ne-Osh' allow
run readonly 'git grep --context 2 -- -Osh' allow
run readonly 'git grep --open-f=sh pattern' block
run readonly 'git grep --textc pattern' block
run readonly 'git grep --text pattern' allow
run readonly 'git --exec-path' allow
run readonly 'git branch -uorigin/main' block
run readonly-build './gradlew :androidApp:assembleDebug --console plain' allow
run readonly-build './gradlew :shared:dependencyInsight --dependency koin --configuration androidRuntimeClasspath' allow
run readonly-build './gradlew --project-dir=/private/tmp :androidApp:assembleDebug' block
run readonly-build './gradlew -p/private/tmp :androidApp:assembleDebug' block
# ---------- compilation role excludes all device and setup actions ----------
run readonly-compile './gradlew :androidApp:assembleDebug --console=plain' allow
run readonly-compile './gradlew :shared:assembleSharedDebugXCFramework --console=plain' allow
run readonly-compile 'xcodebuild -scheme iosApp -configuration Debug -derivedDataPath /tmp/grippo-compile build' allow
run readonly-compile 'xcrun --sdk iphonesimulator xcodebuild -scheme iosApp build' allow
run readonly-compile './gradlew :androidApp:assembleDebug > /tmp/grippo-compile.log 2>&1' allow
run readonly-compile 'git diff --stat' allow
for command in \
  'adb devices' \
  'adb -s emulator-5554 install -r app.apk' \
  'adb -s emulator-5554 shell input tap 1 2' \
  'emulator -avd Pixel' \
  'simctl list devices' \
  'xcrun simctl list devices' \
  'xcrun --sdk iphonesimulator simctl boot ABC' \
  'env command xcrun simctl io ABC screenshot /tmp/probe.png' \
  'kill 123' \
  'pkill emulator' \
  'open -a Simulator' \
  'sips -s format png a.webp --out /tmp/a.png' \
  'mkdir /tmp/grippo-compile' \
  'gh pr view 1' \
  './gradlew :androidApp:assembleDebug > compile.log' \
  './gradlew :androidApp:testDebugUnitTest' \
  'xcodebuild -scheme iosApp test' \
  'xcrun xcodebuild -scheme iosApp test-without-building' \
  'xcodebuild -scheme iosApp install' \
  'xcodebuild -downloadPlatform iOS'; do
  run readonly-compile "$command" block
done
run readonly-build 'adb -s emulator-5554 shell input tap 1 2' allow
run readonly-build 'xcrun simctl io ABC screenshot /tmp/probe.png' allow
run readonly-build 'xcodebuild -scheme iosApp test' block
run readonly-compile './gradlew :androidApp:assembleDebug 2 > /tmp/grippo-compile.log' block
run readonly-compile './gradlew :androidApp:assembleDebug 2>/tmp/grippo-compile-errors.log' allow
run readonly-compile './gradlew :androidApp:assembleDebug;2>/tmp/grippo-compile-errors.log' allow
run readonly-compile './gradlew :androidApp:assembleDebug FDSPAN2' block
run readonly-compile 'xcodebuild -license accept' block
run readonly-compile 'xcodebuild -downloadComponent metalToolchain' block
run readonly-build 'xcodebuild -downloadComponent metalToolchain' block
run readonly-compile 'xcodebuild -scheme iosApp installhdrs' block
run readonly-compile 'xcodebuild -scheme iosApp -allowProvisioningUpdates build' block
run readonly-compile 'xcodebuild -scheme iosApp -allowProvisioningDeviceRegistration build' block
run readonly-compile 'xcodebuild -scheme iosApp -showdestinations' block
run readonly-build 'xcodebuild -scheme iosApp -showdestinations' allow
check_payload readonly '{"tool_input":{"command":"git status --short"}}' block 'missing tool_name'
check_payload readonly '{"tool_name":null}' block 'non-string tool_name'
check_payload readonly '{"tool_name":""}' block 'empty tool_name'
run readonly-compile 'xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator -destination "generic/platform=iOS Simulator" -arch arm64 -derivedDataPath /tmp/grippo-compile build' allow
# Git short-option clusters cannot hide reference mutations.
for mode in readonly readonly-compile readonly-build no-git-write; do
  run "$mode" 'git branch -vuorigin/main' block
  run "$mode" 'git branch -vumain' block
  run "$mode" 'git branch -vd' block
  run "$mode" 'git branch -vD' block
  run "$mode" 'git branch -vm' block
  run "$mode" 'git branch -vM' block
  run "$mode" 'git branch -vc' block
  run "$mode" 'git branch -vC' block
  run "$mode" 'git branch -vf' block
  run "$mode" 'git tag -la' block
  run "$mode" 'git tag -ls' block
  run "$mode" 'git tag -lmmessage' block
  run "$mode" 'git branch -avv' allow
  run "$mode" 'git branch --sort -committerdate --list' allow
  run "$mode" 'git tag -ln2' allow
done
echo "failures: $fails"
exit $((fails > 0))
