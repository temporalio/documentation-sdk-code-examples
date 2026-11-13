#!/usr/bin/env bash
# Runs each Workflow from the Workflow basics page against a dev server and
# checks the result. Needs the Temporal CLI and the RoadRunner binary at
# php-sandbox/rr (./vendor/bin/rr get). Run from anywhere:
#   workflows/basics/test.sh
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
run() { # task-queue type input
  temporal workflow execute --address "$TEMPORAL_ADDRESS" --task-queue "$1" --type "$2" \
    --workflow-id "$1-$RANDOM" --input "$3" --output json | jq -r '.result'
}
input='{"sourceUrl":"s3://in","destinationUrl":"s3://out"}'

check define 'Processed s3://in to s3://out' "$(run workflow-basics-define FileProcessingWorkflow "$input")"
check return-type 'Processed s3://in' "$(run workflow-basics-return-type FileProcessingWorkflow "$input")"
check custom-name 'Processed s3://in' "$(run workflow-basics-custom-name FileProcessing "$input")"
check default-name 'Processed s3://in' "$(run workflow-basics-default-name YourWorkflowDefinitionInterface "$input")"

# GreetingExample waits for titleHasBeenChecked, which nothing sets, so check
# that the first Workflow Task completed and the Workflow is still waiting. An
# exception in the constructor fails the Workflow Execution.
id=workflow-init-$RANDOM
temporal workflow start --address "$TEMPORAL_ADDRESS" --task-queue workflow-basics-workflow-init \
  --type GreetingExample --workflow-id "$id" --input '"Temporal"' >/dev/null
for _ in $(seq 20); do
  events=$(temporal workflow show --address "$TEMPORAL_ADDRESS" -w "$id" --output json | jq -r '[.events[].eventType] | join(",")')
  case "$events" in *WORKFLOW_TASK_COMPLETED*|*WORKFLOW_TASK_FAILED*) break ;; esac
  sleep 0.5
done
status=$(temporal workflow describe --address "$TEMPORAL_ADDRESS" -w "$id" --output json | jq -r '.workflowExecutionInfo.status')
case "$events" in
  *WORKFLOW_TASK_COMPLETED*) check workflow-init WORKFLOW_EXECUTION_STATUS_RUNNING "$status" ;;
  *) check workflow-init WORKFLOW_TASK_COMPLETED "$events" ;;
esac
temporal workflow terminate --address "$TEMPORAL_ADDRESS" -w "$id" >/dev/null 2>&1 || true

if [ "$failures" -gt 0 ]; then echo "logs: $LOG"; exit 1; fi
