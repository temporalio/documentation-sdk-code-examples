#!/usr/bin/env bash
# Runs each Workflow from the Child Workflows page against a dev server and
# checks the result. Needs the Temporal CLI, jq, and the RoadRunner binary at
# php-sandbox/rr (./vendor/bin/rr get). Run from anywhere:
#   workflows/child-workflows/test.sh
set -euo pipefail

cd "$(dirname "$0")"
RR=../../rr
PORT=${TEMPORAL_TEST_PORT:-7299}
export TEMPORAL_ADDRESS="127.0.0.1:$PORT"
export RR_RPC_PORT=${RR_RPC_PORT:-6099}
LOG=$(mktemp -d)
TQ=child-workflows

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
run() { # type -> result
  t workflow execute --execution-timeout 30s --task-queue "$TQ" --type "$1" --workflow-id "$1-$RANDOM" --output json | jq -r '.result'
}

check stub 'Child received value' "$(run StubParentWorkflow)"
check stub-child-status WORKFLOW_EXECUTION_STATUS_COMPLETED \
  "$(t workflow describe -w BID-SIMPLE-CHILD-WORKFLOW --output json | jq -r '.workflowExecutionInfo.status')"

# Both parents use the fixed child ID BID-SIMPLE-CHILD-WORKFLOW. The default
# Workflow ID reuse policy for Child Workflows is AllowDuplicateFailedOnly, so
# delete the completed child before reusing its ID.
t workflow delete --yes -w BID-SIMPLE-CHILD-WORKFLOW >/dev/null
while t workflow describe -w BID-SIMPLE-CHILD-WORKFLOW >/dev/null 2>&1; do sleep 0.5; done

check execute 'Child received args' "$(run ExecuteParentWorkflow)"

# The abandoned child keeps running after its parent completes.
id=AbandonParentWorkflow-$RANDOM
t workflow execute --execution-timeout 30s --task-queue "$TQ" --type AbandonParentWorkflow --workflow-id "$id" >/dev/null
history=$(t workflow show -w "$id" --output json)
check abandon-policy PARENT_CLOSE_POLICY_ABANDON \
  "$(jq -r '.events[] | select(.eventType == "EVENT_TYPE_START_CHILD_WORKFLOW_EXECUTION_INITIATED") | .startChildWorkflowExecutionInitiatedEventAttributes.parentClosePolicy' <<<"$history")"
child=$(jq -r '.events[] | select(.eventType == "EVENT_TYPE_CHILD_WORKFLOW_EXECUTION_STARTED") | .childWorkflowExecutionStartedEventAttributes.workflowExecution.workflowId' <<<"$history")
check abandon-child-running WORKFLOW_EXECUTION_STATUS_RUNNING \
  "$(t workflow describe -w "$child" --output json | jq -r '.workflowExecutionInfo.status')"
t workflow terminate -w "$child" >/dev/null 2>&1 || true

if [ "$failures" -gt 0 ]; then echo "logs: $LOG"; exit 1; fi
