#!/usr/bin/env bash
# Runs the Workflows from the Enriching the UI page against a dev server and
# checks the Timer and Activity summaries recorded in the Event History, then
# runs test-client.php for the Client code. Needs the Temporal CLI, jq, and the
# RoadRunner binary at php-sandbox/rr (./vendor/bin/rr get). Run from anywhere:
#   platform/enriching-ui/test.sh
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
summary() { # id event-type attributes-field -> decoded summary of the first matching event
  t workflow show -w "$1" --output json \
    | jq -r --arg type "$2" '[.events[] | select(.eventType == $type)][0].userMetadata.summary.data // empty | @base64d'
}

# Timer summary: the 5-minute Timer starts with its summary; the test doesn't wait for it.
t workflow start --execution-timeout 1m --task-queue enriching-ui-timer --type YourWorkflow -w timer-1 --input '"workflow input"' >/dev/null
timer=
for _ in $(seq 20); do
  timer=$(summary timer-1 EVENT_TYPE_TIMER_STARTED)
  [ -n "$timer" ] && break
  sleep 0.5
done
check timer-summary '"Waiting for payment confirmation"' "$timer"
t workflow terminate -w timer-1 >/dev/null

# Activity summary
check activity-result '"processed workflow input"' "$(t workflow execute --execution-timeout 1m --task-queue enriching-ui-activity \
  --type YourWorkflow -w activity-1 --input '"workflow input"' --output json | jq -c '.result')"
check activity-summary '"Processing user data"' "$(summary activity-1 EVENT_TYPE_ACTIVITY_TASK_SCHEDULED)"

# Client code
if php ../../platform/enriching-ui/test-client.php >"$LOG/test-client.log" 2>&1; then
  check test-client ok ok
else
  check test-client passes "see $LOG/test-client.log"
fi

if [ "$failures" -gt 0 ]; then echo "logs: $LOG"; exit 1; fi
