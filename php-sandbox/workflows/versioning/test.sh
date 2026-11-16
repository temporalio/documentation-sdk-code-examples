#!/usr/bin/env bash
# Runs each revision of MyWorkflow from the Versioning page in turn, saves the
# Event History of one Execution per revision, and replays the older histories
# against each newer revision with the WorkflowReplayer. Compatible histories
# must replay; histories a revision no longer supports must fail.
# Needs the Temporal CLI, jq, and the RoadRunner binary at php-sandbox/rr
# (./vendor/bin/rr get). Run from anywhere:
#   workflows/versioning/test.sh
set -euo pipefail

cd "$(dirname "$0")"
RR=../../rr
PORT=${TEMPORAL_TEST_PORT:-7299}
export TEMPORAL_ADDRESS="127.0.0.1:$PORT"
export RR_RPC_PORT=${RR_RPC_PORT:-6099}
export RR_RPC="tcp://127.0.0.1:$RR_RPC_PORT"
LOG=$(mktemp -d)

temporal server start-dev --headless --port "$PORT" --ui-port $((PORT + 1000)) >"$LOG/server.log" 2>&1 &
SERVER=$!
RR_PID=
stop_rr() {
  if [ -n "$RR_PID" ]; then kill "$RR_PID" 2>/dev/null || true; wait "$RR_PID" 2>/dev/null || true; RR_PID=; fi
}
cleanup() {
  stop_rr
  kill "$SERVER" 2>/dev/null || true
  wait 2>/dev/null || true
}
trap cleanup EXIT

until temporal operator cluster health --address "$TEMPORAL_ADDRESS" >/dev/null 2>&1; do sleep 1; done

failures=0
check() { # name expected actual
  if [ "$2" == "$3" ]; then echo "ok   $1"; else echo "FAIL $1: expected '$2', got '$3'"; failures=$((failures + 1)); fi
}
t() { temporal "$@" --address "$TEMPORAL_ADDRESS"; }

use_revision() { # revision
  stop_rr
  VERSIONING_REVISION=$1 "$RR" serve -c .rr.yaml >>"$LOG/rr-$1.log" 2>&1 &
  RR_PID=$!
}
run_and_save() { # revision -> saves $LOG/<revision>.json, checks the Activity it ran
  local id=versioning-$1
  t workflow execute --execution-timeout 30s --task-queue versioning --type MyWorkflow -w "$id" >/dev/null
  t workflow show -w "$id" --output json >"$LOG/$1.json"
}
activity_of() { jq -r '[.events[] | select(.eventType == "EVENT_TYPE_ACTIVITY_TASK_SCHEDULED") | .activityTaskScheduledEventAttributes.activityType.name] | join(",")' "$LOG/$1.json"; }
replay() { # history-revision -> ok | failed: ...
  php ../../workflows/versioning/replay.php "$LOG/$1.json" 2>&1 | head -1
}
expect_replay_ok() { check "$1 replays $2" ok "$(replay "$2")"; }
expect_replay_fails() {
  case "$(replay "$2")" in failed:*) check "$1 rejects $2" ok ok ;; *) check "$1 rejects $2" "replay failure" "ok" ;; esac
}

use_revision pre-patch
run_and_save pre-patch
check pre-patch-activity prePatchActivity "$(activity_of pre-patch)"

use_revision patched
run_and_save patched
check patched-activity postPatchActivity "$(activity_of patched)"
expect_replay_ok patched pre-patch

use_revision another-patch
run_and_save another-patch
check another-patch-activity anotherPatchActivity "$(activity_of another-patch)"
expect_replay_ok another-patch pre-patch
expect_replay_ok another-patch patched

use_revision min-supported-1
run_and_save min-supported-1
check min-supported-1-activity anotherPatchActivity "$(activity_of min-supported-1)"
expect_replay_ok min-supported-1 patched
expect_replay_ok min-supported-1 another-patch
expect_replay_fails min-supported-1 pre-patch

use_revision min-supported-2
run_and_save min-supported-2
check min-supported-2-activity anotherPatchActivity "$(activity_of min-supported-2)"
expect_replay_ok min-supported-2 another-patch
expect_replay_fails min-supported-2 patched

# Without getVersion, the original code can't replay a patched history.
use_revision pre-patch
t workflow execute --execution-timeout 30s --task-queue versioning --type MyWorkflow -w versioning-warmup >/dev/null
expect_replay_fails pre-patch patched

# Workflow cutover: both Workflow Types run side by side.
check cutover-v1 '"v1"' "$(t workflow execute --execution-timeout 30s --task-queue versioning-cutover --type MyWorkflow -w cutover-1 --output json | jq -c '.result')"
check cutover-v2 '"v2"' "$(t workflow execute --execution-timeout 30s --task-queue versioning-cutover --type MyWorkflowV2 -w cutover-2 --output json | jq -c '.result')"

if [ "$failures" -gt 0 ]; then echo "logs: $LOG"; exit 1; fi
