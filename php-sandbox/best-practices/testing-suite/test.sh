#!/usr/bin/env bash
# Tests the code from the Testing page:
#  - Runs worker.test.php (the Temporal\Testing WorkerFactory) under RoadRunner
#    with .rr.test.yaml (the KV storage for Activity mocks) against a dev
#    server, sets Activity mocks with ActivityMocker the way the PHPUnit test
#    case does, and runs the Workflow with the Temporal CLI.
#  - Runs the replay examples with tests/test-replay.php.
#  - Checks the PHPUnit test case and bootstraps with PHPStan (they need the
#    grpc extension and the temporal-test-server binary to run), and validates
#    phpunit.xml against PHPUnit's schema.
# Needs the Temporal CLI, jq, xmllint, and the RoadRunner binary at
# php-sandbox/rr (./vendor/bin/rr get). Run from anywhere:
#   best-practices/testing-suite/test.sh
set -euo pipefail

cd "$(dirname "$0")"
SANDBOX=$(cd ../.. && pwd)
RR=$SANDBOX/rr
PORT=${TEMPORAL_TEST_PORT:-7299}
export TEMPORAL_ADDRESS="127.0.0.1:$PORT"
export RR_RPC_PORT=${RR_RPC_PORT:-6099}
export RR_RPC="tcp://127.0.0.1:$RR_RPC_PORT"
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

(cd tests && exec "$RR" serve -c .rr.test.yaml) >"$LOG/rr.log" 2>&1 &
RR_PID=$!

failures=0
check() { # name expected actual
  if [ "$2" == "$3" ]; then echo "ok   $1"; else echo "FAIL $1: expected '$2', got '$3'"; failures=$((failures + 1)); fi
}
t() { temporal "$@" --address "$TEMPORAL_ADDRESS"; }
run() { # id -> result, or the failure message
  t workflow execute --execution-timeout 30s --task-queue default --type MyWorkflow -w "$1" --input '"hello"' --output json 2>/dev/null \
    | jq -r 'if .result != null then .result else empty end' || true
}
failure() { # id -> innermost failure message
  t workflow show -w "$1" --output json \
    | jq -r '[.. | objects | select(has("message")) | .message] | last'
}

# Without a mock, the real Activity runs.
check real-activity HELLO "$(run mock-0)"

# expectCompletion
php tests/mock-activity.php complete world
check mocked-completion world "$(run mock-1)"

# expectFailure
php tests/mock-activity.php fail 'something went wrong'
run mock-2 >/dev/null
check mocked-failure 'something went wrong' "$(failure mock-2)"

# clear
php tests/mock-activity.php clear
check mock-cleared HELLO "$(run mock-3)"

# Replay: export Executions and one Event History with the CLI, then replay them.
jq -n '[{workflowId: "mock-0"}, {workflowId: "mock-3"}]' >"$LOG/ids.json"
for id in mock-0 mock-3; do
  t workflow describe -w "$id" --output json | jq '{workflowId: .workflowExecutionInfo.execution.workflowId, runId: .workflowExecutionInfo.execution.runId}'
done | jq -s . >"$LOG/executions.json"
t workflow show -w mock-0 --output json >"$LOG/history.json"
# RoadRunner reads replayFromJSON's 'history.json' relative to its working directory.
cp "$LOG/history.json" tests/history.json
php tests/test-replay.php "$LOG" >"$LOG/test-replay.log" 2>&1 || true
rm -f tests/history.json
cat "$LOG/test-replay.log"
if grep -q '^FAIL' "$LOG/test-replay.log" || ! grep -q 'replay-detects-nondeterminism' "$LOG/test-replay.log"; then
  failures=$((failures + 1))
fi

# Static checks
cd "$SANDBOX"
if vendor/bin/phpstan analyse -c best-practices/testing-suite/phpstan.neon --no-progress --memory-limit=1G >"$LOG/phpstan.log" 2>&1; then
  check phpstan ok ok
else
  check phpstan "no errors" "see $LOG/phpstan.log"
fi
# The test cases need grpc to run; check that PHPUnit loads them.
listed=$(for f in SimpleWorkflowTestCase SimpleWorkflowFailureTestCase; do
  vendor/bin/phpunit --no-configuration --bootstrap vendor/autoload.php --list-tests "best-practices/testing-suite/tests/$f.php" | grep -c -- ' - ' || true
done | paste -sd, -)
check phpunit-test-cases 1,1 "$listed"
if xmllint --noout --schema vendor/phpunit/phpunit/phpunit.xsd best-practices/testing-suite/phpunit.xml 2>"$LOG/xmllint.log"; then
  check phpunit-xml valid valid
else
  check phpunit-xml valid "see $LOG/xmllint.log"
fi

if [ "$failures" -gt 0 ]; then echo "logs: $LOG"; exit 1; fi
