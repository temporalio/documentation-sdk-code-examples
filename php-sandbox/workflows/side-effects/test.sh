#!/usr/bin/env bash
# Runs the Workflow from the Side Effects page against a dev server and checks
# that the Side Effect result is recorded in the Event History as a Marker.
# Needs the Temporal CLI, jq, and the RoadRunner binary at php-sandbox/rr
# (./vendor/bin/rr get). Run from anywhere:
#   workflows/side-effects/test.sh
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

id=side-effect-$RANDOM
t workflow execute --execution-timeout 30s --task-queue side-effects --type SideEffectWorkflow --workflow-id "$id" >/dev/null
history=$(t workflow show -w "$id" --output json)
check completed EVENT_TYPE_WORKFLOW_EXECUTION_COMPLETED "$(jq -r '.events[-1].eventType' <<<"$history")"

marker=$(jq -c '.events[] | select(.eventType == "EVENT_TYPE_MARKER_RECORDED") | .markerRecordedEventAttributes' <<<"$history")
check marker-name SideEffect "$(jq -r '.markerName' <<<"$marker")"
# The recorded value is the JSON-encoded random number.
value=$(jq -r '.details.data.payloads[0].data' <<<"$marker" | base64 -d)
if [[ "$value" =~ ^[0-9]+$ ]] && [ "$value" -ge 0 ] && [ "$value" -le 100 ]; then
  check marker-value ok ok
else
  check marker-value "integer 0-100" "$value"
fi

if [ "$failures" -gt 0 ]; then echo "logs: $LOG"; exit 1; fi
