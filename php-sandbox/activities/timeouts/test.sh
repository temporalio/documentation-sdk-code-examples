#!/usr/bin/env bash
# Runs the Workflows and Activities from the Activity Timeouts page against a
# dev server. Checks the timeouts and Retry Policy each Activity is scheduled
# with, that a non-retryable exception isn't retried, that nextRetryDelay
# replaces the Retry Policy's interval, and that Heartbeats keep a long
# download alive past its Heartbeat Timeout. Needs the Temporal CLI, jq, and
# the RoadRunner binary at php-sandbox/rr (./vendor/bin/rr get). Run from
# anywhere:
#   activities/timeouts/test.sh
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
run() { # type id [input] -> result
  t workflow execute --execution-timeout 2m --task-queue activity-timeouts --type "$1" -w "$2" ${3:+--input "$3"} --output json | jq -c '.result'
}
scheduled() { # id jq-filter -> value from the first ActivityTaskScheduled event
  t workflow show -w "$1" --output json \
    | jq -c "[.events[] | select(.eventType == \"EVENT_TYPE_ACTIVITY_TASK_SCHEDULED\") | .activityTaskScheduledEventAttributes][0] | $2"
}

# Schedule-To-Close Timeout
check timeout-result '"Hello World"' "$(run TimeoutWorkflow timeout-1 '"World"')"
check schedule-to-close '"2s"' "$(scheduled timeout-1 .scheduleToCloseTimeout)"

# Retry Policy
check retry-result '"Hello World"' "$(run RetryPolicyWorkflow retry-1 '"World"')"
check retry-policy '{"initialInterval":"1s","maximumAttempts":5,"nonRetryableErrorTypes":["InvalidArgumentException"]}' \
  "$(scheduled retry-1 '.retryPolicy | {initialInterval, maximumAttempts, nonRetryableErrorTypes}')"
check non-retryable-result '"failed: InvalidArgumentException"' "$(run RetryPolicyWorkflow retry-2 '""')"
check non-retryable-attempts 1 "$(t workflow show -w retry-2 --output json \
  | jq '[.events[] | select(.eventType == "EVENT_TYPE_ACTIVITY_TASK_STARTED")][0].activityTaskStartedEventAttributes.attempt')"

# nextRetryDelay: attempts 1 and 2 fail and ask for 3 and 6 seconds instead of
# the Retry Policy's 60.
start=$(date +%s)
check next-retry-delay-result 3 "$(run RetryDelayWorkflow delay-1)"
elapsed=$(( $(date +%s) - start ))
if [ "$elapsed" -ge 8 ] && [ "$elapsed" -lt 30 ]; then check next-retry-delay-elapsed ok ok; else check next-retry-delay-elapsed "8-29s" "${elapsed}s"; fi

# Heartbeats: the 5-second download outlives the 2-second Heartbeat Timeout.
t workflow start --execution-timeout 1m --task-queue activity-timeouts --type DownloadWorkflow -w download-1 >/dev/null
sleep 3
progress=$(t workflow describe -w download-1 --output json | jq -c '.pendingActivities[0].heartbeatDetails')
if [[ "$progress" =~ ^\[[0-9]+\]$ ]]; then check heartbeat-details ok ok; else check heartbeat-details "[<progress>]" "$progress"; fi
check heartbeat-result '"downloaded"' "$(t workflow result -w download-1 --output json | jq -c '.result')"

if [ "$failures" -gt 0 ]; then echo "logs: $LOG"; exit 1; fi
