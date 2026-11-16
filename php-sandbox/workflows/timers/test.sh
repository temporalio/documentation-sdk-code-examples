#!/usr/bin/env bash
# Runs the Workflow from the Timers page against a dev server and checks that it
# starts a 5-minute Timer. Needs the Temporal CLI, jq, and the RoadRunner binary
# at php-sandbox/rr (./vendor/bin/rr get). Run from anywhere:
#   workflows/timers/test.sh
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

id=timer-$RANDOM
t workflow start --execution-timeout 1m --task-queue timers --type TimerWorkflow --workflow-id "$id" >/dev/null
timer=
for _ in $(seq 20); do
  timer=$(t workflow show -w "$id" --output json \
    | jq -r '.events[] | select(.eventType == "EVENT_TYPE_TIMER_STARTED") | .timerStartedEventAttributes.startToFireTimeout')
  [ -n "$timer" ] && break
  sleep 0.5
done
check timer-duration 300s "$timer"
check still-sleeping WORKFLOW_EXECUTION_STATUS_RUNNING \
  "$(t workflow describe -w "$id" --output json | jq -r '.workflowExecutionInfo.status')"
t workflow terminate -w "$id" >/dev/null

if [ "$failures" -gt 0 ]; then echo "logs: $LOG"; exit 1; fi
