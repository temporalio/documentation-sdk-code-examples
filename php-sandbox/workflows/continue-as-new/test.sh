#!/usr/bin/env bash
# Runs the Workflow from the Continue-As-New page against a dev server and
# checks that it continues as new and carries its state into the next run.
# Needs the Temporal CLI, jq, and the RoadRunner binary at php-sandbox/rr
# (./vendor/bin/rr get). Run from anywhere:
#   workflows/continue-as-new/test.sh
set -euo pipefail

cd "$(dirname "$0")"
RR=../../rr
PORT=${TEMPORAL_TEST_PORT:-7299}
export TEMPORAL_ADDRESS="127.0.0.1:$PORT"
export RR_RPC_PORT=${RR_RPC_PORT:-6099}
LOG=$(mktemp -d)
JOBS=20

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

id=cluster-manager-$RANDOM
t workflow start --task-queue continue-as-new --type MessageHandlerWorkflowInterface --workflow-id "$id" \
  --execution-timeout 2m --input '{"testContinueAsNew": true}' >/dev/null
t workflow signal -w "$id" --name startCluster >/dev/null

# Each Update adds Events, so the test-only history limit of 50 triggers Continue-As-New.
for i in $(seq "$JOBS"); do
  t workflow update execute -w "$id" --name assignNodesToJob --input "\"job-$i\"" --input 1 >/dev/null
done

runs=$(t workflow list --query "WorkflowId = '$id'" --output json | jq -s 'flatten | length')
if [ "$runs" -gt 1 ]; then check continued-as-new ok ok; else check continued-as-new "more than 1 run" "$runs"; fi
check first-run-status WORKFLOW_EXECUTION_STATUS_CONTINUED_AS_NEW \
  "$(t workflow list --query "WorkflowId = '$id'" --output json | jq -rs 'flatten | sort_by(.startTime) | .[0].status')"

# State carries over: every job's node is still assigned in the latest run.
check state-carried-over "$JOBS" \
  "$(t workflow query -w "$id" --type getState --output json | jq '.queryResult[0].jobsAssigned | length')"

t workflow signal -w "$id" --name shutdownCluster >/dev/null
check result "$JOBS" "$(t workflow result -w "$id" --output json | jq -r '.result')"

if [ "$failures" -gt 0 ]; then echo "logs: $LOG"; exit 1; fi
