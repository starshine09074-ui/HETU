#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

# postStartCommand and postAttachCommand can both run for one Codespaces
# startup. Serialize them and make the checks below safe to repeat.
exec 9>/tmp/hetu-start-dev.lock
flock 9

export HETU_TOKEN_SECRET="$(openssl rand -hex 64)"
export HETU_DB_URL="${HETU_DB_URL:-jdbc:mysql://mysql:3306/ry-vue?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=GMT%2B8}"
export HETU_DB_USERNAME="${HETU_DB_USERNAME:-root}"
export HETU_DB_PASSWORD="${HETU_DB_PASSWORD:-hetu-local-only}"
export HETU_REDIS_HOST="${HETU_REDIS_HOST:-redis}"
export HETU_CORS_ALLOWED_ORIGINS="${HETU_CORS_ALLOWED_ORIGINS:-http://localhost:5173}"
if [[ -n "${CODESPACE_NAME:-}" ]]; then
  export HETU_CORS_ALLOWED_ORIGINS="${HETU_CORS_ALLOWED_ORIGINS},https://${CODESPACE_NAME}-5173.app.github.dev"
fi

if ! curl --silent --fail http://127.0.0.1:5173/ >/dev/null; then
  if [[ ! -x hetu-ui/node_modules/.bin/vite ]]; then
    npm --prefix hetu-ui ci
  fi
  nohup npm --prefix hetu-ui run dev -- --host 0.0.0.0 \
    > /tmp/hetu-frontend.log 2>&1 &
fi

if [[ ! -f ruoyi-admin/target/ruoyi-admin.jar ]]; then
  mvn -B -DskipTests package
fi

captcha_response="$(curl --silent --show-error --max-time 2 http://127.0.0.1:8080/captchaImage 2>/dev/null || true)"
if [[ "$captcha_response" != *'"captchaEnabled"'* ]]; then
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
  nohup java -jar ruoyi-admin/target/ruoyi-admin.jar \
    > /tmp/hetu-backend.log 2>&1 &
  echo $! > /tmp/hetu-backend.pid
fi

for attempt in $(seq 1 30); do
  if curl --silent --fail http://127.0.0.1:5173/ >/dev/null; then
    break
  fi
  sleep 1
done
curl --silent --fail http://127.0.0.1:5173/ >/dev/null

for attempt in $(seq 1 90); do
  captcha_response="$(curl --silent --max-time 2 http://127.0.0.1:8080/captchaImage 2>/dev/null || true)"
  if [[ "$captcha_response" == *'"captchaEnabled"'* ]]; then
    echo "Hetu is ready: frontend http://localhost:5173; backend is private on port 8080."
    exit 0
  fi
  sleep 2
done
tail -80 /tmp/hetu-backend.log
exit 1
