#!/usr/bin/env bash
# Runs the Workflows from the Activity Execution page against a dev server and
# checks their results and the Start-To-Close Timeout of each Activity.
# Needs the Temporal CLI, jq, and the RoadRunner binary at php-sandbox/rr
# (./vendor/bin/rr get). Run from anywhere:
#   activities/execution/test.sh
# The S3 Activities are tested by test-activities.php.
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
run() { # type id -> result
  # The Execution Timeout is longer than the 30-minute Activity timeout: the
  # Temporal Service caps an Activity's timeouts at the Workflow's timeout.
  t workflow execute --execution-timeout 1h --task-queue activity-execution --type "$1" -w "$2" --input '"World"' --output json | jq -c '.result'
}
timeouts() { # id -> Start-To-Close Timeout of each scheduled Activity
  t workflow show -w "$1" --output json \
    | jq -r '[.events[] | select(.eventType == "EVENT_TYPE_ACTIVITY_TASK_SCHEDULED") | .activityTaskScheduledEventAttributes.startToCloseTimeout] | join(",")'
}

check get-result '"Hello World"' "$(run GreetingWorkflowInterface greeting-1)"
check get-result-timeout 30s "$(timeouts greeting-1)"

check multiple-stubs '["Hello World","Goodbye World"]' "$(run MultipleStubsWorkflow stubs-1)"
check multiple-stubs-timeouts 30s,1800s "$(timeouts stubs-1)"

if [ "$failures" -gt 0 ]; then echo "logs: $LOG"; exit 1; fi
