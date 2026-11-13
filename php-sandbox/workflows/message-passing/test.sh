#!/usr/bin/env bash
# Runs the Workflows from the Workflow message passing page against a dev
# server and drives their Signal, Query, and Update handlers with the Temporal
# CLI. Needs the Temporal CLI, jq, and the RoadRunner binary at php-sandbox/rr
# (./vendor/bin/rr get). Run from anywhere:
#   workflows/message-passing/test.sh
# The Client code on the page is tested by test-client.php.
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
start() { # task-queue type id [input]
  t workflow start --execution-timeout 2m --task-queue "$1" --type "$2" --workflow-id "$3" ${4:+--input "$4"} >/dev/null
}
result() { t workflow result -w "$1" --output json | jq -c '.result'; }
query() { # id type [input]
  t workflow query -w "$1" --type "$2" ${3:+--input "$3"} --output json | jq -c '.queryResult[0]'
}
update() { # id name [input...]
  local id=$1 name=$2; shift 2
  local args=()
  for i in "$@"; do args+=(--input "$i"); done
  t workflow update execute -w "$id" --name "$name" ${args[@]+"${args[@]}"} --output json 2>&1 | jq -c '.result' 2>/dev/null || true
}
update_error() { # id name [input] -> error text
  t workflow update execute -w "$1" --name "$2" ${3:+--input "$3"} 2>&1 >/dev/null | tr '\n' ' ' || true
}
contains() { # name needle haystack
  case "$3" in *"$2"*) check "$1" ok ok ;; *) check "$1" "contains '$2'" "$3" ;; esac
}
event_types() { t workflow show -w "$1" --output json | jq -r '[.events[].eventType] | join(",")'; }

# Define a Signal / Define a Query
start mp-define FileProcessingWorkflow define-1 '{"sourceUrl":"s3://in"}'
# Let the main method record "started" first. Handlers run before the main method
# when a Signal arrives with the first Workflow Task.
sleep 1
t workflow signal -w define-1 --name retryNow >/dev/null
check define-status '"processing"' "$(query define-1 status)"
t workflow signal -w define-1 --name abandon >/dev/null
check define-result '["started s3://in","retried","abandoned"]' "$(result define-1)"
check define-history-query '["started s3://in","retried","abandoned"]' "$(query define-1 history)"

# Handle a Signal
start mp-handle-signal YourWorkflow signal-1
t workflow signal -w signal-1 --name setValue --input true >/dev/null
check handle-signal '"OK"' "$(result signal-1)"

# Send a Signal from a Workflow (typed and untyped External Workflow stubs)
start mp-handle-signal YourWorkflow signal-2
t workflow execute --execution-timeout 1m --task-queue mp-handle-signal --type SignalSenderWorkflow -w sender-1 --input '"signal-2"' >/dev/null
check signal-from-workflow '"OK"' "$(result signal-2)"
start mp-handle-signal YourWorkflow signal-3
t workflow execute --execution-timeout 1m --task-queue mp-handle-signal --type UntypedSignalSenderWorkflow -w sender-2 --input '"signal-3"' >/dev/null
check untyped-signal-from-workflow '"OK"' "$(result signal-3)"

# Handle a Query (the Workflow has already completed)
t workflow execute --execution-timeout 1m --task-queue mp-handle-query --type YourWorkflow -w query-1 >/dev/null
check handle-query 42 "$(query query-1 getValue)"

# Query the current state
start mp-query-current-state YourWorkflow query-2
sleep 1
check query-current-state '"waiting timer"' "$(query query-2 current_state)"
t workflow terminate -w query-2 >/dev/null

# Define an Update
start mp-define-update MyWorkflowInterface update-1
check define-update '"updated to v"' "$(update update-1 myUpdate '"v"')"
check define-update-result '"v"' "$(result update-1)"

# Handle an Update
start mp-handle-update FileProcessingWorkflow update-2 '{"name":"f.txt"}'
check handle-update null "$(update update-2 pauseProcessing)"
check handle-update-result '{"fileName":"f.txt","paused":true}' "$(result update-2)"

# Custom Update name
start mp-update-custom-name FileProcessingWorkflow update-3 '{"names":["a","b"]}'
check update-custom-name null "$(update update-3 pause)"
check update-custom-name-result 2 "$(result update-3)"

# Register an Update handler dynamically
start mp-register-update PausableWorkflow update-4
check register-update true "$(update update-4 pause)"
check register-update-result '"paused"' "$(result update-4)"

# Validate an Update
start mp-validate-update GreetingWorkflow update-5
contains validate-update-rejected 'Name must not be empty' "$(update_error update-5 addGreeting '""')"
check validate-update-accepted 1 "$(update update-5 addGreeting '"World"')"
uuid=$(update update-5 storeGreetings)
if [[ "$uuid" =~ ^\"[0-9a-f-]{36}\"$ ]]; then check store-greetings-uuid ok ok; else check store-greetings-uuid uuid "$uuid"; fi
check validate-update-second 2 "$(update update-5 addGreeting '"Temporal"')"
check validate-update-result '["Hello, World!","Hello, Temporal!"]' "$(result update-5)"
events=$(event_types update-5)
case "$events" in *UPDATE_ACCEPTED*) ;; esac
check rejected-update-not-in-history 3 "$(grep -o UPDATE_ACCEPTED <<<"$events" | wc -l | tr -d ' ')"

# Update-With-Start
uws() { # id update-name [update inputs...]
  local id=$1 name=$2; shift 2
  local args=()
  for i in "$@"; do args+=(--update-input "$i"); done
  t workflow execute-update-with-start --task-queue mp-update-with-start --type ShoppingCartWorkflow -w "$id" \
    --id-conflict-policy UseExisting --update-name "$name" "${args[@]}" --output json | jq -c '.updateResult // .result'
}
check update-with-start-new 20 "$(uws cart-1 addItem '"apple"' 2)"
check update-with-start-existing 30 "$(uws cart-1 addItem '"apple"' 1)"
check update-with-start-one-run 1 "$(t workflow list --query "WorkflowId = 'cart-1'" --output json | jq -s 'flatten | length')"
t workflow signal -w cart-1 --name checkout >/dev/null
check update-with-start-result '{"apple":3}' "$(result cart-1)"

# Use wait conditions in handlers
start mp-wait-in-handler MyWorkflow wait-1
t workflow update start -w wait-1 --name myUpdate --update-id u1 --wait-for-stage accepted --input '{"value":"v"}' >/dev/null
sleep 1
check wait-in-handler-blocked WORKFLOW_EXECUTION_STATUS_RUNNING \
  "$(t workflow describe -w wait-1 --output json | jq -r '.workflowExecutionInfo.status')"
t workflow signal -w wait-1 --name markReady >/dev/null
check wait-in-handler-update "$(printf '%s' '"v"')" "$(t workflow update result -w wait-1 --update-id u1 --output json | jq -c '.result')"
check wait-in-handler-result '"v"' "$(result wait-1)"

# Wait for all handlers to finish. Update-With-Start runs the Update handler
# before the main method, which returns right away unless it waits.
check all-handlers-update '"update-result"' "$(t workflow execute-update-with-start --task-queue mp-all-handlers-finished \
  --type MyWorkflow -w finish-1 --id-conflict-policy Fail --update-name slowUpdate --output json | jq -c '.updateResult // .result')"
check all-handlers-result '"workflow-result"' "$(result finish-1)"
case "$(event_types finish-1)" in
  *UPDATE_COMPLETED*WORKFLOW_EXECUTION_COMPLETED*) check all-handlers-order ok ok ;;
  *) check all-handlers-order "UPDATE_COMPLETED before WORKFLOW_EXECUTION_COMPLETED" "$(event_types finish-1)" ;;
esac

# Unfinished handler policy: the Workflow returns while myUpdate runs, and the
# Worker doesn't log the unfinished-handler warning.
t workflow start-update-with-start --task-queue mp-unfinished-policy --type MyWorkflow -w unfinished-1 \
  --id-conflict-policy Fail --update-name myUpdate --update-wait-for-stage accepted >/dev/null 2>&1 || true
check unfinished-result '"workflow-result"' "$(result unfinished-1)"
sleep 1
if grep -q "while update handlers are still running" "$LOG/rr.log"; then
  check unfinished-no-warning "no warning" "warning logged"
else
  check unfinished-no-warning ok ok
fi

# #[WorkflowInit]
start mp-workflow-init GreetingExample init-1 '"Temporal"'
check workflow-init-update true "$(update init-1 checkTitleValidity)"
check workflow-init-result '"Hello Sir Temporal"' "$(result init-1)"

# Handler concurrency
for wf in BadWorkflow SafeWorkflow; do
  id=concurrency-$wf
  handler=$([ "$wf" = BadWorkflow ] && echo badAsyncHandler || echo safeAsyncHandler)
  start mp-handler-concurrency "$wf" "$id"
  t workflow signal -w "$id" --name "$handler" >/dev/null
  t workflow signal -w "$id" --name "$handler" >/dev/null
  sleep 5
  xy=$(query "$id" getXY)
  check "$wf-ran" true "$(jq -r '.[0] != null and .[1] != null' <<<"$xy")"
  [ "$wf" = SafeWorkflow ] && check safe-handler-consistent true "$(jq -r '(.[0] | ltrimstr("x")) == (.[1] | ltrimstr("y"))' <<<"$xy")"
  case "$(event_types "$id")" in *WORKFLOW_TASK_FAILED*) check "$wf-no-task-failure" none failed ;; *) check "$wf-no-task-failure" ok ok ;; esac
  t workflow terminate -w "$id" >/dev/null
done

# Dynamic handlers
start mp-dynamic DynamicHandlersWorkflow dynamic-1
sleep 1
check dynamic-query '"Got query `anything` with 1 arguments"' "$(query dynamic-1 anything '"x"')"
t workflow signal -w dynamic-1 --name someSignal >/dev/null
check dynamic-update '"Got update `update_x` with 0 arguments"' "$(update dynamic-1 update_x)"
contains dynamic-update-rejected 'Invalid update name' "$(update_error dynamic-1 other)"
t workflow signal -w dynamic-1 --name finish >/dev/null
result dynamic-1 >/dev/null
if grep -q 'Executed signal `someSignal` with 0 arguments' "$LOG/rr.log"; then check dynamic-signal ok ok; else check dynamic-signal "log line" missing; fi

if [ "$failures" -gt 0 ]; then echo "logs: $LOG"; exit 1; fi
