#!/usr/bin/env bash
# Runs the Activities from the Activity basics page against a dev server. A
# test Workflow calls each Activity Type by name, so a wrong name or return
# value fails the check. Needs the Temporal CLI, jq, and the RoadRunner binary
# at php-sandbox/rr (./vendor/bin/rr get). Run from anywhere:
#   activities/basics/test.sh
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
check() { # name expected actual (JSON objects compare with sorted keys)
  local expected=$2
  expected=$(jq -cS . <<<"$expected" 2>/dev/null || echo "$expected")
  set -- "$1" "$expected" "$3"
  if [ "$2" == "$3" ]; then echo "ok   $1"; else echo "FAIL $1: expected '$2', got '$3'"; failures=$((failures + 1)); fi
}
call() { # task-queue calls-json -> results with errors reduced to "error"
  temporal workflow execute --address "$TEMPORAL_ADDRESS" --execution-timeout 1m --task-queue "$1" \
    --type ActivityBasicsTestWorkflow -w "$1-$RANDOM" --input "$2" --output json \
    | jq -cS '.result | map_values(if type == "string" and startswith("error: ") then "error" else . end)'
}

# Default names are the method names; #[ActivityMethod("transcode_file")] renames download.
check define \
  '{"upload":null,"transcode_file":null,"processFile":"processed","deleteLocalFile":null,"download":"error"}' \
  "$(call activity-basics-define '{"upload":["b","l","t"],"transcode_file":["b","r"],"processFile":[],"deleteLocalFile":["f"],"download":["b","r"]}')"

# The prefix applies to every Activity Type in the interface, including the renamed one.
check custom-name \
  '{"file_activities.upload":null,"file_activities.transcode_file":null,"file_activities.processFile":"processed","file_activities.deleteLocalFile":null,"upload":"error","transcode_file":"error"}' \
  "$(call activity-basics-custom-name '{"file_activities.upload":["b","l","t"],"file_activities.transcode_file":["b","r"],"file_activities.processFile":[],"file_activities.deleteLocalFile":["f"],"upload":["b","l","t"],"transcode_file":["b","r"]}')"

check return-primitive '{"composeGreeting":"Hello World"}' \
  "$(call activity-basics-return-primitive '{"composeGreeting":["Hello","World"]}')"

check return-object '{"composeGreeting":{"greeting":"Hello","name":"World"}}' \
  "$(call activity-basics-return-object '{"composeGreeting":["Hello","World"]}')"

if [ "$failures" -gt 0 ]; then echo "logs: $LOG"; exit 1; fi
