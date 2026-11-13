#!/usr/bin/env bash
# Runs the Workflow from the Cancellation page against a dev server and checks
# that the Activity receives the cancellation, cleans up, and ends canceled.
# Needs the Temporal CLI, jq, and the RoadRunner binary at php-sandbox/rr
# (./vendor/bin/rr get). Run from anywhere:
#   workflows/cancellation/test.sh
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

id=cancel-activity-$RANDOM
result=$(t workflow execute --execution-timeout 45s --task-queue cancellation --type CancelActivityWorkflow \
  --workflow-id "$id" --output json | jq -r '.result')
check result 'Activity canceled' "$result"

events=$(t workflow show -w "$id" --output json | jq -r '[.events[].eventType] | join(",")')
case "$events" in *ACTIVITY_TASK_CANCEL_REQUESTED*) check cancel-requested ok ok ;; *) check cancel-requested ACTIVITY_TASK_CANCEL_REQUESTED "$events" ;; esac
# WaitCancellationCompleted: the Activity acknowledged the cancellation before the Workflow completed.
case "$events" in *ACTIVITY_TASK_CANCELED*WORKFLOW_EXECUTION_COMPLETED*) check activity-canceled ok ok ;; *) check activity-canceled "ACTIVITY_TASK_CANCELED before completion" "$events" ;; esac

if grep -q "processItems canceled after" "$LOG/rr.log"; then check activity-cleanup ok ok; else check activity-cleanup "cleanup log line" missing; fi

if [ "$failures" -gt 0 ]; then echo "logs: $LOG"; exit 1; fi
