#!/usr/bin/env bash
# PreToolUse(Bash) 훅: 운영 환경 접속과 되돌릴 수 없는 명령을 막는다.
# 막을 때는 stderr에 "이유 + 대신 할 일"을 쓰고 exit 2로 끝낸다. 이 메시지는 Claude에게 전달된다.
# 모든 Bash 호출마다 실행되므로 외부 명령(git 등) 없이 문자열 검사만 한다.

input=$(cat)

if ! command -v jq >/dev/null 2>&1; then
  echo "guard-bash: jq가 없어 명령 검사를 하지 못했습니다" >&2
  exit 0
fi
cmd=$(printf '%s' "$input" | jq -r '.tool_input.command // empty' 2>/dev/null)
[ -n "$cmd" ] || exit 0

block() {
  echo "차단: $1" >&2
  echo "대신: $2" >&2
  exit 2
}

# 1. 운영 호스트·DB 이름 (팀 명명 규칙)
case "$cmd" in
  *eduhub-prod*|*prod-db*)
    block "운영 호스트·DB(eduhub-prod, prod-db)에는 접속하지 않습니다." \
          "로컬(localhost:5432/eduhub)이나 개발 환경에서 확인하세요. 운영 작업은 사람이 직접 합니다." ;;
esac

# 2. flywayClean: DB의 모든 객체를 지운다
case "$cmd" in
  *flywayClean*)
    block "flywayClean은 DB의 모든 테이블과 데이터를 지웁니다." \
          "스키마는 새 마이그레이션(V{다음 번호}__설명.sql)으로 바꾸세요. 로컬 DB 초기화가 필요하면 사람이 직접 합니다." ;;
esac

# git push 부분만 잘라 검사한다 (;, &, | 앞까지)
push_re='git[[:space:]]+push([^;&|]*)'
if [[ $cmd =~ $push_re ]]; then
  args=" ${BASH_REMATCH[1]} "

  # 3. 강제 푸시: --force(--force-with-lease 포함), -f
  if [[ $args =~ [[:space:]](--force|-f)([[:space:]=]|$) || $args == *" --force-with-lease"* ]]; then
    block "강제 푸시는 원격 이력을 덮어써 되돌리기 어렵습니다." \
          "새 커밋으로 고쳐 일반 git push를 쓰세요. 이력 정리가 꼭 필요하면 사람이 직접 합니다."
  fi

  # 4. main 직접 푸시: git push origin main, HEAD:main, main:main 등
  if [[ $args =~ [[:space:]:]main[[:space:]] ]]; then
    block "main에는 직접 푸시하지 않습니다. PR로만 병합합니다." \
          "작업 브랜치(<타입>/<짧은-설명>)에 푸시하고 PR을 만드세요."
  fi
fi

exit 0
