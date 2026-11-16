#!/usr/bin/env bash
# Runs the Workflows from the Asynchronous Activity Completion page against a
# dev server. Checks that Workflow::async runs both coroutines in parallel, and
# that the async Activity stays open after its function returns until it's
# completed from outside. Needs the Temporal CLI, jq, and the RoadRunner binary
# at php-sandbox/rr (./vendor/bin/rr get). Run from anywhere:
#   activities/asynchronous-activity/test.sh
# The completion Client code is tested by test-client.php.
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

# Workflow::async: both coroutines start their first Activity in the same Workflow Task.
check parallel-result '"Hello World; Bye World\nHola World; Chao World"' \
  "$(t workflow execute --execution-timeout 1m --task-queue async-activity --type ParallelGreetingWorkflow -w parallel-1 \
    --input '"World"' --output json | jq -c '.result')"
first_batch=$(t workflow show -w parallel-1 --output json | jq -r '
  [.events[] | select(.eventType == "EVENT_TYPE_ACTIVITY_TASK_SCHEDULED") | .activityTaskScheduledEventAttributes]
  | group_by(.workflowTaskCompletedEventId | tonumber) | .[0] | map(.input.payloads[0].data | @base64d) | join(",")')
check parallel-first-batch '"Hello","Hola"' "$first_batch"

# Async completion: the Activity function returns, but the Activity stays open.
t workflow start --execution-timeout 1m --task-queue async-activity --type AsyncGreetingWorkflow -w async-1 --input '"World"' >/dev/null
for _ in $(seq 20); do grep -q "GreetingActivity token:" "$LOG/rr.log" && break; sleep 0.5; done
token=$(grep -o "GreetingActivity token: [A-Za-z0-9+/=]*" "$LOG/rr.log" | head -1 | cut -d' ' -f3)
if [ -n "$token" ]; then check token-logged ok ok; else check token-logged token missing; fi
sleep 2
pending=$(t workflow describe -w async-1 --output json | jq -r '.pendingActivities[0] | "\(.state) \(.activityId)"')
check activity-still-open PENDING_ACTIVITY_STATE_STARTED "${pending% *}"
check workflow-waiting WORKFLOW_EXECUTION_STATUS_RUNNING \
  "$(t workflow describe -w async-1 --output json | jq -r '.workflowExecutionInfo.status')"

# Complete it from outside the Worker.
t activity complete -w async-1 --activity-id "${pending#* }" --result '"Hello World, completed externally"' >/dev/null
check async-result '"Hello World, completed externally"' "$(t workflow result -w async-1 --output json | jq -c '.result')"

if [ "$failures" -gt 0 ]; then echo "logs: $LOG"; exit 1; fi
