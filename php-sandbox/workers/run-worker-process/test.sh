#!/usr/bin/env bash
# Runs the Worker code from the "Run a Worker" page against a dev server:
#  - worker.php under RoadRunner with the page's .rr.yaml (its address and RPC
#    port overridden to point at the dev server),
#  - examples-worker.php, built from the other examples, with its own config.
# Then runs a Workflow on each Task Queue with the Temporal CLI. Needs the
# Temporal CLI, jq, and the RoadRunner binary at php-sandbox/rr
# (./vendor/bin/rr get). Run from anywhere:
#   workers/run-worker-process/test.sh
set -euo pipefail

cd "$(dirname "$0")"
SANDBOX=$(cd ../.. && pwd)
RR=$SANDBOX/rr
PORT=${TEMPORAL_TEST_PORT:-7299}
export TEMPORAL_ADDRESS="127.0.0.1:$PORT"
export RR_RPC_PORT=${RR_RPC_PORT:-6099}
LOG=$(mktemp -d)

temporal server start-dev --headless --port "$PORT" --ui-port $((PORT + 1000)) >"$LOG/server.log" 2>&1 &
SERVER=$!
PIDS=()
cleanup() {
  for p in ${PIDS[@]+"${PIDS[@]}"}; do kill "$p" 2>/dev/null || true; done
  kill "$SERVER" 2>/dev/null || true
  wait 2>/dev/null || true
}
trap cleanup EXIT

until temporal operator cluster health --address "$TEMPORAL_ADDRESS" >/dev/null 2>&1; do sleep 1; done

# worker.php includes "vendor/autoload.php" relative to the working directory,
# so run it from php-sandbox/.
(cd "$SANDBOX" && exec "$RR" serve -c workers/run-worker-process/.rr.yaml -w "$SANDBOX" \
  -o "temporal.address=$TEMPORAL_ADDRESS" \
  -o "rpc.listen=tcp://127.0.0.1:$((RR_RPC_PORT + 1))" \
  -o "server.command=php workers/run-worker-process/worker.php") >"$LOG/rr-worker.log" 2>&1 &
PIDS+=($!)
"$RR" serve -c examples.rr.yaml >"$LOG/rr-examples.log" 2>&1 &
PIDS+=($!)

failures=0
check() { # name expected actual
  if [ "$2" == "$3" ]; then echo "ok   $1"; else echo "FAIL $1: expected '$2', got '$3'"; failures=$((failures + 1)); fi
}
run() { # task-queue id -> result
  temporal workflow execute --address "$TEMPORAL_ADDRESS" --execution-timeout 1m --task-queue "$1" \
    --type DemoWorkflow -w "$2" --input '"World"' --output json | jq -c '.result'
}
expected='["Hello, World!","stored: Hello, World!"]'

# worker.php on the default Task Queue, configured by the page's .rr.yaml
check worker-php "$expected" "$(run default worker-1)"

# examples-worker.php: WorkerOptions, registered types, Activity factory, finalizer
check examples-worker "$expected" "$(run your-task-queue examples-1)"
sleep 1
check finalizer-runs 2 "$(grep -c 'Kernel shut down' "$LOG/rr-examples.log" || true)"

# Workers on two Task Queues in one Worker Process
check second-task-queue "$expected" "$(run another-task-queue examples-2)"

# Offline checks on the WorkerOptions and Task Queue examples
offline=$(cd "$SANDBOX" && php -r '
  require "vendor/autoload.php";
  $e = new App\Workers\RunWorkerProcess\WorkerExamples();
  echo $e->taskQueue(Temporal\WorkerFactory::create())->getID(), " ";
  echo $e->workerOptions(Temporal\WorkerFactory::create())->getOptions()->maxConcurrentWorkflowTaskPollers;')
check task-queue-and-options "your-task-queue 10" "$offline"

if [ "$failures" -gt 0 ]; then echo "logs: $LOG"; exit 1; fi
