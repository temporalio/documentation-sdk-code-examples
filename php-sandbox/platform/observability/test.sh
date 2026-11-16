#!/usr/bin/env bash
# Runs the Workflows from the Observability page against a dev server with the
# DestinationTime and OrderId Search Attributes registered. Checks the Workflow
# logger with the default and custom loggers, and the Search Attribute Updates.
# Then runs test-client.php for the Client code. Needs the Temporal CLI, jq,
# and the RoadRunner binary at php-sandbox/rr (./vendor/bin/rr get). Run from
# anywhere:
#   platform/observability/test.sh
set -euo pipefail

cd "$(dirname "$0")"
RR=../../rr
PORT=${TEMPORAL_TEST_PORT:-7299}
export TEMPORAL_ADDRESS="127.0.0.1:$PORT"
export RR_RPC_PORT=${RR_RPC_PORT:-6099}
export RR_RPC_PORT_2=$((RR_RPC_PORT + 1))
LOG=$(mktemp -d)

temporal server start-dev --headless --port "$PORT" --ui-port $((PORT + 1000)) \
  --search-attribute DestinationTime=Datetime --search-attribute OrderId=Keyword >"$LOG/server.log" 2>&1 &
SERVER=$!
PIDS=()
cleanup() {
  for p in ${PIDS[@]+"${PIDS[@]}"}; do kill "$p" 2>/dev/null || true; done
  kill "$SERVER" 2>/dev/null || true
  wait 2>/dev/null || true
}
trap cleanup EXIT

until temporal operator cluster health --address "$TEMPORAL_ADDRESS" >/dev/null 2>&1; do sleep 1; done

"$RR" serve -c .rr.yaml >"$LOG/rr.log" 2>&1 &
PIDS+=($!)
"$RR" serve -c custom-logger.rr.yaml >"$LOG/rr-custom-logger.log" 2>&1 &
PIDS+=($!)

failures=0
check() { # name expected actual
  if [ "$2" == "$3" ]; then echo "ok   $1"; else echo "FAIL $1: expected '$2', got '$3'"; failures=$((failures + 1)); fi
}
contains() { # name needle file
  if grep -q -- "$2" "$3"; then check "$1" ok ok; else check "$1" "log line '$2'" missing; fi
}
t() { temporal "$@" --address "$TEMPORAL_ADDRESS"; }
run() { # task-queue type id [input]
  t workflow execute --execution-timeout 1m --task-queue "$1" --type "$2" -w "$3" ${4:+--input "$4"} --output json 2>&1 | jq -c '.result' 2>/dev/null || true
}
destination_time() { # id -> DestinationTime Search Attribute, or null
  t workflow describe -w "$1" --output json \
    | jq -r '.workflowExecutionInfo.searchAttributes.indexedFields.DestinationTime.data // empty | @base64d' || true
}

# Default logger, with logging in replay enabled
check default-logger-result '"Done"' "$(run your-task-queue MyWorkflow log-1 '"my-param"')"
sleep 1
contains default-logger-started 'Workflow started' "$LOG/rr.log"
contains default-logger-context 'parameter.\{1,3\}:.\{1,3\}my-param' "$LOG/rr.log"
contains default-logger-completed 'Workflow completed' "$LOG/rr.log"

# Custom logger
check custom-logger-result '"Done"' "$(run my-task-queue MyWorkflow log-2 '"my-param"')"
sleep 1
contains custom-logger-started '\[MyLogger\] INFO Workflow started' "$LOG/rr-custom-logger.log"
contains custom-logger-completed '\[MyLogger\] INFO Workflow completed' "$LOG/rr-custom-logger.log"

# Search Attributes: start with them set, postpone DestinationTime, then remove it.
t workflow start --execution-timeout 1m --task-queue observability-orders --type OrderWorkflowInterface -w order-1 \
  --search-attribute 'OrderId="order-42"' --search-attribute 'DestinationTime="2028-11-05T00:10:07Z"' >/dev/null
check initial-destination-time '"2028-11-05T00:10:07Z"' "$(destination_time order-1)"
t workflow update execute -w order-1 --name postponeDestinationTime --input '"PT1H"' >/dev/null
check postponed-destination-time '"2028-11-05T01:10:07Z"' "$(destination_time order-1)"
t workflow update execute -w order-1 --name unsetDestinationTime >/dev/null
check removed-destination-time '' "$(destination_time order-1)"
check order-id-kept 'order-1' "$(t workflow list --query 'OrderId = "order-42"' --output json | jq -rs 'flatten | .[0].execution.workflowId')"
t workflow signal -w order-1 --name deliver >/dev/null

# Client code
if php ../../platform/observability/test-client.php >"$LOG/test-client.log" 2>&1; then
  check test-client ok ok
else
  check test-client passes "see $LOG/test-client.log"
fi

if [ "$failures" -gt 0 ]; then echo "logs: $LOG"; exit 1; fi
