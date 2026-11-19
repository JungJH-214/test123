#!/usr/bin/env bash
# PreToolUse(Edit|Write) 훅: 사람이 관리해야 하는 파일을 Claude가 고치지 못하게 막는다.
# 막을 때는 stderr에 "이유 + 대신 할 일"을 쓰고 exit 2로 끝낸다. 이 메시지는 Claude에게 전달된다.

input=$(cat)

if ! command -v jq >/dev/null 2>&1; then
  echo "protect-files: jq가 없어 보호 규칙을 검사하지 못했습니다" >&2
  exit 0
fi
file=$(printf '%s' "$input" | jq -r '.tool_input.file_path // empty' 2>/dev/null)
[ -n "$file" ] || exit 0

path="${file//\\//}"   # C:\a\b → C:/a/b (Windows 경로를 / 구분자로)
name="${path##*/}"

block() {
  echo "차단: $1" >&2
  echo "대신: $2" >&2
  exit 2
}

case "$name" in
  application-prod.yml)
    block "운영 설정은 사람이 PR로만 수정합니다." "개발 설정은 application-dev.yml을 수정하세요." ;;
  .env.example) ;;  # 허용
  .env|.env.*)
    block "비밀 파일은 수정하지 않습니다." ".env.example에 변수 이름만 추가하세요." ;;
esac

case "$path" in
  */frontend/packages/api-client/src/generated/*)
    block "생성 코드입니다." "cd frontend && npm run generate:api 로 다시 생성하세요." ;;
  */db/migration/V*.sql|*/db/vendor/*/V*.sql)
    if [ -e "$path" ]; then
      block "기존 마이그레이션은 수정 금지입니다." "새 버전 파일(V{다음 번호}__설명.sql)을 추가하세요."
    fi ;;
esac

exit 0
