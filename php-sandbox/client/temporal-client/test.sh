#!/usr/bin/env bash
# Runs the Workflow interfaces from the Temporal Client page on a dev server
# and drives them with the Temporal CLI, then runs the Client tests: PHPStan
# on the connection scripts (which need the grpc extension to run) and
# test-client.php against a fake Temporal Service. Needs the Temporal CLI, jq,
# and the RoadRunner binary at php-sandbox/rr (./vendor/bin/rr get). Run from
# anywhere:
#   client/temporal-client/test.sh
set -euo pipefail

cd "$(dirname "$0")"
RR=../../rr
PORT=${TEMPORAL_TEST_PORT:-7299}
export TEMPORAL_ADDRESS="127.0.0.1:$PORT"
export RR_RPC_PORT=${RR_RPC_PORT:-6099}
LOG=$(mktemp -d)

temporal server start-dev --headless --port "$PORT" --ui-port $((PORT + 1000)) >"$LOG/server.log" 2>&1 &
SERVER=$!
RR_PID=
cleanup() {
  [ -n "$RR_PID" ] && kill "$RR_PID" 2>/dev/null || true
  kill "$SERVER" 2>/dev/null || true
  wait 2>/dev/null || true
}
trap cleanup EXIT

until temporal operator cluster health --address "$TEMPORAL_ADDRESS" >/dev/null 2>&1; do sleep 1; done

"$RR" serve -c .rr.yaml >"$LOG/rr.log" 2>&1 &
RR_PID=$!

failures=0
check() { # name expected actual
  if [ "$2" == "$3" ]; then echo "ok   $1"; else echo "FAIL $1: expected '$2', got '$3'"; failures=$((failures + 1)); fi
}
t() { temporal "$@" --address "$TEMPORAL_ADDRESS"; }

# account.transfer with its pay and finish Updates
tx=11111111-1111-4111-8111-111111111111
from=22222222-2222-4222-8222-222222222222
to=33333333-3333-4333-8333-333333333333
t workflow start --execution-timeout 1m --task-queue temporal-client --type account.transfer -w transfer-1 --input "\"$tx\"" >/dev/null
check pay-update 1 "$(t workflow update execute -w transfer-1 --name pay --input "\"$from\"" --input "\"$to\"" --input 100 --output json | jq -c '.result')"
t workflow update execute -w transfer-1 --name finish >/dev/null
expected=$(jq -ncS --arg tx "$tx" --arg move "100 $from -> $to" '{transaction: $tx, moves: [$move], outcome: "committed"}')
check account-transfer-result "$expected" "$(t workflow result -w transfer-1 --output json | jq -cS '.result')"

# MoneyTransfer returns a UUID
result=$(t workflow execute --execution-timeout 1m --task-queue temporal-client --type MoneyTransfer -w money-1 \
  --input '"fromID"' --input '"toID"' --input '"refID"' --input 1000 --output json | jq -r '.result')
if [[ "$result" =~ ^[0-9a-f-]{36}$ ]]; then check money-transfer-uuid ok ok; else check money-transfer-uuid uuid "$result"; fi

# Client code
cd ../..
if vendor/bin/phpstan analyse -c client/temporal-client/phpstan.neon --no-progress --memory-limit=1G >"$LOG/phpstan.log" 2>&1; then
  check phpstan-connection-scripts ok ok
else
  check phpstan-connection-scripts "no errors" "see $LOG/phpstan.log"
fi
if vendor/bin/yaml-lint client/temporal-client/rr-cloud.yaml >/dev/null 2>&1; then check rr-cloud-yaml ok ok; else check rr-cloud-yaml valid invalid; fi
if php client/temporal-client/test-client.php >"$LOG/test-client.log" 2>&1; then
  check test-client "$(grep -c '^ok' "$LOG/test-client.log") passed" "$(grep -c '^ok' "$LOG/test-client.log") passed"
else
  check test-client passes "see $LOG/test-client.log"
fi

if [ "$failures" -gt 0 ]; then echo "logs: $LOG"; exit 1; fi
