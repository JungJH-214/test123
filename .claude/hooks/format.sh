#!/usr/bin/env bash
# PostToolUse(Edit|Write) 훅: Claude가 수정한 파일을 포맷한다.
# 포맷 도구가 없거나 실패해도 작업을 막지 않도록 항상 exit 0으로 끝낸다.

input=$(cat)

command -v jq >/dev/null 2>&1 || exit 0
file=$(printf '%s' "$input" | jq -r '.tool_input.file_path // empty' 2>/dev/null)
[ -n "$file" ] || exit 0

root="${CLAUDE_PROJECT_DIR:-$(pwd)}"

# Windows 경로(C:\...)를 Git Bash 경로(/c/...)로 바꾼다
if command -v cygpath >/dev/null 2>&1; then
  file=$(cygpath -u "$file")
  root=$(cygpath -u "$root")
fi

case "$file" in
  *.java)
    # 개인 설정(settings.local.json)의 env.JAVA_HOME이 있으면 그 JDK로 Gradle을 실행한다.
    # 시스템 JAVA_HOME이 JDK 8이고, 앱이 이 env를 훅에 넘겨 주지 않는 환경이 있기 때문이다.
    local_settings="$root/.claude/settings.local.json"
    if [ -f "$local_settings" ]; then
      jdk=$(jq -r '.env.JAVA_HOME // empty' "$local_settings" 2>/dev/null)
      [ -n "$jdk" ] && export JAVA_HOME="$jdk"
    fi
    (cd "$root/backend" && ./gradlew spotlessApply -q) >/dev/null 2>&1
    ;;
  *.ts|*.tsx)
    prettier="$root/frontend/node_modules/.bin/prettier"
    if [ -x "$prettier" ]; then
      (cd "$root/frontend" && "$prettier" --write "$file") >/dev/null 2>&1
    fi
    ;;
esac

exit 0
