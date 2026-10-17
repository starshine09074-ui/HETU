#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if [[ -f /tmp/hetu-backend.pid ]]; then
  backend_pid="$(cat /tmp/hetu-backend.pid)"
  if [[ -r "/proc/${backend_pid}/cmdline" ]] && tr '\0' ' ' < "/proc/${backend_pid}/cmdline" | grep -Fq 'java -jar ruoyi-admin/target/ruoyi-admin.jar'; then
    kill "$backend_pid"
    for attempt in $(seq 1 15); do
      kill -0 "$backend_pid" 2>/dev/null || break
      sleep 1
    done
  fi
  rm -f /tmp/hetu-backend.pid
fi

mvn -B -DskipTests package

export HETU_TOKEN_SECRET="$(openssl rand -hex 64)"
export HETU_DB_URL="${HETU_DB_URL:-jdbc:mysql://mysql:3306/ry-vue?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=GMT%2B8}"
export HETU_DB_USERNAME="${HETU_DB_USERNAME:-root}"
export HETU_DB_PASSWORD="${HETU_DB_PASSWORD:-hetu-local-only}"
export HETU_REDIS_HOST="${HETU_REDIS_HOST:-redis}"
export HETU_CORS_ALLOWED_ORIGINS="${HETU_CORS_ALLOWED_ORIGINS:-http://localhost:5173}"
if [[ -n "${CODESPACE_NAME:-}" ]]; then
  export HETU_CORS_ALLOWED_ORIGINS="${HETU_CORS_ALLOWED_ORIGINS},https://${CODESPACE_NAME}-5173.app.github.dev"
fi

if ! curl --silent --fail http://127.0.0.1:8080/captchaImage >/dev/null; then
  nohup java -jar ruoyi-admin/target/ruoyi-admin.jar \
    > /tmp/hetu-backend.log 2>&1 &
  echo $! > /tmp/hetu-backend.pid
fi

if ! curl --silent --fail http://127.0.0.1:5173/ >/dev/null; then
  nohup npm --prefix hetu-ui run dev -- --host 0.0.0.0 \
    > /tmp/hetu-frontend.log 2>&1 &
fi

for attempt in $(seq 1 90); do
  if curl --silent --fail http://127.0.0.1:8080/captchaImage >/dev/null; then
    break
  fi
  sleep 2
done
curl --silent --fail http://127.0.0.1:8080/captchaImage >/dev/null || {
  tail -80 /tmp/hetu-backend.log
  exit 1
}

for attempt in $(seq 1 30); do
  if curl --silent --fail http://127.0.0.1:5173/ >/dev/null; then
    break
  fi
  sleep 1
done
curl --silent --fail http://127.0.0.1:5173/ >/dev/null
echo "Hetu is ready: frontend http://localhost:5173; backend is private on port 8080."
